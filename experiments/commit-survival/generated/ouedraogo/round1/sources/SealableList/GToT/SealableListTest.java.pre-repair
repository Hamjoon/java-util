package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link SealableList}.
 *
 * The tests cover:
 *  <ul>
 *      <li>All public methods (read‑only and mutable)</li>
 *      <li>Typical use cases</li>
 *      <li>Edge cases such as null elements, empty collections, and index bounds</li>
 *      <li>Exception handling when the list is sealed (mutation attempts should throw)</li>
 *      <li>Iterator / ListIterator behaviour under sealed and unsealed states</li>
 *      <li>Sub‑list view correctness and seal‑honouring</li>
 *  </ul>
 */
public class SealableListTest {

    /** Supplier that can be toggled between sealed (true) and mutable (false). */
    private AtomicBoolean sealedFlag;

    /** A fresh unsealed list before each test. */
    private SealableList<String> unsealedList;

    /** A fresh sealed list before each test. */
    private SealableList<String> sealedList;

    /** Simple supplier that returns the current state of {@link #sealedFlag}. */
    private Supplier<Boolean> sealedSupplier() {
        return sealedFlag::get;
    }

    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false);
        unsealedList = new SealableList<>(sealedSupplier());
        sealedList = new SealableList<>(sealedSupplier());
        // Populate both lists with the same initial data
        Collections.addAll(unsealedList, "alpha", "beta", "gamma");
        Collections.addAll(sealedList, "alpha", "beta", "gamma");
    }

    /* ----------------------------------------------------------------------
     *  Helper methods for creating lists in different states
     * ---------------------------------------------------------------------- */

    private void seal() {
        sealedFlag.set(true);
    }

    private void unseal() {
        sealedFlag.set(false);
    }

    /* ----------------------------------------------------------------------
     *  Tests for immutable/read‑only API
     * ---------------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(3, unsealedList.size());
        assertFalse(unsealedList.isEmpty());

        unsealedList.clear();
        assertEquals(0, unsealedList.size());
        assertTrue(unsealedList.isEmpty());
    }

    @Test
    public void testContainsAndContainsAll() {
        assertTrue(unsealedList.contains("beta"));
        assertFalse(unsealedList.contains("delta"));

        Collection<String> subset = Arrays.asList("alpha", "gamma");
        assertTrue(unsealedList.containsAll(subset));

        Collection<String> superset = Arrays.asList("alpha", "beta", "gamma", "delta");
        assertFalse(unsealedList.containsAll(superset));
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        unsealedList.add("beta"); // duplicate element
        assertEquals(1, unsealedList.indexOf("beta"));
        assertEquals(3, unsealedList.lastIndexOf("beta"));
    }

    @Test
    public void testGet() {
        assertEquals("alpha", unsealedList.get(0));
        assertEquals("gamma", unsealedList.get(2));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetOutOfBounds() {
        unsealedList.get(5);
    }

    @Test
    public void testToArray() {
        Object[] arr = unsealedList.toArray();
        assertArrayEquals(new Object[]{"alpha", "beta", "gamma"}, arr);
    }

    @Test
    public void testToArrayWithSuppliedArray() {
        String[] supplied = new String[5];
        String[] result = unsealedList.toArray(supplied);
        assertSame(supplied, result); // same array is returned when large enough
        assertEquals("alpha", result[0]);
        assertNull(result[3]); // extra slot should be null
    }

    @Test
    public void testEqualsAndHashCode() {
        List<String> other = new ArrayList<>(Arrays.asList("alpha", "beta", "gamma"));
        assertTrue(unsealedList.equals(other));
        assertEquals(unsealedList.hashCode(), other.hashCode());

        other.add("delta");
        assertFalse(unsealedList.equals(other));
    }

    @Test
    public void testToString() {
        assertEquals("[alpha, beta, gamma]", unsealedList.toString());
    }

    @Test
    public void testIteratorReadOnly() {
        Iterator<String> it = unsealedList.iterator();
        List<String> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        assertEquals(unsealedList, collected);
    }

    @Test
    public void testListIteratorReadOnly() {
        ListIterator<String> lit = unsealedList.listIterator();
        assertTrue(lit.hasNext());
        assertEquals("alpha", lit.next());
        assertTrue(lit.hasPrevious());
        assertEquals("alpha", lit.previous());
    }

    @Test
    public void testSubList() {
        List<String> sub = unsealedList.subList(1, 3);
        assertEquals(Arrays.asList("beta", "gamma"), sub);
        // Mutations on sub‑list should affect the original when unsealed
        sub.remove(0);
        assertEquals(Arrays.asList("alpha", "gamma"), unsealedList);
    }

    /* ----------------------------------------------------------------------
     *  Tests for mutable API when list is unsealed
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndAddAtIndex() {
        assertTrue(unsealedList.add("delta"));
        assertEquals(4, unsealedList.size());
        unsealedList.add(0, "zero");
        assertEquals("zero", unsealedList.get(0));
    }

    @Test
    public void testAddNullElement() {
        assertTrue(unsealedList.add(null));
        assertTrue(unsealedList.contains(null));
    }

    @Test
    public void testAddAll() {
        Collection<String> extra = Arrays.asList("delta", "epsilon");
        assertTrue(unsealedList.addAll(extra));
        assertEquals(5, unsealedList.size());
        assertTrue(unsealedList.containsAll(extra));
    }

    @Test
    public void testAddAllAtIndex() {
        Collection<String> extra = Arrays.asList("delta", "epsilon");
        assertTrue(unsealedList.addAll(1, extra));
        assertEquals(Arrays.asList("alpha", "delta", "epsilon", "beta", "gamma"), unsealedList);
    }

    @Test
    public void testRemoveObject() {
        assertTrue(unsealedList.remove("beta"));
        assertFalse(unsealedList.contains("beta"));
    }

    @Test
    public void testRemoveByIndex() {
        String removed = unsealedList.remove(1);
        assertEquals("beta", removed);
        assertFalse(unsealedList.contains("beta"));
    }

    @Test
    public void testRemoveAll() {
        Collection<String> toRemove = Arrays.asList("alpha", "gamma");
        assertTrue(unsealedList.removeAll(toRemove));
        assertEquals(Collections.singletonList("beta"), unsealedList);
    }

    @Test
    public void testRetainAll() {
        Collection<String> toRetain = Collections.singleton("beta");
        assertTrue(unsealedList.retainAll(toRetain));
        assertEquals(Collections.singletonList("beta"), unsealedList);
    }

    @Test
    public void testClear() {
        unsealedList.clear();
        assertTrue(unsealedList.isEmpty());
    }

    @Test
    public void testSet() {
        String old = unsealedList.set(1, "newBeta");
        assertEquals("beta", old);
        assertEquals("newBeta", unsealedList.get(1));
    }

    @Test
    public void testIteratorRemoveWhenUnsealed() {
        Iterator<String> it = unsealedList.iterator();
        while (it.hasNext()) {
            if ("beta".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(unsealedList.contains("beta"));
    }

    @Test
    public void testListIteratorMutationsWhenUnsealed() {
        ListIterator<String> lit = unsealedList.listIterator();
        while (lit.hasNext()) {
            String val = lit.next();
            if ("beta".equals(val)) {
                lit.set("BETA");
                lit.add("afterBeta");
            }
        }
        assertEquals(Arrays.asList("alpha", "BETA", "afterBeta", "gamma"), unsealedList);
    }

    /* ----------------------------------------------------------------------
     *  Tests for mutable API when list is sealed – expecting exceptions
     * ---------------------------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealed() {
        seal();
        sealedList.add("new");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAtIndexWhenSealed() {
        seal();
        sealedList.add(0, "new");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllWhenSealed() {
        seal();
        sealedList.addAll(Arrays.asList("x", "y"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllAtIndexWhenSealed() {
        seal();
        sealedList.addAll(1, Arrays.asList("x"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveObjectWhenSealed() {
        seal();
        sealedList.remove("alpha");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveByIndexWhenSealed() {
        seal();
        sealedList.remove(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAllWhenSealed() {
        seal();
        sealedList.removeAll(Collections.singleton("alpha"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAllWhenSealed() {
        seal();
        sealedList.retainAll(Collections.singleton("beta"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealed() {
        seal();
        sealedList.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetWhenSealed() {
        seal();
        sealedList.set(0, "new");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveWhenSealed() {
        seal();
        Iterator<String> it = sealedList.iterator();
        while (it.hasNext()) {
            if ("beta".equals(it.next())) {
                it.remove(); // should throw
            }
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorRemoveWhenSealed() {
        seal();
        ListIterator<String> lit = sealedList.listIterator();
        while (lit.hasNext()) {
            if ("beta".equals(lit.next())) {
                lit.remove(); // should throw
            }
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorSetWhenSealed() {
        seal();
        ListIterator<String> lit = sealedList.listIterator();
        while (lit.hasNext()) {
            if ("beta".equals(lit.next())) {
                lit.set("new"); // should throw
            }
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorAddWhenSealed() {
        seal();
        ListIterator<String> lit = sealedList.listIterator();
        while (lit.hasNext()) {
            lit.next();
        }
        lit.add("new"); // should throw
    }

    /* ----------------------------------------------------------------------
     *  Tests for sub‑list view honouring the seal state
     * ---------------------------------------------------------------------- */

    @Test
    public void testSubListMutationHonorsSeal() {
        List<String> sub = sealedList.subList(0, 2); // ["alpha","beta"]
        // Sub‑list should be mutable while parent is unsealed
        sub.add("subNew");
        assertTrue(sealedList.contains("subNew"));

        // Now seal the parent and attempt mutation via sub‑list
        seal();
        try {
            sub.remove("alpha");
            fail("Expected UnsupportedOperationException from sub‑list when parent sealed");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* ----------------------------------------------------------------------
     *  Tests for constructors with initial collections / lists
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorWithCollection() {
        Collection<String> init = Arrays.asList("one", "two");
        SealableList<String> list = new SealableList<>(init, sealedSupplier());
        assertEquals(2, list.size());
        assertTrue(list.containsAll(init));
    }

    @Test
    public void testConstructorWithExistingList() {
        List<String> backing = new ArrayList<>(Arrays.asList("x", "y"));
        SealableList<String> wrapper = new SealableList<>(backing, sealedSupplier());

        // Mutate through wrapper – should affect backing list when unsealed
        wrapper.add("z");
        assertEquals(Arrays.asList("x", "y", "z"), backing);
    }

    @Test
    public void testConstructorWithSupplierOnly() {
        SealableList<Integer> list = new SealableList<>(sealedSupplier());
        assertTrue(list.isEmpty());
        list.add(1);
        assertEquals(Integer.valueOf(1), list.get(0));
    }
}
