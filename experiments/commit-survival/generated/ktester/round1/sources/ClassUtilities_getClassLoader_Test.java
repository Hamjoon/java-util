package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.security.Permission;
import java.security.RuntimePermission;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_getClassLoader_Test {

    private SecurityManager originalSecurityManager;

    class Dummy {
    }

    class CustomLoader extends ClassLoader {

        CustomLoader() {
            super(CustomLoader.class.getClassLoader());
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if ("com.example.Fake".equals(name)) {
                // For the purpose of the test we simply return an existing class.
                // The actual class object is irrelevant; we only care about the loader.
                return Long.class;
            }
            throw new ClassNotFoundException(name);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Preserve the original security manager so we can restore it after each test
        originalSecurityManager = System.getSecurityManager();
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original security manager (if any)
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * Verify that the no‑arg overload returns the same loader as the overload that
     * receives the ClassUtilities class itself as the anchor.
     */
    @Test
    void testGetClassLoader_NoArgEqualsAnchorClass() {
        ClassLoader noArg = ClassUtilities.getClassLoader();
        ClassLoader anchor = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNotNull(noArg, "Class loader should never be null");
        assertSame(anchor, noArg, "The no‑arg version should delegate to the anchor‑class version");
    }

    /**
     * Verify that the overload with a concrete anchor class returns that class's
     * defining loader.
     */
    @Test
    void testGetClassLoader_WithConcreteAnchor() {
        // Use an inner class to have a predictable loader (the system loader)
        class Dummy {
        }
        ClassLoader expected = Dummy.class.getClassLoader();
        ClassLoader actual = ClassUtilities.getClassLoader(Dummy.class);
        assertSame(expected, actual, "Loader returned should be the class's defining loader");
    }

    /**
     * When a null anchor is supplied the utility should fall back to the current
     * thread's context class loader (the typical contract for such helpers).
     */
    @Test
    void testGetClassLoader_WithNullAnchor() {
        ClassLoader expected = Thread.currentThread().getContextClassLoader();
        ClassLoader actual = ClassUtilities.getClassLoader(null);
        assertSame(expected, actual, "Null anchor should yield the thread context class loader");
    }

    /**
     * Verify that a class loaded by a custom ClassLoader is correctly reported.
     */
    @Test
    void testGetClassLoader_WithCustomClassLoader() throws Exception {
        // Custom loader that pretends to load a class named "com.example.Fake"
        class CustomLoader extends ClassLoader {

            CustomLoader() {
                super(CustomLoader.class.getClassLoader());
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if ("com.example.Fake".equals(name)) {
                    // For the purpose of the test we simply return an existing class.
                    // The actual class object is irrelevant; we only care about the loader.
                    return Long.class;
                }
                throw new ClassNotFoundException(name);
            }
        }
        CustomLoader customLoader = new CustomLoader();
        // Load a class via the custom loader – the returned Class object will have
        // customLoader as its defining loader.
        Class<?> fakeClass = Class.forName("com.example.Fake", true, customLoader);
        assertSame(customLoader, fakeClass.getClassLoader(), "The class should be defined by the custom loader");
        ClassLoader fromUtility = ClassUtilities.getClassLoader(fakeClass);
        assertSame(customLoader, fromUtility, "ClassUtilities.getClassLoader should return the custom loader for the given class");
    }

    /**
     * When the anchor class is loaded by the bootstrap class loader (e.g. {@link String}),
     * {@code getClassLoader} should return {@code null}.
     */
    @Test
    void testGetClassLoader_BootstrapClassLoader() {
        ClassLoader loader = ClassUtilities.getClassLoader(String.class);
        assertNull(loader, "Bootstrap‑loaded classes are represented by a null loader");
    }

    /**
     * If the anchor class loader is {@code null} the method must fall back to the
     * thread context class loader.
     */
    @Test
    void testGetClassLoader_AnchorLoaderNull_FallbackToThreadContext() {
        // {@link String} is bootstrap‑loaded → anchor loader is null
        ClassLoader threadLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader result = ClassUtilities.getClassLoader(String.class);
        assertSame(threadLoader, result, "When anchor loader is null the thread context loader should be used");
    }

    /**
     * When both the anchor loader and the thread context loader are {@code null},
     * the system class loader should be returned.
     */
    @Test
    void testGetClassLoader_BothNull_FallbackToSystem() {
        // Use a class that is bootstrap‑loaded and explicitly set the thread context loader to null
        Thread.currentThread().setContextClassLoader(null);
        ClassLoader system = ClassLoader.getSystemClassLoader();
        ClassLoader result = ClassUtilities.getClassLoader(String.class);
        assertSame(system, result, "When both anchor and thread loaders are null the system loader should be used");
    }

    /**
     * Passing {@code null} as the anchor class should result in a {@link NullPointerException}
     * (the implementation attempts to call {@code getClassLoader()} on the argument).
     */
    @Test
    void testGetClassLoader_NullAnchor_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.getClassLoader(null), "Calling with a null anchor class should throw NPE");
    }

    /**
     * Simulate a {@link SecurityException} being thrown from all three attempts to obtain a
     * loader (anchor, thread context, system). The method should swallow the exceptions
     * and finally return {@code null}.
     */
    @Test
    void testGetClassLoader_AllSecurityExceptions_ReturnsNull() {
        // Install a SecurityManager that denies every getClassLoader permission
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(RuntimePermission perm, Object context) {
                // allow everything else
            }

            // The critical part: deny any attempt to obtain a ClassLoader
            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof RuntimePermission && "getClassLoader".equals(perm.getName())) {
                    throw new SecurityException("Denied getClassLoader");
                }
            }
        });
        // With the above SecurityManager every getClassLoader call throws,
        // the utility should swallow the exceptions and finally return null.
        ClassLoader result = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertNull(result, "When all attempts throw SecurityException the method should return null");
    }
}
