package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for {@link BigDecimalConversions}.
 *
 * The test class resides in the same package so it can access the
 * package‑private static methods of {@code BigDecimalConversions}.
 */
public class BigDecimalConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The real Converter class requires a ConverterOptions instance.
        // For the purpose of these unit tests we use the default constructor
        // and rely on the default time‑zone/zone‑id provided by the options.
        ConverterOptions options = new ConverterOptions(); // assumes a public no‑arg ctor
        // If the real library requires explicit time‑zone configuration you can
        // uncomment the following two lines (they compile against the public API):
        // options.setTimeZone(TimeZone.getTimeZone("UTC"));
        // options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic functional tests                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCalendar() {
        BigDecimal seconds = new BigDecimal("123.456");
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        assertEquals(TimeZone.getDefault(), cal.getTimeZone());
        assertEquals(123456L, cal.getTimeInMillis());
    }

    @Test
    public void testToInstant() {
        BigDecimal seconds = new BigDecimal("10.5");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(10L, instant.getEpochSecond());
        assertEquals(500_000_000L, instant.getNano());
    }

    @Test
    public void testToDuration() {
        BigDecimal seconds = new BigDecimal("2.25");
        Duration d = BigDecimalConversions.toDuration(seconds, converter);
        assertEquals(2L, d.getSeconds());
        assertEquals(250_000_000L, d.getNano());
    }

    @Test
    public void testToLocalTime() {
        BigDecimal seconds = new BigDecimal("3661.123456789");
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.of(1, 1, 1, 123_456_789), lt);
    }

    @Test
    public void testToLocalDate() {
        BigDecimal epoch = new BigDecimal("1577836800"); // 2020‑01‑01
        LocalDate date = BigDecimalConversions.toLocalDate(epoch, converter);
        assertEquals(LocalDate.of(2020, 1, 1), date);
    }

    @Test
    public void testToLocalDateTime() {
        BigDecimal epoch = new BigDecimal("1609459200.123"); // 2021‑01‑01T00:00:00.123Z
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(epoch, converter);
        assertEquals(LocalDateTime.of(2021, 1, 1, 0, 0, 0, 123_000_000), ldt);
    }

    @Test
    public void testToOffsetDateTime() {
        BigDecimal epoch = new BigDecimal("0");
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(epoch, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()), odt);
    }

    @Test
    public void testToZonedDateTime() {
        BigDecimal epoch = new BigDecimal("86400");
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(epoch, converter);
        assertEquals(Instant.ofEpochSecond(86400), zdt.toInstant());
        assertEquals(ZoneId.systemDefault(), zdt.getZone());
    }

    @Test
    public void testToDate() {
        BigDecimal epoch = new BigDecimal("1000");
        Date date = BigDecimalConversions.toDate(epoch, converter);
        assertEquals(Date.from(Instant.ofEpochSecond(1000)), date);
    }

    @Test
    public void testToSqlDate() {
        BigDecimal epoch = new BigDecimal("86400");
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(epoch, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(1970, 1, 2)), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        BigDecimal epoch = new BigDecimal("0");
        Timestamp ts = BigDecimalConversions.toTimestamp(epoch, converter);
        assertEquals(Timestamp.from(Instant.EPOCH), ts);
    }

    @Test
    public void testToBigInteger() {
        BigDecimal bd = new BigDecimal("12345678901234567890");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    @Test
    public void testToString() {
        BigDecimal bd = new BigDecimal("123.45000");
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("123.45", s);
    }

    @Test
    public void testToUUID() {
        // Use a deterministic 128‑bit value.
        BigInteger bigInt = new BigInteger("0123456789abcdef0123456789abcdef", 16);
        BigDecimal bd = new BigDecimal(bigInt);
        UUID uuid = BigDecimalConversions.toUUID(bd, converter);
        assertEquals(bigInt, BigIntegerConversions.toBigInteger(uuid, converter));
    }

    @Test
    public void testSecondsAndNanosToDouble() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(5L, 250_000_000L);
        assertEquals(new BigDecimal("5.25"), result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case & exception tests                                          */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Negative() {
        BigDecimal negative = new BigDecimal("-1");
        BigDecimalConversions.toLocalTime(negative, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLarge() {
        BigDecimal tooLarge = new BigDecimal("86400"); // exactly 24 h → out of range
        BigDecimalConversions.toLocalTime(tooLarge, converter);
    }

    @Test
    public void testToInstant_FractionalPrecisionTruncation() {
        BigDecimal bd = new BigDecimal("1.1234567899");
        Instant instant = BigDecimalConversions.toInstant(bd, converter);
        assertEquals(1L, instant.getEpochSecond());
        assertEquals(123_456_789L, instant.getNano()); // extra digit dropped
    }

    @Test
    public void testToDuration_FractionalPrecisionTruncation() {
        BigDecimal bd = new BigDecimal("0.9999999999");
        Duration d = BigDecimalConversions.toDuration(bd, converter);
        assertEquals(0L, d.getSeconds());
        assertEquals(999_999_999L, d.getNano());
    }

    @Test
    public void testToString_ZeroAndTrailingZeros() {
        BigDecimal zero = new BigDecimal("0.0000");
        assertEquals("0", BigDecimalConversions.toString(zero, converter));
    }

    @Test
    public void testSecondsAndNanosToDouble_NanosOutOfRange() {
        // 2 000 000 000 nanos = 2 seconds; method adds them as a scale‑9 fraction.
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(3L, 2_000_000_000L);
        assertEquals(new BigDecimal("5.0"), result);
    }
}
