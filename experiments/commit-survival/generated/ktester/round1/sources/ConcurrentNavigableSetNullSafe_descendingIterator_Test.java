package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_descendingIterator_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor (natural ordering, null is treated as the greatest element).
        set = new ConcurrentNavigableSetNullSafe<>();
        // Populate the set with a mixture of regular elements and a null.
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared between tests.
        set.clear();
    }

    private static void setInternalSet(ConcurrentNavigableSetNullSafe<?> target, NavigableSet<Object> internal) throws Exception {
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(target, internal);
    }

    private static NavigableSet<Object> getInternalSet(ConcurrentNavigableSetNullSafe<?> target) throws Exception {
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        NavigableSet<Object> set = (NavigableSet<Object>) f.get(target);
        return set;
    }

    @Test
    void testDescendingIteratorOrder() {
        Iterator<String> descending = set.descendingIterator();
        // According to the implementation, null is the greatest element, so it appears first.
        assertTrue(descending.hasNext());
        // null (greatest)
        assertNull(descending.next());
        assertTrue(descending.hasNext());
        // next greatest
        assertEquals("cherry", descending.next());
        assertTrue(descending.hasNext());
        assertEquals("banana", descending.next());
        assertTrue(descending.hasNext());
        assertEquals("apple", descending.next());
        assertFalse(descending.hasNext());
    }

    @Test
    void testDescendingIteratorRemove() {
        Iterator<String> descending = set.descendingIterator();
        // Remove the first element returned by the descending iterator (null).
        assertTrue(descending.hasNext());
        assertNull(descending.next());
        descending.remove();
        // Verify that the set no longer contains null but still contains the other elements.
        assertFalse(set.contains(null));
        assertEquals(3, set.size());
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains("cherry"));
        // Continue iteration to ensure the iterator remains functional after a remove.
        assertTrue(descending.hasNext());
        assertEquals("cherry", descending.next());
        // remove "cherry"
        descending.remove();
        assertFalse(set.contains("cherry"));
        assertEquals(2, set.size());
    }

    @Test
    void testDescendingIteratorOnEmptySet() {
        // Create an empty set and obtain its descending iterator.
        NavigableSet<Integer> emptySet = new ConcurrentNavigableSetNullSafe<>();
        Iterator<Integer> descending = emptySet.descendingIterator();
        assertFalse(descending.hasNext());
        assertThrows(java.util.NoSuchElementException.class, descending::next);
    }

    @Test
    void testMultipleDescendingIteratorsAreIndependent() {
        Iterator<String> it1 = set.descendingIterator();
        Iterator<String> it2 = set.descendingIterator();
        // Advance the first iterator once.
        assertTrue(it1.hasNext());
        // null
        assertNull(it1.next());
        // The second iterator should still start at the beginning (null).
        assertTrue(it2.hasNext());
        assertNull(it2.next());
        // Advance the first iterator again.
        assertTrue(it1.hasNext());
        assertEquals("cherry", it1.next());
        // The second iterator should still be at its second element.
        assertTrue(it2.hasNext());
        assertEquals("cherry", it2.next());
    }

    @Test
    void descendingIterator_emptyInternalSet_behaviour() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        setInternalSet(set, new ConcurrentSkipListSet<>());
        Iterator<String> it = set.descendingIterator();
        assertFalse(it.hasNext(), "Iterator should report no elements");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    void descendingIterator_reverseOrderWithNull_unmaskWorks() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("a");
        set.add(null);
        set.add("c");
        Iterator<String> it = set.descendingIterator();
        assertTrue(it.hasNext());
        assertEquals("c", it.next());
        assertTrue(it.hasNext());
        assertNull(it.next(), "The masked null must be unmasked to null");
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingIterator_removeDeletesElement() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("x");
        set.add("y");
        set.add("z");
        Iterator<String> it = set.descendingIterator();
        assertEquals("z", it.next());
        it.remove();
        assertFalse(set.contains("z"));
        assertEquals(2, set.size());
        assertEquals("y", it.next());
        it.remove();
        assertFalse(set.contains("y"));
        assertEquals(1, set.size());
        assertEquals("x", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingIterator_doubleRemove_throwsIllegalStateException() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("only");
        Iterator<String> it = set.descendingIterator();
        assertEquals("only", it.next());
        it.remove();
        assertThrows(IllegalStateException.class, it::remove, "Second consecutive remove must throw IllegalStateException");
    }

    @Test
    void descendingIterator_concurrentModification_seesNewElement() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("a");
        set.add("b");
        Iterator<String> it = set.descendingIterator();
        assertEquals("b", it.next());
        set.add("z");
        boolean sawZ = false;
        while (it.hasNext()) {
            String val = it.next();
            if ("z".equals(val)) {
                sawZ = true;
                break;
            }
        }
        assertTrue(sawZ, "Iterator should eventually see the concurrently added element 'z'");
    }

    @Test
    void descendingIterator_singleNullElement_behaviour() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(null);
        Iterator<String> it = set.descendingIterator();
        assertTrue(it.hasNext());
        assertNull(it.next(), "The only element should be null");
        it.remove();
        assertFalse(set.contains(null));
        assertEquals(0, set.size());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingIterator_twoIteratorsIndependent() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("x", "y", "z"));
        Iterator<String> it1 = set.descendingIterator();
        Iterator<String> it2 = set.descendingIterator();
        assertEquals("z", it1.next());
        assertEquals("z", it2.next());
        assertEquals("y", it1.next());
        assertEquals("y", it2.next());
    }

    @Test
    void descendingIterator_externalRemoval_skipsRemovedElement() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = set.descendingIterator();
        assertEquals("c", it.next());
        set.remove("b");
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingIterator_removeAfterExhaustion_throwsIllegalStateException() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("1", "2"));
        Iterator<String> it = set.descendingIterator();
        while (it.hasNext()) {
            it.next();
        }
        assertFalse(it.hasNext());
        assertThrows(IllegalStateException.class, it::remove, "remove() after iterator is exhausted must throw IllegalStateException");
    }

    @Test
    void descendingIterator_respectsCustomComparator() throws Exception {
        Comparator<String> reverse = Comparator.<String>reverseOrder();
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(reverse);
        set.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = set.descendingIterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingIterator_internalSetNull_throwsNPE() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        setInternalSet(set, null);
        assertThrows(NullPointerException.class, set::descendingIterator, "Calling descendingIterator when internalSet is null must throw NPE");
    }

    @Test
    void descendingIterator_internalSetDescendingIteratorThrows_UOE() throws Exception {
        ???;
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        setInternalSet(set, faultySet);
        assertThrows(UnsupportedOperationException.class, set::descendingIterator, "descendingIterator should propagate UnsupportedOperationException from internalSet");
    }

    @Test
    void descendingIterator_nextClassCastException_dueToWrongElementType() throws Exception {
        ConcurrentSkipListSet<Object> internal = new ConcurrentSkipListSet<>();
        internal.add(42);
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        setInternalSet(set, internal);
        Iterator<String> it = set.descendingIterator();
        assertThrows(ClassCastException.class, it::next, "next() must throw ClassCastException when element type does not match generic type");
    }

    @Test
    void descendingIterator_removeUnsupportedOperationException_propagates() throws Exception {
        Iterator<Object> badIter = new Iterator<>() {

            private boolean hasReturned = false;

            @Override
            public boolean hasNext() {
                return !hasReturned;
            }

            @Override
            public Object next() {
                hasReturned = true;
                return "elem";
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        ???;
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        setInternalSet(set, customSet);
        Iterator<String> it = set.descendingIterator();
        assertEquals("elem", it.next());
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void descendingIterator_internalSetReturnsNull_NPEOnUse() throws Exception {
        ???;
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        setInternalSet(set, nullIterSet);
        Iterator<String> it = set.descendingIterator();
        assertThrows(NullPointerException.class, it::hasNext, "hasNext() should throw NPE because underlying iterator is null");
        assertThrows(NullPointerException.class, it::next);
    }
}
