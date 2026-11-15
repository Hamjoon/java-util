package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

class CaseInsensitiveMap_values_Test {

    private CaseInsensitiveMap<String, Integer> caseInsensitiveMap;

    private Map<String, Integer> referenceMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a fresh map for each test to avoid cross‑test interference.
        caseInsensitiveMap = new CaseInsensitiveMap<>();
        referenceMap = new LinkedHashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required but keeps the pattern consistent.
        caseInsensitiveMap = null;
        referenceMap = null;
    }

    /**
     * Helper to replace the private {@code map} field via reflection.
     */
    private void setInternalMap(Map<String, Integer> newMap) throws Exception {
        Field f = CaseInsensitiveMap.class.getDeclaredField("map");
        f.setAccessible(true);
        f.set(caseInsensitiveMap, newMap);
    }

    /**
     * Helper to null‑out the internal map (used for NPE test).
     */
    private void nullifyInternalMap() throws Exception {
        Field f = CaseInsensitiveMap.class.getDeclaredField("map");
        f.setAccessible(true);
        f.set(caseInsensitiveMap, null);
    }

    static Stream<Collection<Integer>> threeValueProvider() {
        // Dummy stream – JUnit requires a source, but the test does not need parameters.
        return Stream.of(Collections.emptyList());
    }

    @Test
    void valuesReflectInsertedEntries() {
        // given
        referenceMap.put("One", 1);
        referenceMap.put("Two", 2);
        referenceMap.put("Three", 3);
        caseInsensitiveMap.putAll(referenceMap);
        // when
        Collection<Integer> values = caseInsensitiveMap.values();
        // then
        assertEquals(3, values.size(), "Collection size should match number of entries");
        assertTrue(values.containsAll(referenceMap.values()), "Returned collection should contain all values from the map");
    }

    @Test
    void valuesAreLiveView() {
        // given
        caseInsensitiveMap.put("Alpha", 10);
        caseInsensitiveMap.put("Beta", 20);
        Collection<Integer> values = caseInsensitiveMap.values();
        // when – modify the map after obtaining the collection
        caseInsensitiveMap.put("Gamma", 30);
        caseInsensitiveMap.remove("Alpha");
        // then – the collection should reflect those changes
        assertEquals(2, values.size(), "Collection should reflect removal and addition");
        assertTrue(values.contains(20), "Remaining value should still be present");
        assertTrue(values.contains(30), "Newly added value should be present");
        assertFalse(values.contains(10), "Removed value should no longer be present");
    }

    @Test
    void valuesFromEmptyMapIsEmpty() {
        // given – an empty map (already created in @BeforeEach)
        // when
        Collection<Integer> values = caseInsensitiveMap.values();
        // then
        assertNotNull(values, "Values collection should never be null");
        assertTrue(values.isEmpty(), "Values collection of an empty map must be empty");
    }

    @Test
    void valuesDoNotAllowModificationThroughCollection() {
        // given
        caseInsensitiveMap.put("Key", 42);
        Collection<Integer> values = caseInsensitiveMap.values();
        // when / then – attempting to modify the collection should throw
        assertThrows(UnsupportedOperationException.class, () -> values.add(99), "Adding directly to the values collection should not be supported");
        assertThrows(UnsupportedOperationException.class, () -> values.remove(42), "Removing directly from the values collection should not be supported");
    }

    @Test
    void valuesReflectCaseInsensitiveKeyBehaviour() {
        // given – case‑insensitive key handling
        caseInsensitiveMap.put("MiXeD", 5);
        // When we query using a different case, the same entry is returned
        Integer retrieved = caseInsensitiveMap.get("mixed");
        // then
        assertEquals(5, retrieved, "Map should treat keys case‑insensitively");
        // The values collection must contain the single value regardless of key case used
        Collection<Integer> values = caseInsensitiveMap.values();
        assertEquals(1, values.size());
        assertTrue(values.contains(5));
    }

    /* ---------- 1. empty collection ---------- */
    @Test
    void valuesWhenMapIsEmptyReturnsEmptyCollection() {
        Collection<Integer> values = caseInsensitiveMap.values();
        assertNotNull(values, "values() must never return null");
        assertTrue(values.isEmpty(), "Empty map should yield empty collection");
    }

    /* ---------- 2. collection containing three values (any order) ---------- */
    @ParameterizedTest
    @MethodSource("threeValueProvider")
    void valuesContainThreeElements(Collection<Integer> ignored) {
        // The provider supplies no arguments; we just use it to document the scenario.
        caseInsensitiveMap.put("a", 1);
        caseInsensitiveMap.put("b", 2);
        caseInsensitiveMap.put("c", 3);
        Collection<Integer> values = caseInsensitiveMap.values();
        assertEquals(3, values.size(), "Should contain three values");
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3)), "Values collection must contain all inserted values");
    }

    /* ---------- 3. live view – addition ---------- */
    @Test
    void valuesLiveViewReflectsAdditions() {
        caseInsensitiveMap.put("x", 10);
        Collection<Integer> values = caseInsensitiveMap.values();
        assertTrue(values.contains(10));
        caseInsensitiveMap.put("y", 20);
        assertTrue(values.contains(20), "Values view must reflect newly added entry");
        assertEquals(2, values.size());
    }

    /* ---------- 4. live view – removals ---------- */
    @Test
    void valuesLiveViewReflectsRemovals() {
        caseInsensitiveMap.put("k1", 100);
        caseInsensitiveMap.put("k2", 200);
        Collection<Integer> values = caseInsensitiveMap.values();
        assertEquals(2, values.size());
        caseInsensitiveMap.remove("k1");
        assertFalse(values.contains(100), "Removed value must disappear from view");
        assertEquals(1, values.size());
    }

    /* ---------- 5. UnsupportedOperationException from immutable underlying map ---------- */
    @Test
    void valuesRemoveOnImmutableUnderlyingMapThrowsUOE() throws Exception {
        Map<String, Integer> immutable = Collections.unmodifiableMap(new HashMap<>());
        setInternalMap(immutable);
        Collection<Integer> values = caseInsensitiveMap.values();
        assertThrows(UnsupportedOperationException.class, () -> values.remove(1));
    }

    /* ---------- 6. non‑null Collection instance ---------- */
    @Test
    void valuesNeverReturnsNull() {
        assertNotNull(caseInsensitiveMap.values(), "values() must never return null on a newly created map");
    }

    /* ---------- 7. same Collection instance on repeated calls ---------- */
    @Test
    void valuesReturnsSameInstanceOnRepeatedCalls() {
        Collection<Integer> first = caseInsensitiveMap.values();
        Collection<Integer> second = caseInsensitiveMap.values();
        assertSame(first, second, "Repeated calls to values() must return the identical object");
    }

    /* ---------- 8. iterator.remove updates the map ---------- */
    @Test
    void iteratorRemoveUpdatesUnderlyingMap() {
        caseInsensitiveMap.put("one", 1);
        caseInsensitiveMap.put("two", 2);
        caseInsensitiveMap.put("three", 3);
        Iterator<Integer> it = caseInsensitiveMap.values().iterator();
        while (it.hasNext()) {
            Integer v = it.next();
            if (v.equals(2)) {
                // should delete the entry with value 2
                it.remove();
                break;
            }
        }
        assertFalse(caseInsensitiveMap.containsValue(2));
        assertEquals(2, caseInsensitiveMap.size());
    }

    /* ---------- 9. collection.clear() empties the map ---------- */
    @Test
    void clearOnValuesCollectionEmptiesMap() {
        caseInsensitiveMap.put("a", 10);
        caseInsensitiveMap.put("b", 20);
        assertEquals(2, caseInsensitiveMap.size());
        caseInsensitiveMap.values().clear();
        assertEquals(0, caseInsensitiveMap.size(), "Map must be empty after clearing the values view");
        assertTrue(caseInsensitiveMap.values().isEmpty(), "Values view must also be empty");
    }

    /* ---------- 10. add on values collection throws UOE ---------- */
    @Test
    void valuesAddThrowsUnsupportedOperationException() {
        caseInsensitiveMap.put("k", 5);
        Collection<Integer> values = caseInsensitiveMap.values();
        assertThrows(UnsupportedOperationException.class, () -> values.add(99));
    }

    /* ---------- 11. collection contains null value ---------- */
    @Test
    void valuesContainNullWhenMapHasNullValue() {
        caseInsensitiveMap.put("nullKey", null);
        Collection<Integer> values = caseInsensitiveMap.values();
        assertTrue(values.contains(null), "Values collection must report presence of null");
        assertEquals(1, values.size());
    }

    /* ---------- 12. toArray returns all values (any order) ---------- */
    @Test
    void valuesToArrayContainsAllElements() {
        caseInsensitiveMap.put("x", 7);
        caseInsensitiveMap.put("y", 8);
        caseInsensitiveMap.put("z", 9);
        Object[] array = caseInsensitiveMap.values().toArray();
        assertEquals(3, array.length);
        List<Object> list = Arrays.asList(array);
        assertTrue(list.containsAll(Arrays.asList(7, 8, 9)), "toArray must contain exactly the map's values");
    }

    /* ---------- 13. contains true/false ---------- */
    @Test
    void valuesContainsWorksAsExpected() {
        caseInsensitiveMap.put("a", 1);
        caseInsensitiveMap.put("b", 2);
        Collection<Integer> values = caseInsensitiveMap.values();
        assertTrue(values.contains(1), "Should contain present value");
        assertFalse(values.contains(3), "Should not contain absent value");
    }

    /* ---------- 14. size matches map size after mutations ---------- */
    @Test
    void sizeAlwaysMatchesMapSize() {
        assertEquals(caseInsensitiveMap.size(), caseInsensitiveMap.values().size());
        caseInsensitiveMap.put("one", 1);
        assertEquals(caseInsensitiveMap.size(), caseInsensitiveMap.values().size());
        caseInsensitiveMap.put("two", 2);
        assertEquals(caseInsensitiveMap.size(), caseInsensitiveMap.values().size());
        caseInsensitiveMap.remove("one");
        assertEquals(caseInsensitiveMap.size(), caseInsensitiveMap.values().size());
        caseInsensitiveMap.clear();
        assertEquals(0, caseInsensitiveMap.size());
        assertEquals(0, caseInsensitiveMap.values().size());
    }

    /* ---------- 15. iterator.remove before next throws IllegalStateException ---------- */
    @Test
    void iteratorRemoveBeforeNextThrowsIllegalStateException() {
        caseInsensitiveMap.put("a", 1);
        Iterator<Integer> it = caseInsensitiveMap.values().iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    /* ---------- 16. iterator.remove twice without next throws IllegalStateException ---------- */
    @Test
    void iteratorRemoveTwiceThrowsIllegalStateException() {
        caseInsensitiveMap.put("a", 1);
        caseInsensitiveMap.put("b", 2);
        Iterator<Integer> it = caseInsensitiveMap.values().iterator();
        assertTrue(it.hasNext());
        // point to first element
        it.next();
        // first removal OK
        it.remove();
        assertThrows(IllegalStateException.class, it::remove, "Second remove without intervening next must fail");
    }

    /* ---------- 17. ConcurrentModificationException after structural change ---------- */
    @Test
    void iteratorDetectsConcurrentModification() {
        caseInsensitiveMap.put("a", 1);
        caseInsensitiveMap.put("b", 2);
        Iterator<Integer> it = caseInsensitiveMap.values().iterator();
        // Structural change after iterator creation
        caseInsensitiveMap.put("c", 3);
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must detect concurrent modification");
    }

    /* ---------- 18. NullPointerException when internal map is null ---------- */
    @Test
    void valuesThrowsNPEWhenInternalMapIsNull() throws Exception {
        nullifyInternalMap();
        assertThrows(NullPointerException.class, () -> caseInsensitiveMap.values(), "Calling values() with a null internal map should throw NPE");
    }

    /* ---------- 19. values collection backed by a mock map (demonstrating Mockito usage) ---------- */
    @Test
    void valuesDelegatesToUnderlyingMapUsingMockito() throws Exception {
        // Create a mock map that returns a pre‑defined collection.
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        Collection<Integer> mockValues = Arrays.asList(42, 43);
        when(mockMap.values()).thenReturn(mockValues);
        // Inject the mock via reflection.
        setInternalMap(mockMap);
        Collection<Integer> values = caseInsensitiveMap.values();
        // Verify delegation.
        verify(mockMap, times(1)).values();
        assertSame(mockValues, values, "Returned collection must be exactly what the underlying map provides");
    }
}
