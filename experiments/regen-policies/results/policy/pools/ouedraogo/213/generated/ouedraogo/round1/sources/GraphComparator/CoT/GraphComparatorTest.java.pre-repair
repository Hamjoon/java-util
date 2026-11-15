package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Field;
import java.util.*;

public class GraphComparatorTest {

    /* ----------------------------------------------------------------------
     * Helper classes used in the tests
     * ---------------------------------------------------------------------- */

    /** Simple node class with an identifiable id and several mutable fields. */
    public static class Node {
        public Long id;                     // used for ID fetching
        public String name;
        public int[] numbers;
        public List<String> list;
        public Set<String> set;
        public Map<String, Integer> map;

        public Node(Long id) {
            this.id = id;
            this.name = "";
            this.numbers = new int[0];
            this.list = new ArrayList<>();
            this.set = new HashSet<>();
            this.map = new HashMap<>();
        }
    }

    /** Container that holds a collection of Nodes – used for orphan detection. */
    public static class Container {
        public List<Node> children = new ArrayList<>();
    }

    /** ID fetcher that returns the public field “id” of any object that possesses it. */
    private static final GraphComparator.ID ID_FETCHER = new GraphComparator.ID() {
        @Override
        public Object getId(Object objectToId) {
            try {
                Field f = objectToId.getClass().getField("id");
                return f.get(objectToId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    /* ----------------------------------------------------------------------
     * 1.  Public method signatures extracted from GraphComparator
     * ---------------------------------------------------------------------- */
    // List<Delta> compare(Object source, Object target, ID idFetcher)
    // List<DeltaError> applyDelta(Object source, List<Delta> commands, ID idFetcher, DeltaProcessor deltaProcessor, boolean... failFast)
    // DeltaProcessor getJavaDeltaProcessor()
    // Command fromName(String name)   (static in Delta.Command)
    // getters / setters on Delta and DeltaError (already public)

    /* ----------------------------------------------------------------------
     * 2.  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testCompareDetectsPrimitiveChange() {
        Node src = new Node(1L);
        src.name = "Alice";

        Node tgt = new Node(1L);
        tgt.name = "Bob";

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        Assert.assertEquals(1, deltas.size());

        GraphComparator.Delta d = deltas.get(0);
        Assert.assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, d.getCmd());
        Assert.assertEquals("name", d.getFieldName());
        Assert.assertEquals("Alice", d.getSourceValue());
        Assert.assertEquals("Bob", d.getTargetValue());
    }

    @Test
    public void testCompareArrayResizeAndSetElement() {
        Node src = new Node(2L);
        src.numbers = new int[]{1, 2};

        Node tgt = new Node(2L);
        tgt.numbers = new int[]{1, 2, 3};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect one ARRAY_RESIZE and one ARRAY_SET_ELEMENT for the new element
        Assert.assertEquals(2, deltas.size());

        boolean resizeFound = false, setFound = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.ARRAY_RESIZE) {
                resizeFound = true;
                Assert.assertEquals(3, d.getOptionalKey()); // new length
            } else if (d.getCmd() == GraphComparator.Delta.Command.ARRAY_SET_ELEMENT) {
                setFound = true;
                Assert.assertEquals(2, d.getOptionalKey()); // index of new element
                Assert.assertNull(d.getSourceValue());
                Assert.assertEquals(3, d.getTargetValue());
            }
        }
        Assert.assertTrue(resizeFound);
        Assert.assertTrue(setFound);
    }

    @Test
    public void testCompareSetAddRemove() {
        Node src = new Node(3L);
        src.set.add("a");

        Node tgt = new Node(3L);
        tgt.set.add("a");
        tgt.set.add("b");

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        Assert.assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        Assert.assertEquals(GraphComparator.Delta.Command.SET_ADD, d.getCmd());
        Assert.assertEquals("b", d.getTargetValue());
    }

    @Test
    public void testCompareMapPutAndRemove() {
        Node src = new Node(4L);
        src.map.put("k1", 10);
        src.map.put("k2", 20);

        Node tgt = new Node(4L);
        tgt.map.put("k1", 10);          // unchanged
        tgt.map.put("k2", 30);          // changed value
        tgt.map.put("k3", 40);          // new entry

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect one MAP_PUT for k2 (value change) and one MAP_PUT for k3 (new entry)
        // No MAP_REMOVE because all source keys still exist in target
        Assert.assertEquals(2, deltas.size());

        Set<String> putKeys = new HashSet<>();
        for (GraphComparator.Delta d : deltas) {
            Assert.assertEquals(GraphComparator.Delta.Command.MAP_PUT, d.getCmd());
            putKeys.add((String) d.getOptionalKey());
        }
        Assert.assertTrue(putKeys.containsAll(Arrays.asList("k2", "k3")));
    }

    @Test
    public void testCompareListResizeAndSetElement() {
        Node src = new Node(5L);
        src.list.addAll(Arrays.asList("x", "y"));

        Node tgt = new Node(5L);
        tgt.list.addAll(Arrays.asList("x", "z", "w")); // change at index 1, plus extra element

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect one LIST_RESIZE and two LIST_SET_ELEMENT (index 1 change, index 2 new)
        Assert.assertEquals(3, deltas.size());

        boolean resizeSeen = false;
        int setCount = 0;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.LIST_RESIZE) {
                resizeSeen = true;
                Assert.assertEquals(3, d.getOptionalKey());
            } else if (d.getCmd() == GraphComparator.Delta.Command.LIST_SET_ELEMENT) {
                setCount++;
                if ((int) d.getOptionalKey() == 1) {
                    Assert.assertEquals("y", d.getSourceValue());
                    Assert.assertEquals("z", d.getTargetValue());
                } else if ((int) d.getOptionalKey() == 2) {
                    Assert.assertNull(d.getSourceValue());
                    Assert.assertEquals("w", d.getTargetValue());
                }
            }
        }
        Assert.assertTrue(resizeSeen);
        Assert.assertEquals(2, setCount);
    }

    @Test
    public void testCompareDetectsObjectFieldTypeChanged() {
        class Mixed {
            public Object field;
        }

        Mixed src = new Mixed();
        src.field = "string";

        Mixed tgt = new Mixed();
        tgt.field = Integer.valueOf(5);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        Assert.assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        Assert.assertEquals(GraphComparator.Delta.Command.OBJECT_FIELD_TYPE_CHANGED, d.getCmd());
        Assert.assertEquals("field", d.getFieldName());
    }

    @Test
    public void testCompareDetectsOrphanObject() {
        Container src = new Container();
        Node n1 = new Node(10L);
        Node n2 = new Node(20L);
        src.children.addAll(Arrays.asList(n1, n2));

        Container tgt = new Container();
        tgt.children.add(n1); // n2 is missing -> orphan

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // There should be one OBJECT_ORPHAN delta for node with id 20
        boolean foundOrphan = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.OBJECT_ORPHAN) {
                foundOrphan = true;
                Assert.assertEquals(20L, d.getId());
            }
        }
        Assert.assertTrue(foundOrphan);
    }

    @Test
    public void testDeltaEqualsAndHashCode() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "ptr", null, null, null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "ptr", null, null, null);
        GraphComparator.Delta d3 = new GraphComparator.Delta(3L, "field", "different", null, null, null);

        Assert.assertEquals(d1, d2); // same srcPtr
        Assert.assertNotEquals(d1, d3);
        Assert.assertEquals(d1.hashCode(), d2.hashCode());
        Assert.assertNotEquals(d1.hashCode(), d3.hashCode());
    }

    @Test
    public void testDeltaErrorToStringAndGetError() {
        GraphComparator.Delta base = new GraphComparator.Delta(1L, "field", "ptr", null, null, null);
        GraphComparator.DeltaError err = new GraphComparator.DeltaError("boom", base);
        Assert.assertEquals("boom", err.getError());
        Assert.assertTrue(err.toString().contains("boom"));
        Assert.assertTrue(err.toString().contains("Delta"));
    }

    /* ----------------------------------------------------------------------
     * 3.  Edge‑case / exception tests for Command.fromName
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testCommandFromNameWithNull() {
        GraphComparator.Delta.Command.fromName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommandFromNameWithEmptyString() {
        GraphComparator.Delta.Command.fromName("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommandFromNameWithUnknown() {
        GraphComparator.Delta.Command.fromName("nonexistent");
    }

    @Test
    public void testCommandFromNameCaseInsensitivity() {
        GraphComparator.Delta.Command cmd = GraphComparator.Delta.Command.fromName("ArRaY.ResiZe");
        Assert.assertEquals(GraphComparator.Delta.Command.ARRAY_RESIZE, cmd);
    }

    /* ----------------------------------------------------------------------
     * 4.  ApplyDelta integration tests using the built‑in JavaDeltaProcessor
     * ---------------------------------------------------------------------- */

    @Test
    public void testApplyDeltaMutatesSourceToMatchTarget() {
        // Build source object graph
        Node src = new Node(100L);
        src.name = "old";
        src.numbers = new int[]{1, 2};
        src.list.addAll(Arrays.asList("a", "b"));
        src.set.add("x");
        src.map.put("k", 1);

        // Build target object graph (desired state)
        Node tgt = new Node(100L);
        tgt.name = "new";
        tgt.numbers = new int[]{1, 3, 4};
        tgt.list.addAll(Arrays.asList("a", "c", "d"));
        tgt.set.add("x");
        tgt.set.add("y");
        tgt.map.put("k", 2);
        tgt.map.put("newKey", 5);

        // Compute deltas
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Apply them to a *clone* of the source (so we keep the original for comparison)
        Node mutableCopy = deepCopyNode(src);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                mutableCopy,
                deltas,
                ID_FETCHER,
                GraphComparator.getJavaDeltaProcessor()
        );
        Assert.assertTrue("No errors expected when applying valid deltas", errors.isEmpty());

        // After applying deltas, source and target should be equivalent (no further deltas)
        List<GraphComparator.Delta> postApply = GraphComparator.compare(mutableCopy, tgt, ID_FETCHER);
        Assert.assertTrue("After applying deltas, graphs should be identical", postApply.isEmpty());

        // Spot‑check a few fields
        Assert.assertEquals("new", mutableCopy.name);
        Assert.assertArrayEquals(new int[]{1, 3, 4}, mutableCopy.numbers);
        Assert.assertEquals(Arrays.asList("a", "c", "d"), mutableCopy.list);
        Assert.assertTrue(mutableCopy.set.containsAll(Arrays.asList("x", "y")));
        Assert.assertEquals(Integer.valueOf(2), mutableCopy.map.get("k"));
        Assert.assertEquals(Integer.valueOf(5), mutableCopy.map.get("newKey"));
    }

    /**
     * Simple deep‑copy for Node (fields are mutable but contain only
     * primitives / collections of immutable elements, sufficient for the test).
     */
    private Node deepCopyNode(Node original) {
        Node copy = new Node(original.id);
        copy.name = original.name;
        copy.numbers = Arrays.copyOf(original.numbers, original.numbers.length);
        copy.list = new ArrayList<>(original.list);
        copy.set = new HashSet<>(original.set);
        copy.map = new HashMap<>(original.map);
        return copy;
    }

    /* ----------------------------------------------------------------------
     * 5.  Verify getJavaDeltaProcessor returns a functional processor
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetJavaDeltaProcessorIsNotNullAndImplementsAllMethods() {
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();
        Assert.assertNotNull(processor);
        // Ensure each method can be called without throwing (use simple dummy data)
        Node dummy = new Node(999L);
        dummy.numbers = new int[]{0};

        // Prepare a delta for ARRAY_SET_ELEMENT
        GraphComparator.Delta delta = new GraphComparator.Delta(999L, "numbers", "-root-.numbers[0]", 0, 7, 0);
        delta.setCmd(GraphComparator.Delta.Command.ARRAY_SET_ELEMENT);
        try {
            processor.processArraySetElement(dummy, Node.class.getField("numbers"), delta);
        } catch (Exception e) {
            Assert.fail("processArraySetElement threw: " + e);
        }
        Assert.assertEquals(7, dummy.numbers[0]);
    }
}
