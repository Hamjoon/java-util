package com.cedarsoftware.util.convert;

import java.util.*;
import org.mockito.Mock;
import org.mockito.Mockito;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class MapConversions_initMap_Test {

    /**
     * Shared Converter instance used for all tests.
     */
    private static Converter converter;

//     @BeforeAll
    static void setupBeforeAll() {
        converter = null;
    }

//     @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

//     private static Stream<Arguments> basicBehaviorProvider() {
//         // The converter argument is not used by initMap for these cases, so we pass null.
//         return // Non‑null primitive wrapper
//         Stream.// Non‑null primitive wrapper
//         of(// Null source
//         Arguments.of("Primitive wrapper Integer", 5, null, 5), // Collection with null converter
//         Arguments.of("Null source", null, null, null), // Plain Object (reference comparison)
//         Arguments.of("List source, null converter", Arrays.asList("a", "b"), null, Arrays.asList("a", "b")), Arguments.of("Plain Object", new Object(), null, new Object()));
//     }

//     private static Stream<Arguments> primitiveArrayProvider() {
//         return Stream.of(Arguments.of("int[] array", new int[] { 1, 2, 3 }), Arguments.of("String[] array", new String[] { "x", "y" }));
//     }

//     private static Stream<Arguments> emptyCollectionProvider() {
//         return Stream.of(Arguments.of("Empty List", Collections.emptyList()), Arguments.of("Empty Map", Collections.emptyMap()));
//     }

//     private static Stream<Arguments> specialNumericProvider() {
//         return Stream.of(Arguments.of("Double.NaN", Double.NaN), Arguments.of("Float.POSITIVE_INFINITY", Float.POSITIVE_INFINITY));
//     }

//     @Test
//     void initMap_WithStringValue_ShouldContainValueUnderKeyV() {
//         String source = "sampleString";
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull("Resulting map must not be null", result);
//         assertEquals("Map should contain exactly one entry", 1, result.size());
//         assertTrue("Map must contain the key \"_v\"", result.containsKey(MapConversions.V));
//         assertEquals("The value associated with \"_v\" must be the original object", source, result.get(MapConversions.V));
//         assertTrue("Returned map must be a LinkedHashMap", result instanceof LinkedHashMap);
//     }

//     @Test
//     void initMap_WithIntegerValue_ShouldContainValueUnderKeyV() {
//         Integer source = 42;
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertEquals(source, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

//     @Test
//     void initMap_WithNullValue_ShouldContainNullUnderKeyV() {
//         Object source = null;
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertTrue(result.containsKey(MapConversions.V));
//         assertNull("The value for key \"_v\" should be null when the source is null", result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

    // ---------- Basic behavior (primitive wrapper, null, collection, plain object) ----------
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("basicBehaviorProvider")
//     void initMap_BasicBehavior(String description, Object source, Converter conv, Object expected) {
//         Map<String, ?> result = MapConversions.initMap(source, conv);
//         assertNotNull(result);
//         assertEquals(1, result.size(), "Map must contain exactly one entry");
//         assertTrue(result.containsKey(MapConversions.V), "Map must contain the key \"_v\"");
//         if (expected instanceof Double && ((Double) expected).isNaN()) {
//             assertTrue(result.get(MapConversions.V) instanceof Double);
//             assertTrue(Double.isNaN((Double) result.get(MapConversions.V)));
//         } else if (expected instanceof Float && ((Float) expected).isInfinite()) {
//             assertEquals(expected, result.get(MapConversions.V));
//         } else {
//             // For objects we expect the exact same reference to be stored.
//             assertSame(expected, result.get(MapConversions.V), "Stored reference must be the same as the source");
//         }
//         assertTrue(result instanceof LinkedHashMap, "Returned map must be a LinkedHashMap");
//     }

    // ---------- Primitive array inputs ----------
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("primitiveArrayProvider")
//     void initMap_PrimitiveArray(String description, Object array) {
//         Map<String, ?> result = MapConversions.initMap(array, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertSame(array, result.get(MapConversions.V), "Array reference must be stored unchanged");
//         assertTrue(result instanceof LinkedHashMap);
//     }

    // ---------- Empty collection inputs ----------
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("emptyCollectionProvider")
//     void initMap_EmptyCollections(String description, Object empty) {
//         Map<String, ?> result = MapConversions.initMap(empty, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertSame(empty, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

    // ---------- Non‑empty map input ----------
//     @Test
//     void initMap_NonEmptyMap_ShouldStoreReference() {
//         Map<String, String> source = Collections.singletonMap("key", "value");
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertSame(source, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

    // ---------- Special numeric values ----------
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("specialNumericProvider")
//     void initMap_SpecialNumericValues(String description, Object value) {
//         Map<String, ?> result = MapConversions.initMap(value, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         Object stored = result.get(MapConversions.V);
//         if (value instanceof Double) {
//             assertTrue(stored instanceof Double);
//             assertTrue(Double.isNaN((Double) stored));
//         } else if (value instanceof Float) {
//             assertEquals(Float.POSITIVE_INFINITY, stored);
//         } else {
//             fail("Unexpected special numeric type");
//         }
//         assertTrue(result instanceof LinkedHashMap);
//     }

    // ---------- Map mutability verification ----------
//     @Test
//     void initMap_ReturnedMapIsMutable() {
//         Map<String, ?> result = MapConversions.initMap("initial", converter);
//         assertTrue(result instanceof LinkedHashMap);
//         @SuppressWarnings("unchecked")
//         Map<String, Object> mutable = (Map<String, Object>) result;
//         mutable.put("extra", "value");
//         assertEquals(2, mutable.size());
//         assertEquals("value", mutable.get("extra"));
//     }

    // ---------- Custom Converter subclass that throws if used ----------
//     @Test
//     void initMap_WithHostileConverter_SubclassDoesNotInteract() {
//         // Mock a Converter that throws on any method call.
//         Converter hostile = Mockito.mock(Converter.class, invocation -> {
//             throw new RuntimeException("Converter should not be invoked");
//         });
//         String source = "test";
//         Map<String, ?> result = MapConversions.initMap(source, hostile);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertEquals(source, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

    // ---------- Thread object source ----------
//     @Test
//     void initMap_WithThreadObject_ShouldStoreReference() {
//         // not started
//         Thread thread = new Thread();
//         Map<String, ?> result = MapConversions.initMap(thread, null);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertSame(thread, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

    // ---------- Custom Converter mock that should not be interacted with ----------
//     @Test
//     void initMap_WithHostileConverter_MockDoesNotInteract() {
//         // A mock that will throw if any method other than toString is called.
//         Converter hostile = Mockito.mock(Converter.class, invocation -> {
//             throw new RuntimeException("Converter should not be invoked");
//         });
//         String source = "test";
//         Map<String, ?> result = MapConversions.initMap(source, hostile);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertEquals(source, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

//     @Test
//     public void initMap_WithList_ShouldStoreReference() {
//         List<String> source = Arrays.asList("a", "b");
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertSame(source, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

//     @Test
//     public void initMap_WithPlainObject_ShouldStoreReference() {
//         Object source = new Object();
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertSame(source, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

//     @Test
//     public void initMap_WithPrimitiveIntArray_ShouldStoreReference() {
//         int[] source = new int[] { 1, 2, 3 };
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertSame(source, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

//     @Test
//     public void initMap_WithStringArray_ShouldStoreReference() {
//         String[] source = new String[] { "x", "y" };
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertSame(source, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

//     @Test
//     public void initMap_WithEmptyList_ShouldStoreReference() {
//         List<?> source = Collections.emptyList();
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertSame(source, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

//     @Test
//     public void initMap_WithEmptyMap_ShouldStoreReference() {
//         Map<?, ?> source = Collections.emptyMap();
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         assertSame(source, result.get(MapConversions.V));
//         assertTrue(result instanceof LinkedHashMap);
//     }

//     @Test
//     public void initMap_WithSpecialDoubleNaN_ShouldPreserveNaN() {
//         Double source = Double.NaN;
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         Object stored = result.get(MapConversions.V);
//         assertTrue(stored instanceof Double);
//         assertTrue(Double.isNaN((Double) stored));
//         assertTrue(result instanceof LinkedHashMap);
//     }

//     @Test
//     public void initMap_WithSpecialFloatInfinity_ShouldPreserveInfinity() {
//         Float source = Float.POSITIVE_INFINITY;
//         Map<String, ?> result = MapConversions.initMap(source, converter);
//         assertNotNull(result);
//         assertEquals(1, result.size());
//         Object stored = result.get(MapConversions.V);
//         assertEquals(Float.POSITIVE_INFINITY, stored);
//         assertTrue(result instanceof LinkedHashMap);
//     }
}