package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_put_Test {

    private TTLCache<Integer, String> cache;

    /**
     * Subclass that forces {@code insertAtTail} to throw an {@link IllegalStateException}.
     * The method is made {@code protected} in this test subclass so that the override is
     * possible – the real class declares it {@code private}, but for the purpose of the
     * test we rely on the fact that the source shown is a simplification and that the
     * method can be overridden in the same package.
     */
    static class TTLCacheInsertAtTailException extends TTLCache<Integer, String> {

        TTLCacheInsertAtTailException(long ttl, int maxSize) {
            super(ttl, maxSize);
        }

        @Override
        protected void insertAtTail(Node<Integer, String> node) {
            throw new IllegalStateException("forced insertAtTail failure");
        }
    }

    /**
     * Subclass that forces {@code removeEntry} to throw an {@link IllegalStateException}
     * during eviction.
     */
    static class TTLCacheRemoveEntryException extends TTLCache<Integer, String> {

        TTLCacheRemoveEntryException(long ttl, int maxSize) {
            super(ttl, maxSize);
        }

        @Override
        protected void removeEntry(Integer cacheKey) {
            throw new IllegalStateException("forced removeEntry failure");
        }
    }

    /**
     * Create a fresh cache before each test. The default TTL is long enough
     * that expiration does not interfere with the pure‑put behaviour we are
     * testing here.
     */
    @BeforeEach
    void setupBeforeEach() {
        // 10 seconds TTL, no size limit (maxSize = -1)
        cache = new TTLCache<>(10_000L, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid cross‑test contamination.
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * TTLCache uses a static background thread for purging expired entries.
     * Shut it down once after all tests have run.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    private static Stream<Arguments> basicInsertionProvider() {
        return Stream.of(// case 1 – first insertion, empty cache
        Arguments.of("A", "alpha", null, 1, List.of("A")), // case 2 – second distinct key
        Arguments.of("B", "beta", null, 2, List.of("A", "B")), // case 3 – replace existing key, size unchanged
        Arguments.of("A", "alpha2", "alpha", 2, List.of("A", "B")));
    }

    @Test
    void putAddsNewEntryAndReturnsNull() {
        // given
        Integer key = 1;
        String value = "one";
        // when
        String returned = cache.put(key, value);
        // then
        assertNull(returned, "First insertion should return null");
        assertEquals(value, cache.get(key), "Cache should contain the inserted value");
        assertEquals(1, cache.size(), "Cache size should be 1 after one put");
    }

    @Test
    void putReplacesExistingEntryAndReturnsOldValue() {
        // given
        cache.put(1, "A");
        cache.put(2, "B");
        // when
        String old = cache.put(1, "A2");
        // then
        assertEquals("A", old, "put should return the previous value for the key");
        assertEquals("A2", cache.get(1), "New value should replace the old one");
        assertEquals(2, cache.size(), "Cache size should remain unchanged after replacement");
    }

    @Test
    void putUpdatesLruOrderWhenLockIsAcquired() {
        // Create a cache with a maxSize to trigger LRU logic.
        cache = new TTLCache<>(10_000L, 3);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // Access key 1 to make it most‑recently used.
        assertEquals("A", cache.get(1));
        // Adding a fourth entry should evict the least‑recently used (key 2).
        cache.put(4, "D");
        assertNull(cache.get(2), "Key 2 should have been evicted by LRU policy");
        assertEquals("A", cache.get(1), "Key 1 should still be present (recently used)");
        assertEquals("D", cache.get(4), "Newly added key 4 should be present");
        assertEquals(3, cache.size(), "Cache size should stay at the configured maxSize");
    }

    @Test
    void putDoesNotThrowWhenLockCannotBeAcquired() {
        // This test verifies that the method behaves correctly even if the
        // ReentrantLock tryLock() fails. We simulate contention by holding the
        // lock in another thread while performing put().
        cache = new TTLCache<>(10_000L, -1);
        // Acquire the lock manually.
        // lock is now held by this test thread
        cache.lock.tryLock();
        // Start a second thread that attempts to put while the lock is held.
        Thread t = new Thread(() -> {
            // The put call should succeed; it will simply skip the LRU update.
            assertDoesNotThrow(() -> cache.put(5, "E"));
        });
        t.start();
        // Give the thread a moment to execute.
        try {
            t.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Release the lock so that the cache can be cleaned up normally.
        cache.lock.unlock();
        // Verify that the entry was stored despite the lock contention.
        assertEquals("E", cache.get(5), "Entry should be present even when LRU update was skipped");
    }

    @ParameterizedTest(name = "{index}: put({0}, {1})")
    @MethodSource("basicInsertionProvider")
    void putBasicInsertionScenarios(String keyStr, String value, String expectedOld, int expectedSize, List<String> expectedLruOrder) throws Exception {
        // Populate the cache with the first two entries (A and B) when needed.
        if (!keyStr.equals("A")) {
            cache.put("A", "alpha");
        }
        if (!keyStr.equals("B") && !keyStr.equals("A")) {
            cache.put("B", "beta");
        }
        // Act
        String returned = cache.put(keyStr, value);
        // Assert return value
        if (expectedOld == null) {
            assertNull(returned, "Expected null when no previous value existed");
        } else {
            assertEquals(expectedOld, returned, "Returned old value mismatch");
        }
        // Assert size
        assertEquals(expectedSize, cache.size(), "Cache size after put should match expected");
        // Verify LRU order by walking the internal linked list via reflection.
        // The list is: head -> … -> tail (sentinel). We collect the keys.
        Field headField = TTLCache.class.getDeclaredField("head");
        headField.setAccessible(true);
        Object head = headField.get(cache);
        Field nextField = TTLCache.Node.class.getDeclaredField("next");
        nextField.setAccessible(true);
        Field keyField = TTLCache.Node.class.getDeclaredField("key");
        keyField.setAccessible(true);
        List<String> actualOrder = new ArrayList<>();
        // first real node
        Object node = nextField.get(head);
        while (node != null && node != cache.tail) {
            actualOrder.add(String.valueOf(keyField.get(node)));
            node = nextField.get(node);
        }
        assertEquals(expectedLruOrder, actualOrder, "LRU order after put does not match expectation");
    }

    @Test
    void putEvictsLruWhenMaxSizeReached() {
        // maxSize = 2, ttl long enough to avoid expiration
        TTLCache<String, String> smallCache = new TTLCache<>(10_000L, 2);
        smallCache.put("A", "alpha");
        // cache now full
        smallCache.put("B", "beta");
        // Insert third entry – should evict the LRU entry ("A")
        smallCache.put("C", "gamma");
        assertNull(smallCache.get("A"), "Key A should have been evicted");
        assertEquals("beta", smallCache.get("B"));
        assertEquals("gamma", smallCache.get("C"));
        assertEquals(2, smallCache.size(), "Cache size must stay at maxSize");
    }

    @Test
    void putAcceptsNullKeyWhenUnderlyingMapAllowsIt() throws Exception {
        // The default implementation uses a ConcurrentHashMapNullSafe that permits nulls.
        // Insert a null key.
        String returned = cache.put(null, "nullKey");
        assertNull(returned);
        assertEquals("nullKey", cache.get(null));
        assertEquals(1, cache.size());
    }

    @Test
    void putAcceptsNullValue() throws Exception {
        String returned = cache.put("E", null);
        assertNull(returned);
        assertNull(cache.get("E"));
        assertEquals(1, cache.size());
    }

    @Test
    void putEntryExpiresAfterTtl() throws Exception {
        // Very short TTL (10 ms)
        TTLCache<String, String> shortTtlCache = new TTLCache<>(10L, -1);
        shortTtlCache.put("F", "expireSoon");
        // Wait longer than the TTL
        Thread.sleep(30L);
        // The entry should be gone
        assertNull(shortTtlCache.get("F"), "Entry should have expired and be removed");
    }

    @Test
    void putThrowsNullPointerExceptionForNullKeyWhenMapDisallowsIt() {
        // Create a TTLCache that uses a plain ConcurrentHashMap (which forbids null keys)
        TTLCache<String, String> chmCache = new TTLCache<>(10_000L, -1);
        // Replace the internal map with a real ConcurrentHashMap via reflection
        try {
            Field mapField = TTLCache.class.getDeclaredField("cacheMap");
            mapField.setAccessible(true);
            mapField.set(chmCache, new ConcurrentHashMap<>());
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        assertThrows(NullPointerException.class, () -> chmCache.put(null, "someValue"));
    }

    @Test
    void putThrowsNullPointerExceptionForNullValueWhenMapDisallowsIt() {
        TTLCache<String, String> chmCache = new TTLCache<>(10_000L, -1);
        try {
            Field mapField = TTLCache.class.getDeclaredField("cacheMap");
            mapField.setAccessible(true);
            mapField.set(chmCache, new ConcurrentHashMap<>());
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        assertThrows(NullPointerException.class, () -> chmCache.put("someKey", null));
    }

    @Test
    void putPropagatesUnsupportedOperationExceptionFromUnderlyingMap() throws Exception {
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, TTLCache.CacheEntry<String, String>> badMap = mock(ConcurrentMap.class);
        when(badMap.put(anyString(), any())).thenThrow(new UnsupportedOperationException("map put not supported"));
        // Inject the mock map
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(cache, badMap);
        assertThrows(UnsupportedOperationException.class, () -> cache.put("K1", "V1"), "Exception from underlying map should propagate");
    }

    @Test
    void putThrowsIllegalStateExceptionWhenInsertAtTailFails() {
        TTLCacheInsertAtTailException badCache = new TTLCacheInsertAtTailException(10_000L, -1);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> badCache.put(1, "value"));
        assertEquals("forced insertAtTail failure", ex.getMessage());
        // Ensure the lock was released (size should be 1 because map insertion succeeded)
        assertEquals(1, badCache.size(), "Map entry should still be present after exception");
    }

    @Test
    void putThrowsIllegalStateExceptionDuringEviction() {
        // maxSize = 1 forces eviction on the second insertion
        TTLCacheRemoveEntryException badCache = new TTLCacheRemoveEntryException(10_000L, 1);
        badCache.put(1, "first");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> badCache.put(2, "second"));
        assertEquals("forced removeEntry failure", ex.getMessage());
        // The first entry should still be present because eviction failed
        assertEquals("first", badCache.get(1));
        // The second entry is present in the map (put succeeded) but LRU list is inconsistent;
        // size reflects two entries because eviction did not happen.
        assertEquals(2, badCache.size());
    }
}
