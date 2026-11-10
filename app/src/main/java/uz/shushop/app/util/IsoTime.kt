package uz.shushop.app.util

/**
 * The server sends every time as text like "2026-11-10T07:50:30.846Z". This turns it into epoch milliseconds
 * without java.time (which needs Android 8+; this app supports Android 7).
 * Returns 0 for null or text it cannot read, so a bad value never crashes a screen.
 */
fun parseIsoMillis(text: String?): Long {
    if (text == null) return 0L
    val m = ISO.matchEntire(text.trim()) ?: return 0L
    val (y, mo, d, h, mi, s) = m.destructured
    val fraction = m.groupValues[7]
    val zone = m.groupValues[8]
    val millis = if (fraction.isEmpty()) 0L else (fraction + "000").substring(0, 3).toLong()
    val days = daysFromCivil(y.toInt(), mo.toInt(), d.toInt())
    var total = ((days * 24 + h.toInt()) * 60 + mi.toInt()) * 60L + s.toInt()
    if (zone.isNotEmpty() && zone != "Z") {                     // "+05:00" means the clock is 5 hours ahead of UTC
        val sign = if (zone[0] == '-') -1 else 1
        val digits = zone.drop(1).replace(":", "")
        total -= sign * (digits.substring(0, 2).toInt() * 3_600 + digits.substring(2, 4).toInt() * 60)
    }
    return total * 1000 + millis
}

private val ISO = Regex("""(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,9}))?(Z|[+-]\d{2}:?\d{2})?""")

// Days since 1970-01-01 for a calendar date (Howard Hinnant's algorithm).
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146_097L + doe - 719_468L
}
