package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import java.time.Instant;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toBigDecimal_Test {

    /**
     * Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh Converter with default options before each test.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the static reference to help GC and avoid accidental reuse.
        converter = null;
    }

    private static Stream<Arguments> successCases() {
        return Stream.of(Arguments.of("Valid conversion – regular timestamp with nanoseconds", Timestamp.from(Instant.parse("2023-01-02T03:04:05.123456789Z")), new Converter(new ConverterOptions()), new BigDecimal("1672614245.123456789")), Arguments.of("Epoch zero – 1970‑01‑01T00:00:00Z", Timestamp.from(Instant.EPOCH), new Converter(new ConverterOptions()), new BigDecimal("0")), Arguments.of("Negative epoch – 1960‑01‑01T00:00:00Z", Timestamp.from(Instant.parse("1960-01-01T00:00:00Z")), new Converter(new ConverterOptions()), new BigDecimal("-315619200")), Arguments.of("Maximum nanosecond fraction – 999,999,999 ns", Timestamp.from(Instant.parse("2023-01-02T03:04:05.999999999Z")), new Converter(new ConverterOptions()), new BigDecimal("1672614245.999999999")));
    }

    private static Stream<Arguments> exceptionCases() {
        // Timestamp subclass that throws from toInstant()
        Timestamp throwingTimestamp = new Timestamp(0) {

            @Override
            public Instant toInstant() {
                throw new IllegalStateException("toInstant failure");
            }
        };
        // Timestamp subclass that returns null from toInstant()
        Timestamp nullInstantTimestamp = new Timestamp(0) {

            @Override
            public Instant toInstant() {
                return null;
            }
        };
        return Stream.of(// null from argument
        Arguments.of("Null `from` argument should cause NPE", null, new Converter(new ConverterOptions()), NullPointerException.class), // incompatible type (java.util.Date)
        Arguments.of("Incompatible type (java.util.Date) should cause ClassCastException", new java.util.Date(), new Converter(new ConverterOptions()), ClassCastException.class), // null converter – downstream call will NPE
        Arguments.of("Null `converter` argument should cause NPE inside InstantConversions", Timestamp.from(Instant.parse("2023-01-02T03:04:05Z")), null, NullPointerException.class), // converter that throws a ConversionException – we mock the static call
        Arguments.of("Converter that triggers a ConversionException should propagate it", Timestamp.from(Instant.parse("2023-01-02T03:04:05Z")), new Converter(new ConverterOptions()) {
        }, ConversionException.class), // Timestamp's toInstant throws IllegalStateException
        Arguments.of("Timestamp subclass whose toInstant throws IllegalStateException should propagate it", throwingTimestamp, new Converter(new ConverterOptions()), IllegalStateException.class), // Timestamp's toInstant returns null – InstantConversions will NPE
        Arguments.of("Timestamp subclass whose toInstant returns null should cause NPE", nullInstantTimestamp, new Converter(new ConverterOptions()), NullPointerException.class));
    }

    @Test
    void toBigDecimal_delegatesToInstantConversions_correctResult() {
        // GIVEN: a Timestamp representing a known instant
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:34:56.123456789");
        // WHEN: converting the Timestamp to BigDecimal via TimestampConversions
        BigDecimal actual = TimestampConversions.toBigDecimal(ts, converter);
        // THEN: the result must be identical to converting the underlying Instant directly
        BigDecimal expected = InstantConversions.toBigDecimal(ts.toInstant(), converter);
        assertEquals(expected, actual, "TimestampConversions should delegate to InstantConversions");
    }

    @Test
    void toBigDecimal_nullTimestamp_throwsNullPointerException() {
        // GIVEN: a null reference
        Timestamp nullTs = null;
        // WHEN / THEN: a NullPointerException is expected because the method casts the input to Timestamp
        assertThrows(NullPointerException.class, () -> TimestampConversions.toBigDecimal(nullTs, converter), "Calling toBigDecimal with a null Timestamp should throw NPE");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("successCases")
    void toBigDecimal_successfulConversion(String description, Timestamp ts, Converter conv, BigDecimal expected) {
        // WHEN
        BigDecimal actual = TimestampConversions.toBigDecimal(ts, conv);
        // THEN
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionCases")
    void toBigDecimal_exceptionPropagation(String description, Object from, Converter conv, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> TimestampConversions.toBigDecimal(from, conv), description);
    }

    /**
     * Specific test for the scenario where the downstream static method
     * {@link InstantConversions#toBigDecimal(java.time.Instant, Converter)} throws
     * a {@link ConversionException}. We use Mockito's mockStatic to force the
     * exception and verify that it propagates unchanged.
     */
    @Test
    void toBigDecimal_converterThrowsConversionException_propagates() {
        // GIVEN
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-02T03:04:05Z"));
        Converter dummy = new Converter(new ConverterOptions());
        // Mock the static method to throw ConversionException
        try (MockedStatic<InstantConversions> mocked = mockStatic(InstantConversions.class)) {
            mocked.when(() -> InstantConversions.toBigDecimal(any(Instant.class), eq(dummy))).thenThrow(new ConversionException("forced conversion failure"));
            // WHEN / THEN
            assertThrows(ConversionException.class, () -> TimestampConversions.toBigDecimal(ts, dummy), "ConversionException should be propagated");
        }
    }
}
