package uz.shushop.app.util

/** What the customer pays. The server always has the last word (checkout quote); this is the same maths for instant feedback. */
data class PriceBreakdown(val subtotal: Long, val discount: Long, val shipping: Long, val total: Long)

object Pricing {
    const val DEFAULT_SHIPPING_FEE = 25_000L
    const val DEFAULT_FREE_SHIPPING_FROM = 500_000L

    /**
     * Subtotal minus coupon percent; delivery is free from [freeFrom] (counted AFTER the discount).
     * The discount is rounded down to a whole so'm, exactly like the server.
     */
    fun price(
        subtotal: Long,
        couponPercent: Int = 0,
        shippingFee: Long = DEFAULT_SHIPPING_FEE,
        freeFrom: Long = DEFAULT_FREE_SHIPPING_FROM,
    ): PriceBreakdown {
        val discount = subtotal * couponPercent / 100
        val afterDiscount = subtotal - discount
        val shipping = if (subtotal == 0L || afterDiscount >= freeFrom) 0L else shippingFee
        return PriceBreakdown(subtotal, discount, shipping, afterDiscount + shipping)
    }

    /** How much more to add for free delivery, or 0 when it is already free. */
    fun toFreeShipping(subtotal: Long, discount: Long, freeFrom: Long = DEFAULT_FREE_SHIPPING_FROM): Long =
        maxOf(0L, freeFrom - (subtotal - discount))
}
