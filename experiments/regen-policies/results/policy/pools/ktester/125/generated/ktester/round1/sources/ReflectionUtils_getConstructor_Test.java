package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import java.security.reflect.ReflectPermission;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getConstructor_Test {

    /* -----------------------------------------------------------
       Test fixtures – simple classes with various constructor
       visibilities and signatures used by the test methods.
       ----------------------------------------------------------- */
    public static class PublicNoArg {

        public PublicNoArg() {
        }
    }

    public static class PublicWithArgs {

        public PublicWithArgs(String s, int i) {
        }
    }

    static class PackagePrivateNoArg {

        PackagePrivateNoArg() {
        }
    }

    private static class PrivateNoArg {

        private PrivateNoArg() {
        }
    }

    public static class PublicParam {

        public PublicParam(String s, int i) {
        }
    }

    private static class PrivateCtor {

        private PrivateCtor() {
        }
    }

    public static class PrimitiveCtor {

        public PrimitiveCtor(int i) {
        }
    }

    public static abstract class AbstractBase {

        protected AbstractBase() {
        }
    }

    // Used only for the “SecurityExceptionDuringGetDeclaredConstructor” case.
    public static class RestrictedConstructor {

        public RestrictedConstructor() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Replace the internal cache with a fresh map to avoid cross‑test contamination.
        ReflectionUtils.setConstructorCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the default security manager if a test replaced it.
        System.setSecurityManager(null);
    }

    static Stream<Arguments> primitiveVsWrapperProvider() {
        return Stream.of(Arguments.of(PrimitiveCtor.class, new Class[] { int.class }, true), Arguments.of(PrimitiveCtor.class, new Class[] { Integer.class }, false));
    }

    @Test
    void getPublicNoArgConstructor_returnsConstructor() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PublicNoArg.class);
        assertNotNull(ctor, "Constructor should not be null for a public no‑arg ctor");
        assertTrue(Modifier.isPublic(ctor.getModifiers()));
        assertEquals(0, ctor.getParameterCount());
    }

    @Test
    void getPublicWithArgsConstructor_returnsConstructor() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PublicWithArgs.class, String.class, int.class);
        assertNotNull(ctor, "Constructor should not be null for a matching public ctor");
        assertEquals(2, ctor.getParameterCount());
        assertEquals(String.class, ctor.getParameterTypes()[0]);
        assertEquals(int.class, ctor.getParameterTypes()[1]);
    }

    @Test
    void getPackagePrivateConstructor_isReturnedAndInstantiable() throws Exception {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PackagePrivateNoArg.class);
        assertNotNull(ctor, "Package‑private constructor should be returned");
        // The constructor is not public; ReflectionUtils should have called setAccessible(true)
        // Verify that we can actually instantiate the class via the returned constructor.
        Object instance = ctor.newInstance();
        assertNotNull(instance);
        assertTrue(instance instanceof PackagePrivateNoArg);
    }

    @Test
    void getPrivateConstructor_isReturnedAndInstantiable() throws Exception {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PrivateNoArg.class);
        assertNotNull(ctor, "Private constructor should be returned");
        // Verify that the constructor can be used despite its private visibility.
        Object instance = ctor.newInstance();
        assertNotNull(instance);
        assertTrue(instance instanceof PrivateNoArg);
    }

    @Test
    void getConstructor_cachesResult() {
        Constructor<?> first = ReflectionUtils.getConstructor(PublicNoArg.class);
        Constructor<?> second = ReflectionUtils.getConstructor(PublicNoArg.class);
        assertSame(first, second, "Subsequent calls should return the cached Constructor instance");
    }

    @Test
    void getConstructor_nonExistent_returnsNull() {
        // No such constructor (different signature)
        Constructor<?> ctor = ReflectionUtils.getConstructor(PublicNoArg.class, String.class);
        assertNull(ctor, "Should return null when the constructor does not exist");
    }

    @Test
    void getConstructor_nullClass_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getConstructor(null));
        assertTrue(ex.getMessage().contains("class cannot be null"));
    }

    // ---------- NullClassParameter ----------
    @Test
    void nullClassParameter_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getConstructor(null));
        assertTrue(ex.getMessage().contains("class cannot be null"));
    }

    // ---------- PrimitiveVsWrapperParameter ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("primitiveVsWrapperProvider")
    void primitiveVsWrapperParameter(Class<?> clazz, Class<?>[] paramTypes, boolean expectNonNull) {
        Constructor<?> ctor = ReflectionUtils.getConstructor(clazz, paramTypes);
        if (expectNonNull) {
            assertNotNull(ctor, "Expected a non‑null constructor");
        } else {
            assertNull(ctor, "Expected null because primitive/wrapper mismatch");
        }
    }

    // ---------- NullParameterTypesArray ----------
    @Test
    void nullParameterTypesArray_returnsNull() {
        // The reflective call will throw a NullPointerException which is caught.
        Constructor<?> ctor = ReflectionUtils.getConstructor(PublicNoArg.class, (Class<?>[]) null);
        assertNull(ctor, "When parameterTypes is null the method should return null");
    }

    // ---------- InterfaceClass ----------
    @Test
    void interfaceClass_returnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(List.class);
        assertNull(ctor, "Interfaces have no constructors");
    }

    // ---------- AbstractClassConstructor ----------
    @Test
    void abstractClassConstructor_isFoundEvenIfClassIsAbstract() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(AbstractBase.class);
        assertNotNull(ctor, "Abstract class with a protected ctor should be returned");
        assertEquals(0, ctor.getParameterCount());
    }

    // ---------- CacheSeparateKeys ----------
    @Test
    void cacheSeparateKeys_independentEntries() {
        Constructor<?> ctorA = ReflectionUtils.getConstructor(PublicNoArg.class);
        assertNotNull(ctorA);
        Constructor<?> ctorB = ReflectionUtils.getConstructor(PublicParam.class, String.class);
        // No matching ctor → null, but it must be cached independently of the first entry.
        assertNull(ctorB);
    }

    // ---------- NullElementInParameterTypes ----------
    @Test
    void nullElementInParameterTypes_returnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(PublicParam.class, new Class[] { null });
        assertNull(ctor, "A null element in the parameterTypes array should lead to a null result");
    }

    // ---------- PrimitiveClassParameter ----------
    @Test
    void primitiveClassParameter_returnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(int.class);
        assertNull(ctor, "Primitive types have no constructors");
    }

    // ---------- ArrayClassParameter ----------
    @Test
    void arrayClassParameter_returnsNull() {
        Constructor<?> ctor = ReflectionUtils.getConstructor(String[].class);
        assertNull(ctor, "Array classes have no constructors");
    }

    // ---------- SecurityExceptionDuringSetAccessible ----------
    @Test
    void securityExceptionDuringSetAccessible_isIgnoredAndConstructorReturned() {
        // Install a SecurityManager that denies suppressAccessChecks (setAccessible) but
        // allows reflective lookup of the constructor itself.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    // Called from Constructor.setAccessible → throw.
                    throw new SecurityException("setAccessible denied");
                }
                // Allow everything else.
            }
        });
        Constructor<?> ctor = ReflectionUtils.getConstructor(PrivateCtor.class);
        assertNotNull(ctor, "Constructor should be returned even when setAccessible throws");
        // The constructor may still be inaccessible; we only verify that the method did not fail.
    }

    // ---------- SecurityExceptionDuringGetDeclaredConstructor ----------
    @Test
    void securityExceptionDuringGetDeclaredConstructor_returnsNull() {
        // SecurityManager that denies reflective lookup of constructors (getDeclaredConstructor)
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    // Allow setAccessible – we never reach it because lookup fails first.
                    return;
                }
                // For any reflective access that requires a ReflectPermission, reject.
                if (perm instanceof ReflectPermission) {
                    throw new SecurityException("getDeclaredConstructor denied");
                }
            }
        });
        Constructor<?> ctor = ReflectionUtils.getConstructor(RestrictedConstructor.class);
        assertNull(ctor, "When getDeclaredConstructor throws SecurityException the method should return null");
    }

    // ---------- ConstructorCacheKeyEqualityWithDifferentArrayInstances ----------
    @Test
    void constructorCacheKeyEqualityWithDifferentArrayInstances_returnsSameInstance() {
        Class<?>[] firstArray = new Class[] { String.class, int.class };
        // distinct instance
        Class<?>[] secondArray = new Class[] { String.class, int.class };
        Constructor<?> first = ReflectionUtils.getConstructor(PublicParam.class, firstArray);
        Constructor<?> second = ReflectionUtils.getConstructor(PublicParam.class, secondArray);
        assertNotNull(first);
        assertSame(first, second, "Cache must treat identical parameter type contents as the same key");
    }

    // ---------- CacheReuseSameKey ----------
    @Test
    void cacheReuseSameKey_returnsSameConstructorInstance() {
        Constructor<?> first = ReflectionUtils.getConstructor(PublicNoArg.class);
        Constructor<?> second = ReflectionUtils.getConstructor(PublicNoArg.class);
        assertSame(first, second, "Two identical look‑ups must hit the cache");
    }
}
