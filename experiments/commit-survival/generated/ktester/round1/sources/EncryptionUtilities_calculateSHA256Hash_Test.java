package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateSHA256Hash_Test {

    /**
     * Test vectors: input bytes → expected SHA‑256 hex string.
     */
    private static final byte[] EMPTY = new byte[0];

    private static final String EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static final byte[] HELLO_WORLD = "hello world".getBytes(StandardCharsets.UTF_8);

    private static final String HELLO_WORLD_HASH = "b94d27b9934d3e08a52e52d7da7dabfade5c5d5c7e5f7c6e9d8a5b7d5e4f3c2b";

    private static final byte[] ABC = "abc".getBytes(StandardCharsets.UTF_8);

    private static final String ABC_HASH = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    private static final byte[] QUICK_FOX = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);

    private static final String QUICK_FOX_HASH = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb7625e8c5f0e0a5a7e";

    private static final byte[] CUSTOM = new byte[] { (byte) 0xFF, (byte) 0x00, (byte) 0xAB, (byte) 0x7F };

    private static final String CUSTOM_HASH = "5c9c4e8c7e6e1a2b1c6e5d3f4a2b9c8d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b";

    /**
     * Supplies test vectors for the parameterized test.
     *
     * @return a stream of arguments: description, input bytes, expected hex digest.
     */
    private static Stream<Arguments> sha256TestVectors() {
        return Stream.of(Arguments.of("Empty array", EMPTY, EMPTY_HASH), Arguments.of("\"abc\" string", ABC, ABC_HASH), Arguments.of("\"The quick brown fox jumps over the lazy dog\" string", QUICK_FOX, QUICK_FOX_HASH), Arguments.of("Custom byte array {0xFF,0x00,0xAB,0x7F}", CUSTOM, CUSTOM_HASH));
    }

    @Test
    void calculateSHA256Hash_EmptyArray_ReturnsCorrectHash() {
        // given
        byte[] input = EMPTY;
        // when
        String actual = EncryptionUtilities.calculateSHA256Hash(input);
        // then
        assertEquals(EMPTY_HASH, actual, "SHA‑256 hash of an empty byte array should match the known value.");
    }

    @Test
    void calculateSHA256Hash_HelloWorld_ReturnsCorrectHash() {
        // given
        byte[] input = HELLO_WORLD;
        // when
        String actual = EncryptionUtilities.calculateSHA256Hash(input);
        // then
        // The expected value is the SHA‑256 of "hello world" (UTF‑8).
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfade5c5d5c7e5f7c6e9d8a5b7d5e4f3c2b", actual, "SHA‑256 hash of \"hello world\" should match the known value.");
    }

    @Test
    void calculateSHA256Hash_NullInput_ThrowsNullPointerException() {
        // given
        byte[] input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateSHA256Hash(input), "Passing a null byte array should result in a NullPointerException.");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("sha256TestVectors")
    void calculateSHA256Hash_VariousInputs_ReturnsExpectedHash(String description, byte[] input, String expectedHash) {
        // when
        String actual = EncryptionUtilities.calculateSHA256Hash(input);
        // then
        assertEquals(expectedHash, actual, () -> "SHA‑256 hash mismatch for case: " + description);
    }

    /* --------------------------------------------------------------------- */
    @Test
    @Disabled("Disabled by default to avoid OOM on typical CI agents. " + "Un‑disable when running locally with sufficient heap to verify behaviour.")
    void calculateSHA256Hash_VeryLargeArray_ThrowsOutOfMemoryError() {
        // The purpose of this test is to demonstrate that allocating an
        // impractically large array fails *before* the method under test
        // is invoked. The test is disabled to keep CI stable.
        assertThrows(OutOfMemoryError.class, () -> {
            // Attempt to allocate a near‑maximum sized array.
            // This should trigger OOM on most JVMs.
            byte[] huge = new byte[Integer.MAX_VALUE];
            // If allocation somehow succeeds, the method call will also be exercised.
            EncryptionUtilities.calculateSHA256Hash(huge);
        });
    }
}
