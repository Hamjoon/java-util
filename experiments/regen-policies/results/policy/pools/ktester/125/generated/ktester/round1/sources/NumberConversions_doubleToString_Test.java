package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_doubleToString_Test {

    /**
     * The Converter dependency – not used by the method but required by the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Supplies the arguments for {@link #doubleToString_variousScenarios(Object, String, String)}.
     */
    private static Stream<Arguments> doubleToStringProvider() {
        return Stream.of(// zero value
        Arguments.of(0.0d, "0", "When `from` is a Double with value 0.0 the method should return the literal string \"0\" (the special zero‑branch)."), // positive non‑zero
        Arguments.of(123.456d, "123.456", "A positive non‑zero Double should bypass the zero‑branch and return its standard string representation."), // negative non‑zero
        Arguments.of(-78.9d, "-78.9", "A negative non‑zero Double should bypass the zero‑branch and return its standard string representation."), // negative zero (treated as zero)
        Arguments.of(-0.0d, "0", "Negative zero (`-0.0`) compares equal to `0.0`; the method should treat it as zero and return the literal \"0\"."), // NaN
        Arguments.of(Double.NaN, "NaN", "`Double.NaN` is not equal to zero, so the method should return the string produced by `Double.toString()`, i.e., \"NaN\"."), // positive infinity
        Arguments.of(Double.POSITIVE_INFINITY, "Infinity", "`Double.POSITIVE_INFINITY` is not zero; the method should return its `toString()` value \"Infinity\"."), // negative infinity
        Arguments.of(Double.NEGATIVE_INFINITY, "-Infinity", "`Double.NEGATIVE_INFINITY` is not zero; the method should return its `toString()` value \"-Infinity\"."), // non‑Double type (Float) – expect ClassCastException
        Arguments.of(Float.valueOf(1.0f), "ClassCastException", "Passing a `Float` (or any non‑Double wrapper) causes a `ClassCastException` when the method attempts to cast to `double`."), // non‑Double type (String) – expect ClassCastException
        Arguments.of("1.0", "ClassCastException", "Passing a `String` results in a `ClassCastException` because the cast to `double` expects a `Double` instance."), // null from – expect NullPointerException
        Arguments.of(null, "NullPointerException", "A `null` argument triggers a `NullPointerException` during unboxing to `double`."), // non‑null converter (unused) – behaviour identical to normal case
        Arguments.of(2.5d, "2.5", "The `converter` argument is not used; providing a non‑null instance should not affect the returned string."));
    }

    @Test
    void whenDoubleIsZero_thenReturnsStringZero() {
        // GIVEN
        Double zero = 0.0d;
        // WHEN
        String result = NumberConversions.doubleToString(zero, converter);
        // THEN
        assertEquals("0", result, "Zero double should be converted to the literal string \"0\"");
    }

    @Test
    void whenDoubleIsNonZero_thenReturnsObjectToString() {
        // GIVEN
        Double value = 12.34d;
        // WHEN
        String result = NumberConversions.doubleToString(value, converter);
        // THEN
        assertEquals(value.toString(), result, "Non‑zero double should be converted using its toString() representation");
    }

    @Test
    void whenDoubleIsNegativeNonZero_thenReturnsObjectToString() {
        // GIVEN
        Double value = -7.89d;
        // WHEN
        String result = NumberConversions.doubleToString(value, converter);
        // THEN
        assertEquals(value.toString(), result, "Negative non‑zero double should be converted using its toString() representation");
    }

    @Test
    void whenDoubleIsNaN_thenReturnsObjectToString() {
        // GIVEN
        Double value = Double.NaN;
        // WHEN
        String result = NumberConversions.doubleToString(value, converter);
        // THEN
        assertEquals(value.toString(), result, "NaN double should be converted using its toString() representation");
    }

    /**
     * Parameterised test that drives all scenarios described in the JSON
     * test case list.  The {@code expected} argument can be:
     * <ul>
     *   <li>a literal string – the expected return value, or</li>
     *   <li>"ClassCastException" – the test expects a {@code ClassCastException}, or</li>
     *   <li>"NullPointerException" – the test expects a {@code NullPointerException}.</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("doubleToStringProvider")
    void doubleToString_variousScenarios(Object from, String expected, String description) {
        if ("ClassCastException".equals(expected)) {
            assertThrows(ClassCastException.class, () -> NumberConversions.doubleToString(from, converter), description);
        } else if ("NullPointerException".equals(expected)) {
            assertThrows(NullPointerException.class, () -> NumberConversions.doubleToString(from, converter), description);
        } else {
            String actual = NumberConversions.doubleToString(from, converter);
            assertEquals(expected, actual, description);
        }
    }
}
