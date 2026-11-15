package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoubleConversions_toDate_Test {

    /**
     * Mocked Converter – not used by the method under test but required by the signature.
     */
    @Mock
    private Converter mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Ensure the method never touches the mock.
        verifyNoInteractions(mockConverter);
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validDoubleConversionsProvider() {
        return Stream.of(arguments(0.0, null, 0L, "Zero double should produce Date at epoch (0 ms)."), arguments(1.5, null, 1500L, "Positive double with fractional part should be multiplied by 1000 and truncated."), arguments(-2.75, null, -2750L, "Negative double should be converted to negative epoch milliseconds."), arguments(123456789.987, null, 123456789987L, "Large positive double, verify correct truncation after multiplication."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> specialDoubleValuesProvider() {
        return Stream.of(arguments(Double.NaN, null, 0L, "NaN cast to long yields 0, so Date should be epoch."), arguments(Double.POSITIVE_INFINITY, null, Long.MAX_VALUE, "Positive infinity cast to long results in Long.MAX_VALUE."), arguments(Double.NEGATIVE_INFINITY, null, Long.MIN_VALUE, "Negative infinity cast to long results in Long.MIN_VALUE."), // Value just above Long.MAX_VALUE after scaling
        arguments(9.223372036854776e+18, null, Long.MAX_VALUE, "Double larger than Long.MAX_VALUE should be capped at Long.MAX_VALUE after casting."), arguments(-9.223372036854776e+18, null, Long.MIN_VALUE, "Double smaller than Long.MIN_VALUE should be capped at Long.MIN_VALUE after casting."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> subMillisecondProvider() {
        return Stream.of(arguments(0.0005, null, 0L, "Positive sub‑millisecond value should truncate to 0 ms."), arguments(-0.0005, null, 0L, "Negative sub‑millisecond value truncates toward zero, yielding 0 ms."), arguments(0.999, null, 999L, "Value just below 1 second should become 999 ms."), arguments(-0.999, null, -999L, "Negative value just above -1 second should become -999 ms."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> extremeValuesProvider() {
        return Stream.of(arguments(Double.MAX_VALUE, null, Long.MAX_VALUE, "Double.MAX_VALUE overflows to Infinity after multiplication; cast yields Long.MAX_VALUE."), arguments(Double.MIN_VALUE, null, 0L, "Double.MIN_VALUE (smallest positive) remains < 1 ms after scaling, resulting in 0 ms."), arguments(-0.0, null, 0L, "Negative zero should produce the same epoch (0 ms) as positive zero."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> nonNullConverterProvider() {
        // For these cases we deliberately pass a non‑null mock to verify it is ignored.
        return Stream.of(arguments(2.0, mock(Converter.class), 2000L, "Converter argument is ignored; conversion of 2.0 double must still yield 2000 ms."), arguments(-3.14159, mock(Converter.class), -3141L, "Even with a complex non‑null converter object, method should return -3141 ms."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidInputsProvider() {
        return Stream.of(arguments(null, null, NullPointerException.class, "Null 'from' causes a NullPointerException when casting to Double."), arguments(42, null, ClassCastException.class, "Non‑Double object (Integer) triggers ClassCastException during cast."), arguments("123.45", null, ClassCastException.class, "String cannot be cast to Double, resulting in ClassCastException."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> additionalCastExceptionProvider() {
        return Stream.of(arguments(true, null, ClassCastException.class, "Boolean value cannot be cast to Double, should throw ClassCastException."), arguments(new int[] { 1, 2, 3 }, null, ClassCastException.class, "Array (int[]) cannot be cast to Double, should throw ClassCastException."), arguments(1234567890123L, null, ClassCastException.class, "Long integer is not a Double wrapper, casting should fail with ClassCastException."), arguments(new java.util.HashMap<String, String>() {

            {
                put("some", "object");
            }
        }, null, ClassCastException.class, "Custom object cannot be cast to Double, should result in ClassCastException."));
    }

    @Test
    void toDate_positiveWholeNumber() {
        // seconds
        Double source = 1_234_567.0;
        Date result = DoubleConversions.toDate(source, mockConverter);
        assertNotNull(result);
        assertEquals(1_234_567_000L, result.getTime(), "Date time should be seconds * 1000 (milliseconds).");
    }

    @Test
    void toDate_zero() {
        Double source = 0.0;
        Date result = DoubleConversions.toDate(source, mockConverter);
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "Zero seconds must produce epoch start (0 ms).");
    }

    @Test
    void toDate_negativeNumber() {
        // seconds
        Double source = -42.5;
        Date result = DoubleConversions.toDate(source, mockConverter);
        assertNotNull(result);
        // Expected milliseconds = -42.5 * 1000 = -42500
        assertEquals(-42_500L, result.getTime(), "Negative seconds should be correctly converted to negative milliseconds.");
    }

    @Test
    void toDate_fractionalSeconds() {
        // seconds
        Double source = 1.234;
        Date result = DoubleConversions.toDate(source, mockConverter);
        assertNotNull(result);
        // (long)(1.234 * 1000) = (long)1234.0 = 1234
        assertEquals(1_234L, result.getTime(), "Fractional seconds must be truncated when cast to long.");
    }

    @Test
    void toDate_invalidSourceType_throwsClassCastException() {
        // The method expects a Double; passing any other type should cause a ClassCastException.
        Object source = "not a double";
        assertThrows(ClassCastException.class, () -> DoubleConversions.toDate(source, mockConverter));
    }

    @Test
    void toDate_nullSource_throwsNullPointerException() {
        // The implementation casts the argument to Double without a null‑check.
        assertThrows(NullPointerException.class, () -> DoubleConversions.toDate(null, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validDoubleConversionsProvider")
    void toDate_validDoubleConversions(double from, Converter converter, long expectedMillis, String description) {
        Date result = DoubleConversions.toDate(from, converter != null ? converter : mockConverter);
        assertNotNull(result, "Resulting Date must not be null.");
        assertEquals(expectedMillis, result.getTime(), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("specialDoubleValuesProvider")
    void toDate_specialDoubleValues(double from, Converter converter, long expectedMillis, String description) {
        Date result = DoubleConversions.toDate(from, converter != null ? converter : mockConverter);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("subMillisecondProvider")
    void toDate_subMillisecondAndRounding(double from, Converter converter, long expectedMillis, String description) {
        Date result = DoubleConversions.toDate(from, converter != null ? converter : mockConverter);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("extremeValuesProvider")
    void toDate_extremeDoubleValues(double from, Converter converter, long expectedMillis, String description) {
        Date result = DoubleConversions.toDate(from, converter != null ? converter : mockConverter);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nonNullConverterProvider")
    void toDate_nonNullConverterIgnored(double from, Converter converter, long expectedMillis, String description) {
        Date result = DoubleConversions.toDate(from, converter);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), description);
        // The specific (non‑mock) converter should still not be interacted with.
        verifyNoInteractions(converter);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputsProvider")
    void toDate_invalidInputs(Object from, Converter converter, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> DoubleConversions.toDate(from, converter != null ? converter : mockConverter), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("additionalCastExceptionProvider")
    void toDate_additionalClassCastExceptions(Object from, Converter converter, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> DoubleConversions.toDate(from, converter != null ? converter : mockConverter), description);
    }
}
