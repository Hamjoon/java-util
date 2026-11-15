package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllConstructors_Test {

    // No‑arg interface – has no declared constructors
    interface NoCtorInterface {
    }

    // Class with a public and a private constructor
    static class MixedCtorClass {

        public MixedCtorClass(int i) {
            /* public ctor */
        }

        private MixedCtorClass() {
            /* private ctor */
        }
    }

    // Simple class with a single public no‑arg constructor
    static class PublicNoArg {

        public PublicNoArg() {
        }
    }

    // Class with a single private constructor
    static class PrivateCtor {

        private PrivateCtor() {
        }
    }

    // Class with three constructors of different visibility
    static class MixedCtors {

        public MixedCtors() {
        }

        private MixedCtors(int i) {
        }

        protected MixedCtors(String s) {
        }
    }

    // Class with a package‑private (default) constructor
    static class DefaultCtor {

        DefaultCtor() {
        }
    }

    // Abstract class with a protected constructor
    abstract static class AbstractProtectedCtor {

        protected AbstractProtectedCtor() {
        }
    }

    // Class used to verify caching – same as MixedCtorClass but named differently for clarity
    static class CachedCtors {

        public CachedCtors() {
        }

        private CachedCtors(int i) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Clear the internal constructor cache before each test to avoid cross‑test contamination.
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    @Test
    void getAllConstructors_nullClass_returnsEmptyArray() {
        Constructor<?>[] result = ReflectionUtils.getAllConstructors(null);
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.length, "Null input must yield an empty array");
    }

    @Test
    void getAllConstructors_interfaceWithoutConstructors_returnsEmptyArray() {
        Constructor<?>[] result = ReflectionUtils.getAllConstructors(NoCtorInterface.class);
        assertNotNull(result);
        assertEquals(0, result.length, "Interface without constructors should return empty array");
    }

    @Test
    void getAllConstructors_mixedVisibilityConstructors_returnsBothAndMakesPrivateAccessible() throws Exception {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(MixedCtorClass.class);
        assertNotNull(ctors);
        assertEquals(2, ctors.length, "MixedCtorClass should expose two constructors");
        Constructor<?> privateCtor = null;
        Constructor<?> publicCtor = null;
        for (Constructor<?> c : ctors) {
            if (c.getParameterCount() == 0) {
                privateCtor = c;
            } else if (c.getParameterCount() == 1) {
                publicCtor = c;
            }
        }
        assertNotNull(privateCtor, "Private zero‑arg constructor should be present");
        assertNotNull(publicCtor, "Public one‑arg constructor should be present");
        // The private constructor must have been made accessible by getAllConstructors()
        // (canAccess(null) returns true if the constructor is accessible for static contexts)
        assertTrue(privateCtor.canAccess(null), "Private constructor should be made accessible by ReflectionUtils");
        // Verify we can actually instantiate using the private constructor without IllegalAccessException
        Object instance = privateCtor.newInstance();
        assertNotNull(instance, "Instantiating via the now‑accessible private constructor should succeed");
        assertTrue(instance instanceof MixedCtorClass);
    }

    @Test
    void getAllConstructors_isCached_betweenCalls() {
        Constructor<?>[] firstCall = ReflectionUtils.getAllConstructors(MixedCtorClass.class);
        Constructor<?>[] secondCall = ReflectionUtils.getAllConstructors(MixedCtorClass.class);
        assertEquals(firstCall.length, secondCall.length, "Both calls must return the same number of constructors");
        for (int i = 0; i < firstCall.length; i++) {
            // The same Constructor objects (identical references) must be returned because of caching
            assertSame(firstCall[i], secondCall[i], "Constructor at index " + i + " should be cached and identical across calls");
        }
    }

    @Test
    void getAllConstructors_classWithOnlyPublicNoArgConstructor_returnsIt() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(PublicNoArg.class);
        assertNotNull(ctors);
        assertEquals(1, ctors.length, "PublicNoArg should have exactly one constructor");
        Constructor<?> ctor = ctors[0];
        assertEquals(0, ctor.getParameterCount(), "Constructor should have no parameters");
        assertTrue(Modifier.isPublic(ctor.getModifiers()), "Constructor must be public");
        assertTrue(ctor.canAccess(null), "Public constructor should be accessible");
    }

    @Test
    void getAllConstructors_classWithPrivateConstructor_makesItAccessible() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(PrivateCtor.class);
        assertNotNull(ctors);
        assertEquals(1, ctors.length, "PrivateCtor should expose one constructor");
        Constructor<?> ctor = ctors[0];
        assertTrue(Modifier.isPrivate(ctor.getModifiers()), "Constructor should be private");
        assertTrue(ctor.canAccess(null), "Private constructor must have been made accessible");
    }

    @Test
    void getAllConstructors_classWithMultipleConstructors_allReturnedAndAccessible() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(MixedCtors.class);
        assertNotNull(ctors);
        assertEquals(3, ctors.length, "MixedCtors should expose three constructors");
        boolean foundPublic = false, foundPrivate = false, foundProtected = false;
        for (Constructor<?> c : ctors) {
            int mods = c.getModifiers();
            if (Modifier.isPublic(mods)) {
                foundPublic = true;
            } else if (Modifier.isPrivate(mods)) {
                foundPrivate = true;
            } else if (Modifier.isProtected(mods)) {
                foundProtected = true;
            }
            // Every constructor returned by the utility should be accessible
            assertTrue(c.canAccess(null), "Constructor " + c + " should be accessible");
        }
        assertTrue(foundPublic, "Public constructor should be present");
        assertTrue(foundPrivate, "Private constructor should be present");
        assertTrue(foundProtected, "Protected constructor should be present");
    }

    @Test
    void getAllConstructors_classWithPackagePrivateConstructor_makesItAccessible() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(DefaultCtor.class);
        assertNotNull(ctors);
        assertEquals(1, ctors.length, "DefaultCtor should have exactly one constructor");
        Constructor<?> ctor = ctors[0];
        assertFalse(Modifier.isPublic(ctor.getModifiers()), "Constructor should not be public");
        assertFalse(Modifier.isProtected(ctor.getModifiers()));
        assertFalse(Modifier.isPrivate(ctor.getModifiers()));
        assertTrue(ctor.canAccess(null), "Package‑private constructor must have been made accessible");
    }

    @Test
    void getAllConstructors_abstractClassWithProtectedConstructor_makesItAccessible() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(AbstractProtectedCtor.class);
        assertNotNull(ctors);
        assertEquals(1, ctors.length, "AbstractProtectedCtor should expose its protected constructor");
        Constructor<?> ctor = ctors[0];
        assertTrue(Modifier.isProtected(ctor.getModifiers()), "Constructor should be protected");
        assertTrue(ctor.canAccess(null), "Protected constructor must be made accessible");
    }

    @Test
    void getAllConstructors_nullConstructorCache_throwsNullPointerException() {
        // Set the internal cache to null
        ReflectionUtils.setConstructorCache(null);
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllConstructors(PublicNoArg.class), "When the constructor cache is null a NullPointerException should be thrown");
    }

    @Test
    void getAllConstructors_unmodifiableConstructorCache_throwsUnsupportedOperationException() {
        // Provide an unmodifiable map – computeIfAbsent will try to modify it and fail
        ReflectionUtils.setConstructorCache(Collections.unmodifiableMap(new HashMap<>()));
        assertThrows(UnsupportedOperationException.class, () -> ReflectionUtils.getAllConstructors(PublicNoArg.class), "An unmodifiable cache should cause UnsupportedOperationException on computeIfAbsent");
    }

    @Test
    void getAllConstructors_securityManagerDeniesAccess_throwsSecurityException() {
        // Save the original security manager so we can restore it later
        SecurityManager original = System.getSecurityManager();
        // Install a security manager that denies reflective suppressAccessChecks permission
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except the specific reflect permission used by setAccessible
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
            }
        });
        try {
            // System class has a private constructor; attempting to make it accessible should trigger the exception
            assertThrows(SecurityException.class, () -> ReflectionUtils.getAllConstructors(System.class), "A SecurityException should propagate when access to constructors is denied");
        } finally {
            // Restore the original security manager to avoid side effects on other tests
            System.setSecurityManager(original);
        }
    }

    @Test
    void getAllConstructors_cachingConsistencyAcrossCalls_onCachedCtorsClass() {
        Constructor<?>[] first = ReflectionUtils.getAllConstructors(CachedCtors.class);
        Constructor<?>[] second = ReflectionUtils.getAllConstructors(CachedCtors.class);
        assertEquals(first.length, second.length, "Both calls must return the same number of constructors");
        for (int i = 0; i < first.length; i++) {
            assertSame(first[i], second[i], "Constructor at index " + i + " should be identical across calls (cached)");
        }
    }
}
