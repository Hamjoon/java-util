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

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link NumberConversions}.
 * The tests cover every public static method in the class, exercising
 * normal, edge‑case and exceptional behaviour.
 */
public class NumberConversionsTest {

    // Helper to obtain a default Converter instance (options provide a ZoneId)
    private static Converter newConverter() {
        return new Converter(new ConverterOptions());
    }

    /*--------------------------------------------------------------
     * Primitive number conversions and zero‑value shortcuts
     *-------------------------------------------------------------*/
    @Test
    public void testPrimitiveConversions() {
        Converter c = newConverter();

        Byte b = (byte) 12;
        assertEquals((byte) 12, NumberConversions.toByte(b, c));
        assertEquals(Byte.valueOf((byte) 0), NumberConversions.toByteZero(b, c));

        Short s = (short) 345;
        assertEquals((short) 345, NumberConversions.toShort(s, c));
        assertEquals(Short.valueOf((short) 0), NumberConversions.toShortZero(s, c));

        Integer i = 123456;
        assertEquals(123456, NumberConversions.toInt(i, c));
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(i, c));

        Long l = 9876543210L;
        assertEquals(9876543210L, NumberConversions.toLong(l, c));
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(l, c));

        Float f = 3.14f;
        assertEquals(3.14f, NumberConversions.toFloat(f, c), 0.0f);
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(f, c));

        Double d = 2.71828;
        assertEquals(2.71828, NumberConversions.toDouble(d, c), 0.0);
        assertEquals(Double.valueOf(0d), NumberConversions.toDoubleZero(d, c));
    }

    /*--------------------------------------------------------------
     * String conversion helpers for float / double
     *-------------------------------------------------------------*/
    @Test
    public void testFloatToString() {
        Converter c = newConverter();

        assertEquals("0", NumberConversions.floatToString(0.0f, c));
        assertEquals("1.23", NumberConversions.floatToString(1.23f, c));
    }

    @Test
    public void testDoubleToString() {
        Converter c = newConverter();

        assertEquals("0", NumberConversions.doubleToString(0.0d, c));
        assertEquals("4.56", NumberConversions.doubleToString(4.56d, c));
    }

    /*--------------------------------------------------------------
     * Integer → BigDecimal / BigInteger
     *-------------------------------------------------------------*/
    @Test
    public void testIntegerTypeToBigDecimalAndBigInteger() {
        Converter c = newConverter();

        Long l = 12345L;
        assertEquals(BigDecimal.valueOf(12345L), NumberConversions.integerTypeToBigDecimal(l, c));
        assertEquals(BigInteger.valueOf(12345L), NumberConversions.integerTypeToBigInteger(l, c));
    }

    /*--------------------------------------------------------------
     * Atomic conversions
     *-------------------------------------------------------------*/
    @Test
    public void testAtomicConversions() {
        Converter c = newConverter();

        AtomicLong al = NumberConversions.toAtomicLong(42L, c);
        assertEquals(42L, al.get());

        AtomicInteger ai = NumberConversions.toAtomicInteger(7, c);
        assertEquals(7, ai.get());

        AtomicBoolean abTrue = NumberConversions.toAtomicBoolean(5L, c);
        AtomicBoolean abFalse = NumberConversions.toAtomicBoolean(0L, c);
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    /*--------------------------------------------------------------
     * Floating‑point → BigDecimal / BigInteger
     *-------------------------------------------------------------*/
    @Test
    public void testFloatingPointToBigDecimalAndBigInteger() {
        Converter c = newConverter();

        Double d = 123.456;
        assertEquals(BigDecimal.valueOf(123.456), NumberConversions.floatingPointToBigDecimal(d, c));

        // Positive value – floor is used
        Double pos = 123.9;
        assertEquals(BigInteger.valueOf(123), NumberConversions.floatingPointToBigInteger(pos, c));

        // Negative value – ceil is used
        Double neg = -123.9;
        assertEquals(BigInteger.valueOf(-123), NumberConversions.floatingPointToBigInteger(neg, c));
    }

    /*--------------------------------------------------------------
     * Zero‑check helpers
     *-------------------------------------------------------------*/
    @Test
    public void testIsNotZeroChecks() {
        Converter c = newConverter();

        assertTrue(NumberConversions.isIntTypeNotZero(5, c));
        assertFalse(NumberConversions.isIntTypeNotZero(0L, c));

        assertTrue(NumberConversions.isFloatTypeNotZero(0.1d, c));
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0f, c));

        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.valueOf(7), c));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, c));

        assertTrue(NumberConversions.isBigDecimalNotZero(new BigDecimal("3.14"), c));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, c));
    }

    /*--------------------------------------------------------------
     * Character conversion
     *-------------------------------------------------------------*/
    @Test
    public void testToCharacterWithinRange() {
        Converter c = newConverter();

        // 65 -> 'A'
        assertEquals('A', NumberConversions.toCharacter(65, c));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRange() {
        Converter c = newConverter();

        // 70000 exceeds Character.MAX_VALUE (65535)
        NumberConversions.toCharacter(70000, c);
    }

    /*--------------------------------------------------------------
     * Date / Instant / Timestamp / SqlDate
     *-------------------------------------------------------------*/
    @Test
    public void testDateInstantTimestampConversions() {
        Converter c = newConverter();

        long epochMillis = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        Date date = NumberConversions.toDate(epochMillis, c);
        assertEquals(epochMillis, date.getTime());

        Instant instant = NumberConversions.toInstant(epochMillis, c);
        assertEquals(epochMillis, instant.toEpochMilli());

        Timestamp ts = NumberConversions.toTimestamp(epochMillis, c);
        assertEquals(epochMillis, ts.getTime());

        // SqlDate should represent the same calendar day in the converter's zone.
        java.sql.Date sqlDate = NumberConversions.toSqlDate(epochMillis, c);
        // Default zone is UTC for the test environment; adjust if needed.
        assertEquals(java.sql.Date.valueOf(LocalDate.ofInstant(Instant.ofEpochMilli(epochMillis),
                c.getOptions().getZoneId())), sqlDate);
    }

    /*--------------------------------------------------------------
     * Duration conversion for the various numeric families
     *-------------------------------------------------------------*/
    @Test
    public void testToDurationFromLong() {
        Converter c = newConverter();

        Duration d = NumberConversions.toDuration(5_000L, c);
        assertEquals(Duration.ofMillis(5_000L), d);
    }

    @Test
    public void testToDurationFromBigDecimalSeconds() {
        Converter c = newConverter();

        BigDecimal bd = new BigDecimal("2.5"); // 2 seconds + 500 ms
        Duration d = NumberConversions.toDuration(bd, c);
        assertEquals(Duration.ofSeconds(2, 500_000_000L), d);
    }

    @Test
    public void testToDurationFromDoubleSeconds() {
        Converter c = newConverter();

        Double dVal = 3.75; // 3 seconds + 750 ms
        Duration d = NumberConversions.toDuration(dVal, c);
        assertEquals(Duration.ofSeconds(3, 750_000_000L), d);
    }

    @Test
    public void testToDurationFallbackFromStringNumber() {
        Converter c = newConverter();

        // A string that parses to a number; the fallback treats it as seconds.
        String numberStr = "1.2";
        Duration d = NumberConversions.toDuration(numberStr, c);
        assertEquals(Duration.ofSeconds(1, 200_000_000L), d);
    }

    /*--------------------------------------------------------------
     * Calendar conversion (relies on CalendarConversions.create)
     *-------------------------------------------------------------*/
    @Test
    public void testToCalendar() {
        Converter c = newConverter();

        Calendar cal = NumberConversions.toCalendar(0L, c);
        assertNotNull(cal);
        assertEquals(0L, cal.getTimeInMillis());
    }

    /*--------------------------------------------------------------
     * LocalTime conversion – valid and invalid ranges
     *-------------------------------------------------------------*/
    @Test
    public void testToLocalTimeValid() {
        Converter c = newConverter();

        // 1 second = 1_000_000_000 nanoseconds
        long millis = 1_234L;
        LocalTime lt = NumberConversions.toLocalTime(millis, c);
        assertEquals(LocalTime.ofNanoOfDay(millis * 1_000_000L), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeNegative() {
        Converter c = newConverter();

        NumberConversions.toLocalTime(-1L, c);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeTooLarge() {
        Converter c = newConverter();

        // 86_400_000 ms = 24 h, which is out of LocalTime range.
        NumberConversions.toLocalTime(86_400_000L, c);
    }

    /*--------------------------------------------------------------
     * Zoned / Offset / Local date‑time families
     *-------------------------------------------------------------*/
    @Test
    public void testZonedAndLocalDateTimeConversions() {
        Converter c = newConverter();

        long epoch = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epoch, c);
        assertEquals(Instant.ofEpochMilli(epoch).atZone(c.getOptions().getZoneId()), zdt);

        LocalDate ld = NumberConversions.toLocalDate(epoch, c);
        assertEquals(zdt.toLocalDate(), ld);

        LocalDateTime ldt = NumberConversions.toLocalDateTime(epoch, c);
        assertEquals(zdt.toLocalDateTime(), ldt);
    }

    @Test
    public void testOffsetTimeFromLongAndBigDecimal() {
        Converter c = newConverter();

        // From Long (epoch milliseconds)
        OffsetTime otLong = NumberConversions.toOffsetTime(0L, c);
        assertEquals(OffsetTime.ofInstant(Instant.EPOCH, c.getOptions().getZoneId()), otLong);

        // From BigDecimal seconds (1.5 seconds = 1500 ms)
        BigDecimal bd = new BigDecimal("1.5");
        OffsetTime otBd = NumberConversions.toOffsetTime(bd, c);
        Instant expected = Instant.ofEpochMilli(1500);
        assertEquals(OffsetTime.ofInstant(expected, c.getOptions().getZoneId()), otBd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOffsetTimeUnsupportedType() {
        Converter c = newConverter();

        NumberConversions.toOffsetTime("unsupported", c);
    }

    @Test
    public void testOffsetDateTimeConversion() {
        Converter c = newConverter();

        long epoch = 1_600_000_000_000L;
        OffsetDateTime odt = NumberConversions.toOffsetDateTime(epoch, c);
        assertEquals(NumberConversions.toZonedDateTime(epoch, c).toOffsetDateTime(), odt);
    }

    /*--------------------------------------------------------------
     * Year conversion
     *-------------------------------------------------------------*/
    @Test
    public void testYearConversion() {
        Converter c = newConverter();

        Year y = NumberConversions.toYear((short) 2023, c);
        assertEquals(Year.of(2023), y);
    }
}
