package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Period;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toPeriod_Test {

    /**
     * The Converter instance required by the static method. It is not used inside the method,
     *  but we provide a mock to satisfy the signature without pulling in heavy infrastructure.
     */
    @Mock
    Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us via the @Mock annotation.
        // Ensure the mock is reset before each test to avoid accidental interactions.
        reset(mockConverter);
    }

    private static Stream<Arguments> provideValidPeriodStrings() {
        return Stream.of(// basic components
        Arguments.of("P2Y3M5D", Period.of(2, 3, 5)), // zero length period
        Arguments.of("P0D", Period.of(0, 0, 0)), // negative year
        Arguments.of("P-1Y", Period.of(-1, 0, 0)), // years & months only
        Arguments.of("P1Y2M", Period.of(1, 2, 0)), // negative month with positive days
        Arguments.of("P-2M3D", Period.of(0, -2, 3)), // weeks are converted to days
        Arguments.of("P2W", Period.ofDays(14)), // explicit plus sign
        Arguments.of("P+1Y", Period.of(1, 0, 0)), // explicit zero components
        Arguments.of("P0Y0M0D", Period.of(0, 0, 0)), // very large year value
        Arguments.of("P9999Y", Period.of(9999, 0, 0)));
    }

    private static Stream<Arguments> provideInvalidPeriodStrings() {
        return Stream.of(// time‑based component not supported by Period
        Arguments.of("PT20H", IllegalArgumentException.class), // non‑ISO string
        Arguments.of("invalid", IllegalArgumentException.class), // empty string
        Arguments.of("", IllegalArgumentException.class), // lower‑case designator
        Arguments.of("p1Y2M3D", IllegalArgumentException.class), // extra trailing characters
        Arguments.of("P1Y2M3DXYZ", IllegalArgumentException.class), // leading/trailing whitespace
        Arguments.of("  P1Y2M3D  ", IllegalArgumentException.class));
    }

    @Test
    @DisplayName("toPeriod converts a valid ISO‑8601 period string")
    void toPeriod_ValidString_ReturnsPeriod() {
        // given
        // 2 years, 3 months, 5 days
        String isoPeriod = "P2Y3M5D";
        // when
        Period result = StringConversions.toPeriod(isoPeriod, mockConverter);
        // then
        assertEquals(Period.of(2, 3, 5), result, "The parsed Period should match the ISO‑8601 representation");
        // Verify that the converter is not interacted with (method does not use it)
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toPeriod throws IllegalArgumentException for an unparsable string")
    void toPeriod_InvalidString_ThrowsIllegalArgumentException() {
        // given
        String badPeriod = "not-a-period";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toPeriod(badPeriod, mockConverter));
        assertTrue(ex.getMessage().contains("Unable to parse"), "Exception message should indicate a parsing problem");
        // The original cause should be a DateTimeParseException (or another parsing exception)
        assertNotNull(ex.getCause(), "Cause of the IllegalArgumentException must be present");
        // Verify that the converter is not used.
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toPeriod throws IllegalArgumentException when input is null")
    void toPeriod_NullInput_ThrowsIllegalArgumentException() {
        // given
        Object nullInput = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toPeriod(nullInput, mockConverter));
        assertTrue(ex.getMessage().contains("Unable to parse"), "Exception message should indicate a parsing problem for null input");
        // Verify no interaction with the converter.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] parse \"{0}\" → {1}")
    @MethodSource("provideValidPeriodStrings")
    @DisplayName("toPeriod parses various valid ISO‑8601 period strings")
    void toPeriod_ValidInputs_ReturnsExpectedPeriod(String input, Period expected) {
        Period actual = StringConversions.toPeriod(input, mockConverter);
        assertEquals(expected, actual, () -> "Period parsed from \"" + input + "\" should be " + expected);
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] input \"{0}\" → throws {1}")
    @MethodSource("provideInvalidPeriodStrings")
    @DisplayName("toPeriod throws IllegalArgumentException for malformed period strings")
    void toPeriod_InvalidInputs_ThrowsIllegalArgumentException(String input, Class<? extends Exception> expectedException) {
        Exception ex = assertThrows(expectedException, () -> StringConversions.toPeriod(input, mockConverter));
        // All illegal‑format cases are wrapped in IllegalArgumentException by the method.
        assertTrue(ex instanceof IllegalArgumentException, "Method should wrap parsing problems in IllegalArgumentException");
        assertNotNull(ex.getCause(), "Cause of the IllegalArgumentException must be present");
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toPeriod throws ClassCastException when source is not a String")
    void toPeriod_NonStringInput_ThrowsClassCastException() {
        Integer notAString = 12345;
        assertThrows(ClassCastException.class, () -> StringConversions.toPeriod(notAString, mockConverter), "Passing a non‑String should result in a ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toPeriod wraps NullPointerException from Period.parse into IllegalArgumentException")
    void toPeriod_NullString_ThrowsIllegalArgumentExceptionWithNPECause() {
        // The cast to String yields null, which causes Period.parse(null) → NullPointerException.
        Object nullString = null;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toPeriod(nullString, mockConverter));
        assertNotNull(ex.getCause(), "Cause should be present");
        assertTrue(ex.getCause() instanceof NullPointerException, "Underlying cause must be NullPointerException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toPeriod ignores a non‑null Converter argument")
    void toPeriod_NonNullConverter_Ignored() {
        // The method does not use the converter; we pass a real mock to prove it is ignored.
        String periodStr = "P1Y2M3D";
        Period expected = Period.of(1, 2, 3);
        Period actual = StringConversions.toPeriod(periodStr, mockConverter);
        assertEquals(expected, actual);
        // No interaction with the converter is expected.
        verifyNoInteractions(mockConverter);
    }
}
