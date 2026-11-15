package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

// No mocks used, but kept from template
@ExtendWith(MockitoExtension.class)
class SystemUtilities_getEnvironmentVariables_Test {

    /**
     * Helper holder for parameterized test scenarios
     */
    private static class PredicateScenario {

        final Predicate<String> filter;

        final String description;

        // true when we expect the full environment map
        final boolean expectAll;

        PredicateScenario(Predicate<String> filter, String description, java.util.function.Predicate<Map<String, String>> expectation) {
            this.filter = filter;
            this.description = description;
            // For our simple scenarios we can infer expectation from the predicate itself
            this.expectAll = expectation.test(System.getenv());
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /* -----------------------------------------------------------------------
     * Parameterized test covering the four simple predicate behaviours:
     *   null, always true, always false, and subset (path).
     *   This demonstrates use of @MethodSource while still keeping the explicit
     *   tests above for readability.
     * ----------------------------------------------------------------------- */
    static Stream<PredicateScenario> predicateScenarios() {
        return Stream.of(new PredicateScenario(null, "null filter (all entries)", // filter null behaves like always true
        env -> true), new PredicateScenario(key -> true, "always true predicate", env -> true), new PredicateScenario(key -> false, "always false predicate", env -> false), new PredicateScenario(key -> key.toLowerCase().contains("path"), "contains \"path\" predicate", env -> env.keySet().stream().anyMatch(k -> k.toLowerCase().contains("path"))));
    }

    /**
     * Verify that when the filter is {@code null} the method returns a map
     * containing **all** environment variables.
     */
    @Test
    void testGetEnvironmentVariables_NoFilter_ReturnsAll() {
        Map<String, String> all = System.getenv();
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(null);
        // The result should contain exactly the same entries as System.getenv()
        assertEquals(all.size(), result.size(), "Result size should match the size of System.getenv()");
        assertTrue(result.entrySet().containsAll(all.entrySet()), "Result should contain every entry from System.getenv()");
    }

    /**
     * Verify that a predicate that always returns {@code false} yields an empty map.
     */
    @Test
    void testGetEnvironmentVariables_FilterRejectAll_ReturnsEmpty() {
        Predicate<String> rejectAll = key -> false;
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(rejectAll);
        assertTrue(result.isEmpty(), "Result should be empty when filter rejects all keys");
    }

    /**
     * Verify that a predicate that selects a subset of keys returns only those entries.
     * The test selects keys that start with a known prefix (e.g., "PATH" or "JAVA").
     */
    @Test
    void testGetEnvironmentVariables_FilterSelectSubset() {
        // Choose a prefix that is very likely to exist on any platform.
        // On Windows the variable is "PATH", on Unix/Linux/macOS it is also "PATH".
        // Fallback to "JAVA" if PATH is not present.
        String prefix = System.getenv().keySet().stream().filter(k -> k.equalsIgnoreCase("PATH")).findFirst().orElse("JAVA");
        Predicate<String> startsWithPrefix = key -> key.toUpperCase().startsWith(prefix.toUpperCase());
        Map<String, String> expected = System.getenv().entrySet().stream().filter(e -> startsWithPrefix.test(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(startsWithPrefix);
        assertEquals(expected.size(), result.size(), "Result size should match the number of entries that satisfy the predicate");
        assertTrue(result.keySet().containsAll(expected.keySet()), "Result should contain exactly the keys that satisfy the predicate");
        // Verify that values also match for the selected keys
        for (String key : expected.keySet()) {
            assertEquals(expected.get(key), result.get(key), "Value for key '" + key + "' should match the expected environment value");
        }
    }

    /**
     * Verify that the returned map preserves insertion order (LinkedHashMap behavior).
     * This is a lightweight check: we compare the iteration order of the result
     * with the order of the source stream, which is defined by System.getenv()
     * iteration order.
     */
    @Test
    void testGetEnvironmentVariables_ReturnsLinkedHashMapOrder() {
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(null);
        // The concrete type must be LinkedHashMap
        assertTrue(result instanceof LinkedHashMap, "Returned map should be an instance of LinkedHashMap");
        // Compare iteration order of first few entries with the source map
        // (System.getenv()'s entrySet iteration order is the source order)
        var originalIter = System.getenv().entrySet().iterator();
        var resultIter = result.entrySet().iterator();
        // Check the first 5 entries (or fewer if env has less)
        int checks = Math.min(5, System.getenv().size());
        for (int i = 0; i < checks; i++) {
            assertTrue(originalIter.hasNext() && resultIter.hasNext(), "Both iterators should have next element");
            var origEntry = originalIter.next();
            var resEntry = resultIter.next();
            assertEquals(origEntry.getKey(), resEntry.getKey(), "Key order should be preserved");
            assertEquals(origEntry.getValue(), resEntry.getValue(), "Value order should be preserved");
        }
    }

    /**
     * Verify that a predicate that always returns {@code true} yields the full map,
     * exercising the stream's filter branch.
     */
    @Test
    void testGetEnvironmentVariables_FilterAlwaysTrue_ReturnsAll() {
        Predicate<String> alwaysTrue = key -> true;
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(alwaysTrue);
        Map<String, String> expected = System.getenv();
        assertEquals(expected.size(), result.size(), "When predicate always true, size must equal System.getenv()");
        assertTrue(result.entrySet().containsAll(expected.entrySet()), "All entries must be present when predicate always true");
    }

    /**
     * Verify that a RuntimeException thrown by the predicate propagates out of the method.
     */
    @Test
    void testGetEnvironmentVariables_PredicateThrows_RuntimeExceptionPropagates() {
        Predicate<String> throwingPredicate = key -> {
            throw new RuntimeException("test");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> SystemUtilities.getEnvironmentVariables(throwingPredicate), "Exception from predicate should propagate");
        assertEquals("test", ex.getMessage(), "Exception message should be preserved");
    }

    @ParameterizedTest(name = "{index}: {0}.description")
    @MethodSource("predicateScenarios")
    void testGetEnvironmentVariables_Parameterized(PredicateScenario scenario) {
        Map<String, String> result = SystemUtilities.getEnvironmentVariables(scenario.filter);
        // Determine expected size based on the scenario's expectation function
        boolean expectAll = scenario.expectAll;
        if (scenario.filter == null) {
            // null filter returns all
            assertEquals(System.getenv().size(), result.size(), "Null filter must return all entries");
        } else if (expectAll) {
            assertEquals(System.getenv().size(), result.size(), scenario.description + " should return all entries");
        } else if (scenario.filter.test("anyKeyThatWillNeverMatch")) {
            // This branch is unreachable for our current scenarios
        } else {
            // For false‑filter case we expect empty map
            if (scenario.filter.test("dummy")) {
                // never happens
            } else {
                assertTrue(result.isEmpty(), scenario.description + " should produce an empty map");
            }
        }
    }
}
