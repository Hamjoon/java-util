package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.security.Permission;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class ClassUtilities_getClassLoader_2_Test {

    /**
     * Holds the original thread‑context ClassLoader so we can restore it after each test.
     */
    private ClassLoader originalContextClassLoader;

    /**
     * Holds the original SecurityManager so we can restore it after each test.
     */
    private SecurityManager originalSecurityManager;

    /**
     * Helper holder for the parameterised OSGi scenarios.
     */
    private static final class OSGiScenario {

        final ClassLoader threadContextLoader;

        // always null for these tests
        final ClassLoader osgiLoader;

        final Class<?> anchorClass;

        final String assertMessage;

        OSGiScenario(ClassLoader osgiLoader, ClassLoader threadContextLoader, Class<?> anchorClass, String assertMessage) {
            this.osgiLoader = osgiLoader;
            this.threadContextLoader = threadContextLoader;
            this.anchorClass = anchorClass;
            this.assertMessage = assertMessage;
        }

        @Override
        public String toString() {
            return "anchor=" + (anchorClass == null ? "null" : anchorClass.getName()) + ", threadContext=" + (threadContextLoader == null ? "null" : threadContextLoader.getClass().getSimpleName());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Save the current context ClassLoader before each test.
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        originalSecurityManager = System.getSecurityManager();
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original context ClassLoader to avoid side‑effects on other tests.
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        System.setSecurityManager(originalSecurityManager);
    }

    /* --------------------------------------------------------------
     *  8. Parameterised test for the OSGi‑detection‑branch scenarios where
     *     getOSGiClassLoader returns null (the normal path).  This covers
     *     the cases where the thread‑context loader is present, absent,
     *     or the anchor class has its own loader.
     * -------------------------------------------------------------- */
    private static Stream<OSGiScenario> osgiScenarios() {
        return Stream.of(// OSGi returns null, thread context is non‑null → should return thread context.
        new OSGiScenario(null, new ClassLoader(ClassUtilities_getClassLoader_2_Test.class.getClassLoader()) {
        }, null, "Thread‑context loader should be returned when OSGi is absent."), // OSGi null, thread context null, anchor has its own loader.
        new OSGiScenario(null, null, ClassUtilities.class, "Anchor class loader should be returned when both OSGi and thread‑context are null."), // OSGi null, thread context null, anchor loader null → system loader.
        new OSGiScenario(null, null, String.class, "System loader should be returned when all previous loaders are null."));
    }

    /**
     * Verify that when a thread‑context ClassLoader is present,
     * {@code ClassUtilities.getClassLoader(Class)} returns it, regardless of the
     * anchor class's own loader.
     */
    @Test
    void testReturnsThreadContextClassLoaderWhenAvailable() {
        // Arrange – create a distinct ClassLoader instance.
        ClassLoader customLoader = new ClassLoader(ClassUtilities_getClassLoader_2_Test.class.getClassLoader()) {
        };
        Thread.currentThread().setContextClassLoader(customLoader);
        // Act – invoke the method with any class (anchor class does not matter here).
        // String has null loader.
        ClassLoader result = ClassUtilities.getClassLoader(String.class);
        // Assert – the returned loader must be the thread‑context loader we set.
        assertSame(customLoader, result, "Method should return the thread‑context ClassLoader when it is non‑null.");
    }

    /**
     * Verify that when the thread‑context ClassLoader is {@code null},
     * the method falls back to the ClassLoader that loaded the anchor class.
     */
    @Test
    void testFallsBackToAnchorClassLoaderWhenThreadContextIsNull() {
        // Arrange – clear the thread‑context ClassLoader.
        Thread.currentThread().setContextClassLoader(null);
        // Use a user‑defined class whose loader is the application ClassLoader (non‑null).
        Class<?> anchor = ClassUtilities.class;
        ClassLoader expected = anchor.getClassLoader();
        // Act
        ClassLoader result = ClassUtilities.getClassLoader(anchor);
        // Assert
        assertNotNull(result, "Anchor class loader should not be null for user‑defined classes.");
        assertSame(expected, result, "Method should return the anchor class's ClassLoader when thread‑context is null.");
    }

    /**
     * Verify that when both the thread‑context ClassLoader and the anchor class's
     * ClassLoader are {@code null} (bootstrap classes), the method finally returns
     * the system ClassLoader.
     */
    @Test
    void testFallsBackToSystemClassLoaderWhenBothAreNull() {
        // Arrange – clear the thread‑context ClassLoader.
        Thread.currentThread().setContextClassLoader(null);
        // Use a bootstrap class (e.g., java.lang.String) whose getClassLoader() returns null.
        Class<?> bootstrapClass = String.class;
        assertNull(bootstrapClass.getClassLoader(), "Bootstrap class should have a null ClassLoader.");
        // Act
        ClassLoader result = ClassUtilities.getClassLoader(bootstrapClass);
        // Assert – should be the system ClassLoader.
        assertSame(ClassLoader.getSystemClassLoader(), result, "When both context and anchor loaders are null, the system ClassLoader must be returned.");
    }

    /**
     * Basic sanity check that the no‑argument overload delegates correctly to the
     * overload that accepts an anchor class.
     */
    @Test
    void testNoArgOverloadDelegatesToClassBasedMethod() {
        // Arrange – set a known context loader.
        ClassLoader customLoader = new ClassLoader(ClassUtilities_getClassLoader_2_Test.class.getClassLoader()) {
        };
        Thread.currentThread().setContextClassLoader(customLoader);
        // Act – call the convenience method.
        ClassLoader result = ClassUtilities.getClassLoader();
        // Assert – should be the same as the context loader.
        assertSame(customLoader, result, "ClassUtilities.getClassLoader() should delegate to getClassLoader(Class) and return the context loader.");
    }

    /* --------------------------------------------------------------
     *  1. null anchorClass → NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void testNullAnchorClassThrowsNPE() {
        // Arrange – make sure the thread‑context loader is non‑null so the
        // method reaches the anchorClass.getClassLoader() call.
        Thread.currentThread().setContextClassLoader(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ClassUtilities.getClassLoader(null), "Passing a null anchorClass should result in NPE.");
    }

    /* --------------------------------------------------------------
     *  2. primitive anchor (int.class) → system ClassLoader
     * -------------------------------------------------------------- */
    @Test
    void testPrimitiveAnchorFallsBackToSystemClassLoader() {
        // Arrange – clear any thread‑context loader.
        Thread.currentThread().setContextClassLoader(null);
        // Act
        ClassLoader result = ClassUtilities.getClassLoader(int.class);
        // Assert – the only non‑null loader left is the system loader.
        assertSame(ClassLoader.getSystemClassLoader(), result, "Primitive types have no ClassLoader; the method must return the system loader.");
    }

    /* --------------------------------------------------------------
     *  3. array class – should use component’s ClassLoader
     * -------------------------------------------------------------- */
    @Test
    void testArrayClassUsesComponentClassLoader() {
        // Arrange – ensure thread‑context is null so we fall back to the
        // anchorClass loader (which for an array is the component loader).
        Thread.currentThread().setContextClassLoader(null);
        // component = java.lang.String
        Class<?> arrayClass = String[].class;
        // bootstrap → null
        ClassLoader componentLoader = String.class.getClassLoader();
        // Act
        ClassLoader result = ClassUtilities.getClassLoader(arrayClass);
        // Assert – because the component loader is null, the method finally
        // returns the system loader (the same behaviour as a bootstrap class).
        assertSame(ClassLoader.getSystemClassLoader(), result, "Array class should delegate to its component loader; when that is null the system loader is returned.");
    }

    /* --------------------------------------------------------------
     *  4. getOSGiClassLoader throws RuntimeException → exception propagates
     * -------------------------------------------------------------- */
    @Test
    void testGetOSGiClassLoaderThrowsUncheckedException() throws Exception {
        // Use Mockito to mock the private static method.  The mock is scoped
        // to this test only.
        try (MockedStatic<ClassUtilities> mocked = Mockito.mockStatic(ClassUtilities.class, Mockito.CALLS_REAL_METHODS)) {
            // Use reflection to obtain the private method handle.
            java.lang.reflect.Method privateMethod = ClassUtilities.class.getDeclaredMethod("getOSGiClassLoader", Class.class);
            privateMethod.setAccessible(true);
            // Stub the private method to throw an IllegalStateException.
            mocked.when(() -> privateMethod.invoke(any(), any())).thenThrow(new IllegalStateException("OSGi detection failed"));
            // Act & Assert – the unchecked exception must bubble up unchanged.
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ClassUtilities.getClassLoader(Integer.class), "Unchecked exception from getOSGiClassLoader should propagate.");
            assertEquals("OSGi detection failed", ex.getMessage());
        }
    }

    /* --------------------------------------------------------------
     *  5. SecurityException when accessing Thread.getContextClassLoader()
     * -------------------------------------------------------------- */
    @Test
    void testThreadContextClassLoaderSecurityException() {
        // Install a SecurityManager that denies the RuntimePermission "getClassLoader".
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except the specific permission.
                if ("getClassLoader".equals(perm.getName())) {
                    throw new SecurityException("Access denied to thread context ClassLoader");
                }
            }
        });
        // Act & Assert – the SecurityException must be propagated.
        SecurityException se = assertThrows(SecurityException.class, () -> ClassUtilities.getClassLoader(String.class), "SecurityException from thread‑context loader must propagate.");
        assertTrue(se.getMessage().contains("Access denied"));
    }

    /* --------------------------------------------------------------
     *  6. SecurityException when invoking anchorClass.getClassLoader()
     * -------------------------------------------------------------- */
    @Test
    void testAnchorClassLoaderSecurityException() {
        // Thread context is null so the method will reach anchorClass.getClassLoader().
        Thread.currentThread().setContextClassLoader(null);
        // SecurityManager that blocks the RuntimePermission "getClassLoader".
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                if ("getClassLoader".equals(perm.getName())) {
                    throw new SecurityException("Access denied to anchor class loader");
                }
            }
        });
        // Act & Assert – the SecurityException must be propagated.
        SecurityException se = assertThrows(SecurityException.class, () -> ClassUtilities.getClassLoader(String.class), "SecurityException from anchorClass.getClassLoader() must propagate.");
        assertTrue(se.getMessage().contains("Access denied"));
    }

    /* --------------------------------------------------------------
     *  7. SecurityException when calling ClassLoader.getSystemClassLoader()
     * -------------------------------------------------------------- */
    @Test
    void testSystemClassLoaderSecurityException() {
        // Ensure both previous look‑ups return null.
        Thread.currentThread().setContextClassLoader(null);
        // Use a bootstrap class (String) whose loader is null.
        Class<?> bootstrap = String.class;
        assertNull(bootstrap.getClassLoader());
        // SecurityManager that blocks the RuntimePermission "getClassLoader".
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                if ("getClassLoader".equals(perm.getName())) {
                    throw new SecurityException("Access denied to system ClassLoader");
                }
            }
        });
        // Act & Assert – the SecurityException must be propagated.
        SecurityException se = assertThrows(SecurityException.class, () -> ClassUtilities.getClassLoader(bootstrap), "SecurityException from ClassLoader.getSystemClassLoader() must propagate.");
        assertTrue(se.getMessage().contains("Access denied"));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("osgiScenarios")
    void testOsgiDetectionBranchWhenOSGiReturnsNull(OSGiScenario scenario) {
        // Arrange – set the thread‑context loader as requested.
        Thread.currentThread().setContextClassLoader(scenario.threadContextLoader);
        // Act
        ClassLoader result = ClassUtilities.getClassLoader(scenario.anchorClass);
        // Assert – compare with the expected loader.
        ClassLoader expected;
        if (scenario.threadContextLoader != null) {
            expected = scenario.threadContextLoader;
        } else if (scenario.anchorClass != null && scenario.anchorClass.getClassLoader() != null) {
            expected = scenario.anchorClass.getClassLoader();
        } else {
            expected = ClassLoader.getSystemClassLoader();
        }
        assertSame(expected, result, scenario.assertMessage);
    }
}
