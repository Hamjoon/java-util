package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCIHashSet}.
 *
 * <p>This suite covers:
 * <ul>
 *   <li>Both constructors (empty and collection‑based)</li>
 *   <li>Case‑insensitive behavior for {@code add}, {@code contains},
 *       {@code remove} and bulk operations</li>
 *   <li>Standard {@link Set} contract methods: {@code size},
 *       {@code isEmpty}, {@code clear}, {@code iterator},
 *       {@code toArray}, {@code equals} and {@code hashCode}</li>
 *   <li>Deprecated {@code getNewSet()} method</li>
 *   <li>Error scenarios such as {@code null} collection argument</li>
 * </ul>
 * </p>
 */
public class CompactCIHashSetTest {

    private CompactCIHashSet<String> emptySet;
    private CompactCIHashSet<String> populatedSet;

    @Before
    public void setUp() {
        emptySet = new CompactCIHashSet<>();
        populatedSet = new CompactCIHashSet<>(Arrays.asList("Apple", "Banana", "Cherry"));
    }

    /* --------------------------------------------------------------
     * Constructor tests
     * -------------------------------------------------------------- */

    @Test
    public void testEmptyConstructorCreatesEmptySet() {
        assertTrue("Newly constructed set should be empty", emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElementsCaseInsensitive() {
        // The constructor should add the three elements from the collection
        assertEquals(3, populatedSet.size());
        // Verify case‑insensitive lookup
        assertTrue(populatedSet.contains("apple"));
        assertTrue(populatedSet.contains("BANANA"));
        assertTrue(populatedSet.contains("cHeRrY"));
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNull() {
        new CompactCIHashSet<String>(null);
    }

    /* --------------------------------------------------------------
     * Core behavior – case‑insensitivity
     * -------------------------------------------------------------- */

    @Test
    public void testAddIsCaseInsensitive() {
        assertTrue(emptySet.add("Apple"));
        // Adding the same logical element with different case must return false
        assertFalse(emptySet.add("APPLE"));
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("apple"));
        assertTrue(emptySet.contains("ApPlE"));
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        emptySet.add("Banana");
        assertTrue(emptySet.remove("BANANA"));
        assertFalse(emptySet.contains("banana"));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testAddAllCaseInsensitive() {
        Collection<String> toAdd = Arrays.asList("dog", "Cat", "DOG", "cat");
        assertTrue(emptySet.addAll(toAdd)); // first two distinct values added
        assertEquals(2, emptySet.size());

        // The set must contain the values irrespective of case
        assertTrue(emptySet.contains("DOG"));
        assertTrue(emptySet.contains("cat"));
    }

    @Test
    public void testContainsAllCaseInsensitive() {
        assertTrue(populatedSet.containsAll(Arrays.asList("apple", "BANANA")));
        assertFalse(populatedSet.containsAll(Arrays.asList("apple", "durian")));
    }

    @Test
    public void testRetainAllCaseInsensitive() {
        // retain only “banana” (different case) and “durian”
        assertTrue(populatedSet.retainAll(Arrays.asList("BANANA", "DURIAN")));
        // After retainAll, only “banana” should remain
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("banana"));
        assertFalse(populatedSet.contains("apple"));
    }

    @Test
    public void testRemoveAllCaseInsensitive() {
        assertTrue(populatedSet.removeAll(Arrays.asList("CHERRY", "apple")));
        assertEquals(1, populatedSet.size());
        assertFalse(populatedSet.contains("apple"));
        assertFalse(populatedSet.contains("cherry"));
        assertTrue(populatedSet.contains("banana"));
    }

    /* --------------------------------------------------------------
     * Standard Set contract methods
     * -------------------------------------------------------------- */

    @Test
    public void testIteratorOrderAndRemoval() {
        Iterator<String> it = populatedSet.iterator();
        // The iterator should traverse all elements (order is unspecified)
        int count = 0;
        while (it.hasNext()) {
            String val = it.next();
            assertNotNull(val);
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testToArray() {
        Object[] arr = populatedSet.toArray();
        assertEquals(3, arr.length);
        // All elements must be present (case‑insensitive check)
        assertTrue(Arrays.asList(arr).contains("Apple"));
        assertTrue(Arrays.asList(arr).contains("Banana"));
        assertTrue(Arrays.asList(arr).contains("Cherry"));
    }

    @Test
    public void testToArrayWithRuntimeType() {
        String[] a = new String[0];
        String[] result = populatedSet.toArray(a);
        assertEquals(3, result.length);
        // Verify content irrespective of order
        assertTrue(Arrays.asList(result).containsAll(Arrays.asList("Apple", "Banana", "Cherry")));
    }

    @Test
    public void testClear() {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactCIHashSet<String> other = new CompactCIHashSet<>(Arrays.asList("CHERRY", "banana", "apple"));
        assertEquals(populatedSet, other);
        assertEquals(populatedSet.hashCode(), other.hashCode());

        other.add("Durian");
        assertNotEquals(populatedSet, other);
    }

    @Test
    public void testToStringContainsElements() {
        String s = populatedSet.toString();
        assertTrue(s.contains("Apple"));
        assertTrue(s.contains("Banana"));
        assertTrue(s.contains("Cherry"));
    }

    /* --------------------------------------------------------------
     * Protected/Deprecated methods specific to CompactCIHashSet
     * -------------------------------------------------------------- */

    @Test
    public void testIsCaseInsensitiveOverridesSuper() {
        // The method is protected; we can access it via a subclass wrapper
        class Wrapper extends CompactCIHashSet<String> {
            Wrapper() { super(); }
            boolean expose() { return isCaseInsensitive(); }
        }
        Wrapper w = new Wrapper();
        assertTrue("CompactCIHashSet must report case‑insensitive", w.expose());
    }

    @Test
    public void testDeprecatedGetNewSetReturnsNull() {
        // Direct call is allowed because the method is protected; use a wrapper
        class Wrapper extends CompactCIHashSet<String> {
            Wrapper() { super(); }
            Set<String> expose() { return getNewSet(); }
        }
        Wrapper w = new Wrapper();
        assertNull("Deprecated getNewSet() should return null", w.expose());
    }

    /* --------------------------------------------------------------
     * Non‑String generic type – case‑insensitivity has no effect
     * -------------------------------------------------------------- */

    @Test
    public void testNonStringElementsBehaviour() {
        CompactCIHashSet<Integer> intSet = new CompactCIHashSet<>();
        assertTrue(intSet.add(10));
        assertFalse(intSet.add(10)); // duplicate
        assertTrue(intSet.contains(10));
        assertTrue(intSet.remove(10));
        assertTrue(intSet.isEmpty());
    }
}
