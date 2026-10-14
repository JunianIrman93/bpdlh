package id.lapor.lh.feature.report.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import id.lapor.lh.feature.report.domain.LocationProvider
import id.lapor.lh.feature.report.domain.LocationResult
import id.lapor.lh.feature.report.domain.ReportLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

private const val LOCATION_TIMEOUT_MS = 20_000L
private const val ADDRESS_TIMEOUT_MS = 10_000L
private const val MAX_ADDRESS_LENGTH = 300
private val ADDRESS_LOCALE: Locale = Locale.forLanguageTag("id-ID")

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

        override suspend fun addressOf(location: ReportLocation): String? {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context, ADDRESS_LOCALE)
            val address =
                withTimeoutOrNull(ADDRESS_TIMEOUT_MS) { geocode(geocoder, location.latitude, location.longitude) }
                    ?: return null
            return address.describe()?.take(MAX_ADDRESS_LENGTH)
        }

        private suspend fun geocode(
            geocoder: Geocoder,
            latitude: Double,
            longitude: Double,
        ): Address? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        },
                    )
                }
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                    } catch (e: IOException) {
                        null
                    }
                }
            }

        private fun Address.describe(): String? {
            val line =
                if (maxAddressLineIndex >= 0) {
                    getAddressLine(0)
                } else {
                    listOfNotNull(thoroughfare, subLocality, locality, subAdminArea, adminArea).joinToString(", ")
                }
            return line
                ?.removeSuffix(", ${countryName.orEmpty()}")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
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
