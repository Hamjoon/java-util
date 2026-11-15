package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_hashCode_Test {

    private LockingLRUCacheStrategy<String, String> cache;

    /**
     * Simple holder for a description and the expected hashCode.
     * Using a dedicated class makes the {@code @MethodSource} signatures tidy.
     */
    private static class ArgumentsHolder {

        final String description;

        final int expected;

        ArgumentsHolder(String description, int expected) {
            this.description = description;
            this.expected = expected;
        }

        @Override
        public String toString() {
            // JUnit5 uses this for the test name placeholder {0}
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // capacity is arbitrary for the test – we just need a working instance
        cache = new LockingLRUCacheStrategy<>(10);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to avoid any side‑effects between tests
        cache.clear();
        cache = null;
    }

    /**
     * Provides arguments for the {@code hashCode_emptyCache} test group.
     */
    private static Stream<ArgumentsHolder> emptyCacheProvider() {
        return Stream.of(new ArgumentsHolder("When the cache contains no entries (head.next == tail), the loop is never entered and the method should return the initial hashCode value of 1.", 1));
    }

    /**
     * Provides arguments for the {@code hashCode_singleEntry} test group.
     */
    private static Stream<ArgumentsHolder> singleEntryProvider() {
        // key = 1 (Integer), value = "a"
        // pre‑computed in the JSON spec
        int expected = 1089;
        return Stream.of(new ArgumentsHolder("Cache contains a single entry (key=1, value=\"a\").", expected));
    }

    /**
     * Provides arguments for the {@code hashCode_multipleEntries} test group.
     */
    private static Stream<ArgumentsHolder> multipleEntriesProvider() {
        // Two entries inserted in order: (1,"a") then (2,"b")
        // as documented
        int expected = 1_046_689;
        return Stream.of(new ArgumentsHolder("Cache contains two entries inserted in order: (1,\"a\") then (2,\"b\").", expected));
    }

    /**
     * Provides arguments for the {@code hashCode_nullKey} test group.
     */
    private static Stream<ArgumentsHolder> nullKeyProvider() {
        // Single entry: key = null, value = "a"
        // as documented
        int expected = 1_058;
        return Stream.of(new ArgumentsHolder("Cache contains a single entry with a null key and value \"a\".", expected));
    }

    /**
     * Provides arguments for the {@code hashCode_nullValue} test group.
     */
    private static Stream<ArgumentsHolder> nullValueProvider() {
        // Single entry: key = 1, value = null
        // as documented
        int expected = 992;
        return Stream.of(new ArgumentsHolder("Cache contains a single entry with key 1 and a null value.", expected));
    }

    /**
     * Provides arguments for the {@code hashCode_nullKeyAndValue} test group.
     */
    private static Stream<ArgumentsHolder> nullKeyAndValueProvider() {
        // Single entry: key = null, value = null
        // as documented
        int expected = 961;
        return Stream.of(new ArgumentsHolder("Cache contains a single entry where both key and value are null.", expected));
    }

    /**
     * Provides arguments for the {@code hashCode_orderSensitivity} test group.
     */
    private static Stream<ArgumentsHolder> orderSensitivityProvider() {
        return Stream.of(new ArgumentsHolder("Two entries inserted as (1,\"a\") then (2,\"b\").", 1_046_689), new ArgumentsHolder("Two entries inserted in reverse order (2,\"b\") then (1,\"a\").", 1_077_409));
    }

    /**
     * Provides arguments for the {@code hashCode_consistency} test group.
     */
    private static Stream<ArgumentsHolder> consistencyProvider() {
        return Stream.of(new ArgumentsHolder("Calling hashCode twice on a cache with a single entry (1,\"a\") without any modifications should return the same value (1089).", 1089));
    }

    @Test
    void hashCodeEmptyCache_returnsOne() {
        // The implementation starts with hashCode = 1 and never enters the loop for an empty map
        assertEquals(1, cache.hashCode(), "hashCode of an empty LockingLRUCacheStrategy should be 1");
    }

    @Test
    void hashCodeWithTwoEntries_matchesManualCalculation() {
        // Insert two entries.  The LRU implementation puts the most‑recent entry right after the dummy head.
        // first entry – becomes most‑recent
        cache.put("a", "1");
        // second entry – becomes most‑recent, order: b, a
        cache.put("b", "2");
        int expected = 1;
        // entry "b" -> "2"
        expected = 31 * expected + "b".hashCode();
        expected = 31 * expected + "2".hashCode();
        // entry "a" -> "1"
        expected = 31 * expected + "a".hashCode();
        expected = 31 * expected + "1".hashCode();
        assertEquals(expected, cache.hashCode(), "hashCode should reflect the LRU iteration order (most‑recent first)");
    }

    @Test
    void hashCodeChangesWhenAccessReordersEntries() {
        cache.put("a", "1");
        // order now: b, a
        cache.put("b", "2");
        // Access "a" – it becomes most‑recent, order changes to a, b
        cache.get("a");
        int expected = 1;
        // entry "a" -> "1"
        expected = 31 * expected + "a".hashCode();
        expected = 31 * expected + "1".hashCode();
        // entry "b" -> "2"
        expected = 31 * expected + "b".hashCode();
        expected = 31 * expected + "2".hashCode();
        assertEquals(expected, cache.hashCode(), "hashCode should reflect the new ordering after a get() that moves the entry to the front");
    }

    @Test
    void hashCodeIsConsistentAcrossMultipleCalls() {
        cache.put("x", "X");
        cache.put("y", "Y");
        int first = cache.hashCode();
        int second = cache.hashCode();
        assertEquals(first, second, "hashCode should be deterministic for the same cache state");
    }

    @Test
    void hashCodeHandlesNullKeyAndValue() {
        cache.put(null, null);
        cache.put("k", null);
        cache.put(null, "v");
        // Insertion order: last put becomes most‑recent, so order is (null,"v"), ("k",null), (null,null)
        int expected = 1;
        // key null
        expected = 31 * expected + (null == null ? 0 : null.hashCode());
        // value "v"
        expected = 31 * expected + "v".hashCode();
        expected = 31 * expected + "k".hashCode();
        // null value
        expected = 31 * expected + 0;
        // null key
        expected = 31 * expected + 0;
        // null value
        expected = 31 * expected + 0;
        assertEquals(expected, cache.hashCode(), "hashCode should correctly handle null keys and values");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("emptyCacheProvider")
    void hashCode_emptyCache(ArgumentsHolder holder) {
        // No entries are added – cache stays empty.
        assertEquals(holder.expected, cache.hashCode(), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("singleEntryProvider")
    void hashCode_singleEntry(ArgumentsHolder holder) {
        cache.put(1, "a");
        assertEquals(holder.expected, cache.hashCode(), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("multipleEntriesProvider")
    void hashCode_multipleEntries(ArgumentsHolder holder) {
        cache.put(1, "a");
        cache.put(2, "b");
        assertEquals(holder.expected, cache.hashCode(), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullKeyProvider")
    void hashCode_nullKey(ArgumentsHolder holder) {
        cache.put(null, "a");
        assertEquals(holder.expected, cache.hashCode(), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullValueProvider")
    void hashCode_nullValue(ArgumentsHolder holder) {
        cache.put(1, null);
        assertEquals(holder.expected, cache.hashCode(), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullKeyAndValueProvider")
    void hashCode_nullKeyAndValue(ArgumentsHolder holder) {
        cache.put(null, null);
        assertEquals(holder.expected, cache.hashCode(), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("orderSensitivityProvider")
    void hashCode_orderSensitivity(ArgumentsHolder holder) {
        // Determine insertion order from description text
        if (holder.description.contains("reverse order")) {
            // reverse order: (2,"b") then (1,"a")
            cache.put(2, "b");
            cache.put(1, "a");
        } else {
            // normal order: (1,"a") then (2,"b")
            cache.put(1, "a");
            cache.put(2, "b");
        }
        assertEquals(holder.expected, cache.hashCode(), holder.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("consistencyProvider")
    void hashCode_consistency(ArgumentsHolder holder) {
        cache.put(1, "a");
        int first = cache.hashCode();
        int second = cache.hashCode();
        assertEquals(first, second, holder.description);
        assertEquals(holder.expected, first, "Expected deterministic hashCode value");
    }
}
