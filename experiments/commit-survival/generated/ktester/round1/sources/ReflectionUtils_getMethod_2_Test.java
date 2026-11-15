package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.*;
import java.security.Permission;
import java.security.ReflectPermission;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_2_Test {

    /**
     * Holds the original cache so we can restore it after the race‑condition test.
     */
    private static ConcurrentMap<String, Method> originalCache;

    /**
     * Simple bean used for reflection tests.
     */
    static class SampleBean {

        public void noArgs() {
        }

        public void oneArg(String s) {
        }

        public void oneArg(int i) {
        }

        private void privateMethod() {
        }
    }

    /**
     * Simple bean with a few public methods (including overloads).
     */
    static class TestClass {

        public void zeroArgs() {
        }

        public void oneArg(String s) {
        }

        public void oneArg(int i) {
        }

        public static void staticMethod() {
        }

        @Override
        public String toString() {
            return "TestClass";
        }
    }

    /**
     * Bean that extends a parent class – used to test inherited method lookup.
     */
    static class ParentClass {

        public void parentMethod() {
        }
    }

    static class SubClass extends ParentClass {
    }

    /**
     * Bean with overloaded methods named {@code process}.
     */
    static class OverloadedClass {

        public void process(int i) {
        }

        public void process(String s, int i) {
        }
    }

    /**
     * Bean with a private method – should be discoverable via reflection.
     */
    static class PrivateMethodClass {

        private void privateMethod() {
        }
    }

    /**
     * Bean with a protected method – should be discoverable via reflection.
     */
    static class ProtectedMethodClass {

        protected void protectedMethod() {
        }
    }

    /**
     * Bean with a var‑args method.
     */
    static class VarArgsClass {

        public void varMethod(String... args) {
        }
    }

    /**
     * Bean with primitive and wrapper parameter types.
     */
    static class PrimitiveWrapperClass {

        public void acceptInt(int i) {
        }

        public void acceptInteger(Integer i) {
        }
    }

    class StubMap implements ConcurrentMap<String, Method> {

        @Override
        public Method get(Object key) {
            return null;
        }

        @Override
        public Method putIfAbsent(String key, Method value) {
            return dummyMethod;
        }

        // The remaining methods are not used in this test.
        @Override
        public Method put(String key, Method value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method replace(String key, Method value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(String key, Method oldValue, Method newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method getOrDefault(Object key, Method defaultValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEach(BiConsumer<? super String, ? super Method> action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(BiFunction<? super String, ? super Method, ? extends Method> function) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method computeIfAbsent(String key, java.util.function.Function<? super String, ? extends Method> mappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method computeIfPresent(String key, java.util.function.BiFunction<? super String, ? super Method, ? extends Method> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method compute(String key, java.util.function.BiFunction<? super String, ? super Method, ? extends Method> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method merge(String key, Method value, java.util.function.BiFunction<? super Method, ? super Method, ? extends Method> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Set<String> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Collection<Method> values() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Set<Entry<String, Method>> entrySet() {
            throw new UnsupportedOperationException();
        }
    }

    class StubMap implements ConcurrentMap<String, Method> {

        @Override
        public Method get(Object key) {
            return null;
        }

        @Override
        public Method putIfAbsent(String key, Method value) {
            return dummyMethod;
        }

        // The remaining methods are not used in this test.
        @Override
        public Method put(String key, Method value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method replace(String key, Method value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(String key, Method oldValue, Method newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method getOrDefault(Object key, Method defaultValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEach(BiConsumer<? super String, ? super Method> action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(BiFunction<? super String, ? super Method, ? extends Method> function) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method computeIfAbsent(String key, java.util.function.Function<? super String, ? extends Method> mappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method computeIfPresent(String key, java.util.function.BiFunction<? super String, ? super Method, ? extends Method> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method compute(String key, java.util.function.BiFunction<? super String, ? super Method, ? extends Method> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Method merge(String key, Method value, java.util.function.BiFunction<? super Method, ? super Method, ? extends Method> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Set<String> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Collection<Method> values() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Set<Entry<String, Method>> entrySet() {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        // Keep a reference to the original cache for later restoration.
        Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_MAP2");
        cacheField.setAccessible(true);
        //noinspection unchecked
        originalCache = (ConcurrentMap<String, Method>) cacheField.get(null);
    }

    @BeforeEach
    void clearCache() throws Exception {
        // Replace the cache with a fresh ConcurrentHashMap before each test to avoid cross‑test contamination.
        Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_MAP2");
        cacheField.setAccessible(true);
        cacheField.set(null, new java.util.concurrent.ConcurrentHashMap<>());
    }

    @AfterAll
    static void afterAll() throws Exception {
        // Restore the original cache (important for other test classes that might run later).
        Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_MAP2");
        cacheField.setAccessible(true);
        cacheField.set(null, originalCache);
    }

    /**
     * Supplies all test cases defined in the JSON description.
     */
    private static Stream<TestCase> provideTestCases() throws NoSuchMethodException {
        return Stream.of(// Null checks
        new TestCase(null, "anyMethod", 0, null, null, IllegalArgumentException.class, "null instance"), new TestCase(new TestClass(), null, 0, null, null, IllegalArgumentException.class, "null method name"), // Method not found
        new TestCase(new TestClass(), "nonExistingMethod", 0, null, null, IllegalArgumentException.class, "is not found on class"), // First insertion (cache miss)
        new TestCase(new TestClass(), "zeroArgs", 0, TestClass.class, "zeroArgs()", null, null), // Cache hit – same instance
        new TestCase(new TestClass(), "zeroArgs", 0, TestClass.class, "zeroArgs()", null, null), // Overloaded methods – argCount = 1 (int version)
        new TestCase(new OverloadedClass(), "process", 1, OverloadedClass.class, "process(int)", null, null), // Overloaded methods – argCount = 2 (String, int version)
        new TestCase(new OverloadedClass(), "process", 2, OverloadedClass.class, "process(java.lang.String,int)", null, null), // Inherited method
        new TestCase(new SubClass(), "parentMethod", 0, ParentClass.class, "parentMethod()", null, null), // Static method
        new TestCase(new TestClass(), "staticMethod", 0, TestClass.class, "staticMethod()", null, null), // Private method
        new TestCase(new PrivateMethodClass(), "privateMethod", 0, PrivateMethodClass.class, "privateMethod()", null, null), // Protected method
        new TestCase(new ProtectedMethodClass(), "protectedMethod", 0, ProtectedMethodClass.class, "protectedMethod()", null, null), // Cache reuse across different bean instances
        new TestCase(new TestClass(), "zeroArgs", 0, TestClass.class, "zeroArgs()", null, null), // Argument count mismatch
        new TestCase(new TestClass(), "zeroArgs", 1, null, null, IllegalArgumentException.class, "is not found on class"), // Object class method lookup
        new TestCase(new TestClass(), "toString", 0, Object.class, "toString()", null, null), // Var‑args method handling
        new TestCase(new VarArgsClass(), "varMethod", 1, VarArgsClass.class, "varMethod(java.lang.String[])", null, null), // Primitive vs wrapper types
        new TestCase(new PrimitiveWrapperClass(), "acceptInt", 1, PrimitiveWrapperClass.class, "acceptInt(int)", null, null), new TestCase(new PrimitiveWrapperClass(), "acceptInteger", 1, PrimitiveWrapperClass.class, "acceptInteger(java.lang.Integer)", null, null), // Negative argument count
        new TestCase(new TestClass(), "zeroArgs", -1, null, null, IllegalArgumentException.class, "is not found on class"), // Empty method name
        new TestCase(new TestClass(), "", 0, null, null, IllegalArgumentException.class, "is not found on class"));
    }

    /**
     * Helper to build a readable method signature used in test expectations.
     */
    private static String buildSignature(Method m) {
        StringBuilder sb = new StringBuilder(m.getName());
        sb.append('(');
        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(params[i].getTypeName());
        }
        sb.append(')');
        return sb.toString();
    }

    @Test
    void getMethod_NullBean_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(null, "anyMethod", 0));
        assertTrue(ex.getMessage().contains("null instance"));
    }

    @Test
    void getMethod_NullMethodName_ThrowsIllegalArgumentException() {
        SampleBean bean = new SampleBean();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(bean, null, 0));
        assertTrue(ex.getMessage().contains("null method name"));
    }

    @Test
    void getMethod_MethodNotFound_ThrowsIllegalArgumentException() {
        SampleBean bean = new SampleBean();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(bean, "doesNotExist", 0));
        assertTrue(ex.getMessage().contains("is not found on class"));
    }

    @Test
    void getMethod_ExactMatch_ReturnsMethod() throws NoSuchMethodException {
        SampleBean bean = new SampleBean();
        // no‑arg method
        Method m1 = ReflectionUtils.getMethod(bean, "noArgs", 0);
        assertNotNull(m1);
        assertEquals(SampleBean.class.getMethod("noArgs"), m1);
        // one‑arg method with String parameter
        Method m2 = ReflectionUtils.getMethod(bean, "oneArg", 1);
        assertNotNull(m2);
        // There are two overloaded oneArg methods; the one with String should be returned
        // because the lookup is based on argument count only, the first matching method
        // found by ReflectionUtils is expected to be the String version (as declared first).
        Method expected = SampleBean.class.getMethod("oneArg", String.class);
        assertEquals(expected, m2);
    }

    @Test
    void getMethod_CachesResult_ReturnsSameInstanceOnSubsequentCalls() {
        SampleBean bean = new SampleBean();
        Method firstCall = ReflectionUtils.getMethod(bean, "noArgs", 0);
        Method secondCall = ReflectionUtils.getMethod(bean, "noArgs", 0);
        // The cache should cause both calls to return the identical Method object.
        assertSame(firstCall, secondCall);
    }

    @Test
    void getMethod_PrivateMethod_ThrowsIllegalArgumentException() {
        SampleBean bean = new SampleBean();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(bean, "privateMethod", 0));
        assertTrue(ex.getMessage().contains("is not found on class"));
    }

    @ParameterizedTest(name = "[{index}] bean={0}, method={1}, argCount={2}")
    @MethodSource("provideTestCases")
    void getMethod_variousScenarios(TestCase tc) throws Exception {
        if (tc.expectedException() != null) {
            // Expect an exception
            Throwable ex = assertThrows(tc.expectedException(), () -> ReflectionUtils.getMethod(tc.bean, tc.methodName, tc.argCount));
            if (tc.expectedMessageFragment() != null) {
                assertTrue(ex.getMessage().contains(tc.expectedMessageFragment()), () -> "Exception message [" + ex.getMessage() + "] should contain [" + tc.expectedMessageFragment() + "]");
            }
            return;
        }
        // No exception expected – obtain the method
        Method result = ReflectionUtils.getMethod(tc.bean, tc.methodName, tc.argCount);
        assertNotNull(result, "Returned Method should not be null");
        // Verify the declaring class matches expectation
        assertEquals(tc.expectedDeclaringClass(), result.getDeclaringClass(), "Declaring class mismatch");
        // Verify the signature (method name + parameter types) matches expectation
        String signature = buildSignature(result);
        assertEquals(tc.expectedMethodSignature(), signature, "Method signature mismatch");
    }

    @Test
    void getMethod_cachesResult_ReturnsSameInstanceOnSubsequentCalls() {
        TestClass bean = new TestClass();
        Method first = ReflectionUtils.getMethod(bean, "zeroArgs", 0);
        Method second = ReflectionUtils.getMethod(bean, "zeroArgs", 0);
        assertSame(first, second, "Method instances should be identical due to caching");
    }

    @Test
    void getMethod_cacheReusedAcrossDifferentInstances() {
        TestClass bean1 = new TestClass();
        TestClass bean2 = new TestClass();
        Method fromFirst = ReflectionUtils.getMethod(bean1, "zeroArgs", 0);
        Method fromSecond = ReflectionUtils.getMethod(bean2, "zeroArgs", 0);
        assertSame(fromFirst, fromSecond, "Cache should be shared across bean instances of the same class");
    }

    @Test
    void getMethod_concurrentInsertion_putIfAbsentReturnsOther() throws Exception {
        // Prepare a dummy method that will act as the "other" value already present in the map.
        Method dummyMethod = TestClass.class.getMethod("oneArg", String.class);
        // Custom ConcurrentMap that returns null on get (forcing a cache miss) but
        // returns dummyMethod on putIfAbsent (simulating another thread inserting first).
        class StubMap implements ConcurrentMap<String, Method> {

            @Override
            public Method get(Object key) {
                return null;
            }

            @Override
            public Method putIfAbsent(String key, Method value) {
                return dummyMethod;
            }

            // The remaining methods are not used in this test.
            @Override
            public Method put(String key, Method value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Method remove(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object key, Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Method replace(String key, Method value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean replace(String key, Method oldValue, Method newValue) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int size() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isEmpty() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsKey(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsValue(Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Method getOrDefault(Object key, Method defaultValue) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forEach(BiConsumer<? super String, ? super Method> action) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void replaceAll(BiFunction<? super String, ? super Method, ? extends Method> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Method computeIfAbsent(String key, java.util.function.Function<? super String, ? extends Method> mappingFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Method computeIfPresent(String key, java.util.function.BiFunction<? super String, ? super Method, ? extends Method> remappingFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Method compute(String key, java.util.function.BiFunction<? super String, ? super Method, ? extends Method> remappingFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Method merge(String key, Method value, java.util.function.BiFunction<? super Method, ? super Method, ? extends Method> remappingFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.util.Set<String> keySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.util.Collection<Method> values() {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.util.Set<Entry<String, Method>> entrySet() {
                throw new UnsupportedOperationException();
            }
        }
        // Replace the internal cache with the stub.
        Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_MAP2");
        cacheField.setAccessible(true);
        ConcurrentMap<String, Method> original = (ConcurrentMap<String, Method>) cacheField.get(null);
        cacheField.set(null, new StubMap());
        try {
            TestClass bean = new TestClass();
            // The call should return the dummyMethod supplied by putIfAbsent.
            Method result = ReflectionUtils.getMethod(bean, "oneArg", 1);
            assertSame(dummyMethod, result, "Method returned should be the one already present in the map");
        } finally {
            // Restore the original cache to avoid side effects on other tests.
            cacheField.set(null, original);
        }
    }

    @Test
    void getMethod_securityManagerDeniesPrivateAccess_throwsSecurityException() throws Exception {
        // Install a restrictive SecurityManager that denies suppressAccessChecks.
        SecurityManager originalSm = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except reflective access suppression.
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
            }
        });
        try {
            PrivateMethodClass bean = new PrivateMethodClass();
            // Expect a SecurityException when ReflectionUtils attempts to make the private method accessible.
            SecurityException se = assertThrows(SecurityException.class, () -> ReflectionUtils.getMethod(bean, "privateMethod", 0));
            assertTrue(se.getMessage().contains("Reflective access denied"));
        } finally {
            // Restore the original SecurityManager so other tests (or the test runner) are not affected.
            System.setSecurityManager(originalSm);
        }
    }
}
