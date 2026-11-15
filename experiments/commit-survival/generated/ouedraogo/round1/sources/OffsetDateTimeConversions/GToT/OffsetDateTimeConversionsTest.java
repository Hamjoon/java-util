package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@code OffsetDateTimeConversions}.
 *
 * The test class covers:
 * <ul>
 *   <li>basic happy‑path verification for every public static conversion method,</li>
 *   <li>edge‑case handling (epoch start, extreme offsets, maximum/minimum values),</li>
 *   <li>exception scenarios (null argument, wrong source type).</li>
 * </ul>
 *
 * The library supplies {@link Converter} and {@link ConverterOptions}.  The tests use a
 * default {@code ConverterOptions} instance – it provides the system default {@link ZoneId}
 * and {@link java.util.TimeZone}, which are sufficient for the conversion logic exercised
 * here.
 */
public class OffsetDateTimeConversionsTest {

    /** A reusable {@link Converter} used by all test methods. */
    private Converter converter;

    /** Common {@link OffsetDateTime} used for most happy‑path tests. */
    private OffsetDateTime sampleOdt;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the original library.
        // It supplies the default time‑zone/zone‑id, which is suitable for our tests.
        converter = new Converter(new ConverterOptions());

        // Sample date‑time: 2022‑03‑14T15:09:26.123456789+02:00
        sampleOdt = OffsetDateTime.of(
                LocalDateTime.of(2022, Month.MARCH, 14, 15, 9, 26, 123_456_789),
                ZoneOffset.ofHours(2));
    }

    /* --------------------------------------------------------------------- */
    /*  Basic happy‑path tests                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToInstant() {
        Instant expected = sampleOdt.toInstant();
        Instant actual = OffsetDateTimeConversions.toInstant(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        long expected = sampleOdt.toInstant().toEpochMilli();
        long actual = OffsetDateTimeConversions.toLong(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = sampleOdt.toInstant()
                .atZone(converter.getOptions().getZoneId())
                .toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = sampleOdt.toInstant()
                .atZone(converter.getOptions().getZoneId())
                .toLocalDate();
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = sampleOdt.toInstant()
                .atZone(converter.getOptions().getZoneId())
                .toLocalTime();
        LocalTime actual = OffsetDateTimeConversions.toLocalTime(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToAtomicLong() {
        long expected = sampleOdt.toInstant().toEpochMilli();
        AtomicLong actual = OffsetDateTimeConversions.toAtomicLong(sampleOdt, converter);
        assertEquals(expected, actual.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(sampleOdt.toInstant());
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = OffsetDateTimeConversions.toCalendar(sampleOdt, converter);
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
        assertEquals(sampleOdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = new java.sql.Date(sampleOdt.toInstant().toEpochMilli());
        java.sql.Date actual = OffsetDateTimeConversions.toSqlDate(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = sampleOdt.toInstant().atZone(converter.getOptions().getZoneId());
        ZonedDateTime actual = OffsetDateTimeConversions.toZonedDateTime(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        Date expected = new Date(sampleOdt.toInstant().toEpochMilli());
        Date actual = OffsetDateTimeConversions.toDate(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        Instant instant = sampleOdt.toInstant();
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = sampleOdt.toInstant();
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(sampleOdt, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToOffsetTime() {
        OffsetTime expected = sampleOdt.toOffsetTime();
        OffsetTime actual = OffsetDateTimeConversions.toOffsetTime(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToString() {
        String expected = sampleOdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String actual = OffsetDateTimeConversions.toString(sampleOdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = OffsetDateTimeConversions.toMap(sampleOdt, converter);
        ZonedDateTime zdt = sampleOdt.toInstant().atZone(converter.getOptions().getZoneId());

        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(zdt.getOffset().toString(), map.get(MapConversions.OFFSET));
        assertTrue(map instanceof CompactLinkedMap);
    }

    @Test
    public void testToDouble() {
        Instant instant = sampleOdt.toInstant();
        double expected = BigDecimalConversions
                .secondsAndNanosToDouble(instant.getEpochSecond(), instant.getNano())
                .doubleValue();
        double actual = OffsetDateTimeConversions.toDouble(sampleOdt, converter);
        assertEquals(expected, actual, 0.0);
    }

    /* --------------------------------------------------------------------- */
    /*  Edge‑case tests                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEpochStart() {
        OffsetDateTime epoch = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
        assertEquals(0L, OffsetDateTimeConversions.toLong(epoch, converter));
        assertEquals(0.0, OffsetDateTimeConversions.toDouble(epoch, converter), 0.0);
        assertEquals("1970-01-01T00:00:00Z", OffsetDateTimeConversions.toString(epoch, converter));
    }

    @Test
    public void testExtremePositiveOffset() {
        // Max allowed offset is +18:00 (according to ISO‑8601)
        ZoneOffset maxOffset = ZoneOffset.ofHoursMinutes(18, 0);
        OffsetDateTime odt = OffsetDateTime.of(2000, 1, 1, 12, 0, 0, 0, maxOffset);
        // Ensure conversion respects the offset when turning into a ZonedDateTime
        ZonedDateTime zdt = OffsetDateTimeConversions.toZonedDateTime(odt, converter);
        assertEquals(maxOffset, zdt.getOffset());
    }

    @Test
    public void testExtremeNegativeOffset() {
        // Minimum allowed offset is -18:00
        ZoneOffset minOffset = ZoneOffset.ofHoursMinutes(-18, 0);
        OffsetDateTime odt = OffsetDateTime.of(2000, 1, 1, 12, 0, 0, 0, minOffset);
        ZonedDateTime zdt = OffsetDateTimeConversions.toZonedDateTime(odt, converter);
        assertEquals(minOffset, zdt.getOffset());
    }

    @Test
    public void testFarFutureDate() {
        // Year 3000‑01‑01T00:00:00+00:00
        OffsetDateTime farFuture = OffsetDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        long epochMilli = OffsetDateTimeConversions.toLong(farFuture, converter);
        assertTrue(epochMilli > 0);
        // Verify that the double conversion does not overflow
        double d = OffsetDateTimeConversions.toDouble(farFuture, converter);
        assertTrue(Double.isFinite(d));
    }

    @Test
    public void testFarPastDate() {
        // Year -1000‑01‑01T00:00:00+00:00  (supported by java.time)
        OffsetDateTime farPast = OffsetDateTime.of(-1000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        long epochMilli = OffsetDateTimeConversions.toLong(farPast, converter);
        // epochMilli will be negative
        assertTrue(epochMilli < 0);
        double d = OffsetDateTimeConversions.toDouble(farPast, converter);
        assertTrue(Double.isFinite(d));
    }

    /* --------------------------------------------------------------------- */
    /*  Exception (error) tests                                             */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        OffsetDateTimeConversions.toInstant(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstant_WrongType() {
        OffsetDateTimeConversions.toInstant("not an OffsetDateTime", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        OffsetDateTimeConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        OffsetDateTimeConversions.toLong(12345, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        OffsetDateTimeConversions.toString(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        OffsetDateTimeConversions.toString(new Date(), converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        OffsetDateTimeConversions.toMap(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType() {
        OffsetDateTimeConversions.toMap(Instant.now(), converter);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods (if any)                                             */
    /* --------------------------------------------------------------------- */
    // No helper methods are required for this test suite; all logic resides
    // within the individual test cases.
}
