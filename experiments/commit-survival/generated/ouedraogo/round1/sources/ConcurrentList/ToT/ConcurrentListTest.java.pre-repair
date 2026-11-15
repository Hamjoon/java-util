package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ConcurrentListTest {

    private ConcurrentList<Integer> emptyList;
    private ConcurrentList<String> populatedList;

    @Before
    public void setUp() {
        emptyList = new ConcurrentList<>();
        populatedList = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c")));
    }

    /* -------------------------------------------------
     * Constructor tests
     * ------------------------------------------------- */

    @Test
    public void testNoArgConstructorCreatesEmptyList() {
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    @Test
    public void testWrappingConstructorUsesProvidedList() {
        List<String> backing = new LinkedList<>(Arrays.asList("x", "y"));
        ConcurrentList<String> wrapper = new ConcurrentList<>(backing);
        assertEquals(2, wrapper.size());
        assertEquals("x", wrapper.get(0));
        // Mutate backing list directly and verify wrapper sees change
        backing.add("z");
        assertEquals(3, wrapper.size());
        assertEquals("z", wrapper.get(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrappingConstructorRejectsNull() {
        new ConcurrentList<>(null);
    }

    /* -------------------------------------------------
     * Immutable API tests
     * ------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        List<String> other = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(populatedList.equals(other));
        assertEquals(other.hashCode(), populatedList.hashCode());

        // Different content => not equal
        other.set(2, "x");
        assertFalse(populatedList.equals(other));
    }

    @Test
    public void testToString() {
        assertEquals("[a, b, c]", populatedList.toString());
        assertEquals("[]", emptyList.toString());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(3, populatedList.size());
        assertFalse(populatedList.isEmpty());

        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testContainsAndContainsAll() {
        assertTrue(populatedList.contains("b"));
        assertFalse(populatedList.contains("z"));

        Collection<String> subset = Arrays.asList("a", "c");
        Collection<String> notSubset = Arrays.asList("a", "z");
        assertTrue(populatedList.containsAll(subset));
        assertFalse(populatedList.containsAll(notSubset));
    }

    @Test
    public void testGetIndexOfLastIndexOf() {
        populatedList.add("b"); // now list = a,b,c,b
        assertEquals(1, populatedList.indexOf("b"));
        assertEquals(3, populatedList.lastIndexOf("b"));
        assertEquals(-1, populatedList.indexOf("z"));
    }

    @Test
    public void testIteratorProvidesSnapshot() {
        Iterator<String> it = populatedList.iterator();
        // modify underlying list after iterator creation
        populatedList.add("d");
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // iterator should not see "d"
        assertEquals(Arrays.asList("a", "b", "c"), iterated);
    }

    @Test
    public void testToArray() {
        Object[] arr = populatedList.toArray();
        assertArrayEquals(new Object[]{"a", "b", "c"}, arr);
    }

    @Test
    public void testToArrayWithTypedArray() {
        String[] arr = populatedList.toArray(new String[0]);
        assertArrayEquals(new String[]{"a", "b", "c"}, arr);
    }

    @Test
    public void testListIteratorProvidesSnapshot() {
        ListIterator<String> it = populatedList.listIterator();
        // modify list after iterator acquisition
        populatedList.remove("b");
        List<String> snapshot = new ArrayList<>();
        while (it.hasNext()) {
            snapshot.add(it.next());
        }
        // snapshot should still contain original three elements
        assertEquals(Arrays.asList("a", "b", "c"), snapshot);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorWithIndexUnsupported() {
        populatedList.listIterator(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubListUnsupported() {
        populatedList.subList(0, 1);
    }

    /* -------------------------------------------------
     * Mutable API tests
     * ------------------------------------------------- */

    @Test
    public void testAddAndAddAll() {
        assertTrue(emptyList.add(10));
        assertEquals(1, emptyList.size());
        assertEquals(Integer.valueOf(10), emptyList.get(0));

        List<Integer> toAdd = Arrays.asList(20, 30);
        assertTrue(emptyList.addAll(toAdd));
        assertEquals(3, emptyList.size());
        assertEquals(Integer.valueOf(20), emptyList.get(1));
    }

    @Test
    public void testAddAllAtIndex() {
        List<String> extra = Arrays.asList("x", "y");
        populatedList.addAll(1, extra); // a, x, y, b, c
        assertEquals(5, populatedList.size());
        assertEquals("a", populatedList.get(0));
        assertEquals("x", populatedList.get(1));
        assertEquals("y", populatedList.get(2));
        assertEquals("b", populatedList.get(3));
        assertEquals("c", populatedList.get(4));
    }

    @Test
    public void testAddAtIndex() {
        populatedList.add(1, "z"); // a, z, b, c
        assertEquals(4, populatedList.size());
        assertEquals("z", populatedList.get(1));
    }

    @Test
    public void testSet() {
        String old = populatedList.set(2, "new");
        assertEquals("c", old);
        assertEquals("new", populatedList.get(2));
    }

    @Test
    public void testRemoveByIndexAndObject() {
        String removed = populatedList.remove(1); // removes "b"
        assertEquals("b", removed);
        assertEquals(2, populatedList.size());
        assertFalse(populatedList.contains("b"));

        assertTrue(populatedList.remove("c"));
        assertEquals(1, populatedList.size());
        assertFalse(populatedList.contains("c"));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        populatedList.addAll(Arrays.asList("d", "e"));
        assertTrue(populatedList.removeAll(Arrays.asList("a", "e")));
        assertEquals(Arrays.asList("b", "c", "d"), toList(populatedList));

        assertTrue(populatedList.retainAll(Arrays.asList("b", "x")));
        assertEquals(Collections.singletonList("b"), toList(populatedList));
    }

    @Test
    public void testClear() {
        assertFalse(emptyList.isEmpty());
        emptyList.clear();
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    /* -------------------------------------------------
     * Concurrency tests – simple read/write interleaving
     * ------------------------------------------------- */

    @Test
    public void testConcurrentReadWriteDoesNotCorruptState() throws Exception {
        final ConcurrentList<Integer> clist = new ConcurrentList<>();
        final int threadCount = 10;
        final int opsPerThread = 1000;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount * 2);
        AtomicInteger totalAdded = new AtomicInteger();

        // Writer tasks
        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < opsPerThread; j++) {
                        clist.add(totalAdded.incrementAndGet());
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Reader tasks – iterate and sum values
        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                try {
                    startLatch.await();
                    int sum = 0;
                    for (int j = 0; j < opsPerThread; j++) {
                        // snapshot iterator; safe even if list changes concurrently
                        Iterator<Integer> it = clist.iterator();
                        while (it.hasNext()) {
                            sum += it.next();
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads
        startLatch.countDown();
        // Wait for completion
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        exec.shutdownNow();

        // Expected size = threadCount * opsPerThread
        assertEquals(threadCount * opsPerThread, clist.size());

        // Verify that all numbers 1..size are present (order not guaranteed)
        Set<Integer> seen = new HashSet<>(toList(clist));
        for (int i = 1; i <= clist.size(); i++) {
            assertTrue("Missing element " + i, seen.contains(i));
        }
    }

    /* -------------------------------------------------
     * Helper
     * ------------------------------------------------- */

    private static <T> List<T> toList(ConcurrentList<T> cl) {
        List<T> result = new ArrayList<>(cl.size());
        for (int i = 0; i < cl.size(); i++) {
            result.add(cl.get(i));
        }
        return result;
    }
}
