package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_addAll_2_Test {

    /**
     * The list under test – a fresh instance for every test method.
     */
    private ConcurrentList<Integer> list;

    /**
     * The list under test – a fresh instance for every test method.
     */
    private ConcurrentList<Integer> intList;

    /**
     * A separate list for String‑specific tests.
     */
    private ConcurrentList<String> stringList;

    @BeforeEach
    void setupBeforeEach() {
        // Integer list used by most existing tests.
        intList = new ConcurrentList<>();
        // String list used by the new parameterised / null‑element tests.
        stringList = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse between tests.
        intList = null;
        stringList = null;
    }

    /**
     * Supplies normal‑operation test data:
     * <ul>
     *   <li>add at start of empty list</li>
     *   <li>add in the middle of a non‑empty list</li>
     *   <li>add at the end (append)</li>
     * </ul>
     */
    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(// index 0 on empty list
        Arguments.of(0, List.of("a", "b", "c"), true, List.of("a", "b", "c")), // index 1 on list ["a","b","c"] -> after first element
        Arguments.of(1, List.of("x", "y"), true, List.of("a", "x", "y", "b", "c")), // index 5 (size) on the list after previous insertion
        Arguments.of(5, List.of("z"), true, List.of("a", "x", "y", "b", "c", "z")));
    }

    @Test
    void addAllAtStartOnEmptyList() {
        List<Integer> toAdd = Arrays.asList(1, 2, 3);
        assertTrue(intList.addAll(0, toAdd), "addAll should return true when elements are added");
        assertEquals(Arrays.asList(1, 2, 3), intList, "Elements must be inserted at the start of an empty list");
    }

    @Test
    void addAllAtStartOnNonEmptyList() {
        intList.addAll(Arrays.asList(10, 20, 30));
        List<Integer> toAdd = Arrays.asList(1, 2, 3);
        assertTrue(intList.addAll(0, toAdd));
        assertEquals(Arrays.asList(1, 2, 3, 10, 20, 30), intList);
    }

    @Test
    void addAllInMiddle() {
        intList.addAll(Arrays.asList(0, 1, 4, 5));
        List<Integer> toAdd = Arrays.asList(2, 3);
        // Insert after the second element (index 2)
        assertTrue(intList.addAll(2, toAdd));
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), intList);
    }

    @Test
    void addAllAtEnd() {
        intList.addAll(Arrays.asList(0, 1, 2));
        List<Integer> toAdd = Arrays.asList(3, 4);
        // Adding at index equal to current size appends the collection
        assertTrue(intList.addAll(3, toAdd));
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), intList);
    }

    @Test
    void addAllWithEmptyCollectionDoesNotChangeList() {
        intList.addAll(Arrays.asList(5, 6));
        assertFalse(intList.addAll(1, Collections.emptyList()), "addAll should return false when collection is empty");
        assertEquals(Arrays.asList(5, 6), intList, "List must remain unchanged when adding an empty collection");
    }

    @Test
    void addAllWithNullCollectionThrowsNPE() {
        assertThrows(NullPointerException.class, () -> intList.addAll(0, null), "addAll should throw NullPointerException when the collection argument is null");
    }

    @Test
    void addAllWithNegativeIndexThrowsIndexOutOfBoundsException() {
        List<Integer> toAdd = Arrays.asList(1, 2);
        assertThrows(IndexOutOfBoundsException.class, () -> intList.addAll(-1, toAdd));
    }

    @Test
    void addAllWithIndexGreaterThanSizeThrowsIndexOutOfBoundsException() {
        intList.addAll(Arrays.asList(1, 2));
        List<Integer> toAdd = Arrays.asList(3, 4);
        // Current size is 2; index 3 is out of bounds.
        assertThrows(IndexOutOfBoundsException.class, () -> intList.addAll(3, toAdd));
    }

    @Test
    void addAllIsThreadSafeWhenMultipleThreadsInsertAtDifferentPositions() throws InterruptedException {
        // Populate the list with a known base.
        intList.addAll(Arrays.asList(0, 0, 0, 0, 0));
        // Two threads will insert distinct ranges at different indexes.
        Thread t1 = new Thread(() -> intList.addAll(0, Arrays.asList(1, 2, 3)));
        Thread t2 = new Thread(() -> intList.addAll(5, Arrays.asList(4, 5, 6)));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        // The exact order of the two insertions is nondeterministic,
        // but the final size and content set must be as expected.
        assertEquals(11, intList.size(), "Final size after concurrent insertions should be 11");
        // Verify that all inserted elements are present.
        for (int i = 1; i <= 6; i++) {
            assertTrue(intList.contains(i), "List must contain inserted element " + i);
        }
        // Verify that the original zeros are still present.
        long zeroCount = intList.stream().filter(i -> i == 0).count();
        assertEquals(5, zeroCount, "Original elements must remain unchanged");
    }

    @ParameterizedTest(name = "[{index}] addAll at index {0} with {1}")
    @MethodSource("normalOperationProvider")
    void addAllNormalOperations(int index, List<String> toAdd, boolean expectedResult, List<String> expectedList) {
        // Build the initial state step‑by‑step so that the three cases run
        // sequentially on a fresh list.
        stringList.clear();
        // First case: empty list, index 0
        if (index == 0 && expectedList.size() == toAdd.size()) {
            assertTrue(stringList.addAll(index, toAdd));
            assertEquals(expectedList, stringList);
            return;
        }
        // For the remaining cases we need the list to contain the previous
        // successful insertions.
        stringList.addAll(Arrays.asList("a", "b", "c"));
        if (index == 1) {
            assertTrue(stringList.addAll(index, toAdd));
            assertEquals(expectedList, stringList);
            return;
        }
        // Third case: after the previous two insertions the list size is 5.
        stringList.addAll(Arrays.asList("a", "x", "y", "b", "c"));
        assertTrue(stringList.addAll(index, toAdd));
        assertEquals(expectedList, stringList);
    }

    @Test
    void addAllWithNullElements() {
        // Collection contains a null and a valid element.
        List<String> collection = Arrays.asList(null, "valid");
        assertTrue(stringList.addAll(0, collection), "addAll should return true when collection contains nulls");
        assertEquals(Arrays.asList(null, "valid"), stringList, "List must preserve null elements in the order they appear");
    }

    @Test
    void addAllWithIncompatibleElementTypeThrowsClassCastException() {
        // List is typed to Integer, but we try to add a collection of Strings.
        ConcurrentList<Integer> intTypedList = new ConcurrentList<>();
        List<String> badCollection = List.of("not-an-integer");
        // The generic type is erased at runtime, so the underlying ArrayList will
        // attempt to store a String where an Integer is expected, causing a
        // ClassCastException when the list is later accessed as Integer.
        assertThrows(ClassCastException.class, () -> intTypedList.addAll(0, (Collection) badCollection));
    }

    @Test
    void addAllAfterPreviousNormalOperationsRemainsConsistent() {
        // Re‑create the scenario described in the JSON “concurrent modification safety” group.
        // 1. Populate with ["a","x","y","b","c","z"]
        stringList.addAll(Arrays.asList("a", "x", "y", "b", "c", "z"));
        // 2. Add a new collection at the beginning.
        List<String> newElements = Arrays.asList("a", "b");
        assertTrue(stringList.addAll(0, newElements));
        // Expected final list:
        List<String> expected = Arrays.asList("a", "b", "a", "x", "y", "b", "c", "z");
        assertEquals(expected, stringList);
    }
}
