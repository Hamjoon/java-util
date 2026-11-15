package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Iterator;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableSet_clear_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed = false;

    private Supplier<Boolean> sealedSupplier = () -> sealed;

    class CustomRuntimeException extends RuntimeException {

        CustomRuntimeException(String msg) {
            super(msg);
        }
    }

    class CustomRuntimeException extends RuntimeException {

        CustomRuntimeException(String msg) {
            super(msg);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SealableSet for each test and populate it with a few values
        set = new SealableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(null);
        // Ensure the set starts in an un‑sealed state
        sealed = false;
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and guarantee no cross‑test contamination
        set = null;
    }

    private static void setUnderlyingSetToNull(SealableSet<?> sealableSet) throws Exception {
        Field f = SealableSet.class.getDeclaredField("set");
        f.setAccessible(true);
        f.set(sealableSet, null);
    }

    @Test
    void clearWhenUnsealedEmptiesSet() {
        // GIVEN – set already contains three elements (added in @BeforeEach)
        // WHEN
        set.clear();
        // THEN
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertEquals(0, set.size(), "Size should be 0 after clear()");
        Iterator<Integer> it = set.iterator();
        assertFalse(it.hasNext(), "Iterator should have no elements after clear()");
    }

    @Test
    void clearWhenSealedThrowsUnsupportedOperationException() {
        // GIVEN – seal the set
        sealed = true;
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, set::clear, "clear() must throw when the set is sealed");
    }

    @Test
    void clearOnAlreadyEmptySetDoesNotThrow() {
        // GIVEN – empty the set first
        // now the set is empty
        set.clear();
        assertTrue(set.isEmpty());
        // WHEN – call clear again while still unsealed
        assertDoesNotThrow(() -> set.clear(), "Calling clear() on an already empty, unsealed set should not throw");
        // THEN – still empty
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods for reflection‑based manipulation of the private   *
     *  'set' field inside SealableSet.                                      *
     * --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static <T> void setUnderlyingSet(SealableSet<T> sealableSet, Set<T> newSet) throws Exception {
        Field f = SealableSet.class.getDeclaredField("set");
        f.setAccessible(true);
        f.set(sealableSet, newSet);
    }

    @Test
    void clearWhenSealedThrowsIllegalStateException() {
        // GIVEN – seal the set
        sealed = true;
        // WHEN / THEN
        assertThrows(IllegalStateException.class, set::clear, "clear() must throw IllegalStateException when the set is sealed");
    }

    @Test
    void clearWhenSealedDoesNotChangeSealedState() {
        // GIVEN – seal the set
        sealed = true;
        // WHEN – first clear attempt throws
        assertThrows(IllegalStateException.class, set::clear);
        // THEN – the sealed flag is still true (another clear should fail again)
        assertThrows(IllegalStateException.class, set::clear, "Sealed state must remain true after a failed clear()");
    }

    @Test
    void clearPropagatesExceptionFromUnderlyingSet() throws Exception {
        // GIVEN – a mock Set that throws UnsupportedOperationException on clear()
        @SuppressWarnings("unchecked")
        Set<Integer> mockSet = mock(Set.class);
        doThrow(new UnsupportedOperationException("mock clear failure")).when(mockSet).clear();
        // Replace the internal set with the mock
        setUnderlyingSet(set, mockSet);
        // WHEN / THEN – the exception from the underlying set is propagated
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, set::clear);
        assertEquals("mock clear failure", ex.getMessage());
    }

    @Test
    void clearPropagatesCustomRuntimeException() throws Exception {
        // GIVEN – a custom RuntimeException implementation
        class CustomRuntimeException extends RuntimeException {

            CustomRuntimeException(String msg) {
                super(msg);
            }
        }
        // Mock Set that throws the custom exception on clear()
        @SuppressWarnings("unchecked")
        Set<Integer> mockSet = mock(Set.class);
        doThrow(new CustomRuntimeException("custom failure")).when(mockSet).clear();
        setUnderlyingSet(set, mockSet);
        // WHEN / THEN – the custom exception is propagated unchanged
        CustomRuntimeException ex = assertThrows(CustomRuntimeException.class, set::clear);
        assertEquals("custom failure", ex.getMessage());
    }

    @Test
    void clearWithExternalReferenceToUnderlyingSet() throws Exception {
        // GIVEN – an external HashSet that we will share with SealableSet
        Set<Integer> external = new HashSet<>();
        external.add(1);
        external.add(2);
        external.add(3);
        // Inject the external set into the SealableSet instance
        setUnderlyingSet(set, external);
        // Verify that both views see the same elements
        assertEquals(3, set.size());
        assertEquals(3, external.size());
        // WHEN – clear via SealableSet
        set.clear();
        // THEN – both collections are empty
        assertTrue(set.isEmpty(), "SealableSet should be empty after clear()");
        assertTrue(external.isEmpty(), "External reference should also be empty after clear()");
    }

    @Test
    void clearAfterObtainingIterator() {
        // GIVEN – obtain an iterator before clearing
        Iterator<Integer> iterator = set.iterator();
        assertTrue(iterator.hasNext(), "Iterator should have elements before clear");
        // WHEN – clear the set
        set.clear();
        // THEN – the previously obtained iterator reports no remaining elements
        assertFalse(iterator.hasNext(), "Iterator obtained before clear() should report no elements after clear()");
    }

    @Test
    void clearWhenUnderlyingSetIsNullThrowsNullPointerException() throws Exception {
        // GIVEN – replace the internal set with null via reflection
        setUnderlyingSetToNull(set);
        // WHEN / THEN – clear() attempts to invoke clear on null and throws NPE
        assertThrows(NullPointerException.class, set::clear, "clear() should throw NullPointerException when underlying set is null");
    }

    @Test
    void clearPropagatesUnsupportedOperationExceptionFromUnmodifiableSet() throws Exception {
        // GIVEN – an unmodifiable set that throws UnsupportedOperationException on clear()
        Set<Integer> unmodifiable = Collections.unmodifiableSet(new HashSet<>(Set.of(5, 6, 7)));
        // Inject the unmodifiable set via reflection
        setUnderlyingSet(set, unmodifiable);
        // WHEN / THEN – the exception is propagated
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, set::clear);
        // The exact message is JDK‑dependent; we only assert the type here.
        assertNotNull(ex);
    }
}
