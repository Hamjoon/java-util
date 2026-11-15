package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

class OffsetTimeConversions_toBigDecimal_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter does not need any special configuration for this test
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// 12:34:56.789+02:00  → 10:34:56.789Z → 38096.789 seconds
        Arguments.of(OffsetTime.parse("12:34:56.789+02:00"), new BigDecimal("38096.789")), // 00:00:00Z → epoch instant → 0 seconds
        Arguments.of(OffsetTime.parse("00:00:00Z"), new BigDecimal("0")), // 23:59:59.999999999-05:30 → 1970‑01‑02T05:29:59.999999999Z
        // = 106259 seconds + 0.999999999
        Arguments.of(OffsetTime.parse("23:59:59.999999999-05:30"), new BigDecimal("106259.999999999")));
    }

    private static Stream<Arguments> boundaryOffsetsProvider() {
        return Stream.of(// 00:00:00+02:00 → 1969‑12‑31T22:00:00Z → -7200 seconds
        Arguments.of(OffsetTime.parse("00:00:00+02:00"), new BigDecimal("-7200")), // 00:00:00+18:00 → 1969‑12‑31T06:00:00Z → -64800 seconds
        Arguments.of(OffsetTime.parse("00:00:00+18:00"), new BigDecimal("-64800")), // 23:59:59-18:00 → 1970‑01‑02T17:59:59Z → 151199 seconds
        Arguments.of(OffsetTime.parse("23:59:59-18:00"), new BigDecimal("151199")));
    }

    private static Stream<Arguments> invalidInputsProvider() {
        return Stream.of(// Null source object → NullPointerException (cast on null)
        Arguments.of(null, NullPointerException.class), // Wrong type (String) → ClassCastException
        Arguments.of("2023-01-01", ClassCastException.class));
    }

    @Test
    void toBigDecimal_WithUtcOffset_ReturnsCorrectValue() {
        // given: an OffsetTime at 12:30:15.250 with UTC offset
        OffsetTime offsetTime = OffsetTime.of(12, 30, 15, 250_000_000, ZoneOffset.UTC);
        // when: converting to BigDecimal via the static method
        BigDecimal result = OffsetTimeConversions.toBigDecimal(offsetTime, converter);
        // then: the result should equal seconds + nanos/1_000_000_000
        // 12*3600 + 30*60 + 15 = 45015
        long expectedSeconds = offsetTime.toInstant().getEpochSecond();
        // 250_000_000
        int expectedNanos = offsetTime.toInstant().getNano();
        BigDecimal expected = BigDecimal.valueOf(expectedSeconds).add(BigDecimal.valueOf(expectedNanos).divide(BigDecimalConversions.BILLION));
        assertEquals(expected, result, "BigDecimal conversion of OffsetTime is incorrect");
    }

    @Test
    void toBigDecimal_WithPositiveOffset_ReturnsAdjustedEpoch() {
        // given: an OffsetTime at 03:00:00.000 with a +02:00 offset
        OffsetTime offsetTime = OffsetTime.of(3, 0, 0, 0, ZoneOffset.ofHours(2));
        // when: converting to BigDecimal
        BigDecimal result = OffsetTimeConversions.toBigDecimal(offsetTime, converter);
        // then: the instant represented is 01:00:00 UTC on the epoch day
        // should be 3600 (1 hour)
        long expectedSeconds = offsetTime.toInstant().getEpochSecond();
        // 0
        int expectedNanos = offsetTime.toInstant().getNano();
        BigDecimal expected = BigDecimal.valueOf(expectedSeconds).add(BigDecimal.valueOf(expectedNanos).divide(BigDecimalConversions.BILLION));
        assertEquals(expected, result, "BigDecimal conversion should account for the offset");
    }

    // ---------- valid conversions (including fractional seconds) ----------
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("validConversionsProvider")
    void toBigDecimal_ValidConversions(OffsetTime input, BigDecimal expected) {
        // The converter argument is not used by the implementation, so we can pass null.
        BigDecimal actual = OffsetTimeConversions.toBigDecimal(input, null);
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but got " + actual);
    }

    // ---------- boundary offset cases ----------
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("boundaryOffsetsProvider")
    void toBigDecimal_BoundaryOffsets(OffsetTime input, BigDecimal expected) {
        BigDecimal actual = OffsetTimeConversions.toBigDecimal(input, null);
        assertEquals(0, expected.compareTo(actual), () -> "Boundary offset conversion failed for " + input + ". Expected " + expected + " but got " + actual);
    }

    // ---------- invalid input cases ----------
    @ParameterizedTest(name = "[{index}] from={0} expects {1}")
    @MethodSource("invalidInputsProvider")
    void toBigDecimal_InvalidInputs(Object input, Class<? extends Throwable> expectedException) {
        // The method casts the input to OffsetTime, so any non‑OffsetTime (or null) should throw.
        assertThrows(expectedException, () -> OffsetTimeConversions.toBigDecimal(input, null), () -> "Expected " + expectedException.getSimpleName() + " for input: " + input);
    }
}
