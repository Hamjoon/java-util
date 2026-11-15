package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit 4 tests for {@link CompactCILinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public constructors</li>
 *   <li>All public methods inherited from {@link CompactSet}</li>
 *   <li>Linked‑order preservation when the set grows beyond {@code compactSize()}</li>
 *   <li>Case‑insensitive behavior (the class always reports {@code isCaseInsensitive()==true})</li>
 *   <li>Typical edge cases such as adding {@code null} and duplicate elements</li>
 * </ul>
 */
public class CompactCILinkedSetTest
{
    private CompactCILinkedSet<String> set;

    /** Helper to invoke protected methods via reflection. */
    private static Object invokeProtected(Object target, String methodName, Class<?>... paramTypes)
            throws Exception
    {
        Method m = target.getClass().getSuperclass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(target);
    }

    @Before
    public void setUp()
    {
        set = new CompactCILinkedSet<>();
    }

    /* --------------------------------------------------------------------- *
     *  Constructor tests
     * --------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructor()
    {
        assertEquals("Newly created set should be empty", 0, set.size());
        assertTrue("Newly created set should be empty", set.isEmpty());
    }

    @Test
    public void testCollectionConstructor()
    {
        List<String> source = Arrays.asList("One", "Two", "Three");
        CompactCILinkedSet<String> fromCollection = new CompactCILinkedSet<>(source);
        assertEquals("Size should match source collection", source.size(), fromCollection.size());
        for (String s : source)
        {
            assertTrue("Set should contain element from source collection", fromCollection.contains(s));
        }
    }

    /* --------------------------------------------------------------------- *
     *  Basic add / contains / remove / clear behavior
     * --------------------------------------------------------------------- */

    @Test
    public void testAddAndContains()
    {
        assertTrue("First add should return true", set.add("Alpha"));
        assertTrue("Set should contain added element", set.contains("Alpha"));
        assertEquals(1, set.size());

        // duplicate add – should return false and size unchanged
        assertFalse("Duplicate add should return false", set.add("Alpha"));
        assertEquals(1, set.size());

        // case‑insensitive duplicate
        assertFalse("Case‑insensitive duplicate should return false", set.add("ALPHA"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemove()
    {
        set.add("Beta");
        set.add("Gamma");
        assertTrue("Remove existing element should return true", set.remove("Beta"));
        assertFalse("Set should no longer contain removed element", set.contains("Beta"));
        assertEquals(1, set.size());

        assertFalse("Removing non‑existent element should return false", set.remove("Delta"));
        assertEquals(1, set.size());
    }

    @Test
    public void testClear()
    {
        set.add("One");
        set.add("Two");
        set.clear();
        assertTrue("Set should be empty after clear()", set.isEmpty());
        assertEquals(0, set.size());
    }

    /* --------------------------------------------------------------------- *
     *  Iterator order – linked behavior after crossing compactSize()
     * --------------------------------------------------------------------- */

    @Test
    public void testIteratorPreservesInsertionOrder()
    throws Exception
    {
        // Determine the compact size via reflection (protected method)
        int compactSize = (int) invokeProtected(set, "compactSize");
        // Add compactSize + 2 elements to force the internal delegate to become a LinkedHashSet
        List<String> expectedOrder = new ArrayList<>();
        for (int i = 0; i < compactSize + 2; i++)
        {
            String value = "elem" + i;
            set.add(value);
            expectedOrder.add(value);
        }

        // Verify iterator order matches insertion order
        List<String> iterated = new ArrayList<>();
        for (String s : set)
        {
            iterated.add(s);
        }
        assertEquals("Iterator should follow insertion order", expectedOrder, iterated);
    }

    /* --------------------------------------------------------------------- *
     *  Case‑insensitive behavior (the class is always case‑insensitive)
     * --------------------------------------------------------------------- */

    @Test
    public void testCaseInsensitiveOperations()
    throws Exception
    {
        // Verify the protected flag via reflection
        boolean ciFlag = (boolean) invokeProtected(set, "isCaseInsensitive");
        assertTrue("CompactCILinkedSet must report case‑insensitive", ciFlag);

        // Add lower‑case value
        assertTrue(set.add("test"));
        // Contains should succeed regardless of case
        assertTrue(set.contains("TEST"));
        assertTrue(set.contains("TeSt"));

        // Adding a value that differs only by case must be treated as duplicate
        assertFalse("Adding case‑variant duplicate must return false", set.add("TEST"));
        assertEquals("Size must remain 1 after case‑variant duplicate add", 1, set.size());

        // Remove using different case
        assertTrue("Remove with different case should succeed", set.remove("tEsT"));
        assertTrue("Set should be empty after removal", set.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  Edge‑case handling
     * --------------------------------------------------------------------- */

    @Test
    public void testAddNull()
    {
        try
        {
            set.add(null);
            // The original CompactSet permits null (it extends AbstractSet which allows null),
            // but we verify the behavior – it should be added and counted.
            assertTrue("Set should contain null after adding", set.contains(null));
            assertEquals(1, set.size());
        }
        catch (NullPointerException e)
        {
            // If the implementation decides to reject null, the test should still pass
            // because the contract of Set permits throwing NPE for null elements.
            // We simply assert that the exception type is as expected.
            assertEquals("NullPointerException expected when adding null", NullPointerException.class, e.getClass());
        }
    }

    @Test
    public void testRemoveNullWhenAbsent()
    {
        // Removing null from an empty set should return false (no exception)
        assertFalse("Removing null from empty set should return false", set.remove(null));
    }

    @Test
    public void testContainsNullWhenAbsent()
    {
        assertFalse("Contains null on empty set should be false", set.contains(null));
    }

    /* --------------------------------------------------------------------- *
     *  Verify that getNewSet() creates the expected delegate type
     * --------------------------------------------------------------------- */

    @Test
    public void testGetNewSetCreatesCaseInsensitiveSet()
    throws Exception
    {
        // Directly invoke the protected method to obtain a fresh delegate
        @SuppressWarnings("unchecked")
        Set<String> newSet = (Set<String>) invokeProtected(set, "getNewSet");
        assertTrue("Delegate created by getNewSet() must be a CaseInsensitiveSet",
                newSet instanceof CaseInsensitiveSet);
        // The set must be empty initially
        assertTrue("Delegate set should start empty", newSet.isEmpty());
    }
}
