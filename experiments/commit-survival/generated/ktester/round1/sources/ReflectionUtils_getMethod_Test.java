package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_Test {

    /**
     * Simple class used for reflection tests.
     */
    static class Sample {

        public void noArgs() {
        }

        public String foo(String s) {
            return s;
        }

        // overloaded variants
        public void overloaded(int i) {
        }

        public void overloaded(String s) {
        }

        // primitive‑type method
        public void primitive(int i) {
        }

        private void privateMethod() {
        }
    }

    /**
     * Another class with a method that has the same signature as Sample.foo.
     */
    static class OtherSample {

        public String foo(String s) {
            return s.toUpperCase();
        }
    }

    /**
     * Helper holder for parameterised invalid‑argument test data.
     */
    private static class InvalidArgumentCase {

        final Class<?> clazz;

        final String methodName;

        final Class<?>[] types;

        // always null for these cases
        final Object expected;

        final String description;

        InvalidArgumentCase(Class<?> clazz, String methodName, Class<?>[] types, Object expected, String description) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.types = types;
            this.expected = expected;
            this.description = description;
        }
    }

    static Stream<InvalidArgumentCase> invalidArgumentProvider() {
        return Stream.of(new InvalidArgumentCase(null, "foo", new Class<?>[] {}, null, "Class argument is null"), new InvalidArgumentCase(Sample.class, null, new Class<?>[] {}, null, "Method name is null"), new InvalidArgumentCase(null, null, null, null, "All arguments are null"), new InvalidArgumentCase(Sample.class, "nonExistingMethod", new Class<?>[] {}, null, "Method name does not exist"));
    }

    @Test
    void getMethodReturnsCorrectMethod() {
        // GIVEN a class and a public method name with a single parameter
        Class<?> clazz = Sample.class;
        String methodName = "foo";
        Class<?>[] paramTypes = new Class<?>[] { String.class };
        // WHEN we request the method via ReflectionUtils
        Method method = ReflectionUtils.getMethod(clazz, methodName, paramTypes);
        // THEN the method is found and its signature matches expectations
        assertNotNull(method, "Method should not be null");
        assertEquals(methodName, method.getName(), "Method name should match");
        assertArrayEquals(paramTypes, method.getParameterTypes(), "Parameter types should match");
        assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    void getMethodReturnsSameInstanceFromCache() {
        // GIVEN the same request executed twice
        Class<?> clazz = Sample.class;
        String methodName = "foo";
        Class<?>[] paramTypes = new Class<?>[] { String.class };
        Method firstCall = ReflectionUtils.getMethod(clazz, methodName, paramTypes);
        Method secondCall = ReflectionUtils.getMethod(clazz, methodName, paramTypes);
        // THEN both calls return the *same* Method object (cached)
        assertNotNull(firstCall, "First call should return a Method");
        assertNotNull(secondCall, "Second call should return a Method");
        assertSame(firstCall, secondCall, "Method instances should be identical (cached)");
    }

    @Test
    void getMethodHandlesNoParameterMethod() {
        // GIVEN a method with no parameters
        Class<?> clazz = Sample.class;
        String methodName = "noArgs";
        // WHEN we request it
        Method method = ReflectionUtils.getMethod(clazz, methodName);
        // THEN we receive the correct Method object
        assertNotNull(method, "Method should not be null");
        assertEquals(methodName, method.getName());
        assertEquals(0, method.getParameterCount(), "Method should have no parameters");
    }

    @Test
    void getMethodReturnsNullWhenMethodDoesNotExist() {
        // GIVEN a method name that does not exist on the target class
        Class<?> clazz = Sample.class;
        String bogusMethod = "doesNotExist";
        // WHEN we request it
        Method method = ReflectionUtils.getMethod(clazz, bogusMethod);
        // THEN the utility returns null rather than throwing
        assertNull(method, "Method should be null for a non‑existent method");
    }

    @Test
    void getMethodReturnsNullWhenParameterTypesDoNotMatch() {
        // GIVEN an existing method name but wrong parameter types
        Class<?> clazz = Sample.class;
        String methodName = "foo";
        Class<?>[] wrongTypes = new Class<?>[] { Integer.class };
        // WHEN we request it
        Method method = ReflectionUtils.getMethod(clazz, methodName, wrongTypes);
        // THEN null is returned because the signature does not match
        assertNull(method, "Method should be null when parameter types do not match");
    }

    @Test
    void getMethodCacheFirstInvocationCreatesEntry() {
        // GIVEN a method that is not yet cached
        Class<?> clazz = Sample.class;
        String methodName = "foo";
        Class<?>[] types = new Class<?>[] { String.class };
        // WHEN we request it for the first time
        Method first = ReflectionUtils.getMethod(clazz, methodName, types);
        // THEN the method is found and cached (subsequent call will hit the cache)
        assertNotNull(first, "First lookup should return a Method");
        assertEquals(methodName, first.getName());
        assertArrayEquals(types, first.getParameterTypes());
    }

    @Test
    void getMethodCacheSecondInvocationReturnsSameInstance() {
        // GIVEN the same request executed twice
        Class<?> clazz = Sample.class;
        String methodName = "foo";
        Class<?>[] types = new Class<?>[] { String.class };
        Method first = ReflectionUtils.getMethod(clazz, methodName, types);
        Method second = ReflectionUtils.getMethod(clazz, methodName, types);
        // THEN both calls return the *same* Method object (cached)
        assertNotNull(second, "Second lookup should also return a Method");
        assertSame(first, second, "Method instances should be identical (cached)");
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("invalidArgumentProvider")
    void getMethodReturnsNullForInvalidArguments(InvalidArgumentCase c) {
        // WHEN we invoke the utility with the supplied arguments
        Method result = ReflectionUtils.getMethod(c.clazz, c.methodName, c.types);
        // THEN the result must be null (the implementation swallows all exceptions)
        assertNull(result, "Expected null for case: " + c.description);
    }

    @Test
    void getMethodOverloadedIntIsCachedSeparately() {
        // GIVEN overloaded(int) signature
        Class<?>[] intParam = new Class<?>[] { int.class };
        // WHEN we request it
        Method m1 = ReflectionUtils.getMethod(Sample.class, "overloaded", intParam);
        // THEN we obtain the correct Method
        assertNotNull(m1);
        assertEquals("overloaded", m1.getName());
        assertArrayEquals(intParam, m1.getParameterTypes());
        // SECOND CALL – should hit cache
        Method m2 = ReflectionUtils.getMethod(Sample.class, "overloaded", intParam);
        assertSame(m1, m2, "Second call for same overloaded signature should return cached instance");
    }

    @Test
    void getMethodOverloadedStringIsDistinctFromInt() {
        // GIVEN two different overload signatures
        Class<?>[] intParam = new Class<?>[] { int.class };
        Class<?>[] stringParam = new Class<?>[] { String.class };
        // WHEN we request each
        Method intMethod = ReflectionUtils.getMethod(Sample.class, "overloaded", intParam);
        Method stringMethod = ReflectionUtils.getMethod(Sample.class, "overloaded", stringParam);
        // THEN both are found and are different Method objects (different cache keys)
        assertNotNull(intMethod);
        assertNotNull(stringMethod);
        assertNotSame(intMethod, stringMethod, "Different overloads must not share the same cached Method instance");
        assertArrayEquals(intParam, intMethod.getParameterTypes());
        assertArrayEquals(stringParam, stringMethod.getParameterTypes());
    }

    @Test
    void getMethodPrimitiveParameterMatchesExactPrimitive() {
        // GIVEN a method that takes a primitive int
        Class<?>[] intParam = new Class<?>[] { int.class };
        // WHEN we look it up with the primitive class literal
        Method method = ReflectionUtils.getMethod(Sample.class, "primitive", intParam);
        // THEN the method is found
        assertNotNull(method);
        assertEquals("primitive", method.getName());
        assertArrayEquals(intParam, method.getParameterTypes());
    }

    @Test
    void getMethodPrimitiveParameterDoesNotMatchWrapper() {
        // GIVEN the same method but we ask for Integer.class (wrapper)
        Class<?>[] wrapperParam = new Class<?>[] { Integer.class };
        // WHEN we look it up
        Method method = ReflectionUtils.getMethod(Sample.class, "primitive", wrapperParam);
        // THEN the lookup fails because getMethod requires an exact match
        assertNull(method, "Lookup with wrapper type should return null");
    }

    @Test
    void getMethodExplicitEmptyArrayFindsNoArgsMethod() {
        // GIVEN an explicit empty varargs array
        Class<?>[] empty = new Class<?>[] {};
        // WHEN we request the no‑arg method
        Method method = ReflectionUtils.getMethod(Sample.class, "noArgs", empty);
        // THEN we receive the correct Method object
        assertNotNull(method);
        assertEquals("noArgs", method.getName());
        assertEquals(0, method.getParameterCount());
    }

    @Test
    void getMethodNullVarargsReturnsNull() {
        // GIVEN a null varargs argument (the implementation will NPE in makeParamKey)
        Method method = ReflectionUtils.getMethod(Sample.class, "noArgs", (Class<?>[]) null);
        // THEN the utility swallows the exception and returns null
        assertNull(method, "Null varargs should result in null return");
    }

    @Test
    void getMethodCacheIsIsolatedBetweenDifferentClasses() {
        // GIVEN a method foo on Sample
        Method sampleMethod = ReflectionUtils.getMethod(Sample.class, "foo", new Class<?>[] { String.class });
        assertNotNull(sampleMethod, "Sample.foo should be found");
        // AND the same signature on a different class
        Method otherMethod = ReflectionUtils.getMethod(OtherSample.class, "foo", new Class<?>[] { String.class });
        assertNotNull(otherMethod, "OtherSample.foo should be found");
        // THEN the cached Method objects must be distinct (different declaring class)
        assertNotSame(sampleMethod, otherMethod, "Cache entries must be distinct for different declaring classes");
        assertEquals(Sample.class, sampleMethod.getDeclaringClass());
        assertEquals(OtherSample.class, otherMethod.getDeclaringClass());
    }
}
