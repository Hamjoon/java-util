package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toLong_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /* -----------------------------------------------------------
     *  Simple stub implementation of Converter used in a few tests
     * ----------------------------------------------------------- */
    private static class MockConverter implements Converter {
        // No behaviour needed – the CalendarConversions.toLong method does not use it.
    }

    private static Stream<Arguments> validCalendarProvider() {
        // Helper to create a GregorianCalendar with UTC zone and a given instant
        Calendar utcCalendarZero = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        // 1970‑01‑01T00:00:00Z
        utcCalendarZero.setTimeInMillis(0L);
        Calendar utcCalendar2021 = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        utcCalendar2021.set(2021, Calendar.DECEMBER, 31, 23, 59, 59);
        utcCalendar2021.set(Calendar.MILLISECOND, 123);
        // 2021‑12‑31T23:59:59.123Z
        long expected2021 = 1640995199123L;
        Calendar pstCalendarSameInstant = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
        pstCalendarSameInstant.set(2021, Calendar.DECEMBER, 31, 15, 59, 59);
        pstCalendarSameInstant.set(Calendar.MILLISECOND, 123);
        // The same instant as utcCalendar2021, therefore same epoch millis
        Calendar utcCalendar1960 = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        utcCalendar1960.set(1960, Calendar.JANUARY, 1, 0, 0, 0);
        utcCalendar1960.set(Calendar.MILLISECOND, 0);
        // 1960‑01‑01T00:00:00Z
        long expected1960 = -315619200000L;
        Calendar defaultCalWithMillis = new GregorianCalendar();
        defaultCalWithMillis.setTimeInMillis(123_456_789L);
        long expectedDefault = 123_456_789L;
        // A dummy non‑null Converter (the method ignores it)
        Converter dummyConverter = new MockConverter();
        return Stream.of(Arguments.of("Epoch start (1970‑01‑01T00:00:00Z)", utcCalendarZero, null, 0L), Arguments.of("2021‑12‑31 23:59:59.123 UTC", utcCalendar2021, null, expected2021), Arguments.of("Same instant in PST (UTC‑08:00)", pstCalendarSameInstant, null, expected2021), Arguments.of("Pre‑epoch date 1960‑01‑01T00:00:00Z", utcCalendar1960, null, expected1960), Arguments.of("Non‑null Converter supplied (ignored)", defaultCalWithMillis, dummyConverter, expectedDefault));
    }

    private static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of("null Calendar, null Converter", null, null), Arguments.of("null Calendar, non‑null Converter", null, new MockConverter()));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("String value", "2021-12-31", null), Arguments.of("java.util.Date instance", new Date(), null), Arguments.of("Integer value with non‑null Converter", Integer.valueOf(42), new MockConverter()));
    }

    @Test
    void toLong_returnsEpochMilli_forEpochZero() {
        // GIVEN
        Calendar calendar = new GregorianCalendar();
        // 1970‑01‑01T00:00:00Z
        calendar.setTimeInMillis(0L);
        // WHEN
        Long result = CalendarConversions.toLong(calendar, mockConverter);
        // THEN
        assertEquals(0L, result);
    }

    @Test
    void toLong_returnsCorrectEpochMilli_forNonZeroInstant() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z in milliseconds
        long expected = 1_640_995_200_000L;
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(expected);
        // WHEN
        Long result = CalendarConversions.toLong(calendar, mockConverter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void toLong_throwsClassCastException_whenObjectIsNotCalendar() {
        // GIVEN
        Object notACalendar = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toLong(notACalendar, mockConverter));
    }

    @Test
    void toLong_throwsNullPointerException_whenCalendarIsNull() {
        // GIVEN
        Calendar nullCalendar = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLong(nullCalendar, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validCalendarProvider")
    void toLong_returnsExpectedEpochMilli(String description, Calendar calendar, Converter converter, long expected) {
        // WHEN
        Long result = CalendarConversions.toLong(calendar, converter);
        // THEN
        assertEquals(expected, result, "Epoch milliseconds did not match for: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullInputProvider")
    void toLong_throwsNullPointerException_forNullCalendar(String description, Object from, Converter converter) {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLong(from, converter), "Expected NullPointerException for: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidTypeProvider")
    void toLong_throwsClassCastException_forInvalidType(String description, Object from, Converter converter) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toLong(from, converter), "Expected ClassCastException for: " + description);
    }
}
