package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_tailMap_2_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    // Dummy field to allow the reflective constructor above to set the instance
    @SuppressWarnings("unused")
    private ConcurrentNavigableMapNullSafe<Integer, String> intMap;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a deterministic ordering; null is considered last
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.clear();
    }

    private static Stream<Arguments> provideNormalAndBoundaryCases() {
        // Helper to build maps more concisely
        Map<Integer, String> map12345 = Map.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e");
        Map<Integer, String> map135 = Map.of(1, "a", 3, "c", 5, "e");
        return Stream.of(// Normal behaviour – fromKey present
        Arguments.of(3, map12345, List.of(3, 4, 5)), // Normal behaviour – fromKey absent, start at next higher key
        Arguments.of(2, map135, List.of(3, 5)), // Boundary – fromKey greater than max key
        Arguments.of(10, map135, List.of()), // Boundary – fromKey less than min key
        Arguments.of(-5, map135, List.of(1, 3, 5)), // Empty map
        Arguments.of(1, Collections.emptyMap(), List.of()));
    }

    private static Stream<Arguments> provideSingleElementCases() {
        return Stream.of(Arguments.of(1, List.of(1)), Arguments.of(0, List.of(1)));
    }

    @Test
    void tailMap_InclusiveFromExistingKey() {
        // fromKey = "b" (inclusive by contract of tailMap(K))
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b");
        // Expected keys: "b", "c", null
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
        // Values must match original map
        assertEquals(2, tail.get("b"));
        assertEquals(3, tail.get("c"));
        assertEquals(99, tail.get(null));
        // The view should be backed by the original map
        tail.put("d", 4);
        assertEquals(4, map.get("d"));
        assertTrue(map.containsKey("d"));
    }

    @Test
    void tailMap_FromNonExistingKey() {
        // "bb" lies between "b" and "c"
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("bb");
        // Expected keys: "c", null (since "bb" is not present, the first key >= "bb" is "c")
        assertEquals(2, tail.size());
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
        assertFalse(tail.containsKey("b"));
        assertFalse(tail.containsKey("a"));
    }

    @Test
    void tailMap_FromNullKey_ShouldReturnOnlyNullEntry() {
        // Null is considered the greatest key; tailMap(null) should contain only the null entry
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
        assertEquals(99, tail.get(null));
        // Modifying the view affects the original map
        tail.remove(null);
        assertFalse(map.containsKey(null));
        // a, b, c remain
        assertEquals(3, map.size());
    }

    @Test
    void tailMap_ViewReflectsRemovalsFromOriginal() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b");
        // Remove a key directly from the original map
        map.remove("c");
        assertFalse(tail.containsKey("c"));
        assertEquals(2, tail.size());
        // Remove via the view and verify original map
        tail.remove(null);
        assertFalse(map.containsKey(null));
        // a, b remain
        assertEquals(2, map.size());
    }

    @Test
    void tailMap_ReturnedMapIsConcurrentNavigableMap() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("a");
        assertTrue(tail instanceof ConcurrentNavigableMap);
        assertTrue(tail instanceof NavigableMap);
    }

    // ---------- Normal behaviour & boundary cases (Integer keys) ----------
    @ParameterizedTest(name = "{index}: tailMap({0}) on map {1} → expected keys {2}")
    @MethodSource("provideNormalAndBoundaryCases")
    void tailMap_IntegerKeys(Integer fromKey, Map<Integer, String> initialEntries, List<Integer> expectedKeys) {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        initialEntries.forEach(intMap::put);
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(fromKey);
        List<Integer> actualKeys = new ArrayList<>(tail.keySet());
        assertIterableEquals(expectedKeys, actualKeys, () -> "tailMap(" + fromKey + ") returned unexpected key set");
    }

    // ---------- Null fromKey handling (Integer keys) ----------
    @Test
    void tailMap_NullFromKey_IntegerKeys() {
        // According to the library contract, null is considered the greatest key.
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(null, "z");
        intMap.put(1, "a");
        intMap.put(2, "b");
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(null);
        // Expect only the null entry
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
        assertEquals("z", tail.get(null));
    }

    // ---------- View reflects underlying map changes ----------
    @Test
    void tailMap_ViewReflectsAdditionsAfterCreation() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.putAll(Map.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e"));
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(3);
        // Add a new entry after the view has been obtained
        intMap.put(6, "f");
        List<Integer> expected = List.of(3, 4, 5, 6);
        List<Integer> actual = new ArrayList<>(tail.keySet());
        assertIterableEquals(expected, actual, "Tail view should reflect entries added to the underlying map after creation");
    }

    // ---------- Single element map ----------
    @ParameterizedTest(name = "single-element map tailMap({0}) → {1}")
    @MethodSource("provideSingleElementCases")
    void tailMap_SingleElement(Integer fromKey, List<Integer> expected) {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "only");
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(fromKey);
        List<Integer> actual = new ArrayList<>(tail.keySet());
        assertIterableEquals(expected, actual);
    }

    // ---------- Invalid key type ----------
    @Test
    void tailMap_InvalidKeyType_ThrowsClassCastException() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        intMap.put(2, "b");
        Executable call = () -> intMap.tailMap("stringKey");
        assertThrows(ClassCastException.class, call, "Calling tailMap with an incompatible key type should throw ClassCastException");
    }

    // ---------- Custom comparator that rejects null ----------
    @Test
    void tailMap_CustomComparatorRejectsNull_ThrowsIllegalArgumentException() {
        // Comparator that throws IllegalArgumentException when either argument is null
        Comparator<Integer> noNullComparator = (o1, o2) -> {
            if (o1 == null || o2 == null) {
                throw new IllegalArgumentException("Null keys are not allowed");
            }
            return Integer.compare(o1, o2);
        };
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>(new ConcurrentSkipListMap<>(noNullComparator));
        intMap.put(1, "a");
        intMap.put(2, "b");
        Executable call = () -> intMap.tailMap(null);
        assertThrows(IllegalArgumentException.class, call, "tailMap(null) should propagate IllegalArgumentException from a comparator that rejects null");
    }

    // ---------- Concurrent modification during view creation ----------
    @Test
    void tailMap_ConcurrentModificationDuringViewAccess_MayThrowCME() throws Exception {
        // This test attempts to provoke a CME by modifying the map from another thread
        // while the view is being accessed.  The underlying implementation uses
        // ConcurrentSkipListMap which is weakly consistent and typically does NOT throw CME,
        // however the contract of NavigableMap permits it, so we verify that no unexpected
        // exception is thrown and that the view eventually reflects the modifications.
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.putAll(Map.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e"));
        // Obtain the view
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(2);
        // Start a thread that removes an element after a tiny pause
        Thread remover = new Thread(() -> {
            try {
                Thread.sleep(10);
                intMap.remove(3);
            } catch (InterruptedException ignored) {
            }
        });
        remover.start();
        // Access the view; depending on timing it may or may not see the removal.
        // The test asserts that no ConcurrentModificationException propagates.
        assertDoesNotThrow(() -> {
            // Force iteration over the key set
            List<Integer> keys = new ArrayList<>(tail.keySet());
            // The result should contain 2,4,5 (3 may have been removed)
            assertTrue(keys.contains(2));
            assertTrue(keys.contains(4));
            assertTrue(keys.contains(5));
        });
        remover.join();
    }
}
