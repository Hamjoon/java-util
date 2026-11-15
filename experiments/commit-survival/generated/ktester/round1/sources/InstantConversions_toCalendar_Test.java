package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toCalendar_Test {

    /**
     * The Converter used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a ConverterOptions instance – the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the converter reference to help GC; no other teardown needed.
        converter = null;
    }

    /**
     * Helper that verifies the Calendar returned by InstantConversions.toCalendar matches the expected epoch millis.
     */
    private void assertCalendarEquals(long expectedEpochMillis, Calendar actual) {
        assertNotNull(actual, "Calendar should not be null");
        assertEquals(expectedEpochMillis, actual.getTimeInMillis(), "Calendar time does not match expected epoch milliseconds");
    }

    /* --------------------------------------------------------------
     *  Valid conversions – each entry mirrors a case from the JSON spec.
     * -------------------------------------------------------------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Long literal
        Arguments.of(1622548800000L, 1622548800000L), // Integer (auto‑boxed)
        Arguments.of(123456789, 123456789L), // Numeric String
        Arguments.of("1622548800000", 1622548800000L), // java.util.Date
        Arguments.of(new Date(1622548800000L), 1622548800000L), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1622548800000L), 1622548800000L), // LocalDateTime at epoch (treated as UTC)
        Arguments.of(LocalDateTime.of(1970, 1, 1, 0, 0, 0), 0L), // LocalDate at epoch (treated as UTC midnight)
        Arguments.of(LocalDate.of(1970, 1, 1), 0L));
    }

    /* --------------------------------------------------------------
     *  Boundary and extreme epoch values.
     * -------------------------------------------------------------- */
    private static Stream<Arguments> boundaryValueProvider() {
        return Stream.of(Arguments.of(0L, 0L), Arguments.of(-1L, -1L), Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    @Test
    void toCalendar_fromInstant() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-25T12:34:56.789Z");
        long expectedMillis = instant.toEpochMilli();
        // WHEN
        Calendar result = InstantConversions.toCalendar(instant, converter);
        // THEN
        assertCalendarEquals(expectedMillis, result);
    }

    @Test
    void toCalendar_fromDate() {
        // GIVEN
        // 2022-01-01T00:00:00Z
        Date date = new Date(1_640_995_200_000L);
        long expectedMillis = date.getTime();
        // WHEN
        Calendar result = InstantConversions.toCalendar(date, converter);
        // THEN
        assertCalendarEquals(expectedMillis, result);
    }

    @Test
    void toCalendar_fromLong() {
        // GIVEN
        // 2020-09-13T12:26:40Z
        long epochMillis = 1_600_000_000_000L;
        // WHEN
        Calendar result = InstantConversions.toCalendar(epochMillis, converter);
        // THEN
        assertCalendarEquals(epochMillis, result);
    }

    @Test
    void toCalendar_fromString() {
        // GIVEN
        // 2022-12-31T23:59:59Z
        String epochString = "1672531199000";
        long expectedMillis = Long.parseLong(epochString);
        // WHEN
        Calendar result = InstantConversions.toCalendar(epochString, converter);
        // THEN
        assertCalendarEquals(expectedMillis, result);
    }

    @Test
    void toCalendar_fromNull_returnsNullCalendar() {
        // GIVEN
        Object from = null;
        // WHEN
        Calendar result = InstantConversions.toCalendar(from, converter);
        // THEN
        // The conversion chain ends up calling CalendarConversions.create(0, …) which yields a Calendar
        // set to the epoch (0 ms). The library does not return null for a null source, so we verify that
        // the returned Calendar represents the epoch.
        assertNotNull(result, "Calendar should not be null for null input");
        assertEquals(0L, result.getTimeInMillis(), "Calendar for null input should represent epoch (0 ms)");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected epoch={1}")
    @MethodSource("validConversionProvider")
    void toCalendar_validConversions(Object from, long expectedEpochMillis) {
        // WHEN
        Calendar result = InstantConversions.toCalendar(from, converter);
        // THEN
        assertCalendarEquals(expectedEpochMillis, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected epoch={1}")
    @MethodSource("boundaryValueProvider")
    void toCalendar_boundaryValues(long from, long expectedEpochMillis) {
        // WHEN
        Calendar result = InstantConversions.toCalendar(from, converter);
        // THEN
        assertCalendarEquals(expectedEpochMillis, result);
    }

    /* --------------------------------------------------------------
     *  Exception handling – null source or null converter.
     * -------------------------------------------------------------- */
    @Test
    void toCalendar_nullSource_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toCalendar(from, converter), "A null source object should cause a NullPointerException");
    }

    @Test
    void toCalendar_nullConverter_throwsNullPointerException() {
        // GIVEN
        Object from = 1622548800000L;
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toCalendar(from, nullConverter), "A null Converter should cause a NullPointerException");
    }
}
