package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_subMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate map with a predictable ordering and a null key (null is considered last)
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; the map itself does not hold external resources
        map = null;
    }

    // ----------------------------------------------------------------------
    private static Stream<Arguments> normalRangeProvider() {
        return Stream.of(// fromInclusive = true, toInclusive = false → ["b","c"]
        Arguments.of("b", true, "d", false, Set.of("b", "c")), // fromInclusive = false, toInclusive = true → ["c","d"]
        Arguments.of("b", false, "d", true, Set.of("c", "d")), // both exclusive → ["c"]
        Arguments.of("b", false, "d", false, Set.of("c")));
    }

    @Test
    void subMap_inclusiveFrom_exclusiveTo_excludesUpperBound() {
        // fromKey = "a" (inclusive), toKey = "c" (exclusive)
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", true, "c", false);
        // Expected keys: "a", "b"
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
        assertFalse(sub.containsKey(null));
        assertEquals(1, sub.get("a"));
        assertEquals(2, sub.get("b"));
    }

    @Test
    void subMap_inclusiveFrom_inclusiveTo_includesUpperBoundAndNullKey() {
        // fromKey = "b" (inclusive), toKey = null (inclusive)
        // Since null is considered the greatest key, it should be included when inclusive
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("b", true, null, true);
        // Expected keys: "b", "c", null
        assertEquals(3, sub.size());
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
        assertTrue(sub.containsKey(null));
        assertFalse(sub.containsKey("a"));
        assertEquals(2, sub.get("b"));
        assertEquals(3, sub.get("c"));
        assertEquals(100, sub.get(null));
    }

    @Test
    void subMap_fromNullKeyResultsInEmptyMap() {
        // fromKey = null (inclusive) is the greatest key; any toKey that is not null will be before it
        ConcurrentNavigableMap<String, Integer> sub = map.subMap(null, true, "z", false);
        // No keys should be in the view because null is last
        assertTrue(sub.isEmpty());
    }

    @Test
    void subMap_modificationsReflectInOriginalMap() {
        // Create a subMap that includes "b" and "c"
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("b", true, "d", false);
        // Modify via the sub‑map
        sub.put("b", 20);
        // "d" is not present in original map but within the view range
        sub.put("d", 40);
        // Verify changes are visible in the original map
        assertEquals(20, map.get("b"));
        assertEquals(40, map.get("d"));
        assertTrue(map.containsKey("d"));
    }

    @Test
    void subMap_excludesNullWhenUpperBoundExclusive() {
        // fromKey = "b" (inclusive), toKey = null (exclusive) → null should be omitted
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("b", true, null, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
        assertFalse(sub.containsKey(null));
    }

    @ParameterizedTest(name = "normalRange [{index}] from={0} inc={1} to={2} inc={3}")
    @MethodSource("normalRangeProvider")
    void subMap_normalRange(String fromKey, boolean fromInc, String toKey, boolean toInc, Set<String> expectedKeys) {
        // Build a fresh map that contains a‑d (no null key for these tests)
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put("a", 1);
        localMap.put("b", 2);
        localMap.put("c", 3);
        localMap.put("d", 4);
        ConcurrentNavigableMap<String, Integer> sub = localMap.subMap(fromKey, fromInc, toKey, toInc);
        assertEquals(expectedKeys.size(), sub.size(), "Unexpected sub‑map size");
        for (String k : expectedKeys) {
            assertTrue(sub.containsKey(k), "Expected key missing: " + k);
        }
        // Ensure no unexpected keys are present
        for (String k : List.of("a", "b", "c", "d")) {
            if (!expectedKeys.contains(k)) {
                assertFalse(sub.containsKey(k), "Unexpected key present: " + k);
            }
        }
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_bothBoundsInclusive() {
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put("a", 1);
        localMap.put("b", 2);
        localMap.put("c", 3);
        localMap.put("d", 4);
        ConcurrentNavigableMap<String, Integer> sub = localMap.subMap("b", true, "d", true);
        assertEquals(Set.of("b", "c", "d"), sub.keySet());
        assertEquals(3, sub.size());
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_equalBoundsInclusive() {
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put("c", 3);
        ConcurrentNavigableMap<String, Integer> sub = localMap.subMap("c", true, "c", true);
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey("c"));
    }

    @Test
    void subMap_equalBoundsExclusive() {
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put("c", 3);
        ConcurrentNavigableMap<String, Integer> sub = localMap.subMap("c", false, "c", false);
        assertTrue(sub.isEmpty());
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_fromKeyGreaterThanToKey_throws() {
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put("a", 1);
        localMap.put("b", 2);
        localMap.put("c", 3);
        assertThrows(IllegalArgumentException.class, () -> localMap.subMap("e", true, "b", true));
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_nullLowerBoundInclusive() {
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put(null, 100);
        localMap.put("a", 1);
        localMap.put("b", 2);
        // from null (inclusive) to "b" (exclusive)
        ConcurrentNavigableMap<String, Integer> sub = localMap.subMap(null, true, "b", false);
        // Expected keys: null and "a"
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(null));
        assertTrue(sub.containsKey("a"));
        assertFalse(sub.containsKey("b"));
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_nullUpperBoundInvalid_throws() {
        // In natural ordering the wrapper masks null to a sentinel that is *greater* than any real key.
        // Using it as the *upper* bound while the lower bound is a normal key makes the range invalid.
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put("a", 1);
        localMap.put(null, 100);
        assertThrows(IllegalArgumentException.class, () -> localMap.subMap("a", true, null, true));
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_onEmptyMap_returnsEmpty() {
        ConcurrentNavigableMapNullSafe<String, Integer> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<String, Integer> sub = emptyMap.subMap("a", true, "z", true);
        assertTrue(sub.isEmpty());
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_singleElement_inRange() {
        ConcurrentNavigableMapNullSafe<String, Integer> single = new ConcurrentNavigableMapNullSafe<>();
        single.put("c", 3);
        ConcurrentNavigableMap<String, Integer> sub = single.subMap("b", false, "d", true);
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey("c"));
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_singleElement_outOfRange() {
        ConcurrentNavigableMapNullSafe<String, Integer> single = new ConcurrentNavigableMapNullSafe<>();
        single.put("c", 3);
        ConcurrentNavigableMap<String, Integer> sub = single.subMap("c", false, "c", false);
        assertTrue(sub.isEmpty());
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_customReverseComparator() {
        // Build a map that orders keys in reverse (descending) order.
        Comparator<String> reverse = Collections.reverseOrder();
        ConcurrentSkipListMap<String, Integer> backing = new ConcurrentSkipListMap<>(reverse);
        ConcurrentNavigableMapNullSafe<String, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(backing, reverse);
        revMap.put("a", 1);
        revMap.put("b", 2);
        revMap.put("c", 3);
        revMap.put("d", 4);
        // null is considered *first* in the descending view
        revMap.put(null, 100);
        // from "d" (inclusive) to "b" (inclusive) in a descending map should yield d, c, b
        ConcurrentNavigableMap<String, Integer> sub = revMap.subMap("d", true, "b", true);
        assertEquals(3, sub.size());
        assertEquals(List.of("d", "c", "b"), new ArrayList<>(sub.keySet()));
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_fullRangeInclusive() {
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put("a", 1);
        localMap.put("b", 2);
        localMap.put("c", 3);
        localMap.put("d", 4);
        localMap.put(null, 100);
        ConcurrentNavigableMap<String, Integer> sub = localMap.subMap("a", true, "d", true);
        assertEquals(4, sub.size());
        assertEquals(Set.of("a", "b", "c", "d"), sub.keySet());
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_incompatibleFromKey_throwsClassCastException() {
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put("a", 1);
        localMap.put("b", 2);
        // fromKey is an Integer, which cannot be compared to String keys
        assertThrows(ClassCastException.class, () -> localMap.subMap((String) (Object) Integer.valueOf(1), true, "d", true));
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_incompatibleToKey_throwsClassCastException() {
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put("b", 2);
        // toKey is an Object that is not comparable to String
        Object badKey = new Object();
        assertThrows(ClassCastException.class, () -> localMap.subMap("b", true, (String) badKey, false));
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_customComparatorMismatchedKey_throwsClassCastException() {
        // Comparator that expects String keys (casts to String)
        Comparator<String> strictStringComparator = (k1, k2) -> ((String) k1).compareTo((String) k2);
        ConcurrentSkipListMap<String, Integer> backing = new ConcurrentSkipListMap<>(strictStringComparator);
        ConcurrentNavigableMapNullSafe<String, Integer> mapWithComparator = new ConcurrentNavigableMapNullSafe<>(backing, strictStringComparator);
        mapWithComparator.put("a", 1);
        mapWithComparator.put("b", 2);
        // Pass a Long as toKey – the comparator will attempt to cast it to String and fail
        assertThrows(ClassCastException.class, () -> mapWithComparator.subMap("a", true, (String) (Object) Long.valueOf(5L), true));
    }

    // ----------------------------------------------------------------------
    @Test
    void subMap_nullLowerBoundExclusive_throwsIllegalArgumentException() {
        // Using natural ordering (no custom comparator). The wrapper masks null to a sentinel that is the
        // smallest possible key. An exclusive lower bound on that sentinel yields an invalid range.
        ConcurrentNavigableMapNullSafe<String, Integer> localMap = new ConcurrentNavigableMapNullSafe<>();
        localMap.put(null, 100);
        localMap.put("a", 1);
        localMap.put("b", 2);
        assertThrows(IllegalArgumentException.class, () -> localMap.subMap(null, false, "c", true));
    }
}
