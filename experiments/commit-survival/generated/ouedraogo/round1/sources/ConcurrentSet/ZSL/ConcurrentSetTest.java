/****************************************************************************************
 * JUnit 4 test suite for com.cedarsoftware.util.ConcurrentSet
 *
 * This test class exercises every public method of ConcurrentSet, including special
 * handling of {@code null} values, equals/hashCode contracts, array conversion,
 * iterator behavior, and basic concurrent modifications.
 *
 * The tests are written using only the API exposed by ConcurrentSet and standard
 * JDK classes; no mocking frameworks or external libraries are used.
 ****************************************************************************************/
package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ConcurrentSetTest {

    private ConcurrentSet<String> emptySet;

    @Before
    public void setUp() {
        emptySet = new ConcurrentSet<>();
    }

    /* --------------------------------------------------------------
     *  Constructor tests
     * -------------------------------------------------------------- */
    @Test
    public void testEmptyConstructor() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructor() {
        List<String> list = Arrays.asList("alpha", null, "beta");
        ConcurrentSet<String> set = new ConcurrentSet<>(list);
        assertEquals(3, set.size());
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("beta"));
        assertTrue(set.contains(null));
    }

    @Test
    public void testSetConstructor() {
        Set<String> source = new HashSet<>(Arrays.asList("x", null, "y"));
        ConcurrentSet<String> set = new ConcurrentSet<>(source);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(source));
    }

    /* --------------------------------------------------------------
     *  Basic mutable operations
     * -------------------------------------------------------------- */
    @Test
    public void testAddAndContains() {
        assertTrue(emptySet.add("one"));
        assertTrue(emptySet.contains("one"));
        // adding duplicate should return false and not change size
        assertFalse(emptySet.add("one"));
        assertEquals(1, emptySet.size());

        // null handling
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        assertEquals(2, emptySet.size());
    }

    @Test
    public void testRemove() {
        emptySet.add("rem");
        emptySet.add(null);
        assertTrue(emptySet.remove("rem"));
        assertFalse(emptySet.contains("rem"));
        assertEquals(1, emptySet.size());

        // remove null
        assertTrue(emptySet.remove(null));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testAddAll() {
        List<String> items = Arrays.asList("a", "b", null, "a"); // duplicate "a"
        assertTrue(emptySet.addAll(items));
        assertEquals(3, emptySet.size()); // a, b, null
        assertTrue(emptySet.containsAll(Arrays.asList("a", "b", null)));
    }

    @Test
    public void testRemoveAll() {
        emptySet.addAll(Arrays.asList("x", "y", null, "z"));
        List<String> toRemove = Arrays.asList("y", null);
        assertTrue(emptySet.removeAll(toRemove));
        assertEquals(2, emptySet.size());
        assertFalse(emptySet.contains("y"));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.containsAll(Arrays.asList("x", "z")));
    }

    @Test
    public void testRetainAll() {
        emptySet.addAll(Arrays.asList("p", "q", null, "r"));
        List<String> toRetain = Arrays.asList("q", null, "missing");
        assertTrue(emptySet.retainAll(toRetain));
        assertEquals(2, emptySet.size());
        assertTrue(emptySet.contains("q"));
        assertTrue(emptySet.contains(null));
        assertFalse(emptySet.contains("p"));
        assertFalse(emptySet.contains("r"));
    }

    @Test
    public void testClear() {
        emptySet.addAll(Arrays.asList("a", null, "b"));
        emptySet.clear();
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    /* --------------------------------------------------------------
     *  Iterator behavior (including remove via iterator)
     * -------------------------------------------------------------- */
    @Test
    public void testIteratorAndRemove() {
        emptySet.addAll(Arrays.asList("first", "second", null));
        Iterator<String> it = emptySet.iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            String val = it.next();
            // verify that iterator returns the actual element (null stays null)
            seen.add(val);
            if ("second".equals(val)) {
                it.remove(); // remove via iterator
            }
        }
        assertEquals(2, emptySet.size());
        assertFalse(emptySet.contains("second"));
        assertTrue(seen.containsAll(Arrays.asList("first", null, "second")));
    }

    /* --------------------------------------------------------------
     *  toArray tests
     * -------------------------------------------------------------- */
    @Test
    public void testToArrayObject() {
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
        String[] a = new String[2]; // smaller than needed to force allocation
        String[] result = emptySet.toArray(a);
        assertEquals(3, result.length);
        List<String> list = Arrays.asList(result);
        assertTrue(list.contains("x"));
        assertTrue(list.contains("y"));
        assertTrue(list.contains(null));

        // When supplied array is larger, the element after size should be null
        String[] larger = new String[5];
        larger[3] = "should be overwritten";
        String[] after = emptySet.toArray(larger);
        assertSame(larger, after);
        assertNull(after[3]); // sentinel null per contract
    }

    /* --------------------------------------------------------------
     *  equals, hashCode, toString
     * -------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode() {
        Set<String> javaSet = new HashSet<>(Arrays.asList("a", null, "b"));
        ConcurrentSet<String> cs = new ConcurrentSet<>(javaSet);
        assertEquals(javaSet, cs);
        assertEquals(cs, javaSet);
        assertEquals(javaSet.hashCode(), cs.hashCode());

        // Different content -> not equal
        cs.add("c");
        assertNotEquals(javaSet, cs);
    }

    @Test
    public void testToString() {
        // Empty set
        assertEquals("{}", emptySet.toString());

        // Non‑empty set with null
        emptySet.addAll(Arrays.asList("alpha", null, "beta"));
        String s = emptySet.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // The string must contain each element's textual representation (null as "null")
        assertTrue(s.contains("alpha"));
        assertTrue(s.contains("beta"));
        assertTrue(s.contains("null"));
    }

    /* --------------------------------------------------------------
     *  containsAll and size/isEmpty edge cases
     * -------------------------------------------------------------- */
    @Test
    public void testContainsAllAndIsEmpty() {
        assertTrue(emptySet.isEmpty());
        assertTrue(emptySet.containsAll(Collections.emptySet()));
        emptySet.addAll(Arrays.asList("a", "b"));
        assertFalse(emptySet.isEmpty());
        assertTrue(emptySet.containsAll(Arrays.asList("a")));
        assertFalse(emptySet.containsAll(Arrays.asList("c")));
    }

    /* --------------------------------------------------------------
     *  Concurrency test – multiple threads adding distinct elements
     * -------------------------------------------------------------- */
    @Test
    public void testConcurrentAdditions() throws Exception {
        final int THREADS = 10;
        final int PER_THREAD = 1000;
        ExecutorService exec = Executors.newFixedThreadPool(THREADS);
        final ConcurrentSet<Integer> concurrentSet = new ConcurrentSet<>();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            final int base = t * PER_THREAD;
            tasks.add(() -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    concurrentSet.add(base + i);
                }
                return null;
            });
        }

        // run all tasks and wait for completion
        List<Future<Void>> futures = exec.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get(); // propagate any exception
        }
        exec.shutdownNow();

        // Verify that every expected element is present exactly once
        assertEquals(THREADS * PER_THREAD, concurrentSet.size());

        for (int i = 0; i < THREADS * PER_THREAD; i++) {
            assertTrue("Missing element: " + i, concurrentSet.contains(i));
        }
    }

    /* --------------------------------------------------------------
     *  Additional edge case – iterator remove on empty set should throw
     * -------------------------------------------------------------- */
    @Test(expected = NoSuchElementException.class)
    public void testIteratorRemoveOnEmptyThrows() {
        Iterator<String> it = emptySet.iterator();
        it.next(); // should throw NoSuchElementException
    }

    /* --------------------------------------------------------------
     *  Verify that retainAll works when collection contains elements not
     *  present in the set (should simply ignore them).
     * -------------------------------------------------------------- */
    @Test
    public void testRetainAllWithExtraElements() {
        emptySet.addAll(Arrays.asList("keep1", "keep2"));
        List<String> retain = Arrays.asList("keep1", "nonexistent");
        assertTrue(emptySet.retainAll(retain));
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("keep1"));
        assertFalse(emptySet.contains("keep2"));
    }
}
