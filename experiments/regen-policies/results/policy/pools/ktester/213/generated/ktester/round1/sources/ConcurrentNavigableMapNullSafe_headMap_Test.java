package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_headMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    private ConcurrentNavigableMapNullSafe<Integer, String> intMap;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a deterministic set of keys.
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null key – should be treated as the greatest key.
        map.put(null, 100);
        intMap = new ConcurrentNavigableMapNullSafe<>();
        // deterministic set of odd integer keys
        intMap.put(1, "one");
        intMap.put(3, "three");
        intMap.put(5, "five");
        intMap.put(7, "seven");
        intMap.put(9, "nine");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
        intMap.clear();
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for numeric keys (inclusive / exclusive)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> numericHeadMapProvider() {
        return Stream.of(// group: headMap with non‑null toKey, inclusive = true
        Arguments.of(5, true, List.of(1, 3, 5), "inclusive true, toKey present"), Arguments.of(0, true, List.of(), "inclusive true, toKey smaller than min"), // group: headMap with non‑null toKey, inclusive = false
        Arguments.of(5, false, List.of(1, 3), "exclusive false, toKey present"), Arguments.of(6, false, List.of(1, 3, 5), "exclusive false, toKey absent"), Arguments.of(0, false, List.of(), "exclusive false, toKey smaller than min"), // group: headMap with toKey greater than maximum key
        Arguments.of(10, true, List.of(1, 3, 5, 7, 9), "inclusive true, toKey > max"), Arguments.of(10, false, List.of(1, 3, 5, 7, 9), "exclusive false, toKey > max"));
    }

    @Test
    void headMap_exclusiveBound_containsOnlyLowerKeys() {
        ConcurrentNavigableMap<String, Integer> head = map.headMap("b", false);
        // Verify type – the implementation returns a ConcurrentNavigableMapNullSafe.
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe, "headMap should return a ConcurrentNavigableMapNullSafe instance");
        // Expected content: only "a"
        assertEquals(1, head.size());
        assertEquals(1, head.get("a"));
        assertNull(head.get("b"));
        assertNull(head.get("c"));
        assertNull(head.get(null));
        // Original map must stay unchanged.
        assertEquals(4, map.size());
    }

    @Test
    void headMap_inclusiveBound_containsToKey() {
        ConcurrentNavigableMap<String, Integer> head = map.headMap("b", true);
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe);
        // Expected content: "a" and "b"
        assertEquals(2, head.size());
        assertEquals(1, head.get("a"));
        assertEquals(2, head.get("b"));
        assertNull(head.get("c"));
        assertNull(head.get(null));
    }

    @Test
    void headMap_toNull_exclusiveExcludesNullKey() {
        // Null is considered the greatest key; exclusive should give all non‑null keys.
        ConcurrentNavigableMap<String, Integer> head = map.headMap(null, false);
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe);
        assertEquals(3, head.size());
        assertEquals(1, head.get("a"));
        assertEquals(2, head.get("b"));
        assertEquals(3, head.get("c"));
        assertNull(head.get(null));
    }

    @Test
    void headMap_toNull_inclusiveIncludesNullKey() {
        // Inclusive should also contain the null key.
        ConcurrentNavigableMap<String, Integer> head = map.headMap(null, true);
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe);
        assertEquals(4, head.size());
        assertEquals(1, head.get("a"));
        assertEquals(2, head.get("b"));
        assertEquals(3, head.get("c"));
        assertEquals(100, head.get(null));
    }

    @Test
    void headMap_isBackedByOriginalMap_mutationsPropagate() {
        // should contain "a" and "b"
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c", false);
        // Add a new entry that belongs to the view.
        head.put("bb", 22);
        assertEquals(22, map.get("bb"));
        assertTrue(head.containsKey("bb"));
        // Remove an entry via the view.
        Integer removed = head.remove("a");
        assertEquals(1, removed);
        assertNull(map.get("a"));
        assertFalse(head.containsKey("a"));
        // Ensure that changes outside the view do not appear inside it.
        map.put("d", 4);
        assertNull(head.get("d"));
    }

    @Test
    void headMap_onEmptyMapReturnsEmpty() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<String, Integer> head = empty.headMap("any", false);
        assertTrue(head.isEmpty());
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("numericHeadMapProvider")
    void headMap_numericKeys_returnsExpectedKeys(Integer toKey, boolean inclusive, List<Integer> expectedKeys, String description) {
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(toKey, inclusive);
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe, "Returned map should be a ConcurrentNavigableMapNullSafe");
        // Verify size and content
        assertEquals(expectedKeys.size(), head.size(), "Size mismatch for toKey=" + toKey + " inclusive=" + inclusive);
        for (Integer k : expectedKeys) {
            assertTrue(head.containsKey(k), "Expected key " + k + " missing for toKey=" + toKey + " inclusive=" + inclusive);
        }
        // Ensure no unexpected keys are present
        Set<Integer> unexpected = new HashSet<>(intMap.keySet());
        unexpected.removeAll(expectedKeys);
        for (Integer k : unexpected) {
            assertFalse(head.containsKey(k), "Unexpected key " + k + " present for toKey=" + toKey + " inclusive=" + inclusive);
        }
    }

    /* -------------------------------------------------------------
     *  Null toKey handling (null is considered the greatest key)
     * ------------------------------------------------------------- */
    @Test
    void headMap_nullToKey_exclusiveExcludesNullKey() {
        // map already contains a null key from the original test class scenario
        ConcurrentNavigableMapNullSafe<String, Integer> mapWithNull = new ConcurrentNavigableMapNullSafe<>();
        mapWithNull.put("a", 1);
        mapWithNull.put(null, 100);
        ConcurrentNavigableMap<String, Integer> head = mapWithNull.headMap(null, false);
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe);
        assertEquals(1, head.size());
        assertEquals(1, head.get("a"));
        assertNull(head.get(null));
    }

    @Test
    void headMap_nullToKey_inclusiveIncludesNullKey() {
        ConcurrentNavigableMapNullSafe<String, Integer> mapWithNull = new ConcurrentNavigableMapNullSafe<>();
        mapWithNull.put("a", 1);
        mapWithNull.put(null, 100);
        ConcurrentNavigableMap<String, Integer> head = mapWithNull.headMap(null, true);
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe);
        assertEquals(2, head.size());
        assertEquals(1, head.get("a"));
        assertEquals(100, head.get(null));
    }

    /* -------------------------------------------------------------
     *  Empty map cases
     * ------------------------------------------------------------- */
    @Test
    void headMap_onEmptyMap_returnsEmpty() {
        ConcurrentNavigableMapNullSafe<Integer, String> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<Integer, String> head = empty.headMap(5, true);
        assertTrue(head.isEmpty());
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe);
    }

    @Test
    void headMap_onEmptyMap_withNullKey_returnsEmpty() {
        ConcurrentNavigableMapNullSafe<Integer, String> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<Integer, String> head = empty.headMap(null, true);
        assertTrue(head.isEmpty());
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe);
    }

    /* -------------------------------------------------------------
     *  Map that contains only a null key
     * ------------------------------------------------------------- */
    @Test
    void headMap_onlyNullKey_inclusiveReturnsNullKey() {
        ConcurrentNavigableMapNullSafe<String, Integer> onlyNull = new ConcurrentNavigableMapNullSafe<>();
        onlyNull.put(null, 42);
        ConcurrentNavigableMap<String, Integer> head = onlyNull.headMap(null, true);
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe);
        assertEquals(1, head.size());
        assertEquals(42, head.get(null));
    }

    @Test
    void headMap_onlyNullKey_exclusiveReturnsEmpty() {
        ConcurrentNavigableMapNullSafe<String, Integer> onlyNull = new ConcurrentNavigableMapNullSafe<>();
        onlyNull.put(null, 42);
        ConcurrentNavigableMap<String, Integer> head = onlyNull.headMap(null, false);
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe);
        assertTrue(head.isEmpty());
    }

    /* -------------------------------------------------------------
     *  Incompatible key type – expect ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void headMap_incompatibleKeyType_throwsClassCastException_inclusive() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(1, "one");
        map.put(2, "two");
        assertThrows(ClassCastException.class, () -> map.headMap((Integer) (Object) "string-key", true), "Passing a String as toKey to an Integer‑keyed map should throw ClassCastException");
    }

    @Test
    void headMap_incompatibleKeyType_throwsClassCastException_exclusive() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(1, "one");
        map.put(2, "two");
        assertThrows(ClassCastException.class, () -> map.headMap((Integer) (Object) "anotherString", false), "Passing a String as toKey to an Integer‑keyed map should throw ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Internal map is null – simulate via reflection
     * ------------------------------------------------------------- */
    @Test
    void headMap_nullInternalMap_throwsNullPointerException() throws Exception {
        // Create an instance using the default constructor
        ConcurrentNavigableMapNullSafe<Integer, String> broken = new ConcurrentNavigableMapNullSafe<>();
        // Use reflection to set the protected field 'internalMap' (declared in AbstractConcurrentNullSafeMap) to null
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(broken, null);
        // The call should now fail with NPE because the delegate map is null
        assertThrows(NullPointerException.class, () -> broken.headMap(5, true), "Calling headMap when internalMap is null must throw NullPointerException");
    }

    @Test
    void headMap_nullInternalMap_withNullKey_throwsNullPointerException() throws Exception {
        ConcurrentNavigableMapNullSafe<Integer, String> broken = new ConcurrentNavigableMapNullSafe<>();
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(broken, null);
        assertThrows(NullPointerException.class, () -> broken.headMap(null, false), "Calling headMap with null toKey when internalMap is null must throw NullPointerException");
    }
}
