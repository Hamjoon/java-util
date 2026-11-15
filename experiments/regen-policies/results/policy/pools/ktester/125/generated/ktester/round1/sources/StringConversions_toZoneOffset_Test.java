package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import java.time.ZoneOffset;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toZoneOffset_Test {

    @Mock
    private Converter // mock – we only need an instance, no behaviour
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test never touches the converter
        verifyNoInteractions(mockConverter);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> validOffsetsProvider() {
        return Stream.of(arguments("+02:00", ZoneOffset.of("+02:00")), arguments("-05:30", ZoneOffset.of("-05:30")), arguments("Z", ZoneOffset.UTC), arguments("+00:00", ZoneOffset.UTC), // extreme but still valid
        arguments("+18:00", ZoneOffset.of("+18:00")), arguments("-18:00", ZoneOffset.of("-18:00")), // short zero‑offset form
        arguments("+00", ZoneOffset.UTC));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> nullOrEmptyProvider() {
        return Stream.of(arguments(null), arguments(""));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> invalidOffsetsProvider() {
        return Stream.of(// missing minutes part
        arguments("+02"), // seconds part not accepted
        arguments("+02:00:00"), // literal "UTC"
        arguments("UTC"), // hour exceeds +18
        arguments("+25:00"), // negative hour beyond -18
        arguments("-19:30"), // minute out of range
        arguments("+02:60"), // leading whitespace
        arguments(" +02:00"), // trailing whitespace
        arguments("+02:00 "), // missing sign
        arguments("02:00"), // extra characters after valid offset
        arguments("+02:00ABC"), // hour not zero‑padded
        arguments("+2:00"), // non‑numeric hour
        arguments("+AB:00"), // minute not zero‑padded
        arguments("+02:0"), // unsupported prefix
        arguments("GMT+02:00"), // newline character
        arguments("+02:00\n"));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> nonStringInputsProvider() {
        return Stream.of(arguments(12345), arguments(Boolean.TRUE));
    }

    @Test
    void returnsNullWhenInputIsNullOrEmpty() {
        // null input
        assertNull(StringConversions.toZoneOffset(null, mockConverter), "Null input should result in null ZoneOffset");
        // empty string input
        assertNull(StringConversions.toZoneOffset("", mockConverter), "Empty string should result in null ZoneOffset");
    }

    @Test
    void returnsCorrectZoneOffsetForValidString() {
        String offsetStr = "+02:00";
        ZoneOffset expected = ZoneOffset.of(offsetStr);
        ZoneOffset actual = StringConversions.toZoneOffset(offsetStr, mockConverter);
        assertEquals(expected, actual, "Valid offset string should be converted to the correct ZoneOffset");
    }

    @Test
    void throwsIllegalArgumentExceptionForInvalidString() {
        String bad = "not-a-zone";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toZoneOffset(bad, mockConverter), "Invalid offset string should cause IllegalArgumentException");
        // Optional: check that the message contains the expected text
        assertTrue(ex.getMessage().contains("Unknown time-zone offset"), "Exception message should indicate unknown time-zone offset");
    }

    @Test
    void throwsIllegalArgumentExceptionWhenInputIsWhitespaceOnly() {
        String whitespace = "   ";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toZoneOffset(whitespace, mockConverter), "Whitespace‑only string should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains("Unknown time-zone offset"), "Exception message should indicate unknown time-zone offset");
    }

    /* -------------------------------------------------------------
     *  Non‑String source objects – expect ClassCastException
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] non‑String input {0} throws ClassCastException")
    @MethodSource("nonStringInputsProvider")
    void throwsClassCastExceptionWhenInputIsNotString(Object input) {
        assertThrows(ClassCastException.class, () -> StringConversions.toZoneOffset(input, mockConverter), "Non‑String input should cause ClassCastException");
    }
}
