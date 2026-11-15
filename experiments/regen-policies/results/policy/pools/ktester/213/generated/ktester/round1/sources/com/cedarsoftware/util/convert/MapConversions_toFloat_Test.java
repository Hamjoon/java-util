package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import java.nio.CharBuffer;
// import org.junit.*;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class MapConversions_toFloat_Test {

    /**
     * The Converter used for all tests – it is a real instance, not a mock,
     *  because {@code MapConversions.toFloat} delegates to the converter’s
     *  conversion logic.
     */
    private static Converter converter;

    /* --------------------------------------------------------------------- */
    private static final class Arguments extends ArrayList<Object> {

        private Arguments(Object... values) {
            super(Arrays.asList(values));
        }

        static Arguments of(Object... values) {
            return new Arguments(values);
        }

//         Object get(int index) {
//             return super.get(index);
//         }
    }

//     @BeforeAll
    static void setupBeforeAll() {
        // The Converter now requires a ConverterOptions instance.
        // Using a fresh options object gives us the default behaviour.
//         converter = new Converter(new ConverterOptions());
    }

//     @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> numericWideningProvider() {
        return Stream.of(Arguments.of(Double.valueOf(56.78), 56.78f), Arguments.of(Integer.valueOf(42), 42.0f), Arguments.of(Long.valueOf(123456789L), 123456789.0f), Arguments.of(Short.valueOf((short) 7), 7.0f), Arguments.of(Byte.valueOf((byte) 9), 9.0f), Arguments.of(new AtomicInteger(77), 77.0f), Arguments.of(new AtomicLong(1234567890123L), 1.2345679E12f));
    }

    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(Arguments.of("3.1415", 3.1415f), Arguments.of("   2.5  ", 2.5f), Arguments.of("NaN", Float.NaN), Arguments.of("+42", 42.0f), Arguments.of("-3.14", -3.14f), Arguments.of("1e3", 1000.0f), Arguments.of("0", 0.0f), Arguments.of("Infinity", Float.POSITIVE_INFINITY), Arguments.of("-Infinity", Float.NEGATIVE_INFINITY), Arguments.of("", NumberFormatException.class), Arguments.of("   ", NumberFormatException.class), Arguments.of("5abc", NumberFormatException.class), Arguments.of("not_a_number", NumberFormatException.class), Arguments.of("null", NumberFormatException.class));
    }

    private static Stream<Arguments> charSequenceProvider() {
        return Stream.of(Arguments.of(new StringBuilder("7.77"), 7.77f), Arguments.of(CharBuffer.wrap("9.99"), 9.99f), Arguments.of(new StringBuilder(""), NumberFormatException.class), Arguments.of(new StringBuilder("   "), NumberFormatException.class), Arguments.of(new StringBuilder("12a34"), NumberFormatException.class));
    }

    private static Stream<Arguments> mapValueKeyProvider() {
        return Stream.of(// "value" key with numeric String
        Arguments.of(Map.of("value", "9.99"), 9.99f), // "value" key with Number
        Arguments.of(Map.of("value", 15), 15.0f), // both keys present – value wins
        Arguments.of(Map.of("value", "1.1", "_v", "2.2"), 1.1f), // missing both keys – illegal argument
        Arguments.of(Map.of("other", "5"), IllegalArgumentException.class), // null value for "value"
        Arguments.of(createMap("value", null), null), // null value for "_v"
        Arguments.of(createMap("_v", null), null), // both keys present but "value" maps to null – prefers "value"
        Arguments.of(createMap("value", null, "_v", "5.5"), null), // value holding CharSequence
        Arguments.of(createMap("value", new StringBuilder("6.66")), 6.66f), // value holding non‑numeric String – NumberFormatException
        Arguments.of(createMap("value", "abc123"), NumberFormatException.class), // value holding unsupported List – IllegalArgumentException
        Arguments.of(createMap("value", List.of(1, 2, 3)), IllegalArgumentException.class), // value holding Boolean – IllegalArgumentException
        Arguments.of(createMap("value", Boolean.FALSE), IllegalArgumentException.class));
    }

    private static Stream<Arguments> mapVKeyProvider() {
        return Stream.of(// "_v" key with numeric String
        Arguments.of(Map.of("_v", "4.2"), 4.2f), // "_v" key with Number
        Arguments.of(Map.of("_v", 8L), 8.0f), // "_v" key with CharSequence
        Arguments.of(Map.of("_v", CharBuffer.wrap("9.99")), 9.99f), // "_v" key with non‑numeric String – NumberFormatException
        Arguments.of(Map.of("_v", "not_a_number"), NumberFormatException.class), // "_v" key with unsupported Object – IllegalArgumentException
        Arguments.of(Map.of("_v", new Object()), IllegalArgumentException.class), // "_v" key with Boolean – IllegalArgumentException
        Arguments.of(Map.of("_v", Boolean.TRUE), IllegalArgumentException.class));
    }

    /**
     * Helper method to create a mutable map that can contain {@code null} values.
     */
    private static Map<String, Object> createMap(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /**
     * Verify that a map containing the standard “value” key is converted
     *  correctly to a {@code Float}.
     */
//     @Test
//     void shouldConvertMapWithValueKey_StringToFloat() {
//         Map<String, Object> source = new HashMap<>();
//         source.put(MapConversions.VALUE, "12.34");
//         Float result = MapConversions.toFloat(source, converter);
//         assertNotNull(result);
//         assertEquals(12.34f, result, 0.00001f);
//     }

    /**
     * Verify that a map containing the short “_v” key is also honoured.
     */
//     @Test
//     void shouldConvertMapWithVKey_IntegerToFloat() {
//         Map<String, Object> source = new HashMap<>();
//         source.put(MapConversions.V, 56);
//         Float result = MapConversions.toFloat(source, converter);
//         assertNotNull(result);
//         assertEquals(56.0f, result, 0.00001f);
//     }

    /**
     * Directly passing a {@code Float} should return the same value.
     */
//     @Test
//     void shouldReturnSameFloatWhenSourceIsFloat() {
//         Float input = 3.14f;
//         Float result = MapConversions.toFloat(input, converter);
//         assertSame(input, result);
//     }

    /**
     * A {@code Double} should be converted to a {@code Float}.
     */
//     @Test
//     void shouldConvertDoubleToFloat() {
//         Double input = 2.5;
//         Float result = MapConversions.toFloat(input, converter);
//         assertNotNull(result);
//         assertEquals(2.5f, result, 0.00001f);
//     }

    /**
     * Converting {@code null} should yield {@code null} (object types).
     */
//     @Test
//     void shouldReturnNullWhenSourceIsNull() {
//         assertNull(MapConversions.toFloat(null, converter));
//     }

    /**
     * A value that cannot be converted (e.g., a Boolean) should result in
     *  {@code null} rather than throwing an unexpected exception.
     */
//     @Test
//     void shouldReturnNullForNonConvertibleValue() {
//         Boolean input = Boolean.TRUE;
//         Float result = MapConversions.toFloat(input, converter);
//         assertNull(result);
//     }

//     @ParameterizedTest(name = "[{index}] from={0} → {1}f")
//     @MethodSource("numericWideningProvider")
//     void shouldConvertVariousNumberTypesToFloat(Object from, float expected) {
//         numericWideningProvider().forEach(arg -> {
//             Object from = arg.get(0);
//             float expected = (Float) arg.get(1);
//             Float result = MapConversions.toFloat(from, converter);
//             assertNotNull(result);
//             assertEquals(expected, result, 0.00001f);
//         });
//     }

//     @ParameterizedTest(name = "[{index}] \"{0}\" → {1}f")
//     @MethodSource("stringParsingProvider")
//     void shouldParseStringRepresentationsToFloat(String from, Object expected) {
//         stringParsingProvider().forEach(arg -> {
//             String from = (String) arg.get(0);
//             Object expected = arg.get(1);
//             if (expected instanceof Class && ((Class<?>) expected).equals(NumberFormatException.class)) {
//                 try {
//                     MapConversions.toFloat(from, converter);
//                     fail("Expected NumberFormatException for input: \"" + from + "\"");
//                 } catch (NumberFormatException ignored) {
//                 }
//             } else {
//                 Float result = MapConversions.toFloat(from, converter);
//                 assertNotNull(result);
//                 Float exp = (Float) expected;
//                 if (Float.isNaN(exp)) {
//                     assertTrue(Float.isNaN(result));
//                 } else if (Float.isInfinite(exp)) {
//                     assertTrue(Float.isInfinite(result) && (exp > 0) == (result > 0));
//                 } else {
//                     assertEquals(exp, result, 0.00001f);
//                 }
//             }
//         });
//     }

//     @ParameterizedTest(name = "[{index}] CharSequence {0} → {1}f")
//     @MethodSource("charSequenceProvider")
//     void shouldParseCharSequenceToFloat(CharSequence from, Object expected) {
//         charSequenceProvider().forEach(arg -> {
//             CharSequence from = (CharSequence) arg.get(0);
//             Object expected = arg.get(1);
//             if (expected instanceof Class && ((Class<?>) expected).equals(NumberFormatException.class)) {
//                 try {
//                     MapConversions.toFloat(from, converter);
//                     fail("Expected NumberFormatException for CharSequence: \"" + from + "\"");
//                 } catch (NumberFormatException ignored) {
//                 }
//             } else {
//                 Float result = MapConversions.toFloat(from, converter);
//                 assertNotNull(result);
//                 assertEquals((Float) expected, result, 0.00001f);
//             }
//         });
//     }

//     @Test
//     void shouldConvertBigDecimalToFloat() {
//         BigDecimal bd = new BigDecimal("12345.678");
//         Float result = MapConversions.toFloat(bd, converter);
//         assertNotNull(result);
//         assertEquals(12345.678f, result, 0.00001f);
//     }

//     @Test
//     void shouldConvertBigIntegerToFloat() {
//         BigInteger bi = new BigInteger("9876543210");
//         Float result = MapConversions.toFloat(bi, converter);
//         assertNotNull(result);
//         assertEquals(9.876543E9f, result, 0.00001f);
//     }

//     @ParameterizedTest(name = "[{index}] map {0} → {1}f")
//     @MethodSource("mapValueKeyProvider")
//     void shouldHandleMapWithValueKey(Map<String, Object> map, Object expected) {
//         mapValueKeyProvider().forEach(arg -> {
//             @SuppressWarnings("unchecked")
//             Map<String, Object> map = (Map<String, Object>) arg.get(0);
//             Object expected = arg.get(1);
//             if (expected instanceof Class) {
//                 Class<?> ex = (Class<?>) expected;
//                 try {
//                     MapConversions.toFloat(map, converter);
//                     fail("Expected exception " + ex.getSimpleName() + " for map: " + map);
//                 } catch (Exception e) {
//                     assertEquals(ex, e.getClass());
//                 }
//             } else {
//                 Float result = MapConversions.toFloat(map, converter);
//                 if (expected == null) {
//                     assertNull(result);
//                 } else {
//                     assertNotNull(result);
//                     assertEquals((Float) expected, result, 0.00001f);
//                 }
//             }
//         });
//     }

//     @ParameterizedTest(name = "[{index}] map {0} → {1}f")
//     @MethodSource("mapVKeyProvider")
//     void shouldHandleMapWithVKey(Map<String, Object> map, Object expected) {
//         mapVKeyProvider().forEach(arg -> {
//             @SuppressWarnings("unchecked")
//             Map<String, Object> map = (Map<String, Object>) arg.get(0);
//             Object expected = arg.get(1);
//             if (expected instanceof Class) {
//                 Class<?> ex = (Class<?>) expected;
//                 try {
//                     MapConversions.toFloat(map, converter);
//                     fail("Expected exception " + ex.getSimpleName() + " for map: " + map);
//                 } catch (Exception e) {
//                     assertEquals(ex, e.getClass());
//                 }
//             } else {
//                 Float result = MapConversions.toFloat(map, converter);
//                 if (expected == null) {
//                     assertNull(result);
//                 } else {
//                     assertNotNull(result);
//                     assertEquals((Float) expected, result, 0.00001f);
//                 }
//             }
//         });
//     }

//     @Test
//     void shouldRejectBooleanSource() {
//         try {
//             MapConversions.toFloat(Boolean.TRUE, converter);
//             fail("Expected IllegalArgumentException");
//         } catch (IllegalArgumentException ignored) {
//         }
//     }

//     @Test
//     void shouldRejectArbitraryObject() {
//         try {
//             MapConversions.toFloat(new Object(), converter);
//             fail("Expected IllegalArgumentException");
//         } catch (IllegalArgumentException ignored) {
//         }
//     }

//     @Test
//     void shouldThrowNPEWhenConverterIsNull() {
//         try {
//             MapConversions.toFloat("10.0", null);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void shouldConvertDoubleMaxValueToPositiveInfinity() {
//         Float result = MapConversions.toFloat(Double.MAX_VALUE, converter);
//         assertTrue(Float.isInfinite(result) && result > 0);
//     }

//     @Test
//     void shouldPreserveNegativeInfinity() {
//         Float result = MapConversions.toFloat(Double.NEGATIVE_INFINITY, converter);
//         assertTrue(Float.isInfinite(result) && result < 0);
//     }
}