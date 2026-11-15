package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toMap_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * Provides a stream of arguments for successful {@code toMap} conversions.
     *
     * Each argument consists of:
     * <ul>
     *   <li>the {@link LocalTime} input</li>
     *   <li>a {@link Converter} (may be {@code null} or a real instance)</li>
     *   <li>the expected string value stored under {@link MapConversions#LOCAL_TIME}</li>
     *   <li>a description used only for readability of the test source</li>
     * </ul>
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Normal conversion – nanoseconds present
        Arguments.of(LocalTime.of(13, 45, 30, 123_456_789), null, "13:45:30.123456789", "A valid LocalTime instance is converted to a map with the ISO‑8601 string"), // Midnight without nanoseconds
        Arguments.of(LocalTime.of(0, 0), null, "00:00", "Edge case: midnight (00:00) with no nanoseconds"), // Maximum representable time with full nanosecond precision
        Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), null, "23:59:59.999999999", "Edge case: maximum time of day with full nanosecond precision"), // Time with trailing zeros in the nanosecond fraction – LocalTime.toString() trims them
        Arguments.of(LocalTime.of(9, 5, 0, 123_000_000), null, "09:05:00.123", "Trailing zeros are trimmed by LocalTime.toString()"), // Non‑null Converter (implementation ignores it)
        Arguments.of(LocalTime.of(12, 30, 15, 0), new Converter(new ConverterOptions()), "12:30:15", "Conversion with a non‑null Converter instance"));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null `from` argument – should throw NullPointerException
        Arguments.of(null, null, NullPointerException.class, "Passing a null `from` argument should cause NPE"), // Wrong type for `from` – should throw ClassCastException
        Arguments.of("12:00", null, ClassCastException.class, "Passing a non‑LocalTime object (String) should cause ClassCastException"));
    }

    @Test
    void toMap_convertsLocalTimeToMapWithCorrectKeyAndValue() {
        // GIVEN
        LocalTime now = LocalTime.of(13, 45, 30, 123_456_789);
        // WHEN
        Map<String, Object> result = LocalTimeConversions.toMap(now, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.LOCAL_TIME), "Map should contain the key defined in MapConversions.LOCAL_TIME");
        assertEquals(now.toString(), result.get(MapConversions.LOCAL_TIME), "The value associated with the key should be the LocalTime string representation");
    }

    @Test
    void toMap_handlesNullInputGracefully() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        // The method casts the input to LocalTime, so a null will cause a NullPointerException.
        // This test documents the current behaviour.
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toMap(nullObj, converter));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successfulConversionProvider")
    void toMap_successfulConversions(LocalTime from, Converter conv, String expectedString, String _description) {
        // GIVEN
        // fallback to the static one if null
        Converter usedConverter = (conv != null) ? conv : converter;
        // WHEN
        Map<String, Object> result = LocalTimeConversions.toMap(from, usedConverter);
        // THEN
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Result map must contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.LOCAL_TIME), "Result map must contain the key defined in MapConversions.LOCAL_TIME");
        assertEquals(expectedString, result.get(MapConversions.LOCAL_TIME), "Map value must match the expected LocalTime string representation");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionProvider")
    void toMap_expectedExceptions(Object from, Converter conv, Class<? extends Throwable> expectedException, String _description) {
        // GIVEN
        Converter usedConverter = (conv != null) ? conv : converter;
        // WHEN / THEN
        assertThrows(expectedException, () -> LocalTimeConversions.toMap(from, usedConverter), "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
