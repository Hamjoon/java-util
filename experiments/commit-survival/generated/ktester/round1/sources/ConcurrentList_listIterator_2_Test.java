package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_listIterator_2_Test {

    private List<Integer> list;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh ConcurrentList before each test
        list = new ConcurrentList<>();
        // Populate with a few elements to make the iterator semantics clearer,
        // even though the method under test will always throw.
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required
        list = null;
    }

    /* --------------------------------------------------------------
     *  Group 1 – basic index values (positive, negative, extreme)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> basicIndexProvider() {
        return Stream.of(Arguments.of(0, "index 0"), Arguments.of(5, "positive index within typical bounds"), Arguments.of(-1, "negative index"), Arguments.of(Integer.MAX_VALUE, "Integer.MAX_VALUE"));
    }

    /* --------------------------------------------------------------
     *  Group 2 – irrespective of list state (empty, various sizes)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> stateIrrelevantProvider() {
        return Stream.of(// Empty list
        Arguments.of(1, "empty list", (Consumer<List<Integer>>) List::clear), // Index equal to current size (size == 3 from @BeforeEach)
        Arguments.of(3, "index equals current size (3)", (Consumer<List<Integer>>) l -> {
        }), // List with five elements, middle index
        Arguments.of(2, "list with five elements, middle index", (Consumer<List<Integer>>) l -> {
            while (l.size() < 5) {
                l.add(l.size() * 10 + 10);
            }
        }), // Index larger than current size
        Arguments.of(10, "index larger than current size", (Consumer<List<Integer>>) l -> {
            while (l.size() < 5) {
                l.add(l.size() * 10 + 10);
            }
        }), // Large negative index
        Arguments.of(-100, "large negative index", (Consumer<List<Integer>>) l -> {
        }), // Integer.MIN_VALUE
        Arguments.of(Integer.MIN_VALUE, "Integer.MIN_VALUE", (Consumer<List<Integer>>) l -> {
        }));
    }

    /* --------------------------------------------------------------
     *  Group 3 – after a series of modifications (add, clear, remove)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> afterModificationsProvider() {
        return Stream.of(// Add several elements, then clear the list
        Arguments.of(0, "after add several elements and clear()", (Consumer<List<Integer>>) l -> {
            for (int i = 0; i < 5; i++) {
                l.add(i * 10);
            }
            l.clear();
        }), // Add five elements, remove three (size becomes 2), then call with index == size
        Arguments.of(2, "after add five and remove three (size==2)", (Consumer<List<Integer>>) l -> {
            for (int i = 0; i < 5; i++) {
                l.add(i * 10);
            }
            l.remove(0);
            l.remove(0);
            l.remove(0);
        }), // Add exactly seven elements, index equals size (7)
        Arguments.of(7, "after adding exactly seven elements", (Consumer<List<Integer>>) l -> {
            l.clear();
            for (int i = 0; i < 7; i++) {
                l.add(i * 10);
            }
        }), // Add a few elements, then use a negative index
        Arguments.of(-50, "after adding a few elements, negative index", (Consumer<List<Integer>>) l -> {
            l.clear();
            l.add(1);
            l.add(2);
            l.add(3);
        }));
    }

    /* --------------------------------------------------------------
     *  Group 4 – arbitrary large positive index
     * -------------------------------------------------------------- */
    private static Stream<Arguments> largePositiveIndexProvider() {
        return Stream.of(Arguments.of(123_456_789, "arbitrarily large positive index"));
    }

    @Test
    void listIterator_withValidIndex_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> list.listIterator(0), "listIterator(0) should throw UnsupportedOperationException");
    }

    @Test
    void listIterator_withMiddleIndex_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> list.listIterator(1), "listIterator(1) should throw UnsupportedOperationException");
    }

    @Test
    void listIterator_withOutOfBoundsIndex_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> list.listIterator(100), "listIterator(100) should throw UnsupportedOperationException even if index is out of bounds");
    }

    @Test
    void listIterator_withNegativeIndex_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> list.listIterator(-1), "listIterator(-1) should throw UnsupportedOperationException for negative index");
    }

    @Test
    void listIterator_usingWrappedListConstructor_throwsUnsupportedOperationException() {
        // Use the constructor that wraps an existing list
        List<Integer> backing = new ArrayList<>();
        backing.add(1);
        backing.add(2);
        List<Integer> wrapped = new ConcurrentList<>(backing);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> wrapped.listIterator(0), "listIterator on a ConcurrentList created with a wrapped list should also throw UnsupportedOperationException");
        assertEquals("listIterator(index) not implemented for ConcurrentList", ex.getMessage(), "Exception message must be the same for wrapped instances");
    }

    @ParameterizedTest(name = "listIterator({0}) – {1}")
    @MethodSource("basicIndexProvider")
    void listIterator_basicIndices_throwsUnsupportedOperationException(int index, String description) {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> list.listIterator(index), () -> "listIterator(" + index + ") should throw UnsupportedOperationException");
        assertEquals("listIterator(index) not implemented for ConcurrentList", ex.getMessage(), "Exception message must match the specification");
    }

    @ParameterizedTest(name = "listIterator({0}) – {1}")
    @MethodSource("stateIrrelevantProvider")
    void listIterator_irrespectiveOfListState_throwsUnsupportedOperationException(int index, String description, Consumer<List<Integer>> listConfigurer) {
        // Apply the specific list configuration for this case
        listConfigurer.accept(list);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> list.listIterator(index), () -> "listIterator(" + index + ") should throw UnsupportedOperationException regardless of list state");
        assertEquals("listIterator(index) not implemented for ConcurrentList", ex.getMessage(), "Exception message must be consistent");
    }

    @ParameterizedTest(name = "listIterator({0}) – {1}")
    @MethodSource("afterModificationsProvider")
    void listIterator_afterModifications_throwsUnsupportedOperationException(int index, String description, Consumer<List<Integer>> listConfigurer) {
        // Prepare the list according to the scenario
        listConfigurer.accept(list);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> list.listIterator(index), () -> "listIterator(" + index + ") should still throw UnsupportedOperationException after modifications");
        assertEquals("listIterator(index) not implemented for ConcurrentList", ex.getMessage(), "Exception message must remain unchanged");
    }

    @ParameterizedTest(name = "listIterator({0}) – {1}")
    @MethodSource("largePositiveIndexProvider")
    void listIterator_largePositiveIndex_throwsUnsupportedOperationException(int index, String description) {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> list.listIterator(index), () -> "listIterator(" + index + ") should throw UnsupportedOperationException even for huge indices");
        assertEquals("listIterator(index) not implemented for ConcurrentList", ex.getMessage(), "Exception message must be consistent for large indices");
    }
}
