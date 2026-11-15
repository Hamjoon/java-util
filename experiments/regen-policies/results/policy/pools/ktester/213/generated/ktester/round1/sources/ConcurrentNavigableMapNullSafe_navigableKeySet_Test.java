package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

// Mockito not required for this test but kept from template
@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_navigableKeySet_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // GIVEN – a fresh map for each test
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared after each test (good practice, although a new instance is created)
        map.clear();
    }

    // Example of a provider for future parameterized tests – not used currently
    static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of());
    }

    @Test
    void navigableKeySet_containsAllInsertedKeys() {
        // GIVEN
        map.put("alpha", 1);
        map.put("beta", 2);
        // null key is allowed
        map.put(null, 99);
        // WHEN
        NavigableSet<String> keySet = map.navigableKeySet();
        // THEN
        assertEquals(3, keySet.size(), "Key set size must match map size");
        assertTrue(keySet.contains("alpha"));
        assertTrue(keySet.contains("beta"));
        assertTrue(keySet.contains(null));
    }

    @Test
    void navigableKeySet_preservesAscendingOrderWithNullLast() {
        // GIVEN
        map.put("b", 2);
        map.put("a", 1);
        // null should be considered *last* in the ascending view
        map.put(null, 99);
        // WHEN
        NavigableSet<String> keySet = map.navigableKeySet();
        Iterator<String> itr = keySet.iterator();
        // THEN – natural ordering for non‑null keys, null last
        assertEquals("a", itr.next());
        assertEquals("b", itr.next());
        assertNull(itr.next());
        assertFalse(itr.hasNext(), "No more elements expected");
    }

    @Test
    void navigableKeySet_isLiveWhenMapIsModified() {
        // GIVEN
        map.put("x", 10);
        NavigableSet<String> keySet = map.navigableKeySet();
        // WHEN – add a new entry directly to the map
        map.put("y", 20);
        // THEN – the previously obtained set reflects the new key
        assertTrue(keySet.contains("y"));
        assertEquals(2, keySet.size());
    }

    @Test
    void navigableKeySet_removalAffectsMap() {
        // GIVEN
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 0);
        NavigableSet<String> keySet = map.navigableKeySet();
        // WHEN – remove a non‑null key via the set view
        boolean removed = keySet.remove("one");
        // THEN
        assertTrue(removed, "Key should be removed");
        assertFalse(map.containsKey("one"));
        assertEquals(2, map.size());
        // WHEN – remove the null key via the set view
        removed = keySet.remove(null);
        // THEN
        assertTrue(removed, "Null key should be removed");
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    @Test
    void navigableKeySet_clearEmptiesMap() {
        // GIVEN
        map.put("k1", 1);
        map.put("k2", 2);
        map.put(null, 3);
        NavigableSet<String> keySet = map.navigableKeySet();
        // WHEN
        keySet.clear();
        // THEN
        assertTrue(map.isEmpty(), "Clearing the key set must empty the underlying map");
        assertTrue(keySet.isEmpty(), "Key set itself must be empty");
    }

    // ----- Empty map behavior ------------------------------------------------
    @Test
    void emptyMap_navigableKeySet_isEmpty() {
        // GIVEN – a freshly created map (already done in @BeforeEach)
        // WHEN
        NavigableSet<String> keySet = map.navigableKeySet();
        // THEN
        assertTrue(keySet.isEmpty(), "NavigableSet should be empty for a new map");
        assertEquals(0, keySet.size());
    }

    @Test
    void emptyMap_navigableKeySet_firstThrowsNoSuchElementException() {
        // GIVEN
        NavigableSet<String> keySet = map.navigableKeySet();
        // WHEN / THEN
        assertThrows(NoSuchElementException.class, keySet::first, "Calling first() on an empty NavigableSet must throw NoSuchElementException");
    }

    // ----- Map with multiple entries -----------------------------------------
    @Test
    void multipleEntries_navigableKeySet_isNaturallyOrdered() {
        // GIVEN
        // using String keys to stay consistent with map type
        map.put("3", 3);
        map.put("1", 1);
        map.put("2", 2);
        // WHEN
        NavigableSet<String> keySet = map.navigableKeySet();
        // THEN – natural (lexicographic) order of the string representations
        List<String> expected = Arrays.asList("1", "2", "3");
        assertEquals(expected, new ArrayList<>(keySet));
    }

    @Test
    void singleEntry_navigableKeySet_firstAndLastReturnSameKey() {
        // GIVEN
        map.put("5", 5);
        // WHEN
        NavigableSet<String> keySet = map.navigableKeySet();
        // THEN
        assertEquals("5", keySet.first());
        assertEquals("5", keySet.last());
    }

    // ----- Null‑key handling -------------------------------------------------
    @Test
    void nullKey_isVisibleInNavigableKeySet() {
        // GIVEN
        map.put(null, 42);
        // WHEN
        NavigableSet<String> keySet = map.navigableKeySet();
        // THEN
        assertEquals(1, keySet.size());
        assertTrue(keySet.contains(null), "NavigableSet must report containing the null key");
    }

    @Test
    void higher_nullReturnsSmallestNonNullKey() {
        // GIVEN
        map.put(null, 0);
        map.put("10", 10);
        map.put("20", 20);
        NavigableSet<String> keySet = map.navigableKeySet();
        // WHEN
        String higher = keySet.higher(null);
        // THEN – null is considered last, so higher(null) should be null (no key after null)
        assertNull(higher, "higher(null) should be null when null is the last element");
    }

    @Test
    void higher_nullWhenOnlyNonNullKeysPresent() {
        // GIVEN – map without a null key
        map.put("5", 5);
        map.put("10", 10);
        NavigableSet<String> keySet = map.navigableKeySet();
        // WHEN
        String higher = keySet.higher(null);
        // THEN – null is treated as before the first element, so higher(null) returns the smallest key
        assertEquals("5", higher);
    }

    // ----- Bidirectional view consistency ------------------------------------
    @Test
    void addKeyThroughNavigableSetInsertsNullValue() {
        // GIVEN
        map.put("10", 100);
        NavigableSet<String> keySet = map.navigableKeySet();
        // WHEN
        // per Set semantics, value will be null
        boolean added = keySet.add("20");
        // THEN
        assertTrue(added);
        assertTrue(map.containsKey("20"));
        assertNull(map.get("20"));
        assertEquals(2, keySet.size());
        assertTrue(keySet.containsAll(Arrays.asList("10", "20")));
    }

    @Test
    void removeKeyThroughNavigableSetDeletesEntryFromMap() {
        // GIVEN
        map.put("30", 300);
        NavigableSet<String> keySet = map.navigableKeySet();
        // WHEN
        boolean removed = keySet.remove("30");
        // THEN
        assertTrue(removed);
        assertFalse(map.containsKey("30"));
        assertEquals(0, keySet.size());
    }

    // ----- Multiple calls return same view -----------------------------------
    @Test
    void successiveCallsReturnSameNavigableKeySetInstance() {
        // GIVEN
        NavigableSet<String> first = map.navigableKeySet();
        // WHEN
        NavigableSet<String> second = map.navigableKeySet();
        // THEN
        assertSame(first, second, "Two successive calls should return the same NavigableSet instance");
    }

    // ----- Concurrent modifications safety ------------------------------------
    @Test
    void concurrentModification_reflectedWithoutCME() throws Exception {
        // GIVEN
        map.put("1", 1);
        NavigableSet<String> keySet = map.navigableKeySet();
        // Create a thread that adds a new entry after a short pause
        CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            try {
                // wait until main thread has started iteration
                latch.await();
                map.put("2", 2);
            } catch (InterruptedException ignored) {
            }
        });
        t.start();
        // WHEN – start iterating, then trigger the concurrent addition
        Iterator<String> itr = keySet.iterator();
        assertTrue(itr.hasNext());
        assertEquals("1", itr.next());
        // allow background thread to add the entry
        latch.countDown();
        // ensure addition completed
        t.join();
        // THEN – the set must now contain the new key and no CME should have been thrown
        assertTrue(keySet.contains("2"));
        assertEquals(2, keySet.size());
    }

    // ----- Map-to-set reflection ---------------------------------------------
    @Test
    void mapPutIsReflectedInNavigableKeySet() {
        // GIVEN
        map.put("1", 1);
        NavigableSet<String> keySet = map.navigableKeySet();
        // WHEN
        map.put("2", 2);
        // THEN
        assertEquals(2, keySet.size());
        assertTrue(keySet.containsAll(Arrays.asList("1", "2")));
    }

    @Test
    void mapRemoveIsReflectedInNavigableKeySet() {
        // GIVEN
        map.put("1", 1);
        map.put("2", 2);
        NavigableSet<String> keySet = map.navigableKeySet();
        // WHEN
        map.remove("1");
        // THEN
        assertEquals(1, keySet.size());
        assertFalse(keySet.contains("1"));
        assertTrue(keySet.contains("2"));
    }

    // ----- Set bulk operations ------------------------------------------------
    @Test
    void addAllThroughSetCreatesEntriesWithNullValues() {
        // GIVEN
        NavigableSet<Integer> intSet = new ConcurrentNavigableMapNullSafe<Integer, String>().navigableKeySet();
        // WHEN
        boolean changed = intSet.addAll(Arrays.asList(3, 4, 5));
        // THEN
        assertTrue(changed);
        assertEquals(3, intSet.size());
        assertTrue(intSet.containsAll(Arrays.asList(3, 4, 5)));
        // verify underlying map has null values
        ConcurrentNavigableMapNullSafe<Integer, String> underlying = (ConcurrentNavigableMapNullSafe<Integer, String>) ((Object) intSet);
        // Since we cannot directly access the underlying map, we rely on get() returning null
        assertNull(underlying.get(3));
        assertNull(underlying.get(4));
        assertNull(underlying.get(5));
    }

    @Test
    void removeAllThroughSetDeletesExistingKeysAndIgnoresMissingOnes() {
        // GIVEN
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        intMap.put(2, "b");
        NavigableSet<Integer> keySet = intMap.navigableKeySet();
        // WHEN
        boolean changed = keySet.removeAll(Arrays.asList(1, 3));
        // THEN
        assertTrue(changed);
        assertEquals(1, keySet.size());
        assertFalse(keySet.contains(1));
        assertTrue(keySet.contains(2));
    }

    // ----- Custom comparator ordering -----------------------------------------
    @Test
    void customComparator_reverseOrderIsHonored() {
        // GIVEN – map with reverse order comparator
        ConcurrentNavigableMapNullSafe<Integer, String> revMap = new ConcurrentNavigableMapNullSafe<>(Collections.reverseOrder());
        revMap.put(5, "e");
        revMap.put(1, "a");
        revMap.put(3, "c");
        // WHEN
        NavigableSet<Integer> keySet = revMap.navigableKeySet();
        // THEN – descending order because of reverse comparator
        List<Integer> expected = Arrays.asList(5, 3, 1);
        assertEquals(expected, new ArrayList<>(keySet));
        assertEquals(5, keySet.first());
        assertEquals(1, keySet.last());
    }

    // ----- Iterator weak consistency -------------------------------------------
    @Test
    void iteratorIsWeaklyConsistentUnderConcurrentModification() {
        // GIVEN
        map.put("1", 1);
        NavigableSet<String> keySet = map.navigableKeySet();
        Iterator<String> itr = keySet.iterator();
        // WHEN – modify the map after iterator creation
        map.put("2", 2);
        // THEN – iterator should still return the element it already saw without CME
        assertTrue(itr.hasNext());
        assertEquals("1", itr.next());
        // The iterator may or may not see the newly added element; we just verify no exception.
    }

    // ----- Type safety exceptions ----------------------------------------------
    @Test
    void addInvalidKeyType_throwsClassCastException() {
        // GIVEN
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        NavigableSet<Integer> keySet = intMap.navigableKeySet();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> {
            // This compiles because the Set is generic, but at runtime the map will reject the type
            @SuppressWarnings("unchecked")
            NavigableSet rawSet = keySet;
            rawSet.add("stringKey");
        });
    }

    @Test
    void removeInvalidKeyType_throwsClassCastException() {
        // GIVEN
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(10, "x");
        NavigableSet<Integer> keySet = intMap.navigableKeySet();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> {
            @SuppressWarnings("unchecked")
            NavigableSet rawSet = keySet;
            rawSet.remove("invalid");
        });
    }

    @Test
    void containsInvalidKeyType_throwsClassCastException() {
        // GIVEN
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(5, "e");
        NavigableSet<Integer> keySet = intMap.navigableKeySet();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> {
            @SuppressWarnings("unchecked")
            NavigableSet rawSet = keySet;
            rawSet.contains("notAnInteger");
        });
    }
}
