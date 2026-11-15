package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import java.util.Date;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toDate_Test {

    /**
     * A real {@link Converter} instance – the method does not use it, but it must be supplied.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options (no overrides needed for this test)
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none here)
        converter = null;
    }

    private static Stream<Arguments> provideNormalConversions() {
        return Stream.of(// Zero
        arguments(0.0, 0L, "Zero double should produce the epoch date."), // Positive fractional
        arguments(1.5, 1500L, "Positive fractional seconds are multiplied by 1000 and truncated to milliseconds."), // Negative whole seconds
        arguments(-2.0, -2000L, "Negative double values produce dates before the epoch."), // Very large but still fitting long
        arguments(1.0E12, 1_000_000_000_000_000L, "A very large double that still fits in a long after scaling."), // Double.MAX_VALUE → overflow → Long.MAX_VALUE
        arguments(Double.MAX_VALUE, Long.MAX_VALUE, "Double.MAX_VALUE overflows when multiplied; casting to long yields Long.MAX_VALUE."), // Double.NaN → 0
        arguments(Double.NaN, 0L, "NaN casts to 0L, resulting in the epoch date."), // Positive infinity → Long.MAX_VALUE
        arguments(Double.POSITIVE_INFINITY, Long.MAX_VALUE, "Positive infinity multiplied by 1000 becomes Infinity; casting to long yields Long.MAX_VALUE."), // Negative infinity → Long.MIN_VALUE
        arguments(Double.NEGATIVE_INFINITY, Long.MIN_VALUE, "Negative infinity multiplied by 1000 becomes -Infinity; casting to long yields Long.MIN_VALUE."), // -Double.MAX_VALUE → Long.MIN_VALUE
        arguments(-Double.MAX_VALUE, Long.MIN_VALUE, "Negating Double.MAX_VALUE results in a value that overflows to -Infinity after scaling; casting to long yields Long.MIN_VALUE."), // Double.MIN_VALUE (smallest positive) → 0
        arguments(Double.MIN_VALUE, 0L, "The smallest positive double, when multiplied by 1000, is still less than 1 ms; casting to long truncates to 0."), // Sub‑millisecond positive
        arguments(0.0004, 0L, "A positive double smaller than 0.001 seconds produces a fractional millisecond that truncates to 0."), // Sub‑millisecond negative
        arguments(-0.0004, 0L, "A negative double with magnitude less than 0.001 seconds yields -0.4 ms; casting to long truncates toward zero, also producing 0."));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(arguments(null, NullPointerException.class, "Passing null as the source object causes a NullPointerException during the cast to Double."), arguments(Integer.valueOf(5), ClassCastException.class, "A non‑Double object (Integer) cannot be cast to Double and should raise ClassCastException."));
    }

    @Test
    void toDate_ZeroSeconds_ReturnsEpoch() {
        // given
        Double source = 0.0;
        // when
        Date result = DoubleConversions.toDate(source, converter);
        // then
        assertEquals(new Date(0L), result, "0 seconds should map to the epoch (0 ms)");
    }

    @Test
    void toDate_PositiveFractionalSeconds_ConvertsCorrectly() {
        // given
        // seconds
        Double source = 1.2345;
        // when
        Date result = DoubleConversions.toDate(source, converter);
        // then
        // truncates toward zero
        long expectedMillis = (long) (1.2345 * 1000);
        assertEquals(new Date(expectedMillis), result, "1.2345 seconds should become " + expectedMillis + " ms");
    }

    @Test
    void toDate_NegativeSeconds_ConvertsCorrectly() {
        // given
        // seconds
        Double source = -2.5;
        // when
        Date result = DoubleConversions.toDate(source, converter);
        // then
        // -2500 ms
        long expectedMillis = (long) (-2.5 * 1000);
        assertEquals(new Date(expectedMillis), result, "-2.5 seconds should become " + expectedMillis + " ms");
    }

    @Test
    void toDate_NonDoubleArgument_ThrowsClassCastException() {
        // given
        // not a Double
        Integer source = 42;
        // when / then
        assertThrows(ClassCastException.class, () -> DoubleConversions.toDate(source, converter), "Passing a non‑Double should result in a ClassCastException");
    }

    @Test
    void toDate_NullArgument_ThrowsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DoubleConversions.toDate(source, converter), "Passing null should result in a NullPointerException due to unboxing");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideNormalConversions")
    void toDate_NormalConversions(Object from, long expectedMillis, String description) {
        // The method under test ignores the converter argument, so we can safely pass null.
        Date result = DoubleConversions.toDate(from, null);
        assertEquals(new Date(expectedMillis), result, () -> "Failed conversion: " + description);
    }

    @Test
    void toDate_ConverterArgumentIgnored_WithNonNullConverter() {
        // given
        Double source = 10.0;
        // a real (non‑null) converter – the method must ignore it
        Converter realConverter = new Converter(new ConverterOptions());
        // when
        Date result = DoubleConversions.toDate(source, realConverter);
        // then
        assertEquals(new Date(10_000L), result, "Even with a non‑null Converter supplied, conversion must be based solely on the double value.");
    }

    @Test
    void toDate_ConverterSideEffectIgnored_MockThrowsIfUsed() {
        // given
        Double source = 5.0;
        // Mock that throws on any method call (default answer)
        Converter throwingMock = Mockito.mock(Converter.class, (Answer<Object>) invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        });
        // when
        Date result = DoubleConversions.toDate(source, throwingMock);
        // then
        assertEquals(new Date(5_000L), result, "The method must not invoke the Converter; conversion should succeed despite the mock throwing.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideExceptionCases")
    void toDate_ExceptionHandling(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> DoubleConversions.toDate(from, null), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
