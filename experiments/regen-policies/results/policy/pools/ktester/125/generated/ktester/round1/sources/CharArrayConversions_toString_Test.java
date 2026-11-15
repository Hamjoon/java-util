package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Array;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toString_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    /* -------------------------------------------------------------
     *  Dummy Converter implementation used in one test case
     * ------------------------------------------------------------- */
    private static class DummyConverter implements Converter {
        // No implementation needed; the method under test never invokes it.
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to a clean state for the next test.
        MockitoAnnotations.openMocks(this).close();
    }

    private static Stream<Arguments> validConversionProvider() {
        // Helper to create a char[] from a String literal (including surrogate pairs)
        java.util.function.Function<String, char[]> toCharArray = s -> s.toCharArray();
        return Stream.of(// simple non‑empty array
        arguments("Converts a normal non‑empty char array to the expected String.", new char[] { 'h', 'e', 'l', 'l', 'o' }, null, "hello"), // empty array
        arguments("Converts an empty char array; result should be an empty String.", new char[] {}, null, ""), // Unicode non‑ASCII characters
        arguments("Converts a char array containing Unicode characters; verifies proper handling of non‑ASCII symbols.", // ©€
        new char[] { '\u00A9', '\u20AC' }, null, "©€"), // large array of 1000 'a' characters
        arguments("Converts a large char array (1000 'a' characters) to ensure performance and correctness on big inputs.", "a".repeat(1000).toCharArray(), null, "a".repeat(1000)), // non‑null Converter instance (method ignores it)
        arguments("Passes a non‑null Converter instance even though the method does not use it; result should still be correct.", new char[] { 'J', 'a', 'v', 'a' }, new DummyConverter(), "Java"), // surrogate pair representing an emoji
        arguments("Converts a char array containing a surrogate pair representing an emoji; verifies that the method correctly builds a String with Unicode supplementary characters.", // 😀
        new char[] { '\uD83D', '\uDE00' }, null, "😀"), // array containing null characters
        arguments("Includes null characters within the array to ensure they are preserved in the resulting String.", new char[] { '\u0000', 'a', '\u0000' }, null, "\u0000a\u0000"));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// null source
        arguments("When the source object is null, the cast to char[] triggers a NullPointerException.", null, null, NullPointerException.class), // wrong type: String
        arguments("Passing a String instead of a char[] should cause a ClassCastException.", "not a char array", null, ClassCastException.class), // wrong type: int[]
        arguments("Passing an int[] (or any non‑char[] array) should also result in a ClassCastException.", new int[] { 1, 2, 3 }, null, ClassCastException.class), // boolean[]
        arguments("Passing a boolean[] should also trigger a ClassCastException because the method expects a char[].", new boolean[] { true, false }, null, ClassCastException.class), // byte[]
        arguments("Passing a byte[] should result in a ClassCastException.", new byte[] { 1, 2 }, null, ClassCastException.class), // arbitrary Object
        arguments("Passing an arbitrary Object that is not an array should cause a ClassCastException.", new Object(), null, ClassCastException.class));
    }

    @Test
    @DisplayName("toString should convert a non‑empty char[] to the expected String")
    void toString_convertsCharArrayToString() {
        // given
        char[] source = { 'J', 'a', 'v', 'a' };
        // when
        String result = CharArrayConversions.toString(source, mockConverter);
        // then
        assertEquals("Java", result);
    }

    @Test
    @DisplayName("toString should return an empty String for an empty char[]")
    void toString_emptyArrayReturnsEmptyString() {
        // given
        char[] source = {};
        // when
        String result = CharArrayConversions.toString(source, mockConverter);
        // then
        assertEquals("", result);
    }

    @Test
    @DisplayName("toString should throw NullPointerException when the source is null")
    void toString_nullSourceThrowsException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toString(source, mockConverter));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    @DisplayName("Valid char array conversions")
    void toString_validConversions(String description, Object from, Converter converter, String expected) {
        // when
        String result = CharArrayConversions.toString(from, converter);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionProvider")
    @DisplayName("Invalid inputs / exception paths")
    void toString_invalidInputs(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> CharArrayConversions.toString(from, converter), description);
    }

    @Test
    @DisplayName("Valid char array conversion - large mixed content")
    void toString_largeMixedContent() {
        // Build a small prefix with normal, Unicode and surrogate characters
        // abc©€😀
        char[] prefix = new char[] { 'a', 'b', 'c', '\u00A9', '\u20AC', '\uD83D', '\uDE00' };
        // Append 1000 null characters
        // default filled with '\u0000'
        char[] suffix = new char[1000];
        // Combine into one array
        char[] combined = new char[prefix.length + suffix.length];
        System.arraycopy(prefix, 0, combined, 0, prefix.length);
        System.arraycopy(suffix, 0, combined, prefix.length, suffix.length);
        // Expected string: prefix + 1000 null chars
        String expected = new String(prefix) + "\u0000".repeat(1000);
        // when
        String result = CharArrayConversions.toString(combined, null);
        // then
        assertEquals(expected, result);
    }
}
