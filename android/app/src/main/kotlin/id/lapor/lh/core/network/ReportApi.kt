package id.lapor.lh.core.network

import id.lapor.lh.core.network.dto.AttachmentReceiptDto
import id.lapor.lh.core.network.dto.ReportDetailDto
import id.lapor.lh.core.network.dto.ReportInputDto
import id.lapor.lh.core.network.dto.ReportListDto
import id.lapor.lh.core.network.dto.ReportReceiptDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ReportApi {
    @PUT("reports/{id}")
    suspend fun upsert(
        @Path("id") id: String,
        @Body report: ReportInputDto,
    ): ReportReceiptDto

    @Multipart
    @PUT("reports/{id}/attachments/{attachmentId}")
    suspend fun uploadAttachment(
        @Path("id") id: String,
        @Path("attachmentId") attachmentId: String,
        @Header("X-Content-SHA256") sha256: String,
        @Part file: MultipartBody.Part,
    ): AttachmentReceiptDto

    @GET("reports")
    suspend fun list(
        @Query("updated_since") updatedSince: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): ReportListDto

    @GET("reports/{id}")
    suspend fun get(
        @Path("id") id: String,
    ): ReportDetailDto

    @Streaming
    @GET("reports/{id}/attachments/{attachmentId}/file")
    suspend fun downloadAttachment(
        @Path("id") id: String,
        @Path("attachmentId") attachmentId: String,
    ): ResponseBody
}
