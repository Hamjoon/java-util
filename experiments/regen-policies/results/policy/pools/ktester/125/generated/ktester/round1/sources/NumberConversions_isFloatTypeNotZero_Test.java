package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_isFloatTypeNotZero_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter converter;

    /**
     * Shared real converter for the majority of test cases.
     */
    private static Converter realConverter;

    /**
     * Converter that always returns {@code 0.0} for any {@code toDouble} call.
     */
    private static class MockConverterReturningZero extends Converter {

        MockConverterReturningZero() {
            super(new ConverterOptions());
        }

        @Override
        public double toDouble(Object from) {
            return 0.0;
        }
    }

    /**
     * Converter that always returns a predefined double value.
     */
    private static class MockConverterReturningDouble extends Converter {

        private final double value;

        MockConverterReturningDouble(double value) {
            super(new ConverterOptions());
            this.value = value;
        }

        @Override
        public double toDouble(Object from) {
            return value;
        }
    }

    /**
     * Converter that throws a checked exception during conversion.
     */
    private static class MockConverterThrowing extends Converter {

        private final Exception toThrow;

        MockConverterThrowing(Exception toThrow) {
            super(new ConverterOptions());
            this.toThrow = toThrow;
        }

        @Override
        public double toDouble(Object from) {
            if (toThrow instanceof RuntimeException) {
                throw (RuntimeException) toThrow;
            } else {
                // wrap checked exceptions in a RuntimeException because the signature
                // does not allow throwing checked ones.
                throw new RuntimeException(toThrow);
            }
        }
    }

    /**
     * Converter that returns {@code null} for {@code toDouble}.
     */
    private static class MockConverterReturningNull extends Converter {

        MockConverterReturningNull() {
            super(new ConverterOptions());
        }

        @Override
        public Double toDouble(Object from) {
            // will cause NPE when unboxed
            return null;
        }
    }

    /**
     * Converter that throws an unchecked exception (not a NumberFormatException).
     */
    private static class MockConverterThrowingRuntime extends Converter {

        private final RuntimeException toThrow;

        MockConverterThrowingRuntime(RuntimeException toThrow) {
            super(new ConverterOptions());
            this.toThrow = toThrow;
        }

        @Override
        public double toDouble(Object from) {
            throw toThrow;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides sensible defaults.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // The library’s default options are sufficient for the standard conversion tests.
        ConverterOptions options = new ConverterOptions();
        realConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
        realConverter = null;
    }

    private static Stream<Arguments> zeroValueProvider() {
        return Stream.of(Arguments.of(0.0d, realConverter), Arguments.of(0.0f, realConverter), Arguments.of(0, realConverter), Arguments.of("0", realConverter), Arguments.of(BigDecimal.ZERO, realConverter), Arguments.of(null, realConverter), // negative zero (double) – should be treated as zero
        Arguments.of(-0.0d, realConverter));
    }

    private static Stream<Arguments> nonZeroValueProvider() {
        return Stream.of(Arguments.of(5.5d, realConverter), Arguments.of(-3.14f, realConverter), Arguments.of(42, realConverter), Arguments.of("-7.89", realConverter), Arguments.of(BigDecimal.valueOf(123.456), realConverter), // special floating values that are not zero
        Arguments.of(Double.NaN, realConverter), Arguments.of(Double.POSITIVE_INFINITY, realConverter), Arguments.of(Float.NEGATIVE_INFINITY, realConverter));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of(new Object(), realConverter, NumberFormatException.class), Arguments.of("not a number", realConverter, NumberFormatException.class), Arguments.of(true, realConverter, NumberFormatException.class), Arguments.of(false, realConverter, NumberFormatException.class), Arguments.of('A', realConverter, NumberFormatException.class));
    }

    private static Stream<Arguments> atomicAndBigIntegerProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(0), realConverter, false), Arguments.of(new AtomicInteger(7), realConverter, true), Arguments.of(new AtomicLong(0L), realConverter, false), Arguments.of(new AtomicLong(12345L), realConverter, true), Arguments.of(BigInteger.ZERO, realConverter, false), Arguments.of(BigInteger.valueOf(999999999999L), realConverter, true));
    }

    private static Stream<Arguments> stringEdgeProvider() {
        return Stream.of(Arguments.of("  0.0  ", realConverter, false), Arguments.of("+3.1415", realConverter, true), Arguments.of("-0.0", realConverter, false));
    }

    private static Stream<Arguments> emptyStringProvider() {
        return Stream.of(Arguments.of("", realConverter, NumberFormatException.class), Arguments.of("   ", realConverter, NumberFormatException.class));
    }

    private static Stream<Arguments> temporalProvider() {
        return Stream.of(Arguments.of(LocalDate.now(), realConverter, IllegalArgumentException.class), Arguments.of(ZonedDateTime.now(), realConverter, IllegalArgumentException.class));
    }

    private static Stream<Arguments> customConverterProvider() {
        return Stream.of(// always returns 0.0 → false
        Arguments.of("any", new MockConverterReturningZero(), false), // always returns 2.718 → true
        Arguments.of("ignored", new MockConverterReturningDouble(2.718), true), // throws NumberFormatException → exception propagates
        Arguments.of(null, new MockConverterThrowing(new NumberFormatException("forced")), NumberFormatException.class), // returns null → NPE on unboxing
        Arguments.of("any", new MockConverterReturningNull(), NullPointerException.class), // throws unchecked IllegalStateException → propagates unchanged
        Arguments.of("ignored", new MockConverterThrowingRuntime(new IllegalStateException("forced")), IllegalStateException.class));
    }

    @Test
    @DisplayName("Non‑zero Double value returns true")
    void nonZeroDouble() {
        Double src = 3.14;
        assertTrue(NumberConversions.isFloatTypeNotZero(src, converter));
    }

    @Test
    @DisplayName("Non‑zero String value returns true")
    void nonZeroString() {
        String src = "2.5";
        assertTrue(NumberConversions.isFloatTypeNotZero(src, converter));
    }

    @Test
    @DisplayName("Non‑zero Integer value returns true")
    void nonZeroInteger() {
        Integer src = 7;
        assertTrue(NumberConversions.isFloatTypeNotZero(src, converter));
    }

    @Test
    @DisplayName("Non‑zero BigDecimal value returns true")
    void nonZeroBigDecimal() {
        BigDecimal src = new BigDecimal("1.23");
        assertTrue(NumberConversions.isFloatTypeNotZero(src, converter));
    }

    @Test
    @DisplayName("Zero Double value returns false")
    void zeroDouble() {
        Double src = 0.0;
        assertFalse(NumberConversions.isFloatTypeNotZero(src, converter));
    }

    @Test
    @DisplayName("Zero String value returns false")
    void zeroString() {
        String src = "0";
        assertFalse(NumberConversions.isFloatTypeNotZero(src, converter));
    }

    @Test
    @DisplayName("Zero Integer value returns false")
    void zeroInteger() {
        Integer src = 0;
        assertFalse(NumberConversions.isFloatTypeNotZero(src, converter));
    }

    @Test
    @DisplayName("Zero BigDecimal value returns false")
    void zeroBigDecimal() {
        BigDecimal src = BigDecimal.ZERO;
        assertFalse(NumberConversions.isFloatTypeNotZero(src, converter));
    }

    @Test
    @DisplayName("Null source returns false (converter treats null as 0)")
    void nullSource() {
        assertFalse(NumberConversions.isFloatTypeNotZero(null, converter));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("zeroValueProvider")
    void zeroValues(Object from, Converter converter) {
        assertFalse(NumberConversions.isFloatTypeNotZero(from, converter), () -> "Zero value '" + from + "' must yield false");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("nonZeroValueProvider")
    void nonZeroValues(Object from, Converter converter) {
        assertTrue(NumberConversions.isFloatTypeNotZero(from, converter), () -> "Non‑zero value '" + from + "' must yield true");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("invalidInputProvider")
    void invalidInputs(Object from, Converter converter, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> NumberConversions.isFloatTypeNotZero(from, converter));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("atomicAndBigIntegerProvider")
    void atomicAndBigIntegerValues(Object from, Converter converter, boolean expected) {
        if (expected) {
            assertTrue(NumberConversions.isFloatTypeNotZero(from, converter));
        } else {
            assertFalse(NumberConversions.isFloatTypeNotZero(from, converter));
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("stringEdgeProvider")
    void stringEdgeCases(String from, Converter converter, boolean expected) {
        if (expected) {
            assertTrue(NumberConversions.isFloatTypeNotZero(from, converter));
        } else {
            assertFalse(NumberConversions.isFloatTypeNotZero(from, converter));
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=\"{0}\"")
    @MethodSource("emptyStringProvider")
    void emptyAndWhitespaceStrings(String from, Converter converter, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> NumberConversions.isFloatTypeNotZero(from, converter));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("temporalProvider")
    void unsupportedTemporalTypes(Object from, Converter converter, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> NumberConversions.isFloatTypeNotZero(from, converter));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] customConverter={1}")
    @MethodSource("customConverterProvider")
    void customConverterInfluence(Object from, Converter customConverter, Object expectedResultOrException) {
        if (expectedResultOrException instanceof Boolean) {
            boolean expected = (Boolean) expectedResultOrException;
            boolean actual = NumberConversions.isFloatTypeNotZero(from, customConverter);
            assertEquals(expected, actual, () -> "With custom converter, expected " + expected + " but got " + actual);
        } else if (expectedResultOrException instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> expectedEx = (Class<? extends Throwable>) expectedResultOrException;
            assertThrows(expectedEx, () -> NumberConversions.isFloatTypeNotZero(from, customConverter));
        }
    }
}
