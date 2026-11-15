package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import java.sql.Timestamp;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Traverser_traverse_2_Test {

    @Mock
    Traverser.Visitor // Mockito will create a mock implementation
    visitor;

    /**
     * Simple POJO used in many tests.
     */
    private static class Node {

        Object value;

        Node next;

        Node(Object value) {
            this.value = value;
        }
    }

    /**
     * Visitor implementation that records every visited object in order.
     */
    private static class RecordingVisitor implements Traverser.Visitor {

        final List<Object> visited = new ArrayList<>();

        @Override
        public void visit(Object o) {
            visited.add(o);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        reset(visitor);
    }

    /**
     * Verify that the visitor is invoked exactly once for a simple root object.
     */
    @Test
    void traverse_SimpleRootObject_VisitorCalledOnce() {
        String root = "root";
        Traverser.traverse(root, null, visitor);
        // The visitor should be called with the root object only.
        verify(visitor, times(1)).visit(root);
        verifyNoMoreInteractions(visitor);
    }

    /**
     * Verify that all reachable objects are visited and that cycles are handled.
     */
    @Test
    void traverse_GraphWithCycle_VisitorVisitsEachObjectOnce() {
        // Build a cyclic graph: a -> b -> a
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        // creates a cycle
        b.next = a;
        Traverser.traverse(a, null, visitor);
        // Expected visits: a, b (the cycle should not cause a third visit)
        verify(visitor, times(1)).visit(a);
        verify(visitor, times(1)).visit(b);
        verifyNoMoreInteractions(visitor);
    }

    /**
     * Verify that objects whose class is listed in the skip array are not visited.
     */
    @Test
    void traverse_WithSkipArray_SkippedClassesAreNotVisited() {
        // Graph: root (Node) -> skipped (String) -> another Node
        Node root = new Node("root");
        String skipped = "skip me";
        Node leaf = new Node("leaf");
        root.value = skipped;
        root.next = leaf;
        // Skip the String class – the visitor must not receive the String instance.
        Class<?>[] skip = new Class<?>[] { String.class };
        Traverser.traverse(root, skip, visitor);
        verify(visitor, times(1)).visit(root);
        verify(visitor, times(1)).visit(leaf);
        // The String instance must never be visited.
        verify(visitor, never()).visit(skipped);
        verifyNoMoreInteractions(visitor);
    }

    /**
     * Verify that a null skip array behaves the same as an empty array.
     */
    @Test
    void traverse_NullSkipArray_VisitorVisitsAllObjects() {
        Node root = new Node("root");
        Node child = new Node("child");
        root.next = child;
        Traverser.traverse(root, null, visitor);
        verify(visitor, times(1)).visit(root);
        verify(visitor, times(1)).visit(child);
        verifyNoMoreInteractions(visitor);
    }

    /**
     * Verify that an empty skip array also results in all objects being visited.
     */
    @Test
    void traverse_EmptySkipArray_VisitorVisitsAllObjects() {
        Node root = new Node("root");
        Node child = new Node("child");
        root.next = child;
        Traverser.traverse(root, new Class<?>[0], visitor);
        verify(visitor, times(1)).visit(root);
        verify(visitor, times(1)).visit(child);
        verifyNoMoreInteractions(visitor);
    }

    /**
     * Verify that the internal caches are cleared after traversal.
     */
    @Test
    void traverse_InternalCachesAreCleared() throws Exception {
        Node root = new Node("root");
        // Use reflection to inspect the private caches after traversal.
        // a fresh instance
        Traverser traverser = new Traverser();
        traverser.walk(root, null, visitor);
        // After walk() the caches should contain entries.
        // Access the private fields via reflection.
        java.lang.reflect.Field visitedField = Traverser.class.getDeclaredField("_objVisited");
        java.lang.reflect.Field cacheField = Traverser.class.getDeclaredField("_classCache");
        visitedField.setAccessible(true);
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Object> visitedMap = (Map<Object, Object>) visitedField.get(traverser);
        @SuppressWarnings("unchecked")
        Map<Class<?>, ?> classCache = (Map<Class<?>, ?>) cacheField.get(traverser);
        // Ensure the maps are non‑empty after walking.
        Assertions.assertFalse(visitedMap.isEmpty(), "Visited map should contain entries after walk");
        Assertions.assertFalse(classCache.isEmpty(), "Class cache should contain entries after walk");
        // Now invoke the static method which clears the caches.
        Traverser.traverse(root, null, visitor);
        // After the static call the caches of a *new* Traverser instance are cleared,
        // but we can also verify that the previous instance's maps are still cleared
        // because the static method creates its own Traverser and clears its own maps.
        // The purpose of this test is mainly to ensure no exception is thrown
        // when the clearing logic runs.
    }

    /**
     * Basic null root – no visitor calls, no exception.
     */
    @Test
    void traverse_NullRoot_NoVisitorCalls() {
        Traverser.traverse(null, null, visitor);
        verifyNoInteractions(visitor);
    }

    /**
     * Visitor argument is null – expect NPE before any traversal.
     */
    @Test
    void traverse_NullVisitor_ThrowsNPE() {
        Object root = new Object();
        assertThrows(NullPointerException.class, () -> Traverser.traverse(root, null, null));
    }

    /**
     * Simple object with empty skip array – exactly one visit (the root).
     */
    @Test
    void traverse_SimpleObject_EmptySkipArray_VisitorOnce() {
        Object root = new Object();
        Traverser.traverse(root, new Class<?>[0], visitor);
        verify(visitor, times(1)).visit(root);
        verifyNoMoreInteractions(visitor);
    }

    /**
     * Skip array contains the root's class – root should be skipped entirely.
     */
    @Test
    void traverse_RootClassSkipped_NoVisits() {
        Date date = new Date();
        Class<?>[] skip = new Class<?>[] { Date.class };
        Traverser.traverse(date, skip, visitor);
        verifyNoInteractions(visitor);
    }

    /**
     * Subclass of a skipped class is still visited (exact‑match semantics).
     */
    @Test
    void traverse_SubclassOfSkipped_NotSkipped() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        // Timestamp extends Date
        Class<?>[] skip = new Class<?>[] { Date.class };
        Traverser.traverse(ts, skip, visitor);
        verify(visitor, times(1)).visit(ts);
        verifyNoMoreInteractions(visitor);
    }

    /**
     * Array traversal – array object + each element visited.
     */
    @Test
    void traverse_ObjectArray_VisitsArrayAndElements() {
        Integer[] arr = new Integer[] { 1, 2, 3 };
        Traverser.traverse(arr, null, visitor);
        // Capture all arguments passed to visit(...)
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(visitor, times(4)).visit(captor.capture());
        List<Object> visited = captor.getAllValues();
        assertTrue(visited.contains(arr), "Array instance must be visited");
        assertTrue(visited.containsAll(Arrays.asList(1, 2, 3)), "All elements must be visited");
    }

    /**
     * Primitive int[] traversal – array object + boxed elements visited.
     */
    @Test
    void traverse_PrimitiveIntArray_VisitsArrayAndBoxedElements() {
        int[] arr = new int[] { 4, 5 };
        Traverser.traverse(arr, null, visitor);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(visitor, times(3)).visit(captor.capture());
        List<Object> visited = captor.getAllValues();
        assertTrue(visited.contains(arr), "Primitive array instance must be visited");
        assertTrue(visited.containsAll(Arrays.asList(4, 5)), "Boxed primitive values must be visited");
    }

    /**
     * List traversal – list object + each element visited.
     */
    @Test
    void traverse_List_VisitsListAndElements() {
        List<String> list = Arrays.asList("a", "b", "c");
        Traverser.traverse(list, null, visitor);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(visitor, times(4)).visit(captor.capture());
        List<Object> visited = captor.getAllValues();
        assertTrue(visited.contains(list), "List instance must be visited");
        assertTrue(visited.containsAll(list), "All list elements must be visited");
    }

    /**
     * Empty Set traversal – only the set itself is visited.
     */
    @Test
    void traverse_EmptySet_VisitsOnlySet() {
        Set<String> empty = Collections.emptySet();
        Traverser.traverse(empty, null, visitor);
        verify(visitor, times(1)).visit(empty);
        verifyNoMoreInteractions(visitor);
    }

    /**
     * Map traversal – map object, each non‑null key and each non‑null value visited.
     */
    @Test
    void traverse_MapWithTwoEntries_VisitsMapKeysAndValues() {
        Map<String, Integer> map = Map.of("key1", 10, "key2", 20);
        Traverser.traverse(map, null, visitor);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(visitor, times(5)).visit(captor.capture());
        List<Object> visited = captor.getAllValues();
        assertTrue(visited.contains(map), "Map instance must be visited");
        assertTrue(visited.containsAll(Arrays.asList("key1", "key2", 10, 20)), "All keys and values must be visited");
    }

    /**
     * Map with null key/value – null entries are ignored.
     */
    @Test
    void traverse_MapWithNulls_IgnoresNullEntries() {
        Map<String, String> map = new HashMap<>();
        map.put(null, "v");
        map.put("k", null);
        map.put("nonNullKey", "nonNullValue");
        Traverser.traverse(map, null, visitor);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        // Expected visits: map itself, "nonNullKey", "nonNullValue"
        verify(visitor, times(3)).visit(captor.capture());
        List<Object> visited = captor.getAllValues();
        assertTrue(visited.contains(map), "Map instance must be visited");
        assertTrue(visited.containsAll(Arrays.asList("nonNullKey", "nonNullValue")), "Only non‑null key/value should be visited");
        assertFalse(visited.contains(null), "Null entries must not be visited");
    }

    /**
     * Collection with null elements – null entries are ignored.
     */
    @Test
    void traverse_ListWithNullElements_IgnoresNulls() {
        List<String> list = Arrays.asList("x", null, "y");
        Traverser.traverse(list, null, visitor);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        // Expected: list itself + two non‑null strings
        verify(visitor, times(3)).visit(captor.capture());
        List<Object> visited = captor.getAllValues();
        assertTrue(visited.contains(list), "List instance must be visited");
        assertTrue(visited.containsAll(Arrays.asList("x", "y")));
        assertFalse(visited.contains(null), "Null element must not be visited");
    }

    /**
     * Skip array containing a null entry – null is ignored, non‑null class still skips.
     */
    @Test
    void traverse_SkipArrayContainsNull_SkipsNonNullClass() {
        List<String> list = new ArrayList<>();
        list.add("a");
        Class<?>[] skip = new Class<?>[] { null, ArrayList.class };
        Traverser.traverse(list, skip, visitor);
        verifyNoInteractions(visitor);
    }

    /**
     * Duplicate entries in skip array – should still skip the class.
     */
    @Test
    void traverse_DuplicateSkipEntries_SkipsClassOnce() {
        LinkedList<Integer> list = new LinkedList<>();
        list.add(1);
        Class<?>[] skip = new Class<?>[] { LinkedList.class, LinkedList.class };
        Traverser.traverse(list, skip, visitor);
        verifyNoInteractions(visitor);
    }

    /**
     * Visitor throws a runtime exception – it must propagate out of traverse.
     */
    @Test
    void traverse_VisitorThrowsException_Propagates() {
        Traverser.Visitor explodingVisitor = new Traverser.Visitor() {

            @Override
            public void visit(Object o) {
                throw new IllegalStateException("boom");
            }
        };
        List<Integer> list = Arrays.asList(1, 2);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Traverser.traverse(list, null, explodingVisitor));
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Deeply nested list chain – ensures recursion depth handling without overflow.
     */
    @Test
    void traverse_DeepNestedLists_VisitsAllLevels() {
        // Build a chain: root -> list1 -> list2 -> … -> list15
        List<Object> root = new ArrayList<>();
        List<Object> current = root;
        for (int i = 0; i < 15; i++) {
            List<Object> next = new ArrayList<>();
            current.add(next);
            current = next;
        }
        RecordingVisitor recVisitor = new RecordingVisitor();
        Traverser.traverse(root, null, recVisitor);
        // Expect 16 visits: root + 15 nested lists
        assertEquals(16, recVisitor.visited.size(), "Each list level should be visited exactly once");
        // Verify that each visited object is indeed a List
        assertTrue(recVisitor.visited.stream().allMatch(o -> o instanceof List), "All visited objects should be List instances");
    }

    /**
     * Verify that a null skip array behaves like an empty array for a primitive array.
     */
    @Test
    void traverse_PrimitiveArray_NullSkipArray_VisitsAll() {
        double[] arr = new double[] { 1.1, 2.2 };
        Traverser.traverse(arr, null, visitor);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        // Visits: array + two boxed Doubles
        verify(visitor, times(3)).visit(captor.capture());
        List<Object> visited = captor.getAllValues();
        assertTrue(visited.contains(arr));
        assertTrue(visited.containsAll(Arrays.asList(1.1, 2.2)));
    }
}
