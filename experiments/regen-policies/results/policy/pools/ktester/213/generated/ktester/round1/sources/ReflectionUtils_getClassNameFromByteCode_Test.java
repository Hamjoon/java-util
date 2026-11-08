package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

// Mockito not actually used but kept to match template
@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassNameFromByteCode_Test {

    /**
     * A simple class used only for generating valid byte‑code.
     */
    static class SampleClass {
        // intentionally empty
    }

    /**
     * Helper that reads the .class file for the supplied class into a byte array.
     */
    private static byte[] readClassBytes(Class<?> clazz) throws IOException {
        // The class file is located in the same package as the class itself.
        String classFileName = clazz.getSimpleName() + ".class";
        try (InputStream is = clazz.getResourceAsStream(classFileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (is == null) {
                throw new IOException("Unable to locate class file for " + clazz.getName());
            }
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Converts a hex string (e.g. "CAFEBABE") into the corresponding byte array.
     *
     * @param s hex string, may be empty or {@code null}
     * @return byte[] representation or {@code null} if the input is {@code null}
     */
    private static byte[] hexStringToByteArray(String s) {
        if (s == null) {
            return null;
        }
        if (s.isEmpty()) {
            return new byte[0];
        }
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Invalid hex character in: " + s);
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }

    /**
     * Supplies all test cases defined in the JSON document.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>description – used as the display name of the parameterised test</li>
     *   <li>hex byte‑code string (or {@code null})</li>
     *   <li>expected fully‑qualified class name (or {@code null} when an exception is expected)</li>
     *   <li>expected exception type (or {@code null} when a class name is expected)</li>
     * </ul>
     */
    private static Stream<Arguments> provideByteCodeCases() {
        return Stream.of(// Normal class name extraction (UTF8 + CLASS)
        arguments("Normal class name extraction (UTF8 + CLASS)", "CAFEBABE000000340003010010636F6D2F6578616D706C652F5465737407000100000002", "com.example.Test", null), // Constant_INTEGER handling
        arguments("Constant_INTEGER handling", "CAFEBABE0000003400040300000001010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_FLOAT handling
        arguments("Constant_FLOAT handling", "CAFEBABE000000340004043F800000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_LONG handling (takes two CP entries)
        arguments("Constant_LONG handling (takes two CP entries)", "CAFEBABE000000340005050000000000000001010010636F6D2F6578616D706C652F5465737407000300000004", "com.example.Test", null), // Constant_DOUBLE handling (takes two CP entries)
        arguments("Constant_DOUBLE handling (takes two CP entries)", "CAFEBABE000000340005063FF0000000000000010010636F6D2F6578616D706C652F5465737407000300000004", "com.example.Test", null), // Constant_STRING handling
        arguments("Constant_STRING handling", "CAFEBABE000000340004080000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_FIELDREF handling
        arguments("Constant_FIELDREF handling", "CAFEBABE0000003400040900000000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_METHODREF handling
        arguments("Constant_METHODREF handling", "CAFEBABE0000003400040A00000000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_INTERFACEMETHODREF handling
        arguments("Constant_INTERFACEMETHODREF handling", "CAFEBABE0000003400040B00000000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_NAMEANDTYPE handling
        arguments("Constant_NAMEANDTYPE handling", "CAFEBABE0000003400040C00000000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_METHODHANDLE handling
        arguments("Constant_METHODHANDLE handling", "CAFEBABE0000003400040F000000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_METHODTYPE handling
        arguments("Constant_METHODTYPE handling", "CAFEBABE000000340004100000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_DYNAMIC handling
        arguments("Constant_DYNAMIC handling", "CAFEBABE0000003400041100000000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_INVOKEDYNAMIC handling
        arguments("Constant_INVOKEDYNAMIC handling", "CAFEBABE0000003400041200000000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_MODULE handling
        arguments("Constant_MODULE handling", "CAFEBABE000000340004130000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Constant_PACKAGE handling
        arguments("Constant_PACKAGE handling", "CAFEBABE000000340004140000010010636F6D2F6578616D706C652F5465737407000200000003", "com.example.Test", null), // Invalid constant pool tag – exception path
        arguments("Invalid constant pool tag – exception path", "CAFEBABE00000034000263", null, IllegalStateException.class), // Multiple constant pool entries with class name later
        arguments("Multiple constant pool entries with class name later", "CAFEBABE000000340006010010636F6D2F6578616D706C652F54657374070001030000007B010006756E7573656407000400010002", "com.example.Test", null), // Inner class name handling (dollar sign)
        arguments("Inner class name handling (dollar sign)", "CAFEBABE000000340003011700636F6D2F6578616D706C652F4F7574657224496E6E657207000100010002", "com.example.Outer$Inner", null), // Empty byte array – EOFException
        arguments("Empty byte array – EOFException", // empty string → empty byte[]
        "", null, IOException.class), // Null byteCode argument – NullPointerException
        arguments("Null byteCode argument – NullPointerException", null, null, NullPointerException.class), // Constant pool count zero – NegativeArraySizeException
        arguments("Constant pool count zero – NegativeArraySizeException", "CAFEBABE000000340000", null, NegativeArraySizeException.class), // Invalid constant pool tag 0 – IllegalStateException
        arguments("Invalid constant pool tag 0 – IllegalStateException", "CAFEBABE00000034000200", null, IllegalStateException.class), // Malformed UTF8 length – IOException
        arguments("Malformed UTF8 length – IOException", "CAFEBABE000000340002010005414207000100010002", null, IOException.class), // thisClassIndex zero – ArrayIndexOutOfBoundsException
        arguments("thisClassIndex zero – ArrayIndexOutOfBoundsException", "CAFEBABE0000003400030100014107000100010000", null, ArrayIndexOutOfBoundsException.class), // thisClassIndex out of range – ArrayIndexOutOfBoundsException
        arguments("thisClassIndex out of range – ArrayIndexOutOfBoundsException", "CAFEBABE0000003400030100014107000100010004", null, ArrayIndexOutOfBoundsException.class), // Class entry string index zero – ArrayIndexOutOfBoundsException
        arguments("Class entry string index zero – ArrayIndexOutOfBoundsException", "CAFEBABE00000034000207000000010001", null, ArrayIndexOutOfBoundsException.class), // Class entry string index out of range – ArrayIndexOutOfBoundsException
        arguments("Class entry string index out of range – ArrayIndexOutOfBoundsException", "CAFEBABE0000003400030100014107000300010002", null, ArrayIndexOutOfBoundsException.class), // Missing UTF8 entry – NullPointerException
        arguments("Missing UTF8 entry – NullPointerException", "CAFEBABE00000034000307000007000100010002", null, NullPointerException.class), // EOF before constant‑pool tag – IllegalStateException
        arguments("EOF before constant‑pool tag – IllegalStateException", "CAFEBABE000000340002", null, IllegalStateException.class));
    }

    @Test
    void getClassNameFromByteCode_returnsFullyQualifiedName() throws IOException {
        // GIVEN – a valid .class byte array for SampleClass
        byte[] classBytes = readClassBytes(SampleClass.class);
        // WHEN – the utility extracts the class name
        String className = ReflectionUtils.getClassNameFromByteCode(classBytes);
        // THEN – the returned name matches the fully qualified name of SampleClass
        assertEquals(SampleClass.class.getName(), className);
    }

    @Test
    void getClassNameFromByteCode_invalidByteArray_throwsIOException() {
        // GIVEN – an obviously invalid byte array (empty)
        byte[] invalidBytes = new byte[0];
        // WHEN / THEN – an IOException (or subclass) is thrown
        assertThrows(IOException.class, () -> ReflectionUtils.getClassNameFromByteCode(invalidBytes));
    }

    @Test
    void getClassNameFromByteCode_corruptedConstantPool_throwsIllegalStateException() throws IOException {
        // GIVEN – a valid class byte array that we deliberately corrupt by changing a constant‑pool tag
        byte[] classBytes = readClassBytes(SampleClass.class);
        // The constant‑pool tag is the first byte after the magic number (4 bytes) and version numbers (4 bytes)
        // We'll set it to an undefined value (e.g., 0xFF) to trigger the IllegalStateException path.
        // 4 (magic) + 2 (minor) + 2 (major)
        int corruptIndex = 8;
        if (classBytes.length > corruptIndex) {
            classBytes[corruptIndex] = (byte) 0xFF;
        }
        // WHEN / THEN – an IllegalStateException is thrown because the tag is unrecognised
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ReflectionUtils.getClassNameFromByteCode(classBytes));
        assertTrue(ex.getMessage().contains("Unrecognized constant pool tag"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideByteCodeCases")
    void testGetClassNameFromByteCode(String description, String hexByteCode, String expectedClassName, Class<? extends Throwable> expectedException) throws IOException {
        byte[] byteCode = hexStringToByteArray(hexByteCode);
        if (expectedException != null) {
            // The method declares IOException, but many test cases expect other runtime exceptions.
            // assertThrows works for both checked and unchecked exceptions.
            Throwable thrown = assertThrows(expectedException, () -> ReflectionUtils.getClassNameFromByteCode(byteCode));
            // For IOException‑related cases we could further assert the cause/message,
            // but the JSON only requires the type check.
        } else {
            // Normal successful extraction
            String actual = ReflectionUtils.getClassNameFromByteCode(byteCode);
            assertEquals(expectedClassName, actual, () -> "Expected class name does not match for case: " + description);
        }
    }
}
