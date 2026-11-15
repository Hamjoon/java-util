package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toCharacter_Test {

    private static final char TRUE_CHAR = 'Y';

    private static final char FALSE_CHAR = 'N';

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock Converter to return the mocked options.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // Define the characters that should be returned for true/false.
        when(mockOptions.trueChar()).thenReturn(TRUE_CHAR);
        when(mockOptions.falseChar()).thenReturn(FALSE_CHAR);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid interaction leakage.
        reset(mockConverter, mockOptions);
    }

    @Test
    void toCharacter_ReturnsTrueChar_WhenAtomicBooleanIsTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Character result = AtomicBooleanConversions.toCharacter(source, mockConverter);
        // then
        assertEquals(TRUE_CHAR, result);
    }

    @Test
    void toCharacter_ReturnsFalseChar_WhenAtomicBooleanIsFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Character result = AtomicBooleanConversions.toCharacter(source, mockConverter);
        // then
        assertEquals(FALSE_CHAR, result);
    }

    @Test
    void toCharacter_ThrowsClassCastException_WhenSourceIsNotAtomicBoolean() {
        // given
        Object notAtomic = Integer.valueOf(5);
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toCharacter(notAtomic, mockConverter));
    }

    @Test
    void toCharacter_ThrowsNullPointerException_WhenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(nullSource, mockConverter));
    }

    @Test
    void toCharacter_ThrowsNullPointerException_WhenConverterIsNull() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        Converter nullConverter = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(source, nullConverter));
    }

    @Test
    void toCharacter_ThrowsNullPointerException_WhenOptionsIsNull() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // override default
        when(mockConverter.getOptions()).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(source, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] AtomicBoolean={0} => ''{1}''")
    @CsvSource({ "true, Z", "false, Z" })
    void toCharacter_ReturnsSameCharRegardlessOfValue(boolean atomicValue, char expected) {
        // given
        AtomicBoolean source = new AtomicBoolean(atomicValue);
        when(mockOptions.trueChar()).thenReturn('Z');
        when(mockOptions.falseChar()).thenReturn('Z');
        // when
        Character result = AtomicBooleanConversions.toCharacter(source, mockConverter);
        // then
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] AtomicBoolean={0} => ''{1}''")
    @CsvSource({ // Euro sign
    "true, \u20AC", // Chinese character '中'
    "false, \u4E2D" })
    void toCharacter_HandlesUnicodeCharacters(boolean atomicValue, char expected) {
        // given
        AtomicBoolean source = new AtomicBoolean(atomicValue);
        when(mockOptions.trueChar()).thenReturn('\u20AC');
        when(mockOptions.falseChar()).thenReturn('\u4E2D');
        // when
        Character result = AtomicBooleanConversions.toCharacter(source, mockConverter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void toCharacter_PropagatesException_FromGetOptions() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        when(mockConverter.getOptions()).thenThrow(new IllegalStateException("options broken"));
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> AtomicBooleanConversions.toCharacter(source, mockConverter));
        assertEquals("options broken", ex.getMessage());
    }

    @Test
    void toCharacter_PropagatesException_FromTrueChar() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        when(mockOptions.trueChar()).thenThrow(new NullPointerException("trueChar NPE"));
        // when / then
        NullPointerException ex = assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toCharacter(source, mockConverter));
        assertEquals("trueChar NPE", ex.getMessage());
    }

    @Test
    void toCharacter_PropagatesException_FromFalseChar() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        when(mockOptions.falseChar()).thenThrow(new IllegalArgumentException("falseChar IAE"));
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> AtomicBooleanConversions.toCharacter(source, mockConverter));
        assertEquals("falseChar IAE", ex.getMessage());
    }
}
