package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toDuration_Test {

    /**
     * The Converter is not used by {@code StringConversions.toDuration}, but the method signature requires it.
     */
    @Mock
    Converter // Mockito will provide a mock instance
    converter;

    private static Stream<Arguments> provideValidIsoDurations() {
        return Stream.of(Arguments.of("Fractional seconds", "PT20.345S", Duration.ofSeconds(20, 345_000_000)), Arguments.of("Minutes only", "PT15M", Duration.ofMinutes(15)), Arguments.of("Days, hours and minutes", "P2DT3H4M", Duration.ofDays(2).plusHours(3).plusMinutes(4)), Arguments.of("Zero duration", "PT0S", Duration.ZERO), Arguments.of("One nanosecond", "PT0.000000001S", Duration.ofNanos(1)), Arguments.of("Negative minutes", "-PT5M", Duration.ofMinutes(-5)), Arguments.of("Negative days and hours", "-P2DT3H", Duration.ofSeconds(-(2L * 86400 + 3L * 3600))), Arguments.of("Week‑based 1W", "P1W", Duration.ofDays(7)), Arguments.of("Week‑based 2W", "P2W", Duration.ofDays(14)), Arguments.of("Very large hours", "PT1000000H", Duration.ofHours(1_000_000)), Arguments.of("Very large seconds", "PT1000000000S", Duration.ofSeconds(1_000_000_000)), Arguments.of("Non‑null converter ignored", "PT10S", Duration.ofSeconds(10)));
    }

    private static Stream<Arguments> provideInvalidDurations() {
        return Stream.of(Arguments.of("Non‑ISO text", "20 seconds", DateTimeParseException.class), Arguments.of("Incorrect negative day designator", "P-1D", DateTimeParseException.class), Arguments.of("Whitespace not tolerated", " PT15M ", DateTimeParseException.class), Arguments.of("Empty string", "", DateTimeParseException.class), Arguments.of("Month designator not supported", "P1M", DateTimeParseException.class), Arguments.of("Leading plus sign", "+PT5M", DateTimeParseException.class), Arguments.of("Overflow causing ArithmeticException", "PT9223372036854775808H", ArithmeticException.class), Arguments.of("Incomplete designator", "P", DateTimeParseException.class));
    }

    private static Stream<Arguments> provideNonStringInputs() {
        return Stream.of(Arguments.of("Integer input", 12345), Arguments.of("Boolean input", true));
    }

    @Test
    void toDuration_ValidIsoString_ReturnsExpectedDuration() {
        // given
        // 15 minutes, 30.5 seconds
        String iso = "PT15M30.5S";
        Duration expected = Duration.parse(iso);
        // when
        Duration actual = StringConversions.toDuration(iso, converter);
        // then
        assertEquals(expected, actual, "The parsed Duration should match the expected value");
    }

    @Test
    void toDuration_InvalidString_ThrowsIllegalArgumentException() {
        // given
        String bad = "not-a-duration";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(bad, converter), "Parsing an invalid duration string must throw IllegalArgumentException");
        // The cause should be the original exception from Duration.parse
        assertNotNull(ex.getCause(), "The thrown exception should have a cause");
        assertTrue(ex.getCause() instanceof Exception, "The cause should be an exception thrown by Duration.parse");
    }

    @Test
    void toDuration_NullInput_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(null, converter), "Null input must be wrapped in IllegalArgumentException");
        assertNotNull(ex.getCause(), "Cause should be present");
        assertTrue(ex.getCause() instanceof NullPointerException, "Cause should be NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  Non‑String input handling
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideNonStringInputs")
    void toDuration_NonStringInput_ThrowsIllegalArgumentException(String description, Object input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(input, converter), description);
        assertNotNull(ex.getCause(), "Cause should be present");
        assertTrue(ex.getCause() instanceof ClassCastException, "Cause should be ClassCastException");
    }
}
