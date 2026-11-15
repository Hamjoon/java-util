package com.cedarsoftware.util;

import org.junit.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableList}.
 *
 * The tests cover:
 * <ul>
 *   <li>All immutable APIs (size, isEmpty, contains, etc.)</li>
 *   <li>All mutable APIs (add, remove, clear, etc.) when the list is unsealed</li>
 *   <li>Correct throwing of {@link UnsupportedOperationException} when the list is sealed</li>
 *   <li>Iterator and ListIterator behaviour with respect to sealing</li>
 *   <li>Sub‑list handling (propagation of changes and sealing)</li>
 *   <li>Edge cases such as {@code null} elements, empty collections and duplicate values</li>
 * </ul>
 */
public class SealableListTest {

    /** Simple mutable supplier that can be toggled during a test */
    private static class ToggleSupplier implements Supplier<Boolean> {
        private final AtomicBoolean sealed = new AtomicBoolean(false);
        @Override public Boolean get() { return sealed.get(); }
        void setSealed(boolean b) { sealed.set(b); }
    }

    private ToggleSupplier supplier;
    private SealableList<String> list;

    @Before
    public void setUp() {
        supplier = new ToggleSupplier();               // starts unsealed
        list = new SealableList<>(supplier);
    }

    /* -------------------------------------------------------------
     *  Immutable API tests
     * ------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());

        list.add("a");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }

    @Test
    public void testContainsAndContainsAll() {
        list.addAll(Arrays.asList("x", "y", "z"));
        assertTrue(list.contains("y"));
        assertFalse(list.contains("w"));

        Collection<String> subset = Arrays.asList("x", "z");
        assertTrue(list.containsAll(subset));

        Collection<String> superset = Arrays.asList("x", "y", "z", "extra");
        assertFalse(list.containsAll(superset));
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        list.addAll(Arrays.asList("a", "b", "a", "c"));
        assertEquals(0, list.indexOf("a"));
        assertEquals(2, list.lastIndexOf("a"));
        assertEquals(-1, list.indexOf("nonexistent"));
    }

    @Test
    public void testGetAndToArray() {
        list.addAll(Arrays.asList("first", "second"));
        assertEquals("first", list.get(0));
        assertEquals("second", list.get(1));

        Object[] arr = list.toArray();
        assertArrayEquals(new Object[]{"first", "second"}, arr);

        String[] strArr = list.toArray(new String[0]);
        assertArrayEquals(new String[]{"first", "second"}, strArr);
    }

    @Test
    public void testEqualsAndHashCode() {
        List<String> other = new ArrayList<>(Arrays.asList("a", "b"));
        list.addAll(other);

        assertTrue(list.equals(other));
        assertEquals(other.hashCode(), list.hashCode());

        // Different list => not equal
        other.add("c");
        assertFalse(list.equals(other));
    }

    @Test
    public void testToString() {
        list.addAll(Arrays.asList("one", "two"));
        assertEquals("[one, two]", list.toString());
    }

    /* -------------------------------------------------------------
     *  Mutable API tests – unsealed (should succeed)
     * ------------------------------------------------------------- */

    @Test
    public void testAddAndAddAtIndex() {
        list.add("alpha");
        list.add(0, "beta");   // now ["beta","alpha"]
        assertEquals(Arrays.asList("beta", "alpha"), list);
    }

    @Test
    public void testSet() {
        list.addAll(Arrays.asList("old1", "old2"));
        String previous = list.set(1, "new2");
        assertEquals("old2", previous);
        assertEquals("new2", list.get(1));
    }

    @Test
    public void testRemoveByObjectAndByIndex() {
        list.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(list.remove("b"));
        assertFalse(list.remove("nonexistent"));
        assertEquals("c", list.remove(1)); // remove last element
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAllAndAddAllAtIndex() {
        list.addAll(Arrays.asList("1", "2"));
        list.addAll(1, Arrays.asList("x", "y"));
        assertEquals(Arrays.asList("1", "x", "y", "2"), list);
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        list.addAll(Arrays.asList("a", "b", "c", "d"));
        assertTrue(list.removeAll(Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("a", "d"), list);

        list.retainAll(Collections.singleton("a"));
        assertEquals(Collections.singletonList("a"), list);
    }

    @Test
    public void testClear() {
        list.addAll(Arrays.asList("x", "y"));
        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNullElementHandling() {
        list.add(null);
        assertTrue(list.contains(null));
        assertEquals(1, list.size());
        assertNull(list.get(0));
        assertTrue(list.remove(null));
        assertTrue(list.isEmpty());
    }

    /* -------------------------------------------------------------
     *  Mutable API tests – sealed (should throw)
     * ------------------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealed() {
        supplier.setSealed(true);
        list.add("shouldFail");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAtIndexWhenSealed() {
        supplier.setSealed(true);
        list.add(0, "fail");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetWhenSealed() {
        list.add("a");
        supplier.setSealed(true);
        list.set(0, "b");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveObjectWhenSealed() {
        list.add("a");
        supplier.setSealed(true);
        list.remove("a");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveIndexWhenSealed() {
        list.add("a");
        supplier.setSealed(true);
        list.remove(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllWhenSealed() {
        supplier.setSealed(true);
        list.addAll(Arrays.asList("x"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllAtIndexWhenSealed() {
        supplier.setSealed(true);
        list.addAll(0, Arrays.asList("x"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAllWhenSealed() {
        list.addAll(Arrays.asList("a", "b"));
        supplier.setSealed(true);
        list.removeAll(Collections.singleton("a"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAllWhenSealed() {
        list.addAll(Arrays.asList("a", "b"));
        supplier.setSealed(true);
        list.retainAll(Collections.singleton("a"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealed() {
        list.add("a");
        supplier.setSealed(true);
        list.clear();
    }

    /* -------------------------------------------------------------
     *  Iterator behaviour
     * ------------------------------------------------------------- */

    @Test
    public void testIteratorRemoveWhenUnsealed() {
        list.addAll(Arrays.asList("one", "two", "three"));
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("two".equals(val)) {
                it.remove();   // should succeed
            }
        }
        assertEquals(Arrays.asList("one", "three"), list);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemoveWhenSealed() {
        list.addAll(Arrays.asList("a", "b"));
        supplier.setSealed(true);
        Iterator<String> it = list.iterator();
        it.next();
        it.remove();   // must throw
    }

    @Test
    public void testListIteratorSetAndAddWhenUnsealed() {
        list.addAll(Arrays.asList("x", "y"));
        ListIterator<String> lit = list.listIterator();
        lit.next();               // points at "x"
        lit.set("X");             // replace
        lit.add("Z");             // insert after "X"
        // Expected order: X, Z, y
        assertEquals(Arrays.asList("X", "Z", "y"), list);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorSetWhenSealed() {
        list.addAll(Arrays.asList("a"));
        supplier.setSealed(true);
        ListIterator<String> lit = list.listIterator();
        lit.next();
        lit.set("b");   // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorAddWhenSealed() {
        list.addAll(Arrays.asList("a"));
        supplier.setSealed(true);
        ListIterator<String> lit = list.listIterator();
        lit.add("b");   // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorRemoveWhenSealed() {
        list.addAll(Arrays.asList("a"));
        supplier.setSealed(true);
        ListIterator<String> lit = list.listIterator();
        lit.next();
        lit.remove();   // should throw
    }

    /* -------------------------------------------------------------
     *  Sub‑list handling
     * ------------------------------------------------------------- */

    @Test
    public void testSubListReflectsParentChangesWhenUnsealed() {
        list.addAll(Arrays.asList("0","1","2","3","4"));
        List<String> sub = list.subList(1, 4);   // ["1","2","3"]
        assertEquals(Arrays.asList("1","2","3"), sub);

        // modify via sub‑list
        sub.remove("2");
        sub.add("X");
        assertEquals(Arrays.asList("1","3","X"), sub);
        // parent sees the same changes
        assertEquals(Arrays.asList("0","1","3","X","4"), list);
    }

    @Test
    public void testSubListHonorsSealing() {
        list.addAll(Arrays.asList("a","b","c"));
        List<String> sub = list.subList(0, 2); // ["a","b"]
        supplier.setSealed(true);
        // Attempt mutating through sub‑list must fail
        try {
            sub.add("z");
            fail("Expected UnsupportedOperationException from subList.add when sealed");
        } catch (UnsupportedOperationException ignored) { }

        try {
            sub.remove(0);
            fail("Expected UnsupportedOperationException from subList.remove when sealed");
        } catch (UnsupportedOperationException ignored) { }

        // Immutable reads still work
        assertEquals("a", sub.get(0));
        assertTrue(sub.contains("b"));
    }

    /* -------------------------------------------------------------
     *  Construction variations
     * ------------------------------------------------------------- */

    @Test
    public void testConstructorWithCollection() {
        Collection<String> src = Arrays.asList("c1", "c2");
        SealableList<String> fromCol = new SealableList<>(src, supplier);
        assertEquals(src, fromCol);
        // ensure independent internal list (modifying source does not affect SealableList)
        ((ArrayList<String>)src).add("c3");
        assertEquals(2, fromCol.size());
    }

    @Test
    public void testConstructorWithExistingList() {
        List<String> backing = new ArrayList<>(Arrays.asList("x","y"));
        SealableList<String> wrapper = new SealableList<>(backing, supplier);
        assertEquals(backing, wrapper);
        wrapper.add("z");
        assertEquals(Arrays.asList("x","y","z"), backing);
    }

    /* -------------------------------------------------------------
     *  Miscellaneous edge cases
     * ------------------------------------------------------------- */

    @Test
    public void testContainsAllWithEmptyCollection() {
        list.addAll(Arrays.asList("a","b"));
        assertTrue(list.containsAll(Collections.emptyList()));
    }

    @Test
    public void testAddAllEmptyCollection() {
        assertTrue(list.addAll(Collections.emptyList()));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRemoveAllEmptyCollection() {
        list.addAll(Arrays.asList("a"));
        assertFalse(list.removeAll(Collections.emptyList()));
        assertEquals(1, list.size());
    }

    @Test
    public void testRetainAllEmptyCollection() {
        list.addAll(Arrays.asList("a","b"));
        assertTrue(list.retainAll(Collections.emptyList()));
        assertTrue(list.isEmpty());
    }
}
