package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toOffsetDateTime_Test {

    // A mock is sufficient.
    @Mock
    private Converter mockConverter;

    // Simple stub implementation for the non‑null converter case.
    private static class MockConverter implements Converter {
        // No behaviour required – the method under test never calls it.
    }

    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// 1. Standard Europe/Paris in March (UTC+01)
        Arguments.of("Valid ZonedDateTime – Europe/Paris (standard time)", ZonedDateTime.of(2023, 3, 25, 14, 30, 0, 0, ZoneId.of("Europe/Paris")), OffsetDateTime.of(2023, 3, 25, 14, 30, 0, 0, ZoneOffset.ofHours(1)), null), // 2. America/New_York with negative offset
        Arguments.of("ZonedDateTime from Instant – America/New_York (UTC‑05)", ZonedDateTime.ofInstant(Instant.parse("2022-12-01T10:15:30Z"), ZoneId.of("America/New_York")), OffsetDateTime.ofInstant(Instant.parse("2022-12-01T10:15:30Z"), ZoneOffset.ofHours(-5)), null), // 3. Fixed offset zone UTC+05:30
        Arguments.of("Fixed offset zone UTC+05:30", ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC+05:30")), OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)), null), // 4. Europe/Paris during DST (UTC+02)
        Arguments.of("Europe/Paris during summer (DST, UTC+02)", ZonedDateTime.of(2023, 7, 15, 9, 45, 0, 0, ZoneId.of("Europe/Paris")), OffsetDateTime.of(2023, 7, 15, 9, 45, 0, 0, ZoneOffset.ofHours(2)), null), // 5. Extreme positive offset Pacific/Kiritimati (+14)
        Arguments.of("Maximum positive offset – Pacific/Kiritimati (+14)", ZonedDateTime.of(2023, 3, 25, 14, 30, 0, 0, ZoneId.of("Pacific/Kiritimati")), OffsetDateTime.of(2023, 3, 25, 14, 30, 0, 0, ZoneOffset.ofHours(14)), null), // 6. America/Los_Angeles winter (UTC‑08)
        Arguments.of("America/Los_Angeles winter (UTC‑08)", ZonedDateTime.of(2023, 1, 15, 8, 0, 0, 0, ZoneId.of("America/Los_Angeles")), OffsetDateTime.of(2023, 1, 15, 8, 0, 0, 0, ZoneOffset.ofHours(-8)), null), // 7. UTC with nanosecond precision and a non‑null Converter
        Arguments.of("UTC with nanosecond precision and non‑null Converter", ZonedDateTime.of(2023, 5, 10, 12, 34, 56, 123_456_789, ZoneId.of("UTC")), OffsetDateTime.of(2023, 5, 10, 12, 34, 56, 123_456_789, ZoneOffset.UTC), new MockConverter()));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Null input – should throw NullPointerException during cast
        Arguments.of("Null source object", null, NullPointerException.class), // Wrong type – LocalDateTime
        Arguments.of("Invalid type – LocalDateTime", LocalDateTime.of(2023, 5, 10, 12, 0), ClassCastException.class), // Wrong type – String representation
        Arguments.of("Invalid type – String", "2023-01-01T00:00:00Z", ClassCastException.class));
    }

    @Test
    void toOffsetDateTime_WithValidZonedDateTime_ReturnsEquivalentOffsetDateTime() {
        // given
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 123_456_789, ZoneId.of("Europe/Paris"));
        // when
        OffsetDateTime result = ZonedDateTimeConversions.toOffsetDateTime(zonedDateTime, mockConverter);
        // then
        assertNotNull(result, "Result should not be null for a valid ZonedDateTime");
        assertEquals(zonedDateTime.toOffsetDateTime(), result, "The returned OffsetDateTime must be equal to the one produced by ZonedDateTime.toOffsetDateTime()");
    }

    @Test
    void toOffsetDateTime_WithNullInput_ThrowsNullPointerException() {
        // given
        Object nullInput = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toOffsetDateTime(nullInput, mockConverter), "Calling toOffsetDateTime with a null source should throw NullPointerException");
    }

    @Test
    void toOffsetDateTime_WithInvalidType_ThrowsClassCastException() {
        // given
        Object notAZonedDateTime = "2023-05-17T14:30:45+01:00[Europe/Paris]";
        // when / then
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toOffsetDateTime(notAZonedDateTime, mockConverter), "Calling toOffsetDateTime with a non‑ZonedDateTime source should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideValidConversions")
    void toOffsetDateTime_ValidCases(String description, ZonedDateTime input, OffsetDateTime expected, Converter converter) {
        // when
        OffsetDateTime result = ZonedDateTimeConversions.toOffsetDateTime(input, converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result, () -> "Conversion failed for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionCases")
    void toOffsetDateTime_ExceptionCases(String description, Object input, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toOffsetDateTime(input, null), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
