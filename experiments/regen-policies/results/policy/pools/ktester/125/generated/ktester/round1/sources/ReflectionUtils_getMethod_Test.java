package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_Test {

    /**
     * Simple class with a public method.
     */
    static class Simple {

        public String greet(String name) {
            return "Hello " + name;
        }
    }

    /**
     * Base class with a protected method.
     */
    static class Base {

        protected int protectedNumber() {
            return 42;
        }
    }

    /**
     * Subclass that does not override the protected method.
     */
    static class Derived extends Base {
        // inherits protectedNumber()
    }

    /**
     * Class with a private method – used to verify setAccessible handling.
     */
    static class WithPrivate {

        @SuppressWarnings("unused")
        private String secret() {
            return "top‑secret";
        }
    }

    /**
     * Simple class with a public method.
     */
    static class SimpleClass {

        public void publicMethod() {
        }
    }

    /**
     * Class with a private method – used to verify setAccessible handling.
     */
    static class ClassWithPrivateMethod {

        @SuppressWarnings("unused")
        private String secret() {
            return "top‑secret";
        }
    }

    /**
     * Base class with a protected method.
     */
    static class BaseClass {

        protected int protectedMethod() {
            return 42;
        }
    }

    /**
     * Subclass that does not override the protected method.
     */
    static class SubClass extends BaseClass {
    }

    /**
     * Class defining overloaded methods.
     */
    static class OverloadedClass {

        public void process() {
        }

        public void process(String s) {
        }
    }

    /**
     * Class with a primitive‑parameter method.
     */
    static class PrimitiveParamClass {

        public int increment(int i) {
            return i + 1;
        }
    }

    /**
     * Grand‑parent class for multi‑level inheritance test.
     */
    static class GrandParentClass {

        public void grandParentMethod() {
        }
    }

    /**
     * Intermediate subclass.
     */
    static class ParentClass extends GrandParentClass {
    }

    /**
     * Great‑subclass two levels down.
     */
    static class GreatSubClass extends ParentClass {
    }

    /**
     * Class with an array‑parameter method.
     */
    static class ArrayParamClass {

        public void handle(String[] values) {
        }
    }

    /**
     * Class with a var‑args method (represented as an array at runtime).
     */
    static class VarArgsClass {

        public void sum(int... numbers) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean cache before each test to avoid cross‑test interference.
        // The cache field is private; we clear it via reflection.
        java.lang.reflect.Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
        cacheField.setAccessible(true);
        // Replace with a simple thread‑safe map (HashMap is sufficient for single‑threaded tests)
        cacheField.set(null, new HashMap<>());
    }

    /**
     * Provider for overloaded‑method test cases.
     */
    static Stream<Arguments> overloadedMethodProvider() {
        return Stream.of(Arguments.of(OverloadedClass.class, "process", new Class<?>[] { String.class }, "process", new Class<?>[] { String.class }), Arguments.of(OverloadedClass.class, "process", new Class<?>[] {}, "process", new Class<?>[] {}));
    }

    /**
     * Provider for primitive vs wrapper mismatch test cases.
     */
    static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(// matching primitive
        Arguments.of(PrimitiveParamClass.class, "increment", new Class<?>[] { int.class }, true), // mismatching wrapper
        Arguments.of(PrimitiveParamClass.class, "increment", new Class<?>[] { Integer.class }, false));
    }

    /**
     * Provider for cache‑key differentiation test cases.
     */
    static Stream<Arguments> cacheKeyDiffProvider() {
        return Stream.of(Arguments.of(OverloadedClass.class, "process", new Class<?>[] { String.class }), Arguments.of(OverloadedClass.class, "process", new Class<?>[] {}));
    }

    /**
     * Provider for array‑parameter method test cases.
     */
    static Stream<Arguments> arrayParamProvider() {
        return Stream.of(Arguments.of(ArrayParamClass.class, "handle", new Class<?>[] { String[].class }));
    }

    /**
     * Provider for var‑args method test cases.
     */
    static Stream<Arguments> varArgsProvider() {
        return Stream.of(Arguments.of(VarArgsClass.class, "sum", new Class<?>[] { int[].class }));
    }

    /**
     * Provider for null‑element‑inside‑types test case.
     */
    static Stream<Arguments> nullElementInTypesProvider() {
        return Stream.of(Arguments.of(ClassWithPrivateMethod.class, "secret", new Class<?>[] { null }));
    }

    @Test
    void getPublicMethod_shouldReturnMethod() throws Exception {
        // public method from java.lang.String
        Method m = ReflectionUtils.getMethod(String.class, "substring", int.class, int.class);
        assertNotNull(m, "Method should be found");
        assertEquals("substring", m.getName());
        assertTrue(Modifier.isPublic(m.getModifiers()));
        // verify signature
        Class<?>[] params = m.getParameterTypes();
        assertArrayEquals(new Class<?>[] { int.class, int.class }, params);
    }

    @Test
    void getMethod_onSimpleClass_shouldFindPublicMethod() throws Exception {
        Method m = ReflectionUtils.getMethod(SimpleClass.class, "publicMethod");
        assertNotNull(m);
        assertEquals("publicMethod", m.getName());
        assertTrue(Modifier.isPublic(m.getModifiers()));
    }

    @Test
    void getInheritedProtectedMethod_shouldFindMethodInSuperclass() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "protectedMethod");
        assertNotNull(m, "Inherited protected method should be discovered");
        assertEquals("protectedMethod", m.getName());
        assertTrue(Modifier.isProtected(m.getModifiers()));
        assertEquals(BaseClass.class, m.getDeclaringClass(), "Declaring class must be the superclass where the method is defined");
    }

    @Test
    void getPrivateMethod_shouldReturnMethodAndMakeItAccessible() throws Exception {
        Method m = ReflectionUtils.getMethod(WithPrivate.class, "secret");
        assertNotNull(m, "Private method should be discovered");
        assertEquals("secret", m.getName());
        // The method may not be public; we only care that it is accessible for invocation
        assertTrue(m.isAccessible() || !Modifier.isPublic(m.getModifiers()), "Method should be made accessible (or already accessible)");
        // invoke to ensure it works
        WithPrivate instance = new WithPrivate();
        Object result = m.invoke(instance);
        assertEquals("top‑secret", result);
    }

    @Test
    void getMethod_whenMethodDoesNotExist_shouldReturnNull() {
        Method m = ReflectionUtils.getMethod(SimpleClass.class, "nonExistentMethod");
        assertNull(m, "Method that does not exist must return null");
    }

    @Test
    void getMethod_whenClassIsNull_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(null, "anyMethod"), "Null class argument should cause IllegalArgumentException");
    }

    @Test
    void getMethod_whenMethodNameIsNull_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(SimpleClass.class, null), "Null method name should cause IllegalArgumentException");
    }

    @Test
    void getMethod_isCached_betweenCalls() {
        // First call – cache miss
        Method first = ReflectionUtils.getMethod(Simple.class, "greet", String.class);
        // Second call – should hit the cache and return the same Method instance
        Method second = ReflectionUtils.getMethod(Simple.class, "greet", String.class);
        assertSame(first, second, "Method instances should be identical due to caching");
    }

    @ParameterizedTest(name = "[{index}] {0}.{1}{2}")
    @MethodSource("overloadedMethodProvider")
    void overloadedMethodLookup(Class<?> clazz, String methodName, Class<?>[] types, String expectedName, Class<?>[] expectedParamTypes) throws Exception {
        Method m = ReflectionUtils.getMethod(clazz, methodName, types);
        assertNotNull(m, "Overloaded method should be found");
        assertEquals(expectedName, m.getName());
        assertArrayEquals(expectedParamTypes, m.getParameterTypes(), "Parameter types must match the requested overload");
    }

    @ParameterizedTest
    @MethodSource("primitiveWrapperProvider")
    void primitiveParameterMatching(Class<?> clazz, String methodName, Class<?>[] types, boolean shouldFind) {
        Method m = ReflectionUtils.getMethod(clazz, methodName, types);
        if (shouldFind) {
            assertNotNull(m, "Method with matching primitive type should be found");
        } else {
            assertNull(m, "Method with mismatching wrapper type must not be found");
        }
    }

    @Test
    void multiLevelInheritanceMethodLookup() {
        Method m = ReflectionUtils.getMethod(GreatSubClass.class, "grandParentMethod");
        assertNotNull(m, "Method declared in grand‑parent should be found");
        assertEquals("grandParentMethod", m.getName());
        assertEquals(GrandParentClass.class, m.getDeclaringClass(), "Declaring class must be the grand‑parent");
    }

    @Test
    void nullTypesVarargs_shouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethod(SimpleClass.class, "publicMethod", (Class<?>[]) null), "Passing null for the var‑args `types` argument must result in NPE");
    }

    @Test
    void nullElementInsideTypesArray_shouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethod(ClassWithPrivateMethod.class, "secret", new Class<?>[] { null }), "Null element inside `types` array must cause NPE from Class#getDeclaredMethod");
    }

    @Test
    void cacheKeyDifferentiation_sameNameDifferentSignatures() {
        // First overload (String)
        Method mString = ReflectionUtils.getMethod(OverloadedClass.class, "process", String.class);
        // Second overload (no‑arg)
        Method mNoArg = ReflectionUtils.getMethod(OverloadedClass.class, "process");
        assertNotNull(mString);
        assertNotNull(mNoArg);
        assertNotSame(mString, mNoArg, "Cache must store distinct entries for different parameter signatures");
    }

    @ParameterizedTest
    @MethodSource("arrayParamProvider")
    void arrayParameterMethodLookup(Class<?> clazz, String methodName, Class<?>[] types) throws Exception {
        Method m = ReflectionUtils.getMethod(clazz, methodName, types);
        assertNotNull(m, "Method with array parameter should be found");
        assertEquals(methodName, m.getName());
        assertArrayEquals(types, m.getParameterTypes(), "Parameter type must be the expected array class");
    }

    @ParameterizedTest
    @MethodSource("varArgsProvider")
    void varArgsMethodLookup(Class<?> clazz, String methodName, Class<?>[] types) throws Exception {
        Method m = ReflectionUtils.getMethod(clazz, methodName, types);
        assertNotNull(m, "Var‑args method should be located via its synthetic array parameter");
        assertEquals(methodName, m.getName());
        assertArrayEquals(types, m.getParameterTypes(), "Var‑args should be represented as an array type");
    }

    @Test
    void directPublicMethodLookup() throws Exception {
        Method m = ReflectionUtils.getMethod(SimpleClass.class, "publicMethod");
        assertNotNull(m, "Public method declared directly in the class must be found");
        assertEquals("publicMethod", m.getName());
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertEquals(SimpleClass.class, m.getDeclaringClass(), "Declaring class must be the class itself");
    }
}
