package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toString_Test {

    /**
     * Converter instance required by the method signature – it is not used internally.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a non‑null options object; use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none here).
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Standard UTC with milliseconds
        Arguments.of("Standard conversion of a UTC ZonedDateTime with millisecond precision", ZonedDateTime.of(2023, 3, 25, 14, 30, 15, 123_000_000, ZoneId.of("UTC")), "2023-03-25T14:30:15.123Z"), // Leap‑day with negative offset and full nanoseconds
        Arguments.of("Leap‑day date in a zone with a negative offset; full nanosecond precision", ZonedDateTime.of(2020, 2, 29, 23, 59, 59, 999_999_999, ZoneId.of("America/New_York")), "2020-02-29T23:59:59.999999999-05:00"), // Epoch start UTC, no fraction
        Arguments.of("Epoch start in UTC without fractional seconds", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "1970-01-01T00:00:00Z"), // Maximum year with unusual positive offset
        Arguments.of("Maximum representable year with an unusual positive offset", ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneId.of("+12:45")), "9999-12-31T23:59:59+12:45"), // Positive offset with minutes, zero nanos
        Arguments.of("Positive offset with non‑zero minutes (+05:30) and zero nanoseconds", ZonedDateTime.of(2022, 11, 5, 9, 15, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)), "2022-11-05T09:15:00+05:30"), // Negative offset with single nanosecond
        Arguments.of("Negative offset with non‑standard minutes (-03:30) and a single‑nanosecond fraction", ZonedDateTime.of(2022, 11, 5, 9, 15, 0, 1, ZoneOffset.ofHoursMinutes(-3, -30)), "2022-11-05T09:15:00.000000001-03:30"), // Maximum positive UTC offset (+18:00)
        Arguments.of("Maximum positive UTC offset (+18:00) allowed by ISO‑8601", ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(18)), "2000-01-01T00:00:00+18:00"), // Maximum negative UTC offset (‑18:00)
        Arguments.of("Maximum negative UTC offset (‑18:00)", ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-18)), "2000-01-01T00:00:00-18:00"), // Year 10000 UTC
        Arguments.of("Year beyond the typical four‑digit range (year 10000) with UTC", ZonedDateTime.of(10000, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC), "10000-01-01T12:00:00Z"), // Summer DST in Europe/Paris (+02:00)
        Arguments.of("Summer date in a DST‑observing zone (Europe/Paris) where the offset is +02:00", ZonedDateTime.of(2021, 7, 15, 14, 45, 30, 0, ZoneId.of("Europe/Paris")), "2021-07-15T14:45:30+02:00"), // Winter standard time in Europe/Paris (+01:00)
        Arguments.of("Winter date in the same DST zone where the offset is +01:00", ZonedDateTime.of(2021, 1, 15, 14, 45, 30, 0, ZoneId.of("Europe/Paris")), "2021-01-15T14:45:30+01:00"));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of("Passing a null `from` argument should result in NullPointerException", null, NullPointerException.class), Arguments.of("Providing a non‑ZonedDateTime (String) must cause ClassCastException", "2023-03-25T14:30:15Z", ClassCastException.class));
    }

    private static Stream<Arguments> converterProvidedProvider() {
        // Dummy converter that throws if any of its methods are called – the method under test must NOT use it.
        Converter throwingConverter = new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // Another example using an anonymous subclass that overrides convert.
        Converter anonymousThrowing = new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                throw new RuntimeException("Should not be called");
            }
        };
        return Stream.of(Arguments.of("Non‑null Converter supplied (UTC now) – method must ignore it", ZonedDateTime.now(ZoneId.of("UTC")), throwingConverter), Arguments.of("Non‑null Converter supplied (Tokyo now) – method must ignore it", ZonedDateTime.now(ZoneId.of("Asia/Tokyo")), anonymousThrowing), Arguments.of("Non‑null Converter supplied (Los Angeles now) – method must ignore it", ZonedDateTime.now(ZoneId.of("America/Los_Angeles")), // the static, properly constructed converter
        converter));
    }

    @Test
    @DisplayName("toString returns ISO‑8601 representation for a valid ZonedDateTime")
    void toStringReturnsIsoString() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 14, 30, 45, 123_456_789, ZoneId.of("UTC"));
        String expected = zdt.format(DateTimeFormatter.ISO_DATE_TIME);
        // when
        String actual = ZonedDateTimeConversions.toString(zdt, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("toString throws NullPointerException when source is null")
    void toStringThrowsOnNull() {
        // given / when / then
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toString(null, converter));
    }

    @Test
    @DisplayName("toString throws ClassCastException when source is not a ZonedDateTime")
    void toStringThrowsOnWrongType() {
        // given
        Object notADate = "2023-05-10T14:30:45Z";
        // when / then
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toString(notADate, converter));
    }

    /**
     * Successful conversions – the method should return the exact ISO‑8601 string
     * produced by {@link DateTimeFormatter#ISO_DATE_TIME}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toStringProducesExpectedIsoString(String description, ZonedDateTime input, String expected) {
        // The converter argument is deliberately set to {@code null} for most cases,
        // because the implementation does not use it.
        String actual = ZonedDateTimeConversions.toString(input, null);
        assertEquals(expected, actual, description);
    }

    /**
     * Cases where the method must throw an exception (null source or wrong type).
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toStringThrowsExpectedException(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toString(input, null), description);
    }

    /**
     * Cases where a non‑null {@link Converter} is supplied.
     * The converter is not used, but we verify that the method still returns a
     * correctly formatted ISO string and does not invoke the converter.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("converterProvidedProvider")
    void toStringWithNonNullConverter(String description, ZonedDateTime input, Converter suppliedConverter) {
        String result = ZonedDateTimeConversions.toString(input, suppliedConverter);
        // The result must be a parsable ISO‑8601 date‑time.
        assertDoesNotThrow(() -> ZonedDateTime.parse(result), description);
    }
}
