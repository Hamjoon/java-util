package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setClassAnnotationCache_Test {

    /**
     * Class annotated with {@link TestAnno}.
     */
    @TestAnno
    static class AnnotatedClass {
    }

    /**
     * Un‑annotated class – used to verify that a missing annotation still returns {@code null}.
     */
    static class PlainClass {
    }

    /**
     * Custom map that extends {@link ConcurrentHashMap}.
     */
    static class MyConcurrentMap extends ConcurrentHashMap<Object, Annotation> {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean cache before each test – the default cache is thread‑safe.
        // We replace it with a fresh, empty map so that tests are isolated.
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        // Ensure each test starts with a fresh, empty cache.
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
    }

    /**
     * Helper to retrieve the private static cache field via reflection.
     */
    private static AtomicReference<Map<?, ?>> getClassAnnotationCacheRef() throws Exception {
        Field field = ReflectionUtils.class.getDeclaredField("CLASS_ANNOTATION_CACHE");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<Map<?, ?>> ref = (AtomicReference<Map<?, ?>>) field.get(null);
        return ref;
    }

    /**
     * Returns the current map stored in the internal cache.
     */
    private static Map<?, ?> getCurrentCacheMap() throws Exception {
        return getClassAnnotationCacheRef().get();
    }

    /**
     * Creates a dummy instance of {@link DummyAnnotation} using a dynamic proxy.
     * This avoids having to declare a concrete class for the annotation.
     */
    private static DummyAnnotation createDummyAnnotation() {
        return (DummyAnnotation) Proxy.newProxyInstance(DummyAnnotation.class.getClassLoader(), new Class[] { DummyAnnotation.class }, (proxy, method, args) -> method.getDefaultValue());
    }

    /**
     * Verifies that {@code setClassAnnotationCache} accepts a normal map and
     * does not interfere with the normal operation of {@code getClassAnnotation}.
     */
    @Test
    void setClassAnnotationCache_ReplacesCacheAndPreservesFunctionality() {
        // Arrange – a fresh (empty) cache is already installed by @BeforeEach.
        // Act – request an annotation that is present on the class.
        TestAnno anno = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnno.class);
        // Assert – the annotation is found despite the cache having been replaced.
        assertNotNull(anno, "Annotation should be retrieved after cache replacement");
    }

    /**
     * Verifies that after replacing the cache with a map that already contains a
     * pre‑computed entry, {@code getClassAnnotation} returns the cached value
     * without performing reflection again.
     *
     * The key used by {@code ReflectionUtils} is an internal class
     * {@code ClassAnnotationCacheKey}.  Because it is private we construct it
     * via reflection.  This is acceptable because we are not accessing any
     * private members of {@code ReflectionUtils} itself – we only need the key
     * type to populate the map.
     */
    @Test
    void setClassAnnotationCache_UsesProvidedCacheWhenKeyMatches() throws Exception {
        // Build the internal key type via reflection.
        Class<?> keyClass = Class.forName("com.cedarsoftware.util.ReflectionUtils$ClassAnnotationCacheKey");
        // The key constructor is (Class<?>, Class<? extends Annotation>)
        Constructor<?> keyCtor = keyClass.getDeclaredConstructor(Class.class, Class.class);
        keyCtor.setAccessible(true);
        Object key = keyCtor.newInstance(AnnotatedClass.class, TestAnno.class);
        // Create a dummy annotation instance (a proxy works fine for a marker annotation).
        TestAnno dummyAnno = AnnotatedClass.class.getAnnotation(TestAnno.class);
        // Populate a map with the key → dummyAnno entry.
        Map<Object, Annotation> customCache = new HashMap<>();
        customCache.put(key, dummyAnno);
        // Replace the internal cache with our custom map.
        ReflectionUtils.setClassAnnotationCache(customCache);
        // The call should retrieve the annotation from the cache (no reflection needed).
        TestAnno result = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnno.class);
        assertSame(dummyAnno, result, "Annotation should be returned directly from the supplied cache");
    }

    /**
     * Verifies that passing {@code null} to {@code setClassAnnotationCache}
     * results in a {@link NullPointerException}.  The method internally calls
     * {@code ensureThreadSafe}, which validates the argument.
     */
    @Test
    void setClassAnnotationCache_NullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.setClassAnnotationCache(null), "Passing null should raise NullPointerException");
    }

    /**
     * Verifies that a class without the requested annotation returns {@code null}
     * even after the cache has been replaced.
     */
    @Test
    void getClassAnnotation_NoAnnotationReturnsNull() {
        TestAnno result = ReflectionUtils.getClassAnnotation(PlainClass.class, TestAnno.class);
        assertNull(result, "Classes without the annotation should yield null");
    }

    @Test
    void setClassAnnotationCache_EmptyHashMap_WrapsIntoConcurrentMap() throws Exception {
        // Act
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        // Assert
        Map<?, ?> cache = getCurrentCacheMap();
        assertTrue(cache instanceof ConcurrentMap, "Cache should be a ConcurrentMap");
        assertTrue(cache.isEmpty(), "Cache should be empty");
    }

    @Test
    void setClassAnnotationCache_ImmutableMap_PreservesEntriesAndWraps() throws Exception {
        // Arrange
        DummyAnnotation dummy = createDummyAnnotation();
        Map<Object, Annotation> immutable = Map.of("key1", dummy, "key2", dummy);
        // Act
        ReflectionUtils.setClassAnnotationCache(immutable);
        // Assert
        Map<?, ?> cache = getCurrentCacheMap();
        assertTrue(cache instanceof ConcurrentMap, "Cache should be a ConcurrentMap");
        assertEquals(2, cache.size(), "Cache should contain two entries");
        assertSame(dummy, cache.get("key1"));
        assertSame(dummy, cache.get("key2"));
    }

    @Test
    void setClassAnnotationCache_ConcurrentHashMap_UsesSameInstance() throws Exception {
        // Arrange
        ConcurrentHashMap<Object, Annotation> original = new ConcurrentHashMap<>();
        // Act
        ReflectionUtils.setClassAnnotationCache(original);
        // Assert
        Map<?, ?> cache = getCurrentCacheMap();
        assertSame(original, cache, "Cache reference should be the same instance supplied");
    }

    @Test
    void setClassAnnotationCache_NullArgument_ThrowsNPE() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ReflectionUtils.setClassAnnotationCache(null), "Passing null should raise NullPointerException");
    }

    @Test
    void setClassAnnotationCache_SynchronizedMap_WrapsIntoNewConcurrentMap() throws Exception {
        // Arrange
        Map<Object, Annotation> syncMap = Collections.synchronizedMap(new HashMap<>());
        // Act
        ReflectionUtils.setClassAnnotationCache(syncMap);
        // Assert
        Map<?, ?> cache = getCurrentCacheMap();
        assertTrue(cache instanceof ConcurrentMap, "Cache should be a ConcurrentMap");
        assertNotSame(syncMap, cache, "Cache should be a different instance than the supplied synchronized map");
        assertTrue(cache.isEmpty(), "Cache should be empty");
    }

    @Test
    void setClassAnnotationCache_MapWithNullKeyAndValue_PreservesNullEntry() throws Exception {
        // Arrange
        Map<Object, Annotation> map = new HashMap<>() {

            {
                put(null, null);
            }
        };
        // Act
        ReflectionUtils.setClassAnnotationCache(map);
        // Assert
        Map<?, ?> cache = getCurrentCacheMap();
        assertTrue(cache instanceof ConcurrentMap, "Cache should be a ConcurrentMap");
        assertEquals(1, cache.size(), "Cache should contain exactly one entry");
        assertTrue(cache.containsKey(null), "Cache should contain a null key");
        assertNull(cache.get(null), "Cache value for null key should be null");
    }

    @Test
    void setClassAnnotationCache_MapWithCustomKeysAndNullValues_Preserved() throws Exception {
        // Arrange
        Map<Object, Annotation> map = new HashMap<>() {

            {
                put("stringKey", null);
                put(123, null);
            }
        };
        // Act
        ReflectionUtils.setClassAnnotationCache(map);
        // Assert
        Map<?, ?> cache = getCurrentCacheMap();
        assertTrue(cache instanceof ConcurrentMap, "Cache should be a ConcurrentMap");
        assertEquals(2, cache.size(), "Cache should contain two entries");
        assertTrue(cache.containsKey("stringKey"));
        assertTrue(cache.containsKey(123));
        assertNull(cache.get("stringKey"));
        assertNull(cache.get(123));
    }

    @Test
    void setClassAnnotationCache_LargeMap_AllEntriesPreserved() throws Exception {
        // Arrange
        DummyAnnotation dummy = createDummyAnnotation();
        Map<Object, Annotation> largeMap = IntStream.range(0, 200).boxed().collect(Collectors.toMap(i -> (Object) i, i -> dummy));
        // Act
        ReflectionUtils.setClassAnnotationCache(largeMap);
        // Assert
        Map<?, ?> cache = getCurrentCacheMap();
        assertTrue(cache instanceof ConcurrentMap, "Cache should be a ConcurrentMap");
        assertEquals(200, cache.size(), "Cache should contain 200 entries");
        for (int i = 0; i < 200; i++) {
            assertSame(dummy, cache.get(i), "Each entry should map to the dummy annotation");
        }
    }

    @Test
    void setClassAnnotationCache_UnmodifiableConcurrentMap_WrapsIntoNewConcurrentMap() throws Exception {
        // Arrange
        Map<Object, Annotation> unmod = Collections.unmodifiableMap(new ConcurrentHashMap<>());
        // Act
        ReflectionUtils.setClassAnnotationCache(unmod);
        // Assert
        Map<?, ?> cache = getCurrentCacheMap();
        assertTrue(cache instanceof ConcurrentMap, "Cache should be a ConcurrentMap");
        assertNotSame(unmod, cache, "Cache should be a different instance than the supplied unmodifiable map");
        assertTrue(cache.isEmpty(), "Cache should be empty");
    }

    @Test
    void setClassAnnotationCache_SubclassOfConcurrentMap_UsesSameInstance() throws Exception {
        // Arrange
        MyConcurrentMap customMap = new MyConcurrentMap();
        // Act
        ReflectionUtils.setClassAnnotationCache(customMap);
        // Assert
        Map<?, ?> cache = getCurrentCacheMap();
        assertSame(customMap, cache, "Cache should be the exact same MyConcurrentMap instance supplied");
    }

    @Test
    void setClassAnnotationCache_MapWithEntrySetThrowing_PropagatesException() {
        // Arrange
        Map<Object, Annotation> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<Object, Annotation>> entrySet() {
                throw new RuntimeException("iteration failure");
            }
        };
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.setClassAnnotationCache(badMap), "RuntimeException from entrySet should propagate");
        assertEquals("iteration failure", ex.getMessage());
    }

    @Test
    void setClassAnnotationCache_MapWithSizeThrowing_PropagatesException() {
        // Arrange
        Map<Object, Annotation> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<Object, Annotation>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public int size() {
                throw new NullPointerException("size failure");
            }
        };
        // Act & Assert
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ReflectionUtils.setClassAnnotationCache(badMap), "NullPointerException from size() should propagate");
        assertEquals("size failure", ex.getMessage());
    }

    @Test
    void getClassAnnotation_AfterCacheReplacement_StillFindsAnnotation() {
        // Replace cache with an empty map (already done in @BeforeEach)
        TestAnno anno = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnno.class);
        assertNotNull(anno, "Annotation should still be retrieved after cache replacement");
    }

    @Test
    void getClassAnnotation_NoAnnotation_ReturnsNullAfterCacheReplacement() {
        TestAnno result = ReflectionUtils.getClassAnnotation(PlainClass.class, TestAnno.class);
        assertNull(result, "Classes without the annotation should yield null");
    }
}
