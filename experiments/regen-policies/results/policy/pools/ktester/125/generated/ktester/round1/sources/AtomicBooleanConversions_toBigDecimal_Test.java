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
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AtomicBooleanConversions_toBigDecimal_Test {

    /**
     * Mocked Converter – the method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter converter;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for the @Mock fields.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the mock context to avoid memory leaks.
        mocks.close();
    }

    /**
     * Supplies all test scenarios for {@link AtomicBooleanConversions#toBigDecimal(Object, Converter)}.
     *
     * @return a stream of arguments: source object, converter (may be null), expected outcome,
     *         and a human‑readable description.
     */
    private static Stream<Arguments> provideToBigDecimalCases() {
        // A real (non‑null) Converter mock – the implementation ignores it.
        Converter realConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // simple pass‑through stub
                return src;
            }
        };
        // Subclass that throws from get()
        AtomicBoolean throwingAtomic = new AtomicBoolean(true) {

            @Override
            public boolean get() {
                throw new RuntimeException("boom");
            }
        };
        return Stream.of(// true → ONE (null converter)
        arguments(new AtomicBoolean(true), null, BigDecimal.ONE, "Converts an AtomicBoolean with value true to BigDecimal.ONE."), // false → ZERO (null converter)
        arguments(new AtomicBoolean(false), null, BigDecimal.ZERO, "Converts an AtomicBoolean with value false to BigDecimal.ZERO."), // true → ONE (non‑null converter)
        arguments(new AtomicBoolean(true), realConverter, BigDecimal.ONE, "Converts an AtomicBoolean true to BigDecimal.ONE when a non‑null Converter is supplied."), // false → ZERO (non‑null converter)
        arguments(new AtomicBoolean(false), realConverter, BigDecimal.ZERO, "Converts an AtomicBoolean false to BigDecimal.ZERO when a non‑null Converter is supplied."), // null source → NullPointerException
        arguments(null, null, NullPointerException.class, "Passing null as the source object should result in NullPointerException."), // non‑AtomicBoolean source → ClassCastException
        arguments("not an AtomicBoolean", null, ClassCastException.class, "Providing a non‑AtomicBoolean object should cause ClassCastException."), // subclass that throws RuntimeException → RuntimeException
        arguments(throwingAtomic, null, RuntimeException.class, "A subclass of AtomicBoolean that throws from get() should propagate RuntimeException."));
    }

    @Test
    void toBigDecimal_trueAtomicBoolean_returnsOne() {
        // given
        AtomicBoolean atomicTrue = new AtomicBoolean(true);
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(atomicTrue, converter);
        // then
        assertEquals(BigDecimal.ONE, result, "AtomicBoolean true should convert to BigDecimal.ONE");
    }

    @Test
    void toBigDecimal_falseAtomicBoolean_returnsZero() {
        // given
        AtomicBoolean atomicFalse = new AtomicBoolean(false);
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(atomicFalse, converter);
        // then
        assertEquals(BigDecimal.ZERO, result, "AtomicBoolean false should convert to BigDecimal.ZERO");
    }

    @Test
    void toBigDecimal_nonAtomicBoolean_throwsClassCastException() {
        // given
        String notAtomic = "not an AtomicBoolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBigDecimal(notAtomic, converter), "Passing a non‑AtomicBoolean should result in ClassCastException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideToBigDecimalCases")
    void toBigDecimal_variousInputs(Object from, Converter suppliedConverter, Object expected, String description) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception – verify that the exact type is thrown.
            assertThrows((Class<? extends Throwable>) expected, () -> AtomicBooleanConversions.toBigDecimal(from, suppliedConverter), description);
        } else {
            // Expected a successful conversion – verify the returned value.
            BigDecimal result = AtomicBooleanConversions.toBigDecimal(from, suppliedConverter);
            assertEquals(expected, result, description);
        }
    }
}
