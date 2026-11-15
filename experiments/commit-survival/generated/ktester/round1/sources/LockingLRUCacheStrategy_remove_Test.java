package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class LockingLRUCacheStrategy_remove_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<String, Integer> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test to guarantee isolation.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
        // Populate with a few entries that will be used by the tests.
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        // Also add a null key/value pair to verify null handling.
        cache.put(null, 0);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to ensure no side‑effects leak between tests.
        cache.clear();
    }

    @Test
    void removeExistingKey_returnsValueAndRemovesEntry() {
        // given
        assertEquals(3, cache.size(), "Pre‑condition: cache should contain three non‑null keys and one null key");
        // when
        Integer removed = cache.remove("two");
        // then
        assertEquals(2, removed, "remove should return the value associated with the key");
        assertFalse(cache.containsKey("two"), "Key should no longer be present after removal");
        assertEquals(3, cache.size(), "Cache size should decrease by one");
        // Ensure other entries are untouched
        assertEquals(1, cache.get("one"));
        assertEquals(3, cache.get("three"));
        assertEquals(0, cache.get(null));
    }

    @Test
    void removeNonExistingKey_returnsNullAndLeavesCacheUnchanged() {
        // given
        int originalSize = cache.size();
        assertFalse(cache.containsKey("missing"));
        // when
        Integer removed = cache.remove("missing");
        // then
        assertNull(removed, "remove should return null when the key is absent");
        assertEquals(originalSize, cache.size(), "Cache size must remain unchanged");
        // Verify existing entries are still present
        assertEquals(1, cache.get("one"));
        assertEquals(2, cache.get("two"));
        assertEquals(3, cache.get("three"));
        assertEquals(0, cache.get(null));
    }

    @Test
    void removeNullKey_returnsValueAndRemovesEntry() {
        // given
        assertTrue(cache.containsKey(null), "Cache should contain a null key before removal");
        // when
        Integer removed = cache.remove(null);
        // then
        assertEquals(0, removed, "remove should return the value associated with the null key");
        assertFalse(cache.containsKey(null), "Null key should be removed from the cache");
        assertEquals(3, cache.size(), "Cache size should decrease by one after null-key removal");
        // Verify other entries are still present
        assertEquals(1, cache.get("one"));
        assertEquals(2, cache.get("two"));
        assertEquals(3, cache.get("three"));
    }

    @Test
    @DisplayName("remove existing entry with custom data – returns correct value")
    void removeExistingEntry_customKey_returnsValue() {
        // given
        cache.put("user123", "John Doe");
        assertTrue(cache.containsKey("user123"));
        // when
        String removed = cache.remove("user123");
        // then
        assertEquals("John Doe", removed, "remove should return the stored value");
        assertFalse(cache.containsKey("user123"), "Entry should be removed");
        // Size should be unchanged compared to before the extra put (4 -> 4)
        assertEquals(4, cache.size());
    }

    @Test
    @DisplayName("remove entry whose value is null – returns null and removes entry")
    void removeEntryWithNullValue_returnsNullAndRemovesEntry() {
        // given
        cache.put("nullValueKey", null);
        assertTrue(cache.containsKey("nullValueKey"));
        assertNull(cache.get("nullValueKey"));
        // when
        Integer removed = cache.remove("nullValueKey");
        // then
        assertNull(removed, "remove must return null because the stored value is null");
        assertFalse(cache.containsKey("nullValueKey"), "Entry should be removed even though its value was null");
        // Cache size should be back to the original four entries
        assertEquals(4, cache.size());
    }

    @Test
    @DisplayName("remove from an empty cache – returns null")
    void removeFromEmptyCache_returnsNull() {
        // given
        cache.clear();
        assertEquals(0, cache.size(), "Cache should be empty after clear");
        // when
        Integer removed = cache.remove("anyKey");
        // then
        assertNull(removed, "Removing any key from an empty cache must return null");
        assertEquals(0, cache.size(), "Cache must remain empty");
    }

    @Test
    @DisplayName("remove after clear – returns null")
    void removeAfterClear_returnsNull() {
        // given
        cache.put("user123", 42);
        cache.clear();
        assertEquals(0, cache.size(), "Cache should be empty after clear");
        // when
        Integer removed = cache.remove("user123");
        // then
        assertNull(removed, "After clear, removal of a previously existing key must return null");
    }

    @Test
    @DisplayName("remove key of different type – returns null")
    void removeKeyOfDifferentType_returnsNull() {
        // given
        assertTrue(cache.containsKey("one"));
        // when
        // Integer key, cache expects String
        Integer removed = cache.remove(12345);
        // then
        assertNull(removed, "Removal with a key of a different type should return null");
        // Ensure original entries are untouched
        assertEquals(1, cache.get("one"));
        assertEquals(4, cache.size());
    }
}
