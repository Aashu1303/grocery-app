package app.rasoi.core.domain.ledger

import app.rasoi.core.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpiLinkTest {
    @Test
    fun buildsAPayLinkWithAmountInRupeesAndAnEncodedNote() {
        val link = UpiLink.build("meera@okbank", "Meera Iyer", Money(123_450), "Rasoi Oct 2026")
        assertEquals("upi://pay?pa=meera@okbank&pn=Meera%20Iyer&am=1234.50&cu=INR&tn=Rasoi%20Oct%202026", link)
    }

    @Test
    fun wholeRupeesKeepTwoDecimals() {
        assertEquals(
            "upi://pay?pa=a@b&pn=A&am=50.00&cu=INR&tn=x",
            UpiLink.build("a@b", "A", Money(5_000), "x"),
        )
    }

    @Test
    fun reservedCharactersAreEscapedSoTheyCannotInjectParameters() {
        val link = UpiLink.build("a@b", "Ravi & Sons=1", Money(100), "50% off#1?")
        assertEquals("upi://pay?pa=a@b&pn=Ravi%20%26%20Sons%3D1&am=1.00&cu=INR&tn=50%25%20off%231%3F", link)
    }

    @Test
    fun nonAsciiNamesAreUtf8PercentEncoded() {
        assertEquals(
            "upi://pay?pa=a@b&pn=%E0%A4%B0%E0%A4%B5%E0%A4%BF&am=1.00&cu=INR&tn=x",
            UpiLink.build("a@b", "रवि", Money(100), "x"),
        )
    }

    @Test
    fun noLinkWithoutAValidAddressOrAPositiveAmount() {
        assertNull(UpiLink.build("not-a-vpa", "A", Money(100), "x"))
        assertNull(UpiLink.build("a@b", "A", Money.ZERO, "x"))
        assertNull(UpiLink.build("a@b", "A", Money(-100), "x"))
        assertNull(UpiLink.build("a b@c", "A", Money(100), "x"))
    }

    @Test
    fun recognisesWellFormedAddresses() {
        listOf("meera@okhdfcbank", "98123.45678@ybl", "a-b_c@upi").forEach {
            assertEquals(true, UpiLink.isValidVpa(it), it)
        }
        listOf("", "@bank", "name@", "na me@bank", "name@bank@x", "name@ba nk").forEach {
            assertEquals(false, UpiLink.isValidVpa(it), it)
        }
    }
}
