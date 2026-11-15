package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import com.cedarsoftware.util.convert.Converter as InternalConverter; // alias to avoid name clash
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.sql.Date as SqlDate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive JUnit‑4 test suite for {@link com.cedarsoftware.util.Converter}.
 *
 * The suite covers:
 * <ul>
 *   <li>Normal conversions (String → Number, Number → String, etc.)</li>
 *   <li>Null handling for primitive and object targets</li>
 *   <li>Array / collection conversion support checks</li>
 *   <li>Custom conversion registration via {@code addConversion}</li>
 *   <li>Deprecated helper methods</li>
 *   <li>Error handling (null target class)</li>
 * </ul>
 *
 * All tests use only the public API presented in the problem statement.
 */
public class ConverterTest
{
    /** A simple POJO used for custom‑conversion tests. */
    public static final class MyCustom
    {
        private final String value;

        public MyCustom(String value) { this.value = value; }

        public String getValue() { return value; }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof MyCustom)) return false;
            MyCustom other = (MyCustom) o;
            return value.equals(other.value);
        }

        @Override
        public int hashCode() { return value.hashCode(); }
    }

    /** Shared reference to the internal converter instance – used for direct calls in some tests. */
    private static InternalConverter internal;

    @BeforeClass
    public static void setUp()
    {
        // The static wrapper class creates its own internal converter instance.
        // We also keep a direct reference to the internal one for a few low‑level checks.
        internal = new InternalConverter(new DefaultConverterOptions());
    }

    @AfterClass
    public static void tearDown()
    {
        internal = null;
    }

    /* --------------------------------------------------------------------- */
    /*                     BASIC CONVERSION TESTS                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testStringToInteger()
    {
        Integer i = Converter.convert("123", Integer.class);
        Assert.assertEquals(Integer.valueOf(123), i);
    }

    @Test
    public void testIntegerToString()
    {
        String s = Converter.convert(456, String.class);
        Assert.assertEquals("456", s);
    }

    @Test
    public void testStringToLocalDate()
    {
        LocalDate expected = LocalDate.of(2024, 4, 27);
        LocalDate actual   = Converter.convertToLocalDate("2024-04-27");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testLocalDateTimeToTimestamp()
    {
        LocalDateTime ldt = LocalDateTime.of(2022, 5, 10, 12, 30, 15, 123_000_000);
        Timestamp ts = Converter.convertToTimestamp(ldt);
        // The expected epoch millis for the above local date‑time in the system default zone
        long expectedMillis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Assert.assertEquals(expectedMillis, ts.getTime());
    }

    @Test
    public void testStringToBigDecimal()
    {
        BigDecimal bd = Converter.convertToBigDecimal("12345.6789");
        Assert.assertEquals(new BigDecimal("12345.6789"), bd);
    }

    @Test
    public void testNullToPrimitiveInt()
    {
        int i = Converter.convert2int(null);
        Assert.assertEquals(0, i);
    }

    @Test
    public void testNullToWrapperInteger()
    {
        Integer i = Converter.convertToInteger(null);
        Assert.assertNull(i);
    }

    @Test
    public void testNullToChar()
    {
        char c = Converter.convert2char(null);
        Assert.assertEquals('\u0000', c);
    }

    @Test
    public void testNullToBoolean()
    {
        boolean b = Converter.convert2boolean(null);
        Assert.assertFalse(b);
    }

    @Test
    public void testNullToAtomicInteger()
    {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        Assert.assertEquals(0, ai.get());
    }

    @Test
    public void testNullToAtomicBoolean()
    {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        Assert.assertFalse(ab.get());
    }

    @Test
    public void testConvert2StringWithNull()
    {
        String s = Converter.convert2String(null);
        Assert.assertEquals("", s);
    }

    @Test
    public void testConvertToStringWithNull()
    {
        String s = Converter.convertToString(null);
        Assert.assertNull(s);
    }

    @Test
    public void testConvert2BigDecimalWithNull()
    {
        BigDecimal bd = Converter.convert2BigDecimal(null);
        Assert.assertEquals(BigDecimal.ZERO, bd);
    }

    @Test
    public void testConvertToBigDecimalWithNull()
    {
        BigDecimal bd = Converter.convertToBigDecimal(null);
        Assert.assertNull(bd);
    }

    @Test
    public void testConvert2BigIntegerWithNull()
    {
        BigInteger bi = Converter.convert2BigInteger(null);
        Assert.assertEquals(BigInteger.ZERO, bi);
    }

    @Test
    public void testConvertToBigIntegerWithNull()
    {
        BigInteger bi = Converter.convertToBigInteger(null);
        Assert.assertNull(bi);
    }

    @Test
    public void testConvertToSqlDate()
    {
        java.util.Date now = new java.util.Date();
        SqlDate sqlDate = Converter.convertToSqlDate(now);
        Assert.assertEquals(now.getTime(), sqlDate.getTime());
    }

    @Test
    public void testConvertToCalendar()
    {
        Calendar cal = Converter.convertToCalendar("2023-12-31");
        Assert.assertNotNull(cal);
        // Verify that the year, month and day match the string representation.
        Assert.assertEquals(2023, cal.get(Calendar.YEAR));
        Assert.assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        Assert.assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }

    /* --------------------------------------------------------------------- */
    /*                     SUPPORTED‑CONVERSION QUERY TESTS                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsConversionSupportedForSimpleTypes()
    {
        Assert.assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
        Assert.assertFalse(Converter.isConversionSupportedFor(String.class, Thread.class));
    }

    @Test
    public void testIsDirectConversionSupported()
    {
        // Direct conversion exists for String → Long
        Assert.assertTrue(Converter.isDirectConversionSupported(String.class, Long.class));
        // No direct conversion for List → Integer
        Assert.assertFalse(Converter.isDirectConversionSupported(List.class, Integer.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported()
    {
        // Simple (non‑collection) conversion should be true
        Assert.assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Double.class));
        // Array‑to‑array is not a simple type conversion
        Assert.assertFalse(Converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
        // Identical type is considered simple
        Assert.assertTrue(Converter.isSimpleTypeConversionSupported(LocalDate.class, LocalDate.class));
    }

    @Test
    public void testAllSupportedConversionsIsNotEmpty()
    {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        Assert.assertNotNull(map);
        Assert.assertFalse(map.isEmpty());
        // Spot‑check that a known source type is present
        Assert.assertTrue(map.containsKey(String.class));
        Assert.assertTrue(map.get(String.class).contains(Integer.class));
    }

    @Test
    public void testGetSupportedConversionsNames()
    {
        Map<String, Set<String>> nameMap = Converter.getSupportedConversions();
        Assert.assertNotNull(nameMap);
        Assert.assertFalse(nameMap.isEmpty());
        Assert.assertTrue(nameMap.containsKey("java.lang.String"));
        Assert.assertTrue(nameMap.get("java.lang.String").contains("java.lang.Integer"));
    }

    /* --------------------------------------------------------------------- */
    /*                     CUSTOM CONVERSION TESTS                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddCustomConversion()
    {
        // Register a conversion from String → MyCustom
        Convert<MyCustom> conv = (from, converter) -> new MyCustom((String) from);
        Convert<?> previous = Converter.addConversion(String.class, MyCustom.class, conv);
        Assert.assertNull(previous); // there should be no prior conversion

        MyCustom result = Converter.convert("custom-value", MyCustom.class);
        Assert.assertEquals(new MyCustom("custom-value"), result);
    }

    @Test
    public void testAddConversionOverridesExisting()
    {
        // Existing conversion String → Integer returns parsed integer.
        // We'll override it to always return 999.
        Convert<Integer> always999 = (from, converter) -> 999;
        Convert<?> previous = Converter.addConversion(String.class, Integer.class, always999);
        Assert.assertNotNull(previous); // there was a conversion before

        Integer overridden = Converter.convert("12345", Integer.class);
        Assert.assertEquals(Integer.valueOf(999), overridden);
    }

    /* --------------------------------------------------------------------- */
    /*                     DEPRECATED HELPER METHOD TESTS                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeprecatedLocalDateToMillis()
    {
        LocalDate ld = LocalDate.of(1970, 1, 2); // exactly one day after epoch
        long millis = Converter.localDateToMillis(ld);
        Assert.assertEquals(24L * 60 * 60 * 1000, millis);
    }

    @Test
    public void testDeprecatedLocalDateTimeToMillis()
    {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 1, 0, 0);
        long expected = 60L * 60 * 1000; // one hour in ms
        long actual = Converter.localDateTimeToMillis(ldt);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDeprecatedZonedDateTimeToMillis()
    {
        ZonedDateTime zdt = ZonedDateTime.of(1970, 1, 1, 0, 0, 30, 0, ZoneId.of("UTC"));
        long expected = 30L * 1000; // 30 seconds
        long actual = Converter.zonedDateTimeToMillis(zdt);
        Assert.assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- */
    /*                     ERROR HANDLING TESTS                              */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConvertNullTargetClassThrows()
    {
        // The static wrapper should forward the IllegalArgumentException from the internal converter.
        Converter.convert("123", null);
    }

    @Test
    public void testConvertUnsupportedConversionThrowsIllegalArgumentException()
    {
        try
        {
            // There is no built‑in conversion from Thread → Integer
            Converter.convert(new Thread(), Integer.class);
            Assert.fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException ex)
        {
            // Expected path
            Assert.assertTrue(ex.getMessage().contains("unsupported"));
        }
    }
}
