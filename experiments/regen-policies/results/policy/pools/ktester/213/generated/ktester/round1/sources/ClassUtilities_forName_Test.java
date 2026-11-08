package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.AccessControlException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_forName_Test {

    /**
     * The {@link ClassLoader} used for the normal (successful) look‑ups.
     */
    private static ClassLoader defaultClassLoader;

    /**
     * A ClassLoader that throws a specific exception type from {@code loadClass}.
     */
    private static class ThrowingClassLoader extends ClassLoader {

        private final Class<? extends Throwable> toThrow;

        ThrowingClassLoader(Class<? extends Throwable> toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (SecurityException.class.isAssignableFrom(toThrow)) {
                throw new SecurityException("Simulated security failure for " + name);
            }
            if (RuntimeException.class.isAssignableFrom(toThrow)) {
                throw new RuntimeException("Simulated runtime failure for " + name);
            }
            if (ClassNotFoundException.class.isAssignableFrom(toThrow)) {
                throw new ClassNotFoundException("Simulated CNF for " + name);
            }
            // Fallback – delegate to parent (should not happen in our tests)
            return super.loadClass(name);
        }
    }

    /**
     * A ClassLoader that never delegates to its parent – it always throws
     * {@link ClassNotFoundException}. This mimics an isolated environment that
     * cannot see any system classes.
     */
    private static class IsolatingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException("Isolating loader cannot find " + name);
        }
    }

    // -----------------------------------------------------------------
    private static class ArgumentsHolder {

        final String name;

        final ClassLoader classLoader;

        final Class<?> expectedClass;

        final String description;

        // For exception‑expectation tests we pass the expected exception type in expectedClass
        ArgumentsHolder(String name, ClassLoader classLoader, Class<?> expectedClass, String description) {
            this.name = name;
            this.classLoader = classLoader;
            this.expectedClass = expectedClass;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use the current thread's context class loader – it is guaranteed to be non‑null
        // and can load standard JDK classes such as java.lang.String.
        defaultClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset any SecurityManager that may have been installed by a test.
        System.setSecurityManager(null);
    }

    /**
     * Successful class loading – includes null and explicit ClassLoaders.
     */
    private static Stream<ArgumentsHolder> successfulLoadingProvider() {
        return Stream.of(new ArgumentsHolder("java.lang.String", null, String.class, "Valid name with null ClassLoader should resolve to String.class"), new ArgumentsHolder("java.util.ArrayList", ClassUtilities.class.getClassLoader(), java.util.ArrayList.class, "Valid name with explicit ClassLoader should resolve to ArrayList.class"));
    }

    /**
     * Empty / whitespace / null name handling.
     */
    private static Stream<ArgumentsHolder> emptyNameProvider() {
        return Stream.of(new ArgumentsHolder(null, null, null, "Null name should return null"), new ArgumentsHolder("", null, null, "Empty string should return null"), new ArgumentsHolder("   ", null, null, "Whitespace‑only string should return null"), // same cases but with a non‑null loader to ensure early exit
        new ArgumentsHolder(null, new IsolatingClassLoader(), null, "Null name with non‑null loader should still return null"), new ArgumentsHolder("", new IsolatingClassLoader(), null, "Empty name with non‑null loader should still return null"), new ArgumentsHolder("   ", new IsolatingClassLoader(), null, "Whitespace‑only name with non‑null loader should still return null"));
    }

    /**
     * Class‑not‑found scenarios (including malformed descriptors).
     */
    private static Stream<ArgumentsHolder> classNotFoundProvider() {
        return Stream.of(new ArgumentsHolder("com.nonexistent.FooBar", null, null, "Non‑existent class should result in null"), new ArgumentsHolder("java.lang.String$NonExistingInner", null, null, "Invalid inner class should result in null"), new ArgumentsHolder("[Lnon.existent.Class;", null, null, "Malformed array descriptor should result in null"), new ArgumentsHolder("int", null, null, "Primitive name should result in null"), new ArgumentsHolder(" java.lang.String ", null, null, "Name with surrounding whitespace should result in null"));
    }

    /**
     * ClassLoaders that throw non‑Security exceptions – method should return null.
     */
    private static Stream<ArgumentsHolder> nonSecurityExceptionProvider() {
        return Stream.of(new ArgumentsHolder("java.util.HashMap", new ThrowingClassLoader(RuntimeException.class), null, "RuntimeException from loader should be swallowed and return null"), new ArgumentsHolder("java.lang.Integer", new ThrowingClassLoader(ClassNotFoundException.class), null, "ClassNotFoundException from loader should result in null"));
    }

    /**
     * SecurityException handling – should be wrapped in IllegalArgumentException.
     */
    private static Stream<ArgumentsHolder> securityExceptionProvider() {
        return Stream.of(new ArgumentsHolder("java.lang.String", new ThrowingClassLoader(SecurityException.class), IllegalArgumentException.class, "SecurityException from loader should be wrapped"));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("forName returns null when name is null")
    void testForName_NullName_ReturnsNull() {
        assertNull(ClassUtilities.forName(null, defaultClassLoader), "Expected null when class name is null");
    }

    @Test
    @DisplayName("forName returns null when name is empty")
    void testForName_EmptyName_ReturnsNull() {
        assertNull(ClassUtilities.forName("", defaultClassLoader), "Expected null when class name is empty");
        assertNull(ClassUtilities.forName("   ", defaultClassLoader), "Expected null when class name contains only whitespace");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("forName resolves a valid fully‑qualified class name")
    void testForName_ValidClass_ReturnsClass() {
        Class<?> result = ClassUtilities.forName("java.lang.String", defaultClassLoader);
        assertNotNull(result, "Result should not be null for a valid class name");
        assertEquals(String.class, result, "Expected java.lang.String class");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("forName returns null when the class cannot be found")
    void testForName_ClassNotFound_ReturnsNull() {
        Class<?> result = ClassUtilities.forName("com.example.DoesNotExist", defaultClassLoader);
        assertNull(result, "Expected null when the class cannot be located");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("forName wraps SecurityException in IllegalArgumentException")
    void testForName_SecurityException_WrappedInIllegalArgumentException() {
        ClassLoader securityThrowingLoader = new ClassLoader() {

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                // Simulate a security manager denying the operation.
                throw new SecurityException("Access denied for class: " + name);
            }
        };
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.forName("java.lang.String", securityThrowingLoader), "Expected IllegalArgumentException when a SecurityException occurs");
        assertTrue(thrown.getMessage().contains("Security exception"), "Exception message should indicate a security problem");
        assertTrue(thrown.getCause() instanceof SecurityException, "Cause of the IllegalArgumentException should be a SecurityException");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("successfulLoadingProvider")
    @DisplayName("Successful class loading")
    void testSuccessfulLoading(ArgumentsHolder holder) {
        ClassLoader loader = holder.classLoader != null ? holder.classLoader : defaultClassLoader;
        Class<?> result = ClassUtilities.forName(holder.name, loader);
        assertNotNull(result, "Result should not be null for a valid class name");
        assertEquals(holder.expectedClass, result, "Resolved class does not match expected");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("emptyNameProvider")
    @DisplayName("Empty / null / whitespace name handling")
    void testEmptyNames(ArgumentsHolder holder) {
        Class<?> result = ClassUtilities.forName(holder.name, holder.classLoader);
        assertNull(result, holder.description);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("classNotFoundProvider")
    @DisplayName("Class not found handling")
    void testClassNotFound(ArgumentsHolder holder) {
        Class<?> result = ClassUtilities.forName(holder.name, defaultClassLoader);
        assertNull(result, holder.description);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("nonSecurityExceptionProvider")
    @DisplayName("Non‑Security exception from ClassLoader")
    void testNonSecurityException(ArgumentsHolder holder) {
        Class<?> result = ClassUtilities.forName(holder.name, holder.classLoader);
        assertNull(result, holder.description);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("securityExceptionProvider")
    @DisplayName("SecurityException handling")
    void testSecurityException(ArgumentsHolder holder) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.forName(holder.name, holder.classLoader), holder.description);
        assertTrue(ex.getMessage().contains("Security exception"), "Exception message should contain 'Security exception'");
        assertTrue(ex.getCause() instanceof SecurityException, "Cause should be a SecurityException");
    }

    @Test
    @DisplayName("Array descriptor resolves to correct array class")
    void testArrayDescriptor() {
        Class<?> result = ClassUtilities.forName("[Ljava.lang.String;", defaultClassLoader);
        assertNotNull(result);
        assertEquals(String[].class, result, "Array descriptor should resolve to String[].class");
    }

    @Test
    @DisplayName("Isolating ClassLoader cannot find java.lang.Integer")
    void testIsolatingClassLoader() {
        ClassLoader isolating = new IsolatingClassLoader();
        Class<?> result = ClassUtilities.forName("java.lang.Integer", isolating);
        assertNull(result, "Isolating loader should not be able to resolve Integer.class");
    }

    @Test
    @DisplayName("Null ClassLoader with active SecurityManager throws IllegalArgumentException")
    void testSecurityManagerActive() {
        // Install a SecurityManager that denies loading of java.lang.System
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else
            }

            @Override
            public void checkMemberAccess(Class<?> clazz, int which) {
                if ("java.lang.System".equals(clazz.getName())) {
                    throw new SecurityException("Access denied for " + clazz.getName());
                }
            }
        });
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.forName("java.lang.System", null), "SecurityManager should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains("Security exception"), "Exception message should indicate a security problem");
        assertTrue(ex.getCause() instanceof SecurityException, "Cause should be a SecurityException");
    }
}
