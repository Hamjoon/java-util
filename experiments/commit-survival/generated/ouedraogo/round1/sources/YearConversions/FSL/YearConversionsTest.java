package com.cedarsoftware.util.convert;

import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.Map;

import org.junit.Test;

public class YearConversionsTest {

    private static final Converter DUMMY_CONVERTER = new Converter(null);

    /* --------------------------------------------------------------------- */
    /*  toInt and primitive numeric conversions                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToIntReturnsYearValue() {
        Year y = Year.of(2023);
        assertEquals(2023, YearConversions.toInt(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToLongDelegatesToToInt() {
        Year y = Year.of(1999);
        assertEquals(1999L, YearConversions.toLong(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToShortCastsCorrectly() {
        Year y = Year.of(32767);               // max value for short
        assertEquals((short) 32767, YearConversions.toShort(y, DUMMY_CONVERTER));
        // a value that overflows short should wrap according to Java casting rules
        Year overflow = Year.of(40000);
        assertEquals((short) 40000, YearConversions.toShort(overflow, DUMMY_CONVERTER));
    }

    @Test
    public void testToDoubleAndFloatReturnSameNumericValue() {
        Year y = Year.of(2020);
        assertEquals(2020.0, YearConversions.toDouble(y, DUMMY_CONVERTER), 0.0);
        assertEquals(2020.0f, YearConversions.toFloat(y, DUMMY_CONVERTER), 0.0f);
    }

    /* --------------------------------------------------------------------- */
    /*  Atomic wrappers                                                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToAtomicInteger() {
        Year y = Year.of(2015);
        assertEquals(2015, YearConversions.toAtomicInteger(y, DUMMY_CONVERTER).get());
    }

    @Test
    public void testToAtomicLong() {
        Year y = Year.of(2015);
        assertEquals(2015L, YearConversions.toAtomicLong(y, DUMMY_CONVERTER).get());
    }

    @Test
    public void testToAtomicBoolean() {
        Year nonZero = Year.of(1);
        Year zero = Year.of(0);
        assertTrue(YearConversions.toAtomicBoolean(nonZero, DUMMY_CONVERTER).get());
        assertFalse(YearConversions.toAtomicBoolean(zero, DUMMY_CONVERTER).get());
    }

    /* --------------------------------------------------------------------- */
    /*  BigInteger / BigDecimal                                               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        Year y = Year.of(1234);
        BigInteger expected = BigInteger.valueOf(1234);
        assertEquals(expected, YearConversions.toBigInteger(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToBigDecimal() {
        Year y = Year.of(5678);
        BigDecimal expected = BigDecimal.valueOf(5678);
        assertEquals(expected, YearConversions.toBigDecimal(y, DUMMY_CONVERTER));
    }

    /* --------------------------------------------------------------------- */
    /*  String conversion                                                     */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToString() {
        Year y = Year.of(2022);
        assertEquals("2022", YearConversions.toString(y, DUMMY_CONVERTER));
    }

    /* --------------------------------------------------------------------- */
    /*  Map conversion                                                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToMapContainsYearKeyAndValue() {
        Year y = Year.of(1995);
        Map<?, ?> map = YearConversions.toMap(y, DUMMY_CONVERTER);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(YEAR));
        assertEquals(Integer.valueOf(1995), map.get(YEAR));
    }

    /* --------------------------------------------------------------------- */
    /*  Null and type‑mismatch handling                                        */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testToIntWithNullThrowsNPE() {
        YearConversions.toInt(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToIntWithWrongTypeThrowsCCE() {
        YearConversions.toInt("not a Year", DUMMY_CONVERTER);
    }
}
