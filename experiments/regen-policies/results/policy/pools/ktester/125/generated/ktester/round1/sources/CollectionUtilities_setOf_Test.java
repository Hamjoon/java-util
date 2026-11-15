package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

class CollectionUtilities_setOf_Test {

    private static Stream<Arguments> nullOrEmptyInputProvider() {
        return Stream.of(// null var‑args
        Arguments.of((Object) null), // empty array
        Arguments.of((Object) new Object[0]));
    }

    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of(42), Arguments.of("single"));
    }

    private static Stream<Arguments> multipleElementsProvider() {
        return Stream.of(Arguments.of(new Object[] { "alpha", "beta", "gamma" }), Arguments.of(new Object[] { 1, 2, 3, 4 }));
    }

    @Test
    void setOf_NullArray_ReturnsEmptyUnmodifiableSet() {
        Set<String> result = CollectionUtilities.setOf((String[]) null);
        assertTrue(result.isEmpty(), "Result should be empty for null input");
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"), "Resulting set must be unmodifiable");
    }

    @Test
    void setOf_EmptyArray_ReturnsEmptyUnmodifiableSet() {
        Set<Integer> result = CollectionUtilities.setOf();
        assertTrue(result.isEmpty(), "Result should be empty for empty var‑args");
        assertThrows(UnsupportedOperationException.class, () -> result.add(1), "Resulting set must be unmodifiable");
        // The implementation returns a shared constant; the same instance should be reused.
        Set<Integer> another = CollectionUtilities.setOf();
        assertSame(result, another, "Empty set instances should be identical (shared constant)");
    }

    @Test
    void setOf_WithElements_ReturnsUnmodifiableSetPreservingOrder() {
        Set<String> result = CollectionUtilities.setOf("first", "second", "third");
        assertEquals(3, result.size(), "Set should contain three distinct elements");
        assertTrue(result.contains("first"));
        assertTrue(result.contains("second"));
        assertTrue(result.contains("third"));
        // Verify iteration order matches insertion order (LinkedHashSet semantics)
        Iterator<String> it = result.iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> result.remove("first"));
    }

    @Test
    void setOf_WithDuplicates_EliminatesDuplicates() {
        Set<String> result = CollectionUtilities.setOf("dup", "dup", "unique");
        assertEquals(2, result.size(), "Duplicates should be collapsed");
        assertTrue(result.contains("dup"));
        assertTrue(result.contains("unique"));
    }

    @Test
    void setOf_WithNullElements_AllowsNulls() {
        Set<String> result = CollectionUtilities.setOf(null, "value", null);
        assertEquals(2, result.size(), "Null should be treated as a regular element");
        assertTrue(result.contains(null), "Result should contain null");
        assertTrue(result.contains("value"));
    }

    /**
     * Group: nullOrEmptyInput – verifies that both {@code null} and an empty
     *  var‑args array return the shared unmodifiable empty set instance.
     */
    @ParameterizedTest(name = "[{index}] items={0}")
    @MethodSource("nullOrEmptyInputProvider")
    void setOf_NullOrEmptyInput_ReturnsSharedEmptySet(Object[] items) throws Exception {
        Set<?> result = CollectionUtilities.setOf(items);
        assertTrue(result.isEmpty(), "Result should be empty");
        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> result.add(new Object()), "Returned set must be unmodifiable");
        // Obtain the private static field via reflection to compare identity
        Field field = CollectionUtilities.class.getDeclaredField("unmodifiableEmptySet");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<?> sharedEmpty = (Set<?>) field.get(null);
        assertSame(sharedEmpty, result, "Empty result must be the shared constant instance");
    }

    /**
     * Group: singleElement – a single non‑null element should be present,
     *  the set must be immutable and have size 1.
     */
    @ParameterizedTest(name = "[{index}] element={0}")
    @MethodSource("singleElementProvider")
    void setOf_SingleElement_ReturnsImmutableSet(Object element) {
        Set<?> result = CollectionUtilities.setOf(element);
        assertEquals(1, result.size(), "Size must be 1");
        assertTrue(result.contains(element), "Result must contain the supplied element");
        // Immutability check
        assertThrows(UnsupportedOperationException.class, () -> result.remove(element), "Mutating operation must fail");
    }

    /**
     * Group: multipleElements – distinct elements are kept in insertion order.
     */
    @ParameterizedTest(name = "[{index}] items={0}")
    @MethodSource("multipleElementsProvider")
    void setOf_MultipleElements_PreservesOrder(Object[] items) {
        @SuppressWarnings("unchecked")
        Set<Object> result = CollectionUtilities.setOf(items);
        assertEquals(items.length, result.size(), "All distinct elements must be present");
        // Verify order by iterating
        Iterator<Object> it = result.iterator();
        for (Object expected : items) {
            assertEquals(expected, it.next(), "Insertion order must be preserved");
        }
        // Immutability
        assertThrows(UnsupportedOperationException.class, () -> result.add(new Object()), "Set must be unmodifiable");
    }

    /**
     * Group: duplicateElements – duplicates are removed, first occurrence order kept.
     */
    @Test
    void setOf_DuplicateElements_RemovesDuplicatesPreservesFirstOrder() {
        Integer[] input = { 1, 2, 2, 3, 1 };
        Set<Integer> result = CollectionUtilities.setOf(input);
        assertEquals(3, result.size(), "Duplicates should be collapsed");
        List<Integer> expectedOrder = Arrays.asList(1, 2, 3);
        assertIterableEquals(expectedOrder, result, "Order must reflect first occurrences");
    }

    /**
     * Group: nullElementInArray – null values are allowed and appear once,
     *  preserving the position of the first null.
     */
    @Test
    void setOf_NullElements_AllowedAndOrdered() {
        String[] input = { null, "foo", null };
        Set<String> result = CollectionUtilities.setOf(input);
        assertEquals(2, result.size(), "Null should be treated as a regular element");
        assertTrue(result.contains(null), "Result must contain null");
        assertTrue(result.contains("foo"), "Result must contain 'foo'");
        // Verify order: null first, then "foo"
        Iterator<String> it = result.iterator();
        assertNull(it.next(), "First element should be null");
        assertEquals("foo", it.next(), "Second element should be 'foo'");
    }

    /**
     * Group: unmodifiableBehavior – any mutating operation must throw.
     */
    @Test
    void setOf_UnmodifiableBehavior_ThrowsOnMutation() {
        Set<String> result = CollectionUtilities.setOf("immutable");
        assertThrows(UnsupportedOperationException.class, () -> result.add("another"), "add() must fail");
        assertThrows(UnsupportedOperationException.class, () -> result.remove("immutable"), "remove() must fail");
        assertThrows(UnsupportedOperationException.class, () -> result.clear(), "clear() must fail");
    }

    /**
     * Group: differentInstances – two calls with same non‑empty arguments must
     *  return distinct objects (reference inequality) but equal content.
     */
    @Test
    void setOf_DifferentInstances_NonEmptyCallsReturnDistinctObjects() {
        Set<Integer> first = CollectionUtilities.setOf(10, 20);
        Set<Integer> second = CollectionUtilities.setOf(10, 20);
        assertNotSame(first, second, "Non‑empty calls must create new Set instances");
        assertEquals(first, second, "Content equality must hold");
    }

    /**
     * Group: emptySetIdentity – empty var‑args must return the exact static field
     *  instance (reference equality).
     */
    @Test
    void setOf_EmptySetIdentity_ReturnsStaticConstant() throws Exception {
        Set<?> result = CollectionUtilities.setOf();
        Field field = CollectionUtilities.class.getDeclaredField("unmodifiableEmptySet");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<?> sharedEmpty = (Set<?>) field.get(null);
        assertSame(sharedEmpty, result, "Empty set must be the shared static constant");
    }

    /**
     * Group: typeInferenceWithSuperclass – generic inference should produce a
     *  Set&lt;Number&gt; when the var‑args contain mixed numeric types.
     */
    @Test
    void setOf_TypeInferenceWithSuperclass_ReturnsSetOfNumber() {
        // The compiler infers Number as the common super type.
        Set<Number> result = CollectionUtilities.setOf(1, 2.5, 3L);
        assertEquals(3, result.size(), "All three numbers must be present");
        assertTrue(result.containsAll(Arrays.asList(1, 2.5, 3L)), "Result must contain the supplied numeric values");
        // Verify order of insertion
        Iterator<Number> it = result.iterator();
        assertEquals(1, it.next());
        assertEquals(2.5, it.next());
        assertEquals(3L, it.next());
    }
}
