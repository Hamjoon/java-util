package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_Test {

    /**
     * Simple class used to verify that all declared fields are discovered.
     */
    static class SampleClass {

        public int publicInt;

        private String privateString;

        protected double protectedDouble;

        static long staticLong;
    }

    /**
     * Simple class with a mixture of field modifiers.
     */
    static class TestClassA {

        public int publicField;

        private String privateField;
    }

    /**
     * Another class to ensure the cache works for multiple distinct keys.
     */
    static class TestClassB {

        int id;

        String name;

        double value;
    }

    /**
     * Clears the ReflectionUtils cache after each test to keep tests independent.
     */
    @AfterEach
    void teardownAfterEach() throws Exception {
        // The cache is a private static final field; we clear it via reflection.
        Field cacheField = ReflectionUtils.class.getDeclaredField("FIELD_META_CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Class<?>, List<Field>> cache = (ConcurrentMap<Class<?>, List<Field>>) cacheField.get(null);
        cache.clear();
    }

    /**
     * Supplies classes that are not yet cached.
     */
    private static Stream<Class<?>> cacheMissClasses() {
        return Stream.of(TestClassA.class, TestClassB.class, Integer.class);
    }

    @Test
    void getDeclaredFields_returnsAllDeclaredFields() {
        // given
        Class<?> target = SampleClass.class;
        // when
        List<Field> fields = ReflectionUtils.getDeclaredFields(target);
        // then
        assertNotNull(fields, "Returned field list must not be null");
        // SampleClass declares four fields (including static)
        assertEquals(4, fields.size(), "Expected number of declared fields does not match");
        // verify that each expected field name appears exactly once
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("publicInt")), "publicInt field missing");
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("privateString")), "privateString field missing");
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("protectedDouble")), "protectedDouble field missing");
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("staticLong")), "staticLong field missing");
    }

    @Test
    void getDeclaredFields_isCached_andReturnsSameInstance() {
        // given
        Class<?> target = SampleClass.class;
        // when
        List<Field> firstCall = ReflectionUtils.getDeclaredFields(target);
        List<Field> secondCall = ReflectionUtils.getDeclaredFields(target);
        // then
        assertSame(firstCall, secondCall, "ReflectionUtils should cache the field list and return the same instance");
    }

    @Test
    void getDeclaredFields_nullClass_throwsNullPointerException() {
        // given
        Class<?> nullClass = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeclaredFields(nullClass), "Passing null should result in NullPointerException");
    }

    /**
     * Returns the internal cache map (used only for assertions).
     */
    @SuppressWarnings("unchecked")
    private static ConcurrentMap<Class<?>, List<Field>> getCache() throws Exception {
        Field cacheField = ReflectionUtils.class.getDeclaredField("FIELD_META_CACHE");
        cacheField.setAccessible(true);
        return (ConcurrentMap<Class<?>, List<Field>>) cacheField.get(null);
    }

    @ParameterizedTest(name = "Cache miss – class {0}")
    @MethodSource("cacheMissClasses")
    void getDeclaredFields_cacheMiss_returnsAndCachesFields(Class<?> target) throws Exception {
        // Pre‑condition: cache must be empty for the target class
        assertFalse(getCache().containsKey(target), "Cache should be empty before first call");
        // When
        List<Field> fields = ReflectionUtils.getDeclaredFields(target);
        // Then – result must not be null and must contain exactly the declared fields
        assertNotNull(fields, "Returned field list must not be null");
        List<String> actualNames = new ArrayList<>();
        for (Field f : fields) actualNames.add(f.getName());
        // Build expected field name list using reflection (the same logic as the method under test)
        List<String> expectedNames = new ArrayList<>();
        for (Field f : target.getDeclaredFields()) expectedNames.add(f.getName());
        assertEquals(new HashSet<>(expectedNames), new HashSet<>(actualNames), "The list should contain exactly the declared fields of " + target.getName());
        // And the cache must now contain the entry, pointing to the same list instance
        assertTrue(getCache().containsKey(target), "Cache must contain the class after the call");
        assertSame(fields, getCache().get(target), "The cached list instance must be identical to the returned list");
    }

    @Test
    void getDeclaredFields_cacheHit_returnsSameInstance() throws Exception {
        // given
        Class<?> target = TestClassA.class;
        // first call populates the cache
        List<Field> first = ReflectionUtils.getDeclaredFields(target);
        assertNotNull(first, "First call must return a non‑null list");
        // when – second call
        List<Field> second = ReflectionUtils.getDeclaredFields(target);
        // then – the same list instance must be returned (cache hit)
        assertSame(first, second, "Subsequent calls for the same class must return the cached list");
    }
}
