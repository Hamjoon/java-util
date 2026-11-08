package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_computeInheritanceDistance_Test {

    /**
     * Supplies a range of scenarios covering nulls, identity, primitives,
     *  interface implementation and unrelated classes.
     */
    private static Stream<Arguments> inheritanceDistanceProvider() {
        return Stream.of(// ------------------------------------------------------------------
        // Null handling
        // ------------------------------------------------------------------
        Arguments.of(null, Object.class, -1), Arguments.of(Object.class, null, -1), Arguments.of(null, null, -1), // ------------------------------------------------------------------
        // Identity (same class / same primitive)
        // ------------------------------------------------------------------
        Arguments.of(String.class, String.class, 0), Arguments.of(int.class, int.class, 0), Arguments.of(void.class, void.class, 0), // ------------------------------------------------------------------
        // Primitive‑primitive matching
        // ------------------------------------------------------------------
        Arguments.of(int.class, long.class, -1), Arguments.of(boolean.class, boolean.class, 0), // ------------------------------------------------------------------
        // Primitive‑wrapper mismatches (should be -1)
        // ------------------------------------------------------------------
        Arguments.of(int.class, Integer.class, -1), Arguments.of(Integer.class, int.class, -1), // ------------------------------------------------------------------
        // Simple class inheritance (single and multi‑step)
        // ------------------------------------------------------------------
        Arguments.of(ArrayList.class, AbstractList.class, 1), Arguments.of(AbstractList.class, AbstractCollection.class, 1), Arguments.of(ArrayList.class, AbstractCollection.class, 2), Arguments.of(Integer.class, Number.class, 1), Arguments.of(Integer.class, Object.class, 2), // ------------------------------------------------------------------
        // Interface implementation (direct)
        // ------------------------------------------------------------------
        Arguments.of(ArrayList.class, java.util.List.class, 1), Arguments.of(LinkedList.class, java.util.List.class, 1), Arguments.of(HashMap.class, java.util.Map.class, 1), // ------------------------------------------------------------------
        // Interface to super‑interface
        // ------------------------------------------------------------------
        Arguments.of(java.util.List.class, java.util.Collection.class, 1), // ------------------------------------------------------------------
        // Interface to Object (should be -1 – interfaces are not treated as having Object)
        // ------------------------------------------------------------------
        Arguments.of(java.util.List.class, Object.class, -1), // ------------------------------------------------------------------
        // Indirect interface implementation (multiple steps)
        // ------------------------------------------------------------------
        Arguments.of(java.util.HashSet.class, java.util.Collection.class, 2), // ------------------------------------------------------------------
        // Unrelated classes / interfaces
        // ------------------------------------------------------------------
        Arguments.of(String.class, java.util.Map.class, -1), Arguments.of(Runnable.class, Error.class, -1), Arguments.of(HashMap.class, java.util.Collection.class, -1), // ------------------------------------------------------------------
        // Array types (all arrays extend Object and implement Cloneable & Serializable)
        // ------------------------------------------------------------------
        Arguments.of(int[].class, Object.class, 1), Arguments.of(int[].class, Cloneable.class, 1), Arguments.of(int[].class, java.util.List.class, -1), // ------------------------------------------------------------------
        // Superclass of an interface implementer
        // ------------------------------------------------------------------
        Arguments.of(AbstractList.class, java.util.List.class, 1), // ------------------------------------------------------------------
        // ArrayList to Collection (indirect via List)
        // ------------------------------------------------------------------
        Arguments.of(ArrayList.class, java.util.Collection.class, 2), // ------------------------------------------------------------------
        // Object as destination of a subclass
        // ------------------------------------------------------------------
        Arguments.of(String.class, Object.class, 1));
    }

    /* ----------------------------------------------------------------------
     *  Parameterised tests for computeInheritanceDistance
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] source={0}, destination={1} => expected={2}")
    @MethodSource("inheritanceDistanceProvider")
    void testComputeInheritanceDistance(Class<?> source, Class<?> destination, int expected) {
        int actual = ClassUtilities.computeInheritanceDistance(source, destination);
        assertEquals(expected, actual, () -> String.format("Distance between %s and %s should be %d but was %d", source, destination, expected, actual));
    }

    /* ----------------------------------------------------------------------
     *  Additional explicit tests for edge‑cases not covered by the parameterised set
     * ---------------------------------------------------------------------- */
    @Test
    void testComputeInheritanceDistance_SamePrimitiveReturnsZero() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));
    }

    @Test
    void testComputeInheritanceDistance_DifferentPrimitivesReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @Test
    void testComputeInheritanceDistance_PrimitiveAndWrapperReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
    }

    @Test
    void testComputeInheritanceDistance_UnrelatedClassesReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, java.util.Map.class));
    }
}
