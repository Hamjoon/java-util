package com.cedarsoftware.util;

import com.cedarsoftware.util.GraphComparator.Delta;
import com.cedarsoftware.util.GraphComparator.Delta.Command;
import com.cedarsoftware.util.GraphComparator.DeltaProcessor;
import com.cedarsoftware.util.GraphComparator.ID;
import com.cedarsoftware.util.GraphComparator.DeltaError;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphComparator_applyDelta_Test {

    /**
     * ID fetcher that works for any object that has a public {@code id} field.
     */
    private static final ID ID_FETCHER = obj -> {
        try {
            Field f = obj.getClass().getDeclaredField("id");
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    @Mock
    private DeltaProcessor // mocked processor – we verify interactions
    deltaProcessor;

    // the object that will be traversed by applyDelta
    private Node sourceNode;

    // list supplied to applyDelta
    private List<Delta> deltas;

    /**
     * Helper ID fetcher that returns the object itself (identity).
     */
    private static final ID IDENTITY_FETCHER = obj -> obj;

    /**
     * A fresh source object for each test.
     */
    private Person sourcePerson;

    /**
     * Simple POJO that can be identified by an {@code id} field.
     */
    private static class Node {

        long id;

        // field used by OBJECT_ASSIGN_FIELD
        int number;

        // field used by SET_ADD / SET_REMOVE
        Set<String> tags = new HashSet<>();

        // field used by MAP_PUT / MAP_REMOVE
        Map<String, Integer> map = new HashMap<>();

        // field used by LIST_SET_ELEMENT / LIST_RESIZE
        List<String> list = new ArrayList<>();

        // field used by ARRAY_SET_ELEMENT / ARRAY_RESIZE
        String[] array = new String[2];

        Node(long id) {
            this.id = id;
        }
    }

    /**
     * Simple POJO used in many tests.
     */
    private static class Person {

        public String name;

        public int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    class Container {

        int number = 1;

        Set<String> tags = new HashSet<>(Set.of("a"));

        Map<String, Integer> map = new HashMap<>(Map.of("k", 1));

        List<String> list = new ArrayList<>(List.of("x"));

        String[] array = new String[] { "a", "b" };
    }

    class Container {

        int number = 1;

        Set<String> tags = new HashSet<>(Set.of("a"));

        Map<String, Integer> map = new HashMap<>(Map.of("k", 1));

        List<String> list = new ArrayList<>(List.of("x"));

        String[] array = new String[] { "a", "b" };
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh source object for every test
        sourceNode = new Node(42L);
        sourceNode.number = 7;
        sourceNode.tags.add("initial");
        sourceNode.map.put("k", 1);
        sourceNode.list.add("first");
        sourceNode.array[0] = "zero";
        deltas = new ArrayList<>();
        sourcePerson = new Person("Alice", 30);
        deltas = new ArrayList<>();
    }

    /* --------------------------------------------------------------------- */
    private Delta delta(Object id, String fieldName, Command cmd, Object srcValue, Object targetValue, Object optionalKey) {
        Delta d = new Delta(id, fieldName, null, srcValue, targetValue, optionalKey);
        d.setCmd(cmd);
        return d;
    }

    @Test
    void applyDelta_callsProcessorForObjectAssignField() {
        // Arrange – change the value of the field "number"
        Delta d = delta(sourceNode.id, "number", Command.OBJECT_ASSIGN_FIELD, 7, 99, null);
        deltas.add(d);
        // Act
        List<DeltaError> errors = GraphComparator.applyDelta(sourceNode, deltas, ID_FETCHER, deltaProcessor, false);
        // Assert – no errors and the processor method was invoked exactly once
        assertTrue(errors.isEmpty(), () -> "Unexpected errors: " + errors);
        verify(deltaProcessor, times(1)).processObjectAssignField(sourceNode, any(Field.class), eq(d));
    }

    @Test
    void applyDelta_callsProcessorForSetAdd() {
        // Arrange – add a new element to the Set field "tags"
        Delta d = delta(sourceNode.id, "tags", Command.SET_ADD, null, "newTag", null);
        deltas.add(d);
        // Act
        List<DeltaError> errors = GraphComparator.applyDelta(sourceNode, deltas, ID_FETCHER, deltaProcessor, false);
        // Assert
        assertTrue(errors.isEmpty());
        verify(deltaProcessor, times(1)).processSetAdd(sourceNode, any(Field.class), eq(d));
    }

    @Test
    void applyDelta_callsProcessorForMapPut() {
        // Arrange – put a new entry into the Map field "map"
        Delta d = delta(sourceNode.id, "map", Command.MAP_PUT, null, 42, "newKey");
        deltas.add(d);
        // Act
        List<DeltaError> errors = GraphComparator.applyDelta(sourceNode, deltas, ID_FETCHER, deltaProcessor, false);
        // Assert
        assertTrue(errors.isEmpty());
        verify(deltaProcessor, times(1)).processMapPut(sourceNode, any(Field.class), eq(d));
    }

    @Test
    void applyDelta_callsProcessorForListResize() {
        // Arrange – resize the List field "list" to size 3
        Delta d = delta(sourceNode.id, "list", Command.LIST_RESIZE, null, null, 3);
        deltas.add(d);
        // Act
        List<DeltaError> errors = GraphComparator.applyDelta(sourceNode, deltas, ID_FETCHER, deltaProcessor, false);
        // Assert
        assertTrue(errors.isEmpty());
        verify(deltaProcessor, times(1)).processListResize(sourceNode, any(Field.class), eq(d));
    }

    @Test
    void applyDelta_reportsErrorWhenSourceObjectMissing() {
        // Arrange – delta refers to a non‑existent id
        Delta d = delta(999L, "number", Command.OBJECT_ASSIGN_FIELD, 0, 1, null);
        deltas.add(d);
        // Act
        List<DeltaError> errors = GraphComparator.applyDelta(sourceNode, deltas, ID_FETCHER, deltaProcessor, false);
        // Assert
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("source object not found"));
        verifyNoInteractions(deltaProcessor);
    }

    @Test
    void applyDelta_reportsErrorWhenFieldMissing() {
        // Arrange – field name that does not exist on Node
        Delta d = delta(sourceNode.id, "nonExistentField", Command.OBJECT_ASSIGN_FIELD, null, null, null);
        deltas.add(d);
        // Act
        List<DeltaError> errors = GraphComparator.applyDelta(sourceNode, deltas, ID_FETCHER, deltaProcessor, false);
        // Assert
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("field name missing"));
        verifyNoInteractions(deltaProcessor);
    }

    @Test
    void applyDelta_honorsFailFastFlag() {
        // Arrange – first delta succeeds, second fails (missing source)
        Delta good = delta(sourceNode.id, "number", Command.OBJECT_ASSIGN_FIELD, 7, 8, null);
        Delta bad = delta(12345L, "number", Command.OBJECT_ASSIGN_FIELD, 0, 1, null);
        deltas.add(good);
        deltas.add(bad);
        // Act – enable fail‑fast
        List<DeltaError> errors = GraphComparator.applyDelta(sourceNode, deltas, ID_FETCHER, deltaProcessor, true);
        // Assert – processing stops after the first error (i.e., only one error)
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("source object not found"));
        // The first delta should have been processed
        verify(deltaProcessor, times(1)).processObjectAssignField(sourceNode, any(Field.class), eq(good));
    }

    @Test
    void applyDelta_objectOrphanWithMissingField_callsProcessor() {
        // OBJECT_ORPHAN does not require the field to exist.
        Delta d = delta(sourcePerson, "anyFieldDoesNotMatter", Command.OBJECT_ORPHAN, null, null, null);
        deltas.add(d);
        List<DeltaError> errors = GraphComparator.applyDelta(sourcePerson, deltas, IDENTITY_FETCHER, deltaProcessor, false);
        assertTrue(errors.isEmpty(), "No errors should be reported for OBJECT_ORPHAN");
        verify(deltaProcessor, times(1)).processObjectOrphan(sourcePerson, any(Field.class), eq(d));
    }

    @Test
    void applyDelta_allCommands_areDispatchedToProcessor() {
        // Build a node that contains every kind of field used by the commands.
        class Container {

            int number = 1;

            Set<String> tags = new HashSet<>(Set.of("a"));

            Map<String, Integer> map = new HashMap<>(Map.of("k", 1));

            List<String> list = new ArrayList<>(List.of("x"));

            String[] array = new String[] { "a", "b" };
        }
        Container container = new Container();
        // Prepare one delta per command.
        deltas.add(delta(container, "number", Command.OBJECT_ASSIGN_FIELD, 1, 2, null));
        deltas.add(delta(container, null, Command.OBJECT_ORPHAN, null, null, null));
        deltas.add(delta(container, "array", Command.ARRAY_SET_ELEMENT, "a", "z", 0));
        deltas.add(delta(container, "array", Command.ARRAY_RESIZE, null, null, 3));
        deltas.add(delta(container, "tags", Command.SET_ADD, null, "newTag", null));
        deltas.add(delta(container, "tags", Command.SET_REMOVE, "newTag", null, null));
        deltas.add(delta(container, "map", Command.MAP_PUT, null, 42, "newKey"));
        deltas.add(delta(container, "map", Command.MAP_REMOVE, 42, null, "newKey"));
        deltas.add(delta(container, "list", Command.LIST_SET_ELEMENT, null, "elem", 0));
        deltas.add(delta(container, "list", Command.LIST_RESIZE, null, null, 5));
        List<DeltaError> errors = GraphComparator.applyDelta(container, deltas, IDENTITY_FETCHER, deltaProcessor, false);
        assertTrue(errors.isEmpty(), "All commands should succeed without errors");
        // Verify each processor method was invoked exactly once with the correct arguments.
        verify(deltaProcessor, times(1)).processObjectAssignField(eq(container), any(Field.class), eq(deltas.get(0)));
        verify(deltaProcessor, times(1)).processObjectOrphan(eq(container), any(Field.class), eq(deltas.get(1)));
        verify(deltaProcessor, times(1)).processArraySetElement(eq(container), any(Field.class), eq(deltas.get(2)));
        verify(deltaProcessor, times(1)).processArrayResize(eq(container), any(Field.class), eq(deltas.get(3)));
        verify(deltaProcessor, times(1)).processSetAdd(eq(container), any(Field.class), eq(deltas.get(4)));
        verify(deltaProcessor, times(1)).processSetRemove(eq(container), any(Field.class), eq(deltas.get(5)));
        verify(deltaProcessor, times(1)).processMapPut(eq(container), any(Field.class), eq(deltas.get(6)));
        verify(deltaProcessor, times(1)).processMapRemove(eq(container), any(Field.class), eq(deltas.get(7)));
        verify(deltaProcessor, times(1)).processListSetElement(eq(container), any(Field.class), eq(deltas.get(8)));
        verify(deltaProcessor, times(1)).processListResize(eq(container), any(Field.class), eq(deltas.get(9)));
    }

    @Test
    void applyDelta_processorThrowsException_errorCaptured() {
        // Configure mock to throw when processing OBJECT_ASSIGN_FIELD.
        Delta d = delta(sourcePerson, "name", Command.OBJECT_ASSIGN_FIELD, "Alice", "Bob", null);
        deltas.add(d);
        doThrow(new RuntimeException("boom")).when(deltaProcessor).processObjectAssignField(eq(sourcePerson), any(Field.class), eq(d));
        List<DeltaError> errors = GraphComparator.applyDelta(sourcePerson, deltas, IDENTITY_FETCHER, deltaProcessor, false);
        assertEquals(1, errors.size(), "One error should be captured");
        assertTrue(errors.get(0).getMessage().contains("boom"), "Error message should contain the exception text");
        // Verify that the processor method was indeed invoked.
        verify(deltaProcessor, times(1)).processObjectAssignField(eq(sourcePerson), any(Field.class), eq(d));
    }

    @Test
    void applyDelta_nullSource_throwsNullPointerException() {
        // Traverser.traverse will dereference the source argument, leading to NPE.
        assertThrows(NullPointerException.class, () -> GraphComparator.applyDelta(null, Collections.emptyList(), IDENTITY_FETCHER, deltaProcessor, false));
    }

    @Test
    void applyDelta_emptyCommandList_returnsEmptyErrorList() {
        List<DeltaError> errors = GraphComparator.applyDelta(sourcePerson, Collections.emptyList(), IDENTITY_FETCHER, deltaProcessor, false);
        assertTrue(errors.isEmpty(), "No errors should be returned for an empty delta list");
    }

    @Test
    void applyDelta_nullDeltaProcessor_throwsNullPointerException() {
        Delta d = delta(sourcePerson, "name", Command.OBJECT_ASSIGN_FIELD, "Alice", "Bob", null);
        deltas.add(d);
        // The processor argument is null – the first command will cause an NPE.
        assertThrows(NullPointerException.class, () -> GraphComparator.applyDelta(sourcePerson, deltas, IDENTITY_FETCHER, null, false));
    }

    @Test
    void applyDelta_deltaWithNullCommand_reportsUnknownCommandError() {
        Delta d = delta(sourcePerson, "name", null, "Alice", "Bob", null);
        deltas.add(d);
        List<DeltaError> errors = GraphComparator.applyDelta(sourcePerson, deltas, IDENTITY_FETCHER, deltaProcessor, false);
        assertEquals(1, errors.size(), "One error expected for null command");
        assertTrue(errors.get(0).getMessage().contains("Unknown command: null"), "Error message should indicate unknown command");
        verifyNoInteractions(deltaProcessor);
    }

    @Test
    void applyDelta_nullFieldNameForNonOrphan_reportsFieldMissingError() {
        Delta d = delta(sourcePerson, null, Command.OBJECT_ASSIGN_FIELD, "Alice", "Bob", null);
        deltas.add(d);
        List<DeltaError> errors = GraphComparator.applyDelta(sourcePerson, deltas, IDENTITY_FETCHER, deltaProcessor, false);
        assertEquals(1, errors.size(), "One error expected for missing field name");
        assertTrue(errors.get(0).getMessage().contains("field name missing: null"), "Error message should mention missing field name");
        verifyNoInteractions(deltaProcessor);
    }

    @Test
    void applyDelta_failFastVarargsOnlyFirstElementIsConsidered() {
        // First delta fails (missing source), second would also fail but should not be processed.
        Delta bad1 = delta(999L, "name", Command.OBJECT_ASSIGN_FIELD, null, "Bob", null);
        Delta bad2 = delta(sourcePerson, "age", Command.OBJECT_ASSIGN_FIELD, 30, 31, null);
        deltas.addAll(List.of(bad1, bad2));
        // Provide two booleans – only the first (true) matters.
        List<DeltaError> errors = GraphComparator.applyDelta(sourcePerson, deltas, IDENTITY_FETCHER, deltaProcessor, true, false);
        assertEquals(1, errors.size(), "Processing should stop after the first error");
        assertTrue(errors.get(0).getMessage().contains("source object not found"));
        // Verify that the second delta never reached the processor.
        verify(deltaProcessor, never()).processObjectAssignField(eq(sourcePerson), any(Field.class), eq(bad2));
    }

    @Test
    void applyDelta_nullIdFetcher_throwsNullPointerExceptionDuringTraversal() {
        // Traverser.traverse will attempt to call idFetcher.getId and NPE.
        assertThrows(NullPointerException.class, () -> GraphComparator.applyDelta(sourcePerson, Collections.emptyList(), null, deltaProcessor, false));
    }
}
