package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_read_Test {

    /**
     * Shared test data for all tests
     */
    private static byte[] sharedData;

    /**
     * Instance under test – recreated before each test
     */
    private FastByteArrayInputStream stream;

    @BeforeAll
    static void setupBeforeAll() {
        // initialise a deterministic byte array that will be used in the tests
        sharedData = new byte[] { 10, 20, 30, 40 };
    }

    @BeforeEach
    void setupBeforeEach() {
        // each test gets a fresh stream so that state does not leak between tests
        stream = new FastByteArrayInputStream(sharedData.clone());
    }

    @AfterEach
    void teardownAfterEach() {
        // help GC and make it obvious that the test finished using the stream
        stream = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // clear the shared data reference
        sharedData = null;
    }

    /**
     * Helper that injects values into the private fields of {@link FastByteArrayInputStream}
     * using reflection.
     */
    private static void injectState(FastByteArrayInputStream s, byte[] buffer, int pos, int count) throws Exception {
        Field bufferField = FastByteArrayInputStream.class.getDeclaredField("buffer");
        Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
        Field countField = FastByteArrayInputStream.class.getDeclaredField("count");
        bufferField.setAccessible(true);
        posField.setAccessible(true);
        countField.setAccessible(true);
        bufferField.set(s, buffer);
        posField.setInt(s, pos);
        countField.setInt(s, count);
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// 0x41 = 65
        Arguments.of(new byte[] { 0x41, 0x42, 0x43 }, 0, 3, 65, "When buffer has data and pos < count, read() should return the first byte (0x41) as an unsigned int (65) and advance pos."), // read zero byte at position 1
        Arguments.of(new byte[] { (byte) 0xFF, 0x00 }, 1, 2, 0, "Read a zero byte at position 1; verify that 0x00 is returned as 0 and pos increments."), // high‑bit set byte
        Arguments.of(new byte[] { (byte) 0x80 }, 0, 1, 128, "Byte with high bit set (0x80) should be returned as 128, confirming unsigned conversion via '& 0xff'."), // empty buffer
        Arguments.of(new byte[] {}, 0, 0, -1, "When buffer is empty (pos == count), read() must return -1 indicating end of stream."), // pos == count
        Arguments.of(new byte[] { 0x01, 0x02 }, 2, 2, -1, "When pos equals count (already at end), read() should return -1 without altering pos."), // read from middle
        Arguments.of(new byte[] { 0x10, 0x20, 0x30 }, 1, 3, 32, "Read from middle of buffer; ensure correct byte (0x20) is returned and pos becomes 2."));
    }

    private static Stream<Arguments> edgeErrorProvider() {
        return Stream.of(// 0xFF => 255
        Arguments.of(new byte[] { (byte) 0xFF, 0x01 }, 0, 2, 255, "Reading a byte with all bits set (0xFF) should return 255 after unsigned conversion."), // pos > count
        Arguments.of(new byte[] { 0x10, 0x20 }, 3, 2, -1, "When the internal position is beyond the count (pos > count), read() must behave as end‑of‑stream and return -1 without throwing."), // null buffer -> NPE
        Arguments.of(null, 0, 0, NullPointerException.class, "A null backing buffer should cause a NullPointerException when read() attempts to access it."), // count > actual length -> AIOOBE
        Arguments.of(new byte[] { 0x01 }, 0, 2, ArrayIndexOutOfBoundsException.class, "If count exceeds the actual buffer length, read() will try to access beyond the array bounds and should throw ArrayIndexOutOfBoundsException."), // negative count -> treated as empty
        Arguments.of(new byte[] { 0x7F }, 0, -1, -1, "A negative count means the stream is considered empty; read() must return -1 regardless of pos."));
    }

    private static Stream<Arguments> additionalExceptionProvider() {
        return Stream.of(// negative pos treated as out‑of‑range -> -1
        Arguments.of(new byte[] {}, -5, 0, -1, "Negative position should be treated as out‑of‑range; read() returns -1 without throwing."), // pos far beyond count (Integer.MAX_VALUE) -> -1
        Arguments.of(new byte[] { 0x01, 0x02 }, Integer.MAX_VALUE, 2, -1, "Pos far beyond count (overflow scenario) must result in end‑of‑stream (-1) and not cause an exception."), // count huge -> AIOOBE
        Arguments.of(new byte[] { 0x01, 0x02 }, 1, Integer.MAX_VALUE, ArrayIndexOutOfBoundsException.class, "Count larger than buffer length leads to out‑of‑bounds access when read() evaluates the condition and attempts to read."));
    }

    @Test
    @DisplayName("read() returns bytes sequentially and -1 at end of stream")
    void testReadSequentialBytes() {
        assertEquals(10, stream.read(), "first byte");
        assertEquals(20, stream.read(), "second byte");
        assertEquals(30, stream.read(), "third byte");
        assertEquals(40, stream.read(), "fourth byte");
        assertEquals(-1, stream.read(), "end of stream should be -1");
    }

    @Test
    @DisplayName("read() on an empty stream returns -1 immediately")
    void testReadEmptyStream() {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        assertEquals(-1, empty.read(), "empty stream must return -1");
    }

    @Test
    @DisplayName("read() after skip() continues from the correct position")
    void testReadAfterSkip() {
        long skipped = stream.skip(2);
        assertEquals(2L, skipped, "skip should move two positions");
        assertEquals(30, stream.read(), "byte after skip");
        assertEquals(40, stream.read(), "next byte after skip");
        assertEquals(-1, stream.read(), "end of stream");
    }

    @Test
    @DisplayName("read() respects mark() and reset()")
    void testReadAfterMarkAndReset() {
        // mark at the beginning
        stream.mark(0);
        assertEquals(10, stream.read(), "first read after mark");
        assertEquals(20, stream.read(), "second read after mark");
        // reset should bring us back to the marked position (position 0)
        stream.reset();
        assertEquals(10, stream.read(), "read after reset should return first byte again");
    }

    @Test
    @DisplayName("read() returns -1 after all bytes have been consumed")
    void testReadBeyondEnd() {
        // consume all bytes
        for (int i = 0; i < sharedData.length; i++) {
            assertNotEquals(-1, stream.read(), "still data available");
        }
        // now we are at EOF
        assertEquals(-1, stream.read(), "reading past EOF must return -1");
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("basicFunctionalityProvider")
    @DisplayName("FastByteArrayInputStream.read() - basic functionality")
    void testReadBasicFunctionality(byte[] buffer, int pos, int count, int expected, String description) throws Exception {
        // construct with a dummy buffer (will be overwritten by reflection)
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[0]);
        injectState(s, buffer, pos, count);
        int result = s.read();
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("edgeErrorProvider")
    @DisplayName("FastByteArrayInputStream.read() - edge and error conditions")
    void testReadEdgeAndErrorConditions(Object bufferObj, int pos, int count, Object expected, String description) throws Exception {
        // may be null
        byte[] buffer = (byte[]) bufferObj;
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[0]);
        injectState(s, buffer, pos, count);
        if (expected instanceof Class) {
            // Expected exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, s::read, description);
        } else {
            // Expected int result
            int expectedInt = (Integer) expected;
            int result = s.read();
            assertEquals(expectedInt, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("additionalExceptionProvider")
    @DisplayName("FastByteArrayInputStream.read() - additional exception scenarios")
    void testReadAdditionalExceptionScenarios(byte[] buffer, int pos, int count, Object expected, String description) throws Exception {
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[0]);
        injectState(s, buffer, pos, count);
        if (expected instanceof Class) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, s::read, description);
        } else {
            int expectedInt = (Integer) expected;
            int result = s.read();
            assertEquals(expectedInt, result, description);
        }
    }
}
