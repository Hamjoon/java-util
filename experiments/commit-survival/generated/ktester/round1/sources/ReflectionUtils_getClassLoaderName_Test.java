package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.Permission;
import java.security.RuntimePermission;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassLoaderName_Test {

    /**
     * Simple class that will be loaded by a custom {@link URLClassLoader}
     * whose {@code toString()} returns {@code "custom-loader"}.
     */
    public static class DummyClassLoadedByCustomLoader {
        // No content needed; the class merely serves as a loadable type.
    }

    /**
     * Simple class that will be loaded by a custom loader whose {@code toString()}
     * deliberately throws a {@link RuntimeException}.
     */
    public static class ClassWithFaultyLoader {
        // No content needed.
    }

    /**
     * Custom {@link URLClassLoader} that overrides {@code toString()} to return a
     * deterministic identifier.
     */
    private static class CustomNameClassLoader extends URLClassLoader {

        CustomNameClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public String toString() {
            return "custom-loader";
        }
    }

    /**
     * Custom {@link URLClassLoader} whose {@code toString()} throws a
     * {@link RuntimeException}. Used to test exception propagation.
     */
    private static class FaultyToStringClassLoader extends URLClassLoader {

        FaultyToStringClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public String toString() {
            throw new RuntimeException("faulty toString");
        }
    }

    /**
     * Verify that a class loaded by the bootstrap class loader yields the literal
     * string {@code "bootstrap"}.
     */
    @Test
    void getClassLoaderName_returnsBootstrap_forBootstrapLoadedClass() {
        // java.lang.String is loaded by the bootstrap class loader.
        String result = ReflectionUtils.getClassLoaderName(String.class);
        assertEquals("bootstrap", result, "Expected 'bootstrap' for a class loaded by the bootstrap class loader");
    }

    /**
     * Verify that a class loaded by a custom {@link URLClassLoader} returns the
     * {@code toString()} representation of that loader.
     */
    @Test
    void getClassLoaderName_returnsLoaderToString_forCustomClassLoader() throws Exception {
        // Use the same classpath URL that loaded this test class.
        URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
        try (URLClassLoader customLoader = new URLClassLoader(urls, null)) {
            // Load a known class (the test class itself) with the custom loader.
            Class<?> loaded = Class.forName(this.getClass().getName(), true, customLoader);
            String expected = customLoader.toString();
            String actual = ReflectionUtils.getClassLoaderName(loaded);
            assertEquals(expected, actual, "The returned class loader name should match the custom loader's toString()");
        }
    }

    /**
     * Verify that passing {@code null} as the class argument results in a
     * {@link NullPointerException}.
     */
    @Test
    void getClassLoaderName_throwsNPE_whenClassIsNull() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassLoaderName(null), "Calling getClassLoaderName with a null Class should throw NPE");
    }

    /**
     * Verify that a class loaded by the application (system) class loader returns
     * the loader's {@code toString()} value.
     */
    @Test
    void getClassLoaderName_returnsAppLoader_toString_forApplicationClass() {
        // loaded by the application class loader
        Class<?> clazz = ReflectionUtils.class;
        ClassLoader loader = clazz.getClassLoader();
        assertNotNull(loader, "Application class loader should not be null for ReflectionUtils");
        String expected = loader.toString();
        String actual = ReflectionUtils.getClassLoaderName(clazz);
        assertEquals(expected, actual, "Expected the application class loader's toString() for ReflectionUtils");
    }

    /**
     * Verify that a JUnit class (loaded by the same application loader as the test)
     * yields the application loader's {@code toString()}.
     */
    @Test
    void getClassLoaderName_returnsAppLoader_toString_forJUnitClass() {
        Class<?> clazz = org.junit.jupiter.api.Test.class;
        ClassLoader loader = clazz.getClassLoader();
        assertNotNull(loader, "JUnit classes should be loaded by a non‑null class loader");
        String expected = loader.toString();
        String actual = ReflectionUtils.getClassLoaderName(clazz);
        assertEquals(expected, actual, "Expected the application class loader's toString() for JUnit's @Test annotation");
    }

    /**
     * Parameterized test covering a set of classes that are loaded by the bootstrap
     * class loader and therefore should return {@code "bootstrap"}.
     */
    @TestFactory
    List<DynamicTest> getClassLoaderName_returnsBootstrap_forVariousBootstrapClasses() {
        List<Class<?>> bootstrapClasses = List.of(java.util.ArrayList.class, int.class, java.lang.String[].class, java.util.List.class);
        List<DynamicTest> tests = new ArrayList<>();
        for (Class<?> c : bootstrapClasses) {
            String name = "Class " + c.getName();
            tests.add(DynamicTest.dynamicTest(name, () -> {
                String actual = ReflectionUtils.getClassLoaderName(c);
                assertEquals("bootstrap", actual, "Expected 'bootstrap' for class " + c.getName());
            }));
        }
        return tests;
    }

    /**
     * Verify that a class loaded by a custom loader whose {@code toString()} returns
     * a fixed value ("custom-loader") results in that exact string.
     */
    @Test
    void getClassLoaderName_returnsCustomLoaderString_forDummyClass() throws Exception {
        URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
        try (CustomNameClassLoader customLoader = new CustomNameClassLoader(urls, null)) {
            Class<?> loaded = Class.forName(DummyClassLoadedByCustomLoader.class.getName(), true, customLoader);
            String actual = ReflectionUtils.getClassLoaderName(loaded);
            assertEquals("custom-loader", actual, "Expected the custom loader's overridden toString() value");
        }
    }

    /**
     * Verify that a {@link SecurityException} thrown by the JVM when a
     * {@link SecurityManager} denies the {@code getClassLoader} permission is
     * propagated by {@code getClassLoaderName}.
     */
    @Test
    void getClassLoaderName_propagatesSecurityException_whenAccessDenied() {
        // Install a SecurityManager that denies RuntimePermission "getClassLoader".
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof RuntimePermission && "getClassLoader".equals(perm.getName())) {
                    throw new SecurityException("getClassLoader permission denied");
                }
                // Allow everything else.
            }
        });
        try {
            // Any class will trigger the check; use ReflectionUtils itself.
            assertThrows(SecurityException.class, () -> ReflectionUtils.getClassLoaderName(ReflectionUtils.class), "Expected SecurityException when getClassLoader permission is denied");
        } finally {
            // Restore the original SecurityManager to avoid side‑effects.
            System.setSecurityManager(original);
        }
    }

    /**
     * Verify that a {@link RuntimeException} thrown from a loader's {@code toString()}
     * propagates out of {@code getClassLoaderName}.
     */
    @Test
    void getClassLoaderName_propagatesRuntimeException_fromFaultyLoaderToString() throws Exception {
        URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
        try (FaultyToStringClassLoader faultyLoader = new FaultyToStringClassLoader(urls, null)) {
            Class<?> loaded = Class.forName(ClassWithFaultyLoader.class.getName(), true, faultyLoader);
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> ReflectionUtils.getClassLoaderName(loaded), "Expected RuntimeException from faulty loader's toString()");
            assertEquals("faulty toString", thrown.getMessage(), "Exception message should match the one thrown by the loader");
        }
    }
}
