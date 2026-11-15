package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_hashCode_Test {

    /**
     * Default capacity and cleanup delay used for all tests.
     */
    private static final int CAPACITY = 10;

    private static final int CLEANUP_DELAY_MILLIS = 100;

    /**
     * Instance under test – created fresh for each test method.
     */
    private ThreadedLRUCacheStrategy<String, String> cache;

    class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad key");
        }
    }

    class BadValue {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad value");
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad key");
        }
    }

    class BadValue {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad value");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a new cache for every test to guarantee isolation.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid side‑effects between tests.
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by ThreadedLRUCacheStrategy
        // to prevent thread‑leak warnings when the JVM exits.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Creates a {@code ThreadedLRUCacheStrategy.Node} instance via reflection,
     * sets its {@code key} and {@code value} fields, and puts it into the
     * internal {@code cache} map.
     *
     * @param key   the key to store inside the Node (may be null)
     * @param value the value to store inside the Node (may be null)
     */
    private void injectNodeIntoCache(Object key, Object value) throws Exception {
        // Obtain the private `cache` field.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> internalMap = (ConcurrentMap<Object, Object>) cacheField.get(cache);
        // Load the inner Node class (it is a static nested class).
        Class<?> nodeClass = Class.forName("com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy$Node");
        // Instantiate the Node via a no‑arg constructor (if present). If not,
        // use the default constructor provided by reflection.
        Constructor<?> ctor = nodeClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object nodeInstance = ctor.newInstance();
        // Set the `key` and `value` fields directly.
        Field keyField = nodeClass.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(nodeInstance, key);
        Field valueField = nodeClass.getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(nodeInstance, value);
        // Use the same object as the map key; the implementation stores the
        // Node under the original key, but for our tests the exact map key
        // does not matter.
        internalMap.put(key, nodeInstance);
    }

    @Test
    void hashCode_EmptyCache_ReturnsBaseValue() {
        // An empty cache never enters the loop, so the result must be the initial value 1.
        assertEquals(1, cache.hashCode(), "hashCode of an empty cache should be 1");
    }

    @Test
    void hashCode_WithMultipleEntries_MatchesManualComputation() {
        // Populate the cache with a deterministic set of entries.
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        // test handling of a null key
        cache.put(null, "nullKey");
        // test handling of a null value
        cache.put("nullValue", null);
        // both key and value null
        cache.put(null, null);
        // Manually compute the expected hash code using the same algorithm
        // that ThreadedLRUCacheStrategy.hashCode() employs.
        int expected = 1;
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            expected = 31 * expected + (key == null ? 0 : key.hashCode());
            expected = 31 * expected + (value == null ? 0 : value.hashCode());
        }
        assertEquals(expected, cache.hashCode(), "hashCode should be equal to the manually computed value");
    }

    @Test
    void hashCode_DeterministicAfterRehashing() {
        // Insert enough entries to trigger internal rehashing in the underlying map.
        for (int i = 0; i < 20; i++) {
            cache.put("k" + i, "v" + i);
        }
        // Compute expected hash in the same way as the implementation.
        int expected = 1;
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            expected = 31 * expected + (key == null ? 0 : key.hashCode());
            expected = 31 * expected + (value == null ? 0 : value.hashCode());
        }
        assertEquals(expected, cache.hashCode(), "hashCode must remain consistent even after internal resizing");
    }

    @Test
    void hashCode_SingleEntry_NonNullKeyAndValue() {
        // a -> b  (both non‑null)
        cache.put("a", "b");
        // Expected value calculated with the same algorithm as the implementation.
        // 1 -> (31*1 + "a".hashCode()) -> (31* (31+97) + "b".hashCode()) = 4066
        int expected = 1;
        expected = 31 * expected + "a".hashCode();
        expected = 31 * expected + "b".hashCode();
        assertEquals(expected, cache.hashCode(), "hashCode should match the pre‑computed value for a single entry a→b");
    }

    @Test
    void hashCode_SingleEntry_NullKey() {
        // null key, value = "b"
        cache.put(null, "b");
        int expected = 1;
        // null key contributes 0
        expected = 31 * expected + 0;
        // value hash
        expected = 31 * expected + "b".hashCode();
        assertEquals(expected, cache.hashCode(), "hashCode should treat a null key as 0");
    }

    @Test
    void hashCode_SingleEntry_NullValue() {
        // key = "a", null value
        cache.put("a", null);
        int expected = 1;
        // key hash
        expected = 31 * expected + "a".hashCode();
        // null value contributes 0
        expected = 31 * expected + 0;
        assertEquals(expected, cache.hashCode(), "hashCode should treat a null value as 0");
    }

    @Test
    void hashCode_NullInternalCache_ThrowsNullPointerException() throws Exception {
        // Use reflection to set the private final field `cache` to null.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        // The method should now throw NPE when trying to iterate over `cache.values()`.
        assertThrows(NullPointerException.class, cache::hashCode, "hashCode should throw NPE when internal cache is null");
    }

    @Test
    void hashCode_KeyHashCodeThrowsRuntimeException() throws Exception {
        // Prepare a key whose hashCode() deliberately throws.
        class BadKey {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad key");
            }
        }
        BadKey badKey = new BadKey();
        // Insert a custom Node containing the BadKey into the internal map.
        injectNodeIntoCache(badKey, "someValue");
        // The RuntimeException from BadKey.hashCode() must propagate.
        RuntimeException ex = assertThrows(RuntimeException.class, cache::hashCode);
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    void hashCode_ValueHashCodeThrowsRuntimeException() throws Exception {
        // Prepare a value whose hashCode() deliberately throws.
        class BadValue {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad value");
            }
        }
        BadValue badValue = new BadValue();
        // Insert a custom Node containing the BadValue into the internal map.
        injectNodeIntoCache("someKey", badValue);
        // The RuntimeException from BadValue.hashCode() must propagate.
        RuntimeException ex = assertThrows(RuntimeException.class, cache::hashCode);
        assertEquals("bad value", ex.getMessage());
    }

    @Test
    void hashCode_NullNodeEntry_ThrowsNullPointerException() throws Exception {
        // Directly put a null Node into the internal map.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, Object> internalMap = (ConcurrentMap<String, Object>) cacheField.get(cache);
        // null Node
        internalMap.put("anyKey", null);
        // Accessing node.key will cause a NPE.
        assertThrows(NullPointerException.class, cache::hashCode, "hashCode should throw NPE when a null Node is present in the map");
    }
}
