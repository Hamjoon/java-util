package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

// retained from the template; not used but harmless
@ExtendWith(MockitoExtension.class)
class StringUtilities_getBytes_Test {

    /**
     * Simple container for normal test parameters and expected byte array.
     */
    private static class ArgumentsHolder {

        final String input;

        final String encoding;

        final byte[] expected;

        ArgumentsHolder(String input, String encoding, byte[] expected) {
            this.input = input;
            this.encoding = encoding;
            this.expected = expected;
        }
    }

    /**
     * Container for exception‑expectation test cases.
     */
    private static class ExceptionCase {

        final String input;

        final String encoding;

        final Class<? extends Throwable> expectedException;

        ExceptionCase(String input, String encoding, Class<? extends Throwable> expectedException) {
            this.input = input;
            this.encoding = encoding;
            this.expectedException = expectedException;
        }
    }

    /**
     * Data provider for normal, empty, multibyte and case‑insensitive encodings.
     */
    private static Stream<ArgumentsHolder> normalEncodingProvider() {
        return Stream.of(// Null string input – expected null result
        new ArgumentsHolder(null, "UTF-8", null), // Normal ASCII string with UTF‑8
        new ArgumentsHolder("Hello, World!", "UTF-8", new byte[] { 72, 101, 108, 108, 111, 44, 32, 87, 111, 114, 108, 100, 33 }), // Empty string – should yield empty array
        new ArgumentsHolder("", "UTF-8", new byte[] {}), // Multibyte Unicode (Japanese Hiragana) – exact UTF‑8 sequence
        new ArgumentsHolder("こんにちは", "UTF-8", new byte[] { (byte) 227, (byte) 129, (byte) 147, (byte) 227, (byte) 130, (byte) 147, (byte) 227, (byte) 129, (byte) 171, (byte) 227, (byte) 129, (byte) 161, (byte) 227, (byte) 129, (byte) 175 }), // Case‑insensitive encoding name
        new ArgumentsHolder("CaseTest", "utf-8", new byte[] { 67, 97, 115, 101, 84, 101, 115, 116 }));
    }

    /**
     * Data provider for exceptional scenarios (unsupported or null encoding).
     */
    private static Stream<ExceptionCase> exceptionProvider() {
        return Stream.of(// Unsupported encoding – should wrap in IllegalArgumentException
        new ExceptionCase("test", "UNSUPPORTED-ENCODING", IllegalArgumentException.class), // Null encoding – String#getBytes(null) throws NullPointerException
        new ExceptionCase("sample", null, NullPointerException.class));
    }

    @Test
    void getBytes_withSupportedEncoding_returnsCorrectBytes() {
        // given
        String input = "Cedar Software";
        String encoding = "UTF-8";
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        assertNotNull(result, "Result should not be null for non‑null input");
        assertArrayEquals(input.getBytes(StandardCharsets.UTF_8), result, "Byte array should match the UTF‑8 representation of the input string");
    }

    @Test
    void getBytes_withNullString_returnsNull() {
        // given
        String input = null;
        String encoding = "UTF-8";
        // when
        byte[] result = StringUtilities.getBytes(input, encoding);
        // then
        assertNull(result, "Result should be null when the input string is null");
    }

    @Test
    void getBytes_withUnsupportedEncoding_throwsIllegalArgumentException() {
        // given
        String input = "test";
        String badEncoding = "UNSUPPORTED-ENCODING";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringUtilities.getBytes(input, badEncoding), "Method should wrap UnsupportedEncodingException in IllegalArgumentException");
        assertTrue(ex.getMessage().contains(badEncoding), "Exception message should contain the unsupported encoding name");
        assertNotNull(ex.getCause(), "Cause of the IllegalArgumentException should be the original UnsupportedEncodingException");
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException, "Cause should be an UnsupportedEncodingException");
    }

    @ParameterizedTest(name = "[{index}] s=''{0}'' encoding=''{1}''")
    @MethodSource("normalEncodingProvider")
    void getBytes_variousInputs_returnsExpected(ArgumentsHolder holder) {
        // when
        byte[] result = StringUtilities.getBytes(holder.input, holder.encoding);
        // then
        if (holder.expected == null) {
            assertNull(result, "Expected null result for null input string");
        } else {
            assertNotNull(result, "Result should not be null for non‑null input");
            assertArrayEquals(holder.expected, result, () -> "Byte array does not match expected value for input: " + holder.input);
        }
    }

    @ParameterizedTest(name = "[{index}] s=''{0}'' encoding=''{1}'' expects {2}")
    @MethodSource("exceptionProvider")
    void getBytes_exceptionalScenarios_throwsExpected(ExceptionCase ec) {
        // when / then
        Throwable thrown = assertThrows(ec.expectedException, () -> StringUtilities.getBytes(ec.input, ec.encoding));
        // Additional checks for the IllegalArgumentException case
        if (thrown instanceof IllegalArgumentException) {
            assertTrue(thrown.getMessage().contains(ec.encoding), "Exception message should contain the unsupported encoding name");
            assertNotNull(thrown.getCause(), "Cause should be the original UnsupportedEncodingException");
            assertTrue(thrown.getCause() instanceof UnsupportedEncodingException, "Cause must be UnsupportedEncodingException");
        }
    }
}
