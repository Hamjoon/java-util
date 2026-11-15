package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassAnnotation_Test {

    /**
     * Class directly annotated with {@link Marker}.
     */
    @Marker
    private static class DirectlyAnnotated {
    }

    /**
     * Superclass annotated with {@link Marker}.
     */
    @Marker
    private static class SuperAnnotated {
    }

    /**
     * Subclass without its own annotation – should inherit from superclass.
     */
    private static class SubclassInherits extends SuperAnnotated {
    }

    /**
     * Interface annotated with {@link Marker}.
     */
    @Marker
    private interface AnnotatedInterface {
    }

    /**
     * Class implementing an annotated interface.
     */
    private static class ImplementsInterface implements AnnotatedInterface {
    }

    /**
     * Unrelated class with no annotation anywhere in its hierarchy.
     */
    private static class NoAnnotation {
    }

    /**
     * Interface annotated with {@link MyAnno}.
     */
    @MyAnno("interface")
    private interface AnnotatedMyAnnoInterface {
    }

    /**
     * Class implementing the above interface.
     */
    private static class InterfaceImpl implements AnnotatedMyAnnoInterface {
    }

    /**
     * Class directly annotated with {@link MyAnno}.
     */
    @MyAnno("class")
    private static class AnnotatedClass {
    }

    /**
     * Superclass annotated with {@link MyAnno}.
     */
    @MyAnno("superclass")
    private static class SuperClassWithAnno {
    }

    /**
     * Subclass without its own annotation – should inherit from superclass.
     */
    private static class SubClass extends SuperClassWithAnno {
    }

    /**
     * Duplicate‑interface scenario – interface annotated with {@link MyAnno}.
     */
    @MyAnno("interface")
    private interface DuplicateAnnotatedInterface {
    }

    /**
     * Base class implements the annotated interface.
     */
    private static class BaseImpl implements DuplicateAnnotatedInterface {
    }

    /**
     * Subclass also implements the same interface (via inheritance and directly).
     */
    private static class DuplicateInterfaceImpl extends BaseImpl implements DuplicateAnnotatedInterface {
    }

    /**
     * Deep hierarchy – grand‑superclass annotated.
     */
    @MyAnno("grandSuperclass")
    private static class GrandSuperclass {
    }

    private static class MidClass extends GrandSuperclass {
    }

    private static class DeepSubClass extends MidClass {
    }

    /**
     * Interface annotated on a superclass.
     */
    @MyAnno("superInterface")
    private interface AnnotatedSuperInterface {
    }

    private static class SuperWithInterface implements AnnotatedSuperInterface {
    }

    private static class SubClassWithSuperInterface extends SuperWithInterface {
    }

    /**
     * Multiple interfaces – first wins.
     */
    @MyAnno("firstInterface")
    private interface FirstAnnotatedInterface {
    }

    @MyAnno("secondInterface")
    private interface SecondAnnotatedInterface {
    }

    private static class MultiInterfaceImpl implements FirstAnnotatedInterface, SecondAnnotatedInterface {
    }

    /**
     * Generic super‑class annotated.
     */
    @MyAnno("genericSuper")
    private static class GenericSuperClass<T> {
    }

    private static class GenericSubclass<T> extends GenericSuperClass<T> {
    }

    /**
     * Shared interface used by two different paths.
     */
    @MyAnno("sharedInterface")
    private interface SharedInterface {
    }

    private interface InterfaceA extends SharedInterface {
    }

    private interface InterfaceB extends SharedInterface {
    }

    private static class BaseClass implements InterfaceB {
    }

    private static class ComplexImpl extends BaseClass implements InterfaceA {
    }

    /**
     * Circular interface hierarchy – no annotation.
     */
    private interface CircularA extends CircularB {
    }

    private interface CircularB extends CircularA {
    }

    private static class ImplementsCircular implements CircularA {
    }

    @Test
    void shouldReturnAnnotationWhenClassIsDirectlyAnnotated() {
        Class<?> target = DirectlyAnnotated.class;
        Marker result = ReflectionUtils.getClassAnnotation(target, Marker.class);
        assertNotNull(result, "Annotation should be found on the class itself");
    }

    @Test
    void shouldFindAnnotationInSuperclassWhenNotPresentOnClass() {
        Class<?> target = SubclassInherits.class;
        Marker result = ReflectionUtils.getClassAnnotation(target, Marker.class);
        assertNotNull(result, "Annotation should be inherited from the superclass");
    }

    @Test
    void shouldFindAnnotationOnImplementedInterface() {
        Class<?> target = ImplementsInterface.class;
        Marker result = ReflectionUtils.getClassAnnotation(target, Marker.class);
        assertNotNull(result, "Annotation should be found on an implemented interface");
    }

    @Test
    void shouldReturnNullWhenAnnotationIsAbsentInEntireHierarchy() {
        Class<?> target = NoAnnotation.class;
        Marker result = ReflectionUtils.getClassAnnotation(target, Marker.class);
        assertNull(result, "No annotation should be found");
    }

    @Test
    void shouldReturnNullWhenSearchingForDifferentAnnotationType() {
        Class<?> target = DirectlyAnnotated.class;
        Another result = ReflectionUtils.getClassAnnotation(target, Another.class);
        assertNull(result, "Different annotation type should not be returned");
    }

    @Test
    void shouldHandleNullClassArgumentGracefully() {
        Class<?> target = null;
        Marker result = ReflectionUtils.getClassAnnotation(target, Marker.class);
        assertNull(result, "Null class argument should result in null return");
    }

    @Test
    void shouldHandleNullAnnotationClassArgumentGracefully() {
        Class<?> target = DirectlyAnnotated.class;
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassAnnotation(target, null), "Null annotation class should cause NPE");
    }

    @Test
    void shouldNotEnterInfiniteLoopWhenCircularInterfaceReferencesExist() {
        ???;
        assertNull(result, "Circular interface hierarchy should be safely handled and return null");
    }

    @Test
    void shouldFindAnnotationOnDirectlyImplementedInterface() {
        MyAnno result = ReflectionUtils.getClassAnnotation(InterfaceImpl.class, MyAnno.class);
        assertNotNull(result);
        assertEquals("interface", result.value(), "Should retrieve annotation from the interface");
    }

    @Test
    void shouldFindAnnotationOnSuperclass() {
        MyAnno result = ReflectionUtils.getClassAnnotation(SubClass.class, MyAnno.class);
        assertNotNull(result);
        assertEquals("superclass", result.value(), "Should retrieve annotation from the superclass");
    }

    @Test
    void shouldReturnAnnotationWhenClassIsDirectlyAnnotatedWithMyAnno() {
        MyAnno result = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, MyAnno.class);
        assertNotNull(result);
        assertEquals("class", result.value(), "Should retrieve annotation directly on the class");
    }

    @Test
    void shouldPreventDuplicateInterfaceProcessing() {
        MyAnno result = ReflectionUtils.getClassAnnotation(DuplicateInterfaceImpl.class, MyAnno.class);
        assertNotNull(result);
        assertEquals("interface", result.value(), "Annotation should be found despite the interface being pushed twice");
    }

    @Test
    void shouldFindAnnotationOnDistantSuperclass() {
        MyAnno result = ReflectionUtils.getClassAnnotation(DeepSubClass.class, MyAnno.class);
        assertNotNull(result);
        assertEquals("grandSuperclass", result.value(), "Should locate annotation several levels up the inheritance chain");
    }

    @Test
    void shouldFindAnnotationOnSuperclassInterface() {
        MyAnno result = ReflectionUtils.getClassAnnotation(SubClassWithSuperInterface.class, MyAnno.class);
        assertNotNull(result);
        assertEquals("superInterface", result.value(), "Should locate annotation on an interface implemented by a superclass");
    }

    @Test
    void shouldReturnFirstFoundAnnotationWhenMultipleInterfacesPresent() {
        MyAnno result = ReflectionUtils.getClassAnnotation(MultiInterfaceImpl.class, MyAnno.class);
        assertNotNull(result);
        assertEquals("firstInterface", result.value(), "First interface discovered (stack order) should win");
    }

    @Test
    void shouldFindAnnotationOnGenericSuperclass() {
        // Use a concrete generic type to obtain a Class object.
        Class<?> concrete = new GenericSubclass<String>() {
        }.getClass();
        MyAnno result = ReflectionUtils.getClassAnnotation(concrete, MyAnno.class);
        assertNotNull(result);
        assertEquals("genericSuper", result.value(), "Annotation on the raw generic superclass should be discovered");
    }

    @Test
    void shouldFindAnnotationWhenInterfaceAppearsViaMultiplePaths() {
        MyAnno result = ReflectionUtils.getClassAnnotation(ComplexImpl.class, MyAnno.class);
        assertNotNull(result);
        assertEquals("sharedInterface", result.value(), "Annotation should be found even when the interface is reachable through several paths");
    }

    @Test
    void shouldHandleCircularInterfaceHierarchyWithoutInfiniteLoop() {
        // No annotation anywhere; method must terminate safely.
        MyAnno result = ReflectionUtils.getClassAnnotation(ImplementsCircular.class, MyAnno.class);
        assertNull(result, "Circular interface hierarchy should be handled and return null");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAnnoClassIsNotAnAnnotation() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getClassAnnotation(AnnotatedClass.class, String.class), "Passing a non‑annotation class should cause IllegalArgumentException");
    }

    @Test
    void shouldReturnNullWhenAnnotationAbsentOnStandardJDKClass() {
        MyAnno result = ReflectionUtils.getClassAnnotation(String.class, MyAnno.class);
        assertNull(result, "Standard JDK classes should not cause unexpected annotation matches");
    }
}
