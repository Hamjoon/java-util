package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Traverser}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic object graph traversal (primitive fields are ignored)</li>
 *   <li>array handling (primitive arrays are skipped, object arrays are traversed)</li>
 *   <li>collection and map traversal</li>
 *   <li>cycle detection (graph with circular references)</li>
 *   <li>skip‑class functionality</li>
 *   <li>null root handling and illegal argument handling</li>
 * </ul>
 *
 * No external mocking frameworks are used – only real objects constructed with the
 * classes defined in this test file.
 */
public class TraverserTest {

    /** Simple visitor that records every object passed to {@code process}. */
    private static class RecordingVisitor implements Traverser.Visitor {
        /** Uses identity semantics – the same object reference must not be recorded twice. */
        private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        @Override
        public void process(Object o) {
            // Guard against accidental nulls – the Traverser never calls process(null)
            assertNotNull("Visitor should never receive null", o);
            visited.add(o);
        }

        /** Returns an immutable snapshot of the visited objects. */
        Set<Object> getVisited() {
            return Collections.unmodifiableSet(visited);
        }
    }

    /** Helper to assert that a collection contains exactly the expected objects (order not important). */
    private static void assertContainsExactly(Set<Object> actual, Object... expected) {
        Set<Object> expectedSet = new HashSet<>(Arrays.asList(expected));
        assertEquals("Visited set does not match expected contents", expectedSet, actual);
    }

    /* --------------------------------------------------------------------- *
     *  Test fixture classes – simple POJOs used to build object graphs.
     * --------------------------------------------------------------------- */

    /** Simple node with a reference to another node (or null). */
    private static class Node {
        String name;
        Node next;               // reference field – should be traversed
        int value;               // primitive – should be ignored

        Node(String name) { this.name = name; }
    }

    /** Container that mixes primitive and reference fields. */
    private static class Container {
        int id;                  // primitive – ignored
        Node node;               // reference – traversed
        String label;            // reference (String) – traversed
        Container(int id, Node node, String label) {
            this.id = id;
            this.node = node;
            this.label = label;
        }
    }

    /** Holds an Object array (including null entries). */
    private static class WithObjectArray {
        Object[] elements;
        WithObjectArray(Object... elements) { this.elements = elements; }
    }

    /** Holds a primitive int array – Traverser must not descend into it. */
    private static class WithIntArray {
        int[] ints;
        WithIntArray(int... ints) { this.ints = ints; }
    }

    /** Holds a Collection of mixed objects. */
    private static class WithCollection {
        Collection<Object> items;
        WithCollection(Collection<Object> items) { this.items = items; }
    }

    /** Holds a Map with mixed key/value types. */
    private static class WithMap {
        Map<Object, Object> map;
        WithMap(Map<Object, Object> map) { this.map = map; }
    }

    /** Class that will be placed in the skip list. */
    private static class Skipped {
        Node inner;
        Skipped(Node inner) { this.inner = inner; }
    }

    /* --------------------------------------------------------------------- *
     *  1.  Simple object graph (no collections, no arrays, no cycles)
     * --------------------------------------------------------------------- */

    @Test
    public void testTraverseSimpleObjectGraph() {
        Node leaf = new Node("leaf");
        Node root = new Node("root");
        root.next = leaf;
        Container container = new Container(42, root, "myLabel");

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(container, visitor);

        // Expected visited objects: container, root node, leaf node, label string.
        // Primitive fields (int, int) are never visited.
        assertContainsExactly(visitor.getVisited(),
                container,
                root,
                leaf,
                "myLabel");
    }

    /* --------------------------------------------------------------------- *
     *  2.  Array handling – object arrays (including nulls) and primitive arrays
     * --------------------------------------------------------------------- */

    @Test
    public void testTraverseObjectArrayAndSkipNulls() {
        Node a = new Node("A");
        Node b = new Node("B");
        WithObjectArray holder = new WithObjectArray(a, null, b);

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(holder, visitor);

        // Expected: holder itself, plus the two Node instances (null ignored)
        assertContainsExactly(visitor.getVisited(),
                holder,
                a,
                b);
    }

    @Test
    public void testTraversePrimitiveArrayIsIgnored() {
        WithIntArray holder = new WithIntArray(1, 2, 3, 4);

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(holder, visitor);

        // Only the holder object should be visited; primitive array elements are ignored.
        assertContainsExactly(visitor.getVisited(),
                holder);
    }

    /* --------------------------------------------------------------------- *
     *  3.  Collection and Map traversal
     * --------------------------------------------------------------------- */

    @Test
    public void testTraverseCollectionAndMap() {
        Node n1 = new Node("n1");
        Node n2 = new Node("n2");
        List<Object> list = new ArrayList<>();
        list.add(n1);
        list.add("stringInList");
        list.add(null);           // null should be ignored

        Map<Object, Object> map = new HashMap<>();
        map.put("key1", n2);
        map.put(n1, "valueForNode"); // both key and value are references
        map.put(null, "nullKey");     // null key ignored
        map.put("nullValue", null);   // null value ignored

        WithCollection colHolder = new WithCollection(list);
        WithMap mapHolder = new WithMap(map);

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(colHolder, visitor);
        Traverser.traverse(mapHolder, visitor); // second traversal uses the same visitor

        // Expected visited objects:
        // - colHolder, list, n1, "stringInList"
        // - mapHolder, map, "key1", n2, n1 (again but deduped), "valueForNode"
        // Note: primitive strings are normal objects and should be visited.
        Set<Object> expected = new HashSet<>(Arrays.asList(
                colHolder, list, n1, "stringInList",
                mapHolder, map, "key1", n2, "valueForNode"
        ));
        assertEquals("Visited set differs from expected", expected, visitor.getVisited());
    }

    /* --------------------------------------------------------------------- *
     *  4.  Cycle detection – ensure traversal terminates and does not duplicate.
     * --------------------------------------------------------------------- */

    @Test
    public void testTraverseCycleDetection() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        b.next = a; // creates a cycle A -> B -> A

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(a, visitor);

        // Both nodes should be visited exactly once, plus the root (which is node a itself)
        assertContainsExactly(visitor.getVisited(),
                a,
                b);
    }

    /* --------------------------------------------------------------------- *
     *  5.  Skip‑class functionality
     * --------------------------------------------------------------------- */

    @Test
    public void testTraverseWithSkipClasses() {
        Node inner = new Node("inner");
        Skipped skippedObj = new Skipped(inner);
        Container container = new Container(1, inner, "label");

        // Skip the Skipped class – its instance (skippedObj) and its fields must not be visited.
        Class<?>[] skip = new Class<?>[]{Skipped.class};

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(skippedObj, skip, visitor);
        Traverser.traverse(container, skip, visitor);

        // Expected visited objects:
        // - From skippedObj: nothing (the Skipped instance itself is skipped)
        // - From container: container, inner node, label string.
        assertContainsExactly(visitor.getVisited(),
                container,
                inner,
                "label");
    }

    /* --------------------------------------------------------------------- *
     *  6.  Null root handling – Traverser should simply do nothing.
     * --------------------------------------------------------------------- */

    @Test
    public void testTraverseNullRootDoesNothing() {
        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(null, visitor);
        assertTrue("Visitor should not be called for null root", visitor.getVisited().isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  7.  Illegal arguments – passing a null Visitor should result in NPE.
     * --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testTraverseNullVisitorThrowsNPE() {
        Container c = new Container(0, null, "test");
        Traverser.traverse(c, null, null); // static method that eventually calls walk -> NPE
    }

    /* --------------------------------------------------------------------- *
     *  8.  Direct ClassInfo skip flag verification (using reflection)
     * --------------------------------------------------------------------- */

    @Test
    public void testClassInfoSkipFlag() throws Exception {
        // ClassInfo for a class that is in the skip list must have its internal _skip flag set.
        Class<?>[] skip = new Class<?>[]{Skipped.class};
        Traverser.ClassInfo info = new Traverser.ClassInfo(Skipped.class, skip);

        // Use reflection to read the private _skip field.
        java.lang.reflect.Field skipField = Traverser.ClassInfo.class.getDeclaredField("_skip");
        skipField.setAccessible(true);
        boolean skipFlag = (Boolean) skipField.get(info);
        assertTrue("ClassInfo should mark class as skipped", skipFlag);
    }

    /* --------------------------------------------------------------------- *
     *  9.  Verify that primitive wrapper objects are treated as normal objects.
     * --------------------------------------------------------------------- */

    @Test
    public void testTraverseWrapperObjects() {
        Integer intObj = Integer.valueOf(7);
        Double dblObj = Double.valueOf(3.14);
        List<Object> list = Arrays.asList(intObj, dblObj);

        RecordingVisitor visitor = new RecordingVisitor();
        Traverser.traverse(list, visitor);

        // Wrapper objects are reference types; they must be visited.
        assertContainsExactly(visitor.getVisited(),
                list,
                intObj,
                dblObj);
    }
}
