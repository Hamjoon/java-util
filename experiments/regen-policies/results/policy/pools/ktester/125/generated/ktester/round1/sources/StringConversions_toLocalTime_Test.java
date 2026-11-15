package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class StringConversions_toLocalTime_Test {

    private static Converter converter;

    // A regular, functional Converter instance used for the happy‑path tests.
    private static Converter dummyConverter;

    /**
     * Simple holder for parameterised‑test arguments.
     */
    private static class ArgumentsHolder {

        // raw JSON value (including quotes when a string)
        final String from;

        // may be null
        final Converter converter;

        // LocalTime, null, or an exception class
        final Object expected;

        ArgumentsHolder(String from, Converter converter, Object expected) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        converter = null;
    }

    /* -------------------------------------------------------------
     *  Parameterized test data
     * ------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validLocalTimeProvider() {
        return Stream.of(new ArgumentsHolder("\"13:45:30\"", dummyConverter, LocalTime.of(13, 45, 30)), new ArgumentsHolder("\"23:59:59.999999999\"", dummyConverter, LocalTime.of(23, 59, 59, 999_999_999)));
    }

    private static Stream<ArgumentsHolder> fallbackZonedDateTimeProvider() {
        return Stream.of(new ArgumentsHolder("\"2021-08-15T13:45:30+02:00\"", dummyConverter, LocalTime.of(13, 45, 30)), new ArgumentsHolder("\"2021-08-15T13:45:30+02:00[Europe/Paris]\"", dummyConverter, LocalTime.of(13, 45, 30)));
    }

    private static Stream<ArgumentsHolder> invalidInputProvider() {
        return Stream.of(new ArgumentsHolder("\"13-45-30\"", dummyConverter, null), new ArgumentsHolder("\"2021-08-15\"", dummyConverter, null), new ArgumentsHolder("\"   \"", dummyConverter, null), new ArgumentsHolder("null", dummyConverter, null), new ArgumentsHolder("\"\"", dummyConverter, null));
    }

    private static Stream<ArgumentsHolder> nullConverterDirectParseProvider() {
        return Stream.of(new ArgumentsHolder("\"09:15:00\"", null, LocalTime.of(9, 15, 0)));
    }

    private static Stream<ArgumentsHolder> nonStringSourceProvider() {
        return Stream.of(new ArgumentsHolder("123", dummyConverter, ClassCastException.class));
    }

    private static Stream<ArgumentsHolder> converterExceptionDuringFallbackProvider() {
        // Converter that throws RuntimeException on any method call.
        Converter exceptionThrowingConverter = Mockito.mock(Converter.class);
        Mockito.doThrow(new RuntimeException("Converter failure")).when(exceptionThrowingConverter).convert(any(), any());
        return Stream.of(new ArgumentsHolder("\"2021-08-15T13:45:30+02:00\"", exceptionThrowingConverter, RuntimeException.class));
    }

    private static Stream<ArgumentsHolder> nullConverterWithFallbackProvider() {
        return Stream.of(new ArgumentsHolder("\"invalid-time\"", null, NullPointerException.class));
    }

    /**
     * Removes surrounding double‑quotes from a JSON‑style string literal.
     */
    private static String stripQuotes(String jsonString) {
        if (jsonString == null)
            return null;
        return jsonString.replaceAll("^\"|\"$", "");
    }

    /**
     * When the input string is a valid {@link LocalTime} representation,
     * the method should return the parsed {@code LocalTime} directly.
     */
    @Test
    void shouldParseSimpleLocalTimeString() {
        // given
        String timeString = "12:34:56";
        // when
        LocalTime result = StringConversions.toLocalTime(timeString, converter);
        // then
        assertNotNull(result, "Result should not be null for a valid LocalTime string");
        assertEquals(LocalTime.of(12, 34, 56), result);
    }

    /**
     * When the input string cannot be parsed as a {@code LocalTime} but can be parsed
     * as a {@link ZonedDateTime}, the method should return the time component of that
     * {@code ZonedDateTime}.
     */
    @Test
    void shouldFallBackToZonedDateTimeAndExtractTime() {
        // given
        // ISO‑8601 date‑time with a UTC zone – LocalTime.parse will fail, but ZonedDateTime parsing succeeds.
        String isoDateTime = "2023-01-01T08:15:30Z";
        // when
        LocalTime result = StringConversions.toLocalTime(isoDateTime, converter);
        // then
        assertNotNull(result, "Result should not be null when fallback to ZonedDateTime succeeds");
        ZonedDateTime expectedZdt = ZonedDateTime.parse(isoDateTime);
        assertEquals(expectedZdt.toLocalTime(), result);
    }

    /**
     * When the input string cannot be parsed as either {@code LocalTime} or {@code ZonedDateTime},
     * the method should return {@code null}.
     */
    @Test
    void shouldReturnNullForUnparseableString() {
        // given
        String badString = "not-a-time";
        // when
        LocalTime result = StringConversions.toLocalTime(badString, converter);
        // then
        assertNull(result, "Result should be null for an unparseable string");
    }

    @ParameterizedTest(name = "[{index}] valid LocalTime: {0}")
    @MethodSource("validLocalTimeProvider")
    void shouldParseValidLocalTimeStrings(ArgumentsHolder holder) {
        // given
        String input = stripQuotes(holder.from);
        // when
        LocalTime result = StringConversions.toLocalTime(input, holder.converter);
        // then
        assertNotNull(result, "Result must not be null for a valid LocalTime string");
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "[{index}] fallback ZonedDateTime: {0}")
    @MethodSource("fallbackZonedDateTimeProvider")
    void shouldFallbackToZonedDateTimeWhenLocalTimeParseFails(ArgumentsHolder holder) {
        // given
        String input = stripQuotes(holder.from);
        // when
        LocalTime result = StringConversions.toLocalTime(input, holder.converter);
        // then
        assertNotNull(result, "Result must not be null when fallback succeeds");
        ZonedDateTime expectedZdt = ZonedDateTime.parse(stripQuotes(holder.from));
        assertEquals(expectedZdt.toLocalTime(), result);
    }

    @ParameterizedTest(name = "[{index}] invalid inputs returning null: {0}")
    @MethodSource("invalidInputProvider")
    void shouldReturnNullForUnparseableOrInvalidInputs(ArgumentsHolder holder) {
        // given
        Object input = holder.from.equals("null") ? null : stripQuotes(holder.from);
        // when
        LocalTime result = StringConversions.toLocalTime(input, holder.converter);
        // then
        assertNull(result, "Result should be null for input: " + holder.from);
    }

    @ParameterizedTest(name = "[{index}] null converter with direct parse: {0}")
    @MethodSource("nullConverterDirectParseProvider")
    void shouldParseWhenConverterIsNullAndLocalTimeParseSucceeds(ArgumentsHolder holder) {
        // given
        String input = stripQuotes(holder.from);
        // when
        LocalTime result = StringConversions.toLocalTime(input, holder.converter);
        // then
        assertNotNull(result);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "[{index}] non‑String source object")
    @MethodSource("nonStringSourceProvider")
    void shouldThrowClassCastExceptionWhenSourceIsNotString(ArgumentsHolder holder) {
        // given
        // already an Integer in the data set
        Object nonString = holder.from;
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toLocalTime(nonString, holder.converter));
    }

    @ParameterizedTest(name = "[{index}] converter throws during fallback")
    @MethodSource("converterExceptionDuringFallbackProvider")
    void shouldPropagateExceptionFromConverterDuringFallback(ArgumentsHolder holder) {
        // given
        String input = stripQuotes(holder.from);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toLocalTime(input, holder.converter));
        assertEquals("Converter failure", ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] null converter with fallback parsing")
    @MethodSource("nullConverterWithFallbackProvider")
    void shouldThrowNPEWhenConverterIsNullDuringFallback(ArgumentsHolder holder) {
        // given
        String input = stripQuotes(holder.from);
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toLocalTime(input, holder.converter));
    }
}
