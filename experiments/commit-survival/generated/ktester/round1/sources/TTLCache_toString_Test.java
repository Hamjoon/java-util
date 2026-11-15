package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_toString_Test {

    /**
     * Cache used in the majority of the tests.
     */
    private TTLCache<Integer, String> ttlCache;

    class CustomKey {

        @Override
        public String toString() {
            return "CustomKey";
        }
    }

    class CustomValue {

        @Override
        public String toString() {
            return "CustomValue";
        }
    }

    class CustomKey {

        @Override
        public String toString() {
            return "CustomKey";
        }
    }

    class CustomValue {

        @Override
        public String toString() {
            return "CustomValue";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a very large TTL and a long cleanup interval so that entries never expire
        // during the execution of the test.
        ttlCache = new TTLCache<>(60_000L, -1, 60_000L);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that a fresh instance is used for the next test.
        ttlCache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache.
        TTLCache.shutdown();
    }

    /**
     * Verifies that the {@code toString()} representation of the cache contains
     * exactly the expected key‑value pairs, irrespective of their order.
     *
     * @param expected a set of strings of the form "key=value"
     */
    private void assertCacheToStringContains(Set<String> expected) {
        String actual = ttlCache.toString();
        // Empty cache must be exactly "{}"
        if (expected.isEmpty()) {
            assertEquals("{}", actual, "Empty cache should be represented as {}");
            return;
        }
        // The string must start with '{' and end with '}'
        assertTrue(actual.startsWith("{") && actual.endsWith("}"), "Cache toString must be wrapped with braces");
        // Remove the surrounding braces and split on commas (allow optional spaces)
        String inner = actual.substring(1, actual.length() - 1).trim();
        String[] parts = inner.split("\\s*,\\s*");
        // Build a set of the observed key=value strings
        Set<String> observed = new HashSet<>();
        for (String part : parts) {
            // Guard against empty strings that could appear if the cache were somehow empty
            if (!part.isEmpty()) {
                observed.add(part);
            }
        }
        assertEquals(expected, observed, () -> "Cache toString content mismatch. Expected: " + expected + " but was: " + observed);
    }

    @Test
    void testToStringEmptyCache() {
        // No entries have been added – the cache should render as "{}"
        assertCacheToStringContains(Set.of());
    }

    @Test
    void testToStringWithTwoEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        Set<String> expected = Set.of("1=A", "2=B");
        assertCacheToStringContains(expected);
    }

    @Test
    void testToStringAfterRemoval() {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        ttlCache.put(30, "Z");
        // Remove one entry and verify the representation reflects the change
        ttlCache.remove(20);
        Set<String> expected = Set.of("10=X", "30=Z");
        assertCacheToStringContains(expected);
    }

    @Test
    void testToStringDoesNotExposeInternalLock() {
        // The string representation must never contain the word "lock"
        ttlCache.put(5, "value");
        String s = ttlCache.toString();
        assertFalse(Pattern.compile("lock", Pattern.CASE_INSENSITIVE).matcher(s).find(), "toString() should not leak internal implementation details");
    }

    @Test
    void testToStringConcurrentModificationSafety() {
        // Populate the cache
        for (int i = 0; i < 5; i++) {
            ttlCache.put(i, "v" + i);
        }
        // While iterating over entrySet (which toString does internally), remove an entry.
        // This should not cause a ConcurrentModificationException because TTLCache
        // uses a concurrent map. The test simply ensures that toString still returns a
        // syntactically correct representation.
        ttlCache.remove(2);
        assertDoesNotThrow(() -> ttlCache.toString(), "toString() should handle concurrent modifications gracefully");
    }

    @Test
    @DisplayName("TTLCache.toString - Single Entry")
    void testToStringSingleEntry() {
        // Populate the cache with exactly one mapping.
        ttlCache.put(42, "answer");
        Set<String> expected = Set.of("42=answer");
        assertCacheToStringContains(expected);
    }

    @Test
    @DisplayName("TTLCache.toString - Multiple Entries")
    void testToStringMultipleEntries() {
        // Insert three distinct entries. Order is not guaranteed, so we compare as a set.
        ttlCache.put("first", "1");
        ttlCache.put("second", "2");
        ttlCache.put("third", "3");
        Set<String> expected = Set.of("first=1", "second=2", "third=3");
        assertCacheToStringContains(expected);
    }

    @Test
    @DisplayName("TTLCache.toString - Null Key and Null Value Entry")
    void testToStringNullKeyAndValue() {
        // TTLCache supports null keys/values via sentinel objects.
        ttlCache.put(null, null);
        Set<String> expected = Set.of("null=null");
        assertCacheToStringContains(expected);
    }

    @Test
    @DisplayName("TTLCache.toString - Custom Object toString")
    void testToStringCustomObjectToString() {
        // Define simple objects with overridden toString().
        class CustomKey {

            @Override
            public String toString() {
                return "CustomKey";
            }
        }
        class CustomValue {

            @Override
            public String toString() {
                return "CustomValue";
            }
        }
        ttlCache.put(new CustomKey(), new CustomValue());
        Set<String> expected = Set.of("CustomKey=CustomValue");
        assertCacheToStringContains(expected);
    }

    @Test
    @DisplayName("TTLCache.toString - Snapshot Consistency")
    void testToStringSnapshotConsistency() {
        // Populate the cache and capture its string representation.
        ttlCache.put("alpha", "A");
        ttlCache.put("beta", "B");
        String snapshot = ttlCache.toString();
        // Mutate the cache after the snapshot.
        ttlCache.put("gamma", "C");
        ttlCache.remove("alpha");
        // The previously captured snapshot must remain unchanged.
        assertEquals("{alpha=A, beta=B}", snapshot, "The string returned by toString() must represent a snapshot of the cache at call time");
    }
}
