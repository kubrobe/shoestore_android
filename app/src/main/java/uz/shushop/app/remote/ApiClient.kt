package uz.shushop.app.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONException
import org.json.JSONObject
import uz.shushop.app.data.ApiException
import uz.shushop.app.data.NetworkException
import uz.shushop.app.data.Parsers

/**
 * The phone's connection to the Shushop server (the same server and database the website uses).
 * Plain HttpURLConnection + JSON, no extra libraries. These calls BLOCK, so the repository runs them on Dispatchers.IO.
 *
 * - [baseUrl] and [token] are lambdas so a changed server address or a fresh login is picked up by the next call.
 * - A non-2xx answer becomes [ApiException] with the server's own sentence ({"error": "..."}).
 * - No connection becomes [NetworkException].
 */
class ApiClient(
    private val baseUrl: () -> String,
    private val token: () -> String?,
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
) {
    fun get(path: String): JSONObject = call("GET", path)
    fun post(path: String, body: JSONObject? = null): JSONObject = call("POST", path, body)
    fun put(path: String, body: JSONObject? = null): JSONObject = call("PUT", path, body)
    fun patch(path: String, body: JSONObject): JSONObject = call("PATCH", path, body)
    fun delete(path: String): JSONObject = call("DELETE", path)

    private fun call(method: String, path: String, body: JSONObject? = null): JSONObject {
        val url = try {
            URL(baseUrl().trimEnd('/') + path)
        } catch (e: java.net.MalformedURLException) {
            throw NetworkException("The server address is not valid. Check it in Profile > Server.", e)
        }
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.setRequestProperty("Accept", "application/json")
            token()?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            // Java's HttpURLConnection cannot send PATCH everywhere, so it goes as POST + a header the server understands.
            conn.requestMethod = if (method == "PATCH") "POST" else method
            if (method == "PATCH") conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")

            val payload = body ?: if (method == "POST" || method == "PUT") JSONObject() else null
            if (payload != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw ApiException(status, Parsers.errorMessage(text, "The server answered with an error ($status). Please try again."))
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } catch (e: IOException) {
            throw NetworkException("Can't reach the shop. Check your internet connection.", e)
        } catch (e: JSONException) {
            throw ApiException(-1, "The shop sent an answer the app can't read. Check the server address in Profile > Server.")
        } finally {
            conn.disconnect()
        }
    }
}
