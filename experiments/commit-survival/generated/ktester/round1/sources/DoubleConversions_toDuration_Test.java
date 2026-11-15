package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toDuration_Test {

    /**
     * The mock Converter that will be passed to the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple holder for a single test case.
     */
    private static final class TestCase {

        // The 'from' argument passed to toDuration
        final Object from;

        // Expected ISO‑8601 string (null if an exception is expected)
        final String expectedIso;

        // Expected exception type (null if success)
        final Class<? extends Throwable> expectedException;

        // Human‑readable description for the test runner
        final String description;

        // Whether to pass a non‑null Converter (mock) or null
        final boolean useConverter;

        TestCase(Object from, String expectedIso, Class<? extends Throwable> expectedException, String description, boolean useConverter) {
            this.from = from;
            this.expectedIso = expectedIso;
            this.expectedException = expectedException;
            this.description = description;
            this.useConverter = useConverter;
        }

        @Override
        public String toString() {
            // JUnit displays this in the test name – keep it concise.
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us via the @Mock annotation.
        // Ensure the mock is clean before each test.
        reset(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test never interacts with the Converter.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Supplies all test cases defined in the JSON specification.
     */
    private static Stream<Arguments> testCases() {
        return Stream.of(// -------------------- Valid double conversions --------------------
        new TestCase(10.0, "PT10S", null, "Whole‑second value should produce a Duration of 10 seconds.", false), new TestCase(2.5, "PT2.5S", null, "Fractional seconds (0.5) should be expressed as 2.5 seconds.", false), new TestCase(1.23e-07, "PT0.000000123S", null, "Very small fraction should be converted to 123 nanoseconds.", false), new TestCase(-3.75, "-PT3.75S", null, "Negative fractional value should produce a negative Duration.", false), new TestCase(9.223372036854775e+18, "PT9223372036854775807S", null, "Value just below Long.MAX_VALUE should be truncated to Long.MAX_VALUE seconds with zero nanoseconds.", false), // -------------------- Edge‑case double values --------------------
        new TestCase(Double.NaN, "PT0S", null, "NaN casts to 0L for both seconds and nanoseconds, yielding a zero Duration.", false), new TestCase(Double.POSITIVE_INFINITY, null, ArithmeticException.class, "Positive infinity overflows when constructing a Duration; an ArithmeticException is expected.", false), new TestCase(Double.NEGATIVE_INFINITY, null, ArithmeticException.class, "Negative infinity also overflows, resulting in an ArithmeticException.", false), // -------------------- Invalid inputs --------------------
        new TestCase(null, null, NullPointerException.class, "Passing null for the source object causes a NullPointerException during the cast.", false), new TestCase("not a double", null, ClassCastException.class, "A non‑Double object (e.g., a String) should trigger a ClassCastException.", false), // -------------------- Fractional rounding and edge nanoseconds --------------------
        new TestCase(0.999999999, "PT0.999999999S", null, "Value just below one second should produce 0 seconds and 999,999,999 nanoseconds.", false), new TestCase(1.1234567895, "PT1.123456789S", null, "Fractional part with more than 9 decimal places is truncated to nanoseconds (123,456,789 ns).", false), new TestCase(-1e-09, "-PT0.000000001S", null, "Negative sub‑nanosecond value results in a negative duration of one nanosecond.", false), new TestCase(-0.0, "PT0S", null, "Negative zero is treated as zero, yielding a zero‑length Duration.", false), new TestCase(1e-12, "PT0S", null, "A value smaller than one nanosecond is truncated to zero nanoseconds.", false), new TestCase(1.9999999999, "PT1.999999999S", null, "Fraction just under two seconds is truncated, leaving 1 second and 999,999,999 ns.", false), // -------------------- Overflow with nanosecond adjustment --------------------
        new TestCase(9.223372036854776e+18, null, ArithmeticException.class, "Value exceeds Long.MAX_VALUE after casting; adding a positive nanosecond adjustment overflows, causing an ArithmeticException.", false), // -------------------- Converter handling (ignored) --------------------
        new TestCase(5.0, "PT5S", null, "A non‑null Converter argument is ignored; conversion result must be identical to the null‑converter case.", true), // -------------------- Additional valid whole‑second conversion --------------------
        new TestCase(5.0, "PT5S", null, "Simple whole‑second value not covered by existing tests.", false), // -------------------- Overflow with extreme double values --------------------
        new TestCase(Double.MAX_VALUE, null, ArithmeticException.class, "The maximum finite double value overflows when Duration.ofSeconds is called because both seconds and nanosecond adjustment are capped at Long.MAX_VALUE, causing an ArithmeticException.", false), new TestCase(-Double.MAX_VALUE, null, ArithmeticException.class, "The most negative finite double value overflows similarly (seconds become Long.MIN_VALUE and nanosecond adjustment overflows), resulting in an ArithmeticException.", false)).map(Arguments::of);
    }

    @Test
    void toDuration_positiveWholeAndFraction() {
        // given
        // 12345 seconds + 0.6789 seconds
        Double input = 12345.6789;
        long expectedSeconds = 12345L;
        long expectedNanos = (long) ((input - expectedSeconds) * 1_000_000_000L);
        Duration expected = Duration.ofSeconds(expectedSeconds, expectedNanos);
        // when
        Duration actual = DoubleConversions.toDuration(input, mockConverter);
        // then
        assertEquals(expected, actual, "Duration should match the positive whole + fraction case");
    }

    @Test
    void toDuration_zero() {
        // given
        Double input = 0.0;
        Duration expected = Duration.ofSeconds(0L, 0L);
        // when
        Duration actual = DoubleConversions.toDuration(input, mockConverter);
        // then
        assertEquals(expected, actual, "Duration of zero should be zero seconds and zero nanos");
    }

    @Test
    void toDuration_negativeWholeAndFraction() {
        // given
        // -42 seconds - 0.25 seconds
        Double input = -42.25;
        // truncates toward zero -> -42
        long expectedSeconds = (long) input;
        // -250_000_000
        long expectedNanos = (long) ((input - expectedSeconds) * 1_000_000_000L);
        Duration expected = Duration.ofSeconds(expectedSeconds, expectedNanos);
        // when
        Duration actual = DoubleConversions.toDuration(input, mockConverter);
        // then
        assertEquals(expected, actual, "Duration should correctly handle negative values with fraction");
    }

    @Test
    void toDuration_largeValue() {
        // given
        // close to Long.MAX_VALUE seconds with fraction
        Double input = 9_223_372_036_854.775807;
        // truncates to 9_223_372_036_854
        long expectedSeconds = (long) input;
        long expectedNanos = (long) ((input - expectedSeconds) * 1_000_000_000L);
        Duration expected = Duration.ofSeconds(expectedSeconds, expectedNanos);
        // when
        Duration actual = DoubleConversions.toDuration(input, mockConverter);
        // then
        assertEquals(expected, actual, "Duration should handle very large double values");
    }

    @Test
    void toDuration_invalidType_throwsClassCastException() {
        // given
        Object notADouble = "not a double";
        // when / then
        assertThrows(ClassCastException.class, () -> DoubleConversions.toDuration(notADouble, mockConverter), "Passing a non‑Double object should result in ClassCastException");
    }

    /**
     * Parameterized test that iterates over all JSON‑defined cases.
     *
     * <p>Each {@link TestCase} contains:
     * <ul>
     *   <li>the {@code from} argument (may be {@code null} or a non‑Double object),</li>
     *   <li>the expected ISO‑8601 duration string or the expected exception type,</li>
     *   <li>a description used only for readability.</li>
     * </ul>
     *
     * The test asserts either equality with the expected {@link Duration}
     * or that the correct exception is thrown.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("testCases")
    void toDuration_parameterized(TestCase tc) {
        Object from = tc.from;
        Converter converter = tc.useConverter ? mockConverter : null;
        if (tc.expectedException != null) {
            // Expect an exception – verify the exact type.
            Throwable thrown = assertThrows(tc.expectedException, () -> DoubleConversions.toDuration(from, converter), tc.description);
            // For ArithmeticException we also want to ensure the message mentions overflow.
            if (tc.expectedException == ArithmeticException.class) {
                assertTrue(thrown.getMessage().contains("overflow") || thrown.getMessage().contains("Duration") || thrown.getMessage().contains("cannot be represented"), "ArithmeticException should indicate an overflow condition");
            }
        } else {
            // No exception expected – compute the expected Duration from the ISO‑8601 string.
            Duration expected = Duration.parse(tc.expectedIso);
            Duration actual = DoubleConversions.toDuration(from, converter);
            assertEquals(expected, actual, tc.description);
        }
    }
}
