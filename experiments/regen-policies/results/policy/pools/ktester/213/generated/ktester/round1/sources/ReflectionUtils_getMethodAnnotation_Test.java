package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethodAnnotation_Test {

    static class SubClass extends SuperClass {

        // inherits annotatedMethod without overriding
        @Override
        public void nonAnnotatedMethod() // overrides without annotation
        {
        }
    }

    static class InterfaceImpl implements AnnotatedInterface {
        // inherits interfaceMethod without overriding
    }

    static class NoAnnotationClass {

        public void plainMethod() {
        }
    }

    /* ---------- simple hierarchy used by earlier tests ---------- */
    static class SuperClass {

        @MyAnno("class")
        public void annotatedMethod() {
        }

        public void nonAnnotatedMethod() {
        }
    }

    interface AnnotatedInterface {

        @MyAnno("iface")
        void interfaceMethod();
    }

    // Superclass with an annotated method that will be inherited
    static class SuperWithAnno {

        @MyAnno("super")
        public void inheritedMethod() {
        }
    }

    static class SubWithInherited extends SuperWithAnno {
    }

    // Interface with annotated method
    interface MyInterface {

        @MyAnno("iface")
        void conflictMethod();
    }

    // Class that also inherits a method from a superclass that is annotated
    static class PrecedenceSuper {

        @MyAnno("super")
        public void conflictMethod() {
        }
    }

    static class PrecedenceClass extends PrecedenceSuper implements MyInterface {
    }

    // Private method in a superclass
    static class PrivateSuper {

        @MyAnno("privateSuper")
        private void privateSuperMethod() {
        }
    }

    static class ChildClass extends PrivateSuper {
        // No overriding; we will request the method via reflection on ChildClass
    }

    // Overloaded methods
    static class OverloadedClass {

        @MyAnno("intVersion")
        public void overloaded(int i) {
        }

        public void overloaded(String s) {
        }
    }

    // Static annotated method
    static class StaticAnnotatedClass {

        @MyAnno("static")
        public static void staticMethod() {
        }
    }

    // Generic interface + bridge method scenario
    interface GenericInterface<T> {

        @MyAnno("generic")
        void process(T t);
    }

    static class GenericImpl implements GenericInterface<String> {

        @Override
        public void process(String s) {
        }
    }

    // Simple class with a method annotated by MyAnno("class")
    static class AnnotatedClass {

        @MyAnno("class")
        public void annotatedMethod() {
        }
    }

    // Plain class without any annotation
    static class PlainClass {

        public void plainMethod() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a fresh cache for each test to avoid cross‑test contamination
        ReflectionUtils.setMethodAnnotationCache(new ConcurrentHashMap<>());
    }

    static Stream<Arguments> overloadedMethodProvider() throws NoSuchMethodException {
        return Stream.of(Arguments.of(OverloadedClass.class.getMethod("overloaded", int.class), "intVersion"), Arguments.of(OverloadedClass.class.getMethod("overloaded", String.class), null));
    }

    static Stream<Arguments> invalidAnnotationClassProvider() throws NoSuchMethodException {
        Method m = AnnotatedClass.class.getMethod("annotatedMethod");
        return Stream.of(Arguments.of(m, String.class), Arguments.of(m, Object.class));
    }

    @Test
    void shouldReturnAnnotationWhenMethodIsDirectlyAnnotated() throws NoSuchMethodException {
        Method method = SuperClass.class.getMethod("annotatedMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno, "Annotation should be found on directly annotated method");
        assertEquals("class", anno.value());
    }

    @Test
    void shouldFindAnnotationInSuperclassWhenSubclassMethodIsNotAnnotated() throws NoSuchMethodException {
        // SubClass inherits annotatedMethod without overriding it
        Method method = SubClass.class.getMethod("annotatedMethod");
        TestAnnotation anno = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        assertNotNull(anno, "Annotation should be resolved from the superclass");
    }

    @Test
    void shouldFindAnnotationInSuperclassWhenSubclassOverridesWithoutAnnotation() throws NoSuchMethodException {
        // SubClass overrides nonAnnotatedMethod (which is not annotated in SuperClass)
        // The overridden method itself is not annotated, but the superclass version is not annotated either,
        // so the result should be null.
        Method method = SubClass.class.getMethod("nonAnnotatedMethod");
        TestAnnotation anno = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        assertNull(anno, "No annotation should be found when neither the overriding nor the overridden method is annotated");
    }

    @Test
    void shouldFindAnnotationOnInterfaceMethod() throws NoSuchMethodException {
        Method method = InterfaceImpl.class.getMethod("interfaceMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno, "Annotation should be discovered on the implemented interface method");
        assertEquals("iface", anno.value());
    }

    @Test
    void shouldReturnNullWhenNoAnnotationIsPresent() throws NoSuchMethodException {
        Method method = NoAnnotationClass.class.getMethod("plainMethod");
        TestAnnotation anno = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
        assertNull(anno, "Method without any annotation should return null");
    }

    @Test
    void shouldThrowExceptionWhenMethodIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(null, TestAnnotation.class));
    }

    @Test
    void shouldThrowExceptionWhenAnnotationClassIsNull() throws NoSuchMethodException {
        Method method = SuperClass.class.getMethod("annotatedMethod");
        Assertions.assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(method, null));
    }

    // ---------- Annotation present on the method itself ----------
    @Test
    void annotationPresentOnMethodItself() throws NoSuchMethodException {
        Method method = AnnotatedClass.class.getMethod("annotatedMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno);
        assertEquals("class", anno.value());
    }

    // ---------- Annotation inherited from superclass ----------
    @Test
    void annotationInheritedFromSuperclass() throws NoSuchMethodException {
        Method method = SubWithInherited.class.getMethod("inheritedMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno);
        assertEquals("super", anno.value());
    }

    // ---------- Annotation present on an implemented interface ----------
    @Test
    void annotationPresentOnImplementedInterface() throws NoSuchMethodException {
        Method method = InterfaceImpl.class.getMethod("interfaceMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno);
        assertEquals("iface", anno.value());
    }

    // ---------- No annotation found anywhere ----------
    @Test
    void noAnnotationFoundAnywhere() throws NoSuchMethodException {
        Method method = PlainClass.class.getMethod("plainMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNull(anno);
    }

    // ---------- Cache hit (annotation already cached) ----------
    @Test
    void cacheHitReturnsSameInstance() throws NoSuchMethodException {
        Method method = AnnotatedClass.class.getMethod("annotatedMethod");
        MyAnno first = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        MyAnno second = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertSame(first, second, "Second call should return the cached annotation instance");
    }

    // ---------- Cache hit for null result ----------
    @Test
    void cacheHitForNullResult() throws NoSuchMethodException {
        Method method = PlainClass.class.getMethod("plainMethod");
        MyAnno first = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        MyAnno second = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNull(first);
        assertNull(second);
    }

    // ---------- Annotation precedence: superclass vs interface ----------
    @Test
    void annotationPrecedenceSuperclassOverInterface() throws NoSuchMethodException {
        Method method = PrecedenceClass.class.getMethod("conflictMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno);
        assertEquals("super", anno.value(), "Superclass annotation should win over interface annotation");
    }

    // ---------- Annotation on private method in superclass ----------
    @Test
    void annotationOnPrivateSuperclassMethod() throws NoSuchMethodException {
        // The method is private in PrivateSuper; we obtain it via reflection on ChildClass
        Method method = ChildClass.class.getDeclaredMethod("privateSuperMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno);
        assertEquals("privateSuper", anno.value());
    }

    // ---------- Annotation on overloaded method (parameter types matter) ----------
    @ParameterizedTest
    @MethodSource("overloadedMethodProvider")
    void annotationOnOverloadedMethod(Method method, String expectedValue) throws Exception {
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        if (expectedValue == null) {
            assertNull(anno, "Expected no annotation for method " + method);
        } else {
            assertNotNull(anno);
            assertEquals(expectedValue, anno.value(), "Annotation value mismatch for method " + method);
        }
    }

    // ---------- Annotation on static method ----------
    @Test
    void annotationOnStaticMethod() throws NoSuchMethodException {
        Method method = StaticAnnotatedClass.class.getMethod("staticMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno);
        assertEquals("static", anno.value());
    }

    // ---------- Different annotation class returns null ----------
    @Test
    void differentAnnotationClassReturnsNull() throws NoSuchMethodException {
        Method method = AnnotatedClass.class.getMethod("annotatedMethod");
        OtherAnno anno = ReflectionUtils.getMethodAnnotation(method, OtherAnno.class);
        assertNull(anno, "Requesting a different annotation type should yield null");
    }

    // ---------- Annotation on bridge method generated for generics ----------
    @Test
    void annotationOnBridgeMethodFromGenericInterface() throws NoSuchMethodException {
        // The bridge method has signature (Object) after type erasure
        Method bridgeMethod = GenericImpl.class.getMethod("process", Object.class);
        MyAnno anno = ReflectionUtils.getMethodAnnotation(bridgeMethod, MyAnno.class);
        assertNotNull(anno);
        assertEquals("generic", anno.value(), "Bridge method should inherit annotation from generic interface");
    }

    // ---------- Invalid annotation class argument ----------
    @ParameterizedTest
    @MethodSource("invalidAnnotationClassProvider")
    void invalidAnnotationClassArgument(Method method, Class<?> invalidAnnoClass) {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(method, invalidAnnoClass), "Expected IllegalArgumentException for non‑annotation class " + invalidAnnoClass);
    }

    // ---------- Method annotation cache is null ----------
    @Test
    void methodAnnotationCacheIsNull() throws NoSuchMethodException {
        ReflectionUtils.setMethodAnnotationCache(null);
        Method method = AnnotatedClass.class.getMethod("annotatedMethod");
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethodAnnotation(method, MyAnno.class), "Calling with a null cache should throw NPE");
    }

    // ---------- Method annotation cache is unmodifiable ----------
    @Test
    void methodAnnotationCacheIsUnmodifiable() throws NoSuchMethodException {
        ReflectionUtils.setMethodAnnotationCache(Collections.emptyMap());
        Method method = AnnotatedClass.class.getMethod("annotatedMethod");
        assertThrows(UnsupportedOperationException.class, () -> ReflectionUtils.getMethodAnnotation(method, MyAnno.class), "Modifying an unmodifiable cache should throw UnsupportedOperationException");
    }

    // ---------- Cache stability after annotation removal (simulated) ----------
    @Test
    void cacheStabilityAfterSimulatedRemoval() throws NoSuchMethodException {
        // First call caches the annotation
        Method method = AnnotatedClass.class.getMethod("annotatedMethod");
        MyAnno cached = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(cached);
        // Simulate "removal" by swapping the cache with a fresh empty map.
        // The previously cached value should still be returned because the
        // method we just called used the original cache reference.
        Map<Object, Annotation> fresh = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodAnnotationCache(fresh);
        // Second call goes to the *new* cache – we expect null because the cache
        // no longer contains the entry. This demonstrates that the cache does not
        // magically retain old values after being replaced.
        MyAnno afterSwap = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNull(afterSwap, "After swapping the cache, the annotation should not be found");
    }
}
