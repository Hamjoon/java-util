package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.ConcurrentHashMapNullSafe;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class LockingLRUCacheStrategy_clear_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<String, Integer> cache;

    class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad hashCode");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad hashCode");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that the lock is released for the next test
        cache.clear();
    }

    /**
     * Retrieves the value of a private field from the given target object.
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Sets the value of a private (or final) field on the given target object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        // Remove final modifier if present (required for Java 12+)
        java.lang.reflect.Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(target, value);
    }

    @Test
    void clearRemovesAllEntries_andCacheIsEmpty() {
        // given: a cache populated with several entries
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        assertEquals(3, cache.size(), "Pre‑condition: cache should contain three entries");
        assertFalse(cache.isEmpty(), "Pre‑condition: cache should not be empty");
        assertEquals(1, cache.get("one"));
        assertEquals(2, cache.get("two"));
        assertEquals(3, cache.get("three"));
        // when: clear() is invoked
        cache.clear();
        // then: the cache reports as empty and all look‑ups return null
        assertEquals(0, cache.size(), "Cache size must be zero after clear()");
        assertTrue(cache.isEmpty(), "Cache must be empty after clear()");
        assertNull(cache.get("one"));
        assertNull(cache.get("two"));
        assertNull(cache.get("three"));
        assertFalse(cache.containsKey("one"));
        assertFalse(cache.containsValue(1));
    }

    @Test
    void clearAllowsCacheToBeReusedAfterwards() {
        // given: a cache with some data that is cleared
        cache.put("alpha", 10);
        cache.put("beta", 20);
        cache.clear();
        // when: a new entry is added after the clear
        cache.put("gamma", 30);
        // then: the cache behaves like a fresh instance
        assertEquals(1, cache.size(), "Cache should contain exactly one entry after re‑use");
        assertFalse(cache.isEmpty());
        assertEquals(30, cache.get("gamma"));
        assertNull(cache.get("alpha"));
        assertNull(cache.get("beta"));
    }

    @Test
    void clearIsIdempotent() {
        // given: an empty cache
        assertTrue(cache.isEmpty());
        // when: clear() is called multiple times
        cache.clear();
        cache.clear();
        cache.clear();
        // then: the cache remains empty and no exception is thrown
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /**
     * Verify that calling {@code clear()} on a freshly constructed (empty) cache
     * resets the internal linked‑list pointers and does not leave the lock held.
     */
    @Test
    void clearOnEmptyCache_resetsPointersAndReleasesLock() throws Exception {
        // pre‑condition: cache is empty
        assertTrue(cache.isEmpty());
        // invoke clear
        cache.clear();
        // verify internal linked list: head.next == tail && tail.prev == head
        Object head = getPrivateField(cache, "head");
        Object tail = getPrivateField(cache, "tail");
        Object headNext = getPrivateField(head, "next");
        Object tailPrev = getPrivateField(tail, "prev");
        assertSame(tail, headNext, "head.next should point to tail after clear()");
        assertSame(head, tailPrev, "tail.prev should point to head after clear()");
        // verify lock is not held
        ReentrantLock lock = (ReentrantLock) getPrivateField(cache, "lock");
        assertFalse(lock.isLocked(), "Lock must be released after clear()");
    }

    /**
     * Populate the cache, clear it and verify that the linked‑list is reset and the lock is released.
     */
    @Test
    void clearOnNonEmptyCache_resetsPointersAndReleasesLock() throws Exception {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        assertEquals(3, cache.size());
        cache.clear();
        // linked‑list sanity checks
        Object head = getPrivateField(cache, "head");
        Object tail = getPrivateField(cache, "tail");
        assertSame(tail, getPrivateField(head, "next"));
        assertSame(head, getPrivateField(tail, "prev"));
        // lock must be free
        ReentrantLock lock = (ReentrantLock) getPrivateField(cache, "lock");
        assertFalse(lock.isLocked());
        assertTrue(cache.isEmpty());
    }

    /**
     * Calling {@code clear()} twice on an already‑empty cache must be a no‑op
     * and must not leave the lock held.
     */
    @Test
    void clearTwiceOnEmptyCache_isIdempotentAndLeavesLockFree() throws Exception {
        // first clear – cache already empty
        cache.clear();
        // second clear
        cache.clear();
        // linked‑list pointers must still be correct
        Object head = getPrivateField(cache, "head");
        Object tail = getPrivateField(cache, "tail");
        assertSame(tail, getPrivateField(head, "next"));
        assertSame(head, getPrivateField(tail, "prev"));
        // lock must be free
        ReentrantLock lock = (ReentrantLock) getPrivateField(cache, "lock");
        assertFalse(lock.isLocked());
    }

    /**
     * Simulate another thread holding the internal lock while {@code clear()} is invoked.
     * The method should wait for the lock, then release it and reset the cache.
     */
    @Test
    // guard against deadlock
    @Timeout(2)
    void clearWhileAnotherThreadHoldsLock_waitsAndReleases() throws Exception {
        // Populate cache so we have something to clear
        cache.put("x", 100);
        cache.put("y", 200);
        assertEquals(2, cache.size());
        // Acquire the lock in a separate thread and hold it briefly
        ReentrantLock lock = (ReentrantLock) getPrivateField(cache, "lock");
        Thread locker = new Thread(() -> {
            lock.lock();
            try {
                // hold the lock for a short period
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
            } finally {
                lock.unlock();
            }
        });
        locker.start();
        // Ensure the other thread has taken the lock before we call clear()
        while (!lock.isLocked()) {
            Thread.yield();
        }
        // This call should block until the other thread releases the lock
        cache.clear();
        // After clear returns, the lock must be free
        assertFalse(lock.isLocked(), "Lock must be released after clear() returns");
        // Cache must be empty and linked list reset
        assertTrue(cache.isEmpty());
        Object head = getPrivateField(cache, "head");
        Object tail = getPrivateField(cache, "tail");
        assertSame(tail, getPrivateField(head, "next"));
        assertSame(head, getPrivateField(tail, "prev"));
        // clean up
        locker.join();
    }

    /**
     * Insert entries whose keys throw an exception from {@code hashCode()}.
     * {@code clear()} must not propagate that exception because it does not
     * need to compute hash codes while clearing.
     */
    @Test
    void clearWithKeyHavingBadHashCode_doesNotPropagateException() {
        class BadKey {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad hashCode");
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }
        }
        // The cache implementation accepts Object as key, so we can cast
        @SuppressWarnings("unchecked")
        LockingLRUCacheStrategy<Object, Integer> rawCache = (LockingLRUCacheStrategy<Object, Integer>) cache;
        rawCache.put(new BadKey(), 42);
        // second insertion replaces the first
        rawCache.put(new BadKey(), 43);
        // clear must succeed without bubbling the RuntimeException from hashCode()
        assertDoesNotThrow(() -> rawCache.clear());
        assertTrue(rawCache.isEmpty());
    }

    /**
     * Replace the internal {@code cache} field with a mock that throws
     * a {@code RuntimeException} when {@code clear()} is called.
     * Verify that the exception propagates out of {@code LockingLRUCacheStrategy.clear()}.
     */
    @Test
    void clearPropagatesExceptionFromUnderlyingCache() throws Exception {
        // Create a mock ConcurrentHashMapNullSafe that throws on clear()
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<String, Integer>> mockMap = mock(ConcurrentHashMapNullSafe.class);
        doThrow(new RuntimeException("boom")).when(mockMap).clear();
        // Inject the mock into the cache instance via reflection
        setPrivateField(cache, "cache", mockMap);
        // Expect the RuntimeException to bubble up
        RuntimeException thrown = assertThrows(RuntimeException.class, cache::clear);
        assertEquals("boom", thrown.getMessage());
    }

    /**
     * Force {@code head} to {@code null} via reflection and verify that {@code clear()}
     * throws a {@code NullPointerException}.
     */
    @Test
    void clearThrowsWhenHeadIsNull() throws Exception {
        setPrivateField(cache, "head", null);
        assertThrows(NullPointerException.class, cache::clear);
    }

    /**
     * Force {@code tail} to {@code null} via reflection and verify that {@code clear()}
     * throws a {@code NullPointerException}.
     */
    @Test
    void clearThrowsWhenTailIsNull() throws Exception {
        setPrivateField(cache, "tail", null);
        assertThrows(NullPointerException.class, cache::clear);
    }
}
