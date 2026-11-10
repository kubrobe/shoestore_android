package uz.shushop.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// The phone keeps an offline copy of the shop in Room (SQLite). The server (PostgreSQL) is the boss:
// every change goes there first, and the copy is refreshed from its answer. All times are epoch milliseconds,
// all money is whole so'm in a Long.
//
//   User 1---N Address            ShoeModel 1---N ShoeVariant (size + colour + stock)
//   User 1---N Order 1---N OrderItem        ShoeModel 1---1 Favourite / StockAlert
//   CartItem and NotificationItem are flat lists (a cart line keeps its own copy of name and price).

/** The logged-in customer. */
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Long,
    val email: String,
    val fullName: String,
    val phone: String,
    val role: String,
    val shoeSizeEu: Int?,
    val memberSince: Long,
)

/** One shoe in the shop, for example "Urban Step" by Nomad. Sizes and colours are its [ShoeVariant]s. */
@Entity(tableName = "shoes")
data class ShoeModel(
    @PrimaryKey val id: Long,
    val name: String,
    val brand: String,
    val type: String,
    val category: String,          // men, women, kids or unisex
    val description: String,
    val price: Long,
    val oldPrice: Long?,           // set while the shoe is on sale
    val artStyle: String,          // which drawn shoe to show when there is no photo
    val imageUrl: String?,
    val avgRating: Double,
    val reviewCount: Int,
    val createdAt: Long,
) {
    val onSale: Boolean get() = oldPrice != null && oldPrice > price
    val discountPercent: Int
        get() = if (oldPrice != null && oldPrice > price) (((oldPrice - price) * 100.0) / oldPrice).let { Math.round(it).toInt() } else 0
}

/** One size in one colour of a shoe, with how many pairs are left. */
@Entity(
    tableName = "variants",
    foreignKeys = [ForeignKey(entity = ShoeModel::class, parentColumns = ["id"], childColumns = ["shoeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("shoeId")],
)
data class ShoeVariant(
    @PrimaryKey val id: Long,
    val shoeId: Long,
    val size: Int,
    val colorName: String,
    val colorHex: String,
    val stock: Int,
)

/** A saved (wishlist) shoe. */
@Entity(
    tableName = "favourites",
    foreignKeys = [ForeignKey(entity = ShoeModel::class, parentColumns = ["id"], childColumns = ["shoeId"], onDelete = ForeignKey.CASCADE)],
)
data class Favourite(@PrimaryKey val shoeId: Long)

/** "Tell me when it is back": a restock alert the customer switched on for a sold-out shoe. */
@Entity(
    tableName = "stock_alerts",
    foreignKeys = [ForeignKey(entity = ShoeModel::class, parentColumns = ["id"], childColumns = ["shoeId"], onDelete = ForeignKey.CASCADE)],
)
data class StockAlert(@PrimaryKey val shoeId: Long)

/**
 * One line of the cart. It copies the shoe's name, size, colour and price, because the shoe may be hidden
 * or sold out while it waits in the cart. [available] is false when checkout would refuse the line.
 */
@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey val variantId: Long,
    val shoeId: Long,
    val name: String,
    val brand: String,
    val size: Int,
    val colorName: String,
    val colorHex: String,
    val artStyle: String,
    val imageUrl: String?,
    val unitPrice: Long,
    val qty: Int,
    val stock: Int,
    val available: Boolean,
) {
    val lineTotal: Long get() = unitPrice * qty
}

@Entity(
    tableName = "addresses",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class Address(
    @PrimaryKey val id: Long,
    val userId: Long,
    val label: String,
    val line: String,
    val city: String,
    val phone: String,
    val isDefault: Boolean,
)

/** A placed order. Its items keep the name and price from the day it was placed. */
@Entity(
    tableName = "orders",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class Order(
    @PrimaryKey val id: Long,
    val userId: Long,
    val status: String,            // new, processing, shipped, delivered or cancelled
    val subtotal: Long,
    val shippingFee: Long,
    val discount: Long,
    val total: Long,
    val couponCode: String?,
    val addressLine: String,
    val city: String,
    val phone: String,
    val paymentMethod: String,     // cash, card, payme or click
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "order_items",
    foreignKeys = [ForeignKey(entity = Order::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("orderId")],
)
data class OrderItem(
    @PrimaryKey val id: Long,
    val orderId: Long,
    val shoeId: Long?,
    val variantId: Long?,
    val name: String,
    val brand: String,
    val size: Int,
    val colorName: String,
    val colorHex: String,
    val artStyle: String,
    val unitPrice: Long,
    val qty: Int,
) {
    val lineTotal: Long get() = unitPrice * qty
}

/** An alert in the notification list: new drop, price drop, restock or order update. */
@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey val id: Long,
    val kind: String,              // drop, restock, price_drop or order
    val title: String,
    val body: String,
    val shoeId: Long?,
    val orderId: Long?,
    val isRead: Boolean,
    val createdAt: Long,
)

// ---- not tables: results of a JOIN, read as one object -------------------------------------------------------

/** A shoe together with all its sizes and colours. */
data class ShoeWithVariants(
    @Embedded val shoe: ShoeModel,
    @Relation(parentColumn = "id", entityColumn = "shoeId") val variants: List<ShoeVariant>,
) {
    val inStock: Boolean get() = variants.any { it.stock > 0 }
    val colors: List<Pair<String, String>> get() = variants.map { it.colorName to it.colorHex }.distinct()
    val sizesInStock: List<Int> get() = variants.filter { it.stock > 0 }.map { it.size }.distinct().sorted()
}

/** An order together with its items. */
data class OrderWithItems(
    @Embedded val order: Order,
    @Relation(parentColumn = "id", entityColumn = "orderId") val items: List<OrderItem>,
) {
    val itemCount: Int get() = items.sumOf { it.qty }
}
