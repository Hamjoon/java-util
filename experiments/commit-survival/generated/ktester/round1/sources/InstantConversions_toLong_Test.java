package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLong_Test {

    /**
     * Mocked {@link Converter} – not used by the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple {@link Converter} implementation that does nothing.
     */
    private static class DummyConverter implements Converter {
        // No behaviour required for the tests.
    }

    /**
     * Converter that throws on any method call – used to prove the method ignores it.
     */
    private static class ThrowingConverter implements Converter {

        @Override
        public Object convert(Object from, Class<?> toClass) {
            throw new UnsupportedOperationException("Should never be called");
        }
        // Other methods of the interface can similarly throw if needed.
    }

    private static Stream<Arguments> provideToLongScenarios() {
        return Stream.of(// epoch start
        Arguments.of(Instant.ofEpochMilli(0L), null, 0L, null), // typical positive value, with a real converter instance
        Arguments.of(Instant.ofEpochMilli(1_600_000_000_000L), new DummyConverter(), 1_600_000_000_000L, null), // negative epoch
        Arguments.of(Instant.ofEpochMilli(-1_000L), null, -1_000L, null), // ClassCastException case – a Date is not an Instant
        Arguments.of(new java.util.Date(), null, null, ClassCastException.class), // Long.MAX_VALUE epoch milli (will overflow to DateTimeException)
        Arguments.of(Instant.ofEpochMilli(Long.MAX_VALUE), null, null, DateTimeException.class), // Long.MIN_VALUE epoch milli (will overflow to DateTimeException)
        Arguments.of(Instant.ofEpochMilli(Long.MIN_VALUE), null, null, DateTimeException.class), // Sub‑millisecond truncation (nanoseconds < 1 000 000)
        Arguments.of(Instant.ofEpochSecond(0, 999_999), null, 0L, null), // 2.5 seconds => 2500 ms (floor truncation)
        Arguments.of(Instant.ofEpochSecond(1, 1_500_000_000), null, 2500L, null), // Negative second with positive nanos => -0.5 s => -500 ms
        Arguments.of(Instant.ofEpochSecond(-1, 500_000_000), null, -500L, null), // Exactly 1.5 seconds => 1500 ms
        Arguments.of(Instant.ofEpochSecond(0, 1_500_000_000), null, 1500L, null), // Throwing converter – should not affect the result
        Arguments.of(Instant.ofEpochMilli(123_456_789L), new ThrowingConverter(), 123_456_789L, null), // Instant.MAX – out of range for toEpochMilli()
        Arguments.of(Instant.MAX, null, null, DateTimeException.class), // Instant.MIN – out of range for toEpochMilli()
        Arguments.of(Instant.MIN, null, null, DateTimeException.class));
    }

    @Test
    @DisplayName("toLong returns the epoch‑millis of a standard Instant")
    void toLong_returnsEpochMilli() {
        // GIVEN
        Instant instant = Instant.parse("2023-08-15T12:34:56.789Z");
        long expected = instant.toEpochMilli();
        // WHEN
        long actual = InstantConversions.toLong(instant, mockConverter);
        // THEN
        assertEquals(expected, actual, "The long value returned should equal Instant.toEpochMilli()");
    }

    @Test
    @DisplayName("toLong works for the epoch Instant (0 milliseconds)")
    void toLong_epochInstant() {
        // GIVEN
        // 1970‑01‑01T00:00:00Z
        Instant epoch = Instant.EPOCH;
        long expected = 0L;
        // WHEN
        long actual = InstantConversions.toLong(epoch, mockConverter);
        // THEN
        assertEquals(expected, actual, "Epoch Instant must be converted to 0 milliseconds");
    }

    @Test
    @DisplayName("toLong throws NullPointerException when null is supplied")
    void toLong_nullArgument() {
        // GIVEN
        Object nullInstant = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toLong(nullInstant, mockConverter), "A null Instant should cause a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("provideToLongScenarios")
    void testToLongVariousScenarios(Object from, Converter converter, Long expectedResult, Class<? extends Throwable> expectedException) {
        // Use the mockConverter when the test data supplies null for the converter
        Converter effectiveConverter = (converter != null) ? converter : mockConverter;
        if (expectedException != null) {
            // Expect an exception (ClassCastException, DateTimeException, etc.)
            assertThrows(expectedException, () -> InstantConversions.toLong(from, effectiveConverter), () -> "Expected exception " + expectedException.getSimpleName());
        } else {
            // Normal successful conversion
            long actual = InstantConversions.toLong(from, effectiveConverter);
            assertEquals(expectedResult.longValue(), actual, "Conversion result does not match expected epoch milliseconds");
        }
    }

    @Test
    @DisplayName("toLong throws NullPointerException when the 'from' argument is null")
    void toLong_nullFromArgument() {
        assertThrows(NullPointerException.class, () -> InstantConversions.toLong(null, mockConverter), "Passing null as the 'from' argument must result in NPE");
    }
}
