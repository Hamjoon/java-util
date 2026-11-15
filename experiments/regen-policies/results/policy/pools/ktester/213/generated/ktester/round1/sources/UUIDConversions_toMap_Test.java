package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class UUIDConversions_toMap_Test {

    // ----------------------------------------------------------------------
    @Mock
    private Converter // not used by the method but required by signature
    converter;

    /**
     * Provides a stream of arguments: the source {@link UUID} and the expected
     * string that must appear in the resulting map under {@link MapConversions#UUID}.
     */
    private static Stream<Arguments> provideUuidsForMap() {
        return Stream.of(// Regular UUID from string
        arguments(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), "123e4567-e89b-12d3-a456-426614174000"), // All‑zero UUID
        arguments(UUID.fromString("00000000-0000-0000-0000-000000000000"), "00000000-0000-0000-0000-000000000000"), // All‑ones (max) UUID
        arguments(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "ffffffff-ffff-ffff-ffff-ffffffffffff"), // UUID created from the maximum signed long values
        arguments(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), "7fffffff-ffff-ffff-7fff-ffffffffffff"), // UUID constructed with both most and least significant bits zero
        arguments(new UUID(0L, 0L), "00000000-0000-0000-0000-000000000000"), // Name‑based UUID generated from an empty byte array
        arguments(UUID.nameUUIDFromBytes(new byte[] {}), "6ba7b810-9dad-11d1-80b4-00c04fd430c8"));
    }

    // ----------------------------------------------------------------------
    @Test
    void toMap_ShouldReturnMapContainingUuidString() {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        // WHEN
        Map<String, Object> result = UUIDConversions.toMap(uuid, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Resulting map must contain exactly one entry");
        assertEquals(uuid.toString(), result.get(MapConversions.UUID), "Map value must be the UUID string representation");
    }

    @Test
    void toMap_NullUuid_ShouldThrowNullPointerException() {
        // GIVEN
        UUID uuid = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UUIDConversions.toMap(uuid, converter), "Calling toMap with a null UUID should throw NullPointerException");
    }

    @Test
    void toMap_NonUuidObject_ShouldThrowClassCastException() {
        // GIVEN
        Object notAUuid = "not-a-uuid";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> UUIDConversions.toMap(notAUuid, converter), "Calling toMap with a non‑UUID object should throw ClassCastException");
    }

    // ----------------------------------------------------------------------
    @Test
    void toMap_RandomUuid_ShouldReturnMapWithMatchingString() {
        // GIVEN
        UUID randomUuid = UUID.randomUUID();
        // WHEN
        Map<String, Object> result = UUIDConversions.toMap(randomUuid, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Resulting map must contain exactly one entry");
        assertEquals(randomUuid.toString(), result.get(MapConversions.UUID), "Map value must equal the original UUID's string representation");
    }
}
