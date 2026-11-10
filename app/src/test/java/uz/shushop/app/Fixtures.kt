package uz.shushop.app

import org.json.JSONObject

/** Real answers saved from the running server (src/test/resources/fixtures), so the parsers are tested on the real thing. */
object Fixtures {
    fun text(name: String): String =
        (Fixtures::class.java.classLoader!!.getResourceAsStream("fixtures/$name") ?: error("missing fixture $name"))
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

    fun json(name: String): JSONObject = JSONObject(text(name))
}
