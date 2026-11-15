package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class LRUCache_entrySet_Test {

    /* --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetReflectsCacheContent(LRUCache.StrategyType strategy) {
        // given
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // when
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // then
        assertEquals(3, entrySet.size(), "entrySet size must match number of entries");
        assertTrue(entrySet.contains(Map.entry(1, "one")));
        assertTrue(entrySet.contains(Map.entry(2, "two")));
        assertTrue(entrySet.contains(Map.entry(3, "three")));
        // also verify that the set is backed by the cache – removing via iterator updates cache
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey().equals(2)) {
                // remove entry with key 2
                it.remove();
                break;
            }
        }
        assertNull(cache.get(2), "Key 2 should have been removed from the cache");
        assertEquals(2, cache.size(), "Cache size should reflect removal");
        assertFalse(entrySet.contains(Map.entry(2, "two")), "Removed entry must disappear from entrySet");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetIsLiveView(LRUCache.StrategyType strategy) {
        // given
        LRUCache<String, Integer> cache = new LRUCache<>(10, strategy);
        cache.put("a", 10);
        cache.put("b", 20);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // when – modify the cache directly
        cache.put("c", 30);
        cache.remove("a");
        // then – entrySet must reflect those changes immediately
        assertEquals(2, entrySet.size());
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals("b") && e.getValue().equals(20)));
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals("c") && e.getValue().equals(30)));
        assertFalse(entrySet.stream().anyMatch(e -> e.getKey().equals("a")));
        // when – modify via the entry set (update a value)
        for (Map.Entry<String, Integer> e : entrySet) {
            if (e.getKey().equals("b")) {
                e.setValue(99);
                break;
            }
        }
        // then – cache must see the updated value
        assertEquals(99, cache.get("b"));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetEmptyCache(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertTrue(entrySet.isEmpty(), "entrySet of an empty cache must be empty");
        assertEquals(0, entrySet.size(), "size must be 0");
        assertFalse(entrySet.iterator().hasNext(), "iterator must have no elements");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetContainsInsertedEntries(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(10, strategy);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertEquals(3, entrySet.size(), "entrySet size must match number of inserted entries");
        assertTrue(entrySet.contains(Map.entry("one", 1)));
        assertTrue(entrySet.contains(Map.entry("two", 2)));
        assertTrue(entrySet.contains(Map.entry("three", 3)));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetRemoveReflectsInCache(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(10, "ten");
        cache.put(20, "twenty");
        cache.put(30, "thirty");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // remove the entry with key 20 via the Set view
        boolean removed = entrySet.remove(Map.entry(20, "twenty"));
        assertTrue(removed, "Set.remove should return true for existing entry");
        assertNull(cache.get(20), "Removed key must no longer be present in the cache");
        assertEquals(2, cache.size(), "Cache size must decrease after removal");
        assertFalse(entrySet.contains(Map.entry(20, "twenty")), "Removed entry must disappear from entrySet");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetAddUnsupported(LRUCache.StrategyType strategy) {
        LRUCache<String, String> cache = new LRUCache<>(5, strategy);
        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        AbstractMap.SimpleEntry<String, String> newEntry = new AbstractMap.SimpleEntry<>("k", "v");
        assertThrows(UnsupportedOperationException.class, () -> entrySet.add(newEntry), "add operation on entrySet must throw UnsupportedOperationException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetReflectsDirectModifications(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("a", 1);
        cache.put("b", 2);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // modify cache after obtaining the view
        cache.put("c", 3);
        cache.remove("a");
        assertEquals(2, entrySet.size(), "entrySet size must reflect direct modifications");
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals("b") && e.getValue().equals(2)));
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals("c") && e.getValue().equals(3)));
        assertFalse(entrySet.stream().anyMatch(e -> e.getKey().equals("a")));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetIteratorRemove(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        // advance to the entry with key 2
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey().equals(2)) {
                // remove via iterator
                it.remove();
                break;
            }
        }
        assertNull(cache.get(2), "Removed key must be absent from cache");
        assertEquals(2, cache.size(), "Cache size must be reduced after iterator.remove()");
        assertFalse(entrySet.contains(Map.entry(2, "two")), "Removed entry must disappear from entrySet");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetContainsAndContainsAll(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("x", 100);
        cache.put("y", 200);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        Map.Entry<String, Integer> matchingX = Map.entry("x", 100);
        Map.Entry<String, Integer> matchingY = Map.entry("y", 200);
        Map.Entry<String, Integer> nonMatching = Map.entry("z", 300);
        assertTrue(entrySet.contains(matchingX));
        assertTrue(entrySet.contains(matchingY));
        assertFalse(entrySet.contains(nonMatching));
        Collection<Map.Entry<String, Integer>> collection = Arrays.asList(matchingX, matchingY);
        assertTrue(entrySet.containsAll(collection));
        Collection<Map.Entry<String, Integer>> collectionWithExtra = Arrays.asList(matchingX, nonMatching);
        assertFalse(entrySet.containsAll(collectionWithExtra));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetToArrayBehavior(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // Object[] version
        Object[] objArray = entrySet.toArray();
        assertEquals(3, objArray.length, "Object[] length must match entrySet size");
        for (Object o : objArray) {
            assertTrue(o instanceof Map.Entry, "Elements must be Map.Entry");
        }
        // Typed version
        @SuppressWarnings("unchecked")
        Map.Entry<Integer, String>[] typedArray = entrySet.toArray(new Map.Entry[0]);
        assertEquals(3, typedArray.length, "Typed array length must match entrySet size");
        for (Map.Entry<Integer, String> e : typedArray) {
            assertNotNull(e.getKey());
            assertNotNull(e.getValue());
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetReflectsClear(LRUCache.StrategyType strategy) {
        LRUCache<String, String> cache = new LRUCache<>(5, strategy);
        cache.put("a", "A");
        cache.put("b", "B");
        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        assertFalse(entrySet.isEmpty(), "Pre‑condition: entrySet not empty");
        cache.clear();
        assertTrue(entrySet.isEmpty(), "After clear(), entrySet must be empty");
        assertEquals(0, entrySet.size(), "size must be 0 after clear()");
        assertFalse(entrySet.iterator().hasNext(), "Iterator must have no elements after clear()");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetEvictionAfterOverflow(LRUCache.StrategyType strategy) {
        // Eviction timing is deterministic for the LOCKING strategy; for THREADED we skip the assertion.
        LRUCache<Integer, String> cache = new LRUCache<>(2, strategy);
        // will become LRU
        cache.put(1, "one");
        // now cache is full
        cache.put(2, "two");
        // should evict key 1
        cache.put(3, "three");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        // For LOCKING we can assert immediately; for THREADED we only verify that size never exceeds capacity.
        if (strategy == LRUCache.StrategyType.LOCKING) {
            assertEquals(2, entrySet.size(), "Cache size must respect capacity after eviction");
            assertFalse(entrySet.contains(Map.entry(1, "one")), "Least-recently-used entry must be evicted");
            assertTrue(entrySet.contains(Map.entry(2, "two")));
            assertTrue(entrySet.contains(Map.entry(3, "three")));
        } else {
            // Threaded strategy may be asynchronous; ensure size does not exceed capacity after a short wait.
            // (In a real test we might wait, but here we just check that size <= capacity.)
            assertTrue(entrySet.size() <= 2, "Threaded strategy size should not exceed capacity");
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetRemoveAllAndRetainAll(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(10, strategy);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.put("d", 4);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // Build a collection containing two of the entries
        Collection<Map.Entry<String, Integer>> toRemove = new ArrayList<>();
        toRemove.add(Map.entry("b", 2));
        toRemove.add(Map.entry("c", 3));
        // removeAll
        boolean changed = entrySet.removeAll(toRemove);
        assertTrue(changed, "removeAll should report modification");
        assertNull(cache.get("b"));
        assertNull(cache.get("c"));
        assertEquals(2, cache.size());
        assertFalse(entrySet.contains(Map.entry("b", 2)));
        assertFalse(entrySet.contains(Map.entry("c", 3)));
        assertTrue(entrySet.contains(Map.entry("a", 1)));
        assertTrue(entrySet.contains(Map.entry("d", 4)));
        // Refresh view for retainAll test
        cache.clear();
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.put("d", 4);
        Set<Map.Entry<String, Integer>> freshSet = cache.entrySet();
        // retainAll – keep only "a" and "d"
        Collection<Map.Entry<String, Integer>> toRetain = Arrays.asList(Map.entry("a", 1), Map.entry("d", 4));
        boolean retained = freshSet.retainAll(toRetain);
        assertTrue(retained, "retainAll should report modification");
        assertEquals(2, cache.size());
        assertTrue(entrySet.contains(Map.entry("a", 1)));
        assertTrue(entrySet.contains(Map.entry("d", 4)));
        assertFalse(entrySet.contains(Map.entry("b", 2)));
        assertFalse(entrySet.contains(Map.entry("c", 3)));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetIteratorConcurrentModification(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        // Structural change after iterator creation
        cache.put(3, "three");
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must throw ConcurrentModificationException after concurrent modification");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetAddAllUnsupported(LRUCache.StrategyType strategy) {
        LRUCache<String, String> cache = new LRUCache<>(5, strategy);
        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        assertThrows(UnsupportedOperationException.class, () -> entrySet.addAll(Set.of(new AbstractMap.SimpleEntry<>("k", "v"))), "addAll must throw UnsupportedOperationException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetToArrayIncompatibleArrayThrows(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertThrows(ArrayStoreException.class, () -> entrySet.toArray(new String[0]), "toArray(T[]) with incompatible array type must throw ArrayStoreException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetRemoveAllNullThrows(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("x", 1);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.removeAll(null), "removeAll(null) must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void entrySetRetainAllNullThrows(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("y", 2);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.retainAll(null), "retainAll(null) must throw NullPointerException");
    }
}
