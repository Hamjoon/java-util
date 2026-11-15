package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_toString_Test {

    /**
     * The instance under test.
     */
    private SealableNavigableSet<Integer> set;

    /**
     * Simple mutable flag used by the supplier to indicate the sealed state.
     */
    private volatile boolean sealedState = false;

    /**
     * Supplier that returns the current value of {@code sealedState}.
     */
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SealableNavigableSet for each test and populate it.
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy.
        set = null;
        sealedState = false;
    }

    private static Stream<Arguments> dummyProvider() {
        return Stream.empty();
    }

    @Test
    void toStringMatchesUnderlyingNavigableSet() {
        // Build a regular TreeSet with the same elements – TreeSet implements NavigableSet.
        NavigableSet<Integer> reference = new TreeSet<>();
        reference.add(10);
        reference.add(20);
        reference.add(30);
        // Create a SealableNavigableSet that wraps the reference set.
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(reference, sealedSupplier);
        // The string representations must be identical.
        assertEquals(reference.toString(), wrapper.toString(), "SealabeNavigableSet.toString() should delegate to the wrapped NavigableSet");
    }

    @Test
    void toStringReflectsAddedElements() {
        // Initial representation.
        String before = set.toString();
        // Add a new element.
        set.add(40);
        String after = set.toString();
        assertNotEquals(before, after, "toString() should change after adding an element");
        assertTrue(after.contains("40"), "New element must appear in the string representation");
    }

    @Test
    void toStringReflectsRemovedElements() {
        // Ensure the element exists first.
        assertTrue(set.contains(20));
        // Capture representation before removal.
        String before = set.toString();
        // Remove the element.
        set.remove(20);
        String after = set.toString();
        assertNotEquals(before, after, "toString() should change after removing an element");
        assertFalse(after.contains("20"), "Removed element must not appear in the string representation");
    }

    @Test
    void toStringWhenEmpty() {
        // Remove all elements.
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        // Expected representation of an empty collection.
        assertEquals("[]", set.toString(), "Empty SealableNavigableSet should produce \"[]\" as its string representation");
    }

    @Test
    void toStringIsUnaffectedBySealedState() {
        // Capture representation while mutable.
        String mutable = set.toString();
        // Switch to sealed mode.
        sealedState = true;
        // The string representation must stay the same.
        String sealed = set.toString();
        assertEquals(mutable, sealed, "Sealed state must not influence the result of toString()");
    }

    @Test
    void emptySetToString() {
        SealableNavigableSet<String> empty = new SealableNavigableSet<>(new TreeSet<>(), sealedSupplier);
        assertEquals("[]", empty.toString(), "Empty underlying NavigableSet should produce \"[]\"");
    }

    @Test
    void nonEmptySetToString() {
        NavigableSet<String> ref = new TreeSet<>();
        ref.add("a");
        ref.add("b");
        ref.add("c");
        SealableNavigableSet<String> wrapper = new SealableNavigableSet<>(ref, sealedSupplier);
        assertEquals("[a, b, c]", wrapper.toString(), "toString() must match the underlying set's representation");
    }

    @Test
    void customComparatorSetToString() {
        NavigableSet<Integer> descending = new TreeSet<>(Comparator.reverseOrder());
        descending.addAll(Arrays.asList(1, 2, 3));
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(descending, sealedSupplier);
        assertEquals("[3, 2, 1]", wrapper.toString(), "Custom comparator order must be reflected in the string");
    }

    @Test
    void underlyingSetThrowsRuntimeException() {
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> mockSet = mock(NavigableSet.class);
        when(mockSet.toString()).thenThrow(new RuntimeException("boom"));
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(mockSet, sealedSupplier);
        RuntimeException ex = assertThrows(RuntimeException.class, wrapper::toString, "RuntimeException from underlying set must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void setWithNullElementsToString() {
        NavigableSet<String> withNull = new TreeSet<>(Comparator.nullsFirst(Comparator.naturalOrder()));
        withNull.add(null);
        withNull.add("a");
        withNull.add("b");
        SealableNavigableSet<String> wrapper = new SealableNavigableSet<>(withNull, sealedSupplier);
        assertEquals("[null, a, b]", wrapper.toString(), "Null element must appear in the string representation");
    }

    @Test
    void singleElementSetToString() {
        NavigableSet<Integer> single = new TreeSet<>();
        single.add(42);
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(single, sealedSupplier);
        assertEquals("[42]", wrapper.toString(), "Singleton set should be represented as \"[42]\"");
    }

    @Test
    void largeSetToString() {
        NavigableSet<Integer> large = new TreeSet<>();
        for (int i = 1; i <= 10; i++) {
            large.add(i);
        }
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(large, sealedSupplier);
        assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", wrapper.toString(), "Large set should be represented correctly");
    }

    @Test
    void immutableUnderlyingSetToString() {
        NavigableSet<String> mutable = new TreeSet<>();
        mutable.addAll(Arrays.asList("x", "y"));
        NavigableSet<String> unmodifiable = Collections.unmodifiableNavigableSet(mutable);
        SealableNavigableSet<String> wrapper = new SealableNavigableSet<>(unmodifiable, sealedSupplier);
        assertEquals("[x, y]", wrapper.toString(), "String representation must work with an unmodifiable underlying set");
    }

    @Test
    void underlyingSetWithCustomToString() {
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> mockSet = mock(NavigableSet.class);
        when(mockSet.toString()).thenReturn("CustomSetString");
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(mockSet, sealedSupplier);
        assertEquals("CustomSetString", wrapper.toString(), "Custom toString() from underlying set must be returned directly");
    }

    @Test
    void nullUnderlyingSetThrowsNPE() {
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, wrapper::toString, "Calling toString() on a wrapper with null underlying set must throw NPE");
    }

    @Test
    void concurrentModificationDuringToString() throws Exception {
        NavigableSet<Integer> concurrentSet = new TreeSet<>();
        for (int i = 0; i < 1000; i++) {
            concurrentSet.add(i);
        }
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(concurrentSet, sealedSupplier);
        Thread modifier = new Thread(() -> {
            try {
                Thread.sleep(5);
                concurrentSet.add(1001);
            } catch (InterruptedException ignored) {
            }
        });
        modifier.start();
        boolean cmeSeen = false;
        for (int i = 0; i < 20 && !cmeSeen; i++) {
            try {
                wrapper.toString();
            } catch (ConcurrentModificationException e) {
                cmeSeen = true;
            }
        }
        modifier.join();
        assertTrue(cmeSeen, "ConcurrentModificationException should propagate when underlying set is modified during toString()");
    }

    @Test
    void underlyingSetThrowsIllegalStateException() {
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> mockSet = mock(NavigableSet.class);
        when(mockSet.toString()).thenThrow(new IllegalStateException("illegal"));
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(mockSet, sealedSupplier);
        IllegalStateException ex = assertThrows(IllegalStateException.class, wrapper::toString, "IllegalStateException must propagate");
        assertEquals("illegal", ex.getMessage());
    }

    @Test
    void recursiveSetCausesStackOverflowError() {
        ???;
        SealableNavigableSet<Object> wrapper = new SealableNavigableSet<>(recursive, sealedSupplier);
        assertThrows(StackOverflowError.class, wrapper::toString, "Recursive underlying set should cause StackOverflowError");
    }
}
