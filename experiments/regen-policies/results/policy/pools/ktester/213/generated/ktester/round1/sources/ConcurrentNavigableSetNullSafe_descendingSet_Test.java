package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentSkipListSet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_descendingSet_Test {

    @Test
    void testDescendingSetWithDefaultConstructor() {
        // Arrange – create a set with natural ordering and a null element
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(original.add("apple"));
        assertTrue(original.add("banana"));
        assertTrue(original.add("cherry"));
        assertTrue(original.add(null));
        // Act – obtain the descending view
        NavigableSet<String> descending = original.descendingSet();
        // Assert – same size and same elements
        assertEquals(original.size(), descending.size());
        assertTrue(descending.containsAll(original));
        // The iterator of the descending set should traverse in reverse order
        Iterator<String> it = descending.iterator();
        // original last element
        assertEquals(null, it.next());
        // then "cherry"
        assertEquals("cherry", it.next());
        // then "banana"
        assertEquals("banana", it.next());
        // finally "apple"
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
        // Mutating the original set must be reflected in the descending view
        original.remove("banana");
        assertFalse(descending.contains("banana"));
        assertEquals(3, descending.size());
        // Adding through the descending view must affect the original set
        descending.add("date");
        assertTrue(original.contains("date"));
        assertEquals(4, original.size());
    }

    @Test
    void testDescendingSetWithComparatorConstructor() {
        // Arrange – comparator based on string length (null is treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        assertTrue(original.add("kiwi"));
        // length 5
        assertTrue(original.add("apple"));
        // length 6
        assertTrue(original.add("banana"));
        // length 0
        assertTrue(original.add(null));
        // Act – descending view
        NavigableSet<String> descending = original.descendingSet();
        // Assert – order is the reverse of the length‑based order
        Iterator<String> it = descending.iterator();
        // longest first
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertEquals("kiwi", it.next());
        // null (length 0) last
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
        // Verify that the comparator of the descending set is the reverse of the original
        Comparator<? super String> descendingComp = descending.comparator();
        // The descending comparator should order longer strings before shorter ones
        assertTrue(descendingComp.compare("banana", "apple") < 0);
        assertTrue(descendingComp.compare(null, "kiwi") > 0);
    }

    @Test
    void testDescendingSetReflectsConcurrentModifications() {
        // Arrange – a set with a range of integers
        NavigableSet<Integer> original = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 5; i++) {
            original.add(i);
        }
        NavigableSet<Integer> descending = original.descendingSet();
        // Act – remove the smallest element via the original set
        original.remove(1);
        // Act – add a new element via the descending view
        descending.add(0);
        // Assert – both views see the same content
        List<Integer> expected = Arrays.asList(5, 4, 3, 2, 0);
        assertEquals(expected, new ArrayList<>(descending));
        assertEquals(5, original.size());
        assertTrue(original.containsAll(expected));
    }

    // ---------- natural ordering ----------
    @Test
    void descendingSet_NaturalOrdering_EmptySet() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        NavigableSet<String> descending = original.descendingSet();
        assertTrue(descending.isEmpty(), "Descending view of an empty set must also be empty");
        assertFalse(descending.iterator().hasNext(), "Iterator of empty descending set must have no elements");
    }

    @Test
    void descendingSet_NaturalOrdering_ReverseIteration() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> descending = original.descendingSet();
        List<String> expected = Arrays.asList("c", "b", "a");
        List<String> actual = new ArrayList<>();
        descending.forEach(actual::add);
        assertEquals(expected, actual, "Iterator should traverse elements in reverse natural order");
    }

    @Test
    void descendingSet_NaturalOrdering_ContainsCheck() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> descending = original.descendingSet();
        assertTrue(descending.contains("b"), "Descending view must delegate contains correctly");
    }

    // ---------- custom comparator ----------
    @Test
    void descendingSet_CustomComparator_ReverseOrderUnderlying() {
        // Underlying set uses reverse order comparator
        NavigableSet<String> backing = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        ConcurrentNavigableSetNullSafe<String> original = new ConcurrentNavigableSetNullSafe<>(backing, null);
        // stored as c,b,a internally
        original.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> descending = original.descendingSet();
        // Descending view should present natural order: a,b,c
        List<String> expected = Arrays.asList("a", "b", "c");
        List<String> actual = new ArrayList<>(descending);
        assertEquals(expected, actual, "Descending view of reverse‑ordered set should yield natural order");
    }

    @Test
    void descendingSet_CustomComparator_ContainsCheck() {
        NavigableSet<String> backing = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
        ConcurrentNavigableSetNullSafe<String> original = new ConcurrentNavigableSetNullSafe<>(backing, null);
        original.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> descending = original.descendingSet();
        assertTrue(descending.contains("b"), "Contains must work on descending view with custom comparator");
    }

    // ---------- modification reflection ----------
    @Test
    void descendingSet_Modifications_AddThroughDescending() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> descending = original.descendingSet();
        descending.add("d");
        assertTrue(original.contains("d"), "Adding via descending view must affect original set");
    }

    @Test
    void descendingSet_Modifications_RemoveThroughDescending() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList("a", "b"));
        NavigableSet<String> descending = original.descendingSet();
        descending.remove("a");
        assertEquals(1, original.size(), "Removal via descending view must be reflected in original set");
        assertFalse(original.contains("a"));
    }

    // ---------- multiple nesting ----------
    @Test
    void descendingSet_MultipleNesting_DoubleDescendingRestoresOrder() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> doubleDesc = original.descendingSet().descendingSet();
        List<String> expected = Arrays.asList("a", "b", "c");
        assertEquals(expected, new ArrayList<>(doubleDesc), "Double descending should restore original order");
    }

    @Test
    void descendingSet_MultipleNesting_EqualityCheck() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> doubleDesc = original.descendingSet().descendingSet();
        assertTrue(doubleDesc.containsAll(original), "Double descending view must contain all elements of original");
        assertTrue(original.containsAll(doubleDesc), "Original must contain all elements of double descending view");
    }

    // ---------- single element ----------
    @Test
    void descendingSet_SingleElement_Iteration() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.add("x");
        NavigableSet<String> descending = original.descendingSet();
        Iterator<String> it = descending.iterator();
        assertTrue(it.hasNext());
        assertEquals("x", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingSet_SingleElement_Contains() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.add("x");
        assertTrue(original.descendingSet().contains("x"));
    }

    // ---------- empty set with custom comparator ----------
    @Test
    void descendingSet_EmptySet_CustomComparator() {
        Comparator<String> lengthComp = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>(lengthComp);
        NavigableSet<String> descending = original.descendingSet();
        assertTrue(descending.isEmpty(), "Descending view of empty set with custom comparator must be empty");
        assertFalse(descending.iterator().hasNext(), "Iterator must have no elements");
    }

    // ---------- null internal set ----------
    @Test
    void descendingSet_NullInternalSet_ThrowsNPE() throws Exception {
        // Use reflection to invoke the package‑private constructor with a null internal set
        Constructor<ConcurrentNavigableSetNullSafe> ctor = ConcurrentNavigableSetNullSafe.class.getDeclaredConstructor(NavigableSet.class, Comparator.class);
        ctor.setAccessible(true);
        ConcurrentNavigableSetNullSafe<String> instance = ctor.newInstance(null, null);
        assertThrows(NullPointerException.class, instance::descendingSet, "descendingSet() should throw NPE when internalSet is null");
    }

    // ---------- underlying set does not support descendingSet ----------
    @Test
    void descendingSet_UnderlyingSetUnsupported_ThrowsUnsupportedOperationException() throws Exception {
        // Mock a NavigableSet that throws on descendingSet()
        @SuppressWarnings("unchecked")
        NavigableSet<Object> badSet = mock(NavigableSet.class);
        when(badSet.descendingSet()).thenThrow(new UnsupportedOperationException("descending not supported"));
        // Build a ConcurrentNavigableSetNullSafe with the mocked set via reflection
        Constructor<ConcurrentNavigableSetNullSafe> ctor = ConcurrentNavigableSetNullSafe.class.getDeclaredConstructor(NavigableSet.class, Comparator.class);
        ctor.setAccessible(true);
        ConcurrentNavigableSetNullSafe<String> instance = ctor.newInstance(badSet, null);
        assertThrows(UnsupportedOperationException.class, instance::descendingSet, "descendingSet() must propagate UnsupportedOperationException from the underlying set");
    }
}
