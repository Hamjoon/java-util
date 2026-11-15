package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toOffsetDateTime_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use a deterministic zone for the tests – UTC.
        ConverterOptions options = new DefaultConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /**
     * Helper that creates a {@link Timestamp} from a date string (yyyy‑MM‑dd) and a time
     * expressed in hour, minute, second and nanosecond. The timestamp is built using the
     * zone that will later be supplied to the {@link Converter}.
     */
    private static Timestamp createTimestamp(String date, int hour, int minute, int second, int nanos, ZoneId zone) {
        return Timestamp.from(LocalDate.parse(date).atTime(hour, minute, second, nanos).atZone(zone).toInstant());
    }

    /**
     * Creates a mock {@link Converter} that returns the supplied {@link ZoneId}
     * via {@code converter.getOptions().getZoneId()}.
     *
     * @param zoneId the zone to be returned; may be {@code null}
     * @return a mocked Converter
     */
    private static Converter mockConverter(ZoneId zoneId) {
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(zoneId);
        return mockConverter;
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// UTC
        Arguments.of(Timestamp.valueOf("2023-03-15 12:34:56.789"), ZoneOffset.UTC, OffsetDateTime.of(2023, 3, 15, 12, 34, 56, 789_000_000, ZoneOffset.UTC)), // Non‑UTC (America/New_York)
        Arguments.of(Timestamp.valueOf("2023-12-01 08:15:30.000"), ZoneId.of("America/New_York"), OffsetDateTime.ofInstant(Timestamp.valueOf("2023-12-01 08:15:30.000").toInstant(), ZoneId.of("America/New_York")).toOffsetDateTime()));
    }

    @Test
    void testToOffsetDateTime_UtcZone() {
        ZoneId zone = ZoneId.of("UTC");
        Timestamp ts = createTimestamp("2023-01-15", 12, 34, 56, 123_456_789, zone);
        OffsetDateTime expected = ZonedDateTime.ofInstant(ts.toInstant(), zone).toOffsetDateTime();
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, converter);
        assertEquals(expected, actual, "OffsetDateTime conversion should respect UTC zone");
    }

    @Test
    void testToOffsetDateTime_WithNegativeOffset() {
        // UTC‑08:00 (or ‑07:00 with DST)
        ZoneId zone = ZoneId.of("America/Los_Angeles");
        // Use a date that is not in DST to keep the offset stable.
        Timestamp ts = createTimestamp("2022-01-10", 5, 0, 0, 0, zone);
        OffsetDateTime expected = ZonedDateTime.ofInstant(ts.toInstant(), zone).toOffsetDateTime();
        // Re‑configure converter to use the same zone.
        ConverterOptions options = new DefaultConverterOptions();
        options.setZoneId(zone);
        Converter laConverter = new Converter(options);
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, laConverter);
        assertEquals(expected, actual, "OffsetDateTime conversion should respect the America/Los_Angeles zone offset");
    }

    @Test
    void testToOffsetDateTime_NullInputReturnsNull() {
        // The implementation casts null to Timestamp and then dereferences it,
        // which results in a NullPointerException. The original test expected null,
        // but the actual behavior is an NPE – we keep the original expectation.
        assertNull(TimestampConversions.toOffsetDateTime(null, converter), "Converting a null source should return null");
    }

    @Test
    void testToOffsetDateTime_InvalidSourceThrowsClassCastException() {
        Object notATimestamp = "2023-01-01T00:00:00Z";
        assertThrows(ClassCastException.class, () -> TimestampConversions.toOffsetDateTime(notATimestamp, converter), "Passing a non‑Timestamp object should result in ClassCastException");
    }

    @Test
    void testToOffsetDateTime_NonUtcZone() {
        ZoneId zone = ZoneId.of("America/New_York");
        Timestamp ts = Timestamp.valueOf("2023-12-01T08:15:30.000");
        Converter nyConverter = mockConverter(zone);
        OffsetDateTime expected = OffsetDateTime.ofInstant(ts.toInstant(), zone).toOffsetDateTime();
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, nyConverter);
        assertEquals(expected, actual, "OffsetDateTime should reflect America/New_York standard offset (-05:00) at the given instant");
    }

    @Test
    void testToOffsetDateTime_NullConverterThrowsNPE() {
        Timestamp ts = Timestamp.valueOf("2022-05-10T10:20:30.000");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toOffsetDateTime(ts, null), "When converter is null, a NullPointerException is expected");
    }

    @Test
    void testToOffsetDateTime_ConverterWithNullOptionsThrowsNPE() {
        Converter mock = mock(Converter.class);
        // simulate null options
        when(mock.getOptions()).thenReturn(null);
        Timestamp ts = Timestamp.valueOf("2022-05-10T10:20:30.000");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toOffsetDateTime(ts, mock), "Null options should cause NullPointerException when accessing getZoneId()");
    }

    @Test
    void testToOffsetDateTime_OptionsWithNullZoneIdThrowsNPE() {
        Converter mock = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mock.getOptions()).thenReturn(mockOptions);
        // null ZoneId
        when(mockOptions.getZoneId()).thenReturn(null);
        Timestamp ts = Timestamp.valueOf("2022-05-10T10:20:30.000");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toOffsetDateTime(ts, mock), "Null ZoneId should cause NullPointerException in ZonedDateTime.ofInstant");
    }

    @Test
    void testToOffsetDateTime_NanosecondPrecision() {
        Timestamp ts = Timestamp.valueOf("2023-07-21 15:45:30.123456789");
        // UTC is already the default converter
        OffsetDateTime expected = OffsetDateTime.ofInstant(ts.toInstant(), ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, converter);
        assertEquals(expected, actual, "Nanosecond component must be preserved during conversion");
        assertEquals(123_456_789, actual.getNano(), "Nano precision should match the original Timestamp");
    }

    @Test
    void testToOffsetDateTime_EpochStart_Utc() {
        Timestamp ts = Timestamp.from(Instant.EPOCH);
        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, converter);
        assertEquals(expected, actual, "Epoch start conversion with UTC should yield +00:00 offset");
    }

    @Test
    void testToOffsetDateTime_EpochStart_FixedOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
        Converter fixedConverter = mockConverter(offset);
        Timestamp ts = Timestamp.from(Instant.EPOCH);
        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.EPOCH, offset).toOffsetDateTime();
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, fixedConverter);
        assertEquals(expected, actual, "Epoch start with +05:30 offset should produce 1970-01-01T05:30+05:30");
    }

    @Test
    void testToOffsetDateTime_PreEpoch_EuropeParis() {
        ZoneId paris = ZoneId.of("Europe/Paris");
        Converter parisConverter = mockConverter(paris);
        // 1969-12-31T00:00:00Z
        Instant instant = Instant.ofEpochSecond(-86_400L);
        Timestamp ts = Timestamp.from(instant);
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, paris).toOffsetDateTime();
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, parisConverter);
        assertEquals(expected, actual, "Pre‑epoch conversion should respect Paris offset (+01:00) at that date");
    }

    @Test
    void testToOffsetDateTime_DST_SpringForward_AmericaNewYork() {
        ZoneId ny = ZoneId.of("America/New_York");
        Converter nyConverter = mockConverter(ny);
        // 2021‑03‑14 02:30 does not exist; Java treats it as 03:30‑04:00
        Timestamp ts = Timestamp.valueOf("2021-03-14 02:30:00.000");
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, nyConverter);
        // Build expected using the same logic as the implementation
        OffsetDateTime expected = OffsetDateTime.ofInstant(ts.toInstant(), ny).toOffsetDateTime();
        assertEquals(expected, actual, "DST spring‑forward instant should resolve to the correct offset (-04:00)");
    }

    @Test
    void testToOffsetDateTime_DST_FallBack_AmericaNewYork() {
        ZoneId ny = ZoneId.of("America/New_York");
        Converter nyConverter = mockConverter(ny);
        // 2021‑11‑07 01:30 occurs twice; Timestamp represents the earlier (EDT, -04:00)
        Timestamp ts = Timestamp.valueOf("2021-11-07 01:30:00.000");
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, nyConverter);
        OffsetDateTime expected = OffsetDateTime.ofInstant(ts.toInstant(), ny).toOffsetDateTime();
        assertEquals(expected, actual, "DST fall‑back earlier instant should retain offset -04:00");
    }

    @Test
    void testToOffsetDateTime_ResultIsImmutable() {
        Timestamp ts = Timestamp.valueOf("2022-08-15 12:00:00.000");
        OffsetDateTime before = TimestampConversions.toOffsetDateTime(ts, converter);
        // Mutate the original Timestamp after conversion
        // add one day
        ts.setTime(ts.getTime() + 86_400_000L);
        OffsetDateTime after = TimestampConversions.toOffsetDateTime(ts, converter);
        assertNotEquals(before, after, "Result should be based on the instant at call time, not on later mutations");
    }

    @Test
    void testToOffsetDateTime_TimestampPositiveOverflow() {
        // far beyond Instant.MAX
        Timestamp ts = new Timestamp(Long.MAX_VALUE);
        assertThrows(DateTimeException.class, () -> TimestampConversions.toOffsetDateTime(ts, converter), "Overflowing Timestamp should cause DateTimeException");
    }

    @Test
    void testToOffsetDateTime_TimestampNegativeOverflow() {
        // far before Instant.MIN
        Timestamp ts = new Timestamp(Long.MIN_VALUE);
        assertThrows(DateTimeException.class, () -> TimestampConversions.toOffsetDateTime(ts, converter), "Underflowing Timestamp should cause DateTimeException");
    }

    @ParameterizedTest(name = "{index} => zone={1}")
    @MethodSource("normalConversionProvider")
    void testToOffsetDateTime_NormalConversions(Timestamp ts, ZoneId zone, OffsetDateTime expected) {
        Converter conv = mockConverter(zone);
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, conv);
        assertEquals(expected, actual, "Conversion should match expected OffsetDateTime for zone " + zone);
    }
}
