package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


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

import org.junit.Test;

/**
 * Unit tests for {@link InstantConversions}. All public static methods are exercised
 * with a variety of {@link Instant} values and with explicit {@link ConverterOptions}
 * (zoneId / timeZone) to guarantee deterministic behaviour.
 */
public class InstantConversionsTest {

    private static final ZoneId ZONE_UTC = ZoneId.of("UTC");
    private static final ZoneId ZONE_NEW_YORK = ZoneId.of("America/New_York");
    private static final TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone TZ_NEW_YORK = TimeZone.getTimeZone("America/New_York");

    /** Helper that creates a {@link Converter} whose options contain the supplied zoneId & timeZone. */
    private static Converter createConverter(ZoneId zoneId, TimeZone timeZone) {
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zoneId);
        options.setTimeZone(timeZone);
        return new Converter(options);
    }

    /** Instant representing the epoch (1970‑01‑01T00:00:00Z). */
    private static final Instant EPOCH = Instant.ofEpochSecond(0L, 0);

    /** Instant with a non‑zero second and nanosecond component. */
    private static final Instant SAMPLE = Instant.ofEpochSecond(
            1615683599L,               // 2021‑03‑14T01:59:59Z (just before DST change in NY)
            123_456_789);              // 123.456789 ms

    @Test
    public void testToMap_Epoch() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        Map map = InstantConversions.toMap(EPOCH, conv);
        assertEquals(0L, map.get("seconds"));
        assertEquals(0,   map.get("nanos"));
        assertTrue(map instanceof CompactLinkedMap);
    }

    @Test
    public void testToMap_Sample() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        Map<String, Object> map = InstantConversions.toMap(SAMPLE, conv);
        assertEquals(1615683599L, map.get("seconds"));
        assertEquals(123_456_789, map.get("nanos"));
    }

    @Test
    public void testToZonedDateTime_WithZoneId() {
        Converter conv = createConverter(ZONE_NEW_YORK, TZ_NEW_YORK);
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(SAMPLE, conv);
        // The instant is 2021‑03‑14T01:59:59.123456789Z.
        // In New York (EST, UTC‑5) the local time is 2021‑03‑13T20:59:59.123456789‑05:00
        ZonedDateTime expected = SAMPLE.atZone(ZONE_NEW_YORK);
        assertEquals(expected, zdt);
    }

    @Test
    public void testToOffsetDateTime_WithTimeZone() {
        Converter conv = createConverter(ZONE_NEW_YORK, TZ_NEW_YORK);
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(SAMPLE, conv);
        // Offset for New York at the sample instant (just before DST starts) is -05:00
        ZoneOffset expectedOffset = ZoneOffset.ofHours(-5);
        OffsetDateTime expected = SAMPLE.atOffset(expectedOffset);
        assertEquals(expected, odt);
    }

    @Test
    public void testToLong_EpochAndSample() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        assertEquals(0L, InstantConversions.toLong(EPOCH, conv));

        long expectedMillis = SAMPLE.toEpochMilli(); // uses built‑in conversion
        assertEquals(expectedMillis, InstantConversions.toLong(SAMPLE, conv));
    }

    @Test
    public void testToDouble_EpochAndSample() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        // epoch -> 0.0 seconds
        assertEquals(0.0, InstantConversions.toDouble(EPOCH, conv), 0.0);

        // compute expected double via the same helper used in the production code
        double expected = BigDecimalConversions
                .secondsAndNanosToDouble(SAMPLE.getEpochSecond(), SAMPLE.getNano())
                .doubleValue();
        assertEquals(expected, InstantConversions.toDouble(SAMPLE, conv), 0.0);
    }

    @Test
    public void testToAtomicLong() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        AtomicLong atomic = InstantConversions.toAtomicLong(SAMPLE, conv);
        assertEquals(SAMPLE.toEpochMilli(), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        Timestamp ts = InstantConversions.toTimestamp(SAMPLE, conv);
        assertEquals(Timestamp.from(SAMPLE), ts);
    }

    @Test
    public void testToSqlDate() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        java.sql.Date sqlDate = InstantConversions.toSqlDate(SAMPLE, conv);
        assertEquals(new java.sql.Date(SAMPLE.toEpochMilli()), sqlDate);
    }

    @Test
    public void testToDate() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        Date date = InstantConversions.toDate(SAMPLE, conv);
        assertEquals(new Date(SAMPLE.toEpochMilli()), date);
    }

    @Test
    public void testToCalendar() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        Calendar cal = InstantConversions.toCalendar(SAMPLE, conv);
        assertEquals(SAMPLE.toEpochMilli(), cal.getTimeInMillis());

        // Verify that the Calendar respects the TimeZone supplied in the ConverterOptions
        Converter convNY = createConverter(ZONE_NEW_YORK, TZ_NEW_YORK);
        Calendar calNY = InstantConversions.toCalendar(SAMPLE, convNY);
        assertEquals(TZ_NEW_YORK, calNY.getTimeZone());
        assertEquals(SAMPLE.toEpochMilli(), calNY.getTimeInMillis());
    }

    @Test
    public void testToBigInteger() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        BigInteger bi = InstantConversions.toBigInteger(SAMPLE, conv);
        // expected = seconds * 1_000_000_000 + nanos
        BigInteger expected = BigInteger.valueOf(SAMPLE.getEpochSecond())
                .multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(SAMPLE.getNano()));
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigDecimal() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        BigDecimal bd = InstantConversions.toBigDecimal(SAMPLE, conv);
        BigDecimal expected = BigDecimalConversions
                .secondsAndNanosToDouble(SAMPLE.getEpochSecond(), SAMPLE.getNano());
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testLocalDateTimeDateAndTime() {
        Converter conv = createConverter(ZONE_NEW_YORK, TZ_NEW_YORK);
        LocalDateTime ldt = InstantConversions.toLocalDateTime(SAMPLE, conv);
        ZonedDateTime zdt = SAMPLE.atZone(ZONE_NEW_YORK);
        assertEquals(zdt.toLocalDateTime(), ldt);
    }

    @Test
    public void testLocalDate() {
        Converter conv = createConverter(ZONE_NEW_YORK, TZ_NEW_YORK);
        LocalDate ld = InstantConversions.toLocalDate(SAMPLE, conv);
        ZonedDateTime zdt = SAMPLE.atZone(ZONE_NEW_YORK);
        assertEquals(zdt.toLocalDate(), ld);
    }

    @Test
    public void testLocalTime() {
        Converter conv = createConverter(ZONE_NEW_YORK, TZ_NEW_YORK);
        LocalTime lt = InstantConversions.toLocalTime(SAMPLE, conv);
        ZonedDateTime zdt = SAMPLE.atZone(ZONE_NEW_YORK);
        assertEquals(zdt.toLocalTime(), lt);
    }

    @Test(expected = NullPointerException.class)
    public void testNullInstantThrowsNPE() {
        Converter conv = createConverter(ZONE_UTC, TZ_UTC);
        // All conversion methods assume a non‑null Instant; a NPE is the expected behaviour.
        InstantConversions.toLong(null, conv);
    }
}
