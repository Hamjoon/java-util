package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentMap;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_2_Test {

    /**
     * Simple bean used for the reflective look‑ups.
     */
    static class SampleBean {

        public void noArgs() {
        }

        public String oneArg(String s) {
            return s;
        }

        // private method – not used in the positive tests but demonstrates that the utility can locate it
        @SuppressWarnings("unused")
        private void hidden() {
        }
    }

    /**
     * Simple bean with a few public methods and one private method.
     */
    static class TestClass {

        public void noArgs() {
        }

        public String oneArg(String s) {
            return s;
        }

        public void overloaded() {
        }

        public void overloaded(String a, int b) {
        }

        // private method – not used in positive tests but demonstrates that the utility can locate it
        @SuppressWarnings("unused")
        private void hidden() {
        }
    }

    /**
     * Superclass containing an inherited method.
     */
    static class SuperClass {

        public void inherited() {
        }
    }

    /**
     * Subclass that inherits {@code inherited()} from {@link SuperClass}.
     */
    static class SubClass extends SuperClass {
    }

    /* --------------------------------------------------------------------- */
    @AfterEach
    void clearCache() throws Exception {
        // ReflectionUtils.METHOD_MAP2 is package‑private; we access it via reflection.
        java.lang.reflect.Field mapField = ReflectionUtils.class.getDeclaredField("METHOD_MAP2");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, Method> map = (ConcurrentMap<String, Method>) mapField.get(null);
        map.clear();
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
    void getMethod_ValidZeroArgMethod_ReturnsMethod() throws NoSuchMethodException {
        SampleBean bean = new SampleBean();
        Method expected = SampleBean.class.getMethod("noArgs");
        Method actual = ReflectionUtils.getMethod(bean, "noArgs", 0);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void getMethod_ValidOneArgMethod_ReturnsMethod() throws NoSuchMethodException {
        SampleBean bean = new SampleBean();
        Method expected = SampleBean.class.getMethod("oneArg", String.class);
        Method actual = ReflectionUtils.getMethod(bean, "oneArg", 1);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void getMethod_CachesMethodInstance() {
        SampleBean bean = new SampleBean();
        // First call – method will be looked up and cached.
        Method first = ReflectionUtils.getMethod(bean, "noArgs", 0);
        assertNotNull(first);
        // Second call – should retrieve the same Method instance from the cache.
        Method second = ReflectionUtils.getMethod(bean, "noArgs", 0);
        assertSame(first, second, "Method instances should be identical due to caching");
    }

    @Test
    void getMethod_EmptyMethodName_ThrowsIllegalArgumentException() {
        TestClass bean = new TestClass();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(bean, "", 0));
        assertTrue(ex.getMessage().contains("is not found"));
    }

    @Test
    void getMethod_NoArgsMethod_ReturnsCorrectMethod() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method expected = TestClass.class.getMethod("noArgs");
        Method actual = ReflectionUtils.getMethod(bean, "noArgs", 0);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void getMethod_OneArgMethod_ReturnsCorrectMethod() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method expected = TestClass.class.getMethod("oneArg", String.class);
        Method actual = ReflectionUtils.getMethod(bean, "oneArg", 1);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] method \"{0}\" with argCount={1}")
    @CsvSource({ "overloaded, 0", "overloaded, 2" })
    void getMethod_OverloadedMethods(String methodName, int argCount) throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method expected;
        if (argCount == 0) {
            expected = TestClass.class.getMethod("overloaded");
        } else {
            expected = TestClass.class.getMethod("overloaded", String.class, int.class);
        }
        Method actual = ReflectionUtils.getMethod(bean, methodName, argCount);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void getMethod_InheritedMethod_ReturnsMethodFromSuperClass() throws NoSuchMethodException {
        SubClass bean = new SubClass();
        Method expected = SuperClass.class.getMethod("inherited");
        Method actual = ReflectionUtils.getMethod(bean, "inherited", 0);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void getMethod_ArgumentCountMismatch_ThrowsIllegalArgumentException() {
        TestClass bean = new TestClass();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(bean, "noArgs", 1));
        assertTrue(ex.getMessage().contains("is not found"));
    }

    @Test
    void getMethod_NegativeArgumentCount_ThrowsIllegalArgumentException() {
        TestClass bean = new TestClass();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(bean, "noArgs", -1));
        assertTrue(ex.getMessage().contains("is not found"));
    }
}
