package uz.shushop.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.shushop.app.Fixtures

class ParsersTest {
    private val sync = Parsers.snapshot(Fixtures.json("sync.json"))

    // ---- the big one: GET /api/mobile/sync

    @Test fun syncCarriesTheUser() {
        assertEquals(2L, sync.user.id)
        assertEquals("alex@demo.uz", sync.user.email)
        assertEquals("Alex Morgan", sync.user.fullName)
        assertEquals("customer", sync.user.role)
        assertEquals(42, sync.user.shoeSizeEu)
        assertEquals(1734680375668L, sync.user.memberSince)
    }

    @Test fun syncCarriesShoesAndTheirSizes() {
        assertEquals(listOf(3L, 1L, 5L, 6L), sync.shoes.map { it.id })
        assertEquals(24, sync.variants.size)
        val urban = sync.shoes.first { it.id == 3L }
        assertEquals("Urban Step", urban.name)
        assertEquals("Nomad", urban.brand)
        assertEquals(720_000L, urban.price)
        assertEquals(890_000L, urban.oldPrice)
        assertTrue(urban.onSale)
        assertEquals(19, urban.discountPercent)
        assertEquals(4.7, urban.avgRating, 0.0001)
        assertEquals(3, urban.reviewCount)
        assertNull(urban.imageUrl)                       // JSON null stays a Kotlin null, never the text "null"
        assertEquals(6, sync.variants.count { it.shoeId == 3L })
        assertTrue(sync.variants.all { it.colorHex.startsWith("#") })
    }

    @Test fun aShoeNotOnSaleHasNoOldPrice() {
        val airflex = sync.shoes.first { it.id == 1L }
        assertNull(airflex.oldPrice)
        assertFalse(airflex.onSale)
        assertEquals(0, airflex.discountPercent)
        assertEquals("unisex", airflex.category)
    }

    @Test fun favouritesAndAlertsArePulledOut() {
        assertEquals(setOf(3L, 1L), sync.favouriteIds.toSet())
        assertEquals(listOf(3L), sync.alertIds)
    }

    @Test fun cartLineKeepsItsOwnCopyOfTheShoe() {
        assertEquals(2, sync.cart.size)
        val line = sync.cart.first()
        assertEquals(41L, line.variantId)
        assertEquals("Swift Run", line.name)
        assertEquals(640_000L, line.unitPrice)
        assertEquals(8, line.stock)
        assertTrue(line.available)
        assertEquals(1_330_000L, sync.cart.sumOf { it.lineTotal })
        // shoe 2 is not in the shoe list at all, yet its cart line is complete
        assertFalse(sync.shoes.any { it.id == line.shoeId })
    }

    @Test fun addressesBelongToTheUser() {
        assertEquals(2, sync.addresses.size)
        assertTrue(sync.addresses.all { it.userId == sync.user.id })
        assertEquals("Home", sync.addresses.first { it.isDefault }.label)
    }

    @Test fun ordersComeWithTheirItems() {
        assertEquals(listOf(2L, 1L), sync.orders.map { it.id })
        assertEquals("shipped", sync.orders[0].status)
        assertEquals(640_000L, sync.orders[0].total)
        assertNull(sync.orders[0].couponCode)
        assertEquals(2, sync.orderItems.size)
        val item = sync.orderItems.first { it.orderId == 2L }
        assertEquals("Swift Run", item.name)
        assertEquals(42, item.size)
        assertEquals(44L, item.variantId)
        assertTrue(sync.orders.all { it.userId == sync.user.id })
    }

    @Test fun notificationsKeepWhatTheyPointTo() {
        assertEquals(4, sync.notifications.size)
        val restock = sync.notifications.first { it.kind == "restock" }
        assertEquals(3L, restock.shoeId)
        assertNull(restock.orderId)
        assertFalse(restock.isRead)
        val order = sync.notifications.first { it.kind == "order" }
        assertEquals(2L, order.orderId)
        assertNull(order.shoeId)
        assertEquals(2, sync.notifications.count { !it.isRead })
    }

    @Test fun metaComesFromTheServer() {
        assertEquals(25_000L, sync.meta.shippingFee)
        assertEquals(500_000L, sync.meta.freeShippingFrom)
        assertEquals(10, sync.meta.maxQtyPerItem)
        assertEquals(listOf("cash", "card", "payme", "click"), sync.meta.paymentMethods)
        assertEquals(27, sync.meta.sizeMin)
        assertEquals(46, sync.meta.sizeMax)
    }

    @Test fun syncTimeIsParsed() {
        assertEquals(1794297030781L, sync.syncedAt)
    }

    // ---- things that could go wrong with real data

    @Test fun hiddenShoesAreLeftOutAndTheirAlertsToo() {
        val root = Fixtures.json("sync.json")
        root.getJSONArray("products").getJSONObject(0).put("isActive", false)       // the owner hid shoe 3
        val s = Parsers.snapshot(root)
        assertFalse(s.shoes.any { it.id == 3L })
        assertFalse(s.variants.any { it.shoeId == 3L })
        assertFalse(s.favouriteIds.contains(3L))
        // the alert pointed to shoe 3; keeping it would break the database's foreign key
        assertTrue(s.alertIds.isEmpty())
    }

    @Test fun aSyncWithoutOptionalPartsStillParses() {
        val s = Parsers.snapshot(JSONObject("""{"user":{"id":9,"email":"a@b.uz","fullName":"A B"}}"""))
        assertTrue(s.shoes.isEmpty() && s.orders.isEmpty() && s.cart.isEmpty() && s.notifications.isEmpty())
        assertEquals(ShopMeta.DEFAULT, s.meta)
        assertNull(s.user.shoeSizeEu)
    }

    // ---- other answers

    @Test fun productDetailHasReviewsAndRatingBars() {
        val d = Parsers.productDetail(Fixtures.json("product_detail.json"))
        assertEquals("Urban Step", d.shoe.name)
        assertEquals(16, d.variants.size)
        assertEquals(3, d.reviews.size)
        assertEquals("Aziza K.", d.reviews[0].author)
        assertEquals(5, d.reviews[0].rating)
        assertFalse(d.reviews.any { it.mine })
        assertFalse(d.canReview)
        assertFalse(d.hasAlert)
        assertEquals(mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 1, 5 to 2), d.ratingBreakdown)
    }

    @Test fun orderDetailHasTheStatusHistory() {
        val d = Parsers.orderDetail(Fixtures.json("order_detail.json"), userId = 2)
        assertEquals(7L, d.order.id)
        assertEquals("new", d.order.status)
        assertEquals(listOf("new"), d.history.map { it.status })
        assertEquals(1794297030846L, d.history[0].at)
    }

    @Test fun checkoutAnswerBecomesAnOrderWithItems() {
        val d = Parsers.orderDetail(Fixtures.json("checkout.json"), userId = 2)
        assertEquals(7L, d.order.id)
        assertEquals("WELCOME10", d.order.couponCode)
        assertEquals(3_951_000L, d.order.total)
        assertEquals(4, d.items.size)
        assertEquals(6, d.items.sumOf { it.qty })
        assertTrue(d.items.all { it.orderId == 7L })
        assertEquals(d.order.subtotal, d.items.sumOf { it.lineTotal })
    }

    @Test fun cartAnswer() {
        val items = Parsers.cart(Fixtures.json("cart.json"))
        assertEquals(6, items.sumOf { it.qty })
        assertEquals(4_390_000L, items.sumOf { it.lineTotal })
    }

    @Test fun notificationsAnswer() {
        val array = Fixtures.json("notifications.json").getJSONArray("notifications")
        val list = (0 until array.length()).map { Parsers.notification(array.getJSONObject(it)) }
        assertEquals(5, list.size)
        assertEquals(3, list.count { !it.isRead })
    }

    @Test fun loginAnswer() {
        val r = Parsers.auth(Fixtures.json("login.json"))
        assertEquals("TEST-TOKEN-0123456789", r.token)
        assertEquals("alex@demo.uz", r.user.email)
    }

    @Test fun quoteAnswer() {
        val q = Parsers.quote(JSONObject("""{"subtotal":1000000,"discount":100000,"shipping":0,"total":900000,"coupon":{"code":"WELCOME10","percent":10}}"""))
        assertEquals(900_000L, q.total)
        assertEquals("WELCOME10", q.couponCode)
        assertEquals(10, q.couponPercent)
        val none = Parsers.quote(JSONObject("""{"subtotal":300000,"discount":0,"shipping":25000,"total":325000,"coupon":null}"""))
        assertNull(none.couponCode)
        assertEquals(0, none.couponPercent)
    }

    @Test fun errorMessageUsesTheServersSentence() {
        assertEquals("Wrong email or password", Parsers.errorMessage(Fixtures.text("error.json"), "fallback"))
        assertEquals("fallback", Parsers.errorMessage("<html>502 Bad Gateway</html>", "fallback"))
        assertEquals("fallback", Parsers.errorMessage("", "fallback"))
        assertEquals("fallback", Parsers.errorMessage(null, "fallback"))
        assertEquals("fallback", Parsers.errorMessage("""{"ok":true}""", "fallback"))
    }

    @Test fun shoeWithVariantsHelpers() {
        val urban = sync.shoes.first { it.id == 3L }
        val withVariants = ShoeWithVariants(urban, sync.variants.filter { it.shoeId == 3L })
        assertTrue(withVariants.inStock)
        assertEquals(listOf(40, 41, 43, 44), withVariants.sizesInStock)
        assertEquals(listOf("Black" to "#1c1c1e"), withVariants.colors)
    }
}
