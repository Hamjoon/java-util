package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_forName_Test {

    // reference to a mock ClassLoader that can be reused across tests.
    @Mock
    ClassLoader mockClassLoader;

    /**
     * Simple ClassLoader that pretends a specific name maps to a given class.
     * All other names are delegated to the parent loader (or result in null).
     */
    private static class AlternateNameClassLoader extends ClassLoader {

        private final String alternateName;

        private final Class<?> mappedClass;

        AlternateNameClassLoader(String alternateName, Class<?> mappedClass) {
            super(AlternateNameClassLoader.class.getClassLoader());
            this.alternateName = alternateName;
            this.mappedClass = mappedClass;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // The ClassUtilities implementation first tries internalClassForName,
            // which eventually ends up calling ClassLoader.loadClass(name).
            // We intercept that call here.
            if (alternateName.equals(name)) {
                return mappedClass;
            }
            // Defer to the normal mechanism for any other name.
            return super.loadClass(name);
        }
    }

    /**
     * -----------------------------------------------------------------
     *  Helper ClassLoader that throws a RuntimeException on loadClass()
     *  -----------------------------------------------------------------
     */
    private static class RuntimeExceptionClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new RuntimeException("simulated runtime failure");
        }
    }

    /**
     * -----------------------------------------------------------------
     *  Helper ClassLoader that loads a class whose static initializer throws an Error
     *  -----------------------------------------------------------------
     */
    private static class ErrorThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (ErrorClass.class.getName().equals(name)) {
                // Force initialization to trigger the static block.
                return Class.forName(name, true, this);
            }
            return super.loadClass(name);
        }
    }

    /**
     * -----------------------------------------------------------------
     *  Test class whose static initializer throws ExceptionInInitializerError
     *  -----------------------------------------------------------------
     */
    public static class ErrorClass {

        static {
            if (true) {
                throw new ExceptionInInitializerError("static init failure");
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure that any alias added by a previous test does not affect the next one.
        // ClassUtilities does not expose a “clear all” method, so we remove the
        // well‑known test alias if it exists.
        ClassUtilities.removePermanentClassAlias("mapski");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up any alias that might have been added during a test.
        ClassUtilities.removePermanentClassAlias("mapski");
    }

    private static Stream<Arguments> validClassProvider() {
        return Stream.of(Arguments.of("java.lang.String", String.class), // Inner class using '$' notation.
        Arguments.of("java.util.Map$Entry", java.util.Map.Entry.class), // Array descriptor – the expected Class object is Integer[].class.
        Arguments.of("[Ljava.lang.Integer;", Integer[].class));
    }

    @Test
    void testForNameNullOrEmpty() {
        // Null name should return null without invoking the loader.
        assertNull(ClassUtilities.forName(null, ClassLoader.getSystemClassLoader()), "null name should return null");
        // Empty string should also return null.
        assertNull(ClassUtilities.forName("", ClassLoader.getSystemClassLoader()), "empty name should return null");
    }

    @Test
    void testForNameValidSystemClass() {
        Class<?> result = ClassUtilities.forName("java.lang.String", ClassLoader.getSystemClassLoader());
        assertEquals(String.class, result, "Should resolve java.lang.String using the system class loader");
    }

    @Test
    void testForNameClassNotFoundReturnsNull() {
        Class<?> result = ClassUtilities.forName("com.this.class.DoesNotExist", ClassLoader.getSystemClassLoader());
        assertNull(result, "Non‑existent class name should result in null");
    }

    @Test
    void testForNameWithPermanentAlias() {
        // Register an alias and verify that forName resolves it.
        ClassUtilities.addPermanentClassAlias(HashMap.class, "mapski");
        Class<?> aliasResult = ClassUtilities.forName("mapski", ClassUtilities.class.getClassLoader());
        assertEquals(HashMap.class, aliasResult, "Alias 'mapski' should resolve to HashMap.class");
        // Remove the alias and verify that the name is no longer resolvable.
        ClassUtilities.removePermanentClassAlias("mapski");
        Class<?> afterRemoval = ClassUtilities.forName("mapski", ClassUtilities.class.getClassLoader());
        assertNull(afterRemoval, "Alias should be removed and therefore resolve to null");
    }

    @Test
    void testForNameWithCustomClassLoader() {
        // This custom loader returns Long.class when the name matches "my.Alternate".
        ClassLoader customLoader = new AlternateNameClassLoader("my.Alternate", Long.class);
        Class<?> result = ClassUtilities.forName("my.Alternate", customLoader);
        assertEquals(Long.class, result, "Custom class loader should resolve the alternate name to Long.class");
    }

    @Test
    void testForNameSecurityExceptionIsWrapped() {
        // Configure the mock to throw a SecurityException when loadClass is invoked.
        when(mockClassLoader.loadClass(anyString())).thenThrow(new SecurityException("blocked"));
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.forName("java.lang.String", mockClassLoader), "SecurityException should be wrapped in IllegalArgumentException");
        assertTrue(thrown.getMessage().contains("Security exception"), "Exception message should indicate a security problem");
        assertTrue(thrown.getCause() instanceof SecurityException, "Cause of the IllegalArgumentException must be the original SecurityException");
    }

    /**
     * -----------------------------------------------------------------
     *  Valid class loading (normal, inner class, array)
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index} => name=''{0}'', expected=''{1}''")
    @MethodSource("validClassProvider")
    void testForNameValidClasses(String name, Class<?> expected) {
        Class<?> result = ClassUtilities.forName(name, ClassLoader.getSystemClassLoader());
        assertEquals(expected, result, () -> "Class name '" + name + "' should resolve to " + expected);
    }

    /**
     * -----------------------------------------------------------------
     *  Unexpected (non‑Security) exception handling – should return null
     *  -----------------------------------------------------------------
     */
    @Test
    void testForNameUnexpectedRuntimeExceptionReturnsNull() {
        ClassLoader runtimeExceptionLoader = new RuntimeExceptionClassLoader();
        Class<?> result = ClassUtilities.forName("java.lang.String", runtimeExceptionLoader);
        assertNull(result, "RuntimeException from the loader should be caught and result in null");
    }

    /**
     * -----------------------------------------------------------------
     *  Primitive name handling – Class.forName does not accept primitive names
     *  -----------------------------------------------------------------
     */
    @Test
    void testForNamePrimitiveNameReturnsNull() {
        // "int" is not a valid name for Class.forName; expect null.
        assertNull(ClassUtilities.forName("int", ClassLoader.getSystemClassLoader()), "Primitive name 'int' should result in null");
    }

    /**
     * -----------------------------------------------------------------
     *  Wrapper class for primitive – should load normally
     *  -----------------------------------------------------------------
     */
    @Test
    void testForNameWrapperClassLoads() {
        Class<?> result = ClassUtilities.forName("java.lang.Integer", ClassLoader.getSystemClassLoader());
        assertEquals(Integer.class, result, "Wrapper class java.lang.Integer should load normally");
    }

    /**
     * -----------------------------------------------------------------
     *  Error propagation – Errors are not caught by forName
     *  -----------------------------------------------------------------
     */
    @Test
    void testForNameErrorPropagation() {
        ClassLoader errorThrowingLoader = new ErrorThrowingClassLoader();
        // The loader will load a class whose static initializer throws ExceptionInInitializerError.
        ExceptionInInitializerError err = assertThrows(ExceptionInInitializerError.class, () -> ClassUtilities.forName(ErrorClass.class.getName(), errorThrowingLoader), "Error from static initializer should propagate");
        assertNotNull(err.getCause(), "The cause of the ExceptionInInitializerError should be present");
    }
}
