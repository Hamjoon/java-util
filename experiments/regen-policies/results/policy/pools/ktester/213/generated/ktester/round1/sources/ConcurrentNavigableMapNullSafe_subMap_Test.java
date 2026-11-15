package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_subMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate a deterministic ordering (natural String order)
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is considered *last* in the ordering
        map.put(null, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    /**
     * Helper to build a sub‑map and assert its content (keys & values).
     */
    private static void assertSubMapContent(ConcurrentNavigableMap<String, String> sub, List<String> expectedKeys, List<String> expectedValues) {
        assertEquals(expectedKeys.size(), sub.size(), "Sub‑map size does not match expected size");
        assertEquals(new LinkedHashSet<>(expectedKeys), sub.keySet(), "Sub‑map keys differ from expected");
        List<String> actualValues = new ArrayList<>();
        for (String k : expectedKeys) {
            actualValues.add(sub.get(k));
        }
        assertEquals(expectedValues, actualValues, "Sub‑map values differ from expected");
    }

    private static Stream<Arguments> normalSubMapScenarios() {
        return Stream.of(// Standard range where both bounds are non‑null and inclusive/exclusive flags produce a sub‑map containing keys a and b but excluding c.
        Arguments.of("Standard range a..c (inc/exc)", "a", true, "c", false, List.of("a", "b"), List.of("A", "B")), // From‑key is null (masked). Inclusive on both ends, so the sub‑map should contain the null entry and the entries for a and b.
        Arguments.of("Null lower bound inclusive, upper bound b inclusive", null, true, "b", true, List.of(null, "a", "b"), List.of("nullVal", "A", "B")));
    }

    private static Stream<Arguments> inclusiveExclusiveVariations() {
        return Stream.of(// Exclusive lower bound excludes "a" while inclusive upper bound includes "c", resulting in keys b and c.
        Arguments.of("Exclusive a, inclusive c", "a", false, "c", true, List.of("b", "c"), List.of("B", "C")), // Lower bound is the masked null key but exclusive, so the range starts after null; upper bound includes "a".
        Arguments.of("Null exclusive lower, a inclusive upper", null, false, "a", true, List.of("a"), List.of("A")), // Both bounds refer to the masked null key and are exclusive → empty map.
        Arguments.of("Null exclusive both ends (empty)", null, false, null, false, List.of(), List.of()), // Both bounds are the masked null key and inclusive → only null entry.
        Arguments.of("Null inclusive both ends (single null)", null, true, null, true, List.of((String) null), List.of("nullVal")), // Identical bounds that are both inclusive produce a sub‑map containing exactly the single entry for key a.
        Arguments.of("Identical inclusive a..a", "a", true, "a", true, List.of("a"), List.of("A")), // Identical bounds where the upper bound is exclusive result in an empty sub‑map.
        Arguments.of("Identical a inclusive lower, exclusive upper (empty)", "a", true, "a", false, List.of(), List.of()));
    }

    private static Stream<Arguments> invalidRangeExceptions() {
        return Stream.of(// From‑key (b) greater than to‑key (a)
        Arguments.of("b > a (both inclusive)", "b", true, "a", true), // Non‑null exclusive lower bound greater than masked null upper bound
        Arguments.of("b exclusive lower, null inclusive upper (invalid)", "b", false, null, true), // From‑key greater than to‑key (c > a)
        Arguments.of("c > a (both inclusive)", "c", true, "a", true), // Null lower exclusive, null upper inclusive (invalid per JSON)
        Arguments.of("null exclusive lower, null inclusive upper", null, false, null, true), // Null lower inclusive, null upper exclusive (invalid per JSON)
        Arguments.of("null inclusive lower, null exclusive upper", null, true, null, false));
    }

    /* -------------------------------------------------------------
       1️⃣  Basic inclusive / exclusive behaviour
       ------------------------------------------------------------- */
    @Test
    void subMap_inclusiveFrom_exclusiveTo() {
        // from “a” inclusive, to “c” exclusive → should contain “a” and “b”
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", true, "c", false);
        assertEquals(2, sub.size(), "Sub‑map should contain exactly two entries");
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
        assertFalse(sub.containsKey(null));
    }

    @Test
    void subMap_exclusiveFrom_exclusiveTo() {
        // from “a” exclusive, to “c” exclusive → should contain only “b”
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", false, "c", false);
        assertEquals(1, sub.size());
        assertFalse(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
    }

    @Test
    void subMap_inclusiveBothEnds() {
        // from “a” inclusive, to “c” inclusive → should contain “a”, “b”, “c”
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", true, "c", true);
        assertEquals(3, sub.size());
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
    }

    /* -------------------------------------------------------------
       2️⃣  Null‑key handling (null is considered last)
       ------------------------------------------------------------- */
    @Test
    void subMap_withNullKey_asUpperBound() {
        // Sub‑map that ends with the null key (inclusive)
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("b", true, null, true);
        // Expected: "b", "c", null
        assertEquals(3, sub.size());
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
        assertTrue(sub.containsKey(null));
        assertFalse(sub.containsKey("a"));
    }

    @Test
    void subMap_withNullKey_asLowerBound() {
        // Sub‑map that starts at null (inclusive) – because null is last,
        // the view can only contain the null entry.
        ConcurrentNavigableMap<String, Integer> sub = map.subMap(null, true, null, true);
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey(null));
        assertFalse(sub.containsKey("a"));
    }

    /* -------------------------------------------------------------
       3️⃣  Mutability – changes in the sub‑map affect the original map
       ------------------------------------------------------------- */
    @Test
    void subMap_modificationsReflectedInParent() {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", true, "c", false);
        // sub currently contains "a" and "b"
        assertEquals(2, sub.size());
        // Add a new entry via the sub‑map
        // “d” is after “c”, but still inside the underlying map
        sub.put("d", 4);
        // The original map must now contain the new entry
        assertEquals(5, map.size());
        assertEquals(4, map.get("d"));
        // The sub‑map view should also contain it because it lies within the original bounds
        assertTrue(sub.containsKey("d"));
    }

    @Test
    void subMap_removeEntryFromParentIsVisibleInSubMap() {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", true, "c", true);
        assertTrue(sub.containsKey("b"));
        // Remove via the parent map
        map.remove("b");
        assertNull(map.get("b"));
        assertFalse(sub.containsKey("b"));
    }

    /* -------------------------------------------------------------
       4️⃣  Edge cases – empty sub‑maps
       ------------------------------------------------------------- */
    @Test
    void subMap_emptyWhenFromKeyGreaterThanToKey() {
        // from “c” (greater) to “a” (smaller) → empty view
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("c", true, "a", true);
        assertTrue(sub.isEmpty());
        assertEquals(0, sub.size());
    }

    @Test
    void subMap_emptyWhenRangeExcludesAllElements() {
        // from “a” exclusive to “a” inclusive → empty because there is no element strictly between them
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", false, "a", true);
        assertTrue(sub.isEmpty());
    }

    /* -------------------------------------------------------------
       5️⃣  Type sanity – the returned map should also be a
            ConcurrentNavigableMapNullSafe instance.
       ------------------------------------------------------------- */
    @Test
    void subMap_returnsConcurrentNavigableMapNullSafeInstance() {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", true, "c", true);
        assertTrue(sub instanceof ConcurrentNavigableMapNullSafe, "subMap should return a ConcurrentNavigableMapNullSafe view");
    }

    /* ---------- Normal subMap scenarios ---------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalSubMapScenarios")
    void normalSubMapScenarios(String description, String fromKey, boolean fromInc, String toKey, boolean toInc, List<String> expKeys, List<String> expVals) {
        ConcurrentNavigableMap<String, String> sub = map.subMap(fromKey, fromInc, toKey, toInc);
        assertSubMapContent(sub, expKeys, expVals);
    }

    /* ---------- Inclusive/Exclusive variations ---------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("inclusiveExclusiveVariations")
    void inclusiveExclusiveVariations(String description, String fromKey, boolean fromInc, String toKey, boolean toInc, List<String> expKeys, List<String> expVals) {
        ConcurrentNavigableMap<String, String> sub = map.subMap(fromKey, fromInc, toKey, toInc);
        assertSubMapContent(sub, expKeys, expVals);
    }

    /* ---------- Invalid range exceptions (non‑null bounds) ---------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidRangeExceptions")
    void invalidRangeExceptions(String description, String fromKey, boolean fromInc, String toKey, boolean toInc) {
        assertThrows(IllegalArgumentException.class, () -> map.subMap(fromKey, fromInc, toKey, toInc), "Expected IllegalArgumentException for range: " + description);
    }

    /* ---------- View consistency (modifications through subMap) ---------- */
    @Test
    void viewConsistency_removeViaSubMap() {
        // Obtain a sub‑map covering a‑c inclusive, remove key "b" via the sub‑map,
        // and verify that the removal is reflected in the original map.
        ConcurrentNavigableMap<String, String> sub = map.subMap("a", true, "c", true);
        assertTrue(sub.containsKey("b"));
        sub.remove("b");
        assertNull(sub.get("b"));
        assertNull(map.get("b"));
    }

    @Test
    void viewConsistency_putOutsideUpperBoundThrows() {
        // Sub‑map a‑c exclusive upper bound; attempting to put "c" should fail.
        ConcurrentNavigableMap<String, String> sub = map.subMap("a", true, "c", false);
        assertThrows(IllegalArgumentException.class, () -> sub.put("c", "C"), "Putting a key outside the exclusive upper bound must throw IllegalArgumentException");
    }

    /* ---------- Returned type verification ---------- */
    @Test
    void returnedTypeVerification() {
        // Confirm that subMap returns a wrapper of type ConcurrentNavigableMapNullSafe.
        ConcurrentNavigableMap<String, String> sub = map.subMap("a", true, "b", true);
        assertTrue(sub instanceof ConcurrentNavigableMapNullSafe, "subMap should return a ConcurrentNavigableMapNullSafe instance");
    }
}
