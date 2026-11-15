package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toSqlDate_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Force UTC for deterministic zone‑dependent conversions.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // The Converter has a public constructor that requires a ConverterOptions instance.
        // Using the default options is sufficient for the built‑in conversions we test.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// java.util.Date representing the epoch (0 ms)
        Arguments.of(new Date(0L), 0L), // java.sql.Timestamp with nanoseconds – nanoseconds are truncated to ms
        Arguments.of(Timestamp.valueOf("2023-01-01 12:34:56.123456789"), 1672578896123L), // Instant with nanosecond part
        Arguments.of(Instant.ofEpochSecond(1609459200L, 500_000_000L), 1609459200500L), // Calendar (UTC) set to a specific instant
        Arguments.of(createUtcCalendar(1672578896000L), 1672578896000L), // LocalDate – start of day in UTC
        Arguments.of(LocalDate.of(2023, 1, 1), 1672531200000L), // LocalDateTime – interpreted in UTC
        Arguments.of(LocalDateTime.of(2023, 3, 15, 10, 20, 30), 1678879230000L), // OffsetDateTime with +02:00 offset – converted to UTC
        Arguments.of(OffsetDateTime.parse("2023-03-15T10:20:30+02:00"), 1678872030000L), // Long epoch milliseconds
        Arguments.of(Long.valueOf(1620000000000L), 1620000000000L), // ISO‑8601 UTC string
        Arguments.of("2023-03-15T10:20:30Z", 1678879230000L));
    }

    private static Stream<Arguments> provideExceptionConversions() {
        return Stream.of(// Null source object
        Arguments.of(null, NullPointerException.class), // Instant.MAX – out of range for java.sql.Date
        Arguments.of(Instant.MAX, DateTimeException.class), // Unsupported source type
        Arguments.of(new Object(), IllegalArgumentException.class), // Unparseable string
        Arguments.of("not-a-date-string", java.time.format.DateTimeParseException.class), // LocalDate far beyond the epoch range
        Arguments.of(LocalDate.of(292278994, 8, 17), DateTimeException.class), // BigDecimal not supported
        Arguments.of(BigDecimal.valueOf(Long.MAX_VALUE), IllegalArgumentException.class));
    }

    /**
     * Helper that creates a {@link Calendar} instance in UTC with the given epoch millis.
     */
    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Verify that a plain {@link Instant} is converted to a {@link Date}
     * with the same epoch millisecond value.
     */
    @Test
    void toSqlDate_fromInstant_returnsEquivalentDate() {
        // GIVEN
        // arbitrary epoch millis
        Instant instant = Instant.ofEpochMilli(1_642_000_123_456L);
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(instant, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(instant.toEpochMilli(), result.getTime(), "Date should represent the same instant as the source Instant");
    }

    /**
     * Verify that a {@link Long} representing epoch milliseconds is converted correctly.
     */
    @Test
    void toSqlDate_fromLong_returnsEquivalentDate() {
        // GIVEN
        long epochMillis = 1_600_000_000_000L;
        Long source = Long.valueOf(epochMillis);
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMillis, result.getTime(), "Date should represent the epoch milliseconds supplied as Long");
    }

    /**
     * Verify that a {@link java.sql.Timestamp} is converted to a {@link Date}
     * preserving the same millisecond instant.
     */
    @Test
    void toSqlDate_fromTimestamp_returnsEquivalentDate() {
        // GIVEN
        java.sql.Timestamp ts = java.sql.Timestamp.valueOf("2023-04-01 12:34:56.789");
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(ts, converter);
        // THEN
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime(), "Date should have the same epoch millis as the source Timestamp");
    }

    /**
     * Verify that a {@link java.util.Date} passed through the method returns a new
     * Date instance with the same instant (i.e., the method does not simply return
     * the original reference).
     */
    @Test
    void toSqlDate_fromDate_returnsNewEquivalentDate() {
        // GIVEN
        Date original = new Date(1_500_000_000_000L);
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(original, converter);
        // THEN
        assertNotNull(result);
        assertEquals(original.getTime(), result.getTime(), "Resulting Date should represent the same instant as the original");
        assertNotSame(original, result, "Result should be a new Date instance, not the original reference");
    }

    /**
     * Verify that a {@link java.time.LocalDateTime} (interpreted as the system default
     * time‑zone) is converted correctly. The conversion path goes through
     * {@code toInstant(...)} which uses the default zone offset.
     */
    @Test
    void toSqlDate_fromLocalDateTime_returnsEquivalentDate() {
        // GIVEN
        java.time.LocalDateTime ldt = java.time.LocalDateTime.of(2022, 12, 31, 23, 59, 59, 123_000_000);
        // WHEN
        Date result = BigDecimalConversions.toSqlDate(ldt, converter);
        // THEN
        assertNotNull(result);
        // Compute the expected epoch millis using the same conversion logic that the
        // library employs (i.e., using the system default zone, forced to UTC above).
        Instant expected = ldt.atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(expected.toEpochMilli(), result.getTime(), "Date should represent the same instant as the source LocalDateTime");
    }

    /**
     * Successful conversions from a variety of source types.
     *
     * The {@code converter} argument is deliberately passed as {@code null}
     * because the library’s conversion utilities accept a null converter for
     * built‑in types.
     */
    @ParameterizedTest(name = "[{index}] toSqlDate({0}) ⇒ epoch {1}")
    @MethodSource("provideSuccessfulConversions")
    void toSqlDate_successfulConversions(Object from, long expectedEpochMillis) {
        Date result = BigDecimalConversions.toSqlDate(from, null);
        assertNotNull(result, "Result should never be null for a successful conversion");
        assertEquals(expectedEpochMillis, result.getTime(), () -> "Converted epoch millis differ for source: " + from);
    }

    /**
     * Conversions that are expected to throw an exception.
     *
     * The test asserts that the exact exception type defined in the JSON spec
     * is thrown when the source value is invalid or unsupported.
     */
    @ParameterizedTest(name = "[{index}] toSqlDate({0}) throws {1}")
    @MethodSource("provideExceptionConversions")
    void toSqlDate_expectedExceptions(Object from, Class<? extends Throwable> exceptionType) {
        assertThrows(exceptionType, () -> BigDecimalConversions.toSqlDate(from, null), () -> "Expected " + exceptionType.getSimpleName() + " for source: " + from);
    }
}
