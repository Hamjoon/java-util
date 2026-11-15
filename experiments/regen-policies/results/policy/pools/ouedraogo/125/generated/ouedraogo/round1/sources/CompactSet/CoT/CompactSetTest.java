package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactSet}.
 *
 * The tests are organised in three logical sections:
 *
 * 1. **Public API extraction** – a comment that lists every public method/constructor
 *    together with its signature.
 *
 * 2. **Basic functionality tests** – one simple test per method that checks the
 *    normal (happy‑path) behaviour.
 *
 * 3. **Edge‑case / exception tests** – tests that exercise corner cases,
 *    illegal arguments and the special behaviour provided by the builder
 *    (case‑insensitivity, ordering, compact‑size validation, etc.).
 *
 * The test class can be compiled and run with any JUnit‑4 runner.
 */
public class CompactSetTest {

    /* --------------------------------------------------------------------- */
    /* 1. PUBLIC METHODS (extracted from CompactSet source)                  */
    /* --------------------------------------------------------------------- */
    //
    // Constructors
    //   CompactSet()
    //   CompactSet(Collection<? extends E> c)
    //
    // Set<E> interface methods
    //   int size()
    //   boolean isEmpty()
    //   boolean contains(Object o)
    //   boolean add(E e)
    //   boolean remove(Object o)
    //   void clear()
    //   boolean containsAll(Collection<?> c)
    //   boolean addAll(Collection<? extends E> c)
    //   boolean retainAll(Collection<?> c)
    //   boolean removeAll(Collection<?> c)
    //   Iterator<E> iterator()
    //   Object[] toArray()
    //   <T> T[] toArray(T[] a)
    //   boolean equals(Object o)
    //   int hashCode()
    //   String toString()
    //
    // Static factory / builder
    //   static <E> Builder<E> builder()
    //
    // Builder<E> public methods
    //   Builder<E> caseSensitive(boolean caseSensitive)
    //   Builder<E> compactSize(int size)
    //   Builder<E> sortedOrder()
    //   Builder<E> reverseOrder()
    //   Builder<E> insertionOrder()
    //   Builder<E> noOrder()
    //   CompactSet<E> build()
    //
    /* --------------------------------------------------------------------- */
    /* 2. BASIC FUNCTIONALITY TESTS                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        List<String> source = Arrays.asList("a", "b", "c");
        CompactSet<String> set = new CompactSet<>(source);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(source));
    }

    @Test
    public void testAddAndContains() {
        CompactSet<Integer> set = new CompactSet<>();
        assertTrue(set.add(1));
        assertTrue(set.contains(1));
        // duplicate add must return false and not increase size
        assertFalse(set.add(1));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemove() {
        CompactSet<String> set = new CompactSet<>();
        set.add("x");
        assertTrue(set.remove("x"));
        assertFalse(set.contains("x"));
        assertFalse(set.remove("non‑existent"));
    }

    @Test
    public void testClear() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testContainsAll() {
        CompactSet<Integer> set = new CompactSet<>();
        set.addAll(Arrays.asList(1, 2, 3, 4));
        assertTrue(set.containsAll(Arrays.asList(2, 3)));
        assertFalse(set.containsAll(Arrays.asList(2, 5)));
    }

    @Test
    public void testAddAllReturnsTrueOnlyIfModified() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.addAll(Arrays.asList("a", "b")));
        // adding the same elements again should return false
        assertFalse(set.addAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testRetainAll() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        boolean changed = set.retainAll(Arrays.asList("a", "c"));
        assertTrue(changed);
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("c"));
        assertFalse(set.contains("b"));
    }

    @Test
    public void testRemoveAll() {
        CompactSet<Integer> set = new CompactSet<>();
        set.addAll(Arrays.asList(1, 2, 3, 4));
        boolean changed = set.removeAll(Arrays.asList(2, 4, 6));
        assertTrue(changed);
        assertEquals(2, set.size());
        assertFalse(set.contains(2));
        assertFalse(set.contains(4));
        assertTrue(set.contains(1));
        assertTrue(set.contains(3));
    }

    @Test
    public void testIteratorOrderMatchesUnderlyingMap() {
        // Default builder -> unordered (hash‑like). We only verify that iterator
        // returns all elements, not a specific order.
        CompactSet<String> set = CompactSet.<String>builder().noOrder().build();
        set.addAll(Arrays.asList("x", "y", "z"));
        Set<String> iterated = new HashSet<>();
        for (String s : set) {
            iterated.add(s);
        }
        assertEquals(new HashSet<>(Arrays.asList("x", "y", "z")), iterated);
    }

    @Test
    public void testToArray() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("a", "b"));
        Object[] arr = set.toArray();
        assertEquals(2, arr.length);
        assertTrue(Arrays.asList("a", "b").containsAll(Arrays.asList(arr)));
    }

    @Test
    public void testToArrayWithTypedArray() {
        CompactSet<Integer> set = new CompactSet<>();
        set.addAll(Arrays.asList(10, 20, 30));
        Integer[] dest = new Integer[5]; // larger than needed, should be filled and null‑terminated
        Integer[] result = set.toArray(dest);
        // result should be the same array instance (as per the contract)
        assertSame(dest, result);
        assertArrayEquals(new Integer[]{10, 20, 30, null, null}, result);
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactSet<String> s1 = new CompactSet<>();
        CompactSet<String> s2 = new CompactSet<>();
        s1.addAll(Arrays.asList("a", "b"));
        s2.addAll(Arrays.asList("b", "a"));
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        // Different content -> not equal
        s2.add("c");
        assertNotEquals(s1, s2);
    }

    @Test
    public void testToStringRepresentation() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("one", "two"));
        String str = set.toString();
        // The exact order may differ, but the string must start with '[' and end with ']'
        assertTrue(str.startsWith("["));
        assertTrue(str.endsWith("]"));
        assertTrue(str.contains("one"));
        assertTrue(str.contains("two"));
    }

    /* --------------------------------------------------------------------- */
    /* 3. EDGE CASE & EXCEPTION TESTS                                        */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE() {
        new CompactSet<>(null);
    }

    @Test
    public void testAddNullElement() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        // adding null again must return false
        assertFalse(set.add(null));
        assertEquals(1, set.size());
    }

    @Test
    public void testBuilderCaseInsensitive() {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(false)   // case‑insensitive
                .noOrder()
                .build();

        assertTrue(set.add("Hello"));
        // Adding a different case should be considered duplicate
        assertFalse(set.add("hello"));
        assertEquals(1, set.size());
        assertTrue(set.contains("HELLO"));
    }

    @Test
    public void testBuilderSortedOrder() {
        CompactSet<Integer> set = CompactSet.<Integer>builder()
                .sortedOrder()
                .build();

        set.addAll(Arrays.asList(5, 1, 3));
        Iterator<Integer> it = set.iterator();
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(5), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testBuilderReverseOrder() {
        CompactSet<Integer> set = CompactSet.<Integer>builder()
                .reverseOrder()
                .build();

        set.addAll(Arrays.asList(2, 4, 1));
        Iterator<Integer> it = set.iterator();
        assertEquals(Integer.valueOf(4), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testBuilderInsertionOrder() {
        CompactSet<String> set = CompactSet.<String>builder()
                .insertionOrder()
                .build();

        set.addAll(Arrays.asList("first", "second", "third"));
        Iterator<String> it = set.iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderCompactSizeTooSmallThrowsException() {
        // compactSize < 2 should trigger IllegalStateException in CompactSet ctor
        CompactSet.<String>builder()
                .compactSize(1)   // illegal value
                .build();
    }

    @Test
    public void testBuilderCompactSizeValidBoundary() {
        // compactSize == 2 is the minimum allowed – should succeed
        CompactSet<String> set = CompactSet.<String>builder()
                .compactSize(2)
                .build();

        assertTrue(set.add("a"));
        assertTrue(set.contains("a"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRetainAllWithEmptyCollectionClearsSet() {
        CompactSet<Integer> set = new CompactSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        boolean changed = set.retainAll(Collections.emptyList());
        assertTrue(changed);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRemoveAllWithEmptyCollectionLeavesSetUnchanged() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("a", "b"));
        boolean changed = set.removeAll(Collections.emptyList());
        assertFalse(changed);
        assertEquals(2, set.size());
    }

    @Test
    public void testContainsAllWithNullElementInCollection() {
        CompactSet<String> set = new CompactSet<>();
        set.add(null);
        List<String> list = Arrays.asList(null, "x");
        assertTrue(set.containsAll(list));
    }

    @Test
    public void testAddAllWithNullElementInSourceCollection() {
        CompactSet<String> set = new CompactSet<>();
        List<String> source = Arrays.asList("a", null, "b");
        assertTrue(set.addAll(source));
        assertEquals(3, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    public void testEqualsWithDifferentImplementation() {
        CompactSet<String> compactSet = new CompactSet<>();
        compactSet.addAll(Arrays.asList("a", "b"));

        Set<String> hashSet = new HashSet<>(Arrays.asList("a", "b"));
        // Equality is defined by the underlying keySet, so both should be equal
        assertEquals(compactSet, hashSet);
        assertEquals(hashSet, compactSet);
    }

    @Test
    public void testHashCodeConsistencyAfterMutations() {
        CompactSet<String> set = new CompactSet<>();
        int emptyHash = set.hashCode();

        set.add("x");
        int afterAddHash = set.hashCode();
        assertNotEquals(emptyHash, afterAddHash);

        set.remove("x");
        assertEquals(emptyHash, set.hashCode());
    }
}
