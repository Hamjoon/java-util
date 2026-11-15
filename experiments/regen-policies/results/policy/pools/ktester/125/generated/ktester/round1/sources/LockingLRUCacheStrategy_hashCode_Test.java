package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_hashCode_Test {

    /**
     * Instance under test – created fresh for each test method.
     */
    private LockingLRUCacheStrategy<Integer, String> cache;

    /**
     * Capacity used for all cache instances in this test class.
     */
    private static final int CAPACITY = 10;

    /**
     * Simple custom key whose {@code hashCode()} returns a fixed number.
     */
    private static final class CustomKey {

        private final int fixed;

        CustomKey(int fixed) {
            this.fixed = fixed;
        }

        @Override
        public int hashCode() {
            return fixed;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    /**
     * Simple custom value whose {@code hashCode()} returns a fixed number.
     */
    private static final class CustomValue {

        private final int fixed;

        CustomValue(int fixed) {
            this.fixed = fixed;
        }

        @Override
        public int hashCode() {
            return fixed;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    /**
     * Key whose hashCode() deliberately throws.
     */
    private static final class ThrowingKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("key hashCode failure");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    /**
     * Value whose hashCode() deliberately throws.
     */
    private static final class ThrowingValue {

        @Override
        public int hashCode() {
            throw new RuntimeException("value hashCode failure");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor only needs the capacity; the underlying map is
        // thread‑safe and does not require further configuration.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to make sure no state leaks between tests.
        cache.clear();
    }

    /**
     * Helper to obtain the private {@code head} field via reflection.
     */
    private static Field getHeadField() throws NoSuchFieldException {
        Field f = LockingLRUCacheStrategy.class.getDeclaredField("head");
        f.setAccessible(true);
        return f;
    }

    /**
     * Helper to obtain the private {@code next} field of the inner Node class.
     */
    private static Field getNodeNextField() throws Exception {
        Class<?> nodeClass = Class.forName("com.cedarsoftware.util.cache.LockingLRUCacheStrategy$Node");
        Field f = nodeClass.getDeclaredField("next");
        f.setAccessible(true);
        return f;
    }

    /**
     * Helper to obtain the private {@code key} field of the inner Node class.
     */
    private static Field getNodeKeyField() throws Exception {
        Class<?> nodeClass = Class.forName("com.cedarsoftware.util.cache.LockingLRUCacheStrategy$Node");
        Field f = nodeClass.getDeclaredField("key");
        f.setAccessible(true);
        return f;
    }

    /**
     * The hash code of an empty cache must be the initial value (1) because
     * the implementation starts with {@code int hashCode = 1} and never
     * modifies it when there are no entries.
     */
    @Test
    void hashCode_ofEmptyCache_isOne() {
        int actual = cache.hashCode();
        assertEquals(1, actual, "Hash code of an empty cache should be 1");
    }

    /**
     * Verify that {@code hashCode()} produces the same result as the algorithm
     * described in the Javadoc (i.e. the same as iterating over the entries and
     * combining the hash codes of keys and values).
     *
     * The test inserts a mixture of regular and {@code null} keys/values,
     * then recomputes the expected hash code using only the public
     * {@code entrySet()} iterator.
     */
    @Test
    void hashCode_matchesManualComputation() {
        // Populate the cache with a deterministic set of entries.
        cache.put(1, "one");
        cache.put(2, null);
        cache.put(null, "nullKey");
        cache.put(3, "three");
        // Manually compute the expected hash code using the same algorithm
        // that the implementation uses.
        int expected = 1;
        for (Map.Entry<Integer, String> e : cache.entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();
            expected = 31 * expected + (key == null ? 0 : key.hashCode());
            expected = 31 * expected + (value == null ? 0 : value.hashCode());
        }
        int actual = cache.hashCode();
        assertEquals(expected, actual, "hashCode() should follow the documented combination algorithm");
    }

    /**
     * The hash code must be stable – repeated invocations without intervening
     * modifications should return the same value.
     */
    @Test
    void hashCode_isStableAcrossCalls() {
        cache.put(10, "ten");
        cache.put(20, "twenty");
        int first = cache.hashCode();
        int second = cache.hashCode();
        int third = cache.hashCode();
        assertAll(() -> assertEquals(first, second, "Second call should match first"), () -> assertEquals(first, third, "Third call should match first"));
    }

    /**
     * After a mutation (put or remove) the hash code must reflect the new
     * state. This test adds an entry, records the hash, removes it, and checks
     * that the hash code changes.
     */
    @Test
    void hashCode_changesAfterMutation() {
        cache.put(5, "five");
        int hashAfterPut = cache.hashCode();
        cache.remove(5);
        int hashAfterRemove = cache.hashCode();
        assertNotEquals(hashAfterPut, hashAfterRemove, "Hash code should change after the cache content changes");
    }

    @Test
    void hashCode_singleEntry_nullKeyAndNullValue() {
        cache.put(null, null);
        // 1 -> key null adds 0 => 31, value null adds 0 => 31*31 = 961
        assertEquals(961, cache.hashCode(), "Hash code with a single null key/value should be 961");
    }

    @Test
    void hashCode_singleEntry_nonNullKeyAndValue() {
        cache.put("A", "B");
        int expected = 1;
        expected = 31 * expected + "A".hashCode();
        expected = 31 * expected + "B".hashCode();
        assertEquals(expected, cache.hashCode(), "Hash code with a single non‑null entry should match manual calculation");
    }

    @Test
    void hashCode_multipleMixedEntries() {
        cache.put("X", null);
        cache.put(null, "Y");
        cache.put("Z", "W");
        int expected = 1;
        for (Map.Entry<Integer, String> e : cache.entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();
            expected = 31 * expected + (key == null ? 0 : key.hashCode());
            expected = 31 * expected + (value == null ? 0 : value.hashCode());
        }
        assertEquals(expected, cache.hashCode(), "Hash code with mixed null/non‑null entries should follow the algorithm");
    }

    @Test
    void hashCode_duplicateEntries_identicalKeyAndValue() {
        cache.put("same", "same");
        // second put moves the entry to the front (LRU order)
        cache.put("same", "same");
        int expected = 1;
        // iteration order is LRU (most recent first). After the second put the map contains a single entry.
        // However the underlying implementation keeps a node per put; the test follows the public contract:
        for (Map.Entry<Integer, String> e : cache.entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();
            expected = 31 * expected + (key == null ? 0 : key.hashCode());
            expected = 31 * expected + (value == null ? 0 : value.hashCode());
        }
        assertEquals(expected, cache.hashCode(), "Hash code must reflect iteration order, not just set uniqueness");
    }

    @Test
    void hashCode_nullKeyOnly() {
        cache.put(null, "V");
        int expected = 1;
        // null key
        expected = 31 * expected + 0;
        // value hash
        expected = 31 * expected + "V".hashCode();
        assertEquals(expected, cache.hashCode(), "Hash code with null key and non‑null value should incorporate value's hash");
    }

    @Test
    void hashCode_nullValueOnly() {
        cache.put("K", null);
        int expected = 1;
        // key hash
        expected = 31 * expected + "K".hashCode();
        // null value
        expected = 31 * expected + 0;
        assertEquals(expected, cache.hashCode(), "Hash code with non‑null key and null value should incorporate key's hash");
    }

    @Test
    void hashCode_sameKeyDifferentValues() {
        cache.put("sameKey", "V1");
        // moves to front
        cache.put("sameKey", "V2");
        int expected = 1;
        for (Map.Entry<Integer, String> e : cache.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            expected = 31 * expected + (k == null ? 0 : k.hashCode());
            expected = 31 * expected + (v == null ? 0 : v.hashCode());
        }
        assertEquals(expected, cache.hashCode(), "Hash code must account for both key and value for each entry, even when keys repeat");
    }

    @Test
    void hashCode_differentKeysSameValue() {
        cache.put("K1", "sameVal");
        cache.put("K2", "sameVal");
        int expected = 1;
        for (Map.Entry<Integer, String> e : cache.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            expected = 31 * expected + (k == null ? 0 : k.hashCode());
            expected = 31 * expected + (v == null ? 0 : v.hashCode());
        }
        assertEquals(expected, cache.hashCode(), "Each distinct key should contribute its own hash even if values repeat");
    }

    @Test
    void hashCode_customObjectsWithOverriddenHashCode() {
        LockingLRUCacheStrategy<CustomKey, CustomValue> customCache = new LockingLRUCacheStrategy<>(CAPACITY);
        customCache.put(new CustomKey(123), new CustomValue(456));
        int expected = 1;
        // key hash
        expected = 31 * expected + 123;
        // value hash
        expected = 31 * expected + 456;
        assertEquals(expected, customCache.hashCode(), "Hash code must use the overridden hashCode() of custom key/value objects");
    }

    @Test
    void hashCode_afterRemovalReflectsRemainingEntry() {
        cache.put("A", "B");
        cache.put("C", "D");
        // remove first entry
        cache.remove("A");
        int expected = 1;
        for (Map.Entry<Integer, String> e : cache.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            expected = 31 * expected + (k == null ? 0 : k.hashCode());
            expected = 31 * expected + (v == null ? 0 : v.hashCode());
        }
        assertEquals(expected, cache.hashCode(), "Hash code after removal should reflect only the remaining mappings");
    }

    @Test
    void hashCode_afterClearReturnsInitialValue() {
        cache.put("X", "Y");
        cache.put("Z", "W");
        cache.clear();
        assertEquals(1, cache.hashCode(), "Clearing the cache must reset the hash code to the initial value 1");
    }

    @Test
    void hashCode_nullHead_throwsNullPointerException() throws Exception {
        // corrupt internal state: head = null
        Field head = getHeadField();
        head.set(cache, null);
        assertThrows(NullPointerException.class, cache::hashCode, "When head is null, hashCode() should throw NPE");
    }

    @Test
    void hashCode_headNextNull_throwsNullPointerException() throws Exception {
        // corrupt internal state: head.next = null
        Field head = getHeadField();
        Object headObj = head.get(cache);
        Field next = getNodeNextField();
        next.set(headObj, null);
        assertThrows(NullPointerException.class, cache::hashCode, "When head.next is null, hashCode() should throw NPE");
    }

    @Test
    void hashCode_breakChainBeforeTail_throwsNullPointerException() throws Exception {
        // Populate with two entries.
        cache.put("A", "B");
        cache.put("C", "D");
        // Locate the node that holds key "A".
        Field head = getHeadField();
        // this is the dummy head node
        Object node = head.get(cache);
        Field next = getNodeNextField();
        Field keyField = getNodeKeyField();
        // first real node (key "A")
        Object nodeA = next.get(node);
        Object keyA = keyField.get(nodeA);
        // ensure we have the right node (defensive)
        assertEquals("A", keyA);
        // break the chain after nodeA
        next.set(nodeA, null);
        assertThrows(NullPointerException.class, cache::hashCode, "Breaking the chain before reaching tail must cause NPE during hashCode()");
    }

    @Test
    void hashCode_keyHashCodeThrowsRuntimeException() {
        LockingLRUCacheStrategy<ThrowingKey, String> c = new LockingLRUCacheStrategy<>(CAPACITY);
        c.put(new ThrowingKey(), "value");
        RuntimeException ex = assertThrows(RuntimeException.class, c::hashCode, "Exception from key.hashCode() must propagate");
        assertTrue(ex.getMessage().contains("key hashCode failure"));
    }

    @Test
    void hashCode_valueHashCodeThrowsRuntimeException() {
        LockingLRUCacheStrategy<String, ThrowingValue> c = new LockingLRUCacheStrategy<>(CAPACITY);
        c.put("key", new ThrowingValue());
        RuntimeException ex = assertThrows(RuntimeException.class, c::hashCode, "Exception from value.hashCode() must propagate");
        assertTrue(ex.getMessage().contains("value hashCode failure"));
    }
}
