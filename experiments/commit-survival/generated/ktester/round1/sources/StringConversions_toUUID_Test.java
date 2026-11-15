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

@ExtendWith(MockitoExtension.class)
class StringConversions_toUUID_Test {

    /**
     * The Converter instance required by the method under test.
     */
    @Mock
    private Converter // not used by the implementation, but must be supplied
    converter;

    private static Stream<Arguments> validUuidProvider() {
        return Stream.of(// lower‑case standard format
        Arguments.of("Standard lower‑case UUID", "123e4567-e89b-12d3-a456-426614174000", UUID.fromString("123e4567-e89b-12d3-a456-426614174000")), // upper‑case is accepted by UUID.fromString
        Arguments.of("Upper‑case UUID string", "123E4567-E89B-12D3-A456-426614174000", UUID.fromString("123e4567-e89b-12d3-a456-426614174000")));
    }

    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(// null reference – cast succeeds (null), UUID.fromString throws NPE
        Arguments.of("Null input object", null), // empty string – UUID.fromString throws IllegalArgumentException
        Arguments.of("Empty string", ""), // whitespace only – not a valid UUID
        Arguments.of("Whitespace‑only string", "   "), // missing hyphens
        Arguments.of("String without hyphens", "123e4567e89b12d3a456426614174000"), // invalid hex characters
        Arguments.of("String with non‑hex characters", "zzze4567-e89b-12d3-a456-426614174000"), // extra trailing characters
        Arguments.of("String with extra characters", "123e4567-e89b-12d3-a456-426614174000XYZ"));
    }

    @Test
    void toUUID_givenValidUuidString_returnsCorrespondingUuid() {
        // given
        String uuidString = "123e4567-e89b-12d3-a456-426655440000";
        // when
        UUID result = StringConversions.toUUID(uuidString, converter);
        // then
        assertEquals(UUID.fromString(uuidString), result, "The UUID returned should match the one parsed directly from the string");
    }

    @Test
    void toUUID_givenInvalidUuidString_throwsIllegalArgumentException() {
        // given
        String badString = "not-a-uuid";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toUUID(badString, converter));
        assertTrue(ex.getMessage().contains("Unable to convert"), "Exception message should indicate conversion failure");
        assertNotNull(ex.getCause(), "The thrown IllegalArgumentException should wrap the original exception");
    }

    @Test
    void toUUID_givenNullInput_throwsIllegalArgumentException() {
        // given
        Object nullInput = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toUUID(nullInput, converter));
        assertTrue(ex.getMessage().contains("Unable to convert"), "Exception message should indicate conversion failure for null input");
        assertNotNull(ex.getCause(), "The thrown IllegalArgumentException should wrap the original exception");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validUuidProvider")
    @DisplayName("toUUID – valid UUID strings")
    void toUUID_givenValidString_returnsUuid(String description, Object input, UUID expected) {
        // The method expects an Object; we pass the string directly.
        UUID actual = StringConversions.toUUID(input, converter);
        assertEquals(expected, actual, "The UUID returned should match the expected value");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("illegalArgumentProvider")
    @DisplayName("toUUID – inputs that should result in IllegalArgumentException")
    void toUUID_givenInvalidInput_throwsIllegalArgumentException(String description, Object input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toUUID(input, converter), "Method should wrap parsing failures in IllegalArgumentException");
        // The original cause should be either NullPointerException or IllegalArgumentException
        assertNotNull(ex.getCause(), "The thrown exception must have a non‑null cause");
        assertTrue(ex.getMessage().contains("Unable to convert"), "Exception message should contain the standard conversion‑failure text");
    }

    @Test
    @DisplayName("toUUID – non‑String source should raise ClassCastException")
    void toUUID_givenNonStringSource_throwsClassCastException() {
        // not a String
        Object nonString = Integer.valueOf(12345);
        assertThrows(ClassCastException.class, () -> StringConversions.toUUID(nonString, converter), "A direct cast to String should fail before any UUID parsing occurs");
    }

    @Test
    @DisplayName("toUUID – valid UUID string with null Converter")
    void toUUID_validString_nullConverter_returnsUuid() {
        String uuidStr = "123e4567-e89b-12d3-a456-426655440000";
        // converter is optional
        UUID result = StringConversions.toUUID(uuidStr, null);
        assertEquals(UUID.fromString(uuidStr), result);
    }
}
