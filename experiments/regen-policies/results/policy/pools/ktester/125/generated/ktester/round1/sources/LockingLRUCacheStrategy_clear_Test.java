package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.ConcurrentHashMapNullSafe;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class LockingLRUCacheStrategy_clear_Test {

    private static final int CAPACITY = 3;

    private LockingLRUCacheStrategy<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache instance before each test
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache reference is cleared after each test
        cache = null;
    }

    @Test
    void clear_RemovesAllEntriesAndResetsState() {
        // GIVEN: a cache populated with several entries
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        assertEquals(3, cache.size(), "Pre‑condition: cache should contain three entries");
        assertFalse(cache.isEmpty(), "Pre‑condition: cache should not be empty");
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        // WHEN: clear is invoked
        cache.clear();
        // THEN: the cache reports as empty and all previous keys are absent
        assertEquals(0, cache.size(), "Cache size must be zero after clear");
        assertTrue(cache.isEmpty(), "Cache must be empty after clear");
        assertNull(cache.get(1), "Previously stored key 1 must no longer be present");
        assertNull(cache.get(2), "Previously stored key 2 must no longer be present");
        assertNull(cache.get(3), "Previously stored key 3 must no longer be present");
    }

    @Test
    void clear_AllowsSubsequentOperations() {
        // GIVEN: a populated cache that is then cleared
        cache.put(10, "ten");
        cache.put(20, "twenty");
        cache.clear();
        // WHEN: new entries are added after the clear
        cache.put(30, "thirty");
        cache.put(40, "forty");
        // THEN: the new entries are stored correctly
        assertEquals(2, cache.size(), "Cache should contain two entries after re‑populating");
        assertEquals("thirty", cache.get(30));
        assertEquals("forty", cache.get(40));
        // Ensure that old keys are still absent
        assertNull(cache.get(10));
        assertNull(cache.get(20));
    }

    @Test
    void clear_IsThreadSafeAndDoesNotLeaveLockHeld() {
        // GIVEN: a cache with entries
        cache.put(5, "five");
        cache.put(6, "six");
        // WHEN: clear is called from the test thread
        cache.clear();
        // THEN: the lock must have been released (no deadlock when we invoke another method)
        // Attempt to acquire the lock indirectly by calling another mutating method
        // If the lock were still held, this call would block and the test would timeout.
        assertDoesNotThrow(() -> cache.put(7, "seven"));
        assertEquals(1, cache.size());
        assertEquals("seven", cache.get(7));
    }

    @Test
    void clear_OnAlreadyEmptyCache_LeavesListInInitialState() throws Exception {
        // GIVEN: a freshly created (empty) cache
        assertEquals(0, cache.size());
        // WHEN: clear() is called on the empty cache
        cache.clear();
        // THEN: internal linked‑list pointers must be reset (head.next == tail && tail.prev == head)
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
        headField.setAccessible(true);
        tailField.setAccessible(true);
        Object head = headField.get(cache);
        Object tail = tailField.get(cache);
        Field nextField = head.getClass().getDeclaredField("next");
        Field prevField = tail.getClass().getDeclaredField("prev");
        nextField.setAccessible(true);
        prevField.setAccessible(true);
        assertSame(tail, nextField.get(head), "head.next should point to tail after clear");
        assertSame(head, prevField.get(tail), "tail.prev should point to head after clear");
    }

    @Test
    void clear_WhenExceptionThrown_AlwaysUnlocks() throws Exception {
        // GIVEN: a cache where the underlying map throws a RuntimeException on clear()
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>> mockMap = mock(ConcurrentHashMapNullSafe.class);
        doThrow(new RuntimeException("simulated failure")).when(mockMap).clear();
        // Replace the private final 'cache' field via reflection
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, mockMap);
        // WHEN: clear() is invoked – we expect the RuntimeException to propagate
        RuntimeException thrown = assertThrows(RuntimeException.class, cache::clear, "clear() should propagate the RuntimeException from the underlying map");
        assertEquals("simulated failure", thrown.getMessage());
        // THEN: the lock must have been released; subsequent operations must not deadlock
        assertDoesNotThrow(() -> cache.put(99, "ninety‑nine"));
        assertEquals(1, cache.size());
        assertEquals("ninety‑nine", cache.get(99));
        // Additionally verify that the lock is not held by the current thread
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        Lock lock = (Lock) lockField.get(cache);
        assertTrue(((ReentrantLock) lock).isLocked() == false, "Lock should be released even after an exception");
    }

    @Test
    void clear_LockContention_BlocksOtherThreadsUntilRelease() throws Exception {
        // GIVEN: we manually acquire the internal lock before invoking clear()
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        ReentrantLock internalLock = (ReentrantLock) lockField.get(cache);
        // hold the lock
        internalLock.lock();
        CountDownLatch clearStarted = new CountDownLatch(1);
        Thread clearThread = new Thread(() -> {
            // signal that clear() has been entered
            clearStarted.countDown();
            cache.clear();
        });
        clearThread.start();
        // Ensure the clear thread has started and is now blocked on the lock
        assertTrue(clearStarted.await(1, TimeUnit.SECONDS), "clear thread should start");
        // give a tiny window for the thread to attempt lock acquisition
        Thread.sleep(100);
        assertTrue(clearThread.isAlive(), "clear() must be blocked while the lock is held by the test thread");
        // WHEN: we release the lock
        internalLock.unlock();
        // THEN: clear() should finish promptly
        clearThread.join(1000);
        assertFalse(clearThread.isAlive(), "clear() should complete after the lock is released");
        assertEquals(0, cache.size(), "Cache must be empty after clear()");
    }

    @Test
    void clear_HeadIsNull_ThrowsNullPointerException() throws Exception {
        // GIVEN: head field forced to null via reflection
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(cache, null);
        // WHEN/THEN: clear() should throw NPE because it tries to access head.next
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NPE when internal head node is null");
    }

    @Test
    void clear_TailIsNull_ThrowsNullPointerException() throws Exception {
        // GIVEN: tail field forced to null via reflection
        Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
        tailField.setAccessible(true);
        tailField.set(cache, null);
        // WHEN/THEN: clear() should throw NPE because it tries to access tail.prev
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NPE when internal tail node is null");
    }

    @Test
    void clear_CacheMapIsNull_ThrowsNullPointerException() throws Exception {
        // GIVEN: the internal map forced to null via reflection
        Field cacheMapField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheMapField.setAccessible(true);
        cacheMapField.set(cache, null);
        // WHEN/THEN: clear() should throw NPE when invoking cache.clear()
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NPE when internal cache map is null");
    }
}
