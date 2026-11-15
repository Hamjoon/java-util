package com.cedarsoftware.util;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_getAllSupertypes_Test {

    /* --------------------------------------------------------------
     *  Test fixtures – simple hierarchy used by many test cases
     * -------------------------------------------------------------- */
    interface TestInterface {
    }

    interface SubInterface extends TestInterface {
    }

    static class BaseClass {
    }

    static class SubClass extends BaseClass implements TestInterface {
    }

    static class UnrelatedClass {
    }

    class MultiIfc implements java.io.Serializable, java.lang.Cloneable {
    }

    class MultiIfc implements java.io.Serializable, java.lang.Cloneable {
    }

    /**
     * Helper that resolves a class name (including primitives and array syntax) to a {@link Class} object.
     */
    private static Class<?> resolveClass(String name) throws ClassNotFoundException {
        // Primitive types
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
        }
        // Array syntax like "java.lang.String[]"
        if (name.endsWith("[]")) {
            String componentName = name.substring(0, name.length() - 2);
            Class<?> component = resolveClass(componentName);
            // create a zero‑length array and take its class
            return Array.newInstance(component, 0).getClass();
        }
        // Normal class name
        return Class.forName(name);
    }

    /**
     * Converts a list of class‑name strings into a {@link Set} of {@link Class} objects.
     */
    private static Set<Class<?>> toClassSet(List<String> names) {
        return names.stream().map(n -> {
            try {
                return resolveClass(n);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to resolve class name: " + n, e);
            }
        }).collect(Collectors.toSet());
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of("ArrayList hierarchy and interfaces", resolveClassSafe("java.util.ArrayList"), toClassSet(List.of("java.util.ArrayList", "java.util.AbstractList", "java.util.AbstractCollection", "java.lang.Object", "java.util.List", "java.util.RandomAccess", "java.lang.Cloneable", "java.io.Serializable", "java.util.Collection", "java.lang.Iterable"))), Arguments.of("Object alone", resolveClassSafe("java.lang.Object"), toClassSet(List.of("java.lang.Object"))));
    }

    private static Stream<Arguments> interfaceOnlyProvider() {
        return Stream.of(Arguments.of("Pure interface List and its super‑interfaces", resolveClassSafe("java.util.List"), toClassSet(List.of("java.util.List", "java.util.Collection", "java.lang.Iterable"))));
    }

    private static Stream<Arguments> primitiveProvider() {
        return Stream.of(Arguments.of("Primitive int", resolveClassSafe("int"), toClassSet(List.of("int"))));
    }

    private static Stream<Arguments> arrayProvider() {
        return Stream.of(Arguments.of("String array super‑types", resolveClassSafe("java.lang.String[]"), toClassSet(List.of("java.lang.String[]", "java.lang.Object", "java.io.Serializable", "java.lang.Cloneable"))));
    }

    private static Stream<Arguments> enumProvider() {
        return Stream.of(Arguments.of("Enum DayOfWeek hierarchy", resolveClassSafe("java.time.DayOfWeek"), toClassSet(List.of("java.time.DayOfWeek", "java.lang.Enum", "java.lang.Object", "java.lang.Comparable", "java.io.Serializable"))));
    }

    private static Stream<Arguments> abstractClassProvider() {
        return Stream.of(Arguments.of("AbstractList hierarchy and interfaces", resolveClassSafe("java.util.AbstractList"), toClassSet(List.of("java.util.AbstractList", "java.util.AbstractCollection", "java.lang.Object", "java.util.List", "java.util.Collection", "java.lang.Iterable"))));
    }

    private static Stream<Arguments> multipleInterfacesProvider() {
        return Stream.of(Arguments.of("HashMap hierarchy and interfaces", resolveClassSafe("java.util.HashMap"), toClassSet(List.of("java.util.HashMap", "java.util.AbstractMap", "java.lang.Object", "java.util.Map", "java.io.Serializable", "java.lang.Cloneable"))));
    }

    private static Stream<Arguments> duplicateAvoidanceProvider() {
        return Stream.of(Arguments.of("ArrayDeque hierarchy without duplicate entries", resolveClassSafe("java.util.ArrayDeque"), toClassSet(List.of("java.util.ArrayDeque", "java.util.AbstractCollection", "java.lang.Object", "java.util.Deque", "java.util.Queue", "java.util.Collection", "java.lang.Iterable", "java.io.Serializable", "java.lang.Cloneable"))));
    }

    /* --------------------------------------------------------------
     *  Utility method used by the providers to avoid checked exceptions
     * -------------------------------------------------------------- */
    private static Class<?> resolveClassSafe(String name) {
        try {
            return resolveClass(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns {@code true} if the supplied {@link java.lang.reflect.Method}
     * has a {@code void} return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(java.lang.reflect.Method method) {
        return method.getReturnType() == void.class;
    }

    @Test
    @DisplayName("Null argument returns an empty set")
    void getAllSupertypes_NullInput_ReturnsEmpty() {
        Set<Class<?>> result = ClassUtilities.getAllSupertypes(null);
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Result should be empty for null input");
    }

    @Test
    @DisplayName("Object class returns a set containing only Object")
    void getAllSupertypes_Object_ReturnsSelfOnly() {
        Set<Class<?>> result = ClassUtilities.getAllSupertypes(Object.class);
        assertEquals(Set.of(Object.class), result);
    }

    @Test
    @DisplayName("Simple class hierarchy includes class and its superclasses")
    void getAllSupertypes_SimpleClassHierarchy() {
        Set<Class<?>> result = ClassUtilities.getAllSupertypes(SubClass.class);
        Set<Class<?>> expected = // Object is a superclass of BaseClass
        Set.// Object is a superclass of BaseClass
        of(// Object is a superclass of BaseClass
        SubClass.class, // Object is a superclass of BaseClass
        BaseClass.class, // Object is a superclass of BaseClass
        TestInterface.class, Object.class);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Interface hierarchy includes the interface and its super‑interfaces")
    void getAllSupertypes_InterfaceHierarchy() {
        Set<Class<?>> result = ClassUtilities.getAllSupertypes(SubInterface.class);
        Set<Class<?>> expected = // All interfaces implicitly extend Object
        Set.// All interfaces implicitly extend Object
        of(// All interfaces implicitly extend Object
        SubInterface.class, // All interfaces implicitly extend Object
        TestInterface.class, Object.class);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Primitive type returns a set containing only the primitive class")
    void getAllSupertypes_Primitive() {
        Set<Class<?>> result = ClassUtilities.getAllSupertypes(int.class);
        assertEquals(Set.of(int.class), result);
    }

    @Test
    @DisplayName("Array type includes the array class, Object, Cloneable and Serializable")
    void getAllSupertypes_Array() {
        Set<Class<?>> result = ClassUtilities.getAllSupertypes(String[].class);
        Set<Class<?>> expected = Set.of(String[].class, Object.class, Cloneable.class, java.io.Serializable.class);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Unrelated classes share only Object")
    void getAllSupertypes_UnrelatedClasses() {
        Set<Class<?>> a = ClassUtilities.getAllSupertypes(SubClass.class);
        Set<Class<?>> b = ClassUtilities.getAllSupertypes(UnrelatedClass.class);
        // Intersection should be exactly Object
        Set<Class<?>> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        assertEquals(Set.of(Object.class), intersection);
    }

    @Test
    @DisplayName("Class that implements multiple interfaces includes all of them")
    void getAllSupertypes_MultipleInterfaces() {
        class MultiIfc implements java.io.Serializable, java.lang.Cloneable {
        }
        Set<Class<?>> result = ClassUtilities.getAllSupertypes(MultiIfc.class);
        Set<Class<?>> expected = Set.of(MultiIfc.class, Object.class, java.io.Serializable.class, java.lang.Cloneable.class);
        assertEquals(expected, result);
    }

    /* ------------------- Basic functionality – ArrayList ------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicFunctionalityProvider")
    void getAllSupertypes_BasicFunctionality(String description, Class<?> input, Set<Class<?>> expected) {
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(input);
        assertEquals(expected, actual, description);
    }

    /* ------------------- Interface only – java.util.List ------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("interfaceOnlyProvider")
    void getAllSupertypes_InterfaceOnly(String description, Class<?> input, Set<Class<?>> expected) {
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(input);
        assertEquals(expected, actual, description);
    }

    /* ------------------- Array type – String[] ------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("arrayProvider")
    void getAllSupertypes_ArrayType(String description, Class<?> input, Set<Class<?>> expected) {
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(input);
        assertEquals(expected, actual, description);
    }

    /* ------------------- Enum type – DayOfWeek ------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("enumProvider")
    void getAllSupertypes_Enum(String description, Class<?> input, Set<Class<?>> expected) {
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(input);
        assertEquals(expected, actual, description);
    }

    /* ------------------- Abstract class – AbstractList ------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("abstractClassProvider")
    void getAllSupertypes_AbstractClass(String description, Class<?> input, Set<Class<?>> expected) {
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(input);
        assertEquals(expected, actual, description);
    }

    /* ------------------- Class with multiple interfaces – HashMap ------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("multipleInterfacesProvider")
    void getAllSupertypes_MultipleInterfacesClass(String description, Class<?> input, Set<Class<?>> expected) {
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(input);
        assertEquals(expected, actual, description);
    }

    /* ------------------- Null input ------------------- */
    @Test
    @DisplayName("Null input from JSON case yields empty set")
    void getAllSupertypes_NullFromJson() {
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(null);
        assertTrue(actual.isEmpty(), "Expected empty set for null input");
    }

    /* ------------------- Duplicate avoidance – ArrayDeque ------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("duplicateAvoidanceProvider")
    void getAllSupertypes_DuplicateAvoidance(String description, Class<?> input, Set<Class<?>> expected) {
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(input);
        assertEquals(expected, actual, description);
    }
}
