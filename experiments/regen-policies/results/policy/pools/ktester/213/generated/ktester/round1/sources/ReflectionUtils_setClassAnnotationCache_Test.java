package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setClassAnnotationCache_Test {

    /**
     * Class annotated with {@link TestAnnotation}.
     */
    @TestAnnotation
    private static class AnnotatedClass {
    }

    /**
     * Before each test we clear the internal cache by installing an empty,
     * thread‑safe map.  This guarantees that each test runs with a clean
     * state and that the cache modifications performed by a test do not
     * affect the others.
     */
    @BeforeEach
    void setupBeforeEach() {
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
    }

    /**
     * After each test we also clear the cache – a safety net in case a test
     * forgets to restore the original state.
     */
    @AfterEach
    void teardownAfterEach() {
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
    }

    /**
     * Verifies that {@code setClassAnnotationCache} stores the supplied map
     * and that {@link ReflectionUtils#getClassAnnotation(Class, Class)} can
     * retrieve an annotation from that cache without performing reflection.
     */
    @Test
    void testSetClassAnnotationCacheUpdatesCache() {
        // --- given ---------------------------------------------------------
        // Build a map that mimics the internal cache: key = class, value = annotation
        Map<Object, Annotation> customCache = new HashMap<>();
        Annotation expected = AnnotatedClass.class.getAnnotation(TestAnnotation.class);
        assertNotNull(expected, "Test setup failed – annotation should be present on AnnotatedClass");
        customCache.put(AnnotatedClass.class, expected);
        // --- when ----------------------------------------------------------
        ReflectionUtils.setClassAnnotationCache(customCache);
        // --- then ----------------------------------------------------------
        // The annotation should now be returned directly from the cache.
        Annotation actual = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnnotation.class);
        assertNotNull(actual, "Annotation should be retrieved from the custom cache");
        assertSame(expected, actual, "The annotation instance returned must be the exact one stored in the cache");
    }

    /**
     * Ensures that calling {@code setClassAnnotationCache} with an empty map
     * results in {@code getClassAnnotation} returning {@code null} for a class
     * that has the annotation (i.e., the cache no longer contains the entry).
     */
    @Test
    void testCacheClearedWhenEmptyMapProvided() {
        // --- given ---------------------------------------------------------
        // First install a cache that contains the annotation.
        Map<Object, Annotation> populated = new HashMap<>();
        populated.put(AnnotatedClass.class, AnnotatedClass.class.getAnnotation(TestAnnotation.class));
        ReflectionUtils.setClassAnnotationCache(populated);
        // Verify the annotation is reachable.
        assertNotNull(ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnnotation.class));
        // --- when ----------------------------------------------------------
        // Replace the cache with an empty one.
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        // --- then ----------------------------------------------------------
        // The annotation should no longer be found in the cache.
        Annotation result = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnnotation.class);
        assertNull(result, "With an empty cache the annotation should not be found");
    }

    /* --------------------------------------------------------------
     * Helper to obtain the internal map stored in CLASS_ANNOTATION_CACHE.
     * -------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static Map<Object, Annotation> getInternalCacheMap() throws Exception {
        Field field = ReflectionUtils.class.getDeclaredField("CLASS_ANNOTATION_CACHE");
        field.setAccessible(true);
        AtomicReference<Map<? super Object, Annotation>> ref = (AtomicReference<Map<? super Object, Annotation>>) field.get(null);
        return (Map<Object, Annotation>) ref.get();
    }

    /* --------------------------------------------------------------
     * Basic replacement – mutable map should be wrapped in a thread‑safe
     * implementation (Collections.synchronizedMap) and retain its entries.
     * -------------------------------------------------------------- */
    @Test
    void testSetClassAnnotationCacheWrapsMutableMap() throws Exception {
        // --- given ---------------------------------------------------------
        Annotation testAnno = AnnotatedClass.class.getAnnotation(TestAnnotation.class);
        assertNotNull(testAnno, "Test setup failed – annotation should be present");
        Map<Object, Annotation> mutable = new HashMap<>();
        mutable.put("key1", testAnno);
        mutable.put("key2", testAnno);
        // --- when ----------------------------------------------------------
        ReflectionUtils.setClassAnnotationCache(mutable);
        // --- then ----------------------------------------------------------
        Map<Object, Annotation> internal = getInternalCacheMap();
        // The internal map must NOT be the same instance we passed.
        assertNotSame(mutable, internal, "A mutable map must be wrapped; the stored map should be a different instance.");
        // The wrapper should be a synchronized map (implementation class name contains "SynchronizedMap").
        assertTrue(internal.getClass().getName().contains("SynchronizedMap"), "The wrapper should be a synchronized map, e.g., Collections$SynchronizedMap.");
        // All entries must be present.
        assertEquals(2, internal.size(), "Wrapped map must contain the same number of entries.");
        assertEquals(testAnno, internal.get("key1"));
        assertEquals(testAnno, internal.get("key2"));
    }

    /* --------------------------------------------------------------
     * When a thread‑safe map (ConcurrentHashMap) is supplied, the method
     * must store the exact same instance without additional wrapping.
     * -------------------------------------------------------------- */
    @Test
    void testSetClassAnnotationCacheAcceptsConcurrentMap() throws Exception {
        // --- given ---------------------------------------------------------
        Annotation testAnno = AnnotatedClass.class.getAnnotation(TestAnnotation.class);
        Map<Object, Annotation> concurrent = new ConcurrentHashMap<>();
        concurrent.put("cKey", testAnno);
        // --- when ----------------------------------------------------------
        ReflectionUtils.setClassAnnotationCache(concurrent);
        // --- then ----------------------------------------------------------
        Map<Object, Annotation> internal = getInternalCacheMap();
        // The stored map must be the exact same instance we supplied.
        assertSame(concurrent, internal, "When a thread‑safe map is supplied, it should be stored without wrapping.");
    }

    @Test
    void testSetClassAnnotationCacheRejectsNullMap() {
        // --- when ----------------------------------------------------------
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ReflectionUtils.setClassAnnotationCache(null), "Passing null should result in NullPointerException.");
        // Optional: verify message (implementation‑specific)
        assertNotNull(ex.getMessage());
    }

    @Test
    void testSetClassAnnotationCacheRejectsNullKey() {
        // --- given ---------------------------------------------------------
        Annotation testAnno = AnnotatedClass.class.getAnnotation(TestAnnotation.class);
        Map<Object, Annotation> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, testAnno);
        // --- when ----------------------------------------------------------
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.setClassAnnotationCache(mapWithNullKey), "Map containing a null key should cause IllegalArgumentException.");
        // Optional: verify message
        assertNotNull(ex.getMessage());
    }

    @Test
    void testSetClassAnnotationCacheRejectsNullValue() {
        // --- given ---------------------------------------------------------
        Map<Object, Annotation> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("validKey", null);
        // --- when ----------------------------------------------------------
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.setClassAnnotationCache(mapWithNullValue), "Map containing a null value should cause IllegalArgumentException.");
        // Optional: verify message
        assertNotNull(ex.getMessage());
    }
}
