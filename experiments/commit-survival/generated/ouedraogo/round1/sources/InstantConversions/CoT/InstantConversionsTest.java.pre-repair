package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link InstantConversions}.
 * All tests are in the same package so the package‑private static methods are directly accessible.
 */
public class InstantConversionsTest {

    // -------------------------------------------------------------------------
    // Test data shared by all tests
    // -------------------------------------------------------------------------
    private static final Instant INSTANT_EPOCH    = Instant.ofEpochSecond(0L, 0);
    private static final Instant INSTANT_POSITIVE = Instant.ofEpochSecond(1_620_000_000L, 123_456_789);
    private static final Instant INSTANT_NEGATIVE = Instant.ofEpochSecond(-1_000L, 987_654_321);
    private static final Instant INSTANT_MAX_NANO = Instant.ofEpochSecond(42L, 999_999_999);

    // The library’s ConverterOptions class provides a default ZoneId/TimeZone (system defaults).
    private static Converter DEFAULT_CONVERTER;

    @BeforeClass
    public static void setUpConverter() {
        // The real library supplies a public no‑arg constructor for ConverterOptions.
        // If the library requires a different constructor, adjust accordingly.
        DEFAULT_CONVERTER = new Converter(new ConverterOptions());
    }

    // -------------------------------------------------------------------------
    // Basic functional tests
    // -------------------------------------------------------------------------

    @Test
    public void testToMap_basic() {
        Map<String, Object> map = InstantConversions.toMap(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        assertEquals(INSTANT_POSITIVE.getEpochSecond(), map.get("seconds"));
        assertEquals(INSTANT_POSITIVE.getNano(), map.get("nanos"));
    }

    @Test
    public void testToMap_negativeAndMaxNano() {
        Map<String, Object> map = InstantConversions.toMap(INSTANT_MAX_NANO, DEFAULT_CONVERTER);
        assertEquals(INSTANT_MAX_NANO.getEpochSecond(), map.get("seconds"));
        assertEquals(INSTANT_MAX_NANO.getNano(), map.get("nanos"));
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        ZoneId expectedZone = DEFAULT_CONVERTER.getOptions().getZoneId();
        assertEquals(expectedZone, zdt.getZone());
        assertEquals(INSTANT_POSITIVE, zdt.toInstant());
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        TimeZone tz = DEFAULT_CONVERTER.getOptions().getTimeZone();
        int offsetSeconds = tz.getOffset(System.currentTimeMillis()) / 1000;
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        assertEquals(expectedOffset, odt.getOffset());
        assertEquals(INSTANT_POSITIVE, odt.toInstant());
    }

    @Test
    public void testToLong() {
        long expected = INSTANT_POSITIVE.toEpochMilli();
        assertEquals(expected, InstantConversions.toLong(INSTANT_POSITIVE, DEFAULT_CONVERTER));
    }

    @Test
    public void testToDouble() {
        double d = InstantConversions.toDouble(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        // Compute expected value with the same helper used inside the class.
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(
                INSTANT_POSITIVE.getEpochSecond(),
                INSTANT_POSITIVE.getNano());
        assertEquals(expected.doubleValue(), d, 1e-12);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = InstantConversions.toAtomicLong(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        assertEquals(INSTANT_POSITIVE.toEpochMilli(), al.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = InstantConversions.toTimestamp(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        assertEquals(INSTANT_POSITIVE, ts.toInstant());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = InstantConversions.toSqlDate(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        assertEquals(INSTANT_POSITIVE.toEpochMilli(), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Date date = InstantConversions.toDate(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        assertEquals(INSTANT_POSITIVE.toEpochMilli(), date.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = InstantConversions.toCalendar(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        assertEquals(INSTANT_POSITIVE.toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = InstantConversions.toBigInteger(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        BigInteger expected = BigInteger.valueOf(INSTANT_POSITIVE.getEpochSecond())
                .multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(INSTANT_POSITIVE.getNano()));
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = InstantConversions.toBigDecimal(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(
                INSTANT_POSITIVE.getEpochSecond(),
                INSTANT_POSITIVE.getNano());
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = InstantConversions.toLocalDateTime(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        assertEquals(zdt.toLocalDateTime(), ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate ld = InstantConversions.toLocalDate(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        assertEquals(zdt.toLocalDate(), ld);
    }

    @Test
    public void testToLocalTime() {
        LocalTime lt = InstantConversions.toLocalTime(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(INSTANT_POSITIVE, DEFAULT_CONVERTER);
        assertEquals(zdt.toLocalTime(), lt);
    }

    // -------------------------------------------------------------------------
    // Edge‑case / exception tests
    // -------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToMap_nullInstant() {
        InstantConversions.toMap(null, DEFAULT_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_nullInstant() {
        InstantConversions.toZonedDateTime(null, DEFAULT_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_nullInstant() {
        InstantConversions.toOffsetDateTime(null, DEFAULT_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_nullInstant() {
        InstantConversions.toLong(null, DEFAULT_CONVERTER);
    }

    @Test
    public void testToLong_negativeInstant() {
        long expected = INSTANT_NEGATIVE.toEpochMilli();
        assertEquals(expected, InstantConversions.toLong(INSTANT_NEGATIVE, DEFAULT_CONVERTER));
    }

    @Test
    public void testToDouble_maxNano() {
        double d = InstantConversions.toDouble(INSTANT_MAX_NANO, DEFAULT_CONVERTER);
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(
                INSTANT_MAX_NANO.getEpochSecond(),
                INSTANT_MAX_NANO.getNano());
        assertEquals(expected.doubleValue(), d, 1e-12);
    }

    @Test
    public void testToBigInteger_negativeInstant() {
        BigInteger bi = InstantConversions.toBigInteger(INSTANT_NEGATIVE, DEFAULT_CONVERTER);
        BigInteger expected = BigInteger.valueOf(INSTANT_NEGATIVE.getEpochSecond())
                .multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(INSTANT_NEGATIVE.getNano()));
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigDecimal_zeroInstant() {
        BigDecimal bd = InstantConversions.toBigDecimal(INSTANT_EPOCH, DEFAULT_CONVERTER);
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(0L, 0);
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToCalendar_epochZero() {
        Calendar cal = InstantConversions.toCalendar(INSTANT_EPOCH, DEFAULT_CONVERTER);
        assertEquals(0L, cal.getTimeInMillis());
    }

    @Test
    public void testToLocalDateTime_epochZero() {
        LocalDateTime ldt = InstantConversions.toLocalDateTime(INSTANT_EPOCH, DEFAULT_CONVERTER);
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(INSTANT_EPOCH, DEFAULT_CONVERTER);
        assertEquals(zdt.toLocalDateTime(), ldt);
    }

    // -------------------------------------------------------------------------
    // Helper – verify that the default converter really supplies a zone / time‑zone.
    // -------------------------------------------------------------------------

    @Test
    public void sanityCheckDefaultConverterOptions() {
        assertNotNull("Converter options must not be null", DEFAULT_CONVERTER.getOptions());
        assertNotNull("ZoneId must be present", DEFAULT_CONVERTER.getOptions().getZoneId());
        assertNotNull("TimeZone must be present", DEFAULT_CONVERTER.getOptions().getTimeZone());
    }
}
