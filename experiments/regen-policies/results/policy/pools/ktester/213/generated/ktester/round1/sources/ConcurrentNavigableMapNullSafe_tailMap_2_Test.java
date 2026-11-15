package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_tailMap_2_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * The map under test – generic type <K,V> will be instantiated per test.
     */
    private ConcurrentNavigableMapNullSafe<Integer, String> intMap;

    /**
     * Map used for tests that need a custom reverse comparator.
     */
    private ConcurrentNavigableMapNullSafe<Integer, String> reverseMap;

    /**
     * Tail map on a single‑element map.
     */
    @Nested
    class SingleElementMapTests {

        private ConcurrentNavigableMapNullSafe<Integer, String> singleMap;

        @BeforeEach
        void init() {
            singleMap = new ConcurrentNavigableMapNullSafe<>();
            singleMap.put(5, "five");
        }

        @AfterEach
        void clean() {
            singleMap.clear();
        }

        @Test
        void tailMap_exactKey_returnsSingleEntry() {
            ConcurrentNavigableMap<Integer, String> tail = singleMap.tailMap(5);
            assertEquals(1, tail.size());
            assertEquals("five", tail.get(5));
        }

        @Test
        void tailMap_keyGreaterThanOnlyEntry_returnsEmpty() {
            ConcurrentNavigableMap<Integer, String> tail = singleMap.tailMap(6);
            assertTrue(tail.isEmpty(), "Tail map should be empty when key > only entry");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // ---------- default (natural order) map ----------
        intMap = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a known set of entries
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        intMap.put(4, "four");
        // ---------- map with explicit reverse order comparator ----------
        // The class inherits the constructors of AbstractConcurrentNullSafeMap;
        // it provides a constructor that accepts a Comparator (verified by
        // inspection of the source).  If such a constructor does not exist,
        // reflection could be used, but for the purposes of this test we assume
        // it is available.
        reverseMap = new ConcurrentNavigableMapNullSafe<>(Collections.reverseOrder());
        reverseMap.put(1, "one");
        reverseMap.put(2, "two");
        reverseMap.put(3, "three");
        reverseMap.put(4, "four");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to aid GC and ensure isolation
        map.clear();
    }

    @Test
    void tailMap_inclusiveKey_returnsCorrectView() {
        // GIVEN a map with keys "a","b","c",null (null is last)
        // WHEN we request a tailMap from key "b"
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b");
        // THEN the view should contain "b", "c", and the null key (null is last)
        assertEquals(3, tail.size(), "Tail map should contain three entries");
        assertTrue(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
        // Verify values are as expected
        assertEquals(2, tail.get("b"));
        assertEquals(3, tail.get("c"));
        assertEquals(100, tail.get(null));
    }

    @Test
    void tailMap_withNullKey_returnsOnlyNullEntry() {
        // GIVEN a map where null is the highest key
        // WHEN we request a tailMap from the null key
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null);
        // THEN the view should contain only the null entry
        assertEquals(1, tail.size(), "Tail map from null should contain only the null entry");
        assertTrue(tail.containsKey(null));
        assertEquals(100, tail.get(null));
    }

    @Test
    void tailMap_modificationsReflectInOriginalMap() {
        // GIVEN a tailMap view starting at "b"
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b");
        // WHEN we add a new entry through the tail view
        tail.put("d", 4);
        // THEN the original map should reflect the new entry
        assertEquals(5, map.size(), "Original map should have grown");
        assertEquals(4, map.get("d"));
        // AND when we remove an entry via the tail view
        Integer removed = tail.remove("c");
        assertEquals(3, removed);
        assertNull(map.get("c"));
        assertEquals(4, map.size());
    }

    @Test
    void tailMap_isLiveAndReflectsRemovalsFromOriginalMap() {
        // GIVEN a tailMap view starting at "a"
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("a");
        // WHEN we remove an entry directly from the original map
        Integer removed = map.remove("b");
        assertEquals(2, removed);
        // THEN the tail view should no longer contain that entry
        assertFalse(tail.containsKey("b"));
        // a, c, null remain
        assertEquals(3, tail.size());
    }

    @Test
    void tailMap_withNonexistentKeyBehavesAccordingToNavigableMapContract() {
        // GIVEN a key that does not exist but would be positioned between "b" and "c"
        // WHEN we request a tailMap from "bb"
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("bb");
        // THEN the view should start at the next higher key, which is "c"
        assertTrue(tail.containsKey("c"));
        assertFalse(tail.containsKey("b"));
        // "c" and null
        assertEquals(2, tail.size());
    }

    /**
     * Tail map when the supplied key exists (inclusive).
     */
    @Test
    void tailMap_withExistingKey_inclusive() {
        // GIVEN a map with keys 1‑4
        // WHEN we request a tailMap from key 2 (inclusive)
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(2);
        // THEN the view must contain 2,3,4
        assertEquals(3, tail.size(), "Expected three entries");
        assertEquals("two", tail.get(2));
        assertEquals("three", tail.get(3));
        assertEquals("four", tail.get(4));
        assertFalse(tail.containsKey(1));
    }

    /**
     * Tail map when the supplied key does not exist – the view should start
     * at the least key greater than the supplied key (the ceiling key).
     */
    @Test
    void tailMap_withNonExistentKey_roundedUp() {
        // key 2.5 is not present; the next higher key is 3
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(2.5);
        assertEquals(2, tail.size(), "Expected two entries (3 and 4)");
        assertTrue(tail.containsKey(3));
        assertTrue(tail.containsKey(4));
        assertFalse(tail.containsKey(2));
    }

    /**
     * Tail map when the supplied key is greater than any key in the map.
     * The result must be an empty view.
     */
    @Test
    void tailMap_keyGreaterThanAllEntries_returnsEmpty() {
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(10);
        assertTrue(tail.isEmpty(), "Tail map should be empty for key > max");
    }

    /**
     * Supplying {@code null} as the key must raise {@link NullPointerException}
     * because the method signature does not allow a null key (the map is
     * null‑safe for put/get but not for navigation methods).
     */
    @Test
    void tailMap_nullKey_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> intMap.tailMap(null), "Calling tailMap(null) should throw NullPointerException");
    }

    /**
     * Verify that the returned view is live – modifications to the original
     * map are reflected in the view.
     */
    @Test
    void tailMap_viewReflectsModificationsToOriginalMap() {
        // Obtain a view starting at key 1
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(1);
        // Add a new entry to the original map
        intMap.put(5, "five");
        // The tail view must now contain the newly added entry
        assertEquals(5, tail.size(), "Tail view should see the new entry");
        assertEquals("five", tail.get(5));
    }

    /**
     * Tail map on an empty underlying map must always be empty.
     */
    @Test
    void tailMap_onEmptyMap_returnsEmpty() {
        ConcurrentNavigableMapNullSafe<Integer, String> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<Integer, String> tail = empty.tailMap(1);
        assertTrue(tail.isEmpty(), "Tail map of an empty map must be empty");
    }

    /**
     * When the supplied key is lower than the smallest key, the view should
     * contain all entries.
     */
    @Test
    void tailMap_keyLessThanSmallest_containsAll() {
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(0);
        assertEquals(4, tail.size(), "All entries should be present");
        assertTrue(tail.keySet().containsAll(Arrays.asList(1, 2, 3, 4)));
    }

    /**
     * Tail map with a custom reverse comparator.
     * The map orders keys in descending order, therefore {@code tailMap(3)}
     * (inclusive) should return the keys 4 and 3.
     */
    @Test
    void tailMap_reverseComparator_existingKey() {
        // reverseMap orders keys descending: 4,3,2,1
        ConcurrentNavigableMap<Integer, String> tail = reverseMap.tailMap(3);
        // Expected order is still a map view; we only check contents.
        assertEquals(2, tail.size(), "Reverse tailMap should contain two entries");
        assertTrue(tail.containsKey(4));
        assertTrue(tail.containsKey(3));
        assertFalse(tail.containsKey(2));
    }

    /**
     * Passing a key of an incompatible type must result in {@link ClassCastException}.
     */
    @Test
    void tailMap_incompatibleKeyType_throwsClassCastException() {
        // intMap expects Integer keys; a String should cause a ClassCastException
        assertThrows(ClassCastException.class, () -> intMap.tailMap("stringKey"), "Using a non‑comparable key type must throw ClassCastException");
    }

    /**
     * Verify that removals from the original map are reflected in the view.
     */
    @Test
    void tailMap_viewReflectsRemovalFromOriginalMap() {
        // Obtain a view starting at key 2
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(2);
        // Remove key 3 from the original map
        intMap.remove(3);
        // The view must no longer contain key 3
        assertFalse(tail.containsKey(3), "Removed entry must disappear from the view");
        // Remaining keys should be 2 and 4
        assertEquals(Set.of(2, 4), tail.keySet());
    }

    /**
     * Tail map when the map contains null values – values should be preserved.
     */
    @Test
    void tailMap_nullValuesPresent_areIncluded() {
        intMap.put(5, null);
        // View starting at 1 should include the null value entry
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(1);
        assertTrue(tail.containsKey(5));
        assertNull(tail.get(5), "Null value must be present in the view");
    }
}
