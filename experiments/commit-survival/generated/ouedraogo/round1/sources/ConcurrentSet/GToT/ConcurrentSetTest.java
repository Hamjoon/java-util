package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentSet}.
 *
 * The tests are organised in the order in which the public API appears
 * in the source class.  For each method we provide:
 *   • a basic functionality test,
 *   • edge‑case tests (null handling, duplicate handling, etc.),
 *   • and where relevant, tests that provoke exceptional situations.
 *
 * No mocking frameworks are used – all objects are created with real
 * constructors from the JDK or from the class under test.
 */
public class ConcurrentSetTest {

    private ConcurrentSet<String> emptySet;

    @Before
    public void setUp() {
        emptySet = new ConcurrentSet<>();
    }

    /* -------------------------------------------------
     *  Constructors
     * ------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElementsAndHandlesNull() {
        List<String> list = Arrays.asList("a", null, "b");
        ConcurrentSet<String> cs = new ConcurrentSet<>(list);
        assertEquals(3, cs.size());
        assertTrue(cs.contains("a"));
        assertTrue(cs.contains("b"));
        assertTrue(cs.contains(null));
    }

    @Test
    public void testSetConstructorCopiesElementsAndHandlesNull() {
        Set<String> src = new HashSet<>(Arrays.asList("x", null));
        ConcurrentSet<String> cs = new ConcurrentSet<>(src);
        assertEquals(2, cs.size());
        assertTrue(cs.contains("x"));
        assertTrue(cs.contains(null));
    }

    /* -------------------------------------------------
     *  Basic mutable operations
     * ------------------------------------------------- */

    @Test
    public void testAddAndContains() {
        assertTrue(emptySet.add("hello"));
        assertTrue(emptySet.contains("hello"));
        assertFalse(emptySet.add("hello")); // duplicate – should return false
    }

    @Test
    public void testAddNullAndContainsNull() {
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        // adding null again must return false because it already exists
        assertFalse(emptySet.add(null));
    }

    @Test
    public void testRemoveExistingElement() {
        emptySet.add("foo");
        assertTrue(emptySet.remove("foo"));
        assertFalse(emptySet.contains("foo"));
        assertFalse(emptySet.remove("foo")); // already removed
    }

    @Test
    public void testRemoveNullElement() {
        emptySet.add(null);
        assertTrue(emptySet.remove(null));
        assertFalse(emptySet.contains(null));
        assertFalse(emptySet.remove(null));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(emptySet.isEmpty());
        emptySet.add("a");
        assertFalse(emptySet.isEmpty());
        assertEquals(1, emptySet.size());
        emptySet.add(null);
        assertEquals(2, emptySet.size());
        emptySet.clear();
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testIteratorAndIteratorRemove() {
        emptySet.addAll(Arrays.asList("x", "y", null));
        Iterator<String> it = emptySet.iterator();
        List<String> seen = new ArrayList<>();
        while (it.hasNext()) {
            String v = it.next();
            seen.add(v);
            if ("y".equals(v)) {
                it.remove(); // remove via iterator
            }
        }
        assertTrue(seen.containsAll(Arrays.asList("x", "y", null)));
        assertFalse(emptySet.contains("y"));
        assertEquals(2, emptySet.size());
    }

    /* -------------------------------------------------
     *  Bulk operations
     * ------------------------------------------------- */

    @Test
    public void testContainsAll() {
        emptySet.addAll(Arrays.asList("a", "b", null));
        assertTrue(emptySet.containsAll(Arrays.asList("a", null)));
        assertFalse(emptySet.containsAll(Arrays.asList("a", "c")));
    }

    @Test
    public void testAddAllWithDuplicatesAndNulls() {
        List<String> src = Arrays.asList("p", "q", null, "p");
        assertTrue(emptySet.addAll(src)); // at least one new element
        assertEquals(3, emptySet.size()); // "p","q",null
        // second call should not modify the set
        assertFalse(emptySet.addAll(src));
    }

    @Test
    public void testRemoveAll() {
        emptySet.addAll(Arrays.asList("a", "b", null));
        assertTrue(emptySet.removeAll(Arrays.asList("b", null, "missing")));
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("a"));
        // second call – nothing removed
        assertFalse(emptySet.removeAll(Arrays.asList("b", null)));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        emptySet.addAll(Arrays.asList("keep", "discard", null));
        assertTrue(emptySet.retainAll(Arrays.asList("keep", null)));
        assertEquals(2, emptySet.size());
        assertTrue(emptySet.contains("keep"));
        assertTrue(emptySet.contains(null));
        assertFalse(emptySet.contains("discard"));
        // retaining with a superset should leave set unchanged
        assertFalse(emptySet.retainAll(Arrays.asList("keep", null, "extra")));
    }

    @Test
    public void testClearEmptiesSet() {
        emptySet.addAll(Arrays.asList("x", null));
        emptySet.clear();
        assertTrue(emptySet.isEmpty());
    }

    /* -------------------------------------------------
     *  Array conversion
     * ------------------------------------------------- */

    @Test
    public void testToArrayReturnsUnwrappedElements() {
        emptySet.addAll(Arrays.asList("a", null, "b"));
        Object[] arr = emptySet.toArray();
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains(null));
        assertEquals(3, arr.length);
    }

    @Test
    public void testToArrayTypedExactSize() {
        emptySet.addAll(Arrays.asList("x", null));
        String[] a = new String[2];
        String[] result = emptySet.toArray(a);
        assertSame(a, result);
        assertArrayEquals(new String[]{"x", null}, result);
    }

    @Test
    public void testToArrayTypedSmallerArrayCreatesNewArray() {
        emptySet.addAll(Arrays.asList("x", "y"));
        String[] a = new String[1]; // too small
        String[] result = emptySet.toArray(a);
        assertNotSame(a, result);
        assertArrayEquals(new String[]{"x", "y"}, result);
    }

    @Test
    public void testToArrayTypedLargerArrayHasNullTerminator() {
        emptySet.addAll(Arrays.asList("a"));
        String[] a = new String[3];
        a[0] = "pre";
        a[1] = "pre2";
        a[2] = "pre3";
        String[] result = emptySet.toArray(a);
        // result should be the same array, with element[0] = "a" and element[1] = null
        assertSame(a, result);
        assertEquals("a", result[0]);
        assertNull(result[1]); // terminator
        assertEquals("pre3", result[2]); // untouched beyond terminator
    }

    /* -------------------------------------------------
     *  Immutable APIs – equals, hashCode, toString
     * ------------------------------------------------- */

    @Test
    public void testEqualsWithSameElements() {
        ConcurrentSet<String> s1 = new ConcurrentSet<>(Arrays.asList("one", null));
        ConcurrentSet<String> s2 = new ConcurrentSet<>(Arrays.asList(null, "one"));
        assertTrue(s1.equals(s2));
        assertTrue(s2.equals(s1));
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testEqualsDifferentSize() {
        ConcurrentSet<String> s1 = new ConcurrentSet<>(Collections.singleton("a"));
        ConcurrentSet<String> s2 = new ConcurrentSet<>(Arrays.asList("a", "b"));
        assertFalse(s1.equals(s2));
    }

    @Test
    public void testEqualsWithNonSetObject() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Collections.singleton("x"));
        assertFalse(cs.equals("not a set"));
    }

    @Test
    public void testEqualsSelfReferenceIsTrue() {
        assertTrue(emptySet.equals(emptySet));
    }

    @Test
    public void testEqualsCatchesExceptionAndReturnsFalse() {
        // BadSet throws ClassCastException from contains()
        Set<String> bad = new BadSet<>();
        ConcurrentSet<String> cs = new ConcurrentSet<>(Collections.singleton("a"));
        assertFalse(cs.equals(bad));
    }

    @Test
    public void testHashCodeWithNulls() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList(null, "x"));
        int expected = 0 + "x".hashCode(); // null contributes 0
        assertEquals(expected, cs.hashCode());
    }

    @Test
    public void testToStringEmpty() {
        assertEquals("{}", emptySet.toString());
    }

    @Test
    public void testToStringSingleElement() {
        emptySet.add("elem");
        assertEquals("{elem}", emptySet.toString());
    }

    @Test
    public void testToStringContainsSelfReference() {
        // Add the set itself as an element – the sentinel should be replaced with "(this Set)"
        @SuppressWarnings("unchecked")
        ConcurrentSet<Object> raw = (ConcurrentSet<Object>) (ConcurrentSet<?>) emptySet;
        raw.add(raw);
        assertEquals("{(this Set)}", raw.toString());
    }

    /* -------------------------------------------------
     *  Helper class used in testEqualsCatchesException
     * ------------------------------------------------- */

    /**
     * Minimal {@link Set} implementation that throws {@link ClassCastException}
     * from {@link #contains(Object)}.  This allows us to verify that
     * {@link ConcurrentSet#equals(Object)} swallows the exception and returns {@code false}.
     */
    private static class BadSet<E> implements Set<E> {
        private final Set<E> delegate = new HashSet<>();

        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean contains(Object o) { throw new ClassCastException(); }
        @Override public Iterator<E> iterator() { return delegate.iterator(); }
        @Override public Object[] toArray() { return delegate.toArray(); }
        @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
        @Override public boolean add(E e) { return delegate.add(e); }
        @Override public boolean remove(Object o) { return delegate.remove(o); }
        @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
        @Override public boolean addAll(Collection<? extends E> c) { return delegate.addAll(c); }
        @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
        @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
        @Override public void clear() { delegate.clear(); }
    }
}
