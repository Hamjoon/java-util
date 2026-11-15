package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toCharArray_Test {

    /**
     * Converter used in all test cases.
     */
    private Converter converter;

    /**
     * A real {@link Converter} used for the majority of test cases.
     */
    private Converter realConverter;

    /**
     * A mock {@link Converter} used for cases where we need to control its behaviour.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple POJO whose {@code toString()} returns a custom value.
     */
    private static final class TestObject {

        private final String value;

        TestObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Object whose {@code toString()} deliberately throws a {@link RuntimeException}.
     */
    private static final class ThrowingToString {

        private final String message;

        ThrowingToString(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            throw new RuntimeException(message);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The default ConverterOptions contain the standard conversion map.
        ConverterOptions options = new ConverterOptions();
        realConverter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
        converter = null;
    }

    private static Stream<Object> charSequenceProvider() {
        return Stream.of(new StringBuilder("BuilderTest"), new StringBuffer("BufferTest"));
    }

    @Test
    void toCharArray_givenString_returnsExactCharArray() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(source, converter);
        // THEN
        assertArrayEquals(source.toCharArray(), result, "The char array should match the characters of the original String");
    }

    @Test
    void toCharArray_givenByteArray_convertsUsingDefaultCharset() {
        // GIVEN
        // uses platform default charset
        byte[] source = "ByteArray".getBytes();
        // same conversion path as the Converter
        String expected = new String(source);
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(source, converter);
        // THEN
        assertArrayEquals(expected.toCharArray(), result, "The byte[] should be converted to a String and then to a matching char[]");
    }

    @Test
    void toCharArray_givenCharBuffer_returnsCorrectCharArray() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("CharBufferTest");
        // WHEN
        char[] result = ByteArrayConversions.toCharArray(source, converter);
        // THEN
        assertArrayEquals(source.toString().toCharArray(), result, "The CharBuffer should be converted to a String and then to a matching char[]");
    }

    @Test
    void toCharArray_givenNull_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharArray(source, converter), "A null source should cause a NullPointerException because toString returns null");
    }

    // -----------------------------------------------------------------
    @Test
    void toCharArray_givenBothNull_returnsLiteralNullString() {
        // According to the specification, when both arguments are null the
        // implementation should behave like String.valueOf(null) → "null".
        char[] result = ByteArrayConversions.toCharArray(null, null);
        assertArrayEquals("null".toCharArray(), result, "Both arguments null should produce the char array of the literal \"null\"");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("charSequenceProvider")
    void toCharArray_givenCharSequence_returnsCorrectCharArray(Object source) {
        // The source is either a StringBuilder or a StringBuffer.
        String expected = source.toString();
        char[] result = ByteArrayConversions.toCharArray(source, realConverter);
        assertArrayEquals(expected.toCharArray(), result, "CharSequence (" + source.getClass().getSimpleName() + ") should be converted via toString()");
    }

    // -----------------------------------------------------------------
    @Test
    void toCharArray_givenByteArray_utf8Conversion() {
        byte[] source = "Hello".getBytes(StandardCharsets.UTF_8);
        char[] result = ByteArrayConversions.toCharArray(source, realConverter);
        assertArrayEquals("Hello".toCharArray(), result, "Byte array representing UTF‑8 text must be converted to the correct char array");
    }

    @Test
    void toCharArray_givenByteArray_multibyteCharacter() {
        // [0xC3, 0xA9]
        byte[] source = "é".getBytes(StandardCharsets.UTF_8);
        char[] result = ByteArrayConversions.toCharArray(source, realConverter);
        assertArrayEquals("é".toCharArray(), result, "Multibyte UTF‑8 character should be decoded correctly");
    }

    // -----------------------------------------------------------------
    @Test
    void toCharArray_givenCustomObject_usesItsToString() {
        TestObject obj = new TestObject("CustomString");
        char[] result = ByteArrayConversions.toCharArray(obj, realConverter);
        assertArrayEquals("CustomString".toCharArray(), result, "The object's overridden toString() result must be used");
    }

    // -----------------------------------------------------------------
    @Test
    void toCharArray_converterThrowsIllegalArgumentException_propagates() {
        // "ABC"
        byte[] source = new byte[] { 65, 66, 67 };
        // Stub the mock to throw when converting to String
        when(mockConverter.convert(eq(source), eq(String.class))).thenThrow(new IllegalArgumentException("cannot convert"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toCharArray(source, mockConverter), "Exception from Converter must be propagated unchanged");
        assertEquals("cannot convert", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void toCharArray_nullConverterWithString_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharArray("Just a string", null), "A null Converter should cause a NullPointerException");
    }

    // -----------------------------------------------------------------
    @Test
    void toCharArray_converterThrowsRuntimeException_propagates() {
        // "DEF"
        byte[] source = new byte[] { 68, 69, 70 };
        when(mockConverter.convert(eq(source), eq(String.class))).thenThrow(new RuntimeException("conversion failed"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ByteArrayConversions.toCharArray(source, mockConverter), "RuntimeException from Converter must propagate");
        assertEquals("conversion failed", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void toCharArray_sourceToStringThrowsRuntimeException_propagates() {
        ThrowingToString obj = new ThrowingToString("broken toString");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ByteArrayConversions.toCharArray(obj, realConverter), "Exception thrown by source object's toString() must bubble up");
        assertEquals("broken toString", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void toCharArray_unsupportedSourceWithoutConverter_throwsIllegalArgumentException() {
        InputStream src = InputStream.nullInputStream();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ByteArrayConversions.toCharArray(src, null), "Unsupported source type without a Converter should raise IllegalArgumentException");
        // The exact message is implementation‑dependent; we only verify the type.
        assertNotNull(ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void toCharArray_byteArrayWithNullConverter_throwsNullPointerException() {
        byte[] src = new byte[] { 65, 66, 67 };
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toCharArray(src, null), "A null Converter for a byte[] source must cause NullPointerException");
    }
}
