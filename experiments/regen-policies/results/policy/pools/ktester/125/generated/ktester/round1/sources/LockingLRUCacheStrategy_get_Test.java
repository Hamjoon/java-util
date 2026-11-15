package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_get_Test {

    private static final int CAPACITY = 3;

    private LockingLRUCacheStrategy<Integer, String> cache;

    /**
     * Simple holder for branch‑coverage parameters.
     */
    private static class BranchCase {

        final Object key;

        // value stored in the node (null means node == null)
        final String value;

        // result of lock.tryLock()
        final boolean lockSucceeds;

        // expected return from get()
        final Object expectedResult;

        final String description;

        BranchCase(Object key, String value, boolean lockSucceeds, Object expectedResult, String description) {
            this.key = key;
            this.value = value;
            this.lockSucceeds = lockSucceeds;
            this.expectedResult = expectedResult;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
        // Populate with a few entries.
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests.
        cache.clear();
    }

    /* -------------------------------------------------------------
     *  Parameterised branch‑coverage tests
     * ------------------------------------------------------------- */
    private static Stream<BranchCase> branchCases() {
        return Stream.of(new BranchCase(null, null, false, "null", "When the key is null and the underlying cache returns null, get should return null without attempting to lock."), new BranchCase("missingKey", null, false, null, "Key not present in the cache; node == null branch, method returns null."), new BranchCase("presentKey", "value1", true, "value1", "Key present, lock.tryLock() succeeds, moveToHead is invoked, and the stored value is returned."), new BranchCase("presentKeyLockFail", "value2", false, "value2", "Key present, but lock.tryLock() returns false. moveToHead is NOT called, but the value is still returned."));
    }

    /* -------------------------------------------------------------
     *  Helper utilities
     * ------------------------------------------------------------- */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void get_existingKey_returnsCorrespondingValue() {
        // given
        Integer key = 2;
        // when
        String result = cache.get(key);
        // then
        assertEquals("two", result, "get() should return the value associated with the key");
    }

    @Test
    void get_nonExistingKey_returnsNull() {
        // given
        Integer missingKey = 99;
        // when
        String result = cache.get(missingKey);
        // then
        assertNull(result, "get() should return null when the key is not present");
    }

    @Test
    void get_nullKey_whenPresent_returnsValue() {
        // given
        cache.put(null, "nullValue");
        // when
        String result = cache.get(null);
        // then
        assertEquals("nullValue", result, "Cache must support null keys");
    }

    @Test
    void get_nullKey_whenAbsent_returnsNull() {
        // given
        // No entry for null key in the cache (setup does not add one)
        // when
        String result = cache.get(null);
        // then
        assertNull(result, "Cache should return null for a missing null key");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("branchCases")
    void get_branchCoverage(BranchCase c) throws Exception {
        // -----------------------------------------------------------------
        // 1️⃣  Prepare a mock cache that returns a Node with the expected value
        // -----------------------------------------------------------------
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>> mockCache = (ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>>) mock(ConcurrentHashMapNullSafe.class);
        // When key is null we let the mock behave as the real map would (return null)
        if (c.key != null) {
            LockingLRUCacheStrategy.Node<Integer, String> node = new LockingLRUCacheStrategy.Node<>();
            // Use reflection to set the private 'value' field of Node
            Field valueField = node.getClass().getDeclaredField("value");
            valueField.setAccessible(true);
            valueField.set(node, c.value);
            when(mockCache.get(c.key)).thenReturn(node);
        } else {
            when(mockCache.get(null)).thenReturn(null);
        }
        // -----------------------------------------------------------------
        // 2️⃣  Prepare a mock lock that either succeeds or fails on tryLock()
        // -----------------------------------------------------------------
        Lock mockLock = mock(Lock.class);
        when(mockLock.tryLock()).thenReturn(c.lockSucceeds);
        // unlock should be a no‑op
        doNothing().when(mockLock).unlock();
        // -----------------------------------------------------------------
        // 3️⃣  Inject the mocks via reflection
        // -----------------------------------------------------------------
        setPrivateField(cache, "cache", mockCache);
        setPrivateField(cache, "lock", mockLock);
        // -----------------------------------------------------------------
        // 4️⃣  Invoke the method under test
        // -----------------------------------------------------------------
        Object result = cache.get(c.key);
        // -----------------------------------------------------------------
        // 5️⃣  Assertions
        // -----------------------------------------------------------------
        assertEquals(c.expectedResult, result, c.description);
        // verify lock usage according to the scenario
        if (c.lockSucceeds && c.key != null && c.value != null) {
            verify(mockLock, times(1)).tryLock();
            verify(mockLock, times(1)).unlock();
        } else {
            // when tryLock fails or node is null, unlock must not be called
            verify(mockLock, atMost(1)).tryLock();
            verify(mockLock, never()).unlock();
        }
    }

    /* -------------------------------------------------------------
     *  Additional scenarios
     * ------------------------------------------------------------- */
    @Test
    void get_presentKeyNullValue() throws Exception {
        // key present but value is null; lock succeeds
        String key = "presentKeyNullValue";
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>> mockCache = (ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>>) mock(ConcurrentHashMapNullSafe.class);
        LockingLRUCacheStrategy.Node<Integer, String> node = new LockingLRUCacheStrategy.Node<>();
        // value stays null
        Field valueField = node.getClass().getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(node, null);
        when(mockCache.get(key)).thenReturn(node);
        Lock mockLock = mock(Lock.class);
        when(mockLock.tryLock()).thenReturn(true);
        doNothing().when(mockLock).unlock();
        setPrivateField(cache, "cache", mockCache);
        setPrivateField(cache, "lock", mockLock);
        Object result = cache.get(key);
        assertNull(result, "When the stored value is null the method must return null");
    }

    @Test
    void get_presentKeyNullValueLockFails() throws Exception {
        // same as above but tryLock() returns false
        String key = "presentKeyNullValueLockFail";
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>> mockCache = (ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>>) mock(ConcurrentHashMapNullSafe.class);
        LockingLRUCacheStrategy.Node<Integer, String> node = new LockingLRUCacheStrategy.Node<>();
        // value stays null
        Field valueField = node.getClass().getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(node, null);
        when(mockCache.get(key)).thenReturn(node);
        Lock mockLock = mock(Lock.class);
        when(mockLock.tryLock()).thenReturn(false);
        // unlock never called
        setPrivateField(cache, "cache", mockCache);
        setPrivateField(cache, "lock", mockLock);
        Object result = cache.get(key);
        assertNull(result, "Even when lock is not obtained the stored null value must be returned");
        verify(mockLock, times(1)).tryLock();
        verify(mockLock, never()).unlock();
    }

    @Test
    void get_presentKeyDifferentType() throws Exception {
        // Cache is typed with Integer keys, but we store a String key via the underlying map.
        String key = "presentKeyDifferentType";
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>> mockCache = (ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>>) mock(ConcurrentHashMapNullSafe.class);
        LockingLRUCacheStrategy.Node<Integer, String> node = new LockingLRUCacheStrategy.Node<>();
        Field valueField = node.getClass().getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(node, "value3");
        when(mockCache.get(key)).thenReturn(node);
        Lock mockLock = mock(Lock.class);
        when(mockLock.tryLock()).thenReturn(true);
        doNothing().when(mockLock).unlock();
        setPrivateField(cache, "cache", mockCache);
        setPrivateField(cache, "lock", mockLock);
        Object result = cache.get(key);
        assertEquals("value3", result, "The method must work even if the key object's runtime type differs from K");
    }

    @Test
    void get_presentKeyOrderEffect() throws Exception {
        // Populate cache with distinct keys to observe LRU order.
        cache.clear();
        cache.put(10, "ten");
        cache.put(20, "twenty");
        // LRU order: 30 (most recent), 20, 10 (least)
        cache.put(30, "thirty");
        // Access the least‑recently‑used key (10)
        String value = cache.get(10);
        assertEquals("ten", value, "Value must be returned correctly");
        // After a successful tryLock (default ReentrantLock), 10 should become most‑recent.
        // The entry set reflects LRU order (head first). Verify that 10 is now first.
        List<Integer> keysInOrder = new ArrayList<>();
        for (Map.Entry<Integer, String> e : cache.entrySet()) {
            keysInOrder.add(e.getKey());
        }
        assertEquals(List.of(10, 30, 20), keysInOrder, "After get(10) the accessed key should move to the head of the LRU list");
    }

    /* -------------------------------------------------------------
     *  Exception scenarios
     * ------------------------------------------------------------- */
    @Test
    void get_nullKey_underlyingCacheThrows_NPE() throws Exception {
        // Mock cache that throws NPE when get(null) is invoked
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>> mockCache = (ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>>) mock(ConcurrentHashMapNullSafe.class);
        when(mockCache.get(null)).thenThrow(new NullPointerException("null keys not allowed"));
        setPrivateField(cache, "cache", mockCache);
        assertThrows(NullPointerException.class, () -> cache.get(null), "The NPE from the underlying map must propagate");
    }

    @Test
    void get_presentKeyLockThrows_IllegalStateException() throws Exception {
        // Prepare a normal node
        String key = "presentKeyLockThrows";
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>> mockCache = (ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<Integer, String>>) mock(ConcurrentHashMapNullSafe.class);
        LockingLRUCacheStrategy.Node<Integer, String> node = new LockingLRUCacheStrategy.Node<>();
        Field valueField = node.getClass().getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(node, "valueX");
        when(mockCache.get(key)).thenReturn(node);
        // Mock lock that throws when tryLock() is called
        Lock mockLock = mock(Lock.class);
        when(mockLock.tryLock()).thenThrow(new IllegalStateException("lock broken"));
        setPrivateField(cache, "cache", mockCache);
        setPrivateField(cache, "lock", mockLock);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cache.get(key), "Exception from tryLock must bubble up");
        assertEquals("lock broken", ex.getMessage());
    }
}
