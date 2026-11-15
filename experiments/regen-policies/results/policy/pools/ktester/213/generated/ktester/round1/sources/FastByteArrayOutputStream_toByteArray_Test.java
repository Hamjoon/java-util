package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_toByteArray_Test {

    private FastByteArrayOutputStream stream;

    /* --------------------------------------------------------------
     * Helper holder class for parameterized arguments
     * -------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final String name;

        final byte[] expected;

        final String description;

        ArgumentsHolder(String name, byte[] expected, String description) {
            this.name = name;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // each test gets a fresh, empty stream
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // close to release any resources (the class implements Closeable)
        if (stream != null) {
            stream.close();
        }
    }

    /**
     * Helper to write a single byte (int)
     */
    private void writeInt(int value) throws IOException {
        stream.write(value);
    }

    /**
     * Helper to write a slice of a byte array
     */
    private void writeSlice(byte[] src, int off, int len) throws IOException {
        stream.write(src, off, len);
    }

    static Stream<ArgumentsHolder> basicCasesProvider() {
        return Stream.of(new ArgumentsHolder("empty", new byte[0], "When the stream has never received any data, toByteArray() should return an empty byte array."), new ArgumentsHolder("singleByte", new byte[] { 65 }, "Write a single byte (value 65, character 'A') and verify that toByteArray() returns an array containing exactly that byte."), new ArgumentsHolder("helloString", "Hello".getBytes(), "Write the UTF‑8 bytes of the string \"Hello\" and ensure toByteArray() returns the same sequence of bytes."), new ArgumentsHolder("resetAfterWrite", new byte[0], "Write some bytes, call reset() to clear the buffer, then toByteArray() must return an empty array."), new ArgumentsHolder("defensiveCopy", new byte[] { 1, 2, 3, 4, 5 }, "Write five bytes, obtain a copy via toByteArray(), mutate the returned array, then call toByteArray() again. The second call must still return the original five bytes."), new ArgumentsHolder("bufferGrowth", new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, "Write more bytes than the initial internal buffer capacity (assumed default < 10) to trigger buffer growth. Verify that toByteArray() returns all written bytes in correct order."), new ArgumentsHolder("afterClose", new byte[] { 100, 101, 102 }, "After closing the stream, toByteArray() should still be usable and return the bytes that were written before close."));
    }

    private static Field getField(String name) throws NoSuchFieldException {
        Field f = FastByteArrayOutputStream.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @Test
    void toByteArrayOnEmptyStreamReturnsZeroLengthArray() {
        byte[] result = stream.toByteArray();
        assertNotNull(result, "Result must not be null");
        assertEquals(0, result.length, "Empty stream should return an empty array");
    }

    @Test
    void toByteArrayReturnsExactWrittenBytes() throws IOException {
        byte[] data = "FastByteArray".getBytes();
        stream.write(data);
        assertEquals(data.length, stream.size(), "Size must match bytes written");
        byte[] result = stream.toByteArray();
        assertArrayEquals(data, result, "toByteArray should return the exact data written");
    }

    @Test
    void toByteArrayReturnsCopyNotReference() throws IOException {
        byte[] data = "CopyTest".getBytes();
        stream.write(data);
        // Obtain the internal buffer via the public accessor (if present)
        byte[] internal = stream.getBuffer();
        byte[] copy = stream.toByteArray();
        assertNotSame(internal, copy, "toByteArray must return a copy, not the internal buffer");
        // Mutate the returned copy and verify the internal buffer is unchanged
        copy[0] = (byte) (copy[0] ^ 0xFF);
        assertEquals(data[0], internal[0], "Modifying the returned array must not affect internal buffer");
    }

    @Test
    void toByteArrayAfterResetReturnsEmptyArray() throws IOException {
        stream.write("SomeData".getBytes());
        assertTrue(stream.size() > 0, "Pre‑condition: stream should contain data");
        stream.reset();
        assertEquals(0, stream.size(), "After reset the size must be zero");
        byte[] result = stream.toByteArray();
        assertNotNull(result);
        assertEquals(0, result.length, "After reset, toByteArray must be empty");
    }

    @Test
    void toByteArrayHandlesBufferGrowthCorrectly() throws IOException {
        // Write more bytes than the default initial capacity (32) to force growth
        byte[] manyBytes = new byte[100];
        for (int i = 0; i < manyBytes.length; i++) {
            manyBytes[i] = (byte) i;
        }
        stream.write(manyBytes);
        assertEquals(manyBytes.length, stream.size(), "Size must reflect all bytes written");
        byte[] result = stream.toByteArray();
        assertArrayEquals(manyBytes, result, "Result must contain all bytes after internal buffer expansion");
    }

    @Test
    void toByteArrayDoesNotThrowWhenUsedInTypicalUtilityMethod() throws IOException {
        // Simulate a typical usage pattern (see invoke examples)
        FastByteArrayOutputStream out = new FastByteArrayOutputStream(16);
        out.write("Hello".getBytes());
        out.write("World".getBytes());
        // The method under test should simply return a copy of the data
        byte[] bytes = out.toByteArray();
        assertArrayEquals("HelloWorld".getBytes(), bytes);
        assertEquals(10, out.size(), "Size must still be correct after toByteArray");
        out.close();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicCasesProvider")
    void toByteArrayBasicScenarios(ArgumentsHolder holder) throws IOException {
        switch(holder.name) {
            case "empty":
                // nothing to write
                break;
            case "singleByte":
                writeInt(65);
                break;
            case "helloString":
                stream.write(holder.expected);
                break;
            case "resetAfterWrite":
                stream.write(new byte[] { 9, 9, 9 });
                stream.reset();
                break;
            case "defensiveCopy":
                stream.write(holder.expected);
                byte[] firstCopy = stream.toByteArray();
                // mutate copy
                firstCopy[0] = (byte) (firstCopy[0] ^ 0xFF);
                // second call must give original data
                byte[] secondCopy = stream.toByteArray();
                assertArrayEquals(holder.expected, secondCopy, "Defensive copy must not affect internal state");
                // test completed
                return;
            case "bufferGrowth":
                stream.write(holder.expected);
                break;
            case "afterClose":
                stream.write(holder.expected);
                // close before toByteArray
                stream.close();
                break;
            default:
                fail("Unhandled case: " + holder.name);
        }
        byte[] result = stream.toByteArray();
        assertArrayEquals(holder.expected, result, holder.description);
    }

    /**
     * Slice write test
     */
    @Test
    void toByteArrayWithOffsetLengthWrite() throws IOException {
        byte[] src = { 0, 1, 2, 3, 4, 5 };
        // writes 2,3,4
        writeSlice(src, 2, 3);
        byte[] result = stream.toByteArray();
        assertArrayEquals(new byte[] { 2, 3, 4 }, result, "Write a byte array {0,1,2,3,4,5} using write(byte[], off=2, len=3). toByteArray() must return only the slice that was written.");
    }

    /**
     * High‑byte value test
     */
    @Test
    void toByteArrayWithHighByteValue() throws IOException {
        // int > 127, only low 8 bits stored
        writeInt(200);
        byte[] result = stream.toByteArray();
        assertArrayEquals(new byte[] { (byte) 200 }, result, "Write an int value larger than 127 (e.g., 200). The stored byte should be the unsigned value 200.");
    }

    /**
     * Zero‑length write test
     */
    @Test
    void toByteArrayZeroLengthWrite() throws IOException {
        byte[] empty = new byte[0];
        // no bytes added
        writeSlice(empty, 0, 0);
        byte[] result = stream.toByteArray();
        assertEquals(0, result.length, "Calling write(byte[], off=0, len=0) should not add bytes; toByteArray() returns empty array.");
    }

    /**
     * Multiple calls immutability test
     */
    @Test
    void toByteArrayMultipleCallsImmutability() throws IOException {
        byte[] data = { 10, 20, 30 };
        stream.write(data);
        byte[] first = stream.toByteArray();
        byte[] second = stream.toByteArray();
        assertNotSame(first, second, "Each call must return a new array instance.");
        assertArrayEquals(data, first, "First call content must match written data.");
        assertArrayEquals(data, second, "Second call content must match written data.");
    }

    /**
     * Reset then new write test
     */
    @Test
    void toByteArrayAfterResetAndNewWrite() throws IOException {
        stream.write(new byte[] { 1, 2, 3 });
        stream.reset();
        stream.write(new byte[] { 9, 8, 7 });
        byte[] result = stream.toByteArray();
        assertArrayEquals(new byte[] { 9, 8, 7 }, result, "After reset and new write, toByteArray() must return only the newly written bytes.");
    }

    @Test
    void toByteArrayThrowsNullPointerExceptionWhenInternalBufferIsNull() throws Exception {
        // Ensure a fresh instance with a valid buffer
        stream.write(new byte[] { 1, 2, 3 });
        // Force buf to null via reflection
        Field bufField = getField("buf");
        bufField.set(stream, null);
        assertThrows(NullPointerException.class, stream::toByteArray, "When internal buffer is null, Arrays.copyOf should throw NullPointerException.");
    }

    @Test
    void toByteArrayThrowsIllegalArgumentExceptionWhenCountIsNegative() throws Exception {
        // Write something to ensure buf is non‑null
        stream.write(new byte[] { 1, 2, 3 });
        // Set count to -1 via reflection
        Field countField = getField("count");
        countField.setInt(stream, -1);
        assertThrows(IllegalArgumentException.class, stream::toByteArray, "Negative count should cause Arrays.copyOf to throw IllegalArgumentException.");
    }

    @Test
    @Disabled("Running this test would likely OOM the JVM; kept for completeness.")
    void toByteArrayThrowsOutOfMemoryErrorWhenCountIsExcessivelyLarge() throws Exception {
        // Ensure buf is non‑null
        stream.write(new byte[] { 0 });
        // Set count to a huge value
        Field countField = getField("count");
        countField.setInt(stream, Integer.MAX_VALUE);
        assertThrows(OutOfMemoryError.class, stream::toByteArray, "Excessively large count should cause allocation failure leading to OutOfMemoryError.");
    }
}
