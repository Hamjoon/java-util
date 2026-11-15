package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLong_Test {

    /**
     * A mock {@link Converter}. The method under test does not use this argument,
     *  but it must be supplied to match the signature.
     */
    @Mock
    Converter mockConverter;

    /**
     * Supplies a stream of {@link Arguments} where each argument consists of:
     * <ul>
     *   <li>the {@link Instant} to convert</li>
     *   <li>the expected epoch‑millisecond value</li>
     * </ul>
     */
    private static Stream<Arguments> provideValidInstants() {
        return Stream.of(// Current moment – value is computed at test execution time
        Arguments.of(Instant.now(), Instant.now().toEpochMilli()), // The epoch (1970‑01‑01T00:00:00Z)
        Arguments.of(Instant.ofEpochSecond(0, 0), 0L), // Positive epoch second
        Arguments.of(Instant.ofEpochSecond(123_456_789L, 0), 123_456_789_000L), // Negative epoch second (one second before epoch)
        Arguments.of(Instant.ofEpochSecond(-1, 0), -1_000L), // Nanosecond part is ignored for the millisecond value
        Arguments.of(Instant.ofEpochSecond(0, 999_999_999L), 0L), // Largest instant that still fits into a signed long milliseconds
        // (seconds = Long.MAX_VALUE / 1000, nanos = 999_999_999)
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE / 1000, 999_999_999L), Instant.ofEpochSecond(Long.MAX_VALUE / 1000, 999_999_999L).toEpochMilli()), // Smallest instant that still fits into a signed long milliseconds
        // (seconds = Long.MIN_VALUE / 1000, nanos = 0)
        Arguments.of(Instant.ofEpochSecond(Long.MIN_VALUE / 1000, 0L), Instant.ofEpochSecond(Long.MIN_VALUE / 1000, 0L).toEpochMilli()), // Exactly one millisecond expressed as nanoseconds
        Arguments.of(Instant.ofEpochSecond(0, 1_000_000L), 1L), // 1.5 milliseconds expressed as nanoseconds (truncates to 1)
        Arguments.of(Instant.ofEpochSecond(0, 1_500_000_000L), 1L), // Explicit millisecond fraction via ISO‑8601 parsing
        Arguments.of(Instant.parse("1970-01-01T00:00:00.123Z"), 123L));
    }

    /**
     * Supplies a stream of {@link Arguments} where each argument consists of:
     * <ul>
     *   <li>the invalid {@code from} object</li>
     *   <li>the expected exception class</li>
     * </ul>
     */
    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(// Null source – the cast to Instant triggers NPE
        Arguments.of(null, NullPointerException.class), // Incompatible type (String) – cast to Instant triggers ClassCastException
        Arguments.of("2023-01-01T00:00:00Z", ClassCastException.class), // Instant whose epoch second is too large for milliseconds – DateTimeException
        Arguments.of(// This instant overflows when converting to milliseconds
        Instant.ofEpochSecond(Long.MAX_VALUE / 1000 + 1, 0), DateTimeException.class));
    }

    @Test
    void toLong_givenInstant_returnsEpochMilli() {
        // GIVEN
        Instant now = Instant.now();
        long expected = now.toEpochMilli();
        // WHEN
        long actual = InstantConversions.toLong(now, mockConverter);
        // THEN
        assertEquals(expected, actual, "InstantConversions.toLong should return the Instant's epoch milli value");
    }

    @Test
    void toLong_givenNull_throwsNullPointerException() {
        // GIVEN
        Object nullInstant = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toLong(nullInstant, mockConverter), "Passing a null source should result in a NullPointerException");
    }

    /**
     * Parameterised test for all valid {@link Instant} inputs.
     * The expected value is calculated with {@link Instant#toEpochMilli()}
     * to avoid hard‑coding large numbers that could be mis‑typed.
     */
    @ParameterizedTest(name = "[{index}] toLong({0}) = {1}")
    @MethodSource("provideValidInstants")
    void toLong_givenValidInstant_returnsEpochMilli(Instant instant, long expected) {
        // WHEN
        long actual = InstantConversions.toLong(instant, mockConverter);
        // THEN
        assertEquals(expected, actual, () -> "Expected epoch milli for " + instant + " to be " + expected);
    }

    /**
     * Parameterised test for inputs that should cause an exception.
     * The expected exception class is asserted via {@link Assertions#assertThrows}.
     */
    @ParameterizedTest(name = "[{index}] toLong({0}) throws {1}")
    @MethodSource("provideInvalidInputs")
    void toLong_givenInvalidInput_throwsException(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> InstantConversions.toLong(from, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
