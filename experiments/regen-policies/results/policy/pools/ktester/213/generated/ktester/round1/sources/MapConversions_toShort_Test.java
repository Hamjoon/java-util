package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
// import org.junit.*;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class MapConversions_toShort_Test {

    private Converter converter;

//     @BeforeEach
    void setupBeforeEach() {
        // The Converter class requires a ConverterOptions instance.
        // Using a fresh options object gives the default behaviour required for the tests.
//         converter = new Converter(new ConverterOptions());
    }

//     private static Stream<Arguments> successfulConversionProvider() {
//         return Stream.of(Arguments.of("Direct Short instance", Short.valueOf((short) 5), Short.valueOf((short) 5)), Arguments.of("Integer convertible to Short", Integer.valueOf(123), Short.valueOf((short) 123)), Arguments.of("String numeric", "42", Short.valueOf((short) 42)), Arguments.of("Map with key \"value\" -> \"7\"", mapWith(MapConversions.VALUE, "7"), Short.valueOf((short) 7)), Arguments.of("Map with key \"_v\" -> \"9\"", mapWith(MapConversions.V, "9"), Short.valueOf((short) 9)), Arguments.of("Object with numeric toString()", new Object() {
// 
//             @Override
//             public String toString() {
//                 return "15";
//             }
//         }, Short.valueOf((short) 15)), Arguments.of("Byte instance", Byte.valueOf((byte) 10), Short.valueOf((short) 10)), Arguments.of("Long within range", Long.valueOf(12345L), Short.valueOf((short) 12345)), Arguments.of("Double whole number", Double.valueOf(12.0), Short.valueOf((short) 12)), Arguments.of("Float with fraction (truncated)", Float.valueOf(7.9f), Short.valueOf((short) 7)), Arguments.of("BigDecimal integer", BigDecimal.valueOf(9), Short.valueOf((short) 9)), Arguments.of("BigInteger integer", BigInteger.valueOf(20), Short.valueOf((short) 20)), Arguments.of("String with surrounding whitespace", " 30 ", Short.valueOf((short) 30)), Arguments.of("String with explicit plus sign", "+31", Short.valueOf((short) 31)), Arguments.of("Map with key \"value\" -> null", mapWith(MapConversions.VALUE, null), null), Arguments.of("StringBuilder numeric", new StringBuilder("45"), Short.valueOf((short) 45)), Arguments.of("CharBuffer numeric", CharBuffer.wrap("55"), Short.valueOf((short) 55)), Arguments.of("AtomicInteger numeric", new AtomicInteger(60), Short.valueOf((short) 60)));
//     }

//     private static Stream<Arguments> failureConversionProvider() {
//         return Stream.of(Arguments.of("Non‑numeric string", "notANumber", NumberFormatException.class), Arguments.of("Unsupported plain Object", new Object(), ClassCastException.class), Arguments.of("Map value non‑numeric string", mapWith(MapConversions.VALUE, "abc"), NumberFormatException.class), Arguments.of("Boolean value", Boolean.TRUE, ClassCastException.class), Arguments.of("Empty string", "", NumberFormatException.class), Arguments.of("String with only whitespace", "   ", NumberFormatException.class), Arguments.of("String with only plus sign", "+", NumberFormatException.class), Arguments.of("CharBuffer non‑numeric", CharBuffer.wrap("abc"), NumberFormatException.class), Arguments.of("List source", Arrays.asList(1), ClassCastException.class), Arguments.of("Primitive array source", new int[] { 5 }, ClassCastException.class), Arguments.of("Double.NaN", Double.NaN, NumberFormatException.class), Arguments.of("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY, NumberFormatException.class), Arguments.of("Empty Map without expected keys", new HashMap<>(), ClassCastException.class));
//     }

    private static Map<String, Object> mapWith(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

//     @Test
//     void toShort_fromMapWithUnderscoreVKey_stringNumber() {
//         Map<String, Object> map = new HashMap<>();
//         map.put(MapConversions.V, "123");
//         Short result = MapConversions.toShort(map, converter);
//         assertNotNull(result);
//         assertEquals(Short.valueOf((short) 123), result);
//     }

//     @Test
//     void toShort_fromMapWithValueKey_integerNumber() {
//         Map<String, Object> map = new HashMap<>();
//         map.put(MapConversions.VALUE, Integer.valueOf(45));
//         Short result = MapConversions.toShort(map, converter);
//         assertNotNull(result);
//         assertEquals(Short.valueOf((short) 45), result);
//     }

//     @Test
//     void toShort_fromDirectShortWrapper() {
//         Short source = Short.valueOf((short) 7);
//         Short result = MapConversions.toShort(source, converter);
//         assertNotNull(result);
//         assertEquals(source, result);
//     }

//     @Test
//     void toShort_fromDirectPrimitiveShort() {
//         short source = 8;
//         Short result = MapConversions.toShort(source, converter);
//         assertNotNull(result);
//         assertEquals(Short.valueOf(source), result);
//     }

//     @Test
//     void toShort_fromStringParsable() {
//         String source = "9";
//         Short result = MapConversions.toShort(source, converter);
//         assertNotNull(result);
//         assertEquals(Short.valueOf((short) 9), result);
//     }

//     @Test
//     void toShort_fromNull_returnsNull() {
//         Short result = MapConversions.toShort(null, converter);
//         assertNull(result);
//     }

//     @Test
//     void toShort_fromMapWithoutValueKeys_returnsNull() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("someOtherKey", "123");
//         Short result = MapConversions.toShort(map, converter);
//         assertNull(result);
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("successfulConversionProvider")
//     void toShort_successfulConversions(String description, Object from, Short expected) {
//         Short result = MapConversions.toShort(from, converter);
//         assertEquals(expected, result, description);
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("failureConversionProvider")
//     void toShort_expectedFailures(String description, Object from, Class<? extends Throwable> expectedException) {
//         assertThrows(expectedException, () -> MapConversions.toShort(from, converter), description);
//     }

//     @Test
//     void toShort_nullConverter_throwsNullPointerException() {
//         Integer source = Integer.valueOf(10);
//         try {
//             MapConversions.toShort(source, null);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//             // expected
//         }
//     }

//     @Test
//     public void successfulConversion_DirectShortInstance() {
//         Short result = MapConversions.toShort(Short.valueOf((short) 5), converter);
//         assertEquals(Short.valueOf((short) 5), result);
//     }

//     @Test
//     public void successfulConversion_IntegerConvertibleToShort() {
//         Short result = MapConversions.toShort(Integer.valueOf(123), converter);
//         assertEquals(Short.valueOf((short) 123), result);
//     }

//     @Test
//     public void successfulConversion_StringNumeric() {
//         Short result = MapConversions.toShort("42", converter);
//         assertEquals(Short.valueOf((short) 42), result);
//     }

//     @Test
//     public void successfulConversion_MapWithValueKey() {
//         Short result = MapConversions.toShort(mapWith(MapConversions.VALUE, "7"), converter);
//         assertEquals(Short.valueOf((short) 7), result);
//     }

//     @Test
//     public void successfulConversion_MapWithUnderscoreVKey() {
//         Short result = MapConversions.toShort(mapWith(MapConversions.V, "9"), converter);
//         assertEquals(Short.valueOf((short) 9), result);
//     }

//     @Test
//     public void successfulConversion_ObjectWithNumericToString() {
//         Object src = new Object() {
// 
//             @Override
//             public String toString() {
//                 return "15";
//             }
//         };
//         Short result = MapConversions.toShort(src, converter);
//         assertEquals(Short.valueOf((short) 15), result);
//     }

//     @Test
//     public void successfulConversion_ByteInstance() {
//         Short result = MapConversions.toShort(Byte.valueOf((byte) 10), converter);
//         assertEquals(Short.valueOf((short) 10), result);
//     }

//     @Test
//     public void successfulConversion_LongWithinRange() {
//         Short result = MapConversions.toShort(Long.valueOf(12345L), converter);
//         assertEquals(Short.valueOf((short) 12345), result);
//     }

//     @Test
//     public void successfulConversion_DoubleWholeNumber() {
//         Short result = MapConversions.toShort(Double.valueOf(12.0), converter);
//         assertEquals(Short.valueOf((short) 12), result);
//     }

//     @Test
//     public void successfulConversion_FloatWithFractionTruncated() {
//         Short result = MapConversions.toShort(Float.valueOf(7.9f), converter);
//         assertEquals(Short.valueOf((short) 7), result);
//     }

//     @Test
//     public void successfulConversion_BigDecimalInteger() {
//         Short result = MapConversions.toShort(BigDecimal.valueOf(9), converter);
//         assertEquals(Short.valueOf((short) 9), result);
//     }

//     @Test
//     public void successfulConversion_BigIntegerInteger() {
//         Short result = MapConversions.toShort(BigInteger.valueOf(20), converter);
//         assertEquals(Short.valueOf((short) 20), result);
//     }

//     @Test
//     public void successfulConversion_StringWithWhitespace() {
//         Short result = MapConversions.toShort(" 30 ", converter);
//         assertEquals(Short.valueOf((short) 30), result);
//     }

//     @Test
//     public void successfulConversion_StringWithPlusSign() {
//         Short result = MapConversions.toShort("+31", converter);
//         assertEquals(Short.valueOf((short) 31), result);
//     }

//     @Test
//     public void successfulConversion_MapWithNullValue() {
//         Short result = MapConversions.toShort(mapWith(MapConversions.VALUE, null), converter);
//         assertNull(result);
//     }

//     @Test
//     public void successfulConversion_StringBuilderNumeric() {
//         Short result = MapConversions.toShort(new StringBuilder("45"), converter);
//         assertEquals(Short.valueOf((short) 45), result);
//     }

//     @Test
//     public void successfulConversion_CharBufferNumeric() {
//         Short result = MapConversions.toShort(CharBuffer.wrap("55"), converter);
//         assertEquals(Short.valueOf((short) 55), result);
//     }

//     @Test
//     public void successfulConversion_AtomicIntegerNumeric() {
//         Short result = MapConversions.toShort(new AtomicInteger(60), converter);
//         assertEquals(Short.valueOf((short) 60), result);
//     }

//     @Test
//     public void failureConversion_NonNumericString() {
//         try {
//             MapConversions.toShort("notANumber", converter);
//             fail("Expected NumberFormatException");
//         } catch (NumberFormatException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_UnsupportedPlainObject() {
//         try {
//             MapConversions.toShort(new Object(), converter);
//             fail("Expected ClassCastException");
//         } catch (ClassCastException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_MapValueNonNumericString() {
//         try {
//             MapConversions.toShort(mapWith(MapConversions.VALUE, "abc"), converter);
//             fail("Expected NumberFormatException");
//         } catch (NumberFormatException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_BooleanValue() {
//         try {
//             MapConversions.toShort(Boolean.TRUE, converter);
//             fail("Expected ClassCastException");
//         } catch (ClassCastException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_EmptyString() {
//         try {
//             MapConversions.toShort("", converter);
//             fail("Expected NumberFormatException");
//         } catch (NumberFormatException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_StringWithOnlyWhitespace() {
//         try {
//             MapConversions.toShort("   ", converter);
//             fail("Expected NumberFormatException");
//         } catch (NumberFormatException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_StringWithOnlyPlusSign() {
//         try {
//             MapConversions.toShort("+", converter);
//             fail("Expected NumberFormatException");
//         } catch (NumberFormatException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_CharBufferNonNumeric() {
//         try {
//             MapConversions.toShort(CharBuffer.wrap("abc"), converter);
//             fail("Expected NumberFormatException");
//         } catch (NumberFormatException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_ListSource() {
//         try {
//             MapConversions.toShort(Arrays.asList(1), converter);
//             fail("Expected ClassCastException");
//         } catch (ClassCastException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_PrimitiveArraySource() {
//         try {
//             MapConversions.toShort(new int[] { 5 }, converter);
//             fail("Expected ClassCastException");
//         } catch (ClassCastException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_DoubleNaN() {
//         try {
//             MapConversions.toShort(Double.NaN, converter);
//             fail("Expected NumberFormatException");
//         } catch (NumberFormatException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_DoublePositiveInfinity() {
//         try {
//             MapConversions.toShort(Double.POSITIVE_INFINITY, converter);
//             fail("Expected NumberFormatException");
//         } catch (NumberFormatException ignored) {
//         }
//     }

//     @Test
//     public void failureConversion_EmptyMapWithoutExpectedKeys() {
//         try {
//             MapConversions.toShort(new HashMap<>(), converter);
//             fail("Expected ClassCastException");
//         } catch (ClassCastException ignored) {
//         }
//     }
}