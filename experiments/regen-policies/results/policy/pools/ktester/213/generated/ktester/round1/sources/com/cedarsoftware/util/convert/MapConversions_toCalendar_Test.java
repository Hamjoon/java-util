package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class MapConversions_toCalendar_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /* --------------------------------------------------------------
     *  Helper – a simple functional interface for assertions
     * -------------------------------------------------------------- */
    @FunctionalInterface
    private interface Consumer<T> {

        void accept(T t);
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter now requires a ConverterOptions instance.
//         converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Supplies a stream of arguments for the normal conversion tests.
     * Each argument consists of:
     *   - a description (used only for readability in the test name)
     *   - the source object to convert
     *   - a lambda that asserts the resulting Calendar is correct
     */
    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// java.util.Date → Calendar
        Arguments.of("Date epoch 0", new Date(0L), (Consumer<Calendar>) cal -> assertEquals(0L, cal.getTimeInMillis(), "Date(0) should produce a Calendar at epoch")), // java.time.Instant → Calendar (via epochMillis key)
        Arguments.of("Instant epoch 123456789", Instant.ofEpochMilli(123_456_789L), (Consumer<Calendar>) cal -> assertEquals(123_456_789L, cal.getTimeInMillis(), "Instant should be converted to the same epoch millis")), // Long (epoch millis) → Calendar
        Arguments.of("Long epoch 123456789", 123_456_789L, (Consumer<Calendar>) cal -> assertEquals(123_456_789L, cal.getTimeInMillis(), "Long value should be interpreted as epoch millis")), // java.sql.Date → Calendar
        Arguments.of("java.sql.Date epoch 0", new java.sql.Date(0L), (Consumer<Calendar>) cal -> assertEquals(0L, cal.getTimeInMillis(), "java.sql.Date(0) should map to epoch")), // java.sql.Timestamp → Calendar
        Arguments.of("Timestamp epoch 123456789", new java.sql.Timestamp(123_456_789L), (Consumer<Calendar>) cal -> assertEquals(123_456_789L, cal.getTimeInMillis(), "Timestamp should map to the same epoch millis")), // LocalDate → Calendar (midnight of that date in default zone)
        Arguments.of("LocalDate 2022-05-15", LocalDate.of(2022, 5, 15), (Consumer<Calendar>) cal -> {
            ZonedDateTime zdt = LocalDate.of(2022, 5, 15).atStartOfDay(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "LocalDate should become midnight of that day");
        }), // LocalDateTime → Calendar (same fields, default zone)
        Arguments.of("LocalDateTime 2022-05-15T13:45:30.123", LocalDateTime.of(2022, 5, 15, 13, 45, 30, 123_000_000), (Consumer<Calendar>) cal -> {
            ZonedDateTime zdt = LocalDateTime.of(2022, 5, 15, 13, 45, 30, 123_000_000).atZone(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "LocalDateTime should be converted preserving the fields");
        }), // LocalTime → Calendar (epoch date + time)
        Arguments.of("LocalTime 13:45:30.123", LocalTime.of(13, 45, 30, 123_000_000), (Consumer<Calendar>) cal -> {
            ZonedDateTime zdt = LocalDate.ofEpochDay(0).atTime(LocalTime.of(13, 45, 30, 123_000_000)).atZone(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "LocalTime should be interpreted on the epoch date");
        }), // OffsetTime → Calendar (apply offset to get UTC instant)
        Arguments.of("OffsetTime 13:45:30.123+02:00", OffsetTime.of(13, 45, 30, 123_000_000, ZoneOffset.ofHours(2)), (Consumer<Calendar>) cal -> {
            Instant instant = OffsetTime.of(13, 45, 30, 123_000_000, ZoneOffset.ofHours(2)).atDate(LocalDate.ofEpochDay(0)).toInstant();
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "OffsetTime should be normalised to UTC then to default zone");
        }), // OffsetDateTime → Calendar (normalise to instant)
        Arguments.of("OffsetDateTime 2022-05-15T13:45:30.123-05:00", OffsetDateTime.of(2022, 5, 15, 13, 45, 30, 123_000_000, ZoneOffset.ofHours(-5)), (Consumer<Calendar>) cal -> {
            Instant instant = OffsetDateTime.of(2022, 5, 15, 13, 45, 30, 123_000_000, ZoneOffset.ofHours(-5)).toInstant();
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "OffsetDateTime should be converted to the same instant");
        }), // ZonedDateTime → Calendar (preserve instant)
        Arguments.of("ZonedDateTime 2022-05-15T13:45:30.123 America/New_York", ZonedDateTime.of(2022, 5, 15, 13, 45, 30, 123_000_000, ZoneId.of("America/New_York")), (Consumer<Calendar>) cal -> {
            Instant instant = ZonedDateTime.of(2022, 5, 15, 13, 45, 30, 123_000_000, ZoneId.of("America/New_York")).toInstant();
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "ZonedDateTime should be converted to the same instant");
        }), // YearMonth → Calendar (first day of month at midnight)
        Arguments.of("YearMonth 2022-05", YearMonth.of(2022, 5), (Consumer<Calendar>) cal -> {
            ZonedDateTime zdt = YearMonth.of(2022, 5).atDay(1).atStartOfDay(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "YearMonth should become first day of month at midnight");
        }), // MonthDay → Calendar (current year, midnight)
        Arguments.of("MonthDay 05-15", MonthDay.of(5, 15), (Consumer<Calendar>) cal -> {
            int year = Year.now().getValue();
            ZonedDateTime zdt = MonthDay.of(5, 15).atYear(year).atStartOfDay(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "MonthDay should use current year and midnight");
        }), // Period → Calendar (epoch + period)
        Arguments.of("Period P1Y2M3D", Period.of(1, 2, 3), (Consumer<Calendar>) cal -> {
            LocalDate epoch = LocalDate.ofEpochDay(0);
            LocalDate resultDate = epoch.plus(Period.of(1, 2, 3));
            ZonedDateTime zdt = resultDate.atStartOfDay(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "Period should be added to epoch start");
        }), // Duration → Calendar (epoch + duration)
        Arguments.of("Duration PT5H30M", Duration.ofHours(5).plusMinutes(30), (Consumer<Calendar>) cal -> {
            Instant instant = Instant.EPOCH.plus(Duration.ofHours(5).plusMinutes(30));
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "Duration should be added to epoch start");
        }), // ZoneOffset → Calendar (epoch shifted by offset)
        Arguments.of("ZoneOffset +03:00", ZoneOffset.ofHours(3), (Consumer<Calendar>) cal -> {
            Instant instant = Instant.EPOCH.plusSeconds(3L * 3600);
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "ZoneOffset should shift epoch accordingly");
        }), // ZoneId → Calendar (epoch with supplied zone)
        Arguments.of("ZoneId Europe/Paris", ZoneId.of("Europe/Paris"), (Consumer<Calendar>) cal -> {
            Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
            expected.setTimeInMillis(0L);
            assertEquals(expected.getTimeInMillis(), cal.getTimeInMillis(), "Calendar time millis should be epoch");
            assertEquals(expected.getTimeZone().getID(), cal.getTimeZone().getID(), "Calendar should use the supplied ZoneId as its time zone");
        }), // TimeZone → Calendar (epoch with supplied zone)
        Arguments.of("TimeZone GMT+2", TimeZone.getTimeZone("GMT+2"), (Consumer<Calendar>) cal -> {
            Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("GMT+2"));
            expected.setTimeInMillis(0L);
            assertEquals(expected.getTimeInMillis(), cal.getTimeInMillis(), "Calendar time millis should be epoch");
            assertEquals(expected.getTimeZone().getID(), cal.getTimeZone().getID(), "Calendar should use the supplied TimeZone");
        }), // Map with both 'date' and other keys – 'date' wins
        Arguments.of("Map with date and value", Map.of("date", new Date(0L), "value", 12345L), (Consumer<Calendar>) cal -> assertEquals(0L, cal.getTimeInMillis(), "'date' entry should be used, other keys ignored")), // Map with 'value' and 'epochMillis' – order defined by CALENDAR_KEYS
        Arguments.of("Map with value and epochMillis", Map.of("value", 987_654_321L, "epochMillis", 123L), (Consumer<Calendar>) cal -> assertEquals(987_654_321L, cal.getTimeInMillis(), "'value' key should be processed before 'epochMillis'")), // Negative epoch millis
        Arguments.of("Negative long epoch", -123_456_789L, (Consumer<Calendar>) cal -> assertEquals(-123_456_789L, cal.getTimeInMillis(), "Negative epoch millis should be interpreted correctly")), // ISO‑8601 string
        Arguments.of("ISO string 2022-05-15T13:45:30.123Z", "2022-05-15T13:45:30.123Z", (Consumer<Calendar>) cal -> {
            Instant instant = Instant.parse("2022-05-15T13:45:30.123Z");
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
            assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "ISO‑8601 string should be parsed to the same instant");
        }));
    }

    /**
     * Provides a stream of arguments for the exception‑handling tests.
     */
//     private static Stream<Arguments> exceptionProvider() {
//         return Stream.of(// Null converter with Date
//         Arguments.of("Null converter with Date", new Date(0L), null, NullPointerException.class), // Non‑ISO string
//         Arguments.of("Invalid ISO string", "not-a-date-string", converter, ConversionException.class), // Map value key with non‑numeric string
//         Arguments.of("Map value non‑numeric", Map.of("value", "abc"), converter, ConversionException.class), // Map calendar key with wrong type
//         Arguments.of("Map calendar wrong type", Map.of("calendar", 12345), converter, ConversionException.class), // Map epochMillis with Boolean
//         Arguments.of("Map epochMillis Boolean", Map.of("epochMillis", true), converter, ConversionException.class), // Unsupported collection type
//         Arguments.of("List as source", Arrays.asList(1, 2, 3), converter, ConversionException.class), // Map value key with null
//         Arguments.of("Map value null", Map.of("value", null), converter, ConversionException.class), // Empty string
//         Arguments.of("Empty string", "", converter, ConversionException.class), // Null converter with unsupported object
//         Arguments.of("Null converter with Object", new Object(), null, NullPointerException.class), // Map date key with non‑Date object
//         Arguments.of("Map date non‑Date", Map.of("date", new Object()), converter, ConversionException.class));
//     }

    @Test
    void toCalendar_givenNull_returnsNull() {
        Calendar result = MapConversions.toCalendar(null, converter);
        assertNull(result, "Converting a null source should produce a null Calendar");
    }

    @Test
    void toCalendar_givenCalendarInstance_returnsSameInstance() {
        Calendar now = Calendar.getInstance();
        Calendar result = MapConversions.toCalendar(now, converter);
        assertSame(now, result, "When the source is already a Calendar, the same instance should be returned");
    }

    @Test
    void toCalendar_givenMapWithCalendarKey_returnsThatCalendar() {
        Calendar expected = Calendar.getInstance();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.CALENDAR, expected);
        Calendar result = MapConversions.toCalendar(map, converter);
        assertSame(expected, result, "Map containing a 'calendar' key should yield the associated Calendar");
    }

    @Test
    void toCalendar_givenMapWithValueKeyContainingCalendar_returnsThatCalendar() {
        Calendar expected = Calendar.getInstance();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.VALUE, expected);
        Calendar result = MapConversions.toCalendar(map, converter);
        assertSame(expected, result, "Map containing a 'value' key with a Calendar should be converted correctly");
    }

    @Test
    void toCalendar_givenMapWithEpochMillisKey_returnsCalendarWithCorrectTime() {
        long epoch = 1_632_000_000_000L;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        Calendar result = MapConversions.toCalendar(map, converter);
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(epoch, result.getTimeInMillis(), "Calendar created from 'epochMillis' map entry should have the exact epoch time");
    }

    @Test
    void toCalendar_givenMapWithVKeyContainingCalendar_returnsThatCalendar() {
        Calendar expected = Calendar.getInstance();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.V, expected);
        Calendar result = MapConversions.toCalendar(map, converter);
        assertSame(expected, result, "Map containing a '_v' key with a Calendar should be converted correctly");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalConversionProvider")
    void toCalendar_normalConversions(String description, Object source, Consumer<Calendar> assertions) {
        Calendar result = MapConversions.toCalendar(source, converter);
        assertNotNull(result, "Resulting Calendar should never be null for valid input");
        assertions.accept(result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toCalendar_exceptionScenarios(String description, Object source, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapConversions.toCalendar(source, conv), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}