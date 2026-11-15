package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Constructor;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toString_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    static Stream<Arguments> validOffsetTimeProvider() {
        return Stream.of(Arguments.of("Converts a normal OffsetTime instance to its ISO_OFFSET_TIME string representation.", OffsetTime.parse("10:15:30+01:00"), "10:15:30+01:00"), Arguments.of("Handles maximum nanosecond precision and a negative UTC offset.", OffsetTime.parse("23:59:59.999999999-05:00"), "23:59:59.999999999-05:00"), Arguments.of("Ensures the provided Converter argument is ignored and does not affect the result.", OffsetTime.parse("00:00+00:00"), "00:00+00:00"));
    }

    static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("Providing a String instead of an OffsetTime should cause a ClassCastException.", "2023-01-01"), Arguments.of("Providing a LocalDate (different type) should also result in a ClassCastException.", LocalDate.now()));
    }

    static Stream<Arguments> invalidParsingProvider() {
        return Stream.of(Arguments.of("Parsing an invalid hour (25) should throw DateTimeException before the method is invoked.", "25:00:00+01:00"), Arguments.of("Invalid minute value (61) causes DateTimeException at parsing time.", "10:61:00+01:00"));
    }

    static Stream<Arguments> extremeOffsetProvider() {
        return Stream.of(Arguments.of("OffsetTime supports offsets up to +18:00; ensure the formatter handles the maximum positive offset.", OffsetTime.parse("12:00+18:00"), "12:00+18:00"), Arguments.of("OffsetTime supports offsets down to -18:00; ensure the formatter handles the maximum negative offset.", OffsetTime.parse("12:00-18:00"), "12:00-18:00"));
    }

    @Test
    void toString_WithOffsetTime_ReturnsIsoFormattedString() {
        // GIVEN
        OffsetTime offsetTime = OffsetTime.of(LocalTime.of(13, 45, 30, 0), ZoneOffset.ofHours(2));
        // WHEN
        String result = OffsetTimeConversions.toString(offsetTime, mockConverter);
        // THEN
        assertEquals("13:45:30+02:00", result);
    }

    @Test
    void toString_WithOffsetTime_WithMinutesOffset_ReturnsIsoFormattedString() {
        // GIVEN
        OffsetTime offsetTime = OffsetTime.of(LocalTime.of(9, 5, 7, 123_000_000), ZoneOffset.ofHoursMinutes(-5, -30));
        // WHEN
        String result = OffsetTimeConversions.toString(offsetTime, mockConverter);
        // THEN
        assertEquals("09:05:07.123-05:30", result);
    }

    @Test
    void toString_NullSource_ThrowsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toString(nullObj, mockConverter));
    }

    @Test
    void toString_NonOffsetTime_ThrowsClassCastException() {
        // GIVEN
        Object notAnOffsetTime = "2023-01-01T12:00:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toString(notAnOffsetTime, mockConverter));
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validOffsetTimeProvider")
    void toString_ValidOffsetTime(String description, OffsetTime input, String expected) {
        // WHEN
        String result = OffsetTimeConversions.toString(input, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    // --------------------------------------------------------------------- //
    @Test
    void toString_NullFrom_ThrowsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toString(from, null));
    }

    @Test
    void toString_NullConverter_IsIgnoredWhenFromIsValid() {
        // GIVEN
        OffsetTime from = OffsetTime.parse("12:00+02:00");
        // WHEN (converter is null)
        String result = OffsetTimeConversions.toString(from, null);
        // THEN
        assertEquals("12:00+02:00", result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidTypeProvider")
    void toString_InvalidSourceType_ThrowsClassCastException(String description, Object from) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toString(from, mockConverter), description);
    }

    // --------------------------------------------------------------------- //
    @Test
    void toString_OffsetTimeWithoutSeconds_PadsSecondsWithZero() {
        // GIVEN
        // seconds omitted
        OffsetTime input = OffsetTime.parse("10:15+01:00");
        // WHEN
        String result = OffsetTimeConversions.toString(input, mockConverter);
        // THEN
        assertEquals("10:15:00+01:00", result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidParsingProvider")
    void toString_InvalidOffsetTimeString_ParsingThrowsDateTimeException(String description, String badString) {
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> OffsetTime.parse(badString), description);
    }

    // --------------------------------------------------------------------- //
    @Test
    void toString_OffsetTimeWithNullLocalTime_ThrowsNullPointerException() throws Exception {
        // GIVEN – create an OffsetTime instance with a null LocalTime via reflection
        Constructor<OffsetTime> ctor = OffsetTime.class.getDeclaredConstructor(LocalTime.class, ZoneOffset.class);
        ctor.setAccessible(true);
        OffsetTime corrupted = ctor.newInstance(null, ZoneOffset.UTC);
        // WHEN / THEN – the formatter will attempt to read the time component and NPE
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toString(corrupted, mockConverter));
    }

    // --------------------------------------------------------------------- //
    @Test
    void toString_ConverterThrowsButMethodIgnoresIt() {
        // GIVEN – a Converter that throws on any call
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("converter failure");
            }
        };
        OffsetTime input = OffsetTime.parse("12:34:56+02:00");
        // WHEN – the method never touches the converter
        String result = OffsetTimeConversions.toString(input, explodingConverter);
        // THEN
        assertEquals("12:34:56+02:00", result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("extremeOffsetProvider")
    void toString_ExtremeOffsets(String description, OffsetTime input, String expected) {
        // WHEN
        String result = OffsetTimeConversions.toString(input, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }
}
