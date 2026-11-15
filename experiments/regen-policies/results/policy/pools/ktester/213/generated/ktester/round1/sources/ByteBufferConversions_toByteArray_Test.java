package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.Base64;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toByteArray_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – they are sufficient for the built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // The Converter does not hold external resources that need explicit cleanup.
        converter = null;
    }

    /**
     * Normal (non‑exceptional) scenarios.
     *
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>description – used as the test name</li>
     *   <li>input object (may be null)</li>
     *   <li>ByteBuffer that the mocked {@code toByteBuffer} should return (null allowed)</li>
     *   <li>expected byte[] result (null means we expect {@code EMPTY_BYTE_ARRAY})</li>
     * </ul>
     */
    static Stream<Arguments> normalCases() {
        return Stream.of(// 1️⃣ toByteBuffer returns null → EMPTY_BYTE_ARRAY
        Arguments.of("null ByteBuffer from converter", null, null, ArrayUtilities.EMPTY_BYTE_ARRAY), // 2️⃣ empty buffer (capacity 0) → EMPTY_BYTE_ARRAY
        Arguments.of("\"empty\" string → empty buffer", "empty", ByteBuffer.allocate(0), ArrayUtilities.EMPTY_BYTE_ARRAY), // 3️⃣ buffer with data → copy of data
        Arguments.of("\"hello\" → buffer {10,20,30,40,50}", "hello", ByteBuffer.wrap(new byte[] { 10, 20, 30, 40, 50 }), new byte[] { 10, 20, 30, 40, 50 }), // 4️⃣ partial view (position 2, limit 5) → remaining bytes {7,8,9}
        Arguments.of("\"partial\" → buffer position=2 limit=5", "partial", createBuffer(new byte[] { 0, 0, 7, 8, 9 }, 2, 5), new byte[] { 7, 8, 9 }), // 5️⃣ source null but converter still returns a buffer
        Arguments.of("null source → buffer {99,100}", null, ByteBuffer.wrap(new byte[] { 99, 100 }), new byte[] { 99, 100 }), // 6️⃣ read‑only buffer
        Arguments.of("\"anyObject\" → read‑only buffer {1,2,3}", "anyObject", ByteBuffer.wrap(new byte[] { 1, 2, 3 }).asReadOnlyBuffer(), new byte[] { 1, 2, 3 }), // 7️⃣ direct (off‑heap) buffer
        Arguments.of("\"directBuffer\" → direct buffer {5,6,7,8}", "directBuffer", directBuffer(new byte[] { 5, 6, 7, 8 }), new byte[] { 5, 6, 7, 8 }), // 8️⃣ buffer with position == limit (no remaining) → EMPTY_BYTE_ARRAY
        Arguments.of("\"noRemaining\" → position==limit", "noRemaining", ByteBuffer.wrap(new byte[] { 0, 0, 0 }).position(3), ArrayUtilities.EMPTY_BYTE_ARRAY), // 9️⃣ larger backing array, limit 5, position 2 → {2,3,4}
        Arguments.of("\"largerArray\" → position=2 limit=5", "largerArray", ByteBuffer.wrap(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }).position(2).limit(5), new byte[] { 2, 3, 4 }), // 🔟 sliced buffer (position 1, limit 4) → {20,30,40}
        Arguments.of("\"sliceBuffer\" → slice after pos=1 limit=4", "sliceBuffer", slicedBuffer(new byte[] { 10, 20, 30, 40, 50 }, 1, 4), new byte[] { 20, 30, 40 }));
    }

    /**
     * Exception‑propagation scenarios.
     *
     * Each entry contains:
     * <ul>
     *   <li>description</li>
     *   <li>input object</li>
     *   <li>the exception that the mocked {@code toByteBuffer} should throw</li>
     *   <li>the expected exception type (must be the same as the thrown one)</li>
     * </ul>
     */
    static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null converter – the real method will NPE when it tries to use it.
        Arguments.of("null Converter → NullPointerException", "any", null, NullPointerException.class), // Converter throws IllegalArgumentException
        Arguments.of("converter throws IllegalArgumentException", "bad", new IllegalArgumentException("bad argument"), IllegalArgumentException.class), // Converter throws RuntimeException (generic)
        Arguments.of("converter throws RuntimeException", "exception", new RuntimeException("conversion failed"), RuntimeException.class), // Simulated OutOfMemoryError due to huge remaining size
        Arguments.of("huge buffer → OutOfMemoryError", "huge", new OutOfMemoryError("simulated OOM"), OutOfMemoryError.class));
    }

    /**
     * Creates a ByteBuffer with given backing array, position and limit.
     */
    private static ByteBuffer createBuffer(byte[] backing, int position, int limit) {
        ByteBuffer buf = ByteBuffer.wrap(backing);
        buf.position(position);
        buf.limit(limit);
        return buf;
    }

    /**
     * Returns a direct (off‑heap) ByteBuffer containing the supplied bytes, ready for reading.
     */
    private static ByteBuffer directBuffer(byte[] data) {
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length);
        direct.put(data);
        // prepare for reading
        direct.flip();
        return direct;
    }

    /**
     * Returns a sliced view of {@code backing} where the slice starts at {@code start}
     * and ends at {@code end} (exclusive). The slice's position is 0 and its limit
     * equals {@code end‑start}.
     */
    private static ByteBuffer slicedBuffer(byte[] backing, int start, int end) {
        ByteBuffer base = ByteBuffer.wrap(backing);
        base.position(start);
        base.limit(end);
        // slice inherits the remaining bytes
        return base.slice();
    }

    @Test
    void toByteArray_convertsByteBufferWithData() {
        // GIVEN
        byte[] original = "Hello, world!".getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(original);
        // WHEN
        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        // THEN
        assertArrayEquals(original, result, "The byte array should match the original buffer contents");
    }

    @Test
    void toByteArray_returnsEmptyArrayWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        byte[] result = ByteBufferConversions.toByteArray(source, converter);
        // THEN
        assertNotNull(result, "Result must never be null");
        assertEquals(0, result.length, "Result should be an empty array for null source");
    }

    @Test
    void toByteArray_returnsEmptyArrayWhenBufferHasNoRemaining() {
        // GIVEN
        // no remaining bytes
        ByteBuffer empty = ByteBuffer.allocate(0);
        // WHEN
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        // THEN
        assertNotNull(result, "Result must never be null");
        assertEquals(0, result.length, "Result should be an empty array when buffer has no remaining bytes");
    }

    @Test
    void toByteArray_convertsBase64StringViaConverter() {
        // GIVEN
        String base64 = Base64.getEncoder().encodeToString("test".getBytes());
        // The Converter can turn a Base64 string into a ByteBuffer (via its internal conversion rules)
        // We rely on the public API of Converter for this conversion.
        ByteBuffer expected = ByteBuffer.wrap(Base64.getDecoder().decode(base64));
        // WHEN
        byte[] result = ByteBufferConversions.toByteArray(base64, converter);
        // THEN
        assertArrayEquals(expected.array(), result, "The decoded bytes should match the expected array");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalCases")
    void toByteArray_normalScenarios(String description, Object from, ByteBuffer mockedBuffer, byte[] expected) {
        // Mock the static toByteBuffer method.
        try (MockedStatic<ByteBufferConversions> mockedStatic = mockStatic(ByteBufferConversions.class)) {
            // When toByteBuffer is called with any arguments, return the buffer we prepared.
            mockedStatic.when(() -> ByteBufferConversions.toByteBuffer(any(), any())).thenReturn(mockedBuffer);
            // Exercise the method under test.
            byte[] result = ByteBufferConversions.toByteArray(from, converter);
            // Verify the result – the method never returns null.
            assertNotNull(result, description + " – result must not be null");
            assertArrayEquals(expected, result, description + " – unexpected byte content");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCases")
    void toByteArray_exceptionScenarios(String description, Object from, Throwable toBufferThrowable, Class<? extends Throwable> expectedException) {
        // When the supplied converter is null we cannot mock the static method – just call directly.
        if (converter == null) {
            assertThrows(expectedException, () -> ByteBufferConversions.toByteArray(from, null), description + " – expected exception");
            return;
        }
        try (MockedStatic<ByteBufferConversions> mockedStatic = mockStatic(ByteBufferConversions.class)) {
            // Stub the static method to throw the desired exception.
            mockedStatic.when(() -> ByteBufferConversions.toByteBuffer(any(), any())).thenThrow(toBufferThrowable);
            // Verify that the exception propagates unchanged.
            assertThrows(expectedException, () -> ByteBufferConversions.toByteArray(from, converter), description + " – exception should propagate");
        }
    }
}
