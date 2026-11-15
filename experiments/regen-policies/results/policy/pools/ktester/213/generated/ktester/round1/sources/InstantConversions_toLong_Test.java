package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLong_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up, but nullify the reference for completeness.
        converter = null;
    }

    /**
     * Supplies all test cases described in the JSON specification.
     *
     * Each argument consists of:
     *   1. {@code from} – the object passed to {@code toLong}.
     *   2. {@code expectedOrException} – either a {@link Long} with the expected result
     *      or a {@link Class} of the exception that must be thrown.
     *   3. {@code description} – a short textual description (used as the test name).
     */
    private static Stream<Arguments> toLongProvider() {
        // a dummy Converter instance to verify that the method truly ignores the argument
        Converter dummyConverter = new Converter(new ConverterOptions());
        return Stream.of(// epoch instant (1970‑01‑01T00:00:00Z) → 0
        Arguments.of(Instant.ofEpochMilli(0), 0L, "Convert the epoch Instant (1970‑01‑01T00:00:00Z) to its epoch‑millisecond representation, which must be 0."), // positive offset
        Arguments.of(Instant.ofEpochMilli(1_234_567_890L), 1_234_567_890L, "Convert a positive‑offset Instant to its epoch‑millisecond value; verifies normal forward conversion."), // negative offset
        Arguments.of(Instant.ofEpochMilli(-1_000L), -1_000L, "Convert a negative‑offset Instant (before the epoch) to ensure the method correctly returns negative epoch milliseconds."), // maximum long value
        Arguments.of(Instant.ofEpochMilli(Long.MAX_VALUE), Long.MAX_VALUE, "Convert the maximum representable Instant (far future) to confirm that the method handles the upper bound without overflow."), // null source → NPE
        Arguments.of(null, NullPointerException.class, "Passing a null `from` argument should cause a NullPointerException because the cast to Instant fails on a null reference."), // wrong type (String) → ClassCastException
        Arguments.of("2023-01-01T00:00:00Z", ClassCastException.class, "Passing an object that is not an Instant (a String) must result in a ClassCastException, as the method directly casts the argument to Instant."), // wrong type (Date) → ClassCastException
        Arguments.of(new java.util.Date(), ClassCastException.class, "Even though `java.util.Date` can represent an instant, the method expects an `Instant` object; casting a Date should throw ClassCastException."), // minimum long value
        Arguments.of(Instant.ofEpochMilli(Long.MIN_VALUE), Long.MIN_VALUE, "Convert the minimum representable epoch‑millisecond Instant to verify correct handling of the lower bound without overflow."), // sub‑millisecond part should be truncated (10 s + 500 µs)
        Arguments.of(Instant.ofEpochSecond(10, 500_000), 10_000L, "Instant with 10 seconds and 500 µs nanoseconds; toLong must truncate sub‑millisecond part, yielding 10 000 ms."), // less than one millisecond → 0
        Arguments.of(Instant.ofEpochSecond(0, 999_999), 0L, "Instant less than one millisecond (999 999 ns); conversion should floor to 0 ms."), // Instant.MAX → DateTimeException
        Arguments.of(Instant.MAX, DateTimeException.class, "Instant.MAX is beyond the range representable in epoch‑milliseconds; toLong must throw DateTimeException."), // Instant.MIN → DateTimeException
        Arguments.of(Instant.MIN, DateTimeException.class, "Instant.MIN is before the minimum epoch‑millisecond value; conversion should raise DateTimeException."), // non‑null converter should be ignored
        Arguments.of(Instant.ofEpochMilli(42), 42L, "Pass a non‑null Converter instance to confirm the method ignores the converter argument and still returns the correct millisecond value."));
    }

    @Test
    void toLong_returnsEpochMilli_forGivenInstant() {
        // GIVEN
        // arbitrary epoch milli value
        long expected = 1_617_283_945_123L;
        Instant instant = Instant.ofEpochMilli(expected);
        // WHEN
        long actual = InstantConversions.toLong(instant, converter);
        // THEN
        assertEquals(expected, actual, "InstantConversions.toLong should return the same epoch milli as the source Instant");
    }

    @Test
    void toLong_returnsEpochMilli_forCurrentInstant() {
        // GIVEN
        Instant now = Instant.now();
        long expected = now.toEpochMilli();
        // WHEN
        long actual = InstantConversions.toLong(now, converter);
        // THEN
        assertEquals(expected, actual, "InstantConversions.toLong should correctly convert the current Instant to epoch milli");
    }

    @Test
    void toLong_throwsNullPointerException_whenSourceIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toLong(nullSource, converter), "Calling toLong with a null source should throw NullPointerException because of the cast to Instant");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("toLongProvider")
    void toLong_parameterizedTests(Object from, Object expectedOrException, String description) {
        // The method under test does not use the Converter argument, but we pass a dummy when needed.
        Converter usedConverter = // any non‑null works
        (from == null) ? // any non‑null works
        converter : // still irrelevant
        (from instanceof String || from instanceof java.util.Date) ? // still irrelevant
        converter : (from == Instant.ofEpochMilli(42)) ? new Converter(new ConverterOptions()) : converter;
        if (expectedOrException instanceof Class<?>) {
            Class<?> expectedException = (Class<?>) expectedOrException;
            assertThrows(expectedException, () -> InstantConversions.toLong(from, usedConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + description);
        } else {
            long expected = (Long) expectedOrException;
            long actual = InstantConversions.toLong(from, usedConverter);
            assertEquals(expected, actual, () -> "Incorrect conversion for input: " + description);
        }
    }
}
