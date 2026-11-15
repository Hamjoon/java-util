package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toString_Test {

    @Mock
    private Converter // Converter is not used by the method, but required for the signature
    mockConverter;

    /**
     * Provides a stream of valid {@link LocalDate} instances together with the
     * expected ISO‑8601 string representation.
     */
    private static Stream<Arguments> provideValidLocalDates() {
        return Stream.of(// Branch‑coverage group
        Arguments.of(LocalDate.of(2023, 1, 15), "2023-01-15"), Arguments.of(LocalDate.of(1999, 12, 31), "1999-12-31"), Arguments.of(LocalDate.MIN, "-999999999-01-01"), Arguments.of(LocalDate.MAX, "+999999999-12-31"), // Additional cases group
        // leap year
        Arguments.of(LocalDate.of(2020, 2, 29), "2020-02-29"), // epoch start
        Arguments.of(LocalDate.of(1970, 1, 1), "1970-01-01"), // year zero (ISO‑8601)
        Arguments.of(LocalDate.of(0, 1, 1), "0000-01-01"), // high realistic year
        Arguments.of(LocalDate.of(9999, 12, 31), "9999-12-31"), // 1 BCE
        Arguments.of(LocalDate.of(-1, 12, 31), "-0001-12-31"), // earliest positive year
        Arguments.of(LocalDate.of(1, 1, 1), "0001-01-01"), Arguments.of(LocalDate.of(2023, 12, 31), "2023-12-31"), Arguments.of(LocalDate.of(2023, 1, 1), "2023-01-01"), Arguments.of(LocalDate.of(2023, 4, 30), "2023-04-30"), Arguments.of(LocalDate.of(2023, 5, 31), "2023-05-31"));
    }

    /**
     * Provides inputs that should cause the method to throw an exception.
     */
    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(Arguments.of(null, NullPointerException.class), Arguments.of("2023-01-15", ClassCastException.class));
    }

    @Test
    void toString_returnsIsoLocalDateString() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 5, 17);
        // WHEN
        String result = LocalDateConversions.toString(date, mockConverter);
        // THEN
        assertEquals("2023-05-17", result);
    }

    @Test
    void toString_throwsClassCastExceptionWhenInputIsNotLocalDate() {
        // GIVEN
        Object notALocalDate = "2023-05-17";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toString(notALocalDate, mockConverter));
    }

    @Test
    void toString_throwsNullPointerExceptionWhenInputIsNull() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toString(nullInput, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] toString({0}) → \"{1}\"")
    @MethodSource("provideValidLocalDates")
    void toString_formatsLocalDateCorrectly(LocalDate input, String expected) {
        // WHEN
        String result = LocalDateConversions.toString(input, mockConverter);
        // THEN
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] toString({0}) throws {1}")
    @MethodSource("provideInvalidInputs")
    void toString_invalidInputs_throwExpectedException(Object input, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> LocalDateConversions.toString(input, mockConverter));
    }
}
