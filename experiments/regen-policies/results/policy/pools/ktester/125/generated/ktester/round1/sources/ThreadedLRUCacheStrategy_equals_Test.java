package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_equals_Test {

    /**
     * Cache used as the primary subject of the test.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cacheUnderTest;

    /**
     * Second cache used for cross‑comparison.
     */
    private ThreadedLRUCacheStrategy<String, Integer> anotherCache;

    /**
     * Simple {@link java.util.Map} implementation for cross‑type equality checks.
     */
    private Map<String, Integer> plainMap;

    /**
     * Capacity and cleanup delay used for all cache instances in the tests.
     */
    private static final int CAPACITY = 10;

    private static final int CLEANUP_DELAY_MILLIS = 50;

    private static class ReferenceCase {

        final String name;

        final java.util.function.Function<ThreadedLRUCacheStrategy<String, Integer>, Object> otherProvider;

        final boolean expected;

        final String description;

        ReferenceCase(String name, java.util.function.Function<ThreadedLRUCacheStrategy<String, Integer>, Object> otherProvider, boolean expected, String description) {
            this.name = name;
            this.otherProvider = otherProvider;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Map that throws IllegalStateException when entrySet() is invoked.
     */
    private static class FaultyEntrySetMap<K, V> extends HashMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new IllegalStateException("entrySet failure");
        }
    }

    /**
     * Subclass of ThreadedLRUCacheStrategy that throws RuntimeException from entrySet().
     */
    private static class FaultyThreadedCache<K, V> extends ThreadedLRUCacheStrategy<K, V> {

        FaultyThreadedCache(int capacity, int cleanupDelay) {
            super(capacity, cleanupDelay);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            throw new RuntimeException("entrySet failure");
        }
    }

    /**
     * Set wrapper whose equals() method always throws UnsupportedOperationException.
     */
    private static class FaultyEqualsSet<E> extends AbstractSet<E> {

        private final Set<E> delegate;

        FaultyEqualsSet(Set<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean equals(Object o) {
            throw new UnsupportedOperationException("equals not supported");
        }
    }

    /**
     * Map that returns a FaultyEqualsSet from entrySet().
     */
    private static class FaultyEqualsMap<K, V> extends HashMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new FaultyEqualsSet<>(super.entrySet());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        cacheUnderTest = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
        anotherCache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
        plainMap = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the caches to avoid interference between tests.
        cacheUnderTest.clear();
        anotherCache.clear();
        plainMap.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by ThreadedLRUCacheStrategy.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* --------------------------------------------------------------
     *  Reference checks (same instance, null, non‑map)
     * -------------------------------------------------------------- */
    private static Stream<ReferenceCase> referenceCases() {
        return Stream.of(new ReferenceCase("sameInstance", cache -> cache, true, "The argument is the exact same object as the tested instance (this == o)."), new ReferenceCase("null", cache -> null, false, "The argument is null; instanceof Map fails, so false is returned."), new ReferenceCase("nonMap", cache -> "someString", false, "The argument is a non‑Map object; instanceof Map fails, so false is returned."));
    }

    @Test
    void equalsSameInstanceReturnsTrue() {
        assertTrue(cacheUnderTest.equals(cacheUnderTest), "A cache should be equal to itself.");
    }

    @Test
    void equalsAnotherMapWithSameEntriesReturnsTrue() {
        cacheUnderTest.put("one", 1);
        cacheUnderTest.put("two", 2);
        anotherCache.put("one", 1);
        anotherCache.put("two", 2);
        assertTrue(cacheUnderTest.equals(anotherCache), "Two caches with identical entries should be equal.");
        // Also verify that equality works with a plain HashMap having the same entries.
        plainMap.put("one", 1);
        plainMap.put("two", 2);
        assertTrue(cacheUnderTest.equals(plainMap), "Cache should be equal to any Map with the same entry set.");
    }

    @Test
    void equalsAnotherMapWithDifferentEntriesReturnsFalse() {
        cacheUnderTest.put("one", 1);
        cacheUnderTest.put("two", 2);
        anotherCache.put("one", 1);
        // different value
        anotherCache.put("two", 3);
        assertFalse(cacheUnderTest.equals(anotherCache), "Caches with differing entries must not be equal.");
        plainMap.put("one", 1);
        plainMap.put("two", 2);
        // extra entry
        plainMap.put("three", 3);
        assertFalse(cacheUnderTest.equals(plainMap), "Cache should not be equal to a Map with a different entry set.");
    }

    @Test
    void equalsNonMapObjectReturnsFalse() {
        Object notAMap = new Object();
        assertFalse(cacheUnderTest.equals(notAMap), "Equals should return false when compared with a non‑Map object.");
    }

    @Test
    void equalsNullReturnsFalse() {
        assertFalse(cacheUnderTest.equals(null), "Equals should return false when compared with null.");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("referenceCases")
    void equalsReferenceChecks(ReferenceCase rc) {
        Object other = rc.otherProvider.apply(cacheUnderTest);
        boolean actual = cacheUnderTest.equals(other);
        assertEquals(rc.expected, actual, rc.description);
    }

    /* --------------------------------------------------------------
     *  Equality with other maps
     * -------------------------------------------------------------- */
    @Test
    void equalsEmptyMapsReturnsTrue() {
        // Both caches are empty by default.
        assertTrue(cacheUnderTest.equals(anotherCache), "Two empty caches should be equal.");
        assertTrue(cacheUnderTest.equals(Collections.emptyMap()), "Cache should be equal to any empty Map.");
    }

    /* --------------------------------------------------------------
     *  Inequality with other maps
     * -------------------------------------------------------------- */
    @Test
    void equalsMapWithDifferentValueReturnsFalse() {
        cacheUnderTest.put("key", 1);
        // same key, different value
        anotherCache.put("key", 2);
        assertFalse(cacheUnderTest.equals(anotherCache), "Caches with the same key but different values must not be equal.");
    }

    @Test
    void equalsMapWithAdditionalEntryReturnsFalse() {
        cacheUnderTest.put("a", 1);
        cacheUnderTest.put("b", 2);
        plainMap.put("a", 1);
        plainMap.put("b", 2);
        // extra entry
        plainMap.put("c", 3);
        assertFalse(cacheUnderTest.equals(plainMap), "Cache should not be equal to a Map that contains an additional entry.");
    }

    @Test
    void equalsMapWithMissingEntryReturnsFalse() {
        cacheUnderTest.put("x", 10);
        cacheUnderTest.put("y", 20);
        // missing "y"
        plainMap.put("x", 10);
        assertFalse(cacheUnderTest.equals(plainMap), "Cache should not be equal to a Map that is missing an entry present in the cache.");
    }

    /* --------------------------------------------------------------
     *  Exception propagation – other.map.entrySet()
     * -------------------------------------------------------------- */
    @Test
    void equalsOtherMapWithFaultyEntrySetPropagatesException() {
        Map<String, Integer> faultyMap = new FaultyEntrySetMap<>();
        faultyMap.put("a", 1);
        // The call to other.entrySet() should throw IllegalStateException.
        assertThrows(IllegalStateException.class, () -> cacheUnderTest.equals(faultyMap), "equals should propagate IllegalStateException thrown by other.entrySet().");
    }

    /* --------------------------------------------------------------
     *  Exception propagation – this.entrySet()
     * -------------------------------------------------------------- */
    @Test
    void equalsWhenThisEntrySetThrowsPropagatesException() {
        // Subclass that overrides entrySet() to throw.
        ThreadedLRUCacheStrategy<String, Integer> faultyCache = new FaultyThreadedCache<>(CAPACITY, CLEANUP_DELAY_MILLIS);
        // Even comparing with an empty map should trigger the overridden entrySet().
        assertThrows(RuntimeException.class, () -> faultyCache.equals(Collections.emptyMap()), "equals should propagate RuntimeException thrown by this.entrySet().");
    }

    /* --------------------------------------------------------------
     *  Exception propagation – other.entrySet().equals()
     * -------------------------------------------------------------- */
    @Test
    void equalsOtherMapWithFaultySetEqualsPropagatesException() {
        // Map whose entrySet returns a Set whose equals throws.
        Map<String, Integer> faultyEqualsMap = new FaultyEqualsMap<>();
        faultyEqualsMap.put("k", 1);
        // make entry sets logically equal
        cacheUnderTest.put("k", 1);
        assertThrows(UnsupportedOperationException.class, () -> cacheUnderTest.equals(faultyEqualsMap), "equals should propagate UnsupportedOperationException thrown by other.entrySet().equals().");
    }
}
