package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toBigInteger_Test {

    @Mock
    private Converter // not used by the method, but required by signature
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already initializes @Mock fields, but we keep this hook
        // for possible future extensions.
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to a clean state after each test.
        // This also clears any interactions that might have been recorded.
        reset(mockConverter);
    }

    /**
     * Supplies all test scenarios extracted from the JSON description.
     * Each {@link Arguments} tuple contains:
     *   1. A short description (used as the display name of the parameterized test)
     *   2. The {@code from} argument to pass to the method
     *   3. The {@code converter} argument (may be {@code null}, a Mockito mock, or a custom impl)
     *   4. Either the expected {@link BigInteger} result or the expected {@link Class} of exception.
     */
    private static Stream<Arguments> provideToBigIntegerScenarios() {
        // Dummy Converter that does nothing – the method should ignore it.
        Converter dummyConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // never called
                return null;
            }
        };
        // Converter that throws if its convert method is ever invoked.
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        return Stream.of(// 1. true -> ONE, null converter
        Arguments.of("AtomicBoolean true, null converter", new AtomicBoolean(true), null, BigInteger.ONE), // 2. false -> ZERO, null converter
        Arguments.of("AtomicBoolean false, null converter", new AtomicBoolean(false), null, BigInteger.ZERO), // 3. null source -> NPE
        Arguments.of("null source, null converter", null, null, NullPointerException.class), // 4. non‑AtomicBoolean (Boolean) -> ClassCastException
        Arguments.of("Boolean.TRUE as source, null converter", Boolean.TRUE, null, ClassCastException.class), // 5. true with a non‑null dummy converter -> ONE
        Arguments.of("AtomicBoolean true, dummy converter", new AtomicBoolean(true), dummyConverter, BigInteger.ONE), // 6. false with a non‑null dummy converter -> ZERO
        Arguments.of("AtomicBoolean false, dummy converter", new AtomicBoolean(false), dummyConverter, BigInteger.ZERO), // 7. AtomicInteger source -> ClassCastException
        Arguments.of("AtomicInteger source, null converter", new AtomicInteger(1), null, ClassCastException.class), // 8. AtomicLong source -> ClassCastException
        Arguments.of("AtomicLong source, null converter", new AtomicLong(0L), null, ClassCastException.class), // 9. true with a throwing converter -> ONE (converter ignored)
        Arguments.of("AtomicBoolean true, throwing converter", new AtomicBoolean(true), throwingConverter, BigInteger.ONE), //10. false with a throwing converter -> ZERO (converter ignored)
        Arguments.of("AtomicBoolean false, throwing converter", new AtomicBoolean(false), throwingConverter, BigInteger.ZERO));
    }

    @Test
    void toBigInteger_whenAtomicBooleanTrue_returnsOne() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        // WHEN
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, mockConverter);
        // THEN
        assertEquals(BigInteger.ONE, result, "AtomicBoolean true should convert to BigInteger.ONE");
        // Verify that the converter was never used.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBigInteger_whenAtomicBooleanFalse_returnsZero() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(false);
        // WHEN
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, mockConverter);
        // THEN
        assertEquals(BigInteger.ZERO, result, "AtomicBoolean false should convert to BigInteger.ZERO");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBigInteger_whenNullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toBigInteger(source, mockConverter), "Passing null should result in NullPointerException");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideToBigIntegerScenarios")
    void toBigInteger_parameterized(String description, Object from, Converter converter, Object expected) {
        // The method under test does not use the Converter, but we still verify that
        // no interaction occurs when a mock is supplied.
        if (converter == mockConverter) {
            // ensure a clean state for verification
            reset(mockConverter);
        }
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            Throwable thrown = assertThrows(exClass, () -> AtomicBooleanConversions.toBigInteger(from, converter), () -> "Expected " + exClass.getSimpleName() + " for input: " + description);
            // No interaction with the mock should have happened
            if (converter == mockConverter) {
                verifyNoInteractions(mockConverter);
            }
        } else {
            // Expected a successful conversion
            BigInteger result = AtomicBooleanConversions.toBigInteger(from, converter);
            assertEquals(expected, result, () -> "Unexpected conversion result for: " + description);
            // Verify the mock (if used) was not touched
            if (converter == mockConverter) {
                verifyNoInteractions(mockConverter);
            }
        }
    }
}
