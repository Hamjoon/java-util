package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableList_removeAll_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Reset sealed flag and create a fresh SealableList for each test
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // Populate with a known set of elements
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent
        list = null;
    }

    /**
     * Provides test data derived from the JSON specification.
     */
    private static Stream<Arguments> removeAllProvider() {
        return Stream.of(// 1. collection contains one matching (2) and one non‑matching (4)
        Arguments.of(// initial list
        Arrays.asList(1, 2, 3, 4), // collection to remove
        Arrays.asList(2, 4), // expected return value
        true, // expected list after removal
        Arrays.asList(1, 3)), // 2. empty collection – no change
        Arguments.of(Arrays.asList(1, 2, 3, 4), Collections.emptyList(), false, Arrays.asList(1, 2, 3, 4)), // 3. empty list, collection contains non‑present element
        Arguments.of(Collections.emptyList(), Collections.singletonList(1), false, Collections.emptyList()), // 4. null collection – expect NPE
        Arguments.of(Arrays.asList(1, 2, 3, 4), null, NullPointerException.class, null), // 5. sealed list – expect IllegalStateException (as per spec)
        Arguments.of(Arrays.asList(1, 2, 3, 4), Arrays.asList(1, 2), IllegalStateException.class, null), // 6. list with duplicates, remove all occurrences of 1
        Arguments.of(Arrays.asList(1, 1, 2), Collections.singletonList(1), true, Collections.singletonList(2)));
    }

    @Test
    void removeAllRemovesPresentElements() {
        // given
        Collection<Integer> toRemove = Arrays.asList(2, 3);
        // when
        boolean changed = list.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should report a change when elements are removed");
        assertFalse(list.contains(2), "List should no longer contain 2");
        assertFalse(list.contains(3), "List should no longer contain 3");
        assertEquals(2, list.size(), "Size should reflect the removed elements");
        assertTrue(list.containsAll(Arrays.asList(1, 4)), "Remaining elements should stay untouched");
    }

    @Test
    void removeAllReturnsFalseWhenNoElementsMatch() {
        // given
        Collection<Integer> toRemove = Arrays.asList(5, 6, 7);
        // when
        boolean changed = list.removeAll(toRemove);
        // then
        assertFalse(changed, "removeAll should return false when nothing is removed");
        assertEquals(4, list.size(), "Size must remain unchanged");
        assertTrue(list.containsAll(Arrays.asList(1, 2, 3, 4)), "All original elements must still be present");
    }

    @Test
    void removeAllWithEmptyCollectionDoesNothing() {
        // given
        Collection<Integer> empty = Collections.emptyList();
        // when
        boolean changed = list.removeAll(empty);
        // then
        assertFalse(changed, "removeAll on an empty collection should return false");
        assertEquals(4, list.size(), "Size must remain unchanged");
    }

    @Test
    void removeAllThrowsWhenSealed() {
        // given
        // make the list immutable
        sealedState = true;
        Collection<Integer> toRemove = Collections.singleton(1);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> list.removeAll(toRemove), "removeAll should throw UnsupportedOperationException when the list is sealed");
    }

    @Test
    void removeAllThrowsNullPointerExceptionForNullArgument() {
        // given
        Collection<?> nullCollection = null;
        // when / then
        assertThrows(NullPointerException.class, () -> list.removeAll(nullCollection), "removeAll should throw NullPointerException when the argument is null");
    }

    @ParameterizedTest(name = "[{index}] initial={0}, col={1}")
    @MethodSource("removeAllProvider")
    void removeAllParameterized(List<Integer> initial, Collection<Integer> col, Object expectedResult, List<Integer> expectedList) {
        // -------------------------------------------------------
        // Arrange
        // -------------------------------------------------------
        list.clear();
        // populate the list according to the scenario
        initial.forEach(list::add);
        // If the scenario expects a sealed list, set the flag before invoking
        if (expectedResult instanceof Class && ((Class<?>) expectedResult).equals(IllegalStateException.class)) {
            sealedState = true;
        }
        // -------------------------------------------------------
        // Act & Assert
        // -------------------------------------------------------
        if (expectedResult instanceof Class) {
            // Expecting an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expectedResult;
            assertThrows(exClass, () -> list.removeAll(col), () -> "Expected " + exClass.getSimpleName() + " when calling removeAll");
        } else {
            // Normal boolean result
            boolean changed = list.removeAll(col);
            assertEquals(expectedResult, changed, "removeAll should return " + expectedResult);
            // Verify final state of the list
            if (expectedList != null) {
                assertEquals(expectedList, list, "List content after removeAll does not match expected");
            }
        }
    }

    @Test
    void removeAllThrowsWhenSealedExplicit() {
        // given
        sealedState = true;
        Collection<Integer> toRemove = Collections.singleton(1);
        // when / then
        assertThrows(IllegalStateException.class, () -> list.removeAll(toRemove), "removeAll should throw IllegalStateException when the list is sealed");
    }

    @Test
    void removeAllNullArgumentExplicit() {
        // given
        Collection<?> nullCol = null;
        // when / then
        assertThrows(NullPointerException.class, () -> list.removeAll(nullCol), "removeAll should throw NullPointerException for a null collection");
    }
}
