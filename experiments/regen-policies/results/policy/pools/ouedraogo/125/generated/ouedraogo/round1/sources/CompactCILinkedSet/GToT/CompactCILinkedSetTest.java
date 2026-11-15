package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCILinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public constructors</li>
 *   <li>All public {@link Set} operations inherited from {@link CompactSet}</li>
 *   <li>Case‑insensitivity behavior</li>
 *   <li>Insertion‑order iteration</li>
 *   <li>Edge‑case handling (null arguments, duplicate elements, empty collections)</li>
 * </ul>
 */
public class CompactCILinkedSetTest
{
    private CompactCILinkedSet<String> emptySet;
    private CompactCILinkedSet<String> populatedSet;

    @Before
    public void setUp()
    {
        emptySet = new CompactCILinkedSet<>();
        populatedSet = new CompactCILinkedSet<>(Arrays.asList("Apple", "Banana", "Cherry"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructors                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNoArgConstructorCreatesEmptySet()
    {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements()
    {
        assertEquals(3, populatedSet.size());
        assertTrue(populatedSet.contains("Apple"));
        assertTrue(populatedSet.contains("Banana"));
        assertTrue(populatedSet.contains("Cherry"));
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNull()
    {
        new CompactCILinkedSet<String>(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Case‑insensitivity                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsAreCaseInsensitive()
    {
        emptySet.add("Apple");
        assertTrue(emptySet.contains("apple"));
        assertTrue(emptySet.contains("APPLE"));
    }

    @Test
    public void testDuplicateAddIsCaseInsensitive()
    {
        emptySet.add("Apple");
        // Adding the same value with different case should not increase size
        assertFalse(emptySet.add("APPLE"));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testRemoveIsCaseInsensitive()
    {
        populatedSet.remove("BANANA"); // different case
        assertFalse(populatedSet.contains("Banana"));
        assertEquals(2, populatedSet.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  Insertion order (linked behaviour)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIterationRespectsInsertionOrder()
    {
        List<String> expected = Arrays.asList("Apple", "Banana", "Cherry");
        List<String> actual = new ArrayList<>();
        for (String s : populatedSet)
        {
            actual.add(s);
        }
        assertEquals(expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  addAll – case‑insensitive handling                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAllCaseInsensitive()
    {
        List<String> more = Arrays.asList("apple", "Date", "ELDERBERRY");
        boolean changed = populatedSet.addAll(more);
        // "apple" is duplicate (different case) → no change for that element
        // "Date" and "ELDERBERRY" are new
        assertTrue(changed);
        assertEquals(5, populatedSet.size());
        assertTrue(populatedSet.containsAll(Arrays.asList("Apple", "Banana", "Cherry", "Date", "ELDERBERRY")));
    }

    @Test
    public void testAddAllReturnsFalseWhenNoNewElements()
    {
        List<String> duplicates = Arrays.asList("apple", "BANANA");
        boolean changed = populatedSet.addAll(duplicates);
        assertFalse(changed);
        assertEquals(3, populatedSet.size());
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllRejectsNullCollection()
    {
        populatedSet.addAll(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Standard Set operations                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testClearEmptiesSet()
    {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    @Test
    public void testContainsAll()
    {
        assertTrue(populatedSet.containsAll(Arrays.asList("apple", "BANANA")));
        assertFalse(populatedSet.containsAll(Arrays.asList("apple", "Fig")));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements()
    {
        boolean changed = populatedSet.retainAll(Arrays.asList("banana", "CHERRY", "fig"));
        assertTrue(changed);
        assertEquals(2, populatedSet.size());
        assertTrue(populatedSet.containsAll(Arrays.asList("Banana", "Cherry")));
        assertFalse(populatedSet.contains("Apple"));
    }

    @Test
    public void testRemoveAllRemovesSpecifiedElements()
    {
        boolean changed = populatedSet.removeAll(Arrays.asList("BANANA", "fig"));
        assertTrue(changed);
        assertEquals(2, populatedSet.size());
        assertFalse(populatedSet.contains("Banana"));
        assertTrue(populatedSet.contains("Apple"));
        assertTrue(populatedSet.contains("Cherry"));
    }

    @Test
    public void testToArray()
    {
        Object[] arr = populatedSet.toArray();
        assertArrayEquals(new Object[]{"Apple", "Banana", "Cherry"}, arr);
    }

    @Test
    public void testToTypedArray()
    {
        String[] arr = populatedSet.toArray(new String[0]);
        assertArrayEquals(new String[]{"Apple", "Banana", "Cherry"}, arr);
    }

    @Test
    public void testEqualsAndHashCode()
    {
        Set<String> other = new LinkedHashSet<>(Arrays.asList("Apple", "Banana", "Cherry"));
        assertEquals(other, populatedSet);
        assertEquals(other.hashCode(), populatedSet.hashCode());
    }

    @Test
    public void testToStringContainsElements()
    {
        String s = populatedSet.toString();
        assertTrue(s.contains("Apple"));
        assertTrue(s.contains("Banana"));
        assertTrue(s.contains("Cherry"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge cases involving null elements (if supported by CompactSet)      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddNullElementIfSupported()
    {
        // CompactSet allows null values – this test verifies the behaviour.
        emptySet.add(null);
        assertTrue(emptySet.contains(null));
        assertEquals(1, emptySet.size());

        // Adding another null should be considered a duplicate
        assertFalse(emptySet.add(null));
        assertEquals(1, emptySet.size());

        // Removal of null works case‑insensitively (trivial for null)
        assertTrue(emptySet.remove(null));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.isEmpty());
    }
}
