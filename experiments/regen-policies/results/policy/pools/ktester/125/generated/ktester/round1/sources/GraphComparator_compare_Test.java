package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class GraphComparator_compare_Test {

    /**
     * ID fetcher used by GraphComparator – extracts the `id` field from Bean or returns null otherwise
     */
    private GraphComparator.ID idFetcher;

    /**
     * Simple bean used for ID‑based comparison
     */
    private static class Bean {

        long id;

        String name;

        int count;

        Bean[] children;

        List<String> tags;

        Set<Integer> numbers;

        Map<String, String> attributes;

        Bean(long id, String name, int count) {
            this.id = id;
            this.name = name;
            this.count = count;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // initialise the ID fetcher for every test
        idFetcher = objectToId -> {
            if (objectToId instanceof Bean) {
                return ((Bean) objectToId).id;
            }
            // collections/maps do not have an ID
            if (objectToId instanceof Collection || objectToId instanceof Map) {
                return null;
            }
            throw new IllegalArgumentException("Unsupported type for ID fetching: " + (objectToId == null ? "null" : objectToId.getClass()));
        };
    }

    private static Stream<Arguments> logicalPrimitiveDifferences() {
        return Stream.of(// Integer
        Arguments.of(5, 6, 5, 6), // Long
        Arguments.of(10L, 20L, 10L, 20L), // Boolean
        Arguments.of(true, false, true, false), // Character
        Arguments.of('a', 'b', 'a', 'b'), // BigInteger
        Arguments.of(new BigInteger("123"), new BigInteger("124"), new BigInteger("123"), new BigInteger("124")), // String
        Arguments.of("foo", "bar", "foo", "bar"));
    }

    @Test
    void compareIdenticalBeansReturnsEmptyDeltaList() {
        Bean src = new Bean(1L, "alpha", 10);
        Bean tgt = new Bean(1L, "alpha", 10);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertTrue(deltas.isEmpty(), "Identical beans should produce no deltas");
    }

    @Test
    void compareBeanWithDifferentPrimitiveFieldProducesAssignDelta() {
        Bean src = new Bean(1L, "alpha", 10);
        // count differs
        Bean tgt = new Bean(1L, "alpha", 20);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals("count", d.getFieldName());
        assertEquals(10, d.getSourceValue());
        assertEquals(20, d.getTargetValue());
    }

    @Test
    void compareArrayChangesProducesResizeAndSetElementDeltas() {
        Bean src = new Bean(1L, "root", 0);
        Bean tgt = new Bean(1L, "root", 0);
        src.children = new Bean[] { new Bean(2L, "c1", 1), new Bean(3L, "c2", 2) };
        tgt.children = new Bean[] { new Bean(2L, "c1", 1), new Bean(3L, "c2", 99), new Bean(4L, "c3", 3) };
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Expect a resize (new length) and a set element for the changed element
        boolean hasResize = deltas.stream().anyMatch(d -> d.getCmd() == ARRAY_RESIZE);
        boolean hasSet = deltas.stream().anyMatch(d -> d.getCmd() == ARRAY_SET_ELEMENT);
        assertTrue(hasResize, "Array resize delta expected");
        assertTrue(hasSet, "Array set element delta expected");
    }

    @Test
    void compareSetAddAndRemove() {
        Bean src = new Bean(1L, "set", 0);
        Bean tgt = new Bean(1L, "set", 0);
        src.numbers = new HashSet<>(Arrays.asList(1, 2, 3));
        tgt.numbers = new HashSet<>(Arrays.asList(2, 3, 4));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Expect one REMOVE (1) and one ADD (4)
        long adds = deltas.stream().filter(d -> d.getCmd() == SET_ADD).count();
        long removes = deltas.stream().filter(d -> d.getCmd() == SET_REMOVE).count();
        assertEquals(1, adds, "One SET_ADD expected");
        assertEquals(1, removes, "One SET_REMOVE expected");
    }

    @Test
    void compareMapPutAndRemove() {
        Bean src = new Bean(1L, "map", 0);
        Bean tgt = new Bean(1L, "map", 0);
        src.attributes = new HashMap<>();
        src.attributes.put("a", "1");
        src.attributes.put("b", "2");
        tgt.attributes = new HashMap<>();
        tgt.attributes.put("b", "2");
        tgt.attributes.put("c", "3");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        boolean hasPut = deltas.stream().anyMatch(d -> d.getCmd() == MAP_PUT && "c".equals(d.getOptionalKey()));
        boolean hasRemove = deltas.stream().anyMatch(d -> d.getCmd() == MAP_REMOVE && "a".equals(d.getOptionalKey()));
        assertTrue(hasPut, "MAP_PUT for key 'c' expected");
        assertTrue(hasRemove, "MAP_REMOVE for key 'a' expected");
    }

    @Test
    void compareDifferentObjectTypesProducesFieldTypeChangedDelta() {
        Bean src = new Bean(1L, "type", 0);
        Bean tgt = new Bean(1L, "type", 0);
        // src uses a List, tgt uses a Set for the same logical field (tags)
        src.tags = new ArrayList<>(Arrays.asList("x", "y"));
        tgt.tags = new LinkedHashSet<>(Arrays.asList("x", "y"));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Since both are collections, type change should NOT be reported.
        // To force a type change we use a non‑collection field.
        src.name = "string";
        // change type from String to null (still same class) – not a type change.
        tgt.name = null;
        // Instead we create a field that holds different concrete class not covered by collection checks:
        src.attributes = new HashMap<>();
        src.attributes.put("k", "v");
        tgt.attributes = new TreeMap<>();
        tgt.attributes.put("k", "v");
        deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Map vs TreeMap are both Map, so still no type change.
        // To trigger OBJECT_FIELD_TYPE_CHANGED we need two non‑compatible classes, e.g., a String vs a List.
        src.name = "hello";
        // still same class (String) – not change.
        tgt.name = null;
        // Use a dummy field of type Object to hold different concrete types.
        Object srcObj = "string";
        Object tgtObj = new ArrayList<>();
        List<GraphComparator.Delta> typeDeltas = GraphComparator.compare(srcObj, tgtObj, idFetcher);
        assertFalse(typeDeltas.isEmpty());
        assertEquals(OBJECT_FIELD_TYPE_CHANGED, typeDeltas.get(0).getCmd());
    }

    @Test
    void compareOrphanedObjectProducesOrphanDelta() {
        Bean src = new Bean(1L, "root", 0);
        Bean child = new Bean(2L, "orphan", 0);
        src.children = new Bean[] { child };
        // target does NOT contain the child (orphan)
        Bean tgt = new Bean(1L, "root", 0);
        tgt.children = new Bean[0];
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        boolean hasOrphan = deltas.stream().anyMatch(d -> d.getCmd() == OBJECT_ORPHAN && Objects.equals(d.getId(), 2L));
        assertTrue(hasOrphan, "Orphan delta for child with id 2 expected");
    }

    @Test
    void compareWhenSourceIsNullProducesAssignDelta() {
        Bean tgt = new Bean(1L, "target", 5);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(null, tgt, idFetcher);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertNull(d.getSourceValue());
        assertEquals(tgt, d.getTargetValue());
    }

    @Test
    void compareWhenTargetIsNullProducesAssignDelta() {
        Bean src = new Bean(1L, "source", 5);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, null, idFetcher);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals(src, d.getSourceValue());
        assertNull(d.getTargetValue());
    }

    @Test
    void compareSameInstanceReturnsEmpty() {
        Bean bean = new Bean(1L, "same", 10);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(bean, bean, idFetcher);
        assertTrue(deltas.isEmpty(), "Comparing the same instance should yield no deltas");
    }

    /**
     * Parameterized test for logical primitive differences.
     *
     * <p>Logical primitives are handled by {@code GraphComparator.isLogicalPrimitive}
     * and should result in a single {@code OBJECT_ASSIGN_FIELD} delta when values differ.
     */
    @ParameterizedTest
    @MethodSource("logicalPrimitiveDifferences")
    void compareLogicalPrimitiveDifferences(Object src, Object tgt, Object expectedSrc, Object expectedTgt) {
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(1, deltas.size(), "Exactly one delta expected for differing logical primitives");
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals(expectedSrc, d.getSourceValue());
        assertEquals(expectedTgt, d.getTargetValue());
    }

    @Test
    void compareArrayResizeOnly() {
        Bean src = new Bean(1L, "arrayResize", 0);
        Bean tgt = new Bean(1L, "arrayResize", 0);
        src.children = new Bean[] { new Bean(2L, "c1", 1), new Bean(3L, "c2", 2), new Bean(4L, "c3", 3) };
        // shorter array, same first element
        tgt.children = new Bean[] { new Bean(2L, "c1", 1) };
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // Expect only an ARRAY_RESIZE delta (no element changes)
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == ARRAY_RESIZE), "Array resize delta expected");
        assertFalse(deltas.stream().anyMatch(d -> d.getCmd() == ARRAY_SET_ELEMENT), "No array set element delta expected when elements are unchanged");
    }

    @Test
    void compareMultipleSetAddRemoveOperations() {
        Bean src = new Bean(1L, "multiSet", 0);
        Bean tgt = new Bean(1L, "multiSet", 0);
        src.numbers = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        tgt.numbers = new HashSet<>(Arrays.asList(3, 4, 5, 6));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        long adds = deltas.stream().filter(d -> d.getCmd() == SET_ADD).count();
        long removes = deltas.stream().filter(d -> d.getCmd() == SET_REMOVE).count();
        assertEquals(2, adds, "Two SET_ADD deltas expected (5 and 6)");
        assertEquals(2, removes, "Two SET_REMOVE deltas expected (1 and 2)");
    }

    @Test
    void compareCyclicGraphDoesNotLoopAndProducesNoDeltasWhenIdentical() {
        // Build a cyclic graph: a <-> b
        Bean a = new Bean(1L, "a", 0);
        Bean b = new Bean(2L, "b", 0);
        a.children = new Bean[] { b };
        b.children = new Bean[] { a };
        // Clone the structure for target
        Bean a2 = new Bean(1L, "a", 0);
        Bean b2 = new Bean(2L, "b", 0);
        a2.children = new Bean[] { b2 };
        b2.children = new Bean[] { a2 };
        List<GraphComparator.Delta> deltas = GraphComparator.compare(a, a2, idFetcher);
        assertTrue(deltas.isEmpty(), "Identical cyclic graphs should produce no deltas");
    }

    @Test
    void compareMapsWithSameEntriesDifferentImplementationsYieldNoDeltas() {
        Bean src = new Bean(1L, "mapImpl", 0);
        Bean tgt = new Bean(1L, "mapImpl", 0);
        src.attributes = new HashMap<>();
        src.attributes.put("x", "1");
        src.attributes.put("y", "2");
        // TreeMap has the same entries but a different concrete class
        tgt.attributes = new TreeMap<>();
        tgt.attributes.put("y", "2");
        tgt.attributes.put("x", "1");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertTrue(deltas.isEmpty(), "Maps with identical entries should not produce deltas despite different implementations");
    }
}
