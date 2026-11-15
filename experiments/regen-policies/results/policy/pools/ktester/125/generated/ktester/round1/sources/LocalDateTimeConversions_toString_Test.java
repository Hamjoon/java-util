package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toString_Test {

    /**
     * Converter instance – the method under test does not use it, but we pass a real one.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter constructor requires a ConverterOptions instance.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        // a real Converter instance (used in one case to prove it is ignored)
        Converter realConverter = new Converter(new ConverterOptions());
        // a mock that throws on any interaction – used to ensure the method never calls it
        Converter explodingConverter = mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter should not be used");
        });
        return Stream.of(// Standard conversion without nanoseconds
        arguments(LocalDateTime.of(2023, 1, 15, 10, 20, 30), null, "2023-01-15T10:20:30"), // Full nanosecond precision
        arguments(LocalDateTime.of(1999, 12, 31, 23, 59, 59, 123_456_789), null, "1999-12-31T23:59:59.123456789"), // Leap‑day midnight
        arguments(LocalDateTime.of(2020, 2, 29, 0, 0, 0), null, "2020-02-29T00:00:00"), // Non‑null Converter instance (should be ignored)
        arguments(LocalDateTime.of(2021, 6, 30, 15, 45, 0), realConverter, "2021-06-30T15:45:00"), // Maximum supported LocalDateTime
        arguments(LocalDateTime.MAX, null, "999999999-12-31T23:59:59.999999999"), // Minimum supported LocalDateTime
        arguments(LocalDateTime.MIN, null, "-999999999-01-01T00:00:00"), // Trailing zeros in the fractional part are trimmed
        arguments(LocalDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000), null, "2023-05-17T14:30:45.123"), // Explicit zero nanoseconds – no fractional part emitted
        arguments(LocalDateTime.of(2023, 5, 17, 14, 30, 45, 0), null, "2023-05-17T14:30:45"), // Negative (BCE) year
        arguments(LocalDateTime.of(-1, 12, 31, 23, 59, 59), null, "-0001-12-31T23:59:59"), // Half‑second fraction with a Converter that would explode if used
        arguments(LocalDateTime.of(2022, 11, 30, 0, 0, 0, 500_000_000), explodingConverter, "2022-11-30T00:00:00.5"));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Null source should cause NPE during the cast
        arguments(null, null, NullPointerException.class), // Wrong type: String
        arguments("2023-01-01T12:00:00", null, ClassCastException.class), // Wrong type: java.sql.Timestamp
        arguments(Timestamp.valueOf("2023-01-01 12:34:56"), null, ClassCastException.class), // Wrong type: LocalDate
        arguments(LocalDate.of(2023, 1, 1), null, ClassCastException.class), // Completely unrelated object
        arguments(new Object(), null, ClassCastException.class));
    }

    @Test
    void toString_formatsLocalDateTimeUsingIsoPattern() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 14, 30, 45, 123_456_789);
        String expected = ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        // WHEN
        String result = LocalDateTimeConversions.toString(ldt, converter);
        // THEN
        assertEquals(expected, result, "LocalDateTimeConversions.toString should format the LocalDateTime using ISO_LOCAL_DATE_TIME");
    }

    @Test
    void toString_ignoresConverterArgument() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 59, 59);
        // Create a mock Converter to ensure it is not invoked.
        Converter mockConverter = mock(Converter.class);
        // WHEN
        String result = LocalDateTimeConversions.toString(ldt, mockConverter);
        // THEN
        assertEquals(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), result);
        // Verify that no interaction with the mock occurred.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toString_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toString(nullObj, converter), "Passing null as the source should result in a NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideSuccessfulConversions")
    void toString_successful(Object from, Converter conv, String expected) {
        // WHEN
        String result = LocalDateTimeConversions.toString(from, conv);
        // THEN
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideExceptionCases")
    void toString_exceptions(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateTimeConversions.toString(from, conv));
    }
}
