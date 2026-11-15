package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_hashCode_Test {

    private TTLCache<Integer, String> cache;

    /**
     * Helper key whose hashCode throws a RuntimeException.
     */
    static class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("BadKey hashCode");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    /**
     * Helper value whose hashCode throws a RuntimeException.
     */
    static class BadValue {

        @Override
        public int hashCode() {
            throw new RuntimeException("BadValue hashCode");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a very large TTL so entries never expire during the test
        cache = new TTLCache<>(Long.MAX_VALUE, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests to avoid cross‑test interference
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache
        TTLCache.shutdown();
    }

    /* -------------------------------------------------------------
     *  Helper to compute the expected hashCode using the same algorithm
     *  as TTLCache.hashCode()
     * ------------------------------------------------------------- */
    private static int expectedHashCode(Map<?, ?> map) {
        int hash = 1;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            hash = 31 * hash + (k == null ? 0 : k.hashCode());
            hash = 31 * hash + (v == null ? 0 : v.hashCode());
        }
        return hash;
    }

    /**
     * Supplies the normal test cases defined in the JSON payload.
     */
    private static Stream<Arguments> hashCodeNormalCases() {
        // 1 – empty cache (already covered by a dedicated test, but kept for completeness)
        Map<Object, Object> empty = Collections.emptyMap();
        // 2 – single entry ("a","b")
        Map<Object, Object> singleAB = new LinkedHashMap<>();
        singleAB.put("a", "b");
        // 3 – two entries in forward order
        Map<Object, Object> twoForward = new LinkedHashMap<>();
        twoForward.put("a", "b");
        twoForward.put("c", "d");
        // 4 – null key, non‑null value
        Map<Object, Object> nullKey = new LinkedHashMap<>();
        nullKey.put(null, "val");
        // 5 – non‑null key, null value
        Map<Object, Object> nullValue = new LinkedHashMap<>();
        nullValue.put("k", null);
        // 6 – both key and value null
        Map<Object, Object> bothNull = new LinkedHashMap<>();
        bothNull.put(null, null);
        // 7 – two entries inserted in reverse order
        Map<Object, Object> twoReverse = new LinkedHashMap<>();
        twoReverse.put("c", "d");
        twoReverse.put("a", "b");
        return Stream.of(Arguments.of("Empty TTLCache – no entries", empty, 1), Arguments.of("Single entry (\"a\",\"b\")", singleAB, ((1 * 31 + "a".hashCode()) * 31 + "b".hashCode())), Arguments.of("Two entries forward order (\"a\",\"b\"), (\"c\",\"d\")", twoForward, ((1 * 31 + "a".hashCode()) * 31 + "b".hashCode()) * 31 * 31 + "c".hashCode() * 31 + // computed by helper
        "d".hashCode()), Arguments.of("Null key, non‑null value", nullKey, ((1 * 31 + 0) * 31 + "val".hashCode())), Arguments.of("Non‑null key, null value", nullValue, ((1 * 31 + "k".hashCode()) * 31 + 0)), Arguments.of("Both key and value null", bothNull, ((1 * 31 + 0) * 31 + 0)), Arguments.of("Two entries reverse order (\"c\",\"d\"), (\"a\",\"b\")", twoReverse, ((1 * 31 + "c".hashCode()) * 31 + "d".hashCode()) * 31 * 31 + "a".hashCode() * 31 + "b".hashCode()));
    }

    @Test
    void hashCodeEmptyCache() {
        // An empty cache should return the initial value (1)
        assertEquals(1, cache.hashCode(), "hashCode of an empty TTLCache must be 1");
    }

    @Test
    void hashCodeWithMultipleEntries() {
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // Build a reference map that preserves insertion order.
        // TTLCache preserves insertion order for LRU‑disabled caches.
        Map<Integer, String> reference = new LinkedHashMap<>();
        reference.put(1, "A");
        reference.put(2, "B");
        reference.put(3, "C");
        int expected = expectedHashCode(reference);
        assertEquals(expected, cache.hashCode(), "hashCode should reflect all key/value pairs in the cache");
    }

    @Test
    void hashCodeWithNullKeyAndNullValue() {
        // TTLCache supports null keys/values via its internal sentinel handling.
        cache.put(null, null);
        cache.put(1, null);
        cache.put(null, "X");
        cache.put(2, "Y");
        Map<Object, Object> reference = new HashMap<>();
        reference.put(null, null);
        reference.put(1, null);
        // the second null key overwrites the first entry
        reference.put(null, "X");
        reference.put(2, "Y");
        // The map now contains two entries: (null, "X") and (2, "Y")
        int expected = expectedHashCode(reference);
        assertEquals(expected, cache.hashCode(), "hashCode must correctly handle null keys and null values");
    }

    @Test
    void hashCodeConsistentAcrossCalls() {
        cache.put(10, "ten");
        cache.put(20, "twenty");
        int first = cache.hashCode();
        int second = cache.hashCode();
        assertEquals(first, second, "hashCode should be stable when the cache is not mutated");
    }

    @Test
    void hashCodeReflectsMutations() {
        cache.put(5, "five");
        int before = cache.hashCode();
        // mutate cache
        cache.put(6, "six");
        int after = cache.hashCode();
        assertNotEquals(before, after, "hashCode must change after the cache content changes");
    }

    /* -------------------------------------------------------------
     *  Normal behaviour tests – parameterised over the JSON cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("hashCodeNormalCases")
    void hashCodeNormalScenarios(String description, Map<Object, Object> entries, int expected) {
        // Populate the cache exactly as described in the test case.
        // The cache's put method preserves insertion order for LRU‑disabled caches.
        entries.forEach((k, v) -> cache.put((Integer) k, (String) v));
        // Verify that the algorithm used by TTLCache produces the expected value.
        assertEquals(expected, cache.hashCode(), "hashCode mismatch – " + description);
    }

    /* -------------------------------------------------------------
     *  Exception / corrupted‑state tests
     * ------------------------------------------------------------- */
    @Test
    void hashCode_throwsWhenHeadIsNull() throws Exception {
        // Corrupt the internal state: set the private 'head' field to null.
        Field headField = TTLCache.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(cache, null);
        assertThrows(NullPointerException.class, cache::hashCode, "When head is null the implementation should throw NPE");
    }

    @Test
    void hashCode_throwsWhenNodeNextBecomesNull() throws Exception {
        // Normal population first.
        cache.put(1, "one");
        cache.put(2, "two");
        // Reach the first real node (head.next) and break the chain.
        Field headField = TTLCache.class.getDeclaredField("head");
        headField.setAccessible(true);
        // Node instance
        Object head = headField.get(cache);
        Field nextField = head.getClass().getDeclaredField("next");
        nextField.setAccessible(true);
        // the node that holds key 1
        Object firstNode = nextField.get(head);
        // Break the chain by setting its 'next' to null.
        nextField.set(firstNode, null);
        assertThrows(NullPointerException.class, cache::hashCode, "A null 'next' before reaching tail should cause NPE");
    }

    @Test
    void hashCode_propagatesExceptionFromKeyHashCode() {
        BadKey badKey = new BadKey();
        // insertion itself does not call key.hashCode()
        cache.put(badKey, "good");
        RuntimeException ex = assertThrows(RuntimeException.class, cache::hashCode, "Exception from key.hashCode() must propagate");
        assertEquals("BadKey hashCode", ex.getMessage());
    }

    @Test
    void hashCode_propagatesExceptionFromValueHashCode() {
        BadValue badValue = new BadValue();
        cache.put("good", badValue);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::hashCode, "Exception from value.hashCode() must propagate");
        assertEquals("BadValue hashCode", ex.getMessage());
    }
}
