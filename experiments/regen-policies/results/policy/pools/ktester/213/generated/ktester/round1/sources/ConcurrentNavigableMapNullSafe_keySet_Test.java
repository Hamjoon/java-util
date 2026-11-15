package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentNavigableMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_keySet_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test to avoid cross‑test contamination.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map explicitly; not strictly required because a new instance
        // is created for each test, but it demonstrates good hygiene.
        map.clear();
    }

    /* --------------------------------------------------------------------- */
    private void setInternalMap(ConcurrentMap<?, ?> newMap) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(map, newMap);
    }

    @Test
    void testKeySetContainsAllKeysIncludingNull() {
        // GIVEN
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put(null, 99);
        map.put("gamma", 3);
        // WHEN
        NavigableSet<String> keySet = map.keySet();
        // THEN
        assertEquals(4, keySet.size(), "keySet should contain four entries");
        assertTrue(keySet.contains("alpha"));
        assertTrue(keySet.contains("beta"));
        assertTrue(keySet.contains("gamma"));
        assertTrue(keySet.contains(null), "keySet must contain the null key");
    }

    @Test
    void testKeySetOrderingNullIsLastInAscendingView() {
        // GIVEN
        map.put("b", 2);
        map.put(null, 0);
        map.put("a", 1);
        map.put("c", 3);
        // WHEN
        NavigableSet<String> keySet = map.keySet();
        // THEN
        List<String> iterationOrder = new ArrayList<>();
        for (String k : keySet) {
            iterationOrder.add(k);
        }
        // Expected order: "a", "b", "c", null (null last in ascending view)
        List<String> expectedOrder = List.of("a", "b", "c", null);
        assertEquals(expectedOrder, iterationOrder, "Ascending keySet should place null key last");
    }

    @Test
    void testKeySetIsLiveView_RemoveViaKeySetReflectsInMap() {
        // GIVEN
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 99);
        NavigableSet<String> keySet = map.keySet();
        // WHEN
        // remove a non‑null key
        boolean removed = keySet.remove("two");
        // remove the null key
        boolean removedNull = keySet.remove(null);
        // THEN
        assertTrue(removed, "keySet.remove should return true for existing key");
        assertTrue(removedNull, "keySet.remove should return true for existing null key");
        assertNull(map.get("two"), "Removed key should no longer be present in the map");
        assertNull(map.get(null), "Removed null key should no longer be present in the map");
        assertEquals(1, map.size(), "Map size should reflect the removals");
        assertFalse(keySet.contains("two"));
        assertFalse(keySet.contains(null));
    }

    @Test
    void testKeySetIsLiveView_AddViaMapReflectedInKeySet() {
        // GIVEN
        map.put("x", 10);
        NavigableSet<String> keySet = map.keySet();
        // WHEN
        map.put("y", 20);
        map.put(null, 30);
        // THEN
        assertTrue(keySet.contains("x"));
        assertTrue(keySet.contains("y"));
        assertTrue(keySet.contains(null));
        assertEquals(3, keySet.size());
    }

    @Test
    void testNavigableKeySetDelegatesToKeySet() {
        // GIVEN
        map.put("first", 1);
        map.put(null, 2);
        map.put("second", 3);
        // WHEN
        NavigableSet<String> viaKeySet = map.keySet();
        NavigableSet<String> viaNavigableKeySet = map.navigableKeySet();
        // THEN
        assertEquals(viaKeySet, viaNavigableKeySet, "navigableKeySet() should return the same set instance as keySet()");
    }

    // ----- group: keySet basic behavior -----
    @Test
    void testKeySetEmptyMapReturnsEmptySet() {
        NavigableSet<String> keySet = map.keySet();
        assertTrue(keySet.isEmpty(), "keySet of an empty map should be empty");
        assertEquals(0, keySet.size());
    }

    @Test
    void testKeySetNaturalOrder() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        NavigableSet<String> keySet = map.keySet();
        List<String> expected = List.of("apple", "banana", "cherry");
        assertEquals(expected, new ArrayList<>(keySet), "keySet should return keys in natural (ascending) order");
    }

    // ----- group: keySet null‑key handling (null‑safe wrapper) -----
    @Test
    void testKeySetContainsNullAndOrdering() {
        map.put(null, 0);
        map.put("alpha", 1);
        map.put("beta", 2);
        NavigableSet<String> keySet = map.keySet();
        assertTrue(keySet.contains(null), "keySet must contain the null key");
        // According to the library contract, null is considered *last* in ascending view.
        List<String> expected = List.of("alpha", "beta", null);
        assertEquals(expected, new ArrayList<>(keySet), "null should appear last in the natural ordering view");
    }

    @Test
    void testKeySetIterationDoesNotThrowWhenNullKeyPresent() {
        map.put(null, 42);
        map.put("x", 1);
        NavigableSet<String> keySet = map.keySet();
        // Just iterate – any exception will cause the test to fail.
        for (String k : keySet) {
            // no‑op
        }
    }

    // ----- group: keySet live view semantics -----
    @Test
    void testLiveViewAddsAfterObtainingSet() {
        map.put("alpha", 1);
        NavigableSet<String> keySet = map.keySet();
        map.put("delta", 4);
        assertTrue(keySet.contains("delta"), "KeySet view must reflect entries added to the map after the view was obtained");
    }

    @Test
    void testLiveViewRemoveViaSetReflectsInMap() {
        map.put("banana", 2);
        map.put("apple", 1);
        NavigableSet<String> keySet = map.keySet();
        assertTrue(keySet.remove("banana"));
        assertNull(map.get("banana"), "Removal via keySet must delete the entry from the map");
    }

    @Test
    void testLiveViewClearEmptiesMap() {
        map.put("a", 1);
        map.put("b", 2);
        NavigableSet<String> keySet = map.keySet();
        keySet.clear();
        assertTrue(map.isEmpty(), "Clearing the keySet view must clear the underlying map");
    }

    // ----- group: keySet iterator behavior -----
    @Test
    void testIteratorOrderAndNoCME() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        NavigableSet<String> keySet = map.keySet();
        Iterator<String> it = keySet.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(List.of("a", "b", "c"), iterated, "Iterator must traverse keys in ascending order");
    }

    @Test
    void testIteratorRemoveDeletesFromMap() {
        map.put("x", 10);
        map.put("y", 20);
        NavigableSet<String> keySet = map.keySet();
        Iterator<String> it = keySet.iterator();
        while (it.hasNext()) {
            String k = it.next();
            if ("y".equals(k)) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("y"));
        assertTrue(map.containsKey("x"));
    }

    // ----- group: keySet reflects external map removals -----
    @Test
    void testSetReflectsMapRemove() {
        map.put("apple", 1);
        map.put("banana", 2);
        NavigableSet<String> keySet = map.keySet();
        map.remove("apple");
        assertFalse(keySet.contains("apple"));
    }

    @Test
    void testSetReflectsMapClear() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        NavigableSet<String> keySet = map.keySet();
        map.clear();
        assertTrue(keySet.isEmpty(), "After map.clear(), the keySet view must be empty");
    }

    // ----- group: keySet with custom comparator -----
    @Test
    void testKeySetRespectsCustomReverseComparator() throws Exception {
        // Use reflection to inject a ConcurrentSkipListMap with a reverse comparator.
        ConcurrentSkipListMap<String, Integer> reverseMap = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        setInternalMap(reverseMap);
        map.put("alpha", 1);
        map.put("epsilon", 2);
        map.put("zeta", 3);
        NavigableSet<String> keySet = map.keySet();
        List<String> expected = List.of("zeta", "epsilon", "alpha");
        assertEquals(expected, new ArrayList<>(keySet), "keySet must follow the custom reverse comparator ordering");
    }

    @Test
    void testFirstAndLastWithCustomComparator() throws Exception {
        ConcurrentSkipListMap<String, Integer> reverseMap = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        setInternalMap(reverseMap);
        map.put("alpha", 1);
        map.put("epsilon", 2);
        map.put("zeta", 3);
        NavigableSet<String> keySet = map.keySet();
        assertEquals("zeta", keySet.first(), "first() should be the highest key under reverse order");
        assertEquals("alpha", keySet.last(), "last() should be the lowest key under reverse order");
    }

    // ----- group: keySet subSet view semantics -----
    @Test
    void testSubSetContainsAndReflectsAdditions() {
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put("date", 4);
        map.put("apple", 1);
        NavigableSet<String> sub = map.keySet().subSet("banana", true, "date", false);
        List<String> expectedInitial = List.of("banana", "cherry");
        assertEquals(expectedInitial, new ArrayList<>(sub));
        // Add a key that falls inside the sub‑set range via the map.
        map.put("blueberry", 5);
        assertTrue(sub.contains("blueberry"), "SubSet view must reflect additions that lie within its range");
    }

    @Test
    void testSubSetRemoveReflectsInMap() {
        map.put("cherry", 3);
        map.put("banana", 2);
        NavigableSet<String> sub = map.keySet().subSet("banana", true, "date", false);
        assertTrue(sub.remove("cherry"));
        assertNull(map.get("cherry"), "Removing via SubSet must delete the entry from the underlying map");
    }

    // ----- group: keySet iterator weak‑consistency -----
    @Test
    void testWeakConsistencyIteratorContinuesAfterConcurrentPut() {
        map.put("a", 1);
        map.put("b", 2);
        NavigableSet<String> keySet = map.keySet();
        Iterator<String> it = keySet.iterator();
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        // Concurrent modification after iterator creation.
        map.put("newKey", 99);
        // The iterator should continue without throwing CME.
        List<String> remaining = new ArrayList<>();
        while (it.hasNext()) {
            remaining.add(it.next());
        }
        // The new entry *may* appear; we only assert that no exception was thrown.
        assertTrue(remaining.contains("b") || remaining.contains("newKey"));
    }

    @Test
    void testWeakConsistencyIteratorMayNotSeeNewEntry() {
        map.put("x", 1);
        NavigableSet<String> keySet = map.keySet();
        Iterator<String> it = keySet.iterator();
        // Add after iterator creation.
        map.put("newKey", 2);
        // Collect what the iterator sees.
        List<String> seen = new ArrayList<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        // According to weak‑consistency, the new entry is not guaranteed to be seen.
        assertFalse(seen.contains("newKey"), "Iterator is allowed to omit entries added after its creation");
    }

    // ----- group: keySet toArray handling of null keys -----
    @Test
    void testToArrayOrderingWithNull() {
        map.put(null, 0);
        map.put("alpha", 1);
        map.put("beta", 2);
        Object[] array = map.keySet().toArray();
        // Expected order: "alpha", "beta", null (null last)
        Object[] expected = new Object[] { "alpha", "beta", null };
        assertArrayEquals(expected, array);
    }

    @Test
    void testToArrayTypedWithNull() {
        map.put(null, 0);
        map.put("alpha", 1);
        map.put("beta", 2);
        String[] array = map.keySet().toArray(new String[0]);
        String[] expected = new String[] { "alpha", "beta", null };
        assertArrayEquals(expected, array);
    }

    // ----- group: keySet contains() semantics with null -----
    @Test
    void testContainsNullWhenPresent() {
        map.put(null, 5);
        assertTrue(map.keySet().contains(null), "contains(null) must be true when null key exists");
    }

    @Test
    void testContainsNullWhenAbsent() {
        map.put("notNull", 1);
        assertFalse(map.keySet().contains(null), "contains(null) must be false when no null key exists");
    }

    // ----- group: keySet exception scenarios -----
    @Test
    void testKeySetThrowsNPEWhenInternalMapIsNull() throws Exception {
        // Corrupt the instance via reflection.
        setInternalMap(null);
        assertThrows(NullPointerException.class, () -> map.keySet(), "Calling keySet() when internalMap is null should throw NPE");
    }

    @Test
    void testKeySetPropagatesRuntimeExceptionFromInternalMap() throws Exception {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockMap = mock(ConcurrentMap.class);
        when(mockMap.keySet()).thenThrow(new RuntimeException("boom"));
        setInternalMap(mockMap);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.keySet(), "Exception from internalMap.keySet() must propagate");
        assertEquals("boom", ex.getMessage());
    }
}
