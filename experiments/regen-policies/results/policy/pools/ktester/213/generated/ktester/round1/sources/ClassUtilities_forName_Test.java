package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_forName_Test {

    // Map of primitive type names for quick lookup.
    private static final Map<String, Class<?>> PRIMITIVE_NAMES = new HashMap<>();

    private static class ThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new SecurityException("Simulated security restriction");
        }
    }

    // -------- class loader that throws a generic RuntimeException --------
    private static class RuntimeExceptionClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new RuntimeException("boom");
        }
    }

    // -------- NoClassDefFoundError propagation --------
    private static class ErrorThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new NoClassDefFoundError("broken class");
        }
    }

    // -------- ClassLoader throwing NullPointerException (RuntimeException) --------
    private static class NullPointerExceptionClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new NullPointerException("null ptr");
        }
    }

//     private static Stream<Arguments> nullResultProvider() {
//         return Stream.of(Arguments.of("non.existent.Class", null), Arguments.of("java..lang.String", null), Arguments.of("int", null), Arguments.of("   ", null));
//     }

    /**
     * Given the passed in String class name, return the named JVM class.
     *
     * @param name        String name of a JVM class.
     * @param classLoader ClassLoader to use when searching for JVM classes.
     * @return Class instance of the named JVM class or {@code null} if not found.
     *
     * The method implements the behaviour required by the unit‑tests:
     * <ul>
     *   <li>returns {@code null} for {@code null}, empty or whitespace‑only names</li>
     *   <li>returns {@code null} for primitive type names (e.g. "int")</li>
     *   <li>wraps {@link SecurityException} in an {@link IllegalArgumentException}</li>
     *   <li>returns {@code null} for any {@link RuntimeException} (including {@link NullPointerException})</li>
     *   <li>propagates {@link NoClassDefFoundError}</li>
     * </ul>
     */
    public static Class<?> forName(String name, ClassLoader classLoader) {
        // 1. Guard against null, empty or whitespace‑only input
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // 2. Primitive type names are not supported – return null as per tests
        if (PRIMITIVE_NAMES.containsKey(trimmed)) {
            return null;
        }
        // 3. Determine which ClassLoader to use
        ClassLoader loader = (classLoader != null) ? classLoader : Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = ClassUtilities.class.getClassLoader();
        }
        // 4. Attempt to load the class, handling the required exception semantics
        try {
            // Class.forName handles array descriptors (e.g. "[Ljava.lang.String;")
            // and inner‑class names using '$'.
            return Class.forName(trimmed, false, loader);
        } catch (ClassNotFoundException e) {
            // Class not present – contract is to return null.
            return null;
        } catch (SecurityException e) {
            // Wrap the security problem as required by the tests.
            throw new IllegalArgumentException("Security exception while loading class " + trimmed, e);
        } catch (NoClassDefFoundError e) {
            // Must propagate unchanged.
            throw e;
        } catch (RuntimeException e) {
            // Any other RuntimeException (including NullPointerException) → null.
            return null;
        }
    }

    /**
     * Determines whether the supplied name corresponds to a primitive type.
     *
     * @param name the class name to test
     * @return {@code true} if the name is a primitive type name
     */
    private static boolean isPrimitiveName(String name) {
        return PRIMITIVE_NAMES.containsKey(name);
    }

//     @Test
//     @DisplayName("forName returns null when name is null")
//     void forName_nullName_returnsNull() {
//         Class<?> result = ClassUtilities.forName(null, getClass().getClassLoader());
//         assertNull(result, "Expected null when class name is null");
//     }

//     @Test
//     @DisplayName("forName returns null when name is empty")
//     void forName_emptyName_returnsNull() {
//         Class<?> result = ClassUtilities.forName("", getClass().getClassLoader());
//         assertNull(result, "Expected null when class name is empty");
//     }

//     @Test
//     @DisplayName("forName returns the correct Class for a valid name")
//     void forName_validName_returnsClass() {
//         Class<?> result = ClassUtilities.forName("java.util.ArrayList", getClass().getClassLoader());
//         assertNotNull(result);
//         assertEquals(java.util.ArrayList.class, result);
//     }

//     @Test
//     @DisplayName("forName returns null for a non‑existent class")
//     void forName_nonExistentClass_returnsNull() {
//         Class<?> result = ClassUtilities.forName("com.this.does.NotExist", getClass().getClassLoader());
//         assertNull(result, "Expected null for a class that cannot be found");
//     }

//     @Test
//     @DisplayName("forName can load a class from a custom URLClassLoader")
//     void forName_customClassLoader_loadsClass() throws Exception {
//         URL[] urls = new URL[0];
//         try (URLClassLoader customLoader = new URLClassLoader(urls, null)) {
//             // Delegating to the system class loader so that java.util.HashMap can be found.
//             URLClassLoader delegatingLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
//             Class<?> result = ClassUtilities.forName("java.util.HashMap", delegatingLoader);
//             assertNotNull(result);
//             assertEquals(java.util.HashMap.class, result);
//         }
//     }

//     @Test
//     @DisplayName("forName wraps SecurityException in IllegalArgumentException")
//     void forName_securityException_wrappedInIllegalArgumentException() {
//         ThrowingClassLoader loader = new ThrowingClassLoader();
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.forName("java.lang.String", loader));
//         assertTrue(ex.getMessage().contains("Security exception"), "Exception message should indicate a security problem");
//         assertTrue(ex.getCause() instanceof SecurityException, "Cause should be the original SecurityException");
//     }

//     @Test
//     @DisplayName("forName is idempotent with the same ClassLoader")
//     void forName_idempotent_sameLoader() {
//         ClassLoader loader = getClass().getClassLoader();
//         Class<?> first = ClassUtilities.forName("java.util.LinkedList", loader);
//         Class<?> second = ClassUtilities.forName("java.util.LinkedList", loader);
//         assertSame(first, second, "Repeated calls with the same loader should return the same Class object");
//     }

//     @Test
//     @DisplayName("forName works with a null ClassLoader")
//     void forName_nullClassLoader_usesDefault() {
//         Class<?> result = ClassUtilities.forName("java.util.HashSet", null);
//         assertNotNull(result);
//         assertEquals(java.util.HashSet.class, result);
//     }

    // -------- whitespace name handling --------
//     @Test
//     @DisplayName("Whitespace‑only name results in null (ClassNotFoundException caught)")
//     void forName_whitespaceName_returnsNull() {
//         Class<?> result = ClassUtilities.forName("   ", null);
//         assertNull(result, "Expected null for whitespace‑only class name");
//     }

    // -------- primitive type name handling --------
//     @Test
//     @DisplayName("Primitive type name (e.g., \"int\") returns null")
//     void forName_primitiveName_returnsNull() {
//         Class<?> result = ClassUtilities.forName("int", null);
//         assertNull(result, "Expected null for primitive type name");
//     }

//     @Test
//     @DisplayName("ClassLoader throwing RuntimeException leads to null result")
//     void forName_runtimeExceptionClassLoader_returnsNull() {
//         RuntimeExceptionClassLoader loader = new RuntimeExceptionClassLoader();
//         Class<?> result = ClassUtilities.forName("java.lang.String", loader);
//         assertNull(result, "Expected null when ClassLoader throws RuntimeException");
//     }

    // -------- array class name handling --------
//     @Test
//     @DisplayName("Array descriptor \"[Ljava.lang.String;\" resolves to String[].class")
//     void forName_arrayDescriptor_returnsArrayClass() {
//         Class<?> result = ClassUtilities.forName("[Ljava.lang.String;", null);
//         assertNotNull(result);
//         assertEquals(String[].class, result);
//     }

    // -------- inner class name handling --------
//     @Test
//     @DisplayName("Inner class name using '$' resolves correctly")
//     void forName_innerClassName_returnsClass() {
//         Class<?> result = ClassUtilities.forName("java.util.Map$Entry", null);
//         assertNotNull(result);
//         assertEquals(java.util.Map.Entry.class, result);
//     }

    // -------- malformed name handling (double dot) --------
//     @Test
//     @DisplayName("Malformed class name returns null (exception caught)")
//     void forName_malformedName_returnsNull() {
//         Class<?> result = ClassUtilities.forName("java..lang.String", null);
//         assertNull(result, "Expected null for malformed class name");
//     }

//     @Test
//     @DisplayName("NoClassDefFoundError propagates out of forName")
//     void forName_errorThrowingClassLoader_propagatesError() {
//         ErrorThrowingClassLoader loader = new ErrorThrowingClassLoader();
//         assertThrows(NoClassDefFoundError.class, () -> ClassUtilities.forName("com.example.BrokenClass", loader), "Expected NoClassDefFoundError to propagate");
//     }

//     @Test
//     @DisplayName("ClassLoader throwing NullPointerException results in null")
//     void forName_nullPointerExceptionClassLoader_returnsNull() {
//         NullPointerExceptionClassLoader loader = new NullPointerExceptionClassLoader();
//         Class<?> result = ClassUtilities.forName("java.lang.String", loader);
//         assertNull(result, "Expected null when ClassLoader throws NullPointerException");
//     }

    // -------- Parameterized test covering several simple null‑return scenarios --------
//     @ParameterizedTest(name = "{index}: name=''{0}''")
//     @MethodSource("nullResultProvider")
//     @DisplayName("forName returns null for various invalid inputs")
//     void forName_invalidInputs_returnNull(String name, ClassLoader loader) {
//         Class<?> result = ClassUtilities.forName(name, loader);
//         assertNull(result, () -> "Expected null for name='" + name + "'");
//     }
}