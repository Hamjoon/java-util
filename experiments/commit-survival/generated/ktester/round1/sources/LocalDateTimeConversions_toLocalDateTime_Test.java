package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

class LocalDateTimeConversions_toLocalDateTime_Test {

    /**
     * The Converter instance used for all tests.
     */
    private Converter converter;

    /**
     * Common options for the Converter – the default constructor of {@link ConverterOptions}
     * provides an empty configuration which is sufficient for the built‑in conversions.
     */
    private static final ConverterOptions OPTIONS = new ConverterOptions();

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test to avoid cross‑test contamination.
        converter = new Converter(OPTIONS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the reference – helps the GC and makes the test intent clear.
        converter = null;
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// ZonedDateTime → LocalDateTime (zone offset ignored)
        Arguments.of("ZonedDateTime should return its local part unchanged", ZonedDateTime.of(2023, 5, 15, 10, 30, 45, 0, ZoneOffset.ofHours(2)), LocalDateTime.of(2023, 5, 15, 10, 30, 45)), // OffsetDateTime → LocalDateTime (offset discarded)
        Arguments.of("OffsetDateTime should drop the offset and keep the local date‑time", OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5)), LocalDateTime.of(2022, 12, 31, 23, 59, 59)), // LocalDateTime → itself
        Arguments.of("LocalDateTime should be returned unchanged", LocalDateTime.of(2021, 1, 1, 0, 0, 0), LocalDateTime.of(2021, 1, 1, 0, 0, 0)), // LocalDate → start of day
        Arguments.of("LocalDate is interpreted as the start of the day (midnight)", LocalDate.of(2020, 2, 29), LocalDateTime.of(2020, 2, 29, 0, 0, 0)), // java.util.Date (UTC instant) → LocalDateTime in UTC
        Arguments.of("java.util.Date should be converted to UTC LocalDateTime", Date.from(Instant.parse("2024-03-01T12:15:30Z")), LocalDateTime.of(2024, 3, 1, 12, 15, 30)), // java.sql.Timestamp → same as Date
        Arguments.of("java.sql.Timestamp should be treated like a LocalDateTime", Timestamp.valueOf(LocalDateTime.of(1999, 12, 31, 23, 59, 59)), LocalDateTime.of(1999, 12, 31, 23, 59, 59)), // Calendar (UTC) → LocalDateTime
        Arguments.of("Calendar instance (UTC) should be converted to the equivalent LocalDateTime", utcCalendar(2018, Calendar.JUNE, 5, 8, 0, 0), LocalDateTime.of(2018, 6, 5, 8, 0, 0)), // Long epoch‑millis → UTC LocalDateTime
        Arguments.of("Long epoch‑millis should be interpreted as an instant in UTC", Long.valueOf(Instant.parse("2005-07-20T14:45:00Z").toEpochMilli()), LocalDateTime.of(2005, 7, 20, 14, 45, 0)), // ISO‑8601 string without offset
        Arguments.of("ISO‑8601 string should be parsed into LocalDateTime", "2022-11-30T16:20:10", LocalDateTime.of(2022, 11, 30, 16, 20, 10)), // String with positive offset (offset discarded)
        Arguments.of("String with positive offset should be parsed and offset discarded", "2022-11-30T16:20:10+02:00", LocalDateTime.of(2022, 11, 30, 16, 20, 10)), // String with UTC 'Z' designator
        Arguments.of("String ending with 'Z' should be parsed as UTC and offset discarded", "2022-11-30T16:20:10Z", LocalDateTime.of(2022, 11, 30, 16, 20, 10)), // Negative epoch‑millis (before Unix epoch)
        Arguments.of("Negative epoch‑millis should be converted correctly", // 1 day before 1970‑01‑01
        Long.valueOf(-86_400_000L), LocalDateTime.of(1969, 12, 31, 0, 0, 0)), // Date‑only string → midnight
        Arguments.of("Date‑only string should be parsed as midnight of that day", "2022-11-30", LocalDateTime.of(2022, 11, 30, 0, 0, 0)), // java.sql.Date → midnight UTC
        Arguments.of("java.sql.Date should be converted to midnight UTC", new java.sql.Date(Instant.parse("2022-05-10T00:00:00Z").toEpochMilli()), LocalDateTime.of(2022, 5, 10, 0, 0, 0)), // Instant → UTC LocalDateTime
        Arguments.of("Instant should be interpreted as UTC and converted directly", Instant.parse("2023-07-04T15:30:45Z"), LocalDateTime.of(2023, 7, 4, 15, 30, 45)), // AtomicLong holding epoch‑millis
        Arguments.of("AtomicLong holding epoch‑millis should be treated like Long", new AtomicLong(Instant.parse("2021-01-01T00:00:00Z").toEpochMilli()), LocalDateTime.of(2021, 1, 1, 0, 0, 0)), // Calendar with non‑UTC zone (America/New_York)
        Arguments.of("Calendar in America/New_York should retain its fields when converted", newYorkCalendar(2021, Calendar.DECEMBER, 31, 23, 0, 0), LocalDateTime.of(2021, 12, 31, 23, 0, 0)));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("Impossible date (Feb 30) should fail", "2022-02-30T10:00:00"), Arguments.of("Completely non‑date string should fail", "not-a-date"), Arguments.of("Invalid offset (+25:00) should fail", "2022-11-30T16:20:10+25:00"));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(Arguments.of("Boolean should be unsupported", Boolean.TRUE), Arguments.of("Double numeric type is unsupported for epoch‑millis", Double.valueOf(12345.67)), Arguments.of("LocalTime lacks a date component", LocalTime.of(12, 34, 56)), Arguments.of("OffsetTime lacks a date component", OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2))));
    }

    /**
     * Creates a UTC Calendar set to the given date‑time fields (millis cleared).
     */
    private static Calendar utcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setLenient(false);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * Creates a Calendar in America/New_York zone set to the given fields (millis cleared).
     */
    private static Calendar newYorkCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.setLenient(false);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void shouldConvertFromLocalDateTime() {
        // truncate nanos for deterministic compare
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(now, converter);
        assertEquals(now, result, "Converting a LocalDateTime should return the same value");
    }

    @Test
    void shouldConvertFromZonedDateTime() {
        ZonedDateTime zoned = ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 0, ZoneId.of("Europe/Paris"));
        LocalDateTime expected = zoned.toLocalDateTime();
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(zoned, converter);
        assertEquals(expected, result, "ZonedDateTime should be converted to its LocalDateTime component");
    }

    @Test
    void shouldConvertFromOffsetDateTime() {
        OffsetDateTime offset = OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5));
        // OffsetDateTime → ZonedDateTime → LocalDateTime (zone offset is applied)
        ZonedDateTime zoned = offset.toZonedDateTime();
        LocalDateTime expected = zoned.toLocalDateTime();
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(offset, converter);
        assertEquals(expected, result, "OffsetDateTime should be converted correctly");
    }

    @Test
    void shouldConvertFromjavaUtilDate() {
        // java.util.Date stores an instant in UTC.
        Date date = Date.from(Instant.parse("2021-08-15T10:20:30Z"));
        // The conversion uses the system default zone; we normalise both sides to that zone.
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(date.toInstant(), systemZone);
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(date, converter);
        assertEquals(expected, result, "java.util.Date should be converted to LocalDateTime using the system default zone");
    }

    @Test
    void shouldConvertFromSqlTimestamp() {
        Timestamp ts = Timestamp.valueOf("2020-01-02 12:34:56.123456789");
        // Timestamp implements java.util.Date, so conversion follows the same rule.
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(ts.toInstant(), systemZone);
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, result, "java.sql.Timestamp should be converted to LocalDateTime");
    }

    @Test
    void shouldConvertFromStringIsoFormat() {
        String iso = "2022-07-04T15:45:30";
        LocalDateTime expected = LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(iso, converter);
        assertEquals(expected, result, "ISO‑8601 string should be parsed into LocalDateTime");
    }

    @Test
    void shouldConvertFromMapWithValueKey() {
        // The conversion logic for Map expects a key named "value" that holds a temporal object.
        LocalDateTime ldt = LocalDateTime.of(1999, 12, 31, 23, 59, 59);
        Map<String, Object> map = new HashMap<>();
        map.put("value", ldt);
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(map, converter);
        assertEquals(ldt, result, "Map containing a LocalDateTime under the key 'value' should be converted correctly");
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        // The conversion utilities treat a null source as null for reference types.
        assertNull(LocalDateTimeConversions.toLocalDateTime(null, converter), "Null input should result in null output");
    }

    @Test
    void shouldThrowWhenConversionIsImpossible() {
        // An object that has no known conversion to a temporal type should cause an IllegalArgumentException.
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toLocalDateTime(unsupported, converter), "Unsupported source types should cause an IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionsProvider")
    void shouldConvertValidInputs(String description, Object source, LocalDateTime expected) {
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, description);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenSourceIsNull() {
        // According to the JSON spec, a null source leads to a NullPointerException
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toLocalDateTime(null, converter), "Passing a null source object should cause a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidStringProvider")
    void shouldThrowDateTimeParseExceptionForMalformedStrings(String description, String source) {
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedSourceProvider")
    void shouldThrowIllegalArgumentExceptionForUnsupportedTypes(String description, Object source) {
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, converter), description);
    }

    @Test
    void shouldPropagateExceptionFromCustomConverter() {
        // A custom Converter that deliberately throws a RuntimeException
        Converter failingConverter = new Converter(OPTIONS) {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("converter failure");
            }
        };
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, 10, 30, 45, 0, ZoneOffset.UTC);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> LocalDateTimeConversions.toLocalDateTime(source, failingConverter), "Exception from custom Converter should propagate unchanged");
        assertEquals("converter failure", ex.getMessage());
    }
}
