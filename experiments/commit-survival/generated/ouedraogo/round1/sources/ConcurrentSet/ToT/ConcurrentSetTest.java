package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentSet}.
 *
 * The tests are grouped by the method they verify and cover:
 * <ul>
 *   <li>Typical usage (adding, removing, iteration, etc.)</li>
 *   <li>Edge cases (null elements, duplicate adds, self‑reference)</li>
 *   <li>Error scenarios (type mismatches, exceptions inside {@code equals})</li>
 *   <li>All public API methods of {@code ConcurrentSet}</li>
 * </ul>
 */
public class ConcurrentSetTest {

    private ConcurrentSet<String> emptySet;
    private ConcurrentSet<Integer> intSet;

    @Before
    public void setUp() {
        emptySet = new ConcurrentSet<>();
        intSet = new ConcurrentSet<>();
    }

    @After
    public void tearDown() {
        emptySet.clear();
        intSet.clear();
    }

    /* -----------------------------------------------------------
     *  Constructor tests
     * ----------------------------------------------------------- */

    @Test
    public void testEmptyConstructor() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructor() {
        List<String> list = Arrays.asList("a", null, "b", "a");
        ConcurrentSet<String> cs = new ConcurrentSet<>(list);
        // Duplicates removed, null kept
        assertEquals(3, cs.size());
        assertTrue(cs.contains("a"));
        assertTrue(cs.contains("b"));
        assertTrue(cs.contains(null));
    }

    @Test
    public void testSetConstructor() {
        Set<Integer> src = new HashSet<>(Arrays.asList(1, 2, null, 2));
        ConcurrentSet<Integer> cs = new ConcurrentSet<>(src);
        assertEquals(3, cs.size());
        assertTrue(cs.contains(1));
        assertTrue(cs.contains(2));
        assertTrue(cs.contains(null));
    }

    /* -----------------------------------------------------------
     *  Basic mutable operations
     * ----------------------------------------------------------- */

    @Test
    public void testAddAndContains() {
        assertTrue(emptySet.add("x"));
        assertTrue(emptySet.contains("x"));
        // duplicate add should return false and not change size
        assertFalse(emptySet.add("x"));
        assertEquals(1, emptySet.size());

        // null handling
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        assertEquals(2, emptySet.size());
    }

    @Test
    public void testRemove() {
        emptySet.add("y");
        emptySet.add(null);
        assertTrue(emptySet.remove("y"));
        assertFalse(emptySet.contains("y"));
        assertEquals(1, emptySet.size());

        // removing null
        assertTrue(emptySet.remove(null));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.isEmpty());

        // removing absent element
        assertFalse(emptySet.remove("not-there"));
    }

    @Test
    public void testAddAll() {
        List<String> items = Arrays.asList("a", "b", null, "a");
        assertTrue(emptySet.addAll(items));
        assertEquals(3, emptySet.size());
        assertTrue(emptySet.containsAll(Arrays.asList("a", "b", null)));

        // second call with same collection should return false (no modification)
        assertFalse(emptySet.addAll(items));
    }

    @Test
    public void testRemoveAll() {
        emptySet.addAll(Arrays.asList("a", "b", null));
        assertTrue(emptySet.removeAll(Arrays.asList("a", null)));
        assertFalse(emptySet.contains("a"));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.contains("b"));
        assertEquals(1, emptySet.size());

        // removing a collection that does not intersect
        assertFalse(emptySet.removeAll(Arrays.asList("x", "y")));
    }

    @Test
    public void testRetainAll() {
        emptySet.addAll(Arrays.asList("a", "b", null, "c"));
        // retain only "a" and null
        assertTrue(emptySet.retainAll(Arrays.asList("a", null)));
        assertEquals(2, emptySet.size());
        assertTrue(emptySet.contains("a"));
        assertTrue(emptySet.contains(null));
        assertFalse(emptySet.contains("b"));
        assertFalse(emptySet.contains("c"));
    }

    @Test
    public void testClearAndIsEmpty() {
        emptySet.addAll(Arrays.asList("x", "y"));
        assertFalse(emptySet.isEmpty());
        emptySet.clear();
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    /* -----------------------------------------------------------
     *  Iterator behaviour
     * ----------------------------------------------------------- */

    @Test
    public void testIteratorTraversalAndRemove() {
        emptySet.addAll(Arrays.asList("one", "two", null));
        Iterator<String> it = emptySet.iterator();

        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            String val = it.next();
            assertTrue(seen.add(val)); // ensure no duplicates from iterator
            if ("two".equals(val)) {
                it.remove(); // remove via iterator
            }
        }

        assertEquals(2, emptySet.size());
        assertFalse(emptySet.contains("two"));
        assertTrue(seen.containsAll(Arrays.asList("one", "two", null)));
    }

    /* -----------------------------------------------------------
     *  toArray tests
     * ----------------------------------------------------------- */

    @Test
    public void testToArray() {
        emptySet.addAll(Arrays.asList("a", null, "b"));
        Object[] arr = emptySet.toArray();
        assertEquals(3, arr.length);
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayTyped() {
        emptySet.addAll(Arrays.asList("x", null, "y"));
        String[] a = new String[2]; // smaller than needed
        String[] result = emptySet.toArray(a);
        assertEquals(3, result.length);
        assertTrue(Arrays.asList(result).containsAll(Arrays.asList("x", "y", null)));

        // when the supplied array is larger, the element after size must be null
        String[] larger = new String[5];
        larger[0] = "pre";
        String[] after = emptySet.toArray(larger);
        assertEquals(5, after.length);
        assertNull(after[3]); // index == size
        assertEquals("pre", after[0]); // pre‑existing values remain untouched
    }

    /* -----------------------------------------------------------
     *  equals and hashCode
     * ----------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeWithSameElements() {
        ConcurrentSet<String> s1 = new ConcurrentSet<>();
        ConcurrentSet<String> s2 = new ConcurrentSet<>();

        s1.addAll(Arrays.asList("a", null, "b"));
        s2.addAll(Arrays.asList(null, "b", "a"));

        assertTrue(s1.equals(s2));
        assertTrue(s2.equals(s1));
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testEqualsDifferentSize() {
        ConcurrentSet<String> s1 = new ConcurrentSet<>();
        ConcurrentSet<String> s2 = new ConcurrentSet<>();

        s1.add("only");
        assertFalse(s1.equals(s2));
        assertFalse(s2.equals(s1));
    }

    @Test
    public void testEqualsDifferentContent() {
        ConcurrentSet<String> s1 = new ConcurrentSet<>();
        ConcurrentSet<String> s2 = new ConcurrentSet<>();

        s1.addAll(Arrays.asList("a", "b"));
        s2.addAll(Arrays.asList("a", "c"));
        assertFalse(s1.equals(s2));
    }

    @Test
    public void testEqualsWithNonSetObject() {
        assertFalse(emptySet.equals("not a set"));
    }

    @Test
    public void testEqualsWithNull() {
        assertFalse(emptySet.equals(null));
    }

    @Test
    public void testEqualsCatchesExceptionFromOtherSet() {
        // A Set implementation that throws ClassCastException from contains()
        Set<Object> brokenSet = new Set<Object>() {
            @Override public int size() { return 1; }
            @Override public boolean isEmpty() { return false; }
            @Override public boolean contains(Object o) { throw new ClassCastException(); }
            @Override public Iterator<Object> iterator() { return Collections.emptyIterator(); }
            @Override public Object[] toArray() { return new Object[0]; }
            @Override public <T> T[] toArray(T[] a) { return a; }
            @Override public boolean add(Object o) { return false; }
            @Override public boolean remove(Object o) { return false; }
            @Override public boolean containsAll(Collection<?> c) { return false; }
            @Override public boolean addAll(Collection<? extends Object> c) { return false; }
            @Override public boolean retainAll(Collection<?> c) { return false; }
            @Override public boolean removeAll(Collection<?> c) { return false; }
            @Override public void clear() {}
        };

        emptySet.add("test");
        assertFalse(emptySet.equals(brokenSet));
    }

    @Test
    public void testHashCodeEmptySet() {
        assertEquals(0, emptySet.hashCode());
    }

    @Test
    public void testHashCodeWithNullElements() {
        emptySet.add(null);
        emptySet.add("a");
        // hash = 0 (null) + "a".hashCode()
        int expected = "a".hashCode();
        assertEquals(expected, emptySet.hashCode());
    }

    /* -----------------------------------------------------------
     *  toString tests
     * ----------------------------------------------------------- */

    @Test
    public void testToStringEmpty() {
        assertEquals("{}", emptySet.toString());
    }

    @Test
    public void testToStringWithElements() {
        emptySet.addAll(Arrays.asList("one", null, "two"));
        String s = emptySet.toString();
        // Order is unspecified due to concurrency set; verify format
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // Remove braces and split
        String inner = s.substring(1, s.length() - 1);
        Set<String> parts = new HashSet<>(Arrays.asList(inner.split(", ")));
        assertTrue(parts.contains("one"));
        assertTrue(parts.contains("two"));
        assertTrue(parts.contains("null"));
    }

    @Test
    public void testToStringSelfReference() {
        // Add the set itself as an element
        @SuppressWarnings("unchecked")
        ConcurrentSet<Object> selfSet = new ConcurrentSet<>();
        selfSet.add(selfSet);
        String s = selfSet.toString();
        assertEquals("{(this Set)}", s);
    }

    /* -----------------------------------------------------------
     *  contains and containsAll
     * ----------------------------------------------------------- */

    @Test
    public void testContainsNullAndNonNull() {
        emptySet.addAll(Arrays.asList(null, "z"));
        assertTrue(emptySet.contains(null));
        assertTrue(emptySet.contains("z"));
        assertFalse(emptySet.contains("missing"));
    }

    @Test
    public void testContainsAll() {
        emptySet.addAll(Arrays.asList("a", "b", null));
        assertTrue(emptySet.containsAll(Arrays.asList("a", null)));
        assertFalse(emptySet.containsAll(Arrays.asList("a", "missing")));
    }

    /* -----------------------------------------------------------
     *  retainAll edge cases with nulls and duplicates
     * ----------------------------------------------------------- */

    @Test
    public void testRetainAllWithDuplicatesAndNulls() {
        emptySet.addAll(Arrays.asList("x", "y", null, "x"));
        // collection to retain contains duplicate "x" and null
        assertTrue(emptySet.retainAll(Arrays.asList("x", null, "x")));
        assertEquals(2, emptySet.size());
        assertTrue(emptySet.contains("x"));
        assertTrue(emptySet.contains(null));
    }

    /* -----------------------------------------------------------
     *  Additional sanity checks
     * ----------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmptyConsistency() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());

        emptySet.add("elem");
        assertFalse(emptySet.isEmpty());
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testIteratorRemoveOnEmptySetThrows() {
        Iterator<String> it = emptySet.iterator();
        try {
            it.remove();
            fail("Expected IllegalStateException from iterator.remove() on empty iterator");
        } catch (IllegalStateException | UnsupportedOperationException e) {
            // ConcurrentHashMap's iterator throws IllegalStateException when remove() is called before next()
        }
    }

    @Test
    public void testAddNullMultipleTimes() {
        assertTrue(emptySet.add(null));
        assertFalse(emptySet.add(null)); // duplicate null should not be added again
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testConcurrentSetWorksWithDifferentGenericTypes() {
        ConcurrentSet<Integer> ints = new ConcurrentSet<>();
        ints.addAll(Arrays.asList(1, 2, null, 3));
        assertTrue(ints.contains(2));
        assertTrue(ints.contains(null));
        assertEquals(4, ints.size());

        // Ensure that toArray returns Integer objects (or null)
        Object[] arr = ints.toArray();
        assertTrue(Arrays.asList(arr).containsAll(Arrays.asList(1, 2, 3, null)));
    }
}
