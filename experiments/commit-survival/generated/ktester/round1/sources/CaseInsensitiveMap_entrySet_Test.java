package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_entrySet_Test {

    private CaseInsensitiveMap<String, Integer> ciMap;

    private Set<Map.Entry<String, Integer>> entrySet;

    @BeforeEach
    void setupBeforeEach() {
        ciMap = new CaseInsensitiveMap<>();
        ciMap.put("key1", "value1");
        ciMap.put("key2", "value2");
        ciMap.put("key3", "value3");
        entrySet = ciMap.entrySet();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure map is cleared after each test to avoid cross‑test contamination
        ciMap.clear();
    }

    private static Stream<Arguments> provideContainsCases() {
        return Stream.of(// not a Map.Entry
        Arguments.of("not an entry", false), // key missing from map
        Arguments.of(new AbstractMap.SimpleEntry<>("missingKey", "value"), false), // key present but value different
        Arguments.of(new AbstractMap.SimpleEntry<>("key1", "differentValue"), false), // exact match
        Arguments.of(new AbstractMap.SimpleEntry<>("key1", "value1"), true));
    }

    private static Stream<Arguments> provideRemoveCases() {
        return Stream.of(// not a Map.Entry
        Arguments.of(12345, false), // existing key (case‑insensitive)
        Arguments.of(new AbstractMap.SimpleEntry<>("KEY2", "value2"), true), // non‑existing key
        Arguments.of(new AbstractMap.SimpleEntry<>("nonExisting", "value"), false));
    }

    @Test
    void entrySetSizeAndIsEmptyReflectMapState() {
        assertEquals(3, entrySet.size(), "Entry set size should match map size");
        assertFalse(entrySet.isEmpty(), "Entry set should not be empty");
        ciMap.clear();
        assertEquals(0, entrySet.size(), "After clear, entry set size should be 0");
        assertTrue(entrySet.isEmpty(), "After clear, entry set should be empty");
    }

    @Test
    void entrySetClearEmptiesUnderlyingMap() {
        entrySet.clear();
        assertTrue(ciMap.isEmpty(), "Clearing the entry set must clear the underlying map");
        assertTrue(entrySet.isEmpty(), "Entry set should be empty after clear");
    }

    @Test
    void entrySetContainsWorksCaseInsensitiveOnKey() {
        Map.Entry<String, Integer> exact = new AbstractMap.SimpleEntry<>("One", 1);
        assertTrue(entrySet.contains(exact), "Entry set should contain the exact entry");
        // Different case for the key – should still be found because the map is case‑insensitive
        Map.Entry<String, Integer> differentCase = new AbstractMap.SimpleEntry<>("ONE", 1);
        assertTrue(entrySet.contains(differentCase), "Entry set should contain entry with key case‑insensitively");
    }

    @Test
    void entrySetRemoveDeletesEntry() {
        Map.Entry<String, Integer> toRemove = new AbstractMap.SimpleEntry<>("Two", 2);
        boolean removed = entrySet.remove(toRemove);
        assertTrue(removed, "remove should return true when an entry is removed");
        assertFalse(ciMap.containsKey("Two"));
        assertEquals(2, ciMap.size());
        assertFalse(entrySet.contains(toRemove));
    }

    @Test
    void entrySetRemoveAllDeletesMultipleEntries() {
        Collection<Map.Entry<String, Integer>> toRemove = new ArrayList<>();
        toRemove.add(new AbstractMap.SimpleEntry<>("One", 1));
        // different case
        toRemove.add(new AbstractMap.SimpleEntry<>("THREE", 3));
        boolean changed = entrySet.removeAll(toRemove);
        assertTrue(changed, "removeAll should report a change when entries are removed");
        assertEquals(1, ciMap.size());
        assertFalse(ciMap.containsKey("One"));
        assertFalse(ciMap.containsKey("Three"));
        assertTrue(ciMap.containsKey("Two"));
    }

    @Test
    void entrySetRetainAllKeepsOnlySpecifiedEntries() {
        Collection<Map.Entry<String, Integer>> toRetain = new ArrayList<>();
        // case‑insensitive match
        toRetain.add(new AbstractMap.SimpleEntry<>("ONE", 1));
        // wrong value, should be removed
        toRetain.add(new AbstractMap.SimpleEntry<>("Two", 99));
        boolean changed = entrySet.retainAll(toRetain);
        assertTrue(changed, "retainAll should report a change when entries are removed");
        assertEquals(1, ciMap.size());
        assertTrue(ciMap.containsKey("One"));
        assertEquals(1, ciMap.get("One"));
        assertFalse(ciMap.containsKey("Two"));
        assertFalse(ciMap.containsKey("Three"));
    }

    @Test
    void iteratorProvidesCaseInsensitiveEntryObjects() {
        Iterator<Map.Entry<String, Integer>> it = entrySet.iterator();
        Set<String> seenKeys = new HashSet<>();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            // The key returned must be the original key (preserving case)
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
            seenKeys.add(entry.getKey());
            // Test iterator.remove()
            if ("Two".equals(entry.getKey())) {
                it.remove();
            }
        }
        assertEquals(2, ciMap.size(), "Map size should be reduced after iterator.remove()");
        assertFalse(ciMap.containsKey("Two"));
        assertTrue(seenKeys.containsAll(Arrays.asList("One", "Two", "Three")));
    }

    @Test
    void iteratorNextWrapsUnderlyingEntryInCaseInsensitiveEntry() {
        // The iterator returns a wrapper (CaseInsensitiveEntry). We cannot reference the class,
        // but we can verify its behavior through the Map.Entry contract.
        Iterator<Map.Entry<String, Integer>> it = entrySet.iterator();
        Map.Entry<String, Integer> first = it.next();
        assertEquals("One", first.getKey(), "Key should be the original case-preserved key");
        assertEquals(1, first.getValue());
        // Verify that containsKey works case‑insensitively using the map directly
        assertTrue(ciMap.containsKey("ONE"));
        assertEquals(1, ciMap.get("ONE"));
    }

    @ParameterizedTest(name = "{index}: contains({0}) → {1}")
    @MethodSource("provideContainsCases")
    void entrySetContains(Object input, boolean expected) {
        // The entrySet under test
        boolean actual = entrySet.contains(input);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "{index}: remove({0}) → {1}")
    @MethodSource("provideRemoveCases")
    void entrySetRemove(Object input, boolean expected) {
        // capture size before removal
        int sizeBefore = ciMap.size();
        boolean actual = entrySet.remove(input);
        assertEquals(expected, actual);
        // verify map size change only when expected == true
        if (expected) {
            assertEquals(sizeBefore - 1, ciMap.size(), "Map size must decrease by one when removal succeeds");
        } else {
            assertEquals(sizeBefore, ciMap.size(), "Map size must stay unchanged when removal fails");
        }
    }

    @Test
    void entrySetRemoveAllIgnoresNonEntriesAndReturnsFalseWhenNoChange() {
        Collection<Object> mixed = Arrays.asList("string", 42);
        boolean changed = entrySet.removeAll(mixed);
        assertFalse(changed, "removeAll should ignore non‑Entry objects and report no change");
        assertEquals(3, ciMap.size(), "Map size must stay unchanged");
    }

    @Test
    void entrySetRemoveAllRemovesExistingKeysAndReturnsTrue() {
        Collection<Map.Entry<String, String>> toRemove = Arrays.asList(new AbstractMap.SimpleEntry<>("key1", "value1"), new AbstractMap.SimpleEntry<>("key3", "value3"));
        boolean changed = entrySet.removeAll(toRemove);
        assertTrue(changed, "removeAll must report a change when entries are removed");
        assertEquals(1, ciMap.size());
        assertTrue(ciMap.containsKey("key2"));
        assertFalse(ciMap.containsKey("key1"));
        assertFalse(ciMap.containsKey("key3"));
    }

    @Test
    void entrySetRemoveAllWithOnlyMissingKeysReturnsFalse() {
        Collection<Map.Entry<String, String>> toRemove = Collections.singletonList(new AbstractMap.SimpleEntry<>("missing", "value"));
        boolean changed = entrySet.removeAll(toRemove);
        assertFalse(changed, "removeAll should return false when nothing is removed");
        assertEquals(3, ciMap.size());
    }

    @Test
    void entrySetRemoveAllNullCollectionThrowsNPE() {
        assertThrows(NullPointerException.class, () -> entrySet.removeAll(null), "Calling removeAll(null) must throw NullPointerException");
    }

    @Test
    void entrySetRetainAllWithEmptyCollectionClearsMap() {
        boolean changed = entrySet.retainAll(Collections.emptyList());
        assertTrue(changed, "retainAll(empty) must clear the map and report a change");
        assertTrue(ciMap.isEmpty());
    }

    @Test
    void entrySetRetainAllExactMatchLeavesMapUnchanged() {
        Collection<Map.Entry<String, String>> exact = Arrays.asList(new AbstractMap.SimpleEntry<>("key1", "value1"), new AbstractMap.SimpleEntry<>("key2", "value2"), new AbstractMap.SimpleEntry<>("key3", "value3"));
        boolean changed = entrySet.retainAll(exact);
        assertFalse(changed, "retainAll with identical collection must not change the map");
        assertEquals(3, ciMap.size());
    }

    @Test
    void entrySetRetainAllRemovesMismatchedValue() {
        Collection<Map.Entry<String, String>> mix = Arrays.asList(new AbstractMap.SimpleEntry<>("key1", "value1"), new AbstractMap.SimpleEntry<>("key2", "different"));
        boolean changed = entrySet.retainAll(mix);
        assertTrue(changed, "retainAll must remove entry whose value does not match");
        assertEquals(1, ciMap.size());
        assertTrue(ciMap.containsKey("key1"));
        assertFalse(ciMap.containsKey("key2"));
        assertFalse(ciMap.containsKey("key3"));
    }

    @Test
    void entrySetRetainAllIgnoresNonEntryObjects() {
        Collection<Object> mixed = Arrays.asList(new AbstractMap.SimpleEntry<>("key1", "value1"), "not an entry");
        boolean changed = entrySet.retainAll(mixed);
        assertTrue(changed, "retainAll must ignore non‑Entry objects and remove non‑matching entries");
        assertEquals(2, ciMap.size());
        assertTrue(ciMap.containsKey("key1"));
        assertTrue(ciMap.containsKey("key2"));
        assertFalse(ciMap.containsKey("key3"));
    }

    @Test
    void iteratorIteratesInMapOrderAndProvidesCorrectKeyValues() {
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        List<String> observed = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            observed.add(e.getKey());
            assertEquals(ciMap.get(e.getKey()), e.getValue(), "Iterator must return entry with matching value");
        }
        // The underlying map is a LinkedHashMap (preserves insertion order)
        assertEquals(Arrays.asList("key1", "key2", "key3"), observed);
    }

    @Test
    void iteratorRemoveDeletesLastReturnedEntryFromMap() {
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("key2".equals(e.getKey())) {
                // remove key2
                it.remove();
                break;
            }
        }
        assertFalse(ciMap.containsKey("key2"));
        assertEquals(2, ciMap.size());
    }

    @Test
    void iteratorHasNextReturnsFalseWhenAllElementsConsumed() {
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        while (it.hasNext()) {
            it.next();
        }
        assertFalse(it.hasNext(), "hasNext() must be false after the last element");
    }

    @Test
    void iteratorNextSetValueUpdatesUnderlyingMap() {
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        // first entry: key1
        Map.Entry<String, String> entry = it.next();
        assertEquals("key1", entry.getKey());
        String old = entry.setValue("newValue");
        assertEquals("value1", old, "setValue should return the previous value");
        assertEquals("newValue", ciMap.get("key1"), "Map must reflect the new value");
    }

    @Test
    void iteratorNextSetValueNullStoresNullInMap() {
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        // first entry: key1
        Map.Entry<String, String> entry = it.next();
        entry.setValue(null);
        assertTrue(ciMap.containsKey("key1"));
        assertNull(ciMap.get("key1"));
        assertTrue(ciMap.containsValue(null), "Map should now contain a null value");
    }
}
