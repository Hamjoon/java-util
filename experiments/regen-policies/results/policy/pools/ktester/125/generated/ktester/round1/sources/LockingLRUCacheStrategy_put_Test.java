package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

class LockingLRUCacheStrategy_put_Test {

    private static final int CAPACITY = 2;

    private LockingLRUCacheStrategy<Integer, String> cache;

    private static final int DEFAULT_CAPACITY = 2;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test to guarantee isolation
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to ensure no side‑effects between tests
        cache.clear();
    }

    @Test
    void putNewEntryReturnsNullAndStoresValue() {
        // given
        Integer key = 1;
        String value = "one";
        // when
        String returned = cache.put(key, value);
        // then
        assertNull(returned, "Putting a new key should return null (no previous value).");
        assertEquals(1, cache.size(), "Cache size should be 1 after a single insertion.");
        assertEquals(value, cache.get(key), "The value retrieved should match the inserted value.");
    }

    @Test
    void putExistingKeyUpdatesValueAndReturnsNewValue() {
        // given
        Integer key = 2;
        String firstValue = "first";
        String secondValue = "second";
        // initial insertion
        cache.put(key, firstValue);
        // when
        String returned = cache.put(key, secondValue);
        // then
        assertEquals(secondValue, returned, "Putting an existing key should return the new value.");
        assertEquals(1, cache.size(), "Cache size should remain unchanged when updating an existing key.");
        assertEquals(secondValue, cache.get(key), "The stored value should be the updated one.");
    }

    @Test
    void putEvictsLeastRecentlyUsedWhenCapacityExceeded() {
        // given a cache with capacity 2
        // becomes most‑recent
        cache.put(1, "one");
        // 1 is LRU, 2 is MRU
        cache.put(2, "two");
        // Access key 1 to make it MRU
        assertEquals("one", cache.get(1));
        // when we add a third entry, the LRU (key 2) should be evicted
        cache.put(3, "three");
        // then
        assertNull(cache.get(2), "Key 2 should have been evicted as the least recently used.");
        assertEquals("one", cache.get(1), "Key 1 should still be present.");
        assertEquals("three", cache.get(3), "Key 3 should be present.");
        assertEquals(DEFAULT_CAPACITY, cache.size(), "Cache size should stay at its capacity after eviction.");
    }

    @Test
    void putAllowsNullKeyAndNullValue() {
        // when inserting null key and null value
        String returned = cache.put(null, null);
        // then
        assertNull(returned, "Inserting a null key/value pair should behave like a new entry and return null.");
        assertEquals(1, cache.size(), "Cache should contain the null entry.");
        assertNull(cache.get(null), "Retrieving the null key should return null value.");
    }

    @Test
    @DisplayName("Update existing entry – returns previous value")
    void updateExistingEntryReturnsPreviousValue() {
        // capacity 3, pre‑populate with key "a" -> 1
        LockingLRUCacheStrategy<String, Integer> c = new LockingLRUCacheStrategy<>(3);
        c.put("a", 1);
        // when updating the same key
        Integer returned = c.put("a", 2);
        // then
        assertEquals(1, returned, "Should return the previous value (1).");
        assertEquals(1, c.size(), "Size must stay 1 after an update.");
        assertEquals(2, c.get("a"), "The new value must be stored.");
    }

    @Test
    @DisplayName("Add new entry without eviction (capacity not exceeded)")
    void addNewEntryWithoutEviction() {
        // capacity 3, empty cache
        LockingLRUCacheStrategy<String, String> c = new LockingLRUCacheStrategy<>(3);
        String returned = c.put("b", "val");
        assertNull(returned, "Inserting a new key should return null.");
        assertEquals(1, c.size(), "Cache size should be 1.");
        assertEquals("val", c.get("b"), "Value must be retrievable.");
    }

    @Test
    @DisplayName("Add new entry causing eviction")
    void addNewEntryCausingEviction() {
        // capacity 2, pre‑populate with x=10 (LRU) and y=20 (MRU)
        LockingLRUCacheStrategy<String, Integer> c = new LockingLRUCacheStrategy<>(2);
        c.put("x", 10);
        // now "x" is LRU
        c.put("y", 20);
        // inserting third entry should evict "x"
        String returned = c.put("z", 100);
        assertNull(returned, "Insertion should return null.");
        assertEquals(2, c.size(), "Size stays at capacity.");
        assertNull(c.get("x"), "Key 'x' must have been evicted.");
        assertEquals(20, c.get("y"));
        assertEquals(100, c.get("z"));
    }

    @Test
    @DisplayName("Put null value for existing key – returns previous value")
    void putNullValueForExistingKey() {
        // capacity 3, key "c" -> "initial"
        LockingLRUCacheStrategy<String, String> c = new LockingLRUCacheStrategy<>(3);
        c.put("c", "initial");
        String returned = c.put("c", null);
        assertEquals("initial", returned, "Should return the previous non‑null value.");
        assertNull(c.get("c"), "After update, the value must be null.");
        assertEquals(1, c.size(), "Size remains unchanged.");
    }

    @Test
    @DisplayName("Put null key with non‑null value")
    void putNullKeyWithNonNullValue() {
        // default capacity (2)
        String returned = cache.put(null, "nullKeyValue");
        assertNull(returned, "Inserting a null key should behave like a new entry.");
        assertEquals(1, cache.size(), "Cache size should be 1.");
        assertEquals("nullKeyValue", cache.get(null), "Value for null key must be stored.");
    }

    @Test
    @DisplayName("Insert new entry with null value")
    void insertNewEntryWithNullValue() {
        // capacity 3, empty cache
        LockingLRUCacheStrategy<String, String> c = new LockingLRUCacheStrategy<>(3);
        String returned = c.put("d", null);
        assertNull(returned, "Insertion should return null.");
        assertEquals(1, c.size(), "Size should be 1.");
        assertNull(c.get("d"), "Stored value must be null.");
    }

    @Test
    @DisplayName("Update existing key at full capacity – no eviction")
    void updateExistingKeyAtFullCapacity() {
        // capacity 2, pre‑populate with x=1, y=2
        LockingLRUCacheStrategy<String, Integer> c = new LockingLRUCacheStrategy<>(2);
        c.put("x", 1);
        c.put("y", 2);
        Integer returned = c.put("x", 10);
        assertEquals(1, returned, "Should return previous value (1).");
        assertEquals(2, c.size(), "Size must stay at capacity.");
        assertEquals(10, c.get("x"));
        assertEquals(2, c.get("y"));
    }

    @Test
    @DisplayName("Eviction order after get access")
    void evictionOrderAfterGetAccess() {
        // capacity 3, insert a=1, b=2, c=3
        LockingLRUCacheStrategy<String, Integer> c = new LockingLRUCacheStrategy<>(3);
        c.put("a", 1);
        c.put("b", 2);
        c.put("c", 3);
        // get("a") makes "a" MRU, LRU becomes "b"
        assertEquals(1, c.get("a"));
        // inserting "d" should evict "b"
        c.put("d", 4);
        assertNull(c.get("b"), "Key 'b' should have been evicted.");
        assertEquals(1, c.get("a"));
        assertEquals(3, c.get("c"));
        assertEquals(4, c.get("d"));
        assertEquals(3, c.size(), "Size stays at capacity.");
    }

    @Test
    @DisplayName("Zero‑capacity cache – every put is evicted")
    void zeroCapacityCacheBehavior() {
        LockingLRUCacheStrategy<String, Integer> c = new LockingLRUCacheStrategy<>(0);
        String returned = c.put("any", 5);
        assertNull(returned, "Put should return null.");
        assertEquals(0, c.size(), "Size must remain 0.");
        assertNull(c.get("any"), "Inserted entry must be instantly evicted.");
    }

    @Test
    @DisplayName("Re‑insert previously evicted key")
    void reinsertPreviouslyEvictedKey() {
        // capacity 2, insert p=1, q=2
        LockingLRUCacheStrategy<String, Integer> c = new LockingLRUCacheStrategy<>(2);
        c.put("p", 1);
        c.put("q", 2);
        // inserting r=3 evicts LRU ("p")
        c.put("r", 3);
        assertNull(c.get("p"), "Key 'p' should have been evicted.");
        // re‑insert "p" with new value
        String returned = c.put("p", 4);
        assertNull(returned, "Re‑insertion should behave like a new entry.");
        assertEquals(2, c.size(), "Size stays at capacity.");
        assertEquals(4, c.get("p"));
        assertEquals(2, c.get("q"));
        assertEquals(3, c.get("r"));
    }

    @Test
    @DisplayName("Put null key when underlying map disallows null keys – expect NPE")
    void putNullKeyWhenMapDisallowsNullKeys() throws Exception {
        // Create a cache with normal capacity
        LockingLRUCacheStrategy<Integer, String> c = new LockingLRUCacheStrategy<>(2);
        // Mock the internal ConcurrentHashMapNullSafe to throw NPE on null key
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>> mockMap = mock(ConcurrentHashMapNullSafe.class);
        // Define behaviour: get(null) and put(null, ...) throw NPE
        when(mockMap.get(null)).thenThrow(new NullPointerException("null key"));
        doThrow(new NullPointerException("null key")).when(mockMap).put(eq(null), any());
        // Replace the private 'cache' field via reflection
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(c, mockMap);
        // The put should propagate the NullPointerException
        Executable putNullKey = () -> c.put(null, "someValue");
        assertThrows(NullPointerException.class, putNullKey, "Putting a null key should propagate NullPointerException from the backing map.");
    }
}
