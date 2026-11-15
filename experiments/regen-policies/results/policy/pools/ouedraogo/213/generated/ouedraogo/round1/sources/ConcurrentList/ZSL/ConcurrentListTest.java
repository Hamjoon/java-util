package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentList}.
 *
 * <p>This test class validates:
 * <ul>
 *   <li>Correct behavior of all public methods (both immutable and mutable APIs).</li>
 *   <li>Proper handling of edge cases such as null arguments, out‑of‑range indexes,
 *       and unsupported operations.</li>
 *   <li>Snapshot semantics of {@code iterator()} and {@code listIterator()}.</li>
 *   <li>Thread‑safety under concurrent reads and writes.</li>
 *   <li>Correct interaction when the list is used as a wrapper around an existing list.</li>
 * </ul>
 * </p>
 */
public class ConcurrentListTest {

    private ConcurrentList<String> emptyList;
    private ConcurrentList<String> populatedList;
    private List<String> backingList; // used for wrapper‑mode tests

    @Before
    public void setUp() {
        emptyList = new ConcurrentList<>();

        populatedList = new ConcurrentList<>();
        populatedList.addAll(Arrays.asList("alpha", "bravo", "charlie"));

        backingList = new ArrayList<>(Arrays.asList("delta", "echo"));
        // wrapper mode
        ConcurrentList<String> wrapper = new ConcurrentList<>(backingList);
        // replace populatedList with the wrapper for the wrapper‑mode tests
        populatedList = wrapper;
    }

    /* ========================= Immutable API Tests ========================= */

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());

        assertEquals(2, populatedList.size());
        assertFalse(populatedList.isEmpty());
    }

    @Test
    public void testContainsAndContainsAll() {
        assertTrue(populatedList.contains("delta"));
        assertFalse(populatedList.contains("alpha")); // not in wrapper list

        Collection<String> subset = Arrays.asList("delta", "echo");
        assertTrue(populatedList.containsAll(subset));

        Collection<String> superset = Arrays.asList("delta", "echo", "foxtrot");
        assertFalse(populatedList.containsAll(superset));
    }

    @Test
    public void testGetIndexOfLastIndexOf() {
        assertEquals(0, populatedList.indexOf("delta"));
        assertEquals(1, populatedList.lastIndexOf("echo"));
        assertEquals(-1, populatedList.indexOf("nonexistent"));
    }

    @Test
    public void testEqualsHashCodeToString() {
        List<String> copy = new ArrayList<>(backingList);
        ConcurrentList<String> other = new ConcurrentList<>(copy);

        assertTrue(populatedList.equals(other));
        assertEquals(populatedList.hashCode(), other.hashCode());
        assertEquals(populatedList.toString(), other.toString());

        // equality with a plain List should be false (different concrete class)
        assertFalse(populatedList.equals(backingList));
    }

    @Test
    public void testIteratorSnapshot() {
        Iterator<String> it = populatedList.iterator();

        // modify underlying list after iterator creation
        populatedList.add("foxtrot");

        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }

        // iterator must reflect the snapshot taken before the add
        assertEquals(Arrays.asList("delta", "echo"), iterated);
        // list itself contains the new element
        assertTrue(populatedList.contains("foxtrot"));
    }

    @Test
    public void testListIteratorSnapshot() {
        ListIterator<String> lit = populatedList.listIterator();

        // modify while iterator exists
        populatedList.remove("delta");
        populatedList.add("golf");

        List<String> forward = new ArrayList<>();
        while (lit.hasNext()) {
            forward.add(lit.next());
        }

        // snapshot should be original order before modifications
        assertEquals(Arrays.asList("delta", "echo"), forward);
    }

    @Test
    public void testToArray() {
        Object[] arr = populatedList.toArray();
        assertArrayEquals(new Object[]{"delta", "echo"}, arr);

        String[] typed = populatedList.toArray(new String[0]);
        assertArrayEquals(new String[]{"delta", "echo"}, typed);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorWithIndexUnsupported() {
        populatedList.listIterator(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubListUnsupported() {
        populatedList.subList(0, 1);
    }

    /* ========================= Mutable API Tests ========================= */

    @Test
    public void testAddAndAddAtIndex() {
        emptyList.add("hotel");
        assertEquals(1, emptyList.size());
        assertEquals("hotel", emptyList.get(0));

        emptyList.add(0, "india");
        assertEquals(2, emptyList.size());
        assertEquals("india", emptyList.get(0));
        assertEquals("hotel", emptyList.get(1));
    }

    @Test
    public void testAddAllAndAddAllWithIndex() {
        List<String> toAdd = Arrays.asList("juliet", "kilo");
        assertTrue(emptyList.addAll(toAdd));
        assertEquals(toAdd, emptyList);

        List<String> more = Arrays.asList("lima", "mike");
        assertTrue(emptyList.addAll(1, more));
        assertEquals(Arrays.asList("juliet", "lima", "mike", "kilo"), emptyList);
    }

    @Test
    public void testSet() {
        populatedList.set(1, "hotel");
        assertEquals("hotel", populatedList.get(1));
    }

    @Test
    public void testRemoveByIndexAndObject() {
        // remove by index
        String removed = populatedList.remove(0);
        assertEquals("delta", removed);
        assertFalse(populatedList.contains("delta"));

        // remove by object
        assertTrue(populatedList.remove("echo"));
        assertFalse(populatedList.contains("echo"));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        populatedList.addAll(Arrays.asList("november", "oscar"));
        Collection<String> toRemove = Arrays.asList("delta", "november");
        assertTrue(populatedList.removeAll(toRemove));
        assertFalse(populatedList.contains("delta"));
        assertFalse(populatedList.contains("november"));
        assertTrue(populatedList.contains("echo"));
        assertTrue(populatedList.contains("oscar"));

        Collection<String> toRetain = Arrays.asList("echo", "oscar");
        assertTrue(populatedList.retainAll(toRetain));
        assertEquals(2, populatedList.size());
        assertTrue(populatedList.containsAll(toRetain));
    }

    @Test
    public void testClear() {
        populatedList.clear();
        assertTrue(populatedList.isEmpty());
        assertTrue(backingList.isEmpty()); // wrapper shares the backing list
    }

    @Test
    public void testAddNullElement() {
        emptyList.add(null);
        assertEquals(1, emptyList.size());
        assertNull(emptyList.get(0));
        assertTrue(emptyList.contains(null));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetInvalidIndex() {
        emptyList.get(5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAtInvalidIndex() {
        emptyList.add(3, "papa");
    }

    /* ========================= Wrapper Mode Specific Tests ========================= */

    @Test
    public void testWrapperReflectsExternalChanges() {
        // modify backing list directly
        backingList.add("quebec");
        assertTrue(populatedList.contains("quebec"));
        assertEquals(3, populatedList.size());
    }

    @Test
    public void testWrapperModificationsAffectBackingList() {
        populatedList.add("romeo");
        assertTrue(backingList.contains("romeo"));
        assertEquals(3, backingList.size());
    }

    /* ========================= Concurrency Tests ========================= */

    @Test
    public void testConcurrentAddAndRead() throws Exception {
        final int threadCount = 10;
        final int addsPerThread = 1000;
        final ConcurrentList<Integer> clist = new ConcurrentList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger totalAdded = new AtomicInteger();

        // Each thread adds a sequence of integers and reads size intermittently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < addsPerThread; j++) {
                        int value = threadId * addsPerThread + j;
                        clist.add(value);
                        totalAdded.incrementAndGet();

                        // occasional read operation to increase interleaving
                        if (j % 100 == 0) {
                            int size = clist.size();
                            assertTrue(size >= 0 && size <= totalAdded.get());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // start all workers
        startLatch.countDown();
        // wait for completion
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        // final verification
        int expectedSize = threadCount * addsPerThread;
        assertEquals(expectedSize, clist.size());

        // ensure all expected values are present (order not guaranteed)
        Set<Integer> observed = new HashSet<>();
        for (Integer v : clist) {
            observed.add(v);
        }
        assertEquals(expectedSize, observed.size());
        for (int i = 0; i < expectedSize; i++) {
            assertTrue("Missing value " + i, observed.contains(i));
        }
    }

    @Test
    public void testIteratorIsSnapshotUnderConcurrentModification() throws Exception {
        final ConcurrentList<Integer> clist = new ConcurrentList<>();
        for (int i = 0; i < 100; i++) {
            clist.add(i);
        }

        // Obtain snapshot iterator
        Iterator<Integer> snapshot = clist.iterator();

        // Concurrently modify the list
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            for (int i = 100; i < 200; i++) {
                clist.add(i);
                clist.remove(Integer.valueOf(i - 100));
            }
        });
        future.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        // The snapshot must still contain the original 0‑99 values
        List<Integer> collected = new ArrayList<>();
        snapshot.forEachRemaining(collected::add);
        assertEquals(100, collected.size());
        for (int i = 0; i < 100; i++) {
            assertTrue(collected.contains(i));
        }
    }

    /* ========================= Constructor Validation ========================= */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNullBackingList() {
        new ConcurrentList<String>(null);
    }

    @Test
    public void testInitialCapacityConstructor() {
        ConcurrentList<String> list = new ConcurrentList<>(5);
        assertTrue(list.isEmpty());
        list.add("sierra");
        assertEquals(1, list.size());
        assertEquals("sierra", list.get(0));
    }
}
