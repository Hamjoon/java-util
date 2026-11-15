package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link Traverser}.  The tests exercise the
 * traversal of primitive and reference fields, arrays, collections,
 * maps, cycle detection and the “skip” functionality.
 */
public class TraverserTest {

    /** Simple visitor that records every object passed to {@code process}. */
    private static class RecordingVisitor implements Traverser.Visitor {
        final List<Object> visited = new ArrayList<>();

        @Override
        public void process(Object o) {
            visited.add(o);
        }
    }

    /** A class that only contains primitive fields – nothing else to traverse. */
    private static class SimpleClass {
        private int id = 1;
        private double value = 3.14;
    }

    /** A class that contains a single reference field. */
    private static class RefClass {
        private String text;

        RefClass(String text) {
            this.text = text;
        }
    }

    /** A node that can form a linked‑list / graph, used for cycle detection tests. */
    private static class Node {
        private Node next;
        private String label;

        Node(String label) {
            this.label = label;
        }

        void setNext(Node n) {
            this.next = n;
        }
    }

    /** Test that a plain object with only primitive fields is visited exactly once. */
    @Test
    public void testTraverseSimpleObject() {
        SimpleClass root = new SimpleClass();
        RecordingVisitor visitor = new RecordingVisitor();

        Traverser.traverse(root, visitor);

        assertEquals("Only the root object should be visited", 1, visitor.visited.size());
        assertSame("Visited object must be the root instance", root, visitor.visited.get(0));
    }

    /** Test traversal of an object that holds a reference field (String). */
    @Test
    public void testTraverseObjectWithReferenceField() {
        RefClass root = new RefClass("hello");
        RecordingVisitor visitor = new RecordingVisitor();

        Traverser.traverse(root, visitor);

        assertEquals("Root and its String field should be visited", 2, visitor.visited.size());
        assertSame(root, visitor.visited.get(0));
        assertEquals("hello", visitor.visited.get(1));
    }

    /** Verify that cycles are handled – each object is visited only once. */
    @Test
    public void testTraverseCyclicGraph() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.setNext(b);
        b.setNext(a);               // creates a cycle A -> B -> A

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(a, visitor);

        // Expected order (BFS): a, b
        assertEquals("Two distinct nodes should be visited", 2, visitor.visited.size());
        assertSame(a, visitor.visited.get(0));
        assertSame(b, visitor.visited.get(1));
    }

    /** Test traversal of an object array containing reference elements. */
    @Test
    public void testTraverseObjectArray() {
        String[] array = new String[] { "one", "two", "three" };
        RecordingVisitor visitor = new RecordingVisitor();

        Traverser.traverse(array, visitor);

        // Expected: the array itself, then each element in insertion order.
        assertEquals("Array + 3 elements", 4, visitor.visited.size());
        assertSame(array, visitor.visited.get(0));
        assertEquals("one", visitor.visited.get(1));
        assertEquals("two", visitor.visited.get(2));
        assertEquals("three", visitor.visited.get(3));
    }

    /** Primitive arrays should not cause traversal of their elements. */
    @Test
    public void testTraversePrimitiveArray() {
        int[] primitives = new int[] { 1, 2, 3 };
        RecordingVisitor visitor = new RecordingVisitor();

        Traverser.traverse(primitives, visitor);

        // Only the array object itself is visited.
        assertEquals("Only the primitive array itself", 1, visitor.visited.size());
        assertSame(primitives, visitor.visited.get(0));
    }

    /** Verify that collections are traversed and that null elements are ignored. */
    @Test
    public void testTraverseCollection() {
        List<Object> list = new LinkedList<>();
        list.add("alpha");
        list.add(null);
        list.add(new SimpleClass());

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(list, visitor);

        // Expected: the collection, then "alpha", then SimpleClass instance.
        assertEquals("Collection + 2 non‑null elements", 3, visitor.visited.size());
        assertSame(list, visitor.visited.get(0));
        assertEquals("alpha", visitor.visited.get(1));
        assertTrue(visitor.visited.get(2) instanceof SimpleClass);
    }

    /** Verify that maps are traversed – both keys and values are visited. */
    @Test
    public void testTraverseMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(map, visitor);

        // Expected: the map itself, then each key, then each value (order may vary per entry iteration)
        assertEquals("Map + 2 keys + 2 values", 5, visitor.visited.size());
        assertSame(map, visitor.visited.get(0));

        // Collect the remaining visited objects for easier verification
        List<Object> remainder = visitor.visited.subList(1, visitor.visited.size());
        assertTrue("Contains key \"one\"", remainder.contains("one"));
        assertTrue("Contains key \"two\"", remainder.contains("two"));
        assertTrue("Contains value 1", remainder.contains(1));
        assertTrue("Contains value 2", remainder.contains(2));
    }

    /** Test the skip‑class functionality – objects of the skipped type are not visited. */
    @Test
    public void testTraverseWithSkip() {
        RefClass root = new RefClass("skipMe");
        RecordingVisitor visitor = new RecordingVisitor();

        // Skip all String instances
        Traverser.traverse(root, new Class<?>[] { String.class }, visitor);

        // Only the RefClass instance should be visited; the String field is omitted.
        assertEquals("Only root should be visited when String is skipped", 1, visitor.visited.size());
        assertSame(root, visitor.visited.get(0));
    }

    /** Passing a null root should result in no processing. */
    @Test
    public void testTraverseNullRoot() {
        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(null, visitor);
        assertTrue("Visitor should not be called for null root", visitor.visited.isEmpty());
    }
}
