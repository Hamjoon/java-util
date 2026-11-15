package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.util.*;

import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;
import static org.junit.Assert.*;

public class GraphComparatorTest {

    /* --------------------------------------------------------------------- */
    /* Helper test beans                                                    */
    /* --------------------------------------------------------------------- */

    public static class SimpleNode {
        public Long id;
        public String name;

        public SimpleNode(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class ArrayBean {
        public int[] numbers;

        public ArrayBean(int[] numbers) {
            this.numbers = numbers;
        }
    }

    public static class ListBean {
        public List<String> items;

        public ListBean(List<String> items) {
            this.items = items;
        }
    }

    public static class SetBean {
        public Set<String> values;

        public SetBean(Set<String> values) {
            this.values = values;
        }
    }

    public static class MapBean {
        public Map<String, Integer> map;

        public MapBean(Map<String, Integer> map) {
            this.map = map;
        }
    }

    /* --------------------------------------------------------------------- */
    /* ID implementation used in tests                                       */
    /* --------------------------------------------------------------------- */

    private GraphComparator.ID idFetcher = new GraphComparator.ID() {
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

    /* --------------------------------------------------------------------- */
    /* 1. Delta basic contract                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeltaEqualsAndHashCode() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "ptr", "src", "tgt", null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "ptr", "x", "y", null);
        GraphComparator.Delta d3 = new GraphComparator.Delta(3L, "field", "different", "a", "b", null);

        assertEquals(d1, d2);               // equality based on srcPtr
        assertNotEquals(d1, d3);
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    /* --------------------------------------------------------------------- */
    /* 2. Command enum – fromName validation                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCommandFromNameValid() {
        assertEquals(ARRAY_SET_ELEMENT, GraphComparator.Delta.Command.fromName("array.setelement"));
        assertEquals(SET_ADD, GraphComparator.Delta.Command.fromName("SET.ADD"));
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCommandFromNameInvalid() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Name is required");
        GraphComparator.Delta.Command.fromName("");
    }

    @Test
    public void testCommandFromNameUnknown() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown Command enum");
        GraphComparator.Delta.Command.fromName("unknown.command");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Simple object comparison – field change detection                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompareSimpleObjectFieldChange() {
        SimpleNode src = new SimpleNode(1L, "Alice");
        SimpleNode tgt = new SimpleNode(1L, "Bob");

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals("name", d.getFieldName());
        assertEquals("Alice", d.getSourceValue());
        assertEquals("Bob", d.getTargetValue());
    }

    @Test
    public void testCompareSimpleObjectDifferentId() {
        SimpleNode src = new SimpleNode(1L, "Alice");
        SimpleNode tgt = new SimpleNode(2L, "Alice");

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(1, deltas.size());
        assertEquals(OBJECT_ASSIGN_FIELD, deltas.get(0).getCmd());
    }

    /* --------------------------------------------------------------------- */
    /* 4. Orphan detection – source contains extra ID object                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testOrphanDetection() {
        SimpleNode srcOnly = new SimpleNode(3L, "Orphan");
        SimpleNode src = new SimpleNode(1L, "A");
        SimpleNode tgt = new SimpleNode(1L, "A");

        List<Object> sourceGraph = Arrays.asList(src, srcOnly);
        List<Object> targetGraph = Collections.singletonList(tgt);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(sourceGraph, targetGraph, idFetcher);
        boolean hasOrphan = deltas.stream().anyMatch(d -> d.getCmd() == OBJECT_ORPHAN);
        assertTrue(hasOrphan);
    }

    /* --------------------------------------------------------------------- */
    /* 5. Array comparison – resize and element set                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompareArrayResizeAndSetElement() {
        ArrayBean src = new ArrayBean(new int[]{1, 2});
        ArrayBean tgt = new ArrayBean(new int[]{1, 2, 3, 4});

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // expect one resize + two set element deltas
        assertEquals(3, deltas.size());

        boolean hasResize = deltas.stream().anyMatch(d -> d.getCmd() == ARRAY_RESIZE);
        assertTrue(hasResize);
        long resizeTarget = (Long) deltas.stream()
                .filter(d -> d.getCmd() == ARRAY_RESIZE)
                .findFirst().get()
                .getOptionalKey();
        assertEquals(4L, resizeTarget);
    }

    @Test
    public void testApplyDeltaArrayResizeAndSetElement() throws Exception {
        ArrayBean src = new ArrayBean(new int[]{1, 2});
        ArrayBean tgt = new ArrayBean(new int[]{1, 2, 3, 4});

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas,
                idFetcher, GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());
        assertArrayEquals(new int[]{1, 2, 3, 4}, src.numbers);
    }

    /* --------------------------------------------------------------------- */
    /* 6. List comparison – resize and element set                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompareListResizeAndSetElement() {
        ListBean src = new ListBean(new ArrayList<>(Arrays.asList("a", "b")));
        ListBean tgt = new ListBean(new ArrayList<>(Arrays.asList("a", "c", "d")));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // one resize + two set element deltas (index 1 change, index 2 addition)
        assertEquals(3, deltas.size());

        boolean hasResize = deltas.stream().anyMatch(d -> d.getCmd() == LIST_RESIZE);
        assertTrue(hasResize);
        assertEquals(3, deltas.stream()
                .filter(d -> d.getCmd() == LIST_RESIZE)
                .findFirst().get()
                .getOptionalKey());
    }

    @Test
    public void testApplyDeltaListResizeAndSetElement() {
        ListBean src = new ListBean(new LinkedList<>(Arrays.asList("a", "b")));
        ListBean tgt = new ListBean(new LinkedList<>(Arrays.asList("a", "c", "d")));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas,
                idFetcher, GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());
        assertEquals(Arrays.asList("a", "c", "d"), src.items);
    }

    /* --------------------------------------------------------------------- */
    /* 7. Set comparison – add and remove                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompareSetAddRemove() {
        SetBean src = new SetBean(new HashSet<>(Arrays.asList("x", "y")));
        SetBean tgt = new SetBean(new HashSet<>(Arrays.asList("y", "z")));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // expect one add (z) and one remove (x)
        assertEquals(2, deltas.size());

        boolean hasAdd = deltas.stream().anyMatch(d -> d.getCmd() == SET_ADD);
        boolean hasRemove = deltas.stream().anyMatch(d -> d.getCmd() == SET_REMOVE);
        assertTrue(hasAdd);
        assertTrue(hasRemove);
    }

    @Test
    public void testApplyDeltaSetAddRemove() {
        SetBean src = new SetBean(new LinkedHashSet<>(Arrays.asList("x", "y")));
        SetBean tgt = new SetBean(new LinkedHashSet<>(Arrays.asList("y", "z")));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas,
                idFetcher, GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());
        assertTrue(src.values.containsAll(Arrays.asList("y", "z")));
        assertFalse(src.values.contains("x"));
    }

    /* --------------------------------------------------------------------- */
    /* 8. Map comparison – put and remove                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompareMapPutRemove() {
        MapBean src = new MapBean(new HashMap<>());
        src.map.put("a", 1);
        src.map.put("b", 2);

        MapBean tgt = new MapBean(new HashMap<>());
        tgt.map.put("b", 2);          // unchanged
        tgt.map.put("c", 3);          // new entry
        // remove "a"

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // expect one put (c) and one remove (a)
        assertEquals(2, deltas.size());

        boolean hasPut = deltas.stream().anyMatch(d -> d.getCmd() == MAP_PUT);
        boolean hasRemove = deltas.stream().anyMatch(d -> d.getCmd() == MAP_REMOVE);
        assertTrue(hasPut);
        assertTrue(hasRemove);
    }

    @Test
    public void testApplyDeltaMapPutRemove() {
        MapBean src = new MapBean(new HashMap<>());
        src.map.put("a", 1);
        src.map.put("b", 2);

        MapBean tgt = new MapBean(new HashMap<>());
        tgt.map.put("b", 2);
        tgt.map.put("c", 3);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas,
                idFetcher, GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());
        assertFalse(src.map.containsKey("a"));
        assertEquals(Integer.valueOf(3), src.map.get("c"));
    }

    /* --------------------------------------------------------------------- */
    /* 9. Object assign field – direct field update via DeltaProcessor       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testApplyDeltaObjectAssignField() throws Exception {
        SimpleNode src = new SimpleNode(1L, "Old");
        SimpleNode tgt = new SimpleNode(1L, "New");

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas,
                idFetcher, GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());
        assertEquals("New", src.name);
    }

    /* --------------------------------------------------------------------- */
    /* 10. Error handling – missing field name (non‑orphan)                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testApplyDeltaMissingFieldError() {
        // Build a Delta that references a non‑existent field on SimpleNode
        GraphComparator.Delta badDelta = new GraphComparator.Delta(1L, "nonExistent", "ptr", "x", "y", null);
        badDelta.setCmd(OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                new SimpleNode(1L, "test"),
                Collections.singletonList(badDelta),
                idFetcher,
                GraphComparator.getJavaDeltaProcessor());

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("field name missing"));
    }

    /* --------------------------------------------------------------------- */
    /* 11. Error handling – invalid optionalKey for resize (negative)       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testApplyDeltaInvalidResizeKeyError() {
        // Create a resize delta with a negative optionalKey
        GraphComparator.Delta resizeDelta = new GraphComparator.Delta(1L, "numbers", "ptr", null, null, -5);
        resizeDelta.setCmd(ARRAY_RESIZE);

        ArrayBean src = new ArrayBean(new int[]{1, 2});

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                src,
                Collections.singletonList(resizeDelta),
                idFetcher,
                GraphComparator.getJavaDeltaProcessor());

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("optionalKey must be a integer value 0 or greater"));
    }

    /* --------------------------------------------------------------------- */
    /* 12. Verify that getJavaDeltaProcessor returns a non‑null processor   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetJavaDeltaProcessorNotNull() {
        assertNotNull(GraphComparator.getJavaDeltaProcessor());
    }
}
