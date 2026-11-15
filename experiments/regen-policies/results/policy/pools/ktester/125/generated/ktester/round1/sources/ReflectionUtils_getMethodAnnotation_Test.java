package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethodAnnotation_Test {

    static class SuperClass {

        @TestAnnotation
        public void superAnnotatedMethod() {
        }
    }

    interface AnnotatedInterface {

        @TestAnnotation
        void interfaceAnnotatedMethod();
    }

    static class ImplementsInterface implements AnnotatedInterface {

        // inherits the method from the interface
        public void interfaceAnnotatedMethod() {
        }
    }

    /* -------------------------------------------------------------
     *  Helper classes / interfaces for the test scenarios
     * ------------------------------------------------------------- */
    static class DirectAnnotated {

        @TestAnno("direct")
        public void annotatedMethod() {
        }

        public void nonAnnotatedMethod() {
        }
    }

    static class SubClass extends SuperClass {

        // overrides without annotation – should inherit from SuperClass
        @Override
        public void inheritedMethod() {
        }
    }

    interface TestInterface {

        @TestAnno("iface")
        void interfaceMethod();
    }

    static class ImplClass implements TestInterface {

        // implementation without annotation – should inherit from interface
        @Override
        public void interfaceMethod() {
        }
    }

    static class NoAnnoClass {

        public void plainMethod() {
        }
    }

    static class ParamClass {

        @TestAnno("params")
        public void paramMethod(String s, int i) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Replace the cache with a fresh, empty map to guarantee test isolation
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
    }

    @Test
    void testDirectAnnotationFound() throws NoSuchMethodException {
        Method method = DirectAnnotated.class.getMethod("annotatedMethod");
        TestAnnotation ann = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        assertNotNull(ann, "Annotation should be found on the method itself");
    }

    @Test
    void testDirectAnnotationNotFound() throws NoSuchMethodException {
        Method method = DirectAnnotated.class.getMethod("nonAnnotatedMethod");
        TestAnnotation ann = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        assertNull(ann, "No annotation should be returned when none is present");
    }

    @Test
    void testInheritedFromSuperclass() throws NoSuchMethodException {
        // Method is declared in SuperClass but accessed via SubClass
        Method method = SubClass.class.getMethod("superAnnotatedMethod");
        TestAnnotation ann = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        assertNotNull(ann, "Annotation should be discovered on the superclass method");
    }

    @Test
    void testInheritedFromInterface() throws NoSuchMethodException {
        // Method is declared on an interface and implemented by a concrete class
        Method method = ImplementsInterface.class.getMethod("interfaceAnnotatedMethod");
        TestAnnotation ann = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        assertNotNull(ann, "Annotation should be discovered on the interface method");
    }

    @Test
    void testNullMethodArgument() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethodAnnotation(null, TestAnnotation.class), "Passing a null Method should throw NPE");
    }

    @Test
    void testNullAnnotationClassArgument() throws NoSuchMethodException {
        Method method = DirectAnnotated.class.getMethod("annotatedMethod");
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethodAnnotation(method, null), "Passing a null annotation class should throw NPE");
    }

    @Test
    void testCacheReturnsSameInstance() throws NoSuchMethodException {
        Method method = DirectAnnotated.class.getMethod("annotatedMethod");
        TestAnnotation first = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        TestAnnotation second = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        // The cache should return the exact same proxy instance
        assertSame(first, second, "Subsequent look‑ups should return the cached annotation instance");
    }

    /* -------------------------------------------------------------
     *  1. Null arguments – expect IllegalArgumentException with exact messages
     * ------------------------------------------------------------- */
    @Test
    void nullMethodArgument() throws NoSuchMethodException {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(null, TestAnno.class));
        assertEquals("method cannot be null", ex.getMessage());
    }

    @Test
    void nullAnnotationClassArgument() throws NoSuchMethodException {
        Method method = DirectAnnotated.class.getMethod("annotatedMethod");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(method, null));
        assertEquals("annotation class cannot be null", ex.getMessage());
    }

    /* -------------------------------------------------------------
     *  2. Direct annotation present on the method itself
     * ------------------------------------------------------------- */
    @Test
    void directAnnotationFound() throws NoSuchMethodException {
        Method method = DirectAnnotated.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno, "Annotation should be found on the method itself");
        assertEquals("direct", anno.value(), "Annotation value should match the declared one");
    }

    /* -------------------------------------------------------------
     *  3. Annotation inherited from a superclass
     * ------------------------------------------------------------- */
    @Test
    void inheritedFromSuperclass() throws NoSuchMethodException {
        // Method is declared in SuperClass but accessed via SubClass
        Method method = SubClass.class.getMethod("inheritedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno, "Annotation should be discovered on the superclass method");
        assertEquals("super", anno.value(), "Annotation value should be taken from the superclass");
    }

    /* -------------------------------------------------------------
     *  4. Annotation inherited from an interface
     * ------------------------------------------------------------- */
    @Test
    void inheritedFromInterface() throws NoSuchMethodException {
        Method method = ImplClass.class.getMethod("interfaceMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno, "Annotation should be discovered on the interface method");
        assertEquals("iface", anno.value(), "Annotation value should be taken from the interface");
    }

    /* -------------------------------------------------------------
     *  5. No annotation found anywhere in the hierarchy
     * ------------------------------------------------------------- */
    @Test
    void noAnnotationFound() throws NoSuchMethodException {
        Method method = NoAnnoClass.class.getMethod("plainMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNull(anno, "When no annotation is present the result must be null");
    }

    /* -------------------------------------------------------------
     *  6. Method with parameters – ensure signature matching works
     * ------------------------------------------------------------- */
    @Test
    void methodWithParameters() throws NoSuchMethodException {
        Method method = ParamClass.class.getMethod("paramMethod", String.class, int.class);
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno, "Annotation on a method with parameters should be found");
        assertEquals("params", anno.value(), "Annotation value should match the declared one");
    }

    /* -------------------------------------------------------------
     *  7. Cache reuse – repeated look‑ups must return the same instance
     * ------------------------------------------------------------- */
    @Test
    void cacheReturnsSameInstance() throws NoSuchMethodException {
        Method method = DirectAnnotated.class.getMethod("annotatedMethod");
        TestAnno first = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        TestAnno second = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertSame(first, second, "Subsequent look‑ups should return the cached annotation instance");
    }
}
