package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableList_lastIndexOf_Test {

    private static SealableList<Integer> list;

    private static volatile boolean sealedState;

    private static Supplier<Boolean> sealedSupplier;

    /**
     * Simple POJO with equality based on an identifier.
     */
    private static final class Custom {

        private final int id;

        Custom(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Custom))
                return false;
            return id == ((Custom) o).id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /**
     * equals() deliberately throws a RuntimeException.
     */
    private static final class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("Bad equals");
        }
    }

    /**
     * equals() deliberately throws an AssertionError.
     */
    private static final class BadEqualsError {

        @Override
        public boolean equals(Object o) {
            throw new AssertionError("Bad equals error");
        }
    }

    /**
     * equals() recursively calls itself leading to StackOverflowError.
     */
    private static final class BadEqualsStackOverflow {

        @Override
        public boolean equals(Object o) {
            // infinite recursion
            return this.equals(o);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise beyond the supplier
        sealedState = false;
        sealedSupplier = () -> sealedState;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh list for each test to guarantee isolation
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
        // duplicate to test “last” index behaviour
        list.add(20);
        // include a null element
        list.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mutable state; the list itself will be garbage‑collected after the test
        sealedState = false;
        list.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing special to clean up
        list = null;
        sealedSupplier = null;
    }

    /* ----------------------------------------------------------------------
     *  Basic scenarios – parameterised tests
     * ---------------------------------------------------------------------- */
    private static Stream<Arguments> basicScenarios() {
        return Stream.of(// Empty underlying list – any element returns -1
        Arguments.of(Collections.emptyList(), "any", -1, "Empty underlying list should return -1 for any searched element."), // Single element matching – index 0
        Arguments.of(Collections.singletonList("single"), "single", 0, "List with one matching element should return index 0."), // Duplicate elements – last occurrence at index 2
        Arguments.of(Arrays.asList("banana", "apple", "apple"), "apple", 2, "List [\"banana\",\"apple\",\"apple\"] – lastIndexOf should return the last occurrence at index 2."), // Nulls in the middle – last null at index 2
        Arguments.of(Arrays.asList("first", null, null), null, 2, "List [\"first\",null,null] – searching for null should return the last null at index 2."), // No nulls – searching for null returns -1
        Arguments.of(Arrays.asList("a", "b", "c"), null, -1, "List [\"a\",\"b\",\"c\"] with no nulls – searching for null should return -1."), // Different type – Integer in a String list returns -1
        Arguments.of(Arrays.asList("a", "b", "c"), 123, -1, "List [\"a\",\"b\",\"c\"] – searching for an object of different type (Integer) should return -1."), // Custom objects with same id – equals should match, last index 1
        Arguments.of(Arrays.asList(new Custom(1), new Custom(1), new Custom(2)), new Custom(1), 1, "List containing two Custom objects with same id – lastIndexOf should use equals and return the last matching index (1)."));
    }

    /* ----------------------------------------------------------------------
     *  Advanced scenarios – parameterised tests
     * ---------------------------------------------------------------------- */
    private static Stream<Arguments> advancedScenarios() {
        return Stream.of(// Multiple non‑consecutive occurrences – last index 4
        Arguments.of(Arrays.asList("x", "y", "x", "z", "x"), "x", 4, "List [\"x\",\"y\",\"x\",\"z\",\"x\"] – multiple non‑consecutive occurrences; lastIndexOf should return the final index 4."), // Distinct String instance equal by value – index 0
        Arguments.of(Arrays.asList("a", "b", "c"), new String("a"), 0, "List [\"a\",\"b\",\"c\"] – search with a distinct String instance that is equal by value; should return index 0."), // Null in the middle – index 1
        Arguments.of(Arrays.asList("first", null, "second"), null, 1, "List [\"first\",null,\"second\"] – null appears in the middle; lastIndexOf should return its index 1."), // Two nulls – last null at end index 3
        Arguments.of(Arrays.asList("first", null, "second", null), null, 3, "List [\"first\",null,\"second\",null] – two nulls, the last one at the end; lastIndexOf should return index 3."), // Element whose equals throws RuntimeException – exception propagation
        Arguments.of(Arrays.asList(new BadEquals(), "safe"), new BadEquals(), RuntimeException.class, "List contains an element whose equals() throws RuntimeException; invoking lastIndexOf should propagate that exception."), // Empty list – searching for null returns -1
        Arguments.of(Collections.emptyList(), null, -1, "Empty underlying list; searching for null should still return -1."));
    }

    @Test
    void lastIndexOf_existingElement_returnsCorrectIndex() {
        // The last occurrence of 20 is at index 3 (0‑based)
        assertEquals(3, list.lastIndexOf(20));
    }

    @Test
    void lastIndexOf_nonExistingElement_returnsMinusOne() {
        assertEquals(-1, list.lastIndexOf(99));
    }

    @Test
    void lastIndexOf_nullElement_returnsCorrectIndex() {
        // Null was added at index 4
        assertEquals(4, list.lastIndexOf(null));
    }

    @Test
    void lastIndexOf_whenSealed_behavesLikeReadOnly() {
        // make the list immutable
        sealedState = true;
        // lastIndexOf should still work and must not throw UnsupportedOperationException
        assertEquals(3, list.lastIndexOf(20));
        assertEquals(4, list.lastIndexOf(null));
        assertEquals(-1, list.lastIndexOf(100));
    }

    @Test
    void lastIndexOf_afterModifications_reflectsCurrentState() {
        // Remove the duplicate 20 and check the index updates
        // removes first occurrence (index 1)
        list.remove(Integer.valueOf(20));
        // now last 20 is at index 2
        assertEquals(2, list.lastIndexOf(20));
        // Add another 20 at the end
        list.add(20);
        assertEquals(5, list.lastIndexOf(20));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicScenarios")
    void lastIndexOf_basicScenarios(List<Object> initialElements, Object search, int expected, String description) {
        // Build a SealableList<Object> with the supplied elements
        @SuppressWarnings("unchecked")
        SealableList<Object> localList = new SealableList<>(new ArrayList<>(initialElements), sealedSupplier);
        assertEquals(expected, localList.lastIndexOf(search), description);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("advancedScenarios")
    void lastIndexOf_advancedScenarios(List<Object> initialElements, Object search, Object expectedOrException, String description) {
        @SuppressWarnings("unchecked")
        SealableList<Object> localList = new SealableList<>(new ArrayList<>(initialElements), sealedSupplier);
        if (expectedOrException instanceof Class) {
            // Expect an exception to be thrown
            Class<? extends Throwable> expectedException = (Class<? extends Throwable>) expectedOrException;
            assertThrows(expectedException, () -> localList.lastIndexOf(search), description);
        } else {
            int expected = (int) expectedOrException;
            assertEquals(expected, localList.lastIndexOf(search), description);
        }
    }

    @Test
    void lastIndexOf_nullInternalList_throwsNullPointerException() throws Exception {
        // Create a SealableList with a valid list first
        SealableList<Integer> faulty = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // Use reflection to set the private 'list' field to null
        Field listField = SealableList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(faulty, null);
        assertThrows(NullPointerException.class, () -> faulty.lastIndexOf(1), "SealableList instantiated with a null internal list should throw NullPointerException.");
    }

    @Test
    void lastIndexOf_underlyingListThrowsUnsupportedOperationException_propagates() {
        // Mock a List that throws UnsupportedOperationException on lastIndexOf
        @SuppressWarnings("unchecked")
        List<Integer> mockList = mock(List.class);
        when(mockList.lastIndexOf(any())).thenThrow(new UnsupportedOperationException("not supported"));
        SealableList<Integer> wrapper = new SealableList<>(mockList, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.lastIndexOf(1), "Underlying list's UnsupportedOperationException must be propagated.");
    }

    @Test
    void lastIndexOf_concurrentModification_propagatesConcurrentModificationException() throws Exception {
        // Use a custom List implementation that throws CME when iterated
        List<Integer> concurrentList = new ArrayList<>(Arrays.asList(1, 2, 3));
        SealableList<Integer> wrapper = new SealableList<>(concurrentList, sealedSupplier);
        // Start a thread that modifies the list while lastIndexOf is executing
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(() -> {
            // Small delay to increase chance of overlap
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
            concurrentList.add(4);
        });
        // The call should detect the concurrent modification via ArrayList's modCount
        assertThrows(ConcurrentModificationException.class, () -> wrapper.lastIndexOf(2), "Concurrent modification during lastIndexOf should propagate ConcurrentModificationException.");
        // ensure the modifier thread finishes
        future.get();
        exec.shutdownNow();
    }

    @Test
    void lastIndexOf_elementEqualsThrowsAssertionError_propagates() {
        List<Object> data = Arrays.asList(new BadEqualsError(), "ok");
        SealableList<Object> wrapper = new SealableList<>(new ArrayList<>(data), sealedSupplier);
        assertThrows(AssertionError.class, () -> wrapper.lastIndexOf(new BadEqualsError()), "AssertionError thrown from equals() must be propagated.");
    }

    @Test
    void lastIndexOf_elementEqualsThrowsStackOverflowError_propagates() {
        List<Object> data = Arrays.asList(new BadEqualsStackOverflow(), "ok");
        SealableList<Object> wrapper = new SealableList<>(new ArrayList<>(data), sealedSupplier);
        assertThrows(StackOverflowError.class, () -> wrapper.lastIndexOf(new BadEqualsStackOverflow()), "StackOverflowError thrown from equals() must be propagated.");
    }
}
