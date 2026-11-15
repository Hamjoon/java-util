package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import com.cedarsoftware.util.convert.Converter as InternalConverter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

import static org.junit.Assert.*;

public class ConverterTest {

    /* ----------------------------------------------------------------------
     *  Helper – reset the global singleton after each test that mutates it.
     * ---------------------------------------------------------------------- */
    private InternalConverter originalInstance;

    @Before
    public void setUp() {
        // keep a reference to the original singleton so we can restore it later
        originalInstance = Converter.instance;
    }

    @After
    public void tearDown() {
        // restore the original singleton – this isolates the effects of addConversion()
        Converter.instance = originalInstance;
    }

    /* ----------------------------------------------------------------------
     *  1. convert(Object, Class<T>) – basic sanity check
     * ---------------------------------------------------------------------- */
    @Test
    public void testConvert_StringToInteger() {
        Integer result = Converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), result);
    }

    /* ----------------------------------------------------------------------
     *  2. isConversionSupportedFor
     * ---------------------------------------------------------------------- */
    @Test
    public void testIsConversionSupportedFor_KnownPair() {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
    }

    @Test
    public void testIsConversionSupportedFor_UnknownPair() {
        // there is no built‑in conversion from Object to Void
        assertFalse(Converter.isConversionSupportedFor(Object.class, Void.class));
    }

    /* ----------------------------------------------------------------------
     *  3. allSupportedConversions & getSupportedConversions
     * ---------------------------------------------------------------------- */
    @Test
    public void testAllSupportedConversions_NotEmpty() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
    }

    @Test
    public void testGetSupportedConversions_NotEmpty() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  4. addConversion – custom conversion from String to Boolean ("yes" => true)
     * ---------------------------------------------------------------------- */
    @Test
    public void testAddConversion_CustomBoolean() {
        Convert<Boolean> yesToTrue = new Convert<Boolean>() {
            @Override
            public Boolean convert(Object from, InternalConverter converter) {
                return "yes".equalsIgnoreCase(String.valueOf(from));
            }
        };
        Converter.instance.addConversion(String.class, Boolean.class, yesToTrue);

        assertTrue(Converter.isConversionSupportedFor(String.class, Boolean.class));
        assertTrue(Converter.convert("yes", Boolean.class));
        assertFalse(Converter.convert("no", Boolean.class));
    }

    /* ----------------------------------------------------------------------
     *  5. convert2String / convertToString
     * ---------------------------------------------------------------------- */
    @Test
    public void testConvert2String_NullReturnsEmpty() {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvertToString_NullReturnsNull() {
        assertNull(Converter.convertToString(null));
    }

    @Test
    public void testConvertString_NonNull() {
        assertEquals("456", Converter.convert2String(456));
        assertEquals("456", Converter.convertToString(456));
    }

    /* ----------------------------------------------------------------------
     *  6. BigDecimal conversions
     * ---------------------------------------------------------------------- */
    @Test
    public void testConvert2BigDecimal_NullAndEmpty() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(""));
    }

    @Test
    public void testConvertToBigDecimal_NullAndEmpty() {
        assertNull(Converter.convertToBigDecimal(null));
        assertEquals(BigDecimal.ZERO, Converter.convertToBigDecimal(""));
        assertEquals(new BigDecimal("10.5"),
                Converter.convertToBigDecimal("10.5"));
    }

    /* ----------------------------------------------------------------------
     *  7. BigInteger conversions
     * ---------------------------------------------------------------------- */
    @Test
    public void testConvert2BigInteger_NullAndEmpty() {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(""));
    }

    @Test
    public void testConvertToBigInteger_NullAndEmpty() {
        assertNull(Converter.convertToBigInteger(null));
        assertEquals(BigInteger.ZERO, Converter.convertToBigInteger(""));
        assertEquals(new BigInteger("12345678901234567890"),
                Converter.convertToBigInteger("12345678901234567890"));
    }

    /* ----------------------------------------------------------------------
     *  8. Date / Time conversions
     * ---------------------------------------------------------------------- */
    @Test
    public void testConvertToSqlDate_Null() {
        assertNull(Converter.convertToSqlDate(null));
    }

    @Test
    public void testConvertToSqlDate_Value() {
        java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
        assertEquals(now, Converter.convertToSqlDate(now));
    }

    @Test
    public void testConvertToTimestamp_Null() {
        assertNull(Converter.convertToTimestamp(null));
    }

    @Test
    public void testConvertToTimestamp_Value() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        assertEquals(ts, Converter.convertToTimestamp(ts));
    }

    @Test
    public void testConvertToDate_Null() {
        assertNull(Converter.convertToDate(null));
    }

    @Test
    public void testConvertToDate_Value() {
        Date d = new Date();
        assertEquals(d, Converter.convertToDate(d));
    }

    @Test
    public void testConvertToLocalDate_Null() {
        assertNull(Converter.convertToLocalDate(null));
    }

    @Test
    public void testConvertToLocalDate_Value() {
        LocalDate ld = LocalDate.now();
        assertEquals(ld, Converter.convertToLocalDate(ld));
    }

    @Test
    public void testConvertToLocalDateTime_Null() {
        assertNull(Converter.convertToLocalDateTime(null));
    }

    @Test
    public void testConvertToLocalDateTime_Value() {
        LocalDateTime ldt = LocalDateTime.now();
        assertEquals(ldt, Converter.convertToLocalDateTime(ldt));
    }

    @Test
    public void testConvertToZonedDateTime_Null() {
        assertNull(Converter.convertToZonedDateTime(null));
    }

    @Test
    public void testConvertToZonedDateTime_Value() {
        ZonedDateTime zdt = ZonedDateTime.now();
        assertEquals(zdt, Converter.convertToZonedDateTime(zdt));
    }

    @Test
    public void testConvertToCalendar_Null() {
        assertNull(Converter.convertToCalendar(null));
    }

    @Test
    public void testConvertToCalendar_Value() {
        Calendar cal = Calendar.getInstance();
        assertEquals(cal, Converter.convertToCalendar(cal));
    }

    /* ----------------------------------------------------------------------
     *  9. Primitive / Wrapper conversions – null handling
     * ---------------------------------------------------------------------- */
    @Test
    public void testConvert2char_NullReturnsZeroChar() {
        assertEquals('\0', Converter.convert2char(null));
    }

    @Test
    public void testConvertToCharacter_NullReturnsNull() {
        assertNull(Converter.convertToCharacter(null));
    }

    @Test
    public void testConvert2char_FromString() {
        assertEquals('A', Converter.convert2char("A"));
    }

    @Test
    public void testConvertToCharacter_FromString() {
        assertEquals(Character.valueOf('B'), Converter.convertToCharacter("B"));
    }

    @Test
    public void testConvert2byte_NullReturnsZero() {
        assertEquals((byte) 0, Converter.convert2byte(null));
    }

    @Test
    public void testConvertToByte_NullReturnsNull() {
        assertNull(Converter.convertToByte(null));
    }

    @Test
    public void testConvert2byte_FromString() {
        assertEquals((byte) 127, Converter.convert2byte("127"));
    }

    @Test
    public void testConvert2short_NullReturnsZero() {
        assertEquals((short) 0, Converter.convert2short(null));
    }

    @Test
    public void testConvertToShort_NullReturnsNull() {
        assertNull(Converter.convertToShort(null));
    }

    @Test
    public void testConvert2short_FromString() {
        assertEquals((short) 32767, Converter.convert2short("32767"));
    }

    @Test
    public void testConvert2int_NullReturnsZero() {
        assertEquals(0, Converter.convert2int(null));
    }

    @Test
    public void testConvertToInteger_NullReturnsNull() {
        assertNull(Converter.convertToInteger(null));
    }

    @Test
    public void testConvert2int_FromString() {
        assertEquals(42, Converter.convert2int("42"));
    }

    @Test
    public void testConvert2long_NullReturnsCommonZero() {
        assertEquals(CommonValues.LONG_ZERO, Long.valueOf(Converter.convert2long(null)));
    }

    @Test
    public void testConvertToLong_NullReturnsNull() {
        assertNull(Converter.convertToLong(null));
    }

    @Test
    public void testConvert2long_FromString() {
        assertEquals(123456789L, Converter.convert2long("123456789"));
    }

    @Test
    public void testConvert2float_NullReturnsCommonZero() {
        assertEquals(CommonValues.FLOAT_ZERO, Float.valueOf(Converter.convert2float(null)));
    }

    @Test
    public void testConvertToFloat_NullReturnsNull() {
        assertNull(Converter.convertToFloat(null));
    }

    @Test
    public void testConvert2float_FromString() {
        assertEquals(3.14f, Converter.convert2float("3.14"), 0.00001f);
    }

    @Test
    public void testConvert2double_NullReturnsCommonZero() {
        assertEquals(CommonValues.DOUBLE_ZERO, Double.valueOf(Converter.convert2double(null)));
    }

    @Test
    public void testConvertToDouble_NullReturnsNull() {
        assertNull(Converter.convertToDouble(null));
    }

    @Test
    public void testConvert2double_FromString() {
        assertEquals(2.71828, Converter.convert2double("2.71828"), 0.0000001);
    }

    @Test
    public void testConvert2boolean_NullReturnsFalse() {
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testConvertToBoolean_NullReturnsNull() {
        assertNull(Converter.convertToBoolean(null));
    }

    @Test
    public void testConvert2boolean_FromString() {
        assertTrue(Converter.convert2boolean("true"));
        assertFalse(Converter.convert2boolean("false"));
    }

    /* ----------------------------------------------------------------------
     * 10. Atomic conversions – null handling
     * ---------------------------------------------------------------------- */
    @Test
    public void testConvert2AtomicInteger_NullReturnsZero() {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        assertNotNull(ai);
        assertEquals(0, ai.get());
    }

    @Test
    public void testConvertToAtomicInteger_NullReturnsNull() {
        assertNull(Converter.convertToAtomicInteger(null));
    }

    @Test
    public void testConvert2AtomicLong_NullReturnsZero() {
        AtomicLong al = Converter.convert2AtomicLong(null);
        assertNotNull(al);
        assertEquals(0L, al.get());
    }

    @Test
    public void testConvertToAtomicLong_NullReturnsNull() {
        assertNull(Converter.convertToAtomicLong(null));
    }

    @Test
    public void testConvert2AtomicBoolean_NullReturnsFalse() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    @Test
    public void testConvertToAtomicBoolean_NullReturnsNull() {
        assertNull(Converter.convertToAtomicBoolean(null));
    }

    /* ----------------------------------------------------------------------
     * 11. Deprecated epoch‑millis helpers
     * ---------------------------------------------------------------------- */
    @Test
    public void testLocalDateToMillis() {
        LocalDate ld = LocalDate.of(1970, 1, 1);
        assertEquals(0L, Converter.localDateToMillis(ld));
    }

    @Test
    public void testLocalDateTimeToMillis() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0);
        assertEquals(0L, Converter.localDateTimeToMillis(ldt));
    }

    @Test
    public void testZonedDateTimeToMillis() {
        ZonedDateTime zdt = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0,
                java.time.ZoneOffset.UTC);
        assertEquals(0L, Converter.zonedDateTimeToMillis(zdt));
    }

    /* ----------------------------------------------------------------------
     * 12. Error / exception scenarios
     * ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testConvert2int_InvalidStringThrows() {
        // The underlying converter throws IllegalArgumentException for an illegal numeric string
        Converter.convert2int("not-a-number");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvert2boolean_InvalidStringThrows() {
        Converter.convert2boolean("not-boolean");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvert2char_InvalidStringThrows() {
        // A string longer than 1 character is not convertible to a primitive char
        Converter.convert2char("ab");
    }
}
