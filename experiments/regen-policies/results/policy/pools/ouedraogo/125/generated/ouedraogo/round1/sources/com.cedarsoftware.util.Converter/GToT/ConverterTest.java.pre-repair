package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import com.cedarsoftware.util.convert.Converter;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link com.cedarsoftware.util.Converter}.
 *
 * The suite follows the steps requested:
 * <ol>
 *   <li>List of public static methods (signatures) of {@code Converter}.</li>
 *   <li>Basic functional test for each method.</li>
 *   <li>Identification of edge‑cases / exceptional scenarios.</li>
 *   <li>Targeted tests for those edge‑cases.</li>
 *   <li>All tests merged into a single file.</li>
 * </ol>
 *
 * Only APIs that appear in the supplied source code (or in the small
 * dependency list) are used – no mocking, no external libraries besides JUnit‑4.
 */
public class ConverterTest {

    /* -----------------------------------------------------------------
     *  1. PUBLIC STATIC METHOD SIGNATURES (extracted from the source)
     * -----------------------------------------------------------------
     *
     * public static <T> T convert(Object from, Class<T> toType)
     * public static boolean isConversionSupportedFor(Class<?> source, Class<?> target)
     * public static boolean isDirectConversionSupported(Class<?> source, Class<?> target)
     * public static boolean isSimpleTypeConversionSupported(Class<?> source, Class<?> target)
     * public static Map<Class<?>, Set<Class<?>>> allSupportedConversions()
     * public static Map<String, Set<String>> getSupportedConversions()
     * public static Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionFunction)
     * public static String convert2String(Object fromInstance)
     * public static String convertToString(Object fromInstance)
     * public static BigDecimal convert2BigDecimal(Object fromInstance)
     * public static BigDecimal convertToBigDecimal(Object fromInstance)
     * public static BigInteger convert2BigInteger(Object fromInstance)
     * public static BigInteger convertToBigInteger(Object fromInstance)
     * public static java.sql.Date convertToSqlDate(Object fromInstance)
     * public static Timestamp convertToTimestamp(Object fromInstance)
     * public static Date convertToDate(Object fromInstance)
     * public static LocalDate convertToLocalDate(Object fromInstance)
     * public static LocalDateTime convertToLocalDateTime(Object fromInstance)
     * public static ZonedDateTime convertToZonedDateTime(Object fromInstance)
     * public static Calendar convertToCalendar(Object fromInstance)
     * public static char convert2char(Object fromInstance)
     * public static Character convertToCharacter(Object fromInstance)
     * public static byte convert2byte(Object fromInstance)
     * public static Byte convertToByte(Object fromInstance)
     * public static short convert2short(Object fromInstance)
     * public static Short convertToShort(Object fromInstance)
     * public static int convert2int(Object fromInstance)
     * public static Integer convertToInteger(Object fromInstance)
     * public static long convert2long(Object fromInstance)
     * public static Long convertToLong(Object fromInstance)
     * public static float convert2float(Object fromInstance)
     * public static Float convertToFloat(Object fromInstance)
     * public static double convert2double(Object fromInstance)
     * public static Double convertToDouble(Object fromInstance)
     * public static boolean convert2boolean(Object fromInstance)
     * public static Boolean convertToBoolean(Object fromInstance)
     * public static AtomicInteger convert2AtomicInteger(Object fromInstance)
     * public static AtomicInteger convertToAtomicInteger(Object fromInstance)
     * public static AtomicLong convert2AtomicLong(Object fromInstance)
     * public static AtomicLong convertToAtomicLong(Object fromInstance)
     * public static AtomicBoolean convert2AtomicBoolean(Object fromInstance)
     * public static AtomicBoolean convertToAtomicBoolean(Object fromInstance)
     * @Deprecated public static long localDateToMillis(LocalDate localDate)
     * @Deprecated public static long localDateTimeToMillis(LocalDateTime localDateTime)
     * @Deprecated public static long zonedDateTimeToMillis(ZonedDateTime zonedDateTime)
     */

    /* -----------------------------------------------------------------
     *  2. BASIC FUNCTIONAL TESTS
     * ----------------------------------------------------------------- */

    @Test
    public void testConvert_basicTypes() {
        Integer i = Converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), i);

        Long l = Converter.convert(45.6, Long.class);
        assertEquals(Long.valueOf(45L), l);

        Boolean b = Converter.convert("true", Boolean.class);
        assertTrue(b);
    }

    @Test
    public void testConvert_nullHandling_objectTypes() {
        assertNull(Converter.convert(null, String.class));
        assertNull(Converter.convert(null, Integer.class));
    }

    @Test
    public void testConvert_nullHandling_primitives() {
        assertEquals(0, (int) Converter.convert(null, int.class));
        assertEquals(0L, (long) Converter.convert(null, long.class));
        assertEquals(false, (boolean) Converter.convert(null, boolean.class));
        assertEquals('\u0000', (char) Converter.convert(null, char.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvert_unsupportedConversionThrows() {
        // Object -> Integer is not a supported conversion
        Converter.convert(new Object(), Integer.class);
    }

    @Test
    public void testIsConversionSupportedFor_simple() {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
        assertFalse(Converter.isConversionSupportedFor(String.class, Map.class));
    }

    @Test
    public void testIsDirectConversionSupported() {
        assertTrue(Converter.isDirectConversionSupported(String.class, Integer.class));
        // Direct conversion for collection types is not considered "direct"
        assertFalse(Converter.isDirectConversionSupported(List.class, Set.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported() {
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        assertFalse(Converter.isSimpleTypeConversionSupported(List.class, Set.class));
        // identity conversion
        assertTrue(Converter.isSimpleTypeConversionSupported(ZonedDateTime.class, ZonedDateTime.class));
    }

    @Test
    public void testAllSupportedConversions_notEmpty() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
        // Spot‑check that String can be converted to at least one other type
        assertTrue(map.get(String.class).size() > 0);
    }

    @Test
    public void testGetSupportedConversions_consistency() {
        Map<String, Set<String>> byName = Converter.getSupportedConversions();
        assertNotNull(byName);
        assertFalse(byName.isEmpty());
        // The entry for "java.lang.String" must exist
        assertTrue(byName.containsKey("java.lang.String"));
    }

    @Test
    public void testAddConversion_andUseIt() {
        // custom conversion: from String to UUID (already supported, but we create a trivial one)
        Convert<UUID> toUuid = (from, conv) -> UUID.nameUUIDFromBytes(((String) from).getBytes());

        Convert<?> previous = Converter.addConversion(String.class, UUID.class, toUuid);
        // previous may be null (default conversion) – we only care that the new conversion works
        UUID uuid = Converter.convert("test", UUID.class);
        assertNotNull(uuid);
        // Verify that the generated UUID matches the deterministic name‑based algorithm
        assertEquals(UUID.nameUUIDFromBytes("test".getBytes()), uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddConversion_nullTargetThrows() {
        Convert<String> dummy = (from, conv) -> "dummy";
        Converter.addConversion(String.class, null, dummy);
    }

    @Test
    public void testConvert2String_nullReturnsEmptyString() {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvertToString_nullReturnsNull() {
        assertNull(Converter.convertToString(null));
    }

    @Test
    public void testConvert2BigDecimal_nullAndEmpty() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(""));
    }

    @Test
    public void testConvertToBigDecimal_nullAndEmpty() {
        assertNull(Converter.convertToBigDecimal(null));
        // Empty string is converted to zero by underlying converter
        assertEquals(BigDecimal.ZERO, Converter.convertToBigDecimal(""));
    }

    @Test
    public void testConvert2BigInteger_nullAndEmpty() {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(""));
    }

    @Test
    public void testConvertToBigInteger_nullAndEmpty() {
        assertNull(Converter.convertToBigInteger(null));
        assertEquals(BigInteger.ZERO, Converter.convertToBigInteger(""));
    }

    @Test
    public void testDateConversions() {
        java.util.Date now = new java.util.Date();
        java.sql.Date sqlDate = Converter.convertToSqlDate(now);
        assertNotNull(sqlDate);
        assertEquals(now.getTime(), sqlDate.getTime());

        Timestamp ts = Converter.convertToTimestamp(now);
        assertNotNull(ts);
        assertEquals(now.getTime(), ts.getTime());

        Date utilDate = Converter.convertToDate(now);
        assertNotNull(utilDate);
        assertEquals(now, utilDate);
    }

    @Test
    public void testJavaTimeConversions() {
        LocalDate ld = LocalDate.of(2022, 3, 14);
        LocalDate resultLd = Converter.convertToLocalDate("2022-03-14");
        assertEquals(ld, resultLd);

        LocalDateTime ldt = LocalDateTime.of(2022, 3, 14, 12, 30, 45);
        LocalDateTime resultLdt = Converter.convertToLocalDateTime("2022-03-14T12:30:45");
        assertEquals(ldt, resultLdt);

        ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.of("UTC"));
        ZonedDateTime resultZdt = Converter.convertToZonedDateTime("2022-03-14T12:30:45Z[UTC]");
        assertEquals(zdt, resultZdt);
    }

    @Test
    public void testConvertToCalendar() {
        Calendar cal = Converter.convertToCalendar(Instant.now().toEpochMilli());
        assertNotNull(cal);
        // The calendar should hold the same epoch millis we supplied
        assertEquals(Instant.now().toEpochMilli(), cal.getTimeInMillis(), 10); // tolerance for timing
    }

    @Test
    public void testPrimitiveConverters_nullHandling() {
        assertEquals((char) 0, Converter.convert2char(null));
        assertEquals((byte) 0, Converter.convert2byte(null));
        assertEquals((short) 0, Converter.convert2short(null));
        assertEquals(0, Converter.convert2int(null));
        assertEquals(0L, Converter.convert2long(null));
        assertEquals(0.0f, Converter.convert2float(null), 0.0f);
        assertEquals(0.0d, Converter.convert2double(null), 0.0d);
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testWrapperConverters_nullHandling() {
        assertNull(Converter.convertToCharacter(null));
        assertNull(Converter.convertToByte(null));
        assertNull(Converter.convertToShort(null));
        assertNull(Converter.convertToInteger(null));
        assertNull(Converter.convertToLong(null));
        assertNull(Converter.convertToFloat(null));
        assertNull(Converter.convertToDouble(null));
        assertNull(Converter.convertToBoolean(null));
    }

    @Test
    public void testAtomicConverters_nullHandling() {
        assertEquals(new AtomicInteger(0), Converter.convert2AtomicInteger(null));
        assertNull(Converter.convertToAtomicInteger(null));

        assertEquals(new AtomicLong(0L), Converter.convert2AtomicLong(null));
        assertNull(Converter.convertToAtomicLong(null));

        assertEquals(new AtomicBoolean(false), Converter.convert2AtomicBoolean(null));
        assertNull(Converter.convertToAtomicBoolean(null));
    }

    @Test
    public void testDeprecatedLocalDateToMillis() {
        LocalDate ld = LocalDate.of(1970, 1, 2); // one day after epoch
        long expected = ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        assertEquals(expected, Converter.localDateToMillis(ld));
    }

    @Test
    public void testDeprecatedLocalDateTimeToMillis() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 2, 3, 4, 5);
        long expected = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        assertEquals(expected, Converter.localDateTimeToMillis(ldt));
    }

    @Test
    public void testDeprecatedZonedDateTimeToMillis() {
        ZonedDateTime zdt = ZonedDateTime.of(1970, 1, 2, 3, 4, 5, 0, ZoneId.of("UTC"));
        long expected = zdt.toInstant().toEpochMilli();
        assertEquals(expected, Converter.zonedDateTimeToMillis(zdt));
    }

    /* -----------------------------------------------------------------
     *  3. EDGE CASES & EXCEPTION SCENARIOS
     * ----------------------------------------------------------------- */

    @Test
    public void testConvert_invalidStringToNumberThrows() {
        try {
            Converter.convert("not-a-number", Integer.class);
            fail("Expected IllegalArgumentException for invalid numeric string");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testConvert_arrayToArrayUnsupported() {
        // String[] -> Integer[] is not a supported conversion (deep check)
        assertFalse(Converter.isConversionSupportedFor(String[].class, Integer[].class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported_withCollections() {
        assertFalse(Converter.isSimpleTypeConversionSupported(List.class, Set.class));
    }

    @Test
    public void testAddConversion_overwriteExisting() {
        // First custom conversion
        Convert<String> upperCase = (from, conv) -> ((String) from).toUpperCase();
        Converter.addConversion(String.class, String.class, upperCase);
        String result = Converter.convert("test", String.class);
        assertEquals("TEST", result);

        // Overwrite with identity conversion
        Convert<String> identity = (from, conv) -> (String) from;
        Converter.addConversion(String.class, String.class, identity);
        result = Converter.convert("test", String.class);
        assertEquals("test", result);
    }

    @Test
    public void testConvert2String_nonStringInput() {
        // Passing an Integer should be converted to its String representation
        assertEquals("42", Converter.convert2String(42));
    }

    @Test
    public void testConvertToString_nonStringInput() {
        // Passing a Boolean should be converted to its String representation
        assertEquals("true", Converter.convertToString(Boolean.TRUE));
    }

    @Test
    public void testConvert2char_numericString() {
        // "65" should become 'A'
        assertEquals('A', Converter.convert2char("65"));
    }

    @Test
    public void testConvert2boolean_numericString() {
        // "1" should be true, "0" should be false
        assertTrue(Converter.convert2boolean("1"));
        assertFalse(Converter.convert2boolean("0"));
    }

    @Test
    public void testConvertToBoolean_invalidString() {
        try {
            Converter.convertToBoolean("notABoolean");
            fail("Expected IllegalArgumentException for invalid boolean string");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testConvertToCalendar_invalidType() {
        try {
            Converter.convertToCalendar(new Object());
            fail("Expected IllegalArgumentException for unsupported Calendar conversion");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testConvert2BigDecimal_invalidString() {
        try {
            Converter.convert2BigDecimal("NaN");
            fail("Expected IllegalArgumentException for non‑numeric BigDecimal conversion");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testConvertToBigInteger_invalidString() {
        try {
            Converter.convertToBigInteger("xyz");
            fail("Expected IllegalArgumentException for non‑numeric BigInteger conversion");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
