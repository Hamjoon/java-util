package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2long_Test {

    /**
     * Holds the original {@link Converter#instance} so it can be restored after a test.
     */
    private static Converter originalInstance;

    /**
     * Mock that will replace {@link Converter#instance} during the delegation test.
     */
    private Converter mockInstance;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Preserve the real instance.
        originalInstance = Converter.instance;
        // Create a mock Converter and inject it into the static field.
        mockInstance = mock(Converter.class);
        setStaticInstance(mockInstance);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Restore the original static instance to keep the environment clean.
        setStaticInstance(originalInstance);
    }

    /**
     * Helper that uses reflection to replace the {@code Converter.instance} static field.
     */
    private static void setStaticInstance(Converter instance) throws Exception {
        Field field = Converter.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, instance);
    }

    // ---------- Numeric wrappers and primitives ----------
    private static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(Arguments.of(Long.valueOf(1_234_567_890_123L), 1_234_567_890_123L), Arguments.of(Integer.valueOf(42), 42L), Arguments.of(Short.valueOf((short) -7), -7L), Arguments.of(Byte.valueOf((byte) 3), 3L), // truncation
        Arguments.of(Double.valueOf(9.99), 9L), // truncation
        Arguments.of(Float.valueOf(-2.5f), -2L));
    }

    // ---------- Atomic number types ----------
    private static Stream<Arguments> atomicNumberProvider() {
        return Stream.of(Arguments.of(new AtomicLong(9_876_543_210L), 9_876_543_210L), Arguments.of(new AtomicInteger(-12_345), -12_345L));
    }

    // ---------- Big number types ----------
    private static Stream<Arguments> bigNumberProvider() {
        return Stream.of(// fits in long
        Arguments.of(new BigInteger("12345678901234567890"), 12_345_678_901_234_567_890L), // truncates fraction
        Arguments.of(new BigDecimal("-9876543210.99"), -9_876_543_210L));
    }

    // ---------- Boolean values ----------
    private static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, 1L), Arguments.of(Boolean.FALSE, 0L));
    }

    // ---------- Numeric strings ----------
    private static Stream<Arguments> numericStringProvider() {
        return Stream.of(Arguments.of("0", 0L), Arguments.of("  42  ", 42L), Arguments.of("-1234567890", -1_234_567_890L), Arguments.of("+777", 777L), Arguments.of("9223372036854775807", Long.MAX_VALUE));
    }

    // ---------- Non‑numeric strings (expect NumberFormatException) ----------
    private static Stream<String> nonNumericStringProvider() {
        return Stream.of("abc123", "", "   ");
    }

    // ---------- Overflow handling ----------
    private static Stream<Arguments> overflowProvider() {
        return Stream.of(// Long.MAX_VALUE + 1
        Arguments.of(new BigInteger("9223372036854775808")), // Slightly > Long.MAX_VALUE
        Arguments.of(new BigDecimal("9.223372036854775807E18")));
    }

    @Test
    void convert2long_NullInput_ReturnsLogicalZero() {
        // GIVEN a null argument
        Object input = null;
        // WHEN the conversion method is invoked
        long result = Converter.convert2long(input);
        // THEN the logical zero for long is returned
        assertEquals(CommonValues.LONG_ZERO, result, "convert2long(null) should return CommonValues.LONG_ZERO");
    }

    @Test
    void convert2long_NonNullInput_DelegatesToInstanceAndReturnsResult() {
        // GIVEN a non‑null input and a stubbed conversion result
        String input = "12345";
        long expected = 12_345L;
        when(mockInstance.convert(input, long.class)).thenReturn(expected);
        // WHEN the static helper is called
        long actual = Converter.convert2long(input);
        // THEN the value returned by the mock is propagated
        assertEquals(expected, actual, "convert2long should return the value produced by instance.convert");
        // AND the mock was invoked exactly once with the correct arguments
        verify(mockInstance, times(1)).convert(input, long.class);
    }

    @ParameterizedTest(name = "[{index}] convert2long({0}) => {1}")
    @MethodSource("numericWrapperProvider")
    void convert2long_NumericWrappers(Object input, long expected) throws Exception {
        // Use the real Converter instance for actual conversion.
        setStaticInstance(originalInstance);
        long actual = Converter.convert2long(input);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] convert2long({0}) => {1}")
    @MethodSource("atomicNumberProvider")
    void convert2long_AtomicNumbers(Object input, long expected) throws Exception {
        setStaticInstance(originalInstance);
        long actual = Converter.convert2long(input);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] convert2long({0}) => {1}")
    @MethodSource("bigNumberProvider")
    void convert2long_BigNumbers(Object input, long expected) throws Exception {
        setStaticInstance(originalInstance);
        long actual = Converter.convert2long(input);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] convert2long({0}) => {1}")
    @MethodSource("booleanProvider")
    void convert2long_BooleanValues(Object input, long expected) throws Exception {
        setStaticInstance(originalInstance);
        long actual = Converter.convert2long(input);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] convert2long(\"{0}\") => {1}")
    @MethodSource("numericStringProvider")
    void convert2long_NumericStrings(String input, long expected) throws Exception {
        setStaticInstance(originalInstance);
        long actual = Converter.convert2long(input);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] convert2long(\"{0}\") throws NumberFormatException")
    @MethodSource("nonNumericStringProvider")
    void convert2long_NonNumericStrings_Throws(String input) throws Exception {
        setStaticInstance(originalInstance);
        assertThrows(NumberFormatException.class, () -> Converter.convert2long(input));
    }

    // ---------- Date and Calendar ----------
    @Test
    void convert2long_DateInstance() throws Exception {
        setStaticInstance(originalInstance);
        Date date = new Date(0L);
        assertEquals(0L, Converter.convert2long(date));
    }

    @Test
    void convert2long_TimestampInstance() throws Exception {
        setStaticInstance(originalInstance);
        Timestamp ts = new Timestamp(1_622_548_800_000L);
        assertEquals(1_622_548_800_000L, Converter.convert2long(ts));
    }

    @Test
    void convert2long_CalendarInstance() throws Exception {
        setStaticInstance(originalInstance);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(123_456_789L);
        assertEquals(123_456_789L, Converter.convert2long(calendar));
    }

    // ---------- Unsupported type ----------
    @Test
    void convert2long_UnsupportedType_ThrowsIllegalArgumentException() throws Exception {
        setStaticInstance(originalInstance);
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2long(unsupported));
    }

    @ParameterizedTest(name = "[{index}] convert2long({0}) throws ArithmeticException")
    @MethodSource("overflowProvider")
    void convert2long_Overflow_ThrowsArithmeticException(Object input) throws Exception {
        setStaticInstance(originalInstance);
        assertThrows(ArithmeticException.class, () -> Converter.convert2long(input));
    }
}
