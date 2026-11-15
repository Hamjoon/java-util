package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;

/**
 * Comprehensive JUnit‑4 test suite for {@link Converter}.
 *
 * The tests verify:
 * <ul>
 *   <li>Correct handling of {@code null} for the {@code convert2*} (primitive/zero) and {@code convertTo*} (null) families.</li>
 *   <li>Typical successful conversions for the most common source/target pairs.</li>
 *   <li>Behaviour of the conversion‑support query APIs.</li>
 *   <li>Custom conversion registration via {@code addConversion}.</li>
 *   <li>Deprecated utility methods that delegate to the core converter.</li>
 * </ul>
 *
 * All tests use only the public API that appears in the supplied source files.
 */
public class ConverterTest
{
    // Reset the singleton instance before each test to avoid cross‑test interference
    @Before
    public void resetSingleton()
    {
        // The library does not provide a reset method; creating a fresh instance is sufficient
        // because the static field is mutable and we can replace it.
        Converter.instance = new com.cedarsoftware.util.convert.Converter(new DefaultConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /*  Null handling for primitive‑zero (convert2*) methods                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConvert2String_Null()
    {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvert2BigDecimal_Null()
    {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
    }

    @Test
    public void testConvert2BigInteger_Null()
    {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
    }

    @Test
    public void testConvert2Char_Null()
    {
        assertEquals((char)0, Converter.convert2char(null));
    }

    @Test
    public void testConvert2Byte_Null()
    {
        assertEquals((byte)0, Converter.convert2byte(null));
    }

    @Test
    public void testConvert2Short_Null()
    {
        assertEquals((short)0, Converter.convert2short(null));
    }

    @Test
    public void testConvert2Int_Null()
    {
        assertEquals(0, Converter.convert2int(null));
    }

    @Test
    public void testConvert2Long_Null()
    {
        assertEquals(CommonValues.LONG_ZERO, Converter.convert2long(null));
    }

    @Test
    public void testConvert2Float_Null()
    {
        assertEquals(CommonValues.FLOAT_ZERO, Converter.convert2float(null));
    }

    @Test
    public void testConvert2Double_Null()
    {
        assertEquals(CommonValues.DOUBLE_ZERO, Converter.convert2double(null));
    }

    @Test
    public void testConvert2Boolean_Null()
    {
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testConvert2AtomicInteger_Null()
    {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        assertNotNull(ai);
        assertEquals(0, ai.get());
    }

    @Test
    public void testConvert2AtomicLong_Null()
    {
        AtomicLong al = Converter.convert2AtomicLong(null);
        assertNotNull(al);
        assertEquals(0L, al.get());
    }

    @Test
    public void testConvert2AtomicBoolean_Null()
    {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  Null handling for object (convertTo*) methods                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConvertToString_Null()
    {
        assertNull(Converter.convertToString(null));
    }

    @Test
    public void testConvertToBigDecimal_Null()
    {
        assertNull(Converter.convertToBigDecimal(null));
    }

    @Test
    public void testConvertToBigInteger_Null()
    {
        assertNull(Converter.convertToBigInteger(null));
    }

    @Test
    public void testConvertToCharacter_Null()
    {
        assertNull(Converter.convertToCharacter(null));
    }

    @Test
    public void testConvertToByte_Null()
    {
        assertNull(Converter.convertToByte(null));
    }

    @Test
    public void testConvertToShort_Null()
    {
        assertNull(Converter.convertToShort(null));
    }

    @Test
    public void testConvertToInteger_Null()
    {
        assertNull(Converter.convertToInteger(null));
    }

    @Test
    public void testConvertToLong_Null()
    {
        assertNull(Converter.convertToLong(null));
    }

    @Test
    public void testConvertToFloat_Null()
    {
        assertNull(Converter.convertToFloat(null));
    }

    @Test
    public void testConvertToDouble_Null()
    {
        assertNull(Converter.convertToDouble(null));
    }

    @Test
    public void testConvertToBoolean_Null()
    {
        assertNull(Converter.convertToBoolean(null));
    }

    @Test
    public void testConvertToAtomicInteger_Null()
    {
        assertNull(Converter.convertToAtomicInteger(null));
    }

    @Test
    public void testConvertToAtomicLong_Null()
    {
        assertNull(Converter.convertToAtomicLong(null));
    }

    @Test
    public void testConvertToAtomicBoolean_Null()
    {
        assertNull(Converter.convertToAtomicBoolean(null));
    }

    @Test
    public void testConvertToDate_Null()
    {
        assertNull(Converter.convertToDate(null));
    }

    @Test
    public void testConvertToLocalDate_Null()
    {
        assertNull(Converter.convertToLocalDate(null));
    }

    @Test
    public void testConvertToLocalDateTime_Null()
    {
        assertNull(Converter.convertToLocalDateTime(null));
    }

    @Test
    public void testConvertToZonedDateTime_Null()
    {
        assertNull(Converter.convertToZonedDateTime(null));
    }

    @Test
    public void testConvertToCalendar_Null()
    {
        assertNull(Converter.convertToCalendar(null));
    }

    @Test
    public void testConvertToSqlDate_Null()
    {
        assertNull(Converter.convertToSqlDate(null));
    }

    @Test
    public void testConvertToTimestamp_Null()
    {
        assertNull(Converter.convertToTimestamp(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  Typical successful conversions                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testStringToInteger()
    {
        assertEquals(Integer.valueOf(42), Converter.convert("42", Integer.class));
        assertEquals(42, (int)Converter.convert2int("42"));
    }

    @Test
    public void testNumberToString()
    {
        assertEquals("123", Converter.convert2String(123));
        assertEquals("3.14", Converter.convert2String(3.14));
    }

    @Test
    public void testStringToBigDecimal()
    {
        BigDecimal expected = new BigDecimal("12345.6789");
        assertEquals(expected, Converter.convert2BigDecimal("12345.6789"));
        assertEquals(expected, Converter.convertToBigDecimal("12345.6789"));
    }

    @Test
    public void testEmptyStringToBigDecimal()
    {
        // Empty string is interpreted as zero by the underlying converter
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(""));
        assertEquals(BigDecimal.ZERO, Converter.convertToBigDecimal(""));
    }

    @Test
    public void testStringToBigInteger()
    {
        BigInteger expected = new BigInteger("9876543210");
        assertEquals(expected, Converter.convert2BigInteger("9876543210"));
        assertEquals(expected, Converter.convertToBigInteger("9876543210"));
    }

    @Test
    public void testCharConversions()
    {
        assertEquals('X', Converter.convert2char("X"));
        assertEquals(Character.valueOf('Y'), Converter.convertToCharacter('Y'));
    }

    @Test
    public void testByteConversions()
    {
        assertEquals((byte)7, Converter.convert2byte("7"));
        assertEquals(Byte.valueOf((byte)8), Converter.convertToByte((byte)8));
    }

    @Test
    public void testShortConversions()
    {
        assertEquals((short)9, Converter.convert2short("9"));
        assertEquals(Short.valueOf((short)10), Converter.convertToShort((short)10));
    }

    @Test
    public void testFloatConversions()
    {
        assertEquals(1.23f, Converter.convert2float("1.23"), 0.00001f);
        assertEquals(Float.valueOf(4.56f), Converter.convertToFloat(4.56f));
    }

    @Test
    public void testDoubleConversions()
    {
        assertEquals(2.34d, Converter.convert2double("2.34"), 0.0000001d);
        assertEquals(Double.valueOf(5.67d), Converter.convertToDouble(5.67d));
    }

    @Test
    public void testBooleanConversions()
    {
        assertTrue(Converter.convert2boolean("true"));
        assertFalse(Converter.convert2boolean("false"));
        assertEquals(Boolean.TRUE, Converter.convertToBoolean("true"));
        assertEquals(Boolean.FALSE, Converter.convertToBoolean("false"));
    }

    @Test
    public void testAtomicIntegerConversions()
    {
        AtomicInteger ai = Converter.convert2AtomicInteger(123);
        assertNotNull(ai);
        assertEquals(123, ai.get());

        AtomicInteger nullAi = Converter.convertToAtomicInteger(null);
        assertNull(nullAi);
    }

    @Test
    public void testAtomicLongConversions()
    {
        AtomicLong al = Converter.convert2AtomicLong(456L);
        assertNotNull(al);
        assertEquals(456L, al.get());

        assertNull(Converter.convertToAtomicLong(null));
    }

    @Test
    public void testAtomicBooleanConversions()
    {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(true);
        assertNotNull(ab);
        assertTrue(ab.get());

        assertNull(Converter.convertToAtomicBoolean(null));
    }

    @Test
    public void testDateAndTemporalConversions()
    {
        // java.util.Date from a long epoch
        Date now = new Date();
        assertEquals(now, Converter.convertToDate(now));

        // java.sql.Timestamp -> java.util.Date
        Timestamp ts = new Timestamp(now.getTime());
        assertEquals(new Date(ts.getTime()), Converter.convertToDate(ts));

        // LocalDate -> java.util.Date (midnight at system default zone)
        LocalDate ld = LocalDate.of(2020, 5, 15);
        Date ldDate = Converter.convertToDate(ld);
        assertNotNull(ldDate);
        assertEquals(ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                     ldDate.getTime());

        // LocalDateTime -> java.util.Date
        LocalDateTime ldt = LocalDateTime.of(2021, 3, 10, 12, 30, 45);
        Date ldtDate = Converter.convertToDate(ldt);
        assertNotNull(ldtDate);
        assertEquals(ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                     ldtDate.getTime());

        // ZonedDateTime -> java.util.Date
        ZonedDateTime zdt = ZonedDateTime.of(2022, 1, 2, 3, 4, 5, 0, ZoneId.of("UTC"));
        Date zdtDate = Converter.convertToDate(zdt);
        assertNotNull(zdtDate);
        assertEquals(zdt.toInstant().toEpochMilli(), zdtDate.getTime());

        // Calendar conversion
        Calendar cal = Converter.convertToCalendar(now);
        assertNotNull(cal);
        assertEquals(now.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testTemporalToMillisDeprecatedMethods()
    {
        LocalDate ld = LocalDate.of(1970, 1, 1);
        assertEquals(0L, Converter.localDateToMillis(ld));

        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        assertEquals(0L, Converter.localDateTimeToMillis(ldt));

        ZonedDateTime zdt = ZonedDateTime.ofInstant(java.time.Instant.EPOCH, ZoneId.of("UTC"));
        assertEquals(0L, Converter.zonedDateTimeToMillis(zdt));
    }

    /* ---------------------------------------------------------------------- */
    /*  Conversion‑support query APIs                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsConversionSupportedFor_KnownPair()
    {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
        assertTrue(Converter.isConversionSupportedFor(Integer.class, String.class));
    }

    @Test
    public void testIsConversionSupportedFor_UnknownPair()
    {
        assertFalse(Converter.isConversionSupportedFor(String.class, Thread.class));
    }

    @Test
    public void testAllSupportedConversions_NotEmpty()
    {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
        // At least one source class should have a target set
        boolean found = map.values().stream().anyMatch(set -> set != null && !set.isEmpty());
        assertTrue("Expected at least one source with supported targets", found);
    }

    @Test
    public void testGetSupportedConversions_NotEmpty()
    {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
        boolean found = map.values().stream().anyMatch(set -> set != null && !set.isEmpty());
        assertTrue("Expected at least one source name with supported target names", found);
    }

    /* ---------------------------------------------------------------------- */
    /*  Custom conversion registration and verification                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddCustomConversion()
    {
        // Define a conversion from Integer to String that prefixes "custom-"
        Convert<String> custom = new Convert<String>()
        {
            @Override
            public String convert(Object from, com.cedarsoftware.util.convert.Converter converter)
            {
                return "custom-" + from.toString();
            }
        };

        // Register the conversion; there should be no previous conversion for this pair
        Convert<?> previous = Converter.instance.addConversion(Integer.class, String.class, custom);
        assertNull(previous);

        // Verify that the new conversion is now used
        String result = Converter.convert(99, String.class);
        assertEquals("custom-99", result);

        // Clean‑up: replace the conversion with the default (null) to avoid side‑effects on other tests
        Converter.instance.addConversion(Integer.class, String.class, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Additional edge cases                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConvert_StringEmptyToInt()
    {
        // Empty string to int should result in logical zero (the underlying converter treats it as 0)
        assertEquals(0, Converter.convert2int(""));
    }

    @Test
    public void testConvert_StringEmptyToBoolean()
    {
        // Empty string to Boolean should be false for primitive conversion, null for wrapper conversion
        assertFalse(Converter.convert2boolean(""));
        assertNull(Converter.convertToBoolean(""));
    }
}
