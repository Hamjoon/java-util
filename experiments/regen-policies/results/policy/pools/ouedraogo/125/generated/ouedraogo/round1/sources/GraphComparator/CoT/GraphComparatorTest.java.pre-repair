package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;

public class GraphComparatorTest {

    /* -------------------------------------------------
     *  Helper classes used in the tests
     * ------------------------------------------------- */
    public static class SimpleNode {
        public int value;
        public SimpleNode child;

        public SimpleNode() {}
        public SimpleNode(int v) { this.value = v; }
    }

    public static class Container {
        public int[] nums;
        public List<String> list;
        public Set<String> set;
        public Map<String, Integer> map;

        public Container() {}
    }

    public static class Bad {
        public String notArray;
        public Bad() { this.notArray = "oops"; }
    }

    /** Simple ID implementation that uses System.identityHashCode() as the unique id. */
    private static final GraphComparator.ID ID_FETCHER = new GraphComparator.ID() {
        @Override
        public Object getId(Object objectToId) {
            return System.identityHashCode(objectToId);
        }
    };

    private GraphComparator.DeltaProcessor javaProcessor;

    @Before
    public void setUp() {
        javaProcessor = GraphComparator.getJavaDeltaProcessor();
        assertNotNull("JavaDeltaProcessor must not be null", javaProcessor);
    }

    /* -------------------------------------------------
     *  1. Public method signatures
     * ------------------------------------------------- */
    // public static List<Delta> compare(Object source, Object target, final ID idFetcher)
    // public static List<DeltaError> applyDelta(Object source, List<Delta> commands,
    //                                         final ID idFetcher, DeltaProcessor deltaProcessor,
    //                                         boolean ... failFast)
    // public static DeltaProcessor getJavaDeltaProcessor()
    // public static Command fromName(String name)   (enum method)

    /* -------------------------------------------------
     *  2. Basic test cases for each public method
     * ------------------------------------------------- */

    @Test
    public void testGetJavaDeltaProcessor_NotNull() {
        assertNotNull(GraphComparator.getJavaDeltaProcessor());
    }

    @Test
    public void testCommandFromName_Valid() {
        assertEquals(ARRAY_SET_ELEMENT, GraphComparator.Delta.Command.fromName("array.setElement"));
        assertEquals(LIST_RESIZE, GraphComparator.Delta.Command.fromName("LIST.RESIZE".toLowerCase()));
    }

    @Test
    public void testCommandFromName_Invalid() {
        try {
            GraphComparator.Delta.Command.fromName("nonexistent");
            fail("Expected IllegalArgumentException for unknown command name");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            GraphComparator.Delta.Command.fromName("");
            fail("Expected IllegalArgumentException for empty command name");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            GraphComparator.Delta.Command.fromName(null);
            fail("Expected IllegalArgumentException for null command name");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testCompare_PrimitiveFieldChange() {
        SimpleNode src = new SimpleNode(1);
        SimpleNode tgt = new SimpleNode(2);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        assertEquals(1, deltas.size());

        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals("value", d.getFieldName());
        assertEquals(1, d.getSourceValue());
        assertEquals(2, d.getTargetValue());
    }

    @Test
    public void testCompare_NullVsObject() {
        SimpleNode src = null;
        SimpleNode tgt = new SimpleNode(5);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        assertEquals(1, deltas.size());

        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertNull(d.getSourceValue());
        assertEquals(tgt, d.getTargetValue());
    }

    @Test
    public void testCompare_ArrayResizeAndSetElement() {
        int[] src = new int[]{1, 2};
        int[] tgt = new int[]{1, 2, 3};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect one resize and one set element for the new index
        assertEquals(2, deltas.size());

        boolean resizeSeen = false, setSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == ARRAY_RESIZE) {
                resizeSeen = true;
                assertEquals(3, d.getOptionalKey());
            } else if (d.getCmd() == ARRAY_SET_ELEMENT) {
                setSeen = true;
                assertEquals(2, d.getOptionalKey()); // index 2
                assertNull(d.getSourceValue());
                assertEquals(3, d.getTargetValue());
            }
        }
        assertTrue(resizeSeen);
        assertTrue(setSeen);
    }

    @Test
    public void testCompare_SetAddRemove() {
        Set<String> src = new HashSet<>(Arrays.asList("a"));
        Set<String> tgt = new HashSet<>(Arrays.asList("b"));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect one remove ("a") and one add ("b")
        assertEquals(2, deltas.size());

        boolean addSeen = false, removeSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == SET_ADD) {
                addSeen = true;
                assertEquals("b", d.getTargetValue());
            } else if (d.getCmd() == SET_REMOVE) {
                removeSeen = true;
                assertEquals("a", d.getSourceValue());
            }
        }
        assertTrue(addSeen);
        assertTrue(removeSeen);
    }

    @Test
    public void testCompare_MapPutAndUpdate() {
        Map<String, String> src = new HashMap<>();
        src.put("k1", "v1");
        Map<String, String> tgt = new HashMap<>();
        tgt.put("k1", "v2"); // changed value
        tgt.put("k2", "v3"); // new entry

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect two MAP_PUT deltas (k1 updated, k2 added)
        assertEquals(2, deltas.size());

        Set<String> keys = new HashSet<>();
        for (GraphComparator.Delta d : deltas) {
            assertEquals(MAP_PUT, d.getCmd());
            keys.add((String) d.getOptionalKey());
        }
        assertTrue(keys.containsAll(Arrays.asList("k1", "k2")));
    }

    @Test
    public void testCompare_OrphanDetection() {
        SimpleNode orphan = new SimpleNode(99);
        SimpleNode src = new SimpleNode(1);
        src.child = orphan; // src graph has an extra node

        SimpleNode tgt = new SimpleNode(1);
        // tgt has no child -> orphan should be reported

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        boolean orphanSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == OBJECT_ORPHAN) {
                orphanSeen = true;
                assertEquals(System.identityHashCode(orphan), d.getId());
            }
        }
        assertTrue("Orphan delta expected", orphanSeen);
    }

    /* -------------------------------------------------
     *  3. Edge case and exception handling tests for applyDelta
     * ------------------------------------------------- */

    @Test
    public void testApplyDelta_UpdatesContainerCorrectly() {
        // Build source container
        Container src = new Container();
        src.nums = new int[]{1, 2};
        src.list = new ArrayList<>(Arrays.asList("x"));
        src.set = new HashSet<>(Arrays.asList("a"));
        src.map = new HashMap<>();
        src.map.put("k1", 10);

        // Build target container
        Container tgt = new Container();
        tgt.nums = new int[]{1, 2, 3}; // extra element
        tgt.list = new ArrayList<>(Arrays.asList("x", "y")); // resized + new element
        tgt.set = new HashSet<>(Arrays.asList("a", "b")); // add element
        tgt.map = new HashMap<>();
        tgt.map.put("k1", 20); // changed value
        tgt.map.put("k2", 30); // new entry

        // Compute deltas
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Apply them to src
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas, ID_FETCHER, javaProcessor);
        assertTrue("No errors expected while applying valid deltas", errors.isEmpty());

        // Verify source now matches target
        assertArrayEquals(tgt.nums, src.nums);
        assertEquals(tgt.list, src.list);
        assertEquals(tgt.set, src.set);
        assertEquals(tgt.map, src.map);
    }

    @Test
    public void testApplyDelta_FieldMissingProducesError() {
        Bad src = new Bad();

        // Create a delta that references a non‑existent field "doesNotExist"
        GraphComparator.Delta delta = new GraphComparator.Delta(
                ID_FETCHER.getId(src),
                "doesNotExist",
                GraphComparator.ROOT,
                null,
                "newValue",
                null);
        delta.setCmd(OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Collections.singletonList(delta),
                ID_FETCHER,
                javaProcessor);

        assertEquals(1, errors.size());
        GraphComparator.DeltaError err = errors.get(0);
        assertTrue(err.getError().contains("field name missing"));
    }

    @Test
    public void testApplyDelta_ArraySetElement_WithWrongFieldType() throws NoSuchFieldException {
        Bad src = new Bad();

        // Build a delta that tries to treat the String field as an array
        GraphComparator.Delta delta = new GraphComparator.Delta(
                ID_FETCHER.getId(src),
                "notArray",
                GraphComparator.ROOT,
                null,
                "ignored",
                0); // optionalKey = index
        delta.setCmd(ARRAY_SET_ELEMENT);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Collections.singletonList(delta),
                ID_FETCHER,
                javaProcessor);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("is not an Array [] type"));
    }

    @Test
    public void testApplyDelta_ArrayResize_NegativeSizeThrowsError() throws NoSuchFieldException {
        // Container with an int[] field
        Container src = new Container();
        src.nums = new int[]{1, 2};

        // delta with negative optionalKey -> should trigger IllegalArgumentException inside Helper.getResizeValue
        GraphComparator.Delta delta = new GraphComparator.Delta(
                ID_FETCHER.getId(src),
                "nums",
                GraphComparator.ROOT,
                null,
                null,
                -5);
        delta.setCmd(ARRAY_RESIZE);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Collections.singletonList(delta),
                ID_FETCHER,
                javaProcessor);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("failed, the optionalKey must be a integer value 0 or greater"));
    }

    @Test
    public void testApplyDelta_FailFastStopsAfterFirstError() {
        // Two deltas: first will fail (missing field), second would succeed if reached
        SimpleNode src = new SimpleNode(1);
        SimpleNode tgt = new SimpleNode(2);

        GraphComparator.Delta badDelta = new GraphComparator.Delta(
                ID_FETCHER.getId(src),
                "nonExisting",
                GraphComparator.ROOT,
                null,
                99,
                null);
        badDelta.setCmd(OBJECT_ASSIGN_FIELD);

        GraphComparator.Delta goodDelta = new GraphComparator.Delta(
                ID_FETCHER.getId(src),
                "value",
                GraphComparator.ROOT,
                src.value,
                tgt.value,
                null);
        goodDelta.setCmd(OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                src,
                Arrays.asList(badDelta, goodDelta),
                ID_FETCHER,
                javaProcessor,
                true); // failFast = true

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("field name missing"));
        // Verify that the good delta was NOT applied
        assertEquals(1, src.value);
    }

    /* -------------------------------------------------
     *  4. Additional sanity checks for Delta equals / hashCode
     * ------------------------------------------------- */
    @Test
    public void testDeltaEqualsAndHashCode() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "ptr", "src", "tgt", null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "ptr", "a", "b", null);
        GraphComparator.Delta d3 = new GraphComparator.Delta(3L, "field", "different", "src", "tgt", null);

        assertTrue(d1.equals(d2)); // same srcPtr
        assertEquals(d1.hashCode(), d2.hashCode());

        assertFalse(d1.equals(d3));
    }
}
