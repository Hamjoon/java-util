package com.cedarsoftware.util.convert;

import java.nio.CharBuffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toCharBuffer_Test {

    @Mock
    private Converter // Converter is not used by the method but required by the signature
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test never interacts with the Converter
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> provideValidCharArrays() {
        return Stream.of(Arguments.of(new char[] { 'a', 'b', 'c' }, null, "Valid non‑empty char[] with null Converter"), Arguments.of(new char[] {}, null, "Empty char[] with null Converter"));
    }

    @Test
    void toCharBuffer_wrapsGivenCharArray() {
        // WHEN
        CharBuffer result = CharArrayConversions.toCharBuffer(source, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(source.length, result.remaining(), "CharBuffer should contain exactly the number of characters in the source array");
        // Verify the content by reading from the buffer
        char[] extracted = new char[source.length];
        result.get(extracted);
        assertArrayEquals(source, extracted, "CharBuffer content must match the original array");
        // CharBuffer.wrap returns a view backed by the original array
        assertSame(source, result.array(), "CharBuffer should be backed by the original char[] (wrap shares the array)");
    }

    @Test
    void toCharBuffer_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toCharBuffer(nullInput, mockConverter), "Calling toCharBuffer with null should throw NullPointerException");
    }

    @Test
    void toCharBuffer_invalidType_throwsClassCastException() {
        // GIVEN
        Object notACharArray = "not a char array";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toCharBuffer(notACharArray, mockConverter), "Calling toCharBuffer with a non‑char[] should throw ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Converter argument independence
     * ------------------------------------------------------------- */
    @Test
    void toCharBuffer_ignoresNullConverter() {
        char[] source = new char[] { 'x', 'y' };
        // WHEN
        CharBuffer result = CharArrayConversions.toCharBuffer(source, null);
        // THEN – same assertions as in the normal case
        assertNotNull(result);
        assertEquals(source.length, result.remaining());
        char[] extracted = new char[source.length];
        result.get(extracted);
        assertArrayEquals(source, extracted);
        assertSame(source, result.array());
    }

    @Test
    void toCharBuffer_ignoresNonNullConverter() {
        char[] source = new char[] { 'p', 'q' };
        // WHEN
        CharBuffer result = CharArrayConversions.toCharBuffer(source, mockConverter);
        // THEN – same assertions as in the normal case
        assertNotNull(result);
        assertEquals(source.length, result.remaining());
        char[] extracted = new char[source.length];
        result.get(extracted);
        assertArrayEquals(source, extracted);
        assertSame(source, result.array());
    }

    /* -------------------------------------------------------------
     *  Buffer reflects underlying array changes
     * ------------------------------------------------------------- */
    @Test
    void toCharBuffer_reflectsChangesInUnderlyingArray() {
        // GIVEN
        char[] arr = new char[] { 'm', 'n', 'o' };
        CharBuffer buffer = CharArrayConversions.toCharBuffer(arr, null);
        // WHEN – modify the original array after the buffer has been created
        arr[1] = 'z';
        // THEN – the buffer should see the updated value because it shares the array
        assertEquals('m', buffer.get(0));
        assertEquals('z', buffer.get(1));
        assertEquals('o', buffer.get(2));
    }
}
