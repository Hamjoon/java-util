package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Set;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertype_Test {

    /**
     * Supplies a stream of arguments:
     * <ul>
     *   <li>Two classes that share a clear, single most‑specific supertype.</li>
     *   <li>Classes that share multiple supertypes – we verify assignability.</li>
     *   <li>Cases where the only common supertype is {@code Object} – method should return {@code null}.</li>
     *   <li>Null arguments – method should return {@code null}.</li>
     *   <li>Identical classes – method should return that class.</li>
     * </ul>
     */
    private static Stream<Arguments> provideClassPairs() {
        return Stream.of(// -----------------------------------------------------------------
        // 1. Common superclass between concrete classes
        // -----------------------------------------------------------------
        Arguments.of(Integer.class, Double.class, Number.class), Arguments.of(java.util.ArrayList.class, java.util.LinkedList.class, java.util.List.class), // -----------------------------------------------------------------
        // 2. Same class supplied for both parameters
        // -----------------------------------------------------------------
        Arguments.of(String.class, String.class, String.class), // -----------------------------------------------------------------
        // 3. Common supertype is Object (no more specific relationship)
        // -----------------------------------------------------------------
        Arguments.of(Thread.class, java.util.Random.class, Object.class), // -----------------------------------------------------------------
        // 4. Primitive types – no common reference supertype
        // -----------------------------------------------------------------
        Arguments.of(int.class, double.class, null), // -----------------------------------------------------------------
        // 5. Null arguments handling
        // -----------------------------------------------------------------
        Arguments.of(null, String.class, null), Arguments.of(String.class, null, null), Arguments.of(null, null, null), // -----------------------------------------------------------------
        // 6. Array types with common component supertype
        // -----------------------------------------------------------------
        Arguments.of(String[].class, Object[].class, Object[].class), Arguments.of(int[].class, Object[].class, null), // -----------------------------------------------------------------
        // 7. Interface vs class relationship
        // -----------------------------------------------------------------
        Arguments.of(java.util.HashMap.class, java.util.Map.class, java.util.Map.class), // -----------------------------------------------------------------
        // 8. Superclass relationship between class and its direct superclass
        // -----------------------------------------------------------------
        Arguments.of(java.util.ArrayList.class, java.util.AbstractList.class, java.util.AbstractList.class), // -----------------------------------------------------------------
        // 9. Additional explicit edge cases
        // -----------------------------------------------------------------
        // Both arguments are Object – should return Object
        Arguments.of(Object.class, Object.class, Object.class), // Primitive and its wrapper – method may return either side
        // we will verify with a dedicated test
        Arguments.of(int.class, Integer.class, null));
    }

    /* -----------------------------------------------------------------
     *  Parameterised tests for findLowestCommonSupertype
     * ----------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} & {1} ⇒ expected assignable to {2}")
    @MethodSource("provideClassPairs")
    void testFindLowestCommonSupertype(Class<?> a, Class<?> b, Class<?> expectedAssignable) {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(a, b);
        if (expected == null) {
            // Expected null when there is no common supertype (or when any argument is null)
            assertNull(result, () -> "Expected null for inputs " + a + " and " + b + " but got " + result);
        } else {
            assertNotNull(result, () -> "Expected a non‑null result for inputs " + a + " and " + b);
            // For Object we expect an exact match; for all other cases the result must be
            // assignable to the expected type (the method may return any of the common supertypes).
            if (expected == Object.class) {
                assertEquals(Object.class, result, () -> "Result should be exactly Object.class for inputs " + a + " and " + b);
            } else {
                assertTrue(expected.isAssignableFrom(result), () -> "Result " + result + " is not assignable to expected " + expected);
            }
        }
    }

    @Test
    @DisplayName("When both arguments are Object, the result is Object")
    void testBothObject() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(Object.class, Object.class);
        assertEquals(Object.class, result);
    }

    @Test
    @DisplayName("When one argument is a primitive wrapper and the other its primitive, they are considered common")
    void testPrimitiveAndWrapper() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(int.class, Integer.class);
        // The implementation treats primitive and wrapper as interchangeable,
        // so the common supertype can be either int.class or Integer.class.
        assertTrue(result == int.class || result == Integer.class, "Result should be int.class or Integer.class but was " + result);
    }
}
