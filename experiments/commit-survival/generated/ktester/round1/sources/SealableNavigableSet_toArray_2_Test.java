package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class SealableNavigableSet_toArray_2_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor that only needs a Supplier is sufficient for the
        // toArray tests – we are not exercising the “sealed” behaviour here,
        // but we keep the supplier so that the class can be instantiated.
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate the set with a null element and three integers.
        // The underlying ConcurrentNavigableSetNullSafe implementation
        // preserves the natural ordering (null is considered the smallest).
        set.add(null);
        set.add(30);
        set.add(10);
        set.add(20);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the sealed flag so that later tests start from a clean state.
        sealedState = false;
    }

    /**
     * Supplies the arguments for {@link #testToArrayVariousScenarios(Object[])}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> toArrayArguments() {
        return Stream.of(// Empty input array – new array of correct size must be allocated.
        arguments(new Integer[0], new Integer[] { null, 10, 20, 30 }, false, null), // Larger array (size 5) – same instance, sentinel null after last element.
        arguments(new Integer[5], new Integer[] { null, 10, 20, 30, null }, true, null), // Exact size array – same instance, fully populated.
        arguments(new Integer[4], new Integer[] { null, 10, 20, 30 }, true, null), // Incompatible component type – expect ArrayStoreException.
        arguments(new String[0], null, false, ArrayStoreException.class), // Null array reference – expect NullPointerException.
        arguments(null, null, false, NullPointerException.class), // Generic Object array – should succeed.
        arguments(new Object[0], new Object[] { null, 10, 20, 30 }, false, null), // Pre‑filled array of exact size – contents are overwritten.
        arguments(new Integer[] { 1, 2, 3, 4 }, new Integer[] { null, 10, 20, 30 }, true, null), // Pre‑filled null array larger than set – sentinel null after elements.
        arguments(new Integer[] { null, null, null, null, null }, new Integer[] { null, 10, 20, 30, null }, true, null));
    }

    @Test
    void testToArrayExactSizeArray() {
        Integer[] supplied = new Integer[4];
        Integer[] result = set.toArray(supplied);
        // The method should return the same array instance when it is big enough.
        assertSame(supplied, result, "Returned array should be the supplied array when it fits");
        // Expected order: null, 10, 20, 30 (natural ordering, null considered smallest)
        assertArrayEquals(new Integer[] { null, 10, 20, 30 }, result, "Array contents must reflect the set ordering");
    }

    @Test
    void testToArraySmallerArrayCreatesNewOne() {
        // smaller than the set size (4)
        Integer[] supplied = new Integer[2];
        Integer[] result = set.toArray(supplied);
        // A new array must be created because the supplied one is too small.
        assertNotSame(supplied, result, "Returned array should be a new array when supplied array is too small");
        assertEquals(4, result.length, "New array length must match the set size");
        assertArrayEquals(new Integer[] { null, 10, 20, 30 }, result);
    }

    @Test
    void testToArrayLargerArrayKeepsReferenceAndFillsRemainingWithNull() {
        // larger than the set size
        Integer[] supplied = new Integer[6];
        Integer[] result = set.toArray(supplied);
        // The same array instance should be returned.
        assertSame(supplied, result, "Returned array should be the supplied array when it is larger than needed");
        // First four positions hold the set elements in order.
        assertArrayEquals(new Integer[] { null, 10, 20, 30, null, null }, result, "Remaining positions after the elements must be null");
    }

    @Test
    void testToArrayWithSupertypeArray() {
        Number[] supplied = new Number[4];
        Number[] result = set.toArray(supplied);
        // Same instance because the supplied array fits.
        assertSame(supplied, result);
        assertArrayEquals(new Number[] { null, 10, 20, 30 }, result);
    }

    @Test
    void testToArrayWhenSealedDoesNotAffectResult() {
        // Seal the set – the toArray operation must still succeed.
        sealedState = true;
        Integer[] supplied = new Integer[4];
        Integer[] result = assertDoesNotThrow(() -> set.toArray(supplied));
        assertSame(supplied, result);
        assertArrayEquals(new Integer[] { null, 10, 20, 30 }, result);
    }

    @Test
    void testToArrayWithIteratorAfterModification() {
        // Obtain an iterator, modify the set through the iterator (allowed because not sealed),
        // then call toArray to verify the snapshot reflects the modification.
        Iterator<Integer> it = set.iterator();
        // points to null
        it.next();
        // remove null
        it.remove();
        // let the method allocate the right size
        Integer[] arr = set.toArray(new Integer[0]);
        assertArrayEquals(new Integer[] { 10, 20, 30 }, arr);
    }

    @ParameterizedTest(name = "[{index}] supplied array class={0.getClass().getSimpleName()}, length={0 == null ? \"null\" : 0.length}")
    @MethodSource("toArrayArguments")
    void testToArrayVariousScenarios(Object suppliedArray, Object[] expectedContent, boolean expectSameInstance, Class<? extends Throwable> expectedException) throws Exception {
        if (expectedException != null) {
            // The contract states that a null array or an incompatible component type
            // must cause an exception.
            assertThrows(expectedException, () -> set.toArray(suppliedArray));
            return;
        }
        // Normal execution path – invoke the method via reflection to avoid raw‑type warnings.
        @SuppressWarnings("unchecked")
        Object result = set.toArray((Object[]) suppliedArray);
        if (expectSameInstance) {
            assertSame(suppliedArray, result, "Method should return the supplied array instance");
        } else {
            assertNotSame(suppliedArray, result, "Method should allocate a new array when supplied one is too small");
        }
        assertArrayEquals(expectedContent, (Object[]) result, "Array contents must match the expected ordering and sentinel nulls");
    }

    @Test
    void testToArrayWithNullUnderlyingNavSetThrowsNPE() throws Exception {
        // Create a normal instance first.
        SealableNavigableSet<Integer> broken = new SealableNavigableSet<>(sealedSupplier);
        // Use reflection to null‑out the private navSet field.
        Field f = SealableNavigableSet.class.getDeclaredField("navSet");
        f.setAccessible(true);
        f.set(broken, null);
        Integer[] a = new Integer[0];
        assertThrows(NullPointerException.class, () -> broken.toArray(a), "If the internal navSet is null, toArray must throw NPE");
    }

    @Test
    void testToArrayArrayStoreExceptionWhenElementTypesMismatch() throws Exception {
        // Build a set that actually holds Strings (raw type to bypass generics).
        @SuppressWarnings("unchecked")
        SealableNavigableSet rawSet = new SealableNavigableSet(sealedSupplier);
        NavigableSet<String> stringBacking = new TreeSet<>();
        stringBacking.add("alpha");
        stringBacking.add("beta");
        // Inject the string backing set via reflection.
        Field f = SealableNavigableSet.class.getDeclaredField("navSet");
        f.setAccessible(true);
        f.set(rawSet, stringBacking);
        // Supplying an Integer[] should cause an ArrayStoreException.
        Integer[] a = new Integer[0];
        assertThrows(ArrayStoreException.class, () -> rawSet.toArray(a), "Storing a String into an Integer[] must raise ArrayStoreException");
    }

    @Test
    void testToArrayEmptySetReturnsEmptyArray() {
        // Create a set with no elements.
        SealableNavigableSet<Integer> empty = new SealableNavigableSet<>(sealedSupplier);
        // Ensure it is really empty.
        assertTrue(empty.isEmpty());
        Integer[] result = empty.toArray(new Integer[0]);
        assertEquals(0, result.length, "Empty set should produce an empty array");
    }
}
