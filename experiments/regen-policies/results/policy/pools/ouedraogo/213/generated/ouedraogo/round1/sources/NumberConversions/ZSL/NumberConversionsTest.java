package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
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

public class NumberConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Assuming ConverterOptions has a public no‑arg constructor and defaults to UTC.
        converter = new Converter(new ConverterOptions());
    }

    // ---------- primitive number conversions ----------
    @Test
    public void testToByte() {
        assertEquals((byte) 123, NumberConversions.toByte((byte) 123, converter));
        assertEquals((byte) -5, NumberConversions.toByte((short) -5, converter));
    }

    @Test
    public void testToByteZero() {
        assertEquals(Byte.valueOf((byte)0), NumberConversions.toByteZero(123, converter));
    }

    @Test
    public void testToShort() {
        assertEquals((short) 32000, NumberConversions.toShort((int) 32000, converter));
        assertEquals((short) -123, NumberConversions.toShort((byte) -123, converter));
    }

    @Test
    public void testToShortZero() {
        assertEquals(Short.valueOf((short)0), NumberConversions.toShortZero(1, converter));
    }

    @Test
    public void testToInt() {
        assertEquals(42, NumberConversions.toInt((short) 42, converter));
        assertEquals(-1, NumberConversions.toInt(-1L, converter));
    }

    @Test
    public void testToIntZero() {
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(99, converter));
    }

    @Test
    public void testToLong() {
        assertEquals(123456789L, NumberConversions.toLong(123456789L, converter));
        assertEquals(0L, NumberConversions.toLong((byte)0, converter));
    }

    @Test
    public void testToLongZero() {
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(5, converter));
    }

    @Test
    public void testToFloat() {
        assertEquals(1.5f, NumberConversions.toFloat(1.5f, converter), 0.0f);
        assertEquals(-2.0f, NumberConversions.toFloat(-2, converter), 0.0f);
    }

    @Test
    public void testToFloatZero() {
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(123, converter));
    }

    @Test
    public void testFloatToString() {
        assertEquals("0", NumberConversions.floatToString(0f, converter));
        assertEquals("3.14", NumberConversions.floatToString(3.14f, converter));
    }

    @Test
    public void testToDouble() {
        assertEquals(2.718, NumberConversions.toDouble(2.718, converter), 0.0);
        assertEquals(-1.0, NumberConversions.toDouble(-1, converter), 0.0);
    }

    @Test
    public void testToDoubleZero() {
        assertEquals(Double.valueOf(0.0), NumberConversions.toDoubleZero(0, converter));
    }

    @Test
    public void testDoubleToString() {
        assertEquals("0", NumberConversions.doubleToString(0d, converter));
        assertEquals("1.23", NumberConversions.doubleToString(1.23d, converter));
    }

    // ---------- BigDecimal / BigInteger ----------
    @Test
    public void testIntegerTypeToBigDecimal() {
        BigDecimal bd = NumberConversions.integerTypeToBigDecimal(42L, converter);
        assertEquals(BigDecimal.valueOf(42L), bd);
    }

    @Test
    public void testIntegerTypeToBigInteger() {
        BigInteger bi = NumberConversions.integerTypeToBigInteger(12345, converter);
        assertEquals(BigInteger.valueOf(12345L), bi);
    }

    @Test
    public void testFloatingPointToBigDecimal() {
        BigDecimal bd = NumberConversions.floatingPointToBigDecimal(3.14159, converter);
        assertEquals(BigDecimal.valueOf(3.14159), bd);
    }

    @Test
    public void testFloatingPointToBigInteger() {
        BigInteger bi = NumberConversions.floatingPointToBigInteger(9.9, converter);
        assertEquals(BigInteger.valueOf(9), bi);
        bi = NumberConversions.floatingPointToBigInteger(-2.3, converter);
        assertEquals(BigInteger.valueOf(-2), bi);
    }

    // ---------- Atomic types ----------
    @Test
    public void testToAtomicLong() {
        AtomicLong al = NumberConversions.toAtomicLong(123L, converter);
        assertEquals(123L, al.get());
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = NumberConversions.toAtomicInteger(7, converter);
        assertEquals(7, ai.get());
    }

    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean abTrue = NumberConversions.toAtomicBoolean(5L, converter);
        AtomicBoolean abFalse = NumberConversions.toAtomicBoolean(0L, converter);
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    // ---------- Zero‑checks ----------
    @Test
    public void testIsIntTypeNotZero() {
        assertTrue(NumberConversions.isIntTypeNotZero(10, converter));
        assertFalse(NumberConversions.isIntTypeNotZero(0, converter));
    }

    @Test
    public void testIsFloatTypeNotZero() {
        assertTrue(NumberConversions.isFloatTypeNotZero(0.1, converter));
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0, converter));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.ONE, converter));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        assertTrue(NumberConversions.isBigDecimalNotZero(BigDecimal.TEN, converter));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
    }

    // ---------- Character ----------
    @Test
    public void testToCharacterWithinRange() {
        char c = NumberConversions.toCharacter(65, converter);
        assertEquals('A', c);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRange() {
        NumberConversions.toCharacter(70000, converter);
    }

    // ---------- Date / Time ----------
    @Test
    public void testToDate() {
        long ms = 1622548800000L; // 2021‑06‑01T12:00:00Z
        Date d = NumberConversions.toDate(ms, converter);
        assertEquals(ms, d.getTime());
    }

    @Test
    public void testToDurationWithLong() {
        Duration d = NumberConversions.toDuration(1500L, converter);
        assertEquals(Duration.ofMillis(1500L), d);
    }

    @Test
    public void testToDurationWithBigDecimalSeconds() {
        Duration d = NumberConversions.toDuration(new BigDecimal("2.5"), converter);
        assertEquals(Duration.ofSeconds(2, 500_000_000L), d);
    }

    @Test
    public void testToDurationWithDouble() {
        Duration d = NumberConversions.toDuration(1.75d, converter);
        assertEquals(Duration.ofSeconds(1, 750_000_000L), d);
    }

    @Test
    public void testToInstant() {
        long epoch = 0L;
        Instant i = NumberConversions.toInstant(epoch, converter);
        assertEquals(Instant.EPOCH, i);
    }

    @Test
    public void testToSqlDate() {
        // 2022‑01‑01 UTC
        long epochMs = LocalDate.of(2022, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        java.sql.Date sqlDate = NumberConversions.toSqlDate(epochMs, converter);
        assertEquals(java.sql.Date.valueOf("2022-01-01"), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        long now = System.currentTimeMillis();
        Timestamp ts = NumberConversions.toTimestamp(now, converter);
        assertEquals(now, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        long now = System.currentTimeMillis();
        Calendar cal = NumberConversions.toCalendar(now, converter);
        assertEquals(now, cal.getTimeInMillis());
    }

    @Test
    public void testToLocalTimeValid() {
        // 01:02:03.004 => 1h*3600 + 2m*60 + 3s = 3723 seconds = 3723000 ms
        long millis = 3723000L;
        LocalTime lt = NumberConversions.toLocalTime(millis, converter);
        assertEquals(LocalTime.of(1, 2, 3, 4_000_000), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeInvalid() {
        NumberConversions.toLocalTime(-1L, converter);
    }

    @Test
    public void testToLocalDateAndDateTime() {
        long epoch = 1622548800000L; // 2021‑06‑01T12:00:00Z
        LocalDate ld = NumberConversions.toLocalDate(epoch, converter);
        LocalDateTime ldt = NumberConversions.toLocalDateTime(epoch, converter);
        assertEquals(LocalDate.of(2021, 6, 1), ld);
        assertEquals(LocalDateTime.of(2021, 6, 1, 12, 0), ldt);
    }

    @Test
    public void testToZonedDateTime() {
        long epoch = 0L;
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epoch, converter);
        assertEquals(Instant.EPOCH, zdt.toInstant());
        assertEquals(converter.getOptions().getZoneId(), zdt.getZone());
    }

    @Test
    public void testToOffsetTimeFromLong() {
        long epoch = 0L; // 1970‑01‑01T00:00:00Z
        OffsetTime ot = NumberConversions.toOffsetTime(epoch, converter);
        assertEquals(OffsetTime.ofInstant(Instant.EPOCH, converter.getOptions().getZoneId()), ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTimeUnsupported() {
        NumberConversions.toOffsetTime("not a number", converter);
    }

    @Test
    public void testToOffsetDateTime() {
        long epoch = 0L;
        OffsetDateTime odt = NumberConversions.toOffsetDateTime(epoch, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, converter.getOptions().getZoneId()), odt);
    }

    @Test
    public void testToYear() {
        Year y = NumberConversions.toYear((short)2025, converter);
        assertEquals(2025, y.getValue());
    }
}
