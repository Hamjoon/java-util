package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_hashCode_Test {

    private static final int CAPACITY = 10;

    private LockingLRUCacheStrategy<String, Integer> cache;

    /**
     * Helper class whose {@code hashCode()} always throws.
     */
    private static final class BadHash {

        private final String name;

        BadHash(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            throw new IllegalStateException("BadHash for " + name);
        }

        @Override
        public String toString() {
            return "BadHash{" + name + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared after each test to avoid cross‑test interference
        cache.clear();
    }

    /**
     * Supplies data for {@link #hashCodeBranchCoverage(String, List, int)}.
     */
    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(Arguments.of("Empty cache – should be 1", Collections.emptyList(), 1), Arguments.of("Single (null,null) entry – 961", Collections.singletonList(new AbstractMap.SimpleEntry<>(null, null)), computeExpectedHash(Collections.singletonList(new AbstractMap.SimpleEntry<>(null, null)))), Arguments.of("Null key, non‑null value", Collections.singletonList(new AbstractMap.SimpleEntry<>(null, "val")), computeExpectedHash(Collections.singletonList(new AbstractMap.SimpleEntry<>(null, "val")))), Arguments.of("Non‑null key, null value", Collections.singletonList(new AbstractMap.SimpleEntry<>("key", null)), computeExpectedHash(Collections.singletonList(new AbstractMap.SimpleEntry<>("key", null)))), Arguments.of("Two non‑null entries", Arrays.asList(new AbstractMap.SimpleEntry<>("k1", "v1"), new AbstractMap.SimpleEntry<>("k2", "v2")), computeExpectedHash(Arrays.asList(new AbstractMap.SimpleEntry<>("k1", "v1"), new AbstractMap.SimpleEntry<>("k2", "v2")))), Arguments.of("Mixed null/non‑null entries", Arrays.asList(new AbstractMap.SimpleEntry<>("a", "b"), new AbstractMap.SimpleEntry<>(null, "c"), new AbstractMap.SimpleEntry<>("d", null)), computeExpectedHash(Arrays.asList(new AbstractMap.SimpleEntry<>("a", "b"), new AbstractMap.SimpleEntry<>(null, "c"), new AbstractMap.SimpleEntry<>("d", null)))));
    }

    /**
     * Utility to compute the expected hash using the same algorithm as the class under test.
     */
    private static int computeExpectedHash(List<Map.Entry<Object, Object>> entries) {
        int hash = 1;
        for (Map.Entry<Object, Object> e : entries) {
            Object k = e.getKey();
            Object v = e.getValue();
            hash = 31 * hash + (k == null ? 0 : k.hashCode());
            hash = 31 * hash + (v == null ? 0 : v.hashCode());
        }
        return hash;
    }

    /**
     * Verify that hashCode() returns the same value as a manual calculation that follows the
     * contract used inside LockingLRUCacheStrategy.hashCode().
     */
    @Test
    void hashCodeMatchesManualComputation() {
        // given
        cache.put("one", 1);
        cache.put("two", 2);
        // test handling of null key/value
        cache.put(null, null);
        cache.put("three", null);
        cache.put(null, 5);
        // when
        int actualHash = cache.hashCode();
        // then – compute expected hash using the same algorithm
        int expected = 1;
        // The iteration order is LRU order (head.next ... tail). Since we inserted sequentially,
        // the order will be the same as insertion for this simple test.
        Object[][] entries = { { "one", 1 }, { "two", 2 }, { null, null }, { "three", null }, { null, 5 } };
        for (Object[] e : entries) {
            Object key = e[0];
            Object value = e[1];
            expected = 31 * expected + (key == null ? 0 : key.hashCode());
            expected = 31 * expected + (value == null ? 0 : value.hashCode());
        }
        assertEquals(expected, actualHash, "LockingLRUCacheStrategy.hashCode() should produce the same result as the manual algorithm");
    }

    /**
     * Ensure that an empty cache returns the initial hash code value (1) as defined in the implementation.
     */
    @Test
    void hashCodeOfEmptyCacheIsOne() {
        // given – cache is newly created and empty
        // when
        int hash = cache.hashCode();
        // then
        assertEquals(1, hash, "Empty LockingLRUCacheStrategy should have hash code 1");
    }

    /**
     * Verify that hashCode is consistent across multiple invocations when the cache content does not change.
     */
    @Test
    void hashCodeIsConsistentWhenUnmodified() {
        // given
        cache.put("alpha", 100);
        cache.put("beta", 200);
        int first = cache.hashCode();
        // when
        int second = cache.hashCode();
        int third = cache.hashCode();
        // then
        assertEquals(first, second, "hashCode should be consistent across calls");
        assertEquals(first, third, "hashCode should remain consistent across multiple calls");
    }

    @Test
    void hashCodeWithSingleNullKeyAndNullValue() {
        // Cache contains one entry: (null, null)
        cache.put(null, null);
        int actual = cache.hashCode();
        // Expected: start 1 -> 31*1+0 =31 -> 31*31+0 =961
        int expected = 31 * 1 + 0;
        expected = 31 * expected + 0;
        assertEquals(expected, actual, "hashCode with a single (null,null) entry should follow the null‑handling branches");
    }

    @Test
    void hashCodeWithNullKeyNonNullValue() {
        // Cache contains one entry: (null, "val")
        cache.put(null, "val");
        int actual = cache.hashCode();
        // null key contributes 0
        int expected = 31 * 1 + 0;
        expected = 31 * expected + "val".hashCode();
        assertEquals(expected, actual, "hashCode should treat a null key correctly while using the value's hash");
    }

    @Test
    void hashCodeWithNonNullKeyNullValue() {
        // Cache contains one entry: ("key", null)
        cache.put("key", null);
        int actual = cache.hashCode();
        // key contributes
        int expected = 31 * 1 + "key".hashCode();
        // null value contributes 0
        expected = 31 * expected + 0;
        assertEquals(expected, actual, "hashCode should treat a null value correctly while using the key's hash");
    }

    @Test
    void hashCodeWithMultipleNonNullEntries() {
        // Cache contains two entries: ("k1","v1") and ("k2","v2")
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        int actual = cache.hashCode();
        int expected = 1;
        expected = 31 * expected + "k1".hashCode();
        expected = 31 * expected + "v1".hashCode();
        expected = 31 * expected + "k2".hashCode();
        expected = 31 * expected + "v2".hashCode();
        assertEquals(expected, actual, "hashCode should accumulate hashes of multiple non‑null entries in LRU order");
    }

    @Test
    void hashCodeWithMixedNullAndNonNullEntries() {
        // Entries: ("a","b"), (null,"c"), ("d",null)
        cache.put("a", "b");
        cache.put(null, "c");
        cache.put("d", null);
        int actual = cache.hashCode();
        int expected = 1;
        expected = 31 * expected + "a".hashCode();
        expected = 31 * expected + "b".hashCode();
        // null key
        expected = 31 * expected + 0;
        expected = 31 * expected + "c".hashCode();
        expected = 31 * expected + "d".hashCode();
        // null value
        expected = 31 * expected + 0;
        assertEquals(expected, actual, "hashCode should correctly handle a mixture of null and non‑null keys/values");
    }

    @Test
    void hashCodeOrderSensitivity_InsertionOrderAThenB() {
        cache.put("first", "1");
        cache.put("second", "2");
        int actual = cache.hashCode();
        int expected = 1;
        expected = 31 * expected + "first".hashCode();
        expected = 31 * expected + "1".hashCode();
        expected = 31 * expected + "second".hashCode();
        expected = 31 * expected + "2".hashCode();
        assertEquals(expected, actual, "hashCode must reflect insertion order A→B");
    }

    @Test
    void hashCodeOrderSensitivity_InsertionOrderBThenA() {
        cache.put("second", "2");
        cache.put("first", "1");
        int actual = cache.hashCode();
        int expected = 1;
        expected = 31 * expected + "second".hashCode();
        expected = 31 * expected + "2".hashCode();
        expected = 31 * expected + "first".hashCode();
        expected = 31 * expected + "1".hashCode();
        assertNotEquals(cache.hashCode(), new LockingLRUCacheStrategy<String, Integer>(CAPACITY) {

            {
                put("first", "1");
                put("second", "2");
            }
        }.hashCode(), "Different insertion order must yield different hashCode values");
        assertEquals(expected, actual, "hashCode must reflect insertion order B→A");
    }

    @Test
    void hashCodeThrowsWhenHeadIsNull() throws Exception {
        // Create a fresh cache (empty) and then set its private 'head' field to null.
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(cache, null);
        // Expect a NullPointerException when hashCode() tries to dereference head.next
        assertThrows(NullPointerException.class, cache::hashCode, "hashCode should throw NPE if internal head reference is null");
    }

    @Test
    void hashCodeThrowsWhenTailIsNull() throws Exception {
        // Populate cache with a couple of entries first.
        cache.put("x", 1);
        cache.put("y", 2);
        // Now null out the private 'tail' field.
        Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
        tailField.setAccessible(true);
        tailField.set(cache, null);
        // Accessing node != tail will eventually evaluate node != null, and when the
        // loop attempts to read node.key/value on a null node a NPE is thrown.
        assertThrows(NullPointerException.class, cache::hashCode, "hashCode should throw NPE if internal tail reference is null");
    }

    @Test
    void hashCodePropagatesExceptionFromKeyHashCode() {
        BadHash badKey = new BadHash("key");
        // value is a normal Integer
        cache.put(badKey, 123);
        IllegalStateException ex = assertThrows(IllegalStateException.class, cache::hashCode, "Exception from key.hashCode() should propagate");
        assertTrue(ex.getMessage().contains("BadHash"), "Exception message should contain identifying information");
    }

    @Test
    void hashCodePropagatesExceptionFromValueHashCode() {
        BadHash badValue = new BadHash("value");
        cache.put("normalKey", badValue);
        IllegalStateException ex = assertThrows(IllegalStateException.class, cache::hashCode, "Exception from value.hashCode() should propagate");
        assertTrue(ex.getMessage().contains("BadHash"), "Exception message should contain identifying information");
    }

    /**
     * Parameterized test that runs through a collection of entry‑sets
     * defined in {@link #branchCoverageProvider()} and verifies the
     * computed hash matches a manual calculation.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("branchCoverageProvider")
    void hashCodeBranchCoverage(String description, List<Map.Entry<Object, Object>> entries, int expectedHash) {
        // Populate the cache with the supplied entries.
        for (Map.Entry<Object, Object> e : entries) {
            // The cache is generic <String,Integer> but we can safely cast because the
            // test data only uses String/Integer or null. For null we just put it directly.
            @SuppressWarnings("unchecked")
            Map.Entry<String, Integer> cast = (Map.Entry<String, Integer>) e;
            cache.put(cast.getKey(), cast.getValue());
        }
        int actual = cache.hashCode();
        assertEquals(expectedHash, actual, description);
    }
}
