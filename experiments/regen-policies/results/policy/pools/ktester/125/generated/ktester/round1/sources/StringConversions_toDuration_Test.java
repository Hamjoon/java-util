package com.cedarsoftware.util.convert;

import java.time.Duration;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class StringConversions_toDuration_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is not used by toDuration, but we provide a real instance
        // to satisfy the method signature.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> successfulParsingProvider() {
        return Stream.of(// 15 minutes
        Arguments.of("PT15M", Duration.ofMinutes(15)), // fractional seconds (20.345 seconds)
        Arguments.of("PT20.345S", Duration.ofSeconds(20).plusNanos(345_000_000)), // zero duration
        Arguments.of("PT0S", Duration.ZERO));
    }

    private static Stream<Arguments> malformedProvider() {
        return Stream.of(Arguments.of("15 minutes"), Arguments.of("P"), Arguments.of("PT5X"));
    }

    private static Stream<Arguments> negativeProvider() {
        return Stream.of(Arguments.of("-PT5S", Duration.ofSeconds(-5)), Arguments.of("-PT0.5S", Duration.ofMillis(-500)), // "-PT0S" is treated as zero
        Arguments.of("-PT0S", Duration.ZERO));
    }

    private static Stream<Arguments> whitespaceProvider() {
        return Stream.of(Arguments.of(" PT15M ", Duration.ofMinutes(15)), Arguments.of("\tPT20S\n", Duration.ofSeconds(20)));
    }

    private static Stream<Arguments> largeValuesProvider() {
        return Stream.of(// Very large hour count – fits within long seconds
        Arguments.of("PT9999999999H", Duration.ofHours(9_999_999_999L)), // Smallest fraction (one nanosecond)
        Arguments.of("PT0.000000001S", Duration.ofNanos(1)));
    }

    private static Stream<Arguments> emptyOrBlankProvider() {
        return Stream.of(Arguments.of(""), Arguments.of("   "));
    }

    private static Stream<Arguments> invalidSignProvider() {
        return Stream.of(Arguments.of("PT-5S"), Arguments.of("P5DT-3H"));
    }

    private static Stream<Arguments> unsupportedPeriodProvider() {
        return Stream.of(Arguments.of("P1Y2M3DT4H"), Arguments.of("P2M"));
    }

    private static Stream<Arguments> overflowProvider() {
        return Stream.of(// One more than Long.MAX_VALUE seconds
        Arguments.of("PT9223372036854775808S"), // Negative overflow
        Arguments.of("PT-9223372036854775809S"));
    }

    @Test
    void toDuration_parsesValidIso8601String() {
        // given
        // 15 minutes, 30.5 seconds
        String iso = "PT15M30.5S";
        // when
        Duration result = StringConversions.toDuration(iso, converter);
        // then
        Duration expected = Duration.parse(iso);
        assertEquals(expected, result);
    }

    @Test
    void toDuration_parsesVariousValidFormats() {
        // given
        String[] valid = { // seconds with fraction
        "PT20.345S", // 2 days, 3 hours, 4 minutes
        "P2DT3H4M", // zero seconds
        "PT0S", // zero days
        "P0D", // negative hour with positive minute (ISO‑8601 allows sign on the whole duration)
        "PT-6H3M" };
        // when / then
        for (String s : valid) {
            Duration d = StringConversions.toDuration(s, converter);
            assertEquals(Duration.parse(s), d, "Failed to parse: " + s);
        }
    }

    @Test
    void toDuration_throwsIllegalArgumentExceptionOnInvalidString() {
        // given
        String bad = "not-a-duration";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(bad, converter));
        assertTrue(ex.getMessage().contains(bad));
    }

    @Test
    void toDuration_throwsIllegalArgumentExceptionOnNull() {
        // given
        Object nullObj = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(nullObj, converter));
        assertTrue(ex.getMessage().contains("null"));
    }

    @ParameterizedTest(name = "[{index}] parse \"{0}\"")
    @MethodSource("successfulParsingProvider")
    void toDuration_successfulParsing(String input, Duration expected) {
        // when
        Duration result = StringConversions.toDuration(input, null);
        // then
        assertEquals(expected, result, () -> "Duration parsed from \"" + input + "\" does not match expected.");
    }

    @Test
    void toDuration_nullInput_throwsIllegalArgumentException() {
        // given
        Object nullObj = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(nullObj, null));
        assertTrue(ex.getMessage().contains("null"));
    }

    @Test
    void toDuration_nonStringInput_throwsIllegalArgumentException() {
        // given a non‑String (Integer) – the method casts to String and fails
        Integer nonString = 123;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(nonString, null));
        assertTrue(ex.getMessage().contains("123"));
    }

    @ParameterizedTest(name = "[{index}] malformed \"{0}\"")
    @MethodSource("malformedProvider")
    void toDuration_malformedStrings(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(input, null));
        // The wrapped exception message should contain the original input
        assertTrue(ex.getMessage().contains(input));
    }

    @ParameterizedTest(name = "[{index}] negative \"{0}\"")
    @MethodSource("negativeProvider")
    void toDuration_negativeDurations(String input, Duration expected) {
        Duration result = StringConversions.toDuration(input, null);
        assertEquals(expected, result, () -> "Negative duration parsed from \"" + input + "\" does not match expected.");
    }

    @ParameterizedTest(name = "[{index}] whitespace \"{0}\"")
    @MethodSource("whitespaceProvider")
    void toDuration_whitespaceHandling(String input, Duration expected) {
        Duration result = StringConversions.toDuration(input, null);
        assertEquals(expected, result, () -> "Whitespace‑trimmed duration from \"" + input + "\" does not match expected.");
    }

    @ParameterizedTest(name = "[{index}] large \"{0}\"")
    @MethodSource("largeValuesProvider")
    void toDuration_largeValues(String input, Duration expected) {
        Duration result = StringConversions.toDuration(input, null);
        assertEquals(expected, result, () -> "Large duration parsed from \"" + input + "\" does not match expected.");
    }

    @ParameterizedTest(name = "[{index}] empty/blank \"{0}\"")
    @MethodSource("emptyOrBlankProvider")
    void toDuration_emptyOrBlankStrings(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(input, null));
        assertTrue(ex.getMessage().contains(input));
    }

    @ParameterizedTest(name = "[{index}] invalid sign \"{0}\"")
    @MethodSource("invalidSignProvider")
    void toDuration_invalidSignPlacement(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(input, null));
        assertTrue(ex.getMessage().contains(input));
    }

    @ParameterizedTest(name = "[{index}] unsupported period \"{0}\"")
    @MethodSource("unsupportedPeriodProvider")
    void toDuration_unsupportedPeriodComponents(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(input, null));
        assertTrue(ex.getMessage().contains(input));
    }

    @ParameterizedTest(name = "[{index}] overflow \"{0}\"")
    @MethodSource("overflowProvider")
    void toDuration_overflowScenarios(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(input, null));
        assertTrue(ex.getMessage().contains(input));
    }
}
