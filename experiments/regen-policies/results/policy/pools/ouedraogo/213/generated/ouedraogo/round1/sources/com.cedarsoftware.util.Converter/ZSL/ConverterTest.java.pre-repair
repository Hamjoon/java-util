package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

public class ConverterTest {

    /** Simple POJO used for custom conversion tests */
    public static final class MyCustom {
        private final String value;
        public MyCustom(String value) { this.value = value; }
        public String getValue() { return value; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MyCustom)) return false;
            MyCustom that = (MyCustom) o;
            return Objects.equals(value, that.value);
        }
        @Override public int hashCode() { return Objects.hash(value); }
        @Override public String toString() { return "MyCustom[" + value + "]"; }
    }

    /** Clean up any custom conversion added during the test suite */
    @AfterClass
    public static void cleanup() {
        // The library does not expose a removal API.  To avoid side‑effects we
        // re‑initialize the static Converter instance by adding a no‑op conversion
        // for the custom type (overwrites the test conversion).  This ensures later
        // tests (if any) see a deterministic behaviour.
        Converter.addConversion(String.class, MyCustom.class,
                (from, conv) -> null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic conversion sanity checks (String -> primitive / wrapper types) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testStringToIntegerConversion() {
        Integer i = Converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), i);
    }

    @Test
    public void testStringToIntPrimitiveConversion() {
        int i = Converter.convert("456", int.class);
        assertEquals(456, i);
    }

    @Test
    public void testStringToLongPrimitiveConversion() {
        long l = Converter.convert("789", long.class);
        assertEquals(789L, l);
    }

    @Test
    public void testStringToBooleanConversion() {
        Boolean b = Converter.convert("true", Boolean.class);
        assertTrue(b);
        Boolean b2 = Converter.convert("false", Boolean.class);
        assertFalse(b2);
    }

    @Test
    public void testStringToDoublePrimitiveConversion() {
        double d = Converter.convert("3.1415", double.class);
        assertEquals(3.1415, d, 0.000001);
    }

    @Test
    public void testStringToCharConversion() {
        char c = Converter.convert("Z", char.class);
        assertEquals('Z', c);
    }

    @Test
    public void testStringToBigDecimalConversion() {
        BigDecimal bd = Converter.convert("12.34", BigDecimal.class);
        assertEquals(new BigDecimal("12.34"), bd);
    }

    @Test
    public void testStringToBigIntegerConversion() {
        BigInteger bi = Converter.convert("9876543210", BigInteger.class);
        assertEquals(new BigInteger("9876543210"), bi);
    }

    /* ---------------------------------------------------------------------- */
    /*  Null handling for primitive‑type helpers (convert2X) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConvert2IntNull() {
        assertEquals(0, Converter.convert2int(null));
    }

    @Test
    public void testConvert2LongNull() {
        assertEquals(CommonValues.LONG_ZERO.longValue(), Converter.convert2long(null));
    }

    @Test
    public void testConvert2FloatNull() {
        assertEquals(CommonValues.FLOAT_ZERO.floatValue(), Converter.convert2float(null), 0.0f);
    }

    @Test
    public void testConvert2DoubleNull() {
        assertEquals(CommonValues.DOUBLE_ZERO.doubleValue(), Converter.convert2double(null), 0.0);
    }

    @Test
    public void testConvert2BooleanNull() {
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testConvert2CharNull() {
        assertEquals('\u0000', Converter.convert2char(null));
    }

    @Test
    public void testConvert2ByteNull() {
        assertEquals((byte)0, Converter.convert2byte(null));
    }

    @Test
    public void testConvert2ShortNull() {
        assertEquals((short)0, Converter.convert2short(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  Null handling for object‑type helpers (convertToX) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConvertToStringNull() {
        assertNull(Converter.convertToString(null));
    }

    @Test
    public void testConvertToIntegerNull() {
        assertNull(Converter.convertToInteger(null));
    }

    @Test
    public void testConvertToBigDecimalNull() {
        assertNull(Converter.convertToBigDecimal(null));
    }

    @Test
    public void testConvertToBigIntegerNull() {
        assertNull(Converter.convertToBigInteger(null));
    }

    @Test
    public void testConvertToDateNull() {
        assertNull(Converter.convertToDate(null));
    }

    @Test
    public void testConvertToLocalDateNull() {
        assertNull(Converter.convertToLocalDate(null));
    }

    @Test
    public void testConvertToLocalDateTimeNull() {
        assertNull(Converter.convertToLocalDateTime(null));
    }

    @Test
    public void testConvertToZonedDateTimeNull() {
        assertNull(Converter.convertToZonedDateTime(null));
    }

    @Test
    public void testConvertToCalendarNull() {
        assertNull(Converter.convertToCalendar(null));
    }

    @Test
    public void testConvertToAtomicIntegerNull() {
        AtomicInteger ai = Converter.convertToAtomicInteger(null);
        assertNull(ai);
    }

    @Test
    public void testConvert2AtomicIntegerNull() {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        assertNotNull(ai);
        assertEquals(0, ai.get());
    }

    @Test
    public void testConvertToAtomicLongNull() {
        AtomicLong al = Converter.convertToAtomicLong(null);
        assertNull(al);
    }

    @Test
    public void testConvert2AtomicLongNull() {
        AtomicLong al = Converter.convert2AtomicLong(null);
        assertNotNull(al);
        assertEquals(0L, al.get());
    }

    @Test
    public void testConvertToAtomicBooleanNull() {
        AtomicBoolean ab = Converter.convertToAtomicBoolean(null);
        assertNull(ab);
    }

    @Test
    public void testConvert2AtomicBooleanNull() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper methods that perform default‑value conversion (convert2X) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConvert2StringNullReturnsEmpty() {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvert2BigDecimalNullReturnsZero() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
    }

    @Test
    public void testConvert2BigIntegerNullReturnsZero() {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  Date / time conversions – simple round‑trip checks */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDateToInstantRoundTrip() {
        Date now = new Date();
        Instant instant = Converter.convert(now, Instant.class);
        assertEquals(now.toInstant(), instant);
    }

    @Test
    public void testLocalDateToStringAndBack() {
        LocalDate ld = LocalDate.of(2023, Month.APRIL, 15);
        String s = Converter.convert(ld, String.class);
        LocalDate parsed = Converter.convert(s, LocalDate.class);
        assertEquals(ld, parsed);
    }

    @Test
    public void testLocalDateTimeToStringAndBack() {
        LocalDateTime ldt = LocalDateTime.of(2023, 12, 31, 23, 59, 59);
        String s = Converter.convert(ldt, String.class);
        LocalDateTime parsed = Converter.convert(s, LocalDateTime.class);
        assertEquals(ldt, parsed);
    }

    @Test
    public void testZonedDateTimeToStringAndBack() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 2, 28, 12, 30, 0, 0, ZoneId.of("UTC"));
        String s = Converter.convert(zdt, String.class);
        ZonedDateTime parsed = Converter.convert(s, ZonedDateTime.class);
        assertEquals(zdt, parsed);
    }

    @Test
    public void testTimestampConversion() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Long epochMillis = Converter.convert(ts, Long.class);
        assertEquals(ts.getTime(), epochMillis.longValue());
        Timestamp back = Converter.convert(epochMillis, Timestamp.class);
        assertEquals(ts.getTime(), back.getTime());
    }

    /* ---------------------------------------------------------------------- */
    /*  Support‑query methods */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsConversionSupportedForKnown() {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
        assertTrue(Converter.isConversionSupportedFor(Integer.class, String.class));
    }

    @Test
    public void testIsConversionSupportedForUnknown() {
        // No built‑in conversion from String to Thread
        assertFalse(Converter.isConversionSupportedFor(String.class, Thread.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupportedKnown() {
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Double.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupportedArray() {
        // Arrays are not considered “simple” conversions
        assertFalse(Converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
    }

    @Test
    public void testAllSupportedConversionsNotEmpty() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
        // Spot‑check: there should be at least one entry for String
        assertTrue(map.containsKey(String.class));
        assertTrue(map.get(String.class).contains(Integer.class));
    }

    @Test
    public void testGetSupportedConversionsByNameNotEmpty() {
        Map<String, Set<String>> nameMap = Converter.getSupportedConversions();
        assertNotNull(nameMap);
        assertFalse(nameMap.isEmpty());
        assertTrue(nameMap.containsKey("java.lang.String"));
        assertTrue(nameMap.get("java.lang.String").contains("java.lang.Integer"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Custom conversion registration */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddCustomConversionAndUseIt() {
        // Register a conversion from String to MyCustom
        Convert<MyCustom> conversion = (from, conv) -> new MyCustom((String) from);
        Convert<?> previous = Converter.addConversion(String.class, MyCustom.class, conversion);
        assertNull("No previous conversion should exist for this pair", previous);

        // Verify conversion works
        MyCustom result = Converter.convert("hello", MyCustom.class);
        assertNotNull(result);
        assertEquals("hello", result.getValue());

        // Verify that the conversion is now reported as supported
        assertTrue(Converter.isConversionSupportedFor(String.class, MyCustom.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, MyCustom.class));
    }

    @Test
    public void testAddConversionOverridesExisting() {
        // Existing conversion: String -> Integer (built‑in)
        Convert<Integer> original = (from, conv) -> Integer.valueOf((String) from);
        Convert<?> previous = Converter.addConversion(String.class, Integer.class, original);
        assertNotNull("There should be a previous built‑in conversion", previous);

        // Use the overridden conversion (same logic, just to prove it runs)
        Integer i = Converter.convert("99", Integer.class);
        assertEquals(Integer.valueOf(99), i);
    }

    /* ---------------------------------------------------------------------- */
    /*  Deprecated helper methods – they delegate to the main convert() */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeprecatedLocalDateToMillis() {
        LocalDate ld = LocalDate.of(1970, 1, 2); // one day after epoch
        long millis = Converter.localDateToMillis(ld);
        assertEquals(Duration.ofDays(1).toMillis(), millis);
    }

    @Test
    public void testDeprecatedLocalDateTimeToMillis() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 1, 0, 0);
        long expected = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long actual = Converter.localDateTimeToMillis(ldt);
        assertEquals(expected, actual);
    }

    @Test
    public void testDeprecatedZonedDateTimeToMillis() {
        ZonedDateTime zdt = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        long actual = Converter.zonedDateTimeToMillis(zdt);
        assertEquals(0L, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  Additional sanity checks for primitive wrappers via convert() */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConvertNullToPrimitiveWrapperReturnsNull() {
        assertNull(Converter.convert(null, Integer.class));
        assertNull(Converter.convert(null, Boolean.class));
        assertNull(Converter.convert(null, Character.class));
    }

    @Test
    public void testConvertNullToPrimitiveReturnsDefault() {
        assertEquals(0, (int) Converter.convert(null, int.class));
        assertEquals(0L, (long) Converter.convert(null, long.class));
        assertEquals(0.0f, (float) Converter.convert(null, float.class), 0.0f);
        assertEquals(0.0, (double) Converter.convert(null, double.class), 0.0);
        assertFalse((boolean) Converter.convert(null, boolean.class));
        assertEquals('\u0000', (char) Converter.convert(null, char.class));
    }
}
