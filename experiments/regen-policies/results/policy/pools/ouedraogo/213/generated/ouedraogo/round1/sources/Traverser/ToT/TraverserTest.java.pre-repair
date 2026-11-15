package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

/**
 * Comprehensive JUnit‑4 test suite for {@link Traverser}.
 *
 * The tests are organised as if three independent experts contributed one test
 * each, then merged their ideas into a single file.  The suite covers:
 *
 * <ul>
 *   <li>Basic traversal (root node, field collection)</li>
 *   <li>Null‑argument validation for the public API</li>
 *   <li>Skipping of classes supplied by the caller</li>
 *   <li>Array, {@link Collection} and {@link Map} handling</li>
 *   <li>Cycle detection (object graph containing loops)</li>
 *   <li>Correctness of {@link Traverser.NodeVisit} accessors</li>
 * </ul>
 */
public class TraverserTest {

    /* --------------------------------------------------------------------- */
    /* Helper POJOs used by the tests                                           */
    /* --------------------------------------------------------------------- */

    /** Simple bean with a reference to another bean – used to create cycles. */
    public static class BeanA {
        public String name;
        public BeanA next;               // may refer back to itself or another BeanA

        public BeanA(String name) {
            this.name = name;
        }
    }

    /** Bean that contains every kind of container the Traverser knows about. */
    public static class BeanB {
        public int primitive = 42;
        public String text = "hello";

        public BeanA[] array;
        public List<BeanA> list = new ArrayList<>();
        public Map<String, BeanA> map = new HashMap<>();

        public BeanB() { }
    }

    /* --------------------------------------------------------------------- */
    /* 1. Validation of null arguments (expert 1)                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTraverseNullVisitorThrows() {
        try {
            Traverser.traverse(new Object(), (Consumer<Traverser.NodeVisit>) null, null);
            Assert.fail("Expected IllegalArgumentException when visitor is null");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("visitor cannot be null", e.getMessage());
        }
    }

    @Test
    public void testLegacyTraverseNullVisitorThrows() {
        try {
            Traverser.traverse(new Object(), (Traverser.Visitor) null);
            Assert.fail("Expected IllegalArgumentException when legacy visitor is null");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("visitor cannot be null", e.getMessage());
        }
    }

    @Test
    public void testLegacyTraverseWithSkipArrayNullVisitorThrows() {
        try {
            Traverser.traverse(new Object(), null, (Traverser.Visitor) null);
            Assert.fail("Expected IllegalArgumentException when legacy visitor is null (skip array overload)");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("visitor cannot be null", e.getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    /* 2. Simple object graph traversal (expert 2)                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSimpleTraversalVisitsRootAndFields() {
        BeanA root = new BeanA("root");
        root.next = new BeanA("child");

        List<Traverser.NodeVisit> visits = new ArrayList<>();
        Consumer<Traverser.NodeVisit> collector = visits::add;

        Traverser.traverse(root, collector, null);

        // Expect exactly two nodes: root and child
        Assert.assertEquals(2, visits.size());

        // Helper to find a visit by node identity
        Traverser.NodeVisit rootVisit = visits.stream()
                .filter(v -> v.getNode() == root)
                .findFirst()
                .orElse(null);
        Traverser.NodeVisit childVisit = visits.stream()
                .filter(v -> v.getNode() == root.next)
                .findFirst()
                .orElse(null);

        Assert.assertNotNull("Root node should be visited", rootVisit);
        Assert.assertNotNull("Child node should be visited", childVisit);

        // Verify NodeVisit API
        Assert.assertEquals(root, rootVisit.getNode());
        Assert.assertEquals(BeanA.class, rootVisit.getNodeClass());

        // Verify that fields map contains both declared fields
        Map<Field, Object> rootFields = rootVisit.getFields();
        Assert.assertEquals(2, rootFields.size()); // name + next
        // Use reflection to locate the specific fields
        Field nameField = getDeclaredField(BeanA.class, "name");
        Field nextField = getDeclaredField(BeanA.class, "next");
        Assert.assertTrue(rootFields.containsKey(nameField));
        Assert.assertTrue(rootFields.containsKey(nextField));
        Assert.assertEquals("root", rootFields.get(nameField));
        Assert.assertEquals(root.next, rootFields.get(nextField));
    }

    /* --------------------------------------------------------------------- */
    /* 3. Advanced scenarios – containers, skipping, cycles (expert 3)       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTraversalSkipsSpecifiedClasses() {
        BeanB root = new BeanB();
        root.array = new BeanA[] { new BeanA("a1"), new BeanA("a2") };
        root.list.add(new BeanA("l1"));
        root.map.put("k1", new BeanA("m1"));

        // Skip all String instances – they should never appear as visited nodes
        Set<Class<?>> skip = Collections.singleton(String.class);
        List<Object> visitedNodes = new ArrayList<>();

        Traverser.traverse(root, visit -> visitedNodes.add(visit.getNode()), skip);

        // All BeanA instances must be visited, but no String instance
        for (Object node : visitedNodes) {
            Assert.assertFalse("String instances must be skipped", node instanceof String);
        }

        // Verify that we have visited exactly: root (BeanB) + 4 BeanA objects
        long beanACount = visitedNodes.stream().filter(o -> o instanceof BeanA).count();
        Assert.assertEquals(4, beanACount);
        Assert.assertTrue(visitedNodes.contains(root));
    }

    @Test
    public void testArrayAndCollectionProcessing() {
        BeanB root = new BeanB();
        BeanA a1 = new BeanA("a1");
        BeanA a2 = new BeanA("a2");
        BeanA a3 = new BeanA("a3");

        root.array = new BeanA[] { a1, a2 };
        root.list.add(a3);
        root.map.put("key", a1); // duplicate reference to a1 should be visited only once

        Set<Object> visited = new HashSet<>();
        Traverser.traverse(root, visit -> visited.add(visit.getNode()), null);

        // Expected nodes: root, a1, a2, a3
        Assert.assertTrue(visited.contains(root));
        Assert.assertTrue(visited.contains(a1));
        Assert.assertTrue(visited.contains(a2));
        Assert.assertTrue(visited.contains(a3));
        Assert.assertEquals(4, visited.size()); // duplicate a1 must not create a second entry
    }

    @Test
    public void testMapProcessing() {
        BeanB root = new BeanB();
        BeanA keyBean = new BeanA("keyBean");
        BeanA valueBean = new BeanA("valueBean");

        root.map.put("stringKey", valueBean);
        root.map.put(keyBean, valueBean); // key is a BeanA, value is same BeanA as above

        Set<Object> visited = new HashSet<>();
        Traverser.traverse(root, visit -> visited.add(visit.getNode()), null);

        // Expected nodes: root, keyBean, valueBean, "stringKey"
        Assert.assertTrue(visited.contains(root));
        Assert.assertTrue(visited.contains(keyBean));
        Assert.assertTrue(visited.contains(valueBean));
        Assert.assertTrue(visited.contains("stringKey"));
        Assert.assertEquals(4, visited.size());
    }

    @Test
    public void testCycleDetection() {
        BeanA a = new BeanA("a");
        BeanA b = new BeanA("b");
        a.next = b;
        b.next = a; // creates a cycle a -> b -> a

        List<Traverser.NodeVisit> visits = new ArrayList<>();
        Traverser.traverse(a, visits::add, null);

        // Both nodes must be visited exactly once, no infinite loop
        Assert.assertEquals(2, visits.size());

        Set<Object> visitedObjects = new HashSet<>();
        for (Traverser.NodeVisit v : visits) {
            visitedObjects.add(v.getNode());
        }
        Assert.assertTrue(visitedObjects.contains(a));
        Assert.assertTrue(visitedObjects.contains(b));
    }

    @Test
    public void testPrimitiveArrayIsIgnored() {
        // An int[] should not cause any additional node visits (primitive array)
        int[] primitiveArray = new int[] {1, 2, 3};

        List<Object> visited = new ArrayList<>();
        Traverser.traverse(primitiveArray, visit -> visited.add(visit.getNode()), null);

        // Only the root array itself should be visited
        Assert.assertEquals(1, visited.size());
        Assert.assertSame(primitiveArray, visited.get(0));
    }

    @Test
    public void testNodeVisitUnmodifiableFieldsMap() {
        BeanA bean = new BeanA("test");
        List<Traverser.NodeVisit> visits = new ArrayList<>();
        Traverser.traverse(bean, visits::add, null);
        Assert.assertEquals(1, visits.size());

        Traverser.NodeVisit nodeVisit = visits.get(0);
        Map<Field, Object> fields = nodeVisit.getFields();

        try {
            fields.put(null, null);
            Assert.fail("Fields map should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /* Utility method for accessing private fields in assertions               */
    /* --------------------------------------------------------------------- */

    private static Field getDeclaredField(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Field '" + name + "' not found in " + clazz, e);
        }
    }
}
