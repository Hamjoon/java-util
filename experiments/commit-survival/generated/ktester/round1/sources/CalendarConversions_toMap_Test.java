package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toMap_Test {

    private Calendar calendar;

    @Mock
    private Converter // Converter is not used by toMap, a mock is sufficient
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a deterministic Calendar instance (UTC) with known fields
        calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2021);
        // 0‑based, March = 2
        calendar.set(Calendar.MONTH, Calendar.MARCH);
        calendar.set(Calendar.DAY_OF_MONTH, 15);
        calendar.set(Calendar.HOUR_OF_DAY, 13);
        calendar.set(Calendar.MINUTE, 45);
        calendar.set(Calendar.SECOND, 30);
        calendar.set(Calendar.MILLISECOND, 123);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        calendar = null;
    }

    /**
     * Creates a {@link GregorianCalendar} with the supplied fields.
     *
     * @param year   year (e.g. 2021)
     * @param month  month (0‑based, as required by {@link Calendar})
     * @param day    day of month
     * @param hour   hour of day (0‑23)
     * @param minute minute (0‑59)
     * @param second second (0‑59)
     * @param millis millisecond (0‑999)
     * @param tzId   time‑zone ID (e.g. "UTC")
     * @return configured {@link GregorianCalendar}
     */
    private static GregorianCalendar buildCalendar(int year, int month, int day, int hour, int minute, int second, int millis, String tzId) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone(tzId));
        // make illegal values throw exceptions when possible
        cal.setLenient(false);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Builds the expected map that {@code CalendarConversions.toMap} should return for a given calendar.
     *
     * @param cal the source calendar
     * @return map with keys DATE, TIME, ZONE, EPOCH_MILLIS
     */
    private static Map<String, Object> expectedMapFromCalendar(Calendar cal) {
        Map<String, Object> map = new HashMap<>();
        // DATE
        String date = LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)).toString();
        // TIME (nanoseconds = millis * 1_000_000)
        String time = LocalTime.of(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND) * 1_000_000).toString();
        map.put(MapConversions.DATE, date);
        map.put(MapConversions.TIME, time);
        map.put(MapConversions.ZONE, cal.getTimeZone().toZoneId().toString());
        map.put(MapConversions.EPOCH_MILLIS, cal.getTimeInMillis());
        return map;
    }

    static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// 2023‑05‑15 13:45:30.123 UTC
        Arguments.of(buildCalendar(2023, Calendar.MAY, 15, 13, 45, 30, 123, "UTC")), // 1999‑12‑31 23:59:59.999 America/New_York
        Arguments.of(buildCalendar(1999, Calendar.DECEMBER, 31, 23, 59, 59, 999, "America/New_York")));
    }

    static Stream<Arguments> boundaryCasesProvider() {
        return Stream.of(// Epoch start 1970‑01‑01T00:00:00.000 UTC
        Arguments.of(buildCalendar(1970, Calendar.JANUARY, 1, 0, 0, 0, 0, "UTC")), // Leap day 2020‑02‑29T12:00:00.500 UTC
        Arguments.of(buildCalendar(2020, Calendar.FEBRUARY, 29, 12, 0, 0, 500, "UTC")), // Ambiguous DST fall‑back 2021‑11‑07T01:30:00.000 America/New_York
        Arguments.of(buildCalendar(2021, Calendar.NOVEMBER, 7, 1, 30, 0, 0, "America/New_York")), // Pre‑1970 date 1965‑07‑20T20:17:40.123 UTC
        Arguments.of(buildCalendar(1965, Calendar.JULY, 20, 20, 17, 40, 123, "UTC")));
    }

    static Stream<Arguments> errorCasesProvider() {
        return Stream.of(// Not a Calendar instance
        Arguments.of(new Object(), ClassCastException.class), // Null 'from' argument
        Arguments.of(null, NullPointerException.class), // Invalid month (-1) – month+1 = 0 → invalid month for LocalDate
        Arguments.of(buildCalendar(2021, -1, 10, 10, 10, 10, 0, "UTC"), DateTimeException.class), // Invalid day (0) – LocalDate.of will reject day 0
        Arguments.of(buildCalendar(2021, Calendar.JANUARY, 0, 10, 10, 10, 0, "UTC"), DateTimeException.class), // Invalid hour (24) – LocalTime.of will reject hour 24
        Arguments.of(buildCalendar(2021, Calendar.JANUARY, 10, 24, 10, 10, 0, "UTC"), DateTimeException.class), // Invalid minute (60)
        Arguments.of(buildCalendar(2021, Calendar.JANUARY, 10, 10, 60, 10, 0, "UTC"), DateTimeException.class), // Invalid second (60)
        Arguments.of(buildCalendar(2021, Calendar.JANUARY, 10, 10, 10, 60, 0, "UTC"), DateTimeException.class), // Invalid millisecond (-1) – negative nanoseconds cause exception in LocalTime.of
        Arguments.of(buildCalendar(2021, Calendar.JANUARY, 10, 10, 10, 10, -1, "UTC"), DateTimeException.class));
    }

    @Test
    void testToMapProducesCorrectMap() {
        // GIVEN – a Calendar instance and a (mocked) Converter
        // WHEN
        Map<String, Object> result = CalendarConversions.toMap(calendar, mockConverter);
        // THEN – verify that all expected entries are present and correctly formatted
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(4, result.size(), "Map should contain exactly four entries");
        // Expected values
        String expectedDate = "2021-03-15";
        // milliseconds converted to nanoseconds
        String expectedTime = "13:45:30.123000000";
        String expectedZone = "UTC";
        long expectedEpoch = calendar.getTimeInMillis();
        assertEquals(expectedDate, result.get(MapConversions.DATE), "DATE entry is incorrect");
        assertEquals(expectedTime, result.get(MapConversions.TIME), "TIME entry is incorrect");
        assertEquals(expectedZone, result.get(MapConversions.ZONE), "ZONE entry is incorrect");
        assertEquals(expectedEpoch, result.get(MapConversions.EPOCH_MILLIS), "EPOCH_MILLIS entry is incorrect");
    }

    @ParameterizedTest(name = "[Normal] {index} – {0.timeZone.id}")
    @MethodSource("normalCasesProvider")
    void testToMap_NormalCases(GregorianCalendar calendar) {
        Map<String, Object> expected = expectedMapFromCalendar(calendar);
        Map<String, Object> result = CalendarConversions.toMap(calendar, mockConverter);
        assertNotNull(result, "Result map must not be null");
        assertEquals(expected, result, "Map content does not match expected values");
    }

    @ParameterizedTest(name = "[Boundary] {index} – {0.timeZone.id}")
    @MethodSource("boundaryCasesProvider")
    void testToMap_BoundaryCases(GregorianCalendar calendar) {
        Map<String, Object> expected = expectedMapFromCalendar(calendar);
        Map<String, Object> result = CalendarConversions.toMap(calendar, mockConverter);
        assertEquals(expected, result, "Boundary case conversion failed");
    }

    @Test
    void testToMap_NonNullConverter() {
        GregorianCalendar calendar = buildCalendar(2000, Calendar.JANUARY, 1, 0, 0, 0, 0, "Asia/Tokyo");
        // Use a dummy (non‑null) converter – the method ignores it
        Converter dummyConverter = new Converter() {
        };
        Map<String, Object> expected = expectedMapFromCalendar(calendar);
        Map<String, Object> result = CalendarConversions.toMap(calendar, dummyConverter);
        assertEquals(expected, result, "Conversion with non‑null Converter should behave identically");
    }

    @ParameterizedTest(name = "[Error] {index} – expecting {1.simpleName}")
    @MethodSource("errorCasesProvider")
    void testToMap_ErrorCases(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CalendarConversions.toMap(from, mockConverter), "Expected exception not thrown for input: " + from);
    }
}
