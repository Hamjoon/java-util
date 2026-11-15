package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toInstant_Test {

    /**
     * The Converter instance is *not* used by {@code BigIntegerConversions.toInstant},
     *  but we provide a mock to verify that no interaction occurs.
     */
    @Mock
    Converter mockConverter;

    /**
     * A real Converter can also be created for completeness – it is never passed
     *  to the method under test, but the field is kept for potential future tests.
     */
    static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter – it requires a ConverterOptions instance.
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock Converter was never interacted with.
        verifyNoInteractions(mockConverter);
        // The method under test must never call any method on the converter.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Helper that invokes the package‑private method via reflection.
     *  The test class resides in the same package, so we can call it directly.
     */
    private static Instant callToInstant(Object from, Converter converter) {
        return BigIntegerConversions.toInstant(from, converter);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Zero nanoseconds → epoch
        Arguments.of("Zero nanoseconds should produce the epoch instant (1970‑01‑01T00:00:00Z).", BigInteger.valueOf(0), Instant.ofEpochSecond(0, 0)), // 1.5 seconds → 1 sec + 500_000_000 nanos
        Arguments.of("Nanoseconds greater than one second should be split into seconds (1) and nanoseconds (500,000,000).", BigInteger.valueOf(1_500_000_000L), Instant.ofEpochSecond(1, 500_000_000L)), // -2.5 seconds → -3 sec + 500_000_000 nanos (normalised)
        Arguments.of("Negative nanoseconds must be correctly divided: -2.5 s => -3 seconds + 500 000 000 nanos.", new BigInteger("-2500000000"), Instant.ofEpochSecond(-3, 500_000_000L)), // Max long seconds, zero nanos
        Arguments.of("Maximum long seconds with zero nanoseconds should be handled without overflow.", BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)), Instant.ofEpochSecond(Long.MAX_VALUE, 0)), // Min long seconds, max positive nanos
        Arguments.of("Minimum long seconds with maximum positive nanoseconds part should be converted correctly.", BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(999_999_999L)), Instant.ofEpochSecond(Long.MIN_VALUE, 999_999_999L)), // Half‑second
        Arguments.of("Half‑second (500 million nanoseconds) should produce 0 seconds and 500 000 000 nanos.", BigInteger.valueOf(500_000_000L), Instant.ofEpochSecond(0, 500_000_000L)), // Exactly one second
        Arguments.of("Exactly one second in nanoseconds should be split into 1 second and 0 nanoseconds.", BigInteger.valueOf(1_000_000_000L), Instant.ofEpochSecond(1, 0)), // Exactly minus one second
        Arguments.of("Exactly minus one second should result in -1 seconds and 0 nanoseconds.", new BigInteger("-1000000000"), Instant.ofEpochSecond(-1, 0)), // Single nanosecond
        Arguments.of("A single nanosecond should produce 0 seconds and 1 nanosecond.", BigInteger.ONE, Instant.ofEpochSecond(0, 1)), // Minus one nanosecond (normalised)
        Arguments.of("Minus one nanosecond normalises to -1 second + 999,999,999 nanoseconds.", new BigInteger("-1"), Instant.ofEpochSecond(-1, 999_999_999L)), // Max long seconds with non‑zero nanos
        Arguments.of("Maximum long seconds with a non‑zero nanosecond part should be accepted.", BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(123_456_789L)), Instant.ofEpochSecond(Long.MAX_VALUE, 123_456_789L)), // Minimum long seconds with zero nanos
        Arguments.of("Minimum long seconds with zero nanoseconds should be converted correctly.", BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)), Instant.ofEpochSecond(Long.MIN_VALUE, 0)), // Converter argument ignored (non‑null)
        Arguments.of("Converter argument is ignored; passing a non‑null Converter should still yield the correct instant.", BigInteger.valueOf(0), Instant.ofEpochSecond(0, 0)));
    }

    @Test
    void toInstant_zeroNanoseconds_returnsEpoch() {
        BigInteger zero = BigInteger.ZERO;
        Instant result = callToInstant(zero, mockConverter);
        assertEquals(Instant.EPOCH, result, "Zero nanoseconds should map to the epoch instant");
    }

    @Test
    void toInstant_exactSeconds_returnsCorrectInstant() {
        // 5 seconds = 5_000_000_000 nanoseconds
        BigInteger fiveSeconds = BigInteger.valueOf(5L).multiply(BigIntegerConversions.BILLION);
        Instant result = callToInstant(fiveSeconds, mockConverter);
        assertEquals(Instant.ofEpochSecond(5L, 0L), result, "Exact seconds should have zero nanosecond part");
    }

    @Test
    void toInstant_secondsAndNanos_returnsCorrectInstant() {
        // 2.75 seconds = 2 seconds + 750_000_000 nanoseconds
        BigInteger twoPoint75 = BigInteger.valueOf(2L).multiply(BigIntegerConversions.BILLION).add(BigInteger.valueOf(750_000_000L));
        Instant result = callToInstant(twoPoint75, mockConverter);
        assertEquals(Instant.ofEpochSecond(2L, 750_000_000L), result, "Mixed seconds and nanos should be combined correctly");
    }

    @Test
    void toInstant_largeValue_returnsCorrectInstant() {
        // 123456789 seconds and 123456789 nanoseconds
        BigInteger secondsPart = BigInteger.valueOf(123_456_789L).multiply(BigIntegerConversions.BILLION);
        BigInteger nanosPart = BigInteger.valueOf(123_456_789L);
        BigInteger total = secondsPart.add(nanosPart);
        Instant result = callToInstant(total, mockConverter);
        assertEquals(Instant.ofEpochSecond(123_456_789L, 123_456_789L), result, "Large values should be handled without overflow");
    }

    @Test
    void toInstant_negativeNanoseconds_normalizesCorrectly() {
        // -1.5 seconds = -1_500_000_000 nanoseconds
        BigInteger negative = BigInteger.valueOf(-1_500_000_000L);
        Instant result = callToInstant(negative, mockConverter);
        // Instant.ofEpochSecond normalises the negative nanos internally.
        Instant expected = Instant.ofEpochSecond(-2L, 500_000_000L);
        assertEquals(expected, result, "Negative nanoseconds should be normalised to a valid Instant");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void toInstant_validConversions(String description, Object from, Instant expected) {
        Instant result = callToInstant(from, mockConverter);
        assertEquals(expected, result, description);
    }

    @Test
    void toInstant_nullFrom_throwsNullPointerException() {
        // The cast succeeds (null), but divideAndRemainder on null triggers NPE.
        assertThrows(NullPointerException.class, () -> callToInstant(null, mockConverter), "Passing a null `from` should result in NullPointerException.");
    }

    @Test
    void toInstant_wrongType_throwsClassCastException() {
        // Passing a String cannot be cast to BigInteger.
        assertThrows(ClassCastException.class, () -> callToInstant("1234567890", mockConverter), "Passing an object of wrong type (String) should result in a ClassCastException.");
    }

    @Test
    void toInstant_secondsOverflow_throwsDateTimeException() {
        // Instant.MAX.getEpochSecond() + 1 seconds → overflow
        long overflowSec = Instant.MAX.getEpochSecond() + 1;
        BigInteger tooLarge = BigInteger.valueOf(overflowSec).multiply(BigInteger.valueOf(1_000_000_000L));
        assertThrows(DateTimeException.class, () -> callToInstant(tooLarge, mockConverter), "Instant seconds exceed Instant.MAX; should throw DateTimeException.");
    }

    @Test
    void toInstant_secondsUnderflow_throwsDateTimeException() {
        // Instant.MIN.getEpochSecond() - 1 seconds → underflow
        long underflowSec = Instant.MIN.getEpochSecond() - 1;
        BigInteger tooSmall = BigInteger.valueOf(underflowSec).multiply(BigInteger.valueOf(1_000_000_000L));
        assertThrows(DateTimeException.class, () -> callToInstant(tooSmall, mockConverter), "Instant seconds less than Instant.MIN; should throw DateTimeException.");
    }
}
