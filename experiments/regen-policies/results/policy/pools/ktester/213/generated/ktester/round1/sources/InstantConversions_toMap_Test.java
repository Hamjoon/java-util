package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toMap_Test {

    /**
     * Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * Sample instant used in tests.
     */
    private static final Instant SAMPLE_INSTANT = Instant.parse("2023-01-01T12:34:56Z");

    /**
     * A Converter instance – the method under test does not use it, but it must be non‑null.
     */
    private static final Converter CONVERTER = new Converter(new ConverterOptions());

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; default options are sufficient for this test.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * Parameter source for successful conversion cases.
     *
     * Each element is an {@code Object[]} where:
     * <ul>
     *   <li>[0] – the {@code Instant} to convert</li>
     *   <li>[1] – the expected ISO‑8601 string representation</li>
     * </ul>
     */
    private static Stream<Object[]> successfulConversionProvider() {
        return Stream.of(new Object[] { Instant.ofEpochSecond(0), "1970-01-01T00:00:00Z" }, new Object[] { Instant.parse("2023-08-15T12:34:56.789Z"), "2023-08-15T12:34:56.789Z" });
    }

    /**
     * Parameter source for invalid type cases.
     *
     * Each element is an {@code Object[]} where:
     * <ul>
     *   <li>[0] – the invalid input object</li>
     * </ul>
     */
    private static Stream<Object[]> invalidTypeProvider() {
        return Stream.of(new Object[] { "not an instant" }, new Object[] { java.time.LocalDate.now() }, new Object[] { 12345L });
    }

    @Test
    void toMap_returnsMapContainingInstantString() {
        // GIVEN
        Object from = SAMPLE_INSTANT;
        // WHEN
        Map<String, Object> result = InstantConversions.toMap(from, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(SAMPLE_INSTANT.toString(), result.get(MapConversions.INSTANT), "Map entry value should be the ISO‑8601 string representation of the instant");
    }

    @Test
    void toMap_withNullInstant_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toMap(from, converter), "Calling toMap with a null Instant should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] toMap should produce map with instant ''{1}''")
    @MethodSource("successfulConversionProvider")
    void toMap_successfulConversion(Instant from, String expectedInstantString) {
        // GIVEN: an Instant instance; the converter argument is deliberately passed as null
        // to verify that the implementation truly ignores it.
        Object input = from;
        // WHEN
        Map<String, Object> result = InstantConversions.toMap(input, null);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map must contain exactly one entry");
        assertEquals(expectedInstantString, result.get(MapConversions.INSTANT), "Map entry must contain the ISO‑8601 string of the instant");
    }

    @Test
    void toMap_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toMap(from, CONVERTER), "Calling toMap with a null source should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] toMap with invalid type ''{0}'' should throw ClassCastException")
    @MethodSource("invalidTypeProvider")
    void toMap_invalidType_throwsClassCastException(Object invalidInput) {
        // GIVEN: an object that is not an Instant
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> InstantConversions.toMap(invalidInput, CONVERTER), "Casting a non‑Instant to Instant should result in ClassCastException");
    }
}
