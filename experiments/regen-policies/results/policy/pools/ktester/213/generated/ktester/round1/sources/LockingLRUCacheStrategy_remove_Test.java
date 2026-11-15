package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_remove_Test {

    private LockingLRUCacheStrategy<Integer, String> cache;

    /* --------------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final String key;

        final String expectedValue;

        final boolean expectPresentBeforeRemoval;

        final String description;

        ArgumentsHolder(String key, String expectedValue, boolean expectPresentBeforeRemoval, String description) {
            this.key = key;
            this.expectedValue = expectedValue;
            this.expectPresentBeforeRemoval = expectPresentBeforeRemoval;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Create a fresh cache instance before each test.
     * Capacity is set to a small number (2) – the exact value is not important for the
     * remove() behaviour we are testing.
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new LockingLRUCacheStrategy<>(2);
    }

    /**
     * No special teardown is required – the cache instance will be garbage‑collected.
     */
    @AfterEach
    void teardownAfterEach() {
        cache = null;
    }

    /* Helper to obtain the private ReentrantLock via reflection.            */
    private ReentrantLock getInternalLock() throws Exception {
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        return (ReentrantLock) lockField.get(cache);
    }

    /* --------------------------------------------------------------------- */
    static Stream<ArgumentsHolder> removeScenarios() {
        return Stream.of(// remove existing key "alpha"
        new ArgumentsHolder("alpha", "valueAlpha", true, "The cache contains a mapping for key \"alpha\" with value \"valueAlpha\". " + "Calling remove(\"alpha\") should acquire the lock, remove the node from the internal map, " + "invoke removeNode(node) and return the associated value."), // remove existing key "beta" after previous removal (fresh cache)
        new ArgumentsHolder("beta", "valueBeta", true, "After populating the cache with a second entry (\"beta\" → \"valueBeta\"), " + "remove(\"beta\") must return the correct value and leave the cache empty."), // remove non‑existing key "gamma"
        new ArgumentsHolder("gamma", null, false, "The cache does not contain the key \"gamma\". The method should lock, attempt to remove, " + "receive a null node, skip removeNode and return null."), // remove null key when not present
        new ArgumentsHolder(null, null, false, "If the cache does not contain a null key, remove(null) should behave like a miss and return null " + "without throwing an exception."), // remove key whose associated value is null
        new ArgumentsHolder("nullValueKey", null, true, "Insert an entry with a non‑null key but a null value (put(\"nullValueKey\", null)). " + "Calling remove(\"nullValueKey\") should return null (the stored value) while still removing the node."));
    }

    @Test
    @DisplayName("remove(Object) returns the correct value for an existing key and updates the cache")
    void testRemoveExistingKey() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals(2, cache.size(), "pre‑condition: cache should contain two entries");
        // when
        String removed = cache.remove(1);
        // then
        assertEquals("one", removed, "remove should return the associated value");
        assertEquals(1, cache.size(), "size should decrease after removal");
        assertNull(cache.get(1), "removed key should no longer be present");
        assertFalse(cache.containsKey(1), "containsKey should be false for removed key");
        assertTrue(cache.containsKey(2), "other entries must remain untouched");
    }

    @Test
    @DisplayName("remove(Object) returns null when the key is not present")
    void testRemoveNonExistingKey() {
        // given
        cache.put(10, "ten");
        assertEquals(1, cache.size());
        // when
        // 99 was never added
        String removed = cache.remove(99);
        // then
        assertNull(removed, "remove should return null for a missing key");
        assertEquals(1, cache.size(), "size must stay unchanged");
        assertTrue(cache.containsKey(10), "existing entry must remain");
    }

    @Test
    @DisplayName("remove(Object) works with a null key")
    void testRemoveNullKey() {
        // given
        cache.put(null, "nullValue");
        cache.put(5, "five");
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey(null));
        // when
        String removed = cache.remove(null);
        // then
        assertEquals("nullValue", removed, "remove should return the value associated with null key");
        assertEquals(1, cache.size(), "size should decrease after removing null key");
        assertFalse(cache.containsKey(null), "null key should no longer be present");
        assertTrue(cache.containsKey(5), "other entries must remain");
    }

    @ParameterizedTest(name = "[{index}] remove key: {0}")
    @MethodSource("removeScenarios")
    @DisplayName("remove(Object) various scenarios")
    void testRemoveScenarios(ArgumentsHolder args) throws Exception {
        // -----------------------------------------------------------------
        // GIVEN – populate the cache according to the scenario description
        // -----------------------------------------------------------------
        if (args.key != null || "null".equals(args.key)) {
            // For the "null key present" case we need a separate test (handled later)
            // Here we only put the entry if we expect it to be present before removal.
            if (args.expectPresentBeforeRemoval) {
                cache.put(args.key, args.expectedValue);
            }
        }
        // Special handling for the "null key present" scenario – it is not part of the
        // parameterised set because its expected value is non‑null.
        // (Handled in a dedicated test method below.)
        // -----------------------------------------------------------------
        // WHEN
        // -----------------------------------------------------------------
        String removed = cache.remove(args.key);
        // -----------------------------------------------------------------
        // THEN – validate return value and cache state
        // -----------------------------------------------------------------
        assertEquals(args.expectedValue, removed, "remove should return the expected value for key: " + args.key);
        if (args.expectPresentBeforeRemoval) {
            // The entry should have been removed.
            assertFalse(cache.containsKey(args.key), "After removal the key must no longer be present: " + args.key);
            assertNull(cache.get(args.key), "get() must return null for the removed key: " + args.key);
        } else {
            // No entry existed before, cache size must stay unchanged.
            assertEquals(0, cache.size(), "Cache should remain empty when removing a non‑existent key.");
        }
        // Verify that the internal lock is not held after the operation.
        ReentrantLock lock = getInternalLock();
        assertFalse(lock.isLocked(), "Lock must be released after remove()");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("remove(null) when null key is present")
    void testRemoveNullKeyPresent() throws Exception {
        // GIVEN
        cache.put(null, "nullKeyValue");
        assertTrue(cache.containsKey(null));
        assertEquals(1, cache.size());
        // WHEN
        String removed = cache.remove(null);
        // THEN
        assertEquals("nullKeyValue", removed, "remove(null) should return the stored value");
        assertFalse(cache.containsKey(null), "null key must be removed");
        assertEquals(0, cache.size(), "cache should be empty after removing the null key");
        assertNull(cache.get(null));
        // Verify lock release
        ReentrantLock lock = getInternalLock();
        assertFalse(lock.isLocked(), "Lock must be released after removing null key");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("verify lock is released after removal")
    void testLockReleasedAfterRemoval() throws Exception {
        // GIVEN
        cache.put("delta", "valueDelta");
        assertEquals(1, cache.size());
        // WHEN
        String removed = cache.remove("delta");
        assertEquals("valueDelta", removed);
        // THEN – internal lock must be free
        ReentrantLock lock = getInternalLock();
        assertFalse(lock.isLocked(), "Lock should be unlocked after remove()");
        // Attempt to acquire the lock from another thread.
        AtomicBoolean acquired = new AtomicBoolean(false);
        Thread t = new Thread(() -> {
            lock.lock();
            try {
                acquired.set(true);
            } finally {
                lock.unlock();
            }
        });
        t.start();
        // wait a short time
        t.join(TimeUnit.SECONDS.toMillis(2));
        assertTrue(acquired.get(), "Another thread must be able to acquire the lock after removal");
    }
}
