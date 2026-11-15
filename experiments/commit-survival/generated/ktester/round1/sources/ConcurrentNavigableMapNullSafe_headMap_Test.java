package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_headMap_Test {

    /**
     * The map under test – a fresh instance for each test method.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * Populate the map with a deterministic ordering of keys.
     */
    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        // null key is considered *last* in the ascending view
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up – the map will be garbage‑collected.
        map = null;
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// inclusive = true → should contain the toKey
        Arguments.of(1, true, List.of(1)), // inclusive = false → should NOT contain the toKey
        Arguments.of(1, false, List.of()));
    }

    @Test
    void headMap_exclusive_boundary() {
        // toKey = "c", inclusive = false → entries < "c" (a, b)
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c", false);
        assertEquals(2, head.size(), "Exclusive headMap should contain two entries");
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertFalse(head.containsKey("c"));
        assertFalse(head.containsKey("d"));
        assertFalse(head.containsKey(null));
        // Verify that the view is still backed by the original map:
        head.put("x", 42);
        assertEquals(42, map.get("x"));
    }

    @Test
    void headMap_inclusive_boundary() {
        // toKey = "c", inclusive = true → entries ≤ "c" (a, b, c)
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c", true);
        assertEquals(3, head.size(), "Inclusive headMap should contain three entries");
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertTrue(head.containsKey("c"));
        assertFalse(head.containsKey("d"));
        assertFalse(head.containsKey(null));
        // Removing an entry from the view removes it from the original map
        Integer removed = head.remove("b");
        assertEquals(2, removed);
        assertNull(map.get("b"));
    }

    @Test
    void headMap_with_null_toKey() {
        // According to the contract, null is treated as the *largest* key.
        // Therefore a headMap up to null (inclusive = false) should contain all *non‑null* keys.
        ConcurrentNavigableMap<String, Integer> head = map.headMap(null, false);
        // All regular keys should be present, but the null key itself must not.
        assertEquals(4, head.size());
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertTrue(head.containsKey("c"));
        assertTrue(head.containsKey("d"));
        assertFalse(head.containsKey(null));
        // Inclusive = true should also include the null key.
        ConcurrentNavigableMap<String, Integer> headInc = map.headMap(null, true);
        assertEquals(5, headInc.size());
        assertTrue(headInc.containsKey(null));
    }

    @Test
    void headMap_result_is_nullSafe_instance() {
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c", false);
        assertTrue(head instanceof ConcurrentNavigableMapNullSafe, "headMap should return a ConcurrentNavigableMapNullSafe instance");
    }

    @Test
    void headMap_empty_when_toKey_is_before_first() {
        // "a" is the smallest key; using a key that sorts before it yields an empty view.
        ConcurrentNavigableMap<String, Integer> head = map.headMap("a", false);
        assertTrue(head.isEmpty(), "headMap before the first key should be empty");
    }

    @ParameterizedTest(name = "headMap(1, inclusive={1}) returns keys {2}")
    @MethodSource("basicFunctionalityProvider")
    void headMap_basic_functionality(Integer toKey, boolean inclusive, List<Integer> expectedKeys) {
        // Build a map with keys 1‑4
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        intMap.put(4, "four");
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(toKey, inclusive);
        List<Integer> actual = new ArrayList<>(head.keySet());
        // The expected list is built from the specification: keys less than (or equal) to toKey
        List<Integer> expected = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            if (i < toKey || (inclusive && i.equals(toKey))) {
                expected.add(i);
            }
        }
        assertEquals(expected, actual);
    }

    @Test
    void headMap_null_toKey_inclusive_true_contains_null_key() {
        // null is the largest key; inclusive=true must include the null entry.
        ConcurrentNavigableMap<String, Integer> head = map.headMap(null, true);
        assertTrue(head.containsKey(null), "Inclusive true should include the null key");
        assertEquals(5, head.size(), "All entries including null should be present");
    }

    @Test
    void headMap_null_toKey_inclusive_false_excludes_null_key() {
        // null is the largest key; exclusive should exclude it but contain all others.
        ConcurrentNavigableMap<String, Integer> head = map.headMap(null, false);
        assertFalse(head.containsKey(null), "Exclusive false must NOT contain the null key");
        assertEquals(4, head.size(), "All non‑null entries should be present");
    }

    @Test
    void headMap_on_empty_map_returns_empty() {
        ConcurrentNavigableMapNullSafe<Integer, String> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<Integer, String> head = empty.headMap(10, true);
        assertTrue(head.isEmpty(), "headMap on an empty map must be empty");
    }

    @Test
    void headMap_on_empty_map_exclusive_returns_empty() {
        ConcurrentNavigableMapNullSafe<Integer, String> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<Integer, String> head = empty.headMap(10, false);
        assertTrue(head.isEmpty(), "headMap on an empty map must be empty regardless of inclusivity");
    }

    @Test
    void headMap_toKey_not_present_exclusive() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        intMap.put(4, "four");
        // toKey = 5 (not present), exclusive → should contain 1‑4
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(5, false);
        assertEquals(Set.of(1, 2, 3, 4), head.keySet());
    }

    @Test
    void headMap_toKey_not_present_inclusive_behaves_like_exclusive() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        intMap.put(4, "four");
        // toKey = 5 (not present), inclusive → still only 1‑4
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(5, true);
        assertEquals(Set.of(1, 2, 3, 4), head.keySet());
    }

    @Test
    void headMap_with_reverse_comparator_inclusive() {
        // Comparator that orders integers descending
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> revMap = new ConcurrentNavigableMapNullSafe<>(new ConcurrentSkipListMap<>(reverse));
        revMap.put(1, "one");
        revMap.put(2, "two");
        revMap.put(3, "three");
        revMap.put(4, "four");
        // In reverse order, "less than or equal" means "greater or equal" in natural order.
        // headMap(2, true) should therefore contain keys 4 and 3 and 2.
        ConcurrentNavigableMap<Integer, String> head = revMap.headMap(2, true);
        assertEquals(List.of(4, 3, 2), new ArrayList<>(head.keySet()));
    }

    @Test
    void headMap_with_reverse_comparator_exclusive() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> revMap = new ConcurrentNavigableMapNullSafe<>(new ConcurrentSkipListMap<>(reverse));
        revMap.put(1, "one");
        revMap.put(2, "two");
        revMap.put(3, "three");
        revMap.put(4, "four");
        // Exclusive: headMap(2, false) should contain keys > 2 in natural order → 4 and 3.
        ConcurrentNavigableMap<Integer, String> head = revMap.headMap(2, false);
        assertEquals(List.of(4, 3), new ArrayList<>(head.keySet()));
    }

    @Test
    void headMap_is_live_view_mutations_reflected() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        intMap.put(4, "four");
        // Obtain a headMap up to 3 inclusive.
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(3, true);
        assertEquals(Set.of(1, 2, 3), head.keySet());
        // Add a new entry to the original map that is within the headMap range.
        // update existing key
        intMap.put(2, "new-two");
        // outside range – should NOT appear in head
        intMap.put(5, "five");
        // smaller than any existing key – should appear
        intMap.put(0, "zero");
        // The headMap view must reflect the changes.
        assertEquals(Set.of(0, 1, 2, 3), head.keySet());
        // Removing via the view must affect the original map.
        head.remove(1);
        assertNull(intMap.get(1));
        assertFalse(head.containsKey(1));
    }

    @Test
    void headMap_with_incompatible_toKey_throws_ClassCastException() {
        // Map with Integer keys; provide a String key.
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        assertThrows(ClassCastException.class, () -> intMap.headMap((Integer) (Object) "UNCOMPARABLE_OBJECT", true));
    }

    @Test
    void headMap_with_null_inclusive_flag_throws_NullPointerException() throws Exception {
        // Use reflection to invoke the method with a null Boolean (unboxing will NPE).
        Method method = ConcurrentNavigableMapNullSafe.class.getDeclaredMethod("headMap", Object.class, boolean.class);
        method.setAccessible(true);
        assertThrows(NullPointerException.class, () -> method.invoke(map, "c", (Object) null));
    }

    @Test
    void headMap_with_illegal_internal_map_throws_IllegalStateException() throws Exception {
        // Replace the internal map with a plain HashMap (not a ConcurrentNavigableMap).
        ConcurrentNavigableMapNullSafe<String, Integer> broken = new ConcurrentNavigableMapNullSafe<>();
        // populate so internalMap is non‑null
        broken.put("x", 1);
        // Access the private field declared in AbstractConcurrentNullSafeMap.
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // non‑ConcurrentNavigableMap
        internalMapField.set(broken, new HashMap<>());
        // The cast inside headMap will fail, which the library wraps as IllegalStateException.
        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> broken.headMap("x", true));
        assertTrue(ise.getCause() instanceof ClassCastException, "Root cause should be ClassCastException");
    }

    @Test
    void headMap_when_underlying_map_throws_UnsupportedOperationException() throws Exception {
        // Create a dummy ConcurrentNavigableMap that throws on headMap().
        ConcurrentNavigableMap<Object, Object> throwingMap = new ConcurrentNavigableMap<Object, Object>() {

            @Override
            public Entry<Object, Object> lowerEntry(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object lowerKey(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Entry<Object, Object> floorEntry(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object floorKey(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Entry<Object, Object> ceilingEntry(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object ceilingKey(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Entry<Object, Object> higherEntry(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object higherKey(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Entry<Object, Object> firstEntry() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Entry<Object, Object> lastEntry() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Entry<Object, Object> pollFirstEntry() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Entry<Object, Object> pollLastEntry() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public boolean containsKey(Object key) {
                return false;
            }

            @Override
            public boolean containsValue(Object value) {
                return false;
            }

            @Override
            public Object get(Object key) {
                return null;
            }

            @Override
            public Object put(Object key, Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object remove(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends Object, ? extends Object> m) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Comparator<? super Object> comparator() {
                return null;
            }

            @Override
            public Object firstKey() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object lastKey() {
                throw new UnsupportedOperationException();
            }

            @Override
            public NavigableSet<Object> navigableKeySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public NavigableSet<Object> descendingKeySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public NavigableMap<Object, Object> descendingMap() {
                throw new UnsupportedOperationException();
            }

            @Override
            public NavigableMap<Object, Object> subMap(Object fromKey, boolean fromInclusive, Object toKey, boolean toInclusive) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NavigableMap<Object, Object> headMap(Object toKey, boolean inclusive) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NavigableMap<Object, Object> tailMap(Object fromKey, boolean inclusive) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SortedMap<Object, Object> subMap(Object fromKey, Object toKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SortedMap<Object, Object> headMap(Object toKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SortedMap<Object, Object> tailMap(Object fromKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<Entry<Object, Object>> entrySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<Object> keySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<Object> values() {
                throw new UnsupportedOperationException();
            }
        };
        // Inject the throwing map via reflection.
        ConcurrentNavigableMapNullSafe<String, Integer> wrapper = new ConcurrentNavigableMapNullSafe<>();
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(wrapper, throwingMap);
        // The call must propagate the UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> wrapper.headMap("any", true));
    }
}
