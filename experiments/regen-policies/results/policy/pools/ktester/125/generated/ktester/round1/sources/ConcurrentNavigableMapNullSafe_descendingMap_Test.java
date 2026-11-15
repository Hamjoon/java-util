package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_descendingMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a mix of normal and null keys
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.clear();
    }

    @Test
    void descendingMapPreservesAllEntriesInReverseOrder() {
        // When we obtain the descending view
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        // The descending map must contain the same number of entries
        assertEquals(map.size(), desc.size(), "Descending map should have same size");
        // The null key is considered *first* in the descending order
        assertNull(desc.firstKey(), "First key of descending map should be null");
        // The last key should be the smallest non‑null key ("a")
        assertEquals("a", desc.lastKey(), "Last key of descending map should be the smallest key");
        // Verify the ordering of the key set
        NavigableSet<String> keySet = desc.navigableKeySet();
        // Expected order: null, "b", "a"
        assertEquals(3, keySet.size());
        assertTrue(keySet.contains(null));
        assertTrue(keySet.contains("a"));
        assertTrue(keySet.contains("b"));
        // The iterator of the descending key set should return keys in the expected order
        var iterator = keySet.iterator();
        assertNull(iterator.next(), "First element should be null");
        assertEquals("b", iterator.next(), "Second element should be 'b'");
        assertEquals("a", iterator.next(), "Third element should be 'a'");
    }

    @Test
    void modificationsThroughDescendingMapAreReflectedInOriginalMap() {
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        // Add a new entry via the descending view
        desc.put("c", 3);
        assertEquals(3, desc.get("c"));
        assertEquals(3, map.get("c"), "Original map must see the entry added through descending map");
        // Update an existing entry via descending map
        desc.put("b", 20);
        assertEquals(20, desc.get("b"));
        assertEquals(20, map.get("b"), "Original map must see the updated value");
        // Remove the null key via descending map
        Integer removed = desc.remove(null);
        assertEquals(100, removed);
        assertNull(desc.get(null));
        assertNull(map.get(null), "Original map must no longer contain the null key");
    }

    @Test
    void descendingMapHandlesEmptyMapGracefully() {
        // Create an empty map instance
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<String, Integer> desc = empty.descendingMap();
        assertTrue(desc.isEmpty(), "Descending map of an empty map should also be empty");
        assertThrows(NoSuchElementException.class, desc::firstKey, "firstKey on empty descending map should throw");
        assertThrows(NoSuchElementException.class, desc::lastKey, "lastKey on empty descending map should throw");
    }

    @Test
    void descendingMapReturnsNonNullAndPreservesComparator() {
        // custom comparator (reverse natural order)
        Comparator<String> reverse = Comparator.reverseOrder();
        // Build a map with the custom comparator
        ConcurrentNavigableMap<String, Integer> backing = new ConcurrentSkipListMap<>(reverse);
        backing.put("x", 10);
        backing.put("y", 20);
        // Wrap it
        ConcurrentNavigableMapNullSafe<String, Integer> custom = createInstance(backing, reverse);
        ConcurrentNavigableMap<String, Integer> desc = custom.descendingMap();
        assertNotNull(desc, "descendingMap() must not return null");
        assertTrue(desc instanceof ConcurrentNavigableMapNullSafe, "Result should be a ConcurrentNavigableMapNullSafe");
        // Comparator of the descending view must be the same instance as the original one
        assertSame(reverse, desc.comparator(), "Comparator must be preserved");
    }

    @Test
    void descendingMapIteratorYieldsEntriesInReverseOrder() {
        map.clear();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);
        // Ascending order: 1,2,3 (null key not present)
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Integer> e : desc.entrySet()) {
            keys.add(e.getKey());
        }
        // Expected reverse order: 3,2,1
        assertEquals(List.of("3", "2", "1"), keys);
    }

    @Test
    void descendingMapReflectsSubsequentModificationsToOriginal() {
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        // Add a new entry to the original map
        map.put("z", 99);
        // The new entry must appear in the descending view at the correct position (after null, before 'b')
        assertTrue(desc.containsKey("z"));
        // Verify order: null, z, b, a
        Iterator<String> it = desc.navigableKeySet().iterator();
        assertNull(it.next());
        assertEquals("z", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
    }

    @Test
    void descendingMapReflectsModificationsThroughDescMap() {
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        // Put via descending map
        desc.put("d", 4);
        assertEquals(4, desc.get("d"));
        assertEquals(4, map.get("d"));
    }

    @Test
    void descendingMapSizeEqualsOriginalSize() {
        map.clear();
        map.putAll(Map.of("k1", 1, "k2", 2, "k3", 3, "k4", 4, "k5", 5));
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        assertEquals(5, desc.size(), "Descending map size must equal original size");
    }

    @Test
    void descendingMapIsEmptyAfterOriginalCleared() {
        map.put("x", 10);
        map.clear();
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        assertTrue(desc.isEmpty(), "Descending map should be empty after original map is cleared");
    }

    @Test
    void descendingMapFirstKeyEqualsOriginalLastKey() {
        map.clear();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        assertEquals(map.lastKey(), desc.firstKey(), "firstKey of descending map should equal lastKey of original");
    }

    @Test
    void descendingMapLastKeyEqualsOriginalFirstKey() {
        map.clear();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        assertEquals(map.firstKey(), desc.lastKey(), "lastKey of descending map should equal firstKey of original");
    }

    @Test
    void descendingMapContainsNullKeyAndReturnsItsValue() {
        map.put(null, 777);
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        assertTrue(desc.containsKey(null), "Descending map must contain the null key");
        assertEquals(777, desc.get(null));
    }

    @Test
    void descendingMapNullKeyAppearsAtFirstPositionInIteration() {
        map.clear();
        map.put(null, 0);
        map.put("1", 1);
        map.put("2", 2);
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        Iterator<Map.Entry<String, Integer>> it = desc.entrySet().iterator();
        Map.Entry<String, Integer> first = it.next();
        assertNull(first.getKey(), "Null key must be first in descending iteration");
        // Followed by the highest non‑null key
        assertEquals("2", it.next().getKey());
        assertEquals("1", it.next().getKey());
        assertFalse(it.hasNext());
    }

    @Test
    void removalViaDescendingMapPropagatesToOriginal() {
        map.clear();
        map.putAll(Map.of("A", 1, "B", 2, "C", 3));
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        Integer removed = desc.remove("B");
        assertEquals(2, removed);
        assertFalse(map.containsKey("B"));
        assertEquals(2, map.size());
        assertEquals(2, desc.size());
    }

    @Test
    void clearOnDescendingMapClearsOriginal() {
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        desc.clear();
        assertTrue(desc.isEmpty(), "Descending map should be empty after clear()");
        assertTrue(map.isEmpty(), "Original map must also be empty after clear() on descending view");
    }

    @Test
    void entrySetIteratorRemovePropagatesToOriginal() {
        map.clear();
        map.putAll(Map.of("10", 10, "20", 20, "30", 30));
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        Iterator<Map.Entry<String, Integer>> it = desc.entrySet().iterator();
        // iterator yields entries in descending order: 30,20,10
        // 30
        it.next();
        // 20
        Map.Entry<String, Integer> entry20 = it.next();
        assertEquals("20", entry20.getKey());
        // remove 20 via iterator
        it.remove();
        assertFalse(map.containsKey("20"));
        assertEquals(2, map.size());
        assertEquals(2, desc.size());
    }

    @Test
    void descendingMapThrowsNullPointerExceptionWhenInternalMapIsNull() throws Exception {
        // Create instance via default constructor then null‑out internalMap
        ConcurrentNavigableMapNullSafe<String, Integer> victim = new ConcurrentNavigableMapNullSafe<>();
        Field internal = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internal.setAccessible(true);
        internal.set(victim, null);
        assertThrows(NullPointerException.class, victim::descendingMap, "descendingMap() must throw NPE when internalMap is null");
    }

    @Test
    void descendingMapThrowsClassCastExceptionWhenBackingIsNotNavigable() throws Exception {
        // Backing map that is only a ConcurrentMap (not a ConcurrentNavigableMap)
        ConcurrentMap<String, Integer> backing = new ConcurrentHashMap<>();
        backing.put("x", 1);
        // Use the package‑private constructor via reflection
        ConcurrentNavigableMapNullSafe<String, Integer> wrapper = createInstance(backing, null);
        assertThrows(ClassCastException.class, wrapper::descendingMap, "descendingMap() should throw CCE when internalMap is not a ConcurrentNavigableMap");
    }

    @Test
    void descendingMapPropagatesUnsupportedOperationExceptionFromDelegate(@Mock ConcurrentNavigableMap<String, Integer> mockDelegate) {
        // Configure the mock to throw on descendingMap()
        when(mockDelegate.descendingMap()).thenThrow(new UnsupportedOperationException("unsupported"));
        // Wrap the mock (constructor via reflection)
        ConcurrentNavigableMapNullSafe<String, Integer> wrapper = createInstance(mockDelegate, null);
        assertThrows(UnsupportedOperationException.class, wrapper::descendingMap, "Exception from delegate's descendingMap() must propagate");
    }

    /**
     * Creates a {@link ConcurrentNavigableMapNullSafe} instance using the
     * package‑private constructor that accepts an internal map and a comparator.
     *
     * @param internal   the backing map (may be any {@link ConcurrentMap})
     * @param comparator the comparator to store (may be {@code null})
     * @return a new {@code ConcurrentNavigableMapNullSafe} wrapping the supplied map
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <K, V> ConcurrentNavigableMapNullSafe<K, V> createInstance(ConcurrentMap<K, V> internal, Comparator<? super K> comparator) {
        try {
            Constructor<ConcurrentNavigableMapNullSafe> ctor = (Constructor<ConcurrentNavigableMapNullSafe>) ConcurrentNavigableMapNullSafe.class.getDeclaredConstructor(ConcurrentMap.class, Comparator.class);
            ctor.setAccessible(true);
            return ctor.newInstance(internal, comparator);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate ConcurrentNavigableMapNullSafe via reflection", e);
        }
    }
}
