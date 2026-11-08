package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DeepEquals_hasCustomEquals_Test {

    /**
     * A class that provides its own {@code equals(Object)} implementation.
     */
    static class WithCustomEquals {

        private final int id;

        WithCustomEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof WithCustomEquals))
                return false;
            return id == ((WithCustomEquals) o).id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * A class that does **not** override {@code equals(Object)} – it inherits the
     * implementation from {@code java.lang.Object}.
     */
    static class WithoutCustomEquals {

        private final String name;

        WithoutCustomEquals(String name) {
            this.name = name;
        }
    }

    /**
     * A subclass that does not override {@code equals(Object)} itself but inherits
     * the custom implementation from its parent.
     */
    static class SubclassInheritingCustomEquals extends WithCustomEquals {

        SubclassInheritingCustomEquals(int id) {
            super(id);
        }
    }

    /**
     * Simple POJO without a custom {@code equals} implementation.
     */
    static class SimplePojo {

        private final int value = 42;
    }

    /**
     * POJO that overrides {@code equals(Object)}.
     */
    static class CustomEqualsPojo {

        private final int id;

        CustomEqualsPojo(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomEqualsPojo))
                return false;
            return id == ((CustomEqualsPojo) o).id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * Subclass that inherits the custom {@code equals} from its superclass.
     */
    static class InheritedEqualsPojo extends CustomEqualsPojo {

        InheritedEqualsPojo(int id) {
            super(id);
        }
    }

    static Stream<Arguments> provideDetectionCases() {
        return Stream.of(// Object itself – no custom equals
        Arguments.of(Object.class, false, "Object class does not declare its own equals method; should return false."), // Simple POJO – inherits Object.equals
        Arguments.of(SimplePojo.class, false, "SimplePojo does not override equals; should return false."), // POJO with its own equals
        Arguments.of(CustomEqualsPojo.class, true, "CustomEqualsPojo provides its own equals; should return true."), // Subclass inheriting the custom equals from its parent
        Arguments.of(InheritedEqualsPojo.class, true, "InheritedEqualsPojo inherits custom equals; should return true."), // Standard library wrapper that overrides equals
        Arguments.of(Integer.class, true, "Integer overrides equals; should return true."));
    }

    static Stream<Arguments> providePrimitiveClasses() {
        return Stream.of(Arguments.of(int.class), Arguments.of(double.class), Arguments.of(boolean.class), Arguments.of(void.class));
    }

    static Stream<Arguments> provideArrayClasses() {
        return Stream.of(Arguments.of(int[].class), Arguments.of(String[].class));
    }

    @Test
    void hasCustomEquals_returnsTrueWhenClassOverridesEquals() {
        assertTrue(DeepEquals.hasCustomEquals(WithCustomEquals.class), "Class that overrides equals should be reported as having a custom equals");
    }

    @Test
    void hasCustomEquals_returnsFalseWhenClassDoesNotOverrideEquals() {
        assertFalse(DeepEquals.hasCustomEquals(WithoutCustomEquals.class), "Class that does not override equals should be reported as not having a custom equals");
    }

    @Test
    void hasCustomEquals_returnsTrueWhenSuperclassProvidesCustomEquals() {
        assertTrue(DeepEquals.hasCustomEquals(SubclassInheritingCustomEquals.class), "Subclass inheriting a custom equals from its superclass should be reported as having a custom equals");
    }

    @Test
    void hasCustomEquals_returnsTrueForArrayClass() {
        // Arrays do not have an equals method declared on the array class itself;
        // the method should therefore return false (declaring class is Object).
        assertFalse(DeepEquals.hasCustomEquals(int[].class), "Array class should not be considered to have a custom equals implementation");
    }

    @Test
    void hasCustomEquals_returnsTrueForEnum() {
        // Enums inherit equals from java.lang.Enum which overrides Object.equals.
        assertTrue(DeepEquals.hasCustomEquals(TestEnum.class), "Enum types have a custom equals implementation in java.lang.Enum");
    }

    /**
     * Verifies that {@code DeepEquals.hasCustomEquals} returns the expected boolean
     * for a variety of classes that either do or do not declare their own {@code equals}
     * method.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideDetectionCases")
    void hasCustomEquals_detection(Class<?> clazz, boolean expected, String description) {
        assertEquals(expected, DeepEquals.hasCustomEquals(clazz), description);
    }

    /**
     * Primitive {@code Class} objects do not have methods; {@code ReflectionUtils.getMethod}
     * throws {@link IllegalArgumentException}. The test confirms that the exception
     * propagates unchanged.
     */
    @ParameterizedTest(name = "[{index}] primitive {0} throws IllegalArgumentException")
    @MethodSource("providePrimitiveClasses")
    void hasCustomEquals_primitiveThrows(Class<?> primitiveClass) {
        assertThrows(IllegalArgumentException.class, () -> DeepEquals.hasCustomEquals(primitiveClass), "Primitive class " + primitiveClass + " should cause IllegalArgumentException");
    }

    @Test
    void hasCustomEquals_nullArgumentThrows() {
        assertThrows(NullPointerException.class, () -> DeepEquals.hasCustomEquals(null), "Passing null should result in NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] array class {0} returns false")
    @MethodSource("provideArrayClasses")
    void hasCustomEquals_arrayClassesReturnFalse(Class<?> arrayClass) {
        assertFalse(DeepEquals.hasCustomEquals(arrayClass), "Array class " + arrayClass.getName() + " should not be considered to have a custom equals");
    }
}
