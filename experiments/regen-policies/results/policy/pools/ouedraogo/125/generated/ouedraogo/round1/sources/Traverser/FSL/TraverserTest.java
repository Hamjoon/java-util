package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Traverser}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public {@code traverse} overloads (modern and legacy)</li>
 *   <li>Correct handling of arrays, collections, maps and plain objects</li>
 *   <li>Cycle detection</li>
 *   <li>Class‑skipping logic</li>
 *   <li>Null handling and argument validation</li>
 *   <li>Behaviour of {@link Traverser.NodeVisit}</li>
 * </ul>
 */
public class TraverserTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used in the tests                                         */
    /* ---------------------------------------------------------------------- */

    /** Simple POJO with primitive, reference fields and a self‑reference. */
    static class Simple {
        int id;
        String name;
        Simple child;

        Simple(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** POJO that holds an array field (object array). */
    static class WithArray {
        Simple[] elements;

        WithArray(Simple[] elements) {
            this.elements = elements;
        }
    }

    /** POJO that holds a collection field. */
    static class WithCollection {
        List<Simple> list;

        WithCollection(List<Simple> list) {
            this.list = list;
        }
    }

    /** POJO that holds a map field. */
    static class WithMap {
        Map<Object, Simple> map;

        WithMap(Map<Object, Simple> map) {
            this.map = map;
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Utility methods for the test suite                                      */
    /* ---------------------------------------------------------------------- */

    /** Executes {@link Traverser#traverse(Object, Consumer, Set)} and returns the
     *  ordered list of {@link Traverser.NodeVisit}s that were produced. */
    private List<Traverser.NodeVisit> walk(Object root, Set<Class<?>> skip) {
        final List<Traverser.NodeVisit> visits = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = visits::add;
        Traverser.traverse(root, collector, skip);
        return visits;
    }

    /** Convenience overload when no class‑skip set is required. */
    private List<Traverser.NodeVisit> walk(Object root) {
        return walk(root, null);
    }

    /** Returns the set of nodes (objects) that were visited. */
    private Set<Object> visitedObjects(List<Traverser.NodeVisit> visits) {
        Set<Object> set = new HashSet<>();
        for (Traverser.NodeVisit v : visits) {
            set.add(v.getNode());
        }
        return set;
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Basic traversal – root + fields                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTraverseRootAndNestedObject() {
        Simple child = new Simple(2, "child");
        Simple root = new Simple(1, "root");
        root.child = child;

        List<Traverser.NodeVisit> visits = walk(root);
        // Expect exactly two nodes: the root and its child
        assertEquals(2, visits.size());

        Set<Object> nodes = visitedObjects(visits);
        assertTrue(nodes.contains(root));
        assertTrue(nodes.contains(child));

        // Verify that NodeVisit correctly reports the node class
        for (Traverser.NodeVisit v : visits) {
            assertEquals(v.getNode().getClass(), v.getNodeClass());
        }

        // Verify that fields map contains the declared fields of the class
        Traverser.NodeVisit rootVisit = visits.stream()
                .filter(v -> v.getNode() == root)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Root visit missing"));
        Map<Field, Object> rootFields = rootVisit.getFields();
        // The map must contain at least the three declared fields
        assertTrue(rootFields.keySet().stream()
                .anyMatch(f -> f.getName().equals("id")));
        assertTrue(rootFields.keySet().stream()
                .anyMatch(f -> f.getName().equals("name")));
        assertTrue(rootFields.keySet().stream()
                .anyMatch(f -> f.getName().equals("child")));
        // The value for 'child' must be the child object we set
        Field childField = rootFields.keySet().stream()
                .filter(f -> f.getName().equals("child"))
                .findFirst()
                .get();
        assertSame(child, rootFields.get(childField));
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Traversal of an object array                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTraverseObjectArray() {
        Simple a = new Simple(1, "A");
        Simple b = new Simple(2, "B");
        Simple[] array = new Simple[]{a, b};

        List<Traverser.NodeVisit> visits = walk(array);
        // Three nodes: the array itself + the two Simple instances
        assertEquals(3, visits.size());

        Set<Object> nodes = visitedObjects(visits);
        assertTrue(nodes.contains(array));
        assertTrue(nodes.contains(a));
        assertTrue(nodes.contains(b));
    }

    @Test
    public void testTraversePrimitiveArrayIsNotExpanded() {
        int[] primitives = new int[]{1, 2, 3};

        List<Traverser.NodeVisit> visits = walk(primitives);
        // Only the array itself should be visited (primitive elements are ignored)
        assertEquals(1, visits.size());
        assertSame(primitives, visits.get(0).getNode());
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Traversal of collections                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTraverseCollection() {
        Simple a = new Simple(1, "A");
        Simple b = new Simple(2, "B");
        List<Simple> list = new ArrayList<>(Arrays.asList(a, b));

        List<Traverser.NodeVisit> visits = walk(list);
        // Expect the list itself + the two Simple objects
        assertEquals(3, visits.size());

        Set<Object> nodes = visitedObjects(visits);
        assertTrue(nodes.contains(list));
        assertTrue(nodes.contains(a));
        assertTrue(nodes.contains(b));
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Traversal of maps (both keys and values)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTraverseMap() {
        Simple keyObj = new Simple(10, "keyObj");
        Simple valueObj = new Simple(20, "valueObj");
        Map<Object, Simple> map = new HashMap<>();
        map.put("stringKey", valueObj);
        map.put(keyObj, null); // key is an object, value is null

        List<Traverser.NodeVisit> visits = walk(map);
        // Nodes: the map itself, the string key (String), the keyObj, and valueObj
        // (null values are ignored)
        assertEquals(4, visits.size());

        Set<Object> nodes = visitedObjects(visits);
        assertTrue(nodes.contains(map));
        assertTrue(nodes.contains("stringKey"));
        assertTrue(nodes.contains(keyObj));
        assertTrue(nodes.contains(valueObj));
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Class‑skip logic                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSkipStringClass() {
        Simple child = new Simple(2, "child");
        Simple root = new Simple(1, "root");
        root.child = child; // child contains a String field 'name'

        Set<Class<?>> skip = new HashSet<>();
        skip.add(String.class); // skip all Strings

        List<Traverser.NodeVisit> visits = walk(root, skip);
        // Expected nodes: root and child only (String values are not traversed)
        assertEquals(2, visits.size());

        Set<Object> nodes = visitedObjects(visits);
        assertTrue(nodes.contains(root));
        assertTrue(nodes.contains(child));
        // Verify that no String instance appears as a visited node
        for (Object o : nodes) {
            assertFalse(o instanceof String);
        }
    }

    @Test
    public void testLegacyTraverseWithSkipArray() {
        Simple a = new Simple(1, "A");
        Simple b = new Simple(2, "B");
        Simple[] array = new Simple[]{a, b};

        // Legacy API: traverse(root, Class<?>[] skip, Visitor)
        final List<Object> visited = new ArrayList<>();
        Traverser.Visitor visitor = visited::add;
        Traverser.traverse(array, new Class<?>[]{Simple.class}, visitor);

        // Because Simple.class is in the skip list, only the array itself should be visited
        assertEquals(1, visited.size());
        assertSame(array, visited.get(0));
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Cycle detection                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCycleDetection() {
        Simple a = new Simple(1, "A");
        Simple b = new Simple(2, "B");
        a.child = b;
        b.child = a; // creates a cycle

        List<Traverser.NodeVisit> visits = walk(a);
        // Only two distinct nodes should be visited despite the cycle
        assertEquals(2, visits.size());

        Set<Object> nodes = visitedObjects(visits);
        assertTrue(nodes.contains(a));
        assertTrue(nodes.contains(b));
    }

    /* ---------------------------------------------------------------------- */
    /* 7. Null root handling                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTraverseNullRootDoesNotVisit() {
        final List<Traverser.NodeVisit> visited = new ArrayList<>();
        Traverser.traverse(null, visited::add, null);
        assertTrue(visited.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 8. Argument validation (null visitor)                                    */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testTraverseNullVisitorThrows() {
        Traverser.traverse(new Object(), (Consumer<Traverser.NodeVisit>) null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLegacyTraverseNullVisitorThrows() {
        Traverser.traverse(new Object(), (Traverser.Visitor) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLegacyTraverseNullVisitorWithSkipThrows() {
        Traverser.traverse(new Object(), new Class<?>[]{String.class}, null);
    }

    /* ---------------------------------------------------------------------- */
    /* 9. NodeVisit immutability and content                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNodeVisitFieldsMapIsUnmodifiable() {
        Simple root = new Simple(1, "root");
        List<Traverser.NodeVisit> visits = walk(root);
        Traverser.NodeVisit visit = visits.get(0);
        Map<Field, Object> fields = visit.getFields();

        try {
            fields.put(null, null);
            fail("Expected UnsupportedOperationException when modifying fields map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 10. Traversal of objects containing collections/maps/arrays as fields    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTraverseObjectContainingArrayField() {
        Simple a = new Simple(1, "A");
        Simple b = new Simple(2, "B");
        WithArray root = new WithArray(new Simple[]{a, b});

        List<Traverser.NodeVisit> visits = walk(root);
        // Nodes: root object, the array, and the two Simple instances
        assertEquals(4, visits.size());

        Set<Object> nodes = visitedObjects(visits);
        assertTrue(nodes.contains(root));
        assertTrue(nodes.contains(root.elements));
        assertTrue(nodes.contains(a));
        assertTrue(nodes.contains(b));
    }

    @Test
    public void testTraverseObjectContainingCollectionField() {
        Simple a = new Simple(1, "A");
        Simple b = new Simple(2, "B");
        WithCollection root = new WithCollection(Arrays.asList(a, b));

        List<Traverser.NodeVisit> visits = walk(root);
        // Nodes: root, the list, and the two Simple objects
        assertEquals(4, visits.size());

        Set<Object> nodes = visitedObjects(visits);
        assertTrue(nodes.contains(root));
        // The collection instance (ArrayList) is visited
        assertTrue(nodes.stream().anyMatch(o -> o instanceof List));
        assertTrue(nodes.contains(a));
        assertTrue(nodes.contains(b));
    }

    @Test
    public void testTraverseObjectContainingMapField() {
        Simple key = new Simple(10, "key");
        Simple value = new Simple(20, "value");
        Map<Object, Simple> map = new HashMap<>();
        map.put(key, value);
        WithMap root = new WithMap(map);

        List<Traverser.NodeVisit> visits = walk(root);
        // Nodes: root, the map, the key object, and the value object
        assertEquals(4, visits.size());

        Set<Object> nodes = visitedObjects(visits);
        assertTrue(nodes.contains(root));
        assertTrue(nodes.contains(map));
        assertTrue(nodes.contains(key));
        assertTrue(nodes.contains(value));
    }
}
