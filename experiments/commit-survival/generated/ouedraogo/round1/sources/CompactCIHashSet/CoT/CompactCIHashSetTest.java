package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * JUnit‑4 test suite for {@link CompactCIHashSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors</li>
 *   <li>All public methods inherited from {@link CompactSet}</li>
 *   <li>Case‑insensitive behaviour that is specific to this subclass</li>
 *   <li>Edge cases such as null arguments, duplicate entries, removal of absent elements,
 *       and the transition from the compact representation to the backing {@code HashSet}
 *       when the number of elements exceeds the internal {@code compactSize} threshold.</li>
 * </ul>
 */
public class CompactCIHashSetTest
{
    private CompactCIHashSet<String> set;

    @Before
    public void setUp()
    {
        set = new CompactCIHashSet<>();
    }

    /* --------------------------------------------------------------------- *
     *  1.  Public constructors
     * --------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructorCreatesEmptySet()
    {
        CompactCIHashSet<Integer> empty = new CompactCIHashSet<>();
        assertEquals("Newly created set must be empty", 0, empty.size());
        assertTrue("Newly created set must report isEmpty()", empty.isEmpty());
    }

    @Test
    public void testConstructorWithCollectionCopiesElements()
    {
        List<String> source = new ArrayList<>();
        source.add("One");
        source.add("Two");
        source.add("three");
        source.add("THREE");      // duplicate with different case – should be ignored

        CompactCIHashSet<String> fromCollection = new CompactCIHashSet<>(source);
        // Because the set is case‑insensitive, "three" and "THREE" count as one entry
        assertEquals("Size should reflect case‑insensitive uniqueness", 3, fromCollection.size());
        assertTrue(fromCollection.contains("ONE"));
        assertTrue(fromCollection.contains("two"));
        assertTrue(fromCollection.contains("Three"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE()
    {
        new CompactCIHashSet<String>((Collection<String>) null);
    }

    /* --------------------------------------------------------------------- *
     *  2.  Basic add / contains / remove / clear operations
     * --------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsCaseInsensitive()
    {
        assertTrue(set.add("Apple"));
        assertTrue("Contains should be case‑insensitive (lower)", set.contains("apple"));
        assertTrue("Contains should be case‑insensitive (upper)", set.contains("APPLE"));
        // Adding a duplicate (different case) must not change the size
        assertFalse(set.add("ApPlE"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemoveExistingElement()
    {
        set.add("Banana");
        assertTrue(set.remove("BANANA"));
        assertFalse(set.contains("banana"));
        assertEquals(0, set.size());
    }

    @Test
    public void testRemoveNonExistingElement()
    {
        set.add("Cherry");
        assertFalse(set.remove("Durian"));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddNullElement()
    {
        // CompactSet (and thus CompactCIHashSet) permits a single null entry.
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());

        // Adding null again must be a no‑op
        assertFalse(set.add(null));
        assertEquals(1, set.size());
    }

    @Test
    public void testClearEmptiesSet()
    {
        set.add("Eagle");
        set.add("Falcon");
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* --------------------------------------------------------------------- *
     *  3.  Iterator behaviour
     * --------------------------------------------------------------------- */

    @Test
    public void testIteratorIteratesAllElements()
    {
        set.add("Giraffe");
        set.add("Hippo");
        set.add("iguana");

        List<String> collected = new ArrayList<>();
        for (Iterator<String> it = set.iterator(); it.hasNext(); )
        {
            collected.add(it.next());
        }

        assertEquals(3, collected.size());
        assertTrue(collected.contains("GIRAFFE"));
        assertTrue(collected.contains("hippo"));
        assertTrue(collected.contains("IGUANA"));
    }

    /* --------------------------------------------------------------------- *
     *  4.  Edge case – transition from compact to HashSet representation
     * --------------------------------------------------------------------- */

    @Test
    public void testTransitionBeyondCompactSizeKeepsCaseInsensitivity()
    {
        // The exact compactSize is an internal detail; we add enough distinct strings
        // to guarantee the threshold is exceeded (the implementation uses a small default,
        // typically 8).  The test verifies that after the transition the set still behaves
        // case‑insensitively.
        int elementsToAdd = 20;
        for (int i = 0; i < elementsToAdd; i++)
        {
            set.add("key" + i);
        }
        assertEquals(elementsToAdd, set.size());

        // Verify case‑insensitive lookup works after the transition
        assertTrue(set.contains("KEY5"));
        assertTrue(set.contains("key10"));
        assertFalse(set.contains("nonexistent"));
    }

    /* --------------------------------------------------------------------- *
     *  5.  Size, isEmpty and contains edge cases
     * --------------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmptyAfterVariousOperations()
    {
        assertTrue(set.isEmpty());
        set.add("Jackal");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());

        set.add("Kangaroo");
        assertEquals(2, set.size());

        set.remove("jackal");
        assertEquals(1, set.size());

        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }
}
