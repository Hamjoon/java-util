package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_keySet_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test
        map = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure map is cleared after each test (good practice, not strictly required)
        map.clear();
    }

    /**
     * Helper to populate the map with a given number of entries
     */
    private void populateMap(int count) {
        for (int i = 0; i < count; i++) {
            map.put("key" + i, i);
        }
    }

    @Test
    void keySetSizeMatchesMapSize() {
        populateMap(5);
        Set<String> ks = map.keySet();
        assertEquals(map.size(), ks.size(), "keySet().size() should be equal to map.size()");
    }

    @Test
    void keySetContainsReflectsMapContainsKey() {
        map.put("alpha", 1);
        map.put("beta", 2);
        Set<String> ks = map.keySet();
        assertTrue(ks.contains("alpha"));
        assertTrue(ks.contains("beta"));
        assertFalse(ks.contains("gamma"));
    }

    @Test
    void keySetClearEmptiesTheMap() {
        populateMap(3);
        Set<String> ks = map.keySet();
        assertFalse(ks.isEmpty());
        ks.clear();
        assertTrue(ks.isEmpty(), "After clear(), keySet should be empty");
        assertEquals(0, map.size(), "Map size should be zero after keySet.clear()");
    }

    @Test
    void keySetRemoveDeletesEntryAndReturnsCorrectFlag() {
        map.put("one", 1);
        map.put("two", 2);
        Set<String> ks = map.keySet();
        // remove existing key – should return true
        assertTrue(ks.remove("one"));
        assertFalse(map.containsKey("one"));
        assertEquals(1, map.size());
        // remove non‑existing key – should return false
        assertFalse(ks.remove("nonexistent"));
        assertEquals(1, map.size());
    }

    @Test
    void keySetRemoveAllDeletesAllSpecifiedKeys() {
        // keys: key0 .. key3
        populateMap(4);
        Set<String> ks = map.keySet();
        Collection<String> toRemove = Arrays.asList("key1", "key3", "notPresent");
        assertTrue(ks.removeAll(toRemove), "removeAll should report a change when at least one key is removed");
        assertFalse(map.containsKey("key1"));
        assertFalse(map.containsKey("key3"));
        assertTrue(map.containsKey("key0"));
        assertTrue(map.containsKey("key2"));
        assertEquals(2, map.size());
    }

    @Test
    void keySetRetainAllKeepsOnlySpecifiedKeys() {
        // Populate with 5 entries
        // keys: key0 .. key4
        populateMap(5);
        Set<String> ks = map.keySet();
        // Retain only a subset
        Collection<String> toRetain = Arrays.asList("key0", "key2", "key4");
        assertTrue(ks.retainAll(toRetain), "retainAll should report a change when elements are removed");
        // Verify that only the retained keys remain
        assertTrue(map.containsKey("key0"));
        assertTrue(map.containsKey("key2"));
        assertTrue(map.containsKey("key4"));
        assertFalse(map.containsKey("key1"));
        assertFalse(map.containsKey("key3"));
        assertEquals(3, map.size());
    }

    @Test
    void keySetIteratorIteratesAllKeys() {
        populateMap(3);
        Set<String> ks = map.keySet();
        Iterator<String> it = ks.iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("key0", "key1", "key2")), seen, "Iterator should return all keys present in the map");
    }

    /**
     * Verify that overriding useCopyIterator() returns a CopyKeyIterator.
     */
    @Test
    void iteratorSelectionReturnsCopyKeyIteratorWhenOverridden() {
        CompactMap<String, Integer> copyMap = new CompactMap<String, Integer>() {

            @Override
            protected boolean useCopyIterator() {
                return true;
            }
        };
        copyMap.put("a", 1);
        copyMap.put("b", 2);
        Iterator<String> it = copyMap.keySet().iterator();
        // The concrete class name contains "CopyKeyIterator"
        assertTrue(it.getClass().getSimpleName().contains("CopyKeyIterator"), "Iterator should be an instance of CopyKeyIterator when useCopyIterator() returns true");
    }

    /**
     * Verify that default behaviour returns a CompactKeyIterator.
     */
    @Test
    void iteratorSelectionReturnsCompactKeyIteratorWhenDefault() {
        CompactMap<String, Integer> defaultMap = new CompactMap<String, Integer>() {

            @Override
            protected boolean useCopyIterator() {
                return false;
            }
        };
        defaultMap.put("x", 10);
        defaultMap.put("y", 20);
        Iterator<String> it = defaultMap.keySet().iterator();
        assertTrue(it.getClass().getSimpleName().contains("CompactKeyIterator"), "Iterator should be an instance of CompactKeyIterator when useCopyIterator() returns false");
    }

    /**
     * Size reflection after insertions, deletions and clear.
     */
    @Test
    void sizeReflectionAfterVariousOperations() {
        CompactMap<String, Integer> m = new CompactMap<>();
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        assertEquals(3, m.keySet().size());
        m.remove("b");
        assertEquals(2, m.keySet().size());
        m.clear();
        assertEquals(0, m.keySet().size());
    }

    /**
     * clear() via keySet reflects on map.isEmpty().
     */
    @Test
    void clearEffectViaKeySet() {
        populateMap(4);
        Set<String> ks = map.keySet();
        ks.clear();
        assertTrue(map.isEmpty(), "Map should be empty after keySet().clear()");
        assertEquals(0, ks.size(), "keySet size should be 0 after clear");
    }

    /**
     * contains(null) behaviour.
     */
    @Test
    void containsBehaviorWithNullKey() {
        map.put(null, 99);
        Set<String> ks = map.keySet();
        assertTrue(ks.contains(null), "keySet should contain null when map has a null key");
        map.remove(null);
        assertFalse(ks.contains(null), "keySet should not contain null after removal");
    }

    /**
     * removeAll returns false when nothing matches.
     */
    @Test
    void removeAllReturnsFalseWhenNoMatch() {
        map.put("a", 1);
        map.put("b", 2);
        Set<String> ks = map.keySet();
        boolean changed = ks.removeAll(Arrays.asList("c", "d"));
        assertFalse(changed, "removeAll should return false when no keys are removed");
        assertEquals(2, ks.size(), "Size should remain unchanged");
    }

    /**
     * retainAll returns false when set is unchanged.
     */
    @Test
    void retainAllReturnsFalseWhenUnchanged() {
        map.put("1", 1);
        map.put("2", 2);
        Set<String> ks = map.keySet();
        boolean changed = ks.retainAll(Arrays.asList("1", "2", "3"));
        assertFalse(changed, "retainAll should return false when no elements are removed");
        assertEquals(2, ks.size(), "Size should stay the same");
    }

    /**
     * Iterator reflects modifications when using CopyKeyIterator (snapshot).
     */
    @Test
    void iteratorConsistencyAfterModificationWithCopyIterator() {
        CompactMap<String, Integer> copyMap = new CompactMap<String, Integer>() {

            @Override
            protected boolean useCopyIterator() {
                return true;
            }
        };
        copyMap.put("a", 1);
        copyMap.put("b", 2);
        Iterator<String> it = copyMap.keySet().iterator();
        // modify map after iterator creation
        copyMap.put("c", 3);
        // iterator should still iterate over the snapshot (a,b) without throwing
        List<String> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        assertTrue(collected.containsAll(Arrays.asList("a", "b")), "Snapshot iterator should contain original elements");
        assertFalse(collected.contains("c"), "Snapshot iterator should not contain elements added after its creation");
    }

    /**
     * CompactKeyIterator should throw CME on concurrent modification.
     */
    @Test
    void concurrentModificationExceptionFromCompactKeyIterator() {
        CompactMap<String, Integer> compactMap = new CompactMap<String, Integer>() {

            @Override
            protected boolean useCopyIterator() {
                // force CompactKeyIterator
                return false;
            }
        };
        compactMap.put("x", 10);
        compactMap.put("y", 20);
        Iterator<String> it = compactMap.keySet().iterator();
        // structural change after iterator creation
        compactMap.put("z", 30);
        // The next call should raise CME (or any RuntimeException indicating concurrent modification)
        assertThrows(ConcurrentModificationException.class, it::next, "CompactKeyIterator should throw ConcurrentModificationException after map modification");
    }

    /**
     * Case‑insensitive keySet.contains works when map is case‑insensitive.
     */
    @Test
    void caseInsensitiveKeySetContains() {
        CompactMap<String, Integer> ciMap = new CompactMap<String, Integer>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // TreeMap with case‑insensitive ordering
                return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            }
        };
        ciMap.put("Key", 1);
        Set<String> ks = ciMap.keySet();
        assertTrue(ks.contains("key"), "keySet should treat keys case‑insensitively");
        assertTrue(ks.contains("KEY"), "keySet should treat keys case‑insensitively");
    }

    /**
     * iteratorConsistencyAfterModification when iterator is not copy‑based – may throw CME.
     */
    @Test
    void iteratorConsistencyAfterModificationWhenNotCopyBased() {
        CompactMap<String, Integer> compactMap = new CompactMap<String, Integer>() {

            @Override
            protected boolean useCopyIterator() {
                // CompactKeyIterator
                return false;
            }
        };
        compactMap.put("a", 1);
        compactMap.put("b", 2);
        Iterator<String> it = compactMap.keySet().iterator();
        // modify map before iterating
        compactMap.put("c", 3);
        // The iterator is expected to throw CME on next()
        assertThrows(ConcurrentModificationException.class, it::next, "CompactKeyIterator should detect concurrent modification");
    }
}
