package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimeZoneConversions_toMap_Test {

    @Mock
    private Converter // Converter is not used by the method, a mock is sufficient
    converter;

    private static Stream<Arguments> validTimeZoneProvider() {
        return Stream.of(Arguments.of("Standard UTC zone", TimeZone.getTimeZone("UTC"), "UTC"), Arguments.of("America/New_York zone", TimeZone.getTimeZone("America/New_York"), "America/New_York"), Arguments.of("Custom GMT offset (+05:30)", TimeZone.getTimeZone("GMT+05:30"), "GMT+05:30"), Arguments.of("GMT alias (treated as UTC)", TimeZone.getTimeZone("GMT"), "GMT"), Arguments.of("Less common identifier Etc/UTC", TimeZone.getTimeZone("Etc/UTC"), "Etc/UTC"), Arguments.of("Zero offset with negative sign", TimeZone.getTimeZone("GMT-00:00"), "GMT-00:00"));
    }

    private static Stream<Arguments> exceptionalInputProvider() {
        // Custom TimeZone subclass that returns null from getID()
        TimeZone nullIdTimeZone = new TimeZone() {

            @Override
            public String getID() {
                return null;
            }

            @Override
            public void setID(String ID) {
                // no‑op
            }

            @Override
            public int getRawOffset() {
                return 0;
            }

            @Override
            public void setRawOffset(int offsetMillis) {
                // no‑op
            }

            @Override
            public boolean useDaylightTime() {
                return false;
            }

            @Override
            public boolean inDaylightTime(Date date) {
                return false;
            }

            @Override
            public int getDSTSavings() {
                return 0;
            }

            @Override
            public boolean observesDaylightTime() {
                return false;
            }
        };
        return Stream.of(Arguments.of("Null TimeZone argument", null, NullPointerException.class), Arguments.of("Non‑TimeZone object", new Object(), ClassCastException.class), Arguments.of("Custom TimeZone with null ID", nullIdTimeZone, NullPointerException.class));
    }

    @Test
    @DisplayName("toMap returns a map containing the zone ID for a given TimeZone")
    void toMap_returnsMapWithZoneId() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        // WHEN
        Map<String, Object> result = TimeZoneConversions.toMap(tz, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Resulting map should contain exactly one entry");
        assertEquals(tz.getID(), result.get(ZONE), "Map should contain the correct zone ID under key ZONE");
    }

    @Test
    @DisplayName("toMap works with UTC time zone")
    void toMap_withUtcTimeZone() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("UTC");
        // WHEN
        Map<String, Object> result = TimeZoneConversions.toMap(tz, converter);
        // THEN
        assertEquals(tz.getID(), result.get(ZONE), "UTC zone ID should be stored correctly");
    }

    /* --------------------------------------------------------------
     *  Valid conversions – the method should return a map containing
     *  the zone ID under the key {@link MapConversions#ZONE}.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validTimeZoneProvider")
    @DisplayName("toMap returns a map with the correct ZONE value for valid TimeZone instances")
    void toMap_returnsMapWithCorrectZone(String description, TimeZone tz, String expectedZoneId) {
        // WHEN
        Map<String, Object> result = TimeZoneConversions.toMap(tz, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Resulting map should contain exactly one entry");
        assertEquals(expectedZoneId, result.get(ZONE), () -> "Map should contain the expected zone ID under key ZONE for " + description);
    }

    /* --------------------------------------------------------------
     *  Invalid / edge‑case inputs – the method is expected to throw
     *  either NullPointerException or ClassCastException.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionalInputProvider")
    @DisplayName("toMap throws the expected exception for invalid inputs")
    void toMap_throwsExpectedException(String description, Object input, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> TimeZoneConversions.toMap(input, converter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
