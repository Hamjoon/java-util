package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toLocalDateTime_Test {

    /**
     * Holds the original default time‑zone so we can restore it after the suite runs.
     */
    private static TimeZone originalDefaultTimeZone;

    /**
     * Converter instance used by the tests.
     */
    private Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original default zone and force a known zone (UTC) for deterministic results.
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a public no‑arg constructor; use it to create a plain Converter.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stale state leaks between tests.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone to avoid side‑effects on other tests.
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// ZonedDateTime input
        Arguments.of("ZonedDateTime (UTC) should yield identical LocalDateTime", ZonedDateTime.of(2023, 5, 15, 13, 45, 30, 123_000_000, ZoneId.of("UTC")), LocalDateTime.of(2023, 5, 15, 13, 45, 30, 123_000_000)), // OffsetDateTime input
        Arguments.of("OffsetDateTime should ignore offset and keep wall‑clock time", OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5)), LocalDateTime.of(2022, 12, 31, 23, 59, 59)), // LocalDateTime input
        Arguments.of("Plain LocalDateTime should be returned unchanged", LocalDateTime.of(2021, 1, 1, 0, 0, 0), LocalDateTime.of(2021, 1, 1, 0, 0, 0)), // java.util.Date input
        Arguments.of("Date (instant) should be interpreted in UTC (default zone)", Date.from(Instant.parse("2020-06-30T12:34:56.789Z")), LocalDateTime.of(2020, 6, 30, 12, 34, 56, 789_000_000)), // java.sql.Timestamp input
        Arguments.of("Timestamp with nanosecond precision should map directly", Timestamp.valueOf("2019-11-11 11:11:11.111111111"), LocalDateTime.of(2019, 11, 11, 11, 11, 11, 111_111_111)), // java.util.Calendar input (America/New_York)
        Arguments.of("Calendar with specific zone should keep its field values", calendarWith(ZoneId.of("America/New_York"), 2022, Calendar.MARCH, 14, 9, 30, 0, 250), LocalDateTime.of(2022, 3, 14, 9, 30, 0, 250_000_000)), // Instant input
        Arguments.of("Instant should be converted using the default zone (UTC)", Instant.parse("2023-08-01T07:45:30.123456789Z"), LocalDateTime.ofInstant(Instant.parse("2023-08-01T07:45:30.123456789Z"), ZoneId.systemDefault())), // ISO‑8601 String input
        Arguments.of("ISO‑8601 string without offset should be parsed correctly", "2024-02-29T23:59:59.999", LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999_000_000)), // Number (epoch seconds) input
        Arguments.of("Long epoch seconds should be interpreted in the default zone", // 2022‑01‑01T00:00:00Z
        Long.valueOf(1_640_995_200L), LocalDateTime.ofInstant(Instant.ofEpochSecond(1_640_995_200L), ZoneId.systemDefault())), // LocalDate input
        Arguments.of("LocalDate should become midnight of that date", LocalDate.of(2022, 12, 31), LocalDateTime.of(2022, 12, 31, 0, 0)), // LocalTime input
        Arguments.of("LocalTime should be combined with today's date (UTC)", LocalTime.of(13, 45, 30, 250_000_000), LocalDateTime.of(LocalDate.now(ZoneId.systemDefault()), LocalTime.of(13, 45, 30, 250_000_000))), // java.sql.Date input
        Arguments.of("java.sql.Date should become midnight of that date", java.sql.Date.valueOf("2021-07-04"), LocalDateTime.of(2021, 7, 4, 0, 0)), // String with offset
        Arguments.of("ISO‑8601 string with offset should ignore offset for LocalDateTime", "2023-03-10T15:30:00+02:00", LocalDateTime.of(2023, 3, 10, 15, 30)), // Double epoch seconds with fractional part
        Arguments.of("Double with fractional seconds should be split into seconds and nanos", Double.valueOf(1_640_995_200.75), LocalDateTime.ofInstant(Instant.ofEpochSecond(1_640_995_200L, 750_000_000L), ZoneId.systemDefault())), // Calendar with non‑default TimeZone (Asia/Tokyo)
        Arguments.of("Calendar set to Asia/Tokyo should keep its field values", calendarWith(ZoneId.of("Asia/Tokyo"), 2022, Calendar.DECEMBER, 25, 23, 0, 0, 500), LocalDateTime.of(2022, 12, 25, 23, 0, 0, 500_000_000)));
    }

    // Helper to build a Calendar with given fields (months are zero‑based in Calendar)
    private static Calendar calendarWith(ZoneId zone, int year, int monthConstant, int day, int hour, int minute, int second, int milli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zone));
        cal.setLenient(false);
        cal.set(year, monthConstant, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, milli);
        return cal;
    }

    private static Stream<Arguments> invalidConversionsProvider() {
        return Stream.of(// Null source (the JSON expects IllegalArgumentException – keep both behaviours)
        Arguments.of("Null source should trigger IllegalArgumentException", null, IllegalArgumentException.class), // Unsupported type (plain Object)
        Arguments.of("Unsupported object type should cause IllegalArgumentException", new Object(), IllegalArgumentException.class), // Malformed string
        Arguments.of("Malformed date‑time string should raise DateTimeParseException", "not-a-date-time", DateTimeParseException.class), // Empty string
        Arguments.of("Empty string should raise DateTimeParseException", "", DateTimeParseException.class), // Unsupported Number type: BigDecimal
        Arguments.of("BigDecimal is not a supported numeric type", new BigDecimal("1625097600"), IllegalArgumentException.class), // Unsupported Number type: BigInteger
        Arguments.of("BigInteger is not a supported numeric type", new BigInteger("1625097600"), IllegalArgumentException.class), // Number with NaN
        Arguments.of("Double.NaN should cause IllegalArgumentException", Double.NaN, IllegalArgumentException.class), // Number with Infinity
        Arguments.of("Double.POSITIVE_INFINITY should cause IllegalArgumentException", Double.POSITIVE_INFINITY, IllegalArgumentException.class), // String with invalid offset
        Arguments.of("Offset outside +/-18:00 should raise DateTimeParseException", "2023-04-01T12:00:00+25:00", DateTimeParseException.class), // String with date only
        Arguments.of("Date‑only string should raise DateTimeParseException", "2023-01-01", DateTimeParseException.class), // Epoch seconds near Long.MAX_VALUE (out of Instant range)
        Arguments.of("Epoch seconds equal to Long.MAX_VALUE exceed Instant range", Long.valueOf(Long.MAX_VALUE), DateTimeException.class), // Instant.MAX (out of range for LocalDateTime)
        Arguments.of("Instant.MAX should cause DateTimeException when converted", Instant.MAX, DateTimeException.class));
    }

    @Test
    void toLocalDateTime_fromZonedDateTime_returnsSameLocalDateTime() {
        // GIVEN
        ZonedDateTime zoned = ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 123_456_789, ZoneId.of("America/New_York"));
        // WHEN
        LocalDateTime result = BigDecimalConversions.toLocalDateTime(zoned, converter);
        // THEN
        LocalDateTime expected = zoned.toLocalDateTime();
        assertEquals(expected, result, "The LocalDateTime should match the one extracted from the ZonedDateTime");
    }

    @Test
    void toLocalDateTime_fromDate_returnsCorrectLocalDateTimeInUtc() {
        // GIVEN
        // Create a Date representing an exact instant (2023‑01‑01T12:00:00Z)
        Instant instant = Instant.parse("2023-01-01T12:00:00Z");
        Date date = Date.from(instant);
        // WHEN
        LocalDateTime result = BigDecimalConversions.toLocalDateTime(date, converter);
        // THEN
        // Because the default zone is forced to UTC in @BeforeAll, the expected LocalDateTime is the UTC representation.
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        assertEquals(expected, result, "The LocalDateTime derived from Date should be in UTC");
    }

    @Test
    void toLocalDateTime_fromTimestamp_returnsCorrectLocalDateTimeInUtc() {
        // GIVEN
        // Timestamp holds nanosecond precision; use the same instant as above.
        Timestamp timestamp = Timestamp.from(Instant.parse("2023-06-15T08:15:30.500Z"));
        // WHEN
        LocalDateTime result = BigDecimalConversions.toLocalDateTime(timestamp, converter);
        // THEN
        // Timestamp's method respects the JVM default zone (UTC here)
        LocalDateTime expected = timestamp.toLocalDateTime();
        assertEquals(expected, result, "Timestamp should be converted to the same LocalDateTime");
    }

    @Test
    void toLocalDateTime_nullInput_returnsNull() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        LocalDateTime result = BigDecimalConversions.toLocalDateTime(nullObj, converter);
        // THEN
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void toLocalDateTime_fromStringIso8601_returnsParsedLocalDateTime() {
        // GIVEN
        // The Converter can parse ISO‑8601 strings to a ZonedDateTime, then to LocalDateTime.
        String isoString = "2022-12-31T23:59:59+02:00";
        // WHEN
        LocalDateTime result = BigDecimalConversions.toLocalDateTime(isoString, converter);
        // THEN
        // The string includes an offset of +02:00; the resulting LocalDateTime should be the same wall‑clock time.
        LocalDateTime expected = LocalDateTime.of(2022, 12, 31, 23, 59, 59);
        assertEquals(expected, result, "ISO‑8601 string should be parsed to the correct LocalDateTime");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionsProvider")
    void validConversions(String description, Object from, LocalDateTime expected) {
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(from, null);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionsProvider")
    void invalidConversions(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> BigDecimalConversions.toLocalDateTime(from, null), description);
    }
}
