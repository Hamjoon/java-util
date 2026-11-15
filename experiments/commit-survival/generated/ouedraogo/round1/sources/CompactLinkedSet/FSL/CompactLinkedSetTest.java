package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link CompactLinkedSet}.
 *
 * The tests focus on:
 * <ul>
 *   <li>basic Set contract (add, remove, contains, size, isEmpty)</li>
 *   <li>behaviour when the internal storage switches from the compact representation
 *       to a {@link java.util.LinkedHashSet} (i.e. when size() exceeds {@code compactSize()})</li>
 *   <li>preservation of insertion order after the switch</li>
 *   <li>construction from an existing {@link java.util.Collection}</li>
 *   <li>edge cases such as duplicate adds, null elements and clear()</li>
 * </ul>
 *
 * The test class lives in the same package as {@link CompactLinkedSet}
 * so that protected members (e.g. {@code compactSize()}) are directly accessible.
 */
public class CompactLinkedSetTest {

    @Test
    public void testInitiallyEmpty() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        assertTrue("New set should be empty", set.isEmpty());
        assertEquals("Size of new set should be 0", 0, set.size());
    }

    @Test
    public void testAddAndContains() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        assertTrue("First add should return true", set.add("alpha"));
        assertTrue("Set should contain the added element", set.contains("alpha"));
        assertFalse("Adding the same element again should return false", set.add("alpha"));
        assertEquals("Size should still be 1 after duplicate add", 1, set.size());

        assertTrue("Adding a different element should return true", set.add("beta"));
        assertTrue("Set should contain the second element", set.contains("beta"));
        assertEquals("Size should be 2 after two distinct adds", 2, set.size());
    }

    @Test
    public void testRemoveAndClear() {
        CompactLinkedSet<Integer> set = new CompactLinkedSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        assertTrue("Remove existing element should return true", set.remove(2));
        assertFalse("Set should no longer contain removed element", set.contains(2));
        assertEquals("Size should be 2 after removal", 2, set.size());

        set.clear();
        assertTrue("Set should be empty after clear()", set.isEmpty());
        assertEquals("Size should be 0 after clear()", 0, set.size());
    }

    @Test
    public void testNullElementHandling() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        assertTrue("Adding null should be allowed and return true", set.add(null));
        assertTrue("Set should contain null after addition", set.contains(null));
        assertFalse("Adding null a second time should return false", set.add(null));
        assertTrue("Removing null should succeed", set.remove(null));
        assertFalse("Set should no longer contain null after removal", set.contains(null));
        assertEquals("Size should be 0 after removing null", 0, set.size());
    }

    @Test
    public void testTransitionToLinkedHashSetPreservesInsertionOrder() {
        CompactLinkedSet<Integer> set = new CompactLinkedSet<>();
        int compactThreshold = set.compactSize(); // protected, accessible because test is in same package

        // Fill the set up to the compact threshold
        for (int i = 0; i < compactThreshold; i++) {
            assertTrue("Adding element " + i + " should succeed", set.add(i));
        }
        assertEquals("Size should equal compact threshold before transition", compactThreshold, set.size());

        // Add one more element to trigger the switch to LinkedHashSet
        assertTrue("Adding element that exceeds compactSize should succeed", set.add(compactThreshold));
        assertEquals("Size should be threshold+1 after transition", compactThreshold + 1, set.size());

        // Verify that iteration follows insertion order (0,1,2,...,threshold)
        int expected = 0;
        for (Integer value : set) {
            assertEquals("Iteration order should follow insertion order after transition", Integer.valueOf(expected), value);
            expected++;
        }
        assertEquals("All elements should have been iterated", compactThreshold + 1, expected);
    }

    @Test
    public void testConstructorWithCollectionTriggersTransitionAndPreservesOrder() {
        // Build a collection larger than compactSize()
        List<String> source = new ArrayList<>();
        CompactLinkedSet<String> dummy = new CompactLinkedSet<>();
        int threshold = dummy.compactSize();

        for (int i = 0; i < threshold + 3; i++) {
            source.add("elem-" + i);
        }

        CompactLinkedSet<String> set = new CompactLinkedSet<>(source);
        assertEquals("Set size should match source collection size", source.size(), set.size());

        // Verify that iteration order matches the order of the source collection
        Iterator<String> expectedIt = source.iterator();
        Iterator<String> actualIt = set.iterator();
        while (expectedIt.hasNext() && actualIt.hasNext()) {
            assertEquals("Element order must be preserved from constructor collection", expectedIt.next(), actualIt.next());
        }
        assertFalse("Both iterators should be exhausted at the same time", expectedIt.hasNext() ^ actualIt.hasNext());
    }

    @Test
    public void testAddAfterTransitionContinuesToPreserveOrder() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        int threshold = set.compactSize();

        // Fill to threshold
        for (int i = 0; i < threshold; i++) {
            set.add("pre-" + i);
        }

        // Trigger transition
        set.add("trigger");

        // Add more elements after transition
        set.add("post-1");
        set.add("post-2");

        // Expected order: all pre‑elements, then trigger, then post‑elements
        List<String> expectedOrder = new ArrayList<>();
        for (int i = 0; i < threshold; i++) {
            expectedOrder.add("pre-" + i);
        }
        expectedOrder.add("trigger");
        expectedOrder.add("post-1");
        expectedOrder.add("post-2");

        int idx = 0;
        for (String actual : set) {
            assertEquals("Order after adding post‑transition elements", expectedOrder.get(idx), actual);
            idx++;
        }
        assertEquals("All expected elements should be present", expectedOrder.size(), idx);
    }

    @Test
    public void testIteratorRemoveDuringAndAfterTransition() {
        CompactLinkedSet<Integer> set = new CompactLinkedSet<>();
        int threshold = set.compactSize();

        // Populate up to threshold (still compact)
        for (int i = 0; i < threshold; i++) {
            set.add(i);
        }

        // Remove via iterator while still compact
        Iterator<Integer> it = set.iterator();
        assertTrue("Iterator should have next", it.hasNext());
        Integer first = it.next();
        it.remove();
        assertFalse("Set should no longer contain the removed element", set.contains(first));
        assertEquals("Size should be threshold-1 after iterator removal", threshold - 1, set.size());

        // Add one more element to cause transition
        set.add(threshold);

        // Verify iterator remove works after transition
        Iterator<Integer> it2 = set.iterator();
        while (it2.hasNext()) {
            Integer val = it2.next();
            if (val.equals(0)) { // remove a known element if present
                it2.remove();
                assertFalse("Element 0 should be removed", set.contains(0));
                break;
            }
        }
    }

    @Test
    public void testGetNewSetReturnsLinkedHashSetWhenUsedInternally() throws Exception {
        // The only way to observe the concrete Set returned by getNewSet()
        // is to force the transition and then inspect the delegate via reflection.
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        int threshold = set.compactSize();

        // Fill to just below the threshold
        for (int i = 0; i < threshold; i++) {
            set.add("v" + i);
        }

        // Force transition
        set.add("trigger");

        // Use reflection to obtain the private field that holds the delegate Set.
        // The field name in CompactSet (the superclass) is typically "set".
        // If the implementation changes, the test will fail, which is acceptable
        // because the contract of CompactLinkedSet requires a LinkedHashSet after transition.
        java.lang.reflect.Field delegateField = CompactSet.class.getDeclaredField("set");
        delegateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> internal = (Set<String>) delegateField.get(set);
        assertTrue("After transition the internal Set should be a LinkedHashSet", internal instanceof java.util.LinkedHashSet);
    }
}
