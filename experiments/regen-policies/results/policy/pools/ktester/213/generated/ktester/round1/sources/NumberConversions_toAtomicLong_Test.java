package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class NumberConversions_toAtomicLong_Test {

    /**
     * Converter used for all tests – created once per test method.
     */
    private Converter converter;

    static class MyPojo {

        final long value;

        MyPojo(long value) {
            this.value = value;
        }
    }

    static class TestCase {

        final Object from;

        final Supplier<Converter> converterSupplier;

        final Long expectedValue;

        final Class<? extends Throwable> expectedException;

        final String description;

        TestCase(Object from, Supplier<Converter> converterSupplier, Long expectedValue, Class<? extends Throwable> expectedException, String description) {
            this.from = from;
            this.converterSupplier = converterSupplier;
            this.expectedValue = expectedValue;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a Converter with default options.  The default options are sufficient for the
        // built‑in conversions exercised by NumberConversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – helps the GC when many tests run.
        converter = null;
    }

    Stream<TestCase> testCases() {
        ???;
    }

    private static Calendar createCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    @Test
    void shouldConvertLongPrimitive() {
        AtomicLong result = NumberConversions.toAtomicLong(123L, converter);
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    void shouldConvertIntegerNumber() {
        AtomicLong result = NumberConversions.toAtomicLong(42, converter);
        assertEquals(42L, result.get());
    }

    @Test
    void shouldConvertStringRepresentingNumber() {
        AtomicLong result = NumberConversions.toAtomicLong("9876543210", converter);
        assertEquals(9_876_543_210L, result.get());
    }

    @Test
    void shouldConvertBigInteger() {
        AtomicLong result = NumberConversions.toAtomicLong(new java.math.BigInteger("1234567890123456789"), converter);
        assertEquals(1_234_567_890_123_456_789L, result.get());
    }

    @Test
    void shouldConvertBigDecimal() {
        AtomicLong result = NumberConversions.toAtomicLong(new java.math.BigDecimal("12345.67"), converter);
        // Conversion truncates the fractional part (standard long conversion semantics)
        assertEquals(12_345L, result.get());
    }

    @Test
    void shouldConvertAtomicLongDirectly() {
        AtomicLong source = new AtomicLong(555L);
        AtomicLong result = NumberConversions.toAtomicLong(source, converter);
        assertEquals(555L, result.get());
    }

    @Test
    void shouldConvertDateToEpochMillis() {
        // a fixed epoch millis
        java.util.Date date = new java.util.Date(1_600_000_000_000L);
        AtomicLong result = NumberConversions.toAtomicLong(date, converter);
        assertEquals(1_600_000_000_000L, result.get());
    }

    @Test
    void shouldConvertNullToZero() {
        // According to Converter contract, null for a numeric target yields 0.
        AtomicLong result = NumberConversions.toAtomicLong(null, converter);
        assertEquals(0L, result.get());
    }

    @Test
    void shouldThrowWhenConversionIsImpossible() {
        // no built‑in conversion to long
        Object unsupported = new Object();
        assertThrows(ConversionException.class, () -> NumberConversions.toAtomicLong(unsupported, converter));
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("testCases")
    void shouldConvertAccordingToSpecification(TestCase tc) {
        Converter conv = (tc.converterSupplier != null) ? tc.converterSupplier.get() : converter;
        if (tc.expectedException != null) {
            assertThrows(tc.expectedException, () -> NumberConversions.toAtomicLong(tc.from, conv), tc.description);
        } else {
            AtomicLong result = NumberConversions.toAtomicLong(tc.from, conv);
            assertNotNull(result, tc.description);
            assertEquals(tc.expectedValue.longValue(), result.get(), tc.description);
        }
    }
}
