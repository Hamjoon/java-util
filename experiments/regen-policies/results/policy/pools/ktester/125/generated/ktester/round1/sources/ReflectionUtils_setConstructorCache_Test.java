package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setConstructorCache_Test {

    /**
     * Simple class used only for reflection tests.
     */
    static class Dummy {

        public Dummy() {
        }
    }

    /**
     * Helper classes used by several test cases.
     */
    static class ReflectionUtilsTestHelper {

        /**
         * A custom mutable map implementation – just extends {@link HashMap}.
         */
        static class CustomMap<K, V> extends HashMap<K, V> {
        }

        /**
         * A simple LRU‑style cache based on {@link LinkedHashMap}.
         */
        static class CustomLRUCache<K, V> extends LinkedHashMap<K, V> {

            CustomLRUCache(int maxSize) {
                super(maxSize, 0.75f, true);
            }
        }

        /**
         * Sample class whose constructor will be stored in a pre‑populated map.
         */
        static class SampleClass {

            public SampleClass() {
            }
        }
    }

    /**
     * A custom mutable map implementation – just extends {@link HashMap}.
     */
    static class CustomMap<K, V> extends HashMap<K, V> {
    }

    /**
     * A simple LRU‑style cache based on {@link LinkedHashMap}.
     */
    static class CustomLRUCache<K, V> extends LinkedHashMap<K, V> {

        CustomLRUCache(int maxSize) {
            super(maxSize, 0.75f, true);
        }
    }

    /**
     * Sample class whose constructor will be stored in a pre‑populated map.
     */
    static class SampleClass {

        public SampleClass() {
        }
    }

    /**
     * A custom mutable map implementation – just extends {@link HashMap}.
     */
    static class CustomMap<K, V> extends HashMap<K, V> {
    }

    /**
     * A simple LRU‑style cache based on {@link LinkedHashMap}.
     */
    static class CustomLRUCache<K, V> extends LinkedHashMap<K, V> {

        CustomLRUCache(int maxSize) {
            super(maxSize, 0.75f, true);
        }
    }

    /**
     * Sample class whose constructor will be stored in a pre‑populated map.
     */
    static class SampleClass {

        public SampleClass() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the cache is in a known (empty) state before each test.
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the cache so that other test classes are not affected.
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    /**
     * Utility to read the private static {@code CONSTRUCTOR_CACHE} field via reflection.
     */
    private static Map<?, ?> getInternalCache() throws ReflectiveOperationException {
        Field field = ReflectionUtils.class.getDeclaredField("CONSTRUCTOR_CACHE");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> cache = (Map<?, ?>) field.get(null);
        return cache;
    }

    /**
     * Verifies that {@code setConstructorCache} accepts a user‑provided map and that
     * subsequent reflective calls continue to work (i.e. they return a constructor
     * for the supplied class).  The test does not inspect the private cache directly,
     * but checks the observable behaviour of {@link ReflectionUtils#getConstructor}.
     */
    @Test
    void testSetConstructorCacheWithCustomMap() throws NoSuchMethodException {
        // Obtain the real constructor for Dummy via standard reflection.
        Constructor<Dummy> realConstructor = Dummy.class.getDeclaredConstructor();
        // Prepare a custom cache containing the constructor we just retrieved.
        Map<Object, Constructor<?>> customCache = new HashMap<>();
        customCache.put(Dummy.class, realConstructor);
        // Install the custom cache.
        ReflectionUtils.setConstructorCache(customCache);
        // Ask ReflectionUtils for the constructor of Dummy.  The method should succeed
        // and return a constructor whose declaring class is Dummy.
        Constructor<?> cachedConstructor = ReflectionUtils.getConstructor(Dummy.class);
        assertNotNull(cachedConstructor, "ReflectionUtils should return a constructor after the cache is set");
        assertEquals(Dummy.class, cachedConstructor.getDeclaringClass(), "The returned constructor must belong to Dummy");
        // The instance we placed in the map should be the same object that the utility returns.
        // This confirms that the cache is being consulted.
        assertSame(realConstructor, cachedConstructor, "The constructor returned by ReflectionUtils should be the one stored in the custom cache");
    }

    /**
     * Verifies that passing {@code null} to {@code setConstructorCache} does not cause
     * subsequent reflective operations to fail.  The method should simply replace the
     * internal cache with {@code null}; later calls must behave as if a fresh empty
     * cache were in place.
     */
    @Test
    void testSetConstructorCacheWithNull() throws NoSuchMethodException {
        // Replace the internal cache with null.
        ReflectionUtils.setConstructorCache(null);
        // The utility should still be able to locate a constructor for a normal class.
        Constructor<?> ctor = ReflectionUtils.getConstructor(Dummy.class);
        assertNotNull(ctor, "ReflectionUtils must still locate constructors after a null cache is set");
        assertEquals(Dummy.class, ctor.getDeclaringClass(), "The constructor returned after a null cache should belong to Dummy");
    }

    /**
     * Simple sanity check that an empty map does not interfere with normal operation.
     */
    @Test
    void testSetConstructorCacheWithEmptyMap() throws NoSuchMethodException {
        ReflectionUtils.setConstructorCache(new HashMap<>());
        Constructor<?> ctor = ReflectionUtils.getConstructor(Dummy.class);
        assertNotNull(ctor, "ReflectionUtils should still locate constructors when the cache is empty");
        assertEquals(Dummy.class, ctor.getDeclaringClass(), "The constructor returned should belong to Dummy");
    }

    @Test
    @DisplayName("ReflectionUtils.setConstructorCache – regular mutable HashMap")
    void testSetConstructorCacheWithHashMap() throws Exception {
        Map<Object, Constructor<?>> hashMap = new HashMap<>();
        ReflectionUtils.setConstructorCache(hashMap);
        Map<?, ?> internal = getInternalCache();
        assertSame(hashMap, internal, "CONSTRUCTOR_CACHE should reference the supplied HashMap instance");
    }

    @Test
    @DisplayName("ReflectionUtils.setConstructorCache – successive invocations replace previous cache")
    void testSetConstructorCacheSuccessiveMaps() throws Exception {
        Map<Object, Constructor<?>> firstMap = new HashMap<>();
        ReflectionUtils.setConstructorCache(firstMap);
        assertSame(firstMap, getInternalCache(), "First map should be stored");
        Map<Object, Constructor<?>> secondMap = new LinkedHashMap<>();
        ReflectionUtils.setConstructorCache(secondMap);
        assertSame(secondMap, getInternalCache(), "Second map should replace the first one");
    }

    @Test
    @DisplayName("ReflectionUtils.setConstructorCache – immutable (unmodifiable) map")
    void testSetConstructorCacheWithImmutableMap() throws Exception {
        Map<Object, Constructor<?>> immutable = Collections.unmodifiableMap(new HashMap<>());
        ReflectionUtils.setConstructorCache(immutable);
        Map<?, ?> internal = getInternalCache();
        assertSame(immutable, internal, "CONSTRUCTOR_CACHE should reference the unmodifiable map instance");
    }

    @Test
    @DisplayName("ReflectionUtils.setConstructorCache – ConcurrentHashMap")
    void testSetConstructorCacheWithConcurrentHashMap() throws Exception {
        ConcurrentHashMap<Object, Constructor<?>> concurrent = new ConcurrentHashMap<>();
        ReflectionUtils.setConstructorCache(concurrent);
        Map<?, ?> internal = getInternalCache();
        assertSame(concurrent, internal, "CONSTRUCTOR_CACHE should reference the supplied ConcurrentHashMap instance");
    }

    @Test
    @DisplayName("ReflectionUtils.setConstructorCache – map pre‑populated with entries")
    void testSetConstructorCacheWithPrepopulatedEntries() throws Exception {
        Map<Object, Constructor<?>> map = new HashMap<>();
        Constructor<?> ctor = ReflectionUtilsTestHelper.SampleClass.class.getDeclaredConstructor();
        map.put("key", ctor);
        ReflectionUtils.setConstructorCache(map);
        Map<?, ?> internal = getInternalCache();
        assertSame(map, internal, "CONSTRUCTOR_CACHE should reference the supplied map with entries");
        assertEquals(ctor, internal.get("key"), "The entry stored in the cache must be the same as the one supplied");
    }

    @Test
    @DisplayName("ReflectionUtils.setConstructorCache – dynamic Proxy Map")
    void testSetConstructorCacheWithProxyMap() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Object, Constructor<?>> proxy = (Map<Object, Constructor<?>>) Proxy.newProxyInstance(Map.class.getClassLoader(), new Class<?>[] { Map.class }, (proxyObj, method, args) -> {
            // Minimal behaviour – size() and isEmpty() are enough for the test.
            if ("size".equals(method.getName()))
                return 0;
            if ("isEmpty".equals(method.getName()))
                return true;
            return null;
        });
        ReflectionUtils.setConstructorCache(proxy);
        Map<?, ?> internal = getInternalCache();
        assertSame(proxy, internal, "CONSTRUCTOR_CACHE should reference the proxy map instance");
    }

    @Test
    @DisplayName("ReflectionUtils.setConstructorCache – raw map with mismatched generics")
    void testSetConstructorCacheWithWrongGenerics() throws Exception {
        // Create a raw map (String → String) and cast it to the expected type.
        @SuppressWarnings("unchecked")
        Map<Object, Constructor<?>> raw = (Map) Collections.singletonMap("stringKey", "stringValue");
        ReflectionUtils.setConstructorCache(raw);
        Map<?, ?> internal = getInternalCache();
        assertSame(raw, internal, "CONSTRUCTOR_CACHE should reference the raw map despite generic mismatch");
    }

    @Test
    @DisplayName("ReflectionUtils.setConstructorCache – synchronized map wrapper")
    void testSetConstructorCacheWithSynchronizedMap() throws Exception {
        Map<Object, Constructor<?>> syncMap = Collections.synchronizedMap(new HashMap<>());
        ReflectionUtils.setConstructorCache(syncMap);
        Map<?, ?> internal = getInternalCache();
        assertSame(syncMap, internal, "CONSTRUCTOR_CACHE should reference the synchronized map instance");
    }

    @Test
    @DisplayName("ReflectionUtils.setConstructorCache – WeakHashMap")
    void testSetConstructorCacheWithWeakHashMap() throws Exception {
        WeakHashMap<Object, Constructor<?>> weakMap = new WeakHashMap<>();
        ReflectionUtils.setConstructorCache(weakMap);
        Map<?, ?> internal = getInternalCache();
        assertSame(weakMap, internal, "CONSTRUCTOR_CACHE should reference the supplied WeakHashMap instance");
    }

    @Test
    @DisplayName("ReflectionUtils.setConstructorCache – custom LRUCache implementation")
    void testSetConstructorCacheWithCustomLRUCache() throws Exception {
        ReflectionUtilsTestHelper.CustomLRUCache<Object, Constructor<?>> customLRU = new ReflectionUtilsTestHelper.CustomLRUCache<>(10);
        ReflectionUtils.setConstructorCache(customLRU);
        Map<?, ?> internal = getInternalCache();
        assertSame(customLRU, internal, "CONSTRUCTOR_CACHE should reference the custom LRUCache instance");
    }
}
