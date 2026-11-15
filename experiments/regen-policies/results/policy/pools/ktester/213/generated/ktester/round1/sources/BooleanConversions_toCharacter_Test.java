package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toCharacter_Test {

    @Mock
    private Converter // mock of the Converter used in the call
    mockConverter;

    @Mock
    private ConverterOptions // mock of the options returned by the converter
    mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Every test expects the same options to be returned from the converter.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // Define the characters that represent true / false for the purpose of the test.
        when(mockOptions.trueChar()).thenReturn('T');
        when(mockOptions.falseChar()).thenReturn('F');
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions to keep each test independent.
        Mockito.reset(mockConverter, mockOptions);
    }

    @Test
    void toCharacter_returnsTrueChar_whenBooleanTrue() {
        // GIVEN
        Boolean from = Boolean.TRUE;
        // WHEN
        char result = BooleanConversions.toCharacter(from, mockConverter);
        // THEN
        assertEquals('T', result, "Expected trueChar ('T') for Boolean.TRUE");
        // Verify that the correct option methods were consulted.
        verify(mockConverter).getOptions();
        verify(mockOptions).trueChar();
        verify(mockOptions, never()).falseChar();
    }

    @Test
    void toCharacter_returnsFalseChar_whenBooleanFalse() {
        // GIVEN
        Boolean from = Boolean.FALSE;
        // WHEN
        char result = BooleanConversions.toCharacter(from, mockConverter);
        // THEN
        assertEquals('F', result, "Expected falseChar ('F') for Boolean.FALSE");
        verify(mockConverter).getOptions();
        verify(mockOptions).falseChar();
        verify(mockOptions, never()).trueChar();
    }

    @Test
    void toCharacter_throwsNullPointerException_whenFromIsNull() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toCharacter(from, mockConverter), "Null source should cause NPE due to cast to Boolean");
    }

    @Test
    void toCharacter_throwsClassCastException_whenFromIsNotBoolean() {
        // GIVEN
        Object from = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toCharacter(from, mockConverter), "Non‑Boolean source should cause ClassCastException");
    }

    @Test
    void toCharacter_returnsConfiguredTrueChar_whenUsingCustomConverter() {
        // GIVEN
        when(mockOptions.trueChar()).thenReturn('Y');
        when(mockOptions.falseChar()).thenReturn('N');
        Boolean from = Boolean.TRUE;
        // WHEN
        char result = BooleanConversions.toCharacter(from, mockConverter);
        // THEN
        assertEquals('Y', result, "Expected custom trueChar ('Y') for Boolean.TRUE");
    }

    @Test
    void toCharacter_returnsConfiguredFalseChar_whenUsingCustomConverter() {
        // GIVEN
        when(mockOptions.trueChar()).thenReturn('Y');
        when(mockOptions.falseChar()).thenReturn('N');
        Boolean from = Boolean.FALSE;
        // WHEN
        char result = BooleanConversions.toCharacter(from, mockConverter);
        // THEN
        assertEquals('N', result, "Expected custom falseChar ('N') for Boolean.FALSE");
    }

    @Test
    void toCharacter_throwsNullPointerException_whenConverterIsNull() {
        // GIVEN
        Boolean from = Boolean.TRUE;
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toCharacter(from, nullConverter), "Calling with a null Converter should NPE on getOptions()");
    }

    @Test
    void toCharacter_throwsNullPointerException_whenOptionsAreNull() {
        // GIVEN
        when(mockConverter.getOptions()).thenReturn(null);
        Boolean from = Boolean.FALSE;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toCharacter(from, mockConverter), "Null ConverterOptions should cause NPE when falseChar() is invoked");
    }

    @Test
    void toCharacter_returnsWhitespace_whenTrueCharIsSpace() {
        // GIVEN
        when(mockOptions.trueChar()).thenReturn(' ');
        when(mockOptions.falseChar()).thenReturn('X');
        Boolean from = Boolean.TRUE;
        // WHEN
        char result = BooleanConversions.toCharacter(from, mockConverter);
        // THEN
        assertEquals(' ', result, "Expected space character for trueChar");
    }

    @Test
    void toCharacter_returnsFalseChar_whenTrueCharIsSpace_andBooleanFalse() {
        // GIVEN
        when(mockOptions.trueChar()).thenReturn(' ');
        when(mockOptions.falseChar()).thenReturn('X');
        Boolean from = Boolean.FALSE;
        // WHEN
        char result = BooleanConversions.toCharacter(from, mockConverter);
        // THEN
        assertEquals('X', result, "Expected falseChar ('X') when Boolean is false");
    }

    @Test
    void toCharacter_returnsSameChar_forBothTrueAndFalse() {
        // GIVEN
        when(mockOptions.trueChar()).thenReturn('*');
        when(mockOptions.falseChar()).thenReturn('*');
        // WHEN true
        char trueResult = BooleanConversions.toCharacter(Boolean.TRUE, mockConverter);
        // WHEN false
        char falseResult = BooleanConversions.toCharacter(Boolean.FALSE, mockConverter);
        // THEN
        assertEquals('*', trueResult, "Expected '*' for Boolean.TRUE");
        assertEquals('*', falseResult, "Expected '*' for Boolean.FALSE");
    }

    @Test
    void toCharacter_propagatesException_fromGetOptions() {
        // GIVEN
        when(mockConverter.getOptions()).thenThrow(new IllegalArgumentException("boom"));
        Boolean from = Boolean.TRUE;
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BooleanConversions.toCharacter(from, mockConverter), "Exception from getOptions() should be propagated");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toCharacter_propagatesException_fromTrueChar() {
        // GIVEN
        when(mockOptions.trueChar()).thenThrow(new RuntimeException("trueChar failure"));
        Boolean from = Boolean.TRUE;
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BooleanConversions.toCharacter(from, mockConverter), "Exception from trueChar() should be propagated");
        assertEquals("trueChar failure", ex.getMessage());
    }

    @Test
    void toCharacter_propagatesException_fromFalseChar() {
        // GIVEN
        when(mockOptions.falseChar()).thenThrow(new RuntimeException("falseChar failure"));
        Boolean from = Boolean.FALSE;
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BooleanConversions.toCharacter(from, mockConverter), "Exception from falseChar() should be propagated");
        assertEquals("falseChar failure", ex.getMessage());
    }
}
