package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;

public class GraphComparatorTest {

    /* -------------------------------------------------
     * Helper classes used in the tests
     * ------------------------------------------------- */
    public static class Node {
        public Long id;
        public int primitive;
        public int[] intArray;
        public List<String> stringList;
        public Set<String> stringSet;
        public Map<String, Integer> stringMap;
        public Node child;          // reference to another node (to test ID handling)

        public Node(Long id) {
            this.id = id;
        }
    }

    private GraphComparator.ID idFetcher = new GraphComparator.ID() {
        @Override
        public Object getId(Object objectToId) {
            return ((Node) objectToId).id;
        }
    };

    private GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();

    /* -------------------------------------------------
     * 1. Public methods of GraphComparator
     * ------------------------------------------------- */
    // public static List<Delta> compare(Object source, Object target, final ID idFetcher)
    // public static List<DeltaError> applyDelta(Object source, List<Delta> commands,
    //                                          final ID idFetcher, DeltaProcessor deltaProcessor,
    //                                          boolean ... failFast)
    // public static DeltaProcessor getJavaDeltaProcessor()
    // public static Command fromName(String name)   (enum method)

    /* -------------------------------------------------
     * 2. Basic compare – identical objects → no deltas
     * ------------------------------------------------- */
    @Test
    public void testCompareIdenticalObjects() {
        Node a = new Node(1L);
        a.primitive = 10;
        a.intArray = new int[]{1, 2, 3};
        a.stringList = new ArrayList<>(Arrays.asList("a", "b"));
        a.stringSet = new LinkedHashSet<>(Arrays.asList("x", "y"));
        a.stringMap = new HashMap<>();
        a.stringMap.put("k1", 100);
        a.stringMap.put("k2", 200);
        a.child = new Node(2L);
        a.child.primitive = 5;

        List<GraphComparator.Delta> deltas = GraphComparator.compare(a, a, idFetcher);
        assertTrue("Identical objects should produce no deltas", deltas.isEmpty());
    }

    /* -------------------------------------------------
     * 3. Primitive field difference → OBJECT_ASSIGN_FIELD delta
     * ------------------------------------------------- */
    @Test
    public void testComparePrimitiveDifference() {
        Node src = new Node(1L);
        src.primitive = 10;

        Node tgt = new Node(1L);
        tgt.primitive = 20; // different

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(1, deltas.size());

        GraphComparator.Delta d = deltas.get(0);
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals("primitive", d.getFieldName());
        assertEquals(10, d.getSourceValue());
        assertEquals(20, d.getTargetValue());
    }

    /* -------------------------------------------------
     * 4. Array resize and element change
     * ------------------------------------------------- */
    @Test
    public void testCompareArrayResizeAndSet() {
        Node src = new Node(1L);
        src.intArray = new int[]{1, 2};

        Node tgt = new Node(1L);
        tgt.intArray = new int[]{1, 9, 3}; // resize to 3 and change element at index 1

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Expect one ARRAY_RESIZE and one ARRAY_SET_ELEMENT
        assertEquals(2, deltas.size());

        boolean resizeSeen = false;
        boolean setSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.ARRAY_RESIZE) {
                resizeSeen = true;
                assertEquals(3, d.getOptionalKey()); // new length
            } else if (d.getCmd() == GraphComparator.Delta.Command.ARRAY_SET_ELEMENT) {
                setSeen = true;
                assertEquals(1, d.getOptionalKey()); // index
                assertEquals(2, d.getSourceValue()); // old value
                assertEquals(9, d.getTargetValue()); // new value
            }
        }
        assertTrue("Resize delta not found", resizeSeen);
        assertTrue("Set-element delta not found", setSeen);
    }

    /* -------------------------------------------------
     * 5. Set add and remove detection
     * ------------------------------------------------- */
    @Test
    public void testCompareSetAddRemove() {
        Node src = new Node(1L);
        src.stringSet = new LinkedHashSet<>(Arrays.asList("A", "B"));

        Node tgt = new Node(1L);
        tgt.stringSet = new LinkedHashSet<>(Arrays.asList("B", "C")); // remove A, add C

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(2, deltas.size());

        Set<GraphComparator.Delta.Command> cmds = new HashSet<>();
        for (GraphComparator.Delta d : deltas) {
            cmds.add(d.getCmd());
            if (d.getCmd() == GraphComparator.Delta.Command.SET_REMOVE) {
                assertEquals("A", d.getSourceValue());
            } else if (d.getCmd() == GraphComparator.Delta.Command.SET_ADD) {
                assertEquals("C", d.getTargetValue());
            }
        }
        assertTrue(cmds.contains(GraphComparator.Delta.Command.SET_REMOVE));
        assertTrue(cmds.contains(GraphComparator.Delta.Command.SET_ADD));
    }

    /* -------------------------------------------------
     * 6. Map put and remove detection
     * ------------------------------------------------- */
    @Test
    public void testCompareMapPutRemove() {
        Node src = new Node(1L);
        src.stringMap = new HashMap<>();
        src.stringMap.put("one", 1);
        src.stringMap.put("two", 2);

        Node tgt = new Node(1L);
        tgt.stringMap = new HashMap<>();
        tgt.stringMap.put("two", 20); // value change
        tgt.stringMap.put("three", 3); // new entry

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Expect one MAP_REMOVE (key "one") and two MAP_PUT (key "two" changed, key "three" added)
        assertEquals(3, deltas.size());

        boolean removeSeen = false;
        int putCount = 0;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.MAP_REMOVE) {
                removeSeen = true;
                assertEquals("one", d.getOptionalKey());
            } else if (d.getCmd() == GraphComparator.Delta.Command.MAP_PUT) {
                putCount++;
                if ("two".equals(d.getOptionalKey())) {
                    assertEquals(2, d.getSourceValue());
                    assertEquals(20, d.getTargetValue());
                } else if ("three".equals(d.getOptionalKey())) {
                    assertNull(d.getSourceValue());
                    assertEquals(3, d.getTargetValue());
                }
            }
        }
        assertTrue("Map remove delta missing", removeSeen);
        assertEquals(2, putCount);
    }

    /* -------------------------------------------------
     * 7. List resize and element set detection
     * ------------------------------------------------- */
    @Test
    public void testCompareListResizeSet() {
        Node src = new Node(1L);
        src.stringList = new ArrayList<>(Arrays.asList("a", "b"));

        Node tgt = new Node(1L);
        tgt.stringList = new ArrayList<>(Arrays.asList("a", "X", "c")); // resize to 3, change index 1

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Expect one LIST_RESIZE and one LIST_SET_ELEMENT
        assertEquals(2, deltas.size());

        boolean resizeSeen = false;
        boolean setSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.LIST_RESIZE) {
                resizeSeen = true;
                assertEquals(3, d.getOptionalKey());
            } else if (d.getCmd() == GraphComparator.Delta.Command.LIST_SET_ELEMENT) {
                setSeen = true;
                assertEquals(1, d.getOptionalKey()); // index
                assertEquals("b", d.getSourceValue());
                assertEquals("X", d.getTargetValue());
            }
        }
        assertTrue("List resize delta missing", resizeSeen);
        assertTrue("List set-element delta missing", setSeen);
    }

    /* -------------------------------------------------
     * 8. Orphan detection (source node without counterpart)
     * ------------------------------------------------- */
    @Test
    public void testCompareOrphanDetection() {
        Node src = new Node(1L);
        src.child = new Node(2L); // present only in source

        Node tgt = new Node(1L);
        // tgt has no child

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Expect a single OBJECT_ORPHAN delta for id 2L
        boolean orphanFound = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == GraphComparator.Delta.Command.OBJECT_ORPHAN && d.getId().equals(2L)) {
                orphanFound = true;
                break;
            }
        }
        assertTrue("Orphan delta not generated", orphanFound);
    }

    /* -------------------------------------------------
     * 9. Command.fromName – valid and invalid inputs
     * ------------------------------------------------- */
    @Test
    public void testCommandFromNameValid() {
        GraphComparator.Delta.Command cmd = GraphComparator.Delta.Command.fromName("array.setElement");
        assertEquals(GraphComparator.Delta.Command.ARRAY_SET_ELEMENT, cmd);

        // case‑insensitivity test
        GraphComparator.Delta.Command cmd2 = GraphComparator.Delta.Command.fromName("LiSt_Resize");
        assertEquals(GraphComparator.Delta.Command.LIST_RESIZE, cmd2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommandFromNameNull() {
        GraphComparator.Delta.Command.fromName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommandFromNameEmpty() {
        GraphComparator.Delta.Command.fromName("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommandFromNameUnknown() {
        GraphComparator.Delta.Command.fromName("unknown.command");
    }

    /* -------------------------------------------------
     * 10. applyDelta – successful mutation of source object
     * ------------------------------------------------- */
    @Test
    public void testApplyDeltaSuccess() {
        Node src = new Node(1L);
        src.primitive = 5;
        src.intArray = new int[]{1, 2};
        src.stringList = new ArrayList<>(Arrays.asList("old"));
        src.stringSet = new LinkedHashSet<>(Arrays.asList("A"));
        src.stringMap = new HashMap<>();
        src.stringMap.put("k", 10);

        // Build a target with several differences
        Node tgt = new Node(1L);
        tgt.primitive = 99;                     // primitive change
        tgt.intArray = new int[]{1, 9, 3};       // resize + element change
        tgt.stringList = new ArrayList<>(Arrays.asList("new", "extra"));
        tgt.stringSet = new LinkedHashSet<>(Arrays.asList("A", "B")); // add B
        tgt.stringMap = new HashMap<>();
        tgt.stringMap.put("k", 20);              // value change
        tgt.stringMap.put("newKey", 5);          // new entry

        // Generate deltas
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Apply them
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas, idFetcher, processor);
        assertTrue("No errors expected when applying valid deltas", errors.isEmpty());

        // Verify mutations
        assertEquals(99, src.primitive);
        assertArrayEquals(new int[]{1, 9, 3}, src.intArray);
        assertEquals(Arrays.asList("new", "extra"), src.stringList);
        assertTrue(src.stringSet.containsAll(Arrays.asList("A", "B")));
        assertEquals(Integer.valueOf(20), src.stringMap.get("k"));
        assertEquals(Integer.valueOf(5), src.stringMap.get("newKey"));
    }

    /* -------------------------------------------------
     * 11. applyDelta – missing field results in DeltaError
     * ------------------------------------------------- */
    @Test
    public void testApplyDeltaMissingFieldError() {
        Node src = new Node(1L);
        src.primitive = 1;

        // Manually craft a delta that refers to a non‑existent field
        GraphComparator.Delta badDelta = new GraphComparator.Delta(1L, "nonexistentField",
                GraphComparator.ROOT, 1, 2, null);
        badDelta.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Collections.singletonList(badDelta), idFetcher, processor);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("field name missing"));
    }

    /* -------------------------------------------------
     * 12. applyDelta – failFast behaviour
     * ------------------------------------------------- */
    @Test
    public void testApplyDeltaFailFast() {
        Node src = new Node(1L);
        src.primitive = 1;

        // Two bad deltas: one missing field, another with wrong id
        GraphComparator.Delta delta1 = new GraphComparator.Delta(1L, "badField",
                GraphComparator.ROOT, 1, 2, null);
        delta1.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);

        GraphComparator.Delta delta2 = new GraphComparator.Delta(99L, "primitive",
                GraphComparator.ROOT, 1, 2, null);
        delta2.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Arrays.asList(delta1, delta2), idFetcher, processor, true); // failFast = true

        // Only the first error should be reported
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("field name missing"));
    }

    /* -------------------------------------------------
     * 13. Helper – getResizeValue validation (indirectly via processor)
     * ------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testArrayResizeInvalidOptionalKey() throws Exception {
        // Build a delta with a negative optionalKey for ARRAY_RESIZE
        GraphComparator.Delta badResize = new GraphComparator.Delta(1L, "intArray",
                GraphComparator.ROOT, new int[]{1}, new int[]{1,2}, -5);
        badResize.setCmd(GraphComparator.Delta.Command.ARRAY_RESIZE);

        Node src = new Node(1L);
        src.intArray = new int[]{1};

        // Processor should throw IllegalArgumentException when it asks for resize value
        processor.processArrayResize(src, Node.class.getField("intArray"), badResize);
    }

    /* -------------------------------------------------
     * 14. Helper – getStringValue for array formatting (indirect test)
     * ------------------------------------------------- */
    @Test
    public void testHelperGetStringValueArrayFormatting() throws Exception {
        // Use reflection to access the private helper method
        java.lang.reflect.Method m = GraphComparator.DeltaProcessor.Helper.class
                .getDeclaredMethod("getStringValue", Object.class);
        m.setAccessible(true);
        int[] arr = new int[]{1, 2, 3};
        String result = (String) m.invoke(null, arr);
        assertEquals("[1,2,3]", result);
    }
}
