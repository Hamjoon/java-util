package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Test;

/**
 * Comprehensive JUnit4 tests for {@link Traverser}.
 */
public class TraverserTest {

    /* ------------------------------------------------------------
     * Helper POJOs used in the tests
     * ------------------------------------------------------------ */

    /** Simple POJO with primitive and reference fields. */
    public static class Simple {
        private int id;
        private String name;
        private Simple child;

        public Simple(int id, String name, Simple child) {
            this.id = id;
            this.name = name;
            this.child = child;
        }
    }

    /** Two objects that reference each other – used to test cycle detection. */
    public static class CycleA {
        CycleB b;
        public CycleA(CycleB b) { this.b = b; }
    }

    public static class CycleB {
        CycleA a;
        public CycleB(CycleA a) { this.a = a; }
    }

    /* ------------------------------------------------------------
     * Tests for the modern API (Consumer<NodeVisit>)
     * ------------------------------------------------------------ */

    @Test
    public void testTraverseNullVisitorThrows() {
        try {
            Traverser.traverse(new Object(), null, null);
            fail("Expected IllegalArgumentException for null visitor");
        } catch (IllegalArgumentException e) {
            assertEquals("visitor cannot be null", e.getMessage());
        }
    }

    @Test
    public void testTraverseNullRootProducesNoVisits() {
        List<Traverser.NodeVisit> visited = new ArrayList<>();
        Traverser.traverse(null, visited::add, null);
        assertTrue("No nodes should be visited for a null root", visited.isEmpty());
    }

    @Test
    public void testTraverseSimpleObjectVisitsRootAndFields() {
        Simple leaf = new Simple(2, "leaf", null);
        Simple root = new Simple(1, "root", leaf);

        List<Traverser.NodeVisit> visited = new ArrayList<>();
        Traverser.traverse(root, visited::add, null);

        // Expect two nodes: root and leaf
        assertEquals(2, visited.size());

        // Verify root visit
        Traverser.NodeVisit rootVisit = visited.stream()
                .filter(v -> v.getNode() == root)
                .findFirst()
                .orElse(null);
        assertNotNull("Root node must be visited", rootVisit);
        assertEquals(Simple.class, rootVisit.getNodeClass());

        // All declared fields must be present
        Map<Field, Object> rootFields = rootVisit.getFields();
        assertEquals(3, rootFields.size()); // id, name, child
        // Check values via reflection (field may be private)
        for (Map.Entry<Field, Object> entry : rootFields.entrySet()) {
            entry.getKey().setAccessible(true);
        }
        assertEquals(1, rootFields.get(getField(Simple.class, "id")));
        assertEquals("root", rootFields.get(getField(Simple.class, "name")));
        assertEquals(leaf, rootFields.get(getField(Simple.class, "child")));

        // Verify leaf visit
        Traverser.NodeVisit leafVisit = visited.stream()
                .filter(v -> v.getNode() == leaf)
                .findFirst()
                .orElse(null);
        assertNotNull("Leaf node must be visited", leafVisit);
        assertEquals(Simple.class, leafVisit.getNodeClass());
        assertEquals(3, leafVisit.getFields().size());
        assertEquals(2, leafVisit.getFields().get(getField(Simple.class, "id")));
        assertEquals("leaf", leafVisit.getFields().get(getField(Simple.class, "name")));
        assertNull(leafVisit.getFields().get(getField(Simple.class, "child")));
    }

    @Test
    public void testTraverseArrayElementsAreVisited() {
        Simple a = new Simple(10, "a", null);
        Simple b = new Simple(20, "b", null);
        Simple[] array = new Simple[]{a, b};

        List<Traverser.NodeVisit> visited = new ArrayList<>();
        Traverser.traverse(array, visited::add, null);

        // Expect 3 visits: the array itself + two elements
        assertEquals(3, visited.size());

        // Verify that the array node is present
        Traverser.NodeVisit arrayVisit = visited.stream()
                .filter(v -> v.getNode() == array)
                .findFirst()
                .orElse(null);
        assertNotNull(arrayVisit);
        assertTrue(arrayVisit.getNodeClass().isArray());

        // Verify element visits
        assertTrue(visited.stream().anyMatch(v -> v.getNode() == a));
        assertTrue(visited.stream().anyMatch(v -> v.getNode() == b));
    }

    @Test
    public void testTraverseCollectionElementsAreVisited() {
        Simple a = new Simple(1, "a", null);
        Simple b = new Simple(2, "b", null);
        List<Simple> list = new ArrayList<>();
        list.add(a);
        list.add(b);

        List<Traverser.NodeVisit> visited = new ArrayList<>();
        Traverser.traverse(list, visited::add, null);

        // Expect 3 visits: the collection itself + two elements
        assertEquals(3, visited.size());

        assertTrue(visited.stream().anyMatch(v -> v.getNode() == list));
        assertTrue(visited.stream().anyMatch(v -> v.getNode() == a));
        assertTrue(visited.stream().anyMatch(v -> v.getNode() == b));
    }

    @Test
    public void testTraverseMapKeysAndValuesAreVisited() {
        Simple key = new Simple(99, "key", null);
        Simple value = new Simple(100, "value", null);
        Map<Simple, Simple> map = new HashMap<>();
        map.put(key, value);

        List<Traverser.NodeVisit> visited = new ArrayList<>();
        Traverser.traverse(map, visited::add, null);

        // Expect 3 visits: the map itself, the key, and the value
        assertEquals(3, visited.size());

        assertTrue(visited.stream().anyMatch(v -> v.getNode() == map));
        assertTrue(visited.stream().anyMatch(v -> v.getNode() == key));
        assertTrue(visited.stream().anyMatch(v -> v.getNode() == value));
    }

    @Test
    public void testTraverseDetectsAndAvoidsCycles() {
        CycleA a = new CycleA(null);
        CycleB b = new CycleB(a);
        a.b = b; // create cycle

        List<Traverser.NodeVisit> visited = new ArrayList<>();
        Traverser.traverse(a, visited::add, null);

        // Expect exactly two nodes (a and b) – no infinite loop
        assertEquals(2, visited.size());

        assertTrue(visited.stream().anyMatch(v -> v.getNode() == a));
        assertTrue(visited.stream().anyMatch(v -> v.getNode() == b));
    }

    @Test
    public void testTraverseWithClassSkipping() {
        Simple leaf = new Simple(5, "leaf", null);
        Simple root = new Simple(6, "root", leaf);
        // root also contains a String field (name) which should be skipped
        Set<Class<?>> skip = new HashSet<>();
        skip.add(String.class); // skip all Strings

        List<Traverser.NodeVisit> visited = new ArrayList<>();
        Traverser.traverse(root, visited::add, skip);

        // Visits: root and leaf only (String fields are not visited as separate nodes)
        assertEquals(2, visited.size());

        // Verify that no String instance appears as a visited node
        for (Traverser.NodeVisit v : visited) {
            assertFalse(v.getNode() instanceof String);
        }
    }

    @Test
    public void testNodeVisitFieldsMapIsUnmodifiable() {
        Simple s = new Simple(1, "test", null);
        List<Traverser.NodeVisit> visited = new ArrayList<>();
        Traverser.traverse(s, visited::add, null);
        Traverser.NodeVisit nv = visited.get(0);
        Map<Field, Object> fields = nv.getFields();
        try {
            fields.put(null, null);
            fail("Expected UnsupportedOperationException when modifying fields map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /* ------------------------------------------------------------
     * Tests for the legacy API (Visitor) – they delegate to the modern API.
     * ------------------------------------------------------------ */

    @Test
    public void testLegacyTraverseVisitorNullThrows() {
        try {
            Traverser.traverse(new Object(), (Traverser.Visitor) null);
            fail("Expected IllegalArgumentException for null Visitor");
        } catch (IllegalArgumentException e) {
            assertEquals("visitor cannot be null", e.getMessage());
        }
    }

    @Test
    public void testLegacyTraverseVisitsAllNodes() {
        Simple leaf = new Simple(2, "leaf", null);
        Simple root = new Simple(1, "root", leaf);
        List<Object> visited = new ArrayList<>();

        Traverser.Visitor visitor = visited::add;
        Traverser.traverse(root, visitor);

        // Legacy API only supplies the raw node objects (no field details)
        // Expect two nodes: root and leaf
        assertEquals(2, visited.size());
        assertTrue(visited.contains(root));
        assertTrue(visited.contains(leaf));
    }

    @Test
    public void testLegacyTraverseWithSkipArray() {
        Simple leaf = new Simple(2, "leaf", null);
        Simple root = new Simple(1, "root", leaf);
        List<Object> visited = new ArrayList<>();

        Traverser.Visitor visitor = visited::add;
        Traverser.traverse(root, new Class<?>[]{String.class}, visitor);

        // String fields are skipped, but the objects themselves (root, leaf) are still visited
        assertEquals(2, visited.size());
        assertTrue(visited.contains(root));
        assertTrue(visited.contains(leaf));
    }

    /* ------------------------------------------------------------
     * Utility method to fetch a declared field by name (used in assertions)
     * ------------------------------------------------------------ */
    private static Field getField(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Field '" + name + "' not found in class " + clazz.getName(), e);
        }
    }
}
