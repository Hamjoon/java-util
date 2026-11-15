package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.Objects;

@ExtendWith(MockitoExtension.class)
class TTLCache_hashCode_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Helper class whose {@code hashCode()} returns a constant value.
     */
    private static final class FixedHash {

        private final int value;

        FixedHash(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    /**
     * Key object whose hashCode throws IllegalStateException.
     */
    private static final class BadKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("BadKey hashCode");
        }
    }

    /**
     * Value object whose hashCode throws IllegalArgumentException.
     */
    private static final class BadValue {

        @Override
        public int hashCode() {
            throw new IllegalArgumentException("BadValue hashCode");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a very large TTL so that entries never expire during the test.
        ttlCache = new TTLCache<>(Long.MAX_VALUE, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid interference between tests.
        ttlCache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache.
        TTLCache.shutdown();
    }

    /**
     * An empty cache should return the initial hash value (1) as defined by the
     * implementation.
     */
    @Test
    void hashCodeEmptyCache() {
        // no entries → loop never executes
        int expected = 1;
        assertEquals(expected, ttlCache.hashCode(), "hashCode of an empty TTLCache should be 1");
    }

    /**
     * Verify hashCode for a cache with several non‑null entries. The order
     * of iteration is the insertion order because the internal linked list
     * preserves that order.
     */
    @Test
    void hashCodeWithMultipleEntries() {
        ttlCache.put(10, "Alpha");
        ttlCache.put(20, "Beta");
        ttlCache.put(30, "Gamma");
        int expected = 1;
        // entry 10 → "Alpha"
        expected = 31 * expected + Integer.valueOf(10).hashCode();
        expected = 31 * expected + "Alpha".hashCode();
        // entry 20 → "Beta"
        expected = 31 * expected + Integer.valueOf(20).hashCode();
        expected = 31 * expected + "Beta".hashCode();
        // entry 30 → "Gamma"
        expected = 31 * expected + Integer.valueOf(30).hashCode();
        expected = 31 * expected + "Gamma".hashCode();
        assertEquals(expected, ttlCache.hashCode(), "hashCode should reflect insertion order and values");
    }

    /**
     * Verify that null keys and null values are handled correctly (treated as 0).
     */
    @Test
    void hashCodeWithNullKeyAndValue() {
        ttlCache.put(null, "OnlyValue");
        ttlCache.put(42, null);
        ttlCache.put(null, null);
        int expected = 1;
        // entry null → "OnlyValue"
        // null key
        expected = 31 * expected + 0;
        expected = 31 * expected + "OnlyValue".hashCode();
        // entry 42 → null
        expected = 31 * expected + Integer.valueOf(42).hashCode();
        // null value
        expected = 31 * expected + 0;
        // entry null → null
        expected = 31 * expected + 0;
        expected = 31 * expected + 0;
        assertEquals(expected, ttlCache.hashCode(), "hashCode must treat null keys/values as 0");
    }

    /**
     * Ensure that repeated calls to hashCode produce the same result and do not
     * corrupt the internal state (i.e., the lock is correctly released).
     */
    @Test
    void hashCodeIdempotent() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        int first = ttlCache.hashCode();
        int second = ttlCache.hashCode();
        assertEquals(first, second, "hashCode should be deterministic across calls");
        // After calling hashCode we should still be able to modify the cache.
        ttlCache.put(3, "C");
        int afterModification = ttlCache.hashCode();
        assertNotEquals(first, afterModification, "hashCode should change after the cache content changes");
    }

    @Test
    void hashCodeSingleEntryNonNull() {
        ttlCache.put(1, 2);
        // Expected: ((31*1 + 1) *31 + 2) = 994
        assertEquals(994, ttlCache.hashCode(), "Single non‑null entry (1,2) should yield hash 994");
    }

    @Test
    void hashCodeSingleEntryNullKey() {
        ttlCache.put(null, 2);
        // Expected: ((31*1 + 0) *31 + 2) = 963
        assertEquals(963, ttlCache.hashCode(), "Single entry with null key should yield hash 963");
    }

    @Test
    void hashCodeSingleEntryNullValue() {
        ttlCache.put(1, null);
        // Expected: ((31*1 + 1) *31 + 0) = 992
        assertEquals(992, ttlCache.hashCode(), "Single entry with null value should yield hash 992");
    }

    @Test
    void hashCodeMultipleEntriesMixedNulls() {
        ttlCache.put(1, 2);
        ttlCache.put(null, 3);
        ttlCache.put(4, null);
        // Pre‑computed expected value from the specification.
        assertEquals(917_982_881, ttlCache.hashCode(), "Mixed null/non‑null entries should produce the documented hash");
    }

    @Test
    void hashCodeConsistencyAcrossInstances() {
        TTLCache<Integer, Integer> cacheA = new TTLCache<>(Long.MAX_VALUE, -1);
        TTLCache<Integer, Integer> cacheB = new TTLCache<>(Long.MAX_VALUE, -1);
        cacheA.put(5, 6);
        cacheB.put(5, 6);
        assertEquals(1122, cacheA.hashCode(), "First cache should produce hash 1122");
        assertEquals(1122, cacheB.hashCode(), "Second cache should produce the same hash 1122");
    }

    @Test
    void hashCodeMultipleNonNullEntries() {
        ttlCache.put(10, 20);
        ttlCache.put(30, 40);
        ttlCache.put(50, 60);
        ttlCache.put(70, 80);
        ttlCache.put(90, 100);
        // Expected value supplied in the JSON test case.
        assertEquals(-1_870_435_539, ttlCache.hashCode(), "Five non‑null entries should yield the documented hash");
    }

    @Test
    void hashCodeRemovalEffect() {
        ttlCache.put(1, 2);
        ttlCache.put(3, 4);
        ttlCache.remove(1);
        // Only (3,4) remains → ((31*1 + 3)*31 + 4) = 1058
        assertEquals(1058, ttlCache.hashCode(), "After removal of key 1 the hash should be 1058");
    }

    @Test
    void hashCodeClearResetsHash() {
        ttlCache.put(1, 1);
        ttlCache.put(2, 2);
        ttlCache.put(3, 3);
        ttlCache.clear();
        assertEquals(1, ttlCache.hashCode(), "After clear the hash must return to the initial value 1");
    }

    @Test
    void hashCodeCustomObjectHash() {
        FixedHash customKey = new FixedHash(42);
        FixedHash customValue = new FixedHash(99);
        // The cache is generic, so we need raw types for the custom objects.
        @SuppressWarnings("unchecked")
        TTLCache<Object, Object> rawCache = new TTLCache<>(Long.MAX_VALUE, -1);
        rawCache.put(customKey, customValue);
        // ((31*1 + 42) *31 + 99) = 2362
        assertEquals(2362, rawCache.hashCode(), "Custom objects with fixed hash codes should produce 2362");
    }

    @ParameterizedTest(name = "order {0} → expected {2}")
    @CsvSource({ "'1,2;3,4', 955331, 'Insertion order (1,2) then (3,4)'", "'3,4;1,2', 1016771, 'Insertion order (3,4) then (1,2)'" })
    void hashCodeOrderEffect(String entries, int expected, String _desc) {
        // entries format: "k1,v1;k2,v2"
        TTLCache<Integer, Integer> cache = new TTLCache<>(Long.MAX_VALUE, -1);
        for (String pair : entries.split(";")) {
            String[] kv = pair.split(",");
            Integer k = "null".equals(kv[0]) ? null : Integer.valueOf(kv[0]);
            Integer v = "null".equals(kv[1]) ? null : Integer.valueOf(kv[1]);
            cache.put(k, v);
        }
        assertEquals(expected, cache.hashCode(), "Hash code should reflect insertion order");
    }

    @Test
    void hashCodeAllNullEntries() {
        ttlCache.put(null, null);
        ttlCache.put(null, null);
        ttlCache.put(null, null);
        assertEquals(1, ttlCache.hashCode(), "All-null entries should leave the hash at its initial value 1");
    }

    @Test
    void hashCodeNullHeadThrowsNPE() throws Exception {
        // Use reflection to set the private 'head' field to null.
        Field headField = TTLCache.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(ttlCache, null);
        assertThrows(NullPointerException.class, ttlCache::hashCode, "When head is null, hashCode() should throw NPE");
    }

    @Test
    void hashCodeBrokenLinkThrowsNPE() throws Exception {
        // creates a single real node between head and tail
        ttlCache.put(1, 1);
        // Obtain the sentinel head node.
        Field headField = TTLCache.class.getDeclaredField("head");
        headField.setAccessible(true);
        Object headNode = headField.get(ttlCache);
        // The first real node is head.next
        Field nextField = headNode.getClass().getDeclaredField("next");
        nextField.setAccessible(true);
        Object realNode = nextField.get(headNode);
        // Break the link by setting realNode.next = null
        Field realNodeNext = realNode.getClass().getDeclaredField("next");
        realNodeNext.setAccessible(true);
        realNodeNext.set(realNode, null);
        assertThrows(NullPointerException.class, ttlCache::hashCode, "A broken linked list should cause NPE during hashCode computation");
    }

    @Test
    void hashCodeKeyHashThrows() {
        TTLCache<BadKey, Integer> cache = new TTLCache<>(Long.MAX_VALUE, -1);
        cache.put(new BadKey(), 1);
        IllegalStateException ex = assertThrows(IllegalStateException.class, cache::hashCode, "Exception from key.hashCode() should propagate");
        assertEquals("BadKey hashCode", ex.getMessage());
    }

    @Test
    void hashCodeValueHashThrows() {
        TTLCache<Integer, BadValue> cache = new TTLCache<>(Long.MAX_VALUE, -1);
        cache.put(1, new BadValue());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, cache::hashCode, "Exception from value.hashCode() should propagate");
        assertEquals("BadValue hashCode", ex.getMessage());
    }

    @Test
    void hashCodeNullLockThrowsNPE() throws Exception {
        // Set the private 'lock' field to null via reflection.
        Field lockField = TTLCache.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        lockField.set(ttlCache, null);
        assertThrows(NullPointerException.class, ttlCache::hashCode, "When lock is null, calling hashCode() should throw NPE");
    }
}
