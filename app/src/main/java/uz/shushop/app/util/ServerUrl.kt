package uz.shushop.app.util

/**
 * Cleans an address typed by hand: "shoestore.onrender.com/" -> "https://shoestore.onrender.com".
 * Returns null when it cannot be an address. http:// is allowed (for a server on the same Wi-Fi), but plain
 * words without a dot are only accepted for "localhost" and IP addresses.
 */
fun normalizeServerUrl(input: String): String? {
    var text = input.trim().trimEnd('/')
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null
    if (!text.contains("://")) {
        val isLocal = text.startsWith("localhost") || text.startsWith("10.") || text.startsWith("192.168.") || text.startsWith("127.")
        text = (if (isLocal) "http://" else "https://") + text
    }
    val match = Regex("^(https?)://([A-Za-z0-9.-]+)(:\\d{1,5})?(/[^\\s]*)?$").matchEntire(text) ?: return null
    val host = match.groupValues[2]
    val ok = host == "localhost" || host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")) || (host.contains('.') && !host.startsWith('.') && !host.endsWith('.'))
    return if (ok) text else null
}
