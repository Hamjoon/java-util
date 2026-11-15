package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.DeepEquals.deepEquals;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_addAll_2_Test {

    private ConcurrentList<Integer> list;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        list = null;
    }

    /**
     * Parameter source for the “valid inputs” group.
     *
     * Each {@link Arguments} contains:
     *   - initial list content (may be empty)
     *   - index at which to insert
     *   - collection to insert
     *   - expected boolean result from addAll
     *   - expected final list content
     */
    private static Stream<Arguments> validInputProvider() {
        return Stream.of(// Insert into an empty list at index 0
        Arguments.of(Collections.emptyList(), 0, Arrays.asList(1, 2, 3), true, Arrays.asList(1, 2, 3)), // Insert in the middle of a pre‑populated list
        Arguments.of(Arrays.asList(1, 2, 3), 2, Arrays.asList(8, 9), true, Arrays.asList(1, 2, 8, 9, 3)), // Insert at the end (index == size)
        Arguments.of(Arrays.asList(1, 2, 8, 9, 3), 5, Arrays.asList(10), true, Arrays.asList(1, 2, 8, 9, 3, 10)), // Insert an empty collection – should return false and keep list unchanged
        Arguments.of(Arrays.asList(1, 2, 8, 9, 3, 10), 3, Collections.emptyList(), false, Arrays.asList(1, 2, 8, 9, 3, 10)));
    }

    /**
     * Parameter source for edge‑case/error handling scenarios.
     */
    private static Stream<Arguments> errorCaseProvider() {
        return Stream.of(// Negative index
        Arguments.of(-1, Arrays.asList(4, 5), IndexOutOfBoundsException.class), // Index greater than size
        Arguments.of(100, Arrays.asList(6), IndexOutOfBoundsException.class), // Null collection
        Arguments.of(0, null, NullPointerException.class), // Collection containing nulls (should succeed)
        Arguments.of(0, Arrays.asList((Integer) null, (Integer) null), null));
    }

    @Test
    void addAllAtStart() {
        // Arrange
        list.addAll(Arrays.asList(1, 5));
        // Act
        boolean changed = list.addAll(0, Arrays.asList(-1, 0));
        // Assert
        assertTrue(changed, "addAll should report that the list was modified");
        deepEquals(Arrays.asList(-1, 0, 1, 5), list);
    }

    @Test
    void addAllAtMiddle() {
        // Arrange
        list.addAll(Arrays.asList(-1, 0, 1, 5));
        // Act
        boolean changed = list.addAll(2, Arrays.asList(2, 3, 4));
        // Assert
        assertTrue(changed, "addAll should report that the list was modified");
        deepEquals(Arrays.asList(-1, 0, 2, 3, 4, 1, 5), list);
    }

    @Test
    void addAllAtEnd() {
        // Arrange
        list.addAll(Arrays.asList(-1, 0, 2, 3, 4, 1, 5));
        // Act
        boolean changed = list.addAll(7, Arrays.asList(6, 7));
        // Assert
        assertTrue(changed, "addAll should report that the list was modified");
        deepEquals(Arrays.asList(-1, 0, 2, 3, 4, 1, 5, 6, 7), list);
    }

    @Test
    void addAllWithEmptyCollectionDoesNotModifyList() {
        // Arrange
        list.addAll(Arrays.asList(10, 20, 30));
        // Act
        boolean changed = list.addAll(1, List.of());
        // Assert
        assertFalse(changed, "addAll should return false when collection is empty");
        deepEquals(Arrays.asList(10, 20, 30), list);
    }

    @Test
    void addAllWithIndexOutOfBoundsThrows() {
        // Arrange
        list.addAll(Arrays.asList(1, 2, 3));
        // Act & Assert
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(5, Arrays.asList(9, 9)), "addAll should throw IndexOutOfBoundsException when index > size");
    }

    @Test
    void addAllWithNegativeIndexThrows() {
        // Arrange
        list.addAll(Arrays.asList(1, 2, 3));
        // Act & Assert
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, Arrays.asList(0)), "addAll should throw IndexOutOfBoundsException for negative index");
    }

    @Test
    void addAllWithNullCollectionThrows() {
        // Arrange
        list.addAll(Arrays.asList(1, 2, 3));
        // Act & Assert
        assertThrows(NullPointerException.class, () -> list.addAll(1, null), "addAll should throw NullPointerException when collection is null");
    }

    @ParameterizedTest(name = "[{index}] valid addAll – insert {2} at index {1}")
    @MethodSource("validInputProvider")
    void addAllValidInputs(List<Integer> initial, int index, Collection<Integer> toAdd, boolean expectedChanged, List<Integer> expectedResult) {
        // Arrange – populate the list with the supplied initial content
        list.addAll(initial);
        // Act
        boolean changed = list.addAll(index, toAdd);
        // Assert
        assertEquals(expectedChanged, changed, () -> "Expected addAll to return " + expectedChanged);
        deepEquals(expectedResult, list);
    }

    @ParameterizedTest(name = "[{index}] addAll error case – index={0}, collection={1}")
    @MethodSource("errorCaseProvider")
    void addAllErrorCases(int index, Collection<Integer> coll, Class<? extends Throwable> expectedException) {
        // Arrange – start with a non‑empty list to make index checks meaningful
        list.addAll(Arrays.asList(1, 2, 3));
        if (expectedException != null) {
            // Expect an exception
            assertThrows(expectedException, () -> list.addAll(index, coll), () -> "Expected " + expectedException.getSimpleName() + " for index=" + index);
        } else {
            // No exception expected – verify normal behavior (null elements are allowed)
            boolean changed = list.addAll(index, coll);
            assertTrue(changed, "addAll should report modification when inserting null elements");
            // The list should now contain the nulls at the front
            deepEquals(Arrays.asList(null, null, 1, 2, 3), list);
        }
    }

    @Test
    void addAllWhenUnderlyingListIsUnmodifiableThrowsUnsupportedOperationException() {
        // Arrange – create a backing list that deliberately throws on modification
        List<Integer> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(unmodifiable);
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> wrapped.addAll(0, Arrays.asList(1, 2)), "addAll should propagate UnsupportedOperationException from the underlying list");
    }

    @Test
    void addAllWithDuplicateElementsPreservesOrderAndDuplicates() {
        // Arrange
        list.addAll(Arrays.asList(1, 2, 8, 9, 3, 10));
        // Act
        boolean changed = list.addAll(0, Arrays.asList(1, 1, 1));
        // Assert
        assertTrue(changed);
        deepEquals(Arrays.asList(1, 1, 1, 1, 2, 8, 9, 3, 10), list);
    }

    @Test
    void addAllWithSetBackedCollectionRespectsIterationOrder() {
        // Arrange – use a LinkedHashSet to guarantee deterministic iteration order
        Set<Integer> set = new LinkedHashSet<>(Arrays.asList(7, 8));
        list.addAll(Arrays.asList(1, 2, 8, 9, 3, 10));
        // Act
        boolean changed = list.addAll(3, set);
        // Assert
        assertTrue(changed);
        deepEquals(Arrays.asList(1, 2, 8, 7, 8, 9, 3, 10), list);
    }

    @Test
    void addAllWithMixedObjectTypesViaRawListDemonstratesErasureHandling() {
        // This test uses a raw ConcurrentList to bypass generic type checking.
        @SuppressWarnings("unchecked")
        ConcurrentList rawList = new ConcurrentList();
        rawList.addAll(Arrays.asList(1, 2, 8, 9, 3, 10));
        // Act – insert a collection of Strings
        boolean changed = rawList.addAll(1, Arrays.asList("a", "b", "c"));
        // Assert
        assertTrue(changed);
        deepEquals(Arrays.asList(1, "a", "b", "c", 2, 8, 9, 3, 10), rawList);
    }
}
