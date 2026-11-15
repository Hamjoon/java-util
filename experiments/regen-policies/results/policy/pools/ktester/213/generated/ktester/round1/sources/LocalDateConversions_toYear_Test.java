package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toYear_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    converter;

    private static Stream<org.junit.jupiter.params.provider.Arguments> normalBehaviorProvider() {
        return Stream.of(// typical date
        org.junit.jupiter.params.provider.Arguments.of(LocalDate.of(2023, 5, 17), Year.of(2023)), // maximum supported year (Year.MAX_VALUE == 999_999_999)
        org.junit.jupiter.params.provider.Arguments.of(LocalDate.of(Year.MAX_VALUE, 12, 31), Year.of(Year.MAX_VALUE)));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> boundaryYearProvider() {
        return Stream.of(// minimum supported year (Year.MIN_VALUE == -999_999_999)
        org.junit.jupiter.params.provider.Arguments.of(LocalDate.of(Year.MIN_VALUE, 1, 1), Year.of(Year.MIN_VALUE)), // epoch start date
        org.junit.jupiter.params.provider.Arguments.of(LocalDate.of(1970, 1, 1), Year.of(1970)));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> exceptionProvider() {
        return Stream.of(// null source – Year.from(null) throws NPE
        org.junit.jupiter.params.provider.Arguments.of(null, NullPointerException.class), // wrong type – cast to LocalDate fails
        org.junit.jupiter.params.provider.Arguments.of("2023-05-17", ClassCastException.class));
    }

    @Test
    void toYear_givenValidLocalDate_returnsCorrectYear() {
        // given
        LocalDate localDate = LocalDate.of(2023, 5, 17);
        // when
        Year result = LocalDateConversions.toYear(localDate, converter);
        // then
        assertEquals(Year.of(2023), result);
    }

    @Test
    void toYear_givenNull_throwsNullPointerException() {
        // given
        Object nullObject = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toYear(nullObject, converter));
    }

    @Test
    void toYear_givenNonLocalDate_throwsClassCastException() {
        // given
        Object notALocalDate = "2023-01-01";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toYear(notALocalDate, converter));
    }

    /* -------------------------------------------------------------
     *  Boundary year tests
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] toYear({0}) => {1}")
    @MethodSource("boundaryYearProvider")
    void toYear_givenBoundaryLocalDate_returnsCorrectYear(LocalDate input, Year expected) {
        Year result = LocalDateConversions.toYear(input, converter);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------------------
     *  Exception handling tests
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] toYear({0}) throws {1}")
    @MethodSource("exceptionProvider")
    void toYear_givenInvalidInput_throwsExpectedException(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateConversions.toYear(input, converter));
    }

    /* -------------------------------------------------------------
     *  Additional explicit tests (mirroring the original scaffold)
     * ------------------------------------------------------------- */
    @Test
    void toYear_givenValidLocalDate_returnsCorrectYear_explicit() {
        // given
        LocalDate localDate = LocalDate.of(2023, 5, 17);
        // when
        Year result = LocalDateConversions.toYear(localDate, converter);
        // then
        assertEquals(Year.of(2023), result);
    }

    @Test
    void toYear_givenNull_throwsNullPointerException_explicit() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toYear(null, converter));
    }

    @Test
    void toYear_givenNonLocalDate_throwsClassCastException_explicit() {
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toYear("2023-01-01", converter));
    }
}
