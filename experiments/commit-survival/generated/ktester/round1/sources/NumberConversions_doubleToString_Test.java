package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_doubleToString_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    private AutoCloseable mocks;

    /**
     * A dummy non‑null Converter used for the few test cases that explicitly pass a Converter.
     */
    private static final Converter DUMMY_CONVERTER = mock(Converter.class);

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for the @Mock fields.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        mocks.close();
    }

    /**
     * Provides all inputs that should succeed and return a string.
     */
    private static Stream<Arguments> provideSuccessCases() {
        return Stream.of(// Zero handling (positive and negative zero)
        Arguments.of(0.0d, null, "0"), Arguments.of(-0.0d, null, "0"), // Normal non‑zero values
        Arguments.of(123.456d, null, "123.456"), Arguments.of(123.0d, DUMMY_CONVERTER, "123.0"), // Special double constants
        Arguments.of(Double.NaN, null, "NaN"), Arguments.of(Double.POSITIVE_INFINITY, null, "Infinity"), Arguments.of(Double.NEGATIVE_INFINITY, null, "-Infinity"), // Extreme double values
        Arguments.of(Double.MAX_VALUE, null, Double.toString(Double.MAX_VALUE)), Arguments.of(-Double.MAX_VALUE, null, Double.toString(-Double.MAX_VALUE)), Arguments.of(Double.MIN_VALUE, null, Double.toString(Double.MIN_VALUE)), Arguments.of(-Double.MIN_VALUE, null, Double.toString(-Double.MIN_VALUE)), Arguments.of(Double.MIN_NORMAL, null, Double.toString(Double.MIN_NORMAL)), // Zero with an explicit non‑null Converter
        Arguments.of(0.0d, DUMMY_CONVERTER, "0"));
    }

    /**
     * Provides all inputs that should result in an exception.
     */
    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Wrong numeric wrapper types
        Arguments.of(Integer.valueOf(0), null, ClassCastException.class), Arguments.of(Float.valueOf(1.23f), null, ClassCastException.class), Arguments.of(Long.valueOf(5L), null, ClassCastException.class), // Completely unrelated types
        Arguments.of("someString", null, ClassCastException.class), Arguments.of(Boolean.TRUE, null, ClassCastException.class), Arguments.of('c', null, ClassCastException.class), // Big numbers and other objects
        Arguments.of(new BigDecimal("123.45"), null, ClassCastException.class), Arguments.of(new BigInteger("12345"), null, ClassCastException.class), Arguments.of(new int[] { 1, 2, 3 }, null, ClassCastException.class), Arguments.of(new Object(), null, ClassCastException.class), // Null source – unboxing to primitive double triggers NPE
        Arguments.of(null, null, NullPointerException.class));
    }

    @Test
    void doubleToString_returnsZeroStringWhenValueIsZero() {
        // GIVEN
        Double zero = 0.0;
        // WHEN
        String result = NumberConversions.doubleToString(zero, mockConverter);
        // THEN
        assertEquals("0", result, "Expected the string \"0\" for a double value of 0.0");
        // Verify that the converter is not interacted with (method does not use it)
        verifyNoInteractions(mockConverter);
    }

    @Test
    void doubleToString_returnsObjectToStringWhenValueIsNonZero() {
        // GIVEN
        Double value = 123.456;
        // WHEN
        String result = NumberConversions.doubleToString(value, mockConverter);
        // THEN
        assertEquals(value.toString(), result, "Expected the object's toString() result for a non‑zero double");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void doubleToString_throwsClassCastExceptionWhenObjectIsNotDouble() {
        // GIVEN
        Object notADouble = "not a double";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> NumberConversions.doubleToString(notADouble, mockConverter), "Casting a non‑Double should result in ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Successful conversions – the method should return the expected string.
     */
    @ParameterizedTest(name = "[{index}] from={0} (converter={1}) => \"{2}\"")
    @MethodSource("provideSuccessCases")
    void doubleToString_returnsExpectedString(Object from, Converter converter, String expected) {
        // WHEN
        String result = NumberConversions.doubleToString(from, converter);
        // THEN
        assertEquals(expected, result, () -> "Expected conversion result \"" + expected + "\" for input " + from);
        // The method never touches the Converter, so the mock should stay untouched.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Exceptional conversions – the method should throw the expected exception.
     */
    @ParameterizedTest(name = "[{index}] from={0} (converter={1}) => throws {2}")
    @MethodSource("provideExceptionCases")
    void doubleToString_throwsExpectedException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> NumberConversions.doubleToString(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input " + from);
        verifyNoInteractions(mockConverter);
    }
}
