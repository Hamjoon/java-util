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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_headMap_2_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    private static final class NonComparable {

        final int value;

        NonComparable(int v) {
            this.value = v;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test.
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null key is allowed and considered “last”
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to aid GC – not strictly necessary.
        map.clear();
    }

    private static Stream<Arguments> numericHeadMapProvider() {
        return Stream.of(// toKey less than the smallest key → empty view
        Arguments.of(0, Collections.emptyList()), // toKey equals an existing key → keys strictly less than it
        Arguments.of(10, Collections.singletonList(5)), // toKey greater than the greatest key → all entries
        Arguments.of(20, Arrays.asList(5, 10, 15)), // toKey between existing keys → keys lower than it
        Arguments.of(12, Arrays.asList(5, 10)));
    }

    @Test
    void headMap_excludesUpperBoundKey() {
        // “b” is the upper bound (exclusive)
        ConcurrentNavigableMap<String, Integer> head = map.headMap("b");
        // Expected keys: only “a”
        assertEquals(1, head.size(), "headMap should contain only keys less than the bound");
        assertTrue(head.containsKey("a"));
        assertFalse(head.containsKey("b"));
        assertFalse(head.containsKey("c"));
        assertFalse(head.containsKey(null));
        // Verify that the value is the same as in the original map.
        assertEquals(1, head.get("a"));
    }

    @Test
    void headMap_reflectsChangesInOriginalMap() {
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c");
        // Add a new entry via the view.
        head.put("b", 20);
        // The original map must see the updated value.
        assertEquals(20, map.get("b"));
        // Size of original map stays the same because we replaced an existing key.
        assertEquals(4, map.size());
    }

    @Test
    void headMap_isLiveViewWhenRemovingThroughOriginal() {
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c");
        // Remove a key directly from the original map.
        map.remove("a");
        // The view must no longer contain that key.
        assertFalse(head.containsKey("a"));
        // only “b” is < "c" but we removed “a”; “b” is still present
        assertEquals(0, head.size());
        assertTrue(head.containsKey("b"));
    }

    @Test
    void headMap_withNullUpperBoundIncludesAllNonNullKeys() {
        // Null is considered the greatest key, so headMap(null) should contain every
        // key except the null key itself.
        ConcurrentNavigableMap<String, Integer> head = map.headMap(null);
        assertEquals(3, head.size(), "All non‑null keys should be present");
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertTrue(head.containsKey("c"));
        assertFalse(head.containsKey(null));
        // Verify values are correct.
        assertEquals(1, head.get("a"));
        assertEquals(2, head.get("b"));
        assertEquals(3, head.get("c"));
    }

    @Test
    void headMap_onEmptyMapReturnsEmpty() {
        // now the map is empty, including no null key
        map.clear();
        ConcurrentNavigableMap<String, Integer> head = map.headMap("anyKey");
        assertTrue(head.isEmpty(), "headMap of an empty map must be empty");
    }

    @Test
    void headMap_returnsCorrectConcreteType() {
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c");
        // The implementation returns a ConcurrentNavigableMap; we simply verify the
        // runtime type implements the expected interface.
        assertTrue(head instanceof ConcurrentNavigableMap, "Result should be a ConcurrentNavigableMap");
    }

    /**
     * Parameterised tests for the basic numeric scenarios where the map
     * contains the keys {@code [5, 10, 15]}.  The expected result is expressed
     * as a list of keys that should be present in the view returned by
     * {@code headMap(toKey)}.
     */
    @ParameterizedTest(name = "[{index}] toKey={0} → expected={1}")
    @MethodSource("numericHeadMapProvider")
    void headMap_numericScenarios(Integer toKey, List<Integer> expectedKeys) {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(5, "five");
        intMap.put(10, "ten");
        intMap.put(15, "fifteen");
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(toKey);
        List<Integer> actualKeys = new ArrayList<>(head.keySet());
        assertEquals(expectedKeys, actualKeys, () -> "headMap(" + toKey + ") should contain keys " + expectedKeys);
    }

    @Test
    void headMap_onEmptyMapAnyKeyReturnsEmpty() {
        ConcurrentNavigableMapNullSafe<Integer, String> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<Integer, String> head = emptyMap.headMap(42);
        assertTrue(head.isEmpty(), "headMap on an empty map must be empty regardless of the bound");
    }

    @Test
    void headMap_toKeyEqualsSmallestKeyReturnsEmpty() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(5, "five");
        intMap.put(10, "ten");
        intMap.put(15, "fifteen");
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(5);
        assertTrue(head.isEmpty(), "When toKey equals the smallest key the view must be empty");
    }

    @Test
    void headMap_toKeyIntegerMaxValueReturnsAll() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(5, "five");
        intMap.put(10, "ten");
        intMap.put(15, "fifteen");
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(Integer.MAX_VALUE);
        assertEquals(Set.of(5, 10, 15), head.keySet(), "Integer.MAX_VALUE should be greater than any key, therefore all entries are returned");
    }

    @Test
    void headMap_incompatibleKeyTypeThrowsClassCastException() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(5, "five");
        intMap.put(10, "ten");
        intMap.put(15, "fifteen");
        assertThrows(ClassCastException.class, () -> intMap.headMap("string"), "Passing a key of incompatible type must result in ClassCastException");
    }

    @Test
    void headMap_stringComparatorNaturalOrder() {
        // Natural order comparator (case‑sensitive).  Keys: a, c, z.
        ConcurrentNavigableMapNullSafe<String, Integer> strMap = new ConcurrentNavigableMapNullSafe<>(Comparator.naturalOrder());
        strMap.put("a", 1);
        strMap.put("c", 3);
        strMap.put("z", 26);
        // 'm' lies between 'c' and 'z'
        ConcurrentNavigableMap<String, Integer> head = strMap.headMap("m");
        assertEquals(Set.of("a", "c"), head.keySet(), "headMap(\"m\") should contain keys less than \"m\"");
    }

    @Test
    void headMap_liveView_additionIsReflected() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(5, "five");
        intMap.put(10, "ten");
        intMap.put(15, "fifteen");
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(10);
        // add a new key that is lower than the upper bound
        intMap.put(7, "seven");
        assertEquals(Set.of(5, 7), head.keySet(), "The headMap view must reflect additions to the original map that fall within the range");
    }

    @Test
    void headMap_liveView_removalIsReflected() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(5, "five");
        intMap.put(10, "ten");
        intMap.put(15, "fifteen");
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(10);
        // remove the only key that satisfies the view
        intMap.remove(5);
        assertTrue(head.isEmpty(), "After removing a key that belongs to the view, the headMap must become empty");
    }

    @Test
    void headMap_nonComparableKeyThrowsClassCastException() {
        // Map without a comparator and a key type that does not implement Comparable.
        ConcurrentNavigableMapNullSafe<NonComparable, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(new NonComparable(1), "one");
        assertThrows(ClassCastException.class, () -> map.headMap(new NonComparable(2)), "Using a non‑Comparable key without a comparator must throw ClassCastException");
    }

    @Test
    void headMap_comparatorThrowsIllegalArgumentException() {
        Comparator<Integer> rejectingNegative = (a, b) -> {
            if (a < 0 || b < 0) {
                throw new IllegalArgumentException("negative values not allowed");
            }
            return Integer.compare(a, b);
        };
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>(rejectingNegative);
        map.put(1, "one");
        map.put(2, "two");
        assertThrows(IllegalArgumentException.class, () -> map.headMap(-5), "Comparator should propagate IllegalArgumentException for negative toKey");
    }

    @Test
    void headMap_comparatorThrowsIllegalStateException() {
        Comparator<Integer> explodeOn42 = (a, b) -> {
            if (a == 42 || b == 42) {
                throw new IllegalStateException("42 is forbidden");
            }
            return Integer.compare(a, b);
        };
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>(explodeOn42);
        map.put(10, "ten");
        map.put(20, "twenty");
        assertThrows(IllegalStateException.class, () -> map.headMap(42), "Comparator should propagate IllegalStateException when it encounters the forbidden key");
    }
}
