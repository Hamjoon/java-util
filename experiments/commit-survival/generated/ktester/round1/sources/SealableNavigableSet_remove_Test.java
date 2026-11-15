package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_remove_Test {

    private SealableNavigableSet<Integer> set;

    private static volatile boolean sealedState;

    private static Supplier<Boolean> sealedSupplier;

    @Mock
    private NavigableSet<String> // injected mock for the underlying set
    mockNavSet;

    /* --------------------------------------------------------------------- *
     *  Helper holder for parameterised arguments
     * --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Object input;

        // null when we expect an exception
        final Boolean expectedResult;

        final String description;

        ArgumentsHolder(Object input, Boolean expectedResult, String description) {
            this.input = input;
            this.expectedResult = expectedResult;
            this.description = description;
        }

        @Override
        public String toString() {
            return description + " (input=" + input + ")";
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the shared sealed state supplier – it will be re‑assigned per test in setupBeforeEach
        sealedState = false;
        sealedSupplier = () -> sealedState;
        sealedState = false;
        sealedSupplier = () -> sealedState;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure each test starts with an un‑sealed set and a known collection of elements
        sealedState = false;
        set = new SealableNavigableSet<>(sealedSupplier);
        // test removal of a null element
        set.add(null);
        set.add(10);
        set.add(20);
        set.add(30);
        sealedState = false;
        // Create a SealableNavigableSet that uses the supplier; the underlying set will be replaced by the mock
        set = new SealableNavigableSet<>(sealedSupplier);
        // Inject the mock into the private field 'navSet'
        Field f = SealableNavigableSet.class.getDeclaredField("navSet");
        f.setAccessible(true);
        f.set(set, mockNavSet);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test footprint tidy
        set = null;
        set = null;
    }

    private static Stream<ArgumentsHolder> removeWhenUnsealedAndElementPresent() {
        return Stream.of(new ArgumentsHolder("apple", true, "element present"));
    }

    private static Stream<ArgumentsHolder> removeWhenUnsealedAndElementAbsent() {
        return Stream.of(new ArgumentsHolder("banana", false, "element absent"));
    }

    private static Stream<ArgumentsHolder> removeNullWhenUnsealedAndUnderlyingRejectsNull() {
        return Stream.of(new ArgumentsHolder(null, false, "null not accepted"));
    }

    private static Stream<ArgumentsHolder> removeIncompatibleType() {
        return Stream.of(new ArgumentsHolder(123, null, "incompatible type"));
    }

    private static Stream<ArgumentsHolder> removeWhenUnderlyingThrowsRuntime() {
        return Stream.of(new ArgumentsHolder("trigger", null, "underlying throws RuntimeException"));
    }

    private static Stream<ArgumentsHolder> removeWhenUnderlyingThrowsIllegalArgument() {
        return Stream.of(new ArgumentsHolder("invalid", null, "underlying throws IllegalArgumentException"));
    }

    private static Stream<ArgumentsHolder> removeWhenUnderlyingThrowsConcurrentModification() {
        return Stream.of(new ArgumentsHolder("concurrent", null, "underlying throws ConcurrentModificationException"));
    }

    private static Stream<ArgumentsHolder> removeWhenUnderlyingUnmodifiable() {
        return Stream.of(new ArgumentsHolder("apple", null, "underlying unmodifiable set"));
    }

    private static Stream<ArgumentsHolder> removeWhenEmptySet() {
        return Stream.of(new ArgumentsHolder("any", false, "empty set"));
    }

    @Test
    void removeExistingElementWhenUnsealed_returnsTrueAndRemoves() {
        assertTrue(set.contains(20), "pre‑condition: set should contain 20");
        boolean removed = set.remove(20);
        assertTrue(removed, "remove should return true for existing element");
        assertFalse(set.contains(20), "element should no longer be present after removal");
        assertEquals(3, set.size(), "size should decrease by one");
    }

    @Test
    void removeNonExistingElementWhenUnsealed_returnsFalse() {
        assertFalse(set.contains(99), "pre‑condition: set should not contain 99");
        boolean removed = set.remove(99);
        assertFalse(removed, "remove should return false when element is absent");
        assertEquals(4, set.size(), "size must remain unchanged");
    }

    @Test
    void removeNullElementWhenUnsealed_behavesCorrectly() {
        assertTrue(set.contains(null), "pre‑condition: set should contain null");
        boolean removed = set.remove(null);
        assertTrue(removed, "remove should return true for null element");
        assertFalse(set.contains(null), "null should no longer be present");
        assertEquals(3, set.size(), "size should decrease by one");
    }

    @Test
    void removeFromEmptySetWhenUnsealed_returnsFalse() {
        // Clear the set first
        set.clear();
        assertEquals(0, set.size(), "set should be empty after clear");
        boolean removed = set.remove(10);
        assertFalse(removed, "remove on empty set must return false");
    }

    @Test
    void removeWhenSealed_throwsUnsupportedOperationException() {
        // switch to sealed mode
        sealedState = true;
        // The contract of SealableNavigableSet is to throw UnsupportedOperationException for mutating ops
        assertThrows(UnsupportedOperationException.class, () -> set.remove(10));
        // also verify that removal of null is blocked
        assertThrows(UnsupportedOperationException.class, () -> set.remove(null));
    }

    @ParameterizedTest(name = "[{index}] remove existing ''{0}'' → {1}")
    @MethodSource("removeWhenUnsealedAndElementPresent")
    void removeWhenUnsealedAndElementPresent(ArgumentsHolder ah) {
        // arrange
        when(mockNavSet.remove(ah.input)).thenReturn(ah.expectedResult);
        // act
        boolean result = set.remove(ah.input);
        // assert
        assertTrue(result, "remove should return true for present element");
        verify(mockNavSet, times(1)).remove(ah.input);
    }

    @ParameterizedTest(name = "[{index}] remove absent ''{0}'' → {1}")
    @MethodSource("removeWhenUnsealedAndElementAbsent")
    void removeWhenUnsealedAndElementAbsent(ArgumentsHolder ah) {
        when(mockNavSet.remove(ah.input)).thenReturn(ah.expectedResult);
        boolean result = set.remove(ah.input);
        assertFalse(result, "remove should return false when element is absent");
        verify(mockNavSet, times(1)).remove(ah.input);
    }

    @ParameterizedTest(name = "[{index}] remove null when underlying rejects null → {1}")
    @MethodSource("removeNullWhenUnsealedAndUnderlyingRejectsNull")
    void removeNullWhenUnsealedAndUnderlyingRejectsNull(ArgumentsHolder ah) {
        // TreeSet (natural ordering) throws NPE on null; we simulate that
        when(mockNavSet.remove(null)).thenThrow(NullPointerException.class);
        assertThrows(NullPointerException.class, () -> set.remove(null));
        verify(mockNavSet, times(1)).remove(null);
    }

    @ParameterizedTest(name = "[{index}] remove incompatible type ''{0}'' → ClassCastException")
    @MethodSource("removeIncompatibleType")
    void removeIncompatibleType(ArgumentsHolder ah) {
        // Simulate TreeSet throwing ClassCastException for wrong type
        when(mockNavSet.remove(ah.input)).thenThrow(ClassCastException.class);
        assertThrows(ClassCastException.class, () -> set.remove(ah.input));
        verify(mockNavSet, times(1)).remove(ah.input);
    }

    @ParameterizedTest(name = "[{index}] underlying throws RuntimeException for ''{0}''")
    @MethodSource("removeWhenUnderlyingThrowsRuntime")
    void removeWhenUnderlyingThrowsRuntimeException(ArgumentsHolder ah) {
        when(mockNavSet.remove("trigger")).thenThrow(RuntimeException.class);
        assertThrows(RuntimeException.class, () -> set.remove("trigger"));
        verify(mockNavSet, times(1)).remove("trigger");
    }

    @ParameterizedTest(name = "[{index}] underlying throws IllegalArgumentException for ''{0}''")
    @MethodSource("removeWhenUnderlyingThrowsIllegalArgument")
    void removeWhenUnderlyingThrowsIllegalArgumentException(ArgumentsHolder ah) {
        when(mockNavSet.remove("invalid")).thenThrow(IllegalArgumentException.class);
        assertThrows(IllegalArgumentException.class, () -> set.remove("invalid"));
        verify(mockNavSet, times(1)).remove("invalid");
    }

    @ParameterizedTest(name = "[{index}] underlying throws ConcurrentModificationException for ''{0}''")
    @MethodSource("removeWhenUnderlyingThrowsConcurrentModification")
    void removeWhenUnderlyingThrowsConcurrentModificationException(ArgumentsHolder ah) {
        when(mockNavSet.remove("concurrent")).thenThrow(ConcurrentModificationException.class);
        assertThrows(ConcurrentModificationException.class, () -> set.remove("concurrent"));
        verify(mockNavSet, times(1)).remove("concurrent");
    }

    @ParameterizedTest(name = "[{index}] underlying unmodifiable set throws UnsupportedOperationException for ''{0}''")
    @MethodSource("removeWhenUnderlyingUnmodifiable")
    void removeWhenUnderlyingUnmodifiableSet(ArgumentsHolder ah) {
        when(mockNavSet.remove("apple")).thenThrow(UnsupportedOperationException.class);
        assertThrows(UnsupportedOperationException.class, () -> set.remove("apple"));
        verify(mockNavSet, times(1)).remove("apple");
    }

    @ParameterizedTest(name = "[{index}] remove from empty set returns false for ''{0}''")
    @MethodSource("removeWhenEmptySet")
    void removeFromEmptySetReturnsFalse(ArgumentsHolder ah) {
        when(mockNavSet.remove(ah.input)).thenReturn(ah.expectedResult);
        boolean result = set.remove(ah.input);
        assertFalse(result, "remove on empty set should return false");
        verify(mockNavSet, times(1)).remove(ah.input);
    }

    @Test
    void removeAfterSealThenUnseal_succeeds() {
        // seal then unseal via the shared supplier
        sealedState = true;
        // first call must fail
        assertThrows(UnsupportedOperationException.class, () -> set.remove("apple"));
        // unseal
        sealedState = false;
        when(mockNavSet.remove("apple")).thenReturn(true);
        assertTrue(set.remove("apple"), "after unsealing removal should succeed");
        verify(mockNavSet, times(1)).remove("apple");
    }
}
