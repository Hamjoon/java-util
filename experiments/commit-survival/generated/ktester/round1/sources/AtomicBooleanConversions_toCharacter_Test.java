package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toCharacter_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    /**
     * Prepare the mock objects for each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Initialise the @Mock fields (MockitoExtension does this automatically,
        // but we also want to explicitly define the behavior that is common to all tests).
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    /**
     * Reset mock interactions after each test to avoid cross‑test interference.
     */
    @AfterEach
    void teardownAfterEach() {
        // Reset the mocks so that verification in one test does not affect another.
        reset(mockConverter, mockOptions);
    }

    /**
     * Configures {@link #mockOptions} to return the supplied characters.
     */
    private void configureMockOptions(Character trueChar, Character falseChar) {
        when(mockOptions.trueChar()).thenReturn(trueChar);
        when(mockOptions.falseChar()).thenReturn(falseChar);
    }

    /**
     * Supplies data for tests where true/false map to the same character.
     */
    private static Stream<Arguments> sameCharProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), 'X', "X"), Arguments.of(new AtomicBoolean(false), 'X', "X"));
    }

    /**
     * Supplies data for Unicode true/false characters.
     */
    private static Stream<Arguments> unicodeProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), '✓', "✓"), Arguments.of(new AtomicBoolean(false), '✗', "✗"));
    }

    /**
     * Verify that a true {@link AtomicBoolean} yields the character defined by {@code trueChar()}.
     */
    @Test
    void toCharacter_returnsTrueChar_whenAtomicBooleanIsTrue() {
        // given
        AtomicBoolean atomicTrue = new AtomicBoolean(true);
        when(mockOptions.trueChar()).thenReturn('Y');
        when(mockOptions.falseChar()).thenReturn('N');
        // when
        Character result = AtomicBooleanConversions.toCharacter(atomicTrue, mockConverter);
        // then
        assertEquals('Y', result);
        // ensure trueChar() was consulted
        verify(mockOptions).trueChar();
        verify(mockOptions, never()).falseChar();
    }

    /**
     * Verify that a false {@link AtomicBoolean} yields the character defined by {@code falseChar()}.
     */
    @Test
    void toCharacter_returnsFalseChar_whenAtomicBooleanIsFalse() {
        // given
        AtomicBoolean atomicFalse = new AtomicBoolean(false);
        when(mockOptions.trueChar()).thenReturn('Y');
        when(mockOptions.falseChar()).thenReturn('N');
        // when
        Character result = AtomicBooleanConversions.toCharacter(atomicFalse, mockConverter);
        // then
        assertEquals('N', result);
        // ensure falseChar() was consulted
        verify(mockOptions).falseChar();
        verify(mockOptions, never()).trueChar();
    }

    /**
     * Verify that passing a non‑AtomicBoolean object results in a ClassCastException.
     */
    @Test
    void toCharacter_throwsClassCastException_whenObjectIsNotAtomicBoolean() {
        // given
        Object notAtomic = new Object();
        when(mockOptions.trueChar()).thenReturn('Y');
        when(mockOptions.falseChar()).thenReturn('N');
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toCharacter(notAtomic, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] AtomicBoolean={0}, expected={2}")
    @MethodSource("sameCharProvider")
    void toCharacter_returnsSameCharRegardlessOfValue(AtomicBoolean source, Character trueChar, String expected) {
        configureMockOptions(trueChar, trueChar);
        Character result = AtomicBooleanConversions.toCharacter(source, mockConverter);
        assertEquals(expected.charAt(0), result);
    }

    @ParameterizedTest(name = "[{index}] AtomicBoolean={0}, expected={2}")
    @MethodSource("unicodeProvider")
    void toCharacter_returnsUnicodeChar(AtomicBoolean source, Character trueChar, String expected) {
        // trueChar / falseChar are fixed for this test
        configureMockOptions('✓', '✗');
        Character result = AtomicBooleanConversions.toCharacter(source, mockConverter);
        assertEquals(expected.charAt(0), result);
    }

    @Test
    void toCharacter_doesNotMutateAtomicBoolean_trueCase() {
        AtomicBoolean b = new AtomicBoolean(true);
        configureMockOptions('Y', 'N');
        Character result = AtomicBooleanConversions.toCharacter(b, mockConverter);
        assertEquals('Y', result);
        assertTrue(b.get(), "AtomicBoolean must remain true after conversion");
    }

    @Test
    void toCharacter_doesNotMutateAtomicBoolean_falseCase() {
        AtomicBoolean b = new AtomicBoolean(false);
        configureMockOptions('Y', 'N');
        Character result = AtomicBooleanConversions.toCharacter(b, mockConverter);
        assertEquals('N', result);
        assertFalse(b.get(), "AtomicBoolean must remain false after conversion");
    }

    @Test
    void toCharacter_throwsNullPointerException_whenSourceIsNull() {
        configureMockOptions('Y', 'N');
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(null, mockConverter));
    }

    @Test
    void toCharacter_throwsNullPointerException_whenConverterIsNull() {
        AtomicBoolean source = new AtomicBoolean(true);
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(source, null));
    }

    @Test
    void toCharacter_throwsNullPointerException_whenConverterReturnsNullOptions() {
        AtomicBoolean source = new AtomicBoolean(false);
        // simulate null options
        when(mockConverter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(source, mockConverter));
    }

    @Test
    void toCharacter_throwsNullPointerException_whenTrueCharIsNull() {
        AtomicBoolean source = new AtomicBoolean(true);
        // trueChar returns null
        configureMockOptions(null, 'N');
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(source, mockConverter));
    }

    @Test
    void toCharacter_throwsNullPointerException_whenFalseCharIsNull() {
        AtomicBoolean source = new AtomicBoolean(false);
        // falseChar returns null
        configureMockOptions('Y', null);
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(source, mockConverter));
    }
}
