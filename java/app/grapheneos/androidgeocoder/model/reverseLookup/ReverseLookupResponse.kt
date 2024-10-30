package app.grapheneos.androidgeocoder.model.reverseLookup

import android.location.Address

sealed class ReverseLookupResponse {

    data class Success(val address: Address) : ReverseLookupResponse()
    data class Failed(val error: String?) : ReverseLookupResponse()

}
