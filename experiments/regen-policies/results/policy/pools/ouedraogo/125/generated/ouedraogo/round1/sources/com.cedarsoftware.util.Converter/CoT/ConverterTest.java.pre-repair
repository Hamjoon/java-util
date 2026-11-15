package com.cedarsoftware.util;

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

import com.cedarsoftware.util.convert.Convert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit 4 test suite for {@link Converter}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public static methods (basic functionality)</li>
 *   <li>Edge‑case handling (null values, primitive defaults, unsupported conversions)</li>
 *   <li>Custom conversion registration via {@code addConversion}</li>
 *   <li>Conversion‑support query methods</li>
 * </ul>
 */
public class ConverterTest {

    /** Simple POJO used for a custom conversion test. */
    public static final class CustomType {
        private final String value;

        public CustomType(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CustomType)) return false;
            CustomType that = (CustomType) o;
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "CustomType{" + "value='" + value + '\'' + '}';
        }
    }

    /** Clean‑up any custom conversion added during a test. */
    @After
    public void cleanUpCustomConversions() {
        // The library does not expose a remove method, but adding a null conversion
        // overwrites the previous one with a no‑op that throws IllegalArgumentException.
        // This ensures subsequent tests are not polluted.
        Converter.addConversion(String.class, CustomType.class,
                (from, conv) -> { throw new IllegalArgumentException("Removed"); });
    }

    // -----------------------------------------------------------------------
    // 1.  Basic conversion tests (convert, primitive defaults, wrappers, etc.)
    // -----------------------------------------------------------------------

    @Test
    public void testConvertStringToInteger() {
        Integer result = Converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testConvertNullToObjectReturnsNull() {
        assertNull(Converter.convert(null, String.class));
    }

    @Test
    public void testConvertNullToPrimitiveIntReturnsZero() {
        int result = Converter.convert(null, int.class);
        assertEquals(0, result);
    }

    @Test
    public void testConvertNullToPrimitiveBooleanReturnsFalse() {
        boolean result = Converter.convert(null, boolean.class);
        assertFalse(result);
    }

    @Test
    public void testConvertNullToPrimitiveCharReturnsZeroChar() {
        char result = Converter.convert(null, char.class);
        assertEquals('\u0000', result);
    }

    @Test
    public void testConvertDateToString() {
        Date now = new Date();
        String s = Converter.convert(now, String.class);
        assertNotNull(s);
        assertTrue(s.contains(String.valueOf(now.getTime())));
    }

    @Test
    public void testConvertStringToLocalDate() {
        LocalDate ld = Converter.convert("2024-01-15", LocalDate.class);
        assertEquals(LocalDate.of(2024, 1, 15), ld);
    }

    @Test
    public void testConvertStringToZonedDateTime() {
        ZonedDateTime zdt = Converter.convert("2024-01-15T10:15:30+01:00[Europe/Paris]",
                ZonedDateTime.class);
        assertEquals(ZonedDateTime.parse("2024-01-15T10:15:30+01:00[Europe/Paris]"), zdt);
    }

    // -----------------------------------------------------------------------
    // 2.  Helper “convert2*” methods – null handling & default values
    // -----------------------------------------------------------------------

    @Test
    public void testConvert2String_NullReturnsEmptyString() {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvert2String_NonNull() {
        assertEquals("hello", Converter.convert2String("hello"));
    }

    @Test
    public void testConvert2BigDecimal_NullReturnsZero() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
    }

    @Test
    public void testConvert2BigDecimal_NonNull() {
        BigDecimal bd = Converter.convert2BigDecimal("12.34");
        assertEquals(new BigDecimal("12.34"), bd);
    }

    @Test
    public void testConvert2char_NullReturnsZeroChar() {
        assertEquals('\u0000', Converter.convert2char(null));
    }

    @Test
    public void testConvert2char_NonNull() {
        assertEquals('A', Converter.convert2char("A"));
    }

    @Test
    public void testConvert2int_NullReturnsZero() {
        assertEquals(0, Converter.convert2int(null));
    }

    @Test
    public void testConvert2int_NonNull() {
        assertEquals(42, Converter.convert2int("42"));
    }

    @Test
    public void testConvert2boolean_NullReturnsFalse() {
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testConvert2boolean_NonNull() {
        assertTrue(Converter.convert2boolean("true"));
    }

    @Test
    public void testConvert2AtomicInteger_NullReturnsZeroAtomic() {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        assertNotNull(ai);
        assertEquals(0, ai.get());
    }

    @Test
    public void testConvert2AtomicLong_NullReturnsZeroAtomic() {
        AtomicLong al = Converter.convert2AtomicLong(null);
        assertNotNull(al);
        assertEquals(0L, al.get());
    }

    @Test
    public void testConvert2AtomicBoolean_NullReturnsFalseAtomic() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    // -----------------------------------------------------------------------
    // 3.  Edge‑case / exception tests
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testConvert_NullTargetClassThrowsException() {
        Converter.convert("123", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvert_UnsupportedConversionThrowsException() {
        // Dummy source type that has no built‑in conversion to Integer.
        class Dummy {}
        Converter.convert(new Dummy(), Integer.class);
    }

    @Test
    public void testAddConversionAndUseCustomConverter() {
        // Register a custom conversion from String to CustomType
        Convert<CustomType> conversion = (from, conv) -> new CustomType((String) from);
        Convert<?> previous = Converter.addConversion(String.class, CustomType.class, conversion);
        assertNull(previous); // no previous conversion

        // Verify conversion works
        CustomType ct = Converter.convert("myValue", CustomType.class);
        assertEquals(new CustomType("myValue"), ct);
    }

    @Test
    public void testAddConversionOverridesExisting() {
        // First conversion
        Convert<CustomType> first = (from, conv) -> new CustomType("first");
        Converter.addConversion(String.class, CustomType.class, first);

        // Override with second conversion
        Convert<CustomType> second = (from, conv) -> new CustomType("second");
        Convert<?> previous = Converter.addConversion(String.class, CustomType.class, second);
        assertNotNull(previous); // previous conversion should be returned

        // New conversion should be used
        CustomType ct = Converter.convert("ignored", CustomType.class);
        assertEquals(new CustomType("second"), ct);
    }

    // -----------------------------------------------------------------------
    // 4.  Conversion‑support query methods
    // -----------------------------------------------------------------------

    @Test
    public void testIsConversionSupportedFor_KnownSupported() {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
    }

    @Test
    public void testIsConversionSupportedFor_KnownUnsupported() {
        assertFalse(Converter.isConversionSupportedFor(String.class, Map.class));
    }

    @Test
    public void testIsDirectConversionSupported_KnownSupported() {
        assertTrue(Converter.isDirectConversionSupported(String.class, Integer.class));
    }

    @Test
    public void testIsDirectConversionSupported_KnownUnsupported() {
        assertFalse(Converter.isDirectConversionSupported(String.class, Map.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported_PrimitiveAndWrapper() {
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Integer.class, Integer.class));
    }

    @Test
    public void testIsSimpleTypeConversionSupported_CollectionsReturnsFalse() {
        assertFalse(Converter.isSimpleTypeConversionSupported(java.util.List.class,
                java.util.Set.class));
    }

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

    // -----------------------------------------------------------------------
    // 5.  Deprecated epoch‑millis helpers – they delegate to the main convert()
    // -----------------------------------------------------------------------

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
        ZonedDateTime zdt = ZonedDateTime.parse("1970-01-01T00:00:01Z[UTC]");
        long millis = Converter.zonedDateTimeToMillis(zdt);
        assertEquals(1000L, millis);
    }
}
