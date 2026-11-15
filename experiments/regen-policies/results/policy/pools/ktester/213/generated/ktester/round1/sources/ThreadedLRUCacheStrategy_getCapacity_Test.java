package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_getCapacity_Test {

    /**
     * The cache instance used in most tests.
     */
    private ThreadedLRUCacheStrategy<String, String> cache;

    /**
     * Common parameters for a valid cache instance.
     */
    private static final int VALID_CAPACITY = 10;

    private static final int VALID_CLEANUP_DELAY_MILLIS = 100;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache before each test so that state does not leak.
        cache = new ThreadedLRUCacheStrategy<>(VALID_CAPACITY, VALID_CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // The cache does not expose a close/shutdown method, and the
        // internal scheduler is shared/static, so nothing specific is needed.
        cache = null;
    }

    @Test
    @DisplayName("getCapacity() returns the capacity supplied to the constructor")
    void getCapacityReturnsConstructorValue() {
        // GIVEN – a cache created in @BeforeEach with VALID_CAPACITY
        // WHEN
        int actual = cache.getCapacity();
        // THEN
        assertEquals(VALID_CAPACITY, actual, "The capacity reported by getCapacity() should match the value passed to the constructor.");
    }

    @Test
    @DisplayName("getCapacity() reflects the capacity of a cache created with a different value")
    void getCapacityReflectsDifferentConstructorValue() {
        // GIVEN
        int otherCapacity = 42;
        ThreadedLRUCacheStrategy<Integer, Integer> otherCache = new ThreadedLRUCacheStrategy<>(otherCapacity, VALID_CLEANUP_DELAY_MILLIS);
        // WHEN
        int actual = otherCache.getCapacity();
        // THEN
        assertEquals(otherCapacity, actual, "The capacity reported must correspond to the constructor argument, even for different instances.");
    }

    @Test
    @DisplayName("Capacity does not change after put operations")
    void capacityRemainsConstantAfterMutations() {
        // GIVEN a fresh cache with the supplied capacity
        ThreadedLRUCacheStrategy<Integer, Integer> localCache = new ThreadedLRUCacheStrategy<>(capacity, VALID_CLEANUP_DELAY_MILLIS);
        // Perform a mixture of operations that could trigger cleanup.
        for (int i = 0; i < capacity + 2; i++) {
            localCache.put(i, i * 10);
        }
        // explicit clear
        localCache.clear();
        // add a new entry after clear
        localCache.put(999, 9990);
        // remove the entry again
        localCache.remove(999);
        // WHEN the capacity is queried after the operations
        int actual = localCache.getCapacity();
        // THEN it must still be the original constructor value
        assertEquals(capacity, actual, () -> "Capacity changed after mutations; expected " + capacity + " but got " + actual);
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException for capacity < 1")
    void constructorRejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ThreadedLRUCacheStrategy<>(0, VALID_CLEANUP_DELAY_MILLIS), "Capacity less than 1 must cause an IllegalArgumentException.");
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException for cleanupDelayMillis < 10")
    void constructorRejectsTooSmallCleanupDelay() {
        assertThrows(IllegalArgumentException.class, () -> new ThreadedLRUCacheStrategy<>(VALID_CAPACITY, 5), "cleanupDelayMillis less than 10 must cause an IllegalArgumentException.");
    }

    /**
     * Verifies that {@code getCapacity()} returns exactly the value supplied to the constructor
     * for a variety of positive capacities, including the extreme {@code Integer.MAX_VALUE}.
     *
     * @param capacity the capacity passed to the constructor
     */
    @ParameterizedTest(name = "[{index}] capacity = {0}")
    @CsvSource({ // smallest positive capacity
    "1", // typical default used in the framework
    "10", // mid‑range value
    "100", // Integer.MAX_VALUE
    "2147483647" })
    @DisplayName("getCapacity() returns the constructor value for valid capacities")
    void getCapacityReturnsConstructorValueForValidCapacities(int capacity) {
        // GIVEN a cache created with the supplied capacity
        ThreadedLRUCacheStrategy<Integer, Integer> localCache = new ThreadedLRUCacheStrategy<>(capacity, VALID_CLEANUP_DELAY_MILLIS);
        // WHEN the capacity is queried
        int actual = localCache.getCapacity();
        // THEN it must match the constructor argument
        assertEquals(capacity, actual, () -> "Expected capacity to be " + capacity + " but was " + actual);
    }

    /**
     * The current implementation rejects non‑positive capacities by throwing
     * {@link IllegalArgumentException}. This test confirms that behaviour for
     * zero, negative and {@code Integer.MIN_VALUE} values.
     *
     * @param capacity the illegal capacity to pass to the constructor
     */
    @ParameterizedTest(name = "[{index}] illegal capacity = {0}")
    @CsvSource({ "0", "-1", "-5", "-2147483648" })
    @DisplayName("Constructor rejects non‑positive capacities")
    void constructorRejectsNonPositiveCapacities(int capacity) {
        assertThrows(IllegalArgumentException.class, () -> new ThreadedLRUCacheStrategy<>(capacity, VALID_CLEANUP_DELAY_MILLIS), () -> "Constructor should reject capacity " + capacity);
    }
}
