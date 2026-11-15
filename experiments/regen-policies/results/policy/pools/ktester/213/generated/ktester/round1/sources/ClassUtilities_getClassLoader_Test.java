import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Comparator;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_getClassLoader_Test {

    private ClassLoader originalThreadContext;

    private SecurityManager originalSecurityManager;

    class LocalTestClass {
    }

    class LocalTestClass {
    }

    /**
     * Simple SecurityManager that can be instructed to throw a {@link SecurityException}
     * when a call to {@code Class.getClassLoader()} (or {@code Thread.getContextClassLoader()})
     * is performed. The decision is based on the immediate caller class name.
     */
    private static class ConfigurableSecurityManager extends SecurityManager {

        // deny when the caller is java.lang.Class
        private final boolean denyAnchor;

        // deny when the caller is java.lang.Thread
        private final boolean denyThread;

        ConfigurableSecurityManager(boolean denyAnchor, boolean denyThread) {
            this.denyAnchor = denyAnchor;
            this.denyThread = denyThread;
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            checkPermission(perm);
        }

        @Override
        public void checkPermission(Permission perm) {
            if ((perm instanceof RuntimePermission) && "getClassLoader".equals(perm.getName())) {
                // The immediate caller is at stack[2] (0 = this SM, 1 = ClassUtilities, 2 = caller)
                Class<?>[] stack = getClassContext();
                if (stack.length > 2) {
                    String caller = stack[2].getName();
                    if (denyAnchor && "java.lang.Class".equals(caller)) {
                        throw new SecurityException("Denied getClassLoader() for anchor class");
                    }
                    if (denyThread && "java.lang.Thread".equals(caller)) {
                        throw new SecurityException("Denied getContextClassLoader()");
                    }
                }
            }
        }
    }

    class LocalTestClass {
    }

    @BeforeEach
    void captureOriginalState() {
        originalThreadContext = Thread.currentThread().getContextClassLoader();
        originalSecurityManager = System.getSecurityManager();
    }

    @AfterEach
    void restoreOriginalState() {
        Thread.currentThread().setContextClassLoader(originalThreadContext);
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * Provides the primitive, array and wrapper classes used by the
     * {@code primitive/array/wrapper} test group.
     */
    private static Stream<Class<?>> primitiveAndWrapperProvider() {
        return Stream.of(int.class, String[].class, Integer.class);
    }

    /**
     * Utility that creates a minimal byte‑code representation of a public class
     * with the given fully‑qualified name. The generated class has an empty body.
     *
     * <p>We use the {@code java.lang.invoke.MethodHandles.Lookup#defineClass}
     * trick via reflection to avoid a compile‑time dependency on ASM or similar
     * libraries.</p>
     */
    private static byte[] generateSimpleClassBytes(String fqcn) throws Exception {
        // Use the built‑in compiler (javax.tools) to compile a tiny source string.
        // This keeps the test self‑contained and avoids external byte‑code libraries.
        String source = "package " + fqcn.substring(0, fqcn.lastIndexOf('.')) + ";\n" + "public class " + fqcn.substring(fqcn.lastIndexOf('.') + 1) + " {}\n";
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println(source);
        }
        Path sourceFile = Files.createTempFile("Tmp", ".java");
        Files.writeString(sourceFile, source);
        Path outputDir = Files.createTempDirectory("tmpClasses");
        // Compile
        int result = compiler.run(null, null, null, "-d", outputDir.toString(), sourceFile.toString());
        assertEquals(0, result, "Compilation of temporary class failed");
        // Load the resulting .class file into a byte array
        String classPath = fqcn.replace('.', '/') + ".class";
        Path classFile = outputDir.resolve(classPath);
        byte[] bytes = Files.readAllBytes(classFile);
        // Cleanup temporary files
        Files.deleteIfExists(sourceFile);
        Files.walk(outputDir).sorted(Comparator.reverseOrder()).forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (Exception ignored) {
            }
        });
        return bytes;
    }

    /**
     * Verify that the no‑arg overload delegates to the overload that receives a class and
     * returns the class loader that loaded {@code ClassUtilities}.
     */
    @Test
    @DisplayName("getClassLoader() returns the same loader as getClassLoader(ClassUtilities.class)")
    void testGetClassLoader_NoArg_DelegatesCorrectly() {
        ClassLoader expected = ClassUtilities.class.getClassLoader();
        ClassLoader actual = ClassUtilities.getClassLoader();
        assertSame(expected, actual, "ClassUtilities.getClassLoader() should return the loader of ClassUtilities.class");
    }

    /**
     * Verify that the overload returns the class loader of the supplied class.
     */
    @Test
    @DisplayName("getClassLoader(Class) returns the loader of the supplied class")
    void testGetClassLoader_WithAnchorClass() {
        // A class that is part of the application classpath (this test class itself)
        ClassLoader expected = ClassUtilities_getClassLoader_Test.class.getClassLoader();
        ClassLoader actual = ClassUtilities.getClassLoader(ClassUtilities_getClassLoader_Test.class);
        assertSame(expected, actual, "ClassUtilities.getClassLoader(anchor) should return the loader of the anchor class");
    }

    /**
     * Verify that a core JDK class, which is loaded by the bootstrap class loader,
     * results in a {@code null} return value.
     */
    @Test
    @DisplayName("getClassLoader(Class) returns null for bootstrap‑loaded classes")
    void testGetClassLoader_BootstrapClass() {
        ClassLoader loader = ClassUtilities.getClassLoader(String.class);
        assertNull(loader, "ClassUtilities.getClassLoader(String.class) should return null because String is loaded by the bootstrap class loader");
    }

    /**
     * Verify that a custom inner class (loaded by the same class loader as this test)
     * yields a non‑null class loader.
     */
    @Test
    @DisplayName("getClassLoader(Class) returns non‑null for user‑defined classes")
    void testGetClassLoader_UserDefinedClass() {
        class LocalTestClass {
        }
        ClassLoader loader = ClassUtilities.getClassLoader(LocalTestClass.class);
        assertNotNull(loader, "ClassUtilities.getClassLoader(LocalTestClass.class) should not be null for user‑defined classes");
        // It should be the same loader that loaded this test class.
        assertSame(ClassUtilities_getClassLoader_Test.class.getClassLoader(), loader, "The loader for the local test class should match the loader of the test class");
    }

    /**
     * Group: {@code getClassLoader(Class<?>) – edge / error handling}
     *
     * <p>Passing {@code null} must result in a {@link NullPointerException}.</p>
     */
    @Test
    @DisplayName("getClassLoader(null) throws NullPointerException")
    void testGetClassLoader_NullAnchor_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.getClassLoader(((Class<?>) (null))), "Calling getClassLoader(null) should throw NullPointerException");
    }

    /**
     * Group: {@code getClassLoader(Class<?>) – primitive and array types}
     *
     * <p>Primitive, array and wrapper classes have no defining loader (null);
     * the method must fall back to a usable non‑null loader (normally the
     * thread‑context loader).</p>
     */
    @ParameterizedTest(name = "fallback for {0}")
    @MethodSource("primitiveAndWrapperProvider")
    @DisplayName("getClassLoader(Class) falls back for primitive/array/wrapper types")
    void testGetClassLoader_PrimitiveArrayWrapper_Fallback(Class<?> anchor) {
        // Ensure the thread‑context loader is non‑null (the default in most JVMs)
        assertNotNull(Thread.currentThread().getContextClassLoader(), "Thread context ClassLoader must be non‑null for this test");
        ClassLoader loader = ClassUtilities.getClassLoader(anchor);
        assertNotNull(loader, "Fallback ClassLoader should not be null for " + anchor);
        // The fallback should be the thread‑context loader
        assertSame(Thread.currentThread().getContextClassLoader(), loader, "Fallback should be the thread‑context ClassLoader for " + anchor);
    }

    /**
     * Group: {@code getClassLoader(Class<?>) – thread‑context ClassLoader is null}
     *
     * <p>When the thread‑context loader is cleared, the method must fall back
     * to {@link ClassLoader#getSystemClassLoader()}.</p>
     */
    @Test
    @DisplayName("getClassLoader(String) falls back to system loader when thread context is null")
    void testGetClassLoader_ThreadContextNull_FallbackToSystem() {
        // clear the thread‑context loader
        Thread.currentThread().setContextClassLoader(null);
        ClassLoader system = ClassLoader.getSystemClassLoader();
        ClassLoader loader = ClassUtilities.getClassLoader(String.class);
        assertSame(system, loader, "When both anchor loader and thread context are null, the system loader should be returned");
    }

    /**
     * Group: {@code getClassLoader(Class<?>) – security exception with null thread‑context}
     *
     * <p>Simulate a {@link SecurityException} when obtaining the anchor class loader.
     * The method should catch it and fall back to the thread‑context loader.</p>
     */
    @Test
    @DisplayName("SecurityException on anchor class loader – fallback to thread context")
    void testGetClassLoader_SecurityExceptionOnAnchor_FallbackToThreadContext() {
        // Install a SecurityManager that denies getClassLoader() for java.lang.Class (anchor)
        System.setSecurityManager(new ConfigurableSecurityManager(true, false));
        // The thread‑context loader is the normal one (non‑null)
        ClassLoader expected = Thread.currentThread().getContextClassLoader();
        // Use a class that would normally have a null loader (bootstrap class)
        ClassLoader loader = ClassUtilities.getClassLoader(String.class);
        assertSame(expected, loader, "When a SecurityException occurs on the anchor, the method must return the thread‑context loader");
    }

    /**
     * Group: {@code getClassLoader(Class<?>) – security exception on thread‑context ClassLoader}
     *
     * <p>Simulate a {@link SecurityException} when reading the thread‑context loader.
     * The method should then fall back to {@link ClassLoader#getSystemClassLoader()}.</p>
     */
    @Test
    @DisplayName("SecurityException on thread‑context loader – fallback to system loader")
    void testGetClassLoader_SecurityExceptionOnThreadContext_FallbackToSystem() {
        // Install a SecurityManager that denies getClassLoader() for java.lang.Thread (thread‑context)
        System.setSecurityManager(new ConfigurableSecurityManager(false, true));
        // Anchor class is a bootstrap class (loader == null)
        ClassLoader system = ClassLoader.getSystemClassLoader();
        ClassLoader loader = ClassUtilities.getClassLoader(String.class);
        assertSame(system, loader, "When a SecurityException occurs on the thread‑context loader, the method must return the system loader");
    }

    /**
     * Group: {@code getClassLoader(Class<?>) – system ClassLoader fallback when both anchor and thread‑context are null}
     *
     * <p>Both the anchor loader and the thread‑context loader are null; the method
     * must return {@link ClassLoader#getSystemClassLoader()}.</p>
     */
    @Test
    @DisplayName("Both anchor and thread‑context loaders null – fallback to system loader")
    void testGetClassLoader_BothNull_FallbackToSystem() {
        // Ensure thread‑context is null
        Thread.currentThread().setContextClassLoader(null);
        // Anchor is a bootstrap class (String)
        ClassLoader system = ClassLoader.getSystemClassLoader();
        ClassLoader loader = ClassUtilities.getClassLoader(String.class);
        assertSame(system, loader, "When both anchor and thread‑context loaders are null, the system loader should be used");
    }

    /**
     * Group: {@code getClassLoader(Class<?>) – unexpected exception handling}
     *
     * <p>Simulate an unexpected (non‑Security) exception while obtaining the
     * thread‑context loader. The method should not catch it and let it propagate.</p>
     */
    @Test
    @DisplayName("Unexpected exception while accessing thread‑context loader propagates")
    void testGetClassLoader_UnexpectedExceptionPropagation() {
        // Custom SecurityManager that throws IllegalStateException instead of SecurityException
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm, Object context) {
                checkPermission(perm);
            }

            @Override
            public void checkPermission(Permission perm) {
                if ((perm instanceof RuntimePermission) && "getClassLoader".equals(perm.getName())) {
                    // Force an unexpected exception when Thread tries to read its context loader
                    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                    // The call originates from java.lang.Thread.getContextClassLoader()
                    for (StackTraceElement e : stack) {
                        if ("java.lang.Thread".equals(e.getClassName()) && "getContextClassLoader".equals(e.getMethodName())) {
                            throw new IllegalStateException("Simulated unexpected failure");
                        }
                    }
                }
            }
        });
        // The anchor is a bootstrap class; we expect the IllegalStateException to bubble up
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ClassUtilities.getClassLoader(String.class), "Unexpected exceptions should propagate unchanged");
        assertEquals("Simulated unexpected failure", ex.getMessage());
    }

    /**
     * Group: {@code getClassLoader(Class<?>) – custom class loader scenario}
     *
     * <p>Load a dynamically generated class with a dedicated {@link ClassLoader}
     * and verify that the utility returns that exact loader.</p>
     */
    @Test
    @DisplayName("Custom loaded class returns its own ClassLoader")
    void testGetClassLoader_CustomClassLoader() throws Exception {
        // Create a tiny class byte‑code at runtime (using a simple lambda trick)
        // The class name must be unique to avoid clashes.
        String className = "com.cedarsoftware.util.CustomLoadedClass";
        byte[] classBytes = generateSimpleClassBytes(className);
        // Custom loader that defines the class from the supplied byte array
        ClassLoader customLoader = new ClassLoader(this.getClass().getClassLoader()) {

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (name.equals(className)) {
                    return defineClass(name, classBytes, 0, classBytes.length);
                }
                return super.findClass(name);
            }
        };
        // Load the class via the custom loader
        Class<?> customClass = Class.forName(className, true, customLoader);
        assertSame(customLoader, customClass.getClassLoader(), "The dynamically loaded class must be associated with the custom loader");
        // Now invoke the utility method
        ClassLoader loader = ClassUtilities.getClassLoader(customClass);
        assertSame(customLoader, loader, "ClassUtilities.getClassLoader(customClass) should return the custom loader");
    }
}
