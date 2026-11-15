package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_reset_Test {

    /**
     * Instance under test – created fresh for each test method.
     */
    private FastByteArrayOutputStream outputStream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a new instance before every test to guarantee isolation.
        outputStream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – helps the GC in long test runs.
        outputStream = null;
    }

    @Test
    @DisplayName("reset() clears the stream and allows further writes")
    void testResetClearsBufferAndAllowsReuse() {
        // given: some data written to the stream
        byte[] initialData = "data".getBytes();
        outputStream.write(initialData);
        assertEquals(initialData.length, outputStream.size(), "Pre‑condition: size must reflect written bytes");
        // when: the stream is reset
        outputStream.reset();
        // then: size is zero and the internal buffer appears empty
        assertEquals(0, outputStream.size(), "Size must be zero after reset");
        assertArrayEquals(new byte[0], outputStream.toByteArray(), "toByteArray() should return an empty array after reset");
        // and when: we write again after the reset
        outputStream.write('X');
        // then: the stream behaves like a fresh instance
        assertEquals(1, outputStream.size(), "Size should reflect the new write");
        assertArrayEquals(new byte[] { (byte) 'X' }, outputStream.toByteArray(), "Content after reset and a new write must match the newly written byte");
    }

    @Test
    @DisplayName("reset() does not throw any exception")
    void testResetDoesNotThrow() {
        assertDoesNotThrow(() -> {
            outputStream.write(123);
            outputStream.reset();
        }, "reset() should never throw an exception");
    }

    @Test
    @DisplayName("reset() on a newly created stream leaves size at 0")
    void testResetOnNewStream() {
        // When reset is called immediately after construction
        outputStream.reset();
        // Then size must be zero and toByteArray empty
        assertEquals(0, outputStream.size(), "size() should be 0 after reset on a fresh stream");
        assertArrayEquals(new byte[0], outputStream.toByteArray(), "toByteArray() must return an empty array after reset on a fresh stream");
    }

    @Test
    @DisplayName("reset() clears previously written data")
    void testResetAfterWrites() {
        byte[] data = { 1, 2, 3, 4, 5 };
        outputStream.write(data);
        assertEquals(data.length, outputStream.size(), "Pre‑condition: size reflects written bytes");
        outputStream.reset();
        assertEquals(0, outputStream.size(), "Size must be 0 after reset");
        assertArrayEquals(new byte[0], outputStream.toByteArray(), "toByteArray() should be empty after reset");
    }

    @Test
    @DisplayName("size() reflects only data written after the most recent reset")
    void testSizeAfterResetAndSubsequentWrites() {
        // Write first three bytes
        outputStream.write(new byte[] { 10, 20, 30 });
        assertEquals(3, outputStream.size());
        // Reset and write two new bytes
        outputStream.reset();
        outputStream.write(new byte[] { 40, 50 });
        assertEquals(2, outputStream.size(), "Size must reflect only the bytes written after reset");
        assertArrayEquals(new byte[] { 40, 50 }, outputStream.toByteArray(), "Content must be the bytes written after reset");
    }

    @Test
    @DisplayName("multiple resets on an empty stream have no adverse effect")
    void testMultipleResetsOnEmptyStream() {
        outputStream.reset();
        // second call without intervening writes
        outputStream.reset();
        assertEquals(0, outputStream.size(), "Size should remain 0 after consecutive resets");
        assertDoesNotThrow(() -> outputStream.reset(), "reset() must not throw even when called repeatedly on an empty stream");
    }

    @Test
    @DisplayName("reset() works correctly after the internal buffer has grown")
    void testResetAfterLargeWrite() {
        // Write 10,000 bytes to force buffer expansion
        byte[] large = new byte[10_000];
        Arrays.fill(large, (byte) 7);
        outputStream.write(large);
        assertEquals(10_000, outputStream.size(), "Pre‑condition: large write size");
        outputStream.reset();
        assertEquals(0, outputStream.size(), "Size must be 0 after reset");
        assertArrayEquals(new byte[0], outputStream.toByteArray(), "toByteArray() must be empty after reset, regardless of previous capacity");
    }

    @Test
    @DisplayName("reset() after write(int) leaves only later writes")
    void testResetAfterWriteInt() {
        outputStream.write(10);
        outputStream.write(20);
        outputStream.write(30);
        outputStream.reset();
        outputStream.write(99);
        assertEquals(1, outputStream.size(), "Only the byte written after reset should be counted");
        assertArrayEquals(new byte[] { (byte) 99 }, outputStream.toByteArray(), "Content must be the single byte written after reset");
    }

    @Test
    @DisplayName("reset() after write(byte[], off, len) respects offset/length")
    void testResetAfterWriteWithOffset() {
        byte[] source = { 5, 6, 7, 8, 9 };
        outputStream.write(source);
        outputStream.reset();
        // Write the same array with offset 1 and length 3 -> bytes {6,7,8}
        outputStream.write(source, 1, 3);
        assertEquals(3, outputStream.size(), "Size must reflect the three bytes written after reset");
        assertArrayEquals(new byte[] { 6, 7, 8 }, outputStream.toByteArray(), "Content must be the sliced portion written after reset");
    }

    @Test
    @DisplayName("reset() does not clear underlying buffer content")
    void testResetDoesNotClearUnderlyingBuffer() {
        byte[] data = { 1, 2, 3, 4 };
        outputStream.write(data);
        outputStream.reset();
        // getBuffer() is public – inspect first element
        byte[] buffer = outputStream.getBuffer();
        assertNotNull(buffer, "Internal buffer should never be null");
        assertEquals(1, buffer[0], "Underlying buffer should still contain the original first byte");
    }

    @Test
    @DisplayName("reset() after close() still allows further writes")
    void testResetAfterClose() throws IOException {
        outputStream.write(new byte[] { 10, 20 });
        // according to API, close does not prevent further writes
        outputStream.close();
        outputStream.reset();
        outputStream.write(55);
        assertEquals(1, outputStream.size(), "Size after reset and a single write must be 1");
        assertArrayEquals(new byte[] { 55 }, outputStream.toByteArray(), "Content must be the byte written after reset");
    }

    @Test
    @DisplayName("reset() before writeTo() transfers only post‑reset data")
    void testResetBeforeWriteTo() throws IOException {
        outputStream.write(new byte[] { 1, 2, 3 });
        outputStream.reset();
        outputStream.write(new byte[] { 4, 5 });
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        outputStream.writeTo(target);
        assertArrayEquals(new byte[] { 4, 5 }, target.toByteArray(), "Target stream should receive only the bytes written after reset");
    }

    @Test
    @DisplayName("reset() is idempotent on an empty stream")
    void testResetIdempotentOnEmpty() {
        // three consecutive resets on a brand‑new stream
        outputStream.reset();
        outputStream.reset();
        outputStream.reset();
        assertEquals(0, outputStream.size(), "Size must stay 0 after multiple resets");
        assertDoesNotThrow(() -> outputStream.reset(), "reset() must not throw when called repeatedly on an empty stream");
    }

    @Test
    @DisplayName("reset() after writeBytes(byte[]) keeps only later bytes")
    void testResetAfterWriteBytes() {
        // writeBytes is a convenience method that writes the whole array
        outputStream.writeBytes(new byte[] { 9, 8, 7 });
        outputStream.reset();
        outputStream.writeBytes(new byte[] { 6, 5 });
        assertEquals(2, outputStream.size(), "Size must reflect only the bytes after reset");
        assertArrayEquals(new byte[] { 6, 5 }, outputStream.toByteArray(), "Content must be the bytes written after reset");
    }
}
