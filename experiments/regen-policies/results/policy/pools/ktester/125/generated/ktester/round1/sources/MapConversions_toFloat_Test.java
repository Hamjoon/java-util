package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class MapConversions_toFloat_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance – use the default options
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> successfulConversionProvider() {
        // Helper converters for the two custom‑converter scenarios
        Converter customStringConverter = mock(Converter.class);
        when(customStringConverter.convert(eq("custom"), eq(Float.class))).thenReturn(9.9f);
        Converter exceptionThrowingConverter = mock(Converter.class);
        when(exceptionThrowingConverter.convert(eq("custom"), eq(Float.class))).thenThrow(new IllegalStateException("forced"));
        return Stream.of(// null source
        Arguments.of(null, null, null), // Direct Float
        Arguments.of(Float.valueOf(12.34f), 12.34f, null), // Double → Float (precision loss is acceptable)
        Arguments.of(Double.valueOf(56.78), 56.78f, null), // Numeric String
        Arguments.of("99.99", 99.99f, null), // BigDecimal
        Arguments.of(new BigDecimal("123.45"), 123.45f, null), // BigInteger
        Arguments.of(new BigInteger("6789"), 6789.0f, null), // Integer
        Arguments.of(Integer.valueOf(42), 42.0f, null), // Long (scientific notation acceptable)
        Arguments.of(Long.valueOf(123456789L), 1.2345679E8f, null), // Boolean TRUE → 1.0f
        Arguments.of(Boolean.TRUE, 1.0f, null), // Boolean FALSE → 0.0f
        Arguments.of(Boolean.FALSE, 0.0f, null), // AtomicInteger
        Arguments.of(new AtomicInteger(7), 7.0f, null), // AtomicLong
        Arguments.of(new AtomicLong(123456L), 1.23456E5f, null), // AtomicBoolean TRUE
        Arguments.of(new AtomicBoolean(true), 1.0f, null), // Date (epoch millis)
        Arguments.of(new Date(0L), 0.0f, null), // Timestamp (epoch millis)
        Arguments.of(new Timestamp(1_600_000_000_000L), 1.6E12f, null), // Object with overridden toString()
        Arguments.of(new Object() {

            @Override
            public String toString() {
                return "3.14";
            }
        }, 3.14f, null), // Double.NaN
        Arguments.of(Double.NaN, Float.NaN, null), // Double.POSITIVE_INFINITY
        Arguments.of(Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, null), // Double.NEGATIVE_INFINITY
        Arguments.of(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, null), // String "NaN"
        Arguments.of("NaN", Float.NaN, null), // String "Infinity"
        Arguments.of("Infinity", Float.POSITIVE_INFINITY, null), // String "-Infinity"
        Arguments.of("-Infinity", Float.NEGATIVE_INFINITY, null), // Short
        Arguments.of(Short.valueOf((short) 12), 12.0f, null), // Byte
        Arguments.of(Byte.valueOf((byte) 7), 7.0f, null), // Map with key "value"
        Arguments.of(Collections.singletonMap("value", "123.45"), 123.45f, null), // Map with key "_v"
        Arguments.of(Collections.singletonMap("_v", "67.89"), 67.89f, null), // Map entry "value" with null → result null
        Arguments.of(Collections.singletonMap("value", null), null, null), // Map entry "value" Boolean TRUE → 1.0f
        Arguments.of(Collections.singletonMap("value", Boolean.TRUE), 1.0f, null), // Custom converter mapping "custom" → 9.9f
        Arguments.of(Collections.singletonMap("value", "custom"), 9.9f, customStringConverter), // BigDecimal overflow → +Infinity
        Arguments.of(new BigDecimal("1e40"), Float.POSITIVE_INFINITY, null), // BigDecimal negative overflow → -Infinity
        Arguments.of(new BigDecimal("-1e40"), Float.NEGATIVE_INFINITY, null));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        // Mock converter that throws IllegalStateException for the "custom" string
        Converter exceptionThrowingConverter = mock(Converter.class);
        when(exceptionThrowingConverter.convert(eq("custom"), eq(Float.class))).thenThrow(new IllegalStateException("forced"));
        return Stream.of(// Empty string
        Arguments.of("", NumberFormatException.class, null), // Whitespace only string
        Arguments.of("   ", NumberFormatException.class, null), // Map with unsupported object as value ("value")
        Arguments.of(Collections.singletonMap("value", new Object()), ClassCastException.class, null), // Map with unsupported object as value ("_v")
        Arguments.of(Collections.singletonMap("_v", new Object()), ClassCastException.class, null), // List (unsupported source type)
        Arguments.of(Arrays.asList(1, 2, 3), ClassCastException.class, null), // Primitive int array (unsupported)
        Arguments.of(new int[] { 1, 2, 3 }, ClassCastException.class, null), // StringBuilder (unsupported)
        Arguments.of(new StringBuilder("123.45"), ClassCastException.class, null), // Custom converter that throws IllegalStateException
        Arguments.of("custom", IllegalStateException.class, exceptionThrowingConverter));
    }

    @Test
    @DisplayName("Convert a map with key \"_v\" containing a numeric string to Float")
    void toFloatFromMapString() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "12.34");
        Float result = MapConversions.toFloat(map, converter);
        assertNotNull(result);
        assertEquals(12.34f, result);
    }

    @Test
    @DisplayName("Pass a Float instance directly – should be returned unchanged")
    void toFloatFromFloatInstance() {
        Float input = 5.5f;
        Float result = MapConversions.toFloat(input, converter);
        assertNotNull(result);
        assertEquals(input, result);
    }

    @Test
    @DisplayName("Pass null – should return null")
    void toFloatFromNull() {
        Float result = MapConversions.toFloat(null, converter);
        assertNull(result);
    }

    @Test
    @DisplayName("Convert a map with key \"value\" containing a numeric string to Float")
    void toFloatFromMapValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "7.89");
        Float result = MapConversions.toFloat(map, converter);
        assertNotNull(result);
        assertEquals(7.89f, result);
    }

    @Test
    @DisplayName("Convert a map with a numeric value (not a string) to Float")
    void toFloatFromMapNumeric() {
        Map<String, Object> map = new HashMap<>();
        // Integer
        map.put("_v", 42);
        Float result = MapConversions.toFloat(map, converter);
        assertNotNull(result);
        assertEquals(42.0f, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} (converter={2}) => expected={1}")
    @MethodSource("successfulConversionProvider")
    @DisplayName("Successful conversions to Float")
    void testSuccessfulConversions(Object from, Object expected, Converter customConverter) {
        Float result = MapConversions.toFloat(from, customConverter);
        if (expected == null) {
            assertNull(result);
        } else if (expected instanceof Float) {
            Float exp = (Float) expected;
            if (exp.isNaN()) {
                assertTrue(result.isNaN(), "Result should be NaN");
            } else if (exp.isInfinite()) {
                assertEquals(exp, result, "Result should be the expected infinity");
            } else {
                assertEquals(exp, result);
            }
        } else {
            fail("Unexpected expected value type: " + expected);
        }
    }

    @ParameterizedTest(name = "[{index}] from={0} (converter={2}) => throws {1}")
    @MethodSource("exceptionConversionProvider")
    @DisplayName("Conversions that should throw exceptions")
    void testExceptionConversions(Object from, Class<? extends Throwable> expectedException, Converter customConverter) {
        assertThrows(expectedException, () -> MapConversions.toFloat(from, customConverter));
    }
}
