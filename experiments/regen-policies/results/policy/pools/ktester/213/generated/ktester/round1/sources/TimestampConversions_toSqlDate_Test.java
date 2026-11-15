package com.cedarsoftware.util.convert;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toSqlDate_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // By default we use a deep‑stubbed Converter that returns the system default zone.
        converter = mock(Converter.class, RETURNS_DEEP_STUBS);
        when(converter.getOptions().getZoneId()).thenReturn(ZoneId.systemDefault());
    }

    /**
     * Helper to create a {@link Timestamp} from a date string (ISO‑8601) and a time component.
     */
    private static Timestamp createTimestamp(String isoDate, int hour, int minute, int second, int nanoOfSecond) {
        LocalDateTime ldt = LocalDate.parse(isoDate).atTime(hour, minute, second, nanoOfSecond);
        // Use the system default zone – the same zone the default ConverterOptions will use.
        return Timestamp.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Helper to create a {@link Timestamp} from an ISO‑8601 instant string.
     */
    private static Timestamp ts(String isoInstant) {
        return Timestamp.from(Instant.parse(isoInstant));
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// UTC – same day
        Arguments.of("UTC conversion – same calendar day", ts("2023-03-15T10:30:00Z"), ZoneId.of("UTC"), Date.valueOf(LocalDate.of(2023, 3, 15))), // New York (UTC‑5) – still same day
        Arguments.of("America/New_York conversion – same calendar day", ts("2023-03-15T23:30:00Z"), ZoneId.of("America/New_York"), Date.valueOf(LocalDate.of(2023, 3, 15))), // Kiritimati (UTC+14) – crosses to next day
        Arguments.of("Pacific/Kiritimati conversion – next calendar day", ts("2023-03-15T23:30:00Z"), ZoneId.of("Pacific/Kiritimati"), Date.valueOf(LocalDate.of(2023, 3, 16))));
    }

    private static Stream<Arguments> boundaryConversionProvider() {
        return Stream.of(// Epoch instant in UTC
        Arguments.of("Epoch instant UTC", ts("1970-01-01T00:00:00Z"), ZoneId.of("UTC"), Date.valueOf(LocalDate.of(1970, 1, 1))), // Maximum supported java.sql.Date (year 9999)
        Arguments.of("Maximum supported date (9999-12-31) UTC", ts("9999-12-31T23:59:59Z"), ZoneId.of("UTC"), Date.valueOf(LocalDate.of(9999, 12, 31))), // Leap‑day in Tokyo (UTC+9) – stays on same day
        Arguments.of("Leap day in Asia/Tokyo", ts("2020-02-29T12:00:00Z"), ZoneId.of("Asia/Tokyo"), Date.valueOf(LocalDate.of(2020, 2, 29))), // UTC instant 02:00 becomes previous day in Los_Angeles (UTC‑8)
        Arguments.of("Crossing back a day in America/Los_Angeles", ts("2023-01-01T02:00:00Z"), ZoneId.of("America/Los_Angeles"), Date.valueOf(LocalDate.of(2022, 12, 31))), // DST spring‑forward transition in Europe/Paris (CET/CEST)
        Arguments.of("DST transition in Europe/Paris (2023‑03‑26)", ts("2023-03-26T00:30:00Z"), ZoneId.of("Europe/Paris"), Date.valueOf(LocalDate.of(2023, 3, 26))), // Year‑ending instant crossing to next year in Kiritimati (UTC+14)
        Arguments.of("Year‑end crossing in Pacific/Kiritimati", ts("2023-12-31T23:00:00Z"), ZoneId.of("Pacific/Kiritimati"), Date.valueOf(LocalDate.of(2024, 1, 1))), // Most negative offset (UTC‑12) – previous calendar day
        Arguments.of("Most negative offset Etc/GMT+12", ts("2023-01-01T00:00:00Z"), ZoneId.of("Etc/GMT+12"), Date.valueOf(LocalDate.of(2022, 12, 31))));
    }

    @Test
    void testTimestampToSqlDate() {
        // 1888‑01‑02 12:30:45.123456789 → 1888‑01‑02
        Timestamp ts1 = createTimestamp("1888-01-02", 12, 30, 45, 123_456_789);
        Date expected1 = Date.valueOf(LocalDate.of(1888, 1, 2));
        assertEquals(expected1, TimestampConversions.toSqlDate(ts1, converter));
        // 1969‑12‑31 23:59:59.999999999 → 1969‑12‑31
        Timestamp ts2 = createTimestamp("1969-12-31", 23, 59, 59, 999_999_999);
        Date expected2 = Date.valueOf(LocalDate.of(1969, 12, 31));
        assertEquals(expected2, TimestampConversions.toSqlDate(ts2, converter));
        // 1970‑01‑01 00:00:00.000000001 → 1970‑01‑01
        Timestamp ts3 = createTimestamp("1970-01-01", 0, 0, 0, 1);
        Date expected3 = Date.valueOf(LocalDate.of(1970, 1, 1));
        assertEquals(expected3, TimestampConversions.toSqlDate(ts3, converter));
        // 2023‑06‑15 15:30:00.500000000 → 2023‑06‑15
        Timestamp ts4 = createTimestamp("2023-06-15", 15, 30, 0, 500_000_000);
        Date expected4 = Date.valueOf(LocalDate.of(2023, 6, 15));
        assertEquals(expected4, TimestampConversions.toSqlDate(ts4, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalConversionProvider")
    void testNormalConversions(String description, Timestamp ts, ZoneId zone, Date expected) {
        // arrange – mock converter to return the required zone
        when(converter.getOptions().getZoneId()).thenReturn(zone);
        // act
        Date actual = TimestampConversions.toSqlDate(ts, converter);
        // assert
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryConversionProvider")
    void testBoundaryConversions(String description, Timestamp ts, ZoneId zone, Date expected) {
        when(converter.getOptions().getZoneId()).thenReturn(zone);
        Date actual = TimestampConversions.toSqlDate(ts, converter);
        assertEquals(expected, actual, description);
    }

    @Test
    void testFromIsNull() {
        // from == null → NullPointerException when cast to Timestamp then call toInstant()
        assertThrows(NullPointerException.class, () -> TimestampConversions.toSqlDate(null, converter), "Expected NPE when 'from' is null");
    }

    @Test
    void testFromWrongType() {
        // Passing a java.util.Date instead of java.sql.Timestamp → ClassCastException
        java.util.Date wrong = java.util.Date.from(Instant.parse("2023-03-15T10:30:00Z"));
        assertThrows(ClassCastException.class, () -> TimestampConversions.toSqlDate(wrong, converter), "Expected CCE when 'from' is not a java.sql.Timestamp");
    }

    @Test
    void testConverterIsNull() {
        Timestamp ts = ts("2023-03-15T10:30:00Z");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toSqlDate(ts, null), "Expected NPE when converter is null");
    }

    @Test
    void testConverterOptionsIsNull() {
        Timestamp ts = ts("2023-03-15T10:30:00Z");
        Converter badConverter = mock(Converter.class);
        // options == null
        when(badConverter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> TimestampConversions.toSqlDate(ts, badConverter), "Expected NPE when converter.getOptions() returns null");
    }

    @Test
    void testConverterZoneIdIsNull() {
        Timestamp ts = ts("2023-03-15T10:30:00Z");
        Converter badConverter = mock(Converter.class, RETURNS_DEEP_STUBS);
        // zoneId == null
        when(badConverter.getOptions().getZoneId()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> TimestampConversions.toSqlDate(ts, badConverter), "Expected NPE when zoneId is null");
    }

    @Test
    void testDateOutOfRange() {
        // Year 10000 is outside java.sql.Date range → IllegalArgumentException
        Timestamp ts = ts("10000-01-01T00:00:00Z");
        // Use a valid zone to isolate the out‑of‑range error
        when(converter.getOptions().getZoneId()).thenReturn(ZoneId.of("UTC"));
        assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toSqlDate(ts, converter), "Expected IAE for dates beyond 9999‑12‑31");
    }
}
