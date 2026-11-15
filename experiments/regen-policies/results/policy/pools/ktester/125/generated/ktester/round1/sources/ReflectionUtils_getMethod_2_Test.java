package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_2_Test {

    // ----------------------------------------------------------------------
    private Map<?, ?> originalCache;

    // ----------------------------------------------------------------------
    static class Sample {

        public void noArgs() {
        }

        private String privateMethod(String s) {
            return s;
        }

        public String overloaded(int i) {
            return "int";
        }

        public String overloaded(String s) {
            return "String";
        }
    }

    static class SuperClass {

        protected void inheritedMethod() {
        }
    }

    static class SubClass extends SuperClass {
    }

    static class OverloadedClass {

        public void overloaded(int i) {
        }

        public void overloaded(String s) {
        }
    }

    static class PrimitiveClass {

        public void primitiveMethod(int i, double d) {
        }
    }

    static class VarargsClass {

        public void varargsMethod(String... args) {
        }
    }

    /**
     * Save the original cache before each test and replace it with a fresh map.
     */
    @BeforeEach
    void replaceCache() throws Exception {
        // Use reflection to read the private static field METHOD_CACHE
        var field = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
        field.setAccessible(true);
        originalCache = (Map<?, ?>) field.get(null);
        // Replace with a simple HashMap (no LRU behaviour needed for tests)
        ReflectionUtils.setMethodCache(new HashMap<>());
    }

    /**
     * Restore the original cache after each test.
     */
    @AfterEach
    void restoreCache() throws Exception {
        ReflectionUtils.setMethodCache(originalCache);
    }

    @Test
    void getMethod_findsPublicMethodWithCorrectArgCount() {
        Sample instance = new Sample();
        Method m = ReflectionUtils.getMethod(instance, "noArgs", 0);
        assertNotNull(m);
        assertEquals("noArgs", m.getName());
        assertEquals(0, m.getParameterCount());
        assertTrue(m.canAccess(instance) || m.isAccessible(), "Public method should be accessible");
    }

    @Test
    void getMethod_makesPrivateMethodAccessible() {
        Sample instance = new Sample();
        Method m = ReflectionUtils.getMethod(instance, "privateMethod", 1);
        assertNotNull(m);
        assertEquals("privateMethod", m.getName());
        assertEquals(1, m.getParameterCount());
        // The method should be accessible after the call (setAccessible(true) was attempted)
        assertTrue(m.canAccess(instance) || m.isAccessible(), "Method should be accessible after ReflectionUtils processing");
    }

    @Test
    void getMethod_selectsCorrectOverloadedMethodByParameterCount() {
        Sample instance = new Sample();
        // argCount = 1, both overloaded methods have one parameter but different types.
        // The implementation fills the type array with Object.class, so both match.
        // The private selectMethod() decides which to return; we only assert that a method is returned.
        Method m = ReflectionUtils.getMethod(instance, "overloaded", 1);
        assertNotNull(m);
        assertEquals("overloaded", m.getName());
        assertEquals(1, m.getParameterCount());
    }

    @Test
    void getMethod_throwsWhenInstanceIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(null, "anyMethod", 0));
        assertTrue(ex.getMessage().contains("Object instance cannot be null"));
    }

    @Test
    void getMethod_throwsWhenMethodNameIsNull() {
        Sample instance = new Sample();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, null, 0));
        assertTrue(ex.getMessage().contains("Method name cannot be null"));
    }

    @Test
    void getMethod_throwsWhenArgCountIsNegative() {
        Sample instance = new Sample();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, "noArgs", -1));
        assertTrue(ex.getMessage().contains("Argument count cannot be negative"));
    }

    @Test
    void getMethod_throwsWhenMethodNotFound() {
        Sample instance = new Sample();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(instance, "doesNotExist", 0));
        assertTrue(ex.getMessage().contains("Method 'doesNotExist' with 0 parameters not found"));
    }

    @Test
    void getMethod_cachesResult() {
        Sample instance = new Sample();
        // First call – method is not cached yet.
        Method first = ReflectionUtils.getMethod(instance, "noArgs", 0);
        assertNotNull(first);
        // Second call – should retrieve the same Method instance from the cache.
        Method second = ReflectionUtils.getMethod(instance, "noArgs", 0);
        assertNotNull(second);
        // The cached method instance should be the same object.
        assertSame(first, second, "Method should be cached and identical on subsequent calls");
    }

    @Test
    void getMethod_inheritedMethodRetrieval() {
        SubClass instance = new SubClass();
        Method m = ReflectionUtils.getMethod(instance, "inheritedMethod", 0);
        assertNotNull(m);
        assertEquals("inheritedMethod", m.getName());
        assertEquals(0, m.getParameterCount());
        // The method is declared in the superclass
        assertEquals(SuperClass.class, m.getDeclaringClass());
        assertTrue(m.canAccess(instance) || m.isAccessible(), "Inherited method should be accessible after processing");
    }

    @Test
    void getMethod_overloadedMethodSelectionSameArgCount() {
        OverloadedClass instance = new OverloadedClass();
        Method m = ReflectionUtils.getMethod(instance, "overloaded", 1);
        assertNotNull(m);
        assertEquals("overloaded", m.getName());
        assertEquals(1, m.getParameterCount());
        assertEquals(OverloadedClass.class, m.getDeclaringClass(), "Method must belong to OverloadedClass");
    }

    @Test
    void getMethod_primitiveParameterMethod() {
        PrimitiveClass instance = new PrimitiveClass();
        Method m = ReflectionUtils.getMethod(instance, "primitiveMethod", 2);
        assertNotNull(m);
        assertEquals("primitiveMethod", m.getName());
        assertEquals(2, m.getParameterCount());
        Class<?>[] params = m.getParameterTypes();
        assertArrayEquals(new Class<?>[] { int.class, double.class }, params, "Parameter types must match the primitive signature");
    }

    @Test
    void getMethod_varargsMethodHandling() {
        VarargsClass instance = new VarargsClass();
        Method m = ReflectionUtils.getMethod(instance, "varargsMethod", 1);
        assertNotNull(m);
        assertEquals("varargsMethod", m.getName());
        assertEquals(1, m.getParameterCount());
        // Var‑args are compiled as an array parameter
        assertTrue(m.getParameterTypes()[0].isArray(), "Var‑args parameter should be represented as an array");
    }

    @Test
    void getMethod_publicMethodAlreadyAccessible() {
        Sample instance = new Sample();
        Method m = ReflectionUtils.getMethod(instance, "noArgs", 0);
        assertNotNull(m);
        assertTrue(m.canAccess(instance) || m.isAccessible(), "Public method should remain accessible without modification");
    }

    @Test
    void getMethod_cacheHitWithNullEntry() throws Exception {
        // Build the same key that getMethod would use
        Class<?> beanClass = Sample.class;
        String methodName = "nonExistingMethod";
        // argCount = 0
        Class<?>[] types = new Class<?>[0];
        // The key class is a private static inner class – use reflection.
        Class<?> keyClass = Class.forName("com.cedarsoftware.util.ReflectionUtils$MethodCacheKey");
        var ctor = keyClass.getDeclaredConstructor(Class.class, String.class, Class[].class);
        ctor.setAccessible(true);
        Object key = ctor.newInstance(beanClass, methodName, types);
        // Insert a null entry into the custom cache
        @SuppressWarnings("unchecked")
        Map<Object, Method> customCache = (Map<Object, Method>) ReflectionUtils.getClass().getDeclaredMethod("setMethodCache", Map.class).invoke(null, new HashMap<>());
        customCache.put(key, null);
        ReflectionUtils.setMethodCache(customCache);
        // The call should return null (cached miss) rather than throwing.
        Method result = ReflectionUtils.getMethod(new Sample(), methodName, 0);
        assertNull(result, "A cached null entry must be returned as‑is");
    }

    @Test
    void getMethod_throwsWhenCacheIsNull() {
        // Replace the internal cache with null
        ReflectionUtils.setMethodCache(null);
        Sample instance = new Sample();
        NullPointerException npe = assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethod(instance, "noArgs", 0));
        // The exact message depends on the JDK, we only assert the type.
        assertNotNull(npe);
    }
}
