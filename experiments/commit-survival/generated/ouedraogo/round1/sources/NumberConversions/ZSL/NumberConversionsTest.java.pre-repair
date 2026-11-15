package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
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

/**
 * Comprehensive JUnit‑4 tests for {@link NumberConversions}.
 *
 * The tests exercise every public static method in the class, covering
 * normal operation, edge cases and expected exceptions.
 *
 * NOTE: The library provides a {@link Converter} class that requires a
 * {@link ConverterOptions} instance.  The default constructor of
 * {@code ConverterOptions} is used – it supplies a sensible default
 * {@link ZoneId} (the system default) which is sufficient for the date‑time
 * conversions exercised here.
 */
public class NumberConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library supplies a default‑constructor for ConverterOptions.
        // This is the easiest way to obtain a usable Converter instance.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     * Primitive numeric conversions and their “zero” variants
     * ------------------------------------------------------------- */
    @Test
    public void testToByteAndZero() {
        Byte source = 123;
        assertEquals((byte) 123, NumberConversions.toByte(source, converter));
        assertEquals(Byte.valueOf((byte) 0), NumberConversions.toByteZero(source, converter));
    }

    @Test
    public void testToShortAndZero() {
        Short source = 32123;
        assertEquals((short) 32123, NumberConversions.toShort(source, converter));
        assertEquals(Short.valueOf((short) 0), NumberConversions.toShortZero(source, converter));
    }

    @Test
    public void testToIntAndZero() {
        Integer source = 12345678;
        assertEquals(12345678, NumberConversions.toInt(source, converter));
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(source, converter));
    }

    @Test
    public void testToLongAndZero() {
        Long source = 9_223_372_036_854_775_807L; // Long.MAX_VALUE
        assertEquals(9_223_372_036_854_775_807L, NumberConversions.toLong(source, converter));
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(source, converter));
    }

    @Test
    public void testToFloatAndZero_andFloatToString() {
        Float source = 12.34f;
        assertEquals(12.34f, NumberConversions.toFloat(source, converter), 0.00001f);
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(source, converter));

        // floatToString – zero case
        assertEquals("0", NumberConversions.floatToString(0f, converter));
        // non‑zero case – uses toString of the Float instance
        assertEquals(source.toString(), NumberConversions.floatToString(source, converter));
    }

    @Test
    public void testToDoubleAndZero_andDoubleToString() {
        Double source = 12345.6789;
        assertEquals(12345.6789, NumberConversions.toDouble(source, converter), 0.0000001);
        assertEquals(Double.valueOf(0d), NumberConversions.toDoubleZero(source, converter));

        // doubleToString – zero case
        assertEquals("0", NumberConversions.doubleToString(0d, converter));
        // non‑zero case – uses toString of the Double instance
        assertEquals(source.toString(), NumberConversions.doubleToString(source, converter));
    }

    /* -------------------------------------------------------------
     * Conversions to BigDecimal / BigInteger
     * ------------------------------------------------------------- */
    @Test
    public void testIntegerTypeToBigDecimalAndBigInteger() {
        Long source = 123456789L;
        BigDecimal bd = NumberConversions.integerTypeToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(123456789L), bd);

        BigInteger bi = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(123456789L), bi);
    }

    @Test
    public void testFloatingPointToBigDecimalAndBigInteger() {
        Double source = 9876.54321;
        BigDecimal bd = NumberConversions.floatingPointToBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(9876.54321), bd);

        // Positive number – should be floored
        Double pos = 123.9;
        BigInteger posInt = NumberConversions.floatingPointToBigInteger(pos, converter);
        assertEquals(BigInteger.valueOf(123), posInt);

        // Negative number – should be ceiled
        Double neg = -123.1;
        BigInteger negInt = NumberConversions.floatingPointToBigInteger(neg, converter);
        assertEquals(BigInteger.valueOf(-123), negInt);
    }

    /* -------------------------------------------------------------
     * Atomic wrapper conversions
     * ------------------------------------------------------------- */
    @Test
    public void testAtomicConversions() {
        Integer intSource = 42;
        Long longSource = 123456789L;

        AtomicInteger ai = NumberConversions.toAtomicInteger(intSource, converter);
        assertEquals(42, ai.get());

        AtomicLong al = NumberConversions.toAtomicLong(longSource, converter);
        assertEquals(123456789L, al.get());

        // AtomicBoolean – true when the long value is non‑zero
        AtomicBoolean abTrue = NumberConversions.toAtomicBoolean(1L, converter);
        assertTrue(abTrue.get());

        AtomicBoolean abFalse = NumberConversions.toAtomicBoolean(0L, converter);
        assertFalse(abFalse.get());
    }

    /* -------------------------------------------------------------
     * Zero‑check helpers
     * ------------------------------------------------------------- */
    @Test
    public void testIsIntTypeNotZero() {
        assertFalse(NumberConversions.isIntTypeNotZero(0L, converter));
        assertTrue(NumberConversions.isIntTypeNotZero(5L, converter));
    }

    @Test
    public void testIsFloatTypeNotZero() {
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0, converter));
        assertTrue(NumberConversions.isFloatTypeNotZero(-0.0001, converter));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.valueOf(-7), converter));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
        assertTrue(NumberConversions.isBigDecimalNotZero(new BigDecimal("0.00001"), converter));
    }

    /* -------------------------------------------------------------
     * Character conversion
     * ------------------------------------------------------------- */
    @Test
    public void testToCharacterWithinRange() {
        // value 65 -> 'A'
        char result = NumberConversions.toCharacter(65L, converter);
        assertEquals('A', result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeNegative() {
        NumberConversions.toCharacter(-1L, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeTooLarge() {
        NumberConversions.toCharacter((long) Character.MAX_VALUE + 1, converter);
    }

    /* -------------------------------------------------------------
     * Date / Time conversions
     * ------------------------------------------------------------- */
    @Test
    public void testDateRelatedConversions() {
        long epochMillis = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z

        Date date = NumberConversions.toDate(epochMillis, converter);
        assertEquals(epochMillis, date.getTime());

        Duration duration = NumberConversions.toDuration(epochMillis, converter);
        assertEquals(epochMillis, duration.toMillis());

        Instant instant = NumberConversions.toInstant(epochMillis, converter);
        assertEquals(epochMillis, instant.toEpochMilli());

        java.sql.Date sqlDate = NumberConversions.toSqlDate(epochMillis, converter);
        assertEquals(epochMillis, sqlDate.getTime());

        Timestamp timestamp = NumberConversions.toTimestamp(epochMillis, converter);
        assertEquals(epochMillis, timestamp.getTime());
    }

    @Test
    public void testToCalendar() {
        long epochMillis = 1_600_000_000_000L;
        Calendar cal = NumberConversions.toCalendar(epochMillis, converter);
        assertEquals(epochMillis, cal.getTimeInMillis());
    }

    @Test
    public void testZonedAndOffsetDateTimeConversions() {
        long epochMillis = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        ZoneId zone = converter.getOptions().getZoneId();

        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epochMillis, converter);
        assertEquals(Instant.ofEpochMilli(epochMillis), zdt.toInstant());
        assertEquals(zone, zdt.getZone());

        OffsetDateTime odt = NumberConversions.toOffsetDateTime(epochMillis, converter);
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testLocalDateAndLocalDateTime() {
        long epochMillis = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epochMillis, converter);

        LocalDate ld = NumberConversions.toLocalDate(epochMillis, converter);
        assertEquals(zdt.toLocalDate(), ld);

        LocalDateTime ldt = NumberConversions.toLocalDateTime(epochMillis, converter);
        assertEquals(zdt.toLocalDateTime(), ldt);
    }

    /* -------------------------------------------------------------
     * Year conversion
     * ------------------------------------------------------------- */
    @Test
    public void testToYear() {
        Short source = 2023;
        Year year = NumberConversions.toYear(source, converter);
        assertEquals(2023, year.getValue());
    }
}
