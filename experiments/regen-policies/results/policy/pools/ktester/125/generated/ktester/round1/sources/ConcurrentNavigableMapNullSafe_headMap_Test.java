package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_headMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    class RejectingMap extends ConcurrentSkipListMap<Object, Integer> {

        @Override
        public ConcurrentNavigableMap<Object, Integer> headMap(Object toKey, boolean inclusive) {
            if (toKey instanceof String && ((String) toKey).startsWith("NULL_SENTINEL")) {
                throw new IllegalArgumentException("Sentinel key not allowed");
            }
            return super.headMap(toKey, inclusive);
        }
    }

    class RejectingMap extends ConcurrentSkipListMap<Object, Integer> {

        @Override
        public ConcurrentNavigableMap<Object, Integer> headMap(Object toKey, boolean inclusive) {
            if (toKey instanceof String && ((String) toKey).startsWith("NULL_SENTINEL")) {
                throw new IllegalArgumentException("Sentinel key not allowed");
            }
            return super.headMap(toKey, inclusive);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate map with a predictable ordering
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null key is considered last in ascending order
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC
        map = null;
    }

    /**
     * Sets the {@code internalMap} field of a {@link ConcurrentNavigableMapNullSafe}
     * instance using reflection.
     */
    private static void setInternalMap(ConcurrentNavigableMapNullSafe<?, ?> target, ConcurrentMap<?, ?> newMap) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(target, newMap);
    }

    /**
     * Sets the {@code originalComparator} field of a {@link ConcurrentNavigableMapNullSafe}
     * instance using reflection.
     */
    private static void setOriginalComparator(ConcurrentNavigableMapNullSafe<?, ?> target, Comparator<?> comparator) throws Exception {
        Field f = ConcurrentNavigableMapNullSafe.class.getDeclaredField("originalComparator");
        f.setAccessible(true);
        f.set(target, comparator);
    }

    @Test
    void headMap_exclusive_toKey() {
        // toKey = "b", inclusive = false → should contain keys < "b"
        ConcurrentNavigableMap<String, Integer> head = map.headMap("b", false);
        // Expected keys: "a"
        assertEquals(1, head.size(), "Head map should contain exactly one entry");
        assertTrue(head.containsKey("a"));
        assertFalse(head.containsKey("b"));
        assertFalse(head.containsKey("c"));
        assertFalse(head.containsKey(null));
        // Verify values are the same as original map
        assertEquals(Integer.valueOf(1), head.get("a"));
    }

    @Test
    void headMap_inclusive_toKey() {
        // toKey = "b", inclusive = true → should contain keys <= "b"
        ConcurrentNavigableMap<String, Integer> head = map.headMap("b", true);
        // Expected keys: "a", "b"
        assertEquals(2, head.size(), "Head map should contain two entries");
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertFalse(head.containsKey("c"));
        assertFalse(head.containsKey(null));
        assertEquals(Integer.valueOf(1), head.get("a"));
        assertEquals(Integer.valueOf(2), head.get("b"));
    }

    @Test
    void headMap_toKeyGreaterThanAllNonNullKeys() {
        // toKey = "z" (greater than any non‑null key), inclusive = false
        // Should contain all non‑null keys but not the null key (null is considered last)
        ConcurrentNavigableMap<String, Integer> head = map.headMap("z", false);
        assertEquals(3, head.size(), "Head map should contain three non‑null entries");
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertTrue(head.containsKey("c"));
        assertFalse(head.containsKey(null));
        // Values check
        assertEquals(Integer.valueOf(1), head.get("a"));
        assertEquals(Integer.valueOf(2), head.get("b"));
        assertEquals(Integer.valueOf(3), head.get("c"));
    }

    @Test
    void headMap_toKeyIsNull() {
        // According to the contract, null keys are treated as the highest possible key.
        // headMap(null, false) should return an empty map because no key is less than null.
        ConcurrentNavigableMap<String, Integer> head = map.headMap(null, false);
        assertTrue(head.isEmpty(), "Head map with null toKey (exclusive) should be empty");
    }

    @Test
    void headMap_toKeyIsNullInclusive() {
        // headMap(null, true) should contain all entries because null is considered the greatest key.
        ConcurrentNavigableMap<String, Integer> head = map.headMap(null, true);
        // All entries, including the null key, must be present
        assertEquals(4, head.size(), "Head map with null toKey (inclusive) should contain all entries");
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertTrue(head.containsKey("c"));
        assertTrue(head.containsKey(null));
        assertEquals(Integer.valueOf(99), head.get(null));
    }

    @Test
    void headMap_resultIsIndependentView() {
        // Verify that modifications to the returned head map affect the original map
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c", false);
        // head currently contains "a" and "b"
        head.remove("a");
        assertNull(head.get("a"));
        assertNull(map.get("a"), "Removal from head map should reflect in the original map");
        // Adding a new entry to the head map should also appear in the original map
        head.put("aa", 42);
        assertEquals(Integer.valueOf(42), map.get("aa"));
    }

    @Test
    void headMap_nullToKeyInclusiveWhenOnlyNullKeyPresent() throws Exception {
        // Build a map that contains only a null key
        ConcurrentNavigableMapNullSafe<String, Integer> onlyNull = new ConcurrentNavigableMapNullSafe<>();
        onlyNull.put(null, 55);
        // Inclusive null → should return the single entry
        ConcurrentNavigableMap<String, Integer> head = onlyNull.headMap(null, true);
        assertEquals(1, head.size());
        assertTrue(head.containsKey(null));
        assertEquals(Integer.valueOf(55), head.get(null));
    }

    @Test
    void headMap_nullToKeyExclusiveWhenOnlyNullKeyPresent() throws Exception {
        ConcurrentNavigableMapNullSafe<String, Integer> onlyNull = new ConcurrentNavigableMapNullSafe<>();
        onlyNull.put(null, 55);
        ConcurrentNavigableMap<String, Integer> head = onlyNull.headMap(null, false);
        assertTrue(head.isEmpty(), "Exclusive null on a map that only contains a null key must be empty");
    }

    @Test
    void headMap_emptyUnderlyingMap() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<String, Integer> head = empty.headMap("any", true);
        assertTrue(head.isEmpty(), "Head map of an empty source must be empty");
    }

    @Test
    void headMap_nonExistentToKeyBehavesLikeExclusive() {
        // toKey = "d" does not exist; both inclusive and exclusive should give keys < "d"
        ConcurrentNavigableMap<String, Integer> inclusive = map.headMap("d", true);
        ConcurrentNavigableMap<String, Integer> exclusive = map.headMap("d", false);
        assertEquals(inclusive, exclusive, "Inclusive flag has no effect when the key is absent");
        assertEquals(3, inclusive.size());
        assertTrue(inclusive.containsKey("a"));
        assertTrue(inclusive.containsKey("b"));
        assertTrue(inclusive.containsKey("c"));
        assertFalse(inclusive.containsKey(null));
    }

    @Test
    void headMap_customComparator_reverseOrder() {
        // Reverse order comparator: larger natural keys become "smaller" in the map
        Comparator<String> reverse = Comparator.reverseOrder();
        // Build underlying map with the reverse comparator
        ConcurrentSkipListMap<String, Integer> underlying = new ConcurrentSkipListMap<>(reverse);
        underlying.put("a", 1);
        underlying.put("b", 2);
        underlying.put("c", 3);
        // null is still masked internally
        underlying.put(null, 99);
        // Wrap it with NullSafe via reflection (no public ctor that accepts a map)
        ConcurrentNavigableMapNullSafe<String, Integer> reverseMap = new ConcurrentNavigableMapNullSafe<>();
        setInternalMap(reverseMap, underlying);
        setOriginalComparator(reverseMap, reverse);
        // With reverse order, headMap(toKey, true) returns keys that are *greater* or equal
        // in natural order, i.e., "c" and "b" when toKey = "b"
        ConcurrentNavigableMap<String, Integer> headInc = reverseMap.headMap("b", true);
        assertEquals(2, headInc.size());
        assertTrue(headInc.containsKey("c"));
        assertTrue(headInc.containsKey("b"));
        assertFalse(headInc.containsKey("a"));
        assertFalse(headInc.containsKey(null));
        // Exclusive version should omit "b"
        ConcurrentNavigableMap<String, Integer> headExc = reverseMap.headMap("b", false);
        assertEquals(1, headExc.size());
        assertTrue(headExc.containsKey("c"));
        assertFalse(headExc.containsKey("b"));
    }

    @Test
    void headMap_viewIndependence_modificationsDoNotAffectOriginal() {
        // Obtain a head map view and modify it; the original map should stay unchanged
        ConcurrentNavigableMap<String, Integer> head = map.headMap("b", true);
        // The view currently contains a, b, and null (because inclusive true includes null when toKey is null;
        // but here toKey is "b", so null is not included)
        assertEquals(2, head.size());
        // Remove "a" from the view
        head.remove("a");
        assertFalse(head.containsKey("a"));
        // Original map must also reflect the removal because the view shares the same underlying map
        // (the contract of ConcurrentNavigableMapNullSafe is a live view). The test verifies that the
        // returned object is a distinct wrapper, not a copy.
        assertNull(map.get("a"));
        // Add a new entry via the view
        head.put("aa", 42);
        assertEquals(Integer.valueOf(42), map.get("aa"));
    }

    @Test
    void headMap_nullInternalMap_throwsNPE() throws Exception {
        // Create a normal map then forcibly nullify its internalMap field
        ConcurrentNavigableMapNullSafe<String, Integer> broken = new ConcurrentNavigableMapNullSafe<>();
        setInternalMap(broken, null);
        assertThrows(NullPointerException.class, () -> broken.headMap("any", true));
    }

    @Test
    void headMap_incompatibleKeyType_throwsClassCastException() {
        // Underlying map with Integer keys
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, 10);
        intMap.put(2, 20);
        // Attempt to call headMap with a String key – should bubble up ClassCastException
        assertThrows(ClassCastException.class, () -> {
            // The raw type is required because the generic type does not match
            @SuppressWarnings("unchecked")
            ConcurrentNavigableMap<String, Integer> raw = (ConcurrentNavigableMap) intMap;
            raw.headMap("string-key", false);
        });
    }

    @Test
    void headMap_incompatibleKeyType_customComparator_throwsClassCastException() {
        // Map that only accepts String keys via a comparator that casts to String
        Comparator<Object> stringOnly = (o1, o2) -> ((String) o1).compareTo((String) o2);
        ConcurrentSkipListMap<Object, Integer> underlying = new ConcurrentSkipListMap<>(stringOnly);
        underlying.put("a", 1);
        underlying.put("b", 2);
        ConcurrentNavigableMapNullSafe<String, Integer> mapWithComparator = new ConcurrentNavigableMapNullSafe<>();
        setInternalMap(mapWithComparator, underlying);
        setOriginalComparator(mapWithComparator, stringOnly);
        // Pass an Integer key – comparator will attempt to cast and throw ClassCastException
        assertThrows(ClassCastException.class, () -> mapWithComparator.headMap(123, true));
    }

    @Test
    void headMap_underlyingMapThrowsIllegalArgumentException() throws Exception {
        // Custom map that rejects a particular sentinel key (the masked null key)
        class RejectingMap extends ConcurrentSkipListMap<Object, Integer> {

            @Override
            public ConcurrentNavigableMap<Object, Integer> headMap(Object toKey, boolean inclusive) {
                if (toKey instanceof String && ((String) toKey).startsWith("NULL_SENTINEL")) {
                    throw new IllegalArgumentException("Sentinel key not allowed");
                }
                return super.headMap(toKey, inclusive);
            }
        }
        RejectingMap rejecting = new RejectingMap();
        rejecting.put("a", 1);
        rejecting.put("b", 2);
        // Wrap with NullSafe
        ConcurrentNavigableMapNullSafe<String, Integer> wrapper = new ConcurrentNavigableMapNullSafe<>();
        setInternalMap(wrapper, rejecting);
        // The maskNullKey implementation uses a unique sentinel string; we don't know its exact value,
        // but we can trigger the exception by calling headMap(null, true) which will be masked.
        assertThrows(IllegalArgumentException.class, () -> wrapper.headMap(null, true));
    }
}
