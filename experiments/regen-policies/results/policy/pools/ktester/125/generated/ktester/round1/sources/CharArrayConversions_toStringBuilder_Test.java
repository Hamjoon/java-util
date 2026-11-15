package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.nio.ByteBuffer;
import java.nio.charset.MalformedInputException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toStringBuilder_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Holds a single test case definition.
     */
    private static final class TestCaseData {

        final Object from;

        final Converter converter;

        // null when an exception is expected
        final String expectedString;

        // null when normal result is expected
        final Class<? extends Throwable> expectedException;

        final String description;

        TestCaseData(Object from, Converter converter, String expectedString, Class<? extends Throwable> expectedException, String description) {
            this.from = from;
            this.converter = converter;
            this.expectedString = expectedString;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter is fully functional without any custom configuration.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Supplies all test cases defined in the JSON document.
     */
    private static Stream<Arguments> allTestCases() {
        return Stream.of(/* ---------- Successful conversions ---------- */
        new TestCaseData("hello", IdentityConverter.INSTANCE, "hello", null, "Converts a plain String to a StringBuilder via toCharBuffer."), new TestCaseData(new char[] { 'a', 'b', 'c' }, IdentityConverter.INSTANCE, "abc", null, "Converts a char[] to a StringBuilder."), new TestCaseData(CharBuffer.wrap("world"), IdentityConverter.INSTANCE, "world", null, "Converts an existing CharBuffer to a StringBuilder."), new TestCaseData(ByteBuffer.wrap(new byte[] { 'x', 'y' }), ByteToCharConverter.UTF8_INSTANCE, "xy", null, "Converts a ByteBuffer (UTF‑8 encoded) to a StringBuilder using a UTF‑8 decoder."), /* ---------- Edge cases – empty inputs ---------- */
        new // empty String
        TestCaseData(// empty String
        "", IdentityConverter.INSTANCE, "", null, "Converts an empty String; result should be an empty StringBuilder."), new // empty char[]
        TestCaseData(// empty char[]
        new char[] {}, IdentityConverter.INSTANCE, "", null, "Converts an empty char array; result should be an empty StringBuilder."), new TestCaseData(CharBuffer.allocate(0), IdentityConverter.INSTANCE, "", null, "Converts an empty CharBuffer; result should be an empty StringBuilder."), new TestCaseData(ByteBuffer.allocate(0), ByteToCharConverter.UTF8_INSTANCE, "", null, "Converts an empty ByteBuffer using UTF‑8 decoder; result should be an empty StringBuilder."), /* ---------- Null handling ---------- */
        new TestCaseData(null, IdentityConverter.INSTANCE, null, NullPointerException.class, "Passing a null source object should result in a NullPointerException."), new TestCaseData("test", null, null, NullPointerException.class, "Passing a null Converter should result in a NullPointerException."), new TestCaseData(null, null, null, NullPointerException.class, "Both parameters are null; the method should fail fast with NullPointerException."), /* ---------- Non‑String source with generic ToString converter ---------- */
        new TestCaseData(Integer.valueOf(123), ToStringConverter.INSTANCE, "123", null, "Converts an Integer using a converter that calls Object.toString()."), /* ---------- Multibyte UTF‑8 characters ---------- */
        new TestCaseData(ByteBuffer.wrap(new byte[] { (byte) 0xC3, (byte) 0xA9 }), ByteToCharConverter.UTF8_INSTANCE, "é", null, "ByteBuffer holds the UTF‑8 encoding of 'é'; the UTF‑8 converter should decode it correctly."), /* ---------- ISO‑8859‑1 charset conversion ---------- */
        new TestCaseData(ByteBuffer.wrap(new byte[] { (byte) 0xE9 }), ByteToCharConverter.ISO_8859_1_INSTANCE, "é", null, "ISO‑8859‑1 converter decodes byte 0xE9 to 'é'."), /* ---------- Converter that throws runtime exception ---------- */
        new TestCaseData("any", FaultyConverter.INSTANCE, null, RuntimeException.class, "A converter deliberately throws an exception; the method should propagate the RuntimeException."), /* ---------- Exception scenarios not covered ---------- */
        new TestCaseData(new Object(), IdentityConverter.INSTANCE, null, ClassCastException.class, "Source object type is unsupported; IdentityConverter cannot handle it and a ClassCastException is thrown."), new TestCaseData(new byte[] { 0x00, 0x01 }, IdentityConverter.INSTANCE, null, IllegalArgumentException.class, "Byte array passed with a converter that does not know how to decode bytes; should raise IllegalArgumentException."), new TestCaseData("test", new // anonymous faulty converter
        Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new UnsupportedOperationException();
            }
        }, null, UnsupportedOperationException.class, "Converter throws UnsupportedOperationException; method must propagate it unchanged."), new TestCaseData(ByteBuffer.wrap(new byte[] { (byte) 0xFF }), ByteToCharConverter.UTF8_INSTANCE, null, MalformedInputException.class, "Invalid UTF‑8 byte sequence; UTF‑8 converter should raise MalformedInputException.")).map(tc -> Arguments.of(tc));
    }

    @Test
    @DisplayName("toStringBuilder converts a plain String")
    void testToStringBuilderFromString() {
        String source = "Hello, world!";
        StringBuilder result = CharArrayConversions.toStringBuilder(source, converter);
        assertNotNull(result);
        assertEquals(source, result.toString());
    }

    @Test
    @DisplayName("toStringBuilder converts a char array")
    void testToStringBuilderFromCharArray() {
        char[] source = new char[] { 'J', 'a', 'v', 'a' };
        StringBuilder result = CharArrayConversions.toStringBuilder(source, converter);
        assertNotNull(result);
        assertEquals(new String(source), result.toString());
    }

    @Test
    @DisplayName("toStringBuilder converts a CharBuffer")
    void testToStringBuilderFromCharBuffer() {
        CharBuffer source = CharBuffer.wrap("CharBuffer content");
        StringBuilder result = CharArrayConversions.toStringBuilder(source, converter);
        assertNotNull(result);
        assertEquals(source.toString(), result.toString());
    }

    @Test
    @DisplayName("toStringBuilder handles null input gracefully")
    void testToStringBuilderFromNull() {
        // According to Converter's contract, converting null to a CharBuffer yields an empty buffer.
        StringBuilder result = CharArrayConversions.toStringBuilder(null, converter);
        assertNotNull(result);
        assertEquals("", result.toString());
    }

    @Test
    @DisplayName("toStringBuilder works with a ByteBuffer that represents UTF‑8 characters")
    void testToStringBuilderFromByteBuffer() {
        // The Converter can turn a ByteBuffer into a CharBuffer using the platform charset.
        // default charset (UTF‑8 on most platforms)
        byte[] bytes = "bytes".getBytes();
        java.nio.ByteBuffer source = java.nio.ByteBuffer.wrap(bytes);
        StringBuilder result = CharArrayConversions.toStringBuilder(source, converter);
        assertNotNull(result);
        assertEquals("bytes", result.toString());
    }

    @ParameterizedTest(name = "[{index}] {0.description}")
    @MethodSource("allTestCases")
    void testToStringBuilderAllCases(TestCaseData data) {
        if (data.expectedException != null) {
            // Expect an exception – the exact type must be thrown (or a subtype).
            assertThrows(data.expectedException, () -> CharArrayConversions.toStringBuilder(data.from, data.converter), () -> "Expected exception " + data.expectedException.getSimpleName() + " for case: " + data.description);
        } else {
            // Normal conversion – verify the resulting StringBuilder content.
            StringBuilder sb = CharArrayConversions.toStringBuilder(data.from, data.converter);
            assertNotNull(sb, "Resulting StringBuilder should not be null for case: " + data.description);
            assertEquals(data.expectedString, sb.toString(), () -> "Unexpected StringBuilder content for case: " + data.description);
        }
    }
}
