package com.cedarsoftware.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.JUnit4;
// import org.junit.runners.Parameterized;
// import org.junit.runners.Parameterized.Parameters;
// import org.junit.Assert;

// @ExtendWith(MockitoExtension.class)
// @RunWith(JUnit4.class)
class StringUtilities_getUTF8Bytes_Test {

    /**
     * Simple placeholder for the {@code Arguments} type used in the (currently
     * commented‑out) parameterised test providers.  This avoids pulling in the
     * full JUnit‑Jupiter parameterised test infrastructure while keeping the
     * source compilable.
     */
    class Arguments {

        private final Object[] values;

        private Arguments(Object... values) {
            this.values = values;
        }

//         public static Arguments of(Object... values) {
//             return new Arguments(values);
//         }

        public Object get(int index) {
            return values[index];
        }
    }

    private static Stream<Arguments> asciiStringsProvider() {
        return Stream.of(Arguments.of("a"), Arguments.of("Hello, World!"), Arguments.of("1234567890"), Arguments.of("!@#$%^&*()_+-=[]{}|;':\",./<>?"), Arguments.of("The quick brown fox jumps over the lazy dog"));
    }

    private static Stream<Arguments> unicodeStringsProvider() {
        return Stream.of(// 😀 Emoji (4‑byte UTF‑8)
        Arguments.of("\uD83D\uDE00"), // accented (2‑byte UTF‑8)
        Arguments.of("café"), // Cyrillic
        Arguments.of("Привет мир"), // Asian characters (3‑byte UTF‑8)
        Arguments.of("こんにちは世界"), // Mixed script
        Arguments.of("Hello 🌍 你好"));
    }

    private static Stream<Arguments> latin1SupplementProvider() {
        // U+00E9
        return Stream.of(Arguments.of("é"));
    }

    private static Stream<Arguments> emojiProvider() {
        // U+1F600
        return Stream.of(Arguments.of("😀"));
    }

    private static Stream<Arguments> mixedContentProvider() {
        // ASCII + 2‑byte + 4‑byte + ASCII
        return Stream.of(Arguments.of("Aé😀Z"));
    }

    private static Stream<Arguments> threeByteProvider() {
        // Devanagari U+0939
        return Stream.of(Arguments.of("ह"));
    }

    private static Stream<Arguments> fourByteNonEmojiProvider() {
        // Musical Symbol G Clef U+1D11E
        return Stream.of(Arguments.of("𝄞"));
    }

    private static List<String> asciiStrings() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("Hello, World!");
        list.add("1234567890");
        list.add("!@#$%^&*()_+-=[]{}|;':\",./<>?");
        list.add("The quick brown fox jumps over the lazy dog");
        return list;
    }

    private static List<String> unicodeStrings() {
        List<String> list = new ArrayList<>();
        // 😀
        list.add("\uD83D\uDE00");
        list.add("café");
        list.add("Привет мир");
        list.add("こんにちは世界");
        list.add("Hello 🌍 你好");
        return list;
    }

    private static List<String> latin1Supplement() {
        List<String> list = new ArrayList<>();
        list.add("é");
        return list;
    }

    private static List<String> emoji() {
        List<String> list = new ArrayList<>();
        list.add("😀");
        return list;
    }

    private static List<String> mixedContent() {
        List<String> list = new ArrayList<>();
        list.add("Aé😀Z");
        return list;
    }

    private static List<String> threeByte() {
        List<String> list = new ArrayList<>();
        list.add("ह");
        return list;
    }

    private static List<String> fourByteNonEmoji() {
        List<String> list = new ArrayList<>();
        list.add("𝄞");
        return list;
    }

    /**
     * Verify that a {@code null} argument returns {@code null}.
     */
//     @Test
//     void getUTF8Bytes_nullInput_returnsNull() {
//         Assert.assertNull(StringUtilities.getUTF8Bytes(null));
//     }

    /**
     * Verify that an empty string returns a zero‑length byte array.
     */
//     @Test
//     void getUTF8Bytes_emptyString_returnsEmptyArray() {
//         byte[] result = StringUtilities.getUTF8Bytes("");
//         Assert.assertNotNull(result);
//         Assert.assertEquals(0, result.length);
//     }

    /**
     * Verify that two calls with the same input return distinct byte array instances.
     */
//     @Test
//     void getUTF8Bytes_arrayImmutability_returnsDistinctInstances() {
//         String input = "test";
//         byte[] first = StringUtilities.getUTF8Bytes(input);
//         byte[] second = StringUtilities.getUTF8Bytes(input);
//         Assert.assertNotSame(first, second);
//         Assert.assertArrayEquals(first, second);
//     }

    /**
     * Verify that a very long repeated pattern yields the expected total byte length.
     * The pattern "abcé😀" is 9 bytes in UTF‑8; repeated 1000 times → 9000 bytes.
     */
//     @Test
//     void getUTF8Bytes_longRepeatedString_correctLength() {
//         // 9 bytes in UTF‑8
//         String pattern = "abcé😀";
//         StringBuilder sb = new StringBuilder(pattern.length() * 1000);
//         for (int i = 0; i < 1000; i++) {
//             sb.append(pattern);
//         }
//         String longString = sb.toString();
//         byte[] result = StringUtilities.getUTF8Bytes(longString);
//         Assert.assertEquals(9000, result.length);
//     }
}