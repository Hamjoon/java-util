package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableList_hashCode_Test {

    /* --------------------------------------------------------------------- */
    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the sealed flag starts as false for each test
        sealedState = false;
        // Initialise a SealableList backed by a mutable ArrayList
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // Populate with a deterministic set of elements (used by the three
        // hand‑written tests at the bottom of this class)
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
        list = null;
    }

    /**
     * Provides a stream of arguments for {@link #testHashCodeDelegation}.
     *
     * <p>
     * Each argument consists of:
     * <ul>
     *   <li>a textual description (used as the test name)</li>
     *   <li>the initial list contents</li>
     *   <li>an optional mutating action applied after construction</li>
     *   <li>the expected hash code (computed from a plain {@link ArrayList})</li>
     * </ul>
     * </p>
     */
    private static Stream<Arguments> hashCodeDelegationProvider() {
        return Stream.of(// ----- basic delegation -------------------------------------------------
        arguments("empty list", List.of(), null, Collections.emptyList().hashCode()), arguments("non‑empty distinct elements", List.of("a", "b", "c"), null, List.of("a", "b", "c").hashCode()), arguments("list containing nulls", List.of(null, "x", null), null, List.of(null, "x", null).hashCode()), arguments("add element after construction", List.of(1, 2, 3), (Consumer<SealableList<Object>>) sl -> sl.add(4), List.of(1, 2, 3, 4).hashCode()), arguments("duplicate values", List.of("same", "same"), null, List.of("same", "same").hashCode()), // ----- additional scenarios --------------------------------------------
        arguments("boolean values", List.of(true, false, true), null, List.of(true, false, true).hashCode()), arguments("empty/whitespace strings", List.of("", " ", ""), null, List.of("", " ", "").hashCode()), arguments("distinct Object instances (default hashCode)", List.of(new Object(), new Object()), null, List.of(new Object(), new Object()).hashCode()), arguments("remove element at index 1", List.of("a", "b", "c"), (Consumer<SealableList<Object>>) sl -> sl.remove(1), List.of("a", "c").hashCode()), arguments("set index 0 to new value", List.of("x", "y", "z"), (Consumer<SealableList<Object>>) sl -> sl.set(0, "new"), List.of("new", "y", "z").hashCode()), arguments("all‑null list", List.of(null, null, null), null, List.of(null, null, null).hashCode()), arguments("clear the list", List.of("a", "b", "c"), (Consumer<SealableList<Object>>) SealableList::clear, Collections.emptyList().hashCode()), arguments("unicode characters", List.of("α", "β", "γ"), null, List.of("α", "β", "γ").hashCode()));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testHashCodeEquality() {
        // Create another SealableList with the same contents and supplier
        SealableList<Integer> other = new SealableList<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(30);
        // The hash codes must be identical because the underlying lists are equal
        assertEquals(list.hashCode(), other.hashCode(), "Hash codes should be equal for lists with identical elements");
    }

    @Test
    void testHashCodeInequalityAfterModification() {
        // Create a second list with the same initial elements
        SealableList<Integer> other = new SealableList<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(30);
        // Verify they start equal
        assertEquals(list.hashCode(), other.hashCode(), "Initial hash codes should match");
        // Modify the second list – this must change its hash code
        other.add(40);
        // The hash codes must now differ
        assertNotEquals(list.hashCode(), other.hashCode(), "Hash codes should differ after the underlying list changes");
    }

    @Test
    void testHashCodeRespectsSealState() {
        // The seal state does not affect hashCode – it simply delegates to the list
        // Verify hash code while unsealed
        int hashUnsealed = list.hashCode();
        // Seal the list and verify the hash code is unchanged
        sealedState = true;
        assertEquals(hashUnsealed, list.hashCode(), "Hash code should remain the same regardless of sealed state");
    }

    /**
     * Verifies that {@link SealableList#hashCode()} returns exactly the same value as the
     * underlying list implementation for a wide range of data scenarios.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("hashCodeDelegationProvider")
    void testHashCodeDelegation(String description, List<Object> initialElements, Consumer<SealableList<Object>> modifier, int expectedHash) {
        // Build a mutable backing list with the requested initial elements
        List<Object> backing = new ArrayList<>(initialElements);
        SealableList<Object> sealable = new SealableList<>(backing, () -> false);
        // Apply the optional modification (add, remove, set, clear, …)
        if (modifier != null) {
            modifier.accept(sealable);
        }
        // The hash code must match the expected value derived from a plain ArrayList
        assertEquals(expectedHash, sealable.hashCode(), () -> "Hash code mismatch for scenario: " + description);
    }

    /**
     * If the internal {@code list} reference is {@code null}, {@code hashCode()}
     * should throw a {@link NullPointerException}.  The test uses reflection to
     * inject a {@code null} value because the constructor never allows it.
     */
    @Test
    void testHashCodeThrowsWhenUnderlyingListIsNull() throws Exception {
        // Create a normal SealableList first
        SealableList<Object> sealable = new SealableList<>(new ArrayList<>(), () -> false);
        // Use reflection to set the private field 'list' to null
        Field listField = SealableList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(sealable, null);
        // Expect NPE when hashCode() is invoked
        assertThrows(NullPointerException.class, sealable::hashCode, "hashCode() should throw NPE when the backing list is null");
    }

    /**
     * If the underlying list's {@code hashCode()} method throws an exception,
     * {@code SealableList.hashCode()} must propagate it unchanged.
     */
    @Test
    void testHashCodePropagatesUnderlyingException(@Mock List<Object> mockList) {
        // Configure the mock to throw IllegalStateException on hashCode()
        when(mockList.hashCode()).thenThrow(new IllegalStateException("underlying failure"));
        // Build a SealableList that wraps the mock list
        SealableList<Object> sealable = new SealableList<>(mockList, () -> false);
        // The same exception type should be re‑thrown
        IllegalStateException ex = assertThrows(IllegalStateException.class, sealable::hashCode, "hashCode() should propagate the exception from the underlying list");
        assertEquals("underlying failure", ex.getMessage());
    }
}
