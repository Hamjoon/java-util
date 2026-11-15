package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
// import org.junit.*;

// @ExtendWith(MockitoExtension.class)
class MapConversions_toBigInteger_Test {

    private Converter converter;

//     @BeforeEach
    void setupBeforeEach() {
        // Converter now requires a ConverterOptions instance; use the default options
//         converter = new Converter(new ConverterOptions());
    }

//     @AfterEach
    void teardownAfterEach() {
        // Clear the reference – helps the GC and isolates each test
        converter = null;
    }

//     private static Stream<Arguments> validDirectConversions() {
//         return // Long → BigInteger
//         Stream.// Long → BigInteger
//         of(// Double with whole number → BigInteger (fraction truncated)
//         arguments(Long.valueOf(9876543210L), new BigInteger("9876543210")), // String containing a valid integer → BigInteger
//         arguments(Double.valueOf(12345.0), new BigInteger("12345")), // Integer → BigInteger
//         arguments("-42424242424242424242", new BigInteger("-42424242424242424242")), // Short → BigInteger
//         arguments(Integer.valueOf(42), BigInteger.valueOf(42)), // Byte → BigInteger
//         arguments(Short.valueOf((short) -7), BigInteger.valueOf(-7)), // Float with fractional part → BigInteger (truncated toward zero)
//         arguments(Byte.valueOf((byte) 3), BigInteger.valueOf(3)), // BigDecimal → BigInteger (fraction discarded)
//         arguments(Float.valueOf(12.34f), BigInteger.valueOf(12)), // AtomicInteger → BigInteger
//         arguments(new BigDecimal("12345.678"), BigInteger.valueOf(12345)), // AtomicLong → BigInteger
//         arguments(new AtomicInteger(99), BigInteger.valueOf(99)), arguments(new AtomicLong(1234567890123L), new BigInteger("1234567890123")));
//     }

//     private static Stream<Arguments> validMapConversions() {
//         return // key "value" with numeric String
//         Stream.// key "value" with numeric String
//         of(// key "_v" with numeric Long
//         arguments(Map.of("value", "55555555555555555555"), new BigInteger("55555555555555555555")), // map with extra entries – only first matching key is used
//         arguments(Map.of("_v", 7777777777777777777L), new BigInteger("7777777777777777777")), arguments(new LinkedHashMap<String, Object>() {
// 
//             {
//                 put("value", "123");
//                 put("extra", "ignore");
//             }
//         }, new BigInteger("123")));
//     }

//     private static Stream<Arguments> unsupportedTypes() {
//         return Stream.of(arguments(new Date()), arguments(new Object()), arguments(Boolean.TRUE), arguments(Character.valueOf('9')), arguments(Arrays.asList("123", "456")), arguments(new byte[] { 1, 2, 3 }));
//     }

//     private static Stream<Arguments> invalidStringInputs() {
//         return Stream.of(arguments("12abc34"), arguments("   "));
//     }

//     private static Stream<Arguments> invalidMapValues() {
//         return Stream.of(arguments(Map.of("value", "notANumber")), arguments(Map.of("_v", null)));
//     }

//     private static Stream<Arguments> specialNumericInputs() {
//         return Stream.of(arguments(Double.NaN), arguments(Double.POSITIVE_INFINITY), arguments(Float.NaN));
//     }

//     @Test
//     void shouldReturnNullWhenSourceIsNull() {
//         Object source = null;
//         BigInteger result = MapConversions.toBigInteger(source, converter);
//         Assert.assertNull("Converting a null source should return null", result);
//     }

//     @Test
//     void shouldReturnSameBigIntegerWhenSourceIsAlreadyBigInteger() {
//         BigInteger expected = new BigInteger("12345678901234567890");
//         BigInteger result = MapConversions.toBigInteger(expected, converter);
//         Assert.assertSame("If the source is already a BigInteger it should be returned unchanged", expected, result);
//     }

//     @Test
//     void shouldConvertStringToBigInteger() {
//         String source = "987654321";
//         BigInteger result = MapConversions.toBigInteger(source, converter);
//         Assert.assertEquals(new BigInteger("987654321"), result);
//     }

//     @Test
//     void shouldExtractValueFromMapUsingKeyValue() {
//         Map<String, Object> source = new HashMap<>();
//         source.put("value", "5555");
//         BigInteger result = MapConversions.toBigInteger(source, converter);
//         Assert.assertEquals(new BigInteger("5555"), result);
//     }

//     @Test
//     void shouldExtractValueFromMapUsingKeyV() {
//         Map<String, Object> source = new HashMap<>();
//         source.put("_v", "7777");
//         BigInteger result = MapConversions.toBigInteger(source, converter);
//         Assert.assertEquals(new BigInteger("7777"), result);
//     }

//     @Test
//     void shouldConvertNumberTypesToBigInteger() {
//         Long source = 42L;
//         BigInteger result = MapConversions.toBigInteger(source, converter);
//         Assert.assertEquals(BigInteger.valueOf(42L), result);
//     }

//     @Test
//     void shouldHandleMapWithNonStringValue() {
//         Map<String, Object> source = new HashMap<>();
//         // Integer value
//         source.put("value", 123);
//         BigInteger result = MapConversions.toBigInteger(source, converter);
//         Assert.assertEquals(BigInteger.valueOf(123L), result);
//     }

    /* -------------------------------------------------------------
     *  Valid conversions – direct types (parameterized)
     * ------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] Convert {0} ({1}) → {2}")
//     @MethodSource("validDirectConversions")
//     void shouldConvertDirectTypes(Object input, BigInteger expected) {
//         Object[][] data = new Object[][] { // Long → BigInteger
//         { Long.valueOf(9876543210L), new BigInteger("9876543210") }, // Double with whole number → BigInteger (fraction truncated)
//         { Double.valueOf(12345.0), new BigInteger("12345") }, // String containing a valid integer → BigInteger
//         { "-42424242424242424242", new BigInteger("-42424242424242424242") }, // Integer → BigInteger
//         { Integer.valueOf(42), BigInteger.valueOf(42) }, // Short → BigInteger
//         { Short.valueOf((short) -7), BigInteger.valueOf(-7) }, // Byte → BigInteger
//         { Byte.valueOf((byte) 3), BigInteger.valueOf(3) }, // Float with fractional part → BigInteger (truncated toward zero)
//         { Float.valueOf(12.34f), BigInteger.valueOf(12) }, // BigDecimal → BigInteger (fraction discarded)
//         { new BigDecimal("12345.678"), BigInteger.valueOf(12345) }, // AtomicInteger → BigInteger
//         { new AtomicInteger(99), BigInteger.valueOf(99) }, // AtomicLong → BigInteger
//         { new AtomicLong(1234567890123L), new BigInteger("1234567890123") } };
//         for (Object[] pair : data) {
//             Object input = pair[0];
//             BigInteger expected = (BigInteger) pair[1];
//             BigInteger result = MapConversions.toBigInteger(input, converter);
//             Assert.assertEquals("Failed converting " + input, expected, result);
//         }
//     }

    /* -------------------------------------------------------------
     *  Valid conversions – map based (parameterized)
     * ------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] Convert map {0} → {1}")
//     @MethodSource("validMapConversions")
//     void shouldConvertFromMap(Map<String, Object> map, BigInteger expected) {
//         List<Object[]> cases = new ArrayList<>();
//         // key "value" with numeric String
//         cases.add(new Object[] { Map.of("value", "55555555555555555555"), new BigInteger("55555555555555555555") });
//         // key "_v" with numeric Long
//         cases.add(new Object[] { Map.of("_v", 7777777777777777777L), new BigInteger("7777777777777777777") });
//         // map with extra entries – only first matching key is used
//         Map<String, Object> linked = new LinkedHashMap<>();
//         linked.put("value", "123");
//         linked.put("extra", "ignore");
//         cases.add(new Object[] { linked, new BigInteger("123") });
//         for (Object[] pair : cases) {
//             @SuppressWarnings("unchecked")
//             Map<String, Object> map = (Map<String, Object>) pair[0];
//             BigInteger expected = (BigInteger) pair[1];
//             BigInteger result = MapConversions.toBigInteger(map, converter);
//             Assert.assertEquals("Failed converting map " + map, expected, result);
//         }
//     }

    /* -------------------------------------------------------------
     *  Edge cases – null and missing keys
     * ------------------------------------------------------------- */
//     @Test
//     void shouldReturnNullWhenSourceIsNull_EdgeCase() {
//         Assert.assertNull("Null source must return null", MapConversions.toBigInteger(null, converter));
//     }

    /* -------------------------------------------------------------
     *  Null converter argument
     * ------------------------------------------------------------- */
//     @Test
//     void shouldThrowNPEWhenConverterIsNull() {
//         String source = "12345";
//         MapConversions.toBigInteger(source, null);
//     }
}