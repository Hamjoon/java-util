package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.*;

public class TraverserTest {

    /**
     * Simple Visitor implementation that records each processed object
     * in a list preserving encounter order.
     */
    private static class RecordingVisitor implements Traverser.Visitor {
        private final List<Object> visited = new ArrayList<>();

        @Override
        public void process(Object o) {
            visited.add(o);
        }

        List<Object> getVisited() {
            return visited;
        }
    }

    /** Helper to create a cyclic object graph: A -> B -> A */
    private static class Node {
        Node next;
        String name;
        Node(String name) { this.name = name; }
    }

    /** Helper class with primitive and reference fields */
    private static class Mixed {
        int primitive = 42;
        String ref;
        Mixed(String ref) { this.ref = ref; }
    }

    private RecordingVisitor visitor;

    @Before
    public void setUp() {
        visitor = new RecordingVisitor();
    }

    @Test
    public void testSimpleObjectTraversal() {
        Mixed root = new Mixed("root");
        Traverser.traverse(root, visitor);
        List<Object> visited = visitor.getVisited();

        // root object itself must be visited
        assertTrue(visited.contains(root));
        // reference field must also be visited
        assertTrue(visited.contains("root"));
        // primitive field is ignored
        assertEquals(2, visited.size());
    }

    @Test
    public void testArrayTraversal_PrimitiveAndObjectElements() {
        String[] strings = new String[] { "a", null, "b" };
        Traverser.traverse(strings, visitor);
        List<Object> visited = visitor.getVisited();

        // array object itself
        assertTrue(visited.contains(strings));
        // non‑null elements
        assertTrue(visited.contains("a"));
        assertTrue(visited.contains("b"));
        // null element is ignored
        assertEquals(3, visited.size());
    }

    @Test
    public void testArrayTraversal_NonPrimitiveComponent() {
        Mixed[] mixedArray = new Mixed[] { new Mixed("x"), null, new Mixed("y") };
        Traverser.traverse(mixedArray, visitor);
        List<Object> visited = visitor.getVisited();

        // array object itself
        assertTrue(visited.contains(mixedArray));
        // each Mixed instance and its internal String reference
        assertTrue(visited.contains(mixedArray[0]));
        assertTrue(visited.contains("x"));
        assertTrue(visited.contains(mixedArray[2]));
        assertTrue(visited.contains("y"));
        // null element ignored
        assertEquals(6, visited.size());
    }

    @Test
    public void testCollectionTraversal() {
        List<Object> list = new ArrayList<>();
        list.add("first");
        list.add(null);
        list.add(new Mixed("second"));
        Traverser.traverse(list, visitor);
        List<Object> visited = visitor.getVisited();

        // collection itself
        assertTrue(visited.contains(list));
        // first element
        assertTrue(visited.contains("first"));
        // Mixed element and its internal String
        Mixed mixed = (Mixed) list.get(2);
        assertTrue(visited.contains(mixed));
        assertTrue(visited.contains("second"));
        // null element ignored
        assertEquals(5, visited.size());
    }

    @Test
    public void testMapTraversal() {
        Map<Object, Object> map = new HashMap<>();
        map.put("k1", "v1");
        map.put("k2", null);
        map.put(new Mixed("mk"), new Mixed("mv"));
        Traverser.traverse(map, visitor);
        List<Object> visited = visitor.getVisited();

        // map itself
        assertTrue(visited.contains(map));
        // first entry key/value
        assertTrue(visited.contains("k1"));
        assertTrue(visited.contains("v1"));
        // second entry key is processed, value null ignored
        assertTrue(visited.contains("k2"));
        // third entry key/value and their internal strings
        Mixed mk = (Mixed) map.keySet().stream()
                .filter(o -> o instanceof Mixed).findFirst().orElse(null);
        Mixed mv = (Mixed) map.get(mk);
        assertTrue(visited.contains(mk));
        assertTrue(visited.contains("mk"));
        assertTrue(visited.contains(mv));
        assertTrue(visited.contains("mv"));
        // total count
        assertEquals(9, visited.size());
    }

    @Test
    public void testSkipClasses() {
        // Skip Mixed class, so its instances and their fields are not visited
        Class<?>[] skip = new Class<?>[] { Mixed.class };
        Mixed root = new Mixed("skipMe");
        Traverser.traverse(root, skip, visitor);
        List<Object> visited = visitor.getVisited();

        // root object is of type Mixed and should be skipped entirely
        assertFalse(visited.contains(root));
        // its internal String should also be skipped
        assertFalse(visited.contains("skipMe"));
        // nothing else visited
        assertTrue(visited.isEmpty());
    }

    @Test
    public void testCycleDetection() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        b.next = a; // creates a cycle

        Traverser.traverse(a, visitor);
        List<Object> visited = visitor.getVisited();

        // Both nodes should be visited exactly once
        assertTrue(visited.contains(a));
        assertTrue(visited.contains(b));
        // No infinite loop – size must be 2 (the nodes) + 2 Strings (names)
        assertTrue(visited.contains("A"));
        assertTrue(visited.contains("B"));
        assertEquals(4, visited.size());
    }

    @Test
    public void testClassInfoCachingAndSkipLogic() throws Exception {
        // Use reflection to access the private cache via a Traverser instance
        Traverser traverser = new Traverser();

        // First call – cache miss, creates ClassInfo for Mixed
        Traverser.ClassInfo info1 = traverser.getClassInfo(Mixed.class, null);
        assertNotNull(info1);

        // Second call – should retrieve same instance from cache
        Traverser.ClassInfo info2 = traverser.getClassInfo(Mixed.class, null);
        assertSame(info1, info2);

        // Verify that when a skip class matches, _skip is true
        Traverser.ClassInfo skipInfo = traverser.getClassInfo(Mixed.class,
                new Class<?>[] { Object.class }); // Object is assignable from Mixed
        // Because we passed a new skip array, a new ClassInfo is created (cache miss)
        // but its _skip flag should be true. Use reflection to read the private field.
        java.lang.reflect.Field skipField = Traverser.ClassInfo.class.getDeclaredField("_skip");
        skipField.setAccessible(true);
        boolean isSkipped = (boolean) skipField.get(skipInfo);
        assertTrue(isSkipped);
    }
}
