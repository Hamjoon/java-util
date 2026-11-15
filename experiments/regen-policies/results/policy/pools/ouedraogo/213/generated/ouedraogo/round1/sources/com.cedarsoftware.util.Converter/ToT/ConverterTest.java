package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.Convert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Converter}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic primitive / wrapper conversions (including default values for {@code null}).</li>
 *   <li>String‑to‑String handling (null → empty string vs. null).</li>
 *   <li>BigDecimal / BigInteger convenience methods.</li>
 *   <li>Atomic types default handling.</li>
 *   <li>Supported‑conversion queries.</li>
 *   <li>Custom conversion registration via {@link Converter#addConversion(Class, Class, Convert)}.</li>
 *   <li>Edge‑case & error scenarios (unsupported conversion, null target class, etc.).</li>
 * </ul>
 */
public class ConverterTest {

    /*-------------------------------------------------------------
     *  Helper class used for custom‑conversion tests
     *------------------------------------------------------------*/
    public static final class CustomType {
        private final String value;

        public CustomType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CustomType)) return false;
            CustomType that = (CustomType) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "CustomType{" + value + '}';
        }
    }

    /*-------------------------------------------------------------
     *  1.  Core {@code convert(Object,Class<T>)} tests
     *------------------------------------------------------------*/

    @Test
    public void testStringToIntegerConversion() {
        Integer result = Converter.convert("42", Integer.class);
        assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testStringToBooleanConversion() {
        Boolean resultTrue = Converter.convert("true", Boolean.class);
        Boolean resultFalse = Converter.convert("false", Boolean.class);
        assertTrue(resultTrue);
        assertFalse(resultFalse);
    }

    @Test
    public void testStringToLocalDateConversion() {
        // ISO‑8601 date string is universally supported
        LocalDate date = Converter.convert("2024-01-15", LocalDate.class);
        assertEquals(LocalDate.of(2024, 1, 15), date);
    }

    @Test
    public void testStringToLocalDateTimeConversion() {
        LocalDateTime ldt = Converter.convert("2024-01-15T12:30:45", LocalDateTime.class);
        assertEquals(LocalDateTime.of(2024, 1, 15, 12, 30, 45), ldt);
    }

    @Test
    public void testStringToZonedDateTimeConversion() {
        ZonedDateTime zdt = Converter.convert("2024-01-15T12:30:45+01:00[Europe/Paris]",
                                             ZonedDateTime.class);
        assertEquals(ZonedDateTime.parse("2024-01-15T12:30:45+01:00[Europe/Paris]"), zdt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertNullTargetClassThrows() {
        // The API explicitly forbids a null target class
        Converter.convert("anything", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedConversionThrows() {
        // There is no built‑in conversion from a Random object to Integer
        Converter.convert(new Random(), Integer.class);
    }

    /*-------------------------------------------------------------
     *  2.  Primitive‑default handling (null source)
     *------------------------------------------------------------*/

    @Test
    public void testPrimitiveDefaultsWhenSourceIsNull() {
        assertEquals((char) 0, Converter.convert2char(null));
        assertEquals((byte) 0, Converter.convert2byte(null));
        assertEquals((short) 0, Converter.convert2short(null));
        assertEquals(0, Converter.convert2int(null));
        assertEquals(CommonValues.LONG_ZERO.longValue(), Converter.convert2long(null));
        assertEquals(CommonValues.FLOAT_ZERO.floatValue(), Converter.convert2float(null), 0.0f);
        assertEquals(CommonValues.DOUBLE_ZERO.doubleValue(), Converter.convert2double(null), 0.0);
        assertFalse(Converter.convert2boolean(null));
    }

    @Test
    public void testWrapperDefaultsWhenSourceIsNull() {
        assertNull(Converter.convertToCharacter(null));
        assertNull(Converter.convertToByte(null));
        assertNull(Converter.convertToShort(null));
        assertNull(Converter.convertToInteger(null));
        assertNull(Converter.convertToLong(null));
        assertNull(Converter.convertToFloat(null));
        assertNull(Converter.convertToDouble(null));
        assertNull(Converter.convertToBoolean(null));
    }

    /*-------------------------------------------------------------
     *  3.  String convenience methods
     *------------------------------------------------------------*/

    @Test
    public void testConvert2StringHandlesNull() {
        assertEquals("", Converter.convert2String(null));
    }

    @Test
    public void testConvertToStringHandlesNull() {
        assertNull(Converter.convertToString(null));
    }

    @Test
    public void testConvert2StringWithNonNull() {
        assertEquals("hello", Converter.convert2String("hello"));
        // also works for non‑String objects (toString is used internally)
        assertEquals("123", Converter.convert2String(123));
    }

    /*-------------------------------------------------------------
     *  4.  BigDecimal / BigInteger convenience methods
     *------------------------------------------------------------*/

    @Test
    public void testConvert2BigDecimalNullReturnsZero() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null));
    }

    @Test
    public void testConvertToBigDecimalNullReturnsNull() {
        assertNull(Converter.convertToBigDecimal(null));
    }

    @Test
    public void testConvert2BigDecimalFromString() {
        BigDecimal bd = Converter.convert2BigDecimal("1234.56");
        assertEquals(new BigDecimal("1234.56"), bd);
    }

    @Test
    public void testConvert2BigIntegerNullReturnsZero() {
        assertEquals(BigInteger.ZERO, Converter.convert2BigInteger(null));
    }

    @Test
    public void testConvertToBigIntegerNullReturnsNull() {
        assertNull(Converter.convertToBigInteger(null));
    }

    @Test
    public void testConvert2BigIntegerFromString() {
        BigInteger bi = Converter.convert2BigInteger("9876543210");
        assertEquals(new BigInteger("9876543210"), bi);
    }

    /*-------------------------------------------------------------
     *  5.  Atomic type convenience methods
     *------------------------------------------------------------*/

    @Test
    public void testConvert2AtomicIntegerNullReturnsZeroInstance() {
        AtomicInteger ai = Converter.convert2AtomicInteger(null);
        assertNotNull(ai);
        assertEquals(0, ai.get());
    }

    @Test
    public void testConvert2AtomicLongNullReturnsZeroInstance() {
        AtomicLong al = Converter.convert2AtomicLong(null);
        assertNotNull(al);
        assertEquals(0L, al.get());
    }

    @Test
    public void testConvert2AtomicBooleanNullReturnsFalseInstance() {
        AtomicBoolean ab = Converter.convert2AtomicBoolean(null);
        assertNotNull(ab);
        assertFalse(ab.get());
    }

    @Test
    public void testAtomicConversionsFromString() {
        AtomicInteger ai = Converter.convertToAtomicInteger("7");
        assertEquals(7, ai.get());

        AtomicLong al = Converter.convertToAtomicLong("1234567890123");
        assertEquals(1234567890123L, al.get());

        AtomicBoolean abTrue = Converter.convertToAtomicBoolean("true");
        AtomicBoolean abFalse = Converter.convertToAtomicBoolean("false");
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    /*-------------------------------------------------------------
     *  6.  Queries about supported conversions
     *------------------------------------------------------------*/

    @Test
    public void testIsSimpleTypeConversionSupported() {
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, Integer.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Integer.class, String.class));
        // Collections are not considered "simple"
        assertFalse(Converter.isSimpleTypeConversionSupported(List.class, Set.class));
        // Array types are not considered "simple"
        assertFalse(Converter.isSimpleTypeConversionSupported(String[].class, Integer[].class));
        // Identity conversion is always supported
        assertTrue(Converter.isSimpleTypeConversionSupported(LocalDate.class, LocalDate.class));
    }

    @Test
    public void testIsConversionSupportedForSimpleAndArray() {
        // Simple conversion
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));

        // Array‑to‑array conversion (String[] → Integer[]) should be supported because
        // both the container conversion and the component conversion exist.
        assertTrue(Converter.isConversionSupportedFor(String[].class, Integer[].class));

        // Incompatible conversion
        assertFalse(Converter.isConversionSupportedFor(UUID.class, Calendar.class));
    }

    @Test
    public void testAllSupportedConversionsIsNotEmpty() {
        Map<Class<?>, Set<Class<?>>> map = Converter.allSupportedConversions();
        assertNotNull(map);
        assertFalse(map.isEmpty());

        // Spot‑check that a well‑known conversion exists (String → Integer)
        Set<Class<?>> targets = map.get(String.class);
        assertNotNull(targets);
        assertTrue(targets.contains(Integer.class));
    }

    @Test
    public void testGetSupportedConversionsNamesMap() {
        Map<String, Set<String>> nameMap = Converter.getSupportedConversions();
        assertNotNull(nameMap);
        assertFalse(nameMap.isEmpty());

        Set<String> targets = nameMap.get("java.lang.String");
        assertNotNull(targets);
        assertTrue(targets.contains("java.lang.Integer"));
    }

    /*-------------------------------------------------------------
     *  7.  Custom conversion registration
     *------------------------------------------------------------*/

    @Test
    public void testAddCustomConversionAndUseIt() {
        // Register a conversion from String → CustomType
        Convert<CustomType> conversion = (from, conv) -> new CustomType((String) from);
        Convert<?> previous = Converter.addConversion(String.class, CustomType.class, conversion);
        assertNull("No prior conversion should exist for this pair", previous);

        // Verify that the custom conversion works
        CustomType ct = Converter.convert("myValue", CustomType.class);
        assertEquals(new CustomType("myValue"), ct);
    }

    @Test
    public void testAddConversionOverridesExisting() {
        // First, ensure there is a built‑in conversion from String → Integer
        Integer before = Converter.convert("99", Integer.class);
        assertEquals(Integer.valueOf(99), before);

        // Override it with a custom conversion that always returns 42
        Convert<Integer> override = (from, conv) -> 42;
        Convert<?> previous = Converter.addConversion(String.class, Integer.class, override);
        assertNotNull("There should be a previous conversion to override", previous);

        Integer after = Converter.convert("anyString", Integer.class);
        assertEquals(Integer.valueOf(42), after);
    }

    /*-------------------------------------------------------------
     *  8.  Deprecated helper methods – they delegate to {@code convert}
     *------------------------------------------------------------*/

    @Test
    public void testDeprecatedLocalDateToMillis() {
        LocalDate ld = LocalDate.of(1970, 1, 2); // one day after epoch
        long expected = ld.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        long actual = Converter.localDateToMillis(ld);
        assertEquals(expected, actual);
    }

    @Test
    public void testDeprecatedLocalDateTimeToMillis() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 12, 0, 0);
        long expected = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long actual = Converter.localDateTimeToMillis(ldt);
        assertEquals(expected, actual);
    }

    @Test
    public void testDeprecatedZonedDateTimeToMillis() {
        ZonedDateTime zdt = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0,
                java.time.ZoneId.of("UTC"));
        long expected = 0L;
        long actual = Converter.zonedDateTimeToMillis(zdt);
        assertEquals(expected, actual);
    }
}
