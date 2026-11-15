package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactLinkedSet}.
 *
 * The tests are organised as follows:
 *
 * 1. **Public API extraction** – the only public members of {@code CompactLinkedSet}
 *    are the two constructors.
 *
 * 2. **Basic functionality tests** – verify that the constructors create a usable
 *    {@link Set} and that the inherited {@link CompactSet} operations work as
 *    expected (add, remove, contains, iteration order, etc.).
 *
 * 3. **Edge‑case / exception tests** – cover null arguments, case‑insensitive
 *    behaviour, duplicate handling, and the behaviour of the protected
 *    {@code getNewSet()} method (exposed via a small test‑only subclass).
 *
 * All tests run with JUnit 4.
 */
public class CompactLinkedSetTest {

    /** A helper subclass that makes the protected {@code getNewSet()} method visible. */
    private static class ExposedCompactLinkedSet<E> extends CompactLinkedSet<E> {
        ExposedCompactLinkedSet() { super(); }
        @Override
        public Set<E> exposeGetNewSet() {
            return super.getNewSet();
        }
    }

    private CompactLinkedSet<String> emptySet;
    private CompactLinkedSet<String> prepopulatedSet;

    @Before
    public void setUp() {
        emptySet = new CompactLinkedSet<>();
        prepopulatedSet = new CompactLinkedSet<>(Arrays.asList("Alpha", "Beta"));
    }

    /* -------------------------------------------------
     *  1.  Public constructors
     * ------------------------------------------------- */

    @Test
    public void testNoArgConstructorCreatesEmptySet() {
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        List<String> source = Arrays.asList("One", "Two", "Three");
        CompactLinkedSet<String> set = new CompactLinkedSet<>(source);

        assertEquals(source.size(), set.size());
        // order must be preserved because it behaves like a LinkedHashSet
        Iterator<String> it = set.iterator();
        for (String expected : source) {
            assertTrue(it.hasNext());
            assertEquals(expected, it.next());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNull() {
        new CompactLinkedSet<String>(null);
    }

    /* -------------------------------------------------
     *  2.  Basic Set operations inherited from CompactSet
     * ------------------------------------------------- */

    @Test
    public void testAddAndContains() {
        assertTrue(emptySet.add("Hello"));
        assertTrue(emptySet.contains("Hello"));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testAddDuplicateIsIgnored() {
        assertTrue(emptySet.add("dup"));
        // second add returns false because the element already exists (case‑insensitive)
        assertFalse(emptySet.add("dup"));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testCaseInsensitiveAddAndContains() {
        assertTrue(emptySet.add("Test"));
        // different case should be considered the same element
        assertFalse(emptySet.add("test"));
        assertTrue(emptySet.contains("TEST"));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testRemove() {
        emptySet.add("removeMe");
        assertTrue(emptySet.remove("removeMe"));
        assertFalse(emptySet.contains("removeMe"));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testClear() {
        prepopulatedSet.clear();
        assertTrue(prepopulatedSet.isEmpty());
        assertEquals(0, prepopulatedSet.size());
    }

    @Test
    public void testAddAllPreservesOrderAndCaseInsensitivity() {
        List<String> extra = Arrays.asList("Gamma", "ALPHA", "Delta");
        assertTrue(prepopulatedSet.addAll(extra));

        // "ALPHA" is a duplicate of "Alpha" (case‑insensitive) and should not increase size
        assertEquals(4, prepopulatedSet.size());

        // Expected iteration order: Alpha, Beta, Gamma, Delta
        List<String> expected = Arrays.asList("Alpha", "Beta", "Gamma", "Delta");
        assertEquals(expected, new ArrayList<>(prepopulatedSet));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        List<String> keep = Arrays.asList("alpha", "BETA"); // different case
        assertTrue(prepopulatedSet.retainAll(keep));
        assertEquals(2, prepopulatedSet.size());
        assertTrue(prepopulatedSet.contains("Alpha"));
        assertTrue(prepopulatedSet.contains("Beta"));
    }

    @Test
    public void testRemoveAllEliminatesElements() {
        List<String> toRemove = Arrays.asList("ALPHA");
        assertTrue(prepopulatedSet.removeAll(toRemove));
        assertEquals(1, prepopulatedSet.size());
        assertFalse(prepopulatedSet.contains("Alpha"));
        assertTrue(prepopulatedSet.contains("Beta"));
    }

    @Test
    public void testToArray() {
        Object[] array = prepopulatedSet.toArray();
        assertArrayEquals(new Object[]{"Alpha", "Beta"}, array);
    }

    @Test
    public void testToTypedArray() {
        String[] array = prepopulatedSet.toArray(new String[0]);
        assertArrayEquals(new String[]{"Alpha", "Beta"}, array);
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedSet<String> another = new CompactLinkedSet<>(Arrays.asList("Alpha", "Beta"));
        assertEquals(prepopulatedSet, another);
        assertEquals(prepopulatedSet.hashCode(), another.hashCode());

        another.add("Gamma");
        assertNotEquals(prepopulatedSet, another);
    }

    @Test
    public void testToStringContainsElements() {
        String s = prepopulatedSet.toString();
        assertTrue(s.contains("Alpha"));
        assertTrue(s.contains("Beta"));
    }

    /* -------------------------------------------------
     *  3.  Edge cases / protected method exposure
     * ------------------------------------------------- */

    @Test
    public void testGetNewSetReturnsNull() {
        ExposedCompactLinkedSet<String> exposed = new ExposedCompactLinkedSet<>();
        assertNull("CompactLinkedSet#getNewSet() is documented to return null", exposed.exposeGetNewSet());
    }

    @Test
    public void testAddNullElement() {
        // CompactSet (and by extension CompactLinkedSet) follows the Set contract:
        // adding null is allowed unless the underlying map forbids it.
        // The implementation used here (CompactMap) permits null keys, so we assert true.
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        assertEquals(1, emptySet.size());

        // Adding null again should be ignored
        assertFalse(emptySet.add(null));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testContainsNullWhenNotPresent() {
        assertFalse(emptySet.contains(null));
    }

    @Test
    public void testRemoveNullWhenNotPresent() {
        assertFalse(emptySet.remove(null));
    }

    @Test
    public void testIteratorRemoveSupported() {
        emptySet.addAll(Arrays.asList("A", "B", "C"));
        Iterator<String> it = emptySet.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("B".equalsIgnoreCase(val)) {
                it.remove();
            }
        }
        assertFalse(emptySet.contains("B"));
        assertEquals(2, emptySet.size());
        assertTrue(emptySet.contains("A"));
        assertTrue(emptySet.contains("C"));
    }
}
