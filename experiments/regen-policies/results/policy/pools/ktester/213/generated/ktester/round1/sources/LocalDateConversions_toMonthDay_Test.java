package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

class LocalDateConversions_toMonthDay_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter is required by the method signature, even though the implementation does not use it.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test
        converter = null;
    }

    private static Stream<Arguments> validMonthDayProvider() {
        Converter realConverter = new Converter(new ConverterOptions());
        Converter mockConverter = Mockito.mock(Converter.class);
        return Stream.of(// Typical case
        Arguments.of(LocalDate.of(2023, 5, 17), MonthDay.of(5, 17), realConverter), // Leap‑day
        Arguments.of(LocalDate.of(2020, 2, 29), MonthDay.of(2, 29), realConverter), // End‑of‑year
        Arguments.of(LocalDate.of(2023, 12, 31), MonthDay.of(12, 31), realConverter), // Minimum supported year (the LocalDate class allows a very large negative year)
        Arguments.of(LocalDate.of(-999_999_999, 1, 1), MonthDay.of(1, 1), realConverter), // Maximum supported year
        Arguments.of(LocalDate.of(999_999_999, 12, 31), MonthDay.of(12, 31), realConverter), // Month with 30 days
        Arguments.of(LocalDate.of(2021, 4, 30), MonthDay.of(4, 30), realConverter), // Month with 31 days
        Arguments.of(LocalDate.of(2021, 7, 31), MonthDay.of(7, 31), realConverter), // Non‑null Converter (mock, no interaction expected)
        Arguments.of(LocalDate.of(2023, 5, 17), MonthDay.of(5, 17), mockConverter), // Null Converter (allowed by signature)
        Arguments.of(LocalDate.of(2023, 5, 17), MonthDay.of(5, 17), null));
    }

    private static Stream<Arguments> exceptionMonthDayProvider() {
        Converter realConverter = new Converter(new ConverterOptions());
        Converter mockConverter = Mockito.mock(Converter.class);
        return Stream.of(// Null source, any converter (real)
        Arguments.of(null, NullPointerException.class, realConverter), // Null source, mock converter
        Arguments.of(null, NullPointerException.class, mockConverter), // Invalid type: String
        Arguments.of("2023-05-17", ClassCastException.class, realConverter), // Wrong temporal type: LocalDateTime
        Arguments.of(LocalDateTime.of(2023, 5, 17, 12, 30), ClassCastException.class, realConverter), // Source already a MonthDay
        Arguments.of(MonthDay.of(5, 17), ClassCastException.class, realConverter), // SQL Date
        Arguments.of(java.sql.Date.valueOf("2023-05-17"), ClassCastException.class, realConverter), // java.util.Date
        Arguments.of(Date.from(Instant.parse("2023-05-17T00:00:00Z")), ClassCastException.class, realConverter), // LocalTime
        Arguments.of(LocalTime.of(12, 30), ClassCastException.class, realConverter), // Instant
        Arguments.of(Instant.parse("2023-05-17T00:00:00Z"), ClassCastException.class, realConverter), // YearMonth
        Arguments.of(YearMonth.of(2023, 5), ClassCastException.class, realConverter), // Year
        Arguments.of(Year.of(2023), ClassCastException.class, realConverter), // ZonedDateTime
        Arguments.of(ZonedDateTime.of(2023, 5, 17, 12, 30, 0, 0, ZoneOffset.UTC), ClassCastException.class, realConverter), // OffsetDateTime
        Arguments.of(OffsetDateTime.of(2023, 5, 17, 12, 30, 0, 0, ZoneOffset.UTC), ClassCastException.class, realConverter), // Calendar
        Arguments.of(Calendar.getInstance(), ClassCastException.class, realConverter), // Generic Map
        Arguments.of(new LinkedHashMap<>(), ClassCastException.class, realConverter), // Primitive array
        Arguments.of(new int[] { 1, 2, 3 }, ClassCastException.class, realConverter));
    }

    @Test
    void toMonthDay_WithValidLocalDate_ReturnsCorrectMonthDay() {
        // given
        // 17 May 2023
        LocalDate date = LocalDate.of(2023, 5, 17);
        // when
        MonthDay result = LocalDateConversions.toMonthDay(date, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(MonthDay.of(5, 17), result, "MonthDay should match the month and day of the source LocalDate");
    }

    @Test
    void toMonthDay_WithNonLocalDateObject_ThrowsClassCastException() {
        // given
        Object notALocalDate = "2023-05-17";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toMonthDay(notALocalDate, converter), "Casting a non‑LocalDate object should result in ClassCastException");
    }

    @Test
    void toMonthDay_WithNullObject_ThrowsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toMonthDay(nullObj, converter), "Passing null should cause a NullPointerException when MonthDay.from is invoked");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={2} => expected={1}")
    @MethodSource("validMonthDayProvider")
    void toMonthDay_ValidInputs_ReturnsExpectedMonthDay(Object from, MonthDay expected, Converter conv) {
        // when
        MonthDay result = LocalDateConversions.toMonthDay(from, conv);
        // then
        assertEquals(expected, result, () -> "Expected MonthDay " + expected + " but got " + result + " for input " + from);
    }

    @ParameterizedTest(name = "[{index}] from={0} => throws {1}")
    @MethodSource("exceptionMonthDayProvider")
    void toMonthDay_InvalidInputs_ThrowsExpectedException(Object from, Class<? extends Throwable> expectedException, Converter conv) {
        // when / then
        assertThrows(expectedException, () -> LocalDateConversions.toMonthDay(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
