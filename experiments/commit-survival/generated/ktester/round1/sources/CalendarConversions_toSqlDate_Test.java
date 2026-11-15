package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import java.util.TimeZone;
import java.time.Instant;
import java.time.ZoneOffset;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toSqlDate_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    @Test
    void toSqlDate_returnsDateWithSameEpochMilli() {
        // GIVEN
        // 2022-01-01T00:00:00Z
        long expectedMillis = 1_640_995_200_000L;
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(expectedMillis);
        // WHEN
        Date result = CalendarConversions.toSqlDate(calendar, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(expectedMillis, result.getTime(), "The Date returned should have the same epoch milliseconds as the Calendar");
    }

    @Test
    void toSqlDate_throwsClassCastException_whenObjectIsNotCalendar() {
        // GIVEN
        Object notACalendar = "I am not a calendar";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toSqlDate(notACalendar, mockConverter), "Passing a non‑Calendar object should result in a ClassCastException");
    }

    @Test
    void toSqlDate_throwsNullPointerException_whenFromIsNull() {
        // GIVEN
        Object nullObject = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toSqlDate(nullObject, mockConverter), "Passing null as the source object should result in a NullPointerException");
    }

    @Test
    @DisplayName("GregorianCalendar UTC instant is converted correctly")
    void toSqlDate_utcInstant() {
        // 2023-01-15T10:30:00Z  -> epoch 1673778600000
        long expected = 1_673_778_600_000L;
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(expected);
        Date result = CalendarConversions.toSqlDate(cal, mockConverter);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "Date epoch must match the Calendar's epoch milliseconds");
    }

    @Test
    @DisplayName("Calendar with America/New_York zone is converted to the same instant")
    void toSqlDate_nonUtcZone() {
        // 2022‑12‑31 23:00:00 in New York = 2023‑01‑01 04:00:00 UTC
        long expected = 1_672_537_200_000L;
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        cal.set(2022, Calendar.DECEMBER, 31, 23, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date result = CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(expected, result.getTime(), "Conversion must respect the absolute instant regardless of the Calendar's zone");
    }

    @Test
    @DisplayName("Default Calendar subclass (Calendar.getInstance()) works")
    void toSqlDate_defaultCalendarSubclass() {
        // usually GregorianCalendar
        Calendar cal = Calendar.getInstance();
        long before = System.currentTimeMillis();
        Date result = CalendarConversions.toSqlDate(cal, mockConverter);
        long after = System.currentTimeMillis();
        assertTrue(result.getTime() >= before && result.getTime() <= after, "Resulting epoch should be within the call window");
    }

    @Test
    @DisplayName("Epoch zero (1970‑01‑01T00:00:00Z) yields Date with epoch 0")
    void toSqlDate_epochZero() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(0L);
        Date result = CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(0L, result.getTime());
    }

    @Test
    @DisplayName("Pre‑1970 date produces negative epoch")
    void toSqlDate_pre1970() {
        // 1965‑07‑20T12:00:00Z -> epoch -150422400000
        long expected = -150_422_400_000L;
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(expected);
        Date result = CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(expected, result.getTime());
    }

    @Test
    @DisplayName("DST spring‑forward non‑existent local time is handled")
    void toSqlDate_dstSpringForward() {
        // America/New_York 2023‑03‑12 02:30 does not exist; Calendar rolls to 03:30 EDT
        // Expected UTC: 2023‑03‑12 07:30:00Z -> epoch 1_678_625_400_000
        long expected = 1_678_625_400_000L;
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        cal.set(2023, Calendar.MARCH, 12, 2, 30, 0);
        // lenient = true, will roll forward
        cal.set(Calendar.MILLISECOND, 0);
        Date result = CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(expected, result.getTime(), "Calendar should roll the nonexistent time to the next valid instant");
    }

    @Test
    @DisplayName("Timezone with 45‑minute offset (Asia/Kathmandu) is converted correctly")
    void toSqlDate_45MinuteOffset() {
        // 2023‑01‑01 00:00:00 local (UTC+5:45) => 2022‑12‑31 18:15:00 UTC
        // epoch 1_672_510_500_000
        long expected = 1_672_510_500_000L;
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Kathmandu"));
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date result = CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(expected, result.getTime());
    }

    @Test
    @DisplayName("Converter argument is ignored – a non‑null mock does not affect result")
    void toSqlDate_converterIgnored() {
        // 2023‑06‑15T08:15:30.123Z -> epoch 1_686_819_330_123
        long expected = 1_686_819_330_123L;
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(expected);
        // mock is non‑null
        Date result = CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(expected, result.getTime(), "The Converter parameter must not influence the conversion");
    }

    @Test
    @DisplayName("Millisecond precision is preserved")
    void toSqlDate_millisecondPrecision() {
        // 2023‑05‑04 12:34:56.789 UTC -> epoch 1_683_203_696_789
        long expected = 1_683_203_696_789L;
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(expected);
        Date result = CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(expected, result.getTime());
    }

    @Test
    @DisplayName("Null Calendar argument results in NullPointerException")
    void toSqlDate_nullInput() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toSqlDate(null, mockConverter));
    }

    @Test
    @DisplayName("Non‑Calendar object causes ClassCastException")
    void toSqlDate_notACalendar() {
        Object notCalendar = "just a string";
        assertThrows(ClassCastException.class, () -> CalendarConversions.toSqlDate(notCalendar, mockConverter));
    }

    @Test
    @DisplayName("Lenient mode disabled with an invalid date throws IllegalArgumentException")
    void toSqlDate_lenientDisabledInvalidDate() {
        Calendar cal = new GregorianCalendar();
        cal.setLenient(false);
        // February 30th is invalid
        cal.set(2023, Calendar.FEBRUARY, 30);
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toSqlDate(cal, mockConverter));
    }

    @Test
    @DisplayName("Calendar with extremely large epoch (Long.MAX_VALUE) is handled")
    void toSqlDate_extremelyLargeEpoch() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(Long.MAX_VALUE);
        Date result = CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(Long.MAX_VALUE, result.getTime(), "java.util.Date can hold Long.MAX_VALUE milliseconds; no overflow should occur");
    }

    @Test
    @DisplayName("Calendar subclass whose getTime() returns null triggers NullPointerException")
    void toSqlDate_calendarGetTimeReturnsNull() {
        Calendar cal = new Calendar() {

            @Override
            public int get(int field) {
                return 0;
            }

            @Override
            public void set(int field, int value) {
            }

            @Override
            public void clear() {
            }

            @Override
            public void clear(int field) {
            }

            @Override
            public void setTime(Date date) {
            }

            @Override
            public Date getTime() {
                return null;
            }

            @Override
            public void setTimeInMillis(long millis) {
            }

            @Override
            public long getTimeInMillis() {
                return 0L;
            }

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public void roll(int field, boolean up) {
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
            public int getActualMaximum(int field) {
                return 0;
            }

            @Override
            public int getActualMinimum(int field) {
                return 0;
            }

            @Override
            public void setTimeZone(TimeZone value) {
            }

            @Override
            public TimeZone getTimeZone() {
                return TimeZone.getDefault();
            }

            @Override
            public void setLenient(boolean lenient) {
            }

            @Override
            public boolean isLenient() {
                return true;
            }

            @Override
            public void setFirstDayOfWeek(int value) {
            }

            @Override
            public int getFirstDayOfWeek() {
                return 1;
            }

            @Override
            public void setMinimalDaysInFirstWeek(int value) {
            }

            @Override
            public int getMinimalDaysInFirstWeek() {
                return 1;
            }

            @Override
            public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) {
            }

            @Override
            public int getWeekYear() {
                return 0;
            }

            @Override
            public int getWeeksInWeekYear() {
                return 0;
            }

            @Override
            public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek, int hour, int minute, int second, int millisecond) {
            }
        };
        assertThrows(NullPointerException.class, () -> CalendarConversions.toSqlDate(cal, mockConverter));
    }

    @Test
    @DisplayName("Calendar subclass that throws RuntimeException from getTime() propagates the exception")
    void toSqlDate_calendarThrowsRuntimeException() {
        Calendar cal = new Calendar() {

            @Override
            public int get(int field) {
                return 0;
            }

            @Override
            public void set(int field, int value) {
            }

            @Override
            public void clear() {
            }

            @Override
            public void clear(int field) {
            }

            @Override
            public void setTime(Date date) {
            }

            @Override
            public Date getTime() {
                throw new RuntimeException("forced");
            }

            @Override
            public void setTimeInMillis(long millis) {
            }

            @Override
            public long getTimeInMillis() {
                return 0L;
            }

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public void roll(int field, boolean up) {
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
            public int getActualMaximum(int field) {
                return 0;
            }

            @Override
            public int getActualMinimum(int field) {
                return 0;
            }

            @Override
            public void setTimeZone(TimeZone value) {
            }

            @Override
            public TimeZone getTimeZone() {
                return TimeZone.getDefault();
            }

            @Override
            public void setLenient(boolean lenient) {
            }

            @Override
            public boolean isLenient() {
                return true;
            }

            @Override
            public void setFirstDayOfWeek(int value) {
            }

            @Override
            public int getFirstDayOfWeek() {
                return 1;
            }

            @Override
            public void setMinimalDaysInFirstWeek(int value) {
            }

            @Override
            public int getMinimalDaysInFirstWeek() {
                return 1;
            }

            @Override
            public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek) {
            }

            @Override
            public int getWeekYear() {
                return 0;
            }

            @Override
            public int getWeeksInWeekYear() {
                return 0;
            }

            @Override
            public void setWeekDate(int weekYear, int weekOfYear, int dayOfWeek, int hour, int minute, int second, int millisecond) {
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CalendarConversions.toSqlDate(cal, mockConverter));
        assertEquals("forced", ex.getMessage());
    }
}
