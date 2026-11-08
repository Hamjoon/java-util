package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.Objects;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_haveCommonAncestor_Test {

    interface Base {
    }

    interface Derived extends Base {
    }

    interface First {
    }

    interface Second {
    }

    /**
     * Supplies the positive test data.
     */
//     private static Stream<org.junit.jupiter.params.provider.Arguments> positiveCases() {
//         return Stream.of(// Both concrete classes – lowest common supertype is Object
//         org.junit.jupiter.params.provider.Arguments.of(String.class, StringBuilder.class), // List implementations share AbstractList (via List)
//         org.junit.jupiter.params.provider.Arguments.of(ArrayList.class, LinkedList.class), // Map implementations share AbstractMap (via Map)
//         org.junit.jupiter.params.provider.Arguments.of(java.util.HashMap.class, java.util.TreeMap.class), // Concrete class vs. interface it implements
//         org.junit.jupiter.params.provider.Arguments.of(ArrayList.class, java.util.List.class), // Unrelated interfaces – they still share Object
//         org.junit.jupiter.params.provider.Arguments.of(Runnable.class, java.util.concurrent.Callable.class), // Identical classes
//         org.junit.jupiter.params.provider.Arguments.of(String.class, String.class), // Array types – share Object (and Cloneable, Serializable)
//         org.junit.jupiter.params.provider.Arguments.of(int[].class, Object[].class));
//     }

    /**
     * Supplies the negative test data involving primitives and reference types.
     */
//     private static Stream<org.junit.jupiter.params.provider.Arguments> negativePrimitiveCases() {
//         return Stream.of(// Primitive int vs. reference String
//         org.junit.jupiter.params.provider.Arguments.of(int.class, String.class), // Primitive double vs. interface List
//         org.junit.jupiter.params.provider.Arguments.of(double.class, java.util.List.class), // Two different primitives
//         org.junit.jupiter.params.provider.Arguments.of(int.class, double.class));
//     }

    /**
     * Returns {@code true} if the two supplied classes share a common ancestor
     * other than {@link Object}. Primitive types are considered to have a common
     * ancestor with their wrapper classes (e.g. {@code int} and {@code Integer}).
     *
     * @param classA the first class; may be {@code null}
     * @param classB the second class; may be {@code null}
     * @return {@code true} if a non‑Object common ancestor exists, otherwise {@code false}
     */
//     public static boolean haveCommonAncestor(Class<?> classA, Class<?> classB) {
//         if (classA == null || classB == null) {
//             return false;
//         }
//         // Find the set of the most specific common supertypes.
//         Set<Class<?>> commons = findLowestCommonSupertypesExcluding(classA, classB, Collections.emptySet());
//         if (commons.isEmpty()) {
//             return false;
//         }
//         // If the only common type is Object we treat it as “no common ancestor”.
//         return !(commons.size() == 1 && commons.contains(Object.class));
//     }

    /**
     * Returns the set of the lowest (most specific) common supertypes of {@code classA}
     * and {@code classB}, excluding any types supplied in {@code skip}.
     *
     * @param classA   the first class; may be {@code null}
     * @param classB   the second class; may be {@code null}
     * @param skipList a set of types to exclude from the result; never {@code null}
     * @return the set of lowest common supertypes, possibly empty
     */
//     public static Set<Class<?>> findLowestCommonSupertypesExcluding(Class<?> classA, Class<?> classB, Set<Class<?>> skipList) {
//         if (classA == null || classB == null) {
//             return Collections.emptySet();
//         }
//         // All supertypes (including the class itself)
//         Set<Class<?>> aSupers = getAllSupertypes(classA);
//         aSupers.add(classA);
//         Set<Class<?>> bSupers = getAllSupertypes(classB);
//         bSupers.add(classB);
//         // Intersection = common supertypes
//         Set<Class<?>> common = new HashSet<>(aSupers);
//         common.retainAll(bSupers);
//         common.removeAll(skipList);
//         // Keep only the “lowest” ones – those that are not a supertype of any other element in the set.
//         Set<Class<?>> lowest = new HashSet<>(common);
//         for (Class<?> c : common) {
//             for (Class<?> other : common) {
//                 if (c != other && c.isAssignableFrom(other)) {
//                     lowest.remove(c);
//                     break;
//                 }
//             }
//         }
//         return lowest;
//     }

    /**
     * Returns every super‑class and interface of the supplied class.
     *
     * @param clazz the class whose hierarchy is inspected
     * @return a set containing all super‑classes and interfaces
     */
//     private static Set<Class<?>> getAllSupertypes(Class<?> clazz) {
//         Set<Class<?>> result = new HashSet<>();
//         // Super‑classes
//         Class<?> sup = clazz.getSuperclass();
//         while (sup != null) {
//             result.add(sup);
//             sup = sup.getSuperclass();
//         }
//         // Interfaces (including those inherited from super‑classes)
//         collectInterfaces(clazz, result);
//         return result;
//     }

    /**
     * Recursively adds all interfaces implemented by {@code clazz} (and by its
     * super‑classes) to {@code set}.
     */
//     private static void collectInterfaces(Class<?> clazz, Set<Class<?>> set) {
//         for (Class<?> i : clazz.getInterfaces()) {
//             if (set.add(i)) {
//                 collectInterfaces(i, set);
//             }
//         }
//         Class<?> sup = clazz.getSuperclass();
//         if (sup != null) {
//             collectInterfaces(sup, set);
//         }
//     }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to examine; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        Objects.requireNonNull(method, "method");
        return method.getReturnType().equals(void.class);
    }

    /**
     * Finds a method with the given name and parameter types on the supplied class
     * (including its super‑classes).  If the method cannot be found {@code null}
     * is returned.
     *
     * @param clazz          the class to search; never {@code null}
     * @param name           the method name; never {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(parameterTypes, "parameterTypes");
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method m = current.getDeclaredMethod(name, parameterTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // continue searching up the hierarchy
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if a method with the given signature exists on the
     * supplied class (or any of its super‑classes).
     *
     * @param clazz          the class to inspect; never {@code null}
     * @param name           the method name; never {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return {@code true} if such a method is present
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

//     @Test
//     @DisplayName("haveCommonAncestor returns true for List implementations")
//     void testHaveCommonAncestor_ListImplementations() {
//         assertTrue(ClassUtilities.haveCommonAncestor(LinkedList.class, ArrayList.class), "LinkedList and ArrayList share java.util.List");
//     }

//     @Test
//     @DisplayName("haveCommonAncestor returns true for Number subclasses")
//     void testHaveCommonAncestor_NumberSubclasses() {
//         assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class), "Integer and Double share java.lang.Number");
//     }

//     @Test
//     @DisplayName("haveCommonAncestor returns true for interface inheritance")
//     void testHaveCommonAncestor_InterfaceHierarchy() {
//         interface Base {
//         }
//         interface Derived extends Base {
//         }
//         assertTrue(ClassUtilities.haveCommonAncestor(Derived.class, Base.class), "Derived extends Base, so they share Base as a common ancestor");
//     }

//     @Test
//     @DisplayName("haveCommonAncestor returns true when both arguments are the same class")
//     void testHaveCommonAncestor_SameClass() {
//         assertTrue(ClassUtilities.haveCommonAncestor(String.class, String.class), "The same class should always have a common ancestor");
//     }

//     @Test
//     @DisplayName("haveCommonAncestor returns false for unrelated classes")
//     void testHaveCommonAncestor_UnrelatedClasses() {
//         assertFalse(ClassUtilities.haveCommonAncestor(Runnable.class, Error.class), "Runnable and Error share only java.lang.Object");
//     }

//     @Test
//     @DisplayName("haveCommonAncestor returns false for unrelated interfaces")
//     void testHaveCommonAncestor_UnrelatedInterfaces() {
//         interface First {
//         }
//         interface Second {
//         }
//         assertFalse(ClassUtilities.haveCommonAncestor(First.class, Second.class), "Two unrelated interfaces share only Object");
//     }

//     @Test
//     @DisplayName("haveCommonAncestor returns false when first argument is null")
//     void testHaveCommonAncestor_NullFirst() {
//         assertFalse(ClassUtilities.haveCommonAncestor(null, String.class), "Null as first argument should yield false");
//     }

//     @Test
//     @DisplayName("haveCommonAncestor returns false when second argument is null")
//     void testHaveCommonAncestor_NullSecond() {
//         assertFalse(ClassUtilities.haveCommonAncestor(String.class, null), "Null as second argument should yield false");
//     }

//     @Test
//     @DisplayName("haveCommonAncestor returns false when both arguments are null")
//     void testHaveCommonAncestor_BothNull() {
//         assertFalse(ClassUtilities.haveCommonAncestor(null, null), "Both arguments null should yield false");
//     }

//     @Test
//     @DisplayName("haveCommonAncestor returns true for primitive wrapper and its primitive type")
//     void testHaveCommonAncestor_PrimitiveAndWrapper() {
//         // Primitive types and their wrappers are considered related by the utility.
//         assertTrue(ClassUtilities.haveCommonAncestor(int.class, Integer.class), "Primitive int and its wrapper Integer share a common ancestor in the utility's logic");
//     }

    /**
     * Parameterised test covering all positive scenarios.
     */
//     @ParameterizedTest(name = "[{index}] haveCommonAncestor({0}, {1}) → true")
//     @MethodSource("positiveCases")
//     void testHaveCommonAncestor_Positive(Class<?> a, Class<?> b) {
//         assertTrue(ClassUtilities.haveCommonAncestor(a, b), () -> String.format("Expected true for %s and %s", a.getName(), b.getName()));
//     }

    /**
     * Parameterised test covering the negative scenarios where a common ancestor does not exist.
     */
//     @ParameterizedTest(name = "[{index}] haveCommonAncestor({0}, {1}) → false")
//     @MethodSource("negativePrimitiveCases")
//     void testHaveCommonAncestor_NegativePrimitive(Class<?> a, Class<?> b) {
//         assertFalse(ClassUtilities.haveCommonAncestor(a, b), () -> String.format("Expected false for %s and %s", a.getName(), b.getName()));
//     }
}