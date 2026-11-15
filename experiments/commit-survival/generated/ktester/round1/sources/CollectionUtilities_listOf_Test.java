package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Array;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_listOf_Test {

    /* --------------------------------------------------------------------- */
    @FunctionalInterface
    private interface Executable {

        void execute() throws Throwable;
    }

    /**
     * Supplies a stream of {@link Executable} actions that attempt to mutate
     * the list (or its views/iterators). Each action is expected to throw
     * {@link UnsupportedOperationException}.
     */
    private static Stream<Executable> mutatingOperationsProvider() {
        List<String> base = CollectionUtilities.listOf("a", "b", "c");
        return Stream.of(// direct List mutations
        () -> base.add("x"), () -> base.addAll(Arrays.asList("x", "y")), () -> base.remove("a"), () -> base.removeAll(Collections.singleton("b")), () -> base.retainAll(Collections.singleton("c")), () -> base.clear(), () -> base.set(0, "z"), () -> base.replaceAll(e -> e + "z"), // iterator mutations
        () -> {
            Iterator<String> it = base.iterator();
            it.next();
            // should fail
            it.remove();
        }, // ListIterator mutations
        () -> {
            ListIterator<String> lit = base.listIterator();
            lit.next();
            // should fail
            lit.add("new");
        }, () -> {
            ListIterator<String> lit = base.listIterator();
            lit.next();
            // should fail
            lit.set("new");
        }, // subList mutations
        () -> {
            List<String> sub = base.subList(0, 2);
            sub.add("new");
        }, () -> {
            List<String> sub = base.subList(0, 2);
            sub.remove(0);
        }, () -> {
            List<String> sub = base.subList(0, 2);
            sub.set(0, "new");
        }, () -> {
            List<String> sub = base.subList(0, 2);
            sub.clear();
        }, () -> {
            List<String> sub = base.subList(0, 2);
            sub.addAll(Collections.singleton("new"));
        }, // sorting via Collections.sort (which uses List#set)
        () -> Collections.sort(base));
    }

    @Test
    void listOf_nullArray_returnsEmptyUnmodifiableList() {
        // GIVEN
        String[] items = null;
        // WHEN
        List<String> result = CollectionUtilities.listOf(items);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Result should be an empty list");
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"), "The returned list must be unmodifiable");
    }

    @Test
    void listOf_emptyArray_returnsEmptyUnmodifiableList() {
        // GIVEN
        Integer[] items = new Integer[0];
        // WHEN
        List<Integer> result = CollectionUtilities.listOf(items);
        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be empty for an empty array");
        assertThrows(UnsupportedOperationException.class, () -> result.remove(0), "The returned list must be unmodifiable");
    }

    @Test
    void listOf_multipleElements_returnsUnmodifiableListWithSameOrder() {
        // GIVEN
        String a = "alpha";
        String b = "beta";
        String c = "gamma";
        String[] items = new String[] { a, b, c };
        // WHEN
        List<String> result = CollectionUtilities.listOf(items);
        // THEN
        assertEquals(3, result.size(), "Result should contain three elements");
        assertEquals(Arrays.asList(a, b, c), result, "Elements must preserve the original order");
        assertThrows(UnsupportedOperationException.class, () -> result.clear(), "The returned list must be unmodifiable");
    }

    @Test
    void listOf_varArgsConvenience_overloadWorksSameAsArray() {
        // GIVEN / WHEN
        List<Integer> result = CollectionUtilities.listOf(1, 2, 3, 4);
        // THEN
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
        assertThrows(UnsupportedOperationException.class, () -> result.set(0, 99), "The returned list must be unmodifiable");
    }

    @Test
    void listOf_doesNotExposeInternalMutableList() {
        // GIVEN
        List<String> result = CollectionUtilities.listOf("one", "two");
        // WHEN / THEN
        // Trying to cast to a mutable implementation should fail – we verify that
        // the list is indeed unmodifiable by attempting a mutation.
        assertThrows(UnsupportedOperationException.class, () -> ((ArrayList<String>) result).add("three"), "The list must not be a mutable ArrayList instance");
    }

    @Test
    void listOf_nullArray_returnsSharedEmptyInstance() {
        List<Object> first = CollectionUtilities.listOf((Object[]) null);
        List<Object> second = CollectionUtilities.listOf((Object[]) null);
        assertSame(first, second, "Two calls with null var‑args must return the exact same empty list instance");
    }

    @Test
    void listOf_emptyArray_returnsSharedEmptyInstance() {
        List<Object> first = CollectionUtilities.listOf();
        List<Object> second = CollectionUtilities.listOf();
        assertSame(first, second, "Two calls with no arguments must return the same shared empty list");
    }

    @Test
    void listOf_singleElement_containsThatElementAndIsUnmodifiable() {
        List<String> result = CollectionUtilities.listOf("alpha");
        assertEquals(1, result.size());
        assertEquals("alpha", result.get(0));
        assertThrows(UnsupportedOperationException.class, () -> result.add("beta"));
    }

    @Test
    void listOf_nullElementInsideArray_isPreserved() {
        List<String> result = CollectionUtilities.listOf(null, "beta");
        assertEquals(2, result.size());
        assertNull(result.get(0));
        assertEquals("beta", result.get(1));
        assertThrows(UnsupportedOperationException.class, () -> result.remove(0));
    }

    @Test
    void listOf_externalArrayModificationDoesNotAffectReturnedList() {
        // GIVEN an array that will be mutated after the call
        String[] source = new String[] { "x", "y" };
        List<String> result = CollectionUtilities.listOf(source);
        // mutate the original array
        source[0] = "changed";
        // THEN the list must still contain the original values
        assertEquals(Arrays.asList("x", "y"), result);
    }

    @Test
    void listOf_duplicateElements_arePreservedInOrder() {
        List<String> result = CollectionUtilities.listOf("dup", "dup", "unique");
        assertEquals(Arrays.asList("dup", "dup", "unique"), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("another"));
    }

    @Test
    void listOf_mixedNumericTypes_arePreservedInOrder() {
        // Integer and Double share Number as a common super‑type
        List<Number> result = CollectionUtilities.listOf(10, 5.5);
        assertEquals(Arrays.asList(10, 5.5), result);
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    void listOf_singleNullElement_isUnmodifiable() {
        List<Object> result = CollectionUtilities.listOf((Object) null);
        assertEquals(1, result.size());
        assertNull(result.get(0));
        assertThrows(UnsupportedOperationException.class, () -> result.set(0, "x"));
    }

    @Test
    void listOf_twoCallsWithSameNonEmptyContent_returnDifferentInstances() {
        List<String> first = CollectionUtilities.listOf("a");
        List<String> second = CollectionUtilities.listOf("a");
        assertNotSame(first, second, "Non‑empty results must be distinct objects");
    }

    @ParameterizedTest
    @MethodSource("mutatingOperationsProvider")
    void listOf_allMutatingOperationsThrowUnsupportedOperationException(Executable mutatingAction) {
        assertThrows(UnsupportedOperationException.class, mutatingAction, "Mutating operation on the unmodifiable list or its views must throw UnsupportedOperationException");
    }
}
