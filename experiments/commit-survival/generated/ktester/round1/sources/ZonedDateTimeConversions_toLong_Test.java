package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.time.*;
import java.util.Objects;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toLong_Test {

    @Mock
    private Converter // not used by toLong, but required by signature
    converter;

    /**
     * Supplies all test cases described in the JSON document.
     *
     * @return a stream of arguments: source object, converter, expected result (Long or Exception class), description
     */
    private static Stream<Arguments> provideToLongCases() {
        Converter dummyConverter = new Converter() {

            @Override
            public Object convert(Object source, Class<?> target) {
                // never invoked
                return null;
            }
        };
        // Helper to create a ZonedDateTime with a null internal ZoneId via reflection.
        // This is used for the "null ZoneId" scenario that cannot be built through the public API.
        ZonedDateTime zdtWithNullZone = null;
        try {
            ZonedDateTime valid = ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC"));
            Field zoneField = ZonedDateTime.class.getDeclaredField("zone");
            zoneField.setAccessible(true);
            // corrupt the internal state
            zoneField.set(valid, null);
            zdtWithNullZone = valid;
        } catch (Exception e) {
            // If reflection fails, the related test will be skipped (null will cause NPE anyway)
        }
        return Stream.of(// -----------------------------------------------------------------
        // Standard conversions
        // -----------------------------------------------------------------
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), null, 1684139400000L, "Standard conversion: UTC ZonedDateTime to epoch milli"), Arguments.of(ZonedDateTime.of(2023, 5, 15, 12, 30, 0, 0, ZoneId.of("Europe/Paris")), null, 1684135800000L, "Non‑UTC zone: method must use the underlying instant (UTC)"), Arguments.of(ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC")), null, 0L, "Epoch start: should return 0"), // -----------------------------------------------------------------
        // Null source
        // -----------------------------------------------------------------
        Arguments.of(null, null, NullPointerException.class, "Null input: casting null to ZonedDateTime should throw NPE"), // -----------------------------------------------------------------
        // Invalid type (String)
        // -----------------------------------------------------------------
        Arguments.of("2023-05-15T10:30:00Z", null, ClassCastException.class, "Invalid type: passing a String should result in ClassCastException"), // -----------------------------------------------------------------
        // Sub‑millisecond precision (nanoseconds are ignored)
        // -----------------------------------------------------------------
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 123_456_789, ZoneId.of("UTC")), null, 1684139400000L, "Nanosecond part is truncated – same epoch milli as without nanos"), // -----------------------------------------------------------------
        // Far‑future date
        // -----------------------------------------------------------------
        Arguments.of(ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), null, 32546880000000L, "Far‑future date conversion"), // -----------------------------------------------------------------
        // Historical date (pre‑epoch)
        // -----------------------------------------------------------------
        Arguments.of(ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), null, -2208988800000L, "Historical date before epoch yields negative epoch milli"), // -----------------------------------------------------------------
        // DST fallback (ambiguous local time)
        // -----------------------------------------------------------------
        Arguments.of(ZonedDateTime.of(2021, 11, 7, 1, 30, 0, 0, ZoneId.of("America/New_York")), null, 1636263000000L, "DST fallback – earlier offset (EDT) should be used"), // -----------------------------------------------------------------
        // Non‑null Converter argument (method should ignore it)
        // -----------------------------------------------------------------
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), dummyConverter, 1684139400000L, "Method ignores provided Converter"), // -----------------------------------------------------------------
        // Unrelated object type – should cause ClassCastException
        // -----------------------------------------------------------------
        Arguments.of(new Object(), null, ClassCastException.class, "Passing an unrelated Object should cause ClassCastException"), // -----------------------------------------------------------------
        // ZonedDateTime with internal null ZoneId (simulated via reflection)
        // -----------------------------------------------------------------
        Arguments.of(zdtWithNullZone, null, NullPointerException.class, "ZonedDateTime with null internal ZoneId should trigger NPE when toInstant() is called"), // -----------------------------------------------------------------
        // Converter that throws RuntimeException – should be ignored
        // -----------------------------------------------------------------
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), new Converter() {

            @Override
            public Object convert(Object source, Class<?> target) {
                throw new RuntimeException("converter failure");
            }
        }, 1684139400000L, "Even if Converter throws, toLong must ignore it and succeed"));
    }

    @Test
    void toLong_givenZonedDateTime_returnsCorrectEpochMilli() {
        // GIVEN
        long expectedMillis = 123_456_789L;
        Instant instant = Instant.ofEpochMilli(expectedMillis);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
        // WHEN
        long actual = ZonedDateTimeConversions.toLong(zdt, converter);
        // THEN
        assertEquals(expectedMillis, actual, "The epoch milli returned should match the ZonedDateTime's instant");
    }

    @Test
    void toLong_givenZonedDateTimeWithDifferentZone_returnsSameEpochMilli() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2021, 5, 4, 12, 30, 0, 0, ZoneId.of("America/New_York"));
        long expected = zdt.toInstant().toEpochMilli();
        // WHEN
        long actual = ZonedDateTimeConversions.toLong(zdt, converter);
        // THEN
        assertEquals(expected, actual, "Epoch milli should be based on the instant, independent of the zone");
    }

    @Test
    void toLong_givenNullObject_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLong(nullObj, converter), "Calling toLong with a null source should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideToLongCases")
    void toLong_parameterised(Object from, Converter conv, Object expected, String description) {
        if (expected instanceof Class) {
            // Expected an exception
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> excClass = (Class<? extends Throwable>) expected;
            assertThrows(excClass, () -> ZonedDateTimeConversions.toLong(from, conv), description);
        } else {
            // Expected a long value
            long actual = ZonedDateTimeConversions.toLong(from, conv);
            assertEquals(((Long) expected).longValue(), actual, description);
        }
    }
}
