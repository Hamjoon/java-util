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

    /**
     * A mocked {@link Converter} – the method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Shared UUID used by the tests.
     */
    private static UUID sharedUuid;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a deterministic UUID that can be reused across tests.
        sharedUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none here).
        sharedUuid = null;
    }

    /**
     * Supplies a stream of valid UUID instances for the successful conversion test.
     */
    private static Stream<Arguments> provideValidUuids() {
        return Stream.of(arguments(sharedUuid), // Additional deterministic UUIDs can be added here if needed.
        arguments(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }

    /**
     * Supplies invalid inputs together with the exception type we expect the method to throw.
     */
    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(arguments(null, NullPointerException.class), arguments("not-a-uuid", ClassCastException.class));
    }

    @Test
    void toMap_returnsMapContainingUuidString() {
        // GIVEN
        // the object to be converted
        Object source = sharedUuid;
        // mockConverter is injected by Mockito; it is not used inside the method.
        // WHEN
        Map<String, Object> result = UUIDConversions.toMap(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        // The key used by the conversion logic is defined in MapConversions.UUID
        assertTrue(result.containsKey(MapConversions.UUID), "Result map should contain the key defined by MapConversions.UUID");
        assertEquals(sharedUuid.toString(), result.get(MapConversions.UUID), "The stored value must be the string representation of the UUID");
    }

    @Test
    void toMap_ignoresAdditionalEntries() {
        // GIVEN
        Object source = sharedUuid;
        // WHEN
        Map<String, Object> result = UUIDConversions.toMap(source, mockConverter);
        // THEN
        // The implementation creates a fresh CompactMap with insertion order and only puts one entry.
        assertEquals(1, result.size(), "The conversion should produce a map with exactly one entry");
    }

    @Test
    void toMap_throwsClassCastExceptionWhenSourceIsNotUuid() {
        // GIVEN
        Object source = "not-a-uuid";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> UUIDConversions.toMap(source, mockConverter), "Passing a non‑UUID object should result in a ClassCastException");
    }

    /**
     * Verifies that a valid {@link UUID} is converted into a map containing exactly one entry:
     * the key is {@link MapConversions#UUID} and the value is the UUID's string representation.
     */
    @ParameterizedTest(name = "toMap_successfulConversion [{index}]")
    @MethodSource("provideValidUuids")
    void toMap_successfulConversion(UUID from) {
        // GIVEN
        // the object to be converted
        Object source = from;
        // WHEN
        Map<String, Object> result = UUIDConversions.toMap(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "The conversion should produce a map with exactly one entry");
        assertTrue(result.containsKey(MapConversions.UUID), "Result map should contain the key defined by MapConversions.UUID");
        assertEquals(from.toString(), result.get(MapConversions.UUID), "The stored value must be the string representation of the UUID");
    }

    /**
     * Ensures the method throws the expected exception when the input is invalid.
     * Two cases are covered:
     * <ul>
     *   <li> {@code null} – results in {@link NullPointerException} because the method
     *        dereferences the casted UUID.</li>
     *   <li> a non‑UUID object (e.g., a {@link String}) – results in {@link ClassCastException}
     *        when the cast to {@link UUID} fails.</li>
     * </ul>
     */
    @ParameterizedTest(name = "toMap_invalidInput [{index}]")
    @MethodSource("provideInvalidInputs")
    void toMap_invalidInput(Object from, Class<? extends Throwable> expectedException) {
        // GIVEN
        Object source = from;
        // WHEN / THEN
        assertThrows(expectedException, () -> UUIDConversions.toMap(source, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
