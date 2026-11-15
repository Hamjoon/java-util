/****************************************************************************************
 * CompactSetTest.java
 *
 * JUnit‑4 test suite that exercises all public behaviour of {@link CompactSet}.
 * The tests verify correct handling of the internal representation (Object[] ↔ Set)
 * that CompactSet switches between when the number of elements crosses the
 * {@code compactSize()} threshold (default 80).  Reflection is used only to
 * inspect the private {@code val} field – this does not affect the production
 * code and satisfies the requirement to avoid mocking or subclassing library
 * types.
 *
 * The test class resides in the same package as {@code CompactSet} so that it
 * can access package‑private members if needed.
 ****************************************************************************************/
package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class CompactSetTest
{
    private CompactSet<String> set;
    private static final int COMPACT_SIZE = 80;   // matches CompactSet.default

    /** Helper to read the private {@code val} field via reflection. */
    private static Object getInternalVal(CompactSet<?> cs) throws Exception
    {
        Field f = CompactSet.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(cs);
    }

    @Before
    public void setUp()
    {
        set = new CompactSet<>();
    }

    /*--------------------------------------------------------------
     *  Basic construction and empty‑set behaviour
     *--------------------------------------------------------------*/
    @Test
    public void testEmptyConstructor()
    {
        assertTrue("New set must be empty", set.isEmpty());
        assertEquals("Size of new set must be 0", 0, set.size());
        assertFalse("Contains any element must be false", set.contains("anything"));
        assertFalse("Contains null must be false", set.contains(null));
    }

    @Test
    public void testConstructorWithCollection()
    {
        List<String> src = new ArrayList<>();
        src.add("a");
        src.add("b");
        src.add("c");

        CompactSet<String> cs = new CompactSet<>(src);
        assertEquals(3, cs.size());
        assertTrue(cs.contains("a"));
        assertTrue(cs.contains("b"));
        assertTrue(cs.contains("c"));

        // iterator must return elements in insertion order while still using the array representation
        Iterator<String> it = cs.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
    }

    /*--------------------------------------------------------------
     *  add(), contains() and duplicate handling
     *--------------------------------------------------------------*/
    @Test
    public void testAddAndContains()
    {
        assertTrue(set.add("one"));
        assertTrue(set.add("two"));
        assertFalse(set.add("one"));               // duplicate
        assertEquals(2, set.size());

        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));
        assertFalse(set.contains("three"));
    }

    @Test
    public void testAddNullElement()
    {
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());

        // second add of null must be rejected
        assertFalse(set.add(null));
        assertEquals(1, set.size());
    }

    /*--------------------------------------------------------------
     *  Case‑sensitivity (default is false → case‑sensitive)
     *--------------------------------------------------------------*/
    @Test
    public void testCaseSensitivity()
    {
        set.add("Test");
        assertTrue(set.contains("Test"));
        assertFalse(set.contains("test"));   // different case → not contained
    }

    /*--------------------------------------------------------------
     *  Transition from array representation to Set at compactSize()
     *--------------------------------------------------------------*/
    @Test
    public void testThresholdSwitchToSet() throws Exception
    {
        // Fill up to compactSize() elements (80)
        for (int i = 0; i < COMPACT_SIZE; i++)
        {
            assertTrue(set.add("e" + i));
        }
        assertEquals(COMPACT_SIZE, set.size());

        Object internal = getInternalVal(set);
        assertTrue("Before threshold we expect an Object[]", internal instanceof Object[]);

        // Adding the 81st element should trigger conversion to a Set
        assertTrue(set.add("trigger"));
        assertEquals(COMPACT_SIZE + 1, set.size());

        internal = getInternalVal(set);
        assertTrue("After crossing threshold we expect a Set", internal instanceof Set);
    }

    /*--------------------------------------------------------------
     *  Remove element that brings size back to compactSize() → back to array
     *--------------------------------------------------------------*/
    @Test
    public void testRemoveSwitchBackToArray() throws Exception
    {
        // Build a Set with 81 elements (crossed threshold)
        for (int i = 0; i < COMPACT_SIZE + 1; i++)
        {
            set.add("v" + i);
        }
        assertTrue(getInternalVal(set) instanceof Set);
        assertEquals(COMPACT_SIZE + 1, set.size());

        // Remove one element → size becomes 80, should revert to Object[]
        assertTrue(set.remove("v0"));
        assertEquals(COMPACT_SIZE, set.size());

        Object internal = getInternalVal(set);
        assertTrue("After removal to compactSize we expect an Object[]", internal instanceof Object[]);
    }

    /*--------------------------------------------------------------
     *  Removal while still using the array representation
     *--------------------------------------------------------------*/
    @Test
    public void testRemoveFromArray()
    {
        set.add("alpha");
        set.add("beta");
        assertEquals(2, set.size());

        // Remove existing element
        assertTrue(set.remove("alpha"));
        assertEquals(1, set.size());
        assertFalse(set.contains("alpha"));
        assertTrue(set.contains("beta"));

        // Remove the last element – set becomes empty (EMPTY_SET)
        assertTrue(set.remove("beta"));
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());

        // Removing a non‑existent element must return false
        assertFalse(set.remove("nonexistent"));
    }

    /*--------------------------------------------------------------
     *  Iterator functionality, including remove() contract
     *--------------------------------------------------------------*/
    @Test
    public void testIteratorRemove()
    {
        set.add("x");
        set.add("y");
        set.add("z");

        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals("x", it.next());

        // Valid remove after next()
        it.remove();
        assertEquals(2, set.size());
        assertFalse(set.contains("x"));

        // IllegalStateException if remove() called again before next()
        try
        {
            it.remove();
            fail("Expected IllegalStateException when remove() called twice without next()");
        }
        catch (IllegalStateException ignored) { }

        // Continue iteration
        assertEquals("y", it.next());
        it.remove(); // remove y
        assertEquals(1, set.size());
        assertFalse(set.contains("y"));
        assertTrue(set.contains("z"));
    }

    /*--------------------------------------------------------------
     *  clear() resets the set completely
     *--------------------------------------------------------------*/
    @Test
    public void testClear()
    {
        set.add("a");
        set.add("b");
        set.add(null);
        assertEquals(3, set.size());

        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains("a"));
        assertFalse(set.contains(null));
    }

    /*--------------------------------------------------------------
     *  Verify that the internal EMPTY_SET marker is used after removing
     *  the final element.
     *--------------------------------------------------------------*/
    @Test
    public void testInternalEmptyMarker() throws Exception
    {
        set.add("solo");
        assertTrue(set.remove("solo"));
        assertEquals(0, set.size());

        Object internal = getInternalVal(set);
        // The EMPTY_SET constant is a unique String; we compare by reference/value.
        Field emptyField = CompactSet.class.getDeclaredField("EMPTY_SET");
        emptyField.setAccessible(true);
        String emptyMarker = (String) emptyField.get(null);
        assertTrue("Internal val should be the EMPTY_SET marker", internal == emptyMarker);
    }
}
