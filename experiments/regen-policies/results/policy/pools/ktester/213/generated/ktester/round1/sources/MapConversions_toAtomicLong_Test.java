package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicLong;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
// import org.junit.*;

class MapConversions_toAtomicLong_Test {

    private Converter converter;

    /**
     * Simple custom type used to verify that a user‑provided Converter is honoured.
     */
    static final class CustomNumber {

        private final long value;

        CustomNumber(long value) {
            this.value = value;
        }

        long getValue() {
            return value;
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // The library now requires a ConverterOptions instance; we provide an anonymous
        // concrete implementation because the class is abstract.
        converter = new Converter(new ConverterOptions() {
        });
    }

//     @AfterEach
    void teardownAfterEach() {
        converter = null;
    }

//     static Stream<Arguments> nullHandlingProvider() {
//         return Stream.of(new Object[] { null, null, null });
//     }

//     static Stream<Arguments> numericTypesProvider() {
//         return Stream.of(new Object[] { Long.valueOf(123L), 123L, null }, new Object[] { Integer.valueOf(42), 42L, null }, new Object[] { new BigInteger("9876543210"), 9876543210L, null }, new Object[] { Double.valueOf(3.14), 3L, null });
//     }

//     static Stream<Arguments> stringRepresentationsProvider() {
//         return Stream.of(new Object[] { "4567", 4567L, null }, new Object[] { "   89   ", 89L, null }, new Object[] { "-12", -12L, null }, new Object[] { "notANumber", null, NumberFormatException.class }, new Object[] { "", null, NumberFormatException.class }, new Object[] { "   \t\n", null, NumberFormatException.class });
//     }

//     static Stream<Arguments> atomicTypesProvider() {
//         return Stream.of(new Object[] { new AtomicLong(777L), 777L, null }, new Object[] { new AtomicInteger(55), 55L, null });
//     }

//     static Stream<Arguments> mapWithValueKeysProvider() {
//         Map<String, Object> map1 = new HashMap<>();
//         map1.put("value", 321L);
//         Map<String, Object> map2 = new HashMap<>();
//         map2.put("_v", "654");
//         Map<String, Object> map3 = new LinkedHashMap<>();
//         map3.put("value", "-99");
//         map3.put("_v", 100L);
//         Map<String, Object> map4 = new HashMap<>();
//         map4.put("value", "notNumber");
//         return Stream.of(new Object[] { map1, 321L, null }, new Object[] { map2, 654L, null }, new Object[] { map3, -99L, null }, new Object[] { map4, null, NumberFormatException.class });
//     }

//     static Stream<Arguments> unsupportedTypesProvider() {
//         return Stream.of(new Object[] { Boolean.TRUE, null, IllegalArgumentException.class }, new Object[] { new Object(), null, IllegalArgumentException.class });
//     }

    /**
     * Verify conversion from a map that contains the standard "value" key.
     */
//     @Test
//     void toAtomicLong_fromMapWithValueKey_stringNumber() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("value", "123");
//         AtomicLong result = MapConversions.toAtomicLong(map, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(123L, result.get());
//     }

    /**
     * Verify conversion from a map that contains the short "_v" key.
     */
//     @Test
//     void toAtomicLong_fromMapWithVKey_longObject() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("_v", 456L);
//         AtomicLong result = MapConversions.toAtomicLong(map, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(456L, result.get());
//     }

    /**
     * Verify conversion when the source object is already a Long.
     */
//     @Test
//     void toAtomicLong_fromLong() {
//         Long src = 789L;
//         AtomicLong result = MapConversions.toAtomicLong(src, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(789L, result.get());
//     }

    /**
     * Verify conversion when the source object is a numeric String.
     */
//     @Test
//     void toAtomicLong_fromString() {
//         String src = "321";
//         AtomicLong result = MapConversions.toAtomicLong(src, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(321L, result.get());
//     }

    /**
     * Verify conversion when the source object is already an AtomicLong.
     */
//     @Test
//     void toAtomicLong_fromAtomicLong() {
//         AtomicLong src = new AtomicLong(100L);
//         AtomicLong result = MapConversions.toAtomicLong(src, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(100L, result.get());
//     }

    /**
     * Verify that a null source yields a null result.
     */
//     @Test
//     void toAtomicLong_fromNull() {
//         AtomicLong result = MapConversions.toAtomicLong(null, converter);
//         Assert.assertNull(result);
//     }

    // -----------------------------------------------------------------
//     @ParameterizedTest(name = "[{index}] from={0}")
//     @MethodSource("nullHandlingProvider")
//     void toAtomicLong_nullHandling(Object from, Long expectedValue, Class<? extends Throwable> expectedException) {
//         nullHandlingProvider().forEach(params -> {
//             Object from = params[0];
//             Long expected = (Long) params[1];
//             @SuppressWarnings("unchecked")
//             Class<? extends Throwable> expectedEx = (Class<? extends Throwable>) params[2];
//             if (expectedEx != null) {
//                 try {
//                     MapConversions.toAtomicLong(from, converter);
//                     Assert.fail("Expected exception: " + expectedEx.getName());
//                 } catch (Throwable t) {
//                     Assert.assertTrue(expectedEx.isInstance(t));
//                 }
//             } else {
//                 AtomicLong result = MapConversions.toAtomicLong(from, converter);
//                 Assert.assertNull(result);
//             }
//         });
//     }

    // -----------------------------------------------------------------
//     @ParameterizedTest(name = "[{index}] from={0}")
//     @MethodSource("numericTypesProvider")
//     void toAtomicLong_numericTypes(Object from, Long expectedValue, Class<? extends Throwable> expectedException) {
//         numericTypesProvider().forEach(params -> {
//             Object from = params[0];
//             Long expected = (Long) params[1];
//             @SuppressWarnings("unchecked")
//             Class<? extends Throwable> expectedEx = (Class<? extends Throwable>) params[2];
//             if (expectedEx != null) {
//                 try {
//                     MapConversions.toAtomicLong(from, converter);
//                     Assert.fail("Expected exception: " + expectedEx.getName());
//                 } catch (Throwable t) {
//                     Assert.assertTrue(expectedEx.isInstance(t));
//                 }
//             } else {
//                 AtomicLong result = MapConversions.toAtomicLong(from, converter);
//                 Assert.assertNotNull(result);
//                 Assert.assertEquals(expected.longValue(), result.get());
//             }
//         });
//     }

    // -----------------------------------------------------------------
//     @ParameterizedTest(name = "[{index}] from=\"{0}\"")
//     @MethodSource("stringRepresentationsProvider")
//     void toAtomicLong_stringRepresentations(String from, Long expectedValue, Class<? extends Throwable> expectedException) {
//         stringRepresentationsProvider().forEach(params -> {
//             String from = (String) params[0];
//             Long expected = (Long) params[1];
//             @SuppressWarnings("unchecked")
//             Class<? extends Throwable> expectedEx = (Class<? extends Throwable>) params[2];
//             if (expectedEx != null) {
//                 try {
//                     MapConversions.toAtomicLong(from, converter);
//                     Assert.fail("Expected exception: " + expectedEx.getName());
//                 } catch (Throwable t) {
//                     Assert.assertTrue(expectedEx.isInstance(t));
//                 }
//             } else {
//                 AtomicLong result = MapConversions.toAtomicLong(from, converter);
//                 Assert.assertNotNull(result);
//                 Assert.assertEquals(expected.longValue(), result.get());
//             }
//         });
//     }

    // -----------------------------------------------------------------
//     @ParameterizedTest(name = "[{index}] from={0}")
//     @MethodSource("atomicTypesProvider")
//     void toAtomicLong_atomicTypes(Object from, Long expectedValue, Class<? extends Throwable> expectedException) {
//         atomicTypesProvider().forEach(params -> {
//             Object from = params[0];
//             Long expected = (Long) params[1];
//             @SuppressWarnings("unchecked")
//             Class<? extends Throwable> expectedEx = (Class<? extends Throwable>) params[2];
//             if (expectedEx != null) {
//                 try {
//                     MapConversions.toAtomicLong(from, converter);
//                     Assert.fail("Expected exception: " + expectedEx.getName());
//                 } catch (Throwable t) {
//                     Assert.assertTrue(expectedEx.isInstance(t));
//                 }
//             } else {
//                 AtomicLong result = MapConversions.toAtomicLong(from, converter);
//                 Assert.assertNotNull(result);
//                 Assert.assertEquals(expected.longValue(), result.get());
//             }
//         });
//     }

    // -----------------------------------------------------------------
//     @ParameterizedTest(name = "[{index}] map={0}")
//     @MethodSource("mapWithValueKeysProvider")
//     void toAtomicLong_mapWithValueKeys(Map<String, Object> map, Long expectedValue, Class<? extends Throwable> expectedException) {
//         mapWithValueKeysProvider().forEach(params -> {
//             @SuppressWarnings("unchecked")
//             Map<String, Object> map = (Map<String, Object>) params[0];
//             Long expected = (Long) params[1];
//             @SuppressWarnings("unchecked")
//             Class<? extends Throwable> expectedEx = (Class<? extends Throwable>) params[2];
//             if (expectedEx != null) {
//                 try {
//                     MapConversions.toAtomicLong(map, converter);
//                     Assert.fail("Expected exception: " + expectedEx.getName());
//                 } catch (Throwable t) {
//                     Assert.assertTrue(expectedEx.isInstance(t));
//                 }
//             } else {
//                 AtomicLong result = MapConversions.toAtomicLong(map, converter);
//                 Assert.assertNotNull(result);
//                 Assert.assertEquals(expected.longValue(), result.get());
//             }
//         });
//     }

    // -----------------------------------------------------------------
//     @ParameterizedTest(name = "[{index}] from={0}")
//     @MethodSource("unsupportedTypesProvider")
//     void toAtomicLong_unsupportedTypes(Object from, Long expectedValue, Class<? extends Throwable> expectedException) {
//         unsupportedTypesProvider().forEach(params -> {
//             Object from = params[0];
//             @SuppressWarnings("unchecked")
//             Class<? extends Throwable> expectedEx = (Class<? extends Throwable>) params[2];
//             try {
//                 MapConversions.toAtomicLong(from, converter);
//                 Assert.fail("Expected exception: " + expectedEx.getName());
//             } catch (Throwable t) {
//                 Assert.assertTrue(expectedEx.isInstance(t));
//             }
//         });
//     }

    // -----------------------------------------------------------------
//     @Test
//     void toAtomicLong_customConverterUsage() {
//         // Placeholder for a test that would verify custom conversion logic when
//         // the library provides a suitable registration mechanism.
//     }

    // -----------------------------------------------------------------
//     @Test
//     void toAtomicLong_nullConverter() {
//         Long src = 10L;
//         MapConversions.toAtomicLong(src, null);
//     }

    // -----------------------------------------------------------------
//     @Test
//     void toAtomicLong_mapWithoutRecognizedKeys() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("foo", 123L);
//         MapConversions.toAtomicLong(map, converter);
//     }

    // -----------------------------------------------------------------
//     @Test
//     void toAtomicLong_mapWithNullValue() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("value", null);
//         MapConversions.toAtomicLong(map, converter);
//     }

    // -----------------------------------------------------------------
//     @Test
//     void toAtomicLong_unsupportedArrayType() {
//         int[] array = new int[] { 1, 2, 3 };
//         MapConversions.toAtomicLong(array, converter);
//     }

    // -----------------------------------------------------------------
//     @Test
//     void toAtomicLong_converterThrowsRuntimeException() {
//         // Placeholder for a test that would verify exception propagation from a
//         // user‑supplied converter when such a converter can be registered.
//     }

//     @Before
//     public void setUp() {
//         // The library now requires a ConverterOptions instance; we provide an empty
//         // implementation because the class is abstract.
//         converter = new Converter(new ConverterOptions() {
//         });
//     }

//     @After
//     public void tearDown() {
//         converter = null;
//     }
}