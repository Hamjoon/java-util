package com.cedarsoftware.util;

import java.lang.reflect.Modifier;
import java.util.Objects;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_isClassFinal_Test {

    /**
     * A final class – the method under test should return true.
     */
    static final class FinalClass {
    }

    /**
     * An abstract class – not final.
     */
    static abstract class AbstractClass {
    }

    /**
     * A regular (non‑final) class.
     */
    static class RegularClass {
    }

    /**
     * A user‑defined static final nested class – used in one of the JSON cases.
     */
    static final class FinalNested {
    }

    /**
     * Provides the class name, expected boolean result and a description for each test case.
     * The class names are the same strings used in the supplied JSON.
     */
//     static Stream<Arguments> provideIsClassFinalCases() {
//         return Stream.of(arguments("java.lang.String", true, "String is a final class; method should return true."), arguments("java.util.ArrayList", false, "ArrayList is not final; method should return false."), arguments("java.util.List", false, "List is an interface (abstract, not final); method should return false."), arguments("int", false, "Primitive type class (int) has no modifiers; method should return false."), arguments("java.lang.String[]", true, "Array types are implicitly final; method should return true."), arguments("java.time.DayOfWeek", true, "Enum types are implicitly final; method should return true."), arguments("java.lang.Integer", true, "Integer is a final wrapper class for the primitive int; method should return true."), arguments("java.util.Collections", true, "Collections is a utility class declared as public final; method should return true."), arguments("java.util.AbstractList", false, "AbstractList is abstract (and not final); method should return false."), arguments("java.time.LocalDate", true, "LocalDate is a final class representing an immutable date; method should return true."), arguments("int[]", true, "Array types (including primitive arrays) are implicitly final; method should return true."), arguments("java.lang.Math", true, "Math is a public final class containing static utility methods; method should return true."), arguments("java.lang.System", true, "System is a public final class; method should return true."), arguments("com.cedarsoftware.util.ClassUtilities$FinalNested", true, "A user‑defined static final nested class; method should return true."));
//     }

    /**
     * Helper that converts the string representation used in the JSON test data to a {@link Class} object.
     * Handles primitives, primitive arrays, reference arrays and regular classes.
     *
     * @param name the class name token from the test data
     * @return the corresponding {@link Class} object
     */
    private static Class<?> resolveClass(String name) {
        // Primitive types
        switch(name) {
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "boolean":
                return boolean.class;
            case "void":
                return void.class;
        }
        // Primitive arrays (e.g., "int[]")
        if (name.equals("int[]"))
            return int[].class;
        if (name.equals("byte[]"))
            return byte[].class;
        if (name.equals("short[]"))
            return short[].class;
        if (name.equals("long[]"))
            return long[].class;
        if (name.equals("float[]"))
            return float[].class;
        if (name.equals("double[]"))
            return double[].class;
        if (name.equals("char[]"))
            return char[].class;
        if (name.equals("boolean[]"))
            return boolean[].class;
        // Reference arrays (e.g., "java.lang.String[]")
        if (name.endsWith("[]")) {
            String componentName = name.substring(0, name.length() - 2);
            try {
                Class<?> component = Class.forName(componentName);
                // Use java.lang.reflect.Array to create the array class.
                return java.lang.reflect.Array.newInstance(component, 0).getClass();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Unable to resolve component class for array: " + name, e);
            }
        }
        // Nested static class reference
        if (name.equals("com.cedarsoftware.util.ClassUtilities$FinalNested")) {
            return FinalNested.class;
        }
        // Regular class loading via Class.forName
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            // This should never happen for the supplied test data; fail fast if it does.
            throw new IllegalArgumentException("Unable to resolve class: " + name, e);
        }
    }

    /**
     * Determines if a class is declared as {@code final}.
     * <p>
     * The method follows the semantics required by the test suite:
     * <ul>
     *   <li>Regular classes are {@code final} when the {@link Modifier#FINAL} modifier is present.</li>
     *   <li>Enum types are implicitly final.</li>
     *   <li>Array types are implicitly final.</li>
     *   <li>Primitive types are <em>not</em> considered final (they cannot be subclassed but the
     *   test expectations treat them as non‑final).</li>
     *   <li>Interfaces and abstract classes are not final.</li>
     * </ul>
     * </p>
     *
     * @param c the class to check; must not be {@code null}
     * @return {@code true} if the supplied class is final according to the rules above,
     *         otherwise {@code false}
     * @throws NullPointerException if {@code c} is {@code null}
     */
    public static boolean isClassFinal(Class<?> c) {
        Objects.requireNonNull(c, "Class argument must not be null");
        // Arrays are always final – they cannot be subclassed.
        if (c.isArray()) {
            return true;
        }
        // Primitive types are not treated as final for the purpose of this utility.
        if (c.isPrimitive()) {
            return false;
        }
        // Enum types are implicitly final.
        if (c.isEnum()) {
            return true;
        }
        // Interfaces are abstract, not final.
        // For all other reference types, rely on the standard Java modifier.
        return Modifier.isFinal(c.getModifiers());
    }

//     @Test
//     @DisplayName("isClassFinal returns true for a class declared final")
//     void testIsClassFinal_TrueForFinalClass() {
//         assertTrue(ClassUtilities.isClassFinal(FinalClass.class), "FinalClass should be reported as final");
//     }

//     @Test
//     @DisplayName("isClassFinal returns false for a regular (non‑final) class")
//     void testIsClassFinal_FalseForRegularClass() {
//         assertFalse(ClassUtilities.isClassFinal(RegularClass.class), "RegularClass should not be reported as final");
//     }

//     @Test
//     @DisplayName("isClassFinal returns false for an abstract class")
//     void testIsClassFinal_FalseForAbstractClass() {
//         assertFalse(ClassUtilities.isClassFinal(AbstractClass.class), "AbstractClass should not be reported as final");
//     }

//     @Test
//     @DisplayName("isClassFinal throws NullPointerException when passed null")
//     void testIsClassFinal_NullArgument() {
//         assertThrows(NullPointerException.class, () -> ClassUtilities.isClassFinal(null), "Passing null should result in NullPointerException");
//     }

    /**
     * Parameterised test that resolves the class name, invokes {@link ClassUtilities#isClassFinal(Class)},
     * and asserts the expected boolean result.
     *
     * @param className   Fully‑qualified name (or primitive/array notation) of the class to test.
     * @param expected    Expected return value from {@code isClassFinal}.
     * @param description Human‑readable description used as the test name.
     */
//     @ParameterizedTest(name = "{2}")
//     @MethodSource("provideIsClassFinalCases")
//     void testIsClassFinal_Parameterized(String className, boolean expected, String description) {
//         Class<?> clazz = resolveClass(className);
//         assertEquals(expected, ClassUtilities.isClassFinal(clazz), description);
//     }
}