package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_descendingMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    class UnsupportedDescMap<K, V> extends ConcurrentSkipListMap<K, V> {

        @Override
        public ConcurrentNavigableMap<K, V> descendingMap() {
            throw new UnsupportedOperationException("descendingMap not supported");
        }
    }

    class UnsupportedDescMap<K, V> extends ConcurrentSkipListMap<K, V> {

        @Override
        public ConcurrentNavigableMap<K, V> descendingMap() {
            throw new UnsupportedOperationException("descendingMap not supported");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default map used by many tests – a mixture of normal keys and a null key.
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent.
        map = null;
    }

    /**
     * Creates a {@link ConcurrentNavigableMapNullSafe} that uses the supplied comparator.
     *
     * @param comparator comparator to order the keys
     * @param <K>        key type
     * @param <V>        value type
     * @return a new map instance
     */
    private static <K, V> ConcurrentNavigableMapNullSafe<K, V> createMapWithComparator(Comparator<? super K> comparator) {
        // Build the underlying ConcurrentSkipListMap with the comparator.
        ConcurrentSkipListMap<K, V> internal = new ConcurrentSkipListMap<>(comparator);
        try {
            // The constructor used by descendingMap is package‑private; we access it via reflection.
            Constructor<ConcurrentNavigableMapNullSafe> ctor = ConcurrentNavigableMapNullSafe.class.getDeclaredConstructor(ConcurrentNavigableMap.class, Comparator.class);
            ctor.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentNavigableMapNullSafe<K, V> wrapper = ctor.newInstance(internal, comparator);
            return wrapper;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate map with custom comparator", e);
        }
    }

    /**
     * Reflectively sets the {@code internalMap} field of a {@link ConcurrentNavigableMapNullSafe}
     * instance. Used to simulate error conditions.
     */
    private static void setInternalMap(Object mapInstance, Object newInternal) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(mapInstance, newInternal);
    }

    // Example of a parameterized test for natural ordering cases.
    private static Stream<Arguments> naturalOrderingProvider() {
        return Stream.of(Arguments.of(3, 1, Arrays.asList(3, 2, 1)), Arguments.of(5, 2, Arrays.asList(5, 4, 3, 2, 1)));
    }

    @Test
    void descendingMapShouldReturnMapWithNullKeyFirstAndReverseOrder() {
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        // Basic sanity checks.
        assertNotNull(desc, "descendingMap() must not return null");
        assertTrue(desc instanceof ConcurrentNavigableMapNullSafe, "descendingMap() should return a ConcurrentNavigableMapNullSafe instance");
        // According to the ConcurrentNavigableMapNullSafe_descendingMap_Test, null keys are considered last in ascending order,
        // therefore they become the first entry in the descending view.
        assertNull(desc.firstKey(), "The first key of the descending map must be null");
        // Verify the remaining keys are in reverse natural order: "c", "b", "a".
        NavigableSet<String> keySet = desc.navigableKeySet();
        Iterator<String> it = keySet.iterator();
        assertEquals(null, it.next(), "First key should be null");
        assertEquals("c", it.next(), "Second key should be 'c'");
        assertEquals("b", it.next(), "Third key should be 'b'");
        assertEquals("a", it.next(), "Fourth key should be 'a'");
        assertFalse(it.hasNext(), "No more keys should be present");
        // Mutations on the descending view must affect the original map.
        desc.put("d", 4);
        assertEquals(4, map.get("d"), "Insertion via descending map should reflect in original map");
        Integer removed = desc.remove("b");
        assertEquals(2, removed, "Removed value should match original mapping");
        assertNull(map.get("b"), "Removal via descending map should reflect in original map");
        // Size consistency between the two views.
        assertEquals(map.size(), desc.size(), "Original map and its descending view must report the same size");
    }

    @Test
    void descendingMapOnEmptyMapReturnsEmptyMap() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<String, Integer> desc = empty.descendingMap();
        assertTrue(desc.isEmpty(), "Descending view of an empty map must be empty");
        assertEquals(0, desc.size(), "Size of descending view of an empty map must be 0");
    }

    @Test
    void emptyMapBehavior() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<String, Integer> desc = empty.descendingMap();
        assertTrue(desc.isEmpty(), "Descending view of an empty map must be empty");
        assertEquals(0, desc.size(), "Size of descending view of an empty map must be 0");
        // firstKey() and lastKey() should throw NoSuchElementException.
        assertThrows(NoSuchElementException.class, desc::firstKey, "firstKey() on empty descending map must throw NoSuchElementException");
        assertThrows(NoSuchElementException.class, desc::lastKey, "lastKey() on empty descending map must throw NoSuchElementException");
    }

    @Test
    void naturalOrderingDescendingView() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        intMap.put(2, "b");
        intMap.put(3, "c");
        ConcurrentNavigableMap<Integer, String> desc = intMap.descendingMap();
        assertEquals(3, desc.firstKey(), "firstKey() of descending view should be the original last key");
        assertEquals(1, desc.lastKey(), "lastKey() of descending view should be the original first key");
        assertEquals(3, desc.size(), "Size must be preserved");
        List<Integer> iterationOrder = new ArrayList<>();
        desc.forEach((k, v) -> iterationOrder.add(k));
        assertEquals(Arrays.asList(3, 2, 1), iterationOrder, "Iteration order must be reverse of natural order");
    }

    @Test
    void modificationsOnDescendingMapReflectedInOriginal() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "a");
        intMap.put(2, "b");
        intMap.put(3, "c");
        ConcurrentNavigableMap<Integer, String> desc = intMap.descendingMap();
        desc.put(4, "d");
        assertTrue(intMap.containsKey(4), "Original map must contain key added via descending view");
        assertEquals("d", intMap.get(4), "Value must match the one inserted through descending view");
    }

    @Test
    void customComparatorReverseOrder() {
        // Comparator that orders integers in descending order (larger first)
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> map = createMapWithComparator(reverseComparator);
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        // The internal map is already reversed, so the descending view restores natural order.
        assertEquals(1, desc.firstKey(), "firstKey() should be the smallest element after reversal");
        assertEquals(3, desc.lastKey(), "lastKey() should be the largest element after reversal");
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(desc.keySet()), "Iteration order must be natural (1,2,3)");
    }

    @Test
    void nullKeyHandlingMaskedNull() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(null, "nullVal");
        map.put(2, "two");
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        assertEquals(2, desc.size(), "Size must be 2 (null + one non‑null key)");
        assertNull(desc.firstKey(), "firstKey() must be null in descending view");
        assertEquals(2, desc.lastKey(), "lastKey() must be the greatest non‑null key");
        List<Integer> iterationOrder = new ArrayList<>();
        desc.forEach((k, v) -> iterationOrder.add(k));
        assertEquals(Arrays.asList(null, 2), iterationOrder, "Iteration order must be [null, 2]");
    }

    @Test
    void singleElementMapNonNullKey() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(10, "ten");
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        assertEquals(1, desc.size(), "Size must be 1");
        assertEquals(10, desc.firstKey(), "firstKey() must be 10");
        assertEquals(10, desc.lastKey(), "lastKey() must be 10");
        List<Integer> iteration = new ArrayList<>();
        desc.forEach((k, v) -> iteration.add(k));
        assertEquals(Collections.singletonList(10), iteration, "Iteration order must contain only 10");
    }

    @Test
    void singleElementMapNullKey() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(null, "nullVal");
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        assertEquals(1, desc.size(), "Size must be 1");
        assertNull(desc.firstKey(), "firstKey() must be null");
        assertNull(desc.lastKey(), "lastKey() must be null");
        List<Integer> iteration = new ArrayList<>();
        desc.forEach((k, v) -> iteration.add(k));
        assertEquals(Collections.singletonList(null), iteration, "Iteration order must contain only null");
    }

    @Test
    void comparatorAbsoluteValueReversal() {
        // Comparator that orders by absolute value (ascending)
        Comparator<Integer> absComparator = Comparator.comparingInt(Math::abs);
        ConcurrentNavigableMapNullSafe<Integer, String> map = createMapWithComparator(absComparator);
        map.put(-3, "neg3");
        map.put(2, "two");
        map.put(-1, "neg1");
        // Expected natural order according to absComparator: -1, 2, -3
        List<Integer> naturalOrder = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList(-1, 2, -3), naturalOrder, "Natural order with absolute comparator");
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        // Descending view should reverse the natural order.
        assertEquals(-3, desc.firstKey(), "firstKey() must be -3 after reversal");
        assertEquals(-1, desc.lastKey(), "lastKey() must be -1 after reversal");
        List<Integer> iteration = new ArrayList<>();
        desc.forEach((k, v) -> iteration.add(k));
        assertEquals(Arrays.asList(-3, 2, -1), iteration, "Iteration order must be [-3, 2, -1] (reverse of absolute order)");
    }

    @Test
    void idempotenceOfDescendingMap() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        ConcurrentNavigableMap<Integer, String> firstDesc = map.descendingMap();
        ConcurrentNavigableMap<Integer, String> secondDesc = firstDesc.descendingMap();
        // The second descending map should be equivalent to the original map.
        assertEquals(map.size(), secondDesc.size(), "Size must be unchanged after double descending");
        assertEquals(map.firstKey(), secondDesc.firstKey(), "firstKey after double descending must equal original");
        assertEquals(map.lastKey(), secondDesc.lastKey(), "lastKey after double descending must equal original");
        assertEquals(new ArrayList<>(map.keySet()), new ArrayList<>(secondDesc.keySet()), "Key order after double descending must match original");
    }

    @Test
    void removalPropagationFromDescendingView() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        Integer removed = desc.remove(2);
        assertEquals("b", removed, "Removed value must be 'b'");
        // Verify original map reflects removal.
        assertEquals(2, map.size(), "Original map size must be reduced to 2");
        assertFalse(map.containsKey(2), "Original map must no longer contain key 2");
        assertEquals(1, map.firstKey(), "Original firstKey must be 1");
        assertEquals(3, map.lastKey(), "Original lastKey must be 3");
        // Verify descending view after removal.
        assertEquals(2, desc.size(), "Descending view size must be 2 after removal");
        assertEquals(3, desc.firstKey(), "Descending firstKey must be 3");
        assertEquals(1, desc.lastKey(), "Descending lastKey must be 1");
        assertEquals(Arrays.asList(3, 1), new ArrayList<>(desc.keySet()), "Descending iteration order must be [3,1]");
    }

    @Test
    void descendingViewOfSubMap() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        // Populate keys 1‑5
        for (int i = 1; i <= 5; i++) {
            map.put(i, "v" + i);
        }
        // SubMap from 2 (inclusive) to 5 (exclusive) => keys [2,3,4]
        ConcurrentNavigableMap<Integer, String> sub = map.subMap(2, true, 5, false);
        assertEquals(Arrays.asList(2, 3, 4), new ArrayList<>(sub.keySet()), "SubMap must contain keys [2,3,4]");
        ConcurrentNavigableMap<Integer, String> desc = sub.descendingMap();
        assertEquals(3, desc.size(), "Descending subMap size must be 3");
        assertEquals(4, desc.firstKey(), "firstKey of descending subMap must be 4");
        assertEquals(2, desc.lastKey(), "lastKey of descending subMap must be 2");
        assertEquals(Arrays.asList(4, 3, 2), new ArrayList<>(desc.keySet()), "Iteration order of descending subMap must be [4,3,2]");
    }

    @Test
    void nullInternalMapHandlingThrowsNPE() throws Exception {
        // Create a normal map and then corrupt its internal map via reflection.
        ConcurrentNavigableMapNullSafe<String, Integer> corrupted = new ConcurrentNavigableMapNullSafe<>();
        // internalMap = null
        setInternalMap(corrupted, null);
        assertThrows(NullPointerException.class, corrupted::descendingMap, "descendingMap() must throw NullPointerException when internalMap is null");
    }

    @Test
    void invalidInternalMapTypeThrowsClassCastException() throws Exception {
        // Use a plain HashMap as the internal map – it does NOT implement ConcurrentNavigableMap.
        ConcurrentNavigableMapNullSafe<String, Integer> wrapper = new ConcurrentNavigableMapNullSafe<>();
        // wrong type
        setInternalMap(wrapper, new HashMap<>());
        assertThrows(ClassCastException.class, wrapper::descendingMap, "descendingMap() must throw ClassCastException when internalMap is not a ConcurrentNavigableMap");
    }

    @Test
    void unsupportedDescendingMapOnInternalMapPropagatesException() throws Exception {
        // Custom ConcurrentNavigableMap that throws UnsupportedOperationException on descendingMap().
        class UnsupportedDescMap<K, V> extends ConcurrentSkipListMap<K, V> {

            @Override
            public ConcurrentNavigableMap<K, V> descendingMap() {
                throw new UnsupportedOperationException("descendingMap not supported");
            }
        }
        ConcurrentNavigableMapNullSafe<String, Integer> wrapper = new ConcurrentNavigableMapNullSafe<>();
        setInternalMap(wrapper, new UnsupportedDescMap<String, Integer>());
        assertThrows(UnsupportedOperationException.class, wrapper::descendingMap, "descendingMap() must propagate UnsupportedOperationException from internal map");
    }

    @ParameterizedTest(name = "natural ordering descending view – firstKey={0}, lastKey={1}")
    @MethodSource("naturalOrderingProvider")
    void parameterizedNaturalOrdering(int expectedFirst, int expectedLast, List<Integer> expectedOrder) {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        // Insert keys 1..expectedLast (or more) to cover the order.
        for (int i = 1; i <= expectedFirst; i++) {
            map.put(i, "v" + i);
        }
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        assertEquals(expectedFirst, desc.firstKey(), "firstKey mismatch");
        assertEquals(expectedLast, desc.lastKey(), "lastKey mismatch");
        assertEquals(expectedOrder, new ArrayList<>(desc.keySet()), "Iteration order mismatch");
    }
}
