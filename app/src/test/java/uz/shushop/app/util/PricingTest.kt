package uz.shushop.app.util

import org.json.JSONObject
import uz.shushop.app.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class PricingTest {
    @Test fun smallOrderPaysDelivery() {
        val p = Pricing.price(subtotal = 300_000)
        assertEquals(PriceBreakdown(300_000, 0, 25_000, 325_000), p)
    }

    @Test fun deliveryIsFreeFrom500k() {
        assertEquals(25_000, Pricing.price(499_999).shipping)
        assertEquals(0, Pricing.price(500_000).shipping)
        assertEquals(0, Pricing.price(1_330_000).shipping)
    }

    @Test fun freeDeliveryIsCountedAfterTheDiscount() {
        // 550 000 - 10% = 495 000, which is under 500 000, so delivery is charged
        val p = Pricing.price(550_000, couponPercent = 10)
        assertEquals(55_000, p.discount)
        assertEquals(25_000, p.shipping)
        assertEquals(520_000, p.total)
    }

    @Test fun discountIsRoundedDownToAWholeSom() {
        assertEquals(15_000, Pricing.price(100_001, couponPercent = 15).discount)      // 15 000.15 -> 15 000
        assertEquals(0, Pricing.price(9, couponPercent = 10).discount)                  // 0.9 -> 0
    }

    @Test fun emptyCartCostsNothing() {
        assertEquals(PriceBreakdown(0, 0, 0, 0), Pricing.price(0))
    }

    @Test fun toFreeShipping() {
        assertEquals(200_000, Pricing.toFreeShipping(300_000, 0))
        assertEquals(0, Pricing.toFreeShipping(600_000, 0))
    }

    @Test fun matchesWhatTheServerChargedInARealCheckout() {
        val order = JSONObject(Fixtures.text("checkout.json")).getJSONObject("order")
        val p = Pricing.price(order.getLong("subtotal"), couponPercent = 10)       // the order used WELCOME10
        assertEquals(order.getLong("discount"), p.discount)
        assertEquals(order.getLong("shippingFee"), p.shipping)
        assertEquals(order.getLong("total"), p.total)
    }
}
