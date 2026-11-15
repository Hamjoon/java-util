package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.ZoneOffset;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toZoneOffset_Test {

    /**
     * A mock {@link Converter} – the method does not use it, but it must be supplied.
     */
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        mockConverter = mock(Converter.class);
    }

    private static Stream<Arguments> validOffsetProvider() {
        return Stream.of(Arguments.of("Positive hour and minute offset", "+02:00", "+02:00"), Arguments.of("Negative hour and minute offset", "-05:30", "-05:30"), Arguments.of("UTC designator", "Z", "Z"));
    }

    private static Stream<Arguments> emptyAndNullProvider() {
        return Stream.of(// According to the implementation a null source is treated as empty → returns null.
        Arguments.of("null source returns null", null, null, null), // Empty string is considered empty by StringUtilities.isEmpty → returns null.
        Arguments.of("empty string returns null", "", null, null), // Whitespace only string is NOT empty → ZoneOffset.of throws, wrapped as IllegalArgumentException.
        Arguments.of("whitespace only string throws IllegalArgumentException", " ", IllegalArgumentException.class, null));
    }

    private static Stream<Arguments> additionalValidOffsetsProvider() {
        return Stream.of(Arguments.of("Hour‑only format", "+02", "+02:00"), Arguments.of("Offset with seconds (seconds ignored)", "+02:00:00", "+02:00"), Arguments.of("Zero offset using '+00:00' normalizes to 'Z'", "+00:00", "Z"), Arguments.of("Zero offset with negative sign normalizes to 'Z'", "-00:00", "Z"));
    }

    private static Stream<Arguments> invalidFormatsProvider() {
        return Stream.of(Arguments.of("Non‑parseable string", "invalid", IllegalArgumentException.class), Arguments.of("Single‑digit hour (invalid)", "+2", IllegalArgumentException.class), Arguments.of("Double sign (invalid)", "++02:00", IllegalArgumentException.class), Arguments.of("Minute out of range (60)", "+02:60", IllegalArgumentException.class), Arguments.of("Hour out of range (24)", "+24:00", IllegalArgumentException.class));
    }

    private static Stream<Arguments> nonStringInputsProvider() {
        return Stream.of(Arguments.of("Integer input", 123, ClassCastException.class), Arguments.of("Boolean input", true, ClassCastException.class), Arguments.of("Object input", new Object(), ClassCastException.class));
    }

    @Test
    @DisplayName("null input returns null")
    void testNullInputReturnsNull() {
        ZoneOffset result = StringConversions.toZoneOffset(null, mockConverter);
        assertNull(result, "A null source should result in a null ZoneOffset");
    }

    @Test
    @DisplayName("empty string input returns null")
    void testEmptyStringReturnsNull() {
        ZoneOffset result = StringConversions.toZoneOffset("", mockConverter);
        assertNull(result, "An empty string should result in a null ZoneOffset");
    }

    @Test
    @DisplayName("valid offset strings are converted correctly")
    void testValidOffsets() {
        // Positive offset
        ZoneOffset plusTwo = StringConversions.toZoneOffset("+02:00", mockConverter);
        assertEquals(ZoneOffset.ofHours(2), plusTwo, "Should parse '+02:00' correctly");
        // Negative offset
        ZoneOffset minusFive = StringConversions.toZoneOffset("-05:00", mockConverter);
        assertEquals(ZoneOffset.ofHours(-5), minusFive, "Should parse '-05:00' correctly");
        // UTC designator
        ZoneOffset utc = StringConversions.toZoneOffset("Z", mockConverter);
        assertEquals(ZoneOffset.UTC, utc, "Should parse 'Z' (UTC) correctly");
    }

    @Test
    @DisplayName("invalid offset string throws IllegalArgumentException")
    void testInvalidOffsetThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toZoneOffset("invalid-offset", mockConverter), "An unparsable offset should cause IllegalArgumentException");
        // The message is defined in the implementation – verify it contains the supplied value.
        assertTrue(ex.getMessage().contains("invalid-offset"), "Exception message should contain the problematic offset string");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validOffsetProvider")
    void testValidZoneOffsets(String description, Object input, String expected) {
        ZoneOffset result = StringConversions.toZoneOffset(input, mockConverter);
        ZoneOffset expectedOffset = "Z".equals(expected) ? ZoneOffset.UTC : ZoneOffset.of(expected);
        assertEquals(expectedOffset, result, () -> "Expected ZoneOffset.of(\"" + expected + "\") for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("emptyAndNullProvider")
    void testEmptyAndNullInputs(String description, Object input, Class<? extends Throwable> expectedException, String expectedResult) {
        if (expectedException != null) {
            // Expect an exception (NullPointerException, IllegalArgumentException, etc.)
            Throwable thrown = assertThrows(expectedException, () -> StringConversions.toZoneOffset(input, mockConverter), () -> "Expected exception " + expectedException.getSimpleName() + " for input: " + input);
            // For IllegalArgumentException we also verify the message contains the problematic value
            if (expectedException == IllegalArgumentException.class && input != null) {
                assertTrue(thrown.getMessage().contains(String.valueOf(input)), "Exception message should contain the problematic value");
            }
        } else {
            // Expect a normal return (null)
            ZoneOffset result = StringConversions.toZoneOffset(input, mockConverter);
            assertNull(result, "Expected null result for input: " + input);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalValidOffsetsProvider")
    void testAdditionalValidOffsets(String description, Object input, String expected) {
        ZoneOffset result = StringConversions.toZoneOffset(input, mockConverter);
        ZoneOffset expectedOffset = "Z".equals(expected) ? ZoneOffset.UTC : ZoneOffset.of(expected);
        assertEquals(expectedOffset, result, () -> "Expected normalized ZoneOffset.of(\"" + expected + "\") for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidFormatsProvider")
    void testInvalidFormats(String description, Object input, Class<? extends Throwable> expectedException) {
        Throwable thrown = assertThrows(expectedException, () -> StringConversions.toZoneOffset(input, mockConverter), () -> "Expected exception for invalid input: " + input);
        if (expectedException == IllegalArgumentException.class) {
            assertTrue(thrown.getMessage().contains(String.valueOf(input)), "Exception message should contain the problematic offset string");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonStringInputsProvider")
    void testNonStringInputs(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringConversions.toZoneOffset(input, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for non‑String input: " + input);
    }
}
