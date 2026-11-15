package com.cedarsoftware.util;

import java.util.*;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CompactMap_values_Test {

    private CompactMap<String, Integer> map;

//     @BeforeEach
    void setupBeforeEach() {
        // Use the builder to obtain a default CompactMap instance.
        // The default compactSize is 50 (>=2) so the constructor will succeed.
        map = CompactMap.<String, Integer>builder().build();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared after each test to avoid cross‑test contamination.
        if (map != null) {
            map.clear();
        }
    }

//     @Test
//     void valuesCollectionReflectsMapContents() {
//         map.put("one", 1);
//         map.put("two", 2);
//         map.put("three", 3);
//         Collection<Integer> values = map.values();
//         assertEquals(map.size(), values.size());
//         assertTrue(values.contains(1));
//         assertTrue(values.contains(2));
//         assertTrue(values.contains(3));
//         map.put("four", 4);
//         assertTrue(values.contains(4));
//         assertEquals(4, values.size());
//     }

//     @Test
//     void valuesIteratorRemoveUpdatesMap() {
//         map.put("a", 10);
//         map.put("b", 20);
//         map.put("c", 30);
//         Collection<Integer> values = map.values();
//         Iterator<Integer> it = values.iterator();
//         assertTrue(it.hasNext());
//         Integer first = it.next();
//         it.remove();
//         assertFalse(map.containsValue(first));
//         assertEquals(2, map.size());
//         assertEquals(2, values.size());
//     }

//     @Test
//     void valuesIteratorRemoveWithoutNextThrows() {
//         map.put("x", 100);
//         Collection<Integer> values = map.values();
//         Iterator<Integer> it = values.iterator();
//         try {
//             it.remove();
//             fail("Expected IllegalStateException");
//         } catch (IllegalStateException ignored) {
//             // expected
//         }
//     }

//     @Test
//     void valuesClearEmptiesMap() {
//         map.put("k1", 1);
//         map.put("k2", 2);
//         map.put("k3", 3);
//         Collection<Integer> values = map.values();
//         assertFalse(values.isEmpty());
//         values.clear();
//         assertTrue(values.isEmpty());
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @Test
//     void valuesIteratorExhaustionThrowsNoSuchElementException() {
//         map.put("only", 42);
//         Iterator<Integer> it = map.values().iterator();
//         assertTrue(it.hasNext());
//         assertEquals(Integer.valueOf(42), it.next());
//         assertFalse(it.hasNext());
//         try {
//             it.next();
//             fail("Expected NoSuchElementException");
//         } catch (NoSuchElementException ignored) {
//             // expected
//         }
//     }

//     @Test
//     @DisplayName("values().size() returns 0 when map is empty")
//     void valuesSizeEmptyMap() {
//         assertEquals(0, map.values().size());
//     }

//     @Test
//     @DisplayName("values().size() matches map size after three insertions")
//     void valuesSizeAfterThreeEntries() {
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         assertEquals(3, map.values().size());
//     }

//     @Test
//     @DisplayName("values().containsAll(Collection) returns true for exact match")
//     void valuesContainsAllExactMatch() {
//         map.put("x", 10);
//         map.put("y", 20);
//         map.put("z", 30);
//         Collection<Integer> expected = Arrays.asList(10, 20, 30);
//         assertTrue(map.values().containsAll(expected));
//     }

//     @Test
//     @DisplayName("values().containsAll(Collection) returns false when collection has extra value")
//     void valuesContainsAllWithExtra() {
//         map.put("x", 10);
//         map.put("y", 20);
//         Collection<Integer> superset = Arrays.asList(10, 20, 999);
//         assertFalse(map.values().containsAll(superset));
//     }

//     @Test
//     @DisplayName("values().isEmpty() reflects map state")
//     void valuesIsEmptyReflectsMapState() {
//         assertTrue(map.values().isEmpty());
//         map.put("k", 1);
//         assertFalse(map.values().isEmpty());
//         map.clear();
//         assertTrue(map.values().isEmpty());
//     }

//     @Test
//     @DisplayName("ConcurrentModificationException when map modified after iterator creation")
//     void iteratorDetectsConcurrentModification() {
//         map.put("a", 1);
//         map.put("b", 2);
//         Iterator<Integer> it = map.values().iterator();
//         // structural change outside iterator
//         map.put("c", 3);
//         try {
//             it.next();
//             fail("Expected ConcurrentModificationException");
//         } catch (ConcurrentModificationException ignored) {
//             // expected
//         }
//     }

//     @Test
//     @DisplayName("Iterator returns values in insertion order (default ordering)")
//     void iteratorRespectsInsertionOrder() {
//         // Default builder creates insertion‑ordered map
//         map.put("first", 100);
//         map.put("second", 200);
//         map.put("third", 300);
//         List<Integer> iterated = new ArrayList<>();
//         for (Integer v : map.values()) {
//             iterated.add(v);
//         }
//         List<Integer> expected = Arrays.asList(100, 200, 300);
//         assertEquals(expected, iterated);
//     }

//     @Test
//     @DisplayName("values().toArray().length equals map size")
//     void valuesToArrayLengthMatchesSize() {
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         Object[] array = map.values().toArray();
//         assertEquals(map.size(), array.length);
//     }

//     @Test
//     @DisplayName("values().removeAll(Collection) clears the map when all values are supplied")
//     void valuesRemoveAllClearsMap() {
//         map.put("a", 10);
//         map.put("b", 20);
//         map.put("c", 30);
//         Collection<Integer> allValues = new ArrayList<>(map.values());
//         boolean changed = map.values().removeAll(allValues);
//         assertTrue(changed);
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.values().size());
//     }

//     @Test
//     @DisplayName("values().add(V) throws UnsupportedOperationException")
//     void valuesAddUnsupported() {
//         try {
//             map.values().add(123);
//             fail("Expected UnsupportedOperationException");
//         } catch (UnsupportedOperationException ignored) {
//             // expected
//         }
//     }

//     @Test
//     @DisplayName("values().addAll(Collection) throws UnsupportedOperationException")
//     void valuesAddAllUnsupported() {
//         try {
//             map.values().addAll(Arrays.asList(1, 2, 3));
//             fail("Expected UnsupportedOperationException");
//         } catch (UnsupportedOperationException ignored) {
//             // expected
//         }
//     }

//     @Test
//     @DisplayName("values().remove(Object) throws UnsupportedOperationException")
//     void valuesRemoveObjectUnsupported() {
//         map.put("k", 1);
//         try {
//             map.values().remove(1);
//             fail("Expected UnsupportedOperationException");
//         } catch (UnsupportedOperationException ignored) {
//             // expected
//         }
//     }

//     @Test
//     @DisplayName("values().retainAll(Collection) throws UnsupportedOperationException")
//     void valuesRetainAllUnsupported() {
//         map.put("a", 1);
//         map.put("b", 2);
//         try {
//             map.values().retainAll(Collections.singleton(1));
//             fail("Expected UnsupportedOperationException");
//         } catch (UnsupportedOperationException ignored) {
//             // expected
//         }
//     }
}