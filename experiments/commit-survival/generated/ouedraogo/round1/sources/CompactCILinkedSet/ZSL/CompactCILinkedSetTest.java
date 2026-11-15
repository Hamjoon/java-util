/****************************************************************************************
 * JUnit 4 test suite for {@link com.cedarsoftware.util.CompactCILinkedSet}.
 *
 * The tests cover:
 *   • Basic Set contract (size, isEmpty, contains, iterator, add, remove, clear)
 *   • Transition from the compact internal representation to the linked Set when the
 *     number of elements exceeds {@code compactSize()}
 *   • Case‑insensitivity behaviour specific to {@code CompactCILinkedSet}
 *   • Construction from an existing {@link java.util.Collection}
 *
 * The test class lives in the same package as the class under test so that we can
 * access the protected {@code compactSize()} method via reflection.
 *
 * Author: OpenAI ChatGPT (generated for the exercise)
 ****************************************************************************************/

package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class CompactCILinkedSetTest {

    /** The instance under test. */
    private CompactCILinkedSet<String> set;

    /** The compact threshold taken from the protected {@code compactSize()} method. */
    private int compactSize;

    @Before
    public void setUp() throws Exception {
        set = new CompactCILinkedSet<>();

        // Use reflection to obtain the protected compactSize() value.
        Method compactSizeMethod = CompactCILinkedSet.class.getDeclaredMethod("compactSize");
        compactSizeMethod.setAccessible(true);
        compactSize = (Integer) compactSizeMethod.invoke(set);
        assertTrue("compactSize should be > 0 for meaningful tests", compactSize > 0);
    }

    /**
     * Verify the basic Set contract before the internal representation switches.
     */
    @Test
    public void testBasicOperationsWithinCompactRange() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // Add elements up to (but not exceeding) compactSize
        for (int i = 0; i < compactSize; i++) {
            String element = "elem" + i;
            assertTrue("add() should return true for new element", set.add(element));
            assertTrue("set should contain just added element", set.contains(element));
            assertEquals(i + 1, set.size());
        }

        // Adding a duplicate (case‑insensitive) must not change size
        assertFalse("adding duplicate (different case) must return false", set.add("ELEM0"));
        assertEquals("size must stay unchanged after duplicate add", compactSize, set.size());

        // Verify iterator order matches insertion order (still within compact range)
        Iterator<String> it = set.iterator();
        for (int i = 0; i < compactSize; i++) {
            assertTrue("iterator should have next element", it.hasNext());
            assertEquals("iterator order must follow insertion order", "elem" + i, it.next());
        }
        assertFalse("iterator should be exhausted", it.hasNext());

        // Remove an element and verify size decrements
        assertTrue(set.remove("elem5"));
        assertEquals(compactSize - 1, set.size());
        assertFalse(set.contains("elem5"));

        // Clear the set
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /**
     * Verify that once the number of elements exceeds {@code compactSize()},
     * the backing Set is a linked (order‑preserving) Set and that case‑insensitivity
     * continues to work.
     */
    @Test
    public void testTransitionToLinkedSetAndOrderPreservation() {
        // Fill the set up to the compact limit
        for (int i = 0; i < compactSize; i++) {
            set.add("c" + i);
        }
        assertEquals(compactSize, set.size());

        // Add one more element – this should trigger the switch to the linked Set
        set.add("trigger");
        assertEquals(compactSize + 1, set.size());

        // Build the expected order: original insertion order followed by the trigger element
        List<String> expectedOrder = new ArrayList<>();
        for (int i = 0; i < compactSize; i++) {
            expectedOrder.add("c" + i);
        }
        expectedOrder.add("trigger");

        // Verify iterator respects insertion order after transition
        List<String> actualOrder = new ArrayList<>();
        for (String s : set) {
            actualOrder.add(s);
        }
        assertEquals("Iterator must preserve insertion order after transition", expectedOrder, actualOrder);

        // Verify case‑insensitivity still works after transition
        assertTrue(set.contains("C0"));          // different case, same logical element
        assertFalse(set.add("C0"));              // duplicate should not be added
        assertEquals(compactSize + 1, set.size());

        // Removal should also be case‑insensitive
        assertTrue(set.remove("TRIGGER"));
        assertEquals(compactSize, set.size());
        assertFalse(set.contains("trigger"));
    }

    /**
     * Test construction from an existing collection and ensure that the case‑insensitive
     * behaviour is applied during the copy.
     */
    @Test
    public void testConstructorWithCollection() {
        List<String> source = Arrays.asList("One", "Two", "ONE", "three");
        CompactCILinkedSet<String> fromCollection = new CompactCILinkedSet<>(source);

        // "ONE" is a duplicate of "One" (case‑insensitive), so effective size is 3
        assertEquals(3, fromCollection.size());
        assertTrue(fromCollection.contains("one"));
        assertTrue(fromCollection.contains("TWO"));
        assertTrue(fromCollection.contains("Three"));
        assertFalse(fromCollection.contains("Four"));
    }

    /**
     * Ensure that remove(Object) works correctly for elements that are not present
     * and for {@code null} (CompactSet does not document null handling, but we verify
     * the contract does not throw unexpectedly).
     */
    @Test
    public void testRemoveEdgeCases() {
        set.add("alpha");
        set.add("beta");

        // Removing a non‑existent element must return false
        assertFalse(set.remove("gamma"));

        // Removing null – depends on underlying implementation; we expect false, not exception
        try {
            boolean result = set.remove(null);
            assertFalse("remove(null) should return false", result);
        } catch (NullPointerException e) {
            // If the implementation throws NPE, the test should still pass as it is an allowed
            // behaviour for a Set that does not support nulls.
        }
    }

    /**
     * Verify that the set reports {@code isEmpty()} correctly after various operations.
     */
    @Test
    public void testIsEmptySemantics() {
        assertTrue(set.isEmpty());

        set.add("x");
        assertFalse(set.isEmpty());

        set.remove("X"); // case‑insensitive removal
        assertTrue(set.isEmpty());

        // Add many elements to force transition, then clear
        for (int i = 0; i < compactSize + 2; i++) {
            set.add("item" + i);
        }
        assertFalse(set.isEmpty());

        set.clear();
        assertTrue(set.isEmpty());
    }

    /**
     * Confirm that the iterator returned by {@code iterator()} does not support
     * {@code remove()} (the default behavior of {@link java.util.AbstractSet} iterator
     * is to allow removal, but we explicitly test that removal via iterator works
     * as expected for this implementation).
     */
    @Test
    public void testIteratorRemoveCapability() {
        set.add("first");
        set.add("second");
        set.add("third");

        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals("first", it.next());

        // Remove via iterator
        it.remove();
        assertEquals(2, set.size());
        assertFalse(set.contains("first"));
        assertTrue(set.contains("second"));
        assertTrue(set.contains("third"));
    }
}
