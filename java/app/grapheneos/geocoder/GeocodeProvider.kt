package app.grapheneos.geocoder

import android.app.Service
import android.content.Intent
import android.location.Address
import android.location.provider.ForwardGeocodeRequest
import android.location.provider.GeocodeProviderBase
import android.location.provider.ReverseGeocodeRequest
import android.os.IBinder
import android.os.OutcomeReceiver
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

    //TODO: replace the base url with self hosted one.
    private val provider by lazy { NominatimProvider("https://nominatim.openstreetmap.org") }

    override fun onCreate() {
        super.onCreate()
        logger.d("onCreate: ")
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
        super.onDestroy()
    }
}