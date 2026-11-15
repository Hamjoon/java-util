package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.InvalidMarkException;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toCharBuffer_Test {

    /**
     * A shared {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    /**
     * Original mutable {@link CharBuffer} used as the source for conversion.
     */
    private CharBuffer sourceBuffer;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not need any special options for this test.
        converter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mutable CharBuffer for each test to avoid cross‑test contamination.
        sourceBuffer = CharBuffer.allocate(10);
        // fill with known data
        sourceBuffer.put("abcde");
        // prepare for reading (position = 0, limit = 5)
        sourceBuffer.flip();
        // Mark the position to later verify that the mark is preserved.
        sourceBuffer.mark();
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up; simply nullify references.
        sourceBuffer = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Helper that creates a mock Converter which will fail the test if any
     *  method is invoked (the method under test must ignore the argument).
     */
    private static Converter mockConverterThatFailsIfUsed() {
        return mock(Converter.class, invocation -> {
            throw new AssertionError("Converter should not be used by toCharBuffer()");
        });
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(Arguments.of("Mutable buffer created via wrap(\"hello\")", CharBuffer.wrap("hello"), CharBuffer.wrap("hello")), Arguments.of("Allocated buffer with capacity 10, filled with \"abcdef\"", CharBuffer.allocate(10).put("abcdef").flip(), CharBuffer.wrap("abcdef")));
    }

    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(Arguments.of("String instead of CharBuffer", "just a string"), Arguments.of("ByteBuffer instance", java.nio.ByteBuffer.allocate(4)));
    }

    @Test
    void toCharBuffer_returnsReadOnlyBufferWithSameContent() {
        // GIVEN
        // keep a snapshot of original state
        CharBuffer original = sourceBuffer.duplicate();
        // WHEN
        CharBuffer result = CharBufferConversions.toCharBuffer(sourceBuffer, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer must not be null");
        assertTrue(result.isReadOnly(), "Resulting CharBuffer should be read‑only");
        // Content must be identical
        assertEquals(original.remaining(), result.remaining(), "Remaining characters should match");
        while (original.hasRemaining()) {
            assertEquals(original.get(), result.get(), "Characters at each position must be equal");
        }
        // Original buffer must remain unchanged (position, limit, mark)
        assertEquals(0, sourceBuffer.position(), "Original buffer position must stay at 0 after conversion");
        assertEquals(5, sourceBuffer.limit(), "Original buffer limit must stay unchanged");
        // The mark should still be at position 0 (the only valid mark after flip)
        // will throw IllegalStateException if mark lost
        sourceBuffer.reset();
    }

    @Test
    void toCharBuffer_doesNotAllowWritingToResult() {
        // GIVEN
        CharBuffer result = CharBufferConversions.toCharBuffer(sourceBuffer, converter);
        // WHEN / THEN
        assertThrows(ReadOnlyBufferException.class, () -> result.put('x'), "Attempting to write to a read‑only CharBuffer must throw ReadOnlyBufferException");
    }

    @Test
    void toCharBuffer_throwsClassCastExceptionWhenSourceIsNotCharBuffer() {
        // GIVEN
        Object notABuffer = "not a CharBuffer";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharBufferConversions.toCharBuffer(notABuffer, converter), "A non‑CharBuffer source should cause a ClassCastException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("successfulConversionProvider")
    void successfulConversion(String description, CharBuffer source, CharBuffer expected) {
        CharBuffer result = CharBufferConversions.toCharBuffer(source, null);
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        // Verify state (position, limit, capacity) matches expectation
        assertEquals(expected.capacity(), result.capacity(), "Capacity must match");
        assertEquals(expected.limit(), result.limit(), "Limit must match");
        assertEquals(0, result.position(), "Result position should start at 0");
        // Verify content
        char[] expectedArray = new char[expected.remaining()];
        expected.get(expectedArray);
        char[] actualArray = new char[result.remaining()];
        result.get(actualArray);
        assertArrayEquals(expectedArray, actualArray, "Buffer contents must be identical");
    }

    @Test
    void readOnlyPropertyEnforcement_putThrows() {
        CharBuffer result = CharBufferConversions.toCharBuffer(CharBuffer.wrap("xyz"), null);
        assertThrows(ReadOnlyBufferException.class, () -> result.put('a'), "Calling put on the returned buffer must throw ReadOnlyBufferException");
    }

    @Test
    void positionIndependence_sourceHasNonZeroPosition_resultStartsAtZero() {
        CharBuffer source = CharBuffer.wrap("12345");
        // move source position
        source.position(2);
        CharBuffer result = CharBufferConversions.toCharBuffer(source, null);
        assertEquals(0, result.position(), "Result buffer position must be independent and start at 0");
        assertEquals(2, source.position(), "Source buffer position must remain unchanged");
    }

    @Test
    void nullSourceThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toCharBuffer(null, null), "Passing null as source must throw NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidSourceProvider")
    void invalidSourceTypeThrows(String description, Object source) {
        assertThrows(ClassCastException.class, () -> CharBufferConversions.toCharBuffer(source, null), "Invalid source type should cause ClassCastException");
    }

    @Test
    void converterArgumentIsIgnored_mockNeverInvoked() {
        Converter mock = mockConverterThatFailsIfUsed();
        CharBuffer source = CharBuffer.wrap("ignore");
        CharBuffer result = CharBufferConversions.toCharBuffer(source, mock);
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals("ignore", result.toString(), "Content must match source");
        verifyNoInteractions(mock);
    }

    @Test
    void readOnlySourceIsAccepted_andResultIsReadOnly() {
        CharBuffer source = CharBuffer.wrap("readonly").asReadOnlyBuffer();
        CharBuffer result = CharBufferConversions.toCharBuffer(source, null);
        assertTrue(result.isReadOnly(), "Result must be read‑only even if source already is");
        assertEquals("readonly", result.toString(), "Content must be preserved");
        assertEquals(source.position(), result.position(), "Position must match source");
        assertEquals(source.limit(), result.limit(), "Limit must match source");
        assertEquals(source.capacity(), result.capacity(), "Capacity must match source");
    }

    @Test
    void readOnlySourceWithMockConverter_converterStillIgnored() {
        Converter mock = mockConverterThatFailsIfUsed();
        CharBuffer source = CharBuffer.wrap("readonly").asReadOnlyBuffer();
        CharBuffer result = CharBufferConversions.toCharBuffer(source, mock);
        assertTrue(result.isReadOnly());
        assertEquals("readonly", result.toString());
        verifyNoInteractions(mock);
    }

    @Test
    void sliceBufferConversionPreservesSliceState() {
        CharBuffer source = CharBuffer.wrap("abcdefgh").position(// now position points at 'c'
        2).limit(// limit after 'f'
        6).// slice -> capacity 4, position 0, limit 4, content "cdef"
        slice();
        CharBuffer result = CharBufferConversions.toCharBuffer(source, null);
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals(4, result.capacity(), "Slice capacity should be 4");
        assertEquals(4, result.limit(), "Slice limit should be 4");
        assertEquals(0, result.position(), "Slice position should be 0");
        assertEquals("cdef", result.toString(), "Slice content must be \"cdef\"");
    }

    @Test
    void duplicateBufferConversionPreservesState() {
        CharBuffer source = CharBuffer.wrap("duplicate").duplicate();
        CharBuffer result = CharBufferConversions.toCharBuffer(source, null);
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals(source.capacity(), result.capacity(), "Capacity must be preserved");
        assertEquals(source.limit(), result.limit(), "Limit must be preserved");
        assertEquals(source.position(), result.position(), "Position must be preserved");
        assertEquals("duplicate", result.toString(), "Content must be preserved");
    }

    @Test
    void markIsNotCopied_resultResetThrowsInvalidMarkException() {
        CharBuffer source = CharBuffer.wrap("marktest").position(2).mark().position(4);
        CharBuffer result = CharBufferConversions.toCharBuffer(source, null);
        assertThrows(InvalidMarkException.class, result::reset, "Result buffer should not have a valid mark; reset must throw InvalidMarkException");
    }

    @Test
    void underlyingArrayMutationIsVisibleInReadOnlyBuffer() {
        char[] arr = "mutable".toCharArray();
        CharBuffer src = CharBuffer.wrap(arr);
        CharBuffer ro = CharBufferConversions.toCharBuffer(src, null);
        // mutate the backing array
        arr[0] = 'M';
        assertEquals('M', ro.get(0), "Change to backing array must be reflected in read‑only view");
    }

    @Test
    void zeroRemainingBufferConversion() {
        CharBuffer source = CharBuffer.allocate(5).put("abc").flip().position(// position == limit => no remaining
        3);
        CharBuffer result = CharBufferConversions.toCharBuffer(source, null);
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals(0, result.remaining(), "Result should have zero remaining characters");
        assertEquals(0, result.position(), "Result position must be reset to 0");
        assertEquals(0, result.limit(), "Result limit must be 0");
        assertEquals(5, result.capacity(), "Capacity must remain 5");
    }

    @Test
    void resultIsNotSameInstanceAsSource() {
        CharBuffer source = CharBuffer.wrap("distinct");
        CharBuffer result = CharBufferConversions.toCharBuffer(source, null);
        assertNotSame(source, result, "Returned buffer must be a distinct instance");
    }

    @Test
    void compactOnResultThrowsReadOnlyBufferException() {
        CharBuffer result = CharBufferConversions.toCharBuffer(CharBuffer.wrap("compact"), null);
        assertThrows(ReadOnlyBufferException.class, result::compact, "compact() on a read‑only buffer must throw ReadOnlyBufferException");
    }

    @Test
    void emptyBufferConversion() {
        CharBuffer source = CharBuffer.allocate(0);
        CharBuffer result = CharBufferConversions.toCharBuffer(source, null);
        assertTrue(result.isReadOnly(), "Result must be read‑only");
        assertEquals(0, result.capacity(), "Capacity must be 0");
        assertEquals(0, result.limit(), "Limit must be 0");
        assertEquals(0, result.position(), "Position must be 0");
        assertFalse(result.hasRemaining(), "Empty buffer must have no remaining characters");
    }
}
