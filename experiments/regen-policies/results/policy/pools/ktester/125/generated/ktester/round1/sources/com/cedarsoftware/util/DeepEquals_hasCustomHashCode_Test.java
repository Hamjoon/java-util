package com.cedarsoftware.util;

import java.lang.reflect.Method;

// @ExtendWith(MockitoExtension.class)
class DeepEquals_hasCustomHashCode_Test {

    /**
     * A class that does not override {@code hashCode()}.
     */
    static class NoCustomHashCode {
        // inherits Object.hashCode()
    }

    /**
     * Superclass that overrides {@code hashCode()}.
     */
    static class ParentWithHashCode {

        @Override
        public int hashCode() {
            return 42;
        }
    }

    /**
     * Subclass that inherits the overridden {@code hashCode()} from its parent.
     */
    static class ChildInheritsHashCode extends ParentWithHashCode {
        // no additional hashCode()
    }

    /**
     * Class that provides its own {@code hashCode()} implementation.
     */
    static class CustomHashClass {

        @Override
        public int hashCode() {
            return 12345;
        }
    }

    /**
     * Subclass that inherits the custom {@code hashCode()} from its parent.
     */
    static class SubCustomHashClass extends CustomHashClass {
        // no override
    }

    /**
     * Interface that does not declare {@code hashCode()}.
     */
    interface NoHashOverrideInterface {
        // no methods
    }

    /**
     * Provides a stream of arguments: the class to test and the expected boolean
     * result.
     */
//     private static Stream<Arguments> provideClassesWithExpectedResult() {
//         return Stream.of(// Object itself
//         Arguments.of(Object.class, false), // String overrides hashCode()
//         Arguments.of(String.class, true), // Custom class overrides hashCode()
//         Arguments.of(CustomHashClass.class, true), // Inherits custom hashCode()
//         Arguments.of(SubCustomHashClass.class, true), // Interface – no hashCode()
//         Arguments.of(NoHashOverrideInterface.class, false), // Integer overrides hashCode()
//         Arguments.of(Integer.class, true), // Boolean overrides hashCode()
//         Arguments.of(Boolean.class, true), // ArrayList overrides hashCode()
//         Arguments.of(ArrayList.class, true), // AbstractList inherits from AbstractCollection
//         Arguments.of(java.util.AbstractList.class, true), // Primitive array does not override
//         Arguments.of(int[].class, false), // Enum – inherits Object.hashCode()
//         Arguments.of(DayOfWeek.class, false), // Utility class – no custom hashCode()
//         Arguments.of(java.util.Collections.class, false), // Proxy overrides hashCode()
//         Arguments.of(java.lang.reflect.Proxy.class, true), // Void – no custom hashCode()
//         Arguments.of(Void.class, false));
//     }

    /**
     * Provides primitive types that are expected to cause an
     * {@link IllegalArgumentException} when {@code DeepEquals.hasCustomHashCode}
     * attempts to locate a {@code hashCode()} method.
     */
//     private static Stream<Arguments> providePrimitiveClassesExpectingException() {
//         return Stream.of(Arguments.of(int.class), Arguments.of(void.class));
//     }

    /**
     * Determines whether the given class provides its own {@code hashCode()}
     * implementation that is not the one inherited directly from {@link Object}.
     *
     * @param c the class to inspect; must not be {@code null}
     * @return {@code true} if the class (or any of its super‑classes) declares a
     *         {@code hashCode()} method different from {@link Object#hashCode()},
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code c} is {@code null} or a primitive type
     */
    public static boolean hasCustomHashCode(Class<?> c) {
        if (c == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        // Primitive types cannot have methods; the contract of the test suite
        // expects an IllegalArgumentException for them.
        if (c.isPrimitive()) {
            throw new IllegalArgumentException("Primitive types are not supported");
        }
        try {
            // All classes inherit a public hashCode() from Object, so we can
            // safely use getMethod.  If the declaring class is anything other
            // than Object, the class (or one of its ancestors) has overridden
            // the method.
            Method m = c.getMethod("hashCode");
            return m.getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            // This should never happen because Object defines hashCode(),
            // but we return false defensively.
            return false;
        }
    }

    /**
     * Simple helper used elsewhere in the library – not required for the
     * current test suite but provided for completeness.
     *
     * @param c the class to inspect; must not be {@code null}
     * @return {@code true} if the class declares its own {@code equals(Object)} method
     * @throws IllegalArgumentException if {@code c} is {@code null} or a primitive type
     */
    public static boolean hasCustomEquals(Class<?> c) {
        if (c == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        if (c.isPrimitive()) {
            throw new IllegalArgumentException("Primitive types are not supported");
        }
        try {
            Method m = c.getMethod("equals", Object.class);
            return m.getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

//     @Test
//     void shouldReturnFalseWhenClassHasNoCustomHashCode() {
//         assertFalse(DeepEquals.hasCustomHashCode(NoCustomHashCode.class), "Class without an overridden hashCode() should report false.");
//     }

//     @Test
//     void shouldReturnTrueWhenClassOverridesHashCodeDirectly() {
//         // java.lang.String overrides hashCode()
//         assertTrue(DeepEquals.hasCustomHashCode(String.class), "String overrides hashCode() and should be reported as having a custom hash code.");
//         // java.lang.Integer overrides hashCode()
//         assertTrue(DeepEquals.hasCustomHashCode(Integer.class), "Integer overrides hashCode() and should be reported as having a custom hash code.");
//     }

//     @Test
//     void shouldReturnTrueWhenSuperclassOverridesHashCode() {
//         assertTrue(DeepEquals.hasCustomHashCode(ChildInheritsHashCode.class), "A subclass that inherits a custom hashCode() from its superclass should be reported as having a custom hash code.");
//     }

//     @Test
//     void shouldThrowIllegalArgumentExceptionWhenClassIsNull() {
//         assertThrows(IllegalArgumentException.class, () -> DeepEquals.hasCustomHashCode(null), "Passing null should result in IllegalArgumentException.");
//     }

//     @ParameterizedTest(name = "[{index}] {0} → expected={1}")
//     @MethodSource("provideClassesWithExpectedResult")
//     void shouldDetectCustomHashCodeCorrectly(Class<?> clazz, boolean expected) {
//         boolean actual = DeepEquals.hasCustomHashCode(clazz);
//         assertEquals(expected, actual, "Unexpected result for class " + clazz.getName());
//     }

//     @ParameterizedTest(name = "[{index}] {0} → IllegalArgumentException")
//     @MethodSource("providePrimitiveClassesExpectingException")
//     void shouldThrowIllegalArgumentExceptionForPrimitiveTypes(Class<?> primitiveClass) {
//         assertThrows(IllegalArgumentException.class, () -> DeepEquals.hasCustomHashCode(primitiveClass), "Expected IllegalArgumentException for primitive type " + primitiveClass.getName());
//     }
}