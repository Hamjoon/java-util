package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toOffsetTime_Test {

    /**
     * Converter instance used for the tests – the method under test does not actually use it,
     * but the signature requires it.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter has a public constructor that requires ConverterOptions.
        converter = new Converter(new ConverterOptions());
    }

    static Stream<Arguments> validOffsetDateTimeProvider() {
        return Stream.of(// Simple positive offset, zero nanoseconds
        Arguments.of(OffsetDateTime.of(2023, 5, 15, 10, 30, 45, 0, ZoneOffset.ofHours(2)), OffsetTime.of(10, 30, 45, 0, ZoneOffset.ofHours(2)), "Converts a proper OffsetDateTime instance to its corresponding OffsetTime, preserving the time and offset."), // Edge case: maximum nanosecond value within a day
        Arguments.of(OffsetDateTime.of(2023, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC), OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.UTC), "Edge case: maximum nanosecond value within a day, ensuring nanoseconds are retained."), // Leap day with negative offset
        Arguments.of(OffsetDateTime.of(2020, 2, 29, 0, 0, 0, 0, ZoneOffset.ofHoursMinutes(-5, -30)), OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHoursMinutes(-5, -30)), "Leap day with negative offset, verifying that date part is ignored and offset preserved."));
    }

    static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Null source – should raise NPE because of the cast
        Arguments.of(null, NullPointerException.class, "Passing a null reference as the source object should result in a NullPointerException."), // String source – not an OffsetDateTime
        Arguments.of("2023-05-15T10:30:45+02:00", ClassCastException.class, "Providing a String instead of an OffsetDateTime triggers a ClassCastException."), // LocalDateTime lacks offset information
        Arguments.of(LocalDateTime.of(2023, 5, 15, 10, 30, 45), ClassCastException.class, "Supplying a LocalDateTime (which lacks offset information) should cause a ClassCastException."), // Arbitrary object
        Arguments.of(new Object(), ClassCastException.class, "An arbitrary Object that is not an OffsetDateTime should also result in a ClassCastException."));
    }

    @Test
    void toOffsetTime_ValidOffsetDateTime_ReturnsCorrespondingOffsetTime() {
        // given
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 15, 10, 30, 45, 123_000_000, ZoneOffset.ofHoursMinutes(2, 30));
        // when
        OffsetTime result = OffsetDateTimeConversions.toOffsetTime(odt, converter);
        // then
        OffsetTime expected = odt.toOffsetTime();
        assertEquals(expected, result, "The OffsetTime produced should match the source OffsetDateTime's time component");
    }

    @Test
    void toOffsetTime_NullInput_ThrowsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toOffsetTime(nullObj, converter), "Calling toOffsetTime with a null source should throw NullPointerException");
    }

    @Test
    void toOffsetTime_WrongType_ThrowsClassCastException() {
        // given
        String notAnOffsetDateTime = "2023-05-15T10:30:45+02:30";
        // when / then
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toOffsetTime(notAnOffsetDateTime, converter), "Calling toOffsetTime with a non‑OffsetDateTime source should throw ClassCastException");
    }

    /**
     * Valid conversion scenarios – the method should return an {@link OffsetTime}
     * that matches the time‑and‑offset part of the supplied {@link OffsetDateTime}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validOffsetDateTimeProvider")
    void toOffsetTime_validOffsetDateTime(OffsetDateTime from, OffsetTime expected, String description) {
        // when
        // converter not used
        OffsetTime result = OffsetDateTimeConversions.toOffsetTime(from, null);
        // then
        assertEquals(expected, result, description);
    }

    /**
     * Invalid input scenarios – the method is expected to throw either
     * {@link NullPointerException} or {@link ClassCastException} depending on the
     * supplied argument.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("invalidInputProvider")
    void toOffsetTime_invalidInputs(Object from, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toOffsetTime(from, null), description);
    }
}
