package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDoubleZero_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter sharedConverter;

    /**
     * Minimal Converter that does nothing – used for the majority of cases.
     */
    private static final class DummyConverter extends Converter {

        static final DummyConverter INSTANCE = new DummyConverter(new ConverterOptions());

        private DummyConverter(ConverterOptions options) {
            super(options);
        }
    }

    /**
     * Converter that throws on any conversion attempt.
     * The test ensures that {@code toDoubleZero} never calls the converter,
     * so no exception should be observed.
     */
    private static final class ThrowingConverter extends Converter {

        static final ThrowingConverter INSTANCE = new ThrowingConverter(new ConverterOptions());

        private ThrowingConverter(ConverterOptions options) {
            super(options);
        }

        @Override
        public Object convert(Object from, Class<?> toClass) {
            throw new RuntimeException("Converter should not be invoked");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        // The actual conversion logic is irrelevant for this test because the method
        // does not use the converter, but we still provide a valid instance.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared converter reference.
        sharedConverter = null;
    }

    /**
     * Provides a stream of arguments for the parameterized test.
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>the {@code from} object (may be {@code null})</li>
     *   <li>the {@code Converter} instance to pass</li>
     * </ul>
     */
    private static Stream<Arguments> provideAllInputs() {
        return Stream.of(// ----- basic behavior -------------------------------------------------
        Arguments.of(null, DummyConverter.INSTANCE), Arguments.of("any string", DummyConverter.INSTANCE), Arguments.of(Integer.valueOf(123), DummyConverter.INSTANCE), Arguments.of(Double.valueOf(123.456), DummyConverter.INSTANCE), Arguments.of(new BigDecimal("99.99"), DummyConverter.INSTANCE), Arguments.of(new Date(), DummyConverter.INSTANCE), Arguments.of(new AtomicLong(42L), DummyConverter.INSTANCE), Arguments.of(new Object(), null), // ----- diverse non‑numeric inputs ------------------------------------
        Arguments.of(Boolean.TRUE, DummyConverter.INSTANCE), Arguments.of('c', DummyConverter.INSTANCE), Arguments.of(Byte.valueOf((byte) 5), DummyConverter.INSTANCE), Arguments.of(Short.valueOf((short) 10), DummyConverter.INSTANCE), Arguments.of(Long.valueOf(999L), DummyConverter.INSTANCE), Arguments.of(Float.valueOf(3.14f), DummyConverter.INSTANCE), Arguments.of(new BigInteger("12345678901234567890"), DummyConverter.INSTANCE), Arguments.of(Calendar.getInstance(), DummyConverter.INSTANCE), Arguments.of(Instant.now(), DummyConverter.INSTANCE), Arguments.of(new AtomicBoolean(true), DummyConverter.INSTANCE), // ----- converter side‑effects ignored ---------------------------------
        Arguments.of("any string", ThrowingConverter.INSTANCE), Arguments.of(new BigDecimal("123.45"), ThrowingConverter.INSTANCE));
    }

    @Test
    void toDoubleZero_returnsConstantZero_ForNullInput() {
        // given
        // any value is acceptable; null is a natural edge case
        Object input = null;
        // when
        Double result = NumberConversions.toDoubleZero(input, sharedConverter);
        // then
        assertNotNull(result, "Result should never be null");
        assertSame(CommonValues.DOUBLE_ZERO, result, "Method must return the exact constant defined in CommonValues");
        assertEquals(0.0, result.doubleValue(), "The constant should represent the numeric value zero");
    }

    @Test
    void toDoubleZero_returnsConstantZero_ForNonNullInput() {
        // given
        // arbitrary non‑null object
        Object input = "123.45";
        // when
        Double result = NumberConversions.toDoubleZero(input, sharedConverter);
        // then
        assertSame(CommonValues.DOUBLE_ZERO, result, "Even with a non‑null argument the method must return the constant zero");
    }

    /**
     * Verifies that {@code NumberConversions.toDoubleZero} always returns the
     * exact {@link CommonValues#DOUBLE_ZERO} constant, irrespective of the
     * supplied arguments.
     *
     * @param from      the source object (may be {@code null})
     * @param converter the converter instance (may be {@code null})
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("provideAllInputs")
    void toDoubleZero_alwaysReturnsConstantZero(Object from, Converter converter) {
        // When
        Double result = NumberConversions.toDoubleZero(from, converter);
        // Then
        assertNotNull(result, "Result must never be null");
        assertSame(CommonValues.DOUBLE_ZERO, result, "Method must return the exact constant defined in CommonValues");
        assertEquals(0.0, result.doubleValue(), "The constant should represent the numeric value zero");
    }

    @Test
    void toDoubleZero_returnsConstantZero_ForNullInput_Explicit() {
        // given
        Object input = null;
        // when
        Double result = NumberConversions.toDoubleZero(input, sharedConverter);
        // then
        assertNotNull(result);
        assertSame(CommonValues.DOUBLE_ZERO, result);
        assertEquals(0.0, result);
    }

    @Test
    void toDoubleZero_returnsConstantZero_ForNonNullInput_Explicit() {
        // given
        Object input = "123.45";
        // when
        Double result = NumberConversions.toDoubleZero(input, sharedConverter);
        // then
        assertSame(CommonValues.DOUBLE_ZERO, result);
    }
}
