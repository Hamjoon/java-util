package com.cedarsoftware.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@RunWith(Parameterized.class)
class StringUtilities_createUTF8String_Test {

    /**
     * Expected string result of the conversion.
     */
    private final String expected;

    /**
     * Input byte array to be converted.
     */
    private final byte[] input;

    /**
     * Optional description used only for readability in failure messages.
     */
    private final String description;

    /**
     * Supplies a stream of arguments: expected string and its UTF‑8 byte representation.
     */
    private static Stream<Arguments> validUtf8Provider() {
        return Stream.of(Arguments.of("", new byte[0]), Arguments.of("A", "A".getBytes(StandardCharsets.UTF_8)), Arguments.of("ñ", "ñ".getBytes(StandardCharsets.UTF_8)), Arguments.of("𐍈", "𐍈".getBytes(StandardCharsets.UTF_8)), Arguments.of("Привет", "Привет".getBytes(StandardCharsets.UTF_8)), Arguments.of("🌍🚀", "🌍🚀".getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Supplies a {@link Stream} of {@link Arguments} matching the JSON test cases.
     * The large repetitive‑pattern case is generated programmatically to keep the source
     * readable while still exercising the method with a very big payload.
     */
    private static Stream<Arguments> jsonBasedProvider() {
        List<Arguments> args = new ArrayList<>();
        // 1. null input – already covered by a dedicated test, but we keep it here for completeness
        args.add(Arguments.of(null, null, "null input should return null"));
        // 2. empty array
        args.add(Arguments.of("", new byte[0], "empty array should return empty string"));
        // 3. ASCII "Hello"
        args.add(Arguments.of("Hello", new byte[] { 72, 101, 108, 108, 111 }, "standard ASCII bytes are decoded correctly"));
        // 4. Multi‑byte check‑mark (U+2713)
        args.add(Arguments.of("✓", new byte[] { (byte) 0xE2, (byte) 0x9C, (byte) 0x93 }, "a valid three‑byte UTF‑8 sequence for CHECK MARK"));
        // 5. Mixed valid/invalid bytes (0, -1, 0, 127)
        args.add(Arguments.of("\u0000\uFFFD\u0000\u007F", new byte[] { 0, (byte) 0xFF, 0, 127 }, "invalid continuation byte -1 is replaced by U+FFFD"));
        // 6. Three NUL bytes
        args.add(Arguments.of("\u0000\u0000\u0000", new byte[] { 0, 0, 0 }, "array of NUL bytes should become a string of NUL characters"));
        // 7. Two sole continuation bytes (0x80)
        args.add(Arguments.of("\uFFFD\uFFFD", new byte[] { (byte) 0x80, (byte) 0x80 }, "illegal continuation bytes each become U+FFFD"));
        // 8. Overlong encoding of '/' (0xC0 0xAF)
        args.add(Arguments.of("\uFFFD\uFFFD", new byte[] { (byte) 0xC0, (byte) 0xAF }, "overlong encoding is malformed – each byte replaced by U+FFFD"));
        // 9. Grinning face emoji (U+1F600)
        args.add(Arguments.of("😀", new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }, "valid four‑byte UTF‑8 sequence for 😀"));
        // 10. Mixed string "Café 😊"
        args.add(Arguments.of("Café 😊", new byte[] { 67, 97, 102, (byte) 0xC3, (byte) 0xA9, 32, (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x8A }, "mix of ASCII, two‑byte accented char and four‑byte emoji"));
        // 11. Invalid bytes 0xFE 0xFF
        args.add(Arguments.of("\uFFFD\uFFFD", new byte[] { (byte) 0xFE, (byte) 0xFF }, "bytes 0xFE and 0xFF are never valid start bytes"));
        // 12. Very large repetitive pattern ("abc" repeated many times)
        int repeat = 1000;
        byte[] largePayload = new byte[repeat * 3];
        for (int i = 0; i < repeat; i++) {
            // 'a'
            largePayload[i * 3] = 97;
            // 'b'
            largePayload[i * 3 + 1] = 98;
            // 'c'
            largePayload[i * 3 + 2] = 99;
        }
        StringBuilder sb = new StringBuilder(repeat * 3);
        for (int i = 0; i < repeat; i++) {
            sb.append("abc");
        }
        args.add(Arguments.of(sb.toString(), largePayload, "large payload consisting of \"abc\" repeated 1000 times"));
        return args.stream();
    }

    /**
     * {@code null} input must result in {@code null}.
     */
    @Test
    void createUTF8String_nullInput_returnsNull() {
        assertNull(StringUtilities.createUTF8String(null), "null input should return null");
    }

    /**
     * An empty byte array should produce an empty string.
     */
    @Test
    void createUTF8String_emptyArray_returnsEmptyString() {
        assertEquals("", StringUtilities.createUTF8String(new byte[0]), "empty array should return empty string");
    }

    /**
     * Simple ASCII bytes are decoded correctly.
     */
    @Test
    void createUTF8String_asciiBytes_returnsCorrectString() {
        byte[] ascii = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        assertEquals("Hello, World!", StringUtilities.createUTF8String(ascii));
    }

    /**
     * Multi‑byte UTF‑8 characters (e.g., emojis, accented letters) are decoded correctly.
     */
    @Test
    void createUTF8String_multibyteCharacters_returnsCorrectString() {
        String original = "Café ☕️ 漢字 🚀";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        assertEquals(original, StringUtilities.createUTF8String(bytes));
    }

    /**
     * Malformed UTF‑8 sequences are replaced with the Unicode replacement character (U+FFFD).
     */
    @Test
    void createUTF8String_malformedUtf8_returnsStringWithReplacementChar() {
        // 0xC3 is a lead byte for a 2‑byte sequence, but the continuation byte is missing.
        byte[] malformed = new byte[] { (byte) 0xC3 };
        assertEquals("\uFFFD", StringUtilities.createUTF8String(malformed));
    }

    /**
     * Parameterized test covering a variety of valid UTF‑8 payloads.
     */
    @ParameterizedTest
    @MethodSource("validUtf8Provider")
    void createUTF8String_variousValidInputs_returnsExpected(String expected, byte[] input) {
        assertEquals(expected, StringUtilities.createUTF8String(input));
    }

    /**
     * Parameterized test that covers the exhaustive list of cases extracted from the JSON specification.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("jsonBasedProvider")
    void createUTF8String_jsonBasedCases(String expected, byte[] input, String description) {
        assertEquals(expected, StringUtilities.createUTF8String(input), description);
    }

    @Parameters(name = "[{index}] {2}")
    public static Collection<Object[]> data() {
        List<Object[]> args = new ArrayList<>();
        args.add(new Object[] { null, null, "null input should return null" });
        args.add(new Object[] { "", new byte[0], "empty array should return empty string" });
        args.add(new Object[] { "Hello", new byte[] { 72, 101, 108, 108, 111 }, "standard ASCII bytes are decoded correctly" });
        args.add(new Object[] { "✓", new byte[] { (byte) 0xE2, (byte) 0x9C, (byte) 0x93 }, "a valid three‑byte UTF‑8 sequence for CHECK MARK" });
        args.add(new Object[] { "\u0000\uFFFD\u0000\u007F", new byte[] { 0, (byte) 0xFF, 0, 127 }, "invalid continuation byte -1 is replaced by U+FFFD" });
        args.add(new Object[] { "\u0000\u0000\u0000", new byte[] { 0, 0, 0 }, "array of NUL bytes should become a string of NUL characters" });
        args.add(new Object[] { "\uFFFD\uFFFD", new byte[] { (byte) 0x80, (byte) 0x80 }, "illegal continuation bytes each become U+FFFD" });
        args.add(new Object[] { "\uFFFD\uFFFD", new byte[] { (byte) 0xC0, (byte) 0xAF }, "overlong encoding is malformed – each byte replaced by U+FFFD" });
        args.add(new Object[] { "😀", new byte[] { (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80 }, "valid four‑byte UTF‑8 sequence for 😀" });
        args.add(new Object[] { "Café 😊", new byte[] { 67, 97, 102, (byte) 0xC3, (byte) 0xA9, 32, (byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x8A }, "mix of ASCII, two‑byte accented char and four‑byte emoji" });
        args.add(new Object[] { "\uFFFD\uFFFD", new byte[] { (byte) 0xFE, (byte) 0xFF }, "bytes 0xFE and 0xFF are never valid start bytes" });
        int repeat = 1000;
        byte[] largePayload = new byte[repeat * 3];
        StringBuilder sb = new StringBuilder(repeat * 3);
        for (int i = 0; i < repeat; i++) {
            largePayload[i * 3] = 97;
            largePayload[i * 3 + 1] = 98;
            largePayload[i * 3 + 2] = 99;
            sb.append("abc");
        }
        args.add(new Object[] { sb.toString(), largePayload, "large payload consisting of \"abc\" repeated 1000 times" });
        return args;
    }

    @Test
    public void createUTF8String_parameterised() {
        // The description is included in the assertion message to make failures easy to locate.
        assertEquals(description, expected, StringUtilities.createUTF8String(input));
    }
}
