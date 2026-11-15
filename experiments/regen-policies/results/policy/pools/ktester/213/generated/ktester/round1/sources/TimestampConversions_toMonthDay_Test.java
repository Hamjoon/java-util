package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toMonthDay_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // A real Converter is required because the method accesses
        // converter.getOptions().getZoneId().
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that builds a {@link Timestamp} from a date string and time components.
     */
    private static Timestamp createTimestamp(String date, int hour, int minute, int second, int nano) {
        return Timestamp.from(LocalDate.parse(date).atTime(hour, minute, second, nano).atZone(ZoneId.systemDefault()).toInstant());
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Basic UTC conversion
        Arguments.of("Basic UTC conversion", createTimestamp("2023-05-15 10:20:30"), ZoneId.of("UTC"), MonthDay.of(5, 15)), // Different zone offset (+02:00) causing date roll‑over
        Arguments.of("Zone +02:00 shifts 2023‑12‑31T23:00Z to 2024‑01‑01", createTimestamp(Instant.parse("2023-12-31T23:00:00Z")), ZoneId.of("+02:00"), MonthDay.of(1, 1)), // Leap day handling
        Arguments.of("Leap day 2020‑02‑29 in UTC", createTimestamp("2020-02-29 12:00:00"), ZoneId.of("UTC"), MonthDay.of(2, 29)));
    }

    @Test
    void testToMonthDay() {
        // Classic dates covering different months and edge cases.
        assertEquals(MonthDay.of(1, 2), TimestampConversions.toMonthDay(createTimestamp("1888-01-02", 3, 45, 20, 222_222_222), converter));
        assertEquals(MonthDay.of(12, 31), TimestampConversions.toMonthDay(createTimestamp("1969-12-31", 14, 25, 35, 444_444_444), converter));
        assertEquals(MonthDay.of(1, 1), TimestampConversions.toMonthDay(createTimestamp("1970-01-01", 8, 50, 40, 666_666_666), converter));
        assertEquals(MonthDay.of(6, 15), TimestampConversions.toMonthDay(createTimestamp("2023-06-15", 17, 10, 5, 999_999_999), converter));
    }

    @Test
    void testToMonthDayWithNullTimestamp() {
        // The method casts the first argument to Timestamp; passing null should
        // result in a NullPointerException (the same behaviour as the original
        // implementation).
        assertThrows(NullPointerException.class, () -> TimestampConversions.toMonthDay(null, converter));
    }

    @Test
    void testToMonthDayWithWrongType() {
        // Passing an object that is not a Timestamp should cause a ClassCastException.
        assertThrows(ClassCastException.class, () -> TimestampConversions.toMonthDay("not a timestamp", converter));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void testValidConversions(String description, Timestamp from, ZoneId zoneId, MonthDay expected) {
        // Build a real Converter with the supplied ZoneId.
        ConverterOptions options = new DefaultConverterOptions();
        options.setZoneId(zoneId);
        Converter converter = new Converter(options);
        MonthDay result = TimestampConversions.toMonthDay(from, converter);
        assertEquals(expected, result, description);
    }

    @Test
    void testNullFromArgument() {
        Converter converter = new Converter(new DefaultConverterOptions());
        assertThrows(NullPointerException.class, () -> TimestampConversions.toMonthDay(null, converter), "Null 'from' should cause NullPointerException");
    }

    @Test
    void testNonTimestampFromArgument() {
        Converter converter = new Converter(new DefaultConverterOptions());
        assertThrows(ClassCastException.class, () -> TimestampConversions.toMonthDay(new java.util.Date(), converter), "Non‑Timestamp 'from' should cause ClassCastException");
    }

    @Test
    void testNullConverterArgument() {
        Timestamp ts = createTimestamp("2023-01-01 00:00:00");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toMonthDay(ts, null), "Null converter should cause NullPointerException");
    }

    @Test
    void testConverterWithNullOptions() {
        // Mock a Converter that returns null for getOptions()
        Converter mockConverter = Mockito.mock(Converter.class);
        Mockito.when(mockConverter.getOptions()).thenReturn(null);
        Timestamp ts = createTimestamp("2023-01-01 00:00:00");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toMonthDay(ts, mockConverter), "Converter with null options should cause NullPointerException");
    }

    @Test
    void testConverterWithNullZoneId() {
        // Mock ConverterOptions to return null ZoneId
        ConverterOptions mockOptions = Mockito.mock(ConverterOptions.class);
        Mockito.when(mockOptions.getZoneId()).thenReturn(null);
        // Mock Converter to return the mocked options
        Converter mockConverter = Mockito.mock(Converter.class);
        Mockito.when(mockConverter.getOptions()).thenReturn(mockOptions);
        Timestamp ts = createTimestamp("2023-01-01 00:00:00");
        assertThrows(NullPointerException.class, () -> TimestampConversions.toMonthDay(ts, mockConverter), "Converter with null ZoneId should cause NullPointerException");
    }
}
