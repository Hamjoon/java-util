package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class TrackingMap_entrySet_Test {

    // ---------- fields used by the tests ----------
    private TrackingMap<String, Integer> trackingMapWithMock;

    private Map<String, Integer> mockBackingMap;

    private TrackingMap<String, Integer> trackingMapWithReal;

    private Map<String, Integer> realBackingMap;

    class BadMap<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new UnsupportedOperationException("entrySet not supported");
        }

        // --- minimal implementations for other methods (throw UnsupportedOperationException) ---
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V get(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<K> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<V> values() {
            throw new UnsupportedOperationException();
        }
    }

    class BadMap<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new UnsupportedOperationException("entrySet not supported");
        }

        // --- minimal implementations for other methods (throw UnsupportedOperationException) ---
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V get(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<K> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<V> values() {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // --- Mock‑backed TrackingMap ----------------------------------------
        mockBackingMap = mock(Map.class);
        // Prepare a deterministic Set that the mock will return
        Set<Map.Entry<String, Integer>> mockEntrySet = new HashSet<>();
        when(mockBackingMap.entrySet()).thenReturn(mockEntrySet);
        trackingMapWithMock = new TrackingMap<>(mockBackingMap);
        // --- Real‑backed TrackingMap ----------------------------------------
        // LinkedHashMap preserves insertion order for easier assertions
        realBackingMap = new LinkedHashMap<>();
        realBackingMap.put("one", 1);
        realBackingMap.put("two", 2);
        realBackingMap.put("three", 3);
        trackingMapWithReal = new TrackingMap<>(realBackingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helpful for GC in larger test suites
        trackingMapWithMock = null;
        mockBackingMap = null;
        trackingMapWithReal = null;
        realBackingMap = null;
    }

    // ---------- tests ----------
    @Test
    void entrySetDelegatesToBackingMap() {
        // When entrySet() is called on the TrackingMap, it should delegate to the backing map
        Set<Map.Entry<String, Integer>> result = trackingMapWithMock.entrySet();
        // Verify delegation
        verify(mockBackingMap, times(1)).entrySet();
        // The result must be exactly what the mock returned
        assertNotNull(result, "entrySet() must not return null");
        assertTrue(result.isEmpty(), "The mocked entry set is empty as we configured");
    }

    @Test
    void entrySetReflectsRealBackingMapChanges() {
        // Populate the real map
        realBackingMap.put("one", 1);
        realBackingMap.put("two", 2);
        realBackingMap.put("three", 3);
        // Obtain the entry set from the real‑backed TrackingMap
        Set<Map.Entry<String, Integer>> entrySet = trackingMapWithReal.entrySet();
        // The entry set should contain exactly the entries we put into the backing map
        assertEquals(3, entrySet.size(), "Entry set size should match the number of entries in the map");
        // Verify each expected key/value pair is present
        Map<String, Integer> collected = new HashMap<>();
        for (Map.Entry<String, Integer> e : entrySet) {
            collected.put(e.getKey(), e.getValue());
        }
        assertEquals(realBackingMap, collected, "Collected entries must equal the backing map's contents");
        // Remove an entry via the iterator and ensure the TrackingMap reflects the removal
        Iterator<Map.Entry<String, Integer>> it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("two".equals(e.getKey())) {
                // removes from the underlying map
                it.remove();
                break;
            }
        }
        // After removal, both the entry set and the TrackingMap should report size 2
        assertEquals(2, entrySet.size(), "Entry set size should shrink after removal");
        assertEquals(2, trackingMapWithReal.size(), "TrackingMap size should shrink after removal");
        assertFalse(trackingMapWithReal.containsKey("two"), "Key 'two' must no longer be present");
    }

    // 1. entrySet() on an empty map returns an empty Set
    @Test
    void entrySetEmptyMapReturnsEmptySet() {
        TrackingMap<String, Integer> emptyMap = new TrackingMap<>(new HashMap<>());
        Set<Map.Entry<String, Integer>> set = emptyMap.entrySet();
        assertNotNull(set);
        assertTrue(set.isEmpty(), "Entry set of a newly created empty TrackingMap must be empty");
    }

    // 2. entrySet() contains exactly the entries that were put into the map
    @Test
    void entrySetContainsAllPutEntries() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Set<Map.Entry<String, Integer>> set = map.entrySet();
        assertEquals(3, set.size());
        // Build a reference map and compare
        Map<String, Integer> expected = Map.of("a", 1, "b", 2, "c", 3);
        for (Map.Entry<String, Integer> e : set) {
            assertEquals(expected.get(e.getKey()), e.getValue(), "EntrySet should contain the exact key/value pair");
        }
    }

    // 3. Removing an entry via iterator.remove() updates the map
    @Test
    void iteratorRemoveUpdatesMap() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Set<Map.Entry<String, Integer>> set = map.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("b".equals(e.getKey())) {
                it.remove();
                break;
            }
        }
        assertEquals(2, map.size());
        assertNull(map.get("b"));
        assertFalse(set.contains(new AbstractMap.SimpleEntry<>("b", 2)));
    }

    // 4. Modifications to the map after obtaining entrySet are reflected in the same Set instance
    @Test
    void entrySetReflectsLaterPutOperations() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        // capture view when size = 2
        Set<Map.Entry<String, Integer>> set = map.entrySet();
        assertEquals(2, set.size());
        // modify underlying map
        map.put("d", 4);
        assertEquals(3, set.size(), "Entry set view must reflect new entry added after view creation");
        assertTrue(set.contains(new AbstractMap.SimpleEntry<>("d", 4)));
    }

    // 5. entrySet().clear() empties the TrackingMap
    @Test
    void entrySetClearEmptiesMap() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("x", 10);
        map.put("y", 20);
        Set<Map.Entry<String, Integer>> set = map.entrySet();
        set.clear();
        assertTrue(map.isEmpty(), "Calling clear() on entrySet must empty the underlying map");
        assertTrue(map.entrySet().isEmpty(), "Subsequent entrySet view must also be empty");
    }

    // 6. entrySet() returns the same instance on successive calls
    @Test
    void entrySetIdentityEquality() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        Set<Map.Entry<String, Integer>> first = map.entrySet();
        Set<Map.Entry<String, Integer>> second = map.entrySet();
        assertSame(first, second, "Multiple calls to entrySet() must return the same Set instance");
    }

    // 7. Adding a new entry to the Set throws UnsupportedOperationException
    @Test
    void entrySetAddUnsupported() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        Set<Map.Entry<String, Integer>> set = map.entrySet();
        Map.Entry<String, Integer> newEntry = new AbstractMap.SimpleEntry<>("x", 99);
        assertThrows(UnsupportedOperationException.class, () -> set.add(newEntry), "entrySet.add() must throw UnsupportedOperationException");
    }

    // ---------- removal via Set.remove(entry) ----------
    @Test
    void setRemoveExistingEntry() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // obtain a concrete entry for key "c"
        Map.Entry<String, Integer> target = null;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if ("c".equals(e.getKey())) {
                target = e;
                break;
            }
        }
        assertNotNull(target, "Target entry must be found");
        boolean removed = map.entrySet().remove(target);
        assertTrue(removed, "remove(entry) should return true for existing entry");
        assertEquals(2, map.size());
        assertNull(map.get("c"));
    }

    @Test
    void setRemoveNonExistentEntry() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("x", 10);
        Map.Entry<String, Integer> fake = new AbstractMap.SimpleEntry<>("y", 20);
        boolean removed = map.entrySet().remove(fake);
        assertFalse(removed, "remove(non‑existent entry) must return false");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("x"));
    }

    // ---------- contains and containsAll ----------
    @Test
    void setContainsExistingEntry() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("k", 5);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertTrue(map.entrySet().contains(entry), "Set must contain an entry that is actually present");
    }

    @Test
    void setContainsEntryWithDifferentValue() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("k", 5);
        Map.Entry<String, Integer> different = new AbstractMap.SimpleEntry<>("k", 99);
        assertFalse(map.entrySet().contains(different), "Set must not contain entry with same key but different value");
    }

    @Test
    void setContainsAllSubset() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        List<Map.Entry<String, Integer>> subset = List.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("c", 3));
        assertTrue(map.entrySet().containsAll(subset), "containsAll should be true for a subset of entries");
    }

    @Test
    void setContainsAllWithMissingEntry() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        List<Map.Entry<String, Integer>> superset = List.of(new AbstractMap.SimpleEntry<>("a", 1), // not present
        new AbstractMap.SimpleEntry<>("d", 4));
        assertFalse(map.entrySet().containsAll(superset), "containsAll must be false if any entry is missing");
    }

    // ---------- entry.setValue updates underlying map ----------
    @Test
    void entrySetValueUpdatesMap() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("p", 7);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        Integer old = entry.setValue(77);
        assertEquals(7, old);
        assertEquals(77, map.get("p"));
        assertEquals(77, entry.getValue());
    }

    @Test
    void entrySetValueAllowsNullWhenMapPermitsIt() {
        // HashMap permits nulls
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("n", null);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertNull(entry.getValue());
        // set null again – should be a no‑op but must not throw
        entry.setValue(null);
        assertNull(map.get("n"));
        // replace non‑null with null
        map.put("m", 1);
        Map.Entry<String, Integer> entryM = null;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if ("m".equals(e.getKey())) {
                entryM = e;
                break;
            }
        }
        assertNotNull(entryM);
        entryM.setValue(null);
        assertNull(map.get("m"));
    }

    // ---------- toArray correctness ----------
    @Test
    void entrySetToArrayReturnsCorrectObjectArray() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("x", 10);
        map.put("y", 20);
        Object[] array = map.entrySet().toArray();
        assertEquals(2, array.length);
        for (Object obj : array) {
            assertTrue(obj instanceof Map.Entry);
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) obj;
            assertTrue(map.containsKey(e.getKey()));
            assertEquals(map.get(e.getKey()), e.getValue());
        }
    }

    @Test
    void entrySetToArrayWithSuppliedArray() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        @SuppressWarnings("unchecked")
        Map.Entry<String, Integer>[] supplied = (Map.Entry<String, Integer>[]) new Map.Entry[5];
        Map.Entry<String, Integer>[] result = map.entrySet().toArray(supplied);
        assertSame(supplied, result, "toArray(T[]) must return the supplied array when it is large enough");
        assertEquals(5, result.length);
        // first three positions must be non‑null entries; last two must be null
        assertNotNull(result[0]);
        assertNotNull(result[1]);
        assertNotNull(result[2]);
        assertNull(result[3]);
        assertNull(result[4]);
    }

    // ---------- handling of null keys and values ----------
    @Test
    void entrySetHandlesNullKey() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put(null, 100);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> entry = it.next();
        assertNull(entry.getKey(), "Entry key should be null");
        it.remove();
        assertTrue(map.isEmpty(), "Map must be empty after removing the null‑key entry");
    }

    @Test
    void entrySetHandlesNullValueAndSetValue() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("z", null);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertNull(entry.getValue(), "Entry value should be null");
        // no change, but must not throw
        entry.setValue(null);
        assertNull(map.get("z"));
    }

    // ---------- bulk operations ----------
    @Test
    void entrySetRemoveAllRemovesMatchingEntries() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        List<Map.Entry<String, Integer>> toRemove = List.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("c", 3));
        boolean changed = map.entrySet().removeAll(toRemove);
        assertTrue(changed);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("b"));
    }

    @Test
    void entrySetRetainAllKeepsOnlySpecifiedEntries() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        List<Map.Entry<String, Integer>> toKeep = List.of(new AbstractMap.SimpleEntry<>("b", 2));
        boolean changed = map.entrySet().retainAll(toKeep);
        assertTrue(changed);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("b"));
    }

    @Test
    void entrySetRemoveAllWithEmptyCollection() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("x", 9);
        boolean changed = map.entrySet().removeAll(Collections.emptyList());
        assertFalse(changed, "removeAll with empty collection must return false");
        assertEquals(1, map.size());
    }

    @Test
    void entrySetRetainAllWithEmptyCollectionClearsMap() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("p", 5);
        map.put("q", 6);
        boolean changed = map.entrySet().retainAll(Collections.emptyList());
        assertTrue(changed);
        assertTrue(map.isEmpty(), "retainAll with empty collection must clear the map");
    }

    // ---------- iterator concurrent modification ----------
    @Test
    void iteratorDetectsConcurrentModification() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        // Structural change outside iterator
        map.put("new", 42);
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must throw CME after external structural modification");
    }

    @Test
    void iteratorRemoveAfterSetValueWorks() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("k", 10);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        Map.Entry<String, Integer> entry = it.next();
        entry.setValue(999);
        // should succeed
        it.remove();
        assertFalse(map.containsKey("k"));
        assertEquals(0, map.size());
    }

    // ---------- equals and hashCode consistency ----------
    @Test
    void entrySetEqualsOtherMapEntrySet() {
        Map<String, Integer> backing1 = new HashMap<>();
        backing1.put("a", 1);
        backing1.put("b", 2);
        TrackingMap<String, Integer> tm1 = new TrackingMap<>(backing1);
        Map<String, Integer> backing2 = new HashMap<>();
        backing2.put("a", 1);
        backing2.put("b", 2);
        TrackingMap<String, Integer> tm2 = new TrackingMap<>(backing2);
        assertEquals(tm1.entrySet(), tm2.entrySet(), "Entry sets of two maps with identical entries must be equal");
    }

    @Test
    void entrySetHashCodeMatchesSumOfEntryHashCodes() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        int expected = 0;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            expected += e.hashCode();
        }
        assertEquals(expected, map.entrySet().hashCode(), "EntrySet hashCode must be the sum of its entries' hashCodes");
    }

    // ---------- exception scenarios ----------
    @Test
    void entrySetThrowsWhenInternalMapIsNull() throws Exception {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        // Use reflection to set the private internalMap field to null
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(map, null);
        assertThrows(NullPointerException.class, map::entrySet, "Calling entrySet when internalMap is null should throw NPE");
    }

    @Test
    void entrySetPropagatesUnsupportedOperationExceptionFromBackingMap() throws Exception {
        // Custom map that throws on entrySet()
        class BadMap<K, V> implements Map<K, V> {

            @Override
            public Set<Entry<K, V>> entrySet() {
                throw new UnsupportedOperationException("entrySet not supported");
            }

            // --- minimal implementations for other methods (throw UnsupportedOperationException) ---
            @Override
            public int size() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isEmpty() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsKey(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsValue(Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public V get(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public V put(K key, V value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public V remove(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends K, ? extends V> m) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<K> keySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<V> values() {
                throw new UnsupportedOperationException();
            }
        }
        BadMap<String, Integer> badMap = new BadMap<>();
        TrackingMap<String, Integer> tracking = new TrackingMap<>(badMap);
        assertThrows(UnsupportedOperationException.class, tracking::entrySet, "UnsupportedOperationException from backing map must propagate");
    }
}
