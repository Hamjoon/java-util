package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCIHashSet}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>All public constructors of {@code CompactCIHashSet}.</li>
 *   <li>Inherited {@link Set} operations (add, remove, contains, etc.).</li>
 *   <li>Case‑insensitive behavior specific to this subclass.</li>
 *   <li>Edge‑case scenarios such as null arguments, duplicate elements,
 *       empty collections, and interaction with other {@code Set}
 *       implementations.</li>
 *   <li>Verification of the deprecated {@code getNewSet()} method via reflection.</li>
 * </ul>
 * </p>
 */
public class CompactCIHashSetTest {

    private CompactCIHashSet<String> ciSet;

    @Before
    public void setUp() {
        ciSet = new CompactCIHashSet<>();
    }

    /*** ----------- Constructor Tests ----------------------------------- */

    @Test
    public void testNoArgConstructorCreatesEmptySet() {
        CompactCIHashSet<Integer> set = new CompactCIHashSet<>();
        assertTrue("Set should be empty after no‑arg construction", set.isEmpty());
        assertEquals("Size should be 0", 0, set.size());
    }

    @Test
    public void testConstructorWithCollectionCopiesElements() {
        List<String> source = Arrays.asList("One", "Two", "Three");
        CompactCIHashSet<String> set = new CompactCIHashSet<>(source);
        assertEquals("All elements should be copied", source.size(), set.size());
        for (String s : source) {
            assertTrue("Set should contain '" + s + "'", set.contains(s));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE() {
        new CompactCIHashSet<>(null);
    }

    /*** ----------- Case‑Insensitive Behavior --------------------------- */

    @Test
    public void testCaseInsensitiveAddAndContains() {
        ciSet.add("Apple");
        assertTrue("Contains should be case‑insensitive (lower)", ciSet.contains("apple"));
        assertTrue("Contains should be case‑insensitive (upper)", ciSet.contains("APPLE"));
        assertTrue("Contains should be case‑insensitive (mixed)", ciSet.contains("ApPlE"));
    }

    @Test
    public void testCaseInsensitiveDuplicateIsIgnored() {
        assertTrue(ciSet.add("Banana"));
        assertFalse("Adding a case‑variant duplicate should return false", ciSet.add("BANANA"));
        assertEquals("Set size should remain 1 after duplicate add", 1, ciSet.size());
    }

    /*** ----------- Basic Set Operations -------------------------------- */

    @Test
    public void testAddRemoveContains() {
        assertTrue(ciSet.add("Cat"));
        assertTrue(ciSet.contains("cat"));
        assertTrue(ciSet.remove("CAT"));
        assertFalse(ciSet.contains("cat"));
    }

    @Test
    public void testRemoveNonExistingElement() {
        assertFalse("Removing element that is not present should return false", ciSet.remove("nonexistent"));
    }

    @Test
    public void testAddAllAndContainsAll() {
        List<String> list = Arrays.asList("Dog", "Elephant", "Frog");
        assertTrue(ciSet.addAll(list));
        assertTrue(ciSet.containsAll(list));
        // case‑insensitive check
        assertTrue(ciSet.containsAll(Arrays.asList("dog", "ELEPHANT", "fRoG")));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        ciSet.addAll(Arrays.asList("Giraffe", "Hippo", "Iguana"));
        boolean changed = ciSet.retainAll(Arrays.asList("hippo", "iguana", "jaguar"));
        assertTrue("retainAll should report change", changed);
        assertEquals("Size after retainAll", 2, ciSet.size());
        assertTrue(ciSet.contains("HIPPO"));
        assertTrue(ciSet.contains("iguana"));
        assertFalse(ciSet.contains("giraffe"));
    }

    @Test
    public void testRemoveAllRemovesSpecifiedElements() {
        ciSet.addAll(Arrays.asList("Koala", "Lemur", "Mongoose"));
        boolean changed = ciSet.removeAll(Arrays.asList("LEMU R", "mongoose"));
        assertTrue("removeAll should report change", changed);
        assertEquals("Size after removeAll", 1, ciSet.size());
        assertTrue(ciSet.contains("koala"));
        assertFalse(ciSet.contains("lemur"));
        assertFalse(ciSet.contains("mongoose"));
    }

    @Test
    public void testClearEmptiesSet() {
        ciSet.addAll(Arrays.asList("Newt", "Octopus"));
        ciSet.clear();
        assertTrue("Set should be empty after clear", ciSet.isEmpty());
        assertEquals(0, ciSet.size());
    }

    @Test
    public void testIteratorTraversal() {
        List<String> items = Arrays.asList("Penguin", "Quail", "Rabbit");
        ciSet.addAll(items);
        Set<String> iterated = new HashSet<>();
        for (String s : ciSet) {
            iterated.add(s);
        }
        // iterator respects case‑insensitivity by returning the stored form
        assertEquals("Iterator should have visited all elements", new HashSet<>(items), iterated);
    }

    @Test
    public void testToArray() {
        ciSet.addAll(Arrays.asList("Snake", "Tiger"));
        Object[] arr = ciSet.toArray();
        assertEquals(2, arr.length);
        assertTrue(Arrays.asList(arr).containsAll(Arrays.asList("Snake", "Tiger")));
    }

    @Test
    public void testToArrayTyped() {
        ciSet.addAll(Arrays.asList("Umbrella", "Vulture"));
        String[] arr = ciSet.toArray(new String[0]);
        assertEquals(2, arr.length);
        assertTrue(Arrays.asList(arr).containsAll(Arrays.asList("Umbrella", "Vulture")));
    }

    /*** ----------- Equality, HashCode and toString ---------------------- */

    @Test
    public void testEqualsAndHashCodeWithAnotherSet() {
        Set<String> other = new HashSet<>(Arrays.asList("wolf", "Xerus"));
        ciSet.addAll(other);
        assertTrue("CompactCIHashSet should be equal to a standard Set with same elements (case‑insensitive)",
                ciSet.equals(other));
        assertEquals("Hash codes should match for equal sets", other.hashCode(), ciSet.hashCode());
    }

    @Test
    public void testToStringContainsElements() {
        ciSet.addAll(Arrays.asList("Yak", "Zebra"));
        String s = ciSet.toString();
        assertTrue("toString should contain 'Yak'", s.contains("Yak"));
        assertTrue("toString should contain 'Zebra'", s.contains("Zebra"));
    }

    /*** ----------- Deprecated / Protected Method Tests ------------------- */

    @Test
    public void testGetNewSetIsDeprecatedAndReturnsNull() throws Exception {
        Method m = CompactCIHashSet.class.getDeclaredMethod("getNewSet");
        m.setAccessible(true);
        Object result = m.invoke(ciSet);
        assertNull("Deprecated getNewSet() should return null", result);
    }

    /*** ----------- Edge Cases ------------------------------------------- */

    @Test
    public void testAddNullElement() {
        // CompactSet historically allows null; verify behaviour.
        assertTrue("Adding null should succeed", ciSet.add(null));
        assertTrue("Set should contain null after addition", ciSet.contains(null));
        assertEquals("Size should be 1 after adding null", 1, ciSet.size());

        // Adding another null should be ignored (duplicate)
        assertFalse("Adding null again should return false", ciSet.add(null));
        assertEquals("Size should remain 1 after duplicate null add", 1, ciSet.size());

        // Removing null
        assertTrue("Removing null should succeed", ciSet.remove(null));
        assertFalse("Set should no longer contain null", ciSet.contains(null));
    }

    @Test
    public void testAddAllWithNullCollectionThrowsNPE() {
        try {
            ciSet.addAll(null);
            fail("addAll(null) should throw NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testRetainAllWithEmptyCollectionEmptiesSet() {
        ciSet.addAll(Arrays.asList("alpha", "beta"));
        boolean changed = ciSet.retainAll(Collections.emptyList());
        assertTrue("retainAll(empty) should report change", changed);
        assertTrue("Set should be empty after retainAll(empty)", ciSet.isEmpty());
    }

    @Test
    public void testRemoveAllWithEmptyCollectionDoesNotChangeSet() {
        ciSet.addAll(Arrays.asList("gamma", "delta"));
        boolean changed = ciSet.removeAll(Collections.emptyList());
        assertFalse("removeAll(empty) should report no change", changed);
        assertEquals("Size should stay unchanged", 2, ciSet.size());
    }

    @Test
    public void testContainsWithNullReturnsTrueIfNullPresent() {
        ciSet.add(null);
        assertTrue("contains(null) should be true after null added", ciSet.contains(null));
    }

    @Test
    public void testContainsWithNullReturnsFalseIfNotPresent() {
        assertFalse("contains(null) should be false when null not present", ciSet.contains(null));
    }
}
