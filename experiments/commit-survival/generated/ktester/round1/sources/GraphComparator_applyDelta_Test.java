package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@ExtendWith(MockitoExtension.class)
class GraphComparator_applyDelta_Test {

    /**
     * Mocked {@link GraphComparator.ID} implementation.
     */
    @Mock
    GraphComparator.ID idFetcher;

    /**
     * Mocked {@link GraphComparator.DeltaProcessor} implementation.
     */
    @Mock
    GraphComparator.DeltaProcessor deltaProcessor;

    /**
     * The source object that will be traversed by {@code applyDelta}.
     */
    Sample source;

    /**
     * Common list used to hold deltas for each test (cleared in @BeforeEach).
     */
    List<GraphComparator.Delta> deltas;

    /**
     * Simple POJO used as the source graph object.
     */
    static class Sample {

        public String value;
    }

    @BeforeEach
    void setupBeforeEach() {
        source = new Sample();
        source.value = "original";
        deltas = new ArrayList<>();
        // By default the ID fetcher returns the same id for any object.
        // Individual tests can override this behaviour.
        when(idFetcher.getId(any())).thenReturn("id-1");
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        clearInvocations(idFetcher, deltaProcessor);
    }

    /**
     * Verify that a {@link GraphComparator.Delta.Command#OBJECT_ASSIGN_FIELD}
     * results in a call to {@link GraphComparator.DeltaProcessor#processObjectAssignField}
     * and that no {@link GraphComparator.DeltaError} is produced.
     */
    @Test
    void applyDelta_callsObjectAssignFieldProcessor() {
        // ----------------------------------------------------
        // given
        // ----------------------------------------------------
        GraphComparator.Delta delta = new GraphComparator.Delta(// id
        "id-1", // fieldName
        "value", // srcPtr (not used by applyDelta)
        "-root-.value", // srcValue (not used)
        "original", // targetValue (not used)
        "newValue", // optionalKey
        null);
        delta.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        deltas.add(delta);
        // ----------------------------------------------------
        // when
        // ----------------------------------------------------
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher, deltaProcessor, false);
        // ----------------------------------------------------
        // then
        // ----------------------------------------------------
        assertTrue(errors.isEmpty(), "No errors should be reported for a successful delta");
        // Capture the arguments passed to the processor to assert they are correct.
        ArgumentCaptor<Object> srcCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Field> fieldCaptor = ArgumentCaptor.forClass(Field.class);
        ArgumentCaptor<GraphComparator.Delta> deltaCaptor = ArgumentCaptor.forClass(GraphComparator.Delta.class);
        verify(deltaProcessor, times(1)).processObjectAssignField(srcCaptor.capture(), fieldCaptor.capture(), deltaCaptor.capture());
        assertSame(source, srcCaptor.getValue(), "Processor should receive the original source object");
        assertEquals("value", fieldCaptor.getValue().getName(), "Processor should receive the correct field");
        assertSame(delta, deltaCaptor.getValue(), "Processor should receive the original delta");
    }

    /**
     * When the source graph does not contain an object with the supplied id,
     * {@code applyDelta} should return a {@link GraphComparator.DeltaError}
     * describing the problem and must not invoke any processor method.
     */
    @Test
    void applyDelta_missingSourceObjectProducesError() {
        // ----------------------------------------------------
        // given
        // ----------------------------------------------------
        // The ID fetcher will still return "id-1" for the source object,
        // but the delta references a different id.
        GraphComparator.Delta delta = new GraphComparator.Delta("missing-id", "value", "-root-.value", null, null, null);
        delta.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        deltas.add(delta);
        // ----------------------------------------------------
        // when
        // ----------------------------------------------------
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher, deltaProcessor, false);
        // ----------------------------------------------------
        // then
        // ----------------------------------------------------
        assertEquals(1, errors.size(), "Exactly one error should be reported");
        GraphComparator.DeltaError error = errors.get(0);
        assertTrue(error.getMessage().contains("source object not found"), "Error message should indicate missing source object");
        assertSame(delta, error.getDelta(), "Error should reference the offending delta");
        // No processor method should have been called.
        verifyNoInteractions(deltaProcessor);
    }

    /**
     * When the field named in the delta cannot be found on the source object,
     * {@code applyDelta} should produce a {@link GraphComparator.DeltaError}
     * and not invoke any processor method.
     */
    @Test
    void applyDelta_missingFieldProducesError() {
        // ----------------------------------------------------
        // given
        // ----------------------------------------------------
        GraphComparator.Delta delta = new GraphComparator.Delta("id-1", "nonExistentField", "-root-.nonExistentField", null, null, null);
        delta.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        deltas.add(delta);
        // ----------------------------------------------------
        // when
        // ----------------------------------------------------
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher, deltaProcessor, false);
        // ----------------------------------------------------
        // then
        // ----------------------------------------------------
        assertEquals(1, errors.size(), "Exactly one error should be reported for a missing field");
        GraphComparator.DeltaError error = errors.get(0);
        assertTrue(error.getMessage().contains("field name missing"), "Error message should indicate missing field");
        assertSame(delta, error.getDelta(), "Error should reference the offending delta");
        // No processor method should have been called.
        verifyNoInteractions(deltaProcessor);
    }

    /**
     * Verify that the {@code failFast} flag stops processing after the first error.
     */
    @Test
    void applyDelta_failFastStopsAfterFirstError() {
        // ----------------------------------------------------
        // given
        // ----------------------------------------------------
        // First delta will cause an error (missing field)
        GraphComparator.Delta badDelta = new GraphComparator.Delta("id-1", "badField", "-root-.badField", null, null, null);
        badDelta.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        // Second delta would be valid if it were processed
        GraphComparator.Delta goodDelta = new GraphComparator.Delta("id-1", "value", "-root-.value", null, null, null);
        goodDelta.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        deltas.addAll(Arrays.asList(badDelta, goodDelta));
        // ----------------------------------------------------
        // when
        // ----------------------------------------------------
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher, deltaProcessor, // failFast = true
        true);
        // ----------------------------------------------------
        // then
        // ----------------------------------------------------
        assertEquals(1, errors.size(), "Fail‑fast should stop after the first error");
        verify(deltaProcessor, never()).processObjectAssignField(any(), any(), any());
    }

    @Test
    void applyDelta_unknownCommandProducesError() {
        GraphComparator.Delta delta = new GraphComparator.Delta("id-1", "any", null, null, null, null);
        // Intentionally leave cmd as null to simulate an unknown command.
        delta.setCmd(null);
        deltas.add(delta);
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher, deltaProcessor, false);
        assertEquals(1, errors.size(), "One error should be reported for unknown command");
        GraphComparator.DeltaError error = errors.get(0);
        assertTrue(error.getMessage().contains("Unknown command"), "Error message should mention unknown command");
        assertSame(delta, error.getDelta(), "Error should reference the offending delta");
        verifyNoInteractions(deltaProcessor);
    }

    @Test
    void applyDelta_processorThrowsExceptionIsCapturedAsDeltaError() {
        GraphComparator.Delta delta = new GraphComparator.Delta("id-1", "value", null, null, null, null);
        delta.setCmd(GraphComparator.Delta.Command.OBJECT_ASSIGN_FIELD);
        deltas.add(delta);
        // Configure the mock to throw when the specific processor method is invoked.
        doThrow(new RuntimeException("processor failure")).when(deltaProcessor).processObjectAssignField(any(), any(), any());
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher, deltaProcessor, false);
        assertEquals(1, errors.size(), "Processor exception should be turned into a DeltaError");
        GraphComparator.DeltaError error = errors.get(0);
        assertTrue(error.getMessage().contains("processor failure"), "Error message should contain the processor exception text");
        assertSame(delta, error.getDelta(), "Error should reference the offending delta");
        // Verify that the processor method was indeed called (and threw).
        verify(deltaProcessor, times(1)).processObjectAssignField(eq(source), any(Field.class), eq(delta));
    }

    @ParameterizedTest
    @EnumSource(GraphComparator.Delta.Command.class)
    void applyDelta_routesEachCommandToCorrectProcessor(GraphComparator.Delta.Command cmd) throws Exception {
        // ----------------------------------------------------
        // given
        // ----------------------------------------------------
        GraphComparator.Delta delta = new GraphComparator.Delta("id-1", // For OBJECT_ORPHAN the field name is irrelevant; we set it to null.
        cmd == GraphComparator.Delta.Command.OBJECT_ORPHAN ? null : "value", null, null, null, null);
        delta.setCmd(cmd);
        deltas.add(delta);
        // ----------------------------------------------------
        // when
        // ----------------------------------------------------
        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(source, deltas, idFetcher, deltaProcessor, false);
        // ----------------------------------------------------
        // then
        // ----------------------------------------------------
        assertTrue(errors.isEmpty(), "No errors should be produced for a valid command");
        // Verify the correct processor method was called exactly once.
        switch(cmd) {
            case ARRAY_SET_ELEMENT:
                verify(deltaProcessor, times(1)).processArraySetElement(eq(source), any(Field.class), eq(delta));
                break;
            case ARRAY_RESIZE:
                verify(deltaProcessor, times(1)).processArrayResize(eq(source), any(Field.class), eq(delta));
                break;
            case OBJECT_ASSIGN_FIELD:
                verify(deltaProcessor, times(1)).processObjectAssignField(eq(source), any(Field.class), eq(delta));
                break;
            case OBJECT_ORPHAN:
                // For OBJECT_ORPHAN the field argument is null.
                verify(deltaProcessor, times(1)).processObjectOrphan(eq(source), isNull(), eq(delta));
                break;
            case OBJECT_FIELD_TYPE_CHANGED:
                verify(deltaProcessor, times(1)).processObjectTypeChanged(eq(source), any(Field.class), eq(delta));
                break;
            case SET_ADD:
                verify(deltaProcessor, times(1)).processSetAdd(eq(source), any(Field.class), eq(delta));
                break;
            case SET_REMOVE:
                verify(deltaProcessor, times(1)).processSetRemove(eq(source), any(Field.class), eq(delta));
                break;
            case MAP_PUT:
                verify(deltaProcessor, times(1)).processMapPut(eq(source), any(Field.class), eq(delta));
                break;
            case MAP_REMOVE:
                verify(deltaProcessor, times(1)).processMapRemove(eq(source), any(Field.class), eq(delta));
                break;
            case LIST_RESIZE:
                verify(deltaProcessor, times(1)).processListResize(eq(source), any(Field.class), eq(delta));
                break;
            case LIST_SET_ELEMENT:
                verify(deltaProcessor, times(1)).processListSetElement(eq(source), any(Field.class), eq(delta));
                break;
            default:
                fail("Unhandled command: " + cmd);
        }
        // Ensure no other processor methods were inadvertently called.
        verifyNoMoreInteractions(deltaProcessor);
    }
}
