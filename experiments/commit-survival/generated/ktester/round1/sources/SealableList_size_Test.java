package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableList_size_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure each test starts with a fresh, un‑sealed list.
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // Populate with a known baseline of elements.
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental cross‑test contamination.
        list = null;
    }

    /**
     * Supplies test data for the parameterized size tests.
     * Each entry contains:
     *   - a description (used only for readability)
     *   - the initial elements to populate the underlying list
     *   - a Consumer that performs the mutation steps before size() is called
     *   - the expected size after the mutations
     */
    private static Stream<Arguments> sizeTestProvider() {
        return Stream.of(// group: size method
        Arguments.of("empty list → size 0", List.of(), (Runnable) () -> {
            /* no mutation */
        }, 0), Arguments.of("pre‑populated three elements → size 3", List.of("alpha", "beta", "gamma"), (Runnable) () -> {
            /* no mutation */
        }, 3), Arguments.of("one element then add → size 2", List.of("one"), (Runnable) () -> list.add("two"), 2), Arguments.of("two elements then remove index 0 → size 1", List.of("first", "second"), (Runnable) () -> list.remove(0), 1), Arguments.of("non‑empty then clear → size 0", List.of("x", "y", "z"), (Runnable) () -> list.clear(), 0), // group: size method - additional scenarios
        Arguments.of("empty then addAll 4 elements → size 4", List.of(), (Runnable) () -> list.addAll(Arrays.asList("a", "b", "c", "d")), 4), Arguments.of("three elements then removeAll (one matches) → size 2", List.of("p", "q", "r"), (Runnable) () -> list.removeAll(Arrays.asList("q", "x")), 2), Arguments.of("four elements then retainAll (keep 2) → size 2", List.of("m", "n", "o", "p"), (Runnable) () -> list.retainAll(Arrays.asList("n", "p", "z")), 2), Arguments.of("three elements then ListIterator.add → size 4", List.of("u", "v", "w"), (Runnable) () -> {
            ListIterator<String> it = list.listIterator(1);
            it.add("new");
        }, 4), Arguments.of("subList(0,2) cleared → size 1", List.of("a", "b", "c"), (Runnable) () -> {
            List<String> sub = list.subList(0, 2);
            sub.clear();
        }, 1), Arguments.of("single element then add at index 0 → size 2", List.of("x"), (Runnable) () -> list.add(0, "y"), 2), Arguments.of("ten elements, no mutation → size 10", List.of("e0", "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9"), (Runnable) () -> {
            /* no mutation */
        }, 10));
    }

    @Test
    void sizeOnEmptyList() {
        // GIVEN an empty SealableList
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // THEN size should be zero
        assertEquals(0, list.size(), "Size of a newly created list should be 0");
    }

    @Test
    void sizeReflectsNumberOfElements() {
        list = new SealableList<>(new ArrayList<>(Arrays.asList("10", "20", "30")), sealedSupplier);
        assertEquals(3, list.size(), "Initial size should match the number of added elements");
        // WHEN we add another element
        list.add("40");
        // THEN size increments
        assertEquals(4, list.size());
        // WHEN we remove an element
        list.remove("20");
        // THEN size decrements
        assertEquals(3, list.size());
    }

    @Test
    void sizeUnchangedWhenSealed() {
        list = new SealableList<>(new ArrayList<>(Arrays.asList("a", "b", "c")), sealedSupplier);
        // GIVEN a sealed list
        sealedState = true;
        // WHEN we query size (no mutation attempted)
        int sizeWhileSealed = list.size();
        // THEN the size is still the same as before sealing
        assertEquals(3, sizeWhileSealed);
    }

    @Test
    void sizeReflectsChangesThroughSubList() {
        list = new SealableList<>(new ArrayList<>(Arrays.asList("a", "b", "c")), sealedSupplier);
        // GIVEN a sub‑list view that shares the same backing list
        List<String> sub = list.subList(0, list.size());
        // WHEN we add an element via the sub‑list
        sub.add("d");
        // THEN both sub‑list and original list report the new size
        assertEquals(4, sub.size(), "Sub‑list size should reflect the addition");
        assertEquals(4, list.size(), "Original list size should reflect the addition made through sub‑list");
    }

    @Test
    void sizeAfterMultipleMutations() {
        list = new SealableList<>(new ArrayList<>(Arrays.asList("10", "20", "30")), sealedSupplier);
        assertEquals(3, list.size());
        // WHEN we perform a series of adds and removes
        list.add("40");
        list.add("50");
        list.remove("10");
        list.remove("30");
        // THEN the final size matches the net number of elements
        // Expected elements: 20, 40, 50  => size 3
        assertEquals(3, list.size());
        assertTrue(list.containsAll(List.of("20", "40", "50")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sizeTestProvider")
    void sizeParameterizedTests(String description, List<String> initialElements, Runnable mutation, int expectedSize) {
        // GIVEN a SealableList initialised with the supplied elements
        list = new SealableList<>(new ArrayList<>(initialElements), sealedSupplier);
        // WHEN the specific mutation steps are executed
        mutation.run();
        // THEN size() must reflect the expected number of elements
        assertEquals(expectedSize, list.size(), () -> String.format("Failed scenario: %s – expected size %d but got %d", description, expectedSize, list.size()));
    }
}
