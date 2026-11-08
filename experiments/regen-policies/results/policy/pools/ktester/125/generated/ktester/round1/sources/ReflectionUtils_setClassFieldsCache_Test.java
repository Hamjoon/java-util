package com.cedarsoftware.util;

import org.mockito.Mock;
import java.lang.reflect.Field;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
// import org.junit.*;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
import org.mockito.junit.MockitoJUnitRunner;
// import org.junit.*;
// import org.junit.runner.RunWith;
// import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class ReflectionUtils_setClassFieldsCache_Test {

    /**
     * Mock cache that will be injected into {@link ReflectionUtils}.
     */
    @Mock
    Map<Object, Collection<Field>> mockCache;

    /**
     * Helper to access the private static {@code FIELDS_CACHE} field via reflection.
     */
    private static Field cacheField;

    //     @RunWith(Parameterized.class)
    public static class ParameterisedCacheTests {

        private final String description;

        private final Map<Object, Collection<Field>> suppliedCache;

        private final boolean expectNullAfterSet;

        private final boolean verifyModification;

        public ParameterisedCacheTests(String description, Map<Object, Collection<Field>> suppliedCache, boolean expectNullAfterSet, boolean verifyModification) {
            this.description = description;
            this.suppliedCache = suppliedCache;
            this.expectNullAfterSet = expectNullAfterSet;
            this.verifyModification = verifyModification;
        }
        //         @Parameterized.Parameters(name = "{0}")
        //         public static Collection<Object[]> cacheProvider() {
        //             Map<Object, Collection<Field>> emptyMap = new HashMap<>();
        //             Map<Object, Collection<Field>> nonEmptyMap = new HashMap<>();
        //             nonEmptyMap.put("key", Collections.emptyList());
        //             Map<Object, Collection<Field>> unmodifiableMap = Collections.unmodifiableMap(new HashMap<>());
        //             Map<Object, Collection<Field>> concurrentMap = new ConcurrentHashMap<>();
        //             Map<Object, Collection<Field>> customMap = new CustomMap<>();
        //             Map<Object, Collection<Field>> mapWithNullKey = new HashMap<>();
        //             mapWithNullKey.put(null, Collections.emptyList());
        //             Map<Object, Collection<Field>> mapWithNullValue = new HashMap<>();
        //             mapWithNullValue.put("k", null);
        //             Map<Object, Collection<Field>> lruCacheInstance = new LRUCache<>(10);
        //             Map<Object, Collection<Field>> mapThenModified = new HashMap<>();
        //             mapThenModified.put("initial", Collections.emptyList());
        //             Map<Object, Collection<Field>> nullMap = null;
        //
        //             return Arrays.asList(new Object[][]{
        //                 {"nonEmptyMap", nonEmptyMap, false, false},
        //                 {"emptyMap", emptyMap, false, false},
        //                 {"null", nullMap, true, false},
        //                 {"unmodifiableMap", unmodifiableMap, false, false},
        //                 {"concurrentHashMap", concurrentMap, false, false},
        //                 {"customMapImplementation", customMap, false, false},
        //                 {"mapWithNullKey", mapWithNullKey, false, false},
        //                 {"mapWithNullValue", mapWithNullValue, false, false},
        //                 {"lrucacheInstance", lruCacheInstance, false, false},
        //                 {"mapThenModified", mapThenModified, false, true}
        //             });
        //         }
        //         @org.junit.Test
        //         public void testSetClassFieldsCache_variousMaps() throws Exception {
        //             // Act: replace the internal cache with the supplied map (may be null).
        //             ReflectionUtils.setClassFieldsCache(suppliedCache);
        //             // Retrieve the internal cache via reflection.
        //             Object internalCache = cacheField.get(null);
        //             if (expectNullAfterSet) {
        //                 assertNull("When a null map is supplied the internal cache should become null.", internalCache);
        //             } else {
        //                 assertSame("The internal cache must reference the exact map instance supplied.", suppliedCache, internalCache);
        //             }
        //             // For the special “mapThenModified” case we verify that later modifications
        //             // of the supplied map are reflected in the static field.
        //             if (verifyModification && suppliedCache != null) {
        //                 suppliedCache.put("newKey", Collections.emptyList());
        //                 Object afterModification = cacheField.get(null);
        //                 @SuppressWarnings("unchecked")
        //                 Map<Object, Collection<Field>> afterMap = (Map<Object, Collection<Field>>) afterModification;
        //                 assertTrue("Modifications to the supplied map must be visible through the static cache reference.", afterMap.containsKey("newKey"));
        //             }
        //         }
    }

    /**
     * Simple class with two fields used for reflective queries.
     */
    private static class Dummy {

        private int intField;

        private String stringField;
    }

    /**
     * Custom map implementation used in one of the test cases.
     */
    private static class CustomMap<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }
    }

    /**
     * Before each test we replace the internal cache with the mocked one.
     * The mock is left in its default state (all methods return {@code null}
     * or {@code false}) which mimics a cache miss on the first lookup.
     */
//     @BeforeEach
    void setupBeforeEach() {
        when(mockCache.get(any())).thenReturn(null);
        ReflectionUtils.setClassFieldsCache(mockCache);
    }

    /**
     * After each test we restore a harmless cache implementation so that
     * other tests that rely on the original caching behaviour are not affected.
     */
//     @AfterEach
    void teardownAfterEach() {
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
    }

    /**
     * Supplies the different map instances required by the parameterized test.
     */
//     private static Stream<org.junit.jupiter.params.provider.Arguments> cacheProvider() {
//         Map<Object, Collection<Field>> emptyMap = new HashMap<>();
//         Map<Object, Collection<Field>> nonEmptyMap = new HashMap<>();
//         nonEmptyMap.put("key", Collections.emptyList());
//         Map<Object, Collection<Field>> unmodifiableMap = Collections.unmodifiableMap(new HashMap<>());
//         Map<Object, Collection<Field>> concurrentMap = new ConcurrentHashMap<>();
//         Map<Object, Collection<Field>> customMap = new CustomMap<>();
//         Map<Object, Collection<Field>> mapWithNullKey = new HashMap<>();
//         mapWithNullKey.put(null, Collections.emptyList());
//         Map<Object, Collection<Field>> mapWithNullValue = new HashMap<>();
//         mapWithNullValue.put("k", null);
//         Map<Object, Collection<Field>> lruCacheInstance = new LRUCache<>(10);
//         Map<Object, Collection<Field>> mapThenModified = new HashMap<>();
//         mapThenModified.put("initial", Collections.emptyList());
//         Map<Object, Collection<Field>> nullMap = null;
//         return Stream.of(arguments("nonEmptyMap", nonEmptyMap, false, false), arguments("emptyMap", emptyMap, false, false), arguments("null", nullMap, true, false), arguments("unmodifiableMap", unmodifiableMap, false, false), arguments("concurrentHashMap", concurrentMap, false, false), arguments("customMapImplementation", customMap, false, false), arguments("mapWithNullKey", mapWithNullKey, false, false), arguments("mapWithNullValue", mapWithNullValue, false, false), arguments("lrucacheInstance", lruCacheInstance, false, false), arguments("mapThenModified", mapThenModified, false, true));
//     }

    /**
     * Initialise {@code cacheField} once for the whole test class.
     */
//     @BeforeClass
//     public static void initCacheField() throws Exception {
//         cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
//         cacheField.setAccessible(true);
//     }

    /**
     * Helper that returns the current internal cache instance.
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Collection<Field>> getInternalCache() throws IllegalAccessException {
        return (Map<Object, Collection<Field>>) cacheField.get(null);
    }

    /**
     * Verifies that {@code setClassFieldsCache} really swaps the cache used by {@code getAllDeclaredFields}.
     */
//     @Test
//     public void testSetClassFieldsCache_usesProvidedCache() {
//         // Act – request the declared fields of {@code Dummy}. This should trigger a cache miss
//         // (mock returns null) and then a put of the computed collection.
//         Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(Dummy.class);
//         // Assert – the method returns the expected number of fields (2).
//         assertNotNull("Returned field collection must not be null", fields);
//         assertEquals("Dummy class should have exactly two declared fields", 2, fields.size());
//         // Verify interaction with the mock cache:
//         verify(mockCache, atLeastOnce()).get(any());
//         verify(mockCache, atLeastOnce()).put(any(), eq(fields));
//     }

    /**
     * A minimal sanity check that passing a non‑mock map works without throwing.
     */
//     @Test
//     public void testSetClassFieldsCache_withRealMap() throws Exception {
//         Map<Object, Collection<Field>> realMap = new HashMap<>();
//         // No exception should be thrown.
//         ReflectionUtils.setClassFieldsCache(realMap);
//         // Subsequent reflective call should populate the map.
//         ReflectionUtils.getAllDeclaredFields(Dummy.class);
//         assertFalse("The real map should have been populated by the reflective call", realMap.isEmpty());
//         // Ensure the internal cache reference is exactly the map we supplied.
//         assertSame("Internal cache must be the same instance we supplied", realMap, getInternalCache());
//     }

    /**
     * Verifies that supplying the current cache instance again does not replace it.
     */
//     @Test
//     public void testSetClassFieldsCache_sameInstanceAsCurrentCache() throws Exception {
//         Map<Object, Collection<Field>> currentCache = getInternalCache();
//         assertNotNull("Pre‑condition: the default cache must not be null.", currentCache);
//         // Call the method with the exact same instance.
//         ReflectionUtils.setClassFieldsCache(currentCache);
//         // Verify that the reference has not changed.
//         Map<Object, Collection<Field>> afterCache = getInternalCache();
//         assertSame("Passing the current cache instance again must keep the same reference.", currentCache, afterCache);
//     }

    /**
     * Parameterised‑style test covering a variety of map implementations.
     */
//     @Test
//     public void testSetClassFieldsCache_variousMaps() throws Exception {
//         // Build the different map variants.
//         Map<Object, Collection<Field>> emptyMap = new HashMap<>();
//         Map<Object, Collection<Field>> nonEmptyMap = new HashMap<>();
//         nonEmptyMap.put("key", Collections.emptyList());
//         Map<Object, Collection<Field>> unmodifiableMap = Collections.unmodifiableMap(new HashMap<>());
//         Map<Object, Collection<Field>> concurrentMap = new ConcurrentHashMap<>();
//         Map<Object, Collection<Field>> customMap = new CustomMap<>();
//         Map<Object, Collection<Field>> mapWithNullKey = new HashMap<>();
//         mapWithNullKey.put(null, Collections.emptyList());
//         Map<Object, Collection<Field>> mapWithNullValue = new HashMap<>();
//         mapWithNullValue.put("k", null);
//         Map<Object, Collection<Field>> lruCacheInstance = new LRUCache<>(10);
//         Map<Object, Collection<Field>> mapThenModified = new HashMap<>();
//         mapThenModified.put("initial", Collections.emptyList());
//         Map<Object, Collection<Field>> nullMap = null;
//         // Helper array of test data.
//         Object[][] data = { { "nonEmptyMap", nonEmptyMap, false, false }, { "emptyMap", emptyMap, false, false }, { "null", nullMap, true, false }, { "unmodifiableMap", unmodifiableMap, false, false }, { "concurrentHashMap", concurrentMap, false, false }, { "customMapImplementation", customMap, false, false }, { "mapWithNullKey", mapWithNullKey, false, false }, { "mapWithNullValue", mapWithNullValue, false, false }, { "lrucacheInstance", lruCacheInstance, false, false }, { "mapThenModified", mapThenModified, false, true } };
//         for (Object[] row : data) {
//             String description = (String) row[0];
//             @SuppressWarnings("unchecked")
//             Map<Object, Collection<Field>> suppliedCache = (Map<Object, Collection<Field>>) row[1];
//             boolean expectNullAfterSet = (boolean) row[2];
//             boolean verifyModification = (boolean) row[3];
//             // Act
//             ReflectionUtils.setClassFieldsCache(suppliedCache);
//             Object internalCache = cacheField.get(null);
//             // Assert
//             if (expectNullAfterSet) {
//                 assertNull(description + ": cache should be null when supplied map is null", internalCache);
//             } else {
//                 assertSame(description + ": internal cache must be the exact supplied instance", suppliedCache, internalCache);
//             }
//             // Additional modification check for the special case.
//             if (verifyModification && suppliedCache != null) {
//                 suppliedCache.put("newKey", Collections.emptyList());
//                 @SuppressWarnings("unchecked")
//                 Map<Object, Collection<Field>> afterMap = (Map<Object, Collection<Field>>) cacheField.get(null);
//                 assertTrue(description + ": modifications to supplied map must be visible", afterMap.containsKey("newKey"));
//             }
//         }
//     }
}