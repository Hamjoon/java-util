package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toAtomicLong_Test {

    @Mock
    private Converter // mock – not used by the method under test
    converter;

    /**
     * Helper to create a GregorianCalendar with the given fields.
     */
    private static GregorianCalendar buildCalendar(TimeZone tz, int year, int month, int day, int hour, int minute, int second, int milli) {
        GregorianCalendar cal = new GregorianCalendar(tz, Locale.US);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, milli);
        return cal;
    }

    static Stream<Arguments> validCalendarProvider() {
        return Stream.of(// 2023‑01‑01T00:00:00.000Z (UTC)
        arguments(buildCalendar(TimeZone.getTimeZone("UTC"), 2023, Calendar.JANUARY, 1, 0, 0, 0, 0), 1672531200000L, "UTC GregorianCalendar representing 2023‑01‑01T00:00:00.000Z"), // Same wall‑clock time in America/New_York (UTC‑05:00)
        arguments(buildCalendar(TimeZone.getTimeZone("America/New_York"), 2023, Calendar.JANUARY, 1, 0, 0, 0, 0), 1672552800000L, "New York GregorianCalendar 2023‑01‑01T00:00:00.000 (UTC‑05:00)"), // Unix epoch (0L)
        arguments(new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US) {

            {
                setTimeInMillis(0L);
            }
        }, 0L, "Calendar set to Unix epoch"), // One day before epoch (-86400000L)
        arguments(new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US) {

            {
                setTimeInMillis(-86400000L);
            }
        }, -86400000L, "Calendar one day before epoch"), // Arbitrary positive millis with a real Converter instance
        arguments(new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US) {

            {
                setTimeInMillis(123456789L);
            }
        }, 123456789L, "Calendar with 123456789L and non‑null Converter"));
    }

    static Stream<Arguments> invalidInputProvider() {
        return Stream.of(arguments(null, null, NullPointerException.class, "null source should trigger NPE"), arguments("not a calendar", null, ClassCastException.class, "non‑Calendar source should trigger ClassCastException"), arguments(new java.util.Date(), null, ClassCastException.class, "java.util.Date (not a Calendar) should trigger ClassCastException"));
    }

    static Stream<Arguments> edgeTimeZoneProvider() {
        return Stream.of(// Asia/Kolkata (UTC+05:30) – wall‑clock 2023‑01‑01T00:00:00 => 2022‑12‑31T18:30Z
        arguments(buildCalendar(TimeZone.getTimeZone("Asia/Kolkata"), 2023, Calendar.JANUARY, 1, 0, 0, 0, 0), 1672511400000L, "Asia/Kolkata UTC+05:30 offset"), // America/Los_Angeles PDT with sub‑second precision
        arguments(buildCalendar(TimeZone.getTimeZone("America/Los_Angeles"), 2023, Calendar.JULY, 15, 12, 30, 45, 250), 1689432645250L, "Los Angeles PDT with milliseconds"));
    }

    static Stream<Arguments> leapAndMillisProvider() {
        return Stream.of(// Leap day 2024‑02‑29 UTC
        arguments(buildCalendar(TimeZone.getTimeZone("UTC"), 2024, Calendar.FEBRUARY, 29, 0, 0, 0, 0), 1709164800000L, "Leap day 2024‑02‑29 UTC"), // Arbitrary date with non‑zero milliseconds
        arguments(buildCalendar(TimeZone.getTimeZone("UTC"), 2023, Calendar.MARCH, 10, 14, 22, 33, 987), 1678464153987L, "2023‑03‑10 14:22:33.987 UTC"));
    }

    static Stream<Arguments> extremeEpochProvider() {
        return Stream.of(arguments(new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US) {

            {
                setTimeInMillis(Long.MAX_VALUE);
            }
        }, Long.MAX_VALUE, "Calendar with Long.MAX_VALUE epoch"), arguments(new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US) {

            {
                setTimeInMillis(Long.MIN_VALUE);
            }
        }, Long.MIN_VALUE, "Calendar with Long.MIN_VALUE epoch"));
    }

    /**
     * Verify that a Calendar instance is correctly converted to an {@link AtomicLong}
     * containing the same epoch‑millisecond value as the Calendar's underlying {@link java.util.Date}.
     */
    @Test
    void toAtomicLong_returnsAtomicLongWithCalendarTime() {
        // GIVEN – a Calendar set to a known instant (2020‑01‑01T00:00:00.000Z)
        Calendar cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        // clear all fields to avoid leftover values
        cal.clear();
        cal.set(2020, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long expectedMillis = cal.getTimeInMillis();
        // WHEN – the conversion method is invoked
        AtomicLong result = CalendarConversions.toAtomicLong(cal, converter);
        // THEN – the AtomicLong contains the same epoch‑millis value
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(expectedMillis, result.get(), "AtomicLong value should match Calendar's time in milliseconds");
    }

    /**
     * Verify that passing {@code null} as the source object results in a {@link NullPointerException}.
     * The method performs a cast to {@link Calendar} and then dereferences it, so a null source
     * cannot be processed.
     */
    @Test
    void toAtomicLong_nullSource_throwsNullPointerException() {
        // GIVEN – a null source
        Object source = null;
        // WHEN / THEN – the method should throw NPE
        assertThrows(NullPointerException.class, () -> CalendarConversions.toAtomicLong(source, converter), "Calling toAtomicLong with null should throw NullPointerException");
    }

    /**
     * Verify that passing an object that is not a {@link Calendar} results in a {@link ClassCastException}.
     */
    @Test
    void toAtomicLong_nonCalendarSource_throwsClassCastException() {
        // GIVEN – an object of an unrelated type
        Object notACalendar = "I am not a calendar";
        // WHEN / THEN – the method should throw ClassCastException
        assertThrows(ClassCastException.class, () -> CalendarConversions.toAtomicLong(notACalendar, converter), "Calling toAtomicLong with a non‑Calendar object should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validCalendarProvider")
    void toAtomicLong_validCalendars(GregorianCalendar calendar, long expected, String description) {
        // The converter is deliberately passed as null for most cases, but we also test a real instance.
        Converter usedConverter = (calendar.getTimeInMillis() == 123456789L) ? new Converter() {
        } : null;
        AtomicLong result = CalendarConversions.toAtomicLong(calendar, usedConverter);
        assertNotNull(result, "Result should not be null");
        assertEquals(expected, result.get(), "AtomicLong value should match expected epoch milliseconds");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputProvider")
    void toAtomicLong_invalidInputs(Object source, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CalendarConversions.toAtomicLong(source, conv), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("edgeTimeZoneProvider")
    void toAtomicLong_timeZoneEdgeCases(GregorianCalendar calendar, long expected, String description) {
        AtomicLong result = CalendarConversions.toAtomicLong(calendar, null);
        assertEquals(expected, result.get(), "Epoch millis for time‑zone edge case should match expected value");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("leapAndMillisProvider")
    void toAtomicLong_leapYearAndMillis(GregorianCalendar calendar, long expected, String description) {
        AtomicLong result = CalendarConversions.toAtomicLong(calendar, null);
        assertEquals(expected, result.get(), "Epoch millis for leap‑year / millisecond case should match expected");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("extremeEpochProvider")
    void toAtomicLong_extremeEpochValues(GregorianCalendar calendar, long expected, String description) {
        AtomicLong result = CalendarConversions.toAtomicLong(calendar, null);
        assertEquals(expected, result.get(), "Extreme epoch value should be preserved exactly");
    }

    @Test
    void toAtomicLong_converterIsNeverInvoked() {
        // Calendar with a known instant
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        calendar.setTimeInMillis(123456789L);
        // Converter that throws if its convert method is called
        Converter hostileConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // The method should ignore the converter and succeed
        AtomicLong result = CalendarConversions.toAtomicLong(calendar, hostileConverter);
        assertEquals(123456789L, result.get(), "Result must match calendar time even when converter throws on use");
    }

    @Test
    void toAtomicLong_returnsSnapshot_notLiveView() {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        calendar.setTimeInMillis(5000L);
        AtomicLong snapshot = CalendarConversions.toAtomicLong(calendar, null);
        assertEquals(5000L, snapshot.get(), "Initial snapshot should reflect the original millis");
        // Mutate the original calendar after conversion
        calendar.setTimeInMillis(9999L);
        assertEquals(5000L, snapshot.get(), "AtomicLong must remain unchanged after the source Calendar is mutated");
    }
}
