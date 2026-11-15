package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toLong_Test {

    /**
     * Mocked {@link Converter} – not used by the method under test.
     */
    @Mock
    Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock automatically via the @Mock annotation.
        // Ensure the mock has no unexpected interactions.
        clearInvocations(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test never touched the converter mock.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Supplies all test cases defined in the JSON document.
     *
     * @return a stream of arguments for the parameterized test
     */
    private static Stream<Arguments> provideToLongCases() {
        // Helper converter instance for the case where a non‑null Converter is supplied.
        Converter nonNullConverter = new Converter() {
        };
        return Stream.of(// Positive duration – 5 seconds → 5,000 ms
        Arguments.of("Positive duration – 5 seconds should be converted to 5,000 milliseconds.", Duration.ofSeconds(5), null, 5_000L), // Zero duration → 0 ms
        Arguments.of("Zero duration should return 0 milliseconds.", Duration.ZERO, null, 0L), // Negative duration – -3 seconds → -3,000 ms
        Arguments.of("Negative duration – -3 seconds should be converted to -3,000 milliseconds.", Duration.ofSeconds(-3), null, -3_000L), // Large duration – 1 day → 86,400,000 ms
        Arguments.of("Large duration – 1 day equals 86,400,000 milliseconds.", Duration.ofDays(1), null, 86_400_000L), // Null 'from' argument → NullPointerException
        Arguments.of("Passing a null 'from' argument should result in a NullPointerException.", null, null, NullPointerException.class), // Non‑Duration object → ClassCastException
        Arguments.of("Providing an object that is not a java.time.Duration should cause a ClassCastException.", "not a duration", null, ClassCastException.class), // Maximum positive millisecond value – no overflow
        Arguments.of("Maximum positive millisecond value – Duration.ofMillis(Long.MAX_VALUE) should return Long.MAX_VALUE without overflow.", Duration.ofMillis(Long.MAX_VALUE), null, Long.MAX_VALUE), // Overflow on conversion – seconds too large for millis
        Arguments.of("Overflow on conversion – Duration.ofSeconds(Long.MAX_VALUE) exceeds the range of long milliseconds and should throw ArithmeticException.", Duration.ofSeconds(Long.MAX_VALUE), null, ArithmeticException.class), // Overflow on negative conversion
        Arguments.of("Overflow on negative conversion – Duration.ofSeconds(Long.MIN_VALUE) cannot be represented in milliseconds and should throw ArithmeticException.", Duration.ofSeconds(Long.MIN_VALUE), null, ArithmeticException.class), // Fractional millisecond rounding – 1.5 ms → 1 ms
        Arguments.of("Fractional millisecond rounding – 1,500,000 nanoseconds equals 1.5 ms; toMillis truncates toward zero, returning 1 millisecond.", Duration.ofNanos(1_500_000), null, 1L), // Negative fractional millisecond – -1.5 ms → -1 ms
        Arguments.of("Negative fractional millisecond – -1,500,000 nanoseconds equals -1.5 ms; truncation yields -1 millisecond.", Duration.ofNanos(-1_500_000), null, -1L), // Sub‑millisecond nanosecond component – just under 1 ms → 0 ms
        Arguments.of("Sub‑millisecond nanosecond component – 999,999,999 ns is just under 1 ms, so toMillis returns 0.", Duration.ofSeconds(0, 999_999_999), null, 0L), // Converter argument is ignored – non‑null converter supplied
        Arguments.of("Converter argument is ignored – even when a non‑null Converter implementation is supplied, the result equals the millisecond value of the Duration.", Duration.ofSeconds(42), nonNullConverter, 42_000L), // Overflow with seconds + max nanos
        Arguments.of("Overflow on conversion – seconds at Long.MAX_VALUE combined with the maximum nanosecond adjustment cause toMillis() to overflow and throw ArithmeticException.", Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), null, ArithmeticException.class), // Negative overflow with seconds + negative nanos
        Arguments.of("Negative overflow – seconds at Long.MIN_VALUE with a negative nanosecond adjustment exceeds the representable range in milliseconds, resulting in ArithmeticException.", Duration.ofSeconds(Long.MIN_VALUE, -999_999_999), null, ArithmeticException.class));
    }

    @Test
    void toLong_returnsMillis_forPositiveDuration() {
        // given
        // 5,250 ms
        Duration duration = Duration.ofSeconds(5).plusMillis(250);
        long expected = duration.toMillis();
        // when
        long result = DurationConversions.toLong(duration, mockConverter);
        // then
        assertEquals(expected, result, "toLong should return the exact millisecond value");
    }

    @Test
    void toLong_returnsZero_forZeroDuration() {
        // given
        Duration zero = Duration.ZERO;
        // when
        long result = DurationConversions.toLong(zero, mockConverter);
        // then
        assertEquals(0L, result, "Zero duration must produce 0 milliseconds");
    }

    @Test
    void toLong_returnsNegativeMillis_forNegativeDuration() {
        // given
        // -3,400 ms
        Duration negative = Duration.ofSeconds(-3).minusMillis(400);
        long expected = negative.toMillis();
        // when
        long result = DurationConversions.toLong(negative, mockConverter);
        // then
        assertEquals(expected, result, "Negative durations should preserve sign in milliseconds");
    }

    @Test
    void toLong_throwsClassCastException_whenObjectIsNotDuration() {
        // given
        Object notADuration = "I am not a Duration";
        // when / then
        assertThrows(ClassCastException.class, () -> DurationConversions.toLong(notADuration, mockConverter), "A non‑Duration argument must cause a ClassCastException");
    }

    @Test
    void toLong_throwsNullPointerException_whenFromIsNull() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DurationConversions.toLong(nullObj, mockConverter), "Passing null as the source should result in NPE due to the cast");
    }

    /**
     * Parameterized test that iterates over every case described in the JSON.
     *
     * @param description human‑readable description of the test case
     * @param from        the {@code Object} to be converted (may be {@code null} or a non‑Duration)
     * @param converter   the {@link Converter} supplied to the method (may be {@code null})
     * @param expected    either a {@link Long} with the expected millisecond value,
     *                    or a {@link Class} representing the expected exception type
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideToLongCases")
    void toLong_parameterized(String description, Object from, Converter converter, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception – verify that it is thrown.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> DurationConversions.toLong(from, converter), () -> "Expected " + exClass.getSimpleName() + " for case: " + description);
        } else {
            // Expected a concrete long value.
            long expectedLong = (Long) expected;
            long result = DurationConversions.toLong(from, converter);
            assertEquals(expectedLong, result, () -> "Incorrect conversion for case: " + description);
        }
    }
}
