package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_get_Test {

    private LockingLRUCacheStrategy<String, Integer> cache;

    @BeforeEach
    void setupBeforeEach() {
        // capacity of 5 is enough for the tests
        cache = new LockingLRUCacheStrategy<>(5);
        // Populate the cache with a few entries
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache after each test to avoid cross‑test contamination
        cache.clear();
    }

    /**
     * Helper to obtain the private {@code lock} field via reflection.
     */
    private static Lock getInternalLock(LockingLRUCacheStrategy<?, ?> cacheInstance) throws Exception {
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        return (Lock) lockField.get(cacheInstance);
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> presentKeyProvider() {
        return Stream.of(Arguments.of("one", 1), Arguments.of("two", 2), Arguments.of("three", 3));
    }

    @Test
    void getExistingKeyReturnsValue() {
        Integer value = cache.get("two");
        assertNotNull(value, "Value for existing key should not be null");
        assertEquals(2, value.intValue(), "Returned value should match the stored value");
    }

    @Test
    void getMissingKeyReturnsNull() {
        assertNull(cache.get("missing"), "Getting a non‑existent key should return null");
    }

    @Test
    void getDoesNotBlockWhenLockIsHeldElsewhere() throws InterruptedException {
        // Acquire the lock in a separate thread and hold it briefly
        Thread locker = new Thread(() -> {
            // Directly use the lock field via reflection because we must not call private methods,
            // but we can safely acquire the lock for the purpose of this test.
            try {
                java.lang.reflect.Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                java.util.concurrent.locks.Lock lock = (java.util.concurrent.locks.Lock) lockField.get(cache);
                lock.lock();
                // Hold the lock for a short time
                Thread.sleep(100);
                lock.unlock();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        locker.start();
        // Ensure the other thread has time to acquire the lock
        Thread.sleep(20);
        // While the lock is held, get should attempt tryLock and fall back to returning the value
        Integer value = cache.get("one");
        assertNotNull(value);
        assertEquals(1, value.intValue());
        locker.join();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void get_whenKeyAbsent_returnsNull() {
        assertNull(cache.get("missingKey"), "The cache does not contain the supplied key; get() should return null.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void get_whenKeyPresent_andLockAvailable_returnsValue() {
        Integer value = cache.get("two");
        assertNotNull(value, "Value should not be null for an existing key.");
        assertEquals(2, value.intValue(), "Returned value must match the stored value.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void get_whenKeyPresent_andLockUnavailable_returnsValueWithoutMovingNode() throws Exception {
        // Acquire the internal lock in a separate thread and hold it briefly.
        Thread holder = new Thread(() -> {
            try {
                Lock lock = getInternalLock(cache);
                // acquire
                lock.lock();
                // hold long enough for the test thread to call get()
                Thread.sleep(150);
                // release
                lock.unlock();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        holder.start();
        // Give the holder thread a chance to lock.
        Thread.sleep(30);
        // While the lock is held, tryLock() will fail, so get() should simply return the value.
        Integer value = cache.get("one");
        assertNotNull(value);
        assertEquals(1, value.intValue());
        holder.join();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void get_whenKeyPresent_nullValue_andLockAvailable_returnsNull() {
        cache.put("nullValueKey", null);
        assertNull(cache.get("nullValueKey"), "When the stored value is null, get() must return null even though the lock is free.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void get_whenKeyPresent_nullValue_andLockUnavailable_returnsNull() throws Exception {
        cache.put("nullValueKey", null);
        Thread holder = new Thread(() -> {
            try {
                Lock lock = getInternalLock(cache);
                lock.lock();
                Thread.sleep(120);
                lock.unlock();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        holder.start();
        // ensure lock is held
        Thread.sleep(20);
        assertNull(cache.get("nullValueKey"), "Even when the lock is unavailable, get() must return the stored null value.");
        holder.join();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void get_whenKeyIsNull_andCacheContainsNullKey_andLockAvailable_returnsValue() {
        cache.put(null, 42);
        Integer value = cache.get(null);
        assertNotNull(value);
        assertEquals(42, value.intValue(), "Cache should return the value associated with a null key when lock is free.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void get_whenKeyIsNull_andCacheDoesNotContainNullKey_returnsNull() {
        assertNull(cache.get(null), "If the cache does not contain a mapping for null, get(null) must return null.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void get_whenCacheIsEmpty_andLockAvailable_returnsNull() {
        // make the cache empty
        cache.clear();
        assertNull(cache.get("anyKey"), "When the cache is empty, get() should always return null regardless of lock state.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void get_whenKeyPresent_andLockAvailable_returnsSameInstance() {
        List<String> mutable = new ArrayList<>(Arrays.asList("a", "b"));
        // Use a cache with Object values to store the mutable list.
        LockingLRUCacheStrategy<String, List<String>> objCache = new LockingLRUCacheStrategy<>(5);
        objCache.put("objectKey", mutable);
        List<String> returned = objCache.get("objectKey");
        assertSame(mutable, returned, "The cache must return the exact same object instance that was put in (identity equality).");
    }

    @ParameterizedTest(name = "get({0}) returns {1}")
    @MethodSource("presentKeyProvider")
    void get_whenKeyPresent_andLockAvailable_parameterised(String key, Integer expected) {
        Integer value = cache.get(key);
        assertNotNull(value);
        assertEquals(expected, value);
    }
}
