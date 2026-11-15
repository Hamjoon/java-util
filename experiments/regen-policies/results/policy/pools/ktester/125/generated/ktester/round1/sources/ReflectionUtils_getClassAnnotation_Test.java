package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassAnnotation_Test {

    @ControllerClass
    static class Foo {
    }

    static class Bar extends Foo {
    }

    @ControllerClass
    interface Baz {
    }

    interface Qux extends Baz {
    }

    static class Impl implements Qux {
    }

    static class NoAnno {
    }

    /**
     * Class directly annotated with a runtime annotation.
     */
    @RuntimeAnnotation
    static class DirectAnnotated {
    }

    /**
     * Superclass annotated; subclass is not.
     */
    @RuntimeAnnotation
    static class SuperAnnotated {
    }

    static class SubWithoutAnnotation extends SuperAnnotated {
    }

    /**
     * Interface annotated – should NOT be inherited by implementing classes.
     */
    @RuntimeAnnotation
    interface AnnotatedInterface {
    }

    /**
     * Interface extending an annotated interface – still should not be inherited.
     */
    interface ChildInterface extends AnnotatedInterface {
    }

    /**
     * Class implementing an interface that (indirectly) carries the annotation.
     */
    static class ImplementsAnnotatedInterface implements ChildInterface {
    }

    /**
     * Class with an annotation that has CLASS retention (invisible at runtime).
     */
    @ClassRetentionAnnotation
    static class ClassRetentionAnnotated {
    }

    /**
     * Class annotated with @Deprecated (runtime retained).
     */
    @Deprecated
    static class DeprecatedClass {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a fresh cache for every test to avoid cross‑test interference
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        // fresh mutable cache for every test
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache again – defensive, in case a test mutates it directly
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
        // defensive clean‑up
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
    }

    /* -------------------------------------------------------------
     *  Parameter provider for the "direct annotation" group
     * ------------------------------------------------------------- */
    static Stream<Arguments> directAnnotationProvider() {
        return Stream.of(// java.lang.Deprecated on a custom class
        Arguments.of(DeprecatedClass.class, Deprecated.class, true), // custom runtime annotation
        Arguments.of(DirectAnnotated.class, RuntimeAnnotation.class, true));
    }

    @Test
    void testNullClassReturnsNull() {
        // When the class to check is null the method should simply return null
        assertNull(ReflectionUtils.getClassAnnotation(null, ControllerClass.class));
    }

    @Test
    void testNullAnnotationClassThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getClassAnnotation(DirectAnnotated.class, null));
        assertEquals("annotation class cannot be null", ex.getMessage());
    }

    @Test
    void testDirectClassAnnotation() {
        // The annotation is present directly on the class
        ControllerClass anno = ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        assertNotNull(anno);
    }

    @Test
    void testSuperclassAnnotationIsInherited() {
        // Annotation on superclass should be found on subclass
        RuntimeAnnotation anno = ReflectionUtils.getClassAnnotation(SubWithoutAnnotation.class, RuntimeAnnotation.class);
        assertNotNull(anno, "Annotation should be inherited from the superclass");
    }

    @Test
    void testInterfaceAnnotationIsInheritedThroughInterfaceHierarchy() {
        // Baz is annotated; Qux extends Baz; Impl implements Qux
        ControllerClass anno = ReflectionUtils.getClassAnnotation(Impl.class, ControllerClass.class);
        assertNotNull(anno);
    }

    @Test
    void testNoAnnotationReturnsNull() {
        // No annotation on the class or its hierarchy
        assertNull(ReflectionUtils.getClassAnnotation(NoAnno.class, ControllerClass.class));
    }

    @Test
    void testCacheStoresAndReturnsSameInstance() {
        // Prepare a dedicated cache so we can inspect its behaviour
        Map<Object, Annotation> testCache = new HashMap<>();
        ReflectionUtils.setClassAnnotationCache(testCache);
        // First call – annotation is looked up and stored
        ControllerClass first = ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        assertNotNull(first);
        // Second call – should retrieve the same instance from the cache
        ControllerClass second = ReflectionUtils.getClassAnnotation(Foo.class, ControllerClass.class);
        assertSame(first, second, "Cached annotation instances should be identical");
        // Verify that the cache indeed contains a single entry for the key
        assertEquals(1, testCache.size(), "Cache should contain exactly one entry after two identical look‑ups");
    }

    @ParameterizedTest(name = "{index}: {0} with {1}")
    @MethodSource("directAnnotationProvider")
    void testDirectAnnotation(Class<?> clazz, Class<? extends Annotation> annoClass, boolean expectNonNull) {
        // When the annotation is present directly on the class we should receive a non‑null result.
        Annotation result = ReflectionUtils.getClassAnnotation(clazz, annoClass);
        if (expectNonNull) {
            assertNotNull(result, "Expected a non‑null annotation instance");
        } else {
            assertNull(result, "Expected null because the annotation is not retained at runtime");
        }
    }

    @Test
    void testInterfaceAnnotationIsNotInherited() {
        // Even though the interface hierarchy carries the annotation, implementing class should NOT see it.
        RuntimeAnnotation anno = ReflectionUtils.getClassAnnotation(ImplementsAnnotatedInterface.class, RuntimeAnnotation.class);
        assertNull(anno, "Annotations on interfaces are not inherited to implementing classes");
    }

    @Test
    void testClassRetentionAnnotationReturnsNull() {
        // Annotation with CLASS retention is not visible at runtime.
        Annotation anno = ReflectionUtils.getClassAnnotation(ClassRetentionAnnotated.class, ClassRetentionAnnotation.class);
        assertNull(anno, "CLASS‑retention annotations should not be returned at runtime");
    }

    @Test
    void testCacheNullHandling() {
        // Inject a null cache – any operation on it should cause NPE.
        ReflectionUtils.setClassAnnotationCache(null);
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassAnnotation(DirectAnnotated.class, RuntimeAnnotation.class));
    }

    @Test
    void testCacheTypeMismatchLeadsToClassCastException() {
        // Custom map that pretends to contain a mismatched annotation for the key.
        Map<Object, Annotation> mismatchedCache = new HashMap<>() {

            @Override
            public boolean containsKey(Object key) {
                // Pretend we have the key for any request
                return true;
            }

            @Override
            public Annotation get(Object key) {
                // Return a RuntimeAnnotation regardless of the requested type
                return DirectAnnotated.class.getAnnotation(RuntimeAnnotation.class);
            }
        };
        ReflectionUtils.setClassAnnotationCache(mismatchedCache);
        // The method will retrieve the RuntimeAnnotation and try to cast it to @Deprecated,
        // causing a ClassCastException.
        assertThrows(ClassCastException.class, () -> ReflectionUtils.getClassAnnotation(DirectAnnotated.class, Deprecated.class));
    }

    @Test
    void testCacheUnmodifiableMapThrowsUnsupportedOperationException() {
        // Unmodifiable map – computeIfAbsent will attempt to put a new entry and fail.
        Map<Object, Annotation> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        ReflectionUtils.setClassAnnotationCache(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> ReflectionUtils.getClassAnnotation(DirectAnnotated.class, RuntimeAnnotation.class));
    }
}
