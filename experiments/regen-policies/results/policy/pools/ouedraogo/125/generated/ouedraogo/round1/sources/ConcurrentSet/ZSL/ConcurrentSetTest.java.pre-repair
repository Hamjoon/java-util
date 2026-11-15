package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ConcurrentSetTest {

    private ConcurrentSet<String> emptySet;
    private ConcurrentSet<Integer> setWithNulls;

    @Before
    public void setUp() {
        emptySet = new ConcurrentSet<>();
        setWithNulls = new ConcurrentSet<>(Arrays.asList(1, null, 3));
    }

    // -----------------------------------------------------------------
    // Constructor tests
    // -----------------------------------------------------------------
    @Test
    public void testEmptyConstructor() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructor() {
        List<String> list = Arrays.asList("a", null, "b");
        ConcurrentSet<String> set = new ConcurrentSet<>(list);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains(null));
    }

    @Test
    public void testSetConstructor() {
        Set<Integer> source = new HashSet<>(Arrays.asList(5, null, 7));
        ConcurrentSet<Integer> set = new ConcurrentSet<>(source);
        assertEquals(3, set.size());
        assertTrue(set.contains(5));
        assertTrue(set.contains(7));
        assertTrue(set.contains(null));
    }

    // -----------------------------------------------------------------
    // Basic mutable operations
    // -----------------------------------------------------------------
    @Test
    public void testAddAndContains() {
        assertTrue(emptySet.add("x"));
        assertTrue(emptySet.contains("x"));
        // duplicate add should return false
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

        // remove null
        assertTrue(emptySet.remove(null));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testAddAllAndContainsAll() {
        List<Integer> list = Arrays.asList(10, 20, null);
        assertTrue(setWithNulls.addAll(list));
        // setWithNulls originally had 1, null, 3
        assertEquals(5, setWithNulls.size());
        assertTrue(setWithNulls.containsAll(Arrays.asList(1, 3, 10, 20, null)));
    }

    @Test
    public void testRemoveAll() {
        List<Integer> toRemove = Arrays.asList(1, null);
        assertTrue(setWithNulls.removeAll(toRemove));
        assertEquals(1, setWithNulls.size());
        assertFalse(setWithNulls.contains(1));
        assertFalse(setWithNulls.contains(null));
        assertTrue(setWithNulls.contains(3));
    }

    @Test
    public void testRetainAll() {
        List<Integer> toRetain = Arrays.asList(1, null);
        assertTrue(setWithNulls.retainAll(toRetain));
        assertEquals(2, setWithNulls.size());
        assertTrue(setWithNulls.contains(1));
        assertTrue(setWithNulls.contains(null));
        assertFalse(setWithNulls.contains(3));
    }

    @Test
    public void testClear() {
        emptySet.add("a");
        emptySet.add(null);
        emptySet.clear();
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    // -----------------------------------------------------------------
    // Immutable APIs
    // -----------------------------------------------------------------
    @Test
    public void testEqualsAndHashCode() {
        Set<String> javaSet = new HashSet<>(Arrays.asList("alpha", null, "beta"));
        ConcurrentSet<String> cs = new ConcurrentSet<>(javaSet);
        assertEquals(javaSet, cs);
        assertEquals(cs, javaSet);
        assertEquals(javaSet.hashCode(), cs.hashCode());

        // Different content
        javaSet.remove("beta");
        assertNotEquals(javaSet, cs);
    }

    @Test
    public void testToString() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList("one", null, "two"));
        String s = cs.toString();

        // Order is not guaranteed due to concurrency set, so split and verify contents
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        String inner = s.substring(1, s.length() - 1);
        Set<String> parts = new HashSet<>(Arrays.asList(inner.split(",\\s*")));
        assertEquals(3, parts.size());
        assertTrue(parts.contains("one"));
        assertTrue(parts.contains("two"));
        assertTrue(parts.contains("null")); // null is printed as "null"
    }

    @Test
    public void testIteratorAndRemoveViaIterator() {
        ConcurrentSet<Integer> cs = new ConcurrentSet<>(Arrays.asList(100, 200, null));
        Iterator<Integer> it = cs.iterator();
        Set<Integer> seen = new HashSet<>();
        while (it.hasNext()) {
            Integer v = it.next();
            seen.add(v);
            if (v != null && v == 100) {
                it.remove(); // remove element 100 via iterator
            }
        }
        assertEquals(3, seen.size());
        assertFalse(cs.contains(100));
        assertTrue(cs.contains(200));
        assertTrue(cs.contains(null));
    }

    @Test
    public void testToArray() {
        Object[] arr = setWithNulls.toArray();
        assertEquals(3, arr.length);
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains(1));
        assertTrue(list.contains(3));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayWithProvidedArray() {
        Integer[] a = new Integer[2];
        Integer[] result = setWithNulls.toArray(a);
        // result should be a new array of size 3 because original too small
        assertEquals(3, result.length);
        List<Integer> list = Arrays.asList(result);
        assertTrue(list.contains(1));
        assertTrue(list.contains(3));
        assertTrue(list.contains(null));

        // When the supplied array is larger, it should be filled and the extra slot set to null
        Integer[] big = new Integer[5];
        Integer[] bigResult = setWithNulls.toArray(big);
        assertSame(big, bigResult);
        assertEquals(5, bigResult.length);
        assertNull(bigResult[3]); // first extra slot
        assertNull(bigResult[4]); // second extra slot
    }

    // -----------------------------------------------------------------
    // Concurrency tests
    // -----------------------------------------------------------------
    @Test
    public void testConcurrentAdds() throws Exception {
        final ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        final int threadCount = 10;
        final int perThread = 1000;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int base = i * perThread;
            tasks.add(() -> {
                for (int j = 0; j < perThread; j++) {
                    cs.add(base + j);
                }
                return null;
            });
        }
        exec.invokeAll(tasks);
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(threadCount * perThread, cs.size());

        // Verify all expected values are present
        for (int i = 0; i < threadCount * perThread; i++) {
            assertTrue("Missing value: " + i, cs.contains(i));
        }
    }

    @Test
    public void testConcurrentAddAndRemove() throws Exception {
        final ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        final int iterations = 5000;
        ExecutorService exec = Executors.newFixedThreadPool(2);
        AtomicInteger adds = new AtomicInteger();
        AtomicInteger removes = new AtomicInteger();

        Callable<Void> adder = () -> {
            for (int i = 0; i < iterations; i++) {
                if (cs.add(i)) {
                    adds.incrementAndGet();
                }
            }
            return null;
        };

        Callable<Void> remover = () -> {
            for (int i = 0; i < iterations; i++) {
                if (cs.remove(i)) {
                    removes.incrementAndGet();
                }
            }
            return null;
        };

        Future<Void> f1 = exec.submit(adder);
        Future<Void> f2 = exec.submit(remover);
        f1.get();
        f2.get();
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS));

        // After adding then removing same range, set should be empty
        assertTrue(cs.isEmpty());
        // The number of successful adds should be equal to successful removes
        assertEquals(adds.get(), removes.get());
    }

    // -----------------------------------------------------------------
    // Edge cases for null handling in retainAll and containsAll
    // -----------------------------------------------------------------
    @Test
    public void testContainsAllWithNulls() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList("x", null));
        assertTrue(cs.containsAll(Arrays.asList("x", null)));
        assertFalse(cs.containsAll(Arrays.asList("x", "y", null)));
    }

    @Test
    public void testRetainAllWithNulls() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList("a", "b", null));
        List<String> toRetain = Arrays.asList("b", null);
        cs.retainAll(toRetain);
        assertEquals(2, cs.size());
        assertTrue(cs.contains("b"));
        assertTrue(cs.contains(null));
        assertFalse(cs.contains("a"));
    }

    // -----------------------------------------------------------------
    // Verify that equals returns false for non-Set objects
    // -----------------------------------------------------------------
    @Test
    public void testEqualsWithNonSet() {
        assertFalse(emptySet.equals("not a set"));
        assertFalse(emptySet.equals(null));
    }

    // -----------------------------------------------------------------
    // Verify hashCode for empty set is zero (no elements contribute)
    // -----------------------------------------------------------------
    @Test
    public void testHashCodeEmptySet() {
        assertEquals(0, emptySet.hashCode());
    }
}
