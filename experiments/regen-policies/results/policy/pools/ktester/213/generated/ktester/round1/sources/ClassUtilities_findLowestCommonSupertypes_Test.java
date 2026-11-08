package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertypes_Test {

    // ----------------------------------------------------------------------
    interface TestInterface {
    }

    static class TestClass {
    }

    static class SubClass extends TestClass implements TestInterface {
    }

    /**
     * Normal (non‑exception) test cases.
     *
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>classA</li>
     *   <li>classB</li>
     *   <li>expected set of lowest common supertypes (empty set if none)</li>
     *   <li>description – used as the display name for the parameterized test</li>
     * </ul>
     */
    private static Stream<Arguments> normalCases() {
        return Stream.of(// Null Parameters – handled separately (exception cases)
        // Same Class
        Arguments.of(String.class, String.class, Set.of(String.class), "Same Class – both arguments are java.lang.String"), // Direct Superclass
        Arguments.of(Integer.class, Number.class, Set.of(Number.class), "Direct Superclass – Integer & Number, expect Number"), Arguments.of(ArrayList.class, LinkedList.class, Set.of(java.util.AbstractList.class), "Direct Superclass – ArrayList & LinkedList, expect AbstractList"), // Common Interface
        Arguments.of(HashMap.class, Hashtable.class, Set.of(java.util.Map.class), "Common Interface – HashMap & Hashtable, expect Map"), Arguments.of(Runnable.class, Thread.class, Set.of(Runnable.class), "Common Interface – Runnable & Thread, expect Runnable"), Arguments.of(java.util.List.class, java.util.Set.class, Set.of(java.util.Collection.class), "Common Interface – List & Set, expect Collection"), // Array Types
        Arguments.of(int[].class, Object[].class, Collections.emptySet(), "Array Types – int[] & Object[], expect empty set"), Arguments.of(String[].class, Object[].class, Collections.emptySet(), "Array Types – String[] & Object[], expect empty set"), // Primitive and Wrapper
        Arguments.of(int.class, Integer.class, Collections.emptySet(), "Primitive & Wrapper – int & Integer, expect empty set"), // Enum Types
        Arguments.of(Enum.class, Thread.State.class, Set.of(Enum.class), "Enum Types – Enum & Thread.State, expect Enum"), // Multiple Common Superinterfaces
        Arguments.of(ArrayList.class, HashSet.class, Set.of(java.util.Collection.class), "Multiple Common Superinterfaces – ArrayList & HashSet, expect Collection"), // Exclusion Only
        Arguments.of(Object.class, String.class, Collections.emptySet(), "Exclusion Only – Object & String, expect empty set"), Arguments.of(Serializable.class, String.class, Collections.emptySet(), "Exclusion Only – Serializable & String, expect empty set"), // Comparable Interface
        Arguments.of(Comparable.class, String.class, Set.of(Comparable.class), "Comparable Interface – Comparable & String, expect Comparable"), // Wrapper vs Wrapper
        Arguments.of(Integer.class, Long.class, Set.of(Number.class), "Wrapper vs Wrapper – Integer & Long, expect Number"), // Primitive vs Primitive
        Arguments.of(int.class, long.class, Collections.emptySet(), "Primitive vs Primitive – int & long, expect empty set"), // Abstract Class Common Supertype
        Arguments.of(HashMap.class, TreeMap.class, Set.of(java.util.AbstractMap.class), "Abstract Class Common Supertype – HashMap & TreeMap, expect AbstractMap"), // AbstractSet Common Supertype
        Arguments.of(HashSet.class, TreeSet.class, Set.of(java.util.AbstractSet.class), "AbstractSet Common Supertype – HashSet & TreeSet, expect AbstractSet"), // Interface Subtype
        Arguments.of(SortedSet.class, Set.class, Set.of(Set.class), "Interface Subtype – SortedSet & Set, expect Set"), // Interface vs Interface No Common
        Arguments.of(Runnable.class, java.util.concurrent.Callable.class, Collections.emptySet(), "Interface vs Interface No Common – Runnable & Callable, expect empty set"), // BlockingQueue vs ArrayDeque
        Arguments.of(BlockingQueue.class, ArrayDeque.class, Set.of(java.util.Queue.class), "BlockingQueue vs ArrayDeque – expect Queue"), // Multiple Lowest Common Supertypes
        Arguments.of(ArrayList.class, Vector.class, Set.of(java.util.List.class, java.util.RandomAccess.class), "Multiple Lowest Common Supertypes – ArrayList & Vector, expect List & RandomAccess"));
    }

    /**
     * Cases where one or both arguments are {@code null}. The method is expected
     * to throw a {@link NullPointerException}.
     */
    private static Stream<Arguments> nullParameterCases() {
        return Stream.of(Arguments.of(null, null, "Both arguments are null"), Arguments.of(null, String.class, "classA is null, classB is String"), Arguments.of(Integer.class, null, "classB is null, classA is Integer"));
    }

    /* ----------------------------------------------------------------------
     *  1) Simple common interface (java.util.List) between two concrete classes.
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Common interface should be returned after exclusions")
    void testCommonInterface() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(ArrayList.class, LinkedList.class);
        assertNotNull(result);
        // Object, Serializable, Cloneable are filtered out, leaving only List.
        assertEquals(1, result.size(), "Only java.util.List should remain after default exclusions");
        assertTrue(result.contains(java.util.List.class), "Result must contain java.util.List");
    }

    /* ----------------------------------------------------------------------
     *  2) No common supertype other than Object (which is excluded).
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("When only Object is common the result is empty")
    void testNoCommonSupertype() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Runnable.class, Error.class);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be empty because the only common supertype is Object, which is excluded");
    }

    /* ----------------------------------------------------------------------
     *  3) Common concrete superclass (Number) between wrapper types.
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Common superclass (Number) should be present")
    void testCommonSuperclass() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Double.class);
        assertNotNull(result);
        assertTrue(result.contains(Number.class), "Result should contain java.lang.Number");
    }

    /* ----------------------------------------------------------------------
     *  4) Interface implemented by a class and the interface itself.
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Interface and implementing class share the interface as common supertype")
    void testInterfaceAndClass() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(SubClass.class, TestInterface.class);
        assertNotNull(result);
        // The only non‑excluded common supertype is the interface itself.
        assertTrue(result.contains(TestInterface.class), "Result should contain the TestInterface type");
    }

    /* ----------------------------------------------------------------------
     *  5) Verify that Serializable, Cloneable and Externalizable are excluded.
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Excluded types are not present in the result")
    void testExclusions() {
        // String and StringBuilder share CharSequence, Comparable and Serializable.
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(String.class, StringBuilder.class);
        assertNotNull(result);
        assertTrue(result.contains(CharSequence.class), "Result should contain CharSequence");
        assertTrue(result.contains(Comparable.class), "Result should contain Comparable");
        assertFalse(result.contains(Serializable.class), "Serializable must be excluded by the default exclusion set");
        assertFalse(result.contains(Cloneable.class), "Cloneable must be excluded by the default exclusion set");
        assertFalse(result.contains(Object.class), "Object must be excluded by the default exclusion set");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("normalCases")
    void testFindLowestCommonSupertypes_normal(Class<?> classA, Class<?> classB, Set<Class<?>> expected, String description) {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(classA, classB);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("nullParameterCases")
    void testFindLowestCommonSupertypes_nullParameters(Class<?> classA, Class<?> classB, String description) {
        assertThrows(NullPointerException.class, () -> ClassUtilities.findLowestCommonSupertypes(classA, classB), description);
    }
}
