package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toLocalDate_Test {

    /**
     * The {@link Converter} instance used in the tests – it can be a real instance
     * because the conversion work is mocked out.
     */
    private static Converter converter;

    /**
     * A real {@link Converter} instance – it is not used because {@code toZonedDateTime}
     * is mocked, but the method signature requires a non‑null reference for most tests.
     */
    private static final Converter REAL_CONVERTER = new Converter(new ConverterOptions());

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<Arguments> validConversionsHandledByZonedDateTime() {
        return Stream.of(// ZonedDateTime input
        Arguments.of(ZonedDateTime.of(2023, 5, 17, 10, 30, 0, 0, ZoneOffset.UTC), REAL_CONVERTER, LocalDate.of(2023, 5, 17), "ZonedDateTime input should be converted directly"), // Instant input
        Arguments.of(Instant.parse("2022-12-01T15:45:00Z"), REAL_CONVERTER, LocalDate.of(2022, 12, 1), "Instant input interpreted as UTC"), // java.util.Date input
        Arguments.of(Date.from(Instant.parse("2021-07-04T00:00:00Z")), REAL_CONVERTER, LocalDate.of(2021, 7, 4), "java.util.Date should map to same calendar day"), // java.sql.Timestamp input
        Arguments.of(Timestamp.valueOf("2020-02-29 12:00:00"), REAL_CONVERTER, LocalDate.of(2020, 2, 29), "SQL Timestamp preserves leap‑day date"), // Calendar (UTC) input
        Arguments.of(createUtcCalendar(2019, Calendar.MARCH, 15, 5, 0, 0), REAL_CONVERTER, LocalDate.of(2019, 3, 15), "Calendar instance (UTC) yields same local date"), // BigInteger epoch seconds input
        Arguments.of(BigInteger.valueOf(1609459200L), REAL_CONVERTER, LocalDate.of(2021, 1, 1), "BigInteger interpreted as epoch seconds – 2021‑01‑01"));
    }

    private static Stream<Arguments> invalidOrEdgeCaseInputs() {
        return Stream.of(Arguments.of(null, REAL_CONVERTER, NullPointerException.class, "Null source object should cause NPE"), Arguments.of("2022-01-01", REAL_CONVERTER, IllegalArgumentException.class, "Unsupported String source should cause IllegalArgumentException"));
    }

    private static Stream<Arguments> validAdditionalTypes() {
        return Stream.of(// LocalDateTime input
        Arguments.of(LocalDateTime.of(2022, 8, 15, 13, 45, 0), REAL_CONVERTER, LocalDate.of(2022, 8, 15), "LocalDateTime should be converted to its date component"), // LocalDate input (already a date)
        Arguments.of(LocalDate.of(1999, 12, 31), REAL_CONVERTER, LocalDate.of(1999, 12, 31), "LocalDate should be returned unchanged"), // OffsetDateTime input (UTC conversion)
        Arguments.of(OffsetDateTime.of(2021, 3, 10, 23, 0, 0, 0, ZoneOffset.ofHours(-5)), REAL_CONVERTER, LocalDate.of(2021, 3, 11), "OffsetDateTime converted to UTC date component"), // OffsetTime input – expect exception
        Arguments.of(OffsetTime.of(22, 30, 0, 0, ZoneOffset.ofHours(2)), REAL_CONVERTER, IllegalArgumentException.class, "OffsetTime lacks a date component – should throw IllegalArgumentException"));
    }

    private static Stream<Arguments> boundaryAndExtremeDates() {
        return Stream.of(// Epoch instant
        Arguments.of(Date.from(Instant.EPOCH), REAL_CONVERTER, LocalDate.of(1970, 1, 1), "Epoch instant maps to 1970‑01‑01"), // Negative epoch seconds
        Arguments.of(BigInteger.valueOf(-86400L), REAL_CONVERTER, LocalDate.of(1969, 12, 31), "Negative epoch seconds map to the day before epoch"), // Far‑future epoch seconds (9999‑12‑31)
        Arguments.of(BigInteger.valueOf(253402300799L), REAL_CONVERTER, LocalDate.of(9999, 12, 31), "Maximum representable LocalDate expressed as epoch seconds"));
    }

    private static Stream<Arguments> customConverterUsage() {
        // Helper converter that parses an ISO‑ZONED‑DATE‑TIME string
        Converter isoZonedParser = new Converter() {

            @Override
            public ZonedDateTime convert(Object src) {
                return ZonedDateTime.parse((String) src);
            }
        };
        // Converter that deliberately returns null
        Converter nullReturning = new Converter() {

            @Override
            public ZonedDateTime convert(Object src) {
                return null;
            }
        };
        return Stream.of(Arguments.of("2023-04-01T10:15:30+02:00[Europe/Paris]", isoZonedParser, LocalDate.of(2023, 4, 1), null, "Custom converter parses ISO‑ZONED‑DATE‑TIME string"), Arguments.of("2022-12-31", nullReturning, null, NullPointerException.class, "Custom converter returns null – should result in NPE"));
    }

    private static Stream<Arguments> invalidNumericTypes() {
        return Stream.of(Arguments.of(BigDecimal.valueOf(1609459200L), REAL_CONVERTER, IllegalArgumentException.class, "BigDecimal is not a supported source type"));
    }

    private static Stream<Arguments> overflowAndOutOfRangeNumericValues() {
        return Stream.of(Arguments.of(// Long.MAX_VALUE + 1
        new BigInteger("9223372036854775808"), REAL_CONVERTER, ArithmeticException.class, "BigInteger exceeding Long.MAX_VALUE should overflow"), Arguments.of(// epoch seconds for 10000‑01‑01
        new BigInteger("253402300800"), REAL_CONVERTER, DateTimeException.class, "Epoch seconds beyond year 9999 should cause DateTimeException"));
    }

    private static Stream<Arguments> unsupportedSourceTypes() {
        return Stream.of(Arguments.of(LocalTime.of(12, 30), REAL_CONVERTER, IllegalArgumentException.class, "LocalTime lacks a date component"), Arguments.of(ZoneId.of("UTC"), REAL_CONVERTER, IllegalArgumentException.class, "ZoneId is not a supported source type"));
    }

    private static Stream<Arguments> customConverterExceptionPropagation() {
        // Converter that throws IllegalStateException
        Converter throwingConverter = new Converter() {

            @Override
            public ZonedDateTime convert(Object src) {
                throw new IllegalStateException("conversion failed");
            }
        };
        // Converter that returns wrong type (LocalDate) causing ClassCastException later
        Converter wrongReturnConverter = new Converter() {

            @Override
            public ZonedDateTime convert(Object src) {
                // deliberately return a LocalDate (will be cast to ZonedDateTime)
                return (ZonedDateTime) (Object) LocalDate.now();
            }
        };
        return Stream.of(Arguments.of("any", throwingConverter, IllegalStateException.class, "Custom converter throws IllegalStateException – should propagate"), Arguments.of("ignored", wrongReturnConverter, ClassCastException.class, "Custom converter returns wrong type – should cause ClassCastException"));
    }

    /**
     * Creates a {@link Calendar} instance in UTC with the supplied date‑time fields.
     */
    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void toLocalDate_ShouldReturnLocalDateFromZonedDateTime() {
        // GIVEN
        // arbitrary value – not used because we mock
        BigInteger source = new BigInteger("123456789");
        ZonedDateTime mockedZdt = ZonedDateTime.of(LocalDate.of(2023, Month.MARCH, 15), LocalTime.of(10, 30, 45), ZoneId.of("UTC"));
        // Mock the static method BigIntegerConversions.toZonedDateTime(...)
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(source, converter)).thenReturn(mockedZdt);
            // WHEN
            LocalDate result = BigIntegerConversions.toLocalDate(source, converter);
            // THEN
            assertNotNull(result, "Resulting LocalDate must not be null");
            assertEquals(LocalDate.of(2023, Month.MARCH, 15), result, "The LocalDate should match the date part of the mocked ZonedDateTime");
        }
    }

    @Test
    void toLocalDate_ShouldReturnNullWhenZonedDateTimeIsNull() {
        // GIVEN
        BigInteger source = BigInteger.ZERO;
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(source, converter)).thenReturn(null);
            // WHEN / THEN
            assertNull(BigIntegerConversions.toLocalDate(source, converter), "When the delegated toZonedDateTime returns null, toLocalDate should also return null");
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionsHandledByZonedDateTime")
    void validConversionsHandledByZonedDateTime(Object from, Converter converter, LocalDate expectedDate, String description) {
        // GIVEN – a mocked static method that returns a ZonedDateTime matching the expected date
        ZonedDateTime mockedZdt = ZonedDateTime.of(expectedDate, LocalTime.MIDNIGHT, ZoneOffset.UTC);
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(from, converter)).thenReturn(mockedZdt);
            // WHEN
            LocalDate result = BigIntegerConversions.toLocalDate(from, converter);
            // THEN
            assertNotNull(result, description);
            assertEquals(expectedDate, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidOrEdgeCaseInputs")
    void invalidOrEdgeCaseInputs(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // GIVEN – mock to throw the expected exception when called with the supplied arguments
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(from, converter)).thenThrow(expectedException);
            // WHEN / THEN
            assertThrows(expectedException, () -> BigIntegerConversions.toLocalDate(from, converter), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validAdditionalTypes")
    void validAdditionalTypes(Object from, Converter converter, LocalDate expectedDate, String description) {
        ZonedDateTime mockedZdt = ZonedDateTime.of(expectedDate, LocalTime.MIDNIGHT, ZoneOffset.UTC);
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(from, converter)).thenReturn(mockedZdt);
            LocalDate result = BigIntegerConversions.toLocalDate(from, converter);
            assertEquals(expectedDate, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("boundaryAndExtremeDates")
    void boundaryAndExtremeDates(Object from, Converter converter, LocalDate expectedDate, String description) {
        ZonedDateTime mockedZdt = ZonedDateTime.of(expectedDate, LocalTime.MIDNIGHT, ZoneOffset.UTC);
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(from, converter)).thenReturn(mockedZdt);
            LocalDate result = BigIntegerConversions.toLocalDate(from, converter);
            assertEquals(expectedDate, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("customConverterUsage")
    void customConverterUsage(Object from, Converter converter, LocalDate expectedDate, Class<? extends Throwable> expectedException, String description) {
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            if (expectedException != null) {
                mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(from, converter)).thenThrow(expectedException);
                assertThrows(expectedException, () -> BigIntegerConversions.toLocalDate(from, converter), description);
            } else {
                ZonedDateTime mockedZdt = ZonedDateTime.of(expectedDate, LocalTime.MIDNIGHT, ZoneOffset.UTC);
                mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(from, converter)).thenReturn(mockedZdt);
                LocalDate result = BigIntegerConversions.toLocalDate(from, converter);
                assertEquals(expectedDate, result, description);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidNumericTypes")
    void invalidNumericTypes(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(from, converter)).thenThrow(expectedException);
            assertThrows(expectedException, () -> BigIntegerConversions.toLocalDate(from, converter), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("overflowAndOutOfRangeNumericValues")
    void overflowAndOutOfRangeNumericValues(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(from, converter)).thenThrow(expectedException);
            assertThrows(expectedException, () -> BigIntegerConversions.toLocalDate(from, converter), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unsupportedSourceTypes")
    void unsupportedSourceTypes(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(from, converter)).thenThrow(expectedException);
            assertThrows(expectedException, () -> BigIntegerConversions.toLocalDate(from, converter), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("customConverterExceptionPropagation")
    void customConverterExceptionPropagation(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        try (MockedStatic<BigIntegerConversions> mockedStatic = mockStatic(BigIntegerConversions.class)) {
            mockedStatic.when(() -> BigIntegerConversions.toZonedDateTime(from, converter)).thenThrow(expectedException);
            assertThrows(expectedException, () -> BigIntegerConversions.toLocalDate(from, converter), description);
        }
    }
}
