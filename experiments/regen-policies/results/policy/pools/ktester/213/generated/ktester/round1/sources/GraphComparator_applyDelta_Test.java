package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import static com.cedarsoftware.util.GraphComparator.Delta.Command.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphComparator_applyDelta_Test {

    /**
     * ID fetcher that treats every non‑null object as an identifiable node.
     */
    private GraphComparator.ID idFetcher;

    /**
     * Mocked processor that records which command methods are invoked.
     */
    @Mock
    private GraphComparator.DeltaProcessor deltaProcessor;

    private Sample source;

    /**
     * Simple POJO used as the source graph for the tests.
     */
    private static class Sample {

        int[] numbers = new int[3];

        List<String> names = new ArrayList<>(Arrays.asList("a", "b"));

        Map<String, Integer> map = new HashMap<>();

        Set<String> set = new HashSet<>();

        Sample() {
            map.put("one", 1);
            set.add("x");
        }
    }

    /**
     * POJO used only for the “null command” test – it carries an id field.
     */
    private static class SampleObject {

        final int id;

        String someField;

        SampleObject(int id) {
            this.id = id;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh source for each test
        source = new Sample();
        // ID fetcher returns the object itself (non‑null) – this makes the
        // Traverser treat the object as an ID‑object.
        // simple identity fetcher
        idFetcher = object -> object;
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test.
        reset(deltaProcessor);
    }

    @Test
    void applyDelta_arraySetElement_callsProcessor() {
        // GIVEN
        GraphComparator.Delta delta = new GraphComparator.Delta(// id – the source object itself
        source, // field name
        "numbers", // srcPtr (irrelevant for processing)
        "-root-", // srcValue (unused)
        null, // targetValue (unused)
        null, // optionalKey = index to set
        1);
        delta.setCmd(ARRAY_SET_ELEMENT);
        // WHEN
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, Collections.singletonList(delta), idFetcher, deltaProcessor);
        // THEN
        assertTrue(errors.isEmpty(), "No errors should be reported");
        // Verify the correct processor method was called with the expected arguments.
        verify(deltaProcessor, times(1)).processArraySetElement(eq(source), any(Field.class), eq(delta));
    }

    @Test
    void applyDelta_unknownCommand_reportsError() {
        // GIVEN
        GraphComparator.Delta delta = new GraphComparator.Delta(source, "names", "-root-", null, null, null);
        // forces the default branch in the switch
        delta.setCmd(null);
        // WHEN
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, Collections.singletonList(delta), idFetcher, deltaProcessor);
        // THEN
        assertEquals(1, errors.size(), "Exactly one error expected");
        assertTrue(errors.get(0).getMessage().contains("Unknown command"), "Error message should mention unknown command");
        // No processor interaction should happen.
        verifyNoInteractions(deltaProcessor);
    }

    @Test
    void applyDelta_sourceObjectNotFound_reportsError() {
        // GIVEN
        // not part of the source graph
        Object unknownId = new Object();
        GraphComparator.Delta delta = new GraphComparator.Delta(unknownId, "numbers", "-root-", null, null, 0);
        delta.setCmd(ARRAY_SET_ELEMENT);
        // WHEN
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, Collections.singletonList(delta), idFetcher, deltaProcessor);
        // THEN
        assertEquals(1, errors.size(), "One error expected for missing source object");
        assertTrue(errors.get(0).getMessage().contains("source object not found"), "Error message should mention missing source object");
        verifyNoInteractions(deltaProcessor);
    }

    @Test
    void applyDelta_failFastStopsAfterFirstError() {
        // GIVEN – first delta will fail (unknown command), second would succeed.
        GraphComparator.Delta badDelta = new GraphComparator.Delta(source, "map", "-root-", null, null, null);
        // forces error
        badDelta.setCmd(null);
        GraphComparator.Delta goodDelta = new GraphComparator.Delta(source, "set", "-root-", null, null, "newValue");
        goodDelta.setCmd(SET_ADD);
        List<GraphComparator.Delta> deltas = Arrays.asList(badDelta, goodDelta);
        // WHEN
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher, deltaProcessor, // failFast = true
        true);
        // THEN
        assertEquals(1, errors.size(), "Fail‑fast should stop after the first error");
        assertTrue(errors.get(0).getMessage().contains("Unknown command"), "First error should be about the unknown command");
        // The good delta must not be processed.
        verify(deltaProcessor, never()).processSetAdd(any(), any(), any());
    }

    @Test
    void applyDelta_multipleCommandsAllSucceed() {
        // GIVEN – a mix of commands that should all be processed without error.
        GraphComparator.Delta arrayDelta = new GraphComparator.Delta(source, "numbers", "-root-", null, null, 2);
        arrayDelta.setCmd(ARRAY_SET_ELEMENT);
        GraphComparator.Delta listDelta = new GraphComparator.Delta(source, "names", "-root-", null, null, 1);
        listDelta.setCmd(LIST_SET_ELEMENT);
        GraphComparator.Delta mapDelta = new GraphComparator.Delta(source, "map", "-root-", null, null, "two");
        mapDelta.setCmd(MAP_PUT);
        GraphComparator.Delta setDelta = new GraphComparator.Delta(source, "set", "-root-", null, null, "y");
        setDelta.setCmd(SET_ADD);
        List<GraphComparator.Delta> deltas = Arrays.asList(arrayDelta, listDelta, mapDelta, setDelta);
        // WHEN
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher, deltaProcessor);
        // THEN
        assertTrue(errors.isEmpty(), "All commands should succeed, no errors returned");
        verify(deltaProcessor, times(1)).processArraySetElement(eq(source), any(Field.class), eq(arrayDelta));
        verify(deltaProcessor, times(1)).processListSetElement(eq(source), any(Field.class), eq(listDelta));
        verify(deltaProcessor, times(1)).processMapPut(eq(source), any(Field.class), eq(mapDelta));
        verify(deltaProcessor, times(1)).processSetAdd(eq(source), any(Field.class), eq(setDelta));
    }

    @Test
    void testApplyDelta_NullSource_ThrowsException() {
        // GIVEN
        // identity fetcher
        GraphComparator.ID idFetcher = o -> o;
        List<GraphComparator.Delta> commands = Collections.emptyList();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> GraphComparator.applyDelta(// source is null
        null, commands, idFetcher, GraphComparator.getJavaDeltaProcessor()), "Calling applyDelta with a null source should throw NullPointerException");
    }

    @Test
    void testApplyDelta_NullCommands_ThrowsException() {
        // GIVEN
        // identity fetcher
        GraphComparator.ID idFetcher = o -> o;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> GraphComparator.applyDelta(new Object(), // commands list is null
        null, idFetcher, GraphComparator.getJavaDeltaProcessor()), "Passing a null commands list should throw NullPointerException");
    }

    @Test
    void testApplyDelta_NullIdFetcher_ThrowsException() {
        // GIVEN
        List<GraphComparator.Delta> commands = Collections.emptyList();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> GraphComparator.applyDelta(new Object(), commands, // idFetcher is null
        null, GraphComparator.getJavaDeltaProcessor()), "A null ID fetcher should cause a NullPointerException");
    }

    @Test
    void testApplyDelta_NullDeltaProcessor_ThrowsException() {
        // GIVEN – a delta that would otherwise be processed successfully
        GraphComparator.Delta delta = new GraphComparator.Delta(source, "numbers", "-root-", null, null, 0);
        delta.setCmd(ARRAY_SET_ELEMENT);
        List<GraphComparator.Delta> commands = Collections.singletonList(delta);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> GraphComparator.applyDelta(source, commands, idFetcher, // deltaProcessor is null
        null), "A null DeltaProcessor should cause a NullPointerException when a command is executed");
    }

    @Test
    void testApplyDelta_NullDeltaCommand_ThrowsException() {
        // GIVEN – delta with a null command (cmd field not set)
        SampleObject obj = new SampleObject(1);
        GraphComparator.Delta delta = new GraphComparator.Delta(obj, "someField", GraphComparator.ROOT, null, null, null);
        // deliberately leave cmd as null
        // fetch id from SampleObject
        GraphComparator.ID fetcher = o -> ((SampleObject) o).id;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> GraphComparator.applyDelta(obj, Collections.singletonList(delta), fetcher, GraphComparator.getJavaDeltaProcessor()), "A Delta with a null command should cause a NullPointerException in the switch statement");
    }
}
