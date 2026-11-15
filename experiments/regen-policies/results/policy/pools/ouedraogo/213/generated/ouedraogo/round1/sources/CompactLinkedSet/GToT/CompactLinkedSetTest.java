package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * JUnit‑4 test suite for {@link CompactLinkedSet}.
 *
 * The tests cover:
 *  1. Public constructors.
 *  2. Behaviour inherited from {@link CompactSet} (add, remove, contains, iterator, etc.).
 *  3. Case‑insensitive semantics (the core feature of this subclass).
 *  4. Edge‑cases such as null arguments, duplicate elements, empty collections,
 *     and case‑variance handling.
 *  5. Protected methods {@code isCaseInsensitive()} and {@code getNewSet()}
 *     accessed via a small test‑only subclass.
 */
public class CompactLinkedSetTest {

    /** Simple subclass that exposes the protected methods for testing. */
    private static class TestableCompactLinkedSet<E> extends CompactLinkedSet<E> {
        TestableCompactLinkedSet() { super(); }
        TestableCompactLinkedSet(Collection<E> c) { super(c); }

        /** Expose the protected {@code isCaseInsensitive()} */
        boolean exposedIsCaseInsensitive() {
            return super.isCaseInsensitive();
        }

        /** Expose the protected {@code getNewSet()} */
        Set<E> exposedGetNewSet() {
            return super.getNewSet();
        }
    }

    private TestableCompactLinkedSet<String> set;

    @Before
    public void setUp() {
        set = new TestableCompactLinkedSet<>();
    }

    /* --------------------------------------------------------------
     *  Constructor tests
     * -------------------------------------------------------------- */

    @Test
    public void testEmptyConstructorCreatesEmptySet() {
        assertTrue("Newly constructed set should be empty", set.isEmpty());
        assertEquals("Size of new set should be 0", 0, set.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        List<String> src = Arrays.asList("One", "Two", "Three");
        TestableCompactLinkedSet<String> s = new TestableCompactLinkedSet<>(src);
        assertEquals("Size must match source collection size", src.size(), s.size());
        for (String e : src) {
            assertTrue("Set must contain element '" + e + "'", s.contains(e));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNull() {
        new TestableCompactLinkedSet<String>(null);
    }

    @Test
    public void testCollectionConstructorHandlesDuplicatesCaseInsensitively() {
        List<String> src = Arrays.asList("Apple", "apple", "APPLE");
        TestableCompactLinkedSet<String> s = new TestableCompactLinkedSet<>(src);
        assertEquals("Duplicates differing only by case must collapse to a single entry", 1, s.size());
        assertTrue(s.contains("Apple"));
    }

    /* --------------------------------------------------------------
     *  Core Set operations – case‑insensitive behaviour
     * -------------------------------------------------------------- */

    @Test
    public void testAddAndContainsCaseInsensitive() {
        assertTrue(set.add("Hello"));
        assertTrue(set.contains("hello"));   // different case
        assertFalse(set.add("HELLO"));       // duplicate (case‑insensitive)
        assertEquals(1, set.size());
    }

    @Test
    public void testRemoveCaseInsensitive() {
        set.add("World");
        assertTrue(set.remove("world"));   // different case
        assertFalse(set.contains("World"));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testAddNullElement() {
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());

        // Adding null again must be reported as duplicate
        assertFalse(set.add(null));
        assertEquals(1, set.size());
    }

    @Test
    public void testIteratorPreservesInsertionOrder() {
        List<String> elements = Arrays.asList("first", "second", "third");
        for (String e : elements) {
            set.add(e);
        }
        Iterator<String> it = set.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals("Iterator must follow insertion order", elements, iterated);
    }

    @Test
    public void testClearEmptiesSet() {
        set.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testAddAllWithDuplicatesAndNulls() {
        List<String> src = Arrays.asList("One", "ONE", null, "Two", null);
        assertTrue(set.addAll(src));
        assertEquals(3, set.size()); // "One"/"ONE" collapse, two nulls collapse
        assertTrue(set.contains("one"));
        assertTrue(set.contains(null));
        assertTrue(set.contains("Two"));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        set.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        Collection<String> keep = Arrays.asList("beta", "delta");
        assertTrue(set.retainAll(keep));
        assertEquals(1, set.size());
        assertTrue(set.contains("Beta"));
    }

    @Test
    public void testRemoveAllEliminatesSpecifiedElementsCaseInsensitive() {
        set.addAll(Arrays.asList("Cat", "Dog", "Fish"));
        Collection<String> remove = Arrays.asList("dog", "FISH");
        assertTrue(set.removeAll(remove));
        assertEquals(1, set.size());
        assertTrue(set.contains("Cat"));
    }

    @Test
    public void testContainsAllIsCaseInsensitive() {
        set.addAll(Arrays.asList("Red", "Green", "Blue"));
        Collection<String> query = Arrays.asList("red", "GREEN");
        assertTrue(set.containsAll(query));
    }

    /* --------------------------------------------------------------
     *  Protected method exposure tests
     * -------------------------------------------------------------- */

    @Test
    public void testIsCaseInsensitiveReturnsTrue() {
        assertTrue("CompactLinkedSet must report case‑insensitive", set.exposedIsCaseInsensitive());
    }

    @Test
    public void testGetNewSetReturnsNullAsDocumented() {
        assertNull("getNewSet() is documented to return null", set.exposedGetNewSet());
    }

    /* --------------------------------------------------------------
     *  Equality and hashCode (inherited) – sanity checks
     * -------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeRespectCaseInsensitivity() {
        TestableCompactLinkedSet<String> s1 = new TestableCompactLinkedSet<>();
        TestableCompactLinkedSet<String> s2 = new TestableCompactLinkedSet<>();

        s1.add("Key");
        s2.add("key");

        assertEquals("Sets with case‑insensitive equal elements must be equal", s1, s2);
        assertEquals("Hash codes must match for equal sets", s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testToStringContainsElements() {
        set.addAll(Arrays.asList("x", "y"));
        String str = set.toString();
        assertTrue("String representation must contain element 'x'", str.contains("x"));
        assertTrue("String representation must contain element 'y'", str.contains("y"));
    }
}
