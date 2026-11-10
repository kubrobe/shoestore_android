package uz.shushop.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 650000 -> "650 000 so'm". The same format as the website. */
fun formatSom(amount: Long): String {
    val digits = Math.abs(amount).toString()
    val out = StringBuilder()
    digits.forEachIndexed { i, c ->
        if (i > 0 && (digits.length - i) % 3 == 0) out.append(' ')
        out.append(c)
    }
    return (if (amount < 0) "-" else "") + out + " so'm"
}

/** 1 -> "1 pair", 3 -> "3 pairs" */
fun pairs(n: Int): String = if (n == 1) "1 pair" else "$n pairs"

/** Epoch millis -> "10 Nov 2026" (in the phone's time zone unless [zone] is given). */
fun formatDate(millis: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val f = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
    f.timeZone = zone
    return f.format(Date(millis))
}

/** Epoch millis -> "10 Nov, 12:39" */
fun formatDateTime(millis: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val f = SimpleDateFormat("d MMM, HH:mm", Locale.ENGLISH)
    f.timeZone = zone
    return f.format(Date(millis))
}

/** "just now", "5 min ago", "3 h ago", "2 days ago", and after a week the date. */
fun timeAgo(thenMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val seconds = (nowMillis - thenMillis) / 1000
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60} min ago"
        seconds < 86_400 -> "${seconds / 3_600} h ago"
        seconds < 7 * 86_400 -> {
            val days = seconds / 86_400
            if (days == 1L) "1 day ago" else "$days days ago"
        }
        else -> formatDate(thenMillis)
    }
}

/** "Alex Morgan" -> "AM", "alex" -> "A" */
fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }
