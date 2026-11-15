package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toCalendar_Test {

    private Timestamp timestamp;

    private ConverterOptions options;

    private Converter converter;

    class MyGregorianCalendar extends GregorianCalendar {

        private static final long serialVersionUID = 1L;

        boolean created = false;

        MyGregorianCalendar(TimeZone tz) {
            super(tz);
            this.created = true;
        }
    }

    class BadTimestamp extends Timestamp {

        BadTimestamp(long time) {
            super(time);
        }

        @Override
        public int getNanos() {
            throw new RuntimeException("Nanos not available");
        }
    }

    class MyGregorianCalendar extends GregorianCalendar {

        private static final long serialVersionUID = 1L;

        boolean created = false;

        MyGregorianCalendar(TimeZone tz) {
            super(tz);
            this.created = true;
        }
    }

    class BadTimestamp extends Timestamp {

        BadTimestamp(long time) {
            super(time);
        }

        @Override
        public int getNanos() {
            throw new RuntimeException("Nanos not available");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A deterministic timestamp (2023‑01‑01 12:34:56.123 UTC)
        timestamp = Timestamp.valueOf("2023-01-01 12:34:56.123");
        // Create ConverterOptions and explicitly set a known time‑zone.
        options = new ConverterOptions();
        // UTC+2
        options.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
        // Build the Converter that will be passed to the method under test.
        converter = new Converter(options);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Zero‑epoch Timestamp, UTC
        Arguments.of(Timestamp.from(java.time.Instant.EPOCH), TimeZone.getTimeZone("UTC"), "Zero‑epoch Timestamp with UTC", 0L), // 2023‑01‑01T12:34:56.789, America/Los_Angeles (PST/PDT)
        Arguments.of(Timestamp.valueOf("2023-01-01 12:34:56.789"), TimeZone.getTimeZone("America/Los_Angeles"), "Non‑zero Timestamp with America/Los_Angeles", 1672576496789L));
    }

    private static Stream<Arguments> invalidInputProvider() {
        // Helper to create a Converter with UTC timezone
        ConverterOptions utcOpts = new ConverterOptions();
        utcOpts.setTimeZone(TimeZone.getTimeZone("UTC"));
        Converter utcConv = new Converter(utcOpts);
        return Stream.of(// null `from`
        Arguments.of(null, utcConv, NullPointerException.class, "Passing null `from` should throw NPE"), // Wrong type (java.util.Date)
        Arguments.of(new Date(), utcConv, ClassCastException.class, "Passing a non‑Timestamp object should throw CCE"), // null `converter`
        Arguments.of(new Timestamp(System.currentTimeMillis()), null, NullPointerException.class, "Null converter leads to NPE when accessing options"), // Converter with null TimeZone
        Arguments.of(new Timestamp(System.currentTimeMillis()), // options.timeZone == null
        new Converter(new ConverterOptions()), NullPointerException.class, "Null TimeZone in options causes Calendar.getInstance(null) to NPE"));
    }

    private static Stream<Arguments> boundaryTimeZoneProvider() {
        return Stream.of(// Leap day 2000‑02‑29T23:59:59.999, Asia/Kolkata (+05:30)
        Arguments.of(Timestamp.valueOf("2000-02-29 23:59:59.999"), "Asia/Kolkata", "Leap‑day timestamp with Asia/Kolkata", 951782399999L), // DST spring‑forward gap in America/New_York (2021‑03‑14 02:30 does not exist)
        Arguments.of(new Timestamp(1615704600000L), "America/New_York", "DST spring‑forward gap (timestamp should stay unchanged)", 1615704600000L), // DST fall‑back ambiguous time in America/New_York
        Arguments.of(new Timestamp(1636266600000L), "America/New_York", "DST fall‑back ambiguous time (timestamp should stay unchanged)", 1636266600000L));
    }

    private static Stream<Arguments> extremeEpochProvider() {
        return Stream.of(Arguments.of(new Timestamp(Long.MAX_VALUE), "Maximum long epoch", Long.MAX_VALUE), Arguments.of(new Timestamp(Long.MIN_VALUE), "Minimum long epoch", Long.MIN_VALUE));
    }

    @Test
    void toCalendar_returnsCalendarWithSameInstantAsTimestamp() {
        // GIVEN – a timestamp and a converter (timezone does not affect the instant)
        // WHEN
        Calendar result = TimestampConversions.toCalendar(timestamp, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(timestamp.getTime(), result.getTimeInMillis(), "Calendar time should match the Timestamp instant");
    }

    @Test
    void toCalendar_respectsConverterTimeZone() {
        options.setTimeZone(TimeZone.getTimeZone("GMT+02:00"));
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:34:56.123");
        Calendar result = TimestampConversions.toCalendar(ts, converter);
        assertEquals(options.getTimeZone(), result.getTimeZone(), "Calendar should use the time‑zone supplied by ConverterOptions");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void toCalendar_validConversion(Timestamp ts, TimeZone tz, String description, long expectedMillis) {
        // Arrange – a Converter that returns the supplied TimeZone
        options.setTimeZone(tz);
        // Act
        Calendar cal = TimestampConversions.toCalendar(ts, converter);
        // Assert
        assertNotNull(cal, "Calendar must not be null");
        assertEquals(tz, cal.getTimeZone(), "Calendar must use the Converter's TimeZone");
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar must preserve the exact epoch milliseconds");
    }

    /* --------------------------------------------------------------
     *  Invalid input tests
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("invalidInputProvider")
    void toCalendar_invalidInputs(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> TimestampConversions.toCalendar(from, conv), description);
    }

    /* --------------------------------------------------------------
     *  Boundary time‑zone tests (leap day, DST gaps/overlaps)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("boundaryTimeZoneProvider")
    void toCalendar_boundaryTimeZones(Timestamp ts, String tzId, String description, long expectedMillis) {
        TimeZone tz = TimeZone.getTimeZone(tzId);
        options.setTimeZone(tz);
        Calendar cal = TimestampConversions.toCalendar(ts, converter);
        assertEquals(tz, cal.getTimeZone(), "TimeZone must be preserved");
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Epoch millis must be unchanged even across DST gaps/overlaps");
    }

    /* --------------------------------------------------------------
     *  Extreme epoch values
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("extremeEpochProvider")
    void toCalendar_extremeEpochValues(Timestamp ts, String description, long expectedMillis) {
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar cal = TimestampConversions.toCalendar(ts, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "UTC must be used");
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar must store the extreme epoch value without overflow");
    }

    /* --------------------------------------------------------------
     *  Converter with custom Calendar subclass (using Mockito static mock)
     * -------------------------------------------------------------- */
    @Test
    void toCalendar_respectsCustomCalendarSubclass() {
        // Custom subclass that records that it was created
        class MyGregorianCalendar extends GregorianCalendar {

            private static final long serialVersionUID = 1L;

            boolean created = false;

            MyGregorianCalendar(TimeZone tz) {
                super(tz);
                this.created = true;
            }
        }
        // Mock static Calendar.getInstance(TimeZone) to return our subclass
        try (MockedStatic<Calendar> mocked = mockStatic(Calendar.class)) {
            mocked.when(() -> Calendar.getInstance(any(TimeZone.class))).thenAnswer(invocation -> {
                TimeZone tz = invocation.getArgument(0);
                return new MyGregorianCalendar(tz);
            });
            Timestamp ts = Timestamp.valueOf("2022-12-31 23:59:59.999");
            options.setTimeZone(TimeZone.getTimeZone("UTC"));
            Calendar result = TimestampConversions.toCalendar(ts, converter);
            assertTrue(result instanceof MyGregorianCalendar, "Returned Calendar should be the custom subclass");
            MyGregorianCalendar myCal = (MyGregorianCalendar) result;
            assertTrue(myCal.created, "Custom Calendar constructor flag must be true");
            assertEquals(ts.getTime(), myCal.getTimeInMillis(), "Epoch millis must be preserved");
            assertEquals(TimeZone.getTimeZone("UTC"), myCal.getTimeZone(), "TimeZone must be the one supplied by the Converter");
        }
    }

    /* --------------------------------------------------------------
     *  Timestamp with broken getNanos() – conversion must ignore nanos.
     * -------------------------------------------------------------- */
    @Test
    void toCalendar_timestampWithFaultyNanosDoesNotAffectConversion() {
        // Subclass that throws from getNanos()
        class BadTimestamp extends Timestamp {

            BadTimestamp(long time) {
                super(time);
            }

            @Override
            public int getNanos() {
                throw new RuntimeException("Nanos not available");
            }
        }
        BadTimestamp bad = new BadTimestamp(123456789L);
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar cal = TimestampConversions.toCalendar(bad, converter);
        assertEquals(123456789L, cal.getTimeInMillis(), "Conversion must rely only on getTime(), not getNanos()");
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "TimeZone must be UTC");
    }
}
