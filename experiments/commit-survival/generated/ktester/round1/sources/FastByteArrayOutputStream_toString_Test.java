package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_toString_Test {

    /**
     * Instance under test – created fresh before each test method.
     */
    private FastByteArrayOutputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a clean, default‑size stream.
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and guard against accidental reuse.
        stream = null;
    }

    /**
     * Sets a private field on the given target instance.
     *
     * @param target the object whose field should be modified
     * @param name   the field name
     * @param value  the new value
     * @throws Exception if reflection fails
     */
    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves the value of a private field.
     *
     * @param target the object
     * @param name   the field name
     * @return the field value
     * @throws Exception if reflection fails
     */
    private static Object getPrivateField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    @DisplayName("toString() on a newly created stream returns an empty string")
    void testToStringEmpty() {
        assertEquals("", stream.toString(), "A freshly constructed stream should produce an empty string");
        assertEquals(0, stream.size(), "Size must remain zero after calling toString() on an empty stream");
    }

    @Test
    @DisplayName("toString() returns the exact string that was written")
    void testToStringSimple() {
        String expected = "Hello, World!";
        stream.write(expected.getBytes());
        assertEquals(expected, stream.toString(), "toString() should reflect the bytes written to the stream");
        assertEquals(expected.length(), stream.size(), "Size must match the number of bytes written");
    }

    @Test
    @DisplayName("toString() after reset returns an empty string")
    void testToStringAfterReset() {
        stream.write("Some data".getBytes());
        assertFalse(stream.toString().isEmpty(), "Pre‑reset string should not be empty");
        stream.reset();
        assertEquals("", stream.toString(), "After reset, toString() must return an empty string");
        assertEquals(0, stream.size(), "Size must be zero after reset");
    }

    @Test
    @DisplayName("toString() reflects only the portion written with offset/length")
    void testToStringPartialWrite() {
        // 7 bytes
        byte[] source = "ABCDEFG".getBytes();
        // Write only "CDE" (offset 2, length 3)
        stream.write(source, 2, 3);
        assertEquals("CDE", stream.toString(), "toString() should contain exactly the bytes written via write(byte[],off,len)");
        assertEquals(3, stream.size(), "Size must equal the length argument supplied to write(byte[],off,len)");
    }

    @Test
    @DisplayName("toString() works with non‑ASCII (UTF‑8 multi‑byte) characters")
    void testToStringWithUnicode() {
        // The Euro sign (€) is three bytes in UTF‑8.
        String unicode = "Price: €100";
        stream.write(unicode.getBytes());
        assertEquals(unicode, stream.toString(), "toString() must correctly handle multi‑byte UTF‑8 characters");
    }

    @Test
    @DisplayName("toString() after writing a single ASCII byte via write(int)")
    void testToStringSingleByte() {
        // write(int) writes the low‑order byte
        stream.write('A');
        assertEquals("A", stream.toString(), "Single ASCII byte written with write(int) should be returned");
        assertEquals(1, stream.size(), "Size must be 1 after writing a single byte");
    }

    @Test
    @DisplayName("toString() with control and high‑ASCII bytes")
    void testToStringControlAndHighAscii() {
        // NUL, SOH, DEL (7‑bit max)
        byte[] data = new byte[] { 0x00, 0x01, 0x7F };
        stream.write(data, 0, data.length);
        assertEquals("\u0000\u0001\u007F", stream.toString(), "toString() must preserve null, control and 0x7F characters");
        assertEquals(3, stream.size(), "Size must reflect three bytes written");
    }

    @Test
    @DisplayName("toString() after manually resetting count to zero via reflection")
    void testToStringCountResetViaReflection() throws Exception {
        // Populate the buffer first.
        stream.write("persistent".getBytes());
        // Force count to 0 while leaving the buffer untouched.
        setPrivateField(stream, "count", 0);
        assertEquals("", stream.toString(), "When count is forced to 0, toString() must ignore existing buffer data");
        assertEquals(0, getPrivateField(stream, "count"));
    }

    @Test
    @DisplayName("toString() throws StringIndexOutOfBoundsException when count > buf.length")
    void testToStringCountGreaterThanBuffer() throws Exception {
        // Write a few bytes so the buffer is allocated.
        stream.write("abc".getBytes());
        // Corrupt internal state: set count larger than the actual buffer.
        setPrivateField(stream, "count", 1000);
        assertThrows(StringIndexOutOfBoundsException.class, stream::toString, "A too‑large count should cause String constructor to throw");
    }

    @Test
    @DisplayName("toString() after buffer expansion (100 'a's)")
    void testToStringBufferExpansion() {
        byte[] manyA = new byte[100];
        for (int i = 0; i < manyA.length; i++) {
            manyA[i] = 'a';
        }
        stream.write(manyA, 0, manyA.length);
        assertEquals(100, stream.size(), "Size must be 100 after writing 100 bytes");
        assertEquals("a".repeat(100), stream.toString(), "toString() must return the full expanded content");
    }

    @Test
    @DisplayName("toString() after writing a sub‑range (CDEFG) from a larger array")
    void testToStringPartialArrayWrite() {
        // 10 bytes
        byte[] source = "ABCDEFGHIJ".getBytes();
        // write "CDEFG"
        stream.write(source, 2, 5);
        assertEquals("CDEFG", stream.toString(), "Only the selected sub‑range should be present");
        assertEquals(5, stream.size(), "Size must equal the length argument");
    }

    @Test
    @DisplayName("toString() after close() does not clear the buffer")
    void testToStringAfterClose() throws IOException {
        String text = "Closed";
        stream.write(text.getBytes());
        // close should be a no‑op for the buffer
        stream.close();
        assertEquals(text, stream.toString(), "Closing the stream must not clear previously written data");
    }

    @Test
    @DisplayName("toString() replaces malformed UTF‑8 with the replacement character")
    void testToStringInvalidUtf8() {
        // illegal sequence
        byte[] malformed = new byte[] { (byte) 0xC3, (byte) 0x28 };
        stream.write(malformed, 0, malformed.length);
        // Expected: replacement character () followed by '('
        assertEquals("\uFFFD(", stream.toString(), "Invalid UTF‑8 should be decoded as replacement character");
    }

    @Test
    @DisplayName("toString() throws StringIndexOutOfBoundsException for negative count via reflection")
    void testToStringNegativeCountViaReflection() throws Exception {
        stream.write("data".getBytes());
        setPrivateField(stream, "count", -5);
        assertThrows(StringIndexOutOfBoundsException.class, stream::toString, "Negative count must cause String constructor to throw");
    }

    @Test
    @DisplayName("toString() throws NullPointerException when internal buffer is null")
    void testToStringNullBufferViaReflection() throws Exception {
        // Ensure count is non‑zero to trigger the constructor path.
        stream.write("abc".getBytes());
        setPrivateField(stream, "buf", null);
        assertThrows(NullPointerException.class, stream::toString, "A null internal buffer should result in NPE");
    }

    @Test
    @DisplayName("toString() throws StringIndexOutOfBoundsException for zero‑length buffer with count=1")
    void testToStringZeroLengthBufferOverflow() throws Exception {
        // Write one byte so count becomes 1.
        stream.write('x');
        // zero‑length buffer
        setPrivateField(stream, "buf", new byte[0]);
        assertThrows(StringIndexOutOfBoundsException.class, stream::toString, "count > buf.length must cause StringIndexOutOfBoundsException");
    }

    @Test
    @DisplayName("toString() throws StringIndexOutOfBoundsException when buffer shrunk below count")
    void testToStringCountExceedsShrunkBuffer() throws Exception {
        // Write five bytes.
        stream.write("12345".getBytes());
        // Replace the internal buffer with a smaller one (length 2) without adjusting count.
        setPrivateField(stream, "buf", new byte[2]);
        assertThrows(StringIndexOutOfBoundsException.class, stream::toString, "Shrinking buffer below count must trigger an exception");
    }
}
