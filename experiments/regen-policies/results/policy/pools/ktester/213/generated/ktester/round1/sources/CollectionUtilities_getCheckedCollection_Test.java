package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_getCheckedCollection_Test {

    /* --------------------------------------------------------------
     *  Supertype Type Compatibility
     * -------------------------------------------------------------- */
    @Nested
    class SupertypeCompatibilityTests {

        @Test
        void getCheckedCollection_listWithSupertype_NumberAcceptsDouble() {
            List<Integer> original = new ArrayList<>(Arrays.asList(1, 2, 3));
            // Request a checked view for Number (supertype of Integer)
            Collection<Number> checked = CollectionUtilities.getCheckedCollection(original, Number.class);
            assertTrue(checked instanceof List, "Expected a List view");
            // Adding a Double (subclass of Number) must succeed
            checked.add(4.5);
            assertEquals(4, checked.size());
            assertTrue(checked.contains(4.5));
        }

        @Test
        void getCheckedCollection_setWithSupertype_ObjectAcceptsString() {
            Set<Integer> original = new HashSet<>(Arrays.asList(10, 20));
            // Request a checked view for Object (top of hierarchy)
            Collection<Object> checked = CollectionUtilities.getCheckedCollection(original, Object.class);
            assertTrue(checked instanceof Set, "Expected a Set view");
            // Adding a completely unrelated type (String) must succeed
            checked.add("test");
            assertEquals(3, checked.size());
            assertTrue(checked.contains("test"));
        }
    }

    /* --------------------------------------------------------------
     *  Null‑argument validation
     * -------------------------------------------------------------- */
    @Test
    void getCheckedCollection_nullCollection_throwsNPE() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.getCheckedCollection(null, Integer.class));
        assertEquals("Collection must not be null", ex.getMessage());
    }

    @Test
    void getCheckedCollection_nullType_throwsNPE() {
        List<String> list = new ArrayList<>();
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.getCheckedCollection(list, null));
        assertEquals("Type (Class) must not be null", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  NavigableSet – most specific wrapper
     * -------------------------------------------------------------- */
    @Test
    void getCheckedCollection_navigableSet_returnsCheckedNavigableSet() {
        NavigableSet<Integer> original = new TreeSet<>(Arrays.asList(1, 2, 3));
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(original, Integer.class);
        // The returned collection should still be a NavigableSet
        assertTrue(checked instanceof NavigableSet, "Expected a NavigableSet view");
        NavigableSet<Integer> ns = (NavigableSet<Integer>) checked;
        // Adding a correct type works
        ns.add(4);
        assertEquals(4, ns.size());
        // Adding a wrong type (via raw type) must throw ClassCastException
        @SuppressWarnings("unchecked")
        Collection raw = ns;
        assertThrows(ClassCastException.class, () -> raw.add("not an integer"));
    }

    /* --------------------------------------------------------------
     *  SortedSet – when not a NavigableSet
     * -------------------------------------------------------------- */
    @Test
    void getCheckedCollection_sortedSet_returnsCheckedSortedSet() {
        SortedSet<String> original = new TreeSet<>(Arrays.asList("x", "y"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(original, String.class);
        assertTrue(checked instanceof SortedSet, "Expected a SortedSet view");
        SortedSet<String> ss = (SortedSet<String>) checked;
        ss.add("z");
        assertEquals(3, ss.size());
        @SuppressWarnings("unchecked")
        Collection raw = ss;
        assertThrows(ClassCastException.class, () -> raw.add(42));
    }

    /* --------------------------------------------------------------
     *  Set – when not SortedSet/NavigableSet
     * -------------------------------------------------------------- */
    @Test
    void getCheckedCollection_set_returnsCheckedSet() {
        Set<String> original = new HashSet<>(Arrays.asList("one", "two"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(original, String.class);
        assertTrue(checked instanceof Set, "Expected a Set view");
        Set<String> set = (Set<String>) checked;
        set.add("three");
        assertEquals(3, set.size());
        @SuppressWarnings("unchecked")
        Collection raw = set;
        assertThrows(ClassCastException.class, () -> raw.add(new Object()));
    }

    /* --------------------------------------------------------------
     *  List – when not a Set
     * -------------------------------------------------------------- */
    @Test
    void getCheckedCollection_list_returnsCheckedList() {
        List<String> original = new ArrayList<>(Arrays.asList("alpha", "beta"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(original, String.class);
        assertTrue(checked instanceof List, "Expected a List view");
        List<String> list = (List<String>) checked;
        list.add("gamma");
        assertEquals(3, list.size());
        @SuppressWarnings("unchecked")
        Collection raw = list;
        assertThrows(ClassCastException.class, () -> raw.add(Boolean.TRUE));
    }

    /* --------------------------------------------------------------
     *  Generic Collection – when none of the above interfaces match
     * -------------------------------------------------------------- */
    @Test
    void getCheckedCollection_genericCollection_returnsCheckedCollection() {
        Collection<String> original = new LinkedList<>(Arrays.asList("first", "second"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(original, String.class);
        // The view is still a Collection but not any of the more specific interfaces
        assertFalse(checked instanceof List, "Should not be a List view");
        assertFalse(checked instanceof Set, "Should not be a Set view");
        assertFalse(checked instanceof SortedSet, "Should not be a SortedSet view");
        assertFalse(checked instanceof NavigableSet, "Should not be a NavigableSet view");
        // Adding a correct element works
        checked.add("third");
        assertEquals(3, checked.size());
        // Adding a wrong element type throws ClassCastException
        @SuppressWarnings("unchecked")
        Collection raw = checked;
        assertThrows(ClassCastException.class, () -> raw.add(1.23));
    }

    /* --------------------------------------------------------------
     *  Verify that the original collection instance is *not* modified
     *  when the checked view rejects an illegal element.
     * -------------------------------------------------------------- */
    @Test
    void getCheckedCollection_illegalAddDoesNotAlterOriginal() {
        List<String> original = new ArrayList<>(Collections.singletonList("a"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(original, String.class);
        @SuppressWarnings("unchecked")
        Collection raw = checked;
        assertThrows(ClassCastException.class, () -> raw.add(10));
        // original list must still contain only the original element
        assertEquals(1, original.size());
        assertEquals("a", original.get(0));
    }

    /* --------------------------------------------------------------
     *  SortedSet – when not a NavigableSet
     * -------------------------------------------------------------- */
    @Test
    void getCheckedCollection_sortedSet_nonNavigable_returnsCheckedSortedSet() {
        // TreeSet implements NavigableSet, so we wrap it with an unmodifiable view that
        // implements SortedSet but NOT NavigableSet.
        SortedSet<String> original = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList("a", "b")));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(original, String.class);
        assertTrue(checked instanceof SortedSet, "Expected a SortedSet view");
        assertFalse(checked instanceof NavigableSet, "Should NOT be a NavigableSet view");
        SortedSet<String> ss = (SortedSet<String>) checked;
        ss.add("c");
        assertEquals(3, ss.size());
        @SuppressWarnings("unchecked")
        Collection raw = ss;
        assertThrows(ClassCastException.class, () -> raw.add(123));
    }

    /* --------------------------------------------------------------
     *  Branch Precedence – declared SortedSet but runtime NavigableSet
     * -------------------------------------------------------------- */
    @Test
    void getCheckedCollection_sortedSetDeclaredButNavigableInstance_returnsNavigableSet() {
        // Declare as SortedSet but instantiate a TreeSet (which implements NavigableSet)
        SortedSet<Integer> sortedSet = new TreeSet<>(Arrays.asList(1, 2, 3));
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(sortedSet, Integer.class);
        // The most specific branch is NavigableSet
        assertTrue(checked instanceof NavigableSet, "Expected a NavigableSet view due to runtime type");
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> ns = (NavigableSet<Integer>) checked;
        // Wrong type must still be rejected
        @SuppressWarnings("unchecked")
        Collection raw = ns;
        assertThrows(ClassCastException.class, () -> raw.add("bad"));
    }
}
