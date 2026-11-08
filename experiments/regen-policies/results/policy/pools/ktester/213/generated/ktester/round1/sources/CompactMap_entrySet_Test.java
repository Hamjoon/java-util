package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

// Mockito is not required for these tests but kept from the template
@ExtendWith(MockitoExtension.class)
class CompactMap_entrySet_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a CompactMap that will stay in the “compact array” state for the
        // first few inserts (compactSize = 3).  Using a LinkedHashMap as the backing
        // map for the “large” state keeps iteration order predictable.
        map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /* --------------------------------------------------------------------- *
     *  Helper – creates a simple immutable Map.Entry
     * --------------------------------------------------------------------- */
    private static <K, V> Map.Entry<K, V> getEntry(final K key, final V value) {
        return new Map.Entry<K, V>() {

            private V v = value;

            @Override
            public K getKey() {
                return key;
            }

            @Override
            public V getValue() {
                return v;
            }

            @Override
            public V setValue(V value) {
                V old = v;
                v = value;
                return old;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Map.Entry)) {
                    return false;
                }
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return Objects.equals(key, e.getKey()) && Objects.equals(v, e.getValue());
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(key) ^ Objects.hashCode(v);
            }
        };
    }

    private static Stream<Arguments> provideContainsCases() {
        return Stream.of(// Map contains the key "foo" with a non‑null value "bar"; entrySet().contains should return true.
        Arguments.of("existing non‑null entry", new SimpleEntry<>("foo", 1), true), // Map stores a null value for key "nullKey"; entrySet().contains must treat a null value as a legitimate entry and return true.
        Arguments.of("existing entry with null value", new SimpleEntry<>("nullKey", null), true), // Key "missing" is not present in the map; contains should return false.
        Arguments.of("missing key", new SimpleEntry<>("missing", 5), false), // Parameter is not a Map.Entry instance; contains must return false.
        // We simulate this by passing a non‑entry object via reflection (the Set.contains method will just return false)
        // The test harness calls entrySet.contains(Object) directly, so we use a raw cast.
        Arguments.of("non‑entry object", (Map.Entry<String, Integer>) (Object) "just a string", false));
    }

    private static Stream<Arguments> provideRemoveCases() {
        return Stream.of(Arguments.of("existing entry removal (key only matters)", new SimpleEntry<>("toRemove", 999), true), Arguments.of("object not an entry", "not an entry", false));
    }

    private static Stream<Arguments> provideRemoveAllCases() {
        return Stream.of(Arguments.of("removeAll existing entries", List.of(new SimpleEntry<>("a", 1), new SimpleEntry<>("b", 2)), true), Arguments.of("removeAll with non‑existent entry", List.of(new SimpleEntry<>("x", 9)), false));
    }

    private static Stream<Arguments> provideRetainAllCases() {
        return Stream.of(Arguments.of("retain subset with mismatched value for B", List.of(new SimpleEntry<>("A", 1), new SimpleEntry<>("B", 99)), true), Arguments.of("retain exact same collection", List.of(new SimpleEntry<>("A", 1), new SimpleEntry<>("B", 2), new SimpleEntry<>("C", 3)), false));
    }

    @Test
    void entrySetContainsExistingEntry() {
        map.put("one", 1);
        map.put("two", 2);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Map.Entry<String, Integer> present = getEntry("one", 1);
        Map.Entry<String, Integer> absent = getEntry("three", 3);
        assertTrue(entrySet.contains(present), "entrySet should contain an existing entry");
        assertFalse(entrySet.contains(absent), "entrySet should not contain a non‑existing entry");
    }

    @Test
    void entrySetRemoveRemovesEntryAndUpdatesSize() {
        map.put("a", 10);
        map.put("b", 20);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Map.Entry<String, Integer> toRemove = getEntry("a", 10);
        assertTrue(entrySet.remove(toRemove), "remove should return true for an existing entry");
        assertEquals(1, map.size(), "Map size must be decreased after removal");
        assertFalse(map.containsKey("a"), "Key \"a\" must no longer be present");
    }

    @Test
    void entrySetRemoveReturnsFalseWhenNotPresent() {
        map.put("x", 5);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Map.Entry<String, Integer> notPresent = getEntry("y", 5);
        assertFalse(entrySet.remove(notPresent), "remove should return false for a missing entry");
        assertEquals(1, map.size(), "Map size must stay unchanged");
    }

    @Test
    void entrySetRemoveAllRemovesAllSpecifiedEntries() {
        map.put("k1", 1);
        map.put("k2", 2);
        map.put("k3", 3);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Collection<Map.Entry<String, Integer>> toRemove = List.of(getEntry("k1", 1), getEntry("k2", 2));
        assertTrue(entrySet.removeAll(toRemove), "removeAll should report a change");
        assertEquals(1, map.size(), "Only one entry should remain");
        assertTrue(map.containsKey("k3"));
        assertFalse(map.containsKey("k1"));
        assertFalse(map.containsKey("k2"));
    }

    @Test
    void entrySetRetainAllKeepsOnlySpecifiedEntries() {
        map.put("a", 100);
        map.put("b", 200);
        map.put("c", 300);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Collection<Map.Entry<String, Integer>> toRetain = List.of(getEntry("b", 200), getEntry("c", 300));
        assertTrue(entrySet.retainAll(toRetain), "retainAll should report a change");
        assertEquals(2, map.size(), "Map should contain exactly two entries after retainAll");
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("a"));
    }

    @Test
    void entrySetClearEmptiesMap() {
        map.put("x", 42);
        map.put("y", 84);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        entrySet.clear();
        assertTrue(map.isEmpty(), "Map must be empty after entrySet.clear()");
        assertEquals(0, entrySet.size(), "entrySet size must be zero after clear");
    }

    @Test
    void iteratorRemoveWorksCorrectly() {
        map.put("p", 1);
        map.put("q", 2);
        map.put("r", 3);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("q".equals(e.getKey())) {
                // remove via iterator
                it.remove();
            }
        }
        assertEquals(2, map.size(), "Map size must reflect iterator removal");
        assertFalse(map.containsKey("q"));
        assertTrue(map.containsKey("p"));
        assertTrue(map.containsKey("r"));
    }

    // ---------- contains ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideContainsCases")
    void entrySetContains(String description, Map.Entry<String, Integer> entry, boolean expected) {
        // Populate map with the entries required for the test
        map.put("foo", 1);
        map.put("nullKey", null);
        map.put("nonNullKey", 99);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertEquals(expected, entrySet.contains(entry), description);
    }

    // ---------- remove ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideRemoveCases")
    void entrySetRemove(String description, Object toRemove, boolean expected) {
        map.put("toRemove", 42);
        map.put("other", 99);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        boolean result = entrySet.remove(toRemove);
        assertEquals(expected, result, description);
        if (expected) {
            assertFalse(map.containsKey("toRemove"));
            assertEquals(1, map.size());
        } else {
            assertTrue(map.containsKey("toRemove"));
            assertEquals(2, map.size());
        }
    }

    // ---------- removeAll ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideRemoveAllCases")
    void entrySetRemoveAll(String description, Collection<?> toRemove, boolean expected) {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        boolean changed = entrySet.removeAll(toRemove);
        assertEquals(expected, changed, description);
    }

    // ---------- retainAll ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideRetainAllCases")
    void entrySetRetainAll(String description, Collection<?> toRetain, boolean expected) {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        boolean changed = entrySet.retainAll(toRetain);
        assertEquals(expected, changed, description);
    }

    // ---------- clear and size ----------
    @Test
    void clearViaEntrySetAndSizeReflectsMap() {
        map.put("x", 42);
        map.put("y", 84);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        entrySet.clear();
        assertTrue(map.isEmpty(), "Map must be empty after clear");
        assertEquals(0, entrySet.size(), "entrySet size must be zero after clear");
    }

    @Test
    void entrySetSizeDelegatesToMapSize() {
        map.put("one", 1);
        map.put("two", 2);
        assertEquals(map.size(), map.entrySet().size(), "entrySet.size() should delegate to CompactMap.size()");
    }

    // ---------- iterator behavior ----------
    @Test
    void iteratorOnEmptyMapHasNoNext() {
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertFalse(it.hasNext(), "Iterator on empty map must report hasNext == false");
    }

    @Test
    void iteratorNextReturnsCorrectEntry() {
        map.put("k1", 100);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertTrue(it.hasNext(), "Iterator should have next for a non‑empty map");
        Map.Entry<String, Integer> e = it.next();
        assertEquals("k1", e.getKey());
        assertEquals(100, e.getValue());
    }

    @Test
    void iteratorRemoveWithoutNextThrows() {
        map.put("a", 1);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    void iteratorRemoveDecreasesSizeAndHasNextReflectsRemaining() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        // a
        Map.Entry<String, Integer> first = it.next();
        // remove a
        it.remove();
        assertEquals(1, map.size(), "Size should be decreased after iterator.remove()");
        assertTrue(it.hasNext(), "Iterator should still have next element (b)");
        Map.Entry<String, Integer> second = it.next();
        assertEquals("b", second.getKey());
    }

    @Test
    void iteratorRemoveTwiceWithoutNextThrows() {
        map.put("a", 1);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        // point to first element
        it.next();
        // first removal ok
        it.remove();
        assertThrows(IllegalStateException.class, it::remove, "Second consecutive remove() without next() must throw IllegalStateException");
    }

    @Test
    void iteratorConcurrentModificationDetected() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        // Modify the map directly after iterator creation
        map.put("c", 3);
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must detect concurrent modification and throw CME");
    }

    // ---------- removeMismatchedValue ----------
    @Test
    void removeMismatchedValueRemovesBasedOnKeyOnly() {
        map.put("present", 10);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        // Value differs from stored value, but removal should still succeed
        boolean removed = entrySet.remove(new SimpleEntry<>("present", 999));
        assertTrue(removed, "Removal should succeed even when supplied value differs");
        assertFalse(map.containsKey("present"));
    }

    @Test
    void removeMismatchedValueKeyAbsentReturnsFalse() {
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        boolean removed = entrySet.remove(new SimpleEntry<>("absent", 123));
        assertFalse(removed, "Removal must return false when key is absent");
    }

    // ---------- containsValueMismatch ----------
    @Test
    void containsValueMismatchNullStoredValue() {
        map.put("nullKey", null);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertFalse(entrySet.contains(new SimpleEntry<>("nullKey", 1)), "Entry with same key but non‑null value must not be considered contained");
    }

    @Test
    void containsValueMismatchNonNullStoredValue() {
        map.put("nonNullKey", 5);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertFalse(entrySet.contains(new SimpleEntry<>("nonNullKey", null)), "Entry with same key but null value must not be considered contained");
    }

    // ---------- size consistency after iterator.remove ----------
    @Test
    void sizeConsistencyAfterIteratorRemove() {
        map.put("x", 1);
        map.put("y", 2);
        int originalSize = map.size();
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        // advance to first entry
        it.next();
        it.remove();
        assertEquals(originalSize - 1, map.size(), "Map size must be originalSize - 1 after iterator.remove()");
    }

    // ---------- clear and size via iterator's entrySet ----------
    @Test
    void clearViaEntrySetObtainedFromIteratorEmptiesMap() {
        map.put("alpha", 10);
        map.put("beta", 20);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        // Clear through the Set view (the same view that an iterator would be based on)
        entrySet.clear();
        assertTrue(map.isEmpty(), "Map must be empty after entrySet.clear()");
        assertEquals(0, entrySet.size(), "EntrySet size must be zero after clear");
    }

    // ---------- null collection handling ----------
    @Test
    void removeAllNullCollectionThrowsNPE() {
        map.put("a", 1);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.removeAll(null), "removeAll(null) must throw NullPointerException");
    }

    @Test
    void retainAllNullCollectionThrowsNPE() {
        map.put("a", 1);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.retainAll(null), "retainAll(null) must throw NullPointerException");
    }

    // ---------- add operation unsupported ----------
    @Test
    void entrySetAddUnsupported() {
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Map.Entry<String, Integer> newEntry = new SimpleEntry<>("newKey", 123);
        assertThrows(UnsupportedOperationException.class, () -> entrySet.add(newEntry), "entrySet().add(entry) must throw UnsupportedOperationException");
    }
}
