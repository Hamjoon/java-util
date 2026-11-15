package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toMap_Test {

    /**
     * Converter is not used by the method under test, but we provide a mock to satisfy the signature.
     */
    private static Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a Mockito mock once for all tests.
        mockConverter = mock(Converter.class);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static mock reference.
        mockConverter = null;
    }

    /**
     * Supplies the test data for {@link #toMap_successfulConversion}.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// 2023‑01‑01T12:34:56.789Z (UTC)
        Arguments.of("UTC ZonedDateTime with nanoseconds", ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000, ZoneId.of("UTC")), "2023-01-01", "12:34:56.789", "UTC"), // 2022‑12‑31T23:59:59‑05:00[America/New_York] (no nanos)
        Arguments.of("Non‑UTC zone without nanoseconds", ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York")), "2022-12-31", "23:59:59", "America/New_York"), // Leap day with full nanosecond precision
        Arguments.of("Leap day with full nanoseconds in Europe/Paris", ZonedDateTime.of(LocalDate.of(2000, 2, 29), LocalTime.of(0, 0, 0, 123_456_789), ZoneId.of("Europe/Paris")), "2000-02-29", "00:00:00.123456789", "Europe/Paris"));
    }

    /**
     * Supplies objects that are not instances of {@link ZonedDateTime}.
     */
    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("String instance", "not a ZonedDateTime"), Arguments.of("Integer instance", 42), Arguments.of("LocalDate instance", LocalDate.now()));
    }

    @Test
    @DisplayName("toMap should return a map containing DATE, TIME and ZONE strings")
    void toMap_returnsCorrectMap() {
        // given: a known ZonedDateTime instance
        ZonedDateTime zdt = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nano
        15, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        0, ZoneId.of("America/New_York"));
        // when: converting to a map
        Map<String, Object> result = ZonedDateTimeConversions.toMap(zdt, mockConverter);
        // then: map contains the expected string representations
        assertEquals("2023-05-10", result.get(DATE), "DATE value is incorrect");
        assertEquals("15:30:45", result.get(TIME), "TIME value is incorrect");
        assertEquals("America/New_York", result.get(ZONE), "ZONE value is incorrect");
        // also verify the map size is exactly 3
        assertEquals(3, result.size(), "Result map should contain exactly three entries");
    }

    @Test
    @DisplayName("toMap should throw NullPointerException when source is null")
    void toMap_nullSource_throwsException() {
        // given: a null source object
        Object nullSource = null;
        // when / then: a NullPointerException is expected because the method casts without null‑check
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toMap(nullSource, mockConverter));
    }

    /**
     * Verifies that a {@link ZonedDateTime} is converted to a map containing the expected
     * {@code DATE}, {@code TIME} and {@code ZONE} string values.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("successfulConversionProvider")
    void toMap_successfulConversion(String description, ZonedDateTime input, String expectedDate, String expectedTime, String expectedZone) {
        // when: converting to a map (converter argument is irrelevant, we pass the mock)
        Map<String, Object> result = ZonedDateTimeConversions.toMap(input, mockConverter);
        // then: map contains exactly the three expected entries
        assertEquals(3, result.size(), "Result map should contain exactly three entries");
        assertEquals(expectedDate, result.get(DATE), "DATE value is incorrect");
        assertEquals(expectedTime, result.get(TIME), "TIME value is incorrect");
        assertEquals(expectedZone, result.get(ZONE), "ZONE value is incorrect");
    }

    @Test
    @DisplayName("toMap should throw NullPointerException when source is null")
    void toMap_nullFrom_throwsException() {
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toMap(null, mockConverter));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidTypeProvider")
    void toMap_invalidType(String description, Object invalidInput) {
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toMap(invalidInput, mockConverter), "Expected ClassCastException for input: " + description);
    }
}
