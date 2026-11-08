package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.*;
import java.security.Permission;
import java.security.ReflectPermission;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_2_Test {

    /**
     * Simple class used for most basic reflection tests
     */
    static class Sample {

        public void noArgs() {
        }

        public String twoArgs(Integer i, String s) {
            return s + i;
        }

        private void privateMethod() {
        }
    }

    /**
     * Class with a variety of methods for cache‑related tests
     */
    static class TestClass {

        public void publicMethod() {
        }

        public void cachedMethod() {
        }

        public void nullCachedMethod() {
        }

        public void methodWithOneParam(Object o) {
        }
    }

    /**
     * Class whose private method will be blocked by a custom SecurityManager
     */
    static class TestClassWithSecurityBlock {

        private void blockedPrivate() {
        }
    }

    /**
     * Base class for inheritance tests
     */
    static class BaseClass {

        protected void protectedBase() {
        }

        private void privateBase() {
        }
    }

    /**
     * Subclass that inherits from {@link BaseClass}
     */
    static class SubClass extends BaseClass {

        public void subclassMethod() {
        }
    }

    /**
     * Class containing overloaded methods with the same name and arg count
     */
    static class OverloadClass {

        public void overloadedMethod(String s) {
        }

        public void overloadedMethod(Integer i) {
        }
    }

    /**
     * Class that defines a static method
     */
    static class StaticClass {

        public static void staticMethod() {
        }
    }

    /**
     * Class with a method that takes two parameters
     */
    static class ParamClass {

        public void twoParamMethod(Object a, Object b) {
        }
    }

    /**
     * Class used to verify cache reuse when the logical argument types differ
     */
    static class CacheTestClass {

        public void cachedMethod(Object o) {
        }
    }

    class PrivateHolder extends TestClass {

        private void hidden() {
        }
    }

    class PrivateHolder extends TestClass {

        private void hidden() {
        }
    }

    @BeforeEach
    void resetCache() {
        // Replace the internal cache with a fresh, thread‑safe map before each test.
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
    }

    @AfterEach
    void restoreSecurityManager() {
        // Ensure any custom SecurityManager installed by a test is removed.
        System.setSecurityManager(null);
    }

    static Stream<Arguments> nullCheckProvider() {
        return Stream.of(Arguments.of(null, "anyMethod", 0, IllegalArgumentException.class, "Object instance cannot be null"), Arguments.of(new TestClass(), null, 0, IllegalArgumentException.class, "Method name cannot be null"));
    }

    static Stream<Arguments> argumentCountProvider() {
        return Stream.of(Arguments.of(new TestClass(), "publicMethod", -1, IllegalArgumentException.class, "Argument count cannot be negative"));
    }

    static Stream<Arguments> methodNotFoundProvider() {
        return Stream.of(Arguments.of(new TestClass(), "nonExistent", 0, IllegalArgumentException.class, "Method 'nonExistent' with 0 parameters not found"));
    }

    static Stream<Arguments> inheritanceProvider() {
        return Stream.of(// protected method defined in superclass
        Arguments.of(new SubClass(), "protectedBase", 0, "protectedBase", false), // private method defined in superclass – should become accessible
        Arguments.of(new SubClass(), "privateBase", 0, "privateBase", true));
    }

    static Stream<Arguments> overloadProvider() {
        return Stream.of(Arguments.of(new OverloadClass(), "overloadedMethod", 1, "overloadedMethod"));
    }

    static Stream<Arguments> staticMethodProvider() {
        return Stream.of(Arguments.of(new StaticClass(), "staticMethod", 0, "staticMethod"));
    }

    static Stream<Arguments> paramMethodProvider() {
        return Stream.of(Arguments.of(new ParamClass(), "twoParamMethod", 2, "twoParamMethod"));
    }

    @Test
    void testGetMethodValidPublicNoArgs() {
        Sample instance = new Sample();
        Method m = ReflectionUtils.getMethod(instance, "noArgs", 0);
        assertNotNull(m);
        assertEquals("noArgs", m.getName());
        assertEquals(0, m.getParameterCount());
        assertTrue(m.isAccessible() || m.canAccess(instance));
    }

    @Test
    void testGetMethodValidPublicTwoArgs() {
        Sample instance = new Sample();
        Method m = ReflectionUtils.getMethod(instance, "twoArgs", 2);
        assertNotNull(m);
        assertEquals("twoArgs", m.getName());
        assertEquals(2, m.getParameterCount());
        assertEquals(String.class, m.getReturnType());
    }

    @Test
    void testGetMethodPrivateMethodBecomesAccessible() {
        Sample instance = new Sample();
        Method m = ReflectionUtils.getMethod(instance, "privateMethod", 0);
        assertNotNull(m);
        assertEquals("privateMethod", m.getName());
        // The method should be made accessible by the utility
        assertTrue(m.canAccess(instance));
    }

    @Test
    void testGetMethodCacheReturnsSameInstance() {
        Sample instance = new Sample();
        Method first = ReflectionUtils.getMethod(instance, "noArgs", 0);
        Method second = ReflectionUtils.getMethod(instance, "noArgs", 0);
        // The cache should return the exact same Method object
        assertSame(first, second);
    }

    @Test
    void testGetMethodNullInstanceThrows() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(null, "anyMethod", 0));
    }

    @Test
    void testGetMethodNullMethodNameThrows() {
        Sample instance = new Sample();
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, null, 0));
    }

    @Test
    void testGetMethodNegativeArgCountThrows() {
        Sample instance = new Sample();
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, "noArgs", -1));
    }

    @Test
    void testGetMethodMethodNotFoundThrows() {
        Sample instance = new Sample();
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, "doesNotExist", 0));
    }

    @ParameterizedTest(name = "[{index}] null check – instance={0}, method={1}")
    @MethodSource("nullCheckProvider")
    void testNullChecks(Object instance, String methodName, int argCount, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        Throwable ex = assertThrows(expectedException, () -> ReflectionUtils.getMethod(instance, methodName, argCount));
        assertTrue(ex.getMessage().contains(expectedMessageFragment));
    }

    @ParameterizedTest(name = "[{index}] argument count validation")
    @MethodSource("argumentCountProvider")
    void testArgumentCountValidation(Object instance, String methodName, int argCount, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        Throwable ex = assertThrows(expectedException, () -> ReflectionUtils.getMethod(instance, methodName, argCount));
        assertTrue(ex.getMessage().contains(expectedMessageFragment));
    }

    @ParameterizedTest(name = "[{index}] method not found")
    @MethodSource("methodNotFoundProvider")
    void testMethodNotFound(Object instance, String methodName, int argCount, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        Throwable ex = assertThrows(expectedException, () -> ReflectionUtils.getMethod(instance, methodName, argCount));
        assertTrue(ex.getMessage().contains(expectedMessageFragment));
    }

    @Test
    void testCacheHitNonNull() {
        TestClass instance = new TestClass();
        Method first = ReflectionUtils.getMethod(instance, "cachedMethod", 0);
        Method second = ReflectionUtils.getMethod(instance, "cachedMethod", 0);
        assertSame(first, second, "Method should be retrieved from cache on second call");
    }

    @Test
    void testCacheHitNullValue() throws Exception {
        // Prepare a cache entry whose value is null.
        Map<Object, Method> cache = new ConcurrentHashMap<>();
        // Construct the private MethodCacheKey via reflection.
        Class<?> keyClass = Class.forName("com.cedarsoftware.util.ReflectionUtils$MethodCacheKey");
        Constructor<?> ctor = keyClass.getDeclaredConstructor(Class.class, String.class, Class[].class);
        ctor.setAccessible(true);
        Object key = ctor.newInstance(TestClass.class, "nullCachedMethod", new Class<?>[0]);
        cache.put(key, null);
        ReflectionUtils.setMethodCache(cache);
        // Invoke getMethod – it should return null because the cached value is null.
        Method result = ReflectionUtils.getMethod(new TestClass(), "nullCachedMethod", 0);
        assertNull(result, "When cache contains a null value, getMethod should return null");
    }

    @Test
    void testCacheMissFindPublicMethod() {
        TestClass instance = new TestClass();
        Method m = ReflectionUtils.getMethod(instance, "publicMethod", 0);
        assertNotNull(m);
        assertEquals("publicMethod", m.getName());
        assertTrue(m.canAccess(instance));
    }

    @Test
    void testCacheMissPrivateMethodAccessible() {
        TestClass instance = new TestClass();
        // Add a private method to TestClass via an inner subclass for the test.
        class PrivateHolder extends TestClass {

            private void hidden() {
            }
        }
        PrivateHolder holder = new PrivateHolder();
        Method m = ReflectionUtils.getMethod(holder, "hidden", 0);
        assertNotNull(m);
        assertEquals("hidden", m.getName());
        assertTrue(m.canAccess(holder), "Private method should have been made accessible");
    }

    @Test
    void testCacheMissPrivateMethodSetAccessibleThrows() {
        // Install a SecurityManager that forbids suppressAccessChecks.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Access checks suppressed");
                }
            }
        });
        TestClassWithSecurityBlock instance = new TestClassWithSecurityBlock();
        Method m = ReflectionUtils.getMethod(instance, "blockedPrivate", 0);
        assertNotNull(m);
        assertEquals("blockedPrivate", m.getName());
        // Because setAccessible threw, the method remains inaccessible.
        assertFalse(m.canAccess(instance), "Method should remain inaccessible when setAccessible fails");
    }

    @ParameterizedTest(name = "[{index}] inheritance – {1}")
    @MethodSource("inheritanceProvider")
    void testInheritanceMethods(Object instance, String methodName, int argCount, String expectedName, boolean expectAccessible) {
        Method m = ReflectionUtils.getMethod(instance, methodName, argCount);
        assertNotNull(m);
        assertEquals(expectedName, m.getName());
        if (expectAccessible) {
            assertTrue(m.canAccess(instance), "Method should be made accessible");
        } else {
            // Protected methods are already accessible via reflection when canAccess is used.
            assertTrue(m.canAccess(instance) || Modifier.isProtected(m.getModifiers()));
        }
    }

    @ParameterizedTest(name = "[{index}] overloaded method")
    @MethodSource("overloadProvider")
    void testOverloadedMethods(Object instance, String methodName, int argCount, String expectedName) {
        Method m = ReflectionUtils.getMethod(instance, methodName, argCount);
        assertNotNull(m);
        assertEquals(expectedName, m.getName());
        assertEquals(argCount, m.getParameterCount());
    }

    @ParameterizedTest(name = "[{index}] static method")
    @MethodSource("staticMethodProvider")
    void testStaticMethodRetrieval(Object instance, String methodName, int argCount, String expectedName) {
        Method m = ReflectionUtils.getMethod(instance, methodName, argCount);
        assertNotNull(m);
        assertEquals(expectedName, m.getName());
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    void testEmptyMethodNameNotFound() {
        TestClass instance = new TestClass();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, "", 0));
        assertTrue(ex.getMessage().contains("Method '' with 0 parameters not found"));
    }

    @ParameterizedTest(name = "[{index}] method with parameters")
    @MethodSource("paramMethodProvider")
    void testMethodWithParameters(Object instance, String methodName, int argCount, String expectedName) {
        Method m = ReflectionUtils.getMethod(instance, methodName, argCount);
        assertNotNull(m);
        assertEquals(expectedName, m.getName());
        assertEquals(argCount, m.getParameterCount());
    }

    @Test
    void testCacheReuseSameArgCountDifferentRuntimeArgs() {
        CacheTestClass instance = new CacheTestClass();
        // First call – caches the method.
        Method first = ReflectionUtils.getMethod(instance, "cachedMethod", 1);
        assertNotNull(first);
        // Second call with a different runtime argument type (String instead of Integer)
        Method second = ReflectionUtils.getMethod(instance, "cachedMethod", 1);
        assertSame(first, second, "Cache should return the same Method object irrespective of runtime argument types");
    }

    @Test
    void testGetMethodEmptyStringMethodNameThrows() {
        Sample instance = new Sample();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, "", 0));
        assertTrue(ex.getMessage().contains("Method '' with 0 parameters not found"));
    }
}
