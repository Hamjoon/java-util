package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class SealableMap_get_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        // start each test with an un‑sealed map and a known content
        sealedState = false;
        sealedSupplier = () -> sealedState;
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared so a subsequent test cannot be polluted
        map.clear();
    }

    /**
     * Provides arguments for the "Basic Retrieval" group.
     *
     * <p>
     * Two scenarios are covered:
     * <ul>
     *   <li>String key "apple" → value "red"</li>
     *   <li>Integer key 42 → value 100</li>
     * </ul>
     * </p>
     */
    private static Stream<Arguments> basicRetrievalProvider() {
        return Stream.of(arguments("apple", "red"), arguments(42, 100));
    }

    /**
     * Provides arguments for the "Null and Missing Keys" group.
     *
     * <p>
     * Two scenarios:
     * <ul>
     *   <li>Calling {@code get(null)} on a map that does not contain a null key.</li>
     *   <li>Calling {@code get("nonexistent")} where the key is absent.</li>
     * </ul>
     * Both must return {@code null} without throwing.
     * </p>
     */
    private static Stream<Arguments> nullAndMissingProvider() {
        return Stream.of(arguments(null, null), arguments("nonexistent", null));
    }

    @Test
    void getReturnsValueForExistingKey() {
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(3, map.get("three"));
    }

    @Test
    void getReturnsNullWhenKeyIsAbsent() {
        assertNull(map.get("missing"));
    }

    @Test
    void getHandlesNullKey() {
        // initially the map contains a null key with a null value
        assertNull(map.get(null));
        // replace the null key value and verify retrieval
        map.put(null, 99);
        assertEquals(99, map.get(null));
    }

    @Test
    void getWorksWhenMapIsSealed() {
        // switch to sealed (read‑only) mode
        sealedState = true;
        // get should still return the existing entries regardless of sealed state
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertNull(map.get("non‑existent"));
        // also verify null‑key handling while sealed
        assertNull(map.get(null));
    }

    @Test
    void getDoesNotMutateMap() {
        int originalSize = map.size();
        map.get("one");
        map.get(null);
        map.get("non‑existent");
        assertEquals(originalSize, map.size(), "Calling get must not change the map size");
    }

    @ParameterizedTest(name = "[{index}] get({0}) should return {1}")
    @MethodSource("basicRetrievalProvider")
    void basicRetrieval(Object key, Object expected) {
        // Create a fresh map for each parameter set; sealed state is irrelevant for reads.
        Supplier<Boolean> supplier = () -> false;
        SealableMap<Object, Object> testMap = new SealableMap<>(supplier);
        // Populate the map according to the current test case.
        if ("apple".equals(key)) {
            testMap.put("apple", "red");
        } else if (Integer.valueOf(42).equals(key)) {
            testMap.put(42, 100);
        }
        // Verify that get returns the expected value.
        assertEquals(expected, testMap.get(key), () -> "Expected get(" + key + ") to return " + expected);
    }

    @ParameterizedTest(name = "[{index}] get({0}) should return null")
    @MethodSource("nullAndMissingProvider")
    void nullAndMissingKeys(Object key, Object expected) {
        // Fresh map without any entries (especially no null key)
        Supplier<Boolean> supplier = () -> false;
        SealableMap<Object, Object> testMap = new SealableMap<>(supplier);
        // No put operations – the map stays empty.
        // The method must return null for both cases.
        assertNull(testMap.get(key), () -> "Expected get(" + key + ") to return null");
    }
}
