package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.InaccessibleObjectException;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Traverser_traverse_Test {

    /**
     * Mocked visitor that will receive callbacks from {@link Traverser#traverse}.
     */
    @Mock
    Traverser.Visitor visitorMock;

    /**
     * Simple POJO used to build test graphs.
     */
    static class Node {

        String name;

        Node child;

        Node(String name) {
            this.name = name;
        }
    }

    /**
     * POJO with private fields – used for the “Custom Object Fields” test.
     */
    static class Person {

        private final String name;

        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /**
     * Helper visitor that records every visited object (and its order).
     */
    static class RecordingVisitor implements Traverser.Visitor {

        private final List<Object> visited = new ArrayList<>();

        @Override
        public void visit(Object o) {
            visited.add(o);
        }

        List<Object> getVisited() {
            return visited;
        }
    }

    /**
     * Visitor that throws a RuntimeException when a specific object is visited.
     */
    static class ThrowingVisitor implements Traverser.Visitor {

        private final Object trigger;

        ThrowingVisitor(Object trigger) {
            this.trigger = trigger;
        }

        @Override
        public void visit(Object o) {
            if (Objects.equals(o, trigger)) {
                throw new RuntimeException("Visitor forced exception");
            }
        }
    }

    /**
     * Visitor that throws an AssertionError (an {@code Error}) for a specific object.
     */
    static class ThrowingErrorVisitor implements Traverser.Visitor {

        private final Object trigger;

        ThrowingErrorVisitor(Object trigger) {
            this.trigger = trigger;
        }

        @Override
        public void visit(Object o) {
            if (Objects.equals(o, trigger)) {
                throw new AssertionError("Visitor forced error");
            }
        }
    }

    /**
     * Visitor that mutates a collection while it is being traversed.
     */
    static class ModifyingVisitor implements Traverser.Visitor {

        private final List<Integer> target;

        ModifyingVisitor(List<Integer> target) {
            this.target = target;
        }

        @Override
        public void visit(Object o) {
            // Remove the first element when we encounter the integer 2
            if (Integer.valueOf(2).equals(o)) {
                target.remove(0);
            }
        }
    }

    /**
     * Visitor that mutates a map while it is being traversed.
     */
    static class ModifyingMapVisitor implements Traverser.Visitor {

        private final Map<String, Integer> target;

        ModifyingMapVisitor(Map<String, Integer> target) {
            this.target = target;
        }

        @Override
        public void visit(Object o) {
            // When we encounter the key "a", add a new entry
            if ("a".equals(o)) {
                target.put("c", 3);
            }
        }
    }

    /**
     * Helper class with a private field used for the security‑manager test.
     */
    static class PrivateFieldHolder {

        private final String secret;

        PrivateFieldHolder(String secret) {
            this.secret = secret;
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        clearInvocations(visitorMock);
    }

    /**
     * Verify that a simple, non‑null root object is visited exactly once.
     */
    @Test
    void traverse_visitsRootObjectOnce() {
        // GIVEN
        String root = "root";
        // WHEN
        Traverser.traverse(root, visitorMock);
        // THEN
        // The visitor should be called exactly once with the root object.
        verify(visitorMock, times(1)).visit(root);
        verifyNoMoreInteractions(visitorMock);
    }

    /**
     * Verify that {@code null} passed as the root does not result in any visitor calls.
     */
    @Test
    void traverse_nullRootDoesNotInvokeVisitor() {
        // GIVEN
        Object root = null;
        // WHEN
        Traverser.traverse(root, visitorMock);
        // THEN
        verifyNoInteractions(visitorMock);
    }

    /**
     * Verify that a graph containing multiple objects is fully traversed.
     * The graph is:
     *
     *   parent -> child -> grandChild
     *
     * The visitor should be invoked for each distinct object, including the root.
     */
    @Test
    void traverse_visitsAllObjectsInAcyclicGraph() {
        // GIVEN
        Node grandChild = new Node("grandChild");
        Node child = new Node("child");
        child.child = grandChild;
        Node parent = new Node("parent");
        parent.child = child;
        // WHEN
        Traverser.traverse(parent, visitorMock);
        // THEN
        // Visitor should be called once for each node (parent, child, grandChild)
        verify(visitorMock, times(1)).visit(parent);
        verify(visitorMock, times(1)).visit(child);
        verify(visitorMock, times(1)).visit(grandChild);
        verifyNoMoreInteractions(visitorMock);
    }

    /**
     * Verify that cycles are detected and do not cause infinite recursion.
     * The graph is:
     *
     *   a -> b -> a (cycle)
     *
     * The visitor should be called exactly once for each distinct object.
     */
    @Test
    void traverse_handlesCyclicGraphWithoutRepeatingVisits() {
        // GIVEN
        Node a = new Node("A");
        Node b = new Node("B");
        a.child = b;
        // creates a cycle
        b.child = a;
        // WHEN
        Traverser.traverse(a, visitorMock);
        // THEN
        // Both nodes should be visited exactly once.
        verify(visitorMock, times(1)).visit(a);
        verify(visitorMock, times(1)).visit(b);
        verifyNoMoreInteractions(visitorMock);
    }

    /**
     * Verify that the traversal works with collections (List) as root.
     * The visitor should be invoked for the collection itself and for each
     * element contained in it.
     */
    @Test
    void traverse_visitsCollectionAndItsElements() {
        // GIVEN
        List<String> list = new ArrayList<>(Arrays.asList("one", "two", "three"));
        // WHEN
        Traverser.traverse(list, visitorMock);
        // THEN
        // Visitor should be called for the list instance and each string element.
        verify(visitorMock, times(1)).visit(list);
        for (String s : list) {
            verify(visitorMock, times(1)).visit(s);
        }
        verifyNoMoreInteractions(visitorMock);
    }

    @Test
    void traverse_nullVisitorThrowsNPE() {
        Object root = new Object();
        assertThrows(NullPointerException.class, () -> Traverser.traverse(root, null));
    }

    @Test
    void traverse_skipArrayContainsNullThrowsNPE() {
        Object root = new Object();
        Class<?>[] skip = new Class<?>[] { null };
        assertThrows(NullPointerException.class, () -> Traverser.traverse(root, skip, visitorMock));
    }

    @Test
    void traverse_visitsSingleIntegerOnce() {
        Integer value = Integer.valueOf(42);
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(value, rec);
        assertEquals(1, rec.getVisited().size());
        assertSame(value, rec.getVisited().get(0));
    }

    @Test
    void traverse_visitsSingleStringOnce() {
        String value = "hello world";
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(value, rec);
        assertEquals(1, rec.getVisited().size());
        assertSame(value, rec.getVisited().get(0));
    }

    @Test
    void traverse_visitsPrimitiveIntArrayAndElements() {
        int[] array = new int[] { 1, 2, 3 };
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(array, rec);
        // 1 call for the array itself + 3 calls for each element (autoboxed)
        assertEquals(4, rec.getVisited().size());
        assertSame(array, rec.getVisited().get(0));
        assertTrue(rec.getVisited().subList(1, 4).containsAll(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3))));
    }

    @Test
    void traverse_visitsObjectArrayAndElements() {
        String[] array = new String[] { "a", "b" };
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(array, rec);
        // 1 call for the array itself + 2 calls for the elements
        assertEquals(3, rec.getVisited().size());
        assertSame(array, rec.getVisited().get(0));
        assertTrue(rec.getVisited().subList(1, 3).containsAll(Arrays.asList("a", "b")));
    }

    @Test
    void traverse_visitsListAndElements() {
        List<Integer> list = Arrays.asList(10, 20, 30);
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(list, rec);
        // 1 for the list itself + 3 for its elements
        assertEquals(4, rec.getVisited().size());
        assertSame(list, rec.getVisited().get(0));
        assertTrue(rec.getVisited().subList(1, 4).containsAll(list));
    }

    @Test
    void traverse_visitsSetAndElements() {
        Set<String> set = new HashSet<>(Arrays.asList("x", "y"));
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(set, rec);
        // 1 for the set itself + 2 for its elements (order not guaranteed)
        assertEquals(3, rec.getVisited().size());
        assertSame(set, rec.getVisited().get(0));
        assertTrue(set.containsAll(rec.getVisited().subList(1, 3)));
    }

    @Test
    void traverse_visitsSingletonMapKeyAndValue() {
        Map<String, Integer> map = Collections.singletonMap("key", 99);
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(map, rec);
        // Expected visits: map, key, value
        assertEquals(3, rec.getVisited().size());
        assertSame(map, rec.getVisited().get(0));
        assertTrue(rec.getVisited().containsAll(Arrays.asList("key", 99)));
    }

    @Test
    void traverse_visitsComplexMapWithMixedValues() {
        Map<String, Object> map = new HashMap<String, Object>() {

            {
                put("a", new int[] { 1, 2 });
                put("b", Arrays.asList("x", "y"));
            }
        };
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(map, rec);
        // Minimum expected visits:
        // map, "a", int[] container, 1, 2, "b", List container, "x", "y"
        // (total 9). We assert at least 9 to guarantee deep traversal.
        assertTrue(rec.getVisited().size() >= 9);
        assertTrue(rec.getVisited().contains(map));
        assertTrue(rec.getVisited().contains("a"));
        assertTrue(rec.getVisited().contains("b"));
    }

    @Test
    void traverse_respectsSkipClasses() {
        List<Object> list = Arrays.asList(new Date(), "text");
        Class<?>[] skip = new Class<?>[] { Date.class };
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(list, skip, rec);
        // The list itself and the String element should be visited; the Date should be skipped.
        assertEquals(2, rec.getVisited().size());
        assertSame(list, rec.getVisited().get(0));
        assertEquals("text", rec.getVisited().get(1));
    }

    @Test
    void traverse_detectsAndHandlesCycles() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.child = b;
        // creates a cycle
        b.child = a;
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(a, rec);
        // Both nodes must be visited exactly once.
        assertEquals(2, rec.getVisited().size());
        assertTrue(rec.getVisited().containsAll(Arrays.asList(a, b)));
    }

    @Test
    void traverse_visitsPrivateFieldsOfPojo() {
        Person person = new Person("Alice", 30);
        RecordingVisitor rec = new RecordingVisitor();
        Traverser.traverse(person, rec);
        // Expect the Person instance, its name and its age to be visited.
        assertTrue(rec.getVisited().contains(person));
        assertTrue(rec.getVisited().contains("Alice"));
        assertTrue(rec.getVisited().contains(30));
    }

    @Test
    void traverse_propagatesRuntimeExceptionFromVisitor() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        ThrowingVisitor throwingVisitor = new ThrowingVisitor(2);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> Traverser.traverse(list, throwingVisitor));
        assertEquals("Visitor forced exception", ex.getMessage());
    }

    @Test
    void traverse_propagatesErrorFromVisitor() {
        List<String> list = Collections.singletonList("test");
        ThrowingErrorVisitor errorVisitor = new ThrowingErrorVisitor("test");
        AssertionError err = assertThrows(AssertionError.class, () -> Traverser.traverse(list, errorVisitor));
        assertEquals("Visitor forced error", err.getMessage());
    }

    @Test
    void traverse_detectsConcurrentModificationInCollection() {
        List<Integer> mutable = new ArrayList<>(Arrays.asList(1, 2, 3));
        ModifyingVisitor modVisitor = new ModifyingVisitor(mutable);
        assertThrows(ConcurrentModificationException.class, () -> Traverser.traverse(mutable, modVisitor));
    }

    @Test
    void traverse_detectsConcurrentModificationInMap() {
        Map<String, Integer> map = new HashMap<String, Integer>() {

            {
                put("a", 1);
                put("b", 2);
            }
        };
        ModifyingMapVisitor modMapVisitor = new ModifyingMapVisitor(map);
        assertThrows(ConcurrentModificationException.class, () -> Traverser.traverse(map, modMapVisitor));
    }

    @Test
    void traverse_privateFieldAccessMayThrowInaccessibleObjectException() {
        // This test attempts to provoke an InaccessibleObjectException by
        // accessing a private field in a sealed module environment.
        // In most JVMs (without a restrictive SecurityManager) the traversal
        // succeeds, so we only assert that the exception, if thrown, is of the
        // expected type.
        PrivateFieldHolder holder = new PrivateFieldHolder("secret");
        RecordingVisitor rec = new RecordingVisitor();
        try {
            Traverser.traverse(holder, rec);
            // If no exception, verify that the holder itself was visited.
            assertTrue(rec.getVisited().contains(holder));
        } catch (InaccessibleObjectException e) {
            // Expected in a strict module / security manager setup.
            assertTrue(e.getMessage().contains("secret"));
        }
    }
}
