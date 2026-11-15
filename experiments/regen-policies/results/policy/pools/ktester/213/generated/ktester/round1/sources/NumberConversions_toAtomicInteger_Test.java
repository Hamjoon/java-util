package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toAtomicInteger_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared real Converter instance for normal test cases.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up, but null‑ify the reference for completeness.
        converter = null;
    }

    /**
     * Provides arguments for the majority of test cases (numeric, string, boolean, …).
     */
    private static Stream<Arguments> normalCases() {
        return Stream.of(// Direct numeric types
        Arguments.of(42, null, 42, null), // low 32‑bits
        Arguments.of(1234567890123L, null, (int) 1234567890123L, null), Arguments.of(3.99, null, 3, null), Arguments.of((byte) 7, null, 7, null), Arguments.of((short) -300, null, -300, null), Arguments.of(2.7f, null, 2, null), Arguments.of(new BigDecimal("1234.56"), null, 1234, null), Arguments.of(new BigInteger("9876543210"), null, new BigInteger("9876543210").intValue(), null), // Edge integer values
        Arguments.of(Integer.MAX_VALUE, null, Integer.MAX_VALUE, null), Arguments.of(Integer.MIN_VALUE, null, Integer.MIN_VALUE, null), // -1
        Arguments.of(Long.MAX_VALUE, null, (int) Long.MAX_VALUE, null), // Integer.MIN_VALUE
        Arguments.of(Long.MIN_VALUE, null, (int) Long.MIN_VALUE, null), // String parsing
        Arguments.of("56", null, 56, null), Arguments.of("-12", null, -12, null), Arguments.of("invalid-number", null, null, NumberFormatException.class), // whitespace not trimmed
        Arguments.of(" 42 ", null, null, NumberFormatException.class), Arguments.of("+15", null, 15, null), Arguments.of("", null, null, NumberFormatException.class), Arguments.of("0x10", null, null, NumberFormatException.class), Arguments.of("123.45", null, null, NumberFormatException.class), // Boolean handling
        Arguments.of(Boolean.TRUE, null, 1, null), Arguments.of(Boolean.FALSE, null, 0, null), // Special floating values
        Arguments.of(Double.NaN, null, 0, null), Arguments.of(Double.POSITIVE_INFINITY, null, Integer.MAX_VALUE, null), Arguments.of(Double.NEGATIVE_INFINITY, null, Integer.MIN_VALUE, null), // Atomic wrappers
        Arguments.of(new AtomicLong(12345L), null, 12345, null), Arguments.of(new AtomicBoolean(true), null, 1, null), Arguments.of(new AtomicBoolean(false), null, 0, null), // Character conversion
        Arguments.of('A', null, (int) 'A', null), Arguments.of(Character.valueOf('9'), null, (int) '9', null), // Null source with real converter (should become 0)
        Arguments.of(null, null, 0, null));
    }

    /**
     * Provides arguments for cases that require a mocked {@link Converter}.
     */
    private static Stream<Arguments> mockConverterCases() {
        return Stream.of(// Converter fallback – returns 99
        Arguments.of("anyObject", mockReturning(99), 99, null), // Converter fallback – throws ConversionException
        Arguments.of("anyObject", mockThrowingConversionException(), null, ConversionException.class), // Null source with converter returning 77
        Arguments.of(null, mockReturning(77), 77, null), // Converter returns null → NullPointerException
        Arguments.of("anyObject", mockReturningNull(), null, NullPointerException.class), // Converter returns non‑numeric object → ConversionException
        Arguments.of("anyObject", mockReturning("not-a-number"), null, ConversionException.class));
    }

    /**
     * Provides arguments for unsupported types that should raise {@link ConversionException}.
     */
    private static Stream<Arguments> unsupportedCases() {
        return Stream.of(// Array handling
        Arguments.of(new int[] { 1, 2, 3 }, null, null, ConversionException.class), // Collection handling
        Arguments.of(Arrays.asList(4, 5, 6), null, null, ConversionException.class), // Enum handling
        Arguments.of(DayOfWeek.MONDAY, null, null, ConversionException.class), // AtomicInteger source without converter – should fail
        Arguments.of(new AtomicInteger(77), null, null, ConversionException.class), // Null converter with unsupported type (plain Object)
        Arguments.of(new Object(), null, null, ConversionException.class));
    }

    /**
     * Creates a mock {@link Converter} that returns the supplied value when asked for an {@code Integer}.
     */
    private static Converter mockReturning(Object value) {
        Converter mock = mock(Converter.class);
        // The internal implementation of NumberConversions.toInt calls
        // converter.convert(source, Integer.class).  We stub that signature.
        when(mock.convert(any(), eq(Integer.class))).thenReturn(value);
        return mock;
    }

    /**
     * Creates a mock {@link Converter} that returns {@code null}.
     */
    private static Converter mockReturningNull() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Integer.class))).thenReturn(null);
        return mock;
    }

    /**
     * Creates a mock {@link Converter} that throws a {@link ConversionException}.
     */
    private static Converter mockThrowingConversionException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Integer.class))).thenThrow(new ConversionException("Mock conversion failure"));
        return mock;
    }

    @Test
    void toAtomicInteger_fromInteger_returnsSameValue() {
        // given
        Integer source = 42;
        // when
        AtomicInteger result = NumberConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result, "Resulting AtomicInteger should not be null");
        assertEquals(42, result.get(), "AtomicInteger should contain the same int value as the source Integer");
    }

    @Test
    void toAtomicInteger_fromStringNumeric_returnsParsedValue() {
        // given
        String source = "123";
        // when
        AtomicInteger result = NumberConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    @Test
    void toAtomicInteger_fromNull_returnsZero() {
        // given
        Object source = null;
        // when
        AtomicInteger result = NumberConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.get(), "Null source should be converted to 0 for primitive int conversion");
    }

    @Test
    void toAtomicInteger_fromDoubleTruncatesFraction() {
        // given
        // Converter should truncate toward zero when converting to int
        Double source = 7.9;
        // when
        AtomicInteger result = NumberConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(7, result.get(), "Double to int conversion should truncate the fractional part");
    }

    @Test
    void toAtomicInteger_fromBooleanTrue_returnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        AtomicInteger result = NumberConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(1, result.get(), "Boolean true should be converted to int value 1");
    }

    @Test
    void toAtomicInteger_fromBooleanFalse_returnsZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        AtomicInteger result = NumberConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.get(), "Boolean false should be converted to int value 0");
    }

    @ParameterizedTest(name = "[{index}] from={0}, expected={2}")
    @MethodSource("normalCases")
    void toAtomicInteger_normalCases(Object from, Converter ignoredConverter, Integer expectedValue, Class<? extends Throwable> expectedException) {
        // Use the real converter for these cases
        Converter conv = realConverter;
        if (expectedException != null) {
            assertThrows(expectedException, () -> NumberConversions.toAtomicInteger(from, conv), () -> "Expected exception " + expectedException.getSimpleName() + " for input: " + from);
        } else {
            AtomicInteger result = NumberConversions.toAtomicInteger(from, conv);
            assertNotNull(result, "Result should never be null");
            assertEquals(expectedValue.intValue(), result.get(), () -> "Incorrect conversion result for input: " + from);
        }
    }

    @ParameterizedTest(name = "[{index}] from={0}, mockConverter, expected={2}")
    @MethodSource("mockConverterCases")
    void toAtomicInteger_mockConverterCases(Object from, Converter mockConverter, Integer expectedValue, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> NumberConversions.toAtomicInteger(from, mockConverter), () -> "Expected exception " + expectedException.getSimpleName() + " for mock converter case with input: " + from);
        } else {
            AtomicInteger result = NumberConversions.toAtomicInteger(from, mockConverter);
            assertNotNull(result, "Result should not be null when mock converter supplies a value");
            assertEquals(expectedValue.intValue(), result.get(), () -> "Mock converter should produce value " + expectedValue + " for input: " + from);
        }
    }

    @ParameterizedTest(name = "[{index}] unsupported from={0}")
    @MethodSource("unsupportedCases")
    void toAtomicInteger_unsupportedTypes(Object from, Converter ignoredConverter, Integer ignored, Class<? extends Throwable> expectedException) {
        // All these cases should raise ConversionException regardless of the converter (null in our data)
        assertThrows(expectedException, () -> NumberConversions.toAtomicInteger(from, realConverter), () -> "Unsupported source type should cause " + expectedException.getSimpleName() + " for input: " + from);
    }
}
