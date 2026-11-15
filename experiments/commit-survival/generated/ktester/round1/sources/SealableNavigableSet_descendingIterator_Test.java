package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_descendingIterator_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh set for each test – the constructor that accepts a Supplier
        // creates an internal NavigableSet (a ConcurrentNavigableSetNullSafe) that
        // permits null values.
        set = new SealableNavigableSet<>(sealedSupplier);
        // null is allowed by the underlying set
        set.add(null);
        set.add(30);
        set.add(10);
        set.add(20);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent.
        set = null;
        sealedState = false;
    }

    @Test
    void testDescendingIteratorReturnsElementsInReverseOrder() {
        Iterator<Integer> it = set.descendingIterator();
        // The natural order of the set is [null,10,20,30]; descending should be 30,20,10,null
        assertEquals(30, it.next());
        assertEquals(20, it.next());
        assertEquals(10, it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testDescendingIteratorModificationExceptionWhenSealed() {
        Iterator<Integer> iterator = set.descendingIterator();
        // Seal the set *after* the iterator has been obtained.
        sealedState = true;
        // The iterator must still be able to read the next element.
        assertDoesNotThrow(iterator::next);
        // But any mutating operation must be prohibited.
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @Test
    void testDescendingIteratorAllowsRemovalWhenUnsealed() {
        Iterator<Integer> iterator = set.descendingIterator();
        // Ensure the set is unsealed.
        sealedState = false;
        // Move to the first element (30) and remove it.
        assertEquals(30, iterator.next());
        assertDoesNotThrow(iterator::remove);
        // The underlying set should now have size 3.
        assertEquals(3, set.size());
        // Verify the remaining order (still descending from the iterator's perspective).
        assertEquals(20, iterator.next());
        assertEquals(10, iterator.next());
        assertNull(iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void testDescendingIteratorReflectsSealStateChangesDuringIteration() {
        Iterator<Integer> iterator = set.descendingIterator();
        // First element should be accessible regardless of seal state.
        assertEquals(30, iterator.next());
        // Seal the set now – subsequent mutating calls must fail.
        sealedState = true;
        // The next element can still be read.
        assertEquals(20, iterator.next());
        // Attempt to remove should now fail.
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @Test
    void testDescendingIteratorOnEmptySet() {
        // Create an empty set (no elements added).
        set = new SealableNavigableSet<>(sealedSupplier);
        Iterator<Integer> iterator = set.descendingIterator();
        assertFalse(iterator.hasNext());
        assertThrows(java.util.NoSuchElementException.class, iterator::next);
    }

    /**
     * Group: descendingIterator - normal operation (non‑empty set) **
     */
    @Test
    void descendingIteratorNormalOperationCollectsElementsInReverse() {
        // Build a set containing {1,2,3}
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(1);
        set.add(2);
        set.add(3);
        Iterator<Integer> it = set.descendingIterator();
        List<Integer> collected = new ArrayList<>();
        it.forEachRemaining(collected::add);
        assertEquals(Arrays.asList(3, 2, 1), collected, "Iterator should traverse elements in descending order");
    }

    @Test
    void descendingIteratorNormalOperationRemovalSucceedsWhenUnsealed() {
        // Build a set containing {1,2,3}
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(1);
        set.add(2);
        set.add(3);
        Iterator<Integer> it = set.descendingIterator();
        // Remove the first element (3)
        assertEquals(3, it.next());
        assertDoesNotThrow(it::remove);
        // Verify the set now contains {1,2}
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertFalse(set.contains(3));
        assertEquals(2, set.size());
        // Next element should be 2
        assertEquals(2, it.next());
    }

    /**
     * Group: descendingIterator - empty set **
     */
    @Test
    void descendingIteratorEmptySetCollectsNoElements() {
        // empty
        set = new SealableNavigableSet<>(sealedSupplier);
        Iterator<Integer> it = set.descendingIterator();
        List<Integer> collected = new ArrayList<>();
        it.forEachRemaining(collected::add);
        assertTrue(collected.isEmpty(), "Iterator over empty set should yield no elements");
    }

    @Test
    void descendingIteratorEmptySetHasNextIsFalse() {
        set = new SealableNavigableSet<>(sealedSupplier);
        Iterator<Integer> it = set.descendingIterator();
        assertFalse(it.hasNext(), "hasNext() must be false for an empty set");
    }

    @Test
    void descendingIteratorEmptySetNextThrowsNoSuchElementException() {
        set = new SealableNavigableSet<>(sealedSupplier);
        Iterator<Integer> it = set.descendingIterator();
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    /**
     * Group: descendingIterator - null backing set **
     */
    @Test
    void descendingIteratorNullBackingSetThrowsNullPointerException() throws Exception {
        // Replace the private navSet field with null via reflection.
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        navSetField.set(set, null);
        assertThrows(NullPointerException.class, set::descendingIterator, "When navSet is null, descendingIterator() should throw NullPointerException");
    }

    /**
     * Group: descendingIterator - underlying iterator unsupported **
     */
    @Test
    void descendingIteratorUnderlyingIteratorThrowsUnsupportedOperationException() throws Exception {
        // Create a mock NavigableSet whose descendingIterator() throws UOE.
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> mockSet = mock(NavigableSet.class);
        when(mockSet.descendingIterator()).thenThrow(new UnsupportedOperationException());
        // Inject the mock into the SealableNavigableSet instance.
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        navSetField.set(set, mockSet);
        // The exception must propagate.
        assertThrows(UnsupportedOperationException.class, set::descendingIterator, "Exception from underlying iterator should be propagated");
    }

    /**
     * Group: descendingIterator - sealed set removal **
     */
    @Test
    void descendingIteratorRemovalWhenSealedThrowsIllegalStateException() {
        // Seal the set before obtaining the iterator.
        sealedState = true;
        Iterator<Integer> it = set.descendingIterator();
        // Advance to first element so that remove() is legal from iterator's perspective.
        assertEquals(30, it.next());
        // Removal must now fail with IllegalStateException (as per spec).
        assertThrows(IllegalStateException.class, it::remove, "remove() on a sealed set's iterator should throw IllegalStateException");
    }

    /**
     * Parameterized test for various seal‑state transitions **
     */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void descendingIteratorSealStateAffectsRemoval(boolean sealBeforeIteration) {
        // Arrange: optionally seal before iterator creation.
        sealedState = sealBeforeIteration;
        Iterator<Integer> it = set.descendingIterator();
        // Advance to first element.
        assertEquals(30, it.next());
        // If sealed, removal must fail; otherwise it must succeed.
        if (sealBeforeIteration) {
            assertThrows(IllegalStateException.class, it::remove);
        } else {
            assertDoesNotThrow(it::remove);
            assertFalse(set.contains(30), "Element should have been removed when unsealed");
        }
    }
}
