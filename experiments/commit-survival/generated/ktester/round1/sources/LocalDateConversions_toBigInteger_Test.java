package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toBigInteger_Test {

    /**
     * A mock {@link Converter} – the real implementation is not required for these tests.
     */
    @Mock
    Converter mockConverter;

    /**
     * The {@link Converter} used in the tests – created from the mock to avoid null‑pointer issues.
     */
    Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a real Converter instance using the mocked Converter as a placeholder.
        // The constructor of Converter expects a ConverterOptions object; passing null is allowed
        // for the purpose of these unit tests because the options are not consulted by the
        // methods we are exercising.
        converter = new Converter(null);
        // The real Converter does not need any configuration for the tests.
        converter = new Converter(null);
    }

    /* -------------------------------------------------
       Helper for parameterized test data
       ------------------------------------------------- */
    private static Stream<ArgumentsHolder> provideTestCases() {
        return Stream.of(// Null source – should return null (no NPE because toInstant handles null)
        new ArgumentsHolder(null, null, null), // ISO‑8601 string
        new ArgumentsHolder("2023-01-01T12:34:56Z", BigInteger.valueOf(Instant.parse("2023-01-01T12:34:56Z").getEpochSecond()), null), // java.util.Date
        new ArgumentsHolder(Date.from(Instant.parse("2023-01-01T00:00:00Z")), BigInteger.valueOf(1672531200L), null), // LocalDate (start of day UTC)
        new ArgumentsHolder(LocalDate.of(2023, 1, 1), BigInteger.valueOf(1672531200L), null), // LocalDateTime (treated as UTC)
        new ArgumentsHolder(LocalDateTime.of(2023, 1, 1, 12, 0, 0), BigInteger.valueOf(1672574400L), null), // Instant (already an instant)
        new ArgumentsHolder(Instant.parse("2023-01-01T12:34:56Z"), BigInteger.valueOf(1672574096L), null), // BigInteger (already a BigInteger)
        new ArgumentsHolder(BigInteger.valueOf(123456789L), BigInteger.valueOf(123456789L), null), // BigDecimal (truncated)
        new ArgumentsHolder(BigDecimal.valueOf(12345.678), BigInteger.valueOf(12345L), null), // ZonedDateTime with offset
        new ArgumentsHolder(ZonedDateTime.parse("2023-01-01T12:34:56+02:00[Europe/Paris]"), BigInteger.valueOf(Instant.parse("2023-01-01T10:34:56Z").getEpochSecond()), null), // OffsetDateTime
        new ArgumentsHolder(OffsetDateTime.parse("2023-01-01T12:34:56-05:00"), BigInteger.valueOf(Instant.parse("2023-01-01T17:34:56Z").getEpochSecond()), null), // Calendar set to UTC start of day
        new ArgumentsHolder(createUtcCalendar(2023, 0, 1, 0, 0, 0), BigInteger.valueOf(1672531200L), null), // java.sql.Timestamp (interpreted as UTC)
        new ArgumentsHolder(Timestamp.valueOf("2023-01-01 12:34:56"), BigInteger.valueOf(Instant.parse("2023-01-01T12:34:56Z").getEpochSecond()), null), // java.sql.Date (date only, start of day UTC)
        new ArgumentsHolder(java.sql.Date.valueOf("2023-01-01"), BigInteger.valueOf(1672531200L), null), // LocalTime – should throw IllegalArgumentException
        new ArgumentsHolder(LocalTime.of(12, 34, 56), null, IllegalArgumentException.class), // Long epoch seconds
        new ArgumentsHolder(Long.valueOf(1672574096L), BigInteger.valueOf(1672574096L), null), // Integer epoch seconds
        new ArgumentsHolder(Integer.valueOf(12345), BigInteger.valueOf(12345L), null), // AtomicLong
        new ArgumentsHolder(new AtomicLong(987654321L), BigInteger.valueOf(987654321L), null), // Double (truncated)
        new ArgumentsHolder(Double.valueOf(12345.987), BigInteger.valueOf(12345L), null), // Invalid string – DateTimeParseException
        new ArgumentsHolder("not-a-date", null, DateTimeParseException.class), // Empty string – DateTimeParseException
        new ArgumentsHolder("", null, DateTimeParseException.class), // ZoneOffset alone – IllegalArgumentException
        new ArgumentsHolder(ZoneOffset.ofHours(2), null, IllegalArgumentException.class), // Null converter – NullPointerException
        new ArgumentsHolder(Instant.parse("2023-01-01T12:34:56Z"), null, NullPointerException.class, true), // Boolean – IllegalArgumentException
        new ArgumentsHolder(Boolean.TRUE, null, IllegalArgumentException.class), // Arbitrary Object – IllegalArgumentException
        new ArgumentsHolder(new Object(), null, IllegalArgumentException.class), // java.sql.Time – IllegalArgumentException
        new ArgumentsHolder(java.sql.Time.valueOf("12:34:56"), null, IllegalArgumentException.class), // Year – IllegalArgumentException
        new ArgumentsHolder(Year.of(2023), null, IllegalArgumentException.class), // LocalDate with out‑of‑range year – DateTimeException
        new ArgumentsHolder(LocalDate.of(1_000_000_000, 1, 1), null, DateTimeException.class), // Empty Map – IllegalArgumentException
        new ArgumentsHolder(Collections.emptyMap(), null, IllegalArgumentException.class), // Empty List – IllegalArgumentException
        new ArgumentsHolder(Collections.emptyList(), null, IllegalArgumentException.class));
    }

    /**
     * Helper to create a UTC Calendar at the given date‑time.
     */
    private static Calendar createUtcCalendar(int year, int monthZeroBased, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year, monthZeroBased, day, hour, minute, second);
        return cal;
    }

    // Overloaded method to handle the optional null‑converter flag
    private void toBigInteger_VariousInputs(Object from, BigInteger expected, Class<? extends Throwable> expectedEx, boolean useNullConverter) {
        Converter conv = useNullConverter ? null : converter;
        if (expectedEx != null) {
            // Expect an exception – the exact type matters
            assertThrows(expectedEx, () -> LocalDateConversions.toBigInteger(from, conv), () -> "Expected " + expectedEx.getSimpleName() + " for input: " + from);
        } else {
            // Normal conversion – result must match expectation (including null)
            BigInteger result = LocalDateConversions.toBigInteger(from, conv);
            assertEquals(expected, result, () -> "Conversion result mismatch for input: " + from);
        }
    }

    /**
     * Verify that a {@code null} source value results in a {@code null} {@link BigInteger}.
     * The internal {@code toInstant} conversion returns {@code null} for a {@code null}
     * argument, and {@link InstantConversions#toBigInteger} also returns {@code null}
     * when given a {@code null} instant.
     */
    @Test
    void toBigInteger_NullInput_ReturnsNull() {
        // given
        Object from = null;
        // when
        BigInteger result = LocalDateConversions.toBigInteger(from, converter);
        // then
        assertNull(result, "Converting a null source should yield a null BigInteger");
    }

    /**
     * Verify conversion of a {@link java.time.LocalDate} to {@link BigInteger}.
     * A {@link LocalDate} is first converted to an {@link Instant} at the start of the day
     * in the UTC zone, then to a {@link BigInteger} representing the epoch‑second count.
     */
    @Test
    void toBigInteger_LocalDate_ReturnsExpectedBigInteger() {
        // given
        // epoch start
        java.time.LocalDate localDate = java.time.LocalDate.of(1970, 1, 1);
        // The expected Instant is 1970‑01‑01T00:00:00Z, which corresponds to epoch second 0.
        BigInteger expected = BigInteger.ZERO;
        // when
        BigInteger result = LocalDateConversions.toBigInteger(localDate, converter);
        // then
        assertEquals(expected, result, "LocalDate representing the epoch start should convert to BigInteger zero");
    }

    /**
     * Verify conversion of a {@link java.util.Date} to {@link BigInteger}.
     * The {@link Date} value is interpreted as milliseconds since the epoch;
     * the resulting {@link BigInteger} should match the epoch‑second count.
     */
    @Test
    void toBigInteger_Date_ReturnsExpectedBigInteger() {
        // given
        // 1 second after epoch (1000 ms)
        java.util.Date date = new java.util.Date(1_000L);
        // 1 second => 1
        BigInteger expected = BigInteger.ONE;
        // when
        BigInteger result = LocalDateConversions.toBigInteger(date, converter);
        // then
        assertEquals(expected, result, "Date 1000 ms after epoch should convert to BigInteger value 1");
    }

    /**
     * Verify conversion of a {@link java.time.Instant} to {@link BigInteger}.
     * The instant is directly forwarded to {@link InstantConversions#toBigInteger},
     * so the result should be the epoch‑second count as a {@link BigInteger}.
     */
    @Test
    void toBigInteger_Instant_ReturnsExpectedBigInteger() {
        // given
        java.time.Instant instant = java.time.Instant.ofEpochSecond(12345L);
        BigInteger expected = BigInteger.valueOf(12345L);
        // when
        BigInteger result = LocalDateConversions.toBigInteger(instant, converter);
        // then
        assertEquals(expected, result, "Instant epoch seconds should be reflected in the resulting BigInteger");
    }

    /**
     * Parameterized test that covers the majority of conversion scenarios.
     *
     * @param from          the source object to convert
     * @param expected      the expected {@link BigInteger} result (null if an exception is expected)
     * @param expectedEx    the expected exception class (null if no exception)
     * @param useNullConv   if true, the test passes {@code null} as the converter argument
     */
    @ParameterizedTest(name = "{index}: from={0}, expected={1}, exception={2}")
    @MethodSource("provideTestCases")
    void toBigInteger_VariousInputs(Object from, BigInteger expected, Class<? extends Throwable> expectedEx) {
        toBigInteger_VariousInputs(from, expected, expectedEx, false);
    }

    @Test
    void toBigInteger_NullSource_ReturnsNull() {
        assertNull(LocalDateConversions.toBigInteger(null, converter), "Null source should produce null result");
    }

    @Test
    void toBigInteger_NullConverter_ThrowsNPE() {
        // Using a valid source but null converter should raise NPE
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toBigInteger(Instant.now(), null), "Null converter must cause NullPointerException");
    }
}
