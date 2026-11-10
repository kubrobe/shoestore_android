package uz.shushop.app.util

/** The same rules the server applies, so the app can say "check your email" before it sends anything. */
object Validators {
    private val email = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    private val phone = Regex("^[+\\d][\\d\\s\\-()]{6,19}$")

    fun isValidEmail(value: String): Boolean = value.trim().length <= 254 && email.matches(value.trim())

    /** The server needs 8 to 72 characters (72 is bcrypt's limit). */
    fun isValidPassword(value: String): Boolean = value.length in 8..72

    fun isValidName(value: String): Boolean = value.trim().length in 2..60

    fun isValidPhone(value: String): Boolean = phone.matches(value.trim())

    fun isValidAddressLine(value: String): Boolean = value.trim().length in 5..200
}
