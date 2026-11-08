package com.cedarsoftware.util;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setConstructorCache_Test {

    /**
     * Holds the original cache so we can restore it after each test.
     */
    private Map<Object, Constructor<?>> originalCacheMap;

    /* --------------------------------------------------------------
     *  Dummy class used for reflective operations.
     * -------------------------------------------------------------- */
    static class Dummy {
        // default no‑arg constructor
    }

    @BeforeEach
    void captureOriginalCache() throws Exception {
        originalCacheMap = getInternalCacheMap();
    }

    @AfterEach
    void restoreOriginalCache() {
        // Restore the cache to the state captured before the test.
        // The original map is already thread‑safe (it was the default LRUCache),
        // so calling setConstructorCache with it is safe.
        ReflectionUtils.setConstructorCache(originalCacheMap);
    }

    /**
     * Verifies that {@link ReflectionUtils#setConstructorCache(Map)} accepts a user‑provided map
     * and that subsequent reflective operations (e.g., {@link ReflectionUtils#getAllConstructors(Class)})
     * continue to work correctly. The test does not inspect the private cache directly,
     * but checks observable behaviour after the cache swap.
     */
    @Test
    void testSetConstructorCacheReplacesCache() throws Exception {
        // GIVEN: a custom cache containing an arbitrary entry
        Map<Object, Constructor<?>> customCache = new HashMap<>();
        Constructor<Dummy> dummyConstructor = Dummy.class.getDeclaredConstructor();
        // key is irrelevant for the utility logic
        Object dummyKey = new Object();
        customCache.put(dummyKey, dummyConstructor);
        // WHEN: the custom cache is installed
        ReflectionUtils.setConstructorCache(customCache);
        // THEN: reflective utilities must still return correct information for a class
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(Dummy.class);
        assertNotNull(constructors, "Constructors array should not be null");
        assertTrue(constructors.length > 0, "Dummy class should have at least one constructor");
        assertEquals(Dummy.class, constructors[0].getDeclaringClass(), "The returned constructor should belong to Dummy class");
    }

    /**
     * Helper to obtain the internal cache map via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Constructor<?>> getInternalCacheMap() throws Exception {
        Field field = ReflectionUtils.class.getDeclaredField("CONSTRUCTOR_CACHE");
        field.setAccessible(true);
        AtomicReference<Map<?, Constructor<?>>> ref = (AtomicReference<Map<?, Constructor<?>>>) field.get(null);
        return (Map<Object, Constructor<?>>) ref.get();
    }

    /* --------------------------------------------------------------
     *  Basic functionality – non‑thread‑safe map is wrapped.
     * -------------------------------------------------------------- */
    @Test
    void setConstructorCache_wrapsNonThreadSafeMap_andKeepsEntry() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>> custom = new HashMap<>();
        Constructor<Dummy> ctor = Dummy.class.getDeclaredConstructor();
        String key = "key";
        custom.put(key, ctor);
        // WHEN
        ReflectionUtils.setConstructorCache(custom);
        // THEN
        Map<Object, Constructor<?>> internal = getInternalCacheMap();
        assertNotSame(custom, internal, "A non‑thread‑safe map should be wrapped, resulting in a different instance.");
        assertEquals(ctor, internal.get(key), "The entry must be preserved after wrapping.");
    }

    /* --------------------------------------------------------------
     *  Thread‑safe map (ConcurrentHashMap) is used as‑is.
     * -------------------------------------------------------------- */
    @Test
    void setConstructorCache_usesConcurrentHashMapDirectly() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>> concurrent = new ConcurrentHashMap<>();
        Constructor<Dummy> ctor = Dummy.class.getDeclaredConstructor();
        String cKey = "cKey";
        concurrent.put(cKey, ctor);
        // WHEN
        ReflectionUtils.setConstructorCache(concurrent);
        // THEN
        Map<Object, Constructor<?>> internal = getInternalCacheMap();
        assertSame(concurrent, internal, "A ConcurrentHashMap should be used directly without wrapping.");
        assertEquals(ctor, internal.get(cKey), "The entry must be present in the cache.");
    }

    /* --------------------------------------------------------------
     *  Null argument handling – should throw NPE.
     * -------------------------------------------------------------- */
    @Test
    void setConstructorCache_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.setConstructorCache(null), "Passing null to setConstructorCache should result in NullPointerException.");
    }

    /* --------------------------------------------------------------
     *  Empty HashMap – wrapped and remains empty.
     * -------------------------------------------------------------- */
    @Test
    void setConstructorCache_emptyHashMap_isWrappedAndEmpty() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>> empty = new HashMap<>();
        // WHEN
        ReflectionUtils.setConstructorCache(empty);
        // THEN
        Map<Object, Constructor<?>> internal = getInternalCacheMap();
        assertNotSame(empty, internal, "Empty non‑thread‑safe map must be wrapped.");
        assertTrue(internal.isEmpty(), "The wrapped cache should be empty.");
    }

    /* --------------------------------------------------------------
     *  Replacement overrides previous cache.
     * -------------------------------------------------------------- */
    @Test
    void setConstructorCache_replacesPreviousCache() throws Exception {
        // First map
        Map<Object, Constructor<?>> first = new HashMap<>();
        Constructor<Dummy> ctor = Dummy.class.getDeclaredConstructor();
        first.put("first", ctor);
        ReflectionUtils.setConstructorCache(first);
        Map<Object, Constructor<?>> afterFirst = getInternalCacheMap();
        assertEquals(ctor, afterFirst.get("first"));
        assertEquals(1, afterFirst.size());
        // Second map (thread‑safe)
        Map<Object, Constructor<?>> second = new ConcurrentHashMap<>();
        second.put("second", ctor);
        ReflectionUtils.setConstructorCache(second);
        Map<Object, Constructor<?>> afterSecond = getInternalCacheMap();
        assertEquals(ctor, afterSecond.get("second"));
        assertNull(afterSecond.get("first"), "Previous entry must not be present after replacement.");
        assertEquals(1, afterSecond.size());
    }

    /* --------------------------------------------------------------
     *  Synchronized map is recognized as thread‑safe.
     * -------------------------------------------------------------- */
    @Test
    void setConstructorCache_synchronizedMap_isRecognizedAsThreadSafe() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>> sync = Collections.synchronizedMap(new HashMap<>());
        Constructor<Dummy> ctor = Dummy.class.getDeclaredConstructor();
        sync.put("syncKey", ctor);
        // WHEN
        ReflectionUtils.setConstructorCache(sync);
        // THEN
        Map<Object, Constructor<?>> internal = getInternalCacheMap();
        assertSame(sync, internal, "A synchronized map should be used directly.");
        assertEquals(ctor, internal.get("syncKey"));
    }

    /* --------------------------------------------------------------
     *  Map with null key.
     * -------------------------------------------------------------- */
    @Test
    void setConstructorCache_mapWithNullKey_preservesNullKey() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>> map = new HashMap<>();
        Constructor<Dummy> ctor = Dummy.class.getDeclaredConstructor();
        map.put(null, ctor);
        // WHEN
        ReflectionUtils.setConstructorCache(map);
        // THEN
        Map<Object, Constructor<?>> internal = getInternalCacheMap();
        assertTrue(internal.containsKey(null), "Cache must contain the null key.");
        assertEquals(ctor, internal.get(null));
    }

    /* --------------------------------------------------------------
     *  Map with null constructor value.
     * -------------------------------------------------------------- */
    @Test
    void setConstructorCache_mapWithNullValue_preservesNullValue() throws Exception {
        // GIVEN
        Map<Object, Constructor<?>> map = new HashMap<>();
        map.put("nullVal", null);
        // WHEN
        ReflectionUtils.setConstructorCache(map);
        // THEN
        Map<Object, Constructor<?>> internal = getInternalCacheMap();
        assertTrue(internal.containsKey("nullVal"), "Cache must contain the key 'nullVal'.");
        assertNull(internal.get("nullVal"), "The value associated with 'nullVal' must be null.");
    }
}
