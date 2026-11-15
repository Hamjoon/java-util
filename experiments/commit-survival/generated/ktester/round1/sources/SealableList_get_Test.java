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
class SealableList_get_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // Use the constructor that accepts an existing collection so we can control the initial order
        List<Integer> backing = new ArrayList<>();
        backing.add(10);
        backing.add(20);
        backing.add(30);
        list = new SealableList<>(backing, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        list = null;
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// index 0 -> first element (10)
        Arguments.of(0, 10), // last valid index -> element 30
        Arguments.of(2, 30), // negative index -> exception
        Arguments.of(-1, IndexOutOfBoundsException.class), // index == size -> exception
        Arguments.of(3, IndexOutOfBoundsException.class), // far beyond size -> exception
        Arguments.of(1000, IndexOutOfBoundsException.class), // null element at index 0 -> should return null
        Arguments.of(0, null));
    }

    private static Stream<Arguments> emptyListProvider() {
        return Stream.of(Arguments.of(0, IndexOutOfBoundsException.class), Arguments.of(-5, IndexOutOfBoundsException.class));
    }

    private static Stream<Arguments> middleIndexProvider() {
        return Stream.of(// index 2 on ["a","b","c","d","e"] -> "c"
        Arguments.of(2, "c"), // index 1 on ["x", null, "y"] -> null (expected string "null" in JSON, we assert null)
        Arguments.of(1, null));
    }

    @Test
    void getReturnsElementAtSpecifiedIndex() {
        assertEquals(10, list.get(0));
        assertEquals(20, list.get(1));
        assertEquals(30, list.get(2));
    }

    @Test
    void getThrowsIndexOutOfBoundsWhenIndexIsNegative() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    void getThrowsIndexOutOfBoundsWhenIndexIsEqualToSize() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(list.size()));
    }

    @Test
    void getWorksAfterModificationsWhenUnsealed() {
        list.add(40);
        assertEquals(40, list.get(3));
        list.remove(Integer.valueOf(20));
        // After removal, elements shift left
        assertEquals(30, list.get(1));
        assertEquals(40, list.get(2));
    }

    @Test
    void getReflectsSealStateOnlyWhenMutating() {
        // The get method itself does not check seal state, but we verify that sealing does not affect read access
        sealedState = true;
        assertEquals(20, list.get(1));
        // Ensure that subsequent mutating operation still respects seal
        assertThrows(UnsupportedOperationException.class, () -> list.add(50));
    }

    @ParameterizedTest(name = "{index}: get({0}) on basic list")
    @MethodSource("basicFunctionalityProvider")
    void basicFunctionality(int index, Object expected) {
        // Create a list with three distinct elements and a null at index 0 for the special case
        List<Object> backing = new ArrayList<>(Arrays.asList(10, 20, 30));
        if (expected == null) {
            // test case where underlying list contains a null at index 0
            backing.set(0, null);
        }
        SealableList<Object> localList = new SealableList<>(backing, sealedSupplier);
        if (expected instanceof Class && ((Class<?>) expected).equals(IndexOutOfBoundsException.class)) {
            assertThrows(IndexOutOfBoundsException.class, () -> localList.get(index));
        } else {
            assertEquals(expected, localList.get(index));
        }
    }

    @ParameterizedTest(name = "{index}: get({0}) on empty list")
    @MethodSource("emptyListProvider")
    void emptyListHandling(int index, Class<? extends Throwable> expectedException) {
        List<Integer> emptyBacking = new ArrayList<>();
        SealableList<Integer> emptyList = new SealableList<>(emptyBacking, sealedSupplier);
        assertThrows(expectedException, () -> emptyList.get(index));
    }

    @ParameterizedTest(name = "{index}: get({0}) on middle‑element list")
    @MethodSource("middleIndexProvider")
    void middleIndexRetrieval(int index, String expected) {
        // Two different backing lists are supplied via the arguments
        List<String> backing;
        if (expected.equals("c")) {
            // ["a","b","c","d","e"]
            backing = Arrays.asList("a", "b", "c", "d", "e");
        } else {
            // ["x", null, "y"]
            backing = Arrays.asList("x", null, "y");
        }
        SealableList<String> localList = new SealableList<>(new ArrayList<>(backing), sealedSupplier);
        assertEquals(expected, localList.get(index));
    }

    @Test
    void afterRemovalReflectsNewFirstElement() {
        // Start with ["a","b","c"]
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b", "c"));
        SealableList<String> modList = new SealableList<>(backing, sealedSupplier);
        // Remove element at index 0 ("a")
        modList.remove(0);
        // Now first element should be "b"
        assertEquals("b", modList.get(0));
    }

    @Test
    void afterAddAtIndexReflectsInsertedElement() {
        // Start with ["a","b"]
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        SealableList<String> modList = new SealableList<>(backing, sealedSupplier);
        // Insert "c" at index 1
        modList.add(1, "c");
        // Element at index 1 should now be "c"
        assertEquals("c", modList.get(1));
    }

    @Test
    void getAfterMultipleAddsThrowsWhenIndexEqualsSize() {
        // Add three elements to an initially empty list
        List<Integer> backing = new ArrayList<>();
        SealableList<Integer> modList = new SealableList<>(backing, sealedSupplier);
        modList.add(0, 1);
        modList.add(1, 2);
        modList.add(2, 3);
        // Size is now 3; get(3) should throw
        assertThrows(IndexOutOfBoundsException.class, () -> modList.get(3));
    }

    @Test
    void getWithIntegerMaxValueThrows() {
        // Use the default list from @BeforeEach (size 3)
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(Integer.MAX_VALUE));
    }
}
