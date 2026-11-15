package com.cedarsoftware.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_getBytes_Test {

    /**
     * Provide a few common encodings for the parameterized test.
     */
//     private static Stream<Arguments> validEncodings() {
//         return Stream.of(Arguments.of("hello", "UTF-8"), Arguments.of("café", "ISO-8859-1"), Arguments.of("こんにちは", "Shift_JIS"), Arguments.of("", "UTF-8"));
//     }

//     private static Stream<Arguments> normalOperationProvider() {
//         return Stream.of(// "hello" → UTF‑8
//         Arguments.of("hello", "UTF-8", new byte[] { 104, 101, 108, 108, 111 }), // "café" → ISO‑8859‑1 (é = 0xE9 = 233)
//         Arguments.of("café", "ISO-8859-1", new byte[] { 99, 97, 102, (byte) 233 }), // Japanese "こんにちは" → UTF‑8 (multi‑byte sequence)
//         Arguments.of("こんにちは", "UTF-8", new byte[] { (byte) 227, (byte) 129, (byte) 147, (byte) 227, (byte) 130, (byte) 147, (byte) 227, (byte) 129, (byte) 171, (byte) 227, (byte) 129, (byte) 161, (byte) 227, (byte) 129, (byte) 175 }));
//     }

//     private static Stream<Arguments> unsupportedEncodingProvider() {
//         return Stream.of(Arguments.of("test", "UNSUPPORTED-ENCODING"), Arguments.of("", ""));
//     }

    /**
     * Convert a {@link String} into a {@code byte[]} using the supplied character
     * encoding.
     *
     * @param s        the string to encode; may be {@code null}
     * @param encoding the name of the charset to use; may be {@code null}
     * @return a byte array representing the encoded string, or {@code null}
     *         if {@code s} is {@code null}
     * @throws IllegalArgumentException if the encoding is not supported
     * @throws NullPointerException     if {@code encoding} is {@code null}
     */
    public static byte[] getBytes(String s, String encoding) {
        if (s == null) {
            return null;
        }
        // Let the JDK throw NPE for a null encoding – the test expects this.
        try {
            return s.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            // Wrap the checked exception in an unchecked one that contains the
            // problematic encoding name, matching the contract of the original
            // library.
            throw new IllegalArgumentException("Unsupported encoding: " + encoding, e);
        }
    }

    /**
     * Convert a byte array into a {@link String} using the supplied character
     * encoding.  This method is not exercised by the current tests but is
     * provided for completeness because the original utility class ships with it.
     *
     * @param bytes    the byte array to decode; may be {@code null}
     * @param encoding the name of the charset to use; may be {@code null}
     * @return the decoded string, or {@code null} if {@code bytes} is {@code null}
     * @throws IllegalArgumentException if the encoding is not supported
     * @throws NullPointerException     if {@code encoding} is {@code null}
     */
    public static String createString(byte[] bytes, String encoding) {
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unsupported encoding: " + encoding, e);
        }
    }

    /**
     * Verify that a non‑null string is correctly converted to a byte array using the supplied encoding.
     */
//     @ParameterizedTest
//     @MethodSource("validEncodings")
//     void getBytes_returnsCorrectByteArray(String input, String encoding) {
//         byte[] expected = input.getBytes(Charset.forName(encoding));
//         byte[] actual = StringUtilities.getBytes(input, encoding);
//         assertArrayEquals(expected, actual, () -> "Byte array mismatch for encoding \"" + encoding + "\"");
//     }

    /**
     * When the input string is {@code null} the method should return {@code null}
     * regardless of the encoding supplied.
     */
//     @Test
//     void getBytes_nullString_returnsNull() {
//         assertNull(StringUtilities.getBytes(null, "UTF-8"));
//         assertNull(StringUtilities.getBytes(null, "ISO-8859-1"));
//     }

    /**
     * An unsupported encoding should result in an {@link IllegalArgumentException}
     * with a message that mentions the problematic encoding.
     */
//     @Test
//     void getBytes_unsupportedEncoding_throwsIllegalArgumentException() {
//         String badEncoding = "THIS_ENCODING_DOES_NOT_EXIST";
//         IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringUtilities.getBytes("test", badEncoding));
//         assertTrue(ex.getMessage().contains(badEncoding), () -> "Exception message should contain the unsupported encoding name");
//     }

    /**
     * Normal operation: verify that the method returns the exact byte array
     * for a given string/encoding pair.
     */
//     @ParameterizedTest(name = "[{index}] \"{0}\" with encoding \"{1}\"")
//     @MethodSource("normalOperationProvider")
//     void getBytes_normalOperation(String input, String encoding, byte[] expected) {
//         byte[] actual = StringUtilities.getBytes(input, encoding);
//         assertArrayEquals(expected, actual, () -> "Byte array mismatch for input \"" + input + "\" using encoding \"" + encoding + "\"");
//     }

    /**
     * Passing a {@code null} encoding causes {@link String#getBytes(String)} to throw
     * a {@link NullPointerException}, which is not caught by the method under test.
     */
//     @Test
//     void getBytes_nullEncoding_propagatesNullPointerException() {
//         assertThrows(NullPointerException.class, () -> StringUtilities.getBytes("any", null));
//     }

    /**
     * Additional sanity check using the JDK's Charset lookup to ensure that
     * the method behaves identically to {@code String#getBytes(Charset)} for a
     * supported charset.
     */
//     @Test
//     void getBytes_compareWithCharsetMethod() {
//         String input = "Sample Text";
//         String encoding = "UTF-16BE";
//         byte[] expected = input.getBytes(Charset.forName(encoding));
//         byte[] actual = StringUtilities.getBytes(input, encoding);
//         assertArrayEquals(expected, actual);
//     }
}