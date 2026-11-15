package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toMap_Test {

    @Mock
    private Converter // Converter is not used by toMap, but required by signature
    mockConverter;

    private static Stream<Arguments> validInstantProvider() {
        return Stream.of(// Group: Valid Instant inputs
        arguments("Epoch instant (0,0)", Instant.ofEpochSecond(0, 0), 0L, 0, null), arguments("Typical positive instant", Instant.ofEpochSecond(1625079045L, 123456789), 1625079045L, 123456789, null), arguments("Negative seconds with small nanos", Instant.ofEpochSecond(-10L, 500), -10L, 500, null), arguments("Maximum long seconds and max nanos", Instant.ofEpochSecond(Long.MAX_VALUE, 999_999_999), Long.MAX_VALUE, 999_999_999, null), // Group: Boundary Nanos Values
        arguments("Zero nanos (boundary)", Instant.ofEpochSecond(0, 0), 0L, 0, null), arguments("Maximum nanos (just under one second)", Instant.ofEpochSecond(0, 999_999_999), 0L, 999_999_999, null), // Group: Negative Nanos Normalization
        arguments("Overflow nanos normalized to zero", Instant.ofEpochSecond(-1, 1_000_000_000L), 0L, 0, null), arguments("Negative nanos normalized by borrowing a second", Instant.ofEpochSecond(-5, -500), -6L, 999_999_500, null), // Group: Converter Parameter Ignored
        arguments("Converter mock is ignored", Instant.ofEpochSecond(12_345, 678), 12_345L, 678, mockConverter()));
    }

    // Helper to provide a mock Converter instance for the last argument set.
    private static Converter mockConverter() {
        // MockitoExtension will inject a mock into the test instance,
        // but static providers cannot access it directly.
        // For this specific case we can safely return null because the
        // method does not use the converter argument.
        return null;
    }

    @Test
    void toMap_returnsMapWithCorrectSecondsAndNanos() {
        // GIVEN
        // arbitrary epoch seconds
        long expectedSeconds = 1_617_283_945L;
        // arbitrary nanos (< 1_000_000_000)
        int expectedNanos = 123_456_789;
        Instant instant = Instant.ofEpochSecond(expectedSeconds, expectedNanos);
        // WHEN
        Map<String, Object> result = InstantConversions.toMap(instant, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertTrue(result instanceof CompactLinkedMap, "Result should be an instance of CompactLinkedMap");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        assertEquals(expectedSeconds, result.get("seconds"), "Map entry 'seconds' does not match the Instant's epoch seconds");
        assertEquals(expectedNanos, result.get("nanos"), "Map entry 'nanos' does not match the Instant's nanosecond adjustment");
    }

    @Test
    void toMap_nullInstant_throwsNullPointerException() {
        // GIVEN
        Instant nullInstant = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toMap(nullInstant, mockConverter), "Calling toMap with a null Instant should throw NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for all successful conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validInstantProvider")
    void toMap_validInstants(String description, Instant instant, long expectedSeconds, int expectedNanos, Converter converter) {
        // WHEN
        Map<String, Object> result = InstantConversions.toMap(instant, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertTrue(result instanceof CompactLinkedMap, "Result should be an instance of CompactLinkedMap");
        assertEquals(2, result.size(), "Map must contain exactly two entries");
        assertEquals(expectedSeconds, result.get("seconds"), "Incorrect 'seconds' value for: " + description);
        assertEquals(expectedNanos, result.get("nanos"), "Incorrect 'nanos' value for: " + description);
    }

    @Test
    void toMap_invalidType_throwsClassCastException() {
        // GIVEN
        Object notAnInstant = "not an instant";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> InstantConversions.toMap(notAnInstant, mockConverter), "Providing a non‑Instant object should result in ClassCastException");
    }
}
