package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableList_isEmpty_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure each test starts with a fresh, un‑sealed, empty list
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse
        list = null;
    }

    private static Stream<Arguments> emptyListScenarios() {
        return Stream.of(// 1. newly created empty list
        Arguments.of((Consumer<SealableList<Integer>>) l -> {
        }, /* no mutation */
        true, "A newly created SealableList wrapping an empty backing list should report isEmpty() == true."), // 2. clear() on a previously non‑empty list
        Arguments.of((Consumer<SealableList<Integer>>) l -> {
            l.add(1);
            l.add(2);
            l.clear();
        }, true, "After calling clear() on a non‑empty SealableList, isEmpty() must return true."), // 3. list contains only null element, then removed
        Arguments.of((Consumer<SealableList<Integer>>) l -> {
            l.add(null);
            l.remove(null);
        }, // list is now non‑empty (size 1)
        // remove the null element
        true, "A SealableList that contains only null elements is still considered non‑empty; however, after removing that element the list becomes empty and isEmpty() returns true."));
    }

    private static Stream<Arguments> nonEmptyListScenarios() {
        return Stream.of(// 1. backing list already contains an element
        Arguments.of((Consumer<SealableList<Integer>>) l -> {
            l.add(1);
        }, // replace the backing list with a pre‑populated one
        // (cannot change the internal list directly, so we simulate by adding)
        false, "A newly created SealableList wrapping a non‑empty backing list (e.g., initialized with [1]) should report isEmpty() == false."), // 2. add a single non‑null element via add(T)
        Arguments.of((Consumer<SealableList<Integer>>) l -> l.add(42), false, "After adding a single non‑null element via add(T), isEmpty() must return false."), // 3. add multiple elements via addAll(Collection)
        Arguments.of((Consumer<SealableList<Integer>>) l -> l.addAll(Arrays.asList(3, 4, 5)), false, "After adding multiple elements via addAll(Collection), isEmpty() must return false."), // 4. list contains only a null element
        Arguments.of((Consumer<SealableList<Integer>>) l -> l.add(null), false, "A SealableList that contains only a null element (added via add(null)) is considered non‑empty; isEmpty() returns false."));
    }

    private static Stream<Arguments> stateTransitionScenarios() {
        return Stream.of(// 1. add then remove the same element
        Arguments.of((Consumer<SealableList<Integer>>) l -> {
            l.add(99);
            l.remove(Integer.valueOf(99));
        }, true, "After adding a non‑null element and then removing it with remove(Object), the list becomes empty and isEmpty() returns true."), // 2. add several elements then clear()
        Arguments.of((Consumer<SealableList<Integer>>) l -> {
            l.addAll(Arrays.asList(1, 2, 3));
            l.clear();
        }, true, "After adding several elements and calling clear(), isEmpty() must return true."), // 3. add elements then removeAll with a collection containing all of them
        Arguments.of((Consumer<SealableList<Integer>>) l -> {
            List<Integer> elems = Arrays.asList(7, 8, 9);
            l.addAll(elems);
            l.removeAll(elems);
        }, true, "After adding elements and invoking removeAll with a collection that contains all current elements, the list becomes empty and isEmpty() returns true."), // 4. add elements then retainAll with an empty collection
        Arguments.of((Consumer<SealableList<Integer>>) l -> {
            l.addAll(Arrays.asList(10, 11));
            l.retainAll(Collections.emptyList());
        }, true, "After adding elements and invoking retainAll with an empty collection, all elements are removed and isEmpty() returns true."));
    }

    @Test
    void testIsEmptyOnNewList() {
        assertTrue(list.isEmpty(), "A newly created list should be empty");
        assertEquals(0, list.size(), "Size of a newly created list should be 0");
    }

    @Test
    void testIsEmptyAfterAddingElements() {
        list.add(10);
        list.add(20);
        assertFalse(list.isEmpty(), "List should not be empty after adding elements");
        assertEquals(2, list.size(), "Size should reflect the number of added elements");
    }

    @Test
    void testIsEmptyWhenSealedAndEmpty() {
        // seal the list
        sealedState = true;
        assertTrue(list.isEmpty(), "Empty list should remain empty when sealed");
    }

    @Test
    void testIsEmptyWhenSealedAndNotEmpty() {
        list.add(42);
        // seal after adding
        sealedState = true;
        assertFalse(list.isEmpty(), "Non‑empty list should report not empty even when sealed");
    }

    @Test
    void testIsEmptyAfterClearing() {
        list.add(1);
        list.add(2);
        assertFalse(list.isEmpty(), "List should not be empty after adding elements");
        list.clear();
        assertTrue(list.isEmpty(), "List should be empty after clear()");
        assertEquals(0, list.size(), "Size should be 0 after clear()");
    }

    @Test
    void testIsEmptyRemainsConsistentAcrossSealToggle() {
        list.add(5);
        assertFalse(list.isEmpty(), "Initially not empty");
        sealedState = true;
        assertFalse(list.isEmpty(), "Still not empty when sealed");
        sealedState = false;
        assertFalse(list.isEmpty(), "Still not empty after unsealing");
        list.remove(Integer.valueOf(5));
        assertTrue(list.isEmpty(), "Empty after removing the only element");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("emptyListScenarios")
    void testEmptyListScenarios(Consumer<SealableList<Integer>> setup, boolean expected, String description) {
        // Apply the scenario‑specific set‑up
        setup.accept(list);
        // Verify the result of isEmpty()
        assertEquals(expected, list.isEmpty(), () -> "Failed scenario: " + description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nonEmptyListScenarios")
    void testNonEmptyListScenarios(Consumer<SealableList<Integer>> setup, boolean expected, String description) {
        setup.accept(list);
        assertEquals(expected, list.isEmpty(), () -> "Failed scenario: " + description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("stateTransitionScenarios")
    void testStateTransitionScenarios(Consumer<SealableList<Integer>> setup, boolean expected, String description) {
        setup.accept(list);
        assertEquals(expected, list.isEmpty(), () -> "Failed scenario: " + description);
    }

    @Test
    void testIsEmptyThrowsWhenBackingListIsNull() {
        // Construct a SealableList with a null backing list – this should cause NPE on any operation.
        SealableList<Integer> nullBackedList = new SealableList<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, nullBackedList::isEmpty, "Constructing a SealableList with a null backing list should cause isEmpty() to throw NullPointerException.");
    }
}
