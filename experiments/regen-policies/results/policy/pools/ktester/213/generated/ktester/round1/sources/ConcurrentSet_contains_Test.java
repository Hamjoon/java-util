package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_contains_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    private ConcurrentSet<Number> numberSet;

    private ConcurrentSet<Double> doubleSet;

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
        numberSet = new ConcurrentSet<>();
        doubleSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each set is cleared after a test – helps with memory when many tests run
        intSet.clear();
        stringSet.clear();
        numberSet.clear();
        doubleSet.clear();
    }

    /**
     * Provides arguments for the branch‑coverage group.
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>a description (for readability in test reports)</li>
     *   <li>the object to query with {@code contains}</li>
     *   <li>the expected outcome – either {@code true}/{@code false} or an exception class</li>
     * </ul>
     */
    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// 1. present integer
        Arguments.of("present integer 5", 5, true, null), // 2. absent integer
        Arguments.of("absent integer 10", 10, false, null), // 3. null argument – for Integer set we expect false (null not stored)
        Arguments.of("null argument on Integer set", null, false, null), // 4. incompatible type – String on Integer set => ClassCastException
        Arguments.of("String on Integer set", "string", null, ClassCastException.class), // 5. Number set with BigDecimal – should be true because 1 (Integer) equals 1.0 (BigDecimal)
        Arguments.of("BigDecimal 1.0 on Number set", new BigDecimal("1.0"), true, null));
    }

    /**
     * Provides arguments for the “additional cases” group.
     */
    private static Stream<Arguments> additionalCasesProvider() {
        return Stream.of(// ArrayList on Integer set -> ClassCastException
        Arguments.of("ArrayList on Integer set", new ArrayList<>(), ClassCastException.class), // Plain Object on Integer set -> ClassCastException
        Arguments.of("Object on Integer set", new Object(), ClassCastException.class), // Long value on Integer set -> ClassCastException
        Arguments.of("Long 5L on Integer set", 5L, ClassCastException.class), // Double.NaN on Double set -> false (not stored)
        Arguments.of("Double.NaN on Double set", Double.NaN, false, null), // String "5" on Integer set -> ClassCastException
        Arguments.of("String \"5\" on Integer set", "5", ClassCastException.class), // BigInteger on Integer set -> ClassCastException
        Arguments.of("BigInteger 5 on Integer set", new BigInteger("5"), ClassCastException.class), // int[] on Integer set -> ClassCastException
        Arguments.of("int[] {5} on Integer set", new int[] { 5 }, ClassCastException.class), // Explicit null on Integer set -> false
        Arguments.of("Explicit null on Integer set", null, false, null));
    }

    @Test
    void containsExistingElement() {
        assertTrue(intSet.add(42), "Add should return true for a new element");
        assertTrue(intSet.contains(42), "Set should contain the element that was added");
        assertEquals(1, intSet.size(), "Size should be 1 after adding a single element");
    }

    @Test
    void containsNonExistingElement() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertFalse(intSet.contains(99), "Set should not contain an element that was never added");
    }

    @Test
    void containsAfterRemoval() {
        intSet.addAll(Arrays.asList(5, 6, 7));
        assertTrue(intSet.remove(6), "Removal of an existing element should return true");
        assertFalse(intSet.contains(6), "Set should no longer contain the removed element");
    }

    @Test
    void containsNullElement() {
        // ConcurrentSet supports null via a sentinel; test that behaviour
        assertTrue(stringSet.add(null), "Adding null should succeed");
        assertTrue(stringSet.contains(null), "Set should report that it contains null");
        assertEquals(1, stringSet.size(), "Size should be 1 after adding null");
    }

    @Test
    void containsNullWhenNotPresent() {
        stringSet.addAll(Arrays.asList("a", "b"));
        assertFalse(stringSet.contains(null), "Set should not contain null when it was never added");
    }

    @Test
    void containsWithDifferentObjectType() {
        intSet.addAll(Arrays.asList(10, 20));
        // Passing a String should simply return false – no ClassCastException should be thrown
        assertFalse(intSet.contains("10"), "Contains should return false when the type differs");
    }

    @Test
    void containsAllUsesContainsInternally() {
        // Verify that containsAll works correctly, which indirectly tests contains for each element
        intSet.addAll(Arrays.asList(1, 2, 3, 4));
        assertTrue(intSet.containsAll(Arrays.asList(2, 3)), "containsAll should succeed when all elements are present");
        assertFalse(intSet.containsAll(Arrays.asList(2, 5)), "containsAll should fail when any element is missing");
    }

    @Test
    void containsAfterClear() {
        intSet.addAll(Arrays.asList(8, 9));
        intSet.clear();
        assertTrue(intSet.isEmpty(), "Set should be empty after clear");
        assertFalse(intSet.contains(8), "Set should not contain any element after clear");
    }

    @Test
    void containsWithEmptySet() {
        assertFalse(intSet.contains(1), "Empty set should not contain any element");
    }

    @Test
    void containsWhenUnderlyingSetIsConcurrent() throws InterruptedException {
        // Simple concurrency sanity check – multiple threads add then query contains
        final int threads = 4;
        final int perThread = 500;
        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int base = i * perThread;
            workers[i] = new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    intSet.add(base + j);
                }
            });
            workers[i].start();
        }
        for (Thread t : workers) {
            t.join();
        }
        // After all additions, every added element should be reported as present
        for (int i = 0; i < threads * perThread; i++) {
            assertTrue(intSet.contains(i), "Set should contain element added concurrently: " + i);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("branchCoverageProvider")
    void testContainsBranchCoverage(String description, Object query, Boolean expectedResult, Class<? extends Throwable> expectedException) {
        // Choose the appropriate set based on the expected type of the query.
        if (query instanceof BigDecimal) {
            // Number set: add an Integer 1, then query with BigDecimal 1.0
            assertTrue(numberSet.add(1), "Setup: add Integer 1 to Number set");
            if (expectedException != null) {
                assertThrows(expectedException, () -> numberSet.contains(query), description);
            } else {
                assertEquals(expectedResult, numberSet.contains(query), description);
            }
        } else if (query instanceof String && expectedException == ClassCastException.class) {
            // Integer set: we expect a ClassCastException
            if (expectedException != null) {
                assertThrows(expectedException, () -> intSet.contains(query), description);
            }
        } else {
            // Default: Integer set
            // Pre‑populate the set when we expect a true result
            if (Boolean.TRUE.equals(expectedResult) && query != null) {
                assertTrue(intSet.add((Integer) query), "Setup: add element to Integer set");
            }
            if (expectedException != null) {
                assertThrows(expectedException, () -> intSet.contains(query), description);
            } else {
                assertEquals(expectedResult, intSet.contains(query), description);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalCasesProvider")
    void testContainsAdditionalCases(String description, Object query, Object expectedOrException, Class<? extends Throwable> maybeException) {
        // Helper to decide which set to use based on the expected type
        Set<?> targetSet;
        if (query instanceof Double) {
            targetSet = doubleSet;
            // For the Double.NaN case we need a regular Double stored first
            if (maybeException == null && Boolean.FALSE.equals(expectedOrException)) {
                assertTrue(doubleSet.add(1.23), "Setup: add a regular Double to the Double set");
            }
        } else {
            targetSet = intSet;
        }
        // If the test case expects an exception, assertThrows
        if (maybeException != null) {
            assertThrows(maybeException, () -> targetSet.contains(query), description);
        } else {
            // Normal boolean expectation
            boolean expected = (Boolean) expectedOrException;
            assertEquals(expected, targetSet.contains(query), description);
        }
    }
}
