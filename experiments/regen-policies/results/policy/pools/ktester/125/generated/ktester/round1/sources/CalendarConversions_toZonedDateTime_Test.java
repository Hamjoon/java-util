package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toZonedDateTime_Test {

    /**
     * Shared {@link Converter} used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The converter is not used by the implementation, but a non‑null instance
        // is required for the method signature in many test cases.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// epoch 0 in UTC
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("UTC")) {

            {
                setTimeInMillis(0L);
            }
        }, "1970-01-01T00:00Z[UTC]", "Convert a GregorianCalendar set to the epoch (0 ms) with UTC zone"), // epoch 0 in America/New_York (offset -05:00 at 1970‑01‑01)
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("America/New_York")) {

            {
                setTimeInMillis(0L);
            }
        }, "1969-12-31T19:00-05:00[America/New_York]", "Convert a GregorianCalendar set to the epoch (0 ms) with America/New_York zone"), // 2023‑03‑15 12:30:45 in Asia/Tokyo (UTC+09:00)
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("Asia/Tokyo")) {

            {
                set(2023, Calendar.MARCH, 15, 12, 30, 45);
                set(Calendar.MILLISECOND, 0);
            }
        }, "2023-03-15T12:30:45+09:00[Asia/Tokyo]", "Convert a GregorianCalendar representing 2023‑03‑15 12:30:45 in Tokyo"), // 2022‑12‑31T23:59:59Z in Europe/Paris (offset +01:00 after DST change)
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("Europe/Paris")) {

            {
                // 2022‑12‑31T23:59:59Z
                setTimeInMillis(1672531199000L);
            }
        }, "2023-01-01T00:59:59+01:00[Europe/Paris]", "Convert a GregorianCalendar with a specific instant in Europe/Paris"), // Fixed offset GMT+02:00 at epoch
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("GMT+02:00")) {

            {
                setTimeInMillis(0L);
            }
        }, "1970-01-01T02:00+02:00[GMT+02:00]", "Calendar with a fixed offset zone (GMT+02:00) at the epoch"), // Negative epoch (one day before 1970) in UTC
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("UTC")) {

            {
                // -1 day
                setTimeInMillis(-86_400_000L);
            }
        }, "1969-12-31T00:00Z[UTC]", "Negative epoch (one day before 1970‑01‑01) in UTC"), // DST transition – just before spring forward in Europe/Paris (+01:00)
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("Europe/Paris")) {

            {
                set(2023, Calendar.MARCH, 26, 1, 30, 0);
                set(Calendar.MILLISECOND, 0);
            }
        }, "2023-03-26T01:30+01:00[Europe/Paris]", "Date‑time just before the DST spring‑forward transition in Europe/Paris"), // DST transition – just after spring forward in Europe/Paris (+02:00)
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("Europe/Paris")) {

            {
                set(2023, Calendar.MARCH, 26, 3, 30, 0);
                set(Calendar.MILLISECOND, 0);
            }
        }, "2023-03-26T03:30+02:00[Europe/Paris]", "Date‑time just after the DST spring‑forward transition in Europe/Paris"), // America/Phoenix (no DST) with non‑zero epoch
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("America/Phoenix")) {

            {
                setTimeInMillis(123_456_789L);
            }
        }, "1970-01-02T10:17:36.789Z[America/Phoenix]", "Calendar with a non‑UTC zone that has no DST and a non‑zero epoch"), // Same as previous but with a non‑null Converter instance
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("UTC")) {

            {
                setTimeInMillis(123_456_789L);
            }
        }, "1970-01-02T10:17:36.789Z[UTC]", "Conversion with a non‑null Converter (should be ignored)"));
    }

    private static Stream<Arguments> exceptionProvider() {
        // Helper calendar that returns null for getTimeZone()
        Calendar calendarWithNullZone = new Calendar() {

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public boolean after(Object when) {
                return false;
            }

            @Override
            public boolean before(Object when) {
                return false;
            }

            @Override
            public Date getTime() {
                return new Date();
            }

            @Override
            public void setTime(Date date) {
            }

            @Override
            public Calendar clone() {
                return this;
            }

            @Override
            public int get(int field) {
                return 0;
            }

            @Override
            public void set(int field, int value) {
            }

            @Override
            public TimeZone getTimeZone() {
                return null;
            }

            @Override
            public void setTimeZone(TimeZone tz) {
            }

            @Override
            public long getTimeInMillis() {
                return 0L;
            }

            @Override
            public void setTimeInMillis(long millis) {
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
            public int getActualMaximum(int field) {
                return 0;
            }

            @Override
            public int getActualMinimum(int field) {
                return 0;
            }

            @Override
            public int getLeastMaximum(int field) {
                return 0;
            }

            @Override
            public int getGreatestMinimum(int field) {
                return 0;
            }

            @Override
            public int getMaximum(int field) {
                return 0;
            }

            @Override
            public int getMinimum(int field) {
                return 0;
            }

            @Override
            public void roll(int field, boolean up) {
            }

            @Override
            public void roll(int field, int amount) {
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
        };
        // Calendar with a year far beyond the supported range (will trigger DateTimeException)
        Calendar outOfRangeCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        outOfRangeCalendar.set(1_000_000_001, Calendar.JANUARY, 1, 0, 0, 0);
        outOfRangeCalendar.set(Calendar.MILLISECOND, 0);
        return Stream.of(// null `from` argument
        Arguments.of(null, null, NullPointerException.class, "Passing a null `from` argument should cause NullPointerException"), // Object that is not a Calendar (java.util.Date)
        Arguments.of(new Date(), null, ClassCastException.class, "Providing a non‑Calendar object must result in ClassCastException"), // Calendar whose getTimeZone() returns null
        Arguments.of(calendarWithNullZone, null, NullPointerException.class, "Calendar with null TimeZone should cause NullPointerException"), // Calendar with year beyond java.time range
        Arguments.of(outOfRangeCalendar, null, DateTimeException.class, "GregorianCalendar with an out‑of‑range year should trigger DateTimeException"));
    }

    @Test
    void convertsGregorianCalendarWithDefaultTimeZone() {
        // GIVEN: a GregorianCalendar set to a known instant in the default zone
        Calendar cal = new GregorianCalendar();
        // 2020‑09‑13T12:26:40Z
        cal.setTimeInMillis(1_600_000_000_000L);
        // WHEN: converting to ZonedDateTime
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        // THEN: the result should match the calendar's instant and default zone
        ZonedDateTime expected = ZonedDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault());
        assertEquals(expected, result);
    }

    @Test
    void convertsCalendarWithSpecificTimeZone() {
        // GIVEN: a calendar using a non‑default time‑zone (UTC‑5)
        // UTC‑5 / UTC‑4 depending on DST
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar cal = new GregorianCalendar(tz);
        // 2020‑09‑13T12:26:40Z
        cal.setTimeInMillis(1_600_000_000_000L);
        // WHEN: converting
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, converter);
        // THEN: the zone of the result must be the calendar's zone
        ZonedDateTime expected = ZonedDateTime.ofInstant(cal.toInstant(), tz.toZoneId());
        assertEquals(expected, result);
    }

    @Test
    void throwsClassCastExceptionWhenObjectIsNotCalendar() {
        // GIVEN: an object that is not a Calendar
        Object notACalendar = "I am not a calendar";
        // WHEN / THEN: a ClassCastException is expected
        assertThrows(ClassCastException.class, () -> CalendarConversions.toZonedDateTime(notACalendar, converter));
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (parameterized)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversionProvider")
    void toZonedDateTime_successful(Calendar calendar, String expectedIso, String description) {
        // WHEN
        ZonedDateTime result = CalendarConversions.toZonedDateTime(calendar, converter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Exception test cases (parameterized)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionProvider")
    void toZonedDateTime_exceptions(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CalendarConversions.toZonedDateTime(from, conv), description);
    }
}
