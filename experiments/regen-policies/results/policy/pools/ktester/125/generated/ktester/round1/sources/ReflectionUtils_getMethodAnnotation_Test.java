package com.cedarsoftware.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethodAnnotation_Test {

    /* -------------------------------------------------------------
     *  Helper classes / interfaces
     * ------------------------------------------------------------- */
    static class SuperClass {

        @TestMethodAnno
        public void annotatedInSuper() {
        }

        public void notAnnotated() {
        }
    }

    static class SubClass extends SuperClass {
        // inherits annotatedInSuper()
    }

    interface AnnotatedInterface {

        @TestMethodAnno
        void annotatedInterfaceMethod();
    }

    static class Impl implements AnnotatedInterface {

        @Override
        public void annotatedInterfaceMethod() {
        }
    }

    static class NoAnnotationClass {

        public void plainMethod() {
        }
    }

    // -----------------------------------------------------------------
    static class BaseClass {

        @MyAnno
        public void annotated(String s) {
        }

        public void notAnnotated(String s) {
        }
    }

    static class SubClassForSuper extends BaseClass {
        // inherits annotated(String)
    }

    interface MyInterface {

        @MyAnno
        void interfaceMethod();
    }

    static class ImplClass implements MyInterface {

        @Override
        public void interfaceMethod() {
        }
    }

    static class PlainClass {

        public void plainMethod(int i) {
        }
    }

    static class OverloadedClass {

        @MyAnno
        public void overloaded(String s) {
        }

        public void overloaded(int i) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the internal cache so each test runs in isolation
        ReflectionUtils.setMethodAnnotationCache(new HashMap<>());
    }

    private static Stream<Arguments> overloadedMethodProvider() throws NoSuchMethodException {
        Method stringOverload = OverloadedClass.class.getDeclaredMethod("overloaded", String.class);
        Method intOverload = OverloadedClass.class.getDeclaredMethod("overloaded", int.class);
        return Stream.of(Arguments.of("String overload is annotated", stringOverload, MyAnno.class, MyAnno.class), Arguments.of("int overload is not annotated", intOverload, MyAnno.class, null));
    }

    @Test
    void shouldReturnAnnotationWhenMethodIsDirectlyAnnotated() throws NoSuchMethodException {
        Method method = SuperClass.class.getMethod("annotatedInSuper");
        TestMethodAnno anno = ReflectionUtils.getMethodAnnotation(method, TestMethodAnno.class);
        assertNotNull(anno, "Annotation should be found on directly annotated method");
    }

    @Test
    void shouldFindAnnotationDeclaredInSuperclass() throws NoSuchMethodException {
        Method method = SubClass.class.getMethod("annotatedInSuper");
        TestMethodAnno anno = ReflectionUtils.getMethodAnnotation(method, TestMethodAnno.class);
        assertNotNull(anno, "Annotation should be discovered in the superclass hierarchy");
    }

    @Test
    void shouldFindAnnotationDeclaredOnInterface() throws NoSuchMethodException {
        Method method = Impl.class.getMethod("annotatedInterfaceMethod");
        TestMethodAnno anno = ReflectionUtils.getMethodAnnotation(method, TestMethodAnno.class);
        assertNotNull(anno, "Annotation should be discovered on the implemented interface");
    }

    @Test
    void shouldReturnNullWhenNoAnnotationPresent() throws NoSuchMethodException {
        Method method = NoAnnotationClass.class.getMethod("plainMethod");
        TestMethodAnno anno = ReflectionUtils.getMethodAnnotation(method, TestMethodAnno.class);
        assertNull(anno, "No annotation should be returned when none is present");
    }

    @Test
    void shouldCacheResultSoSubsequentCallsReturnSameInstance() throws NoSuchMethodException {
        Method method = SuperClass.class.getMethod("annotatedInSuper");
        TestMethodAnno first = ReflectionUtils.getMethodAnnotation(method, TestMethodAnno.class);
        TestMethodAnno second = ReflectionUtils.getMethodAnnotation(method, TestMethodAnno.class);
        assertSame(first, second, "The cached annotation instance should be returned on subsequent calls");
    }

    @Test
    void shouldThrowWhenMethodArgumentIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(null, TestMethodAnno.class));
        assertTrue(ex.getMessage().contains("method cannot be null"));
    }

    @Test
    void shouldThrowWhenAnnotationClassArgumentIsNull() throws NoSuchMethodException {
        Method method = SuperClass.class.getMethod("annotatedInSuper");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(method, null));
        assertTrue(ex.getMessage().contains("annotation class cannot be null"));
    }

    @Test
    void shouldReturnNullWhenAnnotationClassDoesNotMatch() throws NoSuchMethodException {
        Method method = SuperClass.class.getMethod("annotatedInSuper");
        AnotherAnno anno = ReflectionUtils.getMethodAnnotation(method, AnotherAnno.class);
        assertNull(anno, "Different annotation type should result in null");
    }

    // ---------- NullChecks ----------
    @Test
    void json_nullMethodThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(null, MyAnno.class));
        assertTrue(ex.getMessage().contains("method cannot be null"));
    }

    @Test
    void json_nullAnnoClassThrows() throws NoSuchMethodException {
        Method method = BaseClass.class.getDeclaredMethod("annotated", String.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(method, null));
        assertTrue(ex.getMessage().contains("annotation class cannot be null"));
    }

    // ---------- DirectAnnotation ----------
    @Test
    void json_directAnnotationFound() throws NoSuchMethodException {
        Method method = BaseClass.class.getDeclaredMethod("annotated", String.class);
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno, "Method is directly annotated with @MyAnno");
    }

    // ---------- SuperclassAnnotation ----------
    @Test
    void json_superclassAnnotationFound() throws NoSuchMethodException {
        Method method = SubClassForSuper.class.getDeclaredMethod("annotated", String.class);
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno, "Annotation should be found in the superclass");
    }

    // ---------- InterfaceAnnotation ----------
    @Test
    void json_interfaceAnnotationFound() throws NoSuchMethodException {
        Method method = ImplClass.class.getDeclaredMethod("interfaceMethod");
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNotNull(anno, "Annotation should be discovered on the interface method");
    }

    // ---------- NoAnnotation ----------
    @Test
    void json_noAnnotationReturnsNull() throws NoSuchMethodException {
        Method method = PlainClass.class.getDeclaredMethod("plainMethod", int.class);
        MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertNull(anno, "No @MyAnno present anywhere in the hierarchy");
    }

    // ---------- CacheReuse ----------
    @Test
    void json_cacheReuseReturnsSameInstance() throws NoSuchMethodException {
        Method method = BaseClass.class.getDeclaredMethod("annotated", String.class);
        MyAnno first = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        MyAnno second = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
        assertSame(first, second, "Subsequent calls should return the cached annotation instance");
    }

    // ---------- OverloadedMethodAnnotation ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("overloadedMethodProvider")
    void json_overloadedMethodAnnotation(String description, Method method, Class<? extends Annotation> annoClass, Class<? extends Annotation> expectedType) throws Exception {
        Annotation result = ReflectionUtils.getMethodAnnotation(method, annoClass);
        if (expectedType == null) {
            assertNull(result, description);
        } else {
            assertNotNull(result, description);
            assertTrue(expectedType.isInstance(result), description);
        }
    }

    // ---------- InvalidAnnotationClass ----------
    @Test
    void json_invalidAnnotationClassThrows() throws NoSuchMethodException {
        Method method = PlainClass.class.getDeclaredMethod("plainMethod", int.class);
        // Passing a non‑annotation type (String.class) should cause IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(method, (Class) String.class));
    }
}
