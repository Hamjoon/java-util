package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.CommonValues;
import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Converter}.
 *
 * The suite follows the systematic approach requested:
 *   1. List all public methods (see {@link #listPublicMethods()} comment).
 *   2. Provide a basic test for each method verifying the documented behaviour.
 *   3. Identify edge‑cases and exception scenarios (null handling, unsupported
 *      conversions, deprecated APIs, etc.).
 *   4. Add dedicated tests for those edge‑cases.
 *   5. Combine everything into a single test class.
 *
 * All tests use only the public API that appears in the supplied source files.
 */
public class ConverterTest {

    /** ---------------------------------------------------------------------- *
     *  1. PUBLIC METHOD LIST (signature only – for reference)
     *  ---------------------------------------------------------------------- *
     *
     *  public static <T> T convert(Object from, Class<T> toType)
     *  public static boolean isConversionSupportedFor(Class<?> source, Class<?> target)
     *  public static boolean isSimpleTypeConversionSupported(Class<?> source, Class<?> target)
     *  public static Map<Class<?>, Set<Class<?>>> allSupportedConversions()
     *  public static Map<String, Set<String>> getSupportedConversions()
     *  public static Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionFunction)
     *
     *  // “to‑String” helpers
     *  public static String convert2String(Object fromInstance)
     *  public static String convertToString(Object fromInstance)
     *
     *  // BigDecimal helpers
     *  public static BigDecimal convert2BigDecimal(Object fromInstance)
     *  public static BigDecimal convertToBigDecimal(Object fromInstance)
     *
     *  // BigInteger helpers
     *  public static BigInteger convert2BigInteger(Object fromInstance)
     *  public static BigInteger convertToBigInteger(Object fromInstance)
     *
     *  // SQL/Date helpers
     *  public static java.sql.Date convertToSqlDate(Object fromInstance)
     *  public static Timestamp convertToTimestamp(Object fromInstance)
     *  public static Date convertToDate(Object fromInstance)
     *
     *  // Java‑time helpers
     *  public static LocalDate convertToLocalDate(Object fromInstance)
     *  public static LocalDateTime convertToLocalDateTime(Object fromInstance)
     *  public static ZonedDateTime convertToZonedDateTime(Object fromInstance)
     *
     *  // Calendar helper
     *  public static Calendar convertToCalendar(Object fromInstance)
     *
     *  // Primitive / wrapper helpers (char, byte, short, int, long, float, double, boolean)
     *  public static char   convert2char(Object fromInstance)
     *  public static Character convertToCharacter(Object fromInstance)
     *  public static byte   convert2byte(Object fromInstance)
     *  public static Byte   convertToByte(Object fromInstance)
     *  public static short  convert2short(Object fromInstance)
     *  public static Short  convertToShort(Object fromInstance)
     *  public static int    convert2int(Object fromInstance)
     *  public static Integer convertToInteger(Object fromInstance)
     *  public static long   convert2long(Object fromInstance)
     *  public static Long   convertToLong(Object fromInstance)
     *  public static float  convert2float(Object fromInstance)
     *  public static Float  convertToFloat(Object fromInstance)
     *  public static double convert2double(Object fromInstance)
     *  public static Double convertToDouble(Object fromInstance)
     *  public static boolean convert2boolean(Object fromInstance)
     *  public static Boolean convertToBoolean(Object fromInstance)
     *
     *  // Atomic helpers
     *  public static AtomicInteger convert2AtomicInteger(Object fromInstance)
     *  public static AtomicInteger convertToAtomicInteger(Object fromInstance)
     *  public static AtomicLong    convert2AtomicLong(Object fromInstance)
     *  public static AtomicLong    convertToAtomicLong(Object fromInstance)
     *  public static AtomicBoolean convert2AtomicBoolean(Object fromInstance)
     *  public static AtomicBoolean convertToAtomicBoolean(Object fromInstance)
     *
     *  // Deprecated epoch‑millis helpers
     *  @Deprecated public static long localDateToMillis(LocalDate localDate)
     *  @Deprecated public static long localDateTimeToMillis(LocalDateTime localDateTime)
     *  @Deprecated public static long zonedDateTimeToMillis(ZonedDateTime zonedDateTime)
     *
     * ----------------------------------------------------------------------- */

    /** ---------------------------------------------------------------------- *
     *  2. BASIC FUNCTIONALITY TESTS
     *  ---------------------------------------------------------------------- */

    @Test
    public void testConvert_StringToInteger() {
        Integer result = Converter.convert("42", Integer.class);
        assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testConvert_NullTargetTypeThrows() {
        try {
            Converter.convert("123", null);
            fail("Expected IllegalArgumentException for null target type");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testIsConversionSupportedFor_KnownSupported() {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
    }

    @Test
    public void testIsConversionSupportedFor_Unsupported() {
        assertFalse(Converter.isConversionSupportedFor(Object.class, Thread.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported_KnownSupported() {
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Integer.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported_ArrayReturnsFalse() {
        assertFalse(Converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
    }

    @Test
    public void testAllSupportedConversions_NotEmpty() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
    }

    @Test
    public void testGetSupportedConversions_NotEmpty() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
    }

    /** ---------------------------------------------------------------------- *
     *  3. EDGE‑CASE & EXCEPTION TESTS
     *  ---------------------------------------------------------------------- */

    @Test
    public void testConvert2String_NullInput() {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvertToString_NullInput() {
        assertNull(Converter.convertToString(null));
    }

    @Test
    public void testConvert2BigDecimal_NullInput() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
    }

    @Test
    public void testConvertToBigDecimal_NullInput() {
        assertNull(Converter.convertToBigDecimal(null));
    }

    @Test
    public void testConvert2BigInteger_NullInput() {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
    }

    @Test
    public void testConvertToBigInteger_NullInput() {
        assertNull(Converter.convertToBigInteger(null));
    }

    @Test
    public void testConvert2char_NullInput() {
        assertEquals('\0', Converter.convert2char(null));
    }

    @Test
    public void testConvert2boolean_NullInput() {
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testConvert2int_NullInput() {
        assertEquals(0, Converter.convert2int(null));
    }

    @Test
    public void testConvert2long_NullInput() {
        assertEquals(CommonValues.LONG_ZERO.longValue(), Converter.convert2long(null));
    }

    @Test
    public void testConvert2float_NullInput() {
        assertEquals(CommonValues.FLOAT_ZERO.floatValue(), Converter.convert2float(null), 0.0f);
    }

    @Test
    public void testConvert2double_NullInput() {
        assertEquals(CommonValues.DOUBLE_ZERO.doubleValue(), Converter.convert2double(null), 0.0);
    }

    @Test
    public void testConvert2AtomicInteger_NullInput() {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        assertNotNull(ai);
        assertEquals(0, ai.get());
    }

    @Test
    public void testConvert2AtomicLong_NullInput() {
        AtomicLong al = Converter.convert2AtomicLong(null);
        assertNotNull(al);
        assertEquals(0L, al.get());
    }

    @Test
    public void testConvert2AtomicBoolean_NullInput() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    @Test
    public void testUnsupportedConversionThrows() {
        try {
            // Object has no sensible conversion to Integer in the default set
            Converter.convert(new Object(), Integer.class);
            fail("Expected IllegalArgumentException for unsupported conversion");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testAddCustomConversionAndOverride() {
        // First, add a custom conversion from String -> Integer (length of the string)
        Convert<Integer> lengthConversion = (from, conv) -> ((String) from).length();
        Convert<?> previous = Converter.addConversion(String.class, Integer.class, lengthConversion);
        // The library already has a conversion, so previous should be non‑null
        assertNotNull(previous);

        // Verify that the new conversion is now used
        Integer len = Converter.convert("hello", Integer.class);
        assertEquals(Integer.valueOf(5), len);
    }

    @Test
    public void testDeprecatedLocalDateToMillis() {
        LocalDate date = LocalDate.of(1970, 1, 1);
        long millis = Converter.localDateToMillis(date);
        assertEquals(0L, millis);
    }

    @Test
    public void testDeprecatedLocalDateTimeToMillis() {
        LocalDateTime dateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        long millis = Converter.localDateTimeToMillis(dateTime);
        assertEquals(0L, millis);
    }

    @Test
    public void testDeprecatedZonedDateTimeToMillis() {
        ZonedDateTime zdt = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
        long millis = Converter.zonedDateTimeToMillis(zdt);
        assertEquals(0L, millis);
    }

    /** ---------------------------------------------------------------------- *
     *  4. VALIDATION OF DATE/TIME CONVERSIONS
     *  ---------------------------------------------------------------------- */

    @Test
    public void testConvertToLocalDate() {
        LocalDate date = LocalDate.of(2023, 3, 14);
        LocalDate result = Converter.convertToLocalDate(date);
        assertEquals(date, result);
    }

    @Test
    public void testConvertToLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 14, 15, 9, 26);
        LocalDateTime result = Converter.convertToLocalDateTime(ldt);
        assertEquals(ldt, result);
    }

    @Test
    public void testConvertToZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.now();
        ZonedDateTime result = Converter.convertToZonedDateTime(zdt);
        assertEquals(zdt, result);
    }

    @Test
    public void testConvertToCalendar() {
        Calendar cal = Calendar.getInstance();
        Calendar result = Converter.convertToCalendar(cal);
        assertEquals(cal, result);
    }

    /** ---------------------------------------------------------------------- *
     *  5. VALIDATION OF PRIMITIVE/WAPPER CONVERSIONS
     *  ---------------------------------------------------------------------- */

    @Test
    public void testConvertToCharacter() {
        Character ch = Converter.convertToCharacter("A");
        assertEquals(Character.valueOf('A'), ch);
    }

    @Test
    public void testConvertToByte() {
        Byte b = Converter.convertToByte("127");
        assertEquals(Byte.valueOf((byte) 127), b);
    }

    @Test
    public void testConvertToShort() {
        Short s = Converter.convertToShort("32000");
        assertEquals(Short.valueOf((short) 32000), s);
    }

    @Test
    public void testConvertToInteger() {
        Integer i = Converter.convertToInteger("12345");
        assertEquals(Integer.valueOf(12345), i);
    }

    @Test
    public void testConvertToLong() {
        Long l = Converter.convertToLong("1234567890123");
        assertEquals(Long.valueOf(1234567890123L), l);
    }

    @Test
    public void testConvertToFloat() {
        Float f = Converter.convertToFloat("3.14");
        assertEquals(Float.valueOf(3.14f), f);
    }

    @Test
    public void testConvertToDouble() {
        Double d = Converter.convertToDouble("2.71828");
        assertEquals(Double.valueOf(2.71828), d);
    }

    @Test
    public void testConvertToBoolean() {
        Boolean bTrue = Converter.convertToBoolean("true");
        Boolean bFalse = Converter.convertToBoolean("false");
        assertTrue(bTrue);
        assertFalse(bFalse);
    }

    /** ---------------------------------------------------------------------- *
     *  6. VALIDATION OF ATOMIC CONVERSIONS
     *  ---------------------------------------------------------------------- */

    @Test
    public void testConvertToAtomicInteger() {
        AtomicInteger ai = Converter.convertToAtomicInteger("77");
        assertNotNull(ai);
        assertEquals(77, ai.get());
    }

    @Test
    public void testConvertToAtomicLong() {
        AtomicLong al = Converter.convertToAtomicLong("123456789");
        assertNotNull(al);
        assertEquals(123456789L, al.get());
    }

    @Test
    public void testConvertToAtomicBoolean() {
        AtomicBoolean ab = Converter.convertToAtomicBoolean("true");
        assertNotNull(ab);
        assertTrue(ab.get());
    }

    /** ---------------------------------------------------------------------- *
     *  7. VALIDATION OF SQL/DATES CONVERSIONS
     *  ---------------------------------------------------------------------- */

    @Test
    public void testConvertToSqlDate() {
        java.sql.Date sqlDate = Converter.convertToSqlDate(java.sql.Date.valueOf("2022-01-01"));
        assertEquals(java.sql.Date.valueOf("2022-01-01"), sqlDate);
    }

    @Test
    public void testConvertToTimestamp() {
        Timestamp ts = Timestamp.valueOf("2022-01-01 12:34:56.789");
        Timestamp result = Converter.convertToTimestamp(ts);
        assertEquals(ts, result);
    }

    @Test
    public void testConvertToDate() {
        Date now = new Date();
        Date result = Converter.convertToDate(now);
        assertEquals(now, result);
    }

    /** ---------------------------------------------------------------------- *
     *  8. SMOKE TEST FOR convert(Object, Class) WITH VARIOUS TARGETS
     *  ---------------------------------------------------------------------- */

    @Test
    public void testConvertVariousTypes() {
        assertEquals("123", Converter.convert(123, String.class));
        assertEquals(Integer.valueOf(123), Converter.convert("123", Integer.class));
        assertEquals(Double.valueOf(12.5), Converter.convert("12.5", Double.class));
        assertEquals(Boolean.TRUE, Converter.convert("true", Boolean.class));
        assertEquals(Character.valueOf('Z'), Converter.convert("Z", Character.class));
    }

    /** ---------------------------------------------------------------------- *
     *  Helper method to ensure the test class compiles even if the library
     *  adds new overloads later.  Not part of the test logic.
     *  ---------------------------------------------------------------------- */
    private void listPublicMethods() {
        // Method list is documented at the top of this file.
    }
}
