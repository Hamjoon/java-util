package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toStringBuilder_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter with default options.
        // ConverterOptions has a no‑arg constructor that provides the standard configuration.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /**
     * Provides a stream of arguments for the parameterized tests.
     * Each element is an array: {description, fromObject, expectedResult, expectedExceptionClass}
     */
    private static Stream<Arguments> testCases() {
        return Stream.of(// Null input cases
        Arguments.of("Both arguments null", null, null, NullPointerException.class), Arguments.of("From null, converter null", null, null, NullPointerException.class), // CharBuffer cases
        Arguments.of("Empty CharBuffer", CharBuffer.wrap(""), "", null), // String cases
        Arguments.of("Empty String", "", "", null), // Byte array cases (real Converter handles UTF‑8)
        Arguments.of("Byte array 'Hello'", new byte[] { 72, 101, 108, 108, 111 }, "Hello", null), Arguments.of("Empty byte array", EMPTY_BYTE_ARRAY, "", null), // Unsupported type
        Arguments.of("Unsupported Object type", new Object(), null, IllegalArgumentException.class), // ByteBuffer supported without explicit converter
        Arguments.of("ByteBuffer containing ABC", ByteBuffer.wrap(new byte[] { 65, 66, 67 }), "ABC", null), // Large input
        Arguments.of("Large 10,000 'a' characters", "a".repeat(10_000), "a".repeat(10_000), null), // Non‑ASCII Unicode string
        Arguments.of("Non‑ASCII characters", "こんにちは", "こんにちは", null));
    }

    @Test
    void toStringBuilder_givenCharBuffer_returnsStringBuilderWithSameContent() {
        // given
        CharBuffer charBuffer = CharBuffer.wrap("hello");
        // when
        StringBuilder result = ByteBufferConversions.toStringBuilder(charBuffer, converter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals("hello", result.toString(), "StringBuilder content must match the CharBuffer source");
    }

    @Test
    void toStringBuilder_givenString_returnsStringBuilderWithSameContent() {
        // given
        String source = "world";
        // when
        StringBuilder result = ByteBufferConversions.toStringBuilder(source, converter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals("world", result.toString(), "StringBuilder content must match the original String");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void toStringBuilder_variousInputs(String description, Object from, String expectedContent, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // Expect an exception to be thrown
            assertThrows(expectedException, () -> ByteBufferConversions.toStringBuilder(from, converter), "Expected " + expectedException.getSimpleName() + " for case: " + description);
        } else {
            // Normal execution – verify the produced StringBuilder
            StringBuilder sb = ByteBufferConversions.toStringBuilder(from, converter);
            assertNotNull(sb, "Result should not be null for case: " + description);
            assertEquals(expectedContent, sb.toString(), "Content mismatch for case: " + description);
        }
    }
}
