package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;

class BooleanConversions_toDouble_Test {

    // shared for all tests
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The BooleanConversions.toDouble method does not actually use the Converter
        // instance, but we create a real one to satisfy the method signature.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    /**
     * Provides arguments for the conversion tests where the expected result is a primitive double.
     */
    private static Stream<Arguments> conversionValuesProvider() {
        return Stream.of(// group: Conversion of Boolean TRUE
        Arguments.of(Boolean.TRUE, null, 1.0, "Boolean.TRUE with null converter"), // group: Conversion of Boolean FALSE
        Arguments.of(Boolean.FALSE, null, 0.0, "Boolean.FALSE with null converter"), // group: Converter argument ignored (non‑null real converter)
        Arguments.of(Boolean.TRUE, new Converter(new ConverterOptions()), 1.0, "Boolean.TRUE with real converter (ignored)"), // group: Conversion of Boolean FALSE with non‑null converter
        Arguments.of(Boolean.FALSE, new Converter(new ConverterOptions()), 0.0, "Boolean.FALSE with real converter (ignored)"), // group: Converter argument ignored even if it throws
        Arguments.of(Boolean.TRUE, throwingConverterMock(), 1.0, "Boolean.TRUE with throwing mock converter (ignored)"));
    }

    /**
     * A Mockito mock of {@link Converter} that throws a {@link RuntimeException}
     * for **any** method invocation. The method under test never calls the
     * converter, so the exception should never be observed.
     */
    private static Converter throwingConverterMock() {
        // Using the generic Answer that throws for any method call.
        return mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        });
    }

    @Test
    void toDouble_returnsOneWhenTrue() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN
        Double result = BooleanConversions.toDouble(source, converter);
        // THEN
        assertEquals(CommonValues.DOUBLE_ONE, result, "Conversion of Boolean.TRUE should yield Double 1.0");
    }

    @Test
    void toDouble_returnsZeroWhenFalse() {
        // GIVEN
        Boolean source = Boolean.FALSE;
        // WHEN
        Double result = BooleanConversions.toDouble(source, converter);
        // THEN
        assertEquals(CommonValues.DOUBLE_ZERO, result, "Conversion of Boolean.FALSE should yield Double 0.0");
    }

    @Test
    void toDouble_throwsNPEWhenNull() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toDouble(source, converter), "Passing null should result in NullPointerException because of the cast to Boolean");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("conversionValuesProvider")
    void toDouble_ignoresConverter(Object from, Converter conv, double expectedPrimitive, String description) {
        // WHEN
        Double result = BooleanConversions.toDouble(from, conv);
        // THEN
        assertEquals(expectedPrimitive, result.doubleValue(), "Expected conversion result to match primitive " + expectedPrimitive);
    }

    @Test
    void toDouble_returnsExactConstantInstanceForTrue() {
        Double result = BooleanConversions.toDouble(Boolean.TRUE, converter);
        assertSame(CommonValues.DOUBLE_ONE, result, "Method should return the exact constant instance CommonValues.DOUBLE_ONE");
    }

    @Test
    void toDouble_returnsExactConstantInstanceForFalse() {
        Double result = BooleanConversions.toDouble(Boolean.FALSE, converter);
        assertSame(CommonValues.DOUBLE_ZERO, result, "Method should return the exact constant instance CommonValues.DOUBLE_ZERO");
    }

    @Test
    void toDouble_throwsClassCastExceptionForInvalidSourceType() {
        // GIVEN a String instead of Boolean
        Object source = "true";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toDouble(source, converter), "Casting a non‑Boolean source should throw ClassCastException");
    }

    @Test
    void toDouble_throwsNPEWhenSourceIsNullEvenWithNonNullConverter() {
        // GIVEN null source and a non‑null converter (mocked)
        Object source = null;
        Converter mockConv = mock(Converter.class);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toDouble(source, mockConv), "Null source must still cause NullPointerException regardless of converter");
    }
}
