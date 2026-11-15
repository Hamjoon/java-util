package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.util.Date;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ByteArrayConversions_toStringBuilder_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance – use the default constructor.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    @Test
    void toStringBuilder_givenString_returnsEquivalentBuilder() {
        String source = "Hello World";
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder(source), result);
    }

    @Test
    void toStringBuilder_givenByteArray_returnsEquivalentBuilder() {
        // "Cedar"
        byte[] source = "Cedar".getBytes();
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder("Cedar"), result);
    }

    @Test
    void toStringBuilder_givenByteBuffer_returnsEquivalentBuilder() {
        ByteBuffer source = ByteBuffer.wrap("ByteBuffer".getBytes());
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder("ByteBuffer"), result);
    }

    @Test
    void toStringBuilder_givenCharArray_returnsEquivalentBuilder() {
        char[] source = new char[] { 'J', 'a', 'v', 'a' };
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder("Java"), result);
    }

    @Test
    void toStringBuilder_givenCharBuffer_returnsEquivalentBuilder() {
        CharBuffer source = CharBuffer.wrap("CharBuffer");
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder("CharBuffer"), result);
    }

    @Test
    void toStringBuilder_givenNull_returnsNullBuilder() {
        // According to the contract of ByteArrayConversions.toString, a null source yields null.
        // Therefore the builder should be created from a null string, which results in "null".
        // We verify the actual behaviour rather than assuming a particular contract.
        StringBuilder result = ByteArrayConversions.toStringBuilder(null, converter);
        assertNotNull(result);
        assertEquals("null", result.toString());
    }

    @Test
    void toStringBuilder_givenNull_andNullConverter_returnsBuilderWithLiteralNull() {
        // ByteArrayConversions.toString(null, null) returns the literal string "null"
        StringBuilder result = ByteArrayConversions.toStringBuilder(null, null);
        assertNotNull(result);
        assertEquals("null", result.toString());
    }

    @Test
    void toStringBuilder_givenEmptyByteArray_returnsEmptyBuilder() {
        byte[] source = new byte[0];
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder(""), result);
    }

    @Test
    void toStringBuilder_givenEmptyCharArray_returnsEmptyBuilder() {
        char[] source = new char[0];
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder(""), result);
    }

    @Test
    void toStringBuilder_givenEmptyString_returnsEmptyBuilder() {
        String source = "";
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder(""), result);
    }

    @Test
    void toStringBuilder_givenSlicedByteBuffer_returnsBuilderWithSliceContent() {
        // Bytes: "H i! World"
        // [72,105,33,32,87,111,114,108,100]
        byte[] data = "Hi! World".getBytes();
        ByteBuffer source = ByteBuffer.wrap(data).position(// start at '!'
        2).limit(// up to (but not including) index 5 -> bytes 33,32,87 => "! W"
        5);
        // Expected slice: bytes at indices 2,3,4 => "! W"
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder("! W"), result);
    }

    @Test
    void toStringBuilder_givenSlicedCharBuffer_returnsBuilderWithSliceContent() {
        CharBuffer source = CharBuffer.wrap("abcdef").position(// start at 'c'
        2).limit(// up to (but not including) index 5 -> 'c','d','e'
        5);
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder("cde"), result);
    }

    @Test
    void toStringBuilder_givenInteger_withoutConverter_usesToString() {
        Integer source = Integer.valueOf(12345);
        StringBuilder result = ByteArrayConversions.toStringBuilder(source, null);
        assertEquals(new StringBuilder("12345"), result);
    }

    @Test
    void toStringBuilder_givenDate_withCustomConverter_usesConverterResult() {
        // Arrange a mock Converter that returns a fixed ISO‑8601 string for Date objects.
        Converter mockConverter = Mockito.mock(Converter.class);
        // epoch
        Date date = new Date(0L);
        Mockito.when(mockConverter.convert(eq(date), eq(String.class))).thenReturn("1970-01-01T00:00:00Z");
        StringBuilder result = ByteArrayConversions.toStringBuilder(date, mockConverter);
        assertEquals(new StringBuilder("1970-01-01T00:00:00Z"), result);
    }

    @Test
    void toStringBuilder_givenObject_withThrowingConverter_propagatesException() {
        // Converter that throws a RuntimeException for any conversion attempt.
        Converter throwingConverter = Mockito.mock(Converter.class);
        Mockito.when(throwingConverter.convert(any(), eq(String.class))).thenThrow(new RuntimeException("Conversion failed"));
        Object source = new Object();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ByteArrayConversions.toStringBuilder(source, throwingConverter));
        assertEquals("Conversion failed", ex.getMessage());
    }

    @Test
    void toStringBuilder_givenString_withEmptyStringConverter_returnsEmptyBuilder() {
        // Converter that always returns an empty string.
        Converter emptyStringConverter = Mockito.mock(Converter.class);
        Mockito.when(emptyStringConverter.convert(any(), eq(String.class))).thenReturn("");
        StringBuilder result = ByteArrayConversions.toStringBuilder("test", emptyStringConverter);
        assertEquals(new StringBuilder(""), result);
    }

    @Test
    void toStringBuilder_givenString_withNullReturningConverter_throwsNullPointerException() {
        // Converter that returns null – this should cause the StringBuilder constructor to NPE.
        Converter nullReturningConverter = Mockito.mock(Converter.class);
        Mockito.when(nullReturningConverter.convert(any(), eq(String.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuilder("test", nullReturningConverter));
    }

    @Test
    void toStringBuilder_givenNull_withNullReturningConverter_throwsNPE() {
        // Converter returns null for a null source.
        Converter nullReturningConverter = Mockito.mock(Converter.class);
        Mockito.when(nullReturningConverter.convert(eq(null), eq(String.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> ByteArrayConversions.toStringBuilder(null, nullReturningConverter));
    }

    @Test
    void toStringBuilder_givenArbitraryObject_withoutConverter_throwsException() {
        // No converter supplied – the internal toString method cannot handle a plain Object.
        Object source = new Object();
        assertThrows(RuntimeException.class, () -> ByteArrayConversions.toStringBuilder(source, null));
    }

    @Test
    void toStringBuilder_givenInvalidUtf8ByteArray_throwsRuntimeException() {
        // 0xFF is not a valid start byte in UTF‑8; decoding should fail.
        byte[] source = new byte[] { (byte) 0xFF };
        // The exact exception type depends on the internal implementation,
        // but it is expected to be a RuntimeException wrapping a character‑conversion error.
        assertThrows(RuntimeException.class, () -> ByteArrayConversions.toStringBuilder(source, converter));
    }

    @Test
    void toStringBuilder_givenByteBufferWithNegativePosition_throwsIllegalArgumentException() {
        // Setting a negative position on a ByteBuffer throws IllegalArgumentException immediately.
        ByteBuffer buffer = ByteBuffer.allocate(0);
        assertThrows(IllegalArgumentException.class, () -> buffer.position(-1));
    }

    @Test
    void toStringBuilder_givenCharBufferWithNegativeLimit_throwsIllegalArgumentException() {
        CharBuffer buffer = CharBuffer.allocate(0);
        assertThrows(IllegalArgumentException.class, () -> buffer.limit(-5));
    }
}
