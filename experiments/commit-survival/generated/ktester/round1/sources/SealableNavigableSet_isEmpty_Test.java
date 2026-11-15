package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_isEmpty_Test {

    /**
     * The SealableNavigableSet under test
     */
    private SealableNavigableSet<Integer> set;

    /**
     * Mutable flag used by the Supplier to indicate sealed/unsealed state
     */
    private static volatile boolean sealedState = false;

    /**
     * Supplier that simply returns the current value of {@code sealedState}
     */
    private static final Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // By default start with an *unsealed* empty set.
        sealedState = false;
        set = new SealableNavigableSet<>(sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no cross‑test contamination.
        set = null;
    }

    @Test
    void isEmpty_onFreshSet_returnsTrue() {
        assertTrue(set.isEmpty(), "A newly created SealableNavigableSet should be empty");
    }

    @Test
    void isEmpty_afterAddingElements_returnsFalse() {
        set.add(1);
        set.add(2);
        set.add(3);
        assertFalse(set.isEmpty(), "Set with elements must report non‑empty");
    }

    @Test
    void isEmpty_afterClear_returnsTrue() {
        set.add(10);
        set.add(20);
        assertFalse(set.isEmpty(), "Pre‑condition: set is not empty");
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
    }

    @Test
    void isEmpty_whenSealedStateIsTrue_behavesLikeUnsealed() {
        set.add(5);
        set.add(15);
        // switch to sealed mode
        sealedState = true;
        assertFalse(set.isEmpty(), "Sealed state must not affect isEmpty() result");
        // clear() would normally throw when sealed; we catch it to keep focus on isEmpty()
        try {
            set.clear();
        } catch (UnsupportedOperationException ignored) {
        }
        // After the attempted clear (which fails), the set is still non‑empty.
        // To verify isEmpty() still works, we unseal and clear again.
        sealedState = false;
        set.clear();
        assertTrue(set.isEmpty(), "After successful clear (when unsealed) the set is empty");
    }

    @Test
    void isEmpty_usingComparatorConstructor() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        set = new SealableNavigableSet<>(reverse, sealedSupplier);
        assertTrue(set.isEmpty(), "Set constructed with comparator should start empty");
        set.add(42);
        assertFalse(set.isEmpty(), "After adding an element, set is not empty");
    }

    @Test
    void isEmpty_usingCollectionConstructor() {
        Collection<Integer> initial = Arrays.asList(7, 8, 9);
        set = new SealableNavigableSet<>(initial, sealedSupplier);
        assertFalse(set.isEmpty(), "Collection‑based constructor should populate the set");
    }

    @Test
    void isEmpty_usingSortedSetConstructor() {
        SortedSet<Integer> sorted = new TreeSet<>(Arrays.asList(3, 1, 2));
        set = new SealableNavigableSet<>(sorted, sealedSupplier);
        assertFalse(set.isEmpty(), "SortedSet‑based constructor should populate the set");
    }

    @Test
    void isEmpty_usingNavigableSetConstructor() {
        NavigableSet<Integer> nav = new TreeSet<>(Arrays.asList(11, 22));
        set = new SealableNavigableSet<>(nav, sealedSupplier);
        assertFalse(set.isEmpty(), "NavigableSet‑based constructor should populate the set");
    }

    @Test
    @DisplayName("isEmpty: underlying navSet is null -> NullPointerException")
    void isEmpty_nullUnderlying_throwsNullPointerException() throws Exception {
        // Create a normal instance first
        set = new SealableNavigableSet<>(sealedSupplier);
        // Use reflection to set the private 'navSet' field to null
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        navSetField.set(set, null);
        // Verify that calling isEmpty() now throws NPE
        assertThrows(NullPointerException.class, () -> set.isEmpty(), "Calling isEmpty() when internal navSet is null should throw NullPointerException");
    }

    @Test
    @DisplayName("isEmpty: underlying NavigableSet throws IllegalStateException -> propagated")
    void isEmpty_underlyingThrows_propagatesException() throws Exception {
        // Mock a NavigableSet that throws IllegalStateException on isEmpty()
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> mockNavSet = mock(NavigableSet.class);
        when(mockNavSet.isEmpty()).thenThrow(new IllegalStateException("Corrupted state"));
        // Build a SealableNavigableSet using a constructor that accepts a NavigableSet.
        // Assuming such a constructor exists (mirroring other constructors in the API).
        // If not, we fall back to reflection injection after default construction.
        try {
            set = new SealableNavigableSet<>(mockNavSet, sealedSupplier);
        } catch (NoSuchMethodError | IllegalArgumentException e) {
            // Fallback: create a normal instance then inject the mock via reflection.
            set = new SealableNavigableSet<>(sealedSupplier);
            Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
            navSetField.setAccessible(true);
            navSetField.set(set, mockNavSet);
        }
        // The exception should be propagated unchanged.
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> set.isEmpty(), "isEmpty() should propagate IllegalStateException from the underlying set");
        assertEquals("Corrupted state", thrown.getMessage(), "Exception message should be preserved");
    }
}
