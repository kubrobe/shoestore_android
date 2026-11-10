package uz.shushop.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/** Everything the server sends in one answer (GET /api/mobile/sync), ready to be written into Room. */
class Snapshot(
    val syncedAt: Long,
    val user: User,
    val shoes: List<ShoeModel>,
    val variants: List<ShoeVariant>,
    val favouriteIds: List<Long>,
    val alertIds: List<Long>,
    val cart: List<CartItem>,
    val addresses: List<Address>,
    val orders: List<Order>,
    val orderItems: List<OrderItem>,
    val notifications: List<NotificationItem>,
    val meta: ShopMeta,
)

/**
 * Replaces the phone's copy of the data with what the server sent.
 * It is ONE @Transaction: the screens never see a half-updated database, and if anything fails the old copy stays.
 * Order matters because of the foreign keys: children are deleted first and parents are inserted first.
 */
@Dao
abstract class SyncDao {
    @Query("DELETE FROM notifications") abstract suspend fun clearNotifications()
    @Query("DELETE FROM order_items") abstract suspend fun clearOrderItems()
    @Query("DELETE FROM orders") abstract suspend fun clearOrders()
    @Query("DELETE FROM addresses") abstract suspend fun clearAddresses()
    @Query("DELETE FROM cart_items") abstract suspend fun clearCart()
    @Query("DELETE FROM favourites") abstract suspend fun clearFavourites()
    @Query("DELETE FROM stock_alerts") abstract suspend fun clearAlerts()
    @Query("DELETE FROM variants") abstract suspend fun clearVariants()
    @Query("DELETE FROM shoes") abstract suspend fun clearShoes()
    @Query("DELETE FROM users") abstract suspend fun clearUsers()

    @Insert abstract suspend fun insertUser(user: User)
    @Insert abstract suspend fun insertShoes(items: List<ShoeModel>)
    @Insert abstract suspend fun insertVariants(items: List<ShoeVariant>)
    @Insert abstract suspend fun insertFavourites(items: List<Favourite>)
    @Insert abstract suspend fun insertAlerts(items: List<StockAlert>)
    @Insert abstract suspend fun insertCart(items: List<CartItem>)
    @Insert abstract suspend fun insertAddresses(items: List<Address>)
    @Insert abstract suspend fun insertOrders(items: List<Order>)
    @Insert abstract suspend fun insertOrderItems(items: List<OrderItem>)
    @Insert abstract suspend fun insertNotifications(items: List<NotificationItem>)

    @Transaction
    open suspend fun clearAll() {
        clearNotifications(); clearOrderItems(); clearOrders(); clearAddresses(); clearCart()
        clearFavourites(); clearAlerts(); clearVariants(); clearShoes(); clearUsers()
    }

    @Transaction
    open suspend fun replaceAll(s: Snapshot) {
        clearAll()
        insertUser(s.user)
        insertShoes(s.shoes)
        insertVariants(s.variants)
        insertFavourites(s.favouriteIds.map { Favourite(it) })
        insertAlerts(s.alertIds.map { StockAlert(it) })
        insertCart(s.cart)
        insertAddresses(s.addresses)
        insertOrders(s.orders)
        insertOrderItems(s.orderItems)
        insertNotifications(s.notifications)
    }
}
