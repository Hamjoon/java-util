package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toInstant_Test {

    // The Converter is not used by the method under test, but we provide a mock to satisfy the signature.
    @Mock
    private Converter mockConverter;

    /**
     * Simple holder for a single test scenario.
     */
    private static final class TestCase {

        // The value passed as the first argument to toInstant
        final Object from;

        // Null if a normal result is expected
        final Class<? extends Throwable> expectedException;

        // Null if an exception is expected
        final Instant expectedInstant;

        // Human‑readable description used in test names / messages
        final String description;

        TestCase(Object from, Class<? extends Throwable> expectedException, Instant expectedInstant, String description) {
            this.from = from;
            this.expectedException = expectedException;
            this.expectedInstant = expectedInstant;
            this.description = description;
        }

        @Override
        public String toString() {
            // JUnit uses this for the display name of each parameterised test.
            return description;
        }
    }

    /**
     * Supplies all test cases from the JSON specification.
     */
    private static Stream<Arguments> toInstantTestCases() {
        // Helper reference to the (mock) converter – the method does not use it.
        // we can also pass mockConverter later; null works for all cases.
        Converter converter = null;
        return Stream.of(// -----------------------------------------------------------------
        // Normal conversions (group: toInstant)
        // -----------------------------------------------------------------
        new TestCase(new BigDecimal("1625097600"), null, Instant.parse("2021-07-01T00:00:00Z"), "Whole‑second value – verifies that a BigDecimal without a fractional part is converted to an Instant with zero nanoseconds."), new TestCase(new BigDecimal("1625097600.123456789"), null, Instant.parse("2021-07-01T00:00:00.123456789Z"), "Positive fractional seconds with exactly nine decimal places – checks correct nanosecond extraction."), new TestCase(new BigDecimal("1625097600.5"), null, Instant.parse("2021-07-01T00:00:00.500000000Z"), "Fractional part with fewer than nine digits – ensures the method pads the nanoseconds to the right scale."), new TestCase(new BigDecimal("-1.75"), null, // -1.75 => -2 seconds + 250_000_000 nanos (Instant normalises)
        Instant.ofEpochSecond(-3L, 250_000_000L), "Negative seconds with a fractional part – validates handling of negative remainders and proper normalization by Instant."), new TestCase(new BigDecimal("9223372036854775807"), null, // Long.MAX_VALUE seconds, zero nanos
        Instant.ofEpochSecond(Long.MAX_VALUE, 0L), "Maximum long value as seconds – tests conversion of very large epoch seconds without overflow."), new TestCase(null, NullPointerException.class, null, "Null `from` argument – the cast to BigDecimal should throw a NullPointerException."), new TestCase(// Not a BigDecimal – will cause a ClassCastException when the method casts.
        (Object) "not a bigdecimal", ClassCastException.class, null, "`from` is not a BigDecimal – the explicit cast should raise a ClassCastException."), new TestCase(new BigDecimal("0.1234567899"), null, // extra digit after 9 decimal places is truncated
        Instant.ofEpochSecond(0L, 123_456_789L), "Fractional part with more than nine digits – the extra digits are truncated when converting to nanoseconds."), new TestCase(new BigDecimal("0"), null, Instant.ofEpochSecond(0L, 0L), "Zero epoch seconds – verifies that a plain zero BigDecimal yields the Unix epoch instant."), new TestCase(new BigDecimal("0.000000001"), null, Instant.ofEpochSecond(0L, 1L), "Minimum positive nanosecond – checks that a fractional part representing exactly one nanosecond is handled correctly."), new TestCase(new BigDecimal("0.00000123"), null, Instant.ofEpochSecond(0L, 1_230L), "Fractional seconds with leading zeros – ensures proper left‑padding of nanoseconds to nine digits."), new TestCase(new BigDecimal("0.0000000004"), null, Instant.ofEpochSecond(0L, 0L), "Fractional part smaller than one nanosecond – the extra precision is truncated, resulting in zero nanoseconds."), new TestCase(new BigDecimal("-0.5"), null, // -0.5 => -1 second + 500_000_000 nanos
        Instant.ofEpochSecond(-1L, 500_000_000L), "Negative fractional seconds without an integer part – tests the adjustment logic when nanoseconds are negative."), new TestCase(new BigDecimal("-0.0"), null, Instant.ofEpochSecond(0L, 0L), "Negative zero – confirms that \"-0.0\" is treated the same as zero."), new TestCase(new BigDecimal("-9223372036854775808"), null, // Long.MIN_VALUE seconds, zero nanos
        Instant.ofEpochSecond(Long.MIN_VALUE, 0L), "Minimum long value as seconds – validates conversion at the lower bound of `long` without overflow."), new TestCase(new BigDecimal("1.0000000019"), null, // fraction part > 9 digits, truncates to 1 nanosecond
        Instant.ofEpochSecond(1L, 1L), "Fractional part with more than nine digits where extra digits are truncated – ensures correct truncation (no rounding)."), new TestCase(new BigDecimal("-1.0000000019"), null, // -1.0000000019 => -2 seconds + 999_999_999 nanos (because -0.0000000019 truncated)
        Instant.ofEpochSecond(-2L, 999_999_999L), "Negative seconds with a fractional part that has more than nine digits – tests negative nanosecond handling and proper carry‑over."), // -----------------------------------------------------------------
        // Out‑of‑range conversions (group: toInstant_outOfRange)
        // -----------------------------------------------------------------
        new TestCase(new BigDecimal("31556889864403200"), DateTimeException.class, null, "Epoch seconds just beyond Instant.MAX (Instant.MAX seconds + 1) – the call to Instant.ofEpochSecond should throw a DateTimeException because the resulting instant is outside the supported range."), new TestCase(new BigDecimal("-31557014167219201"), DateTimeException.class, null, "Epoch seconds just before Instant.MIN (Instant.MIN seconds - 1) – the method should raise a DateTimeException due to under‑flow beyond the minimum instant supported.")).map(Arguments::of);
    }

    @Test
    void toInstant_WithWholeSeconds_ReturnsExactInstant() {
        // given
        // 10 seconds, no fractional part
        BigDecimal input = BigDecimal.valueOf(10L);
        // when
        Instant result = BigDecimalConversions.toInstant(input, mockConverter);
        // then
        assertEquals(Instant.ofEpochSecond(10L, 0L), result, "Instant should represent exactly 10 seconds since epoch");
    }

    @Test
    void toInstant_WithFractionalSeconds_ReturnsInstantWithNanos() {
        // given
        // 10 seconds + 0.5 second
        BigDecimal input = new BigDecimal("10.5");
        // when
        Instant result = BigDecimalConversions.toInstant(input, mockConverter);
        // then
        assertEquals(Instant.ofEpochSecond(10L, 500_000_000L), result, "Instant should contain 500,000,000 nanoseconds for the .5 fraction");
    }

    @Test
    void toInstant_WithNegativeFractionalSeconds_ReturnsNormalizedInstant() {
        // given
        // -2 seconds and -0.75 second
        BigDecimal input = new BigDecimal("-2.75");
        // when
        Instant result = BigDecimalConversions.toInstant(input, mockConverter);
        // then
        // Instant.ofEpochSecond normalises negative nanos, so we expect -3 seconds + 250,000,000 nanos
        Instant expected = Instant.ofEpochSecond(-3L, 250_000_000L);
        assertEquals(expected, result, "Instant should correctly handle negative fractional seconds");
    }

    @Test
    void toInstant_WithLargeNumberOfSeconds_ReturnsCorrectInstant() {
        // given
        // large seconds with fraction
        BigDecimal input = new BigDecimal("1234567890.123456789");
        // when
        Instant result = BigDecimalConversions.toInstant(input, mockConverter);
        // then
        long expectedSeconds = 1_234_567_890L;
        // 0.123456789 * 1_000_000_000
        long expectedNanos = 123_456_789L;
        assertEquals(Instant.ofEpochSecond(expectedSeconds, expectedNanos), result);
    }

    /**
     * Parameterised test that runs through every scenario described in the JSON test matrix.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("toInstantTestCases")
    void toInstant_VariousScenarios(TestCase tc) {
        if (tc.expectedException != null) {
            // Expect an exception – the actual exception type must match exactly.
            Throwable thrown = assertThrows(tc.expectedException, () -> BigDecimalConversions.toInstant(tc.from, tc.converter), tc.description);
            // Optional: verify the message contains something useful (not required for this kata).
        } else {
            // Normal conversion – compare the resulting Instant with the expected one.
            Instant result = BigDecimalConversions.toInstant(tc.from, tc.converter);
            assertEquals(tc.expectedInstant, result, () -> tc.description + " – expected " + tc.expectedInstant + " but got " + result);
        }
    }
}
