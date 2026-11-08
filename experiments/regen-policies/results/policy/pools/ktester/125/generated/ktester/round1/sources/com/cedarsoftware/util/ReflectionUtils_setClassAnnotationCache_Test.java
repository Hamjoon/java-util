package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
// import org.junit.runners.Parameterized;
import org.mockito.junit.MockitoJUnitRunner;
// import org.junit.*;
// import org.junit.*;
// import org.junit.runner.RunWith;
// import org.junit.runner.RunWith;
// import static org.junit.Assert.*;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class ReflectionUtils_setClassAnnotationCache_Test {

    /**
     * Holds the original cache so we can restore it after each test.
     */
    private static Map<?, ?> originalCache;

    /**
     * Name of the private static field that stores the cache.
     */
    private static final String CACHE_FIELD_NAME = "CLASS_ANNOTATION_CACHE";

    //     @BeforeAll
    static void setupBeforeAll() {
        originalCache = getCurrentCache();
    }

    //     @AfterEach
    void teardownAfterEach() {
        restoreOriginalCache();
    }

    //     @AfterAll
    static void teardownAfterAll() {
        restoreOriginalCache();
    }

    /**
     * Restores the original cache that was present before the test suite started.
     */
    private static void restoreOriginalCache() {
        try {
            Field f = ReflectionUtils.class.getDeclaredField(CACHE_FIELD_NAME);
            f.setAccessible(true);
            f.set(null, originalCache);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to restore ReflectionUtils cache field", e);
        }
    }

    /**
     * Creates a minimal {@link Annotation} instance for the given annotation type.
     * The returned proxy only implements {@link Annotation#annotationType()}.
     */
    private static Annotation createAnnotation(Class<? extends Annotation> annoClass) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("annotationType".equals(method.getName())) {
                return annoClass;
            }
            // Default handling for Object methods (toString, hashCode, equals)
            return method.invoke(proxy, args);
        };
        @SuppressWarnings("unchecked")
        Annotation proxy = (Annotation) Proxy.newProxyInstance(annoClass.getClassLoader(), new Class[] { annoClass }, handler);
        return proxy;
    }

    /**
     * Retrieves the private static {@code CLASS_ANNOTATION_CACHE} field via reflection.
     *
     * @return the current cache instance (may be {@code null})
     * @throws RuntimeException if reflection fails
     */
    @SuppressWarnings("unchecked")
    private static Map<?, ?> getCurrentCache() {
        try {
            Field f = ReflectionUtils.class.getDeclaredField(CACHE_FIELD_NAME);
            f.setAccessible(true);
            return (Map<?, ?>) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to access ReflectionUtils cache field", e);
        }
    }

//     @Test
//     public void testSetClassAnnotationCacheReplacesCache() {
//         // GIVEN – a brand‑new empty cache
//         Map<Object, Annotation> newCache = new HashMap<>();
//         // WHEN – we set the cache via the public API
//         ReflectionUtils.setClassAnnotationCache(newCache);
//         // THEN – the internal static field should reference the same map instance
//         Map<?, ?> currentCache = getCurrentCache();
//         assertSame("ReflectionUtils should store the exact map instance supplied to setClassAnnotationCache", newCache, currentCache);
//     }

//     @Test
//     public void testSetClassAnnotationCacheCanBeSetToNull() {
//         // GIVEN – a null reference
//         Map<Object, Annotation> nullCache = null;
//         // WHEN – we set the cache to null
//         ReflectionUtils.setClassAnnotationCache(nullCache);
//         // THEN – the internal field should now be null
//         Map<?, ?> currentCache = getCurrentCache();
//         assertNull("Cache should be null after setting a null map", currentCache);
//     }

//     @Test
//     public void testCacheIsRestoredBetweenTests() {
//         // The @After method restores the original cache, so this test verifies that behaviour.
//         // GIVEN – the cache is restored to the original value
//         Map<?, ?> restoredCache = getCurrentCache();
//         // THEN – it must be equal to the snapshot taken in @BeforeClass
//         assertSame("Cache after @After should be the same instance captured before any test ran", originalCache, restoredCache);
//     }

    /**
     * Basic functionality – immutable empty map
     */
//     @Test
//     public void testSetClassAnnotationCacheWithEmptyImmutableMap() {
//         Map<Object, Annotation> emptyMap = Collections.emptyMap();
//         ReflectionUtils.setClassAnnotationCache(emptyMap);
//         Map<?, ?> current = getCurrentCache();
//         assertSame("The empty immutable map should be stored as‑is", emptyMap, current);
//         // Verify that attempts to modify through the cached reference fail.
//         try {
//             @SuppressWarnings("unchecked")
//             Map<Object, Annotation> mod = (Map<Object, Annotation>) current;
//             mod.put("any", createAnnotation(Deprecated.class));
//             fail("Modifying the immutable map should throw UnsupportedOperationException");
//         } catch (UnsupportedOperationException ignored) {
//             // expected
//         }
//     }

//     @Test
//     public void testSetClassAnnotationCacheAcceptsMapWithNullKey() {
//         Map<Object, Annotation> map = new HashMap<>();
//         map.put(null, createAnnotation(Override.class));
//         ReflectionUtils.setClassAnnotationCache(map);
//         Map<?, ?> current = getCurrentCache();
//         assertSame("Cache should store map containing a null key", map, current);
//         assertTrue("Cache must contain the null key", current.containsKey(null));
//     }

//     @Test
//     public void testSetClassAnnotationCacheAcceptsMapWithNullValue() {
//         Map<Object, Annotation> map = new HashMap<>();
//         map.put("nullValueKey", null);
//         ReflectionUtils.setClassAnnotationCache(map);
//         Map<?, ?> current = getCurrentCache();
//         assertSame("Cache should store map containing a null value", map, current);
//         assertTrue(current.containsKey("nullValueKey"));
//         assertNull(current.get("nullValueKey"));
//     }

//     @Test
//     public void testModificationsToOriginalMapAreSeenThroughCacheReference() {
//         Map<Object, Annotation> original = new HashMap<>();
//         ReflectionUtils.setClassAnnotationCache(original);
//         // Modify the original map after setting it as cache
//         original.put("newKey", createAnnotation(Deprecated.class));
//         Map<?, ?> cached = getCurrentCache();
//         assertSame("Cache should still reference the original map", original, cached);
//         assertTrue(cached.containsKey("newKey"));
//         assertEquals("The annotation stored via original map must be visible through the cache", createAnnotation(Deprecated.class).annotationType(), ((Annotation) cached.get("newKey")).annotationType());
//     }

//     @Test
//     public void testSuccessiveCallsReplaceCacheReference() {
//         Map<Object, Annotation> first = new HashMap<>();
//         first.put("first", createAnnotation(Override.class));
//         Map<Object, Annotation> second = new HashMap<>();
//         second.put("second", createAnnotation(SuppressWarnings.class));
//         ReflectionUtils.setClassAnnotationCache(first);
//         Map<?, ?> afterFirst = getCurrentCache();
//         assertSame("First cache instance should be stored", first, afterFirst);
//         ReflectionUtils.setClassAnnotationCache(second);
//         Map<?, ?> afterSecond = getCurrentCache();
//         assertSame("Second cache instance should replace the first", second, afterSecond);
//         assertNotSame("References must differ after replacement", first, afterSecond);
//     }

//     @Test
//     public void testSetClassAnnotationCacheDoesNotInvokeMapMethods() {
//         // Custom map that throws on any mutating call
//         Map<Object, Annotation> hostile = new HashMap<Object, Annotation>() {
// 
//             @Override
//             public Annotation put(Object key, Annotation value) {
//                 throw new UnsupportedOperationException("put should not be called by setClassAnnotationCache");
//             }
//         };
//         // No entries are added; we just want to ensure setClassAnnotationCache doesn't call put().
//         ReflectionUtils.setClassAnnotationCache(hostile);
//         Map<?, ?> current = getCurrentCache();
//         assertSame("Cache should store the hostile map without invoking its put method", hostile, current);
//     }

    /**
     * Helper that creates a few different map implementations for manual testing.
     * (The original parameterised test was removed because JUnit 4 parameterisation
     * would add unnecessary complexity for this kata.)
     */
//     @Test
//     public void testSetClassAnnotationCacheWithVariousMapsManually() {
//         // ConcurrentHashMap
//         Map<Object, Annotation> chm = new ConcurrentHashMap<>();
//         chm.put("concurrentKey", createAnnotation(Override.class));
//         ReflectionUtils.setClassAnnotationCache(chm);
//         assertSame(chm, getCurrentCache());
//         // TreeMap with a comparator that can compare any objects (by their string representation)
//         Map<Object, Annotation> tree = new TreeMap<>(Comparator.comparing(Object::toString));
//         tree.put("treeKey", createAnnotation(SuppressWarnings.class));
//         ReflectionUtils.setClassAnnotationCache(tree);
//         assertSame(tree, getCurrentCache());
//         // Synchronized HashMap wrapper
//         Map<Object, Annotation> sync = Collections.synchronizedMap(new HashMap<>());
//         sync.put("syncKey", createAnnotation(Deprecated.class));
//         ReflectionUtils.setClassAnnotationCache(sync);
//         assertSame(sync, getCurrentCache());
//         // Unmodifiable map
//         Map<Object, Annotation> unmodInner = new HashMap<>();
//         unmodInner.put("unmodKey", createAnnotation(Override.class));
//         Map<Object, Annotation> unmod = Collections.unmodifiableMap(unmodInner);
//         ReflectionUtils.setClassAnnotationCache(unmod);
//         assertSame(unmod, getCurrentCache());
//         // LRUCache (public class in the same package)
//         LRUCache<Object, Annotation> lru = new LRUCache<>(5);
//         lru.put("lruKey", createAnnotation(Deprecated.class));
//         ReflectionUtils.setClassAnnotationCache(lru);
//         assertSame(lru, getCurrentCache());
//     }
}