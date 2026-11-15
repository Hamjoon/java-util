package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_toString_Test {

    private FastByteArrayOutputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test to guarantee isolation
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – not strictly required but keeps the pattern clear
        stream = null;
    }

    private static Stream<Arguments> provideWriteZeroLengthCases() {
        return Stream.of(Arguments.of("Zero length write on empty array", new byte[] { 'A', 'B', 'C' }, 0, 0), Arguments.of("Zero length write on larger array", new byte[] { 'X', 'Y', 'Z' }, 1, 0));
    }

    @Test
    void testToStringWhenEmpty() {
        // GIVEN an empty FastByteArrayOutputStream
        // WHEN toString() is invoked
        String result = stream.toString();
        // THEN the result should be an empty string
        assertEquals("", result, "toString() on an empty stream should return an empty string");
    }

    @Test
    void testToStringAfterWritingSingleBytes() {
        // GIVEN a stream with several single‑byte writes
        stream.write('H');
        stream.write('e');
        stream.write('l');
        stream.write('l');
        stream.write('o');
        // WHEN converting to a string
        String result = stream.toString();
        // THEN the result should match the written characters
        assertEquals("Hello", result);
    }

    @Test
    void testToStringAfterWritingByteArray() {
        // GIVEN a byte array representing the word "World"
        // default charset, safe for ASCII
        byte[] data = "World".getBytes();
        stream.write(data, 0, data.length);
        // WHEN converting to a string
        String result = stream.toString();
        // THEN the result should be "World"
        assertEquals("World", result);
    }

    @Test
    void testToStringAfterPartialWriteWithOffsetAndLength() {
        // GIVEN a larger byte array, but we only write a slice of it
        byte[] data = "Hello, FastByteArrayOutputStream!".getBytes();
        // Write only the substring "Fast"
        // position of 'F' in the source array
        int offset = 7;
        // "Fast"
        int length = 4;
        stream.write(data, offset, length);
        // WHEN converting to a string
        String result = stream.toString();
        // THEN the result should be the sliced portion
        assertEquals("Fast", result);
    }

    @Test
    void testToStringAfterMultipleWritesAndGrowth() {
        // GIVEN a series of writes that force the internal buffer to grow
        // Write 1 byte, then 2 bytes, then 4 bytes, then 8 bytes
        for (int i = 1; i <= 15; i++) {
            stream.write('a' + (i % 26));
        }
        // Build the expected string using the same logic
        StringBuilder expected = new StringBuilder();
        for (int i = 1; i <= 15; i++) {
            expected.append((char) ('a' + (i % 26)));
        }
        // WHEN converting to a string
        String result = stream.toString();
        // THEN the result should match the expected sequence
        assertEquals(expected.toString(), result);
    }

    // ---------- Normal behaviour tests ----------
    @Test
    void testToStringSingleAsciiByte() {
        // Write a single ASCII byte (65 -> 'A')
        stream.write(65);
        assertEquals("A", stream.toString());
    }

    @Test
    void testToStringFullByteArray() {
        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        stream.write(data, 0, data.length);
        assertEquals("Hello, World!", stream.toString());
    }

    @Test
    void testToStringBufferGrowthAlphabet() {
        // Write 26 letters, which is larger than the default buffer size (usually 32)
        for (char c = 'A'; c <= 'Z'; c++) {
            stream.write(c);
        }
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", stream.toString());
    }

    @Test
    void testToStringAfterReset() throws IOException {
        // Write some data, reset, then verify toString() is empty
        stream.write("temp".getBytes(StandardCharsets.UTF_8));
        stream.reset();
        assertEquals("", stream.toString());
    }

    @Test
    void testToStringUtf8EAcute() {
        // UTF‑8 bytes for 'é' (U+00E9) -> 0xC3 0xA9
        byte[] éBytes = new byte[] { (byte) 0xC3, (byte) 0xA9 };
        stream.write(éBytes, 0, éBytes.length);
        assertEquals("é", stream.toString());
    }

    @Test
    void testToStringImmutability() {
        // Obtain a string, then write more data and ensure the first string stays unchanged
        stream.write('X');
        // should be "X"
        String first = stream.toString();
        stream.write('Y');
        assertEquals("X", first, "Previously obtained string must remain immutable");
        assertEquals("XY", stream.toString(), "Current toString() must reflect all writes");
    }

    // ---------- Edge‑case and exception tests ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideWriteZeroLengthCases")
    void testWriteZeroLengthDoesNotAlterStream(String description, byte[] data, int off, int len) {
        // Write with length 0 should be a no‑op
        stream.write(data, off, len);
        assertEquals("", stream.toString(), description);
    }

    @Test
    void testWriteSubRange() {
        byte[] src = new byte[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J' };
        // CDEFG
        stream.write(src, 2, 5);
        assertEquals("CDEFG", stream.toString());
    }

    @Test
    void testWriteNegativeOffsetThrows() {
        byte[] src = new byte[] { 'X', 'Y', 'Z' };
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(src, -1, 2), "Negative offset should throw IndexOutOfBoundsException");
    }

    @Test
    void testWriteLengthBeyondArrayThrows() {
        byte[] src = new byte[] { 'M', 'N', 'O' };
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(src, 1, 5), "Length beyond array bounds should throw IndexOutOfBoundsException");
    }

    @Test
    void testWriteNullArrayThrows() {
        assertThrows(NullPointerException.class, () -> stream.write(null, 0, 1), "Passing null array should throw NullPointerException");
    }

    @Test
    void testToStringAfterClose() throws IOException {
        stream.write("TestAfterClose".getBytes(StandardCharsets.UTF_8));
        // close should not discard data
        stream.close();
        assertEquals("TestAfterClose", stream.toString(), "toString() after close must still return previously written data");
    }

    @Test
    void testLargeWriteForMultipleExpansions() {
        // 10,000 'x' characters
        byte[] large = new byte[10_000];
        for (int i = 0; i < large.length; i++) {
            large[i] = 'x';
        }
        stream.write(large, 0, large.length);
        assertEquals("x".repeat(10_000), stream.toString(), "Large write should produce a string of 10,000 'x' characters");
    }

    @Test
    void testWriteEmojiUtf8() {
        // 😀
        byte[] emoji = new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 };
        stream.write(emoji, 0, emoji.length);
        assertEquals("😀", stream.toString(), "UTF‑8 emoji bytes must be decoded correctly by toString()");
    }

    @Test
    void testMixedWriteCalls() {
        // Sequence: write(int), write(byte[],...), write(int)
        // 'A'
        stream.write(65);
        stream.write(new byte[] { 'b', 'c' }, 0, 2);
        // 'D'
        stream.write(68);
        assertEquals("AbcD", stream.toString());
    }

    @Test
    void testResetThenWriteNewData() {
        stream.write("Old".getBytes(StandardCharsets.UTF_8));
        stream.reset();
        stream.write(new byte[] { 'N', 'e', 'w' }, 0, 3);
        assertEquals("New", stream.toString());
    }

    // ---------- writeBytes() exception test ----------
    @Test
    void testWriteBytesNullThrows() {
        assertThrows(NullPointerException.class, () -> stream.writeBytes(null), "writeBytes(null) must throw NullPointerException");
    }

    // ---------- Additional bounds checks for write(byte[],off,len) ----------
    @Test
    void testWriteOffsetBeyondArrayLengthThrows() {
        byte[] src = new byte[] { 'A', 'B' };
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(src, 3, 0), "Offset greater than array length should throw IndexOutOfBoundsException");
    }

    @Test
    void testWriteNegativeLengthThrows() {
        byte[] src = new byte[] { 'A', 'B' };
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(src, 0, -1), "Negative length should throw IndexOutOfBoundsException");
    }
}
