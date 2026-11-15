package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class ByteArrayConversions_toCharBuffer_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared Converter instance used for the tests that do not need a custom mock.
     */
    private static Converter defaultConverter;

    class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("bad toString");
        }
    }

    /**
     * Dummy checked exception used in the test suite to emulate the real
     * {@code ConversionException} that the library may throw.
     */
    class ConversionException extends RuntimeException {

        ConversionException(String message) {
            super(message);
        }
    }

    class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("bad toString");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance; an empty one is sufficient for the simple conversions we test.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies all basic and edge‑case scenarios where no custom Converter logic is required.
     */
    private static Stream<TestCase> basicCases() {
        return Stream.of(new TestCase("Hello World", null, CharBuffer.wrap("Hello World"), null, "plain String, null converter"), new TestCase(new StringBuilder("builder"), null, CharBuffer.wrap("builder"), null, "StringBuilder, null converter"), new TestCase(new StringBuffer("buffer"), null, CharBuffer.wrap("buffer"), null, "StringBuffer, null converter"), new TestCase(new byte[] { 65, 66, 67 }, null, CharBuffer.wrap("ABC"), null, "byte[] -> \"ABC\""), new TestCase(null, null, CharBuffer.wrap("null"), null, "null source should become literal \"null\""), new TestCase("Unicode \u00A9", null, CharBuffer.wrap("Unicode ©"), null, "Unicode copyright character"), new TestCase("", null, CharBuffer.wrap(""), null, "empty string results in empty CharBuffer"), new TestCase("   leading and trailing   ", null, CharBuffer.wrap("   leading and trailing   "), null, "whitespace must be preserved"), new TestCase(new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        }, null, CharBuffer.wrap("custom"), null, "object with overridden toString"), new TestCase(Integer.valueOf(12345), null, CharBuffer.wrap("12345"), null, "Integer wrapper"), new TestCase(Boolean.TRUE, null, CharBuffer.wrap("true"), null, "Boolean wrapper"), new TestCase(new char[] { 'x', 'y', 'z' }, null, CharBuffer.wrap("xyz"), null, "char[] conversion"), new TestCase(new int[] { 1, 2, 3 }, null, CharBuffer.wrap("[1, 2, 3]"), null, "int[] uses Arrays.toString"), new TestCase(new char[] {}, null, CharBuffer.wrap(""), null, "empty char[]"), new TestCase(CharBuffer.wrap("bufferInput"), null, CharBuffer.wrap("bufferInput"), null, "already a CharBuffer"), new TestCase(createPartialCharBuffer(), null, CharBuffer.wrap("abc"), null, "partially filled CharBuffer"), new TestCase(ByteBuffer.wrap(new byte[] { 68, 69, 70 }), null, CharBuffer.wrap("DEF"), null, "ByteBuffer -> \"DEF\""), new TestCase("\uD83D\uDE00", null, CharBuffer.wrap("\uD83D\uDE00"), null, "surrogate pair (😀)"), new TestCase(String.join("", Collections.nCopies(10_000, "a")), null, CharBuffer.wrap(String.join("", Collections.nCopies(10_000, "a"))), null, "large 10,000‑character string"), new TestCase(Arrays.asList("a", "b", "c"), null, CharBuffer.wrap("[a, b, c]"), null, "collection toString representation"));
    }

    /**
     * Supplies cases that involve a custom {@link Converter}.
     */
    private static Stream<TestCase> converterCases() {
        // Mock that doubles an integer and returns a String
        Converter doubleIntConverter = Mockito.mock(Converter.class);
        Mockito.when(doubleIntConverter.convert(any())).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof Number) {
                return String.valueOf(((Number) arg).intValue() * 2);
            }
            return null;
        });
        // Mock that returns a StringBuilder
        Converter sbConverter = Mockito.mock(Converter.class);
        Mockito.when(sbConverter.convert(any())).thenReturn(new StringBuilder("converted"));
        // Mock that returns null
        Converter nullReturningConverter = Mockito.mock(Converter.class);
        Mockito.when(nullReturningConverter.convert(any())).thenReturn(null);
        // Mock that throws RuntimeException
        Converter runtimeExceptionConverter = Mockito.mock(Converter.class);
        Mockito.when(runtimeExceptionConverter.convert(any())).thenThrow(new RuntimeException("converter failure"));
        // Mock that throws a checked ConversionException (assume it extends RuntimeException for test simplicity)
        Converter checkedExceptionConverter = Mockito.mock(Converter.class);
        Mockito.when(checkedExceptionConverter.convert(any())).thenThrow(new ConversionException("checked conversion error"));
        // Mock that returns an object whose toString throws
        class BadToString {

            @Override
            public String toString() {
                throw new RuntimeException("bad toString");
            }
        }
        Converter badToStringConverter = Mockito.mock(Converter.class);
        Mockito.when(badToStringConverter.convert(any())).thenReturn(new BadToString());
        // Mock that throws NullPointerException when input is null
        Converter nullInputConverter = Mockito.mock(Converter.class);
        Mockito.when(nullInputConverter.convert(null)).thenThrow(new NullPointerException("null input not allowed"));
        return Stream.of(new TestCase(123, doubleIntConverter, CharBuffer.wrap("246"), null, "custom converter doubles integer"), new TestCase("orig", sbConverter, CharBuffer.wrap("converted"), null, "custom converter returns StringBuilder"), new TestCase("any", nullReturningConverter, CharBuffer.wrap("null"), null, "custom converter returns null => literal \"null\""), new TestCase("test", runtimeExceptionConverter, null, RuntimeException.class, "custom converter throws RuntimeException"), new TestCase("any", checkedExceptionConverter, null, ConversionException.class, "custom converter throws checked ConversionException"), new TestCase(123, badToStringConverter, null, RuntimeException.class, "converter returns object whose toString throws"), new TestCase(null, nullInputConverter, null, NullPointerException.class, "converter receives null and throws NPE"));
    }

    /**
     * Supplies cases where the source object's {@code toString()} itself throws.
     */
    private static Stream<TestCase> sourceToStringExceptionCases() {
        Object badObject = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("source toString failure");
            }
        };
        return Stream.of(new TestCase(badObject, null, null, RuntimeException.class, "source object's toString throws RuntimeException"));
    }

    /**
     * Supplies cases that provoke buffer‑related exceptions.
     */
    private static Stream<TestCase> bufferExceptionCases() {
        // ByteBuffer with position == limit (no remaining bytes) – decoding may cause BufferUnderflowException
        ByteBuffer underflowBuffer = ByteBuffer.allocate(5);
        // limit is also 5
        underflowBuffer.position(5);
        return Stream.of(new TestCase(underflowBuffer, null, null, BufferUnderflowException.class, "ByteBuffer with no remaining bytes should cause BufferUnderflowException"));
    }

    /**
     * Supplies cases where a CharSequence implementation misbehaves.
     */
    private static Stream<TestCase> badCharSequenceCases() {
        CharSequence badSeq = new CharSequence() {

            @Override
            public int length() {
                throw new IllegalStateException("bad length");
            }

            @Override
            public char charAt(int index) {
                return 'x';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return this;
            }

            @Override
            public String toString() {
                return "unused";
            }
        };
        return Stream.of(new TestCase(badSeq, null, null, IllegalStateException.class, "CharSequence.length() throws IllegalStateException"));
    }

    /**
     * Creates a CharBuffer that contains the characters "abc" and is ready for reading.
     */
    private static CharBuffer createPartialCharBuffer() {
        CharBuffer buf = CharBuffer.allocate(10);
        buf.put("abc");
        // position -> 0, limit -> 3
        buf.flip();
        return buf;
    }

    @Test
    void toCharBuffer_givenString_returnsCharBufferWithSameContent() {
        // GIVEN
        String input = "Hello, World!";
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(input, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(input, result.toString(), "CharBuffer should contain the original string");
    }

    @Test
    void toCharBuffer_givenByteArray_returnsCharBufferWithDecodedString() {
        // GIVEN
        // default charset – same bytes we expect to see as a string
        byte[] input = "ByteArray".getBytes();
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(input, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(new String(input), result.toString(), "CharBuffer should contain the string decoded from the byte array");
    }

    @Test
    void toCharBuffer_givenStringBuilder_returnsCharBufferWithSameContent() {
        // GIVEN
        StringBuilder sb = new StringBuilder("StringBuilderContent");
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(sb, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(sb.toString(), result.toString(), "CharBuffer should contain the content of the StringBuilder");
    }

    @Test
    void toCharBuffer_givenStringBuffer_returnsCharBufferWithSameContent() {
        // GIVEN
        StringBuffer sbuf = new StringBuffer("StringBufferContent");
        // WHEN
        CharBuffer result = ByteArrayConversions.toCharBuffer(sbuf, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(sbuf.toString(), result.toString(), "CharBuffer should contain the content of the StringBuffer");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource({ "basicCases", "converterCases", "sourceToStringExceptionCases", "bufferExceptionCases", "badCharSequenceCases" })
    void toCharBuffer_variousScenarios(TestCase tc) {
        // Arrange
        Object from = tc.from();
        Converter conv = tc.converter() != null ? tc.converter() : defaultConverter;
        // Act & Assert
        if (tc.expectedException() != null) {
            Throwable thrown = assertThrows(tc.expectedException(), () -> ByteArrayConversions.toCharBuffer(from, conv), tc.description());
            // For RuntimeException cases we also verify the message when possible
            if (thrown.getMessage() != null && !thrown.getMessage().isEmpty()) {
                // no strict check – just ensure a non‑empty message is present
                assertFalse(thrown.getMessage().isBlank(), "Exception message should not be blank");
            }
        } else {
            CharBuffer result = ByteArrayConversions.toCharBuffer(from, conv);
            assertNotNull(result, "Resulting CharBuffer must not be null – " + tc.description());
            // Expected CharBuffer is built via CharBuffer.wrap(String)
            CharBuffer expected = (CharBuffer) tc.expectedResult();
            assertEquals(expected.toString(), result.toString(), "CharBuffer content mismatch – " + tc.description());
        }
    }
}
