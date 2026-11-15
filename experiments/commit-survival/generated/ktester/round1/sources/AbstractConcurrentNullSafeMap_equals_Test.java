package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_equals_Test {

    private TestMap<String, String> map;

    /**
     * Concrete implementation used only for testing the abstract class.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // each test gets a fresh map backed by a ConcurrentHashMap
        map = new TestMap<>(new ConcurrentHashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure map is cleared between tests
        map.clear();
    }

    /**
     * Supplies arguments for {@link #equalsParameterizedTest}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> equalsTestCases() {
        // Helper to create a map from a literal JSON‑like string representation
        // (the test data is small, so we build maps manually)
        return Stream.of(// Same instance – already covered by a dedicated test, but kept for completeness
        arguments(null, null, true, "Same instance should be equal (handled separately)"), // Non‑Map object – also covered, but included for completeness
        arguments(null, null, false, "Non‑Map object should not be equal (handled separately)"), // Different size
        arguments(Map.of("a", "1"), Map.of("a", "1", "b", "2"), false, "Map with different size should not be equal"), // Missing key
        arguments(Map.of("a", "1"), Map.of("b", "2"), false, "Map missing a key present in this map"), // Same key, different value
        arguments(Map.of("key1", "value1"), Map.of("key1", "valueX"), false, "Map with same key but different value should not be equal"), // Identical entries including null value
        arguments(Map.of("key1", "value1", "key2", null), Map.of("key1", "value1", "key2", null), true, "Maps with identical entries including null value should be equal"), // Both empty
        arguments(Collections.emptyMap(), Collections.emptyMap(), true, "Both maps are empty – should be equal"), // Same entries, different iteration order
        arguments(Map.of("a", "1", "b", "2"), new LinkedHashMap<>() {

            {
                put("b", "2");
                put("a", "1");
            }
        }, true, "Same entries in a different iteration order – order must not affect equality"), // Null key with same non‑null value
        arguments(Map.of(null, "val"), Map.of(null, "val"), true, "Both maps contain a null key mapped to the same non‑null value – null‑safe handling should treat them as equal"), // Null key with different values
        arguments(Map.of(null, "val1"), Map.of(null, "different"), false, "Both maps contain a null key but with different values – equality must detect the value mismatch"), // Same key with null value
        arguments(Map.of("key1", null), Map.of("key1", null), true, "Both maps contain the same key with a null value – null values are compared with Objects.equals"), // Different Map implementation (ConcurrentHashMap) with identical entries
        arguments(Map.of("key1", "value1", "key2", "value2"), new ConcurrentHashMap<>(Map.of("key1", "value1", "key2", "value2")), true, "Other map is a different implementation (ConcurrentHashMap) but holds identical entries"));
    }

    @Test
    void equalsSameInstance_returnsTrue() {
        assertTrue(map.equals(map), "Map should be equal to itself");
    }

    @Test
    void equalsWithNonMap_returnsFalse() {
        assertFalse(map.equals("not a map"), "Map should not be equal to a non‑Map object");
    }

    @Test
    void equalsDifferentSize_returnsFalse() {
        map.put("a", "1");
        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        // extra entry
        other.put("b", "2");
        assertFalse(map.equals(other), "Maps with different sizes must not be equal");
    }

    @Test
    void equalsMissingKey_returnsFalse() {
        map.put("a", "1");
        Map<String, String> other = new HashMap<>();
        // different key
        other.put("b", "1");
        assertFalse(map.equals(other), "Maps missing a key present in the other must not be equal");
    }

    @Test
    void equalsDifferentValue_returnsFalse() {
        map.put("a", "1");
        Map<String, String> other = new HashMap<>();
        // same key, different value
        other.put("a", "2");
        assertFalse(map.equals(other), "Maps with different values for the same key must not be equal");
    }

    @Test
    void equalsBothEmpty_returnsTrue() {
        Map<String, String> other = new HashMap<>();
        assertTrue(map.equals(other), "Two empty maps should be equal");
    }

    @Test
    void equalsWithNullKeyAndNullValue_returnsTrue() {
        map.put(null, "nullKey");
        map.put("b", null);
        map.put("a", "1");
        Map<String, String> other = new HashMap<>();
        other.put(null, "nullKey");
        other.put("b", null);
        other.put("a", "1");
        assertTrue(map.equals(other), "Maps containing null keys and null values should be equal when entries match");
    }

    @Test
    void equalsWhenOtherContainsExtraNullKey_returnsFalse() {
        map.put(null, "nullKey");
        Map<String, String> other = new HashMap<>();
        other.put(null, "nullKey");
        // extra entry
        other.put("extra", "value");
        assertFalse(map.equals(other), "Maps with extra entries must not be equal");
    }

    /**
     * Parameterized test that covers many of the JSON‑driven scenarios.
     *
     * The {@code other} map is built from the JSON description; the
     * {@code thisMapEntries} describe the content that must be placed into
     * {@code map} before invoking {@code equals}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("equalsTestCases")
    void equalsParameterizedTest(Map<String, String> thisMapEntries, Map<String, String> other, boolean expected, String description) {
        // Populate the map under test
        if (thisMapEntries != null) {
            thisMapEntries.forEach(map::put);
        }
        // Perform the equality check
        assertEquals(expected, map.equals(other), description);
    }

    /**
     * Passing {@code null} to {@code equals} must return {@code false}
     * and must not throw a {@link NullPointerException}.
     */
    @Test
    void equalsWithNullArgument_returnsFalse() {
        assertFalse(map.equals(null), "Passing null should return false instead of throwing NPE");
    }

    /**
     * If the other {@link Map} implementation throws a runtime exception
     * during the equality check, the exception should propagate.
     */
    @Test
    void equalsWhenOtherMapThrowsRuntimeException_propagates() {
        // Prepare a map under test with a single entry so that equals will invoke
        // other.containsKey(...)
        map.put("key", "value");
        // Create a mock Map whose entrySet() throws a RuntimeException.
        @SuppressWarnings("unchecked")
        Map<String, String> badMap = Mockito.mock(Map.class);
        Mockito.when(badMap.size()).thenReturn(1);
        Mockito.when(badMap.containsKey(Mockito.any())).thenAnswer(invocation -> {
            // Trigger the exception via entrySet()
            badMap.entrySet();
            // unreachable
            return false;
        });
        Mockito.when(badMap.entrySet()).thenThrow(new RuntimeException("entrySet failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.equals(badMap), "Exception from the other map should propagate");
        assertEquals("entrySet failure", ex.getMessage());
    }

    /**
     * Equality must work across different {@link Map} implementations.
     */
    @Test
    void equalsWithConcurrentHashMapImplementation_returnsTrue() {
        map.put("a", "1");
        map.put("b", "2");
        ConcurrentHashMap<String, String> other = new ConcurrentHashMap<>();
        other.put("b", "2");
        other.put("a", "1");
        assertTrue(map.equals(other), "Equality should succeed across different Map implementations");
    }
}
