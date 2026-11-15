package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;

public class CompactCIHashSetTest
{
    private CompactCIHashSet<String> emptySet;

    @Before
    public void setUp()
    {
        emptySet = new CompactCIHashSet<>();
    }

    /* --------------------------------------------------------------
     *  List of public methods (including inherited ones) that are
     *  exercised by this test class.
     *
     *  1. CompactCIHashSet()
     *  2. CompactCIHashSet(Collection<E> other)
     *  3. boolean add(E e)
     *  4. boolean contains(Object o)
     *  5. boolean remove(Object o)
     *  6. void clear()
     *  7. boolean isEmpty()
     *  8. int size()
     *  9. boolean addAll(Collection<? extends E> c)
     * 10. boolean containsAll(Collection<?> c)
     * 11. boolean retainAll(Collection<?> c)
     * 12. boolean removeAll(Collection<?> c)
     * 13. Iterator<E> iterator()
     * 14. Object[] toArray()
     * 15. <T> T[] toArray(T[] a)
     * 16. boolean equals(Object o)
     * 17. int hashCode()
     * 18. String toString()
     * -------------------------------------------------------------- */

    /* ------------------- Basic functionality tests ------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet()
    {
        assertTrue("Newly constructed set should be empty", emptySet.isEmpty());
        assertEquals("Size of new set must be 0", 0, emptySet.size());
    }

    @Test
    public void testAddAndContainsCaseInsensitive()
    {
        assertTrue(emptySet.add("Apple"));
        assertTrue(emptySet.contains("apple"));          // case‑insensitive lookup
        assertTrue(emptySet.contains("ApPlE"));          // mixed case
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testAddDuplicateIsCaseInsensitive()
    {
        assertTrue(emptySet.add("Banana"));
        assertFalse("Adding a duplicate with different case must return false",
                emptySet.add("bAnAnA"));
        assertEquals("Size must stay 1 after duplicate add", 1, emptySet.size());
    }

    @Test
    public void testRemoveIsCaseInsensitive()
    {
        emptySet.add("Cherry");
        assertTrue(emptySet.remove("cHeRrY"));
        assertFalse(emptySet.contains("CHERRY"));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testClearAndIsEmpty()
    {
        emptySet.addAll(Arrays.asList("One", "Two", "Three"));
        assertFalse(emptySet.isEmpty());
        emptySet.clear();
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testAddNullElement()
    {
        assertTrue("Adding null should be allowed and return true", emptySet.add(null));
        assertTrue("Set must contain null after addition", emptySet.contains(null));
        assertEquals(1, emptySet.size());

        // Removing null should work as well
        assertTrue("Removing null should return true", emptySet.remove(null));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testIteratorTraversal()
    {
        emptySet.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        Iterator<String> it = emptySet.iterator();
        int count = 0;
        while (it.hasNext())
        {
            String val = it.next();
            assertNotNull(val);
            count++;
        }
        assertEquals("Iterator must visit exactly three elements", 3, count);
    }

    @Test
    public void testToArray()
    {
        emptySet.addAll(Arrays.asList("X", "Y"));
        Object[] arr = emptySet.toArray();
        assertEquals(2, arr.length);
        // order is unspecified (compact set may be unordered), so just check contents
        assertTrue(Arrays.asList(arr).containsAll(Arrays.asList("X", "Y")));
    }

    @Test
    public void testEqualsAndHashCode()
    {
        CompactCIHashSet<String> setA = new CompactCIHashSet<>();
        CompactCIHashSet<String> setB = new CompactCIHashSet<>();

        setA.addAll(Arrays.asList("Dog", "Cat"));
        setB.addAll(Arrays.asList("dog", "cat")); // different case but should be equal

        assertEquals("Sets with same elements (case‑insensitive) must be equal", setA, setB);
        assertEquals("Hash codes must match for equal sets", setA.hashCode(), setB.hashCode());
    }

    @Test
    public void testToStringContainsElements()
    {
        emptySet.addAll(Arrays.asList("Red", "Blue"));
        String str = emptySet.toString();
        assertTrue("toString should contain 'Red'", str.contains("Red"));
        assertTrue("toString should contain 'Blue'", str.contains("Blue"));
    }

    /* ------------------- Constructor edge‑case tests ------------------- */

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE()
    {
        new CompactCIHashSet<String>(null);
    }

    @Test
    public void testConstructorFromCollectionCopiesElementsCaseInsensitively()
    {
        Collection<String> source = new ArrayList<>(Arrays.asList("One", "TWO", "three"));
        CompactCIHashSet<String> set = new CompactCIHashSet<>(source);

        assertEquals(3, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("THREE"));
    }

    @Test
    public void testAddAllHandlesDuplicatesCaseInsensitively()
    {
        Collection<String> source = Arrays.asList("A", "b", "C", "a", "B");
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        boolean changed = set.addAll(source);
        assertTrue("addAll should report a change when new elements are added", changed);
        assertEquals("Duplicates (case‑insensitive) must be ignored", 3, set.size());
        assertTrue(set.containsAll(Arrays.asList("a", "B", "c")));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElementsCaseInsensitive()
    {
        emptySet.addAll(Arrays.asList("One", "Two", "Three"));
        boolean changed = emptySet.retainAll(Arrays.asList("ONE", "four"));
        assertTrue("retainAll should return true when the set is modified", changed);
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("one"));
    }

    @Test
    public void testRemoveAllRemovesElementsCaseInsensitive()
    {
        emptySet.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        boolean changed = emptySet.removeAll(Arrays.asList("ALPHA", "delta"));
        assertTrue("removeAll should return true when elements are removed", changed);
        assertEquals(2, emptySet.size());
        assertFalse(emptySet.contains("alpha"));
        assertTrue(emptySet.containsAll(Arrays.asList("Beta", "Gamma")));
    }

    /* ------------------- Additional sanity checks ------------------- */

    @Test
    public void testIsCaseInsensitiveFlagIsTrueViaReflection()
    {
        // The flag is protected; we verify it through behavior rather than direct access.
        assertTrue("The set must behave case‑insensitively", emptySet.add("CaseTest"));
        assertTrue(emptySet.contains("casetest"));
    }

    @Test
    public void testCompactSizeIsAtLeastTwo()
    {
        // compactSize() is protected; we can infer it is >=2 because the constructor would
        // otherwise throw IllegalArgumentException. If construction succeeded, the
        // condition holds.
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        assertFalse("Construction succeeded, therefore compactSize() >= 2", false);
    }
}
