package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_put_Test {

    private LRUCache<Integer, String> lruCache;

    /**
     * --------------------------------------------------------------------- *
     *  Simple data holder for each test case.
     *  ---------------------------------------------------------------------
     */
    private static class PutTestCase {

        final Object key;

        final Object value;

        // either previous value or exception class
        final Object expected;

        final String description;

        final String expectedExceptionName;

        final Class<? extends Throwable> expectedException;

        // string representation of expected previous value (may be null)
        final String previousValue;

        PutTestCase(String key, Integer value, Integer previous, String description) {
            this.key = key;
            this.value = value;
            this.previousValue = previous == null ? null : previous.toString();
            this.expected = previous;
            this.expectedException = null;
            this.description = description;
            this.expectedExceptionName = null;
        }

        PutTestCase(String key, Integer value, String previous, String description, Integer dummy) {
            // overload for case where previous value is non‑null but represented as Integer
            this.key = key;
            this.value = value;
            this.previousValue = previous;
            this.expected = previous;
            this.expectedException = null;
            this.description = description;
            this.expectedExceptionName = null;
        }

        PutTestCase(String key, Integer value, Class<? extends Throwable> exception, String description) {
            this.key = key;
            this.value = value;
            this.previousValue = null;
            this.expected = exception;
            this.expectedException = exception;
            this.description = description;
            this.expectedExceptionName = exception.getSimpleName();
        }
    }

    /**
     * --------------------------------------------------------------------- *
     *  Per‑test lifecycle handling
     *  ---------------------------------------------------------------------
     */
    @BeforeEach
    void setupBeforeEach() {
        // The concrete cache instance is created in each test via setUp(strategy).
        // Keeping the field null here ensures a clean start for every test.
        lruCache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to avoid cross‑test contamination.
        if (lruCache != null) {
            lruCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the thread pool used by the threaded strategy (if any).
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * --------------------------------------------------------------------- *
     *  Helper for creating a cache with a specific strategy
     *  ---------------------------------------------------------------------
     */
    private void setUp(LRUCache.StrategyType strategy) {
        // Capacity 3 is enough for the eviction‑related tests while keeping the
        // execution fast.
        lruCache = new LRUCache<>(3, strategy);
    }

    /**
     * --------------------------------------------------------------------- *
     *  Parameter source for the two supported strategies
     *  ---------------------------------------------------------------------
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * --------------------------------------------------------------------- *
     *  Helper to create a cache instance whose private {@code strategy} field
     *  is replaced by the supplied mock map.
     *  ---------------------------------------------------------------------
     */
    private void setUpWithMockMap(Map<Integer, String> mockMap) throws Exception {
        // Use the public constructor that selects a strategy; the actual map will be
        // overwritten via reflection.
        lruCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(lruCache, mockMap);
    }

    /**
     * --------------------------------------------------------------------- *
     *  Parameter source for the JSON test cases.
     *  ---------------------------------------------------------------------
     */
    static Stream<PutTestCase> putTestCases() {
        return Stream.of(// group: LRUCache.put(K, V) behavior
        new PutTestCase("alpha", 123, null, "Putting a new key that does not exist in the cache. The underlying strategy.map.put returns null according to Map contract, so LRUCache.put should also return null."), new PutTestCase("beta", 456, 789, "Putting a key that already exists. The mocked strategy.map.put is set to return the previous value (789). LRUCache.put must return that previous value."), new PutTestCase(null, 999, null, "Putting a null key when the underlying strategy permits null keys. The strategy.map.put returns null, so LRUCache.put should return null without throwing."), new PutTestCase("gamma", null, null, "Putting a null value for an existing key. The strategy.map.put returns the previous non‑null value (e.g., 111). The test verifies that LRUCache.put returns that previous value.", 111), new PutTestCase("delta", 555, RuntimeException.class, "The underlying strategy.map.put throws a RuntimeException. LRUCache.put must not catch it; the exception should propagate to the caller."), // group: LRUCache.put(K, V) edge cases
        new PutTestCase("epsilon", 321, 111, "Putting a key that already exists with a different value. The underlying strategy.map.put returns the previous value (111), which LRUCache.put must return."), new PutTestCase("zeta", null, null, "Putting a key that already exists whose current value is null. The strategy.map.put returns null, so LRUCache.put should also return null."), new PutTestCase(null, null, null, "Putting a null key and a null value when the underlying strategy permits nulls. No previous entry exists, therefore null is returned."), new PutTestCase(null, 777, NullPointerException.class, "The underlying strategy does not allow null keys and throws NullPointerException. LRUCache.put must let the exception propagate."), new PutTestCase("theta", null, NullPointerException.class, "The underlying strategy does not allow null values and throws NullPointerException. LRUCache.put must let the exception propagate."), new PutTestCase("iota", 999, IllegalStateException.class, "Calling put after LRUCache.shutdown() should result in IllegalStateException according to the cache’s lifecycle contract."), // group: LRUCache.put(K, V) exception scenarios
        new PutTestCase("wrongTypeKey", 123, ClassCastException.class, "The underlying strategy.map.put throws ClassCastException because the key type does not match the map's generic type. LRUCache.put must let the exception propagate."), new PutTestCase("validKey", "invalidValue", IllegalArgumentException.class, "The underlying strategy.map.put validates the value and throws IllegalArgumentException (e.g., value out of allowed range). LRUCache.put must not swallow the exception."), new PutTestCase("anyKey", 456, UnsupportedOperationException.class, "The underlying strategy.map is an unmodifiable map; its put operation throws UnsupportedOperationException. LRUCache.put should propagate this exception."), new PutTestCase("someKey", 789, NullPointerException.class, "LRUCache was constructed with a null strategy (e.g., via reflection or a faulty constructor). Calling put results in a NullPointerException when delegating to strategy.put. The exception must propagate."));
    }

    /**
     * --------------------------------------------------------------------- *
     *  Basic put / get verification
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAndGet(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // first insertion returns null
        assertNull(lruCache.put(1, "A"));
        assertEquals("A", lruCache.get(1));
        assertNull(lruCache.put(2, "B"));
        assertEquals("B", lruCache.get(2));
        assertNull(lruCache.put(3, "C"));
        assertEquals("C", lruCache.get(3));
    }

    /**
     * --------------------------------------------------------------------- *
     *  Overwrite an existing key – put should return the previous value
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutOverwrite(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "first");
        String previous = lruCache.put(1, "second");
        assertEquals("first", previous, "put should return the previous value for the key");
        assertEquals("second", lruCache.get(1), "new value should replace the old one");
    }

    /**
     * --------------------------------------------------------------------- *
     *  Null handling – both key and value may be null
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutNullKeyAndValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // null key, non‑null value
        assertNull(lruCache.put(null, "value"));
        assertTrue(lruCache.containsKey(null));
        assertTrue(lruCache.containsValue("value"));
        assertEquals("value", lruCache.get(null));
        // null key, null value
        assertNull(lruCache.put(null, null));
        assertTrue(lruCache.containsKey(null));
        assertTrue(lruCache.containsValue(null));
        assertNull(lruCache.get(null));
    }

    /**
     * --------------------------------------------------------------------- *
     *  Eviction policy – when capacity is exceeded the LRU entry should be removed.
     *  The threaded strategy evicts asynchronously, so we wait briefly.
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testEvictionPolicy(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        // Access key 1 to make it most‑recently used
        assertEquals("A", lruCache.get(1));
        // Adding a fourth entry should trigger eviction of key 2 (the LRU)
        lruCache.put(4, "D");
        // For the threaded strategy eviction happens on a background thread.
        // Poll until we observe the expected state or timeout.
        // 5 seconds max
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            boolean key2Gone = !lruCache.containsKey(2);
            boolean key1Present = lruCache.containsKey(1);
            boolean key4Present = lruCache.containsKey(4);
            if (key2Gone && key1Present && key4Present) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        assertNull(lruCache.get(2), "Key 2 should have been evicted");
        assertEquals("A", lruCache.get(1), "Key 1 should still be present");
        assertEquals("D", lruCache.get(4), "Key 4 should be present");
    }

    /**
     * --------------------------------------------------------------------- *
     *  Parameterized test that drives all cases defined above.
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest
    @MethodSource("putTestCases")
    void testPutWithMockedStrategy(PutTestCase tc) throws Exception {
        // Prepare the mock map according to the expected behaviour.
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        // Determine the key and value types expected by the cache (Integer, String).
        // For cases that intentionally use wrong types we let the mock throw.
        if (tc.expectedException == ClassCastException.class) {
            when(mockMap.put(any(), any())).thenThrow(new ClassCastException("bad type"));
        } else if (tc.expectedException == IllegalArgumentException.class) {
            when(mockMap.put(any(), any())).thenThrow(new IllegalArgumentException("bad value"));
        } else if (tc.expectedException == UnsupportedOperationException.class) {
            when(mockMap.put(any(), any())).thenThrow(new UnsupportedOperationException("unmodifiable"));
        } else if (tc.expectedException == NullPointerException.class && "someKey".equals(tc.key)) {
            // Simulate null strategy by setting the field to null later.
        } else if (tc.expectedException == NullPointerException.class && (tc.key == null || tc.value == null)) {
            when(mockMap.put(isNull(), any())).thenThrow(new NullPointerException("null key"));
            when(mockMap.put(any(), isNull())).thenThrow(new NullPointerException("null value"));
        } else if (tc.expectedException == RuntimeException.class) {
            when(mockMap.put(any(), any())).thenThrow(new RuntimeException("strategy failure"));
        } else {
            // Normal behaviour: return the configured previous value (may be null)
            when(mockMap.put(any(), any())).thenReturn(tc.previousValue);
        }
        // Set up the cache with the mocked map.
        setUpWithMockMap(mockMap);
        // Special case: test shutdown behaviour.
        if (tc.expectedException == IllegalStateException.class) {
            lruCache.shutdown();
        }
        // Special case: null strategy field.
        if (tc.expectedException == NullPointerException.class && "someKey".equals(tc.key)) {
            // Overwrite the private field with null via reflection.
            Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            f.set(lruCache, null);
        }
        // Execute the put operation and verify expectations.
        if (tc.expectedException != null) {
            assertThrows(tc.expectedException, () -> lruCache.put((Integer) tc.key, (String) tc.value), tc.description);
        } else {
            // Normal return value check.
            String result = lruCache.put((Integer) tc.key, (String) tc.value);
            assertEquals(tc.previousValue, result, tc.description);
            // Verify that the underlying map's put was invoked with the same arguments.
            verify(mockMap, times(1)).put((Integer) tc.key, (String) tc.value);
        }
    }
}
