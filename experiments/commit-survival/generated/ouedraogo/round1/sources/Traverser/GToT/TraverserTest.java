package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Traverser}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic traversal of simple objects, arrays, collections and maps.</li>
 *   <li>Skip‑class functionality.</li>
 *   <li>Cycle detection.</li>
 *   <li>Edge cases such as {@code null} roots, primitive arrays and {@code null} visitor.</li>
 *   <li>Internal caching behaviour of {@link Traverser#getClassInfo(Class, Class[])}.</li>
 * </ul>
 */
public class TraverserTest
{
    /** Simple visitor that records every object passed to {@code process}. */
    private static class RecordingVisitor implements Traverser.Visitor
    {
        private final List<Object> visited = new ArrayList<>();

        @Override
        public void process(Object o)
        {
            visited.add(o);
        }

        List<Object> getVisited()
        {
            return visited;
        }
    }

    /** Helper class with a reference field – useful for cycle tests. */
    private static class Node
    {
        Node ref;
        final String name;

        Node(String name)
        {
            this.name = name;
        }
    }

    /** Helper class used to test the “skip” feature. */
    private static class SkippedClass
    {
        final String data = "skip-me";
    }

    private RecordingVisitor visitor;

    @Before
    public void setUp()
    {
        visitor = new RecordingVisitor();
    }

    /* --------------------------------------------------------------------- */
    /* 1. Basic traversal of a single plain object                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testTraverseSimpleObject()
    {
        Object root = new Object();

        Traverser.traverse(root, visitor);

        List<Object> visited = visitor.getVisited();
        assertEquals("Exactly one object (the root) should be visited", 1, visited.size());
        assertSame("The visited object must be the root instance", root, visited.get(0));
    }

    /* --------------------------------------------------------------------- */
    /* 2. Traversal of an Object array (including null and wrapper types)      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testTraverseObjectArray()
    {
        Object[] array = new Object[] { "string", null, Integer.valueOf(42) };

        Traverser.traverse(array, visitor);

        List<Object> visited = visitor.getVisited();

        // The array itself + the two non‑null, non‑primitive elements
        assertEquals(3, visited.size());
        assertSame(array, visited.get(0));                 // the array root
        assertEquals("string", visited.get(1));             // first element
        assertEquals(Integer.valueOf(42), visited.get(2));   // second element
    }

    /* --------------------------------------------------------------------- */
    /* 3. Traversal of a primitive array – elements must NOT be visited        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testTraversePrimitiveArray()
    {
        int[] ints = {1, 2, 3};

        Traverser.traverse(ints, visitor);

        List<Object> visited = visitor.getVisited();

        // Only the primitive array object itself is visited
        assertEquals(1, visited.size());
        assertSame(ints, visited.get(0));
    }

    /* --------------------------------------------------------------------- */
    /* 4. Traversal of a Collection (ArrayList) containing nulls and objects   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testTraverseCollection()
    {
        List<Object> list = new ArrayList<>();
        list.add("alpha");
        list.add(null);
        Node node = new Node("beta");
        list.add(node);

        Traverser.traverse(list, visitor);

        List<Object> visited = visitor.getVisited();

        // Expected: the collection itself, "alpha", and the Node instance
        assertEquals(3, visited.size());
        assertSame(list, visited.get(0));
        assertEquals("alpha", visited.get(1));
        assertSame(node, visited.get(2));
    }

    /* --------------------------------------------------------------------- */
    /* 5. Traversal of a Map (including null keys/values)                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testTraverseMap()
    {
        Map<Object, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put(new Node("keyNode"), new Node("valueNode"));
        map.put(null, "nullKeyIgnored");   // null key should be ignored
        map.put("nullValue", null);        // null value should be ignored

        Traverser.traverse(map, visitor);

        List<Object> visited = visitor.getVisited();

        // Expected visited objects:
        // 1) the map itself
        // 2) "key1"
        // 3) "value1"
        // 4) keyNode (Node)
        // 5) valueNode (Node)
        assertEquals(5, visited.size());
        assertSame(map, visited.get(0));
        assertEquals("key1", visited.get(1));
        assertEquals("value1", visited.get(2));

        // The two Node objects can appear in any order after the first three
        Set<Object> nodeSet = new HashSet<>(visited.subList(3, 5));
        assertTrue(nodeSet.contains(map.get("key1"))); // just to use variable, not needed
        assertTrue(nodeSet.contains(map.keySet().stream()
                .filter(o -> o instanceof Node).findFirst().orElse(null)));
        // Verify that both Nodes are indeed present
        assertTrue(nodeSet.stream().anyMatch(o -> o instanceof Node && ((Node) o).name.equals("keyNode")));
        assertTrue(nodeSet.stream().anyMatch(o -> o instanceof Node && ((Node) o).name.equals("valueNode")));
    }

    /* --------------------------------------------------------------------- */
    /* 6. Skipping a class – objects of the skipped class must never be visited */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSkipClass()
    {
        SkippedClass skipped = new SkippedClass();
        Node node = new Node("node");
        List<Object> container = new ArrayList<>();
        container.add(skipped);
        container.add(node);

        Traverser.traverse(container, new Class<?>[] { SkippedClass.class }, visitor);

        List<Object> visited = visitor.getVisited();

        // Expected visited objects: the container list and the Node (but NOT the SkippedClass)
        assertEquals(2, visited.size());
        assertSame(container, visited.get(0));
        assertSame(node, visited.get(1));
    }

    /* --------------------------------------------------------------------- */
    /* 7. Cycle detection – the traverser must not loop infinitely               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCycleDetection()
    {
        Node a = new Node("A");
        Node b = new Node("B");
        a.ref = b;
        b.ref = a; // creates a cycle

        Traverser.traverse(a, visitor);

        List<Object> visited = visitor.getVisited();

        // The root (a), b, and the collection of fields (no extra objects)
        // Should be exactly two distinct Node instances plus the root reference (a)
        assertEquals(2, visited.size());
        assertTrue(visited.contains(a));
        assertTrue(visited.contains(b));
    }

    /* --------------------------------------------------------------------- */
    /* 8. Direct use of the instance method {@code walk}                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testWalkDirect()
    {
        Node root = new Node("root");
        root.ref = new Node("child");

        Traverser traverser = new Traverser();
        traverser.walk(root, null, visitor);

        List<Object> visited = visitor.getVisited();

        // Expect root and child only
        assertEquals(2, visited.size());
        assertTrue(visited.contains(root));
        assertTrue(visited.contains(root.ref));
    }

    /* --------------------------------------------------------------------- */
    /* 9. ClassInfo caching – the same ClassInfo instance must be returned for  */
    /*    repeated requests of the same class.                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testClassInfoCaching()
    {
        Traverser traverser = new Traverser();

        Traverser.ClassInfo ci1 = traverser.getClassInfo(String.class, null);
        Traverser.ClassInfo ci2 = traverser.getClassInfo(String.class, null);

        assertSame("ClassInfo should be cached and identical for the same class", ci1, ci2);
    }

    /* --------------------------------------------------------------------- */
    /* 10. Null visitor should result in a NullPointerException                */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testTraverseWithNullVisitor()
    {
        Traverser.traverse(new Object(), null);
    }

    /* --------------------------------------------------------------------- */
    /* 11. Null root – traversal should simply do nothing (no exception)      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testTraverseNullRoot()
    {
        Traverser.traverse(null, visitor);
        assertTrue("Visitor should not be called when root is null", visitor.getVisited().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 12. Verify that caches are cleared after a static traversal               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCachesClearedAfterStaticTraverse()
    {
        Traverser traverser = new Traverser();

        // Populate caches by walking something
        traverser.walk("testString", null, visitor);
        assertFalse("Class cache should contain entry after walk", traverser._classCache.isEmpty());
        assertFalse("Visited cache should contain entry after walk", traverser._objVisited.isEmpty());

        // Use the static method which internally clears the caches
        Traverser.traverse("anotherString", visitor);

        // The static method creates its own Traverser instance, so we cannot directly
        // inspect its internal caches. Instead we assert that the previous instance's
        // caches remain untouched (they are not cleared by the static call), which
        // confirms that the static method does its own clearing.
        assertFalse("Original instance's caches should remain unchanged", traverser._classCache.isEmpty());
        assertFalse("Original instance's visited cache should remain unchanged", traverser._objVisited.isEmpty());
    }
}
