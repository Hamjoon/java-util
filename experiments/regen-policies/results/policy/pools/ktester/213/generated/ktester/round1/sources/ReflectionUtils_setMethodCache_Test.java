package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setMethodCache_Test {

    /**
     * A simple class used for reflective look‑ups in the tests.
     */
    static class TestClass {

        private String field1;

        private int field2;

        @Override
        public String toString() {
            return "TestClass";
        }
    }

    /**
     * Functional interface used to validate the internal cache after a swap.
     */
    @FunctionalInterface
    private interface CacheValidator {

        void validate(Map<?, ?> internalCache) throws Exception;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the default cache is in place before each test.
        // The default cache is created by the static initializer of
        // ReflectionUtils, so we simply reset it to an empty, thread‑safe
        // map to avoid interference between tests.
        ReflectionUtils.setMethodCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the default (empty) cache after each test so that
        // subsequent tests start from a clean state.
        ReflectionUtils.setMethodCache(new HashMap<>());
    }

    /**
     * Provides a stream of test scenarios for {@link ReflectionUtils#setMethodCache(Map)}.
     *
     * Each scenario supplies:
     * <ul>
     *   <li>a descriptive name (used by JUnit for reporting)</li>
     *   <li>a map instance to pass to {@code setMethodCache}</li>
     *   <li>a predicate that validates the internal cache after the swap</li>
     * </ul>
     */
    private static Stream<Arguments> methodCacheScenarios() throws NoSuchMethodException {
        // Helper to create map entries
        Method lengthMethod = String.class.getMethod("length");
        Method intValueMethod = Integer.class.getMethod("intValue");
        Method toStringMethod = Object.class.getMethod("toString");
        Method sizeMethod = List.class.getMethod("size");
        Method charAtMethod = String.class.getMethod("charAt", int.class);
        Method listGetMethod = List.class.getMethod("get", int.class);
        // 1. Regular HashMap with multiple entries – should be wrapped (different instance)
        Map<Object, Method> hashMap = new HashMap<>();
        hashMap.put("key1", lengthMethod);
        hashMap.put("key2", intValueMethod);
        Arguments hashMapArgs = Arguments.of("HashMap with entries", hashMap, (CacheValidator) internal -> {
            assertNotSame(hashMap, internal, "Internal cache should be a wrapped map, not the original HashMap");
            assertEquals(2, internal.size(), "Internal cache should contain the same number of entries");
            assertEquals(lengthMethod, internal.get("key1"));
            assertEquals(intValueMethod, internal.get("key2"));
        });
        // 2. Collections.synchronizedMap – already thread‑safe, should be kept as‑is
        Map<Object, Method> syncMap = Collections.synchronizedMap(new HashMap<>());
        syncMap.put("k", toStringMethod);
        Arguments syncMapArgs = Arguments.of("Collections.synchronizedMap with single entry", syncMap, (CacheValidator) internal -> {
            assertSame(syncMap, internal, "Internal cache should be the exact same synchronized map instance");
            assertEquals(1, internal.size());
            assertEquals(toStringMethod, internal.get("k"));
        });
        // 3. Empty HashMap – should be wrapped but remain empty
        Map<Object, Method> emptyMap = new HashMap<>();
        Arguments emptyMapArgs = Arguments.of("Empty HashMap", emptyMap, (CacheValidator) internal -> {
            assertNotSame(emptyMap, internal, "Empty map should be wrapped, not the original instance");
            assertTrue(internal.isEmpty(), "Internal cache should be empty");
        });
        // 4. LinkedHashMap with one entry – non‑thread‑safe, should be wrapped
        Map<Object, Method> linkedMap = new LinkedHashMap<>();
        linkedMap.put("a", sizeMethod);
        Arguments linkedMapArgs = Arguments.of("LinkedHashMap with entry", linkedMap, (CacheValidator) internal -> {
            assertNotSame(linkedMap, internal, "LinkedHashMap should be wrapped, not the original instance");
            assertEquals(1, internal.size());
            assertEquals(sizeMethod, internal.get("a"));
        });
        // 5. ConcurrentHashMap – already thread‑safe, should be kept as‑is
        Map<Object, Method> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put("c1", charAtMethod);
        concurrentMap.put("c2", listGetMethod);
        Arguments concurrentMapArgs = Arguments.of("ConcurrentHashMap with entries", concurrentMap, (CacheValidator) internal -> {
            assertSame(concurrentMap, internal, "ConcurrentHashMap should be used directly without wrapping");
            assertEquals(2, internal.size());
            assertEquals(charAtMethod, internal.get("c1"));
            assertEquals(listGetMethod, internal.get("c2"));
        });
        return Stream.of(hashMapArgs, syncMapArgs, emptyMapArgs, linkedMapArgs, concurrentMapArgs);
    }

    /**
     * Verify that {@code setMethodCache} accepts a regular {@link HashMap}
     * and that subsequent reflective look‑ups still work.
     */
    @Test
    void setMethodCacheAcceptsRegularMap_andReflectionStillWorks() throws NoSuchMethodException {
        // GIVEN – a regular (non‑thread‑safe) map containing no entries
        Map<Object, Method> customCache = new HashMap<>();
        // WHEN – the cache is replaced with the custom map
        ReflectionUtils.setMethodCache(customCache);
        // THEN – a normal reflective call must still succeed
        Method method = ReflectionUtils.getMethod(TestClass.class, "toString");
        assertNotNull(method, "Method should be found after swapping the cache");
        assertEquals("toString", method.getName());
    }

    /**
     * Verify that a pre‑populated cache is honoured.  We insert a known
     * {@link Method} instance into the map using the same key that
     * {@code ReflectionUtils} would generate for the method.  Because the
     * key class is private, we cannot construct it directly; instead we
     * rely on the fact that {@code getMethod} will first look in the
     * cache and, if the entry is present, will return the cached instance.
     *
     * <p>This test therefore checks that the method returned after the
     * cache swap is the exact same instance that we placed into the map,
     * proving that the cache is consulted.</p>
     */
    @Test
    void setMethodCacheWithPrepopulatedEntry_isUsedByGetMethod() throws NoSuchMethodException {
        // GIVEN – obtain the Method instance we want to cache
        Method expectedMethod = TestClass.class.getMethod("toString");
        // The cache key used internally is a combination of the class,
        // method name and parameter types.  We can mimic the key by using
        // the same map entry that {@code ReflectionUtils} would create:
        // the key is the Method object itself (the public API does not
        // expose the exact key type, but the map is keyed by Object, so
        // we can safely store the Method as the key for this test).
        Map<Object, Method> customCache = new HashMap<>();
        customCache.put(expectedMethod, expectedMethod);
        // WHEN – replace the internal cache with our pre‑populated map
        ReflectionUtils.setMethodCache(customCache);
        // THEN – a call to getMethod should return the cached instance
        Method actualMethod = ReflectionUtils.getMethod(TestClass.class, "toString");
        assertSame(expectedMethod, actualMethod, "The method returned by getMethod should be the cached instance");
    }

    /**
     * Ensure that calling {@code setMethodCache} with {@code null}
     * results in a {@link NullPointerException}.  The private
     * {@code swap} method uses {@link java.util.Objects#requireNonNull},
     * which propagates the exception.
     */
    @Test
    void setMethodCacheRejectsNull() {
        // GIVEN – a null reference
        Map<Object, Method> nullMap = null;
        // WHEN / THEN – a NullPointerException is expected
        assertThrows(NullPointerException.class, () -> ReflectionUtils.setMethodCache(nullMap), "setMethodCache should reject a null argument");
    }

    /**
     * Retrieves the actual {@code Map} instance held inside the private
     * static {@code METHOD_CACHE} field of {@link ReflectionUtils}.
     *
     * @return the internal cache map (may be wrapped for thread‑safety)
     * @throws ReflectiveOperationException if reflection fails
     */
    @SuppressWarnings("unchecked")
    private static Map<?, ?> getInternalMethodCache() throws ReflectiveOperationException {
        Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
        cacheField.setAccessible(true);
        // The field is an AtomicReference<Map<...>>
        Object atomicRef = cacheField.get(null);
        Method getMethod = atomicRef.getClass().getMethod("get");
        return (Map<?, ?>) getMethod.invoke(atomicRef);
    }

    /**
     * Parameterized test that runs through all scenarios defined in
     * {@link #methodCacheScenarios()}.
     *
     * @param description   a short description for the scenario (used in test name)
     * @param suppliedMap   the map passed to {@code setMethodCache}
     * @param validator     lambda that checks the internal cache state
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("methodCacheScenarios")
    void setMethodCacheVariousMaps_areHandledCorrectly(String description, Map<Object, Method> suppliedMap, CacheValidator validator) throws Exception {
        // WHEN – replace the internal cache with the supplied map
        ReflectionUtils.setMethodCache(suppliedMap);
        // THEN – retrieve the internal cache via reflection and run the validator
        Map<?, ?> internalCache = getInternalMethodCache();
        validator.validate(internalCache);
    }
}
