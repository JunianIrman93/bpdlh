package id.lapor.lh.feature.report.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import id.lapor.lh.feature.report.domain.LocationProvider
import id.lapor.lh.feature.report.domain.LocationResult
import id.lapor.lh.feature.report.domain.ReportLocation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

private const val LOCATION_TIMEOUT_MS = 20_000L

class AndroidLocationProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LocationProvider {
        override suspend fun currentLocation(): LocationResult {
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) return LocationResult.PermissionDenied

            val manager = context.getSystemService(LocationManager::class.java)
            if (!LocationManagerCompat.isLocationEnabled(manager)) return LocationResult.Disabled
            val provider =
                when {
                    manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> return LocationResult.Disabled
                }

            val location =
                withTimeoutOrNull(LOCATION_TIMEOUT_MS) { requestSingle(manager, provider) }
                    ?: return LocationResult.Timeout
            return LocationResult.Found(
                ReportLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyM = location.accuracy,
                    capturedAt = location.time,
                    isMock = LocationCompat.isMock(location),
                ),
            )
        }

        @Suppress("MissingPermission")
        private suspend fun requestSingle(
            manager: LocationManager,
            provider: String,
        ): Location? =
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) {
                    if (continuation.isActive) continuation.resume(it)
                }
            }
    }
