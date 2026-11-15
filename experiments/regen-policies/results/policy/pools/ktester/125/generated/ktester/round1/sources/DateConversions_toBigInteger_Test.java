package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

class DateConversions_toBigInteger_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options for the real Converter.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // Force the JVM default time‑zone to UTC so that LocalDateTime tests are deterministic.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// String that can be parsed as a number
        Arguments.of("12345", BigInteger.valueOf(12345L)), // Long value (epoch‑millis)
        Arguments.of(12345L, BigInteger.valueOf(12345L)), // Date representing epoch start
        Arguments.of(new Date(0L), BigInteger.ZERO), // LocalDate at epoch (1970‑01‑01)
        Arguments.of(LocalDate.of(1970, 1, 1), BigInteger.ZERO), // Instant (epoch seconds)
        Arguments.of(Instant.ofEpochSecond(123456789L), BigInteger.valueOf(123456789L)), // BigDecimal (numeric value)
        Arguments.of(new BigDecimal("9876543210"), BigInteger.valueOf(9876543210L)), // AtomicLong
        Arguments.of(new AtomicLong(42L), BigInteger.valueOf(42L)), // Integer (auto‑boxed)
        Arguments.of(Integer.valueOf(42), BigInteger.valueOf(42L)), // Negative long
        Arguments.of(-98765L, BigInteger.valueOf(-98765L)), // BigInteger input (within long range)
        Arguments.of(new BigInteger("12345678901234567890"), new BigInteger("12345678901234567890")), // java.sql.Timestamp (fractional part discarded)
        Arguments.of(Timestamp.valueOf("1970-01-01 00:00:12.345"), BigInteger.valueOf(12L)), // java.sql.Date (epoch day)
        Arguments.of(java.sql.Date.valueOf("1970-01-01"), BigInteger.ZERO), // Calendar set to 555 000 ms (555 seconds)
        Arguments.of(createCalendarAtMillis(555_000L), BigInteger.valueOf(555L)), // ZonedDateTime in UTC
        Arguments.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(777L), ZoneOffset.UTC), BigInteger.valueOf(777L)), // OffsetDateTime in UTC
        Arguments.of(OffsetDateTime.ofInstant(Instant.ofEpochSecond(999L), ZoneOffset.UTC), BigInteger.valueOf(999L)), // LocalDateTime interpreted in the system default zone (UTC for this test)
        Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0, 30), BigInteger.valueOf(30L)));
    }

    private static Calendar createCalendarAtMillis(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// String with whitespace that cannot be parsed cleanly
        Arguments.of("  77  ", ConversionException.class), // Decimal string
        Arguments.of("12.34", ConversionException.class), // Unsupported object type
        Arguments.of(new Object(), ConversionException.class), // Double.NaN
        Arguments.of(Double.NaN, ConversionException.class), // Double.POSITIVE_INFINITY
        Arguments.of(Double.POSITIVE_INFINITY, ConversionException.class), // Float.NaN
        Arguments.of(Float.NaN, ConversionException.class), // Invalid ISO‑8601 date (month 13)
        Arguments.of("2020-13-01T00:00:00Z", ConversionException.class), // Empty string
        Arguments.of("", ConversionException.class), // Optional (unsupported)
        Arguments.of(Optional.of(123), ConversionException.class));
    }

    @Test
    void toBigInteger_givenInstant_returnsExpectedBigInteger() {
        // GIVEN
        // deterministic value
        Instant instant = Instant.ofEpochSecond(1_234_567L, 890_000_000L);
        BigInteger expected = converter.convert(instant, BigInteger.class);
        // WHEN
        BigInteger actual = DateConversions.toBigInteger(instant, converter);
        // THEN
        assertEquals(expected, actual, "BigInteger conversion of an Instant should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_givenDate_returnsExpectedBigInteger() {
        // GIVEN
        // deterministic epoch‑millis value
        Date date = new Date(1_234_567_890L);
        BigInteger expected = converter.convert(date, BigInteger.class);
        // WHEN
        BigInteger actual = DateConversions.toBigInteger(date, converter);
        // THEN
        assertEquals(expected, actual, "BigInteger conversion of a java.util.Date should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_givenNull_returnsNull() {
        // GIVEN
        Object from = null;
        // WHEN
        BigInteger actual = DateConversions.toBigInteger(from, converter);
        // THEN
        assertNull(actual, "Converting a null source should yield null for object return types");
    }

    /* -------------------------------------------------------------
     *  Successful conversion test cases (parameterised)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: from={0} → {1}")
    @MethodSource("successfulConversionProvider")
    void toBigInteger_successfulConversions(Object from, BigInteger expected) {
        // WHEN
        BigInteger actual = DateConversions.toBigInteger(from, converter);
        // THEN
        assertEquals(expected, actual, () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should yield " + expected);
    }

    /* -------------------------------------------------------------
     *  Null handling test cases
     * ------------------------------------------------------------- */
    @Test
    void toBigInteger_givenNullSource_returnsNull() {
        // WHEN
        BigInteger actual = DateConversions.toBigInteger(null, converter);
        // THEN
        assertNull(actual, "Converting a null source should yield null");
    }

    @Test
    void toBigInteger_givenNullSourceAndNullConverter_throwsNPE() {
        // GIVEN both arguments null
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toBigInteger(null, null), "Both null arguments should cause a NullPointerException");
    }

    @Test
    void toBigInteger_givenNonNullSourceAndNullConverter_throwsNPE() {
        // GIVEN a non‑null source but null converter
        Object source = "12345";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toBigInteger(source, null), "A null converter should cause a NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Exception propagation test cases (parameterised)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: from={0} → throws {1}")
    @MethodSource("exceptionConversionProvider")
    void toBigInteger_expectedExceptions(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> DateConversions.toBigInteger(from, converter), () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should throw " + expectedException.getSimpleName());
    }
}
