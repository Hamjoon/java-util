package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toOffsetTime_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    converter;

    /**
     * Simple dummy implementation of Converter for test cases where a non‑null instance is required.
     */
    private static class DummyConverter implements Converter {
        // No behaviour needed for these tests
    }

    private static Stream<Arguments> validOffsetDateTimeProvider() {
        return Stream.of(// Positive offset +02:00
        Arguments.of("Converts a standard OffsetDateTime with a positive offset (+02:00) to the corresponding OffsetTime.", OffsetDateTime.parse("2023-07-15T10:15:30+02:00"), null, OffsetTime.parse("10:15:30+02:00")), // Negative offset with nanoseconds
        Arguments.of("Converts an OffsetDateTime that includes nanoseconds and a negative offset (-05:30). The converter argument is ignored.", OffsetDateTime.parse("2023-07-15T23:59:59.123456789-05:30"), new DummyConverter(), OffsetTime.parse("23:59:59.123456789-05:30")), // UTC (Z) offset
        Arguments.of("Converts an OffsetDateTime at UTC (offset Z) to OffsetTime, ensuring zero‑offset handling.", OffsetDateTime.parse("2023-01-01T00:00:00Z"), null, OffsetTime.parse("00:00:00Z")));
    }

    @Test
    void toOffsetTime_returnsCorrectOffsetTime_forValidOffsetDateTime() {
        // GIVEN
        OffsetDateTime offsetDateTime = OffsetDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nano
        15, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        123_456_789, // offset -05:00
        ZoneOffset.ofHours(-5));
        OffsetTime expected = offsetDateTime.toOffsetTime();
        // WHEN
        OffsetTime actual = OffsetDateTimeConversions.toOffsetTime(offsetDateTime, converter);
        // THEN
        assertEquals(expected, actual, "The converted OffsetTime should match the source OffsetDateTime's offset time");
    }

    @Test
    void toOffsetTime_throwsClassCastException_forInvalidSourceObject() {
        // GIVEN
        Object notAnOffsetDateTime = "2023-05-10T15:30:45-05:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toOffsetTime(notAnOffsetDateTime, converter), "A non‑OffsetDateTime source should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validOffsetDateTimeProvider")
    void toOffsetTime_validInputs(String description, OffsetDateTime from, Converter conv, OffsetTime expected) {
        // WHEN
        OffsetTime actual = OffsetDateTimeConversions.toOffsetTime(from, conv);
        // THEN
        assertEquals(expected, actual, description);
    }

    @Test
    void toOffsetTime_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toOffsetTime(from, converter), "Passing a null `from` argument should result in NullPointerException.");
    }

    @Test
    void toOffsetTime_incompatibleString_throwsClassCastException() {
        // GIVEN
        Object from = "2023-07-15T10:15:30+02:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toOffsetTime(from, converter), "Passing a String should trigger a ClassCastException.");
    }

    @Test
    void toOffsetTime_incompatibleDate_throwsClassCastException() {
        // GIVEN
        Object from = new java.util.Date();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toOffsetTime(from, converter), "Passing a java.util.Date should trigger a ClassCastException.");
    }
}
