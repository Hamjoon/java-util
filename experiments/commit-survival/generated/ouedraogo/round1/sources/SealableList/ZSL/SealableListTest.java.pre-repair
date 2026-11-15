package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableListTest {

    private Supplier<Boolean> unsealedSupplier;
    private Supplier<Boolean> sealedSupplier;
    private AtomicBoolean sealFlag;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        sealFlag = new AtomicBoolean(false);
        unsealedSupplier = () -> sealFlag.get();
        sealedSupplier = () -> sealFlag.get();
    }

    /*** Helper to create a list with some initial data ***/
    private SealableList<String> createListWithElements(String... elems) {
        return new SealableList<>(Arrays.asList(elems), unsealedSupplier);
    }

    @Test
    public void testBasicImmutableOperationsAlwaysWork() {
        SealableList<Integer> list = createListWithElements(1, 2, 3);
        assertEquals(3, list.size());
        assertFalse(list.isEmpty());
        assertTrue(list.contains(2));
        assertEquals(Integer.valueOf(2), list.get(1));
        assertEquals(0, list.indexOf(1));
        assertEquals(2, list.lastIndexOf(3));
        assertArrayEquals(new Object[]{1, 2, 3}, list.toArray());

        // equals & hashCode delegate to underlying list
        List<Integer> other = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertTrue(list.equals(other));
        assertEquals(other.hashCode(), list.hashCode());
        assertEquals(other.toString(), list.toString());
    }

    @Test
    public void testAddAndRemoveWhenUnsealed() {
        SealableList<String> list = createListWithElements("a", "b");
        assertTrue(list.add("c"));
        assertEquals(3, list.size());
        assertEquals("c", list.get(2));

        assertTrue(list.remove("b"));
        assertEquals(2, list.size());
        assertFalse(list.contains("b"));
    }

    @Test
    public void testMutatingMethodsThrowWhenSealed() {
        SealableList<String> list = createListWithElements("x", "y");
        sealFlag.set(true); // now sealed

        // add
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("sealed");
        list.add("z");
    }

    @Test
    public void testIteratorRemoveHonorsSealState() {
        SealableList<Integer> list = createListWithElements(10, 20, 30);
        Iterator<Integer> it = list.iterator();

        // advance to first element
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(10), it.next());

        // remove while unsealed
        it.remove();
        assertEquals(2, list.size());
        assertFalse(list.contains(10));

        // seal and attempt remove via iterator
        sealFlag.set(true);
        it = list.iterator(); // new iterator over sealed list
        assertTrue(it.hasNext());
        it.next(); // point to 20
        thrown.expect(UnsupportedOperationException.class);
        it.remove();
    }

    @Test
    public void testListIteratorSetAndAddRespectSeal() {
        SealableList<String> list = createListWithElements("a", "b", "c");
        ListIterator<String> lit = list.listIterator();

        // move to first element and set it while unsealed
        assertTrue(lit.hasNext());
        assertEquals("a", lit.next());
        lit.set("alpha");
        assertEquals("alpha", list.get(0));

        // add a new element after current position
        lit.add("beta");
        assertEquals(4, list.size());
        assertEquals("beta", list.get(1));

        // now seal and verify set/add throw
        sealFlag.set(true);
        lit = list.listIterator();
        lit.next(); // point to "alpha"
        thrown.expect(UnsupportedOperationException.class);
        lit.set("new"); // should throw
    }

    @Test
    public void testSubListSealedBehaviorPropagation() {
        SealableList<Integer> list = createListWithElements(1, 2, 3, 4, 5);
        List<Integer> sub = list.subList(1, 4); // should contain 2,3,4

        assertEquals(3, sub.size());
        assertArrayEquals(new Object[]{2, 3, 4}, sub.toArray());

        // mutate sublist while unsealed
        sub.remove(Integer.valueOf(3));
        assertEquals(2, sub.size());
        assertFalse(sub.contains(3));
        assertEquals(4, list.size()); // original list reflects change

        // seal the parent and try mutating via sublist
        sealFlag.set(true);
        thrown.expect(UnsupportedOperationException.class);
        sub.add(99);
    }

    @Test
    public void testAddAllAndAddAllAtIndex() {
        SealableList<String> list = createListWithElements("a", "b");
        List<String> extra = Arrays.asList("c", "d");
        assertTrue(list.addAll(extra));
        assertEquals(Arrays.asList("a", "b", "c", "d"), new ArrayList<>(list));

        List<String> more = Arrays.asList("x", "y");
        list.addAll(1, more);
        assertEquals(Arrays.asList("a", "x", "y", "b", "c", "d"), new ArrayList<>(list));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        SealableList<Integer> list = createListWithElements(1, 2, 3, 4, 5);
        assertTrue(list.removeAll(Arrays.asList(2, 4)));
        assertEquals(Arrays.asList(1, 3, 5), new ArrayList<>(list));

        assertTrue(list.retainAll(Arrays.asList(1, 5)));
        assertEquals(Arrays.asList(1, 5), new ArrayList<>(list));
    }

    @Test
    public void testClearAndSet() {
        SealableList<String> list = createListWithElements("one", "two");
        list.set(1, "TWO");
        assertEquals("TWO", list.get(1));

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testConstructorWrappingExistingList() {
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        SealableList<String> wrapper = new SealableList<>(backing, unsealedSupplier);
        assertEquals(2, wrapper.size());

        // modify via wrapper and ensure backing list sees change
        wrapper.add("c");
        assertEquals(3, backing.size());
        assertTrue(backing.contains("c"));

        // seal and try modifying via backing list directly (should be allowed because seal only blocks wrapper)
        sealFlag.set(true);
        backing.add("d");
        assertEquals(4, wrapper.size()); // wrapper reflects external change
    }

    @Test
    public void testContainsAllUsesHashSetImplementation() {
        SealableList<Integer> list = createListWithElements(1, 2, 3, 4);
        // containsAll should be true for a superset collection
        assertTrue(list.containsAll(Arrays.asList(2, 3)));
        // false when any element missing
        assertFalse(list.containsAll(Arrays.asList(2, 5)));
    }

    @Test
    public void testToArrayWithTypedArray() {
        SealableList<String> list = createListWithElements("x", "y");
        String[] arr = list.toArray(new String[0]);
        assertArrayEquals(new String[]{"x", "y"}, arr);
    }
}
