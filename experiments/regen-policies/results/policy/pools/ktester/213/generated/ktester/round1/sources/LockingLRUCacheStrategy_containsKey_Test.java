package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_containsKey_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<String, Integer> cache;

    /**
     * Simple wrapper used to test that {@code containsKey} respects custom
     * {@code equals} and {@code hashCode} implementations.
     */
    private static final class KeyWrapper {

        private final String id;

        KeyWrapper(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof KeyWrapper))
                return false;
            KeyWrapper that = (KeyWrapper) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test to guarantee isolation.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
        // Populate the cache with a few entries.
        cache.put("one", 1);
        cache.put("two", 2);
        // verify that null keys are allowed
        cache.put(null, 0);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to help the garbage collector and keep the test clean.
        cache.clear();
    }

    @Test
    void containsKey_existingKey_returnsTrue() {
        // given
        String key = "one";
        // when
        boolean result = cache.containsKey(key);
        // then
        assertTrue(result, "containsKey should return true for a key that was put into the cache");
    }

    @Test
    void containsKey_missingKey_returnsFalse() {
        // given
        String missingKey = "three";
        // when
        boolean result = cache.containsKey(missingKey);
        // then
        assertFalse(result, "containsKey should return false for a key that is not present in the cache");
    }

    @Test
    void containsKey_nullKey_returnsTrueWhenNullKeyPresent() {
        // given
        // null key was added in setupBeforeEach
        // when
        boolean result = cache.containsKey(null);
        // then
        assertTrue(result, "containsKey should return true for a null key when it has been stored");
    }

    @Test
    void containsKey_nullKey_returnsFalseWhenNullKeyAbsent() {
        // given
        // ensure null key is not present
        cache.remove(null);
        // when
        boolean result = cache.containsKey(null);
        // then
        assertFalse(result, "containsKey should return false for a null key when it is not stored");
    }

    // ---------- Simple presence / absence cases ----------
    @Test
    void containsKey_alphaKey_present_returnsTrue() {
        // "alpha" is added explicitly for this test
        cache.put("alpha", 10);
        assertTrue(cache.containsKey("alpha"), "Key \"alpha\" has been put into the cache prior to the call; containsKey should return true.");
    }

    @Test
    void containsKey_betaKey_absent_returnsFalse() {
        assertFalse(cache.containsKey("beta"), "Key \"beta\" has never been added to the cache; containsKey should return false.");
    }

    @Test
    void containsKey_integerKey_typeMismatch_returnsFalse() {
        // The cache stores String keys; an Integer key must not be found.
        assertFalse(cache.containsKey(123), "Cache contains only String keys; passing an Integer key should result in false.");
    }

    @Test
    void containsKey_nullKey_notStored_returnsFalse() {
        // Create a fresh cache without adding a null key.
        LockingLRUCacheStrategy<String, Integer> fresh = new LockingLRUCacheStrategy<>(CAPACITY);
        assertFalse(fresh.containsKey(null), "Null keys are not stored in the cache; containsKey(null) should return false rather than throwing.");
    }

    @Test
    void containsKey_emptyCache_anyKey_returnsFalse() {
        // Empty cache – no puts performed.
        LockingLRUCacheStrategy<String, Integer> empty = new LockingLRUCacheStrategy<>(CAPACITY);
        assertFalse(empty.containsKey("gamma"), "Cache is empty (no put operations performed); any key should return false.");
    }

    @Test
    void containsKey_keyAddedThenRemoved_returnsFalse() {
        cache.put("delta", 4);
        cache.remove("delta");
        assertFalse(cache.containsKey("delta"), "Key \"delta\" was added then removed; after removal containsKey should return false.");
    }

    // ---------- afterClear ----------
    @Test
    void containsKey_afterClear_returnsFalse() {
        cache.put("epsilon", 5);
        cache.clear();
        assertFalse(cache.containsKey("epsilon"), "Key \"epsilon\" is put into the cache, then the cache is cleared; containsKey should return false because the cache is empty.");
    }

    // ---------- afterPutAll ----------
    @Test
    void containsKey_afterPutAll_returnsTrueForInsertedKey() {
        Map<String, Integer> toAdd = new HashMap<>();
        toAdd.put("zeta", 6);
        toAdd.put("eta", 7);
        cache.putAll(toAdd);
        assertTrue(cache.containsKey("zeta"), "A map containing keys \"zeta\" and \"eta\" is added via putAll; containsKey(\"zeta\") must return true.");
    }

    // ---------- duplicatePut ----------
    @Test
    void containsKey_duplicatePut_keyRemainsPresent() {
        cache.put("theta", 1);
        // overwrite with a different value
        cache.put("theta", 2);
        assertTrue(cache.containsKey("theta"), "Key \"theta\" is put twice with different values; the key remains present, so containsKey should return true.");
    }

    // ---------- LRU eviction ----------
    @Test
    void containsKey_lruEviction_correctEntriesPresent() {
        // Capacity of 2 to trigger eviction.
        LockingLRUCacheStrategy<String, Integer> lruCache = new LockingLRUCacheStrategy<>(2);
        lruCache.put("a", 1);
        lruCache.put("b", 2);
        // Access "b" to make it most‑recently used (optional, but putAll already updates order)
        lruCache.get("b");
        // should evict "a"
        lruCache.put("c", 3);
        assertFalse(lruCache.containsKey("a"), "After inserting a, b, then c with capacity 2, the least‑recently‑used key \"a\" must be evicted.");
        assertTrue(lruCache.containsKey("b"), "Key \"b\" should still be present after eviction.");
        assertTrue(lruCache.containsKey("c"), "Key \"c\" should be present after insertion.");
    }

    // ---------- keyEqualityDifferentInstance ----------
    @Test
    void containsKey_keyEqualityDifferentInstance_returnsTrue() {
        // distinct instance
        String stored = new String("iota");
        cache.put(stored, 9);
        // another distinct instance but equal content
        String query = new String("iota");
        assertTrue(cache.containsKey(query), "A key created with new String(\"iota\") is put into the cache; a different String instance with the same characters should return true.");
    }

    // ---------- customKeyObjectEquality ----------
    @Test
    void containsKey_customKeyObjectEquality_returnsTrue() {
        // Use a cache with a generic Object key type to store custom key objects.
        LockingLRUCacheStrategy<Object, Integer> customCache = new LockingLRUCacheStrategy<>(CAPACITY);
        KeyWrapper key1 = new KeyWrapper("kappa");
        // equal according to overridden equals()
        KeyWrapper key2 = new KeyWrapper("kappa");
        customCache.put(key1, 42);
        assertTrue(customCache.containsKey(key2), "A custom key object that overrides equals()/hashCode() based on an internal string is stored; a different instance with the same internal value should be found.");
    }
}
