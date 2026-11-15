package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.annotation.*;
import java.lang.reflect.Method;

class ReflectionUtils_getMethodAnnotation_Test {

    interface InterfaceA {

        @TestAnno("interface")
        void annotatedMethod();
    }

    static class SuperClass {

        @TestAnno("superclass")
        public void annotatedMethod() {
        }
    }

    static class DirectClass {

        @TestAnno("direct")
        public void annotatedMethod() {
        }
    }

    static class ChildClass extends SuperClass implements InterfaceA {

        @Override
        public void annotatedMethod() {
        }
    }

    static class InterfaceOnlyClass implements InterfaceA {

        @Override
        public void annotatedMethod() {
        }
    }

    static class NoAnnotationClass {

        public void plainMethod() {
        }
    }

    static class DirectAnnotated {

        @MyAnno("direct")
        public void foo() {
        }
    }

    static class BaseAnnotated {

        @MyAnno("base")
        public void foo() {
        }
    }

    static class SubOverride extends BaseAnnotated {

        @Override
        public void foo() {
        }
    }

    interface IAnnotated {

        @MyAnno("iface")
        void foo();
    }

    static class Impl implements IAnnotated {

        @Override
        public void foo() {
        }
    }

    static class NoAnno {

        public void foo() {
        }
    }

    @Test
    void shouldReturnAnnotationWhenMethodIsDirectlyAnnotated() throws Exception {
        Method m = DirectClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno, "Annotation should be found on the method itself");
        assertEquals("direct", anno.value());
    }

    @Test
    void shouldFindAnnotationInSuperclassWhenNotPresentInSubclass() throws Exception {
        Method m = ChildClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno, "Annotation should be found in the superclass");
        assertEquals("superclass", anno.value());
    }

    @Test
    void shouldFindAnnotationInInterfaceWhenNotPresentInClassHierarchy() throws Exception {
        Method m = InterfaceOnlyClass.class.getMethod("annotatedMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNotNull(anno, "Annotation should be found in the implemented interface");
        assertEquals("interface", anno.value());
    }

    @Test
    void shouldReturnNullWhenNoAnnotationIsPresentAnywhere() throws Exception {
        Method m = NoAnnotationClass.class.getMethod("plainMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(m, TestAnno.class);
        assertNull(anno, "No annotation should be found");
    }

    @Test
    void shouldReturnNullWhenAnnotationClassDoesNotMatch() throws Exception {
        Method m = DirectClass.class.getMethod("annotatedMethod");
        ???;
        assertNull(anno, "Different annotation type should not be found");
    }

    @Test
    void directAnnotation_JSON() throws Exception {
        Method m = DirectAnnotated.class.getMethod("foo");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(m, MyAnno.class);
        assertNotNull(anno, "Annotation should be present on DirectAnnotated.foo");
        assertEquals("direct", anno.value());
    }

    @Test
    void superclassAnnotation_JSON() throws Exception {
        Method m = SubOverride.class.getMethod("foo");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(m, MyAnno.class);
        assertNotNull(anno, "Annotation should be inherited from BaseAnnotated.foo");
        assertEquals("base", anno.value());
    }

    @Test
    void interfaceAnnotation_JSON() throws Exception {
        Method m = Impl.class.getMethod("foo");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(m, MyAnno.class);
        assertNotNull(anno, "Annotation should be found on IAnnotated.foo");
        assertEquals("iface", anno.value());
    }

    @Test
    void overriddenWithoutAnnotation_JSON() throws Exception {
        Method m = SubOverride.class.getMethod("foo");
        Deprecated anno = ReflectionUtils.getMethodAnnotation(m, Deprecated.class);
        assertNull(anno, "No @Deprecated annotation should be found");
    }

    @Test
    void noAnnotation_JSON() throws Exception {
        Method m = NoAnno.class.getMethod("foo");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(m, MyAnno.class);
        assertNull(anno, "Method without any annotation should return null");
    }

    @Test
    void nullMethod_JSON() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethodAnnotation(null, MyAnno.class), "Passing a null Method should throw NPE");
    }

    @Test
    void nullAnnotationClass_JSON() throws Exception {
        Method m = DirectAnnotated.class.getMethod("foo");
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethodAnnotation(m, null), "Passing a null annotation class should throw NPE");
    }

    @Test
    void nonAnnotationClass_JSON() throws Exception {
        Method m = DirectAnnotated.class.getMethod("foo");
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(m, String.class), "Passing a non‑annotation class should throw IllegalArgumentException");
    }
}
