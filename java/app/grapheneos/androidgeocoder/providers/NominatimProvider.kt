package app.grapheneos.androidgeocoder.providers

import android.location.Address
import app.grapheneos.androidgeocoder.model.reverseLookup.LookupResponse
import app.grapheneos.androidgeocoder.model.forwardLookup.ForwardLookupResponse
import app.grapheneos.androidgeocoder.model.reverseLookup.ReverseLookupResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.time.Duration
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val STATUS_CODE_SUCCESS = 200
private const val DEFAULT_LAT_LONG = 0.0 //0.0 is default

class NominatimProvider(
    private val gson: Gson = Gson(),
    private val baseUrl: () -> String,
) : Provider {

    private fun Double.isDefault() = this == DEFAULT_LAT_LONG

    override suspend fun getFromLocationName(
        locationName: String,
        maxResults: Int,
        lowerLeftLatitude: Double,
        lowerLeftLongitude: Double,
        upperRightLatitude: Double,
        upperRightLongitude: Double
    ): ForwardLookupResponse {
        return try {

            val serverAddress = baseUrl()
            if (serverAddress.isEmpty()) {
                return ForwardLookupResponse.Failed("")
            }

            val coreSearch =
                "${serverAddress}/search?format=json&bounded=1&addressdetails=1&limit=$maxResults&q=$locationName"

            val url =
                if (lowerLeftLatitude.isDefault() &&
                    lowerLeftLongitude.isDefault() &&
                    upperRightLatitude.isDefault() &&
                    upperRightLongitude.isDefault()
                ) {
                    coreSearch
                } else {
                    "$coreSearch&bounded=1&viewbox=$lowerLeftLongitude,$lowerLeftLatitude,$upperRightLongitude,$upperRightLatitude"
                }

            val response = String(getResponseFromUrl(url))
            val responseListType = object : TypeToken<List<LookupResponse>>() {}.type
            val responses = gson.fromJson<List<LookupResponse>>(response, responseListType)

            ForwardLookupResponse.Success(if (responses.isEmpty()) emptyList() else responses.toAndroidAddress())
        } catch (e: JSONException) {
            ForwardLookupResponse.Failed(e.localizedMessage)
        } catch (e: MalformedURLException) {
            ForwardLookupResponse.Failed(e.localizedMessage)
        } catch (e: IOException) {
            ForwardLookupResponse.Failed(e.localizedMessage)
        } catch (e: CancellationException) {
            ForwardLookupResponse.Failed(e.localizedMessage)
        }
    }

    private fun List<LookupResponse>.toAndroidAddress() =
        map { LookupResponse.toAndroidAddress(it) }

    override suspend fun reverseLookup(latitude: Double, longitude: Double): ReverseLookupResponse {
        return try {
            val serverAddress = baseUrl()
            if (serverAddress.isEmpty()) {
                return ReverseLookupResponse.Failed("")
            }
            ReverseLookupResponse.Success(reverseLookupImpl(serverAddress, latitude, longitude))
        } catch (e: JSONException) {
            ReverseLookupResponse.Failed(e.localizedMessage)
        } catch (e: MalformedURLException) {
            ReverseLookupResponse.Failed(e.localizedMessage)
        } catch (e: IOException) {
            ReverseLookupResponse.Failed(e.localizedMessage)
        } catch (e: CancellationException) {
            ReverseLookupResponse.Failed(e.localizedMessage)
        }
    }

    @Throws(JSONException::class,
        MalformedURLException::class,
        IOException::class,
        CancellationException::class)
    suspend fun reverseLookupImpl(serverAddress: String, latitude: Double, longitude: Double): Address {
        val url = "$serverAddress/reverse?lat=$latitude&lon=$longitude&format=json&zoom=18"
        val response = String(getResponseFromUrl(url))
        val responseType = object : TypeToken<LookupResponse>() {}.type
        val responses = gson.fromJson<LookupResponse>(response, responseType)
        return LookupResponse.toAndroidAddress(responses)
    }

    @Throws(
        JSONException::class,
        MalformedURLException::class,
        IOException::class,
        CancellationException::class)
    private suspend fun getResponseFromUrl(url: String): ByteArray = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        val timeout = Duration.ofSeconds(20).toMillis().toInt()

        connection.readTimeout = timeout
        connection.connectTimeout = timeout

        connection.connect()

        if (connection.responseCode != STATUS_CODE_SUCCESS) {
            throw IOException("request failed, response code ${connection.responseCode}")
        }
        //TODO as of now response is few KBs
        //but convert it into reading chunk of 4KB
        //and call `ensureActive` to properly respect
        //cancellation request
        connection.inputStream.readAllBytes()
    }

}
