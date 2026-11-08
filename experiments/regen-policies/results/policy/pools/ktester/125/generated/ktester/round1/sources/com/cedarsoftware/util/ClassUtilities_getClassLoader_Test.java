package com.cedarsoftware.util;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class ClassUtilities_getClassLoader_Test {

    /* ----------------------------------------------------------------------
     *  Preserve original thread‑context ClassLoader so we can restore it after
     *  each test that mutates it.
     * ---------------------------------------------------------------------- */
    private ClassLoader originalContextClassLoader;

//     @BeforeEach
    void captureOriginalContextClassLoader() {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    }

//     @AfterEach
    void restoreOriginalContextClassLoader() {
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
    }

    /* ----------------------------------------------------------------------
     *  Helper: create a temporary directory.
     * ---------------------------------------------------------------------- */
    private static File createTempDirectory(String prefix) throws IOException {
        File temp = File.createTempFile(prefix, Long.toString(System.nanoTime()));
        if (!temp.delete()) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!temp.mkdir()) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        temp.deleteOnExit();
        return temp;
    }

    /* ----------------------------------------------------------------------
     *  Helper: compile a trivial Java class into the supplied directory using
     *  the built‑in JavaCompiler API.
     * ---------------------------------------------------------------------- */
    private static void compileSimpleClass(File outputDir, String fullyQualifiedName) throws IOException {
        String source = "package " + fullyQualifiedName.substring(0, fullyQualifiedName.lastIndexOf('.')) + ";\n" + "public class " + fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1) + " {}\n";
        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No Java compiler available – tests must run on a JDK, not a JRE");
        }
        javax.tools.StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        fileManager.setLocation(javax.tools.StandardLocation.CLASS_OUTPUT, java.util.Collections.singletonList(outputDir));
        javax.tools.JavaFileObject compilationUnit = new javax.tools.SimpleJavaFileObject(java.net.URI.create("string:///" + fullyQualifiedName.replace('.', '/') + ".java"), javax.tools.JavaFileObject.Kind.SOURCE) {

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        javax.tools.JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null, java.util.Collections.singletonList(compilationUnit));
        Boolean success = task.call();
        fileManager.close();
        if (!Boolean.TRUE.equals(success)) {
            throw new IllegalStateException("Compilation of test class failed");
        }
    }

//     @Test
//     @DisplayName("getClassLoader() returns a non‑null ClassLoader and matches the overload version")
//     void testGetClassLoader_NoArg() {
//         // Act
//         ClassLoader noArgLoader = ClassUtilities.getClassLoader();
//         ClassLoader overloadLoader = ClassUtilities.getClassLoader(ClassUtilities.class);
//         // Assert
//         assertNotNull("The no‑arg getClassLoader() must not return null", noArgLoader);
//         assertSame("The no‑arg overload should delegate to getClassLoader(Class<?>) with ClassUtilities.class", overloadLoader, noArgLoader);
//     }

//     @Test
//     @DisplayName("getClassLoader(Class<?>) returns the correct loader for a user‑defined class")
//     void testGetClassLoader_WithAnchorClass() {
//         // Act
//         ClassLoader loader = ClassUtilities.getClassLoader(ClassUtilities.class);
//         // Assert
//         assertNotNull("ClassUtilities class should have a non‑null ClassLoader", loader);
//         assertSame("Returned ClassLoader must match the one obtained directly from the class", ClassUtilities.class.getClassLoader(), loader);
//     }

//     @Test
//     @DisplayName("Repeated calls return the same ClassLoader instance (caching behavior)")
//     void testGetClassLoader_CacheConsistency() {
//         // Act
//         ClassLoader first = ClassUtilities.getClassLoader();
//         ClassLoader second = ClassUtilities.getClassLoader();
//         ClassLoader third = ClassUtilities.getClassLoader(ClassUtilities.class);
//         // Assert
//         assertSame("Subsequent no‑arg calls should return the same cached ClassLoader", first, second);
//         assertSame("The overload should return the same cached ClassLoader as the no‑arg version", first, third);
//     }

//     @Test
//     @DisplayName("getClassLoader(Class<?>) handles bootstrap classes (null loader) gracefully")
//     void testGetClassLoader_BootstrapClass() {
//         // java.lang.String is loaded by the bootstrap class loader, which may be null
//         ClassLoader loader = ClassUtilities.getClassLoader(String.class);
//         // The utility must never throw; it may return null for bootstrap classes
//         assertTrue("Bootstrap class loader may be null but must not cause an exception", loader == null || loader instanceof ClassLoader);
//     }

    /* ----------------------------------------------------------------------
     *  1. Null anchor class – the method should treat it as
     *     ClassUtilities.class.
     * ---------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Null anchor class defaults to ClassUtilities.class loader")
//     void testGetClassLoader_NullAnchor() {
//         // Act
//         ClassLoader loader = ClassUtilities.getClassLoader((Class<?>) null);
//         // Assert – should be the same loader that loaded ClassUtilities
//         assertSame("When anchorClass is null the utility must fall back to ClassUtilities.class", ClassUtilities.class.getClassLoader(), loader);
//     }

    /* ----------------------------------------------------------------------
     *  4. Anchor class loaded by bootstrap (java.lang.Object) and thread
     *     context loader is null – should fall back to system ClassLoader.
     * ---------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Bootstrap anchor + null thread context → system ClassLoader")
//     void testGetClassLoader_BootstrapAnchor_NullThreadContext() {
//         // Arrange
//         Thread.currentThread().setContextClassLoader(null);
//         // Act
//         ClassLoader loader = ClassUtilities.getClassLoader(Object.class);
//         // Assert
//         assertSame("When both anchor and thread context loaders are null the system loader must be returned", ClassLoader.getSystemClassLoader(), loader);
//     }

    /* ----------------------------------------------------------------------
     *  5. Thread context ClassLoader is null (bootstrap anchor) – fallback to
     *     system ClassLoader.
     * ---------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Bootstrap anchor with null thread context falls back to system loader")
//     void testGetClassLoader_BootstrapAnchor_NullThreadContext_Explicit() {
//         // Arrange
//         Thread.currentThread().setContextClassLoader(null);
//         // Act
//         ClassLoader loader = ClassUtilities.getClassLoader(Object.class);
//         // Assert
//         assertSame(ClassLoader.getSystemClassLoader(), loader);
//     }

    /* ----------------------------------------------------------------------
     *  8. Custom class loaded by a dedicated URLClassLoader – the method must
     *     return that exact loader, regardless of the thread context loader.
     * ---------------------------------------------------------------------- */
//     @Test
//     @DisplayName("Custom loaded class returns its own ClassLoader")
//     void testGetClassLoader_CustomLoadedClass() throws IOException, ClassNotFoundException {
//         // Create a temporary directory to hold compiled class files
//         File tempDir = createTempDirectory("customloader");
//         // Compile a simple class into that directory
//         compileSimpleClass(tempDir, "custom.CustomLoaded");
//         // Load the class with a dedicated URLClassLoader
//         URLClassLoader customLoader = new URLClassLoader(new URL[] { tempDir.toURI().toURL() }, null);
//         Class<?> customClass = Class.forName("custom.CustomLoaded", true, customLoader);
//         // Ensure thread context loader is different
//         Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
//         // Act
//         ClassLoader result = ClassUtilities.getClassLoader(customClass);
//         // Assert
//         assertSame("The loader that actually loaded the custom class must be returned", customLoader, result);
//     }

    /* ----------------------------------------------------------------------
     *  9. Verify that the no‑arg overload still delegates correctly after the
     *     above manipulations (sanity check).
     * ---------------------------------------------------------------------- */
//     @Test
//     @DisplayName("No‑arg overload still delegates to ClassUtilities.class loader")
//     void testNoArgOverloadAfterManipulations() {
//         // Act
//         ClassLoader noArg = ClassUtilities.getClassLoader();
//         ClassLoader overload = ClassUtilities.getClassLoader(ClassUtilities.class);
//         // Assert
//         assertNotNull(noArg);
//         assertSame(overload, noArg);
//     }
}