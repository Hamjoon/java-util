package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_Test {

    /**
     * Simple class used for reflection tests.
     */
    static class Sample {

        public void noArgs() {
        }

        public String withArgs(String s, int i) {
            return s + i;
        }
    }

    // Classes for "Method Retrieval" group
    static class ClassWithMethod {

        public void publicMethod() {
        }
    }

    static class BaseClass {

        private void privateBaseMethod() {
        }
    }

    static class SubClass extends BaseClass {
    }

    static class ClassWithNoMethod {
    }

    // Overloaded methods
    static class OverloadedClass {

        public void process(int i) {
        }

        public void process(String s) {
        }
    }

    // Visibility variants
    static class VisibilityClass {

        protected void protectedMethod() {
        }

        void packagePrivateMethod() {
        }

        static void staticMethod() {
        }
    }

    // Interface with default method
    interface InterfaceWithDefault {

        default void defaultOperation() {
        }
    }

    static class InterfaceWithDefaultImpl implements InterfaceWithDefault {
    }

    // Primitive vs Wrapper mismatch
    static class PrimitiveWrapperClass {

        public void acceptInt(int i) {
        }

        public void acceptInteger(Integer i) {
        }
    }

    // Cache distinctness across classes
    static class ClassA {

        public void sharedMethod() {
        }
    }

    static class ClassB {

        public void sharedMethod() {
        }
    }

    // Varargs method
    static class VarargsClass {

        public void concat(String... parts) {
        }
    }

    // Simple class for invalid types array elements
    static class SimpleClass {

        public void someMethod(String s) {
        }
    }

    @BeforeEach
    void clearCache() {
        // Replace the internal cache with a fresh, empty ConcurrentHashMap.
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
    }

    @Test
    void getMethod_shouldReturnMethodWhenItExists() {
        Method m = ReflectionUtils.getMethod(Sample.class, "noArgs");
        assertNotNull(m, "Method should be found");
        assertEquals("noArgs", m.getName());
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    void getMethod_shouldReturnMethodWithParameters() {
        Method m = ReflectionUtils.getMethod(Sample.class, "withArgs", String.class, int.class);
        assertNotNull(m, "Method with parameters should be found");
        assertEquals("withArgs", m.getName());
        assertEquals(String.class, m.getReturnType());
        Class<?>[] paramTypes = m.getParameterTypes();
        assertArrayEquals(new Class<?>[] { String.class, int.class }, paramTypes);
    }

    @Test
    void getMethod_shouldReturnNullWhenMethodDoesNotExist() {
        Method m = ReflectionUtils.getMethod(Sample.class, "doesNotExist");
        assertNull(m, "Non‑existent method should return null");
    }

    @Test
    void getMethod_shouldCacheResult() {
        // First call – method is looked up and cached.
        Method first = ReflectionUtils.getMethod(Sample.class, "noArgs");
        // Second call – should retrieve the same Method instance from the cache.
        Method second = ReflectionUtils.getMethod(Sample.class, "noArgs");
        assertSame(first, second, "Subsequent calls should return the cached Method instance");
    }

    @Test
    void getMethod_nullClass_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(null, "anyMethod"));
        assertTrue(ex.getMessage().contains("class cannot be null"));
    }

    @Test
    void getMethod_nullMethodName_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(Sample.class, null));
        assertTrue(ex.getMessage().contains("methodName cannot be null"));
    }

    @Test
    void getMethod_nullTypesArray_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethod(Sample.class, "noArgs", (Class<?>[]) null));
    }

    @Test
    void getMethod_directMethodFound() {
        Method m = ReflectionUtils.getMethod(ClassWithMethod.class, "publicMethod");
        assertNotNull(m);
        assertEquals(ClassWithMethod.class, m.getDeclaringClass());
    }

    @Test
    void getMethod_inheritedPrivateMethodFound() {
        Method m = ReflectionUtils.getMethod(SubClass.class, "privateBaseMethod");
        assertNotNull(m);
        assertEquals(BaseClass.class, m.getDeclaringClass());
    }

    @Test
    void getMethod_methodDoesNotExist_returnsNull() {
        Method m = ReflectionUtils.getMethod(ClassWithNoMethod.class, "nonExisting");
        assertNull(m);
    }

    @Test
    void getMethod_nameMatchesButParameterTypesDoNot_returnsNull() {
        Method m = ReflectionUtils.getMethod(ClassWithMethod.class, "publicMethod", String.class);
        assertNull(m);
    }

    @Test
    void getMethod_cacheHit_returnsSameInstance() {
        Method first = ReflectionUtils.getMethod(ClassWithMethod.class, "publicMethod");
        Method second = ReflectionUtils.getMethod(ClassWithMethod.class, "publicMethod");
        assertSame(first, second, "Cache should return the same Method instance");
    }

    @Test
    void getMethod_overload_intVersion() {
        Method m = ReflectionUtils.getMethod(OverloadedClass.class, "process", int.class);
        assertNotNull(m);
        assertEquals(int.class, m.getParameterTypes()[0]);
    }

    @Test
    void getMethod_overload_stringVersion() {
        Method m = ReflectionUtils.getMethod(OverloadedClass.class, "process", String.class);
        assertNotNull(m);
        assertEquals(String.class, m.getParameterTypes()[0]);
    }

    @Test
    void getMethod_protectedMethodFound() {
        Method m = ReflectionUtils.getMethod(VisibilityClass.class, "protectedMethod");
        assertNotNull(m);
        assertTrue(java.lang.reflect.Modifier.isProtected(m.getModifiers()));
    }

    @Test
    void getMethod_packagePrivateMethodFound() {
        Method m = ReflectionUtils.getMethod(VisibilityClass.class, "packagePrivateMethod");
        assertNotNull(m);
        assertFalse(java.lang.reflect.Modifier.isPublic(m.getModifiers()));
        assertFalse(java.lang.reflect.Modifier.isProtected(m.getModifiers()));
        assertFalse(java.lang.reflect.Modifier.isPrivate(m.getModifiers()));
    }

    @Test
    void getMethod_staticMethodFound() {
        Method m = ReflectionUtils.getMethod(VisibilityClass.class, "staticMethod");
        assertNotNull(m);
        assertTrue(java.lang.reflect.Modifier.isStatic(m.getModifiers()));
    }

    @Test
    void getMethod_interfaceDefaultMethodFound() {
        Method m = ReflectionUtils.getMethod(InterfaceWithDefaultImpl.class, "defaultOperation");
        assertNotNull(m);
        // Declaring class is the interface, not the impl
        assertEquals(InterfaceWithDefault.class, m.getDeclaringClass());
        assertTrue(m.isDefault());
    }

    @Test
    void getMethod_primitiveExpectedButWrapperSupplied_returnsNull() {
        Method m = ReflectionUtils.getMethod(PrimitiveWrapperClass.class, "acceptInt", Integer.class);
        assertNull(m);
    }

    @Test
    void getMethod_wrapperExpectedButPrimitiveSupplied_returnsNull() {
        Method m = ReflectionUtils.getMethod(PrimitiveWrapperClass.class, "acceptInteger", int.class);
        assertNull(m);
    }

    @Test
    void getMethod_sameSignatureDifferentClasses_returnDistinctMethods() {
        Method a = ReflectionUtils.getMethod(ClassA.class, "sharedMethod");
        Method b = ReflectionUtils.getMethod(ClassB.class, "sharedMethod");
        assertNotNull(a);
        assertNotNull(b);
        assertNotSame(a, b, "Cache keys include the declaring class, so instances must differ");
        assertEquals(ClassA.class, a.getDeclaringClass());
        assertEquals(ClassB.class, b.getDeclaringClass());
    }

    @Test
    void getMethod_varargsMethodFound() {
        Method m = ReflectionUtils.getMethod(VarargsClass.class, "concat", String[].class);
        assertNotNull(m);
        assertEquals(String[].class, m.getParameterTypes()[0]);
        assertTrue(m.isVarArgs());
    }

    @Test
    void getMethod_typesArrayContainsNullElement_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethod(SimpleClass.class, "someMethod", (Class<?>[]) new Class<?>[] { null }));
    }

    @Test
    void getMethod_typesArrayMixedNullAndValid_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethod(SimpleClass.class, "someMethod", String.class, null, int.class));
    }
}
