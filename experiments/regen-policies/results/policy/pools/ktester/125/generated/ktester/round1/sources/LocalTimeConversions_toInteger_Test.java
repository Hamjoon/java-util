package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalTime;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toInteger_Test {

    /**
     * Shared Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Provides a stream of arguments for successful conversions.
     * Each argument consists of:
     *   - the LocalTime to convert,
     *   - the Converter to pass (null or the shared instance),
     *   - the expected millisecond result,
     *   - a short description for readability.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Valid conversions group
        Arguments.of(LocalTime.MIDNIGHT, null, 0, "Midnight (00:00) → 0 ms"), Arguments.of(LocalTime.of(12, 34, 56, 789_123_456), null, 45296789, "Typical time 12:34:56.789123456 → 45,296,789 ms"), Arguments.of(LocalTime.MAX, null, 86_399_999, "Maximum time 23:59:59.999999999 → 86,399,999 ms"), Arguments.of(LocalTime.of(0, 0, 0, 999_999), null, 0, "Sub‑millisecond only (999 999 ns) → 0 ms"), // Exact millisecond conversion group
        Arguments.of(LocalTime.of(0, 0, 0, 1_000_000), null, 1, "Exactly one millisecond after midnight → 1 ms"), Arguments.of(LocalTime.of(0, 0, 1, 0), null, 1_000, "One full second after midnight → 1,000 ms"), Arguments.of(LocalTime.of(13, 45, 30, 0), null, 49_530_000, "13:45:30.000 → 49,530,000 ms"), // Mixed hour/minute/second values group
        Arguments.of(LocalTime.of(23, 59, 59, 999_000_000), null, 86_399_999, "23:59:59.999 → 86,399,999 ms"), Arguments.of(LocalTime.of(1, 1, 1, 1_001_001), null, 3_661_001, "01:01:01.001001001 → 3,661,001 ms"), // Converter argument usage group (non‑null converter)
        Arguments.of(LocalTime.of(12, 0, 0, 0), converter, 43_200_000, "12:00:00 with non‑null converter → 43,200,000 ms"));
    }

    @Test
    void midnightShouldReturnZeroMilliseconds() {
        // 00:00:00.000000000
        LocalTime lt = LocalTime.MIDNIGHT;
        int result = LocalTimeConversions.toInteger(lt, converter);
        assertEquals(0, result, "Midnight must be converted to 0 ms");
    }

    @Test
    void oneSecondShouldReturnOneThousandMilliseconds() {
        // 00:00:01.000000000
        LocalTime lt = LocalTime.of(0, 0, 1);
        int result = LocalTimeConversions.toInteger(lt, converter);
        assertEquals(1_000, result, "One second equals 1000 ms");
    }

    @Test
    void arbitraryTimeShouldBeConvertedCorrectly() {
        // 12:34:56.123456789
        LocalTime lt = LocalTime.of(12, 34, 56, 123_456_789);
        // total nanoseconds since start of day
        long nanos = lt.toNanoOfDay();
        // expected milliseconds
        int expected = (int) (nanos / 1_000_000L);
        int actual = LocalTimeConversions.toInteger(lt, converter);
        assertEquals(expected, actual, "Conversion should match nanoseconds‑to‑milliseconds calculation");
    }

    @Test
    void nullLocalTimeShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toInteger(null, converter), "Passing null as the source should result in NPE");
    }

    @Test
    void nullConverterIsAcceptedBecauseItIsNotUsed() {
        LocalTime lt = LocalTime.of(1, 2, 3, 4);
        // The method does not dereference the converter, so a null value is acceptable.
        int result = LocalTimeConversions.toInteger(lt, null);
        long expected = lt.toNanoOfDay() / 1_000_000L;
        assertEquals((int) expected, result, "Result should be correct even when the Converter argument is null");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successfulConversionProvider")
    void shouldConvertLocalTimeToMilliseconds(LocalTime from, Converter conv, int expected, String _description) {
        // Act
        int actual = LocalTimeConversions.toInteger(from, conv);
        // Assert
        assertEquals(expected, actual, () -> "Conversion of " + from + " did not match expected milliseconds");
    }

    @Test
    void invalidFromTypeShouldThrowClassCastException() {
        String notALocalTime = "not a LocalTime";
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toInteger(notALocalTime, converter), "Providing a non‑LocalTime object must result in ClassCastException");
    }
}
