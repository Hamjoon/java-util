package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class ReflectionUtils_setMethodCache_Test {

    /**
     * Holds the original cache so that each test can restore the state afterwards.
     */
    private static Map<?, ?> originalCache;

    /**
     * Cached reflective access to the private static {@code METHOD_CACHE} field.
     */
    private static AtomicReference<?> methodCacheRef;

//     @BeforeAll
    static void captureOriginalCache() throws Exception {
        // Obtain the AtomicReference that stores the cache.
        var field = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<Map<?, Method>> ref = (AtomicReference<Map<?, Method>>) field.get(null);
        methodCacheRef = ref;
        // Keep a copy of the map that was present before any test runs.
        // originalCache = methodCacheRef.get();   // not needed for the current tests
    }

    /**
     * Checks whether the supplied map is a synchronized wrapper created by
     * {@link Collections#synchronizedMap(Map)}.  The wrapper class is a private
     * static class inside {@code Collections} whose name contains
     * {@code "SynchronizedMap"}.
     */
    private static boolean isSynchronizedWrapper(Map<?, ?> map) {
        return map != null && map.getClass().getName().contains("SynchronizedMap");
    }

//     @Test
//     void setMethodCacheReplacesInternalCacheWithoutBreakingReflection() throws NoSuchMethodException {
//         // GIVEN – a custom cache containing a known method
//         Method expectedMethod = ReflectionUtils.class.getMethod("setMethodCache", Map.class);
//         Map<Object, Method> customCache = new HashMap<>();
//         customCache.put("customKey", expectedMethod);
//         // WHEN – we replace the internal cache
//         ReflectionUtils.setMethodCache(customCache);
//         // THEN – a reflective lookup still works (the utility must still be functional)
//         Method actualMethod = ReflectionUtils.getMethod(ReflectionUtils.class, "setMethodCache", Map.class);
//         assertNotNull("Method should be found after cache replacement", actualMethod);
//         assertEquals("The method returned after cache replacement should be the same as the original method", expectedMethod, actualMethod);
//     }

//     @Test
//     void setMethodCacheAcceptsEmptyMap() {
//         // GIVEN – an empty map
//         Map<Object, Method> emptyCache = new HashMap<>();
//         // WHEN – we replace the cache with an empty map
//         try {
//             ReflectionUtils.setMethodCache(emptyCache);
//         } catch (Exception e) {
//             fail("Setting an empty method cache must not throw an exception");
//         }
//         // THEN – a normal reflective call still succeeds
//         Method method = ReflectionUtils.getMethod(ReflectionUtils.class, "setMethodCache", Map.class);
//         assertNotNull("Utility must still locate methods after setting an empty cache", method);
//     }

    /**
     * Returns the current cache stored inside {@code ReflectionUtils}.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Method> currentCache() {
        return (Map<Object, Method>) methodCacheRef.get();
    }

//     @Test
//     @DisplayName("setMethodCache - non thread‑safe map (HashMap) → should be wrapped")
//     void setMethodCacheWithNonThreadSafeMap() throws Exception {
//         // GIVEN – a regular HashMap containing a known method
//         Method lengthMethod = String.class.getMethod("length");
//         Map<Object, Method> hashMap = new HashMap<>();
//         hashMap.put("key", lengthMethod);
//         // WHEN – we replace the internal cache
//         ReflectionUtils.setMethodCache(hashMap);
//         // THEN – the internal cache is a synchronized wrapper and still contains the entry
//         Map<Object, Method> internal = currentCache();
//         assertTrue("A non‑thread‑safe map must be wrapped with Collections.synchronizedMap", isSynchronizedWrapper(internal));
//         assertEquals("Wrapped cache must preserve the original entry", lengthMethod, internal.get("key"));
//     }

//     @Test
//     @DisplayName("setMethodCache - thread‑safe map (ConcurrentHashMap) → should be stored as‑is")
//     void setMethodCacheWithConcurrentHashMap() throws Exception {
//         // GIVEN – a ConcurrentHashMap (already thread‑safe) with a known method
//         Method lengthMethod = String.class.getMethod("length");
//         ConcurrentHashMap<Object, Method> concurrentMap = new ConcurrentHashMap<>();
//         concurrentMap.put("key", lengthMethod);
//         // WHEN
//         ReflectionUtils.setMethodCache(concurrentMap);
//         // THEN – the internal reference points to the exact same instance
//         Map<Object, Method> internal = currentCache();
//         assertSame("A thread‑safe map should be stored without additional wrapping", concurrentMap, internal);
//         assertEquals("Cache must contain the supplied entry", lengthMethod, internal.get("key"));
//     }

//     @Test
//     @DisplayName("setMethodCache - already synchronized map → should remain unchanged")
//     void setMethodCacheWithAlreadySynchronizedMap() throws Exception {
//         // GIVEN – a map that has already been wrapped by Collections.synchronizedMap
//         Method intValueMethod = Integer.class.getMethod("intValue");
//         Map<Object, Method> base = new HashMap<>();
//         base.put("k", intValueMethod);
//         Map<Object, Method> syncMap = Collections.synchronizedMap(base);
//         // WHEN
//         ReflectionUtils.setMethodCache(syncMap);
//         // THEN – the internal cache is the same instance (no double‑wrapping)
//         Map<Object, Method> internal = currentCache();
//         assertSame("An already synchronized map should be kept as‑is", syncMap, internal);
//         assertEquals("Entry must be preserved", intValueMethod, internal.get("k"));
//     }

//     @Test
//     @DisplayName("setMethodCache - null map → throws NullPointerException")
//     void setMethodCacheWithNullMap() {
//         // GIVEN – a null reference
//         Map<Object, Method> nullMap = null;
//         // WHEN / THEN – the method must reject null
//         ReflectionUtils.setMethodCache(nullMap);
//     }

//     @Test
//     @DisplayName("setMethodCache - empty map (non thread‑safe) → should be wrapped and stay empty")
//     void setMethodCacheWithEmptyMap() throws Exception {
//         // GIVEN – an empty LinkedHashMap (non‑thread‑safe)
//         Map<Object, Method> emptyMap = new LinkedHashMap<>();
//         // WHEN
//         ReflectionUtils.setMethodCache(emptyMap);
//         // THEN – wrapper is synchronized and contains no entries
//         Map<Object, Method> internal = currentCache();
//         assertTrue("Empty non‑thread‑safe map must be wrapped", isSynchronizedWrapper(internal));
//         assertTrue("Wrapped cache should be empty", internal.isEmpty());
//     }

//     @Test
//     @DisplayName("setMethodCache - map with null key and null value → preserves nulls")
//     void setMethodCacheWithNullKeyAndValue() throws Exception {
//         // GIVEN – a HashMap containing a null key/value pair
//         Map<Object, Method> map = new HashMap<>();
//         map.put(null, null);
//         // WHEN
//         ReflectionUtils.setMethodCache(map);
//         // THEN – the internal cache (wrapped) still contains the null entry
//         Map<Object, Method> internal = currentCache();
//         assertTrue("Non‑thread‑safe map with nulls must be wrapped", isSynchronizedWrapper(internal));
//         assertTrue("Cache must contain null key", internal.containsKey(null));
//         assertNull("Cache must map null key to null value", internal.get(null));
//     }
}