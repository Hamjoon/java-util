package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableList_toString_Test {

    /**
     * The list under test
     */
    private SealableList<Integer> list;

    /**
     * Simple mutable flag used by the supplier to indicate sealed state
     */
    private volatile boolean sealedState;

    /**
     * Supplier that returns the current value of {@code sealedState}
     */
    private Supplier<Boolean> sealedSupplier;

    /**
     * Runs before each test method
     */
    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // Use the constructor that accepts a backing collection so we can control the initial content
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
    }

    /**
     * Runs after each test method
     */
    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no cross‑test contamination
        list = null;
    }

    /**
     * Provides arguments for the "Non‑empty list" group.
     */
    private static Stream<Arguments> nonEmptyListProvider() {
        return Stream.of(// Simple integer list
        Arguments.of(Arrays.asList(1, 2, 3), "[1, 2, 3]", "SealableList wrapping an ArrayList containing three integers should return the standard list representation \"[1, 2, 3]\"."), // Mixed String / null list
        Arguments.of(Arrays.asList("a", null, "c"), "[a, null, c]", "SealableList wrapping a list with mixed String and null elements should preserve order and represent null as \"null\" in the output."), // Cleared list (populate then clear)
        Arguments.of(new ArrayList<Integer>(Arrays.asList(5, 6, 7)), "[]", "SealableList wrapping a list that was cleared after being populated should reflect the cleared state, returning an empty representation."), // Boolean list
        Arguments.of(Arrays.asList(true, false, true), "[true, false, true]", "SealableList wrapping a list of Boolean values should correctly render the boolean literals in the string."), // Nested list
        Arguments.of(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)), "[[1, 2], [3, 4]]", "SealableList wrapping a list of nested lists should delegate to the underlying list's toString(), producing a nested representation."));
    }

    @Test
    void toStringMatchesUnderlyingList() {
        List<Integer> expected = Arrays.asList(10, 20, 30);
        assertEquals(expected.toString(), list.toString(), "toString() should delegate to the backing list's toString()");
    }

    @Test
    void toStringOnEmptyList() {
        SealableList<Integer> empty = new SealableList<>(sealedSupplier);
        assertEquals("[]", empty.toString(), "An empty SealableList should render as []");
    }

    @Test
    void toStringReflectsLaterModifications() {
        list.add(40);
        List<Integer> expected = Arrays.asList(10, 20, 30, 40);
        assertEquals(expected.toString(), list.toString(), "toString() must reflect elements added after construction");
    }

    @Test
    void toStringHandlesNullElements() {
        list.add(null);
        List<Integer> expected = Arrays.asList(10, 20, 30, null);
        assertEquals(expected.toString(), list.toString(), "null values should appear as the string \"null\" in the output");
    }

    @Test
    void toStringWorksWhenSealed() {
        // switch to sealed mode
        sealedState = true;
        // The list should still be readable; only mutating operations are blocked
        List<Integer> expected = Arrays.asList(10, 20, 30);
        assertEquals(expected.toString(), list.toString(), "toString() must be callable even when the list is sealed");
    }

    @Test
    void toStringWhenConstructedWithExistingList() {
        List<Integer> backing = new ArrayList<>(Arrays.asList(1, 2, 3));
        SealableList<Integer> fromList = new SealableList<>(backing, sealedSupplier);
        assertEquals(backing.toString(), fromList.toString(), "Constructor that receives an existing List should expose its toString()");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonEmptyListProvider")
    void toStringNonEmptyListVarious(List<?> backingContent, String expectedString, String description) {
        // Arrange: create a SealableList with the supplied backing content
        List<Object> backing = new ArrayList<>(backingContent);
        SealableList<Object> sl = new SealableList<>(backing, () -> false);
        // Special handling for the "cleared list" case – we need to clear after population
        if (backingContent instanceof ArrayList && ((ArrayList<?>) backingContent).size() > 0 && expectedString.equals("[]")) {
            sl.clear();
        }
        // Act & Assert
        assertEquals(expectedString, sl.toString(), description);
    }

    /**
     * Test that constructing with a {@code null} backing list leads to NPE when toString() is called.
     */
    @Test
    void toStringWithNullUnderlyingListThrowsNPE() throws Exception {
        // Use reflection to invoke the (List<T>, Supplier<Boolean>) constructor with a null list.
        Constructor<SealableList> ctor = SealableList.class.getDeclaredConstructor(List.class, Supplier.class);
        ctor.setAccessible(true);
        SealableList<?> sl = ctor.newInstance(null, (Supplier<Boolean>) () -> false);
        // The NPE should be thrown when delegating to the underlying list.
        assertThrows(NullPointerException.class, sl::toString, "SealableList constructed with a null backing list should throw NullPointerException when toString() is invoked.");
    }

    /**
     * Test that a backing list whose toString() throws propagates the exception.
     */
    @Test
    void toStringWhenUnderlyingListThrowsException() {
        // Create a mock List that throws a RuntimeException from toString()
        @SuppressWarnings("unchecked")
        List<Object> throwingList = mock(List.class);
        when(throwingList.toString()).thenThrow(new RuntimeException("boom"));
        // Build a SealableList around the mock
        SealableList<Object> sl = new SealableList<>(throwingList, () -> false);
        // Verify that the RuntimeException propagates unchanged
        RuntimeException ex = assertThrows(RuntimeException.class, sl::toString, "SealableList should propagate RuntimeException thrown by underlying list's toString().");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Test that an explicitly empty list (created with the no‑arg constructor) renders as []
     */
    @Test
    void toStringEmptyListViaNoArgConstructor() {
        SealableList<Integer> empty = new SealableList<>(sealedSupplier);
        assertEquals("[]", empty.toString(), "SealableList created with the no‑arg constructor should be empty and render as []");
    }
}
