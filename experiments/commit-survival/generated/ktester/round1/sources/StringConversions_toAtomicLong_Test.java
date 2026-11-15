package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toAtomicLong_Test {

    /**
     * Shared Converter used by all test cases.
     */
    private static Converter converter;

    @Mock
    Converter // injected by Mockito
    mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides a
        // sensible, empty configuration suitable for the conversion logic exercised in these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Help the garbage collector and make the test class tidy.
        converter = null;
    }

    private static Stream<Arguments> simpleNumberProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(12345), 12345L), Arguments.of(Long.valueOf(9876543210L), 9876543210L), // truncation
        Arguments.of(Double.valueOf(3.14159), 3L), Arguments.of(Byte.valueOf((byte) 7), 7L), Arguments.of(Short.valueOf((short) -12), -12L));
    }

    private static Stream<Arguments> numericStringProvider() {
        return Stream.of(Arguments.of("123456", 123456L), // leading/trailing whitespace
        Arguments.of("   42   ", 42L), // negative number
        Arguments.of("-98765", -98765L));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("abc123", NumberFormatException.class), Arguments.of("", NumberFormatException.class), // whitespace only
        Arguments.of("   ", NumberFormatException.class), // hexadecimal not supported
        Arguments.of("0xFF", NumberFormatException.class));
    }

    private static Stream<Arguments> otherNumberProvider() {
        return Stream.of(Arguments.of(new BigInteger("1234567890123456789"), 1234567890123456789L), // truncation (floor)
        Arguments.of(new BigDecimal("12345.678"), 12345L), Arguments.of(new AtomicLong(555L), 555L));
    }

    private static Stream<Arguments> boundaryValueProvider() {
        return Stream.of(Arguments.of("9223372036854775807", Long.MAX_VALUE), Arguments.of("-9223372036854775808", Long.MIN_VALUE));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE), Arguments.of(new Object()));
    }

    @Test
    @DisplayName("toAtomicLong converts a Long value correctly")
    void testToAtomicLongFromLong() {
        // given
        Long source = 123L;
        // when
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(123L, result.get(), "AtomicLong value should match the source Long");
    }

    @Test
    @DisplayName("toAtomicLong converts a numeric String correctly")
    void testToAtomicLongFromString() {
        // given
        String source = "456";
        // when
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(456L, result.get(), "AtomicLong value should match the parsed String");
    }

    @Test
    @DisplayName("toAtomicLong returns 0L when the source is null")
    void testToAtomicLongFromNull() {
        // given
        Object source = null;
        // when
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null even for null input");
        assertEquals(0L, result.get(), "AtomicLong value should be 0L for null input");
    }

    @Test
    @DisplayName("toAtomicLong throws NullPointerException for null source")
    void testToAtomicLong_NullSource() {
        assertThrows(NullPointerException.class, () -> StringConversions.toAtomicLong(null, converter), "Passing null should trigger NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] from={0} → {1}")
    @MethodSource("simpleNumberProvider")
    @DisplayName("toAtomicLong converts simple numeric types correctly")
    void testToAtomicLong_SimpleNumbers(Object from, long expected) {
        AtomicLong result = StringConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("numericStringProvider")
    @DisplayName("toAtomicLong parses numeric strings")
    void testToAtomicLong_FromString(String source, long expected) {
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → throws {1}")
    @MethodSource("invalidStringProvider")
    @DisplayName("toAtomicLong throws NumberFormatException for non‑numeric strings")
    void testToAtomicLong_InvalidString(String source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringConversions.toAtomicLong(source, converter), "Input \"" + source + "\" should trigger " + expectedException.getSimpleName());
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("otherNumberProvider")
    @DisplayName("toAtomicLong converts other numeric types")
    void testToAtomicLong_OtherNumbers(Object source, long expected) {
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    @DisplayName("toAtomicLong uses a custom Converter to transform input before parsing")
    void testToAtomicLong_WithMockConverter() {
        // The mock converter will ignore the original input and return the string "777".
        when(mockConverter.convert(any(), eq(String.class))).thenReturn("777");
        // The source value itself is irrelevant because the mock overrides it.
        Object source = "ignored";
        AtomicLong result = StringConversions.toAtomicLong(source, mockConverter);
        assertNotNull(result);
        assertEquals(777L, result.get());
        // Verify that the mock was indeed called.
        verify(mockConverter, times(1)).convert(any(), eq(String.class));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("boundaryValueProvider")
    @DisplayName("toAtomicLong correctly handles Long boundary values")
    void testToAtomicLong_BoundaryValues(String source, long expected) {
        AtomicLong result = StringConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    @DisplayName("toAtomicLong throws NumberFormatException when BigDecimal exceeds Long range")
    void testToAtomicLong_BigDecimalOutOfRange() {
        // Long.MAX_VALUE + 1
        BigDecimal tooBig = new BigDecimal("9223372036854775808");
        assertThrows(NumberFormatException.class, () -> StringConversions.toAtomicLong(tooBig, converter));
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws IllegalArgumentException")
    @MethodSource("unsupportedSourceProvider")
    @DisplayName("toAtomicLong throws IllegalArgumentException for unsupported types")
    void testToAtomicLong_UnsupportedSource(Object source) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong(source, converter));
    }

    @Test
    @DisplayName("toAtomicLong throws IllegalArgumentException when Converter returns non‑numeric type")
    void testToAtomicLong_ConverterReturnsUnsupported() {
        // Mock converter returns a Boolean, which cannot be turned into a long.
        when(mockConverter.convert(any(), any())).thenReturn(Boolean.TRUE);
        Object source = "ignored";
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicLong(source, mockConverter));
    }
}
