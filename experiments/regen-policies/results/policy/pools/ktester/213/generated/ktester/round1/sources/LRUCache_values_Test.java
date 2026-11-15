package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class LRUCache_values_Test {

    private LRUCache<Integer, String> cache;

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required
        cache = null;
    }

    /* --------------------------------------------------------------------- */
    private void setUpCache(LRUCache.StrategyType strategy) {
        // Capacity 3 is enough for the tests; strategy determines the internal impl
        cache = new LRUCache<>(3, strategy);
    }

    /* --------------------------------------------------------------------- */
    static List<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesContainsAllInsertedValues(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        Collection<String> values = cache.values();
        assertNotNull(values, "values() must not return null");
        assertEquals(3, values.size(), "Collection size should match number of entries");
        assertTrue(values.containsAll(Arrays.asList("A", "B", "C")), "Collection should contain all inserted values");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesReflectRemoval(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "X");
        cache.put(2, "Y");
        cache.put(3, "Z");
        Collection<String> values = cache.values();
        assertTrue(values.contains("Y"));
        // Remove the entry via the map and verify the collection updates
        cache.remove(2);
        assertFalse(values.contains("Y"), "Removed value must disappear from values() collection");
        assertEquals(2, values.size(), "Size must shrink after removal");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesAddUnsupported(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "one");
        Collection<String> values = cache.values();
        assertThrows(UnsupportedOperationException.class, () -> values.add("illegal"), "Adding directly to the values collection must throw UnsupportedOperationException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesEmptyCache(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        Collection<String> values = cache.values();
        assertNotNull(values, "values() must never be null");
        assertTrue(values.isEmpty(), "Empty cache should yield an empty values collection");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesLiveViewRemovalViaCollection(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        Collection<String> values = cache.values();
        assertTrue(values.remove("two"), "Collection.remove should return true for existing value");
        assertEquals(2, cache.size(), "Cache size must decrease after collection removal");
        assertFalse(cache.containsValue("two"), "Removed value must no longer be present in the cache");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesLiveViewReflectsPut(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "alpha");
        Collection<String> values = cache.values();
        assertFalse(values.contains("beta"));
        cache.put(2, "beta");
        assertTrue(values.contains("beta"), "Values view must reflect newly added entry");
        assertEquals(2, values.size(), "Values collection size must match cache size after put");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesContainsNullValue(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, null);
        Collection<String> values = cache.values();
        assertEquals(1, values.size(), "Collection size should be 1 when a null value is stored");
        assertTrue(values.contains(null), "Collection must contain the null value");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesReflectStrategyReplacement(LRUCache.StrategyType strategy) throws Exception {
        setUpCache(strategy);
        // Populate the original cache (will be overwritten)
        cache.put(1, "old");
        // New backing map with distinct entries
        Map<Integer, String> newStrategy = new HashMap<>();
        newStrategy.put(10, "newA");
        newStrategy.put(20, "newB");
        // Replace the private 'strategy' field
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, newStrategy);
        Collection<String> values = cache.values();
        assertEquals(2, values.size(), "Values collection must reflect the new strategy map size");
        assertTrue(values.containsAll(Arrays.asList("newA", "newB")), "Values must come from the injected map");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIteratorRemoveReflectsCache(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        Iterator<String> it = cache.values().iterator();
        assertTrue(it.hasNext());
        // could be any value
        String first = it.next();
        // should remove the entry that produced 'first'
        it.remove();
        assertEquals(2, cache.size(), "Cache size must decrease after iterator.remove()");
        assertFalse(cache.containsValue(first), "Removed value must no longer be present in the cache");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesViewAfterClear(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "a");
        cache.put(2, "b");
        Collection<String> values = cache.values();
        cache.clear();
        assertTrue(values.isEmpty(), "Values view must be empty after cache.clear()");
        assertEquals(0, values.size(), "Size must be zero after clear");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesViewAfterPutAll(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        // capture early
        Collection<String> values = cache.values();
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "x");
        map.put(2, "y");
        map.put(3, "z");
        cache.putAll(map);
        assertEquals(3, values.size(), "Values view must reflect entries added via putAll");
        assertTrue(values.containsAll(Arrays.asList("x", "y", "z")));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testMultipleValuesCallsSameInstance(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        Collection<String> first = cache.values();
        Collection<String> second = cache.values();
        assertSame(first, second, "Two consecutive calls to values() must return the identical instance");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testCollectionToArray(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "v1");
        cache.put(2, "v2");
        cache.put(3, "v3");
        Collection<String> values = cache.values();
        Object[] array = values.toArray();
        assertEquals(3, array.length, "toArray length must match cache size");
        List<Object> expected = Arrays.asList("v1", "v2", "v3");
        assertTrue(expected.containsAll(Arrays.asList(array)), "Array must contain exactly the cache values");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveAllAffectsCache(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        Collection<String> values = cache.values();
        assertTrue(values.removeAll(Arrays.asList("two", "three")), "removeAll should report modification");
        assertEquals(1, cache.size(), "Cache size must reflect removals");
        assertTrue(cache.containsValue("one"));
        assertFalse(cache.containsValue("two"));
        assertFalse(cache.containsValue("three"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testRetainAllAffectsCache(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        Collection<String> values = cache.values();
        assertTrue(values.retainAll(Collections.singleton("one")), "retainAll should report modification");
        assertEquals(1, cache.size(), "Cache size must be 1 after retainAll");
        assertTrue(cache.containsValue("one"));
        assertFalse(cache.containsValue("two"));
        assertFalse(cache.containsValue("three"));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesWhenStrategyIsNull(LRUCache.StrategyType strategy) throws Exception {
        setUpCache(strategy);
        // Force the internal strategy to null
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.values(), "Calling values() with a null internal strategy must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testConcurrentModificationDuringIteration(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "a");
        cache.put(2, "b");
        Iterator<String> it = cache.values().iterator();
        assertTrue(it.hasNext());
        // advance once
        it.next();
        // Modify the cache while iterator is still alive
        cache.put(3, "c");
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must be fail‑fast and throw CME after concurrent modification");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesCollectionClearUnsupported(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "x");
        Collection<String> values = cache.values();
        assertThrows(UnsupportedOperationException.class, values::clear, "Calling clear() on the values view must throw UnsupportedOperationException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesCollectionAddAllUnsupported(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "x");
        Collection<String> values = cache.values();
        assertThrows(UnsupportedOperationException.class, () -> values.addAll(Arrays.asList("y", "z")), "addAll on the values view must throw UnsupportedOperationException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesCollectionRemoveObjectUnsupported(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(1, "x");
        Collection<String> values = cache.values();
        assertThrows(UnsupportedOperationException.class, () -> values.remove("x"), "remove(Object) on the values view must throw UnsupportedOperationException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesWithUnmodifiableUnderlyingMap(LRUCache.StrategyType strategy) throws Exception {
        setUpCache(strategy);
        // Populate a mutable map first
        Map<Integer, String> mutable = new HashMap<>();
        mutable.put(1, "a");
        mutable.put(2, "b");
        // Wrap it with Collections.unmodifiableMap and inject via reflection
        Map<Integer, String> unmodifiable = Collections.unmodifiableMap(mutable);
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, unmodifiable);
        Collection<String> values = cache.values();
        // Attempting to remove via the view must propagate the underlying map's immutability
        assertThrows(UnsupportedOperationException.class, () -> values.remove("a"), "Removal from values view must fail when underlying map is unmodifiable");
    }
}
