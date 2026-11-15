package com.cedarsoftware.util.convert;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.GregorianCalendar;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toDate_Test {

    /**
     * Converter instance – required by the signature but not used by the method.
     */
    private static Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5/‑4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    /**
     * A {@link GregorianCalendar} that overrides {@code getTime()} to throw
     * {@link UnsupportedOperationException}. Used to verify that {@code toDate}
     * propagates runtime exceptions from the underlying calendar.
     */
    private static class CustomCalendarThrowingUnsupportedOperationException extends GregorianCalendar {

        @Override
        public Date getTime() {
            throw new UnsupportedOperationException("Intentional exception for testing");
        }
    }

    /**
     * A {@link GregorianCalendar} that overrides {@code getTime()} to return {@code null}.
     * This tests the method's behaviour when the underlying calendar returns {@code null}
     * instead of a {@link Date} instance.
     */
    private static class CustomCalendarReturningNull extends GregorianCalendar {

        @Override
        public Date getTime() {
            return null;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not influence CalendarConversions.toDate, but we need a non‑null instance.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Calendar} with the supplied fields.
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar months are zero‑based.
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Standard GregorianCalendar (UTC)
        Arguments.of("Standard case – GregorianCalendar UTC", createCalendar(2023, 1, 15, 10, 20, 30, 0, TimeZone.getTimeZone("UTC")), createCalendar(2023, 1, 15, 10, 20, 30, 0, TimeZone.getTimeZone("UTC")).getTime()), // Non‑UTC timezone (America/New_York)
        Arguments.of("Non‑UTC timezone – America/New_York", createCalendar(2022, 12, 31, 23, 59, 59, 0, NEW_YORK), createCalendar(2022, 12, 31, 23, 59, 59, 0, NEW_YORK).getTime()), // Epoch start (1970‑01‑01T00:00:00Z)
        Arguments.of("Epoch start – 0 ms", createCalendar(1970, 1, 1, 0, 0, 0, 0, TimeZone.getTimeZone("UTC")), new Date(0L)), // Leap day (2024‑02‑29)
        Arguments.of("Leap day – 2024‑02‑29", createCalendar(2024, 2, 29, 12, 0, 0, 0, TimeZone.getTimeZone("UTC")), createCalendar(2024, 2, 29, 12, 0, 0, 0, TimeZone.getTimeZone("UTC")).getTime()), // 2038‑01‑19 03:14:07 (just before 32‑bit overflow)
        Arguments.of("Just before 32‑bit overflow – 2038‑01‑19 03:14:07", createCalendar(2038, 1, 19, 3, 14, 7, 0, TimeZone.getTimeZone("UTC")), createCalendar(2038, 1, 19, 3, 14, 7, 0, TimeZone.getTimeZone("UTC")).getTime()), // One second before epoch (1969‑12‑31 23:59:59Z)
        Arguments.of("Negative epoch – 1969‑12‑31 23:59:59", createCalendar(1969, 12, 31, 23, 59, 59, 0, TimeZone.getTimeZone("UTC")), createCalendar(1969, 12, 31, 23, 59, 59, 0, TimeZone.getTimeZone("UTC")).getTime()), // DST forward gap (Europe/Paris 2023‑03‑26 02:30 does not exist; Calendar rolls forward)
        Arguments.of("DST forward gap – Europe/Paris 2023‑03‑26 02:30", createCalendar(2023, 3, 26, 2, 30, 0, 0, PARIS), createCalendar(2023, 3, 26, 2, 30, 0, 0, PARIS).getTime()), // DST backward overlap (Europe/Paris 2023‑10‑29 02:30 – first occurrence)
        Arguments.of("DST backward overlap – Europe/Paris 2023‑10‑29 02:30", createCalendar(2023, 10, 29, 2, 30, 0, 0, PARIS), createCalendar(2023, 10, 29, 2, 30, 0, 0, PARIS).getTime()), // Calendar holding Long.MAX_VALUE (max representable Date)
        Arguments.of("Maximum long value – Long.MAX_VALUE", new GregorianCalendar() {

            {
                setTimeInMillis(Long.MAX_VALUE);
            }
        }, new Date(Long.MAX_VALUE)));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// Non‑lenient Calendar with impossible date 2023‑02‑30
        Arguments.of("Non‑lenient invalid date – 2023‑02‑30", createNonLenientCalendar(2023, 2, 30, 0, 0, 0, 0, TimeZone.getTimeZone("UTC")), IllegalArgumentException.class), // Calendar with month = 0 (invalid) and lenient=false
        Arguments.of("Non‑lenient invalid month – month=0", createNonLenientCalendar(2023, 0, 15, 0, 0, 0, 0, TimeZone.getTimeZone("UTC")), IllegalArgumentException.class), // Custom Calendar that throws UnsupportedOperationException from getTime()
        Arguments.of("Custom Calendar throws UnsupportedOperationException", new CustomCalendarThrowingUnsupportedOperationException(), UnsupportedOperationException.class), // Custom Calendar that returns null from getTime()
        Arguments.of("Custom Calendar returns null", new CustomCalendarReturningNull(), // null indicates we expect a null result, not an exception
        null));
    }

    /**
     * Helper that creates a non‑lenient {@link Calendar} with the supplied fields.
     */
    private static Calendar createNonLenientCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.setLenient(false);
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    @Test
    void testToDateWithVariousTimeZones() {
        // Tokyo – UTC+9
        Calendar tokyoCal = createCalendar(2023, 6, 15, 12, 30, 45, 123, TOKYO);
        Date expectedTokyo = tokyoCal.getTime();
        assertEquals(expectedTokyo, CalendarConversions.toDate(tokyoCal, converter));
        // Paris – UTC+1 (standard) / UTC+2 (daylight)
        Calendar parisCal = createCalendar(2023, 12, 25, 23, 59, 59, 999, PARIS);
        Date expectedParis = parisCal.getTime();
        assertEquals(expectedParis, CalendarConversions.toDate(parisCal, converter));
        // New York – UTC‑5 (standard) / UTC‑4 (daylight)
        Calendar nyCal = createCalendar(1970, 1, 1, 0, 0, 0, 0, NEW_YORK);
        Date expectedNy = nyCal.getTime();
        assertEquals(expectedNy, CalendarConversions.toDate(nyCal, converter));
    }

    @Test
    void testToDateMatchesCalendarGetTimeExactly() {
        Calendar cal = createCalendar(1999, 12, 31, 23, 59, 59, 999, TimeZone.getTimeZone("UTC"));
        Date dateFromMethod = CalendarConversions.toDate(cal, converter);
        Date dateDirect = cal.getTime();
        // The two Date objects must represent the same instant and be equal.
        assertEquals(dateDirect, dateFromMethod);
        assertEquals(dateDirect.getTime(), dateFromMethod.getTime());
    }

    @Test
    void testToDateWithNullCalendarThrowsException() {
        // The method casts the argument to Calendar, so a null should cause a NullPointerException
        // when the cast is attempted (as per Java's behavior).
        assertThrows(NullPointerException.class, () -> CalendarConversions.toDate(null, converter));
    }

    @Test
    void testToDateWithNonCalendarObjectThrowsClassCastException() {
        Object notACalendar = "I am not a Calendar";
        assertThrows(ClassCastException.class, () -> CalendarConversions.toDate(notACalendar, converter));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("successfulConversionProvider")
    void testToDateSuccessfulConversions(String description, Calendar calendar, Date expected) {
        Date actual = CalendarConversions.toDate(calendar, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionConversionProvider")
    void testToDateExceptionScenarios(String description, Calendar calendar, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CalendarConversions.toDate(calendar, converter), description);
    }

    /**
     * Test for the scenario where a custom Calendar returns {@code null} from {@code getTime()}.
     * The method under test should simply propagate the {@code null} result.
     */
    @Test
    void testCustomCalendarReturningNull() {
        Calendar custom = new CustomCalendarReturningNull();
        Date result = CalendarConversions.toDate(custom, converter);
        assertNull(result, "Expected null when Calendar#getTime() returns null");
    }
}
