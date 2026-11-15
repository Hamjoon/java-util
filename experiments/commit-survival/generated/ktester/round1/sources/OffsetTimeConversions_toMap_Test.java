package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toMap_Test {

    // shared for all tests
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor is sufficient for this test.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none needed here).
        converter = null;
    }

    /**
     * Supplies the successful test cases described in the JSON input.
     */
    private static Stream<Arguments> provideValidOffsetTimes() {
        return Stream.of(// Normal conversion with nanoseconds and a +02:30 offset
        Arguments.of(OffsetTime.of(13, 45, 30, 123_456_789, ZoneOffset.ofHoursMinutes(2, 30)), "13:45:30.123456789+02:30"), // Zero offset (UTC) – should render the short “Z” suffix
        Arguments.of(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), "00:00Z"), // Negative hour offset
        Arguments.of(OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(-5)), "23:59:59.999999999-05:00"), // Maximum allowed OffsetTime value
        Arguments.of(OffsetTime.MAX, "23:59:59.999999999+18:00"), // Minimum allowed OffsetTime value
        Arguments.of(OffsetTime.MIN, "00:00-18:00"));
    }

    @Test
    void toMap_returnsMapWithTimeKeyAndStringValue() {
        // GIVEN
        OffsetTime offsetTime = OffsetTime.of(LocalTime.of(13, 45, 30), ZoneOffset.ofHours(2));
        // WHEN
        Map<String, Object> result = OffsetTimeConversions.toMap(offsetTime, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertTrue(result.containsKey(MapConversions.TIME), "Map should contain the key defined by MapConversions.TIME");
        assertEquals(offsetTime.toString(), result.get(MapConversions.TIME), "The value associated with the TIME key must be the OffsetTime's string representation");
    }

    @Test
    void toMap_withNonOffsetTime_throwsClassCastException() {
        // GIVEN
        Object notAnOffsetTime = "I am not an OffsetTime";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toMap(notAnOffsetTime, converter), "Passing a non‑OffsetTime object should result in a ClassCastException");
    }

    /* ------------------------------------------------------------
     *  Successful conversion scenarios (parameterised)
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] OffsetTime={0} → expected=\"{1}\"")
    @MethodSource("provideValidOffsetTimes")
    void toMap_returnsMapWithTimeKeyAndExpectedString(OffsetTime offsetTime, String expectedString) {
        // WHEN
        // converter not used
        Map<String, Object> result = OffsetTimeConversions.toMap(offsetTime, null);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.TIME), "Map must contain the key defined by MapConversions.TIME");
        assertEquals(expectedString, result.get(MapConversions.TIME), "Map value must match the OffsetTime's string representation");
    }

    @Test
    void toMap_nullSource_throwsNullPointerException() {
        // The method casts null to OffsetTime and then calls toString(),
        // which results in a NullPointerException.
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toMap(null, null), "Passing a null source should throw NullPointerException");
    }

    @Test
    void toMap_invalidType_throwsClassCastException() {
        // Supplying a non‑OffsetTime object triggers the unchecked cast.
        Object notAnOffsetTime = "13:45";
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toMap(notAnOffsetTime, null), "Supplying an object of the wrong type must cause ClassCastException");
    }

    /* ------------------------------------------------------------
     *  Non‑null converter argument – method should ignore it.
     * ------------------------------------------------------------ */
    @Test
    void toMap_withNonNullConverter_behavesIdenticallyToNullConverter() {
        OffsetTime offsetTime = OffsetTime.of(13, 45, 30, 123_456_789, ZoneOffset.ofHoursMinutes(2, 30));
        // Use a stubbed Converter – the method does not consult it.
        Converter stubConverter = new Converter(new ConverterOptions());
        Map<String, Object> result = OffsetTimeConversions.toMap(offsetTime, stubConverter);
        assertNotNull(result);
        assertEquals(offsetTime.toString(), result.get(MapConversions.TIME), "The returned string must be identical regardless of the Converter argument");
    }
}
