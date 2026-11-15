package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.ConcurrentHashMapNullSafe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;

class LockingLRUCacheStrategy_size_Test {

    private static final int CAPACITY = 10;

    private LockingLRUCacheStrategy<String, Integer> cacheStrategy;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test to guarantee isolation.
        cacheStrategy = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to help GC and avoid side‑effects if a test forgets to do so.
        cacheStrategy.clear();
    }

    @Test
    @DisplayName("size() returns 0 for a newly created cache")
    void sizeIsZeroWhenEmpty() {
        assertEquals(0, cacheStrategy.size(), "New cache should be empty");
    }

    @Test
    @DisplayName("size() reflects the number of entries added via put")
    void sizeReflectsNumberOfPuts() {
        cacheStrategy.put("one", 1);
        cacheStrategy.put("two", 2);
        cacheStrategy.put("three", 3);
        assertEquals(3, cacheStrategy.size(), "Cache size should match number of inserted entries");
    }

    @Test
    @DisplayName("size() decreases after removing an entry")
    void sizeDecreasesAfterRemove() {
        cacheStrategy.put("a", 10);
        cacheStrategy.put("b", 20);
        assertEquals(2, cacheStrategy.size());
        cacheStrategy.remove("a");
        assertEquals(1, cacheStrategy.size(), "Cache size should decrement after removal");
    }

    @Test
    @DisplayName("size() becomes zero after clear()")
    void sizeZeroAfterClear() {
        cacheStrategy.put("x", 100);
        cacheStrategy.put("y", 200);
        assertTrue(cacheStrategy.size() > 0, "Cache should contain entries before clear");
        cacheStrategy.clear();
        assertEquals(0, cacheStrategy.size(), "Cache size should be zero after clear");
    }

    @Test
    @DisplayName("size() respects capacity limit and LRU eviction")
    void sizeRespectsCapacityAndEviction() {
        // Fill cache up to its capacity
        for (int i = 0; i < CAPACITY; i++) {
            cacheStrategy.put("k" + i, i);
        }
        assertEquals(CAPACITY, cacheStrategy.size());
        // Adding one more entry should evict the least recently used entry,
        // keeping the size constant.
        cacheStrategy.put("extra", 999);
        assertEquals(CAPACITY, cacheStrategy.size(), "Size should stay equal to capacity after eviction of LRU entry");
    }

    @Test
    @DisplayName("size() idempotently returns 0 on an empty cache")
    void sizeIdempotentOnEmptyCache() {
        // Call size() repeatedly and ensure the result never changes.
        for (int i = 0; i < 5; i++) {
            assertEquals(0, cacheStrategy.size(), "size() should consistently return 0 on an empty cache (iteration " + i + ")");
        }
    }

    @Test
    @DisplayName("size() remains 1 when inserting a duplicate key")
    void sizeRemainsOneOnDuplicateKey() {
        cacheStrategy.put("dupKey", 1);
        assertEquals(1, cacheStrategy.size(), "Size should be 1 after first insertion");
        // Insert the same key with a different value – should replace, not grow.
        cacheStrategy.put("dupKey", 2);
        assertEquals(1, cacheStrategy.size(), "Size should stay 1 after inserting a duplicate key with a new value");
    }

    @Test
    @DisplayName("size() throws NullPointerException when internal cache is null")
    void sizeThrowsWhenInternalCacheIsNull() throws Exception {
        // Use reflection to set the private final field 'cache' to null.
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cacheStrategy, null);
        // The contract of size() is to dereference the field, so a NPE is expected.
        assertThrows(NullPointerException.class, () -> cacheStrategy.size(), "Calling size() with a null internal cache should throw NullPointerException");
    }

    @Test
    @DisplayName("size() propagates RuntimeException thrown by underlying map")
    void sizePropagatesExceptionFromUnderlyingMap() throws Exception {
        // Create a mock that throws when size() is invoked.
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<String, Integer>> mockMap = (ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<String, Integer>>) Mockito.mock(ConcurrentHashMapNullSafe.class);
        when(mockMap.size()).thenThrow(new RuntimeException("forced failure"));
        // Replace the real cache with the misbehaving mock via reflection.
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cacheStrategy, mockMap);
        // Verify that the exception bubbles up unchanged.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cacheStrategy.size(), "size() should propagate RuntimeException from the underlying map");
        assertEquals("forced failure", ex.getMessage());
    }

    @ParameterizedTest(name = "size() call #{index} on empty cache returns 0")
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5 })
    @DisplayName("Repeated size() calls on empty cache remain consistent")
    void repeatedSizeCallsOnEmptyCache(int iteration) {
        assertEquals(0, cacheStrategy.size(), "Iteration " + iteration + ": size() must consistently return 0 on an empty cache");
    }
}
