package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Array;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_getClassLoader_Test {

    private ClassLoader originalThreadContext;

    @Mock
    Class<?> // Mockito will create a mock of Class (requires mockito‑inline)
    securityRestrictedClass;

    /**
     * Custom ClassLoader used for deterministic expectations.
     */
    private static class SimpleClassLoader extends ClassLoader {

        SimpleClassLoader() {
            super(ClassUtilities_getClassLoader_Test.class.getClassLoader());
        }
    }

    /**
     * Dummy static inner class to test inner‑class behaviour.
     */
    static class DummyInner {
    }

    /**
     * Holder for parameterised test arguments.
     */
    private static class ArgumentsHolder {

        final Class<?> anchorClass;

        final java.util.function.Supplier<ClassLoader> expectedSupplier;

        final String description;

        ArgumentsHolder(Class<?> anchorClass, java.util.function.Supplier<ClassLoader> expectedSupplier, String description) {
            this.anchorClass = anchorClass;
            this.expectedSupplier = expectedSupplier;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    static class CustomLoadedClass {
    }

    @BeforeEach
    void captureThreadContext() {
        originalThreadContext = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    void restoreThreadContext() {
        Thread.currentThread().setContextClassLoader(originalThreadContext);
    }

    /**
     * Test cases that only need a concrete anchor class and expect the
     * loader of that class (or a fallback) to be returned.
     */
    static Stream<ArgumentsHolder> provideAnchorClasses() {
        return Stream.of(// normal class loader – ArrayList
        new ArgumentsHolder(ArrayList.class, () -> ArrayList.class.getClassLoader(), "ArrayList should return its own (non‑null) ClassLoader"), // bootstrap class – Object, fallback to thread‑context
        new ArgumentsHolder(Object.class, () -> Thread.currentThread().getContextClassLoader(), "Object (bootstrap) should fall back to thread‑context ClassLoader"), // primitive type – int, fallback to thread‑context
        new ArgumentsHolder(int.class, () -> Thread.currentThread().getContextClassLoader(), "Primitive type should fall back to thread‑context ClassLoader"), // array type – String[], should use component loader (null for bootstrap)
        new ArgumentsHolder(String[].class, () -> String.class.getClassLoader(), "Array type should delegate to component type loader (null for bootstrap)"), // inner static class – ClassUtilities$InnerClass (does not exist, create dummy)
        new ArgumentsHolder(DummyInner.class, () -> ClassUtilities.class.getClassLoader(), "Static inner class should share the outer class loader"));
    }

    /**
     * Helper that defines a trivial class byte‑code at runtime and loads it with the supplied loader.
     * For the purpose of the test we use an existing inner class to avoid byte‑code generation.
     */
    private static Class<?> loadClassWithLoader(ClassLoader loader, String className) throws ClassNotFoundException {
        // The class already exists (inner class below), just load it via the given loader.
        // In a real scenario we would generate a class; here we simply return the class object.
        return Class.forName(className, true, loader);
    }

    /**
     * Verify that the no‑argument overload returns the same ClassLoader that
     * would be obtained directly from the {@code ClassUtilities} class.
     */
    @Test
    void testGetClassLoader_NoArgs() {
        ClassLoader expected = ClassUtilities.class.getClassLoader();
        ClassLoader actual = ClassUtilities.getClassLoader();
        assertNotNull(actual, "ClassLoader should never be null for a user‑defined class");
        assertSame(expected, actual, "getClassLoader() must return the ClassLoader of ClassUtilities");
    }

    /**
     * Verify that the overload which accepts an anchor class returns the
     * ClassLoader of that anchor class.
     */
    @Test
    void testGetClassLoader_WithAnchor_UserClass() {
        // Use a class that definitely has a non‑null ClassLoader (any class from this
        // project will be loaded by the application ClassLoader).
        ClassLoader expected = ClassUtilities.class.getClassLoader();
        ClassLoader actual = ClassUtilities.getClassLoader(ClassUtilities.class);
        assertSame(expected, actual, "Anchor class ClassUtilities should yield its own ClassLoader");
    }

    /**
     * Verify that passing a JDK core class (e.g., {@code String}) – which is
     * loaded by the bootstrap ClassLoader – results in a {@code null}
     * ClassLoader, matching the behaviour of {@code Class#getClassLoader()}.
     */
    @Test
    void testGetClassLoader_WithAnchor_JdkClass() {
        // bootstrap => null
        ClassLoader expected = String.class.getClassLoader();
        ClassLoader actual = ClassUtilities.getClassLoader(String.class);
        assertSame(expected, actual, "Bootstrap‑loaded classes should result in a null ClassLoader");
    }

    /**
     * Verify that a {@code null} anchor class is handled gracefully.
     * The expected contract (based on the source implementation) is to fall
     * back to the ClassLoader of {@code ClassUtilities} itself.
     */
    @Test
    void testGetClassLoader_WithAnchor_Null() {
        ClassLoader expected = ClassUtilities.class.getClassLoader();
        ClassLoader actual = ClassUtilities.getClassLoader(null);
        assertSame(expected, actual, "When anchorClass is null, the method should return the ClassLoader of ClassUtilities");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideAnchorClasses")
    void testGetClassLoader_WithVariousAnchors(ArgumentsHolder holder) {
        // Arrange: set a known thread‑context loader for fallback scenarios
        ClassLoader threadCtx = new SimpleClassLoader();
        Thread.currentThread().setContextClassLoader(threadCtx);
        // Act
        ClassLoader actual = ClassUtilities.getClassLoader(holder.anchorClass);
        // Expected loader may be null (bootstrap) – the lambda resolves the fallback logic.
        ClassLoader expected = holder.expectedSupplier.get();
        // Assert
        assertSame(expected, actual, "Unexpected ClassLoader for anchor class " + holder.anchorClass.getName());
    }

    @Test
    void testGetClassLoader_WithCustomClassLoader() throws Exception {
        // Load a dummy class via a custom URLClassLoader (simulated with a simple subclass)
        ClassLoader customLoader = new SimpleClassLoader();
        Class<?> customLoaded = loadClassWithLoader(customLoader, "com.cedarsoftware.util.ClassUtilities_getClassLoader_Test$CustomLoadedClass");
        // Act
        ClassLoader actual = ClassUtilities.getClassLoader(customLoaded);
        // Assert
        assertSame(customLoader, actual, "Class loaded by a custom loader should return that loader");
    }

    @Test
    void testGetClassLoader_SecurityException_FallbackToThreadContext() {
        // Arrange: mock getClassLoader() to throw SecurityException
        when(securityRestrictedClass.getClassLoader()).thenThrow(new SecurityException("restricted"));
        // Provide a known thread‑context loader
        ClassLoader threadCtx = new SimpleClassLoader();
        Thread.currentThread().setContextClassLoader(threadCtx);
        // Act
        ClassLoader actual = ClassUtilities.getClassLoader(securityRestrictedClass);
        // Assert: should fall back to thread‑context loader
        assertSame(threadCtx, actual, "When anchorClass.getClassLoader() throws, method should fall back to thread‑context loader");
    }

    @Test
    void testGetClassLoader_SecurityException_ThreadContextNull_FallbackToClassUtilities() {
        // Arrange: mock getClassLoader() to throw SecurityException
        when(securityRestrictedClass.getClassLoader()).thenThrow(new SecurityException("restricted"));
        // Thread‑context loader is null
        Thread.currentThread().setContextClassLoader(null);
        // Act
        ClassLoader actual = ClassUtilities.getClassLoader(securityRestrictedClass);
        // Assert: final fallback is ClassUtilities' own loader
        assertSame(ClassUtilities.class.getClassLoader(), actual, "When both anchorClass and thread‑context loader are unavailable, fallback to ClassUtilities loader");
    }

    @Test
    void testGetClassLoader_ThreadContextSecurityException_FallbackToClassUtilities() {
        // Arrange: anchor class is bootstrap (Object) → null loader
        Class<?> anchor = Object.class;
        // Thread‑context loader that throws SecurityException when any method is called.
        ClassLoader throwingLoader = new ClassLoader() {

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new SecurityException("thread‑context not allowed");
            }
        };
        Thread.currentThread().setContextClassLoader(throwingLoader);
        // Act
        ClassLoader actual = ClassUtilities.getClassLoader(anchor);
        // Assert: fallback to ClassUtilities loader
        assertSame(ClassUtilities.class.getClassLoader(), actual, "If thread‑context loader throws SecurityException, fallback to ClassUtilities loader");
    }

    @Test
    void testGetClassLoader_NullAnchor_NullThreadContext_FallbackToClassUtilities() {
        // Both anchor and thread‑context are null
        Thread.currentThread().setContextClassLoader(null);
        ClassLoader actual = ClassUtilities.getClassLoader(null);
        assertSame(ClassUtilities.class.getClassLoader(), actual, "When both anchorClass and thread‑context are null, should return ClassUtilities loader");
    }

    @Test
    void testGetClassLoader_BootstrapClass_NullThreadContext_FallbackToClassUtilities() {
        // Object is bootstrap (null loader)
        Thread.currentThread().setContextClassLoader(null);
        ClassLoader actual = ClassUtilities.getClassLoader(Object.class);
        assertSame(ClassUtilities.class.getClassLoader(), actual, "Bootstrap class with null thread‑context should fall back to ClassUtilities loader");
    }
}
