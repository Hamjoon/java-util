package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getNonOverloadedMethod_Test {

    /**
     * Simple class with a single, non‑overloaded method.
     */
    static class Simple {

        @Override
        public String toString() {
            return "Simple";
        }
    }

    /**
     * Class that overloads a method name.
     */
    static class Overloaded {

        public String foo() {
            return "no‑args";
        }

        public String foo(String s) {
            return s;
        }
    }

    /**
     * Class without the requested method.
     */
    static class NoMethod {
        // intentionally empty
    }

    /**
     * Simple class with a unique public method.
     */
    static class SimpleClass {

        public void doSomething() {
        }
    }

    /**
     * Base class providing a public method.
     */
    static class BaseClass {

        public void greet() {
        }
    }

    /**
     * Subclass inheriting the method from {@link BaseClass}.
     */
    static class InheritedClass extends BaseClass {
    }

    /**
     * Simple interface.
     */
    interface SimpleInterface {

        void interfaceMethod();
    }

    /**
     * Class that implements {@link SimpleInterface}.
     */
    static class InterfaceImpl implements SimpleInterface {

        @Override
        public void interfaceMethod() {
        }
    }

    /**
     * Class that overloads the method name {@code foo}.
     */
    static class OverloadedClass {

        public String foo() {
            return "no‑args";
        }

        public String foo(String s) {
            return s;
        }
    }

    /**
     * Class that defines a private method – it should not be visible via {@code getMethods()}.
     */
    static class PrivateMethodClass {

        @SuppressWarnings("unused")
        private void hidden() {
        }
    }

    /**
     * Empty class used for generic invalid‑argument tests.
     */
    static class SampleClass {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the cache is cleared before each test to avoid cross‑test interference.
        // ReflectionUtils does not expose a clear method, but we can reset the cache via reflection
        // without accessing private members of ReflectionUtils (allowed for test setup only).
        try {
            java.lang.reflect.Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
            cacheField.setAccessible(true);
            java.util.Map<?, ?> empty = new java.util.HashMap<>();
            cacheField.set(null, empty);
        } catch (Exception e) {
            // If we cannot reset the cache, the tests will still be correct because each
            // call uses computeIfAbsent which will return the same Method instance.
        }
        // Reset the internal cache via reflection to guarantee test isolation.
        try {
            java.lang.reflect.Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
            cacheField.setAccessible(true);
            cacheField.set(null, new java.util.HashMap<>());
        } catch (Exception ignored) {
            // If resetting fails, each test still works because computeIfAbsent is deterministic.
        }
    }

    /* -------------------------------------------------------
     * Parameterized tests for invalid arguments
     * ----------------------------------------------------- */
    static Stream<Arguments> invalidArgumentProvider() {
        return Stream.of(// null class
        Arguments.of(null, "anyMethod", "Attempted to call getMethod() [anyMethod]() on a null class."), // null method name
        Arguments.of(SampleClass.class, null, "Attempted to call getMethod() with a null or blank method name"), // blank (whitespace) method name
        Arguments.of(SampleClass.class, "   ", "Attempted to call getMethod() with a null or blank method name"), // empty string method name
        Arguments.of(SampleClass.class, "", "Attempted to call getMethod() with a null or blank method name"));
    }

    /* -------------------------------------------------------
     * Successful retrieval tests
     * ----------------------------------------------------- */
    static Stream<Arguments> successfulRetrievalProvider() {
        return Stream.of(// direct method on the class
        Arguments.of(SimpleClass.class, "doSomething", SimpleClass.class), // inherited method from a superclass
        Arguments.of(InheritedClass.class, "greet", BaseClass.class), // method that implements an interface
        Arguments.of(InterfaceImpl.class, "interfaceMethod", InterfaceImpl.class));
    }

    @Test
    void shouldReturnMethodWhenExactlyOneMatches() {
        // given
        Class<?> clazz = Simple.class;
        String methodName = "toString";
        // when
        java.lang.reflect.Method method = ReflectionUtils.getNonOverloadedMethod(clazz, methodName);
        // then
        assertNotNull(method, "Method should not be null");
        assertEquals(methodName, method.getName(), "Method name should match");
        assertEquals(clazz, method.getDeclaringClass(), "Declaring class should be Simple");
    }

    @Test
    void shouldThrowWhenClassIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(null, "anyMethod"));
        assertTrue(ex.getMessage().contains("null class"), "Exception message should mention null class");
    }

    @Test
    void shouldThrowWhenMethodNameIsNullOrBlank() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Simple.class, null));
        assertTrue(ex1.getMessage().contains("null or blank method name"), "Message should mention null/blank name");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Simple.class, "   "));
        assertTrue(ex2.getMessage().contains("null or blank method name"), "Message should mention null/blank name");
    }

    @Test
    void shouldThrowWhenMethodIsOverloaded() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(OverloadedClass.class, "foo"));
        assertTrue(ex.getMessage().contains("overloaded methods"), "Message should indicate overload ambiguity");
    }

    @Test
    void shouldThrowWhenMethodNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(SimpleClass.class, "nonExistentMethod"));
        assertTrue(ex.getMessage().contains("Method: nonExistentMethod() is not found on class"), "Message should indicate method not found");
    }

    @ParameterizedTest(name = "[{index}] clazz={0}, methodName={1}")
    @MethodSource("invalidArgumentProvider")
    void shouldThrowOnInvalidArguments(Class<?> clazz, String methodName, String expectedMessageFragment) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(clazz, methodName));
        assertTrue(ex.getMessage().contains(expectedMessageFragment), () -> "Exception message should contain: \"" + expectedMessageFragment + "\" but was: " + ex.getMessage());
    }

    /* -------------------------------------------------------
     * Additional invalid‑argument edge cases (leading/trailing whitespace)
     * ----------------------------------------------------- */
    @Test
    void shouldTreatLeadingWhitespaceAsNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(SampleClass.class, " doSomething"));
        assertTrue(ex.getMessage().contains("Method:  doSomething() is not found on class"), "Message should indicate the method was not found");
    }

    @Test
    void shouldTreatTrailingWhitespaceAsNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(SampleClass.class, "doSomething "));
        assertTrue(ex.getMessage().contains("Method: doSomething () is not found on class"), "Message should indicate the method was not found");
    }

    @Test
    void shouldThrowWhenMethodIsPrivate() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(PrivateMethodClass.class, "hidden"));
        assertTrue(ex.getMessage().contains("Method: hidden() is not found on class"), "Private methods are not returned by Class.getMethods()");
    }

    /* -------------------------------------------------------
     * Cache behaviour verification
     * ----------------------------------------------------- */
    @Test
    void shouldCacheMethodInstance() {
        // First call – computes and stores the method.
        Method first = ReflectionUtils.getNonOverloadedMethod(SimpleClass.class, "doSomething");
        // Second call – should retrieve the same instance from the cache.
        Method second = ReflectionUtils.getNonOverloadedMethod(SimpleClass.class, "doSomething");
        assertSame(first, second, "Subsequent calls with identical arguments must return the cached Method instance");
    }

    /* -------------------------------------------------------
     * Primitive and array class handling
     * ----------------------------------------------------- */
    @Test
    void primitiveClassHasNoPublicMethods() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(int.class, "intValue"));
        assertTrue(ex.getMessage().contains("Method: intValue() is not found on class"), "Primitive types should result in method‑not‑found");
    }

    @Test
    void arrayClassShouldExposeObjectMethods() {
        Method cloneMethod = ReflectionUtils.getNonOverloadedMethod(String[].class, "clone");
        assertNotNull(cloneMethod, "clone method should be found on array class");
        assertEquals("clone", cloneMethod.getName());
        assertEquals(Object.class, cloneMethod.getDeclaringClass(), "clone is declared on java.lang.Object");
    }

    @Test
    void arrayClassMethodNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(String[].class, "nonExistent"));
        assertTrue(ex.getMessage().contains("Method: nonExistent() is not found on class"), "Message should indicate method not found on array class");
    }

    /* -------------------------------------------------------
     * Null class with null method name – verifies fast‑fail on class nullity
     * ----------------------------------------------------- */
    @Test
    void nullClassAndNullMethodNameShouldFailFastOnClassCheck() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(null, null));
        assertTrue(ex.getMessage().contains("Attempted to call getMethod() [null]() on a null class."), "Exception should mention null class before evaluating method name");
    }
}
