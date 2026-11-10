package uz.shushop.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class, ShoeModel::class, ShoeVariant::class, Favourite::class, StockAlert::class, CartItem::class,
        Address::class, Order::class, OrderItem::class, NotificationItem::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoeDao(): ShoeDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun accountDao(): AccountDao
    abstract fun notificationDao(): NotificationDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Singleton: the whole app shares ONE database object
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "shushop.db")
                    // The database is only a copy of the server's data. If its layout changes in a new version
                    // of the app, throw it away and download a fresh copy.
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
