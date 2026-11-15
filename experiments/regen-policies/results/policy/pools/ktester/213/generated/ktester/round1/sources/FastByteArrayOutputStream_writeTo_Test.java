package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_writeTo_Test {

    private static Stream<Arguments> provideNormalOperationCases() {
        return Stream.of(Arguments.of("empty buffer", new byte[0]), Arguments.of("small buffer – \"Hello\"", "Hello".getBytes()), Arguments.of("large buffer – 1024 bytes (0‑255 repeated four times)", generateLargeByteArray()));
    }

    /**
     * Generates a 1024‑byte array containing the sequence 0‑255 repeated four times.
     */
    private static byte[] generateLargeByteArray() {
        byte[] arr = new byte[1024];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte) (i % 256);
        }
        return arr;
    }

    @Test
    void writeTo_copiesAllBytesToTargetStream() throws IOException {
        // GIVEN
        FastByteArrayOutputStream fastStream = new FastByteArrayOutputStream();
        byte[] data = "Hello FastByteArrayOutputStream".getBytes();
        // fill the buffer
        fastStream.write(data);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        // WHEN
        fastStream.writeTo(target);
        // THEN
        assertArrayEquals(data, target.toByteArray(), "The target OutputStream must contain exactly the bytes written to the FastByteArrayOutputStream");
    }

    @Test
    void writeTo_withEmptyBufferWritesNothing() throws IOException {
        // GIVEN
        // empty
        FastByteArrayOutputStream fastStream = new FastByteArrayOutputStream();
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        // WHEN
        fastStream.writeTo(target);
        // THEN
        assertEquals(0, target.size(), "When the source stream is empty, the target stream should remain empty");
    }

    @Test
    void writeTo_nullOutputStream_throwsNullPointerException() throws IOException {
        // GIVEN
        FastByteArrayOutputStream fastStream = new FastByteArrayOutputStream();
        // any non‑empty content
        fastStream.write(new byte[] { 1, 2, 3 });
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> fastStream.writeTo(null), "Passing a null OutputStream must result in a NullPointerException");
    }

    @Test
    void writeTo_propagatesIOExceptionFromTarget(@Mock OutputStream faultyOut) throws IOException {
        // GIVEN
        FastByteArrayOutputStream fastStream = new FastByteArrayOutputStream();
        fastStream.write(new byte[] { 10, 20, 30 });
        // Configure the mock to throw IOException when write(byte[],int,int) is invoked
        doThrow(new IOException("simulated failure")).when(faultyOut).write(any(byte[].class), eq(0), anyInt());
        // WHEN / THEN
        IOException ex = assertThrows(IOException.class, () -> fastStream.writeTo(faultyOut), "IOException from the target OutputStream must be propagated");
        assertEquals("simulated failure", ex.getMessage());
    }

    /**
     * Parameterised test that verifies {@code writeTo} forwards the exact contents of the
     * internal buffer to the supplied {@link OutputStream}. Three scenarios are covered:
     * <ul>
     *   <li>empty buffer – length 0</li>
     *   <li>small buffer – the bytes of the string “Hello”</li>
     *   <li>large buffer – 1024 bytes (0‑255 repeated four times) to trigger internal growth</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideNormalOperationCases")
    void writeTo_normalOperation(String description, byte[] data) throws IOException {
        // GIVEN
        FastByteArrayOutputStream fastStream = new FastByteArrayOutputStream();
        if (data.length > 0) {
            // populate the internal buffer
            fastStream.write(data);
        }
        OutputStream mockOut = mock(OutputStream.class);
        // WHEN
        fastStream.writeTo(mockOut);
        // THEN – capture the arguments of the single write(byte[],off,len) call
        ArgumentCaptor<byte[]> bufCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<Integer> offCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> lenCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockOut, times(1)).write(bufCaptor.capture(), offCaptor.capture(), lenCaptor.capture());
        byte[] capturedBuf = bufCaptor.getValue();
        int capturedOff = offCaptor.getValue();
        int capturedLen = lenCaptor.getValue();
        // The offset must always be zero and the length must equal the amount of data written.
        assertEquals(0, capturedOff, "Offset passed to OutputStream.write must be 0");
        assertEquals(data.length, capturedLen, "Length passed to OutputStream.write must match the number of bytes stored");
        // The slice of the captured buffer must equal the original data.
        byte[] slice = Arrays.copyOfRange(capturedBuf, capturedOff, capturedOff + capturedLen);
        assertArrayEquals(data, slice, "The bytes forwarded to the OutputStream must be identical to the source data");
    }

    @Test
    void writeTo_singleByteWritesForwardedCorrectly() throws IOException {
        // GIVEN
        FastByteArrayOutputStream fastStream = new FastByteArrayOutputStream();
        // 65
        fastStream.write('A');
        // 66
        fastStream.write('B');
        // 67
        fastStream.write('C');
        OutputStream mockOut = mock(OutputStream.class);
        // WHEN
        fastStream.writeTo(mockOut);
        // THEN – capture the write call and verify the exact three bytes were sent
        ArgumentCaptor<byte[]> bufCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<Integer> offCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> lenCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockOut, times(1)).write(bufCaptor.capture(), offCaptor.capture(), lenCaptor.capture());
        assertEquals(0, offCaptor.getValue(), "Offset must be 0");
        assertEquals(3, lenCaptor.getValue(), "Length must be 3 for three written bytes");
        byte[] expected = new byte[] { 65, 66, 67 };
        byte[] actual = Arrays.copyOfRange(bufCaptor.getValue(), offCaptor.getValue(), offCaptor.getValue() + lenCaptor.getValue());
        assertArrayEquals(expected, actual, "writeTo must forward the three bytes written via write(int)");
    }

    @Test
    void writeTo_propagatesRuntimeExceptionFromTarget(@Mock OutputStream badOut) throws IOException {
        // GIVEN
        FastByteArrayOutputStream fastStream = new FastByteArrayOutputStream();
        // arbitrary data
        fastStream.write(new byte[] { 42, 43 });
        // Mock throws IllegalStateException on any write call
        doThrow(new IllegalStateException("runtime failure")).when(badOut).write(any(byte[].class), anyInt(), anyInt());
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fastStream.writeTo(badOut), "RuntimeException from the target OutputStream must not be swallowed");
        assertEquals("runtime failure", ex.getMessage());
    }
}
