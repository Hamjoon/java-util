package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class SealableList_indexOf_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        // Use the constructor that takes a backing collection and a sealed supplier
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
        // include a null element for null‑handling test
        list.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        list = null;
    }

    /**
     * Helper to create a SealableList with a given backing collection.
     */
    private <T> SealableList<T> createSealableList(List<T> backing) {
        return new SealableList<>(backing, sealedSupplier);
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// element 1 at index 0
        Arguments.of(1, 0), // element 3 at index 2 (middle)
        Arguments.of(3, 2), // element 5 at last index
        Arguments.of(5, 4));
    }

    @Test
    void testIndexOfExistingElement() {
        assertEquals(0, list.indexOf(10), "First element should be at index 0");
        assertEquals(1, list.indexOf(20), "Second element should be at index 1");
        assertEquals(2, list.indexOf(30), "Third element should be at index 2");
    }

    @Test
    void testIndexOfNonExistingElement() {
        assertEquals(-1, list.indexOf(99), "Element not present should return -1");
        assertEquals(-1, list.indexOf(0), "Element not present should return -1");
    }

    @Test
    void testIndexOfNullElement() {
        // The list contains a single null at index 3
        assertEquals(3, list.indexOf(null), "Null element should be found at its index");
    }

    @Test
    void testIndexOfWhenSealed() {
        // Even when sealed (immutable), read‑only operations like indexOf must still work
        sealedState = true;
        assertEquals(1, list.indexOf(20), "Index lookup should succeed while sealed");
        assertEquals(-1, list.indexOf(999), "Non‑existent lookup should still return -1 while sealed");
    }

    @Test
    void testIndexOfWithCustomBackingList() {
        // Verify that the constructor which receives an existing List works correctly
        List<Integer> backing = new ArrayList<>();
        backing.add(5);
        backing.add(15);
        backing.add(25);
        SealableList<Integer> custom = new SealableList<>(backing, sealedSupplier);
        assertEquals(0, custom.indexOf(5));
        assertEquals(1, custom.indexOf(15));
        assertEquals(2, custom.indexOf(25));
        assertEquals(-1, custom.indexOf(100));
    }

    @Test
    void testIndexOfAfterModifications() {
        // Modify the list and ensure indexOf reflects the changes
        list.add(40);
        assertEquals(4, list.indexOf(40));
        list.remove(Integer.valueOf(20));
        // After removal, 30 shifts to index 1
        assertEquals(1, list.indexOf(30));
        // 20 is no longer present
        assertEquals(-1, list.indexOf(20));
    }

    @ParameterizedTest(name = "{index} => element={0}, expectedIndex={1}")
    @MethodSource("basicFunctionalityProvider")
    @DisplayName("SealableList.indexOf(Object) - basic functionality")
    void testBasicFunctionality(Object element, int expectedIndex) {
        // List = [1, 2, 3, 4, 5]
        SealableList<Integer> custom = createSealableList(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(expectedIndex, custom.indexOf(element), "indexOf should return correct index for element " + element);
    }

    @Test
    @DisplayName("SealableList.indexOf(Object) - empty list")
    void testIndexOfEmptyList() {
        SealableList<Integer> empty = createSealableList(new ArrayList<>());
        assertEquals(-1, empty.indexOf(42), "Any search on an empty list must return -1");
    }

    @Test
    @DisplayName("SealableList.indexOf(Object) - element not present")
    void testIndexOfElementNotPresent() {
        SealableList<Integer> custom = createSealableList(new ArrayList<>(Arrays.asList(1, 2, 3)));
        assertEquals(-1, custom.indexOf(99), "Searching for a missing element should return -1");
    }

    @Test
    @DisplayName("SealableList.indexOf(Object) - null element present")
    void testIndexOfNullPresent() {
        // Underlying list = ["a", null, "b"]
        SealableList<String> custom = createSealableList(new ArrayList<>(Arrays.asList("a", null, "b")));
        assertEquals(1, custom.indexOf(null), "First occurrence of null should be at index 1");
    }

    @Test
    @DisplayName("SealableList.indexOf(Object) - null element absent")
    void testIndexOfNullAbsent() {
        // Underlying list = ["x", "y"]
        SealableList<String> custom = createSealableList(new ArrayList<>(Arrays.asList("x", "y")));
        assertEquals(-1, custom.indexOf(null), "Searching for null when absent must return -1");
    }

    @Test
    @DisplayName("SealableList.indexOf(Object) - duplicate elements")
    void testIndexOfDuplicateElements() {
        // Underlying list = ["dup", "dup", "x"]
        SealableList<String> custom = createSealableList(new ArrayList<>(Arrays.asList("dup", "dup", "x")));
        assertEquals(0, custom.indexOf("dup"), "indexOf must return the first occurrence index (0)");
    }

    @Test
    @DisplayName("SealableList.indexOf(Object) - different type")
    void testIndexOfDifferentType() {
        // Underlying list holds Integers [1,2,3]
        SealableList<Integer> custom = createSealableList(new ArrayList<>(Arrays.asList(1, 2, 3)));
        // Search with a String "123" – should not match
        assertEquals(-1, custom.indexOf("123"), "Type‑mismatched search must return -1");
    }

    @Test
    @DisplayName("SealableList.indexOf(Object) - first element")
    void testIndexOfFirstElement() {
        // Underlying list = ["first", "mid", "end"]
        SealableList<String> custom = createSealableList(new ArrayList<>(Arrays.asList("first", "mid", "end")));
        assertEquals(0, custom.indexOf("first"), "First element should be at index 0");
    }

    @Test
    @DisplayName("SealableList.indexOf(Object) - last element")
    void testIndexOfLastElement() {
        // Underlying list = ["a", "b", "c", "last"]
        SealableList<String> custom = createSealableList(new ArrayList<>(Arrays.asList("a", "b", "c", "last")));
        assertEquals(3, custom.indexOf("last"), "Last element should be at index 3");
    }
}
