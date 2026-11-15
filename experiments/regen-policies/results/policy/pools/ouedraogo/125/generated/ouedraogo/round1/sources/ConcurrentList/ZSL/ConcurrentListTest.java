package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link ConcurrentList}.
 */
public class ConcurrentListTest {

    private ConcurrentList<String> emptyList;
    private ConcurrentList<Integer> intList;
    private List<String> backingList;

    @Before
    public void setUp() {
        emptyList = new ConcurrentList<>();
        intList = new ConcurrentList<>(10);
        backingList = new ArrayList<>();
        backingList.add("alpha");
        backingList.add("beta");
        backingList.add("gamma");
    }

    /* ===================== Constructors ===================== */

    @Test
    public void testNoArgConstructorCreatesEmptyList() {
        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testInitialCapacityConstructor() {
        // Capacity is just a hint; we verify we can add up to that many elements without error.
        for (int i = 0; i < 10; i++) {
            assertTrue(intList.add(i));
        }
        assertEquals(10, intList.size());
        assertFalse(intList.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrapConstructorRejectsNull() {
        new ConcurrentList<String>(null);
    }

    @Test
    public void testWrapConstructorReflectsUnderlyingList() {
        ConcurrentList<String> wrapped = new ConcurrentList<>(backingList);
        assertEquals(3, wrapped.size());
        assertEquals("beta", wrapped.get(1));

        // Mutate via wrapper and see change in original list
        wrapped.add("delta");
        assertEquals(4, backingList.size());
        assertEquals("delta", backingList.get(3));

        // Mutate underlying list and see change via wrapper
        backingList.remove("alpha");
        assertEquals(3, wrapped.size());
        assertFalse(wrapped.contains("alpha"));
    }

    /* ===================== Immutable Operations ===================== */

    @Test
    public void testContainsAndContainsAll() {
        emptyList.addAll(Arrays.asList("x", "y", "z"));
        assertTrue(emptyList.contains("y"));
        assertFalse(emptyList.contains("a"));

        Collection<String> subset = Arrays.asList("x", "z");
        Collection<String> superset = Arrays.asList("x", "y", "z", "extra");
        assertTrue(emptyList.containsAll(subset));
        assertFalse(emptyList.containsAll(superset));
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        emptyList.addAll(Arrays.asList("a", "b", "a", "c"));
        assertEquals(0, emptyList.indexOf("a"));
        assertEquals(2, emptyList.lastIndexOf("a"));
        assertEquals(-1, emptyList.indexOf("nonexistent"));
    }

    @Test
    public void testEqualsHashCodeAndToString() {
        List<String> reference = new ArrayList<>(Arrays.asList("one", "two"));
        ConcurrentList<String> clist = new ConcurrentList<>(reference);
        assertTrue(clist.equals(reference));
        assertEquals(reference.hashCode(), clist.hashCode());
        assertEquals(reference.toString(), clist.toString());
    }

    @Test
    public void testToArrayVariants() {
        emptyList.addAll(Arrays.asList("p", "q", "r"));
        Object[] objArray = emptyList.toArray();
        assertArrayEquals(new Object[]{"p", "q", "r"}, objArray);

        String[] strArray = emptyList.toArray(new String[0]);
        assertArrayEquals(new String[]{"p", "q", "r"}, strArray);
    }

    /* ===================== Mutable Operations ===================== */

    @Test
    public void testAddGetAndSize() {
        assertTrue(emptyList.add("first"));
        assertTrue(emptyList.add("second"));
        assertEquals(2, emptyList.size());
        assertEquals("first", emptyList.get(0));
        assertEquals("second", emptyList.get(1));
    }

    @Test
    public void testAddAtIndex() {
        emptyList.addAll(Arrays.asList("a", "c"));
        emptyList.add(1, "b");
        assertEquals(Arrays.asList("a", "b", "c"), toList(emptyList));
    }

    @Test
    public void testSet() {
        emptyList.addAll(Arrays.asList(1, 2, 3));
        Integer old = emptyList.set(1, 20);
        assertEquals(Integer.valueOf(2), old);
        assertEquals(Integer.valueOf(20), emptyList.get(1));
    }

    @Test
    public void testRemoveByIndex() {
        emptyList.addAll(Arrays.asList("x", "y", "z"));
        String removed = emptyList.remove(1);
        assertEquals("y", removed);
        assertEquals(Arrays.asList("x", "z"), toList(emptyList));
    }

    @Test
    public void testRemoveObject() {
        emptyList.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(emptyList.remove("b"));
        assertFalse(emptyList.contains("b"));
        assertFalse(emptyList.remove("nonexistent"));
    }

    @Test
    public void testAddAllCollection() {
        Collection<String> toAdd = Arrays.asList("d", "e");
        assertTrue(emptyList.addAll(toAdd));
        assertEquals(toAdd, emptyList);
    }

    @Test
    public void testAddAllAtIndex() {
        emptyList.addAll(Arrays.asList("a", "d"));
        Collection<String> middle = Arrays.asList("b", "c");
        assertTrue(emptyList.addAll(1, middle));
        assertEquals(Arrays.asList("a", "b", "c", "d"), toList(emptyList));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        emptyList.addAll(Arrays.asList("a", "b", "c", "d"));
        assertTrue(emptyList.removeAll(Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("a", "d"), toList(emptyList));

        emptyList.addAll(Arrays.asList("e", "f"));
        assertTrue(emptyList.retainAll(Arrays.asList("a", "e")));
        assertEquals(Arrays.asList("a", "e"), toList(emptyList));
    }

    @Test
    public void testClear() {
        emptyList.addAll(Arrays.asList("x", "y"));
        emptyList.clear();
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    /* ===================== Iterator Behaviour ===================== */

    @Test
    public void testIteratorIsSnapshot() {
        emptyList.addAll(Arrays.asList("1", "2", "3"));
        Iterator<String> it = emptyList.iterator();

        // modify the list after iterator creation
        emptyList.add("4");
        emptyList.remove("2");

        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // iterator should reflect the state at creation time
        assertEquals(Arrays.asList("1", "2", "3"), iterated);
        // final state of the list should include modifications
        assertEquals(Arrays.asList("1", "3", "4"), toList(emptyList));
    }

    @Test
    public void testListIteratorIsSnapshot() {
        emptyList.addAll(Arrays.asList("a", "b", "c"));
        ListIterator<String> li = emptyList.listIterator();

        // mutate after obtaining iterator
        emptyList.set(1, "beta");
        emptyList.add("d");

        List<String> forward = new ArrayList<>();
        while (li.hasNext()) {
            forward.add(li.next());
        }
        assertEquals(Arrays.asList("a", "b", "c"), forward);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedListIteratorWithIndex() {
        emptyList.listIterator(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedSubList() {
        emptyList.subList(0, 0);
    }

    /* ===================== Concurrency Test ===================== */

    @Test
    public void testConcurrentAdditions() throws Exception {
        final int threadCount = 10;
        final int addsPerThread = 1000;
        final ConcurrentList<Integer> clist = new ConcurrentList<>();

        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(threadCount);
        final AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < addsPerThread; j++) {
                        if (!clist.add(threadId * addsPerThread + j)) {
                            failures.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    endGate.countDown();
                }
            }).start();
        }

        // Release all threads to start adding
        startGate.countDown();
        // Wait for all threads to finish
        endGate.await();

        assertEquals(0, failures.get());
        assertEquals(threadCount * addsPerThread, clist.size());

        // Verify that all expected integers are present (order not guaranteed)
        Set<Integer> expected = new HashSet<>();
        for (int i = 0; i < threadCount * addsPerThread; i++) {
            expected.add(i);
        }
        Set<Integer> actual = new HashSet<>(toList(clist));
        assertEquals(expected, actual);
    }

    /* ===================== Helper ===================== */

    /**
     * Utility method to convert a {@link List} to a plain {@link java.util.ArrayList}
     * for easier comparison in assertions.
     */
    private static <T> List<T> toList(List<T> list) {
        List<T> copy = new ArrayList<>(list.size());
        for (T e : list) {
            copy.add(e);
        }
        return copy;
    }
}
