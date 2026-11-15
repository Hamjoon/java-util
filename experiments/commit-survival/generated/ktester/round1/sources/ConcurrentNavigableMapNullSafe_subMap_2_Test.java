package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_subMap_2_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is considered the greatest key
        map.put(null, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.clear();
    }

    /**
     * Private helper that creates a {@link ConcurrentNavigableMapNullSafe} with a
     * supplied comparator.  The public API of the class does not expose such a
     * constructor, but the underlying implementation extends {@link java.util.concurrent.ConcurrentSkipListMap},
     * which accepts a comparator.  We use reflection to invoke the protected
     * constructor that takes a {@code Comparator}.
     */
    private static <K, V> ConcurrentNavigableMapNullSafe<K, V> newMapWithComparator(Comparator<? super K> comparator) {
        try {
            // Find the protected constructor ConcurrentNavigableMapNullSafe(Comparator)
            java.lang.reflect.Constructor<ConcurrentNavigableMapNullSafe> ctor = ConcurrentNavigableMapNullSafe.class.getDeclaredConstructor(Comparator.class);
            ctor.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentNavigableMapNullSafe<K, V> map = ctor.newInstance(comparator);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate map with comparator via reflection", e);
        }
    }

    @Test
    void testSubMapInclusiveExclusive() {
        // subMap(K fromKey, K toKey) is defined as fromKey inclusive, toKey exclusive
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", "c");
        // Expected keys: "a" and "b"
        assertEquals(2, sub.size(), "Sub‑map should contain exactly two entries");
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
        assertFalse(sub.containsKey(null));
        assertEquals(1, sub.get("a"));
        assertEquals(2, sub.get("b"));
    }

    @Test
    void testSubMapReflectsChangesInParent() {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", "c");
        // Modify through the sub‑map
        sub.put("b", 20);
        assertEquals(20, sub.get("b"));
        // The change must be visible in the original map
        assertEquals(20, map.get("b"));
        // Remove through the sub‑map
        sub.remove("a");
        assertNull(sub.get("a"));
        assertNull(map.get("a"));
    }

    @Test
    void testSubMapWithNullUpperBound() {
        // toKey = null (the greatest key) is exclusive, so null itself is not included
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("b", null);
        // Expected keys: "b" and "c"
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
        assertFalse(sub.containsKey(null));
        assertFalse(sub.containsKey("a"));
        assertEquals(2, sub.get("b"));
        assertEquals(3, sub.get("c"));
    }

    @Test
    void testSubMapWithNullLowerBound() {
        // fromKey = null is the greatest key, so the range is empty because fromKey > any other key
        assertThrows(IllegalArgumentException.class, () -> map.subMap(null, "a"), "subMap(null, \"a\") should throw IllegalArgumentException because null is considered the last key");
    }

    @Test
    void testSubMapInvalidRangeThrows() {
        // fromKey > toKey should raise IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> map.subMap("c", "a"), "subMap with fromKey greater than toKey must throw IllegalArgumentException");
    }

    @Test
    void testSubMapIsConcurrentNavigableMap() {
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", "c");
        assertTrue(sub instanceof NavigableMap, "Result should be a NavigableMap");
        assertTrue(sub instanceof ConcurrentNavigableMap, "Result should be a ConcurrentNavigableMap");
    }

    /**
     * Normal range where fromKey < toKey.
     * Expected map contains keys 2 and 3 (inclusive lower, exclusive upper).
     */
    @Test
    void testSubMapNormalRange_IntegerKeys() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        intMap.put(2, "b");
        intMap.put(3, "c");
        intMap.put(4, "d");
        ConcurrentNavigableMap<Integer, String> sub = intMap.subMap(2, 4);
        assertEquals(2, sub.size(), "Sub‑map should contain exactly two entries");
        assertEquals("b", sub.get(2));
        assertEquals("c", sub.get(3));
        assertFalse(sub.containsKey(4));
    }

    /**
     * Edge case where the range collapses to an empty view (fromKey == toKey).
     */
    @Test
    void testSubMapEmptyWhenBoundsEqual() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        intMap.put(2, "b");
        intMap.put(3, "c");
        ConcurrentNavigableMap<Integer, String> sub = intMap.subMap(3, 3);
        assertTrue(sub.isEmpty(), "Sub‑map should be empty when fromKey equals toKey");
        assertEquals(0, sub.size());
    }

    /**
     * fromKey > toKey must throw IllegalArgumentException.
     */
    @Test
    void testSubMapFromKeyGreaterThanToKey() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        intMap.put(2, "b");
        intMap.put(3, "c");
        assertThrows(IllegalArgumentException.class, () -> intMap.subMap(4, 2), "subMap with fromKey greater than toKey must throw IllegalArgumentException");
    }

    /**
     * Null lower bound – because {@code null} is treated as the greatest key,
     * the call is equivalent to {@code fromKey > toKey} and therefore throws
     * {@code IllegalArgumentException}.
     */
    @Test
    void testSubMapNullLowerBoundThrows() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        intMap.put(2, "b");
        assertThrows(IllegalArgumentException.class, () -> intMap.subMap(null, 2), "Using null as the lower bound should be illegal because null is the greatest key");
    }

    /**
     * Null upper bound – {@code null} is the greatest key, but the upper bound
     * is exclusive, so the call is valid and simply returns a view that does not
     * contain the sentinel null key.
     */
    @Test
    void testSubMapNullUpperBoundExclusive() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        intMap.put(2, "b");
        intMap.put(3, "c");
        // sentinel for null key
        intMap.put(null, "nullValue");
        ConcurrentNavigableMap<Integer, String> sub = intMap.subMap(2, null);
        // Expected keys: 2 and 3 (null is exclusive)
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(2));
        assertTrue(sub.containsKey(3));
        assertFalse(sub.containsKey(null));
    }

    /**
     * Verify that the returned sub‑map is a live view: inserting a new entry
     * that lies on the exclusive upper bound does **not** make it appear in the view.
     */
    @Test
    void testSubMapLiveViewExclusiveUpperBound() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(2, "b");
        intMap.put(3, "c");
        intMap.put(4, "d");
        ConcurrentNavigableMap<Integer, String> sub = intMap.subMap(2, 5);
        // Insert a new key that equals the exclusive upper bound
        intMap.put(5, "e");
        assertFalse(sub.containsKey(5), "Key equal to exclusive upper bound must not appear in sub‑map");
        assertEquals(3, sub.size(), "Sub‑map should still contain only keys 2,3,4");
    }

    /**
     * Verify that modifications performed via the sub‑map are reflected in the
     * backing map (live writable view).
     */
    @Test
    void testSubMapLiveViewWritable() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        intMap.put(3, "c");
        intMap.put(4, "d");
        ConcurrentNavigableMap<Integer, String> sub = intMap.subMap(1, 4);
        // Write through the sub‑map
        sub.put(2, "b");
        assertEquals("b", sub.get(2));
        assertEquals("b", intMap.get(2), "Change via sub‑map must be visible in the original map");
    }

    /**
     * When the backing map is empty, any sub‑map request must return an empty view.
     */
    @Test
    void testSubMapOnEmptyBackingMap() {
        ConcurrentNavigableMapNullSafe<Integer, String> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<Integer, String> sub = emptyMap.subMap(0, 10);
        assertTrue(sub.isEmpty(), "Sub‑map of an empty backing map should be empty");
        assertEquals(0, sub.size());
    }

    /**
     * Custom case‑insensitive comparator: keys are ordered without regard to case.
     * The sub‑map from "b" (inclusive) to "d" (exclusive) must contain "B" and "c".
     */
    @Test
    void testSubMapWithCaseInsensitiveComparator() {
        // Build a map with a case‑insensitive ordering
        ConcurrentNavigableMapNullSafe<String, String> ciMap = new ConcurrentNavigableMapNullSafe<>(String.CASE_INSENSITIVE_ORDER);
        ciMap.put("A", "alpha");
        ciMap.put("B", "beta");
        ciMap.put("c", "gamma");
        ciMap.put("d", "delta");
        ConcurrentNavigableMap<String, String> sub = ciMap.subMap("b", "d");
        assertEquals(2, sub.size(), "Sub‑map should contain two entries");
        // Keys are stored with original case; the view must contain the original entries
        assertTrue(sub.containsKey("B"));
        assertTrue(sub.containsKey("c"));
        assertEquals("beta", sub.get("B"));
        assertEquals("gamma", sub.get("c"));
    }

    /**
     * Mismatched key type: the underlying map stores {@code Integer} keys,
     * but the caller supplies {@code String} keys.  This should result in a
     * {@code ClassCastException} at runtime.
     */
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void testSubMapMismatchedKeyTypeThrowsClassCastException() {
        // Use raw types to bypass compile‑time generic checks
        ConcurrentNavigableMapNullSafe rawMap = new ConcurrentNavigableMapNullSafe();
        rawMap.put(1, 100);
        rawMap.put(2, 200);
        Executable call = () -> rawMap.subMap("a", "b");
        assertThrows(ClassCastException.class, call, "Supplying keys of the wrong type must cause a ClassCastException");
    }
}
