package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@link Converter}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Null handling for all “convert2*” convenience methods.</li>
 *   <li>Typical primitive / wrapper conversions.</li>
 *   <li>BigDecimal / BigInteger handling (including empty‑string shortcut).</li>
 *   <li>Date / Time conversions.</li>
 *   <li>Atomic types handling.</li>
 *   <li>String conversion shortcuts.</li>
 *   <li>Support‑query APIs (isConversionSupportedFor, isDirectConversionSupported,
 *       isSimpleTypeConversionSupported).</li>
 *   <li>Retrieval of the supported‑conversion maps.</li>
 *   <li>Custom conversion registration via {@code addConversion}.</li>
 *   <li>Deprecated millisecond helpers.</li>
 * </ul>
 * </p>
 */
public class ConverterTest {

    /* ---------------------------------------------------------------------- */
    /*  Simple null‑handling convenience methods                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConvert2String_Null() {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvertToString_Null() {
        assertNull(Converter.convertToString(null));
    }

    @Test
    public void testConvert2BigDecimal_Null() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
    }

    @Test
    public void testConvertToBigDecimal_Null() {
        assertNull(Converter.convertToBigDecimal(null));
    }

    @Test
    public void testConvert2BigInteger_Null() {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
    }

    @Test
    public void testConvertToBigInteger_Null() {
        assertNull(Converter.convertToBigInteger(null));
    }

    @Test
    public void testConvert2Char_Null() {
        assertEquals('\0', Converter.convert2char(null));
    }

    @Test
    public void testConvert2Byte_Null() {
        assertEquals((byte) 0, Converter.convert2byte(null));
    }

    @Test
    public void testConvert2Short_Null() {
        assertEquals((short) 0, Converter.convert2short(null));
    }

    @Test
    public void testConvert2Int_Null() {
        assertEquals(0, Converter.convert2int(null));
    }

    @Test
    public void testConvert2Long_Null() {
        assertEquals(CommonValues.LONG_ZERO, Converter.convert2long(null));
    }

    @Test
    public void testConvert2Float_Null() {
        assertEquals(CommonValues.FLOAT_ZERO, Converter.convert2float(null));
    }

    @Test
    public void testConvert2Double_Null() {
        assertEquals(CommonValues.DOUBLE_ZERO, Converter.convert2double(null));
    }

    @Test
    public void testConvert2Boolean_Null() {
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testConvert2AtomicInteger_Null() {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        assertNotNull(ai);
        assertEquals(0, ai.get());
    }

    @Test
    public void testConvert2AtomicLong_Null() {
        AtomicLong al = Converter.convert2AtomicLong(null);
        assertNotNull(al);
        assertEquals(0L, al.get());
    }

    @Test
    public void testConvert2AtomicBoolean_Null() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  Primitive / wrapper conversions                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testStringToInteger() {
        Integer i = Converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), i);
    }

    @Test
    public void testIntegerToString() {
        String s = Converter.convert(456, String.class);
        assertEquals("456", s);
    }

    @Test
    public void testStringToBoolean() {
        Boolean bTrue = Converter.convert("true", Boolean.class);
        Boolean bFalse = Converter.convert("false", Boolean.class);
        assertTrue(bTrue);
        assertFalse(bFalse);
    }

    @Test
    public void testBooleanToString() {
        assertEquals("true", Converter.convert(true, String.class));
        assertEquals("false", Converter.convert(false, String.class));
    }

    @Test
    public void testStringToChar() {
        Character c = Converter.convert("Z", Character.class);
        assertEquals(Character.valueOf('Z'), c);
    }

    @Test
    public void testCharToString() {
        String s = Converter.convert('Q', String.class);
        assertEquals("Q", s);
    }

    @Test
    public void testStringToByte() {
        Byte b = Converter.convert("12", Byte.class);
        assertEquals(Byte.valueOf((byte)12), b);
    }

    @Test
    public void testStringToShort() {
        Short s = Converter.convert("321", Short.class);
        assertEquals(Short.valueOf((short)321), s);
    }

    @Test
    public void testStringToLong() {
        Long l = Converter.convert("9876543210", Long.class);
        assertEquals(Long.valueOf(9876543210L), l);
    }

    @Test
    public void testStringToFloat() {
        Float f = Converter.convert("3.14", Float.class);
        assertEquals(Float.valueOf(3.14f), f);
    }

    @Test
    public void testStringToDouble() {
        Double d = Converter.convert("2.71828", Double.class);
        assertEquals(Double.valueOf(2.71828), d);
    }

    /* ---------------------------------------------------------------------- */
    /*  BigDecimal / BigInteger                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testStringToBigDecimal() {
        BigDecimal bd = Converter.convert("12345.6789", BigDecimal.class);
        assertEquals(new BigDecimal("12345.6789"), bd);
    }

    @Test
    public void testEmptyStringToBigDecimal_Convert2() {
        // convert2BigDecimal treats empty string as zero
        BigDecimal bd = Converter.convert2BigDecimal("");
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test
    public void testStringToBigInteger() {
        BigInteger bi = Converter.convert("98765432109876543210", BigInteger.class);
        assertEquals(new BigInteger("98765432109876543210"), bi);
    }

    @Test
    public void testEmptyStringToBigInteger_Convert2() {
        // convert2BigInteger treats empty string as zero
        BigInteger bi = Converter.convert2BigInteger("");
        assertEquals(BigInteger.ZERO, bi);
    }

    /* ---------------------------------------------------------------------- */
    /*  Date / Time conversions                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDateToTimestamp() {
        Date now = new Date();
        Timestamp ts = Converter.convertToTimestamp(now);
        assertEquals(now.getTime(), ts.getTime());
    }

    @Test
    public void testTimestampToDate() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Date d = Converter.convert(ts, Date.class);
        assertEquals(ts.getTime(), d.getTime());
    }

    @Test
    public void testDateToSqlDate() {
        Date now = new Date();
        java.sql.Date sqlDate = Converter.convertToSqlDate(now);
        assertEquals(now.getTime(), sqlDate.getTime());
    }

    @Test
    public void testStringToLocalDate() {
        LocalDate expected = LocalDate.of(2024, 4, 27);
        LocalDate actual = Converter.convert("2024-04-27", LocalDate.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testStringToLocalDateTime() {
        LocalDateTime expected = LocalDateTime.of(2024, 4, 27, 15, 30, 45);
        LocalDateTime actual = Converter.convert("2024-04-27T15:30:45", LocalDateTime.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testStringToZonedDateTime() {
        ZonedDateTime expected = ZonedDateTime.parse("2024-04-27T12:00:00Z");
        ZonedDateTime actual = Converter.convert("2024-04-27T12:00:00Z", ZonedDateTime.class);
        assertEquals(expected, actual);
    }

    @Test
    public void testLocalDateToMillis_DeprecatedHelper() {
        LocalDate ld = LocalDate.of(1970, 1, 2);
        long expected = Converter.convert(ld, long.class);
        long actual = Converter.localDateToMillis(ld);
        assertEquals(expected, actual);
    }

    @Test
    public void testLocalDateTimeToMillis_DeprecatedHelper() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 1);
        long expected = Converter.convert(ldt, long.class);
        long actual = Converter.localDateTimeToMillis(ldt);
        assertEquals(expected, actual);
    }

    @Test
    public void testZonedDateTimeToMillis_DeprecatedHelper() {
        ZonedDateTime zdt = ZonedDateTime.parse("1970-01-01T00:00:01Z");
        long expected = Converter.convert(zdt, long.class);
        long actual = Converter.zonedDateTimeToMillis(zdt);
        assertEquals(expected, actual);
    }

    @Test
    public void testDateToCalendar() {
        Date now = new Date();
        Calendar cal = Converter.convertToCalendar(now);
        assertNotNull(cal);
        assertEquals(now.getTime(), cal.getTimeInMillis());
    }

    /* ---------------------------------------------------------------------- */
    /*  Atomic types                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testStringToAtomicInteger() {
        AtomicInteger ai = Converter.convert("42", AtomicInteger.class);
        assertEquals(42, ai.get());
    }

    @Test
    public void testAtomicIntegerToInteger() {
        AtomicInteger ai = new AtomicInteger(77);
        Integer i = Converter.convert(ai, Integer.class);
        assertEquals(Integer.valueOf(77), i);
    }

    @Test
    public void testStringToAtomicLong() {
        AtomicLong al = Converter.convert("123456789", AtomicLong.class);
        assertEquals(123456789L, al.get());
    }

    @Test
    public void testStringToAtomicBoolean() {
        AtomicBoolean ab = Converter.convert("true", AtomicBoolean.class);
        assertTrue(ab.get());
        AtomicBoolean abFalse = Converter.convert("false", AtomicBoolean.class);
        assertFalse(abFalse.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  Support‑query APIs                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsConversionSupportedFor_KnownPair() {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
    }

    @Test
    public void testIsConversionSupportedFor_UnknownPair() {
        assertFalse(Converter.isConversionSupportedFor(String.class, Thread.class));
    }

    @Test
    public void testIsDirectConversionSupported_KnownPair() {
        assertTrue(Converter.isDirectConversionSupported(String.class, Integer.class));
    }

    @Test
    public void testIsDirectConversionSupported_UnknownPair() {
        assertFalse(Converter.isDirectConversionSupported(String.class, Thread.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported_KnownPair() {
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Integer.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported_CollectionPair() {
        assertFalse(Converter.isSimpleTypeConversionSupported(java.util.List.class, java.util.Set.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Retrieval of supported conversion maps                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAllSupportedConversions_NotEmpty() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map);
        // The map should contain at least one known source type (e.g., String)
        assertTrue(map.containsKey(String.class));
    }

    @Test
    public void testGetSupportedConversions_NotEmpty() {
        Map<String, Set<String>> map = Converter.getSupportedConversions();
        assertNotNull(map);
        assertTrue(map.containsKey("java.lang.String"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Custom conversion registration                                         */
    /* ---------------------------------------------------------------------- */

    /** Simple POJO used in the custom‑conversion test. */
    private static final class MyCustom {
        private final String payload;

        MyCustom(String payload) { this.payload = payload; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MyCustom)) return false;
            MyCustom that = (MyCustom) o;
            return payload.equals(that.payload);
        }

        @Override
        public int hashCode() {
            return payload.hashCode();
        }

        @Override
        public String toString() {
            return "MyCustom{" + payload + '}';
        }
    }

    @Test
    public void testAddConversion_AndConvert() {
        // Register a conversion from String to MyCustom
        Converter.addConversion(String.class, MyCustom.class,
                (from, conv) -> new MyCustom((String) from));

        // Verify that the conversion is now reported as supported
        assertTrue(Converter.isConversionSupportedFor(String.class, MyCustom.class));

        // Perform the conversion and validate the result
        MyCustom expected = new MyCustom("hello world");
        MyCustom actual = Converter.convert("hello world", MyCustom.class);
        assertEquals(expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  Additional sanity checks (identity conversion, primitive wrappers)    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIdentityConversion_SameType() {
        LocalDate now = LocalDate.now();
        LocalDate result = Converter.convert(now, LocalDate.class);
        // The converter should return the original instance (or an equal one)
        assertEquals(now, result);
    }

    @Test
    public void testPrimitiveWrapperConversion() {
        // Convert primitive int to Integer wrapper
        Integer boxed = Converter.convert(5, Integer.class);
        assertEquals(Integer.valueOf(5), boxed);
    }

    @Test
    public void testUuidConversionFromString() {
        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();
        UUID result = Converter.convert(uuidStr, UUID.class);
        assertEquals(uuid, result);
    }

    @Test
    public void testMapToUuidConversion() {
        UUID uuid = UUID.randomUUID();
        Map<String, Object> map = Map.of(
                "mostSigBits", uuid.getMostSignificantBits(),
                "leastSigBits", uuid.getLeastSignificantBits()
        );
        UUID result = Converter.convert(map, UUID.class);
        assertEquals(uuid, result);
    }
}
