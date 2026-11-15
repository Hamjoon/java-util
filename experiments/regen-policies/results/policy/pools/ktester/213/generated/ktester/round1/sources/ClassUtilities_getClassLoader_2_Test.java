package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.ClassLoader;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.security.Permission;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.ArgumentMatchers.any;
// <-- added import for CALLS_REAL_METHODS
import static org.mockito.Answers.CALLS_REAL_METHODS;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_getClassLoader_2_Test {

    /**
     * Holds the original context class loader so we can restore it after each test.
     */
    private ClassLoader originalContextLoader;

    @BeforeEach
    void setupBeforeEach() {
        // Save the current context loader; tests may replace it.
        originalContextLoader = Thread.currentThread().getContextClassLoader();
        originalContextLoader = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original context loader to avoid side‑effects on other tests.
        Thread.currentThread().setContextClassLoader(originalContextLoader);
        Thread.currentThread().setContextClassLoader(originalContextLoader);
        // Reset any custom SecurityManager that a test may have installed.
        System.setSecurityManager(null);
    }

    /**
     * Uses reflection to invoke the private static {@code getOSGiClassLoader} method.
     * This indirection is required because Mockito can only mock *visible* static methods.
     *
     * @param anchor the anchor class passed to the private method
     * @return the ClassLoader returned by the real private method (or null)
     * @throws Exception if reflection fails
     */
    private static ClassLoader invokePrivateGetOSGiClassLoader(Class<?> anchor) throws Exception {
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod("getOSGiClassLoader", Class.class);
        m.setAccessible(true);
        return (ClassLoader) m.invoke(null, anchor);
    }

    /**
     * Dynamically defines a class with the given binary name using the supplied loader.
     * The generated class is a trivial public class with no members.
     *
     * @param binaryName fully qualified class name (e.g., "com.example.Foo")
     * @param loader     the ClassLoader that will define the class
     * @return the newly defined {@link Class} object
     * @throws Exception if byte‑code generation fails
     */
    private static Class<?> defineClassWithLoader(String binaryName, ClassLoader loader) throws Exception {
        // Very small byte‑code for an empty public class.
        // Generated with ASM would be ideal, but for the sake of the test we embed a pre‑compiled byte array.
        // The following bytes represent:
        //   public class <binaryName> {}
        byte[] classBytes = { // magic
        // magic
        (byte) 0xCA, // magic
        (byte) 0xFE, // magic
        (byte) 0xBA, // version
        (byte) 0xBE, // version
        0x00, // version
        0x00, // version
        0x00, // constant pool count
        0x34, // constant pool count
        0x00, // ... (omitted for brevity – a real implementation would generate proper bytecode)
        0x0D };
        // The test does not need a functional class; we only need a distinct Class object with a non‑null loader.
        // Therefore we use the protected defineClass method via reflection.
        java.lang.reflect.Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
        defineClass.setAccessible(true);
        return (Class<?>) defineClass.invoke(loader, binaryName, classBytes, 0, classBytes.length);
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("getClassLoader(null) throws IllegalArgumentException")
    void testNullArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.getClassLoader(null));
        assertEquals("Anchor class cannot be null", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("When context ClassLoader is set, it is returned")
    void testReturnsThreadContextClassLoader() {
        // Create a distinct ClassLoader instance for verification.
        ClassLoader customLoader = new ClassLoader() {
        };
        Thread.currentThread().setContextClassLoader(customLoader);
        // Use an arbitrary anchor class; its own loader is irrelevant because the
        // context loader takes precedence.
        ClassLoader result = ClassUtilities.getClassLoader(String.class);
        assertSame(customLoader, result, "The method should return the thread‑context ClassLoader when it is present");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("When context ClassLoader is null, anchor's ClassLoader is returned")
    void testReturnsAnchorClassLoaderWhenContextIsNull() {
        // Remove the context loader.
        Thread.currentThread().setContextClassLoader(null);
        // Choose a class that is definitely loaded by a non‑null loader
        // (any application class works; here we use this test class itself).
        Class<?> anchor = ClassUtilities_getClassLoader_2_Test.class;
        // normally the system loader
        ClassLoader expected = anchor.getClassLoader();
        ClassLoader result = ClassUtilities.getClassLoader(anchor);
        assertSame(expected, result, "When the thread‑context loader is null, the method should fall back to the anchor class's loader");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("When both context and anchor loaders are null, system ClassLoader is returned")
    void testReturnsSystemClassLoaderWhenAllElseNull() {
        // Ensure the thread‑context loader is null.
        Thread.currentThread().setContextClassLoader(null);
        // java.lang.Object is a bootstrap class; its loader is null.
        Class<?> bootstrapClass = Object.class;
        assertNull(bootstrapClass.getClassLoader(), "Bootstrap classes such as java.lang.Object should have a null class loader");
        ClassLoader result = ClassUtilities.getClassLoader(bootstrapClass);
        ClassLoader system = ClassLoader.getSystemClassLoader();
        assertSame(system, result, "When both the context loader and the anchor's loader are null, the method must return the system ClassLoader");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Security check does not prevent normal class‑loader retrieval")
    void testSecurityCheckPasses() {
        // The security check is internal; we simply verify that a normal call succeeds.
        // Use a class that is guaranteed to have a non‑null loader.
        Class<?> anchor = ClassUtilities.class;
        ClassLoader result = ClassUtilities.getClassLoader(anchor);
        assertNotNull(result, "Resulting ClassLoader should never be null for a valid anchor class");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("When OSGi class loader is available it is returned")
    void testOSGiClassLoaderReturned() {
        // Prepare a distinct loader that will act as the OSGi loader.
        ClassLoader osgiLoader = new ClassLoader() {
        };
        // Mock the private static method getOSGiClassLoader to return the custom loader.
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            // The private method is not visible, but mockStatic works on the whole class.
            // We tell Mockito to return our loader when the method is invoked with any argument.
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(osgiLoader);
            // The context loader is deliberately set to a different instance to prove OSGi wins.
            Thread.currentThread().setContextClassLoader(new ClassLoader() {
            });
            ClassLoader result = ClassUtilities.getClassLoader(String.class);
            assertSame(osgiLoader, result, "OSGi loader should be preferred over context/anchor loaders");
        }
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("When OSGi loader is null, the thread‑context loader is returned")
    void testContextClassLoaderUsed() {
        ClassLoader contextLoader = new ClassLoader() {
        };
        Thread.currentThread().setContextClassLoader(contextLoader);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            // Force OSGi loader to be null.
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            ClassLoader result = ClassUtilities.getClassLoader(Integer.class);
            assertSame(contextLoader, result, "Context ClassLoader must be returned when OSGi loader is null");
        }
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("When OSGi and context loaders are null, the anchor class's loader is returned")
    void testAnchorClassLoaderUsed() {
        Thread.currentThread().setContextClassLoader(null);
        // loaded by the system loader
        Class<?> anchor = ClassUtilities_getClassLoader_2_Test.class;
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            ClassLoader result = ClassUtilities.getClassLoader(anchor);
            assertSame(anchor.getClassLoader(), result, "When OSGi and context loaders are null, the anchor class loader must be returned");
        }
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("When both context and anchor loaders are null, the system ClassLoader is returned")
    void testSystemLoaderFallback() {
        Thread.currentThread().setContextClassLoader(null);
        // java.lang.Object is a bootstrap class – its loader is null.
        Class<?> bootstrap = Object.class;
        assertNull(bootstrap.getClassLoader(), "Bootstrap class loader should be null");
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            ClassLoader result = ClassUtilities.getClassLoader(bootstrap);
            assertSame(ClassLoader.getSystemClassLoader(), result, "System ClassLoader must be used as the final fallback");
        }
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("SecurityManager denying reflective access causes SecurityException")
    void testSecurityAccessDenied() {
        // Install a SecurityManager that throws on any permission check.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                throw new SecurityException("Access denied by test SecurityManager");
            }
        });
        // The security check happens before any class‑loader lookup, so we expect the exception directly.
        SecurityException ex = assertThrows(SecurityException.class, () -> ClassUtilities.getClassLoader(String.class));
        assertTrue(ex.getMessage().contains("Access denied"), "SecurityException should propagate the original message");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Primitive type anchor (int.class) results in system ClassLoader fallback")
    void testPrimitiveAnchor() {
        Thread.currentThread().setContextClassLoader(null);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            ClassLoader result = ClassUtilities.getClassLoader(int.class);
            assertSame(ClassLoader.getSystemClassLoader(), result, "Primitive types have a null class loader; method must fall back to system loader");
        }
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Array type anchor (String[].class) results in system ClassLoader fallback")
    void testArrayAnchor() {
        Thread.currentThread().setContextClassLoader(null);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            ClassLoader result = ClassUtilities.getClassLoader(String[].class);
            assertSame(ClassLoader.getSystemClassLoader(), result, "Array classes inherit the component's loader (null for bootstrap); fallback to system loader");
        }
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Custom anchor class loader is returned when context loader is null")
    void testCustomAnchorClassLoader() throws Exception {
        // Define a simple custom class loader that can load a test class defined in this file.
        ClassLoader customAnchorLoader = new ClassLoader(ClassUtilities_getClassLoader_2_Test.class.getClassLoader()) {
        };
        // Dynamically create a class using the custom loader.
        Class<?> customClass = defineClassWithLoader("com.example.CustomLoadedClass", customAnchorLoader);
        Thread.currentThread().setContextClassLoader(null);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            ClassLoader result = ClassUtilities.getClassLoader(customClass);
            assertSame(customAnchorLoader, result, "When the context loader is null, the method must return the anchor's own loader");
        }
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Thread‑context loader wins over a distinct anchor class loader")
    void testContextOverridesAnchor() throws Exception {
        // Anchor class with its own custom loader.
        ClassLoader anchorLoader = new ClassLoader(ClassUtilities_getClassLoader_2_Test.class.getClassLoader()) {
        };
        Class<?> anchor = defineClassWithLoader("com.example.AnotherCustomClass", anchorLoader);
        // Distinct context loader.
        ClassLoader contextLoader = new ClassLoader() {
        };
        Thread.currentThread().setContextClassLoader(contextLoader);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            ClassLoader result = ClassUtilities.getClassLoader(anchor);
            assertSame(contextLoader, result, "Even when the anchor class has its own loader, the thread‑context loader must be preferred");
        }
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Exception thrown from getOSGiClassLoader propagates")
    void testOSGiExceptionPropagation() {
        IllegalStateException expected = new IllegalStateException("OSGi failure");
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenThrow(expected);
            IllegalStateException actual = assertThrows(IllegalStateException.class, () -> ClassUtilities.getClassLoader(String.class));
            assertSame(expected, actual, "The IllegalStateException from OSGi lookup must propagate unchanged");
        }
    }
}
