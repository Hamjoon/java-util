package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtils_getConstructor_Test {

    /**
     * Simple class with a no‑arg constructor and a single‑int constructor.
     */
    public static class Sample {

        public Sample() {
        }

        public Sample(int size) {
        }
    }

    /**
     * Class that has only a no‑arg constructor – used to provoke a failure.
     */
    public static class NoIntConstructor {

        public NoIntConstructor() {
        }
    }

    /**
     * Public class with a public no‑arg constructor – used for cache‑miss / hit tests.
     */
    public static class NoArgClass {

        public NoArgClass() {
        }
    }

    /**
     * Class that has a public constructor with an int argument only – no public no‑arg constructor.
     */
    public static class NoSuchConstructorClass {

        public NoSuchConstructorClass(int i) {
        }
    }

    /**
     * Class that defines only a private no‑arg constructor.
     */
    public static class PrivateConstructorClass {

        private PrivateConstructorClass() {
        }
    }

    /**
     * Class with a constructor that takes an {@link Integer}.
     */
    public static class ParamConstructorClass {

        public ParamConstructorClass(Integer value) {
        }
    }

    /**
     * Verify that a constructor that exists is returned and cached.
     */
    @Test
    void getConstructor_existingConstructor_isReturnedAndCached() {
        // GIVEN
        Class<?> targetClass = Sample.class;
        Class<?>[] paramTypes = new Class<?>[] { int.class };
        // WHEN – first lookup
        Constructor<?> firstLookup = ReflectionUtils.getConstructor(targetClass, paramTypes);
        // THEN – we obtain a non‑null constructor matching the expected signature
        assertNotNull(firstLookup, "Constructor should not be null");
        assertEquals(int.class, firstLookup.getParameterTypes()[0], "Constructor should accept a single int argument");
        // WHEN – second lookup (should hit the cache)
        Constructor<?> secondLookup = ReflectionUtils.getConstructor(targetClass, paramTypes);
        // THEN – the same Constructor instance is returned (cache hit)
        assertSame(firstLookup, secondLookup, "Subsequent calls with the same key must return the cached Constructor");
    }

    /**
     * Verify that a request for a non‑existent constructor results in an
     *  IllegalArgumentException as specified by the API.
     */
    @Test
    void getConstructor_nonExistingConstructor_throwsIllegalArgumentException() {
        // GIVEN
        Class<?> targetClass = NoIntConstructor.class;
        Class<?>[] paramTypes = new Class<?>[] { int.class };
        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getConstructor(targetClass, paramTypes), "Requesting a missing constructor must throw IllegalArgumentException");
        // OPTIONAL: verify that the cause is NoSuchMethodException
        assertTrue(thrown.getCause() instanceof NoSuchMethodException, "Cause of the IllegalArgumentException should be NoSuchMethodException");
    }

    /**
     * Verify that different parameter signatures produce distinct cached entries.
     */
    @Test
    void getConstructor_differentParameterTypes_returnDifferentConstructors() {
        // GIVEN
        Class<?> targetClass = Sample.class;
        // Constructor with no arguments
        Constructor<?> noArgCtor = ReflectionUtils.getConstructor(targetClass);
        // Constructor with a single int argument
        Constructor<?> intArgCtor = ReflectionUtils.getConstructor(targetClass, int.class);
        // THEN
        assertNotNull(noArgCtor);
        assertNotNull(intArgCtor);
        assertNotEquals(noArgCtor, intArgCtor, "Different parameter lists must yield different Constructor objects");
    }

    /**
     * Cache miss – retrieve a public no‑arg constructor and store it.
     */
    @Test
    void getConstructor_cacheMiss_publicNoArgConstructor_isReturnedAndCached() {
        // GIVEN
        Class<?> targetClass = NoArgClass.class;
        // WHEN – first lookup (cache miss)
        Constructor<?> ctor = ReflectionUtils.getConstructor(targetClass);
        // THEN – a non‑null constructor for the no‑arg signature is returned
        assertNotNull(ctor, "Constructor should be found for public no‑arg class");
        assertEquals(0, ctor.getParameterTypes().length, "Returned constructor must have no parameters");
    }

    /**
     * Cache hit – the same Constructor instance is returned on the second call.
     */
    @Test
    void getConstructor_cacheHit_sameConstructorInstanceReturned() {
        // GIVEN
        Class<?> targetClass = NoArgClass.class;
        // First lookup – populates the cache
        Constructor<?> first = ReflectionUtils.getConstructor(targetClass);
        // Second lookup – should hit the cache
        Constructor<?> second = ReflectionUtils.getConstructor(targetClass);
        // THEN – both references are identical
        assertSame(first, second, "Second call should return the exact same Constructor instance cached previously");
    }

    /**
     * Passing a null {@code clazz} argument must result in a NullPointerException.
     */
    @Test
    void getConstructor_nullClassArgument_throwsNullPointerException() {
        // GIVEN
        Class<?> nullClazz = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getConstructor(nullClazz), "Calling getConstructor with a null class should throw NullPointerException");
    }

    /**
     * Passing a null {@code parameterTypes} array must result in a NullPointerException.
     */
    @Test
    void getConstructor_nullParameterTypesArgument_throwsNullPointerException() {
        // GIVEN
        Class<?> targetClass = NoArgClass.class;
        Class<?>[] nullParams = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getConstructor(targetClass, nullParams), "Calling getConstructor with a null parameterTypes array should throw NullPointerException");
    }

    /**
     * Requesting a constructor that does not exist should raise IllegalArgumentException.
     */
    @Test
    void getConstructor_nonExistentConstructor_throwsIllegalArgumentException() {
        // GIVEN
        // only int‑arg ctor exists
        Class<?> targetClass = NoSuchConstructorClass.class;
        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getConstructor(targetClass), "Missing public no‑arg constructor must cause IllegalArgumentException");
        assertTrue(thrown.getCause() instanceof NoSuchMethodException, "Root cause should be NoSuchMethodException");
    }

    /**
     * Private constructors are not accessible via {@code Class#getConstructor}; expect IllegalArgumentException.
     */
    @Test
    void getConstructor_privateConstructorOnly_throwsIllegalArgumentException() {
        // GIVEN
        Class<?> targetClass = PrivateConstructorClass.class;
        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getConstructor(targetClass), "Private constructor should not be reachable and must cause IllegalArgumentException");
        assertTrue(thrown.getCause() instanceof NoSuchMethodException, "Root cause should be NoSuchMethodException");
    }

    /**
     * Supplying mismatched parameter types should result in IllegalArgumentException.
     */
    @Test
    void getConstructor_mismatchedParameterTypes_throwsIllegalArgumentException() {
        // GIVEN
        // expects Integer
        Class<?> targetClass = ParamConstructorClass.class;
        Class<?>[] mismatched = new Class<?>[] { String.class };
        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getConstructor(targetClass, mismatched), "Mismatched parameter types must cause IllegalArgumentException");
        assertTrue(thrown.getCause() instanceof NoSuchMethodException, "Root cause should be NoSuchMethodException");
    }
}
