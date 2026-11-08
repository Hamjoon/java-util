package com.cedarsoftware.util;

import java.util.Optional;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_setFieldCache_Test {

    /**
     * Simple class used for obtaining {@link java.lang.reflect.Field} objects.
     */
    static class TestClass {

        private int field1;

        public String field2;

        protected double field3;
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean (non‑null) cache before each test.
//         ReflectionUtils.setFieldCache(new HashMap<>());
        // Start each test with a fresh empty cache to avoid cross‑test contamination.
//         ReflectionUtils.setFieldCache(new HashMap<>());
    }

//     @AfterEach
    void teardownAfterEach() {
        // Restore a fresh empty cache so that a failure in one test does not affect the next.
//         ReflectionUtils.setFieldCache(new HashMap<>());
        // Ensure the static field is cleared after each test.
//         ReflectionUtils.setFieldCache(new HashMap<>());
    }

    /**
     * Returns {@code true} if the supplied method returns {@code void}.
     */
//     public static boolean returnsVoid(Method method) {
//         return method.getReturnType().equals(void.class);
//     }

    /**
     * Finds a declared method with the given name and parameter types.
     */
//     public static Optional<Method> findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
//         try {
//             return Optional.of(clazz.getDeclaredMethod(name, parameterTypes));
//         } catch (NoSuchMethodException e) {
//             return Optional.empty();
//         }
//     }

    /**
     * Returns {@code true} if a method with the given signature is present.
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes).isPresent();
    }

    /**
     * Returns {@code true} if the method is {@code abstract}.
     */
//     public static boolean isAbstract(Method method) {
//         return Modifier.isAbstract(method.getModifiers());
//     }

    /**
     * Returns {@code true} if the method is {@code final}.
     */
//     public static boolean isFinal(Method method) {
//         return Modifier.isFinal(method.getModifiers());
//     }

//     public static boolean isPublic(Class<?> clazz) {
//         return Modifier.isPublic(clazz.getModifiers());
//     }

//     public static boolean isPublic(Field field) {
//         return Modifier.isPublic(field.getModifiers());
//     }

//     public static boolean isPublic(Constructor<?> constructor) {
//         return Modifier.isPublic(constructor.getModifiers());
//     }

//     public static boolean isStatic(Field field) {
//         return Modifier.isStatic(field.getModifiers());
//     }

//     public static boolean isStatic(Constructor<?> constructor) {
//         return Modifier.isStatic(constructor.getModifiers());
//     }

    /**
     * Verify that after supplying an empty map the public API still works
     * and returns the correct fields for a class.
     */
//     @Test
//     void testSetFieldCacheWithEmptyMap() {
//         Map<Object, Field> emptyCache = new HashMap<>();
//         ReflectionUtils.setFieldCache(emptyCache);
//         List<Field> fields = ReflectionUtils.getAllDeclaredFields(TestClass.class);
//         // The TestClass declares exactly two fields: field1 and field2.
//         assertEquals(2, fields.size(), "Expected two fields to be discovered");
//         assertTrue(fields.stream().anyMatch(f -> f.getName().equals("field1")));
//         assertTrue(fields.stream().anyMatch(f -> f.getName().equals("field2")));
//     }

    /**
     * Supplying {@code null} replaces the internal cache with {@code null}.
     * Subsequent calls that rely on the cache should therefore throw a
     * {@link NullPointerException}.
     */
//     @Test
//     void testSetFieldCacheWithNullCausesNPEOnSubsequentUse() {
//         ReflectionUtils.setFieldCache(null);
//         assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllDeclaredFields(TestClass.class), "Expected NPE because the internal cache was set to null");
//     }

    /**
     * Helper to read the private static {@code FIELD_NAME_CACHE} field via reflection.
     */
//     @SuppressWarnings("unchecked")
//     private static Map<Object, Field> getInternalCache() throws Exception {
//         Field cacheField = ReflectionUtils.class.getDeclaredField("FIELD_NAME_CACHE");
//         cacheField.setAccessible(true);
//         return (Map<Object, Field>) cacheField.get(null);
//     }

//     @Test
//     @DisplayName("Passing null replaces the internal cache with null")
//     void testSetFieldCacheWithNull() throws Exception {
//         ReflectionUtils.setFieldCache(null);
//         assertNull(getInternalCache(), "FIELD_NAME_CACHE should be null after setting null");
//     }

//     @Test
//     @DisplayName("Map with a single entry is stored and contains the entry")
//     void testSetFieldCacheWithOneEntry() throws Exception {
//         Map<Object, Field> map = new HashMap<>();
//         Object key = new Object();
//         Field value = TestClass.class.getDeclaredField("field1");
//         map.put(key, value);
//         ReflectionUtils.setFieldCache(map);
//         Map<Object, Field> internal = getInternalCache();
//         assertSame(map, internal, "Cache reference should be the same map instance");
//         assertEquals(1, internal.size(), "Cache should contain exactly one entry");
//         assertSame(value, internal.get(key), "Stored field should be identical to the one put in");
//     }

//     @Test
//     @DisplayName("Unmodifiable map is accepted without modification")
//     void testSetFieldCacheWithUnmodifiableMap() throws Exception {
//         Map<Object, Field> mutable = new HashMap<>();
//         mutable.put("k", TestClass.class.getDeclaredField("field2"));
//         Map<Object, Field> unmod = Collections.unmodifiableMap(mutable);
//         ReflectionUtils.setFieldCache(unmod);
//         Map<Object, Field> internal = getInternalCache();
//         assertSame(unmod, internal, "Cache should reference the unmodifiable map");
//         // Verify that the map is indeed unmodifiable by attempting a write (should throw).
//         assertThrows(UnsupportedOperationException.class, () -> internal.put("newKey", TestClass.class.getDeclaredField("field3")));
//     }

//     @Test
//     @DisplayName("Subsequent call overwrites previous cache reference")
//     void testSetFieldCacheReplacesPreviousMap() throws Exception {
//         Map<Object, Field> first = new HashMap<>();
//         first.put("first", TestClass.class.getDeclaredField("field1"));
//         ReflectionUtils.setFieldCache(first);
//         assertSame(first, getInternalCache(), "First map should be stored");
//         Map<Object, Field> second = new HashMap<>();
//         second.put("second", TestClass.class.getDeclaredField("field2"));
//         ReflectionUtils.setFieldCache(second);
//         assertSame(second, getInternalCache(), "Second map should replace the first");
//         assertFalse(getInternalCache().containsKey("first"), "Old entry must no longer be present");
//     }

//     @Test
//     @DisplayName("Map with multiple entries preserves all entries")
//     void testSetFieldCacheWithMultipleEntries() throws Exception {
//         Map<Object, Field> map = new HashMap<>();
//         map.put("k1", TestClass.class.getDeclaredField("field1"));
//         map.put("k2", TestClass.class.getDeclaredField("field2"));
//         map.put("k3", TestClass.class.getDeclaredField("field3"));
//         ReflectionUtils.setFieldCache(map);
//         Map<Object, Field> internal = getInternalCache();
//         assertSame(map, internal, "Cache reference should be the same map");
//         assertEquals(3, internal.size(), "All three entries must be present");
//         assertSame(TestClass.class.getDeclaredField("field1"), internal.get("k1"));
//         assertSame(TestClass.class.getDeclaredField("field2"), internal.get("k2"));
//         assertSame(TestClass.class.getDeclaredField("field3"), internal.get("k3"));
//     }

//     @Test
//     @DisplayName("Map containing a null key is stored unchanged")
//     void testSetFieldCacheWithNullKey() throws Exception {
//         Map<Object, Field> map = new HashMap<>();
//         map.put(null, TestClass.class.getDeclaredField("field1"));
//         ReflectionUtils.setFieldCache(map);
//         Map<Object, Field> internal = getInternalCache();
//         assertSame(map, internal, "Cache reference should be the same map");
//         assertTrue(internal.containsKey(null), "Cache must contain the null key");
//         assertSame(TestClass.class.getDeclaredField("field1"), internal.get(null));
//     }

//     @Test
//     @DisplayName("Map containing a null value is stored unchanged")
//     void testSetFieldCacheWithNullValue() throws Exception {
//         Map<Object, Field> map = new HashMap<>();
//         Object key = new Object();
//         map.put(key, null);
//         ReflectionUtils.setFieldCache(map);
//         Map<Object, Field> internal = getInternalCache();
//         assertSame(map, internal, "Cache reference should be the same map");
//         assertTrue(internal.containsKey(key), "Cache must contain the key");
//         assertNull(internal.get(key), "Value associated with the key should be null");
//     }

//     @Test
//     @DisplayName("ConcurrentHashMap instance is preserved")
//     void testSetFieldCacheWithConcurrentHashMap() throws Exception {
//         ConcurrentHashMap<Object, Field> concurrent = new ConcurrentHashMap<>();
//         concurrent.put("c1", TestClass.class.getDeclaredField("field1"));
//         concurrent.put("c2", TestClass.class.getDeclaredField("field2"));
//         ReflectionUtils.setFieldCache(concurrent);
//         Map<Object, Field> internal = getInternalCache();
//         assertSame(concurrent, internal, "Cache should reference the same ConcurrentHashMap instance");
//         assertEquals(2, internal.size());
//     }

//     @Test
//     @DisplayName("Cache holds live reference – modifications after set are visible")
//     void testSetFieldCacheLiveReference() throws Exception {
//         Map<Object, Field> mutable = new HashMap<>();
//         mutable.put("initial", TestClass.class.getDeclaredField("field1"));
//         ReflectionUtils.setFieldCache(mutable);
//         // Modify the original map after the call.
//         mutable.put("addedLater", TestClass.class.getDeclaredField("field2"));
//         Map<Object, Field> internal = getInternalCache();
//         assertSame(mutable, internal, "Cache should still reference the original mutable map");
//         assertEquals(2, internal.size(), "New entry added after setFieldCache must be observable");
//         assertSame(TestClass.class.getDeclaredField("field2"), internal.get("addedLater"));
//     }

//     @Test
//     @DisplayName("Setting cache with the same instance does not change identity")
//     void testSetFieldCacheSameInstanceReuse() throws Exception {
//         Map<Object, Field> map = new HashMap<>();
//         map.put("once", TestClass.class.getDeclaredField("field1"));
//         ReflectionUtils.setFieldCache(map);
//         Map<Object, Field> firstRef = getInternalCache();
//         // Call setFieldCache again with the exact same map instance.
//         ReflectionUtils.setFieldCache(map);
//         Map<Object, Field> secondRef = getInternalCache();
//         assertSame(firstRef, secondRef, "Cache reference should remain the same instance");
//         assertEquals(1, secondRef.size());
//     }
}