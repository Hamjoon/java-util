package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Date;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toSqlDate_Test {

    /**
     * The Converter instance required by the method signature.
     *  It is not used by {@code LocalDateConversions.toSqlDate}, so a mock is sufficient.
     */
    @Mock
    Converter mockConverter;

    private static Stream<Arguments> provideValidLocalDates() {
        return Stream.of(Arguments.of(LocalDate.of(2023, 5, 17), Date.valueOf(LocalDate.of(2023, 5, 17))), Arguments.of(LocalDate.of(1970, 1, 1), Date.valueOf(LocalDate.of(1970, 1, 1))), Arguments.of(LocalDate.of(9999, 12, 31), Date.valueOf(LocalDate.of(9999, 12, 31))));
    }

    private static Stream<Arguments> provideOutOfRangeLocalDates() {
        return Stream.of(// year too high
        Arguments.of(LocalDate.of(10000, 1, 1)), // year zero (not allowed)
        Arguments.of(LocalDate.of(0, 1, 1)), // negative year
        Arguments.of(LocalDate.of(-1, 1, 1)));
    }

    @Test
    void toSqlDate_givenValidLocalDate_returnsCorrespondingSqlDate() {
        // GIVEN
        LocalDate localDate = LocalDate.of(2023, 5, 10);
        // WHEN
        Date result = LocalDateConversions.toSqlDate(localDate, mockConverter);
        // THEN
        assertNotNull(result, "Resulting java.sql.Date should not be null");
        assertEquals(Date.valueOf(localDate), result, "The sql Date should represent the same calendar day as the input LocalDate");
    }

    @Test
    void toSqlDate_givenNullInput_throwsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toSqlDate(nullInput, mockConverter), "Calling toSqlDate with a null source should throw NPE because Date.valueOf(null) does");
    }

    @Test
    void toSqlDate_givenNonLocalDateInput_throwsClassCastException() {
        // GIVEN
        String notALocalDate = "2023-05-10";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toSqlDate(notALocalDate, mockConverter), "Passing a non‑LocalDate object should result in a ClassCastException");
    }

    /**
     * Same as the previous test but supplies a non‑null {@link Converter}
     * (the mock) to confirm the argument is ignored.
     */
    @ParameterizedTest(name = "[{index}] toSqlDate({0}) with non‑null converter")
    @MethodSource("provideValidLocalDates")
    void toSqlDate_givenValidLocalDate_andNonNullConverter_ignoresConverter(LocalDate input, Date expected) {
        // WHEN
        Date result = LocalDateConversions.toSqlDate(input, mockConverter);
        // THEN
        assertEquals(expected, result, () -> "Conversion should be identical regardless of the Converter argument");
    }

    /**
     * {@code java.sql.Date.valueOf} rejects years outside the range 1‑9999.
     * This test confirms that {@code toSqlDate} propagates the same exception.
     */
    @ParameterizedTest(name = "[{index}] toSqlDate({0}) throws IllegalArgumentException")
    @MethodSource("provideOutOfRangeLocalDates")
    void toSqlDate_givenOutOfRangeYear_throwsIllegalArgumentException(LocalDate outOfRange) {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toSqlDate(outOfRange, mockConverter), () -> "Year " + outOfRange.getYear() + " should be rejected by Date.valueOf");
    }
}
