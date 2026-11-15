package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class GraphComparator_compare_Test {

    /**
     * ID fetcher used by all tests.
     */
    private GraphComparator.ID idFetcher;

    /**
     * Simple marker interface used by test objects to expose an identifier.
     */
    private interface HasId {

        Object getId();
    }

    class SetHolder implements HasId {

        final long id = 1L;

        final Set<String> values;

        SetHolder(Set<String> values) {
            this.values = values;
        }

        @Override
        public Object getId() {
            return id;
        }
    }

    /**
     * Simple POJO that implements {@code HasId}.
     */
    private static class Node implements HasId {

        final long id;

        String name;

        List<Node> children = new ArrayList<>();

        Node(long id) {
            this.id = id;
        }

        Node(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Object getId() {
            return id;
        }
    }

    /**
     * Simple POJO used for ID‑object tests.
     */
    private static class Person implements HasId {

        final Long id;

        String name;

        Person(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Object getId() {
            return id;
        }
    }

    /**
     * Simple POJO without an ID – used for DeepEquals fallback tests.
     */
    private static class Box {

        final String color;

        Box(String color) {
            this.color = color;
        }
    }

    /**
     * Custom collection that implements {@code Collection} but is neither {@code List} nor {@code Set}.
     */
    private static class MyCollection<E> implements Collection<E> {

        private final Collection<E> delegate = new ArrayList<>();

        MyCollection(Collection<E> c) {
            delegate.addAll(c);
        }

        // delegate all methods – only a subset is needed for the test
        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return delegate.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return delegate.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(c);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }

    /**
     * Custom list that extends {@code ArrayList} – should be treated as a List.
     */
    private static class MyArrayList<E> extends ArrayList<E> {

        MyArrayList(Collection<? extends E> c) {
            super(c);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the ID fetcher before each test.
        idFetcher = objectToId -> {
            if (objectToId instanceof HasId) {
                return ((HasId) objectToId).getId();
            }
            if (objectToId instanceof Collection || objectToId instanceof Map) {
                // Collections and maps are not ID‑tracked.
                return null;
            }
            throw new IllegalArgumentException("Unsupported object type for ID fetching: " + (objectToId == null ? "null" : objectToId.getClass()));
        };
        idFetcher = objectToId -> {
            if (objectToId instanceof HasId) {
                return ((HasId) objectToId).getId();
            }
            if (objectToId instanceof Collection || objectToId instanceof Map) {
                // Collections and maps are not ID‑tracked.
                return null;
            }
            throw new IllegalArgumentException("Unsupported object type for ID fetching: " + (objectToId == null ? "null" : objectToId.getClass()));
        };
    }

    /**
     * Verify that comparing two identical object graphs yields an empty delta list.
     */
    @Test
    void compareIdenticalGraphs_returnsNoDeltas() {
        // GIVEN
        Node root1 = new Node(1L, "root");
        Node childA = new Node(2L, "A");
        Node childB = new Node(3L, "B");
        root1.children.add(childA);
        root1.children.add(childB);
        childA.children.add(new Node(4L, "A1"));
        childB.children.add(new Node(5L, "B1"));
        // Clone the graph using JsonIo (as done in the original test suite) to obtain a
        // separate but equal object graph.
        Node root2 = JsonIo.deepCopy(root1, null, null);
        // WHEN
        List<GraphComparator.Delta> deltas = GraphComparator.compare(root1, root2, idFetcher);
        // THEN
        assertNotNull(deltas, "Delta list must not be null");
        assertTrue(deltas.isEmpty(), "Identical graphs should produce no deltas");
    }

    /**
     * Verify that a changed primitive field is reported with OBJECT_ASSIGN_FIELD.
     */
    @Test
    void compareDifferentPrimitiveField_reportsAssignField() {
        // GIVEN
        Node src = new Node(1L, "original");
        // same id, different name
        Node tgt = new Node(1L, "modified");
        // WHEN
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // THEN
        assertEquals(1, deltas.size(), "Exactly one delta expected");
        GraphComparator.Delta delta = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, delta.getCmd(), "Delta command should be OBJECT_ASSIGN_FIELD");
        assertEquals("name", delta.getFieldName(), "Field name should be 'name'");
        assertEquals("original", delta.getSourceValue());
        assertEquals("modified", delta.getTargetValue());
    }

    /**
     * Verify that a missing child in the target graph is reported as OBJECT_ASSIGN_FIELD on the collection field.
     */
    @Test
    void compareMissingChild_reportsAssignFieldOnCollection() {
        // GIVEN
        Node src = new Node(1L, "root");
        Node child = new Node(2L, "child");
        src.children.add(child);
        // same root, but no child
        Node tgt = new Node(1L, "root");
        // WHEN
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // THEN
        // The comparator will detect that the List field 'children' differs.
        // The exact command for list differences is LIST_RESIZE or LIST_SET_ELEMENT,
        // but because the whole list content differs, the algorithm falls back to
        // OBJECT_ASSIGN_FIELD for the collection field.
        assertFalse(deltas.isEmpty(), "There should be at least one delta");
        boolean found = deltas.stream().anyMatch(d -> d.getCmd() == OBJECT_ASSIGN_FIELD && "children".equals(d.getFieldName()));
        assertTrue(found, "Expected OBJECT_ASSIGN_FIELD delta for the 'children' field");
    }

    /**
     * Verify that an orphaned object (present in source but not in target) is reported with OBJECT_ORPHAN.
     */
    @Test
    void compareOrphanedObject_reportsObjectOrphan() {
        // GIVEN
        Node src = new Node(1L, "root");
        Node orphan = new Node(99L, "orphan");
        // orphan is only reachable from source
        src.children.add(orphan);
        // target has no children
        Node tgt = new Node(1L, "root");
        // WHEN
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // THEN
        // Expect at least two deltas:
        // 1. OBJECT_ASSIGN_FIELD for the missing 'children' list content.
        // 2. OBJECT_ORPHAN for the orphaned node.
        boolean hasOrphan = deltas.stream().anyMatch(d -> d.getCmd() == OBJECT_ORPHAN && Objects.equals(d.getId(), 99L));
        assertTrue(hasOrphan, "Orphaned node should produce an OBJECT_ORPHAN delta");
    }

    /**
     * Verify that differing collection implementations (HashSet vs TreeSet) are handled correctly.
     */
    @Test
    void compareDifferentSetImplementations_detectsElementChanges() {
        // GIVEN
        Set<String> srcSet = new HashSet<>(Arrays.asList("a", "b"));
        // 'b' removed, 'c' added
        Set<String> tgtSet = new TreeSet<>(Arrays.asList("a", "c"));
        // Wrap sets in a container that implements HasId so the comparator can track them.
        class SetHolder implements HasId {

            final long id = 1L;

            final Set<String> values;

            SetHolder(Set<String> values) {
                this.values = values;
            }

            @Override
            public Object getId() {
                return id;
            }
        }
        SetHolder src = new SetHolder(srcSet);
        SetHolder tgt = new SetHolder(tgtSet);
        // WHEN
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        // THEN
        // Expect two deltas: one SET_REMOVE for "b" and one SET_ADD for "c".
        assertEquals(2, deltas.size(), "Two deltas expected for set element changes");
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == SET_REMOVE && "b".equals(d.getTargetValue())), "SET_REMOVE delta for element 'b' expected");
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == SET_ADD && "c".equals(d.getTargetValue())), "SET_ADD delta for element 'c' expected");
    }

    @Test
    void compareSameInstance_returnsNoDeltas() {
        Integer src = Integer.valueOf(5);
        // same instance
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, src, null);
        assertTrue(deltas.isEmpty(), "Identical instance should produce no deltas");
    }

    @Test
    void compareNullSource_createsAssignFieldDelta() {
        Integer target = Integer.valueOf(10);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(null, target, null);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertNull(d.getSourceValue());
        assertEquals(target, d.getTargetValue());
    }

    @Test
    void compareNullTarget_createsAssignFieldDelta() {
        String src = "abc";
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, null, null);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals(src, d.getSourceValue());
        assertNull(d.getTargetValue());
    }

    @Test
    void compareDifferentNonCollectionClasses_reportsFieldTypeChanged() {
        String src = new String("foo");
        Integer tgt = Integer.valueOf(1);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        assertEquals(OBJECT_FIELD_TYPE_CHANGED, deltas.get(0).getCmd());
    }

    @Test
    void compareLogicalPrimitiveMismatch_reportsAssignField() {
        Integer src = Integer.valueOf(5);
        Integer tgt = Integer.valueOf(6);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        assertEquals(OBJECT_ASSIGN_FIELD, deltas.get(0).getCmd());
    }

    @Test
    void compareLogicalPrimitiveEqualityDifferentInstances_noDelta() {
        Integer src = new Integer(128);
        Integer tgt = Integer.valueOf(128);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertTrue(deltas.isEmpty(), "Equal logical primitives should yield no deltas");
    }

    @Test
    void compareArrayElementChange_reportsArraySetElement() {
        int[] src = { 1, 2, 3 };
        int[] tgt = { 1, 9, 3 };
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(ARRAY_SET_ELEMENT, d.getCmd());
        // optionalKey holds the index of the changed element
        assertEquals(1, d.getOptionalKey());
    }

    @Test
    void compareArrayResize_shrink_reportsArrayResize() {
        int[] src = { 1, 2, 3, 4 };
        int[] tgt = { 1, 2, 3 };
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        assertEquals(ARRAY_RESIZE, deltas.get(0).getCmd());
    }

    @Test
    void compareArrayResize_grow_reportsArrayResize() {
        int[] src = { 1, 2 };
        int[] tgt = { 1, 2, 3, 4 };
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        assertEquals(ARRAY_RESIZE, deltas.get(0).getCmd());
    }

    @Test
    void compareSetDifferences_reportsAddAndRemove() {
        Set<String> src = new HashSet<>(Arrays.asList("a"));
        Set<String> tgt = new HashSet<>(Arrays.asList("b"));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(2, deltas.size());
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == SET_REMOVE && "a".equals(d.getTargetValue())));
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == SET_ADD && "b".equals(d.getTargetValue())));
    }

    @Test
    void compareSetIdentical_noDelta() {
        Set<String> src = new HashSet<>(Arrays.asList("x", "y"));
        Set<String> tgt = new HashSet<>(Arrays.asList("y", "x"));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertTrue(deltas.isEmpty());
    }

    @Test
    void compareMapValueChange_reportsMapPut() {
        Map<String, Integer> src = Collections.singletonMap("key", 1);
        Map<String, Integer> tgt = Collections.singletonMap("key", 2);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(MAP_PUT, d.getCmd());
        assertEquals("key", d.getOptionalKey());
        assertEquals(2, d.getTargetValue());
    }

    @Test
    void compareMapExtraKeyRemoval_reportsMapRemove() {
        Map<String, Integer> src = Map.of("a", 1, "b", 2);
        Map<String, Integer> tgt = Collections.singletonMap("a", 1);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(MAP_REMOVE, d.getCmd());
        assertEquals("b", d.getOptionalKey());
    }

    @Test
    void compareListElementChange_reportsListSetElement() {
        List<Integer> src = Arrays.asList(1, 2);
        List<Integer> tgt = Arrays.asList(1, 9);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(LIST_SET_ELEMENT, d.getCmd());
        // index
        assertEquals(1, d.getOptionalKey());
    }

    @Test
    void compareListResizeShrink_reportsListResize() {
        List<Integer> src = Arrays.asList(1, 2, 3);
        List<Integer> tgt = Arrays.asList(1, 2);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        assertEquals(LIST_RESIZE, deltas.get(0).getCmd());
    }

    @Test
    void compareListResizeGrow_reportsListResize() {
        List<Integer> src = Arrays.asList(1, 2);
        List<Integer> tgt = Arrays.asList(1, 2, 3, 4);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        assertEquals(LIST_RESIZE, deltas.get(0).getCmd());
    }

    @Test
    void compareUnsupportedCustomCollection_throwsRuntimeException() {
        MyCollection<Integer> src = new MyCollection<>(Arrays.asList(1, 2));
        MyCollection<Integer> tgt = new MyCollection<>(Arrays.asList(1, 2));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> GraphComparator.compare(src, tgt, null));
        assertTrue(ex.getMessage().contains("Detected custom Collection that does not extend List or Set"));
    }

    @Test
    void compareIdObjectSameIdDifferentField_reportsAssignFieldOnField() {
        Person src = new Person(1L, "Alice");
        // same id, different name
        Person tgt = new Person(1L, "Bob");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertEquals("name", d.getFieldName());
        assertEquals("Alice", d.getSourceValue());
        assertEquals("Bob", d.getTargetValue());
    }

    @Test
    void compareIdObjectDifferentIds_reportsAssignFieldAtRoot() {
        Person src = new Person(1L, "Alice");
        Person tgt = new Person(2L, "Alice");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertNull(d.getFieldName(), "Root delta should have null fieldName");
    }

    @Test
    void compareIdObjectNullId_fallbackToDeepEquals_reportsAssignFieldAtRoot() {
        Person src = new Person(null, "Alice");
        Person tgt = new Person(null, "Bob");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, idFetcher);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertNull(d.getFieldName());
    }

    @Test
    void compareNonIdObjectFallbackDeepEquals_reportsAssignFieldAtRoot() {
        Box src = new Box("red");
        Box tgt = new Box("blue");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(OBJECT_ASSIGN_FIELD, d.getCmd());
        assertNull(d.getFieldName());
    }

    @Test
    void compareOrphanedIdObject_reportsObjectOrphan() {
        Person src = new Person(1L, "Alice");
        // target does not contain the person at all
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, null, idFetcher);
        assertTrue(deltas.stream().anyMatch(d -> d.getCmd() == OBJECT_ORPHAN && Objects.equals(d.getId(), 1L)), "Orphan delta for id 1L expected");
    }

    @Test
    void compareCollectionTypeMismatch_setVsList_reportsFieldTypeChanged() {
        Set<Integer> src = new HashSet<>(Arrays.asList(1, 2));
        List<Integer> tgt = Arrays.asList(1, 2);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        assertEquals(OBJECT_FIELD_TYPE_CHANGED, deltas.get(0).getCmd());
    }

    @Test
    void compareCollectionTypeMismatch_mapVsSet_reportsFieldTypeChanged() {
        Map<String, Integer> src = Collections.singletonMap("k", 1);
        Set<Integer> tgt = new HashSet<>(Arrays.asList(1));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        assertEquals(OBJECT_FIELD_TYPE_CHANGED, deltas.get(0).getCmd());
    }

    @Test
    void compareBothNull_returnsEmptyList() {
        List<GraphComparator.Delta> deltas = GraphComparator.compare(null, null, null);
        assertTrue(deltas.isEmpty());
    }

    @Test
    void compareCyclicGraphs_identical_noDeltas() {
        Node a = new Node(1);
        Node b = new Node(2);
        a.children.add(b);
        // cycle
        b.children.add(a);
        Node aCopy = new Node(1);
        Node bCopy = new Node(2);
        aCopy.children.add(bCopy);
        // identical cycle
        bCopy.children.add(aCopy);
        List<GraphComparator.Delta> deltas = GraphComparator.compare(a, aCopy, idFetcher);
        assertTrue(deltas.isEmpty(), "Identical cyclic graphs should produce no deltas");
    }

    @Test
    void compareCustomListImplementation_reportsListSetElement() {
        MyArrayList<Integer> src = new MyArrayList<>(Arrays.asList(1, 2, 3));
        MyArrayList<Integer> tgt = new MyArrayList<>(Arrays.asList(1, 9, 3));
        List<GraphComparator.Delta> deltas = GraphComparator.compare(src, tgt, null);
        assertEquals(1, deltas.size());
        GraphComparator.Delta d = deltas.get(0);
        assertEquals(LIST_SET_ELEMENT, d.getCmd());
        assertEquals(1, d.getOptionalKey());
    }

    @Test
    void compareWithThrowingIdFetcher_propagatesException() {
        GraphComparator.ID throwingFetcher = object -> {
            throw new IllegalStateException("Fetcher failed");
        };
        Person src = new Person(1L, "Alice");
        Person tgt = new Person(1L, "Bob");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> GraphComparator.compare(src, tgt, throwingFetcher));
        assertEquals("Fetcher failed", ex.getMessage());
    }
}
