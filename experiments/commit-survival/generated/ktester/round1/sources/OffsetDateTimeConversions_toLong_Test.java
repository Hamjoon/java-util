package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toLong_Test {

    /**
     * Mocked Converter that will be used to stub conversion to {@link Instant}.
     */
    @Mock
    Converter converter;

    /**
     * A deterministic Instant used across the test cases.
     */
    static Instant FIXED_INSTANT;

    /**
     * Mocked Converter – used only for the custom‑converter test case.
     */
    @Mock
    Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a fixed point in time so the expected epoch‑millis are stable.
        FIXED_INSTANT = Instant.parse("2023-01-01T12:34:56.789Z");
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions to keep each test isolated.
        clearInvocations(converter);
    }

    /**
     * Supplies all successful conversion scenarios.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// OffsetDateTime with positive offset (+02:00)
        Arguments.of(OffsetDateTime.parse("2023-01-01T12:34:56+02:00"), null, "OffsetDateTime with positive offset", 1672563296000L), // Epoch start instant
        Arguments.of(OffsetDateTime.parse("1970-01-01T00:00:00Z"), null, "Epoch start instant", 0L), // Instant directly
        Arguments.of(Instant.ofEpochMilli(123456789L), null, "Instant input", 123456789L), // java.util.Date
        Arguments.of(new Date(1609459200000L), null, "java.util.Date representing 2021‑01‑01T00:00:00Z", 1609459200000L), // java.sql.Timestamp
        Arguments.of(new Timestamp(1622505600123L), null, "java.sql.Timestamp", 1622505600123L), // Long value (already a long)
        Arguments.of(12345L, null, "Long input", 12345L), // BigDecimal within long range
        Arguments.of(new BigDecimal("9876543210"), null, "BigDecimal within long range", 9876543210L), // ISO‑8601 string
        Arguments.of("2022-12-31T23:59:59+00:00", null, "String ISO‑8601 offset date‑time", 1672531199000L), // OffsetDateTime with negative offset (-05:00)
        Arguments.of(OffsetDateTime.parse("2023-01-01T12:34:56-05:00"), null, "OffsetDateTime with negative offset", 1672594496000L), // GregorianCalendar in UTC
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("UTC")) {

            {
                setTimeInMillis(1655280000000L);
            }
        }, null, "GregorianCalendar UTC", 1655280000000L), // GregorianCalendar in America/New_York representing same instant
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("America/New_York")) {

            {
                set(2022, Calendar.JUNE, 15, 4, 0, 0);
                set(Calendar.MILLISECOND, 0);
            }
        }, null, "GregorianCalendar America/New_York", 1655280000000L), // AtomicLong
        Arguments.of(new AtomicLong(9876543210L), null, "AtomicLong", 9876543210L), // BigInteger fitting in long
        Arguments.of(new BigInteger("123456789012345"), null, "BigInteger within long range", 123456789012345L), // Numeric string
        Arguments.of("1610000000000", null, "String containing numeric epoch millis", 1610000000000L), // ZonedDateTime with zone Europe/Paris
        Arguments.of(ZonedDateTime.parse("2023-03-10T15:20:30+01:00[Europe/Paris]"), null, "ZonedDateTime Europe/Paris", 1678458030000L), // java.sql.Date (subclass of java.util.Date)
        Arguments.of(new java.sql.Date(1655280000000L), null, "java.sql.Date", 1655280000000L), // Double wrapper (exact integer value)
        Arguments.of(Double.valueOf(12345.0), null, "Double wrapper with integer value", 12345L), // String with surrounding whitespace
        Arguments.of("   1622505600123   ", null, "String with surrounding whitespace", 1622505600123L), // Custom converter case – source is ignored, converter returns fixed instant
        Arguments.of("ignored", new Converter() {

            @SuppressWarnings("unchecked")
            @Override
            public <T> T convert(Object src, Class<T> target) {
                return (T) Instant.ofEpochMilli(5555555L);
            }
        }, "Custom converter returning fixed instant", 5555555L));
    }

    /**
     * Supplies all scenarios that must result in an exception.
     */
    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// Null input – should NPE from toInstant
        Arguments.of(null, null, "Null source", NullPointerException.class), // Unsupported type – plain Object
        Arguments.of(new Object(), null, "Unsupported source type", IllegalArgumentException.class), // Unparseable date‑time string
        Arguments.of("invalid-date-string", null, "Invalid date‑time string", DateTimeParseException.class), // Numeric overflow via BigDecimal (1E30)
        Arguments.of(new BigDecimal("1E30"), null, "BigDecimal overflow", ArithmeticException.class), // BigInteger exceeding Long.MAX_VALUE
        Arguments.of(new BigInteger("9223372036854775808"), null, "BigInteger overflow", ArithmeticException.class), // String below Long.MIN_VALUE
        Arguments.of("-9223372036854775809", null, "String numeric underflow", NumberFormatException.class), // Double.NaN
        Arguments.of(Double.NaN, null, "Double NaN", IllegalArgumentException.class), // Double.POSITIVE_INFINITY
        Arguments.of(Double.POSITIVE_INFINITY, null, "Double positive infinity", IllegalArgumentException.class), // AtomicLong with value exceeding epoch‑millis range (Long.MAX_VALUE)
        Arguments.of(new AtomicLong(Long.MAX_VALUE), null, "AtomicLong overflow", ArithmeticException.class), // Calendar with millis beyond Instant.MAX
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("UTC")) {

            {
                setTimeInMillis(Long.MAX_VALUE);
            }
        }, null, "Calendar overflow beyond Instant.MAX", DateTimeException.class), // ZonedDateTime far beyond supported range
        Arguments.of(ZonedDateTime.parse("999999999-12-31T23:59:59+00:00[UTC]"), null, "ZonedDateTime far future", DateTimeException.class), // Timestamp beyond Instant range
        Arguments.of(new Timestamp(Long.MAX_VALUE), null, "Timestamp overflow", DateTimeException.class), // Empty string
        Arguments.of("", null, "Empty string", DateTimeParseException.class), // Hexadecimal string (unsupported)
        Arguments.of("0x1F4", null, "Hexadecimal string", NumberFormatException.class));
    }

    /**
     * Verify that {@code toLong} returns the epoch‑millis when the source object
     * is already an {@link Instant}.  In this scenario the {@link Converter}
     * is not consulted, so a {@code null} mock is acceptable.
     */
    @Test
    void toLong_givenInstant_returnsEpochMilli() {
        // Act
        long result = OffsetDateTimeConversions.toLong(FIXED_INSTANT, null);
        // Assert
        assertEquals(FIXED_INSTANT.toEpochMilli(), result, "toLong should return the epoch‑millis of the supplied Instant");
    }

    /**
     * Verify that {@code toLong} correctly converts an {@link OffsetDateTime}
     * to epoch‑millis by delegating to the {@link Converter}.
     */
    @Test
    void toLong_givenOffsetDateTime_delegatesToConverterAndReturnsEpochMilli() {
        // Arrange
        OffsetDateTime odt = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        // Stub the converter so that a conversion request for Instant returns our fixed instant.
        when(converter.convert(eq(odt), eq(Instant.class))).thenReturn(FIXED_INSTANT);
        // Act
        long result = OffsetDateTimeConversions.toLong(odt, converter);
        // Assert
        assertEquals(FIXED_INSTANT.toEpochMilli(), result, "toLong should delegate to the converter and return the epoch‑millis of the resulting Instant");
        // Verify that the converter was invoked exactly once with the expected arguments.
        verify(converter, times(1)).convert(eq(odt), eq(Instant.class));
    }

    /**
     * Verify that {@code toLong} works with a {@link java.util.Date} instance.
     * The conversion path also goes through the {@link Converter}.
     */
    @Test
    void toLong_givenDate_delegatesToConverterAndReturnsEpochMilli() {
        // Arrange
        java.util.Date date = java.util.Date.from(FIXED_INSTANT);
        when(converter.convert(eq(date), eq(Instant.class))).thenReturn(FIXED_INSTANT);
        // Act
        long result = OffsetDateTimeConversions.toLong(date, converter);
        // Assert
        assertEquals(FIXED_INSTANT.toEpochMilli(), result, "toLong should convert a Date to epoch‑millis via the converter");
        verify(converter, times(1)).convert(eq(date), eq(Instant.class));
    }

    /* -------------------------------------------------------------
     *  Successful conversion test cases (expected long result)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulConversionProvider")
    void toLong_successful(Object from, Converter converter, String description, long expected) {
        long result = OffsetDateTimeConversions.toLong(from, converter);
        assertEquals(expected, result, () -> "Failed conversion for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Exception test cases (expected to throw)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionConversionProvider")
    void toLong_exceptions(Object from, Converter converter, String description, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toLong(from, converter), () -> "Expected exception for case: " + description);
    }
}
