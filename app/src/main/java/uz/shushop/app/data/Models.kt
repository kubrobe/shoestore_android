package uz.shushop.app.data

/** The shop's rules, sent by the server so the phone never hard-codes them. */
data class ShopMeta(
    val currency: String,
    val shippingFee: Long,
    val freeShippingFrom: Long,
    val maxQtyPerItem: Int,
    val paymentMethods: List<String>,
    val orderStatuses: List<String>,
    val sizeMin: Int,
    val sizeMax: Int,
) {
    companion object {
        /** Used until the first sync (and offline before it): the same numbers the server starts with. */
        val DEFAULT = ShopMeta(
            currency = "so'm", shippingFee = 25_000, freeShippingFrom = 500_000, maxQtyPerItem = 10,
            paymentMethods = listOf("cash", "card", "payme", "click"),
            orderStatuses = listOf("new", "processing", "shipped", "delivered", "cancelled"),
            sizeMin = 27, sizeMax = 46,
        )
    }
}

/** What the server says the cart will cost with a coupon (POST /api/checkout/quote). */
data class Quote(val subtotal: Long, val discount: Long, val shipping: Long, val total: Long, val couponCode: String?, val couponPercent: Int)

data class Review(val id: Long, val rating: Int, val comment: String, val author: String, val mine: Boolean, val createdAt: Long)

/** One shoe's page: the shoe, its sizes, and the live parts (reviews, my alert) which are only fetched when the page opens. */
data class ProductDetail(
    val shoe: ShoeModel,
    val variants: List<ShoeVariant>,
    val isFavourite: Boolean,
    val hasAlert: Boolean,
    val canReview: Boolean,
    val ratingBreakdown: Map<Int, Int>,
    val reviews: List<Review>,
)

/** One step of an order's life: "shipped" at 10:30. */
data class StatusStep(val status: String, val at: Long)

data class OrderDetail(val order: Order, val items: List<OrderItem>, val history: List<StatusStep>)

data class PlacedOrder(val order: Order, val items: List<OrderItem>)

data class AuthResult(val token: String, val user: User)

data class AddressInput(val label: String, val line: String, val city: String, val phone: String, val isDefault: Boolean)

/** The server answered with an error. [message] is written for the customer ("Only 2 left of Urban Step ..."). */
class ApiException(val status: Int, message: String) : Exception(message) {
    val isUnauthorized: Boolean get() = status == 401
}

/** The phone could not reach the server (no internet, wrong address, timeout). */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
