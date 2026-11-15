package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InOrder;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;

class LockingLRUCacheStrategy_putAll_Test {

    private static final int CAPACITY = 2;

    private LockingLRUCacheStrategy<String, Integer> cache;

    class ThrowingCache extends LockingLRUCacheStrategy<String, Integer> {

        ThrowingCache(int capacity) {
            super(capacity);
        }

        @Override
        public Integer put(String key, Integer value) {
            throw new RuntimeException("forced");
        }
    }

    /**
     * Map whose {@code entrySet()} returns {@code null}.
     */
    private static class MapWithNullEntrySet<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return null;
        }

        // All other methods are unsupported for brevity – they will never be called.
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V get(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<K> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<V> values() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Map whose {@code entrySet()} iterator throws a {@code RuntimeException} on first {@code next()} call.
     */
    private static class MapWithFaultyIterator<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {

                        private boolean first = true;

                        @Override
                        public boolean hasNext() {
                            return true;
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (first) {
                                first = false;
                                throw new RuntimeException("Iterator failure");
                            }
                            return null;
                        }
                    };
                }

                @Override
                public int size() {
                    return 1;
                }
            };
        }

        // Remaining Map methods are not needed for this test.
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V get(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<K> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<V> values() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Map with a single entry whose {@code getKey()} throws an exception.
     */
    private static class MapWithBadKeyEntry<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Set.of(new Entry<>() {

                @Override
                public K getKey() {
                    throw new IllegalStateException("bad key");
                }

                @Override
                public V getValue() {
                    return null;
                }

                @Override
                public V setValue(V value) {
                    throw new UnsupportedOperationException();
                }
            });
        }

        // Stub out other methods.
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V get(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<K> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<V> values() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Map with a single entry whose {@code getValue()} throws an exception.
     */
    private static class MapWithBadValueEntry<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Set.of(new Entry<>() {

                @Override
                public K getKey() {
                    return null;
                }

                @Override
                public V getValue() {
                    throw new IllegalArgumentException("bad value");
                }

                @Override
                public V setValue(V value) {
                    throw new UnsupportedOperationException();
                }
            });
        }

        // Stub out other methods.
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V get(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<K> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<V> values() {
            throw new UnsupportedOperationException();
        }
    }

    class ThrowingCache extends LockingLRUCacheStrategy<String, Integer> {

        ThrowingCache(int capacity) {
            super(capacity);
        }

        @Override
        public Integer put(String key, Integer value) {
            throw new RuntimeException("forced");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test so tests are independent.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to aid garbage collection; not strictly required.
        cache.clear();
    }

    /**
     * Uses reflection to replace the private final {@code lock} field with a mock.
     */
    private static void setInternalLock(LockingLRUCacheStrategy<?, ?> target, Lock mockLock) throws Exception {
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        // Remove final modifier via reflection (works on most JVMs for testing)
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(lockField, lockField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        lockField.set(target, mockLock);
    }

    @Test
    void putAllAddsAllEntriesWhenUnderCapacity() {
        // given
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        // when
        cache.putAll(source);
        // then
        assertEquals(2, cache.size(), "Cache size should match number of inserted entries");
        assertEquals(1, cache.get("one"));
        assertEquals(2, cache.get("two"));
    }

    @Test
    void putAllEvictsLeastRecentlyUsedWhenCapacityExceeded() {
        // given
        // Fill the cache to its capacity first.
        cache.put("old", 0);
        cache.put("middle", 1);
        // source map contains three entries; after putAll the cache capacity is 2,
        // so the least‑recently used entry ("old") should be evicted.
        Map<String, Integer> source = new HashMap<>();
        source.put("new1", 10);
        source.put("new2", 20);
        source.put("new3", 30);
        // when
        cache.putAll(source);
        // then
        assertEquals(CAPACITY, cache.size(), "Cache size must never exceed its capacity");
        // The entry "old" should have been evicted.
        assertNull(cache.get("old"), "Least recently used entry should be evicted");
        // The cache should contain the two most recently inserted entries.
        // In the current implementation the order of eviction follows insertion order,
        // therefore the last two entries from the source map are expected to remain.
        assertEquals(20, cache.get("new2"));
        assertEquals(30, cache.get("new3"));
        // The first entry from the source map ("new1") should have been evicted.
        assertNull(cache.get("new1"));
    }

    @Test
    void putAllHandlesEmptyMapGracefully() {
        // given
        Map<String, Integer> empty = new HashMap<>();
        // when
        cache.putAll(empty);
        // then
        assertEquals(0, cache.size(), "Cache should remain empty after putting an empty map");
    }

    @Test
    void putAllThrowsNullPointerExceptionWhenArgumentIsNull() {
        // given
        Map<String, Integer> nullMap = null;
        // when / then
        assertThrows(NullPointerException.class, () -> cache.putAll(nullMap));
    }

    /**
     * Verify that the internal lock is acquired before any put operation and released afterwards.
     */
    @Test
    void putAll_acquiresAndReleasesLock_normalFlow() throws Exception {
        // Replace the real lock with a Mockito mock.
        Lock lockMock = mock(Lock.class);
        setInternalLock(cache, lockMock);
        Map<String, Integer> source = Map.of("c", 3);
        // when
        cache.putAll(source);
        // then – lock.lock() must be called before any put and unlock() after.
        InOrder inOrder = inOrder(lockMock);
        inOrder.verify(lockMock).lock();
        // The put method is called for each entry; we cannot verify its internals here,
        // but we can be sure unlock() is called exactly once after lock().
        inOrder.verify(lockMock).unlock();
        verifyNoMoreInteractions(lockMock);
    }

    /**
     * Simulate a put() that throws an exception and ensure the lock is still released.
     */
    @Test
    void putAll_unlocksEvenWhenPutThrows() throws Exception {
        // Subclass that throws from put()
        class ThrowingCache extends LockingLRUCacheStrategy<String, Integer> {

            ThrowingCache(int capacity) {
                super(capacity);
            }

            @Override
            public Integer put(String key, Integer value) {
                throw new RuntimeException("forced");
            }
        }
        ThrowingCache throwingCache = new ThrowingCache(CAPACITY);
        Lock lockMock = mock(Lock.class);
        setInternalLock(throwingCache, lockMock);
        Map<String, Integer> source = Map.of("d", 4);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> throwingCache.putAll(source));
        assertEquals("forced", ex.getMessage());
        // verify lock was released
        InOrder inOrder = inOrder(lockMock);
        inOrder.verify(lockMock).lock();
        inOrder.verify(lockMock).unlock();
        verifyNoMoreInteractions(lockMock);
    }

    @Test
    void putAll_normalBehavior_multipleEntries() {
        Map<String, Integer> source = Map.of("a", 1, "b", 2, "c", 3);
        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals(1, cache.get("a"));
        assertEquals(2, cache.get("b"));
        assertEquals(3, cache.get("c"));
    }

    @Test
    void putAll_emptyMapLeavesCacheUnchanged() {
        cache.put("x", 99);
        int originalSize = cache.size();
        Map<String, Integer> empty = Map.of();
        cache.putAll(empty);
        assertEquals(originalSize, cache.size());
        assertEquals(99, cache.get("x"));
    }

    @Test
    void putAll_duplicateKeys_overwritesExistingValue() {
        cache.put("x", 1);
        cache.put("y", 2);
        Map<String, Integer> source = Map.of("x", 10, "z", 3);
        cache.putAll(source);
        assertEquals(3, cache.size());
        // overwritten
        assertEquals(10, cache.get("x"));
        assertEquals(2, cache.get("y"));
        assertEquals(3, cache.get("z"));
    }

    @Test
    void putAll_allowsNullKeyAndNullValue() {
        Map<String, Integer> source = new HashMap<>();
        source.put(null, 0);
        source.put("nullValue", null);
        cache.putAll(source);
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertEquals(0, cache.get(null));
        assertNull(cache.get("nullValue"));
    }

    @Test
    void putAll_entrySetReturnsNull_throwsNullPointerException() {
        Map<String, Integer> badMap = new MapWithNullEntrySet<>();
        assertThrows(NullPointerException.class, () -> cache.putAll(badMap));
    }

    @Test
    void putAll_iteratorThrowsRuntimeException_propagatesAndUnlocks() throws Exception {
        Map<String, Integer> faultyMap = new MapWithFaultyIterator<>();
        // Spy on the lock to verify unlock()
        Lock lockMock = mock(Lock.class);
        setInternalLock(cache, lockMock);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.putAll(faultyMap));
        assertEquals("Iterator failure", ex.getMessage());
        // Ensure unlock happened even though iteration failed
        InOrder inOrder = inOrder(lockMock);
        inOrder.verify(lockMock).lock();
        inOrder.verify(lockMock).unlock();
    }

    @Test
    void putAll_entryKeyThrowsException_propagatesAndUnlocks() throws Exception {
        Map<String, Integer> badKeyMap = new MapWithBadKeyEntry<>();
        Lock lockMock = mock(Lock.class);
        setInternalLock(cache, lockMock);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cache.putAll(badKeyMap));
        assertEquals("bad key", ex.getMessage());
        InOrder inOrder = inOrder(lockMock);
        inOrder.verify(lockMock).lock();
        inOrder.verify(lockMock).unlock();
    }

    @Test
    void putAll_entryValueThrowsException_propagatesAndUnlocks() throws Exception {
        Map<String, Integer> badValueMap = new MapWithBadValueEntry<>();
        Lock lockMock = mock(Lock.class);
        setInternalLock(cache, lockMock);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cache.putAll(badValueMap));
        assertEquals("bad value", ex.getMessage());
        InOrder inOrder = inOrder(lockMock);
        inOrder.verify(lockMock).lock();
        inOrder.verify(lockMock).unlock();
    }
}
