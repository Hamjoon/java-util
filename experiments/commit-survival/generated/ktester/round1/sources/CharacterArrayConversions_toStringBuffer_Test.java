package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterArrayConversions_toStringBuffer_Test {

    @Mock
    private Converter // Converter is not used by the method but must be supplied
    mockConverter;

    /* -------------------------------------------------------------
     *  Helper class to bundle parameterised test arguments
     * ------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Character[] from;

        final Converter converter;

        final String expected;

        final String description;

        ArgumentsHolder(Character[] from, Converter converter, String expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /* -------------------------------------------------------------
     *  Minimal NoOpConverter implementation used in tests where a real
     *  instance is required. The actual conversion logic is irrelevant.
     * ------------------------------------------------------------- */
    private static class NoOpConverter implements Converter {
        // No methods are required for the current test suite because
        // CharacterArrayConversions.toStringBuffer never invokes any Converter API.
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure any interactions with the mock are cleared between tests
        clearInvocations(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for the "Standard conversion" group
     * ------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> standardConversionProvider() {
        return Stream.of(new ArgumentsHolder(new Character[] { 'J', 'a', 'v', 'a' }, new NoOpConverter(), "Java", "Typical non‑empty array"), new ArgumentsHolder(new Character[] { 'T', 'e', 's', 't', null, '1' }, null, "Testnull1", "Array containing a null element"));
    }

    /* -------------------------------------------------------------
     *  Invalid type handling – various inputs that are not Character[]
     * ------------------------------------------------------------- */
    private static Stream<Object> invalidInputsProvider() {
        return Stream.of(// primitive int array
        new int[] { 1, 2, 3 }, // String[]
        new String[] { "a", "b" }, // non‑array object
        Integer.valueOf(42), // single Character, not an array
        Character.valueOf('A'));
    }

    @Test
    void toStringBuffer_withTypicalCharacterArray_returnsCorrectBuffer() {
        // given
        Character[] input = new Character[] { 'J', 'a', 'v', 'a' };
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(input, mockConverter);
        // then
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals("Java", result.toString(), "StringBuffer content should match the characters");
        // Verify that the converter was never used (method does not depend on it)
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_withEmptyArray_returnsEmptyBuffer() {
        // given
        Character[] input = new Character[0];
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(input, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(0, result.length(), "Buffer should be empty for an empty input array");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_withNullElements_appendsStringNull() {
        // given
        Character[] input = new Character[] { 'A', null, 'B' };
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(input, mockConverter);
        // then
        assertNotNull(result);
        // The null element is converted to the string "null" by StringBuffer.append(Object)
        assertEquals("AnullB", result.toString(), "Null elements should be rendered as the string \"null\"");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_withInvalidObject_throwsClassCastException() {
        // given
        Object notACharacterArray = new int[] { 1, 2, 3 };
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toStringBuffer(notACharacterArray, mockConverter), "Passing a non‑Character[] should result in a ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("standardConversionProvider")
    void toStringBuffer_standardConversion(ArgumentsHolder holder) {
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(holder.from, holder.converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(holder.expected, result.toString(), "StringBuffer content should match expected value");
        // The method never touches the converter, regardless of it being null or a real instance.
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Empty array test
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_emptyArray_returnsEmptyBuffer() {
        // given
        Character[] input = new Character[0];
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(input, mockConverter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(0, result.length(), "Buffer should be empty for an empty array");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Null 'from' argument – should throw NPE because of cast/length access
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_nullFrom_throwsNullPointerException() {
        // given
        Object nullFrom = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuffer(nullFrom, mockConverter), "Passing null for 'from' should result in NullPointerException");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] Invalid input: {0}")
    @MethodSource("invalidInputsProvider")
    void toStringBuffer_invalidInput_throwsClassCastException(Object invalid) {
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toStringBuffer(invalid, mockConverter), "Invalid input should cause ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Primitive char[] handling – also results in ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_primitiveCharArray_throwsClassCastException() {
        char[] primitive = new char[] { 'a', 'b' };
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toStringBuffer(primitive, mockConverter), "primitive char[] cannot be cast to Character[]");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Single element array test
     * ------------------------------------------------------------- */
    @Test
    void toStringBuffer_singleElementArray_returnsSingleCharBuffer() {
        Character[] input = new Character[] { 'Z' };
        StringBuffer result = CharacterArrayConversions.toStringBuffer(input, mockConverter);
        assertNotNull(result);
        assertEquals("Z", result.toString(), "Single element array should produce a buffer with that character");
        verifyNoInteractions(mockConverter);
    }
}
