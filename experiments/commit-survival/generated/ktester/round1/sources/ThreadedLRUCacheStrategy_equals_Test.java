package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_equals_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cacheUnderTest;

    private ThreadedLRUCacheStrategy<String, Integer> anotherCache;

    class BadKey {

        @Override
        public boolean equals(Object o) {
            throw new IllegalStateException("Bad key equality");
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    class BadValue {

        @Override
        public boolean equals(Object o) {
            throw new IllegalStateException("Bad value equality");
        }

        @Override
        public int hashCode() {
            return 2;
        }
    }

    class FaultyCache extends ThreadedLRUCacheStrategy<String, Integer> {

        FaultyCache(int capacity, long cleanupDelay) {
            super(capacity, cleanupDelay);
        }

        @Override
        public Set<Entry<String, Integer>> entrySet() {
            throw new RuntimeException("this entrySet failure");
        }
    }

    class BadKey {

        @Override
        public boolean equals(Object o) {
            throw new IllegalStateException("Bad key equality");
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    class BadValue {

        @Override
        public boolean equals(Object o) {
            throw new IllegalStateException("Bad value equality");
        }

        @Override
        public int hashCode() {
            return 2;
        }
    }

    class FaultyCache extends ThreadedLRUCacheStrategy<String, Integer> {

        FaultyCache(int capacity, long cleanupDelay) {
            super(capacity, cleanupDelay);
        }

        @Override
        public Set<Entry<String, Integer>> entrySet() {
            throw new RuntimeException("this entrySet failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a small capacity and a short cleanup delay – the values are irrelevant for the equals tests.
        cacheUnderTest = new ThreadedLRUCacheStrategy<>(5, 50);
        anotherCache = new ThreadedLRUCacheStrategy<>(5, 50);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each cache is cleared so that later tests start with a clean state.
        cacheUnderTest.clear();
        anotherCache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        assertTrue(cacheUnderTest.equals(cacheUnderTest), "An object should be equal to itself");
    }

    @Test
    void equals_equalMap_returnsTrue() {
        // Populate both caches with identical entries.
        cacheUnderTest.put("one", 1);
        cacheUnderTest.put("two", 2);
        anotherCache.put("one", 1);
        anotherCache.put("two", 2);
        // anotherCache implements Map, so equals should compare entry sets.
        assertTrue(cacheUnderTest.equals(anotherCache), "Caches with identical entry sets should be equal");
    }

    @Test
    void equals_differentMap_returnsFalse() {
        cacheUnderTest.put("one", 1);
        cacheUnderTest.put("two", 2);
        anotherCache.put("one", 1);
        // different value
        anotherCache.put("two", 3);
        assertFalse(cacheUnderTest.equals(anotherCache), "Caches with different entry sets should not be equal");
    }

    @Test
    void equals_nonMapObject_returnsFalse() {
        Object notAMap = new Object();
        assertFalse(cacheUnderTest.equals(notAMap), "equals should return false when the argument is not a Map");
    }

    @Test
    void equals_null_returnsFalse() {
        assertFalse(cacheUnderTest.equals(null), "equals should return false when compared with null");
    }

    @Test
    void equals_mapWithSameEntriesButDifferentImplementation_returnsTrue() {
        // Use a plain HashMap as the comparator.
        Map<String, Integer> plainMap = new HashMap<>();
        plainMap.put("alpha", 10);
        plainMap.put("beta", 20);
        cacheUnderTest.putAll(plainMap);
        // The entrySet of cacheUnderTest should equal the entrySet of plainMap.
        assertTrue(cacheUnderTest.equals(plainMap), "Cache should be equal to any Map with the same entries, regardless of implementation");
    }

    @Test
    void equals_emptyCacheEqualsEmptyMap_returnsTrue() {
        Map<String, Integer> emptyMap = new HashMap<>();
        assertTrue(cacheUnderTest.equals(emptyMap), "Both cache and map are empty – entry sets should be equal");
    }

    @Test
    void equals_emptyCacheNotEqualToNonEmptyMap_returnsFalse() {
        Map<String, Integer> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("x", 42);
        assertFalse(cacheUnderTest.equals(nonEmptyMap), "Empty cache must not equal a non‑empty map");
    }

    @Test
    void equals_cacheWithNullKeyAndValueEqualsMapWithSameNullEntry_returnsTrue() {
        // Cache supports null keys/values.
        cacheUnderTest.put(null, null);
        Map<String, Integer> mapWithNullEntry = new HashMap<>();
        // raw types allow null key/value
        mapWithNullEntry.put(null, null);
        assertTrue(cacheUnderTest.equals(mapWithNullEntry), "Cache containing (null, null) should equal a map with the same entry");
    }

    @Test
    void equals_cacheWithNullKeyAndValueNotEqualToMapWithoutNulls_returnsFalse() {
        cacheUnderTest.put(null, null);
        Map<String, Integer> mapWithoutNulls = new HashMap<>();
        mapWithoutNulls.put("a", 1);
        assertFalse(cacheUnderTest.equals(mapWithoutNulls), "Cache with a null entry must not equal a map lacking that entry");
    }

    @Test
    void equals_cacheEqualsTreeMapWithSameEntries_returnsTrue() {
        TreeMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("c", 3);
        treeMap.put("d", 4);
        cacheUnderTest.putAll(treeMap);
        assertTrue(cacheUnderTest.equals(treeMap), "Cache should be equal to a TreeMap with identical entries (order independent)");
    }

    @Test
    void equals_cacheEqualsUnmodifiableMapWithSameEntries_returnsTrue() {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("e", 5);
        mutable.put("f", 6);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(mutable);
        cacheUnderTest.putAll(unmodifiable);
        assertTrue(cacheUnderTest.equals(unmodifiable), "Cache should be equal to an unmodifiable map with the same entries");
    }

    @Test
    void equals_otherMapKeyEqualsThrowsException_propagatesException() {
        // Map with a key whose equals() throws IllegalStateException.
        class BadKey {

            @Override
            public boolean equals(Object o) {
                throw new IllegalStateException("Bad key equality");
            }

            @Override
            public int hashCode() {
                return 1;
            }
        }
        Map<BadKey, Integer> badKeyMap = new HashMap<>();
        badKeyMap.put(new BadKey(), 1);
        cacheUnderTest.put("normal", 1);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cacheUnderTest.equals(badKeyMap), "Exception from key.equals() should propagate");
        assertEquals("Bad key equality", ex.getMessage());
    }

    @Test
    void equals_otherMapValueEqualsThrowsException_propagatesException() {
        // Map with a value whose equals() throws IllegalStateException.
        class BadValue {

            @Override
            public boolean equals(Object o) {
                throw new IllegalStateException("Bad value equality");
            }

            @Override
            public int hashCode() {
                return 2;
            }
        }
        Map<String, BadValue> badValueMap = new HashMap<>();
        badValueMap.put("k", new BadValue());
        // different value type, but entrySet comparison will invoke value.equals
        cacheUnderTest.put("k", 1);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cacheUnderTest.equals(badValueMap), "Exception from value.equals() should propagate");
        assertEquals("Bad value equality", ex.getMessage());
    }

    @Test
    void equals_otherMapEntrySetThrowsRuntimeException_propagatesException() {
        Map<String, Integer> faultyMap = new HashMap<String, Integer>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                throw new RuntimeException("entrySet failure");
            }
        };
        cacheUnderTest.put("a", 1);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cacheUnderTest.equals(faultyMap), "RuntimeException from other.entrySet() should propagate");
        assertEquals("entrySet failure", ex.getMessage());
    }

    @Test
    void equals_thisCacheEntrySetThrowsRuntimeException_propagatesException() {
        // Subclass that overrides entrySet() to throw.
        class FaultyCache extends ThreadedLRUCacheStrategy<String, Integer> {

            FaultyCache(int capacity, long cleanupDelay) {
                super(capacity, cleanupDelay);
            }

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                throw new RuntimeException("this entrySet failure");
            }
        }
        FaultyCache faultyCache = new FaultyCache(5, 50);
        faultyCache.put("x", 10);
        Map<String, Integer> regularMap = new HashMap<>();
        regularMap.put("x", 10);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> faultyCache.equals(regularMap), "RuntimeException from this.entrySet() should propagate");
        assertEquals("this entrySet failure", ex.getMessage());
    }

    @Test
    void equals_otherMapEntrySetReturnsNull_throwsNullPointerException() {
        Map<String, Integer> nullEntrySetMap = new HashMap<String, Integer>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                // deliberately break contract
                return null;
            }
        };
        cacheUnderTest.put("z", 99);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> cacheUnderTest.equals(nullEntrySetMap), "Calling equals on a map whose entrySet() returns null should NPE");
        // Message may be null; we only assert the type.
    }
}
