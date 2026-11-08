package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.util.concurrent.Callable;
import static org.mockito.ArgumentMatchers.any;

class ClassUtilities_getClassLoader_2_Test {

    private ClassLoader originalContextLoader;

    private SecurityManager originalSecurityManager;

    @BeforeEach
    void setupBeforeEach() {
        // Preserve the original context class loader so we can restore it after each test
        originalContextLoader = Thread.currentThread().getContextClassLoader();
        // Preserve the original SecurityManager (may be null)
        originalSecurityManager = System.getSecurityManager();
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original context class loader to avoid side‑effects on other tests
        Thread.currentThread().setContextClassLoader(originalContextLoader);
        // Restore original SecurityManager
        System.setSecurityManager(originalSecurityManager);
    }

    private static ClassLoader invokePrivateGetOSGiClassLoader(Class<?> cls) throws Exception {
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod("getOSGiClassLoader", Class.class);
        m.setAccessible(true);
        return (ClassLoader) m.invoke(null, cls);
    }

    private static void invokePrivateCheckSecurityAccess() throws Exception {
        java.lang.reflect.Method m = ClassUtilities.class.getDeclaredMethod("checkSecurityAccess");
        m.setAccessible(true);
        m.invoke(null);
    }

    /**
     * 1. Null anchor class must cause an IllegalArgumentException **
     */
    @Test
    @DisplayName("getClassLoader(null) throws IllegalArgumentException")
    void testNullAnchorThrows() {
        assertThrows(IllegalArgumentException.class, () -> ClassUtilities.getClassLoader(null), "Anchor class cannot be null");
    }

    /**
     * 2. When a thread context ClassLoader is present it should be returned **
     */
    @Test
    @DisplayName("Returns thread context ClassLoader when it is non‑null")
    void testReturnsContextClassLoader() {
        // Arrange – create a dummy class loader and install it as the context loader
        ClassLoader dummyContextLoader = new ClassLoader() {
        };
        Thread.currentThread().setContextClassLoader(dummyContextLoader);
        // Act
        // any non‑null anchor
        ClassLoader result = ClassUtilities.getClassLoader(String.class);
        // Assert
        assertSame(dummyContextLoader, result, "Method should return the thread‑context ClassLoader when it is available");
    }

    /**
     * 3. When the context loader is null, the anchor class's loader should be used **
     */
    @Test
    @DisplayName("Falls back to anchor class loader when context loader is null")
    void testFallsBackToAnchorClassLoader() {
        // Arrange – clear the context loader
        Thread.currentThread().setContextClassLoader(null);
        // Use a class that is loaded by the system class loader (non‑bootstrap)
        Class<?> anchor = java.util.ArrayList.class;
        // normally the system class loader
        ClassLoader expected = anchor.getClassLoader();
        // Act
        ClassLoader result = ClassUtilities.getClassLoader(anchor);
        // Assert
        assertSame(expected, result, "When the context loader is null, the method should return the anchor class's ClassLoader");
    }

    /**
     * 4. When both context and anchor loaders are null (bootstrap class), the system loader is returned **
     */
    @Test
    @DisplayName("Returns SYSTEM_LOADER when both context and anchor loaders are null")
    void testReturnsSystemLoaderForBootstrapClass() {
        // Arrange – clear the context loader
        Thread.currentThread().setContextClassLoader(null);
        // java.lang.Object is loaded by the bootstrap class loader (null)
        Class<?> bootstrapClass = Object.class;
        // Act
        ClassLoader result = ClassUtilities.getClassLoader(bootstrapClass);
        // Assert – the utility falls back to the cached system loader
        assertSame(ClassLoader.getSystemClassLoader(), result, "When both context and anchor loaders are null, the method should return the system ClassLoader");
    }

    /**
     * 5. OSGi class loader present – should be returned directly **
     */
    @Test
    @DisplayName("Returns OSGi ClassLoader when getOSGiClassLoader() returns non‑null")
    void testOSGiClassLoaderPresent() {
        ClassLoader osgiLoader = new ClassLoader() {
        };
        // Ensure context loader is null so we can verify OSGi takes precedence
        Thread.currentThread().setContextClassLoader(null);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            // Mock the private static method getOSGiClassLoader to return our custom loader
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(osgiLoader);
            // checkSecurityAccess should do nothing
            mocked.when(() -> invokePrivateCheckSecurityAccess()).thenAnswer(invocation -> null);
            ClassLoader result = ClassUtilities.getClassLoader(ClassUtilities.class);
            assertSame(osgiLoader, result, "When OSGi loader is available it must be returned, bypassing other fallbacks");
        }
    }

    /**
     * 6. OSGi class loader throws unchecked exception – should propagate **
     */
    @Test
    @DisplayName("Unchecked exception from getOSGiClassLoader propagates")
    void testOSGiClassLoaderThrowsUnchecked() {
        Thread.currentThread().setContextClassLoader(null);
        IllegalStateException cause = new IllegalStateException("OSGi failure");
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenThrow(cause);
            mocked.when(() -> invokePrivateCheckSecurityAccess()).thenAnswer(invocation -> null);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ClassUtilities.getClassLoader(ClassUtilities.class), "Unchecked exception from OSGi loader must not be swallowed");
            assertSame(cause, thrown);
        }
    }

    /**
     * 7. Security access denied – checkSecurityAccess throws SecurityException **
     */
    @Test
    @DisplayName("SecurityException from checkSecurityAccess propagates")
    void testSecurityAccessDenied() {
        SecurityException secEx = new SecurityException("Access denied");
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateCheckSecurityAccess()).thenThrow(secEx);
            // OSGi loader is irrelevant; let it return null
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            SecurityException thrown = assertThrows(SecurityException.class, () -> ClassUtilities.getClassLoader(ClassUtilities.class), "SecurityException from checkSecurityAccess must propagate");
            assertSame(secEx, thrown);
        }
    }

    /**
     * 8. Thread context loader takes precedence over anchor class loader **
     */
    @Test
    @DisplayName("Thread context ClassLoader overrides anchor ClassLoader")
    void testContextLoaderPrecedenceOverAnchor() {
        ClassLoader contextLoader = new ClassLoader() {
        };
        Thread.currentThread().setContextClassLoader(contextLoader);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            mocked.when(() -> invokePrivateCheckSecurityAccess()).thenAnswer(invocation -> null);
            ClassLoader result = ClassUtilities.getClassLoader(ClassUtilities.class);
            assertSame(contextLoader, result, "Even though the anchor class has its own loader, the thread‑context loader must win");
        }
    }

    /**
     * 9. Primitive array class (bootstrap) with no context loader – returns SYSTEM_LOADER **
     */
    @Test
    @DisplayName("Primitive array class falls back to SYSTEM_LOADER")
    void testPrimitiveArrayFallback() {
        Thread.currentThread().setContextClassLoader(null);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            mocked.when(() -> invokePrivateCheckSecurityAccess()).thenAnswer(invocation -> null);
            ClassLoader result = ClassUtilities.getClassLoader(int[].class);
            assertSame(ClassLoader.getSystemClassLoader(), result, "Primitive array classes have null loader; method should return SYSTEM_LOADER");
        }
    }

    /**
     * 10. Primitive type class (bootstrap) with no context loader – returns SYSTEM_LOADER **
     */
    @Test
    @DisplayName("Primitive type class falls back to SYSTEM_LOADER")
    void testPrimitiveTypeFallback() {
        Thread.currentThread().setContextClassLoader(null);
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            mocked.when(() -> invokePrivateCheckSecurityAccess()).thenAnswer(invocation -> null);
            ClassLoader result = ClassUtilities.getClassLoader(int.class);
            assertSame(ClassLoader.getSystemClassLoader(), result, "Primitive types have null loader; method should return SYSTEM_LOADER");
        }
    }

    /**
     * 11. Thread context loader is the system loader – should be returned **
     */
    @Test
    @DisplayName("System ClassLoader as thread context is returned")
    void testSystemLoaderAsContext() {
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            mocked.when(() -> invokePrivateCheckSecurityAccess()).thenAnswer(invocation -> null);
            ClassLoader result = ClassUtilities.getClassLoader(ClassUtilities.class);
            assertSame(ClassLoader.getSystemClassLoader(), result, "When the thread‑context loader is the system loader it must be returned");
        }
    }

    /**
     * 12. Thread context loader access denied – SecurityException propagates **
     */
    @Test
    @DisplayName("SecurityException from Thread.getContextClassLoader propagates")
    void testThreadContextLoaderAccessDenied() {
        // Install a SecurityManager that denies the "getClassLoader" runtime permission
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else
            }

            @Override
            public void checkPropertyAccess(String key) {
                // Allow everything else
            }

            @Override
            public void checkAccess(Thread t) {
                // Allow everything else
            }

            @Override
            public void checkMemberAccess(Class<?> clazz, int which) {
                // Allow everything else
            }

            @Override
            public void checkPackageAccess(String pkg) {
                // Allow everything else
            }

            @Override
            public void checkPackageDefinition(String pkg) {
                // Allow everything else
            }

            @Override
            public void checkCreateClassLoader() {
                // Allow everything else
            }

            @Override
            public void checkSetFactory() {
                // Allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else
            }

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else
            }

            @Override
            public void checkRead(String file) {
                // Allow everything else
            }

            @Override
            public void checkWrite(String file) {
                // Allow everything else
            }

            @Override
            public void checkExec(String cmd) {
                // Allow everything else
            }

            @Override
            public void checkLink(String lib) {
                // Allow everything else
            }

            @Override
            public void checkDelete(String file) {
                // Allow everything else
            }

            @Override
            public void checkConnect(String host, int port) {
                // Allow everything else
            }

            @Override
            public void checkAccept(String host, int port) {
                // Allow everything else
            }

            @Override
            public void checkMulticast(java.net.InetAddress maddr) {
                // Allow everything else
            }

            @Override
            public void checkPropertiesAccess() {
                // Allow everything else
            }

            @Override
            public void checkPrintJobAccess() {
                // Allow everything else
            }

            @Override
            public void checkSystemClipboardAccess() {
                // Allow everything else
            }

            @Override
            public void checkAwtEventQueueAccess() {
                // Allow everything else
            }

            @Override
            public void checkPackageDefinition(String pkg) {
                // Allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                if (perm instanceof RuntimePermission && "getClassLoader".equals(perm.getName())) {
                    throw new SecurityException("Denied getClassLoader");
                }
            }
        });
        // Mock static methods to avoid OSGi interference
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            mocked.when(() -> invokePrivateCheckSecurityAccess()).thenAnswer(invocation -> null);
            SecurityException thrown = assertThrows(SecurityException.class, () -> ClassUtilities.getClassLoader(ClassUtilities.class), "SecurityException from thread‑context loader must propagate");
            assertEquals("Denied getClassLoader", thrown.getMessage());
        }
    }

    /**
     * 13. Anchor class loader access denied – SecurityException propagates **
     */
    @Test
    @DisplayName("SecurityException from anchorClass.getClassLoader propagates")
    void testAnchorClassLoaderAccessDenied() {
        // Context loader returns null, OSGi returns null
        Thread.currentThread().setContextClassLoader(null);
        // Install a SecurityManager that denies class‑loader access for the specific anchor class
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // Allow everything else
            }

            @Override
            public void checkMemberAccess(Class<?> clazz, int which) {
                // Allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                if (perm instanceof RuntimePermission && "getClassLoader".equals(perm.getName())) {
                    // Deny only when the request originates from the anchor class
                    // (simplified – we deny globally for the test)
                    throw new SecurityException("Denied getClassLoader for anchor");
                }
            }
        });
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetOSGiClassLoader(any())).thenReturn(null);
            mocked.when(() -> invokePrivateCheckSecurityAccess()).thenAnswer(invocation -> null);
            SecurityException thrown = assertThrows(SecurityException.class, () -> ClassUtilities.getClassLoader(ClassUtilities.class), "SecurityException from anchorClass.getClassLoader must propagate");
            assertEquals("Denied getClassLoader for anchor", thrown.getMessage());
        }
    }
}
