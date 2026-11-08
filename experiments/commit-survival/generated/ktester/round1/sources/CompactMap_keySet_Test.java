package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class CompactMap_keySet_Test {

    /**
     * The map instance used in most tests.
     */
    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for every test case.
        map = new CompactMap<>();
        // Populate with a few entries that are well‑under the default compactSize()
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear to help the GC and to make sure no leftover state interferes with the next test.
        if (map != null) {
            map.clear();
        }
    }

    @Test
    void keySetProvidesCorrectViewAndBasicOperations() {
        Set<String> ks = map.keySet();
        // size() mirrors the map size
        assertEquals(3, ks.size(), "keySet size should reflect map size");
        // contains() delegates to map.containsKey()
        assertTrue(ks.contains("two"));
        assertFalse(ks.contains("four"));
        // remove() removes the entry from the map and returns true if the size changed
        boolean changed = ks.remove("two");
        assertTrue(changed, "remove should report a change when the key existed");
        assertFalse(map.containsKey("two"));
        assertEquals(2, ks.size());
        // remove() of a non‑existent key must not change the size
        changed = ks.remove("non‑existent");
        assertFalse(changed);
        assertEquals(2, ks.size());
        // removeAll() removes every key present in the supplied collection
        Collection<String> toRemove = Arrays.asList("one", "three", "does‑not‑exist");
        boolean removedAny = ks.removeAll(toRemove);
        assertTrue(removedAny, "removeAll should report a change when at least one key is removed");
        assertTrue(map.isEmpty(), "All entries should have been removed");
        assertEquals(0, ks.size());
        // After clearing via keySet, the map must also be empty
        map.put("alpha", 10);
        map.put("beta", 20);
        ks = map.keySet();
        ks.clear();
        assertTrue(map.isEmpty(), "clear via keySet must empty the underlying map");
    }

    @Test
    void retainAllKeepsOnlySpecifiedKeys() {
        // Starting map contains: one, two, three
        Set<String> ks = map.keySet();
        // Retain only "one" and a key that does not exist ("four")
        Collection<String> retain = Arrays.asList("one", "four");
        boolean changed = ks.retainAll(retain);
        assertTrue(changed, "retainAll should report a change when the view shrinks");
        assertEquals(1, ks.size());
        assertTrue(ks.contains("one"));
        assertFalse(ks.contains("two"));
        assertFalse(ks.contains("three"));
        assertFalse(map.containsKey("two"));
        assertFalse(map.containsKey("three"));
    }

    @Test
    void iteratorReturnsAllCurrentKeys() {
        Set<String> ks = map.keySet();
        Iterator<String> it = ks.iterator();
        // Collect keys from the iterator
        Set<String> iterated = new HashSet<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // The iterator must have produced exactly the same keys as the map
        assertEquals(new HashSet<>(Arrays.asList("one", "two", "three")), iterated);
    }

    @Test
    void iteratorTypeCanBeForcedViaSubclass() {
        // Subclass that forces the copy‑iterator path
        CompactMap<String, Integer> copyIterMap = new CompactMap<String, Integer>() {

            @Override
            protected boolean useCopyIterator() {
                // force the CopyKeyIterator
                return true;
            }

            @Override
            protected int compactSize() {
                // keep the default compactSize() (>=2) – we just need a small map
                return super.compactSize();
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // Use a regular HashMap for the backing map when we exceed compactSize()
                return new HashMap<>();
            }
        };
        copyIterMap.put("a", 1);
        copyIterMap.put("b", 2);
        copyIterMap.put("c", 3);
        // The keySet view should still work correctly even when the iterator implementation is swapped.
        Set<String> ks = copyIterMap.keySet();
        List<String> collected = new ArrayList<>();
        ks.forEach(collected::add);
        assertTrue(collected.containsAll(Arrays.asList("a", "b", "c")), "All keys must be present regardless of iterator implementation");
    }

    // -----------------------------------------------------------------
    @Test
    void iteratorSelection_copyIteratorReturnedWhenUseCopyIteratorTrue() {
        CompactMap<String, Integer> mapCopyIter = new CompactMap<String, Integer>() {

            @Override
            protected boolean useCopyIterator() {
                return true;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        mapCopyIter.put("x", 1);
        mapCopyIter.put("y", 2);
        Iterator<String> it = mapCopyIter.keySet().iterator();
        assertTrue(it.getClass().getSimpleName().contains("CopyKeyIterator"), "Iterator should be an instance of CopyKeyIterator when useCopyIterator() returns true");
    }

    @Test
    void iteratorSelection_compactIteratorReturnedWhenUseCopyIteratorFalse() {
        CompactMap<String, Integer> mapCompactIter = new CompactMap<String, Integer>() {

            @Override
            protected boolean useCopyIterator() {
                return false;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        mapCompactIter.put("x", 1);
        mapCompactIter.put("y", 2);
        Iterator<String> it = mapCompactIter.keySet().iterator();
        assertTrue(it.getClass().getSimpleName().contains("CompactKeyIterator"), "Iterator should be an instance of CompactKeyIterator when useCopyIterator() returns false");
    }

    // -----------------------------------------------------------------
    @Test
    void size_emptyMapReturnsZero() {
        CompactMap<String, Integer> empty = new CompactMap<>();
        assertEquals(0, empty.keySet().size(), "keySet().size() must be 0 for an empty map");
    }

    @Test
    void size_fiveEntriesReflectsMapSize() {
        CompactMap<String, Integer> five = new CompactMap<>();
        for (int i = 0; i < 5; i++) {
            five.put("k" + i, i);
        }
        assertEquals(5, five.keySet().size(), "keySet().size() must match map size after adding five entries");
    }

    // -----------------------------------------------------------------
    @Test
    void clear_viaKeySetEmptiesUnderlyingMap() {
        Set<String> ks = map.keySet();
        ks.clear();
        assertTrue(map.isEmpty(), "Calling clear() on the keySet view must empty the underlying map");
        assertEquals(0, ks.size(), "keySet size must be 0 after clear()");
    }

    // -----------------------------------------------------------------
    @Test
    void contains_existingKeyReturnsTrue() {
        assertTrue(map.keySet().contains("one"));
    }

    @Test
    void contains_nonExistingKeyReturnsFalse() {
        assertFalse(map.keySet().contains("doesNotExist"));
    }

    @Test
    void contains_nullKeyThrowsNullPointerException() {
        // Default CompactMap allows null keys (HashMap), so we need a map that disallows them.
        CompactMap<String, Integer> noNullMap = new CompactMap<String, Integer>() {

            @Override
            protected Map<String, Integer> getNewMap() {
                // ConcurrentHashMap does not permit null keys or values.
                return new ConcurrentHashMap<>();
            }
        };
        noNullMap.put("a", 1);
        assertThrows(NullPointerException.class, () -> noNullMap.keySet().contains(null), "contains(null) should throw NullPointerException when underlying map disallows null keys");
    }

    // -----------------------------------------------------------------
    @Test
    void remove_existingKeyReturnsTrueAndDeletesEntry() {
        Set<String> ks = map.keySet();
        assertTrue(ks.remove("one"));
        assertFalse(map.containsKey("one"));
        assertEquals(2, ks.size());
    }

    @Test
    void remove_nonExistingKeyReturnsFalseAndLeavesMapUnchanged() {
        Set<String> ks = map.keySet();
        int before = ks.size();
        assertFalse(ks.remove("nonexistent"));
        assertEquals(before, ks.size());
    }

    @Test
    void remove_nullKeyThrowsNullPointerException() {
        CompactMap<String, Integer> noNullMap = new CompactMap<String, Integer>() {

            @Override
            protected Map<String, Integer> getNewMap() {
                return new ConcurrentHashMap<>();
            }
        };
        noNullMap.put("a", 1);
        assertThrows(NullPointerException.class, () -> noNullMap.keySet().remove(null), "remove(null) should throw NullPointerException when underlying map disallows null keys");
    }

    // -----------------------------------------------------------------
    @Test
    void removeAll_collectionWithAtLeastOnePresentKeyReturnsTrue() {
        Set<String> ks = map.keySet();
        Collection<String> toRemove = Arrays.asList("one", "doesNotExist");
        assertTrue(ks.removeAll(toRemove));
        assertFalse(map.containsKey("one"));
        assertEquals(2, ks.size());
    }

    @Test
    void removeAll_collectionWithNoMatchingKeysReturnsFalse() {
        Set<String> ks = map.keySet();
        Collection<String> toRemove = Arrays.asList("x", "y");
        assertFalse(ks.removeAll(toRemove));
        assertEquals(3, ks.size());
    }

    @Test
    void removeAll_emptyCollectionReturnsFalseAndDoesNotModifyMap() {
        Set<String> ks = map.keySet();
        assertFalse(ks.removeAll(Collections.emptyList()));
        assertEquals(3, ks.size());
    }

    @Test
    void removeAll_nullCollectionThrowsNullPointerException() {
        Set<String> ks = map.keySet();
        assertThrows(NullPointerException.class, () -> ks.removeAll(null), "removeAll(null) should throw NullPointerException");
    }

    // -----------------------------------------------------------------
    @Test
    void retainAll_properSubsetReturnsTrueAndRemovesOtherKeys() {
        Set<String> ks = map.keySet();
        Collection<String> retain = Arrays.asList("one", "two");
        assertTrue(ks.retainAll(retain));
        assertEquals(2, ks.size());
        assertTrue(ks.containsAll(retain));
        assertFalse(ks.contains("three"));
    }

    @Test
    void retainAll_allKeysPresentReturnsFalseAndLeavesMapUnchanged() {
        Set<String> ks = map.keySet();
        Collection<String> retain = Arrays.asList("one", "two", "three");
        assertFalse(ks.retainAll(retain));
        assertEquals(3, ks.size());
    }

    @Test
    void retainAll_emptyCollectionClearsMapAndReturnsTrue() {
        Set<String> ks = map.keySet();
        assertTrue(ks.retainAll(Collections.emptyList()));
        assertTrue(map.isEmpty());
        assertEquals(0, ks.size());
    }

    @Test
    void retainAll_nullCollectionThrowsNullPointerException() {
        Set<String> ks = map.keySet();
        assertThrows(NullPointerException.class, () -> ks.retainAll(null), "retainAll(null) should throw NullPointerException");
    }

    @Test
    void retainAll_incompatibleElementTypeThrowsClassCastException() {
        Set<String> ks = map.keySet();
        Collection<Object> mixed = Arrays.asList(123, "validKey");
        assertThrows(ClassCastException.class, () -> ks.retainAll(mixed), "retainAll with an element of incompatible type should throw ClassCastException");
    }
}
