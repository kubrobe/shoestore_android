package uz.shushop.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.shushop.app.data.Address
import uz.shushop.app.data.AddressInput
import uz.shushop.app.data.ApiException
import uz.shushop.app.data.CartItem
import uz.shushop.app.data.NetworkException
import uz.shushop.app.data.NotificationItem
import uz.shushop.app.data.OrderWithItems
import uz.shushop.app.data.ProductDetail
import uz.shushop.app.data.Quote
import uz.shushop.app.data.ShoeFilter
import uz.shushop.app.data.ShoeWithVariants
import uz.shushop.app.data.ShopMeta
import uz.shushop.app.data.ShopRepository
import uz.shushop.app.data.StatusStep
import uz.shushop.app.data.User
import uz.shushop.app.util.PriceBreakdown
import uz.shushop.app.util.Pricing
import uz.shushop.app.util.normalizeServerUrl

/**
 * MVVM: the screens only SHOW what is in here and CALL what is in here.
 * Data flows up from Room as StateFlows (the screens redraw by themselves); user actions go down to the repository.
 * Errors from the server are turned into short messages ([messages]) that the screen shows as a snackbar.
 */
class ShopViewModel(private val repo: ShopRepository) : ViewModel() {

    // ---------------------------------------------------------------- what the screens show

    val loggedIn: StateFlow<Boolean> = repo.loggedIn
    val meta: StateFlow<ShopMeta> = repo.meta

    val shoes: StateFlow<List<ShoeWithVariants>> = state(repo.shoes, emptyList())
    val favouriteIds: StateFlow<Set<Long>> = state(repo.favouriteIds.map { it.toSet() }, emptySet())
    val alertIds: StateFlow<Set<Long>> = state(repo.alertIds.map { it.toSet() }, emptySet())
    val cart: StateFlow<List<CartItem>> = state(repo.cart, emptyList())
    val orders: StateFlow<List<OrderWithItems>> = state(repo.orders, emptyList())
    val notifications: StateFlow<List<NotificationItem>> = state(repo.notifications, emptyList())
    val user: StateFlow<User?> = state(repo.user, null)
    val addresses: StateFlow<List<Address>> = state(repo.addresses, emptyList())

    val unreadCount: StateFlow<Int> = state(repo.notifications.map { list -> list.count { !it.isRead } }, 0)
    val cartCount: StateFlow<Int> = state(repo.cart.map { list -> list.sumOf { it.qty } }, 0)
    val brands: StateFlow<List<String>> = state(repo.shoes.map { list -> list.map { it.shoe.brand }.distinct().sorted() }, emptyList())

    /** The shop screen's search box, chip and filter sheet. */
    val filter = MutableStateFlow(ShoeFilter())
    val shown: StateFlow<List<ShoeWithVariants>> = state(combine(repo.shoes, filter) { all, f -> f.apply(all) }, emptyList())

    /** Wishlist = the saved shoes, newest save order is not stored, so they are listed by name. */
    val wishlist: StateFlow<List<ShoeWithVariants>> =
        state(combine(repo.shoes, repo.favouriteIds) { all, ids -> all.filter { it.shoe.id in ids }.sortedBy { it.shoe.name } }, emptyList())

    // ---------------------------------------------------------------- small status flags

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** One-line messages for the snackbar ("Added to your cart", the server's error sentences...). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val refreshing = MutableStateFlow(false)
    /** true when the last attempt to reach the server failed: the screens show the saved copy and say so. */
    val offline = MutableStateFlow(false)
    /** true while a button that talks to the server is working (login, confirm order...). */
    val working = MutableStateFlow(false)
    /** Set when the server rejects the login form, shown under the fields. */
    val authError = MutableStateFlow<String?>(null)

    private fun <T> state(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    fun say(text: String) { _messages.tryEmit(text) }

    /**
     * Runs one server call. A server answer that says no ("Only 2 left...") and a missing connection both end up
     * as a message; [onError] lets a form show it next to its fields instead.
     */
    private fun call(
        busy: Boolean = true,
        onError: ((String) -> Unit)? = null,
        onSuccess: () -> Unit = {},
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            if (busy) working.value = true
            try {
                block()
                offline.value = false
                onSuccess()
            } catch (e: ApiException) {
                report(onError, e.message ?: "Something went wrong")
            } catch (e: NetworkException) {
                offline.value = true
                report(onError, e.message ?: "No connection")
            } finally {
                if (busy) working.value = false
            }
        }
    }

    private fun report(onError: ((String) -> Unit)?, text: String) {
        if (onError != null) onError(text) else say(text)
    }

    // ---------------------------------------------------------------- account

    fun login(email: String, password: String) {
        authError.value = null
        call(onError = { authError.value = it }) { repo.login(email, password) }
    }

    fun register(fullName: String, email: String, phone: String, password: String) {
        authError.value = null
        call(onError = { authError.value = it }) { repo.register(fullName, email, phone, password) }
    }

    fun clearAuthError() { authError.value = null }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            resetScreens()
        }
    }

    private fun resetScreens() {
        filter.value = ShoeFilter()
        coupon.value = null
        reviews.value = null
        history.value = emptyList()
    }

    fun updateProfile(fullName: String, phone: String, shoeSize: Int?, onDone: () -> Unit) =
        call(onSuccess = { say("Profile saved"); onDone() }) { repo.updateProfile(fullName, phone, shoeSize) }

    fun changePassword(current: String, next: String, onDone: () -> Unit, onError: (String) -> Unit) =
        call(onError = onError, onSuccess = { say("Password changed"); onDone() }) { repo.changePassword(current, next) }

    fun saveAddress(id: Long?, input: AddressInput, onDone: () -> Unit, onError: (String) -> Unit) =
        call(onError = onError, onSuccess = { say(if (id == null) "Address added" else "Address saved"); onDone() }) { repo.saveAddress(id, input) }

    fun deleteAddress(id: Long) = call(onSuccess = { say("Address removed") }) { repo.deleteAddress(id) }

    val serverUrl: String get() = repo.serverUrl
    val hasCustomServer: Boolean get() = repo.hasCustomServer

    /** Saves a server address typed by hand. Returns false (and changes nothing) when it is not an address. */
    fun changeServer(input: String): Boolean {
        val url = normalizeServerUrl(input) ?: return false
        if (url == repo.serverUrl) return true
        viewModelScope.launch {
            if (repo.loggedIn.value) { repo.logout(); resetScreens() }      // a login is only valid on the server that gave it
            repo.setServerUrl(url)
            say("Server set to $url")
        }
        return true
    }

    fun resetServer() {
        viewModelScope.launch {
            if (repo.loggedIn.value) { repo.logout(); resetScreens() }
            repo.resetServerUrl()
            say("Back to the default server")
        }
    }

    // ---------------------------------------------------------------- shop

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            try {
                repo.sync()
                offline.value = false
            } catch (e: ApiException) {
                if (!e.isUnauthorized) say(e.message ?: "Could not refresh")
            } catch (e: NetworkException) {
                offline.value = true
            } finally {
                refreshing.value = false
            }
        }
    }

    fun setQuery(text: String) { filter.value = filter.value.copy(query = text) }
    fun setCategory(category: String) { filter.value = filter.value.copy(category = category) }
    fun setFilter(f: ShoeFilter) { filter.value = f }

    fun toggleFavourite(shoeId: Long) {
        val on = shoeId !in favouriteIds.value
        call(busy = false, onSuccess = {}) { repo.setFavourite(shoeId, on) }
    }

    fun toggleAlert(shoeId: Long) {
        val on = shoeId !in alertIds.value
        call(busy = false, onSuccess = { say(if (on) "We'll tell you when it's back" else "Alert switched off") }) { repo.setAlert(shoeId, on) }
    }

    // ---------------------------------------------------------------- one shoe (live reviews)

    /** Reviews are live data, fetched when the page opens. null = not loaded (yet, or offline). */
    val reviews = MutableStateFlow<ProductDetail?>(null)

    fun openShoe(shoeId: Long) {
        reviews.value = null
        viewModelScope.launch {
            try {
                reviews.value = repo.loadDetail(shoeId)
            } catch (e: ApiException) {
            } catch (e: NetworkException) {
            }
        }
    }

    fun postReview(shoeId: Long, rating: Int, comment: String, onDone: () -> Unit, onError: (String) -> Unit) =
        call(onError = onError, onSuccess = { say("Thanks for your review"); onDone(); openShoe(shoeId) }) { repo.postReview(shoeId, rating, comment) }

    // ---------------------------------------------------------------- cart and checkout

    fun addToCart(variantId: Long, qty: Int, onDone: () -> Unit = {}) =
        call(onSuccess = onDone) { repo.addToCart(variantId, qty) }

    fun setCartQty(variantId: Long, qty: Int) = call(busy = false) { repo.setCartQty(variantId, qty) }
    fun removeFromCart(variantId: Long) = call(busy = false) { repo.removeFromCart(variantId) }

    /** The coupon the server accepted, with the percent it takes off. null = none. */
    val coupon = MutableStateFlow<Quote?>(null)
    val couponError = MutableStateFlow<String?>(null)

    fun applyCoupon(code: String) {
        couponError.value = null
        if (code.isBlank()) { coupon.value = null; return }
        call(onError = { couponError.value = it }) {
            val q = repo.quote(code)
            coupon.value = q
        }
    }

    fun clearCoupon() { coupon.value = null; couponError.value = null }

    /** What the cart costs right now with the coupon (the same maths as the server; the server confirms at checkout). */
    val price: StateFlow<PriceBreakdown> = state(
        combine(repo.cart, coupon, repo.meta) { items, q, m ->
            Pricing.price(items.sumOf { it.lineTotal }, q?.couponPercent ?: 0, m.shippingFee, m.freeShippingFrom)
        },
        Pricing.price(0),
    )

    fun placeOrder(addressId: Long, payment: String, onPlaced: (Long) -> Unit) =
        call(onSuccess = {}) {
            val id = repo.placeOrder(addressId, payment, coupon.value?.couponCode)
            coupon.value = null
            say("Order #$id placed")
            onPlaced(id)
        }

    // ---------------------------------------------------------------- orders

    /** The status steps of the order that is open (live, from the server). */
    val history = MutableStateFlow<List<StatusStep>>(emptyList())

    fun openOrder(orderId: Long) {
        history.value = emptyList()
        viewModelScope.launch {
            try {
                history.value = repo.loadOrderDetail(orderId).history
            } catch (e: ApiException) {
            } catch (e: NetworkException) {
            }
        }
    }

    fun order(id: Long) = repo.order(id)
    fun shoe(id: Long) = repo.shoe(id)

    fun cancelOrder(orderId: Long) =
        call(onSuccess = { say("Order cancelled"); openOrder(orderId) }) { repo.cancelOrder(orderId) }

    // ---------------------------------------------------------------- notifications

    fun openNotification(n: NotificationItem) {
        if (!n.isRead) call(busy = false) { repo.markNotificationRead(n.id) }
    }

    fun markAllRead() = call(busy = false) { repo.markAllNotificationsRead() }

    init {
        if (repo.loggedIn.value) refresh()      // reopening the app: show the saved copy now, update it in the background
    }

    companion object {
        fun factory(repo: ShopRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ShopViewModel(repo) as T
        }
    }
}
