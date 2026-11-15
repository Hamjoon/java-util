package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.TimeZone;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.ZoneId;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimeZoneConversions_toMap_Test {

    /**
     * The Converter is not used by the method under test, but a mock is supplied to satisfy the signature.
     */
    @Mock
    private Converter mockConverter;

    private TimeZone utcTimeZone;

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a deterministic TimeZone instance.
        utcTimeZone = TimeZone.getTimeZone("UTC");
        // Mockito will initialise mockConverter automatically because of @Mock.
    }

    /**
     * Provides a stream of successful test cases.
     * Each argument consists of:
     *   1. a {@link TimeZone} instance (or subclass)
     *   2. the expected value stored under the {@code ZONE} key (may be {@code null})
     *   3. a flag indicating whether the test should use a non‑null {@link Converter}
     */
    private static Stream<Arguments> successfulCases() {
        return Stream.of(// standard IDs
        Arguments.of(TimeZone.getTimeZone("UTC"), "UTC", false), Arguments.of(TimeZone.getTimeZone("America/New_York"), "America/New_York", false), Arguments.of(TimeZone.getTimeZone("GMT+02:00"), "GMT+02:00", false), // anonymous subclass with overridden getID()
        Arguments.of(customTimeZone("customID"), "customID", false), // unknown zone falls back to GMT
        Arguments.of(TimeZone.getTimeZone("Invalid/Zone"), "GMT", false), // empty string also resolves to GMT
        Arguments.of(TimeZone.getTimeZone(""), "GMT", false), // anonymous subclass where getID() returns null
        Arguments.of(customTimeZone(null), null, false), // GMT offset strings
        Arguments.of(TimeZone.getTimeZone("GMT-05:00"), "GMT-05:00", false), Arguments.of(TimeZone.getTimeZone("GMT+00:00"), "GMT+00:00", false));
    }

    /**
     * Provides inputs that should cause the method to throw an exception.
     * Each argument consists of:
     *   1. the source object passed as {@code from}
     *   2. the expected exception type
     */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// null source – NPE from casting
        Arguments.of(null, NullPointerException.class), // wrong type – ClassCastException from casting to TimeZone
        Arguments.of("not a TimeZone", ClassCastException.class));
    }

    /**
     * Creates an anonymous {@link TimeZone} subclass with a configurable {@code getID()} result.
     *
     * @param id the value that {@code getID()} should return (may be {@code null})
     * @return a concrete {@code TimeZone} instance
     */
    private static TimeZone customTimeZone(String id) {
        return new TimeZone() {

            @Override
            public String getID() {
                return id;
            }

            @Override
            public void setID(String ID) {
                // no‑op for test stub
            }

            @Override
            public int getRawOffset() {
                return 0;
            }

            @Override
            public void setRawOffset(int offsetMillis) {
                // no‑op for test stub
            }

            @Override
            public boolean useDaylightTime() {
                return false;
            }

            @Override
            public boolean inDaylightTime(java.util.Date date) {
                return false;
            }

            @Override
            public int getDSTSavings() {
                return 0;
            }
        };
    }

    @Test
    void toMap_returnsMapContainingZoneId() {
        // GIVEN
        // (utcTimeZone and mockConverter are already prepared)
        // WHEN
        Map<String, Object> result = TimeZoneConversions.toMap(utcTimeZone, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertTrue(result.containsKey(ZONE), "Map must contain the key defined by MapConversions.ZONE");
        assertEquals(utcTimeZone.getID(), result.get(ZONE), "The value associated with ZONE must be the TimeZone ID");
    }

    @Test
    void toMap_nullTimeZone_throwsNullPointerException() {
        // GIVEN
        TimeZone nullTz = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimeZoneConversions.toMap(nullTz, mockConverter), "Passing a null TimeZone should result in a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] toMap({0}) → {1}")
    @MethodSource("successfulCases")
    void toMap_successful(TimeZone from, String expectedZone, boolean useConverter) {
        // Arrange
        Converter converter = useConverter ? mock(Converter.class) : null;
        // Act
        Map<String, Object> result = TimeZoneConversions.toMap(from, converter);
        // Assert
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Result map should contain exactly one entry");
        assertTrue(result.containsKey(ZONE), "Result map must contain the ZONE key");
        assertEquals(expectedZone, result.get(ZONE), "Unexpected value for ZONE key");
    }

    @Test
    void toMap_withNonNullConverter_ignoresConverter() {
        // GIVEN: a TimeZone created from a ZoneId and a real (mocked) Converter
        TimeZone tz = TimeZone.getTimeZone(ZoneId.of("Europe/Paris"));
        // WHEN
        Map<String, Object> result = TimeZoneConversions.toMap(tz, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals("Europe/Paris", result.get(ZONE));
    }

    @ParameterizedTest(name = "[{index}] toMap({0}) throws {1}")
    @MethodSource("exceptionCases")
    void toMap_throws(Object from, Class<? extends Throwable> expectedException) {
        // Act & Assert
        assertThrows(expectedException, () -> TimeZoneConversions.toMap(from, null), () -> "Expected " + expectedException.getSimpleName() + " when passing: " + from);
    }
}
