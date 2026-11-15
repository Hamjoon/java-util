package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import java.lang.reflect.Field;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_descendingSet_Test {

    @Test
    void descendingSetUsesNaturalOrderAndReversesElements() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        // natural ordering – null is treated as the smallest element
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        // Ascending iterator (already verified in existing tests) would be:
        // apple, banana, cherry, null
        NavigableSet<String> descending = set.descendingSet();
        // The descending view must contain the same elements
        assertEquals(4, descending.size());
        assertTrue(descending.containsAll(set));
        // Verify order is the exact reverse of the natural order
        Iterator<String> it = descending.iterator();
        // smallest element first in descending view
        assertEquals(null, it.next());
        assertEquals("cherry", it.next());
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingSetPreservesCustomComparatorAndReversesOrder() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 6
        assertTrue(set.add("banana"));
        // length 0
        assertTrue(set.add(null));
        // Ascending order according to the comparator: null, kiwi, apple, banana
        NavigableSet<String> descending = set.descendingSet();
        // The comparator of the descending set must be the same instance as the original
        assertSame(lengthComparator, descending.comparator());
        // Verify reversed order
        Iterator<String> it = descending.iterator();
        // longest first
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertEquals("kiwi", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingSetReflectsMutationsFromBothSides() {
        NavigableSet<Integer> original = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 3; i++) {
            original.add(i);
        }
        NavigableSet<Integer> descending = original.descendingSet();
        // Add via descending view
        assertTrue(descending.add(0));
        assertTrue(original.contains(0));
        assertEquals(4, original.size());
        // Remove via original set and ensure descending view updates
        assertTrue(original.remove(2));
        assertFalse(descending.contains(2));
        assertEquals(3, descending.size());
        // Verify final ordering of descending view: 3,1,0
        Iterator<Integer> it = descending.iterator();
        assertEquals(3, it.next());
        assertEquals(1, it.next());
        assertEquals(0, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingSetOfEmptySetIsEmptyAndHasSameComparator() {
        NavigableSet<String> empty = new ConcurrentNavigableSetNullSafe<>();
        NavigableSet<String> descending = empty.descendingSet();
        assertTrue(descending.isEmpty());
        assertSame(empty.comparator(), descending.comparator());
    }

    /**
     * Helper to obtain the private {@code internalSet} field via reflection.
     */
    @SuppressWarnings("unchecked")
    private static NavigableSet<Object> getInternalSet(ConcurrentNavigableSetNullSafe<?> wrapper) {
        try {
            Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
            f.setAccessible(true);
            return (NavigableSet<Object>) f.get(wrapper);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to access internalSet via reflection", e);
        }
    }

    @Test
    void descendingSetReturnsDistinctWrapperButSameUnderlyingSet() {
        NavigableSet<Integer> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> d1 = original.descendingSet();
        NavigableSet<Integer> d2 = original.descendingSet();
        // Distinct wrapper objects
        assertNotSame(d1, d2);
        // Both wrappers must reference the SAME underlying descending internal set
        NavigableSet<Object> internal1 = getInternalSet((ConcurrentNavigableSetNullSafe<Integer>) d1);
        NavigableSet<Object> internal2 = getInternalSet((ConcurrentNavigableSetNullSafe<Integer>) d2);
        assertSame(internal1, internal2);
    }

    @Test
    void descendingSetWithNonNullComparatorRejectsNullAdd() {
        // Comparator that does NOT handle null (natural order on Strings)
        Comparator<String> natural = Comparator.naturalOrder();
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(natural);
        // valid
        set.add("a");
        NavigableSet<String> descending = set.descendingSet();
        // Adding null should throw NPE, mirroring ConcurrentSkipListSet behavior
        Executable addNull = () -> descending.add(null);
        assertThrows(NullPointerException.class, addNull);
    }

    @Test
    void descendingSetFirstAndLastAreSwapped() {
        NavigableSet<Integer> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList(10, 20, 30, 40));
        NavigableSet<Integer> descending = original.descendingSet();
        // In descending view, first() should be original.last()
        assertEquals(original.last(), descending.first());
        // and last() should be original.first()
        assertEquals(original.first(), descending.last());
    }

    @Test
    void descendingSetPreservesCustomComparatorInstance() {
        Comparator<String> reverseAlpha = Comparator.reverseOrder();
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>(reverseAlpha);
        original.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> descending = original.descendingSet();
        // The comparator must be exactly the same instance supplied originally
        assertSame(reverseAlpha, descending.comparator());
    }

    @Test
    void descendingSetSubSetRespectsReversedOrdering() {
        NavigableSet<Integer> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList(1, 2, 3, 4, 5));
        NavigableSet<Integer> descending = original.descendingSet();
        // subSet(fromElement, fromInclusive, toElement, toInclusive) in descending view
        // expects fromElement > toElement according to original ordering.
        NavigableSet<Integer> sub = descending.subSet(4, true, 2, false);
        // Expected elements: 4 and 3 (in descending order)
        List<Integer> expected = Arrays.asList(4, 3);
        assertEquals(expected, new ArrayList<>(sub));
    }

    @Test
    void descendingSetHeadSetAndTailSetAreInverted() {
        NavigableSet<Integer> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList(1, 2, 3, 4, 5));
        NavigableSet<Integer> descending = original.descendingSet();
        // In descending view, headSet(toElement) should behave like original.tailSet(toElement)
        // includes 3
        NavigableSet<Integer> head = descending.headSet(3, true);
        // 3,4,5
        NavigableSet<Integer> expectedHead = original.tailSet(3, true);
        // Both sets must contain the same elements (order is descending)
        assertEquals(new ArrayList<>(expectedHead).size(), head.size());
        assertEquals(new ArrayList<>(expectedHead), new ArrayList<>(head.descendingSet()));
        // Similarly, tailSet(fromElement) should behave like original.headSet(fromElement)
        // elements <4 in original
        NavigableSet<Integer> tail = descending.tailSet(4, false);
        // 1,2,3
        NavigableSet<Integer> expectedTail = original.headSet(4, false);
        assertEquals(new ArrayList<>(expectedTail).size(), tail.size());
        assertEquals(new ArrayList<>(expectedTail), new ArrayList<>(tail.descendingSet()));
    }

    @Test
    void descendingSetReflectsConcurrentModifications() throws Exception {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> descending = original.descendingSet();
        // Perform modifications in another thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            // add via descending view
            descending.add("d");
            // remove via original view
            original.remove("b");
        });
        // wait for modifications to complete
        future.get();
        executor.shutdown();
        // Expected contents after modifications: a, c, d (b removed)
        // descending order
        List<String> expected = Arrays.asList("d", "c", "a");
        assertEquals(expected, new ArrayList<>(descending));
    }
}
