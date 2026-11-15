package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toOffsetTime_Test {

    /**
     * A mock {@link Converter} – the method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Minimal stub implementation of {@link Converter}.  The real interface
     * contains many conversion methods, but none are used by the method under test,
     * so an empty implementation is sufficient for compilation.
     */
    private static class DummyConverter implements Converter {
        // No‑op implementation – all methods (if any) are left with default behaviour.
    }

    /**
     * Supplies all valid conversion scenarios.
     */
    private static Stream<Arguments> validConversionProvider() {
        // Helper converter instances
        Converter nullConverter = null;
        // The @Mock field cannot be accessed in a static context, so we will
        // use a dummy implementation for non‑null converters.
        Converter dummyConverter = new DummyConverter();
        return Stream.of(// ---- group: Valid OffsetDateTime conversions ----
        Arguments.of(OffsetDateTime.parse("2023-05-01T10:15:30+02:00"), OffsetTime.parse("10:15:30+02:00"), nullConverter, "Standard conversion: positive offset (+02:00)"), Arguments.of(OffsetDateTime.parse("2020-12-31T23:59:59-05:00"), OffsetTime.parse("23:59:59-05:00"), nullConverter, "Negative offset (-05:00) is retained"), Arguments.of(OffsetDateTime.parse("1970-01-01T00:00:00Z"), OffsetTime.parse("00:00:00Z"), dummyConverter, "UTC conversion with a non‑null Converter"), Arguments.of(OffsetDateTime.of(2022, 2, 28, 12, 0, 0, 0, ZoneOffset.ofHoursMinutes(3, 30)), OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHoursMinutes(3, 30)), nullConverter, "Offset with minutes (+03:30) is preserved"), // ---- group: Additional valid OffsetDateTime conversions ----
        Arguments.of(OffsetDateTime.of(2023, 3, 10, 5, 6, 7, 0, ZoneOffset.ofHours(14)), OffsetTime.of(5, 6, 7, 0, ZoneOffset.ofHours(14)), nullConverter, "Maximum positive offset (+14:00)"), Arguments.of(OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-12)), OffsetTime.of(23, 59, 59, 0, ZoneOffset.ofHours(-12)), nullConverter, "Maximum negative offset (-12:00)"), Arguments.of(OffsetDateTime.of(2021, 7, 15, 8, 30, 45, 123_456_789, ZoneOffset.ofHoursMinutes(5, 45)), OffsetTime.of(8, 30, 45, 123_456_789, ZoneOffset.ofHoursMinutes(5, 45)), nullConverter, "Nanosecond precision with non‑hour offset (+05:45)"), Arguments.of(OffsetDateTime.of(-999_999_999, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), nullConverter, "Extreme past year – date part ignored"), Arguments.of(OffsetDateTime.of(999_999_999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC), nullConverter, "Extreme future year – date part ignored"), Arguments.of(OffsetDateTime.parse("2022-08-15T12:34:56.987654321+00:00"), OffsetTime.parse("12:34:56.987654321Z"), dummyConverter, "Nanosecond precision with UTC and non‑null Converter"));
    }

    /**
     * Supplies all invalid conversion scenarios.
     */
    private static Stream<Arguments> invalidConversionProvider() {
        Converter nullConverter = null;
        Converter dummyConverter = new DummyConverter();
        return Stream.of(// ---- group: Invalid inputs ----
        Arguments.of(null, nullConverter, NullPointerException.class, "null 'from' argument triggers NPE"), Arguments.of("2023-05-01T10:15:30+02:00", nullConverter, ClassCastException.class, "String is not an OffsetDateTime"), Arguments.of(LocalDateTime.now(), nullConverter, ClassCastException.class, "LocalDateTime cannot be cast to OffsetDateTime"), // ---- group: Additional invalid inputs ----
        Arguments.of(new Date(), nullConverter, ClassCastException.class, "java.util.Date is not an OffsetDateTime"), Arguments.of(new java.util.concurrent.atomic.AtomicLong(123L), nullConverter, ClassCastException.class, "AtomicLong is not an OffsetDateTime"));
    }

    @Test
    void toOffsetTime_ShouldReturnCorrectOffsetTime_WhenGivenOffsetDateTime() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2023, 5, 17, 14, 30, 45, 123_456_789, ZoneOffset.ofHoursMinutes(2, 30));
        // WHEN
        OffsetTime result = OffsetDateTimeConversions.toOffsetTime(source, mockConverter);
        // THEN
        OffsetTime expected = source.toOffsetTime();
        assertEquals(expected, result, "The returned OffsetTime should match the one derived from the source OffsetDateTime");
    }

    @Test
    void toOffsetTime_ShouldThrowClassCastException_WhenGivenIncompatibleObject() {
        // GIVEN
        Object notAnOffsetDateTime = "2023-05-17T14:30:45+02:30";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toOffsetTime(notAnOffsetDateTime, mockConverter), "A ClassCastException is expected when the source object is not an OffsetDateTime");
    }

    /**
     * Parameterized test for inputs that should result in an exception.
     */
    @ParameterizedTest(name = "{3}")
    @MethodSource("invalidConversionProvider")
    void toOffsetTime_ShouldThrow_WhenGivenInvalidInput(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toOffsetTime(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for: " + description);
    }

    /**
     * Verifies that a {@link Converter} that throws on any method call does not affect
     * the conversion because {@code toOffsetTime} never invokes it.
     */
    @Test
    void toOffsetTime_ShouldIgnoreThrowingConverter() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.parse("2024-01-01T00:00:00+01:00");
        // A mock Converter that throws on any interaction.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        // WHEN
        OffsetTime result = OffsetDateTimeConversions.toOffsetTime(source, throwingConverter);
        // THEN
        OffsetTime expected = OffsetTime.parse("00:00:00+01:00");
        assertEquals(expected, result, "Conversion must succeed even when the supplied Converter throws on use");
        // Verify that no method on the mock was actually invoked.
        verifyNoInteractions(throwingConverter);
    }
}
