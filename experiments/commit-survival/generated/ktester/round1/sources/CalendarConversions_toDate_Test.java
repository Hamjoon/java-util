package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toDate_Test {

    @Mock
    private Converter // Converter is not used by the method, a mock is sufficient
    mockConverter;

    /**
     * Supplies the arguments for {@link #testToDate_exceptions(Object, Converter, Class)}.
     *
     * <p>Each {@link Arguments} row corresponds to a test case described in the JSON:
     *
     * <ul>
     *   <li>Both parameters {@code null} → {@link NullPointerException}</li>
     *   <li>Non‑Calendar {@code from} (String) → {@link ClassCastException}</li>
     * </ul>
     */
    private static Stream<Arguments> exceptionTestProvider() {
        return Stream.of(// from = null, converter = null -> NullPointerException (cast succeeds, getTime() on null)
        Arguments.of(null, null, NullPointerException.class), // from = non‑Calendar (String), converter = null -> ClassCastException
        Arguments.of("2021-01-01", null, ClassCastException.class));
    }

    @Test
    @DisplayName("toDate returns the same Date as Calendar#getTime()")
    void testToDateReturnsCorrectDate() {
        // given: a GregorianCalendar set to a known instant
        Calendar calendar = new GregorianCalendar();
        calendar.set(2023, Calendar.JANUARY, 15, 10, 30, 45);
        calendar.set(Calendar.MILLISECOND, 123);
        Date expected = calendar.getTime();
        // when: converting the calendar to a Date
        Date actual = CalendarConversions.toDate(calendar, mockConverter);
        // then: the result must be exactly the Calendar's time
        assertEquals(expected, actual, "The Date returned by toDate should match Calendar#getTime()");
    }

    @Test
    @DisplayName("toDate throws ClassCastException when source is not a Calendar")
    void testToDateWithNonCalendarThrowsClassCastException() {
        // given
        Object notACalendar = "I am not a Calendar";
        // when / then
        assertThrows(ClassCastException.class, () -> CalendarConversions.toDate(notACalendar, mockConverter), "Passing a non‑Calendar object should result in ClassCastException");
    }

    @Test
    @DisplayName("toDate throws NullPointerException when source is null")
    void testToDateWithNullThrowsNullPointerException() {
        // given
        Object nullObject = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CalendarConversions.toDate(nullObject, mockConverter), "Passing null should result in NullPointerException due to cast then getTime()");
    }

    @Test
    @DisplayName("toDate works even when the converter argument is null")
    void testToDateWithNullConverter() {
        // given: a valid Calendar and a null converter
        Calendar calendar = new GregorianCalendar();
        // arbitrary epoch milli
        calendar.setTimeInMillis(1_600_000_000_000L);
        Date expected = calendar.getTime();
        // when: invoking toDate with a null converter
        Date actual = CalendarConversions.toDate(calendar, null);
        // then: conversion should succeed because the converter is not used
        assertEquals(expected, actual, "Converter being null must not affect conversion");
    }

    /* -----------------------------------------------------------------------
     * Parameterised exception tests – derived from the JSON test case group
     * ----------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, converter={1}, expected={2}")
    @MethodSource("exceptionTestProvider")
    @DisplayName("toDate exception scenarios")
    void testToDate_exceptions(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then: the expected exception must be thrown
        assertThrows(expectedException, () -> CalendarConversions.toDate(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for from=" + from);
    }

    @Test
    @DisplayName("toDate throws NullPointerException when source is null (converter non‑null)")
    void testToDateWithNullFromThrowsNullPointerException() {
        // given
        Object nullObject = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CalendarConversions.toDate(nullObject, mockConverter), "Passing null 'from' should result in NullPointerException due to cast then getTime()");
    }
}
