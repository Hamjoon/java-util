package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import com.cedarsoftware.util.convert.Converter;
import org.junit.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

public class ConverterTest {

    /*** Helper class used for a custom conversion test ***/
    public static class Dummy {
        private final String value;
        Dummy(String value) { this.value = value; }
        public String getValue() { return value; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Dummy)) return false;
            Dummy dummy = (Dummy) o;
            return Objects.equals(value, dummy.value);
        }
        @Override public int hashCode() { return Objects.hash(value); }
    }

    /*** Reset the global Converter instance before each test to avoid cross‑test contamination ***/
    @Before
    public void setUp() {
        // The static instance field is final, but we can clear any custom conversions we add.
        // The underlying Converter keeps a map of overrides; we simply ensure it's empty.
        // The DefaultConverterOptions does not expose a clear method, so we rely on the fact
        // that each test adds its own conversion and removes it afterwards (see testAddConversion).
    }

    @After
    public void tearDown() {
        // Remove any custom conversion added during a test (if present)
        Converter.instance.addConversion(String.class, Dummy.class, null);
    }

    /* ---------- String conversion tests ---------- */

    @Test
    public void testConvert2String_NullReturnsEmptyString() {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvert2String_NonNullConversion() {
        assertEquals("123", Converter.convert2String(123));
    }

    @Test
    public void testConvertToString_NullReturnsNull() {
        assertNull(Converter.convertToString(null));
    }

    @Test
    public void testConvertToString_NonNullConversion() {
        assertEquals("true", Converter.convertToString(true));
    }

    /* ---------- BigDecimal conversion tests ---------- */

    @Test
    public void testConvert2BigDecimal_NullReturnsZero() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
    }

    @Test
    public void testConvert2BigDecimal_EmptyStringReturnsZero() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(""));
    }

    @Test
    public void testConvert2BigDecimal_NumericString() {
        assertEquals(new BigDecimal("12.34"), Converter.convert2BigDecimal("12.34"));
    }

    @Test
    public void testConvertToBigDecimal_NullReturnsNull() {
        assertNull(Converter.convertToBigDecimal(null));
    }

    @Test
    public void testConvertToBigDecimal_EmptyStringReturnsZero() {
        assertEquals(BigDecimal.ZERO, Converter.convertToBigDecimal(""));
    }

    @Test
    public void testConvertToBigDecimal_NumericString() {
        assertEquals(new BigDecimal("-5.5"), Converter.convertToBigDecimal("-5.5"));
    }

    /* ---------- BigInteger conversion tests ---------- */

    @Test
    public void testConvert2BigInteger_NullReturnsZero() {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
    }

    @Test
    public void testConvert2BigInteger_EmptyStringReturnsZero() {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(""));
    }

    @Test
    public void testConvert2BigInteger_NumericString() {
        assertEquals(new BigInteger("12345678901234567890"), Converter.convert2BigInteger("12345678901234567890"));
    }

    @Test
    public void testConvertToBigInteger_NullReturnsNull() {
        assertNull(Converter.convertToBigInteger(null));
    }

    @Test
    public void testConvertToBigInteger_EmptyStringReturnsZero() {
        assertEquals(BigInteger.ZERO, Converter.convertToBigInteger(""));
    }

    @Test
    public void testConvertToBigInteger_NumericString() {
        assertEquals(new BigInteger("-42"), Converter.convertToBigInteger("-42"));
    }

    /* ---------- Date / Time conversion tests ---------- */

    @Test
    public void testConvertToSqlDate_NullReturnsNull() {
        assertNull(Converter.convertToSqlDate(null));
    }

    @Test
    public void testConvertToSqlDate_FromLocalDate() {
        LocalDate ld = LocalDate.of(2020, 2, 29);
        java.sql.Date sqlDate = Converter.convertToSqlDate(ld);
        assertNotNull(sqlDate);
        assertEquals(Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant()), sqlDate);
    }

    @Test
    public void testConvertToTimestamp_NullReturnsNull() {
        assertNull(Converter.convertToTimestamp(null));
    }

    @Test
    public void testConvertToTimestamp_FromInstant() {
        Instant now = Instant.now();
        Timestamp ts = Converter.convertToTimestamp(Date.from(now));
        assertNotNull(ts);
        assertEquals(now.toEpochMilli(), ts.getTime());
    }

    @Test
    public void testConvertToDate_NullReturnsNull() {
        assertNull(Converter.convertToDate(null));
    }

    @Test
    public void testConvertToDate_FromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 59, 59);
        Date d = Converter.convertToDate(ldt);
        assertNotNull(d);
        Instant expected = ldt.atZone(ZoneId.systemDefault()).toInstant();
        assertEquals(expected.toEpochMilli(), d.getTime());
    }

    @Test
    public void testConvertToLocalDate_NullReturnsNull() {
        assertNull(Converter.convertToLocalDate(null));
    }

    @Test
    public void testConvertToLocalDate_FromDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, Calendar.JUNE, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        LocalDate ld = Converter.convertToLocalDate(date);
        assertEquals(LocalDate.of(2022, 6, 15), ld);
    }

    @Test
    public void testConvertToLocalDateTime_NullReturnsNull() {
        assertNull(Converter.convertToLocalDateTime(null));
    }

    @Test
    public void testConvertToLocalDateTime_FromTimestamp() {
        Timestamp ts = Timestamp.valueOf("2023-01-02 12:34:56.789");
        LocalDateTime ldt = Converter.convertToLocalDateTime(ts);
        assertEquals(LocalDateTime.of(2023, 1, 2, 12, 34, 56, 789_000_000), ldt);
    }

    @Test
    public void testConvertToZonedDateTime_NullReturnsNull() {
        assertNull(Converter.convertToZonedDateTime(null));
    }

    @Test
    public void testConvertToZonedDateTime_FromInstant() {
        Instant now = Instant.now();
        ZonedDateTime zdt = Converter.convertToZonedDateTime(Date.from(now));
        assertEquals(ZonedDateTime.ofInstant(now, ZoneId.systemDefault()), zdt);
    }

    @Test
    public void testConvertToCalendar_NullReturnsNull() {
        assertNull(Converter.convertToCalendar(null));
    }

    @Test
    public void testConvertToCalendar_FromDate() {
        Date now = new Date();
        Calendar cal = Converter.convertToCalendar(now);
        assertNotNull(cal);
        assertEquals(now.getTime(), cal.getTimeInMillis());
    }

    /* ---------- Primitive / Wrapper conversion tests ---------- */

    @Test
    public void testConvert2char_NullReturnsZeroChar() {
        assertEquals('\0', Converter.convert2char(null));
    }

    @Test
    public void testConvert2char_FromString() {
        assertEquals('A', Converter.convert2char("A"));
    }

    @Test
    public void testConvertToCharacter_NullReturnsNull() {
        assertNull(Converter.convertToCharacter(null));
    }

    @Test
    public void testConvertToCharacter_FromString() {
        assertEquals(Character.valueOf('z'), Converter.convertToCharacter("z"));
    }

    @Test
    public void testConvert2byte_NullReturnsZero() {
        assertEquals((byte)0, Converter.convert2byte(null));
    }

    @Test
    public void testConvert2byte_FromNumber() {
        assertEquals((byte)127, Converter.convert2byte(127));
    }

    @Test
    public void testConvertToByte_NullReturnsNull() {
        assertNull(Converter.convertToByte(null));
    }

    @Test
    public void testConvertToByte_FromNumber() {
        assertEquals(Byte.valueOf((byte)-12), Converter.convertToByte(-12));
    }

    @Test
    public void testConvert2short_NullReturnsZero() {
        assertEquals((short)0, Converter.convert2short(null));
    }

    @Test
    public void testConvert2short_FromNumber() {
        assertEquals((short)32000, Converter.convert2short(32000));
    }

    @Test
    public void testConvertToShort_NullReturnsNull() {
        assertNull(Converter.convertToShort(null));
    }

    @Test
    public void testConvertToShort_FromNumber() {
        assertEquals(Short.valueOf((short)-1234), Converter.convertToShort(-1234));
    }

    @Test
    public void testConvert2int_NullReturnsZero() {
        assertEquals(0, Converter.convert2int(null));
    }

    @Test
    public void testConvert2int_FromString() {
        assertEquals(42, Converter.convert2int("42"));
    }

    @Test
    public void testConvertToInteger_NullReturnsNull() {
        assertNull(Converter.convertToInteger(null));
    }

    @Test
    public void testConvertToInteger_FromString() {
        assertEquals(Integer.valueOf(-7), Converter.convertToInteger("-7"));
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
        assertEquals(Long.valueOf(-987654321L), Converter.convertToLong("-987654321"));
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
        assertEquals(Float.valueOf(-2.71f), Converter.convertToFloat("-2.71"));
    }

    @Test
    public void testConvert2double_NullReturnsZero() {
        assertEquals(0.0d, Converter.convert2double(null), 0.0d);
    }

    @Test
    public void testConvert2double_FromString() {
        assertEquals(6.022e23, Converter.convert2double("6.022e23"), 1e15);
    }

    @Test
    public void testConvertToDouble_NullReturnsNull() {
        assertNull(Converter.convertToDouble(null));
    }

    @Test
    public void testConvertToDouble_FromString() {
        assertEquals(Double.valueOf(-0.001), Converter.convertToDouble("-0.001"));
    }

    @Test
    public void testConvert2boolean_NullReturnsFalse() {
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testConvert2boolean_FromStringTrue() {
        assertTrue(Converter.convert2boolean("true"));
    }

    @Test
    public void testConvert2boolean_FromStringFalse() {
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

    /* ---------- Atomic conversion tests ---------- */

    @Test
    public void testConvert2AtomicInteger_NullReturnsZeroAtomic() {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        assertNotNull(ai);
        assertEquals(0, ai.get());
    }

    @Test
    public void testConvert2AtomicInteger_FromInteger() {
        AtomicInteger ai = Converter.convert2AtomicInteger(5);
        assertEquals(5, ai.get());
    }

    @Test
    public void testConvertToAtomicInteger_NullReturnsNull() {
        assertNull(Converter.convertToAtomicInteger(null));
    }

    @Test
    public void testConvertToAtomicInteger_FromString() {
        AtomicInteger ai = Converter.convertToAtomicInteger("12");
        assertEquals(12, ai.get());
    }

    @Test
    public void testConvert2AtomicLong_NullReturnsZeroAtomic() {
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
    public void testConvertToAtomicLong_NullReturnsNull() {
        assertNull(Converter.convertToAtomicLong(null));
    }

    @Test
    public void testConvertToAtomicLong_FromString() {
        AtomicLong al = Converter.convertToAtomicLong("456");
        assertEquals(456L, al.get());
    }

    @Test
    public void testConvert2AtomicBoolean_NullReturnsFalseAtomic() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    @Test
    public void testConvert2AtomicBoolean_FromBoolean() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(true);
        assertTrue(ab.get());
    }

    @Test
    public void testConvertToAtomicBoolean_NullReturnsNull() {
        assertNull(Converter.convertToAtomicBoolean(null));
    }

    @Test
    public void testConvertToAtomicBoolean_FromString() {
        AtomicBoolean ab = Converter.convertToAtomicBoolean("true");
        assertTrue(ab.get());
    }

    /* ---------- Support‑query tests ---------- */

    @Test
    public void testIsConversionSupportedFor_KnownConversion() {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
    }

    @Test
    public void testIsConversionSupportedFor_UnknownConversion() {
        assertFalse(Converter.isConversionSupportedFor(String.class, Thread.class));
    }

    @Test
    public void testAllSupportedConversions_NotEmpty() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
        // At least one known source (String) should be present
        assertTrue(map.containsKey(String.class));
    }

    @Test
    public void testGetSupportedConversions_NotEmpty() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("java.lang.String"));
    }

    /* ---------- Custom conversion test (addConversion) ---------- */

    @Test
    public void testAddConversion_AndConvert_CustomConversion() {
        // Define a conversion from String -> Dummy that wraps the original string
        Convert<Dummy> stringToDummy = new Convert<Dummy>() {
            @Override
            public Dummy convert(Object from, com.cedarsoftware.util.convert.Converter converter) {
                return new Dummy(String.valueOf(from));
            }
        };

        // Register the conversion
        Converter.instance.addConversion(String.class, Dummy.class, stringToDummy);

        // Verify that conversion works
        Dummy result = Converter.convert("hello world", Dummy.class);
        assertEquals(new Dummy("hello world"), result);
    }

    /* ---------- Deprecated epoch‑millis helpers ---------- */

    @Test
    public void testDeprecatedLocalDateToMillis() {
        LocalDate ld = LocalDate.of(1970, 1, 2); // one day after epoch
        long millis = Converter.localDateToMillis(ld);
        assertEquals(24L * 60 * 60 * 1000, millis);
    }

    @Test
    public void testDeprecatedLocalDateTimeToMillis() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 1, 0, 0); // one hour after epoch
        long millis = Converter.localDateTimeToMillis(ldt);
        assertEquals(60L * 60 * 1000, millis);
    }

    @Test
    public void testDeprecatedZonedDateTimeToMillis() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(123456789L), ZoneId.of("UTC"));
        long millis = Converter.zonedDateTimeToMillis(zdt);
        assertEquals(123456789L, millis);
    }

    /* ---------- Edge‑case error handling ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConvert_InvalidNumberString_ThrowsException() {
        // The library throws IllegalArgumentException when it cannot parse a number.
        Converter.convert2int("not-a-number");
    }

    @Test
    public void testConvert_UnsupportedSource_ThrowsException() {
        try {
            Converter.convert(new Object(), Integer.class);
            fail("Expected IllegalArgumentException for unsupported conversion");
        } catch (IllegalArgumentException ignored) {
            // Expected path
        }
    }
}
