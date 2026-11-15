package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Date;
import java.util.stream.Stream;

class InstantConversions_toMap_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter is required by the signature, even though the method does not use it.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
        converter = null;
    }

    /**
     * Provides a stream of valid {@code Instant} inputs together with the
     * expected {@code seconds} and {@code nanos} values.
     *
     * The {@code Converter} argument is varied between {@code null} and a real
     * instance to demonstrate that the method does not depend on it.
     */
    private static Stream<Arguments> normalCases() {
        return Stream.of(// Epoch instant – both seconds and nanos are zero
        Arguments.of(Instant.ofEpochSecond(0, 0), null, 0L, 0), // Typical positive instant with non‑zero nanos
        Arguments.of(Instant.ofEpochSecond(1_625_097_600L, 123_456_789), new Converter(new ConverterOptions()), 1_625_097_600L, 123_456_789), // Instant before the Unix epoch with maximum nanos
        Arguments.of(Instant.ofEpochSecond(-1, 999_999_999), null, -1L, 999_999_999));
    }

    /**
     * Supplies inputs that are expected to cause an exception.
     *
     * @return a stream of arguments: the {@code from} object, a {@code Converter}
     * (may be {@code null}), and the expected exception class.
     */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null Instant – should trigger NullPointerException during cast
        Arguments.of(null, null, NullPointerException.class), // Wrong type: String
        Arguments.of("some string", null, ClassCastException.class), // Wrong type: java.util.Date (not an Instant)
        Arguments.of(Date.from(Instant.now()), null, ClassCastException.class), // Wrong type: Integer
        Arguments.of(Integer.valueOf(12_345), null, ClassCastException.class));
    }

    @Test
    void toMap_returnsMapWithCorrectSecondsAndNanos() {
        // given
        long epochSecond = 1_642_000_123L;
        int nanos = 456_789_123;
        Instant instant = Instant.ofEpochSecond(epochSecond, nanos);
        // when
        Map<String, Object> result = InstantConversions.toMap(instant, converter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        assertTrue(result.containsKey("seconds"), "Map should contain key 'seconds'");
        assertTrue(result.containsKey("nanos"), "Map should contain key 'nanos'");
        assertEquals(epochSecond, result.get("seconds"));
        assertEquals(nanos, result.get("nanos"));
    }

    @Test
    void toMap_handlesInstantWithZeroNanos() {
        // given
        Instant instant = Instant.ofEpochSecond(0L, 0L);
        // when
        Map<String, Object> result = InstantConversions.toMap(instant, converter);
        // then
        assertEquals(0L, result.get("seconds"));
        assertEquals(0, result.get("nanos"));
    }

    @ParameterizedTest(name = "[{index}] seconds={2}, nanos={3}")
    @MethodSource("normalCases")
    void toMap_normalBehavior(Instant instant, Converter conv, long expectedSeconds, int expectedNanos) {
        // when
        Map<String, Object> result = InstantConversions.toMap(instant, conv);
        // then
        assertNotNull(result, "Result map must not be null");
        assertEquals(2, result.size(), "Result map must contain exactly two entries");
        assertEquals(expectedSeconds, result.get("seconds"), "Map entry 'seconds' should match the Instant's epoch second");
        assertEquals(expectedNanos, result.get("nanos"), "Map entry 'nanos' should match the Instant's nanosecond component");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expect {2}")
    @MethodSource("exceptionCases")
    void toMap_invalidInputs(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> InstantConversions.toMap(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
