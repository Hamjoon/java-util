package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

// retained from the template; not used but harmless
@ExtendWith(MockitoExtension.class)
class DeepEquals_hasCustomHashCode_Test {

    /**
     * A simple class that does **not** override {@code hashCode}.
     */
    private static final class NoCustomHashCode {
        // No hashCode implementation – inherits from Object
    }

    /**
     * A simple class that **does** override {@code hashCode}.
     */
    private static final class WithCustomHashCode {

        @Override
        public int hashCode() {
            // arbitrary constant – the actual value is irrelevant for the test
            return 42;
        }
    }

    /**
     * A class that overrides {@code equals} but **not** {@code hashCode}.
     */
    private static final class ClassWithCustomEqualsOnly {

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
        // No hashCode override
    }

    /**
     * Alias for the positive‑case class used in the JSON spec.
     */
    private static final class ClassWithCustomHashCode {

        @Override
        public int hashCode() {
            return 7;
        }
    }

    /**
     * Alias for the negative‑case class used in the JSON spec.
     */
    private static final class ClassWithoutCustomHashCode {
        // No hashCode override
    }

    private static Stream<Arguments> positiveHashCodeProvider() {
        return Stream.of(Arguments.of("Class overrides hashCode()", ClassWithCustomHashCode.class, true), Arguments.of("java.lang.Integer overrides hashCode()", Integer.class, true));
    }

    private static Stream<Arguments> negativeHashCodeProvider() {
        return Stream.of(Arguments.of("java.lang.Object (no custom hashCode)", Object.class, false), Arguments.of("User‑defined class without custom hashCode", ClassWithoutCustomHashCode.class, false), Arguments.of("Class overrides equals only", ClassWithCustomEqualsOnly.class, false), Arguments.of("java.lang.Enum base class", Enum.class, false), Arguments.of("int[] array class", int[].class, false));
    }

    private static Stream<Arguments> exceptionHashCodeProvider() {
        return Stream.of(Arguments.of("null Class reference", null, NullPointerException.class), Arguments.of("primitive int type", int.class, IllegalArgumentException.class), Arguments.of("void type", void.class, IllegalArgumentException.class));
    }

    @Test
    void hasCustomHashCode_returnsFalseWhenClassDoesNotOverrideHashCode() {
        // GIVEN – a class that inherits Object.hashCode()
        Class<?> clazz = NoCustomHashCode.class;
        // WHEN
        boolean result = DeepEquals.hasCustomHashCode(clazz);
        // THEN
        assertFalse(result, "Expected hasCustomHashCode to be false for a class that does not override hashCode");
    }

    @Test
    void hasCustomHashCode_returnsTrueWhenClassOverridesHashCode() {
        // GIVEN – a class that provides its own hashCode implementation
        Class<?> clazz = WithCustomHashCode.class;
        // WHEN
        boolean result = DeepEquals.hasCustomHashCode(clazz);
        // THEN
        assertTrue(result, "Expected hasCustomHashCode to be true for a class that overrides hashCode");
    }

    @Test
    void hasCustomHashCode_returnsFalseForObjectClassItself() {
        // GIVEN – the java.lang.Object class
        Class<?> clazz = Object.class;
        // WHEN
        boolean result = DeepEquals.hasCustomHashCode(clazz);
        // THEN
        assertFalse(result, "Expected hasCustomHashCode to be false for java.lang.Object itself");
    }

    /**
     * Positive cases – classes that *do* have a custom hashCode implementation.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("positiveHashCodeProvider")
    void hasCustomHashCode_positiveCases(String description, Class<?> clazz, boolean expected) {
        boolean actual = DeepEquals.hasCustomHashCode(clazz);
        assertEquals(expected, actual, () -> "Unexpected result for " + description + " (class: " + clazz.getName() + ")");
    }

    /**
     * Negative cases – classes that *do not* have a custom hashCode implementation.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("negativeHashCodeProvider")
    void hasCustomHashCode_negativeCases(String description, Class<?> clazz, boolean expected) {
        boolean actual = DeepEquals.hasCustomHashCode(clazz);
        assertEquals(expected, actual, () -> "Unexpected result for " + description + " (class: " + clazz.getName() + ")");
    }

    /**
     * Exception handling – verify that illegal inputs raise the documented exceptions.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionHashCodeProvider")
    void hasCustomHashCode_exceptionCases(String description, Class<?> clazz, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DeepEquals.hasCustomHashCode(clazz), () -> "Expected " + expectedException.getSimpleName() + " for " + description);
    }
}
