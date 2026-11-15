package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.Objects;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_setSortedConstructorsCache_Test {

    /**
     * A simple map used as a test fixture.
     */
//     private Map<Object, Constructor<?>[]> emptyCache;

    /**
     * A second map used to verify that a later call replaces the previous cache.
     */
//     private Map<Object, Constructor<?>[]> anotherCache;

    /**
     * The cache used by {@link #getAllConstructors(Class)}.  It is wrapped in an
     * {@link AtomicReference} so that it can be replaced atomically at runtime.
     */
//     private static final AtomicReference<Map<?, ?>> SORTED_CONSTRUCTORS_CACHE = new AtomicReference<>(Collections.synchronizedMap(new java.util.LinkedHashMap<>()));

//     @BeforeEach
    void setupBeforeEach() {
        // Initialise fresh map instances before each test so that the
        // static cache is always put into a known state.
//         emptyCache = new HashMap<>();
//         anotherCache = new HashMap<>();
        // Populate the second map with a dummy entry – the actual content is
        // irrelevant for the behaviour we are testing; we only need a non‑empty map.
//         anotherCache.put("dummyKey", new Constructor<?>[0]);
        // Ensure the static cache starts from a clean state.
        ReflectionUtils.setSortedConstructorsCache(emptyCache);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Reset the static cache to an empty map so that tests are isolated.
//         ReflectionUtils.setSortedConstructorsCache(new HashMap<>());
    }

    /**
     * Group: basic replacement – regular HashMap and ConcurrentHashMap
     */
//     static Stream<Arguments> basicReplacementProvider() {
//         // regular HashMap – should be wrapped with Collections.synchronizedMap
//         Map<Object, Constructor<?>[]> hashMap = new HashMap<>();
//         hashMap.put("key1", new Constructor<?>[0]);
//         // ConcurrentHashMap – should be stored as‑is
//         Map<Object, Constructor<?>[]> concurrentMap = new ConcurrentHashMap<>();
//         concurrentMap.put("key2", new Constructor<?>[0]);
//         return Stream.of(Arguments.of("Regular HashMap should be wrapped", hashMap, true, "SynchronizedMap"), Arguments.of("ConcurrentHashMap should be stored directly", concurrentMap, false, null));
//     }

    /**
     * Group: already synchronized map
     */
//     static Stream<Arguments> alreadySynchronizedProvider() {
//         Map<Object, Constructor<?>[]> syncMap = Collections.synchronizedMap(new HashMap<>());
//         syncMap.put("syncKey", new Constructor<?>[0]);
//         return Stream.of(Arguments.of("Already synchronized map is stored unchanged", syncMap));
//     }

    /**
     * Replaces the internal cache that stores sorted constructors.
     *
     * <p>The supplied {@code cache} must be non‑null and thread‑safe.  If the
     * map is a plain {@link java.util.HashMap} (or any other non‑concurrent
     * implementation) it is wrapped with {@link Collections#synchronizedMap}
     * before being stored.  If the map already implements {@link ConcurrentMap}
     * or is already a synchronized wrapper, it is stored unchanged.</p>
     *
     * @param cache the new cache implementation; must not be {@code null}
     * @throws NullPointerException if {@code cache} is {@code null}
     */
//     public static void setSortedConstructorsCache(Map<Object, Constructor<?>[]> cache) {
//         if (cache == null) {
//             throw new NullPointerException("Cache map must not be null");
//         }
//         // If the map is already a ConcurrentMap we can store it as‑is.
//         // Otherwise, if it is already a synchronized wrapper we also keep it as‑is.
//         // For any other Map implementation we create a synchronized wrapper.
//         Map<Object, Constructor<?>[]> toStore;
//         if (cache instanceof ConcurrentMap) {
//             toStore = cache;
//         } else if (isSynchronizedMap(cache)) {
//             toStore = cache;
//         } else {
//             toStore = Collections.synchronizedMap(cache);
//         }
//         // Replace the reference atomically.
//         SORTED_CONSTRUCTORS_CACHE.set(toStore);
//     }

    /**
     * Helper that detects the JDK‑generated synchronized map wrapper.
     */
//     private static boolean isSynchronizedMap(Map<?, ?> map) {
//         // The wrapper class name always contains the word “SynchronizedMap”.
//         // Using the class name is the simplest and most reliable way without
//         // depending on internal JDK classes.
//         return map.getClass().getName().contains("SynchronizedMap");
//     }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; must not be {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        Objects.requireNonNull(method, "method must not be null");
        return method.getReturnType() == void.class;
    }

    /**
     * Finds a method with the given name and parameter types on the supplied class
     * (including its super‑classes and interfaces).  Returns {@code null} if no such
     * method exists.
     *
     * @param clazz          the class to search; must not be {@code null}
     * @param name           the method name; must not be {@code null}
     * @param parameterTypes the parameter types; may be {@code null} or empty
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        Objects.requireNonNull(name, "name must not be null");
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            // continue searching declared methods (including private)
            try {
                return clazz.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ex) {
                // Search in superclass hierarchy
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null) {
                    return findMethod(superClass, name, parameterTypes);
                }
                // No method found
                return null;
            }
        }
    }

    /**
     * Returns {@code true} if a method with the given signature is present on the
     * supplied class (including its super‑classes).  This is a convenience wrapper
     * around {@link #findMethod(Class, String, Class[])}.
     *
     * @param clazz          the class to search; must not be {@code null}
     * @param name           the method name; must not be {@code null}
     * @param parameterTypes the parameter types; may be {@code null} or empty
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

//     @Test
    void setSortedConstructorsCacheAcceptsEmptyMap() {
        // GIVEN an empty map (already set in @BeforeEach)
        // WHEN we replace the cache with another empty map
//         Map<Object, Constructor<?>[]> newCache = new HashMap<>();
        ReflectionUtils.setSortedConstructorsCache(newCache);
        // THEN no exception is thrown and the method returns normally.
//         assertTrue(true, "Method completed without throwing an exception");
    }

//     @Test
//     void setSortedConstructorsCacheAcceptsNonEmptyMap() {
//         // GIVEN a non‑empty map (anotherCache)
//         // WHEN we install it as the sorted‑constructors cache
//         ReflectionUtils.setSortedConstructorsCache(anotherCache);
//         // THEN the call succeeds – again, the contract is that no exception is thrown.
//         assertTrue(true, "Non‑empty map accepted without error");
//     }

//     @Test
//     void setSortedConstructorsCacheRejectsNull() {
//         // GIVEN a null reference
//         // WHEN we attempt to set it as the cache
//         // THEN a NullPointerException (or any RuntimeException) is expected.
//         assertThrows(NullPointerException.class, () -> ReflectionUtils.setSortedConstructorsCache(null), "Passing null should result in a NullPointerException");
//     }

//     @Test
//     void subsequentCallsReplacePreviousCache() {
//         // GIVEN the cache is initially empty
//         ReflectionUtils.setSortedConstructorsCache(emptyCache);
//         // WHEN we set a different cache
//         ReflectionUtils.setSortedConstructorsCache(anotherCache);
//         // THEN the second call succeeds – we verify by calling the method again
//         // with a third map and ensuring no exception is thrown.
//         Map<Object, Constructor<?>[]> thirdCache = new HashMap<>();
//         ReflectionUtils.setSortedConstructorsCache(thirdCache);
//         assertTrue(true, "Cache was successfully replaced multiple times");
//     }

    /* --------------------------------------------------------------------- */
//     @SuppressWarnings("unchecked")
//     private static Map<Object, Constructor<?>[]> getInternalCache() throws Exception {
//         java.lang.reflect.Field field = ReflectionUtils.class.getDeclaredField("SORTED_CONSTRUCTORS_CACHE");
//         field.setAccessible(true);
//         AtomicReference<Map<?, ?>> ref = (AtomicReference<Map<?, ?>>) field.get(null);
//         return (Map<Object, Constructor<?>[]>) ref.get();
//     }

//     @ParameterizedTest(name = "{0}")
//     @MethodSource("basicReplacementProvider")
//     void setSortedConstructorsCache_BasicReplacement(String description, Map<Object, Constructor<?>[]> input, boolean expectWrapper, String wrapperClassNameFragment) throws Exception {
//         // WHEN we set the supplied map as the cache
//         ReflectionUtils.setSortedConstructorsCache(input);
//         // THEN the internal cache reflects the expected behaviour
//         Map<Object, Constructor<?>[]> internal = getInternalCache();
//         if (expectWrapper) {
//             // The map must be a synchronized wrapper (implementation class name contains the fragment)
//             assertTrue(internal.getClass().getName().contains(wrapperClassNameFragment), "Expected a synchronized wrapper, got: " + internal.getClass().getName());
//             // The wrapper must contain the entry we put in the original map
//             assertArrayEquals(new Constructor<?>[0], internal.get("key1"), "Wrapped map should preserve the original entry");
//         } else {
//             // Should be the exact same instance we passed in
//             assertSame(input, internal, "Expected the same map instance to be stored");
//         }
//     }

//     @ParameterizedTest(name = "{0}")
//     @MethodSource("alreadySynchronizedProvider")
//     void setSortedConstructorsCache_AlreadySynchronized(String description, Map<Object, Constructor<?>[]> input) throws Exception {
//         ReflectionUtils.setSortedConstructorsCache(input);
//         Map<Object, Constructor<?>[]> internal = getInternalCache();
//         // The exact same instance must be kept (no double‑wrapping)
//         assertSame(input, internal, "Expected the same synchronized map instance to be stored");
//         assertArrayEquals(new Constructor<?>[0], internal.get("syncKey"));
//     }

    /**
     * Group: empty map
     */
//     @Test
//     void setSortedConstructorsCache_EmptyMapIsWrapped() throws Exception {
//         Map<Object, Constructor<?>[]> empty = new HashMap<>();
//         ReflectionUtils.setSortedConstructorsCache(empty);
//         Map<Object, Constructor<?>[]> internal = getInternalCache();
//         // Must be a synchronized wrapper and be empty
//         assertTrue(internal.getClass().getName().contains("SynchronizedMap"), "Empty map should be wrapped with a synchronized map");
//         assertTrue(internal.isEmpty(), "Wrapped map should be empty");
//     }

    /**
     * Group: map with null key and null value
     */
//     @Test
//     void setSortedConstructorsCache_MapWithNullKeyAndValue() throws Exception {
//         Map<Object, Constructor<?>[]> map = new HashMap<>();
//         map.put(null, null);
//         ReflectionUtils.setSortedConstructorsCache(map);
//         Map<Object, Constructor<?>[]> internal = getInternalCache();
//         // Must be a synchronized wrapper that preserves the null entry
//         assertTrue(internal.getClass().getName().contains("SynchronizedMap"));
//         assertTrue(internal.containsKey(null), "Wrapper should contain the null key");
//         assertNull(internal.get(null), "Value for null key should be null");
//     }

    /**
     * Group: custom concurrent map (ConcurrentSkipListMap)
     */
//     @Test
//     void setSortedConstructorsCache_CustomConcurrentMap() throws Exception {
//         Map<Object, Constructor<?>[]> skipMap = new ConcurrentSkipListMap<>();
//         skipMap.put("skipKey", new Constructor<?>[0]);
//         ReflectionUtils.setSortedConstructorsCache(skipMap);
//         Map<Object, Constructor<?>[]> internal = getInternalCache();
//         // Should be stored directly (same instance) because it already implements ConcurrentMap
//         assertSame(skipMap, internal, "ConcurrentSkipListMap should be stored without wrapping");
//         assertArrayEquals(new Constructor<?>[0], internal.get("skipKey"));
//     }

    /**
     * Group: map with non‑String key type
     */
//     @Test
//     void setSortedConstructorsCache_NonStringKey() throws Exception {
//         Map<Object, Constructor<?>[]> map = new HashMap<>();
//         map.put(12345, new Constructor<?>[0]);
//         ReflectionUtils.setSortedConstructorsCache(map);
//         Map<Object, Constructor<?>[]> internal = getInternalCache();
//         // Should be wrapped, but the key type must be preserved
//         assertTrue(internal.getClass().getName().contains("SynchronizedMap"));
//         assertArrayEquals(new Constructor<?>[0], internal.get(12345));
//     }

    /**
     * Group: null map – already covered by existing test, but kept for completeness
     */
//     @Test
//     void setSortedConstructorsCache_NullMapThrows() {
//         assertThrows(NullPointerException.class, () -> ReflectionUtils.setSortedConstructorsCache(null), "Passing null should result in NullPointerException");
//     }
}