package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

class MapConversions_toShort_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        // If a no‑arg constructor is not available, create a minimal options object.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    /**
     * Supplies all test cases. The data mirrors the JSON test matrix.
     */
    private static Stream<Arguments> conversionScenarios() {
        // Helper to create a map with a single entry.
        java.util.function.BiFunction<String, Object, Map<String, Object>> singleMap = (k, v) -> {
            Map<String, Object> m = new HashMap<>();
            m.put(k, v);
            return m;
        };
        // Helper to create a map with both recognised keys.
        java.util.function.BiFunction<Object, Object, Map<String, Object>> dualMap = (value, alt) -> {
            Map<String, Object> m = new HashMap<>();
            m.put(MapConversions.VALUE, value);
            m.put(MapConversions.V, alt);
            return m;
        };
        // Helper to create a nested map representing a class/value pair.
        java.util.function.BiFunction<String, String, Map<String, Object>> classValueMap = (cls, val) -> {
            Map<String, Object> inner = new HashMap<>();
            inner.put("class", cls);
            inner.put("value", val);
            return inner;
        };
        return Stream.of(// -------------------- conversion scenarios --------------------
        Arguments.of("Map contains key \"value\" with an Integer", singleMap.apply(MapConversions.VALUE, 123), converter, Short.valueOf((short) 123), null), Arguments.of("Map contains alternative key \"_v\" with a numeric String", singleMap.apply(MapConversions.V, "456"), converter, Short.valueOf((short) 456), null), Arguments.of("Map contains key \"value\" with a numeric String", singleMap.apply(MapConversions.VALUE, "789"), converter, Short.valueOf((short) 789), null), Arguments.of("Map contains key \"value\" with null", singleMap.apply(MapConversions.VALUE, null), converter, null, null), Arguments.of("Map missing recognised keys – should throw ConversionException", singleMap.apply("someOtherKey", 12), converter, null, ConversionException.class), Arguments.of("Plain Integer source – should convert directly", 321, converter, Short.valueOf((short) 321), null), Arguments.of("Non‑numeric String source – should throw ConversionException", "notANumber", converter, null, ConversionException.class), Arguments.of("Null source object – should return null", null, converter, null, null), // -------------------- additional scenarios --------------------
        Arguments.of("Map with both \"value\" and \"_v\" – prefers \"value\"", dualMap.apply(100, "200"), converter, Short.valueOf((short) 100), null), Arguments.of("String value with surrounding whitespace – should trim", singleMap.apply(MapConversions.VALUE, " 300 "), converter, Short.valueOf((short) 300), null), Arguments.of("String value with non‑numeric characters – ConversionException", singleMap.apply(MapConversions.VALUE, "400abc"), converter, null, ConversionException.class), Arguments.of("Integer value exceeding Short.MAX_VALUE – ConversionException", singleMap.apply(MapConversions.VALUE, 40000), converter, null, ConversionException.class), Arguments.of("Maximum positive Short value – conversion succeeds", singleMap.apply(MapConversions.VALUE, 32767), converter, Short.valueOf((short) 32767), null), Arguments.of("Minimum negative Short value – conversion succeeds", singleMap.apply(MapConversions.VALUE, -32768), converter, Short.valueOf((short) -32768), null), Arguments.of("Value just above Short.MAX_VALUE – ConversionException", singleMap.apply(MapConversions.VALUE, 32768), converter, null, ConversionException.class), Arguments.of("Value just below Short.MIN_VALUE – ConversionException", singleMap.apply(MapConversions.VALUE, -32769), converter, null, ConversionException.class), Arguments.of("Nested BigDecimal (integral) – should convert", singleMap.apply(MapConversions.VALUE, classValueMap.apply("java.math.BigDecimal", "123")), converter, Short.valueOf((short) 123), null), Arguments.of("Nested BigDecimal with fraction – ConversionException", singleMap.apply(MapConversions.VALUE, classValueMap.apply("java.math.BigDecimal", "123.45")), converter, null, ConversionException.class), Arguments.of("Nested BigInteger within range – should convert", singleMap.apply(MapConversions.VALUE, classValueMap.apply("java.math.BigInteger", "123")), converter, Short.valueOf((short) 123), null), Arguments.of("Nested BigInteger out of range – ConversionException", singleMap.apply(MapConversions.VALUE, classValueMap.apply("java.math.BigInteger", "40000")), converter, null, ConversionException.class), Arguments.of("Boolean true as map value – ConversionException", singleMap.apply(MapConversions.VALUE, Boolean.TRUE), converter, null, ConversionException.class), Arguments.of("Single‑digit numeric String – should convert", singleMap.apply(MapConversions.VALUE, "5"), converter, Short.valueOf((short) 5), null), Arguments.of("Empty String – ConversionException", singleMap.apply(MapConversions.VALUE, ""), converter, null, ConversionException.class), Arguments.of("Alternative key \"_v\" with numeric String – should convert", singleMap.apply(MapConversions.V, "600"), converter, Short.valueOf((short) 600), null), Arguments.of("Null \"value\" but valid \"_v\" – should fallback to \"_v\"", dualMap.apply(null, "700"), converter, Short.valueOf((short) 700), null), Arguments.of("Both recognised keys null – should return null", dualMap.apply(null, null), converter, null, null), // -------------------- additional exception scenarios --------------------
        Arguments.of("Nested map with unsupported class (String) – ConversionException", singleMap.apply(MapConversions.VALUE, classValueMap.apply("java.lang.String", "abc")), converter, null, ConversionException.class), // Duplicate of overflow case already covered – kept for completeness
        Arguments.of("Integer value exceeding Short.MAX_VALUE – ConversionException (duplicate)", singleMap.apply(MapConversions.VALUE, 40000), converter, null, ConversionException.class), Arguments.of("String representing a too‑large number – ConversionException", singleMap.apply(MapConversions.VALUE, "40000"), converter, null, ConversionException.class), Arguments.of("Double with fractional part – ConversionException", singleMap.apply(MapConversions.VALUE, 123.45d), converter, null, ConversionException.class), Arguments.of("Float with fractional part – ConversionException", singleMap.apply(MapConversions.VALUE, 123.45f), converter, null, ConversionException.class), Arguments.of("Numeric String with decimal point – ConversionException", singleMap.apply(MapConversions.VALUE, "123.45"), converter, null, ConversionException.class), Arguments.of("String containing only whitespace – ConversionException", singleMap.apply(MapConversions.VALUE, "   "), converter, null, ConversionException.class), Arguments.of("Hexadecimal String – ConversionException", singleMap.apply(MapConversions.VALUE, "0x1F"), converter, null, ConversionException.class), Arguments.of("Scientific notation String – ConversionException", singleMap.apply(MapConversions.VALUE, "123e2"), converter, null, ConversionException.class), Arguments.of("Boolean true directly in map – ConversionException", singleMap.apply(MapConversions.VALUE, Boolean.TRUE), converter, null, ConversionException.class), Arguments.of("Non‑numeric single character String – ConversionException", singleMap.apply(MapConversions.VALUE, "A"), converter, null, ConversionException.class), Arguments.of("Null Converter argument – NullPointerException", singleMap.apply(MapConversions.VALUE, "123"), null, null, NullPointerException.class), Arguments.of("Source object is a List – ConversionException", Arrays.asList(1, 2, 3), converter, null, ConversionException.class), Arguments.of("Nested map representing a Date – ConversionException", singleMap.apply(MapConversions.VALUE, classValueMap.apply("java.util.Date", "2023-01-01T00:00:00Z")), converter, null, ConversionException.class), Arguments.of("BigDecimal in scientific notation – ConversionException", singleMap.apply(MapConversions.VALUE, classValueMap.apply("java.math.BigDecimal", "1E3")), converter, null, ConversionException.class));
    }

    @Test
    void toShort_fromInteger() {
        Integer source = 123;
        Short result = MapConversions.toShort(source, converter);
        assertEquals(Short.valueOf((short) 123), result);
    }

    @Test
    void toShort_fromString() {
        String source = "45";
        Short result = MapConversions.toShort(source, converter);
        assertEquals(Short.valueOf((short) 45), result);
    }

    @Test
    void toShort_fromMapWithValueKey_Number() {
        Map<String, Object> map = new HashMap<>();
        // value is an Integer
        map.put(MapConversions.VALUE, 7);
        Short result = MapConversions.toShort(map, converter);
        assertEquals(Short.valueOf((short) 7), result);
    }

    @Test
    void toShort_fromMapWithValueKey_String() {
        Map<String, Object> map = new HashMap<>();
        // value is a String
        map.put(MapConversions.VALUE, "9");
        Short result = MapConversions.toShort(map, converter);
        assertEquals(Short.valueOf((short) 9), result);
    }

    @Test
    void toShort_fromNull_returnsNull() {
        Short result = MapConversions.toShort(null, converter);
        assertNull(result);
    }

    @Test
    void toShort_fromEmptyMap_returnsNull() {
        Map<String, Object> emptyMap = new HashMap<>();
        Short result = MapConversions.toShort(emptyMap, converter);
        assertNull(result);
    }

    /**
     * Parameterized test that drives all conversion scenarios described in the JSON
     * test specification. Each argument set contains:
     * <ul>
     *   <li>a description (used as the test name)</li>
     *   <li>the {@code from} object supplied to {@code toShort}</li>
     *   <li>the {@code Converter} instance to use (null for the NPE case)</li>
     *   <li>the expected {@link Short} result (null if no result is expected)</li>
     *   <li>the expected exception type (null if the call should succeed)</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("conversionScenarios")
    void testToShort(String description, Object from, Converter conv, Short expected, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // Expect an exception – use the appropriate assertThrows variant.
            assertThrows(expectedException, () -> MapConversions.toShort(from, conv), description);
        } else {
            Short actual = MapConversions.toShort(from, conv);
            assertEquals(expected, actual, description);
        }
    }
}
