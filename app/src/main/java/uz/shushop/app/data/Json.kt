package uz.shushop.app.data

import org.json.JSONArray
import org.json.JSONObject

// Small helpers around org.json. The important one: a JSON null must become a Kotlin null (or the default),
// because Android's optString() would give the TEXT "null".

internal fun JSONObject.str(name: String, default: String = ""): String = if (isNull(name)) default else optString(name, default)
internal fun JSONObject.strOrNull(name: String): String? = if (isNull(name)) null else optString(name).ifEmpty { null }
internal fun JSONObject.long(name: String, default: Long = 0L): Long = if (isNull(name)) default else optLong(name, default)
internal fun JSONObject.longOrNull(name: String): Long? = if (isNull(name)) null else optLong(name)
internal fun JSONObject.int(name: String, default: Int = 0): Int = if (isNull(name)) default else optInt(name, default)
internal fun JSONObject.intOrNull(name: String): Int? = if (isNull(name)) null else optInt(name)
internal fun JSONObject.double(name: String, default: Double = 0.0): Double = if (isNull(name)) default else optDouble(name, default)
internal fun JSONObject.bool(name: String, default: Boolean = false): Boolean = if (isNull(name)) default else optBoolean(name, default)

internal fun JSONObject.objects(name: String): List<JSONObject> = optJSONArray(name).objects()

internal fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    val out = ArrayList<JSONObject>(length())
    for (i in 0 until length()) optJSONObject(i)?.let { out.add(it) }
    return out
}

internal fun JSONObject.longs(name: String): List<Long> {
    val a = optJSONArray(name) ?: return emptyList()
    val out = ArrayList<Long>(a.length())
    for (i in 0 until a.length()) out.add(a.optLong(i))
    return out
}

internal fun JSONObject.strings(name: String): List<String> {
    val a = optJSONArray(name) ?: return emptyList()
    val out = ArrayList<String>(a.length())
    for (i in 0 until a.length()) out.add(a.optString(i))
    return out
}
