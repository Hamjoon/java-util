package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IOUtilities_close_Test {

    @Mock
    XMLStreamReader // used in the non‑null tests
    mockReader;

    class MyXMLStreamException extends XMLStreamException {

        MyXMLStreamException(String msg) {
            super(msg);
        }
    }

    class MyXMLStreamException extends XMLStreamException {

        MyXMLStreamException(String msg) {
            super(msg);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep each test isolated
        reset(mockReader);
    }

    /**
     * Verify that calling {@code IOUtilities.close(null)} does not throw any exception.
     */
    @Test
    void close_nullReader_doesNotThrow() {
        // given
        XMLStreamReader reader = null;
        // when / then
        assertDoesNotThrow(() -> IOUtilities.close(reader));
    }

    /**
     * Verify that a non‑null {@link XMLStreamReader} has its {@code close()} method invoked exactly once.
     */
    @Test
    void close_validReader_invokesClose() throws XMLStreamException {
        // given
        // mockReader is injected by MockitoExtension
        // when
        IOUtilities.close(mockReader);
        // then
        verify(mockReader, times(1)).close();
    }

    /**
     * Verify that if {@link XMLStreamReader#close()} throws {@link XMLStreamException},
     * {@code IOUtilities.close} swallows the exception and does not propagate it.
     */
    @Test
    void close_readerThrowsException_exceptionIsSwallowed() throws XMLStreamException {
        // given
        doThrow(new XMLStreamException("simulated failure")).when(mockReader).close();
        // when / then
        assertDoesNotThrow(() -> IOUtilities.close(mockReader));
        // also verify that close() was still called
        verify(mockReader, times(1)).close();
    }

    /**
     * Verify that if {@link XMLStreamReader#close()} throws {@link XMLStreamException},
     * {@code IOUtilities.close} swallows the exception and does not propagate it.
     */
    @Test
    void close_readerThrowsXMLStreamException_exceptionIsSwallowed() throws XMLStreamException {
        // given
        doThrow(new XMLStreamException("simulated failure")).when(mockReader).close();
        // when / then
        assertDoesNotThrow(() -> IOUtilities.close(mockReader));
        // also verify that close() was still called exactly once
        verify(mockReader, times(1)).close();
    }

    /**
     * Verify that a subclass of {@link XMLStreamException} is also swallowed.
     */
    @Test
    void close_readerThrowsSubclassOfXMLStreamException_exceptionIsSwallowed() throws XMLStreamException {
        // given
        class MyXMLStreamException extends XMLStreamException {

            MyXMLStreamException(String msg) {
                super(msg);
            }
        }
        doThrow(new MyXMLStreamException("custom subclass")).when(mockReader).close();
        // when / then
        assertDoesNotThrow(() -> IOUtilities.close(mockReader));
        // ensure exactly one invocation
        verify(mockReader, times(1)).close();
    }

    /**
     * Verify that unchecked exceptions (e.g., {@link IllegalStateException}) are NOT swallowed
     * and are propagated to the caller.
     */
    @Test
    void close_readerThrowsUncheckedException_exceptionPropagates() throws XMLStreamException {
        // given
        doThrow(new IllegalStateException("illegal state")).when(mockReader).close();
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> IOUtilities.close(mockReader));
        assertEquals("illegal state", ex.getMessage());
        // verify single call
        verify(mockReader, times(1)).close();
    }

    /**
     * Verify that a {@link NullPointerException} (unchecked) is propagated.
     */
    @Test
    void close_readerThrowsNullPointerException_exceptionPropagates() throws XMLStreamException {
        // given
        doThrow(new NullPointerException("null pointer")).when(mockReader).close();
        // when / then
        NullPointerException ex = assertThrows(NullPointerException.class, () -> IOUtilities.close(mockReader));
        assertEquals("null pointer", ex.getMessage());
        verify(mockReader, times(1)).close();
    }

    /**
     * Verify that an {@link AssertionError} (Error) is propagated.
     */
    @Test
    void close_readerThrowsAssertionError_errorPropagates() throws XMLStreamException {
        // given
        doThrow(new AssertionError("assertion failed")).when(mockReader).close();
        // when / then
        AssertionError err = assertThrows(AssertionError.class, () -> IOUtilities.close(mockReader));
        assertEquals("assertion failed", err.getMessage());
        verify(mockReader, times(1)).close();
    }

    /**
     * Ensure that {@code close()} is invoked exactly once even when it throws an
     * {@link XMLStreamException}. The method must not retry the close operation.
     */
    @Test
    void close_readerThrowsXMLStreamException_calledOnlyOnce() throws XMLStreamException {
        // given: first call throws, second call would also throw if invoked
        doThrow(new XMLStreamException("first call")).doThrow(new XMLStreamException("second call")).when(mockReader).close();
        // when
        assertDoesNotThrow(() -> IOUtilities.close(mockReader));
        // then: verify only a single invocation
        verify(mockReader, times(1)).close();
    }
}
