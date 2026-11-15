package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toInt_Test {

    /**
     * The Converter used for all tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – they are sufficient for the built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    // ---------- Helper for invoking the method under test ----------
    private static Integer invokeToInt(Object from, Converter conv) {
        return MapConversions.toInt(from, conv);
    }

    // ---------- Direct Integer Types ----------
    private static Stream<Arguments> directIntegerTypes() {
        return Stream.of(// Integer → same value
        Arguments.of(Integer.valueOf(123), 123, null, "already Integer"), // Long within range
        Arguments.of(Long.valueOf(123L), 123, null, "Long fitting into int"), // Short → widened
        Arguments.of(Short.valueOf((short) 12), 12, null, "Short widened"), // Byte → widened
        Arguments.of(Byte.valueOf((byte) 7), 7, null, "Byte widened"));
    }

    // ---------- String Numeric Conversion ----------
    private static Stream<Arguments> stringNumericConversion() {
        return Stream.of(Arguments.of("456", 456, null, "plain numeric string"), Arguments.of("   789   ", 789, null, "string with surrounding whitespace"), Arguments.of("-42", -42, null, "negative numeric string"));
    }

    // ---------- Map Key Extraction ----------
    private static Stream<Arguments> mapKeyExtraction() {
        return Stream.of(// recognized "value" key with Integer
        Arguments.of(Map.of("value", 55), 55, null, "value key with Integer"), // recognized "_v" key with numeric String
        Arguments.of(Map.of("_v", "66"), 66, null, "_v key with String"), // un‑underscored "v" key with Long (should be recognized as generic “value”? actually not, expecting 77)
        Arguments.of(Map.of("v", 77L), 77, null, "v key with Long"), // map without any recognized key → IllegalArgumentException
        Arguments.of(Map.of("NUMBER", 88), null, IllegalArgumentException.class, "no recognized key"));
    }

    // ---------- Null and Invalid Inputs ----------
    private static Stream<Arguments> nullAndInvalidInputs() {
        return Stream.of(Arguments.of(null, null, null, "null source"), Arguments.of("notANumber", null, NumberFormatException.class, "non‑numeric string"), Arguments.of(new Object(), null, IllegalArgumentException.class, "unsupported object"));
    }

    // ---------- Converter Delegation ----------
    private static Stream<Arguments> converterDelegation() {
        return Stream.of(// custom converter that returns 999 for any String
        Arguments.of("123", 999, null, "custom converter overrides string parsing"), // custom converter that returns 111 for key "value"
        Arguments.of(Map.of("value", "321"), 111, null, "custom converter for map value"));
    }

    // ---------- Other Number Types ----------
    private static Stream<Arguments> otherNumberTypes() {
        return Stream.of(Arguments.of(BigInteger.valueOf(12345L), 12345, null, "BigInteger within range"), Arguments.of(BigDecimal.valueOf(6789), 6789, null, "BigDecimal without fraction"), Arguments.of(Double.valueOf(42.0), 42, null, "Double whole number"), Arguments.of(Float.valueOf(3.14f), null, NumberFormatException.class, "Float with fraction"));
    }

    // ---------- Overflow and Boundary ----------
    private static Stream<Arguments> overflowAndBoundary() {
        return Stream.of(Arguments.of(Long.valueOf((long) Integer.MAX_VALUE + 1L), null, IllegalArgumentException.class, "Long > MAX_VALUE"), Arguments.of(Long.valueOf((long) Integer.MIN_VALUE - 1L), null, IllegalArgumentException.class, "Long < MIN_VALUE"), Arguments.of(Integer.valueOf(Integer.MAX_VALUE), Integer.MAX_VALUE, null, "MAX_VALUE"), Arguments.of(Integer.valueOf(Integer.MIN_VALUE), Integer.MIN_VALUE, null, "MIN_VALUE"));
    }

    // ---------- String Edge Cases ----------
    private static Stream<Arguments> stringEdgeCases() {
        return Stream.of(Arguments.of("+123", 123, null, "plus sign"), Arguments.of("0x10", null, NumberFormatException.class, "hex string not supported"), Arguments.of("", null, NumberFormatException.class, "empty string"), Arguments.of("  \t\n  ", null, NumberFormatException.class, "whitespace only"));
    }

    // ---------- Case Sensitivity in Map Keys ----------
    private static Stream<Arguments> caseSensitivityInMapKeys() {
        return Stream.of(Arguments.of(Map.of("VALUE", 99), null, IllegalArgumentException.class, "upper‑case VALUE"), Arguments.of(Map.of("v", 88), null, IllegalArgumentException.class, "lower‑case v"), Arguments.of(Map.of("_V", "77"), null, IllegalArgumentException.class, "upper‑case _V"));
    }

    // ---------- Converter Returning Null ----------
    private static Stream<Arguments> converterReturningNull() {
        return Stream.of(Arguments.of("123", null, null, "converter returns null for String"), Arguments.of(Map.of("value", "456"), null, null, "converter returns null for map value"));
    }

    // ---------- Unsupported Map Value Types ----------
    private static Stream<Arguments> unsupportedMapValueTypes() {
        return Stream.of(Arguments.of(Map.of("value", new Object()), null, IllegalArgumentException.class, "Object as map value"), Arguments.of(Map.of("_v", Boolean.TRUE), null, IllegalArgumentException.class, "Boolean as map value"));
    }

    // ---------- Map Value Parsing Errors ----------
    private static Stream<Arguments> mapValueParsingErrors() {
        return Stream.of(Arguments.of(Map.of("value", "abc"), null, NumberFormatException.class, "non‑numeric string value"), Arguments.of(Map.of("_v", "12a"), null, NumberFormatException.class, "string with trailing chars"), Arguments.of(Map.of("value", null), null, IllegalArgumentException.class, "null map value"));
    }

    // ---------- Numeric Type Overflow ----------
    private static Stream<Arguments> numericTypeOverflow() {
        return Stream.of(Arguments.of(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE), null, IllegalArgumentException.class, "BigInteger > MAX_VALUE"), Arguments.of(BigInteger.valueOf(Integer.MIN_VALUE).subtract(BigInteger.ONE), null, IllegalArgumentException.class, "BigInteger < MIN_VALUE"), Arguments.of(BigDecimal.valueOf(12345.67), null, NumberFormatException.class, "BigDecimal with fraction"));
    }

    // ---------- Unsupported Types ----------
    private static Stream<Arguments> unsupportedTypes() {
        return Stream.of(Arguments.of(new AtomicInteger(10), null, IllegalArgumentException.class, "AtomicInteger unsupported"), Arguments.of(new Date(), null, IllegalArgumentException.class, "Date unsupported"), Arguments.of(List.of(1, 2, 3), null, IllegalArgumentException.class, "List unsupported"));
    }

    // ---------- Invalid String Formats ----------
    private static Stream<Arguments> invalidStringFormats() {
        return Stream.of(Arguments.of("12 34", null, NumberFormatException.class, "internal whitespace"), Arguments.of("-", null, NumberFormatException.class, "lone minus"), Arguments.of("+", null, NumberFormatException.class, "lone plus"));
    }

    // ---------- Null and Empty Map Keys ----------
    private static Stream<Arguments> nullAndEmptyMapKeys() {
        // Map with a null key – must be built manually because Map.of forbids null keys
        Map<Object, Object> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, 5);
        return Stream.of(Arguments.of(mapWithNullKey, null, IllegalArgumentException.class, "null map key"), Arguments.of(Map.of("", 10), null, IllegalArgumentException.class, "empty string key"));
    }

    @Test
    @DisplayName("toInt extracts the special \"_v\" key from a map and converts it")
    void testToIntFromMapWithVKey() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // the special key used by MapConversions
        map.put(MapConversions.V, "123");
        // WHEN
        Integer result = MapConversions.toInt(map, converter);
        // THEN
        assertNotNull(result);
        assertEquals(123, result.intValue());
    }

    @Test
    @DisplayName("toInt extracts the generic \"value\" key from a map and converts it")
    void testToIntFromMapWithValueKey() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "456");
        // WHEN
        Integer result = MapConversions.toInt(map, converter);
        // THEN
        assertNotNull(result);
        assertEquals(456, result.intValue());
    }

    @Test
    @DisplayName("toInt returns null when the source object is null")
    void testToIntWithNullSource() {
        // GIVEN
        Object source = null;
        // WHEN
        Integer result = MapConversions.toInt(source, converter);
        // THEN
        assertNull(result);
    }

    @Test
    @DisplayName("toInt returns null when the map does not contain a recognizable key")
    void testToIntWithUnrelatedMap() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("unrelated", "789");
        // WHEN
        Integer result = MapConversions.toInt(map, converter);
        // THEN
        // The conversion falls back to the generic “value” key; none is present → null.
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("directIntegerTypes")
    void testDirectIntegerTypes(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("stringNumericConversion")
    void testStringNumericConversion(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("mapKeyExtraction")
    void testMapKeyExtraction(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullAndInvalidInputs")
    void testNullAndInvalidInputs(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("converterDelegation")
    void testConverterDelegation(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        // Create a mock Converter for these two cases
        Converter mockConverter = mock(Converter.class);
        if (from instanceof String) {
            // any String → 999
            when(mockConverter.convert(eq("123"), eq(Integer.class))).thenReturn(999);
        } else if (from instanceof Map) {
            // map value "321" → 111
            when(mockConverter.convert(eq("321"), eq(Integer.class))).thenReturn(111);
        }
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, mockConverter));
        } else {
            Integer result = invokeToInt(from, mockConverter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("otherNumberTypes")
    void testOtherNumberTypes(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("overflowAndBoundary")
    void testOverflowAndBoundary(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("stringEdgeCases")
    void testStringEdgeCases(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("caseSensitivityInMapKeys")
    void testCaseSensitivityInMapKeys(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("converterReturningNull")
    void testConverterReturningNull(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        Converter mockConverter = mock(Converter.class);
        // Return null for any conversion request
        when(mockConverter.convert(any(), eq(Integer.class))).thenReturn(null);
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, mockConverter));
        } else {
            Integer result = invokeToInt(from, mockConverter);
            assertNull(result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unsupportedMapValueTypes")
    void testUnsupportedMapValueTypes(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("mapValueParsingErrors")
    void testMapValueParsingErrors(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("numericTypeOverflow")
    void testNumericTypeOverflow(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unsupportedTypes")
    void testUnsupportedTypes(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidStringFormats")
    void testInvalidStringFormats(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullAndEmptyMapKeys")
    void testNullAndEmptyMapKeys(Object from, Integer expected, Class<? extends Throwable> exception, String description) {
        if (exception != null) {
            assertThrows(exception, () -> invokeToInt(from, converter));
        } else {
            Integer result = invokeToInt(from, converter);
            assertEquals(expected, result);
        }
    }
}
