package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.time.chrono.JapaneseDate;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toYearMonth_Test {

    /**
     * Converter instance – not used by the method but required by the signature.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance; use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> provideValidLocalDates() {
        return Stream.of(// Standard conversion
        Arguments.of(LocalDate.of(2023, 5, 15), YearMonth.of(2023, 5)), // Edge of year – last day
        Arguments.of(LocalDate.of(1999, 12, 31), YearMonth.of(1999, 12)), // First day of a (leap) year
        Arguments.of(LocalDate.of(2000, 1, 1), YearMonth.of(2000, 1)), // Leap day – day component is ignored
        Arguments.of(LocalDate.of(2024, 2, 29), YearMonth.of(2024, 2)), // Minimum representable LocalDate
        Arguments.of(LocalDate.MIN, YearMonth.from(LocalDate.MIN)), // Maximum representable LocalDate
        Arguments.of(LocalDate.MAX, YearMonth.from(LocalDate.MAX)));
    }

    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(// Null source – triggers NPE inside YearMonth.from(null)
        Arguments.of(null, NullPointerException.class), // Various non‑LocalDate types – all should cause ClassCastException
        Arguments.of("2023-05-15", ClassCastException.class), Arguments.of(java.sql.Date.valueOf("2023-12-01"), ClassCastException.class), Arguments.of(new Date(), ClassCastException.class), Arguments.of(YearMonth.of(2023, 5), ClassCastException.class), Arguments.of(LocalDateTime.now(), ClassCastException.class), Arguments.of(Instant.now(), ClassCastException.class), Arguments.of(ZonedDateTime.now(), ClassCastException.class), Arguments.of(JapaneseDate.from(LocalDate.of(2023, 5, 15)), ClassCastException.class), Arguments.of(MonthDay.of(5, 15), ClassCastException.class), Arguments.of(Integer.valueOf(42), ClassCastException.class));
    }

    @Test
    void toYearMonth_givenValidLocalDate_returnsCorrespondingYearMonth() {
        // given
        LocalDate source = LocalDate.of(2023, 5, 17);
        YearMonth expected = YearMonth.of(2023, 5);
        // when
        YearMonth result = LocalDateConversions.toYearMonth(source, converter);
        // then
        assertEquals(expected, result, "YearMonth should be derived from the supplied LocalDate");
    }

    @Test
    void toYearMonth_givenNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toYearMonth(source, converter), "Passing null should result in NullPointerException because YearMonth.from(null) is invoked");
    }

    @Test
    void toYearMonth_givenNonLocalDate_throwsClassCastException() {
        // given
        // a String, not a LocalDate
        Object source = "2023-05-17";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toYearMonth(source, converter), "A source that cannot be cast to LocalDate should cause ClassCastException");
    }

    /**
     * Parameterized test that verifies the method throws the expected exception
     * for invalid inputs (null or non‑{@link LocalDate} objects).
     */
    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("provideInvalidInputs")
    void toYearMonth_givenInvalidInput_throwsExpectedException(Object from, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> LocalDateConversions.toYearMonth(from, converter), () -> "Calling toYearMonth with " + (from == null ? "null" : from.getClass().getSimpleName()) + " should throw " + expectedException.getSimpleName());
    }
}
