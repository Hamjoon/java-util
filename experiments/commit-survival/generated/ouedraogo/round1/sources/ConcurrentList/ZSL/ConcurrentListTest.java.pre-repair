package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentList}.
 *
 * The tests cover:
 * <ul>
 *   <li>Both constructors (no‑arg and wrapping constructor)</li>
 *   <li>All read‑only operations (equals, hashCode, toString, size, isEmpty, contains, …)</li>
 *   <li>All mutable operations (add, addAll, add at index, set, remove, clear, …)</li>
 *   <li>Iterator and ListIterator behavior (snapshot semantics)</li>
 *   <li>Unsupported operations (listIterator(int), subList(int,int))</li>
 *   <li>Basic concurrent usage (multiple writer threads + readers)</li>
 * </ul>
 *
 * No mocking frameworks are used – only the public API of {@link ConcurrentList}
 * and standard JDK classes.
 */
public class ConcurrentListTest
{
    private ConcurrentList<String> emptyList;
    private ConcurrentList<Integer> wrappedList;

    @Before
    public void setUp()
    {
        emptyList = new ConcurrentList<>();                     // uses internal ArrayList
        List<Integer> backing = new ArrayList<>(Arrays.asList(1, 2, 3));
        wrappedList = new ConcurrentList<>(backing);           // wraps existing list
    }

    /* ---------------------------------------------------------------------- */
    /* Constructor tests                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNoArgConstructor()
    {
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
        emptyList.add("one");
        assertFalse(emptyList.isEmpty());
        assertEquals(1, emptyList.size());
        assertEquals("one", emptyList.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrapConstructorRejectsNull()
    {
        new ConcurrentList<String>(null);
    }

    @Test
    public void testWrapConstructorOperatesOnOriginalList()
    {
        List<Integer> original = new ArrayList<>(Arrays.asList(10, 20));
        ConcurrentList<Integer> cl = new ConcurrentList<>(original);

        // Mutate through the ConcurrentList
        cl.add(30);
        cl.remove(Integer.valueOf(10));

        // Original reference must reflect the changes (no copy was made)
        assertEquals(Arrays.asList(20, 30), original);
    }

    /* ---------------------------------------------------------------------- */
    /* Immutable (read‑only) API tests                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode()
    {
        List<String> reference = new ArrayList<>(Arrays.asList("a", "b"));
        ConcurrentList<String> cl = new ConcurrentList<>(reference);

        assertEquals(reference, cl);
        assertEquals(reference.hashCode(), cl.hashCode());

        // Change underlying list – both should stay equal because they share data
        reference.add("c");
        assertEquals(reference, cl);
        assertEquals(reference.hashCode(), cl.hashCode());
    }

    @Test
    public void testToString()
    {
        emptyList.add("x");
        emptyList.add("y");
        assertEquals("[x, y]", emptyList.toString());
    }

    @Test
    public void testSizeAndIsEmpty()
    {
        assertTrue(emptyList.isEmpty());
        emptyList.add("elem");
        assertFalse(emptyList.isEmpty());
        assertEquals(1, emptyList.size());
    }

    @Test
    public void testContainsAndContainsAll()
    {
        emptyList.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(emptyList.contains("b"));
        assertFalse(emptyList.contains("z"));

        Collection<String> subset = Arrays.asList("a", "c");
        Collection<String> superset = Arrays.asList("a", "b", "c", "d");
        assertTrue(emptyList.containsAll(subset));
        assertFalse(emptyList.containsAll(superset));
    }

    @Test
    public void testGetIndexOfLastIndexOf()
    {
        emptyList.addAll(Arrays.asList("x", "y", "x", "z"));
        assertEquals(0, emptyList.indexOf("x"));
        assertEquals(2, emptyList.lastIndexOf("x"));
        assertEquals(-1, emptyList.indexOf("nonexistent"));
    }

    @Test
    public void testIteratorSnapshot()
    {
        emptyList.addAll(Arrays.asList("first", "second"));
        Iterator<String> iterator = emptyList.iterator();

        // Modify the underlying list after iterator creation
        emptyList.add("third");

        // The iterator should reflect the state at the time of creation (snapshot)
        assertTrue(iterator.hasNext());
        assertEquals("first", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("second", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testToArray()
    {
        emptyList.addAll(Arrays.asList("a", "b"));
        Object[] arr = emptyList.toArray();
        assertArrayEquals(new Object[]{"a", "b"}, arr);

        String[] typed = emptyList.toArray(new String[0]);
        assertArrayEquals(new String[]{"a", "b"}, typed);
    }

    /* ---------------------------------------------------------------------- */
    /* Mutable API tests                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndAddAll()
    {
        assertTrue(emptyList.add("one"));
        assertEquals(1, emptyList.size());

        assertTrue(emptyList.addAll(Arrays.asList("two", "three")));
        assertEquals(3, emptyList.size());
        assertEquals(Arrays.asList("one", "two", "three"), new ArrayList<>(emptyList));
    }

    @Test
    public void testAddAtIndexAndSet()
    {
        emptyList.addAll(Arrays.asList("a", "c"));
        emptyList.add(1, "b");               // insert at index 1
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(emptyList));

        String old = emptyList.set(2, "d");   // replace "c" with "d"
        assertEquals("c", old);
        assertEquals("d", emptyList.get(2));
    }

    @Test
    public void testRemoveByIndexAndObject()
    {
        emptyList.addAll(Arrays.asList("x", "y", "z"));
        String removed = emptyList.remove(1);   // removes "y"
        assertEquals("y", removed);
        assertFalse(emptyList.contains("y"));
        assertEquals(2, emptyList.size());

        assertTrue(emptyList.remove("z"));
        assertFalse(emptyList.contains("z"));
        assertEquals(1, emptyList.size());

        assertFalse(emptyList.remove("nonexistent"));
    }

    @Test
    public void testRemoveAllAndRetainAll()
    {
        emptyList.addAll(Arrays.asList("a", "b", "c", "d"));
        assertTrue(emptyList.removeAll(Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("a", "d"), new ArrayList<>(emptyList));

        assertTrue(emptyList.retainAll(Arrays.asList("d", "e")));
        assertEquals(Arrays.asList("d"), new ArrayList<>(emptyList));
    }

    @Test
    public void testClear()
    {
        emptyList.addAll(Arrays.asList("one", "two"));
        assertFalse(emptyList.isEmpty());
        emptyList.clear();
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    @Test
    public void testListIteratorSnapshot()
    {
        emptyList.addAll(Arrays.asList("p", "q"));
        ListIterator<String> it = emptyList.listIterator();

        // Mutate after obtaining iterator
        emptyList.add("r");

        // Iterator should still see only the snapshot ("p","q")
        assertTrue(it.hasNext());
        assertEquals("p", it.next());
        assertTrue(it.hasNext());
        assertEquals("q", it.next());
        assertFalse(it.hasNext());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedListIteratorWithIndex()
    {
        emptyList.listIterator(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedSubList()
    {
        emptyList.subList(0, 1);
    }

    /* ---------------------------------------------------------------------- */
    /* Concurrency tests                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConcurrentAddsAndReads()
        throws Exception
    {
        final int threadCount = 10;
        final int addsPerThread = 1000;
        final ConcurrentList<Integer> cl = new ConcurrentList<>();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger totalAdded = new AtomicInteger();

        // Writer threads
        for (int i = 0; i < threadCount; i++)
        {
            final int threadId = i;
            new Thread(() ->
            {
                try
                {
                    startLatch.await(); // wait for the go‑signal
                    for (int j = 0; j < addsPerThread; j++)
                    {
                        int value = threadId * addsPerThread + j;
                        cl.add(value);
                        totalAdded.incrementAndGet();
                    }
                }
                catch (InterruptedException ignored) { }
                finally
                {
                    doneLatch.countDown();
                }
            }, "writer-" + i).start();
        }

        // Reader thread that continuously reads size while writers are active
        Thread reader = new Thread(() ->
        {
            try
            {
                startLatch.await();
                while (doneLatch.getCount() > 0)
                {
                    // The size may be anywhere between 0 and totalAdded.get()
                    int size = cl.size();
                    assertTrue(size >= 0 && size <= totalAdded.get());
                }
            }
            catch (InterruptedException ignored) { }
        }, "reader");
        reader.start();

        // Kick off all threads
        startLatch.countDown();

        // Wait for writers to finish
        doneLatch.await();

        // Ensure reader also finishes
        reader.join(2000);
        assertEquals(threadCount * addsPerThread, cl.size());

        // Verify that all expected elements are present (order not guaranteed)
        for (int i = 0; i < threadCount * addsPerThread; i++)
        {
            assertTrue("Missing element " + i, cl.contains(i));
        }
    }
}
