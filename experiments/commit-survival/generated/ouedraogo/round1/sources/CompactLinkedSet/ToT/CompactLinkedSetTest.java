package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link CompactLinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (empty and from a collection)</li>
 *   <li>Basic Set operations: add, remove, contains, clear, size, isEmpty</li>
 *   <li>Behaviour when the number of elements exceeds {@code compactSize()}
 *       – the internal delegate should switch to a {@link LinkedHashSet}
 *       preserving insertion order.</li>
 *   <li>Edge cases such as adding {@code null} and duplicate elements.</li>
 * </ul>
 *
 * Only the public API of {@link CompactLinkedSet} and its superclass
 * {@link CompactSet} is used – no mocking, no subclassing and no reliance on
 * internal fields.  The protected {@code compactSize()} method is accessed via
 * reflection solely to determine the threshold for the “compact” vs “linked”
 * behaviour.
 */
public class CompactLinkedSetTest
{
    private CompactLinkedSet<String> set;
    private int compactSize;   // the threshold at which CompactSet switches to a LinkedHashSet

    @Before
    public void setUp() throws Exception
    {
        set = new CompactLinkedSet<>();
        // Obtain the protected compactSize() value via reflection.
        Method compactSizeMethod = CompactSet.class.getDeclaredMethod("compactSize");
        compactSizeMethod.setAccessible(true);
        compactSize = (Integer) compactSizeMethod.invoke(set);
        // Defensive check – the library guarantees a positive compactSize.
        assertTrue("compactSize() must be > 0", compactSize > 0);
    }

    /*** Constructor tests ***/

    @Test
    public void testEmptyConstructorCreatesEmptySet()
    {
        assertTrue("Newly created set should be empty", set.isEmpty());
        assertEquals("Size of newly created set should be 0", 0, set.size());
    }

    @Test
    public void testConstructorWithCollectionCopiesElements()
    {
        List<String> source = new ArrayList<>();
        source.add("alpha");
        source.add("beta");
        source.add("gamma");

        CompactLinkedSet<String> fromCollection = new CompactLinkedSet<>(source);

        assertEquals("Set should contain the same number of elements as the source collection",
                     source.size(), fromCollection.size());

        for (String s : source)
        {
            assertTrue("Set should contain element '" + s + "'", fromCollection.contains(s));
        }

        // Mutating the original collection must not affect the set.
        source.add("delta");
        assertFalse("Set should not contain element added to source after construction",
                    fromCollection.contains("delta"));
    }

    /*** Basic Set operation tests ***/

    @Test
    public void testAddElementsBelowCompactSize()
    {
        for (int i = 0; i < compactSize; i++)
        {
            String element = "elem-" + i;
            assertTrue("Adding a new element should return true", set.add(element));
            assertTrue("Set should contain the newly added element", set.contains(element));
        }
        assertEquals("Set size should equal compactSize after adding that many elements",
                     compactSize, set.size());
    }

    @Test
    public void testAddDuplicateElement()
    {
        assertTrue(set.add("dup"));
        assertFalse("Adding a duplicate element must return false", set.add("dup"));
        assertEquals("Size must not increase when adding duplicate", 1, set.size());
    }

    @Test
    public void testRemoveExistingAndNonExistingElements()
    {
        set.add("first");
        set.add("second");

        assertTrue("Removing an existing element must return true", set.remove("first"));
        assertFalse("Set should no longer contain the removed element", set.contains("first"));
        assertEquals("Size must decrease after removal", 1, set.size());

        assertFalse("Removing a non‑existing element must return false", set.remove("nonexistent"));
        assertEquals("Size must stay unchanged after failed removal", 1, set.size());
    }

    @Test
    public void testContainsAndIsEmpty()
    {
        assertTrue("New set must be empty", set.isEmpty());
        set.add("x");
        assertFalse("Set should not be empty after adding an element", set.isEmpty());
        assertTrue("Set must contain the added element", set.contains("x"));
        assertFalse("Set must not contain an element that was never added", set.contains("y"));
    }

    @Test
    public void testClearEmptiesSet()
    {
        set.add("a");
        set.add("b");
        set.add("c");
        assertFalse("Set should not be empty before clear()", set.isEmpty());

        set.clear();

        assertTrue("Set must be empty after clear()", set.isEmpty());
        assertEquals("Size must be 0 after clear()", 0, set.size());
        assertFalse("Contains must return false for any element after clear()", set.contains("a"));
    }

    @Test
    public void testAddNullElement()
    {
        assertTrue("Adding null should succeed the first time", set.add(null));
        assertTrue("Set must contain null after adding it", set.contains(null));
        assertEquals("Size must be 1 after adding null", 1, set.size());

        // Adding null again is a duplicate
        assertFalse("Adding null a second time must return false", set.add(null));
        assertEquals("Size must remain 1 after duplicate null add", 1, set.size());

        assertTrue("Removing null must succeed", set.remove(null));
        assertFalse("Set must not contain null after removal", set.contains(null));
        assertTrue("Set should be empty after removing the only element", set.isEmpty());
    }

    /*** Tests that verify LinkedHashSet behaviour after exceeding compactSize() ***/

    @Test
    public void testIterationOrderPreservedWhenExceedingCompactSize()
    {
        // Add compactSize elements first – they may be stored in the compact representation.
        for (int i = 0; i < compactSize; i++)
        {
            set.add("c" + i);
        }

        // Now add a few more to force the switch to LinkedHashSet.
        List<String> extra = new ArrayList<>();
        extra.add("extra-1");
        extra.add("extra-2");
        extra.add("extra-3");
        for (String s : extra)
        {
            set.add(s);
        }

        // Build the expected order: insertion order of *all* elements.
        List<String> expectedOrder = new ArrayList<>();
        for (int i = 0; i < compactSize; i++)
        {
            expectedOrder.add("c" + i);
        }
        expectedOrder.addAll(extra);

        // Verify iteration order matches the expected insertion order.
        Iterator<String> it = set.iterator();
        int idx = 0;
        while (it.hasNext())
        {
            String actual = it.next();
            assertEquals("Element at iteration index " + idx + " should match insertion order",
                         expectedOrder.get(idx), actual);
            idx++;
        }
        assertEquals("Iterator must have visited exactly the expected number of elements",
                     expectedOrder.size(), idx);
    }

    @Test
    public void testOrderRemainsConsistentAfterRemovalAndReAddition()
    {
        // Fill up beyond the compact threshold.
        for (int i = 0; i < compactSize + 2; i++)
        {
            set.add("elem-" + i);
        }

        // Remove a middle element.
        assertTrue(set.remove("elem-1"));
        // Re‑add the same element – it should appear at the *end* of iteration order.
        set.add("elem-1");

        // Expected order: all elements except "elem-1" (which is removed) followed by the re‑added one.
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < compactSize + 2; i++)
        {
            if (i != 1) // skip the removed one
                expected.add("elem-" + i);
        }
        expected.add("elem-1"); // re‑added at the end

        // Verify iteration order.
        Iterator<String> it = set.iterator();
        int pos = 0;
        while (it.hasNext())
        {
            assertEquals("Iteration order after removal/re‑addition", expected.get(pos), it.next());
            pos++;
        }
        assertEquals("All expected elements must be visited", expected.size(), pos);
    }

    /*** Helper test to ensure getNewSet() returns a LinkedHashSet with the correct initial capacity ***/
    @Test
    public void testGetNewSetReturnsLinkedHashSetWithCorrectCapacity()
        throws Exception
    {
        // Access the protected getNewSet() method via reflection.
        Method getNewSetMethod = CompactLinkedSet.class.getDeclaredMethod("getNewSet");
        getNewSetMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> newSet = (Set<String>) getNewSetMethod.invoke(set);

        assertTrue("The delegate set returned by getNewSet() must be a LinkedHashSet",
                   newSet instanceof LinkedHashSet);

        // The capacity of a LinkedHashSet is not directly observable, but we can check that it
        // can accept at least compactSize()+1 elements without rehashing (which would be
        // transparent to the test). Adding that many elements should succeed.
        for (int i = 0; i < compactSize + 1; i++)
        {
            assertTrue(newSet.add("tmp-" + i));
        }
        assertEquals("New set should contain compactSize()+1 elements after insertion",
                     compactSize + 1, newSet.size());
    }
}
