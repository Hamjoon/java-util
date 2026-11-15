package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.CharBuffer;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toStringBuilder_Test {

    /**
     * Mocked {@link Converter} used to supply a predictable conversion result.
     */
    @Mock
    private Converter converter;

    /**
     * Mocked {@link Converter} used in a few tests.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Converter that always throws a RuntimeException.
     */
    private static class ThrowingConverter implements Converter {

        @Override
        public <T> T convert(Object source, Class<T> targetClass) {
            throw new RuntimeException("Intentional conversion failure");
        }
    }

    /**
     * Converter that turns a byte[] into an upper‑case hexadecimal string.
     */
    private static class HexConverter implements Converter {

        @Override
        public <T> T convert(Object source, Class<T> targetClass) {
            if (!(source instanceof byte[]) || targetClass != String.class) {
                return null;
            }
            byte[] bytes = (byte[]) source;
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X", b));
            }
            @SuppressWarnings("unchecked")
            T result = (T) sb.toString();
            return result;
        }
    }

    /**
     * Converter that returns null for any conversion request.
     */
    private static class NullReturningConverter implements Converter {

        @Override
        public <T> T convert(Object source, Class<T> targetClass) {
            return null;
        }
    }

    /**
     * Converter that returns an Integer instead of a String, causing a ClassCastException.
     */
    private static class BadTypeConverter implements Converter {

        @Override
        public <T> T convert(Object source, Class<T> targetClass) {
            @SuppressWarnings("unchecked")
            T result = (T) Integer.valueOf(42);
            return result;
        }
    }

    /**
     * Object whose toString() deliberately throws a RuntimeException.
     */
    private static class ExplodingObject {

        @Override
        public String toString() {
            throw new RuntimeException("ExplodingObject toString failure");
        }
    }

    private static Stream<Arguments> byteArrayConversionProvider() {
        return Stream.of(Arguments.of(new byte[] { 0x0A, 0x1B, 0x2C }, "0A1B2C"), Arguments.of(new byte[] {}, ""));
    }

    /**
     * Verifies that {@code toStringBuilder} returns a {@link StringBuilder}
     * whose content matches the string produced by the mocked {@link Converter}.
     */
    @Test
    void toStringBuilderReturnsStringBuilderWithConvertedString() {
        // GIVEN
        final String expectedString = "converted‑value";
        // The internal implementation of ByteArrayConversions.toString(Object, Converter)
        // calls converter.convert(source, String.class).  We stub that call.
        when(mockConverter.convert(any(), eq(String.class))).thenReturn(expectedString);
        // WHEN
        StringBuilder result = ByteArrayConversions.toStringBuilder(new Object(), mockConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals(expectedString, result.toString(), "StringBuilder content must equal the string returned by the converter");
        // Verify that the converter was asked to convert to a String exactly once.
        verify(mockConverter, times(1)).convert(any(), eq(String.class));
    }

    /**
     * Ensures that a {@code null} conversion result leads to a {@code NullPointerException}
     * when the {@link StringBuilder} constructor receives {@code null}.  This mirrors the
     * behaviour of {@link StringBuilder#StringBuilder(String)}.
     */
    @Test
    void toStringBuilderPropagatesNullConversionResult() {
        // GIVEN
        when(mockConverter.convert(any(), eq(String.class))).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuilder(new Object(), mockConverter), "A null conversion result should cause a NullPointerException from the StringBuilder constructor");
    }

    /**
     * Group: Converter induced exception
     *
     * If the supplied {@link Converter} throws a {@link RuntimeException}
     * during conversion, the exception must propagate out of {@code toStringBuilder}.
     */
    @Test
    void converterThrowsExceptionIsPropagated() {
        // GIVEN
        Converter throwingConverter = new ThrowingConverter();
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> ByteArrayConversions.toStringBuilder("trigger", throwingConverter), "Exception from the converter should propagate");
    }

    /**
     * Group: Standard string input (no converter)
     *
     * When the source object is already a {@link String} and the converter is {@code null},
     * the method should return a {@link StringBuilder} containing the original string.
     */
    @Test
    void stringInputWithoutConverterReturnsSameContent() {
        // GIVEN
        String source = "Hello World";
        // WHEN
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, null);
        // THEN
        assertNotNull(result);
        assertEquals(source, result.toString(), "StringBuilder should contain the original string when no converter is supplied");
    }

    /**
     * Group: Byte array conversion using custom converter.
     *
     * Two scenarios are covered via a parameterized test:
     *   1. Non‑empty byte array → hex string.
     *   2. Empty byte array → empty string.
     */
    @ParameterizedTest(name = "{index}: byteArray={0} => expected=\"{1}\"")
    @MethodSource("byteArrayConversionProvider")
    void byteArrayConversionWithHexConverter(byte[] input, String expected) {
        // GIVEN
        Converter hexConverter = new HexConverter();
        // WHEN
        StringBuilder result = ByteArrayConversions.toStringBuilder(input, hexConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString(), "HexConverter should produce the expected hexadecimal representation");
    }

    /**
     * Group: CharBuffer input (no converter)
     *
     * The internal {@code toString} implementation should extract characters from
     * the {@link CharBuffer} and the resulting {@link StringBuilder} must contain them.
     */
    @Test
    void charBufferInputWithoutConverter() {
        // GIVEN
        CharBuffer buffer = CharBuffer.wrap("abcXYZ");
        // WHEN
        StringBuilder result = ByteArrayConversions.toStringBuilder(buffer, null);
        // THEN
        assertNotNull(result);
        assertEquals("abcXYZ", result.toString(), "StringBuilder should contain the characters from the CharBuffer");
    }

    /**
     * Group: Null source object (no converter)
     *
     * Passing a {@code null} source should cause a {@link NullPointerException}
     * because the underlying conversion cannot produce a valid {@link String}.
     */
    @Test
    void nullSourceObjectThrowsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuilder(source, null), "Null source should result in NullPointerException");
    }

    /**
     * Group: Unsupported type fallback
     *
     * For a type without special handling (e.g., {@link Date}), the default
     * {@code toString()} should be used.
     */
    @Test
    void unsupportedTypeFallsBackToObjectToString() {
        // GIVEN
        Date date = new Date(0L);
        String expected = date.toString();
        // WHEN
        StringBuilder result = ByteArrayConversions.toStringBuilder(date, null);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString(), "StringBuilder should contain the default Date.toString() representation");
    }

    /**
     * Group: Converter returns null
     *
     * If a custom {@link Converter} returns {@code null} instead of a {@link String},
     * the subsequent {@link StringBuilder} constructor will throw {@link NullPointerException}.
     */
    @Test
    void converterReturnsNullLeadsToNullPointerException() {
        // GIVEN
        Converter nullReturning = new NullReturningConverter();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuilder("any", nullReturning), "Null return from converter should cause NullPointerException");
    }

    /**
     * Group: Converter returns non‑String type
     *
     * A converter that returns an object of the wrong type (e.g., {@link Integer})
     * should cause a {@link ClassCastException} when the method attempts to treat it as a {@link String}.
     */
    @Test
    void converterReturnsWrongTypeCausesClassCastException() {
        // GIVEN
        Converter badConverter = new BadTypeConverter();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteArrayConversions.toStringBuilder("any", badConverter), "Returning a non‑String type should cause ClassCastException");
    }

    /**
     * Group: Source object toString exception
     *
     * If the source object's own {@code toString()} throws a {@link RuntimeException},
     * that exception must propagate out of {@code toStringBuilder}.
     */
    @Test
    void sourceObjectToStringThrowsException() {
        // GIVEN
        Object exploding = new ExplodingObject();
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> ByteArrayConversions.toStringBuilder(exploding, null), "Exception from source object's toString should propagate");
    }
}
