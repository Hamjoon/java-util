package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ReflectionUtils_call_2_Test {

    /**
     * Simple class used as the reflection target.
     */
    static class Dummy {

        String greet(String name) {
            return "Hello, " + name;
        }

        /**
         * Method that deliberately throws a checked exception to verify wrapping.
         */
        void explode() throws Exception {
            throw new Exception("boom");
        }
    }

    /**
     * Simple class with a public method and two overloads.
     */
    static class Sample {

        public String greet(String name) {
            return "Hello " + name;
        }

        public String overloaded(Integer i) {
            return "two";
        }

        public String overloaded(String s) {
            return "one";
        }
    }

    /**
     * Class containing a private method – used to trigger IllegalAccessException via the overload API.
     */
    static class SampleWithPrivateMethod {

        @SuppressWarnings("unused")
        private String privateGreet() {
            return "secret";
        }
    }

    /**
     * Class whose method throws a runtime exception, causing an InvocationTargetException.
     */
    static class SampleThrowsException {

        public void boom() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * Helper holder for parameterised test data.
     */
    private static class ArgumentsHolder {

        final Object instance;

        final String methodName;

        final Object[] args;

        final String expected;

        ArgumentsHolder(Object instance, String methodName, Object[] args, String expected) {
            this.instance = instance;
            this.methodName = methodName;
            this.args = args;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return methodName + "(" + java.util.Arrays.toString(args) + ") => " + expected;
        }
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method != null && void.class.equals(method.getReturnType());
    }

    /**
     * Find a method with the given name and parameter types on the supplied class.
     * Returns {@code null} if no such method exists.
     *
     * @param clazz        the class to search
     * @param methodName   the name of the method
     * @param paramTypes   the parameter types (may be empty)
     * @return the {@link Method} or {@code null}
     */
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        if (clazz == null || methodName == null) {
            return null;
        }
        try {
            // First try public methods (including inherited)
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ignored) {
            // Then try declared methods (including private/protected)
            try {
                Method m = clazz.getDeclaredMethod(methodName, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ex) {
                // Search super‑classes recursively
                Class<?> superclass = clazz.getSuperclass();
                if (superclass != null) {
                    return findMethod(superclass, methodName, paramTypes);
                }
                return null;
            }
        }
    }

    /**
     * Returns {@code true} if a method with the given name and parameter types
     * exists on the supplied class.
     *
     * @param clazz       the class to inspect
     * @param methodName  the method name
     * @param paramTypes  the parameter types
     * @return {@code true} if such a method is present
     */
    public static boolean isMethodPresent(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        return findMethod(clazz, methodName, paramTypes) != null;
    }

    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    public static boolean isFinal(Method method) {
        return Modifier.isFinal(method.getModifiers());
    }

    public static boolean isSynthetic(Method method) {
        return method.isSynthetic();
    }

    public static boolean isBridge(Method method) {
        return method.isBridge();
    }

    public static boolean isDefaultMethod(Method method) {
        return method.isDefault();
    }

    public static boolean isAnnotationPresent(Method method, Class<? extends Annotation> annotationClass) {
        return method.isAnnotationPresent(annotationClass);
    }

    public static List<Method> getAllDeclaredMethods(Class<?> clazz) {
        List<Method> result = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            result.add(m);
        }
        return result;
    }

    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    public static Method[] getDeclaredMethodsRecursive(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        while (clazz != null) {
            for (Method m : clazz.getDeclaredMethods()) {
                methods.add(m);
            }
            clazz = clazz.getSuperclass();
        }
        return methods.toArray(new Method[0]);
    }

    /**
     * Data for successful invocation of a public method.
     */
    static Stream<ArgumentsHolder> successfulInvocationProvider() {
        return Stream.of(new ArgumentsHolder(new Sample(), "greet", new Object[] { "World" }, "Hello World"));
    }

    /**
     * Data for overloaded method selection where the argument type matters.
     */
    static Stream<ArgumentsHolder> overloadedSelectionProvider() {
        return Stream.of(new ArgumentsHolder(new Sample(), "overloaded", new Object[] { 42 }, "two"));
    }

    @Test
    void callMethodByNameWithArguments_returnsResult() {
        // given
        Dummy dummy = new Dummy();
        String methodName = "greet";
        Object[] args = new Object[] { "World" };
        // when
        Object result = ReflectionUtils.call(dummy, methodName, args);
        // then
        Assertions.assertEquals("Hello, World", result);
    }

    @Test
    void callMethodByNameWhenMethodThrowsException_wrapsInRuntimeException() {
        // given
        Dummy dummy = new Dummy();
        String methodName = "explode";
        // when / then
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> ReflectionUtils.call(dummy, methodName));
        // The cause should be the original checked exception.
        Assertions.assertTrue(ex.getCause() instanceof Exception);
        Assertions.assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void callMethodByMethodObject_returnsResult() throws NoSuchMethodException {
        // given
        Dummy dummy = new Dummy();
        Method method = Dummy.class.getMethod("greet", String.class);
        Object[] args = new Object[] { "JUnit" };
        // when
        Object result = ReflectionUtils.call(dummy, method, args);
        // then
        Assertions.assertEquals("Hello, JUnit", result);
    }

    @Test
    void callMethodByMethodObjectWhenIllegalAccess_wrapsInRuntimeException() throws NoSuchMethodException {
        // given
        Dummy dummy = new Dummy();
        // Create a method object for a private method (simulate illegal access)
        Method privateMethod = Dummy.class.getDeclaredMethod("greet", String.class);
        // ensure illegal access
        privateMethod.setAccessible(false);
        // when / then
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> ReflectionUtils.call(dummy, privateMethod, "test"));
        Assertions.assertTrue(ex.getMessage().contains("IllegalAccessException"));
        Assertions.assertTrue(ex.getCause() instanceof IllegalAccessException);
    }

    @Test
    void callMethodByMethodObjectWhenInvocationTargetException_wrapsCause() throws NoSuchMethodException {
        // given
        Dummy dummy = new Dummy();
        Method method = Dummy.class.getMethod("explode");
        // when / then
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> ReflectionUtils.call(dummy, method));
        // The RuntimeException should wrap the target exception (Exception with message "boom").
        Assertions.assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method"));
        Assertions.assertTrue(ex.getCause() instanceof Exception);
        Assertions.assertEquals("boom", ex.getCause().getMessage());
    }

    @ParameterizedTest(name = "[{index}] call({0}, \"{1}\", {2}) → {3}")
    @MethodSource("successfulInvocationProvider")
    void call_successfulInvocation_returnsResult(ArgumentsHolder holder) {
        // when
        Object result = ReflectionUtils.call(holder.instance, holder.methodName, holder.args);
        // then
        Assertions.assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "[{index}] overload selection with args {2}")
    @MethodSource("overloadedSelectionProvider")
    void call_overloadedMethodSelection_returnsResult(ArgumentsHolder holder) {
        // when
        Object result = ReflectionUtils.call(holder.instance, holder.methodName, holder.args);
        // then
        Assertions.assertEquals(holder.expected, result);
    }

    @Test
    void call_nullInstance_throwsIllegalArgumentException() {
        // The API specifies IllegalArgumentException for a null instance.
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, "anyMethod"));
        Assertions.assertTrue(ex.getMessage().contains("instance"));
    }

    @Test
    void call_methodNotFound_throwsIllegalArgumentException() {
        Sample sample = new Sample();
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(sample, "nonExistentMethod"));
        Assertions.assertTrue(ex.getMessage().contains("cannot be found"));
    }

    @Test
    void call_privateMethodByName_throwsIllegalArgumentException() {
        // getMethod only looks for public methods, so a private method results in not‑found.
        SampleWithPrivateMethod target = new SampleWithPrivateMethod();
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(target, "privateGreet"));
        Assertions.assertTrue(ex.getMessage().contains("cannot be found"));
    }

    @Test
    void call_privateMethodByMethodObject_IllegalAccessExceptionWrapped() throws NoSuchMethodException {
        // Using the overload that accepts a Method object lets us provoke IllegalAccessException.
        SampleWithPrivateMethod target = new SampleWithPrivateMethod();
        Method privateMethod = SampleWithPrivateMethod.class.getDeclaredMethod("privateGreet");
        // Ensure the method is not accessible to trigger IllegalAccessException inside ReflectionUtils.call.
        privateMethod.setAccessible(false);
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> ReflectionUtils.call(target, privateMethod));
        Assertions.assertTrue(ex.getMessage().contains("IllegalAccessException"));
        Assertions.assertTrue(ex.getCause() instanceof IllegalAccessException);
    }

    @Test
    void call_methodThrowsException_InvocationTargetExceptionWrapped() {
        SampleThrowsException target = new SampleThrowsException();
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> ReflectionUtils.call(target, "boom"));
        // The RuntimeException should wrap the original IllegalStateException.
        Assertions.assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method"));
        Assertions.assertTrue(ex.getCause() instanceof IllegalStateException);
        Assertions.assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void call_argumentCountMismatch_propagatesIllegalArgumentException() {
        Sample sample = new Sample();
        // greet expects one argument; we provide two.
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(sample, "greet", "World", "Extra"));
        Assertions.assertTrue(ex.getMessage().contains("argument"));
    }
}
