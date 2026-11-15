package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toAtomicInteger_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter converter;

    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a Converter with default options – no custom overrides are required for these tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference (helps GC in long‑running test suites).
        converter = null;
    }

    private static Stream<Arguments> conversionCases() {
        ???;
    }

    @Test
    void shouldConvertStringRepresentingNumberToAtomicInteger() {
        // given
        String source = "123";
        // when
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(123, result.get(), "AtomicInteger should contain the parsed int value");
    }

    @Test
    void shouldConvertIntegerObjectToAtomicInteger() {
        // given
        Integer source = 42;
        // when
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(42, result.get());
    }

    @Test
    void shouldConvertLongObjectToAtomicInteger() {
        // given
        // Converter can down‑cast long to int
        Long source = 7L;
        // when
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(7, result.get());
    }

    @Test
    void shouldConvertNullToAtomicIntegerWithZero() {
        // given
        Object source = null;
        // when
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.get(), "Null input should yield an AtomicInteger with value 0");
    }

    @Test
    void shouldThrowNumberFormatExceptionForNonNumericString() {
        // given
        String source = "not-a-number";
        // when / then
        assertThrows(NumberFormatException.class, () -> StringConversions.toAtomicInteger(source, converter), "Converting a non‑numeric string must raise NumberFormatException");
    }

    @Test
    void shouldThrowClassCastExceptionWhenConversionIsImpossible() {
        // given
        // No conversion path to Integer
        Object source = new Object();
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toAtomicInteger(source, converter), "Converting an unsupported type should raise ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("conversionCases")
    void shouldConvertAccordingToSpecification(Object from, Converter converter, Object expectedResultOrException, String description) {
        if (expectedResultOrException instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> expectedException = (Class<? extends Throwable>) expectedResultOrException;
            assertThrows(expectedException, () -> StringConversions.toAtomicInteger(from, converter), description);
        } else {
            AtomicInteger result = StringConversions.toAtomicInteger(from, converter);
            assertNotNull(result, description);
            assertEquals(((Number) expectedResultOrException).intValue(), result.get(), description);
        }
    }
}
