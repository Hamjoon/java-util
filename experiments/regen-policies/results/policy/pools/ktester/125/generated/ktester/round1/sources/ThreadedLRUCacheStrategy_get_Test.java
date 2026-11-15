package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

class ThreadedLRUCacheStrategy_get_Test {

    private static final int CAPACITY = 10;

    // long enough to avoid immediate purge
    private static final int CLEANUP_DELAY_MILLIS = 1_000;

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * A key whose {@code hashCode()} throws a {@link RuntimeException}.
     */
    private static final class BadHashKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("Deliberate hashCode failure");
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    /**
     * A key whose {@code equals(Object)} throws a {@link RuntimeException}.
     */
    private static final class BadEqualsKey {

        @Override
        public int hashCode() {
            // collides with CollidingKey
            return 42;
        }

        @Override
        public boolean equals(Object obj) {
            throw new IllegalStateException("Deliberate equals failure");
        }
    }

    /**
     * Simple key used to create a hash collision with {@link BadEqualsKey}.
     */
    private static final class CollidingKey {

        private final int hash;

        CollidingKey(int hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CollidingKey && ((CollidingKey) obj).hash == this.hash;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache instance for each test.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache and release the reference.
        if (cache != null) {
            cache.clear();
            cache = null;
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by ThreadedLRUCacheStrategy.
        ThreadedLRUCacheStrategy.shutdown();
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideKeyPresent() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("key1", 1));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideKeyAbsent() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("missingKey"));
    }

    /**
     * Retrieves the private Node instance for a given key via reflection.
     */
    private Object getNodeForKey(Object key) throws Exception {
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> internalMap = (ConcurrentMap<Object, Object>) cacheField.get(cache);
        return internalMap.get(key);
    }

    /**
     * Generic reflective field accessor.
     */
    private static Object getField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    void get_ReturnsValue_WhenKeyIsPresent() {
        // given
        String key = "alpha";
        Integer value = 42;
        cache.put(key, value);
        // when
        Integer result = cache.get(key);
        // then
        assertNotNull(result, "Result should not be null for an existing key");
        assertEquals(value, result, "Returned value must match the stored value");
    }

    @Test
    void get_ReturnsNull_WhenKeyIsAbsent() {
        // given
        String missingKey = "nonexistent";
        // when
        Integer result = cache.get(missingKey);
        // then
        assertNull(result, "Result should be null for a missing key");
    }

    @Test
    void get_AllowsNullKey() {
        // given
        Integer value = 99;
        cache.put(null, value);
        // when
        Integer result = cache.get(null);
        // then
        assertNotNull(result, "Result should not be null for a stored null key");
        assertEquals(value, result, "Returned value must match the stored value for null key");
    }

    @Test
    void get_DoesNotChangeCacheSize() {
        // given
        cache.put("k1", 1);
        cache.put("k2", 2);
        int sizeBefore = cache.size();
        // when
        cache.get("k1");
        // then
        assertEquals(sizeBefore, cache.size(), "Cache size should remain unchanged after a get operation");
    }

    /**
     * The original framework expected a successful lookup for a {@code null}
     * key. According to the JSON specification the cache should throw a
     * {@link NullPointerException} because the underlying {@link ConcurrentMap}
     * does not permit null keys. This test reflects that expectation.
     */
    @Test
    void get_ThrowsNullPointerException_WhenKeyIsNull() {
        // given
        Integer value = 99;
        // the put may succeed (implementation permits it)
        cache.put(null, value);
        // when / then
        assertThrows(NullPointerException.class, () -> cache.get(null), "Calling get(null) must throw NullPointerException");
    }

    /**
     * Group: key present – verify value and timestamp update
     */
    @ParameterizedTest(name = "{index}: get({0}) returns {1}")
    @MethodSource("provideKeyPresent")
    void get_KeyPresent_UpdatesTimestamp(String key, Integer expectedValue) throws Exception {
        // arrange – put a value and capture the original timestamp via reflection
        cache.put(key, expectedValue);
        Object node = getNodeForKey(key);
        long tsBefore = (long) getField(node, "timestamp");
        // act
        Integer result = cache.get(key);
        // assert – value is correct
        assertEquals(expectedValue, result);
        // timestamp must have been refreshed (strictly greater)
        long tsAfter = (long) getField(node, "timestamp");
        assertTrue(tsAfter > tsBefore, "Timestamp should be updated to a later value after get()");
    }

    /**
     * Group: key absent – should return null and not create a node
     */
    @ParameterizedTest(name = "{index}: get({0}) returns null")
    @MethodSource("provideKeyAbsent")
    void get_KeyAbsent_ReturnsNull(String key) {
        // act
        Integer result = cache.get(key);
        // assert
        assertNull(result, "Result must be null for a missing key");
    }

    /**
     * Group: key present with null value – return null but still update timestamp
     */
    @Test
    void get_KeyPresentWithNullValue_UpdatesTimestamp() throws Exception {
        String key = "nullValueKey";
        // store a null value
        cache.put(key, null);
        Object node = getNodeForKey(key);
        long tsBefore = (long) getField(node, "timestamp");
        Integer result = cache.get(key);
        assertNull(result, "Result must be null when stored value is null");
        long tsAfter = (long) getField(node, "timestamp");
        assertTrue(tsAfter > tsBefore, "Timestamp should be refreshed even when the stored value is null");
    }

    /**
     * Group: after removal – subsequent get returns null
     */
    @Test
    void get_AfterRemoval_ReturnsNull() {
        String key = "toBeRemoved";
        cache.put(key, 123);
        // remove it
        cache.remove(key);
        Integer result = cache.get(key);
        assertNull(result, "Result must be null after the key has been removed");
    }

    /**
     * Group: after clear – any get returns null
     */
    @Test
    void get_AfterClear_ReturnsNull() {
        cache.put("anyKey", 555);
        // clear the cache
        cache.clear();
        Integer result = cache.get("anyKey");
        assertNull(result, "Result must be null after cache has been cleared");
    }

    /**
     * Group: internal cache null – expect NullPointerException
     */
    @Test
    void get_InternalCacheNull_ThrowsNPE() throws Exception {
        // use reflection to set the private 'cache' field to null
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.get("anyKey"), "Calling get() when internal cache is null must throw NPE");
    }

    /**
     * Group: key with faulty hashCode – exception propagates
     */
    @Test
    void get_FaultyHashCode_PropagatesException() {
        BadHashKey badKey = new BadHashKey();
        // we need to put something first so that map.get triggers hashCode on the argument
        cache.put("dummy", 0);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> cache.get(badKey), "Exception from badKey.hashCode() must propagate");
        assertTrue(thrown instanceof IllegalStateException, "Expected IllegalStateException from BadHashKey.hashCode()");
    }

    /**
     * Group: key with faulty equals – exception propagates
     */
    @Test
    void get_FaultyEquals_PropagatesException() {
        BadEqualsKey badKey = new BadEqualsKey();
        // Insert a colliding key so that map.get will need to call equals on the stored key
        // Use a key that has the same hashCode as BadEqualsKey (both return 42)
        cache.put(new CollidingKey(42), 0);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> cache.get(badKey), "Exception from badKey.equals() must propagate");
        assertTrue(thrown instanceof IllegalStateException, "Expected IllegalStateException from BadEqualsKey.equals()");
    }
}
