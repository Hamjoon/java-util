package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toCharArray_Test {

    /**
     * Mocked static version of {@link CharBufferConversions} used to stub the
     *  {@code toCharBuffer} call that {@code toCharArray} delegates to.
     */
    private MockedStatic<CharBufferConversions> mockedStatic;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh static mock for every test to avoid cross‑test contamination.
        mockedStatic = Mockito.mockStatic(CharBufferConversions.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock; this also clears any stubbing/verification state.
        mockedStatic.close();
    }

    @Test
    void toCharArray_returnsArrayWhenBufferHasRemaining() {
        // GIVEN – a CharBuffer containing three characters
        CharBuffer buffer = CharBuffer.wrap(new char[] { 'x', 'y', 'z' });
        // Stub the static helper to return our prepared buffer regardless of arguments
        mockedStatic.when(() -> CharBufferConversions.toCharBuffer(any(), any())).thenReturn(buffer);
        // A mock Converter is sufficient – its behaviour is irrelevant for this test
        Converter converter = mock(Converter.class);
        // WHEN – invoking the method under test
        char[] result = CharBufferConversions.toCharArray("doesn't matter", converter);
        // THEN – the resulting array matches the buffer's contents
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, result);
    }

    @Test
    void toCharArray_returnsEmptyArrayWhenBufferHasNoRemaining() {
        // GIVEN – an empty CharBuffer (hasRemaining() == false)
        CharBuffer emptyBuffer = CharBuffer.allocate(0);
        // Stub the static helper to return the empty buffer
        mockedStatic.when(() -> CharBufferConversions.toCharBuffer(any(), any())).thenReturn(emptyBuffer);
        Converter converter = mock(Converter.class);
        // WHEN
        char[] result = CharBufferConversions.toCharArray("irrelevant", converter);
        // THEN – the method should return the shared EMPTY_CHAR_ARRAY constant
        assertSame(EMPTY_CHAR_ARRAY, result);
    }

    @Test
    void toCharArray_propagatesNullPointerExceptionWhenSourceIsNull() {
        // GIVEN – the static helper will throw NPE when called with a null source
        mockedStatic.when(() -> CharBufferConversions.toCharBuffer(isNull(), any())).thenThrow(new NullPointerException("source is null"));
        Converter converter = mock(Converter.class);
        // WHEN / THEN – the NPE should propagate out of toCharArray
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toCharArray(null, converter));
    }
}
