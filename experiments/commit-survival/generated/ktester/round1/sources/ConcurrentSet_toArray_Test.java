package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_toArray_Test {

    /**
     * Instance under test – created fresh for each test method.
     */
    private ConcurrentSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor – it creates an empty, thread‑safe set.
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly necessary.
        set = null;
    }

    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// empty set
        Arguments.of("empty set", Collections.emptyList(), Collections.emptyList()), // only non‑null elements
        Arguments.of("only non‑null elements", Arrays.asList("apple", "banana"), Arrays.asList("apple", "banana")), // single null element
        Arguments.of("single null element", Collections.singletonList(null), Collections.singletonList(null)), // multiple null elements
        Arguments.of("multiple null elements", Arrays.asList(null, null), Arrays.asList(null, null)), // mix of null and non‑null
        Arguments.of("mix of null and non‑null", Arrays.asList("apple", null, "banana"), Arrays.asList("apple", null, "banana")), // single non‑null element
        Arguments.of("single non‑null element", Collections.singletonList("single"), Collections.singletonList("single")));
    }

    @Test
    void toArray_returnsEmptyArrayWhenSetIsEmpty() {
        // GIVEN – an empty set (created in @BeforeEach)
        // WHEN
        Object[] result = set.toArray();
        // THEN
        assertNotNull(result, "Resulting array must not be null");
        assertEquals(0, result.length, "Array length should be 0 for an empty set");
    }

    @Test
    void toArray_containsAllElementsWhenNoNullsPresent() {
        // GIVEN
        Collection<Integer> values = Arrays.asList(1, 2, 3, 4);
        set.addAll(values);
        // WHEN
        Object[] array = set.toArray();
        // THEN
        assertEquals(values.size(), array.length, "Array length must match set size");
        List<Object> asList = Arrays.asList(array);
        assertTrue(asList.containsAll(values), "Array must contain all added elements");
    }

    @Test
    void toArray_correctlyReplacesSentinelWithNull() {
        // GIVEN – add a null together with a non‑null value.
        set.addAll(Arrays.asList("a", null, "b"));
        // WHEN
        Object[] array = set.toArray();
        // THEN
        assertEquals(3, array.length, "Array should have three entries");
        List<Object> asList = Arrays.asList(array);
        assertTrue(asList.contains(null), "Array must contain the null element");
        assertTrue(asList.containsAll(Arrays.asList("a", "b")), "Array must contain the non‑null elements");
    }

    @Test
    void toArray_genericVersionBehavesIdentically() {
        // GIVEN
        ConcurrentSet<String> stringSet = new ConcurrentSet<>();
        stringSet.addAll(Arrays.asList("x", null, "y"));
        // WHEN – request a typed array of the exact size.
        String[] typedArray = stringSet.toArray(new String[0]);
        // THEN
        assertEquals(3, typedArray.length, "Typed array length must match set size");
        List<String> asList = Arrays.asList(typedArray);
        assertTrue(asList.contains(null), "Typed array must contain null");
        assertTrue(asList.containsAll(Arrays.asList("x", "y")), "Typed array must contain the non‑null elements");
    }

    @Test
    void toArray_returnsNewArrayInstanceEachCall() {
        // GIVEN
        set.addAll(Arrays.asList(10, 20));
        // WHEN
        Object[] first = set.toArray();
        Object[] second = set.toArray();
        // THEN
        assertNotSame(first, second, "Each call to toArray() should return a new array instance");
        // Ensure content is still correct.
        assertArrayEquals(first, second, "Both arrays should contain the same elements");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("branchCoverageProvider")
    void toArray_branchCoverage(String description, List<Object> input, List<Object> expected) {
        // GIVEN
        set.addAll(input);
        // WHEN
        Object[] result = set.toArray();
        // THEN
        assertEquals(expected.size(), result.length, () -> "Array length must match expected size for case: " + description);
        List<Object> resultList = Arrays.asList(result);
        // Order is not guaranteed – verify that each expected element appears the correct number of times.
        for (Object exp : new HashSet<>(expected)) {
            long expectedCount = expected.stream().filter(Objects::equals).filter(e -> Objects.equals(e, exp)).count();
            long actualCount = resultList.stream().filter(Objects::equals).filter(e -> Objects.equals(e, exp)).count();
            assertEquals(expectedCount, actualCount, () -> "Element [" + exp + "] count mismatch for case: " + description);
        }
    }

    @Test
    void toArray_snapshotReflectsStateAtInvocationEvenWithConcurrentAdds() throws Exception {
        // GIVEN – a set with a few initial elements.
        set.addAll(Arrays.asList("init1", "init2"));
        // Latch to coordinate the background thread.
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        // Background thread that will add many elements after we have started toArray().
        exec.submit(() -> {
            try {
                // wait until main thread signals that toArray() has been called
                startLatch.await();
                // Add a bunch of new elements.
                for (int i = 0; i < 1000; i++) {
                    set.add("bg-" + i);
                }
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // WHEN – invoke toArray() while the background thread is about to add more items.
        // let the background thread know we are about to call toArray()
        startLatch.countDown();
        // this should capture only the initial elements
        Object[] snapshot = set.toArray();
        // Wait for the background thread to finish its additions.
        doneLatch.await();
        exec.shutdownNow();
        // THEN – the snapshot must contain exactly the elements that existed before the concurrent adds.
        List<Object> snapshotList = Arrays.asList(snapshot);
        assertEquals(2, snapshotList.size(), "Snapshot length must equal the number of elements that were present before concurrent modifications");
        assertTrue(snapshotList.containsAll(Arrays.asList("init1", "init2")), "Snapshot must contain the original elements");
        // The final set size should be larger (initial + background adds).
        assertTrue(set.size() > snapshotList.size(), "After background adds the set size must be greater than the snapshot size");
    }

    @Test
    void toArray_genericVersion_throwsClassCastExceptionWhenElementTypeMismatched() throws Exception {
        // GIVEN – a ConcurrentSet<String> but we inject an Integer via reflection.
        ConcurrentSet<String> stringSet = new ConcurrentSet<>();
        // Access the private field 'set' that backs the ConcurrentSet.
        Field internalSetField = ConcurrentSet.class.getDeclaredField("set");
        internalSetField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Object> internalSet = (Set<Object>) internalSetField.get(stringSet);
        // Insert an Integer directly, bypassing the add() method that would replace nulls with the sentinel.
        internalSet.add(42);
        // WHEN / THEN – requesting a String[] should cause a ClassCastException because the Integer cannot be stored in a String[].
        assertThrows(ClassCastException.class, () -> stringSet.toArray(new String[0]), "Calling toArray(T[]) with an incompatible element type must propagate ClassCastException");
    }
}
