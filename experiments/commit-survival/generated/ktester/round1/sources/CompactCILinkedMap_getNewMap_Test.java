package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactCILinkedMap_getNewMap_Test {

    /**
     * Simple subclass that makes the protected {@code getNewMap()} visible for testing.
     */
    private static class TestableCompactCILinkedMap<K, V> extends CompactCILinkedMap<K, V> {

        /**
         * Exposes the protected method as public.
         */
        public Map<K, V> exposeNewMap() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass that forces a known {@code compactSize()} value.
     */
    private static class CompactSize8Map<K, V> extends CompactCILinkedMap<K, V> {

        private final int size;

        CompactSize8Map(int size) {
            this.size = size;
        }

        @Override
        protected int compactSize() {
            return size;
        }

        public Map<K, V> exposeNewMap() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass that controls the copy‑iterator behaviour.
     */
    private static class CopyIteratorMap<K, V> extends CompactCILinkedMap<K, V> {

        private final boolean copy;

        CopyIteratorMap(boolean copy) {
            this.copy = copy;
        }

        @Override
        protected boolean useCopyIterator() {
            return copy;
        }

        public Map<K, V> exposeNewMap() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass that disables case‑insensitivity.
     */
    private static class CaseSensitiveMap<K, V> extends CompactCILinkedMap<K, V> {

        @Override
        protected boolean isCaseInsensitive() {
            return false;
        }

        public Map<K, V> exposeNewMap() {
            return super.getNewMap();
        }
    }

    @Test
    void getNewMap_returnsCaseInsensitiveMapInstance() {
        TestableCompactCILinkedMap<String, Integer> map = new TestableCompactCILinkedMap<>();
        Map<String, Integer> newMap = map.exposeNewMap();
        // The concrete type should be CaseInsensitiveMap.
        assertTrue(newMap instanceof CaseInsensitiveMap, "getNewMap() should return a CaseInsensitiveMap instance");
    }

    @Test
    void caseInsensitiveMapBehavesCaseInsensitively() {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = (CaseInsensitiveMap<String, String>) map.exposeNewMap();
        ciMap.put("KeyOne", "value1");
        // Retrieval with different case should succeed.
        assertEquals("value1", ciMap.get("keyone"));
        assertEquals("value1", ciMap.get("KEYONE"));
    }

    @Test
    void underlyingMapPreservesInsertionOrder() {
        TestableCompactCILinkedMap<String, Integer> map = new TestableCompactCILinkedMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, Integer> ciMap = (CaseInsensitiveMap<String, Integer>) map.exposeNewMap();
        ciMap.put("first", 1);
        ciMap.put("second", 2);
        ciMap.put("third", 3);
        // Verify iteration order matches insertion order (LinkedHashMap semantics).
        String[] expectedOrder = { "first", "second", "third" };
        int index = 0;
        for (Map.Entry<String, Integer> entry : ciMap.entrySet()) {
            assertEquals(expectedOrder[index], entry.getKey(), "Entry order should follow insertion order");
            index++;
        }
        assertEquals(expectedOrder.length, index, "All entries should have been iterated");
    }

    @Test
    void newMapIsInitiallyEmpty() {
        TestableCompactCILinkedMap<Object, Object> map = new TestableCompactCILinkedMap<>();
        Map<Object, Object> newMap = map.exposeNewMap();
        assertTrue(newMap.isEmpty(), "A freshly created map should be empty");
        assertEquals(0, newMap.size(), "Size of a new map must be zero");
    }

    @Test
    void getNewMap_returnsNonNullCaseInsensitiveMap() {
        TestableCompactCILinkedMap<String, Integer> map = new TestableCompactCILinkedMap<>();
        Map<String, Integer> newMap = map.exposeNewMap();
        assertNotNull(newMap, "Map instance must not be null");
        assertTrue(newMap instanceof CaseInsensitiveMap, "Returned map should be an instance of CaseInsensitiveMap");
        assertTrue(newMap.isEmpty(), "Newly created map must be empty");
    }

    @Test
    void successiveCallsReturnDifferentInstances() {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        Map<String, String> first = map.exposeNewMap();
        Map<String, String> second = map.exposeNewMap();
        assertNotSame(first, second, "Each call to getNewMap() must create a new instance");
    }

    @Test
    void caseInsensitiveLookupWorks() {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = (CaseInsensitiveMap<String, String>) map.exposeNewMap();
        ciMap.put("TeSt", "value");
        assertEquals("value", ciMap.get("test"));
        assertEquals("value", ciMap.get("TEST"));
    }

    @Test
    void insertionOrderIsPreserved() {
        TestableCompactCILinkedMap<String, Integer> map = new TestableCompactCILinkedMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, Integer> ciMap = (CaseInsensitiveMap<String, Integer>) map.exposeNewMap();
        ciMap.put("first", 1);
        ciMap.put("second", 2);
        ciMap.put("third", 3);
        List<String> observed = new ArrayList<>();
        for (Map.Entry<String, Integer> e : ciMap.entrySet()) {
            observed.add(e.getKey());
        }
        assertEquals(List.of("first", "second", "third"), observed, "LinkedHashMap should preserve insertion order");
    }

    @Test
    void underlyingLinkedHashMapHasCorrectInitialCapacity() throws Exception {
        // compactSize() returns 7 → expected capacity = 8
        CompactSize8Map<String, Integer> map = new CompactSize8Map<>(7);
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, Integer> ciMap = (CaseInsensitiveMap<String, Integer>) map.exposeNewMap();
        // The delegate map inside CaseInsensitiveMap is a LinkedHashMap.
        Field delegateField = CaseInsensitiveMap.class.getDeclaredField("map");
        delegateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Integer> delegate = (LinkedHashMap<String, Integer>) delegateField.get(ciMap);
        // LinkedHashMap does not expose its capacity directly; we infer it via reflection.
        Field tableField = LinkedHashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(delegate);
        int actualCapacity = table == null ? 0 : table.length;
        assertTrue(actualCapacity >= 8, "Underlying LinkedHashMap capacity should be at least compactSize()+1 (8)");
    }

    @Test
    void mapSupportsStandardMutatingOperations() {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = (CaseInsensitiveMap<String, String>) map.exposeNewMap();
        ciMap.put("A", "1");
        ciMap.put("B", "2");
        assertEquals(2, ciMap.size());
        Map<String, String> other = Map.of("C", "3", "D", "4");
        ciMap.putAll(other);
        assertEquals(4, ciMap.size());
        // case‑insensitive removal
        ciMap.remove("b");
        assertFalse(ciMap.containsKey("B"));
        assertEquals(3, ciMap.size());
        ciMap.clear();
        assertTrue(ciMap.isEmpty());
    }

    @Test
    void linkedHashMapUsesDefaultLoadFactorAndCapacityAtLeastRequested() throws Exception {
        TestableCompactCILinkedMap<String, Integer> map = new TestableCompactCILinkedMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, Integer> ciMap = (CaseInsensitiveMap<String, Integer>) map.exposeNewMap();
        // Access the internal LinkedHashMap via reflection.
        Field delegateField = CaseInsensitiveMap.class.getDeclaredField("map");
        delegateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Integer> delegate = (LinkedHashMap<String, Integer>) delegateField.get(ciMap);
        // Verify load factor (default is 0.75f). LinkedHashMap stores it in "loadFactor".
        Field loadFactorField = LinkedHashMap.class.getDeclaredField("loadFactor");
        loadFactorField.setAccessible(true);
        float loadFactor = loadFactorField.getFloat(delegate);
        assertEquals(0.75f, loadFactor, 0.0001f, "Default load factor should be 0.75");
        // Verify capacity is at least the requested initial capacity.
        Field tableField = LinkedHashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(delegate);
        int capacity = table == null ? 0 : table.length;
        assertTrue(capacity >= 1, "Capacity should be non‑zero (at least the default initial capacity)");
    }

    @Test
    void putNullKeyThrowsNullPointerException() {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = (CaseInsensitiveMap<String, String>) map.exposeNewMap();
        assertThrows(NullPointerException.class, () -> ciMap.put(null, "value"), "Putting a null key should throw NullPointerException");
    }

    @Test
    void putNullValueIsAllowed() {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = (CaseInsensitiveMap<String, String>) map.exposeNewMap();
        ciMap.put("key", null);
        assertTrue(ciMap.containsKey("key"));
        assertNull(ciMap.get("key"));
    }

    @Test
    void putAllWithDuplicateCaseInsensitiveKeysResultsInSingleEntry() {
        TestableCompactCILinkedMap<String, Integer> map = new TestableCompactCILinkedMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, Integer> ciMap = (CaseInsensitiveMap<String, Integer>) map.exposeNewMap();
        Map<String, Integer> source = new HashMap<>();
        source.put("Key", 1);
        // duplicate ignoring case
        source.put("key", 2);
        ciMap.putAll(source);
        assertEquals(1, ciMap.size(), "Duplicate keys differing only by case should collapse to one entry");
        // last value wins
        assertEquals(2, ciMap.get("KEY"));
    }

    @Test
    void iteratorReflectsModificationsWhenCopyIteratorIsFalse() {
        CopyIteratorMap<String, Integer> map = new CopyIteratorMap<>(false);
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, Integer> ciMap = (CaseInsensitiveMap<String, Integer>) map.exposeNewMap();
        ciMap.put("one", 1);
        Iterator<Map.Entry<String, Integer>> it = ciMap.entrySet().iterator();
        // Modify after iterator creation.
        ciMap.put("two", 2);
        // The iterator should see the new entry (no CME and entry appears later).
        List<String> seen = new ArrayList<>();
        while (it.hasNext()) {
            seen.add(it.next().getKey());
        }
        assertTrue(seen.contains("two"), "Iterator should reflect modifications when useCopyIterator() is false");
    }

    @Test
    void iteratorIsSnapshotWhenCopyIteratorIsTrue() {
        CopyIteratorMap<String, Integer> map = new CopyIteratorMap<>(true);
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, Integer> ciMap = (CaseInsensitiveMap<String, Integer>) map.exposeNewMap();
        ciMap.put("one", 1);
        Iterator<Map.Entry<String, Integer>> it = ciMap.entrySet().iterator();
        // Modify after iterator creation.
        ciMap.put("two", 2);
        // Snapshot iterator must not see the new entry.
        List<String> seen = new ArrayList<>();
        while (it.hasNext()) {
            seen.add(it.next().getKey());
        }
        assertFalse(seen.contains("two"), "Iterator should be a snapshot when useCopyIterator() returns true");
    }

    @Test
    void isCaseInsensitiveFlagControlsKeySensitivity() {
        CaseSensitiveMap<String, String> map = new CaseSensitiveMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = (CaseInsensitiveMap<String, String>) map.exposeNewMap();
        ciMap.put("Abc", "value");
        assertNull(ciMap.get("abc"), "When isCaseInsensitive() returns false, lookups must be case‑sensitive");
        assertEquals("value", ciMap.get("Abc"));
    }

    @Test
    void serializationPreservesEntriesAndCaseInsensitivity() throws Exception {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> original = (CaseInsensitiveMap<String, String>) map.exposeNewMap();
        original.put("One", "1");
        original.put("Two", "2");
        // Serialize
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(original);
            out.flush();
            bytes = bos.toByteArray();
        }
        // Deserialize
        CaseInsensitiveMap<String, String> deserialized;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            @SuppressWarnings("unchecked")
            CaseInsensitiveMap<String, String> tmp = (CaseInsensitiveMap<String, String>) in.readObject();
            deserialized = tmp;
        }
        // Verify content
        assertEquals(2, deserialized.size());
        assertEquals("1", deserialized.get("ONE"));
        assertEquals("2", deserialized.get("two"));
    }

    @Test
    void overflowCompactSizeThrowsIllegalArgumentException() {
        CompactSize8Map<String, Integer> map = new CompactSize8Map<>(Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, map::exposeNewMap, "Overflowing compactSize()+1 should cause IllegalArgumentException");
    }

    @Test
    void negativeCompactSizeThrowsIllegalArgumentException() {
        CompactSize8Map<String, Integer> map = new CompactSize8Map<>(-2);
        assertThrows(IllegalArgumentException.class, map::exposeNewMap, "Negative initial capacity should cause IllegalArgumentException");
    }
}
