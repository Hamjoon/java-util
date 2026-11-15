package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.stream.Stream;

class CharArrayConversions_toByteArray_Test {

    private Converter converter;

    /* --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final byte[] expected;

        final String description;

        ArgumentsHolder(Object from, Converter converter, byte[] expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a ConverterOptions instance – the default constructor creates an
        // options object with the standard built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // Use the real default Converter for most tests – it knows the built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * Simple dummy converter that does nothing – used where a converter is required but should not be invoked.
     */
    private static Converter dummyConverter() {
        return new Converter(new ConverterOptions());
    }

    /**
     * Provides test arguments for the "Null and Empty Inputs" group.
     */
    private static Stream<ArgumentsHolder> nullAndEmptyProvider() {
        return Stream.of(new ArgumentsHolder(null, dummyConverter(), new byte[0], "null source should yield empty byte array"), new ArgumentsHolder(new byte[0], dummyConverter(), new byte[0], "empty byte[] source should yield empty byte array"));
    }

    /**
     * Provides test arguments for the "Direct byte[] Input" group.
     */
    private static Stream<ArgumentsHolder> directByteArrayProvider() {
        return Stream.of(new ArgumentsHolder(new byte[] { 10, 20, 30, 40 }, dummyConverter(), new byte[] { 10, 20, 30, 40 }, "byte[] source must be returned unchanged"));
    }

    /**
     * Provides test arguments for the various UTF‑8 conversion cases.
     */
    private static Stream<ArgumentsHolder> utf8ConversionProvider() {
        return Stream.of(new ArgumentsHolder(new char[] { 'J', 'a', 'v', 'a' }, dummyConverter(), new byte[] { 74, 97, 118, 97 }, "char[] conversion using UTF‑8"), new ArgumentsHolder("Café", dummyConverter(), new byte[] { 67, 97, 102, (byte) 0xC3, (byte) 0xA8 }, "String containing non‑ASCII character"), new ArgumentsHolder(CharBuffer.wrap(new char[] { 'X', 'Y', 'Z' }), dummyConverter(), new byte[] { 88, 89, 90 }, "CharBuffer conversion"), new ArgumentsHolder(new StringBuilder("AB"), dummyConverter(), new byte[] { 65, 66 }, "StringBuilder conversion"), new ArgumentsHolder(new StringBuffer("123"), dummyConverter(), new byte[] { 49, 50, 51 }, "StringBuffer conversion"));
    }

    /**
     * Provides test arguments for the "Custom Converter Override" group.
     */
    private static Stream<ArgumentsHolder> customConverterProvider() {
        Converter custom = mock(Converter.class);
        when(custom.convert(any())).thenReturn(ByteBuffer.wrap(new byte[] { 9, 9, 9 }));
        return Stream.of(new ArgumentsHolder("ignored", custom, new byte[] { 9, 9, 9 }, "Custom converter should dominate source type"));
    }

    /**
     * Provides test arguments for the "Unsupported Source Type" group.
     */
    private static Stream<ArgumentsHolder> unsupportedSourceProvider() {
        return Stream.of(new ArgumentsHolder(Integer.valueOf(123), dummyConverter(), null, "Unsupported source type should raise ConversionException"));
    }

    /**
     * Provides test arguments for the "Large Data Boundary" group.
     */
    private static Stream<ArgumentsHolder> largeDataProvider() {
        // all zeros by default
        byte[] large = new byte[65536];
        return Stream.of(new ArgumentsHolder(large, dummyConverter(), large, "64 KiB byte array must be returned unchanged"));
    }

    /**
     * Provides test arguments for the "Buffer Independence" group.
     */
    private static Stream<ArgumentsHolder> bufferIndependenceProvider() {
        ByteBuffer buf = ByteBuffer.allocate(4).put(new byte[] { 1, 2, 3, 4 });
        // prepare for reading
        buf.flip();
        return Stream.of(new ArgumentsHolder(buf, dummyConverter(), new byte[] { 1, 2, 3, 4 }, "Result must be a copy; later buffer mutation must not affect output"));
    }

    /**
     * Provides test arguments for the "Converter Throws ConversionException" group.
     */
    private static Stream<ArgumentsHolder> converterThrowsProvider() {
        Converter bad = mock(Converter.class);
        when(bad.convert(any())).thenThrow(new ConversionException("forced failure"));
        return Stream.of(new ArgumentsHolder("trigger", bad, null, "Exception from converter must propagate"));
    }

    /**
     * Provides test arguments for the "Converter Returns Null ByteBuffer" group.
     */
    private static Stream<ArgumentsHolder> converterReturnsNullProvider() {
        Converter nullReturning = mock(Converter.class);
        when(nullReturning.convert(any())).thenReturn(null);
        return Stream.of(new ArgumentsHolder("any", nullReturning, null, "Null ByteBuffer from converter leads to NPE"));
    }

    @Test
    void toByteArray_fromCharArray_returnsCorrespondingBytes() {
        // given
        char[] source = new char[] { 'a', 'b', 'c', '1', '2', '3' };
        byte[] expected = new byte[] { (byte) 'a', (byte) 'b', (byte) 'c', (byte) '1', (byte) '2', (byte) '3' };
        // when
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(expected, actual, "Byte array should contain the low‑order byte of each char in the source array");
    }

    @Test
    void toByteArray_fromEmptyCharArray_returnsEmptyByteArray() {
        // given
        char[] source = new char[0];
        byte[] expected = new byte[0];
        // when
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(expected, actual);
    }

    @Test
    void toByteArray_fromNull_returnsEmptyByteArray() {
        // given
        Object source = null;
        byte[] expected = new byte[0];
        // when
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        // then
        assertArrayEquals(expected, actual);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("nullAndEmptyProvider")
    void testNullAndEmptyInputs(ArgumentsHolder holder) {
        byte[] actual = CharArrayConversions.toByteArray(holder.from, holder.converter);
        assertArrayEquals(holder.expected, actual);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("directByteArrayProvider")
    void testDirectByteArrayInput(ArgumentsHolder holder) {
        byte[] actual = CharArrayConversions.toByteArray(holder.from, holder.converter);
        assertArrayEquals(holder.expected, actual);
        // ensure a copy is returned (different reference)
        assertNotSame(holder.from, actual);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("utf8ConversionProvider")
    void testUtf8Conversions(ArgumentsHolder holder) {
        byte[] actual = CharArrayConversions.toByteArray(holder.from, holder.converter);
        assertArrayEquals(holder.expected, actual);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("customConverterProvider")
    void testCustomConverterOverride(ArgumentsHolder holder) {
        byte[] actual = CharArrayConversions.toByteArray(holder.from, holder.converter);
        assertArrayEquals(holder.expected, actual);
    }

    @Test
    void testUnsupportedSourceTypeThrowsConversionException() {
        ArgumentsHolder holder = new ArgumentsHolder(Integer.valueOf(123), dummyConverter(), null, "Unsupported source type should raise ConversionException");
        assertThrows(ConversionException.class, () -> CharArrayConversions.toByteArray(holder.from, holder.converter));
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("largeDataProvider")
    void testLargeDataBoundary(ArgumentsHolder holder) {
        byte[] actual = CharArrayConversions.toByteArray(holder.from, holder.converter);
        assertEquals(holder.expected.length, actual.length);
        assertArrayEquals(holder.expected, actual);
    }

    @Test
    void testBufferIndependence() {
        // Arrange
        ByteBuffer original = ByteBuffer.allocate(4).put(new byte[] { 1, 2, 3, 4 });
        original.flip();
        byte[] result = CharArrayConversions.toByteArray(original, dummyConverter());
        // Mutate original after conversion
        original.put(0, (byte) 9);
        // Assert that the returned array is unaffected
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    void testNullConverterArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteArray("sample", null));
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("converterThrowsProvider")
    void testConverterThrowsConversionException(ArgumentsHolder holder) {
        assertThrows(ConversionException.class, () -> CharArrayConversions.toByteArray(holder.from, holder.converter));
    }

    @Test
    void testConverterReturnsNullByteBufferThrowsNPE() {
        Converter nullReturning = mock(Converter.class);
        when(nullReturning.convert(any())).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toByteArray("any", nullReturning));
    }
}
