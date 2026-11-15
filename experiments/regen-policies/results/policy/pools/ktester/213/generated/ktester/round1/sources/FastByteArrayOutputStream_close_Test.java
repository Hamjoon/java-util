package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.util.Random;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_close_Test {

    private FastByteArrayOutputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the stream does not hold external resources
        stream = null;
    }

    @Test
    void closeDoesNotThrow() {
        assertDoesNotThrow(() -> stream.close(), "close() should not throw any exception");
    }

    @Test
    void closeLeavesStreamUsable() throws IOException {
        // Write some data before closing
        byte[] data = "Hello".getBytes();
        stream.write(data);
        assertEquals(data.length, stream.size());
        // Close the stream (no‑op)
        stream.close();
        // After close we should still be able to write and read
        byte[] more = "World".getBytes();
        stream.write(more);
        assertEquals(data.length + more.length, stream.size());
        byte[] expected = "HelloWorld".getBytes();
        assertArrayEquals(expected, stream.toByteArray(), "Stream should still contain all written bytes after close()");
    }

    @Test
    void closeMultipleTimesIsIdempotent() {
        assertDoesNotThrow(() -> {
            stream.close();
            // second call should also be a no‑op
            stream.close();
        });
    }

    @Test
    @DisplayName("Edge‑case: size unchanged after close")
    void closeDoesNotAlterSize() throws IOException {
        // Write some data, remember size, close, verify size unchanged
        byte[] payload = "EdgeCase".getBytes();
        stream.write(payload);
        int sizeBeforeClose = stream.size();
        assertDoesNotThrow(() -> stream.close(), "close() should not throw");
        assertEquals(sizeBeforeClose, stream.size(), "Size should remain unchanged after close()");
    }

    @Test
    @DisplayName("Edge‑case: toString reflects existing data after close")
    void closeDoesNotAffectToString() throws IOException {
        String text = "SampleString";
        stream.writeBytes(text.getBytes());
        assertDoesNotThrow(() -> stream.close(), "close() should not throw");
        assertEquals(text, stream.toString(), "toString() must return the data written before close");
    }

    @Test
    @DisplayName("Close on empty stream leaves it empty")
    void closeOnEmptyStream() throws IOException {
        assertEquals(0, stream.size(), "New stream should be empty");
        assertDoesNotThrow(() -> stream.close(), "close() on empty stream must not throw");
        assertEquals(0, stream.size(), "Size should still be 0 after close");
        assertArrayEquals(new byte[0], stream.toByteArray(), "toByteArray() should return an empty array after close");
    }

    @Test
    @DisplayName("Multiple close calls keep size unchanged")
    void multipleCloseCallsKeepSize() throws IOException {
        byte[] data = "RepeatedClose".getBytes();
        stream.write(data);
        int expectedSize = stream.size();
        assertDoesNotThrow(() -> {
            stream.close();
            // second close
            stream.close();
        }, "Multiple close calls must not throw");
        assertEquals(expectedSize, stream.size(), "Size must be unchanged after multiple close calls");
    }

    @Test
    @DisplayName("Close then write additional data")
    void closeThenWriteAdditionalData() throws IOException {
        byte[] first = "First".getBytes();
        byte[] second = "Second".getBytes();
        stream.write(first);
        // close should be a no‑op
        stream.close();
        // should still work
        stream.write(second);
        assertEquals(first.length + second.length, stream.size(), "Size must reflect both writes");
        assertArrayEquals("FirstSecond".getBytes(), stream.toByteArray(), "toByteArray() must contain concatenated data");
    }

    @Test
    @DisplayName("Close after reset leaves size zero")
    void closeAfterReset() throws IOException {
        stream.write("data".getBytes());
        // clears buffer
        stream.reset();
        assertEquals(0, stream.size(), "Size should be zero after reset");
        assertDoesNotThrow(() -> stream.close(), "close() after reset must not throw");
        assertEquals(0, stream.size(), "Size must remain zero after close");
    }

    @Test
    @DisplayName("Close then writeTo another stream")
    void closeThenWriteToAnotherStream() throws IOException {
        byte[] payload = "Transfer".getBytes();
        stream.write(payload);
        stream.close();
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> stream.writeTo(target), "writeTo should not throw after close");
        assertArrayEquals(payload, target.toByteArray(), "Target stream must contain exactly the original bytes");
        assertEquals(payload.length, stream.size(), "Original stream size must remain unchanged after writeTo");
    }

    @Test
    @DisplayName("Close after large data write")
    void closeAfterLargeDataWrite() throws IOException {
        // Create a large payload (10,000 random bytes)
        byte[] largeData = new byte[10_000];
        new Random(42).nextBytes(largeData);
        stream.write(largeData);
        assertEquals(largeData.length, stream.size(), "Size should match large data length before close");
        assertDoesNotThrow(() -> stream.close(), "close() must not throw for large data");
        assertEquals(largeData.length, stream.size(), "Size must remain unchanged after close");
        assertArrayEquals(largeData, stream.toByteArray(), "toByteArray() must return the original large data");
    }
}
