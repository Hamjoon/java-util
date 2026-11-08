package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.annotation.*;
import java.util.*;

class ReflectionUtils_getClassAnnotation_Test {

    @MyAnno("direct")
    static class DirectAnnotated {
    }

    static class SubClass extends DirectAnnotated {
    }

    static class ImplementsInterface implements AnnotatedInterface {
    }

    static class NoAnnotation {
    }

    // 1. Direct annotation
    @MyAnno("direct")
    static class AnnotatedClass {
    }

    // 2. Superclass annotation
    @MyAnno("super")
    static class SuperClassWithAnno {
    }

    static class SubClassWithoutAnno extends SuperClassWithAnno {
    }

    // 3. Interface hierarchy
    @MyAnno("interface")
    interface AnnotatedInterface {
    }

    // 4. No annotation anywhere
    static class UnannotatedClass {
    }

    // 5. Cycle in interface graph
    @MyAnno("cycle")
    interface InterfaceA {
    }

    interface InterfaceB extends InterfaceA {
    }

    static class CyclicClass implements InterfaceA, InterfaceB {
    }

    // 6. Super‑interface annotation
    @MyAnno("parent")
    interface InterfaceParent {
    }

    interface InterfaceChild extends InterfaceParent {
    }

    static class ImplSubInterface implements InterfaceChild {
    }

    // 7. Both superclass and interface have the annotation – algorithm should return the first popped (interface)
    @MyAnno("superClass")
    static class SuperClassForBoth {
    }

    @MyAnno("interface")
    interface InterfaceWithAnno {
    }

    static class ClassWithBothAnno extends SuperClassForBoth implements InterfaceWithAnno {
    }

    // 8. Non‑@Inherited annotation on superclass
    @PlainAnno("plain")
    static class SuperClassPlainAnno {
    }

    static class SubClassWithNonInheritedAnno extends SuperClassPlainAnno {
    }

    // 9. Restricted annotation – SecurityManager will deny access
    @RestrictedAnno("restricted")
    static class RestrictedAnnotatedClass {
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any custom SecurityManager that might have been installed by a test
        if (System.getSecurityManager() != null) {
            System.setSecurityManager(null);
        }
    }

    @Test
    void shouldReturnDirectAnnotation() {
        MyAnno anno = ReflectionUtils.getClassAnnotation(DirectAnnotated.class, MyAnno.class);
        assertNotNull(anno, "Annotation should be found on the class itself");
        assertEquals("direct", anno.value());
    }

    @Test
    void shouldFindAnnotationOnSuperclass() {
        MyAnno anno = ReflectionUtils.getClassAnnotation(SubClassWithoutAnno.class, MyAnno.class);
        assertNotNull(anno, "Annotation should be inherited from superclass");
        assertEquals("super", anno.value());
    }

    @Test
    void shouldFindAnnotationOnImplementedInterface() {
        MyAnno anno = ReflectionUtils.getClassAnnotation(ImplementsInterface.class, MyAnno.class);
        assertNotNull(anno, "Annotation should be discovered on an implemented interface");
        assertEquals("interface", anno.value());
    }

    @Test
    void shouldReturnNullWhenNoAnnotationPresent() {
        MyAnno anno = ReflectionUtils.getClassAnnotation(UnannotatedClass.class, MyAnno.class);
        assertNull(anno, "No annotation should be found, method must return null");
    }

    @Test
    void shouldReturnNullWhenClassIsNull() {
        MyAnno anno = ReflectionUtils.getClassAnnotation(null, MyAnno.class);
        assertNull(anno, "Null class argument should result in null return");
    }

    @Test
    void shouldThrowNPEWhenAnnotationClassIsNull() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassAnnotation(DirectAnnotated.class, null));
    }

    @Test
    void shouldHandleInterfaceCycleWithoutInfiniteLoop() {
        MyAnno anno = ReflectionUtils.getClassAnnotation(CyclicClass.class, MyAnno.class);
        assertNotNull(anno, "Annotation should be found despite the cyclic interface graph");
        assertEquals("cycle", anno.value());
    }

    @Test
    void shouldFindAnnotationOnSuperInterface() {
        MyAnno anno = ReflectionUtils.getClassAnnotation(ImplSubInterface.class, MyAnno.class);
        assertNotNull(anno, "Annotation should be found on a super‑interface of an implemented interface");
        assertEquals("parent", anno.value());
    }

    @Test
    void shouldPreferInterfaceOverSuperclassWhenBothHaveAnnotation() {
        MyAnno anno = ReflectionUtils.getClassAnnotation(ClassWithBothAnno.class, MyAnno.class);
        assertNotNull(anno, "Annotation should be found on the first visited type (interface)");
        // According to the algorithm, the interface is visited before the superclass
        assertEquals("interface", anno.value());
    }

    @Test
    void shouldNotInheritNonInheritedAnnotation() {
        PlainAnno anno = ReflectionUtils.getClassAnnotation(SubClassWithNonInheritedAnno.class, PlainAnno.class);
        assertNull(anno, "Non‑@Inherited annotation on a superclass must not be inherited");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAnnoClassIsNotAnAnnotation() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getClassAnnotation(AnnotatedClass.class, String.class));
    }

    @Test
    void shouldReturnNullForArrayClass() {
        MyAnno anno = ReflectionUtils.getClassAnnotation(String[].class, MyAnno.class);
        assertNull(anno, "Array types cannot carry class‑level annotations");
    }

    @Test
    void shouldReturnNullForPrimitiveClass() {
        MyAnno anno = ReflectionUtils.getClassAnnotation(int.class, MyAnno.class);
        assertNull(anno, "Primitive types never have annotations");
    }

    @Test
    void shouldPropagateSecurityExceptionWhenAccessDenied() {
        // Install a SecurityManager that denies reflective access to annotations
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Throw for any ReflectPermission (used internally by getAnnotation)
                if (perm instanceof java.lang.reflect.ReflectPermission) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
            }
        });
        assertThrows(SecurityException.class, () -> ReflectionUtils.getClassAnnotation(RestrictedAnnotatedClass.class, RestrictedAnno.class));
    }
}
