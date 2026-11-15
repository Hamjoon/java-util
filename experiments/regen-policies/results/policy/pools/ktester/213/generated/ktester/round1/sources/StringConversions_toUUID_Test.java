package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StringConversions_toUUID_Test {

    /**
     * The Converter is not used by {@code toUUID}, but the method signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Valid UUID strings – the method should return the parsed UUID.
     */
    private static Stream<Arguments> validUuidProvider() {
        return Stream.of(// lower‑case standard representation
        Arguments.of("123e4567-e89b-12d3-a456-426614174000"), // upper‑case representation – UUID parsing is case‑insensitive
        Arguments.of("123E4567-E89B-12D3-A456-426614174000"));
    }

    /**
     * Invalid UUID strings – the method must wrap the thrown exception.
     */
    private static Stream<Arguments> invalidUuidProvider() {
        return Stream.of(Arguments.of("not-a-uuid"), // empty string
        Arguments.of(""), // leading/trailing spaces
        Arguments.of(" 123e4567-e89b-12d3-a456-426614174000 "), // braces
        Arguments.of("{123e4567-e89b-12d3-a456-426614174000}"), // extra chars
        Arguments.of("123e4567-e89b-12d3-a456-426614174000XYZ"), // non‑hex character
        Arguments.of("g23e4567-e89b-12d3-a456-426614174000"), // missing hyphens
        Arguments.of("123e4567e89b12d3a456426614174000"), // extra hyphen segment
        Arguments.of("123e4567-e89b-12d3-a456-4266-14174000"), // newline at end
        Arguments.of("123e4567-e89b-12d3-a456-426614174000\n"), // leading tab
        Arguments.of("\t123e4567-e89b-12d3-a456-426614174000"), // non‑breaking space
        Arguments.of("\u00A0123e4567-e89b-12d3-a456-426614174000"));
    }

    /**
     * Null input – should be wrapped in IllegalArgumentException.
     */
    private static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of((Object) null));
    }

    /**
     * Non‑String input – the cast to String happens before the try block, so a ClassCastException is expected.
     */
    private static Stream<Arguments> nonStringProvider() {
        return Stream.of(Arguments.of(12345), Arguments.of(new Object()));
    }

    @Test
    void toUUID_ValidString_ReturnsExpectedUuid() {
        // WHEN
        UUID result = StringConversions.toUUID(uuidString, mockConverter);
        // THEN
        assertEquals(UUID.fromString(uuidString.toLowerCase()), result, "The UUID returned by toUUID should match the parsed UUID.");
        // Verify that the converter is truly unused.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toUUID_InvalidString_ThrowsIllegalArgumentException() {
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toUUID(badUuid, mockConverter));
        // The message should contain the original input value.
        assertTrue(ex.getMessage().contains("Unable to convert"), "Exception message should indicate conversion failure.");
        assertTrue(ex.getMessage().contains(badUuid), "Exception message should contain the problematic input.");
        assertNotNull(ex.getCause(), "The thrown IllegalArgumentException should wrap the original exception.");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toUUID_NullInput_ThrowsIllegalArgumentException() {
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toUUID(nullObj, mockConverter));
        assertTrue(ex.getMessage().contains("Unable to convert"), "Exception message should indicate conversion failure for null input.");
        assertTrue(ex.getMessage().contains("null"), "Exception message should contain the string \"null\".");
        assertNotNull(ex.getCause(), "The thrown IllegalArgumentException should wrap the original exception.");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] non‑String input \"{0}\"")
    @MethodSource("nonStringProvider")
    void toUUID_NonStringInput_ThrowsClassCastException(Object nonString) {
        // The cast to String occurs before the try block, so we expect a ClassCastException.
        assertThrows(ClassCastException.class, () -> StringConversions.toUUID(nonString, mockConverter), "Passing a non‑String should result in a ClassCastException.");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toUUID_ValidString_WithNonNullConverter_DoesNotAffectResult() {
        // GIVEN
        String uuidString = "123e4567-e89b-12d3-a456-426614174000";
        // WHEN
        UUID result = StringConversions.toUUID(uuidString, mockConverter);
        // THEN
        assertEquals(UUID.fromString(uuidString), result, "The presence of a non‑null Converter should not affect conversion.");
        verifyNoInteractions(mockConverter);
    }
}
