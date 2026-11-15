package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class SealableSet_toArray_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed = false;

    private Supplier<Boolean> sealedSupplier = () -> sealed;

    /* -------------------------------------------------------------
     *  Per‑test setup/teardown – we create a fresh SealableSet for
     *  each test and populate it with a known collection of values.
     * ------------------------------------------------------------- */
    @BeforeEach
    void setupBeforeEach() {
        set = new SealableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(null);
        // ensure mutable state for each test
        sealed = false;
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing specific to clean up per test
        set = null;
    }

    /* -------------------------------------------------------------
     *  Helper – replace the private final 'set' field using reflection.
     * ------------------------------------------------------------- */
    private static void setInternalBacking(SealableSet<?> target, Set<?> newBacking) throws Exception {
        Field field = SealableSet.class.getDeclaredField("set");
        field.setAccessible(true);
        // Remove final modifier if present (Java 12+ may require additional steps,
        // but for test purposes setAccessible is sufficient).
        field.set(target, newBacking);
    }

    @Test
    void toArrayReturnsArrayContainingAllElements() {
        Object[] array = set.toArray();
        // The array must contain exactly three elements (10, 20, null) in any order
        assertEquals(3, array.length, "Array length should be 3");
        List<Object> list = Arrays.asList(array);
        assertTrue(list.contains(10), "Array should contain 10");
        assertTrue(list.contains(20), "Array should contain 20");
        assertTrue(list.contains(null), "Array should contain null");
    }

    @Test
    void toArrayWorksWhenSetIsSealed() {
        // Seal the set – read‑only operations must still succeed
        sealed = true;
        Object[] array = set.toArray();
        assertEquals(3, array.length, "Sealed set should still return an array of size 3");
        List<Object> list = Arrays.asList(array);
        assertTrue(list.contains(10));
        assertTrue(list.contains(20));
        assertTrue(list.contains(null));
    }

    @Test
    void toArrayOnEmptySetReturnsEmptyArray() {
        SealableSet<String> empty = new SealableSet<>(sealedSupplier);
        Object[] array = empty.toArray();
        assertNotNull(array, "Returned array must not be null");
        assertEquals(0, array.length, "Empty set should produce an empty array");
    }

    @Test
    void toArrayReturnsCopyNotReference() {
        Object[] array = set.toArray();
        // Mutate the returned array – the underlying set must remain unchanged
        array[0] = "tampered";
        // Re‑fetch the array from the set
        Object[] fresh = set.toArray();
        // The original element (10, 20 or null) must still be present, not "tampered"
        assertFalse(Arrays.asList(fresh).contains("tampered"), "Modifying the returned array must not affect the set");
    }

    @Test
    void toArrayDoesNotExposeInternalImplementationDetails() {
        // The underlying implementation uses a ConcurrentSet; we verify that callers
        // cannot obtain a reference to that internal collection via toArray().
        Object[] array = set.toArray();
        // Attempt to cast the array to a Set – should fail with ClassCastException
        assertThrows(ClassCastException.class, () -> {
            @SuppressWarnings("unused")
            Set<?> bogus = (Set<?>) array;
        });
    }

    @Test
    @DisplayName("Empty Set – toArray returns a new empty array each call")
    void emptySetReturnsNewEmptyArrayEachCall() {
        SealableSet<String> empty = new SealableSet<>(sealedSupplier);
        Object[] first = empty.toArray();
        Object[] second = empty.toArray();
        assertEquals(0, first.length, "First array should be empty");
        assertEquals(0, second.length, "Second array should be empty");
        assertNotSame(first, second, "Each call must return a fresh array instance");
    }

    @Test
    @DisplayName("Non‑empty Set – array reflects iterator order")
    void nonEmptySetArrayPreservesIterationOrder() throws Exception {
        // Use a LinkedHashSet to guarantee iteration order: 1, 2, 3
        Set<Integer> backing = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        // Replace the internal 'set' field via reflection
        setInternalBacking(set, backing);
        Object[] array = set.toArray();
        assertArrayEquals(new Object[] { 1, 2, 3 }, array, "toArray must return elements in the same order as the iterator");
    }

    @Test
    @DisplayName("Non‑empty Set – each toArray call returns a distinct array")
    void nonEmptySetArrayIdentity() {
        Object[] first = set.toArray();
        Object[] second = set.toArray();
        assertNotSame(first, second, "Two calls must produce different array instances");
        assertArrayEquals(first, second, "Contents of the two arrays must be equal");
    }

    @Test
    @DisplayName("Non‑empty Set – array is immutable after backing set changes")
    void arrayUnaffectedBySubsequentModifications() {
        Object[] snapshot = set.toArray();
        // Modify the underlying set after the snapshot was taken
        set.add(99);
        // The previously captured array must not contain the newly added element
        assertFalse(Arrays.asList(snapshot).contains(99), "Original array must not reflect later modifications to the set");
    }

    @Test
    @DisplayName("Non‑empty Set – null element is preserved in the array")
    void arrayContainsNullElement() {
        // The default set created in @BeforeEach already contains a null
        Object[] array = set.toArray();
        assertTrue(Arrays.asList(array).contains(null), "Array must contain the null element");
    }

    @Test
    @DisplayName("Null backing set – toArray throws NullPointerException")
    void toArrayWithNullBackingSetThrowsNPE() throws Exception {
        // Force the private 'set' field to null via reflection
        setInternalBacking(set, null);
        assertThrows(NullPointerException.class, () -> set.toArray(), "Calling toArray when the internal set is null should throw NPE");
    }

    @Test
    @DisplayName("Concurrent modification – exception propagates")
    void toArrayPropagatesConcurrentModificationException() throws Exception {
        // Mock a Set that throws CME when toArray() is invoked
        @SuppressWarnings("unchecked")
        Set<Integer> mockSet = mock(Set.class);
        when(mockSet.toArray()).thenThrow(new ConcurrentModificationException("simulated CME"));
        setInternalBacking(set, mockSet);
        assertThrows(ConcurrentModificationException.class, () -> set.toArray(), "toArray must propagate ConcurrentModificationException from the backing set");
    }

    @Test
    @DisplayName("Backing set throws UnsupportedOperationException – propagates")
    void toArrayPropagatesUnsupportedOperationException() throws Exception {
        // Mock a Set whose toArray() throws UOE
        @SuppressWarnings("unchecked")
        Set<Integer> mockSet = mock(Set.class);
        when(mockSet.toArray()).thenThrow(new UnsupportedOperationException("unsupported"));
        setInternalBacking(set, mockSet);
        assertThrows(UnsupportedOperationException.class, () -> set.toArray(), "toArray must propagate UnsupportedOperationException from the backing set");
    }
}
