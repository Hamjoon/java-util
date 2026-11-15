package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class ConcurrentSetTest {

    private ConcurrentSet<String> emptySet;
    private ConcurrentSet<Integer> intSet;
    private ConcurrentSet<String> setWithNull;

    @Before
    public void setUp() {
        emptySet = new ConcurrentSet<>();
        intSet = new ConcurrentSet<>();
        setWithNull = new ConcurrentSet<>();
    }

    /* -------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElementsIncludingNull() {
        List<String> list = Arrays.asList("a", null, "b");
        ConcurrentSet<String> cs = new ConcurrentSet<>(list);
        assertEquals(3, cs.size());
        assertTrue(cs.contains("a"));
        assertTrue(cs.contains("b"));
        assertTrue(cs.contains(null));
    }

    @Test
    public void testSetConstructorCopiesElementsIncludingNull() {
        Set<String> source = new HashSet<>();
        source.add("x");
        source.add(null);
        ConcurrentSet<String> cs = new ConcurrentSet<>(source);
        assertEquals(2, cs.size());
        assertTrue(cs.contains("x"));
        assertTrue(cs.contains(null));
    }

    /* -------------------------------------------------
     *  Basic add / remove / contains
     * ------------------------------------------------- */

    @Test
    public void testAddAndContains() {
        assertTrue(emptySet.add("hello"));
        assertTrue(emptySet.contains("hello"));
        // adding duplicate should return false
        assertFalse(emptySet.add("hello"));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testAddNullAndContainsNull() {
        assertTrue(setWithNull.add(null));
        assertTrue(setWithNull.contains(null));
        // duplicate null
        assertFalse(setWithNull.add(null));
        assertEquals(1, setWithNull.size());
    }

    @Test
    public void testRemoveExistingElement() {
        emptySet.add("test");
        assertTrue(emptySet.remove("test"));
        assertFalse(emptySet.contains("test"));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testRemoveNullElement() {
        setWithNull.add(null);
        assertTrue(setWithNull.remove(null));
        assertFalse(setWithNull.contains(null));
        assertTrue(setWithNull.isEmpty());
    }

    @Test
    public void testRemoveNonExistingElement() {
        assertFalse(emptySet.remove("absent"));
    }

    /* -------------------------------------------------
     *  Bulk operations
     * ------------------------------------------------- */

    @Test
    public void testAddAllWithNulls() {
        List<String> list = Arrays.asList("a", null, "b");
        assertTrue(emptySet.addAll(list));
        assertEquals(3, emptySet.size());
        assertTrue(emptySet.containsAll(list));
    }

    @Test
    public void testRemoveAllWithNulls() {
        List<String> list = Arrays.asList("a", null, "b");
        emptySet.addAll(list);
        List<String> toRemove = Arrays.asList(null, "b");
        assertTrue(emptySet.removeAll(toRemove));
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("a"));
        assertFalse(emptySet.contains(null));
        assertFalse(emptySet.contains("b"));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        List<String> initial = Arrays.asList("x", "y", null, "z");
        emptySet.addAll(initial);
        List<String> retain = Arrays.asList(null, "y");
        assertTrue(emptySet.retainAll(retain));
        assertEquals(2, emptySet.size());
        assertTrue(emptySet.contains(null));
        assertTrue(emptySet.contains("y"));
        assertFalse(emptySet.contains("x"));
        assertFalse(emptySet.contains("z"));
    }

    @Test
    public void testContainsAllWhenMissingElement() {
        emptySet.addAll(Arrays.asList("a", "b"));
        assertFalse(emptySet.containsAll(Arrays.asList("a", "c")));
    }

    /* -------------------------------------------------
     *  Iterator behavior
     * ------------------------------------------------- */

    @Test
    public void testIteratorTraversesAllElementsIncludingNull() {
        List<String> data = Arrays.asList("one", null, "two");
        emptySet.addAll(data);
        Set<String> visited = new HashSet<>();
        for (String s : emptySet) {
            visited.add(s);
        }
        assertEquals(new HashSet<>(data), visited);
    }

    @Test
    public void testIteratorRemove() {
        emptySet.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = emptySet.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("b".equals(val)) {
                it.remove();
            }
        }
        assertFalse(emptySet.contains("b"));
        assertEquals(2, emptySet.size());
    }

    /* -------------------------------------------------
     *  toArray variants
     * ------------------------------------------------- */

    @Test
    public void testToArrayReturnsObjectArrayWithNulls() {
        emptySet.addAll(Arrays.asList("x", null, "y"));
        Object[] arr = emptySet.toArray();
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains("x"));
        assertTrue(list.contains("y"));
        assertTrue(list.contains(null));
        assertEquals(3, arr.length);
    }

    @Test
    public void testToArrayTypedExactSize() {
        emptySet.addAll(Arrays.asList("p", null, "q"));
        String[] a = new String[3];
        String[] result = emptySet.toArray(a);
        assertSame(a, result);
        List<String> list = Arrays.asList(result);
        assertTrue(list.contains("p"));
        assertTrue(list.contains("q"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayTypedLargerArrayPadsWithNull() {
        emptySet.addAll(Arrays.asList("a", "b"));
        String[] a = new String[5];
        String[] result = emptySet.toArray(a);
        assertSame(a, result);
        // first two positions are filled (order not guaranteed)
        assertNotNull(result[0]);
        assertNotNull(result[1]);
        // the third element must be null per spec
        assertNull(result[2]);
    }

    @Test
    public void testToArrayTypedSmallerArrayCreatesNewArray() {
        emptySet.addAll(Arrays.asList("1", "2"));
        String[] a = new String[1];
        String[] result = emptySet.toArray(a);
        assertNotSame(a, result);
        assertEquals(2, result.length);
        List<String> list = Arrays.asList(result);
        assertTrue(list.containsAll(Arrays.asList("1", "2")));
    }

    /* -------------------------------------------------
     *  equals, hashCode, toString
     * ------------------------------------------------- */

    @Test
    public void testEqualsSameElements() {
        ConcurrentSet<String> s1 = new ConcurrentSet<>();
        ConcurrentSet<String> s2 = new ConcurrentSet<>();
        s1.addAll(Arrays.asList("a", null, "b"));
        s2.addAll(Arrays.asList(null, "b", "a"));
        assertEquals(s1, s2);
        assertEquals(s2, s1);
    }

    @Test
    public void testEqualsDifferentSize() {
        ConcurrentSet<String> s1 = new ConcurrentSet<>();
        ConcurrentSet<String> s2 = new ConcurrentSet<>();
        s1.add("x");
        s2.addAll(Arrays.asList("x", "y"));
        assertNotEquals(s1, s2);
    }

    @Test
    public void testEqualsAgainstNonSet() {
        emptySet.add("test");
        assertNotEquals(emptySet, "test");
    }

    @Test
    public void testHashCodeMatchesSumOfElements() {
        ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        cs.add(1);
        cs.add(2);
        cs.add(null); // null contributes 0
        int expected = 1 + 2; // null ignored
        assertEquals(expected, cs.hashCode());
    }

    @Test
    public void testToStringEmpty() {
        assertEquals("{}", emptySet.toString());
    }

    @Test
    public void testToStringNonEmpty() {
        emptySet.addAll(Arrays.asList("alpha", null, "beta"));
        String s = emptySet.toString();
        // Order is unspecified, but must contain all elements separated by ", "
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("alpha"));
        assertTrue(s.contains("beta"));
        assertTrue(s.contains("null"));
        // Count commas (should be two for three elements)
        assertEquals(2, s.chars().filter(ch -> ch == ',').count());
    }

    /* -------------------------------------------------
     *  Concurrency sanity (basic) – not exhaustive but ensures no
     *  unexpected exceptions when accessed from multiple threads.
     * ------------------------------------------------- */

    @Test
    public void testConcurrentAddAndContains() throws Exception {
        final ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                cs.add(i);
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                cs.contains(i);
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertEquals(1000, cs.size());
        for (int i = 0; i < 1000; i++) {
            assertTrue(cs.contains(i));
        }
    }
}
