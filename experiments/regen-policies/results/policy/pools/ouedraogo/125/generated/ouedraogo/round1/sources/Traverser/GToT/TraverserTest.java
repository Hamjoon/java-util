package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

/**
 * Comprehensive JUnit‑4 test suite for {@link Traverser}.
 *
 * <p>This suite was created by a simulated collaboration of three experts.
 * It covers:
 * <ul>
 *   <li>Extraction of all public methods and their signatures.</li>
 *   <li>Basic functional tests for each public method.</li>
 *   <li>Edge‑case and exception handling tests (null arguments, empty collections,
 *       primitive arrays, cycles, class‑skip logic, etc.).</li>
 *   <li>Verification that the {@link Traverser.NodeVisit} object contains the
 *       expected node and field map.</li>
 * </ul>
 * </p>
 */
public class TraverserTest {

    /** Helper class used in the object graph. */
    public static class SimpleNode {
        public String name;
        public SimpleNode child;
        public List<SimpleNode> siblings = new ArrayList<>();

        public SimpleNode(String name) {
            this.name = name;
        }
    }

    /** Helper class with primitive array fields – used to test array handling. */
    public static class PrimitiveArrayHolder {
        public int[] ints = {1, 2, 3};
        public double[] doubles = {0.1, 0.2};
    }

    /** Helper class with various collection types – used to test collection handling. */
    public static class CollectionHolder {
        public List<String> list = Arrays.asList("a", "b");
        public Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        public Map<String, SimpleNode> map = new HashMap<>();
    }

    /** Holds visited nodes for verification in tests. */
    private List<Traverser.NodeVisit> visits;

    @Before
    public void setUp() {
        visits = new ArrayList<>();
    }

    @After
    public void tearDown() {
        visits = null;
    }

    /**
     * Utility consumer that records every {@link Traverser.NodeVisit}.
     */
    private Consumer<Traverser.NodeVisit> recorder() {
        return visit -> visits.add(visit);
    }

    /* -----------------------------------------------------------
     *  Public API extraction (for documentation / sanity)
     * ----------------------------------------------------------- */
    // public static void traverse(Object root, Consumer<NodeVisit> visitor, Set<Class<?>> classesToSkip)
    // public static void traverse(Object root, Visitor visitor)
    // public static void traverse(Object root, Class<?>[] skip, Visitor visitor)
    // public static class NodeVisit { ... }
    // public interface Visitor { void process(Object o); }

    /* -----------------------------------------------------------
     *  Basic functional tests
     * ----------------------------------------------------------- */

    @Test
    public void testTraverseWithConsumerSimpleGraph() {
        SimpleNode root = new SimpleNode("root");
        SimpleNode child = new SimpleNode("child");
        root.child = child;
        root.siblings.add(new SimpleNode("sib1"));
        root.siblings.add(new SimpleNode("sib2"));

        Traverser.traverse(root, recorder(), null);

        // Expect root, child, sib1, sib2 visited (order may vary because stack is LIFO)
        Set<String> expectedNames = new HashSet<>(Arrays.asList("root", "child", "sib1", "sib2"));
        Set<String> actualNames = new HashSet<>();
        for (Traverser.NodeVisit v : visits) {
            SimpleNode node = (SimpleNode) v.getNode();
            actualNames.add(node.name);
        }
        assertEquals(expectedNames, actualNames);
    }

    @Test
    public void testTraverseWithVisitorLegacyApi() {
        SimpleNode root = new SimpleNode("legacyRoot");
        List<Object> visited = new ArrayList<>();

        Traverser.Visitor legacyVisitor = visited::add;
        Traverser.traverse(root, legacyVisitor);

        // legacyVisitor receives only the node objects (no NodeVisit wrapper)
        assertTrue(visited.contains(root));
        // No other objects in this minimal graph
        assertEquals(1, visited.size());
    }

    @Test
    public void testTraverseWithVisitorAndSkipArray() {
        SimpleNode root = new SimpleNode("root");
        SimpleNode child = new SimpleNode("child");
        root.child = child;

        // Skip SimpleNode class entirely – the child should never be visited
        Set<Class<?>> skip = Collections.singleton(SimpleNode.class);
        Traverser.traverse(root, recorder(), skip);

        assertEquals(1, visits.size());
        assertSame(root, visits.get(0).getNode());
    }

    @Test
    public void testTraverseWithVisitorAndSkipUsingLegacyArray() {
        SimpleNode root = new SimpleNode("root");
        SimpleNode child = new SimpleNode("child");
        root.child = child;

        // Use deprecated overload with Class<?>[] skip
        Traverser.Visitor legacyVisitor = o -> visits.add(new Traverser.NodeVisit(o, Collections.emptyMap()));
        Traverser.traverse(root, new Class<?>[]{SimpleNode.class}, legacyVisitor);

        // Both root and child are SimpleNode, so only root (first encounter) is visited
        assertEquals(1, visits.size());
        assertSame(root, visits.get(0).getNode());
    }

    @Test
    public void testTraverseArrayOfObjects() {
        SimpleNode[] array = new SimpleNode[] {
                new SimpleNode("a"),
                new SimpleNode("b")
        };
        Traverser.traverse(array, recorder(), null);

        Set<String> names = new HashSet<>();
        for (Traverser.NodeVisit v : visits) {
            SimpleNode node = (SimpleNode) v.getNode();
            names.add(node.name);
        }
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), names);
    }

    @Test
    public void testTraversePrimitiveArrayDoesNotRecurse() {
        int[] primitives = {5, 6, 7};
        Traverser.traverse(primitives, recorder(), null);

        // Primitive array itself is visited once; its primitive elements are not objects.
        assertEquals(1, visits.size());
        assertTrue(visits.get(0).getNode() instanceof int[]);
    }

    @Test
    public void testTraverseCollectionAndMap() {
        CollectionHolder holder = new CollectionHolder();
        holder.map.put("nodeKey", new SimpleNode("mapNode"));

        Traverser.traverse(holder, recorder(), null);

        // Expect holder, list elements, set elements, map key, map value
        Set<Object> expected = new HashSet<>();
        expected.add(holder);
        expected.addAll(holder.list);
        expected.addAll(holder.set);
        expected.add("nodeKey");
        expected.add(holder.map.get("nodeKey"));

        Set<Object> actual = new HashSet<>();
        for (Traverser.NodeVisit v : visits) {
            actual.add(v.getNode());
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testNodeVisitProvidesCorrectMetadata() throws NoSuchFieldException {
        SimpleNode node = new SimpleNode("meta");
        Traverser.traverse(node, recorder(), null);
        Traverser.NodeVisit visit = visits.get(0);

        assertSame(node, visit.getNode());
        assertEquals(node.getClass(), visit.getNodeClass());

        // Verify that the field map contains the declared fields
        Field nameField = SimpleNode.class.getField("name");
        Field childField = SimpleNode.class.getField("child");
        Field siblingsField = SimpleNode.class.getField("siblings");

        Map<Field, Object> fields = visit.getFields();
        assertTrue(fields.containsKey(nameField));
        assertTrue(fields.containsKey(childField));
        assertTrue(fields.containsKey(siblingsField));

        assertEquals("meta", fields.get(nameField));
        assertNull(fields.get(childField));
        assertEquals(node.siblings, fields.get(siblingsField));
    }

    /* -----------------------------------------------------------
     *  Edge‑case & exception handling tests
     * ----------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testTraverseNullConsumerThrows() {
        Traverser.traverse(new Object(), null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLegacyTraverseNullVisitorThrows() {
        Traverser.traverse(new Object(), (Traverser.Visitor) null);
    }

    @Test
    public void testTraverseNullRootDoesNothing() {
        Traverser.traverse(null, recorder(), null);
        assertTrue(visits.isEmpty());
    }

    @Test
    public void testTraverseRootAlreadyVisitedCycle() {
        SimpleNode a = new SimpleNode("a");
        SimpleNode b = new SimpleNode("b");
        a.child = b;
        b.child = a; // cycle

        Traverser.traverse(a, recorder(), null);

        // Both nodes should be visited exactly once, despite the cycle
        Set<String> names = new HashSet<>();
        for (Traverser.NodeVisit v : visits) {
            SimpleNode n = (SimpleNode) v.getNode();
            names.add(n.name);
        }
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), names);
    }

    @Test
    public void testShouldSkipClassWithSuperclassMatch() {
        // Skip CharSequence, which is a super‑interface of String
        Set<Class<?>> skip = Collections.singleton(CharSequence.class);
        Traverser.traverse("stringRoot", recorder(), skip);

        // The string itself should be skipped, so no visits at all
        assertTrue(visits.isEmpty());
    }

    @Test
    public void testProcessArrayWithNullElementsAndSkip() {
        Object[] mixed = new Object[] {
                new SimpleNode("keep"),
                null,
                new SimpleNode("skip")
        };
        Set<Class<?>> skip = Collections.singleton(SimpleNode.class);
        Traverser.traverse(mixed, recorder(), skip);

        // Only the first element (non‑skipped) should be visited
        assertEquals(1, visits.size());
        assertEquals("keep", ((SimpleNode) visits.get(0).getNode()).name);
    }

    @Test
    public void testProcessCollectionWithNullElements() {
        List<Object> list = Arrays.asList(
                new SimpleNode("valid"),
                null,
                "string"
        );
        Traverser.traverse(list, recorder(), null);

        Set<Object> expected = new HashSet<>(Arrays.asList(list, "string", list.get(0)));
        Set<Object> actual = new HashSet<>();
        for (Traverser.NodeVisit v : visits) {
            actual.add(v.getNode());
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testProcessMapWithNullKeyAndValue() {
        Map<Object, Object> map = new HashMap<>();
        map.put(null, "valueOnly");
        map.put("keyOnly", null);
        map.put("both", "bothVal");

        Traverser.traverse(map, recorder(), null);

        Set<Object> expected = new HashSet<>(Arrays.asList(map, "valueOnly", "keyOnly", "both", "bothVal"));
        Set<Object> actual = new HashSet<>();
        for (Traverser.NodeVisit v : visits) {
            actual.add(v.getNode());
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testProcessFieldsInaccessibleDueToIllegalAccess() throws Exception {
        // Create a class with a private field that will throw IllegalAccessException when accessed.
        class InaccessibleFieldHolder {
            private final Object secret = new Object();

            // Force inaccessible access by making the field non‑accessible via reflection.
            public InaccessibleFieldHolder() throws Exception {
                Field f = InaccessibleFieldHolder.class.getDeclaredField("secret");
                f.setAccessible(false); // ensure it stays inaccessible
            }
        }

        InaccessibleFieldHolder holder = new InaccessibleFieldHolder();
        Traverser.traverse(holder, recorder(), null);

        // The holder itself must be visited; the private field will be recorded as "<inaccessible>"
        Traverser.NodeVisit visit = visits.get(0);
        Field secretField = InaccessibleFieldHolder.class.getDeclaredField("secret");
        assertEquals("<inaccessible>", visit.getFields().get(secretField));
    }

    /* -----------------------------------------------------------
     *  Helper assertions (none needed beyond JUnit asserts)
     * ----------------------------------------------------------- */
}
