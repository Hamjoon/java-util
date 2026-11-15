package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_descendingMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test – uses the default constructor which creates a
        // ConcurrentSkipListMap internally.
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null key is treated as the greatest in ascending order
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; map has no external resources
        map = null;
    }

    /**
     * Utility to set a private field via reflection.
     *
     * @param target    the object whose field should be modified
     * @param fieldName the name of the field
     * @param value     the new value
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass();
        // Walk up the hierarchy until we find the field
        while (f != null) {
            try {
                Field field = f.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                f = f.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " not found in class hierarchy");
    }

    @Test
    void descendingMapPreservesAllEntries() {
        // When we obtain the descending view, it must contain exactly the same entries
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        assertEquals(map.size(), desc.size(), "Descending map should have same size");
        assertTrue(desc.containsKey("a"));
        assertTrue(desc.containsKey("b"));
        assertTrue(desc.containsKey("c"));
        assertTrue(desc.containsKey(null));
        // Values must match
        assertEquals(1, desc.get("a"));
        assertEquals(2, desc.get("b"));
        assertEquals(3, desc.get("c"));
        assertEquals(99, desc.get(null));
    }

    @Test
    void descendingMapOrdersKeysCorrectly() {
        // In the descending view, the null key is considered the *first* element
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        // Obtain the key set iterator and verify order
        Iterator<String> it = desc.keySet().iterator();
        // First key should be null
        assertTrue(it.hasNext());
        assertNull(it.next(), "First key in descending map should be null");
        // Then the remaining keys in reverse natural order: c, b, a
        List<String> expected = Arrays.asList("c", "b", "a");
        for (String exp : expected) {
            assertTrue(it.hasNext(), "Expected key " + exp);
            assertEquals(exp, it.next());
        }
        assertFalse(it.hasNext(), "No more keys should remain");
    }

    @Test
    void descendingKeySetReflectsDescendingOrder() {
        // descendingKeySet() is implemented via descendingMap().navigableKeySet()
        NavigableSet<String> descendingKeySet = map.descendingKeySet();
        // The set's iterator should produce the same ordering as verified above
        Iterator<String> it = descendingKeySet.iterator();
        assertTrue(it.hasNext());
        assertNull(it.next(), "First element of descendingKeySet should be null");
        List<String> expected = Arrays.asList("c", "b", "a");
        for (String exp : expected) {
            assertTrue(it.hasNext());
            assertEquals(exp, it.next());
        }
        assertFalse(it.hasNext());
    }

    @Test
    void modificationsOnDescendingMapAffectOriginal() {
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        // Remove an entry via the descending map
        Integer removed = desc.remove("b");
        assertEquals(2, removed);
        assertNull(map.get("b"));
        assertEquals(3, map.size());
        // Add a new entry via the descending map
        desc.put("d", 4);
        assertEquals(4, map.get("d"));
        assertEquals(4, desc.size());
    }

    @Test
    void descendingMapHandlesNullValues() {
        map.put("x", null);
        // null key with null value
        map.put(null, null);
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        // Verify presence of null value entries
        assertTrue(desc.containsKey("x"));
        assertNull(desc.get("x"));
        assertTrue(desc.containsKey(null));
        assertNull(desc.get(null));
        // Verify ordering: null key first, then keys in reverse order
        Iterator<String> it = desc.keySet().iterator();
        // null key
        assertNull(it.next());
        // Remaining keys in reverse natural order (including "x")
        List<String> expected = Arrays.asList("x", "c", "b", "a");
        for (String exp : expected) {
            assertEquals(exp, it.next());
        }
    }

    @Test
    @DisplayName("descendingMap() – iterator yields entries in reverse order")
    void descendingMapIteratorReversedOrder() {
        // Original map natural order: a, b, c, null
        List<Map.Entry<String, Integer>> original = new ArrayList<>(map.entrySet());
        // Expected reverse order (null first)
        List<Map.Entry<String, Integer>> expected = new ArrayList<>(original);
        Collections.reverse(expected);
        List<Map.Entry<String, Integer>> actual = new ArrayList<>(map.descendingMap().entrySet());
        assertEquals(expected.size(), actual.size(), "Size must match");
        for (int i = 0; i < expected.size(); i++) {
            Map.Entry<String, Integer> e1 = expected.get(i);
            Map.Entry<String, Integer> e2 = actual.get(i);
            assertEquals(e1.getKey(), e2.getKey(), "Key at position " + i);
            assertEquals(e1.getValue(), e2.getValue(), "Value at position " + i);
        }
    }

    @Test
    @DisplayName("descendingMap() – comparator instance is preserved")
    void descendingMapComparatorPreserved() throws Exception {
        Comparator<String> custom = Comparator.reverseOrder();
        // Use reflection to inject a custom comparator into the map
        Field compField = ConcurrentNavigableMapNullSafe.class.getDeclaredField("originalComparator");
        compField.setAccessible(true);
        compField.set(map, custom);
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        assertSame(custom, desc.comparator(), "Comparator returned by descendingMap() must be the same instance supplied to the original map");
    }

    @Test
    @DisplayName("descendingMap() – modifications after creation are reflected")
    void descendingMapReflectsLaterModifications() {
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        // Add a new entry to the original map
        map.put("z", 26);
        // The descending view must now contain the new entry at the correct position (after null)
        Iterator<String> it = desc.keySet().iterator();
        // null key stays first
        assertNull(it.next());
        // newly added key should appear next (since 'z' > 'c')
        assertEquals("z", it.next());
        // Remaining keys continue in reverse order
        List<String> expected = Arrays.asList("c", "b", "a");
        for (String exp : expected) {
            assertEquals(exp, it.next());
        }
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("descendingMap() – double descending restores original order")
    void doubleDescendingRestoresOriginalOrder() {
        ConcurrentNavigableMap<String, Integer> firstDesc = map.descendingMap();
        ConcurrentNavigableMap<String, Integer> secondDesc = firstDesc.descendingMap();
        // The second descending map should iterate in the same order as the original map
        List<String> originalOrder = new ArrayList<>(map.keySet());
        List<String> doubleDescOrder = new ArrayList<>(secondDesc.keySet());
        assertEquals(originalOrder, doubleDescOrder, "A double‑descending map must present keys in the original natural order");
    }

    @Test
    @DisplayName("descendingMap() – empty map yields empty descending map")
    void descendingMapOnEmptyMap() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        assertTrue(empty.isEmpty());
        ConcurrentNavigableMap<String, Integer> desc = empty.descendingMap();
        assertTrue(desc.isEmpty(), "Descending view of an empty map must also be empty");
        assertEquals(0, desc.size());
    }

    @Test
    @DisplayName("descendingMap() – single entry map behaviour")
    void descendingMapSingleEntry() {
        ConcurrentNavigableMapNullSafe<String, Integer> single = new ConcurrentNavigableMapNullSafe<>();
        single.put("only", 42);
        ConcurrentNavigableMap<String, Integer> desc = single.descendingMap();
        assertEquals(1, desc.size());
        assertEquals("only", desc.firstKey());
        assertEquals("only", desc.lastKey());
        assertEquals(42, desc.get("only"));
    }

    @Test
    @DisplayName("descendingMap() – null key is present and first")
    void descendingMapNullKeyPresence() {
        map.put(null, 123);
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        assertTrue(desc.containsKey(null));
        assertEquals(123, desc.get(null));
        Iterator<String> it = desc.keySet().iterator();
        assertNull(it.next(), "Null key must be the first element in descending view");
    }

    @Test
    @DisplayName("descendingMap() – removal via iterator reflects in original map")
    void removalThroughDescendingIterator() {
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        Iterator<Map.Entry<String, Integer>> it = desc.entrySet().iterator();
        assertTrue(it.hasNext());
        // this is the null key entry
        Map.Entry<String, Integer> first = it.next();
        assertNull(first.getKey());
        // Remove the entry via iterator
        it.remove();
        assertFalse(map.containsKey(null), "Original map must no longer contain the null key after removal");
        assertFalse(desc.containsKey(null));
        assertEquals(map.size(), desc.size());
    }

    @Test
    @DisplayName("descendingMap() – size stays consistent after mutations")
    void sizeConsistencyDuringMutations() {
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        assertEquals(map.size(), desc.size());
        // Add via original
        map.put("x", 10);
        assertEquals(map.size(), desc.size());
        // Remove via descending map
        desc.remove("a");
        assertEquals(map.size(), desc.size());
        // Clear via original
        map.clear();
        assertEquals(0, map.size());
        assertEquals(0, desc.size());
    }

    @Test
    @DisplayName("descendingMap() – keySet order is reverse of original")
    void keySetOrderReversal() {
        // Use numeric string keys to have a natural ordering
        ConcurrentNavigableMapNullSafe<String, Integer> numericMap = new ConcurrentNavigableMapNullSafe<>();
        numericMap.put("1", 1);
        numericMap.put("2", 2);
        numericMap.put("3", 3);
        numericMap.put("4", 4);
        List<String> expected = Arrays.asList("4", "3", "2", "1");
        List<String> actual = new ArrayList<>(numericMap.descendingMap().keySet());
        assertEquals(expected, actual, "Key set of descending map must be reverse of the original order");
    }

    @Test
    @DisplayName("descendingMap() – custom comparator is preserved")
    void descendingMapCustomComparatorPreserved() throws Exception {
        Comparator<String> reverse = Comparator.reverseOrder();
        // Construct map with a custom comparator via reflection (no public ctor exposed)
        ConcurrentNavigableMapNullSafe<String, Integer> customMap = new ConcurrentNavigableMapNullSafe<>();
        Field compField = ConcurrentNavigableMapNullSafe.class.getDeclaredField("originalComparator");
        compField.setAccessible(true);
        compField.set(customMap, reverse);
        // Populate map – ordering will be according to reverse comparator
        customMap.put("a", 1);
        customMap.put("b", 2);
        customMap.put("c", 3);
        ConcurrentNavigableMap<String, Integer> desc = customMap.descendingMap();
        assertSame(reverse, desc.comparator(), "The comparator returned by descendingMap() must be the exact instance supplied originally");
    }

    @Test
    @DisplayName("descendingMap() – NPE when internalMap is null")
    void descendingMapNullInternalMapThrowsNPE() throws Exception {
        // Create an instance and then force internalMap to null via reflection
        ConcurrentNavigableMapNullSafe<String, Integer> broken = new ConcurrentNavigableMapNullSafe<>();
        Field internalField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalField.setAccessible(true);
        internalField.set(broken, null);
        assertThrows(NullPointerException.class, broken::descendingMap, "Calling descendingMap() with a null internalMap must throw NPE");
    }

    @Test
    @DisplayName("descendingMap() – ClassCastException when internalMap not ConcurrentNavigableMap")
    void descendingMapWrongInternalMapThrowsClassCast() throws Exception {
        // Replace internalMap with a plain HashMap (does not implement ConcurrentNavigableMap)
        ConcurrentNavigableMapNullSafe<String, Integer> broken = new ConcurrentNavigableMapNullSafe<>();
        Field internalField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalField.setAccessible(true);
        internalField.set(broken, new HashMap<>());
        assertThrows(ClassCastException.class, broken::descendingMap, "A non‑ConcurrentNavigableMap internalMap should cause a ClassCastException");
    }

    @Test
    @DisplayName("descendingMap() – NPE when originalComparator is null and comparator() is accessed")
    void descendingMapNullComparatorThrowsNPEOnAccess() throws Exception {
        // Force originalComparator to null
        ConcurrentNavigableMapNullSafe<String, Integer> mapWithNullComp = new ConcurrentNavigableMapNullSafe<>();
        Field compField = ConcurrentNavigableMapNullSafe.class.getDeclaredField("originalComparator");
        compField.setAccessible(true);
        compField.set(mapWithNullComp, null);
        ConcurrentNavigableMap<String, Integer> desc = mapWithNullComp.descendingMap();
        // The map itself can be created, but calling comparator() should NPE because the wrapper expects a non‑null comparator
        assertThrows(NullPointerException.class, desc::comparator, "Accessing comparator() on a descending map whose originalComparator is null must throw NPE");
    }

    @Test
    @DisplayName("descendingMap() – propagates ConcurrentModificationException from internal map")
    void descendingMapConcurrentModificationDuringCreation() throws Exception {
        // Mock a ConcurrentNavigableMap that throws CME when descendingMap() is invoked
        @SuppressWarnings("unchecked")
        ConcurrentNavigableMap<Object, Object> mockInternal = (ConcurrentNavigableMap<Object, Object>) org.mockito.Mockito.mock(ConcurrentNavigableMap.class);
        org.mockito.Mockito.when(mockInternal.descendingMap()).thenThrow(new ConcurrentModificationException("simulated"));
        // Inject the mock into a wrapper instance
        ConcurrentNavigableMapNullSafe<String, Integer> wrapper = new ConcurrentNavigableMapNullSafe<>();
        Field internalField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalField.setAccessible(true);
        internalField.set(wrapper, mockInternal);
        assertThrows(ConcurrentModificationException.class, wrapper::descendingMap, "If the underlying map throws CME during descendingMap(), it must be propagated");
    }
}
