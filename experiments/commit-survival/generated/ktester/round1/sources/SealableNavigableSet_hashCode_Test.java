package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_hashCode_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // The constructor used in the production code accepts a Supplier<Boolean>
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate the set with a deterministic collection of values
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly necessary
        set = null;
    }

    private static Stream<Arguments> provideDelegationScenarios() {
        return Stream.of(Arguments.of("empty set", Collections.<Integer>emptySet()), Arguments.of("non‑empty set", Set.of(10, 20, 30)));
    }

    @Test
    @DisplayName("hashCode() should match the hashCode of an equivalent NavigableSet")
    void testHashCodeMatchesUnderlyingSet() {
        NavigableSet<Integer> reference = new TreeSet<>();
        reference.add(10);
        reference.add(20);
        reference.add(30);
        assertEquals(reference.hashCode(), set.hashCode(), "SealabeNavigableSet.hashCode() must delegate to the wrapped set");
    }

    @Test
    @DisplayName("hashCode() changes when the set is modified")
    void testHashCodeChangesAfterModification() {
        int originalHash = set.hashCode();
        // modify the set
        set.add(40);
        int newHash = set.hashCode();
        assertNotEquals(originalHash, newHash, "hashCode should reflect the current contents of the set");
    }

    @Test
    @DisplayName("hashCode() is independent of the sealed state")
    void testHashCodeWhenSealed() {
        // Ensure the set is sealed
        sealedState = true;
        NavigableSet<Integer> reference = new TreeSet<>();
        reference.add(10);
        reference.add(20);
        reference.add(30);
        assertEquals(reference.hashCode(), set.hashCode(), "Sealed state must not affect hashCode computation");
    }

    @Test
    @DisplayName("hashCode() of an empty SealableNavigableSet matches an empty TreeSet")
    void testHashCodeEmptySet() {
        // Create a fresh empty set
        SealableNavigableSet<Integer> emptySet = new SealableNavigableSet<>(sealedSupplier);
        NavigableSet<Integer> emptyReference = new TreeSet<>();
        assertEquals(emptyReference.hashCode(), emptySet.hashCode(), "Empty sets should have identical hash codes");
    }

    /**
     * Verify that {@code hashCode()} returns the same value as the wrapped
     * {@link NavigableSet} for both empty and non‑empty sets.
     */
    @ParameterizedTest(name = "{index} => scenario={0}")
    @MethodSource("provideDelegationScenarios")
    @DisplayName("hashCode() delegates to the underlying NavigableSet")
    void testHashCodeDelegatesToUnderlyingSet(String scenario, NavigableSet<Integer> initialElements) {
        // Create a fresh SealableNavigableSet for each scenario
        SealableNavigableSet<Integer> localSet = new SealableNavigableSet<>(sealedSupplier);
        initialElements.forEach(localSet::add);
        // Build a reference TreeSet with the same elements
        NavigableSet<Integer> reference = new TreeSet<>(initialElements);
        assertEquals(reference.hashCode(), localSet.hashCode(), "SealabeNavigableSet.hashCode() must delegate to the wrapped set for scenario: " + scenario);
    }

    /**
     * The implementation directly calls {@code navSet.hashCode()}.  If the
     * private field {@code navSet} is {@code null}, a {@link NullPointerException}
     * should be thrown.
     */
    @Test
    @DisplayName("hashCode() throws NullPointerException when internal navSet is null")
    void testHashCodeThrowsWhenNavSetNull() throws Exception {
        // Use reflection to set the private field 'navSet' to null
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        navSetField.set(set, null);
        assertThrows(NullPointerException.class, set::hashCode, "Calling hashCode() with a null internal navSet must throw NullPointerException");
    }
}
