package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toDate_Test {

    // not used by the method but required
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter constructor requires a ConverterOptions instance.
        // Using the default options is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Parameter source for valid {@link Calendar} objects.
     * The first argument is the Calendar instance,
     * the second argument indicates whether the test should pass {@code null}
     * as the {@link Converter} argument.
     */
    private static Stream<Arguments> validCalendarProvider() {
        // Fixed GregorianCalendar: 2023‑01‑01T12:00:00.000
        Calendar fixed = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        fixed.set(2023, Calendar.JANUARY, 1, 12, 0, 0);
        fixed.set(Calendar.MILLISECOND, 0);
        // Calendar obtained via getInstance() – current time.
        Calendar runtime = Calendar.getInstance();
        return Stream.of(// Use the real converter (non‑null)
        Arguments.of(fixed, false), // Use a null converter – method should ignore it
        Arguments.of(fixed, true), // Runtime calendar with real converter
        Arguments.of(runtime, false));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null source object – cast to Calendar triggers NPE
        Arguments.of(null, NullPointerException.class), // Non‑Calendar object (String)
        Arguments.of("2023-01-01", ClassCastException.class));
    }

    private static Stream<Arguments> nonCalendarProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(42)), Arguments.of(new Date()), Arguments.of(Timestamp.valueOf("2023-01-01 12:00:00")), Arguments.of(new Object()));
    }

    @Test
    void toDate_returnsCalendarTime() {
        // GIVEN
        Calendar cal = new GregorianCalendar();
        cal.set(2023, Calendar.MARCH, 15, 10, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        Date expected = cal.getTime();
        // WHEN
        Date actual = CalendarConversions.toDate(cal, converter);
        // THEN
        assertEquals(expected, actual, "The Date returned should match Calendar#getTime()");
    }

    @Test
    void toDate_nullCalendar_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toDate(nullObj, converter), "Calling toDate with a null source should throw NPE");
    }

    @Test
    void toDate_nonCalendarObject_throwsClassCastException() {
        // GIVEN
        Object notACalendar = "I am not a Calendar";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toDate(notACalendar, converter), "Calling toDate with a non‑Calendar object should throw ClassCastException");
    }

    @ParameterizedTest(name = "validCalendarProvider[{index}] – useNullConverter={1}")
    @MethodSource("validCalendarProvider")
    void toDate_withValidCalendars_returnsMatchingDate(Calendar calendar, boolean useNullConverter) {
        // GIVEN
        Date expected = calendar.getTime();
        // WHEN
        Date actual = CalendarConversions.toDate(calendar, useNullConverter ? null : converter);
        // THEN
        assertEquals(expected, actual, "The Date returned must be exactly the one produced by Calendar#getTime()");
    }

    @ParameterizedTest(name = "invalidInputProvider[{index}] – expects {1}")
    @MethodSource("invalidInputProvider")
    void toDate_invalidInputs_throwException(Object source, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CalendarConversions.toDate(source, converter), "Calling toDate with invalid input should throw the expected exception");
    }

    @ParameterizedTest(name = "nonCalendarProvider[{index}] – source type: {0}")
    @MethodSource("nonCalendarProvider")
    void toDate_nonCalendarTypes_throwClassCastException(Object nonCalendar) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toDate(nonCalendar, converter), "Passing a non‑Calendar object should cause a ClassCastException");
    }
}
