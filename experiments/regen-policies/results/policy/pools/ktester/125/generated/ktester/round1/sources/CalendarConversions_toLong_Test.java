package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;
import java.sql.Timestamp;
import java.time.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toLong_Test {

    /**
     * Shared Converter instance – the real implementation is fine because the method does not use it.
     */
    private static Converter converter;

    /**
     * Calendar subclass whose {@link #getTime()} returns {@code null}.
     * All other abstract methods are stubbed with {@code UnsupportedOperationException}
     * because they are never invoked by {@code toLong}.
     */
    private static class CalendarReturningNull extends Calendar {

        @Override
        public Date getTime() {
            return null;
        }

        @Override
        public void setTime(Date date) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int field, int amount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void roll(int field, boolean up) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int get(int field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(int field, int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear(int field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSet(int field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TimeZone getTimeZone() {
            return TimeZone.getDefault();
        }

        @Override
        public void setTimeZone(TimeZone tz) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Calendar subclass whose {@link #getTime()} throws an {@link IllegalStateException}.
     */
    private static class CalendarThrowingException extends Calendar {

        @Override
        public Date getTime() {
            throw new IllegalStateException("forced failure");
        }

        @Override
        public void setTime(Date date) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int field, int amount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void roll(int field, boolean up) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int get(int field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(int field, int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear(int field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSet(int field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TimeZone getTimeZone() {
            return TimeZone.getDefault();
        }

        @Override
        public void setTimeZone(TimeZone tz) {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options; no special configuration is needed for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up for the static Converter.
        converter = null;
    }

    /**
     * Provides a set of {@link Calendar} instances together with the expected
     * epoch‑millisecond value.  The {@code converter} argument is deliberately
     * passed as {@code null} to reflect the JSON test data – the method does
     * not depend on it.
     */
    private static Stream<Arguments> successfulConversionsProvider() {
        // 1. Unix epoch (1970‑01‑01T00:00:00Z) in UTC
        Calendar utcZero = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcZero.setTimeInMillis(0L);
        // 2. 2021‑12‑31 23:59:59 UTC
        Calendar utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcDate.set(2021, Calendar.DECEMBER, 31, 23, 59, 59);
        utcDate.set(Calendar.MILLISECOND, 0);
        // 3. Wall‑time 2021‑01‑01 00:00:00 in GMT+02:00 (should map to 2020‑12‑31 22:00:00 UTC)
        Calendar tzCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+02:00"));
        tzCal.set(2021, Calendar.JANUARY, 1, 0, 0, 0);
        tzCal.set(Calendar.MILLISECOND, 0);
        // 4. GregorianCalendar subclass set to 1999‑12‑31 23:59:59 UTC
        Calendar customGreg = new GregorianCalendar(1999, Calendar.DECEMBER, 31, 23, 59, 59) {

            {
                set(Calendar.MILLISECOND, 0);
                setTimeZone(TimeZone.getTimeZone("UTC"));
            }
        };
        return Stream.of(arguments(utcZero, 0L, "Unix epoch in UTC"), arguments(utcDate, 1640995199000L, "2021‑12‑31 23:59:59 UTC"), arguments(tzCal, 1609452000000L, "2021‑01‑01 00:00:00 in GMT+02:00"), arguments(customGreg, 946684799000L, "1999‑12‑31 23:59:59 UTC"));
    }

    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(arguments("not a calendar", ClassCastException.class, "String source"), arguments(Instant.now(), ClassCastException.class, "Instant source"));
    }

    @Test
    void toLong_returnsCorrectEpochMilli_forGregorianCalendar() {
        // GIVEN
        // 2022-01-01T00:00:00Z
        long expectedEpochMilli = 1_640_995_200_000L;
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(expectedEpochMilli);
        // WHEN
        Long result = CalendarConversions.toLong(calendar, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(expectedEpochMilli, result.longValue(), "The epoch milli returned by toLong must match the calendar's time");
    }

    @Test
    void toLong_throwsNullPointerException_whenSourceIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLong(nullSource, converter), "Calling toLong with a null source should throw NullPointerException");
    }

    @Test
    void toLong_throwsClassCastException_whenSourceIsNotCalendar() {
        // GIVEN
        Object notACalendar = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toLong(notACalendar, converter), "Calling toLong with a non‑Calendar source should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversionsProvider")
    void toLong_successfulConversions(Calendar calendar, long expectedEpochMilli, String description) {
        // WHEN
        // converter not used
        Long result = CalendarConversions.toLong(calendar, null);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(expectedEpochMilli, result.longValue(), () -> "Incorrect epoch milli for case: " + description);
    }

    @Test
    void toLong_nullSource_throwsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLong(nullSource, null), "Calling toLong with a null source should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] Invalid source: {2}")
    @MethodSource("invalidSourceProvider")
    void toLong_invalidSourceTypes(Object invalidSource, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CalendarConversions.toLong(invalidSource, null), () -> "Expected " + expectedException.getSimpleName() + " for " + description);
    }

    @Test
    void toLong_customCalendarReturningNull_throwsNullPointerException() {
        Calendar calendar = new CalendarReturningNull();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLong(calendar, null), "A Calendar whose getTime() returns null should cause NullPointerException");
    }

    @Test
    void toLong_customCalendarThrowingException_propagatesException() {
        Calendar calendar = new CalendarThrowingException();
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CalendarConversions.toLong(calendar, null), "Exception thrown by getTime() should propagate");
        assertEquals("forced failure", ex.getMessage(), "Exception message should be preserved");
    }
}
