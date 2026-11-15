package com.cedarsoftware.util;

import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;

import org.junit.Test;

/**
 * Comprehensive JUnit4 tests for {@link GraphComparator}.
 */
public class GraphComparatorTest {

    /* ---------- Helper classes used in the tests ---------- */

    public static class SimpleNode {
        public String id;
        public int value;
        public SimpleNode(String id, int value) {
            this.id = id;
            this.value = value;
        }
    }

    public static class ParentNode {
        public String id;
        public SimpleNode child;
        public ParentNode(String id, SimpleNode child) {
            this.id = id;
            this.child = child;
        }
    }

    public static class ArrayHolder {
        public String id;
        public int[] numbers;
        public ArrayHolder(String id, int[] numbers) {
            this.id = id;
            this.numbers = numbers;
        }
    }

    public static class SetHolder {
        public String id;
        public Set<String> values;
        public SetHolder(String id, Set<String> values) {
            this.id = id;
            this.values = values;
        }
    }

    public static class MapHolder {
        public String id;
        public Map<String, Integer> map;
        public MapHolder(String id, Map<String, Integer> map) {
            this.id = id;
            this.map = map;
        }
    }

    public static class ListHolder {
        public String id;
        public List<String> list;
        public ListHolder(String id, List<String> list) {
            this.id = id;
            this.list = list;
        }
    }

    /** ID fetcher that returns the public field {@code id} of any object. */
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

    /* ---------- Tests for Delta class ---------- */

    @Test
    public void testDeltaEqualsAndHashCode() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "ptr", "a", "b", null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "ptr", "c", "d", null);
        GraphComparator.Delta d3 = new GraphComparator.Delta(3L, "field", "different", "x", "y", null);

        assertTrue(d1.equals(d2));               // same srcPtr -> equal
        assertEquals(d1.hashCode(), d2.hashCode());

        assertFalse(d1.equals(d3));
        assertNotEquals(d1.hashCode(), d3.hashCode());
    }

    @Test
    public void testDeltaGettersSettersAndToString() {
        GraphComparator.Delta d = new GraphComparator.Delta(10L, "f", "p", "src", "tgt", "key");
        d.setCmd(OBJECT_ASSIGN_FIELD);
        assertEquals(10L, d.getId());
        assertEquals("f", d.getFieldName());
        assertEquals("src", d.getSourceValue());
        assertEquals("tgt", d.getTargetValue());
        assertEquals("key", d.getOptionalKey());
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());

        String s = d.toString();
        assertTrue(s.contains("id=10"));
        assertTrue(s.contains("fieldName='f'"));
        assertTrue(s.contains("srcValue=src"));
        assertTrue(s.contains("targetValue=tgt"));
        assertTrue(s.contains("optionalKey=key"));
        assertTrue(s.contains("cmd='OBJECT_ASSIGN_FIELD'"));
    }

    /* ---------- Tests for Command enum ---------- */

    @Test
    public void testCommandFromNameValid() {
        assertEquals(ARRAY_SET_ELEMENT, GraphComparator.Delta.Command.fromName("array.setelement"));
        assertEquals(SET_ADD, GraphComparator.Delta.Command.fromName("set.add"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommandFromNameInvalid() {
        GraphComparator.Delta.Command.fromName("non.existent");
    }

    /* ---------- Tests for compare() ---------- */

    @Test
    public void testComparePrimitiveFieldChangeProducesAssignDelta() {
        SimpleNode src = new SimpleNode("n1", 5);
        SimpleNode tgt = new SimpleNode("n1", 10); // same id, different value

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals("value", d.getFieldName()); // field name is "value"
        assertEquals(5, d.getSourceValue());
        assertEquals(10, d.getTargetValue());
    }

    @Test
    public void testCompareOrphanDetection() {
        SimpleNode child = new SimpleNode("c1", 1);
        ParentNode src = new ParentNode("p1", child);
        ParentNode tgt = new ParentNode("p1", null); // child removed

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect two deltas: assign null to field, then orphan for child id
        assertEquals(2, deltas.size());

        boolean foundAssign = false, foundOrphan = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == OBJECT_ASSIGN_FIELD) {
                foundAssign = true;
                assertEquals("child", d.getFieldName());
                assertEquals(child, d.getSourceValue());
                assertNull(d.getTargetValue());
            } else if (d.getCmd() == OBJECT_ORPHAN) {
                foundOrphan = true;
                assertNull(d.getFieldName());
                assertEquals("c1", d.getId());
            }
        }
        assertTrue(foundAssign);
        assertTrue(foundOrphan);
    }

    @Test
    public void testCompareArrayResizeAndSetElement() {
        ArrayHolder src = new ArrayHolder("a1", new int[]{1, 2});
        ArrayHolder tgt = new ArrayHolder("a1", new int[]{1, 3, 4});

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect one ARRAY_RESIZE and two ARRAY_SET_ELEMENT (index 1 and 2)
        int resizeCnt = 0, setCnt = 0;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == ARRAY_RESIZE) resizeCnt++;
            if (d.getCmd() == ARRAY_SET_ELEMENT) setCnt++;
        }
        assertEquals(1, resizeCnt);
        assertEquals(2, setCnt);
    }

    @Test
    public void testCompareSetAddAndRemove() {
        Set<String> srcSet = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> tgtSet = new HashSet<>(Arrays.asList("b", "c"));
        SetHolder src = new SetHolder("s1", srcSet);
        SetHolder tgt = new SetHolder("s1", tgtSet);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect one SET_REMOVE (a) and one SET_ADD (c)
        boolean hasAdd = false, hasRemove = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == SET_ADD) {
                hasAdd = true;
                assertEquals("c", d.getTargetValue());
            } else if (d.getCmd() == SET_REMOVE) {
                hasRemove = true;
                assertEquals("a", d.getSourceValue());
            }
        }
        assertTrue(hasAdd);
        assertTrue(hasRemove);
    }

    @Test
    public void testCompareMapPutAndRemove() {
        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("x", 1);
        srcMap.put("y", 2);
        Map<String, Integer> tgtMap = new HashMap<>();
        tgtMap.put("y", 3); // changed value
        tgtMap.put("z", 4); // new entry

        MapHolder src = new MapHolder("m1", srcMap);
        MapHolder tgt = new MapHolder("m1", tgtMap);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect: MAP_REMOVE for "x", MAP_PUT for "y" (value change), MAP_PUT for "z" (new)
        boolean removeX = false, putY = false, putZ = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == MAP_REMOVE && "x".equals(d.getOptionalKey())) removeX = true;
            if (d.getCmd() == MAP_PUT && "y".equals(d.getOptionalKey())) putY = true;
            if (d.getCmd() == MAP_PUT && "z".equals(d.getOptionalKey())) putZ = true;
        }
        assertTrue(removeX);
        assertTrue(putY);
        assertTrue(putZ);
    }

    @Test
    public void testCompareListResizeAndSetElement() {
        List<String> srcList = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> tgtList = new ArrayList<>(Arrays.asList("a", "c", "d"));
        ListHolder src = new ListHolder("l1", srcList);
        ListHolder tgt = new ListHolder("l1", tgtList);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect LIST_RESIZE and two LIST_SET_ELEMENT (index 1 and 2)
        boolean hasResize = false, set1 = false, set2 = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == LIST_RESIZE) hasResize = true;
            if (d.getCmd() == LIST_SET_ELEMENT) {
                int idx = (Integer) d.getOptionalKey();
                if (idx == 1) set1 = true;
                if (idx == 2) set2 = true;
            }
        }
        assertTrue(hasResize);
        assertTrue(set1);
        assertTrue(set2);
    }

    /* ---------- Tests for applyDelta() using the built‑in JavaDeltaProcessor ---------- */

    @Test
    public void testApplyDeltaUpdatesPrimitiveField() {
        SimpleNode src = new SimpleNode("n1", 5);
        SimpleNode tgt = new SimpleNode("n1", 42);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        assertEquals(1, deltas.size());

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas,
                ID_FETCHER, GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.isEmpty());
        assertEquals(42, src.value);
    }

    @Test
    public void testApplyDeltaArrayResizeAndSet() {
        ArrayHolder src = new ArrayHolder("a1", new int[]{1, 2});
        ArrayHolder tgt = new ArrayHolder("a1", new int[]{1, 3, 4});
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                deltas, ID_FETCHER, GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.isEmpty());

        assertArrayEquals(new int[]{1, 3, 4}, src.numbers);
    }

    @Test
    public void testApplyDeltaSetAddRemove() {
        SetHolder src = new SetHolder("s1", new HashSet<>(Arrays.asList("a", "b")));
        SetHolder tgt = new SetHolder("s1", new HashSet<>(Arrays.asList("b", "c")));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                deltas, ID_FETCHER, GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.isEmpty());

        assertTrue(src.values.contains("b"));
        assertTrue(src.values.contains("c"));
        assertFalse(src.values.contains("a"));
    }

    @Test
    public void testApplyDeltaMapPutRemove() {
        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("x", 1);
        srcMap.put("y", 2);
        MapHolder src = new MapHolder("m1", srcMap);

        Map<String, Integer> tgtMap = new HashMap<>();
        tgtMap.put("y", 3); // change
        tgtMap.put("z", 4); // add
        MapHolder tgt = new MapHolder("m1", tgtMap);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                deltas, ID_FETCHER, GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.isEmpty());

        assertFalse(src.map.containsKey("x"));
        assertEquals(Integer.valueOf(3), src.map.get("y"));
        assertEquals(Integer.valueOf(4), src.map.get("z"));
    }

    @Test
    public void testApplyDeltaListResizeAndSet() {
        ListHolder src = new ListHolder("l1", new ArrayList<>(Arrays.asList("a", "b")));
        ListHolder tgt = new ListHolder("l1", new ArrayList<>(Arrays.asList("a", "c", "d")));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                deltas, ID_FETCHER, GraphComparator.getJavaDeltaProcessor());
        assertTrue(errors.isEmpty());

        assertEquals(3, src.list.size());
        assertEquals("a", src.list.get(0));
        assertEquals("c", src.list.get(1));
        assertEquals("d", src.list.get(2));
    }

    @Test
    public void testApplyDeltaWithMissingFieldProducesError() {
        // Create a delta that refers to a non‑existent field
        GraphComparator.Delta bogus = new GraphComparator.Delta("dummyId", "noSuchField",
                "ptr", "src", "tgt", null);
        bogus.setCmd(OBJECT_ASSIGN_FIELD);

        SimpleNode src = new SimpleNode("n1", 5);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Collections.singletonList(bogus), ID_FETCHER, GraphComparator.getJavaDeltaProcessor());

        assertEquals(1, errors.size());
        GraphComparator.DeltaError err = errors.get(0);
        assertTrue(err.getError().contains("field name missing"));
        assertEquals(bogus, err.getTargetValue()); // the delta itself is stored inside the error
    }

    @Test
    public void testDeltaErrorToStringFormatting() {
        GraphComparator.Delta base = new GraphComparator.Delta(1L, "f", "p", "src", "tgt", null);
        GraphComparator.DeltaError de = new GraphComparator.DeltaError("boom", base);
        String s = de.toString();
        assertTrue(s.startsWith("boom ("));
        assertTrue(s.contains("Delta {"));
    }
}
