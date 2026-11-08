package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassAnnotation_Test {

    @SampleAnno
    private static class DirectAnnotated {
    }

    private static class SubClassAnnotated extends DirectAnnotated {
    }

    @SampleAnno
    private interface AnnotatedInterface {
    }

    private static class ImplementsAnnotatedInterface implements AnnotatedInterface {
    }

    private static class NoAnnotation {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Replace the internal cache with a fresh, empty map so each test starts clean.
        // The utility class provides a setter for this purpose.
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared after each test as a safety net.
        ReflectionUtils.setClassAnnotationCache(new HashMap<>());
    }

    private static Stream<Arguments> nullClassProvider() {
        return Stream.of(Arguments.of(Deprecated.class), Arguments.of(SampleAnno.class));
    }

    private static Stream<Arguments> directAnnotationProvider() {
        return Stream.of(Arguments.of(DirectAnnotated.class, SampleAnno.class));
    }

    private static Stream<Arguments> absentAnnotationProvider() {
        return Stream.of(Arguments.of(NoAnnotation.class, SampleAnno.class));
    }

    private static Stream<Arguments> inheritedAnnotationProvider() {
        return Stream.of(Arguments.of(SubClassAnnotated.class, SampleAnno.class));
    }

    @Test
    void testNullClassReturnsNull() {
        // When the class to inspect is null the method should simply return null.
        assertNull(ReflectionUtils.getClassAnnotation(null, SampleAnno.class));
    }

    @Test
    void testNullAnnotationClassThrowsException() {
        // The utility validates the annotation class argument via Convention.throwIfNull.
        // We expect an IllegalArgumentException (or NullPointerException) – the exact type
        // is not part of the public contract, so we assert that *any* exception is thrown.
        assertThrows(RuntimeException.class, () -> ReflectionUtils.getClassAnnotation(DirectAnnotated.class, null));
    }

    @Test
    void testDirectAnnotationFound() {
        SampleAnno anno = ReflectionUtils.getClassAnnotation(DirectAnnotated.class, SampleAnno.class);
        assertNotNull(anno, "Annotation should be found on the class itself");
    }

    @Test
    void testInheritedAnnotationFound() {
        // Because @SampleAnno is @Inherited, a subclass should inherit it.
        SampleAnno anno = ReflectionUtils.getClassAnnotation(SubClassAnnotated.class, SampleAnno.class);
        assertNotNull(anno, "Annotation should be inherited from the superclass");
    }

    @Test
    void testInterfaceAnnotationNotFound() {
        // Annotations on interfaces are *not* inherited by implementing classes.
        SampleAnno anno = ReflectionUtils.getClassAnnotation(ImplementsAnnotatedInterface.class, SampleAnno.class);
        assertNull(anno, "Annotation on an interface should not be visible on the implementing class");
    }

    @Test
    void testCacheReturnsSameInstance() {
        // First call populates the cache.
        SampleAnno first = ReflectionUtils.getClassAnnotation(DirectAnnotated.class, SampleAnno.class);
        // Second call should retrieve the *same* annotation instance from the cache.
        SampleAnno second = ReflectionUtils.getClassAnnotation(DirectAnnotated.class, SampleAnno.class);
        assertSame(first, second, "Subsequent look‑ups must return the cached annotation instance");
    }

    @Test
    void testCacheStoresNullForMissingAnnotation() {
        // First call stores a null entry for a class without the annotation.
        SampleAnno first = ReflectionUtils.getClassAnnotation(NoAnnotation.class, SampleAnno.class);
        assertNull(first);
        // Second call should also return null (cached) and not trigger a new lookup.
        SampleAnno second = ReflectionUtils.getClassAnnotation(NoAnnotation.class, SampleAnno.class);
        assertNull(second);
    }

    @Test
    void testCacheIsNullLeadsToNPE() {
        // Replace the cache with a literal null – the next call should NPE inside computeIfAbsent.
        ReflectionUtils.setClassAnnotationCache(null);
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassAnnotation(DirectAnnotated.class, SampleAnno.class));
    }

    @Test
    void testAnnoClassNotAnnotationThrowsAnnotationFormatError() {
        // Passing a non‑annotation type should cause an AnnotationFormatError to be thrown.
        assertThrows(AnnotationFormatError.class, () -> ReflectionUtils.getClassAnnotation(DirectAnnotated.class, String.class));
    }

    /**
     * Group: Null classToCheck – verifies that any annotation class (including unrelated ones)
     * returns null when the target class is null.
     */
    @ParameterizedTest(name = "[{index}] classToCheck=null, annoClass={1}")
    @MethodSource("nullClassProvider")
    void testNullClassParameterized(Class<?> annoClass) {
        assertNull(ReflectionUtils.getClassAnnotation(null, annoClass));
    }

    /**
     * Group: Annotation present on class – direct annotation lookup.
     */
    @ParameterizedTest(name = "[{index}] class={0}, annoClass={1}")
    @MethodSource("directAnnotationProvider")
    void testDirectAnnotationParameterized(Class<?> clazz, Class<? extends Annotation> annoClass) {
        Annotation anno = ReflectionUtils.getClassAnnotation(clazz, annoClass);
        assertNotNull(anno, "Expected annotation to be present on the class");
    }

    /**
     * Group: Annotation absent returns null – class without the requested annotation.
     */
    @ParameterizedTest(name = "[{index}] class={0}, annoClass={1}")
    @MethodSource("absentAnnotationProvider")
    void testAbsentAnnotationParameterized(Class<?> clazz, Class<? extends Annotation> annoClass) {
        Annotation anno = ReflectionUtils.getClassAnnotation(clazz, annoClass);
        assertNull(anno, "Expected null when annotation is not present");
    }

    /**
     * Group: Annotation inherited from superclass – verifies that @Inherited works.
     */
    @ParameterizedTest(name = "[{index}] subclass={0}, annoClass={1}")
    @MethodSource("inheritedAnnotationProvider")
    void testInheritedAnnotationParameterized(Class<?> subclass, Class<? extends Annotation> annoClass) {
        Annotation anno = ReflectionUtils.getClassAnnotation(subclass, annoClass);
        assertNotNull(anno, "Expected inherited annotation to be found");
    }

    /**
     * Group: annoClass null throws exception – expects IllegalArgumentException.
     */
    @Test
    void testAnnoClassNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getClassAnnotation(DirectAnnotated.class, null));
    }

    /**
     * Group: Cache returns same instance on repeated calls – already covered by a dedicated test,
     * but we also verify via a parameterised approach for completeness.
     */
    @Test
    void testCacheSameInstanceRepeatedCalls() {
        SampleAnno first = ReflectionUtils.getClassAnnotation(DirectAnnotated.class, SampleAnno.class);
        SampleAnno second = ReflectionUtils.getClassAnnotation(DirectAnnotated.class, SampleAnno.class);
        assertSame(first, second, "Cache should return the exact same annotation instance");
    }
}
