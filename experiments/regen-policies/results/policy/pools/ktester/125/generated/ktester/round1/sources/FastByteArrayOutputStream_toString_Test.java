package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_toString_Test {

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// 1. newly created stream – already covered above but kept for completeness
        Arguments.of("Empty stream should return empty string", (Consumer<FastByteArrayOutputStream>) s -> {
        }, /* no write */
        ""), // 2. write a single byte using write(int)
        Arguments.of("Single byte write(int) should produce \"A\"", // 65 = 'A'
        (Consumer<FastByteArrayOutputStream>) s -> s.write(65), "A"), // 3. write a byte array using write(byte[])
        Arguments.of("write(byte[]) with \"Hello\" bytes", (Consumer<FastByteArrayOutputStream>) s -> s.write(new byte[] { 72, 101, 108, 108, 111 }), "Hello"), // 4. zero‑length slice write(b,0,0)
        Arguments.of("Zero‑length slice should leave stream empty", (Consumer<FastByteArrayOutputStream>) s -> s.write(new byte[] { 1, 2, 3 }, 0, 0), ""));
    }

    private static Stream<Arguments> boundaryAndErrorProvider() {
        return Stream.of(// 1. Fill the default buffer exactly (assume default size is 32)
        Arguments.of("Exact‑size write should return full content", (Consumer<FastByteArrayOutputStream>) s -> {
            byte[] data = new byte[32];
            for (int i = 0; i < data.length; i++) data[i] = 'x';
            s.write(data);
        }, // 32 x's
        "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", null), // 2. Write larger than initial buffer to trigger expansion
        Arguments.of("Large write should trigger buffer expansion", (Consumer<FastByteArrayOutputStream>) s -> {
            byte[] data = new byte[100];
            for (int i = 0; i < data.length; i++) data[i] = 'A';
            s.write(data);
        }, "A".repeat(100), null), // 3. Write then reset – should be empty
        Arguments.of("Write then reset should result in empty string", (Consumer<FastByteArrayOutputStream>) s -> {
            s.write("data".getBytes());
            s.reset();
        }, "", null), // 4. writeBytes(byte[])
        Arguments.of("writeBytes should correctly populate the buffer", (Consumer<FastByteArrayOutputStream>) s -> s.writeBytes("12345".getBytes()), "12345", null), // 5. Mixed sequence of writes
        Arguments.of("Mixed writes (write(int), write(byte[],off,len), writeBytes) should concatenate correctly", (Consumer<FastByteArrayOutputStream>) s -> {
            s.write('M');
            s.write(new byte[] { 'i' }, 0, 1);
            s.writeBytes("xed".getBytes());
        }, // write(int)
        // write(byte[],off,len)
        // writeBytes
        "Mixed", null), // 6. toString after close – should still return current content
        Arguments.of("Calling toString() after close should return existing data", (Consumer<FastByteArrayOutputStream>) s -> {
            s.write("closed".getBytes());
            try {
                s.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, // close may throw IOException
        "closed", null), // 7. Internal buffer set to null via reflection – expect NPE
        Arguments.of("Null internal buffer should cause NullPointerException", (Consumer<FastByteArrayOutputStream>) s -> {
            try {
                Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
                bufField.setAccessible(true);
                bufField.set(s, null);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }, // result is irrelevant because we expect an exception
        "", NullPointerException.class));
    }

    private static Stream<Arguments> exceptionScenariosProvider() {
        return Stream.of(// 1. Negative count
        Arguments.of("Negative count should cause StringIndexOutOfBoundsException", (Consumer<FastByteArrayOutputStream>) s -> {
            try {
                Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
                countField.setAccessible(true);
                countField.setInt(s, -1);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }, StringIndexOutOfBoundsException.class), // 2. Count larger than buffer length
        Arguments.of("Count > buffer length should cause StringIndexOutOfBoundsException", (Consumer<FastByteArrayOutputStream>) s -> {
            try {
                Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
                Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
                bufField.setAccessible(true);
                countField.setAccessible(true);
                byte[] buf = (byte[]) bufField.get(s);
                countField.setInt(s, buf.length + 5);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }, // set count to buf.length + 5
        StringIndexOutOfBoundsException.class));
    }

    @Test
    void testToStringWhenEmpty() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        assertEquals("", stream.toString(), "toString() should return an empty string for a newly created stream");
    }

    @Test
    void testToStringWithSimpleAscii() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        String expected = "Hello, World!";
        stream.write(expected.getBytes());
        assertEquals(expected, stream.toString(), "toString() should return the exact ASCII content that was written");
    }

    @Test
    void testToStringAfterReset() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        stream.write("First".getBytes());
        // Verify initial content
        assertEquals("First", stream.toString());
        // clear the buffer
        stream.reset();
        assertEquals("", stream.toString(), "toString() should be empty after reset");
        // Write new data after reset
        String second = "Second";
        stream.write(second.getBytes());
        assertEquals(second, stream.toString(), "toString() should reflect data written after a reset");
    }

    @Test
    void testToStringWithUnicodeCharacters() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // Include characters outside the ASCII range (e.g., emojis and accented letters)
        String expected = "Café ☕️ 漢字";
        stream.write(expected.getBytes());
        assertEquals(expected, stream.toString(), "toString() must correctly handle UTF‑8 encoded Unicode characters");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicFunctionalityProvider")
    void testBasicFunctionality(String description, Consumer<FastByteArrayOutputStream> action, String expected) throws Exception {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        action.accept(stream);
        assertEquals(expected, stream.toString(), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("boundaryAndErrorProvider")
    void testBoundaryAndErrorHandling(String description, Consumer<FastByteArrayOutputStream> action, String expected, Class<? extends Throwable> expectedException) throws Exception {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // Perform the action (may manipulate internal fields via reflection)
        action.accept(stream);
        if (expectedException != null) {
            // Expect an exception when calling toString()
            assertThrows(expectedException, stream::toString, description);
        } else {
            assertEquals(expected, stream.toString(), description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionScenariosProvider")
    void testExceptionScenarios(String description, Consumer<FastByteArrayOutputStream> corruptor, Class<? extends Throwable> expectedException) throws Exception {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // Populate buffer with some data so that the internal array is non‑null
        stream.write("dummy".getBytes());
        // Corrupt internal state via reflection
        corruptor.accept(stream);
        // The toString() call must throw the expected exception
        assertThrows(expectedException, stream::toString, description);
    }
}
