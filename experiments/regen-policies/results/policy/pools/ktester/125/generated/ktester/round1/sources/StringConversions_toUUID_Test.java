package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.UUID;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toUUID_Test {

    /**
     * Converter instance used for the tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not need any special options for UUID conversion,
        // but we must provide a non‑null ConverterOptions instance.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the
        // test class tidy and helps the garbage collector.
        converter = null;
    }

    private static Stream<Arguments> provideValidUuidInputs() {
        return Stream.of(// well‑formed lower‑case UUID
        Arguments.of("123e4567-e89b-12d3-a456-426655440000", converter, "lower‑case UUID string"), // upper‑case UUID – UUID.fromString is case‑insensitive
        Arguments.of("123E4567-E89B-12D3-A456-426655440000", converter, "upper‑case UUID string (case‑insensitive)"), // null converter is allowed because the method never touches it
        Arguments.of("123e4567-e89b-12d3-a456-426655440000", null, "valid UUID with null converter"));
    }

    private static Stream<Arguments> provideInvalidUuidInputs() {
        return Stream.of(// ---- Invalid format -------------------------------------------------
        Arguments.of("12345", converter, "too short, not a UUID"), Arguments.of("123e4567-e89b-12d3-a456-426655440000XYZ", converter, "valid UUID followed by extra characters"), Arguments.of("123e4567e89b12d3a456426655440000", converter, "missing hyphens"), Arguments.of("{123e4567-e89b-12d3-a456-426655440000}", converter, "UUID wrapped in braces"), // ---- Null input ------------------------------------------------------
        Arguments.of(null, null, "null source reference"), // ---- Non‑String inputs ------------------------------------------------
        Arguments.of(123, converter, "Integer object (non‑String)"), Arguments.of(true, converter, "Boolean object (non‑String)"), Arguments.of(new StringBuilder("123e4567-e89b-12d3-a456-426655440000"), converter, "StringBuilder whose toString() is a UUID (cast fails)"), // ---- Whitespace handling ---------------------------------------------
        Arguments.of(" 123e4567-e89b-12d3-a456-426655440000 ", converter, "leading/trailing spaces"), Arguments.of("", converter, "empty string"), Arguments.of("   ", converter, "string with only spaces"), Arguments.of("\n123e4567-e89b-12d3-a456-426655440000", converter, "leading newline character"), // ---- Invalid characters ------------------------------------------------
        Arguments.of("123e4567-e89b-12d3-a456-42665544_0000", converter, "underscore character inside UUID"));
    }

    @Test
    @DisplayName("toUUID converts a valid UUID string to a UUID instance")
    void testToUUID_ValidString() {
        // given
        String uuidStr = "123e4567-e89b-12d3-a456-426655440000";
        // when
        UUID result = StringConversions.toUUID(uuidStr, converter);
        // then
        assertNotNull(result, "Result should not be null for a valid UUID string");
        assertEquals(UUID.fromString(uuidStr), result, "The UUID returned by toUUID should match UUID.fromString");
    }

    @Test
    @DisplayName("toUUID throws IllegalArgumentException for an invalid UUID string")
    void testToUUID_InvalidString() {
        // given
        String badUuid = "not-a-valid-uuid";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toUUID(badUuid, converter), "Expected IllegalArgumentException for an invalid UUID string");
        // Verify that the cause is the original IllegalArgumentException from UUID.fromString
        assertNotNull(ex.getCause(), "Cause should be present");
        assertTrue(ex.getCause() instanceof IllegalArgumentException, "Cause should be IllegalArgumentException from UUID parsing");
    }

    @Test
    @DisplayName("toUUID propagates null source as IllegalArgumentException")
    void testToUUID_NullSource() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toUUID(nullObj, converter), "Passing null as the source should result in a NullPointerException before conversion");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideValidUuidInputs")
    @DisplayName("toUUID – successful conversions")
    void testToUUID_Success(Object from, Converter conv, String description) {
        // when
        UUID result = StringConversions.toUUID(from, conv);
        // then
        assertNotNull(result, "Result must not be null for a valid UUID input");
        // The expected UUID is always the lower‑case canonical form of the string
        // (UUID.fromString is case‑insensitive).
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), result, "Parsed UUID does not match the expected value");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideInvalidUuidInputs")
    @DisplayName("toUUID – expected IllegalArgumentException for invalid inputs")
    void testToUUID_Failure(Object from, Converter conv, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toUUID(from, conv), "Expected IllegalArgumentException for: " + description);
        // The wrapped cause should be the original exception thrown by UUID.fromString
        // (or a ClassCastException for non‑String inputs, or NullPointerException for null).
        assertNotNull(ex.getCause(), "The IllegalArgumentException must have a non‑null cause");
        // The exact cause type varies; we only assert that it is an Exception subclass.
        assertTrue(ex.getCause() instanceof Exception, "Cause should be an exception (was: " + ex.getCause().getClass() + ")");
    }

    @Test
    @DisplayName("toUUID throws IllegalArgumentException when source is null (explicit test)")
    void testToUUID_NullSource_Explicit() {
        // given
        Object nullSource = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toUUID(nullSource, converter));
        assertNotNull(ex.getCause(), "Cause should be the NullPointerException thrown by UUID.fromString(null)");
        assertTrue(ex.getCause() instanceof NullPointerException, "Cause must be NullPointerException for a null input string");
    }

    @Test
    @DisplayName("toUUID throws IllegalArgumentException for non‑String source (explicit test)")
    void testToUUID_NonStringSource_Explicit() {
        // given
        Object intSource = Integer.valueOf(42);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toUUID(intSource, converter));
        assertNotNull(ex.getCause(), "Cause should be the ClassCastException caused by casting to String");
        assertTrue(ex.getCause() instanceof ClassCastException, "Cause must be ClassCastException for a non‑String source");
    }
}
