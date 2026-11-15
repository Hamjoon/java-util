package com.cedarsoftware.util.convert;

import java.nio.CharBuffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toCharBuffer_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(arguments("Simple word \"Java\"", new char[] { 'J', 'a', 'v', 'a' }), arguments("Alphabet subset \"xyz\"", new char[] { 'x', 'y', 'z' }));
    }

    @Test
    void toCharBuffer_wrapsGivenCharArray() {
        // GIVEN
        char[] source = new char[] { 'a', 'b', 'c' };
        // WHEN
        CharBuffer result = CharArrayConversions.toCharBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(source.length, result.remaining(), "Buffer should contain all characters");
        assertArrayEquals(source, result.array(), "Underlying array should be the original one");
        // Verify that the buffer content matches the source characters
        assertEquals('a', result.get(0));
        assertEquals('b', result.get(1));
        assertEquals('c', result.get(2));
    }

    @Test
    void toCharBuffer_withEmptyArray_returnsEmptyBuffer() {
        // GIVEN
        char[] source = new char[0];
        // WHEN
        CharBuffer result = CharArrayConversions.toCharBuffer(source, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(0, result.remaining(), "Buffer should be empty");
        assertArrayEquals(source, result.array(), "Underlying array should be the empty array");
    }

    @Test
    void toCharBuffer_nullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toCharBuffer(source, mockConverter), "Passing null should result in NPE");
    }

    @Test
    void toCharBuffer_wrapsJavaCharArray() {
        // GIVEN
        char[] source = new char[] { 'J', 'a', 'v', 'a' };
        // WHEN
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, null);
        // THEN
        assertNotNull(buffer);
        assertEquals(0, buffer.position());
        assertEquals(source.length, buffer.limit());
        assertArrayEquals(source, buffer.array());
        assertEquals("Java", new String(buffer.array()));
    }

    @Test
    void toCharBuffer_reflectsChangesToOriginalArray() {
        // GIVEN
        char[] source = new char[] { 'x', 'y', 'z' };
        // WHEN
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, null);
        // mutate the original array after wrapping
        source[1] = 'Y';
        // THEN
        assertEquals('Y', buffer.get(1), "Buffer should reflect mutation of the backing array");
    }

    @Test
    void toCharBuffer_invalidSourceType_throwsClassCastException() {
        // GIVEN
        Object source = new String("test");
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toCharBuffer(source, null), "Supplying a non‑char[] source should trigger ClassCastException");
    }

    @Test
    void toCharBuffer_unicodeSurrogatePair() {
        // GIVEN – a surrogate pair representing the 😀 emoji
        char[] source = new char[] { '\uD83D', '\uDE00' };
        // WHEN
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, null);
        // THEN
        assertEquals(0, buffer.position());
        assertEquals(2, buffer.limit());
        assertEquals('\uD83D', buffer.get(0));
        assertEquals('\uDE00', buffer.get(1));
        // The buffer should expose the same two chars; converting to a String yields the emoji
        assertEquals("😀", new String(buffer.array()));
    }

    @Test
    void toCharBuffer_bufferMutationReflectsOriginalArray() {
        // GIVEN
        char[] source = new char[] { 'a', 'b', 'c' };
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, null);
        // WHEN – mutate via the CharBuffer view
        buffer.put(1, 'z');
        // THEN – the underlying array must reflect the change
        assertEquals('z', source[1], "Modifying the buffer should modify the original array");
    }

    @Test
    void toCharBuffer_nonNullConverterArgumentIgnored() {
        // GIVEN
        char[] source = new char[] { 'p', 'q', 'r' };
        // WHEN – pass a non‑null mocked Converter (the method should ignore it)
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, mockConverter);
        // THEN
        assertNotNull(buffer);
        assertArrayEquals(source, buffer.array(), "Result should be identical regardless of the Converter argument");
        assertEquals("pqr", new String(buffer.array()));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalBehaviorProvider")
    void toCharBuffer_normalBehaviorParameterized(String description, char[] source) {
        // WHEN
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, null);
        // THEN
        assertNotNull(buffer);
        assertEquals(0, buffer.position());
        assertEquals(source.length, buffer.limit());
        assertArrayEquals(source, buffer.array(), description);
    }
}
