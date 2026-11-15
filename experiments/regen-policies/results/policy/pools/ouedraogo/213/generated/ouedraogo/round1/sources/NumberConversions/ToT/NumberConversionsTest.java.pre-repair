package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link NumberConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Primitive and wrapper conversions (byte, short, int, long, float, double)</li>
 *   <li>Zero‑value shortcuts (to*Zero)</li>
 *   <li>String representations for floating point numbers (floatToString, doubleToString)</li>
 *   <li>BigDecimal / BigInteger creation from integer and floating‑point sources</li>
 *   <li>Atomic types and AtomicBoolean logic</li>
 *   <li>Zero‑checking helpers for numeric, BigInteger and BigDecimal</li>
 *   <li>Character conversion with range validation</li>
 *   <li>Date / Instant / Timestamp / Calendar conversions</li>
 *   <li>Duration conversion for all supported numeric families</li>
 *   <li>LocalTime, LocalDate, LocalDateTime, ZonedDateTime, OffsetTime, OffsetDateTime</li>
 *   <li>Year conversion</li>
 *   <li>Exception scenarios (out‑of‑range char, invalid LocalTime)</li>
 * </ul>
 *
 * No external mocking frameworks are used – all objects are created via the public
 * constructors or factory methods that appear in the source code or its listed
 * dependencies.
 */
public class NumberConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is required only for zone information; we use UTC for deterministic results.
        ConverterOptions options = new ConverterOptions(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /* -----------------------------------------------------------
     *  Primitive / Wrapper conversions and zero shortcuts
     * ----------------------------------------------------------- */
    @Test
    public void testToByte_andToByteZero() {
        Byte source = 123;
        assertEquals((byte) 123, NumberConversions.toByte(source, converter));
        assertEquals(Byte.valueOf((byte) 0), NumberConversions.toByteZero(source, converter));
    }

    @Test
    public void testToShort_andToShortZero() {
        Short source = 32123;
        assertEquals((short) 32123, NumberConversions.toShort(source, converter));
        assertEquals(Short.valueOf((short) 0), NumberConversions.toShortZero(source, converter));
    }

    @Test
    public void testToInt_andToIntZero() {
        Integer source = 123456;
        assertEquals(123456, NumberConversions.toInt(source, converter));
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(source, converter));
    }

    @Test
    public void testToLong_andToLongZero() {
        Long source = 9876543210L;
        assertEquals(9876543210L, NumberConversions.toLong(source, converter));
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(source, converter));
    }

    @Test
    public void testToFloat_andToFloatZero() {
        Float source = 12.34f;
        assertEquals(12.34f, NumberConversions.toFloat(source, converter), 0.00001f);
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(source, converter));
    }

    @Test
    public void testToDouble_andToDoubleZero() {
        Double source = 12345.6789;
        assertEquals(12345.6789, NumberConversions.toDouble(source, converter), 0.0000001);
        assertEquals(Double.valueOf(0d), NumberConversions.toDoubleZero(source, converter));
    }

    /* -----------------------------------------------------------
     *  Float / Double string helpers
     * ----------------------------------------------------------- */
    @Test
    public void testFloatToString() {
        assertEquals("0", NumberConversions.floatToString(0f, converter));
        assertEquals("3.14", NumberConversions.floatToString(3.14f, converter));
        // Ensure that toString() of Float is returned for non‑zero values
        Float f = 1.0f / 3.0f;
        assertEquals(f.toString(), NumberConversions.floatToString(f, converter));
    }

    @Test
    public void testDoubleToString() {
        assertEquals("0", NumberConversions.doubleToString(0d, converter));
        assertEquals("2.71828", NumberConversions.doubleToString(2.71828, converter));
        Double d = Math.PI;
        assertEquals(d.toString(), NumberConversions.doubleToString(d, converter));
    }

    /* -----------------------------------------------------------
     *  BigDecimal / BigInteger from integer types
     * ----------------------------------------------------------- */
    @Test
    public void testIntegerTypeToBigDecimalAndBigInteger() {
        Long src = 123456789L;
        assertEquals(BigDecimal.valueOf(123456789L), NumberConversions.integerTypeToBigDecimal(src, converter));
        assertEquals(BigInteger.valueOf(123456789L), NumberConversions.integerTypeToBigInteger(src, converter));
    }

    /* -----------------------------------------------------------
     *  Atomic types
     * ----------------------------------------------------------- */
    @Test
    public void testAtomicConversions() {
        Integer intSrc = 42;
        Long longSrc = 987654321L;

        AtomicInteger ai = NumberConversions.toAtomicInteger(intSrc, converter);
        assertEquals(42, ai.get());

        AtomicLong al = NumberConversions.toAtomicLong(longSrc, converter);
        assertEquals(987654321L, al.get());

        AtomicBoolean abTrue = NumberConversions.toAtomicBoolean(longSrc, converter);
        assertTrue(abTrue.get());

        AtomicBoolean abFalse = NumberConversions.toAtomicBoolean(0L, converter);
        assertFalse(abFalse.get());
    }

    /* -----------------------------------------------------------
     *  Floating‑point to BigDecimal / BigInteger
     * ----------------------------------------------------------- */
    @Test
    public void testFloatingPointToBigDecimalAndBigInteger() {
        Double src = 12345.6789;
        assertEquals(BigDecimal.valueOf(12345.6789), NumberConversions.floatingPointToBigDecimal(src, converter));

        // BigInteger conversion truncates toward zero (floor for positive, ceil for negative)
        assertEquals(BigInteger.valueOf(12345), NumberConversions.floatingPointToBigInteger(src, converter));

        Double negative = -12.9;
        assertEquals(BigInteger.valueOf(-12), NumberConversions.floatingPointToBigInteger(negative, converter));
    }

    /* -----------------------------------------------------------
     *  Zero‑checking helpers
     * ----------------------------------------------------------- */
    @Test
    public void testIsIntTypeNotZero() {
        assertTrue(NumberConversions.isIntTypeNotZero(5, converter));
        assertFalse(NumberConversions.isIntTypeNotZero(0L, converter));
    }

    @Test
    public void testIsFloatTypeNotZero() {
        assertTrue(NumberConversions.isFloatTypeNotZero(0.1, converter));
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0d, converter));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.ONE, converter));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        assertTrue(NumberConversions.isBigDecimalNotZero(new BigDecimal("0.0001"), converter));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
    }

    /* -----------------------------------------------------------
     *  Character conversion
     * ----------------------------------------------------------- */
    @Test
    public void testToCharacterWithinRange() {
        assertEquals('A', NumberConversions.toCharacter(65L, converter));
        assertEquals('\u0000', NumberConversions.toCharacter(0L, converter));
        assertEquals(Character.MAX_VALUE, NumberConversions.toCharacter((long) Character.MAX_VALUE, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeNegative() {
        NumberConversions.toCharacter(-1L, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeTooLarge() {
        NumberConversions.toCharacter((long) Character.MAX_VALUE + 1, converter);
    }

    /* -----------------------------------------------------------
     *  Date / Instant / Timestamp / Calendar
     * ----------------------------------------------------------- */
    @Test
    public void testDateInstantTimestampCalendarConversions() {
        long epochMillis = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z

        Date date = NumberConversions.toDate(epochMillis, converter);
        assertEquals(epochMillis, date.getTime());

        Instant instant = NumberConversions.toInstant(epochMillis, converter);
        assertEquals(epochMillis, instant.toEpochMilli());

        Timestamp ts = NumberConversions.toTimestamp(epochMillis, converter);
        assertEquals(epochMillis, ts.getTime());

        Calendar cal = NumberConversions.toCalendar(epochMillis, converter);
        assertEquals(epochMillis, cal.getTimeInMillis());
    }

    /* -----------------------------------------------------------
     *  Duration conversion – exhaustive coverage of branches
     * ----------------------------------------------------------- */
    @Test
    public void testDurationFromWholeNumberTypes() {
        // Long
        Duration d1 = NumberConversions.toDuration(5_000L, converter);
        assertEquals(Duration.ofMillis(5_000L), d1);

        // Integer
        Duration d2 = NumberConversions.toDuration(123, converter);
        assertEquals(Duration.ofMillis(123L), d2);

        // AtomicLong
        Duration d3 = NumberConversions.toDuration(new AtomicLong(9_999L), converter);
        assertEquals(Duration.ofMillis(9_999L), d3);

        // AtomicInteger
        Duration d4 = NumberConversions.toDuration(new AtomicInteger(42), converter);
        assertEquals(Duration.ofMillis(42L), d4);

        // BigInteger
        Duration d5 = NumberConversions.toDuration(BigInteger.valueOf(7_777L), converter);
        assertEquals(Duration.ofMillis(7_777L), d5);
    }

    @Test
    public void testDurationFromBigDecimal() {
        // 2.5 seconds -> 2 seconds + 500_000_000 nanos
        BigDecimal bd = new BigDecimal("2.5");
        Duration d = NumberConversions.toDuration(bd, converter);
        assertEquals(Duration.ofSeconds(2, 500_000_000L), d);
    }

    @Test
    public void testDurationFromFloatingPoint() {
        Double dVal = 3.75; // 3 seconds + 750_000_000 nanos
        Duration dur = NumberConversions.toDuration(dVal, converter);
        assertEquals(Duration.ofSeconds(3, 750_000_000L), dur);
    }

    @Test
    public void testDurationFromStringRepresentableNumber() {
        // Fallback branch – use toString()
        Number custom = new Number() {
            @Override public int intValue() { return 0; }
            @Override public long longValue() { return 0L; }
            @Override public float floatValue() { return 0f; }
            @Override public double doubleValue() { return 0d; }
            @Override public String toString() { return "4.125"; }
        };
        Duration dur = NumberConversions.toDuration(custom, converter);
        assertEquals(Duration.ofSeconds(4, 125_000_000L), dur);
    }

    /* -----------------------------------------------------------
     *  SQL Date conversion (uses converter's zone)
     * ----------------------------------------------------------- */
    @Test
    public void testSqlDateConversion() {
        // 2021‑01‑01T00:00:00Z -> same date in UTC
        long epoch = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli();
        java.sql.Date sqlDate = NumberConversions.toSqlDate(epoch, converter);
        assertEquals(java.sql.Date.valueOf("2021-01-01"), sqlDate);
    }

    /* -----------------------------------------------------------
     *  LocalTime conversion – valid and invalid ranges
     * ----------------------------------------------------------- */
    @Test
    public void testToLocalTimeValid() {
        // 12:34:56.789 -> 12*3600 + 34*60 + 56 = 45296 seconds + 789 ms
        long millis = ((12L * 3600) + (34L * 60) + 56L) * 1000 + 789;
        LocalTime lt = NumberConversions.toLocalTime(millis, converter);
        assertEquals(LocalTime.of(12, 34, 56, 789_000_000), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeNegative() {
        NumberConversions.toLocalTime(-1L, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeTooLarge() {
        // 24 hours in millis would be out of range (max allowed 86_399_999 ms)
        NumberConversions.toLocalTime(86_400_000L, converter);
    }

    /* -----------------------------------------------------------
     *  LocalDate, LocalDateTime, ZonedDateTime, OffsetTime,
     *  OffsetDateTime, Year
     * ----------------------------------------------------------- */
    @Test
    public void testDateTimeConversions() {
        long epoch = Instant.parse("2022-03-14T15:09:26Z").toEpochMilli();

        // ZonedDateTime
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epoch, converter);
        assertEquals(Instant.ofEpochMilli(epoch), zdt.toInstant());
        assertEquals(converter.getOptions().getZoneId(), zdt.getZone());

        // LocalDate / LocalDateTime via ZonedDateTime
        LocalDate ld = NumberConversions.toLocalDate(epoch, converter);
        assertEquals(LocalDate.of(2022, 3, 14), ld);

        LocalDateTime ldt = NumberConversions.toLocalDateTime(epoch, converter);
        assertEquals(LocalDateTime.of(2022, 3, 14, 15, 9, 26), ldt);

        // OffsetDateTime
        OffsetDateTime odt = NumberConversions.toOffsetDateTime(epoch, converter);
        assertEquals(zdt.toOffsetDateTime(), odt);

        // OffsetTime from numeric epoch millis
        OffsetTime ot = NumberConversions.toOffsetTime(epoch, converter);
        assertEquals(OffsetTime.ofInstant(Instant.ofEpochMilli(epoch), converter.getOptions().getZoneId()), ot);
    }

    @Test
    public void testOffsetTimeFromBigDecimalAndBigInteger() {
        // Use the delegated conversions (the real logic resides in the companion classes)
        BigDecimal bd = new BigDecimal("1633072800000"); // 2021‑10‑01T00:00:00Z
        OffsetTime otFromBD = NumberConversions.toOffsetTime(bd, converter);
        assertEquals(OffsetTime.ofInstant(Instant.ofEpochMilli(bd.longValue()), converter.getOptions().getZoneId()), otFromBD);

        BigInteger bi = BigInteger.valueOf(1633072800000L);
        OffsetTime otFromBI = NumberConversions.toOffsetTime(bi, converter);
        assertEquals(OffsetTime.ofInstant(Instant.ofEpochMilli(bi.longValue()), converter.getOptions().getZoneId()), otFromBI);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOffsetTimeUnsupportedType() {
        NumberConversions.toOffsetTime("not a number", converter);
    }

    @Test
    public void testYearConversion() {
        Short src = 99; // short value
        Year year = NumberConversions.toYear(src, converter);
        assertEquals(Year.of(99), year);
    }
}
