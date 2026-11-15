package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_tailMap_2_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * Creates a fresh map instance before each test and populates it with a
     * deterministic set of entries that include a {@code null} key.
     */
    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        // null is treated as the greatest key
        map.put(null, 99);
    }

    /**
     * Clears the reference after each test – helps the GC and mirrors the template.
     */
    @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    @Test
    void tailMap_inclusiveNonNullKey_returnsCorrectView() {
        // GIVEN: a map with keys [alpha, beta, gamma, null] (null is last)
        // WHEN: we request a tailMap starting at "beta"
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("beta");
        // THEN: the view must contain "beta", "gamma" and the null key
        assertEquals(3, tail.size(), "Tail map should contain three entries");
        assertTrue(tail.containsKey("beta"));
        assertTrue(tail.containsKey("gamma"));
        assertTrue(tail.containsKey(null));
        // The view must be *live*: changes to the view affect the original map
        tail.put("delta", 4);
        assertEquals(4, map.size());
        assertEquals(4, map.get("delta"));
        // Removing via the view also removes from the original map
        tail.remove("beta");
        assertFalse(map.containsKey("beta"));
        assertEquals(3, map.size());
    }

    @Test
    void tailMap_inclusiveNullKey_returnsOnlyNullEntry() {
        // GIVEN: the same map as setUp (null is the greatest key)
        // WHEN: we request a tailMap starting at null
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null);
        // THEN: only the null entry should be present
        assertEquals(1, tail.size(), "Tail map starting at null should contain only the null entry");
        assertTrue(tail.containsKey(null));
        assertEquals(99, tail.get(null));
        // Adding a new entry via the view must appear in the original map
        tail.put("epsilon", 5);
        // Because null is considered greater than any non‑null key, the view should now contain
        // both null and the newly added key (the view is inclusive of null and any keys greater
        // than null – there are none, so the new key appears because it is added *after* the view
        // was created, and the view reflects the underlying map).
        assertTrue(tail.containsKey("epsilon"));
        assertEquals(5, map.get("epsilon"));
    }

    @Test
    void tailMap_withNonExistingKey_returnsEmptyWhenKeyIsGreaterThanAll() {
        // GIVEN: a map where the greatest non‑null key is "gamma"
        // WHEN: we request a tailMap starting at "zeta" (greater than any existing key)
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("zeta");
        // THEN: the view should contain only the null entry because null is considered
        // the greatest key and the request is inclusive.
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
        assertFalse(tail.containsKey("zeta"));
    }

    /**
     * Basic inclusive behavior with natural ordering (Integer keys).
     *
     * The map initially contains {1=A,2=B,3=C,4=D,5=E}.
     */
    @ParameterizedTest(name = "tailMap({0}) inclusive basic behavior")
    @CsvSource({ "2, 2,3,4,5", "0, 1,2,3,4,5", // empty result
    "6, " })
    void tailMap_basicInclusiveBehavior(Integer fromKey, String... expectedKeys) {
        // Setup a map with natural Integer ordering
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "A");
        intMap.put(2, "B");
        intMap.put(3, "C");
        intMap.put(4, "D");
        intMap.put(5, "E");
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(fromKey);
        // Build the expected key set
        Set<Integer> expected = new LinkedHashSet<>();
        for (String s : expectedKeys) {
            if (!s.isEmpty()) {
                expected.add(Integer.valueOf(s));
            }
        }
        assertEquals(expected.size(), tail.size(), "Unexpected size for tailMap(" + fromKey + ")");
        assertEquals(expected, tail.keySet(), "Unexpected keys for tailMap(" + fromKey + ")");
    }

    @Test
    void tailMap_liveView_additionIsReflectedInOriginalMap() {
        // Map with natural ordering
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.putAll(Map.of(1, "A", 2, "B", 3, "C", 4, "D", 5, "E"));
        // Obtain a view starting at key 3
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(3);
        // Add a new entry via the view
        tail.put(6, "F");
        // The original map must now contain the new entry
        assertTrue(intMap.containsKey(6));
        assertEquals("F", intMap.get(6));
    }

    @Test
    void tailMap_liveView_removalIsReflectedInOriginalMap() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.putAll(Map.of(1, "A", 2, "B", 3, "C", 4, "D", 5, "E"));
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(3);
        // Remove key 4 via the view
        tail.remove(4);
        assertFalse(intMap.containsKey(4));
        assertEquals(4, intMap.size());
    }

    @Test
    void tailMap_nullKey_handling_isAllowedAndReturnsOnlyNullEntry() {
        // The implementation treats null as the greatest key.
        ConcurrentNavigableMapNullSafe<String, Integer> nsMap = new ConcurrentNavigableMapNullSafe<>();
        nsMap.put("a", 1);
        nsMap.put(null, 99);
        nsMap.put("b", 2);
        ConcurrentNavigableMap<String, Integer> tail = nsMap.tailMap(null);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
        assertEquals(99, tail.get(null));
    }

    @Test
    void tailMap_typeSafety_classCastException() {
        // Map with Integer keys
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.putAll(Map.of(1, "A", 2, "B"));
        // Supplying a String key should cause ClassCastException
        assertThrows(ClassCastException.class, () -> intMap.tailMap("B"));
    }

    @Test
    void tailMap_boundaryInclusive_edgeCases() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.putAll(Map.of(1, "A", 2, "B", 3, "C", 4, "D", 5, "E"));
        // fromKey == greatest key (5) → only that key
        ConcurrentNavigableMap<Integer, String> tailGreatest = intMap.tailMap(5);
        assertEquals(Set.of(5), tailGreatest.keySet());
        // fromKey == smallest key (1) → whole map
        ConcurrentNavigableMap<Integer, String> tailSmallest = intMap.tailMap(1);
        assertEquals(Set.of(1, 2, 3, 4, 5), tailSmallest.keySet());
    }

    @Test
    void tailMap_viewReflectsRemovalFromOriginalMap() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.putAll(Map.of(1, "A", 2, "B", 3, "C", 4, "D", 5, "E"));
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(2);
        // Remove key 3 directly from the backing map
        intMap.remove(3);
        assertFalse(tail.containsKey(3));
        assertEquals(4, tail.size());
    }

    @Test
    void tailMap_concurrentModificationsDuringIteration_noCME_andNewKeysVisible() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.putAll(Map.of(1, "A", 2, "B", 3, "C", 4, "D"));
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(2);
        // Iterate while inserting new higher keys
        Iterator<Map.Entry<Integer, String>> it = tail.entrySet().iterator();
        int insertCount = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey() == 3) {
                // Insert two new entries > 3
                intMap.put(5, "E");
                intMap.put(6, "F");
                insertCount = 2;
            }
        }
        // After iteration, the tail view must contain the newly added keys
        assertTrue(tail.containsKey(5));
        assertTrue(tail.containsKey(6));
        // keys 2,3,4,5,6
        assertEquals(5, tail.size());
        assertEquals(6, intMap.size());
    }

    @Test
    void tailMap_customComparator_reverseCaseInsensitive() {
        // Comparator: reverse order, case‑insensitive
        Comparator<String> revCaseInsensitive = (a, b) -> {
            if (a == null && b == null)
                return 0;
            // null is considered smallest in this custom view
            if (a == null)
                return -1;
            if (b == null)
                return 1;
            return b.compareToIgnoreCase(a);
        };
        ConcurrentNavigableMapNullSafe<String, Integer> cmpMap = new ConcurrentNavigableMapNullSafe<>(revCaseInsensitive);
        cmpMap.putAll(Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5));
        // tailMap("c") according to the comparator (reverse order) should give keys >= "c"
        // In reverse order that means ["c","b","a"] but because we use tailMap (inclusive)
        // with the same comparator, the resulting set is ["c","b","a"].
        ConcurrentNavigableMap<String, Integer> tail = cmpMap.tailMap("c");
        List<String> expectedOrder = List.of("c", "b", "a");
        assertEquals(expectedOrder, new ArrayList<>(tail.keySet()), "Tail map with reverse case‑insensitive comparator should contain keys in reverse order");
    }

    @Test
    void tailMap_emptyMap_alwaysReturnsEmptyView() {
        ConcurrentNavigableMapNullSafe<Integer, String> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<Integer, String> tail = emptyMap.tailMap(1);
        assertTrue(tail.isEmpty(), "Tail map of an empty map must be empty");
    }

    @Test
    void tailMap_nullValueHandling_includesEntryWithNullValue() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "A");
        intMap.put(2, "B");
        // null value
        intMap.put(3, null);
        intMap.put(4, "D");
        intMap.put(5, "E");
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(2);
        assertEquals(Set.of(2, 3, 4, 5), tail.keySet());
        assertNull(tail.get(3), "Value for key 3 should be null");
    }

    @Test
    void tailMap_comparatorInducedIllegalArgumentException() {
        // Comparator that forbids negative numbers
        Comparator<Integer> nonNegative = (a, b) -> {
            if (a < 0 || b < 0) {
                throw new IllegalArgumentException("Negative keys not allowed");
            }
            return Integer.compare(a, b);
        };
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>(nonNegative);
        map.putAll(Map.of(0, "zero", 1, "one", 2, "two"));
        assertThrows(IllegalArgumentException.class, () -> map.tailMap(-1));
    }

    @Test
    void tailMap_comparatorInducedRuntimeException() {
        // Comparator that throws RuntimeException when key equals "boom"
        Comparator<String> boomComparator = (a, b) -> {
            if ("boom".equals(a) || "boom".equals(b)) {
                throw new RuntimeException("boom encountered");
            }
            return a.compareTo(b);
        };
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>(boomComparator);
        map.putAll(Map.of("a", 1, "b", 2, "c", 3));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.tailMap("boom"));
        assertEquals("boom encountered", ex.getMessage());
    }
}
