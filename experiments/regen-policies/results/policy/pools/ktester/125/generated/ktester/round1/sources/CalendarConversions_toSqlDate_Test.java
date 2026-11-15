package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import java.time.Instant;
import java.sql.Date;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toSqlDate_Test {

    /**
     * Shared {@link Converter} instance – the method does not depend on its state.
     */
    private static Converter sharedConverter;

    /**
     * Calendar subclass that overrides {@code getTime()} to return a fixed Date.
     */
    static class MyCalendar extends GregorianCalendar {

        @Override
        public Date getTime() {
            return new Date(123456789L);
        }
    }

    /**
     * Calendar subclass whose {@code getTime()} throws a RuntimeException.
     */
    static class CalendarGetTimeThrows extends GregorianCalendar {

        @Override
        public Date getTime() {
            throw new RuntimeException("getTime failed");
        }
    }

    /**
     * Calendar subclass whose {@code getTime()} returns {@code null}.
     */
    static class CalendarGetTimeReturnsNull extends GregorianCalendar {

        @Override
        public Date getTime() {
            return null;
        }
    }

    /**
     * {@code Date} subclass that throws on {@code getTime()}.
     */
    static class BadDate extends Date {

        BadDate() {
            super();
        }

        @Override
        public long getTime() {
            throw new IllegalStateException("BadDate getTime");
        }
    }

    /**
     * Calendar subclass that returns a {@link BadDate} instance from {@code getTime()}.
     */
    static class CalendarReturningBadDate extends GregorianCalendar {

        @Override
        public Date getTime() {
            return new BadDate();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter constructor requires a ConverterOptions instance.
        // Using the default options is sufficient for these tests.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared reference.
        sharedConverter = null;
    }

    private static Stream<Arguments> provideSuccessfulCalendars() {
        // 1. UTC calendar – 2023‑01‑01T12:34:56.789Z
        Calendar utcCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(Instant.parse("2023-01-01T12:34:56.789Z").toEpochMilli());
        // 2. America/New_York – epoch start (1970‑01‑01T00:00:00‑05:00)
        Calendar nyCal = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        // absolute instant 0
        nyCal.setTimeInMillis(0L);
        // 3. Pre‑epoch – 1965‑12‑31T23:59:59.999Z
        Calendar preEpoch = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        preEpoch.setTimeInMillis(Instant.parse("1965-12-31T23:59:59.999Z").toEpochMilli());
        // 4. Leap day – 2020‑02‑29T12:00:00Z
        Calendar leapDay = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        leapDay.setTimeInMillis(Instant.parse("2020-02-29T12:00:00Z").toEpochMilli());
        // 5. Custom Calendar subclass overriding getTime()
        Calendar customCal = new MyCalendar();
        // 6. Non‑null converter ignored – UTC calendar 2023‑03‑15T08:15:30.123Z
        Calendar utc2 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc2.setTimeInMillis(Instant.parse("2023-03-15T08:15:30.123Z").toEpochMilli());
        // 7. DST fallback ambiguous hour – America/New_York 2021‑11‑07T01:30:00 (standard time)
        Calendar dstCal = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        dstCal.set(2021, Calendar.NOVEMBER, 7, 1, 30, 0);
        // ensure clean millis
        dstCal.set(Calendar.MILLISECOND, 0);
        // 8. Maximum long epoch
        Calendar maxCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        maxCal.setTime(new Date(Long.MAX_VALUE));
        return Stream.of(Arguments.of(utcCal, 1672576496789L, "UTC calendar – 2023‑01‑01T12:34:56.789Z"), Arguments.of(nyCal, 0L, "America/New_York calendar at Unix epoch start"), Arguments.of(preEpoch, -126230400001L, "Pre‑epoch calendar – 1965‑12‑31T23:59:59.999Z"), Arguments.of(leapDay, 1582977600000L, "Leap‑day calendar – 2020‑02‑29T12:00:00Z"), Arguments.of(customCal, 123456789L, "Custom Calendar subclass returning Date(123456789L)"), Arguments.of(utc2, 1678870530123L, "Non‑null converter ignored – 2023‑03‑15T08:15:30.123Z"), Arguments.of(dstCal, 1636285800000L, "DST fallback ambiguous hour – America/New_York 2021‑11‑07T01:30:00"), Arguments.of(maxCal, Long.MAX_VALUE, "Maximum long epoch"));
    }

    @Test
    @DisplayName("toSqlDate returns a Date with the same epoch millis as the source Calendar")
    void toSqlDateReturnsCorrectDate() {
        // GIVEN: a Calendar set to a known instant (including timezone handling)
        Calendar calendar = new GregorianCalendar();
        // 2021‑11‑10T00:00:00Z
        calendar.setTimeInMillis(1_638_720_000_000L);
        // WHEN: converting the Calendar to a java.util.Date via CalendarConversions
        Date result = CalendarConversions.toSqlDate(calendar, sharedConverter);
        // THEN: the resulting Date must have the identical epoch millisecond value
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(calendar.getTimeInMillis(), result.getTime(), "The Date epoch millis must match the Calendar's time");
    }

    @Test
    @DisplayName("toSqlDate creates a new Date instance (does not return the Calendar's internal Date)")
    void toSqlDateCreatesNewInstance() {
        // GIVEN
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // WHEN
        Date result = CalendarConversions.toSqlDate(calendar, sharedConverter);
        // THEN
        // The returned Date must be equal in value but not the same object as the Calendar's internal Date.
        assertNotSame(calendar.getTime(), result, "CalendarConversions.toSqlDate should create a new Date instance, not reuse the Calendar's internal Date");
        assertEquals(calendar.getTimeInMillis(), result.getTime(), "The new Date must represent the same instant as the Calendar");
    }

    @Test
    @DisplayName("toSqlDate throws ClassCastException when the source object is not a Calendar")
    void toSqlDateInvalidSourceThrowsException() {
        // GIVEN
        Object notACalendar = "I am not a Calendar";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toSqlDate(notACalendar, sharedConverter), "A non‑Calendar source should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideSuccessfulCalendars")
    @DisplayName("toSqlDate returns a Date with the expected epoch millis")
    void toSqlDateReturnsExpectedEpoch(Calendar calendar, Long expectedEpoch, String description) {
        // WHEN
        Date result = CalendarConversions.toSqlDate(calendar, null);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedEpoch.longValue(), result.getTime(), "Epoch millis should match expected value");
    }

    @Test
    @DisplayName("toSqlDate throws NullPointerException when source Calendar is null")
    void toSqlDateNullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toSqlDate(null, null), "Passing a null Calendar should result in NPE");
    }

    @Test
    @DisplayName("toSqlDate throws ClassCastException when source is not a Calendar")
    void toSqlDateInvalidSourceThrowsClassCast() {
        Object notACalendar = "I am not a Calendar";
        assertThrows(ClassCastException.class, () -> CalendarConversions.toSqlDate(notACalendar, null), "Non‑Calendar source must cause ClassCastException");
    }

    @Test
    @DisplayName("toSqlDate propagates RuntimeException thrown by Calendar#getTime()")
    void toSqlDateCalendarGetTimeThrowsRuntimeException() {
        Calendar badCal = new CalendarGetTimeThrows();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CalendarConversions.toSqlDate(badCal, null));
        assertEquals("getTime failed", ex.getMessage());
    }

    @Test
    @DisplayName("toSqlDate throws NullPointerException when Calendar#getTime() returns null")
    void toSqlDateCalendarGetTimeReturnsNull() {
        Calendar nullDateCal = new CalendarGetTimeReturnsNull();
        assertThrows(NullPointerException.class, () -> CalendarConversions.toSqlDate(nullDateCal, null), "If Calendar#getTime() returns null, a NPE should be thrown");
    }

    @Test
    @DisplayName("toSqlDate propagates IllegalStateException from Date#getTime()")
    void toSqlDateBadDateThrowsIllegalStateException() {
        Calendar badDateCal = new CalendarReturningBadDate();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CalendarConversions.toSqlDate(badDateCal, null));
        assertEquals("BadDate getTime", ex.getMessage());
    }

    @Test
    @DisplayName("toSqlDate ignores a non‑null Converter that throws on convert()")
    void toSqlDateIgnoresThrowingConverter() {
        // GIVEN – a normal Calendar
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(Instant.parse("2023-04-01T00:00:00Z").toEpochMilli());
        // AND – a mock Converter that throws on any convert call
        Converter throwingConverter = mock(Converter.class);
        when(throwingConverter.convert(any(), any())).thenThrow(new RuntimeException("Should not be called"));
        // WHEN – conversion is performed
        Date result = CalendarConversions.toSqlDate(calendar, throwingConverter);
        // THEN – conversion succeeds and the converter was never invoked
        assertEquals(calendar.getTimeInMillis(), result.getTime(), "Resulting epoch must match the Calendar's epoch");
        verifyNoInteractions(throwingConverter);
    }
}
