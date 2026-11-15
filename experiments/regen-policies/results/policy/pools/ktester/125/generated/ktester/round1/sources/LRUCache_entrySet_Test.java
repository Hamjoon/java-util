package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class LRUCache_entrySet_Test {

    private LRUCache<Integer, String> cache;

    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the thread pool used by the threaded strategy (if any).
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Provides the two supported strategy types.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return java.util.Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Verifies that {@code entrySet()} returns a set view that reflects the
     * current contents of the cache and that removing an entry via the set
     * removes it from the cache.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetBackedByCache(LRUCache.StrategyType strategy) {
        // GIVEN
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // WHEN
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // THEN – the set reflects the three entries
        assertEquals(3, entrySet.size(), "entrySet size should match number of entries");
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>(1, "A")));
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>(2, "B")));
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>(3, "C")));
        // WHEN – remove one entry through the set view
        // any entry
        Map.Entry<Integer, String> toRemove = entrySet.iterator().next();
        boolean removedFromSet = entrySet.remove(toRemove);
        // THEN – removal reported true and cache reflects the change
        assertTrue(removedFromSet, "remove should return true for existing entry");
        assertEquals(2, cache.size(), "cache size should decrease after removal via entrySet");
        assertFalse(cache.containsKey(toRemove.getKey()), "removed key should no longer be present in cache");
        assertFalse(entrySet.contains(toRemove), "entrySet should no longer contain the removed entry");
    }

    /**
     * Confirms that the {@code entrySet()} view is live: changes made directly
     * to the cache are visible in the set without needing to call {@code entrySet()}
     * again.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetReflectsDirectCacheModifications(LRUCache.StrategyType strategy) {
        // GIVEN
        cache = new LRUCache<>(3, strategy);
        cache.put(10, "X");
        cache.put(20, "Y");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertEquals(2, entrySet.size());
        // WHEN – add a new mapping directly to the cache
        cache.put(30, "Z");
        // THEN – the previously obtained set view sees the new entry
        assertEquals(3, entrySet.size(), "entrySet should reflect new entry added to cache");
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>(30, "Z")));
    }

    /**
     * Ensures that the set view does not support addition of new entries
     * (as per {@link Map#entrySet()} contract). Attempting to add should
     * throw {@link UnsupportedOperationException}.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetAddUnsupported(LRUCache.StrategyType strategy) {
        // GIVEN
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // WHEN / THEN – adding via the set throws UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> {
            entrySet.add(new AbstractMap.SimpleEntry<>(2, "B"));
        });
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetEmptyOnNewCache(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertTrue(entrySet.isEmpty(), "Newly created cache should have an empty entrySet");
        assertEquals(0, entrySet.size(), "Size of entrySet for empty cache must be 0");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetSingleMapping(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertEquals(1, entrySet.size(), "entrySet should contain exactly one entry");
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>(1, "one")), "entrySet must contain the inserted mapping");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetMultipleMappings(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertEquals(3, entrySet.size(), "entrySet should contain three entries");
        // Order is not guaranteed – just verify presence of each entry.
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>(1, "a")));
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>(2, "b")));
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>(3, "c")));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetRemoveEntryViaSet(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(10, "X");
        cache.put(20, "Y");
        cache.put(30, "Z");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Remove a specific entry using Set.remove(...)
        Map.Entry<Integer, String> toRemove = new AbstractMap.SimpleEntry<>(20, "Y");
        assertTrue(entrySet.remove(toRemove), "remove should return true for existing entry");
        assertEquals(2, cache.size(), "Cache size must decrease after removal via entrySet");
        assertFalse(cache.containsKey(20), "Key removed via entrySet must no longer be present");
        assertFalse(entrySet.contains(toRemove), "entrySet must no longer contain the removed entry");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetIteratorRemove(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        // Advance to first element and remove it via iterator
        assertTrue(it.hasNext());
        Map.Entry<Integer, String> first = it.next();
        it.remove();
        assertEquals(1, cache.size(), "Cache size must be reduced after iterator.remove()");
        assertFalse(cache.containsKey(first.getKey()), "Removed key must be absent from cache");
        assertFalse(entrySet.contains(first), "entrySet must not contain the removed entry");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetClearViaView(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Clear through the view
        entrySet.clear();
        assertTrue(cache.isEmpty(), "Cache must be empty after entrySet().clear()");
        assertEquals(0, entrySet.size(), "entrySet size must be 0 after clear");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetSameInstanceOnRepeatedCalls(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        Set<Map.Entry<Integer, String>> first = cache.entrySet();
        Set<Map.Entry<Integer, String>> second = cache.entrySet();
        assertSame(first, second, "Repeated calls to entrySet() must return the identical Set instance");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetAddAllUnsupported(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Collection<Map.Entry<Integer, String>> toAdd = java.util.Collections.singleton(new AbstractMap.SimpleEntry<>(3, "C"));
        assertThrows(UnsupportedOperationException.class, () -> entrySet.addAll(toAdd), "addAll operation must be unsupported");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetAddAllNullCollectionThrowsNPE(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.addAll(null), "addAll(null) must throw NullPointerException before evaluating unsupportedness");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetAddNullEntryUnsupported(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertThrows(UnsupportedOperationException.class, () -> entrySet.add(null), "add(null) must throw UnsupportedOperationException");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetContainsSemantics(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(42, "answer");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>(42, "answer")), "contains must return true for an existing entry");
        assertFalse(entrySet.contains(new AbstractMap.SimpleEntry<>(99, "missing")), "contains must return false for a non‑existing entry");
        assertFalse(entrySet.contains(null), "contains(null) must return false and not throw");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetNullArgumentCollectionMethods(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.removeAll(null));
        assertThrows(NullPointerException.class, () -> entrySet.retainAll(null));
        assertThrows(NullPointerException.class, () -> entrySet.containsAll(null));
        assertThrows(NullPointerException.class, () -> entrySet.toArray((Object[]) null));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetIteratorEdgeCases(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        assertFalse(it.hasNext(), "Iterator over empty entrySet must report hasNext == false");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on empty iterator must throw NoSuchElementException");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetIteratorIllegalState(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        // remove() before next()
        assertThrows(IllegalStateException.class, it::remove, "Calling iterator.remove() before next() must throw IllegalStateException");
        // proper next() then remove()
        assertTrue(it.hasNext());
        it.next();
        it.remove();
        // second consecutive remove() without next()
        assertThrows(IllegalStateException.class, it::remove, "Consecutive iterator.remove() calls must throw IllegalStateException");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetIteratorConcurrentModificationPut(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        // Structural change after iterator creation
        cache.put(3, "three");
        // The next call on iterator must detect the modification
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must throw ConcurrentModificationException after cache.put()");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetIteratorConcurrentModificationRemove(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        // Direct removal from cache
        cache.remove(1);
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must throw ConcurrentModificationException after cache.remove()");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetToArrayConversion(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "a");
        cache.put(2, "b");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Object[] version
        Object[] array = entrySet.toArray();
        assertEquals(2, array.length, "toArray() must return an array of length equal to set size");
        // Verify that each element is a Map.Entry with expected content
        boolean foundA = false, foundB = false;
        for (Object o : array) {
            assertTrue(o instanceof Map.Entry, "Array elements must be Map.Entry instances");
            Map.Entry<Integer, String> e = (Map.Entry<Integer, String>) o;
            if (e.getKey().equals(1) && e.getValue().equals("a"))
                foundA = true;
            if (e.getKey().equals(2) && e.getValue().equals("b"))
                foundB = true;
        }
        assertTrue(foundA && foundB, "Both entries must be present in the array");
        // Typed array version
        @SuppressWarnings("unchecked")
        Map.Entry<Integer, String>[] typed = entrySet.toArray(new Map.Entry[0]);
        assertEquals(2, typed.length);
        // Convert to a list for easier contains checks
        java.util.List<Map.Entry<Integer, String>> list = java.util.Arrays.asList(typed);
        assertTrue(list.contains(new AbstractMap.SimpleEntry<>(1, "a")));
        assertTrue(list.contains(new AbstractMap.SimpleEntry<>(2, "b")));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetEqualsAndHashCode(LRUCache.StrategyType strategy) {
        // Two caches with identical contents
        LRUCache<Integer, String> cache1 = new LRUCache<>(5, strategy);
        LRUCache<Integer, String> cache2 = new LRUCache<>(5, strategy);
        cache1.put(1, "one");
        cache1.put(2, "two");
        cache2.put(1, "one");
        cache2.put(2, "two");
        Set<Map.Entry<Integer, String>> set1 = cache1.entrySet();
        Set<Map.Entry<Integer, String>> set2 = cache2.entrySet();
        assertEquals(set1, set2, "entrySet() of two caches with same mappings must be equal");
        assertEquals(set1.hashCode(), set2.hashCode(), "Equal entrySet() instances must have identical hash codes");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetReflectsExternalRemoval(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(99, "alpha");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Remove directly from cache
        cache.remove(99);
        assertFalse(entrySet.contains(new AbstractMap.SimpleEntry<>(99, "alpha")), "entrySet must not contain entry removed directly from cache");
        assertEquals(cache.size(), entrySet.size(), "entrySet size must stay in sync with cache size after direct removal");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySetAfterCacheClear(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        cache.clear();
        assertTrue(entrySet.isEmpty(), "entrySet must be empty after cache.clear()");
        assertEquals(0, entrySet.size());
    }
}
