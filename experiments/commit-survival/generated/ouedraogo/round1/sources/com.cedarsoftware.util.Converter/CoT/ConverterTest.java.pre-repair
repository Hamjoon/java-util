package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.Convert;

/**
 * JUnit‑4 test suite for {@link Converter}.
 * Covers normal operation, edge‑case handling and error conditions.
 */
public class ConverterTest {

    // -------------------------------------------------------------------------
    //  Basic functionality tests
    // -------------------------------------------------------------------------

    @Test
    public void testConvert2String() {
        assertEquals("123", Converter.convert2String(123));
    }

    @Test
    public void testConvertToString() {
        assertEquals("true", Converter.convertToString(true));
    }

    @Test
    public void testConvert2BigDecimal() {
        assertEquals(new BigDecimal("42.5"), Converter.convert2BigDecimal("42.5"));
    }

    @Test
    public void testConvertToBigDecimal() {
        assertEquals(new BigDecimal("7"), Converter.convertToBigDecimal(7));
    }

    @Test
    public void testConvert2BigInteger() {
        assertEquals(BigInteger.valueOf(99), Converter.convert2BigInteger("99"));
    }

    @Test
    public void testConvertToBigInteger() {
        assertEquals(BigInteger.valueOf(15), Converter.convertToBigInteger(15L));
    }

    @Test
    public void testConvertToSqlDate() {
        java.util.Date now = new java.util.Date();
        java.sql.Date sql = Converter.convertToSqlDate(now);
        assertNotNull(sql);
        assertEquals(now.getTime(), sql.getTime());
    }

    @Test
    public void testConvertToTimestamp() {
        java.util.Date now = new java.util.Date();
        Timestamp ts = Converter.convertToTimestamp(now);
        assertNotNull(ts);
        assertEquals(now.getTime(), ts.getTime());
    }

    @Test
    public void testConvertToDate() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Date d = Converter.convertToDate(ts);
        assertNotNull(d);
        assertEquals(ts.getTime(), d.getTime());
    }

    @Test
    public void testConvertToLocalDate() {
        java.util.Date d = new java.util.Date();
        LocalDate ld = Converter.convertToLocalDate(d);
        assertNotNull(ld);
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        LocalDate expected = d.toInstant().atZone(zone).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test
    public void testConvertToLocalDateTime() {
        java.util.Date d = new java.util.Date();
        LocalDateTime ldt = Converter.convertToLocalDateTime(d);
        assertNotNull(ldt);
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        LocalDateTime expected = d.toInstant().atZone(zone).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testConvertToZonedDateTime() {
        java.util.Date d = new java.util.Date();
        ZonedDateTime zdt = Converter.convertToZonedDateTime(d);
        assertNotNull(zdt);
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        ZonedDateTime expected = d.toInstant().atZone(zone);
        assertEquals(expected, zdt);
    }

    @Test
    public void testConvertToCalendar() {
        java.util.Date d = new java.util.Date();
        Calendar cal = Converter.convertToCalendar(d);
        assertNotNull(cal);
        assertEquals(d.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testConvert2char() {
        assertEquals('A', Converter.convert2char("A"));
    }

    @Test
    public void testConvertToCharacter() {
        assertEquals(Character.valueOf('z'), Converter.convertToCharacter("z"));
    }

    @Test
    public void testConvert2byte() {
        assertEquals((byte)5, Converter.convert2byte("5"));
    }

    @Test
    public void testConvertToByte() {
        assertEquals(Byte.valueOf((byte)9), Converter.convertToByte("9"));
    }

    @Test
    public void testConvert2short() {
        assertEquals((short)12, Converter.convert2short("12"));
    }

    @Test
    public void testConvertToShort() {
        assertEquals(Short.valueOf((short)34), Converter.convertToShort("34"));
    }

    @Test
    public void testConvert2int() {
        assertEquals(12345, Converter.convert2int("12345"));
    }

    @Test
    public void testConvertToInteger() {
        assertEquals(Integer.valueOf(6789), Converter.convertToInteger("6789"));
    }

    @Test
    public void testConvert2long() {
        assertEquals(123456789L, Converter.convert2long("123456789"));
    }

    @Test
    public void testConvertToLong() {
        assertEquals(Long.valueOf(987654321L), Converter.convertToLong("987654321"));
    }

    @Test
    public void testConvert2float() {
        assertEquals(3.14f, Converter.convert2float("3.14"), 0.00001f);
    }

    @Test
    public void testConvertToFloat() {
        assertEquals(Float.valueOf(2.71f), Converter.convertToFloat("2.71"));
    }

    @Test
    public void testConvert2double() {
        assertEquals(6.283185, Converter.convert2double("6.283185"), 0.000001);
    }

    @Test
    public void testConvertToDouble() {
        assertEquals(Double.valueOf(1.618), Converter.convertToDouble("1.618"));
    }

    @Test
    public void testConvert2boolean() {
        assertTrue(Converter.convert2boolean("true"));
        assertFalse(Converter.convert2boolean("false"));
    }

    @Test
    public void testConvertToBoolean() {
        assertEquals(Boolean.TRUE, Converter.convertToBoolean("true"));
        assertEquals(Boolean.FALSE, Converter.convertToBoolean("false"));
    }

    @Test
    public void testConvert2AtomicInteger() {
        AtomicInteger ai = Converter.convert2AtomicInteger(7);
        assertNotNull(ai);
        assertEquals(7, ai.get());
    }

    @Test
    public void testConvertToAtomicInteger() {
        AtomicInteger ai = Converter.convertToAtomicInteger(11);
        assertNotNull(ai);
        assertEquals(11, ai.get());
    }

    @Test
    public void testConvert2AtomicLong() {
        AtomicLong al = Converter.convert2AtomicLong(123L);
        assertNotNull(al);
        assertEquals(123L, al.get());
    }

    @Test
    public void testConvertToAtomicLong() {
        AtomicLong al = Converter.convertToAtomicLong(456L);
        assertNotNull(al);
        assertEquals(456L, al.get());
    }

    @Test
    public void testConvert2AtomicBoolean() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(true);
        assertNotNull(ab);
        assertTrue(ab.get());
    }

    @Test
    public void testConvertToAtomicBoolean() {
        AtomicBoolean ab = Converter.convertToAtomicBoolean(false);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    // -------------------------------------------------------------------------
    //  Edge‑case / null handling tests
    // -------------------------------------------------------------------------

    @Test
    public void testConvert2StringNull() {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvertToStringNull() {
        assertNull(Converter.convertToString(null));
    }

    @Test
    public void testConvert2BigDecimalNullAndEmpty() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(""));
    }

    @Test
    public void testConvertToBigDecimalNullAndEmpty() {
        assertNull(Converter.convertToBigDecimal(null));
        assertEquals(BigDecimal.ZERO, Converter.convertToBigDecimal(""));
    }

    @Test
    public void testConvert2BigIntegerNullAndEmpty() {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(""));
    }

    @Test
    public void testConvertToBigIntegerNullAndEmpty() {
        assertNull(Converter.convertToBigInteger(null));
        assertEquals(BigInteger.ZERO, Converter.convertToBigInteger(""));
    }

    @Test
    public void testPrimitiveDefaultsWhenNull() {
        assertEquals('\0', Converter.convert2char(null));
        assertEquals((byte)0, Converter.convert2byte(null));
        assertEquals((short)0, Converter.convert2short(null));
        assertEquals(0, Converter.convert2int(null));
        assertEquals(0L, Converter.convert2long(null));
        assertEquals(CommonValues.FLOAT_ZERO.floatValue(), Converter.convert2float(null), 0.0f);
        assertEquals(CommonValues.DOUBLE_ZERO.doubleValue(), Converter.convert2double(null), 0.0d);
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testWrapperDefaultsWhenNull() {
        assertNull(Converter.convertToCharacter(null));
        assertNull(Converter.convertToByte(null));
        assertNull(Converter.convertToShort(null));
        assertNull(Converter.convertToInteger(null));
        assertNull(Converter.convertToLong(null));
        assertNull(Converter.convertToFloat(null));
        assertNull(Converter.convertToDouble(null));
        assertNull(Converter.convertToBoolean(null));
    }

    @Test
    public void testAtomicDefaultsWhenNull() {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        assertNotNull(ai);
        assertEquals(0, ai.get());

        AtomicLong al = Converter.convert2AtomicLong(null);
        assertNotNull(al);
        assertEquals(0L, al.get());

        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    // -------------------------------------------------------------------------
    //  Capability / error handling tests
    // -------------------------------------------------------------------------

    @Test
    public void testIsConversionSupportedFor() {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
        assertFalse(Converter.isConversionSupportedFor(Object.class, Thread.class));
    }

    @Test
    public void testAddCustomConversionAndUseIt() {
        // Custom conversion: reverse a String when converting String → String
        Convert<String> reverse = new Convert<String>() {
            @Override
            public String convert(Object from, com.cedarsoftware.util.convert.Converter converter) {
                String s = (from == null) ? "" : from.toString();
                return new StringBuilder(s).reverse().toString();
            }
        };
        // Register the custom conversion (overwrites the default String→String conversion)
        Converter.addConversion(String.class, String.class, reverse);

        // Verify that the new conversion logic is applied
        String result = Converter.convert("abcd", String.class);
        assertEquals("dcba", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertUnsupportedThrowsException() {
        // The library throws IllegalArgumentException for unsupported conversions
        Converter.convert(new Thread(), Integer.class);
    }
}
