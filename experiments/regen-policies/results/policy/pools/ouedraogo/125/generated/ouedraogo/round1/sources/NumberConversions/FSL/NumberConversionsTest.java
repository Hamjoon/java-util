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

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link NumberConversions}.
 *
 * The tests use only the public constructors and methods that are part of the
 * supplied source code and the listed {@code Converter} API. No mocking frameworks
 * or external libraries are used.
 */
public class NumberConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The real Converter class requires a ConverterOptions instance.
        // The library provides a public no‑arg constructor for both classes.
        // Using the default options gives a system default ZoneId which is
        // sufficient for all date‑time related tests.
        converter = new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /* Simple numeric conversions                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByte_and_ToByteZero() {
        Byte expected = 123;
        assertEquals(expected.byteValue(), NumberConversions.toByte(expected, converter));
        assertEquals(CommonValues.BYTE_ZERO, NumberConversions.toByteZero(expected, converter));
    }

    @Test
    public void testToShort_and_ToShortZero() {
        Short expected = 32123;
        assertEquals(expected.shortValue(), NumberConversions.toShort(expected, converter));
        assertEquals(CommonValues.SHORT_ZERO, NumberConversions.toShortZero(expected, converter));
    }

    @Test
    public void testToInt_and_ToIntZero() {
        Integer expected = 123456;
        assertEquals(expected.intValue(), NumberConversions.toInt(expected, converter));
        assertEquals(CommonValues.INTEGER_ZERO, NumberConversions.toIntZero(expected, converter));
    }

    @Test
    public void testToLong_and_ToLongZero() {
        Long expected = 1234567890123L;
        assertEquals(expected.longValue(), NumberConversions.toLong(expected, converter));
        assertEquals(CommonValues.LONG_ZERO, NumberConversions.toLongZero(expected, converter));
    }

    @Test
    public void testToFloat_and_ToFloatZero() {
        Float expected = 12.34f;
        assertEquals(expected.floatValue(), NumberConversions.toFloat(expected, converter), 0f);
        assertEquals(CommonValues.FLOAT_ZERO, NumberConversions.toFloatZero(expected, converter));
    }

    @Test
    public void testFloatToString() {
        Float zero = 0f;
        Float nonZero = 3.14f;
        assertEquals("0", NumberConversions.floatToString(zero, converter));
        assertEquals(nonZero.toString(), NumberConversions.floatToString(nonZero, converter));
    }

    @Test
    public void testToDouble_and_ToDoubleZero() {
        Double expected = 12345.6789;
        assertEquals(expected.doubleValue(), NumberConversions.toDouble(expected, converter), 0d);
        assertEquals(CommonValues.DOUBLE_ZERO, NumberConversions.toDoubleZero(expected, converter));
    }

    @Test
    public void testDoubleToString() {
        Double zero = 0d;
        Double nonZero = -2.71828;
        assertEquals("0", NumberConversions.doubleToString(zero, converter));
        assertEquals(nonZero.toString(), NumberConversions.doubleToString(nonZero, converter));
    }

    @Test
    public void testIntegerTypeToBigDecimalAndBigInteger() {
        Long value = 9876543210L;
        BigDecimal bd = NumberConversions.integerTypeToBigDecimal(value, converter);
        BigInteger bi = NumberConversions.integerTypeToBigInteger(value, converter);
        assertEquals(BigDecimal.valueOf(value), bd);
        assertEquals(BigInteger.valueOf(value), bi);
    }

    @Test
    public void testAtomicConversions() {
        Integer intVal = 42;
        Long longVal = 123456L;

        AtomicInteger ai = NumberConversions.toAtomicInteger(intVal, converter);
        AtomicLong al = NumberConversions.toAtomicLong(longVal, converter);
        AtomicBoolean abTrue = NumberConversions.toAtomicBoolean(1L, converter);
        AtomicBoolean abFalse = NumberConversions.toAtomicBoolean(0L, converter);

        assertEquals(intVal.intValue(), ai.get());
        assertEquals(longVal.longValue(), al.get());
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    @Test
    public void testFloatingPointToBigDecimalAndBigInteger() {
        Double value = 12345.6789;
        BigDecimal bd = NumberConversions.floatingPointToBigDecimal(value, converter);
        BigInteger bi = NumberConversions.floatingPointToBigInteger(value, converter);

        assertEquals(BigDecimal.valueOf(value), bd);
        // 12345.6789 -> floor = 12345
        assertEquals(BigInteger.valueOf(12345), bi);
    }

    @Test
    public void testFloatingPointToBigInteger_RoundingNegative() {
        Double value = -2.7;
        BigInteger bi = NumberConversions.floatingPointToBigInteger(value, converter);
        // -2.7 -> ceil = -2
        assertEquals(BigInteger.valueOf(-2), bi);
    }

    @Test
    public void testIsIntTypeNotZero() {
        assertTrue(NumberConversions.isIntTypeNotZero(5, converter));
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

    @Test
    public void testToCharacter_ValidAndInvalid() {
        // within char range
        char c = NumberConversions.toCharacter(65, converter); // 'A'
        assertEquals('A', c);

        // out of range should throw IllegalArgumentException
        try {
            NumberConversions.toCharacter(70000, converter);
            fail("Expected IllegalArgumentException for value out of char range");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("out of range"));
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Date / Time conversions                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDate() {
        long epochMillis = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        Date date = NumberConversions.toDate(epochMillis, converter);
        assertEquals(epochMillis, date.getTime());
    }

    @Test
    public void testToDuration() {
        long millis = 12345L;
        Duration duration = NumberConversions.toDuration(millis, converter);
        assertEquals(millis, duration.toMillis());
    }

    @Test
    public void testToInstant() {
        long millis = 1609459200123L; // 2021‑01‑01T00:00:00.123Z
        Instant instant = NumberConversions.toInstant(millis, converter);
        assertEquals(millis, instant.toEpochMilli());
    }

    @Test
    public void testToSqlDate() {
        long millis = 1583020800000L; // 2020‑03‑01T00:00:00Z
        java.sql.Date sqlDate = NumberConversions.toSqlDate(millis, converter);
        assertEquals(millis, sqlDate.getTime());
    }

    @Test
    public void testToTimestamp() {
        long millis = 1622505600456L; // 2021‑06‑01T00:00:00.456Z
        Timestamp ts = NumberConversions.toTimestamp(millis, converter);
        assertEquals(millis, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        long millis = 1672531200000L; // 2023‑01‑01T00:00:00Z
        Calendar cal = NumberConversions.toCalendar(millis, converter);
        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void testZonedAndOffsetDateTimeConversions() {
        long millis = 1630454400000L; // 2021‑09‑01T00:00:00Z
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(millis, converter);
        OffsetDateTime odt = NumberConversions.toOffsetDateTime(millis, converter);
        ZoneId zone = converter.getOptions().getZoneId();

        assertEquals(Instant.ofEpochMilli(millis).atZone(zone), zdt);
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testLocalDateAndLocalDateTime() {
        long millis = 1609459200000L; // 2021‑01‑01T00:00:00Z
        LocalDate ld = NumberConversions.toLocalDate(millis, converter);
        LocalDateTime ldt = NumberConversions.toLocalDateTime(millis, converter);
        ZoneId zone = converter.getOptions().getZoneId();

        ZonedDateTime zdt = Instant.ofEpochMilli(millis).atZone(zone);
        assertEquals(zdt.toLocalDate(), ld);
        assertEquals(zdt.toLocalDateTime(), ldt);
    }

    @Test
    public void testToYear() {
        Short shortYear = 2022;
        Year year = NumberConversions.toYear(shortYear, converter);
        assertEquals(2022, year.getValue());
    }
}
