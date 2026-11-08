package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Proxy;
import java.security.Permission;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassLoaderName_Test {

    static Stream<Arguments> bootstrapClassProvider() {
        return Stream.of(Arguments.of("java.lang.String (bootstrap class)", String.class), Arguments.of("int[] (bootstrap array type)", int[].class), Arguments.of("int primitive (bootstrap primitive)", int.class));
    }

    static Stream<Arguments> nonBootstrapClassProvider() {
        return Stream.of(Arguments.of("ReflectionUtils class (application loader)", ReflectionUtils.class), Arguments.of("Test class itself (application loader)", ReflectionUtils_getClassLoaderName_Test.class));
    }

    /**
     * Verifies that a class loaded by the bootstrap class loader
     * (i.e. {@code null} from {@link Class#getClassLoader()}) yields the
     * literal string {@code "bootstrap"}.
     */
    @Test
    void getClassLoaderName_returnsBootstrapForBootstrapClass() {
        // java.lang.String is loaded by the bootstrap class loader.
        String result = ReflectionUtils.getClassLoaderName(String.class);
        assertEquals("bootstrap", result, "Bootstrap‑loaded classes should return the literal \"bootstrap\".");
    }

    /**
     * Verifies that a class loaded by the application class loader
     * returns the {@code toString()} representation of that loader.
     */
    @Test
    void getClassLoaderName_returnsLoaderToStringForAppClass() {
        // This test class itself is loaded by the application (or platform) class loader.
        Class<?> clazz = ReflectionUtils_getClassLoaderName_Test.class;
        // should be non‑null
        ClassLoader loader = clazz.getClassLoader();
        assertNotNull(loader, "The test class should have a non‑null class loader.");
        String expected = loader.toString();
        String actual = ReflectionUtils.getClassLoaderName(clazz);
        assertEquals(expected, actual, "The method should return the class loader's toString() value for non‑bootstrap classes.");
    }

    @Test
    void getClassLoaderName_returnsBootstrapForArrayClass() {
        // Array types are defined by the bootstrap loader as well.
        String result = ReflectionUtils.getClassLoaderName(int[].class);
        assertEquals("bootstrap", result, "Array classes defined by the bootstrap loader must also return \"bootstrap\".");
    }

    @Test
    void getClassLoaderName_returnsBootstrapForPrimitive() {
        // Primitive types have no associated ClassLoader (null).
        String result = ReflectionUtils.getClassLoaderName(int.class);
        assertEquals("bootstrap", result, "Primitive types should be treated as bootstrap‑loaded.");
    }

    @Test
    void getClassLoaderName_returnsLoaderToStringForReflectionUtils() {
        // ReflectionUtils lives in the same application class loader as this test.
        Class<?> clazz = ReflectionUtils.class;
        ClassLoader loader = clazz.getClassLoader();
        assertNotNull(loader, "ReflectionUtils should have a non‑null class loader.");
        String expected = loader.toString();
        String actual = ReflectionUtils.getClassLoaderName(clazz);
        assertEquals(expected, actual, "Application‑loaded classes must return the loader's toString() value.");
    }

    @Test
    void getClassLoaderName_nullArgument_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassLoaderName(null), "Passing null should result in a NullPointerException.");
    }

    @Test
    void getClassLoaderName_customClassLoader_returnsCustomToString() {
        // Create a mock ClassLoader whose toString() returns a recognizable value.
        ClassLoader customLoader = mock(ClassLoader.class);
        when(customLoader.toString()).thenReturn("custom-loader");
        // Mock a Class<?> instance to return the custom loader.
        @SuppressWarnings("unchecked")
        Class<Object> mockedClass = (Class<Object>) mock(Class.class);
        when(mockedClass.getClassLoader()).thenReturn(customLoader);
        // The method under test should forward the loader's toString().
        String result = ReflectionUtils.getClassLoaderName(mockedClass);
        assertEquals("custom-loader", result, "When a class is loaded by a custom loader, its toString() should be returned.");
    }

    @Test
    void getClassLoaderName_proxyClass_returnsNonBootstrap() {
        // Create a dynamic proxy for Runnable using the same loader as ReflectionUtils.
        ClassLoader loader = ReflectionUtils.class.getClassLoader();
        Object proxy = Proxy.newProxyInstance(loader, new Class[] { Runnable.class }, (p, m, args) -> null);
        Class<?> proxyClass = proxy.getClass();
        assertNotNull(proxyClass.getClassLoader(), "Proxy class should have a non‑null class loader.");
        String result = ReflectionUtils.getClassLoaderName(proxyClass);
        assertNotEquals("bootstrap", result, "Proxy classes are not loaded by the bootstrap loader.");
        assertEquals(loader.toString(), result, "Proxy class loader's toString() should be returned.");
    }

    @Test
    void getClassLoaderName_securityManagerDenial_propagatesSecurityException() {
        // Save the original security manager so we can restore it later.
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive SecurityManager that denies RuntimePermission "getClassLoader".
        SecurityManager denyingManager = new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except the specific permission we want to block.
                if ("getClassLoader".equals(perm.getName())) {
                    throw new SecurityException("Access to ClassLoader denied by test SecurityManager");
                }
            }
        };
        System.setSecurityManager(denyingManager);
        try {
            // The call should now throw SecurityException.
            assertThrows(SecurityException.class, () -> ReflectionUtils.getClassLoaderName(ReflectionUtils.class), "When the SecurityManager denies getClassLoader, the method must propagate the exception.");
        } finally {
            // Restore the original security manager to avoid side‑effects on other tests.
            System.setSecurityManager(original);
        }
    }

    /**
     * Parameterized test that checks that bootstrap‑loaded classes
     * (including primitives and arrays) return the literal "bootstrap".
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bootstrapClassProvider")
    void getClassLoaderName_bootstrapClasses(String description, Class<?> clazz) {
        String result = ReflectionUtils.getClassLoaderName(clazz);
        assertEquals("bootstrap", result, description + " should return \"bootstrap\".");
    }

    /**
     * Parameterized test for any non‑bootstrap class where we only assert
     * that the result is *not* the literal "bootstrap".
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nonBootstrapClassProvider")
    void getClassLoaderName_nonBootstrapClasses(String description, Class<?> clazz) {
        String result = ReflectionUtils.getClassLoaderName(clazz);
        assertNotEquals("bootstrap", result, description + " must not return \"bootstrap\".");
    }
}
