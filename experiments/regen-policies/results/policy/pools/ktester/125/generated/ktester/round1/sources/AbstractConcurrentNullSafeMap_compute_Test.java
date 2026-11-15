package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_compute_Test {

    private TestMap<String, Integer> map;

    /**
     * Integer‑typed map used by the original hand‑written tests.
     */
    private TestMap<String, Integer> intMap;

    /**
     * Concrete implementation used for the tests.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> backing) {
            super(backing);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a fresh backing map for each test to guarantee isolation.
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new TestMap<>(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to aid GC and avoid accidental cross‑test contamination.
        intMap.clear();
    }

    /**
     * Helper method that creates a fresh String‑typed map for each test case.
     */
    private TestMap<String, String> freshStringMap() {
        return new TestMap<>(new ConcurrentHashMap<>());
    }

    /**
     * Arguments provider for normal (non‑exceptional) compute scenarios.
     */
    private static Stream<Arguments> computeNormalProvider() {
        return Stream.of(// group: compute - basic behavior with non‑null key and existing value
        Arguments.of("apple", (BiFunction<String, String, String>) (k, v) -> v + "_updated", "apple_updated", "Key present, remapper returns a new non‑null value"), Arguments.of("banana", (BiFunction<String, String, String>) (k, v) -> null, null, "Key present, remapper returns null → entry removed"), // group: compute - null key with existing value
        Arguments.of(null, (BiFunction<String, String, String>) (k, v) -> v + "_nullKey", "nullKey_nullKey", "Null key present, remapper returns new non‑null value"), // group: compute - null key absent
        Arguments.of(null, (BiFunction<String, String, String>) (k, v) -> (k == null ? "generated" : "unexpected"), "generated", "Null key absent, remapper creates a value"), // group: compute - absent key with non‑null result
        Arguments.of("pear", (BiFunction<String, String, String>) (k, v) -> k + "_new", "pear_new", "Absent key, remapper creates a value"), // group: compute - absent key with null result
        Arguments.of("orange", (BiFunction<String, String, String>) (k, v) -> null, null, "Absent key, remapper returns null → no entry created"), // group: compute - remappingFunction returns same value
        Arguments.of("grape", (BiFunction<String, String, String>) (k, v) -> v, "grape", "Remapper returns the old value unchanged"));
    }

    @Test
    void compute_ReplacesExistingValue() {
        // given
        intMap.put("key", 1);
        BiFunction<String, Integer, Integer> remapper = (k, v) -> v + 10;
        // when
        Integer result = intMap.compute("key", remapper);
        // then
        assertEquals(11, result);
        assertEquals(11, intMap.get("key"));
    }

    @Test
    void compute_AllowsNullKey() {
        // given
        intMap.put(null, 5);
        BiFunction<String, Integer, Integer> remapper = (k, v) -> v * 2;
        // when
        Integer result = intMap.compute(null, remapper);
        // then
        assertEquals(10, result);
        assertEquals(10, intMap.get(null));
    }

    @Test
    void compute_RemappingReturnsNull_RemovesEntry() {
        // given
        intMap.put("toRemove", 3);
        BiFunction<String, Integer, Integer> remapper = (k, v) -> null;
        // when
        Integer result = intMap.compute("toRemove", remapper);
        // then
        assertNull(result);
        assertFalse(intMap.containsKey("toRemove"));
    }

    @Test
    void compute_OnAbsentKeyAddsEntry() {
        // given
        BiFunction<String, Integer, Integer> remapper = (k, v) -> (v == null) ? 100 : v;
        // when
        Integer result = intMap.compute("newKey", remapper);
        // then
        assertEquals(100, result);
        assertEquals(100, intMap.get("newKey"));
    }

    @Test
    void compute_UsesBiFunctionArgumentsCorrectly(@Mock BiFunction<String, Integer, Integer> mockRemapper) {
        // given
        intMap.put("mockKey", 7);
        when(mockRemapper.apply("mockKey", 7)).thenReturn(42);
        // when
        Integer result = intMap.compute("mockKey", mockRemapper);
        // then
        assertEquals(42, result);
        assertEquals(42, intMap.get("mockKey"));
        verify(mockRemapper, times(1)).apply("mockKey", 7);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("computeNormalProvider")
    void compute_NormalScenarios(String key, BiFunction<String, String, String> remapper, String expectedResult, String description) {
        // Arrange
        TestMap<String, String> map = freshStringMap();
        // Pre‑populate the map when the test case expects an existing entry.
        if (key != null && !"pear".equals(key) && !"orange".equals(key) && !"grape".equals(key)) {
            // For the cases where the key is supposed to be already present,
            // we insert a value that matches the description.
            // The exact initial value is not important except that it is non‑null.
            map.put(key, key);
        } else if (key == null && !"generated".equals(expectedResult)) {
            // For the null‑key‑existing‑value case we need a starting value.
            map.put(null, "nullKey");
        }
        // Act
        String result = map.compute(key, remapper);
        // Assert result value
        assertEquals(expectedResult, result, "Returned value does not match expectation");
        // Assert map state
        if (expectedResult == null) {
            // When the remapper returns null the entry must not exist.
            assertFalse(map.containsKey(key), "Entry should have been removed");
        } else {
            // Otherwise the map must contain the new value.
            assertTrue(map.containsKey(key), "Entry should be present");
            assertEquals(expectedResult, map.get(key), "Stored value does not match expected");
        }
    }

    @Test
    void compute_RemappingFunctionThrowsRuntimeException() {
        // Arrange
        TestMap<String, String> map = freshStringMap();
        map.put("kiwi", "green");
        BiFunction<String, String, String> exploding = (k, v) -> {
            throw new IllegalStateException("boom");
        };
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.compute("kiwi", exploding), "Exception from remapping function must propagate");
        assertEquals("boom", ex.getMessage());
        // Ensure map unchanged
        assertEquals("green", map.get("kiwi"));
    }

    @Test
    void compute_NullRemappingFunction_ThrowsNullPointerException() {
        // Arrange
        TestMap<String, String> map = freshStringMap();
        map.put("mango", "sweet");
        // Act & Assert
        assertThrows(NullPointerException.class, () -> map.compute("mango", null), "Passing a null remappingFunction must throw NPE");
        // Map must stay unchanged
        assertEquals("sweet", map.get("mango"));
    }
}
