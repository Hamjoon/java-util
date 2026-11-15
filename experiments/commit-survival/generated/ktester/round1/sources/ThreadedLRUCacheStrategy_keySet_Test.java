package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_keySet_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Create a fresh cache before each test. The cleanup delay is set high enough
     * that the background purge task will not run during the short‑lived tests.
     */
    @BeforeEach
    void setupBeforeEach() {
        // capacity 10 is enough for most tests; for large‑scale tests we will create a new cache with a larger capacity.
        cache = new ThreadedLRUCacheStrategy<>(10, 1_000);
    }

    /**
     * Clear the cache after each test so that no state leaks between tests.
     */
    @AfterEach
    void teardownAfterEach() {
        cache.clear();
    }

    /**
     * The shared scheduler used by all {@link ThreadedLRUCacheStrategy} instances must be
     * shut down after the whole test suite to avoid thread‑leak warnings.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void keySetReturnsAllInsertedKeys() {
        // given
        cache.put("alpha", 1);
        cache.put("beta", 2);
        cache.put("gamma", 3);
        // when
        Set<String> keys = cache.keySet();
        // then
        assertEquals(3, keys.size(), "keySet should contain exactly three keys");
        assertTrue(keys.containsAll(Arrays.asList("alpha", "beta", "gamma")), "keySet should contain all inserted keys");
        assertTrue(keys instanceof java.util.Set, "keySet should be a Set implementation");
    }

    @Test
    void keySetIsUnmodifiable() {
        // given
        cache.put("one", 1);
        cache.put("two", 2);
        Set<String> keys = cache.keySet();
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> keys.add("three"), "keySet must be unmodifiable");
        assertThrows(UnsupportedOperationException.class, () -> keys.remove("one"), "keySet must be unmodifiable");
    }

    @Test
    void keySetReflectsRemovals() {
        // given
        cache.put("keep", 100);
        cache.put("discard", 200);
        assertTrue(cache.keySet().contains("discard"));
        // when
        cache.remove("discard");
        Set<String> keysAfterRemoval = cache.keySet();
        // then
        assertFalse(keysAfterRemoval.contains("discard"), "keySet should no longer contain a key that was removed");
        assertTrue(keysAfterRemoval.contains("keep"), "keySet should still contain keys that remain in the cache");
    }

    @Test
    void keySetIsEmptyWhenCacheIsEmpty() {
        // given: a freshly created cache with no entries
        // when
        Set<String> keys = cache.keySet();
        // then
        assertTrue(keys.isEmpty(), "keySet should be empty for a newly created cache");
        assertEquals(Collections.emptySet(), keys, "keySet should be equal to Collections.emptySet() when empty");
    }

    /* ---------- keySetEmptyCache ---------- */
    @Test
    void keySetEmptyCache_returnsEmptyUnmodifiableSet() {
        Set<String> ks = cache.keySet();
        assertTrue(ks.isEmpty(), "Returned set must be empty");
        assertThrows(UnsupportedOperationException.class, () -> ks.add("any"), "Returned set must be unmodifiable");
    }

    /* ---------- keySetMultipleEntries ---------- */
    @Test
    void keySetMultipleEntries_containsAllKeys() {
        cache.put("alpha", 1);
        cache.put("beta", 2);
        cache.put("gamma", 3);
        Set<String> ks = cache.keySet();
        assertEquals(Set.of("alpha", "beta", "gamma"), ks, "keySet must contain every distinct key that was put into the map");
    }

    /* ---------- keySetUnmodifiable ---------- */
    @Test
    void keySetUnmodifiable_addOperationThrows() {
        cache.put("one", 1);
        Set<String> ks = cache.keySet();
        assertThrows(UnsupportedOperationException.class, () -> ks.add("two"));
    }

    /* ---------- keySetSnapshotIsolation ---------- */
    @Test
    void keySetSnapshotIsolation_isNotAffectedByLaterPuts() {
        // initial population
        cache.put("first", 1);
        Set<String> snapshot = cache.keySet();
        // modify cache after snapshot
        cache.put("second", 2);
        // snapshot must stay unchanged
        assertEquals(Set.of("first"), snapshot, "Original snapshot should not contain keys added after the call");
        // a fresh call reflects the new state
        assertEquals(Set.of("first", "second"), cache.keySet(), "Subsequent keySet call must see the newly added key");
    }

    /* ---------- keySetHandlesNullKey ---------- */
    @Test
    void keySetHandlesNullKey_includesNull() {
        cache.put(null, 0);
        cache.put("notNull", 1);
        Set<String> ks = cache.keySet();
        assertTrue(ks.contains(null), "keySet must include null key");
        assertTrue(ks.contains("notNull"));
        assertEquals(2, ks.size());
    }

    /* ---------- keySetAfterRemoval ---------- */
    @Test
    void keySetAfterRemoval_reflectsRemoval() {
        cache.put("alpha", 1);
        cache.put("beta", 2);
        cache.remove("alpha");
        Set<String> ks = cache.keySet();
        assertEquals(Set.of("beta"), ks);
    }

    /* ---------- keySetMultipleCallsDistinctInstances ---------- */
    @Test
    void keySetMultipleCallsDistinctInstances_areEqualButNotSame() {
        cache.put("one", 1);
        cache.put("two", 2);
        Set<String> first = cache.keySet();
        Set<String> second = cache.keySet();
        assertEquals(first, second, "Both sets must have identical contents");
        assertNotSame(first, second, "Each call should return a new Set instance");
    }

    /* ---------- keySetLargeScale ---------- */
    @Test
    void keySetLargeScale_handlesThousandEntries() {
        // Use a larger capacity to avoid eviction during the test
        cache = new ThreadedLRUCacheStrategy<>(2000, 1_000);
        int count = 1000;
        for (int i = 0; i < count; i++) {
            cache.put("k" + i, i);
        }
        Set<String> ks = cache.keySet();
        assertEquals(count, ks.size(), "keySet size must match number of inserted entries");
        // Spot‑check a few keys
        assertTrue(ks.contains("k0"));
        assertTrue(ks.contains("k500"));
        assertTrue(ks.contains("k999"));
    }

    /* ---------- keySetUnsupportedRemoveClear ---------- */
    @Test
    void keySetUnsupportedRemove_throws() {
        cache.put("x", 1);
        cache.put("y", 2);
        Set<String> ks = cache.keySet();
        assertThrows(UnsupportedOperationException.class, () -> ks.remove("x"));
    }

    @Test
    void keySetUnsupportedClear_throws() {
        cache.put("a", 1);
        Set<String> ks = cache.keySet();
        assertThrows(UnsupportedOperationException.class, ks::clear);
    }

    /* ---------- keySetDuplicateKeyOverwrite ---------- */
    @Test
    void keySetDuplicateKeyOverwrite_containsSingleKey() {
        cache.put("dup", 1);
        // overwrite
        cache.put("dup", 2);
        Set<String> ks = cache.keySet();
        assertEquals(Set.of("dup"), ks, "Key should appear only once even after overwrite");
    }
}
