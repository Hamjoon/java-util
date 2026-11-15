package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Traverser}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Argument validation for the public {@code traverse} overloads.</li>
 *   <li>Correct visitation of objects in arrays, collections, maps and plain POJOs.</li>
 *   <li>Cycle detection – a graph that references itself is traversed only once per object.</li>
 *   <li>Skipping of user‑supplied classes.</li>
 *   <li>Behaviour of the {@link Traverser.NodeVisit} API (immutability, node class reporting).</li>
 *   <li>Handling of null roots and primitive arrays.</li>
 * </ul>
 * </p>
 */
public class TraverserTest {

    /** Collector used by the tests to capture each {@link NodeVisit} emitted by Traverser. */
    private List<Traverser.NodeVisit> visits;

    @Before
    public void setUp() {
        visits = new ArrayList<>();
    }

    /** Helper consumer that stores every NodeVisit into {@link #visits}. */
    private Consumer<Traverser.NodeVisit> collector() {
        return visits::add;
    }

    /* -------------------------------------------------------------------------
     *  Argument validation tests
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testTraverse_NullVisitor_Throws() {
        Traverser.traverse(new Object(), null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLegacyTraverse_NullVisitor_Throws() {
        Traverser.traverse(new Object(), (Traverser.Visitor) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLegacyTraverse_WithArray_NullVisitor_Throws() {
        Traverser.traverse(new Object(), (Class<?>[]) null, null);
    }

    /* -------------------------------------------------------------------------
     *  Null root handling – no visits should be emitted
     * ---------------------------------------------------------------------- */

    @Test
    public void testTraverse_NullRoot_ProducesNoVisits() {
        Traverser.traverse(null, collector(), null);
        assertTrue("No nodes should be visited for a null root", visits.isEmpty());
    }

    /* -------------------------------------------------------------------------
     *  Simple object graph with a cycle – each object visited exactly once
     * ---------------------------------------------------------------------- */

    private static class NodeA {
        NodeB b;
        int primitive = 42;
    }

    private static class NodeB {
        String name = "bob";
        NodeA a; // creates a cycle back to NodeA
    }

    @Test
    public void testTraverse_SimpleCyclicGraph_VisitsEachObjectOnce() {
        NodeA a = new NodeA();
        NodeB b = new NodeB();
        a.b = b;
        b.a = a; // cycle

        Traverser.traverse(a, collector(), null);

        // Collect the actual node instances visited
        Set<Object> visitedObjects = new HashSet<>();
        for (Traverser.NodeVisit v : visits) {
            visitedObjects.add(v.getNode());
        }

        assertEquals("Exactly two distinct objects should be visited (A and B)", 2, visitedObjects.size());
        assertTrue("NodeA instance should have been visited", visitedObjects.contains(a));
        assertTrue("NodeB instance should have been visited", visitedObjects.contains(b));

        // Verify that the primitive field is present in the field map but not visited as a node
        Traverser.NodeVisit aVisit = findVisitFor(a);
        assertNotNull("NodeVisit for NodeA must exist", aVisit);
        Field primitiveField = findFieldByName(NodeA.class, "primitive");
        assertEquals(42, aVisit.getFields().get(primitiveField));
    }

    /* -------------------------------------------------------------------------
     *  Skipping of user supplied classes (e.g., String)
     * ---------------------------------------------------------------------- */

    private static class WithString {
        String text = "skip-me";
        Integer number = 7;
    }

    @Test
    public void testTraverse_WithSkipClasses_DoesNotVisitSkippedObjects() {
        WithString root = new WithString();

        Set<Class<?>> skip = new HashSet<>();
        skip.add(String.class); // we want to skip all String instances

        Traverser.traverse(root, collector(), skip);

        // Ensure that no String instance appears among the visited nodes
        for (Traverser.NodeVisit v : visits) {
            assertFalse("String instances must be skipped", v.getNode() instanceof String);
        }

        // The Integer field should be visited
        boolean integerVisited = visits.stream()
                .anyMatch(v -> v.getNode() instanceof Integer && ((Integer) v.getNode()) == 7);
        assertTrue("Integer field should be visited", integerVisited);
    }

    /* -------------------------------------------------------------------------
     *  Traversal of an array of objects (non‑primitive component type)
     * ---------------------------------------------------------------------- */

    private static class Element {
        String payload;
        Element(String payload) { this.payload = payload; }
    }

    @Test
    public void testTraverse_ObjectArray_VisitsAllElements() {
        Element e1 = new Element("one");
        Element e2 = new Element("two");
        Element[] array = new Element[] { e1, e2 };

        Traverser.traverse(array, collector(), null);

        // The root array itself is visited
        assertTrue("Root array must be visited", containsNode(array));

        // Both elements must be visited exactly once
        assertTrue("First element must be visited", containsNode(e1));
        assertTrue("Second element must be visited", containsNode(e2));

        // No extra visits
        assertEquals("Exactly three nodes (array + 2 elements) should be visited", 3, visits.size());
    }

    /* -------------------------------------------------------------------------
     *  Traversal of primitive array – elements are NOT visited
     * ---------------------------------------------------------------------- */

    @Test
    public void testTraverse_PrimitiveArray_OnlyRootVisited() {
        int[] ints = new int[] {1, 2, 3};

        Traverser.traverse(ints, collector(), null);

        assertEquals("Only the primitive array object itself should be visited", 1, visits.size());
        assertSame("Visited node must be the original array", ints, visits.get(0).getNode());
    }

    /* -------------------------------------------------------------------------
     *  Traversal of collections and maps – keys, values and nested objects visited
     * ---------------------------------------------------------------------- */

    @Test
    public void testTraverse_CollectionAndMap_VisitsAllContainedObjects() {
        Element key = new Element("key");
        Element value = new Element("value");
        Map<Element, Element> map = new HashMap<>();
        map.put(key, value);

        List<Object> list = new ArrayList<>();
        list.add(map);
        list.add("plain-string"); // a String should be visited because we do not skip it here

        Traverser.traverse(list, collector(), null);

        // Root list
        assertTrue("Root list must be visited", containsNode(list));
        // The map itself
        assertTrue("Map must be visited", containsNode(map));
        // Both key and value objects
        assertTrue("Map key must be visited", containsNode(key));
        assertTrue("Map value must be visited", containsNode(value));
        // The plain string (no skip) should be visited as a node
        assertTrue("String element must be visited", visits.stream()
                .anyMatch(v -> v.getNode() instanceof String && "plain-string".equals(v.getNode())));

        // Expected node count: list + map + key + value + string = 5
        assertEquals("Exactly five distinct nodes should be visited", 5, visits.size());
    }

    /* -------------------------------------------------------------------------
     *  NodeVisit API – immutability and node class reporting
     * ---------------------------------------------------------------------- */

    @Test
    public void testNodeVisit_ImmutableFieldsMap() {
        Element e = new Element("immutable");
        Traverser.traverse(e, collector(), null);
        Traverser.NodeVisit visit = findVisitFor(e);
        assertNotNull("NodeVisit for element must exist", visit);

        // The map returned by getFields() must be unmodifiable
        Map<Field, Object> fields = visit.getFields();
        try {
            fields.put(null, null);
            fail("Fields map should be unmodifiable and throw UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        // getNodeClass must match the runtime class of the node
        assertEquals("Node class reported by NodeVisit must match actual class",
                Element.class, visit.getNodeClass());
    }

    /* -------------------------------------------------------------------------
     *  Legacy API – ensure it forwards correctly to the modern Consumer API
     * ---------------------------------------------------------------------- */

    @Test
    public void testLegacyTraverse_DelegatesToConsumer() {
        Element e = new Element("legacy");
        Traverser.Visitor legacyVisitor = new Traverser.Visitor() {
            @Override
            public void process(Object o) {
                // simply add the visited node to our collection
                visits.add(new Traverser.NodeVisit(o, Collections.emptyMap()));
            }
        };

        Traverser.traverse(e, legacyVisitor);
        assertEquals("Legacy traversal should produce exactly one visit (the root)", 1, visits.size());
        assertSame("Visited node must be the element passed to legacy API", e, visits.get(0).getNode());
    }

    /* -------------------------------------------------------------------------
     *  Utility methods used by the tests
     * ---------------------------------------------------------------------- */

    /** Finds the {@link NodeVisit} for a given node instance, or {@code null} if not found. */
    private Traverser.NodeVisit findVisitFor(Object node) {
        for (Traverser.NodeVisit v : visits) {
            if (v.getNode() == node) {
                return v;
            }
        }
        return null;
    }

    /** Returns {@code true} if the supplied node instance appears in the captured visits. */
    private boolean containsNode(Object node) {
        return findVisitFor(node) != null;
    }

    /** Returns the {@link Field} object for a given class/name pair, making it accessible. */
    private static Field findFieldByName(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Field '" + name + "' not found in " + clazz, e);
        }
    }
}
