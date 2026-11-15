package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ReadOnlyBufferException;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toCharBuffer_Test {

    /**
     * The Converter is not used by the method under test, but we provide a mock to satisfy the signature.
     */
    @Mock
    private Converter mockConverter;

    /* --------------------------------------------------------------
     *  Helper holder for parameterized test arguments
     * -------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final CharBuffer source;

        final String expectedContent;

        final boolean expectReadOnly;

        final int expectedPosition;

        final int expectedLimit;

        final int expectedRemaining;

        ArgumentsHolder(CharBuffer source, String expectedContent, boolean expectReadOnly, int expectedPosition, int expectedLimit, int expectedRemaining) {
            this.source = source;
            this.expectedContent = expectedContent;
            this.expectReadOnly = expectReadOnly;
            this.expectedPosition = expectedPosition;
            this.expectedLimit = expectedLimit;
            this.expectedRemaining = expectedRemaining;
        }

        @Override
        public String toString() {
            return "source=" + source;
        }
    }

    /* --------------------------------------------------------------
     *  Custom CharBuffer that throws from asReadOnlyBuffer()
     * -------------------------------------------------------------- */
    private static class ThrowingCharBuffer extends CharBuffer {

        protected ThrowingCharBuffer() {
            super(0);
        }

        @Override
        public CharBuffer asReadOnlyBuffer() {
            throw new IllegalStateException("asReadOnlyBuffer forced failure");
        }

        // The remaining abstract methods are stubbed – they are never called in this test.
        @Override
        public CharBuffer compact() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareTo(CharBuffer that) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharBuffer duplicate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDirect() {
            return false;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public ByteBuffer asByteBuffer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharBuffer slice() {
            throw new UnsupportedOperationException();
        }

        @Override
        public char get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public char get(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharBuffer get(char[] dst, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasArray() {
            return false;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public CharBuffer put(char c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharBuffer put(int index, char c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharBuffer put(CharBuffer src) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharBuffer put(char[] src, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharBuffer put(String src) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharBuffer put(String src, int start, int end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharBuffer slice() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharBuffer subSequence(int start, int end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "";
        }
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the “Valid CharBuffer conversion” group
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validCharBufferProvider() {
        return Stream.of(new ArgumentsHolder(CharBuffer.wrap("hello".toCharArray()), "hello", true, /* expect read‑only */
        0, 5, 5), new ArgumentsHolder(CharBuffer.allocate(10).put("abc").flip(), "abc", true, 0, 3, 3));
    }

    /**
     * Verify that {@code toCharBuffer} returns a read‑only view of the supplied {@link CharBuffer}
     * and that the view contains the same character data as the original buffer.
     */
    @Test
    void toCharBuffer_returnsReadOnlyBufferWithSameContent() {
        // GIVEN: a writable CharBuffer containing some characters
        CharBuffer original = CharBuffer.allocate(10);
        original.put("hello");
        // prepare for reading
        original.flip();
        int originalPosition = original.position();
        int originalLimit = original.limit();
        // WHEN: the conversion method is invoked
        CharBuffer result = CharBufferConversions.toCharBuffer(original, mockConverter);
        // THEN: the returned buffer must be read‑only
        assertTrue(result.isReadOnly(), "Resulting CharBuffer should be read‑only");
        // AND: it must contain the same characters as the original buffer
        assertEquals(original.remaining(), result.remaining(), "Resulting buffer should have the same remaining characters");
        assertEquals(original.toString(), result.toString(), "String representation of both buffers should be identical");
        // AND: the original buffer's position and limit must remain unchanged
        assertEquals(originalPosition, original.position(), "Original buffer position must not be altered");
        assertEquals(originalLimit, original.limit(), "Original buffer limit must not be altered");
    }

    /**
     * Verify that attempting to modify the returned buffer throws a {@link ReadOnlyBufferException}.
     */
    @Test
    void toCharBuffer_readOnlyBufferCannotBeModified() {
        // GIVEN
        CharBuffer original = CharBuffer.wrap("test".toCharArray());
        // WHEN
        CharBuffer readOnly = CharBufferConversions.toCharBuffer(original, mockConverter);
        // THEN: any write operation should fail
        assertThrows(java.nio.ReadOnlyBufferException.class, () -> readOnly.put('X'));
    }

    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("validCharBufferProvider")
    void toCharBuffer_validConversions(ArgumentsHolder args) {
        // GIVEN
        // duplicate to keep original untouched for later checks
        CharBuffer original = args.source.duplicate();
        int originalPos = original.position();
        int originalLim = original.limit();
        int originalCap = original.capacity();
        // WHEN
        CharBuffer result = CharBufferConversions.toCharBuffer(args.source, mockConverter);
        // THEN
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals(args.expectedContent, result.toString(), "Content of the read‑only buffer must match expected");
        assertEquals(args.expectedPosition, result.position(), "Result position should be preserved");
        assertEquals(args.expectedLimit, result.limit(), "Result limit should be preserved");
        assertEquals(args.expectedRemaining, result.remaining(), "Result remaining should be correct");
        // original buffer must stay unchanged
        assertEquals(originalPos, args.source.position(), "Original position unchanged");
        assertEquals(originalLim, args.source.limit(), "Original limit unchanged");
        assertEquals(originalCap, args.source.capacity(), "Original capacity unchanged");
    }

    /* --------------------------------------------------------------
     *  Null input handling – expect NPE
     * -------------------------------------------------------------- */
    @Test
    void toCharBuffer_nullFrom_throwsNPE() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toCharBuffer(null, mockConverter));
    }

    /* --------------------------------------------------------------
     *  Invalid type handling – expect ClassCastException
     * -------------------------------------------------------------- */
    @Test
    void toCharBuffer_nonCharBufferString_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> CharBufferConversions.toCharBuffer("plain string", mockConverter));
    }

    @Test
    void toCharBuffer_nonCharBufferByteBuffer_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> CharBufferConversions.toCharBuffer(java.nio.ByteBuffer.allocate(4), mockConverter));
    }

    /* --------------------------------------------------------------
     *  Converter parameter ignored – ensure no interaction
     * -------------------------------------------------------------- */
    @Test
    void toCharBuffer_converterIgnored_noInteraction() {
        // GIVEN a mock that would throw if any method is called
        doThrow(new AssertionError("Converter should not be used")).when(mockConverter).// any method – just to have a stub
        toString();
        CharBuffer source = CharBuffer.wrap("test".toCharArray());
        // WHEN
        CharBuffer result = CharBufferConversions.toCharBuffer(source, mockConverter);
        // THEN
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals("test", result.toString());
        // verify that no method on the mock was invoked
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------
     *  Read‑only source buffer – should return the same instance
     * -------------------------------------------------------------- */
    @Test
    void toCharBuffer_alreadyReadOnly_returnsSameInstance() {
        CharBuffer readOnly = CharBuffer.wrap("immutable".toCharArray()).asReadOnlyBuffer();
        CharBuffer result = CharBufferConversions.toCharBuffer(readOnly, mockConverter);
        assertTrue(result.isReadOnly(), "Returned buffer must be read‑only");
        assertSame(readOnly, result, "When source is already read‑only, the same instance should be returned");
    }

    /* --------------------------------------------------------------
     *  Original buffer unchanged after conversion – still writable
     * -------------------------------------------------------------- */
    @Test
    void toCharBuffer_originalRemainsWritable() {
        CharBuffer original = CharBuffer.allocate(5);
        original.put("abc");
        // position=0, limit=3
        original.flip();
        CharBuffer result = CharBufferConversions.toCharBuffer(original, mockConverter);
        // result is read‑only
        assertTrue(result.isReadOnly());
        // original must still be writable – try to put another char
        // move to end of current data
        original.position(original.limit());
        // should not throw
        original.put('d');
        assertEquals('d', original.get(3));
    }

    /* --------------------------------------------------------------
     *  Non‑zero position and limit preserved
     * -------------------------------------------------------------- */
    @Test
    void toCharBuffer_nonZeroPositionAndLimit_preserved() {
        CharBuffer source = CharBuffer.wrap("abcdef".toCharArray());
        source.position(2);
        // visible chars: c,d,e
        source.limit(5);
        CharBuffer result = CharBufferConversions.toCharBuffer(source, mockConverter);
        assertTrue(result.isReadOnly());
        assertEquals(2, result.position());
        assertEquals(5, result.limit());
        assertEquals(3, result.remaining());
        assertEquals("cde", result.subSequence(result.position(), result.limit()).toString());
    }

    /* --------------------------------------------------------------
     *  Modification attempt on returned buffer – expect exception
     * -------------------------------------------------------------- */
    @Test
    void toCharBuffer_modificationAttempt_throwsReadOnlyBufferException() {
        CharBuffer source = CharBuffer.wrap("xyz".toCharArray());
        CharBuffer readOnly = CharBufferConversions.toCharBuffer(source, mockConverter);
        assertThrows(ReadOnlyBufferException.class, () -> readOnly.put('A'));
    }

    /* --------------------------------------------------------------
     *  Custom CharBuffer implementation that throws in asReadOnlyBuffer
     * -------------------------------------------------------------- */
    @Test
    void toCharBuffer_customCharBufferThrows_propagatesException() {
        CharBuffer throwing = new ThrowingCharBuffer();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CharBufferConversions.toCharBuffer(throwing, mockConverter));
        assertEquals("asReadOnlyBuffer forced failure", ex.getMessage());
    }
}
