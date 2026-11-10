package uz.shushop.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitiesTest {
    private fun shoe(price: Long, old: Long?) =
        ShoeModel(1, "Urban Step", "Nomad", "Sneakers", "men", "", price, old, "high", null, 4.5, 2, 0)

    @Test fun discountPercentIsRounded() {
        assertEquals(19, shoe(720_000, 890_000).discountPercent)
        assertEquals(20, shoe(800_000, 1_000_000).discountPercent)
        assertTrue(shoe(800_000, 1_000_000).onSale)
    }

    @Test fun noOldPriceMeansNoSale() {
        assertFalse(shoe(800_000, null).onSale)
        assertEquals(0, shoe(800_000, null).discountPercent)
    }

    @Test fun anOldPriceThatIsNotHigherIsNotASale() {
        assertFalse(shoe(800_000, 800_000).onSale)
        assertFalse(shoe(900_000, 800_000).onSale)
    }

    @Test fun lineTotals() {
        val item = CartItem(41, 2, "Swift Run", "Velo", 39, "Cloud", "#e6e6e8", "runner", null, 640_000, 3, 8, true)
        assertEquals(1_920_000L, item.lineTotal)
        val ordered = OrderItem(1, 1, 2, 41, "Swift Run", "Velo", 39, "Cloud", "#e6e6e8", "runner", 640_000, 2)
        assertEquals(1_280_000L, ordered.lineTotal)
    }

    @Test fun orderWithItemsCountsPairs() {
        val order = Order(1, 2, "new", 0, 0, 0, 0, null, "", "", "", "cash", 0, 0)
        val items = listOf(
            OrderItem(1, 1, 2, 41, "A", "B", 39, "c", "#000000", "court", 1, 2),
            OrderItem(2, 1, 3, 58, "A", "B", 40, "c", "#000000", "court", 1, 3),
        )
        assertEquals(5, OrderWithItems(order, items).itemCount)
    }
}
