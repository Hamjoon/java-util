package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Traverser}.
 *
 * The suite covers:
 *   • Basic contract of {@link Traverser.NodeVisit}
 *   • All public static {@code traverse} overloads (including deprecated ones)
 *   • Edge cases: null arguments, primitive arrays, class‑skip logic, cycle detection,
 *                 unmodifiable field map, null root, empty skip set, etc.
 */
public class TraverserTest {

    /** Helper objects used throughout the tests */
    private SimpleNode simpleNode;
    private CycleA cycleA;
    private CycleB cycleB;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        simpleNode = new SimpleNode(42, "hello");
        cycleA = new CycleA();
        cycleB = new CycleB();
        cycleA.b = cycleB;
        cycleB.a = cycleA;   // creates a two‑node cycle
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Traverser.NodeVisit                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNodeVisitBasicContract() throws Exception {
        Map<Field, Object> fieldMap = new HashMap<>();
        Field f = SimpleNode.class.getDeclaredField("primitive");
        f.setAccessible(true);
        fieldMap.put(f, 42);

        Traverser.NodeVisit visit = new Traverser.NodeVisit(simpleNode, fieldMap);

        // verify getters
        assertSame(simpleNode, visit.getNode());
        assertEquals(SimpleNode.class, visit.getNodeClass());

        Map<Field, Object> returnedMap = visit.getFields();
        assertEquals(1, returnedMap.size());
        assertTrue(returnedMap.containsKey(f));
        assertEquals(42, returnedMap.get(f));

        // map must be unmodifiable
        thrown.expect(UnsupportedOperationException.class);
        returnedMap.put(f, 99);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for Traverser.traverse (modern API)                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTraverseNullVisitorThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("visitor cannot be null");
        Traverser.traverse(new Object(), null, null);
    }

    @Test
    public void testTraverseNullRootDoesNotCallVisitor() {
        final List<Object> visited = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = nv -> visited.add(nv.getNode());

        Traverser.traverse(null, collector, null);
        assertTrue("Visitor must not be called for null root", visited.isEmpty());
    }

    @Test
    public void testTraverseSimpleObjectVisitsAllNodes() {
        final List<Object> visited = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = nv -> visited.add(nv.getNode());

        Traverser.traverse(simpleNode, collector, null);

        // Expected nodes: the root SimpleNode and its String field ("hello")
        assertTrue(visited.contains(simpleNode));
        assertTrue(visited.contains("hello"));
        // primitive field is not a separate node
        assertEquals(2, visited.size());
    }

    @Test
    public void testTraverseWithClassSkip() {
        final List<Object> visited = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = nv -> visited.add(nv.getNode());

        // Skip String.class – the string field should not be visited
        Set<Class<?>> skip = new HashSet<>(Collections.singletonList(String.class));
        Traverser.traverse(simpleNode, collector, skip);

        assertTrue(visited.contains(simpleNode));
        assertFalse("String instance should be skipped", visited.contains("hello"));
        assertEquals(1, visited.size());
    }

    @Test
    public void testTraverseArrayOfObjects() {
        SimpleNode a = new SimpleNode(1, "a");
        SimpleNode b = new SimpleNode(2, "b");
        SimpleNode[] array = new SimpleNode[]{a, null, b};

        final List<Object> visited = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = nv -> visited.add(nv.getNode());

        Traverser.traverse(array, collector, null);

        // The array itself + both non‑null elements + their String fields
        assertTrue(visited.contains(array));
        assertTrue(visited.contains(a));
        assertTrue(visited.contains(b));
        assertTrue(visited.contains("a"));
        assertTrue(visited.contains("b"));
        // ensure null element is ignored
        assertEquals(5, visited.size());
    }

    @Test
    public void testTraversePrimitiveArray() {
        int[] ints = {1, 2, 3};

        final List<Object> visited = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = nv -> visited.add(nv.getNode());

        Traverser.traverse(ints, collector, null);

        // Primitive array is visited, but its elements are NOT (they're primitives)
        assertTrue(visited.contains(ints));
        assertEquals(1, visited.size());
    }

    @Test
    public void testTraverseCollection() {
        List<Object> list = new ArrayList<>();
        list.add(simpleNode);
        list.add("world");
        list.add(null); // null should be ignored

        final List<Object> visited = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = nv -> visited.add(nv.getNode());

        Traverser.traverse(list, collector, null);

        assertTrue(visited.contains(list));
        assertTrue(visited.contains(simpleNode));
        assertTrue(visited.contains("world"));
        // primitive field inside simpleNode is not a separate node
        assertEquals(3, visited.size());
    }

    @Test
    public void testTraverseMap() {
        Map<Object, Object> map = new HashMap<>();
        map.put("key", simpleNode);
        map.put(simpleNode, "value");
        map.put(null, "ignored"); // null key ignored
        map.put("ignored", null); // null value ignored

        final List<Object> visited = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = nv -> visited.add(nv.getNode());

        Traverser.traverse(map, collector, null);

        // Expected: the map itself, both keys and values that are non‑null
        assertTrue(visited.contains(map));
        assertTrue(visited.contains("key"));
        assertTrue(visited.contains(simpleNode));
        assertTrue(visited.contains("value"));
        assertEquals(4, visited.size());
    }

    @Test
    public void testTraverseCycleDetection() {
        final List<Object> visited = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = nv -> visited.add(nv.getNode());

        Traverser.traverse(cycleA, collector, null);

        // CycleA and CycleB should each be visited exactly once
        assertTrue(visited.contains(cycleA));
        assertTrue(visited.contains(cycleB));
        assertEquals(2, visited.size());
    }

    @Test
    public void testTraverseWithEmptySkipSet() {
        final List<Object> visited = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = nv -> visited.add(nv.getNode());

        Traverser.traverse(simpleNode, collector, Collections.emptySet());

        // Same result as passing null for skip set
        assertTrue(visited.contains(simpleNode));
        assertTrue(visited.contains("hello"));
        assertEquals(2, visited.size());
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for deprecated Traverser.traverse overloads (Visitor)            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testLegacyTraverseVisitor() {
        final List<Object> visited = new ArrayList<>();
        Traverser.Visitor legacyVisitor = visited::add;

        Traverser.traverse(simpleNode, legacyVisitor);

        // Should behave like the modern API with no skip set
        assertTrue(visited.contains(simpleNode));
        assertTrue(visited.contains("hello"));
        assertEquals(2, visited.size());
    }

    @Test
    public void testLegacyTraverseVisitorWithSkipArray() {
        final List<Object> visited = new ArrayList<>();
        Traverser.Visitor legacyVisitor = visited::add;

        // Skip String.class via the Class<?>[] overload
        Traverser.traverse(simpleNode, new Class<?>[]{String.class}, legacyVisitor);

        assertTrue(visited.contains(simpleNode));
        assertFalse("String should be skipped", visited.contains("hello"));
        assertEquals(1, visited.size());
    }

    @Test
    public void testLegacyTraverseNullVisitorThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("visitor cannot be null");
        Traverser.traverse(new Object(), (Traverser.Visitor) null);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper classes used in tests                                          */
    /* --------------------------------------------------------------------- */

    /** Simple POJO with primitive and reference fields */
    static class SimpleNode {
        int primitive;
        String ref;

        SimpleNode(int primitive, String ref) {
            this.primitive = primitive;
            this.ref = ref;
        }
    }

    /** Two‑node cyclic structure */
    static class CycleA {
        CycleB b;
    }

    static class CycleB {
        CycleA a;
    }
}
