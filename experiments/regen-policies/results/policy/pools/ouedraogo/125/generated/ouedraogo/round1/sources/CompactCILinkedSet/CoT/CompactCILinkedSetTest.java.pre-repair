package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCILinkedSet}.
 *
 * The tests cover:
 *   • All public constructors and methods inherited from {@link CompactSet}
 *   • Case‑insensitive behaviour of add / contains / remove, etc.
 *   • Edge cases such as null arguments, duplicate elements with different case,
 *     and order preservation (insertion order).
 */
public class CompactCILinkedSetTest {

    /** Helper subclass to expose protected members for testing. */
    private static class TestableCompactCILinkedSet<E> extends CompactCILinkedSet<E> {
        TestableCompactCILinkedSet() { super(); }
        TestableCompactCILinkedSet(Collection<E> c) { super(c); }

        /** Expose the deprecated {@code getNewSet()} method. */
        public Set<E> exposeGetNewSet() {
            return super.getNewSet();
        }

        /** Expose the protected {@code isCaseInsensitive()} method. */
        public boolean exposeIsCaseInsensitive() {
            return super.isCaseInsensitive();
        }
    }

    private TestableCompactCILinkedSet<String> set;

    @Before
    public void setUp() {
        set = new TestableCompactCILinkedSet<>();
    }

    /*** ----------------------------------------------------------------------
     *  1.  Public constructors
     * ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue("Newly constructed set should be empty", set.isEmpty());
        assertEquals("Size of new set must be 0", 0, set.size());
    }

    @Test
    public void testConstructorWithCollectionCopiesElementsCaseInsensitive() {
        List<String> source = Arrays.asList("One", "TWO", "three");
        TestableCompactCILinkedSet<String> s = new TestableCompactCILinkedSet<>(source);
        assertEquals(3, s.size());
        assertTrue(s.contains("one"));
        assertTrue(s.contains("two"));
        assertTrue(s.contains("THREE"));
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructorWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        new TestableCompactCILinkedSet<>(null);
    }

    /*** ----------------------------------------------------------------------
     *  2.  Basic Set operations – case‑insensitive behaviour
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsCaseInsensitive() {
        assertTrue(set.add("Apple"));
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("APPLE"));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddDuplicateDifferentCaseReturnsFalse() {
        assertTrue(set.add("Banana"));
        // Adding same value with different case must be considered duplicate
        assertFalse("Duplicate (different case) should not be added", set.add("BANANA"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        set.add("Cherry");
        assertTrue(set.remove("CHERRY"));
        assertFalse(set.contains("cherry"));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testAddNullThrowsNPE() {
        thrown.expect(NullPointerException.class);
        set.add(null);
    }

    /*** ----------------------------------------------------------------------
     *  3.  Bulk operations (addAll, containsAll, retainAll, removeAll)
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAllDeduplicatesIgnoringCaseAndPreservesInsertionOrder() {
        List<String> list = Arrays.asList("dog", "Dog", "cat", "CAT");
        assertTrue(set.addAll(list));
        // Only two distinct elements should exist
        assertEquals(2, set.size());
        // Insertion order: first distinct element is "dog", then "cat"
        Iterator<String> it = set.iterator();
        assertEquals("dog", it.next());
        assertEquals("cat", it.next());
    }

    @Test
    public void testContainsAllCaseInsensitive() {
        set.addAll(Arrays.asList("Elephant", "Frog"));
        Collection<String> query = Arrays.asList("elephant", "FROG");
        assertTrue(set.containsAll(query));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElementsCaseInsensitive() {
        set.addAll(Arrays.asList("Giraffe", "Hippo", "iguana"));
        Collection<String> toKeep = Arrays.asList("giraffe", "IGUANA");
        assertTrue(set.retainAll(toKeep));
        assertEquals(2, set.size());
        assertTrue(set.contains("GIRAFFE"));
        assertTrue(set.contains("iguana"));
        assertFalse(set.contains("hippo"));
    }

    @Test
    public void testRemoveAllCaseInsensitive() {
        set.addAll(Arrays.asList("Jaguar", "Kangaroo", "Llama"));
        Collection<String> toRemove = Arrays.asList("JAGUAR", "llama");
        assertTrue(set.removeAll(toRemove));
        assertEquals(1, set.size());
        assertTrue(set.contains("kangaroo"));
        assertFalse(set.contains("jaguar"));
        assertFalse(set.contains("llama"));
    }

    /*** ----------------------------------------------------------------------
     *  4.  Iterator, toArray and ordering
     * ---------------------------------------------------------------------- */

    @Test
    public void testIteratorProvidesInsertionOrder() {
        List<String> items = Arrays.asList("mango", "nectarine", "orange");
        set.addAll(items);
        Iterator<String> it = set.iterator();
        for (String expected : items) {
            assertTrue(it.hasNext());
            assertEquals(expected, it.next());
        }
        assertFalse(it.hasNext());
    }

    @Test
    public void testToArrayReturnsElementsInInsertionOrder() {
        List<String> items = Arrays.asList("pear", "quince", "raspberry");
        set.addAll(items);
        Object[] array = set.toArray();
        assertArrayEquals(items.toArray(), array);
    }

    @Test
    public void testToTypedArray() {
        List<String> items = Arrays.asList("strawberry", "tangerine");
        set.addAll(items);
        String[] a = set.toArray(new String[0]);
        assertArrayEquals(items.toArray(new String[0]), a);
    }

    /*** ----------------------------------------------------------------------
     *  5.  equals, hashCode and toString
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeCaseInsensitive() {
        CompactCILinkedSet<String> s1 = new CompactCILinkedSet<>();
        CompactCILinkedSet<String> s2 = new CompactCILinkedSet<>();

        s1.addAll(Arrays.asList("Ugli", "Vanilla"));
        s2.addAll(Arrays.asList("ugli", "VANILLA"));

        assertEquals("Sets with same elements (different case) must be equal", s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testToStringContainsElements() {
        set.addAll(Arrays.asList("watermelon", "xigua"));
        String s = set.toString();
        assertTrue(s.contains("watermelon"));
        assertTrue(s.contains("xigua"));
    }

    /*** ----------------------------------------------------------------------
     *  6.  Protected members exposure (sanity checks)
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsCaseInsensitiveProtectedMethod() {
        assertTrue("CompactCILinkedSet must report case‑insensitive", set.exposeIsCaseInsensitive());
    }

    @Test
    public void testDeprecatedGetNewSetReturnsNull() {
        assertNull("Deprecated getNewSet() should return null", set.exposeGetNewSet());
    }

    /*** ----------------------------------------------------------------------
     *  7.  Clear and basic size/emptiness checks
     * ---------------------------------------------------------------------- */

    @Test
    public void testClearEmptiesTheSet() {
        set.addAll(Arrays.asList("yuzu", "zucchini"));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }
}
