package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterArrayConversions_toString_Test {

    @Mock
    Converter // mocked dependency – not used by the method
    mockConverter;

    /**
     * Initialise mocks before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Verify that the mock has no unexpected interactions after each test.
     */
    @AfterEach
    void teardownAfterEach() {
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Supplies the arguments for {@link #toString_validConversions}.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of("Convert a non‑empty Character[] to its string representation.", new Character[] { 'a', 'b', 'c' }, "abc"), Arguments.of("Convert an empty Character[]; expected result is an empty string.", new Character[0], ""), Arguments.of("Array contains a null element – StringBuilder.append(null) yields the literal \"null\".", new Character[] { 'a', null, 'c' }, "anullc"));
    }

    /**
     * Supplies the arguments for {@link #toString_invalidInputs}.
     */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of("Passing null as the source object should cause NullPointerException.", null, NullPointerException.class), Arguments.of("Passing an object that is not a Character[] should trigger ClassCastException.", new String[] { "a", "b" }, ClassCastException.class));
    }

    @Test
    void toString_convertsCharacterArrayToString() {
        // given
        Character[] source = new Character[] { 'J', 'a', 'v', 'a' };
        // when
        String result = CharacterArrayConversions.toString(source, mockConverter);
        // then
        assertEquals("Java", result);
        // the converter is not used – ensure no interaction
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toString_emptyArrayReturnsEmptyString() {
        // given
        Character[] source = new Character[0];
        // when
        String result = CharacterArrayConversions.toString(source, mockConverter);
        // then
        assertEquals("", result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toString_arrayContainingNullThrowsNullPointerException() {
        // given
        Character[] source = new Character[] { 'A', null, 'Z' };
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toString(source, mockConverter));
        // even though an exception is thrown, the converter should still be untouched
        verifyNoInteractions(mockConverter);
    }

    /**
     * Parameterized test covering all valid conversion scenarios described in the
     * JSON test data under the "Valid conversions" group.
     *
     * The expected result is a {@link String} produced by concatenating the
     * characters (including the literal "null" when a {@code null} element is present).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void toString_validConversions(String description, Character[] source, String expected) {
        // when
        String result = CharacterArrayConversions.toString(source, mockConverter);
        // then
        assertEquals(expected, result, description);
        verifyNoInteractions(mockConverter);
    }

    /**
     * Parameterized test for inputs that should cause an exception.
     *
     * The JSON test data under the "Invalid inputs" group defines two cases:
     *   1. {@code from == null} → {@link NullPointerException}
     *   2. {@code from} not a {@code Character[]} → {@link ClassCastException}
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidInputProvider")
    void toString_invalidInputs(String description, Object source, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> CharacterArrayConversions.toString(source, mockConverter), description);
        verifyNoInteractions(mockConverter);
    }
}
