package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toSqlDate_Test {

    private static Converter converter;

    private static ZoneId originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a real Converter – the conversion logic is exercised, not the mock.
        converter = new Converter(new ConverterOptions());
        // Force the JVM default zone to UTC for deterministic tests that rely on the system zone.
        originalDefaultZone = ZoneId.systemDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore original default zone.
        TimeZone.setDefault(TimeZone.getTimeZone(originalDefaultZone));
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Direct long value
        Arguments.of("Long epoch millis", 1622548800000L, 1622548800000L), // Integer value
        Arguments.of("Integer epoch millis", 123456789, 123456789L), // Numeric string
        Arguments.of("String numeric epoch", "1622548800000", 1622548800000L), // java.util.Date
        Arguments.of("java.util.Date instance", new Date(1622548800000L), 1622548800000L), // java.sql.Timestamp
        Arguments.of("java.sql.Timestamp instance", Timestamp.valueOf("2021-06-01 12:34:56"), 1622552096000L), // Instant
        Arguments.of("Instant instance", Instant.ofEpochMilli(1622548800000L), 1622548800000L), // LocalDate (midnight UTC because default zone forced to UTC)
        Arguments.of("LocalDate instance", LocalDate.of(2021, 6, 1), 1622505600000L), // LocalDateTime (treated as system default zone – UTC)
        Arguments.of("LocalDateTime instance", LocalDateTime.of(2021, 6, 1, 12, 34, 56), 1622552096000L), // ZonedDateTime UTC
        Arguments.of("ZonedDateTime UTC", ZonedDateTime.of(2021, 6, 1, 12, 34, 56, 0, ZoneId.of("UTC")), 1622590496000L), // OffsetDateTime UTC
        Arguments.of("OffsetDateTime UTC", OffsetDateTime.of(2021, 6, 1, 12, 34, 56, 0, ZoneOffset.UTC), 1622590496000L), // Calendar UTC
        Arguments.of("Calendar UTC", createCalendar(1622548800000L, "UTC"), 1622548800000L), // BigDecimal
        Arguments.of("BigDecimal epoch", new BigDecimal("1622548800000"), 1622548800000L), // BigInteger
        Arguments.of("BigInteger epoch", new BigInteger("1622548800000"), 1622548800000L), // AtomicLong
        Arguments.of("AtomicLong epoch", new AtomicLong(1622548800000L), 1622548800000L), // Map with DATE, TIME, ZONE
        Arguments.of("Map with date, time, zone", Map.of(DATE, LocalDate.of(2021, 6, 1), TIME, LocalTime.of(12, 34, 56), ZONE, ZoneId.of("UTC")), 1622590496000L));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Null source – according to current implementation it returns null, not NPE.
        // This case is omitted to avoid conflict with the existing null‑test.
        // Non‑numeric string
        Arguments.of("Non‑numeric string", "not-a-number", NumberFormatException.class), // Unsupported Boolean
        Arguments.of("Boolean source", Boolean.TRUE, IllegalArgumentException.class), // BigDecimal with fractional part
        Arguments.of("BigDecimal with fraction", new BigDecimal("1622548800.123"), IllegalArgumentException.class), // Map missing DATE entry
        Arguments.of("Map missing DATE", Map.of(TIME, LocalTime.of(12, 34, 56), ZONE, ZoneId.of("UTC")), IllegalArgumentException.class));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// LocalTime interpreted as epoch date (1970‑01‑01)
        Arguments.of("LocalTime only", LocalTime.of(12, 34, 56), 45296000L), // OffsetTime with +02:00 offset (converted to UTC)
        Arguments.of("OffsetTime +02:00", OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2)), 38096000L), // ZonedDateTime with America/New_York (UTC‑4 in June)
        Arguments.of("ZonedDateTime America/New_York", ZonedDateTime.of(2021, 6, 1, 12, 34, 56, 0, ZoneId.of("America/New_York")), 1622604896000L), // OffsetDateTime with -05:00 offset
        Arguments.of("OffsetDateTime -05:00", OffsetDateTime.of(2021, 6, 1, 12, 34, 56, 0, ZoneOffset.ofHours(-5)), 1622608496000L), // Map with only DATE (treated as start‑of‑day UTC)
        Arguments.of("Map with only DATE", Map.of(DATE, LocalDate.of(2021, 6, 1)), 1622505600000L), // Map with DATE and explicit ZONE (no TIME)
        Arguments.of("Map with DATE and ZONE", Map.of(DATE, LocalDate.of(2021, 6, 1), ZONE, ZoneId.of("UTC")), 1622505600000L), // java.sql.Date directly
        Arguments.of("java.sql.Date instance", java.sql.Date.valueOf("2021-06-01"), 1622505600000L), // Negative epoch (Instant before 1970)
        Arguments.of("Instant before epoch", Instant.ofEpochMilli(-86400000L), -86400000L));
    }

    private static Stream<Arguments> additionalInvalidProvider() {
        return Stream.of(// Empty string
        Arguments.of("Empty string", "", NumberFormatException.class), // Decimal string
        Arguments.of("Decimal string", "12345.67", NumberFormatException.class), // BigInteger overflow
        Arguments.of("BigInteger overflow", new BigInteger("9223372036854775808"), IllegalArgumentException.class), // Double.NaN
        Arguments.of("Double.NaN", Double.NaN, IllegalArgumentException.class), // Float.POSITIVE_INFINITY
        Arguments.of("Float.POSITIVE_INFINITY", Float.POSITIVE_INFINITY, IllegalArgumentException.class), // UUID (unsupported type)
        Arguments.of("UUID unsupported", UUID.randomUUID(), IllegalArgumentException.class), // Map with DATE as String
        Arguments.of("Map DATE as String", Map.of(DATE, "2021-06-01"), IllegalArgumentException.class), // Map with TIME as String
        Arguments.of("Map TIME as String", Map.of(DATE, LocalDate.of(2021, 6, 1), TIME, "12:34:56"), IllegalArgumentException.class), // Empty map
        Arguments.of("Empty map", Collections.emptyMap(), IllegalArgumentException.class));
    }

    /**
     * Creates a {@link Calendar} set to the given epoch millis and time‑zone.
     */
    private static Calendar createCalendar(long epochMilli, String zoneId) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /**
     * Verify that a {@code null} source returns {@code null} when converted to a SQL {@link java.sql.Date}.
     */
    @Test
    void toSqlDate_NullInput_ReturnsNull() {
        Date result = ZonedDateTimeConversions.toSqlDate(null, converter);
        assertNull(result, "Converting null should produce null");
    }

    /**
     * Verify conversion from a {@link ZonedDateTime} instance.
     */
    @Test
    void toSqlDate_FromZonedDateTime_ReturnsCorrectDate() {
        // Use a fixed instant for reproducibility.
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 15, 10, 30, 45, 123_000_000, ZoneId.of("UTC"));
        Date expected = new Date(zdt.toInstant().toEpochMilli());
        Date actual = ZonedDateTimeConversions.toSqlDate(zdt, converter);
        assertEquals(expected, actual, "SQL Date should match the epoch milliseconds of the ZonedDateTime");
    }

    /**
     * Verify conversion from a map that mimics the structure used by {@link MapConversions}
     * for date/time values (keys {@code "time"} and {@code "zone"}).
     */
    @Test
    void toSqlDate_FromMap_ReturnsCorrectDate() {
        // Build a map that represents a ZonedDateTime.
        long epochMilli = Instant.parse("2022-12-01T08:15:30.500Z").toEpochMilli();
        Map<String, Object> map = new HashMap<>();
        // TIME key holds the epoch millis
        map.put(MapConversions.TIME, epochMilli);
        // ZONE key holds the zone id
        map.put(MapConversions.ZONE, "America/New_York");
        // Expected date is based on the epoch millis (zone does not affect the instant value)
        Date expected = new Date(epochMilli);
        Date actual = ZonedDateTimeConversions.toSqlDate(map, converter);
        assertEquals(expected, actual, "SQL Date from map should be based on the provided epoch milliseconds");
    }

    /**
     * Verify conversion from a {@link java.sql.Timestamp}.
     */
    @Test
    void toSqlDate_FromTimestamp_ReturnsCorrectDate() {
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2021, 3, 10, 14, 22, 33, 777_000_000));
        Date expected = new Date(ts.getTime());
        Date actual = ZonedDateTimeConversions.toSqlDate(ts, converter);
        assertEquals(expected, actual, "SQL Date should be derived from the Timestamp's milliseconds");
    }

    /**
     * Verify conversion from a {@link java.util.Date} (including java.sql.Date subclass).
     */
    @Test
    void toSqlDate_FromDate_ReturnsSameInstanceValue() {
        java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2020, 1, 31));
        Date expected = new Date(sqlDate.getTime());
        Date actual = ZonedDateTimeConversions.toSqlDate(sqlDate, converter);
        assertEquals(expected, actual, "SQL Date conversion should preserve the underlying epoch milliseconds");
    }

    // ---------- Valid Conversions -------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void toSqlDate_ValidConversions(String description, Object input, long expectedEpochMillis) {
        Date result = ZonedDateTimeConversions.toSqlDate(input, converter);
        assertNotNull(result, description);
        assertEquals(expectedEpochMillis, result.getTime(), () -> description + " – expected epoch millis " + expectedEpochMillis);
    }

    // ---------- Invalid Conversions -------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidConversionProvider")
    void toSqlDate_InvalidConversions(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toSqlDate(input, converter), description);
    }

    // ---------- Edge‑Case Conversions ------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeCaseProvider")
    void toSqlDate_EdgeCases(String description, Object input, long expectedEpochMillis) {
        Date result = ZonedDateTimeConversions.toSqlDate(input, converter);
        assertNotNull(result, description);
        assertEquals(expectedEpochMillis, result.getTime(), () -> description + " – expected epoch millis " + expectedEpochMillis);
    }

    // ---------- Additional Invalid Conversions --------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalInvalidProvider")
    void toSqlDate_AdditionalInvalid(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toSqlDate(input, converter), description);
    }
}
