package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toByteBuffer_Test {

    /**
     * Mocked {@link ConverterOptions} – required to construct a {@link Converter}.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * The {@link Converter} instance used in the tests.
     */
    private Converter converter;

    /**
     * The {@link Converter} instance used for the majority of tests.
     */
    private Converter defaultConverter;

    /**
     * Simple identity‑like converter that returns the supplied byte[] when asked.
     */
    private static class FixedByteArrayConverter extends Converter {

        private final byte[] fixed;

        FixedByteArrayConverter(ConverterOptions options, byte[] fixed) {
            super(options);
            this.fixed = fixed;
        }

        @Override
        public <T> T convert(Object value, Class<T> target) {
            if (target == byte[].class) {
                @SuppressWarnings("unchecked")
                T result = (T) fixed;
                return result;
            }
            return super.convert(value, target);
        }
    }

    /**
     * Converter that always throws a RuntimeException when convert is called.
     */
    private static class ExceptionThrowingConverter extends Converter {

        ExceptionThrowingConverter(ConverterOptions options) {
            super(options);
        }

        @Override
        public <T> T convert(Object value, Class<T> target) {
            throw new RuntimeException("forced exception");
        }
    }

    /**
     * Converter that returns an unsupported type (e.g., a List) for any conversion request.
     */
    private static class UnsupportedTypeConverter extends Converter {

        UnsupportedTypeConverter(ConverterOptions options) {
            super(options);
        }

        @Override
        public <T> T convert(Object value, Class<T> target) {
            @SuppressWarnings("unchecked")
            T result = (T) Collections.singletonList("unsupported");
            return result;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks for this test method.
        MockitoAnnotations.openMocks(this);
        // Return an empty map to keep the environment clean.
        when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        converter = new Converter(mockOptions);
        MockitoAnnotations.openMocks(this);
        when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        // The Converter constructor copies the map returned by getConverterOverrides().
        defaultConverter = new Converter(mockOptions);
    }

    /**
     * Returns the UTF‑8 byte representation of the supplied string.
     */
    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static Stream<Arguments> happyPathProvider() {
        return Stream.of(// null input → empty buffer
        Arguments.of(null, null, new byte[0], "null input should produce an empty ByteBuffer"), // direct byte[]
        Arguments.of(new byte[] { 0x01, 0x02, 0x03 }, null, new byte[] { 0x01, 0x02, 0x03 }, "byte[] should be wrapped unchanged"), // String → UTF‑8 bytes
        Arguments.of("abc", null, utf8("abc"), "String should be encoded as UTF‑8"), // char[]
        Arguments.of(new char[] { 'x', 'y', 'z' }, null, utf8("xyz"), "char[] should be converted to a String then UTF‑8"), // Integer
        Arguments.of(Integer.valueOf(12345), null, utf8("12345"), "Integer should become its decimal string then UTF‑8"), // existing ByteBuffer (position 0)
        Arguments.of(ByteBuffer.wrap(new byte[] { 0x0A, 0x0B, 0x0C }), null, new byte[] { 0x0A, 0x0B, 0x0C }, "ByteBuffer should be extracted and re‑wrapped"), // empty String
        Arguments.of("", null, new byte[0], "Empty string → empty ByteBuffer"), // empty char[]
        Arguments.of(new char[] {}, null, new byte[0], "Empty char[] → empty ByteBuffer"), // Boolean.TRUE
        Arguments.of(Boolean.TRUE, null, utf8("true"), "Boolean should become \"true\" then UTF‑8"), // Double
        Arguments.of(Double.valueOf(3.14), null, utf8("3.14"), "Double should become its string representation then UTF‑8"), // BigInteger
        Arguments.of(new java.math.BigInteger("12345678901234567890"), null, utf8("12345678901234567890"), "BigInteger → decimal string → UTF‑8"), // UUID
        Arguments.of(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), null, utf8("123e4567-e89b-12d3-a456-426655440000"), "UUID → canonical string → UTF‑8"), // ByteBuffer with non‑zero position (only remaining bytes expected)
        Arguments.of(positionedBuffer(new byte[] { 0x01, 0x02, 0x03, 0x04 }, 2), null, new byte[] { 0x03, 0x04 }, "ByteBuffer with position 2 should yield remaining bytes only"), // Emoji string (multi‑byte UTF‑8)
        Arguments.of("😀👍", null, utf8("😀👍"), "String containing surrogate‑pair characters must be encoded correctly"));
    }

    /**
     * Creates a ByteBuffer from the supplied array and sets its position.
     *
     * @param data     the backing byte array
     * @param position the desired position (must be ≤ data.length)
     * @return a ByteBuffer ready for the test
     */
    private static ByteBuffer positionedBuffer(byte[] data, int position) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.position(position);
        return bb;
    }

    @Test
    void toByteBuffer_wrapsByteArrayDirectly() {
        // GIVEN
        byte[] source = new byte[] { 10, 20, 30 };
        // WHEN
        ByteBuffer result = StringConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer must not be null");
        assertEquals(source.length, result.remaining(), "ByteBuffer should contain exactly the same number of bytes as the source array");
        // ByteBuffer.wrap uses the original array as backing storage.
        assertSame(source, result.array(), "ByteBuffer should wrap the original byte array (same reference)");
        // Verify content.
        assertArrayEquals(source, result.array(), "Content of the ByteBuffer must match the source byte array");
    }

    @Test
    void toByteBuffer_usesConverterWhenNotABinaryArray() {
        // GIVEN
        Object arbitraryObject = new Object();
        byte[] expectedBytes = new byte[] { 1, 2, 3, 4 };
        // Stub the converter so that a conversion to byte[] returns the expected bytes.
        // The real implementation of StringConversions.toByteArray eventually calls
        // Converter.convert(value, byte[].class).  We mock that interaction.
        Converter spyConverter = spy(converter);
        doReturn(expectedBytes).when(spyConverter).convert(eq(arbitraryObject), eq(byte[].class));
        // WHEN
        ByteBuffer result = StringConversions.toByteBuffer(arbitraryObject, spyConverter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer must not be null");
        assertEquals(expectedBytes.length, result.remaining(), "ByteBuffer length must equal the length of the bytes supplied by the converter");
        assertArrayEquals(expectedBytes, result.array(), "ByteBuffer content must match the bytes returned by the converter");
        // Verify that the converter was indeed asked to perform the conversion.
        verify(spyConverter, times(1)).convert(eq(arbitraryObject), eq(byte[].class));
    }

    @Test
    void toByteBuffer_withNullInputReturnsEmptyBuffer() {
        // GIVEN
        Object nullInput = null;
        // WHEN
        ByteBuffer result = StringConversions.toByteBuffer(nullInput, converter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should never be null even for null input");
        assertEquals(0, result.remaining(), "ByteBuffer created from a null input should be empty (zero remaining)");
        // The underlying array should be the constant EMPTY_BYTE_ARRAY defined in StringConversions.
        // We cannot access it directly, but we can assert that the buffer is empty.
        assertEquals(0, result.capacity(), "Empty ByteBuffer should have zero capacity");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("happyPathProvider")
    void toByteBuffer_happyPath(Object from, Converter customConverter, byte[] expectedBytes, String description) {
        // Arrange
        Converter converterToUse = (customConverter != null) ? customConverter : defaultConverter;
        // Act
        ByteBuffer result = StringConversions.toByteBuffer(from, converterToUse);
        // Assert
        assertNotNull(result, "Resulting ByteBuffer must not be null");
        assertEquals(expectedBytes.length, result.remaining(), "ByteBuffer.remaining() must match expected byte count");
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expectedBytes, actual, "ByteBuffer content differs from expectation");
    }

    @Test
    void toByteBuffer_respectsFixedByteArrayConverter() {
        // GIVEN
        Object dummy = new Object();
        byte[] fixed = new byte[] { 0x55, (byte) 0xAA };
        Converter fixedConverter = new FixedByteArrayConverter(mockOptions, fixed);
        // WHEN
        ByteBuffer result = StringConversions.toByteBuffer(dummy, fixedConverter);
        // THEN
        assertNotNull(result);
        assertEquals(fixed.length, result.remaining());
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(fixed, actual);
    }

    @Test
    void toByteBuffer_propagatesExceptionFromConverter() {
        // GIVEN
        Object dummy = new Object();
        Converter throwing = new ExceptionThrowingConverter(mockOptions);
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toByteBuffer(dummy, throwing));
        assertEquals("forced exception", ex.getMessage());
    }

    @Test
    void toByteBuffer_nullConverterThrowsNPE() {
        // GIVEN
        Object dummy = new Object();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toByteBuffer(dummy, null));
    }

    @Test
    void toByteBuffer_nullBothParametersThrowsNPE() {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toByteBuffer(null, null));
    }

    @Test
    void toByteBuffer_converterReturnsUnsupportedTypeThrowsIAE() {
        // GIVEN
        Object dummy = new Object();
        Converter unsupported = new UnsupportedTypeConverter(mockOptions);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toByteBuffer(dummy, unsupported));
    }

    @Test
    void toByteBuffer_unsupportedSourceTypeThrowsIllegalArgumentException() {
        // GIVEN
        Object unsupported = new Object();
        // Use a spy so that the conversion to byte[] is delegated to the real implementation,
        // which will eventually raise the exception.
        Converter spy = spy(defaultConverter);
        // No stubbing – let the real toByteArray logic run.
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toByteBuffer(unsupported, spy));
    }
}
