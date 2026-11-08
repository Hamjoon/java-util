package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.util.stream.Stream;

class DeepEquals_hasCustomEquals_Test {

    /**
     * A class that does NOT override {@code equals(Object)}.
     */
    static class NoEquals {

        int x;

        NoEquals(int x) {
            this.x = x;
        }
    }

    /**
     * A class that overrides {@code equals(Object)}.
     */
    static class WithEquals {

        int x;

        WithEquals(int x) {
            this.x = x;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof WithEquals))
                return false;
            return x == ((WithEquals) o).x;
        }
    }

    /**
     * Subclass of {@link WithEquals} that does NOT override {@code equals}.
     */
    static class SubWithEquals extends WithEquals {

        SubWithEquals(int x) {
            super(x);
        }
    }

    /**
     * Directly overrides {@code Object.equals(Object)}.
     */
    static class OverridesEquals {

        @Override
        public boolean equals(Object o) {
            return true;
        }
    }

    /**
     * Subclass of {@link OverridesEquals} without its own {@code equals}.
     */
    static class SubClassOverrides extends OverridesEquals {
    }

    /**
     * No custom {@code equals} at all (inherits from {@code Object}).
     */
    static class NoOverride {
    }

    /**
     * Defines an overload {@code equals(String)} – not the required signature.
     */
    static class OverloadedEquals {

        public boolean equals(String s) {
            return false;
        }
    }

    /**
     * Interface that declares {@code boolean equals(Object)}.
     */
    interface EqualsInterface {

        boolean equals(Object o);
    }

    /**
     * Interface without an {@code equals} declaration.
     */
    interface NoEqualsInterface {
    }

    /**
     * Class with a static {@code equals(Object)} method.
     */
    static class StaticEquals {

        public static boolean equals(Object o) {
            return false;
        }
    }

    /**
     * Class with a **private** {@code equals(Object)} method.
     */
    static class PrivateEquals {

        @SuppressWarnings("unused")
        private boolean equals(Object o) {
            return false;
        }
    }

    /**
     * Provider for the “Override detection” group.
     */
    static Stream<Arguments> overrideDetectionProvider() {
        return Stream.of(// class directly overrides equals
        Arguments.of(OverridesEquals.class, true, "Class directly overrides Object.equals(Object)"), // subclass inherits the override
        Arguments.of(SubClassOverrides.class, true, "Subclass inherits custom equals from superclass"), // no override at all
        Arguments.of(NoOverride.class, false, "Class does not declare or inherit a custom equals"), // overload only – should be false
        Arguments.of(OverloadedEquals.class, false, "Class defines equals with wrong signature"), // null class – expect NPE
        Arguments.of(null, NullPointerException.class, "Null Class reference should propagate NPE"));
    }

    /**
     * Provider for the “Wrapper and primitive types” group.
     */
    static Stream<Arguments> wrapperAndPrimitiveProvider() {
        return Stream.of(Arguments.of(Integer.class, true, "java.lang.Integer overrides equals"), Arguments.of(int.class, false, "Primitive int has no custom equals"));
    }

    /**
     * Provider for the “Array type handling” group.
     */
    static Stream<Arguments> arrayTypeProvider() {
        return Stream.of(Arguments.of(int[].class, false, "int[] array class has no custom equals"), Arguments.of(String[].class, false, "String[] array class has no custom equals"));
    }

    /**
     * Provider for the “Interface equals declaration” group.
     */
    static Stream<Arguments> interfaceEqualsProvider() {
        return Stream.of(Arguments.of(EqualsInterface.class, true, "Interface declaring equals(Object) is considered custom"));
    }

    /**
     * Provider for the “Static equals method detection” group.
     */
    static Stream<Arguments> staticEqualsProvider() {
        return Stream.of(Arguments.of(StaticEquals.class, true, "Static equals(Object) still satisfies getDeclaredMethod"));
    }

    /**
     * Provider for the “Private equals detection” group.
     */
    static Stream<Arguments> privateEqualsProvider() {
        return Stream.of(Arguments.of(PrivateEquals.class, true, "Private equals(Object) is discovered via reflection"));
    }

    /**
     * Provider for the “Caching behavior verification” group.
     */
    static Stream<Arguments> cachingBehaviorProvider() {
        return Stream.of(Arguments.of(OverridesEquals.class, true, "Repeated calls should hit cache and return same result"));
    }

    /**
     * Provider for the “Interface without equals exception” group.
     */
    static Stream<Arguments> interfaceWithoutEqualsProvider() {
        return Stream.of(Arguments.of(NoEqualsInterface.class, NullPointerException.class, "Interface without equals leads to NPE when loop reaches null superclass"));
    }

    @Test
    void returnsFalseWhenClassHasNoCustomEquals() {
        assertFalse(DeepEquals.hasCustomEquals(NoEquals.class), "Class without an overridden equals() should return false");
    }

    @Test
    void returnsTrueWhenClassOverridesEquals() {
        assertTrue(DeepEquals.hasCustomEquals(WithEquals.class), "Class that overrides equals() should return true");
    }

    @Test
    void returnsTrueForSubclassWhenSuperclassOverridesEquals() {
        // Subclass does not declare its own equals, but its superclass does.
        assertTrue(DeepEquals.hasCustomEquals(SubWithEquals.class), "Subclass should inherit the custom equals detection from its superclass");
    }

    @Test
    void returnsFalseForObjectClass() {
        // java.lang.Object does not declare its own equals (it inherits from itself).
        assertFalse(DeepEquals.hasCustomEquals(Object.class), "Object class should return false because it does not have a custom equals");
    }

    @Test
    void cachingDoesNotChangeResult() {
        // First call populates the cache.
        boolean first = DeepEquals.hasCustomEquals(WithEquals.class);
        // Second call should hit the cache but return the same value.
        boolean second = DeepEquals.hasCustomEquals(WithEquals.class);
        assertEquals(first, second, "Repeated calls must return the same result (cached value)");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("overrideDetectionProvider")
    void testOverrideDetection(Class<?> clazz, Object expected, String description) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected exception
            assertThrows((Class<? extends Throwable>) expected, () -> DeepEquals.hasCustomEquals(clazz), description);
        } else {
            boolean result = DeepEquals.hasCustomEquals(clazz);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("wrapperAndPrimitiveProvider")
    void testWrapperAndPrimitiveTypes(Class<?> clazz, boolean expected, String description) {
        assertEquals(expected, DeepEquals.hasCustomEquals(clazz), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("arrayTypeProvider")
    void testArrayTypes(Class<?> clazz, boolean expected, String description) {
        assertEquals(expected, DeepEquals.hasCustomEquals(clazz), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("interfaceEqualsProvider")
    void testInterfaceEqualsDeclaration(Class<?> clazz, boolean expected, String description) {
        assertEquals(expected, DeepEquals.hasCustomEquals(clazz), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("staticEqualsProvider")
    void testStaticEqualsDetection(Class<?> clazz, boolean expected, String description) {
        assertEquals(expected, DeepEquals.hasCustomEquals(clazz), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("privateEqualsProvider")
    void testPrivateEqualsDetection(Class<?> clazz, boolean expected, String description) {
        assertEquals(expected, DeepEquals.hasCustomEquals(clazz), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("cachingBehaviorProvider")
    void testCachingBehavior(Class<?> clazz, boolean expected, String description) {
        // First invocation – populates cache
        boolean first = DeepEquals.hasCustomEquals(clazz);
        // Second invocation – should read from cache
        boolean second = DeepEquals.hasCustomEquals(clazz);
        assertEquals(first, second, "Cache must not alter the result");
        assertEquals(expected, first, description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("interfaceWithoutEqualsProvider")
    void testInterfaceWithoutEqualsException(Class<?> clazz, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> DeepEquals.hasCustomEquals(clazz), description);
    }

    /**
     * Additional sanity check for Object.class (already covered but kept for completeness).
     */
    @Test
    void testObjectClassIsFalse() {
        assertFalse(DeepEquals.hasCustomEquals(Object.class), "Object.class should not be considered to have a custom equals");
    }
}
