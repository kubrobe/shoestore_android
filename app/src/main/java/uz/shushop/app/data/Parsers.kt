package uz.shushop.app.data

import org.json.JSONException
import org.json.JSONObject
import uz.shushop.app.util.parseIsoMillis

/**
 * Turns the server's JSON into the app's own classes. Pure functions (no Android, no network), so they are
 * unit-tested against real server answers saved in src/test/resources/fixtures.
 */
object Parsers {

    fun user(o: JSONObject): User = User(
        id = o.long("id"),
        email = o.str("email"),
        fullName = o.str("fullName"),
        phone = o.str("phone"),
        role = o.str("role", "customer"),
        shoeSizeEu = o.intOrNull("shoeSizeEu"),
        memberSince = parseIsoMillis(o.strOrNull("memberSince")),
    )

    fun meta(o: JSONObject?): ShopMeta {
        if (o == null) return ShopMeta.DEFAULT
        val d = ShopMeta.DEFAULT
        val range = o.optJSONObject("sizeRange")
        return ShopMeta(
            currency = o.str("currency", d.currency),
            shippingFee = o.long("shippingFee", d.shippingFee),
            freeShippingFrom = o.long("freeShippingFrom", d.freeShippingFrom),
            maxQtyPerItem = o.int("maxQtyPerItem", d.maxQtyPerItem),
            paymentMethods = o.strings("paymentMethods").ifEmpty { d.paymentMethods },
            orderStatuses = o.strings("orderStatuses").ifEmpty { d.orderStatuses },
            sizeMin = range?.int("min", d.sizeMin) ?: d.sizeMin,
            sizeMax = range?.int("max", d.sizeMax) ?: d.sizeMax,
        )
    }

    /** A shoe card (the list) or a whole shoe page: both carry the same shoe fields. */
    fun shoe(o: JSONObject): ShoeModel = ShoeModel(
        id = o.long("id"),
        name = o.str("name"),
        brand = o.str("brand"),
        type = o.str("type"),
        category = o.str("category", "unisex"),
        description = o.str("description"),
        price = o.long("price"),
        oldPrice = o.longOrNull("oldPrice"),
        artStyle = o.str("artStyle", "court"),
        imageUrl = o.strOrNull("imageUrl"),
        avgRating = o.double("avgRating"),
        reviewCount = o.int("reviewCount"),
        createdAt = parseIsoMillis(o.strOrNull("createdAt")),
    )

    fun variants(o: JSONObject): List<ShoeVariant> = o.objects("variants").map {
        ShoeVariant(
            id = it.long("id"), shoeId = it.long("productId"), size = it.int("size"),
            colorName = it.str("colorName"), colorHex = it.str("colorHex", "#cccccc"), stock = it.int("stock"),
        )
    }

    fun cartItem(o: JSONObject): CartItem = CartItem(
        variantId = o.long("variantId"), shoeId = o.long("productId"), name = o.str("name"), brand = o.str("brand"),
        size = o.int("size"), colorName = o.str("colorName"), colorHex = o.str("colorHex", "#cccccc"),
        artStyle = o.str("artStyle", "court"), imageUrl = o.strOrNull("imageUrl"), unitPrice = o.long("unitPrice"),
        qty = o.int("qty", 1), stock = o.int("stock"), available = o.bool("available", true),
    )

    /** The answer of every cart call: {items:[...], count, subtotal, hasUnavailable}. */
    fun cart(o: JSONObject): List<CartItem> = o.objects("items").map(::cartItem)

    fun address(o: JSONObject, userId: Long): Address = Address(
        id = o.long("id"), userId = userId, label = o.str("label", "Home"), line = o.str("line"),
        city = o.str("city"), phone = o.str("phone"), isDefault = o.bool("isDefault"),
    )

    fun order(o: JSONObject, userId: Long): Order = Order(
        id = o.long("id"), userId = userId, status = o.str("status", "new"),
        subtotal = o.long("subtotal"), shippingFee = o.long("shippingFee"), discount = o.long("discount"), total = o.long("total"),
        couponCode = o.strOrNull("couponCode"), addressLine = o.str("addressLine"), city = o.str("city"), phone = o.str("phone"),
        paymentMethod = o.str("paymentMethod", "cash"),
        createdAt = parseIsoMillis(o.strOrNull("createdAt")), updatedAt = parseIsoMillis(o.strOrNull("updatedAt")),
    )

    fun orderItems(o: JSONObject): List<OrderItem> {
        val orderId = o.long("id")
        return o.objects("items").map {
            OrderItem(
                id = it.long("id"), orderId = orderId, shoeId = it.longOrNull("productId"), variantId = it.longOrNull("variantId"),
                name = it.str("name"), brand = it.str("brand"), size = it.int("size"),
                colorName = it.str("colorName"), colorHex = it.str("colorHex", "#cccccc"), artStyle = it.str("artStyle", "court"),
                unitPrice = it.long("unitPrice"), qty = it.int("qty", 1),
            )
        }
    }

    fun notification(o: JSONObject): NotificationItem = NotificationItem(
        id = o.long("id"), kind = o.str("kind", "drop"), title = o.str("title"), body = o.str("body"),
        shoeId = o.longOrNull("productId"), orderId = o.longOrNull("orderId"), isRead = o.bool("isRead"),
        createdAt = parseIsoMillis(o.strOrNull("createdAt")),
    )

    /** GET /api/mobile/sync */
    fun snapshot(root: JSONObject): Snapshot {
        val user = user(root.getJSONObject("user"))
        val products = root.objects("products").filter { it.bool("isActive", true) }
        val shoes = products.map(::shoe)
        val shoeIds = shoes.map { it.id }.toSet()
        val orders = root.objects("orders")
        return Snapshot(
            syncedAt = parseIsoMillis(root.strOrNull("syncedAt")),
            user = user,
            shoes = shoes,
            variants = products.flatMap(::variants),
            favouriteIds = products.filter { it.bool("isFavourite") }.map { it.long("id") },
            // an alert on a shoe the shop has hidden has no row to point to, so it is left out
            alertIds = root.longs("stockAlertProductIds").filter { it in shoeIds },
            cart = root.optJSONObject("cart")?.let(::cart).orEmpty(),
            addresses = root.objects("addresses").map { address(it, user.id) },
            orders = orders.map { order(it, user.id) },
            orderItems = orders.flatMap(::orderItems),
            notifications = root.objects("notifications").map(::notification),
            meta = meta(root.optJSONObject("meta")),
        )
    }

    /** GET /api/products/:id */
    fun productDetail(root: JSONObject): ProductDetail {
        val p = root.getJSONObject("product")
        val breakdown = p.optJSONObject("ratingBreakdown")
        return ProductDetail(
            shoe = shoe(p),
            variants = variants(p),
            isFavourite = p.bool("isFavourite"),
            hasAlert = p.bool("hasAlert"),
            canReview = p.bool("canReview"),
            ratingBreakdown = (1..5).associateWith { breakdown?.int(it.toString()) ?: 0 },
            reviews = p.objects("reviews").map {
                Review(
                    id = it.long("id"), rating = it.int("rating"), comment = it.str("comment"), author = it.str("author"),
                    mine = it.bool("mine"), createdAt = parseIsoMillis(it.strOrNull("createdAt")),
                )
            },
        )
    }

    /** GET /api/orders/:id and the answers of checkout and cancel: {order:{..., items, history?}} */
    fun orderDetail(root: JSONObject, userId: Long): OrderDetail {
        val o = root.getJSONObject("order")
        return OrderDetail(
            order = order(o, userId),
            items = orderItems(o),
            history = o.objects("history").map { StatusStep(it.str("status"), parseIsoMillis(it.strOrNull("at"))) },
        )
    }

    fun quote(root: JSONObject): Quote {
        val coupon = root.optJSONObject("coupon")
        return Quote(
            subtotal = root.long("subtotal"), discount = root.long("discount"), shipping = root.long("shipping"), total = root.long("total"),
            couponCode = coupon?.strOrNull("code"), couponPercent = coupon?.int("percent") ?: 0,
        )
    }

    fun auth(root: JSONObject): AuthResult = AuthResult(root.getString("token"), user(root.getJSONObject("user")))

    /** {"error":"Wrong email or password"} -> the sentence; anything else -> [fallback]. */
    fun errorMessage(body: String?, fallback: String): String {
        if (body.isNullOrBlank()) return fallback
        return try {
            JSONObject(body).strOrNull("error") ?: fallback
        } catch (e: JSONException) {
            fallback
        }
    }
}
