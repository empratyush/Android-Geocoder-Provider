package app.grapheneos.geocoder

import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.ext.settings.GeocodingSettings
import android.location.Address
import android.location.provider.ForwardGeocodeRequest
import android.location.provider.GeocodeProviderBase
import android.location.provider.ReverseGeocodeRequest
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.OutcomeReceiver
import android.provider.Settings
import app.grapheneos.androidgeocoder.model.forwardLookup.ForwardLookupResponse
import app.grapheneos.androidgeocoder.model.reverseLookup.ReverseLookupResponse
import app.grapheneos.androidgeocoder.providers.NominatimProvider
import app.grapheneos.logger.Logger
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class GeocodeProvider : Service() {

    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logger = Logger("GeocodeMockApp")
    private var url = ""

    private fun runWithTimeout(
        timeout: Long = Duration.ofMinutes(1).toMillis(),
        onTimeout: (error: Throwable) -> Unit,
        block: suspend () -> Unit
    ) {
        networkScope.launch {
            try {
                withTimeout(timeout) { block() }
            } catch (error: TimeoutCancellationException) {
                onTimeout(error)
            }
        }
    }

    private fun ForwardGeocodeRequest.toDebugLog() = " locationName : $locationName, " +
            "lowerLeftLatitude $lowerLeftLatitude, " +
            "lowerLeftLongitude $lowerLeftLongitude, " +
            "upperRightLatitude $upperRightLatitude, " +
            "upperRightLongitude $upperRightLongitude "

    private fun ReverseGeocodeRequest.toDebugLog() =
        "latitude $latitude, longitude $longitude, maxResults $maxResults, locale $locale"

    //callback are received on non main thread
    //it should be kept free, no blocking code
    private val baseImplementation by lazy {
        object : GeocodeProviderBase(
            this,
            "NetworkGeolocationService",
        ) {
            override fun onForwardGeocode(request: ForwardGeocodeRequest,
                callback: OutcomeReceiver<MutableList<Address>, Throwable>) {
                logger.d("onForwardGeocode: ${request.toDebugLog()} ")
                runWithTimeout(onTimeout = callback::onError) {
                    handleForwardRequest(request, callback)
                }
            }

            override fun onReverseGeocode(request: ReverseGeocodeRequest,
                callback: OutcomeReceiver<MutableList<Address>, Throwable>) {
                logger.d("onForwardGeocode: ${request.toDebugLog()} ")
                runWithTimeout(onTimeout = callback::onError) {
                    handleReverseGeocode(request, callback)
                }
            }

        }
    }

    private val provider by lazy {
        NominatimProvider {
            url
        }
    }

    private val settingsObserver = object: ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            logger.d("settings updated")
            updateUri()
        }
    }

    override fun onCreate() {
        super.onCreate()
        logger.d("onCreate: ")

        val geocodingKey = Settings.Global.getUriFor(GeocodingSettings.GEOCODING_SETTINGS.key)
        contentResolver.registerContentObserver(
            geocodingKey,
            false,
            settingsObserver
        )
        updateUri()
    }

    private fun updateUri() {
        val value = GeocodingSettings.GEOCODING_SETTINGS.get(this)
        when (value) {
            GeocodingSettings.GEOCODING_DISABLED -> {
                url = ""
            }
            GeocodingSettings.GEOCODING_SERVER_NOMINATIM -> {
                url = "https://nominatim.openstreetmap.org/"
            }
            GeocodingSettings.GEOCODING_SERVER_GRAPHENEOS_PROXY -> {
                url = "https://nominatim.grapheneos.org/"
            }
        }
        logger.d("base url updated to $url")
    }

    private suspend fun handleForwardRequest(
        request: ForwardGeocodeRequest,
        callback: OutcomeReceiver<MutableList<Address>, Throwable>) {

        val response = provider.getFromLocationName(
            request.locationName,
            request.maxResults,
            request.lowerLeftLatitude,
            request.lowerLeftLongitude,
            request.upperRightLatitude,
            request.upperRightLongitude
        )

        when (response) {
            is ForwardLookupResponse.Failed -> {
                logger.d("handleForwardRequest failed: ${response.error}")
                callback.onError(Throwable("failed! ${response.error}"))
            }

            is ForwardLookupResponse.Success -> {
                logger.d("handleForwardRequest success response size: ${response.addresses.size}")
                callback.onResult(response.addresses.toMutableList())
            }
        }

    }

    private suspend fun handleReverseGeocode(
        request: ReverseGeocodeRequest,
        callback: OutcomeReceiver<MutableList<Address>, Throwable>) {

        val response = provider.reverseLookup(request.latitude, request.longitude)

        when (response) {
            is ReverseLookupResponse.Failed -> {
                logger.d("handleReverseGeocode failed: ${response.error}")
                callback.onError(Throwable("failed! ${response.error}"))
            }

            is ReverseLookupResponse.Success -> {
                logger.d("handleReverseGeocode success: ${response.address}")
                callback.onResult(mutableListOf(response.address))
            }
        }

    }

    override fun onBind(intent: Intent?): IBinder? {
        logger.d("onBind: ")
        return baseImplementation.binder
    }

    override fun onDestroy() {
        logger.d("onDestroy: ")
        contentResolver.unregisterContentObserver(settingsObserver)
        super.onDestroy()
    }
}