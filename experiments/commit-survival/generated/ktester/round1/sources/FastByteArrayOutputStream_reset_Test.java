package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_reset_Test {

    /**
     * Instance used by each test method.
     */
    private FastByteArrayOutputStream outputStream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for every test to guarantee isolation.
        outputStream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly required but keeps the pattern clear.
        outputStream = null;
    }

    /**
     * Asserts that the stream is in a freshly‑created state.
     */
    private static void assertStreamIsReset(FastByteArrayOutputStream stream) {
        assertEquals(0, stream.size(), "After reset the size must be zero.");
        assertArrayEquals(new byte[0], stream.toByteArray(), "After reset the underlying byte array should be empty.");
        assertEquals("", stream.toString(), "After reset toString() should return an empty string.");
    }

    @Test
    void reset_ShouldClearWrittenData() throws IOException {
        // given: some data has been written to the stream
        // ASCII 'A'
        outputStream.write(65);
        outputStream.write("Hello".getBytes());
        // pre‑condition check – ensure data is really there
        assertTrue(outputStream.size() > 0, "Pre‑condition failed: stream should contain data before reset.");
        // when: reset is invoked
        outputStream.reset();
        // then: the stream reports a size of zero and behaves as a freshly created instance
        assertEquals(0, outputStream.size(), "After reset the size must be zero.");
        assertArrayEquals(new byte[0], outputStream.toByteArray(), "After reset the underlying byte array should be empty.");
    }

    @Test
    void reset_OnEmptyStream_ShouldRemainEmpty() {
        // given: a newly created (empty) stream – no action needed
        // when: reset is called on an already empty stream
        outputStream.reset();
        // then: size stays zero and no exception is thrown
        assertEquals(0, outputStream.size(), "Resetting an empty stream should keep size at zero.");
    }

    @Test
    void multipleConsecutiveResets_ShouldStayEmpty() {
        // first reset on a fresh stream
        outputStream.reset();
        assertStreamIsReset(outputStream);
        // second reset immediately afterwards
        outputStream.reset();
        assertStreamIsReset(outputStream);
    }

    @Test
    void resetThenWriteNewData_ShouldReflectOnlyNewData() throws IOException {
        // write initial data
        outputStream.write("initial".getBytes());
        assertTrue(outputStream.size() > 0, "Pre‑condition: size must be > 0 before reset.");
        // reset clears it
        outputStream.reset();
        assertStreamIsReset(outputStream);
        // write new data after reset
        byte[] newData = "new".getBytes();
        outputStream.write(newData);
        assertEquals(newData.length, outputStream.size(), "Size after writing new data should equal the new data length.");
        assertArrayEquals(newData, outputStream.toByteArray(), "toByteArray should contain only the newly written bytes.");
    }

    @Test
    void resetAfterWriteInt_ShouldClearSingleByte() throws IOException {
        // write a single byte
        outputStream.write(0x7F);
        assertEquals(1, outputStream.size(), "Pre‑condition: size should be 1 after write(int).");
        outputStream.reset();
        assertStreamIsReset(outputStream);
    }

    @Test
    void resetAfterWriteByteArrayWithOffset_ShouldClearPartialWrite() throws IOException {
        byte[] source = new byte[20];
        for (int i = 0; i < source.length; i++) source[i] = (byte) i;
        // write a slice (offset 5, length 10)
        outputStream.write(source, 5, 10);
        assertEquals(10, outputStream.size(), "Pre‑condition: size should reflect the number of bytes written.");
        outputStream.reset();
        assertStreamIsReset(outputStream);
    }

    @Test
    void resetAfterWriteBytes_ShouldClearData() throws IOException {
        byte[] payload = "payload".getBytes();
        outputStream.writeBytes(payload);
        assertEquals(payload.length, outputStream.size(), "Pre‑condition: size should equal payload length after writeBytes.");
        outputStream.reset();
        assertStreamIsReset(outputStream);
    }

    @Test
    void resetAfterWriteTo_ShouldTransferDataAndThenClear() throws IOException {
        byte[] data = "transfer".getBytes();
        outputStream.write(data);
        assertEquals(data.length, outputStream.size(), "Pre‑condition: size should match written data before writeTo.");
        // target stream to receive the data
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        outputStream.writeTo(target);
        assertArrayEquals(data, target.toByteArray(), "Data written to the target OutputStream must match original data.");
        // now reset the source stream
        outputStream.reset();
        assertStreamIsReset(outputStream);
    }

    @Test
    void resetAfterToString_ShouldClearStringRepresentation() throws IOException {
        String text = "some text";
        outputStream.write(text.getBytes());
        assertEquals(text, outputStream.toString(), "Pre‑condition: toString() should return the written text.");
        outputStream.reset();
        assertStreamIsReset(outputStream);
    }

    @Test
    void resetAfterLargeDataWrite_ShouldHandleBufferGrowth() throws IOException {
        // 10,000 bytes of incremental values (mod 256)
        byte[] large = new byte[10_000];
        for (int i = 0; i < large.length; i++) large[i] = (byte) (i & 0xFF);
        outputStream.write(large);
        assertEquals(large.length, outputStream.size(), "Pre‑condition: size should equal the large payload length.");
        outputStream.reset();
        assertStreamIsReset(outputStream);
    }
}
