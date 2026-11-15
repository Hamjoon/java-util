package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link BigDecimalConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>normal conversions (seconds → {@link Instant}, {@link Duration}, {@link LocalTime}, …)</li>
 *   <li>boundary / edge cases (fractional seconds, midnight, end‑of‑day, negative values)</li>
 *   <li>error handling – especially the {@code IllegalArgumentException} thrown by {@code toLocalTime}</li>
 *   <li>numeric helpers – {@code secondsAndNanosToDouble}</li>
 * </ul>
 * <p>
 * The library’s {@link Converter} requires a {@link ConverterOptions} instance.
 * The test creates a fresh options object (using its public no‑arg constructor) and relies on the
 * default time‑zone/zone‑id (the JVM defaults). This is sufficient because all conversion methods
 * ultimately delegate to the {@code ConverterOptions} values via {@code converter.getOptions()}.
 */
public class BigDecimalConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor (as defined by the library).
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* -----------------------------------------------------------------------
     *  toCalendar()
     * ----------------------------------------------------------------------- */
    @Test
    public void testToCalendar() {
        // 1.234 seconds → 1234 milliseconds
        BigDecimal seconds = new BigDecimal("1.234");
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);

        Calendar expected = Calendar.getInstance(converter.getOptions().getTimeZone());
        expected.setTimeInMillis(1234L);
        assertEquals(expected.getTimeInMillis(), cal.getTimeInMillis());
    }

    /* -----------------------------------------------------------------------
     *  toInstant()
     * ----------------------------------------------------------------------- */
    @Test
    public void testToInstantWholeSeconds() {
        BigDecimal seconds = new BigDecimal("42");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(42L, instant.getEpochSecond());
        assertEquals(0L, instant.getNano());
    }

    @Test
    public void testToInstantWithNanos() {
        // 10.5 seconds → 10 seconds + 500_000_000 nanos
        BigDecimal seconds = new BigDecimal("10.5");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(10L, instant.getEpochSecond());
        assertEquals(500_000_000L, instant.getNano());
    }

    /* -----------------------------------------------------------------------
     *  toDuration()
     * ----------------------------------------------------------------------- */
    @Test
    public void testToDurationWholeSeconds() {
        BigDecimal seconds = new BigDecimal("7");
        Duration dur = BigDecimalConversions.toDuration(seconds, converter);
        assertEquals(Duration.ofSeconds(7), dur);
    }

    @Test
    public void testToDurationWithFraction() {
        // 3.000000001 seconds → 3 seconds + 1 nano
        BigDecimal seconds = new BigDecimal("3.000000001");
        Duration dur = BigDecimalConversions.toDuration(seconds, converter);
        assertEquals(3L, dur.getSeconds());
        assertEquals(1L, dur.getNano());
    }

    /* -----------------------------------------------------------------------
     *  toLocalTime()
     * ----------------------------------------------------------------------- */
    @Test
    public void testToLocalTimeMidnight() {
        BigDecimal seconds = BigDecimal.ZERO;
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToLocalTimeTypical() {
        // 13:45:30.123456789 → 13*3600 + 45*60 + 30.123456789 seconds
        long totalNanos = Duration.ofHours(13).toNanos()
                + Duration.ofMinutes(45).toNanos()
                + Duration.ofSeconds(30).toNanos()
                + 123_456_789L;
        BigDecimal seconds = new BigDecimal(totalNanos).divide(BigDecimalConversions.BILLION);
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.of(13, 45, 30, 123_456_789), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeOutOfRangeNegative() {
        BigDecimal seconds = new BigDecimal("-1");
        BigDecimalConversions.toLocalTime(seconds, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeOutOfRangeTooLarge() {
        // 24 hours = 86400 seconds – just one nanosecond above the allowed max
        BigDecimal seconds = new BigDecimal("86400");
        BigDecimalConversions.toLocalTime(seconds, converter);
    }

    /* -----------------------------------------------------------------------
     *  toLocalDate / toLocalDateTime / toOffsetDateTime / toZonedDateTime
     * ----------------------------------------------------------------------- */
    @Test
    public void testDateTimeConversionsAtEpoch() {
        BigDecimal seconds = BigDecimal.ZERO; // 1970‑01‑01T00:00:00Z
        LocalDate ld = BigDecimalConversions.toLocalDate(seconds, converter);
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(seconds, converter);
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(seconds, converter);
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);

        ZoneId zone = converter.getOptions().getZoneId();

        assertEquals(LocalDate.of(1970, 1, 1), ld);
        assertEquals(LocalDateTime.ofInstant(Instant.EPOCH, zone), ldt);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, zone), odt);
        assertEquals(ZonedDateTime.ofInstant(Instant.EPOCH, zone), zdt);
    }

    @Test
    public void testDateTimeConversionsWithFractionalSecond() {
        // 2022‑03‑14T15:09:26.535897932Z
        BigDecimal seconds = new BigDecimal("1647276566.535897932");
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);
        Instant instant = Instant.ofEpochSecond(1647276566L, 535_897_932L);
        assertEquals(instant, zdt.toInstant());

        // Local equivalents must respect the converter's zone
        ZoneId zone = converter.getOptions().getZoneId();
        assertEquals(LocalDateTime.ofInstant(instant, zone), BigDecimalConversions.toLocalDateTime(seconds, converter));
        assertEquals(LocalDate.ofInstant(instant, zone), BigDecimalConversions.toLocalDate(seconds, converter));
        assertEquals(OffsetDateTime.ofInstant(instant, zone), BigDecimalConversions.toOffsetDateTime(seconds, converter));
    }

    /* -----------------------------------------------------------------------
     *  toDate / toSqlDate / toTimestamp
     * ----------------------------------------------------------------------- */
    @Test
    public void testToDateAndSqlDateAndTimestamp() {
        BigDecimal seconds = new BigDecimal("1609459200"); // 2021‑01‑01T00:00:00Z
        Instant instant = Instant.ofEpochSecond(1609459200L);

        Date javaDate = BigDecimalConversions.toDate(seconds, converter);
        assertEquals(Date.from(instant), javaDate);

        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2021, 1, 1)), sqlDate);

        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        assertEquals(Timestamp.from(instant), ts);
    }

    /* -----------------------------------------------------------------------
     *  toBigInteger()
     * ----------------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        BigDecimal bd = new BigDecimal("12345678901234567890");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    /* -----------------------------------------------------------------------
     *  toString()
     * ----------------------------------------------------------------------- */
    @Test
    public void testToStringStripsTrailingZeros() {
        BigDecimal bd = new BigDecimal("123.45000");
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("123.45", s);
    }

    @Test
    public void testToStringScientificNotation() {
        BigDecimal bd = new BigDecimal("1E+6"); // 1,000,000
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("1000000", s);
    }

    /* -----------------------------------------------------------------------
     *  toUUID()
     * ----------------------------------------------------------------------- */
    @Test
    public void testToUUIDRoundTrip() {
        // Create a known UUID and convert it to a BigDecimal via its 128‑bit integer value.
        UUID original = UUID.randomUUID();
        // 128‑bit unsigned integer = mostSigBits << 64 + (leastSigBits & 0xFFFFFFFFFFFFFFFFL)
        BigInteger bigInt = new BigInteger(1,
                ByteBuffer.allocate(16)
                        .putLong(original.getMostSignificantBits())
                        .putLong(original.getLeastSignificantBits())
                        .array());
        BigDecimal bd = new BigDecimal(bigInt);
        UUID recovered = BigDecimalConversions.toUUID(bd, converter);
        assertEquals(original, recovered);
    }

    /* -----------------------------------------------------------------------
     *  secondsAndNanosToDouble()
     * ----------------------------------------------------------------------- */
    @Test
    public void testSecondsAndNanosToDoubleWhole() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(42L, 0L);
        assertEquals(new BigDecimal("42"), result);
    }

    @Test
    public void testSecondsAndNanosToDoubleWithFraction() {
        // 5 seconds + 250,000,000 nanos = 5.25
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(5L, 250_000_000L);
        assertEquals(new BigDecimal("5.25"), result);
    }

    @Test
    public void testSecondsAndNanosToDoubleMaximumNanos() {
        // 0 seconds + 999,999,999 nanos = 0.999999999
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(0L, 999_999_999L);
        assertEquals(new BigDecimal("0.999999999"), result);
    }
}
