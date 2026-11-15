package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZoneOffsetConversions_toTimeZone_Test {

    @Mock
    private Converter // Converter is not used by the method, but required by signature
    mockConverter;

    /* --------------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final String expectedId;

        final String description;

        ArgumentsHolder(Object from, Converter converter, String expectedId, String description) {
            this.from = from;
            this.converter = converter;
            this.expectedId = expectedId;
            this.description = description;
        }

        @Override
        public String toString() {
            return description + " => expected ID: " + expectedId;
        }
    }

    private static Stream<ArgumentsHolder> provideOffsets() {
        return Stream.of(// UTC
        new ArgumentsHolder(ZoneOffset.UTC, null, "GMT", "When the ZoneOffset is UTC, the method should return a TimeZone whose ID is exactly \"GMT\"."), // Simple positive hour
        new ArgumentsHolder(ZoneOffset.ofHours(2), null, "GMT+02:00", "Positive offset: verify that a ZoneOffset of +02:00 produces a TimeZone with ID \"GMT+02:00\"."), // Negative with minutes
        new ArgumentsHolder(ZoneOffset.ofHoursMinutesSeconds(-5, 30, 0), null, "GMT-05:30", "Negative offset with minutes: ensure that -05:30 is formatted correctly as \"GMT-05:30\"."), // Zero hour, non‑zero minutes
        new ArgumentsHolder(ZoneOffset.ofHoursMinutesSeconds(0, 45, 0), null, "GMT+00:45", "Zero hour offset with non‑zero minutes should produce \"GMT+00:45\" (not \"GMT\")."), // Offset with seconds
        new ArgumentsHolder(ZoneOffset.ofHoursMinutesSeconds(0, 0, 30), null, "GMT+00:00:30", "Offset includes seconds; verify that the full ID string from ZoneOffset.getId() is used (e.g., \"GMT+00:00:30\")."), // Maximum positive (+18:00)
        new ArgumentsHolder(ZoneOffset.ofHours(18), null, "GMT+18:00", "Maximum positive ZoneOffset (+18:00) – verify the method builds the correct GMT ID at the upper bound of ZoneOffset."), // Maximum negative (-18:00)
        new ArgumentsHolder(ZoneOffset.ofHours(-18), null, "GMT-18:00", "Maximum negative ZoneOffset (-18:00) – verify the method builds the correct GMT ID at the lower bound of ZoneOffset."), // Positive with hours, minutes, seconds
        new ArgumentsHolder(ZoneOffset.ofHoursMinutesSeconds(1, 15, 30), null, "GMT+01:15:30", "Positive offset that includes hours, minutes, and seconds – ensure the full offset string from ZoneOffset.getId() is used."), // Negative with hours, minutes, seconds
        new ArgumentsHolder(ZoneOffset.ofHoursMinutesSeconds(-2, 45, 15), null, "GMT-02:45:15", "Negative offset that includes hours, minutes, and seconds – verify correct sign and formatting."), // Positive offset with a non‑null mock converter (should be ignored)
        new ArgumentsHolder(ZoneOffset.ofHours(5), mockConverter, "GMT+05:00", "Pass a non‑null Converter (mock) to confirm the method ignores the converter argument and behaves identically to a null converter."));
    }

    @Test
    void toTimeZone_Utc_ReturnsGMT() {
        // GIVEN
        ZoneOffset utc = ZoneOffset.UTC;
        // WHEN
        TimeZone tz = ZoneOffsetConversions.toTimeZone(utc, mockConverter);
        // THEN
        assertNotNull(tz, "Returned TimeZone should not be null");
        assertEquals("GMT", tz.getID(), "UTC offset should map to \"GMT\"");
    }

    @Test
    void toTimeZone_PositiveOffset_ReturnsCorrectId() {
        // GIVEN
        // +05:30
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
        // WHEN
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, mockConverter);
        // THEN
        assertNotNull(tz);
        assertEquals("GMT+05:30", tz.getID(), "Positive offset should be prefixed with \"GMT\" and retain the sign and format");
    }

    @Test
    void toTimeZone_NegativeOffset_ReturnsCorrectId() {
        // GIVEN
        // -04:15
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-4, -15);
        // WHEN
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, mockConverter);
        // THEN
        assertNotNull(tz);
        assertEquals("GMT-04:15", tz.getID(), "Negative offset should be prefixed with \"GMT\" and retain the sign and format");
    }

    @Test
    void toTimeZone_NonZoneOffset_ThrowsClassCastException() {
        // GIVEN
        Object notAnOffset = "not a ZoneOffset";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toTimeZone(notAnOffset, mockConverter), "Providing a non‑ZoneOffset object should result in a ClassCastException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideOffsets")
    void toTimeZone_VariousOffsets_ReturnsExpectedId(ArgumentsHolder holder) {
        // GIVEN
        Object from = holder.from;
        Converter converter = holder.converter;
        // WHEN
        TimeZone tz = ZoneOffsetConversions.toTimeZone(from, converter);
        // THEN
        assertNotNull(tz, "TimeZone must not be null");
        assertEquals(holder.expectedId, tz.getID(), holder.description);
    }

    @Test
    void toTimeZone_NullFrom_ThrowsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        // The cast to ZoneOffset yields null; the subsequent call to offset.equals(...)
        // throws NullPointerException.
        assertThrows(NullPointerException.class, () -> ZoneOffsetConversions.toTimeZone(from, mockConverter), "Passing null for the 'from' argument should cause a NullPointerException");
    }

    @Test
    void toTimeZone_WrongTypeString_ThrowsClassCastException() {
        // GIVEN
        // String, not a ZoneOffset
        Object from = "+02:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toTimeZone(from, mockConverter), "Passing a String instead of a ZoneOffset should result in ClassCastException");
    }

    @Test
    void toTimeZone_WrongObject_ThrowsClassCastException() {
        // GIVEN
        Object from = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZoneOffsetConversions.toTimeZone(from, mockConverter), "Passing an arbitrary Object should trigger a ClassCastException");
    }
}
