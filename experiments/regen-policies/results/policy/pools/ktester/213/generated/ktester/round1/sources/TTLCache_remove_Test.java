package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

@ExtendWith(MockitoExtension.class)
class TTLCache_remove_Test {

    private TTLCache<Integer, String> ttlCache;

    @BeforeEach
    void setupBeforeEach() {
        // Use a very large TTL (10 seconds) and no size limit (-1) so entries stay alive.
        ttlCache = new TTLCache<>(10_000, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests to avoid cross‑test contamination.
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the background purge thread used by TTLCache.
        TTLCache.shutdown();
    }

    @Test
    void removeExistingKeyReturnsValueAndDeletesEntry() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        // Verify removal returns the associated value
        String removed = ttlCache.remove(1);
        assertEquals("A", removed, "remove should return the value associated with the key");
        // Verify the entry is no longer present
        assertNull(ttlCache.get(1), "Removed key should no longer be retrievable");
        assertFalse(ttlCache.containsKey(1), "Cache should not contain the removed key");
        // Verify other entries remain untouched
        assertEquals("B", ttlCache.get(2));
        assertEquals(1, ttlCache.size(), "Cache size should reflect the removal");
    }

    @Test
    void removeNonExistingKeyReturnsNullAndLeavesCacheUnchanged() {
        ttlCache.put(1, "A");
        // Attempt to remove a key that was never added
        String removed = ttlCache.remove(2);
        assertNull(removed, "Removing a non‑existent key should return null");
        // Cache state must be unchanged
        assertEquals(1, ttlCache.size());
        assertTrue(ttlCache.containsKey(1));
        assertEquals("A", ttlCache.get(1));
    }

    @Test
    void removeKeyWithNullValueReturnsNullButRemovesEntry() {
        ttlCache.put(1, null);
        // The stored value is null; remove should also return null
        String removed = ttlCache.remove(1);
        assertNull(removed, "remove should return null when the stored value is null");
        // The key must be gone after removal
        assertFalse(ttlCache.containsKey(1));
        assertEquals(0, ttlCache.size());
    }

    @Test
    void removeMultipleKeysSequentially() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        assertEquals("A", ttlCache.remove(1));
        assertEquals("C", ttlCache.remove(3));
        // non‑existent
        assertNull(ttlCache.remove(4));
        // Only key 2 should remain
        assertEquals(1, ttlCache.size());
        assertTrue(ttlCache.containsKey(2));
        assertEquals("B", ttlCache.get(2));
    }

    /**
     * The underlying {@link java.util.concurrent.ConcurrentMap} does not allow {@code null}
     * keys.  According to its contract, a {@link NullPointerException} must be thrown.
     */
    @Test
    void removeNullKeyThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ttlCache.remove(null), "Calling remove(null) should throw NullPointerException");
    }

    /**
     * Inject a corrupted {@code CacheEntry} whose {@code node} field is {@code null}.
     * When {@code remove()} accesses {@code entry.node.value} a {@link NullPointerException}
     * is expected.
     *
     * <p>We use reflection to reach the private {@code cacheMap} field and put a
     * mocked {@code CacheEntry} instance with a {@code null} node.
     */
    @Test
    @SuppressWarnings("unchecked")
    void removeEntryWithCorruptedCacheEntryThrowsNullPointerException() throws Exception {
        // 1️⃣ Retrieve the private cacheMap field.
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        ConcurrentMap<String, TTLCache.CacheEntry<String, String>> cacheMap = (ConcurrentMap<String, TTLCache.CacheEntry<String, String>>) mapField.get(ttlCache);
        // 2️⃣ Create a mock CacheEntry whose internal node is null.
        TTLCache.CacheEntry<String, String> corruptEntry = Mockito.mock(TTLCache.CacheEntry.class);
        // Use reflection to set the private 'node' field to null.
        Field nodeField = TTLCache.CacheEntry.class.getDeclaredField("node");
        nodeField.setAccessible(true);
        // explicitly nullify the node
        nodeField.set(corruptEntry, null);
        // 3️⃣ Insert the corrupted entry into the cacheMap directly.
        cacheMap.put("corruptKey", corruptEntry);
        // 4️⃣ Verify that remove() throws NullPointerException when it tries to read entry.node.value.
        assertThrows(NullPointerException.class, () -> ttlCache.remove("corruptKey"), "Removing a key whose CacheEntry has a null node should throw NullPointerException");
    }

    /**
     * Verify that a normal removal still works after we have manually injected a corrupted entry
     * for a different key.  This guards against side‑effects from the reflection manipulation.
     */
    @Test
    @SuppressWarnings("unchecked")
    void removeWorksWhenOtherEntriesAreCorrupted() throws Exception {
        // Normal entry
        ttlCache.put("goodKey", "GOOD");
        // Corrupt a different key
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        ConcurrentMap<String, TTLCache.CacheEntry<String, String>> cacheMap = (ConcurrentMap<String, TTLCache.CacheEntry<String, String>>) mapField.get(ttlCache);
        TTLCache.CacheEntry<String, String> corruptEntry = Mockito.mock(TTLCache.CacheEntry.class);
        Field nodeField = TTLCache.CacheEntry.class.getDeclaredField("node");
        nodeField.setAccessible(true);
        nodeField.set(corruptEntry, null);
        cacheMap.put("corruptKey", corruptEntry);
        // Removing the good key should succeed and be unaffected by the corrupt entry.
        String removed = ttlCache.remove("goodKey");
        assertEquals("GOOD", removed);
        assertFalse(ttlCache.containsKey("goodKey"));
        assertEquals(0, ttlCache.size());
    }
}
