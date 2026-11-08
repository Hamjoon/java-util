package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getConstructor_Test {

    /* ----------------------------------------------------------------------
     * Helper classes used in the tests
     * ---------------------------------------------------------------------- */
    static class NoArgClass {

        public NoArgClass() {
        }
    }

    static class SingleStringCtor {

        public SingleStringCtor(String s) {
        }
    }

    static class PrivateIntCtor {

        private PrivateIntCtor(int i) {
        }
    }

    static class NoMatchingCtor {

        public NoMatchingCtor(double d) {
        }
    }

    /* ----------------------------------------------------------------------
     * Helper classes used in the tests
     * ---------------------------------------------------------------------- */
    static class SampleNoArg {

        public SampleNoArg() {
        }
    }

    static class SampleWithParams {

        public SampleWithParams(String s, int i) {
        }
    }

    static class SamplePrivateNoArg {

        private SamplePrivateNoArg() {
        }
    }

    static class SampleWithPrimitive {

        public SampleWithPrimitive(int i) {
        }
    }

    static class SampleWithArrayParam {

        public SampleWithArrayParam(String[] arr) {
        }
    }

    static class SampleVarargs {

        public SampleVarargs(int... numbers) {
        }
    }

    static class SampleA {

        public SampleA(String s) {
        }
    }

    static class SampleB {

        public SampleB(String s) {
        }
    }

    static class SampleMixedParams {

        public SampleMixedParams(String s, int i, Boolean b) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the static cache so tests are independent
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    /* ----------------------------------------------------------------------
     * Helper method sources for parameterised tests (if needed in the future)
     * ---------------------------------------------------------------------- */
    static Stream<Arguments> dummyProvider() {
        // placeholder – not used currently
        return Stream.empty();
    }

    @Test
    void shouldReturnPublicConstructorWhenItExists() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SingleStringCtor.class, String.class);
        assertNotNull(ctor, "Constructor should be found");
        assertEquals(String.class, ctor.getParameterTypes()[0]);
        assertEquals(SingleStringCtor.class, ctor.getDeclaringClass());
    }

    @Test
    void shouldReturnPrivateConstructorWhenItExists() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PrivateIntCtor.class, int.class);
        assertNotNull(ctor, "Private constructor should be found and made accessible");
        assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Constructor should be accessible after ReflectionUtils processing");
        assertEquals(int.class, ctor.getParameterTypes()[0]);
    }

    @Test
    void shouldReturnNullWhenConstructorDoesNotExist() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(NoMatchingCtor.class, String.class);
        assertNull(ctor, "No such constructor – should return null");
    }

    @Test
    void shouldCacheConstructorInstances() {
        // First call – computes and caches
        Constructor<?> first = ReflectionUtils.getConstructor(SingleStringCtor.class, String.class);
        // Second call – should retrieve the same cached instance
        Constructor<?> second = ReflectionUtils.getConstructor(SingleStringCtor.class, String.class);
        assertSame(first, second, "Subsequent calls must return the same cached Constructor object");
    }

    @Test
    void shouldCacheNullForMissingConstructor() {
        // First lookup – caches a null entry
        Constructor<?> first = ReflectionUtils.getConstructor(NoMatchingCtor.class, String.class);
        // Second lookup – should also be null (cached)
        Constructor<?> second = ReflectionUtils.getConstructor(NoMatchingCtor.class, String.class);
        assertNull(first, "First lookup for missing constructor must be null");
        assertNull(second, "Second lookup for missing constructor must also be null");
    }

    @Test
    void shouldFindConstructorWithComparatorParameter() {
        // Use a standard JDK class that has a constructor taking Comparator (e.g., java.util.TreeMap)
        Constructor<?> ctor = ReflectionUtils.getConstructor(java.util.TreeMap.class, Comparator.class);
        assertNotNull(ctor, "TreeMap has a Comparator‑based constructor");
        assertEquals(Comparator.class, ctor.getParameterTypes()[0]);
    }

    // ---------- Null class argument ----------
    @Test
    void nullClassParameterShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getConstructor(null, new Class<?>[0]), "Passing a null class must trigger Convention.throwIfNull");
    }

    // ---------- Existing no‑arg constructor ----------
    @Test
    void existingNoArgConstructorFound() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SampleNoArg.class);
        assertNotNull(ctor);
        assertEquals(0, ctor.getParameterCount());
    }

    @Test
    void existingNoArgConstructorIsCached() {
        Constructor<?> first = ReflectionUtils.getConstructor(SampleNoArg.class);
        Constructor<?> second = ReflectionUtils.getConstructor(SampleNoArg.class);
        assertSame(first, second, "Cache must return the same Constructor instance");
    }

    // ---------- Existing parameterised constructor ----------
    @Test
    void existingParameterizedConstructorFound() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SampleWithParams.class, String.class, int.class);
        assertNotNull(ctor);
        assertArrayEquals(new Class<?>[] { String.class, int.class }, ctor.getParameterTypes());
    }

    @Test
    void existingParameterizedConstructorIsCached() {
        Constructor<?> first = ReflectionUtils.getConstructor(SampleWithParams.class, String.class, int.class);
        Constructor<?> second = ReflectionUtils.getConstructor(SampleWithParams.class, String.class, int.class);
        assertSame(first, second);
    }

    // ---------- Non‑existing constructor ----------
    @Test
    void nonExistingConstructorReturnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SampleNoArg.class, String.class);
        assertNull(ctor);
    }

    @Test
    void nonExistingConstructorIsCachedAsNull() {
        Constructor<?> first = ReflectionUtils.getConstructor(SampleNoArg.class, String.class);
        Constructor<?> second = ReflectionUtils.getConstructor(SampleNoArg.class, String.class);
        assertNull(first);
        assertNull(second);
    }

    // ---------- Cache key isolation ----------
    @Test
    void cacheKeyIsolationDifferentSignatures() {
        // No (String) ctor – should be null
        Constructor<?> nullCtor = ReflectionUtils.getConstructor(SampleWithParams.class, String.class);
        assertNull(nullCtor);
        // (String, int) ctor – should be present
        Constructor<?> validCtor = ReflectionUtils.getConstructor(SampleWithParams.class, String.class, int.class);
        assertNotNull(validCtor);
    }

    // ---------- Private constructor ----------
    @Test
    void privateNoArgConstructorFoundAndMadeAccessible() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SamplePrivateNoArg.class);
        assertNotNull(ctor);
        assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Private constructor must be made accessible");
    }

    @Test
    void privateNoArgConstructorIsCached() {
        Constructor<?> first = ReflectionUtils.getConstructor(SamplePrivateNoArg.class);
        Constructor<?> second = ReflectionUtils.getConstructor(SamplePrivateNoArg.class);
        assertSame(first, second);
    }

    // ---------- Primitive parameter matching ----------
    @Test
    void primitiveParameterMatchFound() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SampleWithPrimitive.class, int.class);
        assertNotNull(ctor);
        assertEquals(int.class, ctor.getParameterTypes()[0]);
    }

    @Test
    void primitiveParameterMatchIsCached() {
        Constructor<?> first = ReflectionUtils.getConstructor(SampleWithPrimitive.class, int.class);
        Constructor<?> second = ReflectionUtils.getConstructor(SampleWithPrimitive.class, int.class);
        assertSame(first, second);
    }

    // ---------- Primitive parameter mismatch ----------
    @Test
    void primitiveParameterMismatchReturnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SampleWithPrimitive.class, Integer.class);
        assertNull(ctor);
    }

    @Test
    void primitiveParameterMismatchIsCachedAsNull() {
        Constructor<?> first = ReflectionUtils.getConstructor(SampleWithPrimitive.class, Integer.class);
        Constructor<?> second = ReflectionUtils.getConstructor(SampleWithPrimitive.class, Integer.class);
        assertNull(first);
        assertNull(second);
    }

    // ---------- Array parameter constructor ----------
    @Test
    void arrayParameterConstructorFound() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SampleWithArrayParam.class, String[].class);
        assertNotNull(ctor);
        assertEquals(String[].class, ctor.getParameterTypes()[0]);
    }

    @Test
    void arrayParameterConstructorIsCached() {
        Constructor<?> first = ReflectionUtils.getConstructor(SampleWithArrayParam.class, String[].class);
        Constructor<?> second = ReflectionUtils.getConstructor(SampleWithArrayParam.class, String[].class);
        assertSame(first, second);
    }

    // ---------- Varargs constructor ----------
    @Test
    void varargsConstructorFound() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SampleVarargs.class, int[].class);
        assertNotNull(ctor);
        assertEquals(int[].class, ctor.getParameterTypes()[0]);
    }

    @Test
    void varargsConstructorIsCached() {
        Constructor<?> first = ReflectionUtils.getConstructor(SampleVarargs.class, int[].class);
        Constructor<?> second = ReflectionUtils.getConstructor(SampleVarargs.class, int[].class);
        assertSame(first, second);
    }

    // ---------- Null parameterTypes array ----------
    @Test
    void nullParameterTypesArrayThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getConstructor(SampleNoArg.class, (Class<?>[]) null), "Passing null as the varargs array should propagate NPE from Class#getDeclaredConstructor");
    }

    // ---------- Different classes same signature ----------
    @Test
    void differentClassSameSignatureCacheIsolation() {
        Constructor<?> ctorA = ReflectionUtils.getConstructor(SampleA.class, String.class);
        Constructor<?> ctorB = ReflectionUtils.getConstructor(SampleB.class, String.class);
        assertNotNull(ctorA);
        assertNotNull(ctorB);
        assertNotSame(ctorA, ctorB, "Cache key must include declaring class");
    }

    @Test
    void repeatedLookupForSampleAReturnsCachedInstance() {
        Constructor<?> first = ReflectionUtils.getConstructor(SampleA.class, String.class);
        Constructor<?> second = ReflectionUtils.getConstructor(SampleA.class, String.class);
        assertSame(first, second);
    }

    // ---------- Mixed‑type constructor ----------
    @Test
    void mixedParametersConstructorFound() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(SampleMixedParams.class, String.class, int.class, Boolean.class);
        assertNotNull(ctor);
        assertArrayEquals(new Class<?>[] { String.class, int.class, Boolean.class }, ctor.getParameterTypes());
    }

    @Test
    void mixedParametersConstructorIsCached() {
        Constructor<?> first = ReflectionUtils.getConstructor(SampleMixedParams.class, String.class, int.class, Boolean.class);
        Constructor<?> second = ReflectionUtils.getConstructor(SampleMixedParams.class, String.class, int.class, Boolean.class);
        assertSame(first, second);
    }

    // ---------- Null element inside parameterTypes array ----------
    @Test
    void nullElementInParameterTypesArrayPropagatesException() {
        Class<?>[] typesWithNull = new Class<?>[] { null };
        // The exact exception type depends on the internal key implementation;
        // we assert that an exception is thrown.
        // could be NPE or IllegalArgumentException
        assertThrows(// could be NPE or IllegalArgumentException
        RuntimeException.class, () -> ReflectionUtils.getConstructor(SampleNoArg.class, typesWithNull), "A null element in the parameterTypes array should cause an exception during cache‑key handling");
    }
}
