package app.grapheneos.geocoder

import android.app.Service
import android.content.Intent
import android.location.Address
import android.location.provider.ForwardGeocodeRequest
import android.location.provider.GeocodeProviderBase
import android.location.provider.ReverseGeocodeRequest
import android.os.IBinder
import android.os.OutcomeReceiver
import android.util.Log
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.Locale

private const val TAG = "GeocodeMockApp"

class GeocodeMockApp : Service() {

    private val baseImplementation by lazy { object : GeocodeProviderBase(
        this,
        "NetworkGeolocationService",
    ) {
        override fun onForwardGeocode(request: ForwardGeocodeRequest,
            callback: OutcomeReceiver<MutableList<Address>, Exception>) {
            handleForwardRequest(request, callback)
        }

        override fun onReverseGeocode(request: ReverseGeocodeRequest,
            callback: OutcomeReceiver<MutableList<Address>, Exception>) {
            handleReverseGeocode(request, callback)
        }

    }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
    }

    private fun handleForwardRequest(
        request: ForwardGeocodeRequest,
        callback: OutcomeReceiver<MutableList<Address>, Exception>) {

        Log.d(TAG, "handleForwardRequest: ")

        if (request.maxResults == 11) {
            callback.onError(IllegalStateException("this is for testing"))
            return
        }


        val items = mutableListOf<Address>()
        for (index in 0 until request.maxResults) {
            val address = Address(Locale.ENGLISH).apply {
                countryName = "India"
                countryCode = "IN"
                postalCode = "123456"
                phone = "+919876543210"
                url = "https://example.com/random.png"
                locality = "IDK"
                latitude = 73.0
                longitude = 27.0
            }
            items.add(address)
        }
        callback.onResult(items)

    }

    private fun handleReverseGeocode(
        request: ReverseGeocodeRequest,
        callback: OutcomeReceiver<MutableList<Address>, Exception>) {

        Log.d(TAG, "handleReverseGeocode: ")

        if (request.maxResults == 12) {
            callback.onError(IllegalStateException("this is for testing"))
            return
        }

        val items = mutableListOf<Address>()
        for (index in 0 until request.maxResults) {
            val address = Address(Locale.ENGLISH).apply {
                countryName = "India"
                countryCode = "IN"
                postalCode = "123456"
                phone = "+919876543210"
                url = "https://example.com/random.png"
                locality = "IDK"
                latitude = 73.0
                longitude = 27.0
            }
            items.add(address)
        }
        callback.onResult(items)

    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind: ")
        return baseImplementation.binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }
}