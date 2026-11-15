package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StringConversions_toByteBuffer_Test {

    private static Converter converter;

    // used for the “real” conversion cases
    private static Converter defaultConverter;

    // to close Mockito's open mocks
    private AutoCloseable mocks;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter has a no‑arg constructor via ConverterOptions
        converter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void initMocks() {
        // Initialise Mockito annotations for each test that uses @Mock fields
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void closeMocks() throws Exception {
        mocks.close();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none here)
        converter = null;
    }

    @Test
    void toByteBuffer_fromString_returnsCorrectByteBuffer() {
        String source = "hello world";
        ByteBuffer buffer = StringConversions.toByteBuffer(source, defaultConverter);
        assertNotNull(buffer, "ByteBuffer should not be null");
        byte[] expected = source.getBytes();
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(expected, actual, "ByteBuffer content should match the string bytes");
    }

    @Test
    void toByteBuffer_fromByteArray_returnsSameBytes() {
        byte[] source = new byte[] { 10, 20, 30, 40 };
        ByteBuffer buffer = StringConversions.toByteBuffer(source, defaultConverter);
        assertNotNull(buffer, "ByteBuffer should not be null");
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(source, actual, "ByteBuffer should wrap the original byte array");
    }

    @Test
    void toByteBuffer_fromNull_returnsEmptyBuffer() {
        ByteBuffer buffer = StringConversions.toByteBuffer(null, defaultConverter);
        assertNotNull(buffer, "ByteBuffer should not be null even for null input");
        assertEquals(0, buffer.remaining(), "ByteBuffer should be empty when source is null");
    }

    @Test
    void toByteBuffer_nullSourceAndNullConverter_returnsEmptyBuffer() {
        ByteBuffer buffer = StringConversions.toByteBuffer(null, null);
        assertNotNull(buffer, "Buffer must never be null");
        assertEquals(0, buffer.remaining(), "Empty buffer expected when both arguments are null");
    }

    @Test
    void toByteBuffer_byteArraySource_nullConverter_ignoresConverter() {
        byte[] src = new byte[] { 0x01, 0x02, 0x7F };
        ByteBuffer buffer = StringConversions.toByteBuffer(src, null);
        assertNotNull(buffer);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(src, actual, "Converter should be ignored for byte[] source");
    }

    @Test
    void toByteBuffer_byteArraySource_nonNullConverter_ignoresConverter() {
        byte[] src = new byte[] { 0x0A, 0x0B };
        Converter mock = mock(Converter.class);
        // Even if the mock were called it would return null – the implementation must bypass it.
        ByteBuffer buffer = StringConversions.toByteBuffer(src, mock);
        assertNotNull(buffer);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(src, actual, "Byte array source must be wrapped unchanged");
        // ensure the method really ignored the converter
        verifyNoInteractions(mock);
    }

    @ParameterizedTest(name = "[{index}] String \"{0}\" → UTF‑8 bytes")
    @CsvSource({ "'Hello', 48 65 6C 6C 6F", "'©', C2 A9" })
    void toByteBuffer_stringSource_utf8(String input, String expectedHex) {
        ByteBuffer buffer = StringConversions.toByteBuffer(input, null);
        assertNotNull(buffer);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        // Convert expected hex string to a byte array for comparison
        String[] parts = expectedHex.split(" ");
        byte[] expected = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            expected[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        assertArrayEquals(expected, actual, () -> "UTF‑8 bytes for \"" + input + "\" do not match expected");
    }

    @Test
    void toByteBuffer_stringSource_withCustomConverter_returnsFixedBytes() {
        // Mock converter that always returns the same byte[]
        Converter mock = mock(Converter.class);
        byte[] fixed = new byte[] { 0x55, (byte) 0xAA };
        when(mock.convert(any(), eq(byte[].class))).thenReturn(fixed);
        ByteBuffer buffer = StringConversions.toByteBuffer("ignored", mock);
        assertNotNull(buffer);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(fixed, actual, "Custom converter should dictate the byte content");
    }

    @Test
    void toByteBuffer_integerSource_withConverterReturningStringBytes() {
        // Converter that turns any object into its UTF‑8 string bytes
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(byte[].class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            // UTF‑8 by default
            return arg.toString().getBytes();
        });
        ByteBuffer buffer = StringConversions.toByteBuffer(12345, mock);
        assertNotNull(buffer);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals("12345".getBytes(), actual, "Integer should be converted to its string representation bytes");
    }

    @Test
    void toByteBuffer_unsupportedType_withoutConverter_returnsEmpty() {
        Object unsupported = new Object();
        ByteBuffer buffer = StringConversions.toByteBuffer(unsupported, null);
        assertNotNull(buffer);
        assertEquals(0, buffer.remaining(), "When no conversion is possible and no Converter is supplied the result must be empty");
    }

    @Test
    void toByteBuffer_unsupportedType_withConverterThrowingIllegalArgumentException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(byte[].class))).thenThrow(new IllegalArgumentException("bad"));
        Object unsupported = new Object();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toByteBuffer(unsupported, mock), "IllegalArgumentException from the Converter must propagate");
        assertEquals("bad", ex.getMessage());
    }

    @Test
    void toByteBuffer_converterReturnsString_causesClassCastException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(byte[].class))).thenReturn("not bytes");
        ByteBuffer buffer = assertThrows(ClassCastException.class, () -> StringConversions.toByteBuffer("test", mock));
        // Message is JVM specific – we only assert that a ClassCastException was thrown
    }

    @Test
    void toByteBuffer_converterReturnsCharArray_causesClassCastException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(byte[].class))).thenReturn(new char[] { 'a', 'b' });
        assertThrows(ClassCastException.class, () -> StringConversions.toByteBuffer(new Object(), mock));
    }

    @Test
    void toByteBuffer_converterReturnsInteger_causesClassCastException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(byte[].class))).thenReturn(42);
        assertThrows(ClassCastException.class, () -> StringConversions.toByteBuffer(123, mock));
    }

    @Test
    void toByteBuffer_nullSource_withConverterThrowingNullPointerException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(isNull(), eq(byte[].class))).thenThrow(new NullPointerException("null source"));
        NullPointerException ex = assertThrows(NullPointerException.class, () -> StringConversions.toByteBuffer(null, mock));
        assertEquals("null source", ex.getMessage());
    }

    @Test
    void toByteBuffer_converterThrowsRuntimeException_propagatesIt() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(byte[].class))).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toByteBuffer("data", mock));
        assertEquals("boom", ex.getMessage());
    }
}
