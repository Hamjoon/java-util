package com.cedarsoftware.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.util.stream.Stream;
// import org.junit.*;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import org.junit.runners.Parameterized.Parameters;
import org.mockito.junit.MockitoJUnitRunner;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class ClassUtilities_getClassLoader_Test {

    private ClassLoader originalContextLoader;

    private SecurityManager originalSecurityManager;

    /**
     * Simple data holder for the parameterised tests.
     */
    private static class LoaderTestCase {

        final String description;

        final Class<?> anchorClass;

        final ClassLoader threadContextLoader;

        final ClassLoader expectedLoader;

        final Class<?> expectedException;

        LoaderTestCase(String description, Class<?> anchorClass, ClassLoader threadContextLoader, ClassLoader expectedLoader, Class<?> expectedException) {
            this.description = description;
            this.anchorClass = anchorClass;
            this.threadContextLoader = threadContextLoader;
            this.expectedLoader = expectedLoader;
            this.expectedException = expectedException;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Simple custom class loader that loads a single inner class.
     */
    static class CustomClassLoader extends URLClassLoader {

        CustomClassLoader() {
            super(new URL[0], null);
        }
    }

    /**
     * Class loaded by {@link CustomClassLoader}. The static block forces the JVM to
     * define the class using our custom loader.
     */
    public static class CustomLoadedClass {

        static {
            try {
                Class.forName(CustomLoadedClass.class.getName(), true, new CustomClassLoader());
            } catch (ClassNotFoundException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

//     @RunWith(Parameterized.class)
    public static class NormalFallbackTests {

        private final LoaderTestCase tc;

        public NormalFallbackTests(LoaderTestCase tc) {
            this.tc = tc;
        }

//         @Parameters(name = "{0}")
//         public static Collection<LoaderTestCase> data() {
//             URLClassLoader mockContext = createMockClassLoader();
//             Class<?> customLoaded = CustomLoadedClass.class;
//             ClassLoader customLoader = customLoaded.getClassLoader();
//             List<LoaderTestCase> list = new ArrayList<>();
//             // anchor = ClassUtilities (non‑null loader) – should ignore thread context
//             list.add(new LoaderTestCase("anchor non‑null loader overrides thread context", ClassUtilities.class, mockContext, ClassUtilities.class.getClassLoader(), null));
//             // bootstrap anchor, thread context present
//             list.add(new LoaderTestCase("bootstrap anchor, thread context loader present", Object.class, mockContext, mockContext, null));
//             // bootstrap anchor, thread context null → system loader fallback
//             list.add(new LoaderTestCase("bootstrap anchor, thread context null → system loader", Object.class, null, ClassLoader.getSystemClassLoader(), null));
//             // custom class loader anchor overrides thread context
//             list.add(new LoaderTestCase("custom class loader anchor overrides thread context", CustomLoadedClass.class, mockContext, customLoader, null));
//             return list;
//         }

//         @Test
//         public void testGetClassLoaderWithVariousAnchors() {
//             Thread.currentThread().setContextClassLoader(tc.threadContextLoader);
//             ClassLoader result = ClassUtilities.getClassLoader(tc.anchorClass);
//             Assert.assertSame(tc.expectedLoader, result, "Unexpected loader for case: " + tc.description);
//         }
    }

//     @RunWith(Parameterized.class)
    public static class SecurityManagerTests {

        private final LoaderTestCase tc;

        public SecurityManagerTests(LoaderTestCase tc) {
            this.tc = tc;
        }

//         @Parameters(name = "{0}")
//         public static Collection<LoaderTestCase> data() {
//             List<LoaderTestCase> list = new ArrayList<>();
//             // security manager blocks anchor class loader retrieval
//             list.add(new LoaderTestCase("security manager blocks anchor class loader", ClassUtilities.class, null, null, SecurityException.class));
//             // security manager blocks thread context loader
//             list.add(new LoaderTestCase("security manager blocks thread context loader", Object.class, createMockClassLoader(), null, SecurityException.class));
//             // security manager blocks system class loader fallback
//             list.add(new LoaderTestCase("security manager blocks system class loader fallback", Object.class, null, null, SecurityException.class));
//             return list;
//         }

//         @Test
//         public void testGetClassLoaderWithSecurityManager() {
//             // Install a SecurityManager that blocks getClassLoader()
//             System.setSecurityManager(new SecurityManager() {
// 
//                 @Override
//                 public void checkPermission(Permission perm) {
//                     if ("getClassLoader".equals(perm.getName())) {
//                         throw new SecurityException("blocked getClassLoader()");
//                     }
//                 }
//             });
//             Thread.currentThread().setContextClassLoader(tc.threadContextLoader);
//             try {
//                 ClassUtilities.getClassLoader(tc.anchorClass);
//                 Assert.fail("Expected exception for case: " + tc.description);
//             } catch (Throwable ex) {
//                 Assert.assertTrue(tc.expectedException.isInstance(ex), "Expected " + tc.expectedException.getName() + " but got " + ex.getClass().getName() + " for case: " + tc.description);
//             }
//         }
    }

//     @BeforeEach
    void captureGlobalState() {
        originalContextLoader = Thread.currentThread().getContextClassLoader();
        originalSecurityManager = System.getSecurityManager();
    }

//     @AfterEach
    void restoreGlobalState() {
        Thread.currentThread().setContextClassLoader(originalContextLoader);
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * Helper to create a simple URLClassLoader that loads from the test classpath.
     */
    private static URLClassLoader createMockClassLoader() {
        URL[] urls = ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs();
        return new URLClassLoader(urls, null);
    }

    /**
     * Test data for normal fallback logic (no security manager involved).
     */
    private static Stream<LoaderTestCase> normalFallbackProvider() {
        URLClassLoader mockContext = createMockClassLoader();
        Class<?> customLoaded = CustomLoadedClass.class;
        ClassLoader customLoader = customLoaded.getClassLoader();
        return // anchor = ClassUtilities (non‑null loader) – should ignore thread context
        Stream.// anchor = ClassUtilities (non‑null loader) – should ignore thread context
        of(// bootstrap anchor, thread context present
        new LoaderTestCase("anchor non‑null loader overrides thread context", ClassUtilities.class, mockContext, ClassUtilities.class.getClassLoader(), null), // bootstrap anchor, thread context null → system loader fallback
        new LoaderTestCase("bootstrap anchor, thread context loader present", Object.class, mockContext, mockContext, null), // custom class loader anchor overrides thread context
        new LoaderTestCase("bootstrap anchor, thread context null → system loader", Object.class, null, ClassLoader.getSystemClassLoader(), null), new LoaderTestCase("custom class loader anchor overrides thread context", CustomLoadedClass.class, mockContext, customLoader, null));
    }

    /**
     * Test data for security‑manager‑blocking scenarios.
     */
    private static Stream<LoaderTestCase> securityManagerProvider() {
        return // security manager blocks anchor class loader retrieval
        Stream.// security manager blocks anchor class loader retrieval
        of(// security manager blocks thread context loader
        new LoaderTestCase("security manager blocks anchor class loader", ClassUtilities.class, null, null, SecurityException.class), // security manager blocks system class loader fallback
        new LoaderTestCase("security manager blocks thread context loader", Object.class, createMockClassLoader(), null, SecurityException.class), new LoaderTestCase("security manager blocks system class loader fallback", Object.class, null, null, SecurityException.class));
    }

    /**
     * Verify that the default getClassLoader() returns the same ClassLoader that would be
     * obtained by calling ClassUtilities.getClassLoader(ClassUtilities.class).
     */
//     @Test
//     @DisplayName("getClassLoader() returns the same loader as the anchor overload")
//     void testDefaultClassLoaderMatchesAnchor() {
//         ClassLoader defaultLoader = ClassUtilities.getClassLoader();
//         ClassLoader anchorLoader = ClassUtilities.getClassLoader(ClassUtilities.class);
//         Assert.assertNotNull(defaultLoader, "Default class loader should not be null");
//         Assert.assertNotNull(anchorLoader, "Anchor class loader should not be null");
//         Assert.assertSame(defaultLoader, anchorLoader, "Default getClassLoader() should delegate to the anchor overload");
//     }

    /**
     * Verify that getClassLoader(Class<?>) returns the class loader of the supplied class,
     * even when the class is loaded by the bootstrap loader (e.g., java.lang.String).
     */
//     @Test
//     @DisplayName("getClassLoader(Class<?>) returns correct loader for various classes")
//     void testAnchorClassLoaderVarious() {
//         // Class loaded by the system/application class loader
//         ClassLoader utilLoader = ClassUtilities.getClassLoader(ClassUtilities.class);
//         Assert.assertSame(ClassUtilities.class.getClassLoader(), utilLoader, "ClassUtilities class loader should be returned");
//         // Class loaded by the bootstrap class loader (returns null)
//         ClassLoader stringLoader = ClassUtilities.getClassLoader(String.class);
//         Assert.assertNull(stringLoader, "Bootstrap classes (e.g., java.lang.String) have a null class loader");
//     }

    /**
     * Ensure that passing a null anchor class results in a {@link NullPointerException}
     * (the implementation delegates to Class.getClassLoader() which throws NPE for null).
     */
//     @Test
//     @DisplayName("getClassLoader(null) throws NullPointerException")
//     void testAnchorClassLoaderNullArgument() {
//         ClassUtilities.getClassLoader(null);
//     }

//     @ParameterizedTest(name = "{0}")
//     @MethodSource("normalFallbackProvider")
//     void testGetClassLoaderWithVariousAnchors(LoaderTestCase tc) {
//         // Set thread context loader if the case specifies it
//         Thread.currentThread().setContextClassLoader(tc.threadContextLoader);
//         // Invoke and verify
//         ClassLoader result = ClassUtilities.getClassLoader(tc.anchorClass);
//         assertSame(tc.expectedLoader, result, () -> "Unexpected loader for case: " + tc.description);
//     }

//     @ParameterizedTest(name = "{0}")
//     @MethodSource("securityManagerProvider")
//     void testGetClassLoaderWithSecurityManager(LoaderTestCase tc) {
//         // Install a SecurityManager that blocks getClassLoader()
//         System.setSecurityManager(new SecurityManager() {
// 
//             @Override
//             public void checkPermission(Permission perm) {
//                 // Block only the RuntimePermission used for getting a class loader
//                 if ("getClassLoader".equals(perm.getName())) {
//                     throw new SecurityException("blocked getClassLoader()");
//                 }
//                 // Allow everything else
//             }
//         });
//         // Set thread context loader if required for the case
//         Thread.currentThread().setContextClassLoader(tc.threadContextLoader);
//         // Verify that the expected exception is thrown
//         @SuppressWarnings("unchecked")
//         Class<? extends Throwable> expected = (Class<? extends Throwable>) tc.expectedException;
//         assertThrows(expected, () -> ClassUtilities.getClassLoader(tc.anchorClass), "Expected SecurityException for case: " + tc.description);
//     }
}