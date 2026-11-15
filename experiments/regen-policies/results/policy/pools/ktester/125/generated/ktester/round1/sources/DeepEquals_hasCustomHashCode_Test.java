package com.cedarsoftware.util;

// import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

class DeepEquals_hasCustomHashCode_Test {

    /**
     * Simple class that provides its own {@code hashCode()} implementation.
     */
    private static final class WithCustomHash {

        @Override
        public int hashCode() {
            return 12345;
        }
    }

    /**
     * Simple class that does **not** override {@code hashCode()}.
     */
    private static final class WithoutCustomHash {
        // inherits {@code Object#hashCode()}
    }

    private static Stream<Arguments> positiveCases() {
        return Stream.of(Arguments.of("Class directly overrides hashCode", WithCustomHash.class), Arguments.of("java.lang.String overrides hashCode", String.class), Arguments.of("java.util.ArrayList overrides hashCode", java.util.ArrayList.class), Arguments.of("java.util.Collections$UnmodifiableList overrides hashCode", java.util.Collections.unmodifiableList(java.util.Collections.emptyList()).getClass()), Arguments.of("java.time.LocalDate overrides hashCode", java.time.LocalDate.now().getClass()), Arguments.of("java.lang.Integer overrides hashCode", Integer.class), Arguments.of("java.util.concurrent.atomic.AtomicInteger overrides hashCode", java.util.concurrent.atomic.AtomicInteger.class), Arguments.of("java.lang.Enum base class overrides hashCode", java.lang.Enum.class));
    }

    private static Stream<Arguments> negativeCases() {
        return Stream.of(Arguments.of("java.lang.Object does not override hashCode", Object.class), Arguments.of("int[] array class", int[].class), Arguments.of("String[] array class", String[].class));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(Arguments.of("null Class reference", null, NullPointerException.class), Arguments.of("primitive type int.class", int.class, IllegalArgumentException.class));
    }

    @Test
    void hasCustomHashCode_returnsTrueWhenHashCodeIsOverridden() {
        Assertions.assertTrue(DeepEquals.hasCustomHashCode(WithCustomHash.class), "Expected true for a class that overrides hashCode()");
    }

    @Test
    void hasCustomHashCode_returnsFalseWhenHashCodeIsNotOverridden() {
        Assertions.assertFalse(DeepEquals.hasCustomHashCode(WithoutCustomHash.class), "Expected false for a class that does not override hashCode()");
    }

    @Test
    void hasCustomHashCode_returnsFalseForArrayClasses() {
        Assertions.assertFalse(DeepEquals.hasCustomHashCode(int[].class), "Expected false for primitive array class");
        Assertions.assertFalse(DeepEquals.hasCustomHashCode(String[].class), "Expected false for object array class");
    }

    @Test
    void hasCustomHashCode_throwsNullPointerExceptionWhenClassIsNull() {
        Assertions.assertThrows(NullPointerException.class, () -> DeepEquals.hasCustomHashCode(null));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("positiveCases")
    void hasCustomHashCode_returnsTrue(String description, Class<?> clazz) {
        Assertions.assertTrue(DeepEquals.hasCustomHashCode(clazz), () -> "Expected true for class: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("negativeCases")
    void hasCustomHashCode_returnsFalse(String description, Class<?> clazz) {
        Assertions.assertFalse(DeepEquals.hasCustomHashCode(clazz), () -> "Expected false for class: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCases")
    void hasCustomHashCode_throwsExpectedException(String description, Class<?> clazz, Class<? extends Throwable> expectedException) {
        Assertions.assertThrows(expectedException, () -> DeepEquals.hasCustomHashCode(clazz), () -> "Expected " + expectedException.getSimpleName() + " for: " + description);
    }
}
