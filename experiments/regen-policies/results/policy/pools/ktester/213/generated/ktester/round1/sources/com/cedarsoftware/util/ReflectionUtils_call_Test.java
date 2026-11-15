package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.Optional;

// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_call_Test {

    /* -------------------------------------------------------------
     *  Test fixtures
     * ------------------------------------------------------------- */
    static class Simple {

        String echo(String s) {
            return "echo:" + s;
        }
    }

    static class PrivateMethod {

        private String secret() {
            return "top-secret";
        }
    }

    static class ThrowsChecked {

        void explode() throws Exception {
            throw new Exception("boom");
        }
    }

    /* -------------------------------------------------------------
     *  Helper class used by many test cases
     * ------------------------------------------------------------- */
    static class TestClass {

        // Simple public method used for normal invocation and argument‑mismatch checks
        public String greet(String name) {
            return "Hello " + name;
        }

        // Private method – invoking it without setting accessible should cause IllegalAccessException
        private String secret() {
            return "top-secret";
        }

        // Public method that throws a checked exception (declared)
//         public void throwsChecked(String msg) throws IOException {
//             throw new IOException(msg);
//         }

        // Public method that throws a runtime exception
        public void throwsRuntime(String msg) {
            throw new IllegalArgumentException(msg);
        }

        // Public method that throws an Error
        public void throwsError() {
            throw new AssertionError("boom");
        }
    }

    /**
     * Simple holder for parameterized test arguments.
     */
    private static class ArgumentsHolder {

        final Object instance;

        final Method method;

        final Object[] args;

        final Class<? extends Throwable> expectedException;

        // may be null
        final String expectedMessageFragment;

        ArgumentsHolder(Object instance, Method method, Object[] args, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
            this.instance = instance;
            this.method = method;
            this.args = args;
            this.expectedException = expectedException;
            this.expectedMessageFragment = expectedMessageFragment;
        }

        @Override
        public String toString() {
            return "method=" + (method != null ? method.getName() : "null") + ", args=" + java.util.Arrays.toString(args);
        }
    }

    /**
     * Provides test data for null‑check scenarios.
     */
//     static Stream<ArgumentsHolder> nullCheckProvider() {
//         TestClass instance = new TestClass();
//         return // null Method
//         Stream.// null Method
//         of(// null instance
//         new ArgumentsHolder(instance, null, new Object[] {}, IllegalArgumentException.class, "null Method passed to ReflectionUtils.call() on instance of type: TestClass"), new ArgumentsHolder(null, getMethod(TestClass.class, "greet", String.class), new Object[] { "World" }, IllegalArgumentException.class, "Cannot call [greet]() on a null object."));
//     }

    /**
     * Provides test data for exception‑handling scenarios.
     */
//     static Stream<ArgumentsHolder> exceptionHandlingProvider() {
//         TestClass instance = new TestClass();
//         return // private method → IllegalAccessException wrapped
//         Stream.// private method → IllegalAccessException wrapped
//         of(// method throws RuntimeException → wrapped
//         new ArgumentsHolder(instance, getDeclaredMethod(TestClass.class, "secret"), new Object[] {}, RuntimeException.class, "IllegalAccessException"), // method throws checked IOException → wrapped
//         new ArgumentsHolder(instance, getMethod(TestClass.class, "throwsRuntime", String.class), new Object[] { "boom" }, RuntimeException.class, "Exception thrown inside reflectively called method"), // method throws AssertionError (Error) → wrapped
//         new ArgumentsHolder(instance, getMethod(TestClass.class, "throwsChecked", String.class), new Object[] { "boom" }, RuntimeException.class, "Exception thrown inside reflectively called method"), new ArgumentsHolder(instance, getMethod(TestClass.class, "throwsError"), new Object[] {}, RuntimeException.class, "Exception thrown inside reflectively called method"));
//     }

    /**
     * Provides test data for argument‑mismatch scenarios.
     */
//     static Stream<ArgumentsHolder> argumentMismatchProvider() {
//         TestClass instance = new TestClass();
//         return // missing argument
//         Stream.// missing argument
//         of(// wrong type argument
//         new ArgumentsHolder(instance, getMethod(TestClass.class, "greet", String.class), new Object[] {}, IllegalArgumentException.class, null), new ArgumentsHolder(instance, getMethod(TestClass.class, "greet", String.class), new Object[] { 123 }, IllegalArgumentException.class, null));
//     }

    /**
     * Helper to retrieve a public method, rethrowing checked exceptions as unchecked.
     */
    private static Method getMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper to retrieve a declared (possibly private) method.
     */
    private static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Attempts to locate a method with the given name and parameter types on the
     * supplied class (including inherited methods).  If the method cannot be
     * found an empty {@link Optional} is returned.
     *
     * @param clazz        the class on which to search; never {@code null}
     * @param name         the method name; never {@code null}
     * @param paramTypes   the parameter types; may be empty but never {@code null}
     * @return an {@link Optional} containing the {@link Method} if found
     */
    public static Optional<Method> findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            // First try public methods (including inherited)
            return Optional.of(clazz.getMethod(name, paramTypes));
        } catch (NoSuchMethodException ignored) {
            // Fall back to declared methods (including private/protected)
            try {
                Method declared = clazz.getDeclaredMethod(name, paramTypes);
                declared.setAccessible(true);
                return Optional.of(declared);
            } catch (NoSuchMethodException ex) {
                return Optional.empty();
            }
        }
    }

    /**
     * Returns {@code true} if a method with the given signature is present on
     * the supplied class.
     *
     * @param clazz       the class to inspect; never {@code null}
     * @param name        the method name; never {@code null}
     * @param paramTypes  the parameter types; may be empty but never {@code null}
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... paramTypes) {
        return findMethod(clazz, name, paramTypes).isPresent();
    }

//     @ParameterizedTest(name = "[{index}] Null‑check: {0}")
//     @MethodSource("nullCheckProvider")
//     void testNullChecks(ArgumentsHolder holder) {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(holder.instance, holder.method, holder.args));
//         assertTrue(ex.getMessage().contains(holder.expectedMessageFragment));
//     }

//     @ParameterizedTest(name = "[{index}] Exception handling: {0}")
//     @MethodSource("exceptionHandlingProvider")
//     void testExceptionHandling(ArgumentsHolder holder) {
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(holder.instance, holder.method, holder.args));
//         assertTrue(ex.getMessage().contains(holder.expectedMessageFragment));
//         assertNotNull(ex.getCause());
//         switch(holder.method.getName()) {
//             case "secret":
//                 assertTrue(ex.getCause() instanceof IllegalAccessException);
//                 break;
//             case "throwsRuntime":
//                 assertTrue(ex.getCause() instanceof IllegalArgumentException);
//                 assertEquals("boom", ex.getCause().getMessage());
//                 break;
//             case "throwsChecked":
//                 assertTrue(ex.getCause() instanceof IOException);
//                 assertEquals("boom", ex.getCause().getMessage());
//                 break;
//             case "throwsError":
//                 assertTrue(ex.getCause() instanceof AssertionError);
//                 assertEquals("boom", ex.getCause().getMessage());
//                 break;
//             default:
//                 fail("Unexpected method name: " + holder.method.getName());
//         }
//     }

//     @ParameterizedTest(name = "[{index}] Argument mismatch: {0}")
//     @MethodSource("argumentMismatchProvider")
//     void testArgumentMismatchPropagation(ArgumentsHolder holder) {
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(holder.instance, holder.method, holder.args));
//         assertNotNull(ex.getMessage());
//     }
}