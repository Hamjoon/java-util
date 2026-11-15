package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Traverser_walk_Test {

    private Traverser traverser;

    /**
     * Simple POJO used in the tests.
     */
    static class Node {

        Node next;

        String name;

        Node(String name) {
            this.name = name;
        }
    }

    /**
     * A class that will be used for the *skip* feature.
     */
    static class SkipMe {

        String data;

        SkipMe(String d) {
            this.data = d;
        }
    }

    /**
     * Simple POJO used for many tests.
     */
    static class SimpleObject {

        final String name;

        SimpleObject(String name) {
            this.name = name;
        }
    }

    /**
     * Primitive‑only POJO – only primitive fields, no reference fields.
     */
    static class SimplePrimitive {

        int i;

        double d;

        SimplePrimitive(int i, double d) {
            this.i = i;
            this.d = d;
        }
    }

    /**
     * Subclass of {@link SkipMe} to verify assignable‑from logic.
     */
    static class SubSkipMe extends SkipMe {

        SubSkipMe(String d) {
            super(d);
        }
    }

    /**
     * Class used for object‑field traversal tests.
     */
    static class City {

        final String name;

        City(String name) {
            this.name = name;
        }
    }

    static class Address {

        final String street;

        final City city;

        Address(String street, City city) {
            this.street = street;
            this.city = city;
        }
    }

    static class Person {

        final String name;

        final Address address;

        Person(String name, Address address) {
            this.name = name;
            this.address = address;
        }
    }

    /**
     * Class that will be placed in the skip list for array‑element tests.
     */
    static class SkippedClass {

        final String id;

        SkippedClass(String id) {
            this.id = id;
        }
    }

    /**
     * Helper Visitor that records every visited object in order.
     */
    static class RecordingVisitor implements Traverser.Visitor {

        final List<Object> visited = new ArrayList<>();

        @Override
        public void process(Object o) {
            visited.add(o);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh instance for each test
        traverser = new Traverser();
        traverser = new Traverser();
    }

    @Test
    void walk_visitsAllObjectsInSimpleGraph() {
        // given
        Node a = new Node("A");
        Node b = new Node("B");
        // a -> b
        a.next = b;
        Traverser.Visitor visitor = mock(Traverser.Visitor.class);
        // when
        traverser.walk(a, null, visitor);
        // then
        // root + b should be visited exactly once each
        verify(visitor, times(1)).process(a);
        verify(visitor, times(1)).process(b);
        verifyNoMoreInteractions(visitor);
    }

    @Test
    void walk_avoidsCycles() {
        // given
        Node a = new Node("A");
        // self‑cycle
        a.next = a;
        Traverser.Visitor visitor = mock(Traverser.Visitor.class);
        // when
        traverser.walk(a, null, visitor);
        // then
        // visitor should be called only once for the root object
        verify(visitor, times(1)).process(a);
        verifyNoMoreInteractions(visitor);
    }

    @Test
    void walk_respectsSkipArrayElements() {
        // given
        SkipMe s1 = new SkipMe("first");
        SkipMe s2 = new SkipMe("second");
        Object[] array = new Object[] { s1, s2, "string" };
        Traverser.Visitor visitor = mock(Traverser.Visitor.class);
        Class<?>[] skip = new Class<?>[] { SkipMe.class };
        // when
        traverser.walk(array, skip, visitor);
        // then
        // The array itself and the non‑skipped element should be visited.
        verify(visitor, times(1)).process(array);
        verify(visitor, times(1)).process("string");
        // SkipMe instances must NOT be visited.
        verify(visitor, never()).process(s1);
        verify(visitor, never()).process(s2);
        verifyNoMoreInteractions(visitor);
    }

    @Test
    void walk_traversesCollections() {
        // given
        List<Node> list = new ArrayList<>();
        Node a = new Node("A");
        Node b = new Node("B");
        list.add(a);
        list.add(b);
        Traverser.Visitor visitor = mock(Traverser.Visitor.class);
        // when
        traverser.walk(list, null, visitor);
        // then
        // collection itself + both nodes
        verify(visitor, times(1)).process(list);
        verify(visitor, times(1)).process(a);
        verify(visitor, times(1)).process(b);
        verifyNoMoreInteractions(visitor);
    }

    @Test
    void walk_traversesMaps() {
        // given
        Map<String, Node> map = new HashMap<>();
        Node a = new Node("A");
        Node b = new Node("B");
        map.put("first", a);
        map.put("second", b);
        Traverser.Visitor visitor = mock(Traverser.Visitor.class);
        // when
        traverser.walk(map, null, visitor);
        // then
        // map itself + both node values (keys are Strings, which are visited as well)
        verify(visitor, times(1)).process(map);
        verify(visitor, times(1)).process("first");
        verify(visitor, times(1)).process("second");
        verify(visitor, times(1)).process(a);
        verify(visitor, times(1)).process(b);
        verifyNoMoreInteractions(visitor);
    }

    @Test
    void walk_handlesNullRootGracefully() {
        // given
        Traverser.Visitor visitor = mock(Traverser.Visitor.class);
        // when
        traverser.walk(null, null, visitor);
        // then
        // visitor should never be called
        verifyNoInteractions(visitor);
    }

    /* -----------------------------------------------------------
       1. Null and already‑visited handling
       ----------------------------------------------------------- */
    @Test
    void walk_nullRootDoesNotVisitAnything() {
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(null, null, visitor);
        assertTrue(visitor.visited.isEmpty(), "Visitor should not be invoked for null root");
    }

    @Test
    void walk_alreadyVisitedObjectIsSkipped() {
        SimpleObject obj = new SimpleObject("first");
        RecordingVisitor visitor = new RecordingVisitor();
        // first traversal – object should be visited
        traverser.walk(obj, null, visitor);
        assertEquals(1, visitor.visited.size());
        assertSame(obj, visitor.visited.get(0));
        // second traversal with same Traverser instance – object must be ignored
        traverser.walk(obj, null, visitor);
        // still only one visit total
        assertEquals(1, visitor.visited.size(), "Second walk should not add another visit because the object is already recorded");
    }

    /* -----------------------------------------------------------
       2. Skip‑class handling
       ----------------------------------------------------------- */
    @Test
    void walk_skipClassPreventsVisit() {
        SkipMe skip = new SkipMe("ignore");
        RecordingVisitor visitor = new RecordingVisitor();
        Class<?>[] skipArr = new Class<?>[] { SkipMe.class };
        traverser.walk(skip, skipArr, visitor);
        assertTrue(visitor.visited.isEmpty(), "Objects whose class is in the skip list must not be visited");
    }

    @Test
    void walk_skipSuperclassPreventsVisitForSubclass() {
        SubSkipMe sub = new SubSkipMe("ignore");
        RecordingVisitor visitor = new RecordingVisitor();
        Class<?>[] skipArr = new Class<?>[] { SkipMe.class };
        traverser.walk(sub, skipArr, visitor);
        assertTrue(visitor.visited.isEmpty(), "Subclass should be skipped when a superclass is present in the skip list");
    }

    /* -----------------------------------------------------------
       3. Array handling
       ----------------------------------------------------------- */
    @Test
    void walk_primitiveArrayVisitsContainerOnly() {
        int[] ints = { 1, 2, 3 };
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(ints, null, visitor);
        assertEquals(1, visitor.visited.size(), "Primitive array should be visited only once");
        assertSame(ints, visitor.visited.get(0));
    }

    @Test
    void walk_objectArrayWithNullElements() {
        String[] arr = { "a", null, "b" };
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(arr, null, visitor);
        // container + two non‑null strings
        assertEquals(3, visitor.visited.size());
        assertEquals(arr, visitor.visited.get(0));
        assertTrue(visitor.visited.contains("a"));
        assertTrue(visitor.visited.contains("b"));
        assertFalse(visitor.visited.contains(null));
    }

    @Test
    void walk_arrayOfSkippedClassElements() {
        SkippedClass s1 = new SkippedClass("x");
        SkippedClass s2 = new SkippedClass("y");
        SkippedClass[] arr = { s1, s2 };
        RecordingVisitor visitor = new RecordingVisitor();
        Class<?>[] skip = new Class<?>[] { SkippedClass.class };
        traverser.walk(arr, skip, visitor);
        // Only the array container should be visited
        assertEquals(1, visitor.visited.size());
        assertSame(arr, visitor.visited.get(0));
    }

    /* -----------------------------------------------------------
       4. Collection handling
       ----------------------------------------------------------- */
    @Test
    void walk_collectionWithElements() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(list, null, visitor);
        // list + three integers
        assertEquals(4, visitor.visited.size());
        assertEquals(list, visitor.visited.get(0));
        assertTrue(visitor.visited.subList(1, 4).containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    void walk_emptyCollectionVisitsOnlyContainer() {
        List<Object> empty = Collections.emptyList();
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(empty, null, visitor);
        assertEquals(1, visitor.visited.size());
        assertSame(empty, visitor.visited.get(0));
    }

    /* -----------------------------------------------------------
       5. Map handling
       ----------------------------------------------------------- */
    @Test
    void walk_singletonMapVisitsKeyAndValue() {
        Map<String, Integer> map = Collections.singletonMap("key", 42);
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(map, null, visitor);
        // map, key, value
        assertEquals(3, visitor.visited.size());
        assertEquals(map, visitor.visited.get(0));
        assertTrue(visitor.visited.subList(1, 3).containsAll(Arrays.asList("key", 42)));
    }

    @Test
    void walk_mapWithNullValueIgnoresNullEntry() {
        Map<String, Object> map = new HashMap<String, Object>() {

            {
                put("nullVal", null);
            }
        };
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(map, null, visitor);
        // only the map itself should be visited
        assertEquals(1, visitor.visited.size());
        assertSame(map, visitor.visited.get(0));
    }

    /* -----------------------------------------------------------
       6. Object‑field handling
       ----------------------------------------------------------- */
    @Test
    void walk_objectFieldsAreTraversedInDepthFirstOrder() {
        City city = new City("Metropolis");
        Address address = new Address("Main St", city);
        Person person = new Person("John", address);
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(person, null, visitor);
        // Expected order: Person, Address, City, "Metropolis", "John"
        List<Object> expected = Arrays.asList(person, address, city, "Metropolis", "John");
        assertEquals(expected, visitor.visited);
    }

    @Test
    void walk_skipClassOnFieldPreventsSubgraphVisit() {
        City city = new City("SkipCity");
        Address address = new Address("Skip St", city);
        Person person = new Person("Skip", address);
        RecordingVisitor visitor = new RecordingVisitor();
        Class<?>[] skip = new Class<?>[] { Address.class };
        traverser.walk(person, skip, visitor);
        // Only Person should be visited; Address and its nested objects are skipped
        assertEquals(1, visitor.visited.size());
        assertSame(person, visitor.visited.get(0));
    }

    /* -----------------------------------------------------------
       7. Multidimensional array handling
       ----------------------------------------------------------- */
    @Test
    void walk_twoDimensionalObjectArray() {
        String[][] outer = { { "x", null }, { "y" } };
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(outer, null, visitor);
        // Visits: outer array, inner array[0], inner array[1], "x", "y"
        assertEquals(5, visitor.visited.size());
        assertSame(outer, visitor.visited.get(0));
        assertTrue(visitor.visited.get(1) instanceof String[]);
        assertTrue(visitor.visited.get(2) instanceof String[]);
        assertTrue(visitor.visited.contains("x"));
        assertTrue(visitor.visited.contains("y"));
        assertFalse(visitor.visited.contains(null));
    }

    /* -----------------------------------------------------------
       8. Array of collections handling
       ----------------------------------------------------------- */
    @Test
    void walk_arrayContainingCollectionAndPrimitiveArray() {
        List<String> list = Arrays.asList("a", "b");
        int[] prim = { 1, 2, 3 };
        Object[] outer = { list, null, prim };
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(outer, null, visitor);
        // Expected visits: outer array, list, "a", "b", primitive int[] (container only)
        assertEquals(5, visitor.visited.size());
        assertSame(outer, visitor.visited.get(0));
        assertSame(list, visitor.visited.get(1));
        assertTrue(visitor.visited.subList(2, 4).containsAll(Arrays.asList("a", "b")));
        assertSame(prim, visitor.visited.get(4));
    }

    /* -----------------------------------------------------------
       9. Collection with null elements
       ----------------------------------------------------------- */
    @Test
    void walk_collectionWithNullElementsSkipsNulls() {
        List<String> list = Arrays.asList("first", null, "second");
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(list, null, visitor);
        // list + two non‑null strings
        assertEquals(3, visitor.visited.size());
        assertEquals(list, visitor.visited.get(0));
        assertTrue(visitor.visited.subList(1, 3).containsAll(Arrays.asList("first", "second")));
        assertFalse(visitor.visited.contains(null));
    }

    /* -----------------------------------------------------------
       10. Map with null key and skip class
       ----------------------------------------------------------- */
    @Test
    void walk_mapNullKeyAndSkippedValue() {
        SkippedClass ignored = new SkippedClass("ignored");
        Map<String, Object> map = new HashMap<String, Object>() {

            {
                // null key – should be ignored
                put(null, "value");
                // value is of a skipped class
                put("key", ignored);
            }
        };
        RecordingVisitor visitor = new RecordingVisitor();
        Class<?>[] skip = new Class<?>[] { SkippedClass.class };
        traverser.walk(map, skip, visitor);
        // Expected visits: map itself, "key" (null key ignored, value skipped)
        assertEquals(2, visitor.visited.size());
        assertSame(map, visitor.visited.get(0));
        assertEquals("key", visitor.visited.get(1));
    }

    /* -----------------------------------------------------------
       11. Cyclic object graph handling (more complex)
       ----------------------------------------------------------- */
    @Test
    void walk_complexCyclicGraphVisitsEachObjectOnce() {
        // Build: node1 -> node2 -> node1 (cycle) and each node holds a name string
        Traverser_walk_Test.Node node1 = new Traverser_walk_Test.Node("first");
        Traverser_walk_Test.Node node2 = new Traverser_walk_Test.Node("second");
        node1.next = node2;
        // cycle
        node2.next = node1;
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(node1, null, visitor);
        // Expected visits: node1, node2, "first", "second"
        assertEquals(4, visitor.visited.size());
        assertTrue(visitor.visited.contains(node1));
        assertTrue(visitor.visited.contains(node2));
        assertTrue(visitor.visited.contains("first"));
        assertTrue(visitor.visited.contains("second"));
    }

    /* -----------------------------------------------------------
       12. Primitive‑only object handling
       ----------------------------------------------------------- */
    @Test
    void walk_primitiveOnlyObjectVisitedOnce() {
        SimplePrimitive po = new SimplePrimitive(10, 3.14);
        RecordingVisitor visitor = new RecordingVisitor();
        traverser.walk(po, null, visitor);
        assertEquals(1, visitor.visited.size());
        assertSame(po, visitor.visited.get(0));
    }

    /* -----------------------------------------------------------
       13. Static traverse method
       ----------------------------------------------------------- */
    @Test
    void traverse_staticConvenienceMethodBehavesLikeWalk() {
        SimpleObject obj = new SimpleObject("static");
        RecordingVisitor visitor = new RecordingVisitor();
        // static method delegates to walk(null)
        Traverser.traverse(obj, visitor);
        assertEquals(1, visitor.visited.size());
        assertSame(obj, visitor.visited.get(0));
    }

    /* -----------------------------------------------------------
       14. Null visitor handling – expect NPE
       ----------------------------------------------------------- */
    @Test
    void walk_nullVisitorThrowsNPE() {
        SimpleObject obj = new SimpleObject("test");
        assertThrows(NullPointerException.class, () -> traverser.walk(obj, null, null), "Passing a null Visitor should result in a NullPointerException");
    }

    /* -----------------------------------------------------------
       Helper for static traverse method with skip (delegates internally)
       ----------------------------------------------------------- */
    @Test
    void traverse_staticMethodWithSkipDelegatesCorrectly() {
        SimpleObject obj = new SimpleObject("skipTest");
        RecordingVisitor visitor = new RecordingVisitor();
        Class<?>[] skip = new Class<?>[] { SimpleObject.class };
        // The static method with skip array should behave like walk(..., skip, ...)
        Traverser.traverse(obj, skip, visitor);
        // Since SimpleObject is in the skip list, it must not be visited.
        assertTrue(visitor.visited.isEmpty(), "Static traverse with skip list must respect the skip logic");
    }
}
