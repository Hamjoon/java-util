package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Date;
import java.util.stream.Stream;

class TimeZoneConversions_toMap_Test {

    private static Converter converter;

    /* ------------------------------------------------------------
     *  Helper class for parameterized test arguments
     * ------------------------------------------------------------ */
    private static final class ArgumentsHolder {

        final TimeZone timeZone;

        final String expectedId;

        final String description;

        ArgumentsHolder(TimeZone timeZone, String expectedId, String description) {
            this.timeZone = timeZone;
            this.expectedId = expectedId;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    /* ------------------------------------------------------------
     *  Parameterized tests for the “Valid TimeZone conversion” group
     * ------------------------------------------------------------ */
    private static Stream<ArgumentsHolder> validTimeZoneProvider() {
        return Stream.of(new ArgumentsHolder(TimeZone.getTimeZone("UTC"), "UTC", "Pass a standard UTC TimeZone instance; the method should return a map containing the ZONE key with value \"UTC\"."), new ArgumentsHolder(TimeZone.getTimeZone("America/New_York"), "America/New_York", "Pass a non‑UTC TimeZone; the map should contain the ZONE key with the exact ID of the supplied TimeZone."));
    }

    @Test
    void toMap_returnsMapContainingZoneId() {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        // WHEN
        Map<String, Object> result = TimeZoneConversions.toMap(tz, converter);
        // THEN
        assertNotNull(result, "Result map must not be null");
        assertTrue(result instanceof CompactMap, "Result should be a CompactMap (or at least a Map)");
        assertEquals(1, result.size(), "Result map should contain exactly one entry");
        assertEquals(tz.getID(), result.get(MapConversions.ZONE), "Map entry for ZONE must match the TimeZone ID");
    }

    @Test
    void toMap_handlesNullTimeZone() {
        // GIVEN
        TimeZone tz = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimeZoneConversions.toMap(tz, converter), "Calling toMap with a null TimeZone should throw NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validTimeZoneProvider")
    void toMap_returnsMapWithCorrectZone(ArgumentsHolder holder) {
        // GIVEN
        TimeZone tz = holder.timeZone;
        // WHEN
        // converter is not used by the method
        Map<String, Object> result = TimeZoneConversions.toMap(tz, null);
        // THEN
        assertNotNull(result, "Result map must not be null");
        // The implementation uses CompactMap, but we only require it to be a Map.
        assertTrue(result instanceof Map, "Result should be a Map");
        assertEquals(1, result.size(), "Result map should contain exactly one entry");
        assertEquals(holder.expectedId, result.get(MapConversions.ZONE), "Map entry for ZONE must match the expected TimeZone ID");
    }

    @Test
    void toMap_rejectsInvalidArgumentType() {
        // GIVEN
        Object notATimeZone = "not a timezone";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimeZoneConversions.toMap(notATimeZone, null), "Passing a non‑TimeZone object should result in ClassCastException");
    }

    @Test
    void toMap_propagatesExceptionFromFailingGetID() {
        // GIVEN: a TimeZone subclass whose getID() throws IllegalStateException
        TimeZone badTZ = new TimeZone() {

            @Override
            public String getID() {
                throw new IllegalStateException("forced failure");
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
        };
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> TimeZoneConversions.toMap(badTZ, null), "The IllegalStateException from getID() should propagate");
        assertEquals("forced failure", ex.getMessage(), "Exception message should match the one thrown from getID()");
    }
}
