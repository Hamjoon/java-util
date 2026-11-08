package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassNameFromByteCode_Test {

    /**
     * Byte‑code of {@link ReflectionUtils} used for the happy‑path test.
     */
    private static byte[] reflectionUtilsByteCode;

    /**
     * Dummy inner class used only for the inner‑class test above.
     */
    static class Inner {
    }

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Load the .class file of ReflectionUtils from the same class‑loader.
        try (InputStream is = ReflectionUtils.class.getResourceAsStream("/com/cedarsoftware/util/ReflectionUtils.class")) {
            assertNotNull(is, "Unable to locate ReflectionUtils.class on the classpath");
            reflectionUtilsByteCode = is.readAllBytes();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static byte‑array reference.
        reflectionUtilsByteCode = null;
    }

    /**
     * Helper that converts an {@code int[]} (as written in the JSON) to a {@code byte[]}.
     */
    private static byte[] intsToBytes(int[] ints) {
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i];
        }
        return bytes;
    }

    static Stream<Arguments> simpleUtf8Provider() {
        return Stream.of(Arguments.of("Minimal valid class file with a CONSTANT_CLASS entry referencing a CONSTANT_UTF8 string \"Test\"", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 3, 7, 0, 2, 1, 0, 4, 84, 101, 115, 116, 0, 1, 0, 1 }, "Test"));
    }

    static Stream<Arguments> constantLongProvider() {
        return Stream.of(Arguments.of("Constant pool contains a CONSTANT_LONG entry (tag 5) which occupies two slots", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 4, 7, 0, 3, 1, 0, 4, 84, 101, 115, 116, 5, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1 }, "Test"));
    }

    static Stream<Arguments> constantDoubleProvider() {
        return Stream.of(Arguments.of("Constant pool contains a CONSTANT_DOUBLE entry (tag 6) which also occupies two slots", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 4, 7, 0, 3, 1, 0, 4, 84, 101, 115, 116, 6, 64, 0, 0, 0, 64, 0, 0, 0, 0, 1, 0, 1 }, "Test"));
    }

    static Stream<Arguments> allSupportedTagsProvider() {
        return Stream.of(Arguments.of("Class file that includes one entry of every supported constant pool tag (except LONG/DOUBLE)", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 13, // CLASS -> string index 2
        7, // CLASS -> string index 2
        0, // CLASS -> string index 2
        2, // UTF8 "Test"
        1, // UTF8 "Test"
        0, // UTF8 "Test"
        4, // UTF8 "Test"
        84, // UTF8 "Test"
        101, // UTF8 "Test"
        115, // UTF8 "Test"
        116, // STRING (ignored) -> string index 2
        8, // STRING (ignored) -> string index 2
        0, // STRING (ignored) -> string index 2
        2, // FIELDREF (ignored)
        9, // FIELDREF (ignored)
        0, // FIELDREF (ignored)
        1, // FIELDREF (ignored)
        0, // FIELDREF (ignored)
        4, // NAMEANDTYPE (ignored)
        12, // NAMEANDTYPE (ignored)
        0, // NAMEANDTYPE (ignored)
        2, // NAMEANDTYPE (ignored)
        0, // NAMEANDTYPE (ignored)
        2, // METHODREF (ignored)
        10, // METHODREF (ignored)
        0, // METHODREF (ignored)
        1, // METHODREF (ignored)
        0, // METHODREF (ignored)
        4, // METHODHANDLE (ignored)
        15, // METHODHANDLE (ignored)
        1, // METHODHANDLE (ignored)
        0, // METHODHANDLE (ignored)
        1, // METHODTYPE (ignored)
        16, // METHODTYPE (ignored)
        0, // METHODTYPE (ignored)
        2, // DYNAMIC (ignored)
        17, // DYNAMIC (ignored)
        0, // DYNAMIC (ignored)
        0, // DYNAMIC (ignored)
        0, // DYNAMIC (ignored)
        4, // INVOKEDYNAMIC (ignored)
        18, // INVOKEDYNAMIC (ignored)
        0, // INVOKEDYNAMIC (ignored)
        0, // INVOKEDYNAMIC (ignored)
        0, // INVOKEDYNAMIC (ignored)
        4, // MODULE (ignored)
        19, // MODULE (ignored)
        0, // MODULE (ignored)
        2, // PACKAGE (ignored)
        20, // PACKAGE (ignored)
        0, // PACKAGE (ignored)
        2, // access flags (dummy)
        0, // access flags (dummy)
        1, // this_class index -> points to entry 1 (CLASS)
        0, // this_class index -> points to entry 1 (CLASS)
        1, // super_class (dummy)
        0, // super_class (dummy)
        1 }, "Test"));
    }

    static Stream<Arguments> invalidTagProvider() {
        return Stream.of(Arguments.of("Constant pool contains an unrecognized tag value (99)", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 2, 99, 0, 0, 0, 0 }, IllegalStateException.class));
    }

    static Stream<Arguments> packageSlashProvider() {
        return Stream.of(Arguments.of("UTF8 entry encodes internal name \"com/example/MyClass\"", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 3, 1, 0, 19, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 77, 121, 67, 108, 97, 115, 115, 7, 0, 1, 0, 1, 0, 2 }, "com.example.MyClass"));
    }

    static Stream<Arguments> innerClassDollarProvider() {
        return Stream.of(Arguments.of("UTF8 entry encodes inner class name \"Outer$Inner\"", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 3, 1, 0, 11, 79, 117, 116, 101, 114, 36, 73, 110, 110, 101, 114, 7, 0, 1, 0, 1, 0, 2 }, "Outer$Inner"));
    }

    static Stream<Arguments> stringTagIgnoredProvider() {
        return Stream.of(Arguments.of("STRING entry references same UTF8 as CLASS entry; should be ignored", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 4, // UTF8 "Test"
        1, // UTF8 "Test"
        0, // UTF8 "Test"
        4, // UTF8 "Test"
        84, // UTF8 "Test"
        101, // UTF8 "Test"
        115, // UTF8 "Test"
        116, // STRING (ignored) -> index 1
        8, // STRING (ignored) -> index 1
        0, // STRING (ignored) -> index 1
        1, // CLASS -> string index 1
        7, // CLASS -> string index 1
        0, // CLASS -> string index 1
        1, // access flags (dummy)
        0, // access flags (dummy)
        1, // this_class index
        0, // this_class index
        1, 0, 1 }, "Test"));
    }

    static Stream<Arguments> invalidThisClassIndexProvider() {
        return Stream.of(Arguments.of("this_class index points beyond the CLASS array", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 2, // UTF8 "A"
        1, // UTF8 "A"
        0, // UTF8 "A"
        1, // UTF8 "A"
        65, // access flags dummy
        0, // access flags dummy
        1, // this_class = 2 (out of range, only one CLASS entry)
        0, // this_class = 2 (out of range, only one CLASS entry)
        2, 0, 1 }, ArrayIndexOutOfBoundsException.class));
    }

    static Stream<Arguments> negativeConstantPoolCountProvider() {
        return Stream.of(Arguments.of("constant_pool_count is 0, leading to NegativeArraySizeException", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 0 }, NegativeArraySizeException.class));
    }

    static Stream<Arguments> insufficientUtf8BytesProvider() {
        return Stream.of(Arguments.of("UTF8 length declares 2 bytes but only one byte follows", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 3, // declares length 2, provides only one byte (65)
        1, // declares length 2, provides only one byte (65)
        0, // declares length 2, provides only one byte (65)
        2, // declares length 2, provides only one byte (65)
        65, // access flags dummy
        0, // access flags dummy
        1, // this_class index
        0, // this_class index
        1, 0, 1 }, IOException.class));
    }

    static Stream<Arguments> zeroStringIndexProvider() {
        return Stream.of(Arguments.of("CONSTANT_CLASS entry references string index 0 (invalid)", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 3, // CLASS with string_index = 0
        7, // CLASS with string_index = 0
        0, // CLASS with string_index = 0
        0, // access flags dummy
        0, // access flags dummy
        1, // this_class = 1 (points to the CLASS entry above)
        0, // this_class = 1 (points to the CLASS entry above)
        1, 0, 1 }, ArrayIndexOutOfBoundsException.class));
    }

    static Stream<Arguments> classRefNonUtf8Provider() {
        return Stream.of(Arguments.of("CONSTANT_CLASS points to a CONSTANT_INTEGER entry (tag 3)", new int[] { 202, 254, 186, 190, 0, 0, 0, 52, 0, 3, // CLASS -> string_index = 2
        7, // CLASS -> string_index = 2
        0, // CLASS -> string_index = 2
        2, // CONSTANT_INTEGER (tag 3) at index 2
        3, // CONSTANT_INTEGER (tag 3) at index 2
        0, // CONSTANT_INTEGER (tag 3) at index 2
        0, // CONSTANT_INTEGER (tag 3) at index 2
        0, // CONSTANT_INTEGER (tag 3) at index 2
        0, // access flags dummy
        0, // access flags dummy
        1, // this_class = 1 (points to CLASS entry)
        0, // this_class = 1 (points to CLASS entry)
        1, 0, 1 }, NullPointerException.class));
    }

    @Test
    void testGetClassNameFromByteCode_ValidClass() throws IOException {
        // Act
        String className = ReflectionUtils.getClassNameFromByteCode(reflectionUtilsByteCode);
        // Assert – the fully‑qualified name must match the original class.
        assertEquals("com.cedarsoftware.util.ReflectionUtils", className);
    }

    @Test
    void testGetClassNameFromByteCode_ValidInnerClass() throws IOException {
        // Load byte‑code for an inner class (the test class itself) to verify '/' → '.' conversion.
        byte[] innerClassBytes;
        try (InputStream is = ReflectionUtils_getClassNameFromByteCode_Test.class.getResourceAsStream("ReflectionUtils_getClassNameFromByteCode_Test$Inner.class")) {
            // The inner class may not exist if the compiler didn't generate it; guard against null.
            assertNotNull(is, "Inner class byte‑code not found");
            innerClassBytes = is.readAllBytes();
        }
        String className = ReflectionUtils.getClassNameFromByteCode(innerClassBytes);
        assertEquals("com.cedarsoftware.util.ReflectionUtils_getClassNameFromByteCode_Test$Inner", className);
    }

    @Test
    void testGetClassNameFromByteCode_MalformedByteCode_ThrowsIOException() {
        // far too short to be a valid class file
        byte[] malformed = new byte[] { 0, 1, 2, 3, 4 };
        assertThrows(IOException.class, () -> ReflectionUtils.getClassNameFromByteCode(malformed), "Expected an IOException for malformed byte‑code");
    }

    /* ---------- Group: Valid class name extraction - simple UTF8 ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("simpleUtf8Provider")
    void testValidClassNameExtractionSimpleUtf8(String description, int[] byteCodeInts, String expected) throws IOException {
        byte[] byteCode = intsToBytes(byteCodeInts);
        String actual = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(expected, actual, description);
    }

    /* ---------- Group: Valid class name extraction - handling CONSTANT_LONG ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("constantLongProvider")
    void testValidClassNameExtractionConstantLong(String description, int[] byteCodeInts, String expected) throws IOException {
        byte[] byteCode = intsToBytes(byteCodeInts);
        String actual = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(expected, actual, description);
    }

    /* ---------- Group: Valid class name extraction - handling CONSTANT_DOUBLE ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("constantDoubleProvider")
    void testValidClassNameExtractionConstantDouble(String description, int[] byteCodeInts, String expected) throws IOException {
        byte[] byteCode = intsToBytes(byteCodeInts);
        String actual = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(expected, actual, description);
    }

    /* ---------- Group: Valid class name extraction - all supported constant pool tags ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("allSupportedTagsProvider")
    void testValidClassNameExtractionAllSupportedTags(String description, int[] byteCodeInts, String expected) throws IOException {
        byte[] byteCode = intsToBytes(byteCodeInts);
        String actual = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(expected, actual, description);
    }

    /* ---------- Group: Invalid constant pool tag ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidTagProvider")
    void testInvalidConstantPoolTag(String description, int[] byteCodeInts, Class<? extends Throwable> expectedException) {
        byte[] byteCode = intsToBytes(byteCodeInts);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ReflectionUtils.getClassNameFromByteCode(byteCode), description);
        assertTrue(ex.getMessage().contains("Unrecognized constant pool tag"), "Exception message should mention unrecognized tag");
    }

    /* ---------- Group: Null input handling ---------- */
    @Test
    void testNullInputHandling() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassNameFromByteCode(null), "Passing null should result in NullPointerException");
    }

    /* ---------- Group: Empty byte array ---------- */
    @Test
    void testEmptyByteArray() {
        assertThrows(IOException.class, () -> ReflectionUtils.getClassNameFromByteCode(new byte[] {}), "Empty array should cause an IOException (EOF)");
    }

    /* ---------- Group: Class name with package (slashes conversion) ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("packageSlashProvider")
    void testClassNameWithPackage(String description, int[] byteCodeInts, String expected) throws IOException {
        byte[] byteCode = intsToBytes(byteCodeInts);
        String actual = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(expected, actual, description);
    }

    /* ---------- Group: Inner class name with dollar sign ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("innerClassDollarProvider")
    void testInnerClassDollarSign(String description, int[] byteCodeInts, String expected) throws IOException {
        byte[] byteCode = intsToBytes(byteCodeInts);
        String actual = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(expected, actual, description);
    }

    /* ---------- Group: Constant pool includes STRING tag (ignored) ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringTagIgnoredProvider")
    void testStringTagIgnored(String description, int[] byteCodeInts, String expected) throws IOException {
        byte[] byteCode = intsToBytes(byteCodeInts);
        String actual = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(expected, actual, description);
    }

    /* ---------- Group: Invalid this_class index (out of range) ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidThisClassIndexProvider")
    void testInvalidThisClassIndex(String description, int[] byteCodeInts, Class<? extends Throwable> expectedException) {
        byte[] byteCode = intsToBytes(byteCodeInts);
        assertThrows(expectedException, () -> ReflectionUtils.getClassNameFromByteCode(byteCode), description);
    }

    /* ---------- Group: Negative constant pool count ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("negativeConstantPoolCountProvider")
    void testNegativeConstantPoolCount(String description, int[] byteCodeInts, Class<? extends Throwable> expectedException) {
        byte[] byteCode = intsToBytes(byteCodeInts);
        assertThrows(expectedException, () -> ReflectionUtils.getClassNameFromByteCode(byteCode), description);
    }

    /* ---------- Group: Insufficient UTF8 bytes leading to IOException ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("insufficientUtf8BytesProvider")
    void testInsufficientUtf8Bytes(String description, int[] byteCodeInts, Class<? extends Throwable> expectedException) {
        byte[] byteCode = intsToBytes(byteCodeInts);
        assertThrows(expectedException, () -> ReflectionUtils.getClassNameFromByteCode(byteCode), description);
    }

    /* ---------- Group: Invalid string index (zero) causing ArrayIndexOutOfBoundsException ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("zeroStringIndexProvider")
    void testZeroStringIndex(String description, int[] byteCodeInts, Class<? extends Throwable> expectedException) {
        byte[] byteCode = intsToBytes(byteCodeInts);
        assertThrows(expectedException, () -> ReflectionUtils.getClassNameFromByteCode(byteCode), description);
    }

    /* ---------- Group: Class entry referencing non‑UTF8 constant causing NullPointerException ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("classRefNonUtf8Provider")
    void testClassRefNonUtf8(String description, int[] byteCodeInts, Class<? extends Throwable> expectedException) {
        byte[] byteCode = intsToBytes(byteCodeInts);
        assertThrows(expectedException, () -> ReflectionUtils.getClassNameFromByteCode(byteCode), description);
    }
}
