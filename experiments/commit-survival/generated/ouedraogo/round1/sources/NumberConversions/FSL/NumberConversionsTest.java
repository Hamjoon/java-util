package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Comprehensive JUnit 4 test suite for {@link NumberConversions}.
 * <p>
 * The tests exercise every public static method in the class, covering
 * normal, boundary and error conditions.
 * </p>
 */
public class NumberConversionsTest {

    private static Converter converter;

    @BeforeClass
    public static void setUpConverter() {
        // ConverterOptions has a public no‑arg constructor in the library.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     *  Primitive numeric conversions (byte, short, int, long, float,
     *  double) and their “zero” constants.
     * ------------------------------------------------------------- */
    @Test
    public void testToByteAndZero() {
        assertEquals((byte) 123, NumberConversions.toByte((byte) 123, converter));
        assertEquals(Byte.valueOf((byte) 0), NumberConversions.toByteZero(123, converter));
    }

    @Test
    public void testToShortAndZero() {
        assertEquals((short) 3210, NumberConversions.toShort((short) 3210, converter));
        assertEquals(Short.valueOf((short) 0), NumberConversions.toShortZero(3210, converter));
    }

    @Test
    public void testToIntAndZero() {
        assertEquals(42, NumberConversions.toInt(42, converter));
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(42, converter));
    }

    @Test
    public void testToLongAndZero() {
        assertEquals(123456789L, NumberConversions.toLong(123456789L, converter));
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(123456789L, converter));
    }

    @Test
    public void testToFloatAndZero_andFloatToString() {
        assertEquals(3.14f, NumberConversions.toFloat(3.14f, converter), 0.0f);
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(3.14f, converter));

        // floatToString – zero should be "0"
        assertEquals("0", NumberConversions.floatToString(0f, converter));
        // non‑zero should delegate to toString()
        assertEquals("2.5", NumberConversions.floatToString(2.5f, converter));
    }

    @Test
    public void testToDoubleAndZero_andDoubleToString() {
        assertEquals(2.71828, NumberConversions.toDouble(2.71828, converter), 0.0);
        assertEquals(Double.valueOf(0d), NumberConversions.toDoubleZero(2.71828, converter));

        // doubleToString – zero should be "0"
        assertEquals("0", NumberConversions.doubleToString(0d, converter));
        // non‑zero should delegate to toString()
        assertEquals("1.618", NumberConversions.doubleToString(1.618, converter));
    }

    /* -------------------------------------------------------------
     *  Conversions to BigDecimal / BigInteger from integral values.
     * ------------------------------------------------------------- */
    @Test
    public void testIntegerTypeToBigDecimalAndBigInteger() {
        long value = 987654321L;
        assertEquals(BigDecimal.valueOf(value), NumberConversions.integerTypeToBigDecimal(value, converter));
        assertEquals(BigInteger.valueOf(value), NumberConversions.integerTypeToBigInteger(value, converter));
    }

    /* -------------------------------------------------------------
     *  Atomic wrappers.
     * ------------------------------------------------------------- */
    @Test
    public void testAtomicConversions() {
        AtomicLong al = NumberConversions.toAtomicLong(555L, converter);
        assertEquals(555L, al.get());

        AtomicInteger ai = NumberConversions.toAtomicInteger(42, converter);
        assertEquals(42, ai.get());

        AtomicBoolean abTrue = NumberConversions.toAtomicBoolean(1L, converter);
        AtomicBoolean abFalse = NumberConversions.toAtomicBoolean(0L, converter);
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    /* -------------------------------------------------------------
     *  Floating‑point to BigDecimal / BigInteger.
     * ------------------------------------------------------------- */
    @Test
    public void testFloatingPointToBigDecimalAndBigInteger() {
        double d = 12345.678;
        assertEquals(BigDecimal.valueOf(d), NumberConversions.floatingPointToBigDecimal(d, converter));

        // Positive fraction – should floor
        double pos = 99.9;
        assertEquals(new BigInteger("99"),
                NumberConversions.floatingPointToBigInteger(pos, converter));

        // Negative fraction – should ceil
        double neg = -42.3;
        assertEquals(new BigInteger("-42"),
                NumberConversions.floatingPointToBigInteger(neg, converter));

        // Exact integer value
        double exact = -7.0;
        assertEquals(new BigInteger("-7"),
                NumberConversions.floatingPointToBigInteger(exact, converter));
    }

    /* -------------------------------------------------------------
     *  Zero‑check helpers.
     * ------------------------------------------------------------- */
    @Test
    public void testIsIntTypeNotZero() {
        assertTrue(NumberConversions.isIntTypeNotZero(5L, converter));
        assertFalse(NumberConversions.isIntTypeNotZero(0L, converter));
    }

    @Test
    public void testIsFloatTypeNotZero() {
        assertTrue(NumberConversions.isFloatTypeNotZero(0.1, converter));
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0, converter));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.TEN, converter));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        assertTrue(NumberConversions.isBigDecimalNotZero(new BigDecimal("3.14"), converter));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
    }

    /* -------------------------------------------------------------
     *  Character conversion and out‑of‑range handling.
     * ------------------------------------------------------------- */
    @Test
    public void testToCharacterWithinRange() {
        assertEquals('A', NumberConversions.toCharacter(65L, converter));
        assertEquals('\u0000', NumberConversions.toCharacter(0L, converter));
        assertEquals(Character.MAX_VALUE, NumberConversions.toCharacter((long) Character.MAX_VALUE, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterNegativeThrows() {
        NumberConversions.toCharacter(-1L, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterAboveMaxThrows() {
        NumberConversions.toCharacter((long) Character.MAX_VALUE + 1, converter);
    }

    /* -------------------------------------------------------------
     *  Date / Time conversions.
     * ------------------------------------------------------------- */
    @Test
    public void testToDateAndSqlDate() {
        long epoch = 1_600_000_000_000L; // some fixed instant
        Date date = NumberConversions.toDate(epoch, converter);
        assertEquals(epoch, date.getTime());

        java.sql.Date sqlDate = NumberConversions.toSqlDate(epoch, converter);
        assertEquals(epoch, sqlDate.getTime());
    }

    @Test
    public void testToDurationAndInstant() {
        long millis = 12345L;
        Duration dur = NumberConversions.toDuration(millis, converter);
        assertEquals(millis, dur.toMillis());

        Instant instant = NumberConversions.toInstant(millis, converter);
        assertEquals(millis, instant.toEpochMilli());
    }

    @Test
    public void testToTimestamp() {
        long millis = 9_876_543_210L;
        Timestamp ts = NumberConversions.toTimestamp(millis, converter);
        assertEquals(millis, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        long millis = 4_000L;
        Calendar cal = NumberConversions.toCalendar(millis, converter);
        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void testZonedAndOffsetDateTimeConversions() {
        long epoch = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        ZoneId zone = converter.getOptions().getZoneId();

        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epoch, converter);
        assertEquals(Instant.ofEpochMilli(epoch), zdt.toInstant());
        assertEquals(zone, zdt.getZone());

        OffsetDateTime odt = NumberConversions.toOffsetDateTime(epoch, converter);
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testLocalDateAndLocalDateTime() {
        long epoch = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        ZoneId zone = converter.getOptions().getZoneId();

        LocalDate ld = NumberConversions.toLocalDate(epoch, converter);
        LocalDateTime ldt = NumberConversions.toLocalDateTime(epoch, converter);

        ZonedDateTime zdt = Instant.ofEpochMilli(epoch).atZone(zone);
        assertEquals(zdt.toLocalDate(), ld);
        assertEquals(zdt.toLocalDateTime(), ldt);
    }

    /* -------------------------------------------------------------
     *  Year conversion (short → Year).
     * ------------------------------------------------------------- */
    @Test
    public void testToYear() {
        short yearValue = 2023;
        Year year = NumberConversions.toYear(yearValue, converter);
        assertEquals(Year.of(2023), year);
    }
}
