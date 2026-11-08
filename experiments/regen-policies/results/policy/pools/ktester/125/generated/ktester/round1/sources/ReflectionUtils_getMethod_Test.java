package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_Test {

    /**
     * Simple class with a private method.
     */
    static class PrivateMethodHolder {

        @SuppressWarnings("unused")
        private String secret() {
            return "hidden";
        }
    }

    /**
     * Sub‑class that does not declare {@code toString()} – it inherits from {@code Object}.
     */
    static class SubClass extends PrivateMethodHolder {
        // No additional members
    }

    /**
     * Helper class with a private method used for the “private method” test.
     */
    static class ReflectionUtilsTestHelper {

        @SuppressWarnings("unused")
        private void privateHelperMethod() {
            // no‑op
        }
    }

    /**
     * Superclass containing a protected method.
     */
    static class SuperClass {

        @SuppressWarnings("unused")
        protected String protectedSuperMethod() {
            return "super";
        }
    }

    /**
     * Subclass that inherits the protected method from {@link SuperClass}.
     */
    static class SubClass2 extends SuperClass {
        // no additional members
    }

    /**
     * Class with several overloaded {@code process} methods.
     */
    static class OverloadedMethods {

        public void process() {
        }

        public void process(int i) {
        }

        public void process(int i, String s) {
        }

        public void process(Integer i, String s) {
            // different signature (wrapper)
        }
    }

    /**
     * Class with a var‑args method.
     */
    static class VarArgsHelper {

        @SuppressWarnings("unused")
        public String concat(String... parts) {
            return String.join("", parts);
        }
    }

    /**
     * Simple container for test arguments – keeps the {@code MethodSource} signatures tidy.
     */
    private static class ArgumentsHolder {

        final Class<?> c;

        final String methodName;

        final Class<?>[] types;

        // may be null when a null result is expected
        final Method expectedMethod;

        // used only for null‑argument validation
        final String expectedMessage;

        ArgumentsHolder(Class<?> c, String methodName, Class<?>[] types, Method expectedMethod) {
            this.c = c;
            this.methodName = methodName;
            this.types = types;
            this.expectedMethod = expectedMethod;
            this.expectedMessage = null;
        }

        ArgumentsHolder(Class<?> c, String methodName, Class<?>[] types, String expectedMessage) {
            this.c = c;
            this.methodName = methodName;
            this.types = types;
            this.expectedMethod = null;
            this.expectedMessage = expectedMessage;
        }

        @Override
        public String toString() {
            return String.format("%s.%s(%s)", c.getSimpleName(), methodName, types == null ? "null" : java.util.Arrays.toString(types));
        }
    }

    /**
     * Data for the “null argument validation” group.
     */
    static Stream<ArgumentsHolder> nullArgumentProvider() {
        return Stream.of(new ArgumentsHolder(null, "anyMethod", new Class<?>[] {}, "class cannot be null"), new ArgumentsHolder(String.class, null, new Class<?>[] {}, "methodName cannot be null"));
    }

    /**
     * Data for the “public method declared in class” group.
     */
    static Stream<ArgumentsHolder> publicMethodProvider() {
        return Stream.of(new ArgumentsHolder(String.class, "length", new Class<?>[] {}, String.class.getDeclaredMethod("length")));
    }

    /**
     * Data for the “private method declared in class” group.
     */
    static Stream<ArgumentsHolder> privateMethodProvider() {
        return Stream.of(new ArgumentsHolder(ReflectionUtilsTestHelper.class, "privateHelperMethod", new Class<?>[] {}, ReflectionUtilsTestHelper.class.getDeclaredMethod("privateHelperMethod")));
    }

    /**
     * Data for the “protected method in superclass” group.
     */
    static Stream<ArgumentsHolder> protectedMethodProvider() {
        return Stream.of(new ArgumentsHolder(SuperClass.class, "protectedSuperMethod", new Class<?>[] {}, SuperClass.class.getDeclaredMethod("protectedSuperMethod")));
    }

    /**
     * Data for the “overloaded method with specific parameter types” group.
     */
    static Stream<ArgumentsHolder> overloadedMethodProvider() {
        return Stream.of(new ArgumentsHolder(OverloadedMethods.class, "process", new Class<?>[] { int.class, String.class }, OverloadedMethods.class.getDeclaredMethod("process", int.class, String.class)));
    }

    /**
     * Data for the “primitive vs wrapper type mismatch returns null” group.
     */
    static Stream<ArgumentsHolder> primitiveWrapperMismatchProvider() {
        return Stream.of(new ArgumentsHolder(OverloadedMethods.class, "process", new Class<?>[] { Integer.class, String.class }, // expected null
        null));
    }

    /**
     * Data for the “empty types array for no‑arg method” group.
     */
    static Stream<ArgumentsHolder> emptyTypesProvider() {
        return Stream.of(new ArgumentsHolder(Date.class, "toString", new Class<?>[] {}, Date.class.getDeclaredMethod("toString")));
    }

    /**
     * Data for the “varargs method with array type” group.
     */
    static Stream<ArgumentsHolder> varArgsProvider() {
        return Stream.of(new ArgumentsHolder(VarArgsHelper.class, "concat", new Class<?>[] { String[].class }, VarArgsHelper.class.getDeclaredMethod("concat", String[].class)));
    }

    @Test
    void shouldRetrievePublicMethodFromObject() {
        // given
        Class<?> target = Object.class;
        String methodName = "toString";
        // when
        Method method = ReflectionUtils.getMethod(target, methodName);
        // then
        assertNotNull(method, "Method should be found");
        assertEquals(methodName, method.getName());
        assertTrue(Modifier.isPublic(method.getModifiers()));
    }

    @Test
    void shouldRetrievePrivateMethodAndMakeItAccessible() throws Exception {
        // given
        Class<?> target = PrivateMethodHolder.class;
        String methodName = "secret";
        // when
        Method method = ReflectionUtils.getMethod(target, methodName);
        assertNotNull(method, "Private method should be located");
        assertEquals(methodName, method.getName());
        // then – the method should be accessible (setAccessible(true) was attempted)
        assertTrue(method.canAccess(new PrivateMethodHolder()), "Method should be accessible after ReflectionUtils processing");
        // Verify that invoking the method works
        PrivateMethodHolder instance = new PrivateMethodHolder();
        Object result = method.invoke(instance);
        assertEquals("hidden", result);
    }

    @Test
    void shouldFindInheritedMethodWhenDeclaredInSuperclass() {
        // given
        Class<?> target = SubClass.class;
        String methodName = "toString";
        // when
        Method method = ReflectionUtils.getMethod(target, methodName);
        // then
        assertNotNull(method, "Inherited method should be found");
        assertEquals(methodName, method.getName());
        assertEquals(Object.class, method.getDeclaringClass(), "The declaring class should be Object because SubClass does not override toString()");
    }

    @Test
    void shouldReturnSameMethodInstanceFromCache() {
        // given
        Class<?> target = Object.class;
        String methodName = "hashCode";
        // when – first lookup
        Method first = ReflectionUtils.getMethod(target, methodName);
        // when – second lookup (should hit the cache)
        Method second = ReflectionUtils.getMethod(target, methodName);
        // then
        assertNotNull(first);
        assertNotNull(second);
        assertSame(first, second, "Subsequent calls should return the cached Method instance");
    }

    @Test
    void shouldReturnNullWhenMethodDoesNotExist() {
        // given
        Class<?> target = Object.class;
        String methodName = "nonExistentMethod";
        // when
        Method result = ReflectionUtils.getMethod(target, methodName);
        // then
        assertNull(result, "A non‑existent method should result in null");
    }

    @Test
    void shouldHandleMethodWithParameterTypes() {
        // given
        Class<?> target = java.util.List.class;
        String methodName = "contains";
        Class<?>[] paramTypes = new Class<?>[] { Object.class };
        // when
        Method method = ReflectionUtils.getMethod(target, methodName, paramTypes);
        // then
        assertNotNull(method);
        assertEquals(methodName, method.getName());
        assertArrayEquals(paramTypes, method.getParameterTypes());
    }

    @Test
    void shouldReturnNullWhenParameterTypesDoNotMatch() {
        // given
        Class<?> target = java.util.List.class;
        String methodName = "contains";
        Class<?>[] wrongParamTypes = new Class<?>[] { String.class, Integer.class };
        // when
        Method method = ReflectionUtils.getMethod(target, methodName, wrongParamTypes);
        // then
        assertNull(method, "Mismatched parameter list should yield null");
    }

    @ParameterizedTest(name = "[{index}] null argument: {0}")
    @MethodSource("nullArgumentProvider")
    void shouldValidateNullArguments(ArgumentsHolder holder) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(holder.c, holder.methodName, holder.types));
        assertTrue(ex.getMessage().contains(holder.expectedMessage), () -> "Expected message to contain '" + holder.expectedMessage + "' but was: " + ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] public method: {0}")
    @MethodSource("publicMethodProvider")
    void shouldRetrievePublicMethodDeclaredInClass(ArgumentsHolder holder) throws Exception {
        Method method = ReflectionUtils.getMethod(holder.c, holder.methodName, holder.types);
        assertNotNull(method);
        assertEquals(holder.expectedMethod.getName(), method.getName());
        assertArrayEquals(holder.expectedMethod.getParameterTypes(), method.getParameterTypes());
        assertTrue(Modifier.isPublic(method.getModifiers()));
    }

    @ParameterizedTest(name = "[{index}] private method: {0}")
    @MethodSource("privateMethodProvider")
    void shouldRetrievePrivateMethodAndMakeAccessible(ArgumentsHolder holder) throws Exception {
        Method method = ReflectionUtils.getMethod(holder.c, holder.methodName, holder.types);
        assertNotNull(method);
        assertEquals(holder.expectedMethod.getName(), method.getName());
        // The utility attempts to call setAccessible(true); verify that we can invoke it.
        assertTrue(method.canAccess(new ReflectionUtilsTestHelper()), "Method should be accessible after ReflectionUtils processing");
        // Invoke to ensure it works (no exception expected)
        Object instance = holder.c.getDeclaredConstructor().newInstance();
        method.invoke(instance);
    }

    @ParameterizedTest(name = "[{index}] protected method in superclass: {0}")
    @MethodSource("protectedMethodProvider")
    void shouldFindProtectedMethodInSuperclass(ArgumentsHolder holder) throws Exception {
        Method method = ReflectionUtils.getMethod(SubClass2.class, holder.methodName, holder.types);
        assertNotNull(method);
        assertEquals(holder.expectedMethod.getName(), method.getName());
        assertEquals(SuperClass.class, method.getDeclaringClass(), "Method should be declared in the superclass");
        assertTrue(Modifier.isProtected(method.getModifiers()));
    }

    @ParameterizedTest(name = "[{index}] overloaded method exact match: {0}")
    @MethodSource("overloadedMethodProvider")
    void shouldSelectCorrectOverloadedMethod(ArgumentsHolder holder) throws Exception {
        Method method = ReflectionUtils.getMethod(holder.c, holder.methodName, holder.types);
        assertNotNull(method);
        assertEquals(holder.expectedMethod, method);
    }

    @ParameterizedTest(name = "[{index}] primitive vs wrapper mismatch returns null")
    @MethodSource("primitiveWrapperMismatchProvider")
    void shouldReturnNullWhenPrimitiveWrapperMismatch(ArgumentsHolder holder) {
        Method method = ReflectionUtils.getMethod(holder.c, holder.methodName, holder.types);
        assertNull(method, "Mismatched primitive/wrapper types should yield null");
    }

    @ParameterizedTest(name = "[{index}] empty types array for no‑arg method")
    @MethodSource("emptyTypesProvider")
    void shouldHandleEmptyTypesArrayForNoArgMethod(ArgumentsHolder holder) throws Exception {
        Method method = ReflectionUtils.getMethod(holder.c, holder.methodName, holder.types);
        assertNotNull(method);
        assertEquals(holder.expectedMethod, method);
    }

    @ParameterizedTest(name = "[{index}] varargs method with array type")
    @MethodSource("varArgsProvider")
    void shouldFindVarArgsMethodWithArrayParameter(ArgumentsHolder holder) throws Exception {
        Method method = ReflectionUtils.getMethod(holder.c, holder.methodName, holder.types);
        assertNotNull(method);
        assertEquals(holder.expectedMethod, method);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenTypesArrayIsNull() {
        // The varargs parameter is explicitly passed as null.
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethod(String.class, "length", (Class<?>[]) null));
    }
}
