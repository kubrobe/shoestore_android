package uz.shushop.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import uz.shushop.app.remote.ApiClient

/**
 * The one place that knows where data comes from. Screens talk to the ViewModel, the ViewModel talks to this class:
 *
 *   screen  ->  ShopViewModel  ->  ShopRepository  ->  Room DAOs (the phone's copy)  +  ApiClient (the server)
 *
 * READING always comes from Room (as Flows), so every screen works offline with the last copy.
 * WRITING goes to the server first (it is the boss: stock, prices and coupons are checked there); the answer is
 * then written into Room in one @Transaction. If the server says no, the screen gets the server's sentence.
 */
class ShopRepository(
    private val db: AppDatabase,
    private val api: ApiClient,
    private val session: SessionStore,
) {
    private val _loggedIn = MutableStateFlow(session.token != null)
    /** false after logout or when the server says the login has expired; the app then shows the login screen. */
    val loggedIn: StateFlow<Boolean> = _loggedIn

    private val _meta = MutableStateFlow(ShopMeta.DEFAULT)
    val meta: StateFlow<ShopMeta> = _meta

    val shoes: Flow<List<ShoeWithVariants>> = db.shoeDao().observeAll()
    fun shoe(id: Long): Flow<ShoeWithVariants?> = db.shoeDao().observe(id)
    val favouriteIds: Flow<List<Long>> = db.shoeDao().observeFavouriteIds()
    val alertIds: Flow<List<Long>> = db.shoeDao().observeAlertIds()
    val cart: Flow<List<CartItem>> = db.cartDao().observe()
    val orders: Flow<List<OrderWithItems>> = db.orderDao().observeAll()
    fun order(id: Long): Flow<OrderWithItems?> = db.orderDao().observe(id)
    val notifications: Flow<List<NotificationItem>> = db.notificationDao().observeAll()
    val user: Flow<User?> = db.accountDao().observeUser()
    val addresses: Flow<List<Address>> = db.accountDao().observeAddresses()

    val serverUrl: String get() = session.serverUrl
    val hasCustomServer: Boolean get() = session.hasCustomServer
    fun setServerUrl(url: String) { session.serverUrl = url }
    fun resetServerUrl() = session.resetServerUrl()

    // ---------------------------------------------------------------- account

    suspend fun login(email: String, password: String) {
        val result = io { Parsers.auth(api.post("/api/mobile/login", json("email" to email.trim(), "password" to password))) }
        startSession(result)
    }

    suspend fun register(fullName: String, email: String, phone: String, password: String) {
        val body = json("fullName" to fullName.trim(), "email" to email.trim(), "phone" to phone.trim(), "password" to password)
        startSession(io { Parsers.auth(api.post("/api/mobile/register", body)) })
    }

    private suspend fun startSession(result: AuthResult) {
        session.token = result.token
        _loggedIn.value = true
        try {
            sync()
        } catch (e: ApiException) {
            if (e.isUnauthorized) throw e        // the screens open with whatever came with the login and refresh by themselves
        } catch (e: NetworkException) {
            // logged in but offline right now: the shop screen offers a retry
        }
    }

    /** Ends the login on the server (best effort), forgets the token and empties the phone's copy. */
    suspend fun logout() {
        try {
            io { api.post("/api/mobile/logout") }
        } catch (e: ApiException) {
        } catch (e: NetworkException) {
        }
        forgetSession()
    }

    private suspend fun forgetSession() {
        session.token = null
        _loggedIn.value = false
        db.syncDao().clearAll()
    }

    suspend fun updateProfile(fullName: String, phone: String, shoeSizeEu: Int?) {
        val body = JSONObject().put("fullName", fullName.trim()).put("phone", phone.trim()).put("shoeSizeEu", shoeSizeEu ?: JSONObject.NULL)
        val u = authed { io { Parsers.user(api.patch("/api/me", body).getJSONObject("user")) } }
        db.accountDao().saveUser(u)
    }

    suspend fun changePassword(current: String, next: String) {
        authed { io { api.post("/api/me/password", json("currentPassword" to current, "newPassword" to next)) } }
    }

    // ---------------------------------------------------------------- sync (server -> phone)

    /** Downloads everything for this customer and replaces the phone's copy in ONE transaction. */
    suspend fun sync() {
        val snapshot = authed { io { Parsers.snapshot(api.get("/api/mobile/sync")) } }
        db.syncDao().replaceAll(snapshot)
        _meta.value = snapshot.meta
    }

    /** Like [sync], but a failure is not an error: used after a purchase, when the phone already shows the right data. */
    private suspend fun trySync() {
        try {
            sync()
        } catch (e: ApiException) {
        } catch (e: NetworkException) {
        }
    }

    suspend fun hasShoes(): Boolean = db.shoeDao().count() > 0

    // ---------------------------------------------------------------- shoe page

    /** Reviews and "can I review" are live: they are fetched when the page opens, not stored. */
    suspend fun loadDetail(shoeId: Long): ProductDetail = authed { io { Parsers.productDetail(api.get("/api/products/$shoeId")) } }

    suspend fun postReview(shoeId: Long, rating: Int, comment: String) {
        authed { io { api.post("/api/products/$shoeId/reviews", JSONObject().put("rating", rating).put("comment", comment.trim())) } }
    }

    suspend fun setFavourite(shoeId: Long, on: Boolean) {
        val dao = db.shoeDao()
        if (on) dao.addFavourite(Favourite(shoeId)) else dao.removeFavourite(shoeId)      // the heart reacts at once
        try {
            authed { io { if (on) api.put("/api/favourites/$shoeId") else api.delete("/api/favourites/$shoeId") } }
        } catch (e: Exception) {
            withContext(NonCancellable) { if (on) dao.removeFavourite(shoeId) else dao.addFavourite(Favourite(shoeId)) }    // the server said no: undo
            throw e
        }
    }

    suspend fun setAlert(shoeId: Long, on: Boolean) {
        val dao = db.shoeDao()
        authed { io { if (on) api.put("/api/products/$shoeId/alert") else api.delete("/api/products/$shoeId/alert") } }
        if (on) dao.addAlert(StockAlert(shoeId)) else dao.removeAlert(shoeId)
    }

    // ---------------------------------------------------------------- cart and checkout

    suspend fun addToCart(variantId: Long, qty: Int) =
        applyCart { api.post("/api/cart/items", JSONObject().put("variantId", variantId).put("qty", qty)) }

    /** Sets the quantity to exactly [qty]; 0 removes the line. */
    suspend fun setCartQty(variantId: Long, qty: Int) =
        applyCart { api.put("/api/cart/items/$variantId", JSONObject().put("qty", qty)) }

    suspend fun removeFromCart(variantId: Long) = applyCart { api.delete("/api/cart/items/$variantId") }

    // The server answers every cart call with the whole cart, so the saved cart is swapped for it in one step.
    private suspend fun applyCart(call: () -> JSONObject) {
        val items = authed { io { Parsers.cart(call()) } }
        db.cartDao().replaceWith(items)
    }

    suspend fun quote(coupon: String?): Quote {
        val body = JSONObject()
        if (!coupon.isNullOrBlank()) body.put("coupon", coupon.trim())
        return authed { io { Parsers.quote(api.post("/api/checkout/quote", body)) } }
    }

    /** Places the order. Returns its id. The server does its all-or-nothing transaction; then the phone does its own. */
    suspend fun placeOrder(addressId: Long, paymentMethod: String, coupon: String?): Long {
        val userId = currentUserId()
        val body = JSONObject().put("addressId", addressId).put("paymentMethod", paymentMethod)
        if (!coupon.isNullOrBlank()) body.put("coupon", coupon.trim())
        val placed = authed { io { Parsers.orderDetail(api.post("/api/checkout", body), userId) } }
        db.orderDao().savePlacedOrder(placed.order, placed.items)
        trySync()
        return placed.order.id
    }

    suspend fun cancelOrder(orderId: Long) {
        val userId = currentUserId()
        val cancelled = authed { io { Parsers.orderDetail(api.post("/api/orders/$orderId/cancel"), userId) } }
        db.orderDao().saveCancelledOrder(cancelled.order, cancelled.items)
        trySync()
    }

    /** One order with its status history (the history is only on the order's own page, so it is fetched live). */
    suspend fun loadOrderDetail(orderId: Long): OrderDetail {
        val userId = currentUserId()
        return authed { io { Parsers.orderDetail(api.get("/api/orders/$orderId"), userId) } }
    }

    // ---------------------------------------------------------------- addresses

    suspend fun saveAddress(id: Long?, input: AddressInput) {
        val body = JSONObject().put("label", input.label.trim()).put("line", input.line.trim())
            .put("city", input.city.trim()).put("phone", input.phone.trim()).put("isDefault", input.isDefault)
        authed { io { if (id == null) api.post("/api/addresses", body) else api.put("/api/addresses/$id", body) } }
        refreshAddresses()
    }

    suspend fun deleteAddress(id: Long) {
        authed { io { api.delete("/api/addresses/$id") } }
        refreshAddresses()
    }

    private suspend fun refreshAddresses() {
        val userId = currentUserId()
        val list = authed { io { api.get("/api/addresses").let { r -> r.objects("addresses").map { Parsers.address(it, userId) } } } }
        db.accountDao().replaceAddresses(list)
    }

    // ---------------------------------------------------------------- notifications

    suspend fun markNotificationRead(id: Long) {
        authed { io { api.post("/api/notifications/$id/read") } }
        db.notificationDao().markRead(id)
    }

    suspend fun markAllNotificationsRead() {
        authed { io { api.post("/api/notifications/read-all") } }
        db.notificationDao().markAllRead()
    }

    // ---------------------------------------------------------------- helpers

    private suspend fun currentUserId(): Long = db.accountDao().currentUserId() ?: 0L

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    /** Runs a server call; if the server says "not logged in", the saved login is dropped and the app shows the login screen. */
    private suspend fun <T> authed(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: ApiException) {
            if (e.isUnauthorized && session.token != null) withContext(NonCancellable) { forgetSession() }
            throw e
        }
    }

    private fun json(vararg pairs: Pair<String, Any?>): JSONObject {
        val o = JSONObject()
        for ((k, v) in pairs) o.put(k, v)
        return o
    }
}
