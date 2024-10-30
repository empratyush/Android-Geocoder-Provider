package app.grapheneos.androidgeocoder.model.reverseLookup

import android.location.Address
import com.google.gson.annotations.SerializedName
import java.util.Locale


class LookupResponse(
    @SerializedName("place_id") val placeId: Long,
    @SerializedName("lon") val longitude: Double,
    @SerializedName("lat") val latitude: Double,
    @SerializedName("address") val address: LookupAddress,
    @SerializedName("display_name") val displayName: String
) {

    companion object {
        fun toAndroidAddress(lookupResponse: LookupResponse) = Address(Locale.ENGLISH).apply {
            setLatitude(lookupResponse.latitude)
            setLongitude(lookupResponse.longitude)

            adminArea = lookupResponse.address.state
            subAdminArea = lookupResponse.address.neighbourhood
            thoroughfare = lookupResponse.address.road
            locality = lookupResponse.address.city
            subLocality = lookupResponse.address.suburb
            countryName = lookupResponse.address.country
            countryCode = lookupResponse.address.countryCode
            featureName = lookupResponse.displayName
            postalCode = lookupResponse.address.postCode

        }
    }

}
