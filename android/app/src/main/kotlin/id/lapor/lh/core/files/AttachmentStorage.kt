package id.lapor.lh.core.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

data class SourceInfo(
    val displayName: String?,
    val mimeType: String?,
)

data class StoredFile(
    val path: String,
    val sizeBytes: Long,
    val sha256: String,
)

private const val MAX_IMAGE_SIDE = 1600
private const val JPEG_QUALITY = 80
private const val BUFFER_SIZE = 64 * 1024

@Singleton
class AttachmentStorage
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val resolver = context.contentResolver
        private val attachmentsRoot = File(context.filesDir, "attachments")
        private val cameraDir = File(context.cacheDir, "camera")

        fun describe(uri: Uri): SourceInfo {
            val name =
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            val extension = name?.substringAfterLast('.', "")?.lowercase()
            val mime =
                resolver.getType(uri)?.takeUnless { it == "application/octet-stream" }
                    ?: extension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            return SourceInfo(displayName = name, mimeType = normalizeMime(mime, extension))
        }

        fun newCameraUri(): Uri {
            cameraDir.mkdirs()
            val file = File(cameraDir, "${UUID.randomUUID()}.jpg")
            return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }

        fun discardCameraFiles() {
            cameraDir.listFiles()?.forEach { it.delete() }
        }

        suspend fun storeCompressedImage(
            source: Uri,
            reportId: String,
            attachmentId: String,
        ): StoredFile =
            withContext(Dispatchers.IO) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                open(source).use { BitmapFactory.decodeStream(it, null, bounds) }
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "not an image" }

                val orientation =
                    open(source).use {
                        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    }

                var sampleSize = 1
                while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_IMAGE_SIDE) sampleSize *= 2
                val decoded =
                    open(source).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize }) }
                        ?: error("decode failed")

                val scale = minOf(1f, MAX_IMAGE_SIDE.toFloat() / max(decoded.width, decoded.height))
                val matrix = orientationMatrix(orientation).apply { preScale(scale, scale) }
                val output = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (output !== decoded) decoded.recycle()

                val target = targetFile(reportId, attachmentId, "jpg")
                target.outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                output.recycle()
                StoredFile(path = target.absolutePath, sizeBytes = target.length(), sha256 = sha256Of(target))
            }

        suspend fun storeCopy(
            source: Uri,
            reportId: String,
            attachmentId: String,
            extension: String,
            maxBytes: Long,
        ): StoredFile? =
            withContext(Dispatchers.IO) {
                open(source).use { storeStream(it, reportId, attachmentId, extension, maxBytes) }
            }

        suspend fun storeStream(
            input: InputStream,
            reportId: String,
            attachmentId: String,
            extension: String,
            maxBytes: Long,
        ): StoredFile? =
            withContext(Dispatchers.IO) {
                val target = targetFile(reportId, attachmentId, extension)
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                target.outputStream().use { out ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > maxBytes) break
                        digest.update(buffer, 0, read)
                        out.write(buffer, 0, read)
                    }
                }
                if (total > maxBytes) {
                    target.delete()
                    return@withContext null
                }
                StoredFile(path = target.absolutePath, sizeBytes = total, sha256 = digest.digest().toHex())
            }

        fun delete(path: String) {
            File(path).delete()
        }

        fun deleteAttachmentFiles(
            reportId: String,
            attachmentId: String,
        ) {
            File(attachmentsRoot, reportId).listFiles { file -> file.nameWithoutExtension == attachmentId }?.forEach { it.delete() }
        }

        fun deleteReport(reportId: String) {
            File(attachmentsRoot, reportId).deleteRecursively()
        }

        fun deleteAll() {
            attachmentsRoot.deleteRecursively()
            discardCameraFiles()
        }

        private fun open(uri: Uri): InputStream = resolver.openInputStream(uri) ?: error("cannot open $uri")

        private fun targetFile(
            reportId: String,
            attachmentId: String,
            extension: String,
        ): File = File(File(attachmentsRoot, reportId).apply { mkdirs() }, "$attachmentId.$extension")

        private fun sha256Of(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().toHex()
        }
    }

private fun normalizeMime(
    mime: String?,
    extension: String?,
): String? =
    when {
        mime == "text/comma-separated-values" || extension == "csv" -> "text/csv"
        else -> mime
    }

private fun orientationMatrix(orientation: Int): Matrix =
    Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                postRotate(180f)
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                postScale(1f, -1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> {
                postRotate(90f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(-90f)
                postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                postRotate(-90f)
            }
        }
    }

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
