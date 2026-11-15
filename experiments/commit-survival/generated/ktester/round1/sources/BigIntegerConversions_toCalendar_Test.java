package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toCalendar_Test {

    // -----------------------------------------------------------------------
    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    // -----------------------------------------------------------------------
    @Mock
    private Converter mockConverterUTC;

    @Mock
    private ConverterOptions mockOptionsUTC;

    @Mock
    private Converter mockConverterPST;

    @Mock
    private ConverterOptions mockOptionsPST;

    @Mock
    private Converter mockConverterWithNullOptions;

    @Mock
    private Converter mockConverterWithOptionsNullTZ;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
        // ---- UTC mocks ----------------------------------------------------
        when(mockConverterUTC.getOptions()).thenReturn(mockOptionsUTC);
        when(mockOptionsUTC.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // ---- PST mocks ----------------------------------------------------
        when(mockConverterPST.getOptions()).thenReturn(mockOptionsPST);
        when(mockOptionsPST.getTimeZone()).thenReturn(TimeZone.getTimeZone("America/Los_Angeles"));
        // ---- Null options -------------------------------------------------
        when(mockConverterWithNullOptions.getOptions()).thenReturn(null);
        // ---- Options with null TZ -----------------------------------------
        when(mockConverterWithOptionsNullTZ.getOptions()).thenReturn(mockOptionsUTC);
        when(mockOptionsUTC.getTimeZone()).thenReturn(null);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session to release mocks.
        mocks.close();
    }

    /**
     * Normal conversion scenarios (including zero, sub‑millisecond, positive, negative, overflow).
     */
    private static Stream<ConversionCase> normalConversionProvider() {
        return Stream.of(// Zero nanoseconds → epoch start (UTC)
        new ConversionCase("Zero nanoseconds should produce the epoch start.", BigInteger.ZERO, // placeholder, will be replaced in test method
        null, 0L, TimeZone.getTimeZone("UTC")), // Sub‑millisecond (500 000 ns) → truncates to 0 ms (UTC)
        new ConversionCase("Nanoseconds less than one millisecond are truncated by integer division.", new BigInteger("500000"), null, 0L, TimeZone.getTimeZone("UTC")), // Positive value → 1 500 ms (UTC)
        new ConversionCase("Positive nanoseconds that map to a non‑zero millisecond value.", new BigInteger("1500000000"), null, 1500L, TimeZone.getTimeZone("UTC")), // Negative value → -2 ms (UTC)
        new ConversionCase("Negative nanoseconds are handled correctly; division yields -2 milliseconds.", new BigInteger("-2000000"), null, -2L, TimeZone.getTimeZone("UTC")), // Long.MAX_VALUE nanoseconds → fits in long after division
        new ConversionCase("Maximum long value expressed in nanoseconds.", new BigInteger("9223372036854775807"), null, new BigInteger("9223372036854775807").divide(BigInteger.valueOf(1_000_000L)).longValue(), TimeZone.getTimeZone("UTC")), // Long.MAX_VALUE + 1 nanoseconds → overflow to Long.MIN_VALUE
        new ConversionCase("Nanoseconds just above Long.MAX_VALUE cause overflow after division.", new BigInteger("9223372036854775808"), null, new BigInteger("9223372036854775808").divide(BigInteger.valueOf(1_000_000L)).longValue(), TimeZone.getTimeZone("UTC")));
    }

    /**
     * Additional conversion scenarios (different time‑zones, larger numbers, overflow in both directions).
     */
    private static Stream<ConversionCase> additionalConversionProvider() {
        return Stream.of(// Negative non‑divisible nanoseconds → -1500 ms (UTC)
        new ConversionCase("Negative nanoseconds not divisible by 1 000 000 are truncated toward zero.", new BigInteger("-1500000001"), null, new BigInteger("-1500000001").divide(BigInteger.valueOf(1_000_000L)).longValue(), TimeZone.getTimeZone("UTC")), // Exactly one millisecond
        new ConversionCase("Exactly one millisecond expressed in nanoseconds should map to 1 ms.", new BigInteger("1000000"), null, 1L, TimeZone.getTimeZone("UTC")), // Long.MIN_VALUE nanoseconds → fits in long after division
        new ConversionCase("Using Long.MIN_VALUE nanoseconds; after division the result still fits in a long.", new BigInteger("-9223372036854775808"), null, new BigInteger("-9223372036854775808").divide(BigInteger.valueOf(1_000_000L)).longValue(), TimeZone.getTimeZone("UTC")), // Very large value (Long.MAX_VALUE * 1_000_000) → overflow to Long.MIN_VALUE
        new ConversionCase("Nanoseconds just above Long.MAX_VALUE after division overflow to Long.MIN_VALUE.", new BigInteger("9223372036854775808000000"), null, new BigInteger("9223372036854775808000000").divide(BigInteger.valueOf(1_000_000L)).longValue(), TimeZone.getTimeZone("UTC")), // Very small (negative) large value → overflow to Long.MAX_VALUE
        new ConversionCase("Nanoseconds just below Long.MIN_VALUE overflow to Long.MAX_VALUE after division.", new BigInteger("-9223372036854775808000000"), null, new BigInteger("-9223372036854775808000000").divide(BigInteger.valueOf(1_000_000L)).longValue(), TimeZone.getTimeZone("UTC")), // Large value with PST time‑zone
        new ConversionCase("Large nanosecond value that still fits in a long; verifies PST time‑zone handling.", new BigInteger("1234567890123456"), null, new BigInteger("1234567890123456").divide(BigInteger.valueOf(1_000_000L)).longValue(), TimeZone.getTimeZone("America/Los_Angeles")));
    }

    /**
     * Exception scenarios – each case expects a specific exception type.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// from == null
        Arguments.of("The 'from' argument is null.", null, // converter will be mockConverterUTC (valid)
        null, NullPointerException.class), // from not a BigInteger
        Arguments.of("The 'from' argument is a String, not a BigInteger.", "notABigInteger", null, ClassCastException.class), // converter == null
        Arguments.of("The 'converter' argument is null.", BigInteger.ZERO, null, NullPointerException.class), // converter with null options
        Arguments.of("converter.getOptions() returns null.", BigInteger.ZERO, "mockConverterWithNullOptions", NullPointerException.class), // options with null time‑zone
        Arguments.of("converter.getOptions().getTimeZone() returns null.", BigInteger.ZERO, "mockConverterWithOptionsNullTZ", NullPointerException.class));
    }

    @Test
    void toCalendar_convertsNanosecondsToMilliseconds_usingUtcTimeZone() {
        // GIVEN – a BigInteger representing nanoseconds since epoch
        // 1.5 seconds = 1500 ms
        BigInteger epochNanos = BigInteger.valueOf(1_500_000_000L);
        // Expected milliseconds after dividing by 1_000_000 (MILLION constant)
        long expectedMillis = epochNanos.divide(BigInteger.valueOf(1_000_000L)).longValue();
        // WHEN – the conversion method is invoked
        Calendar result = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN – a Calendar is returned with the correct time‑zone and time‑in‑millis
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar should use the time‑zone supplied by ConverterOptions");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar timeInMillis should match epochNanos divided by 1_000_000");
        // Verify interaction with the mocks (optional but ensures contract)
        verify(mockConverter).getOptions();
        verify(mockOptions).getTimeZone();
    }

    @Test
    void toCalendar_zeroNanosecondsProducesEpochStart() {
        // GIVEN – zero nanoseconds (epoch start)
        BigInteger epochNanos = BigInteger.ZERO;
        // WHEN
        Calendar result = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(0L, result.getTimeInMillis(), "Zero nanoseconds should translate to zero milliseconds since epoch");
    }

    @Test
    void toCalendar_negativeNanosecondsHandledCorrectly() {
        // GIVEN – a negative nanosecond value
        // -2.5 seconds = -2500 ms
        BigInteger epochNanos = BigInteger.valueOf(-2_500_000_000L);
        long expectedMillis = epochNanos.divide(BigInteger.valueOf(1_000_000L)).longValue();
        // WHEN
        Calendar result = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTimeInMillis(), "Negative nanoseconds should be correctly converted to negative milliseconds");
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalConversionProvider")
    void toCalendar_normalConversions(ConversionCase tc) {
        // Resolve the proper mock based on the expected time‑zone.
        Converter converter = tc.expectedTimeZone.getID().equals("America/Los_Angeles") ? mockConverterPST : mockConverterUTC;
        // WHEN
        Calendar result = BigIntegerConversions.toCalendar(tc.from, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(tc.expectedTimeZone, result.getTimeZone(), "Calendar should use the time‑zone supplied by ConverterOptions");
        assertEquals(tc.expectedMillis, result.getTimeInMillis(), "Calendar timeInMillis should match expected value");
        // Verify interaction – ensures the method really consulted the mocks.
        verify(converter).getOptions();
        verify(converter == mockConverterPST ? mockOptionsPST : mockOptionsUTC).getTimeZone();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalConversionProvider")
    void toCalendar_additionalConversions(ConversionCase tc) {
        // Resolve the proper mock (PST for the last case, otherwise UTC).
        Converter converter = tc.expectedTimeZone.getID().equals("America/Los_Angeles") ? mockConverterPST : mockConverterUTC;
        // WHEN
        Calendar result = BigIntegerConversions.toCalendar(tc.from, converter);
        // THEN
        assertNotNull(result);
        assertEquals(tc.expectedTimeZone, result.getTimeZone(), "Calendar should be created with the correct time‑zone");
        assertEquals(tc.expectedMillis, result.getTimeInMillis(), "Milliseconds after division should match expected value");
        verify(converter).getOptions();
        verify(converter == mockConverterPST ? mockOptionsPST : mockOptionsUTC).getTimeZone();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionProvider")
    void toCalendar_exceptions(String description, Object from, Object converterId, Class<? extends Throwable> expectedException) {
        // Resolve the appropriate converter mock based on the identifier.
        Converter converter;
        if (converterId == null) {
            // null converter case
            converter = null;
        } else if (converterId instanceof String id) {
            converter = switch(id) {
                case "mockConverterWithNullOptions" ->
                    mockConverterWithNullOptions;
                case "mockConverterWithOptionsNullTZ" ->
                    mockConverterWithOptionsNullTZ;
                // fallback (should not happen)
                default ->
                    mockConverterUTC;
            };
        } else {
            // normal valid mock
            converter = mockConverterUTC;
        }
        // WHEN / THEN
        assertThrows(expectedException, () -> BigIntegerConversions.toCalendar(from, converter), description);
    }
}
