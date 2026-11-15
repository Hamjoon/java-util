package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class InstantConversions_toAtomicLong_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter has a public constructor that requires ConverterOptions.
        // Using the default options is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> numberInputs() {
        return Stream.of(Arguments.of("Direct Long instance should be converted to AtomicLong with same value.", 123L, 123L), Arguments.of("Integer instance should be converted to AtomicLong preserving the integer value.", 42, 42L), Arguments.of("Floating‑point Number (Float) should be truncated to long before creating AtomicLong.", 3.14f, 3L));
    }

    private static Stream<Arguments> stringInputs() {
        return Stream.of(Arguments.of("A numeric String should be parsed as a long and wrapped in an AtomicLong.", "123", 123L), Arguments.of("A negative numeric String should be parsed correctly.", "-9876543210", -9876543210L), Arguments.of("A floating‑point numeric String should be truncated to its integer part before conversion.", "3.14159", 3L), Arguments.of("A non‑numeric String should cause a NumberFormatException when conversion is attempted.", "notANumber", NumberFormatException.class));
    }

    private static Stream<Arguments> booleanInputs() {
        return Stream.of(Arguments.of("Boolean true should be converted to 1.", true, 1L), Arguments.of("Boolean false should be converted to 0.", false, 0L));
    }

    private static Stream<Arguments> dateTimeInputs() {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.setTimeInMillis(1625097600000L);
        return Stream.of(Arguments.of("java.util.Date should be converted to its epoch millisecond value.", new Date(1625097600000L), 1625097600000L), Arguments.of("java.time.Instant should be converted to its epoch millisecond value.", Instant.ofEpochMilli(1625097600000L), 1625097600000L), Arguments.of("java.util.Calendar should be converted to its epoch millisecond value.", utcCalendar, 1625097600000L), Arguments.of("LocalDateTime (interpreted as UTC) should be converted to epoch milliseconds.", LocalDateTime.of(2021, 7, 1, 0, 0), 1625097600000L), Arguments.of("LocalDate (midnight UTC) should be converted to epoch milliseconds.", LocalDate.of(2021, 7, 1), 1625097600000L), Arguments.of("LocalTime should be converted to the number of milliseconds since start of day.", LocalTime.of(12, 34, 56), 12L * 3600_000 + 34L * 60_000 + 56L * 1000), Arguments.of("OffsetDateTime should be converted to its epoch millisecond value.", OffsetDateTime.ofInstant(Instant.ofEpochMilli(1625097600000L), ZoneOffset.UTC), 1625097600000L), Arguments.of("ZonedDateTime should be converted to its epoch millisecond value.", ZonedDateTime.ofInstant(Instant.ofEpochMilli(1625097600000L), ZoneId.of("UTC")), 1625097600000L), Arguments.of("java.sql.Timestamp should be converted to epoch milliseconds.", Timestamp.valueOf("2021-07-01 00:00:00"), 1625097600000L));
    }

    private static Stream<Arguments> bigNumberInputs() {
        return Stream.of(Arguments.of("BigInteger values that fit in a long should be converted directly.", new BigInteger("12345678901234567890"), 12345678901234567890L), Arguments.of("BigDecimal should be truncated to its long part before wrapping.", new BigDecimal("9876543210.99"), 9876543210L));
    }

    private static Stream<Arguments> overflowInputs() {
        return Stream.of(Arguments.of("BigInteger value larger than Long.MAX_VALUE should cause an ArithmeticException during conversion.", new BigInteger("9223372036854775808"), ArithmeticException.class), Arguments.of("BigDecimal whose integral part exceeds Long.MAX_VALUE should trigger an ArithmeticException.", new BigDecimal("9223372036854775807.999"), ArithmeticException.class));
    }

    private static Stream<Arguments> specialNumericInputs() {
        return Stream.of(Arguments.of("Passing Double.NaN should result in an IllegalArgumentException because it cannot be represented as a long.", Double.NaN, IllegalArgumentException.class), Arguments.of("Passing Double.POSITIVE_INFINITY should cause an IllegalArgumentException.", Double.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of("Passing Float.NEGATIVE_INFINITY should cause an IllegalArgumentException.", Float.NEGATIVE_INFINITY, IllegalArgumentException.class));
    }

    @Test
    void toAtomicLong_fromLong() {
        Long source = 123L;
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    void toAtomicLong_fromStringNumber() {
        String source = "456";
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(456L, result.get());
    }

    @Test
    void toAtomicLong_fromInstant() {
        Instant source = Instant.ofEpochMilli(1_600_000_000_000L);
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(1_600_000_000_000L, result.get());
    }

    @Test
    void toAtomicLong_fromDate() {
        Date source = new Date(1_500_000_000_000L);
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(1_500_000_000_000L, result.get());
    }

    @Test
    void toAtomicLong_fromMapWithValueKey() {
        // The Converter can extract a numeric value from a map using the key "value"
        Map<String, Object> source = new HashMap<>();
        source.put("value", 789L);
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(789L, result.get());
    }

    @Test
    void toAtomicLong_fromNull() {
        Object source = null;
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        // Converter returns 0 for null when converting to a numeric primitive
        assertEquals(0L, result.get());
    }

    // ---------- Number inputs ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numberInputs")
    void toAtomicLong_numberInputs(String description, Object from, long expected) {
        AtomicLong result = InstantConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    // ---------- String inputs ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringInputs")
    void toAtomicLong_stringInputs(String description, String from, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(NumberFormatException.class)) {
            assertThrows(NumberFormatException.class, () -> InstantConversions.toAtomicLong(from, converter), description);
        } else {
            AtomicLong result = InstantConversions.toAtomicLong(from, converter);
            assertNotNull(result);
            assertEquals(((Number) expected).longValue(), result.get(), description);
        }
    }

    // ---------- Boolean inputs ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("booleanInputs")
    void toAtomicLong_booleanInputs(String description, Boolean from, long expected) {
        AtomicLong result = InstantConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    // ---------- Date and time inputs ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("dateTimeInputs")
    void toAtomicLong_dateTimeInputs(String description, Object from, long expected) {
        AtomicLong result = InstantConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    // ---------- Big number inputs ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bigNumberInputs")
    void toAtomicLong_bigNumberInputs(String description, Number from, long expected) {
        AtomicLong result = InstantConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    // ---------- Unsupported type ----------
    @Test
    void toAtomicLong_unsupportedType() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toAtomicLong(source, converter), "Passing an object of an unsupported type should result in an IllegalArgumentException.");
    }

    // ---------- Converter provided ----------
    @Test
    void toAtomicLong_customConverter() {
        // Mock a Converter that maps the string "custom" -> 777L
        Converter customConverter = mock(Converter.class);
        when(customConverter.convert(eq("custom"), eq(Long.class))).thenReturn(777L);
        // The real implementation of toAtomicLong eventually calls Converter.convert(..., Long.class)
        // (the exact signature is not public, but this stub matches the expected call pattern.)
        AtomicLong result = InstantConversions.toAtomicLong("custom", customConverter);
        assertNotNull(result);
        assertEquals(777L, result.get());
    }

    // ---------- Overflow inputs ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("overflowInputs")
    void toAtomicLong_overflowInputs(String description, Number from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> InstantConversions.toAtomicLong(from, converter), description);
    }

    // ---------- Special numeric inputs ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("specialNumericInputs")
    void toAtomicLong_specialNumericInputs(String description, Number from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> InstantConversions.toAtomicLong(from, converter), description);
    }

    // ---------- Converter exception handling ----------
    @Test
    void toAtomicLong_converterThrowsRuntimeException() {
        Converter exceptionThrowingConverter = mock(Converter.class);
        when(exceptionThrowingConverter.convert(eq("trigger"), any())).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> InstantConversions.toAtomicLong("trigger", exceptionThrowingConverter), "A non‑null Converter that throws a RuntimeException during conversion should propagate that exception.");
    }

    @Test
    void toAtomicLong_converterReturnsNull() {
        Converter nullReturningConverter = mock(Converter.class);
        when(nullReturningConverter.convert(eq("nullResult"), any())).thenReturn(null);
        // The implementation will try to create new AtomicLong(null) which results in NPE
        assertThrows(NullPointerException.class, () -> InstantConversions.toAtomicLong("nullResult", nullReturningConverter), "If the supplied Converter returns null, toAtomicLong should throw a NullPointerException when attempting to create AtomicLong.");
    }
}
