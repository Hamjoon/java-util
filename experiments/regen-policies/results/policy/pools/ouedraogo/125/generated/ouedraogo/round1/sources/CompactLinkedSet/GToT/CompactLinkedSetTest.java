package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactLinkedSet}.
 *
 * The tests cover:
 *   1. All public constructors.
 *   2. All public methods inherited from {@link CompactSet} that are relevant for a Set implementation.
 *   3. Typical usage scenarios.
 *   4. Edge‑case and exception handling (null arguments, duplicate elements, case‑insensitivity, etc.).
 *
 * The test class follows the “three‑expert” process described in the prompt:
 *   – each method is listed,
 *   – a basic test is provided,
 *   – edge‑case tests are added,
 *   – everything is merged into this single file.
 */
public class CompactLinkedSetTest {

    /** The set instance used by most tests. */
    private CompactLinkedSet<String> set;

    @Before
    public void setUp() {
        set = new CompactLinkedSet<>();
    }

    /* -------------------------------------------------
     *  List of public constructors / methods (signatures)
     * -------------------------------------------------
     *
     * Constructors
     *   CompactLinkedSet()
     *   CompactLinkedSet(Collection<E> other)
     *
     * Inherited public Set methods (from CompactSet)
     *   int size()
     *   boolean isEmpty()
     *   boolean contains(Object o)
     *   boolean add(E e)
     *   boolean remove(Object o)
     *   void clear()
     *   boolean containsAll(Collection<?> c)
     *   boolean addAll(Collection<? extends E> c)
     *   boolean retainAll(Collection<?> c)
     *   boolean removeAll(Collection<?> c)
     *   Iterator<E> iterator()
     *   Object[] toArray()
     *   <T> T[] toArray(T[] a)
     *   boolean equals(Object o)
     *   int hashCode()
     *   String toString()
     *
     * -----------------------------------------------------------------
     *  Basic test cases (one per method) – they verify the normal flow.
     * -----------------------------------------------------------------
     */

    /* ---------- Constructors ---------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        CompactLinkedSet<Integer> empty = new CompactLinkedSet<>();
        assertTrue("Newly created set should be empty", empty.isEmpty());
        assertEquals(0, empty.size());
    }

    @Test
    public void testConstructorWithCollectionCopiesElements() {
        List<String> src = Arrays.asList("one", "two", "three");
        CompactLinkedSet<String> fromColl = new CompactLinkedSet<>(src);
        assertEquals(3, fromColl.size());
        assertTrue(fromColl.containsAll(src));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE() {
        new CompactLinkedSet<String>(null);
    }

    /* ---------- size() & isEmpty() ---------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        set.add("a");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
    }

    /* ---------- add(E) & contains(Object) ---------- */

    @Test
    public void testAddAndContains() {
        assertTrue(set.add("Apple"));
        assertTrue(set.contains("Apple"));
        // duplicate add must return false and not change size
        assertFalse(set.add("Apple"));
        assertEquals(1, set.size());
    }

    @Test
    public void testCaseInsensitiveContains() {
        set.add("Apple");
        assertTrue("Contains should be case‑insensitive", set.contains("APPLE"));
        assertTrue(set.contains("apple"));
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullThrowsNPE() {
        set.add(null);
    }

    /* ---------- remove(Object) ---------- */

    @Test
    public void testRemoveExistingAndNonExisting() {
        set.add("A");
        set.add("B");
        assertTrue(set.remove("A"));
        assertFalse(set.contains("A"));
        assertEquals(1, set.size());

        // removing something that isn’t present should return false
        assertFalse(set.remove("C"));
    }

    /* ---------- clear() ---------- */

    @Test
    public void testClearEmptiesSet() {
        set.addAll(Arrays.asList("x", "y", "z"));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* ---------- addAll(Collection) ---------- */

    @Test
    public void testAddAll() {
        List<String> list = Arrays.asList("one", "two", "three");
        assertTrue(set.addAll(list));
        assertEquals(3, set.size());
        // duplicate elements in the collection should be ignored
        List<String> withDup = Arrays.asList("three", "four");
        assertTrue(set.addAll(withDup));
        assertEquals(4, set.size());
        assertTrue(set.containsAll(Arrays.asList("one", "two", "three", "four")));
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllNullCollectionThrowsNPE() {
        set.addAll(null);
    }

    /* ---------- containsAll(Collection) ---------- */

    @Test
    public void testContainsAll() {
        set.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(set.containsAll(Arrays.asList("A", "B"))); // case‑insensitive
        assertFalse(set.containsAll(Arrays.asList("a", "d")));
    }

    /* ---------- retainAll(Collection) ---------- */

    @Test
    public void testRetainAll() {
        set.addAll(Arrays.asList("keep1", "keep2", "remove"));
        assertTrue(set.retainAll(Arrays.asList("KEEP1", "keep2"))); // case‑insensitive match
        assertEquals(2, set.size());
        assertTrue(set.contains("keep1"));
        assertTrue(set.contains("keep2"));
        assertFalse(set.contains("remove"));
    }

    @Test(expected = NullPointerException.class)
    public void testRetainAllNullCollectionThrowsNPE() {
        set.retainAll(null);
    }

    /* ---------- removeAll(Collection) ---------- */

    @Test
    public void testRemoveAll() {
        set.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(set.removeAll(Arrays.asList("B", "C"))); // case‑insensitive
        assertEquals(1, set.size());
        assertTrue(set.contains("a"));
        assertFalse(set.contains("b"));
        assertFalse(set.contains("c"));
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveAllNullCollectionThrowsNPE() {
        set.removeAll(null);
    }

    /* ---------- iterator() & insertion order ---------- */

    @Test
    public void testIteratorPreservesInsertionOrder() {
        List<String> order = Arrays.asList("first", "second", "third");
        set.addAll(order);
        Iterator<String> it = set.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(order, iterated);
    }

    /* ---------- toArray() ---------- */

    @Test
    public void testToArray() {
        set.addAll(Arrays.asList("x", "y"));
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[]{"x", "y"}, arr);
    }

    @Test
    public void testToArrayTyped() {
        set.addAll(Arrays.asList("p", "q"));
        String[] arr = set.toArray(new String[0]);
        assertArrayEquals(new String[]{"p", "q"}, arr);
    }

    /* ---------- equals(Object) & hashCode() ---------- */

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedSet<String> s1 = new CompactLinkedSet<>();
        CompactLinkedSet<String> s2 = new CompactLinkedSet<>();

        s1.addAll(Arrays.asList("a", "b"));
        s2.addAll(Arrays.asList("A", "B")); // different case but should be equal

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        s2.add("c");
        assertNotEquals(s1, s2);
    }

    /* ---------- toString() ---------- */

    @Test
    public void testToStringContainsElements() {
        set.addAll(Arrays.asList("one", "two"));
        String s = set.toString();
        assertTrue(s.contains("one"));
        assertTrue(s.contains("two"));
    }

    /* -------------------------------------------------
     *  Additional edge‑case tests discovered during review
     * -------------------------------------------------
     */

    @Test
    public void testAddAllWithEmptyCollectionReturnsFalse() {
        assertFalse(set.addAll(Collections.emptyList()));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRetainAllWhenNothingIsRetained() {
        set.addAll(Arrays.asList("a", "b"));
        assertTrue(set.retainAll(Collections.emptyList()));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRemoveAllWhenNoElementsMatch() {
        set.addAll(Arrays.asList("x", "y"));
        assertFalse(set.removeAll(Arrays.asList("z")));
        assertEquals(2, set.size());
    }

    @Test
    public void testContainsNullReturnsFalse() {
        assertFalse(set.contains(null));
    }

    @Test
    public void testIteratorRemoveSupported() {
        set.addAll(Arrays.asList("del", "keep"));
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("del".equalsIgnoreCase(val)) {
                it.remove();
            }
        }
        assertFalse(set.contains("del"));
        assertTrue(set.contains("keep"));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddAllWithNullElementInCollection() {
        List<String> list = new ArrayList<>();
        list.add("good");
        list.add(null);
        try {
            set.addAll(list);
            fail("Expected NullPointerException when adding a collection containing null");
        } catch (NullPointerException e) {
            // Expected – the underlying CompactSet does not allow null elements.
        }
    }
}
