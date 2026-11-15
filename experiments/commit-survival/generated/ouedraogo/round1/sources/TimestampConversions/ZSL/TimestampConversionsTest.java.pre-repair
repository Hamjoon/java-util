package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TimestampConversionsTest {

    private Converter converter;
    private Timestamp testTimestamp;
    private Instant testInstant;
    private ZoneId testZoneId;
    private ZoneOffset testZoneOffset;

    @Before
    public void setUp() {
        // Use the library's default options – they contain a ZoneId and TimeZone.
        // The concrete class for ConverterOptions is part of the library; we rely on its
        // no‑arg constructor to give us sensible defaults (UTC).
        ConverterOptions options = new ConverterOptions();   // defaults to UTC
        converter = new Converter(options);

        // Choose a fixed point in time with nanosecond precision.
        testInstant = Instant.parse("2021-03-14T15:09:26.123456789Z");
        testTimestamp = Timestamp.from(testInstant);

        testZoneId = converter.getOptions().getZoneId();          // should be UTC by default
        testZoneOffset = testZoneId.getRules().getOffset(testInstant);
    }

    @Test
    public void testToDuration() {
        Duration duration = TimestampConversions.toDuration(testTimestamp, converter);
        Duration expected = Duration.between(Instant.EPOCH, testInstant);
        assertEquals("Duration should represent the exact epoch offset", expected, duration);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = TimestampConversions.toLocalDateTime(testTimestamp, converter);
        LocalDateTime expected = testInstant.atZone(testZoneId).toLocalDateTime();
        assertEquals("LocalDateTime conversion must respect the converter's ZoneId", expected, ldt);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(testTimestamp, converter);
        OffsetDateTime expected = testInstant.atZone(testZoneId).toOffsetDateTime();
        assertEquals("OffsetDateTime conversion must respect the converter's ZoneId", expected, odt);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = TimestampConversions.toCalendar(testTimestamp, converter);
        // Calendar should be in the same time‑zone as the ConverterOptions
        assertEquals("Calendar time zone must match converter's time zone",
                converter.getOptions().getTimeZone(), cal.getTimeZone());
        // Milliseconds must match the timestamp's millisecond value
        assertEquals("Calendar milliseconds must equal timestamp milliseconds",
                testTimestamp.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = TimestampConversions.toMap(testTimestamp, converter);

        // Verify required keys exist
        assertTrue("Map must contain DATE key", map.containsKey(MapConversions.DATE));
        assertTrue("Map must contain TIME key", map.containsKey(MapConversions.TIME));
        assertTrue("Map must contain ZONE key", map.containsKey(MapConversions.ZONE));
        assertTrue("Map must contain EPOCH_MILLIS key", map.containsKey(MapConversions.EPOCH_MILLIS));
        assertTrue("Map must contain NANOS key", map.containsKey(MapConversions.NANOS));

        // Re‑create the OffsetDateTime used inside the method for expected values
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(testTimestamp, converter);

        assertEquals("DATE should be the ISO local date string",
                odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals("TIME should be the ISO local time string",
                odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals("ZONE should be the converter's ZoneId string",
                converter.getOptions().getZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals("EPOCH_MILLIS should be the timestamp's epoch milli value",
                testTimestamp.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals("NANOS should be the nanosecond part of the OffsetDateTime",
                odt.getNano(), map.get(MapConversions.NANOS));
    }

    @Test
    public void testToDouble() {
        double result = TimestampConversions.toDouble(testTimestamp, converter);

        // Expected double = seconds since epoch + nanos / 1_000_000_000
        Duration dur = Duration.between(Instant.EPOCH, testInstant);
        double expected = dur.getSeconds() + dur.getNano() / 1_000_000_000.0;

        assertEquals("Double conversion must represent seconds with fractional nanos",
                expected, result, 1e-12);
    }

    @Test
    public void testToBigInteger() {
        BigInteger result = TimestampConversions.toBigInteger(testTimestamp, converter);
        // Duration to BigInteger is defined in DurationConversions – it represents total nanoseconds.
        Duration dur = Duration.between(Instant.EPOCH, testInstant);
        BigInteger expected = BigInteger.valueOf(dur.getSeconds()).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(dur.getNano()));
        assertEquals("BigInteger conversion must equal total nanoseconds", expected, result);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal result = TimestampConversions.toBigDecimal(testTimestamp, converter);
        // InstantConversions.toBigDecimal creates a decimal of epoch seconds with nanos as fraction.
        // We mimic that calculation here.
        Duration dur = Duration.between(Instant.EPOCH, testInstant);
        BigDecimal seconds = new BigDecimal(dur.getSeconds());
        BigDecimal nanosFraction = new BigDecimal(dur.getNano()).divide(new BigDecimal(1_000_000_000));
        BigDecimal expected = seconds.add(nanosFraction);
        assertEquals("BigDecimal conversion must represent epoch seconds with nanos fraction",
                0, expected.compareTo(result));
    }
}
