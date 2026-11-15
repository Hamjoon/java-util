package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toBigDecimal_Test {

    /**
     * A mock {@link Converter} – the implementation under test does not use it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Shared {@link Converter} instance for all tests (could be real or mock).
     */
    private static Converter sharedConverter;

    /**
     * Shared {@link Converter} instance for tests that need a real (non‑mock) object.
     */
    private static Converter dummyConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter needs a ConverterOptions instance; using a mock is simpler.
        // However, because the method under test never touches the converter,
        // we can safely keep it null or create a dummy instance.
        dummyConverter = new Converter() {

            @Override
            public Object convert(Object o) {
                return o;
            }
        };
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock interactions before each test.
        reset(mockConverter);
    }

    /* -----------------------------------------------------------
     *  Parameterized tests for all "Valid conversions" cases
     * ----------------------------------------------------------- */
    private static Stream<Arguments> provideValidInstants() {
        return Stream.of(// epoch instant (0 seconds, 0 nanos)
        Arguments.of(Instant.ofEpochSecond(0, 0), null, BigDecimal.valueOf(0), "Convert the epoch Instant (1970‑01‑01T00:00:00Z)."), // typical positive instant with non‑zero nanos
        Arguments.of(Instant.ofEpochSecond(1_600_000_000L, 123_456_789), null, new BigDecimal("1600000000.123456789"), "Typical positive instant with non‑zero nanoseconds."), // negative epoch second with positive nanos (normalises to -0.5)
        Arguments.of(Instant.ofEpochSecond(-1L, 500_000_000), null, new BigDecimal("-0.5"), "Negative epoch second with positive nanos."), // maximum long epoch second with max nanos
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE, 999_999_999), null, new BigDecimal(Long.toString(Long.MAX_VALUE)).add(new BigDecimal("0.999999999")), "Edge case: maximum long epoch second combined with the maximum nanosecond value."), // minimum long epoch second with zero nanos
        Arguments.of(Instant.ofEpochSecond(Long.MIN_VALUE, 0), null, new BigDecimal(Long.toString(Long.MIN_VALUE)), "Edge case: minimum long epoch second with zero nanoseconds."), // zero seconds with max positive nanos
        Arguments.of(Instant.ofEpochSecond(0, 999_999_999), null, new BigDecimal("0.999999999"), "Zero seconds with the maximum positive nanosecond fraction."), // negative whole second with zero nanos
        Arguments.of(Instant.ofEpochSecond(-1, 0), null, BigDecimal.valueOf(-1), "Negative whole second with zero nanos."), // -1 seconds + 999_999_999 nanos normalises to -0.000000001
        Arguments.of(Instant.ofEpochSecond(-1, 999_999_999), null, new BigDecimal("-0.000000001"), "Instant normalises -1 seconds plus 999 999 999 nanos to -0.000000001 seconds."), // maximum long epoch second with zero nanos
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE, 0), null, new BigDecimal(Long.toString(Long.MAX_VALUE)), "Maximum long epoch second with zero nanoseconds."), // Instant.MIN (defined as ofEpochSecond(Long.MIN_VALUE, -999_999_999))
        Arguments.of(Instant.MIN, null, new BigDecimal(Long.toString(Long.MIN_VALUE - 1)).add(new BigDecimal("0.000000001")), "Instant.MIN normalises to (Long.MIN_VALUE‑1) seconds and 1 nanosecond."));
    }

    /**
     * Verify that a normal {@link Instant} is correctly converted to a {@link BigDecimal}
     * representing the epoch seconds plus the fractional nanosecond part.
     */
    @Test
    void toBigDecimal_convertsInstantCorrectly() {
        // GIVEN
        // arbitrary epoch
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 123_456_789);
        // Expected value: seconds + nanos / 1_000_000_000
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond()).add(// scale = 9 (nanoseconds)
        BigDecimal.valueOf(instant.getNano(), 9));
        // WHEN
        BigDecimal result = InstantConversions.toBigDecimal(instant, mockConverter);
        // THEN
        assertNotNull(result, "Result must not be null");
        // Use compareTo for numeric equality (ignores scale differences)
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
        // Verify that the converter was not touched
        verifyNoInteractions(mockConverter);
    }

    /**
     * Verify that passing {@code null} results in a {@link NullPointerException}
     * because the method casts the argument to {@link Instant} without a null‑check.
     */
    @Test
    void toBigDecimal_nullArgument_throwsNPE() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toBigDecimal(nullObj, mockConverter), "Casting null to Instant should throw NPE");
    }

    /**
     * Verify that passing an object that is not an {@link Instant} results in a
     * {@link ClassCastException}.
     */
    @Test
    void toBigDecimal_wrongType_throwsClassCastException() {
        // GIVEN
        Object notAnInstant = "2021-01-01T00:00:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> InstantConversions.toBigDecimal(notAnInstant, mockConverter), "Casting a non‑Instant object should throw ClassCastException");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideValidInstants")
    void toBigDecimal_validConversions(Instant instant, Converter converter, BigDecimal expected, String description) {
        // GIVEN
        Converter usedConverter = (converter != null) ? converter : mockConverter;
        // WHEN
        BigDecimal result = InstantConversions.toBigDecimal(instant, usedConverter);
        // THEN
        assertNotNull(result, "Result must not be null");
        // Numeric equality – ignore possible scale differences
        assertEquals(0, expected.compareTo(result), () -> description + " Expected " + expected + " but got " + result);
        // The method does not interact with the converter
        verifyNoInteractions(mockConverter);
    }

    /* -----------------------------------------------------------
     *  Converter argument effect – ensure it is ignored
     * ----------------------------------------------------------- */
    @Test
    void toBigDecimal_converterArgumentIsIgnored() {
        // GIVEN
        Instant instant = Instant.ofEpochSecond(42, 0);
        Converter customConverter = new Converter() {

            @Override
            public Object convert(Object o) {
                // deliberately return something else to prove it is not used
                return "ignored";
            }
        };
        BigDecimal expected = BigDecimal.valueOf(42);
        // WHEN
        BigDecimal resultWithCustom = InstantConversions.toBigDecimal(instant, customConverter);
        BigDecimal resultWithMock = InstantConversions.toBigDecimal(instant, mockConverter);
        // THEN
        assertEquals(0, expected.compareTo(resultWithCustom), "Result must match expected value when a custom Converter is supplied.");
        assertEquals(0, expected.compareTo(resultWithMock), "Result must match expected value when a mock Converter is supplied.");
        // Verify that neither converter was interacted with
        verifyNoInteractions(mockConverter);
    }
}
