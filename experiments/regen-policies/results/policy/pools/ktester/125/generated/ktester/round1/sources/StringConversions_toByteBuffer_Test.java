package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toByteBuffer_Test {

    /**
     * Converter used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a ConverterOptions instance – the default constructor
        // provides a clean, empty configuration suitable for our tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release any references; not strictly required but keeps the
        // test class tidy and mirrors typical teardown patterns.
        converter = null;
    }

    static Stream<Arguments> nullAndEmptyCases() {
        return Stream.of(Arguments.of("null source", null, "empty buffer"), Arguments.of("empty byte[]", new byte[0], "empty buffer"));
    }

    static Stream<Arguments> stringAndCharCases() {
        return Stream.of(Arguments.of("String \"Hello\"", "Hello", "Hello".getBytes(StandardCharsets.UTF_8)), Arguments.of("char[] {'a','b','c'}", new char[] { 'a', 'b', 'c' }, "abc".getBytes(StandardCharsets.UTF_8)), Arguments.of("CharBuffer \"XYZ\"", CharBuffer.wrap("XYZ"), "XYZ".getBytes(StandardCharsets.UTF_8)));
    }

    static Stream<Arguments> numericCases() {
        return Stream.of(// int 12345 -> 0x00 0x00 0x30 0x39 (big‑endian)
        Arguments.of("Integer 12345", 12345, new byte[] { 0, 0, 48, 57 }), // double 3.14 -> IEEE‑754 binary representation
        Arguments.of("Double 3.14", 3.14, new byte[] { 64, 9, 30, (byte) 184, 81, (byte) 235, (byte) 133, 31 }), // BigInteger 98765432109876543210
        Arguments.of("BigInteger 98765432109876543210", new BigInteger("98765432109876543210"), new BigInteger("98765432109876543210").toByteArray()));
    }

    @Test
    void toByteBuffer_fromByteArray_returnsWrappedByteBuffer() {
        // GIVEN
        byte[] source = new byte[] { 10, 20, 30, 40, 50 };
        // WHEN
        ByteBuffer result = StringConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should not be null");
        // The ByteBuffer returned by toByteBuffer wraps the exact same array.
        assertArrayEquals(source, result.array(), "ByteBuffer should wrap the original byte array");
        // Position should be at the start (0) and limit should equal length.
        assertEquals(0, result.position(), "ByteBuffer position should be 0 after wrapping");
        assertEquals(source.length, result.limit(), "ByteBuffer limit should match the source array length");
    }

    @Test
    void toByteBuffer_fromString_convertsUsingDefaultEncoding() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        ByteBuffer result = StringConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should not be null");
        byte[] expected = source.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, result.array(), "ByteBuffer should contain UTF‑8 bytes of the source string");
        assertEquals(0, result.position(), "ByteBuffer position should be 0 after wrapping");
        assertEquals(expected.length, result.limit(), "ByteBuffer limit should match the UTF‑8 byte length");
    }

    @Test
    void toByteBuffer_fromNull_returnsEmptyByteBuffer() {
        // GIVEN
        Object source = null;
        // WHEN
        ByteBuffer result = StringConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should not be null even for null input");
        // The implementation delegates to toByteArray which returns EMPTY_BYTE_ARRAY for null.
        assertEquals(0, result.remaining(), "ByteBuffer for null input should have no remaining bytes");
        assertArrayEquals(new byte[0], result.array(), "ByteBuffer should wrap an empty byte array");
    }

    @Test
    void toByteBuffer_fromByteBuffer_isIdempotent() {
        // GIVEN
        byte[] original = new byte[] { 1, 2, 3 };
        ByteBuffer source = ByteBuffer.wrap(original);
        // WHEN
        ByteBuffer result = StringConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting ByteBuffer should not be null");
        // The conversion should treat the source ByteBuffer as a byte[] via the internal
        // toByteArray method, resulting in a new wrapped array equal to the original.
        assertArrayEquals(original, result.array(), "ByteBuffer should contain the same bytes as the source ByteBuffer");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullAndEmptyCases")
    void nullAndEmptyCases(String description, Object from, String expectedMessage) {
        // WHEN / THEN
        ByteBuffer result = StringConversions.toByteBuffer(from, converter);
        assertNotNull(result);
        assertEquals(0, result.remaining(), "Expected an empty ByteBuffer for: " + description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringAndCharCases")
    void stringAndCharConversions(String description, Object from, byte[] expectedBytes) {
        ByteBuffer result = StringConversions.toByteBuffer(from, converter);
        assertNotNull(result);
        assertArrayEquals(expectedBytes, result.array(), "ByteBuffer content mismatch for: " + description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericCases")
    void numericConversions(String description, Object from, byte[] expected) {
        ByteBuffer result = StringConversions.toByteBuffer(from, converter);
        assertNotNull(result);
        assertArrayEquals(expected, result.array(), "ByteBuffer content mismatch for numeric case: " + description);
    }

    @Test
    void byteBufferInput_copiesRemainingBytes() {
        // GIVEN: bytes [1,2,3,4] with position=1, limit=3 → remaining [2,3]
        ByteBuffer source = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 });
        source.position(1);
        source.limit(3);
        // WHEN
        ByteBuffer result = StringConversions.toByteBuffer(source, converter);
        // THEN
        assertNotNull(result);
        assertArrayEquals(new byte[] { 2, 3 }, result.array(), "ByteBuffer should contain only the remaining bytes");
    }

    @Test
    void unsupportedType_throwsIllegalArgumentException() {
        // plain object, no conversion support
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toByteBuffer(unsupported, converter), "Expected IllegalArgumentException for unsupported source type");
    }

    @Test
    void customConverter_intToHexString_isRespected() {
        // A mock converter that turns Integer 42 into the hex string "2A"
        Converter mock = mock(Converter.class);
        when(mock.convert(eq(42), eq(String.class))).thenReturn("2A");
        // The default conversion path will then turn the String into UTF‑8 bytes.
        ByteBuffer result = StringConversions.toByteBuffer(42, mock);
        assertNotNull(result);
        assertArrayEquals(new byte[] { '5', '0' }, result.array(), "Expected UTF‑8 bytes of hex string \"2A\"");
    }

    @Test
    void nullConverterArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringConversions.toByteBuffer("Sample", null), "Passing a null Converter should result in NPE");
    }

    @Test
    void converterThrowsRuntimeException_isPropagated() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), any())).thenThrow(new IllegalStateException("boom"));
        assertThrows(IllegalStateException.class, () -> StringConversions.toByteBuffer(123, mock), "RuntimeException from Converter must propagate");
    }

    @Test
    void converterReturnsNullByteArray_causesNullPointerException() {
        Converter mock = mock(Converter.class);
        // Simulate a converter that returns null when asked for a byte[]
        when(mock.convert(any(), eq(byte[].class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> StringConversions.toByteBuffer("irrelevant", mock), "ByteBuffer.wrap(null) should throw NPE");
    }

    @Test
    void invalidByteBufferState_throwsIllegalArgumentException() throws Exception {
        // Create a valid ByteBuffer first
        ByteBuffer bb = ByteBuffer.wrap(new byte[] { 10, 20, 30 });
        // Use reflection to corrupt its internal state (position > limit)
        Field positionField = java.nio.Buffer.class.getDeclaredField("position");
        Field limitField = java.nio.Buffer.class.getDeclaredField("limit");
        positionField.setAccessible(true);
        limitField.setAccessible(true);
        positionField.setInt(bb, 3);
        // illegal state
        limitField.setInt(bb, 2);
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toByteBuffer(bb, converter), "Expected IllegalArgumentException for ByteBuffer with position > limit");
    }

    @Test
    void unsupportedArrayComponentType_throwsIllegalArgumentException() {
        Object[] objArray = new Object[] { "a", "b" };
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toByteBuffer(objArray, converter), "Object[] should not be convertible to byte[]");
    }
}
