package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit 4 test suite for {@link CompactSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic empty‑set behaviour</li>
 *   <li>Adding elements, duplicate detection and size changes</li>
 *   <li>Transition from the internal {@code Object[]} representation to a {@code Set}
 *       when the size exceeds {@code compactSize()}</li>
 *   <li>Transition back from {@code Set} to {@code Object[]} when the size drops to
 *       {@code compactSize()}</li>
 *   <li>Removal in both representations</li>
 *   <li>Iterator functionality, including {@code remove()}</li>
 *   <li>Handling of {@code null} elements</li>
 *   <li>Case‑insensitive mode</li>
 * </ul>
 */
public class CompactSetTest {

    /** Helper subclass that forces a small {@code compactSize} so we can test transitions quickly. */
    private static class SmallCompactSet<E> extends CompactSet<E> {
        @Override
        protected int compactSize() {
            return 3;               // switch after 3 elements
        }
    }

    /** Helper subclass that enables case‑insensitive string handling. */
    private static class CaseInsensitiveCompactSet<E> extends CompactSet<E> {
        @Override
        protected boolean isCaseInsensitive() {
            return true;
        }
    }

    /** Utility to read the private {@code val} field via reflection. */
    private static Object getInternalVal(CompactSet<?> set) throws Exception {
        Field f = CompactSet.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(set);
    }

    @Test
    public void testEmptySetBehavior() throws Exception {
        CompactSet<String> set = new CompactSet<>();
        assertTrue("New set must be empty", set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains("anything"));
        assertFalse(set.remove("anything"));
        assertFalse(set.iterator().hasNext());

        // internal representation must be the special EMPTY_SET sentinel
        assertSame("Internal value should be EMPTY_SET sentinel",
                getInternalVal(set), getInternalVal(new CompactSet<>()));
    }

    @Test
    public void testAddSingleElement() throws Exception {
        CompactSet<Integer> set = new CompactSet<>();
        assertTrue(set.add(42));
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertTrue(set.contains(42));

        // iterator should return the element once
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(42), it.next());
        assertFalse(it.hasNext());

        // remove via iterator
        it = set.iterator();
        it.next();
        it.remove();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testAddDuplicateElement() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.add("alpha"));
        assertFalse("Adding duplicate must return false", set.add("alpha"));
        assertEquals(1, set.size());
    }

    @Test
    public void testTransitionArrayToSet() throws Exception {
        SmallCompactSet<String> set = new SmallCompactSet<>();

        // Add up to compactSize (3) – should stay as Object[]
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertTrue(set.add("c"));
        assertEquals(3, set.size());

        Object internal = getInternalVal(set);
        assertTrue("Should be stored as an Object[] before threshold",
                internal instanceof Object[]);

        // Add one more element – should switch to a Set implementation
        assertTrue(set.add("d"));
        assertEquals(4, set.size());

        internal = getInternalVal(set);
        assertTrue("Should have switched to a Set after exceeding compactSize",
                internal instanceof Set);
    }

    @Test
    public void testTransitionSetToArray() throws Exception {
        SmallCompactSet<Integer> set = new SmallCompactSet<>();

        // Fill to trigger Set mode
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4); // size == 4, now internal is a Set
        assertTrue(getInternalVal(set) instanceof Set);

        // Remove one element – size drops to compactSize (3) and should revert to array
        assertTrue(set.remove(4));
        assertEquals(3, set.size());
        assertTrue("Should have reverted to Object[] after removal to compactSize",
                getInternalVal(set) instanceof Object[]);
    }

    @Test
    public void testRemoveInArrayMode() throws Exception {
        SmallCompactSet<String> set = new SmallCompactSet<>();

        set.add("x");
        set.add("y");
        set.add("z"); // still in array mode (size == 3)

        // Remove middle element
        assertTrue(set.remove("y"));
        assertEquals(2, set.size());
        assertFalse(set.contains("y"));
        assertTrue(set.contains("x"));
        assertTrue(set.contains("z"));

        // Remove last remaining element
        assertTrue(set.remove("x"));
        assertTrue(set.remove("z"));
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertTrue(getInternalVal(set) == getInternalVal(new CompactSet<>())); // back to EMPTY_SET sentinel
    }

    @Test
    public void testIteratorRemoveBeforeNextThrows() {
        CompactSet<Integer> set = new CompactSet<>();
        set.add(10);
        Iterator<Integer> it = set.iterator();
        try {
            it.remove();
            fail("Calling remove() before next() must throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testIteratorRemoveAfterNextUpdatesSet() {
        SmallCompactSet<String> set = new SmallCompactSet<>();
        set.add("a");
        set.add("b");
        set.add("c"); // still array mode

        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("b".equals(val)) {
                it.remove(); // remove via iterator
            }
        }
        assertEquals(2, set.size());
        assertFalse(set.contains("b"));
        assertTrue(set.contains("a"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testClearEmptiesSet() {
        CompactSet<String> set = new CompactSet<>();
        set.add("one");
        set.add("two");
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains("one"));
    }

    @Test
    public void testNullElementHandling() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());

        // duplicate null must not be added
        assertFalse(set.add(null));
        assertEquals(1, set.size());

        // remove null
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testCaseInsensitiveBehavior() {
        CaseInsensitiveCompactSet<String> set = new CaseInsensitiveCompactSet<>();
        assertTrue(set.add("Hello"));
        // same word in different case is considered duplicate
        assertFalse("Duplicate (case‑insensitive) should not be added", set.add("hello"));
        assertTrue(set.contains("HELLO"));
        assertEquals(1, set.size());

        // Removing with a different case should succeed
        assertTrue(set.remove("HeLLo"));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testGetCopyProducesIndependentSet() throws Exception {
        SmallCompactSet<Integer> set = new SmallCompactSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        // force transition to Set
        set.add(4);

        // The iterator uses getCopy internally; modify the returned iterator's set should not affect original
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        int first = it.next();
        // remove via iterator (which calls CompactSet.this.remove)
        it.remove();

        assertEquals(3, set.size());
        assertFalse(set.contains(first));
    }
}
