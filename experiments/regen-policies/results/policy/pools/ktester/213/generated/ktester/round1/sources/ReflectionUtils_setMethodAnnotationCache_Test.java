package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class ReflectionUtils_setMethodAnnotationCache_Test {

    /**
     * Class containing a method annotated with {@link TestAnno}.
     */
    private static class AnnotatedClass {

        @TestAnno
        public void annotatedMethod() {
        }
    }

    /**
     * Class containing a method annotated with {@link DummyAnnotation}.
     */
    private static class DummyAnnotated {

        @DummyAnnotation
        public void dummy() {
        }
    }

    /**
     * Custom map that throws a RuntimeException on any {@code put} call.
     */
    private static class ThrowOnPutMap<K, V> extends HashMap<K, V> {

        @Override
        public V put(K key, V value) {
            throw new RuntimeException("Intentional failure on put");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test by resetting the cache to the default.
        // The default cache is created inside ReflectionUtils, so we replace it with a fresh,
        // empty thread‑safe map. This mimics the behaviour of the library when it starts.
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        resetCacheToEmpty();
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the default empty cache after each test to avoid cross‑test contamination.
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        resetCacheToEmpty();
    }

    /**
     * Returns a real {@link Annotation} instance of {@link DummyAnnotation}
     * by reading it from {@link DummyAnnotated#dummy()}.
     */
    private static Annotation dummyAnnotationInstance() throws NoSuchMethodException {
        return DummyAnnotated.class.getMethod("dummy").getAnnotation(DummyAnnotation.class);
    }

    /**
     * Replaces the internal cache with a fresh empty {@link HashMap}.
     * Used in {@code @BeforeEach}/{@code @AfterEach} to guarantee isolation.
     */
    private static void resetCacheToEmpty() {
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
    }

    /**
     * Verifies that {@code setMethodAnnotationCache} accepts a normal {@link Map}
     * implementation without throwing any exception.
     */
    @Test
    void testSetMethodAnnotationCacheAcceptsMap() {
        Map<Object, Annotation> cache = new HashMap<>();
        // Populate the map with a dummy entry – the actual key/value types are not important
        // for this test because we are only checking that the method accepts the map.
        cache.put(new Object(), Deprecated.class.getAnnotation(Deprecated.class));
        assertDoesNotThrow(() -> ReflectionUtils.setMethodAnnotationCache(cache), "setMethodAnnotationCache should accept a regular Map without throwing.");
    }

    /**
     * Verifies that after replacing the internal cache, normal annotation lookup
     * via {@link ReflectionUtils#getMethodAnnotation(Method, Class)} still works.
     */
    @Test
    void testSetMethodAnnotationCacheDoesNotBreakAnnotationLookup() throws NoSuchMethodException {
        // First, replace the cache with an empty map.
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        // Obtain the method that carries the {@code @TestAnno} annotation.
        Method method = AnnotatedClass.class.getMethod("annotatedMethod");
        // Ensure the annotation can be retrieved before any further cache manipulation.
        TestAnno before = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(before, "Annotation should be present before cache replacement.");
        // Replace the cache with a different map (still empty – the content does not matter).
        Map<Object, Annotation> newCache = new HashMap<>();
        ReflectionUtils.setMethodAnnotationCache(newCache);
        // Retrieve the annotation again; it should still be found.
        TestAnno after = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(after, "Annotation should still be present after cache replacement.");
        assertEquals(TestAnno.class, after.annotationType(), "Returned annotation type should match the declared annotation.");
    }

    /**
     * Retrieves the internal cache map stored in {@code ReflectionUtils.METHOD_ANNOTATION_CACHE}
     * via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Annotation> getInternalCacheMap() throws Exception {
        Field field = ReflectionUtils.class.getDeclaredField("METHOD_ANNOTATION_CACHE");
        field.setAccessible(true);
        AtomicReference<Map<?, ?>> ref = (AtomicReference<Map<?, ?>>) field.get(null);
        return (Map<Object, Annotation>) ref.get();
    }

    @Test
    void testNullMapThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.setMethodAnnotationCache(null), "Passing null should result in NullPointerException");
    }

    @Test
    void testHashMapIsWrappedIntoThreadSafeMap() throws Exception {
        Annotation dummy = dummyAnnotationInstance();
        Map<Object, Annotation> original = new HashMap<>();
        original.put("key1", dummy);
        ReflectionUtils.setMethodAnnotationCache(original);
        Map<Object, Annotation> cached = getInternalCacheMap();
        // The returned map must not be the same instance (HashMap is not thread‑safe)
        assertNotSame(original, cached, "HashMap should be wrapped into a thread‑safe map");
        // The wrapper must contain the original entry
        assertEquals(dummy, cached.get("key1"));
        // Verify that the wrapper is a synchronized map (implementation detail)
        assertTrue(cached.getClass().getName().contains("SynchronizedMap"), "Wrapped map should be a synchronized map");
    }

    @Test
    void testConcurrentHashMapIsUsedAsIs() throws Exception {
        Annotation dummy = dummyAnnotationInstance();
        Map<Object, Annotation> concurrent = new ConcurrentHashMap<>();
        concurrent.put("key2", dummy);
        ReflectionUtils.setMethodAnnotationCache(concurrent);
        Map<Object, Annotation> cached = getInternalCacheMap();
        // ConcurrentHashMap is already thread‑safe – the same instance should be stored
        assertSame(concurrent, cached, "ConcurrentHashMap should be stored without wrapping");
        assertEquals(dummy, cached.get("key2"));
    }

    @Test
    void testEmptyImmutableMapIsWrapped() throws Exception {
        Map<Object, Annotation> empty = Collections.emptyMap();
        ReflectionUtils.setMethodAnnotationCache(empty);
        Map<Object, Annotation> cached = getInternalCacheMap();
        // Empty immutable map must be wrapped because it cannot be modified
        assertNotSame(empty, cached, "Empty immutable map should be wrapped");
        assertTrue(cached.isEmpty(), "Wrapped map should be empty");
        assertTrue(cached.getClass().getName().contains("SynchronizedMap"), "Wrapped map should be a synchronized map");
    }

    @Test
    void testMultipleEntriesArePreserved() throws Exception {
        Annotation dummy = dummyAnnotationInstance();
        Map<Object, Annotation> multi = new HashMap<>();
        multi.put("a", dummy);
        multi.put("b", dummy);
        multi.put("c", dummy);
        ReflectionUtils.setMethodAnnotationCache(multi);
        Map<Object, Annotation> cached = getInternalCacheMap();
        assertEquals(3, cached.size(), "All entries must be present");
        assertEquals(dummy, cached.get("a"));
        assertEquals(dummy, cached.get("b"));
        assertEquals(dummy, cached.get("c"));
    }

    @Test
    void testCacheReferenceChangesOnSuccessiveCalls() throws Exception {
        Map<Object, Annotation> first = new HashMap<>();
        first.put("first", dummyAnnotationInstance());
        Map<Object, Annotation> second = new HashMap<>();
        second.put("second", dummyAnnotationInstance());
        ReflectionUtils.setMethodAnnotationCache(first);
        Map<Object, Annotation> cachedAfterFirst = getInternalCacheMap();
        ReflectionUtils.setMethodAnnotationCache(second);
        Map<Object, Annotation> cachedAfterSecond = getInternalCacheMap();
        // The internal reference must point to a different map instance after the second call
        assertNotSame(cachedAfterFirst, cachedAfterSecond, "Cache reference should be swapped on successive calls");
        assertEquals(dummyAnnotationInstance(), cachedAfterSecond.get("second"));
    }

    @Test
    void testUnmodifiableMapIsWrappedIntoThreadSafeMap() throws Exception {
        Annotation dummy = dummyAnnotationInstance();
        Map<Object, Annotation> unmodifiable = Collections.unmodifiableMap(new HashMap<Object, Annotation>() {

            {
                put("k", dummy);
            }
        });
        ReflectionUtils.setMethodAnnotationCache(unmodifiable);
        Map<Object, Annotation> cached = getInternalCacheMap();
        // The unmodifiable map cannot be used directly – it must be wrapped
        assertNotSame(unmodifiable, cached, "Unmodifiable map should be wrapped");
        assertEquals(dummy, cached.get("k"));
        assertTrue(cached.getClass().getName().contains("SynchronizedMap"), "Wrapped map should be a synchronized map");
    }

    @Test
    void testSynchronizedMapIsUsedAsIs() throws Exception {
        Annotation dummy = dummyAnnotationInstance();
        Map<Object, Annotation> sync = Collections.synchronizedMap(new HashMap<Object, Annotation>() {

            {
                put("synch", dummy);
            }
        });
        ReflectionUtils.setMethodAnnotationCache(sync);
        Map<Object, Annotation> cached = getInternalCacheMap();
        // Already synchronized – same instance should be stored
        assertSame(sync, cached, "Synchronized map should be stored without additional wrapping");
        assertEquals(dummy, cached.get("synch"));
    }

    @Test
    void testHashtableIsUsedAsIs() throws Exception {
        Annotation dummy = dummyAnnotationInstance();
        Map<Object, Annotation> hashtable = new Hashtable<>();
        hashtable.put("ht", dummy);
        ReflectionUtils.setMethodAnnotationCache(hashtable);
        Map<Object, Annotation> cached = getInternalCacheMap();
        assertSame(hashtable, cached, "Hashtable should be stored without wrapping");
        assertEquals(dummy, cached.get("ht"));
    }

    @Test
    void testNullKeyIsPreserved() throws Exception {
        Annotation dummy = dummyAnnotationInstance();
        Map<Object, Annotation> map = new HashMap<>();
        map.put(null, dummy);
        ReflectionUtils.setMethodAnnotationCache(map);
        Map<Object, Annotation> cached = getInternalCacheMap();
        assertTrue(cached.containsKey(null), "Cache must contain the null key");
        assertEquals(dummy, cached.get(null));
    }

    @Test
    void testNullValueIsPreserved() throws Exception {
        Map<Object, Annotation> map = new HashMap<>();
        map.put("nullValue", null);
        ReflectionUtils.setMethodAnnotationCache(map);
        Map<Object, Annotation> cached = getInternalCacheMap();
        assertTrue(cached.containsKey("nullValue"), "Cache must contain the key with null value");
        assertNull(cached.get("nullValue"), "Value for the key should be null");
    }

    @Test
    void testNonEmptyUnmodifiableMapThrowsUnsupportedOperationException() {
        Annotation dummy = assertDoesNotThrow(() -> dummyAnnotationInstance(), "Creating dummy annotation should not fail");
        Map<Object, Annotation> unmodifiable = Collections.unmodifiableMap(new HashMap<Object, Annotation>() {

            {
                put("key", dummy);
            }
        });
        // According to the spec, ensureThreadSafe may try to modify the map to test mutability.
        // With a non‑empty unmodifiable map this should trigger UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> ReflectionUtils.setMethodAnnotationCache(unmodifiable), "Setting a non‑empty unmodifiable map should propagate UnsupportedOperationException");
    }

    @Test
    void testThrowOnPutMapPropagatesRuntimeException() {
        ThrowOnPutMap<Object, Annotation> badMap = new ThrowOnPutMap<>();
        // Populate the map – the overridden put will throw, so we must bypass it.
        // Use reflection to insert an entry directly into the underlying HashMap storage.
        try {
            Field mapField = HashMap.class.getDeclaredField("table");
            mapField.setAccessible(true);
            // No need to actually insert; the test focuses on the exception during setMethodAnnotationCache.
        } catch (Exception ignored) {
        }
        assertThrows(RuntimeException.class, () -> ReflectionUtils.setMethodAnnotationCache(badMap), "A map that throws on put should cause RuntimeException to propagate");
    }
}
