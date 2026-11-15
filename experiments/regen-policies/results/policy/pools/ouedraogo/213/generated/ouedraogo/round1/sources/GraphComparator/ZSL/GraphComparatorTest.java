package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link GraphComparator}.  The tests exercise all public
 * functionality as well as the internal comparison logic by using real
 * objects (no mocks) and the built‑in {@link GraphComparator.JavaDeltaProcessor}.
 */
public class GraphComparatorTest
{
    /** Simple POJO used for ID‑based comparison. */
    public static class Node
    {
        /** unique identifier – used by the {@link TestIdFetcher}. */
        public Long id;
        /** mutable data field – changes should generate deltas. */
        public String name;

        public Node(Long id, String name)
        {
            this.id   = id;
            this.name = name;
        }

        @Override
        public String toString()
        {
            return "Node{id=" + id + ", name='" + name + "'}";
        }
    }

    /** Container class with a variety of fields used for applyDelta tests. */
    public static class Container
    {
        public int[]           intArray;
        public Node[]          nodeArray;
        public List<Node>      nodeList;
        public Set<Node>       nodeSet;
        public Map<String,Node> nodeMap;

        public Container()
        {
            // keep default constructor – fields are populated directly in tests
        }
    }

    /** ID fetcher that simply returns the {@code id} field of a {@link Node}. */
    private static final GraphComparator.ID TestIdFetcher = new GraphComparator.ID()
    {
        @Override
        public Object getId(Object objectToId)
        {
            if (objectToId instanceof Node)
            {
                return ((Node) objectToId).id;
            }
            throw new IllegalArgumentException("Unsupported type for id fetching");
        }
    };

    private GraphComparator.DeltaProcessor javaProcessor;

    @Before
    public void setUp()
    {
        javaProcessor = GraphComparator.getJavaDeltaProcessor();
    }

    /* -------------------------------------------------------
     *  Basic Delta behaviour (equals, hashCode, toString)
     * ------------------------------------------------------- */
    @Test
    public void testDeltaEqualsAndHashCode()
    {
        GraphComparator.Delta d1 = new GraphComparator.Delta(1L, "field", "ptr", null, null, null);
        GraphComparator.Delta d2 = new GraphComparator.Delta(2L, "other", "ptr", null, null, null);
        GraphComparator.Delta d3 = new GraphComparator.Delta(3L, "field", "differentPtr", null, null, null);

        assertEquals(d1, d2);                 // equality is based ONLY on srcPtr
        assertEquals(d1.hashCode(), d2.hashCode());

        assertNotEquals(d1, d3);
        assertNotEquals(d1.hashCode(), d3.hashCode());

        String s = d1.toString();
        assertTrue(s.contains("id=1"));
        assertTrue(s.contains("fieldName='field'"));
    }

    @Test
    public void testDeltaErrorToString()
    {
        GraphComparator.Delta base = new GraphComparator.Delta(1L, "field", "ptr", null, null, null);
        GraphComparator.DeltaError err = new GraphComparator.DeltaError("boom", base);
        assertEquals("boom", err.getError());
        String s = err.toString();
        assertTrue(s.startsWith("boom ("));
        assertTrue(s.contains("Delta {"));
    }

    @Test
    public void testCommandFromName()
    {
        assertEquals(GraphComparator.Delta.Command.ARRAY_SET_ELEMENT,
                GraphComparator.Delta.Command.fromName("array.setelement"));
        assertEquals(GraphComparator.Delta.Command.OBJECT_ORPHAN,
                GraphComparator.Delta.Command.fromName("OBJECT.ORPHAN"));

        try
        {
            GraphComparator.Delta.Command.fromName(null);
            fail("Expected IllegalArgumentException for null name");
        }
        catch (IllegalArgumentException ignored) {}

        try
        {
            GraphComparator.Delta.Command.fromName("unknown.command");
            fail("Expected IllegalArgumentException for unknown name");
        }
        catch (IllegalArgumentException ignored) {}
    }

    /* -------------------------------------------------------
     *  Array comparison – primitive and object arrays
     * ------------------------------------------------------- */
    @Test
    public void testComparePrimitiveArray()
    {
        int[] src = {1, 2, 3};
        int[] tgt = {1, 9, 3, 4};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, TestIdFetcher);
        // Expect a resize + one element change (index 1)
        assertEquals(2, deltas.size());

        GraphComparator.Delta resize = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.ARRAY_RESIZE)
                .findFirst()
                .orElse(null);
        assertNotNull(resize);
        assertEquals(4, resize.getOptionalKey());

        GraphComparator.Delta set = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.ARRAY_SET_ELEMENT)
                .findFirst()
                .orElse(null);
        assertNotNull(set);
        assertEquals(1, set.getOptionalKey()); // index
        assertEquals(2, set.getSourceValue()); // original value
        assertEquals(9, set.getTargetValue()); // new value
    }

    @Test
    public void testCompareObjectArrayWithId()
    {
        Node n1 = new Node(1L, "A");
        Node n2 = new Node(2L, "B");
        Node[] src = {n1, n2};

        // Change name of node 2, keep node 1 identical, add a new node 3
        Node n2t = new Node(2L, "B-modified");
        Node n3 = new Node(3L, "C");
        Node[] tgt = {n1, n2t, n3};

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, TestIdFetcher);
        // Expect resize (length 3) and a SET_ELEMENT for index 1 (different name)
        assertEquals(2, deltas.size());

        GraphComparator.Delta resize = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.ARRAY_RESIZE)
                .findFirst()
                .orElse(null);
        assertNotNull(resize);
        assertEquals(3, resize.getOptionalKey());

        GraphComparator.Delta set = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.ARRAY_SET_ELEMENT)
                .findFirst()
                .orElse(null);
        assertNotNull(set);
        assertEquals(1, set.getOptionalKey()); // index 1
        assertEquals(n2, set.getSourceValue());
        assertEquals(n2t, set.getTargetValue());
    }

    /* -------------------------------------------------------
     *  Set comparison (add / remove)
     * ------------------------------------------------------- */
    @Test
    public void testCompareSets()
    {
        Node a = new Node(1L, "A");
        Node b = new Node(2L, "B");
        Node c = new Node(3L, "C");

        Set<Node> src = new HashSet<>(Arrays.asList(a, b));
        Set<Node> tgt = new HashSet<>(Arrays.asList(a, c));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, TestIdFetcher);
        // Expect one REMOVE (b) and one ADD (c)
        assertEquals(2, deltas.size());

        GraphComparator.Delta remove = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.SET_REMOVE)
                .findFirst()
                .orElse(null);
        GraphComparator.Delta add = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.SET_ADD)
                .findFirst()
                .orElse(null);

        assertNotNull(remove);
        assertEquals(b, remove.getSourceValue());

        assertNotNull(add);
        assertEquals(c, add.getTargetValue());
    }

    /* -------------------------------------------------------
     *  Map comparison (put / remove)
     * ------------------------------------------------------- */
    @Test
    public void testCompareMaps()
    {
        Node a1 = new Node(1L, "A1");
        Node a2 = new Node(2L, "A2");
        Node b1 = new Node(3L, "B1");
        Node b2 = new Node(4L, "B2");

        Map<String, Node> src = new HashMap<>();
        src.put("a", a1);
        src.put("b", a2);

        Map<String, Node> tgt = new HashMap<>();
        tgt.put("a", a1);          // unchanged
        tgt.put("b", b1);          // different id – should be a PUT
        tgt.put("c", b2);          // new key – should be a PUT

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, TestIdFetcher);
        // Expect: PUT for "b", PUT for "c"
        // No remove because every src key exists in target
        assertEquals(2, deltas.size());

        Set<String> putKeys = new HashSet<>();
        for (GraphComparator.Delta d : deltas)
        {
            assertEquals(GraphComparator.Delta.Command.MAP_PUT, d.getCmd());
            putKeys.add((String) d.getOptionalKey());
        }
        assertTrue(putKeys.containsAll(Arrays.asList("b", "c")));
    }

    @Test
    public void testCompareMapsWithRemoval()
    {
        Node a = new Node(1L, "A");
        Node b = new Node(2L, "B");
        Node c = new Node(3L, "C");

        Map<String, Node> src = new HashMap<>();
        src.put("a", a);
        src.put("b", b);
        src.put("c", c);

        Map<String, Node> tgt = new HashMap<>();
        tgt.put("a", a);   // keep
        // "b" removed, "c" removed, "d" added
        Node d = new Node(4L, "D");
        tgt.put("d", d);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, TestIdFetcher);
        // Expect: REMOVE for "b", REMOVE for "c", PUT for "d"
        assertEquals(3, deltas.size());

        boolean haveRemoveB = false, haveRemoveC = false, havePutD = false;
        for (GraphComparator.Delta dlt : deltas)
        {
            if (dlt.getCmd() == GraphComparator.Delta.Command.MAP_REMOVE)
            {
                if ("b".equals(dlt.getOptionalKey())) haveRemoveB = true;
                if ("c".equals(dlt.getOptionalKey())) haveRemoveC = true;
            }
            else if (dlt.getCmd() == GraphComparator.Delta.Command.MAP_PUT)
            {
                if ("d".equals(dlt.getOptionalKey())) havePutD = true;
            }
        }
        assertTrue(haveRemoveB);
        assertTrue(haveRemoveC);
        assertTrue(havePutD);
    }

    /* -------------------------------------------------------
     *  List comparison (resize + set element)
     * ------------------------------------------------------- */
    @Test
    public void testCompareLists()
    {
        Node n1 = new Node(1L, "A");
        Node n2 = new Node(2L, "B");
        List<Node> src = new ArrayList<>(Arrays.asList(n1, n2));

        // Change name of first element, add a third element
        Node n1t = new Node(1L, "A-mod");
        Node n3 = new Node(3L, "C");
        List<Node> tgt = new ArrayList<>(Arrays.asList(n1t, n2, n3));

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, TestIdFetcher);
        // Expect one resize (size 3) and two set‑element deltas (index 0 changed, index 2 added)
        assertEquals(3, deltas.size());

        GraphComparator.Delta resize = deltas.stream()
                .filter(d -> d.getCmd() == GraphComparator.Delta.Command.LIST_RESIZE)
                .findFirst()
                .orElse(null);
        assertNotNull(resize);
        assertEquals(3, resize.getOptionalKey());

        List<GraphComparator.Delta> setElems = new ArrayList<>();
        for (GraphComparator.Delta d : deltas)
        {
            if (d.getCmd() == GraphComparator.Delta.Command.LIST_SET_ELEMENT)
                setElems.add(d);
        }
        assertEquals(2, setElems.size());

        // index 0 changed
        GraphComparator.Delta idx0 = setElems.stream()
                .filter(d -> (int) d.getOptionalKey() == 0)
                .findFirst()
                .orElse(null);
        assertNotNull(idx0);
        assertEquals(n1, idx0.getSourceValue());
        assertEquals(n1t, idx0.getTargetValue());

        // index 2 added (source value null)
        GraphComparator.Delta idx2 = setElems.stream()
                .filter(d -> (int) d.getOptionalKey() == 2)
                .findFirst()
                .orElse(null);
        assertNotNull(idx2);
        assertNull(idx2.getSourceValue());
        assertEquals(n3, idx2.getTargetValue());
    }

    /* -------------------------------------------------------
     *  Orphan detection – object present only in source
     * ------------------------------------------------------- */
    @Test
    public void testOrphanDetection()
    {
        Node kept = new Node(1L, "keep");
        Node orphan = new Node(2L, "orphan");

        List<Node> src = Arrays.asList(kept, orphan);
        List<Node> tgt = Collections.singletonList(kept);

        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, TestIdFetcher);
        boolean hasOrphan = deltas.stream()
                .anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.OBJECT_ORPHAN &&
                               d.getId().equals(orphan.id));
        assertTrue("Orphan delta should be produced for node with id 2", hasOrphan);
    }

    /* -------------------------------------------------------
     *  applyDelta – end‑to‑end mutation test
     * ------------------------------------------------------- */
    @Test
    public void testApplyDeltaEndToEnd()
    {
        // ----- build source container -----
        Container src = new Container();
        src.intArray = new int[]{1, 2, 3};
        src.nodeArray = new Node[]{new Node(1L, "A"), new Node(2L, "B")};

        src.nodeList = new ArrayList<>(Arrays.asList(new Node(1L, "A"), new Node(2L, "B")));
        src.nodeSet  = new HashSet<>(Arrays.asList(new Node(1L, "A"), new Node(2L, "B")));

        src.nodeMap = new HashMap<>();
        src.nodeMap.put("one", new Node(1L, "A"));
        src.nodeMap.put("two", new Node(2L, "B"));

        // ----- build target container (different in every field) -----
        Container tgt = new Container();
        tgt.intArray = new int[]{1, 9, 3, 4};                     // resize + element change
        tgt.nodeArray = new Node[]{new Node(1L, "A-mod"), new Node(2L, "B"), new Node(3L, "C")};

        tgt.nodeList = new ArrayList<>(Arrays.asList(new Node(1L, "A-mod"), new Node(2L, "B"), new Node(3L, "C")));
        tgt.nodeSet  = new HashSet<>(Arrays.asList(new Node(1L, "A-mod"), new Node(3L, "C"))); // remove 2, add 3

        tgt.nodeMap = new HashMap<>();
        tgt.nodeMap.put("one", new Node(1L, "A-mod")); // changed value
        tgt.nodeMap.put("three", new Node(3L, "C"));   // new entry

        // ----- compute deltas -----
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, TestIdFetcher);
        // sanity check that we have a non‑empty delta list
        assertFalse(deltas.isEmpty());

        // ----- apply them to the source -----
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(src, deltas, TestIdFetcher, javaProcessor);
        assertTrue("No errors expected when applying deltas", errors.isEmpty());

        // ----- verify mutation -----
        // intArray
        assertArrayEquals(tgt.intArray, src.intArray);

        // nodeArray – compare element by element using ids and names
        assertEquals(tgt.nodeArray.length, src.nodeArray.length);
        for (int i = 0; i < tgt.nodeArray.length; i++)
        {
            assertEquals(tgt.nodeArray[i].id, src.nodeArray[i].id);
            assertEquals(tgt.nodeArray[i].name, src.nodeArray[i].name);
        }

        // nodeList
        assertEquals(tgt.nodeList.size(), src.nodeList.size());
        for (int i = 0; i < tgt.nodeList.size(); i++)
        {
            Node expected = tgt.nodeList.get(i);
            Node actual   = src.nodeList.get(i);
            assertEquals(expected.id, actual.id);
            assertEquals(expected.name, actual.name);
        }

        // nodeSet – compare by id and name (sets are unordered)
        assertEquals(tgt.nodeSet.size(), src.nodeSet.size());
        for (Node expected : tgt.nodeSet)
        {
            boolean found = src.nodeSet.stream()
                    .anyMatch(actual -> actual.id.equals(expected.id) && Objects.equals(actual.name, expected.name));
            assertTrue("Set should contain node with id " + expected.id, found);
        }

        // nodeMap – compare each entry
        assertEquals(tgt.nodeMap.size(), src.nodeMap.size());
        for (Map.Entry<String, Node> e : tgt.nodeMap.entrySet())
        {
            Node srcNode = src.nodeMap.get(e.getKey());
            assertNotNull("Map should contain key " + e.getKey(), srcNode);
            assertEquals(e.getValue().id, srcNode.id);
            assertEquals(e.getValue().name, srcNode.name);
        }
    }

    /**
     * Helper to assert two primitive int arrays are equal (JUnit 4 lacks a direct overload for int[]).
     */
    private static void assertArrayEquals(int[] expected, int[] actual)
    {
        assertEquals("Array length differs", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++)
        {
            assertEquals("Element at index " + i + " differs", expected[i], actual[i]);
        }
    }
}
