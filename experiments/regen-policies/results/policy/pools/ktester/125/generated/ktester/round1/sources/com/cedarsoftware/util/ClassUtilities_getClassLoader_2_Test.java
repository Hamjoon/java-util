package com.cedarsoftware.util;

import java.net.URLClassLoader;
import java.net.URL;
// import org.junit.*;
// import static org.junit.Assert.*;

// @ExtendWith(PowerMockExtension.class)
class ClassUtilities_getClassLoader_2_Test {

    /**
     * Holds the original context class loader so we can restore it after each test.
     */
    private ClassLoader originalContextLoader;

    /* --------------------------------------------------------------
     *  Helper dummy class used as an anchor in many tests.
     * -------------------------------------------------------------- */
    private static class DummyClass {
        // empty – only used for its Class object.
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Save the current thread's context class loader.
        originalContextLoader = Thread.currentThread().getContextClassLoader();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Restore the original context class loader to avoid side‑effects on other tests.
        Thread.currentThread().setContextClassLoader(originalContextLoader);
    }

//     @Test
//     @DisplayName("getClassLoader(null) throws IllegalArgumentException")
//     void testNullAnchorClass() {
//         try {
//             ClassUtilities.getClassLoader(null);
//             fail("Expected IllegalArgumentException");
//         } catch (IllegalArgumentException ex) {
//             assertEquals("Anchor class cannot be null", ex.getMessage());
//         }
//     }

//     @Test
//     @DisplayName("When a context ClassLoader is present it is returned")
//     void testContextClassLoaderTakesPrecedence() {
//         // Arrange – create a dummy class loader and install it as the thread's context loader.
//         ClassLoader dummyLoader = new ClassLoader() {
//         };
//         Thread.currentThread().setContextClassLoader(dummyLoader);
//         // Act – ask for a loader using any anchor class (here we use ClassUtilities itself).
//         ClassLoader result = ClassUtilities.getClassLoader(ClassUtilities.class);
//         // Assert – the dummy loader should be returned.
//         assertSame("Context ClassLoader should be preferred over other options", dummyLoader, result);
//     }

//     @Test
//     @DisplayName("When context ClassLoader is null, anchor class's loader is returned")
//     void testFallbackToAnchorClassLoader() {
//         // Arrange – remove any context loader.
//         Thread.currentThread().setContextClassLoader(null);
//         // The anchor class we use is this test class; it definitely has a non‑null loader.
//         ClassLoader anchorLoader = ClassUtilities_getClassLoader_2_Test.class.getClassLoader();
//         assertNotNull("Test class should have a non‑null class loader", anchorLoader);
//         // Act
//         ClassLoader result = ClassUtilities.getClassLoader(ClassUtilities_getClassLoader_2_Test.class);
//         // Assert – we should get the anchor class's loader.
//         assertSame("When no context loader is set, the anchor class's loader must be returned", anchorLoader, result);
//     }

//     @Test
//     @DisplayName("When both context and anchor loaders are null, system loader is returned")
//     void testFallbackToSystemClassLoader() {
//         // Arrange – clear the context loader.
//         Thread.currentThread().setContextClassLoader(null);
//         // Use a bootstrap class (java.lang.Object) whose loader is null.
//         Class<?> bootstrapClass = Object.class;
//         assertNull("Bootstrap classes such as java.lang.Object have a null class loader", bootstrapClass.getClassLoader());
//         // Act
//         ClassLoader result = ClassUtilities.getClassLoader(bootstrapClass);
//         // Assert – the system class loader should be returned.
//         ClassLoader system = ClassLoader.getSystemClassLoader();
//         assertSame("When both context and anchor loaders are null, the system class loader must be used", system, result);
//     }

    /* --------------------------------------------------------------
     *  5. Bootstrap class – fall back to static SYSTEM_LOADER
     * -------------------------------------------------------------- */
//     @Test
//     @DisplayName("Bootstrap class falls back to SYSTEM_LOADER")
//     void testBootstrapClassFallsBackToSystemLoader() throws Exception {
//         // Arrange
//         Thread.currentThread().setContextClassLoader(null);
//         // java.lang.Object is a bootstrap class (loader == null)
//         Class<?> bootstrap = Object.class;
//         // Act
//         ClassLoader result = ClassUtilities.getClassLoader(bootstrap);
//         // Assert – the system class loader is the expected fallback.
//         assertSame("When all look‑ups are null the system ClassLoader must be returned", ClassLoader.getSystemClassLoader(), result);
//     }

    /* --------------------------------------------------------------
     *  7. Primitive anchor class – falls back to SYSTEM_LOADER
     * -------------------------------------------------------------- */
//     @Test
//     @DisplayName("Primitive anchor class falls back to SYSTEM_LOADER")
//     void testPrimitiveAnchorClass() throws Exception {
//         // Arrange
//         Thread.currentThread().setContextClassLoader(null);
//         // Act
//         ClassLoader result = ClassUtilities.getClassLoader(int.class);
//         // Assert
//         assertSame("Primitive types have no class loader – system ClassLoader must be returned", ClassLoader.getSystemClassLoader(), result);
//     }

    /* --------------------------------------------------------------
     *  8. Array of primitive – falls back to SYSTEM_LOADER
     * -------------------------------------------------------------- */
//     @Test
//     @DisplayName("Array of primitive class falls back to SYSTEM_LOADER")
//     void testArrayOfPrimitiveClass() throws Exception {
//         // Arrange
//         Thread.currentThread().setContextClassLoader(null);
//         // Act
//         ClassLoader result = ClassUtilities.getClassLoader(int[].class);
//         // Assert
//         assertSame("Array of primitive types has no class loader – system ClassLoader must be returned", ClassLoader.getSystemClassLoader(), result);
//     }
}