package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.annotation.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassAnnotation_Test {

    @ControllerClass
    static class Foo {
    }

    static class Bar extends Foo {
    }

    @ControllerClass
    static interface Baz {
    }

    static interface Qux extends Baz {
    }

    static class Beta implements Qux {
    }

    static class Alpha extends Beta {
    }

    static interface Blart {
    }

    static class Bogus implements Blart {
    }

    @InheritedAnno
    static class BaseInherited {
    }

    static class SubClass extends BaseInherited {
    }

    @NonInheritedAnno
    static class BaseNonInherited {
    }

    static class SubClass2 extends BaseNonInherited {
    }

    @MyAnno
    @FirstAnno
    @SecondAnno
    static class AnnotatedClass {
    }

    static class NonAnnotatedClass {
    }

    static class SomeClass {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a fresh cache for each test so they are independent
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any custom SecurityManager that a test may have installed
        System.setSecurityManager(null);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset the cache to a clean state after the whole suite
        ReflectionUtils.setClassAnnotationCache(new ConcurrentHashMap<>());
    }

    /**
     * Provides data for the “null classToCheck” group – already covered by a dedicated test,
     * but kept here to illustrate the parameterized‑test pattern.
     */
    static Stream<Arguments> nullClassProvider() {
        return Stream.of(Arguments.of(null, MyAnno.class));
    }

    /**
     * Provides data for the “null annoClass” group.
     */
    static Stream<Arguments> nullAnnoProvider() {
        return Stream.of(Arguments.of(Foo.class, null));
    }

    @Test
    void nullClassReturnsNull() {
        assertNull(ReflectionUtils.getClassAnnotation(null, ControllerClass.class));
    }

    @Test
    void nullAnnotationClassThrows() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getClassAnnotation(Foo.class, null));
    }

    @Test
    void directlyAnnotatedClassReturnsAnnotation() {
        ControllerClass ann = ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        assertNotNull(ann);
        // The annotation instance should be of the correct type
        assertTrue(ann instanceof ControllerClass);
    }

    @Test
    void subclassInheritsAnnotation() {
        // @Inherited works for class hierarchy
        ControllerClass annSuper = ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        ControllerClass annSub = ReflectionUtils.getClassAnnotation(Bar.class, ControllerClass.class);
        assertNotNull(annSub);
        // The cached instance for the same key should be the same object
        assertSame(annSuper, annSub);
    }

    @Test
    void interfaceAnnotationIsNotInheritedByImplementingClass() {
        // Even though the interface is annotated, the implementing class should NOT inherit it
        ControllerClass ann = ReflectionUtils.getClassAnnotation(Beta.class, ControllerClass.class);
        assertNull(ann);
    }

    @Test
    void annotationCacheReturnsSameInstance() {
        ControllerClass first = ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        ControllerClass second = ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        assertSame(first, second, "Cache should return the same annotation instance");
    }

    @Test
    void cacheStoresNullWhenAnnotationMissing() {
        // Class without the annotation – first call stores a null entry
        assertNull(ReflectionUtils.getClassAnnotation(Bogus.class, ControllerClass.class));
        // Second call should also return null (cached) and not cause a new lookup
        assertNull(ReflectionUtils.getClassAnnotation(Bogus.class, ControllerClass.class));
    }

    @Test
    void inheritedAnnotationFromSuperclass() {
        // SubClass does not declare @InheritedAnno directly, but its superclass does.
        InheritedAnno ann = ReflectionUtils.getClassAnnotation(SubClass.class, InheritedAnno.class);
        assertNotNull(ann);
        assertTrue(ann instanceof InheritedAnno);
    }

    @Test
    void inheritedAnnotationIsCached() {
        InheritedAnno first = ReflectionUtils.getClassAnnotation(SubClass.class, InheritedAnno.class);
        InheritedAnno second = ReflectionUtils.getClassAnnotation(SubClass.class, InheritedAnno.class);
        assertSame(first, second, "Cached inherited annotation should be the same instance");
    }

    @Test
    void nonInheritedAnnotationFromSuperclassReturnsNull() {
        // BaseNonInherited is annotated, but the annotation is NOT @Inherited.
        NonInheritedAnno ann = ReflectionUtils.getClassAnnotation(SubClass2.class, NonInheritedAnno.class);
        assertNull(ann);
    }

    @Test
    void nonInheritedAnnotationCacheStoresNull() {
        assertNull(ReflectionUtils.getClassAnnotation(SubClass2.class, NonInheritedAnno.class));
        assertNull(ReflectionUtils.getClassAnnotation(SubClass2.class, NonInheritedAnno.class));
    }

    @Test
    void differentAnnotationTypesForSameClass() {
        FirstAnno first = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, FirstAnno.class);
        SecondAnno second = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, SecondAnno.class);
        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second, "Different annotation types must produce different cache entries");
    }

    @Test
    void sameAnnotationTypeReturnsSameCachedInstance() {
        FirstAnno firstCall = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, FirstAnno.class);
        FirstAnno secondCall = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, FirstAnno.class);
        assertSame(firstCall, secondCall, "Repeated lookup for the same annotation must return the cached instance");
    }

    @Test
    void primitiveClassHandlingReturnsNull() {
        // Primitive types cannot carry annotations.
        MyAnno ann = ReflectionUtils.getClassAnnotation(int.class, MyAnno.class);
        assertNull(ann);
    }

    @Test
    void arrayClassHandlingReturnsNull() {
        // Array types do not have class‑level annotations.
        MyAnno ann = ReflectionUtils.getClassAnnotation(SomeClass[].class, MyAnno.class);
        assertNull(ann);
    }

    @Test
    void metaAnnotationLookup() {
        // java.lang.annotation.Retention is itself annotated with @Documented and @Retention(RetentionPolicy.RUNTIME)
        Documented meta = ReflectionUtils.getClassAnnotation(Retention.class, Documented.class);
        assertNotNull(meta);
        assertTrue(meta instanceof Documented);
    }

    @Test
    void metaAnnotationIsCached() {
        Documented first = ReflectionUtils.getClassAnnotation(Retention.class, Documented.class);
        Documented second = ReflectionUtils.getClassAnnotation(Retention.class, Documented.class);
        assertSame(first, second, "Meta‑annotation should be cached");
    }

    @Test
    void annoClassNotAnAnnotationTypeThrows() {
        // Passing a non‑annotation class (String) must result in IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getClassAnnotation(AnnotatedClass.class, String.class));
    }

    @Test
    void securityManagerDeniesAnnotationAccess() {
        // Install a SecurityManager that denies reflective access to annotations.
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except reflective access to annotations.
                if (perm instanceof ReflectPermission) {
                    throw new SecurityException("Access denied by test SecurityManager");
                }
            }
        });
        try {
            assertThrows(SecurityException.class, () -> ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class));
        } finally {
            // Restore original SecurityManager to avoid side effects on other tests.
            System.setSecurityManager(original);
        }
    }

    @Test
    void cacheMapIsNullThrowsNullPointerException() {
        // Explicitly set the internal cache to null.
        ReflectionUtils.setClassAnnotationCache(null);
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassAnnotation(AnnotatedClass.class, MyAnno.class));
    }

    @ParameterizedTest
    @MethodSource("nullClassProvider")
    void parameterizedNullClassReturnsNull(Class<?> clazz, Class<? extends Annotation> anno) {
        assertNull(ReflectionUtils.getClassAnnotation(clazz, anno));
    }

    @ParameterizedTest
    @MethodSource("nullAnnoProvider")
    void parameterizedNullAnnoThrows(Class<?> clazz, Class<? extends Annotation> anno) {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getClassAnnotation(clazz, anno));
    }
}
