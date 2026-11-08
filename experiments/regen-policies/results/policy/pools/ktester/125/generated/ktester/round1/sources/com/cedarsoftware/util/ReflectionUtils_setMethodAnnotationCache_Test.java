package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setMethodAnnotationCache_Test {

    private static class DummyClass {

        @TestAnnotation
        public void annotatedMethod() {
        }

        public void plainMethod() {
        }

        @Deprecated
        public void deprecatedMethod() {
        }

        @Override
        public String toString() {
            return super.toString();
        }

        @SuppressWarnings("unchecked")
        public void suppressedMethod() {
        }
    }

    class RecordingMap extends AbstractMap<Object, Annotation> {

        private final Map<Object, Annotation> delegate = new HashMap<>();

        boolean putCalled = false;

        @Override
        public Set<Entry<Object, Annotation>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public Annotation put(Object key, Annotation value) {
            putCalled = true;
            return delegate.put(key, value);
        }
    }

    class RecordingMap extends AbstractMap<Object, Annotation> {

        private final Map<Object, Annotation> delegate = new HashMap<>();

        boolean putCalled = false;

        @Override
        public Set<Entry<Object, Annotation>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public Annotation put(Object key, Annotation value) {
            putCalled = true;
            return delegate.put(key, value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean cache before each test.
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the static cache to an empty map so that tests do not interfere.
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
    }

    /* --------------------------------------------------------------------- */
    private static Map<?, ?> getInternalCache() throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField("METHOD_ANNOTATION_CACHE");
        f.setAccessible(true);
        return (Map<?, ?>) f.get(null);
    }

    @Test
    void testSetMethodAnnotationCacheUpdatesCache() throws NoSuchMethodException {
        // GIVEN: a method that carries TestAnnotation and a map containing that entry
        Method method = DummyClass.class.getMethod("annotatedMethod");
        TestAnnotation annotation = method.getAnnotation(TestAnnotation.class);
        assertNotNull(annotation, "The method must be annotated for the test setup.");
        Map<Object, Annotation> customCache = new HashMap<>();
        customCache.put(method, annotation);
        // WHEN: we replace the internal cache with our custom map
        ReflectionUtils.setMethodAnnotationCache(customCache);
        // THEN: ReflectionUtils should retrieve the annotation from the cache
        TestAnnotation cachedAnnotation = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        assertNotNull(cachedAnnotation, "Annotation should be found in the custom cache.");
        assertSame(annotation, cachedAnnotation, "The cached annotation instance should be the one we supplied.");
    }

    @Test
    void testSetMethodAnnotationCacheOverridesPreviousCache() throws NoSuchMethodException {
        // GIVEN: a cache that contains an entry for a method
        Method method = DummyClass.class.getMethod("annotatedMethod");
        TestAnnotation annotation = method.getAnnotation(TestAnnotation.class);
        Map<Object, Annotation> firstCache = new HashMap<>();
        firstCache.put(method, annotation);
        ReflectionUtils.setMethodAnnotationCache(firstCache);
        // Verify the entry is present
        assertNotNull(ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class));
        // WHEN: we replace the cache with an empty map
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
        // THEN: the previously cached annotation should no longer be found
        TestAnnotation afterReset = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        assertNull(afterReset, "Cache should be empty after resetting with a new empty map.");
    }

    @Test
    void testSetMethodAnnotationCacheWithUnrelatedMethodDoesNotAffectOtherLookups() throws NoSuchMethodException {
        // GIVEN: a cache that contains an entry for a method that is NOT the one we will query
        Method annotatedMethod = DummyClass.class.getMethod("annotatedMethod");
        TestAnnotation annotation = annotatedMethod.getAnnotation(TestAnnotation.class);
        Map<Object, Annotation> cache = new HashMap<>();
        cache.put(annotatedMethod, annotation);
        ReflectionUtils.setMethodAnnotationCache(cache);
        // WHEN: we query a different method that has no annotation
        Method plainMethod = DummyClass.class.getMethod("plainMethod");
        TestAnnotation result = ReflectionUtils.getMethodAnnotation(plainMethod, TestAnnotation.class);
        // THEN: result should be null because the cache does not contain an entry for this method
        assertNull(result, "Method without a cached entry should return null.");
    }

    @Test
    void testSetMethodAnnotationCacheStoresProvidedHashMapWithObjectKey() throws Exception {
        // Prepare a map with a raw Object key and a @Deprecated annotation value.
        Method deprecatedMethod = DummyClass.class.getMethod("deprecatedMethod");
        Deprecated deprecatedAnno = deprecatedMethod.getAnnotation(Deprecated.class);
        Object key = new Object();
        Map<Object, Annotation> map = new HashMap<>();
        map.put(key, deprecatedAnno);
        ReflectionUtils.setMethodAnnotationCache(map);
        // Verify that the internal cache reference is exactly the map we supplied.
        assertSame(map, getInternalCache(), "The internal cache should reference the supplied HashMap instance.");
        assertEquals(1, getInternalCache().size(), "Cache size must reflect the provided entry.");
        assertSame(deprecatedAnno, getInternalCache().get(key), "Stored annotation must be the same instance.");
    }

    @Test
    void testSetMethodAnnotationCacheEmptyMap() throws Exception {
        Map<Object, Annotation> empty = new HashMap<>();
        ReflectionUtils.setMethodAnnotationCache(empty);
        assertSame(empty, getInternalCache(), "Empty map should replace the cache reference.");
        assertTrue(getInternalCache().isEmpty(), "Cache must be empty.");
    }

    @Test
    void testSetMethodAnnotationCacheAcceptsNull() throws Exception {
        // Passing null must not throw; the internal field becomes null.
        ReflectionUtils.setMethodAnnotationCache(null);
        assertNull(getInternalCache(), "Internal cache reference should be null after passing null.");
    }

    @Test
    void testSetMethodAnnotationCacheLinkedHashMapWithMismatchedGenerics() throws Exception {
        // LinkedHashMap<String, Annotation> – generic mismatch is ignored due to raw cast.
        Method overrideMethod = DummyClass.class.getMethod("toString");
        Override overrideAnno = overrideMethod.getAnnotation(Override.class);
        LinkedHashMap<String, Annotation> linked = new LinkedHashMap<>();
        linked.put("test", overrideAnno);
        // The method signature accepts Map<Object, Annotation>, so this compiles with raw types.
        @SuppressWarnings("unchecked")
        Map<Object, Annotation> rawMap = (Map) linked;
        ReflectionUtils.setMethodAnnotationCache(rawMap);
        assertSame(linked, getInternalCache(), "The cache should reference the provided LinkedHashMap instance.");
        assertEquals(1, getInternalCache().size());
        assertSame(overrideAnno, getInternalCache().get("test"));
    }

    @Test
    void testSetMethodAnnotationCacheConcurrentHashMap() throws Exception {
        ConcurrentHashMap<Object, Annotation> concurrent = new ConcurrentHashMap<>();
        Method deprecatedMethod = DummyClass.class.getMethod("deprecatedMethod");
        Deprecated depAnno = deprecatedMethod.getAnnotation(Deprecated.class);
        concurrent.put(new Object(), depAnno);
        Method overrideMethod = DummyClass.class.getMethod("toString");
        Override overAnno = overrideMethod.getAnnotation(Override.class);
        concurrent.put(new Object(), overAnno);
        Method suppressMethod = DummyClass.class.getMethod("suppressedMethod");
        SuppressWarnings suppressAnno = suppressMethod.getAnnotation(SuppressWarnings.class);
        concurrent.put(new Object(), suppressAnno);
        ReflectionUtils.setMethodAnnotationCache(concurrent);
        assertSame(concurrent, getInternalCache(), "ConcurrentHashMap instance must be stored as‑is.");
        assertEquals(3, getInternalCache().size(), "All three entries must be preserved.");
    }

    @Test
    void testSetMethodAnnotationCacheUnmodifiableMap() throws Exception {
        Map<Object, Annotation> mutable = new HashMap<>();
        Method deprecatedMethod = DummyClass.class.getMethod("deprecatedMethod");
        mutable.put(new Object(), deprecatedMethod.getAnnotation(Deprecated.class));
        Map<Object, Annotation> unmodifiable = Collections.unmodifiableMap(mutable);
        ReflectionUtils.setMethodAnnotationCache(unmodifiable);
        assertSame(unmodifiable, getInternalCache(), "Unmodifiable map must be stored without copying.");
        // Verify immutability – any attempt to modify should throw.
        assertThrows(UnsupportedOperationException.class, () -> getInternalCache().put(new Object(), null));
    }

    @Test
    void testSetMethodAnnotationCacheNullKey() throws Exception {
        Map<Object, Annotation> map = new HashMap<>();
        Method overrideMethod = DummyClass.class.getMethod("toString");
        map.put(null, overrideMethod.getAnnotation(Override.class));
        ReflectionUtils.setMethodAnnotationCache(map);
        assertSame(map, getInternalCache(), "Map with null key should be stored unchanged.");
        assertTrue(getInternalCache().containsKey(null), "Cache must contain the null key entry.");
    }

    @Test
    void testSetMethodAnnotationCacheNullValue() throws Exception {
        Map<Object, Annotation> map = new HashMap<>();
        Object key = new Object();
        // value representing “no annotation”
        map.put(key, null);
        ReflectionUtils.setMethodAnnotationCache(map);
        assertSame(map, getInternalCache(), "Map with null value should be stored unchanged.");
        assertTrue(getInternalCache().containsKey(key), "Cache must contain the key with null value.");
        assertNull(getInternalCache().get(key), "Stored value must be null.");
    }

    @Test
    void testSetMethodAnnotationCacheCustomMapDoesNotInvokePut() throws Exception {
        // Custom map that records whether put() is called.
        class RecordingMap extends AbstractMap<Object, Annotation> {

            private final Map<Object, Annotation> delegate = new HashMap<>();

            boolean putCalled = false;

            @Override
            public Set<Entry<Object, Annotation>> entrySet() {
                return delegate.entrySet();
            }

            @Override
            public Annotation put(Object key, Annotation value) {
                putCalled = true;
                return delegate.put(key, value);
            }
        }
        RecordingMap custom = new RecordingMap();
        // Pre‑populate with two entries directly via delegate (bypassing put)
        Method dep = DummyClass.class.getMethod("deprecatedMethod");
        Method over = DummyClass.class.getMethod("toString");
        custom.delegate.put(new Object(), dep.getAnnotation(Deprecated.class));
        custom.delegate.put(new Object(), over.getAnnotation(Override.class));
        ReflectionUtils.setMethodAnnotationCache(custom);
        assertSame(custom, getInternalCache(), "Custom map instance must be stored as‑is.");
        assertFalse(custom.putCalled, "setMethodAnnotationCache must not invoke put on the supplied map.");
        assertEquals(2, getInternalCache().size(), "Existing entries must be preserved.");
    }

    @Test
    void testSetMethodAnnotationCacheIdempotentWhenSameInstanceSupplied() throws Exception {
        Map<Object, Annotation> current = (Map<Object, Annotation>) getInternalCache();
        // Ensure the current cache is not null (it is the default LRUCache)
        assertNotNull(current, "Default cache should be non‑null before test.");
        // Call with the same instance
        ReflectionUtils.setMethodAnnotationCache(current);
        assertSame(current, getInternalCache(), "Supplying the same map instance must leave the reference unchanged.");
    }

    @Test
    void testSetMethodAnnotationCacheWithLRUCacheInstance() throws Exception {
        // Attempt to create an LRUCache instance via reflection.
        // The LRUCache class lives in the same package as ReflectionUtils.
        Class<?> lruClass;
        try {
            lruClass = Class.forName("com.cedarsoftware.util.LRUCache");
        } catch (ClassNotFoundException e) {
            // If LRUCache is not visible, skip this test gracefully.
            Assumptions.assumeTrue(false, "LRUCache class not found – skipping test.");
            return;
        }
        Constructor<?> ctor = lruClass.getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Annotation> lruCache = (Map<Object, Annotation>) ctor.newInstance(10);
        // Populate with five dummy entries.
        for (int i = 0; i < 5; i++) {
            lruCache.put(new Object(), null);
        }
        ReflectionUtils.setMethodAnnotationCache(lruCache);
        assertSame(lruCache, getInternalCache(), "Provided LRUCache instance must become the active cache.");
        assertEquals(5, getInternalCache().size(), "All pre‑populated entries must be retained.");
    }
}
