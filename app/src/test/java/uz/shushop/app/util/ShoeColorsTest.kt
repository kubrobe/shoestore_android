package uz.shushop.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The expected values were produced by the website's own shoe-art.js, so the phone draws the same colours. */
class ShoeColorsTest {
    private fun shade(hex: String, amount: Double) = ShoeColors.toHex(ShoeColors.shade(ShoeColors.parse(hex), amount))

    @Test fun shadeMatchesTheWebsite() {
        assertEquals("#d7ccbb", shade("#cdbfaa", 0.2))
        assertEquals("#a49988", shade("#cdbfaa", -0.2))
        assertEquals("#8e8e8f", shade("#1c1c1e", 0.5))
        assertEquals("#111112", shade("#1c1c1e", -0.4))
        assertEquals("#f9b59e", shade("#f26a3d", 0.5))
        assertEquals("#8a8a8b", shade("#e6e6e8", -0.4))
        assertEquals("#5480bd", shade("#3d6fb4", 0.12))
        assertEquals("#d6d6d6", shade("#ffffff", -0.16))
        assertEquals("#383838", shade("#000000", 0.22))
    }

    @Test fun isDarkMatchesTheWebsite() {
        assertFalse(ShoeColors.isDark(ShoeColors.parse("#cdbfaa")))
        assertTrue(ShoeColors.isDark(ShoeColors.parse("#1c1c1e")))
        assertFalse(ShoeColors.isDark(ShoeColors.parse("#e6e6e8")))
        assertTrue(ShoeColors.isDark(ShoeColors.parse("#232326")))
        assertFalse(ShoeColors.isDark(ShoeColors.parse("#6e6e6e")))
        assertFalse(ShoeColors.isDark(ShoeColors.parse("#f26a3d")))
    }

    @Test fun accentIsPickedByShoeId() {
        val expected = mapOf(0L to "#f26a3d", 1L to "#3d6fb4", 2L to "#e0b44c", 3L to "#4f7a5a", 4L to "#d6456b", 5L to "#f26a3d", 17L to "#e0b44c", -3L to "#4f7a5a")
        for ((id, hex) in expected) assertEquals("shoe $id", hex, ShoeColors.toHex(ShoeColors.accentFor(id)))
    }

    @Test fun parseFallsBackOnBadText() {
        assertEquals(ShoeColors.FALLBACK, ShoeColors.parse(null))
        assertEquals(ShoeColors.FALLBACK, ShoeColors.parse("red"))
        assertEquals(ShoeColors.FALLBACK, ShoeColors.parse("#12345"))
        assertEquals(0x1C1C1E, ShoeColors.parse("#1C1C1E"))
    }

    @Test fun accentThatMatchesTheShoeIsMadeVisible() {
        val base = ShoeColors.parse("#f26a3d")
        val same = ShoeColors.visibleAccent(base, base)
        assertTrue(same != base)
        val far = ShoeColors.parse("#3d6fb4")
        assertEquals(far, ShoeColors.visibleAccent(far, ShoeColors.parse("#f6f3ec")))
    }

    @Test fun everyStyleDrawsAtLeastAnUpperAndASole() {
        for (style in listOf("court", "runner", "high", "chunky", "slip", "boot")) {
            val spec = ShoeGeometry.spec(style, "#cdbfaa", "#f26a3d")
            assertTrue("$style upper", spec.upper.isNotEmpty())
            assertTrue("$style sole", spec.sole.isNotEmpty())
        }
    }

    @Test fun unknownStyleDrawsTheCourtShoe() {
        val unknown = ShoeGeometry.spec("spaceship", "#cdbfaa", null)
        assertEquals("court", unknown.style)
    }

    @Test fun orderStatusRules() {
        assertTrue(OrderStatus.canCancel("new"))
        assertTrue(OrderStatus.canCancel("processing"))
        assertFalse(OrderStatus.canCancel("shipped"))
        assertFalse(OrderStatus.canCancel("delivered"))
        assertEquals(2, OrderStatus.step("shipped"))
        assertEquals(-1, OrderStatus.step("cancelled"))
        assertEquals("On the way", OrderStatus.label("shipped"))
        assertTrue(OrderStatus.isActive("processing"))
        assertFalse(OrderStatus.isActive("cancelled"))
    }
}
