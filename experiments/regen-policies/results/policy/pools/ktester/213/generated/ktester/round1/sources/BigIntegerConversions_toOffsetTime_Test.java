package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toOffsetTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    private static final ZoneId UTC = ZoneId.of("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: converter -> options -> zoneId
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(TEST_ZONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        reset(mockConverter, mockOptions);
    }

    /**
     * Helper that delegates to the package‑private method under test.
     */
    private static OffsetTime callToOffsetTime(Object from, Converter converter) {
        // The method is package‑private static, so we can call it directly from the test class
        return BigIntegerConversions.toOffsetTime(from, converter);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// midnight (0 ns) – UTC
        Arguments.of("Midnight (0 ns) UTC", BigInteger.ZERO, UTC, "00:00Z"), // maximum nanoseconds that fit in a day – UTC
        Arguments.of("Maximum nanoseconds of a day UTC", new BigInteger("86399999999999"), UTC, "23:59:59.999999999Z"), // fractional seconds – UTC
        Arguments.of("1 234 567 890 ns (1 s + 234 567 890 ns) UTC", new BigInteger("1234567890"), UTC, "00:00:01.234567890Z"), // exactly 60 seconds – UTC
        Arguments.of("60 000 000 000 ns (1 min) UTC", new BigInteger("60000000000"), UTC, "00:01:00Z"), // noon – UTC
        Arguments.of("12 hours (43 200 000 000 000 ns) UTC", new BigInteger("43200000000000"), UTC, "12:00:00Z"));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// value exceeds the number of nanoseconds in a day (24 h)
        Arguments.of("Value exceeds one day (24 h) – should fail", new BigInteger("86400000000000"), IllegalArgumentException.class), // negative value
        Arguments.of("Negative nanoseconds – should fail", new BigInteger("-1"), IllegalArgumentException.class));
    }

    private static Stream<Arguments> zoneConversionProvider() {
        return Stream.of(Arguments.of("UTC‑05:00 zone, epoch 0 ns → 19:00‑05:00", BigInteger.ZERO, ZoneOffset.ofHours(-5), "19:00-05:00"), Arguments.of("UTC+02:00 zone, epoch 0 ns → 02:00+02:00", BigInteger.ZERO, ZoneOffset.ofHours(2), "02:00+02:00"));
    }

    @Test
    void toOffsetTime_zeroNanoseconds_returnsMidnight() {
        // This test mirrors the first parameterised case but remains for explicitness.
        // 0 nanoseconds since epoch
        BigInteger input = BigInteger.ZERO;
        OffsetTime result = callToOffsetTime(input, mockConverter);
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochSecond(0, 0), UTC);
        assertEquals(expected, result, "OffsetTime for zero nanoseconds should be midnight UTC");
    }

    @Test
    void toOffsetTime_withSecondsAndNanos_returnsCorrectTime() {
        // 1.5 seconds = 1_500_000_000 nanoseconds
        BigInteger input = BigInteger.valueOf(1_500_000_000L);
        OffsetTime result = callToOffsetTime(input, mockConverter);
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochSecond(1, 500_000_000), TEST_ZONE);
        assertEquals(expected, result, "OffsetTime should reflect 1 second and 500 ms");
    }

    @Test
    void toOffsetTime_maximumNanosecondsInDay_returnsLastMomentOfDay() {
        // 86,399,999,999,999 nanoseconds = 86,399 seconds + 999,999,999 nanos
        BigInteger input = BigInteger.valueOf(86_399_999_999_999L);
        OffsetTime result = callToOffsetTime(input, mockConverter);
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochSecond(86_399L, 999_999_999L), TEST_ZONE);
        assertEquals(expected, result, "OffsetTime should be 23:59:59.999999999 for the maximum nanoseconds of a day");
    }

    @Test
    void toOffsetTime_negativeValue_throwsIllegalArgumentException() {
        BigInteger input = BigInteger.valueOf(-1L);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> callToOffsetTime(input, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0"), "Exception message should indicate the value must be non‑negative");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toOffsetTime_validConversions(String description, BigInteger input, ZoneId zone, String expectedIso) {
        // Arrange: configure zone if it differs from the default UTC
        when(mockOptions.getZoneId()).thenReturn(zone);
        // Act
        OffsetTime result = callToOffsetTime(input, mockConverter);
        // Assert
        OffsetTime expected = OffsetTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    void toOffsetTime_invalidConversions(String description, BigInteger input, Class<? extends Throwable> expectedException) {
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> callToOffsetTime(input, mockConverter), description);
        assertTrue(ex.getMessage().contains("must be >= 0"), "Exception message should indicate the value must be non‑negative");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zoneConversionProvider")
    void toOffsetTime_differentZones(String description, BigInteger input, ZoneId zone, String expectedIso) {
        // Arrange: mock the converter to return the required zone
        when(mockOptions.getZoneId()).thenReturn(zone);
        // Act
        OffsetTime result = callToOffsetTime(input, mockConverter);
        // Assert
        OffsetTime expected = OffsetTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @Test
    void toOffsetTime_nullConverter_throwsNullPointerException() {
        BigInteger input = BigInteger.ZERO;
        assertThrows(NullPointerException.class, () -> callToOffsetTime(input, null), "Passing a null Converter should result in NullPointerException");
    }

    @Test
    void toOffsetTime_nullFromArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> callToOffsetTime(null, mockConverter), "Passing a null 'from' argument should result in NullPointerException");
    }

    @Test
    void toOffsetTime_invalidSourceType_throwsClassCastException() {
        // not a BigInteger
        Long wrongType = 123L;
        assertThrows(ClassCastException.class, () -> callToOffsetTime(wrongType, mockConverter), "Providing a Long instead of a BigInteger should throw ClassCastException");
    }
}
