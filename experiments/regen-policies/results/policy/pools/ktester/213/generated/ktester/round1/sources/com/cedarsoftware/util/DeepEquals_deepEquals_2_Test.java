package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// @ExtendWith(MockitoExtension.class)
class DeepEquals_deepEquals_2_Test {

//     private Map<String, Object> options;

    /**
     * Minimal Person class used for diff testing.
     */
    private static final class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        //         @Override
        //         public boolean equals(Object o) {
        //             if (this == o)
        //                 return true;
        //             if (!(o instanceof Person))
        //                 return false;
        //             Person p = (Person) o;
        //             return age == p.age && Objects.equals(name, p.name);
        //         }
        //         @Override
        //         public int hashCode() {
        //             return Objects.hash(name, age);
        //         }
    }

    /**
     * Node class that can form a circular linked structure.
     */
    private static final class Node {

        final String value;

        Node next;

        Node(String value) {
            this.value = value;
        }
        //         @Override
        //         public boolean equals(Object o) {
        //             if (this == o)
        //                 return true;
        //             if (!(o instanceof Node))
        //                 return false;
        //             Node node = (Node) o;
        //             return Objects.equals(value, node.value) && Objects.equals(next, node.next);
        //         }
        //         @Override
        //         public int hashCode() {
        //             return Objects.hash(value, next);
        //         }
    }

    /**
     * Person class that only compares the name (age is ignored).
     */
    private static final class PersonNameOnly {

        final String name;

        final int age;

        PersonNameOnly(String name, int age) {
            this.name = name;
            this.age = age;
        }
        //         @Override
        //         public boolean equals(Object o) {
        //             if (this == o)
        //                 return true;
        //             if (!(o instanceof PersonNameOnly))
        //                 return false;
        //             PersonNameOnly that = (PersonNameOnly) o;
        //             // deliberately ignore age
        //             return Objects.equals(name, that.name);
        //         }
        //         @Override
        //         public int hashCode() {
        //             return Objects.hash(name);
        //         }
    }

    /**
     * Helper methods that build two separate but identical cyclic graphs.
     */
    private static final class CyclicHelper {

        static Node cycleA() {
            Node a1 = new Node("A");
            Node a2 = new Node("B");
            a1.next = a2;
            // cycle back to a1
            a2.next = a1;
            return a1;
        }

        static Node cycleB() {
            Node b1 = new Node("A");
            Node b2 = new Node("B");
            b1.next = b2;
            // cycle back to b1
            b2.next = b1;
            return b1;
        }
    }

    /**
     * Simple POJO with a broken custom equals implementation.
     */
    private static final class BadEquals {

        final int id;

        BadEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            // Intentionally wrong – always returns false.
            return false;
        }
        //         @Override
        //         public int hashCode() {
        //             return Objects.hash(id);
        //         }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Fresh options map for each test.
//         options = new HashMap<>();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        options.clear();
    }

//     private static Stream<Arguments> nullHandlingProvider() {
//         return Stream.of(Arguments.of(null, null, Collections.emptyMap(), true, null), Arguments.of(null, new Object(), Collections.emptyMap(), false, DeepEquals.DIFF), Arguments.of(new Object(), null, Collections.emptyMap(), false, DeepEquals.DIFF));
//     }

//     private static Stream<Arguments> primitiveTypesProvider() {
//         return Stream.of(Arguments.of(Integer.valueOf(42), Integer.valueOf(42), true), Arguments.of(Integer.valueOf(42), Integer.valueOf(43), false));
//     }

//     private static Stream<Arguments> stringNumberOptionProvider() {
//         return Stream.of(Arguments.of("123", Integer.valueOf(123), Collections.emptyMap(), false), Arguments.of("123", Integer.valueOf(123), Map.of(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true), true));
//     }

//     private static Stream<Arguments> listEqualityProvider() {
//         return Stream.of(Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3), true), Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(3, 2, 1), false));
//     }

//     private static Stream<Arguments> setEqualityProvider() {
//         return Stream.of(Arguments.of(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(3, 2, 1)), true), Arguments.of(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(1, 2, 4)), false));
//     }

//     private static Stream<Arguments> mapEqualityProvider() {
//         return Stream.of(Arguments.of(Map.of("x", 1, "y", 2), Map.of("x", 1, "y", 2), true), Arguments.of(Map.of("x", 1, "y", 2), Map.of("x", 1, "y", 3), false));
//     }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType().equals(void.class);
    }

    /**
     * Finds a public method with the given name and parameter types on the
     * supplied class.  Returns {@code null} if no such method exists.
     *
     * @param clazz          the class to search; never {@code null}
     * @param name           the method name; never {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Returns {@code true} if a public method with the given name and parameter
     * types is present on the supplied class.
     *
     * @param clazz          the class to inspect; never {@code null}
     * @param name           the method name; never {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    public static boolean isPublic(Class<?> clazz) {
        return Modifier.isPublic(clazz.getModifiers());
    }

    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * Simple numeric comparison – a double and an integer are considered equal.
     */
//     @Test
//     void numericComparisonIsDeep() {
//         boolean result = DeepEquals.deepEquals(1.0d, 1, options);
//         assertTrue(result, "1.0d should be deep‑equal to 1");
//         assertNull(options.get(DeepEquals.DIFF), "No diff should be recorded for equal values");
//     }

    /**
     * By default a numeric string does NOT match a Number.
     */
//     @Test
//     void stringNumberMismatchWithoutOption() {
//         boolean result = DeepEquals.deepEquals("10", 10, options);
//         assertFalse(result, "String \"10\" should NOT match number 10 without the option");
//         assertNotNull(options.get(DeepEquals.DIFF), "Diff should be present when values differ");
//     }

    /**
     * Enabling the option allows a numeric string to match a Number.
     */
//     @Test
//     void stringNumberMatchesWhenOptionEnabled() {
//         options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
//         boolean result = DeepEquals.deepEquals("10", 10, options);
//         assertTrue(result, "String \"10\" should match number 10 when option is true");
//         assertNull(options.get(DeepEquals.DIFF), "No diff should be recorded for matching values");
//     }

    /**
     * Custom equals can be ignored via the IGNORE_CUSTOM_EQUALS option.
     */
//     @Test
//     void ignoreCustomEqualsOptionWorks() {
//         // Two objects with a deliberately broken custom equals implementation.
//         BadEquals a = new BadEquals(1);
//         BadEquals b = new BadEquals(1);
//         // Without any options the custom equals is used → false.
//         assertFalse(DeepEquals.deepEquals(a, b, options));
//         // Now ignore the custom equals for BadEquals class.
//         options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(BadEquals.class));
//         boolean result = DeepEquals.deepEquals(a, b, options);
//         assertTrue(result, "DeepEquals should fall back to field‑by‑field comparison when custom equals is ignored");
//         assertNull(options.get(DeepEquals.DIFF));
//     }

    /**
     * When objects differ, the diff description is stored under the DIFF key.
     */
//     @Test
//     void diffIsReturnedInOptionsMap() {
//         Person p1 = new Person("Alice", 30);
//         // age differs
//         Person p2 = new Person("Alice", 31);
//         boolean result = DeepEquals.deepEquals(p1, p2, options);
//         assertFalse(result, "Persons with different ages should not be deep equal");
//         Object diff = options.get(DeepEquals.DIFF);
//         assertNotNull(diff, "Diff description must be present");
//         String diffStr = diff.toString();
//         assertTrue(diffStr.contains(".age"), "Diff should mention the differing field");
//     }

    /**
     * DeepEquals must handle circular references without infinite recursion.
     */
//     @Test
//     void handlesCircularReferencesGracefully() {
//         Node n1 = new Node("root");
//         Node n2 = new Node("root");
//         n1.next = n2;
//         // creates a cycle
//         n2.next = n1;
//         Node m1 = new Node("root");
//         Node m2 = new Node("root");
//         m1.next = m2;
//         // same shape cycle
//         m2.next = m1;
//         assertTrue(DeepEquals.deepEquals(n1, m1, options), "Objects with identical circular graphs should be equal");
//     }

    /**
     * Ordered collections must have the same order to be equal.
     */
//     @Test
//     void orderedCollectionOrderMatters() {
//         List<Integer> list1 = Arrays.asList(1, 2, 3);
//         List<Integer> list2 = Arrays.asList(3, 2, 1);
//         assertFalse(DeepEquals.deepEquals(list1, list2, options), "Lists with different order should not be equal");
//         assertNotNull(options.get(DeepEquals.DIFF));
//     }

    /**
     * Unordered collections are compared without regard to order.
     */
//     @Test
//     void unorderedCollectionOrderIgnored() {
//         Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
//         Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 2, 1));
//         assertTrue(DeepEquals.deepEquals(set1, set2, options), "Sets with same elements should be equal regardless of order");
//     }

    /**
     * Maps are compared key‑by‑key; a mismatching value produces a diff.
     */
//     @Test
//     void mapValueMismatchProducesDiff() {
//         Map<String, Integer> m1 = Map.of("a", 1, "b", 2);
//         // value for "b" differs
//         Map<String, Integer> m2 = Map.of("a", 1, "b", 3);
//         assertFalse(DeepEquals.deepEquals(m1, m2, options));
//         String diff = (String) options.get(DeepEquals.DIFF);
//         assertNotNull(diff);
//         assertTrue(diff.contains("b"), "Diff should reference the mismatching key");
//     }

//     @ParameterizedTest(name = "[{index}] a={0}, b={1} → expected={3}")
//     @MethodSource("nullHandlingProvider")
//     void nullHandling(Object a, Object b, Map<String, ?> opts, boolean expected, String diffKey) {
//         // The framework supplies a fresh options map; we copy the supplied map to avoid mutating the constant.
//         options.clear();
//         if (opts != null) {
//             options.putAll(opts);
//         }
//         boolean result = DeepEquals.deepEquals(a, b, options);
//         assertEquals(expected, result);
//         if (expected) {
//             assertNull(options.get(DeepEquals.DIFF));
//         } else {
//             assertNotNull(options.get(diffKey));
//         }
//     }

//     @ParameterizedTest(name = "[{index}] Integer {0} vs {1} → {2}")
//     @MethodSource("primitiveTypesProvider")
//     void primitiveTypes(Integer a, Integer b, boolean expected) {
//         boolean result = DeepEquals.deepEquals(a, b, options);
//         assertEquals(expected, result);
//     }

//     @ParameterizedTest(name = "[{index}] \"{0}\" vs {1} with opts={2} → {3}")
//     @MethodSource("stringNumberOptionProvider")
//     void stringNumberOption(String a, Integer b, Map<String, Object> opts, boolean expected) {
//         options.clear();
//         options.putAll(opts);
//         boolean result = DeepEquals.deepEquals(a, b, options);
//         assertEquals(expected, result);
//     }

//     @ParameterizedTest(name = "[{index}] List {0} vs {1} → {2}")
//     @MethodSource("listEqualityProvider")
//     void listEquality(List<Integer> a, List<Integer> b, boolean expected) {
//         boolean result = DeepEquals.deepEquals(a, b, options);
//         assertEquals(expected, result);
//     }

//     @ParameterizedTest(name = "[{index}] Set {0} vs {1} → {2}")
//     @MethodSource("setEqualityProvider")
//     void setEquality(Set<Integer> a, Set<Integer> b, boolean expected) {
//         boolean result = DeepEquals.deepEquals(a, b, options);
//         assertEquals(expected, result);
//     }

//     @ParameterizedTest(name = "[{index}] Map {0} vs {1} → {2}")
//     @MethodSource("mapEqualityProvider")
//     void mapEquality(Map<String, Integer> a, Map<String, Integer> b, boolean expected) {
//         boolean result = DeepEquals.deepEquals(a, b, options);
//         assertEquals(expected, result);
//     }

//     @Test
//     void primitiveIntArrayEquality() {
//         int[] a = new int[] { 1, 2, 3 };
//         int[] b = new int[] { 1, 2, 3 };
//         assertTrue(DeepEquals.deepEquals(a, b, options));
//     }

//     @Test
//     void primitiveIntArrayInequality() {
//         int[] a = new int[] { 1, 2, 3 };
//         int[] b = new int[] { 3, 2, 1 };
//         assertFalse(DeepEquals.deepEquals(a, b, options));
//         assertNotNull(options.get(DeepEquals.DIFF));
//     }

//     @Test
//     void objectArrayEqualityWithNulls() {
//         Object[] a = new Object[] { "a", null };
//         Object[] b = new Object[] { "a", null };
//         assertTrue(DeepEquals.deepEquals(a, b, options));
//     }

//     @Test
//     void customEqualsUsedByDefault() {
//         PersonNameOnly a = new PersonNameOnly("Alice", 30);
//         // different age, same name
//         PersonNameOnly b = new PersonNameOnly("Alice", 40);
//         // Default behavior should rely on the overridden equals → true
//         assertTrue(DeepEquals.deepEquals(a, b, options));
//         assertNull(options.get(DeepEquals.DIFF));
//     }

//     @Test
//     void ignoreCustomEqualsForPersonNameOnly() {
//         PersonNameOnly a = new PersonNameOnly("Alice", 30);
//         PersonNameOnly b = new PersonNameOnly("Alice", 40);
//         // Instruct DeepEquals to ignore the custom equals of PersonNameOnly
//         options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(PersonNameOnly.class));
//         boolean result = DeepEquals.deepEquals(a, b, options);
//         // Field‑by‑field comparison sees differing ages → false
//         assertFalse(result);
//         assertNotNull(options.get(DeepEquals.DIFF));
//     }

//     @Test
//     void identicalCyclicGraphsAreEqual() {
//         Node a = CyclicHelper.cycleA();
//         Node b = CyclicHelper.cycleB();
//         assertTrue(DeepEquals.deepEquals(a, b, options), "Two identical cyclic structures must compare as equal");
//     }

//     @Test
//     void nullOptionsMapThrowsNPE() {
//         Object o1 = new Object();
//         Object o2 = new Object();
//         assertThrows(NullPointerException.class, () -> DeepEquals.deepEquals(o1, o2, null), "Passing a null options map should throw NullPointerException");
//     }

//     @Test
//     void invalidOptionTypeForStringNumberOption() {
//         // Integer instead of Boolean
//         options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, 1);
//         assertThrows(ClassCastException.class, () -> DeepEquals.deepEquals("123", Integer.valueOf(123), options), "Non‑Boolean value for stringsCanMatchNumbers should cause ClassCastException");
//     }

//     @Test
//     void invalidOptionTypeForIgnoreCustomEquals() {
//         // String instead of Set<Class<?>>
//         options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, "true");
//         assertThrows(ClassCastException.class, () -> DeepEquals.deepEquals(new PersonNameOnly("Alice", 30), new PersonNameOnly("Alice", 30), options), "Non‑Set value for ignoreCustomEquals should cause ClassCastException");
//     }

//     @Test
//     void nullOptionValueForIgnoreCustomEqualsCausesNPE() {
//         options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, null);
//         PersonNameOnly a = new PersonNameOnly("Bob", 25);
//         PersonNameOnly b = new PersonNameOnly("Bob", 30);
//         assertThrows(NullPointerException.class, () -> DeepEquals.deepEquals(a, b, options), "Null value for ignoreCustomEquals should trigger NullPointerException when unboxed");
//     }

//     @Test
//     void nullOptionValueForStringNumberOptionCausesNPE() {
//         options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, null);
//         assertThrows(NullPointerException.class, () -> DeepEquals.deepEquals("456", Integer.valueOf(456), options), "Null value for stringsCanMatchNumbers should trigger NullPointerException when unboxed");
//     }
}