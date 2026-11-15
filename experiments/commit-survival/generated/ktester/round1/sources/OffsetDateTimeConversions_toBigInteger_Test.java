package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toBigInteger_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * Simple holder for successful conversion test data.
     */
    private static class ArgumentsHolder {

        final Object input;

        final BigInteger expected;

        final String description;

        ArgumentsHolder(Object input, BigInteger expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * Holder for exception‑expectation test data.
     */
    private static class ExceptionCase {

        final Object input;

        final Class<? extends Throwable> expectedException;

        final String description;

        final boolean nullConverter;

        ExceptionCase(Object input, Class<? extends Throwable> expectedException, String description) {
            this(input, expectedException, description, false);
        }

        ExceptionCase(Object input, Class<? extends Throwable> expectedException, String description, boolean nullConverter) {
            this.input = input;
            this.expectedException = expectedException;
            this.description = description;
            this.nullConverter = nullConverter;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; an empty one is sufficient for the default behaviour.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /**
     * Test cases for successful conversions that should return a BigInteger.
     */
    static Stream<ArgumentsHolder> validConversionProvider() {
        return Stream.of(// Instant
        new ArgumentsHolder(Instant.parse("2023-01-01T12:34:56Z"), BigInteger.valueOf(Instant.parse("2023-01-01T12:34:56Z").toEpochMilli()), "Instant input should be converted to its epoch‑millisecond representation."), // java.util.Date
        new ArgumentsHolder(new Date(1672570496000L), BigInteger.valueOf(1672570496000L), "Date should be transformed to an Instant and then to a BigInteger."), // java.sql.Timestamp
        new ArgumentsHolder(Timestamp.valueOf("2023-01-01 12:34:56.123"), BigInteger.valueOf(Timestamp.valueOf("2023-01-01 12:34:56.123").getTime()), "Timestamp should be treated like a Date."), // Long epoch‑millis
        new ArgumentsHolder(1672570496000L, BigInteger.valueOf(1672570496000L), "Long representing epoch‑milliseconds should be wrapped directly."), // String numeric (plain)
        new ArgumentsHolder("1672570496000", BigInteger.valueOf(1672570496000L), "String containing a numeric epoch‑millisecond value should be parsed."), // OffsetDateTime with offset
        new ArgumentsHolder(OffsetDateTime.parse("2023-01-01T12:34:56+02:00"), BigInteger.valueOf(OffsetDateTime.parse("2023-01-01T12:34:56+02:00").toInstant().toEpochMilli()), "OffsetDateTime should be converted respecting its offset."), // Boundary: Instant.EPOCH
        new ArgumentsHolder(Instant.EPOCH, BigInteger.ZERO, "Epoch instant should become BigInteger.ZERO."), // Negative epoch‑millis
        new ArgumentsHolder(Instant.ofEpochMilli(-12345L), BigInteger.valueOf(-12345L), "Negative epoch‑millisecond values must be preserved."), // Max long epoch‑millis
        new ArgumentsHolder(Instant.ofEpochMilli(Long.MAX_VALUE), BigInteger.valueOf(Long.MAX_VALUE), "Maximum long epoch‑millisecond value should be wrapped without overflow."), // String with whitespace and plus sign
        new ArgumentsHolder("   +1672570496000  ", BigInteger.valueOf(1672570496000L), "Numeric string with whitespace and explicit plus sign must be parsed."), // String with minus sign
        new ArgumentsHolder("-1672570496000", BigInteger.valueOf(-1672570496000L), "Numeric string with leading minus sign must be parsed."), // Calendar (UTC)
        new ArgumentsHolder(createUtcCalendar(1672570496000L), BigInteger.valueOf(1672570496000L), "UTC Calendar should be converted to its epoch‑millisecond value."), // ZonedDateTime
        new ArgumentsHolder(ZonedDateTime.parse("2023-01-01T12:34:56+02:00[Europe/Paris]"), BigInteger.valueOf(ZonedDateTime.parse("2023-01-01T12:34:56+02:00[Europe/Paris]").toInstant().toEpochMilli()), "ZonedDateTime must be converted respecting zone offset."), // Already a BigInteger
        new ArgumentsHolder(BigInteger.valueOf(123456789L), BigInteger.valueOf(123456789L), "If source is already a BigInteger, it should be returned unchanged."), // AtomicLong
        new ArgumentsHolder(new AtomicLong(987654321L), BigInteger.valueOf(987654321L), "AtomicLong should be read and converted to a BigInteger."));
    }

    /**
     * Test cases that are expected to throw a specific exception.
     */
    static Stream<ExceptionCase> exceptionProvider() {
        return Stream.of(// Unsupported type Boolean
        new ExceptionCase(Boolean.TRUE, ConversionException.class, "Unsupported type should cause ConversionException."), // LocalDateTime (no zone/offset)
        new ExceptionCase(LocalDateTime.of(2023, 1, 1, 12, 34, 56), ConversionException.class, "LocalDateTime lacks zone information and must raise ConversionException."), // OffsetTime (no date)
        new ExceptionCase(OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2)), ConversionException.class, "OffsetTime cannot be turned into an Instant, expect ConversionException."), // Non‑numeric string
        new ExceptionCase("not-a-number", NumberFormatException.class, "Non‑numeric string should cause NumberFormatException."), // Decimal string
        new ExceptionCase("12345.67", NumberFormatException.class, "Decimal string is not a valid epoch‑millisecond integer."), // Null converter
        new ExceptionCase(Instant.now(), NullPointerException.class, "Passing a null Converter should cause NullPointerException.", true));
    }

    /**
     * Creates a UTC Calendar set to the specified epoch‑millis.
     */
    private static Calendar createUtcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /**
     * Verify that an {@link OffsetDateTime} is correctly converted to a {@link BigInteger}
     * representing the same instant (epoch seconds) as the underlying {@link java.time.Instant}.
     */
    @Test
    void toBigInteger_convertsOffsetDateTimeToBigInteger() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 17, 12, 34, 56, 123_000_000, ZoneOffset.UTC);
        // The expected value is the epoch‑second of the instant wrapped in a BigInteger.
        BigInteger expected = BigInteger.valueOf(odt.toInstant().getEpochSecond());
        // WHEN
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(odt, converter);
        // THEN
        assertEquals(expected, actual, "The BigInteger conversion of OffsetDateTime is incorrect");
    }

    /**
     * Verify that {@code null} input returns {@code null} (the Converter follows this contract for
     * reference types).
     */
    @Test
    void toBigInteger_nullInputReturnsNull() {
        // GIVEN
        Object from = null;
        // WHEN
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(from, converter);
        // THEN
        assertNull(actual, "Converting null should yield null");
    }

    /**
     * Verify that a supported non‑OffsetDateTime type (e.g., {@link java.util.Date})
     * is still handled because the method delegates to {@code toInstant}.
     */
    @Test
    void toBigInteger_convertsDateToBigInteger() {
        // GIVEN
        java.util.Date date = java.util.Date.from(OffsetDateTime.now().toInstant());
        BigInteger expected = BigInteger.valueOf(date.toInstant().getEpochSecond());
        // WHEN
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(date, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toBigInteger_validConversions(ArgumentsHolder holder) {
        // WHEN
        BigInteger result = OffsetDateTimeConversions.toBigInteger(holder.input, converter);
        // THEN
        assertEquals(holder.expected, result, holder.description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionProvider")
    void toBigInteger_expectedExceptions(ExceptionCase ec) {
        // Arrange: possibly null converter
        Converter usedConverter = ec.nullConverter ? null : converter;
        // WHEN / THEN
        assertThrows(ec.expectedException, () -> OffsetDateTimeConversions.toBigInteger(ec.input, usedConverter), ec.description);
    }
}
