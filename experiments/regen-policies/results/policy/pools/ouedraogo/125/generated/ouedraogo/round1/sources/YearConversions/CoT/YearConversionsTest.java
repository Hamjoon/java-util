package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.junit.Assert.*;

public class YearConversionsTest {

    /* ----------------------------------------------------------------------
     * Helper – a valid Converter instance (the conversion methods do not use it)
     * ---------------------------------------------------------------------- */
    private static final Converter DUMMY_CONVERTER = null;   // safe because methods ignore the argument

    /* ----------------------------------------------------------------------
     * 1. toInt
     * ---------------------------------------------------------------------- */
    @Test
    public void testToIntBasic() {
        Year y = Year.of(2023);
        int result = YearConversions.toInt(y, DUMMY_CONVERTER);
        assertEquals(2023, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToIntNullInput() {
        YearConversions.toInt(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToIntWrongType() {
        YearConversions.toInt("2023", DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 2. toLong (delegates to toInt)
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLongBasic() {
        Year y = Year.of(1999);
        long result = YearConversions.toLong(y, DUMMY_CONVERTER);
        assertEquals(1999L, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNullInput() {
        YearConversions.toLong(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 3. toShort (delegates to toInt)
     * ---------------------------------------------------------------------- */
    @Test
    public void testToShortBasic() {
        Year y = Year.of(123);
        short result = YearConversions.toShort(y, DUMMY_CONVERTER);
        assertEquals((short)123, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToShortNullInput() {
        YearConversions.toShort(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 4. toAtomicInteger
     * ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicIntegerBasic() {
        Year y = Year.of(2020);
        AtomicInteger ai = YearConversions.toAtomicInteger(y, DUMMY_CONVERTER);
        assertNotNull(ai);
        assertEquals(2020, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNullInput() {
        YearConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 5. toAtomicLong
     * ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicLongBasic() {
        Year y = Year.of(1985);
        AtomicLong al = YearConversions.toAtomicLong(y, DUMMY_CONVERTER);
        assertNotNull(al);
        assertEquals(1985L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullInput() {
        YearConversions.toAtomicLong(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 6. toDouble
     * ---------------------------------------------------------------------- */
    @Test
    public void testToDoubleBasic() {
        Year y = Year.of(2001);
        double d = YearConversions.toDouble(y, DUMMY_CONVERTER);
        assertEquals(2001.0, d, 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNullInput() {
        YearConversions.toDouble(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 7. toFloat
     * ---------------------------------------------------------------------- */
    @Test
    public void testToFloatBasic() {
        Year y = Year.of(1995);
        float f = YearConversions.toFloat(y, DUMMY_CONVERTER);
        assertEquals(1995.0f, f, 0.0f);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloatNullInput() {
        YearConversions.toFloat(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 8. toAtomicBoolean
     * ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicBooleanTrueWhenNonZero() {
        Year y = Year.of(2022);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, DUMMY_CONVERTER);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalseWhenZero() {
        Year y = Year.of(0);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, DUMMY_CONVERTER);
        assertFalse(ab.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBooleanNullInput() {
        YearConversions.toAtomicBoolean(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 9. toBigInteger
     * ---------------------------------------------------------------------- */
    @Test
    public void testToBigIntegerBasic() {
        Year y = Year.of(2048);
        BigInteger bi = YearConversions.toBigInteger(y, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(2048), bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNullInput() {
        YearConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 10. toBigDecimal
     * ---------------------------------------------------------------------- */
    @Test
    public void testToBigDecimalBasic() {
        Year y = Year.of(1970);
        BigDecimal bd = YearConversions.toBigDecimal(y, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(1970), bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNullInput() {
        YearConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 11. toString
     * ---------------------------------------------------------------------- */
    @Test
    public void testToStringBasic() {
        Year y = Year.of(2024);
        String s = YearConversions.toString(y, DUMMY_CONVERTER);
        assertEquals("2024", s);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullInput() {
        YearConversions.toString(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 12. toMap
     * ---------------------------------------------------------------------- */
    @Test
    public void testToMapBasic() {
        Year y = Year.of(2015);
        Map<?, ?> map = YearConversions.toMap(y, DUMMY_CONVERTER);
        assertNotNull(map);
        assertTrue(map.containsKey(YEAR));
        assertEquals(Integer.valueOf(2015), map.get(YEAR));
        // Verify the map implementation is a CompactMap (optional, based on class name)
        assertTrue(map instanceof CompactMap);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullInput() {
        YearConversions.toMap(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * Edge‑case tests for extreme Year values
     * ---------------------------------------------------------------------- */
    @Test
    public void testExtremeYearValues() {
        Year min = Year.of(Year.MIN_VALUE); // Year.MIN_VALUE == -999_999_999
        Year max = Year.of(Year.MAX_VALUE); // Year.MAX_VALUE == 999_999_999

        // int conversion
        assertEquals(Year.MIN_VALUE, YearConversions.toInt(min, DUMMY_CONVERTER));
        assertEquals(Year.MAX_VALUE, YearConversions.toInt(max, DUMMY_CONVERTER));

        // long conversion
        assertEquals((long) Year.MIN_VALUE, YearConversions.toLong(min, DUMMY_CONVERTER));
        assertEquals((long) Year.MAX_VALUE, YearConversions.toLong(max, DUMMY_CONVERTER));

        // BigInteger conversion
        assertEquals(BigInteger.valueOf(Year.MIN_VALUE), YearConversions.toBigInteger(min, DUMMY_CONVERTER));
        assertEquals(BigInteger.valueOf(Year.MAX_VALUE), YearConversions.toBigInteger(max, DUMMY_CONVERTER));

        // BigDecimal conversion
        assertEquals(BigDecimal.valueOf(Year.MIN_VALUE), YearConversions.toBigDecimal(min, DUMMY_CONVERTER));
        assertEquals(BigDecimal.valueOf(Year.MAX_VALUE), YearConversions.toBigDecimal(max, DUMMY_CONVERTER));
    }
}
