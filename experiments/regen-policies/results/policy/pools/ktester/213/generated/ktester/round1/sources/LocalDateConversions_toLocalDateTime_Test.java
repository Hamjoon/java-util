package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

class LocalDateConversions_toLocalDateTime_Test {

    private static Converter converter;

    private static TimeZone originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a deterministic default time‑zone for the whole test suite
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Converter with default options – all built‑in conversions are registered
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default time‑zone
        TimeZone.setDefault(originalDefaultZone);
    }

    private static Stream<Arguments> stringIsoDateTimeProvider() {
        return Stream.of(Arguments.of("ISO‑8601 date‑time string should be parsed directly to the same LocalDateTime.", "2023-08-15T14:30:00", LocalDateTime.of(2023, 8, 15, 14, 30, 0)));
    }

    private static Stream<Arguments> stringIsoDateProvider() {
        return Stream.of(Arguments.of("ISO‑8601 date‑only string should be converted to LocalDateTime at start of day (midnight).", "2023-08-15", LocalDateTime.of(2023, 8, 15, 0, 0, 0)));
    }

    /**
     * Creates a {@link Calendar} instance with the given date‑time components.
     * The calendar uses the default time‑zone (UTC, forced in the test suite).
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault(), Locale.US);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        return cal;
    }

    @Test
    void toLocalDateTime_fromLocalDateTime_returnsSameInstance() {
        LocalDateTime source = LocalDateTime.of(2023, Month.MARCH, 15, 10, 20, 30);
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        assertEquals(source, result, "Conversion from LocalDateTime should return the same value");
    }

    @Test
    void toLocalDateTime_fromString_parsesIsoDateTime() {
        String source = "2023-07-04T14:45:12";
        LocalDateTime expected = LocalDateTime.of(2023, 7, 4, 14, 45, 12);
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, "String ISO‑8601 should be parsed to the correct LocalDateTime");
    }

    @Test
    void toLocalDateTime_fromDate_convertsUsingDefaultZone() {
        // 2022‑01‑01T12:00:00Z expressed as a java.util.Date (milliseconds since epoch)
        Instant instant = Instant.parse("2022-01-01T12:00:00Z");
        Date source = Date.from(instant);
        // Because the default zone is forced to UTC in setup, the expected LocalDateTime is UTC‑based
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, "Date should be converted to LocalDateTime using the default zone (UTC)");
    }

    @Test
    void toLocalDateTime_fromZonedDateTime_stripsZoneInformation() {
        ZonedDateTime source = ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, // any zone is fine
        ZoneId.of("America/New_York"));
        LocalDateTime expected = source.toLocalDateTime();
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, "ZonedDateTime should be converted to its LocalDateTime component");
    }

    @Test
    void toLocalDateTime_fromTimestamp_convertsCorrectly() {
        // Timestamp internally stores nanoseconds; use a known value
        Timestamp source = Timestamp.valueOf("2020-05-20 08:15:30.123456789");
        // Expected LocalDateTime is the same instant (timestamp has no zone)
        LocalDateTime expected = source.toLocalDateTime();
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, "Timestamp should be converted to the equivalent LocalDateTime");
    }

    @Test
    void toLocalDateTime_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toLocalDateTime(null, converter), "Passing null as the source should result in NPE (toZonedDateTime returns null)");
    }

    // ------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringIsoDateTimeProvider")
    void toLocalDateTime_stringIsoDateTime(String description, String input, LocalDateTime expected) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(input, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringIsoDateProvider")
    void toLocalDateTime_stringIsoDate(String description, String input, LocalDateTime expected) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(input, converter);
        assertEquals(expected, result, description);
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_javaUtilDate() {
        // 2023‑01‑01T00:00:00Z => epoch millis 1672531200000L
        Date source = new Date(1672531200000L);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(1672531200000L), ZoneOffset.UTC);
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, "java.util.Date should be converted to the corresponding LocalDateTime in UTC");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_javaSqlTimestamp() {
        Timestamp source = Timestamp.valueOf("2022-12-31 23:59:59");
        LocalDateTime expected = source.toLocalDateTime();
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, "java.sql.Timestamp should be converted to the same LocalDateTime value.");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_instant() {
        Instant instant = Instant.parse("2021-12-31T23:59:59Z");
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime result = LocalDateConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, result, "Instant (UTC) should be converted to LocalDateTime in UTC.");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_longEpochMillis() {
        // 2021‑07‑01T00:00:00Z
        long epochMillis = 1625097600000L;
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
        LocalDateTime result = LocalDateConversions.toLocalDateTime(epochMillis, converter);
        assertEquals(expected, result, "Long epoch millis should be interpreted as an instant and converted to LocalDateTime.");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_localDate() {
        LocalDate source = LocalDate.of(2021, 4, 5);
        LocalDateTime expected = LocalDateTime.of(2021, 4, 5, 0, 0);
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, "LocalDate should be converted to start of day (midnight).");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_calendar() {
        Calendar cal = createCalendar(2022, Calendar.JUNE, 15, 8, 20, 45);
        LocalDateTime expected = LocalDateTime.of(2022, 6, 15, 8, 20, 45);
        LocalDateTime result = LocalDateConversions.toLocalDateTime(cal, converter);
        assertEquals(expected, result, "Calendar should be converted using its time‑zone (UTC in test).");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_zonedDateTime() {
        ZonedDateTime source = ZonedDateTime.of(2020, 5, 10, 15, 45, 30, 0, ZoneId.of("Europe/Paris"));
        LocalDateTime expected = source.toLocalDateTime();
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, "ZonedDateTime should be converted to its LocalDateTime component.");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_offsetDateTime() {
        OffsetDateTime source = OffsetDateTime.of(2021, 3, 14, 9, 30, 0, 0, ZoneOffset.ofHours(-5));
        LocalDateTime expected = source.toLocalDateTime();
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, "OffsetDateTime should be converted to its LocalDateTime part, ignoring the offset.");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_nullInput_returnsNull() {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(null, converter);
        assertNull(result, "A null source value should result in a null LocalDateTime (no exception).");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_invalidType_throwsIllegalArgumentException() {
        Object invalid = new Object();
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toLocalDateTime(invalid, converter), "Unsupported type should cause IllegalArgumentException.");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_invalidString_throwsDateTimeParseException() {
        String bad = "not-a-date";
        assertThrows(DateTimeParseException.class, () -> LocalDateConversions.toLocalDateTime(bad, converter), "Malformed date‑time string should trigger a parsing exception.");
    }

    // ------------------------------------------------------------------
    @Test
    void toLocalDateTime_nullConverter_throwsNullPointerException() {
        String source = "2023-08-15T14:30:00";
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toLocalDateTime(source, null), "Passing a null Converter should cause NullPointerException.");
    }
}
