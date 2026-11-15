package com.cedarsoftware.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_close_3_Test {

    @Mock
    private XMLStreamWriter // injected by MockitoExtension
    writerMock;

    private static class MyXMLStreamException extends XMLStreamException {

        MyXMLStreamException(String message) {
            super(message);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already creates the mock before each test.
        // Reset the mock to guarantee a clean state.
        reset(writerMock);
    }

    @Test
    void close_NullWriter_DoesNothing() {
        // Act & Assert – the method must simply return without throwing.
        IOUtilities.close((XMLStreamWriter) null);
        // No interaction with a mock is possible, test passes if no exception.
    }

    @Test
    void close_ValidWriter_InvokesCloseExactlyOnce() throws Exception {
        // Arrange – the mock writer does nothing on close().
        // Act
        IOUtilities.close(writerMock);
        // Assert – verify that close() was called once and no other interactions occurred.
        verify(writerMock, times(1)).close();
        verifyNoMoreInteractions(writerMock);
    }

    @Test
    void close_WriterThrowsXMLStreamException_ExceptionIsSilentlyIgnored() throws Exception {
        // Arrange – make the mock throw an XMLStreamException when close() is called.
        doThrow(new XMLStreamException("simulated failure")).when(writerMock).close();
        // Act – the exception must be caught and ignored by the utility method.
        IOUtilities.close(writerMock);
        // Assert – close() was still invoked exactly once despite the exception.
        verify(writerMock, times(1)).close();
        verifyNoMoreInteractions(writerMock);
        // No exception should propagate, so reaching this line means the test passed.
    }

    @Test
    void close_WriterThrowsSubclassOfXMLStreamException_IsIgnored() throws Exception {
        // Arrange – mock throws a subclass of XMLStreamException.
        doThrow(new MyXMLStreamException("subclass")).when(writerMock).close();
        // Act – should be ignored.
        IOUtilities.close(writerMock);
        // Assert – interaction count is still exactly one.
        verify(writerMock, times(1)).close();
        verifyNoMoreInteractions(writerMock);
    }

    @Test
    void close_WriterThrowsRuntimeException_Propagates() {
        doThrow(new RuntimeException("runtime")).when(writerMock).close();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.close(writerMock));
        assertEquals("runtime", ex.getMessage());
        verify(writerMock, times(1)).close();
    }

    @Test
    void close_WriterThrowsIllegalArgumentException_Propagates() {
        doThrow(new IllegalArgumentException("bad arg")).when(writerMock).close();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.close(writerMock));
        assertEquals("bad arg", ex.getMessage());
        verify(writerMock, times(1)).close();
    }

    @Test
    void close_WriterThrowsAssertionError_Propagates() {
        doThrow(new AssertionError("assert fail")).when(writerMock).close();
        AssertionError err = assertThrows(AssertionError.class, () -> IOUtilities.close(writerMock));
        assertEquals("assert fail", err.getMessage());
        verify(writerMock, times(1)).close();
    }

    @Test
    void close_WriterThrowsNullPointerException_Propagates() {
        doThrow(new NullPointerException("null ptr")).when(writerMock).close();
        NullPointerException ex = assertThrows(NullPointerException.class, () -> IOUtilities.close(writerMock));
        assertEquals("null ptr", ex.getMessage());
        verify(writerMock, times(1)).close();
    }

    @Test
    void close_WriterThrowsOutOfMemoryError_Propagates() {
        doThrow(new OutOfMemoryError("oom")).when(writerMock).close();
        OutOfMemoryError err = assertThrows(OutOfMemoryError.class, () -> IOUtilities.close(writerMock));
        assertEquals("oom", err.getMessage());
        verify(writerMock, times(1)).close();
    }

    @Test
    void close_WriterPerformsSideEffectBeforeThrowingXMLStreamException() {
        // Use an array as a mutable holder for the side‑effect flag.
        final boolean[] closedFlag = { false };
        // The mock will set the flag then throw XMLStreamException.
        doAnswer(invocation -> {
            closedFlag[0] = true;
            throw new XMLStreamException("boom");
        }).when(writerMock).close();
        // Act – the utility method should swallow the exception.
        IOUtilities.close(writerMock);
        // Assert – side‑effect occurred and interaction count is correct.
        assertTrue(closedFlag[0], "Writer's close() side‑effect should have been executed");
        verify(writerMock, times(1)).close();
        verifyNoMoreInteractions(writerMock);
    }
}
