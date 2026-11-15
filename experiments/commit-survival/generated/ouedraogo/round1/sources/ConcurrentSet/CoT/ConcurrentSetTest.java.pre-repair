package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JUnit 4 test suite for {@link ConcurrentSet}.
 *
 * The test suite is built in four logical parts:
 *
 * 1. **Public API extraction** – a comment block that lists every public method
 *    (including inherited ones from {@link Set}) together with its signature.
 *
 * 2. **Basic functionality tests** – one simple test per method that checks the
 *    normal/expected behaviour.
 *
 * 3. **Edge‑case / exception tests** – tests that target special situations
 *    such as handling of {@code null}, empty collections, duplicate elements,
 *    self‑reference, and type‑mismatch scenarios.
 *
 * 4. **Combined test class** – all tests are merged into a single JUnit‑4 test
 *    class named {@code ConcurrentSetTest}.
 *
 * The tests use only the public constructors and methods that appear in the
 * source code of {@link ConcurrentSet} and the standard JDK classes. No mocking
 * frameworks or external libraries are used.
 */
public class ConcurrentSetTest {

    /* --------------------------------------------------------------
     * 1. PUBLIC METHOD SIGNATURES (extracted from ConcurrentSet)
     * --------------------------------------------------------------
     *
     * Constructors
     *   ConcurrentSet()
     *   ConcurrentSet(Collection<T> col)
     *   ConcurrentSet(Set<T> set)
     *
     * Immutable APIs (from Set)
     *   boolean equals(Object o)
     *   int hashCode()
     *   String toString()
     *   int size()
     *   boolean isEmpty()
     *   boolean contains(Object o)
     *   Iterator<T> iterator()
     *   Object[] toArray()
     *   <T1> T1[] toArray(T1[] a)
     *   boolean containsAll(Collection<?> c)
     *
     * Mutable APIs (from Set)
     *   boolean add(T e)
     *   boolean remove(Object o)
     *   boolean addAll(Collection<? extends T> c)
     *   boolean removeAll(Collection<?> c)
     *   boolean retainAll(Collection<?> c)
     *   void clear()
     */

    private ConcurrentSet<String> set;

    @Before
    public void setUp() {
        set = new ConcurrentSet<>();
    }

    @After
    public void tearDown() {
        set.clear();
    }

    /* --------------------------------------------------------------
     * 2. BASIC FUNCTIONALITY TESTS
     * -------------------------------------------------------------- */

    @Test
    public void testConstructorEmpty() {
        ConcurrentSet<Integer> empty = new ConcurrentSet<>();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
    }

    @Test
    public void testConstructorFromCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        ConcurrentSet<String> fromCol = new ConcurrentSet<>(list);
        assertEquals(3, fromCol.size());
        assertTrue(fromCol.containsAll(list));
    }

    @Test
    public void testConstructorFromSet() {
        Set<Integer> src = new HashSet<>(Arrays.asList(1, 2, 3));
        ConcurrentSet<Integer> fromSet = new ConcurrentSet<>(src);
        assertEquals(3, fromSet.size());
        assertTrue(fromSet.containsAll(src));
    }

    @Test
    public void testAddAndContains() {
        assertTrue(set.add("hello"));
        assertTrue(set.contains("hello"));
        // duplicate add returns false
        assertFalse(set.add("hello"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemove() {
        set.add("x");
        set.add("y");
        assertTrue(set.remove("x"));
        assertFalse(set.contains("x"));
        assertEquals(1, set.size());
        // removing non‑existent element returns false
        assertFalse(set.remove("z"));
    }

    @Test
    public void testAddAll() {
        List<String> items = Arrays.asList("a", "b", "c");
        assertTrue(set.addAll(items));
        assertEquals(3, set.size());
        assertTrue(set.containsAll(items));
        // adding the same collection again should return false (no modification)
        assertFalse(set.addAll(items));
    }

    @Test
    public void testRemoveAll() {
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        assertTrue(set.removeAll(Arrays.asList("b", "d")));
        assertFalse(set.contains("b"));
        assertFalse(set.contains("d"));
        assertEquals(2, set.size());
        // removing a collection that has no members should return false
        assertFalse(set.removeAll(Arrays.asList("x", "y")));
    }

    @Test
    public void testRetainAll() {
        set.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(set.retainAll(Arrays.asList("b", "c", "d")));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
        assertFalse(set.contains("a"));
        assertEquals(2, set.size());
    }

    @Test
    public void testClear() {
        set.addAll(Arrays.asList("a", "b"));
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        set.add("one");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
    }

    @Test
    public void testIterator() {
        set.addAll(Arrays.asList("x", "y", "z"));
        Set<String> iterated = new HashSet<>();
        for (String s : set) {
            iterated.add(s);
        }
        assertEquals(set.size(), iterated.size());
        assertTrue(iterated.containsAll(Arrays.asList("x", "y", "z")));
    }

    @Test
    public void testToArrayObject() {
        set.addAll(Arrays.asList("a", "b"));
        Object[] arr = set.toArray();
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.containsAll(Arrays.asList("a", "b")));
        assertEquals(2, arr.length);
    }

    @Test
    public void testToArrayTyped() {
        set.addAll(Arrays.asList("a", "b"));
        String[] a = new String[0];
        String[] result = set.toArray(a);
        assertArrayEquals(new String[]{"a", "b"}, result);
    }

    @Test
    public void testContainsAll() {
        set.addAll(Arrays.asList("p", "q"));
        assertTrue(set.containsAll(Arrays.asList("p")));
        assertFalse(set.containsAll(Arrays.asList("p", "z")));
    }

    @Test
    public void testEqualsAndHashCode() {
        ConcurrentSet<String> other = new ConcurrentSet<>();
        set.addAll(Arrays.asList("x", "y"));
        other.addAll(Arrays.asList("y", "x"));
        assertTrue(set.equals(other));
        assertEquals(set.hashCode(), other.hashCode());

        other.add("z");
        assertFalse(set.equals(other));
    }

    @Test
    public void testToString() {
        assertEquals("{}", set.toString());
        set.add("one");
        assertEquals("{one}", set.toString());
        set.add("two");
        // order is not guaranteed; check both possible orders
        String s = set.toString();
        assertTrue(s.equals("{one, two}") || s.equals("{two, one}"));
    }

    /* --------------------------------------------------------------
     * 3. EDGE CASE / EXCEPTION TESTS
     * -------------------------------------------------------------- */

    @Test
    public void testNullElementHandling() {
        // Adding null should be accepted (wrapped internally)
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());

        // Adding another null does not change the set
        assertFalse(set.add(null));
        assertEquals(1, set.size());

        // remove null
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testNullInCollectionConstructor() {
        List<String> list = new ArrayList<>();
        list.add(null);
        list.add("a");
        ConcurrentSet<String> cs = new ConcurrentSet<>(list);
        assertEquals(2, cs.size());
        assertTrue(cs.contains(null));
        assertTrue(cs.contains("a"));
    }

    @Test
    public void testNullInAddAll() {
        List<String> list = Arrays.asList(null, "x");
        assertTrue(set.addAll(list));
        assertTrue(set.contains(null));
        assertTrue(set.contains("x"));
        assertEquals(2, set.size());
    }

    @Test
    public void testNullInRemoveAll() {
        set.addAll(Arrays.asList(null, "a", "b"));
        assertTrue(set.removeAll(Arrays.asList(null, "a")));
        assertFalse(set.contains(null));
        assertFalse(set.contains("a"));
        assertTrue(set.contains("b"));
        assertEquals(1, set.size());
    }

    @Test
    public void testNullInRetainAll() {
        set.addAll(Arrays.asList(null, "a", "b"));
        assertTrue(set.retainAll(Arrays.asList(null, "b", "c")));
        assertTrue(set.contains(null));
        assertTrue(set.contains("b"));
        assertFalse(set.contains("a"));
        assertEquals(2, set.size());
    }

    @Test
    public void testToArrayTypedWithLargerDestination() {
        set.addAll(Arrays.asList("a"));
        String[] dest = new String[5];
        dest[0] = "pre";
        dest[4] = "post";
        String[] result = set.toArray(dest);
        // first element should be overwritten, element after size should be null
        assertEquals("a", result[0]);
        assertNull(result[1]); // null terminator per spec
        assertEquals("post", result[4]); // untouched trailing part remains
    }

    @Test
    public void testEqualsWithDifferentImplementation() {
        Set<String> hashSet = new HashSet<>(Arrays.asList("x", "y"));
        set.addAll(Arrays.asList("x", "y"));
        assertTrue(set.equals(hashSet));
        assertTrue(hashSet.equals(set)); // HashSet's equals also works with any Set
    }

    @Test
    public void testEqualsSelfReferenceInToString() {
        // Add the set itself as an element (allowed because generic type is Object)
        ConcurrentSet<Object> objSet = new ConcurrentSet<>();
        objSet.add(objSet);
        String s = objSet.toString();
        assertTrue(s.contains("(this Set)"));
    }

    @Test(expected = ClassCastException.class)
    public void testContainsWithIncompatibleType() {
        // The internal wrap casts to T, so passing a type that cannot be cast
        // will cause a ClassCastException inside contains().
        set.addAll(Arrays.asList("string"));
        // Force a wrong generic type by raw access
        @SuppressWarnings("unchecked")
        ConcurrentSet raw = set;
        raw.contains(new Integer(5)); // should throw ClassCastException
    }

    @Test
    public void testIteratorRemove() {
        set.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String v = it.next();
            if ("b".equals(v)) {
                it.remove();
            }
        }
        assertFalse(set.contains("b"));
        assertEquals(2, set.size());
    }

    /* --------------------------------------------------------------
     * 4. END OF TEST CLASS
     * -------------------------------------------------------------- */
}
