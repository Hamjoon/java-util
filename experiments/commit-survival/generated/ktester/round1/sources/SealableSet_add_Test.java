package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableSet_add_Test {

    /**
     * The SealableSet under test.
     */
    private SealableSet<Integer> set;

    /**
     * Controls the sealed state supplied to the SealableSet.
     */
    private volatile boolean sealed;

    /**
     * Supplier that returns the current value of {@code sealed}.
     */
    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        // Reset the sealed flag and create a fresh supplier for each test.
        sealed = false;
        sealedSupplier = () -> sealed;
        // Create a fresh SealableSet with a *real* backing HashSet.
        // The constructor that accepts a Supplier<Boolean> is part of the library.
        set = new SealableSet<>(sealedSupplier);
        // Populate with a few baseline values used by many tests.
        set.add(10);
        set.add(20);
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly required but keeps the pattern clear.
        set = null;
    }

    @Test
    void testAddNewElementReturnsTrueAndIsPresent() {
        // given
        int originalSize = set.size();
        // when
        boolean added = set.add(30);
        // then
        assertTrue(added, "add() should return true for a new element");
        assertTrue(set.contains(30), "Set must contain the newly added element");
        assertEquals(originalSize + 1, set.size(), "Size should increase by one");
    }

    @Test
    void testAddDuplicateElementReturnsFalseAndDoesNotChangeSize() {
        // given
        int originalSize = set.size();
        // when
        // 10 is already present
        boolean added = set.add(10);
        // then
        assertFalse(added, "add() should return false when the element already exists");
        assertEquals(originalSize, set.size(), "Size must remain unchanged after adding duplicate");
    }

    @Test
    void testAddNullWhenNullAlreadyPresentReturnsFalseAndSizeUnchanged() {
        // given
        int originalSize = set.size();
        // when
        // null was added during setUp()
        boolean added = set.add(null);
        // then
        assertFalse(added, "add() should return false when null is already present");
        assertEquals(originalSize, set.size(), "Size must remain unchanged after adding duplicate null");
    }

    @Test
    void testAddWhenSealedThrowsUnsupportedOperationException() {
        // given
        // make the set immutable
        sealed = true;
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> set.add(40), "add() must throw when the set is sealed");
    }

    @Test
    void testAddWhenSealedDoesNotModifySet() {
        // given
        sealed = true;
        int originalSize = set.size();
        // when
        try {
            set.add(50);
        } catch (UnsupportedOperationException ignored) {
            // expected
        }
        // then
        assertEquals(originalSize, set.size(), "Size must stay the same after a failed add on a sealed set");
        assertFalse(set.contains(50), "Set must not contain the element that was attempted to be added while sealed");
    }

    /**
     * Replaces the private {@code set} field inside {@link SealableSet} with the supplied delegate.
     * This allows us to inject mocks or {@code null} for the various edge‑case tests.
     */
    @SuppressWarnings("unchecked")
    private void injectDelegate(Set<Integer> delegate) throws Exception {
        Field f = SealableSet.class.getDeclaredField("set");
        f.setAccessible(true);
        // Remove final modifier (works for test environments).
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(set, delegate);
    }

    /**
     * Adding {@code null} to a set that does **not** already contain {@code null}
     * should delegate to the underlying set and return {@code true}.
     */
    @Test
    void testAddNullWhenNotPresentReturnsTrue() {
        // create a fresh SealableSet without the pre‑added null
        set = new SealableSet<>(sealedSupplier);
        // ensure the set is empty
        assertFalse(set.contains(null));
        boolean added = set.add(null);
        assertTrue(added, "add(null) should return true when null was not present");
        assertTrue(set.contains(null), "Set must contain null after successful addition");
    }

    @Test
    void testAddWhenSealedThrowsIllegalStateException() {
        // given
        // make the set immutable
        sealed = true;
        // when / then
        assertThrows(IllegalStateException.class, () -> set.add(40), "add() must throw IllegalStateException when the set is sealed");
    }

    @Test
    void testAddNullWhenDelegateRejectsNull_throwsNullPointerException(@Mock Set<Integer> mockDelegate) throws Exception {
        // given: delegate throws NPE for null
        when(mockDelegate.add(null)).thenThrow(new NullPointerException("null not allowed"));
        injectDelegate(mockDelegate);
        // when / then
        NullPointerException ex = assertThrows(NullPointerException.class, () -> set.add(null), "add(null) should propagate NullPointerException from the delegate");
        assertEquals("null not allowed", ex.getMessage());
    }

    @Test
    void testAddWhenDelegateThrowsUnsupportedOperationException(@Mock Set<Integer> mockDelegate) throws Exception {
        // given
        when(mockDelegate.add(1)).thenThrow(new UnsupportedOperationException("immutable"));
        injectDelegate(mockDelegate);
        // when / then
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> set.add(1), "add() should propagate UnsupportedOperationException from the delegate");
        assertEquals("immutable", ex.getMessage());
    }

    @Test
    void testAddWhenDelegateThrowsClassCastException(@Mock Set<Integer> mockDelegate) throws Exception {
        // given
        when(mockDelegate.add(any())).thenThrow(new ClassCastException("cannot cast"));
        injectDelegate(mockDelegate);
        // when / then
        ClassCastException ex = assertThrows(ClassCastException.class, // 123 is an Integer, but the mock pretends to be a Set<Integer>
        () -> set.add(123), "add() should propagate ClassCastException from the delegate");
        assertEquals("cannot cast", ex.getMessage());
    }

    @Test
    void testAddWhenDelegateThrowsIllegalArgumentException(@Mock Set<Integer> mockDelegate) throws Exception {
        // given
        when(mockDelegate.add(any())).thenThrow(new IllegalArgumentException("bad value"));
        injectDelegate(mockDelegate);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> set.add(-5), "add() should propagate IllegalArgumentException from the delegate");
        assertEquals("bad value", ex.getMessage());
    }

    @Test
    void testAddWhenInternalDelegateIsNull_throwsNullPointerException() throws Exception {
        // given: replace the internal set with null
        injectDelegate(null);
        // when / then
        NullPointerException ex = assertThrows(NullPointerException.class, () -> set.add(99), "add() should throw NullPointerException when internal delegate is null");
        // The exact message is JVM‑dependent; we only assert the type.
        assertNotNull(ex.getMessage());
    }
}
