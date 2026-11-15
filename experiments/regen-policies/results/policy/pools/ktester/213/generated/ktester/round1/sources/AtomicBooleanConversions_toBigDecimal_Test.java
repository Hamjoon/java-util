package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AtomicBooleanConversions_toBigDecimal_Test {

    /**
     * Mocked Converter – the method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    private AutoCloseable mocks;

    private AtomicBoolean trueAtomic;

    private AtomicBoolean falseAtomic;

    /**
     * Minimal stub implementation of {@link Converter}.  The real {@code Converter}
     * interface/class is not required for these tests because {@code toBigDecimal}
     * never invokes any of its methods.
     */
    private static class DummyConverter implements Converter {
        // No implementation needed – the conversion method does not use it.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations.
        mocks = MockitoAnnotations.openMocks(this);
        // Prepare the AtomicBoolean instances used in the tests.
        trueAtomic = new AtomicBoolean(true);
        falseAtomic = new AtomicBoolean(false);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        if (mocks != null) {
            mocks.close();
        }
        // Clear references to help GC (optional but tidy).
        trueAtomic = null;
        falseAtomic = null;
        mockConverter = null;
    }

    /**
     * Supplies the arguments for {@link #toBigDecimal_variousScenarios(Object, Converter, Object)}.
     *
     * The list mirrors the JSON test cases, including both {@code null} and non‑null
     * {@link Converter} arguments.
     *
     * @return a stream of {@link Arguments}
     */
    static Stream<Arguments> provideToBigDecimalCases() {
        // Re‑create the atomic booleans for the provider (they are immutable for the test duration).
        AtomicBoolean trueAtomic = new AtomicBoolean(true);
        AtomicBoolean falseAtomic = new AtomicBoolean(false);
        // A dummy converter implementation – the method under test ignores it.
        Converter dummyConverter = new DummyConverter();
        return Stream.of(// true atomic, null converter → ONE
        Arguments.of(trueAtomic, null, BigDecimal.ONE), // false atomic, null converter → ZERO
        Arguments.of(falseAtomic, null, BigDecimal.ZERO), // null source, null converter → NullPointerException (b.get() on null)
        Arguments.of(null, null, NullPointerException.class), // non‑AtomicBoolean source, null converter → ClassCastException
        Arguments.of("not an atomic boolean", null, ClassCastException.class), // true atomic, non‑null converter → ONE (converter ignored)
        Arguments.of(trueAtomic, dummyConverter, BigDecimal.ONE), // false atomic, non‑null converter → ZERO (converter ignored)
        Arguments.of(falseAtomic, dummyConverter, BigDecimal.ZERO));
    }

    @Test
    void toBigDecimal_whenAtomicBooleanTrue_returnsOne() {
        // given
        Object source = trueAtomic;
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(BigDecimal.ONE, result, "AtomicBoolean true should convert to BigDecimal.ONE");
    }

    @Test
    void toBigDecimal_whenAtomicBooleanFalse_returnsZero() {
        // given
        Object source = falseAtomic;
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(BigDecimal.ZERO, result, "AtomicBoolean false should convert to BigDecimal.ZERO");
    }

    @Test
    void toBigDecimal_whenSourceIsNull_throwsClassCastException() {
        // given
        Object source = null;
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBigDecimal(source, mockConverter), "Passing null should result in a ClassCastException due to the cast inside the method");
    }

    /**
     * Parameterised test that exercises every scenario described in the JSON test case list.
     *
     * @param from      the source object passed to {@code toBigDecimal}
     * @param converter the {@link Converter} instance passed to {@code toBigDecimal}
     * @param expected  either a {@link BigDecimal} result or an exception {@link Class}
     */
    @ParameterizedTest(name = "{index}: from={0}, converter={1} => expected={2}")
    @MethodSource("provideToBigDecimalCases")
    void toBigDecimal_variousScenarios(Object from, Converter converter, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception – verify that it is thrown.
            assertThrows((Class<? extends Throwable>) expected, () -> AtomicBooleanConversions.toBigDecimal(from, converter), () -> "Expected exception " + ((Class<?>) expected).getSimpleName());
        } else {
            // Expected a successful conversion – verify the exact value.
            BigDecimal result = AtomicBooleanConversions.toBigDecimal(from, converter);
            assertEquals(expected, result, () -> "Conversion result does not match expected value");
        }
    }
}
