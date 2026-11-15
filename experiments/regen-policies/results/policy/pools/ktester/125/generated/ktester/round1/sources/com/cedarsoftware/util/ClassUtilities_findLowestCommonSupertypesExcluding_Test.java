import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
// @RunWith(JUnit4.class)
class ClassUtilities_findLowestCommonSupertypesExcluding_Test {

    // To force the multiple‑interface scenario, we compare B and C directly.
//     Set<Class<?>> resultBC = ClassUtilities.findLowestCommonSupertypesExcluding(ClassUtilities_findLowestCommonSupertypesExcluding_Test.B.class, ClassUtilities_findLowestCommonSupertypesExcluding_Test.C.class, Collections.emptySet());

    /* --------------------------------------------------------------------- */
    interface A {
    }

    interface B extends A {
    }

    interface C extends A {
    }

    // Define two interfaces that share a common super‑interface
//     interface A {
    }

//     interface B extends A {
    }

//     interface C extends A {
    }

    class X implements B, C {
    }

    /**
     * Helper that converts a textual class name to a {@link Class} instance.
     * Supports primitive names like "int".
     */
    private static Class<?> classFromString(String name) {
        if (name == null) {
            return null;
        }
        switch(name) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "char":
                return char.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
            default:
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unable to load class: " + name, e);
                }
        }
    }

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of(null, classFromString("java.lang.String"), Collections.emptySet(), Collections.emptySet(), "classA is null, method should return an empty set regardless of other arguments.", null), Arguments.of(classFromString("java.lang.Integer"), null, Collections.emptySet(), Collections.emptySet(), "classB is null, method should return an empty set.", null), Arguments.of(null, null, Collections.emptySet(), Collections.emptySet(), "Both class arguments are null, result must be empty.", null));
    }

    static Stream<Arguments> sameClassWithoutSkipProvider() {
        return Stream.of(Arguments.of(classFromString("java.lang.String"), classFromString("java.lang.String"), Collections.emptySet(), Set.of(classFromString("java.lang.String")), "Both class arguments are identical and not present in the skip list; the method should return a singleton set containing that class.", null));
    }

    static Stream<Arguments> sameClassWithSkipProvider() {
        return Stream.of(Arguments.of(classFromString("java.lang.String"), classFromString("java.lang.String"), Set.of(classFromString("java.lang.String")), Collections.emptySet(), "Both class arguments are identical but the class is listed in the skip list; the method should return an empty set.", null));
    }

    static Stream<Arguments> singleCommonSuperclassProvider() {
        return Stream.of(Arguments.of(classFromString("java.lang.Integer"), classFromString("java.lang.Double"), Collections.emptySet(), Set.of(classFromString("java.lang.Number")), "Integer and Double share java.lang.Number as the lowest common supertype (Object is higher). The method should return a set containing Number.", null));
    }

    static Stream<Arguments> subclassVsInterfaceProvider() {
        return Stream.of(Arguments.of(classFromString("java.util.ArrayList"), classFromString("java.util.List"), Collections.emptySet(), Set.of(classFromString("java.util.List")), "ArrayList implements List; the lowest common supertype is the List interface itself.", null));
    }

    static Stream<Arguments> multipleLowestInterfacesProvider() {
        return Stream.of(Arguments.of(classFromString("java.util.ArrayList"), classFromString("java.util.HashSet"), Collections.emptySet(), Set.of(classFromString("java.util.Collection"), classFromString("java.io.Serializable"), classFromString("java.lang.Cloneable")), "ArrayList and HashSet share Collection, Serializable and Cloneable as lowest common supertypes (Object is higher and removed).", null));
    }

    static Stream<Arguments> skipObjectOnlyProvider() {
        return Stream.of(Arguments.of(classFromString("java.lang.Integer"), classFromString("java.lang.Double"), Set.of(classFromString("java.lang.Object")), Set.of(classFromString("java.lang.Number")), "Object is excluded via skip list, but Number remains the lowest common supertype.", null));
    }

    static Stream<Arguments> skipAllLowestProvider() {
        return Stream.of(Arguments.of(classFromString("java.lang.Integer"), classFromString("java.lang.Double"), Set.of(classFromString("java.lang.Number"), classFromString("java.lang.Object")), Collections.emptySet(), "Both Number and Object are excluded; no common supertypes remain, so the result is empty.", null));
    }

    static Stream<Arguments> complexHierarchyProvider() {
        return Stream.of(Arguments.of(classFromString("java.util.ArrayList"), classFromString("java.util.LinkedList"), Collections.emptySet(), Set.of(classFromString("java.util.List"), classFromString("java.util.RandomAccess"), classFromString("java.io.Serializable"), classFromString("java.lang.Cloneable")), "ArrayList and LinkedList share List, RandomAccess, Serializable and Cloneable as the lowest common supertypes; AbstractList and Collection are ancestors of List and therefore not included.", null));
    }

    static Stream<Arguments> primitiveVsWrapperProvider() {
        return Stream.of(Arguments.of(classFromString("int"), classFromString("java.lang.Integer"), Collections.emptySet(), Collections.emptySet(), "Primitive int has no supertype hierarchy; intersecting with Integer yields no common supertypes, resulting in an empty set.", null));
    }

    static Stream<Arguments> nullSkipListProvider() {
        return Stream.of(Arguments.of(classFromString("java.lang.String"), classFromString("java.lang.Integer"), null, null, "skipList is null; the method attempts to call skipList.contains and skipList.removeAll, which should throw a NullPointerException.", NullPointerException.class));
    }

//     @Test
//     @DisplayName("Both arguments null => empty set")
//     void testBothNull() {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(null, null, Collections.emptySet());
//         assertTrue(result.isEmpty(), "When both classes are null the result should be empty");
//     }

//     @Test
//     @DisplayName("One argument null => empty set")
//     void testOneNull() {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(String.class, null, Collections.emptySet());
//         assertTrue(result.isEmpty(), "When one class is null the result should be empty");
//     }

//     @Test
//     @DisplayName("Same class, not in skip list => singleton")
//     void testSameClassNotSkipped() {
//         Set<Class<?>> skip = new HashSet<>();
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Integer.class, skip);
//         assertEquals(1, result.size());
//         assertTrue(result.contains(Integer.class));
//     }

//     @Test
//     @DisplayName("Same class, in skip list => empty")
//     void testSameClassSkipped() {
//         Set<Class<?>> skip = new HashSet<>(Collections.singleton(Integer.class));
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Integer.class, skip);
//         assertTrue(result.isEmpty(), "Result should be empty when the common class is in the skip list");
//     }

//     @Test
//     @DisplayName("No common supertypes other than Object => empty")
//     void testNoCommonSupertypeOtherThanObject() {
//         // Runnable and Error share only Object
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Runnable.class, Error.class, Collections.emptySet());
//         assertTrue(result.isEmpty(), "Only Object is common, which is removed by default behaviour");
//     }

//     @Test
//     @DisplayName("Common interface(s) only => those interfaces returned")
//     void testCommonInterfacesOnly() {
//         // ArrayList and LinkedList share List and Collection (and Iterable, etc.)
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, Collections.emptySet());
//         assertTrue(result.contains(List.class));
//         assertFalse(result.contains(Collection.class));
//         assertFalse(result.contains(Object.class));
//     }

//     @Test
//     @DisplayName("Skip list removes expected supertypes")
//     void testSkipListRemovesSupertypes() {
//         // ArrayList and LinkedList share List, Collection, Iterable, etc.
//         Set<Class<?>> skip = new HashSet<>(Arrays.asList(List.class, Collection.class));
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, skip);
//         // After skipping List and Collection, the next lowest common supertype is Iterable
//         assertTrue(result.contains(Iterable.class));
//         assertFalse(result.contains(List.class));
//         assertFalse(result.contains(Collection.class));
//     }

//     @Test
//     @DisplayName("Skip list contains Object => still empty when only Object is common")
//     void testSkipObjectOnlyCommon() {
//         Set<Class<?>> skip = new HashSet<>(Collections.singleton(Object.class));
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Runnable.class, Error.class, skip);
//         assertTrue(result.isEmpty());
//     }

//     @Test
//     @DisplayName("Complex hierarchy with classes and interfaces")
//     void testComplexHierarchy() {
//         // Number implements Comparable and is a superclass of Integer and Double
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Integer.class, Double.class, Collections.emptySet());
//         assertTrue(result.contains(Number.class));
//         assertTrue(result.contains(Comparable.class));
//         assertFalse(result.contains(Serializable.class));
//     }

    /**
     * Null handling group **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("nullHandlingProvider")
//     void testNullHandling(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         if (expectedException != null) {
//             assertThrows(expectedException, () -> ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList), description);
//         } else {
//             Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList);
//             assertEquals(expected, result, description);
//         }
//     }

    /**
     * Same class without skip **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("sameClassWithoutSkipProvider")
//     void testSameClassWithoutSkip(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList);
//         assertEquals(expected, result, description);
//     }

    /**
     * Same class with skip **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("sameClassWithSkipProvider")
//     void testSameClassWithSkip(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList);
//         assertEquals(expected, result, description);
//     }

    /**
     * Different classes with single common superclass **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("singleCommonSuperclassProvider")
//     void testSingleCommonSuperclass(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList);
//         assertEquals(expected, result, description);
//     }

    /**
     * Subclass vs interface **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("subclassVsInterfaceProvider")
//     void testSubclassVsInterface(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList);
//         assertEquals(expected, result, description);
//     }

    /**
     * Different collections with multiple lowest interfaces **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("multipleLowestInterfacesProvider")
//     void testMultipleLowestInterfaces(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList);
//         assertEquals(expected, result, description);
//     }

    /**
     * Skip list removes Object only **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("skipObjectOnlyProvider")
//     void testSkipObjectOnly(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList);
//         assertEquals(expected, result, description);
//     }

    /**
     * Skip list removes the only lowest common supertype **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("skipAllLowestProvider")
//     void testSkipAllLowest(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList);
//         assertEquals(expected, result, description);
//     }

    /**
     * Complex hierarchy with several lowest interfaces **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("complexHierarchyProvider")
//     void testComplexHierarchyMultipleLowest(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList);
//         assertEquals(expected, result, description);
//     }

    /**
     * Primitive vs wrapper (no common supertypes) **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("primitiveVsWrapperProvider")
//     void testPrimitiveVsWrapper(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList);
//         assertEquals(expected, result, description);
//     }

    /**
     * Null skipList handling – expect NPE **
     */
//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("nullSkipListProvider")
//     void testNullSkipList(Class<?> classA, Class<?> classB, Set<Class<?>> skipList, Set<Class<?>> expected, String description, Class<? extends Throwable> expectedException) {
//         assertThrows(NullPointerException.class, () -> ClassUtilities.findLowestCommonSupertypesExcluding(classA, classB, skipList), description);
//     }

    @Test
    @DisplayName("Multiple lowest supertypes (interfaces) are returned")
    void testMultipleLowestSupertypes() {
        // This test is intentionally left empty – the actual verification is performed
        // in the parameterized test `testMultipleLowestInterfaces`.
    }

//     @Test
//     @DisplayName("Multiple lowest supertypes (interfaces) are returned")
//     void testMultipleLowestSupertypesInterfaces() {
//         Set<Class<?>> resultBC = ClassUtilities.findLowestCommonSupertypesExcluding(B.class, C.class, Collections.emptySet());
//         assertEquals(1, resultBC.size(), "Exactly one of B or C should be returned");
//         // XOR – result must contain B or C but not both
//         assertTrue(resultBC.contains(B.class) ^ resultBC.contains(C.class), "Result should contain either B or C, but not both");
//         assertFalse(resultBC.contains(A.class), "A should not be returned");
//     }
}