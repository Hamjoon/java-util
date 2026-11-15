package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Arrays;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toByteArray_Test {

    /**
     * Shared {@link Converter} used by the tests.
     */
    private static Converter converter;

    /**
     * Shared default {@link Converter} used by the majority of tests.
     */
    private static Converter defaultConverter;

    /**
     * A {@link Converter} that always returns a predefined {@code byte[]} (or {@code null})
     * when asked to convert to {@code byte[].class}. All other conversions fall back to the
     * standard implementation.
     */
    private static class FixedByteArrayConverter extends Converter {

        private final byte[] result;

        FixedByteArrayConverter(byte[] result) {
            super(new ConverterOptions());
            this.result = result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object src, Class<T> target) {
            if (target == byte[].class) {
                return (T) result;
            }
            return super.convert(src, target);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance; the default options are sufficient for our tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * Supplies a wide range of source objects for the normal conversion test.
     */
    private static Stream<Arguments> normalConversionProvider() {
        // simple CharBuffer
        CharBuffer cbHello = CharBuffer.wrap("hello");
        // empty CharBuffer
        CharBuffer cbEmpty = CharBuffer.allocate(0);
        // char array "Java"
        char[] charsJava = new char[] { 'J', 'a', 'v', 'a' };
        // empty char[]
        char[] charsEmpty = new char[0];
        // plain strings
        String strCedar = "Cedar";
        String strEmpty = "";
        // StringBuilder / StringBuffer
        StringBuilder sbuilder = new StringBuilder("builder");
        StringBuffer sbuffer = new StringBuffer("buffer");
        // byte[] and empty byte[]
        byte[] bytesABC = new byte[] { 65, 66, 67 };
        byte[] bytesEmpty = new byte[0];
        // ByteBuffer wrapping bytes
        ByteBuffer bbDef = ByteBuffer.wrap(new byte[] { 68, 69, 70 });
        ByteBuffer bbEmpty = ByteBuffer.allocate(0);
        // Unicode strings
        // U+20AC
        String euro = "€";
        // U+1F600
        String emoji = "😀";
        // read‑only CharBuffer
        CharBuffer cbReadOnly = CharBuffer.wrap("readonly").asReadOnlyBuffer();
        // large char array (10 000 'a')
        char[] large = new char[10_000];
        Arrays.fill(large, 'a');
        return Stream.of(Arguments.of(cbHello), Arguments.of(cbEmpty), Arguments.of(charsJava), Arguments.of(charsEmpty), Arguments.of(strCedar), Arguments.of(strEmpty), Arguments.of(sbuilder), Arguments.of(sbuffer), Arguments.of(bytesABC), Arguments.of(bytesEmpty), Arguments.of(bbDef), Arguments.of(bbEmpty), Arguments.of(euro), Arguments.of(emoji), Arguments.of(cbReadOnly), Arguments.of(large));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(42)), Arguments.of(new Object()));
    }

    /**
     * Verify that a {@link CharBuffer} is correctly converted to a byte array.
     * The expected result is obtained by letting the {@link Converter} perform the conversion
     * directly, then comparing it with the result from {@code CharBufferConversions.toByteArray}.
     */
    @Test
    void shouldConvertCharBufferToByteArray() {
        // GIVEN
        // contains a non‑ASCII space to exercise charset handling
        CharBuffer source = CharBuffer.wrap("Hello World");
        // WHEN
        byte[] expected = converter.convert(source, byte[].class);
        byte[] actual = CharBufferConversions.toByteArray(source, converter);
        // THEN
        assertArrayEquals(expected, actual, "Byte array produced by CharBufferConversions should match the Converter's direct conversion");
    }

    /**
     * Verify that {@code null} as the source object results in {@code null} output.
     * This mirrors the behaviour of {@link ByteBufferConversions#toByteArray}.
     */
    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        byte[] actual = CharBufferConversions.toByteArray(source, defaultConverter);
        // THEN
        assertNull(actual, "Converting a null source should yield a null byte array");
    }

    /**
     * Ensure that {@code CharBufferConversions.toByteArray} delegates to the static methods of
     * {@link ByteBufferConversions}. The static methods are mocked so that we can verify the
     * interaction without relying on their actual implementation.
     */
    @Test
    void shouldDelegateToByteBufferConversions() {
        // GIVEN
        Object source = "irrelevant for the mock";
        ByteBuffer mockedByteBuffer = ByteBuffer.wrap(new byte[] { 10, 20, 30 });
        byte[] mockedResult = new byte[] { 10, 20, 30 };
        try (MockedStatic<ByteBufferConversions> mocked = mockStatic(ByteBufferConversions.class)) {
            // Mock the intermediate conversion to ByteBuffer
            mocked.when(() -> ByteBufferConversions.toByteBuffer(eq(source), eq(converter))).thenReturn(mockedByteBuffer);
            // Mock the final conversion from ByteBuffer to byte[]
            mocked.when(() -> ByteBufferConversions.toByteArray(eq(mockedByteBuffer), eq(converter))).thenReturn(mockedResult);
            // WHEN
            byte[] actual = CharBufferConversions.toByteArray(source, converter);
            // THEN
            assertSame(mockedResult, actual, "The method should return exactly the array supplied by ByteBufferConversions.toByteArray");
            // Verify the delegation order
            mocked.verify(() -> ByteBufferConversions.toByteBuffer(source, converter));
            mocked.verify(() -> ByteBufferConversions.toByteArray(mockedByteBuffer, converter));
        }
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the “normal” conversion paths.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => source={0}")
    @MethodSource("normalConversionProvider")
    void shouldConvertVariousSourcesToByteArray(Object source) {
        // GIVEN a source object (may be CharBuffer, char[], String, …)
        // WHEN we ask CharBufferConversions to produce a byte[]
        byte[] actual = CharBufferConversions.toByteArray(source, defaultConverter);
        // THEN the result must be identical to what the default Converter would produce directly.
        byte[] expected = defaultConverter.convert(source, byte[].class);
        assertArrayEquals(expected, actual, () -> "Conversion result differs for source: " + source);
    }

    /* --------------------------------------------------------------
     *  Null converter handling
     * -------------------------------------------------------------- */
    @Test
    void shouldThrowWhenConverterIsNull() {
        // GIVEN
        String source = "test";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteArray(source, null), "Passing a null Converter must result in NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Unsupported source types
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => unsupported={0}")
    @MethodSource("unsupportedSourceProvider")
    void shouldRejectUnsupportedSourceTypes(Object unsupported) {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toByteArray(unsupported, defaultConverter), "Unsupported source type should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  Custom Converter – returns a fixed byte array
     * -------------------------------------------------------------- */
    @Test
    void shouldHonorCustomConverterReturningFixedBytes() {
        // GIVEN a custom converter that always returns {0x01, 0x02}
        byte[] fixed = new byte[] { 0x01, 0x02 };
        Converter custom = new FixedByteArrayConverter(fixed);
        String source = "any string";
        // WHEN
        byte[] actual = CharBufferConversions.toByteArray(source, custom);
        // THEN
        assertArrayEquals(fixed, actual, "Custom converter should dictate the returned byte array");
    }

    /* --------------------------------------------------------------
     *  Custom Converter – returns null
     * -------------------------------------------------------------- */
    @Test
    void shouldPropagateNullFromCustomConverter() {
        // GIVEN a custom converter that returns null for byte[] conversion
        Converter custom = new FixedByteArrayConverter(null);
        String source = "any";
        // WHEN
        byte[] actual = CharBufferConversions.toByteArray(source, custom);
        // THEN
        assertNull(actual, "If the custom converter returns null, the method must return null");
    }

    /* --------------------------------------------------------------
     *  Custom Converter – throws RuntimeException
     * -------------------------------------------------------------- */
    @Test
    void shouldPropagateExceptionFromCustomConverter() {
        // GIVEN a converter that throws a RuntimeException on conversion
        Converter throwing = mock(Converter.class);
        when(throwing.convert(any(), eq(byte[].class))).thenThrow(new RuntimeException("conversion error"));
        // Mock the static delegation chain so that the exception is triggered
        try (MockedStatic<ByteBufferConversions> mocked = mockStatic(ByteBufferConversions.class)) {
            // toByteBuffer is called first – we let it return a dummy ByteBuffer
            mocked.when(() -> ByteBufferConversions.toByteBuffer(any(), any())).thenReturn(ByteBuffer.wrap(new byte[0]));
            // toByteArray will delegate to the mocked converter (via the mocked static call)
            mocked.when(() -> ByteBufferConversions.toByteArray(any(), eq(throwing))).thenAnswer(invocation -> {
                // simulate the real call that would invoke the converter and thus throw
                throw new RuntimeException("conversion error");
            });
            // WHEN / THEN
            RuntimeException ex = assertThrows(RuntimeException.class, () -> CharBufferConversions.toByteArray("fail", throwing), "Exception from the converter must be propagated");
            assertEquals("conversion error", ex.getMessage());
        }
    }
}
