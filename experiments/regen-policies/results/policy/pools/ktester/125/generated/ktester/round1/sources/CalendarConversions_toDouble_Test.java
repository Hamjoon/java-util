package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.ZoneOffset;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toDouble_Test {

    private static Converter converter;

    /**
     * A trivial subclass of {@link Converter} to prove the argument is ignored.
     */
    private static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    /**
     * Calendar implementation whose {@code getTime()} returns {@code null}.
     */
    private static class NullTimeCalendar extends Calendar {

        @Override
        public Date getTime() {
            return null;
        }

        @Override
        public void setTime(Date date) {
        }

        @Override
        public int get(int field) {
            return 0;
        }

        @Override
        public void set(int field, int value) {
        }

        @Override
        public void add(int field, int amount) {
        }

        @Override
        public void roll(int field, boolean up) {
        }

        @Override
        public void roll(int field, int amount) {
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
        public int getActualMinimum(int field) {
            return 0;
        }

        @Override
        public int getActualMaximum(int field) {
            return 0;
        }

        @Override
        public void setTimeInMillis(long millis) {
        }

        @Override
        public long getTimeInMillis() {
            return 0L;
        }

        @Override
        public void clear() {
        }

        @Override
        public void clear(int field) {
        }

        @Override
        public boolean isSet(int field) {
            return false;
        }

        @Override
        public void setLenient(boolean lenient) {
        }

        @Override
        public boolean isLenient() {
            return false;
        }

        @Override
        public void setTimeZone(TimeZone zone) {
        }

        @Override
        public TimeZone getTimeZone() {
            return TimeZone.getTimeZone("UTC");
        }

        @Override
        public void setFirstDayOfWeek(int value) {
        }

        @Override
        public int getFirstDayOfWeek() {
            return 0;
        }

        @Override
        public void setMinimalDaysInFirstWeek(int value) {
        }

        @Override
        public int getMinimalDaysInFirstWeek() {
            return 0;
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
        public int getWeekYear() {
            return 0;
        }

        @Override
        public int get(CalendarField field) {
            return 0;
        }
    }

    /**
     * Calendar implementation whose {@code getTime()} throws a {@link RuntimeException}.
     */
    private static class ThrowingTimeCalendar extends Calendar {

        @Override
        public Date getTime() {
            throw new RuntimeException("forced failure");
        }

        @Override
        public void setTime(Date date) {
        }

        @Override
        public int get(int field) {
            return 0;
        }

        @Override
        public void set(int field, int value) {
        }

        @Override
        public void add(int field, int amount) {
        }

        @Override
        public void roll(int field, boolean up) {
        }

        @Override
        public void roll(int field, int amount) {
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
        public int getActualMinimum(int field) {
            return 0;
        }

        @Override
        public int getActualMaximum(int field) {
            return 0;
        }

        @Override
        public void setTimeInMillis(long millis) {
        }

        @Override
        public long getTimeInMillis() {
            return 0L;
        }

        @Override
        public void clear() {
        }

        @Override
        public void clear(int field) {
        }

        @Override
        public boolean isSet(int field) {
            return false;
        }

        @Override
        public void setLenient(boolean lenient) {
        }

        @Override
        public boolean isLenient() {
            return false;
        }

        @Override
        public void setTimeZone(TimeZone zone) {
        }

        @Override
        public TimeZone getTimeZone() {
            return TimeZone.getTimeZone("UTC");
        }

        @Override
        public void setFirstDayOfWeek(int value) {
        }

        @Override
        public int getFirstDayOfWeek() {
            return 0;
        }

        @Override
        public void setMinimalDaysInFirstWeek(int value) {
        }

        @Override
        public int getMinimalDaysInFirstWeek() {
            return 0;
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
        public int getWeekYear() {
            return 0;
        }

        @Override
        public int get(CalendarField field) {
            return 0;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special configuration required for this conversion
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none for Converter)
        converter = null;
    }

    /**
     * Provider for all successful conversion cases.
     */
    static Stream<Arguments> successProvider() {
        return Stream.of(// Normal conversions -------------------------------------------------
        Arguments.of(// epoch
        new GregorianCalendar(1970, Calendar.JANUARY, 1, 0, 0, 0), null, 0.0), Arguments.of(createCalendarWithMillis(TimeZone.getTimeZone("UTC"), 1615734566535L), null, 1615734566535L / 1000.0), Arguments.of(createCalendarWithMillis(TimeZone.getTimeZone("America/Los_Angeles"), 1615734566535L), null, 1615734566535L / 1000.0), Arguments.of(createCalendarWithMillis(TimeZone.getTimeZone("UTC"), Long.MAX_VALUE), null, Long.MAX_VALUE / 1000.0), // Boundary and edge cases -------------------------------------------
        Arguments.of(createCalendarWithMillis(TimeZone.getTimeZone("UTC"), -123456789L), null, -123456789L / 1000.0), Arguments.of(createCalendarWithMillis(TimeZone.getTimeZone("UTC"), Long.MIN_VALUE), null, Long.MIN_VALUE / 1000.0), Arguments.of(createCalendarWithFields(TimeZone.getTimeZone("UTC"), 2000, Calendar.JANUARY, 1, 0, 0, 0, 250), null, 946684800250L / 1000.0), Arguments.of(createCalendarWithFields(TimeZone.getTimeZone("America/New_York"), 2000, Calendar.JANUARY, 1, 0, 0, 0, 250), null, 964684800250L / 1000.0), Arguments.of(createCalendarWithMillis(TimeZone.getTimeZone("UTC"), 8_000_000_000_000L), null, 8_000_000_000_000L / 1000.0), // Converter argument effect -------------------------------------------
        Arguments.of(createCalendarWithMillis(TimeZone.getTimeZone("UTC"), 1_234_567_890L), new DummyConverter(), 1_234_567_890L / 1000.0));
    }

    /**
     * Provider for all cases that should throw an exception.
     */
    static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Invalid inputs -------------------------------------------------------
        Arguments.of(null, null, NullPointerException.class), Arguments.of("just a string", null, ClassCastException.class), // Custom Calendar that returns null from getTime()
        Arguments.of(new NullTimeCalendar(), null, NullPointerException.class), // Custom Calendar that throws RuntimeException from getTime()
        Arguments.of(new ThrowingTimeCalendar(), null, RuntimeException.class));
    }

    /**
     * Creates a {@link GregorianCalendar} with the given time‑zone and epoch‑millis.
     */
    private static Calendar createCalendarWithMillis(TimeZone tz, long epochMillis) {
        Calendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Creates a {@link GregorianCalendar} using field setters (year, month, …) and a
     * millisecond fraction.
     *
     * @param tz          the time‑zone of the calendar
     * @param year        the year
     * @param month       the month (Calendar constants, zero‑based)
     * @param day         the day of month
     * @param hour        hour of day
     * @param minute      minute
     * @param second      second
     * @param milli       millisecond fraction
     */
    private static Calendar createCalendarWithFields(TimeZone tz, int year, int month, int day, int hour, int minute, int second, int milli) {
        Calendar cal = new GregorianCalendar(tz);
        // ensure no leftover fields
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, milli);
        return cal;
    }

    @Test
    void toDouble_returnsSecondsSinceEpoch_forGivenCalendar() {
        // GIVEN
        Calendar cal = new GregorianCalendar();
        // set a known instant: 2023‑01‑01T12:34:56Z
        // start from epoch
        cal.setTimeInMillis(0);
        cal.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.JANUARY, 1, 12, 34, 56);
        cal.set(Calendar.MILLISECOND, 0);
        long expectedMillis = cal.getTimeInMillis();
        double expectedSeconds = expectedMillis / 1000.0;
        // WHEN
        double result = CalendarConversions.toDouble(cal, converter);
        // THEN
        assertEquals(expectedSeconds, result, 1e-9, "toDouble should return epoch milliseconds divided by 1000.0");
    }

    @Test
    void toDouble_handlesMillisecondsPrecision() {
        // GIVEN
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(0);
        cal.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.FEBRUARY, 28, 23, 59, 59);
        // add sub‑second part
        cal.set(Calendar.MILLISECOND, 123);
        long expectedMillis = cal.getTimeInMillis();
        double expectedSeconds = expectedMillis / 1000.0;
        // WHEN
        double result = CalendarConversions.toDouble(cal, converter);
        // THEN
        assertEquals(expectedSeconds, result, 1e-9);
    }

    @Test
    void toDouble_throwsClassCastException_whenNonCalendarProvided() {
        // GIVEN
        Object notACalendar = "not a calendar";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toDouble(notACalendar, converter));
    }

    /* -------------------------------------------------------------
     *  Successful conversions – the method should return epoch‑seconds.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successProvider")
    void toDouble_success(Calendar from, Converter conv, double expected) {
        // Act
        double result = CalendarConversions.toDouble(from, conv);
        // Assert – use a tolerance relative to the magnitude of the expected value
        double tolerance = Math.max(1e-9, Math.abs(expected) * 1e-12);
        assertEquals(expected, result, tolerance, () -> "Expected seconds = " + expected + " but got " + result);
    }

    /* -------------------------------------------------------------
     *  Expected exception scenarios.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionProvider")
    void toDouble_exceptions(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CalendarConversions.toDouble(from, conv));
    }
}
