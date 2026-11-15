package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLocalDate_Test {

    /**
     * Converter used by all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a ConverterOptions instance; the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Direct ZonedDateTime conversion
        Arguments.of("ZonedDateTime.of(2023,5,15,10,30,0,0,ZoneId.of(\"UTC\"))", ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), LocalDate.of(2023, 5, 15)), // java.util.Date conversion (midnight UTC)
        Arguments.of("Date.from(Instant.parse(\"2022-12-01T00:00:00Z\"))", Date.from(Instant.parse("2022-12-01T00:00:00Z")), LocalDate.of(2022, 12, 1)), // ISO‑8601 zoned‑date‑time string
        Arguments.of("\"2021-07-20T15:45:30+02:00[Europe/Paris]\"", "2021-07-20T15:45:30+02:00[Europe/Paris]", LocalDate.of(2021, 7, 20)), // java.sql.Timestamp conversion
        Arguments.of("Timestamp.valueOf(\"2023-08-01 12:34:56\")", Timestamp.valueOf("2023-08-01 12:34:56"), LocalDate.of(2023, 8, 1)), // LocalDateTime conversion
        Arguments.of("LocalDateTime.of(2021,12,31,23,59)", LocalDateTime.of(2021, 12, 31, 23, 59), LocalDate.of(2021, 12, 31)), // LocalDate input
        Arguments.of("LocalDate.of(2020,2,29)", LocalDate.of(2020, 2, 29), LocalDate.of(2020, 2, 29)), // Instant conversion (UTC)
        Arguments.of("Instant.parse(\"2022-05-10T08:00:00Z\")", Instant.parse("2022-05-10T08:00:00Z"), LocalDate.of(2022, 5, 10)), // OffsetDateTime conversion
        Arguments.of("OffsetDateTime.of(2024,1,15,5,30,0,0,ZoneOffset.ofHours(-5))", OffsetDateTime.of(2024, 1, 15, 5, 30, 0, 0, ZoneOffset.ofHours(-5)), LocalDate.of(2024, 1, 15)), // ISO‑8601 date‑only string
        Arguments.of("\"2022-11-05\"", "2022-11-05", LocalDate.of(2022, 11, 5)));
    }

    private static Stream<Arguments> nullResultProvider() {
        return Stream.of(Arguments.of("Unsupported type Boolean.TRUE", Boolean.TRUE));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Completely malformed string
        Arguments.of("Malformed string \"invalid-date\"", "invalid-date"), // Empty string
        Arguments.of("Empty string \"\"", ""), // Non‑existent date (Feb 30)
        Arguments.of("Non‑existent date \"2021-02-30\"", "2021-02-30"));
    }

    @Test
    void toLocalDate_fromString_returnsExpectedDate() {
        // given
        String isoDate = "2023-05-10";
        // when
        LocalDate result = StringConversions.toLocalDate(isoDate, converter);
        // then
        assertEquals(LocalDate.of(2023, 5, 10), result);
    }

    @Test
    void toLocalDate_fromZonedDateTime_returnsSameLocalDate() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 31, 15, 0, 0, 0, ZoneId.of("UTC"));
        // when
        LocalDate result = StringConversions.toLocalDate(zdt, converter);
        // then
        assertEquals(LocalDate.of(2022, 12, 31), result);
    }

    @Test
    void toLocalDate_nullInput_returnsNull() {
        // given
        Object nullObj = null;
        // when
        LocalDate result = StringConversions.toLocalDate(nullObj, converter);
        // then
        assertNull(result);
    }

    @Test
    void toLocalDate_fromLocalDateTime_isConvertedCorrectly() {
        // given
        // LocalDateTime will be interpreted as a date‑time in the system default zone.
        String dateTimeStr = "2021-07-15T08:30:00";
        // when
        LocalDate result = StringConversions.toLocalDate(dateTimeStr, converter);
        // then
        assertEquals(LocalDate.of(2021, 7, 15), result);
    }

    @Test
    @DisplayName("Null handling – both source object and converter are null")
    void toLocalDate_nullObjectAndNullConverter_returnsNull() {
        // when
        LocalDate result = StringConversions.toLocalDate(null, null);
        // then
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    @DisplayName("Successful conversions to LocalDate")
    void toLocalDate_successfulConversions(String description, Object input, LocalDate expected) {
        // when
        // converter not required for these cases
        LocalDate result = StringConversions.toLocalDate(input, null);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullResultProvider")
    @DisplayName("Unsupported types result in null")
    void toLocalDate_unsupportedTypesReturnNull(String description, Object input) {
        // when
        LocalDate result = StringConversions.toLocalDate(input, null);
        // then
        assertNull(result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    @DisplayName("Invalid strings trigger DateTimeParseException")
    void toLocalDate_invalidStringsThrowException(String description, Object input) {
        // when / then
        assertThrows(DateTimeParseException.class, () -> StringConversions.toLocalDate(input, null), description);
    }
}
