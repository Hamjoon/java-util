package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.concurrent.locks.Lock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class LockingLRUCacheStrategy_putAll_Test {

    private LockingLRUCacheStrategy<String, Integer> cache;

    /**
     * Map whose {@code entrySet()} returns {@code null}.
     */
    private static class MapWithNullEntrySet<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return null;
        }

        // All other methods delegate to an empty HashMap (they are never used in the test).
        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }
    }

    /**
     * Map whose {@code entrySet()} iterator throws {@code ConcurrentModificationException}.
     */
    private static class MapThatThrowsCMEOnIterate<K, V> implements Map<K, V> {

        private final Map<K, V> delegate;

        MapThatThrowsCMEOnIterate(Map<K, V> source) {
            this.delegate = new HashMap<>(source);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    throw new ConcurrentModificationException("forced CME");
                }

                @Override
                public int size() {
                    return delegate.size();
                }
            };
        }

        // Delegate the remaining methods.
        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }
    }

    /**
     * Map whose {@code entrySet()} contains a {@code null} element.
     */
    private static class MapWithNullEntry<K, V> implements Map<K, V> {

        private final Map<K, V> delegate;

        MapWithNullEntry(Map<K, V> source) {
            this.delegate = new HashMap<>(source);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> set = new HashSet<>(delegate.entrySet());
            // inject a null entry
            set.add(null);
            return set;
        }

        // Delegate the remaining methods.
        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a capacity larger than the number of entries we will insert.
        cache = new LockingLRUCacheStrategy<>(10);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid side‑effects between tests.
        cache.clear();
    }

    /* -------------------------------------------------------------
     * Helper to inspect the private lock field.
     * ------------------------------------------------------------- */
    private static Lock getLock(LockingLRUCacheStrategy<?, ?> c) throws Exception {
        Field f = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        f.setAccessible(true);
        return (Lock) f.get(c);
    }

    @Test
    void putAllAddsAllEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        cache.putAll(source);
        assertEquals(2, cache.size(), "Cache should contain two entries");
        assertEquals(1, cache.get("a"));
        assertEquals(2, cache.get("b"));
    }

    @Test
    void putAllOverridesExistingEntries() {
        // Existing entry that will be overwritten.
        cache.put("a", 10);
        assertEquals(10, cache.get("a"));
        Map<String, Integer> source = Map.of(// should replace the existing value
        "a", // should replace the existing value
        1, // new entry
        "c", // new entry
        3);
        cache.putAll(source);
        assertEquals(2, cache.size(), "Cache should contain two distinct keys after putAll");
        assertEquals(1, cache.get("a"), "Existing key should be overwritten");
        assertEquals(3, cache.get("c"));
    }

    @Test
    void putAllHandlesNullKeyAndNullValue() {
        Map<String, Integer> source = new HashMap<>();
        source.put(null, null);
        cache.putAll(source);
        assertTrue(cache.containsKey(null), "Cache should contain the null key");
        assertTrue(cache.containsValue(null), "Cache should contain the null value");
        assertNull(cache.get(null), "Value retrieved for null key should be null");
    }

    @Test
    void putAllWithEmptyMapLeavesCacheUnchanged() {
        cache.put("x", 5);
        assertEquals(1, cache.size());
        cache.putAll(Collections.emptyMap());
        assertEquals(1, cache.size(), "Cache size should remain unchanged after putting an empty map");
        assertEquals(5, cache.get("x"));
        assertFalse(getLock(cache).isLocked(), "Lock must be unlocked after putAll");
    }

    @Test
    void putAllNullArgumentThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> cache.putAll(null), "Calling putAll with a null argument should throw NPE");
    }

    @Test
    void putAllEmptyMapDoesNotLockBeyondMethod() throws Exception {
        // The cache is empty initially.
        assertEquals(0, cache.size());
        cache.putAll(Collections.emptyMap());
        // After execution the lock must be released.
        assertFalse(getLock(cache).isLocked(), "Lock must be unlocked after putAll with empty map");
    }

    @Test
    void putAllNormalInsertionMultipleEntries() throws Exception {
        Map<String, Integer> source = Map.of("a", 1, "b", 2, "c", 3);
        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals(1, cache.get("a"));
        assertEquals(2, cache.get("b"));
        assertEquals(3, cache.get("c"));
        assertFalse(getLock(cache).isLocked(), "Lock must be unlocked after successful putAll");
    }

    @Test
    void putAllHandlesMixedNullKeyAndValue() throws Exception {
        Map<String, Integer> source = new HashMap<>();
        // null key, non‑null value
        source.put(null, 42);
        // non‑null key, null value
        source.put("nullVal", null);
        cache.putAll(source);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertEquals(42, cache.get(null));
        assertNull(cache.get("nullVal"));
        assertEquals(2, cache.size());
        assertFalse(getLock(cache).isLocked(), "Lock must be unlocked after handling nulls");
    }

    @Test
    void putAllExceptionSafetyLockReleased() throws Exception {
        // Spy the cache so we can force an exception on the second entry.
        LockingLRUCacheStrategy<String, Integer> spyCache = Mockito.spy(new LockingLRUCacheStrategy<>(10));
        // First entry works fine, second entry throws.
        Mockito.doCallRealMethod().when(spyCache).put(eq("first"), any());
        Mockito.doThrow(new RuntimeException("forced failure")).when(spyCache).put(eq("second"), any());
        Map<String, Integer> source = Map.of("first", 1, "second", 2);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> spyCache.putAll(source), "The forced RuntimeException should propagate");
        assertEquals("forced failure", thrown.getMessage());
        // The first entry should have been stored before the exception.
        assertEquals(1, spyCache.get("first"));
        assertNull(spyCache.get("second"));
        // Verify lock is released even after the exception.
        assertFalse(getLock(spyCache).isLocked(), "Lock must be unlocked after exception in putAll");
    }

    @Test
    void putAllEntrySetReturnsNullLeadsToNPEAndUnlocks() throws Exception {
        Map<String, Integer> badMap = new MapWithNullEntrySet<>();
        NullPointerException npe = assertThrows(NullPointerException.class, () -> cache.putAll(badMap), "entrySet() returning null should cause NPE");
        // Ensure the lock is not left locked.
        assertFalse(getLock(cache).isLocked(), "Lock must be unlocked after NPE from entrySet");
    }

    @Test
    void putAllIteratorThrowsCMEPropagatesAndUnlocks() throws Exception {
        Map<String, Integer> badMap = new MapThatThrowsCMEOnIterate<>(Map.of("x", 1, "y", 2));
        ConcurrentModificationException cme = assertThrows(ConcurrentModificationException.class, () -> cache.putAll(badMap), "Iterator throwing CME should propagate");
        assertFalse(getLock(cache).isLocked(), "Lock must be unlocked after CME");
    }

    @Test
    void putAllTypeMismatchCausesClassCastException() throws Exception {
        // Cache expects Integer keys/values.
        LockingLRUCacheStrategy<Integer, Integer> intCache = new LockingLRUCacheStrategy<>(10);
        Map<String, String> wrongMap = Map.of("stringKey", "stringValue");
        assertThrows(ClassCastException.class, () -> intCache.putAll(wrongMap), "Mismatched generic types should cause ClassCastException");
        assertFalse(getLock(intCache).isLocked(), "Lock must be unlocked after ClassCastException");
    }

    @Test
    void putAllNullEntryInsideEntrySetThrowsNPEAndUnlocks() throws Exception {
        Map<String, Integer> badMap = new MapWithNullEntry<>(Map.of("a", 1));
        NullPointerException npe = assertThrows(NullPointerException.class, () -> cache.putAll(badMap), "Null entry inside entrySet should cause NPE");
        assertFalse(getLock(cache).isLocked(), "Lock must be unlocked after NPE from null entry");
    }
}
