package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toAtomicBoolean_Test {

    @Mock
    private Converter // injected by MockitoExtension
    mockConverter;

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> trueFalseProvider() {
        return java.util.stream.Stream.of(org.junit.jupiter.params.provider.Arguments.of(Boolean.TRUE, true), org.junit.jupiter.params.provider.Arguments.of(Boolean.FALSE, false));
    }

    @Test
    void toAtomicBoolean_trueValue_returnsAtomicTrue() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result, "Resulting AtomicBoolean should not be null");
        assertTrue(result.get(), "AtomicBoolean should contain true");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicBoolean_falseValue_returnsAtomicFalse() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result, "Resulting AtomicBoolean should not be null");
        assertFalse(result.get(), "AtomicBoolean should contain false");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicBoolean_nullSource_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toAtomicBoolean(source, mockConverter), "Passing null should result in NullPointerException due to unboxing");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicBoolean_stringSource_throwsClassCastException() {
        // given
        Object source = "some string";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicBoolean(source, mockConverter), "A non‑Boolean source must cause a ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicBoolean_trueWithConverterThatThrows_isIgnored() {
        // given: a Converter mock that throws on any method call
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter should not be called");
        });
        Boolean source = Boolean.TRUE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, throwingConverter);
        // then
        assertNotNull(result);
        assertTrue(result.get(), "Conversion must succeed even if converter would throw");
        // No method on the converter should have been invoked; the exception would have bubbled up otherwise.
        verifyNoInteractions(throwingConverter);
    }

    @Test
    void toAtomicBoolean_falseWithConverterThatThrows_isIgnored() {
        // given: a Converter mock that throws on any method call
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter should not be called");
        });
        Boolean source = Boolean.FALSE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, throwingConverter);
        // then
        assertNotNull(result);
        assertFalse(result.get(), "Conversion must succeed even if converter would throw");
        verifyNoInteractions(throwingConverter);
    }

    @Test
    void toAtomicBoolean_newBooleanInstance_isHandled() {
        // given a freshly created Boolean object (not the cached Boolean.TRUE/FALSE)
        Boolean source = new Boolean(true);
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should reflect the true value of the new Boolean instance");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicBoolean_falseWithRecordingConverter_isIgnored() {
        // given: a regular Mockito mock that records interactions
        Converter recordingConverter = mock(Converter.class);
        Boolean source = Boolean.FALSE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, recordingConverter);
        // then
        assertNotNull(result);
        assertFalse(result.get(), "Conversion should succeed and ignore the converter");
        // Verify that the converter was never used.
        verifyNoInteractions(recordingConverter);
    }

    @Test
    void toAtomicBoolean_trueWithRecordingConverter_isIgnored() {
        // given: a regular Mockito mock that records interactions
        Converter recordingConverter = mock(Converter.class);
        Boolean source = Boolean.TRUE;
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, recordingConverter);
        // then
        assertNotNull(result);
        assertTrue(result.get(), "Conversion should succeed and ignore the converter");
        verifyNoInteractions(recordingConverter);
    }

    @Test
    void toAtomicBoolean_twoCallsReturnDistinctInstances() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        AtomicBoolean first = BooleanConversions.toAtomicBoolean(source, mockConverter);
        AtomicBoolean second = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first.get());
        assertTrue(second.get());
        assertNotSame(first, second, "Each invocation must return a new AtomicBoolean instance");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "toAtomicBoolean({0}) => {1}")
    @MethodSource("trueFalseProvider")
    void toAtomicBoolean_parameterized_trueFalse(Boolean source, boolean expected) {
        // when
        AtomicBoolean result = BooleanConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(expected, result.get());
        verifyNoInteractions(mockConverter);
    }
}
