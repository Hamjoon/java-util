package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 tests for {@link CompactCILinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (empty and from a collection)</li>
 *   <li>Null‑collection handling</li>
 *   <li>Case‑insensitive contract for {@code add}, {@code contains},
 *       {@code remove}, {@code retainAll} and {@code removeAll}</li>
 *   <li>Behaviour of {@code isCaseInsensitive} and {@code getNewSet}</li>
 *   <li>Standard {@link Set} operations (size, isEmpty, clear, iterator, equals,
 *       hashCode, toString)</li>
 * </ul>
 */
public class CompactCILinkedSetTest {

    private CompactCILinkedSet<String> emptySet;
    private CompactCILinkedSet<String> populatedSet;

    @Before
    public void setUp() {
        emptySet = new CompactCILinkedSet<>();
        populatedSet = new CompactCILinkedSet<>(Arrays.asList("One", "Two", "Three"));
    }

    /* -------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------- */

    @Test
    public void testEmptyConstructorCreatesEmptySet() {
        assertTrue("Newly created set should be empty", emptySet.isEmpty());
        assertEquals("Size of a new set must be 0", 0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        Collection<String> source = Arrays.asList("Alpha", "Beta", "Gamma");
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>(source);
        assertEquals("All elements from source must be present", source.size(), set.size());
        for (String s : source) {
            assertTrue("Set must contain element '" + s + "'", set.contains(s));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNull() {
        new CompactCILinkedSet<String>(null);
    }

    @Test
    public void testCollectionConstructorDeduplicatesCaseInsensitive() {
        List<String> dupes = Arrays.asList("Case", "case", "CASE");
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>(dupes);
        assertEquals("Case‑insensitive duplicates should collapse to a single entry", 1, set.size());
        assertTrue(set.contains("cAsE"));
    }

    /* -------------------------------------------------
     *  Core contract tests (case‑insensitivity)
     * ------------------------------------------------- */

    @Test
    public void testIsCaseInsensitiveReturnsTrue() {
        assertTrue("CompactCILinkedSet must report case‑insensitive", emptySet.isCaseInsensitive());
    }

    @Test
    public void testAddIsCaseInsensitive() {
        assertTrue(emptySet.add("Hello"));
        assertFalse("Adding a case‑variant should return false (already present)", emptySet.add("hello"));
        assertEquals("Size must remain 1 after duplicate add", 1, emptySet.size());
        assertTrue(emptySet.contains("HeLLo"));
    }

    @Test
    public void testContainsIsCaseInsensitive() {
        emptySet.addAll(Arrays.asList("Apple", "Banana"));
        assertTrue(emptySet.contains("apple"));
        assertTrue(emptySet.contains("BANANA"));
        assertFalse(emptySet.contains("Cherry"));
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        populatedSet.add("Four");
        assertTrue(populatedSet.remove("four"));
        assertFalse("Element should be removed", populatedSet.contains("Four"));
        assertEquals(3, populatedSet.size());
    }

    @Test
    public void testRetainAllIsCaseInsensitive() {
        Set<String> toRetain = new HashSet<>(Arrays.asList("one", "TWO"));
        assertTrue(populatedSet.retainAll(toRetain));
        assertEquals("Only 'One' and 'Two' should remain", 2, populatedSet.size());
        assertTrue(populatedSet.contains("ONE"));
        assertTrue(populatedSet.contains("two"));
        assertFalse(populatedSet.contains("Three"));
    }

    @Test
    public void testRemoveAllIsCaseInsensitive() {
        Set<String> toRemove = new HashSet<>(Arrays.asList("ONE", "three"));
        assertTrue(populatedSet.removeAll(toRemove));
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("Two"));
        assertFalse(populatedSet.contains("One"));
        assertFalse(populatedSet.contains("Three"));
    }

    /* -------------------------------------------------
     *  Standard Set behaviour
     * ------------------------------------------------- */

    @Test
    public void testClearAndIsEmpty() {
        populatedSet.clear();
        assertTrue("Set must be empty after clear()", populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    @Test
    public void testIteratorOrderPreserved() {
        // CompactCILinkedSet preserves insertion order (linked behaviour)
        List<String> expected = Arrays.asList("One", "Two", "Three");
        List<String> actual = new ArrayList<>();
        for (String s : populatedSet) {
            actual.add(s);
        }
        assertEquals("Iterator must reflect insertion order", expected, actual);
    }

    @Test
    public void testEqualsAndHashCodeWithStandardSet() {
        Set<String> stdSet = new HashSet<>(Arrays.asList("one", "TWO", "three"));
        // Populate a CompactCILinkedSet with the same logical elements
        CompactCILinkedSet<String> ciSet = new CompactCILinkedSet<>(Arrays.asList("One", "Two", "Three"));
        assertEquals("Sets with same logical elements should be equal", stdSet, ciSet);
        assertEquals("Hash codes must match for equal sets", stdSet.hashCode(), ciSet.hashCode());
    }

    @Test
    public void testToArray() {
        Object[] array = populatedSet.toArray();
        assertArrayEquals("toArray must return all elements in insertion order",
                new Object[]{"One", "Two", "Three"}, array);
    }

    @Test
    public void testToArrayTyped() {
        String[] a = new String[0];
        String[] array = populatedSet.toArray(a);
        assertArrayEquals("Typed toArray must return all elements in insertion order",
                new String[]{"One", "Two", "Three"}, array);
    }

    @Test
    public void testToStringContainsElements() {
        String s = populatedSet.toString();
        assertTrue("String representation should contain 'One'", s.contains("One"));
        assertTrue("String representation should contain 'Two'", s.contains("Two"));
        assertTrue("String representation should contain 'Three'", s.contains("Three"));
    }

    /* -------------------------------------------------
     *  Protected / overridden method tests
     * ------------------------------------------------- */

    @Test
    public void testGetNewSetReturnsNull() {
        // The overridden method explicitly returns null – verify that contract.
        assertNull("getNewSet() is overridden to return null", emptySet.getNewSet());
    }
}
