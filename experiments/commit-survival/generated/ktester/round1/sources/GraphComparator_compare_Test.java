package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class GraphComparator_compare_Test {

    /**
     * ID fetcher that extracts the {@code id} field from a {@link Person}.
     */
    private static final GraphComparator.ID PERSON_ID_FETCHER = Person::id;

    /**
     * ID fetcher that always returns {@code null} – used for the NPE test.
     */
    private static final GraphComparator.ID NULL_ID_FETCHER = o -> null;

    /**
     * Simple POJO used for ID‑based tests
     */
    static class Person {

        final int id;

        final String name;

        Person(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    class MyCustomCollection<E> extends AbstractCollection<E> {

        private final List<E> backing = new ArrayList<>();

        MyCustomCollection(Collection<E> c) {
            backing.addAll(c);
        }

        @Override
        public Iterator<E> iterator() {
            return backing.iterator();
        }

        @Override
        public int size() {
            return backing.size();
        }
    }

    class MyCustomCollection<E> extends AbstractCollection<E> {

        private final List<E> backing = new ArrayList<>();

        MyCustomCollection(Collection<E> c) {
            backing.addAll(c);
        }

        @Override
        public Iterator<E> iterator() {
            return backing.iterator();
        }

        @Override
        public int size() {
            return backing.size();
        }
    }

    // Example of a method source (currently unused but kept for illustration)
    static Stream<Arguments> dummySource() {
        return Stream.empty();
    }

    @Test
    void compareBothNull_returnsEmptyList() {
        List<GraphComparator.Delta> deltas = GraphComparator.compare(null, null, PERSON_ID_FETCHER);
        assertNotNull(deltas);
        assertTrue(deltas.isEmpty(), "Both null values should produce no deltas");
    }

    @Test
    void compareOneNull_returnsObjectAssignFieldDelta() {
        List<GraphComparator.Delta> deltas = GraphComparator.compare("source", null, PERSON_ID_FETCHER);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, d.getCmd(), "When one side is null the command should be OBJECT_ASSIGN_FIELD");
    }

    @Test
    void compareLogicalPrimitivesDifferent_returnsObjectAssignFieldDelta() {
        List<GraphComparator.Delta> deltas = GraphComparator.compare(42, 43, PERSON_ID_FETCHER);
        assertEquals(1, deltas.size());
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, deltas.get(0).getCmd(), "Different primitive wrapper values should generate OBJECT_ASSIGN_FIELD");
    }

    @Test
    void compareArraysDetectsChange() {
        int[] src = { 1, 2, 3 };
        int[] tgt = { 1, 9, 3 };
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, PERSON_ID_FETCHER);
        // At least one delta for the changed element (command ARRAY_SET_ELEMENT)
        assertFalse(deltas.isEmpty(), "Array difference should produce at least one delta");
        boolean hasSetElement = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.ARRAY_SET_ELEMENT);
        assertTrue(hasSetElement, "Expected an ARRAY_SET_ELEMENT command for the changed array element");
    }

    @Test
    void compareSetsDetectAddAndRemove() {
        Set<String> src = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> tgt = new HashSet<>(Collections.singletonList("a"));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, PERSON_ID_FETCHER);
        assertFalse(deltas.isEmpty(), "Set difference should produce deltas");
        boolean hasAdd = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.SET_ADD);
        boolean hasRemove = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.SET_REMOVE);
        // In this scenario only a removal is expected, but we check both possibilities
        assertTrue(hasRemove, "Expected a SET_REMOVE command for the missing element");
        // Ensure no unexpected add command
        assertFalse(hasAdd, "No SET_ADD command should be present when target set is a subset of source");
    }

    @Test
    void compareMapsDetectPutAndRemove() {
        Map<String, Integer> src = new HashMap<>();
        src.put("x", 1);
        src.put("y", 2);
        Map<String, Integer> tgt = new HashMap<>();
        tgt.put("x", 1);
        tgt.put("z", 3);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, PERSON_ID_FETCHER);
        assertFalse(deltas.isEmpty(), "Map difference should produce deltas");
        boolean hasPut = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.MAP_PUT);
        boolean hasRemove = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.MAP_REMOVE);
        assertTrue(hasPut, "Expected a MAP_PUT command for the new entry");
        assertTrue(hasRemove, "Expected a MAP_REMOVE command for the missing entry");
    }

    @Test
    void compareIdObjectsDetectOrphan() {
        // Source contains a Person that does not exist in target.
        Person srcPerson = new Person(1, "Alice");
        // different id, will be treated as separate object
        Person tgtPerson = new Person(2, "Bob");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(srcPerson, tgtPerson, PERSON_ID_FETCHER);
        // Expect at least one OBJECT_ASSIGN_FIELD for the id mismatch
        assertFalse(deltas.isEmpty(), "Different IDs should generate a delta");
        boolean hasAssign = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        assertTrue(hasAssign, "Expected OBJECT_ASSIGN_FIELD for mismatched IDs");
        // Now test orphan detection: source has extra Person, target has none.
        Person extra = new Person(99, "Orphan");
        List<GraphComparator.Delta> orphanDeltas = GraphComparator.compare(extra, null, PERSON_ID_FETCHER);
        // The compare method adds an OBJECT_ORPHAN delta for objects present only in source.
        boolean hasOrphan = orphanDeltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.OBJECT_ORPHAN);
        assertTrue(hasOrphan, "Expected OBJECT_ORPHAN delta for a source‑only ID object");
    }

    /**
     * Group: Same instance shortcut
     */
    @Test
    void compareSameInstance_returnsEmptyList() {
        String source = new String("test");
        // target refers to the exact same object
        String target = source;
        List<GraphComparator.Delta> deltas = GraphComparator.compare(source, target, null);
        assertTrue(deltas.isEmpty(), "Identical instance should produce no deltas");
    }

    /**
     * Group: Null handling – source null, target non‑null
     */
    @Test
    void compareSourceNullTargetNonNull_returnsAssignField() {
        List<GraphComparator.Delta> deltas = GraphComparator.compare(null, Integer.valueOf(5), null);
        assertEquals(1, deltas.size());
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, deltas.get(0).getCmd(), "Null source vs non‑null target should yield OBJECT_ASSIGN_FIELD");
    }

    /**
     * Group: Class mismatch – non‑collection objects
     */
    @Test
    void compareClassMismatch_returnsFieldTypeChanged() {
        List<GraphComparator.Delta> deltas = GraphComparator.compare(Integer.valueOf(10), "10", null);
        assertEquals(1, deltas.size());
        assertEquals(GraphComparator.Delta.Command.OBJECT_FIELD_TYPE_CHANGED, deltas.get(0).getCmd(), "Different non‑collection classes should yield OBJECT_FIELD_TYPE_CHANGED");
    }

    /**
     * Group: Logical primitive value change
     */
    @Test
    void compareLogicalPrimitiveBooleanChange() {
        List<GraphComparator.Delta> deltas = GraphComparator.compare(Boolean.TRUE, Boolean.FALSE, null);
        assertEquals(1, deltas.size());
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, deltas.get(0).getCmd(), "Boolean value change should produce OBJECT_ASSIGN_FIELD");
    }

    /**
     * Group: Array comparison – element change and resize
     */
    @Test
    void compareArrayElementChangeAndResize() {
        int[] src = { 1, 2, 3 };
        int[] tgt = { 1, 4, 3, 5 };
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(2, deltas.size(), "Expect exactly two deltas (set element + resize)");
        boolean hasSet = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.ARRAY_SET_ELEMENT);
        boolean hasResize = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.ARRAY_RESIZE);
        assertTrue(hasSet, "Missing ARRAY_SET_ELEMENT delta");
        assertTrue(hasResize, "Missing ARRAY_RESIZE delta");
    }

    /**
     * Group: Set comparison – add and remove
     */
    @Test
    void compareSetAddAndRemove() {
        Set<String> src = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> tgt = new HashSet<>(Arrays.asList("a", "c"));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(2, deltas.size(), "Expect two deltas for add and remove");
        boolean hasAdd = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.SET_ADD);
        boolean hasRemove = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.SET_REMOVE);
        assertTrue(hasAdd, "Missing SET_ADD delta");
        assertTrue(hasRemove, "Missing SET_REMOVE delta");
    }

    /**
     * Group: Map comparison – value change and new entry
     */
    @Test
    void compareMapValueChangeAndNewEntry() {
        Map<String, Integer> src = new HashMap<>();
        src.put("x", 1);
        src.put("y", 2);
        Map<String, Integer> tgt = new HashMap<>();
        tgt.put("x", 1);
        tgt.put("y", 3);
        tgt.put("z", 4);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        // Expect two MAP_PUT deltas (y changed, z new) and one MAP_REMOVE (none in this case)
        assertEquals(2, deltas.size(), "Expect exactly two MAP_PUT deltas");
        long putCount = deltas.stream().filter(d -> d.getCmd() == GraphComparator.Delta.Command.MAP_PUT).count();
        assertEquals(2, putCount, "Both changed and new entries should be MAP_PUT");
    }

    /**
     * Group: List comparison – element change and resize
     */
    @Test
    void compareListElementChangeAndResize() {
        List<Integer> src = Arrays.asList(1, 2, 3);
        List<Integer> tgt = Arrays.asList(1, 4, 3, 5);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(2, deltas.size(), "Expect two deltas (set element + resize)");
        boolean hasSet = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.LIST_SET_ELEMENT);
        boolean hasResize = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.LIST_RESIZE);
        assertTrue(hasSet, "Missing LIST_SET_ELEMENT delta");
        assertTrue(hasResize, "Missing LIST_RESIZE delta");
    }

    /**
     * Group: Custom collection unsupported – throws RuntimeException
     */
    @Test
    void compareCustomCollection_throwsRuntimeException() {
        // Minimal custom collection that implements Collection but not List/Set
        class MyCustomCollection<E> extends AbstractCollection<E> {

            private final List<E> backing = new ArrayList<>();

            MyCustomCollection(Collection<E> c) {
                backing.addAll(c);
            }

            @Override
            public Iterator<E> iterator() {
                return backing.iterator();
            }

            @Override
            public int size() {
                return backing.size();
            }
        }
        MyCustomCollection<Integer> src = new MyCustomCollection<>(Arrays.asList(1, 2));
        MyCustomCollection<Integer> tgt = new MyCustomCollection<>(Arrays.asList(1, 2, 3));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> GraphComparator.compare(src, tgt, null));
        assertTrue(ex.getMessage().contains("Detected custom Collection"), "Exception message should mention custom Collection");
    }

    /**
     * Group: ID object – same ID, different field values
     */
    @Test
    void compareIdObjectSameIdDifferentFields() {
        Person src = new Person(1, "Alice");
        Person tgt = new Person(1, "Bob");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, PERSON_ID_FETCHER);
        // The name field differs, so we expect a delta for that field.
        assertEquals(1, deltas.size(), "Only the differing field should generate a delta");
        GraphComparator.Delta delta = deltas.get(0);
        assertEquals(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD, delta.getCmd(), "Field value change should be OBJECT_ASSIGN_FIELD");
        assertEquals("name", delta.getFieldName(), "Delta should reference the 'name' field");
    }

    /**
     * Group: Orphan detection – object present only in source
     */
    @Test
    void compareOrphanDetection() {
        Person src = new Person(1, "Alice");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, null, PERSON_ID_FETCHER);
        // One delta for root assignment (null→null) is filtered out by the logic,
        // leaving only the OBJECT_ORPHAN delta.
        boolean hasOrphan = deltas.stream().anyMatch(d -> d.getCmd() == GraphComparator.Delta.Command.OBJECT_ORPHAN);
        assertTrue(hasOrphan, "Expected OBJECT_ORPHAN delta for source‑only object");
    }

    /**
     * Group: ID fetcher returns null ID – NullPointerException
     */
    @Test
    void compareIdFetcherReturnsNull_throwsNPE() {
        Person src = new Person(1, "Alice");
        Person tgt = new Person(1, "Bob");
        // The fetcher returns null, causing srcId.equals(targetId) to NPE.
        assertThrows(NullPointerException.class, () -> GraphComparator.compare(src, tgt, NULL_ID_FETCHER));
    }
}
