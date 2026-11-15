package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

@DisplayName("CompactMap.entrySet() behaviour tests")
class CompactMap_entrySet_Test {

    /**
     * The map under test – a small CompactMap that will use the compact array storage.
     */
    private CompactMap<String, Integer> map;

    /**
     * Create a CompactMap that forces the compact‑array implementation.
     * <p>
     *   The overridden {@code compactSize()} returns {@code 3}, therefore
     *   when we add up to three entries the map stays in the compact state.
     *   The {@code getNewMap()} method returns a {@link LinkedHashMap} which
     *   will be used only when the map grows beyond the compact size.
     * </p>
     */
    @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                // force compact array for up to 3 entries
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // backing map when we exceed compactSize
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        map.clear();
    }

    private static Stream<Arguments> provideContainsScenarios() {
        return Stream.of(// 1. key present, non‑null value matches
        Arguments.of(new AbstractMap.SimpleEntry<>("key1", 1), true, "Map contains the key with a non‑null value equal to the entry's value"), // 2. key present, stored value null, entry value null
        Arguments.of(new AbstractMap.SimpleEntry<>("key2", null), true, "Map contains the key whose stored value is null – equality with null returns true"), // 3. key missing
        Arguments.of(new AbstractMap.SimpleEntry<>("missing", 2), false, "Key is not present in the map"), // 4. not a Map.Entry instance
        Arguments.of("not an entry", false, "Parameter is not a Map.Entry"));
    }

    private CompactMap<String, String> createTreeMapBackedCompactMap() {
        // Use the static factory that accepts configuration options
        Map<String, Object> options = Map.of(CompactMap.MAP_TYPE, TreeMap.class);
        @SuppressWarnings("unchecked")
        CompactMap<String, String> treeMapBacked = CompactMap.<String, String>newMap(options);
        // Ensure the map is not empty so that the internal TreeMap is instantiated
        treeMapBacked.put("dummy", "value");
        return treeMapBacked;
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("entrySet size matches map size and iterator returns all entries")
    void testEntrySetSizeAndIteration() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertEquals(map.size(), entrySet.size(), "entrySet size must be identical to map size");
        // Collect entries from iterator and compare with map.entrySet()
        Set<Map.Entry<String, Integer>> iterated = new HashSet<>();
        for (Map.Entry<String, Integer> e : entrySet) {
            iterated.add(e);
        }
        assertEquals(new HashSet<>(map.entrySet()), iterated, "Iterator must traverse exactly the same entries as map.entrySet()");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("entrySet.contains correctly identifies present and absent entries")
    void testContainsEntry() {
        map.put("x", 42);
        map.put("y", 99);
        Map.Entry<String, Integer> present = new AbstractMap.SimpleEntry<>("x", 42);
        Map.Entry<String, Integer> absentKey = new AbstractMap.SimpleEntry<>("z", 0);
        Map.Entry<String, Integer> absentValue = new AbstractMap.SimpleEntry<>("y", 0);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertTrue(entrySet.contains(present), "Should contain the exact entry");
        assertFalse(entrySet.contains(absentKey), "Should not contain a non‑existent key");
        assertFalse(entrySet.contains(absentValue), "Should not contain an entry with wrong value");
        assertFalse(entrySet.contains("not an entry"), "Non‑Entry objects must return false");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("entrySet.remove removes the entry from the map")
    void testRemoveEntry() {
        map.put("k1", 10);
        map.put("k2", 20);
        Map.Entry<String, Integer> toRemove = new AbstractMap.SimpleEntry<>("k1", 10);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertTrue(entrySet.remove(toRemove), "remove must return true for existing entry");
        assertFalse(map.containsKey("k1"), "Key must be removed from the map");
        assertEquals(1, map.size(), "Map size must be decremented");
        assertFalse(entrySet.contains(toRemove), "Removed entry must no longer be reported as present");
    }

    @Test
    @DisplayName("entrySet.remove returns false for non‑Entry objects")
    void testRemoveNonEntry() {
        map.put("a", 1);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertFalse(entrySet.remove("not an entry"));
        assertEquals(1, map.size());
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("entrySet.removeAll removes all matching entries")
    void testRemoveAll() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        List<Map.Entry<String, Integer>> toRemove = List.of(new AbstractMap.SimpleEntry<>("one", 1), new AbstractMap.SimpleEntry<>("two", 2), // entry that does not exist – should be ignored
        new AbstractMap.SimpleEntry<>("four", 4));
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertTrue(entrySet.removeAll(toRemove), "removeAll must return true when something is removed");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("three"));
        assertFalse(map.containsKey("one"));
        assertFalse(map.containsKey("two"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("entrySet.retainAll keeps only the entries present in the collection")
    void testRetainAll() {
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);
        map.put("d", 40);
        // Keep only a and c (note: we deliberately use a wrong value for c to verify value check)
        List<Map.Entry<String, Integer>> toRetain = List.of(new AbstractMap.SimpleEntry<>("a", 10), // wrong value – should cause removal of c
        new AbstractMap.SimpleEntry<>("c", 99));
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertTrue(entrySet.retainAll(toRetain), "retainAll must return true when map changes");
        // After retainAll only entry "a" should survive because "c" had mismatching value
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
        assertEquals(10, map.get("a"));
        assertFalse(map.containsKey("b"));
        assertFalse(map.containsKey("c"));
        assertFalse(map.containsKey("d"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("entrySet.clear empties the map")
    void testClearThroughEntrySet() {
        map.put("x", 1);
        map.put("y", 2);
        assertFalse(map.isEmpty());
        map.entrySet().clear();
        assertTrue(map.isEmpty(), "Map must be empty after entrySet.clear()");
        assertEquals(0, map.size());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("provideContainsScenarios")
    @DisplayName("entrySet.contains() branch coverage")
    void testContainsBranchCoverage(Object entryObj, boolean expected, String description) {
        // Populate map for the first two cases
        map.put("key1", 1);
        // explicit null value
        map.put("key2", null);
        Set<?> entrySet = map.entrySet();
        boolean result = entrySet.contains(entryObj);
        assertEquals(expected, result, description);
    }

    @Test
    @DisplayName("iterator() returns all entries (basic iteration)")
    void testIteratorBasicIteration() {
        map.put("a", 1);
        map.put("b", 2);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Iterator<Map.Entry<String, Integer>> it = entrySet.iterator();
        Set<Map.Entry<String, Integer>> collected = new HashSet<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        Set<Map.Entry<String, Integer>> expected = Set.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2));
        assertEquals(expected, collected, "Iterator should return exactly the inserted entries");
    }

    @Test
    @DisplayName("iterator().remove() removes the last returned entry")
    void testIteratorRemove() {
        map.put("x", 10);
        map.put("y", 20);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Iterator<Map.Entry<String, Integer>> it = entrySet.iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> first = it.next();
        // remove the entry we just visited
        it.remove();
        assertEquals(1, map.size(), "Map size must decrement after iterator.remove()");
        assertFalse(map.containsKey(first.getKey()), "Removed key must no longer be present");
    }

    @Test
    @DisplayName("iterator().next() on empty map throws NoSuchElementException")
    void testIteratorNextOnEmpty() {
        assertTrue(map.isEmpty(), "Map should be empty for this test");
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    @Test
    @DisplayName("iterator().remove() before next() throws IllegalStateException")
    void testIteratorRemoveBeforeNext() {
        map.put("p", 1);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    @DisplayName("iterator().remove() called twice throws IllegalStateException")
    void testIteratorDoubleRemove() {
        map.put("p", 1);
        map.put("q", 2);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        // first element
        it.next();
        // first removal – ok
        it.remove();
        // second removal without an intervening next()
        assertThrows(IllegalStateException.class, it::remove, "Second remove() without next() must throw IllegalStateException");
    }

    @Test
    @DisplayName("iterator detects concurrent modification")
    void testIteratorConcurrentModification() {
        map.put("k1", 1);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        // Modify the map after iterator creation
        map.put("k2", 2);
        // The next call on the iterator must notice the structural change
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must throw ConcurrentModificationException after concurrent modification");
    }

    @Test
    @DisplayName("entrySet.contains() with null key on TreeMap throws NullPointerException")
    void testContainsNullKeyException() {
        CompactMap<String, String> treeMapMap = createTreeMapBackedCompactMap();
        Map.Entry<String, String> entryWithNullKey = new AbstractMap.SimpleEntry<>(null, "value");
        assertThrows(NullPointerException.class, () -> treeMapMap.entrySet().contains(entryWithNullKey), "TreeMap does not permit null keys – contains() should throw NPE");
    }

    @Test
    @DisplayName("entrySet.remove() with null key on TreeMap throws NullPointerException")
    void testRemoveNullKeyException() {
        CompactMap<String, String> treeMapMap = createTreeMapBackedCompactMap();
        Map.Entry<String, String> entryWithNullKey = new AbstractMap.SimpleEntry<>(null, "value");
        assertThrows(NullPointerException.class, () -> treeMapMap.entrySet().remove(entryWithNullKey), "TreeMap does not permit null keys – remove() should throw NPE");
    }

    @Test
    @DisplayName("entrySet.retainAll() with collection containing null key throws NullPointerException")
    void testRetainAllNullKeyException() {
        CompactMap<String, String> treeMapMap = createTreeMapBackedCompactMap();
        // Add a normal entry to make retainAll meaningful
        treeMapMap.put("valid", "v");
        Collection<Map.Entry<String, String>> collectionWithNullKey = List.of(new AbstractMap.SimpleEntry<>(null, "v"));
        assertThrows(NullPointerException.class, () -> treeMapMap.entrySet().retainAll(collectionWithNullKey), "During retainAll, putting a null‑key entry into a TreeMap‑backed map must throw NPE");
    }
}
