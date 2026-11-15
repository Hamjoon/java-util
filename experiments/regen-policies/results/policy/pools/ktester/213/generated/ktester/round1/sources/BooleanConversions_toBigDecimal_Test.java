package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toBigDecimal_Test {

    /**
     * The Converter is not used by the method under test, but we provide a mock to satisfy the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A simple stub implementation of {@link Converter}.  It is not used by the
     * method under test, but having a concrete class demonstrates that any
     * non‑null {@code Converter} instance is ignored.
     */
    static class DummyConverter implements Converter {
        // No methods are overridden because {@code BooleanConversions.toBigDecimal}
        // never calls any method on the converter.
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already initialises @Mock fields.
        // Ensure the mock does not interfere with the method – no stubbing required.
        reset(mockConverter);
    }

    /**
     * Provides a stream of arguments:
     * <ul>
     *   <li>input Boolean (true / false)</li>
     *   <li>Converter instance (null, mock, dummy, throwing mock)</li>
     *   <li>expected result (BigDecimal.ONE / ZERO)</li>
     *   <li>whether reference equality should be asserted</li>
     * </ul>
     */
    private static Stream<Arguments> toBigDecimalProvider() {
        // a regular mock – behaves like the @Mock field but created locally
        Converter dummyConverter = mock(Converter.class);
        // a mock that throws on any interaction – ensures the method truly does not use it
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        });
        return Stream.of(// true / false with null converter (original behaviour)
        arguments(Boolean.TRUE, null, BigDecimal.ONE, false), arguments(Boolean.FALSE, null, BigDecimal.ZERO, false), // true / false with a non‑null dummy converter
        arguments(Boolean.TRUE, dummyConverter, BigDecimal.ONE, false), arguments(Boolean.FALSE, dummyConverter, BigDecimal.ZERO, false), // true / false with a throwing converter – no exception should be thrown
        arguments(Boolean.TRUE, throwingConverter, BigDecimal.ONE, false), arguments(Boolean.FALSE, throwingConverter, BigDecimal.ZERO, false), // true / false using Boolean.valueOf and new Boolean constructors
        arguments(Boolean.valueOf(true), null, BigDecimal.ONE, false), arguments(new Boolean(false), null, BigDecimal.ZERO, false), // reference‑equality checks for the constant instances
        arguments(Boolean.TRUE, null, BigDecimal.ONE, true), arguments(Boolean.FALSE, null, BigDecimal.ZERO, true));
    }

    @Test
    void toBigDecimal_true_returnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        BigDecimal result = BooleanConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(BigDecimal.ONE, result, "True Boolean should convert to BigDecimal.ONE");
    }

    @Test
    void toBigDecimal_false_returnsZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        BigDecimal result = BooleanConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(BigDecimal.ZERO, result, "False Boolean should convert to BigDecimal.ZERO");
    }

    @Test
    void toBigDecimal_null_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toBigDecimal(source, mockConverter), "Passing null should result in NullPointerException due to unboxing");
    }

    @Test
    void toBigDecimal_nonBoolean_throwsClassCastException() {
        // given
        Object source = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toBigDecimal(source, mockConverter), "Passing a non‑Boolean object should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("toBigDecimalProvider")
    void toBigDecimal_variousInputs(Object from, Converter converter, BigDecimal expected, boolean checkSameInstance) {
        // when
        BigDecimal result = BooleanConversions.toBigDecimal(from, converter);
        // then – value equality is always required
        assertEquals(expected, result, () -> "Conversion of " + from + " should yield " + expected);
        // optionally also verify that the exact constant instance is returned
        if (checkSameInstance) {
            assertSame(expected, result, () -> "Result should be the same constant instance for input " + from);
        }
    }
}
