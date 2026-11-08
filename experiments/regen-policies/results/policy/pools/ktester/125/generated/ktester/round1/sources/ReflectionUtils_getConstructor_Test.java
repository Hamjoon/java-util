package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getConstructor_Test {

    /**
     * Simple class with a public no‑arg constructor.
     */
    public static class PublicNoArg {

        public PublicNoArg() {
        }
    }

    /**
     * Simple class with a private no‑arg constructor.
     */
    public static class PrivateNoArg {

        private PrivateNoArg() {
        }
    }

    /**
     * Simple class with a package‑private constructor that takes a single argument.
     */
    public static class OneArg {

        OneArg(String s) {
        }
    }

    /**
     * Public class with several constructors.
     */
    public static class PublicClass {

        public PublicClass() {
        }

        public PublicClass(String s) {
        }

        public PublicClass(int i, double d) {
        }
    }

    /**
     * Private class with private constructors.
     */
    public static class PrivateClass {

        private PrivateClass() {
        }

        private PrivateClass(int i) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the cache is cleared before each test to avoid cross‑test interference.
        // ReflectionUtils provides a setter for the cache – we replace it with a fresh map.
        ReflectionUtils.setConstructorCache(new java.util.HashMap<>());
        // Replace the internal cache with a fresh HashMap to avoid cross‑test interference.
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    static Stream<Arguments> publicConstructorProvider() {
        return Stream.of(// public no‑arg
        Arguments.of(PublicClass.class, new Class<?>[0]), // public String‑arg
        Arguments.of(PublicClass.class, new Class<?>[] { String.class }), // public primitive args (int, double)
        Arguments.of(PublicClass.class, new Class<?>[] { int.class, double.class }));
    }

    static Stream<Arguments> privateConstructorProvider() {
        return Stream.of(// private no‑arg
        Arguments.of(PrivateClass.class, new Class<?>[0]), // private int‑arg
        Arguments.of(PrivateClass.class, new Class<?>[] { int.class }));
    }

    @Test
    void shouldReturnPublicNoArgConstructor() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PublicNoArg.class);
        assertNotNull(ctor, "Constructor should not be null for a public no‑arg ctor");
        assertTrue(Modifier.isPublic(ctor.getModifiers()), "Returned constructor must be public");
        assertEquals(PublicNoArg.class, ctor.getDeclaringClass());
    }

    @Test
    void shouldMakeNonPublicConstructorAccessible() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PrivateNoArg.class);
        assertNotNull(ctor, "Constructor should be found even if it is private");
        // The utility should have called setAccessible(true) for non‑public ctors.
        assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Non‑public constructor should be made accessible");
    }

    @Test
    void shouldRetrieveConstructorWithParameters() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(OneArg.class, String.class);
        assertNotNull(ctor, "Constructor with a single String parameter should be found");
        assertEquals(1, ctor.getParameterCount(), "Parameter count must be 1");
        assertEquals(String.class, ctor.getParameterTypes()[0], "Parameter type must be String");
    }

    @Test
    void shouldCacheConstructorInstances() {
        Constructor<?> first = ReflectionUtils.getConstructor(PublicNoArg.class);
        Constructor<?> second = ReflectionUtils.getConstructor(PublicNoArg.class);
        assertSame(first, second, "Subsequent calls must return the same cached Constructor instance");
    }

    @Test
    void shouldCacheNullWhenConstructorDoesNotExist() {
        // Request a constructor that does not exist – e.g., a String‑arg ctor for PublicNoArg.
        Constructor<?> missing = ReflectionUtils.getConstructor(PublicNoArg.class, String.class);
        assertNull(missing, "Missing constructor should be represented as null");
        // The second call should hit the cache and also return null (not throw).
        Constructor<?> missingAgain = ReflectionUtils.getConstructor(PublicNoArg.class, String.class);
        assertNull(missingAgain, "Cached null should be returned on subsequent look‑ups");
    }

    @Test
    void shouldValidateNullClassArgument() {
        // The utility uses Convention.throwIfNull which throws IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getConstructor(null), "Passing a null class must result in an IllegalArgumentException");
    }

    @Test
    void shouldHandleNullParameterTypes() {
        // Passing a null var‑args array should be treated as a lookup miss and cached as null.
        Constructor<?> first = ReflectionUtils.getConstructor(PublicNoArg.class, (Class<?>[]) null);
        assertNull(first, "Lookup with null parameterTypes should return null");
        // Second call must hit the cache and also return null (no exception).
        Constructor<?> second = ReflectionUtils.getConstructor(PublicNoArg.class, (Class<?>[]) null);
        assertNull(second, "Cached null should be returned on subsequent look‑ups");
    }

    @ParameterizedTest
    @MethodSource("publicConstructorProvider")
    void shouldRetrievePublicConstructors(Class<?> clazz, Class<?>[] paramTypes) {
        Constructor<?> ctor = ReflectionUtils.getConstructor(clazz, paramTypes);
        assertNotNull(ctor, "Public constructor should be found");
        assertTrue(Modifier.isPublic(ctor.getModifiers()), "Returned constructor must be public");
        assertArrayEquals(paramTypes, ctor.getParameterTypes(), "Parameter types must match the requested signature");
    }

    @ParameterizedTest
    @MethodSource("privateConstructorProvider")
    void shouldMakeNonPublicConstructorsAccessible(Class<?> clazz, Class<?>[] paramTypes) {
        Constructor<?> ctor = ReflectionUtils.getConstructor(clazz, paramTypes);
        assertNotNull(ctor, "Private constructor should be found");
        // Since the constructor is non‑public, ReflectionUtils should have called setAccessible(true).
        // The canAccess(null) method works for JDK 9+. For older JDKs, fall back to isAccessible().
        assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Non‑public constructor should be made accessible");
    }

    @Test
    void shouldReturnNullWhenConstructorDoesNotExist() {
        // Request a constructor that does not exist: PublicClass(String, Integer)
        Constructor<?> missing = ReflectionUtils.getConstructor(PublicClass.class, String.class, Integer.class);
        assertNull(missing, "Missing constructor should be represented as null");
        // Verify that the null result is cached.
        Constructor<?> missingAgain = ReflectionUtils.getConstructor(PublicClass.class, String.class, Integer.class);
        assertNull(missingAgain, "Cached null should be returned on subsequent look‑ups");
    }

    @Test
    void shouldCacheNullWhenConstructorDoesNotExistForPublicNoArg() {
        // A constructor with a String argument does not exist in PublicNoArg.
        Constructor<?> missing = ReflectionUtils.getConstructor(PublicNoArg.class, String.class);
        assertNull(missing, "Missing constructor should be null");
        // Second call should hit the cache and also return null.
        Constructor<?> missingAgain = ReflectionUtils.getConstructor(PublicNoArg.class, String.class);
        assertNull(missingAgain, "Cached null should be returned on subsequent look‑ups");
    }
}
