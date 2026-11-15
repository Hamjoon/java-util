package com.cedarsoftware.util;

import org.junit.Test;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactLinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic Set contract (add, remove, contains, size, isEmpty, clear)</li>
 *   <li>construction from another collection</li>
 *   <li>preservation of insertion order once the internal delegate switches to a {@link LinkedHashSet}</li>
 *   <li>the protected {@code getNewSet()} method returns a {@link LinkedHashSet}</li>
 *   <li>the protected {@code compactSize()} value is respected</li>
 * </ul>
 *
 * No mocking is used – real objects are created via the public API.
 */
public class CompactLinkedSetTest {

    /**
     * Helper that uses reflection to expose the protected {@code compactSize()}
     * method from {@link CompactSet}.
     */
    private static int getCompactSize(CompactLinkedSet<?> set) throws Exception {
        Method m = CompactSet.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        return (Integer) m.invoke(set);
    }

    /**
     * Helper that uses reflection to expose the protected {@code getNewSet()}
     * method.
     */
    private static Set<?> invokeGetNewSet(CompactLinkedSet<?> set) throws Exception {
        Method m = CompactSet.class.getDeclaredMethod("getNewSet");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<?> newSet = (Set<?>) m.invoke(set);
        return newSet;
    }

    @Test
    public void testEmptySet() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        assertTrue("New set should be empty", set.isEmpty());
        assertEquals("Size of new set should be 0", 0, set.size());
    }

    @Test
    public void testAddAndContains() {
        CompactLinkedSet<Integer> set = new CompactLinkedSet<>();
        assertTrue(set.add(10));
        assertTrue(set.add(20));
        assertTrue(set.contains(10));
        assertTrue(set.contains(20));
        assertFalse(set.contains(30));
        assertEquals(2, set.size());
    }

    @Test
    public void testAddDuplicate() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        assertTrue(set.add("alpha"));
        assertFalse("Adding a duplicate should return false", set.add("alpha"));
        assertEquals("Size should remain 1 after duplicate add", 1, set.size());
    }

    @Test
    public void testRemove() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.add("one");
        set.add("two");
        assertTrue(set.remove("one"));
        assertFalse(set.contains("one"));
        assertEquals(1, set.size());

        // Removing a non‑existent element must return false
        assertFalse(set.remove("not‑present"));
        assertEquals(1, set.size());
    }

    @Test
    public void testClearAndIsEmpty() {
        CompactLinkedSet<Integer> set = new CompactLinkedSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue("Set should be empty after clear()", set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testConstructorFromCollection() {
        Set<String> source = new LinkedHashSet<>();
        source.add("first");
        source.add("second");
        source.add("third");

        CompactLinkedSet<String> set = new CompactLinkedSet<>(source);
        assertEquals(source.size(), set.size());
        assertTrue(set.containsAll(source));

        // Ensure iteration order matches the source collection
        Iterator<String> srcIt = source.iterator();
        Iterator<String> setIt = set.iterator();
        while (srcIt.hasNext()) {
            assertTrue(setIt.hasNext());
            assertEquals(srcIt.next(), setIt.next());
        }
        assertFalse(setIt.hasNext());
    }

    @Test
    public void testIterationOrderPreservedAfterSwitch() throws Exception {
        CompactLinkedSet<Integer> set = new CompactLinkedSet<>();

        // Determine the compact threshold via reflection
        int compactThreshold = getCompactSize(set);
        int elementsToAdd = compactThreshold + 5; // exceed the threshold

        // Insert elements in a known order
        for (int i = 0; i < elementsToAdd; i++) {
            assertTrue(set.add(i));
        }
        assertEquals(elementsToAdd, set.size());

        // Verify that iteration follows insertion order
        int expected = 0;
        for (Integer value : set) {
            assertEquals("Iteration order must follow insertion order", Integer.valueOf(expected), value);
            expected++;
        }
        assertEquals("All elements should have been iterated", elementsToAdd, expected);
    }

    @Test
    public void testGetNewSetReturnsLinkedHashSet() throws Exception {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        Set<?> newSet = invokeGetNewSet(set);
        assertNotNull("getNewSet() must not return null", newSet);
        assertTrue("getNewSet() must return a LinkedHashSet", newSet instanceof LinkedHashSet);
    }

    @Test
    public void testCompactSizeBehaviour() throws Exception {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        int compactSize = getCompactSize(set);

        // Add exactly compactSize elements – still should be in the “compact” mode
        for (int i = 0; i < compactSize; i++) {
            set.add("c" + i);
        }
        assertEquals(compactSize, set.size());

        // Adding one more element forces a switch to the LinkedHashSet delegate.
        set.add("trigger");
        assertEquals(compactSize + 1, set.size());

        // Verify that after the switch the order is still insertion order
        int index = 0;
        for (String s : set) {
            if (index < compactSize) {
                assertEquals("c" + index, s);
            } else {
                assertEquals("trigger", s);
            }
            index++;
        }
    }

    @Test
    public void testIteratorRemove() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.add("a");
        set.add("b");
        set.add("c");

        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("b".equals(val)) {
                it.remove(); // should remove "b"
            }
        }

        assertFalse(set.contains("b"));
        assertEquals(2, set.size());
    }
}
