package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class ConverterTest {

    private Converter customConverter;

    @Before
    public void setUp() {
        // a fresh converter instance for custom conversion tests
        customConverter = new Converter(new DefaultConverterOptions());
    }

    /*-------------------- String conversion --------------------*/

    @Test
    public void testConvert2String_NullReturnsEmpty() {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvertToString_NullReturnsNull() {
        assertNull(Converter.convertToString(null));
    }

    @Test
    public void testConvert2String_NonNull() {
        assertEquals("123", Converter.convert2String(123));
        assertEquals("true", Converter.convert2String(true));
    }

    /*-------------------- BigDecimal conversion --------------------*/

    @Test
    public void testConvert2BigDecimal_NullReturnsZero() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
    }

    @Test
    public void testConvertToBigDecimal_NullReturnsNull() {
        assertNull(Converter.convertToBigDecimal(null));
    }

    @Test
    public void testConvert2BigDecimal_FromString() {
        assertEquals(new BigDecimal("45.67"), Converter.convert2BigDecimal("45.67"));
    }

    @Test
    public void testConvertToBigDecimal_FromString() {
        assertEquals(new BigDecimal("89.01"), Converter.convertToBigDecimal("89.01"));
    }

    /*-------------------- BigInteger conversion --------------------*/

    @Test
    public void testConvert2BigInteger_NullReturnsZero() {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
    }

    @Test
    public void testConvertToBigInteger_NullReturnsNull() {
        assertNull(Converter.convertToBigInteger(null));
    }

    @Test
    public void testConvert2BigInteger_FromString() {
        assertEquals(new BigInteger("12345678901234567890"), Converter.convert2BigInteger("12345678901234567890"));
    }

    @Test
    public void testConvertToBigInteger_FromString() {
        assertEquals(new BigInteger("98765432109876543210"), Converter.convertToBigInteger("98765432109876543210"));
    }

    /*-------------------- Primitive and Wrapper conversion --------------------*/

    @Test
    public void testConvert2char_NullReturnsZeroChar() {
        assertEquals('\0', Converter.convert2char(null));
    }

    @Test
    public void testConvert2char_FromString() {
        assertEquals('Z', Converter.convert2char("Z"));
    }

    @Test
    public void testConvertToCharacter_NullReturnsNull() {
        assertNull(Converter.convertToCharacter(null));
    }

    @Test
    public void testConvertToCharacter_FromString() {
        assertEquals(Character.valueOf('Y'), Converter.convertToCharacter("Y"));
    }

    @Test
    public void testConvert2byte_NullReturnsZero() {
        assertEquals((byte) 0, Converter.convert2byte(null));
    }

    @Test
    public void testConvert2byte_FromString() {
        assertEquals((byte) 127, Converter.convert2byte("127"));
    }

    @Test
    public void testConvertToByte_NullReturnsNull() {
        assertNull(Converter.convertToByte(null));
    }

    @Test
    public void testConvertToByte_FromString() {
        assertEquals(Byte.valueOf((byte) -42), Converter.convertToByte("-42"));
    }

    @Test
    public void testConvert2short_NullReturnsZero() {
        assertEquals((short) 0, Converter.convert2short(null));
    }

    @Test
    public void testConvert2short_FromString() {
        assertEquals((short) 321, Converter.convert2short("321"));
    }

    @Test
    public void testConvertToShort_NullReturnsNull() {
        assertNull(Converter.convertToShort(null));
    }

    @Test
    public void testConvertToShort_FromString() {
        assertEquals(Short.valueOf((short) -123), Converter.convertToShort("-123"));
    }

    @Test
    public void testConvert2int_NullReturnsZero() {
        assertEquals(0, Converter.convert2int(null));
    }

    @Test
    public void testConvert2int_FromString() {
        assertEquals(2021, Converter.convert2int("2021"));
    }

    @Test
    public void testConvertToInteger_NullReturnsNull() {
        assertNull(Converter.convertToInteger(null));
    }

    @Test
    public void testConvertToInteger_FromString() {
        assertEquals(Integer.valueOf(42), Converter.convertToInteger("42"));
    }

    @Test
    public void testConvert2long_NullReturnsZero() {
        assertEquals(0L, Converter.convert2long(null));
    }

    @Test
    public void testConvert2long_FromString() {
        assertEquals(123456789L, Converter.convert2long("123456789"));
    }

    @Test
    public void testConvertToLong_NullReturnsNull() {
        assertNull(Converter.convertToLong(null));
    }

    @Test
    public void testConvertToLong_FromString() {
        assertEquals(Long.valueOf(9876543210L), Converter.convertToLong("9876543210"));
    }

    @Test
    public void testConvert2float_NullReturnsZero() {
        assertEquals(0.0f, Converter.convert2float(null), 0.0f);
    }

    @Test
    public void testConvert2float_FromString() {
        assertEquals(3.14f, Converter.convert2float("3.14"), 0.0001f);
    }

    @Test
    public void testConvertToFloat_NullReturnsNull() {
        assertNull(Converter.convertToFloat(null));
    }

    @Test
    public void testConvertToFloat_FromString() {
        assertEquals(Float.valueOf(2.718f), Converter.convertToFloat("2.718"));
    }

    @Test
    public void testConvert2double_NullReturnsZero() {
        assertEquals(0.0d, Converter.convert2double(null), 0.0d);
    }

    @Test
    public void testConvert2double_FromString() {
        assertEquals(6.022e23, Converter.convert2double("6.022e23"), 1e10);
    }

    @Test
    public void testConvertToDouble_NullReturnsNull() {
        assertNull(Converter.convertToDouble(null));
    }

    @Test
    public void testConvertToDouble_FromString() {
        assertEquals(Double.valueOf(1.618), Converter.convertToDouble("1.618"));
    }

    @Test
    public void testConvert2boolean_NullReturnsFalse() {
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testConvert2boolean_FromString() {
        assertTrue(Converter.convert2boolean("true"));
        assertFalse(Converter.convert2boolean("false"));
    }

    @Test
    public void testConvertToBoolean_NullReturnsNull() {
        assertNull(Converter.convertToBoolean(null));
    }

    @Test
    public void testConvertToBoolean_FromString() {
        assertEquals(Boolean.TRUE, Converter.convertToBoolean("true"));
        assertEquals(Boolean.FALSE, Converter.convertToBoolean("false"));
    }

    /*-------------------- Atomic types --------------------*/

    @Test
    public void testConvert2AtomicInteger_NullReturnsZero() {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        assertNotNull(ai);
        assertEquals(0, ai.get());
    }

    @Test
    public void testConvert2AtomicInteger_FromInteger() {
        AtomicInteger ai = Converter.convert2AtomicInteger(7);
        assertEquals(7, ai.get());
    }

    @Test
    public void testConvert2AtomicLong_NullReturnsZero() {
        AtomicLong al = Converter.convert2AtomicLong(null);
        assertNotNull(al);
        assertEquals(0L, al.get());
    }

    @Test
    public void testConvert2AtomicLong_FromLong() {
        AtomicLong al = Converter.convert2AtomicLong(123L);
        assertEquals(123L, al.get());
    }

    @Test
    public void testConvert2AtomicBoolean_NullReturnsFalse() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    @Test
    public void testConvert2AtomicBoolean_FromBoolean() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(true);
        assertTrue(ab.get());
    }

    /*-------------------- Date / Time conversion --------------------*/

    @Test
    public void testConvertToDate_FromTimestamp() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Date d = Converter.convertToDate(ts);
        assertEquals(ts.getTime(), d.getTime());
    }

    @Test
    public void testConvertToTimestamp_FromDate() {
        Date now = new Date();
        Timestamp ts = Converter.convertToTimestamp(now);
        assertEquals(now.getTime(), ts.getTime());
    }

    @Test
    public void testConvertToSqlDate_FromDate() {
        Date now = new Date();
        java.sql.Date sqlDate = Converter.convertToSqlDate(now);
        // sqlDate truncates time portion; compare only the day component
        assertEquals(now.getTime() / (24 * 60 * 60 * 1000), sqlDate.getTime() / (24 * 60 * 60 * 1000));
    }

    @Test
    public void testConvertToLocalDate_FromDate() {
        Date now = new Date();
        LocalDate ld = Converter.convertToLocalDate(now);
        LocalDate expected = LocalDate.ofInstant(now.toInstant(), ZoneOffset.systemDefault());
        assertEquals(expected, ld);
    }

    @Test
    public void testConvertToLocalDateTime_FromDate() {
        Date now = new Date();
        LocalDateTime ldt = Converter.convertToLocalDateTime(now);
        LocalDateTime expected = LocalDateTime.ofInstant(now.toInstant(), ZoneOffset.systemDefault());
        assertEquals(expected, ldt);
    }

    @Test
    public void testConvertToZonedDateTime_FromDate() {
        Date now = new Date();
        ZonedDateTime zdt = Converter.convertToZonedDateTime(now);
        ZonedDateTime expected = ZonedDateTime.ofInstant(now.toInstant(), ZoneOffset.systemDefault());
        assertEquals(expected, zdt);
    }

    @Test
    public void testConvertToCalendar_FromDate() {
        Date now = new Date();
        Calendar cal = Converter.convertToCalendar(now);
        assertEquals(now.getTime(), cal.getTimeInMillis());
    }

    /*-------------------- Deprecated epoch conversion helpers --------------------*/

    @Test
    public void testLocalDateToMillis() {
        LocalDate ld = LocalDate.of(1970, 1, 2); // one day after epoch
        long millis = Converter.localDateToMillis(ld);
        assertEquals(24L * 60 * 60 * 1000, millis);
    }

    @Test
    public void testLocalDateTimeToMillis() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 1, 0, 0);
        long millis = Converter.localDateTimeToMillis(ldt);
        assertEquals(60L * 60 * 1000, millis);
    }

    @Test
    public void testZonedDateTimeToMillis() {
        ZonedDateTime zdt = ZonedDateTime.of(1970, 1, 1, 0, 0, 1, 0, ZoneOffset.UTC);
        long millis = Converter.zonedDateTimeToMillis(zdt);
        assertEquals(1000L, millis);
    }

    /*-------------------- Support query methods --------------------*/

    @Test
    public void testIsConversionSupportedFor_KnownConversion() {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
        assertTrue(Converter.isConversionSupportedFor(Integer.class, String.class));
    }

    @Test
    public void testAllSupportedConversions_NotEmpty() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
        // At least one source type should have a non‑empty target set
        boolean found = map.values().stream().anyMatch(set -> set != null && !set.isEmpty());
        assertTrue(found);
    }

    @Test
    public void testGetSupportedConversions_NotEmpty() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
        boolean found = map.values().stream().anyMatch(set -> set != null && !set.isEmpty());
        assertTrue(found);
    }

    /*-------------------- Custom conversion via addConversion --------------------*/

    @Test
    public void testAddCustomConversion() {
        // custom conversion: Integer -> String returns "custom-" + value
        Convert<String> custom = (from, conv) -> "custom-" + from.toString();

        Convert<?> previous = customConverter.addConversion(Integer.class, String.class, custom);
        assertNull(previous); // no previous conversion for this pair

        String result = customConverter.convert(55, String.class);
        assertEquals("custom-55", result);
    }

    @Test
    public void testAddCustomConversion_OverridesExisting() {
        // First add a conversion, then replace it
        Convert<String> first = (from, conv) -> "first-" + from;
        customConverter.addConversion(Integer.class, String.class, first);
        String firstResult = customConverter.convert(1, String.class);
        assertEquals("first-1", firstResult);

        Convert<String> second = (from, conv) -> "second-" + from;
        Convert<?> previous = customConverter.addConversion(Integer.class, String.class, second);
        assertNotNull(previous); // previous conversion existed

        String secondResult = customConverter.convert(1, String.class);
        assertEquals("second-1", secondResult);
    }
}
