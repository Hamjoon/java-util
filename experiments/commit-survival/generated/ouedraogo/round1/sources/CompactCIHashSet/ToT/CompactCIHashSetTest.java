package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link CompactCIHashSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic Set contract (add, remove, contains, iterator, clear, size, isEmpty)</li>
 *   <li>case‑insensitive behaviour (both look‑up and removal)</li>
 *   <li>transition from the compact internal representation to the delegated {@link CaseInsensitiveSet}
 *       when the number of elements exceeds {@link CompactSet#compactSize()}</li>
 *   <li>visibility of protected API via reflection (getNewSet, isCaseInsensitive, compactSize)</li>
 *   <li>constructor that takes an existing collection</li>
 *   <li>edge cases such as adding {@code null} and duplicate elements</li>
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

    /** Helper to invoke a protected method via reflection. */
    private static Object invokeProtected(Object target, String methodName, Class<?>... paramTypes)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(target);
    }

    /** Helper to obtain the compact size threshold. */
    private int getCompactSize()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        return (int) invokeProtected(set, "compactSize");
    }

    /** Verify that the protected {@code isCaseInsensitive()} reports {@code true}. */
    @Test
    public void testIsCaseInsensitiveViaReflection() throws Exception
    {
        assertTrue((Boolean) invokeProtected(set, "isCaseInsensitive"));
    }

    /** Verify that the protected {@code getNewSet()} returns a {@link CaseInsensitiveSet}. */
    @Test
    public void testGetNewSetReturnsCaseInsensitiveSet() throws Exception
    {
        @SuppressWarnings("unchecked")
        Set<String> newSet = (Set<String>) invokeProtected(set, "getNewSet");
        assertNotNull(newSet);
        assertTrue("Expected a CaseInsensitiveSet instance", newSet instanceof CaseInsensitiveSet);
    }

    /** Basic add / size / isEmpty contract. */
    @Test
    public void testAddAndSize()
    {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        assertTrue(set.add("One"));
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());

        // duplicate element – should not increase size
        assertFalse(set.add("One"));
        assertEquals(1, set.size());
    }

    /** Adding elements with different case should be considered the same element. */
    @Test
    public void testCaseInsensitiveAddAndContains()
    {
        assertTrue(set.add("Apple"));
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("APPLE"));
        assertFalse(set.add("APPLE")); // duplicate because of case‑insensitivity
        assertEquals(1, set.size());
    }

    /** Removing elements is also case‑insensitive. */
    @Test
    public void testCaseInsensitiveRemove()
    {
        set.add("Banana");
        assertTrue(set.contains("BANANA"));
        assertTrue(set.remove("bAnAnA"));
        assertFalse(set.contains("banana"));
        assertEquals(0, set.size());
    }

    /** Iterator returns all elements and supports remove(). */
    @Test
    public void testIteratorBehaviour()
    {
        set.add("One");
        set.add("Two");
        set.add("Three");

        Set<String> seen = new HashSet<>();
        Iterator<String> it = set.iterator();
        while (it.hasNext())
        {
            String v = it.next();
            // iterator must respect case‑insensitivity as well
            assertTrue(set.contains(v));
            seen.add(v.toLowerCase());
            // remove the element via iterator
            it.remove();
        }

        // All elements have been removed
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        // Verify that we saw exactly the three distinct values
        assertEquals(new HashSet<>(Arrays.asList("one", "two", "three")), seen);
    }

    /** clear() empties the set regardless of its current size. */
    @Test
    public void testClear()
    {
        set.add("A");
        set.add("B");
        set.add("C");
        assertFalse(set.isEmpty());

        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // after clear we can add again
        assertTrue(set.add("D"));
        assertEquals(1, set.size());
        assertTrue(set.contains("d"));
    }

    /** Adding {@code null} should behave consistently – either reject or treat as a distinct element.
     *  The contract of {@link CompactSet} is not documented, so we accept either behaviour
     *  but we assert that the set's state is coherent after the operation. */
    @Test
    public void testAddNull()
    {
        try
        {
            boolean added = set.add(null);
            // If null is accepted it must be stored exactly once
            if (added)
            {
                assertTrue(set.contains(null));
                assertEquals(1, set.size());
                // second addition must be rejected
                assertFalse(set.add(null));
                assertEquals(1, set.size());
                // removal must work
                assertTrue(set.remove(null));
                assertFalse(set.contains(null));
                assertEquals(0, set.size());
            }
            else
            {
                // null rejected – set should stay empty
                assertFalse(set.contains(null));
                assertEquals(0, set.size());
            }
        }
        catch (NullPointerException npe)
        {
            // Acceptable alternative: the implementation throws NPE on null
            // Verify that the set remains unchanged
            assertEquals(0, set.size());
        }
    }

    /** Verify that the constructor that takes a collection copies the elements correctly. */
    @Test
    public void testCollectionConstructor()
    {
        List<String> list = Arrays.asList("One", "Two", "Three");
        CompactCIHashSet<String> fromCollection = new CompactCIHashSet<>(list);
        assertEquals(3, fromCollection.size());
        for (String s : list)
        {
            assertTrue(fromCollection.contains(s));
        }
    }

    /** Verify transition from compact to delegate set when size exceeds {@code compactSize()}.
     *  The internal representation is not exposed, so we test observable behaviour:
     *  after crossing the threshold the set must still be case‑insensitive and must
     *  contain all elements that were added before the transition. */
    @Test
    public void testTransitionBeyondCompactSize() throws Exception
    {
        int threshold = getCompactSize(); // the size at which the internal Set changes
        // Fill the set up to the threshold
        for (int i = 0; i < threshold; i++)
        {
            assertTrue(set.add("Key" + i));
        }
        assertEquals(threshold, set.size());

        // At this point the internal delegate is still the compact representation.
        // Add one more element to trigger the transition.
        assertTrue(set.add("Trigger"));
        assertEquals(threshold + 1, set.size());

        // Verify that case‑insensitivity still works after the transition
        assertTrue(set.contains("trigger"));
        assertTrue(set.contains("TRIGGER"));
        assertTrue(set.remove("TrIgGeR"));
        assertEquals(threshold, set.size());

        // Verify that all previously added elements are still present
        for (int i = 0; i < threshold; i++)
        {
            assertTrue("Missing element after transition: Key" + i, set.contains("key" + i));
        }
    }

    /** Ensure that {@code getNewSet(int size)} respects the case‑insensitive flag.
     *  The method is protected; we invoke it via reflection with a size larger than the
     *  compact threshold to force the creation of the delegate {@link CaseInsensitiveSet}. */
    @Test
    public void testGetNewSetWithSizeParameter() throws Exception
    {
        Method m = CompactCIHashSet.class.getDeclaredMethod("getNewSet", int.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> newSet = (Set<String>) m.invoke(set, getCompactSize() + 5);
        assertTrue(newSet instanceof CaseInsensitiveSet);

        // The returned set must be case‑insensitive
        newSet.add("Hello");
        assertTrue(newSet.contains("hello"));
        assertTrue(newSet.contains("HELLO"));
    }

    /** Verify that remove(Object) returns {@code false} when the element is not present. */
    @Test
    public void testRemoveAbsentElement()
    {
        assertFalse(set.remove("nonexistent"));
        set.add("present");
        assertFalse(set.remove("absent"));
    }

    /** Verify that iterator's {@code remove()} throws {@link IllegalStateException}
     *  when called before {@code next()}. */
    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNext()
    {
        set.add("one");
        Iterator<String> it = set.iterator();
        it.remove(); // should throw IllegalStateException
    }

    /** Verify that the iterator's {@code remove()} throws {@link IllegalStateException}
     *  when called twice after a single {@code next()}. */
    @Test
    public void testIteratorRemoveTwice()
    {
        set.add("alpha");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        it.next();
        it.remove(); // first removal OK
        try
        {
            it.remove(); // second removal without intervening next() must fail
            fail("Expected IllegalStateException on second remove()");
        }
        catch (IllegalStateException expected)
        {
            // expected path
        }
    }
}
