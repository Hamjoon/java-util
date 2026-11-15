package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactLinkedSet}.
 *
 * The tests are organised as follows:
 *
 * 1. Extraction of the public API (constructors + inherited methods).
 * 2. Basic functional tests for each public method.
 * 3. Edge‑case / exception tests (null arguments, duplicate adds, size‑threshold behaviour, etc.).
 *
 * The test class deliberately uses only the constructors, methods and
 * protected helpers that are part of the supplied source code (or its
 * documented super‑class {@code CompactSet}).  No external mocking frameworks
 * or library versions are used.
 */
public class CompactLinkedSetTest
{
    private CompactLinkedSet<String> set;

    @Before
    public void setUp()
    {
        set = new CompactLinkedSet<>();
    }

    /* --------------------------------------------------------------------- */
    /* 1. Public API extraction (for documentation / reference only)          */
    /* --------------------------------------------------------------------- */
    // Constructors
    //   CompactLinkedSet()
    //   CompactLinkedSet(Collection<E> other)

    // Inherited from CompactSet (public)
    //   int size()
    //   boolean isEmpty()
    //   boolean contains(Object item)
    //   Iterator<E> iterator()
    //   boolean add(E item)
    //   boolean remove(Object item)
    //   void clear()

    /* --------------------------------------------------------------------- */
    /* 2. Basic functional tests                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructor()
    {
        CompactLinkedSet<Integer> empty = new CompactLinkedSet<>();
        assertEquals("New set must be empty", 0, empty.size());
        assertTrue("New set must report isEmpty()", empty.isEmpty());
    }

    @Test
    public void testConstructorFromCollection()
    {
        List<String> src = new ArrayList<>();
        src.add("alpha");
        src.add("beta");
        src.add("gamma");

        CompactLinkedSet<String> fromColl = new CompactLinkedSet<>(src);
        assertEquals("Size must match source collection", src.size(), fromColl.size());
        for (String s : src)
        {
            assertTrue("Set must contain element from source collection", fromColl.contains(s));
        }
    }

    @Test
    public void testAddAndContains()
    {
        assertTrue("First add should succeed", set.add("one"));
        assertTrue("Set must contain added element", set.contains("one"));
        assertEquals("Size must be 1 after one addition", 1, set.size());
    }

    @Test
    public void testAddDuplicate()
    {
        assertTrue(set.add("dup"));
        assertFalse("Adding duplicate must return false", set.add("dup"));
        assertEquals("Size must stay 1 after duplicate add", 1, set.size());
    }

    @Test
    public void testRemoveExisting()
    {
        set.add("removeMe");
        assertTrue("remove must return true for existing element", set.remove("removeMe"));
        assertFalse("Set must no longer contain removed element", set.contains("removeMe"));
        assertEquals("Size must be 0 after removal", 0, set.size());
    }

    @Test
    public void testRemoveNonExisting()
    {
        set.add("present");
        assertFalse("remove must return false for missing element", set.remove("absent"));
        assertEquals("Size must stay unchanged after failed remove", 1, set.size());
    }

    @Test
    public void testClear()
    {
        set.add("a");
        set.add("b");
        set.clear();
        assertTrue("Set must be empty after clear()", set.isEmpty());
        assertEquals("Size must be 0 after clear()", 0, set.size());
    }

    @Test
    public void testIteratorOrderBelowCompactSize()
    {
        // Add fewer elements than the compact threshold; order must be preserved.
        for (char c = 'a'; c <= 'd'; c++)   // 4 elements
        {
            set.add(String.valueOf(c));
        }

        Iterator<String> it = set.iterator();
        for (char c = 'a'; c <= 'd'; c++)
        {
            assertTrue("Iterator must have next element", it.hasNext());
            assertEquals("Iterator order must follow insertion order", String.valueOf(c), it.next());
        }
        assertFalse("Iterator must be exhausted", it.hasNext());
    }

    @Test
    public void testIteratorOrderAboveCompactSize()
    {
        int compactSize = getCompactSizeViaReflection(set);
        // Ensure we exceed the compact threshold by at least one element.
        int total = compactSize + 2;
        for (int i = 0; i < total; i++)
        {
            set.add("e" + i);
        }

        Iterator<String> it = set.iterator();
        for (int i = 0; i < total; i++)
        {
            assertTrue("Iterator must have next element after exceeding compact size", it.hasNext());
            assertEquals("Insertion order must be preserved even after switch to LinkedHashSet",
                    "e" + i, it.next());
        }
        assertFalse("Iterator must be exhausted after all elements are visited", it.hasNext());
    }

    @Test
    public void testAddNullElement()
    {
        // CompactSet (and consequently CompactLinkedSet) inherits from AbstractSet,
        // which permits a single null element.
        assertTrue("Adding null should succeed", set.add(null));
        assertTrue("Set must contain null after addition", set.contains(null));
        assertEquals("Size must be 1 after adding null", 1, set.size());

        // Removing null should also work.
        assertTrue("Removing null must succeed", set.remove(null));
        assertFalse("Set must no longer contain null", set.contains(null));
        assertTrue("Set must be empty after removing null", set.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 3. Edge‑case / exception tests                                        */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testConstructorFromNullCollection()
    {
        // The CompactSet constructor forwards the collection argument to super;
        // passing null should result in a NPE as per standard collection contracts.
        new CompactLinkedSet<String>((Collection<String>) null);
    }

    @Test
    public void testRemoveFromEmptySet()
    {
        assertFalse("Removing from an empty set must return false", set.remove("nothing"));
        assertEquals("Size must remain 0", 0, set.size());
    }

    @Test
    public void testIteratorOnEmptySet()
    {
        Iterator<String> it = set.iterator();
        assertFalse("Iterator of empty set must have no next element", it.hasNext());
    }

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                     */
    /* --------------------------------------------------------------------- */

    /**
     * Retrieves the protected {@code compactSize()} value from a {@link CompactLinkedSet}
     * instance via reflection.  This allows the test suite to adapt to whatever
     * default threshold the library chooses without hard‑coding a magic number.
     *
     * @param set the instance whose compact size is required
     * @return the compact size as defined by the underlying {@code CompactSet}
     */
    private int getCompactSizeViaReflection(CompactLinkedSet<?> set)
    {
        try
        {
            Method m = CompactSet.class.getDeclaredMethod("compactSize");
            m.setAccessible(true);
            return (Integer) m.invoke(set);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to obtain compactSize via reflection", e);
        }
    }
}
