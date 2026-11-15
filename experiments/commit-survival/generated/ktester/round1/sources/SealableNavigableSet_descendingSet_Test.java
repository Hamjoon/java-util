package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_descendingSet_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for every test
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate with a known ordering (null is allowed by the implementation)
        set.add(null);
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        set = null;
        sealedState = false;
    }

    @Test
    void descendingSetPreservesReverseOrder() {
        // The descending view should iterate in reverse natural order (null first, then 30,20,10)
        NavigableSet<Integer> descending = set.descendingSet();
        List<Integer> actual = new ArrayList<>();
        for (Integer i : descending) {
            actual.add(i);
        }
        List<Integer> expected = List.of(null, 30, 20, 10);
        assertEquals(expected, actual, "The descendingSet iterator should return elements in reverse order");
    }

    @Test
    void descendingSetIsSealableAndReflectsModificationsWhenUnsealed() {
        NavigableSet<Integer> descending = set.descendingSet();
        // The set is currently unsealed (sealedState == false)
        assertDoesNotThrow(() -> descending.add(5));
        assertTrue(set.contains(5), "Adding through the descending view must affect the original set");
        assertTrue(descending.contains(5), "The newly added element must be visible in the descending view");
        // Removing via the descending iterator should also affect the original set
        Iterator<Integer> it = descending.iterator();
        // Skip null (first element) to reach 30
        // null
        it.next();
        // 30
        it.next();
        assertDoesNotThrow(it::remove);
        assertFalse(set.contains(30), "Removal via descending iterator must remove element from original set");
    }

    @Test
    void descendingSetModificationThrowsWhenSealed() {
        NavigableSet<Integer> descending = set.descendingSet();
        // Seal the underlying set
        sealedState = true;
        // All mutating operations must now fail
        assertThrows(UnsupportedOperationException.class, () -> descending.add(40));
        assertThrows(UnsupportedOperationException.class, descending::clear);
        // Iterator removal must also be prohibited
        Iterator<Integer> it = descending.iterator();
        // can still read
        assertDoesNotThrow(it::next);
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void descendingSetIsInstanceOfSealableNavigableSet() {
        NavigableSet<Integer> descending = set.descendingSet();
        assertTrue(descending instanceof SealableNavigableSet, "descendingSet() should return a SealableNavigableSet wrapper");
    }

    @Test
    void basicDescendingSetFunctionality_orderAndSize() {
        SealableNavigableSet<Integer> original = new SealableNavigableSet<>(sealedSupplier);
        original.addAll(List.of(1, 2, 3, 4, 5));
        NavigableSet<Integer> descending = original.descendingSet();
        List<Integer> expected = List.of(5, 4, 3, 2, 1);
        List<Integer> actual = new ArrayList<>(descending);
        assertEquals(expected, actual, "Descending view should iterate in reverse order");
        assertEquals(original.size(), descending.size(), "Descending view must have same size as original");
    }

    @Test
    void basicDescendingSetFunctionality_reflectionOnNavSet() throws Exception {
        NavigableSet<Integer> backing = new TreeSet<>(List.of(1, 2, 3, 4, 5));
        SealableNavigableSet<Integer> wrapper = createSealableNavigableSet(backing, sealedSupplier);
        NavigableSet<Integer> descending = wrapper.descendingSet();
        Field navSetField = descending.getClass().getDeclaredField("navSet");
        navSetField.setAccessible(true);
        NavigableSet<Integer> internalNavSet = (NavigableSet<Integer>) navSetField.get(descending);
        assertSame(backing.descendingSet(), internalNavSet, "The internal navSet should be the descending view of the original navSet");
    }

    @Test
    void basicDescendingSetFunctionality_modificationsPropagateFromOriginal() {
        SealableNavigableSet<Integer> original = new SealableNavigableSet<>(sealedSupplier);
        original.addAll(List.of(1, 2, 3, 4, 5));
        NavigableSet<Integer> descending = original.descendingSet();
        original.add(6);
        Iterator<Integer> it = descending.iterator();
        assertEquals(6, it.next(), "Newly added element should appear first in descending view");
    }

    @Test
    void basicDescendingSetFunctionality_modificationsPropagateFromDescending() {
        SealableNavigableSet<Integer> original = new SealableNavigableSet<>(sealedSupplier);
        original.addAll(List.of(1, 2, 3, 4, 5));
        NavigableSet<Integer> descending = original.descendingSet();
        Iterator<Integer> it = descending.iterator();
        while (it.hasNext()) {
            if (it.next().equals(2)) {
                it.remove();
                break;
            }
        }
        assertFalse(original.contains(2), "Removal via descending iterator must affect original set");
        assertEquals(original.size(), descending.size(), "Sizes must stay in sync after removal");
    }

    @Test
    void doubleDescendingViewRestoresOriginalOrdering() throws Exception {
        SealableNavigableSet<Integer> original = new SealableNavigableSet<>(sealedSupplier);
        original.addAll(List.of(10, 20, 30));
        NavigableSet<Integer> firstDesc = original.descendingSet();
        NavigableSet<Integer> secondDesc = firstDesc.descendingSet();
        List<Integer> expected = List.of(10, 20, 30);
        List<Integer> actual = new ArrayList<>(secondDesc);
        assertEquals(expected, actual, "Double descending view should restore original order");
        assertNotSame(original, secondDesc, "doubleDesc should be a different wrapper instance");
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        NavigableSet<Integer> originalNavSet = (NavigableSet<Integer>) navSetField.get(original);
        NavigableSet<Integer> secondNavSet = (NavigableSet<Integer>) navSetField.get(secondDesc);
        assertEquals(originalNavSet, secondNavSet, "Underlying navSet of double descending view must equal the original navSet");
    }

    @Test
    void descendingSet_nullUnderlyingNavSet_throwsNPE() throws Exception {
        SealableNavigableSet<Integer> nullBacking = createSealableNavigableSet(null, sealedSupplier);
        assertThrows(NullPointerException.class, nullBacking::descendingSet, "Calling descendingSet() on a SealableNavigableSet with null navSet should NPE");
    }

    @Test
    void descendingSet_unsupportedOperationFromWrappedSet_propagates() {
        ???;
        SealableNavigableSet<Integer> wrapper = createSealableNavigableSet(throwingSet, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, wrapper::descendingSet, "UnsupportedOperationException from underlying set must be propagated");
    }

    @Test
    void descendingSet_nullSealedSupplier_mutatingOperationThrowsNPE() throws Exception {
        NavigableSet<Integer> backing = new TreeSet<>(List.of(1, 2, 3));
        SealableNavigableSet<Integer> wrapper = createSealableNavigableSet(backing, null);
        NavigableSet<Integer> descending = wrapper.descendingSet();
        assertNotNull(descending, "descendingSet() should succeed even with null sealedSupplier");
        assertThrows(NullPointerException.class, () -> descending.add(4), "Mutating operation with null sealedSupplier should throw NPE");
    }

    @SuppressWarnings("unchecked")
    private static <E> SealableNavigableSet<E> createSealableNavigableSet(NavigableSet<E> navSet, Supplier<Boolean> sealedSupplier) {
        try {
            Constructor<SealableNavigableSet> ctor = (Constructor<SealableNavigableSet>) SealableNavigableSet.class.getDeclaredConstructor(NavigableSet.class, Supplier.class);
            ctor.setAccessible(true);
            return ctor.newInstance(navSet, sealedSupplier);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate SealableNavigableSet via reflection", e);
        }
    }
}
