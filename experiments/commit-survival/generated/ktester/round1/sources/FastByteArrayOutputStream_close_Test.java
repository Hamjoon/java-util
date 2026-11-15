package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_close_Test {

    private FastByteArrayOutputStream outputStream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        outputStream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        outputStream = null;
    }

    @Test
    void testCloseDoesNotThrow() {
        assertDoesNotThrow(() -> outputStream.close(), "Calling close() should not throw any exception");
    }

    @Test
    void testMultipleCloseCallsDoNotThrow() {
        assertDoesNotThrow(() -> {
            outputStream.close();
            outputStream.close();
            outputStream.close();
        }, "Multiple invocations of close() should not throw any exception");
    }

    @Test
    void testWriteAfterCloseStillWorks() throws IOException {
        // Close first – the method is a no‑op, but we verify behaviour afterwards
        outputStream.close();
        // Perform a normal write operation after close
        // ASCII 'A'
        outputStream.write(65);
        assertEquals(1, outputStream.size(), "Size should reflect the written byte after close()");
        // Verify the content is as expected
        assertArrayEquals(new byte[] { (byte) 65 }, outputStream.toByteArray(), "Buffer content should match the written byte after close()");
    }

    /* --------------------------------------------------------------
     * 1. Write after close (array based) – both before & after close
     * -------------------------------------------------------------- */
    @Test
    void testWriteArrayBeforeAndAfterClose() throws IOException {
        byte[] first = "Hello".getBytes(StandardCharsets.UTF_8);
        byte[] second = "World".getBytes(StandardCharsets.UTF_8);
        outputStream.writeBytes(first);
        // no‑op
        outputStream.close();
        outputStream.writeBytes(second);
        byte[] expected = "HelloWorld".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, outputStream.toByteArray(), "Both writes should be present after close()");
        assertEquals(expected.length, outputStream.size(), "Size must reflect total bytes written before and after close()");
    }

    /* --------------------------------------------------------------
     * 2. Reset then close – stream must stay usable
     * -------------------------------------------------------------- */
    @Test
    void testResetThenCloseAndWriteAgain() throws IOException {
        outputStream.writeBytes("Initial".getBytes(StandardCharsets.UTF_8));
        // clear buffer
        outputStream.reset();
        // still a no‑op
        outputStream.close();
        // After reset+close we should be able to write anew
        byte[] later = "After".getBytes(StandardCharsets.UTF_8);
        outputStream.writeBytes(later);
        assertArrayEquals(later, outputStream.toByteArray(), "Buffer should contain only the data written after reset");
        assertEquals(later.length, outputStream.size(), "Size should reflect only the later write");
    }

    /* --------------------------------------------------------------
     * 3. writeTo after close – should flush all data
     * -------------------------------------------------------------- */
    @Test
    void testWriteToAfterClose() throws IOException {
        byte[] data = "DataForTarget".getBytes(StandardCharsets.UTF_8);
        outputStream.writeBytes(data);
        // no‑op
        outputStream.close();
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        outputStream.writeTo(target);
        assertArrayEquals(data, target.toByteArray(), "Target stream must receive all bytes written before close()");
    }

    /* --------------------------------------------------------------
     * 4. writeBytes surrounding close
     * -------------------------------------------------------------- */
    @Test
    void testWriteBytesAroundClose() throws IOException {
        byte[] part1 = "Part1".getBytes(StandardCharsets.UTF_8);
        byte[] part2 = "Part2".getBytes(StandardCharsets.UTF_8);
        outputStream.writeBytes(part1);
        // no‑op
        outputStream.close();
        outputStream.writeBytes(part2);
        byte[] expected = "Part1Part2".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, outputStream.toByteArray(), "Both writeBytes calls must succeed");
        assertEquals(expected.length, outputStream.size(), "Size must equal combined length of both arrays");
    }

    /* --------------------------------------------------------------
     * 5. write(int) surrounding close
     * -------------------------------------------------------------- */
    @Test
    void testWriteIntAroundClose() throws IOException {
        // 65
        outputStream.write('A');
        // no‑op
        outputStream.close();
        // 66
        outputStream.write('B');
        byte[] expected = new byte[] { 'A', 'B' };
        assertArrayEquals(expected, outputStream.toByteArray(), "Both single‑byte writes must be present");
        assertEquals(2, outputStream.size(), "Size should be 2 after two write(int) calls");
    }

    /* --------------------------------------------------------------
     * 6. write(byte[], off, len) surrounding close
     * -------------------------------------------------------------- */
    @Test
    void testWriteSliceAroundClose() throws IOException {
        // 10 bytes
        byte[] source = "ABCDEFGHIJ".getBytes(StandardCharsets.UTF_8);
        // Write slice "CDE" (indexes 2..4)
        outputStream.write(source, 2, 3);
        // no‑op
        outputStream.close();
        // Write slice "XYZ" (indexes 0..2 of a different array)
        byte[] other = "XYZ123".getBytes(StandardCharsets.UTF_8);
        outputStream.write(other, 0, 3);
        byte[] expected = "CDEXYZ".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, outputStream.toByteArray(), "Concatenated slices must match expected order");
        assertEquals(expected.length, outputStream.size(), "Size must equal total bytes of both slices");
    }

    /* --------------------------------------------------------------
     * 7. toString() after close – UTF‑8 compatible bytes
     * -------------------------------------------------------------- */
    @Test
    void testToStringAfterClose() throws IOException {
        // Japanese "Hello World"
        String text = "こんにちは世界";
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        outputStream.writeBytes(utf8);
        // no‑op
        outputStream.close();
        // FastByteArrayOutputStream.toString() uses default charset; enforce UTF‑8 for test
        String result = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(text, result, "toString (via UTF‑8) must reflect data written before and after close()");
    }

    /* --------------------------------------------------------------
     * 8. Mixed operations with multiple close calls – idempotence
     * -------------------------------------------------------------- */
    @Test
    void testMixedOperationsIdempotence() throws IOException {
        // 1. write(byte[])
        outputStream.writeBytes("A".getBytes(StandardCharsets.UTF_8));
        outputStream.close();
        // 2. writeBytes(byte[])
        outputStream.writeBytes("BC".getBytes(StandardCharsets.UTF_8));
        outputStream.close();
        // 3. write(int)
        outputStream.write('D');
        outputStream.close();
        // 4. write(byte[], off, len) – slice "EFG"
        byte[] sliceSrc = "EFGHI".getBytes(StandardCharsets.UTF_8);
        outputStream.write(sliceSrc, 0, 3);
        outputStream.close();
        // Verify no exception has been thrown throughout the sequence
        // (assertion is implicit – test would fail if an exception escaped)
        // Expected concatenated content: "ABCDEF G"
        // "ABCDEFG"
        String expected = "ABCDEF G".replace(" ", "");
        assertEquals(expected, new String(outputStream.toByteArray(), StandardCharsets.UTF_8), "All data written across mixed APIs must be present");
    }

    @Test
    void testMixedOperationsTotalSize() throws IOException {
        // Sequence mirrors the previous test but focuses on size calculation
        // 2
        outputStream.writeBytes("12".getBytes(StandardCharsets.UTF_8));
        outputStream.close();
        // +3
        outputStream.writeBytes("345".getBytes(StandardCharsets.UTF_8));
        outputStream.close();
        // +1
        outputStream.write('6');
        outputStream.close();
        byte[] src = "7890".getBytes(StandardCharsets.UTF_8);
        // writes "89" (+2)
        outputStream.write(src, 1, 2);
        outputStream.close();
        // 8
        int expectedSize = 2 + 3 + 1 + 2;
        assertEquals(expectedSize, outputStream.size(), "Size must equal total bytes written across all operations");
    }
}
