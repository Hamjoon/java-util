package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.SimpleTimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimeZoneConversions_toString_Test {

    /**
     * The Converter is not used by the method under test, but a mock is supplied to satisfy the signature.
     */
    @Mock
    Converter converter;

    /**
     * A minimal {@link TimeZone} subclass whose {@code getID()} method throws a
     * {@link RuntimeException}.  All other abstract methods are implemented with
     * trivial behavior because they are not exercised by {@code TimeZoneConversions.toString}.
     */
    private static final class FaultyTimeZone extends TimeZone {

        @Override
        public String getID() {
            throw new RuntimeException("Intentional failure in getID()");
        }

        // The following methods are required abstract implementations.
        @Override
        public void setRawOffset(int offsetMillis) {
            /* no‑op */
        }

        @Override
        public int getRawOffset() {
            return 0;
        }

        @Override
        public int getOffset(long date) {
            return 0;
        }

        @Override
        public int getOffset(long era, int year, int month, int day, int dayOfWeek, int milliseconds) {
            return 0;
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
    }

    private static Stream<Arguments> validTimeZoneProvider() {
        return Stream.of(// Normal behavior – UTC
        Arguments.of(TimeZone.getTimeZone("UTC"), "UTC"), // Normal behavior – region based
        Arguments.of(TimeZone.getTimeZone("America/New_York"), "America/New_York"));
    }

    private static Stream<Arguments> variousTimeZoneProvider() {
        return Stream.of(// GMT offset string
        Arguments.of(TimeZone.getTimeZone("GMT+02:00"), "GMT+02:00"), // Standard region ID
        Arguments.of(TimeZone.getTimeZone("Europe/Paris"), "Europe/Paris"), // SimpleTimeZone with custom ID
        Arguments.of(new SimpleTimeZone(0, "CustomTZ"), "CustomTZ"), // Invalid zone falls back to GMT (TimeZone.getTimeZone returns GMT)
        Arguments.of(TimeZone.getTimeZone("Invalid/Zone"), "GMT"));
    }

    @Test
    void toStringReturnsTimeZoneId_ForStandardZone() {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        String result = TimeZoneConversions.toString(tz, converter);
        assertEquals("America/Los_Angeles", result);
    }

    @Test
    void toStringReturnsTimeZoneId_ForUTC() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        String result = TimeZoneConversions.toString(tz, converter);
        assertEquals("UTC", result);
    }

    @Test
    void toStringThrowsNullPointerException_WhenFromIsNull() {
        assertThrows(NullPointerException.class, () -> TimeZoneConversions.toString(null, converter));
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected=\"{1}\"")
    @MethodSource("validTimeZoneProvider")
    void toStringReturnsCorrectId_ForValidTimeZones(TimeZone tz, String expectedId) {
        // Converter is irrelevant – pass the mock (or null) – here we use the injected mock.
        String result = TimeZoneConversions.toString(tz, converter);
        assertEquals(expectedId, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected=\"{1}\"")
    @MethodSource("variousTimeZoneProvider")
    void toStringHandlesVariousIds(TimeZone tz, String expectedId) {
        String result = TimeZoneConversions.toString(tz, converter);
        assertEquals(expectedId, result);
    }

    @Test
    void toStringIgnoresNonNullConverter() {
        // Create a distinct mock for this test to emphasise that it is non‑null.
        Converter nonNullConverter = org.mockito.Mockito.mock(Converter.class);
        TimeZone tz = TimeZone.getTimeZone("GMT");
        String result = TimeZoneConversions.toString(tz, nonNullConverter);
        assertEquals("GMT", result);
    }

    @Test
    void toStringThrowsClassCastException_WhenFromIsNotTimeZone() {
        String notATimeZone = "just a string";
        assertThrows(ClassCastException.class, () -> TimeZoneConversions.toString(notATimeZone, converter));
    }

    @Test
    void toStringPropagatesRuntimeException_FromFaultyTimeZone() {
        TimeZone faulty = new FaultyTimeZone();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> TimeZoneConversions.toString(faulty, converter));
        assertEquals("Intentional failure in getID()", thrown.getMessage());
    }
}
