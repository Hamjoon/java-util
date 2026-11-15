package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toCharArray_Test {

    @Mock
    private Converter // Converter is not used by the method but required for signature
    mockConverter;

    private char[] source;

    /* ------------------------------------------------------------
     *  Helper dummy Converter implementation for a test case
     * ------------------------------------------------------------ */
    private static class DummyConverter implements Converter {
        // No implementation needed – methods are never called by toCharArray
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (handled by MockitoExtension) and test data
        source = new char[] { 'a', 'b', 'c' };
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up references
        source = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validCharArrayProvider() {
        return Stream.of(arguments(new char[] { 'a', 'b', 'c' }, null, "Pass a non‑empty char[]; method should return a copy with identical contents but a different reference."), arguments(new char[0], null, "Pass an empty char[]; method should return another empty array."), arguments(new char[] { 'x' }, new DummyConverter(), "Pass a single‑element char[] together with a non‑null Converter; the converter is ignored."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidTypeProvider() {
        return Stream.of(arguments("some string", "Pass a String instead of char[]"), arguments(new int[] { 1, 2, 3 }, "Pass an int[] array"), arguments(new Character[] { 'a', 'b' }, "Pass a Character[] object array"), arguments(new char[][] { { 'a', 'b' }, { 'c', 'd' } }, "Pass a two‑dimensional char array"), arguments(new Object(), "Pass a plain Object instance"), arguments(Character.valueOf('z'), "Pass a boxed Character"));
    }

    @Test
    void toCharArray_returnsCopyWithSameContents() {
        char[] result = CharArrayConversions.toCharArray(source, mockConverter);
        // Verify contents are equal
        assertArrayEquals(source, result, "Returned array should contain the same characters");
        // Verify a new array instance is created (defensive copy)
        assertNotSame(source, result, "Returned array should be a different instance");
    }

    @Test
    void toCharArray_isDefensiveCopy_independentFromOriginal() {
        char[] result = CharArrayConversions.toCharArray(source, mockConverter);
        // Mutate the original source array
        source[0] = 'z';
        // Result should remain unchanged
        assertEquals('a', result[0], "Result array must not reflect changes to the original array");
    }

    @Test
    void toCharArray_handlesEmptyArray() {
        source = new char[0];
        char[] result = CharArrayConversions.toCharArray(source, mockConverter);
        assertArrayEquals(source, result, "Empty array should be returned unchanged");
        assertNotSame(source, result, "Even for empty arrays a new instance should be created");
    }

    @Test
    void toCharArray_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toCharArray(null, mockConverter), "Passing null as source should throw NullPointerException");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("validCharArrayProvider")
    void toCharArray_validInputs_returnCopy(char[] input, Converter converter, String description) {
        // Use the supplied converter (may be null)
        char[] result = CharArrayConversions.toCharArray(input, converter);
        assertArrayEquals(input, result, "Result array must contain the same characters");
        assertNotSame(input, result, "Result array must be a new defensive copy");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidTypeProvider")
    void toCharArray_invalidTypes_throwClassCastException(Object input, String description) {
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toCharArray(input, mockConverter), description);
    }

    @Test
    void toCharArray_largeArray_copyIsIndependent() {
        // default '\u0000' values
        char[] large = new char[1024];
        char[] result = CharArrayConversions.toCharArray(large, mockConverter);
        assertEquals(large.length, result.length, "Length must be preserved");
        assertArrayEquals(large, result, "Contents must be identical");
        assertNotSame(large, result, "Result must be a new array instance");
    }

    @Test
    void toCharArray_patternArray_copyPreservesPattern() {
        char[] pattern = IntStream.range(0, 500).map(i -> 'a' + (i % 26)).mapToObj(c -> (char) c).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString().toCharArray();
        char[] result = CharArrayConversions.toCharArray(pattern, mockConverter);
        assertEquals(500, result.length, "Result length should be 500");
        assertArrayEquals(pattern, result, "Pattern must be preserved");
        assertNotSame(pattern, result, "Result array must be a distinct object");
    }

    @Test
    void toCharArray_unicodeCharacters_areCopiedUnchanged() {
        // ©, €, high & low surrogate of 😀
        char[] unicode = new char[] { '\u00A9', '\u20AC', '\uD83D', '\uDE00' };
        char[] result = CharArrayConversions.toCharArray(unicode, mockConverter);
        assertArrayEquals(unicode, result, "Unicode characters must be copied exactly");
        assertNotSame(unicode, result, "Result must be a new array");
    }

    @Test
    void toCharArray_converterWithSideEffects_isIgnored() {
        // Create a mock Converter that would throw if any method is invoked.
        Converter throwingConverter = mock(Converter.class);
        // No stubbing needed because the method under test never calls the converter.
        char[] input = new char[] { 'm', 'n' };
        char[] result = CharArrayConversions.toCharArray(input, throwingConverter);
        assertArrayEquals(input, result, "Result must match the input array");
        assertNotSame(input, result, "Result must be a defensive copy");
    }

    @Test
    void toCharArray_resultModificationDoesNotAffectOriginal() {
        char[] original = new char[] { 'p', 'q', 'r' };
        char[] result = CharArrayConversions.toCharArray(original, mockConverter);
        // Mutate the result
        result[0] = 'z';
        // Original must stay unchanged
        assertEquals('p', original[0], "Original array must remain unchanged after mutating the result");
        assertEquals('z', result[0], "Result array reflects the mutation");
    }
}
