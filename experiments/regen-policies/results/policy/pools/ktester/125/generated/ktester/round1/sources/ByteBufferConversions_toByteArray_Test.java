package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toByteArray_Test {

    /**
     * Shared {@link Converter} used by all test methods.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor; it provides the default behaviour.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Supplies the two converter variations for the parameterised test above.
     */
    private static Stream<Converter> nullSourceProvider() {
        return Stream.of(// the normal, non‑null converter
        converter, // a null converter
        null);
    }

    @Test
    void toByteArray_returnsEmptyArrayWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        byte[] result = ByteBufferConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(ArrayUtilities.EMPTY_BYTE_ARRAY, result, "A null source should yield the constant EMPTY_BYTE_ARRAY");
    }

    @Test
    void toByteArray_returnsEmptyArrayWhenBufferHasNoRemaining() {
        // given: an empty ByteBuffer (capacity 0)
        ByteBuffer empty = ByteBuffer.allocate(0);
        // The Converter can convert a ByteBuffer to a ByteBuffer – it will just return the same instance.
        Object source = empty;
        // when
        byte[] result = ByteBufferConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(ArrayUtilities.EMPTY_BYTE_ARRAY, result, "An empty ByteBuffer should also yield EMPTY_BYTE_ARRAY");
    }

    @Test
    void toByteArray_returnsCorrectBytesWhenBufferHasData() {
        // given: a ByteBuffer containing the ASCII bytes for "abc"
        byte[] expected = new byte[] { 'a', 'b', 'c' };
        ByteBuffer buffer = ByteBuffer.wrap(expected);
        // passing the ByteBuffer itself as the source
        Object source = buffer;
        // when
        byte[] result = ByteBufferConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(expected, result, "The returned byte array must contain the same data as the source ByteBuffer");
    }

    @Test
    void toByteArray_doesNotAlterOriginalBufferPosition() {
        // given: a buffer with some data and its position set to 1 (skip first byte)
        byte[] data = new byte[] { 10, 20, 30, 40 };
        ByteBuffer buffer = ByteBuffer.wrap(data);
        // now remaining() == 3, bytes {20,30,40}
        buffer.position(1);
        Object source = buffer;
        // when
        byte[] result = ByteBufferConversions.toByteArray(source, converter);
        // then
        byte[] expected = new byte[] { 20, 30, 40 };
        assertArrayEquals(expected, result, "toByteArray should read from the buffer's current position");
        // also verify that the original buffer's position has been advanced to its limit
        assertEquals(buffer.limit(), buffer.position(), "The method consumes the buffer, leaving its position at the limit");
    }

    /**
     * When {@code from} is {@code null} the internal {@code toByteBuffer} returns {@code null}
     * before the {@code Converter} is consulted.  Therefore even a {@code null} converter
     * must result in {@link ArrayUtilities#EMPTY_BYTE_ARRAY}.
     */
    @Test
    void toByteArray_nullFromInputAndNullConverter_returnsEmptyArray() {
        // given
        Object from = null;
        Converter nullConverter = null;
        // when
        byte[] result = ByteBufferConversions.toByteArray(from, nullConverter);
        // then
        assertArrayEquals(EMPTY_BYTE_ARRAY, result, "Null source with null converter should still return EMPTY_BYTE_ARRAY");
    }

    /**
     * Passing a non‑null source with a {@code null} {@link Converter} should cause a
     * {@link NullPointerException} because the conversion routine attempts to invoke a method
     * on the missing converter.
     */
    @Test
    void toByteArray_nullConverter_throwsNullPointerException() {
        // given
        Object from = "someString";
        Converter nullConverter = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ByteBufferConversions.toByteArray(from, nullConverter), "A null Converter must cause NullPointerException");
    }

    /**
     * The library does not know how to convert an {@link Integer} to a {@link ByteBuffer}.
     * The expected behaviour is an {@link IllegalArgumentException} bubbling up from
     * {@code toByteBuffer}.
     */
    @Test
    void toByteArray_unsupportedConversion_throwsIllegalArgumentException() {
        // given
        // Integer, not convertible to ByteBuffer
        Object from = 12345;
        // reuse the regular converter created in @BeforeAll
        // when / then
        assertThrows(IllegalArgumentException.class, () -> ByteBufferConversions.toByteArray(from, converter), "Converting an unsupported type should throw IllegalArgumentException");
    }

    /**
     * Demonstrates a parameterised approach for the two variations of a null source:
     * with a real converter and with a {@code null} converter.
     */
    @ParameterizedTest(name = "[{index}] from=null, converter={0}")
    @MethodSource("nullSourceProvider")
    void toByteArray_nullSource_variations(Converter suppliedConverter) {
        // given
        Object from = null;
        // when
        byte[] result = ByteBufferConversions.toByteArray(from, suppliedConverter);
        // then
        assertArrayEquals(EMPTY_BYTE_ARRAY, result, "Regardless of the converter, a null source must yield EMPTY_BYTE_ARRAY");
    }
}
