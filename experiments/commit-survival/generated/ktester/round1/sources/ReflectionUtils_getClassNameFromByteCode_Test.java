package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.EOFException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassNameFromByteCode_Test {

    /**
     * Helper that reads the *.class file of the supplied class as a byte array.
     */
    private static byte[] readClassBytes(Class<?> clazz) throws IOException {
        String resource = '/' + clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getResourceAsStream(resource)) {
            assertNotNull(is, "Unable to locate class file for " + clazz.getName());
            return is.readAllBytes();
        }
    }

    /**
     * Convert a hex‑encoded string (no spaces) to a byte array.
     */
    private static byte[] hexStringToByteArray(String s) {
        if (s == null) {
            return null;
        }
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static Stream<Arguments> successfulByteCodeProvider() {
        return Stream.of(// CONSTANT_Utf8 (tag 1)
        Arguments.of("Valid tag handling - CONSTANT_Utf8 (tag 1)", "CAFEBABE00000034000501000544756D6D79010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_Integer (tag 3)
        Arguments.of("Valid tag handling - CONSTANT_Integer (tag 3)", "CAFEBABE000000340005030000002A010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_Float (tag 4)
        Arguments.of("Valid tag handling - CONSTANT_Float (tag 4)", "CAFEBABE000000340005040041200000010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_Long (tag 5)
        Arguments.of("Valid tag handling - CONSTANT_Long (tag 5)", "CAFEBABE000000340005050000000000000001010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_Double (tag 6)
        Arguments.of("Valid tag handling - CONSTANT_Double (tag 6)", "CAFEBABE000000340005060000000000000001010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_Class (tag 7)
        Arguments.of("Valid tag handling - CONSTANT_Class (tag 7)", "CAFEBABE000000340005070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_String (tag 8)
        Arguments.of("Valid tag handling - CONSTANT_String (tag 8)", "CAFEBABE00000034000508000201000544756D6D79070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_Fieldref (tag 9)
        Arguments.of("Valid tag handling - CONSTANT_Fieldref (tag 9)", "CAFEBABE0000003400050900030004010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_Methodref (tag 10)
        Arguments.of("Valid tag handling - CONSTANT_Methodref (tag 10)", "CAFEBABE0000003400050A00030004010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_InterfaceMethodref (tag 11)
        Arguments.of("Valid tag handling - CONSTANT_InterfaceMethodref (tag 11)", "CAFEBABE0000003400050B00030004010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_NameAndType (tag 12)
        Arguments.of("Valid tag handling - CONSTANT_NameAndType (tag 12)", "CAFEBABE0000003400050C00030004010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_MethodHandle (tag 15)
        Arguments.of("Valid tag handling - CONSTANT_MethodHandle (tag 15)", "CAFEBABE0000003400050F010003010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_MethodType (tag 16)
        Arguments.of("Valid tag handling - CONSTANT_MethodType (tag 16)", "CAFEBABE000000340005100003010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_Dynamic (tag 17)
        Arguments.of("Valid tag handling - CONSTANT_Dynamic (tag 17)", "CAFEBABE0000003400051100030004010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_InvokeDynamic (tag 18)
        Arguments.of("Valid tag handling - CONSTANT_InvokeDynamic (tag 18)", "CAFEBABE0000003400051200030004010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_Module (tag 19)
        Arguments.of("Valid tag handling - CONSTANT_Module (tag 19)", "CAFEBABE000000340005130004010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // CONSTANT_Package (tag 20)
        Arguments.of("Valid tag handling - CONSTANT_Package (tag 20)", "CAFEBABE000000340005140004010000070004010013636F6D2F6578616D706C652F5465737400210003", "com.example.Test"), // Mixed constant pool entries
        Arguments.of("Mixed constant pool entries", "CAFEBABE00000034000701000769676E6F7265640700030100136F72672F6578616D706C652F4D79436C6173730300000001043F80000008000300210002", "org.example.MyClass"), // Class name with inner class and package separators
        Arguments.of("Class name with inner class and package separators", "CAFEBABE000000340003010017636F6D2F6578616D706C652F4F7574657224496E6E657207000100210002", "com.example.Outer$Inner"));
    }

    private static Stream<Arguments> exceptionByteCodeProvider() {
        return Stream.of(// Unknown tag (0xFF)
        Arguments.of("Invalid tag handling - unknown tag", "CAFEBABE000000340005FF0000010000070004010013636F6D2F6578616D706C652F5465737400210003", IllegalStateException.class), // Zero constant pool entries (cpcnt = 0) -> ArrayIndexOutOfBoundsException
        Arguments.of("Zero constant pool entries (cpcnt = 0)", "CAFEBABE00000034000100210001", ArrayIndexOutOfBoundsException.class), // Invalid this_class index (out of range) -> ArrayIndexOutOfBoundsException
        Arguments.of("Invalid this_class index (out of range)", "CAFEBABE0000003400050100014107000101000142030000000000210005", ArrayIndexOutOfBoundsException.class), // Truncated bytecode (EOF) after magic/minor -> EOFException
        Arguments.of("Truncated bytecode (EOF)", "CAFEBABE0000", EOFException.class), // Unknown tag value 0 -> IllegalStateException
        Arguments.of("Unknown tag value 0", "CAFEBABE00000034000500", IllegalStateException.class), // Null byteCode argument -> NullPointerException (handled in test)
        Arguments.of("Null byteCode argument", null, NullPointerException.class), // Negative array size due to constant_pool_count = 0 -> NegativeArraySizeException
        Arguments.of("Negative array size due to constant_pool_count = 0", "CAFEBABE000000000000", NegativeArraySizeException.class), // Class entry references non‑UTF8 (NullPointerException)
        Arguments.of("Class entry references non‑UTF8 (NullPointerException)", "CAFEBABE000000000003030000000007000100010002", NullPointerException.class), // Zero this_class index -> ArrayIndexOutOfBoundsException
        Arguments.of("Zero this_class index (ArrayIndexOutOfBoundsException)", "CAFEBABE00000000000301000007000100010000", ArrayIndexOutOfBoundsException.class), // EOF after constant_pool_count (IllegalStateException)
        Arguments.of("EOF after constant_pool_count (IllegalStateException)", "CAFEBABE000000000002", IllegalStateException.class), // EOF while reading this_class index (EOFException)
        Arguments.of("EOF while reading this_class index (EOFException)", "CAFEBABE0000000000020100000001", EOFException.class));
    }

    @Test
    @DisplayName("getClassNameFromByteCode returns the fully‑qualified name of a valid class")
    void testValidClassNameExtraction() throws Exception {
        // GIVEN – the byte‑code of this test class
        byte[] classBytes = readClassBytes(ReflectionUtils_getClassNameFromByteCode_Test.class);
        // WHEN – the utility method is invoked
        String className = ReflectionUtils.getClassNameFromByteCode(classBytes);
        // THEN – the returned name matches the expected fully‑qualified name
        assertEquals(ReflectionUtils_getClassNameFromByteCode_Test.class.getName(), className, "The extracted class name should match the original class name");
    }

    @Test
    @DisplayName("getClassNameFromByteCode throws an exception for malformed byte‑code")
    void testMalformedByteCodeThrows() {
        // GIVEN – an obviously invalid byte array (empty)
        byte[] malformed = new byte[0];
        // WHEN / THEN – the method should throw an Exception (any subclass of Exception)
        assertThrows(Exception.class, () -> ReflectionUtils.getClassNameFromByteCode(malformed));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulByteCodeProvider")
    @DisplayName("Successful parsing of various constant‑pool tags")
    void testSuccessfulParsing(String description, String hex, String expectedClassName) throws Exception {
        byte[] bytes = hexStringToByteArray(hex);
        String actual = ReflectionUtils.getClassNameFromByteCode(bytes);
        assertEquals(expectedClassName, actual, () -> "Failed for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionByteCodeProvider")
    @DisplayName("Parsing byte‑code that should result in an exception")
    void testExceptionScenarios(String description, String hex, Class<? extends Throwable> expectedException) {
        byte[] bytes = hexStringToByteArray(hex);
        assertThrows(expectedException, () -> ReflectionUtils.getClassNameFromByteCode(bytes), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }

    @Test
    @DisplayName("getClassNameFromByteCode throws NullPointerException when byteCode is null")
    void testNullByteCodeArgument() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassNameFromByteCode(null), "Passing null should result in NullPointerException");
    }
}
