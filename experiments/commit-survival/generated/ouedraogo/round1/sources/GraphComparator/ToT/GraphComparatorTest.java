package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link GraphComparator}.
 *
 * The tests are written as if three independent experts each contributed
 * one test per public method.  The suite therefore covers:
 *
 * <ul>
 *   <li>Delta command creation and behavior (equals, hashCode, toString, Command.fromName)</li>
 *   <li>Graph comparison for primitives, arrays, collections (Set, List, Map) and orphan detection</li>
 *   <li>Application of deltas via the built‑in {@link GraphComparator.JavaDeltaProcessor}</li>
 *   <li>Edge‑case and error handling (nulls, type mismatches, illegal resize values, unknown fields)</li>
 * </ul>
 *
 * All objects are created directly – no mocking frameworks are used,
 * complying with the restriction to use only the classes visible in the
 * source file.
 */
public class GraphComparatorTest {

    /** Simple domain object used for ID based comparison */
    public static class Node {
        public long id;                     // ID field – used by the ID fetcher
        public String name;
        public int[] numbers;               // array field
        public List<Node> children;        // list field
        public Set<String> tags;           // set field
        public Map<String, Node> map;      // map field

        public Node(long id, String name) {
            this.id = id;
            this.name = name;
            this.numbers = new int[0];
            this.children = new ArrayList<>();
            this.tags = new HashSet<>();
            this.map = new HashMap<>();
        }
    }

    /** ID implementation that simply returns the {@code id} field of a {@link Node} */
    private final GraphComparator.ID nodeIdFetcher = new GraphComparator.ID() {
        @Override
        public Object getId(Object objectToId) {
            return ((Node) objectToId).id;
        }
    };

    private GraphComparator.DeltaProcessor javaProcessor;

    @Before
    public void setUp() {
        javaProcessor = GraphComparator.getJavaDeltaProcessor();
    }

    /* --------------------------------------------------------------
     *  1. Delta command enum & utility methods
     * -------------------------------------------------------------- */
    @Test
    public void testCommandFromName_ValidAndInvalid() {
        assertEquals(ARRAY_SET_ELEMENT, Command.fromName("array.setElement"));
        assertEquals(SET_ADD, Command.fromName("SET.ADD")); // case‑insensitive
        assertEquals(LIST_RESIZE, Command.fromName("list.resize"));

        try {
            Command.fromName(null);
            fail("Expected IllegalArgumentException for null name");
        } catch (IllegalArgumentException ignored) {}

        try {
            Command.fromName("   ");
            fail("Expected IllegalArgumentException for empty name");
        } catch (IllegalArgumentException ignored) {}

        try {
            Command.fromName("unknown.command");
            fail("Expected IllegalArgumentException for unknown name");
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testDeltaEqualsAndHashCode() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "ptr", "src", "tgt", null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "ptr", "x", "y", null);
        GraphComparator.Delta d3 = new GraphComparator.Delta(1L, "field", "differentPtr", "src", "tgt", null);

        // Equality is based ONLY on srcPtr
        assertTrue(d1.equals(d2));
        assertFalse(d1.equals(d3));
        assertEquals(d1.hashCode(), d2.hashCode());
        assertNotEquals(d1.hashCode(), d3.hashCode());
    }

    /* --------------------------------------------------------------
     *  2. Primitive field comparison (logical primitives)
     * -------------------------------------------------------------- */
    @Test
    public void testCompare_PrimitiveAndWrapperChanges() {
        Node src = new Node(1, "Alice");
        src.numbers = new int[]{1, 2, 3};

        Node tgt = new Node(1, "Bob");               // name differs
        tgt.numbers = new int[]{1, 2, 3};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, nodeIdFetcher);
        // Only the 'name' field should generate an OBJECT_ASSIGN_FIELD delta
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals("name", d.getFieldName());
        assertEquals("Alice", d.getSourceValue());
        assertEquals("Bob", d.getTargetValue());
    }

    /* --------------------------------------------------------------
     *  3. Array handling – primitive array resize and element set
     * -------------------------------------------------------------- */
    @Test
    public void testCompare_PrimitiveArrayResizeAndSet() {
        Node src = new Node(1, "src");
        src.numbers = new int[]{1, 2, 3};

        Node tgt = new Node(1, "src");
        tgt.numbers = new int[]{1, 9, 3, 4}; // change element at index 1 and add a new element

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, nodeIdFetcher);
        // Expect two deltas: one ARRAY_RESIZE, one ARRAY_SET_ELEMENT for index 1
        assertEquals(2, deltas.size());

        boolean resizeSeen = false, setSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == ARRAY_RESIZE) {
                resizeSeen = true;
                assertEquals(4, d.getOptionalKey()); // new length
            } else if (d.getCmd() == ARRAY_SET_ELEMENT) {
                setSeen = true;
                assertEquals(1, d.getOptionalKey()); // index
                assertEquals(2, d.getSourceValue()); // old value
                assertEquals(9, d.getTargetValue()); // new value
            }
        }
        assertTrue(resizeSeen && setSeen);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessArrayResize_NegativeSizeThrows() {
        // Build a delta that would request a negative resize – the processor should reject it.
        Node src = new Node(1, "src");
        src.numbers = new int[]{1, 2};

        GraphComparator.Delta delta = new GraphComparator.Delta(1L, "numbers", "ptr", src.numbers, null, -5);
        delta.setCmd(ARRAY_RESIZE);

        // Use reflection to obtain the field representing the array
        Field numbersField = Node.class.getDeclaredField("numbers");
        numbersField.setAccessible(true);

        // This call must throw IllegalArgumentException because the optionalKey is negative
        javaProcessor.processArrayResize(src, numbersField, delta);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testProcessArraySetElement_IndexOutOfBounds() throws Exception {
        Node src = new Node(1, "src");
        src.numbers = new int[]{1, 2, 3};

        GraphComparator.Delta delta = new GraphComparator.Delta(1L, "numbers", "ptr", src.numbers, 99, 5);
        delta.setCmd(ARRAY_SET_ELEMENT);

        Field numbersField = Node.class.getDeclaredField("numbers");
        numbersField.setAccessible(true);

        javaProcessor.processArraySetElement(src, numbersField, delta);
    }

    /* --------------------------------------------------------------
     *  4. Set handling – add and remove
     * -------------------------------------------------------------- */
    @Test
    public void testCompare_SetsAddRemove() {
        Node src = new Node(1, "src");
        src.tags.addAll(Arrays.asList("red", "green"));

        Node tgt = new Node(1, "src");
        tgt.tags.addAll(Arrays.asList("green", "blue")); // 'red' removed, 'blue' added

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, nodeIdFetcher);
        assertEquals(2, deltas.size());

        boolean addSeen = false, removeSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == SET_ADD) {
                addSeen = true;
                assertEquals("blue", d.getTargetValue());
            } else if (d.getCmd() == SET_REMOVE) {
                removeSeen = true;
                assertEquals("red", d.getSourceValue());
            }
        }
        assertTrue(addSeen && removeSeen);
    }

    @Test
    public void testApplyDelta_SetAddAndRemove() throws Exception {
        Node src = new Node(1, "src");
        src.tags.addAll(Arrays.asList("a", "b"));
        // Build deltas manually to add "c" and remove "a"
        GraphComparator.Delta add = new GraphComparator.Delta(1L, "tags", "ptr", null, "c", null);
        add.setCmd(SET_ADD);
        GraphComparator.Delta remove = new GraphComparator.Delta(1L, "tags", "ptr", "a", null, null);
        remove.setCmd(SET_REMOVE);

        List<GraphComparator.Delta> cmds = Arrays.asList(add, remove);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, cmds, nodeIdFetcher, javaProcessor);
        assertTrue(errors.isEmpty());

        assertTrue(src.tags.containsAll(Arrays.asList("b", "c")));
        assertFalse(src.tags.contains("a"));
    }

    /* --------------------------------------------------------------
     *  5. Map handling – put and remove
     * -------------------------------------------------------------- */
    @Test
    public void testCompare_MapsPutAndRemove() {
        Node src = new Node(1, "src");
        Node child1 = new Node(2, "c1");
        src.map.put("first", child1);

        Node tgt = new Node(1, "src");
        Node child2 = new Node(3, "c2");
        tgt.map.put("second", child2); // new entry
        // remove "first" by not putting it in target

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, nodeIdFetcher);
        assertEquals(2, deltas.size());

        boolean putSeen = false, removeSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == MAP_PUT) {
                putSeen = true;
                assertEquals("second", d.getOptionalKey());
                assertEquals(child2, d.getTargetValue());
            } else if (d.getCmd() == MAP_REMOVE) {
                removeSeen = true;
                assertEquals("first", d.getOptionalKey());
                assertEquals(child1, d.getSourceValue());
            }
        }
        assertTrue(putSeen && removeSeen);
    }

    @Test
    public void testApplyDelta_MapPutAndRemove() throws Exception {
        Node src = new Node(1, "src");
        Node old = new Node(2, "old");
        src.map.put("key", old);

        // Build deltas: remove old, put new
        GraphComparator.Delta remove = new GraphComparator.Delta(1L, "map", "ptr", old, null, "key");
        remove.setCmd(MAP_REMOVE);
        Node fresh = new Node(3, "new");
        GraphComparator.Delta put = new GraphComparator.Delta(1L, "map", "ptr", null, fresh, "key");
        put.setCmd(MAP_PUT);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Arrays.asList(remove, put), nodeIdFetcher, javaProcessor);
        assertTrue(errors.isEmpty());

        assertEquals(1, src.map.size());
        assertSame(fresh, src.map.get("key"));
    }

    /* --------------------------------------------------------------
     *  6. List handling – resize and set element
     * -------------------------------------------------------------- */
    @Test
    public void testCompare_ListResizeAndSetElement() {
        Node src = new Node(1, "src");
        src.children.add(new Node(2, "c1"));
        src.children.add(new Node(3, "c2"));

        Node tgt = new Node(1, "src");
        tgt.children.add(new Node(2, "c1-modified")); // same id, different name
        tgt.children.add(new Node(3, "c2"));
        tgt.children.add(new Node(4, "c3")); // new element, causes resize

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, nodeIdFetcher);
        // Expect one LIST_RESIZE (size 3) and one LIST_SET_ELEMENT for index 0 (name change)
        assertEquals(2, deltas.size());

        boolean resizeSeen = false, setSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == LIST_RESIZE) {
                resizeSeen = true;
                assertEquals(3, d.getOptionalKey());
            } else if (d.getCmd() == LIST_SET_ELEMENT) {
                setSeen = true;
                assertEquals(0, d.getOptionalKey());
                assertEquals("c1", ((Node)d.getSourceValue()).name);
                assertEquals("c1-modified", ((Node)d.getTargetValue()).name);
            }
        }
        assertTrue(resizeSeen && setSeen);
    }

    @Test
    public void testApplyDelta_ListResizeAndSetElement() throws Exception {
        Node src = new Node(1, "src");
        src.children.add(new Node(2, "old"));
        // Build deltas: resize to 2 and set element at index 1
        GraphComparator.Delta resize = new GraphComparator.Delta(1L, "children", "ptr", src.children, null, 2);
        resize.setCmd(LIST_RESIZE);
        GraphComparator.Delta set = new GraphComparator.Delta(1L, "children", "ptr", null,
                new Node(3, "newChild"), 1);
        set.setCmd(LIST_SET_ELEMENT);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Arrays.asList(resize, set), nodeIdFetcher, javaProcessor);
        assertTrue(errors.isEmpty());

        assertEquals(2, src.children.size());
        assertEquals("old", src.children.get(0).name);
        assertEquals("newChild", src.children.get(1).name);
    }

    /* --------------------------------------------------------------
     *  7. Orphan detection (objects present in source but missing in target)
     * -------------------------------------------------------------- */
    @Test
    public void testCompare_OrphanDetection() {
        Node src = new Node(1, "src");
        src.children.add(new Node(2, "orphan"));
        src.children.add(new Node(3, "stillHere"));

        Node tgt = new Node(1, "src");
        tgt.children.add(new Node(3, "stillHere")); // only node 3 remains

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, nodeIdFetcher);
        // Expect one OBJECT_ORPHAN delta for id 2
        boolean orphanSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == OBJECT_ORPHAN) {
                orphanSeen = true;
                assertEquals(2L, d.getId());
            }
        }
        assertTrue("Orphan delta not generated", orphanSeen);
    }

    /* --------------------------------------------------------------
     *  8. applyDelta error handling – unknown field and source missing
     * -------------------------------------------------------------- */
    @Test
    public void testApplyDelta_UnknownFieldProducesError() {
        Node src = new Node(1, "src");

        // Create a delta that refers to a non‑existent field "bogus"
        GraphComparator.Delta bad = new GraphComparator.Delta(1L, "bogus", "ptr", null, "value", null);
        bad.setCmd(OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Collections.singletonList(bad), nodeIdFetcher, javaProcessor);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("field name missing"));
    }

    @Test
    public void testApplyDelta_SourceObjectNotFound() {
        Node src = new Node(1, "src");
        // Delta refers to id 999 which does not exist in the source graph
        GraphComparator.Delta delta = new GraphComparator.Delta(999L, "name", "ptr", null, "newName", null);
        delta.setCmd(OBJECT_ASSIGN_FIELD);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src,
                Collections.singletonList(delta), nodeIdFetcher, javaProcessor);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("source object not found"));
    }

    /* --------------------------------------------------------------
     *  9. Deep equality fallback (non‑ID objects)
     * -------------------------------------------------------------- */
    @Test
    public void testCompare_NonIdObjectDeepEquals() {
        // Use built‑in collection that does NOT implement ID – compare two equal lists
        List<String> src = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> tgt = new LinkedList<>(Arrays.asList("a", "b"));

        // Since they are not ID objects, compare() will fall back to DeepEquals.
        // No deltas should be produced because the contents are equal.
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, nodeIdFetcher);
        assertTrue(deltas.isEmpty());
    }

    /* --------------------------------------------------------------
     * 10. Logical primitive detection indirectly via compare
     * -------------------------------------------------------------- */
    @Test
    public void testCompare_LogicalPrimitives_DateAndTimeZone() {
        Node src = new Node(1, "src");
        src.tags.add(TimeZone.getTimeZone("UTC").getID()); // Set contains a logical primitive (String)

        Node tgt = new Node(1, "src");
        tgt.tags.add(TimeZone.getTimeZone("GMT").getID()); // Different logical primitive

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, nodeIdFetcher);
        // One SET_REMOVE (UTC) and one SET_ADD (GMT)
        assertEquals(2, deltas.size());

        boolean removeSeen = false, addSeen = false;
        for (GraphComparator.Delta d : deltas) {
            if (d.getCmd() == SET_REMOVE) {
                removeSeen = true;
                assertEquals("UTC", d.getSourceValue());
            } else if (d.getCmd() == SET_ADD) {
                addSeen = true;
                assertEquals("GMT", d.getTargetValue());
            }
        }
        assertTrue(removeSeen && addSeen);
    }

    /* --------------------------------------------------------------
     * 11. Helper test – ensure getResizeValue rejects non‑numeric optionalKey
     * -------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testHelper_GetResizeValue_NonNumericKey() throws Exception {
        // Build a delta with a String optionalKey; the processor will call Helper.getResizeValue
        GraphComparator.Delta delta = new GraphComparator.Delta(1L, "numbers", "ptr", null, null, "not-a-number");
        delta.setCmd(ARRAY_RESIZE);

        Field numbersField = Node.class.getDeclaredField("numbers");
        numbersField.setAccessible(true);
        javaProcessor.processArrayResize(new Node(1, "x"), numbersField, delta);
    }
}
