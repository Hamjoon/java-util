package com.cedarsoftware.util;

import java.lang.reflect.Field;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.SortedMap;
import java.util.ArrayList;
import java.util.Deque;
import java.util.AbstractCollection;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.AbstractQueue;
import java.util.AbstractSequentialList;

@ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class ClassUtilities_getClassHierarchyInfo_Test {

    interface BaseInterface {
    }

    interface SubInterface extends BaseInterface {
    }

    static class SuperClass {
    }

    static class SubClass extends SuperClass implements SubInterface {
    }

    // Primitive wrapper – should have only ClassUtilities_getClassHierarchyInfo_Test in the hierarchy
    static class PrimitiveWrapper {
    }

    /**
     * Clears the internal {@code CLASS_HIERARCHY_CACHE} between tests.
     * The cache is private static final, so we use reflection.
     */
    @BeforeEach
    void clearCache() throws Exception {
        Field cacheField = ClassUtilities.class.getDeclaredField("CLASS_HIERARCHY_CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Class<?>, ?> cache = (Map<Class<?>, ?>) cacheField.get(null);
        cache.clear();
    }

    @Test
    @DisplayName("Simple class hierarchy – only Object")
    void testObjectOnly() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(Object.class);
        // All supertypes should contain only Object
        Set<Class<?>> expectedSupertypes = Set.of(Object.class);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), "Object should have only Object as a supertype");
        // Distance map should contain Object with distance 0
        Map<Class<?>, Integer> expectedDist = Map.of(Object.class, 0);
        assertEquals(expectedDist, info.getDistanceMap(), "Distance to Object should be 0");
    }

    @Test
    @DisplayName("Class extending another class")
    void testClassExtendsClass() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(SubClass.class);
        // Expected supertypes (order is not important for the Set)
        Set<Class<?>> expected = Set.of(SubClass.class, SuperClass.class, SubInterface.class, BaseInterface.class, Object.class);
        assertEquals(expected, info.getAllSupertypes(), "SubClass hierarchy should contain all ancestors and interfaces");
        // Verify distances
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), distances.get(SubClass.class), "Self distance must be 0");
        assertEquals(Integer.valueOf(1), distances.get(SuperClass.class), "SuperClass should be distance 1");
        assertEquals(Integer.valueOf(1), distances.get(SubInterface.class), "SubInterface should be distance 1");
        assertEquals(Integer.valueOf(2), distances.get(BaseInterface.class), "BaseInterface should be distance 2");
        assertEquals(Integer.valueOf(2), distances.get(Object.class), "Object should be distance 2");
    }

    @Test
    @DisplayName("Interface inheritance")
    void testInterfaceInheritance() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(SubInterface.class);
        Set<Class<?>> expected = Set.of(SubInterface.class, BaseInterface.class, Object.class);
        assertEquals(expected, info.getAllSupertypes(), "SubInterface hierarchy should contain its super‑interface and Object");
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), distances.get(SubInterface.class));
        assertEquals(Integer.valueOf(1), distances.get(BaseInterface.class));
        // Object is a super‑type of every interface; distance is 2 (interface -> Object)
        assertEquals(Integer.valueOf(2), distances.get(Object.class));
    }

    @Test
    @DisplayName("Primitive type handling")
    void testPrimitiveType() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(int.class);
        // Primitive types have no super‑classes or interfaces; only themselves
        Set<Class<?>> expected = Set.of(int.class);
        assertEquals(expected, info.getAllSupertypes(), "Primitive int should have only itself as a supertype");
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        assertEquals(1, distances.size(), "Only one entry expected for primitive");
        assertEquals(Integer.valueOf(0), distances.get(int.class), "Distance to int must be 0");
    }

    @Test
    @DisplayName("Array type handling")
    void testArrayType() {
        Class<?> arrayClass = String[].class;
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(arrayClass);
        // For arrays, the super‑type is Object and Cloneable, Serializable, plus the array class
        Set<Class<?>> expected = Set.of(arrayClass, Object.class, Cloneable.class, java.io.Serializable.class);
        assertEquals(expected, info.getAllSupertypes(), "Array class should include Object, Cloneable and Serializable");
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), distances.get(arrayClass));
        assertEquals(Integer.valueOf(1), distances.get(Object.class));
        assertEquals(Integer.valueOf(1), distances.get(Cloneable.class));
        assertEquals(Integer.valueOf(1), distances.get(java.io.Serializable.class));
    }

    @Test
    @DisplayName("Cache reuse – same instance returned for repeated calls")
    void testCacheReuse() {
        ClassUtilities.ClassHierarchyInfo first = ClassUtilities.getClassHierarchyInfo(SubClass.class);
        ClassUtilities.ClassHierarchyInfo second = ClassUtilities.getClassHierarchyInfo(SubClass.class);
        // The cache returns the same immutable object instance
        assertSame(first, second, "Repeated calls should return the same cached instance");
    }

    @Test
    @DisplayName("Null handling – should throw NullPointerException")
    void testNullHandling() {
        try {
            ClassUtilities.getClassHierarchyInfo(null);
            fail("Passing null must result in NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    @DisplayName("Interface with super‑interface (java.util.List)")
    void testInterfaceWithSuperInterface() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.util.List");
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> expectedSupertypes = Set.of(List.class, Collection.class, Iterable.class, Object.class);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), "List hierarchy should contain List, Collection, Iterable and Object");
        Map<Class<?>, Integer> distances = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), distances.get(List.class));
        assertEquals(Integer.valueOf(1), distances.get(Collection.class));
        assertEquals(Integer.valueOf(2), distances.get(Iterable.class));
        assertEquals(Integer.valueOf(3), distances.get(Object.class));
    }

    @Test
    @DisplayName("Class with multiple interfaces and inheritance chain (ArrayList)")
    void testArrayListHierarchy() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.util.ArrayList");
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> expectedSupertypes = Set.of(ArrayList.class, AbstractList.class, AbstractCollection.class, Object.class, List.class, RandomAccess.class, Cloneable.class, java.io.Serializable.class, Collection.class, Iterable.class);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), "ArrayList hierarchy should contain all expected supertypes");
        Map<Class<?>, Integer> d = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), d.get(ArrayList.class));
        assertEquals(Integer.valueOf(1), d.get(AbstractList.class));
        assertEquals(Integer.valueOf(2), d.get(AbstractCollection.class));
        assertEquals(Integer.valueOf(3), d.get(Object.class));
        assertEquals(Integer.valueOf(1), d.get(List.class));
        assertEquals(Integer.valueOf(1), d.get(RandomAccess.class));
        assertEquals(Integer.valueOf(1), d.get(Cloneable.class));
        assertEquals(Integer.valueOf(1), d.get(java.io.Serializable.class));
        assertEquals(Integer.valueOf(2), d.get(Collection.class));
        assertEquals(Integer.valueOf(3), d.get(Iterable.class));
    }

    @Test
    @DisplayName("Cache reuse – same instance returned for repeated calls (HashMap)")
    void testCacheReuseHashMap() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.util.HashMap");
        ClassUtilities.ClassHierarchyInfo first = ClassUtilities.getClassHierarchyInfo(clazz);
        ClassUtilities.ClassHierarchyInfo second = ClassUtilities.getClassHierarchyInfo(clazz);
        assertSame(first, second, "Repeated calls must return the same cached instance");
    }

    @Test
    @DisplayName("Primitive wrapper class (Integer)")
    void testPrimitiveWrapperInteger() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.lang.Integer");
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> expectedSupertypes = Set.of(Integer.class, Number.class, Object.class, java.io.Serializable.class, Comparable.class);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), "Integer hierarchy should contain Integer, Number, Object, Serializable, Comparable");
        Map<Class<?>, Integer> d = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), d.get(Integer.class));
        assertEquals(Integer.valueOf(1), d.get(Number.class));
        assertEquals(Integer.valueOf(2), d.get(Object.class));
        assertEquals(Integer.valueOf(1), d.get(java.io.Serializable.class));
        assertEquals(Integer.valueOf(1), d.get(Comparable.class));
    }

    @Test
    @DisplayName("Class implementing multiple interfaces with inheritance (LinkedHashMap)")
    void testLinkedHashMapHierarchy() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.util.LinkedHashMap");
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> expectedSupertypes = Set.of(LinkedHashMap.class, HashMap.class, AbstractMap.class, Map.class, Object.class, Collection.class, Iterable.class, java.io.Serializable.class, Cloneable.class);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), "LinkedHashMap hierarchy should contain all expected supertypes");
        Map<Class<?>, Integer> d = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), d.get(LinkedHashMap.class));
        assertEquals(Integer.valueOf(1), d.get(HashMap.class));
        assertEquals(Integer.valueOf(2), d.get(AbstractMap.class));
        assertEquals(Integer.valueOf(1), d.get(Map.class));
        assertEquals(Integer.valueOf(3), d.get(Object.class));
        assertEquals(Integer.valueOf(2), d.get(Collection.class));
        assertEquals(Integer.valueOf(3), d.get(Iterable.class));
        assertEquals(Integer.valueOf(1), d.get(java.io.Serializable.class));
        assertEquals(Integer.valueOf(1), d.get(Cloneable.class));
    }

    @Test
    @DisplayName("Interface that extends multiple interfaces (SortedMap)")
    void testSortedMapHierarchy() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.util.SortedMap");
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> expectedSupertypes = Set.of(SortedMap.class, Map.class, Object.class, Collection.class, Iterable.class);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), "SortedMap hierarchy should contain SortedMap, Map, Object, Collection, Iterable");
        Map<Class<?>, Integer> d = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), d.get(SortedMap.class));
        assertEquals(Integer.valueOf(1), d.get(Map.class));
        assertEquals(Integer.valueOf(2), d.get(Object.class));
        assertEquals(Integer.valueOf(2), d.get(Collection.class));
        assertEquals(Integer.valueOf(3), d.get(Iterable.class));
    }

    @Test
    @DisplayName("Enum type (DayOfWeek)")
    void testEnumDayOfWeek() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.time.DayOfWeek");
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> expectedSupertypes = Set.of(clazz, Enum.class, Object.class, java.io.Serializable.class, Comparable.class);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), "DayOfWeek hierarchy should contain enum, Object, Serializable, Comparable");
        Map<Class<?>, Integer> d = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), d.get(clazz));
        assertEquals(Integer.valueOf(1), d.get(Enum.class));
        assertEquals(Integer.valueOf(2), d.get(Object.class));
        assertEquals(Integer.valueOf(1), d.get(java.io.Serializable.class));
        assertEquals(Integer.valueOf(1), d.get(Comparable.class));
    }

    @Test
    @DisplayName("Abstract class with no interfaces (AbstractQueue)")
    void testAbstractQueueHierarchy() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.util.AbstractQueue");
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> expectedSupertypes = Set.of(AbstractQueue.class, AbstractCollection.class, Collection.class, Iterable.class, Object.class);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), "AbstractQueue hierarchy should contain all expected supertypes");
        Map<Class<?>, Integer> d = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), d.get(AbstractQueue.class));
        assertEquals(Integer.valueOf(1), d.get(AbstractCollection.class));
        assertEquals(Integer.valueOf(2), d.get(Collection.class));
        assertEquals(Integer.valueOf(3), d.get(Iterable.class));
        // Object is reachable via AbstractCollection (distance 2) and via Collection's super‑interface chain.
        assertEquals(Integer.valueOf(2), d.get(Object.class));
    }

    @Test
    @DisplayName("Class with generic super interface (String)")
    void testStringHierarchy() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.lang.String");
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> expectedSupertypes = Set.of(String.class, Object.class, java.io.Serializable.class, Comparable.class, CharSequence.class, Cloneable.class);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), "String hierarchy should contain all expected supertypes");
        Map<Class<?>, Integer> d = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), d.get(String.class));
        assertEquals(Integer.valueOf(1), d.get(Object.class));
        assertEquals(Integer.valueOf(1), d.get(java.io.Serializable.class));
        assertEquals(Integer.valueOf(1), d.get(Comparable.class));
        assertEquals(Integer.valueOf(1), d.get(CharSequence.class));
        assertEquals(Integer.valueOf(1), d.get(Cloneable.class));
    }

    @Test
    @DisplayName("Class with deep inheritance and multiple interfaces (LinkedList)")
    void testLinkedListHierarchy() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.util.LinkedList");
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(clazz);
        Set<Class<?>> expectedSupertypes = Set.of(LinkedList.class, AbstractSequentialList.class, AbstractList.class, AbstractCollection.class, Object.class, List.class, Deque.class, Queue.class, Collection.class, Iterable.class, java.io.Serializable.class, Cloneable.class, RandomAccess.class);
        assertEquals(expectedSupertypes, info.getAllSupertypes(), "LinkedList hierarchy should contain all expected supertypes");
        Map<Class<?>, Integer> d = info.getDistanceMap();
        assertEquals(Integer.valueOf(0), d.get(LinkedList.class));
        assertEquals(Integer.valueOf(1), d.get(AbstractSequentialList.class));
        assertEquals(Integer.valueOf(2), d.get(AbstractList.class));
        assertEquals(Integer.valueOf(3), d.get(AbstractCollection.class));
        assertEquals(Integer.valueOf(4), d.get(Object.class));
        assertEquals(Integer.valueOf(1), d.get(List.class));
        assertEquals(Integer.valueOf(1), d.get(Deque.class));
        assertEquals(Integer.valueOf(2), d.get(Queue.class));
        assertEquals(Integer.valueOf(2), d.get(Collection.class));
        assertEquals(Integer.valueOf(3), d.get(Iterable.class));
        assertEquals(Integer.valueOf(1), d.get(java.io.Serializable.class));
        assertEquals(Integer.valueOf(1), d.get(Cloneable.class));
        assertEquals(Integer.valueOf(1), d.get(RandomAccess.class));
    }

    @Test
    @DisplayName("Cache consistency across same Class objects (ArrayList)")
    void testCacheConsistencyArrayList() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("java.util.ArrayList");
        ClassUtilities.ClassHierarchyInfo first = ClassUtilities.getClassHierarchyInfo(clazz);
        ClassUtilities.ClassHierarchyInfo second = ClassUtilities.getClassHierarchyInfo(clazz);
        assertSame(first, second, "Repeated calls with the same Class must return the same cached instance");
    }
}