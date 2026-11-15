package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class SealableList_equals_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    /**
     * A minimal List implementation whose {@code equals} always throws
     * {@link IllegalStateException}. Only the methods required for the test
     * are implemented.
     */
    private static class TestEqualsThrowingList<T> extends AbstractList<T> {

        @Override
        public T get(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            throw new IllegalStateException("Intentional exception from equals");
        }
    }

    /**
     * A List implementation that delegates its {@code equals} back to the
     * argument, creating a recursive call chain when compared with another
     * List (e.g., {@link SealableList}).
     */
    private static class RecursiveEqualsList<T> extends AbstractList<T> {

        @Override
        public T get(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            // Call the other object's equals, passing 'this' – this will cause
            // the other List (the internal list of SealableList) to call back into
            // this method, leading to infinite recursion.
            return obj.equals(this);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // Use the constructor that accepts a backing collection and the supplier
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly necessary
        list = null;
        sealedSupplier = null;
    }

    private static Stream<Arguments> provideDifferentListImplementation() {
        return Stream.of(Arguments.of(new LinkedList<>(List.of("a", "b")), true), Arguments.of(new ArrayList<>(List.of("a", "b")), true));
    }

    @Test
    void testEqualsSelf() {
        assertEquals(list, list, "A list should be equal to itself");
    }

    @Test
    void testEqualsSameElementsDifferentInstance() {
        SealableList<Integer> other = new SealableList<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(30);
        assertEquals(list, other, "Two SealableList instances with identical elements should be equal");
        assertEquals(other, list, "Equality should be symmetric");
    }

    @Test
    void testEqualsDifferentElements() {
        SealableList<Integer> other = new SealableList<>(sealedSupplier);
        other.add(10);
        other.add(20);
        // different element
        other.add(40);
        assertNotEquals(list, other, "Lists with different contents must not be equal");
    }

    @Test
    void testEqualsNull() {
        assertNotEquals(list, null, "A list must not be equal to null");
    }

    @Test
    void testEqualsDifferentType() {
        List<Integer> arrayList = new ArrayList<>();
        arrayList.add(10);
        arrayList.add(20);
        arrayList.add(30);
        // The underlying list's equals implementation will compare to any List,
        // so SealableList should be equal to a plain List with the same elements.
        assertEquals(list, arrayList, "SealableList should be equal to a regular List with same elements");
        // Conversely, a regular List should also consider SealableList equal.
        assertEquals(arrayList, list, "Regular List should be equal to SealableList with same elements");
    }

    @Test
    void testEqualsWhenSealedStateDiffers() {
        // SealableList delegates equals to the wrapped list, which does not depend on seal state.
        // change seal state for the primary list
        sealedState = true;
        SealableList<Integer> other = new SealableList<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(30);
        assertEquals(list, other, "Seal state should not affect equality");
    }

    /**
     * 1. Equality with the same underlying list instance **
     */
    @Test
    void testEqualsWithExactSameBackingListInstance() {
        // underlying list that will be shared
        List<String> backing = new ArrayList<>(List.of("a", "b"));
        SealableList<String> sealable = new SealableList<>(backing, sealedSupplier);
        // Passing the *same* backing list to equals should succeed
        assertTrue(sealable.equals(backing), "SealableList should delegate to its internal list and return true when compared with the exact same List instance");
    }

    @Test
    void testEqualsWithAnotherSealableListHavingSameElements() {
        // Two different SealableList objects, each wrapping its own ArrayList with the same elements
        SealableList<String> first = new SealableList<>(List.of("a", "b"), sealedSupplier);
        SealableList<String> second = new SealableList<>(List.of("a", "b"), sealedSupplier);
        // The internal list of 'first' receives a SealableList as argument – not a List – so equals must be false
        assertFalse(first.equals(second), "Underlying List.equals receives a SealableList argument, which is not a List, therefore equality must be false");
    }

    /**
     * 2. Equality with null **
     */
    @Test
    void testEqualsWithNullArgument() {
        SealableList<String> sealable = new SealableList<>(List.of("x", "y"), sealedSupplier);
        assertFalse(sealable.equals(null), "Calling equals with null should return false because the wrapped list returns false for a null argument");
    }

    /**
     * 3. Equality with same elements but different List implementation **
     */
    @ParameterizedTest
    @MethodSource("provideDifferentListImplementations")
    void testEqualsWithDifferentListImplementation(List<String> other, boolean expected) {
        SealableList<String> sealable = new SealableList<>(List.of("a", "b"), sealedSupplier);
        assertEquals(expected, sealable.equals(other), "SealableList should be equal to any List implementation that contains the same elements in the same order");
    }

    /**
     * 4. Equality with different elements **
     */
    @Test
    void testEqualsWithDifferentElements() {
        SealableList<String> sealable = new SealableList<>(List.of("a", "b"), sealedSupplier);
        List<String> other = List.of("a", "c");
        assertFalse(sealable.equals(other), "Lists with differing element values are not equal");
    }

    /**
     * 5. Equality with same elements in different order **
     */
    @Test
    void testEqualsWithSameElementsDifferentOrder() {
        SealableList<String> sealable = new SealableList<>(List.of("a", "b"), sealedSupplier);
        List<String> other = List.of("b", "a");
        assertFalse(sealable.equals(other), "List.equals is order‑sensitive; a list with the same elements but a different order should return false");
    }

    /**
     * 6. Equality with empty lists **
     */
    @Test
    void testEqualsWithTwoEmptyLists() {
        SealableList<String> sealable = new SealableList<>(new ArrayList<>(), sealedSupplier);
        List<String> other = Collections.emptyList();
        assertTrue(sealable.equals(other), "Two empty lists are equal");
    }

    /**
     * 7. Equality with null elements inside the list **
     */
    @Test
    void testEqualsWithNullElementsInsideBothLists() {
        SealableList<String> sealable = new SealableList<>(new ArrayList<>(List.of(null, "b")), sealedSupplier);
        List<String> other = new ArrayList<>(List.of(null, "b"));
        assertTrue(sealable.equals(other), "Lists containing null values at the same positions should be considered equal");
    }

    /**
     * 8. Equality with self (same SealableList instance) **
     */
    @Test
    void testEqualsSelfSealableListInstance() {
        SealableList<String> sealable = new SealableList<>(List.of("a", "b"), sealedSupplier);
        // The argument is the *same* SealableList instance, not the underlying List.
        // The internal list receives a SealableList argument, which is not a List → false.
        assertFalse(sealable.equals(sealable), "Calling equals on a SealableList with itself passes the SealableList object to the underlying List.equals, which returns false");
    }

    /**
     * 9. Equality with a non‑List collection type **
     */
    @Test
    void testEqualsWithNonListCollection() {
        SealableList<String> sealable = new SealableList<>(List.of("a", "b"), sealedSupplier);
        Set<String> other = new HashSet<>(List.of("a", "b"));
        assertFalse(sealable.equals(other), "The underlying List.equals returns false when the argument is not a List (e.g., a Set)");
    }

    /**
     * 10. NullPointerException when internal list is null **
     */
    @Test
    void testEqualsWhenInternalListIsNull() throws Exception {
        SealableList<String> sealable = new SealableList<>(List.of("x", "y"), sealedSupplier);
        // Force the private 'list' field to null via reflection
        Field f = SealableList.class.getDeclaredField("list");
        f.setAccessible(true);
        f.set(sealable, null);
        List<String> other = Collections.emptyList();
        assertThrows(NullPointerException.class, () -> sealable.equals(other), "If the internal list field is null, invoking equals should throw NullPointerException");
    }

    /**
     * 11. RuntimeException propagated from other object's equals **
     */
    @Test
    void testEqualsPropagatesExceptionFromOtherEquals() {
        SealableList<String> sealable = new SealableList<>(List.of("a", "b"), sealedSupplier);
        TestEqualsThrowingList<String> throwing = new TestEqualsThrowingList<>();
        // The internal list will call other.equals(this) which throws IllegalStateException
        assertThrows(IllegalStateException.class, () -> sealable.equals(throwing), "Exception thrown by the other object's equals should propagate");
    }

    /**
     * 12. StackOverflowError due to recursive equals invocation **
     */
    @Test
    void testEqualsRecursiveEqualsLeadsToStackOverflow() {
        SealableList<String> sealable = new SealableList<>(List.of("a", "b"), sealedSupplier);
        RecursiveEqualsList<String> recursive = new RecursiveEqualsList<>();
        assertThrows(StackOverflowError.class, () -> sealable.equals(recursive), "Mutual recursion between the two equals implementations should eventually cause StackOverflowError");
    }
}
