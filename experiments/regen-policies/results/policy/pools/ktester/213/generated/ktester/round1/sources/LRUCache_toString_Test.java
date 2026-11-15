package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class LRUCache_toString_Test {

    private LRUCache<Integer, String> cache;

    /**
     * A Map implementation whose {@code toString()} always throws
     * {@link IllegalStateException}. All other operations are delegated to a
     * simple {@link ConcurrentHashMap} to keep the object usable.
     */
    private static class ExplodingMap<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> delegate = new ConcurrentHashMap<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * A Map whose {@code toString()} returns a string that contains duplicate
     * keys. The map stores a single entry internally; the duplicate is only
     * a visual artifact of the overridden {@code toString()}.
     */
    private static class DuplicateKeyMap<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> delegate = new LinkedHashMap<>();

        DuplicateKeyMap() {
            // Store a single entry; the duplicate will be added in toString().
            delegate.put((K) "a", (V) Integer.valueOf(2));
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public String toString() {
            // Simulate duplicate key representation.
            return "{a=1, a=2}";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The cache is created per‑parameterised test via setUp(strategy).
        cache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache reference is cleared between tests.
        cache = null;
    }

    /* --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return java.util.Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- */
    private void setUp(LRUCache.StrategyType strategy) {
        // capacity 3 is enough for the tests below
        cache = new LRUCache<>(3, strategy);
    }

    /**
     * Replaces the private {@code strategy} field with the supplied map.
     */
    private static void setUnderlyingMap(LRUCache<?, ?> cache, Map<?, ?> newMap) {
        try {
            Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            f.set(cache, newMap);
        } catch (ReflectiveOperationException e) {
            fail("Unable to set underlying strategy map via reflection", e);
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void toStringReflectsEntries(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        String s = cache.toString();
        // The default Map#toString format is {key1=value1, key2=value2, ...}
        assertTrue(s.startsWith("{") && s.endsWith("}"), "String should be wrapped in braces");
        assertTrue(s.contains("1=A"));
        assertTrue(s.contains("2=B"));
        assertTrue(s.contains("3=C"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void toStringHandlesNullKeyAndValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(null, null);
        cache.put(5, null);
        cache.put(null, "value");
        String s = cache.toString();
        // Verify the representation of null keys/values
        assertTrue(s.contains("null=null"));
        assertTrue(s.contains("5=null"));
        assertTrue(s.contains("null=value"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void toStringOfEmptyCacheIsEmptyBraces(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // No entries added – cache is empty
        assertEquals("{}", cache.toString(), "Empty cache should produce {} as its string representation");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void toStringConsistentWithMapToString(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(10, "X");
        cache.put(20, "Y");
        // The underlying strategy implements Map, so we can compare directly.
        Map<Integer, String> underlying = (Map<Integer, String>) getUnderlyingMap(cache);
        assertEquals(underlying.toString(), cache.toString(), "LRUCache.toString() must delegate to the strategy's Map#toString()");
    }

    /**
     * Retrieves the private {@code strategy} field from the LRUCache instance.
     * This helper is deliberately isolated in a single method to keep the
     * test logic clear and to avoid direct field access elsewhere.
     */
    @SuppressWarnings("unchecked")
    private static Object getUnderlyingMap(LRUCache<?, ?> cache) {
        try {
            java.lang.reflect.Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            return f.get(cache);
        } catch (ReflectiveOperationException e) {
            fail("Unable to access underlying strategy map via reflection", e);
            // unreachable
            return null;
        }
    }

    /**
     * When the internal {@code strategy} field is {@code null},
     * {@code toString()} must throw a {@link NullPointerException}
     * because it dereferences the field.
     */
    @Test
    void toStringThrowsNPEWhenStrategyIsNull() {
        // Use any valid strategy for construction, then null it out.
        setUp(LRUCache.StrategyType.LOCKING);
        setUnderlyingMap(cache, null);
        assertThrows(NullPointerException.class, cache::toString, "Calling toString() with a null strategy should throw NPE");
    }

    /**
     * Simulates a strategy map whose {@code toString()} throws an
     * {@link IllegalStateException}. The exception must propagate unchanged.
     */
    @Test
    void toStringPropagatesExceptionFromStrategy() {
        setUp(LRUCache.StrategyType.LOCKING);
        Map<Integer, String> explodingMap = new ExplodingMap<>();
        setUnderlyingMap(cache, explodingMap);
        IllegalStateException ex = assertThrows(IllegalStateException.class, cache::toString, "Exception from strategy's toString() must be propagated");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Verifies that complex objects (lists, nested maps) are correctly
     * represented because {@code LRUCache.toString()} simply forwards the call.
     */
    @Test
    void toStringWithComplexObjects() {
        setUp(LRUCache.StrategyType.THREADED);
        List<Integer> list = new ArrayList<>();
        Collections.addAll(list, 1, 2, 3);
        Map<String, String> inner = new LinkedHashMap<>();
        inner.put("inner", "val");
        cache.put(0, "simple");
        // we store the list itself as value
        cache.put(1, list.toString());
        cache.put(2, inner.toString());
        // Build an equivalent map manually to compare string representations.
        Map<Integer, Object> expected = new LinkedHashMap<>();
        expected.put(0, "simple");
        expected.put(1, list);
        expected.put(2, inner);
        assertEquals(expected.toString(), cache.toString(), "toString() must delegate and include complex object representations");
    }

    /**
     * Simulates a strategy map that pretends to contain duplicate keys.
     * Because a regular {@link Map} cannot hold duplicate keys, we provide a
     * custom implementation whose {@code toString()} returns a string with
     * duplicate entries. The test checks that {@code LRUCache.toString()}
     * returns exactly that string.
     */
    @Test
    void toStringShowsDuplicateKeySimulation() {
        setUp(LRUCache.StrategyType.LOCKING);
        Map<String, Integer> dupKeyMap = new DuplicateKeyMap<>();
        setUnderlyingMap(cache, dupKeyMap);
        // The custom map's toString() returns "{a=1, a=2}"
        assertEquals("{a=1, a=2}", cache.toString(), "LRUCache should return the strategy's toString(), even if it contains duplicate keys");
    }
}
