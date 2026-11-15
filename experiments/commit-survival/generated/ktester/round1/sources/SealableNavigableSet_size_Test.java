package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_size_Test {

    /**
     * The set under test
     */
    private SealableNavigableSet<Integer> set;

    /**
     * Simple mutable flag used by the supplier to indicate sealed/unsealed state
     */
    private volatile boolean sealedState = false;

    /**
     * Supplier that the SealableNavigableSet uses to query its sealed state
     */
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for every test, starts un‑sealed
        set = new SealableNavigableSet<>(sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        set = null;
        sealedState = false;
    }

    private static Stream<Arguments> basicSizeProvider() {
        return Stream.of(// Empty set → size 0
        Arguments.of("When the wrapped NavigableSet is empty, size() should return 0.", Collections.emptyList(), 0), // Three elements → size 3
        Arguments.of("When the wrapped NavigableSet contains three elements (\"a\", \"b\", \"c\"), size() should return 3.", Arrays.asList("a", "b", "c"), 3));
    }

    private static Stream<Arguments> additionSizeProvider() {
        return Stream.of(Arguments.of("Start with an empty NavigableSet, add five distinct elements, then size() should return 5.", Arrays.asList(11, 22, 33, 44, 55), 5));
    }

    @Test
    void sizeOnEmptySet() {
        assertEquals(0, set.size(), "A newly created set should have size 0");
    }

    @Test
    void sizeAfterAddingElements() {
        assertTrue(set.add(10));
        assertTrue(set.add(20));
        assertTrue(set.add(30));
        assertEquals(3, set.size(), "Size should reflect the number of added elements");
    }

    @Test
    void sizeAfterRemovingElements() {
        set.add(1);
        set.add(2);
        set.add(3);
        assertEquals(3, set.size());
        assertTrue(set.remove(2));
        assertEquals(2, set.size(), "Size should decrease after a successful removal");
        // Removing a non‑existent element must not change the size
        assertFalse(set.remove(99));
        assertEquals(2, set.size());
    }

    @Test
    void sizeWhenSealed() {
        set.add(5);
        set.add(15);
        assertEquals(2, set.size());
        // simulate sealed mode
        sealedState = true;
        // size should still be accessible and correct
        assertEquals(2, set.size(), "Size must be observable even when the set is sealed");
    }

    @Test
    void sizeWithCollectionConstructor() {
        List<Integer> source = Arrays.asList(7, 8, 9, 10);
        SealableNavigableSet<Integer> fromCollection = new SealableNavigableSet<>(source, sealedSupplier);
        assertEquals(source.size(), fromCollection.size(), "Constructor that accepts a collection should initialise the correct size");
    }

    @Test
    void sizeWithSortedSetConstructor() {
        SortedSet<Integer> sorted = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
        SealableNavigableSet<Integer> fromSorted = new SealableNavigableSet<>(sorted, sealedSupplier);
        assertEquals(sorted.size(), fromSorted.size(), "Constructor that accepts a SortedSet should initialise the correct size");
    }

    @Test
    void sizeWithNavigableSetConstructor() {
        NavigableSet<Integer> nav = new TreeSet<>(Arrays.asList(100, 200));
        SealableNavigableSet<Integer> fromNav = new SealableNavigableSet<>(nav, sealedSupplier);
        assertEquals(nav.size(), fromNav.size(), "Constructor that accepts a NavigableSet should initialise the correct size");
    }

    @Test
    void sizeWithComparatorConstructor() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        SealableNavigableSet<Integer> withComparator = new SealableNavigableSet<>(reverse, sealedSupplier);
        withComparator.add(3);
        withComparator.add(1);
        withComparator.add(2);
        assertEquals(3, withComparator.size(), "Size should be correct regardless of the comparator supplied");
    }

    /**
     * Test basic size behavior: empty set and a set pre‑populated with three elements.
     *
     * @param description textual description of the scenario (used for readability)
     * @param initialElements elements to insert before invoking {@code size()}
     * @param expected expected size result
     */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("basicSizeProvider")
    void sizeBasicBehavior(String description, List<String> initialElements, int expected) {
        // Use a String‑typed SealableNavigableSet for these generic cases
        SealableNavigableSet<String> stringSet = new SealableNavigableSet<>(sealedSupplier);
        initialElements.forEach(stringSet::add);
        assertEquals(expected, stringSet.size(), description);
    }

    /**
     * Test size after a sequence of additions.
     *
     * @param description textual description of the scenario
     * @param elementsToAdd elements to add before checking size
     * @param expected expected size after additions
     */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("additionSizeProvider")
    void sizeAfterAdditions(String description, List<Integer> elementsToAdd, int expected) {
        elementsToAdd.forEach(set::add);
        assertEquals(expected, set.size(), description);
    }
}
