package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCILinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors.</li>
 *   <li>Standard {@link Set} operations with a focus on case‑insensitive behavior.</li>
 *   <li>Edge‑cases such as {@code null} elements and duplicate values differing only by case.</li>
 *   <li>Exception scenarios (e.g., {@code NullPointerException} when a {@code null} collection is supplied).</li>
 *   <li>Correct ordering guaranteed by the linked‑set implementation.</li>
 * </ul>
 */
public class CompactCILinkedSetTest {

    private CompactCILinkedSet<String> set;

    @Before
    public void setUp() {
        set = new CompactCILinkedSet<>();
    }

    /** -----------------------------------------------------------------------
     *  PUBLIC CONSTRUCTORS
     *  ---------------------------------------------------------------------*/

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue("New set should be empty", set.isEmpty());
        assertEquals("Size of a newly created set must be 0", 0, set.size());
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructor_NullCollection_ThrowsNPE() {
        new CompactCILinkedSet<String>(null);
    }

    @Test
    public void testCollectionConstructor_CaseInsensitivePopulation() {
        List<String> source = Arrays.asList("One", "Two", "ONE", "two", "Three");
        CompactCILinkedSet<String> s = new CompactCILinkedSet<>(source);
        // "One"/"ONE" are same, "Two"/"two" are same => total 3 distinct values
        assertEquals(3, s.size());
        assertTrue(s.contains("one"));
        assertTrue(s.contains("TWO"));
        assertTrue(s.contains("three"));
    }

    /** -----------------------------------------------------------------------
     *  BASIC SET OPERATIONS – CASE INSENSITIVE Behaviour
     *  ---------------------------------------------------------------------*/

    @Test
    public void testAddCaseInsensitive_DuplicateDifferentCase() {
        assertTrue(set.add("Apple"));
        assertFalse("Adding same value with different case must return false", set.add("apple"));
        assertEquals(1, set.size());
        assertTrue(set.contains("APPLE"));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        set.add("Banana");
        assertTrue("Removal should be case‑insensitive", set.remove("BANANA"));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testContainsCaseInsensitive() {
        set.add("Cherry");
        assertTrue(set.contains("cherry"));
        assertTrue(set.contains("CHERRY"));
        assertFalse(set.contains("Berry"));
    }

    /** -----------------------------------------------------------------------
     *  NULL ELEMENT HANDLING
     *  ---------------------------------------------------------------------*/

    @Test
    public void testAddNullElement() {
        assertTrue("First null addition should succeed", set.add(null));
        assertFalse("Second null addition must be rejected (set semantics)", set.add(null));
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    public void testRemoveNullElement() {
        set.add(null);
        assertTrue("Removing null should succeed", set.remove(null));
        assertFalse("Set should be empty after removal of null", set.contains(null));
        assertEquals(0, set.size());
    }

    /** -----------------------------------------------------------------------
     *  ORDER PRESERVATION (Linked behaviour)
     *  ---------------------------------------------------------------------*/

    @Test
    public void testIteratorPreservesInsertionOrder() {
        List<String> values = Arrays.asList("first", "second", "third");
        for (String v : values) {
            set.add(v);
        }
        Iterator<String> it = set.iterator();
        for (String expected : values) {
            assertTrue("Iterator should have next element", it.hasNext());
            assertEquals(expected, it.next());
        }
        assertFalse("Iterator should be exhausted", it.hasNext());
    }

    /** -----------------------------------------------------------------------
     *  BULK OPERATIONS (addAll, retainAll, removeAll) – CASE INSENSITIVE
     *  ---------------------------------------------------------------------*/

    @Test
    public void testAddAllCaseInsensitive() {
        List<String> src = Arrays.asList("alpha", "Beta", "ALPHA");
        assertTrue(set.addAll(src));
        // "alpha"/"ALPHA" are the same → only two distinct entries
        assertEquals(2, set.size());
        assertTrue(set.contains("ALPHA"));
        assertTrue(set.contains("beta"));
    }

    @Test
    public void testRetainAllCaseInsensitive() {
        set.addAll(Arrays.asList("A", "B", "C"));
        Collection<String> toRetain = Arrays.asList("a", "c");
        assertTrue(set.retainAll(toRetain));
        assertEquals(2, set.size());
        assertTrue(set.contains("A"));
        assertTrue(set.contains("C"));
        assertFalse(set.contains("B"));
    }

    @Test
    public void testRemoveAllCaseInsensitive() {
        set.addAll(Arrays.asList("X", "Y", "Z"));
        Collection<String> toRemove = Arrays.asList("x", "Z");
        assertTrue(set.removeAll(toRemove));
        assertEquals(1, set.size());
        assertTrue(set.contains("Y"));
        assertFalse(set.contains("X"));
        assertFalse(set.contains("Z"));
    }

    /** -----------------------------------------------------------------------
     *  ARRAY CONVERSION
     *  ---------------------------------------------------------------------*/

    @Test
    public void testToArrayPreservesOrder() {
        set.addAll(Arrays.asList("one", "two", "three"));
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[]{"one", "two", "three"}, arr);
    }

    @Test
    public void testToTypedArrayPreservesOrder() {
        set.addAll(Arrays.asList("red", "green", "blue"));
        String[] arr = set.toArray(new String[0]);
        assertArrayEquals(new String[]{"red", "green", "blue"}, arr);
    }

    /** -----------------------------------------------------------------------
     *  EQUALITY & HASHCODE – CASE INSENSITIVE Equivalence
     *  ---------------------------------------------------------------------*/

    @Test
    public void testEqualsAndHashCodeCaseInsensitive() {
        CompactCILinkedSet<String> s1 = new CompactCILinkedSet<>();
        CompactCILinkedSet<String> s2 = new CompactCILinkedSet<>();

        s1.addAll(Arrays.asList("dog", "cat"));
        s2.addAll(Arrays.asList("DOG", "CAT"));

        assertEquals("Sets with same logical elements must be equal", s1, s2);
        assertEquals("Equal sets must have same hashCode", s1.hashCode(), s2.hashCode());
    }

    /** -----------------------------------------------------------------------
     *  CONFIGURATION (inherited from CompactSet)
     *  ---------------------------------------------------------------------*/

    @Test
    public void testWithConfigReturnsSameImplementation() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("someKey", "someValue");

        CompactSet<String> configured = set.withConfig(cfg);
        assertTrue("withConfig must return a CompactCILinkedSet instance",
                configured instanceof CompactCILinkedSet);
        assertEquals("Configuration map must be stored", cfg, configured.getConfig());
    }

    /** -----------------------------------------------------------------------
     *  TO_STRING REPRESENTATION (basic sanity check)
     *  ---------------------------------------------------------------------*/

    @Test
    public void testToStringContainsElements() {
        set.addAll(Arrays.asList("alpha", "beta"));
        String s = set.toString();
        assertTrue("String representation should contain 'alpha'", s.contains("alpha"));
        assertTrue("String representation should contain 'beta'", s.contains("beta"));
    }
}
