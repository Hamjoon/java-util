package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class SealableNavigableMap_subMap_2_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // a simple backing map – the implementation used by SealableNavigableMap
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        backingMap.put("c", 3);
        backingMap.put("d", 4);
        backingMap.put("e", 5);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /**
     * Returns a deterministic string representation of a map, e.g. {1=a, 2=b}.
     */
    private static String mapToString(NavigableMap<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> e = it.next();
            sb.append(e.getKey()).append("=").append(e.getValue());
            if (it.hasNext())
                sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    static Stream<Arguments> normalRangeMixedInclusivityProvider() {
        return Stream.of(Arguments.of("Sub‑map from 1 (incl) to 3 (excl) → {1=a, 2=b}", 1, true, 3, false, "{1=a, 2=b}"), Arguments.of("Sub‑map from 1 (excl) to 3 (incl) → {2=b, 3=c}", 1, false, 3, true, "{2=b, 3=c}"), Arguments.of("Sub‑map from 2 (incl) to 4 (incl) → {2=b, 3=c, 4=d}", 2, true, 4, true, "{2=b, 3=c, 4=d}"));
    }

    static Stream<Arguments> emptyResultExclusiveBoundsProvider() {
        return Stream.of(Arguments.of("Lower == upper, upper exclusive → {}", 5, true, 5, false, "{}"), Arguments.of("Both bounds exclusive on same key → {}", 2, false, 2, false, "{}"));
    }

    @Test
    void testSubMapRangeIsCorrect() {
        // subMap from "b" (inclusive) to "d" (exclusive) → should contain b and c
        NavigableMap<String, Integer> sub = sealableMap.subMap("b", true, "d", false);
        assertEquals(2, sub.size(), "SubMap should contain exactly two entries");
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
        assertFalse(sub.containsKey("d"));
        assertFalse(sub.containsKey("a"));
    }

    @Test
    void testSubMapMutationsReflectInParent() {
        NavigableMap<String, Integer> sub = sealableMap.subMap("b", true, "e", true);
        // put a new entry via subMap – key within range
        sub.put("bb", 22);
        assertEquals(Integer.valueOf(22), sealableMap.get("bb"));
        assertTrue(sealableMap.containsKey("bb"));
        // remove via subMap
        sub.remove("c");
        assertFalse(sealableMap.containsKey("c"));
    }

    @Test
    void testSubMapPutOutsideRangeThrows() {
        NavigableMap<String, Integer> sub = sealableMap.subMap("b", true, "d", false);
        // key "d" is exclusive, so putting it should fail
        assertThrows(IllegalArgumentException.class, () -> sub.put("d", 40));
        // key before lower bound also fails
        assertThrows(IllegalArgumentException.class, () -> sub.put("a", 10));
    }

    @Test
    void testSubMapWhenSealedIsImmutable() {
        NavigableMap<String, Integer> sub = sealableMap.subMap("b", true, "e", true);
        // seal the map
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed now");
        // any mutation through the sub‑map must be rejected
        assertThrows(UnsupportedOperationException.class, () -> sub.put("bb", 99));
        assertThrows(UnsupportedOperationException.class, () -> sub.remove("c"));
        assertThrows(UnsupportedOperationException.class, sub::clear);
    }

    @Test
    void testSubMapReflectsParentChanges() {
        NavigableMap<String, Integer> sub = sealableMap.subMap("b", true, "e", true);
        // add a key that falls inside the sub‑map range directly on the parent
        sealableMap.put("bd", 42);
        assertTrue(sub.containsKey("bd"));
        // b, c, bd, e
        assertEquals(4, sub.size());
        // remove a key via parent and verify sub‑map no longer sees it
        sealableMap.remove("c");
        assertFalse(sub.containsKey("c"));
        assertEquals(3, sub.size());
    }

    @Test
    void testSubMapWithNullKeyWhenAllowed() {
        // The underlying map permits a null key; test that subMap behaves correctly
        sealableMap.put(null, 0);
        // subMap that starts at null (inclusive) and ends at "b" (exclusive)
        NavigableMap<String, Integer> sub = sealableMap.subMap(null, true, "b", false);
        assertTrue(sub.containsKey(null));
        assertFalse(sub.containsKey("b"));
        assertEquals(1, sub.size());
    }

    // ---------- Normal range – mixed inclusivity ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalRangeMixedInclusivityProvider")
    void testNormalRangeMixedInclusivity(String description, Integer fromKey, boolean fromInc, Integer toKey, boolean toInc, String expected) {
        NavigableMap<Integer, String> sub = sealableMap.subMap(fromKey, fromInc, toKey, toInc);
        assertEquals(expected, mapToString(sub), description);
    }

    // ---------- Empty result – exclusive bounds ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("emptyResultExclusiveBoundsProvider")
    void testEmptyResultExclusiveBounds(String description, Integer fromKey, boolean fromInc, Integer toKey, boolean toInc, String expected) {
        NavigableMap<Integer, String> sub = sealableMap.subMap(fromKey, fromInc, toKey, toInc);
        assertEquals(expected, mapToString(sub), description);
    }

    // ---------- Single‑element range – both inclusive ----------
    @Test
    void testSingleElementBothInclusive() {
        NavigableMap<Integer, String> sub = sealableMap.subMap(3, true, 3, true);
        assertEquals("{3=c}", mapToString(sub), "When both bounds are inclusive and equal, the sub‑map contains exactly that entry.");
    }

    // ---------- Invalid range – fromKey greater than toKey ----------
    @Test
    void testInvalidRangeFromGreaterThanTo() {
        assertThrows(IllegalArgumentException.class, () -> sealableMap.subMap(4, true, 2, true), "Underlying map should throw IllegalArgumentException for fromKey > toKey");
    }

    // ---------- Null key handling ----------
    @Test
    void testNullFromKeyThrows() {
        // TreeMap does not allow null keys, so a NPE is expected
        assertThrows(NullPointerException.class, () -> sealableMap.subMap(null, true, 2, true));
    }

    @Test
    void testNullToKeyThrows() {
        assertThrows(NullPointerException.class, () -> sealableMap.subMap(1, true, null, true));
    }

    // ---------- Result type verification ----------
    @Test
    void testResultIsSealableNavigableMap() {
        NavigableMap<Integer, String> sub = sealableMap.subMap(1, true, 3, true);
        assertTrue(sub instanceof SealableNavigableMap, "subMap must return a SealableNavigableMap wrapper, not the raw delegate.");
    }

    // ---------- Both bounds exclusive – normal range ----------
    @Test
    void testBothBoundsExclusiveNormalRange() {
        NavigableMap<Integer, String> sub = sealableMap.subMap(1, false, 4, false);
        assertEquals("{2=b, 3=c}", mapToString(sub), "Both bounds exclusive; should contain keys strictly between 1 and 4.");
    }

    // ---------- Both bounds exclusive – same key yields empty ----------
    @Test
    void testBothBoundsExclusiveSameKeyEmpty() {
        NavigableMap<Integer, String> sub = sealableMap.subMap(2, false, 2, false);
        assertEquals("{}", mapToString(sub), "Same key with both exclusive bounds must produce an empty sub‑map.");
    }

    // ---------- Lower bound before first key ----------
    @Test
    void testLowerBoundBeforeFirstKey() {
        NavigableMap<Integer, String> sub = sealableMap.subMap(0, true, 2, true);
        assertEquals("{1=a, 2=b}", mapToString(sub), "Lower bound smaller than any key should start from the first entry.");
    }

    // ---------- Upper bound beyond last key ----------
    @Test
    void testUpperBoundBeyondLastKey() {
        NavigableMap<Integer, String> sub = sealableMap.subMap(3, true, 10, false);
        assertEquals("{3=c, 4=d, 5=e}", mapToString(sub), "Upper bound exceeding the greatest key should include all remaining entries.");
    }

    // ---------- Sub‑map mutability – updates affect original ----------
    @Test
    void testSubMapMutabilityReflectsInParent() {
        NavigableMap<Integer, String> sub = sealableMap.subMap(2, true, 4, true);
        sub.put(3, "z");
        assertEquals("z", sealableMap.get(3), "Modification through the sub‑map must be visible in the parent map.");
    }

    // ---------- Custom comparator – reverse order subMap ----------
    @Test
    void testCustomComparatorReverseOrderSubMap() {
        // Create a descending map as the backing map
        NavigableMap<Integer, String> descending = new TreeMap<>(Comparator.reverseOrder());
        // keys 5..1 in reverse order
        descending.putAll(backingMap);
        SealableNavigableMap<Integer, String> reverseSealable = new SealableNavigableMap<>(descending, sealedSupplier);
        // fromKey=4 (exclusive) toKey=2 (exclusive) in descending order → only key 3 remains
        NavigableMap<Integer, String> sub = reverseSealable.subMap(4, false, 2, false);
        assertEquals("{3=c}", mapToString(sub), "Reverse comparator should respect logical order when computing sub‑map.");
    }

    // ---------- Key type mismatch – ClassCastException ----------
    @Test
    void testKeyTypeMismatchFromKey() {
        // Map expects Integer keys; passing a String should cause ClassCastException
        assertThrows(ClassCastException.class, () -> sealableMap.subMap((Integer) (Object) "stringKey", true, 3, true));
    }

    @Test
    void testKeyTypeMismatchToKey() {
        assertThrows(ClassCastException.class, () -> sealableMap.subMap(1, true, (Integer) (Object) "stringKey", true));
    }

    @Test
    void testKeyTypeMismatchBoth() {
        assertThrows(ClassCastException.class, () -> sealableMap.subMap((Integer) (Object) "stringKey", false, (Integer) (Object) "another", false));
    }

    // ---------- Invalid range – reverse comparator ----------
    @Test
    void testInvalidRangeWithReverseComparator() {
        // Build a descending map where logical order is reversed
        NavigableMap<Integer, String> descending = new TreeMap<>(Comparator.reverseOrder());
        descending.putAll(backingMap);
        SealableNavigableMap<Integer, String> reverseSealable = new SealableNavigableMap<>(descending, sealedSupplier);
        // According to the descending comparator, 2 > 4, so this is an illegal range
        assertThrows(IllegalArgumentException.class, () -> reverseSealable.subMap(2, true, 4, true));
    }
}
