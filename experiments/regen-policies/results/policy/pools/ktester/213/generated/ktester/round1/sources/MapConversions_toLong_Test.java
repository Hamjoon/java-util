package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
// <-- added import for Mockito
import org.mockito.Mockito;
// import org.junit.*;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import org.junit.runners.Parameterized.Parameters;
import java.time.LocalDateTime;

// @ExtendWith(MockitoExtension.class)
class MapConversions_toLong_Test {

    /**
     * Shared {@code Converter} instance used by all tests.
     */
    private static Converter converter;

//     @RunWith(Parameterized.class)
    public static class ValidConversionsTest {

        private final String description;

        private final Object from;

        private final Long expected;

        public ValidConversionsTest(String description, Object from, Long expected) {
            this.description = description;
            this.from = from;
            this.expected = expected;
        }

//         @Parameters(name = "[{index}] {0}")
//         public static Collection<Object[]> data() {
//             return Arrays.asList(new Object[][] { { "Direct Long instance", 42L, 42L }, { "Integer value", Integer.valueOf(7), 7L }, { "Numeric String", "123", 123L }, { "Map with \"value\" key (String)", mapOf("value", "456"), 456L }, { "Map with \"_v\" key (Long)", mapOf("_v", 789L), 789L }, { "Map with both keys (value wins)", mapOf("value", "111", "_v", "222"), 111L }, { "Byte value", Byte.valueOf((byte) 127), 127L }, { "Short negative value", Short.valueOf((short) -123), -123L }, { "Float value (truncates)", 12.34f, 12L }, { "Double value (truncates)", 56.78d, 56L }, { "BigDecimal large integer", new BigDecimal("9999999999"), 9_999_999_999L }, { "BigInteger within range", new BigInteger("1234567890123456789"), 1_234_567_890_123_456_789L }, { "AtomicLong value", new AtomicLong(42L), 42L }, { "AtomicInteger value", new AtomicInteger(5), 5L }, { "String with leading/trailing whitespace", "  321  ", 321L }, { "String with plus sign", "+77", 77L }, { "Negative numeric string", "-88", -88L }, { "Zero string", "0", 0L }, { "Long.MAX_VALUE string", "9223372036854775807", Long.MAX_VALUE }, { "Long.MIN_VALUE string", "-9223372036854775808", Long.MIN_VALUE } });
//         }

//         @Test
//         public void shouldConvertValidInputs() {
//             Long result = MapConversions.toLong(from, MapConversions_toLong_Test.converter);
//             Assert.assertEquals("Conversion result differs for: " + description, expected, result);
//         }
    }

//     @RunWith(Parameterized.class)
    public static class InvalidInputsTest {

        private final String description;

        private final Object from;

        private final Class<? extends Throwable> expectedException;

        public InvalidInputsTest(String description, Object from, Class<? extends Throwable> expectedException) {
            this.description = description;
            this.from = from;
            this.expectedException = expectedException;
        }

//         @Parameters(name = "[{index}] {0}")
//         public static Collection<Object[]> data() {
//             return Arrays.asList(new Object[][] { { "Boolean value", Boolean.TRUE, IllegalArgumentException.class }, { "Non‑numeric String", "abc", NumberFormatException.class }, { "Map without value keys", mapOf("unknown", "999"), IllegalArgumentException.class }, { "Arbitrary Object", new Object(), IllegalArgumentException.class }, { "Map with non‑numeric \"value\"", mapOf("value", "abc"), NumberFormatException.class }, { "Empty map", Collections.emptyMap(), IllegalArgumentException.class }, { "java.util.Date instance", new Date(), IllegalArgumentException.class }, { "java.time.LocalDateTime instance", LocalDateTime.now(), IllegalArgumentException.class }, { "String overflow (greater than Long.MAX_VALUE)", "9223372036854775808", NumberFormatException.class }, { "String underflow (less than Long.MIN_VALUE)", "-9223372036854775809", NumberFormatException.class }, { "BigInteger overflow", new BigInteger("9223372036854775808"), IllegalArgumentException.class }, { "BigInteger underflow", new BigInteger("-9223372036854775809"), IllegalArgumentException.class } });
//         }

//         @Test
//         public void shouldThrowForInvalidInputs() {
//             try {
//                 MapConversions.toLong(from, MapConversions_toLong_Test.converter);
//                 Assert.fail("Expected exception for: " + description);
//             } catch (Throwable e) {
//                 Assert.assertTrue("Expected " + expectedException.getName() + " but got " + e.getClass().getName(), expectedException.isInstance(e));
//             }
//         }
    }

//     @RunWith(Parameterized.class)
    public static class MapEdgeCasesTest {

        private final String description;

        private final Map<String, Object> map;

        // either Long or Class<? extends Throwable>
        private final Object expected;

        public MapEdgeCasesTest(String description, Map<String, Object> map, Object expected) {
            this.description = description;
            this.map = map;
            this.expected = expected;
        }

//         @Parameters(name = "[{index}] {0}")
//         public static Collection<Object[]> data() {
//             return Arrays.asList(new Object[][] { { "Map value is Integer", mapOf("value", 555), 555L }, { "Map value is Long", mapOf("value", 666L), 666L }, { "Map value is Double", mapOf("value", 777.99), 777L }, { "Map primary null, fallback _v", mapOf("value", null, "_v", 888L), 888L }, { "Empty map", Collections.emptyMap(), IllegalArgumentException.class }, { "Map value non‑numeric string", mapOf("value", "abc"), NumberFormatException.class } });
//         }

//         @Test
//         public void shouldHandleMapEdgeCases() {
//             if (expected instanceof Class) {
//                 @SuppressWarnings("unchecked")
//                 Class<? extends Throwable> exc = (Class<? extends Throwable>) expected;
//                 try {
//                     MapConversions.toLong(map, MapConversions_toLong_Test.converter);
//                     Assert.fail("Expected exception for: " + description);
//                 } catch (Throwable e) {
//                     Assert.assertTrue("Expected " + exc.getName() + " but got " + e.getClass().getName(), exc.isInstance(e));
//                 }
//             } else {
//                 Long expectedLong = (Long) expected;
//                 Long result = MapConversions.toLong(map, MapConversions_toLong_Test.converter);
//                 Assert.assertEquals("Incorrect conversion for: " + description, expectedLong, result);
//             }
//         }
    }

//     @BeforeAll
    static void setupBeforeAll() {
        // The real Converter constructor is not needed for these tests.
        converter = Mockito.mock(Converter.class);
    }

//     @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

//     private static Stream<Arguments> validConversionsProvider() {
//         return // Direct Long instance – should return the same instance
//         Stream.// Direct Long instance – should return the same instance
//         of(// Integer → Long
//         Arguments.of("Direct Long instance", 42L, 42L), // Numeric String → Long
//         Arguments.of("Integer value", Integer.valueOf(7), 7L), // Map with key "value" (String)
//         Arguments.of("Numeric String", "123", 123L), // Map with key "_v" (Long)
//         Arguments.of("Map with \"value\" key (String)", mapOf("value", "456"), 456L), // Map with both keys – “value” wins
//         Arguments.of("Map with \"_v\" key (Long)", mapOf("_v", 789L), 789L), // Byte → Long
//         Arguments.of("Map with both keys (value wins)", mapOf("value", "111", "_v", "222"), 111L), // Short (negative) → Long
//         Arguments.of("Byte value", Byte.valueOf((byte) 127), 127L), // Float → Long (truncates)
//         Arguments.of("Short negative value", Short.valueOf((short) -123), -123L), // Double → Long (truncates)
//         Arguments.of("Float value (truncates)", 12.34f, 12L), // BigDecimal → Long
//         Arguments.of("Double value (truncates)", 56.78d, 56L), // BigInteger → Long (fits)
//         Arguments.of("BigDecimal large integer", new BigDecimal("9999999999"), 9_999_999_999L), // AtomicLong → Long
//         Arguments.of("BigInteger within range", new BigInteger("1234567890123456789"), 1_234_567_890_123_456_789L), // AtomicInteger → Long
//         Arguments.of("AtomicLong value", new AtomicLong(42L), 42L), // String with whitespace
//         Arguments.of("AtomicInteger value", new AtomicInteger(5), 5L), // String with explicit plus sign
//         Arguments.of("String with leading/trailing whitespace", "  321  ", 321L), // Negative numeric string
//         Arguments.of("String with plus sign", "+77", 77L), // Zero string
//         Arguments.of("Negative numeric string", "-88", -88L), // Long.MAX_VALUE string
//         Arguments.of("Zero string", "0", 0L), // Long.MIN_VALUE string
//         Arguments.of("Long.MAX_VALUE string", "9223372036854775807", Long.MAX_VALUE), Arguments.of("Long.MIN_VALUE string", "-9223372036854775808", Long.MIN_VALUE));
//     }

//     private static Stream<Arguments> invalidInputsProvider() {
//         return // Boolean cannot be converted
//         Stream.// Boolean cannot be converted
//         of(// Non‑numeric String
//         Arguments.of("Boolean value", Boolean.TRUE, IllegalArgumentException.class), // Map without expected keys
//         Arguments.of("Non‑numeric String", "abc", NumberFormatException.class), // Arbitrary Object
//         Arguments.of("Map without value keys", mapOf("unknown", "999"), IllegalArgumentException.class), // Map with non‑numeric "value"
//         Arguments.of("Arbitrary Object", new Object(), IllegalArgumentException.class), // Empty map
//         Arguments.of("Map with non‑numeric \"value\"", mapOf("value", "abc"), NumberFormatException.class), // Date (unsupported)
//         Arguments.of("Empty map", Collections.emptyMap(), IllegalArgumentException.class), // LocalDateTime (unsupported)
//         Arguments.of("java.util.Date instance", new Date(), IllegalArgumentException.class), // String overflow > Long.MAX_VALUE
//         Arguments.of("java.time.LocalDateTime instance", java.time.LocalDateTime.now(), IllegalArgumentException.class), // String underflow < Long.MIN_VALUE
//         Arguments.of("String overflow (greater than Long.MAX_VALUE)", "9223372036854775808", NumberFormatException.class), // BigInteger overflow
//         Arguments.of("String underflow (less than Long.MIN_VALUE)", "-9223372036854775809", NumberFormatException.class), // BigInteger underflow
//         Arguments.of("BigInteger overflow", new BigInteger("9223372036854775808"), IllegalArgumentException.class), Arguments.of("BigInteger underflow", new BigInteger("-9223372036854775809"), IllegalArgumentException.class));
//     }

//     private static Stream<Arguments> mapEdgeCasesProvider() {
//         return // "value" holds Integer
//         Stream.// "value" holds Integer
//         of(// "value" holds Long (no conversion)
//         Arguments.of("Map value is Integer", mapOf("value", 555), 555L), // "value" holds Double (truncates)
//         Arguments.of("Map value is Long", mapOf("value", 666L), 666L), // Primary key null, fallback "_v" valid
//         Arguments.of("Map value is Double", mapOf("value", 777.99), 777L), // Empty map – illegal argument
//         Arguments.of("Map primary null, fallback _v", mapOf("value", null, "_v", 888L), 888L), // Non‑numeric string in "value"
//         Arguments.of("Empty map", Collections.emptyMap(), IllegalArgumentException.class), Arguments.of("Map value non‑numeric string", mapOf("value", "abc"), NumberFormatException.class));
//     }

    /**
     * Utility for building a mutable map from an even number of arguments:
     * key1, value1, key2, value2, ...
     */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = (String) keyValues[i];
            Object value = keyValues[i + 1];
            map.put(key, value);
        }
        return map;
    }

//     @Test
//     void shouldConvertStringToLong() {
//         Assert.assertEquals(Long.valueOf(123L), MapConversions.toLong("123", converter));
//     }

//     @Test
//     void shouldReturnSameLongWhenSourceIsLong() {
//         Long source = 321L;
//         Assert.assertSame(source, MapConversions.toLong(source, converter));
//     }

//     @Test
//     void shouldExtractValueFromMapUsingValueKey() {
//         Map<String, Object> map = Collections.singletonMap("value", "456");
//         Assert.assertEquals(Long.valueOf(456L), MapConversions.toLong(map, converter));
//     }

//     @Test
//     void shouldExtractValueFromMapUsingVKey() {
//         Map<String, Object> map = Collections.singletonMap("_v", 789L);
//         Assert.assertEquals(Long.valueOf(789L), MapConversions.toLong(map, converter));
//     }

//     @Test
//     void shouldReturnNullWhenSourceIsNull() {
//         Assert.assertNull(MapConversions.toLong(null, converter));
//     }

//     @Test
//     void shouldReturnNullWhenMapDoesNotContainValueKeys() {
//         Map<String, Object> map = Collections.singletonMap("other", "100");
//         Assert.assertNull(MapConversions.toLong(map, converter));
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("validConversionsProvider")
//     void shouldConvertValidInputs(String description, Object from, Long expected) {
//         Long result = MapConversions.toLong(from, converter);
//         assertEquals(expected, result, "Conversion result differs for: " + description);
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("invalidInputsProvider")
//     void shouldThrowForInvalidInputs(String description, Object from, Class<? extends Throwable> expectedException) {
//         assertThrows(expectedException, () -> MapConversions.toLong(from, converter), "Expected exception for: " + description);
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("mapEdgeCasesProvider")
//     void shouldHandleMapEdgeCases(String description, Map<String, Object> map, Object expected) {
//         if (expected instanceof Class) {
//             @SuppressWarnings("unchecked")
//             Class<? extends Throwable> exc = (Class<? extends Throwable>) expected;
//             assertThrows(exc, () -> MapConversions.toLong(map, converter), "Expected exception for: " + description);
//         } else {
//             Long expectedLong = (Long) expected;
//             Long result = MapConversions.toLong(map, converter);
//             assertEquals(expectedLong, result, "Incorrect conversion for: " + description);
//         }
//     }
}