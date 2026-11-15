package com.cedarsoftware.util.convert;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toZonedDateTime_Test {

    /**
     * Converter is required by the signature of the method under test, even though the
     *  implementation does not currently use it.  Keeping it realistic makes the test
     *  future‑proof if the implementation ever starts to depend on the converter.
     */
    private final Converter converter = new Converter(new DefaultConverterOptions());

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1 / UTC+2 (DST)
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5 / UTC‑4 (DST)
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    /**
     * Calendar whose getTimeZone() returns null – triggers NPE when toZoneId() is called.
     */
    private static final class CalendarWithNullTimeZone extends GregorianCalendar {

        @Override
        public TimeZone getTimeZone() {
            return null;
        }
    }

    /**
     * Calendar that throws a DateTimeException from toInstant().
     */
    private static final class CalendarThrowingInstant extends GregorianCalendar {

        @Override
        public Instant toInstant() {
            throw new DateTimeException("forced instant exception");
        }
    }

    /**
     * Calendar that throws a generic RuntimeException from toInstant().
     */
    private static final class CalendarThrowingRuntime extends GregorianCalendar {

        @Override
        public Instant toInstant() {
            throw new RuntimeException("boom");
        }
    }

    /**
     * TimeZone whose toZoneId() throws DateTimeException.
     */
    private static final class BadTimeZone extends TimeZone {

        @Override
        public String getID() {
            return "BadTZ";
        }

        @Override
        public int getRawOffset() {
            return 0;
        }

        @Override
        public void setRawOffset(int offsetMillis) {
        }

        @Override
        public boolean useDaylightTime() {
            return false;
        }

        @Override
        public boolean inDaylightTime(Date date) {
            return false;
        }

        @Override
        public ZoneId toZoneId() {
            throw new DateTimeException("bad zone");
        }
    }

    /**
     * Calendar that returns the above BadTimeZone.
     */
    private static final class CalendarWithBadTimeZone extends GregorianCalendar {

        @Override
        public TimeZone getTimeZone() {
            return new BadTimeZone();
        }
    }

    /**
     * Helper that creates a {@link Calendar} with the supplied fields and clears any
     *  pre‑existing data (so the millisecond field is the only sub‑second component).
     */
    private Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar months are zero‑based
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Simple factory that builds a GregorianCalendar from an {@link Instant} and a {@link TimeZone}.
     */
    private static Calendar calendarFrom(Instant instant, TimeZone tz) {
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTime(Date.from(instant));
        return cal;
    }

    private static Stream<Arguments> validCalendarProvider() {
        return Stream.of(// Epoch UTC
        Arguments.of(calendarFrom(Instant.parse("1970-01-01T00:00:00Z"), TimeZone.getTimeZone("UTC")), "1970-01-01T00:00Z[UTC]", "Convert a GregorianCalendar set to the epoch (0 ms) with UTC time‑zone."), // New York winter (no DST)
        Arguments.of(calendarFrom(Instant.parse("2022-01-15T12:30:45-05:00"), TimeZone.getTimeZone("America/New_York")), "2022-01-15T12:30:45-05:00[America/New_York]", "Convert a GregorianCalendar created with the America/New_York zone and a winter date."), // Tokyo offset +09:00
        Arguments.of(calendarFrom(Instant.parse("2021-01-01T00:00:00Z"), TimeZone.getTimeZone("Asia/Tokyo")), "2021-01-01T09:00+09:00[Asia/Tokyo]", "Convert a GregorianCalendar whose underlying instant is 2021‑01‑01T00:00:00Z but zone Asia/Tokyo."), // Paris summer (DST +02:00)
        Arguments.of(calendarFrom(Instant.parse("2023-07-01T13:45:30Z"), TimeZone.getTimeZone("Europe/Paris")), "2023-07-01T15:45:30+02:00[Europe/Paris]", "Convert a GregorianCalendar set to a summer date in Europe/Paris (DST active)."));
    }

    @Test
    void testToZonedDateTime_Tokyo() {
        Calendar cal = createCalendar(2023, 6, 15, 12, 30, 45, 123, TOKYO);
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(cal.toInstant(), zdt.toInstant(), "Instant should be identical to the Calendar's instant");
        assertEquals(cal.getTimeZone().toZoneId(), zdt.getZone(), "ZoneId should match the Calendar's TimeZone");
    }

    @Test
    void testToZonedDateTime_Paris() {
        Calendar cal = createCalendar(1969, 12, 31, 23, 59, 59, 999, PARIS);
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(cal.toInstant(), zdt.toInstant(), "Instant should be identical to the Calendar's instant");
        assertEquals(cal.getTimeZone().toZoneId(), zdt.getZone(), "ZoneId should match the Calendar's TimeZone");
    }

    @Test
    void testToZonedDateTime_NewYork_DaylightSavingTransition() {
        // 2021‑03‑14 02:30 does not exist in New York (DST forward). Calendar rolls it to 03:30.
        Calendar cal = createCalendar(2021, 3, 14, 2, 30, 0, 0, NEW_YORK);
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(cal.toInstant(), zdt.toInstant(), "Instant should reflect Calendar's adjusted time after DST transition");
        assertEquals(cal.getTimeZone().toZoneId(), zdt.getZone(), "ZoneId should match the Calendar's TimeZone");
    }

    @Test
    void testToZonedDateTime_EpochZero() {
        Calendar cal = createCalendar(1970, 1, 1, 0, 0, 0, 0, TimeZone.getTimeZone("UTC"));
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(cal.toInstant(), zdt.toInstant(), "Instant for the epoch should be exactly 1970‑01‑01T00:00:00Z");
        assertEquals(cal.getTimeZone().toZoneId(), zdt.getZone(), "ZoneId should be UTC for the epoch test");
    }

    @Test
    void testToZonedDateTime_MillisecondPrecision() {
        Calendar cal = createCalendar(2000, 2, 29, 23, 59, 59, 999, TOKYO);
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(cal.get(Calendar.MILLISECOND), zdt.getNano() / 1_000_000, "Millisecond component should be preserved in the ZonedDateTime");
        assertEquals(cal.toInstant(), zdt.toInstant(), "Full instant (including milliseconds) must match");
    }

    /**
     * Valid Calendar conversions – covers the first group in the JSON.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validCalendarProvider")
    void testValidCalendarConversions(Calendar cal, String expectedIso, String description) {
        ZonedDateTime expected = ZonedDateTime.parse(expectedIso);
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, actual, description);
    }

    /**
     * Historical dates – before Unix epoch.
     */
    @Test
    void testHistoricalDate_BeforeEpoch() {
        Calendar cal = calendarFrom(Instant.parse("1965-07-20T12:34:56Z"), TimeZone.getTimeZone("UTC"));
        ZonedDateTime expected = ZonedDateTime.parse("1965-07-20T12:34:56Z[UTC]");
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    /**
     * Future dates – far‑future year 2100.
     */
    @Test
    void testFutureDate_Year2100() {
        Calendar cal = calendarFrom(Instant.parse("2100-01-01T00:00:00Z"), TimeZone.getTimeZone("UTC"));
        ZonedDateTime expected = ZonedDateTime.parse("2100-01-01T00:00Z[UTC]");
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    /**
     * DST transition – ambiguous hour (fall back).
     */
    @Test
    void testDstTransition_AmbiguousHour() {
        // 2022‑11‑06 01:30‑04:00 is the first occurrence before the fall‑back
        Calendar cal = calendarFrom(Instant.parse("2022-11-06T05:30:00Z"), TimeZone.getTimeZone("America/New_York"));
        ZonedDateTime expected = ZonedDateTime.parse("2022-11-06T01:30:00-04:00[America/New_York]");
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    /**
     * DST transition – spring forward non‑existent local time.
     */
    @Test
    void testDstTransition_NonExistentTime() {
        // Input description expects the Calendar to roll forward to 03:30‑04:00
        Calendar cal = calendarFrom(Instant.parse("2022-03-13T07:30:00Z"), TimeZone.getTimeZone("America/New_York"));
        ZonedDateTime expected = ZonedDateTime.parse("2022-03-13T03:30:00-04:00[America/New_York]");
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    /**
     * Custom GMT offset TimeZone (GMT+05:30).
     */
    @Test
    void testCustomTimeZone_GMTPlus0530() {
        Calendar cal = calendarFrom(Instant.parse("2022-08-15T00:00:00+05:30"), TimeZone.getTimeZone("GMT+05:30"));
        ZonedDateTime expected = ZonedDateTime.parse("2022-08-15T00:00+05:30[GMT+05:30]");
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    /**
     * Converter argument is ignored – we pass a dummy instance.
     */
    @Test
    void testConverterIgnored() {
        Calendar cal = calendarFrom(Instant.parse("1975-12-31T23:59:59Z"), TimeZone.getTimeZone("UTC"));
        // Use a different Converter instance to prove it is ignored
        Converter dummy = new Converter(new DefaultConverterOptions());
        ZonedDateTime expected = ZonedDateTime.parse("1975-12-31T23:59:59Z[UTC]");
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(cal, dummy);
        assertEquals(expected, actual);
    }

    /**
     * Post‑set TimeZone – change zone after Calendar creation.
     */
    @Test
    void testPostSetTimeZone() {
        Calendar cal = calendarFrom(Instant.parse("1970-01-01T00:00:00Z"), TimeZone.getTimeZone("UTC"));
        cal.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        ZonedDateTime expected = ZonedDateTime.parse("1970-01-01T05:30+05:30[Asia/Kolkata]");
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    /**
     * Millisecond precision – ensure sub‑second part is retained.
     */
    @Test
    void testMillisecondPrecision() {
        Calendar cal = calendarFrom(Instant.parse("2022-03-01T12:34:56.789Z"), TimeZone.getTimeZone("UTC"));
        ZonedDateTime expected = ZonedDateTime.parse("2022-03-01T12:34:56.789Z[UTC]");
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void testNullFromArgument() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toZonedDateTime(null, null), "Passing null 'from' should throw NullPointerException");
    }

    @Test
    void testNonCalendarFromArgument() {
        assertThrows(ClassCastException.class, () -> CalendarConversions.toZonedDateTime("I am not a Calendar", null), "Passing a non‑Calendar object should throw ClassCastException");
    }

    @Test
    void testNullTimeZonePropagation() {
        Calendar cal = new CalendarWithNullTimeZone();
        assertThrows(NullPointerException.class, () -> CalendarConversions.toZonedDateTime(cal, null), "Null TimeZone should cause NullPointerException");
    }

    @Test
    void testCalendarToInstantDateTimeExceptionPropagation() {
        Calendar cal = new CalendarThrowingInstant();
        assertThrows(DateTimeException.class, () -> CalendarConversions.toZonedDateTime(cal, null), "DateTimeException from toInstant() must propagate");
    }

    @Test
    void testCalendarToInstantRuntimeExceptionPropagation() {
        Calendar cal = new CalendarThrowingRuntime();
        assertThrows(RuntimeException.class, () -> CalendarConversions.toZonedDateTime(cal, null), "RuntimeException from toInstant() must propagate");
    }

    @Test
    void testTimeZoneToZoneIdExceptionPropagation() {
        Calendar cal = new CalendarWithBadTimeZone();
        assertThrows(DateTimeException.class, () -> CalendarConversions.toZonedDateTime(cal, null), "DateTimeException from TimeZone.toZoneId() must propagate");
    }
}
