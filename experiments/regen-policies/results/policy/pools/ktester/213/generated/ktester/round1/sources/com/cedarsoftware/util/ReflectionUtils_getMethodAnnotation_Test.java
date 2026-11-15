package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethodAnnotation_Test {

    /* ------------------------------------------------------------
     *  Helper classes / interfaces for the tests
     * ------------------------------------------------------------ */
    static class SuperClass {

//         @TestAnnotation("super")
//         public void annotatedMethod() {
//         }

        public void nonAnnotatedMethod() {
        }
    }

    static class SubClass extends SuperClass {

        @Override
        public void annotatedMethod() {
        }
    }

    interface InterfaceWithAnnotation {

//         @TestAnnotation("iface")
//         void ifaceMethod();
    }

    static class Impl implements InterfaceWithAnnotation {

        @Override
        public void ifaceMethod() {
        }
    }

    static class NoAnnotationClass {

        public void someMethod() {
        }
    }

    // -----------------------------------------------------------------
    static class Base {

//         @MyAnno("base")
//         public void annotated() {
//         }

        public void plain() {
        }
    }

    static class Derived extends Base {
        // inherits annotated() without overriding
    }

    interface InterfaceA {

//         @MyAnno("ifaceA")
//         void shared();
    }

    interface InterfaceB {

        // no annotation
        void shared();
    }

    static class MultiInterfaceImpl implements InterfaceA, InterfaceB {

        @Override
        public void shared() {
        }
    }

    static class SubClassWithOwnAnno extends Base {

//         @MyAnno("sub")
//         @Override
//         public void annotated() {
//         }
    }

    static class SubClassWithoutAnno extends Base {

        @Override
        public void annotated() {
        }
    }

    static class ParamsClass {

//         @MyAnno("params")
//         public void process(String s, int i) {
//         }
    }

    static class StaticAnnoClass {

//         @MyAnno("static")
//         public static void staticAnnotated() {
//         }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Replace the internal cache with a simple thread‑safe map so we can make
        // deterministic assertions about cache hits / misses.
        ReflectionUtils.setMethodAnnotationCache(new ConcurrentHashMap<>());
    }

    /* ------------------------------------------------------------
     *  Parameter provider for the null‑argument validation (optional, shows use of @ParameterizedTest)
     * ------------------------------------------------------------ */
//     static Stream<Arguments> nullArgumentProvider() {
//         return Stream.of(Arguments.of(null, MyAnno.class, IllegalArgumentException.class), Arguments.of(Base.class.getDeclaredMethods()[0], null, IllegalArgumentException.class));
//     }

//     @Test
//     void testDirectAnnotation() throws NoSuchMethodException {
//         Method method = SuperClass.class.getMethod("annotatedMethod");
//         TestAnnotation anno = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
//         assertNotNull(anno);
//         assertEquals("super", anno.value());
//     }

//     @Test
//     void testInheritedAnnotationFromSuperclass() throws NoSuchMethodException {
//         // The method is declared in SuperClass with the annotation,
//         // SubClass overrides it without the annotation.
//         Method method = SubClass.class.getMethod("annotatedMethod");
//         TestAnnotation anno = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
//         assertNotNull(anno);
//         assertEquals("super", anno.value(), "Annotation should be found on the superclass method");
//     }

//     @Test
//     void testAnnotationOnInterfaceMethod() throws NoSuchMethodException {
//         Method method = Impl.class.getMethod("ifaceMethod");
//         TestAnnotation anno = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
//         assertNotNull(anno);
//         assertEquals("iface", anno.value(), "Annotation should be discovered on the interface method");
//     }

//     @Test
//     void testNoAnnotationFoundReturnsNull() throws NoSuchMethodException {
//         Method method = SuperClass.class.getMethod("nonAnnotatedMethod");
//         TestAnnotation anno = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
//         assertNull(anno, "Method without annotation should return null");
//     }

//     @Test
//     void testMethodWithoutAnyAnnotationInHierarchyReturnsNull() throws NoSuchMethodException {
//         Method method = NoAnnotationClass.class.getMethod("someMethod");
//         TestAnnotation anno = ReflectionUtils.getMethodAnnotation(method, TestAnnotation.class);
//         assertNull(anno);
//     }

//     @Test
//     void testNullMethodThrowsException() {
//         assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(null, TestAnnotation.class));
//     }

    @Test
    void testNullAnnotationClassThrowsException() throws NoSuchMethodException {
        Method method = SuperClass.class.getMethod("annotatedMethod");
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(method, null));
    }

    /* ------------------------------------------------------------
     *  Direct annotation on the declaring class (JSON group: Annotation present on the declaring class)
     * ------------------------------------------------------------ */
//     @Test
//     void testDirectAnnotationFound() throws NoSuchMethodException {
//         Method method = Base.class.getDeclaredMethod("annotated");
//         MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
//         assertNotNull(anno);
//         assertEquals("base", anno.value());
//     }

    /* ------------------------------------------------------------
     *  Cache reuse – same instance on repeated calls (JSON group: Cache reuse (idempotent result))
     * ------------------------------------------------------------ */
//     @Test
//     void testCacheReuseReturnsSameInstance() throws NoSuchMethodException {
//         Method method = Base.class.getDeclaredMethod("annotated");
//         MyAnno first = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
//         MyAnno second = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
//         assertSame(first, second, "Repeated lookup should hit the cache and return the same annotation instance");
//     }

    /* ------------------------------------------------------------
     *  Overridden method with its own annotation (JSON group: Overridden method with its own annotation)
     * ------------------------------------------------------------ */
//     @Test
//     void testOverriddenMethodOwnAnnotation() throws NoSuchMethodException {
//         Method method = SubClassWithOwnAnno.class.getDeclaredMethod("annotated");
//         MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
//         assertNotNull(anno);
//         assertEquals("sub", anno.value(), "Annotation from the overriding method should be returned, not the superclass one");
//     }

    /* ------------------------------------------------------------
     *  Overridden method without annotation (inherits from superclass) (JSON group: Overridden method without annotation)
     * ------------------------------------------------------------ */
//     @Test
//     void testOverriddenMethodInheritsAnnotation() throws NoSuchMethodException {
//         Method method = SubClassWithoutAnno.class.getDeclaredMethod("annotated");
//         MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
//         assertNotNull(anno);
//         assertEquals("base", anno.value(), "When the overriding method lacks an annotation, the one from the superclass should be returned");
//     }

    /* ------------------------------------------------------------
     *  Method with parameters – annotation present (JSON group: Method with parameters – annotation present)
     * ------------------------------------------------------------ */
//     @Test
//     void testMethodWithParametersAnnotation() throws NoSuchMethodException {
//         Method method = ParamsClass.class.getDeclaredMethod("process", String.class, int.class);
//         MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
//         assertNotNull(anno);
//         assertEquals("params", anno.value());
//     }

    /* ------------------------------------------------------------
     *  Static method annotation (JSON group: Static method annotation)
     * ------------------------------------------------------------ */
//     @Test
//     void testStaticMethodAnnotation() throws NoSuchMethodException {
//         Method method = StaticAnnoClass.class.getDeclaredMethod("staticAnnotated");
//         MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
//         assertNotNull(anno);
//         assertEquals("static", anno.value());
//     }

    /* ------------------------------------------------------------
     *  Multiple interfaces – only one annotates the method (JSON group: Multiple interfaces – annotation on one interface only)
     * ------------------------------------------------------------ */
//     @Test
//     void testMultipleInterfacesOneAnnotated() throws NoSuchMethodException {
//         Method method = MultiInterfaceImpl.class.getMethod("shared");
//         MyAnno anno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
//         assertNotNull(anno);
//         assertEquals("ifaceA", anno.value(), "Annotation should be found on InterfaceA and not be confused by InterfaceB");
//     }

    /* ------------------------------------------------------------
     *  Non‑annotation class supplied as annoClass (JSON group: Non‑annotation class supplied as annoClass)
     * ------------------------------------------------------------ */
    @Test
    void testNonAnnotationClassReturnsNull() throws NoSuchMethodException {
        Method method = Base.class.getDeclaredMethod("annotated");
        // String.class is not an annotation type; getAnnotation will return null.
        Object result = ReflectionUtils.getMethodAnnotation(method, (Class) String.class);
        assertNull(result, "Supplying a non‑annotation class should result in null, not an exception");
    }

    /* ------------------------------------------------------------
     *  Cache distinguishes different annotation types for same method (JSON group: Cache distinguishes different annotation types for same method)
     * ------------------------------------------------------------ */
//     @Test
//     void testCacheKeyIncludesAnnotationClass() throws NoSuchMethodException {
//         Method method = Base.class.getDeclaredMethod("annotated");
//         // First lookup caches @MyAnno
//         MyAnno myAnno = ReflectionUtils.getMethodAnnotation(method, MyAnno.class);
//         assertNotNull(myAnno);
//         assertEquals("base", myAnno.value());
//         // Second lookup asks for a *different* annotation type.
//         OtherAnno otherAnno = ReflectionUtils.getMethodAnnotation(method, OtherAnno.class);
//         // The method is not annotated with @OtherAnno, so we expect null.
//         assertNull(otherAnno, "A different annotation type must not retrieve the cached @MyAnno instance");
//     }

//     @Test
//     void testNullMethodThrowsExceptionDuplicate() {
//         assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(null, MyAnno.class));
//     }

    @Test
    void testNullAnnotationClassThrowsExceptionDuplicate() throws NoSuchMethodException {
        Method method = SuperClass.class.getMethod("annotatedMethod");
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethodAnnotation(method, null));
    }

    @ParameterizedTest
    @MethodSource("nullArgumentProvider")
    void testNullArgumentValidationParameterized(Method method, Class<?> annoClass, Class<? extends Exception> expected) {
        assertThrows(expected, () -> ReflectionUtils.getMethodAnnotation(method, (Class) annoClass));
    }
}