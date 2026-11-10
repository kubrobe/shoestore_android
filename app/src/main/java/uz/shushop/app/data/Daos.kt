package uz.shushop.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// A DAO (Data Access Object) lists the SQL the app can run.
// Flow<...> = live data: the screen redraws by itself when a table changes.
// suspend = runs in a coroutine, never blocks the screen.

@Dao
interface ShoeDao {
    @Transaction
    @Query("SELECT * FROM shoes ORDER BY id")
    fun observeAll(): Flow<List<ShoeWithVariants>>

    @Transaction
    @Query("SELECT * FROM shoes WHERE id = :id")
    fun observe(id: Long): Flow<ShoeWithVariants?>

    @Query("SELECT shoeId FROM favourites")
    fun observeFavouriteIds(): Flow<List<Long>>

    @Query("SELECT shoeId FROM stock_alerts")
    fun observeAlertIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavourite(favourite: Favourite)

    @Query("DELETE FROM favourites WHERE shoeId = :shoeId")
    suspend fun removeFavourite(shoeId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAlert(alert: StockAlert)

    @Query("DELETE FROM stock_alerts WHERE shoeId = :shoeId")
    suspend fun removeAlert(shoeId: Long)

    @Query("SELECT COUNT(*) FROM shoes")
    suspend fun count(): Int
}

@Dao
abstract class CartDao {
    @Query("SELECT * FROM cart_items ORDER BY rowid")
    abstract fun observe(): Flow<List<CartItem>>

    @Query("DELETE FROM cart_items")
    abstract suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<CartItem>)

    /** The server answers every cart change with the whole cart: swap the saved copy for it in one step. */
    @Transaction
    open suspend fun replaceWith(items: List<CartItem>) {
        clear()
        insertAll(items)
    }
}

@Dao
abstract class OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC, id DESC")
    abstract fun observeAll(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    abstract fun observe(id: Long): Flow<OrderWithItems?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrder(order: Order)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(items: List<OrderItem>)

    @Query("DELETE FROM cart_items")
    abstract suspend fun clearCart()

    // Takes the pairs off the saved stock; never goes below zero.
    @Query("UPDATE variants SET stock = MAX(stock - :qty, 0) WHERE id = :variantId")
    abstract suspend fun decreaseStock(variantId: Long, qty: Int)

    @Query("UPDATE variants SET stock = stock + :qty WHERE id = :variantId")
    abstract suspend fun increaseStock(variantId: Long, qty: Int)

    /**
     * The phone's side of checkout. The server has already confirmed the order in ITS transaction;
     * here, ALL of this happens together or NOTHING does: save the order and its items, take the pairs off the
     * saved stock and empty the cart. The screens never see a half-finished checkout.
     */
    @Transaction
    open suspend fun savePlacedOrder(order: Order, items: List<OrderItem>) {
        insertOrder(order)
        insertItems(items)
        for (item in items) {
            if (item.variantId != null) decreaseStock(item.variantId, item.qty)
        }
        clearCart()
    }

    /** A cancelled order puts its pairs back in the saved stock and shows the new status. */
    @Transaction
    open suspend fun saveCancelledOrder(order: Order, items: List<OrderItem>) {
        insertOrder(order)
        for (item in items) {
            if (item.variantId != null) increaseStock(item.variantId, item.qty)
        }
    }
}

@Dao
abstract class AccountDao {
    @Query("SELECT * FROM users LIMIT 1")
    abstract fun observeUser(): Flow<User?>

    @Query("SELECT id FROM users LIMIT 1")
    abstract suspend fun currentUserId(): Long?

    @Query("SELECT * FROM addresses ORDER BY isDefault DESC, id")
    abstract fun observeAddresses(): Flow<List<Address>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveAddresses(addresses: List<Address>)

    @Query("DELETE FROM addresses")
    abstract suspend fun clearAddresses()

    /** The server answers every address change with the full list: swap the saved copy for it in one step. */
    @Transaction
    open suspend fun replaceAddresses(addresses: List<Address>) {
        clearAddresses()
        saveAddresses(addresses)
    }
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<NotificationItem>>

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)
}
