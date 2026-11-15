package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableList_clear_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure each test starts with a fresh, unsealed list containing three elements.
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse.
        list = null;
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> dummyProvider() {
        // placeholder; can be expanded for future data‑driven tests
        return Stream.empty();
    }

    @Test
    void clearRemovesAllElementsWhenUnsealed() {
        // Preconditions
        assertFalse(list.isEmpty());
        assertEquals(3, list.size());
        // Exercise
        list.clear();
        // Verify
        assertTrue(list.isEmpty(), "List should be empty after clear()");
        assertEquals(0, list.size(), "Size should be zero after clear()");
        assertFalse(list.contains(10));
        assertFalse(list.contains(20));
        assertFalse(list.contains(30));
    }

    @Test
    void clearThrowsWhenSealed() {
        // Arrange – seal the list
        sealedState = true;
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> list.clear(), "clear() must throw when the list is sealed");
    }

    @Test
    void clearIsIdempotentOnEmptyList() {
        // First clear to make the list empty
        list.clear();
        assertTrue(list.isEmpty());
        // Second clear should not throw and the list remains empty
        assertDoesNotThrow(() -> list.clear(), "Calling clear() on an already empty list should not throw");
        assertTrue(list.isEmpty(), "List should remain empty after second clear()");
    }

    @Test
    void clearThenAddWorksAccordingToSealState() {
        // Clear while unsealed
        list.clear();
        assertTrue(list.isEmpty());
        // Adding after clear while still unsealed should succeed
        list.add(100);
        assertEquals(1, list.size());
        assertTrue(list.contains(100));
        // Seal the list and attempt another clear – should fail
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.clear(), "clear() must fail when the list is sealed");
        // Unseal again and clear – should succeed
        sealedState = false;
        assertDoesNotThrow(() -> list.clear());
        assertTrue(list.isEmpty());
    }

    @Test
    void clearSingleElementNonSealed() {
        // Arrange: list with a single element
        // start from empty
        list.clear();
        list.add(42);
        assertEquals(1, list.size());
        // Act
        list.clear();
        // Assert
        assertTrue(list.isEmpty(), "Single element list should be empty after clear()");
        assertEquals(0, list.size());
    }

    @Test
    void clearWithNullElements() {
        // Arrange: list containing null values
        list.clear();
        list.add(null);
        list.add(null);
        assertEquals(2, list.size());
        assertTrue(list.contains(null));
        // Act
        list.clear();
        // Assert
        assertTrue(list.isEmpty(), "List with null elements should be empty after clear()");
        assertEquals(0, list.size());
    }

    @Test
    void clearUnderlyingListThrowsUnsupportedOperationException() {
        // Arrange: an unmodifiable backing list
        List<Integer> backing = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(1, 2, 3)));
        SealableList<Integer> unmodifiableList = new SealableList<>(backing, sealedSupplier);
        // Act & Assert
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, unmodifiableList::clear, "clear() should propagate UnsupportedOperationException from the underlying list");
        // Verify that the list content is unchanged
        assertEquals(3, unmodifiableList.size(), "Size must remain unchanged after exception");
        assertTrue(unmodifiableList.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    void clearSealedFlagUnchangedAfterAttemptedClear() {
        // Arrange: seal the list with a single element
        list.clear();
        list.add(99);
        sealedState = true;
        assertTrue(sealedState, "Precondition: list must be sealed");
        // Act
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> list.clear(), "clear() must throw IllegalStateException when sealed");
        // Assert: sealed flag still true and element unchanged
        assertTrue(sealedState, "Sealed flag should remain true after failed clear");
        assertEquals(1, list.size(), "List size must be unchanged after failed clear");
        assertTrue(list.contains(99));
    }

    @Test
    void clearUnderlyingListIsNull() throws Exception {
        // Arrange: create a normal SealableList then inject null via reflection
        SealableList<Integer> nullBacked = new SealableList<>(new ArrayList<>(), sealedSupplier);
        Field listField = SealableList.class.getDeclaredField("list");
        listField.setAccessible(true);
        // force internal list to null
        listField.set(nullBacked, null);
        // Act & Assert
        assertThrows(NullPointerException.class, nullBacked::clear, "clear() should throw NullPointerException when internal list is null");
    }
}
