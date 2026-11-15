package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

class ConcurrentNavigableMapNullSafe_keySet_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for every test to guarantee isolation
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern symmetrical
        map = null;
    }

    /* --------------------------------------------------------------
     *  Helper method to set the protected internalMap field via reflection.
     * -------------------------------------------------------------- */
    private static void setInternalMapViaReflection(ConcurrentNavigableMapNullSafe<?, ?> target, ConcurrentMap<?, ?> newMap) {
        try {
            Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
            f.setAccessible(true);
            f.set(target, newMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set internalMap via reflection", e);
        }
    }

    /* --------------------------------------------------------------
     *  Tests for keySet()
     * -------------------------------------------------------------- */
    @Test
    void keySetContainsAllInsertedKeysAndRespectsNullOrdering() {
        // Given – insert a few keys, including a null key
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        // null is allowed and should be last
        map.put(null, 99);
        // When – retrieve the key set
        NavigableSet<String> keySet = map.keySet();
        // Then – the set must contain exactly the inserted keys
        assertEquals(4, keySet.size(), "keySet size should match number of inserted keys");
        assertTrue(keySet.containsAll(Arrays.asList("apple", "banana", "cherry", null)), "keySet should contain all inserted keys including null");
        // And – the natural ordering must place the null key last
        List<String> iterationOrder = new ArrayList<>();
        for (String k : keySet) {
            iterationOrder.add(k);
        }
        assertEquals("apple", iterationOrder.get(0));
        assertEquals("banana", iterationOrder.get(1));
        assertEquals("cherry", iterationOrder.get(2));
        assertNull(iterationOrder.get(3), "null key must be the last element in the ascending view");
    }

    @Test
    void keySetIsLiveViewReflectsRemovals() {
        // Given – populate the map
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 0);
        NavigableSet<String> keySet = map.keySet();
        // When – remove a key via the keySet view
        boolean removed = keySet.remove("one");
        // Then – removal should succeed and be reflected in the underlying map
        assertTrue(removed, "keySet.remove should return true for an existing key");
        assertNull(map.get("one"), "Underlying map must no longer contain the removed key");
        assertEquals(2, map.size(), "Map size must be reduced after removal through keySet");
        assertFalse(keySet.contains("one"), "keySet must no longer contain the removed key");
    }

    @Test
    void keySetIteratorSupportsRemove() {
        // Given – fill the map
        map.put("alpha", 10);
        map.put("beta", 20);
        map.put(null, 30);
        NavigableSet<String> keySet = map.keySet();
        // When – iterate and remove via iterator
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            String k = it.next();
            if ("beta".equals(k)) {
                // remove "beta" through iterator
                it.remove();
            }
        }
        // Then – the map and keySet must reflect the removal
        assertNull(map.get("beta"), "Map should not contain 'beta' after iterator removal");
        assertFalse(keySet.contains("beta"), "keySet should not contain 'beta' after iterator removal");
        assertEquals(2, map.size(), "Map size should be 2 after removal");
    }

    @Test
    void keySetIsNavigableAndSupportsHigherAndLowerOperations() {
        // Given – insert ordered keys and a null key
        map.put("a", 1);
        map.put("c", 3);
        map.put("e", 5);
        // null considered greatest
        map.put(null, 0);
        NavigableSet<String> keySet = map.keySet();
        // When/Then – higher and lower queries on the view
        assertEquals("c", keySet.higher("a"));
        assertEquals("e", keySet.higher("c"));
        assertNull(keySet.higher("e"), "There is no higher key after the greatest non‑null key");
        assertEquals("c", keySet.lower("e"));
        assertEquals("a", keySet.lower("c"));
        assertNull(keySet.lower("a"), "There is no lower key before the smallest key");
        // Null is the greatest element, so lower(null) should be the last non‑null key
        assertEquals("e", keySet.lower(null));
    }

    @Test
    void keySetOnEmptyMapReturnsEmptyNavigableSet() {
        // When – keySet is called on a freshly created (empty) map
        NavigableSet<String> keySet = map.keySet();
        // Then – it must be empty and non‑null
        assertNotNull(keySet, "keySet should never be null");
        assertTrue(keySet.isEmpty(), "keySet of an empty map must be empty");
        assertEquals(0, keySet.size(), "Size should be zero");
    }

    @Test
    void keySetIncludesNullKeyWhenPresent() {
        // Given – a map containing a null key
        map.put(null, 42);
        // When – retrieving the key set
        NavigableSet<String> keySet = map.keySet();
        // Then – the null key must be present (unmasked) and be the last element
        assertTrue(keySet.contains(null), "keySet must contain the null key");
        assertEquals(1, keySet.size());
        assertNull(keySet.last(), "null should be the greatest element in the ascending view");
    }

    @Test
    void keySetReflectsConcurrentModificationAfterObtainingView() {
        // Given – obtain a view before any entries
        NavigableSet<String> keySet = map.keySet();
        // When – a new entry is added to the underlying map
        map.put("newKey", 123);
        // Then – the previously obtained view must reflect the addition
        assertTrue(keySet.contains("newKey"), "keySet view should reflect concurrent modifications");
        assertEquals(1, keySet.size());
    }

    @Test
    void keySetAddOperationIsUnsupported() {
        // Given – a key set view
        NavigableSet<String> keySet = map.keySet();
        // When/Then – attempting to add a key directly should throw
        assertThrows(UnsupportedOperationException.class, () -> keySet.add("illegal"), "Adding directly to the keySet view must be unsupported");
    }

    @Test
    void keySetRespectsCustomComparatorOrder() {
        // Prepare a map that uses a reverse order comparator for its internal skip list.
        // We replace the internalMap via reflection because the public API does not expose a comparator ctor.
        ConcurrentSkipListMap<Object, Object> reverseMap = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        setInternalMapViaReflection(map, reverseMap);
        // Insert keys in natural order; the view should iterate in reverse order.
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        NavigableSet<String> keySet = map.keySet();
        // Expected order is c, b, a (reverse of natural order)
        List<String> actual = new ArrayList<>(keySet);
        assertEquals(Arrays.asList("c", "b", "a"), actual, "keySet iteration must follow the map's comparator (reverse order)");
    }

    @Test
    void keySetThrowsNPEWhenInternalMapIsNull() throws Exception {
        // Force the protected internalMap field to null via reflection.
        setInternalMapViaReflection(map, null);
        // Expect a NullPointerException when keySet() tries to call internalMap.keySet()
        assertThrows(NullPointerException.class, map::keySet, "Calling keySet() with a null internalMap should throw NPE");
    }

    @Test
    void keySetThrowsNPEWhenInternalMapKeySetReturnsNull() {
        // Mock a ConcurrentMap whose keySet() returns null.
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockedMap = mock(ConcurrentMap.class);
        when(mockedMap.keySet()).thenReturn(null);
        setInternalMapViaReflection(map, mockedMap);
        // Expect NPE because KeyNavigableSet constructor receives a null argument.
        assertThrows(NullPointerException.class, map::keySet, "If internalMap.keySet() returns null, keySet() should propagate NPE");
    }

    @Test
    void keySetPropagatesUnsupportedOperationExceptionFromInternalMap() {
        // Mock a ConcurrentMap whose keySet() throws UnsupportedOperationException.
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockedMap = mock(ConcurrentMap.class);
        when(mockedMap.keySet()).thenThrow(new UnsupportedOperationException("keySet not supported"));
        setInternalMapViaReflection(map, mockedMap);
        // The exception should bubble up unchanged.
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, map::keySet, "UnsupportedOperationException from internalMap.keySet() must propagate");
        assertEquals("keySet not supported", ex.getMessage());
    }
}
