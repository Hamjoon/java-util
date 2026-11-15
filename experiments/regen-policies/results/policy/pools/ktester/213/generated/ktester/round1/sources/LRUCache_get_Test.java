package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_get_Test {

    private LRUCache<Integer, String> lruCache;

    class CustomKey {

        final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    class MutableKey {

        int id;

        String name;

        MutableKey(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MutableKey))
                return false;
            MutableKey other = (MutableKey) o;
            return id == other.id && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    class BadHashKey {

        final boolean trigger;

        BadHashKey(boolean trigger) {
            this.trigger = trigger;
        }

        @Override
        public int hashCode() {
            if (trigger) {
                throw new RuntimeException("hashCode failure");
            }
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class CustomKey {

        final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    class MutableKey {

        int id;

        String name;

        MutableKey(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MutableKey))
                return false;
            MutableKey other = (MutableKey) o;
            return id == other.id && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    class BadHashKey {

        final boolean trigger;

        BadHashKey(boolean trigger) {
            this.trigger = trigger;
        }

        @Override
        public int hashCode() {
            if (trigger) {
                throw new RuntimeException("hashCode failure");
            }
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the simplest constructor (locking strategy) with a small capacity
        // so that eviction behaviour can be exercised deterministically.
        lruCache = new LRUCache<>(3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid cross‑test contamination.
        if (lruCache != null) {
            lruCache.clear();
        }
        lruCache = null;
    }

    /**
     * Helper that replaces the private final {@code strategy} field with a mock.
     */
    private void injectMockStrategy(Map<?, ?> mock) throws Exception {
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        // Remove final modifier (works on most JVMs)
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(lruCache, mock);
    }

    @Test
    void getReturnsValueWhenKeyPresent() {
        lruCache.put(1, "one");
        lruCache.put(2, "two");
        lruCache.put(3, "three");
        assertEquals("one", lruCache.get(1));
        assertEquals("two", lruCache.get(2));
        assertEquals("three", lruCache.get(3));
    }

    @Test
    void getReturnsNullWhenKeyAbsent() {
        lruCache.put(1, "one");
        assertNull(lruCache.get(2), "Getting a non‑existent key should return null");
    }

    @Test
    void getHandlesNullKeyAndValue() {
        // Null key
        lruCache.put(null, "nullKey");
        assertEquals("nullKey", lruCache.get(null));
        // Null value
        lruCache.put(10, null);
        assertNull(lruCache.get(10));
    }

    @Test
    void getDoesNotAffectCacheSize() {
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        int sizeBefore = lruCache.size();
        // Access both entries
        lruCache.get(1);
        lruCache.get(2);
        assertEquals(sizeBefore, lruCache.size(), "Calling get() should not change the cache size");
    }

    @Test
    void getReflectsEvictionPolicy() throws InterruptedException {
        // Fill cache to capacity
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        // Access key 1 to make it most‑recently used
        lruCache.get(1);
        // Adding a fourth entry should evict the least‑recently used (key 2)
        lruCache.put(4, "D");
        // For the locking strategy eviction is immediate.
        // For the threaded strategy we wait a short time to allow the background
        // cleanup thread to run (if it were used). The wait is harmless for locking.
        TimeUnit.MILLISECONDS.sleep(200);
        assertNull(lruCache.get(2), "Key 2 should have been evicted");
        assertEquals("A", lruCache.get(1), "Key 1 should still be present");
        assertEquals("D", lruCache.get(4), "Key 4 should be present");
    }

    @Test
    void getWorksWithThreadedStrategy() throws InterruptedException {
        // Use the constructor that selects the threaded strategy explicitly
        LRUCache<Integer, String> threadedCache = new LRUCache<>(3, LRUCache.StrategyType.THREADED);
        threadedCache.put(1, "X");
        threadedCache.put(2, "Y");
        threadedCache.put(3, "Z");
        // Access to make key 1 most recent
        threadedCache.get(1);
        // Insert a fourth entry – eviction may happen asynchronously
        threadedCache.put(4, "W");
        // Wait briefly for the background cleanup thread to run
        TimeUnit.MILLISECONDS.sleep(300);
        assertNull(threadedCache.get(2), "Key 2 should have been evicted by the threaded strategy");
        assertEquals("X", threadedCache.get(1));
        assertEquals("W", threadedCache.get(4));
    }

    @Test
    void getPropagatesRuntimeExceptionFromStrategy() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.get(any())).thenThrow(new RuntimeException("boom"));
        injectMockStrategy(mockStrategy);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> lruCache.get(42), "RuntimeException from the underlying map must be propagated");
        assertEquals("boom", ex.getMessage());
        verify(mockStrategy, times(1)).get(42);
    }

    @Test
    void getReturnsNullWhenStrategyMapsKeyToNull() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.get(eq(99))).thenReturn(null);
        injectMockStrategy(mockStrategy);
        assertNull(lruCache.get(99), "Cache should return null when the underlying map maps the key to null");
        verify(mockStrategy, times(1)).get(99);
    }

    @Test
    void getReturnsNullForTypeMismatchKey() {
        // Cache declared with String keys
        LRUCache<String, String> stringCache = new LRUCache<>(3);
        stringCache.put("existingKey", "value1");
        // Supplying an Integer key – map will not find a matching entry and returns null
        assertNull(stringCache.get(12345), "A key of a different type should result in null");
    }

    @Test
    void getFindsEntryWithCustomKeyEquality() {
        class CustomKey {

            final int id;

            CustomKey(int id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
            }

            @Override
            public int hashCode() {
                return Objects.hash(id);
            }
        }
        LRUCache<CustomKey, String> cache = new LRUCache<>(3);
        CustomKey storedKey = new CustomKey(1);
        cache.put(storedKey, "customValue");
        // A different instance that is equal according to equals()
        CustomKey lookupKey = new CustomKey(1);
        assertEquals("customValue", cache.get(lookupKey), "Cache should locate the entry using the overridden equality logic");
    }

    @Test
    void getReturnsNullWhenMutableKeyHasChangedAfterInsertion() {
        class MutableKey {

            int id;

            String name;

            MutableKey(int id, String name) {
                this.id = id;
                this.name = name;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof MutableKey))
                    return false;
                MutableKey other = (MutableKey) o;
                return id == other.id && Objects.equals(name, other.name);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, name);
            }
        }
        LRUCache<MutableKey, String> cache = new LRUCache<>(3);
        MutableKey key = new MutableKey(2, "original");
        cache.put(key, "mutableValue");
        // Mutate the key after it has been placed into the cache
        key.name = "changed";
        // The mutated key no longer matches the stored hash/equality, so lookup fails
        assertNull(cache.get(key), "After mutating the key, get should return null");
    }

    @Test
    void getPropagatesErrorFromStrategy() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.get(any())).thenThrow(new OutOfMemoryError("OOM"));
        injectMockStrategy(mockStrategy);
        OutOfMemoryError err = assertThrows(OutOfMemoryError.class, () -> lruCache.get(1), "Errors thrown by the underlying map must be propagated");
        assertEquals("OOM", err.getMessage());
    }

    @Test
    void getReturnsNullWhenCacheIsEmpty() {
        // No puts performed – cache is empty
        assertNull(lruCache.get("anyKey"), "Empty cache should return null for any key");
    }

    @Test
    void getPropagatesClassCastExceptionFromStrategy() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> mockStrategy = mock(Map.class);
        when(mockStrategy.get(any())).thenThrow(new ClassCastException("bad key type"));
        // We need a cache whose generic type is String to match the mock signature
        LRUCache<String, String> stringCache = new LRUCache<>(3);
        // inject the mock into this cache instance
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(stringCache, mockStrategy);
        ClassCastException ex = assertThrows(ClassCastException.class, () -> stringCache.get(12345), "ClassCastException from the underlying map must be propagated");
        assertEquals("bad key type", ex.getMessage());
    }

    @Test
    void getPropagatesNullPointerExceptionForNullKeyWhenStrategyDisallowsIt() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> mockStrategy = mock(Map.class);
        when(mockStrategy.get(isNull())).thenThrow(new NullPointerException("null keys not allowed"));
        LRUCache<String, String> stringCache = new LRUCache<>(3);
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(stringCache, mockStrategy);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> stringCache.get(null), "NullPointerException from the underlying map must be propagated");
        assertEquals("null keys not allowed", ex.getMessage());
    }

    @Test
    void getPropagatesRuntimeExceptionFromKeyHashCode() {
        class BadHashKey {

            final boolean trigger;

            BadHashKey(boolean trigger) {
                this.trigger = trigger;
            }

            @Override
            public int hashCode() {
                if (trigger) {
                    throw new RuntimeException("hashCode failure");
                }
                return 0;
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }
        }
        BadHashKey badKey = new BadHashKey(true);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> lruCache.get(badKey), "Exception from key.hashCode() must propagate");
        assertEquals("hashCode failure", ex.getMessage());
    }

    @Test
    void getThrowsNullPointerExceptionWhenStrategyFieldIsNull() throws Exception {
        // Use reflection to set the private final strategy field to null
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        // Remove final modifier
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(lruCache, null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> lruCache.get("anyKey"), "Calling get() when strategy is null should throw NPE");
        // The message is JVM‑dependent; we only assert the type.
        assertNotNull(npe);
    }
}
