package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.nio.CharBuffer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharBuffer_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used by tests that need a real converter.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; use the default constructor.
        // If the library provides a no‑arg constructor for ConverterOptions we can use it.
        // Otherwise, create a new instance directly (the class has a public no‑arg constructor).
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // The library provides a no‑arg constructor for ConverterOptions.
        ConverterOptions options = new ConverterOptions();
        realConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
        realConverter = null;
    }

    private static Stream<Arguments> basicCases() {
        return Stream.of(// Standard String – no conversion needed
        Arguments.of("hello", null, "hello", null), // StringBuilder (implements CharSequence)
        Arguments.of(new StringBuilder("world"), null, "world", null), // Integer – uses toString()
        Arguments.of(Integer.valueOf(123), null, "123", null), // char[] – should become a String via new String(char[])
        Arguments.of(new char[] { 'a', 'b', 'c' }, null, "abc", null), // null source – CharBuffer.wrap(null) throws NPE
        Arguments.of(null, null, null, NullPointerException.class), // Mocked Converter – should be ignored
        Arguments.of("test", mock(Converter.class), "test", null));
    }

    private static Stream<Arguments> charSequenceCases() {
        return Stream.of(// StringBuffer
        Arguments.of(new StringBuffer("buffer"), null, "buffer"), // CharBuffer itself
        Arguments.of(CharBuffer.wrap("charbuf"), null, "charbuf"), // Sub‑sequence of a StringBuilder (CharSequence view)
        Arguments.of(((StringBuilder) new StringBuilder("abcd")).subSequence(1, 3), null, "bc"), // Unicode surrogate pair (emoji)
        Arguments.of("😀", null, "😀"));
    }

    @Test
    void toCharBuffer_givenString_returnsCharBufferWithSameContent() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(source, result.toString(), "CharBuffer content should match the original string");
    }

    @Test
    void toCharBuffer_givenInteger_returnsCharBufferWithNumberString() {
        // GIVEN
        Integer source = 42;
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals("42", result.toString(), "CharBuffer should contain the string representation of the integer");
    }

    @Test
    void toCharBuffer_givenCharArray_returnsCharBufferWithArrayContent() {
        // GIVEN
        char[] source = new char[] { 'a', 'b', 'c' };
        // The asString implementation converts a char[] to a String by using new String(char[])
        // (this is the typical behaviour of utility methods in this library).
        String expected = new String(source);
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(expected, result.toString(), "CharBuffer should contain the string representation of the char array");
    }

    @Test
    void toCharBuffer_givenNull_returnsCharBufferWithLiteralNullString() {
        // GIVEN
        Object source = null;
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        // The utility method asString(null) returns the literal "null".
        assertEquals("null", result.toString(), "CharBuffer should contain the literal string \"null\" when the source is null");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("basicCases")
    void toCharBuffer_basicCases(Object from, Converter converter, String expectedContent, Class<? extends Throwable> expectedException) {
        // Use the supplied converter (may be null or a mock). If null, pass null directly.
        // may be null
        Converter usedConverter = converter;
        if (expectedException != null) {
            // Expect an exception when invoking the method.
            assertThrows(expectedException, () -> StringConversions.toCharBuffer(from, usedConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
        } else {
            CharBuffer result = StringConversions.toCharBuffer(from, usedConverter);
            assertNotNull(result, "Resulting CharBuffer must not be null");
            assertEquals(expectedContent, result.toString(), "CharBuffer content should match expected string");
        }
    }

    @ParameterizedTest(name = "[{index}] CharSequence from={0}")
    @MethodSource("charSequenceCases")
    void toCharBuffer_charSequenceTypes(Object from, Converter converter, String expected) {
        CharBuffer result = StringConversions.toCharBuffer(from, converter);
        assertNotNull(result, "Resulting CharBuffer must not be null");
        assertEquals(expected, result.toString(), "CharBuffer should contain the expected string representation");
    }

    @Test
    void toCharBuffer_toStringThrowsIllegalStateException() {
        // An anonymous object whose toString throws IllegalStateException
        Object bad = new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("toString failure");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> StringConversions.toCharBuffer(bad, null), "Exception from toString() should propagate");
        assertEquals("toString failure", ex.getMessage());
    }

    @Test
    void toCharBuffer_charSequenceToStringThrowsRuntimeException() {
        // Custom CharSequence whose toString throws RuntimeException
        CharSequence badCs = new CharSequence() {

            @Override
            public int length() {
                return 1;
            }

            @Override
            public char charAt(int index) {
                return 'a';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return this;
            }

            @Override
            public String toString() {
                throw new RuntimeException("cs toString");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toCharBuffer(badCs, null), "RuntimeException from CharSequence.toString() should propagate");
        assertEquals("cs toString", ex.getMessage());
    }
}
