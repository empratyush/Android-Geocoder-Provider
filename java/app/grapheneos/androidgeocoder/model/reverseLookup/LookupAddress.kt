package app.grapheneos.androidgeocoder.model.reverseLookup

import com.google.gson.annotations.SerializedName

class LookupAddress(
    @SerializedName("suburb") val suburb: String,
    @SerializedName("road") val road: String,
    @SerializedName("city") val city: String,
    @SerializedName("administrative") val administrative: String,

    @SerializedName("state") val state: String,
    @SerializedName("postcode") val postCode: String,

    @SerializedName("country") val country: String,
    @SerializedName("country_code") val countryCode: String,

    @SerializedName("county") val county: String,
    @SerializedName("neighbourhood") val neighbourhood: String

    )
    