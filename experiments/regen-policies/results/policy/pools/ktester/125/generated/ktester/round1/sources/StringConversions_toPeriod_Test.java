package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Period;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DateTimeException;
import java.time.DateTimeParseException;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toPeriod_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; use the default constructor.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    static Stream<Arguments> validPeriodProvider() {
        return Stream.of(// Group: Valid Period Strings
        arguments("P1Y2M3D", Period.of(1, 2, 3)), arguments("P0D", Period.ZERO), arguments("P5Y", Period.of(5, 0, 0)), // Group: Additional Valid Period Strings
        arguments("P3M", Period.of(0, 3, 0)), arguments("P4D", Period.of(0, 0, 4)), // 2 weeks → 14 days
        arguments("P2W", Period.ofDays(14)), arguments("P3M5D", Period.of(0, 3, 5)), arguments("P2147483647Y", Period.of(2147483647, 0, 0)), arguments("P-5D", Period.of(0, 0, -5)), // converter ignored
        arguments("P1Y", Period.of(1, 0, 0)));
    }

    static Stream<Arguments> invalidPeriodProvider() {
        return Stream.of(// Group: Invalid Period Strings
        arguments("not-a-period", DateTimeParseException.class), arguments("", DateTimeParseException.class), // Group: Additional Invalid Period Strings
        // leading whitespace
        arguments(" P1Y", DateTimeParseException.class), // trailing whitespace
        arguments("P1Y ", DateTimeParseException.class), // lower‑case designator
        arguments("p1Y2M3D", DateTimeParseException.class), // time component
        arguments("P1Y2M3DT4H", DateTimeParseException.class), // extra chars
        arguments("P1Y2M3Dextra", DateTimeParseException.class), // overflow days
        arguments("P2147483648D", DateTimeParseException.class), // Group: Edge Case Invalid Period Strings
        // negative year
        arguments("P-1Y", DateTimeParseException.class), // negative month
        arguments("P-1M", DateTimeParseException.class), // only designator
        arguments("P", DateTimeParseException.class), // leading plus
        arguments("P+1Y", DateTimeParseException.class), // overflow months
        arguments("P2147483648M", DateTimeParseException.class));
    }

    static Stream<Arguments> nonStringInputProvider() {
        return Stream.of(// Group: Non‑String Input Types
        // Integer
        arguments(123), // java.util.Date
        arguments(new Date()), // Group: Additional Non‑String Input Types
        // CharSequence but not String
        arguments(new StringBuilder("P1Y")), // Array object
        arguments(new Object[] { "P1Y" }));
    }

    @Test
    @DisplayName("toPeriod parses a valid ISO‑8601 period string")
    void testToPeriod_ValidString() {
        // given
        // 2 years, 3 months, 5 days
        String isoPeriod = "P2Y3M5D";
        Period expected = Period.of(2, 3, 5);
        // when
        Period actual = StringConversions.toPeriod(isoPeriod, converter);
        // then
        assertEquals(expected, actual, () -> "Expected Period " + expected + " but got " + actual);
    }

    @Test
    @DisplayName("toPeriod throws IllegalArgumentException for an invalid period string")
    void testToPeriod_InvalidString() {
        // given
        String badPeriod = "not-a-period";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toPeriod(badPeriod, converter));
        assertTrue(ex.getMessage().contains("Unable to parse"), "Exception message should indicate a parsing problem");
        assertNotNull(ex.getCause(), "The cause of the IllegalArgumentException should be the original parsing exception");
        assertTrue(ex.getCause() instanceof DateTimeParseException, "Cause should be a DateTimeParseException");
    }

    @Test
    @DisplayName("toPeriod throws IllegalArgumentException when source is null")
    void testToPeriod_NullSource() {
        // given
        Object nullSource = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toPeriod(nullSource, converter));
        assertTrue(ex.getMessage().contains("Unable to parse"), "Exception message should indicate a parsing problem for null input");
        assertNotNull(ex.getCause(), "The cause should be the NullPointerException thrown by Period.parse");
        assertTrue(ex.getCause() instanceof NullPointerException, "Cause should be a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] Valid period ''{0}'' → {1}")
    @MethodSource("validPeriodProvider")
    @DisplayName("Valid Period Strings")
    void testToPeriod_ValidStrings(String from, Period expected) {
        // The converter is irrelevant for parsing; we pass null to emphasise that.
        Period actual = StringConversions.toPeriod(from, null);
        assertEquals(expected, actual, () -> "Parsing '" + from + "' should yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] Invalid period ''{0}'' → IllegalArgumentException")
    @MethodSource("invalidPeriodProvider")
    @DisplayName("Invalid Period Strings")
    void testToPeriod_InvalidStrings(String from, Class<? extends Throwable> expectedCause) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toPeriod(from, null), () -> "Expected IllegalArgumentException for input: " + Objects.toString(from));
        assertNotNull(ex.getCause(), "Cause of IllegalArgumentException should not be null");
        assertTrue(expectedCause.isInstance(ex.getCause()), () -> "Expected cause of type " + expectedCause.getSimpleName() + " but got " + ex.getCause().getClass().getSimpleName());
    }

    @ParameterizedTest(name = "[{index}] Non‑String input ''{0}'' → ClassCastException")
    @MethodSource("nonStringInputProvider")
    @DisplayName("Non‑String Input Types")
    void testToPeriod_NonStringInputs(Object from) {
        assertThrows(ClassCastException.class, () -> StringConversions.toPeriod(from, null), () -> "Casting non‑String input " + from.getClass().getSimpleName() + " should throw ClassCastException");
    }
}
