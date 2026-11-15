package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit 4 test suite for {@link CompactLinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (empty and collection‑based)</li>
 *   <li>Case‑insensitive semantics (add, contains, remove)</li>
 *   <li>Linked (insertion) order preservation</li>
 *   <li>Standard {@link Set} operations (addAll, retainAll, removeAll, clear, iterator, toArray, equals, hashCode, toString)</li>
 *   <li>Null handling</li>
 * </ul>
 */
public class CompactLinkedSetTest {

    /* --------------------------------------------------------------------- */
    /*  Construction Tests                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructorCreatesEmptySet() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        assertTrue("Newly constructed set must be empty", set.isEmpty());
        assertEquals("Size of empty set must be 0", 0, set.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        List<String> source = Arrays.asList("Alpha", "Beta", "Gamma");
        CompactLinkedSet<String> set = new CompactLinkedSet<>(source);

        assertEquals("All elements from source collection must be present", source.size(), set.size());
        for (String s : source) {
            assertTrue("Element '" + s + "' must be contained", set.contains(s));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNull() {
        new CompactLinkedSet<String>((Collection<String>) null);
    }

    /* --------------------------------------------------------------------- */
    /*  Case‑Insensitive Behaviour                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddIsCaseInsensitive() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        assertTrue(set.add("Value"));
        assertFalse("Adding the same string with different case must be rejected", set.add("value"));
        assertEquals("Set size must be 1 after case‑insensitive duplicate add", 1, set.size());
    }

    @Test
    public void testContainsIsCaseInsensitive() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.add("Test");
        assertTrue("Contains must succeed with same case", set.contains("Test"));
        assertTrue("Contains must succeed with different case", set.contains("test"));
        assertFalse("Contains must fail for unrelated values", set.contains("other"));
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.add("RemoveMe");
        assertTrue("Remove must succeed with same case", set.remove("RemoveMe"));
        assertTrue("Set must be empty after removal", set.isEmpty());

        set.add("Again");
        assertTrue("Remove must succeed with different case", set.remove("again"));
        assertTrue("Set must be empty after case‑insensitive removal", set.isEmpty());
    }

    @Test
    public void testNullElementHandling() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        assertTrue("Adding null should be allowed", set.add(null));
        assertTrue("Set must contain null after adding", set.contains(null));
        assertEquals("Size must be 1 after adding null", 1, set.size());

        // Adding another null should be considered a duplicate
        assertFalse("Adding a second null must be rejected as duplicate", set.add(null));
        assertEquals("Size must still be 1 after duplicate null add", 1, set.size());

        assertTrue("Removing null must succeed", set.remove(null));
        assertFalse("Set must not contain null after removal", set.contains(null));
        assertTrue("Set must be empty after removing null", set.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  Order Preservation (Linked behavior)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIterationOrderPreservesInsertionOrder() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.add("first");
        set.add("second");
        set.add("third");

        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals("first", it.next());
        assertTrue(it.hasNext());
        assertEquals("second", it.next());
        assertTrue(it.hasNext());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testToArrayPreservesInsertionOrder() {
        CompactLinkedSet<Integer> set = new CompactLinkedSet<>();
        set.add(10);
        set.add(20);
        set.add(30);

        Object[] arr = set.toArray();
        assertArrayEquals(new Object[] {10, 20, 30}, arr);
    }

    @Test
    public void testToTypedArrayPreservesInsertionOrder() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.add("a");
        set.add("b");
        set.add("c");

        String[] arr = set.toArray(new String[0]);
        assertArrayEquals(new String[] {"a", "b", "c"}, arr);
    }

    /* --------------------------------------------------------------------- */
    /*  Bulk Operations                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAllRespectsCaseInsensitivity() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        List<String> list = Arrays.asList("One", "Two", "ONE", "three");
        boolean changed = set.addAll(list);
        assertTrue("addAll must report modification", changed);
        assertEquals("Duplicate (different case) must be ignored", 3, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("three"));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElementsCaseInsensitively() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.addAll(Arrays.asList("Alpha", "Beta", "Gamma", "Delta"));

        Collection<String> toRetain = Arrays.asList("beta", "DELTA", "epsilon");
        boolean changed = set.retainAll(toRetain);
        assertTrue("retainAll must report change when elements are removed", changed);
        assertEquals("Only two elements should remain after retainAll", 2, set.size());
        assertTrue(set.contains("beta"));
        assertTrue(set.contains("delta"));
        assertFalse(set.contains("alpha"));
        assertFalse(set.contains("gamma"));
    }

    @Test
    public void testRemoveAllRemovesElementsCaseInsensitively() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.addAll(Arrays.asList("One", "Two", "Three", "Four"));

        Collection<String> toRemove = Arrays.asList("two", "FOUR");
        boolean changed = set.removeAll(toRemove);
        assertTrue("removeAll must report modification", changed);
        assertEquals("Two elements must remain after removal", 2, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("three"));
        assertFalse(set.contains("two"));
        assertFalse(set.contains("four"));
    }

    @Test
    public void testClearEmptiesSet() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(set.isEmpty());

        set.clear();
        assertTrue("Set must be empty after clear()", set.isEmpty());
        assertEquals(0, set.size());
    }

    /* --------------------------------------------------------------------- */
    /*  Equality, Hashing and String Representation                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeWithAnotherCompactLinkedSet() {
        CompactLinkedSet<String> set1 = new CompactLinkedSet<>();
        set1.addAll(Arrays.asList("X", "Y", "Z"));

        CompactLinkedSet<String> set2 = new CompactLinkedSet<>();
        set2.addAll(Arrays.asList("z", "y", "x")); // different order, different case

        assertTrue("Sets with same logical elements must be equal", set1.equals(set2));
        assertTrue("Equal sets must have identical hash codes", set1.hashCode() == set2.hashCode());
    }

    @Test
    public void testEqualsWithStandardHashSet() {
        CompactLinkedSet<String> compactSet = new CompactLinkedSet<>();
        compactSet.addAll(Arrays.asList("A", "B", "C"));

        Set<String> hashSet = new HashSet<>(Arrays.asList("c", "b", "a"));
        assertTrue("CompactLinkedSet should be equal to a standard Set with same elements (case‑insensitive)", compactSet.equals(hashSet));
    }

    @Test
    public void testToStringContainsAllElements() {
        CompactLinkedSet<Integer> set = new CompactLinkedSet<>();
        set.addAll(Arrays.asList(5, 10, 15));

        String s = set.toString();
        // The exact format is implementation dependent, but it must contain the elements.
        assertTrue("String representation must contain '5'", s.contains("5"));
        assertTrue("String representation must contain '10'", s.contains("10"));
        assertTrue("String representation must contain '15'", s.contains("15"));
    }

    /* --------------------------------------------------------------------- */
    /*  Miscellaneous API Checks                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsCaseInsensitiveIsTrue() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        // The method is protected; we verify its effect through public behavior.
        set.add("Case");
        assertTrue("Set should treat 'case' as already present", set.contains("case"));
    }

    @Test
    public void testGetConfigAndWithConfigRoundTrip() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        // The default config map should not be null.
        assertNotNull("Default config map must be non‑null", set.getConfig());

        // Modify a config entry and ensure withConfig returns a set with that config.
        java.util.Map<String, Object> cfg = new java.util.HashMap<>(set.getConfig());
        cfg.put("testKey", "testValue");
        CompactLinkedSet<String> newSet = set.withConfig(cfg);
        assertEquals("Config value must be preserved after withConfig", "testValue", newSet.getConfig().get("testKey"));
    }
}
