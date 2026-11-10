package uz.shushop.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class IsoTimeTest {
    @Test fun parsesTheServersTimestamps() {
        assertEquals(1794297030846L, parseIsoMillis("2026-11-10T07:50:30.846Z"))
        assertEquals(1734680375668L, parseIsoMillis("2024-12-20T07:39:35.668Z"))
    }

    @Test fun epochAndLeapDay() {
        assertEquals(0L, parseIsoMillis("1970-01-01T00:00:00Z"))
        assertEquals(951868799999L, parseIsoMillis("2000-02-29T23:59:59.999Z"))
    }

    @Test fun respectsAnOffset() {
        assertEquals(1794294000000L, parseIsoMillis("2026-11-10T12:00:00+05:00"))
        assertEquals(1794294000000L, parseIsoMillis("2026-11-10T12:00:00+0500"))
    }

    @Test fun missingFractionIsZeroMillis() {
        assertEquals(1794297030000L, parseIsoMillis("2026-11-10T07:50:30Z"))
    }

    @Test fun badInputGivesZeroInsteadOfCrashing() {
        assertEquals(0L, parseIsoMillis(null))
        assertEquals(0L, parseIsoMillis(""))
        assertEquals(0L, parseIsoMillis("yesterday"))
    }
}
