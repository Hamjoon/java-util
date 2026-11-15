package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
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
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateTimeConversions}.
 * <p>
 * The tests cover every static conversion method in the class, verifying that
 * the result matches the same conversion performed with the JDK core API.
 * <p>
 * A {@link Converter} instance is created with deterministic options (UTC
 * zone) so that the results are repeatable regardless of the environment in
 * which the tests run.
 */
public class LocalDateTimeConversionsTest {

    private static final ZoneId ZONE_ID = ZoneId.of("UTC");
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final LocalDateTime SAMPLE_LDT = LocalDateTime.of(2023, Month.MARCH, 25,
            12, 34, 56, 123_456_789); // 2023‑03‑25T12:34:56.123456789

    private Converter converter;

    /**
     * Build a {@link Converter} with {@link ConverterOptions} that use the UTC
     * time‑zone.  All conversion methods in {@code LocalDateTimeConversions}
     * rely on {@code converter.getOptions().getZoneId()} and
     * {@code converter.getOptions().getTimeZone()}, so a fully‑initialised
     * options object is required.
     */
    @Before
    public void setUp() {
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZONE_ID);
        options.setTimeZone(TIME_ZONE);
        converter = new Converter(options);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = ZonedDateTime.of(SAMPLE_LDT, ZONE_ID);
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(TIME_ZONE.getOffset(System.currentTimeMillis()) / 1000);
        OffsetDateTime expected = SAMPLE_LDT.atOffset(offset);
        OffsetDateTime actual = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant() {
        Instant expected = SAMPLE_LDT.atZone(ZONE_ID).toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        long expected = SAMPLE_LDT.atZone(ZONE_ID).toInstant().toEpochMilli();
        long actual = LocalDateTimeConversions.toLong(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        // InstantConversions.toDouble returns epoch seconds with fractional nanos.
        // The JDK equivalent is epochMilli / 1000.0
        double expected = SAMPLE_LDT.atZone(ZONE_ID).toInstant().toEpochMilli() / 1000.0;
        double actual = LocalDateTimeConversions.toDouble(SAMPLE_LDT, converter);
        assertEquals(expected, actual, 0.000001);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = SAMPLE_LDT;
        LocalDateTime actual = LocalDateTimeConversions.toLocalDateTime(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = SAMPLE_LDT.toLocalDate();
        LocalDate actual = LocalDateTimeConversions.toLocalDate(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = SAMPLE_LDT.toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToAtomicLong() {
        long epochMilli = SAMPLE_LDT.atZone(ZONE_ID).toInstant().toEpochMilli();
        AtomicLong expected = new AtomicLong(epochMilli);
        AtomicLong actual = LocalDateTimeConversions.toAtomicLong(SAMPLE_LDT, converter);
        assertEquals(expected.get(), actual.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(SAMPLE_LDT.atZone(ZONE_ID).toInstant());
        Timestamp actual = LocalDateTimeConversions.toTimestamp(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        long epochMilli = SAMPLE_LDT.atZone(ZONE_ID).toInstant().toEpochMilli();
        Calendar actual = LocalDateTimeConversions.toCalendar(SAMPLE_LDT, converter);
        assertEquals(TIME_ZONE, actual.getTimeZone());
        assertEquals(epochMilli, actual.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        long epochMilli = SAMPLE_LDT.atZone(ZONE_ID).toInstant().toEpochMilli();
        java.sql.Date expected = new java.sql.Date(epochMilli);
        java.sql.Date actual = LocalDateTimeConversions.toSqlDate(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        long epochMilli = SAMPLE_LDT.atZone(ZONE_ID).toInstant().toEpochMilli();
        Date expected = new Date(epochMilli);
        Date actual = LocalDateTimeConversions.toDate(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        long epochMilli = SAMPLE_LDT.atZone(ZONE_ID).toInstant().toEpochMilli();
        BigInteger expected = BigInteger.valueOf(epochMilli);
        BigInteger actual = LocalDateTimeConversions.toBigInteger(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        long epochMilli = SAMPLE_LDT.atZone(ZONE_ID).toInstant().toEpochMilli();
        BigDecimal expected = new BigDecimal(epochMilli);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(SAMPLE_LDT, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToString() {
        String expected = SAMPLE_LDT.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String actual = LocalDateTimeConversions.toString(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        Map<?, ?> map = LocalDateTimeConversions.toMap(SAMPLE_LDT, converter);
        assertTrue(map instanceof Map);
        assertEquals(2, map.size());

        // MapConversions defines the keys used; they are static final strings.
        // We reference them directly.
        String dateKey = MapConversions.DATE;
        String timeKey = MapConversions.TIME;

        assertEquals(SAMPLE_LDT.toLocalDate().toString(), map.get(dateKey));
        assertEquals(SAMPLE_LDT.toLocalTime().toString(), map.get(timeKey));
    }
}
