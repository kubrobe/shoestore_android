package uz.shushop.app.util

/** The life of an order, same words as the website: New -> Packing -> On the way -> Delivered (or Cancelled). */
object OrderStatus {
    const val NEW = "new"
    const val PROCESSING = "processing"
    const val SHIPPED = "shipped"
    const val DELIVERED = "delivered"
    const val CANCELLED = "cancelled"

    /** The happy path, in order; "cancelled" is a side exit and is not on it. */
    val PATH = listOf(NEW, PROCESSING, SHIPPED, DELIVERED)

    fun label(status: String): String = when (status) {
        NEW -> "New"
        PROCESSING -> "Packing"
        SHIPPED -> "On the way"
        DELIVERED -> "Delivered"
        CANCELLED -> "Cancelled"
        else -> status.replaceFirstChar { it.uppercase() }
    }

    /** A customer can cancel until the parcel is on its way (the server enforces the same rule). */
    fun canCancel(status: String): Boolean = status == NEW || status == PROCESSING

    /** Orders that are still moving (shown first, in the profile's "active orders"). */
    fun isActive(status: String): Boolean = status == NEW || status == PROCESSING || status == SHIPPED

    /** 0..3 for the progress bar, or -1 for a cancelled order. */
    fun step(status: String): Int = if (status == CANCELLED) -1 else PATH.indexOf(status).coerceAtLeast(0)

    fun paymentLabel(method: String): String = when (method) {
        "cash" -> "Cash on delivery"
        "card" -> "Card on delivery"
        "payme" -> "Payme"
        "click" -> "Click"
        else -> method.replaceFirstChar { it.uppercase() }
    }
}
