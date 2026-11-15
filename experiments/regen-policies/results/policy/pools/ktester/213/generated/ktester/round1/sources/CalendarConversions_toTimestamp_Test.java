package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.GregorianCalendar;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toTimestamp_Test {

    /**
     * Converter instance – not used by the method but required by the signature.
     */
    private static Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5/‑4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    /**
     * Common time‑zones used in the tests.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // UTC+5:30
    private static final TimeZone KOLKATA = TimeZone.getTimeZone("Asia/Kolkata");

    /**
     * Simple subclass of {@link GregorianCalendar} that does not add any behaviour.
     * Used to verify that subclasses are accepted.
     */
    private static class MyCalendar extends GregorianCalendar {

        MyCalendar(TimeZone tz) {
            super(tz);
        }
    }

    /**
     * Calendar implementation that deliberately throws from {@code getTimeInMillis()}.
     */
    private static class ThrowingCalendar extends Calendar {

        @Override
        public long getTimeInMillis() {
            throw new RuntimeException("forced");
        }

        // The remaining abstract methods are stubbed because they are never called
        // by {@code CalendarConversions.toTimestamp}.
        @Override
        public void setTimeInMillis(long millis) {
            /* no‑op */
        }

        @Override
        public void add(int field, int amount) {
            /* no‑op */
        }

        @Override
        public void roll(int field, boolean up) {
            /* no‑op */
        }

        @Override
        public int getMinimum(int field) {
            return 0;
        }

        @Override
        public int getMaximum(int field) {
            return 0;
        }

        @Override
        public int getGreatestMinimum(int field) {
            return 0;
        }

        @Override
        public int getLeastMaximum(int field) {
            return 0;
        }

        @Override
        public void set(int year, int month, int date) {
            /* no‑op */
        }

        @Override
        public void set(int year, int month, int date, int hourOfDay, int minute) {
            /* no‑op */
        }

        @Override
        public void set(int year, int month, int date, int hourOfDay, int minute, int second) {
            /* no‑op */
        }

        @Override
        public void clear() {
            /* no‑op */
        }

        @Override
        public void clear(int field) {
            /* no‑op */
        }

        @Override
        public boolean isSet(int field) {
            return false;
        }

        @Override
        public int get(int field) {
            return 0;
        }

        @Override
        public void setTime(java.util.Date date) {
            /* no‑op */
        }

        @Override
        public java.util.Date getTime() {
            return null;
        }

        @Override
        public void setLenient(boolean lenient) {
            /* no‑op */
        }

        @Override
        public boolean isLenient() {
            return true;
        }

        @Override
        public void setTimeZone(TimeZone value) {
            /* no‑op */
        }

        @Override
        public TimeZone getTimeZone() {
            return null;
        }

        @Override
        public int getActualMaximum(int field) {
            return 0;
        }

        @Override
        public int getActualMinimum(int field) {
            return 0;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not influence the toTimestamp implementation,
        // but we create a real instance to keep the API contract intact.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Calendar} with the supplied fields.
     * The {@code month} argument follows the conventional 1‑based numbering
     * (January = 1) – the method adjusts it for {@link Calendar}'s 0‑based month.
     */
    private Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    @Test
    void testToTimestampMatchesCalendarMillis_Tokyo() {
        Calendar cal = createCalendar(2023, 6, 15, 12, 30, 45, 123, TOKYO);
        Timestamp ts = CalendarConversions.toTimestamp(cal, converter);
        Assertions.assertEquals(cal.getTimeInMillis(), ts.getTime(), "Timestamp should contain the same epoch millis as the source Calendar (Tokyo)");
    }

    @Test
    void testToTimestampMatchesCalendarMillis_Paris() {
        Calendar cal = createCalendar(1970, 1, 1, 0, 0, 0, 0, PARIS);
        Timestamp ts = CalendarConversions.toTimestamp(cal, converter);
        Assertions.assertEquals(cal.getTimeInMillis(), ts.getTime(), "Timestamp should contain the same epoch millis as the source Calendar (Paris)");
    }

    @Test
    void testToTimestampMatchesCalendarMillis_NewYork() {
        Calendar cal = createCalendar(1999, 12, 31, 23, 59, 59, 999, NEW_YORK);
        Timestamp ts = CalendarConversions.toTimestamp(cal, converter);
        Assertions.assertEquals(cal.getTimeInMillis(), ts.getTime(), "Timestamp should contain the same epoch millis as the source Calendar (New York)");
    }

    @Test
    void testToTimestampWithNullCalendar() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            CalendarConversions.toTimestamp(null, converter);
        }, "Passing a null source should result in a NullPointerException");
    }

    @Test
    void testToTimestampWithNonCalendarObject() {
        Object notACalendar = "I am not a calendar";
        Assertions.assertThrows(ClassCastException.class, () -> {
            CalendarConversions.toTimestamp(notACalendar, converter);
        }, "Passing a non‑Calendar object should cause a ClassCastException");
    }

    /**
     * Valid Calendar conversion – UTC GregorianCalendar.
     */
    @Test
    void testValidConversion_UtcGregorianCalendar() {
        Calendar cal = createCalendar(2023, 1, 1, 0, 0, 0, 0, UTC);
        Timestamp ts = CalendarConversions.toTimestamp(cal, null);
        Assertions.assertEquals(1672531200000L, ts.getTime(), "Timestamp epoch millis should match 2023‑01‑01T00:00:00Z");
    }

    /**
     * Valid Calendar conversion – New York time‑zone, local midnight.
     * The method must honour the Calendar's internal offset.
     */
    @Test
    void testValidConversion_NewYorkLocalMidnight() {
        Calendar cal = createCalendar(2023, 1, 1, 0, 0, 0, 0, NEW_YORK);
        Timestamp ts = CalendarConversions.toTimestamp(cal, null);
        Assertions.assertEquals(1672552800000L, ts.getTime(), "Timestamp should represent 2023‑01‑01 05:00:00 UTC");
    }

    /**
     * Valid Calendar conversion – epoch start (1970‑01‑01 UTC).
     */
    @Test
    void testValidConversion_EpochStart() {
        Calendar cal = createCalendar(1970, 1, 1, 0, 0, 0, 0, UTC);
        Timestamp ts = CalendarConversions.toTimestamp(cal, new Converter(new DefaultConverterOptions()));
        Assertions.assertEquals(0L, ts.getTime(), "Timestamp for epoch start must be zero");
    }

    /**
     * Pre‑epoch conversion – a date before 1970.
     */
    @Test
    void testPreEpochConversion() {
        Calendar cal = createCalendar(1965, 12, 31, 23, 59, 59, 0, UTC);
        Timestamp ts = CalendarConversions.toTimestamp(cal, null);
        Assertions.assertEquals(-157766401000L, ts.getTime(), "Timestamp should correctly represent a pre‑epoch instant");
    }

    /**
     * Non‑UTC timezone with fractional hour offset (Asia/Kolkata, UTC+05:30).
     */
    @Test
    void testFractionalHourOffset() {
        Calendar cal = createCalendar(2023, 3, 15, 12, 34, 56, 789, KOLKATA);
        Timestamp ts = CalendarConversions.toTimestamp(cal, null);
        Assertions.assertEquals(1678866296789L, ts.getTime(), "Timestamp must reflect the 5:30 offset and preserve milliseconds");
    }

    /**
     * Millisecond precision preservation.
     */
    @Test
    void testMillisecondPrecision() {
        Calendar cal = createCalendar(2022, 11, 5, 8, 15, 30, 123, UTC);
        Timestamp ts = CalendarConversions.toTimestamp(cal, null);
        Assertions.assertEquals(1667646930123L, ts.getTime(), "Timestamp should retain the 123‑millisecond component");
    }

    /**
     * DST spring‑forward edge case (America/New_York, 2023‑03‑12 02:30 does not exist).
     * Calendar normalises the time to 03:30 EDT (UTC‑4), i.e. 07:30 UTC.
     */
    @Test
    void testDstSpringForwardEdgeCase() {
        Calendar cal = createCalendar(2023, 3, 12, 2, 30, 0, 0, NEW_YORK);
        Timestamp ts = CalendarConversions.toTimestamp(cal, null);
        Assertions.assertEquals(1678621800000L, ts.getTime(), "Timestamp should correspond to the normalised instant (07:30 UTC)");
    }

    /**
     * Non‑Calendar subclass handling – a custom subclass of {@link GregorianCalendar}.
     */
    @Test
    void testSubclassCalendarConversion() {
        Calendar cal = new MyCalendar(UTC);
        cal.clear();
        cal.set(2024, Calendar.JULY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Timestamp ts = CalendarConversions.toTimestamp(cal, null);
        Assertions.assertEquals(1725148800000L, ts.getTime(), "Subclass of Calendar should be treated like any other Calendar");
    }

    /**
     * Invalid date fields with lenient set to false – should raise {@link IllegalArgumentException}.
     */
    @Test
    void testInvalidDateLenientFalse() {
        Calendar cal = new GregorianCalendar(UTC);
        cal.setLenient(false);
        // February 30th is invalid.
        cal.clear();
        cal.set(2023, Calendar.FEBRUARY, 30);
        Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toTimestamp(cal, null), "Non‑lenient Calendar with impossible date must propagate IllegalArgumentException");
    }

    /**
     * Calendar subclass that throws in {@code getTimeInMillis()} – the exception must propagate.
     */
    @Test
    void testThrowingCalendarSubclass() {
        Calendar cal = new ThrowingCalendar();
        Assertions.assertThrows(RuntimeException.class, () -> CalendarConversions.toTimestamp(cal, null), "RuntimeException from getTimeInMillis should not be swallowed");
    }

    /**
     * Calendar whose internal {@code TimeZone} field has been set to {@code null} via reflection.
     * Expected to throw {@link NullPointerException} when {@code getTimeInMillis()} accesses it.
     */
    @Test
    void testCalendarWithNullTimeZone() throws Exception {
        Calendar cal = new GregorianCalendar(UTC);
        // Use reflection to nullify the private 'timeZone' field.
        Field tzField = Calendar.class.getDeclaredField("timeZone");
        tzField.setAccessible(true);
        tzField.set(cal, null);
        Assertions.assertThrows(NullPointerException.class, () -> CalendarConversions.toTimestamp(cal, null), "Null internal TimeZone should cause NullPointerException");
    }
}
