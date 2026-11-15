package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toDouble_Test {

    /**
     * A mock Converter – the method under test does not use it, but it must be non‑null.
     */
    @Mock
    Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito will initialise @Mock fields automatically.
        // Ensure the mock is reset before each test to avoid accidental interactions.
        reset(mockConverter);
    }

    /**
     * Provides a matrix of inputs covering:
     * <ul>
     *   <li>Boolean.TRUE / Boolean.FALSE</li>
     *   <li>different {@link Converter} implementations (null, dummy mock,
     *       a mock that throws, a mock that returns null)</li>
     *   <li>explicit {@code new Boolean(...)} instances</li>
     * </ul>
     *
     * The expected result is always {@code 1.0} for {@code true} and
     * {@code 0.0} for {@code false}.
     */
    static Stream<Arguments> toDoubleProvider() {
        // A dummy converter – does nothing, just exists.
        Converter dummyConverter = mock(Converter.class);
        // A converter whose any method throws a RuntimeException.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("converter error");
        });
        // A converter that returns null from any method.
        Converter nullReturningConverter = mock(Converter.class, invocation -> null);
        return Stream.of(// source,               converter,           expected, description
        Arguments.of(Boolean.TRUE, null, 1.0, "Boolean.TRUE with null converter"), Arguments.of(Boolean.FALSE, null, 0.0, "Boolean.FALSE with null converter"), Arguments.of(Boolean.TRUE, dummyConverter, 1.0, "Boolean.TRUE with dummy converter"), Arguments.of(Boolean.FALSE, dummyConverter, 0.0, "Boolean.FALSE with dummy converter"), Arguments.of(new Boolean(true), null, 1.0, "Explicit new Boolean(true) with null converter"), Arguments.of(new Boolean(false), null, 0.0, "Explicit new Boolean(false) with null converter"), Arguments.of(Boolean.TRUE, throwingConverter, 1.0, "Boolean.TRUE with throwing converter"), Arguments.of(Boolean.FALSE, throwingConverter, 0.0, "Boolean.FALSE with throwing converter"), Arguments.of(Boolean.TRUE, nullReturningConverter, 1.0, "Boolean.TRUE with null‑returning converter"), Arguments.of(Boolean.FALSE, nullReturningConverter, 0.0, "Boolean.FALSE with null‑returning converter"));
    }

    @Test
    void toDouble_whenTrue_returnsOne() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN
        Double result = BooleanConversions.toDouble(source, mockConverter);
        // THEN
        assertEquals(CommonValues.DOUBLE_ONE, result, "Conversion of Boolean.TRUE should yield Double 1.0");
    }

    @Test
    void toDouble_whenFalse_returnsZero() {
        // GIVEN
        Boolean source = Boolean.FALSE;
        // WHEN
        Double result = BooleanConversions.toDouble(source, mockConverter);
        // THEN
        assertEquals(CommonValues.DOUBLE_ZERO, result, "Conversion of Boolean.FALSE should yield Double 0.0");
    }

    @Test
    void toDouble_whenNullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toDouble(source, mockConverter), "Passing null as the source should throw NPE because of the cast to Boolean");
    }

    @Test
    void toDouble_whenNonBooleanSource_throwsClassCastException() {
        // GIVEN
        Object source = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toDouble(source, mockConverter), "Passing a non‑Boolean object should result in a ClassCastException");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("toDoubleProvider")
    void toDouble_variousValidInputs(Object source, Converter converter, double expected, String _displayName) {
        // WHEN
        Double result = BooleanConversions.toDouble(source, converter);
        // THEN
        assertEquals(expected, result, () -> "Expected conversion result " + expected + " but got " + result);
    }
}
