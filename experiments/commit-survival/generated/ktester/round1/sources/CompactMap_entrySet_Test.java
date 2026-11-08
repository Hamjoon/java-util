package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_entrySet_Test {

    private CompactMap<String, Integer> map;

    /**
     * A concrete CompactMap that forces a copy‑iterator and a small compactSize
     *  so we can test both iterator branches without relying on internal state.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final boolean copyIter;

        TestCompactMap(boolean copyIter) {
            this.copyIter = copyIter;
        }

        @Override
        protected boolean useCopyIterator() {
            return copyIter;
        }

        /**
         * Keep the compact size low so the map stays in the array representation
         *  for the first few entries (makes the tests deterministic).
         */
        @Override
        protected int compactSize() {
            return 4;
        }

        /**
         * Use a plain HashMap when the map grows beyond compactSize.
         */
        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }

        /**
         * Use the same map size for the copy‑iterator case.
         */
        @Override
        protected Map<K, V> getNewMap(int size) {
            return new HashMap<>(size);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a non‑copy iterator for the majority of tests.
        map = new TestCompactMap<>(false);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    /**
     * Populate the current {@code map} with the supplied key/value pairs.
     */
    private void populateMap(List<Object[]> entries) {
        for (Object[] kv : entries) {
            @SuppressWarnings("unchecked")
            K key = (K) kv[0];
            @SuppressWarnings("unchecked")
            V value = (V) kv[1];
            map.put(key, value);
        }
    }

    /**
     * Convert a collection description (list of maps) into a {@code Collection<Object>}
     *  containing {@link Map.Entry} objects.
     */
    private static Collection<Object> toEntryCollection(List<Map<String, Object>> raw) {
        Collection<Object> c = new ArrayList<>();
        for (Map<String, Object> m : raw) {
            Object k = m.get("key");
            Object v = m.get("value");
            c.add(new AbstractMap.SimpleEntry<>(k, v));
        }
        return c;
    }

    private static Stream<Arguments> iteratorSelectionProvider() {
        return Stream.of(Arguments.of(true, List.of(new Object[] { "a", 1 }, new Object[] { "b", 2 }), "CopyEntryIterator"), Arguments.of(false, List.of(new Object[] { "x", "X" }, new Object[] { "y", "Y" }), "CompactEntryIterator"));
    }

    private static Stream<Arguments> containsMethodProvider() {
        return Stream.of(// key exists, non‑null stored value, equal entry value
        Arguments.of(List.of(new Object[] { "k1", "v1" }, new Object[] { "k2", null }), Map.of("key", "k1", "value", "v1"), true), // key exists, values differ
        Arguments.of(List.of(new Object[] { "k1", "v1" }), Map.of("key", "k1", "value", "different"), false), // stored null, entry null
        Arguments.of(List.of(new Object[] { "kNull", null }), Map.of("key", "kNull", "value", null), true), // key absent
        Arguments.of(List.of(new Object[] { "present", "value" }), Map.of("key", "absent", "value", "value"), false), // not an entry object
        Arguments.of(List.of(new Object[] { "a", 1 }), "not a Map.Entry", false));
    }

    private static Stream<Arguments> removeMethodProvider() {
        return Stream.of(// entry present, correct key/value
        Arguments.of(List.of(new Object[] { "rem1", 10 }, new Object[] { "rem2", 20 }), Map.of("key", "rem1", "value", 10), true), // key absent
        Arguments.of(List.of(new Object[] { "only", "one" }), Map.of("key", "missing", "value", "value"), false), // not an entry object
        Arguments.of(List.of(new Object[] { "a", 1 }), "plain string", false));
    }

    private static Stream<Arguments> removeAllMethodProvider() {
        return Stream.of(// one matching entry
        Arguments.of(List.of(new Object[] { "r1", "v1" }, new Object[] { "r2", "v2" }, new Object[] { "r3", "v3" }), List.of(Map.of("key", "r1", "value", "v1"), Map.of("key", "nonexistent", "value", "x")), true), // no matching entries
        Arguments.of(List.of(new Object[] { "a", 1 }, new Object[] { "b", 2 }), List.of(Map.of("key", "c", "value", 3)), false));
    }

    private static Stream<Arguments> retainAllMethodProvider() {
        return Stream.of(// empty collection -> all entries removed
        Arguments.of(List.of(new Object[] { "k1", "v1" }, new Object[] { "k2", "v2" }, new Object[] { "k3", "v3" }), Collections.emptyList(), true), // mixed keep/drop
        Arguments.of(List.of(new Object[] { "keep", "same" }, new Object[] { "dropKey", "any" }, new Object[] { "dropValue", "old" }), List.of(Map.of("key", "keep", "value", "same"), Map.of("key", "dropValue", "value", "new")), true), // everything matches -> no change
        Arguments.of(List.of(new Object[] { "a", 1 }, new Object[] { "b", 2 }), List.of(Map.of("key", "a", "value", 1), Map.of("key", "b", "value", 2)), false));
    }

    private static Stream<Arguments> sizeAndClearProvider() {
        return Stream.of(Arguments.of(List.of(new Object[] { "one", 1 }, new Object[] { "two", 2 }, new Object[] { "three", 3 }), 3, 0), Arguments.of(Collections.emptyList(), 0, 0));
    }

    private static Stream<Arguments> iteratorRemoveProvider() {
        return Stream.of(Arguments.of(true, List.of(new Object[] { "a", 10 }, new Object[] { "b", 20 })), Arguments.of(false, List.of(new Object[] { "x", "X" }, new Object[] { "y", "Y" })));
    }

    private static Stream<Arguments> containsEdgeCasesProvider() {
        return Stream.of(// null stored value, non‑null entry value -> false
        Arguments.of(List.of(new Object[] { "nullKey", null }), Map.of("key", "nullKey", "value", "notNull"), false), // entry key null, not present in map -> false
        Arguments.of(List.of(new Object[] { "present", "value" }), Map.of("key", null, "value", "value"), false), // key exists with non‑null stored value, entry value null -> false
        Arguments.of(List.of(new Object[] { "k", "v" }), Map.of("key", "k", "value", null), false));
    }

    private static Stream<Arguments> removeEdgeCasesProvider() {
        return Stream.of(// key present, value differs -> still removed
        Arguments.of(List.of(new Object[] { "key1", "val1" }, new Object[] { "key2", "val2" }), Map.of("key", "key1", "value", "different"), true), // key present with null value, entry value non‑null -> removed
        Arguments.of(List.of(new Object[] { "k", null }), Map.of("key", "k", "value", "something"), true));
    }

    private static Stream<Arguments> removeAllEdgeCasesProvider() {
        return Stream.of(// collection contains no Map.Entry objects -> no change
        Arguments.of(List.of(new Object[] { "a", 1 }, new Object[] { "b", 2 }), List.of("plainString", 123, Map.of("not", "entry")), false), // collection contains matching entries -> all removed
        Arguments.of(List.of(new Object[] { "x", "X" }, new Object[] { "y", "Y" }), List.of(Map.of("key", "x", "value", "X"), Map.of("key", "y", "value", "Y")), true));
    }

    private static Stream<Arguments> retainAllEdgeCasesProvider() {
        return Stream.of(// collection with no Map.Entry objects -> all map entries removed
        Arguments.of(List.of(new Object[] { "k1", "v1" }, new Object[] { "k2", "v2" }), List.of("justAString", 42), true), // collection contains matching entry with null value -> no change
        Arguments.of(List.of(new Object[] { "k", null }), List.of(Map.of("key", "k", "value", null)), false));
    }

    private static Stream<Arguments> concurrentModificationProvider() {
        return Stream.of(Arguments.of(false, List.of(new Object[] { "c1", 1 }, new Object[] { "c2", 2 })), Arguments.of(true, List.of(new Object[] { "cA", "A" }, new Object[] { "cB", "B" })));
    }

    @Test
    void entrySetSizeReflectsMapSize() {
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertEquals(3, entrySet.size(), "entrySet size should match map size");
    }

    @Test
    void entrySetIteratorUsesCopyIteratorWhenEnabled() {
        // Create a map that forces the copy iterator.
        CompactMap<String, Integer> copyMap = new TestCompactMap<>(true);
        copyMap.put("a", 10);
        copyMap.put("b", 20);
        Iterator<Map.Entry<String, Integer>> it = copyMap.entrySet().iterator();
        // The concrete class name is not part of the public contract,
        // but we can verify behaviour: the iterator must support remove().
        assertTrue(it.hasNext(), "Iterator should have elements");
        Map.Entry<String, Integer> first = it.next();
        assertNotNull(first, "First entry should not be null");
        // Ensure remove works without throwing.
        it.remove();
        assertEquals(1, copyMap.size(), "Size should decrease after iterator.remove()");
    }

    @Test
    void entrySetContainsDetectsExistingEntry() {
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Map.Entry<String, Integer> expected = new AbstractMap.SimpleEntry<>("two", 2);
        assertTrue(entrySet.contains(expected), "entrySet should contain the exact entry");
    }

    @Test
    void entrySetContainsReturnsFalseForNonMatchingValue() {
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Map.Entry<String, Integer> wrong = new AbstractMap.SimpleEntry<>("two", 99);
        assertFalse(entrySet.contains(wrong), "entrySet should not contain entry with mismatched value");
    }

    @Test
    void entrySetRemoveDeletesEntry() {
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Map.Entry<String, Integer> toRemove = new AbstractMap.SimpleEntry<>("three", 3);
        assertTrue(entrySet.remove(toRemove), "remove should report successful deletion");
        assertFalse(map.containsKey("three"), "Map should no longer contain the removed key");
        assertEquals(2, map.size(), "Map size should be reduced after removal");
    }

    @Test
    void entrySetRemoveAllClearsMatchingEntriesOnly() {
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Collection<Object> toRemove = new ArrayList<>();
        toRemove.add(new AbstractMap.SimpleEntry<>("one", 1));
        // should be ignored
        toRemove.add("not an entry");
        assertTrue(entrySet.removeAll(toRemove), "removeAll should report a change");
        assertFalse(map.containsKey("one"));
        assertTrue(map.containsKey("two"));
        assertEquals(2, map.size());
    }

    @Test
    void entrySetRetainAllKeepsOnlySpecifiedEntries() {
        // Prepare a collection that keeps only "two"
        Collection<Object> retain = new ArrayList<>();
        retain.add(new AbstractMap.SimpleEntry<>("two", 2));
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertTrue(entrySet.retainAll(retain), "retainAll should report a change");
        assertFalse(map.containsKey("one"));
        assertTrue(map.containsKey("two"));
        assertFalse(map.containsKey("three"));
        assertEquals(1, map.size());
    }

    @Test
    void entrySetClearEmptiesMap() {
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        entrySet.clear();
        assertTrue(map.isEmpty(), "Map should be empty after entrySet.clear()");
        assertEquals(0, entrySet.size(), "entrySet size should be zero after clear");
    }

    @ParameterizedTest(name = "[{index}] useCopyIterator={0} expects {2}")
    @MethodSource("iteratorSelectionProvider")
    void iteratorSelection(boolean useCopy, List<Object[]> initialEntries, String expectedClassFragment) {
        map = new TestCompactMap<>(useCopy);
        populateMap(initialEntries);
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        // The concrete iterator class is package‑private; we verify via its name.
        String iteratorClassName = it.getClass().getSimpleName();
        assertTrue(iteratorClassName.contains(expectedClassFragment), () -> "Iterator class name [" + iteratorClassName + "] should contain " + expectedClassFragment);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("containsMethodProvider")
    void containsMethod(List<Object[]> initialEntries, Object testEntry, boolean expected) {
        populateMap(initialEntries);
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        Object entryObject;
        if (testEntry instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) testEntry;
            entryObject = new AbstractMap.SimpleEntry<>(m.get("key"), m.get("value"));
        } else {
            // e.g., a plain String
            entryObject = testEntry;
        }
        assertEquals(expected, entrySet.contains(entryObject), () -> "contains() should return " + expected + " for entry " + entryObject);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("removeMethodProvider")
    void removeMethod(List<Object[]> initialEntries, Object testEntry, boolean expected) {
        populateMap(initialEntries);
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        Object entryObject;
        if (testEntry instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) testEntry;
            entryObject = new AbstractMap.SimpleEntry<>(m.get("key"), m.get("value"));
        } else {
            entryObject = testEntry;
        }
        boolean result = entrySet.remove(entryObject);
        assertEquals(expected, result, "remove() should return " + expected);
        // When removal succeeded, the key must be gone.
        if (expected) {
            @SuppressWarnings("unchecked")
            K key = ((Map.Entry<String, Object>) entryObject).getKey();
            assertFalse(map.containsKey(key), "Key should be removed from map");
        }
    }

    @ParameterizedTest(name = "[{index}] expects change={1}")
    @MethodSource("removeAllMethodProvider")
    void removeAllMethod(List<Object[]> initialEntries, List<Map<String, Object>> rawCollection, boolean expected) {
        populateMap(initialEntries);
        Collection<Object> collection = toEntryCollection(rawCollection);
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        boolean changed = entrySet.removeAll(collection);
        assertEquals(expected, changed, "removeAll() should report change=" + expected);
        if (expected) {
            // Verify that the known removed key is absent.
            for (Map<String, Object> m : rawCollection) {
                if (map.containsKey(m.get("key"))) {
                    // key still present – it must have not matched the value.
                    assertNotEquals(m.get("value"), map.get(m.get("key")));
                }
            }
        }
    }

    @ParameterizedTest(name = "[{index}] expects change={2}")
    @MethodSource("retainAllMethodProvider")
    void retainAllMethod(List<Object[]> initialEntries, List<Map<String, Object>> rawCollection, boolean expected) {
        populateMap(initialEntries);
        Collection<Object> collection = toEntryCollection(rawCollection);
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        boolean changed = entrySet.retainAll(collection);
        assertEquals(expected, changed, "retainAll() should report change=" + expected);
        if (expected) {
            // After retainAll, only entries present in collection with matching values should remain.
            for (Object[] kv : initialEntries) {
                String key = (String) kv[0];
                Object value = kv[1];
                boolean shouldStay = rawCollection.stream().anyMatch(m -> Objects.equals(m.get("key"), key) && Objects.equals(m.get("value"), value));
                assertEquals(shouldStay, map.containsKey(key), "Key [" + key + "] presence after retainAll is incorrect");
            }
        }
    }

    @ParameterizedTest(name = "[{index}] sizeBefore={1}, sizeAfter={2}")
    @MethodSource("sizeAndClearProvider")
    void sizeAndClear(List<Object[]> initialEntries, int expectedBefore, int expectedAfter) {
        populateMap(initialEntries);
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        assertEquals(expectedBefore, entrySet.size(), "entrySet size before clear");
        entrySet.clear();
        assertEquals(expectedAfter, entrySet.size(), "entrySet size after clear");
        assertTrue(map.isEmpty(), "Map should be empty after clear");
    }

    @ParameterizedTest(name = "[{index}] useCopyIterator={0}")
    @MethodSource("iteratorRemoveProvider")
    void iteratorRemove(boolean useCopy, List<Object[]> initialEntries) {
        map = new TestCompactMap<>(useCopy);
        populateMap(initialEntries);
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        assertTrue(it.hasNext(), "Iterator should have a first element");
        Map.Entry<String, Object> first = it.next();
        // remove the element we just visited
        it.remove();
        // The map size must have decreased by one.
        assertEquals(initialEntries.size() - 1, map.size(), "Map size after iterator.remove() should be decreased");
        // The removed key must no longer be present.
        assertFalse(map.containsKey(first.getKey()));
    }

    @ParameterizedTest(name = "[{index}] expects={2}")
    @MethodSource("containsEdgeCasesProvider")
    void containsEdgeCases(List<Object[]> initialEntries, Map<String, Object> testEntry, boolean expected) {
        populateMap(initialEntries);
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        Map.Entry<String, Object> entry = new AbstractMap.SimpleEntry<>(testEntry.get("key"), testEntry.get("value"));
        assertEquals(expected, entrySet.contains(entry), "contains() edge case expected " + expected);
    }

    @ParameterizedTest(name = "[{index}] expects={2}")
    @MethodSource("removeEdgeCasesProvider")
    void removeEdgeCases(List<Object[]> initialEntries, Map<String, Object> testEntry, boolean expected) {
        populateMap(initialEntries);
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        Map.Entry<String, Object> entry = new AbstractMap.SimpleEntry<>(testEntry.get("key"), testEntry.get("value"));
        boolean result = entrySet.remove(entry);
        assertEquals(expected, result, "remove() edge case expected " + expected);
        assertFalse(map.containsKey(testEntry.get("key")), "Key should be absent after removal");
    }

    @ParameterizedTest(name = "[{index}] expects change={2}")
    @MethodSource("removeAllEdgeCasesProvider")
    void removeAllEdgeCases(List<Object[]> initialEntries, List<Object> rawCollection, boolean expected) {
        populateMap(initialEntries);
        // Build a collection that mixes entries and non‑entries.
        Collection<Object> collection = new ArrayList<>();
        for (Object o : rawCollection) {
            if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) o;
                collection.add(new AbstractMap.SimpleEntry<>(m.get("key"), m.get("value")));
            } else {
                collection.add(o);
            }
        }
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        boolean changed = entrySet.removeAll(collection);
        assertEquals(expected, changed, "removeAll() edge case expected change=" + expected);
    }

    @ParameterizedTest(name = "[{index}] expects change={2}")
    @MethodSource("retainAllEdgeCasesProvider")
    void retainAllEdgeCases(List<Object[]> initialEntries, List<Object> rawCollection, boolean expected) {
        populateMap(initialEntries);
        Collection<Object> collection = new ArrayList<>();
        for (Object o : rawCollection) {
            if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) o;
                collection.add(new AbstractMap.SimpleEntry<>(m.get("key"), m.get("value")));
            } else {
                collection.add(o);
            }
        }
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        boolean changed = entrySet.retainAll(collection);
        assertEquals(expected, changed, "retainAll() edge case expected change=" + expected);
    }

    @ParameterizedTest(name = "[{index}] useCopyIterator={0}")
    @MethodSource("concurrentModificationProvider")
    void concurrentModification(boolean useCopy, List<Object[]> initialEntries) {
        map = new TestCompactMap<>(useCopy);
        populateMap(initialEntries);
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        // modify the map after iterator creation
        map.put("newKey", "newValue");
        // The iterator should now be fail‑fast.
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator should throw ConcurrentModificationException after map modification");
    }

    @Test
    void nullCollectionRemoveAllThrowsNPE() {
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        assertThrows(NullPointerException.class, () -> entrySet.removeAll(null), "removeAll(null) must throw NullPointerException");
    }
}
