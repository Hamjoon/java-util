package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_getCapacity_Test {

    /**
     * Instance used in the per‑test lifecycle.
     */
    private LockingLRUCacheStrategy<String, Integer> cacheStrategy;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test with a known capacity.
        cacheStrategy = new LockingLRUCacheStrategy<>(10);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the reference; not strictly necessary but keeps the pattern clear.
        cacheStrategy = null;
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of(10, 10), Arguments.of(0, 0), Arguments.of(-5, -5));
    }

    private static Stream<Arguments> extremeValuesProvider() {
        return Stream.of(Arguments.of(1, 1), Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE), Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE), Arguments.of(1_000_000, 1_000_000));
    }

    private static Stream<Arguments> postModificationProvider() {
        return Stream.of(Arguments.of(5, 5), Arguments.of(3, 3), Arguments.of(-2, -2));
    }

    @Test
    @DisplayName("getCapacity() returns the capacity passed to the constructor")
    void testGetCapacityReturnsConstructorValue() {
        // GIVEN a cache created with capacity 10 (set up in @BeforeEach)
        // WHEN we query its capacity
        int actual = cacheStrategy.getCapacity();
        // THEN it should equal the value supplied at construction time
        assertEquals(10, actual, "The capacity reported by getCapacity() should match the constructor argument.");
    }

    @Test
    @DisplayName("Multiple instances retain their own capacities")
    void testMultipleInstancesHaveIndependentCapacities() {
        // GIVEN two distinct cache instances with different capacities
        LockingLRUCacheStrategy<String, Integer> smallCache = new LockingLRUCacheStrategy<>(3);
        LockingLRUCacheStrategy<String, Integer> largeCache = new LockingLRUCacheStrategy<>(100);
        // WHEN we query each capacity
        int small = smallCache.getCapacity();
        int large = largeCache.getCapacity();
        // THEN each should report its own constructor value
        assertEquals(3, small, "Small cache should report capacity 3");
        assertEquals(100, large, "Large cache should report capacity 100");
    }

    @Test
    @DisplayName("Zero capacity is reported correctly")
    void testZeroCapacity() {
        // GIVEN a cache with zero capacity
        LockingLRUCacheStrategy<String, Integer> zeroCache = new LockingLRUCacheStrategy<>(0);
        // WHEN we retrieve the capacity
        int capacity = zeroCache.getCapacity();
        // THEN it should be zero
        assertEquals(0, capacity, "Cache with zero capacity should report 0");
    }

    @Test
    @DisplayName("Negative capacity is reported correctly (no validation in constructor)")
    void testNegativeCapacity() {
        // GIVEN a cache constructed with a negative capacity
        LockingLRUCacheStrategy<String, Integer> negativeCache = new LockingLRUCacheStrategy<>(-5);
        // WHEN we retrieve the capacity
        int capacity = negativeCache.getCapacity();
        // THEN it should reflect the negative value (the class does not enforce positivity)
        assertEquals(-5, capacity, "Cache should report the negative capacity supplied to the constructor");
    }

    // ---------- Basic functionality ----------
    @ParameterizedTest(name = "{index} => capacity={0}, expected={1}")
    @MethodSource("basicFunctionalityProvider")
    @DisplayName("Basic functionality – getCapacity returns constructor value")
    void testBasicFunctionality(int capacity, int expected) {
        // GIVEN a cache with the supplied capacity
        LockingLRUCacheStrategy<String, Integer> cache = new LockingLRUCacheStrategy<>(capacity);
        // WHEN we query the capacity
        int actual = cache.getCapacity();
        // THEN it matches the expected value
        assertEquals(expected, actual, () -> "Expected capacity " + expected + " but got " + actual);
    }

    // ---------- Extreme values ----------
    @ParameterizedTest(name = "{index} => capacity={0}, expected={1}")
    @MethodSource("extremeValuesProvider")
    @DisplayName("Extreme values – getCapacity handles edge integer values")
    void testExtremeValues(int capacity, int expected) {
        // GIVEN a cache with an extreme capacity value
        LockingLRUCacheStrategy<String, Integer> cache = new LockingLRUCacheStrategy<>(capacity);
        // WHEN we query the capacity
        int actual = cache.getCapacity();
        // THEN it matches the expected extreme value
        assertEquals(expected, actual, () -> "Expected capacity " + expected + " but got " + actual);
    }

    // ---------- After cache modifications ----------
    @ParameterizedTest(name = "{index} => capacity={0}, expected={1}")
    @MethodSource("postModificationProvider")
    @DisplayName("Capacity remains unchanged after cache modifications")
    void testCapacityAfterModifications(int capacity, int expected) {
        // GIVEN a cache with the supplied capacity
        LockingLRUCacheStrategy<String, Integer> cache = new LockingLRUCacheStrategy<>(capacity);
        // Perform a series of modifications that are valid for the Map interface.
        // These operations are not expected to affect the immutable capacity field.
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.remove("b");
        cache.clear();
        // Additionally test putAll with a non‑empty map
        Map<String, Integer> extra = new HashMap<>();
        extra.put("x", 99);
        extra.put("y", 100);
        cache.putAll(extra);
        // WHEN we query the capacity after modifications
        int actual = cache.getCapacity();
        // THEN it should still equal the constructor value
        assertEquals(expected, actual, () -> "Capacity should remain " + expected + " after cache operations, but got " + actual);
    }
}
