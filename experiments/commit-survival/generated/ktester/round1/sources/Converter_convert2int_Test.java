package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2int_Test {

    /**
     * Holds the original singleton instance so it can be restored after the tests.
     */
    private static Converter originalInstance;

    /**
     * Mock that will replace {@code Converter.instance} during each test.
     */
    @Mock
    private Converter mockInstance;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the real instance for later restoration.
        originalInstance = Converter.instance;
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default replace the static instance with our mock.
        Converter.instance = mockInstance;
        // Default behaviour: when the mock is asked to convert anything to int, return 42.
        when(mockInstance.convert(any(), eq(int.class))).thenReturn(42);
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original static instance so that other test classes are not affected.
        Converter.instance = originalInstance;
        // Reset the mock for the next test method.
        Mockito.reset(mockInstance);
    }

    /**
     * Helper that restores the real singleton before each conversion test.
     */
    private void useRealConverter() {
        // The originalInstance captured in @BeforeAll is a fully‑functional Converter.
        Converter.instance = originalInstance;
    }

    private static Stream<Arguments> stringNumericProvider() {
        return Stream.of(Arguments.of("12345", 12345), Arguments.of("-2147483648", -2147483648), Arguments.of("2147483647", 2147483647), Arguments.of("   42   ", 42));
    }

    private static Stream<Arguments> numberTypesProvider() {
        return Stream.of(Arguments.of(123L, 123), Arguments.of(123.0f, 123), Arguments.of(123.9d, 123), Arguments.of(new BigInteger("987654321"), 987654321), Arguments.of(new BigDecimal("456.78"), 456));
    }

    private static Stream<Arguments> atomicTypesProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(777), 777), Arguments.of(new AtomicLong(555L), 555));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// String outside int range → NumberFormatException
        Arguments.of("12345678901234567890", NumberFormatException.class), // BigInteger too large → ArithmeticException
        Arguments.of(new BigInteger("12345678901234567890"), ArithmeticException.class), // BigDecimal magnitude beyond int → ArithmeticException
        Arguments.of(new BigDecimal("1E40"), ArithmeticException.class), // AtomicLong > Integer.MAX_VALUE → ArithmeticException
        Arguments.of(new AtomicLong(Long.MAX_VALUE), ArithmeticException.class), // AtomicInteger underflow → ArithmeticException
        Arguments.of(new AtomicInteger(Integer.MIN_VALUE - 1), ArithmeticException.class), // Date far beyond int range → ArithmeticException
        Arguments.of(new Date(Long.MAX_VALUE), ArithmeticException.class), // LocalDate far in the future → ArithmeticException
        Arguments.of(LocalDate.of(3000, 1, 1), ArithmeticException.class), // ZonedDateTime far in the future → ArithmeticException
        Arguments.of(ZonedDateTime.of(3000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), ArithmeticException.class), // Array type → IllegalArgumentException
        Arguments.of(new char[] { 'a', 'b' }, IllegalArgumentException.class));
    }

    @Test
    void convert2int_nullInput_returnsLogicalZero() {
        // Given a null argument
        Object input = null;
        // When we call the method under test
        int result = Converter.convert2int(input);
        // Then we expect the logical zero for primitive int
        assertEquals(0, result, "convert2int(null) should return 0");
        // No interaction with the mock should have occurred for a null input
        verifyNoInteractions(mockInstance);
    }

    @Test
    void convert2int_nonNullInput_delegatesToInstanceConvert() {
        // Given a non‑null object (the actual value is irrelevant for this test)
        Object input = "any value";
        // When we call the method under test
        int result = Converter.convert2int(input);
        // Then the result should be whatever the mock was programmed to return
        assertEquals(42, result, "convert2int should return the value supplied by the mock");
        // And the static instance should have been asked to perform the conversion
        verify(mockInstance, times(1)).convert(input, int.class);
    }

    @ParameterizedTest(name = "String \"{0}\" → {1}")
    @MethodSource("stringNumericProvider")
    void convert2int_stringNumericConversion(String input, int expected) {
        useRealConverter();
        int result = Converter.convert2int(input);
        assertEquals(expected, result, () -> "convert2int should correctly parse numeric string \"" + input + "\"");
    }

    @ParameterizedTest(name = "Number {0} ({1}) → {2}")
    @MethodSource("numberTypesProvider")
    void convert2int_numberTypesConversion(Object input, int expected) {
        useRealConverter();
        int result = Converter.convert2int(input);
        assertEquals(expected, result, () -> "convert2int should convert " + input.getClass().getSimpleName() + " correctly");
    }

    @ParameterizedTest(name = "Atomic {0} → {1}")
    @MethodSource("atomicTypesProvider")
    void convert2int_atomicTypesConversion(Object input, int expected) {
        useRealConverter();
        int result = Converter.convert2int(input);
        assertEquals(expected, result, () -> "convert2int should convert atomic type " + input.getClass().getSimpleName());
    }

    @Test
    void convert2int_booleanTrue_returnsOne() {
        useRealConverter();
        assertEquals(1, Converter.convert2int(true), "Boolean true should be converted to 1");
    }

    @Test
    void convert2int_booleanFalse_returnsZero() {
        useRealConverter();
        assertEquals(0, Converter.convert2int(false), "Boolean false should be converted to 0");
    }

    @Test
    void convert2int_characterLetter_returnsUnicode() {
        useRealConverter();
        assertEquals(65, Converter.convert2int('A'), "Character 'A' should be converted to its Unicode code point 65");
    }

    @Test
    void convert2int_characterUnicodeOmega_returnsCodePoint() {
        useRealConverter();
        assertEquals(937, Converter.convert2int('\u03A9'), "Unicode character Ω should be converted to its code point 937");
    }

    @Test
    void convert2int_dateEpoch_returnsZeroSeconds() {
        useRealConverter();
        // 0 ms since epoch
        Date epoch = new Date(0L);
        assertEquals(0, Converter.convert2int(epoch), "Epoch Date (0 ms) should be converted to 0 seconds");
    }

    @Test
    void convert2int_localDateEpoch_returnsZeroDays() {
        useRealConverter();
        LocalDate epoch = LocalDate.of(1970, 1, 1);
        assertEquals(0, Converter.convert2int(epoch), "LocalDate 1970‑01‑01 should be converted to 0 epoch days");
    }

    @Test
    void convert2int_longJustAboveMax_overflowsToMin() {
        useRealConverter();
        // Integer.MAX_VALUE + 1
        long value = 2147483648L;
        assertEquals(-2147483648, Converter.convert2int(value), "Overflow should wrap to Integer.MIN_VALUE");
    }

    @Test
    void convert2int_longJustBelowMin_overflowsToMax() {
        useRealConverter();
        // Integer.MIN_VALUE - 1
        long value = -2147483649L;
        assertEquals(2147483647, Converter.convert2int(value), "Underflow should wrap to Integer.MAX_VALUE");
    }

    @Test
    void convert2int_unsupportedObject_throwsIllegalArgumentException() {
        useRealConverter();
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2int(unsupported), "Converting an unsupported type should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "convert2int({0}) throws {1}")
    @MethodSource("exceptionProvider")
    void convert2int_exceptionScenarios(Object input, Class<? extends Throwable> expectedException) {
        useRealConverter();
        assertThrows(expectedException, () -> Converter.convert2int(input), () -> "Expected " + expectedException.getSimpleName() + " for input: " + input);
    }
}
