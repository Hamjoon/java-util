import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.Queue;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertypes_Test {

    /**
     * Types that must never appear in the result set.
     */
    private static final Set<Class<?>> DEFAULT_EXCLUDED = Collections.singleton(Object.class);

    /* --------------------------------------------------------------------- */
    class Super {
    }

    class Sub extends Super {
    }

    /**
     * Helper that converts a textual class representation (e.g. "java.lang.String",
     *  "int", "[I") into a {@link Class} instance.
     */
    private static Class<?> resolveClass(String name) {
        try {
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
                default:
                    // For array descriptors (e.g. "[I", "[Ljava.lang.Object;") Class.forName works directly.
                    return Class.forName(name);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to resolve class name: " + name, e);
        }
    }

    /**
     * Converts a list of class name strings to a Set of {@link Class}.
     */
//     private static Set<Class<?>> resolveClassSet(List<String> names) {
//         Set<Class<?>> set = new HashSet<>();
//         for (String n : names) {
//             set.add(resolveClass(n));
//         }
//         return set;
//     }

//     static Stream<Arguments> nullInputProvider() {
//         return Stream.of(Arguments.of("classA is null – method should throw NullPointerException", null, "java.lang.String"), Arguments.of("classB is null – method should throw NullPointerException", "java.lang.Integer", null));
//     }

//     static Stream<Arguments> sameClassProvider() {
//         return Stream.of(Arguments.of("Both arguments are the same concrete class – the only lowest common supertype is that class itself", "java.lang.String", Collections.singletonList("java.lang.String")));
//     }

//     static Stream<Arguments> subclassRelationshipProvider() {
//         return Stream.of(Arguments.of("ArrayList extends List; the lowest common supertype is the interface List", "java.util.ArrayList", "java.util.List", Collections.singletonList("java.util.List")), Arguments.of("LinkedList extends AbstractSequentialList – the method should return that abstract class", "java.util.LinkedList", "java.util.AbstractSequentialList", Collections.singletonList("java.util.AbstractSequentialList")));
//     }

//     static Stream<Arguments> unrelatedClassesProvider() {
//         return Stream.of(Arguments.of("String and Integer share only Object as a common supertype, which is excluded; result should be empty", "java.lang.String", "java.lang.Integer", Collections.emptyList()), Arguments.of("ArrayList and LinkedList share several interfaces; the method should return the set of lowest (most specific) common interfaces", "java.util.ArrayList", "java.util.LinkedList", Arrays.asList("java.util.List", "java.io.Serializable", "java.lang.Cloneable")), Arguments.of("HashMap implements Map – the interface is the lowest common supertype", "java.util.Map", "java.util.HashMap", Collections.singletonList("java.util.Map")), Arguments.of("List extends Collection; the lowest common supertype between the two interfaces is Collection", "java.util.List", "java.util.Collection", Collections.singletonList("java.util.Collection")), Arguments.of("Both arguments are the same interface – result contains that interface", "java.io.Serializable", "java.io.Serializable", Collections.singletonList("java.io.Serializable")), Arguments.of("int[] and Object[] share the interfaces Cloneable and Serializable; Object is excluded, so only those interfaces are returned", "[I", "[Ljava.lang.Object;", Arrays.asList("java.io.Serializable", "java.lang.Cloneable")), Arguments.of("Two different primitive types have no common supertype other than Object, which is excluded; result should be empty", "int", "double", Collections.emptyList()), Arguments.of("Both arguments are the same primitive type – the method should return a set containing that primitive class", "int", "int", Collections.singletonList("int")), Arguments.of("Integer extends Number; the lowest common supertype is the superclass Number", "java.lang.Integer", "java.lang.Number", Collections.singletonList("java.lang.Number")), Arguments.of("Two enum types share the abstract class Enum and the interfaces Serializable and Comparable; these are the lowest common supertypes", "java.time.DayOfWeek", "java.time.Month", Arrays.asList("java.lang.Enum", "java.io.Serializable", "java.lang.Comparable")), Arguments.of("HashSet and TreeSet both extend AbstractSet; the method should return that abstract class as the only lowest common supertype", "java.util.HashSet", "java.util.TreeSet", Collections.singletonList("java.util.AbstractSet")), Arguments.of("String and Date share only the marker interface Serializable (Object is excluded); the result should contain Serializable", "java.lang.String", "java.util.Date", Collections.singletonList("java.io.Serializable")), Arguments.of("TreeSet implements SortedSet/Set, HashSet implements Set; the lowest common interface is Set, plus the common marker interfaces Serializable and Cloneable", "java.util.TreeSet", "java.util.HashSet", Arrays.asList("java.util.Set", "java.io.Serializable", "java.lang.Cloneable")), Arguments.of("Both arguments refer to the same array type (int[]); the method should return that array class", "[I", "[I", Collections.singletonList("[I")));
//     }

    /**
     * Returns all equally “lowest” common supertypes (classes or interfaces) that
     * both {@code classA} and {@code classB} share, automatically excluding
     * {@code Object.class}.
     *
     * @param classA the first class, may be {@code null}
     * @param classB the second class, may be {@code null}
     * @return a {@code Set} of the lowest common supertypes, or an empty set if
     *         none are found beyond {@code Object} (or if either argument is
     *         {@code null})
     */
    public static Set<Class<?>> findLowestCommonSupertypes(Class<?> classA, Class<?> classB) {
        return findLowestCommonSupertypesExcluding(classA, classB, DEFAULT_EXCLUDED);
    }

    /**
     * Core implementation that allows a caller‑provided exclusion set.
     *
     * @param classA   the first class, may be {@code null}
     * @param classB   the second class, may be {@code null}
     * @param exclude  set of types that must be omitted from the result (may be
     *                 {@code null})
     * @return the set of lowest common supertypes after exclusions
     */
    public static Set<Class<?>> findLowestCommonSupertypesExcluding(Class<?> classA, Class<?> classB, Set<Class<?>> exclude) {
        if (classA == null || classB == null) {
            return Collections.emptySet();
        }
        // 1. Gather all supertypes (including the class itself) for each argument.
        Set<Class<?>> aSupers = getAllSupertypes(classA);
        Set<Class<?>> bSupers = getAllSupertypes(classB);
        // 2. Intersection → common supertypes.
        Set<Class<?>> common = new HashSet<>(aSupers);
        common.retainAll(bSupers);
        // 3. Apply exclusion list (Object.class by default).
        if (exclude != null && !exclude.isEmpty()) {
            common.removeAll(exclude);
        }
        // 4. Remove any type that is a supertype of another type in the set.
        //    The remaining types are the "lowest" (most specific) common supertypes.
        Set<Class<?>> lowest = new HashSet<>(common);
        for (Class<?> c1 : common) {
            for (Class<?> c2 : common) {
                if (c1 != c2 && c1.isAssignableFrom(c2)) {
                    // c2 is more specific; c1 must be removed.
                    lowest.remove(c1);
                    break;
                }
            }
        }
        return lowest;
    }

    /**
     * Returns the complete set of supertypes for the given class, including the
     * class itself, its super‑class chain, and all interfaces (recursively).
     *
     * @param clazz the class whose hierarchy is to be explored
     * @return a {@code Set} containing the class and every type it inherits from
     */
    private static Set<Class<?>> getAllSupertypes(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<>();
        Queue<Class<?>> queue = new ArrayDeque<>();
        queue.add(clazz);
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (result.add(current)) {
                // Superclass (if any)
                Class<?> superClass = current.getSuperclass();
                if (superClass != null) {
                    queue.add(superClass);
                }
                // Directly implemented interfaces
                for (Class<?> iface : current.getInterfaces()) {
                    queue.add(iface);
                }
            }
        }
        return result;
    }
}