package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toAtomicLong_Test {

    /**
     * Shared {@link Converter} used by all test methods.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; using the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the test tidy.
        converter = null;
    }

    private static Stream<Arguments> primitiveAndWrapperProvider() {
        return Stream.of(Arguments.of("Direct Long value should be wrapped unchanged.", 123L, 123L), Arguments.of("Integer is converted to long and wrapped.", Integer.valueOf(456), 456L), Arguments.of("Short is widened to long.", Short.valueOf((short) 7), 7L));
    }

    private static Stream<Arguments> stringConversionProvider() {
        return Stream.of(Arguments.of("String containing a decimal number should be parsed.", "789", 789L), Arguments.of("Negative numeric string should be parsed correctly.", "-42", -42L), // Hexadecimal detection is supported by the library (radix detection)
        Arguments.of("Hexadecimal string should be parsed (0xFF → 255).", "0xFF", 255L));
    }

    private static Stream<Arguments> bigNumberProvider() {
        return Stream.of(Arguments.of("BigDecimal is truncated to its long value (floor).", new BigDecimal("12345.67"), 12345L), Arguments.of("BigInteger is converted directly to long.", new BigInteger("9876543210"), 9876543210L), Arguments.of("AtomicLong value is extracted and re‑wrapped.", new AtomicLong(555L), 555L), Arguments.of("AtomicInteger is converted to long.", new AtomicInteger(42), 42L));
    }

    private static Stream<Arguments> timeProvider() {
        // LocalTime conversion: the library divides nanoseconds‑of‑day by 1_000
        // (see JSON expectation 45_296_789 for 12:34:56.789).
        LocalTime lt = LocalTime.of(12, 34, 56, 789_000_000);
        // 45_296_789
        long expectedNanosDiv1000 = lt.toNanoOfDay() / 1_000L;
        Calendar cal = Calendar.getInstance();
        // epoch
        cal.setTimeInMillis(0L);
        return Stream.of(Arguments.of("LocalTime is converted to nanoseconds‑since‑midnight divided by 1_000.", lt, expectedNanosDiv1000), Arguments.of("Calendar at epoch should convert to 0.", cal, 0L));
    }

    private static Stream<Arguments> emptyOrWhitespaceProvider() {
        return Stream.of(Arguments.of("Empty string cannot be parsed as a number.", ""), Arguments.of("String containing only whitespace is not a valid numeric representation.", "   "));
    }

    @Test
    void toAtomicLong_fromLong_returnsSameValue() {
        // GIVEN
        Long source = 123L;
        // WHEN
        AtomicLong result = LocalTimeConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    void toAtomicLong_fromInteger_returnsConvertedLong() {
        // GIVEN
        Integer source = 42;
        // WHEN
        AtomicLong result = LocalTimeConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(42L, result.get());
    }

    @Test
    void toAtomicLong_fromString_returnsParsedLong() {
        // GIVEN
        String source = "987654321";
        // WHEN
        AtomicLong result = LocalTimeConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(987654321L, result.get());
    }

    @Test
    void toAtomicLong_fromLocalTime_returnsNanosOfDay() {
        // GIVEN
        // 01:02:03.004 (hours, minutes, seconds, nanoseconds)
        LocalTime source = LocalTime.of(1, 2, 3, 4_000_000);
        long expectedNanos = source.toNanoOfDay();
        // WHEN
        AtomicLong result = LocalTimeConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedNanos, result.get());
    }

    @Test
    void toAtomicLong_fromNull_returnsZeroAtomicLong() {
        // GIVEN
        Object source = null;
        // WHEN
        AtomicLong result = LocalTimeConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("primitiveAndWrapperProvider")
    void toAtomicLong_primitiveAndWrapperConversions(String description, Object input, long expected) {
        AtomicLong result = LocalTimeConversions.toAtomicLong(input, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringConversionProvider")
    void toAtomicLong_stringConversions(String description, String input, long expected) {
        AtomicLong result = LocalTimeConversions.toAtomicLong(input, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("bigNumberProvider")
    void toAtomicLong_bigNumberConversions(String description, Object input, long expected) {
        AtomicLong result = LocalTimeConversions.toAtomicLong(input, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("timeProvider")
    void toAtomicLong_timeConversions(String description, Object input, long expected) {
        AtomicLong result = LocalTimeConversions.toAtomicLong(input, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    @Test
    void toAtomicLong_unsupportedObject_throwsClassCastException() {
        Object unsupported = new Object();
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toAtomicLong(unsupported, converter), "Unsupported object type should raise ClassCastException");
    }

    @Test
    void toAtomicLong_nonNumericString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> LocalTimeConversions.toAtomicLong("not-a-number", converter), "Non‑numeric string should raise NumberFormatException");
    }

    @Test
    void toAtomicLong_stringOverflow_throwsNumberFormatException() {
        // Value larger than Long.MAX_VALUE
        String tooBig = "9223372036854775808";
        assertThrows(NumberFormatException.class, () -> LocalTimeConversions.toAtomicLong(tooBig, converter), "Overflowing numeric string should raise NumberFormatException");
    }

    @Test
    void toAtomicLong_bigIntegerOverflow_throwsArithmeticException() {
        // Long.MAX_VALUE + 1
        BigInteger tooBig = new BigInteger("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> LocalTimeConversions.toAtomicLong(tooBig, converter), "Overflowing BigInteger should raise ArithmeticException");
    }

    @ParameterizedTest(name = "[{index}] Empty/whitespace string: {0}")
    @MethodSource("emptyOrWhitespaceProvider")
    void toAtomicLong_emptyOrWhitespaceString_throwsNumberFormatException(String description, String input) {
        assertThrows(NumberFormatException.class, () -> LocalTimeConversions.toAtomicLong(input, converter), description);
    }

    @Test
    void toAtomicLong_malformedHexString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> LocalTimeConversions.toAtomicLong("0xGHI", converter), "Malformed hexadecimal string should raise NumberFormatException");
    }

    @Test
    void toAtomicLong_withMockConverter_returnsMockedValue() {
        // Arrange – mock Converter to return 777L for any conversion request.
        Converter mockConverter = mock(Converter.class);
        // The library's internal call is unknown; we stub a generic `convert` method.
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn(777L);
        // Act
        AtomicLong result = LocalTimeConversions.toAtomicLong("custom", mockConverter);
        // Assert
        assertNotNull(result);
        assertEquals(777L, result.get(), "Mocked converter should dictate the resulting long value.");
        // Verify that the mock was indeed used.
        verify(mockConverter, atLeastOnce()).convert(any(), eq(Long.class));
    }

    @Test
    void toAtomicLong_converterThrowsException_propagatesIt() {
        // Arrange – mock Converter that throws IllegalStateException.
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(Long.class))).thenThrow(new IllegalStateException("converter failure"));
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> LocalTimeConversions.toAtomicLong("any", mockConverter), "Exception from Converter should be propagated unchanged.");
        assertEquals("converter failure", ex.getMessage());
    }
}
