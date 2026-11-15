package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toString_Test {

    /**
     * Mocked Converter – the method under test does not use it, but a non‑null instance is required.
     */
    @Mock
    private Converter converter;

    /**
     * Supplies a stream of arguments:
     * <ul>
     *   <li>LocalTime instance to convert</li>
     *   <li>Converter to pass (may be {@code null} or a dummy implementation)</li>
     *   <li>Expected ISO‑8601 string</li>
     * </ul>
     */
    static Stream<Arguments> provideValidLocalTimeCases() {
        // 1. Standard time with hour, minute, second only.
        LocalTime t1 = LocalTime.of(13, 45, 30);
        // 2. Midnight (hour/minute only).
        LocalTime t2 = LocalTime.MIDNIGHT;
        // 3. Time with nanosecond precision.
        LocalTime t3 = LocalTime.of(23, 59, 59, 123_456_789);
        // 4. Time where a custom (non‑null) converter is supplied – it should be ignored.
        LocalTime t4 = LocalTime.of(12, 0, 0, 0);
        Converter dummyConverter = new Converter() {
        };
        // 5. Edge case: hour & minute only, seconds are zero – seconds should be omitted.
        LocalTime t5 = LocalTime.of(13, 45, 0);
        // 6. Edge case: hour, minute, second non‑zero, nanosecond zero – seconds kept, no fraction.
        LocalTime t6 = LocalTime.of(13, 45, 30, 0);
        // 7. Edge case: created from one second past midnight.
        LocalTime t7 = LocalTime.ofSecondOfDay(1);
        // 8. Converter that throws if invoked – verifies the method truly ignores it.
        LocalTime t8 = LocalTime.of(9, 30, 15, 250_000_000);
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be called");
            }
        };
        return Stream.of(Arguments.of(t1, null, t1.format(DateTimeFormatter.ISO_LOCAL_TIME)), Arguments.of(t2, null, t2.format(DateTimeFormatter.ISO_LOCAL_TIME)), Arguments.of(t3, null, t3.format(DateTimeFormatter.ISO_LOCAL_TIME)), Arguments.of(t4, dummyConverter, t4.format(DateTimeFormatter.ISO_LOCAL_TIME)), Arguments.of(t5, null, t5.format(DateTimeFormatter.ISO_LOCAL_TIME)), Arguments.of(t6, null, t6.format(DateTimeFormatter.ISO_LOCAL_TIME)), Arguments.of(t7, null, t7.format(DateTimeFormatter.ISO_LOCAL_TIME)), Arguments.of(t8, throwingConverter, t8.format(DateTimeFormatter.ISO_LOCAL_TIME)));
    }

    @Test
    void toString_ShouldFormatLocalTimeWithoutNanos() {
        // GIVEN
        // 13:45:30
        LocalTime time = LocalTime.of(13, 45, 30);
        String expected = time.format(DateTimeFormatter.ISO_LOCAL_TIME);
        // WHEN
        String result = LocalTimeConversions.toString(time, converter);
        // THEN
        assertEquals(expected, result, "The ISO_LOCAL_TIME string representation should match the expected format");
    }

    @Test
    void toString_ShouldFormatLocalTimeWithNanos() {
        // GIVEN
        // 09:05:07.123456789
        LocalTime time = LocalTime.of(9, 5, 7, 123_456_789);
        String expected = time.format(DateTimeFormatter.ISO_LOCAL_TIME);
        // WHEN
        String result = LocalTimeConversions.toString(time, converter);
        // THEN
        assertEquals(expected, result, "The ISO_LOCAL_TIME string representation should include nanoseconds when present");
    }

    @Test
    void toString_ShouldFormatMidnight() {
        // GIVEN
        // 00:00
        LocalTime midnight = LocalTime.MIDNIGHT;
        String expected = midnight.format(DateTimeFormatter.ISO_LOCAL_TIME);
        // WHEN
        String result = LocalTimeConversions.toString(midnight, converter);
        // THEN
        assertEquals(expected, result, "Midnight should be formatted as 00:00");
    }

    @Test
    void toString_NullInput_ShouldThrowNullPointerException() {
        // GIVEN
        Object nullObject = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toString(nullObject, converter), "Passing null as the source object should result in a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] toString({0}) => \"{2}\"")
    @MethodSource("provideValidLocalTimeCases")
    void toString_ValidScenarios_ReturnsExpectedString(LocalTime from, Converter conv, String expected) {
        // WHEN
        String result = LocalTimeConversions.toString(from, conv);
        // THEN
        assertEquals(expected, result, () -> "Expected ISO_LOCAL_TIME representation to be \"" + expected + "\"");
    }

    @Test
    void toString_WrongTypeInput_ShouldThrowClassCastException() {
        // GIVEN
        Object wrongType = "2021-01-01T12:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toString(wrongType, converter), "Providing a non‑LocalTime object should cause a ClassCastException");
    }
}
