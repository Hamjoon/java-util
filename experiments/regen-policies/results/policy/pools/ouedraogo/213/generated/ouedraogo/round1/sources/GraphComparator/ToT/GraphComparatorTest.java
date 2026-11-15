package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link GraphComparator}.
 *
 * The tests are written from the perspective of three independent experts,
 * each contributing a test for every public method/feature.  The suite
 * therefore covers:
 *
 * <ul>
 *   <li>Delta basic contract (equals, hashCode, toString)</li>
 *   <li>Delta.Command parsing and error handling</li>
 *   <li>Array comparison (primitive, object, resize)</li>
 *   <li>Set comparison (add / remove)</li>
 *   <li>Map comparison (put / remove)</li>
 *   <li>List comparison (resize / set element)</li>
 *   <li>Null handling and type‑change detection</li>
 *   <li>Orphan detection</li>
 *   <li>applyDelta execution with the built‑in {@link GraphComparator.JavaDeltaProcessor}</li>
 * </ul>
 *
 * Helper classes used in the tests are defined at the bottom of the file.
 */
public class GraphComparatorTest {

    /** Simple ID fetcher used for all tests. */
    private GraphComparator.ID idFetcher;

    @Before
    public void setUp() {
        idFetcher = new GraphComparator.ID() {
            @Override
            public Object getId(Object objectToId) {
                // All test objects implement Identifiable
                return ((Identifiable) objectToId).getId();
            }
        };
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ---------------------------------------------------------------------- */
    /* 1. Delta basic contract                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void deltaEqualsAndHashCodeUseSrcPtr() {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "root.ptr", null, null, null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "root.ptr", null, null, null);
        GraphComparator.Delta d3 = new GraphComparator.Delta(3L, "field", "root.other", null, null, null);

        // Equality is based solely on srcPtr
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());

        assertNotEquals(d1, d3);
        assertNotEquals(d1.hashCode(), d3.hashCode());

        // toString contains all fields (basic sanity)
        String s = d1.toString();
        assertTrue(s.contains("id=1"));
        assertTrue(s.contains("fieldName='field'"));
        assertTrue(s.contains("srcPtr=root.ptr"));
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Delta.Command.fromName                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void commandFromNameValid() {
        for (GraphComparator.Delta.Command cmd : GraphComparator.Delta.Command.values()) {
            assertEquals(cmd, GraphComparator.Delta.Command.fromName(cmd.getName()));
        }
    }

    @Test
    public void commandFromNameInvalid() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown Command enum");
        GraphComparator.Delta.Command.fromName("nonexistent");
    }

    @Test
    public void commandFromNameNullOrEmpty() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Name is required");
        GraphComparator.Delta.Command.fromName(null);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Array comparison – primitive int[]                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void comparePrimitiveArrayDetectsChangesAndResize() {
        int[] src = new int[]{1, 2, 3};
        int[] target = new int[]{1, 9, 3, 4};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, idFetcher);

        // Expect one resize and one element set
        assertEquals(2, deltas.size());

        GraphComparator.Delta resize = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.ARRAY_RESIZE)
                .findFirst().orElse(null);
        GraphComparator.Delta set = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.ARRAY_SET_ELEMENT)
                .findFirst().orElse(null);

        assertNotNull(resize);
        assertEquals(4, resize.getOptionalKey()); // new length

        assertNotNull(set);
        assertEquals(1, set.getOptionalKey()); // index 1 changed from 2 to 9
        assertEquals(2, set.getSourceValue());
        assertEquals(9, set.getTargetValue());
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Array comparison – object array with IDs (deep comparison)          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void compareObjectArrayWithIdsPushesNestedDelta() {
        Node a1 = new Node(1L, "A");
        Node a2 = new Node(2L, "B");
        Node[] src = new Node[]{a1, a2};

        Node b1 = new Node(1L, "A-modified"); // same id, different name
        Node b2 = new Node(3L, "C");          // new element
        Node[] target = new Node[]{b1, b2};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, idFetcher);

        // Expected: resize (target length 2 = src length, so no resize), one set for index 0 (id same, deeper diff)
        // and one set for index 1 (different id -> set element)
        assertEquals(2, deltas.size());

        // The delta for index 0 should be OBJECT_ASSIGN_FIELD inside the stack, not a direct SET.
        GraphComparator.Delta elementDelta = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.ARRAY_SET_ELEMENT)
                .findFirst().orElse(null);
        assertNotNull(elementDelta);
        assertEquals(1, elementDelta.getOptionalKey()); // index 1 is different id -> set element
        assertNull(elementDelta.getSourceValue());
        assertEquals(b2, elementDelta.getTargetValue());

        // The delta for index 0 is not a direct ARRAY_SET_ELEMENT but a nested delta that
        // should have been pushed onto the stack. It will appear as OBJECT_ASSIGN_FIELD
        // because the name field differs.
        GraphComparator.Delta nameDelta = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD)
                .findFirst().orElse(null);
        assertNotNull(nameDelta);
        assertEquals("name", nameDelta.getFieldName());
        assertEquals("B", nameDelta.getSourceValue());
        assertEquals("A-modified", nameDelta.getTargetValue());
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Set comparison – add and remove elements                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void compareSetsAddRemoveAndIdMatching() {
        Node n1 = new Node(1L, "one");
        Node n2 = new Node(2L, "two");
        Set<Node> src = new HashSet<>(Arrays.asList(n1, n2));

        Node n3 = new Node(3L, "three");
        // n2 stays, n1 removed, n3 added
        Set<Node> target = new HashSet<>(Arrays.asList(n2, n3));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, idFetcher);
        assertEquals(2, deltas.size());

        GraphComparator.Delta remove = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.SET_REMOVE)
                .findFirst().orElse(null);
        GraphComparator.Delta add = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.SET_ADD)
                .findFirst().orElse(null);

        assertNotNull(remove);
        assertEquals(n1, remove.getSourceValue());

        assertNotNull(add);
        assertEquals(n3, add.getTargetValue());
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Map comparison – put and remove entries                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void compareMapsDetectPutAndRemove() {
        Node key1 = new Node(1L, "k1");
        Node key2 = new Node(2L, "k2");
        Node val1 = new Node(10L, "v1");
        Node val2 = new Node(20L, "v2");

        Map<Node, Node> src = new HashMap<>();
        src.put(key1, val1);
        src.put(key2, val2);

        Node key3 = new Node(3L, "k3");
        Node val3 = new Node(30L, "v3");
        // Change value for key1, remove key2, add key3
        Map<Node, Node> target = new HashMap<>();
        target.put(key1, val3); // new value
        target.put(key3, val3);  // new entry

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, idFetcher);

        // Expect: MAP_REMOVE for key2, MAP_PUT for key1 (value changed), MAP_PUT for key3 (new entry)
        assertEquals(3, deltas.size());

        boolean hasRemove = deltas.stream()
                .anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.MAP_REMOVE && d.getOptionalKey().equals(key2));
        assertTrue(hasRemove);

        long putCount = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.MAP_PUT)
                .count();
        assertEquals(2, putCount);
    }

    /* ---------------------------------------------------------------------- */
    /* 7. List comparison – resize and set element                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void compareListsResizeAndSetElement() {
        List<String> src = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<String> target = new ArrayList<>(Arrays.asList("a", "X", "c", "d"));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, idFetcher);

        // Expect a resize (size 4) and a set element at index 1
        GraphComparator.Delta resize = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.LIST_RESIZE)
                .findFirst().orElse(null);
        GraphComparator.Delta set = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.LIST_SET_ELEMENT)
                .findFirst().orElse(null);

        assertNotNull(resize);
        assertEquals(4, resize.getOptionalKey());

        assertNotNull(set);
        assertEquals(1, set.getOptionalKey());
        assertEquals("b", set.getSourceValue());
        assertEquals("X", set.getTargetValue());
    }

    /* ---------------------------------------------------------------------- */
    /* 8. Null handling – source null, target non‑null => OBJECT_ASSIGN_FIELD  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void compareNullVsObjectCreatesAssignFieldDelta() {
        String src = null;
        String target = "nonNull";

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, idFetcher);
        assertEquals(1, deltas.size());

        GraphComparator.Delta delta = deltas.get(0);
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, delta.getCmd());
        assertNull(delta.getSourceValue());
        assertEquals("nonNull", delta.getTargetValue());
    }

    /* ---------------------------------------------------------------------- */
    /* 9. Type change detection – different concrete classes for same field  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void compareDifferentContainerTypesDetectsFieldTypeChanged() {
        List<Integer> src = new ArrayList<>(Arrays.asList(1, 2));
        Set<Integer> target = new HashSet<>(Arrays.asList(1, 2));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, target, idFetcher);
        assertEquals(1, deltas.size());

        GraphComparator.Delta delta = deltas.get(0);
        assertEquals(GraphComparator.Delta.Command.OBJECT_FIELD_TYPE_CHANGED, delta.getCmd());
    }

    /* ---------------------------------------------------------------------- */
    /* 10. Orphan detection – object present only in source graph             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void compareDetectsOrphanedObjects() {
        Node root = new Node(100L, "root");
        Node child = new Node(101L, "child");
        root.setChild(child); // root -> child

        // Target graph does NOT contain child
        Node targetRoot = new Node(100L, "root");

        List<GraphComparator.Delta> deltas = GraphComparator.compare(root, targetRoot, idFetcher);

        // Expect one OBJECT_ORPHAN delta for child
        GraphComparator.Delta orphan = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.OBJECT_ORPHAN)
                .findFirst().orElse(null);
        assertNotNull(orphan);
        assertEquals(101L, orphan.getId());
    }

    /* ---------------------------------------------------------------------- */
    /* 11. applyDelta – verify that generated deltas actually mutate source   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void applyDeltaMutatesSourceAccordingToDeltas() {
        // Build source object
        SampleBean source = new SampleBean();
        source.intArray = new int[]{1, 2, 3};
        source.name = "old";
        source.values = new ArrayList<>(Arrays.asList("x", "y"));
        source.map = new HashMap<>();
        source.map.put("k1", "v1");

        // Build target object with several differences
        SampleBean target = new SampleBean();
        target.intArray = new int[]{1, 9, 3, 4}; // change index1, add element
        target.name = "new";                     // simple field change
        target.values = new ArrayList<>(Arrays.asList("x", "z", "y")); // resize + set
        target.map = new HashMap<>();
        target.map.put("k1", "v2"); // value change
        target.map.put("k2", "v2"); // new entry

        // Generate deltas
        List<GraphComparator.Delta> deltas = GraphComparator.compare(source, target, idFetcher);
        assertFalse(deltas.isEmpty());

        // Apply deltas
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas,
                idFetcher, GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty());

        // Verify mutations
        assertArrayEquals(new int[]{1, 9, 3, 4}, source.intArray);
        assertEquals("new", source.name);
        assertEquals(Arrays.asList("x", "z", "y"), source.values);
        assertEquals("v2", source.map.get("k1"));
        assertEquals("v2", source.map.get("k2"));
    }

    /* ---------------------------------------------------------------------- */
    /* 12. applyDelta error handling – missing field should produce DeltaError */
    /* ---------------------------------------------------------------------- */

    @Test
    public void applyDeltaWithMissingFieldProducesError() {
        // Source bean without the field "missing"
        SampleBean source = new SampleBean();
        source.name = "orig";

        // Create a delta that references a non‑existent field
        GraphComparator.Delta bogus = new GraphComparator.Delta(0L, "missing", "ptr", "orig", "new", null);
        bogus.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        List<GraphComparator.Delta> list = Collections.singletonList(bogus);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, list,
                idFetcher, GraphComparator.getJavaDeltaProcessor());

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getError().contains("field name missing"));
    }

    /* ---------------------------------------------------------------------- */
    /* Helper classes used by the tests                                       */
    /* ---------------------------------------------------------------------- */

    /**
     * Simple interface used by the ID fetcher.
     */
    private interface Identifiable {
        Long getId();
    }

    /**
     * Minimal POJO with an ID and a couple of fields.
     */
    private static class Node implements Identifiable {
        private final Long id;
        private String name;
        private Node child;

        Node(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        public void setChild(Node child) {
            this.child = child;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;
            Node node = (Node) o;
            return Objects.equals(id, node.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Node{id=" + id + ", name='" + name + "'}";
        }
    }

    /**
     * Bean used for applyDelta tests – fields are public for easy reflection.
     */
    public static class SampleBean {
        public int[] intArray;
        public String name;
        public List<String> values;
        public Map<String, String> map;
    }
}
