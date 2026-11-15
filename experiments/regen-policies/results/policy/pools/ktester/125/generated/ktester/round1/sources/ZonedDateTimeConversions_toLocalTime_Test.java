package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;
import static com.cedarsoftware.util.convert.MapConversions.TIME;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

class ZonedDateTimeConversions_toLocalTime_Test {

    /**
     * Converter used for all tests – created once for the whole test suite.
     */
    private static Converter converter;

    /**
     * Original default zone – restored after all tests.
     */
    private static ZoneId originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original default zone.
        originalDefaultZone = ZoneId.systemDefault();
        // Force UTC to make expected results deterministic.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Initialise the converter (default options are sufficient for these tests).
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone.
        TimeZone.setDefault(TimeZone.getTimeZone(originalDefaultZone));
        converter = null;
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (parameterised)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> successfulConversions() {
        return Stream.of(// ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 13, 45, 30, 0, ZoneId.of("UTC")), "13:45:30", "Converts a ZonedDateTime (UTC) to its LocalTime component."), // OffsetDateTime (+02:00)
        Arguments.of(OffsetDateTime.of(2023, 5, 10, 9, 15, 0, 0, ZoneOffset.ofHours(2)), "09:15:00", "Converts an OffsetDateTime with +02:00 offset."), // java.util.Date (instant in UTC)
        Arguments.of(Date.from(Instant.parse("2023-01-01T12:30:00Z")), "12:30:00", "Converts a java.util.Date representing an instant in UTC."), // java.sql.Timestamp (treated as local date‑time)
        Arguments.of(Timestamp.valueOf("2023-02-15 08:05:45"), "08:05:45", "Converts a java.sql.Timestamp."), // Calendar (UTC)
        Arguments.of(createUtcCalendar(2023, Calendar.MARCH, 20, 17, 40, 55), "17:40:55", "Converts a java.util.Calendar set to UTC."), // Long epoch‑millis (UTC)
        Arguments.of(Long.valueOf(Instant.parse("2023-04-01T06:20:00Z").toEpochMilli()), "06:20:00", "Converts a Long representing epoch‑millis."), // Plain ISO‑LOCAL_TIME string
        Arguments.of("10:20:30", "10:20:30", "Converts a plain ISO‑LOCAL_TIME string."), // ISO‑INSTANT string
        Arguments.of("2023-04-05T14:30:00Z", "14:30:00", "Converts an ISO‑INSTANT string."), // Map with time and zone
        Arguments.of(Map.of(TIME, "15:45:00", ZONE, "America/New_York"), "15:45:00", "Converts a Map containing a time string and a zone identifier."), // Map with Date and zone (epoch 0)
        Arguments.of(Map.of("date", new Date(0), ZONE, "UTC"), "00:00:00", "Converts a Map with a java.util.Date (epoch 0) and a zone."));
    }

    /* --------------------------------------------------------------
     *  Null handling test cases (parameterised)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> nullHandling() {
        return Stream.of(Arguments.of(null, converter, NullPointerException.class, "Source object is null."), Arguments.of(ZonedDateTime.now(), null, NullPointerException.class, "Converter argument is null."), Arguments.of(null, null, NullPointerException.class, "Both arguments are null."));
    }

    /* --------------------------------------------------------------
     *  Unsupported source types (parameterised)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> unsupportedSources() {
        return Stream.of(Arguments.of(Boolean.TRUE, "Boolean is not a supported conversion source."), Arguments.of(BigDecimal.valueOf(123.45), "BigDecimal is not a recognized temporal source."), Arguments.of(new Object(), "Arbitrary Object instances are unsupported."));
    }

    /* --------------------------------------------------------------
     *  Invalid string formats (parameterised)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> invalidStringFormats() {
        return Stream.of(Arguments.of("not-a-time", "String that cannot be parsed as a time or instant."), Arguments.of("2023-13-01T00:00:00Z", "Month value 13 is invalid."), Arguments.of("2023-02-30T10:00:00Z", "February 30th does not exist."), Arguments.of("25:00:00", "Hour value 25 exceeds the valid range."));
    }

    /* --------------------------------------------------------------
     *  Invalid map structures (parameterised)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> invalidMapStructures() {
        return Stream.of(Arguments.of(Map.of("zone", "UTC"), IllegalArgumentException.class, "Map missing both time/date keys."), Arguments.of(Map.of("time", "12:34:56"), IllegalArgumentException.class, "Map provides a time but no zone."), Arguments.of(Map.of(TIME, "invalid", ZONE, "UTC"), DateTimeParseException.class, "Time string is not parsable."), Arguments.of(Map.of("date", new Date(0), ZONE, "Invalid/Zone"), DateTimeException.class, "Zone identifier is not recognized."), Arguments.of(Map.of("date", null, ZONE, "UTC"), IllegalArgumentException.class, "Map contains a null date value."));
    }

    /* --------------------------------------------------------------
     *  Invalid numeric types (parameterised)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> invalidNumericTypes() {
        return Stream.of(Arguments.of(Double.valueOf(12345.67), IllegalArgumentException.class, "Double epoch‑millis is not supported."), Arguments.of(new AtomicLong(Instant.now().toEpochMilli()), NullPointerException.class, "AtomicLong source is valid but the converter argument is null."));
    }

    /**
     * Creates a {@link Calendar} instance set to UTC with the supplied date‑time fields.
     */
    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void testToLocalTimeFromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 14, 23, 45, 0, ZoneId.of("Europe/Paris"));
        LocalTime expected = zdt.toLocalTime();
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(zdt, converter);
        assertEquals(expected, actual, "LocalTime should be extracted unchanged from a ZonedDateTime");
    }

    @Test
    void testToLocalTimeFromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2022, 12, 31, 9, 15, 30, 0, ZoneOffset.ofHours(-5));
        // The conversion first creates a ZonedDateTime in the same instant,
        // then extracts the local time of that ZonedDateTime (which is the same as the offset time).
        LocalTime expected = odt.toLocalTime();
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(odt, converter);
        assertEquals(expected, actual, "LocalTime should match the time component of an OffsetDateTime");
    }

    @Test
    void testToLocalTimeFromDate() {
        // Create a Date representing a known instant.
        Instant instant = Instant.parse("2021-03-14T10:20:30.00Z");
        Date date = Date.from(instant);
        // The conversion will treat the Date as an instant in the system default zone.
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        LocalTime expected = zdt.toLocalTime();
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(date, converter);
        assertEquals(expected, actual, "LocalTime should be derived from a java.util.Date using the system default zone");
    }

    @Test
    void testToLocalTimeFromMapWithZoneAndTime() {
        // Map representation expected by the conversion logic:
        //   "zone" -> zone id string
        //   "time" -> time string (ISO‑local‑time)
        Map<String, Object> map = new CompactMap<>();
        map.put(ZONE, "America/New_York");
        // 7:45:12 AM in New York
        map.put(TIME, "07:45:12");
        LocalTime expected = LocalTime.of(7, 45, 12);
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(map, converter);
        assertEquals(expected, actual, "LocalTime should be parsed from a map containing zone and time keys");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversions")
    void testSuccessfulConversions(Object from, String expectedTime, String description) {
        LocalTime expected = LocalTime.parse(expectedTime);
        LocalTime actual = ZonedDateTimeConversions.toLocalTime(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullHandling")
    void testNullHandling(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toLocalTime(from, conv), description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("unsupportedSources")
    void testUnsupportedSourceTypes(Object from, String description) {
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toLocalTime(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("invalidStringFormats")
    void testInvalidStringFormats(String from, String description) {
        assertThrows(DateTimeParseException.class, () -> ZonedDateTimeConversions.toLocalTime(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidMapStructures")
    void testInvalidMapStructures(Map<String, Object> map, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toLocalTime(map, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidNumericTypes")
    void testInvalidNumericTypes(Object from, Class<? extends Throwable> expectedException, String description) {
        Converter conv = (expectedException == NullPointerException.class) ? null : converter;
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toLocalTime(from, conv), description);
    }
}
