package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toYearMonth_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter used by TimestampConversions.  Default options are sufficient.
        converter = new Converter(new DefaultConverterOptions());
        // a fresh Converter for each test; the zone will be overridden as needed.
        converter = new Converter(new DefaultConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference – helps the GC in long test suites.
        converter = null;
        converter = null;
    }

    /**
     * Helper that creates a {@link Timestamp} from a date string and time components.
     */
    private static Timestamp createTimestamp(String dateStr, int hour, int minute, int second, int nanos) {
        return Timestamp.from(LocalDate.parse(dateStr).atTime(hour, minute, second, nanos).atZone(ZoneId.systemDefault()).toInstant());
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Standard conversion – UTC
        Arguments.of("Standard conversion where the Timestamp represents a date in May 2023 and the converter uses UTC.", Timestamp.valueOf("2023-05-15 10:20:30"), ZoneId.of("UTC"), YearMonth.of(2023, 5)), // Instant with America/Los_Angeles (UTC‑08) shifts month back
        Arguments.of("Instant 2023‑03‑01T00:00:00Z converted to America/Los_Angeles yields 2023‑02‑28.", Timestamp.from(Instant.parse("2023-03-01T00:00:00Z")), ZoneId.of("America/Los_Angeles"), YearMonth.of(2023, 2)), // Time‑zone boundary forward month (UTC+02)
        Arguments.of("Timestamp at 2023‑01‑31 23:30:00 UTC becomes 2023‑02‑01 in UTC+02, month rolls forward.", Timestamp.valueOf("2023-01-31 23:30:00"), ZoneId.of("UTC+02:00"), YearMonth.of(2023, 2)), // Leap‑year February
        Arguments.of("Leap‑day 2020‑02‑29 in UTC stays in February.", Timestamp.valueOf("2020-02-29 12:00:00"), ZoneId.of("UTC"), YearMonth.of(2020, 2)), // Epoch with negative offset (UTC‑01)
        Arguments.of("Epoch instant shifted to UTC‑01 becomes 1969‑12‑31, month = 1969‑12.", Timestamp.from(Instant.EPOCH), ZoneId.of("UTC-01:00"), YearMonth.of(1969, 12)), // Maximum supported year
        Arguments.of("Timestamp at the very end of year 9999 in UTC.", Timestamp.valueOf("9999-12-31 23:59:59"), ZoneId.of("UTC"), YearMonth.of(9999, 12)));
    }

    @Test
    void testToYearMonth() {
        // Classic dates covering different years and months.
        assertEquals(YearMonth.of(1888, 1), TimestampConversions.toYearMonth(createTimestamp("1888-01-02", 12, 30, 45, 123_456_789), converter));
        assertEquals(YearMonth.of(1969, 12), TimestampConversions.toYearMonth(createTimestamp("1969-12-31", 23, 59, 59, 999_999_999), converter));
        assertEquals(YearMonth.of(1970, 1), TimestampConversions.toYearMonth(createTimestamp("1970-01-01", 0, 0, 1, 1), converter));
        assertEquals(YearMonth.of(2023, 6), TimestampConversions.toYearMonth(createTimestamp("2023-06-15", 15, 30, 0, 500_000_000), converter));
    }

    @Test
    void testToYearMonthWithDifferentZoneId() {
        // Verify that the zone supplied via ConverterOptions is honoured.
        ConverterOptions options = new DefaultConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        Converter utcConverter = new Converter(options);
        // The same instant expressed in UTC should still map to the same YearMonth.
        Timestamp ts = createTimestamp("2022-12-31", 23, 0, 0, 0);
        assertEquals(YearMonth.of(2022, 12), TimestampConversions.toYearMonth(ts, utcConverter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void testValidConversion(String description, Timestamp ts, ZoneId zone, YearMonth expected) {
        // configure the converter's zone
        converter.getOptions().setZoneId(zone);
        assertEquals(expected, TimestampConversions.toYearMonth(ts, converter), description);
    }

    @Test
    void testInvalidSourceType() {
        // Passing a java.util.Date should cause a ClassCastException because the method casts to Timestamp.
        java.util.Date notATimestamp = new java.util.Date();
        converter.getOptions().setZoneId(ZoneId.of("UTC"));
        assertThrows(ClassCastException.class, () -> TimestampConversions.toYearMonth(notATimestamp, converter), "Providing a non‑Timestamp source must throw ClassCastException");
    }

    @Test
    void testNullSourceObject() {
        converter.getOptions().setZoneId(ZoneId.of("UTC"));
        assertThrows(NullPointerException.class, () -> TimestampConversions.toYearMonth(null, converter), "Null 'from' argument should trigger NullPointerException");
    }

    @Test
    void testNullConverterArgument() {
        Timestamp ts = Timestamp.valueOf("2023-05-15 10:20:30");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toYearMonth(ts, null), "Null Converter argument should trigger NullPointerException");
    }
}
