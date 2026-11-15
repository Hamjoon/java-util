package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalTime;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toDouble_Test {

    private static Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The real Converter is not used by the method under test, but we provide a valid instance.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validLocalTimeProvider() {
        return Stream.of(arguments("Midnight (00:00:00) should be converted to 0.0 seconds", LocalTime.of(0, 0), 0.0), arguments("Typical time 12:30:15.500 000 000 → 45 015.5 seconds", LocalTime.of(12, 30, 15, 500_000_000), 45_015.5), arguments("Maximum possible LocalTime (23:59:59.999 999 999) → 86 399.999999999 seconds", LocalTime.of(23, 59, 59, 999_999_999), 86_399.999999999));
    }

    private static Stream<Arguments> nanosecondEdgeProvider() {
        return Stream.of(arguments("A single nanosecond after midnight → 1e-9 seconds", LocalTime.of(0, 0, 0, 1), 1e-9), arguments("One nanosecond shy of one second → 0.999999999 seconds", LocalTime.of(0, 0, 0, 999_999_999), 0.999999999), arguments("Exactly noon (12:00:00) → 43 200.0 seconds", LocalTime.of(12, 0, 0, 0), 43_200.0), arguments("One second before midnight without nanoseconds → 86 399.0 seconds", LocalTime.of(23, 59, 59, 0), 86_399.0));
    }

    private static Stream<Arguments> converterHandlingProvider() {
        // Dummy converter – does nothing special.
        Converter dummy = new Converter(new ConverterOptions());
        // Throwing converter – any method call throws an exception.
        Converter throwing = Mockito.mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter should not be invoked");
        });
        return Stream.of(arguments("Method must ignore a non‑null Converter; result based on 01:02:03.000000004", LocalTime.of(1, 2, 3, 4), dummy, 3_723.000000004), arguments("Even when Converter throws, toDouble must not call it; 05:30:00 → 19 800 seconds", LocalTime.of(5, 30, 0, 0), throwing, 19_800.0));
    }

    @Test
    @DisplayName("toDouble returns 0.0 for LocalTime.MIDNIGHT")
    void testMidnight() {
        LocalTime lt = LocalTime.MIDNIGHT;
        double result = LocalTimeConversions.toDouble(lt, converter);
        assertEquals(0.0, result, 1e-12, "Midnight should convert to 0.0 seconds");
    }

    @Test
    @DisplayName("toDouble returns correct seconds for LocalTime.NOON")
    void testNoon() {
        // 12:00
        LocalTime lt = LocalTime.NOON;
        // 43200 seconds
        double expected = 12 * 60 * 60;
        double result = LocalTimeConversions.toDouble(lt, converter);
        assertEquals(expected, result, 1e-12, "Noon should convert to 43200.0 seconds");
    }

    @Test
    @DisplayName("toDouble converts a typical LocalTime with nanosecond precision")
    void testTypicalTime() {
        // 13:45:30.123456789
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        double expected = lt.toNanoOfDay() / 1.0E9;
        double result = LocalTimeConversions.toDouble(lt, converter);
        assertEquals(expected, result, 1e-12, "Conversion should match nanosecond precision");
    }

    @Test
    @DisplayName("toDouble throws NullPointerException when source is null")
    void testNullSource() {
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toDouble(null, converter), "Passing null should result in NullPointerException");
    }

    @Test
    @DisplayName("toDouble throws ClassCastException for non‑LocalTime source")
    void testInvalidSourceType() {
        String notALocalTime = "13:45";
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toDouble(notALocalTime, converter), "Passing a non‑LocalTime should cause ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validLocalTimeProvider")
    @DisplayName("Valid LocalTime conversions")
    void testValidLocalTimeConversions(String description, LocalTime from, double expected) {
        double result = LocalTimeConversions.toDouble(from, converter);
        assertEquals(expected, result, 1e-12, () -> "Failed conversion: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nanosecondEdgeProvider")
    @DisplayName("Nanosecond edge cases")
    void testNanosecondEdgeCases(String description, LocalTime from, double expected) {
        double result = LocalTimeConversions.toDouble(from, converter);
        assertEquals(expected, result, 1e-12, () -> "Failed nanosecond edge case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("converterHandlingProvider")
    @DisplayName("Converter argument handling")
    void testConverterArgumentHandling(String description, LocalTime from, Converter customConverter, double expected) {
        double result = LocalTimeConversions.toDouble(from, customConverter);
        assertEquals(expected, result, 1e-12, () -> "Converter handling failed: " + description);
    }
}
