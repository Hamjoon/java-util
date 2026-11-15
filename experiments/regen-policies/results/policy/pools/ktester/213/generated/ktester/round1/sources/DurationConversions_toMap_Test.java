package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.DURATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toMap_Test {

    /**
     * The mock Converter that will be supplied to the method under test.
     */
    @Mock
    private Converter mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid interaction bleed‑over between tests.
        reset(mockConverter);
    }

    /**
     * Supplies the arguments for {@link #toMap_validDurations(Duration, Converter, String)}.
     *
     * @return a stream of arguments: {@code Duration}, {@code Converter} (null or mock), description
     */
    static Stream<Arguments> validDurationProvider() {
        // A mock Converter for the case that explicitly supplies one
        Converter mock = mock(Converter.class);
        return Stream.of(Arguments.of(Duration.ofHours(1), null, "Converts a positive Duration of 1 hour to a map; expects \"PT1H\""), Arguments.of(Duration.ZERO, null, "Converts a zero Duration; expects \"PT0S\""), Arguments.of(Duration.ofSeconds(-30), null, "Converts a negative Duration of 30 seconds; expects \"-PT30S\" (or Java's representation)"), Arguments.of(Duration.ofDays(365).plusHours(5).plusMinutes(12).plusSeconds(7), null, "Converts a large Duration (1 year + 5h12m7s); expects long ISO‑8601 string"), Arguments.of(Duration.ofMinutes(15), mock, "Passes a non‑null Converter mock to ensure it is ignored"));
    }

    /**
     * Supplies arguments for {@link #toMap_invalidTypes_throwClassCastException(Object)}.
     *
     * @return a stream of objects that are not {@link Duration}
     */
    static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("some string"), Arguments.of(Integer.valueOf(5)));
    }

    @Test
    void toMap_convertsDurationToMapWithCorrectKeyAndValue() {
        // GIVEN
        Duration input = Duration.ofHours(5).plusMinutes(30).plusSeconds(15);
        // WHEN
        Map<String, Object> result = DurationConversions.toMap(input, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Resulting map must contain exactly one entry");
        assertTrue(result.containsKey(DURATION), "Map must contain the DURATION key");
        assertEquals(input.toString(), result.get(DURATION), "Map value for DURATION must be the string representation of the Duration");
        // Verify that the converter was not used (method implementation does not call it)
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toMap_nullInput_throwsNullPointerException() {
        // GIVEN
        Duration input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DurationConversions.toMap(input, mockConverter), "Passing a null Duration should result in NullPointerException");
    }

    /**
     * Parameterized test that covers all valid {@link Duration} conversion scenarios.
     *
     * @param duration   the {@code Duration} instance to convert
     * @param converter  the {@link Converter} to pass (may be {@code null} or a mock)
     * @param description a short description of the test case (used only for readability)
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validDurationProvider")
    void toMap_validDurations(Duration duration, Converter converter, String description) {
        // WHEN
        Map<String, Object> result = DurationConversions.toMap(duration, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Resulting map must contain exactly one entry");
        assertTrue(result.containsKey(DURATION), "Map must contain the DURATION key");
        assertEquals(duration.toString(), result.get(DURATION), "Map value for DURATION must be the ISO‑8601 string representation of the Duration");
        // The implementation does not use the Converter; verify that no interactions occurred
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    /**
     * Parameterized test that verifies a {@link ClassCastException} is thrown when the
     * {@code from} argument is not a {@link Duration}.
     *
     * @param invalidObject the object of an unsupported type
     */
    @ParameterizedTest(name = "[{index}] Invalid type: {0}")
    @MethodSource("invalidTypeProvider")
    void toMap_invalidTypes_throwClassCastException(Object invalidObject) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DurationConversions.toMap(invalidObject, mockConverter), "Passing a non‑Duration object should cause ClassCastException");
    }
}
