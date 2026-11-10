package uz.shushop.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun email() {
        assertTrue(Validators.isValidEmail("alex@demo.uz"))
        assertTrue(Validators.isValidEmail("  alex.m+shop@mail.example.com "))
        assertFalse(Validators.isValidEmail(""))
        assertFalse(Validators.isValidEmail("alex"))
        assertFalse(Validators.isValidEmail("alex@demo"))
        assertFalse(Validators.isValidEmail("alex@demo.u"))
        assertFalse(Validators.isValidEmail("al ex@demo.uz"))
    }

    @Test fun password_needs8to72Characters() {
        assertFalse(Validators.isValidPassword("short"))
        assertFalse(Validators.isValidPassword("1234567"))
        assertTrue(Validators.isValidPassword("12345678"))
        assertTrue(Validators.isValidPassword("a".repeat(72)))
        assertFalse(Validators.isValidPassword("a".repeat(73)))
    }

    @Test fun name() {
        assertFalse(Validators.isValidName("A"))
        assertTrue(Validators.isValidName("Al"))
        assertTrue(Validators.isValidName("  Alex Morgan "))
        assertFalse(Validators.isValidName("x".repeat(61)))
    }

    @Test fun phone() {
        assertTrue(Validators.isValidPhone("+998 90 123 45 67"))
        assertTrue(Validators.isValidPhone("901234567"))
        assertFalse(Validators.isValidPhone("12345"))
        assertFalse(Validators.isValidPhone("call me maybe"))
    }

    @Test fun addressLine() {
        assertFalse(Validators.isValidAddressLine("abc"))
        assertTrue(Validators.isValidAddressLine("Chilonzor 9, apt 12"))
    }
}
