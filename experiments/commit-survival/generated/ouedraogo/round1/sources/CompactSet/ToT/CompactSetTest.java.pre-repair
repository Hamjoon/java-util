package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (empty and from a collection)</li>
 *   <li>Basic API (size, isEmpty, clear)</li>
 *   <li>add – normal add, duplicate add, transition from array to Set</li>
 *   <li>remove – removal from array representation, removal from Set representation,
 *                transition back from Set to array</li>
 *   <li>contains – including case‑insensitive mode and {@code null} handling</li>
 *   <li>Iterator – normal iteration, {@code remove()} before {@code next()},
 *                  iterator‑based removal</li>
 *   <li>Protected hooks – custom {@code compactSize()}, {@code isCaseInsensitive()},
 *                         and validation of illegal {@code compactSize()}</li>
 * </ul>
 *
 * No mocking frameworks are used; all objects are created via the public API or
 * via a small concrete subclass that exposes the protected methods for testing.
 */
public class CompactSetTest
{
    /* --------------------------------------------------------------------- */
    /* Helper subclass that allows us to control the protected configuration */
    /* --------------------------------------------------------------------- */
    private static class TestCompactSet<E> extends CompactSet<E>
    {
        private final int compactSize;
        private final boolean caseInsensitive;

        TestCompactSet()
        {
            this(80, false);
        }

        TestCompactSet(int compactSize, boolean caseInsensitive)
        {
            this.compactSize = compactSize;
            this.caseInsensitive = caseInsensitive;
        }

        @Override
        protected int compactSize()
        {
            return compactSize;
        }

        @Override
        protected boolean isCaseInsensitive()
        {
            return caseInsensitive;
        }
    }

    private TestCompactSet<String> set;

    @Before
    public void setUp()
    {
        set = new TestCompactSet<>();
    }

    /* --------------------------------------------------------------------- */
    /* Construction tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructor()
    {
        CompactSet<Integer> empty = new CompactSet<>();
        assertTrue("Newly created set should be empty", empty.isEmpty());
        assertEquals("Size of a new set must be 0", 0, empty.size());
    }

    @Test
    public void testConstructorFromCollection()
    {
        List<String> src = Arrays.asList("a", "b", "c");
        CompactSet<String> fromColl = new CompactSet<>(src);
        assertEquals("Size must match collection size", src.size(), fromColl.size());
        for (String s : src)
        {
            assertTrue("Set must contain element from collection: " + s, fromColl.contains(s));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalCompactSize()
    {
        // compactSize < 2 must cause an exception in the default constructor
        new TestCompactSet<>(1, false);
    }

    /* --------------------------------------------------------------------- */
    /* Basic API tests                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testClear()
    {
        set.add("x");
        set.add("y");
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* --------------------------------------------------------------------- */
    /* Add operation tests                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddSingleElement()
    {
        assertTrue(set.add("first"));
        assertTrue(set.contains("first"));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddDuplicateReturnsFalse()
    {
        assertTrue(set.add("dup"));
        assertFalse("Adding a duplicate must return false", set.add("dup"));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddNullElement()
    {
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());

        // duplicate null should be rejected
        assertFalse(set.add(null));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddTriggersTransitionToSet()
    throws Exception
    {
        // Fill the set up to the compactSize (default 80)
        for (int i = 0; i < 80; i++)
        {
            assertTrue(set.add("e" + i));
        }
        assertEquals(80, set.size());
        // Verify internal representation is still an array
        assertTrue(isInternalArray(set));

        // Adding the 81st element should switch the internal storage to a Set
        assertTrue(set.add("overflow"));
        assertEquals(81, set.size());
        assertTrue(isInternalSet(set));
    }

    /* --------------------------------------------------------------------- */
    /* Remove operation tests                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRemoveFromArrayRepresentation()
    {
        set.add("one");
        set.add("two");
        assertEquals(2, set.size());

        assertTrue(set.remove("one"));
        assertFalse(set.contains("one"));
        assertEquals(1, set.size());

        // Removing the last element should leave the set empty
        assertTrue(set.remove("two"));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRemoveFromSetAndTransitionBackToArray()
    throws Exception
    {
        // Build a set just over the threshold to force Set storage
        for (int i = 0; i < 81; i++)
        {
            set.add("e" + i);
        }
        assertTrue(isInternalSet(set));
        assertEquals(81, set.size());

        // Remove elements until we hit compactSize (80)
        assertTrue(set.remove("e0"));
        assertEquals(80, set.size());

        // After removal the internal representation must have switched back to array
        assertTrue(isInternalArray(set));
    }

    @Test
    public void testRemoveNonExistingElement()
    {
        set.add("present");
        assertFalse(set.remove("absent"));
        assertEquals(1, set.size());
    }

    /* --------------------------------------------------------------------- */
    /* contains tests (including case‑insensitivity)                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testContainsWithCaseInsensitiveSet()
    {
        TestCompactSet<String> ciSet = new TestCompactSet<>(80, true);
        ciSet.add("Hello");
        assertTrue("Case‑insensitive contains should succeed (HELLO)", ciSet.contains("HELLO"));
        assertTrue("Case‑insensitive contains should succeed (hello)", ciSet.contains("hello"));
        assertFalse(ciSet.contains("World"));
    }

    @Test
    public void testContainsNull()
    {
        set.add(null);
        assertTrue(set.contains(null));
        assertFalse(set.contains("something"));
    }

    /* --------------------------------------------------------------------- */
    /* Iterator tests                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIteratorSimpleIteration()
    {
        set.add("a");
        set.add("b");
        set.add("c");

        Set<String> seen = new HashSet<>();
        for (String s : set)
        {
            assertNotNull(s);
            seen.add(s);
        }
        assertEquals(3, seen.size());
        assertTrue(seen.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveBeforeNext()
    {
        set.add("x");
        Iterator<String> it = set.iterator();
        it.remove(); // should throw IllegalStateException
    }

    @Test
    public void testIteratorRemoveWorks()
    {
        set.add("one");
        set.add("two");
        set.add("three");

        Iterator<String> it = set.iterator();
        while (it.hasNext())
        {
            String v = it.next();
            if ("two".equals(v))
            {
                it.remove(); // remove via iterator
            }
        }
        assertFalse(set.contains("two"));
        assertEquals(2, set.size());
    }

    /* --------------------------------------------------------------------- */
    /* Protected hook tests                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCustomCompactSize()
    {
        TestCompactSet<Integer> small = new TestCompactSet<>(5, false);
        for (int i = 0; i < 5; i++)
        {
            assertTrue(small.add(i));
        }
        // Still array representation
        assertTrue(isInternalArray(small));

        // Adding the 6th element forces a transition to Set
        assertTrue(small.add(5));
        assertTrue(isInternalSet(small));
        assertEquals(6, small.size());
    }

    @Test
    public void testGetNewSetIntUsesConstructorIfAvailable()
    {
        // The default implementation uses reflection to find a constructor with an int argument.
        // HashSet has such a constructor, so the returned set should be a HashSet.
        TestCompactSet<String> cs = new TestCompactSet<>();
        Set<String> s = cs.getNewSet(123);
        assertNotNull(s);
        assertTrue(s instanceof HashSet);
    }

    /* --------------------------------------------------------------------- */
    /* Utility methods for introspection of the private 'val' field          */
    /* --------------------------------------------------------------------- */

    private boolean isInternalArray(CompactSet<?> cs) throws Exception
    {
        Field f = CompactSet.class.getDeclaredField("val");
        f.setAccessible(true);
        Object val = f.get(cs);
        return val instanceof Object[];
    }

    private boolean isInternalSet(CompactSet<?> cs) throws Exception
    {
        Field f = CompactSet.class.getDeclaredField("val");
        f.setAccessible(true);
        Object val = f.get(cs);
        return val instanceof Set;
    }
}
