package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// allows non‑static @MethodSource methods
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BooleanConversions_toBigDecimal_Test {

    private Converter converter;

    /**
     * A dummy (non‑null) {@code Converter} – behaviour is irrelevant for the method under test.
     */
    private Converter dummyConverter;

    /**
     * A {@code Converter} that throws if any of its methods are invoked.
     */
    private Converter throwingConverter;

    @BeforeEach
    void setupBeforeEach(@Mock ConverterOptions mockOptions) {
        // The Converter constructor only needs the overrides map – an empty map is sufficient.
        when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        // Real converter – used as the “dummy” implementation.
        converter = new Converter(mockOptions);
        // same instance, behaviour is irrelevant.
        dummyConverter = converter;
        // A mock that throws on any interaction – verifies that BooleanConversions never touches it.
        throwingConverter = org.mockito.Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
    }

    @AfterEach
    void teardownAfterEach() {
        converter = null;
        dummyConverter = null;
        throwingConverter = null;
    }

    private Stream<Arguments> validConversionProvider() {
        return Stream.of(// true / false with a null converter
        Arguments.of(BigDecimal.ONE, ConvKind.NULL, Boolean.TRUE, "TRUE with null converter → ONE"), Arguments.of(BigDecimal.ZERO, ConvKind.NULL, Boolean.FALSE, "FALSE with null converter → ZERO"), // true / false with a regular (dummy) converter
        Arguments.of(BigDecimal.ONE, ConvKind.DUMMY, Boolean.TRUE, "TRUE with dummy converter → ONE"), Arguments.of(BigDecimal.ZERO, ConvKind.DUMMY, Boolean.FALSE, "FALSE with dummy converter → ZERO"), // true / false with a throwing converter – should still succeed and never invoke the converter
        Arguments.of(BigDecimal.ONE, ConvKind.THROWING, Boolean.TRUE, "TRUE with throwing converter → ONE"), Arguments.of(BigDecimal.ZERO, ConvKind.THROWING, Boolean.FALSE, "FALSE with throwing converter → ZERO"));
    }

    private Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source – always results in NPE regardless of converter
        Arguments.of(NullPointerException.class, ConvKind.NULL, null, "null source with null converter → NPE"), Arguments.of(NullPointerException.class, ConvKind.DUMMY, null, "null source with dummy converter → NPE"), Arguments.of(NullPointerException.class, ConvKind.THROWING, null, "null source with throwing converter → NPE"), // Wrong type (Integer) – cast to Boolean fails, yielding ClassCastException
        Arguments.of(ClassCastException.class, ConvKind.NULL, Integer.valueOf(1), "Integer source with null converter → CCE"), Arguments.of(ClassCastException.class, ConvKind.DUMMY, Integer.valueOf(42), "Integer source with dummy converter → CCE"));
    }

    @Test
    void toBigDecimal_trueReturnsOne() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN
        BigDecimal result = BooleanConversions.toBigDecimal(source, converter);
        // THEN
        assertEquals(BigDecimal.ONE, result, "Boolean TRUE should convert to BigDecimal.ONE");
    }

    @Test
    void toBigDecimal_falseReturnsZero() {
        // GIVEN
        Boolean source = Boolean.FALSE;
        // WHEN
        BigDecimal result = BooleanConversions.toBigDecimal(source, converter);
        // THEN
        assertEquals(BigDecimal.ZERO, result, "Boolean FALSE should convert to BigDecimal.ZERO");
    }

    @Test
    void toBigDecimal_nullThrowsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toBigDecimal(source, converter), "Passing null should result in a NullPointerException");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("validConversionProvider")
    void toBigDecimal_returnsExpected(BigDecimal expected, ConvKind convKind, Object from, String description) {
        // Resolve the actual Converter instance based on the enum value.
        Converter conv = switch(convKind) {
            case NULL ->
                null;
            case DUMMY ->
                dummyConverter;
            case THROWING ->
                throwingConverter;
        };
        // Execute the method under test.
        BigDecimal result = BooleanConversions.toBigDecimal(from, conv);
        // Verify the expected value.
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("exceptionProvider")
    void toBigDecimal_throwsExpectedException(Class<? extends Throwable> expectedException, ConvKind convKind, Object from, String description) {
        Converter conv = switch(convKind) {
            case NULL ->
                null;
            case DUMMY ->
                dummyConverter;
            case THROWING ->
                throwingConverter;
        };
        assertThrows(expectedException, () -> BooleanConversions.toBigDecimal(from, conv), description);
    }
}
