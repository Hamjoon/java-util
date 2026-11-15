package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setConstructorCache_Test {

    /**
     * Simple class used for constructor‑cache testing.
     */
    private static class DummyClass {

        public DummyClass() {
        }
    }

    /**
     * Before each test we reset the internal cache to a known empty state.
     * This prevents interference between the individual test cases.
     */
    @BeforeEach
    void setupBeforeEach() {
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    /**
     * No special per‑test teardown required.
     */
    @AfterEach
    void teardownAfterEach() {
        // restore a clean cache so later tests (or other test classes) are not polluted
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    @Test
    void testSetConstructorCacheUsesProvidedMap() throws Exception {
        // Arrange – create a map that already contains the constructor for DummyClass
        Constructor<DummyClass> expectedConstructor = DummyClass.class.getDeclaredConstructor();
        Map<Object, Constructor<?>> customCache = new HashMap<>();
        customCache.put(DummyClass.class, expectedConstructor);
        // Act – replace the internal cache with our custom map
        ReflectionUtils.setConstructorCache(customCache);
        // Assert – getConstructor should return the exact constructor instance we put into the map
        Constructor<?> actualConstructor = ReflectionUtils.getConstructor(DummyClass.class);
        assertSame(expectedConstructor, actualConstructor, "ReflectionUtils should return the constructor stored in the supplied cache");
    }

    @Test
    void testSetConstructorCacheWithEmptyMapStillWorks() throws Exception {
        // Arrange – an empty cache
        Map<Object, Constructor<?>> emptyCache = new HashMap<>();
        // Act
        ReflectionUtils.setConstructorCache(emptyCache);
        Constructor<?> ctor = ReflectionUtils.getConstructor(DummyClass.class);
        // Assert – a constructor is still returned (the method should compute it on‑the‑fly)
        assertNotNull(ctor, "Even with an empty cache a constructor should be obtainable");
        assertEquals(DummyClass.class, ctor.getDeclaringClass(), "Returned constructor should belong to DummyClass");
    }

    @Test
    void testSetConstructorCacheWithNullLeadsToFailureOnSubsequentLookup() {
        // Act – set the cache to null (the method itself does not throw)
        ReflectionUtils.setConstructorCache(null);
        // Assert – any subsequent reflective lookup that relies on the cache should now NPE
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getConstructor(DummyClass.class), "When the internal cache is null, a subsequent lookup should raise NPE");
    }

    /**
     * Helper to read the private static {@code CONSTRUCTOR_CACHE} field via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Constructor<?>> getInternalCache() throws Exception {
        Field field = ReflectionUtils.class.getDeclaredField("CONSTRUCTOR_CACHE");
        field.setAccessible(true);
        return (Map<Object, Constructor<?>>) field.get(null);
    }

    @Test
    void setConstructorCache_replacesWithNewMutableMap() throws Exception {
        // group: replace with a new mutable map
        Map<Object, Constructor<?>> newMap = new HashMap<>();
        ReflectionUtils.setConstructorCache(newMap);
        Map<Object, Constructor<?>> internal = getInternalCache();
        assertSame(newMap, internal, "The internal cache should be the exact mutable HashMap instance supplied");
    }

    @Test
    void setConstructorCache_replacesWithMapContainingEntries() throws Exception {
        // group: replace with a map containing entries
        Constructor<DummyClass> ctor = DummyClass.class.getDeclaredConstructor();
        // Build a ConstructorCacheKey via reflection (inner class of ReflectionUtils)
        Class<?> keyClass = Class.forName("com.cedarsoftware.util.ReflectionUtils$ConstructorCacheKey");
        Constructor<?> keyCtor = keyClass.getDeclaredConstructor(Class.class, Class[].class);
        keyCtor.setAccessible(true);
        Object keyInstance = keyCtor.newInstance(DummyClass.class, new Class<?>[0]);
        Map<Object, Constructor<?>> mapWithEntry = Collections.singletonMap(keyInstance, ctor);
        ReflectionUtils.setConstructorCache(mapWithEntry);
        Map<Object, Constructor<?>> internal = getInternalCache();
        assertSame(mapWithEntry, internal, "Cache instance should be the supplied map");
        assertTrue(internal.containsKey(keyInstance), "Cache must contain the supplied key");
        assertSame(ctor, internal.get(keyInstance), "Cache must contain the exact constructor value");
    }

    @Test
    void setConstructorCache_acceptsUnmodifiableMap() throws Exception {
        // group: replace with an unmodifiable map
        Map<Object, Constructor<?>> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        ReflectionUtils.setConstructorCache(unmodifiable);
        Map<Object, Constructor<?>> internal = getInternalCache();
        assertSame(unmodifiable, internal, "The internal cache should reference the unmodifiable map instance");
    }

    @Test
    void setConstructorCache_allowsNullAssignment() throws Exception {
        // group: replace with null
        ReflectionUtils.setConstructorCache(null);
        Map<Object, Constructor<?>> internal = getInternalCache();
        assertNull(internal, "Internal cache should be null after assigning null");
    }

    @Test
    void setConstructorCache_acceptsMapWithIncompatibleGenerics() throws Exception {
        // group: replace with a map of incompatible generic types
        @SuppressWarnings("unchecked")
        Map<String, String> rawMap = new HashMap<>();
        rawMap.put("key", "value");
        // Cast to raw Map to match the method signature
        ReflectionUtils.setConstructorCache((Map) rawMap);
        Map<Object, Constructor<?>> internal = getInternalCache();
        assertSame(rawMap, internal, "Due to type erasure, the raw map should be stored unchanged");
    }

    @Test
    void setConstructorCache_acceptsSynchronizedMap() throws Exception {
        // group: replace with a synchronized map
        Map<Object, Constructor<?>> syncMap = Collections.synchronizedMap(new HashMap<>());
        ReflectionUtils.setConstructorCache(syncMap);
        Map<Object, Constructor<?>> internal = getInternalCache();
        assertSame(syncMap, internal, "The internal cache should be the same synchronized map instance supplied");
    }

    @Test
    void setConstructorCache_acceptsConcurrentHashMap() throws Exception {
        // group: replace with a ConcurrentHashMap
        Map<Object, Constructor<?>> concurrentMap = new ConcurrentHashMap<>();
        ReflectionUtils.setConstructorCache(concurrentMap);
        Map<Object, Constructor<?>> internal = getInternalCache();
        assertSame(concurrentMap, internal, "The internal cache should be the exact ConcurrentHashMap instance supplied");
    }

    @Test
    void setConstructorCache_preservesNullKeyAndValue() throws Exception {
        // group: replace with a map containing null key and null value
        Map<Object, Constructor<?>> mapWithNulls = new HashMap<>();
        mapWithNulls.put(null, null);
        ReflectionUtils.setConstructorCache(mapWithNulls);
        Map<Object, Constructor<?>> internal = getInternalCache();
        assertSame(mapWithNulls, internal, "Cache instance should be the map we supplied");
        assertTrue(internal.containsKey(null), "Cache must contain a null key");
        assertNull(internal.get(null), "Cache must map the null key to a null value");
    }

    @Test
    void setConstructorCache_withSameInstanceLeavesCacheUnchanged() throws Exception {
        // group: replace with the same instance already stored
        // should be empty HashMap from @BeforeEach
        Map<Object, Constructor<?>> current = getInternalCache();
        ReflectionUtils.setConstructorCache(current);
        Map<Object, Constructor<?>> after = getInternalCache();
        assertSame(current, after, "When the same map instance is supplied, the internal cache should remain the same object");
    }

    @Test
    void setConstructorCache_acceptsDynamicProxyMap() throws Exception {
        // group: replace with a dynamic proxy map
        @SuppressWarnings("unchecked")
        Map<Object, Constructor<?>> proxy = (Map<Object, Constructor<?>>) Proxy.newProxyInstance(Map.class.getClassLoader(), new Class[] { Map.class }, (proxyObj, method, args) -> {
            // delegate to a real HashMap for simplicity
            Map<Object, Constructor<?>> target = new HashMap<>();
            return method.invoke(target, args);
        });
        ReflectionUtils.setConstructorCache(proxy);
        Map<Object, Constructor<?>> internal = getInternalCache();
        assertSame(proxy, internal, "The internal cache should hold the proxy instance itself, not the underlying map");
    }

    @Test
    void setConstructorCache_acceptsCustomAbstractMapImplementation() throws Exception {
        // group: replace with a custom Map implementation that tracks assignments
        Map<Object, Constructor<?>> customMap = new AbstractMap<Object, Constructor<?>>() {

            private final Map<Object, Constructor<?>> delegate = new HashMap<>();

            @Override
            public Set<Entry<Object, Constructor<?>>> entrySet() {
                return delegate.entrySet();
            }

            @Override
            public Constructor<?> put(Object key, Constructor<?> value) {
                return delegate.put(key, value);
            }
        };
        ReflectionUtils.setConstructorCache(customMap);
        Map<Object, Constructor<?>> internal = getInternalCache();
        assertSame(customMap, internal, "The internal cache should be the exact custom AbstractMap instance supplied");
    }
}
