package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toDuration_Test {

    /**
     * The Converter is not used by {@code StringConversions.toDuration}, but the
     *  method signature requires it, so we provide a mock to keep the test
     *  lightweight and focused on the parsing logic.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> provideSuccessfulCases() {
        return Stream.of(// plain decimal strings
        Arguments.of("123", "PT123S"), Arguments.of("123.456", "PT123.456S"), Arguments.of("-5.5", "-PT5.5S"), Arguments.of("   10.2   ", "PT10.2S"), Arguments.of("00012.34", "PT12.34S"), Arguments.of("0.000", "PT0S"), Arguments.of("9223372036854775807", "PT9223372036854775807S"), Arguments.of("0.1234567891", "PT0.123456789S"), // ISO‑8601 strings
        Arguments.of("PT1H2M3.456S", "PT1H2M3.456S"), Arguments.of("-PT20.5S", "-PT20.5S"), // 2 days → 48h + 3h = 51h
        Arguments.of("P2DT3H4M5.006S", "PT51H4M5.006S"), Arguments.of("P1W", "PT168H"));
    }

    private static Stream<Arguments> provideFailureCases() {
        return Stream.of(// Non‑String inputs
        Arguments.of(42, "Expected a String", null), Arguments.of(null, "Expected a String", null), // Strings that cannot be parsed by either strategy
        Arguments.of("", "Unable to parse", DateTimeParseException.class), Arguments.of("   ", "Unable to parse", DateTimeParseException.class), Arguments.of("+12.34", "Unable to parse", DateTimeParseException.class), Arguments.of(".5", "Unable to parse", DateTimeParseException.class), Arguments.of("123abc", "Unable to parse", DateTimeParseException.class), Arguments.of("notaduration", "Unable to parse", DateTimeParseException.class), Arguments.of("1e3", "Unable to parse", DateTimeParseException.class), Arguments.of("123.", "Unable to parse", DateTimeParseException.class), Arguments.of("-.", "Unable to parse", DateTimeParseException.class), // Invalid ISO‑8601 strings (all result in DateTimeParseException)
        Arguments.of("PT-1H", "Unable to parse", DateTimeParseException.class), Arguments.of("P1M", "Unable to parse", DateTimeParseException.class), Arguments.of("P1Y", "Unable to parse", DateTimeParseException.class), Arguments.of("PT1SXYZ", "Unable to parse", DateTimeParseException.class), Arguments.of("PT1,5S", "Unable to parse", DateTimeParseException.class), Arguments.of("PT1H2M3,456S", "Unable to parse", DateTimeParseException.class), Arguments.of("NaN", "Unable to parse", DateTimeParseException.class), Arguments.of("Infinity", "Unable to parse", DateTimeParseException.class));
    }

    @Test
    void parsesDecimalSecondsWholeNumber() {
        // given
        String source = "123";
        // when
        Duration result = StringConversions.toDuration(source, mockConverter);
        // then
        assertEquals(Duration.ofSeconds(123), result);
    }

    @Test
    void parsesDecimalSecondsWithFraction() {
        // given
        // includes surrounding whitespace
        String source = "  45.6789  ";
        // when
        Duration result = StringConversions.toDuration(source, mockConverter);
        // then
        // 45 seconds + 0.6789 seconds = 45 seconds + 678_900_000 nanoseconds
        Duration expected = Duration.ofSeconds(45, 678_900_000);
        assertEquals(expected, result);
    }

    @Test
    void parsesIso8601Duration() {
        // given
        // 1 hour, 2 minutes, 3.456 seconds
        String source = "PT1H2M3.456S";
        // when
        Duration result = StringConversions.toDuration(source, mockConverter);
        // then
        Duration expected = Duration.ofHours(1).plusMinutes(2).plusSeconds(3).plusNanos(456_000_000);
        assertEquals(expected, result);
    }

    @Test
    void throwsWhenSourceIsNotString() {
        // given
        // an Integer, not a String
        Object source = 12345;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(source, mockConverter));
        assertTrue(ex.getMessage().contains("Expected a String"));
    }

    @Test
    void throwsWhenStringCannotBeParsed() {
        // given
        String source = "not-a-duration";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(source, mockConverter));
        // The message should mention the inability to parse and list the expected formats.
        String msg = ex.getMessage();
        assertTrue(msg.contains("Unable to parse"));
        assertTrue(msg.contains("Decimal seconds"));
        assertTrue(msg.contains("ISO-8601 duration"));
        // The original cause should be a DateTimeParseException or NumberFormatException.
        assertNotNull(ex.getCause());
    }

    @ParameterizedTest(name = "[{index}] parses ''{0}'' → {1}")
    @MethodSource("provideSuccessfulCases")
    void parsesSuccessfully(Object from, String expectedIso) {
        // when
        Duration result = StringConversions.toDuration(from, mockConverter);
        // then
        Duration expected = Duration.parse(expectedIso);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → IllegalArgumentException")
    @MethodSource("provideFailureCases")
    void throwsExpectedException(Object from, String expectedMessageFragment, Class<? extends Throwable> expectedCauseClass) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDuration(from, mockConverter));
        // Verify the message contains the expected fragment.
        assertTrue(ex.getMessage().contains(expectedMessageFragment), () -> "Message [" + ex.getMessage() + "] does not contain expected fragment [" + expectedMessageFragment + "]");
        // Verify the cause, if an expectation was supplied.
        if (expectedCauseClass != null) {
            assertNotNull(ex.getCause(), () -> "Expected cause of type " + expectedCauseClass.getSimpleName() + " but cause was null");
            assertTrue(expectedCauseClass.isInstance(ex.getCause()), () -> "Cause is not an instance of " + expectedCauseClass.getSimpleName());
        }
    }
}
