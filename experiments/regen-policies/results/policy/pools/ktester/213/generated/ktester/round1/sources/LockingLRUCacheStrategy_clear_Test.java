package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class LockingLRUCacheStrategy_clear_Test {

    private LockingLRUCacheStrategy<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Create a cache with a modest capacity and pre‑populate it
        cache = new LockingLRUCacheStrategy<>(5);
        cache.put(1, "one");
        cache.put(2, "two");
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no leftover state between tests
        cache = null;
    }

    /**
     * Sets a private field value via reflection.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the internal {@link ReentrantLock} used by the cache.
     */
    private static ReentrantLock getInternalLock(LockingLRUCacheStrategy<?, ?> cache) {
        Lock lock = getPrivateField(cache, "lock");
        // The implementation uses ReentrantLock, safe to cast
        return (ReentrantLock) lock;
    }

    /**
     * Returns the internal head node of the LRU list.
     */
    private static Object getHeadNode(LockingLRUCacheStrategy<?, ?> cache) {
        return getPrivateField(cache, "head");
    }

    /**
     * Returns the internal tail node of the LRU list.
     */
    private static Object getTailNode(LockingLRUCacheStrategy<?, ?> cache) {
        return getPrivateField(cache, "tail");
    }

    @Test
    void clear_RemovesAllEntries() {
        // given – the cache is populated in @BeforeEach
        assertEquals(2, cache.size(), "Pre‑condition: cache should contain two entries");
        // when
        cache.clear();
        // then – cache should be empty and behave as a fresh instance
        assertTrue(cache.isEmpty(), "Cache should be empty after clear()");
        assertEquals(0, cache.size(), "Size should be zero after clear()");
        assertFalse(cache.containsKey(1), "Old key should no longer be present");
        assertFalse(cache.containsValue("one"), "Old value should no longer be present");
        assertTrue(cache.keySet().isEmpty(), "keySet should be empty after clear()");
        assertTrue(cache.values().isEmpty(), "values collection should be empty after clear()");
    }

    @Test
    void clear_AllowsSubsequentPutAndGet() {
        // when
        cache.clear();
        // then – cache behaves normally after being cleared
        assertTrue(cache.isEmpty(), "Cache should be empty after clear()");
        // when – add a new entry
        cache.put(10, "ten");
        // then – the new entry is stored correctly
        assertEquals(1, cache.size(), "Cache should contain one entry after put()");
        assertEquals("ten", cache.get(10), "Value retrieved should match the one put");
        assertTrue(cache.containsKey(10), "Cache should contain the newly added key");
        assertTrue(cache.containsValue("ten"), "Cache should contain the newly added value");
    }

    /**
     * Retrieves a private field value via reflection.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void clear_OnPopulatedCache_ResetsInternalListAndReleasesLock() {
        // pre‑condition: head and tail are linked with real nodes
        Object head = getHeadNode(cache);
        Object tail = getTailNode(cache);
        assertNotNull(head, "head should not be null before clear");
        assertNotNull(tail, "tail should not be null before clear");
        // when
        cache.clear();
        // then – internal LRU list is reset (head.next == tail && tail.prev == head)
        Object headNext = getPrivateField(head, "next");
        Object tailPrev = getPrivateField(tail, "prev");
        assertSame(tail, headNext, "head.next must point to tail after clear()");
        assertSame(head, tailPrev, "tail.prev must point to head after clear()");
        // lock must be released
        ReentrantLock lock = getInternalLock(cache);
        assertFalse(lock.isLocked(), "Lock must be released after clear()");
    }

    @Test
    void clear_OnEmptyCache_LeavesStructureIntactAndReleasesLock() {
        // Arrange: clear the cache first to make it empty
        cache.clear();
        // Verify empty state before the second clear
        assertTrue(cache.isEmpty(), "Cache should be empty before second clear");
        // Capture internal references for later comparison
        Object headBefore = getHeadNode(cache);
        Object tailBefore = getTailNode(cache);
        ReentrantLock lockBefore = getInternalLock(cache);
        assertFalse(lockBefore.isLocked(), "Lock should be unlocked before clear");
        // when – clear again on an already empty cache
        cache.clear();
        // then – still empty
        assertTrue(cache.isEmpty(), "Cache should remain empty after clear on empty cache");
        assertEquals(0, cache.size(), "Size must stay zero");
        // internal list must still be correctly linked
        Object headNext = getPrivateField(headBefore, "next");
        Object tailPrev = getPrivateField(tailBefore, "prev");
        assertSame(tailBefore, headNext, "head.next must still point to tail");
        assertSame(headBefore, tailPrev, "tail.prev must still point to head");
        // lock must still be released
        ReentrantLock lockAfter = getInternalLock(cache);
        assertFalse(lockAfter.isLocked(), "Lock must be released after clear on empty cache");
    }

    @Test
    void clear_TwiceInARow_IdempotentAndLockReleased() {
        // First clear
        cache.clear();
        // Capture state after first clear
        Object head = getHeadNode(cache);
        Object tail = getTailNode(cache);
        ReentrantLock lock = getInternalLock(cache);
        assertFalse(lock.isLocked(), "Lock should be unlocked after first clear");
        // Second clear
        cache.clear();
        // Verify state unchanged
        assertTrue(cache.isEmpty(), "Cache remains empty after second clear");
        assertEquals(0, cache.size(), "Size remains zero after second clear");
        // List links still correct
        Object headNext = getPrivateField(head, "next");
        Object tailPrev = getPrivateField(tail, "prev");
        assertSame(tail, headNext, "head.next still points to tail after second clear");
        assertSame(head, tailPrev, "tail.prev still points to head after second clear");
        // Lock must still be released
        assertFalse(lock.isLocked(), "Lock must be released after second clear");
    }

    @Test
    void clear_WhenInternalCacheIsNull_ThrowsNullPointerException() {
        // Force the internal cache field to null via reflection
        setPrivateField(cache, "cache", null);
        // clear() should now throw NPE when it attempts cache.clear()
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NullPointerException when internal cache is null");
    }

    @Test
    void clear_WhenHeadIsNull_ThrowsNullPointerException() {
        // Set head to null
        setPrivateField(cache, "head", null);
        // Expect NPE when trying to assign head.next = tail
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NullPointerException when head node is null");
    }

    @Test
    void clear_WhenTailIsNull_ThrowsNullPointerException() {
        // Set tail to null
        setPrivateField(cache, "tail", null);
        // Expect NPE when trying to assign tail.prev = head
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NullPointerException when tail node is null");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void clear_LockIsAlwaysReleased(boolean prepopulate) {
        // Optional pre‑population based on the parameter
        if (!prepopulate) {
            // make it empty first
            cache.clear();
        }
        // Acquire the lock manually to ensure clear() releases it afterwards
        ReentrantLock lock = getInternalLock(cache);
        assertFalse(lock.isLocked(), "Lock should be free before invoking clear()");
        // invoke clear()
        cache.clear();
        // After clear, lock must not be held
        assertFalse(lock.isLocked(), "Lock must be released after clear() regardless of prior state");
    }
}
