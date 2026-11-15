package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.io.IOException;
import java.io.Reader;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastReader_read_2_Test {

    private static final int BUFFER_SIZE = 8;

    private static final int PUSHBACK_SIZE = 4;

    @Mock
    private Reader // the Reader that FastReader wraps
    mockUnderlying;

    // instance under test
    private FastReader fastReader;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh FastReader for each test.
        fastReader = new FastReader(mockUnderlying, BUFFER_SIZE, PUSHBACK_SIZE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the underlying reader was not left in an unexpected state.
        // Most tests only expect a single read() call; if more interactions happen,
        // the test will fail, signalling a problem in the implementation.
        verifyNoMoreInteractions(mockUnderlying);
    }

    @Test
    void read_throwsIOExceptionWhenClosed() throws IOException {
        // Arrange: close the FastReader (the real close() implementation sets the
        // internal `in` field to null). After closing, any call to read should
        // raise an IOException.
        fastReader.close();
        // Act & Assert
        char[] dest = new char[10];
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(dest, 0, dest.length));
        assertEquals("FastReader stream is closed.", ex.getMessage());
    }

    @Test
    void read_returnsDataFromPushbackBuffer() throws IOException {
        // Arrange: push back a known character sequence.
        fastReader.pushback('A');
        // pushback works LIFO, so 'B' will be read first
        fastReader.pushback('B');
        char[] dest = new char[2];
        // Act
        int read = fastReader.read(dest, 0, dest.length);
        // Assert
        assertEquals(2, read, "Should read the two pushed‑back characters");
        assertArrayEquals(new char[] { 'B', 'A' }, dest);
        // No interaction with the underlying Reader because data came from pushback.
        verifyNoInteractions(mockUnderlying);
    }

    @Test
    void read_fillsFromUnderlyingWhenPushbackEmpty() throws IOException {
        // Arrange: prepare the mock underlying Reader to return the string "xyz".
        // The Answer copies the characters into the supplied buffer.
        when(mockUnderlying.read(any(char[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            char[] buf = invocation.getArgument(0);
            int off = invocation.getArgument(1);
            int len = invocation.getArgument(2);
            String data = "xyz";
            int toCopy = Math.min(len, data.length());
            data.getChars(0, toCopy, buf, off);
            // number of chars read
            return toCopy;
        });
        char[] dest = new char[3];
        // Act
        int read = fastReader.read(dest, 0, dest.length);
        // Assert
        assertEquals(3, read, "Should read all three characters from the underlying reader");
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, dest);
        // Verify that the underlying reader was called exactly once.
        verify(mockUnderlying, times(1)).read(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void read_mixesPushbackAndUnderlyingData() throws IOException {
        // Arrange: push back a single character, then let the underlying reader supply more.
        fastReader.pushback('P');
        when(mockUnderlying.read(any(char[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            char[] buf = invocation.getArgument(0);
            int off = invocation.getArgument(1);
            int len = invocation.getArgument(2);
            String data = "QR";
            int toCopy = Math.min(len, data.length());
            data.getChars(0, toCopy, buf, off);
            return toCopy;
        });
        char[] dest = new char[3];
        // Act
        int read = fastReader.read(dest, 0, dest.length);
        // Assert
        assertEquals(3, read, "Should read one pushback char + two from underlying");
        assertArrayEquals(new char[] { 'P', 'Q', 'R' }, dest);
        verify(mockUnderlying, times(1)).read(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void read_returnsMinusOneWhenEndOfStreamAndNoDataRead() throws IOException {
        // Arrange: underlying reader signals EOF immediately.
        when(mockUnderlying.read(any(char[].class), anyInt(), anyInt())).thenReturn(-1);
        char[] dest = new char[5];
        // Act
        int read = fastReader.read(dest, 0, dest.length);
        // Assert
        assertEquals(-1, read, "Should propagate -1 when EOF and nothing was read");
        verify(mockUnderlying, times(1)).read(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void read_returnsPartialCountWhenEOFAfterSomeData() throws IOException {
        // Arrange: first call returns 2 chars, second call returns -1 (EOF).
        when(mockUnderlying.read(any(char[].class), anyInt(), anyInt())).thenAnswer(new Answer<Integer>() {

            private int call = 0;

            @Override
            public Integer answer(InvocationOnMock inv) {
                char[] buf = inv.getArgument(0);
                int off = inv.getArgument(1);
                int len = inv.getArgument(2);
                if (call == 0) {
                    String data = "AB";
                    int toCopy = Math.min(len, data.length());
                    data.getChars(0, toCopy, buf, off);
                    call++;
                    return toCopy;
                }
                // EOF on subsequent fill
                return -1;
            }
        });
        char[] dest = new char[5];
        // Act
        int read = fastReader.read(dest, 0, dest.length);
        // Assert
        assertEquals(2, read, "Should return the number of chars read before EOF");
        assertArrayEquals(new char[] { 'A', 'B', '\0', '\0', '\0' }, dest);
        verify(mockUnderlying, times(2)).read(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void read_normalReadWithoutPushback() throws IOException {
        // Arrange: underlying returns "HelloWorld".
        when(mockUnderlying.read(any(char[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            char[] buf = invocation.getArgument(0);
            int off = invocation.getArgument(1);
            int len = invocation.getArgument(2);
            String data = "HelloWorld";
            int toCopy = Math.min(len, data.length());
            data.getChars(0, toCopy, buf, off);
            return toCopy;
        });
        char[] dest = new char[10];
        // Act: read first 5 characters.
        int read = fastReader.read(dest, 0, 5);
        // Assert
        assertEquals(5, read);
        assertArrayEquals(new char[] { 'H', 'e', 'l', 'l', 'o', '\0', '\0', '\0', '\0', '\0' }, dest);
        verify(mockUnderlying, times(1)).read(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void read_spanningMultipleFills() throws IOException {
        // Underlying data is 10 chars, buffer size is 4, request 15 chars.
        final String source = "HelloWorld";
        when(mockUnderlying.read(any(char[].class), anyInt(), anyInt())).thenAnswer(new Answer<Integer>() {

            private int pos = 0;

            @Override
            public Integer answer(InvocationOnMock inv) {
                char[] buf = inv.getArgument(0);
                int off = inv.getArgument(1);
                int len = inv.getArgument(2);
                if (pos >= source.length()) {
                    // EOF
                    return -1;
                }
                int toCopy = Math.min(len, source.length() - pos);
                source.getChars(pos, pos + toCopy, buf, off);
                pos += toCopy;
                return toCopy;
            }
        });
        // larger than needed
        char[] dest = new char[20];
        // Act: request more than available.
        int read = fastReader.read(dest, 0, 15);
        // Assert: only 10 chars are read.
        assertEquals(10, read);
        assertArrayEquals(new char[] { 'H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0', '\0' }, dest);
        // The underlying read should have been called three times (4+4+2 chars).
        verify(mockUnderlying, times(3)).read(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void read_withPushbackBufferContent() throws IOException {
        // Push back two characters.
        fastReader.pushback('X');
        // LIFO: Y then X
        fastReader.pushback('Y');
        // Underlying returns "HelloWorld".
        when(mockUnderlying.read(any(char[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            char[] buf = invocation.getArgument(0);
            int off = invocation.getArgument(1);
            int len = invocation.getArgument(2);
            String data = "HelloWorld";
            int toCopy = Math.min(len, data.length());
            data.getChars(0, toCopy, buf, off);
            return toCopy;
        });
        char[] dest = new char[10];
        // Act: read 4 chars (2 from pushback, 2 from underlying).
        int read = fastReader.read(dest, 0, 4);
        // Assert
        assertEquals(4, read);
        assertArrayEquals(new char[] { 'Y', 'X', 'H', 'e', '\0', '\0', '\0', '\0', '\0', '\0' }, dest);
        verify(mockUnderlying, times(1)).read(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void read_withPushbackThenEOF() throws IOException {
        // Push back two characters.
        fastReader.pushback('A');
        fastReader.pushback('B');
        // Underlying immediately signals EOF.
        when(mockUnderlying.read(any(char[].class), anyInt(), anyInt())).thenReturn(-1);
        char[] dest = new char[5];
        // Act
        int read = fastReader.read(dest, 0, 5);
        // Assert: only the two push‑back chars are returned.
        assertEquals(2, read);
        assertArrayEquals(new char[] { 'B', 'A', '\0', '\0', '\0' }, dest);
        // Underlying should be called once (fill after pushback is exhausted).
        verify(mockUnderlying, times(1)).read(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void read_zeroLengthReturnsZero() throws IOException {
        // No underlying interaction should happen.
        char[] dest = new char[5];
        int read = fastReader.read(dest, 0, 0);
        assertEquals(0, read);
        verifyNoInteractions(mockUnderlying);
    }

    @Test
    void read_withNonZeroOffset() throws IOException {
        // Underlying returns "Test".
        when(mockUnderlying.read(any(char[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            char[] buf = invocation.getArgument(0);
            int off = invocation.getArgument(1);
            int len = invocation.getArgument(2);
            String data = "Test";
            int toCopy = Math.min(len, data.length());
            data.getChars(0, toCopy, buf, off);
            return toCopy;
        });
        char[] dest = new char[10];
        // Fill with sentinel values to ensure we don't overwrite other slots.
        for (int i = 0; i < dest.length; i++) dest[i] = '-';
        int read = fastReader.read(dest, 3, 4);
        assertEquals(4, read);
        // untouched
        assertEquals('-', dest[0]);
        assertEquals('-', dest[1]);
        assertEquals('-', dest[2]);
        assertArrayEquals(new char[] { 'T', 'e', 's', 't' }, new char[] { dest[3], dest[4], dest[5], dest[6] });
        // remaining positions unchanged
        assertEquals('-', dest[7]);
        assertEquals('-', dest[8]);
        assertEquals('-', dest[9]);
        verify(mockUnderlying, times(1)).read(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void read_afterEOFReturnsMinusOne() throws IOException {
        // Underlying is already at EOF.
        when(mockUnderlying.read(any(char[].class), anyInt(), anyInt())).thenReturn(-1);
        char[] dest = new char[5];
        int read = fastReader.read(dest, 0, 5);
        assertEquals(-1, read);
        verify(mockUnderlying, times(1)).read(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void read_invalidOffsetLen_throwsArrayIndexOutOfBounds() {
        char[] dest = new char[5];
        // off + len = 6 > dest.length
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.read(dest, 4, 2));
        // No interaction with underlying because the exception occurs before any read.
        verifyNoInteractions(mockUnderlying);
    }

    @Test
    void read_negativeOffset_throwsArrayIndexOutOfBounds() {
        char[] dest = new char[10];
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> fastReader.read(dest, -1, 5));
        verifyNoInteractions(mockUnderlying);
    }
}
