package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toCharacter_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: Converter#getOptions() returns the mocked options
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        clearInvocations(mockConverter, mockOptions);
    }

    @Test
    void toCharacter_returnsTrueChar_whenBooleanIsTrue() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        when(mockOptions.trueChar()).thenReturn('Y');
        when(mockOptions.falseChar()).thenReturn('N');
        // WHEN
        char result = BooleanConversions.toCharacter(source, mockConverter);
        // THEN
        assertEquals('Y', result, "Expected trueChar() value to be returned for Boolean.TRUE");
        verify(mockOptions, times(1)).trueChar();
        verify(mockOptions, never()).falseChar();
    }

    @Test
    void toCharacter_returnsFalseChar_whenBooleanIsFalse() {
        // GIVEN
        Boolean source = Boolean.FALSE;
        when(mockOptions.trueChar()).thenReturn('Y');
        when(mockOptions.falseChar()).thenReturn('N');
        // WHEN
        char result = BooleanConversions.toCharacter(source, mockConverter);
        // THEN
        assertEquals('N', result, "Expected falseChar() value to be returned for Boolean.FALSE");
        verify(mockOptions, times(1)).falseChar();
        verify(mockOptions, never()).trueChar();
    }

    @Test
    void toCharacter_throwsClassCastException_whenSourceIsNotBoolean() {
        // GIVEN
        Object notABoolean = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toCharacter(notABoolean, mockConverter), "Expected a ClassCastException when the source object is not a Boolean");
    }

    @Test
    void toCharacter_throwsNullPointerException_whenSourceIsNull() {
        // GIVEN
        // No additional stubbing required; the method will attempt to unbox a null Boolean.
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toCharacter(null, mockConverter), "Expected NullPointerException when the source Boolean is null");
        // Ensure that the converter hierarchy is never touched because the NPE occurs earlier.
        verify(mockConverter, never()).getOptions();
        verifyNoInteractions(mockOptions);
    }

    @Test
    void toCharacter_throwsNullPointerException_whenConverterIsNull() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toCharacter(source, null), "Expected NullPointerException when the Converter argument is null");
        // No interaction with the mockConverter because it is not used.
        verifyNoInteractions(mockConverter, mockOptions);
    }
}
