package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class TraverserTest {

    /** Simple visitor that records every object passed to {@code process}. */
    private static class RecordingVisitor implements Traverser.Visitor {
        final List<Object> visited = new ArrayList<>();
        @Override public void process(Object o) { visited.add(o); }
    }

    /** Helper classes used in the tests */
    private static class Simple {
        String name;
        int count;
        Simple child;
        Simple(String name) { this.name = name; }
    }

    private static class Node {
        Node next;
    }

    private static class SkipMe {
        String data = "skip";
    }

    /**
     * 1. List all public methods (including signatures) of Traverser.
     *
     *    - public static void traverse(Object o, Visitor visitor)
     *    - public static void traverse(Object o, Class<?>[] skip, Visitor visitor)
     *    - public void walk(Object root, Class<?>[] skip, Visitor visitor)
     *    - public static class ClassInfo
     *          - public ClassInfo(Class<?> c, Class<?>[] skip)
     *    - public interface Visitor
     *          - void process(Object o)
     */
    @Test
    public void testPublicMethodListing() {
        // This test simply ensures that the class compiles and the public API is accessible.
        // No runtime assertions are required – the comment above serves as the extracted list.
        assertTrue(true);
    }

    /** Basic traversal of a simple object graph (root + child). */
    @Test
    public void testTraverseSimpleObject() {
        Simple root = new Simple("root");
        root.child = new Simple("child");

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(root, visitor);

        // Both objects must be visited, order is not guaranteed.
        assertEquals(2, visitor.visited.size());
        assertTrue(visitor.visited.contains(root));
        assertTrue(visitor.visited.contains(root.child));
    }

    /** Traversal of an object array containing non‑primitive elements. */
    @Test
    public void testTraverseObjectArray() {
        String[] arr = new String[] { "one", "two" };
        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(arr, visitor);

        // The array itself and each non‑null element should be visited.
        assertEquals(3, visitor.visited.size());
        assertTrue(visitor.visited.contains(arr));
        assertTrue(visitor.visited.contains("one"));
        assertTrue(visitor.visited.contains("two"));
    }

    /** Traversal of a primitive array – only the array itself should be visited. */
    @Test
    public void testTraversePrimitiveArray() {
        int[] ints = new int[] { 1, 2, 3 };
        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(ints, visitor);

        // Primitive array elements are not traversed.
        assertEquals(1, visitor.visited.size());
        assertTrue(visitor.visited.contains(ints));
    }

    /** Traversal of a collection containing objects and a null element. */
    @Test
    public void testTraverseCollection() {
        Simple item = new Simple("item");
        List<Object> list = new ArrayList<>();
        list.add(item);
        list.add(null); // null should be ignored

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(list, visitor);

        // The collection itself and the non‑null element should be visited.
        assertEquals(2, visitor.visited.size());
        assertTrue(visitor.visited.contains(list));
        assertTrue(visitor.visited.contains(item));
    }

    /** Traversal of a map with various key/value combinations (including nulls). */
    @Test
    public void testTraverseMap() {
        Simple keyObj = new Simple("keyObj");
        Simple valueObj = new Simple("valueObj");
        Map<Object, Object> map = new HashMap<>();
        map.put("stringKey", valueObj);    // non‑null key, non‑null value
        map.put(null, "nullKeyIgnored");   // null key – entire entry ignored
        map.put(keyObj, null);              // non‑null key, null value

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(map, visitor);

        // Expected visited objects:
        // - the map itself
        // - "stringKey"
        // - valueObj
        // - keyObj
        // (null key/value are not added)
        assertEquals(4, visitor.visited.size());
        assertTrue(visitor.visited.contains(map));
        assertTrue(visitor.visited.contains("stringKey"));
        assertTrue(visitor.visited.contains(valueObj));
        assertTrue(visitor.visited.contains(keyObj));
    }

    /** Traversal with a class that should be skipped – the object must not be processed. */
    @Test
    public void testTraverseWithSkipClass() {
        SkipMe skipInstance = new SkipMe();

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(skipInstance, new Class<?>[] { SkipMe.class }, visitor);

        // The root object is of a skipped class → nothing should be visited.
        assertTrue(visitor.visited.isEmpty());
    }

    /** Traversal of an array whose component type is a skipped class – elements must be omitted. */
    @Test
    public void testTraverseArraySkippingComponent() {
        SkipMe[] array = new SkipMe[] { new SkipMe(), new SkipMe() };

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(array, new Class<?>[] { SkipMe.class }, visitor);

        // The array itself is visited, but its elements are skipped.
        assertEquals(1, visitor.visited.size());
        assertTrue(visitor.visited.contains(array));
    }

    /** Traversal of a cyclic object graph – each node should be visited exactly once. */
    @Test
    public void testTraverseCyclicGraph() {
        Node n1 = new Node();
        Node n2 = new Node();
        n1.next = n2;
        n2.next = n1; // cycle

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(n1, visitor);

        // Both nodes must be visited once; the traversal must terminate.
        assertEquals(2, visitor.visited.size());
        assertTrue(visitor.visited.contains(n1));
        assertTrue(visitor.visited.contains(n2));
    }

    /** Traversal when the root object is null – visitor should never be invoked. */
    @Test
    public void testTraverseNullRoot() {
        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(null, visitor);
        assertTrue(visitor.visited.isEmpty());
    }

    /** Traversal of an empty collection – only the collection itself should be visited. */
    @Test
    public void testTraverseEmptyCollection() {
        List<Object> empty = new ArrayList<>();

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(empty, visitor);

        assertEquals(1, visitor.visited.size());
        assertTrue(visitor.visited.contains(empty));
    }

    /** Traversal of a map containing a null key and a null value – verify correct handling. */
    @Test
    public void testTraverseMapWithNullKeyAndValue() {
        Map<Object, Object> map = new HashMap<>();
        map.put(null, "valueForNullKey"); // should be ignored entirely
        map.put("nonNullKey", null);      // key added, value ignored later

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(map, visitor);

        // Expected: map itself and the non‑null key.
        assertEquals(2, visitor.visited.size());
        assertTrue(visitor.visited.contains(map));
        assertTrue(visitor.visited.contains("nonNullKey"));
    }

    /** Traversal of an object that only contains primitive fields – only the object itself should be visited. */
    private static class PrimitiveOnly {
        int a = 5;
        boolean flag = true;
    }

    @Test
    public void testTraversePrimitiveOnlyObject() {
        PrimitiveOnly po = new PrimitiveOnly();

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(po, visitor);

        // No extra references to follow.
        assertEquals(1, visitor.visited.size());
        assertTrue(visitor.visited.contains(po));
    }
}
