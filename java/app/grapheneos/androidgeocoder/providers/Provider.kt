package app.grapheneos.androidgeocoder.providers

import androidx.annotation.FloatRange
import app.grapheneos.androidgeocoder.model.forwardLookup.ForwardLookupResponse
import app.grapheneos.androidgeocoder.model.reverseLookup.ReverseLookupResponse

interface Provider {

    suspend fun reverseLookup(latitude: Double, longitude: Double): ReverseLookupResponse

    suspend fun getFromLocationName(
        locationName: String,
        maxResults: Int,
        @FloatRange(from = -90.0, to = 90.0) lowerLeftLatitude: Double,
        @FloatRange(from = -180.0, to = 180.0) lowerLeftLongitude: Double,
        @FloatRange(from = -90.0, to = 90.0) upperRightLatitude: Double,
        @FloatRange(from = -180.0, to = 180.0) upperRightLongitude: Double
    ) : ForwardLookupResponse

}
