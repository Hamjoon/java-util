package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toString_Test {

    /**
     * The mock Converter is not used by the method under test, but it is required
     *  by the method signature.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> validLocalDateTimeProvider() {
        return Stream.of(Arguments.of("Standard conversion without nanoseconds", LocalDateTime.of(2023, 1, 15, 10, 20, 30), "2023-01-15T10:20:30"), Arguments.of("Conversion with nanoseconds (full precision)", LocalDateTime.of(2023, 1, 15, 10, 20, 30, 123_456_789), "2023-01-15T10:20:30.123456789"));
    }

    private static Stream<Arguments> fractionalSecondsProvider() {
        return Stream.of(Arguments.of("Nanoseconds 120,000,000 → .12 seconds", LocalDateTime.of(2023, 5, 10, 12, 30, 45, 120_000_000), "2023-05-10T12:30:45.12"), Arguments.of("Nanoseconds 100,000,000 → .1 second", LocalDateTime.of(2023, 5, 10, 12, 30, 45, 100_000_000), "2023-05-10T12:30:45.1"));
    }

    private static Stream<Arguments> extremeValuesProvider() {
        return Stream.of(Arguments.of("Maximum supported LocalDateTime", LocalDateTime.MAX, "999999999-12-31T23:59:59.999999999"), Arguments.of("Minimum supported LocalDateTime (negative year, no fraction)", LocalDateTime.MIN, "-999999999-01-01T00:00"));
    }

    @Test
    void toString_ShouldReturnIsoLocalDateTimeString() {
        // GIVEN
        LocalDateTime now = LocalDateTime.of(2023, 5, 17, 13, 45, 30, 123_456_789);
        String expected = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        // WHEN
        String result = LocalDateTimeConversions.toString(now, mockConverter);
        // THEN
        assertEquals(expected, result, "The ISO‑8601 string representation should match the formatter output.");
    }

    @Test
    void toString_NullInput_ShouldThrowNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toString(nullObj, mockConverter), "Passing null should result in a NullPointerException because the method casts without a null check.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validLocalDateTimeProvider")
    void toString_ValidLocalDateTime_ReturnsIsoString(String description, LocalDateTime input, String expected) {
        // WHEN
        String result = LocalDateTimeConversions.toString(input, null);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("fractionalSecondsProvider")
    void toString_FractionalSeconds_TrailingZerosTrimmed(String description, LocalDateTime input, String expected) {
        // WHEN
        String result = LocalDateTimeConversions.toString(input, null);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("extremeValuesProvider")
    void toString_ExtremeValues_ReturnsCorrectString(String description, LocalDateTime input, String expected) {
        // WHEN
        String result = LocalDateTimeConversions.toString(input, null);
        // THEN
        assertEquals(expected, result, description);
    }

    @Test
    void toString_NullFrom_ShouldThrowNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toString(nullObj, null), "Passing null should result in a NullPointerException because the method casts without a null check.");
    }

    @Test
    void toString_InvalidFromType_ShouldThrowClassCastException() {
        // GIVEN
        String notALocalDateTime = "2023-01-15T10:20:30";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toString(notALocalDateTime, null), "Passing a non‑LocalDateTime should cause a ClassCastException at the cast site.");
    }

    @Test
    void toString_NonNullConverter_Ignored_ReturnsCorrectString() {
        // GIVEN
        LocalDateTime input = LocalDateTime.of(2022, 12, 31, 23, 59, 59);
        Converter noOpConverter = new Converter() {

            @Override
            public <T> T convert(Object o, Class<T> c) {
                // Should never be called.
                return null;
            }
        };
        String expected = input.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        // WHEN
        String result = LocalDateTimeConversions.toString(input, noOpConverter);
        // THEN
        assertEquals(expected, result, "A non‑null Converter is supplied but must be ignored; result should equal the ISO string.");
    }

    @Test
    void toString_NonNullConverter_ThrowingImplementation_Ignored() {
        // GIVEN
        LocalDateTime input = LocalDateTime.of(2022, 12, 31, 23, 59, 59, 500_000_000);
        Converter throwingConverter = new Converter() {

            @Override
            public <T> T convert(Object o, Class<T> c) {
                throw new RuntimeException("Should not be called");
            }
        };
        String expected = input.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        // WHEN
        String result = LocalDateTimeConversions.toString(input, throwingConverter);
        // THEN
        assertEquals(expected, result, "Even if the supplied Converter throws, the method must not invoke it and should return the correct string.");
    }

    @Test
    void toString_RepeatedCalls_ProduceIdenticalResult() {
        // GIVEN
        LocalDateTime input = LocalDateTime.of(2021, 7, 20, 8, 15, 0);
        String expected = "2021-07-20T08:15:00";
        // WHEN
        String first = LocalDateTimeConversions.toString(input, null);
        String second = LocalDateTimeConversions.toString(input, null);
        // THEN
        assertAll(() -> assertEquals(expected, first, "First call should match expected ISO string."), () -> assertEquals(expected, second, "Second call should produce the same result as the first."));
    }
}
