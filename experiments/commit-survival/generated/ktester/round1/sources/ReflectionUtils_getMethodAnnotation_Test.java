package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

class ReflectionUtils_getMethodAnnotation_Test {

    /* --------------------------------------------------------------------- */
    private static class Direct {

        @MyAnno("direct")
        public void annotatedMethod() {
        }
    }

    private static class Super {

        @MyAnno("super")
        public void foo() {
        }
    }

    private static class Sub extends Super {

        // No annotation here, overrides foo
        @Override
        public void foo() {
        }
    }

    private interface I {

        @MyAnno("interface")
        void bar();
    }

    private static class Impl implements I {

        // No annotation here, implements bar
        @Override
        public void bar() {
        }
    }

    private static class NoAnno {

        public void nothing() {
        }
    }

    // 1. Direct annotation on the method itself
    private static class DirectClass {

        @TestAnno("direct")
        public void annotatedMethod() {
        }
    }

    // 2. Superclass annotation, subclass overrides without annotation
    private static class BaseClass {

        @TestAnno("super")
        public void inheritedMethod() {
        }

        @TestAnno("base")
        public void overriddenMethod() {
        }
    }

    private static class SubClass extends BaseClass {

        // overrides without annotation – should inherit from BaseClass
        @Override
        public void inheritedMethod() {
        }

        // overrides with its own annotation – should take precedence
        @Override
        @TestAnno("sub")
        public void overriddenMethod() {
        }

        // unrelated method – not present in BaseClass
        public void unrelatedMethod() {
        }
    }

    // 3. Interface annotation
    private interface InterfaceAnnotated {

        @TestAnno("interface")
        void interfaceMethod();
    }

    private static class ImplClass implements InterfaceAnnotated {

        @Override
        public void interfaceMethod() {
        }
    }

    // 4. No annotation anywhere
    private static class NoAnnoClass {

        public void someMethod() {
        }
    }

    // 5. Multiple interfaces with common parent (duplicate‑handling test)
    private interface InterfaceCommon {

        @TestAnno("common")
        void commonMethod();
    }

    private interface InterfaceA extends InterfaceCommon {

        @TestAnno("ifaceA")
        void methodA();
    }

    private interface InterfaceB extends InterfaceCommon {

        void methodB();
    }

    private static class MultiInterfaceImpl implements InterfaceA, InterfaceB {

        @Override
        public void methodA() {
        }

        @Override
        public void methodB() {
        }

        @Override
        public void commonMethod() {
        }
    }

    // 6. Interface hierarchy (super‑interface annotation)
    private interface InterfaceSuper {

        @TestAnno("superInterface")
        void interfaceMethod();
    }

    private interface InterfaceChild extends InterfaceSuper {
    }

    private static class ImplOverrides implements InterfaceChild {

        // overrides without annotation – should fall back to InterfaceSuper
        @Override
        public void interfaceMethod() {
        }
    }

    // 7. Parent‑interface annotation (leaf implementation)
    private interface ParentInterface {

        @TestAnno("parentInterface")
        void leafMethod();
    }

    private interface ChildInterface extends ParentInterface {
    }

    private static class LeafImpl implements ChildInterface {
        // does NOT override leafMethod()
    }

    // 8. Static method annotation
    private static class StaticAnnotatedClass {

        @TestAnno("static")
        public static void staticAnnotated() {
        }
    }

    // 9. Private method annotation
    private static class PrivateAnnotatedClass {

        @TestAnno("private")
        private void privateAnnotated() {
        }
    }

    // 10. Primitive parameter method
    private static class PrimitiveParamClass {

        @TestAnno("primitive")
        public void primitiveMethod(int i) {
        }
    }

    // 11. Var‑args method
    private static class VarArgsClass {

        @TestAnno("varargs")
        public void varArgsMethod(String... args) {
        }
    }

    /**
     * Simple holder for parameterised test arguments.
     */
    private static class ArgumentsHolder {

        final Method method;

        final Class<TestAnno> annoClass;

        // null means we expect null result
        final String expectedValue;

        final String description;

        ArgumentsHolder(Method method, Class<TestAnno> annoClass, String expectedValue, String description) {
            this.method = method;
            this.annoClass = annoClass;
            this.expectedValue = expectedValue;
            this.description = description;
        }
    }

    /* --------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> simpleCases() {
        return Stream.of(// Direct annotation
        new ArgumentsHolder(getMethod(DirectClass.class, "annotatedMethod"), TestAnno.class, "direct", "Direct annotation on the method itself"), // Superclass annotation (method overridden without annotation)
        new ArgumentsHolder(getMethod(SubClass.class, "inheritedMethod"), TestAnno.class, "super", "Superclass annotation should be discovered"), // Interface annotation
        new ArgumentsHolder(getMethod(ImplClass.class, "interfaceMethod"), TestAnno.class, "interface", "Annotation on the interface method"), // No annotation anywhere
        new ArgumentsHolder(getMethod(NoAnnoClass.class, "someMethod"), TestAnno.class, null, "No annotation in the whole hierarchy"), // Static method annotation
        new ArgumentsHolder(getMethod(StaticAnnotatedClass.class, "staticAnnotated"), TestAnno.class, "static", "Annotation on a static method"), // Private method annotation (make it accessible)
        new ArgumentsHolder(getPrivateMethod(PrivateAnnotatedClass.class, "privateAnnotated"), TestAnno.class, "private", "Annotation on a private method"), // Primitive parameter method
        new ArgumentsHolder(getMethod(PrimitiveParamClass.class, "primitiveMethod", int.class), TestAnno.class, "primitive", "Annotation on method with primitive parameter"), // Var‑args method
        new ArgumentsHolder(getMethod(VarArgsClass.class, "varArgsMethod", String[].class), TestAnno.class, "varargs", "Annotation on var‑args method"));
    }

    /* --------------------------------------------------------------------- */
    private static Method getMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            fail("Test setup failure – method not found: " + name, e);
            // unreachable
            return null;
        }
    }

    private static Method getPrivateMethod(Class<?> clazz, String name) {
        try {
            Method m = clazz.getDeclaredMethod(name);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            fail("Test setup failure – private method not found: " + name, e);
            // unreachable
            return null;
        }
    }

    @Test
    void shouldReturnDirectAnnotationWhenPresentOnMethod() throws Exception {
        Method method = Direct.class.getMethod("annotatedMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno, "Annotation should be found on the method itself");
        assertEquals("direct", anno.value());
    }

    @Test
    void shouldFindAnnotationInSuperclassWhenMissingOnOverride() throws Exception {
        Method subMethod = Sub.class.getDeclaredMethod("foo");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(subMethod, MyAnno.class);
        assertNotNull(anno, "Annotation should be inherited from the superclass");
        assertEquals("super", anno.value());
    }

    @Test
    void shouldFindAnnotationOnImplementedInterfaceMethod() throws Exception {
        Method implMethod = Impl.class.getDeclaredMethod("bar");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(implMethod, MyAnno.class);
        assertNotNull(anno, "Annotation should be discovered on the interface method");
        assertEquals("interface", anno.value());
    }

    @Test
    void shouldReturnNullWhenAnnotationIsAbsentInHierarchy() throws Exception {
        Method method = NoAnno.class.getMethod("nothing");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNull(anno, "No annotation should be found anywhere in the hierarchy");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("simpleCases")
    void shouldFindAnnotationAccordingToScenario(ArgumentsHolder holder) {
        // Act
        TestAnno result = ReflectionUtils.getMethodAnnotation(holder.method, holder.annoClass);
        // Assert
        if (holder.expectedValue == null) {
            assertNull(result, holder.description);
        } else {
            assertNotNull(result, holder.description);
            assertEquals(holder.expectedValue, result.value(), holder.description);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void visitedSetPreventsDuplicateInterfaceProcessing() throws Exception {
        Method method = MultiInterfaceImpl.class.getDeclaredMethod("methodA");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno, "Annotation should be found on InterfaceA");
        assertEquals("ifaceA", anno.value());
        // The test also ensures that processing InterfaceCommon only once does not cause loops.
    }

    /* --------------------------------------------------------------------- */
    @Test
    void interfaceAnnotationWhenSubclassOverridesWithoutIt() throws Exception {
        Method method = ImplOverrides.class.getDeclaredMethod("interfaceMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno, "Annotation should be resolved from super‑interface");
        assertEquals("superInterface", anno.value());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void annotationOnSuperInterfaceMethod() throws Exception {
        Method method = LeafImpl.class.getDeclaredMethod("leafMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno, "Annotation should be found on ParentInterface");
        assertEquals("parentInterface", anno.value());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void annotationOnOverriddenMethodInSubclassTakesPrecedence() throws Exception {
        Method method = SubClass.class.getDeclaredMethod("overriddenMethod");
        TestAnno anno = ReflectionUtils.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno, "Subclass annotation should override superclass annotation");
        assertEquals("sub", anno.value());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullAnnoClassParameterThrowsNPE() throws Exception {
        Method method = DirectClass.class.getMethod("annotatedMethod");
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethodAnnotation(method, null), "Passing null as annoClass must throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullMethodParameterThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethodAnnotation(null, TestAnno.class), "Passing null as method must throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nonAnnotationClassParameterThrowsIAE() throws Exception {
        Method method = DirectClass.class.getMethod("annotatedMethod");
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(method, String.class), "Providing a non‑annotation class must throw IllegalArgumentException");
    }
}
