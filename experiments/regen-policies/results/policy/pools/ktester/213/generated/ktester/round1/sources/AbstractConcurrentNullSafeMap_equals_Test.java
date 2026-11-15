package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_equals_Test {

    private TestMap<String, String> mapUnderTest;

    /**
     * A minimal concrete implementation of the abstract class for testing purposes.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Simple key class with proper {@code equals}/ {@code hashCode} for custom‑key tests.
     */
    private static final class Key {

        private final int id;

        Key(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Key) && ((Key) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "Key(" + id + ')';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mapUnderTest = new TestMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        mapUnderTest.clear();
    }

    private static Stream<Arguments> branchCoverageCases() {
        return Stream.of(// 1. same instance (this == o)
        Arguments.of("sameMap – should be true", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, // placeholder – will be replaced in the test body
        null, /* other */
        true), // 2. null argument
        Arguments.of("null argument – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, null, false), // 3. non‑Map object
        Arguments.of("non‑Map object – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, new Object(), false), // 4. size mismatch (tested map larger)
        Arguments.of("different size – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, Map.of("a", 1), false), // 5. missing key in other
        Arguments.of("missing key – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, Map.of("a", 1, "c", 3), false), // 6. identical content
        Arguments.of("identical map – should be true", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, Map.of("a", 1, "b", 2), true), // 7. same keys, different value for 'b'
        Arguments.of("different value for same key – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, Map.of("a", 1, "b", 99), false), // 8. null key & null value present in both maps
        Arguments.of("null key/value equal – should be true", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put(null, null);
            m.put("a", 1);
        }, new HashMap<>() {

            {
                put(null, null);
                put("a", 1);
            }
        }, true), // 9. null key with differing value
        Arguments.of("null key with different value – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put(null, null);
            m.put("a", 1);
        }, Map.of(null, 5, "a", 1), false)).map(args -> {
            // Special handling for the "sameMap" case where 'other' must be the map instance itself.
            if (args.get()[0].equals("sameMap – should be true")) {
                // The placeholder null will be replaced later in the test method.
                // No change needed here; the test method will detect the placeholder.
            }
            return args;
        });
    }

    private static Stream<Arguments> additionalScenariosCases() {
        return Stream.of(// ConcurrentHashMap with same entries
        Arguments.of("ConcurrentHashMap same entries – should be true", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, new ConcurrentHashMap<>(Map.of("a", 1, "b", 2)), true), // null key mapped to same non‑null value
        Arguments.of("null key same non‑null value – should be true", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put(null, 5);
            m.put("a", 1);
        }, new HashMap<>() {

            {
                put(null, 5);
                put("a", 1);
            }
        }, true), // non‑null key with null value
        Arguments.of("non‑null key with null value – should be true", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", null);
            m.put("b", 2);
        }, new HashMap<>() {

            {
                put("a", null);
                put("b", 2);
            }
        }, true), // other map larger than tested map
        Arguments.of("other map larger – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, Map.of("a", 1, "b", 2, "c", 3), false), // other map has null for existing key
        Arguments.of("other map null value for existing key – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, Map.of("a", 1, "b", null), false), // same size but completely different keys
        Arguments.of("completely different keys – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put("a", 1);
            m.put("b", 2);
        }, Map.of("c", 3, "d", 4), false), // custom key objects with logical equality
        Arguments.of("custom equal keys – should be true", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
            m.put(new Key(1), "v1");
            m.put(new Key(2), "v2");
        }, new HashMap<>() {

            {
                put(new Key(1), "v1");
                put(new Key(2), "v2");
            }
        }, true));
    }

    private static Stream<Arguments> exceptionScenariosCases() {
        return Stream.of(// null argument (already covered but kept for completeness)
        Arguments.of("null argument – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
        }, /* empty map */
        null, false), // non‑Map object (already covered)
        Arguments.of("non‑Map object – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
        }, /* empty map */
        new Object(), false), // both maps empty
        Arguments.of("both empty – should be true", (java.util.function.Consumer<TestMap<Object, Object>>) m -> {
        }, /* empty map */
        Map.of(), true), // both contain null key → null value
        Arguments.of("both contain null→null – should be true", (java.util.function.Consumer<TestMap<Object, Object>>) m -> m.put(null, null), new HashMap<>() {

            {
                put(null, null);
            }
        }, true), // null key value mismatch
        Arguments.of("null key value mismatch – should be false", (java.util.function.Consumer<TestMap<Object, Object>>) m -> m.put(null, null), Map.of(null, 1), false));
    }

    @Test
    void equals_self_returnsTrue() {
        assertTrue(mapUnderTest.equals(mapUnderTest), "Map should be equal to itself");
    }

    @Test
    void equals_sameContentDifferentInstance_returnsTrue() {
        mapUnderTest.put("a", "1");
        mapUnderTest.put(null, "nullKey");
        mapUnderTest.put("b", null);
        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put(null, "nullKey");
        other.put("b", null);
        assertTrue(mapUnderTest.equals(other), "Maps with identical entries (including nulls) should be equal");
    }

    @Test
    void equals_otherIsNotMap_returnsFalse() {
        assertFalse(mapUnderTest.equals("not a map"));
    }

    @Test
    void equals_differentSize_returnsFalse() {
        mapUnderTest.put("a", "1");
        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        // extra entry
        other.put("b", "2");
        assertFalse(mapUnderTest.equals(other), "Maps with different sizes should not be equal");
    }

    @Test
    void equals_missingKeyInOther_returnsFalse() {
        mapUnderTest.put("a", "1");
        mapUnderTest.put("b", "2");
        Map<String, String> other = new HashMap<>();
        // "b" missing
        other.put("a", "1");
        assertFalse(mapUnderTest.equals(other), "If the other map lacks a key, equality should be false");
    }

    @Test
    void equals_differentValueForSameKey_returnsFalse() {
        mapUnderTest.put("a", "1");
        Map<String, String> other = new HashMap<>();
        other.put("a", "different");
        assertFalse(mapUnderTest.equals(other), "Maps with same keys but different values should not be equal");
    }

    @Test
    void equals_nullValuesHandledCorrectly() {
        mapUnderTest.put(null, null);
        mapUnderTest.put("key", null);
        mapUnderTest.put(null, "value");
        Map<String, String> other = new HashMap<>();
        other.put(null, "value");
        other.put("key", null);
        // overwrites previous null key entry with null value
        other.put(null, null);
        // Because a map cannot have duplicate null keys, the final state is:
        // null -> null, "key" -> null
        // The test therefore expects inequality.
        assertFalse(mapUnderTest.equals(other));
    }

    /* --------------------------------------------------------------
     *  Parameterised test: branch‑coverage scenarios
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("branchCoverageCases")
    void equals_branchCoverage(String description, java.util.function.Consumer<TestMap<Object, Object>> initializer, Object other, boolean expected) {
        // Arrange
        initializer.accept(mapUnderTest);
        // Act & Assert
        assertEquals(expected, mapUnderTest.equals(other), description);
    }

    /* --------------------------------------------------------------
     *  Parameterised test: additional logical scenarios
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalScenariosCases")
    void equals_additionalScenarios(String description, java.util.function.Consumer<TestMap<Object, Object>> initializer, Object other, boolean expected) {
        // Arrange
        initializer.accept(mapUnderTest);
        // Act & Assert
        assertEquals(expected, mapUnderTest.equals(other), description);
    }

    /* --------------------------------------------------------------
     *  Parameterised test: exception / edge scenarios
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionScenariosCases")
    void equals_exceptionScenarios(String description, java.util.function.Consumer<TestMap<Object, Object>> initializer, Object other, boolean expected) {
        // Arrange
        initializer.accept(mapUnderTest);
        // Act & Assert
        assertEquals(expected, mapUnderTest.equals(other), description);
    }

    // Adjusted test method to handle the "sameMap" placeholder.
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("branchCoverageCases")
    void equals_branchCoverageAdjusted(String description, java.util.function.Consumer<TestMap<Object, Object>> initializer, Object other, boolean expected) {
        initializer.accept(mapUnderTest);
        // If the test case is the "sameMap" scenario, 'other' will be null (the placeholder).
        // Replace it with the actual map instance.
        if (description.startsWith("sameMap")) {
            other = mapUnderTest;
        }
        assertEquals(expected, mapUnderTest.equals(other), description);
    }
}
