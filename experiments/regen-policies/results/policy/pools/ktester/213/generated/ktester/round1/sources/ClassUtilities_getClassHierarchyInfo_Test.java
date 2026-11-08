package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_getClassHierarchyInfo_Test {

    private static Stream<Arguments> provideClassesForParameterizedTests() {
        return Stream.of(Arguments.of(Object.class, Set.of(Object.class), Map.of(Object.class, 0)), Arguments.of(int.class, Set.of(int.class), Map.of(int.class, 0)), Arguments.of(String[].class, Set.of(String[].class, Object.class, java.io.Serializable.class, java.lang.Cloneable.class), Map.of(String[].class, 0, Object.class, 1, java.io.Serializable.class, 1, java.lang.Cloneable.class, 1)));
    }

    @Test
    @DisplayName("Simple class hierarchy – String")
    void testGetClassHierarchyInfo_SimpleClass() {
        // given
        Class<?> clazz = String.class;
        // when
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        // then
        assertNotNull(info, "Info should not be null");
        Set<Class<?>> allSupertypes = info.getAllSupertypes();
        Map<Class<?>, Integer> distanceMap = info.getDistanceMap();
        // The set must contain the class itself and its super‑interfaces / super‑classes
        assertTrue(allSupertypes.contains(String.class), "Should contain the class itself");
        assertTrue(allSupertypes.contains(Object.class), "Should contain Object");
        assertTrue(allSupertypes.contains(CharSequence.class), "Should contain CharSequence");
        assertTrue(allSupertypes.contains(Comparable.class), "Should contain Comparable");
        assertTrue(allSupertypes.contains(Serializable.class), "Should contain Serializable");
        // Distance to self is 0
        assertEquals(0, distanceMap.get(String.class), "Distance to self must be 0");
        // Distance to Object must be > 0 (String → Object)
        assertTrue(distanceMap.get(Object.class) > 0, "Distance to Object should be positive");
    }

    @Test
    @DisplayName("Interface hierarchy – List")
    void testGetClassHierarchyInfo_Interface() {
        // given
        Class<?> clazz = java.util.List.class;
        // when
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        // then
        Set<Class<?>> allSupertypes = info.getAllSupertypes();
        Map<Class<?>, Integer> distanceMap = info.getDistanceMap();
        // List itself and its super‑interfaces
        assertTrue(allSupertypes.contains(java.util.List.class), "Should contain List");
        assertTrue(allSupertypes.contains(java.util.Collection.class), "Should contain Collection");
        assertTrue(allSupertypes.contains(java.lang.Iterable.class), "Should contain Iterable");
        assertTrue(allSupertypes.contains(java.io.Serializable.class), "Should contain Serializable");
        // Distance to self is 0
        assertEquals(0, distanceMap.get(java.util.List.class), "Distance to self must be 0");
        // Distance to Collection should be 1 (direct super‑interface)
        assertEquals(1, distanceMap.get(java.util.Collection.class), "Distance to Collection should be 1");
    }

    @Test
    @DisplayName("Cache behavior – same instance returned")
    void testGetClassHierarchyInfo_Cache() {
        // given
        Class<?> clazz = java.util.HashMap.class;
        // when
        ClassUtilities.ClassHierarchyInfo first = ClassUtilities.getClassHierarchyInfo(clazz);
        ClassUtilities.ClassHierarchyInfo second = ClassUtilities.getClassHierarchyInfo(clazz);
        // then
        assertSame(first, second, "Subsequent calls must return the cached instance");
    }

    @Test
    @DisplayName("Null argument should throw NPE")
    void testGetClassHierarchyInfo_NullArgument() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.getClassHierarchyInfo(null));
    }

    /* --------------------------------------------------------------
     *  Simple class without super‑type or interfaces (java.lang.Object)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Object class – only itself")
    void testObjectClass() {
        Class<?> clazz = Object.class;
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        assertNotNull(info);
        Set<Class<?>> supertypes = info.getAllSupertypes();
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        // Expected set contains only Object
        assertEquals(Set.of(Object.class), supertypes, "Object should have no other supertypes");
        // Distance map should contain only Object → 0
        assertEquals(1, distances.size(), "Only one entry expected in distance map");
        assertEquals(0, distances.get(Object.class), "Distance to self must be 0");
    }

    /* --------------------------------------------------------------
     *  Class with superclass and multiple interfaces (java.util.ArrayList)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("ArrayList hierarchy – super‑classes and interfaces")
    void testArrayListHierarchy() {
        Class<?> clazz = java.util.ArrayList.class;
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> supertypes = info.getAllSupertypes();
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        // Expected supertypes (order not important)
        Set<Class<?>> expectedSupertypes = Set.of(java.util.ArrayList.class, java.util.AbstractList.class, java.util.AbstractCollection.class, Object.class, java.util.List.class, java.util.RandomAccess.class, java.io.Serializable.class, java.lang.Cloneable.class, java.util.Collection.class, java.lang.Iterable.class);
        assertEquals(expectedSupertypes, supertypes, "ArrayList supertypes mismatch");
        // Expected distances (BFS distances)
        Map<Class<?>, Integer> expectedDistances = Map.of(java.util.ArrayList.class, 0, java.util.AbstractList.class, 1, java.util.AbstractCollection.class, 2, Object.class, 3, java.util.List.class, 1, java.util.RandomAccess.class, 1, java.io.Serializable.class, 1, java.lang.Cloneable.class, 1, java.util.Collection.class, 2, java.lang.Iterable.class, 3);
        assertEquals(expectedDistances, distances, "ArrayList distance map mismatch");
    }

    /* --------------------------------------------------------------
     *  Interface hierarchy – java.util.List (already covered partially)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("List interface – super‑interfaces")
    void testListInterfaceHierarchy() {
        Class<?> clazz = java.util.List.class;
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> supertypes = info.getAllSupertypes();
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        Set<Class<?>> expectedSupertypes = Set.of(java.util.List.class, java.util.Collection.class, java.lang.Iterable.class, java.io.Serializable.class);
        assertEquals(expectedSupertypes, supertypes, "List supertypes mismatch");
        Map<Class<?>, Integer> expectedDistances = Map.of(java.util.List.class, 0, java.util.Collection.class, 1, java.lang.Iterable.class, 2, // Serializable is a direct super‑interface of List
        java.io.Serializable.class, // Serializable is a direct super‑interface of List
        1);
        assertEquals(expectedDistances, distances, "List distance map mismatch");
    }

    /* --------------------------------------------------------------
     *  Primitive type class (int)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Primitive int – only itself")
    void testPrimitiveInt() {
        Class<?> clazz = int.class;
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> supertypes = info.getAllSupertypes();
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        assertEquals(Set.of(int.class), supertypes, "Primitive int should have no other supertypes");
        assertEquals(1, distances.size(), "Distance map should contain only the primitive itself");
        assertEquals(0, distances.get(int.class), "Distance to self must be 0");
    }

    /* --------------------------------------------------------------
     *  Array class (java.lang.String[])
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Array type String[] – Object, Cloneable, Serializable")
    void testStringArrayHierarchy() {
        Class<?> clazz = String[].class;
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> supertypes = info.getAllSupertypes();
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        Set<Class<?>> expectedSupertypes = Set.of(String[].class, Object.class, java.io.Serializable.class, java.lang.Cloneable.class);
        assertEquals(expectedSupertypes, supertypes, "String[] supertypes mismatch");
        Map<Class<?>, Integer> expectedDistances = Map.of(String[].class, 0, Object.class, 1, java.io.Serializable.class, 1, java.lang.Cloneable.class, 1);
        assertEquals(expectedDistances, distances, "String[] distance map mismatch");
    }

    /* --------------------------------------------------------------
     *  Class implementing interfaces and extending a superclass (java.util.HashMap)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("HashMap hierarchy – super‑class and interfaces")
    void testHashMapHierarchy() {
        Class<?> clazz = java.util.HashMap.class;
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> supertypes = info.getAllSupertypes();
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        Set<Class<?>> expectedSupertypes = Set.of(java.util.HashMap.class, java.util.AbstractMap.class, Object.class, java.util.Map.class, java.io.Serializable.class, java.lang.Cloneable.class);
        assertEquals(expectedSupertypes, supertypes, "HashMap supertypes mismatch");
        Map<Class<?>, Integer> expectedDistances = Map.of(java.util.HashMap.class, 0, java.util.AbstractMap.class, 1, Object.class, 2, // direct interface
        java.util.Map.class, // direct interface
        1, // direct interface
        java.io.Serializable.class, // direct interface
        1, // direct interface
        java.lang.Cloneable.class, // direct interface
        1);
        assertEquals(expectedDistances, distances, "HashMap distance map mismatch");
    }

    /* --------------------------------------------------------------
     *  Deep inheritance chain with many interfaces (java.util.LinkedList)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("LinkedList hierarchy – deep chain and interfaces")
    void testLinkedListHierarchy() {
        Class<?> clazz = java.util.LinkedList.class;
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> supertypes = info.getAllSupertypes();
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        Set<Class<?>> expectedSupertypes = Set.of(java.util.LinkedList.class, java.util.AbstractSequentialList.class, java.util.AbstractList.class, java.util.AbstractCollection.class, Object.class, java.util.List.class, java.util.Deque.class, java.util.Queue.class, java.util.Collection.class, java.lang.Iterable.class, java.io.Serializable.class, java.lang.Cloneable.class);
        assertEquals(expectedSupertypes, supertypes, "LinkedList supertypes mismatch");
        Map<Class<?>, Integer> expectedDistances = Map.of(java.util.LinkedList.class, 0, java.util.AbstractSequentialList.class, 1, java.util.AbstractList.class, 2, java.util.AbstractCollection.class, 3, Object.class, 4, java.util.List.class, 1, java.util.Deque.class, 1, java.util.Queue.class, 2, java.util.Collection.class, 2, java.lang.Iterable.class, 3, java.io.Serializable.class, 2, java.lang.Cloneable.class, 1);
        assertEquals(expectedDistances, distances, "LinkedList distance map mismatch");
    }

    /* --------------------------------------------------------------
     *  Enum type (java.time.DayOfWeek)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Enum DayOfWeek – superclass Enum + interfaces")
    void testEnumDayOfWeekHierarchy() {
        Class<?> clazz = DayOfWeek.class;
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> supertypes = info.getAllSupertypes();
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        Set<Class<?>> expectedSupertypes = Set.of(DayOfWeek.class, java.lang.Enum.class, Object.class, java.time.temporal.TemporalAccessor.class, java.time.temporal.TemporalAdjuster.class, java.io.Serializable.class, java.lang.Comparable.class);
        assertEquals(expectedSupertypes, supertypes, "DayOfWeek supertypes mismatch");
        Map<Class<?>, Integer> expectedDistances = Map.of(DayOfWeek.class, 0, java.lang.Enum.class, 1, Object.class, 2, java.time.temporal.TemporalAccessor.class, 1, java.time.temporal.TemporalAdjuster.class, 1, java.io.Serializable.class, 2, java.lang.Comparable.class, 2);
        assertEquals(expectedDistances, distances, "DayOfWeek distance map mismatch");
    }

    /* --------------------------------------------------------------
     *  Cache reuse – same ClassHierarchyInfo instance returned on repeated calls
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Cache reuse – ArrayList instance identity")
    void testCacheReuseArrayList() {
        Class<?> clazz = java.util.ArrayList.class;
        ClassUtilities.ClassHierarchyInfo first = ClassUtilities.getClassHierarchyInfo(clazz);
        ClassUtilities.ClassHierarchyInfo second = ClassUtilities.getClassHierarchyInfo(clazz);
        assertSame(first, second, "Repeated calls must return the same cached instance");
    }

    /* --------------------------------------------------------------
     *  Null class argument – already covered in base test, but repeat here for completeness
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Null argument throws NPE")
    void testNullArgumentThrows() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.getClassHierarchyInfo(null), "Passing null should result in NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Parameterized test harness for the groups that share the same structure.
     *  (Demonstrates use of MethodSource – not required for the above explicit tests)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} ⇒ class={0}")
    @MethodSource("provideClassesForParameterizedTests")
    void parameterizedHierarchyTest(Class<?> clazz, Set<Class<?>> expectedSupertypes, Map<Class<?>, Integer> expectedDistances) {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), () -> "Unexpected supertypes for " + clazz.getName());
        assertEquals(expectedDistances, info.getDistanceMap(), () -> "Unexpected distance map for " + clazz.getName());
    }
}
