package com.cedarsoftware.util.convert;

import java.nio.CharBuffer;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toCharBuffer_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Supplies a stream of objects that are *not* {@code char[]} to verify that
     * {@code toCharBuffer} throws {@link ClassCastException}.
     */
    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(// int[]
        Arguments.of(new int[] { 1, 2, 3 }), // String
        Arguments.of("some string"), // generic Object
        Arguments.of(new Object()), // byte[]
        Arguments.of(new byte[] { 'a', 'b' }), // Boolean
        Arguments.of(Boolean.TRUE));
    }

    @Test
    void toCharBufferWrapsArrayCorrectly() {
        char[] source = new char[] { 'x', 'y', 'z' };
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, mockConverter);
        assertNotNull(buffer, "Returned CharBuffer should not be null");
        assertEquals(source.length, buffer.remaining(), "CharBuffer should contain the same number of characters as the source array");
        assertArrayEquals(source, buffer.array(), "The underlying array of the CharBuffer should be the original array");
    }

    @Test
    void toCharBufferSharesUnderlyingArray() {
        char[] source = new char[] { 'a', 'b', 'c' };
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, mockConverter);
        // modify the original array after the buffer has been created
        source[1] = 'Z';
        // the change must be visible through the CharBuffer
        assertEquals('Z', buffer.get(1), "CharBuffer should reflect modifications made to the original array");
    }

    @Test
    void toCharBufferHandlesEmptyArray() {
        char[] source = new char[0];
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, mockConverter);
        assertNotNull(buffer);
        assertEquals(0, buffer.remaining(), "CharBuffer created from an empty array should have no remaining characters");
        assertArrayEquals(source, buffer.array(), "Underlying array should be the same empty array");
    }

    @Test
    void toCharBufferThrowsNPEWhenSourceIsNull() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toCharBuffer(null, mockConverter), "Passing a null source should result in NullPointerException");
    }

    @Test
    void toCharBufferAcceptsNullConverter() {
        char[] source = new char[] { 'p', 'q' };
        // Pass null for the Converter – the method does not use it.
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, null);
        assertNotNull(buffer);
        assertEquals(source.length, buffer.remaining());
        assertArrayEquals(source, buffer.array(), "Even when Converter is null, the method should behave identically");
    }

    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("invalidSourceProvider")
    void toCharBufferThrowsClassCastExceptionForInvalidSource(Object invalidSource) {
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toCharBuffer(invalidSource, mockConverter), "Casting a non‑char[] source should throw ClassCastException");
    }
}
