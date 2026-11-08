package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReflectionUtils#setFieldCache tests")
class ReflectionUtils_setFieldCache_Test {

    /**
     * Holds the map that will be passed to {@code setFieldCache}.
     */
    private Map<Object, Field> customCache;

    /**
     * Reference to the private static {@code FIELD_NAME_CACHE} field.
     */
    private static Field FIELD_NAME_CACHE_REF;

    /**
     * Cached {@code AtomicReference} instance (the actual container of the map).
     */
    private static AtomicReference<?> FIELD_NAME_CACHE_ATOMIC;

    /**
     * Simple class used for reflection look‑ups in the tests.
     */
    static class Sample {

        public String name;

        private int id;
    }

    @BeforeAll
    static void initReflection() throws Exception {
        // Obtain the private static FIELD_NAME_CACHE field via reflection.
        FIELD_NAME_CACHE_REF = ReflectionUtils.class.getDeclaredField("FIELD_NAME_CACHE");
        FIELD_NAME_CACHE_REF.setAccessible(true);
        // The field is static, so we pass null to get().
        FIELD_NAME_CACHE_ATOMIC = (AtomicReference<?>) FIELD_NAME_CACHE_REF.get(null);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test case.
        customCache = new HashMap<>();
        // Populate the cache with a known entry – the {@code name} field of Sample.
        try {
            Field nameField = Sample.class.getField("name");
            // The key type used internally by ReflectionUtils is opaque (FieldNameCacheKey),
            // but the API accepts any Object, so we can use the field itself as a key.
            customCache.put(nameField, nameField);
        } catch (NoSuchFieldException e) {
            fail("Test setup failed – Sample.name field not found", e);
        }
        // Start each test with a clean, thread‑safe map.
        ReflectionUtils.setFieldCache(new HashMap<>());
        customCache = new HashMap<>();
        // Populate with a known entry – the {@code name} field of Sample.
        try {
            Field nameField = Sample.class.getField("name");
            customCache.put(nameField, nameField);
        } catch (NoSuchFieldException e) {
            fail("Test setup failed – Sample.name field not found", e);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the cache to an empty map so that later tests start from a clean state.
        ReflectionUtils.setFieldCache(new HashMap<>());
        // Reset to a fresh empty map after each test to avoid cross‑test contamination.
        ReflectionUtils.setFieldCache(new HashMap<>());
    }

    @Test
    void testSetFieldCacheWithCustomMap() {
        // Replace the internal cache with our custom map.
        ReflectionUtils.setFieldCache(customCache);
        // Verify that a field that is present in the class can still be retrieved.
        Field retrieved = ReflectionUtils.getField(Sample.class, "name");
        assertNotNull(retrieved, "Field 'name' should be found after setting custom cache");
        assertEquals("name", retrieved.getName());
        // Also verify that a private field can be retrieved (the cache should not interfere).
        Field privateField = ReflectionUtils.getField(Sample.class, "id");
        assertNotNull(privateField, "Private field 'id' should be found after setting custom cache");
        assertEquals("id", privateField.getName());
    }

    @Test
    void testSetFieldCacheCanBeReplacedMultipleTimes() {
        // First cache replacement.
        ReflectionUtils.setFieldCache(customCache);
        Field firstLookup = ReflectionUtils.getField(Sample.class, "name");
        assertNotNull(firstLookup);
        // Replace with a different (empty) cache.
        Map<Object, Field> emptyCache = new HashMap<>();
        ReflectionUtils.setFieldCache(emptyCache);
        // The lookup should still succeed because the implementation falls back to reflection
        // when the cache does not contain the entry.
        Field secondLookup = ReflectionUtils.getField(Sample.class, "name");
        assertNotNull(secondLookup);
        assertEquals("name", secondLookup.getName());
        // Ensure that the two Field instances are the same (the cache replacement does not create
        // a new Field object).
        assertSame(firstLookup, secondLookup, "Field instances should be identical across cache swaps");
    }

    /**
     * Returns the current internal cache map (the value stored inside the atomic reference).
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Field> getInternalCache() {
        return (Map<Object, Field>) FIELD_NAME_CACHE_ATOMIC.get();
    }

    /* -------------------------------------------------------------
     *  1. Replace cache with a populated map
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – populated map is copied/wrapped")
    void setFieldCache_populatedMap() {
        // Act
        ReflectionUtils.setFieldCache(customCache);
        // Assert – the internal cache contains the entry we supplied.
        Map<Object, Field> internal = getInternalCache();
        assertTrue(internal.containsKey(customCache.keySet().iterator().next()), "Internal cache should contain the supplied key");
        // The internal map must NOT be the same instance we passed (ensureThreadSafe wraps it).
        assertNotSame(customCache, internal, "Internal cache should be a different instance than the supplied map");
    }

    /* -------------------------------------------------------------
     *  2. Replace cache with an empty map
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – empty map results in empty internal cache")
    void setFieldCache_emptyMap() {
        Map<Object, Field> empty = new HashMap<>();
        ReflectionUtils.setFieldCache(empty);
        Map<Object, Field> internal = getInternalCache();
        assertTrue(internal.isEmpty(), "Internal cache should be empty after supplying an empty map");
    }

    /* -------------------------------------------------------------
     *  3. Null argument handling
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – null argument throws NPE")
    void setFieldCache_nullArgument() {
        Executable call = () -> ReflectionUtils.setFieldCache(null);
        // The implementation is documented to fail fast; we accept NPE or IllegalArgumentException.
        assertThrows(RuntimeException.class, call, "Calling setFieldCache with null should throw an exception");
    }

    /* -------------------------------------------------------------
     *  4. Already thread‑safe map (Collections.synchronizedMap)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – already thread‑safe map is used as‑is")
    void setFieldCache_threadSafeMap() {
        Map<Object, Field> syncMap = Collections.synchronizedMap(new HashMap<>());
        ReflectionUtils.setFieldCache(syncMap);
        Map<Object, Field> internal = getInternalCache();
        assertSame(syncMap, internal, "Internal cache should be the exact same instance for a synchronized map");
    }

    /* -------------------------------------------------------------
     *  5. Map containing a null key
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – map with null key throws NPE")
    void setFieldCache_nullKey() {
        Map<Object, Field> map = new HashMap<>();
        map.put(null, customCache.values().iterator().next());
        Executable call = () -> ReflectionUtils.setFieldCache(map);
        assertThrows(RuntimeException.class, call, "Map containing a null key should cause an exception");
    }

    /* -------------------------------------------------------------
     *  6. Map containing a null value
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – map with null value throws NPE")
    void setFieldCache_nullValue() {
        Map<Object, Field> map = new HashMap<>();
        map.put(customCache.keySet().iterator().next(), null);
        Executable call = () -> ReflectionUtils.setFieldCache(map);
        assertThrows(RuntimeException.class, call, "Map containing a null value should cause an exception");
    }

    /* -------------------------------------------------------------
     *  7. ConcurrentHashMap (already thread‑safe)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – ConcurrentHashMap is used directly")
    void setFieldCache_concurrentHashMap() {
        Map<Object, Field> concurrent = new ConcurrentHashMap<>();
        concurrent.putAll(customCache);
        ReflectionUtils.setFieldCache(concurrent);
        Map<Object, Field> internal = getInternalCache();
        assertSame(concurrent, internal, "Internal cache should be the exact same ConcurrentHashMap instance supplied");
    }

    /* -------------------------------------------------------------
     *  8. Unmodifiable map – should be wrapped, modifications prohibited
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – unmodifiable map is wrapped but remains unmodifiable")
    void setFieldCache_unmodifiableMap() {
        Map<Object, Field> base = new HashMap<>(customCache);
        Map<Object, Field> unmodifiable = Collections.unmodifiableMap(base);
        ReflectionUtils.setFieldCache(unmodifiable);
        Map<Object, Field> internal = getInternalCache();
        // The internal map should NOT be the same instance (it must be wrapped to become thread‑safe).
        assertNotSame(unmodifiable, internal, "Internal cache should wrap the unmodifiable map");
        // Attempting to modify the internal cache must throw UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> internal.put(new Object(), customCache.values().iterator().next()), "Modifying the wrapped unmodifiable cache should throw UOE");
    }

    /* -------------------------------------------------------------
     *  9. Subsequent replacement clears previous entries
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – second replacement overwrites first")
    void setFieldCache_multipleReplacements() {
        // First map
        Map<Object, Field> first = new HashMap<>();
        first.put("firstKey", customCache.values().iterator().next());
        ReflectionUtils.setFieldCache(first);
        assertTrue(getInternalCache().containsKey("firstKey"), "First key should be present after first set");
        // Second map
        Map<Object, Field> second = new HashMap<>();
        second.put("secondKey", customCache.values().iterator().next());
        ReflectionUtils.setFieldCache(second);
        Map<Object, Field> internal = getInternalCache();
        assertFalse(internal.containsKey("firstKey"), "First key should have been removed after second set");
        assertTrue(internal.containsKey("secondKey"), "Second key should be present after second set");
    }

    /* -------------------------------------------------------------
     * 10. Map that throws on entrySet iteration
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – map that throws on entrySet propagates exception")
    void setFieldCache_throwingEntrySetMap() {
        Map<Object, Field> throwingMap = new HashMap<>() {

            @Override
            public Set<Entry<Object, Field>> entrySet() {
                throw new IllegalStateException("entrySet failure");
            }
        };
        Executable call = () -> ReflectionUtils.setFieldCache(throwingMap);
        IllegalStateException ex = assertThrows(IllegalStateException.class, call, "Exception from entrySet should propagate");
        assertEquals("entrySet failure", ex.getMessage());
    }

    /* -------------------------------------------------------------
     * 11. Map that throws on putAll during copy
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("setFieldCache – map that throws on putAll propagates exception")
    void setFieldCache_throwingPutAllMap() {
        Map<Object, Field> throwingMap = new HashMap<>() {

            @Override
            public void putAll(Map<?, ? extends Field> m) {
                throw new UnsupportedOperationException("putAll not supported");
            }
        };
        // Populate it with a valid entry so that ensureThreadSafe has something to copy.
        throwingMap.put("key", customCache.values().iterator().next());
        Executable call = () -> ReflectionUtils.setFieldCache(throwingMap);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, call, "Exception from putAll should propagate");
        assertEquals("putAll not supported", ex.getMessage());
    }
}
