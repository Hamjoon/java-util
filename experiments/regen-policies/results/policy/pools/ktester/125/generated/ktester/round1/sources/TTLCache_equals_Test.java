package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_equals_Test {

    private TTLCache<Integer, String> ttlCache;

    @BeforeEach
    void setupBeforeEach() {
        // Use a TTL that will never expire during the test execution.
        ttlCache = new TTLCache<>(10_000, -1);
        // Use a very large TTL so entries never expire during the test run.
        ttlCache = new TTLCache<>(10_000, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid cross‑test contamination.
        ttlCache.clear();
        ttlCache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background thread used by TTLCache.
        TTLCache.shutdown();
        TTLCache.shutdown();
    }

    private static Stream<Arguments> nullKeyValueProvider() {
        return Stream.of(// null key, non‑null value
        Arguments.of(mapOf(null, "nullValue"), mapOf(null, "nullValue"), true, "null key with non‑null value"), // non‑null key, null value
        Arguments.of(mapOf("nullKey", null), mapOf("nullKey", null), true, "non‑null key with null value"), // both null
        Arguments.of(mapOf(null, null), mapOf(null, null), true, "both key and value are null"));
    }

    private static Stream<Arguments> differentMapImplementationsProvider() {
        return Stream.of(Arguments.of(mapOf("a", 1, "b", 2), new TreeMap<>(mapOf("a", 1, "b", 2)), true, "TreeMap with same entries"), Arguments.of(mapOf("x", "y", "z", "w"), new ConcurrentHashMap<>(mapOf("x", "y", "z", "w")), true, "ConcurrentHashMap with same entries"));
    }

    private static Stream<Arguments> insertionOrderProvider() {
        return Stream.of(Arguments.of(linkedHashMap(new Object[][] { { "a", 10 }, { "b", 20 } }), linkedHashMap(new Object[][] { { "b", 20 }, { "a", 10 } }), true, "LinkedHashMap with reverse insertion order"));
    }

    private static Stream<Arguments> unmodifiableMapProvider() {
        return Stream.of(Arguments.of(mapOf("u", 5, "v", 6), Collections.unmodifiableMap(mapOf("u", 5, "v", 6)), true, "Unmodifiable view of a HashMap"));
    }

    private static Stream<Arguments> distinctKeyValueObjectsProvider() {
        return Stream.of(Arguments.of(mapOf(new String("key"), new Integer(100)), mapOf(new String("key"), new Integer(100)), true, "Distinct but equal String/Integer objects"));
    }

    private static Stream<Arguments> exceptionScenariosProvider() {
        // Map that throws on entrySet()
        Map<Object, Object> throwingMap = new AbstractMap<>() {

            @Override
            public Set<Entry<Object, Object>> entrySet() {
                throw new UnsupportedOperationException("entrySet not supported");
            }

            // Minimal required implementations for compilation
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public boolean containsKey(Object key) {
                return false;
            }

            @Override
            public boolean containsValue(Object value) {
                return false;
            }

            @Override
            public Object get(Object key) {
                return null;
            }

            @Override
            public Object put(Object key, Object value) {
                return null;
            }

            @Override
            public Object remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<?, ?> m) {
            }

            @Override
            public void clear() {
            }

            @Override
            public Set<Object> keySet() {
                return null;
            }

            @Override
            public Collection<Object> values() {
                return null;
            }
        };
        // Map that returns null entrySet()
        Map<Object, Object> nullEntrySetMap = new AbstractMap<>() {

            @Override
            public Set<Entry<Object, Object>> entrySet() {
                return null;
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public boolean containsKey(Object key) {
                return false;
            }

            @Override
            public boolean containsValue(Object value) {
                return false;
            }

            @Override
            public Object get(Object key) {
                return null;
            }

            @Override
            public Object put(Object key, Object value) {
                return null;
            }

            @Override
            public Object remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<?, ?> m) {
            }

            @Override
            public void clear() {
            }

            @Override
            public Set<Object> keySet() {
                return null;
            }

            @Override
            public Collection<Object> values() {
                return null;
            }
        };
        // Subclass of TTLCache that breaks entrySet()
        TTLCache<Object, Object> brokenCache = new TTLCache<>(10_000, -1) {

            @Override
            public Set<Entry<Object, Object>> entrySet() {
                throw new IllegalStateException("entrySet broken");
            }
        };
        return Stream.of(Arguments.of(throwingMap, UnsupportedOperationException.class, "Map throws on entrySet()"), Arguments.of(nullEntrySetMap, NullPointerException.class, "Map returns null entrySet()"), Arguments.of(brokenCache, IllegalStateException.class, "Subclass of TTLCache throws in entrySet()"));
    }

    /**
     * Creates a LinkedHashMap preserving the given insertion order.
     */
    private static Map<Object, Object> linkedHashMap(Object[][] pairs) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (Object[] pair : pairs) {
            map.put(pair[0], pair[1]);
        }
        return map;
    }

    @Test
    void equalsSameInstance() {
        assertTrue(ttlCache.equals(ttlCache), "A cache must be equal to itself");
    }

    @Test
    void equalsAnotherMapWithSameEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "A");
        other.put(2, "B");
        assertTrue(ttlCache.equals(other), "Caches with identical entries should be equal");
        // Symmetry check
        assertTrue(other.equals(ttlCache), "Equality should be symmetric");
    }

    @Test
    void equalsAnotherCacheWithSameEntries() {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        TTLCache<Integer, String> otherCache = new TTLCache<>(10_000, -1);
        otherCache.put(10, "X");
        otherCache.put(20, "Y");
        assertTrue(ttlCache.equals(otherCache), "Two TTLCache instances with the same entries should be equal");
        assertTrue(otherCache.equals(ttlCache), "Equality should be symmetric");
    }

    @Test
    void notEqualsWhenEntriesDiffer() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "A");
        // different value
        other.put(2, "C");
        assertFalse(ttlCache.equals(other), "Caches with different values must not be equal");
    }

    @Test
    void notEqualsWhenOtherHasExtraEntry() {
        ttlCache.put(1, "A");
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "A");
        // extra entry
        other.put(2, "B");
        assertFalse(ttlCache.equals(other), "Caches with a different number of entries must not be equal");
    }

    @Test
    void notEqualsWhenOtherIsNull() {
        assertFalse(ttlCache.equals(null), "Cache must not be equal to null");
    }

    @Test
    void notEqualsWhenOtherIsNotMap() {
        String notAMap = "I am not a map";
        assertFalse(ttlCache.equals(notAMap), "Cache must not be equal to an object that is not a Map");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nullKeyValueProvider")
    void equalsWithNullKeyOrValue(Map<Object, Object> cacheEntries, Map<Object, Object> other, boolean expected, String description) {
        ttlCache.putAll(cacheEntries);
        assertEquals(expected, ttlCache.equals(other), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("differentMapImplementationsProvider")
    void equalsAcrossDifferentMapImplementations(Map<Object, Object> cacheEntries, Map<Object, Object> other, boolean expected, String description) {
        ttlCache.putAll(cacheEntries);
        assertEquals(expected, ttlCache.equals(other), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("insertionOrderProvider")
    void equalsIgnoreInsertionOrder(Map<Object, Object> cacheEntries, Map<Object, Object> other, boolean expected, String description) {
        ttlCache.putAll(cacheEntries);
        assertEquals(expected, ttlCache.equals(other), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("unmodifiableMapProvider")
    void equalsWithUnmodifiableMap(Map<Object, Object> cacheEntries, Map<Object, Object> other, boolean expected, String description) {
        ttlCache.putAll(cacheEntries);
        assertEquals(expected, ttlCache.equals(other), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("distinctKeyValueObjectsProvider")
    void equalsWithDistinctButEqualObjects(Map<Object, Object> cacheEntries, Map<Object, Object> other, boolean expected, String description) {
        ttlCache.putAll(cacheEntries);
        assertEquals(expected, ttlCache.equals(other), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("exceptionScenariosProvider")
    void equalsPropagatesExceptions(Map<Object, Object> other, Class<? extends Throwable> expectedException, String description) {
        // The cache is empty; we only care about the behavior when entrySet() is invoked.
        assertThrows(expectedException, () -> ttlCache.equals(other), description);
    }

    /**
     * Convenience var‑args map creator: expects an even number of arguments (k1,v1,k2,v2,…)
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Object> mapOf(Object... entries) {
        Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(entries[i], entries[i + 1]);
        }
        return map;
    }
}
