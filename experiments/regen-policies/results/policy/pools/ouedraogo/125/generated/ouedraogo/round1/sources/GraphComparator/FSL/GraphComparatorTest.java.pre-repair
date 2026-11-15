package com.cedarsoftware.util;

import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;

import org.junit.Test;

/**
 * JUnit 4 tests that exercise the public and internal behaviour of {@link GraphComparator}.
 * The tests cover:
 * <ul>
 *   <li>Delta equality and hash‑code contract</li>
 *   <li>Command enum name lookup</li>
 *   <li>Array, Set, Map and List comparison logic</li>
 *   <li>Application of generated deltas via the built‑in {@code JavaDeltaProcessor}</li>
 *   <li>Error handling when a delta cannot be applied</li>
 *   <li>Validation of helper methods that enforce command arguments</li>
 * </ul>
 */
public class GraphComparatorTest {

    /** Simple POJO used as a graph node – all fields are public so that {@link ReflectionUtils}
        can access them without needing getters/setters. */
    public static class Node {
        public Long id;
        public String name;
        public int[] numbers;
        public Set<String> tags;
        public Map<String,Integer> scores;
        public List<String> notes;

        public Node(Long id) {
            this.id = id;
        }
    }

    /** ID fetcher that simply returns the {@code id} field of the object. */
    private static final GraphComparator.ID ID_FETCHER = new GraphComparator.ID() {
        @Override
        public Object getId(Object objectToId) {
            try {
                Field f = objectToId.getClass().getDeclaredField("id");
                f.setAccessible(true);
                return f.get(objectToId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    /* ---------------------------------------------------------------------- */
    /*  Delta equality / hashCode tests                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeltaEqualsAndHashCode() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "ptr", "src", "tgt", null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "ptr", "x", "y", null);
        // equality is based solely on srcPtr
        assertTrue(d1.equals(d2));
        assertEquals(d1.hashCode(), d2.hashCode());

        GraphComparator.Delta d3 = new GraphComparator.Delta(1L, "field", "differentPtr", "src", "tgt", null);
        assertFalse(d1.equals(d3));
    }

    /* ---------------------------------------------------------------------- */
    /*  Command enum lookup                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCommandFromNameValidAndInvalid() {
        assertEquals(ARRAY_SET_ELEMENT, GraphComparator.Delta.Command.fromName("array.setElement"));
        assertEquals(LIST_RESIZE, GraphComparator.Delta.Command.fromName("list.resize"));
        assertEquals(SET_ADD, GraphComparator.Delta.Command.fromName("SET.ADD")); // case‑insensitive

        try {
            GraphComparator.Delta.Command.fromName("non.existent");
            fail("Expected IllegalArgumentException for unknown command");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        try {
            GraphComparator.Delta.Command.fromName(null);
            fail("Expected IllegalArgumentException for null name");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Array comparison (resize + element set)                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompareArrayResizeAndSetElement() {
        Node src = new Node(1L);
        src.numbers = new int[]{1, 2};

        Node tgt = new Node(1L);
        tgt.numbers = new int[]{1, 2, 3};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect two deltas: one resize, one set element (index 2)
        assertEquals(2, deltas.size());

        GraphComparator.Delta resize = deltas.stream()
                .filter(d -> d.getCmd() == ARRAY_RESIZE)
                .findFirst()
                .orElse(null);
        assertNotNull(resize);
        assertEquals(3, resize.getOptionalKey()); // new length

        GraphComparator.Delta set = deltas.stream()
                .filter(d -> d.getCmd() == ARRAY_SET_ELEMENT)
                .findFirst()
                .orElse(null);
        assertNotNull(set);
        assertEquals(2, set.getOptionalKey()); // index
        assertEquals(3, set.getTargetValue());
    }

    /* ---------------------------------------------------------------------- */
    /*  Set comparison (add & remove)                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompareSetAddRemove() {
        Node src = new Node(2L);
        src.tags = new HashSet<>(Arrays.asList("a", "b"));

        Node tgt = new Node(2L);
        tgt.tags = new HashSet<>(Arrays.asList("b", "c"));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect one remove (a) and one add (c)
        assertEquals(2, deltas.size());

        boolean sawRemove = deltas.stream().anyMatch(d -> d.getCmd() == SET_REMOVE && "a".equals(d.getSourceValue()));
        boolean sawAdd = deltas.stream().anyMatch(d -> d.getCmd() == SET_ADD && "c".equals(d.getTargetValue()));
        assertTrue(sawRemove);
        assertTrue(sawAdd);
    }

    /* ---------------------------------------------------------------------- */
    /*  Map comparison (put & remove)                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompareMapPutRemove() {
        Node src = new Node(3L);
        src.scores = new HashMap<>();
        src.scores.put("x", 1);
        src.scores.put("y", 2);

        Node tgt = new Node(3L);
        tgt.scores = new HashMap<>();
        tgt.scores.put("x", 1);
        tgt.scores.put("z", 3);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect one remove (key y) and one put (key z, value 3)
        assertEquals(2, deltas.size());

        boolean sawRemove = deltas.stream()
                .anyMatch(d -> d.getCmd() == MAP_REMOVE && "y".equals(d.getOptionalKey()));
        boolean sawPut = deltas.stream()
                .anyMatch(d -> d.getCmd() == MAP_PUT && "z".equals(d.getOptionalKey()) && Integer.valueOf(3).equals(d.getTargetValue()));
        assertTrue(sawRemove);
        assertTrue(sawPut);
    }

    /* ---------------------------------------------------------------------- */
    /*  List comparison (resize + set elements)                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompareListResizeAndSetElements() {
        Node src = new Node(4L);
        src.notes = new ArrayList<>(Arrays.asList("a", "b"));

        Node tgt = new Node(4L);
        tgt.notes = new ArrayList<>(Arrays.asList("a", "c", "d"));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Expect resize + two set‑element deltas (index 2 new, index 1 changed)
        assertEquals(3, deltas.size());

        GraphComparator.Delta resize = deltas.stream()
                .filter(d -> d.getCmd() == LIST_RESIZE)
                .findFirst()
                .orElse(null);
        assertNotNull(resize);
        assertEquals(3, resize.getOptionalKey());

        long setCount = deltas.stream()
                .filter(d -> d.getCmd() == LIST_SET_ELEMENT)
                .count();
        assertEquals(2, setCount);
    }

    /* ---------------------------------------------------------------------- */
    /*  Full compare → apply cycle                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testApplyDeltaTransformsSourceToTarget() {
        // Build source graph
        Node src = new Node(5L);
        src.name = "old";
        src.numbers = new int[]{1, 2};
        src.tags = new HashSet<>(Arrays.asList("x"));
        src.scores = new HashMap<>();
        src.scores.put("k", 10);
        src.notes = new ArrayList<>(Arrays.asList("first"));

        // Build target graph with several differences
        Node tgt = new Node(5L);
        tgt.name = "new";                     // primitive change
        tgt.numbers = new int[]{1, 2, 3};     // array resize + element
        tgt.tags = new HashSet<>(Arrays.asList("x", "y")); // set add
        tgt.scores = new HashMap<>();
        tgt.scores.put("k", 20);              // map put (value change)
        tgt.scores.put("z", 30);              // map add
        tgt.notes = new ArrayList<>(Arrays.asList("first", "second")); // list resize + add

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, ID_FETCHER);
        // Apply the deltas
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas,
                ID_FETCHER, GraphComparator.getJavaDeltaProcessor());

        // No errors expected
        assertTrue(errors.isEmpty());

        // Verify that source now matches target
        assertEquals(tgt.name, src.name);
        assertArrayEquals(tgt.numbers, src.numbers);
        assertEquals(tgt.tags, src.tags);
        assertEquals(tgt.scores, src.scores);
        assertEquals(tgt.notes, src.notes);
    }

    /* ---------------------------------------------------------------------- */
    /*  Apply delta with a missing field – should produce a DeltaError          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testApplyDeltaMissingFieldProducesError() {
        Node src = new Node(6L);
        src.name = "alpha";

        Node tgt = new Node(6L);
        tgt.name = "beta";

        // Manually craft a delta that refers to a non‑existent field
        GraphComparator.Delta bogus = new GraphComparator.Delta(6L, "nonExistentField",
                "ptr", "alpha", "beta", null);
        bogus.setCmd(OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Collections.singletonList(bogus), ID_FETCHER, GraphComparator.getJavaDeltaProcessor());

        assertEquals(1, errors.size());
        GraphComparator.DeltaError err = errors.get(0);
        assertTrue(err.getError().contains("field name missing"));
        assertEquals(OBJECT_ASSIGN_FIELD, err.getCmd());
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper validation – illegal optionalKey for resize commands            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHelperGetResizeValueRejectsNegativeKey() {
        // Object with an int[] field
        class Wrapper {
            public int[] data = new int[2];
        }
        Wrapper src = new Wrapper();

        // Delta with a negative optionalKey – this should cause IllegalArgumentException
        GraphComparator.Delta delta = new GraphComparator.Delta(1L, "data",
                "ptr", src.data, src.data, -1);
        delta.setCmd(ARRAY_RESIZE);

        try {
            GraphComparator.getJavaDeltaProcessor().processArrayResize(src, Wrapper.class.getDeclaredField("data"), delta);
            fail("Expected IllegalArgumentException due to negative optionalKey");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("optionalKey must be a integer value 0 or greater"));
        } catch (Exception e) {
            fail("Unexpected exception type: " + e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper validation – out‑of‑bounds array set element                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testArraySetElementOutOfBounds() throws NoSuchFieldException {
        class Wrapper {
            public int[] data = new int[]{0, 1};
        }
        Wrapper src = new Wrapper();

        GraphComparator.Delta delta = new GraphComparator.Delta(1L, "data",
                "ptr", src.data, 99, 5); // index 5 (out of bounds)
        delta.setCmd(ARRAY_SET_ELEMENT);

        try {
            GraphComparator.getJavaDeltaProcessor().processArraySetElement(src,
                    Wrapper.class.getDeclaredField("data"), delta);
            fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expected) {
            assertTrue(expected.getMessage().contains("index out of bounds"));
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Verify that isLogicalPrimitive works for a variety of types via reflection */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsLogicalPrimitiveViaReflection() throws Exception {
        // Access the private method
        java.lang.reflect.Method m = GraphComparator.class.getDeclaredMethod("isLogicalPrimitive", Class.class);
        m.setAccessible(true);

        assertTrue((Boolean) m.invoke(null, int.class));
        assertTrue((Boolean) m.invoke(null, Integer.class));
        assertTrue((Boolean) m.invoke(null, String.class));
        assertTrue((Boolean) m.invoke(null, Date.class));
        assertTrue((Boolean) m.invoke(null, Calendar.class));
        assertTrue((Boolean) m.invoke(null, Boolean.class));
        assertTrue((Boolean) m.invoke(null, Character.class));
        assertTrue((Boolean) m.invoke(null, TimeZone.class));

        assertFalse((Boolean) m.invoke(null, Object.class));
        assertFalse((Boolean) m.invoke(null, List.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  Verify that isIdObject correctly recognises objects with an ID          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsIdObjectRecognition() throws Exception {
        java.lang.reflect.Method m = GraphComparator.class.getDeclaredMethod("isIdObject", Object.class, GraphComparator.ID.class);
        m.setAccessible(true);

        Node n = new Node(10L);
        assertTrue((Boolean) m.invoke(null, n, ID_FETCHER));

        // Primitive wrapper – should be false
        Integer i = 5;
        assertFalse((Boolean) m.invoke(null, i, ID_FETCHER));

        // Null – false
        assertFalse((Boolean) m.invoke(null, null, ID_FETCHER));
    }
}
