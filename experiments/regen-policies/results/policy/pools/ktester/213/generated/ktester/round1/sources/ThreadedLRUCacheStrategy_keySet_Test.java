package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.Collections;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_keySet_Test {

    private ThreadedLRUCacheStrategy<Integer, String> cache;

    /**
     * Creates a fresh cache instance before each test. The capacity and cleanup delay
     * values are chosen to be small so the background purge task does not interfere
     * with the deterministic nature of the tests.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The constructor (capacity, cleanupDelayMs) is part of the public API.
        cache = new ThreadedLRUCacheStrategy<>(10, 50);
    }

    /**
     * Clears the cache after each test to avoid any leftover state that could affect
     * subsequent tests.
     */
    @AfterEach
    void teardownAfterEach() {
        cache.clear();
    }

    @Test
    @DisplayName("keySet returns all keys present in the cache")
    void testKeySetContainsAllKeys() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // when
        Set<Integer> keys = cache.keySet();
        // then
        assertEquals(3, keys.size(), "keySet size should match number of inserted entries");
        assertTrue(keys.containsAll(Set.of(1, 2, 3)), "keySet should contain all inserted keys");
        // verify that the returned set is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> keys.add(4), "keySet should be unmodifiable");
    }

    @Test
    @DisplayName("keySet on an empty cache returns an empty, unmodifiable set")
    void testKeySetOnEmptyCache() {
        // given – cache is freshly created and empty
        // when
        Set<Integer> keys = cache.keySet();
        // then
        assertTrue(keys.isEmpty(), "keySet of an empty cache should be empty");
        assertSame(Collections.emptySet(), Collections.unmodifiableSet(keys), "Returned set should be unmodifiable (wrapped by Collections.unmodifiableSet)");
        assertThrows(UnsupportedOperationException.class, () -> keys.add(1), "keySet should be unmodifiable even when empty");
    }

    @Test
    @DisplayName("keySet reflects removals from the cache")
    void testKeySetReflectsRemovals() {
        // given
        cache.put(10, "ten");
        cache.put(20, "twenty");
        cache.put(30, "thirty");
        // when
        cache.remove(20);
        Set<Integer> keys = cache.keySet();
        // then
        assertEquals(2, keys.size(), "After removal, keySet size should decrease");
        assertTrue(keys.containsAll(Set.of(10, 30)), "keySet should contain remaining keys");
        assertFalse(keys.contains(20), "Removed key should not appear in keySet");
    }

    @Test
    @DisplayName("keySet on a non‑empty cache returns exactly the inserted keys")
    void testKeySetNonEmptyCacheExactKeys() {
        // given – three distinct entries
        cache.put(100, "alpha");
        cache.put(200, "beta");
        cache.put(300, "gamma");
        // when
        Set<Integer> keys = cache.keySet();
        // then
        assertEquals(3, keys.size(), "keySet size must equal number of inserted entries");
        assertTrue(keys.containsAll(Set.of(100, 200, 300)), "keySet must contain exactly the keys that were put into the cache");
        // ensure unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> keys.remove(100), "keySet should be unmodifiable (remove operation)");
    }

    @Test
    @DisplayName("keySet snapshot remains unchanged after the cache is cleared")
    void testKeySetSnapshotIndependenceAfterClear() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        // capture snapshot
        Set<Integer> snapshot = cache.keySet();
        // when
        // mutate the underlying cache
        cache.clear();
        Set<Integer> afterClear = cache.keySet();
        // then
        assertEquals(2, snapshot.size(), "Snapshot should retain original size");
        assertTrue(snapshot.containsAll(Set.of(1, 2)), "Snapshot must still contain original keys");
        assertTrue(afterClear.isEmpty(), "After clearing the cache, a fresh keySet should be empty");
    }

    @Test
    @DisplayName("keySet includes a null key when the cache stores one")
    void testKeySetNullKeySupport() {
        // given
        cache.put(null, "nullValue");
        cache.put(42, "answer");
        // when
        Set<Integer> keys = cache.keySet();
        // then
        assertEquals(2, keys.size(), "keySet size should include the null key");
        assertTrue(keys.contains(null), "keySet must contain null when a null key is stored");
        assertTrue(keys.contains(42), "keySet must also contain the non‑null key");
    }

    @Test
    @DisplayName("keySet snapshot does not include keys added after the call")
    void testKeySetSnapshotDoesNotIncludeLaterAdditions() {
        // given
        cache.put(1, "first");
        // snapshot contains only "first"
        Set<Integer> snapshot = cache.keySet();
        // when
        // add after snapshot
        cache.put(2, "second");
        Set<Integer> later = cache.keySet();
        // then
        assertEquals(1, snapshot.size(), "Snapshot should contain only the original key");
        assertTrue(snapshot.contains(1), "Snapshot must contain the original key");
        assertFalse(snapshot.contains(2), "Snapshot must not contain keys added later");
        assertEquals(2, later.size(), "A fresh keySet after the addition should contain both keys");
        assertTrue(later.containsAll(Set.of(1, 2)));
    }

    @Test
    @DisplayName("keySet throws NullPointerException when internal cache field is null")
    void testKeySetNullInternalCache() throws Exception {
        // given – force the private 'cache' field to null via reflection
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        // when / then
        assertThrows(NullPointerException.class, () -> cache.keySet(), "Calling keySet() with a null internal cache should throw NullPointerException");
    }

    @Test
    @DisplayName("keySet is unmodifiable – both add and remove operations throw")
    void testKeySetUnmodifiableAddAndRemove() {
        // given
        cache.put(5, "five");
        cache.put(6, "six");
        Set<Integer> keys = cache.keySet();
        // then – both mutating operations must fail
        assertThrows(UnsupportedOperationException.class, () -> keys.add(7), "Adding to the keySet must throw UnsupportedOperationException");
        assertThrows(UnsupportedOperationException.class, () -> keys.remove(5), "Removing from the keySet must throw UnsupportedOperationException");
    }
}
