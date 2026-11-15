package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDuration_Test {

    /**
     * Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * Custom {@link Number} implementation used to test the fallback branch.
     * It simply returns the provided string from {@code toString()} and parses
     * numeric values for the abstract {@code Number} methods.
     */
    private static final class CustomNumber extends Number {

        private final String value;

        CustomNumber(String value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return (int) doubleValue();
        }

        @Override
        public long longValue() {
            return (long) doubleValue();
        }

        @Override
        public float floatValue() {
            return (float) doubleValue();
        }

        @Override
        public double doubleValue() {
            return Double.parseDouble(value);
        }

        @Override
        public short shortValue() {
            return (short) doubleValue();
        }

        @Override
        public byte byteValue() {
            return (byte) doubleValue();
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not need any special configuration for these tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> wholeNumberProvider() {
        return Stream.of(// Long
        Arguments.of(12_345L, "PT12.345S"), // Integer (negative)
        Arguments.of(-500, "-PT0.5S"), // BigInteger
        Arguments.of(new BigInteger("10000000000"), "PT115DT17H46M40S"), // AtomicLong (zero)
        Arguments.of(new AtomicLong(0L), "PT0S"), // AtomicInteger (negative)
        Arguments.of(new AtomicInteger(-123), "-PT0.123S"));
    }

    private static Stream<Arguments> bigDecimalProvider() {
        return Stream.of(Arguments.of(new BigDecimal("123.456"), "PT123.456S"), Arguments.of(new BigDecimal("-0.001"), "-PT0.001S"), Arguments.of(BigDecimal.ZERO, "PT0S"));
    }

    private static Stream<Arguments> floatingPointProvider() {
        return Stream.of(Arguments.of(2.5d, "PT2.5S"), Arguments.of(-1.75f, "-PT1.75S"), Arguments.of(1e-9d, "PT0.000000001S"));
    }

    private static Stream<Arguments> fallbackProvider() {
        return Stream.of(// Short (2500 seconds = 41 minutes 40 seconds)
        Arguments.of(Short.valueOf((short) 2500), "PT41M40S"), // Custom Number subclass – string parsed as seconds, truncated to nanoseconds
        Arguments.of(new CustomNumber("6.283185307179586"), "PT6.283185307S"));
    }

    /* -------------------------------------------------------------
     *  Whole‑number types – interpreted as milliseconds
     * ------------------------------------------------------------- */
    @Test
    void wholeNumberTypesAreMilliseconds() {
        // Long
        assertEquals(Duration.ofMillis(1_234L), NumberConversions.toDuration(1_234L, converter));
        // Integer
        assertEquals(Duration.ofMillis(567L), NumberConversions.toDuration(Integer.valueOf(567), converter));
        // BigInteger
        assertEquals(Duration.ofMillis(999L), NumberConversions.toDuration(new BigInteger("999"), converter));
        // AtomicLong
        assertEquals(Duration.ofMillis(2_500L), NumberConversions.toDuration(new AtomicLong(2_500L), converter));
        // AtomicInteger
        assertEquals(Duration.ofMillis(3_000L), NumberConversions.toDuration(new AtomicInteger(3_000), converter));
    }

    /* -------------------------------------------------------------
     *  BigDecimal – interpreted as seconds (fractional part → nanos)
     * ------------------------------------------------------------- */
    @Test
    void bigDecimalIsConvertedToSecondsAndNanos() {
        Duration expected = Duration.parse(expectedIso);
        Duration actual = NumberConversions.toDuration(from, converter);
        assertEquals(expected, actual, () -> "Expected " + expected + " but got " + actual + " for BigDecimal: " + from);
    }

    /* -------------------------------------------------------------
     *  Double – interpreted as seconds (fractional part → nanos)
     * ------------------------------------------------------------- */
    @Test
    void doubleIsConvertedToSecondsAndNanos() {
        // 2 seconds + 0.75 sec
        Double d = 2.75;
        Duration expected = Duration.ofSeconds(2, 750_000_000L);
        assertEquals(expected, NumberConversions.toDuration(d, converter));
    }

    /* -------------------------------------------------------------
     *  Float – interpreted as seconds (fractional part → nanos)
     * ------------------------------------------------------------- */
    @Test
    void floatIsConvertedToSecondsAndNanos() {
        // 1 second + 0.2 sec
        Float f = 1.2f;
        Duration expected = Duration.ofSeconds(1, 200_000_000L);
        assertEquals(expected, NumberConversions.toDuration(f, converter));
    }

    /* -------------------------------------------------------------
     *  Fallback – any other Number type uses its string representation
     * ------------------------------------------------------------- */
    @Test
    void fallbackUsesStringRepresentationAsSeconds() {
        // Short (not covered by the “whole number” branch)
        Short s = 5;
        Duration expected = Duration.ofSeconds(5);
        assertEquals(expected, NumberConversions.toDuration(s, converter));
        // Byte (also falls back)
        Byte b = 7;
        Duration expectedByte = Duration.ofSeconds(7);
        assertEquals(expectedByte, NumberConversions.toDuration(b, converter));
    }

    /* -------------------------------------------------------------
     *  Negative values – ensure sign is preserved across all branches
     * ------------------------------------------------------------- */
    @Test
    void negativeValuesAreHandledCorrectly() {
        // Whole‑number (milliseconds)
        assertEquals(Duration.ofMillis(-1_000L), NumberConversions.toDuration(-1_000L, converter));
        // BigDecimal (seconds)
        BigDecimal bdNeg = new BigDecimal("-2.5");
        Duration expectedNeg = Duration.ofSeconds(-2, -500_000_000L);
        assertEquals(expectedNeg, NumberConversions.toDuration(bdNeg, converter));
        // Double (seconds)
        Double dNeg = -3.125;
        Duration expectedDNeg = Duration.ofSeconds(-3, -125_000_000L);
        assertEquals(expectedDNeg, NumberConversions.toDuration(dNeg, converter));
    }

    /* -------------------------------------------------------------
     *  Floating‑point types – interpreted as seconds (fractional part → nanos)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("floatingPointProvider")
    void floatingPointTypesAreConvertedToSecondsAndNanos(Number from, String expectedIso) {
        Duration expected = Duration.parse(expectedIso);
        Duration actual = NumberConversions.toDuration(from, converter);
        assertEquals(expected, actual, () -> "Expected " + expected + " but got " + actual + " for input: " + from);
    }

    /* -------------------------------------------------------------
     *  Error handling – null source should raise NPE
     * ------------------------------------------------------------- */
    @Test
    void nullInputThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toDuration(null, converter), "Expected NullPointerException when source object is null");
    }

    /* -------------------------------------------------------------
     *  Non‑Number input – should raise ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void nonNumberInputThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> NumberConversions.toDuration("123", converter), "Expected ClassCastException when source object does not implement Number");
    }
}
