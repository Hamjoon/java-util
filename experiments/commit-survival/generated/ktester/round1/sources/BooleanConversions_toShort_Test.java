package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toShort_Test {

    /**
     * Mocked Converter – the method under test does not call any of its methods.
     */
    @Mock
    private Converter mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Ensure the mock was not used in any test that expects no interaction.
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, CommonValues.SHORT_ONE), Arguments.of(Boolean.FALSE, CommonValues.SHORT_ZERO));
    }

    @Test
    void toShort_whenBooleanTrue_returnsShortOne() {
        // GIVEN
        Boolean input = Boolean.TRUE;
        // WHEN
        Short result = BooleanConversions.toShort(input, mockConverter);
        // THEN
        assertEquals(CommonValues.SHORT_ONE, result, "Converting Boolean.TRUE should yield CommonValues.SHORT_ONE");
        assertSame(CommonValues.SHORT_ONE, result, "The returned instance must be the exact constant defined in CommonValues.");
    }

    @Test
    void toShort_whenBooleanFalse_returnsShortZero() {
        // GIVEN
        Boolean input = Boolean.FALSE;
        // WHEN
        Short result = BooleanConversions.toShort(input, mockConverter);
        // THEN
        assertEquals(CommonValues.SHORT_ZERO, result, "Converting Boolean.FALSE should yield CommonValues.SHORT_ZERO");
        assertSame(CommonValues.SHORT_ZERO, result, "The returned instance must be the exact constant defined in CommonValues.");
    }

    @Test
    void toShort_whenNullInput_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toShort(input, mockConverter), "Passing null should result in a NullPointerException");
    }

    @Test
    void toShort_whenNonBooleanInput_throwsClassCastException() {
        // GIVEN
        Object input = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toShort(input, mockConverter), "A non‑Boolean input must cause a ClassCastException");
    }

    @Test
    void toShort_withNonNullConverter_true_returnsShortOne() {
        // GIVEN
        // a non‑null stub
        Converter dummyConverter = mock(Converter.class);
        Boolean input = Boolean.TRUE;
        // WHEN
        Short result = BooleanConversions.toShort(input, dummyConverter);
        // THEN
        assertEquals(CommonValues.SHORT_ONE, result, "Even with a non‑null Converter the result must be SHORT_ONE");
        // ensure method truly ignores the converter
        verifyNoInteractions(dummyConverter);
    }

    @Test
    void toShort_withNonNullConverter_false_returnsShortZero() {
        // GIVEN
        // a non‑null stub
        Converter dummyConverter = mock(Converter.class);
        Boolean input = Boolean.FALSE;
        // WHEN
        Short result = BooleanConversions.toShort(input, dummyConverter);
        // THEN
        assertEquals(CommonValues.SHORT_ZERO, result, "Even with a non‑null Converter the result must be SHORT_ZERO");
        verifyNoInteractions(dummyConverter);
    }

    @ParameterizedTest(name = "toShort repeatedly returns {0} for {1}")
    @MethodSource("booleanProvider")
    void toShort_multipleSequentialCalls_returnConsistentConstant(Boolean input, Short expectedConstant) {
        // Perform the call several times to ensure no side‑effects.
        for (int i = 0; i < 5; i++) {
            Short result = BooleanConversions.toShort(input, mockConverter);
            assertEquals(expectedConstant, result, "Call #" + (i + 1) + " should return the expected constant");
            assertSame(expectedConstant, result, "Call #" + (i + 1) + " should return the exact same Short instance");
        }
    }
}
