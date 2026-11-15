package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toOffsetTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock converter to return a known ZoneId (UTC) via its options.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneOffset.UTC);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        Mockito.reset(mockConverter, mockOptions);
    }

    /**
     * Provides normal conversion data:
     * <ul>
     *   <li>seconds as whole number</li>
     *   <li>seconds with fractional part</li>
     *   <li>maximum allowed value (just before next day)</li>
     *   <li>fraction smaller than one nanosecond – should be truncated to zero</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> successCases() {
        return Stream.of(// 0 seconds → 00:00Z
        org.junit.jupiter.params.provider.Arguments.of(BigDecimal.valueOf(0L), OffsetTime.parse("00:00Z"), "Zero seconds"), // 3600 seconds → 01:00Z
        org.junit.jupiter.params.provider.Arguments.of(BigDecimal.valueOf(3600L), OffsetTime.parse("01:00Z"), "Exact whole hour"), // 3661.123456789 seconds → 01:01:01.123456789Z
        org.junit.jupiter.params.provider.Arguments.of(new BigDecimal("3661.123456789"), OffsetTime.parse("01:01:01.123456789Z"), "Mixed whole and fractional seconds"), // 23:59:59.999999999 → maximum allowed value
        org.junit.jupiter.params.provider.Arguments.of(new BigDecimal("86399.999999999"), OffsetTime.parse("23:59:59.999999999Z"), "Maximum allowed value"), // 0.0000000005 seconds → less than one nanosecond, truncates to 0 ns
        org.junit.jupiter.params.provider.Arguments.of(new BigDecimal("0.0000000005"), OffsetTime.parse("00:00Z"), "Fraction smaller than one nanosecond"));
    }

    /**
     * Supplies inputs that must cause an {@link IllegalArgumentException}.
     * The third argument is a short description used only for test naming.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> errorCases() {
        return Stream.of(// negative seconds
        org.junit.jupiter.params.provider.Arguments.of(new BigDecimal("-1"), // normal mock converter
        "mock", "must be >= 0"), // exactly 24 h – out of range
        org.junit.jupiter.params.provider.Arguments.of(BigDecimal.valueOf(86_400L), "mock", "must be >= 0 && <= 86399"), // integer part larger than Long.MAX_VALUE (overflow)
        org.junit.jupiter.params.provider.Arguments.of(// Long.MAX_VALUE + 1
        new BigDecimal("9223372036854775808"), "mock", "must be >= 0 && <= 86399"), // null input value
        org.junit.jupiter.params.provider.Arguments.of(null, "mock", "must be >= 0"), // wrong type (Integer instead of BigDecimal)
        org.junit.jupiter.params.provider.Arguments.of(Integer.valueOf(12345), "mock", "must be >= 0"), // null converter
        org.junit.jupiter.params.provider.Arguments.of(BigDecimal.ZERO, "nullConverter", "must be >= 0"), // converter returns null options
        org.junit.jupiter.params.provider.Arguments.of(BigDecimal.ZERO, "nullOptions", "must be >= 0"), // options return null ZoneId
        org.junit.jupiter.params.provider.Arguments.of(BigDecimal.ZERO, "nullZoneId", "must be >= 0"));
    }

    @Test
    void toOffsetTime_withWholeSeconds_returnsCorrectOffsetTime() {
        // given
        // epoch start
        BigDecimal seconds = BigDecimal.valueOf(0L);
        // when
        OffsetTime result = BigDecimalConversions.toOffsetTime(seconds, mockConverter);
        // then
        OffsetTime expected = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetTime_withFractionalSeconds_returnsCorrectOffsetTime() {
        // given
        // 12 345.678901234 seconds → 3 h 25 m 45 s + 678 901 234 ns
        BigDecimal seconds = new BigDecimal("12345.678901234");
        // when
        OffsetTime result = BigDecimalConversions.toOffsetTime(seconds, mockConverter);
        // then
        OffsetTime expected = OffsetTime.of(3, 25, 45, 678_901_234, ZoneOffset.UTC);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetTime_negativeValue_throwsIllegalArgumentException() {
        // given
        BigDecimal seconds = new BigDecimal("-1");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toOffsetTime(seconds, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0"));
    }

    @Test
    void toOffsetTime_exceedsOneDay_throwsIllegalArgumentException() {
        // given
        // 86 400 seconds = exactly 24 h, which is outside the allowed range (max 86 399.999999999)
        BigDecimal seconds = BigDecimal.valueOf(86_400L);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toOffsetTime(seconds, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successCases")
    void toOffsetTime_successfulConversions(BigDecimal input, OffsetTime expected, String description) {
        // when
        OffsetTime result = BigDecimalConversions.toOffsetTime(input, mockConverter);
        // then
        assertEquals(expected, result, () -> "Failed for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("errorCases")
    void toOffsetTime_errorScenarios(Object input, String converterScenario, String expectedMessageSnippet) {
        // --------------------------------------------------------------
        // Arrange – configure the converter according to the scenario
        // --------------------------------------------------------------
        Converter converterToUse;
        switch(converterScenario) {
            case "nullConverter":
                converterToUse = null;
                break;
            case "nullOptions":
                when(mockConverter.getOptions()).thenReturn(null);
                converterToUse = mockConverter;
                break;
            case "nullZoneId":
                when(mockOptions.getZoneId()).thenReturn(null);
                converterToUse = mockConverter;
                break;
            default:
                // "mock" – normal behaviour already set in @BeforeEach
                converterToUse = mockConverter;
                break;
        }
        // --------------------------------------------------------------
        // Act & Assert – the method must throw IllegalArgumentException
        // --------------------------------------------------------------
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toOffsetTime(input, converterToUse), () -> "Expected IllegalArgumentException for input: " + input + " scenario: " + converterScenario);
        // Verify that the message contains a hint about the range / cause.
        assertTrue(ex.getMessage().contains(expectedMessageSnippet), () -> "Exception message [" + ex.getMessage() + "] does not contain expected snippet [" + expectedMessageSnippet + "]");
    }
}
