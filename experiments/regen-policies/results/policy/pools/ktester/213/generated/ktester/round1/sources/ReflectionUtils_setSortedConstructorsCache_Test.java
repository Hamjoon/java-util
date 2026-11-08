package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setSortedConstructorsCache_Test {

    /**
     * Holds a reference to the default (empty) cache used for cleanup.
     */
    private static final Map<Object, Constructor<?>[]> EMPTY_CACHE = new HashMap<>();

    /**
     * A simple class with two distinct constructors used for the tests.
     */
    private static class Sample {

        public Sample() {
        }

        public Sample(String s) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean cache before each test runs.
        ReflectionUtils.setSortedConstructorsCache(new HashMap<>());
        // start each test with a fresh (empty) cache
        ReflectionUtils.setSortedConstructorsCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the cache to an empty map so that other tests are not polluted.
        ReflectionUtils.setSortedConstructorsCache(EMPTY_CACHE);
        // restore the original empty cache so other test classes are not polluted
        ReflectionUtils.setSortedConstructorsCache(EMPTY_CACHE);
    }

    /**
     * Detects a map that was created by {@link Collections#synchronizedMap(Map)}.
     */
    private static boolean isSynchronizedMap(Object map) {
        return map != null && map.getClass().getName().contains("SynchronizedMap");
    }

    /**
     * Verifies that {@code setSortedConstructorsCache} replaces the internal cache
     * with the supplied map. The test populates a cache entry that returns an empty
     * constructor array for {@link Sample}. After the replacement, {@code getAllConstructors}
     * must return that empty array rather than the real constructors of {@link Sample}.
     */
    @Test
    void testSetSortedConstructorsCacheReplacesCache() {
        // ---- GIVEN ---------------------------------------------------------
        // Create a custom cache that maps Sample.class to an empty constructor array.
        Map<Object, Constructor<?>[]> customCache = new HashMap<>();
        @SuppressWarnings("unchecked")
        Constructor<?>[] emptyArray = new Constructor[0];
        customCache.put(Sample.class, emptyArray);
        // ---- WHEN ----------------------------------------------------------
        ReflectionUtils.setSortedConstructorsCache(customCache);
        Constructor<?>[] result = ReflectionUtils.getAllConstructors(Sample.class);
        // ---- THEN ----------------------------------------------------------
        assertNotNull(result, "Resulting constructor array must not be null");
        assertSame(emptyArray, result, "The returned constructors should be the exact array stored in the custom cache");
        assertEquals(0, result.length, "The custom cache supplied an empty array, so the length must be zero");
    }

    /**
     * Ensures that calling {@code setSortedConstructorsCache} with an empty map
     * does not throw any exception and that subsequent calls to {@code getAllConstructors}
     * still return the actual constructors of the target class.
     */
    @Test
    void testSetSortedConstructorsCacheAcceptsEmptyMap() {
        // ---- GIVEN ---------------------------------------------------------
        Map<Object, Constructor<?>[]> emptyMap = new HashMap<>();
        // ---- WHEN ----------------------------------------------------------
        assertDoesNotThrow(() -> ReflectionUtils.setSortedConstructorsCache(emptyMap), "Setting an empty cache should not throw an exception");
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(Sample.class);
        // ---- THEN ----------------------------------------------------------
        assertNotNull(constructors, "Constructors array must not be null");
        assertTrue(constructors.length >= 1, "With an empty cache the method should fall back to the real constructors");
    }

    /**
     * Helper that extracts the internal map held by {@code SORTED_CONSTRUCTORS_CACHE}
     * via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Constructor<?>[]> getInternalCache() throws Exception {
        var field = ReflectionUtils.class.getDeclaredField("SORTED_CONSTRUCTORS_CACHE");
        field.setAccessible(true);
        AtomicReference<Map<? super ReflectionUtils.SortedConstructorsCacheKey, Constructor<?>[]>> ref = (AtomicReference<Map<? super ReflectionUtils.SortedConstructorsCacheKey, Constructor<?>[]>>) field.get(null);
        return (Map<Object, Constructor<?>[]>) ref.get();
    }

    @Test
    void plainHashMapIsWrappedIntoSynchronizedMap() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>[]> supplied = new HashMap<>();
        // WHEN
        ReflectionUtils.setSortedConstructorsCache(supplied);
        Map<Object, Constructor<?>[]> internal = getInternalCache();
        // THEN
        assertNotSame(supplied, internal, "The stored map must be a different instance (wrapped for thread‑safety)");
        assertTrue(isSynchronizedMap(internal), "The stored map should be a Collections$SynchronizedMap wrapper");
    }

    @Test
    void nullCacheThrowsNullPointerException() {
        // GIVEN / WHEN / THEN
        assertThrows(NullPointerException.class, () -> ReflectionUtils.setSortedConstructorsCache(null), "Passing null must result in a NullPointerException");
    }

    @Test
    void concurrentHashMapIsStoredUnchanged() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>[]> supplied = new ConcurrentHashMap<>();
        // WHEN
        ReflectionUtils.setSortedConstructorsCache(supplied);
        Map<Object, Constructor<?>[]> internal = getInternalCache();
        // THEN
        assertSame(supplied, internal, "A ConcurrentHashMap should be stored without additional wrapping");
    }

    @Test
    void collectionsSynchronizedMapIsStoredUnchanged() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>[]> supplied = Collections.synchronizedMap(new HashMap<>());
        // WHEN
        ReflectionUtils.setSortedConstructorsCache(supplied);
        Map<Object, Constructor<?>[]> internal = getInternalCache();
        // THEN
        assertSame(supplied, internal, "A map already wrapped by Collections.synchronizedMap must be stored unchanged");
    }

    @Test
    void concurrentSkipListMapIsStoredUnchanged() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>[]> supplied = new ConcurrentSkipListMap<>();
        // WHEN
        ReflectionUtils.setSortedConstructorsCache(supplied);
        Map<Object, Constructor<?>[]> internal = getInternalCache();
        // THEN
        assertSame(supplied, internal, "A ConcurrentSkipListMap should be stored without additional wrapping");
    }

    @Test
    void lruCacheIsWrappedButEntriesPreserved() throws Exception {
        // GIVEN
        // LRUCache is package‑private in the same package; we can instantiate it directly.
        // The cache size constant is package‑private static final int CACHE_SIZE.
        int cacheSize = (int) ReflectionUtils.class.getDeclaredField("CACHE_SIZE").get(null);
        @SuppressWarnings("unchecked")
        Map<Object, Constructor<?>[]> supplied = new LRUCache<>(cacheSize);
        // put a couple of entries to verify they survive the wrapping
        Constructor<?> stringCtor = String.class.getDeclaredConstructor();
        Constructor<?> intCtor = Integer.class.getDeclaredConstructor(int.class);
        supplied.put(String.class, new Constructor[] { stringCtor });
        supplied.put(Integer.class, new Constructor[] { intCtor });
        // WHEN
        ReflectionUtils.setSortedConstructorsCache(supplied);
        Map<Object, Constructor<?>[]> internal = getInternalCache();
        // THEN
        assertNotSame(supplied, internal, "LRUCache must be wrapped into a thread‑safe map, thus a different instance");
        assertTrue(isSynchronizedMap(internal), "The wrapper for LRUCache should be a Collections$SynchronizedMap");
        // verify entries are identical (same key objects and same constructor arrays)
        assertEquals(supplied.size(), internal.size(), "All entries must be present after wrapping");
        assertArrayEquals(supplied.get(String.class), internal.get(String.class), "Constructor array for String must be preserved");
        assertArrayEquals(supplied.get(Integer.class), internal.get(Integer.class), "Constructor array for Integer must be preserved");
    }

    @Test
    void unmodifiableMapIsWrappedIntoSynchronizedMap() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>[]> base = new HashMap<>();
        // dummy entry
        base.put(Sample.class, new Constructor<?>[0]);
        Map<Object, Constructor<?>[]> supplied = Collections.unmodifiableMap(base);
        // WHEN
        ReflectionUtils.setSortedConstructorsCache(supplied);
        Map<Object, Constructor<?>[]> internal = getInternalCache();
        // THEN
        assertNotSame(supplied, internal, "An unmodifiable map must be wrapped to become thread‑safe");
        assertTrue(isSynchronizedMap(internal), "The resulting wrapper should be a Collections$SynchronizedMap");
        // entries must be preserved
        assertEquals(base, internal, "All entries from the original unmodifiable map must be present");
    }

    @Test
    void hashMapWithEntriesIsWrappedAndPreservesContent() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>[]> supplied = new HashMap<>() {

            {
                try {
                    put(String.class, new Constructor[] { String.class.getDeclaredConstructor() });
                    put(Integer.class, new Constructor[] { Integer.class.getDeclaredConstructor(int.class) });
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // WHEN
        ReflectionUtils.setSortedConstructorsCache(supplied);
        Map<Object, Constructor<?>[]> internal = getInternalCache();
        // THEN
        assertNotSame(supplied, internal, "The supplied HashMap must be wrapped, resulting in a different instance");
        assertTrue(isSynchronizedMap(internal), "The wrapper should be a Collections$SynchronizedMap");
        // Verify that each entry is present and equal (same constructor objects)
        assertEquals(supplied.size(), internal.size(), "Wrapped map must contain the same number of entries");
        for (Object key : supplied.keySet()) {
            assertArrayEquals(supplied.get(key), internal.get(key), "Entry for key " + key + " must be preserved after wrapping");
        }
    }
}
