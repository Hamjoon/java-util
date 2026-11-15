package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

class ThreadedLRUCacheStrategy_hashCode_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    class NegHash {

        @Override
        public int hashCode() {
            return -1;
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad key");
        }
    }

    class BadValue {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad value");
        }
    }

    class NegHash {

        @Override
        public int hashCode() {
            return -1;
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad key");
        }
    }

    class BadValue {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad value");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Capacity and cleanup delay are arbitrary but valid.
        cache = new ThreadedLRUCacheStrategy<>(10, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a clean cache.
        if (cache != null) {
            cache.clear();
        }
    }

    private static Stream<Arguments> simpleHashCodeProvider() {
        return Stream.of(// single entry non‑null key/value
        Arguments.of("a", "b", 4066), // entry with null key
        Arguments.of(null, "v", 1079), // entry with null value
        Arguments.of("k", null, 4278), // both key and value null
        Arguments.of(null, null, 961));
    }

    /**
     * Utility to set a private field via reflection.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void hashCode_EmptyCache_ReturnsOne() {
        // The implementation starts with hashCode = 1 and adds nothing for an empty cache.
        assertEquals(1, cache.hashCode(), "hashCode of an empty ThreadedLRUCacheStrategy should be 1");
    }

    @Test
    void hashCode_WithMultipleEntries_MatchesManualCalculation() {
        // Populate the cache with several entries, including a null key/value.
        cache.put("alpha", 10);
        cache.put("beta", 20);
        cache.put(null, null);
        cache.put("gamma", null);
        cache.put(null, 30);
        // Manual calculation using the same algorithm as the implementation.
        int expected = 1;
        for (Map.Entry<String, Integer> e : cache.entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();
            expected = 31 * expected + (key == null ? 0 : key.hashCode());
            expected = 31 * expected + (value == null ? 0 : value.hashCode());
        }
        assertEquals(expected, cache.hashCode(), "hashCode should reflect the contents of the cache");
    }

    @Test
    void hashCode_ChangesAfterRemoval() {
        cache.put("one", 1);
        cache.put("two", 2);
        int hashBefore = cache.hashCode();
        // Remove one entry and recompute expected hash.
        cache.remove("one");
        int expected = 1;
        for (Map.Entry<String, Integer> e : cache.entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();
            expected = 31 * expected + (key == null ? 0 : key.hashCode());
            expected = 31 * expected + (value == null ? 0 : value.hashCode());
        }
        assertEquals(expected, cache.hashCode(), "hashCode after removal should be recomputed correctly");
        assertNotEquals(hashBefore, cache.hashCode(), "hashCode should change when the cache contents change");
    }

    @ParameterizedTest(name = "hashCode with key={0}, value={1} should be {2}")
    @MethodSource("simpleHashCodeProvider")
    void hashCode_SingleEntry_VariousScenarios(String key, String value, int expected) {
        cache.put(key, value);
        assertEquals(expected, cache.hashCode(), "Computed hashCode does not match the expected manual calculation");
    }

    @Test
    void hashCode_MultipleEntriesWithMixedNulls() {
        // order matters: first ("x",null) then (null,"y")
        cache.put("x", null);
        cache.put(null, "y");
        assertEquals(4_498_562, cache.hashCode(), "Hash code for mixed null entries does not match expected value");
    }

    @Test
    void hashCode_ThreeNonNullEntries_OverflowBehaviour() {
        cache.put("a", "b");
        cache.put("c", "d");
        cache.put("e", "f");
        assertEquals(-536_882_268, cache.hashCode(), "Hash code overflow should wrap according to 32‑bit int arithmetic");
    }

    @Test
    void hashCode_DifferentObjectsIdenticalKeyHashCodes() {
        // "Aa".hashCode() == "BB".hashCode() == 2112
        cache.put("Aa", 1);
        cache.put("BB", 2);
        assertEquals(63_954_724, cache.hashCode(), "Even when keys share the same hashCode, values must influence the final result");
    }

    @Test
    void hashCode_AfterRemovalOfFirstEntry() {
        cache.put("k1", 1);
        cache.put("k2", 2);
        cache.remove("k1");
        assertEquals(109_046, cache.hashCode(), "Hash after removal should reflect only the remaining entry");
    }

    @Test
    void hashCode_CustomObjectsWithNegativeHashCode() {
        // Objects whose hashCode() returns -1
        class NegHash {

            @Override
            public int hashCode() {
                return -1;
            }
        }
        NegHash key = new NegHash();
        NegHash value = new NegHash();
        // Bypass the public put() to avoid ConcurrentMap using key.hashCode() during insertion.
        // Directly insert a Node instance via reflection.
        try {
            // Obtain the private cache field.
            Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentMap<Object, Object> internalMap = (ConcurrentMap<Object, Object>) cacheField.get(cache);
            // Create a Node instance via reflection.
            Class<?> nodeClass = Class.forName("com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy$Node");
            java.lang.reflect.Constructor<?> ctor = nodeClass.getDeclaredConstructor(Object.class, Object.class);
            ctor.setAccessible(true);
            Object node = ctor.newInstance(key, value);
            // Insert the node directly.
            internalMap.put(key, node);
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        assertEquals(929, cache.hashCode(), "Custom objects with negative hashCode should be handled correctly");
    }

    @Test
    void hashCode_NullInternalCache_ThrowsNPE() throws Exception {
        // Set the private 'cache' field to null.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        assertThrows(NullPointerException.class, cache::hashCode, "Calling hashCode when internal cache is null should throw NPE");
    }

    @Test
    void hashCode_NullNodeEntry_ThrowsNPE() throws Exception {
        // Insert a mapping whose value (Node) is null.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> internalMap = (ConcurrentMap<Object, Object>) cacheField.get(cache);
        // null Node
        internalMap.put("someKey", null);
        assertThrows(NullPointerException.class, cache::hashCode, "Iterating over a null Node should result in NPE");
    }

    @Test
    void hashCode_KeyHashCodeThrowsRuntimeException() throws Exception {
        // Key whose hashCode() throws.
        class BadKey {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad key");
            }
        }
        BadKey badKey = new BadKey();
        // Insert a Node with the bad key via reflection.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> internalMap = (ConcurrentMap<Object, Object>) cacheField.get(cache);
        Class<?> nodeClass = Class.forName("com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy$Node");
        java.lang.reflect.Constructor<?> ctor = nodeClass.getDeclaredConstructor(Object.class, Object.class);
        ctor.setAccessible(true);
        Object node = ctor.newInstance(badKey, "value");
        internalMap.put(badKey, node);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::hashCode, "Exception from key.hashCode() should propagate");
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    void hashCode_ValueHashCodeThrowsRuntimeException() throws Exception {
        // Value whose hashCode() throws.
        class BadValue {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad value");
            }
        }
        BadValue badValue = new BadValue();
        // Insert a Node with a normal key and the bad value via reflection.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> internalMap = (ConcurrentMap<Object, Object>) cacheField.get(cache);
        Class<?> nodeClass = Class.forName("com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy$Node");
        java.lang.reflect.Constructor<?> ctor = nodeClass.getDeclaredConstructor(Object.class, Object.class);
        ctor.setAccessible(true);
        Object node = ctor.newInstance("goodKey", badValue);
        internalMap.put("goodKey", node);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::hashCode, "Exception from value.hashCode() should propagate");
        assertEquals("bad value", ex.getMessage());
    }

    @Test
    void hashCode_ValuesCollectionThrowsConcurrentModificationException() throws Exception {
        // Mock a ConcurrentMap whose values() collection throws CME when iterated.
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockedMap = mock(ConcurrentMap.class);
        Collection<Object> badValues = mock(Collection.class);
        when(mockedMap.values()).thenReturn(badValues);
        when(badValues.iterator()).thenThrow(new ConcurrentModificationException("boom"));
        // Inject the mock into the cache instance.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, mockedMap);
        assertThrows(ConcurrentModificationException.class, cache::hashCode, "Iterating over a values() collection that throws CME should propagate the exception");
    }
}
