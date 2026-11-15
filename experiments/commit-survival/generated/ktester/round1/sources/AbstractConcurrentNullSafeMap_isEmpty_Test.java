package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_isEmpty_Test {

    /**
     * Mocked internal map that the abstract class delegates to.
     */
    @Mock
    private ConcurrentMap<Object, Object> internalMapMock;

    /**
     * Instance of a concrete subclass used for testing.
     */
    private TestMap<String, Integer> testMap;

    /**
     * Auto‑closeable for Mockito annotations.
     */
    private AutoCloseable mocks;

    /**
     * Simple concrete implementation of the abstract class.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations and create the test subject using the mock.
        mocks = MockitoAnnotations.openMocks(this);
        testMap = new TestMap<>(internalMapMock);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Reset mock interactions and release Mockito resources.
        reset(internalMapMock);
        testMap = null;
        mocks.close();
    }

    /**
     * Provides the scenarios for the "behavior" group.
     *
     * Each {@link Arguments} consists of:
     *   1. A short description (used only for readability in the test name).
     *   2. A {@code Consumer<TestMap<String,Integer>>} that mutates the map to the desired state.
     *   3. The expected result of {@code isEmpty()} after the mutation.
     */
    private static Stream<Arguments> behaviorCases() {
        return Stream.of(Arguments.of("newly created map is empty", (Consumer<TestMap<String, Integer>>) map -> {
        }, // no mutation
        true), Arguments.of("non‑null key/value makes map non‑empty", (Consumer<TestMap<String, Integer>>) map -> map.put("a", 1), false), Arguments.of("null key with non‑null value makes map non‑empty", (Consumer<TestMap<String, Integer>>) map -> map.put(null, 1), false), Arguments.of("non‑null key with null value makes map non‑empty", (Consumer<TestMap<String, Integer>>) map -> map.put("a", null), false), Arguments.of("clear after insertion returns to empty", (Consumer<TestMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.clear();
        }, true), Arguments.of("remove sole entry leaves map empty", (Consumer<TestMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.remove("a");
        }, true), Arguments.of("remove(key,value) matching entry empties map", (Consumer<TestMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.remove("a", 1);
        }, true), Arguments.of("removing one of multiple entries keeps map non‑empty", (Consumer<TestMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.put("b", 2);
            map.remove("a");
        }, false));
    }

    /**
     * Provides the scenarios for the "edge cases" group.
     *
     * Each {@link Arguments} consists of:
     *   1. Description.
     *   2. Mutator that performs the operations.
     *   3. Expected result of {@code isEmpty()}.
     */
    private static Stream<Arguments> edgeCaseCases() {
        return Stream.of(Arguments.of("null key and null value produce a non‑empty map", (Consumer<TestMap<String, Integer>>) map -> map.put(null, null), false), Arguments.of("computeIfAbsent creates a new entry", (Consumer<TestMap<String, Integer>>) map -> map.computeIfAbsent("c", k -> 3), false), Arguments.of("compute that returns null removes the entry, leaving map empty", (Consumer<TestMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.compute("a", (k, v) -> null);
        }, true), Arguments.of("clear after multiple inserts results in empty map", (Consumer<TestMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.put("b", 2);
            map.clear();
        }, true), Arguments.of("merge with remapping function returning null removes the entry", (Consumer<TestMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.merge("a", 2, (oldV, newV) -> null);
        }, true), Arguments.of("putIfAbsent with existing key leaves map non‑empty", (Consumer<TestMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.putIfAbsent("a", 2);
        }, false), Arguments.of("replace with existing key keeps map non‑empty", (Consumer<TestMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.replace("a", 2);
        }, false), Arguments.of("removing a non‑existent key does not affect non‑empty map", (Consumer<TestMap<String, Integer>>) map -> {
            map.put("a", 1);
            map.remove("nonexistent");
        }, false));
    }

    @Test
    void isEmpty_whenInternalMapIsEmpty_returnsTrue() {
        // GIVEN
        when(internalMapMock.isEmpty()).thenReturn(true);
        // WHEN
        boolean result = testMap.isEmpty();
        // THEN
        assertTrue(result, "isEmpty() should return true when the internal map reports empty");
        verify(internalMapMock, times(1)).isEmpty();
    }

    @Test
    void isEmpty_whenInternalMapIsNotEmpty_returnsFalse() {
        // GIVEN
        when(internalMapMock.isEmpty()).thenReturn(false);
        // WHEN
        boolean result = testMap.isEmpty();
        // THEN
        assertFalse(result, "isEmpty() should return false when the internal map reports not empty");
        verify(internalMapMock, times(1)).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("behaviorCases")
    void isEmpty_behaviorTests(String description, Consumer<TestMap<String, Integer>> mutator, boolean expected) {
        // Use a real ConcurrentHashMap for these tests.
        testMap = new TestMap<>(new ConcurrentHashMap<>());
        // Apply the scenario‑specific mutations.
        mutator.accept(testMap);
        // Verify the result of isEmpty().
        assertEquals(expected, testMap.isEmpty(), () -> "Unexpected isEmpty() result for scenario: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeCaseCases")
    void isEmpty_edgeCaseTests(String description, Consumer<TestMap<String, Integer>> mutator, boolean expected) {
        // Use a real ConcurrentHashMap for these tests.
        testMap = new TestMap<>(new ConcurrentHashMap<>());
        // Apply the scenario‑specific mutations.
        mutator.accept(testMap);
        // Verify the result of isEmpty().
        assertEquals(expected, testMap.isEmpty(), () -> "Unexpected isEmpty() result for edge case: " + description);
    }
}
