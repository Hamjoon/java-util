package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class CompactMap_values_Test {

    /**
     * The map under test – created fresh for each test method.
     */
    private CompactMap<String, Integer> map;

    /**
     * Create a map with a tiny compact size so we can exercise the
     *  different internal representations without needing many entries.
     */
    @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected int compactSize() {
                // small size to trigger state changes quickly
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // use a LinkedHashMap to keep insertion order predictable for the tests
                return new LinkedHashMap<>();
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /**
     * Verify that the {@code values()} collection reflects the current map contents.
     */
    @Test
    void testValuesReflectMapContents() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Collection<Integer> values = map.values();
        assertEquals(3, values.size(), "Values collection size must match map size");
        assertTrue(values.containsAll(List.of(1, 2, 3)), "Values collection must contain all map values");
        assertFalse(values.contains(99), "Values collection must not contain values that are not in the map");
    }

    /**
     * The iterator returned by the values collection must iterate over all values.
     */
    @Test
    void testValuesIterator() {
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);
        Collection<Integer> values = map.values();
        Iterator<Integer> it = values.iterator();
        Set<Integer> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(Set.of(10, 20, 30), seen, "Iterator must visit each value exactly once");
    }

    /**
     * Calling {@code clear()} on the values collection must clear the underlying map.
     */
    @Test
    void testValuesClearEmptiesMap() {
        map.put("one", 1);
        map.put("two", 2);
        Collection<Integer> values = map.values();
        assertFalse(map.isEmpty(), "Map should not be empty before clear");
        values.clear();
        assertTrue(map.isEmpty(), "Map must be empty after clearing the values collection");
        assertEquals(0, values.size(), "Values collection size must be zero after clear");
    }

    /**
     * Removing an element via the iterator must remove the corresponding entry from the map.
     */
    @Test
    void testIteratorRemoveUpdatesMap() {
        map.put("first", 100);
        map.put("second", 200);
        map.put("third", 300);
        Collection<Integer> values = map.values();
        Iterator<Integer> it = values.iterator();
        // Remove the second element encountered (value 200)
        while (it.hasNext()) {
            Integer v = it.next();
            if (v == 200) {
                it.remove();
                break;
            }
        }
        assertEquals(2, map.size(), "Map size must shrink after iterator.remove()");
        assertFalse(map.containsValue(200), "Removed value must no longer be present in the map");
        assertTrue(map.containsKey("first"));
        assertTrue(map.containsKey("third"));
    }

    /**
     * The values collection does not support addition – it should throw {@link UnsupportedOperationException}.
     */
    @Test
    void testValuesAddUnsupported() {
        map.put("k", 42);
        Collection<Integer> values = map.values();
        assertThrows(UnsupportedOperationException.class, () -> values.add(99), "Adding directly to the values collection must not be supported");
    }

    /**
     * The values collection does not support {@code addAll} – it should throw {@link UnsupportedOperationException}.
     */
    @Test
    void testValuesAddAllUnsupported() {
        map.put("k", 42);
        Collection<Integer> values = map.values();
        assertThrows(UnsupportedOperationException.class, () -> values.addAll(List.of(1, 2, 3)), "addAll on the values collection must not be supported");
    }

    /**
     * Empty map – values() should be empty and iterator.hasNext() must be false.
     */
    @Test
    void testValuesEmptyMap() {
        Collection<Integer> values = map.values();
        assertEquals(0, values.size(), "Empty map must report values size 0");
        assertFalse(values.iterator().hasNext(), "Iterator on empty values must have no next element");
    }

    /**
     * After inserting three entries, values().size() must be 3 and iterator must yield exactly those values.
     */
    @Test
    void testValuesSizeAfterThreeInsertions() {
        map.put("k1", 10);
        map.put("k2", 20);
        map.put("k3", 30);
        Collection<Integer> values = map.values();
        assertEquals(3, values.size(), "Values size must be 3 after three insertions");
        List<Integer> iterated = new ArrayList<>();
        values.iterator().forEachRemaining(iterated::add);
        assertTrue(iterated.containsAll(List.of(10, 20, 30)), "Iterator must yield exactly the three inserted values (order not significant)");
        assertEquals(3, iterated.size(), "Iterator must not produce duplicate values");
    }

    /**
     * Values collection must reflect removal performed directly on the map.
     */
    @Test
    void testValuesReflectRemoval() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // remove one entry
        map.remove("b");
        Collection<Integer> values = map.values();
        assertEquals(2, values.size(), "Values size must shrink after map removal");
        assertTrue(values.containsAll(List.of(1, 3)));
        assertFalse(values.contains(2));
    }

    /**
     * Values collection must be live – additions after retrieval are visible.
     */
    @Test
    void testValuesReflectAdditionAfterRetrieval() {
        map.put("x", 100);
        map.put("y", 200);
        // obtain view before third entry
        Collection<Integer> values = map.values();
        // add after view obtained
        map.put("z", 300);
        assertEquals(3, values.size(), "Values view must reflect new entry added after retrieval");
        assertTrue(values.contains(300));
    }

    /**
     * Iterator must detect concurrent modification caused by a put operation.
     */
    @Test
    void testValuesIteratorConcurrentModificationPut() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<Integer> it = map.values().iterator();
        // structural modification after iterator creation
        map.put("c", 3);
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must throw CME after map is modified via put");
    }

    /**
     * Iterator must detect concurrent modification caused by a remove operation.
     */
    @Test
    void testValuesIteratorConcurrentModificationRemove() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<Integer> it = map.values().iterator();
        // structural modification after iterator creation
        map.remove("a");
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must throw CME after map is modified via remove");
    }

    /**
     * Calling next() on an iterator from an empty map must throw NoSuchElementException.
     */
    @Test
    void testValuesIteratorNoSuchElementOnEmpty() {
        Iterator<Integer> it = map.values().iterator();
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on empty iterator must throw NoSuchElementException");
    }

    /**
     * After consuming the sole element, a second next() must throw NoSuchElementException.
     */
    @Test
    void testValuesIteratorNoSuchElementAfterSingle() {
        map.put("only", 42);
        Iterator<Integer> it = map.values().iterator();
        assertEquals(42, it.next());
        assertThrows(NoSuchElementException.class, it::next, "Second next() on single‑element iterator must throw NoSuchElementException");
    }

    /**
     * Values collection must respect insertion ordering when the map is built with that option.
     */
    @Test
    void testValuesInsertionOrder() {
        CompactMap<String, Integer> orderedMap = CompactMap.<String, Integer>builder().insertionOrder().build();
        orderedMap.put("A", 1);
        orderedMap.put("B", 2);
        orderedMap.put("C", 3);
        List<Integer> iterated = new ArrayList<>();
        orderedMap.values().iterator().forEachRemaining(iterated::add);
        assertEquals(List.of(1, 2, 3), iterated, "Values iterator must follow insertion order when map is configured for insertion ordering");
    }

    /**
     * Values collection must respect reverse ordering when the map is built with that option.
     */
    @Test
    void testValuesReverseOrder() {
        CompactMap<String, Integer> reverseMap = CompactMap.<String, Integer>builder().reverseOrder().build();
        reverseMap.put("A", 1);
        reverseMap.put("B", 2);
        reverseMap.put("C", 3);
        List<Integer> iterated = new ArrayList<>();
        reverseMap.values().iterator().forEachRemaining(iterated::add);
        assertEquals(List.of(3, 2, 1), iterated, "Values iterator must follow reverse order when map is configured for reverse ordering");
    }

    /**
     * Values view works correctly when the map has grown beyond the default compact size.
     */
    @Test
    void testValuesLargeCompactedMap() {
        // Use the default builder (compactSize = 50) and insert 120 entries
        CompactMap<Integer, Integer> largeMap = CompactMap.<Integer, Integer>builder().build();
        // Populate 120 entries
        IntStream.rangeClosed(1, 120).forEach(i -> largeMap.put(i, i * 10));
        Collection<Integer> values = largeMap.values();
        assertEquals(120, values.size(), "Values size must match number of inserted entries (120)");
        // Verify that the iterator traverses all values without missing any
        Set<Integer> seen = values.iterator().hasNext() ? values.stream().collect(Collectors.toSet()) : Collections.emptySet();
        Set<Integer> expected = IntStream.rangeClosed(1, 120).mapToObj(i -> i * 10).collect(Collectors.toSet());
        assertEquals(expected, seen, "Iterator must visit every value in the large map");
    }
}
