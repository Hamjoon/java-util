package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentList}.
 *
 * The suite covers:
 * <ul>
 *   <li>All constructors (including error handling).</li>
 *   <li>All immutable (read‑only) APIs – typical values, edge cases and null handling.</li>
 *   <li>All mutable APIs – normal operation, boundary conditions and exception paths.</li>
 *   <li>Unsupported operations – verification that the correct exception is thrown.</li>
 *   <li>Thread‑safety guarantees – concurrent writes and snapshot iterators.</li>
 * </ul>
 */
public class ConcurrentListTest {

    private ConcurrentList<String> emptyList;
    private ConcurrentList<Integer> intList;

    @Before
    public void setUp() {
        emptyList = new ConcurrentList<>();
        intList = new ConcurrentList<>(Arrays.asList(1, 2, 3, 4, 5));
    }

    /* ----------------------------------------------------------------------
     *  Constructors
     * ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyList() {
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    @Test
    public void testCapacityConstructorCreatesEmptyList() {
        ConcurrentList<String> list = new ConcurrentList<>(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testWrappingConstructorUsesProvidedList() {
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        ConcurrentList<String> wrapper = new ConcurrentList<>(backing);
        assertEquals(2, wrapper.size());
        assertTrue(wrapper.contains("a"));
        // Mutate backing list directly – wrapper must reflect the change.
        backing.add("c");
        assertEquals(3, wrapper.size());
        assertTrue(wrapper.contains("c"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrappingConstructorRejectsNull() {
        new ConcurrentList<>(null);
    }

    /* ----------------------------------------------------------------------
     *  Immutable APIs
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        List<String> list1 = new ConcurrentList<>(Arrays.asList("x", "y"));
        List<String> list2 = new ConcurrentList<>(Arrays.asList("x", "y"));
        assertEquals(list1, list2);
        assertEquals(list1.hashCode(), list2.hashCode());

        // Different order → not equal
        List<String> list3 = new ConcurrentList<>(Arrays.asList("y", "x"));
        assertNotEquals(list1, list3);
    }

    @Test
    public void testToStringRepresentation() {
        String s = intList.toString();
        assertEquals("[1, 2, 3, 4, 5]", s);
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(5, intList.size());
        assertFalse(intList.isEmpty());

        emptyList.add("first");
        assertEquals(1, emptyList.size());
        assertFalse(emptyList.isEmpty());

        emptyList.clear();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testContainsAndContainsAll() {
        assertTrue(intList.contains(3));
        assertFalse(intList.contains(99));

        Collection<Integer> subset = Arrays.asList(2, 4);
        assertTrue(intList.containsAll(subset));

        Collection<Integer> superset = Arrays.asList(2, 4, 99);
        assertFalse(intList.containsAll(superset));

        // Empty collection is always contained
        assertTrue(intList.containsAll(Collections.emptyList()));
    }

    @Test
    public void testGetAndIndexOf() {
        assertEquals(Integer.valueOf(3), intList.get(2));
        assertEquals(2, intList.indexOf(3));
        assertEquals(-1, intList.indexOf(99));

        assertEquals(4, intList.lastIndexOf(5));
        assertEquals(-1, intList.lastIndexOf(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetWithInvalidIndexThrows() {
        intList.get(100);
    }

    @Test
    public void testToArray() {
        Object[] arr = intList.toArray();
        assertArrayEquals(new Object[]{1,2,3,4,5}, arr);

        Integer[] typed = intList.toArray(new Integer[0]);
        assertArrayEquals(new Integer[]{1,2,3,4,5}, typed);
    }

    @Test
    public void testIteratorSnapshotIsImmutable() {
        Iterator<Integer> it = intList.iterator();
        // Modify underlying list after iterator creation
        intList.add(6);
        // Iterator should not see the new element
        List<Integer> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        assertEquals(Arrays.asList(1,2,3,4,5), collected);
    }

    @Test
    public void testListIteratorSnapshotIsImmutable() {
        ListIterator<Integer> lit = intList.listIterator();
        intList.remove(Integer.valueOf(1));
        intList.add(0, 99);
        List<Integer> forward = new ArrayList<>();
        while (lit.hasNext()) {
            forward.add(lit.next());
        }
        assertEquals(Arrays.asList(1,2,3,4,5), forward);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorWithIndexUnsupported() {
        intList.listIterator(2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubListUnsupported() {
        intList.subList(1,3);
    }

    /* ----------------------------------------------------------------------
     *  Mutable APIs – normal operation
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndAddAtIndex() {
        assertTrue(emptyList.add("alpha"));
        assertEquals(1, emptyList.size());
        assertEquals("alpha", emptyList.get(0));

        emptyList.add(0, "beta");
        assertEquals(2, emptyList.size());
        assertEquals("beta", emptyList.get(0));
        assertEquals("alpha", emptyList.get(1));
    }

    @Test
    public void testAddAllAndAddAllAtIndex() {
        List<String> toAdd = Arrays.asList("x", "y");
        assertTrue(emptyList.addAll(toAdd));
        assertEquals(2, emptyList.size());

        List<String> more = Arrays.asList("a", "b", "c");
        emptyList.addAll(1, more);
        assertEquals(Arrays.asList("x", "a", "b", "c", "y"), toList(emptyList));
    }

    @Test
    public void testSet() {
        assertEquals(Integer.valueOf(3), intList.set(2, 33));
        assertEquals(Integer.valueOf(33), intList.get(2));
    }

    @Test
    public void testRemoveByIndexAndByObject() {
        Integer removed = intList.remove(0);
        assertEquals(Integer.valueOf(1), removed);
        assertFalse(intList.contains(1));

        boolean changed = intList.remove(Integer.valueOf(99));
        assertFalse(changed);
        assertEquals(4, intList.size());
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        Collection<Integer> toRemove = Arrays.asList(2, 4);
        assertTrue(intList.removeAll(toRemove));
        assertEquals(Arrays.asList(1,3,5), toList(intList));

        // retain only 1 and 5
        assertTrue(intList.retainAll(Arrays.asList(1,5)));
        assertEquals(Arrays.asList(1,5), toList(intList));
    }

    @Test
    public void testClear() {
        intList.clear();
        assertTrue(intList.isEmpty());
        assertEquals(0, intList.size());
    }

    /* ----------------------------------------------------------------------
     *  Edge Cases & Error Scenarios
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddNullElementIfBackingListAllowsIt() {
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>());
        assertTrue(list.add(null));
        assertEquals(1, list.size());
        assertNull(list.get(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAtInvalidIndexThrows() {
        emptyList.add(5, "oops");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAllAtInvalidIndexThrows() {
        emptyList.addAll(3, Arrays.asList("a"));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetInvalidIndexThrows() {
        intList.set(100, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveInvalidIndexThrows() {
        intList.remove(-1);
    }

    /* ----------------------------------------------------------------------
     *  Concurrency Tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testConcurrentAdditions() throws Exception {
        final int threadCount = 20;
        final int perThread = 1000;
        final ConcurrentList<Integer> shared = new ConcurrentList<>();

        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int base = i * perThread;
            exec.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < perThread; j++) {
                        shared.add(base + j);
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        // Release all threads simultaneously
        start.countDown();
        done.await(5, TimeUnit.SECONDS);
        exec.shutdownNow();

        assertEquals(0, failures.get());
        assertEquals(threadCount * perThread, shared.size());

        // Verify that all expected numbers are present (order not guaranteed)
        Set<Integer> expected = new HashSet<>();
        for (int i = 0; i < threadCount * perThread; i++) {
            expected.add(i);
        }
        assertEquals(expected, new HashSet<>(toList(shared)));
    }

    @Test
    public void testIteratorSnapshotDuringConcurrentModification() throws Exception {
        final ConcurrentList<Integer> list = new ConcurrentList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        Iterator<Integer> it = list.iterator(); // snapshot of 0..99

        // Concurrently modify the list
        Thread modifier = new Thread(() -> {
            for (int i = 100; i < 200; i++) {
                list.add(i);
            }
        });
        modifier.start();
        modifier.join();

        // Iterator must still contain only the original 100 elements
        List<Integer> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        assertEquals(100, collected.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(Integer.valueOf(i), collected.get(i));
        }
    }

    /* ----------------------------------------------------------------------
     *  Helper
     * ---------------------------------------------------------------------- */

    private <T> List<T> toList(ConcurrentList<T> cl) {
        List<T> result = new ArrayList<>(cl.size());
        for (T e : cl) {
            result.add(e);
        }
        return result;
    }
}
