package com.cedarsoftware.util;

import java.lang.reflect.Method;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_setFieldCache_Test {

    /**
     * Simple class used by the tests – it has instance, static and extra fields.
     */
    static class TestClass {

        private int field1;

        private String field2;

        // used for custom‑key tests
        private int value;

        // used for static‑field tests
        private static int staticValue;
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Ensure the default cache is restored before each test so that
        // tests are independent of one another.
        ReflectionUtils.setFieldCache(null);
        ReflectionUtils.setFieldCache(null);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Clean up by resetting the cache to its default state.
        ReflectionUtils.setFieldCache(null);
        ReflectionUtils.setFieldCache(null);
    }

    /**
     * Returns {@code true} if the supplied {@link Method} has a {@code void}
     * return type.
     *
     * @param method the method to inspect; must not be {@code null}
     * @return {@code true} if the method returns {@code void}, otherwise {@code false}
     */
    public static boolean returnsVoid(Method method) {
        if (method == null) {
            throw new IllegalArgumentException("Method must not be null");
        }
        return void.class.equals(method.getReturnType());
    }

    /**
     * Verify that {@code setFieldCache} accepts a custom map and that the
     * subsequent reflective operations still work as expected.
     */
//     @Test
//     void testSetFieldCacheWithCustomMap() {
//         // GIVEN – a user supplied map (it can be any Map implementation)
//         Map<Object, Field> customCache = new HashMap<>();
//         // WHEN – we replace the internal cache with the custom map
//         ReflectionUtils.setFieldCache(customCache);
//         // THEN – normal public API should still locate fields correctly.
//         // We use {@code getAllDeclaredFieldsMap} which ultimately relies on the
//         // field‑cache but its contract is public.
//         Map<String, Field> fieldMap = ReflectionUtils.getAllDeclaredFieldsMap(TestClass.class);
//         assertNotNull(fieldMap, "Field map must not be null");
//         assertEquals(2, fieldMap.size(), "TestClass should expose exactly two fields");
//         assertTrue(fieldMap.containsKey("field1"), "field1 must be present");
//         assertTrue(fieldMap.containsKey("field2"), "field2 must be present");
//     }

    /**
     * Verify that calling {@code setFieldCache} more than once replaces the
     * previous cache.  The test replaces the cache with an empty map and then
     * checks that the public API still works – the cache is rebuilt lazily.
     */
//     @Test
//     void testSetFieldCacheReplacesPreviousCache() {
//         // First cache – contains a dummy entry (the content does not matter)
//         Map<Object, Field> firstCache = new HashMap<>();
//         firstCache.put("dummyKey", null);
//         ReflectionUtils.setFieldCache(firstCache);
//         // Second cache – empty map, should overwrite the first one
//         Map<Object, Field> secondCache = new HashMap<>();
//         ReflectionUtils.setFieldCache(secondCache);
//         // The public API must still be able to retrieve fields for TestClass
//         Map<String, Field> fields = ReflectionUtils.getAllDeclaredFieldsMap(TestClass.class);
//         assertNotNull(fields, "Fields map must not be null");
//         assertEquals(2, fields.size(), "After replacing the cache, fields must still be discoverable");
//     }

    /**
     * Ensure that passing {@code null} to {@code setFieldCache} does not break
     * subsequent reflective calls.  The method internally swaps the reference
     * with a thread‑safe empty map, so the behaviour should be equivalent to an
     * empty cache.
     */
//     @Test
//     void testSetFieldCacheWithNull() {
//         // WHEN – null is supplied
//         ReflectionUtils.setFieldCache(null);
//         // THEN – the utility must still function normally.
//         Map<String, Field> fieldMap = ReflectionUtils.getAllDeclaredFieldsMap(TestClass.class);
//         assertNotNull(fieldMap, "Field map must not be null");
//         assertEquals(2, fieldMap.size(), "Exactly two fields must be returned");
//     }

    /**
     * Returns the internal {@code FIELD_NAME_CACHE} map via reflection.
     *
     * @return the map currently stored inside {@link ReflectionUtils}
     * @throws Exception if reflection fails
     */
//     @SuppressWarnings("unchecked")
//     private static Map<Object, Field> getInternalCacheMap() throws Exception {
//         Field cacheRefField = ReflectionUtils.class.getDeclaredField("FIELD_NAME_CACHE");
//         cacheRefField.setAccessible(true);
//         AtomicReference<Map<?, Field>> ref = (AtomicReference<Map<?, Field>>) cacheRefField.get(null);
//         return (Map<Object, Field>) ref.get();
//     }

//     @Test
//     void basicFunctionality_nonEmptyMapIsStored() throws Exception {
//         // GIVEN – a map containing a single entry
//         Field valueField = TestClass.class.getDeclaredField("value");
//         Map<Object, Field> customCache = new HashMap<>();
//         customCache.put("key1", valueField);
//         // WHEN – the cache is replaced
//         ReflectionUtils.setFieldCache(customCache);
//         // THEN – the internal cache contains exactly the same entry
//         Map<Object, Field> internal = getInternalCacheMap();
//         assertNotNull(internal, "Internal cache must not be null");
//         assertEquals(1, internal.size(), "Internal cache should contain one entry");
//         assertSame(valueField, internal.get("key1"), "Stored field must be the same instance");
//     }

//     @Test
//     void basicFunctionality_emptyMapResultsInEmptyCache() throws Exception {
//         // GIVEN – an empty map
//         Map<Object, Field> empty = new HashMap<>();
//         // WHEN – set as the field cache
//         ReflectionUtils.setFieldCache(empty);
//         // THEN – internal cache is empty
//         Map<Object, Field> internal = getInternalCacheMap();
//         assertNotNull(internal, "Internal cache must not be null");
//         assertTrue(internal.isEmpty(), "Internal cache should be empty");
//     }

//     @Test
//     void basicFunctionality_nullIsHandledGracefully() throws Exception {
//         // WHEN – null is supplied (the implementation should replace with an empty thread‑safe map)
//         ReflectionUtils.setFieldCache(null);
//         // THEN – internal cache is non‑null and empty
//         Map<Object, Field> internal = getInternalCacheMap();
//         assertNotNull(internal, "Internal cache must not be null after null input");
//         assertTrue(internal.isEmpty(), "Internal cache should be empty after null input");
//     }

//     @Test
//     void mapMutationPropagation_changesAreVisibleInCache() throws Exception {
//         // GIVEN – a mutable map with one entry
//         Field valueField = TestClass.class.getDeclaredField("value");
//         Map<Object, Field> mutableMap = new HashMap<>();
//         mutableMap.put("k1", valueField);
//         ReflectionUtils.setFieldCache(mutableMap);
//         // Verify initial size
//         Map<Object, Field> internal = getInternalCacheMap();
//         assertEquals(1, internal.size(), "Initial cache size should be 1");
//         // WHEN – we mutate the original map
//         Field staticField = TestClass.class.getDeclaredField("staticValue");
//         mutableMap.put("k2", staticField);
//         // THEN – internal cache reflects the mutation (same underlying map)
//         assertEquals(2, internal.size(), "Cache size should reflect mutation of original map");
//         assertSame(staticField, internal.get("k2"), "Mutated entry must be visible in cache");
//     }

//     @Test
//     void threadSafeMapReuse_concurrentHashMapIsUsedDirectly() throws Exception {
//         // GIVEN – a ConcurrentHashMap (already thread‑safe)
//         Field valueField = TestClass.class.getDeclaredField("value");
//         ConcurrentHashMap<Object, Field> concurrentMap = new ConcurrentHashMap<>();
//         concurrentMap.put("c1", valueField);
//         // WHEN – we set it as the cache
//         ReflectionUtils.setFieldCache(concurrentMap);
//         // THEN – the internal reference is exactly the same instance
//         Map<Object, Field> internal = getInternalCacheMap();
//         assertSame(concurrentMap, internal, "Internal cache should be the same instance as the supplied ConcurrentHashMap");
//     }

//     @Test
//     void nonStringKey_isStoredUnchanged() throws Exception {
//         // GIVEN – a map with a non‑String key (Integer)
//         Field valueField = TestClass.class.getDeclaredField("value");
//         Map<Object, Field> mixedKeyMap = new HashMap<>();
//         mixedKeyMap.put(42, valueField);
//         // WHEN – set as cache
//         ReflectionUtils.setFieldCache(mixedKeyMap);
//         // THEN – internal cache contains the Integer key mapping to the correct field
//         Map<Object, Field> internal = getInternalCacheMap();
//         assertTrue(internal.containsKey(42), "Cache must contain the Integer key 42");
//         assertSame(valueField, internal.get(42), "Field associated with key 42 must be the expected one");
//     }

//     @Test
//     void staticField_isStoredWithoutFiltering() throws Exception {
//         // GIVEN – a map containing a static field
//         Field staticField = TestClass.class.getDeclaredField("staticValue");
//         Map<Object, Field> staticMap = new HashMap<>();
//         staticMap.put("staticKey", staticField);
//         // WHEN – set as cache
//         ReflectionUtils.setFieldCache(staticMap);
//         // THEN – the stored field retains its STATIC modifier
//         Map<Object, Field> internal = getInternalCacheMap();
//         Field cached = internal.get("staticKey");
//         assertNotNull(cached, "Static field must be present in cache");
//         assertTrue(Modifier.isStatic(cached.getModifiers()), "Cached field must be marked as static");
//     }
}