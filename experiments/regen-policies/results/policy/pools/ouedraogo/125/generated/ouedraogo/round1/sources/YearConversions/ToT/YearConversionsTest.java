package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link YearConversions}.
 *
 * The tests cover:
 *   • Normal conversion paths for all public static methods.
 *   • Edge cases such as year {@code 0} (which is a valid ISO‑8601 year).
 *   • Error handling when the supplied {@code from} argument is {@code null}
 *     or not an instance of {@link Year}.
 *
 * The {@code Converter} argument is required by the API but is not used by
 * any of the conversion methods.  Therefore a minimal instance created with
 * a {@code null} {@code ConverterOptions} is sufficient.
 */
public class YearConversionsTest {

    private Converter dummyConverter;   // not used by the conversion methods

    @Before
    public void setUp() {
        // The Converter constructor accepts a ConverterOptions; passing null is allowed
        // because the conversions under test never consult the Converter instance.
        dummyConverter = new Converter(null);
    }

    /* -----------------------------------------------------------------
     * Helper – verifies that a non‑Year argument causes a ClassCastException.
     * ----------------------------------------------------------------- */
    private void assertClassCast(Runnable r) {
        try {
            r.run();
            fail("Expected ClassCastException");
        } catch (ClassCastException expected) {
            // expected – nothing to do
        }
    }

    /* -----------------------------------------------------------------
     * toInt() – the core conversion used by almost every other method.
     * ----------------------------------------------------------------- */
    @Test
    public void testToInt_normalYear() {
        Year y = Year.of(2023);
        int result = YearConversions.toInt(y, dummyConverter);
        assertEquals(2023, result);
    }

    @Test
    public void testToInt_yearZero() {
        Year y = Year.of(0);               // ISO‑8601 year zero is valid
        int result = YearConversions.toInt(y, dummyConverter);
        assertEquals(0, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_nullArgument() {
        YearConversions.toInt(null, dummyConverter);
    }

    @Test
    public void testToInt_wrongType() {
        assertClassCast(() -> YearConversions.toInt("2023", dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toLong()
     * ----------------------------------------------------------------- */
    @Test
    public void testToLong() {
        Year y = Year.of(1999);
        long value = YearConversions.toLong(y, dummyConverter);
        assertEquals(1999L, value);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_nullArgument() {
        YearConversions.toLong(null, dummyConverter);
    }

    @Test
    public void testToLong_wrongType() {
        assertClassCast(() -> YearConversions.toLong(1999, dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toShort()
     * ----------------------------------------------------------------- */
    @Test
    public void testToShort() {
        Year y = Year.of(123);
        short s = YearConversions.toShort(y, dummyConverter);
        assertEquals((short) 123, s);
    }

    @Test
    public void testToShort_overflow() {
        // Short overflow – the method simply casts, so overflow behaviour is expected.
        Year y = Year.of(40000);   // 40000 > Short.MAX_VALUE (32767)
        short s = YearConversions.toShort(y, dummyConverter);
        assertEquals((short) 40000, s);
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_nullArgument() {
        YearConversions.toShort(null, dummyConverter);
    }

    @Test
    public void testToShort_wrongType() {
        assertClassCast(() -> YearConversions.toShort(Boolean.TRUE, dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toAtomicInteger()
     * ----------------------------------------------------------------- */
    @Test
    public void testToAtomicInteger() {
        Year y = Year.of(2021);
        AtomicInteger ai = YearConversions.toAtomicInteger(y, dummyConverter);
        assertEquals(2021, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_nullArgument() {
        YearConversions.toAtomicInteger(null, dummyConverter);
    }

    @Test
    public void testToAtomicInteger_wrongType() {
        assertClassCast(() -> YearConversions.toAtomicInteger(42, dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toAtomicLong()
     * ----------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        Year y = Year.of(1975);
        AtomicLong al = YearConversions.toAtomicLong(y, dummyConverter);
        assertEquals(1975L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_nullArgument() {
        YearConversions.toAtomicLong(null, dummyConverter);
    }

    @Test
    public void testToAtomicLong_wrongType() {
        assertClassCast(() -> YearConversions.toAtomicLong(new Object(), dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toDouble()
     * ----------------------------------------------------------------- */
    @Test
    public void testToDouble() {
        Year y = Year.of(2005);
        double d = YearConversions.toDouble(y, dummyConverter);
        assertEquals(2005.0, d, 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_nullArgument() {
        YearConversions.toDouble(null, dummyConverter);
    }

    @Test
    public void testToDouble_wrongType() {
        assertClassCast(() -> YearConversions.toDouble(3.14, dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toFloat()
     * ----------------------------------------------------------------- */
    @Test
    public void testToFloat() {
        Year y = Year.of(1990);
        float f = YearConversions.toFloat(y, dummyConverter);
        assertEquals(1990.0f, f, 0.0f);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_nullArgument() {
        YearConversions.toFloat(null, dummyConverter);
    }

    @Test
    public void testToFloat_wrongType() {
        assertClassCast(() -> YearConversions.toFloat("1990", dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toAtomicBoolean()
     * ----------------------------------------------------------------- */
    @Test
    public void testToAtomicBoolean_true() {
        Year y = Year.of(2022);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, dummyConverter);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBoolean_falseWhenZero() {
        Year y = Year.of(0);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, dummyConverter);
        assertFalse(ab.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_nullArgument() {
        YearConversions.toAtomicBoolean(null, dummyConverter);
    }

    @Test
    public void testToAtomicBoolean_wrongType() {
        assertClassCast(() -> YearConversions.toAtomicBoolean(Boolean.FALSE, dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toBigInteger()
     * ----------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        Year y = Year.of(123456);
        BigInteger bi = YearConversions.toBigInteger(y, dummyConverter);
        assertEquals(BigInteger.valueOf(123456L), bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_nullArgument() {
        YearConversions.toBigInteger(null, dummyConverter);
    }

    @Test
    public void testToBigInteger_wrongType() {
        assertClassCast(() -> YearConversions.toBigInteger(new Object(), dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toBigDecimal()
     * ----------------------------------------------------------------- */
    @Test
    public void testToBigDecimal() {
        Year y = Year.of(98765);
        BigDecimal bd = YearConversions.toBigDecimal(y, dummyConverter);
        assertEquals(BigDecimal.valueOf(98765L), bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_nullArgument() {
        YearConversions.toBigDecimal(null, dummyConverter);
    }

    @Test
    public void testToBigDecimal_wrongType() {
        assertClassCast(() -> YearConversions.toBigDecimal(Double.NaN, dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toString()
     * ----------------------------------------------------------------- */
    @Test
    public void testToString() {
        Year y = Year.of(2024);
        String s = YearConversions.toString(y, dummyConverter);
        assertEquals("2024", s);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_nullArgument() {
        YearConversions.toString(null, dummyConverter);
    }

    @Test
    public void testToString_wrongType() {
        assertClassCast(() -> YearConversions.toString(Boolean.TRUE, dummyConverter));
    }

    /* -----------------------------------------------------------------
     * toMap()
     * ----------------------------------------------------------------- */
    @Test
    public void testToMap_normalYear() {
        Year y = Year.of(2018);
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) YearConversions.toMap(y, dummyConverter);

        // The constant used as the key is defined in MapConversions.YEAR.
        // Its documented value is "year".  We verify both the key and the value.
        assertEquals(1, map.size());
        assertTrue(map.containsKey("year"));
        assertEquals(Integer.valueOf(2018), map.get("year"));
    }

    @Test
    public void testToMap_yearZero() {
        Year y = Year.of(0);
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) YearConversions.toMap(y, dummyConverter);
        assertEquals(Integer.valueOf(0), map.get("year"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_nullArgument() {
        YearConversions.toMap(null, dummyConverter);
    }

    @Test
    public void testToMap_wrongType() {
        assertClassCast(() -> YearConversions.toMap(12345, dummyConverter));
    }
}
