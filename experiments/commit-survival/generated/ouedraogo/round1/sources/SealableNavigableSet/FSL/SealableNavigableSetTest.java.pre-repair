package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableNavigableSetTest {

    private Supplier<Boolean> sealedTrue;
    private Supplier<Boolean> sealedFalse;
    private AtomicBoolean sealFlag;

    @Before
    public void setUp() {
        sealFlag = new AtomicBoolean(false);
        sealedTrue = () -> true;
        sealedFalse = () -> false;
    }

    /** Helper to create a mutable set with predefined elements */
    private SealableNavigableSet<Integer> mutableSet(Integer... elems) {
        return new SealableNavigableSet<>(Arrays.asList(elems), sealedFalse);
    }

    /** Helper to create a sealed set with predefined elements */
    private SealableNavigableSet<Integer> sealedSet(Integer... elems) {
        return new SealableNavigableSet<>(Arrays.asList(elems), sealedTrue);
    }

    @Test
    public void testAddAndRemoveWhenMutable() {
        SealableNavigableSet<Integer> set = mutableSet(1, 2, 3);
        assertTrue(set.add(4));
        assertTrue(set.contains(4));
        assertTrue(set.remove(Integer.valueOf(2)));
        assertFalse(set.contains(2));
        assertEquals(3, set.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealedThrows() {
        SealableNavigableSet<Integer> set = sealedSet(1, 2);
        set.add(3); // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        SealableNavigableSet<Integer> set = sealedSet(1, 2, 3);
        set.remove(2); // should throw
    }

    @Test
    public void testClearAndPollWhenMutable() {
        SealableNavigableSet<String> set = mutableSet("a", "b");
        assertEquals("a", set.pollFirst());
        assertEquals("b", set.pollLast());
        set.add("c");
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        SealableNavigableSet<String> set = sealedSet("x");
        set.clear();
    }

    @Test
    public void testImmutableReadOnlyOperations() {
        SealableNavigableSet<Integer> set = mutableSet(5, 1, 3);
        // order is natural
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(5), set.last());
        assertTrue(set.containsAll(Arrays.asList(1, 3)));
        assertEquals(3, set.size());
        assertFalse(set.isEmpty());

        // comparator should be null for natural ordering
        assertNull(set.comparator());

        // toArray checks
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[]{1,3,5}, arr);
    }

    @Test
    public void testIteratorSealHonoringRemove() {
        SealableNavigableSet<Integer> set = mutableSet(10, 20, 30);
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(10), it.next());
        it.remove(); // allowed because mutable
        assertFalse(set.contains(10));
        assertEquals(2, set.size());

        // now seal the same underlying set via dynamic supplier
        sealFlag.set(false);
        Supplier<Boolean> dynamicSupplier = sealFlag::get;
        SealableNavigableSet<Integer> dynSet = new SealableNavigableSet<>(set, dynamicSupplier);
        Iterator<Integer> dynIt = dynSet.iterator();
        assertTrue(dynIt.hasNext());
        assertEquals(Integer.valueOf(20), dynIt.next());

        // seal it and attempt remove
        sealFlag.set(true);
        try {
            dynIt.remove();
            fail("Expected UnsupportedOperationException when removing via iterator on sealed set");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testDescendingIteratorAndSet() {
        SealableNavigableSet<String> set = mutableSet("a", "b", "c");
        NavigableSet<String> desc = set.descendingSet();
        Iterator<String> it = desc.iterator();
        List<String> order = new ArrayList<>();
        while (it.hasNext()) {
            order.add(it.next());
        }
        assertEquals(Arrays.asList("c", "b", "a"), order);

        // verify that modifications through descending set respect sealing
        desc.add("d");
        assertTrue(set.contains("d"));
        assertEquals("d", set.last());

        // seal and test immutability via descending set
        SealableNavigableSet<String> sealed = new SealableNavigableSet<>(set, sealedTrue);
        NavigableSet<String> sealedDesc = sealed.descendingSet();
        try {
            sealedDesc.add("e");
            fail("Should not be able to add when sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testSubSetHeadSetTailSetViewsRespectSeal() {
        SealableNavigableSet<Integer> base = mutableSet(1,2,3,4,5,6);
        NavigableSet<Integer> sub = base.subSet(2, true, 5, false); // {2,3,4}
        assertTrue(sub.containsAll(Arrays.asList(2,3,4)));
        assertFalse(sub.contains(5));

        // modify through subSet when mutable
        sub.remove(Integer.valueOf(3));
        assertFalse(base.contains(3));

        // now create sealed view
        SealableNavigableSet<Integer> sealedBase = new SealableNavigableSet<>(base, sealedTrue);
        NavigableSet<Integer> sealedSub = sealedBase.subSet(2, true, 5, false);
        try {
            sealedSub.add(3);
            fail("Adding via sealed subSet should fail");
        } catch (UnsupportedOperationException ignored) {}

        // headSet
        NavigableSet<Integer> head = sealedBase.headSet(4, true);
        try {
            head.clear();
            fail("Clearing via sealed headSet should fail");
        } catch (UnsupportedOperationException ignored) {}

        // tailSet
        NavigableSet<Integer> tail = sealedBase.tailSet(4, false);
        assertTrue(tail.containsAll(Arrays.asList(5,6)));
        try {
            tail.pollFirst();
            fail("Polling via sealed tailSet should fail");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testEqualsHashCodeToStringDelegation() {
        SealableNavigableSet<String> set1 = mutableSet("x", "y");
        SealableNavigableSet<String> set2 = mutableSet("x", "y");
        assertEquals(set1, set2);
        assertEquals(set1.hashCode(), set2.hashCode());
        assertEquals("[x, y]", set1.toString());
    }

    @Test
    public void testDynamicSealingChangesBehavior() {
        // start mutable
        sealFlag.set(false);
        Supplier<Boolean> dyn = sealFlag::get;
        SealableNavigableSet<Integer> set = new SealableNavigableSet<>(dyn);
        set.addAll(Arrays.asList(1,2,3));

        // mutable operations succeed
        assertTrue(set.remove(Integer.valueOf(2)));
        assertTrue(set.add(4));

        // seal now
        sealFlag.set(true);
        try {
            set.add(5);
            fail("Should not add when dynamically sealed");
        } catch (UnsupportedOperationException ignored) {}

        try {
            set.clear();
            fail("Should not clear when dynamically sealed");
        } catch (UnsupportedOperationException ignored) {}

        // read‑only operations still work
        assertTrue(set.contains(1));
        assertEquals(3, set.size());
    }
}
