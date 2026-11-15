package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link DoubleConversions}.
 *
 * The tests cover all static conversion methods, exercising normal, boundary
 * and error conditions.  A {@link Converter} instance with deterministic
 * {@link ConverterOptions} (UTC zone / timezone) is created once for the
 * whole test class.
 */
public class DoubleConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The concrete implementation of ConverterOptions is not shown in the
        // source snippet, but it is part of the public API of the library.
        // We create it with a known ZoneId/TimeZone (UTC) so that the results
        // of zone‑sensitive conversions are predictable.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- */
    /*  toInstant                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToInstant_WholeSeconds() {
        Double src = 1_600_000_000d; // epoch seconds for 2020‑09‑13T12:26:40Z
        Instant expected = Instant.ofEpochSecond(1_600_000_000L, 0L);
        assertEquals(expected, DoubleConversions.toInstant(src, converter));
    }

    @Test
    public void testToInstant_WithFractionalNanos_RoundsCorrectly() {
        // 0.123456789 seconds -> 123,456,789 nanoseconds
        Double src = 0.123456789d;
        Instant expected = Instant.ofEpochSecond(0L, 123_456_789L);
        assertEquals(expected, DoubleConversions.toInstant(src, converter));

        // Fraction that should round up (0.9999999995 -> 1_000_000_000 ns -> next second)
        Double srcRoundUp = 0.9999999995d;
        Instant expectedRoundUp = Instant.ofEpochSecond(1L, 0L);
        assertEquals(expectedRoundUp, DoubleConversions.toInstant(srcRoundUp, converter));
    }

    /* --------------------------------------------------------------------- */
    /*  toDate                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDate_BasicConversion() {
        Double src = 1_600_000_000.123d; // milliseconds part ignored
        Date expected = new Date((long) (1_600_000_000.123d * 1000));
        assertEquals(expected, DoubleConversions.toDate(src, converter));
    }

    @Test
    public void testToDate_NegativeEpoch() {
        Double src = -86400.0; // exactly one day before epoch
        Date expected = new Date(-86_400_000L);
        assertEquals(expected, DoubleConversions.toDate(src, converter));
    }

    /* --------------------------------------------------------------------- */
    /*  toTimestamp                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToTimestamp_MatchesInstant() {
        Double src = 1_600_000_000.987654321d;
        Instant instant = DoubleConversions.toInstant(src, converter);
        Timestamp expected = Timestamp.from(instant);
        assertEquals(expected, DoubleConversions.toTimestamp(src, converter));
    }

    /* --------------------------------------------------------------------- */
    /*  toDuration                                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDuration_WholeAndFractional() {
        Double src = 12345.678901234d;
        Duration expected = Duration.ofSeconds(12_345L, 678_901_234L);
        assertEquals(expected, DoubleConversions.toDuration(src, converter));
    }

    @Test
    public void testToDuration_RoundingEdge() {
        // 0.9999999995 seconds should round to 1_000_000_000 nanoseconds -> next second
        Double src = 0.9999999995d;
        Duration expected = Duration.ofSeconds(1L, 0L);
        assertEquals(expected, DoubleConversions.toDuration(src, converter));
    }

    /* --------------------------------------------------------------------- */
    /*  toSqlDate                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToSqlDate_UTCDate() {
        // 2021‑01‑01T00:00:00Z => epoch seconds 1609459200
        Double src = 1_609_459_200d;
        java.sql.Date expected = java.sql.Date.valueOf(LocalDate.of(2021, 1, 1));
        assertEquals(expected, DoubleConversions.toSqlDate(src, converter));
    }

    /* --------------------------------------------------------------------- */
    /*  toCalendar                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCalendar_EpochMillis() {
        Double src = 1_600_000_000.5d; // 1_600_000_000.5 seconds
        long expectedMillis = (long) (1_600_000_000.5d * 1000);
        Calendar cal = DoubleConversions.toCalendar(src, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(expectedMillis, cal.getTimeInMillis());
    }

    /* --------------------------------------------------------------------- */
    /*  toLocalTime                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLocalTime_NormalValue() {
        // 13:45:30.123456789 => seconds = 13*3600 + 45*60 + 30.123456789
        double seconds = 13 * 3600 + 45 * 60 + 30.123456789;
        LocalTime expected = LocalTime.of(13, 45, 30, 123_456_789);
        assertEquals(expected, DoubleConversions.toLocalTime(seconds, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_NegativeThrows() {
        DoubleConversions.toLocalTime(-1.0, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLargeThrows() {
        // 24 hours = 86400 seconds, which is out of range (max 86399.999999999)
        DoubleConversions.toLocalTime(86_400.0, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  toOffsetTime                                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToOffsetTime_NormalValue() {
        // 02:30:15.250 seconds => 2*3600 + 30*60 + 15.250 = 9015.25
        double seconds = 9_015.25;
        Instant instant = Instant.ofEpochSecond(9_015L, 250_000_000L);
        OffsetTime expected = OffsetTime.ofInstant(instant, ZoneId.of("UTC"));
        assertEquals(expected, DoubleConversions.toOffsetTime(seconds, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_NegativeThrows() {
        DoubleConversions.toOffsetTime(-0.1, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_TooLargeThrows() {
        DoubleConversions.toOffsetTime(86_401.0, converter); // beyond one day
    }

    /* --------------------------------------------------------------------- */
    /*  toZonedDateTime, toLocalDate, toLocalDateTime, toOffsetDateTime      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testZonedDateTimeAndRelatedConversions() {
        // 2022‑03‑14T15:09:26.535Z -> epoch seconds 1_645_188_566.535
        double src = 1_645_188_566.535;
        Instant instant = DoubleConversions.toInstant(src, converter);
        ZonedDateTime zdt = DoubleConversions.toZonedDateTime(src, converter);
        LocalDate ld = DoubleConversions.toLocalDate(src, converter);
        LocalDateTime ldt = DoubleConversions.toLocalDateTime(src, converter);
        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(src, converter);

        // All should represent the same point in time in UTC
        assertEquals(instant.atZone(ZoneId.of("UTC")), zdt);
        assertEquals(LocalDate.of(2022, 3, 14), ld);
        assertEquals(LocalDateTime.of(2022, 3, 14, 15, 9, 26, 535_000_000), ldt);
        assertEquals(OffsetDateTime.ofInstant(instant, ZoneId.of("UTC")), odt);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper method to create a Converter with deterministic options        */
    /* --------------------------------------------------------------------- */

    private static final class ConverterOptions {
        private ZoneId zoneId = ZoneId.of("UTC");
        private TimeZone timeZone = TimeZone.getTimeZone("UTC");

        public ZoneId getZoneId() {
            return zoneId;
        }

        public void setZoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }

        public void setTimeZone(TimeZone timeZone) {
            this.timeZone = timeZone;
        }
    }

    /**
     * Minimal stub of the public {@link Converter} class required for the
     * tests.  It forwards the supplied {@link ConverterOptions} via
     * {@link #getOptions()}.
     */
    private static final class Converter {
        private final ConverterOptions options;

        public Converter(ConverterOptions options) {
            this.options = options;
        }

        public ConverterOptions getOptions() {
            return options;
        }
    }
}
