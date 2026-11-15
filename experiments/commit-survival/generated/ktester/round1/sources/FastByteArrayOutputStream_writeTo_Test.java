package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_writeTo_Test {

    private FastByteArrayOutputStream fastStream;

    @Mock
    private OutputStream mockTarget;

    /**
     * Simple OutputStream that always throws IOException on write.
     */
    private static final class TestOutputStreamThrowsIOException extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            throw new IOException("forced failure");
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            throw new IOException("forced failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        fastStream = new FastByteArrayOutputStream();
    }

    /**
     * Supplies data sets for normal operation tests:
     * <ul>
     *   <li>empty stream</li>
     *   <li>small 4‑byte sequence</li>
     *   <li>256‑byte sequence (0‑255) that forces buffer growth</li>
     * </ul>
     */
    private static Stream<Arguments> normalOperationProvider() {
        byte[] empty = new byte[0];
        byte[] four = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        byte[] twoHundredFiftySix = new byte[256];
        for (int i = 0; i < 256; i++) {
            twoHundredFiftySix[i] = (byte) i;
        }
        return Stream.of(Arguments.of("empty stream", empty), Arguments.of("four bytes", four), Arguments.of("256 bytes (buffer growth)", twoHundredFiftySix));
    }

    @Test
    void writeTo_delegatesCorrectBytesToTarget() throws IOException {
        // given
        byte[] data = "Hello World".getBytes();
        // fill the stream
        fastStream.write(data);
        // when
        fastStream.writeTo(mockTarget);
        // then
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(mockTarget, times(1)).write(captor.capture(), eq(0), eq(data.length));
        assertArrayEquals(data, captor.getValue(), "The bytes passed to the target OutputStream must match the stream contents");
    }

    @Test
    void writeTo_withEmptyStreamWritesZeroLength() throws IOException {
        // given: fastStream is empty (size == 0)
        // when
        fastStream.writeTo(mockTarget);
        // then
        // The contract of OutputStream.write(byte[], int, int) allows a zero‑length write.
        // Verify that the method is still called with length 0.
        verify(mockTarget, times(1)).write(any(byte[].class), eq(0), eq(0));
    }

    @Test
    void writeTo_propagatesIOExceptionFromTarget() throws IOException {
        // given
        fastStream.write("data".getBytes());
        doThrow(new IOException("boom")).when(mockTarget).write(any(byte[].class), anyInt(), anyInt());
        // when / then
        IOException ex = assertThrows(IOException.class, () -> fastStream.writeTo(mockTarget), "IOException from the target stream must be propagated");
        assertEquals("boom", ex.getMessage());
    }

    @ParameterizedTest(name = "writeTo copies correctly for {0}")
    @MethodSource("normalOperationProvider")
    void writeTo_copiesBytesCorrectly(String description, byte[] payload) throws IOException {
        // given – write the payload into the FastByteArrayOutputStream
        fastStream.write(payload);
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        // when
        fastStream.writeTo(dest);
        // then – destination must contain exactly the same bytes
        assertArrayEquals(payload, dest.toByteArray(), "Destination stream should contain the same bytes for case: " + description);
        // and the source stream must retain its size
        assertEquals(payload.length, fastStream.size(), "Source stream size must stay unchanged after writeTo");
    }

    @Test
    void writeTo_nullTarget_throwsNullPointerException() {
        // given – fastStream may contain data or be empty; it does not matter
        fastStream.write(new byte[] { 0x01, 0x02 });
        // when / then
        assertThrows(NullPointerException.class, () -> fastStream.writeTo(null), "Calling writeTo with a null OutputStream must throw NPE");
    }

    @Test
    void writeTo_targetThrowsIOException_propagates() {
        // given
        fastStream.write("abc".getBytes());
        OutputStream failingStream = new TestOutputStreamThrowsIOException();
        // when / then
        IOException ex = assertThrows(IOException.class, () -> fastStream.writeTo(failingStream), "IOException from the destination stream must be propagated");
        assertEquals("forced failure", ex.getMessage());
        // verify source stream is untouched
        assertEquals(3, fastStream.size(), "Source stream size must remain unchanged after exception");
    }

    @Test
    void writeTo_doesNotAlterSourceState() throws IOException {
        // given
        byte[] data = new byte[] { (byte) 0xAA, (byte) 0xBB, (byte) 0xCC };
        fastStream.write(data);
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        // when
        fastStream.writeTo(dest);
        // then – destination contains the data
        assertArrayEquals(data, dest.toByteArray(), "Destination must receive the exact bytes written to the source");
        // and source still reports the original size and content
        assertEquals(3, fastStream.size(), "Source size must stay the same after writeTo");
        assertArrayEquals(data, fastStream.toByteArray(), "Source buffer content must stay unchanged after writeTo");
    }

    @Test
    void writeTo_afterResetWritesZeroBytes() throws IOException {
        // given – write some data then reset the stream
        fastStream.write(new byte[] { 0x10, 0x20, 0x30 });
        // clears internal count
        fastStream.reset();
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        // when
        fastStream.writeTo(dest);
        // then – destination should be empty (zero‑length write)
        assertEquals(0, dest.size(), "After reset, writeTo must write zero bytes to the destination");
        // also verify that the source size is zero
        assertEquals(0, fastStream.size(), "Source size must be zero after reset");
    }

    @Test
    void writeTo_multipleCallsAppendData() throws IOException {
        // given
        byte[] data = new byte[] { 0x01, 0x02 };
        fastStream.write(data);
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        // first call
        fastStream.writeTo(dest);
        assertArrayEquals(data, dest.toByteArray(), "After first writeTo the destination must contain the original bytes");
        // second call – should append the same bytes again
        fastStream.writeTo(dest);
        byte[] expected = new byte[] { 0x01, 0x02, 0x01, 0x02 };
        assertArrayEquals(expected, dest.toByteArray(), "After second writeTo the destination must contain the bytes twice");
    }

    @Test
    void writeTo_internalNullBuffer_throwsNullPointerException() throws Exception {
        // given – force the private 'buf' field to null via reflection
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        bufField.setAccessible(true);
        bufField.set(fastStream, null);
        // also set count to a non‑zero value to trigger the write call
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.setInt(fastStream, 5);
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        // when / then – NPE should be thrown because out.write(null, …) is illegal
        assertThrows(NullPointerException.class, () -> fastStream.writeTo(dest), "If internal buffer is null, writeTo must result in NPE");
    }
}
