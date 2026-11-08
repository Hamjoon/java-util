package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.Permission;
import java.util.*;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllConstructors_Test {

    /**
     * Simple interface – has no declared constructors.
     */
    interface SampleInterface {
    }

    /**
     * Class with a public no‑arg constructor and a private constructor
     *  that takes an int.
     */
    static class MixedCtorClass {

        public MixedCtorClass() {
        }

        @SuppressWarnings("unused")
        private MixedCtorClass(int i) {
        }
    }

    /**
     * Class with only a private no‑arg constructor.
     */
    static class PrivateNoArg {

        @SuppressWarnings("unused")
        private PrivateNoArg() {
        }
    }

    /**
     * Class with a single public constructor.
     */
    public static class PublicCtor {

        public PublicCtor() {
        }
    }

    /**
     * Class with a single private constructor.
     */
    public static class PrivateCtor {

        @SuppressWarnings("unused")
        private PrivateCtor() {
        }
    }

    /**
     * Class with public, protected and private constructors.
     */
    public static class MultiCtor {

        public MultiCtor() {
        }

        protected MultiCtor(String s) {
        }

        private MultiCtor(int i) {
        }
    }

    /**
     * Class with a package‑private (default) constructor.
     */
    static class PackagePrivateCtor {

        PackagePrivateCtor() {
        }
    }

    /**
     * Class with a protected constructor.
     */
    public static class ProtectedCtor {

        protected ProtectedCtor() {
        }
    }

    /**
     * Another class with a private no‑arg constructor – used for cache‑isolation test.
     */
    public static class AnotherPrivateCtor {

        @SuppressWarnings("unused")
        private AnotherPrivateCtor() {
        }
    }

    /**
     * Class with constructors declared in a specific order – used to verify order preservation.
     */
    public static class OrderedCtor {

        public OrderedCtor(int i) {
        }

        public OrderedCtor(String s, int i) {
        }

        public OrderedCtor() {
        }
    }

    /**
     * Outer class containing a non‑static inner class (synthetic constructor).
     */
    public static class Outer {

        public class Inner {

            public Inner() {
            }
        }
    }

    public class Inner {

        public Inner() {
        }
    }

    public class Inner {

        public Inner() {
        }
    }

    class ExceptionThrowingMap<K, V> extends HashMap<K, V> {

        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            throw new RuntimeException("forced exception");
        }
    }

    class ExceptionThrowingMap<K, V> extends HashMap<K, V> {

        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            throw new RuntimeException("forced exception");
        }
    }

    /**
     * Clears the internal constructor cache before each test so that tests are independent.
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ReflectionUtils.CONSTRUCTOR_CACHE is a private static field.
        // Replace it with a fresh empty map.
        Field cacheField = ReflectionUtils.class.getDeclaredField("CONSTRUCTOR_CACHE");
        cacheField.setAccessible(true);
        cacheField.set(null, new HashMap<>());
    }

    @Test
    void testGetAllConstructors_NullClass_ReturnsEmptyArray() {
        Constructor<?>[] result = ReflectionUtils.getAllConstructors(null);
        assertNotNull(result);
        assertEquals(0, result.length, "Null class should yield an empty constructor array");
    }

    @Test
    void testGetAllConstructors_Interface_ReturnsEmptyArray() {
        Constructor<?>[] result = ReflectionUtils.getAllConstructors(SampleInterface.class);
        assertNotNull(result);
        assertEquals(0, result.length, "Interface should have no declared constructors");
    }

    @Test
    void testGetAllConstructors_MixedVisibility_ConstructorsReturnedAndAccessible() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(MixedCtorClass.class);
        assertNotNull(ctors);
        assertEquals(2, ctors.length, "MixedCtorClass should expose two constructors");
        boolean foundPublic = false;
        boolean foundPrivate = false;
        for (Constructor<?> ctor : ctors) {
            if (Modifier.isPublic(ctor.getModifiers())) {
                foundPublic = true;
                // public ctor is already accessible
                assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Public constructor should be accessible");
            } else {
                foundPrivate = true;
                // The private ctor should have been made accessible by the utility method
                assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Private constructor should have been set accessible");
            }
        }
        assertTrue(foundPublic, "Public constructor not found");
        assertTrue(foundPrivate, "Private constructor not found");
    }

    @Test
    void testGetAllConstructors_PrivateNoArg_MadeAccessible() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(PrivateNoArg.class);
        assertEquals(1, ctors.length, "PrivateNoArg should have exactly one constructor");
        Constructor<?> ctor = ctors[0];
        assertFalse(Modifier.isPublic(ctor.getModifiers()), "Constructor should be non‑public");
        assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "The private constructor should have been set accessible");
    }

    @Test
    void testGetAllConstructors_CacheReturnsSameInstances() {
        // First call – populates the cache
        Constructor<?>[] firstCall = ReflectionUtils.getAllConstructors(MixedCtorClass.class);
        // Second call – should retrieve the same Constructor objects from the cache
        Constructor<?>[] secondCall = ReflectionUtils.getAllConstructors(MixedCtorClass.class);
        assertEquals(firstCall.length, secondCall.length, "Both calls must return the same number of constructors");
        for (int i = 0; i < firstCall.length; i++) {
            assertSame(firstCall[i], secondCall[i], "Constructor at index " + i + " should be the same instance from the cache");
        }
    }

    @Test
    void testGetAllConstructors_PublicCtor_ReturnsPublicConstructorUnchanged() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(PublicCtor.class);
        assertEquals(1, ctors.length, "PublicCtor should have exactly one constructor");
        Constructor<?> ctor = ctors[0];
        assertTrue(Modifier.isPublic(ctor.getModifiers()), "Constructor must be public");
        assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Public constructor should remain accessible");
    }

    @Test
    void testGetAllConstructors_PrivateCtor_MadeAccessible() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(PrivateCtor.class);
        assertEquals(1, ctors.length, "PrivateCtor should have exactly one constructor");
        Constructor<?> ctor = ctors[0];
        assertFalse(Modifier.isPublic(ctor.getModifiers()), "Constructor must be non‑public");
        assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Private constructor should have been made accessible");
    }

    @Test
    void testGetAllConstructors_MultiCtor_MixedVisibility() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(MultiCtor.class);
        assertEquals(3, ctors.length, "MultiCtor should expose three constructors");
        boolean foundPublic = false, foundProtected = false, foundPrivate = false;
        for (Constructor<?> ctor : ctors) {
            int mods = ctor.getModifiers();
            if (Modifier.isPublic(mods)) {
                foundPublic = true;
                assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Public constructor should be accessible");
            } else if (Modifier.isProtected(mods)) {
                foundProtected = true;
                assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Protected constructor should have been made accessible");
            } else {
                foundPrivate = true;
                assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Private constructor should have been made accessible");
            }
        }
        assertTrue(foundPublic && foundProtected && foundPrivate, "All visibility variants should be present");
    }

    @Test
    void testGetAllConstructors_PackagePrivateCtor_MadeAccessible() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(PackagePrivateCtor.class);
        assertEquals(1, ctors.length, "PackagePrivateCtor should have exactly one constructor");
        Constructor<?> ctor = ctors[0];
        assertFalse(Modifier.isPublic(ctor.getModifiers()), "Constructor should not be public");
        assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Package‑private constructor should have been made accessible");
    }

    @Test
    void testGetAllConstructors_ProtectedCtor_MadeAccessible() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ProtectedCtor.class);
        assertEquals(1, ctors.length, "ProtectedCtor should have exactly one constructor");
        Constructor<?> ctor = ctors[0];
        assertTrue(Modifier.isProtected(ctor.getModifiers()), "Constructor should be protected");
        assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Protected constructor should have been made accessible");
    }

    @Test
    void testGetAllConstructors_SyntheticInnerCtor_MadeAccessible() {
        // The inner class constructor is synthetic (takes Outer as first argument)
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(Outer.Inner.class);
        assertEquals(1, ctors.length, "Inner class should have one synthetic constructor");
        Constructor<?> ctor = ctors[0];
        assertFalse(Modifier.isPublic(ctor.getModifiers()), "Synthetic constructor is not public");
        assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Synthetic constructor should have been made accessible");
    }

    @Test
    void testGetAllConstructors_EnumConstructor_MadeAccessible() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(SampleEnum.class);
        // Enums always have at least one private constructor (the synthetic one for enum constants)
        assertTrue(ctors.length > 0, "Enum should have at least one constructor");
        for (Constructor<?> ctor : ctors) {
            assertFalse(Modifier.isPublic(ctor.getModifiers()), "Enum constructors are non‑public");
            assertTrue(ctor.canAccess(null) || ctor.isAccessible(), "Enum constructor should have been made accessible");
        }
    }

    @Test
    void testGetAllConstructors_PrimitiveType_ReturnsEmptyArray() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(int.class);
        assertEquals(0, ctors.length, "Primitive type should yield empty constructor array");
    }

    @Test
    void testGetAllConstructors_ArrayType_ReturnsEmptyArray() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(String[].class);
        assertEquals(0, ctors.length, "Array type should yield empty constructor array");
    }

    @Test
    void testCacheIsolation_DifferentClassesSameSignature() {
        // First class
        Constructor<?>[] first = ReflectionUtils.getAllConstructors(PrivateCtor.class);
        // Second class with identical signature
        Constructor<?>[] second = ReflectionUtils.getAllConstructors(AnotherPrivateCtor.class);
        assertEquals(1, first.length);
        assertEquals(1, second.length);
        assertNotSame(first[0], second[0], "Cache keys must be scoped per class – constructors should not be shared");
    }

    @Test
    void testConstructorOrderPreserved() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(OrderedCtor.class);
        assertEquals(3, ctors.length, "OrderedCtor should expose three constructors");
        // Expected order: (int), (String, int), ()
        Class<?>[] firstParams = ctors[0].getParameterTypes();
        Class<?>[] secondParams = ctors[1].getParameterTypes();
        Class<?>[] thirdParams = ctors[2].getParameterTypes();
        assertArrayEquals(new Class<?>[] { int.class }, firstParams, "First constructor parameters mismatch");
        assertArrayEquals(new Class<?>[] { String.class, int.class }, secondParams, "Second constructor parameters mismatch");
        assertEquals(0, thirdParams.length, "Third constructor should be the no‑arg constructor");
    }

    @Test
    void testNullConstructorCache_TriggersNullPointerException() throws Exception {
        // Replace cache with null
        var cacheField = ReflectionUtils.class.getDeclaredField("CONSTRUCTOR_CACHE");
        cacheField.setAccessible(true);
        cacheField.set(null, null);
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllConstructors(Object.class), "When CONSTRUCTOR_CACHE is null a NullPointerException should be thrown");
    }

    @Test
    void testUnmodifiableConstructorCache_ThrowsUnsupportedOperationException() throws Exception {
        var cacheField = ReflectionUtils.class.getDeclaredField("CONSTRUCTOR_CACHE");
        cacheField.setAccessible(true);
        cacheField.set(null, Collections.unmodifiableMap(new HashMap<>()));
        assertThrows(UnsupportedOperationException.class, () -> ReflectionUtils.getAllConstructors(PrivateCtor.class), "Storing into an unmodifiable cache must throw UnsupportedOperationException");
    }

    @Test
    void testConstructorCacheCustomException_PropagatesRuntimeException() throws Exception {
        // Custom map that throws RuntimeException from computeIfAbsent
        class ExceptionThrowingMap<K, V> extends HashMap<K, V> {

            @Override
            public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
                throw new RuntimeException("forced exception");
            }
        }
        var cacheField = ReflectionUtils.class.getDeclaredField("CONSTRUCTOR_CACHE");
        cacheField.setAccessible(true);
        cacheField.set(null, new ExceptionThrowingMap<>());
        assertThrows(RuntimeException.class, () -> ReflectionUtils.getAllConstructors(ProtectedCtor.class), "Custom map exception should propagate as RuntimeException");
    }

    @Test
    void testSecurityManagerDeniesReflection_ThrowsSecurityException() throws Exception {
        // Save original security manager
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive security manager
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkMemberAccess(Class<?> clazz, int which) {
                throw new SecurityException("access denied");
            }
        });
        try {
            assertThrows(SecurityException.class, () -> ReflectionUtils.getAllConstructors(PrivateCtor.class), "SecurityManager should block reflective access");
        } finally {
            // Restore original security manager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
