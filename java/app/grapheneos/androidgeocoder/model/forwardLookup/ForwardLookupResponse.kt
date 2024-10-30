package app.grapheneos.androidgeocoder.model.forwardLookup

import android.location.Address

sealed class ForwardLookupResponse {

    data class Success(val addresses: List<Address>) : ForwardLookupResponse()
    data class Failed(val error: String?) : ForwardLookupResponse()

}
