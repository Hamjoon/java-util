package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.lang.reflect.Field;
import java.util.*;

import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;
import static org.junit.Assert.*;

public class GraphComparatorTest {

    // Simple POJO with an id field
    public static class Node {
        public Long id;
        public String name;
        public int[] numbers;
        public List<String> tags;
        public Set<Node> friends;
        public Map<String, Node> map;

        public Node(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private GraphComparator.ID idFetcher;

    @Before
    public void setUp() {
        // ID fetcher based on the 'id' field
        idFetcher = new GraphComparator.ID() {
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
    }

    @Test
    public void testCompare_PrimitiveFieldChange() {
        Node src = new Node(1L, "Alice");
        Node tgt = new Node(1L, "Bob"); // name changed

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals("name", d.getFieldName());
        assertEquals("Alice", d.getSourceValue());
        assertEquals("Bob", d.getTargetValue());
    }

    @Test
    public void testCompare_ArrayResizeAndSetElement() {
        Node src = new Node(2L, "Node");
        src.numbers = new int[]{1, 2, 3};

        Node tgt = new Node(2L, "Node");
        tgt.numbers = new int[]{1, 9, 3, 4}; // change index1, add new element

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Expect a resize + one set element
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == ARRAY_RESIZE));
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == ARRAY_SET_ELEMENT && d.getOptionalKey().equals(1)));
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == ARRAY_SET_ELEMENT && d.getOptionalKey().equals(3)));
        assertEquals(3, deltas.size()); // resize + 2 set element
    }

    @Test
    public void testCompare_SetAddRemove() {
        Node src = new Node(3L, "Src");
        Node friendA = new Node(10L, "A");
        Node friendB = new Node(11L, "B");
        src.friends = new HashSet<>(Arrays.asList(friendA, friendB));

        Node tgt = new Node(3L, "Src");
        Node friendC = new Node(12L, "C");
        // remove B, add C
        tgt.friends = new HashSet<>(Arrays.asList(friendA, friendC));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(2, deltas.size());

        boolean hasAdd = deltas.stream().anyMatch(d -> d.getCmd() == SET_ADD && d.getTargetValue() == friendC);
        boolean hasRemove = deltas.stream().anyMatch(d -> d.getCmd() == SET_REMOVE && d.getSourceValue() == friendB);
        assertTrue(hasAdd);
        assertTrue(hasRemove);
    }

    @Test
    public void testCompare_MapPutRemove() {
        Node src = new Node(4L, "Src");
        src.map = new HashMap<>();
        src.map.put("key1", new Node(20L, "V1"));
        src.map.put("key2", new Node(21L, "V2"));

        Node tgt = new Node(4L, "Src");
        tgt.map = new HashMap<>();
        tgt.map.put("key1", new Node(20L, "V1")); // unchanged
        tgt.map.put("key3", new Node(22L, "V3")); // new entry
        // key2 removed

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(2, deltas.size());

        boolean hasPut = deltas.stream().anyMatch(d -> d.getCmd() == MAP_PUT && "key3".equals(d.getOptionalKey()));
        boolean hasRemove = deltas.stream().anyMatch(d -> d.getCmd() == MAP_REMOVE && "key2".equals(d.getOptionalKey()));
        assertTrue(hasPut);
        assertTrue(hasRemove);
    }

    @Test
    public void testCompare_ListResizeAndSetElement() {
        Node src = new Node(5L, "Src");
        src.tags = new ArrayList<>(Arrays.asList("a", "b"));
        Node tgt = new Node(5L, "Src");
        tgt.tags = new ArrayList<>(Arrays.asList("a", "c", "d")); // change index1, add index2

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Expect resize + two set element deltas
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == LIST_RESIZE));
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == LIST_SET_ELEMENT && d.getOptionalKey().equals(1)));
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == LIST_SET_ELEMENT && d.getOptionalKey().equals(2)));
        assertEquals(3, deltas.size());
    }

    @Test
    public void testOrphanDetection() {
        // src has an extra node not present in target
        Node src = new Node(6L, "Src");
        Node extra = new Node(30L, "Extra");
        src.friends = new HashSet<>(Collections.singleton(extra));

        Node tgt = new Node(6L, "Src");
        tgt.friends = new HashSet<>();

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Should contain one OBJECT_ORPHAN delta for id 30L
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == OBJECT_ORPHAN && d.getId().equals(30L)));
    }

    @Test
    public void testApplyDelta_ArraySetElement() throws NoSuchFieldException {
        Node src = new Node(7L, "Src");
        src.numbers = new int[]{5, 6, 7};

        // create delta to set index 1 to 99
        GraphComparator.Delta delta = new GraphComparator.Delta(7L, "numbers", "dummy", null, null, 1);
        delta.setCmd(ARRAY_SET_ELEMENT);
        delta.setTargetValue(99);

        Field field = Node.class.getField("numbers");
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();

        processor.processArraySetElement(src, field, delta);
        assertArrayEquals(new int[]{5, 99, 7}, src.numbers);
    }

    @Test
    public void testApplyDelta_ListResize() throws NoSuchFieldException {
        Node src = new Node(8L, "Src");
        src.tags = new ArrayList<>(Arrays.asList("x", "y"));

        GraphComparator.Delta delta = new GraphComparator.Delta(8L, "tags", "dummy", null, null, 4);
        delta.setCmd(LIST_RESIZE);
        delta.setOptionalKey(4);

        Field field = Node.class.getField("tags");
        GraphComparator.DeltaProcessor processor = GraphComparator.getJavaDeltaProcessor();

        processor.processListResize(src, field, delta);
        assertEquals(4, src.tags.size());
        assertNull(src.tags.get(2));
        assertNull(src.tags.get(3));
    }

    @Test
    public void testCommandFromName_ValidAndInvalid() {
        assertEquals(ARRAY_SET_ELEMENT, GraphComparator.Delta.Command.fromName("array.setelement"));
        assertEquals(OBJECT_ORPHAN, GraphComparator.Delta.Command.fromName("OBJECT.ORPHAN"));
        try {
            GraphComparator.Delta.Command.fromName("nonexistent");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unknown Command"));
        }
    }

    @Test
    public void testDeltaEqualsAndHashCode() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "f", "ptr", null, null, null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "g", "ptr", null, null, null);
        GraphComparator.Delta d3 = new GraphComparator.Delta(3L, "h", "other", null, null, null);

        assertTrue(d1.equals(d2));
        assertEquals(d1.hashCode(), d2.hashCode());
        assertFalse(d1.equals(d3));
    }

    @Test
    public void testApplyDelta_WithProcessorAndErrors() {
        Node src = new Node(9L, "Src");
        src.tags = new ArrayList<>(Arrays.asList("a"));
        // delta tries to set index 5 (out of bounds) -> should produce error
        GraphComparator.Delta delta = new GraphComparator.Delta(9L, "tags", "ptr", null, null, 5);
        delta.setCmd(LIST_SET_ELEMENT);
        delta.setTargetValue("z");

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Collections.singletonList(delta), idFetcher, GraphComparator.getJavaDeltaProcessor());

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("index out of bounds"));
    }
}
