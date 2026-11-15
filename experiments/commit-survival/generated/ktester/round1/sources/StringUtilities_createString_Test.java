package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_createString_Test {

    // Sample data used across multiple tests
    private byte[] utf8Bytes;

    private byte[] isoBytes;

    /**
     * Simple data holder representing a single test scenario.
     */
    private static final class TestCase {

        // null means the byte[] argument is null
        final List<Integer> bytes;

        // may be null
        final String encoding;

        // expected return value when no exception
        final String expected;

        // true if an exception is expected
        final boolean expectException;

        // type of expected exception
        final Class<? extends Throwable> exceptionClass;

        // substring that must appear in the exception message (optional)
        final String messageContains;

        // human‑readable description (used as test name)
        final String description;

        TestCase(List<Integer> bytes, String encoding, String expected, boolean expectException, Class<? extends Throwable> exceptionClass, String messageContains, String description) {
            this.bytes = bytes;
            this.encoding = encoding;
            this.expected = expected;
            this.expectException = expectException;
            this.exceptionClass = exceptionClass;
            this.messageContains = messageContains;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Prepare byte arrays for the legacy tests (kept unchanged).
        utf8Bytes = "Hello, 世界".getBytes(StandardCharsets.UTF_8);
        isoBytes = "Café".getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * Supplies the stream of test cases defined in the JSON fragment.
     *
     * @return a stream of {@link TestCase} objects
     */
    private static Stream<TestCase> testCases() {
        List<TestCase> cases = new ArrayList<>();
        // 1. null byte array → null result
        cases.add(new TestCase(null, "UTF-8", null, false, null, null, "When the byte array argument is null the method should return null regardless of the encoding string."));
        // 2. simple UTF‑8 conversion
        cases.add(new TestCase(asList(72, 101, 108, 108, 111), "UTF-8", "Hello", false, null, null, "Valid non‑null byte array with a supported encoding should be converted to the expected string."));
        // 3. empty byte array → empty string
        cases.add(new TestCase(Collections.emptyList(), "UTF-8", "", false, null, null, "An empty byte array with a supported encoding should produce an empty string."));
        // 4. unsupported encoding → IllegalArgumentException
        cases.add(new TestCase(asList(65, 66, 67), "INVALID-ENCODING", null, true, IllegalArgumentException.class, "Encoding (INVALID-ENCODING) is not supported by your JVM", "When an unsupported encoding is supplied the method must catch UnsupportedEncodingException and re‑throw an IllegalArgumentException with a descriptive message."));
        // 5. null encoding → NullPointerException (propagated from String constructor)
        cases.add(new TestCase(asList(72, 101, 108, 108, 111), null, null, true, NullPointerException.class, null, "When the encoding argument is null the String constructor throws NullPointerException, which is not caught by the method."));
        // 6. empty encoding string → IllegalArgumentException
        cases.add(new TestCase(asList(65, 66, 67), "", null, true, IllegalArgumentException.class, "Encoding () is not supported by your JVM", "An empty charset name results in UnsupportedEncodingException, which the method converts to IllegalArgumentException with a descriptive message."));
        // 7. lower‑case charset name (case‑insensitive)
        cases.add(new TestCase(asList(72, 101, 108, 108, 111), "utf-8", "Hello", false, null, null, "The charset name is case‑insensitive; lower‑case \"utf-8\" should be accepted and produce the correct string."));
        // 8. multi‑byte character (é) in UTF‑8
        cases.add(new TestCase(asList(195, 169), "UTF-8", "é", false, null, null, "UTF‑8 multi‑byte character (é) is correctly decoded."));
        // 9. emoji (U+1F600) in UTF‑8
        cases.add(new TestCase(asList(240, 159, 152, 128), "UTF-8", "😀", false, null, null, "UTF‑8 encoded emoji (U+1F600) is correctly decoded to the corresponding Unicode character."));
        // 10. ISO‑8859‑1 single byte é
        cases.add(new TestCase(asList(233), "ISO-8859-1", "é", false, null, null, "Using a different supported charset (ISO‑8859‑1) decodes the single byte 0xE9 to the character é."));
        // 11. malformed UTF‑8 sequence (truncated leading byte) → replacement character
        cases.add(new TestCase(asList(195), "UTF-8", "\uFFFD", false, null, null, "Malformed UTF‑8 sequence (truncated leading byte) is replaced by the Unicode replacement character without throwing an exception."));
        // 12. byte array containing null bytes
        cases.add(new TestCase(asList(0, 65, 0, 66), "UTF-8", "\u0000A\u0000B", false, null, null, "Byte array containing null bytes (0) should be preserved in the resulting Java String."));
        return cases.stream();
    }

    /**
     * Helper that converts a list of {@link Integer} (or {@code null}) into a {@code byte[]}.
     *
     * @param list the list of Integer values; may be {@code null}
     * @return a byte array or {@code null} if the input list is {@code null}
     */
    private static byte[] toByteArray(List<Integer> list) {
        if (list == null) {
            return null;
        }
        byte[] arr = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = (byte) (int) list.get(i);
        }
        return arr;
    }

    @Test
    @DisplayName("createString returns correct String for UTF‑8 encoding")
    void createString_utf8Encoding_returnsExpectedString() {
        // given
        String expected = new String(utf8Bytes, StandardCharsets.UTF_8);
        // when
        String actual = StringUtilities.createString(utf8Bytes, "UTF-8");
        // then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("createString returns correct String for ISO‑8859‑1 encoding")
    void createString_isoEncoding_returnsExpectedString() {
        // given
        String expected = new String(isoBytes, StandardCharsets.ISO_8859_1);
        // when
        String actual = StringUtilities.createString(isoBytes, "ISO-8859-1");
        // then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("createString returns null when byte array is null")
    void createString_nullByteArray_returnsNull() {
        // given
        byte[] nullBytes = null;
        // when
        String result = StringUtilities.createString(nullBytes, "UTF-8");
        // then
        assertNull(result);
    }

    @Test
    @DisplayName("createString throws IllegalArgumentException for unsupported encoding")
    void createString_unsupportedEncoding_throwsIllegalArgumentException() {
        // given
        byte[] someBytes = "test".getBytes(StandardCharsets.UTF_8);
        String unsupportedEncoding = "UNSUPPORTED-ENCODING";
        // when / then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> StringUtilities.createString(someBytes, unsupportedEncoding), "Expected createString to throw, but it didn't");
        // Verify the exception message contains the problematic encoding
        assertTrue(thrown.getMessage().contains(unsupportedEncoding));
        // Verify the cause is an UnsupportedEncodingException
        assertTrue(thrown.getCause() instanceof UnsupportedEncodingException);
    }

    /**
     * Executes a single test case.
     *
     * @param tc the description of the test scenario
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("testCases")
    void createString_parameterised(TestCase tc) {
        // Convert the JSON‑style list of Integer values into a real byte[]
        byte[] bytes = toByteArray(tc.bytes);
        if (tc.expectException) {
            // When an exception is expected we verify its type and (optionally) its message.
            Throwable thrown = assertThrows(tc.exceptionClass, () -> StringUtilities.createString(bytes, tc.encoding), () -> "Expected exception of type " + tc.exceptionClass.getSimpleName() + " for case: " + tc.description);
            if (tc.messageContains != null) {
                assertTrue(thrown.getMessage().contains(tc.messageContains), () -> "Exception message [" + thrown.getMessage() + "] does not contain expected text [" + tc.messageContains + "]");
            }
            // For IllegalArgumentException we also assert that the cause is an UnsupportedEncodingException.
            if (thrown instanceof IllegalArgumentException && tc.exceptionClass == IllegalArgumentException.class) {
                assertTrue(thrown.getCause() instanceof UnsupportedEncodingException, () -> "Cause of IllegalArgumentException should be UnsupportedEncodingException");
            }
        } else {
            // Normal (non‑exceptional) execution.
            String actual = StringUtilities.createString(bytes, tc.encoding);
            assertEquals(tc.expected, actual, () -> "Unexpected result for case: " + tc.description);
        }
    }
}
