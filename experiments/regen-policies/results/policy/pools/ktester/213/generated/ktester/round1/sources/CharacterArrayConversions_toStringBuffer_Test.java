package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterArrayConversions_toStringBuffer_Test {

    @Mock
    private Converter // Converter is not used by the method but must be supplied
    mockConverter;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for the @Mock fields
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources
        mocks.close();
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Normal operation – typical arrays
        Arguments.of(new Character[] { 'a', 'b', 'c' }, "abc", "Converts a regular Character array of three letters into \"abc\""), Arguments.of(new Character[] { 'X' }, "X", "Single‑element array should produce a StringBuffer with that single character"), Arguments.of(new Character[] {}, "", "Empty Character array should result in an empty StringBuffer"), Arguments.of(new Character[] { '\u2603', '\u00E9' }, "\u2603\u00E9", "Unicode characters (snowman and é) are appended correctly"), // Boundary characters
        Arguments.of(new Character[] { ' ', '\t', '\n' }, " \t\n", "Space, tab and newline characters preserve their visual representation"), Arguments.of(new Character[] { '\u0000' }, "\u0000", "Null character (U+0000) is appended without throwing"), Arguments.of(new Character[] { '\uFFFF' }, "\uFFFF", "Maximum char value (U+FFFF) is handled correctly"), // Surrogate pair handling (two separate code units)
        Arguments.of(new Character[] { '\uD83D', '\uDE00' }, "\uD83D\uDE00", "Two surrogate characters form a smiley face when combined"), // Large array handling (100 'z' characters)
        Arguments.of(Collections.nCopies(100, 'z').toArray(new Character[0]), "z".repeat(100), "Large array of 100 'z' characters should produce a buffer of length 100"));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        return Stream.of(// Null array reference
        Arguments.of(null, NullPointerException.class, "Passing a null reference for `from` causes NullPointerException"), // Wrong type – String[]
        Arguments.of(new String[] { "a", "b" }, ClassCastException.class, "`from` is not a Character[]; the cast triggers ClassCastException"), // Primitive char[]
        Arguments.of(new char[] { 'a', 'b', 'c' }, ClassCastException.class, "Primitive char[] cannot be cast to Character[] and throws ClassCastException"), // Two‑dimensional Character array
        Arguments.of(new Character[][] { { 'a', 'b' }, { 'c', 'd' } }, ClassCastException.class, "Two‑dimensional Character array cannot be cast to Character[]"), // List<Character> (not an array)
        Arguments.of(List.of('x', 'y'), ClassCastException.class, "A List<Character> is not an array; casting results in ClassCastException"));
    }

    @Test
    void toStringBuffer_withTypicalCharacters_returnsCorrectBuffer() {
        // given
        Character[] chars = new Character[] { 'a', 'b', 'c' };
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(chars, mockConverter);
        // then
        assertNotNull(result);
        assertEquals("abc", result.toString());
        assertEquals(3, result.length());
    }

    @Test
    void toStringBuffer_withEmptyArray_returnsEmptyBuffer() {
        // given
        Character[] chars = new Character[0];
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(chars, mockConverter);
        // then
        assertNotNull(result);
        assertEquals("", result.toString());
        assertEquals(0, result.length());
    }

    @Test
    void toStringBuffer_withNullElements_includesStringNull() {
        // given
        Character[] chars = new Character[] { 'x', null, 'y' };
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(chars, mockConverter);
        // then
        assertNotNull(result);
        // StringBuilder/StringBuffer appends "null" for a null Object
        assertEquals("xnully", result.toString());
        assertEquals(5, result.length());
    }

    @Test
    void toStringBuffer_withNullArray_throwsNullPointerException() {
        // given
        Character[] chars = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuffer(chars, mockConverter));
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideSuccessfulConversions")
    void toStringBuffer_successfulConversions(Object from, String expected, String description) {
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(from, mockConverter);
        // then
        assertNotNull(result, "Resulting StringBuffer should never be null");
        assertEquals(expected, result.toString(), description);
        assertEquals(expected.length(), result.length(), "Length should match expected content");
        // Verify that the converter is never interacted with
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_nullElement_throwsNullPointerException() {
        // given: array contains a null element; unboxing to char triggers NPE
        Character[] chars = new Character[] { 'x', null, 'y' };
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuffer(chars, mockConverter), "Appending a null Character should cause NullPointerException due to unboxing");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideExceptionScenarios")
    void toStringBuffer_invalidInputs(Object from, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> CharacterArrayConversions.toStringBuffer(from, mockConverter), description);
    }

    @Test
    void toStringBuffer_converterIgnored_evenIfItThrows() {
        // given: a Converter implementation that throws when its convert method is called
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("Should not be called");
            }
        };
        Character[] chars = new Character[] { 'p', 'q' };
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(chars, throwingConverter);
        // then
        assertNotNull(result);
        assertEquals("pq", result.toString());
        // No exception should have been thrown, proving the converter was ignored
    }

    @Test
    void toStringBuffer_withNonNullMockConverter_isIgnored() {
        // given
        Character[] chars = new Character[] { 'p', 'q' };
        // when
        StringBuffer result = CharacterArrayConversions.toStringBuffer(chars, mockConverter);
        // then
        assertEquals("pq", result.toString());
        // Verify that the mock converter was never interacted with
        verifyNoInteractions(mockConverter);
    }
}
