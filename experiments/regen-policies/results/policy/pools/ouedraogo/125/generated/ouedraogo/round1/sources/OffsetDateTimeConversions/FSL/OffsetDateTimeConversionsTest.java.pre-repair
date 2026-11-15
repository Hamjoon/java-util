package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link OffsetDateTimeConversions}.
 *
 * <p>
 * The tests construct a real {@link OffsetDateTime} instance and a real {@link Converter}
 * (using the default {@code ConverterOptions}) and then verify the result of each static
 * conversion method against the expected Java‑time values.
 * </p>
 */
public class OffsetDateTimeConversionsTest {

    private static final OffsetDateTime SAMPLE_ODT = OffsetDateTime.parse("2023-01-02T12:34:56.789+02:00");
    // The default ConverterOptions (as supplied by the library) uses UTC for both
    // TimeZone and ZoneId.  This is the behaviour we rely on in the tests.
    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // The default instance yields UTC time‑zone / zone‑id.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToInstant() {
        Instant expected = SAMPLE_ODT.toInstant();
        Instant actual = OffsetDateTimeConversions.toInstant(SAMPLE_ODT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        long expected = SAMPLE_ODT.toInstant().toEpochMilli();
        long actual = OffsetDateTimeConversions.toLong(SAMPLE_ODT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        assertEquals(SAMPLE_ODT.toLocalDateTime(),
                OffsetDateTimeConversions.toLocalDateTime(SAMPLE_ODT, converter));
    }

    @Test
    public void testToLocalDate() {
        assertEquals(SAMPLE_ODT.toLocalDate(),
                OffsetDateTimeConversions.toLocalDate(SAMPLE_ODT, converter));
    }

    @Test
    public void testToLocalTime() {
        assertEquals(SAMPLE_ODT.toLocalTime(),
                OffsetDateTimeConversions.toLocalTime(SAMPLE_ODT, converter));
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong expected = new AtomicLong(SAMPLE_ODT.toInstant().toEpochMilli());
        AtomicLong actual = OffsetDateTimeConversions.toAtomicLong(SAMPLE_ODT, converter);
        assertEquals(expected.get(), actual.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(SAMPLE_ODT.toInstant());
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(SAMPLE_ODT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = OffsetDateTimeConversions.toCalendar(SAMPLE_ODT, converter);
        // The converter's default TimeZone is UTC.
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(SAMPLE_ODT.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = new java.sql.Date(SAMPLE_ODT.toInstant().toEpochMilli());
        java.sql.Date actual = OffsetDateTimeConversions.toSqlDate(SAMPLE_ODT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime() {
        ZoneId expectedZone = converter.getOptions().getZoneId(); // default UTC
        ZonedDateTime expected = SAMPLE_ODT.toInstant().atZone(expectedZone);
        ZonedDateTime actual = OffsetDateTimeConversions.toZonedDateTime(SAMPLE_ODT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        Date expected = new Date(SAMPLE_ODT.toInstant().toEpochMilli());
        Date actual = OffsetDateTimeConversions.toDate(SAMPLE_ODT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        // InstantConversions.toBigInteger ultimately returns the epoch‑millis as a BigInteger.
        // Verify that behaviour matches the contract of this conversion.
        BigInteger expected = BigInteger.valueOf(SAMPLE_ODT.toInstant().toEpochMilli());
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(SAMPLE_ODT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        // InstantConversions.toBigDecimal returns a BigDecimal representing the
        // instant in seconds with fractional nanoseconds.
        Instant instant = SAMPLE_ODT.toInstant();
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9));
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(SAMPLE_ODT, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToOffsetTime() {
        assertEquals(SAMPLE_ODT.toOffsetTime(),
                OffsetDateTimeConversions.toOffsetTime(SAMPLE_ODT, converter));
    }

    @Test
    public void testToString() {
        String expected = SAMPLE_ODT.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String actual = OffsetDateTimeConversions.toString(SAMPLE_ODT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = OffsetDateTimeConversions.toMap(SAMPLE_ODT, converter);
        // Expected values derived from the ZonedDateTime produced by the conversion.
        ZonedDateTime zdt = OffsetDateTimeConversions.toZonedDateTime(SAMPLE_ODT, converter);

        // The MapConversions constants are public static final strings; they resolve to
        // "date", "time", and "offset".  Use the literal keys to avoid a direct dependency.
        assertEquals(3, map.size());
        assertEquals(zdt.toLocalDate().toString(), map.get("date"));
        assertEquals(zdt.toLocalTime().toString(), map.get("time"));
        assertEquals(zdt.getOffset().toString(), map.get("offset"));
    }

    @Test
    public void testToDouble() {
        // Re‑use the same calculation performed inside the method.
        Instant instant = SAMPLE_ODT.toInstant();
        double expected = BigDecimalConversions.secondsAndNanosToDouble(
                instant.getEpochSecond(),
                instant.getNano()).doubleValue();

        double actual = OffsetDateTimeConversions.toDouble(SAMPLE_ODT, converter);
        assertEquals(expected, actual, 0.0);
    }
}
