package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import java.util.stream.Stream;

class CharacterArrayConversions_toStringBuffer_Test {

    @Mock
    private Converter // Converter is not used by the method but required as a parameter
    mockConverter;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for @Mock fields
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close Mockito resources
        mocks.close();
    }

    /**
     * Provides a stream of arguments for the normal conversion cases.
     * Each argument consists of:
     *   1. The {@code Character[]} input (or {@code null} for the “null input” case – handled separately)
     *   2. The {@code Converter} instance to pass (may be {@code null} or a mock)
     *   3. The expected {@link StringBuffer} content as a {@code String}
     */
    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// Typical non‑empty array
        Arguments.of(new Character[] { 'J', 'a', 'v', 'a' }, null, "Java"), // Empty array
        Arguments.of(new Character[] {}, null, ""), // Single element
        Arguments.of(new Character[] { 'Z' }, null, "Z"), // Array containing a null element
        Arguments.of(new Character[] { 'A', null, 'C' }, null, "AnullC"), // Multiple null elements
        Arguments.of(new Character[] { null, null }, null, "nullnull"), // Null element at the beginning
        Arguments.of(new Character[] { null, 'B' }, null, "nullB"), // Control characters: newline, tab, carriage return
        Arguments.of(new Character[] { '\n', '\t', '\r' }, null, "\n\t\r"), // Surrogate pair representing 😀 (U+1F600)
        Arguments.of(new Character[] { '\uD83D', '\uDE00' }, null, "😀"), // Null character (code point 0)
        Arguments.of(new Character[] { '\0' }, null, "\0"), // Two‑character array with a non‑null mock converter
        Arguments.of(new Character[] { 'X', 'Y' }, mock(Converter.class), "XY"));
    }

    @Test
    void toStringBuffer_WithMultipleCharacters_ReturnsCorrectBuffer() {
        // GIVEN
        Character[] input = new Character[] { 'J', 'a', 'v', 'a' };
        // WHEN
        StringBuffer result = CharacterArrayConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should not be null");
        assertEquals("Java", result.toString(), "StringBuffer content should match the characters");
        // Ensure a new instance is returned (not the same as any cached object)
        assertNotSame(input, result);
    }

    @Test
    void toStringBuffer_WithEmptyArray_ReturnsEmptyBuffer() {
        // GIVEN
        Character[] input = new Character[0];
        // WHEN
        StringBuffer result = CharacterArrayConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(0, result.length(), "Buffer length should be zero for empty input");
    }

    @Test
    void toStringBuffer_WithNullArray_ThrowsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuffer(input, mockConverter), "Passing null should result in NullPointerException");
    }

    @Test
    void toStringBuffer_WithArrayContainingNullElements_AppendsStringNull() {
        // GIVEN
        Character[] input = new Character[] { 'A', null, 'B' };
        // WHEN
        StringBuffer result = CharacterArrayConversions.toStringBuffer(input, mockConverter);
        // THEN
        // The null element is appended via StringBuffer.append(Object) which yields "null"
        assertEquals("AnullB", result.toString(), "Null elements should be represented by the string \"null\"");
    }

    @ParameterizedTest(name = "[{index}] from={0}, expected=\"{2}\"")
    @MethodSource("normalCasesProvider")
    void toStringBuffer_NormalCases(Character[] input, Converter converter, String expectedContent) {
        // GIVEN
        // (input and converter are supplied by the method source)
        // WHEN
        StringBuffer result = CharacterArrayConversions.toStringBuffer(input, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuffer should never be null");
        assertEquals(expectedContent, result.toString(), () -> "Expected buffer content to be \"" + expectedContent + "\"");
        // The method always creates a new StringBuffer instance
        assertNotSame(input, result, "Result should be a new StringBuffer, not the original array");
    }

    @Test
    void toStringBuffer_NullFrom_ThrowsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuffer(nullInput, mockConverter), "Passing null for 'from' must throw NullPointerException");
    }

    @Test
    void toStringBuffer_NonCharacterArray_ThrowsClassCastException() {
        // GIVEN
        Object notAnArray = "Not a Character array";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toStringBuffer(notAnArray, mockConverter), "Supplying a non‑Character[] object should raise ClassCastException");
    }

    @Test
    void toStringBuffer_PrimitiveCharArray_ThrowsClassCastException() {
        // GIVEN
        Object primitiveArray = new char[] { 'a', 'b' };
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toStringBuffer(primitiveArray, mockConverter), "Supplying a primitive char[] should raise ClassCastException");
    }

    @Test
    void toStringBuffer_ConverterMockThrowsIfInvoked_MethodIgnoresConverter() {
        // GIVEN
        // Create a mock that throws on *any* method invocation.
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        Character[] input = new Character[] { 'A', 'B' };
        // WHEN – no exception should be thrown because the method never calls the converter
        StringBuffer result = CharacterArrayConversions.toStringBuffer(input, throwingConverter);
        // THEN
        assertNotNull(result);
        assertEquals("AB", result.toString(), "Even when the converter throws, the method should succeed because it never uses it");
        // Verify that no interaction with the mock occurred
        verifyNoInteractions(throwingConverter);
    }
}
