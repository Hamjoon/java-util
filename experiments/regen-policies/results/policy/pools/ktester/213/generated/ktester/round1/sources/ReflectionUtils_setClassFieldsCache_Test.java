package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setClassFieldsCache_Test {

    /**
     * Simple class used for reflection tests.
     */
    static class Sample {

        private int id;

        protected String name;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the default cache is restored before each test to avoid cross‑test interference.
        // The default cache is created by ReflectionUtils itself, so we simply clear any
        // custom cache we may have installed in a previous test.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the cache after each test as a safety net.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
    }

    /**
     * Supplies test data for {@link #testSetClassFieldsCacheBasicScenarios}.
     *
     * @return a stream of arguments: description, map to set, expected wrapper class,
     *         and a flag indicating whether the same instance should be kept.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> basicCacheProvider() {
        // mutable HashMap – should be wrapped into Collections$SynchronizedMap
        Map<Object, Collection<Field>> mutable = new HashMap<>();
        mutable.put("mutKey", ReflectionUtils.getAllDeclaredFields(String.class));
        // already synchronized map – should be kept as‑is
        Map<Object, Collection<Field>> sync = Collections.synchronizedMap(new HashMap<>());
        sync.put("syncKey", ReflectionUtils.getAllDeclaredFields(Integer.class));
        // ConcurrentHashMap – should be kept as‑is
        Map<Object, Collection<Field>> concurrent = new ConcurrentHashMap<>();
        concurrent.put("concKey", ReflectionUtils.getAllDeclaredFields(Long.class));
        // empty mutable map – wrapped
        Map<Object, Collection<Field>> empty = new HashMap<>();
        return Stream.of(org.junit.jupiter.params.provider.Arguments.arguments("Mutable HashMap (should be wrapped)", mutable, Collections.synchronizedMap(new HashMap<>()).getClass(), false), org.junit.jupiter.params.provider.Arguments.arguments("Already synchronized map (no wrapping)", sync, sync.getClass(), true), org.junit.jupiter.params.provider.Arguments.arguments("ConcurrentHashMap (no wrapping)", concurrent, concurrent.getClass(), true), org.junit.jupiter.params.provider.Arguments.arguments("Empty mutable HashMap (should be wrapped)", empty, Collections.synchronizedMap(new HashMap<>()).getClass(), false));
    }

    /**
     * Verify that {@code setClassFieldsCache} replaces the internal cache with the
     * supplied map and that subsequent reflective calls use the supplied cache.
     */
    @Test
    void testSetClassFieldsCacheReplacesCache() {
        // --- given ------------------------------------------------------------
        // Build a custom cache containing a pre‑computed collection of fields for Sample.class.
        Collection<Field> expectedFields = ReflectionUtils.getAllDeclaredFields(Sample.class);
        Map<Object, Collection<Field>> customCache = new HashMap<>();
        // the key used by the implementation is the class itself
        customCache.put(Sample.class, expectedFields);
        // --- when -------------------------------------------------------------
        ReflectionUtils.setClassFieldsCache(customCache);
        // --- then -------------------------------------------------------------
        // The next call to getAllDeclaredFields should return the exact collection we put into the cache.
        Collection<Field> actualFields = ReflectionUtils.getAllDeclaredFields(Sample.class);
        assertSame(expectedFields, actualFields, "ReflectionUtils should return the cached collection after setClassFieldsCache is invoked");
    }

    /**
     * Verify that calling {@code setClassFieldsCache} with an empty map does not
     * throw and that reflective calls still work (they will simply return empty
     * collections because the cache is empty).
     */
    @Test
    void testSetClassFieldsCacheWithEmptyMapDoesNotThrow() {
        // --- given ------------------------------------------------------------
        Map<Object, Collection<Field>> emptyCache = new HashMap<>();
        // --- when -------------------------------------------------------------
        assertDoesNotThrow(() -> ReflectionUtils.setClassFieldsCache(emptyCache));
        // --- then -------------------------------------------------------------
        // Because the cache is empty, a request for fields of Sample.class should result
        // in an empty collection (the implementation falls back to an empty list when the
        // cache does not contain the requested entry).
        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(Sample.class);
        assertTrue(fields.isEmpty(), "When the cache is empty, getAllDeclaredFields should return an empty collection");
    }

    /**
     * Verify that a {@code null} argument is rejected with a {@link NullPointerException}.
     */
    @Test
    void testSetClassFieldsCacheNullThrowsNPE() {
        // --- when -------------------------------------------------------------
        // The method is expected to validate its argument and throw NPE for null.
        assertThrows(NullPointerException.class, () -> ReflectionUtils.setClassFieldsCache(null));
    }

    /* --------------------------------------------------------------------- *
     * Helper – retrieve the internal static cache via reflection.
     * --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static Map<Object, Collection<Field>> getInternalCacheMap() throws Exception {
        Field cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
        cacheField.setAccessible(true);
        AtomicReference<Map<?, ?>> ref = (AtomicReference<Map<?, ?>>) cacheField.get(null);
        return (Map<Object, Collection<Field>>) ref.get();
    }

    /* --------------------------------------------------------------------- *
     * Basic functionality – mutable map is wrapped into a synchronized map.
     * --------------------------------------------------------------------- */
    @Test
    void testSetClassFieldsCacheWithMutableMapWrapsIntoSynchronizedMap() throws Exception {
        // --- given ------------------------------------------------------------
        Collection<Field> stringFields = ReflectionUtils.getAllDeclaredFields(String.class);
        Map<Object, Collection<Field>> mutableMap = new HashMap<>();
        // key is a String, value is a collection of Fields
        mutableMap.put("testKey", stringFields);
        // --- when -------------------------------------------------------------
        ReflectionUtils.setClassFieldsCache(mutableMap);
        // --- then -------------------------------------------------------------
        Map<Object, Collection<Field>> internal = getInternalCacheMap();
        // The map stored internally must NOT be the same instance we passed (it should be wrapped).
        assertNotSame(mutableMap, internal, "A mutable HashMap should be wrapped into a thread‑safe map");
        // The wrapper used by ReflectionUtils is a synchronized map (Collections$SynchronizedMap).
        assertTrue(internal.getClass().getName().contains("SynchronizedMap"), "Internal cache should be an instance of Collections$SynchronizedMap");
        // The entry must be present and the collection reference must be the same as the one we supplied.
        assertSame(stringFields, internal.get("testKey"), "The stored collection should be the exact instance supplied in the map");
    }

    /* --------------------------------------------------------------------- *
     * Basic functionality – already thread‑safe map is kept as‑is.
     * --------------------------------------------------------------------- */
    @Test
    void testSetClassFieldsCacheWithAlreadySynchronizedMapKeepsReference() throws Exception {
        // --- given ------------------------------------------------------------
        Collection<Field> intFields = ReflectionUtils.getAllDeclaredFields(Integer.class);
        Map<Object, Collection<Field>> syncMap = Collections.synchronizedMap(new HashMap<>());
        syncMap.put("k1", intFields);
        syncMap.put("k2", Collections.emptyList());
        // --- when -------------------------------------------------------------
        ReflectionUtils.setClassFieldsCache(syncMap);
        // --- then -------------------------------------------------------------
        Map<Object, Collection<Field>> internal = getInternalCacheMap();
        // The exact same map instance must be stored (no additional wrapping).
        assertSame(syncMap, internal, "An already synchronized map should be stored without creating a new wrapper");
    }

    /* --------------------------------------------------------------------- *
     * Basic functionality – ConcurrentHashMap is recognized as thread‑safe.
     * --------------------------------------------------------------------- */
    @Test
    void testSetClassFieldsCacheWithConcurrentHashMapKeepsReference() throws Exception {
        // --- given ------------------------------------------------------------
        Collection<Field> longFields = ReflectionUtils.getAllDeclaredFields(Long.class);
        Map<Object, Collection<Field>> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put(123, longFields);
        // --- when -------------------------------------------------------------
        ReflectionUtils.setClassFieldsCache(concurrentMap);
        // --- then -------------------------------------------------------------
        Map<Object, Collection<Field>> internal = getInternalCacheMap();
        // The exact same ConcurrentHashMap instance must be stored.
        assertSame(concurrentMap, internal, "A ConcurrentHashMap should be stored unchanged because it is already thread‑safe");
    }

    /* --------------------------------------------------------------------- *
     * Basic functionality – empty mutable map is wrapped correctly.
     * --------------------------------------------------------------------- */
    @Test
    void testSetClassFieldsCacheWithEmptyMap() throws Exception {
        // --- given ------------------------------------------------------------
        Map<Object, Collection<Field>> emptyMap = new HashMap<>();
        // --- when -------------------------------------------------------------
        ReflectionUtils.setClassFieldsCache(emptyMap);
        // --- then -------------------------------------------------------------
        Map<Object, Collection<Field>> internal = getInternalCacheMap();
        // Should be a synchronized wrapper and be empty.
        assertTrue(internal.getClass().getName().contains("SynchronizedMap"), "Empty map should also be wrapped into a synchronized map");
        assertTrue(internal.isEmpty(), "The internal cache should be empty after setting an empty map");
    }

    /* --------------------------------------------------------------------- *
     * Cache replacement semantics – new cache overwrites previous contents.
     * --------------------------------------------------------------------- */
    @Test
    void testCacheReplacementSemantics() throws Exception {
        // --- given ------------------------------------------------------------
        // First cache with an entry "oldKey".
        Collection<Field> intFields = ReflectionUtils.getAllDeclaredFields(Integer.class);
        Map<Object, Collection<Field>> oldCache = new HashMap<>();
        oldCache.put("oldKey", intFields);
        ReflectionUtils.setClassFieldsCache(oldCache);
        // Verify the old entry is present.
        Map<Object, Collection<Field>> afterOld = getInternalCacheMap();
        assertTrue(afterOld.containsKey("oldKey"), "Old cache should contain the key \"oldKey\" before replacement");
        // New cache with a different entry "newKey".
        Collection<Field> longFields = ReflectionUtils.getAllDeclaredFields(Long.class);
        Map<Object, Collection<Field>> newCache = new HashMap<>();
        newCache.put("newKey", longFields);
        // --- when -------------------------------------------------------------
        ReflectionUtils.setClassFieldsCache(newCache);
        // --- then -------------------------------------------------------------
        Map<Object, Collection<Field>> afterNew = getInternalCacheMap();
        // The old key must be gone.
        assertFalse(afterNew.containsKey("oldKey"), "After replacement, the old key should no longer be present in the cache");
        // The new key must be present and map to the exact collection we supplied.
        assertTrue(afterNew.containsKey("newKey"), "The new cache should contain the key \"newKey\" after replacement");
        assertSame(longFields, afterNew.get("newKey"), "The collection stored for \"newKey\" must be the exact instance supplied");
    }

    /* --------------------------------------------------------------------- *
     * Parameterized test covering the three “basic functionality” scenarios.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicCacheProvider")
    void testSetClassFieldsCacheBasicScenarios(String description, Map<Object, Collection<Field>> inputMap, Class<?> expectedWrapperClass, boolean expectSameInstance) throws Exception {
        // --- when -------------------------------------------------------------
        ReflectionUtils.setClassFieldsCache(inputMap);
        // --- then -------------------------------------------------------------
        Map<Object, Collection<Field>> internal = getInternalCacheMap();
        if (expectSameInstance) {
            // The internal map should be the exact same instance we passed.
            assertSame(inputMap, internal, "Expected the same map instance to be stored for: " + description);
        } else {
            // The internal map should be wrapped; verify the wrapper type.
            assertEquals(expectedWrapperClass, internal.getClass(), "Unexpected wrapper type for: " + description);
            // Ensure at least one entry (if any) is preserved.
            if (!inputMap.isEmpty()) {
                Object key = inputMap.keySet().iterator().next();
                assertSame(inputMap.get(key), internal.get(key), "Stored collection should be the same instance for key: " + key);
            }
        }
    }
}
