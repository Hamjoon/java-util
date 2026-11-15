package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toTimestamp_Test {

    /**
     * Shared Converter instance – the method under test does not use it, but the signature requires it.
     */
    private static Converter converter;

    /**
     * Simple custom Calendar subclass that delegates to an internal GregorianCalendar.
     * Used to verify that {@code toTimestamp} works with any Calendar subclass.
     */
    private static final class CustomCalendar extends Calendar {

        private final Calendar delegate;

        CustomCalendar(int year, int month, int day, int hour, int minute, int second, int millis) {
            delegate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            delegate.clear();
            delegate.set(year, month, day, hour, minute, second);
            delegate.set(Calendar.MILLISECOND, millis);
        }

        @Override
        public long getTimeInMillis() {
            return delegate.getTimeInMillis();
        }

        @Override
        public void setTimeInMillis(long millis) {
            delegate.setTimeInMillis(millis);
        }

        @Override
        public void add(int field, int amount) {
            delegate.add(field, amount);
        }

        @Override
        public void roll(int field, boolean up) {
            delegate.roll(field, up);
        }

        @Override
        public int get(int field) {
            return delegate.get(field);
        }

        @Override
        public void set(int field, int value) {
            delegate.set(field, value);
        }

        @Override
        public void set(int year, int month, int date) {
            delegate.set(year, month, date);
        }

        @Override
        public void set(int year, int month, int date, int hourOfDay, int minute) {
            delegate.set(year, month, date, hourOfDay, minute);
        }

        @Override
        public void set(int year, int month, int date, int hourOfDay, int minute, int second) {
            delegate.set(year, month, date, hourOfDay, minute, second);
        }

        @Override
        public Date getTime() {
            return delegate.getTime();
        }

        @Override
        public void setTime(Date date) {
            delegate.setTime(date);
        }

        @Override
        public TimeZone getTimeZone() {
            return delegate.getTimeZone();
        }

        @Override
        public void setTimeZone(TimeZone value) {
            delegate.setTimeZone(value);
        }

        @Override
        public boolean isSet(int field) {
            return delegate.isSet(field);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public void clear(int field) {
            delegate.clear(field);
        }

        @Override
        public void setLenient(boolean lenient) {
            delegate.setLenient(lenient);
        }

        @Override
        public boolean isLenient() {
            return delegate.isLenient();
        }

        @Override
        public int getMinimum(int field) {
            return delegate.getMinimum(field);
        }

        @Override
        public int getMaximum(int field) {
            return delegate.getMaximum(field);
        }

        @Override
        public int getGreatestMinimum(int field) {
            return delegate.getGreatestMinimum(field);
        }

        @Override
        public int getLeastMaximum(int field) {
            return delegate.getLeastMaximum(field);
        }

        @Override
        public int getActualMaximum(int field) {
            return delegate.getActualMaximum(field);
        }

        @Override
        public int getActualMinimum(int field) {
            return delegate.getActualMinimum(field);
        }

        @Override
        public Date getGregorianChange() {
            return delegate.getGregorianChange();
        }

        @Override
        public void setGregorianChange(Date date) {
            delegate.setGregorianChange(date);
        }

        @Override
        public int getWeekYear() {
            return delegate.getWeekYear();
        }

        @Override
        public void setWeekYear(int weekYear) {
            delegate.setWeekYear(weekYear);
        }

        @Override
        public int getWeeksInWeekYear() {
            return delegate.getWeeksInWeekYear();
        }

        @Override
        public int getFirstDayOfWeek() {
            return delegate.getFirstDayOfWeek();
        }

        @Override
        public void setFirstDayOfWeek(int value) {
            delegate.setFirstDayOfWeek(value);
        }

        @Override
        public int getMinimalDaysInFirstWeek() {
            return delegate.getMinimalDaysInFirstWeek();
        }

        @Override
        public void setMinimalDaysInFirstWeek(int value) {
            delegate.setMinimalDaysInFirstWeek(value);
        }

        @Override
        public boolean isWeekDateSupported() {
            return delegate.isWeekDateSupported();
        }

        @Override
        public int getWeekOfYear() {
            return delegate.getWeekOfYear();
        }

        @Override
        public int getWeekOfMonth() {
            return delegate.getWeekOfMonth();
        }

        @Override
        public int getDayOfYear() {
            return delegate.getDayOfYear();
        }

        @Override
        public int getDayOfMonth() {
            return delegate.getDayOfMonth();
        }

        @Override
        public int getDayOfWeek() {
            return delegate.getDayOfWeek();
        }

        @Override
        public int getHourOfDay() {
            return delegate.getHourOfDay();
        }

        @Override
        public int getMinute() {
            return delegate.getMinute();
        }

        @Override
        public int getSecond() {
            return delegate.getSecond();
        }

        @Override
        public int getMillisecond() {
            return delegate.getMillisecond();
        }

        @Override
        public int getZoneOffset() {
            return delegate.getZoneOffset();
        }

        @Override
        public int getDSTOffset() {
            return delegate.getDSTOffset();
        }
    }

    /**
     * Calendar subclass that deliberately throws an {@link IllegalStateException}
     * from {@link #getTimeInMillis()} to test exception propagation.
     */
    private static final class ExceptionThrowingCalendar extends Calendar {

        @Override
        public long getTimeInMillis() {
            throw new IllegalStateException("Deliberate exception from getTimeInMillis");
        }

        // The remaining abstract methods can be left with dummy implementations
        @Override
        public void setTimeInMillis(long millis) {
        }

        @Override
        public void add(int field, int amount) {
        }

        @Override
        public void roll(int field, boolean up) {
        }

        @Override
        public int get(int field) {
            return 0;
        }

        @Override
        public void set(int field, int value) {
        }

        @Override
        public void set(int year, int month, int date) {
        }

        @Override
        public void set(int year, int month, int date, int hourOfDay, int minute) {
        }

        @Override
        public void set(int year, int month, int date, int hourOfDay, int minute, int second) {
        }

        @Override
        public Date getTime() {
            return null;
        }

        @Override
        public void setTime(Date date) {
        }

        @Override
        public TimeZone getTimeZone() {
            return TimeZone.getDefault();
        }

        @Override
        public void setTimeZone(TimeZone value) {
        }

        @Override
        public boolean isSet(int field) {
            return false;
        }

        @Override
        public void clear() {
        }

        @Override
        public void clear(int field) {
        }

        @Override
        public void setLenient(boolean lenient) {
        }

        @Override
        public boolean isLenient() {
            return false;
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
        public Date getGregorianChange() {
            return null;
        }

        @Override
        public void setGregorianChange(Date date) {
        }

        @Override
        public int getWeekYear() {
            return 0;
        }

        @Override
        public void setWeekYear(int weekYear) {
        }

        @Override
        public int getWeeksInWeekYear() {
            return 0;
        }

        @Override
        public int getFirstDayOfWeek() {
            return 0;
        }

        @Override
        public void setFirstDayOfWeek(int value) {
        }

        @Override
        public int getMinimalDaysInFirstWeek() {
            return 0;
        }

        @Override
        public void setMinimalDaysInFirstWeek(int value) {
        }

        @Override
        public boolean isWeekDateSupported() {
            return false;
        }

        @Override
        public int getWeekOfYear() {
            return 0;
        }

        @Override
        public int getWeekOfMonth() {
            return 0;
        }

        @Override
        public int getDayOfYear() {
            return 0;
        }

        @Override
        public int getDayOfMonth() {
            return 0;
        }

        @Override
        public int getDayOfWeek() {
            return 0;
        }

        @Override
        public int getHourOfDay() {
            return 0;
        }

        @Override
        public int getMinute() {
            return 0;
        }

        @Override
        public int getSecond() {
            return 0;
        }

        @Override
        public int getMillisecond() {
            return 0;
        }

        @Override
        public int getZoneOffset() {
            return 0;
        }

        @Override
        public int getDSTOffset() {
            return 0;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance; use the default options.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    /**
     * Valid Calendar conversions – includes different zones and a custom subclass.
     */
    static Stream<Arguments> validCalendarProvider() {
        // 1. GregorianCalendar UTC 2023‑01‑01T00:00:00Z
        Calendar utcCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utcCal.clear();
        utcCal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        Timestamp utcExpected = new Timestamp(utcCal.getTimeInMillis());
        // 2. GregorianCalendar Asia/Tokyo (+09:00) 2023‑01‑01T09:00:00+09:00
        Calendar tokyoCal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Tokyo"));
        tokyoCal.clear();
        tokyoCal.set(2023, Calendar.JANUARY, 1, 9, 0, 0);
        Timestamp tokyoExpected = new Timestamp(tokyoCal.getTimeInMillis());
        // 3. Custom subclass – behaves like a normal Calendar
        Calendar customCal = new CustomCalendar(2023, Calendar.JUNE, 15, 12, 34, 56, 789);
        Timestamp customExpected = new Timestamp(customCal.getTimeInMillis());
        return Stream.of(Arguments.of(utcCal, utcExpected, "UTC calendar conversion"), Arguments.of(tokyoCal, tokyoExpected, "Asia/Tokyo calendar conversion"), Arguments.of(customCal, customExpected, "Custom Calendar subclass conversion"));
    }

    /**
     * Invalid inputs that should result in exceptions.
     */
    static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "null Calendar argument"), Arguments.of("2023-01-01", ClassCastException.class, "String instead of Calendar"), Arguments.of(new Date(), ClassCastException.class, "java.util.Date instead of Calendar"));
    }

    /**
     * Boundary epoch values – zero, negative, far future.
     */
    static Stream<Arguments> epochBoundaryProvider() {
        // Epoch zero
        Calendar epochZero = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epochZero.clear();
        epochZero.setTimeInMillis(0L);
        Timestamp tsZero = new Timestamp(0L);
        // One millisecond before epoch
        Calendar beforeEpoch = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        beforeEpoch.clear();
        beforeEpoch.setTimeInMillis(-1L);
        Timestamp tsBefore = new Timestamp(-1L);
        // Far future (year 3000)
        Calendar farFuture = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        farFuture.clear();
        farFuture.set(3000, Calendar.JANUARY, 1, 0, 0, 0);
        Timestamp tsFuture = new Timestamp(farFuture.getTimeInMillis());
        return Stream.of(Arguments.of(epochZero, tsZero, "Unix epoch (1970‑01‑01T00:00:00Z)"), Arguments.of(beforeEpoch, tsBefore, "One millisecond before epoch"), Arguments.of(farFuture, tsFuture, "Far future year 3000"));
    }

    /**
     * Calendar whose getTimeInMillis throws an unchecked exception.
     */
    static Stream<Arguments> exceptionThrowingCalendarProvider() {
        Calendar badCal = new ExceptionThrowingCalendar();
        return Stream.of(Arguments.of(badCal, IllegalStateException.class, "Calendar that throws IllegalStateException"));
    }

    @Test
    void toTimestamp_convertsCalendarToCorrectTimestamp() {
        // GIVEN – a Calendar set to a known instant (2023‑01‑01T00:00:00Z)
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // clear all fields to avoid leftover values
        calendar.clear();
        calendar.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        long expectedMillis = calendar.getTimeInMillis();
        // WHEN – converting the Calendar to a Timestamp
        Timestamp ts = CalendarConversions.toTimestamp(calendar, converter);
        // THEN – the Timestamp should represent the same epoch milliseconds
        assertNotNull(ts, "Resulting Timestamp should not be null");
        assertEquals(expectedMillis, ts.getTime(), "Timestamp milliseconds should match Calendar's epoch milliseconds");
    }

    @Test
    void toTimestamp_nullCalendar_throwsNullPointerException() {
        // GIVEN – a null reference
        Calendar nullCal = null;
        // WHEN / THEN – a NullPointerException is expected
        assertThrows(NullPointerException.class, () -> CalendarConversions.toTimestamp(nullCal, converter), "Calling toTimestamp with null should throw NullPointerException");
    }

    @Test
    void toTimestamp_nonCalendarObject_throwsClassCastException() {
        // GIVEN – an object that is not a Calendar
        Object notACalendar = "I am not a calendar";
        // WHEN / THEN – a ClassCastException is expected
        assertThrows(ClassCastException.class, () -> CalendarConversions.toTimestamp(notACalendar, converter), "Calling toTimestamp with a non‑Calendar should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validCalendarProvider")
    void toTimestamp_validCalendars_returnsExpectedTimestamp(Calendar from, Timestamp expected, String description) {
        // WHEN
        Timestamp actual = CalendarConversions.toTimestamp(from, converter);
        // THEN
        assertNotNull(actual, "Resulting Timestamp must not be null");
        assertEquals(expected.getTime(), actual.getTime(), "Timestamp milliseconds must match the Calendar's epoch milliseconds");
        assertEquals(expected, actual, "Timestamp objects should be equal");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidInputProvider")
    void toTimestamp_invalidInputs_throwsExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CalendarConversions.toTimestamp(from, converter), "Expected " + expectedException.getSimpleName() + " for input: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("epochBoundaryProvider")
    void toTimestamp_epochBoundaryValues_returnCorrectTimestamp(Calendar from, Timestamp expected, String description) {
        // WHEN
        Timestamp actual = CalendarConversions.toTimestamp(from, converter);
        // THEN
        assertEquals(expected.getTime(), actual.getTime(), "Timestamp milliseconds should match expected for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionThrowingCalendarProvider")
    void toTimestamp_exceptionFromCalendar_isPropagated(Calendar from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CalendarConversions.toTimestamp(from, converter), "Exception should propagate for: " + description);
    }
}
