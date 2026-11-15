package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableList}.
 * All public behaviours – mutable, immutable and iterator/listIterator/subList views – are exercised.
 */
public class SealableListTest {

    private AtomicBoolean sealedFlag;
    private Supplier<Boolean> sealedSupplier;
    private SealableList<String> list;

    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false);
        sealedSupplier = sealedFlag::get;
        list = new SealableList<>(sealedSupplier);
        // start with a known state
        list.addAll(Arrays.asList("a", "b", "c"));
    }

    /** Helper – toggle sealed state */
    private void seal()   { sealedFlag.set(true); }
    private void unseal() { sealedFlag.set(false); }

    /* ----------------------------------------------------------------------
     * Immutable delegating methods
     * ---------------------------------------------------------------------- */
    @Test
    public void testEqualsHashCodeToString() {
        List<String> other = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(list.equals(other));
        assertEquals(other.hashCode(), list.hashCode());
        assertEquals(other.toString(), list.toString());
    }

    @Test
    public void testSizeAndEmpty() {
        assertEquals(3, list.size());
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testContainsAndIndexOf() {
        assertTrue(list.contains("b"));
        assertFalse(list.contains("z"));

        assertEquals(1, list.indexOf("b"));
        assertEquals(-1, list.indexOf("z"));
        assertEquals(1, list.lastIndexOf("b"));
    }

    @Test
    public void testContainsAll() {
        Collection<String> subset = Arrays.asList("a", "c");
        Collection<String> superset = Arrays.asList("a", "b", "c", "d");
        assertTrue(list.containsAll(subset));
        assertFalse(list.containsAll(superset));
    }

    @Test
    public void testGetAndToArray() {
        assertEquals("a", list.get(0));
        assertEquals("c", list.get(2));

        Object[] array = list.toArray();
        assertArrayEquals(new Object[]{"a", "b", "c"}, array);

        String[] typed = list.toArray(new String[0]);
        assertArrayEquals(new String[]{"a", "b", "c"}, typed);
    }

    /* ----------------------------------------------------------------------
     * Mutable operations – unsealed
     * ---------------------------------------------------------------------- */
    @Test
    public void testMutableOperationsWhenUnsealed() {
        unseal();

        // add
        assertTrue(list.add("d"));
        assertEquals("d", list.get(3));

        // add at index
        list.add(0, "z");
        assertEquals("z", list.get(0));
        assertEquals(5, list.size());

        // set
        String old = list.set(1, "y");
        assertEquals("a", old);
        assertEquals("y", list.get(1));

        // remove by object
        assertTrue(list.remove("b"));
        assertFalse(list.contains("b"));

        // remove by index
        String removed = list.remove(2);
        assertEquals("c", removed);

        // addAll
        assertTrue(list.addAll(Arrays.asList("x", "y")));
        assertTrue(list.containsAll(Arrays.asList("x", "y")));

        // addAll at index
        assertTrue(list.addAll(1, Arrays.asList("p", "q")));
        assertEquals("p", list.get(1));
        assertEquals("q", list.get(2));

        // retainAll
        assertTrue(list.retainAll(Arrays.asList("z", "y", "p", "q", "x")));
        assertFalse(list.contains("d"));

        // removeAll
        assertTrue(list.removeAll(Arrays.asList("z", "p")));
        assertFalse(list.contains("z"));
        assertFalse(list.contains("p"));

        // clear
        list.clear();
        assertTrue(list.isEmpty());
    }

    /* ----------------------------------------------------------------------
     * Mutable operations – sealed (should throw)
     * ---------------------------------------------------------------------- */
    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealedThrows() {
        seal();
        list.add("new");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAtIndexWhenSealedThrows() {
        seal();
        list.add(0, "new");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetWhenSealedThrows() {
        seal();
        list.set(0, "new");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveObjectWhenSealedThrows() {
        seal();
        list.remove("a");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAtIndexWhenSealedThrows() {
        seal();
        list.remove(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllWhenSealedThrows() {
        seal();
        list.addAll(Arrays.asList("x"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllAtIndexWhenSealedThrows() {
        seal();
        list.addAll(0, Arrays.asList("x"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAllWhenSealedThrows() {
        seal();
        list.removeAll(Collections.singleton("a"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAllWhenSealedThrows() {
        seal();
        list.retainAll(Collections.emptyList());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        seal();
        list.clear();
    }

    /* ----------------------------------------------------------------------
     * Iterator behaviour – remove respects seal
     * ---------------------------------------------------------------------- */
    @Test
    public void testIteratorRemoveWhenUnsealed() {
        unseal();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String v = it.next();
            if ("b".equals(v)) {
                it.remove();
            }
        }
        assertFalse(list.contains("b"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveWhenSealedThrows() {
        seal();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            it.next(); // advance to first element
            it.remove(); // should throw
        }
    }

    /* ----------------------------------------------------------------------
     * ListIterator behaviour – set, add, remove respect seal
     * ---------------------------------------------------------------------- */
    @Test
    public void testListIteratorSetAndAddWhenUnsealed() {
        unseal();
        ListIterator<String> li = list.listIterator();
        while (li.hasNext()) {
            String cur = li.next();
            if ("b".equals(cur)) {
                li.set("B");
                li.add("afterB");
            }
        }
        assertTrue(list.contains("B"));
        assertTrue(list.contains("afterB"));
        // verify order: ... "B", "afterB", "c"
        int idxB = list.indexOf("B");
        assertEquals("afterB", list.get(idxB + 1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorSetWhenSealedThrows() {
        seal();
        ListIterator<String> li = list.listIterator();
        li.next(); // move to first element
        li.set("new"); // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorAddWhenSealedThrows() {
        seal();
        ListIterator<String> li = list.listIterator();
        li.add("new");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorRemoveWhenSealedThrows() {
        seal();
        ListIterator<String> li = list.listIterator();
        li.next();
        li.remove();
    }

    /* ----------------------------------------------------------------------
     * subList – view must respect same sealed supplier
     * ---------------------------------------------------------------------- */
    @Test
    public void testSubListReflectsParentChanges() {
        unseal();
        // parent list: a,b,c
        List<String> sub = list.subList(1, 3); // should contain b,c
        assertEquals(2, sub.size());
        assertTrue(sub.contains("b"));
        assertTrue(sub.contains("c"));

        // mutate via sublist
        sub.remove("b");
        assertFalse(list.contains("b"));
        assertEquals(2, list.size());

        // add via sublist
        sub.add("d");
        assertTrue(list.contains("d"));
        assertEquals(3, list.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubListModificationWhenSealedThrows() {
        seal();
        List<String> sub = list.subList(0, 2);
        sub.add("x"); // should throw because underlying iterator add checks seal
    }

    /* ----------------------------------------------------------------------
     * Equality with different underlying List implementations
     * ---------------------------------------------------------------------- */
    @Test
    public void testEqualityWithWrappedList() {
        // wrap an existing ArrayList
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b", "c"));
        SealableList<String> wrapped = new SealableList<>(backing, sealedSupplier);
        assertTrue(wrapped.equals(list));
        assertEquals(wrapped.hashCode(), list.hashCode());

        // mutate backing directly when unsealed
        unseal();
        backing.add("d");
        assertTrue(wrapped.contains("d"));
        assertEquals(4, wrapped.size());
    }

    /* ----------------------------------------------------------------------
     * Verify that containsAll uses a HashSet copy (no side‑effects)
     * ---------------------------------------------------------------------- */
    @Test
    public void testContainsAllDoesNotModifyOriginalCollection() {
        Collection<String> col = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(list.containsAll(col));

        // mutate col after call – should not affect list outcome
        col.add("z");
        assertFalse(list.containsAll(col));
    }

    /* ----------------------------------------------------------------------
     * Verify iterator and listIterator traversal order
     * ---------------------------------------------------------------------- */
    @Test
    public void testIteratorTraversalOrder() {
        List<String> expected = Arrays.asList("a", "b", "c");
        int i = 0;
        for (String s : list) {
            assertEquals(expected.get(i++), s);
        }
        assertEquals(expected.size(), i);
    }

    @Test
    public void testListIteratorBidirectional() {
        ListIterator<String> li = list.listIterator(list.size());
        List<String> reversed = new ArrayList<>();
        while (li.hasPrevious()) {
            reversed.add(li.previous());
        }
        Collections.reverse(reversed);
        assertEquals(Arrays.asList("a", "b", "c"), reversed);
    }
}
