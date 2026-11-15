package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toAtomicInteger_Test {

    /**
     * The Converter instance used for all tests.
     */
    private static Converter converter;

    /**
     * The default Converter instance used for the majority of tests.
     */
    private static Converter defaultConverter;

    /**
     * Stub {@link Converter} that always returns {@code 7} for any int conversion.
     */
    private static class StubConverterReturningSeven extends Converter {

        StubConverterReturningSeven() {
            super(new ConverterOptions());
        }

        @Override
        public Integer toInt(Object from) {
            // Ignoring the actual input – the test expects a constant return value.
            return 7;
        }
    }

    /**
     * Stub {@link Converter} that throws an {@link IllegalStateException}
     * when attempting to convert to int.
     */
    private static class StubConverterThrowingException extends Converter {

        StubConverterThrowingException() {
            super(new ConverterOptions());
        }

        @Override
        public Integer toInt(Object from) {
            throw new IllegalStateException("Conversion failed");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        // If ConverterOptions has no public no‑arg constructor, replace with a mock or a suitable stub.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release resources if any (none for the current implementation).
        converter = null;
    }

    private static Stream<Arguments> validConversions() {
        return Stream.of(arguments("Direct conversion from an Integer should preserve the numeric value.", Integer.valueOf(42), 42), arguments("String containing a decimal number should be parsed correctly.", "123", 123), arguments("String with leading/trailing whitespace and a negative sign should be trimmed and parsed.", " -7 ", -7), arguments("Long values should be converted to int by truncation.", Long.valueOf(99L), 99), arguments("Double values are truncated toward zero when converting to int.", Double.valueOf(5.9), 5), arguments("Boolean true should map to integer 1.", Boolean.TRUE, 1), arguments("Boolean false should map to integer 0.", Boolean.FALSE, 0), arguments("BigDecimal is converted by taking its intValue (truncates fractional part).", new BigDecimal("10.2"), 10), arguments("BigInteger values larger than int range are reduced modulo 2^32 (standard int overflow behavior).", new BigInteger("12345678901234567890"), 1234567890), arguments("An AtomicInteger source should be unwrapped to its int value.", new AtomicInteger(55), 55), arguments("AtomicLong is converted via its long value, then truncated to int.", new AtomicLong(123L), 123), arguments("A Character representing a digit should be converted using its Unicode code point (e.g., '7' → 55).", Character.valueOf('7'), (int) '7'));
    }

    private static Stream<Arguments> nullAndInvalidInputs() {
        return Stream.of(arguments("Null source should be treated as 0 rather than throwing an exception.", null, // special handling below
        null), arguments("A non‑numeric string must cause a NumberFormatException.", "abc", NumberFormatException.class), arguments("An empty string cannot be parsed to an int and should raise NumberFormatException.", "", NumberFormatException.class));
    }

    private static Stream<Arguments> numericEdgeCases() {
        return Stream.of(arguments("Long.MAX_VALUE overflows int; expected result is (int)Long.MAX_VALUE which equals -1.", Long.MAX_VALUE, -1), arguments("Long.MIN_VALUE overflows int; expected result is (int)Long.MIN_VALUE which equals 0.", Long.MIN_VALUE, 0), arguments("Float values are truncated toward zero when converting to int.", Float.valueOf(-3.7f), -3), arguments("Byte is a subclass of Number; conversion should preserve its numeric value.", Byte.valueOf((byte) 9), 9), arguments("Short values are converted directly to int.", Short.valueOf((short) -12), -12));
    }

    private static Stream<Arguments> stringFormatEdgeCases() {
        return Stream.of(arguments("String with an explicit plus sign should be parsed as a positive integer.", "+42", 42), arguments("Leading zeros are ignored by Integer.parseInt.", "00123", 123), arguments("Scientific notation is not accepted by Integer.parseInt and should raise an exception.", "1e3", NumberFormatException.class), arguments("Comma‑separated numbers are invalid for direct integer parsing.", "1,000", NumberFormatException.class));
    }

    private static Stream<Arguments> stringParsingExceptionScenarios() {
        return Stream.of(arguments("A string that contains only whitespace should be trimmed to an empty string and cause NumberFormatException.", "   ", NumberFormatException.class), arguments("Hexadecimal notation is not accepted by Integer.parseInt; the method should throw NumberFormatException.", "0x10", NumberFormatException.class), arguments("A numeric string whose value exceeds Integer.MAX_VALUE should cause NumberFormatException due to overflow.", "2147483648", NumberFormatException.class));
    }

    private static Stream<Arguments> unsupportedTypeScenarios() {
        return Stream.of(arguments("Passing an object that is not a recognized convertible type should result in IllegalArgumentException.", new Object(), IllegalArgumentException.class), arguments("Date objects are not directly convertible to int; the method should throw IllegalArgumentException.", new java.util.Date(), IllegalArgumentException.class));
    }

    @Test
    void toAtomicInteger_fromInteger_returnsSameValue() {
        // given
        Integer source = 42;
        // when
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result, "Resulting AtomicInteger should not be null");
        assertEquals(42, result.get(), "AtomicInteger should contain the same int value as the source Integer");
    }

    @Test
    void toAtomicInteger_fromStringNumeric_returnsParsedValue() {
        // given
        String source = "123";
        // when
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(123, result.get(), "AtomicInteger should contain the int value parsed from the numeric String");
    }

    @Test
    void toAtomicInteger_fromNull_returnsZero() {
        // given
        Object source = null;
        // when
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.get(), "AtomicInteger should be zero when the source object is null");
    }

    @Test
    void toAtomicInteger_fromDouble_truncatesTowardsZero() {
        // given
        Double source = 7.9;
        // when
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        // The built‑in conversion from Double to int truncates the fractional part.
        assertEquals(7, result.get(), "AtomicInteger should contain the truncated int value of the source Double");
    }

    @Test
    void toAtomicInteger_fromBoolean_true_returnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(1, result.get(), "AtomicInteger should be 1 when source Boolean is true");
    }

    @Test
    void toAtomicInteger_fromBoolean_false_returnsZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.get(), "AtomicInteger should be 0 when source Boolean is false");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversions")
    void toAtomicInteger_validConversions(String description, Object from, int expected) {
        AtomicInteger result = StringConversions.toAtomicInteger(from, defaultConverter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndInvalidInputs")
    void toAtomicInteger_nullAndInvalidInputs(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringConversions.toAtomicInteger(from, defaultConverter), description);
    }

    @Test
    void toAtomicInteger_nullInput_returnsZero() {
        AtomicInteger result = StringConversions.toAtomicInteger(null, defaultConverter);
        assertNotNull(result);
        assertEquals(0, result.get(), "Null source should be treated as 0");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericEdgeCases")
    void toAtomicInteger_numericEdgeCases(String description, Object from, int expected) {
        AtomicInteger result = StringConversions.toAtomicInteger(from, defaultConverter);
        assertEquals(expected, result.get(), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringFormatEdgeCases")
    void toAtomicInteger_stringFormatEdgeCases(String description, Object from, Object expected) {
        if (expected instanceof Class) {
            // Expected exception
            assertThrows((Class<? extends Throwable>) expected, () -> StringConversions.toAtomicInteger(from, defaultConverter), description);
        } else {
            // Expected successful conversion
            AtomicInteger result = StringConversions.toAtomicInteger(from, defaultConverter);
            assertEquals(expected, result.get(), description);
        }
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicInteger_customConverterOverridesDefault() {
        // A stub Converter that always returns 7 for any int conversion.
        Converter stub = new StubConverterReturningSeven();
        AtomicInteger result = StringConversions.toAtomicInteger("any value", stub);
        assertNotNull(result);
        assertEquals(7, result.get(), "When a non‑null Converter is supplied, its toInt conversion (mocked to return 7) should be used.");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringParsingExceptionScenarios")
    void toAtomicInteger_stringParsingExceptions(String description, Object from, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> StringConversions.toAtomicInteger(from, defaultConverter), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedTypeScenarios")
    void toAtomicInteger_unsupportedTypes(String description, Object from, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> StringConversions.toAtomicInteger(from, defaultConverter), description);
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicInteger_converterThrowsException_propagates() {
        Converter throwingStub = new StubConverterThrowingException();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> StringConversions.toAtomicInteger("any value", throwingStub), "If a non‑null Converter throws an exception during conversion, the exception should propagate unchanged.");
        assertEquals("Conversion failed", ex.getMessage());
    }
}
