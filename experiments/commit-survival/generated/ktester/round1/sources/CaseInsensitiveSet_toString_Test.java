package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

class CaseInsensitiveSet_toString_Test {

    private CaseInsensitiveSet<String> set;

    /* --------------------------------------------------------------
     *  ELEMENT TOSTRING EXCEPTION PROPAGATION
     * -------------------------------------------------------------- */
    private static class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("Bad toString");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for each test to guarantee isolation.
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse.
        set = null;
    }

    /* --------------------------------------------------------------
     *  BASIC SCENARIOS
     * -------------------------------------------------------------- */
    private static Stream<Arguments> basicScenariosProvider() {
        return Stream.of(// empty set
        Arguments.of(Collections.emptyList(), "[]", "When the set is empty, toString() should return \"[]\""), // single element
        Arguments.of(List.of("Apple"), "[Apple]", "After adding a single element \"Apple\""), // three distinct elements – natural (case‑insensitive) order
        Arguments.of(List.of("Apple", "Banana", "Cherry"), "[Apple, Banana, Cherry]", "Three distinct elements should appear in case‑insensitive sorted order"), // case‑insensitive duplicate
        Arguments.of(List.of("Apple", "apple"), "[Apple]", "Adding \"Apple\" then \"apple\" must not create a duplicate"), // null element (implementation permits a null key)
        Arguments.of(List.of((String) null), "[null]", "Adding null should be reflected as \"[null]\""));
    }

    /* --------------------------------------------------------------
     *  ADDALL BEHAVIOR
     * -------------------------------------------------------------- */
    private static Stream<Arguments> addAllProvider() {
        return Stream.of(Arguments.of(List.of("Dog", "cat", "Elephant"), "[cat, Dog, Elephant]", "addAll should order elements case‑insensitively"), Arguments.of(List.of("Apple", "apple", "Banana"), "[Apple, Banana]", "Duplicates differing only by case must be ignored"));
    }

    @Test
    void toString_ShouldReturnEmptyBrackets_ForEmptySet() {
        assertEquals("[]", set.toString(), "toString() of an empty CaseInsensitiveSet must be \"[]\"");
    }

    @Test
    void toString_ShouldContainSingleElement() {
        set.add("Hello");
        assertEquals("[Hello]", set.toString(), "toString() should contain the single inserted element preserving its case");
    }

    @Test
    void toString_ShouldBeCaseInsensitiveAndPreserveFirstCase() {
        set.add("a");
        // duplicate ignoring case, should be ignored
        set.add("A");
        assertEquals(1, set.size(), "Set size must be 1 because \"A\" is a case‑insensitive duplicate of \"a\"");
        assertEquals("[a]", set.toString(), "toString() must reflect the original case of the first inserted element");
    }

    @Test
    void toString_ShouldReturnElementsInCaseInsensitiveSortedOrder() {
        // Underlying map is a CaseInsensitiveMap which uses a TreeMap with a case‑insensitive comparator.
        set.add("b");
        set.add("A");
        set.add("c");
        // Expected order after case‑insensitive sorting: A, b, c (original case retained)
        assertEquals("[A, b, c]", set.toString(), "toString() must present elements in case‑insensitive sorted order while preserving original case");
    }

    @Test
    void toString_WithCollectionConstructor_ShouldReflectContents() {
        // "x" appears twice
        List<String> source = Arrays.asList("x", "Y", "x");
        CaseInsensitiveSet<String> setFromCollection = new CaseInsensitiveSet<>(source);
        // Verify size reflects case‑insensitive uniqueness
        assertEquals(2, setFromCollection.size(), "Duplicate \"x\" (different case) should be collapsed to a single entry");
        // The toString representation must contain both distinct elements.
        String result = setFromCollection.toString();
        assertTrue(result.contains("x") && result.contains("Y"), "toString() should contain both \"x\" and \"Y\" (order may vary)");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicScenariosProvider")
    void toString_BasicScenarios(List<String> elements, String expected, String description) {
        elements.forEach(e -> set.add(e));
        assertEquals(expected, set.toString(), description);
    }

    /* --------------------------------------------------------------
     *  AFTER REMOVALS
     * -------------------------------------------------------------- */
    @Test
    void toString_AfterRemoval_RemovesElement() {
        set.addAll(List.of("Apple", "Banana", "Cherry"));
        assertTrue(set.remove("Banana"), "Removal should succeed");
        assertEquals("[Apple, Cherry]", set.toString(), "After removing \"Banana\" the remaining elements must be shown");
    }

    @Test
    void toString_AfterRemoval_CaseInsensitiveDelete() {
        set.add("Apple");
        assertTrue(set.remove("apple"), "Removal should be case‑insensitive");
        assertEquals("[]", set.toString(), "Set should be empty after removing \"apple\" (different case)");
    }

    /* --------------------------------------------------------------
     *  AFTER CLEAR
     * -------------------------------------------------------------- */
    @Test
    void toString_AfterClear_ReturnsEmpty() {
        set.addAll(List.of("Apple", "Banana"));
        set.clear();
        assertEquals("[]", set.toString(), "clear() must result in an empty representation");
    }

    /* --------------------------------------------------------------
     *  ORDERING WITH MIXED CASE
     * -------------------------------------------------------------- */
    @Test
    void toString_MixedCaseOrdering() {
        // insertion order is irrelevant – the underlying map sorts case‑insensitively
        set.add("banana");
        set.add("Apple");
        set.add("apricot");
        assertEquals("[Apple, apricot, banana]", set.toString(), "Elements must be ordered case‑insensitively while preserving original case");
    }

    /* --------------------------------------------------------------
     *  NON‑STRING ELEMENTS
     * -------------------------------------------------------------- */
    @Test
    void toString_NonStringElements_SortedNaturally() {
        CaseInsensitiveSet<Integer> intSet = new CaseInsensitiveSet<>();
        intSet.addAll(List.of(5, 3, 10));
        assertEquals("[3, 5, 10]", intSet.toString(), "Integer elements should be sorted by their natural ordering");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("addAllProvider")
    void toString_AddAllBehavior(List<String> source, String expected, String description) {
        set.addAll(source);
        assertEquals(expected, set.toString(), description);
    }

    /* --------------------------------------------------------------
     *  NULL HANDLING EDGE CASES
     * -------------------------------------------------------------- */
    @Test
    void toString_NullAddedTwice_OnlyOneNullPresent() {
        set.add(null);
        // second addition should be ignored
        set.add(null);
        assertEquals("[null]", set.toString(), "Only a single null entry must be present");
    }

    @Test
    void toString_NullAndElement_RemoveNull_OnlyElementRemains() {
        set.add(null);
        set.add("Apple");
        assertTrue(set.remove(null), "Null removal must succeed");
        assertEquals("[Apple]", set.toString(), "After removing null, only \"Apple\" should remain");
    }

    /* --------------------------------------------------------------
     *  CONCURRENT MODIFICATION EXCEPTION
     * -------------------------------------------------------------- */
    @Test
    void toString_ConcurrentModification_ShouldThrow() throws Exception {
        // Populate the set with a few elements.
        set.addAll(List.of("Alpha", "Beta", "Gamma"));
        // Latch to synchronize the two threads.
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch modifyLatch = new CountDownLatch(1);
        ExecutorService exec = Executors.newFixedThreadPool(2);
        // Thread that will invoke toString().
        Callable<Void> toStringTask = () -> {
            // wait until the modifier is ready
            startLatch.await();
            try {
                // The call may or may not throw; we loop until it does.
                while (true) {
                    try {
                        // may throw ConcurrentModificationException
                        set.toString();
                    } catch (ConcurrentModificationException e) {
                        // Expected – re‑throw to be caught by the test harness.
                        throw e;
                    }
                }
            } finally {
                // signal that we are done
                modifyLatch.countDown();
            }
        };
        // Thread that mutates the set while the other thread is iterating.
        Runnable modifierTask = () -> {
            // allow toString() to start
            startLatch.countDown();
            // Small pause to increase the chance of overlap.
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
            // concurrent modification
            set.add("Delta");
        };
        Future<Void> toStringFuture = exec.submit(toStringTask);
        exec.submit(modifierTask);
        // Expect a ConcurrentModificationException within a reasonable time.
        assertThrows(ConcurrentModificationException.class, () -> {
            try {
                toStringFuture.get(2, TimeUnit.SECONDS);
            } catch (ExecutionException ee) {
                // unwrap the cause
                throw ee.getCause();
            }
        });
        exec.shutdownNow();
    }

    @Test
    void toString_ElementThrowsRuntimeException_Propagates() {
        CaseInsensitiveSet<Object> objSet = new CaseInsensitiveSet<>();
        objSet.add(new BadToString());
        RuntimeException ex = assertThrows(RuntimeException.class, objSet::toString, "Exception from element's toString() must propagate");
        assertEquals("Bad toString", ex.getMessage());
    }
}
