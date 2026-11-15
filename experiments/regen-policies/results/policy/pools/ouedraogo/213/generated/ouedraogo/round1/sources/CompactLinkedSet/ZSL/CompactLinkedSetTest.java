package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link CompactLinkedSet}.
 *
 * The tests focus on the public contract of {@link CompactLinkedSet} (and by inheritance
 * {@link CompactSet}) while also exercising the case‑insensitive behaviour that the
 * {@code CompactLinkedSet} constructor configures.
 *
 * All tests use only the public API defined in the supplied source snippets.
 */
public class CompactLinkedSetTest
{
    private CompactLinkedSet<String> emptySet;

    @Before
    public void setUp()
    {
        emptySet = new CompactLinkedSet<>();
    }

    /* --------------------------------------------------------------------- */
    /*  Constructor tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet()
    {
        assertTrue("Newly constructed set should be empty", emptySet.isEmpty());
        assertEquals("Size of new set must be 0", 0, emptySet.size());
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNull()
    {
        new CompactLinkedSet<String>(null);
    }

    @Test
    public void testCollectionConstructorCopiesElementsCaseInsensitively()
    {
        List<String> source = Arrays.asList("One", "Two", "ONE", "tWo");
        CompactLinkedSet<String> set = new CompactLinkedSet<>(source);

        // Duplicates differing only by case must be collapsed
        assertEquals("Duplicate elements (case‑insensitive) should be collapsed", 2, set.size());

        // Insertion order is preserved based on the *first* occurrence of each logical element
        Iterator<String> it = set.iterator();
        assertEquals("First element should be the first distinct value added", "One", it.next());
        assertEquals("Second element should be the second distinct value added", "Two", it.next());
        assertFalse("No more elements expected", it.hasNext());

        // Case‑insensitive containment checks
        assertTrue(set.contains("one"));
        assertTrue(set.contains("TWO"));
    }

    /* --------------------------------------------------------------------- */
    /*  Basic add / contains / remove operations                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsCaseInsensitive()
    {
        assertTrue(emptySet.add("Apple"));
        assertEquals(1, emptySet.size());

        // Same logical element, different case – should not be added again
        assertFalse("Adding duplicate (different case) must return false", emptySet.add("APPLE"));
        assertEquals("Size must remain 1 after duplicate add", 1, emptySet.size());

        // Containment must be case‑insensitive
        assertTrue(emptySet.contains("apple"));
        assertTrue(emptySet.contains("ApPlE"));
    }

    @Test
    public void testAddNullElement()
    {
        // CompactSet permits null values (the base implementation does not forbid it)
        assertTrue(emptySet.add(null));
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains(null));

        // Adding null again should behave like a duplicate
        assertFalse(emptySet.add(null));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testRemoveCaseInsensitive()
    {
        emptySet.add("Banana");
        emptySet.add("Cherry");

        assertTrue(emptySet.remove("BANANA"));
        assertEquals(1, emptySet.size());
        assertFalse(emptySet.contains("banana"));

        // Removing a non‑present element returns false
        assertFalse(emptySet.remove("banana"));
    }

    /* --------------------------------------------------------------------- */
    /*  Bulk operations (addAll, retainAll, removeAll)                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAllCollapsesDuplicates()
    {
        List<String> toAdd = Arrays.asList("Dog", "cat", "DOG", "Cat", "mouse");
        assertTrue(emptySet.addAll(toAdd));
        assertEquals("Duplicates (case‑insensitive) must be collapsed", 3, emptySet.size());

        // Verify insertion order based on first occurrence
        Iterator<String> it = emptySet.iterator();
        assertEquals("Dog", it.next());
        assertEquals("cat", it.next());
        assertEquals("mouse", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements()
    {
        emptySet.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        Collection<String> retain = Arrays.asList("beta", "DELTA");

        assertTrue(emptySet.retainAll(retain));
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("Beta"));
        assertFalse(emptySet.contains("Alpha"));
        assertFalse(emptySet.contains("Gamma"));
    }

    @Test
    public void testRemoveAllEliminatesSpecifiedElements()
    {
        emptySet.addAll(Arrays.asList("Red", "Green", "Blue"));
        Collection<String> toRemove = Arrays.asList("green", "YELLOW");

        assertTrue(emptySet.removeAll(toRemove));
        assertEquals(2, emptySet.size());
        assertFalse(emptySet.contains("Green"));
        assertTrue(emptySet.contains("Red"));
        assertTrue(emptySet.contains("Blue"));
    }

    /* --------------------------------------------------------------------- */
    /*  Miscellaneous Set contract methods                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testClearEmptiesSet()
    {
        emptySet.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(emptySet.isEmpty());

        emptySet.clear();
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testEqualsAndHashCode()
    {
        CompactLinkedSet<String> set1 = new CompactLinkedSet<>();
        CompactLinkedSet<String> set2 = new CompactLinkedSet<>();

        set1.addAll(Arrays.asList("One", "Two"));
        set2.addAll(Arrays.asList("ONE", "TWO"));

        assertEquals("Sets with same logical elements must be equal", set1, set2);
        assertEquals("Equal sets must have identical hashCodes", set1.hashCode(), set2.hashCode());

        // Changing one set breaks equality
        set2.add("Three");
        assertNotEquals(set1, set2);
    }

    @Test
    public void testToStringContainsElements()
    {
        emptySet.addAll(Arrays.asList("x", "y"));
        String s = emptySet.toString();

        // The exact format is not specified, but we expect both elements to appear.
        assertTrue("String representation should contain 'x'", s.contains("x"));
        assertTrue("String representation should contain 'y'", s.contains("y"));
        // Order should follow insertion order
        assertTrue("Insertion order should be reflected in toString()", s.indexOf("x") < s.indexOf("y"));
    }

    @Test
    public void testContainsAll()
    {
        emptySet.addAll(Arrays.asList("a", "b", "c"));
        Collection<String> superset = Arrays.asList("A", "B", "C", "D");
        Collection<String> subset = Arrays.asList("a", "c");

        assertTrue(emptySet.containsAll(superset) == false);
        assertTrue(emptySet.containsAll(subset));
    }

    @Test
    public void testIteratorOrderPreservesInsertion()
    {
        List<String> values = Arrays.asList("first", "second", "third");
        emptySet.addAll(values);

        List<String> iterated = new ArrayList<>();
        for (String s : emptySet)
        {
            iterated.add(s);
        }

        assertEquals("Iterator must preserve insertion order", values, iterated);
    }
}
