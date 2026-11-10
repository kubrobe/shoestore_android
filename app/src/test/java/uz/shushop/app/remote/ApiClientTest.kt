package uz.shushop.app.remote

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import uz.shushop.app.data.ApiException
import uz.shushop.app.data.NetworkException

/** Runs the real ApiClient against a tiny local web server, so the headers, bodies and error handling are really exercised. */
class ApiClientTest {
    private lateinit var server: HttpServer
    private var token: String? = "T0KEN"
    private val seen = mutableListOf<Seen>()
    private var reply: (Seen) -> Pair<Int, String> = { 200 to """{"ok":true}""" }

    data class Seen(val method: String, val path: String, val auth: String?, val override: String?, val contentType: String?, val body: String)

    @Before fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { ex: HttpExchange ->
            val s = Seen(
                ex.requestMethod, ex.requestURI.toString(), ex.requestHeaders.getFirst("Authorization"),
                ex.requestHeaders.getFirst("X-HTTP-Method-Override"), ex.requestHeaders.getFirst("Content-Type"),
                ex.requestBody.readBytes().toString(Charsets.UTF_8),
            )
            seen.add(s)
            val (code, text) = reply(s)
            val bytes = text.toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(code, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            if (bytes.isNotEmpty()) ex.responseBody.use { it.write(bytes) }
            ex.close()
        }
        server.start()
    }

    @After fun stop() { server.stop(0) }

    private fun client(url: String = "http://127.0.0.1:${server.address.port}/") = ApiClient({ url }, { token }, 2_000, 5_000)

    @Test fun getSendsTheBearerToken() {
        reply = { 200 to """{"hello":"world"}""" }
        val r = client().get("/api/me")
        assertEquals("world", r.getString("hello"))
        assertEquals("GET", seen[0].method)
        assertEquals("/api/me", seen[0].path)               // the trailing slash of the base URL does not double up
        assertEquals("Bearer T0KEN", seen[0].auth)
    }

    @Test fun noTokenNoHeader() {
        token = null
        client().get("/api/meta")
        assertNull(seen[0].auth)
    }

    @Test fun postSendsJson() {
        client().post("/api/mobile/login", JSONObject().put("email", "a@b.uz").put("password", "secret123"))
        assertEquals("POST", seen[0].method)
        assertTrue(seen[0].contentType!!.startsWith("application/json"))
        val sent = JSONObject(seen[0].body)
        assertEquals("a@b.uz", sent.getString("email"))
    }

    @Test fun postWithoutABodySendsAnEmptyObject() {
        client().post("/api/notifications/read-all")
        assertEquals("{}", seen[0].body)
    }

    @Test fun putAndDelete() {
        client().put("/api/favourites/3")
        client().delete("/api/cart/items/41")
        assertEquals(listOf("PUT", "DELETE"), seen.map { it.method })
        assertEquals("/api/cart/items/41", seen[1].path)
    }

    @Test fun patchGoesAsPostWithTheOverrideHeader() {
        client().patch("/api/me", JSONObject().put("shoeSizeEu", 43))
        assertEquals("POST", seen[0].method)
        assertEquals("PATCH", seen[0].override)
        assertEquals(43, JSONObject(seen[0].body).getInt("shoeSizeEu"))
    }

    @Test fun serverErrorsBecomeApiExceptionWithTheServersSentence() {
        reply = { 409 to """{"error":"Only 2 left of Urban Step (size 42, Black)."}""" }
        try {
            client().post("/api/checkout", JSONObject())
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals(409, e.status)
            assertEquals("Only 2 left of Urban Step (size 42, Black).", e.message)
            assertTrue(!e.isUnauthorized)
        }
    }

    @Test fun unauthorizedIsRecognised() {
        reply = { 401 to """{"error":"Please log in"}""" }
        try {
            client().get("/api/mobile/sync")
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertTrue(e.isUnauthorized)
        }
    }

    @Test fun anErrorPageThatIsNotJsonStillGivesAReadableMessage() {
        reply = { 502 to "<html>Bad gateway</html>" }
        try {
            client().get("/api/meta")
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals(502, e.status)
            assertTrue(e.message!!.contains("502"))
        }
    }

    @Test fun anOkAnswerThatIsNotJsonIsReportedNotCrashed() {
        reply = { 200 to "<html>login page</html>" }
        try {
            client().get("/api/meta")
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals(-1, e.status)
        }
    }

    @Test fun anEmptyOkAnswerIsAnEmptyObject() {
        reply = { 200 to "" }
        assertEquals(0, client().get("/api/health").length())
    }

    @Test fun noServerMeansNetworkException() {
        val dead = ServerSocketPort.free()
        try {
            client("http://127.0.0.1:$dead").get("/api/meta")
            fail("expected NetworkException")
        } catch (e: NetworkException) {
            assertTrue(e.message!!.contains("internet"))
        }
    }

    private object ServerSocketPort {
        fun free(): Int = java.net.ServerSocket(0).use { it.localPort }
    }
}
