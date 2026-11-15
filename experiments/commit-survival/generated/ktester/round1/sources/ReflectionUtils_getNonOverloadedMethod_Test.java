package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getNonOverloadedMethod_Test {

    /**
     * Simple class with a public no‑arg method and an overloaded variant.
     */
    static class Sample {

        public void greet() {
            /* no‑op */
        }

        public void greet(String name) {
            /* overloaded – should be ignored */
        }

        protected void protectedMethod() {
            /* no‑op */
        }

        private void privateMethod() {
            /* no‑op */
        }
    }

    /**
     * Class that deliberately overloads a method named {@code overloaded}.
     */
    static class OverloadedSample {

        public void overloaded() {
            /* no‑op */
        }

        public void overloaded(String s) {
            /* overloaded – should be rejected */
        }
    }

    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(// null class
        Arguments.of(null, "anyMethod", "Attempted to call getMethod() [anyMethod]()", "null class"), // null method name
        Arguments.of(String.class, null, "Attempted to call getMethod() with a null method name on class: java.lang.String", "null method name"));
    }

    @Test
    void getNonOverloadedMethod_nullClass_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(null, "anyMethod"));
        assertTrue(ex.getMessage().contains("null class"));
    }

    @Test
    void getNonOverloadedMethod_nullMethodName_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Sample.class, null));
        assertTrue(ex.getMessage().contains("null method name"));
    }

    @Test
    void getNonOverloadedMethod_methodNotFound_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(ArrayList.class, "nonExistentMethod"));
        assertTrue(ex.getMessage().contains("Method: nonExistentMethod() is not found on class: java.util.ArrayList"), "Exception message should mention the missing method");
    }

    @Test
    void getNonOverloadedMethod_privateOrProtectedMethod_throwsIllegalArgumentException() {
        // protected method – should not be found by the utility
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Sample.class, "protectedMethod"));
        assertTrue(ex1.getMessage().contains("is not found on class"), "Protected method should be reported as not found");
        // private method – also not found
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Sample.class, "privateMethod"));
        assertTrue(ex2.getMessage().contains("is not found on class"), "Private method should be reported as not found");
    }

    @Test
    void getNonOverloadedMethod_existingPublicNoArgMethod_returnsMethod() {
        Method method = ReflectionUtils.getNonOverloadedMethod(Sample.class, "greet");
        assertNotNull(method, "Method should not be null");
        assertEquals("greet", method.getName());
        assertEquals(0, method.getParameterCount(), "Method should have no parameters");
        assertTrue(method.isPublic(), "Method should be public");
    }

    @Test
    void getNonOverloadedMethod_cachesResult_returnsSameMethodInstance() {
        // First call – populates the cache
        Method first = ReflectionUtils.getNonOverloadedMethod(Sample.class, "greet");
        // Second call – should retrieve the cached Method instance
        Method second = ReflectionUtils.getNonOverloadedMethod(Sample.class, "greet");
        assertSame(first, second, "Subsequent calls should return the same cached Method instance");
    }

    @ParameterizedTest(name = "[{index}] clazz={0}, methodName={1}")
    @MethodSource("nullArgumentProvider")
    void getNonOverloadedMethod_nullArguments_throwsIllegalArgumentException(Class<?> clazz, String methodName, String expectedMessageFragment, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(clazz, methodName), "Expected IllegalArgumentException for " + description);
        assertTrue(ex.getMessage().contains(expectedMessageFragment), () -> "Exception message should contain '" + expectedMessageFragment + "' but was: " + ex.getMessage());
    }

    @Test
    void getNonOverloadedMethod_overloadedMethod_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(OverloadedSample.class, "overloaded"));
        assertTrue(ex.getMessage().contains("Method: overloaded() is not found on class:"), "Overloaded method should cause an IllegalArgumentException");
    }

    @Test
    void getNonOverloadedMethod_arrayListSizeMethod_returnsMethodAndCachesIt() {
        // First invocation – method is looked up and cached
        Method first = ReflectionUtils.getNonOverloadedMethod(ArrayList.class, "size");
        assertNotNull(first, "Method should not be null");
        assertEquals("size", first.getName());
        assertEquals(0, first.getParameterCount(), "size() should have no parameters");
        assertTrue(first.isPublic(), "size() should be public");
        // Second invocation – should return the same Method instance from the cache
        Method second = ReflectionUtils.getNonOverloadedMethod(ArrayList.class, "size");
        assertSame(first, second, "Cached Method instance should be returned on subsequent calls");
    }
}
