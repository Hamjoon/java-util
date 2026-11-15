package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_tailMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a deterministic ordering
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is considered the greatest key
        map.put(null, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        map.clear();
        map = null;
    }

    /**
     * Returns the keys of the given map as a List preserving the map's iteration order.
     */
    private static List<String> keysOf(ConcurrentNavigableMap<String, Integer> m) {
        List<String> list = new ArrayList<>(m.keySet());
        return list;
    }

    private static Stream<Arguments> basicInclusiveProvider() {
        return Stream.of(// fromKey = "b", inclusive = true  => ["b","c"]
        Arguments.of("b", true, List.of("b", "c")), // fromKey = "b", inclusive = false => ["c"]
        Arguments.of("b", false, List.of("c")), // fromKey = "d", inclusive = true  => [] (d is greater than any existing key)
        Arguments.of("d", true, List.of()));
    }

    private static Stream<Arguments> nullKeyProvider() {
        return Stream.of(// fromKey = null, inclusive = true  => ["a","b","c"]
        Arguments.of(null, true, List.of("a", "b", "c")), // fromKey = null, inclusive = false => ["a","b","c"]
        Arguments.of(null, false, List.of("a", "b", "c")));
    }

    /**
     * Sets the {@code internalMap} field of {@link AbstractConcurrentNullSafeMap}
     * (a superclass of {@link ConcurrentNavigableMapNullSafe}) via reflection.
     */
    private static void setInternalMap(ConcurrentNavigableMapNullSafe<?, ?> target, Object newMap) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(target, newMap);
    }

    @Test
    void tailMap_InclusiveTrue_ContainsFromKeyAndHigher() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        // Expected keys: "b", "c", null
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
        // Verify values are the same as in the original map
        assertEquals(2, tail.get("b"));
        assertEquals(3, tail.get("c"));
        assertEquals(100, tail.get(null));
    }

    @Test
    void tailMap_InclusiveFalse_ExcludesFromKey() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", false);
        // Expected keys: "c", null
        assertEquals(2, tail.size());
        assertFalse(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
        assertEquals(3, tail.get("c"));
        assertEquals(100, tail.get(null));
    }

    @Test
    void tailMap_FromNull_InclusiveTrue_ReturnsOnlyNullKey() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null, true);
        // Null is the greatest key; only null should be present
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
        assertEquals(100, tail.get(null));
    }

    @Test
    void tailMap_FromNull_InclusiveFalse_ReturnsEmptyMap() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null, false);
        // No key is greater than null, so the view must be empty
        assertTrue(tail.isEmpty());
    }

    @Test
    void tailMap_ViewIsLive_ChangesReflectedBothWays() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        // Add a new entry through the tail view
        tail.put("d", 4);
        assertEquals(4, map.get("d"));
        assertTrue(tail.containsKey("d"));
        // Remove an entry from the original map and ensure the view updates
        map.remove("c");
        assertFalse(tail.containsKey("c"));
        // b, null, d
        assertEquals(3, tail.size());
    }

    @Test
    void tailMap_ReturnedInstanceIsConcurrentNavigableMapNullSafe() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("a", true);
        assertTrue(tail instanceof ConcurrentNavigableMapNullSafe);
    }

    @ParameterizedTest(name = "tailMap(''{0}'', inclusive={1}) should contain {2}")
    @MethodSource("basicInclusiveProvider")
    void tailMap_BasicInclusiveBehavior(String fromKey, boolean inclusive, List<String> expectedKeys) {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(fromKey, inclusive);
        assertEquals(expectedKeys.size(), tail.size(), () -> "Expected size " + expectedKeys.size() + " but got " + tail.size());
        List<String> actualKeys = keysOf(tail);
        assertEquals(expectedKeys, actualKeys, () -> "Expected keys " + expectedKeys + " but got " + actualKeys);
    }

    @ParameterizedTest(name = "tailMap(null, inclusive={1}) should contain {2}")
    @MethodSource("nullKeyProvider")
    void tailMap_NullKeyHandling(String fromKey, boolean inclusive, List<String> expectedKeys) {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(fromKey, inclusive);
        // The null key itself is considered the greatest key; the sentinel used for masking
        // makes the tail view start from the first real key.
        List<String> actualKeys = keysOf(tail);
        assertEquals(expectedKeys, actualKeys);
        // Ensure the null entry is **not** part of the result as per the specification of this test case.
        assertFalse(actualKeys.contains(null));
    }

    @Test
    void tailMap_InternalMapIsNull_ThrowsNullPointerException() throws Exception {
        // Create a fresh instance and forcibly nullify its internalMap field.
        ConcurrentNavigableMapNullSafe<String, Integer> faulty = new ConcurrentNavigableMapNullSafe<>();
        setInternalMap(faulty, null);
        assertThrows(NullPointerException.class, () -> faulty.tailMap("any", true), "Calling tailMap when internalMap is null should throw NPE");
    }

    @Test
    void tailMap_InternalMapWrongType_ThrowsClassCastException() throws Exception {
        // Replace internalMap with a plain HashMap (does not implement ConcurrentNavigableMap)
        ConcurrentNavigableMapNullSafe<String, Integer> faulty = new ConcurrentNavigableMapNullSafe<>();
        setInternalMap(faulty, new HashMap<>());
        assertThrows(ClassCastException.class, () -> faulty.tailMap("a", false), "Calling tailMap when internalMap is not a ConcurrentNavigableMap should throw CCE");
    }

    @Test
    void tailMap_IncompatibleKeyType_ThrowsClassCastException() {
        // The map expects String keys; pass an Integer.
        assertThrows(ClassCastException.class, () -> map.tailMap((String) (Object) 123, true), "Using an incompatible key type should result in ClassCastException");
    }

    @Test
    void tailMap_NonComparableKeyWhenNoComparator_ThrowsClassCastException() {
        // The map uses natural ordering (String implements Comparable). Pass a non‑Comparable object.
        Object nonComparable = new Object();
        assertThrows(ClassCastException.class, () -> map.tailMap((String) nonComparable, false), "Non‑Comparable key with natural ordering should throw ClassCastException");
    }

    @Test
    void tailMap_CustomComparatorThrowsIllegalArgumentException() throws Exception {
        // Comparator that throws IllegalArgumentException for the key "bad".
        Comparator<String> explodingComparator = (k1, k2) -> {
            if ("bad".equals(k1) || "bad".equals(k2)) {
                throw new IllegalArgumentException("bad key not allowed");
            }
            return k1.compareTo(k2);
        };
        // Build a ConcurrentSkipListMap with the exploding comparator.
        ConcurrentSkipListMap<String, Integer> internal = new ConcurrentSkipListMap<>(explodingComparator);
        internal.put("a", 1);
        internal.put("bad", 99);
        internal.put("c", 3);
        // Use reflection to invoke the package‑private constructor that accepts the internal map and comparator.
        ConcurrentNavigableMapNullSafe<String, Integer> customMap = createInstanceWith(internal, explodingComparator);
        // The underlying tailMap call should propagate the IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> customMap.tailMap("bad", true), "Comparator throwing IllegalArgumentException should be propagated");
    }

    /**
     * Creates an instance of {@link ConcurrentNavigableMapNullSafe} using the
     * package‑private constructor that accepts an internal map and a comparator.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> ConcurrentNavigableMapNullSafe<K, V> createInstanceWith(ConcurrentNavigableMap<K, V> internal, Comparator<? super K> comparator) throws Exception {
        // Find the constructor with (ConcurrentNavigableMap, Comparator) signature.
        var ctor = ConcurrentNavigableMapNullSafe.class.getDeclaredConstructor(ConcurrentNavigableMap.class, Comparator.class);
        ctor.setAccessible(true);
        return (ConcurrentNavigableMapNullSafe<K, V>) ctor.newInstance(internal, comparator);
    }
}
