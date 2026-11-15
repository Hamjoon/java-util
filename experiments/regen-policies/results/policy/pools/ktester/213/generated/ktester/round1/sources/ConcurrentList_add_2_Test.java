package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_add_2_Test {

    private List<Integer> list;

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor (stand‑alone mode) for a fresh list before each test
        list = new ConcurrentList<>();
        // Populate with a known sequence to make index calculations easy
        list.addAll(Arrays.asList(1, 3, 4));
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; not strictly required but keeps the pattern consistent
        list = null;
    }

    /**
     * Test cases that verify index bounds, size changes and null handling.
     *
     * <p>Each {@link Arguments} entry contains:
     * <ul>
     *   <li>initial elements (may be empty)</li>
     *   <li>index to add at</li>
     *   <li>element to insert (null allowed)</li>
     *   <li>expected exception class (or {@code null} if no exception)</li>
     *   <li>description – used as the display name of the parameterised test</li>
     * </ul>
     */
    static Stream<Arguments> provideIndexBoundsAndNullCases() {
        return Stream.of(// Insert into an empty list at index 0 (valid lower bound).
        Arguments.of(Collections.emptyList(), 0, "first", null, "Insert into empty list at index 0"), // Insert at the end of a list (valid upper bound = size).
        Arguments.of(Collections.singletonList("first"), 1, "second", null, "Insert at end of list (size)"), // Negative index should cause IndexOutOfBoundsException.
        Arguments.of(Collections.emptyList(), -1, "neg", IndexOutOfBoundsException.class, "Negative index throws IOBE"), // Index greater than current size should cause IndexOutOfBoundsException.
        Arguments.of(Collections.emptyList(), 3, "out", IndexOutOfBoundsException.class, "Index > size throws IOBE"), // Verify that null values are allowed and correctly stored.
        Arguments.of(Collections.emptyList(), 0, null, null, "Insert null element at index 0"));
    }

    @Test
    void addAtStart_ShouldShiftExistingElements() {
        // Act
        list.add(0, 0);
        // Assert
        assertEquals(Arrays.asList(0, 1, 3, 4), list, "Adding at index 0 should prepend the element and shift others right");
    }

    @Test
    void addAtMiddle_ShouldInsertAtCorrectPosition() {
        // Act
        // current list: [1,3,4] -> insert 2 at index 2
        list.add(2, 2);
        // Assert
        assertEquals(Arrays.asList(1, 3, 2, 4), list, "Adding at a middle index should insert without overwriting existing elements");
    }

    @Test
    void addAtEnd_ShouldAppendElement() {
        // Act
        // size is 3 before insertion
        list.add(3, 5);
        // Assert
        assertEquals(Arrays.asList(1, 3, 4, 5), list, "Adding at the list size should behave like append");
    }

    @Test
    void addWithInvalidNegativeIndex_ShouldThrowIndexOutOfBoundsException() {
        // Act & Assert
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 99), "Negative index must result in IndexOutOfBoundsException");
    }

    @Test
    void addWithIndexGreaterThanSize_ShouldThrowIndexOutOfBoundsException() {
        // Act & Assert
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(5, 99), "Index greater than current size must result in IndexOutOfBoundsException");
    }

    @Test
    void addNullElement_ShouldAllowIfUnderlyingListAllowsNulls() {
        // Arrange
        // wrapper mode with ArrayList (allows null)
        List<Integer> nullableList = new ConcurrentList<>(new ArrayList<>());
        // Act
        nullableList.add(0, null);
        // Assert
        assertEquals(1, nullableList.size());
        assertNull(nullableList.get(0), "Null element should be stored when underlying list permits it");
    }

    @Test
    void concurrentAddAtSpecificIndices_ShouldMaintainConsistency() throws InterruptedException {
        // This test demonstrates that add(int, E) remains thread‑safe.
        List<Integer> concurrentList = new ConcurrentList<>();
        int threadCount = 10;
        int addsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < addsPerThread; i++) {
                    // Insert at the end each time; index is current size (thread‑safe via writeOperation)
                    concurrentList.add(concurrentList.size(), threadId * addsPerThread + i);
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(threadCount * addsPerThread, concurrentList.size(), "All elements added concurrently should be present");
        // Verify that each expected value is present
        for (int i = 0; i < threadCount * addsPerThread; i++) {
            assertTrue(concurrentList.contains(i), "List should contain element " + i);
        }
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("provideIndexBoundsAndNullCases")
    void addIndexBoundsAndNullHandling(List<String> initialElements, int index, String element, Class<? extends Throwable> expectedException, String description) {
        // Arrange: create a fresh ConcurrentList<String> and populate it with the supplied initial state
        ConcurrentList<String> testList = new ConcurrentList<>();
        testList.addAll(initialElements);
        // Act & Assert
        if (expectedException != null) {
            assertThrows(expectedException, () -> testList.add(index, element), description + " – expected exception");
        } else {
            testList.add(index, element);
            // Verify size increment
            assertEquals(initialElements.size() + 1, testList.size(), description + " – size should increase by 1");
            // Verify element at the requested index
            assertEquals(element, testList.get(index), description + " – element should be stored at the given index");
        }
    }

    /**
     * Verify that inserting into the middle of a list shifts subsequent elements.
     */
    @Test
    void addInMiddle_ShiftsElementsRight() {
        // Arrange: a list with three known elements
        ConcurrentList<String> middleList = new ConcurrentList<>();
        middleList.addAll(Arrays.asList("a", "b", "c"));
        // Act: insert "X" at index 2 (between "b" and "c")
        middleList.add(2, "X");
        // Assert
        List<String> expected = Arrays.asList("a", "b", "X", "c");
        assertEquals(expected, middleList, "Inserting at index 2 should shift elements at and after that index to the right");
        assertEquals(4, middleList.size(), "Size should increase from 3 to 4");
        assertEquals("X", middleList.get(2), "Element at index 2 should be the newly inserted value");
        assertEquals("c", middleList.get(3), "Element previously at index 2 should now be at index 3");
    }

    /**
     * When the internal delegate list is {@code null} (wrapper mode with a null argument),
     * any mutating operation should propagate a {@link NullPointerException}.
     */
    @Test
    void addWithNullUnderlyingList_PropagatesNullPointerException() {
        // Arrange: construct a ConcurrentList that wraps a null list
        ConcurrentList<String> nullWrapped = new ConcurrentList<>(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> nullWrapped.add(0, "any"), "Calling add on a ConcurrentList with a null internal list should throw NPE");
    }
}
