package uz.shushop.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerUrlTest {
    @Test fun addsHttpsAndRemovesTrailingSlash() {
        assertEquals("https://shushop.onrender.com", normalizeServerUrl("shushop.onrender.com/"))
        assertEquals("https://shushop.onrender.com", normalizeServerUrl("  https://shushop.onrender.com  "))
    }

    @Test fun localAddressesUseHttp() {
        assertEquals("http://10.0.2.2:3000", normalizeServerUrl("10.0.2.2:3000"))
        assertEquals("http://192.168.1.20:3111", normalizeServerUrl("192.168.1.20:3111/"))
        assertEquals("http://localhost:3000", normalizeServerUrl("localhost:3000"))
    }

    @Test fun keepsAnExplicitScheme() {
        assertEquals("http://10.0.2.2:3000", normalizeServerUrl("http://10.0.2.2:3000"))
    }

    @Test fun rejectsWhatCannotBeAnAddress() {
        assertNull(normalizeServerUrl(""))
        assertNull(normalizeServerUrl("   "))
        assertNull(normalizeServerUrl("my shop"))
        assertNull(normalizeServerUrl("shop"))
        assertNull(normalizeServerUrl("ftp://example.com"))
    }
}
