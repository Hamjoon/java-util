package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

/**
 * Comprehensive JUnit‑4 test suite for {@link Traverser}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API signatures</li>
 *   <li>Normal traversal of objects, arrays, collections and maps</li>
 *   <li>Skipping of classes (both Set‑based and array‑based overloads)</li>
 *   <li>Cycle detection</li>
 *   <li>Null handling and IllegalArgumentException throwing</li>
 *   <li>Behaviour of the {@link Traverser.NodeVisit} wrapper</li>
 * </ul>
 */
public class TraverserTest {

    /** Helper visitor that records every {@link Traverser.NodeVisit} it receives. */
    private static class RecordingVisitor implements Consumer<Traverser.NodeVisit> {
        final List<Traverser.NodeVisit> visits = new ArrayList<>();

        @Override
        public void accept(Traverser.NodeVisit visit) {
            visits.add(visit);
        }

        /** Returns the visited root objects in order of visitation. */
        List<Object> visitedObjects() {
            List<Object> list = new ArrayList<>();
            for (Traverser.NodeVisit v : visits) {
                list.add(v.getNode());
            }
            return list;
        }
    }

    /** Simple POJO used for basic traversal tests. */
    private static class SimpleNode {
        int id;
        SimpleNode child;

        SimpleNode(int id) {
            this.id = id;
        }
    }

    /** POJO containing an Object array (non‑primitive). */
    private static class WithObjectArray {
        String[] strings;

        WithObjectArray(String... strings) {
            this.strings = strings;
        }
    }

    /** POJO containing a Collection field. */
    private static class WithCollection {
        List<Object> list = new ArrayList<>();

        WithCollection(Object... elements) {
            Collections.addAll(list, elements);
        }
    }

    /** POJO containing a Map field. */
    private static class WithMap {
        Map<Object, Object> map = new HashMap<>();

        WithMap(Object key, Object value) {
            map.put(key, value);
        }
    }

    /** POJO with a private field that is not accessible without reflection tricks. */
    private static class WithInaccessibleField {
        private final String secret = "top‑secret";
    }

    private RecordingVisitor recorder;

    @Before
    public void setUp() {
        recorder = new RecordingVisitor();
    }

    /* -------------------------------------------------
     *  1.  Public method signatures (extracted for reference)
     * ------------------------------------------------- */
    // Traverser.NodeVisit(Object, Map<Field,Object>)
    // Traverser.NodeVisit.getNode()
    // Traverser.NodeVisit.getFields()
    // Traverser.NodeVisit.getNodeClass()
    // Traverser.traverse(Object, Consumer<NodeVisit>, Set<Class<?>>)
    // Traverser.traverse(Object, Visitor)
    // Traverser.traverse(Object, Class<?>[], Visitor)

    /* -------------------------------------------------
     *  2.  Basic functionality tests
     * ------------------------------------------------- */

    @Test
    public void testTraverseRootOnly() {
        SimpleNode root = new SimpleNode(1);
        Traverser.traverse(root, recorder, null);

        // Only the root should be visited.
        assertEquals(1, recorder.visits.size());
        Traverser.NodeVisit visit = recorder.visits.get(0);
        assertSame(root, visit.getNode());
        assertEquals(SimpleNode.class, visit.getNodeClass());

        // Verify that the field map contains the "id" field with value 1 and "child" field with null.
        Map<Field, Object> fields = visit.getFields();
        assertEquals(2, fields.size());
        boolean idFound = false, childFound = false;
        for (Map.Entry<Field, Object> e : fields.entrySet()) {
            if (e.getKey().getName().equals("id")) {
                assertEquals(1, e.getValue());
                idFound = true;
            } else if (e.getKey().getName().equals("child")) {
                assertNull(e.getValue());
                childFound = true;
            }
        }
        assertTrue(idFound && childFound);
    }

    @Test
    public void testTraverseWithChildObject() {
        SimpleNode parent = new SimpleNode(10);
        SimpleNode child = new SimpleNode(20);
        parent.child = child;

        Traverser.traverse(parent, recorder, null);

        // Expect two visits: parent then child (order depends on stack LIFO – child first because added to front)
        List<Object> visited = recorder.visitedObjects();
        assertEquals(2, visited.size());
        assertTrue(visited.contains(parent));
        assertTrue(visited.contains(child));
    }

    @Test
    public void testTraverseArrayOfObjects() {
        WithObjectArray root = new WithObjectArray("a", "b", null, "c");
        Traverser.traverse(root, recorder, null);

        // Visits: root object + three non‑null strings
        List<Object> visited = recorder.visitedObjects();
        assertEquals(4, visited.size());
        assertTrue(visited.contains(root));
        assertTrue(visited.contains("a"));
        assertTrue(visited.contains("b"));
        assertTrue(visited.contains("c"));
        assertFalse(visited.contains(null));
    }

    @Test
    public void testTraversePrimitiveArrayIsIgnored() {
        int[] primitives = {1, 2, 3};
        Traverser.traverse(primitives, recorder, null);

        // Primitive array is visited as a single node; its elements are not traversed.
        assertEquals(1, recorder.visits.size());
        assertSame(primitives, recorder.visits.get(0).getNode());
    }

    @Test
    public void testTraverseCollection() {
        WithCollection root = new WithCollection("x", 42, null);
        Traverser.traverse(root, recorder, null);

        // Visits: root + "x" + Integer 42 (null ignored)
        List<Object> visited = recorder.visitedObjects();
        assertEquals(3, visited.size());
        assertTrue(visited.contains(root));
        assertTrue(visited.contains("x"));
        assertTrue(visited.contains(42));
    }

    @Test
    public void testTraverseMap() {
        WithMap root = new WithMap("key1", "value1");
        Traverser.traverse(root, recorder, null);

        // Visits: root, key, value
        List<Object> visited = recorder.visitedObjects();
        assertEquals(3, visited.size());
        assertTrue(visited.contains(root));
        assertTrue(visited.contains("key1"));
        assertTrue(visited.contains("value1"));
    }

    /* -------------------------------------------------
     *  3.  Edge‑case & exception handling tests
     * ------------------------------------------------- */

    @Test
    public void testNullRootDoesNotInvokeVisitor() {
        Traverser.traverse(null, recorder, null);
        assertTrue(recorder.visits.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullVisitorThrowsException() {
        Traverser.traverse(new Object(), null, null);
    }

    @Test
    public void testSkipClassViaSet() {
        // Skip all String instances.
        Set<Class<?>> skip = new HashSet<>();
        skip.add(String.class);

        WithObjectArray root = new WithObjectArray("keep", "skip", null);
        Traverser.traverse(root, recorder, skip);

        // "skip" should not be visited.
        List<Object> visited = recorder.visitedObjects();
        assertEquals(2, visited.size()); // root + "keep"
        assertTrue(visited.contains(root));
        assertTrue(visited.contains("keep"));
        assertFalse(visited.contains("skip"));
    }

    @Test
    public void testSkipClassViaArrayParameter() {
        // Use the deprecated overload that accepts a Class[].
        Traverser.Visitor legacyVisitor = o -> recorder.accept(new Traverser.NodeVisit(o, Collections.emptyMap()));
        Traverser.traverse(new WithCollection("a", "b"), new Class<?>[]{String.class}, legacyVisitor);

        // Both strings should be skipped.
        List<Object> visited = recorder.visitedObjects();
        assertEquals(1, visited.size()); // only the root WithCollection instance
        assertTrue(visited.get(0) instanceof WithCollection);
    }

    @Test
    public void testShouldSkipSuperclass() {
        // Skip Number.class – this should also skip Integer instances.
        Set<Class<?>> skip = new HashSet<>();
        skip.add(Number.class);

        WithCollection root = new WithCollection(1, 2L, "s");
        Traverser.traverse(root, recorder, skip);

        List<Object> visited = recorder.visitedObjects();
        // Integer and Long should be skipped; only root and String remain.
        assertEquals(2, visited.size());
        assertTrue(visited.contains(root));
        assertTrue(visited.contains("s"));
        assertFalse(visited.contains(1));
        assertFalse(visited.contains(2L));
    }

    @Test
    public void testCycleDetection() {
        SimpleNode a = new SimpleNode(1);
        SimpleNode b = new SimpleNode(2);
        a.child = b;
        b.child = a; // creates a cycle

        Traverser.traverse(a, recorder, null);

        // Each node should be visited exactly once despite the cycle.
        List<Object> visited = recorder.visitedObjects();
        assertEquals(2, visited.size());
        assertTrue(visited.contains(a));
        assertTrue(visited.contains(b));
    }

    @Test
    public void testLegacyVisitorMethod() {
        Traverser.Visitor legacy = o -> recorder.accept(new Traverser.NodeVisit(o, Collections.emptyMap()));
        SimpleNode root = new SimpleNode(99);
        Traverser.traverse(root, legacy);

        assertEquals(1, recorder.visits.size());
        assertSame(root, recorder.visits.get(0).getNode());
    }

    @Test
    public void testLegacyVisitorWithSkipArray() {
        Traverser.Visitor legacy = o -> recorder.accept(new Traverser.NodeVisit(o, Collections.emptyMap()));
        WithCollection root = new WithCollection("skipMe", "keepMe");
        Traverser.traverse(root, new Class<?>[]{String.class}, legacy);

        // Both strings are skipped, only root visited.
        assertEquals(1, recorder.visits.size());
        assertSame(root, recorder.visits.get(0).getNode());
    }

    @Test
    public void testNodeVisitUnmodifiableFieldsMap() {
        SimpleNode root = new SimpleNode(5);
        Traverser.traverse(root, recorder, null);
        Traverser.NodeVisit visit = recorder.visits.get(0);
        Map<Field, Object> fields = visit.getFields();

        try {
            fields.put(null, null);
            fail("Fields map should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testInaccessibleFieldHandledGracefully() {
        // The field 'secret' is private; ReflectionUtils.getAllDeclaredFields will return it.
        // If Traverser cannot access it, it should store "<inaccessible>".
        WithInaccessibleField root = new WithInaccessibleField();
        Traverser.traverse(root, recorder, null);
        Traverser.NodeVisit visit = recorder.visits.get(0);
        boolean found = false;
        for (Map.Entry<Field, Object> e : visit.getFields().entrySet()) {
            if (e.getKey().getName().equals("secret")) {
                found = true;
                // The actual value may be the real secret or the placeholder.
                // Both are acceptable; we just ensure the map entry exists.
                assertNotNull(e.getValue());
            }
        }
        assertTrue("secret field should be present in the fields map", found);
    }

    /* -------------------------------------------------
     *  4.  Additional sanity checks
     * ------------------------------------------------- */

    @Test
    public void testTraverseEmptyCollection() {
        WithCollection root = new WithCollection(); // empty list
        Traverser.traverse(root, recorder, null);
        assertEquals(1, recorder.visits.size()); // only the root
    }

    @Test
    public void testTraverseEmptyMap() {
        WithMap root = new WithMap("k", "v");
        root.map.clear(); // make it empty
        Traverser.traverse(root, recorder, null);
        assertEquals(1, recorder.visits.size()); // only the root
    }

    @Test
    public void testTraverseNullElementsInCollectionAndMap() {
        WithCollection root = new WithCollection(null, "x");
        Traverser.traverse(root, recorder, null);
        List<Object> visited = recorder.visitedObjects();
        // Null element ignored
        assertEquals(2, visited.size());
        assertTrue(visited.contains(root));
        assertTrue(visited.contains("x"));
    }
}
