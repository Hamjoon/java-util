package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.lang.reflect.Field;
import java.util.*;

import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;
import static org.junit.Assert.*;

public class GraphComparatorTest {

    /** Simple node class used for ID based testing */
    public static class Node {
        public Long id;
        public int intValue;
        public String name;
        public int[] numbers;
        public List<String> list;
        public Set<String> set;
        public Map<String, String> map;
        public Node child;

        public Node(Long id) {
            this.id = id;
        }
    }

    private GraphComparator.ID idFetcher;

    @Before
    public void setUp() {
        // ID fetcher simply returns the public field "id"
        idFetcher = obj -> {
            try {
                Field f = obj.getClass().getField("id");
                return f.get(obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /** Helper to locate a delta with a given command */
    private GraphComparator.Delta findDelta(List<GraphComparator.Delta> deltas,
                                            GraphComparator.Delta.Command cmd) {
        for (GraphComparator.Delta d : deltas) {
            if (cmd.equals(d.getCmd())) {
                return d;
            }
        }
        return null;
    }

    @Test
    public void testDeltaEqualsAndHashCode() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "ptr", "src", "tgt", null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "ptr", "a", "b", null);
        assertTrue(d1.equals(d2));
        assertEquals(d1.hashCode(), d2.hashCode());

        GraphComparator.Delta d3 = new GraphComparator.Delta(1L, "field", "different", "src", "tgt", null);
        assertFalse(d1.equals(d3));
    }

    @Test
    public void testCommandFromNameValidAndInvalid() {
        assertEquals(ARRAY_SET_ELEMENT, GraphComparator.Delta.Command.fromName("array.setElement"));
        assertEquals(ARRAY_RESIZE, GraphComparator.Delta.Command.fromName("ARRAY.RESIZE"));
        assertEquals(OBJECT_ASSIGN_FIELD, GraphComparator.Delta.Command.fromName("object.assignfield"));
        try {
            GraphComparator.Delta.Command.fromName("nonexistent");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            GraphComparator.Delta.Command.fromName(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testCompareLogicalPrimitiveDifference() {
        Node src = new Node(1L);
        src.intValue = 5;
        src.name = "Alice";

        Node tgt = new Node(1L);
        tgt.intValue = 10; // changed
        tgt.name = "Alice";

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        GraphComparator.Delta d = findDelta(deltas, OBJECT_ASSIGN_FIELD);
        assertNotNull(d);
        assertEquals("intValue", d.getFieldName());
        assertEquals(5, d.getSourceValue());
        assertEquals(10, d.getTargetValue());
    }

    @Test
    public void testCompareArrayResizeAndSetElement() {
        Node src = new Node(1L);
        src.numbers = new int[]{1, 2, 3};

        Node tgt = new Node(1L);
        tgt.numbers = new int[]{1, 9, 3, 4}; // second element changed, extra element added

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Expect a resize delta and a set-element delta for index 1 and 3
        GraphComparator.Delta resize = findDelta(deltas, ARRAY_RESIZE);
        assertNotNull(resize);
        assertEquals(4, resize.getOptionalKey());

        GraphComparator.Delta set1 = findDelta(deltas, ARRAY_SET_ELEMENT);
        assertNotNull(set1);
        // there are two set-element deltas; ensure at least one has srcValue 2 and targetValue 9
        boolean found = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == ARRAY_SET_ELEMENT && Integer.valueOf(2).equals(d.getSourceValue())) {
                assertEquals(9, d.getTargetValue());
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testCompareSetAddRemove() {
        Node src = new Node(1L);
        src.set = new HashSet<>(Arrays.asList("a", "b"));

        Node tgt = new Node(1L);
        tgt.set = new HashSet<>(Arrays.asList("b", "c")); // "a" removed, "c" added

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        GraphComparator.Delta add = findDelta(deltas, SET_ADD);
        GraphComparator.Delta remove = findDelta(deltas, SET_REMOVE);
        assertNotNull(add);
        assertEquals("c", add.getTargetValue());
        assertNotNull(remove);
        assertEquals("a", remove.getSourceValue());
    }

    @Test
    public void testCompareMapPutRemove() {
        Node src = new Node(1L);
        src.map = new HashMap<>();
        src.map.put("k1", "v1");
        src.map.put("k2", "v2");

        Node tgt = new Node(1L);
        tgt.map = new HashMap<>();
        tgt.map.put("k1", "v1-modified"); // changed value
        tgt.map.put("k3", "v3"); // new entry

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        GraphComparator.Delta put = findDelta(deltas, MAP_PUT);
        GraphComparator.Delta remove = findDelta(deltas, MAP_REMOVE);
        assertNotNull(put);
        assertEquals("k1", put.getOptionalKey());
        assertEquals("v1-modified", put.getTargetValue());

        assertNotNull(remove);
        assertEquals("k2", remove.getOptionalKey());
    }

    @Test
    public void testCompareListResizeAndSetElement() {
        Node src = new Node(1L);
        src.list = new ArrayList<>(Arrays.asList("a", "b"));

        Node tgt = new Node(1L);
        tgt.list = new ArrayList<>(Arrays.asList("a", "c", "d")); // second element changed, new element added

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        GraphComparator.Delta resize = findDelta(deltas, LIST_RESIZE);
        assertNotNull(resize);
        assertEquals(3, resize.getOptionalKey());

        // Find set-element delta for index 1 (b -> c)
        boolean found = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == LIST_SET_ELEMENT && "b".equals(d.getSourceValue())) {
                assertEquals("c", d.getTargetValue());
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testOrphanDetection() {
        Node src = new Node(1L);
        src.child = new Node(2L); // orphan candidate

        Node tgt = new Node(1L);
        // target has no child

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        GraphComparator.Delta orphan = findDelta(deltas, OBJECT_ORPHAN);
        assertNotNull(orphan);
        assertEquals(Long.valueOf(2L), orphan.getId());
    }

    @Test
    public void testApplyDeltaSuccess() {
        // Build source and target with a simple field change, a set addition, and a list resize
        Node src = new Node(1L);
        src.intValue = 5;
        src.name = "Bob";
        src.set = new HashSet<>(Arrays.asList("x"));
        src.list = new ArrayList<>(Arrays.asList("one"));

        Node tgt = new Node(1L);
        tgt.intValue = 8; // change
        tgt.name = "Bob";
        tgt.set = new HashSet<>(Arrays.asList("x", "y")); // add "y"
        tgt.list = new ArrayList<>(Arrays.asList("one", "two")); // resize + add

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Apply deltas
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas,
                idFetcher, GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());

        // Verify mutations
        assertEquals(8, src.intValue);
        assertTrue(src.set.contains("y"));
        assertEquals(2, src.list.size());
        assertEquals("two", src.list.get(1));
    }

    @Test
    public void testApplyDeltaErrorMissingObject() {
        // Create a delta referencing a non‑existent id
        GraphComparator.Delta delta = new GraphComparator.Delta(999L, "intValue", "ptr", 1, 2, null);
        delta.setCmd(OBJECT_ASSIGN_FIELD);
        List<GraphComparator.Delta> list = Collections.singletonList(delta);

        Node src = new Node(1L);
        src.intValue = 1;

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, list,
                idFetcher, GraphComparator.getJavaDeltaProcessor());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("source object not found"));
    }

    @Test
    public void testApplyDeltaErrorFieldMissing() {
        // Delta with a field name that does not exist on the class
        GraphComparator.Delta delta = new GraphComparator.Delta(1L, "nonExistent", "ptr", 1, 2, null);
        delta.setCmd(OBJECT_ASSIGN_FIELD);
        List<GraphComparator.Delta> list = Collections.singletonList(delta);

        Node src = new Node(1L);
        src.intValue = 1;

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, list,
                idFetcher, GraphComparator.getJavaDeltaProcessor());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("field name missing"));
    }

    @Test
    public void testDeltaErrorToString() {
        GraphComparator.Delta base = new GraphComparator.Delta(1L, "field", "ptr", "src", "tgt", null);
        base.setCmd(OBJECT_ASSIGN_FIELD);
        GraphComparator.DeltaError error = new GraphComparator.DeltaError("boom", base);
        String s = error.toString();
        assertTrue(s.contains("boom"));
        assertTrue(s.contains("OBJECT_ASSIGN_FIELD"));
    }
}
