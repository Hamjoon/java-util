package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toAtomicInteger_Test {

    /**
     * The {@link Converter} used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – they provide the standard conversion mappings.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> validNumberProvider() {
        return Stream.of(// String containing a valid integer
        Arguments.of("42", null, 42), // Integer primitive (autoboxed)
        Arguments.of(7, null, 7), // Long value (truncated)
        Arguments.of(Long.valueOf(123L), null, 123), // BigDecimal (intValue truncates)
        Arguments.of(BigDecimal.valueOf(99.99), null, 99), // BigInteger (intValue truncates)
        Arguments.of(BigInteger.valueOf(555), null, 555));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(Arguments.of(null, null, 0), Arguments.of("", null, 0));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> overflowProvider() {
        return Stream.of(Arguments.of(Integer.MAX_VALUE, null, Integer.MAX_VALUE), Arguments.of(Integer.MIN_VALUE, null, Integer.MIN_VALUE), // Long.MAX_VALUE cast to int yields -1
        Arguments.of(Long.MAX_VALUE, null, -1));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> primitiveNumberProvider() {
        return Stream.of(Arguments.of(3.14f, null, 3), Arguments.of(2.99d, null, 2), Arguments.of((short) 9, null, 9), Arguments.of((byte) -4, null, -4));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> whitespaceSignProvider() {
        return Stream.of(Arguments.of("   56   ", null, 56), Arguments.of("+77", null, 77), Arguments.of("-88", null, -88));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> largeIntegralProvider() {
        return Stream.of(// Long.MIN_VALUE cast to int → Integer.MIN_VALUE
        Arguments.of(Long.MIN_VALUE, null, Integer.MIN_VALUE), // BigInteger(Long.MAX_VALUE) → -1 after int cast
        Arguments.of(BigInteger.valueOf(Long.MAX_VALUE), null, -1), // BigInteger(Long.MIN_VALUE) → Integer.MIN_VALUE
        Arguments.of(BigInteger.valueOf(-9223372036854775808L), null, Integer.MIN_VALUE));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> atomicWrapperProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(321), null, 321), // AtomicLong value exceeds int range → cast
        Arguments.of(new AtomicLong(9876543210L), null, (int) 9876543210L));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of(true), Arguments.of(Date.from(Instant.now())), Arguments.of(new StringBuilder("123")), Arguments.of(Month.JANUARY), Arguments.of('5'));
    }

    @Test
    void toAtomicInteger_withInteger_returnsSameValue() {
        // given
        Integer source = 42;
        // when
        AtomicInteger result = NumberConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result, "Resulting AtomicInteger should not be null");
        assertEquals(42, result.get(), "AtomicInteger should contain the same int value as the source Integer");
    }

    @Test
    void toAtomicInteger_withStringNumber_returnsParsedValue() {
        // given
        String source = "123";
        // when
        AtomicInteger result = NumberConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(123, result.get(), "String containing a numeric value should be converted to the corresponding int");
    }

    @Test
    void toAtomicInteger_withNull_returnsZero() {
        // given
        Object source = null;
        // when
        AtomicInteger result = NumberConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.get(), "Null source should be converted to the default int value 0");
    }

    @Test
    void toAtomicInteger_returnsNewInstanceEachCall() {
        // given
        Integer source = 7;
        // when
        AtomicInteger first = NumberConversions.toAtomicInteger(source, converter);
        AtomicInteger second = NumberConversions.toAtomicInteger(source, converter);
        // then
        assertNotSame(first, second, "Each call should produce a distinct AtomicInteger instance");
        // Mutating one should not affect the other
        first.incrementAndGet();
        assertEquals(7, second.get(), "Changing the first instance must not change the second instance");
    }

    @ParameterizedTest(name = "[{index}] from={0} (converter={1}) → {2}")
    @MethodSource("validNumberProvider")
    void toAtomicInteger_validNumbers(Object from, Converter customConverter, int expected) {
        Converter used = customConverter != null ? customConverter : converter;
        AtomicInteger result = NumberConversions.toAtomicInteger(from, used);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → {2}")
    @MethodSource("nullAndEmptyProvider")
    void toAtomicInteger_nullAndEmpty(Object from, Converter customConverter, int expected) {
        Converter used = customConverter != null ? customConverter : converter;
        AtomicInteger result = NumberConversions.toAtomicInteger(from, used);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicInteger_invalidString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toAtomicInteger("notANumber", converter));
    }

    @Test
    void toAtomicInteger_unsupportedObject_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toAtomicInteger(new Object(), converter));
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicInteger_customConverterReturns777(@Mock Converter mockConverter) {
        // Mock any conversion request to return Integer 777
        when(mockConverter.convert(any(), eq(Integer.class))).thenReturn(777);
        AtomicInteger result = NumberConversions.toAtomicInteger("custom", mockConverter);
        assertNotNull(result);
        assertEquals(777, result.get());
    }

    @Test
    void toAtomicInteger_customConverterReturnsNull(@Mock Converter mockConverter) {
        // Simulate a converter that returns null for a null source
        when(mockConverter.convert(isNull(), eq(Integer.class))).thenReturn(null);
        AtomicInteger result = NumberConversions.toAtomicInteger(null, mockConverter);
        assertNotNull(result);
        // Null is interpreted as zero by toInt → AtomicInteger(0)
        assertEquals(0, result.get());
    }

    @ParameterizedTest(name = "[{index}] from={0} → {2}")
    @MethodSource("overflowProvider")
    void toAtomicInteger_overflowCases(Object from, Converter customConverter, int expected) {
        Converter used = customConverter != null ? customConverter : converter;
        AtomicInteger result = NumberConversions.toAtomicInteger(from, used);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "[{index}] from={0} → {2}")
    @MethodSource("primitiveNumberProvider")
    void toAtomicInteger_primitiveNumbers(Object from, Converter customConverter, int expected) {
        Converter used = customConverter != null ? customConverter : converter;
        AtomicInteger result = NumberConversions.toAtomicInteger(from, used);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → {2}")
    @MethodSource("whitespaceSignProvider")
    void toAtomicInteger_stringWhitespaceAndSign(Object from, Converter customConverter, int expected) {
        Converter used = customConverter != null ? customConverter : converter;
        AtomicInteger result = NumberConversions.toAtomicInteger(from, used);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicInteger_stringWithTrailingChars_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toAtomicInteger("123abc", converter));
    }

    @ParameterizedTest(name = "[{index}] from={0} → {2}")
    @MethodSource("largeIntegralProvider")
    void toAtomicInteger_largeIntegralTypes(Object from, Converter customConverter, int expected) {
        Converter used = customConverter != null ? customConverter : converter;
        AtomicInteger result = NumberConversions.toAtomicInteger(from, used);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "[{index}] from={0} → {2}")
    @MethodSource("atomicWrapperProvider")
    void toAtomicInteger_atomicWrappers(Object from, Converter customConverter, int expected) {
        Converter used = customConverter != null ? customConverter : converter;
        AtomicInteger result = NumberConversions.toAtomicInteger(from, used);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "[{index}] from={0} → IllegalArgumentException")
    @MethodSource("unsupportedProvider")
    void toAtomicInteger_unsupportedTypes(Object from) {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toAtomicInteger(from, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicInteger_converterThrowsException(@Mock Converter mockConverter) {
        when(mockConverter.convert(any(), eq(Integer.class))).thenThrow(new IllegalStateException("converter failure"));
        assertThrows(IllegalStateException.class, () -> NumberConversions.toAtomicInteger("anyValue", mockConverter));
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicInteger_converterReturnsBoolean(@Mock Converter mockConverter) {
        when(mockConverter.convert(any(), eq(Integer.class))).thenReturn(Boolean.TRUE);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toAtomicInteger("ignored", mockConverter));
    }

    @Test
    void toAtomicInteger_converterReturnsNonNumericString(@Mock Converter mockConverter) {
        when(mockConverter.convert(any(), eq(Integer.class))).thenReturn("notANumber");
        assertThrows(NumberFormatException.class, () -> NumberConversions.toAtomicInteger("ignored", mockConverter));
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicInteger_whitespaceOnlyString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toAtomicInteger("   ", converter));
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicInteger_floatNaN_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toAtomicInteger(Float.NaN, converter));
    }

    @Test
    void toAtomicInteger_doubleInfinity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toAtomicInteger(Double.POSITIVE_INFINITY, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicInteger_localDate_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toAtomicInteger(LocalDate.now(), converter));
    }

    @Test
    void toAtomicInteger_instant_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toAtomicInteger(Instant.now(), converter));
    }
}
