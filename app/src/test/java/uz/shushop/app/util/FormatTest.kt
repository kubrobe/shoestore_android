package uz.shushop.app.util

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun formatSom_groupsThousandsWithSpaces() {
        assertEquals("0 so'm", formatSom(0))
        assertEquals("950 so'm", formatSom(950))
        assertEquals("1 000 so'm", formatSom(1_000))
        assertEquals("650 000 so'm", formatSom(650_000))
        assertEquals("3 951 000 so'm", formatSom(3_951_000))
        assertEquals("12 345 678 so'm", formatSom(12_345_678))
    }

    @Test fun formatSom_handlesNegativeAmounts() {
        assertEquals("-439 000 so'm", formatSom(-439_000))
    }

    @Test fun pairs_isSingularForOne() {
        assertEquals("1 pair", pairs(1))
        assertEquals("2 pairs", pairs(2))
        assertEquals("0 pairs", pairs(0))
    }

    @Test fun initials_takesTwoLetters() {
        assertEquals("AM", initials("Alex Morgan"))
        assertEquals("A", initials("alex"))
        assertEquals("AM", initials("  alex   morgan  jr "))
        assertEquals("?", initials("   "))
    }

    @Test fun formatDate_usesTheGivenZone() {
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals("10 Nov 2026", formatDate(1794297030846, utc))
        assertEquals("10 Nov, 07:50", formatDateTime(1794297030846, utc))
        assertEquals("10 Nov, 12:50", formatDateTime(1794297030846, TimeZone.getTimeZone("Asia/Tashkent")))
    }

    @Test fun timeAgo_picksTheRightUnit() {
        val now = 1_794_297_030_846L
        assertEquals("just now", timeAgo(now - 20_000, now))
        assertEquals("5 min ago", timeAgo(now - 5 * 60_000, now))
        assertEquals("3 h ago", timeAgo(now - 3 * 3_600_000, now))
        assertEquals("1 day ago", timeAgo(now - 26 * 3_600_000, now))
        assertEquals("4 days ago", timeAgo(now - 4 * 86_400_000L, now))
    }
}
