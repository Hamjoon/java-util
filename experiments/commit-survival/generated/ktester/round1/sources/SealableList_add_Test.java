package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Supplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// kept from template; no Mockito usage but harmless
@ExtendWith(MockitoExtension.class)
class SealableList_add_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    @Mock
    private List<String> mockListReturningFalse;

    // Custom runtime exception for testing propagation
    static class MyListException extends RuntimeException {

        MyListException(String msg) {
            super(msg);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // use the constructor that accepts an existing collection to verify that path as well
        List<Integer> backing = new ArrayList<>();
        backing.add(1);
        backing.add(2);
        backing.add(3);
        list = new SealableList<>(backing, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references for GC; not strictly required
        list = null;
    }

    private static Stream<Arguments> provideBasicAddCases() {
        return Stream.of(// regular non‑null element
        Arguments.of("alpha", List.of(1, 2, 3, "alpha")), // null element (ArrayList permits null)
        Arguments.of(null, List.of(1, 2, 3, null)), // empty string
        Arguments.of("", List.of(1, 2, 3, "")), // mutable object (StringBuilder)
        Arguments.of(new StringBuilder("test"), List.of(1, 2, 3, new StringBuilder("test"))), // duplicate value
        Arguments.of(2, List.of(1, 2, 3, 2)), // generic Object addition (list is Integer, but we will use a raw SealableList later)
        // This case is handled in a separate test because of type erasure.
        // Adding after many elements – size increment check
        Arguments.of("delta", List.of(1, 2, 3, "delta")));
    }

    @Test
    void addWhenUnsealed_appendsElementAndIncreasesSize() {
        assertFalse(list.isEmpty());
        assertEquals(3, list.size());
        boolean result = list.add(4);
        assertTrue(result, "add should return true when element is added");
        assertTrue(list.contains(4), "list should contain the newly added element");
        assertEquals(4, list.size(), "size should be incremented after add");
    }

    @Test
    void addWhenSealed_throwsUnsupportedOperationException() {
        // seal the list
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.add(5), "add should throw when the list is sealed");
    }

    @Test
    void addMultipleElements_sequentialAddsMaintainOrder() {
        list.add(4);
        list.add(5);
        list.add(6);
        List<Integer> expected = List.of(1, 2, 3, 4, 5, 6);
        assertEquals(expected, list, "list content should reflect the order of added elements");
    }

    @Test
    void addNullElement_allowedWhenUnsealed() {
        // The underlying list permits nulls (ConcurrentList behaves like ArrayList)
        boolean result = list.add(null);
        assertTrue(result);
        assertEquals(4, list.size());
        assertTrue(list.contains(null));
    }

    @Test
    void addNullElement_throwsWhenSealed() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.add(null));
    }

    /**
     * Adds a single element to an unsealed list and verifies the result.
     *
     * @param element   element to add (may be null)
     * @param expected  expected list content after the addition
     */
    @ParameterizedTest(name = "[{index}] add({0}) → expected list {1}")
    @MethodSource("provideBasicAddCases")
    void addWhenUnsealed_basicBehaviour(Object element, List<Object> expected) {
        // Act
        boolean result = list.add(element);
        // Assert
        assertTrue(result, "add should return true for successful insertion");
        assertEquals(expected, list, "list content should match expected after add");
    }

    @Test
    void addNullWhenSealed_throwsUnsupportedOperationException() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.add(null), "add(null) should also throw when the list is sealed");
    }

    @Test
    void addWhenUnderlyingListReturnsFalse_propagatesResult() {
        // Arrange: mock list that always returns false on add
        when(mockListReturningFalse.add(any())).thenReturn(false);
        SealableList<String> sealable = new SealableList<>(mockListReturningFalse, () -> false);
        // Act
        boolean result = sealable.add("any");
        // Assert
        assertFalse(result, "add should propagate false from the delegate list");
        // Verify that the element is not present (delegate didn't store it)
        assertFalse(sealable.contains("any"));
    }

    @Test
    void addWhenUnderlyingListIsUnmodifiable_propagatesUnsupportedOperationException() {
        List<Integer> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        SealableList<Integer> sealable = new SealableList<>(unmodifiable, () -> false);
        assertThrows(UnsupportedOperationException.class, () -> sealable.add(1), "Exception from delegate should propagate unchanged");
    }

    @Test
    void addNullWhenUnderlyingListDisallowsNull_propagatesNullPointerException() {
        List<Integer> nonNullList = new AbstractList<>() {

            private final List<Integer> inner = new ArrayList<>();

            @Override
            public Integer get(int index) {
                return inner.get(index);
            }

            @Override
            public int size() {
                return inner.size();
            }

            @Override
            public boolean add(Integer e) {
                if (e == null) {
                    throw new NullPointerException("null not allowed");
                }
                return inner.add(e);
            }
        };
        SealableList<Integer> sealable = new SealableList<>(nonNullList, () -> false);
        assertThrows(NullPointerException.class, () -> sealable.add(null), "NullPointerException from delegate must propagate");
    }

    @Test
    void addWhenUnderlyingFixedSizeListThrowsIllegalArgumentException() {
        // FixedSizeList with capacity 0 – any add throws IllegalArgumentException
        List<Integer> fixedSizeZero = new AbstractList<>() {

            @Override
            public Integer get(int index) {
                throw new IndexOutOfBoundsException();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean add(Integer e) {
                throw new IllegalArgumentException("capacity exceeded");
            }
        };
        SealableList<Integer> sealable = new SealableList<>(fixedSizeZero, () -> false);
        assertThrows(IllegalArgumentException.class, () -> sealable.add(10), "IllegalArgumentException from delegate must propagate");
    }

    @Test
    void addWhenUnderlyingListHasWrongGenericType_throwsClassCastException() {
        // Underlying list only accepts Integer, but we expose it as SealableList<Object>
        List<Integer> intList = new ArrayList<>();
        SealableList<Object> sealable = new SealableList<>(intList, () -> false);
        assertThrows(ClassCastException.class, () -> sealable.add("string"), "ClassCastException from delegate must propagate");
    }

    @Test
    void addWhenUnderlyingListThrowsCustomException_propagatesIt() {
        List<String> customExceptionList = new AbstractList<>() {

            @Override
            public String get(int index) {
                throw new IndexOutOfBoundsException();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean add(String e) {
                throw new MyListException("deliberate failure");
            }
        };
        SealableList<String> sealable = new SealableList<>(customExceptionList, () -> false);
        MyListException ex = assertThrows(MyListException.class, () -> sealable.add("trigger"), "Custom exception must propagate unchanged");
        assertEquals("deliberate failure", ex.getMessage());
    }

    @Test
    void addDuplicateValue_allowedAndAppended() {
        // duplicate of existing element
        list.add(2);
        List<Integer> expected = List.of(1, 2, 3, 2);
        assertEquals(expected, list);
    }

    @Test
    void addObjectToRawSealableList_genericObjectHandling() {
        // Create a raw SealableList that can hold any Object
        List<Object> backing = new ArrayList<>();
        backing.add("first");
        SealableList<Object> rawList = new SealableList<>(backing, () -> false);
        rawList.add(Integer.valueOf(42));
        rawList.add(new StringBuilder("test"));
        List<Object> expected = List.of("first", 42, new StringBuilder("test"));
        assertEquals(expected, rawList);
    }

    @Test
    void addAfterSealPersistsStateAcrossCalls() {
        // first seal
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.add("alpha"));
        // Even after catching the exception, the list stays sealed
        assertThrows(UnsupportedOperationException.class, () -> list.add("beta"));
    }

    @Test
    void addWhenUnderlyingListReturnsTrueButListIsSealed_firstThrows() {
        // Verify that sealing takes precedence over a successful delegate call
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.add("any"));
    }
}
