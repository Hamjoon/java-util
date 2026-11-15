package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.EOFException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassNameFromByteCode_Test {

    /**
     * Loads the raw .class bytes for the supplied class using the same
     * class‑loader that loaded the class.
     *
     * @param clazz the class whose byte‑code should be read
     * @return a byte array containing the class file contents
     * @throws IOException if the class resource cannot be read
     */
    private static byte[] loadClassBytes(Class<?> clazz) throws IOException {
        String resource = clazz.getSimpleName() + ".class";
        try (java.io.InputStream is = clazz.getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: " + resource);
            }
            return is.readAllBytes();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    private static byte[] toBytes(int... values) {
        byte[] arr = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            arr[i] = (byte) values[i];
        }
        return arr;
    }

    /* --------------------------------------------------------------------- */
    private static Stream<org.junit.jupiter.params.provider.Arguments> validNameProvider() {
        return Stream.of(// Simple UTF8 + Class entries
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, // constant_pool_count = 3 (+1)
        0, // constant_pool_count = 3 (+1)
        3, // #1 UTF8 "com/example/Test"
        1, // #1 UTF8 "com/example/Test"
        0, // #1 UTF8 "com/example/Test"
        16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, // #2 Class -> string_index = 1
        7, // #2 Class -> string_index = 1
        0, // #2 Class -> string_index = 1
        1, // access flags (ignored)
        0, // access flags (ignored)
        33, // this_class = 2
        0, // this_class = 2
        2, 0, // super_class (ignored)
        0), "com.example.Test"), // CONSTANT_Integer (tag 3) before UTF8/Class
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 4, // #1 Integer (skip 4 bytes)
        3, // #1 Integer (skip 4 bytes)
        0, // #1 Integer (skip 4 bytes)
        0, // #1 Integer (skip 4 bytes)
        0, // #1 Integer (skip 4 bytes)
        0, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // CONSTANT_Long (tag 5) occupies two entries
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 5, // #1 Long (8 bytes, consumes two slots)
        5, // #1 Long (8 bytes, consumes two slots)
        0, // #1 Long (8 bytes, consumes two slots)
        0, // #1 Long (8 bytes, consumes two slots)
        0, // #1 Long (8 bytes, consumes two slots)
        0, // #1 Long (8 bytes, consumes two slots)
        0, // #1 Long (8 bytes, consumes two slots)
        0, // #1 Long (8 bytes, consumes two slots)
        0, // #1 Long (8 bytes, consumes two slots)
        1, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // CONSTANT_MethodHandle (tag 15)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 4, // tag 15 + reference_kind + reference_index
        15, // tag 15 + reference_kind + reference_index
        1, // tag 15 + reference_kind + reference_index
        0, // tag 15 + reference_kind + reference_index
        1, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // CONSTANT_MethodType (tag 16)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 4, // tag 16 + descriptor_index
        16, // tag 16 + descriptor_index
        0, // tag 16 + descriptor_index
        1, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // CONSTANT_Dynamic (17) and CONSTANT_InvokeDynamic (18)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 5, // tag 17 + two shorts
        17, // tag 17 + two shorts
        0, // tag 17 + two shorts
        1, // tag 17 + two shorts
        0, // tag 17 + two shorts
        2, // tag 18 + two shorts
        18, // tag 18 + two shorts
        0, // tag 18 + two shorts
        3, // tag 18 + two shorts
        0, // tag 18 + two shorts
        4, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // CONSTANT_Module (19) and CONSTANT_Package (20)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 5, // tag 19 + name_index
        19, // tag 19 + name_index
        0, // tag 19 + name_index
        1, // tag 20 + name_index
        20, // tag 20 + name_index
        0, // tag 20 + name_index
        2, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 3, 0, 33, 0, 4, 0, 0), "com.example.Test"), // CONSTANT_Float (tag 4)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 4, // Float payload
        4, // Float payload
        0, // Float payload
        0, // Float payload
        0, // Float payload
        0, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // CONSTANT_Double (tag 6) occupies two entries, placed after UTF8/Class
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 5, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 1, // Double payload (8 bytes)
        6, // Double payload (8 bytes)
        0, // Double payload (8 bytes)
        0, // Double payload (8 bytes)
        0, // Double payload (8 bytes)
        0, // Double payload (8 bytes)
        0, // Double payload (8 bytes)
        0, // Double payload (8 bytes)
        0, // Double payload (8 bytes)
        0, 0, 33, 0, 2, 0, 0), "com.example.Test"), // CONSTANT_String (tag 8)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 4, // tag 8 + string_index
        8, // tag 8 + string_index
        0, // tag 8 + string_index
        2, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // CONSTANT_Fieldref (tag 9)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 4, // tag 9 + class_index + name_and_type_index
        9, // tag 9 + class_index + name_and_type_index
        0, // tag 9 + class_index + name_and_type_index
        1, // tag 9 + class_index + name_and_type_index
        0, // tag 9 + class_index + name_and_type_index
        2, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // CONSTANT_Methodref (tag 10)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 4, 10, 0, 1, 0, 2, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // CONSTANT_InterfaceMethodref (tag 11)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 4, 11, 0, 1, 0, 2, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // CONSTANT_NameAndType (tag 12)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 4, 12, 0, 1, 0, 2, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), "com.example.Test"), // Inner class name with '$'
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 3, 1, 0, 23, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 79, 117, 116, 101, 114, 36, 73, 110, 110, 101, 114, 7, 0, 1, 0, 33, 0, 2, 0, 0), "com.example.Outer$Inner"), // Default package (no '/')
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 3, 1, 0, 4, 84, 101, 115, 116, 7, 0, 1, 0, 33, 0, 2, 0, 0), "Test"));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<org.junit.jupiter.params.provider.Arguments> exceptionProvider() {
        return Stream.of(// Invalid tag (99) -> IllegalStateException
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 3, 99, 1, 0, 16, 99, 111, 109, 47, 101, 120, 97, 109, 112, 108, 101, 47, 84, 101, 115, 116, 7, 0, 2, 0, 33, 0, 3, 0, 0), IllegalStateException.class), // EOF while reading header -> EOFException
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190), EOFException.class), // Null input -> NullPointerException
        org.junit.jupiter.params.provider.Arguments.of(null, NullPointerException.class), // Negative array size (cpcnt = -1)
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 0), NegativeArraySizeException.class), // ArrayIndexOutOfBounds due to zero cpcnt but later access
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 1, 0, 0, 0, 1), ArrayIndexOutOfBoundsException.class), // thisClassIndex out of bounds
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 2, 1, 0, 0, 0, 0, 0, 2), ArrayIndexOutOfBoundsException.class), // String index out of bounds -> NullPointerException
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 3, 1, 0, 4, 84, 101, 115, 116, 7, 0, 2, 0, 0, 0, 2), NullPointerException.class), // EOF during constant‑pool tag read -> IllegalStateException
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 2), IllegalStateException.class), // thisClassIndex zero -> ArrayIndexOutOfBoundsException
        org.junit.jupiter.params.provider.Arguments.of(toBytes(202, 254, 186, 190, 0, 0, 0, 52, 0, 3, 1, 0, 4, 84, 101, 115, 116, 7, 0, 1, 0, 0, 0, 0), ArrayIndexOutOfBoundsException.class));
    }

    @Test
    void getClassNameFromByteCode_returnsCorrectName_forValidClass() throws Exception {
        // GIVEN: byte‑code of java.lang.String
        byte[] stringClassBytes = loadClassBytes(String.class);
        // WHEN: the utility method is invoked
        String className = ReflectionUtils.getClassNameFromByteCode(stringClassBytes);
        // THEN: the fully‑qualified class name is returned
        assertEquals("java.lang.String", className);
    }

    @Test
    void getClassNameFromByteCode_throwsException_forInvalidByteCode() {
        // GIVEN: an obviously malformed byte‑code array (too short)
        byte[] malformed = new byte[] { 0, 1, 2, 3 };
        // WHEN / THEN: the method should throw an exception (any checked/unchecked)
        assertThrows(Exception.class, () -> ReflectionUtils.getClassNameFromByteCode(malformed));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] extracts ''{1}''")
    @MethodSource("validNameProvider")
    @DisplayName("Valid class name extraction")
    void testGetClassNameFromByteCode_valid(byte[] byteCode, String expected) throws Exception {
        // WHEN
        String actual = ReflectionUtils.getClassNameFromByteCode(byteCode);
        // THEN
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] expects {1}")
    @MethodSource("exceptionProvider")
    @DisplayName("Exception handling")
    void testGetClassNameFromByteCode_exceptions(byte[] byteCode, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> ReflectionUtils.getClassNameFromByteCode(byteCode));
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Real class file (java.lang.String) yields correct name")
    void getClassNameFromByteCode_realClass() throws Exception {
        // GIVEN a real .class file
        byte[] bytes = loadClassBytes(String.class);
        // WHEN
        String name = ReflectionUtils.getClassNameFromByteCode(bytes);
        // THEN
        assertEquals("java.lang.String", name);
    }

    @Test
    @DisplayName("Obvious malformed byte array throws generic Exception")
    void getClassNameFromByteCode_malformed() {
        byte[] malformed = new byte[] { 0, 1, 2, 3 };
        assertThrows(Exception.class, () -> ReflectionUtils.getClassNameFromByteCode(malformed));
    }
}
