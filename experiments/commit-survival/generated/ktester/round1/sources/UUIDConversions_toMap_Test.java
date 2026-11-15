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

@ExtendWith(MockitoExtension.class)
class UUIDConversions_toMap_Test {

    // but we provide a mock to satisfy the method signature.
    @Mock
    private Converter mockConverter;

    /**
     * Supplies non‑UUID arguments for the parameterized test above.
     */
    private static Stream<Arguments> nonUuidInputs() {
        return Stream.of(// a String that cannot be cast to UUID
        Arguments.of("not-a-uuid"), // an Integer
        Arguments.of(12345));
    }

    @Test
    void toMap_convertsUuidToMapWithCorrectKeyAndValue() {
        // GIVEN
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        // WHEN
        Map<String, Object> result = UUIDConversions.toMap(uuid, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.UUID), "Map should contain the UUID key");
        assertEquals(uuid.toString(), result.get(MapConversions.UUID), "Map value for UUID key should be the UUID's string representation");
    }

    /* --------------------------------------------------------------
     *  Normal behavior – a non‑null Converter is supplied.
     * -------------------------------------------------------------- */
    @Test
    void toMap_withNonNullConverterStillReturnsCorrectMap() {
        // GIVEN
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        // WHEN
        Map<String, Object> result = UUIDConversions.toMap(uuid, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(uuid.toString(), result.get(MapConversions.UUID));
    }

    /* --------------------------------------------------------------
     *  Zero UUID (all bits zero) – special edge case.
     * -------------------------------------------------------------- */
    @Test
    void toMap_zeroUuidReturnsCorrectStringRepresentation() {
        // GIVEN
        UUID zeroUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        // WHEN
        Map<String, Object> result = UUIDConversions.toMap(zeroUuid, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(zeroUuid.toString(), result.get(MapConversions.UUID));
    }

    /* --------------------------------------------------------------
     *  Null input – expect NullPointerException.
     * -------------------------------------------------------------- */
    @Test
    void toMap_nullFromThrowsNullPointerException() {
        // GIVEN
        UUID nullUuid = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UUIDConversions.toMap(nullUuid, mockConverter), "Passing null as the 'from' argument should throw NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Non‑UUID inputs – expect ClassCastException.
     *  Parameterized to cover both a String and an Integer.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("nonUuidInputs")
    void toMap_nonUuidInputThrowsClassCastException(Object nonUuidInput) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> UUIDConversions.toMap(nonUuidInput, mockConverter), "Passing a non‑UUID object should throw ClassCastException");
    }
}
