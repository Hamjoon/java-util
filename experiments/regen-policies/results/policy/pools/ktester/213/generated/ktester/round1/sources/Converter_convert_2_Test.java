package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert_2_Test {

    static Stream<Arguments> basicTypeConversionProvider() {
        return Stream.of(Arguments.of("String \"123\" → Integer", "123", Integer.class, 123), Arguments.of("String \"true\" → Boolean", "true", Boolean.class, true), Arguments.of("ISO‑8601 string → LocalDateTime", "2023-01-01T12:34:56", LocalDateTime.class, LocalDateTime.of(2023, 1, 1, 12, 34, 56)), Arguments.of("ISO‑8601 date string → LocalDate", "2023-01-01", LocalDate.class, LocalDate.of(2023, 1, 1)), Arguments.of("UTC ISO‑8601 string → ZonedDateTime (UTC)", "2023-01-01T12:34:56Z", ZonedDateTime.class, ZonedDateTime.ofInstant(Instant.parse("2023-01-01T12:34:56Z"), ZoneOffset.UTC)), Arguments.of("Numeric string → BigDecimal", "123.45", BigDecimal.class, new BigDecimal("123.45")), Arguments.of("Large numeric string → BigInteger", "12345678901234567890", BigInteger.class, new BigInteger("12345678901234567890")), Arguments.of("Single‑character string → Character", "c", Character.class, 'c'), Arguments.of("UUID string → UUID (lower‑case)", "A1B2C3D4-E5F6-7890-1234-56789ABCDEF0", UUID.class, UUID.fromString("a1b2c3d4-e5f6-7890-1234-56789abcdef0")), Arguments.of("ISO‑8601 date string → java.sql.Date", "2023-01-01", java.sql.Date.class, java.sql.Date.valueOf(LocalDate.of(2023, 1, 1))));
    }

    static Stream<Arguments> primitiveWrapperConversionProvider() {
        return Stream.of(Arguments.of("Integer → String", 42, String.class, "42"), Arguments.of("Double → Integer (truncation)", 3.1415, Integer.class, 3), Arguments.of("Boolean true → Integer", true, Integer.class, 1), Arguments.of("Boolean false → String", false, String.class, "false"));
    }

    static Stream<Arguments> nullInputExceptionProvider() {
        return Stream.of(Arguments.of("null source → Integer (wrapper) – IllegalArgumentException", null, Integer.class, IllegalArgumentException.class), Arguments.of("null target class – NullPointerException", "123", null, NullPointerException.class), Arguments.of("null source → primitive int – IllegalArgumentException", null, int.class, IllegalArgumentException.class), Arguments.of("null source → primitive boolean – IllegalArgumentException", null, boolean.class, IllegalArgumentException.class));
    }

    static Stream<Arguments> unsupportedConversionProvider() {
        return Stream.of(Arguments.of("Object → LocalDate – IllegalArgumentException", new Object(), LocalDate.class, IllegalArgumentException.class), Arguments.of("Invalid UUID string → UUID – IllegalArgumentException", "not-a-uuid", UUID.class, IllegalArgumentException.class), Arguments.of("Non‑numeric string → Integer – NumberFormatException", "abc", Integer.class, NumberFormatException.class), Arguments.of("Invalid month in date string → LocalDate – DateTimeParseException", "2023-13-01", LocalDate.class, DateTimeParseException.class), Arguments.of("Invalid boolean string → Boolean – IllegalArgumentException", "not-a-boolean", Boolean.class, IllegalArgumentException.class));
    }

    static Stream<Arguments> primitiveTargetExceptionProvider() {
        return Stream.of(Arguments.of("Floating‑point string → primitive int – NumberFormatException", "123.456", int.class, NumberFormatException.class), Arguments.of("Boolean string → primitive int – IllegalArgumentException", "true", int.class, IllegalArgumentException.class));
    }

    @Test
    @DisplayName("convert(String, Long) returns correct Long value")
    void testConvertStringToLong() {
        // given
        String source = "12345";
        // when
        Long result = Converter.convert(source, Long.class);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(12345L, result.longValue(), "String should be converted to the correct Long value");
    }

    @Test
    @DisplayName("convert(null, Integer) returns null for wrapper type")
    void testConvertNullToWrapper() {
        // given
        Object source = null;
        // when
        Integer result = Converter.convert(source, Integer.class);
        // then
        assertNull(result, "Null source should produce null for object wrapper types");
    }

    @Test
    @DisplayName("convert(null, int) returns 0 for primitive int")
    void testConvertNullToPrimitiveInt() {
        // given
        Object source = null;
        // when
        int result = Converter.convert(source, int.class);
        // then
        assertEquals(0, result, "Null source should produce 0 for primitive int");
    }

    @Test
    @DisplayName("convert(String, Boolean) returns correct Boolean")
    void testConvertStringToBoolean() {
        // given
        String source = "true";
        // when
        Boolean result = Converter.convert(source, Boolean.class);
        // then
        assertNotNull(result);
        assertTrue(result, "String \"true\" should be converted to Boolean true");
    }

    @Test
    @DisplayName("convert(Calendar, Date) converts calendar to Date")
    void testConvertCalendarToDate() {
        // given
        Calendar cal = Calendar.getInstance();
        cal.set(2022, Calendar.JANUARY, 15, 10, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long expectedTime = cal.getTimeInMillis();
        // when
        Date date = Converter.convert(cal, Date.class);
        // then
        assertNotNull(date);
        assertEquals(expectedTime, date.getTime(), "Calendar should be converted to Date with same epoch millis");
    }

    /* ---------- Basic Type Conversions ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicTypeConversionProvider")
    void testBasicTypeConversions(String description, Object from, Class<?> toType, Object expected) {
        Object result = Converter.convert(from, toType);
        assertNotNull(result, "Result should not be null for " + description);
        assertEquals(expected, result, "Conversion mismatch for " + description);
    }

    /* ---------- Primitive and Wrapper Conversions ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("primitiveWrapperConversionProvider")
    void testPrimitiveWrapperConversions(String description, Object from, Class<?> toType, Object expected) {
        Object result = Converter.convert(from, toType);
        assertNotNull(result, "Result should not be null for " + description);
        assertEquals(expected, result, "Conversion mismatch for " + description);
    }

    /* ---------- Null Input Exceptions ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullInputExceptionProvider")
    void testNullInputExceptions(String description, Object from, Class<?> toType, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convert(from, toType), "Expected exception for: " + description);
    }

    /* ---------- Unsupported Conversion Exceptions ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedConversionProvider")
    void testUnsupportedConversions(String description, Object from, Class<?> toType, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convert(from, toType), "Expected exception for: " + description);
    }

    /* ---------- Primitive Target Conversion Exceptions ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("primitiveTargetExceptionProvider")
    void testPrimitiveTargetExceptions(String description, Object from, Class<?> toType, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convert(from, toType), "Expected exception for: " + description);
    }

    /* ---------- Collection/Array Unsupported Conversions ---------- */
    @Test
    @DisplayName("Converting List<Integer> to Integer[] should throw IllegalArgumentException")
    void testListToArrayUnsupported() {
        List<Integer> source = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> Converter.convert(source, Integer[].class), "List → Integer[] conversion is not supported");
    }
}
