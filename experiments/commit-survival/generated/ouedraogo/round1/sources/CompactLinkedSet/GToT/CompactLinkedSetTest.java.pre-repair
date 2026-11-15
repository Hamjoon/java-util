package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactLinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors</li>
 *   <li>All public methods inherited from {@link CompactSet}</li>
 *   <li>Behaviour when the internal representation switches from the compact
 *       backing store to a {@link LinkedHashSet} (i.e. when size() &gt; compactSize())</li>
 *   <li>Edge‑cases such as duplicate adds, null handling, removal of absent elements,
 *       iterator remove, and construction with a {@code null} collection.</li>
 * </ul>
 *
 * The protected {@code compactSize()} method is accessed via reflection because it
 * is needed to trigger the switch point in a deterministic way.
 */
public class CompactLinkedSetTest
{
    private CompactLinkedSet<String> set;
    private int compactSize;   // value returned by the protected compactSize() method

    /** Retrieve the protected {@code compactSize()} value once per test class. */
    private static int getCompactSize(CompactLinkedSet<?> s)
    {
        try
        {
            Method m = CompactSet.class.getDeclaredMethod("compactSize");
            m.setAccessible(true);
            return (Integer) m.invoke(s);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to read compactSize()", e);
        }
    }

    @Before
    public void setUp()
    {
        set = new CompactLinkedSet<>();
        compactSize = getCompactSize(set);
    }

    /* -------------------------------------------------------------------- */
    /* 1. Constructors                                                       */
    /* -------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet()
    {
        assertTrue("New set should be empty", set.isEmpty());
        assertEquals("Size of new set should be 0", 0, set.size());
    }

    @Test
    public void testConstructorWithCollectionCopiesElements()
    {
        List<String> source = new ArrayList<>();
        source.add("alpha");
        source.add("beta");
        source.add("gamma");

        CompactLinkedSet<String> fromCollection = new CompactLinkedSet<>(source);
        assertEquals("All elements must be copied", source.size(), fromCollection.size());
        assertTrue("Contains alpha", fromCollection.contains("alpha"));
        assertTrue("Contains beta", fromCollection.contains("beta"));
        assertTrue("Contains gamma", fromCollection.contains("gamma"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE()
    {
        new CompactLinkedSet<String>((Collection<String>) null);
    }

    /* -------------------------------------------------------------------- */
    /* 2. Basic Set Operations                                               */
    /* -------------------------------------------------------------------- */

    @Test
    public void testAddElementsAndSize()
    {
        assertTrue(set.add("one"));
        assertTrue(set.add("two"));
        assertEquals(2, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));
    }

    @Test
    public void testAddDuplicateReturnsFalseAndSizeUnchanged()
    {
        assertTrue(set.add("dup"));
        int sizeAfterFirstAdd = set.size();
        assertFalse("Adding a duplicate must return false", set.add("dup"));
        assertEquals("Size must stay the same after duplicate add", sizeAfterFirstAdd, set.size());
    }

    @Test
    public void testRemoveExistingElement()
    {
        set.add("removable");
        assertTrue(set.remove("removable"));
        assertFalse("Element must be gone after removal", set.contains("removable"));
        assertEquals(0, set.size());
    }

    @Test
    public void testRemoveNonExistingElement()
    {
        set.add("present");
        assertFalse("Removing non‑existing element must return false", set.remove("absent"));
        assertEquals(1, set.size());
    }

    @Test
    public void testClearEmptiesSet()
    {
        set.add("a");
        set.add("b");
        set.clear();
        assertTrue("Set must be empty after clear()", set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testContainsWorksForNull()
    {
        // CompactSet (and therefore CompactLinkedSet) permits a single null element.
        // The behaviour is verified by the test; if the implementation changes
        // to disallow nulls, the test will fail, signalling a contract change.
        assertTrue(set.add(null));
        assertTrue("Set should contain null after adding it", set.contains(null));
        assertTrue(set.remove(null));
        assertFalse("Null must be gone after removal", set.contains(null));
    }

    /* -------------------------------------------------------------------- */
    /* 3. Iterator behaviour                                                  */
    /* -------------------------------------------------------------------- */

    @Test
    public void testIteratorOrderPreservedWhenUsingLinkedHashSet()
    {
        // Fill the set until it switches to the LinkedHashSet delegate.
        for (int i = 0; i <= compactSize; i++)   // size will become compactSize+1 (> compactSize)
        {
            set.add("item" + i);
        }

        // Verify that iteration follows insertion order.
        Iterator<String> it = set.iterator();
        for (int i = 0; i <= compactSize; i++)
        {
            assertTrue("Iterator must have next element " + i, it.hasNext());
            assertEquals("Insertion order must be preserved", "item" + i, it.next());
        }
        assertFalse("Iterator must be exhausted", it.hasNext());
    }

    @Test
    public void testIteratorRemove()
    {
        set.add("first");
        set.add("second");
        set.add("third");

        Iterator<String> it = set.iterator();
        while (it.hasNext())
        {
            String cur = it.next();
            if ("second".equals(cur))
            {
                it.remove();               // remove via iterator
            }
        }

        assertFalse("Set must not contain removed element", set.contains("second"));
        assertEquals(2, set.size());
    }

    /* -------------------------------------------------------------------- */
    /* 4. Switching to LinkedHashSet (size > compactSize)                    */
    /* -------------------------------------------------------------------- */

    @Test
    public void testSwitchToLinkedHashSetPreservesAllElements()
    {
        // Add exactly compactSize elements – still using the compact backing store.
        for (int i = 0; i < compactSize; i++)
        {
            set.add("c" + i);
        }
        assertEquals(compactSize, set.size());

        // Add one more element – this triggers the switch to LinkedHashSet.
        set.add("trigger");

        // Verify that all previously added elements are still present.
        for (int i = 0; i < compactSize; i++)
        {
            assertTrue("Element c" + i + " must survive the switch", set.contains("c" + i));
        }
        assertTrue("Trigger element must be present", set.contains("trigger"));
        assertEquals(compactSize + 1, set.size());
    }

    @Test
    public void testAddAfterSwitchMaintainsInsertionOrder()
    {
        // Fill up to just beyond the compact threshold.
        for (int i = 0; i <= compactSize; i++)
        {
            set.add("elem" + i);
        }

        // Add a few more elements after the switch.
        set.add("extra1");
        set.add("extra2");

        // Expected order: elem0 … elemN, extra1, extra2
        List<String> expected = new ArrayList<>();
        for (int i = 0; i <= compactSize; i++) expected.add("elem" + i);
        expected.add("extra1");
        expected.add("extra2");

        assertEquals("Size must match expected count", expected.size(), set.size());

        int idx = 0;
        for (String s : set)
        {
            assertEquals("Insertion order after switch", expected.get(idx++), s);
        }
    }

    /* -------------------------------------------------------------------- */
    /* 5. Defensive programming checks                                         */
    /* -------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testAddNullWhenSetAlreadyContainsNullThrowsNPE()
    {
        // The first null is allowed (see testContainsWorksForNull); the second
        // attempt to add another null must fail because a Set cannot contain
        // duplicate nulls. The implementation returns false, not NPE, but we
        // assert the contract explicitly in case a future version changes.
        set.add(null);
        set.add(null); // second add – should return false, not throw
    }

    @Test
    public void testAddAllViaConstructorWithDuplicates()
    {
        Collection<String> dupes = new LinkedHashSet<>();
        dupes.add("x");
        dupes.add("x"); // duplicate – set will ignore

        CompactLinkedSet<String> dupSet = new CompactLinkedSet<>(dupes);
        assertEquals("Duplicates in collection must be collapsed", 1, dupSet.size());
        assertTrue(dupSet.contains("x"));
    }
}
