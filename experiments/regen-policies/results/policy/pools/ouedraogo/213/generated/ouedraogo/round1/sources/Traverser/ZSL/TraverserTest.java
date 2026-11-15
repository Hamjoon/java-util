package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class TraverserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /** Simple bean used for building object graphs */
    public static class SimpleNode {
        public SimpleNode ref;
        public String text;
        public int number;

        public SimpleNode() {}

        public SimpleNode(String text, int number) {
            this.text = text;
            this.number = number;
        }
    }

    private List<Object> visitedNodes;
    private Consumer<Traverser.NodeVisit> collector;

    @Before
    public void setUp() {
        visitedNodes = new ArrayList<>();
        collector = visit -> visitedNodes.add(visit.getNode());
    }

    /*** 1. Argument validation ***/

    @Test
    public void testTraverseWithNullVisitorThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("visitor cannot be null");
        Traverser.traverse(new Object(), null, null);
    }

    @Test
    public void testLegacyTraverseWithNullVisitorThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("visitor cannot be null");
        Traverser.traverse(new Object(), (Traverser.Visitor) null);
    }

    /*** 2. Null root handling – nothing should be visited ***/

    @Test
    public void testTraverseRootNullNoVisits() {
        Traverser.traverse(null, collector, null);
        assertTrue("No nodes should be visited when root is null", visitedNodes.isEmpty());
    }

    /*** 3. Basic graph traversal – includes root and children, respects cycles ***/

    @Test
    public void testTraverseSimpleGraphWithCycle() {
        SimpleNode a = new SimpleNode("A", 1);
        SimpleNode b = new SimpleNode("B", 2);
        a.ref = b;
        b.ref = a; // create a cycle

        Traverser.traverse(a, collector, null);

        // Both objects should be visited exactly once
        assertEquals("Two distinct nodes should be visited", 2, visitedNodes.size());
        assertTrue("Node 'a' should be visited", visitedNodes.contains(a));
        assertTrue("Node 'b' should be visited", visitedNodes.contains(b));
    }

    /*** 4. Skipping classes – e.g., String.class ***/

    @Test
    public void testSkipClassPreventsVisitingSkippedObjects() {
        SimpleNode node = new SimpleNode("skipMe", 10);
        // The string instance referenced by the field should be skipped
        Set<Class<?>> skip = new HashSet<>(Collections.singletonList(String.class));

        Traverser.traverse(node, collector, skip);

        // The node itself is visited, but the String instance is not
        assertEquals("Only the SimpleNode should be visited", 1, visitedNodes.size());
        assertTrue("Visited node must be the SimpleNode instance", visitedNodes.contains(node));
    }

    /*** 5. Array processing – object array ***/

    @Test
    public void testTraverseObjectArray() {
        SimpleNode x = new SimpleNode("X", 100);
        SimpleNode y = new SimpleNode("Y", 200);
        SimpleNode[] array = new SimpleNode[]{x, null, y, x}; // includes duplicate and null

        Traverser.traverse(array, collector, null);

        // Array itself + two distinct objects = 3 visits
        assertEquals("Array and two distinct elements should be visited", 3, visitedNodes.size());
        assertTrue(visitedNodes.contains(array));
        assertTrue(visitedNodes.contains(x));
        assertTrue(visitedNodes.contains(y));
    }

    /*** 6. Primitive array – should not expand elements ***/

    @Test
    public void testTraversePrimitiveArray() {
        int[] ints = new int[]{1, 2, 3};

        Traverser.traverse(ints, collector, null);

        // Only the primitive array itself is visited
        assertEquals("Only the primitive array node should be visited", 1, visitedNodes.size());
        assertTrue(visitedNodes.contains(ints));
    }

    /*** 7. Collection processing – List ***/

    @Test
    public void testTraverseCollection() {
        SimpleNode a = new SimpleNode("A", 1);
        SimpleNode b = new SimpleNode("B", 2);
        List<SimpleNode> list = new ArrayList<>(Arrays.asList(a, b, null, a));

        Traverser.traverse(list, collector, null);

        // List node + two distinct elements = 3 visits
        assertEquals("List and two distinct elements should be visited", 3, visitedNodes.size());
        assertTrue(visitedNodes.contains(list));
        assertTrue(visitedNodes.contains(a));
        assertTrue(visitedNodes.contains(b));
    }

    /*** 8. Map processing – keys and values ***/

    @Test
    public void testTraverseMap() {
        SimpleNode key = new SimpleNode("key", 0);
        SimpleNode value = new SimpleNode("value", 0);
        Map<SimpleNode, SimpleNode> map = new HashMap<>();
        map.put(key, value);
        map.put(null, key); // null key should be ignored, but value (key) already present

        Traverser.traverse(map, collector, null);

        // Map node + two distinct SimpleNode objects = 3 visits
        assertEquals("Map and two distinct nodes should be visited", 3, visitedNodes.size());
        assertTrue(visitedNodes.contains(map));
        assertTrue(visitedNodes.contains(key));
        assertTrue(visitedNodes.contains(value));
    }

    /*** 9. Legacy API – Visitor interface ***/

    @Test
    public void testLegacyVisitorIsInvoked() {
        SimpleNode root = new SimpleNode("root", 0);
        List<Object> legacyVisited = new ArrayList<>();
        Traverser.Visitor visitor = legacyVisited::add;

        Traverser.traverse(root, visitor);

        // Legacy traversal should visit the root only (no children)
        assertEquals("Legacy visitor should receive exactly one node", 1, legacyVisited.size());
        assertSame(root, legacyVisited.get(0));
    }

    /*** 10. NodeVisit immutability – getFields returns unmodifiable map ***/

    @Test
    public void testNodeVisitFieldsAreUnmodifiable() {
        SimpleNode node = new SimpleNode("immutable", 999);
        List<Traverser.NodeVisit> nodeVisits = new ArrayList<>();
        Consumer<Traverser.NodeVisit> capture = nodeVisits::add;

        Traverser.traverse(node, capture, null);
        assertFalse("At least one NodeVisit should have been captured", nodeVisits.isEmpty());

        Traverser.NodeVisit visit = nodeVisits.get(0);
        Map<Field, Object> fields = visit.getFields();

        thrown.expect(UnsupportedOperationException.class);
        fields.put(null, null); // attempt to modify should throw
    }

    /*** 11. Ensure primitive fields are not traversed further ***/

    @Test
    public void testPrimitiveFieldsAreIgnoredInTraversal() {
        // SimpleNode has an int field; ensure traversal does not treat the int as a node
        SimpleNode node = new SimpleNode("prim", 42);
        Traverser.traverse(node, collector, null);

        // Only the node itself should be visited (no extra nodes for the primitive)
        assertEquals("Only the SimpleNode should be visited", 1, visitedNodes.size());
        assertTrue(visitedNodes.contains(node));
    }
}
