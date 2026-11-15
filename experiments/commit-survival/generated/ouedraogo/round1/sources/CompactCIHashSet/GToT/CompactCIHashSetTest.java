package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCIHashSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and methods inherited from {@link CompactSet}.</li>
 *   <li>Typical usage scenarios.</li>
 *   <li>Edge‑case behaviour such as case‑insensitivity, handling of {@code null},
 *       duplicate elements, removal of non‑existent elements and iterator contract.</li>
 *   <li>Behaviour when the internal representation switches from the compact form
 *       to the delegated {@code HashSet} after the {@code compactSize()} threshold is passed.</li>
 * </ul>
 *
 * The test class is deliberately self‑contained – it uses only the public API
 * of {@code CompactCIHashSet} and the protected members accessed via reflection
 * (required to read {@code compactSize()} for the “exceed‑compact‑size” tests).
 */
public class CompactCIHashSetTest
{
    private CompactCIHashSet<String> set;

    @Before
    public void setUp()
    {
        set = new CompactCIHashSet<>();
    }

    @After
    public void tearDown()
    {
        set = null;
    }

    /* -------------------------------------------------
     *  Helper – obtain the protected compactSize() value
     * ------------------------------------------------- */
    private int getCompactSize(CompactCIHashSet<?> s)
    {
        try
        {
            java.lang.reflect.Method m = CompactSet.class.getDeclaredMethod("compactSize");
            m.setAccessible(true);
            return (int) m.invoke(s);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to access compactSize()", e);
        }
    }

    /* -------------------------------------------------
     *  1. Constructors
     * ------------------------------------------------- */

    @Test
    public void testNoArgConstructor()
    {
        CompactCIHashSet<Integer> empty = new CompactCIHashSet<>();
        assertTrue("New set must be empty", empty.isEmpty());
        assertEquals("Size of new set must be 0", 0, empty.size());
    }

    @Test
    public void testCollectionConstructor_UniqueElements()
    {
        List<String> list = new ArrayList<>();
        list.add("Alpha");
        list.add("Beta");
        list.add("Gamma");

        CompactCIHashSet<String> fromCollection = new CompactCIHashSet<>(list);
        assertEquals("All unique elements should be present", 3, fromCollection.size());
        assertTrue(fromCollection.contains("Alpha"));
        assertTrue(fromCollection.contains("Beta"));
        assertTrue(fromCollection.contains("Gamma"));
    }

    @Test
    public void testCollectionConstructor_DuplicateCaseInsensitive()
    {
        List<String> list = new ArrayList<>();
        list.add("Delta");
        list.add("delta");   // duplicate when case‑insensitive
        list.add("DELTA");   // another duplicate

        CompactCIHashSet<String> fromCollection = new CompactCIHashSet<>(list);
        assertEquals("Case‑insensitive duplicates must be collapsed", 1, fromCollection.size());
        assertTrue(fromCollection.contains("delta"));
    }

    /* -------------------------------------------------
     *  2. add(E) & contains(Object)
     * ------------------------------------------------- */

    @Test
    public void testAddAndContains_CaseInsensitive()
    {
        assertTrue(set.add("Hello"));
        assertTrue(set.contains("hello"));
        assertFalse(set.add("HELLO")); // duplicate (case‑insensitive) – should not change set
        assertEquals(1, set.size());

        assertTrue(set.add("World"));
        assertTrue(set.contains("WORLD"));
        assertEquals(2, set.size());
    }

    @Test
    public void testAddNull()
    {
        try
        {
            set.add(null);
            // If we reach here, null is allowed – verify behaviour
            assertTrue("Set should contain null after adding it", set.contains(null));
            assertEquals(1, set.size());
        }
        catch (NullPointerException npe)
        {
            // Expected for implementations that forbid nulls
            // Verify that the set is still empty after the exception
            assertTrue(set.isEmpty());
        }
    }

    /* -------------------------------------------------
     *  3. remove(Object)
     * ------------------------------------------------- */

    @Test
    public void testRemoveExistingElement()
    {
        set.add("Apple");
        set.add("Banana");
        assertTrue(set.remove("apple")); // case‑insensitive removal
        assertFalse(set.contains("Apple"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemoveNonExistingElement()
    {
        set.add("Cat");
        assertFalse(set.remove("Dog"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemoveNull()
    {
        // Behaviour depends on whether null is allowed – we test both possibilities
        try
        {
            set.add(null);
            assertTrue(set.remove(null));
            assertFalse(set.contains(null));
            assertTrue(set.isEmpty());
        }
        catch (NullPointerException npe)
        {
            // If null is not permitted, remove(null) should also throw NPE
            try
            {
                set.remove(null);
                fail("Expected NullPointerException when removing null");
            }
            catch (NullPointerException ignored) { /* expected */ }
        }
    }

    /* -------------------------------------------------
     *  4. clear()
     * ------------------------------------------------- */

    @Test
    public void testClear()
    {
        set.add("One");
        set.add("Two");
        set.add("Three");
        assertFalse(set.isEmpty());

        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* -------------------------------------------------
     *  5. iterator() – basic iteration & iterator.remove()
     * ------------------------------------------------- */

    @Test
    public void testIteratorBasic()
    {
        set.add("X");
        set.add("Y");
        set.add("Z");

        List<String> collected = new ArrayList<>();
        for (String s : set)
        {
            collected.add(s);
        }

        assertEquals(3, collected.size());
        assertTrue(collected.containsAll(java.util.Arrays.asList("X", "Y", "Z")));
    }

    @Test
    public void testIteratorRemove()
    {
        set.add("Alpha");
        set.add("Beta");
        set.add("Gamma");

        Iterator<String> it = set.iterator();
        while (it.hasNext())
        {
            String value = it.next();
            if ("beta".equalsIgnoreCase(value))
            {
                it.remove(); // remove via iterator
            }
        }

        assertFalse(set.contains("Beta"));
        assertEquals(2, set.size());
    }

    /* -------------------------------------------------
     *  6. Transition beyond compactSize()
     * ------------------------------------------------- */

    @Test
    public void testExceedCompactSizeBehaviour()
    {
        int compact = getCompactSize(set);
        // add (compact + 2) distinct elements to force the internal switch
        int toAdd = compact + 2;
        for (int i = 0; i < toAdd; i++)
        {
            String element = "elem" + i;
            assertTrue("Element should be added: " + element, set.add(element));
        }

        assertEquals("Set size must match number of distinct elements added", toAdd, set.size());

        // Verify that case‑insensitive duplicate after the switch does NOT increase size
        assertFalse(set.add("ELEM0")); // duplicate of first element (different case)
        assertEquals(toAdd, set.size());

        // Verify that all elements are still present
        for (int i = 0; i < toAdd; i++)
        {
            assertTrue("Set must contain element: elem" + i, set.contains("elem" + i));
        }
    }

    /* -------------------------------------------------
     *  7. isEmpty() and size() consistency
     * ------------------------------------------------- */

    @Test
    public void testIsEmptyAndSizeConsistency()
    {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        set.add("First");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());

        set.remove("first");
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }
}
