package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import org.mockito.Mock;
import java.lang.reflect.Field;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_equals_Test {

    private SealableNavigableSet<Integer> sealableSet;

    private Supplier<Boolean> sealedSupplier;

    private volatile boolean sealedState;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        sealableSet = new SealableNavigableSet<>(sealedSupplier);
        // populate with a deterministic order
        sealableSet.add(10);
        sealableSet.add(20);
        sealableSet.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; not strictly necessary
        sealableSet = null;
    }

    /**
     * Helper that uses reflection to replace the private {@code navSet} field.
     */
    private void setInternalNavSet(Object newNavSet) throws Exception {
        Field f = SealableNavigableSet.class.getDeclaredField("navSet");
        f.setAccessible(true);
        f.set(sealableSet, newNavSet);
    }

    @Test
    void equalsSameInstance() {
        assertTrue(sealableSet.equals(sealableSet), "An object must be equal to itself");
    }

    @Test
    void equalsNull() {
        assertFalse(sealableSet.equals(null), "Equals should return false for null");
    }

    @Test
    void equalsDifferentClass() {
        assertFalse(sealableSet.equals("some string"), "Equals should return false for different class");
    }

    @Test
    void equalsUnderlyingSetSameElements() {
        // Build a plain NavigableSet with the same elements
        NavigableSet<Integer> plainSet = new TreeSet<>();
        plainSet.add(10);
        plainSet.add(20);
        plainSet.add(30);
        // SealableNavigableSet should delegate to its internal navSet, so equality must hold
        assertTrue(sealableSet.equals(plainSet), "SealableNavigableSet should be equal to a NavigableSet with same elements");
        assertTrue(plainSet.equals(sealableSet), "Symmetry: plain set should be equal to SealableNavigableSet");
    }

    @Test
    void equalsUnderlyingSetDifferentElements() {
        NavigableSet<Integer> otherSet = new TreeSet<>();
        otherSet.add(10);
        otherSet.add(20);
        // missing 30
        assertFalse(sealableSet.equals(otherSet), "Sets with different contents must not be equal");
    }

    @Test
    void equalsAnotherSealableNavigableSetSameElements() {
        SealableNavigableSet<Integer> other = new SealableNavigableSet<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(30);
        assertTrue(sealableSet.equals(other), "Two SealableNavigableSet instances with same elements should be equal");
        assertTrue(other.equals(sealableSet), "Equality must be symmetric");
    }

    @Test
    void equalsAnotherSealableNavigableSetDifferentElements() {
        SealableNavigableSet<Integer> other = new SealableNavigableSet<>(sealedSupplier);
        other.add(10);
        other.add(20);
        // different element
        other.add(40);
        assertFalse(sealableSet.equals(other), "Different element composition must yield inequality");
    }

    @Test
    void equalsAfterModificationWhenSealed() {
        // Verify that sealing does not affect equality logic (it only affects mutability)
        // set sealed, but equality should still delegate to navSet
        sealedState = true;
        NavigableSet<Integer> plainSet = new TreeSet<>(Arrays.asList(10, 20, 30));
        assertTrue(sealableSet.equals(plainSet), "Equality must work irrespective of sealed state");
    }

    @Test
    void equalsHashSetSameElements() {
        Set<Integer> hashSet = new HashSet<>(Arrays.asList(10, 20, 30));
        assertTrue(sealableSet.equals(hashSet), "HashSet with same elements should be equal to SealableNavigableSet");
        assertTrue(hashSet.equals(sealableSet), "Symmetry with HashSet");
    }

    @Test
    void equalsHashSetDifferentElements() {
        Set<Integer> hashSet = new HashSet<>(Arrays.asList(10, 20, 40));
        assertFalse(sealableSet.equals(hashSet), "HashSet with different elements must not be equal");
    }

    @Test
    void equalsArrayListSameElements() {
        List<Integer> arrayList = new ArrayList<>(Arrays.asList(10, 20, 30));
        // List is not a Set, so Set.equals must return false
        assertFalse(sealableSet.equals(arrayList), "ArrayList (a List) should never be equal to a Set");
    }

    @Test
    void equalsEmptyTreeSetWhenBothEmpty() {
        // Empty the sealable set
        sealableSet.clear();
        NavigableSet<Integer> emptyTreeSet = new TreeSet<>();
        assertTrue(sealableSet.equals(emptyTreeSet), "Two empty sets must be equal");
        assertTrue(emptyTreeSet.equals(sealableSet), "Symmetry for empty sets");
    }

    @Test
    void equalsEmptyTreeSetWhenSealableNonEmpty() {
        NavigableSet<Integer> emptyTreeSet = new TreeSet<>();
        assertFalse(sealableSet.equals(emptyTreeSet), "Empty set must not equal a non‑empty SealableNavigableSet");
    }

    @Test
    void equalsWhenInternalNavSetIsNull_NullPointerException() throws Exception {
        // Force the internal navSet to null
        setInternalNavSet(null);
        // The shortcut (o == this) is not taken, so a NPE is expected
        assertThrows(NullPointerException.class, () -> sealableSet.equals(new Object()), "When internal navSet is null, delegating to navSet.equals should throw NPE");
    }

    @Test
    void equalsWhenInternalNavSetIsNull_SelfReferenceShortCircuits() throws Exception {
        setInternalNavSet(null);
        // The (o == this) guard should prevent NPE
        assertTrue(sealableSet.equals(sealableSet), "Self‑reference must short‑circuit even if navSet is null");
    }

    @Test
    void equalsPropagatesIllegalStateExceptionFromNavSet() throws Exception {
        // Create a mock NavigableSet that throws IllegalStateException on equals()
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> throwingSet = mock(NavigableSet.class);
        when(throwingSet.equals(any())).thenThrow(new IllegalStateException("boom"));
        setInternalNavSet(throwingSet);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> sealableSet.equals(new Object()), "IllegalStateException from the wrapped navSet must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void equalsPropagatesClassCastExceptionFromNavSet() throws Exception {
        // Mock that throws ClassCastException when the argument is not a Set
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> throwingSet = mock(NavigableSet.class);
        when(throwingSet.equals(argThat(o -> !(o instanceof Set)))).thenThrow(new ClassCastException("not a Set"));
        // For any Set argument, delegate to normal equals (return false) to avoid interfering with other tests
        when(throwingSet.equals(argThat(o -> o instanceof Set))).thenReturn(false);
        setInternalNavSet(throwingSet);
        ClassCastException ex = assertThrows(ClassCastException.class, () -> sealableSet.equals("some string"), "ClassCastException from the wrapped navSet must propagate");
        assertEquals("not a Set", ex.getMessage());
    }
}
