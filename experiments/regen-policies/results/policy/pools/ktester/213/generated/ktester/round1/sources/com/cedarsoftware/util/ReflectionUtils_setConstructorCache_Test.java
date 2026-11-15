package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
// import org.junit.*;
import java.util.*;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_setConstructorCache_Test {

    /**
     * Simple class used for constructor look‑ups.
     */
    static class Sample {

        public Sample() {
        }
    }

    /* --------------------------------------------------------------
     *  Group: null argument
     * -------------------------------------------------------------- */
//     @Test
//     void setConstructorCache_nullArgument_throwsNPE() {
//         ReflectionUtils.setConstructorCache(null);
//     }

    /* --------------------------------------------------------------
     *  Helper to obtain the internal cache map via reflection.
     * -------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static Map<Object, Constructor<?>> getInternalCache() throws Exception {
        Field field = ReflectionUtils.class.getDeclaredField("CONSTRUCTOR_CACHE");
        field.setAccessible(true);
        AtomicReference<Map<?, Constructor<?>>> ref = (AtomicReference<Map<?, Constructor<?>>>) field.get(null);
        return (Map<Object, Constructor<?>>) ref.get();
    }

    /* --------------------------------------------------------------
     *  Additional sanity check: getConstructor uses the custom cache
     * -------------------------------------------------------------- */
//     @Test
//     public void getConstructor_usesCustomCacheInstance() throws Exception {
//         // Prepare a custom map with a pre‑populated constructor
//         Map<Object, Constructor<?>> custom = new HashMap<>();
//         Constructor<Sample> sampleCtor = Sample.class.getDeclaredConstructor();
//         custom.put(Sample.class, sampleCtor);
//         ReflectionUtils.setConstructorCache(custom);
//         // The call should return the exact instance from the custom map
//         Constructor<?> retrieved = ReflectionUtils.getConstructor(Sample.class);
//         Assert.assertSame(sampleCtor, retrieved);
//     }

    /* --------------------------------------------------------------
     *  Group: empty map
     * -------------------------------------------------------------- */
//     @Test
//     public void setConstructorCache_emptyMap_resultsInEmptyCache() throws Exception {
//         ReflectionUtils.setConstructorCache(new HashMap<>());
//         Map<Object, Constructor<?>> cache = getInternalCache();
//         Assert.assertTrue(cache.isEmpty());
//     }

    /* --------------------------------------------------------------
     *  Group: map with null key
     * -------------------------------------------------------------- */
//     @Test
//     public void setConstructorCache_mapWithNullKey_isAccepted() throws Exception {
//         Map<Object, Constructor<?>> map = new HashMap<>();
//         map.put(null, String.class.getDeclaredConstructor());
//         ReflectionUtils.setConstructorCache(map);
//         Map<Object, Constructor<?>> cache = getInternalCache();
//         Assert.assertEquals(1, cache.size());
//         Assert.assertSame(String.class.getDeclaredConstructor(), cache.get(null));
//     }

    /* --------------------------------------------------------------
     *  Group: map with null value
     * -------------------------------------------------------------- */
//     @Test
//     public void setConstructorCache_mapWithNullValue_isAccepted() throws Exception {
//         Map<Object, Constructor<?>> map = new HashMap<>();
//         map.put("nullValue", null);
//         ReflectionUtils.setConstructorCache(map);
//         Map<Object, Constructor<?>> cache = getInternalCache();
//         Assert.assertEquals(1, cache.size());
//         Assert.assertTrue(cache.containsKey("nullValue"));
//         Assert.assertNull(cache.get("nullValue"));
//     }

    /* --------------------------------------------------------------
     *  Group: thread‑safe map (ConcurrentHashMap)
     * -------------------------------------------------------------- */
//     @Test
//     public void setConstructorCache_threadSafeMap_isNotWrapped() throws Exception {
//         ConcurrentHashMap<Object, Constructor<?>> concurrent = new ConcurrentHashMap<>();
//         concurrent.put("concurrent", Integer.class.getDeclaredConstructor());
//         ReflectionUtils.setConstructorCache(concurrent);
//         Map<Object, Constructor<?>> cache = getInternalCache();
//         // The internal reference should be the exact same instance we supplied
//         Assert.assertSame(concurrent, cache);
//     }

    /* --------------------------------------------------------------
     *  Group: unmodifiable map
     * -------------------------------------------------------------- */
//     @Test
//     public void setConstructorCache_unmodifiableMap_isStoredAsIs() throws Exception {
//         Map<Object, Constructor<?>> modifiable = new HashMap<>();
//         modifiable.put("unmod", Boolean.class.getDeclaredConstructor());
//         Map<Object, Constructor<?>> unmodifiable = Collections.unmodifiableMap(modifiable);
//         ReflectionUtils.setConstructorCache(unmodifiable);
//         Map<Object, Constructor<?>> cache = getInternalCache();
//         Assert.assertSame(unmodifiable, cache);
//         Assert.assertEquals(Boolean.class.getDeclaredConstructor(), cache.get("unmod"));
//         // Verify that attempts to modify the stored map still throw
//         try {
//             cache.put("another", String.class.getDeclaredConstructor());
//             Assert.fail("Expected UnsupportedOperationException");
//         } catch (UnsupportedOperationException ignored) {
//         }
//     }

    /* --------------------------------------------------------------
     *  Group: replace existing cache
     * -------------------------------------------------------------- */
//     @Test
//     public void replaceExistingCache_initialPopulation() throws Exception {
//         Map<Object, Constructor<?>> first = new HashMap<>();
//         first.put("first", String.class.getDeclaredConstructor());
//         ReflectionUtils.setConstructorCache(first);
//         Map<Object, Constructor<?>> cache = getInternalCache();
//         Assert.assertEquals(1, cache.size());
//         Assert.assertSame(String.class.getDeclaredConstructor(), cache.get("first"));
//     }

//     @Test
//     public void replaceExistingCache_secondCallReplaces() throws Exception {
//         Map<Object, Constructor<?>> first = new HashMap<>();
//         first.put("first", String.class.getDeclaredConstructor());
//         ReflectionUtils.setConstructorCache(first);
//         Map<Object, Constructor<?>> second = new HashMap<>();
//         second.put("second", Integer.class.getDeclaredConstructor());
//         ReflectionUtils.setConstructorCache(second);
//         Map<Object, Constructor<?>> cache = getInternalCache();
//         Assert.assertEquals(1, cache.size());
//         Assert.assertNull(cache.get("first"));
//         Assert.assertSame(Integer.class.getDeclaredConstructor(), cache.get("second"));
//     }

    /* --------------------------------------------------------------
     *  Group: verify normal operation after cache replacement
     * -------------------------------------------------------------- */
//     @Test
//     public void testSetConstructorCacheWithEmptyMap() {
//         ReflectionUtils.setConstructorCache(new HashMap<>());
//         Constructor<?> ctor = ReflectionUtils.getConstructor(Sample.class);
//         Assert.assertNotNull(ctor);
//         Assert.assertEquals(Sample.class, ctor.getDeclaringClass());
//     }

//     @Test
//     public void testSetConstructorCacheReplacesCache() {
//         Constructor<?> originalCtor = ReflectionUtils.getConstructor(Sample.class);
//         Assert.assertNotNull(originalCtor);
//         Map<Object, Constructor<?>> customCache = new HashMap<>();
//         customCache.put(Sample.class, originalCtor);
//         ReflectionUtils.setConstructorCache(customCache);
//         Constructor<?> retrievedCtor = ReflectionUtils.getConstructor(Sample.class);
//         Assert.assertNotNull(retrievedCtor);
//         Assert.assertSame(originalCtor, retrievedCtor);
//     }

//     @Test
//     public void testCacheReplacementDoesNotBreakOtherLookups() throws Exception {
//         Constructor<?> sampleCtor = ReflectionUtils.getConstructor(Sample.class);
//         Assert.assertNotNull(sampleCtor);
//         Map<Object, Constructor<?>> unrelatedCache = new HashMap<>();
//         unrelatedCache.put(String.class, String.class.getDeclaredConstructor());
//         ReflectionUtils.setConstructorCache(unrelatedCache);
//         Constructor<?> intCtor = ReflectionUtils.getConstructor(Integer.class, int.class);
//         Assert.assertNotNull(intCtor);
//         Assert.assertEquals(Integer.class, intCtor.getDeclaringClass());
//     }
}