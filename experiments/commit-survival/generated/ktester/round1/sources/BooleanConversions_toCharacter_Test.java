package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toCharacter_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    /**
     * Common setup for all tests – configure the mock converter to return the
     * mocked options instance.
     */
    @BeforeEach
    void setupBeforeEach() {
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    // No resources to clean up for this static utility class.
    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid interaction bleed‑over between tests.
        clearInvocations(mockConverter, mockOptions);
    }

    private static Stream<Arguments> identicalCharProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, '*'), Arguments.of(Boolean.FALSE, '*'));
    }

    private static Stream<Arguments> unicodeCharProvider() {
        return Stream.of(// true -> null character, false -> copyright
        Arguments.of(Boolean.TRUE, '\u0000', '\u00A9', '\u0000'), Arguments.of(Boolean.FALSE, '\u0000', '\u00A9', '\u00A9'), // true -> newline, false -> carriage return
        Arguments.of(Boolean.TRUE, '\n', '\r', '\n'), Arguments.of(Boolean.FALSE, '\n', '\r', '\r'));
    }

    /**
     * Verify that a {@code Boolean.TRUE} value is converted to the character
     * defined by {@link ConverterOptions#trueChar()}.
     */
    @Test
    void toCharacter_returnsTrueChar_whenBooleanTrue() {
        // given
        char expected = 'Y';
        when(mockOptions.trueChar()).thenReturn(expected);
        // not used in this path
        when(mockOptions.falseChar()).thenReturn('N');
        // when
        char actual = BooleanConversions.toCharacter(Boolean.TRUE, mockConverter);
        // then
        assertEquals(expected, actual, "True Boolean should map to trueChar");
        verify(mockOptions, times(1)).trueChar();
        verify(mockOptions, never()).falseChar();
    }

    /**
     * Verify that a {@code Boolean.FALSE} value is converted to the character
     * defined by {@link ConverterOptions#falseChar()}.
     */
    @Test
    void toCharacter_returnsFalseChar_whenBooleanFalse() {
        // given
        char expected = 'N';
        when(mockOptions.falseChar()).thenReturn(expected);
        // not used in this path
        when(mockOptions.trueChar()).thenReturn('Y');
        // when
        char actual = BooleanConversions.toCharacter(Boolean.FALSE, mockConverter);
        // then
        assertEquals(expected, actual, "False Boolean should map to falseChar");
        verify(mockOptions, times(1)).falseChar();
        verify(mockOptions, never()).trueChar();
    }

    /**
     * The method casts the supplied object to {@code Boolean}. Supplying a
     * non‑Boolean should result in a {@link ClassCastException}.
     */
    @Test
    void toCharacter_throwsClassCastException_whenNonBoolean() {
        // given
        Object notABoolean = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toCharacter(notABoolean, mockConverter));
    }

    /**
     * Passing a {@code null} source object should cause a {@link NullPointerException}
     * when the method attempts to cast it to {@code Boolean}.
     */
    @Test
    void toCharacter_throwsNullPointerException_whenSourceIsNull() {
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toCharacter(null, mockConverter));
    }

    /**
     * If the {@code Converter} argument itself is {@code null}, a
     * {@link NullPointerException} is expected when the method tries to call
     * {@code converter.getOptions()}.
     */
    @Test
    void toCharacter_throwsNullPointerException_whenConverterIsNull() {
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toCharacter(Boolean.TRUE, null));
    }

    /**
     * When {@code converter.getOptions()} returns {@code null}, the method will
     * attempt to invoke {@code trueChar()} / {@code falseChar()} on a {@code null}
     * reference, resulting in a {@link NullPointerException}.
     */
    @Test
    void toCharacter_throwsNullPointerException_whenOptionsAreNull() {
        // given
        when(mockConverter.getOptions()).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toCharacter(Boolean.FALSE, mockConverter));
    }

    /**
     * Parameterized test for scenarios where both trueChar and falseChar are the
     * same character. The method should return that character irrespective of
     * the Boolean value.
     */
    @ParameterizedTest(name = "[{index}] from={0} => expected=''{1}''")
    @MethodSource("identicalCharProvider")
    void toCharacter_returnsIdenticalChar_whenTrueAndFalseCharsAreSame(Boolean from, char expected) {
        // given
        when(mockOptions.trueChar()).thenReturn('*');
        when(mockOptions.falseChar()).thenReturn('*');
        // when
        char actual = BooleanConversions.toCharacter(from, mockConverter);
        // then
        assertEquals(expected, actual);
    }

    /**
     * Parameterized test covering various Unicode characters (null char, copyright,
     * newline, carriage‑return). The stubbed options return the exact Unicode
     * values, and the conversion result is verified.
     */
    @ParameterizedTest(name = "[{index}] from={0}, trueChar=''{1}'', falseChar=''{2}'' => expected=''{3}''")
    @MethodSource("unicodeCharProvider")
    void toCharacter_handlesSpecialUnicodeCharacters(Boolean from, char trueChar, char falseChar, char expected) {
        // given
        when(mockOptions.trueChar()).thenReturn(trueChar);
        when(mockOptions.falseChar()).thenReturn(falseChar);
        // when
        char actual = BooleanConversions.toCharacter(from, mockConverter);
        // then
        assertEquals(expected, actual);
    }

    /**
     * When {@code Converter.getOptions()} throws a {@link RuntimeException},
     * the exception should propagate unchanged.
     */
    @Test
    void toCharacter_propagatesException_whenGetOptionsThrows() {
        // given
        when(mockConverter.getOptions()).thenThrow(new RuntimeException("boom"));
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BooleanConversions.toCharacter(Boolean.TRUE, mockConverter));
        assertEquals("boom", ex.getMessage());
    }

    /**
     * If {@code ConverterOptions.trueChar()} throws an {@link IllegalStateException},
     * the method must propagate that exception for a {@code true} source.
     */
    @Test
    void toCharacter_propagatesException_whenTrueCharThrows() {
        // given
        when(mockOptions.trueChar()).thenThrow(new IllegalStateException("trueChar failure"));
        // not used
        when(mockOptions.falseChar()).thenReturn('X');
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> BooleanConversions.toCharacter(Boolean.TRUE, mockConverter));
        assertEquals("trueChar failure", ex.getMessage());
    }

    /**
     * If {@code ConverterOptions.falseChar()} throws an {@link IllegalArgumentException},
     * the method must propagate that exception for a {@code false} source.
     */
    @Test
    void toCharacter_propagatesException_whenFalseCharThrows() {
        // given
        when(mockOptions.falseChar()).thenThrow(new IllegalArgumentException("falseChar failure"));
        // not used
        when(mockOptions.trueChar()).thenReturn('Y');
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BooleanConversions.toCharacter(Boolean.FALSE, mockConverter));
        assertEquals("falseChar failure", ex.getMessage());
    }
}
