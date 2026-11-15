package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setMethodAnnotationCache_Test {

    static class Sample {

        @TestAnnotation
        public void annotatedMethod() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean cache before each test – we reset it to an empty map.
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the default (empty) cache after each test.
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
    }

    @Test
    void testSetMethodAnnotationCacheWithEmptyMap() throws NoSuchMethodException {
        // GIVEN an empty cache map
        Map<Object, Annotation> emptyCache = new HashMap<>();
        // WHEN we replace the internal cache with the empty map
        ReflectionUtils.setMethodAnnotationCache(emptyCache);
        // AND we request an annotation that is present on a method
        Method m = ReflectionUtils.getMethod(Sample.class, "annotatedMethod");
        Annotation ann = ReflectionUtils.getMethodAnnotation(m, TestAnnotation.class);
        // THEN the annotation should still be resolved correctly (the utility
        // falls back to reflection when the cache does not contain the entry)
        assertNotNull(ann, "Annotation should be found even after cache replacement");
        assertTrue(ann instanceof TestAnnotation);
    }

    @Test
    void testSetMethodAnnotationCacheWithSpiedMap() throws NoSuchMethodException {
        // GIVEN a spied map that we can verify interactions on
        @SuppressWarnings("unchecked")
        Map<Object, Annotation> spyCache = spy(new HashMap<>());
        // WHEN we install the spied cache
        ReflectionUtils.setMethodAnnotationCache(spyCache);
        // AND we request the annotation for a method
        Method m = ReflectionUtils.getMethod(Sample.class, "annotatedMethod");
        Annotation ann = ReflectionUtils.getMethodAnnotation(m, TestAnnotation.class);
        // THEN the annotation must be returned
        assertNotNull(ann, "Annotation should be retrieved");
        assertTrue(ann instanceof TestAnnotation);
        // AND the cache should have been used to store the result.
        // We cannot know the exact key type, but we can verify that a put()
        // operation occurred with the same annotation instance we just obtained.
        verify(spyCache, atLeastOnce()).put(any(), eq(ann));
    }

    @Test
    void testSetMethodAnnotationCacheWithNullMap() throws NoSuchMethodException {
        // GIVEN a null map (the method itself does not guard against null)
        // WHEN we set the cache to null, we expect a NullPointerException
        // on the next cache‑using operation.
        ReflectionUtils.setMethodAnnotationCache(null);
        Method m = ReflectionUtils.getMethod(Sample.class, "annotatedMethod");
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethodAnnotation(m, TestAnnotation.class));
    }

    /* --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static Map<Object, Annotation> getInternalCache() {
        try {
            Field f = ReflectionUtils.class.getDeclaredField("METHOD_ANNOTATION_CACHE");
            f.setAccessible(true);
            return (Map<Object, Annotation>) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Group: replaceInternalCacheWithNewMap **
     */
    @Test
    void replaceInternalCacheWithNewMap_emptyHashMap() {
        Map<Object, Annotation> freshMap = new HashMap<>();
        ReflectionUtils.setMethodAnnotationCache(freshMap);
        // The internal cache should be exactly the same instance we passed.
        assertSame(freshMap, getInternalCache(), "Internal cache must reference the provided empty HashMap instance");
    }

    @Test
    void replaceInternalCacheWithNewMap_immutableEmptyMap() {
        Map<Object, Annotation> immutableEmpty = Collections.emptyMap();
        ReflectionUtils.setMethodAnnotationCache(immutableEmpty);
        assertSame(immutableEmpty, getInternalCache(), "Internal cache must reference the provided immutable empty map instance");
    }

    @Test
    void replaceInternalCacheWithNewMap_nullMap() {
        ReflectionUtils.setMethodAnnotationCache(null);
        assertNull(getInternalCache(), "Internal cache field should become null after passing null");
    }

    /**
     * Group: cacheRetainsProvidedEntries **
     */
    @Test
    void cacheRetainsProvidedEntries_normalEntry() throws NoSuchMethodException {
        // Obtain a real annotation instance to store as value.
        Method method = ReflectionUtils.getMethod(Sample.class, "annotatedMethod");
        Annotation testAnno = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        Map<Object, Annotation> customMap = new HashMap<>();
        String dummyKey = "dummyKey";
        customMap.put(dummyKey, testAnno);
        ReflectionUtils.setMethodAnnotationCache(customMap);
        Map<Object, Annotation> internal = getInternalCache();
        assertTrue(internal.containsKey(dummyKey), "Cache must contain the dummy key");
        assertSame(testAnno, internal.get(dummyKey), "Cache must contain the exact annotation instance");
    }

    @Test
    void cacheRetainsProvidedEntries_nullKeyAndValue() {
        Map<Object, Annotation> mapWithNulls = new HashMap<>();
        mapWithNulls.put(null, null);
        ReflectionUtils.setMethodAnnotationCache(mapWithNulls);
        Map<Object, Annotation> internal = getInternalCache();
        assertTrue(internal.containsKey(null), "Cache should contain a null key");
        assertNull(internal.get(null), "Cache should map null key to null value");
    }

    /**
     * Group: cacheReflectsSubsequentModificationsToOriginalMap **
     */
    @Test
    void cacheReflectsSubsequentModificationsToOriginalMap() throws NoSuchMethodException {
        Map<Object, Annotation> original = new HashMap<>();
        ReflectionUtils.setMethodAnnotationCache(original);
        // modify after setting
        Method m = ReflectionUtils.getMethod(Sample.class, "annotatedMethod");
        Annotation ann = ReflectionUtils.getMethodAnnotation(m, TestAnnotation.class);
        original.put("laterKey", ann);
        Map<Object, Annotation> internal = getInternalCache();
        assertTrue(internal.containsKey("laterKey"), "Internal cache must reflect modifications made to the original map");
        assertSame(ann, internal.get("laterKey"));
    }

    /**
     * Group: acceptsUnmodifiableMapImplementation **
     */
    @Test
    void acceptsUnmodifiableMapImplementation() {
        Map<Object, Annotation> mutable = new HashMap<>();
        Map<Object, Annotation> unmodifiable = Collections.unmodifiableMap(mutable);
        ReflectionUtils.setMethodAnnotationCache(unmodifiable);
        assertSame(unmodifiable, getInternalCache(), "Internal cache should reference the provided unmodifiable map instance");
    }

    /**
     * Group: typeErasureAndRawCastBehaviour **
     */
    @Test
    void typeErasureAndRawCastBehaviour() {
        // Create a generic Map<String,String> and then cast to raw Map
        Map<String, String> genericMap = new HashMap<>();
        genericMap.put("k", "v");
        @SuppressWarnings("unchecked")
        Map // raw type
        rawMap = (Map) genericMap;
        // The method signature accepts Map<Object,Annotation> but performs a raw cast,
        // so this should be accepted without ClassCastException.
        ReflectionUtils.setMethodAnnotationCache(rawMap);
        assertSame(rawMap, getInternalCache(), "Internal cache must be the exact raw map instance supplied");
    }
}
