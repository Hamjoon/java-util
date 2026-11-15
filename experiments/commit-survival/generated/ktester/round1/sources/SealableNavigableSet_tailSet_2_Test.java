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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_tailSet_2_Test {

    /**
     * shared mutable sealed flag used by the supplier
     */
    private static volatile boolean sealedState;

    /**
     * supplier that reports the current sealed state
     */
    private static Supplier<Boolean> sealedSupplier;

    /**
     * the set under test
     */
    private SealableNavigableSet<Integer> set;

    @BeforeAll
    static void setupBeforeAll() {
        // initialise the supplier once for the whole test run
        sealedSupplier = () -> sealedState;
        sealedState = false;
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh set for each test – uses the two‑arg constructor that creates an empty backing TreeSet
        set = new SealableNavigableSet<>(new TreeSet<>(), sealedSupplier);
        // deterministic ordering
        set.add(10);
        set.add(20);
        set.add(30);
        set.add(40);
        // start each test un‑sealed
        sealedState = false;
    }

    @AfterEach
    void teardownAfterEach() {
        // reset sealed flag so that a failure in one test does not affect the next
        sealedState = false;
    }

    /**
     * Helper to build a SealableNavigableSet backed by a TreeSet (or a supplied comparator).
     */
    private static SealableNavigableSet<Integer> buildSet(Collection<Integer> elements, Comparator<Integer> comparator) {
        NavigableSet<Integer> backing = (comparator == null) ? new TreeSet<>() : new TreeSet<>(comparator);
        backing.addAll(elements);
        return new SealableNavigableSet<>(backing, sealedSupplier);
    }

    /**
     * Convert a string like "[2, 3, 4]" into a List<Integer>.
     */
    private static List<Integer> parseExpected(String expected) {
        if (expected.isEmpty() || expected.equals("[]")) {
            return Collections.emptyList();
        }
        String inner = expected.replaceAll("[\\[\\]\\s]", "");
        String[] parts = inner.split(",");
        List<Integer> list = new ArrayList<>();
        for (String p : parts) {
            list.add(Integer.valueOf(p));
        }
        return list;
    }

    static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// fromElement = 2, inclusive = true → [2,3,4]
        Arguments.of(2, true, "[2, 3, 4]"), // fromElement = 2, inclusive = false → [3,4]
        Arguments.of(2, false, "[3, 4]"), // fromElement = 5, inclusive = true → []
        Arguments.of(5, true, "[]"), // fromElement = 0, inclusive = false → [1,2,3,4]
        Arguments.of(0, false, "[1, 2, 3, 4]"));
    }

    static Stream<Arguments> sealedBehaviorProvider() {
        return Stream.of(// add should fail when sealed
        Arguments.of(2, true, "add"), // remove should fail when sealed
        Arguments.of(2, false, "remove"));
    }

    static Stream<Arguments> comparatorHandlingProvider() {
        return Stream.of(// descending comparator, inclusive → [4,3]
        Arguments.of(3, true, "[4, 3]"), // descending comparator, exclusive → [4]
        Arguments.of(3, false, "[4]"));
    }

    static Stream<Arguments> emptySetProvider() {
        return Stream.of(Arguments.of(1, true), Arguments.of(1, false));
    }

    @Test
    void tailSetInclusiveContainsFromElement() {
        NavigableSet<Integer> tail = set.tailSet(20, true);
        assertTrue(tail.contains(20), "Inclusive tailSet must contain the fromElement");
        assertTrue(tail.contains(30));
        assertTrue(tail.contains(40));
        assertFalse(tail.contains(10));
        assertEquals(3, tail.size());
    }

    @Test
    void tailSetExclusiveExcludesFromElement() {
        NavigableSet<Integer> tail = set.tailSet(20, false);
        assertFalse(tail.contains(20), "Exclusive tailSet must NOT contain the fromElement");
        assertTrue(tail.contains(30));
        assertTrue(tail.contains(40));
        assertFalse(tail.contains(10));
        assertEquals(2, tail.size());
    }

    @Test
    void tailSetReflectsChangesInOriginalSet() {
        NavigableSet<Integer> tail = set.tailSet(20, true);
        // add a new element to the underlying set that belongs to the tail view
        set.add(25);
        assertTrue(tail.contains(25), "Tail view should reflect additions to the backing set");
        // remove an element via the tail view and verify the original set sees it
        tail.remove(30);
        assertFalse(set.contains(30), "Removal through tail view must affect the original set");
    }

    @Test
    void tailSetIsSealedWhenSupplierReturnsTrue() {
        NavigableSet<Integer> tail = set.tailSet(20, true);
        // switch to sealed mode
        sealedState = true;
        assertThrows(IllegalStateException.class, () -> tail.add(50), "Adding to a sealed tailSet must throw IllegalStateException");
        assertThrows(IllegalStateException.class, tail::clear, "Clearing a sealed tailSet must throw IllegalStateException");
        // iterator removal should also be prohibited
        assertDoesNotThrow(tail::iterator);
        assertThrows(IllegalStateException.class, () -> tail.iterator().remove(), "Iterator.remove on a sealed tailSet must throw IllegalStateException");
    }

    @Test
    void tailSetReturnedIsAlsoSealableNavigableSet() {
        NavigableSet<Integer> tail = set.tailSet(20, true);
        assertTrue(tail instanceof SealableNavigableSet, "The view returned by tailSet should be a SealableNavigableSet");
    }

    @ParameterizedTest(name = "tailSet({0}, {1}) → {2}")
    @MethodSource("basicFunctionalityProvider")
    void tailSetBasicFunctionality(Integer fromElement, boolean inclusive, String expectedString) {
        // underlying set = [1,2,3,4]
        SealableNavigableSet<Integer> localSet = buildSet(Arrays.asList(1, 2, 3, 4), null);
        NavigableSet<Integer> tail = localSet.tailSet(fromElement, inclusive);
        List<Integer> expected = parseExpected(expectedString);
        assertEquals(expected, new ArrayList<>(tail), () -> "tailSet(" + fromElement + ", " + inclusive + ") returned unexpected elements");
    }

    @Test
    void tailSetWithNullFromElementThrowsNPE() {
        SealableNavigableSet<Integer> localSet = buildSet(Arrays.asList(1, 2, 3, 4), null);
        assertThrows(NullPointerException.class, () -> localSet.tailSet(null, true), "Calling tailSet with null fromElement must throw NullPointerException");
    }

    @ParameterizedTest(name = "sealed tailSet({0}, {1}) – mutation {2}")
    @MethodSource("sealedBehaviorProvider")
    void tailSetSealedBehavior(Integer fromElement, boolean inclusive, String mutation) {
        SealableNavigableSet<Integer> localSet = buildSet(Arrays.asList(1, 2, 3, 4), null);
        NavigableSet<Integer> tail = localSet.tailSet(fromElement, inclusive);
        // activate sealed mode
        sealedState = true;
        switch(mutation) {
            case "add" ->
                assertThrows(IllegalStateException.class, () -> tail.add(99), "Adding to a sealed tailSet must throw IllegalStateException");
            case "remove" ->
                assertThrows(IllegalStateException.class, () -> tail.remove(fromElement), "Removing from a sealed tailSet must throw IllegalStateException");
            default ->
                fail("Unsupported mutation type: " + mutation);
        }
    }

    @ParameterizedTest(name = "descending comparator tailSet({0}, {1}) → {2}")
    @MethodSource("comparatorHandlingProvider")
    void tailSetWithDescendingComparator(Integer fromElement, boolean inclusive, String expectedString) {
        Comparator<Integer> descending = Comparator.reverseOrder();
        SealableNavigableSet<Integer> localSet = buildSet(Arrays.asList(1, 2, 3, 4), descending);
        NavigableSet<Integer> tail = localSet.tailSet(fromElement, inclusive);
        List<Integer> expected = parseExpected(expectedString);
        assertEquals(expected, new ArrayList<>(tail), () -> "With descending comparator, tailSet(" + fromElement + ", " + inclusive + ") returned unexpected elements");
    }

    @ParameterizedTest(name = "empty backing set tailSet({0}, {1}) → []")
    @MethodSource("emptySetProvider")
    void tailSetOnEmptyBackingSet(Integer fromElement, boolean inclusive) {
        SealableNavigableSet<Integer> emptySet = buildSet(Collections.emptyList(), null);
        NavigableSet<Integer> tail = emptySet.tailSet(fromElement, inclusive);
        assertTrue(tail.isEmpty(), "Tail set of an empty backing set must be empty");
    }

    @Test
    void tailSetWithIncompatibleTypeThrowsClassCastException() {
        SealableNavigableSet<Integer> localSet = buildSet(Arrays.asList(1, 2, 3, 4), null);
        assertThrows(ClassCastException.class, () -> {
            // raw type usage to force compile‑time bypass
            @SuppressWarnings("unchecked")
            NavigableSet raw = localSet;
            raw.tailSet("a", true);
        }, "Calling tailSet with an element of an incompatible type must throw ClassCastException");
    }

    @Test
    void tailSetViolatingLowerBoundThrowsIllegalArgumentException() {
        // base set [1..10]
        NavigableSet<Integer> base = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        // bounded view: [2,3,4] (lower bound = 2 inclusive, upper bound = 5 exclusive)
        NavigableSet<Integer> bounded = base.subSet(2, true, 5, false);
        SealableNavigableSet<Integer> sealedBounded = new SealableNavigableSet<>(bounded, sealedSupplier);
        // request tailSet with fromElement = 1 which is below the lower bound of the view
        assertThrows(IllegalArgumentException.class, () -> sealedBounded.tailSet(1, true), "Calling tailSet with a fromElement below the view's lower bound must throw IllegalArgumentException");
    }
}
